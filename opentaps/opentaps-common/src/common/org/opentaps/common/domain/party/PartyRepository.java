/*
 * Copyright (c) Open Source Strategies, Inc.
 *
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opentaps.common.domain.party;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.constants.ContactMechPurposeTypeConstants;
import org.opentaps.base.constants.ContactMechTypeConstants;
import org.opentaps.base.constants.PartyRelationshipTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.SecurityPermissionConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.ContactMech;
import org.opentaps.base.entities.ExternalUser;
import org.opentaps.base.entities.PartyAndContactMech;
import org.opentaps.base.entities.PartyFromByRelnAndContactInfoAndPartyClassification;
import org.opentaps.base.entities.PartyFromSummaryByRelationship;
import org.opentaps.base.entities.PartyGroup;
import org.opentaps.base.entities.PartyNoteView;
import org.opentaps.base.entities.PartyRelationship;
import org.opentaps.base.entities.PartySummaryCRMView;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.entities.SecurityGroupPermission;
import org.opentaps.base.entities.TelecomNumber;
import org.opentaps.base.entities.UserLogin;
import org.opentaps.base.services.GetPartyNameForDateService;
import org.opentaps.common.party.PartyContactHelper;
import org.opentaps.domain.DomainRepository;
import org.opentaps.domain.billing.payment.PaymentMethod;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Lead;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Repository for Parties to handle interaction of Party-related domain with the entity engine (database) and the service engine.
 */
public class PartyRepository extends DomainRepository implements PartyRepositoryInterface {

    private static final String MODULE = PartyRepository.class.getName();

    private OrderRepositoryInterface orderRepository;

    /**
     * Default constructor.
     */
    public PartyRepository() {
        super();
    }

    /**
     * Use this for Repositories which will only access the database via the delegator.
     * @param delegator the delegator
     */
    public PartyRepository(Delegator delegator) {
        super(delegator);
    }

    /**
     * Use this for domain Repositories.
     * @param infrastructure the domain infrastructure
     * @param user the domain user
     * @throws RepositoryException if an error occurs
     */
    public PartyRepository(Infrastructure infrastructure, User user) throws RepositoryException {
        super(infrastructure, user);
    }

    /** {@inheritDoc} */
    public Party getPartyById(String partyId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(partyId)) {
            return null;
        }
        return findOneNotNull(Party.class, map(Party.Fields.partyId, partyId), "Party [" + partyId + "] not found");
    }

    /** {@inheritDoc} */
    public Set<Party> getPartyByIds(List<String> partyIds) throws RepositoryException {
        Set<Party> resultSet = new FastSet<Party>();
        resultSet.addAll(findList(Party.class, EntityCondition.makeCondition(Party.Fields.partyId.name(), EntityOperator.IN, partyIds)));
        return resultSet;
    }

    /** {@inheritDoc} */
    public Lead getLeadById(String partyId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(partyId)) {
            Debug.logWarning("Requested lead but identifier was null.", MODULE);
            return null;
        }

        return findOneNotNull(Lead.class, map(Lead.Fields.partyId, partyId), "Lead [" + partyId + "] not found");
    }

    /** {@inheritDoc} */
    public Account getAccountById(String partyId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(partyId)) {
            return null;
        }
        return findOneNotNull(Account.class, map(Account.Fields.partyId, partyId), "Account [" + partyId + "] not found");
    }

    /** {@inheritDoc} */
    public Contact getContactById(String partyId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(partyId)) {
            return null;
        }
        return findOneNotNull(Contact.class, map(Contact.Fields.partyId, partyId), "Contact [" + partyId + "] not found");
    }

    /** {@inheritDoc} */
    public Set<Account> getAccounts(Contact contact) throws RepositoryException {
        if (contact == null) {
            return null;
        }

        Set<Account> resultSet = new FastSet<Account>();

        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                  EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.roleTypeIdFrom.name(), RoleTypeConstants.CONTACT),
                  EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.roleTypeIdTo.name(), RoleTypeConstants.ACCOUNT),
                  EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.partyIdFrom.name(), contact.getPartyId()),
            EntityUtil.getFilterByDateExpr());

        List<PartyFromSummaryByRelationship> accountValues = findList(PartyFromSummaryByRelationship.class, conditions, Arrays.asList("partyIdTo"));
        if (UtilValidate.isEmpty(accountValues)) {
            return null;
        }

        resultSet.addAll(findList(Account.class, EntityCondition.makeCondition(Account.Fields.partyId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(accountValues, PartyFromSummaryByRelationship.Fields.partyIdTo))));

        if (resultSet.size() > 0) {
            return resultSet;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public Set<Account> getSubAccounts(Account account) throws RepositoryException {
        Set<Account> resultSet = new FastSet<Account>();
        // find all PartySummaryCRMView where the given account is the parent account
        List<PartySummaryCRMView> parties = findList(PartySummaryCRMView.class, map(PartySummaryCRMView.Fields.parentPartyId, account.getPartyId()));
        resultSet.addAll(findList(Account.class, EntityCondition.makeCondition(Account.Fields.partyId.getName(), EntityOperator.IN, Entity.getDistinctFieldValues(parties, PartySummaryCRMView.Fields.partyId))));
        return resultSet;
    }

    /** {@inheritDoc} */
    public Set<Contact> getContacts(Account parentAccount) throws RepositoryException {
        Set<Contact> resultSet = new FastSet<Contact>();

        if (parentAccount == null) {
            return null;
        }

        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                       EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.roleTypeIdFrom.name(), RoleTypeConstants.CONTACT),
                       EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.roleTypeIdTo.name(), RoleTypeConstants.ACCOUNT),
                       EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.partyIdTo.name(), parentAccount.getPartyId()),
                       EntityUtil.getFilterByDateExpr());

        List<PartyFromSummaryByRelationship> contactValues = findList(PartyFromSummaryByRelationship.class, conditions, Arrays.asList(PartyFromSummaryByRelationship.Fields.partyIdFrom.name()));
        if (UtilValidate.isEmpty(contactValues)) {
            return null;
        }

        resultSet.addAll(findList(Contact.class, EntityCondition.makeCondition(Contact.Fields.partyId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(contactValues, PartyFromSummaryByRelationship.Fields.partyIdFrom))));

        if (resultSet.size() > 0) {
            return resultSet;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public PartySummaryCRMView getPartySummaryCRMView(String partyId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(partyId)) {
            return null;
        }
        return findOneNotNull(PartySummaryCRMView.class, map(PartySummaryCRMView.Fields.partyId, partyId), "PartySummaryCRMView [" + partyId + "] not found");
    }

    /** {@inheritDoc} */
    public List<PostalAddress> getPostalAddresses(Party party, Party.ContactPurpose purpose) throws RepositoryException {
        String purposeId = getOpentapsContactPurpose(purpose);

        if (purposeId == null) {
            return new FastList<PostalAddress>();
        }

        try {
            List<GenericValue> purposes = PartyContactHelper.getContactMechsByPurpose(party.getPartyId(), ContactMechTypeConstants.POSTAL_ADDRESS, purposeId, true, getDelegator());
            List<String> contactList = EntityUtil.getFieldListFromEntityList(purposes, ContactMech.Fields.contactMechId.name(), true);
            return findList(PostalAddress.class, Arrays.asList(EntityCondition.makeCondition(PostalAddress.Fields.contactMechId.name(), EntityOperator.IN, contactList)));
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public List<ContactMech> getEmailAddresses(Party party) throws RepositoryException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition(PartyAndContactMech.Fields.partyId.name(), party.getPartyId()),
                        EntityCondition.makeCondition(PartyAndContactMech.Fields.contactMechTypeId.name(), ContactMechTypeConstants.ElectronicAddress.EMAIL_ADDRESS),
                        EntityUtil.getFilterByDateExpr());

        List<PartyAndContactMech> partyContacts = findList(PartyAndContactMech.class, conditions, Arrays.asList(PartyAndContactMech.Fields.contactMechId.name()), null);
        return findList(ContactMech.class, Arrays.asList(EntityCondition.makeCondition(ContactMech.Fields.contactMechId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(partyContacts, PartyAndContactMech.Fields.contactMechId))));
    }

    /** {@inheritDoc} */
    public List<TelecomNumber> getPhoneNumbers(Party party) throws RepositoryException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition(PartyAndContactMech.Fields.partyId.name(), party.getPartyId()),
                        EntityCondition.makeCondition(PartyAndContactMech.Fields.contactMechTypeId.name(), ContactMechTypeConstants.TELECOM_NUMBER),
                        EntityUtil.getFilterByDateExpr());

        List<PartyAndContactMech> partyContacts = findList(PartyAndContactMech.class, conditions, Arrays.asList(PartyAndContactMech.Fields.contactMechId.name()), null);
        return findList(TelecomNumber.class, Arrays.asList(EntityCondition.makeCondition(TelecomNumber.Fields.contactMechId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(partyContacts, PartyAndContactMech.Fields.contactMechId))));
    }

    /** {@inheritDoc} */
    public List<TelecomNumber> getPhoneNumbers(Party party, Party.ContactPurpose purpose) throws RepositoryException {
        String purposeId = getOpentapsContactPurpose(purpose);

        if (purposeId == null) {
            return new FastList<TelecomNumber>();
        }

        try {
            List<GenericValue> purposes = PartyContactHelper.getContactMechsByPurpose(party.getPartyId(), ContactMechTypeConstants.TELECOM_NUMBER, purposeId, true, getDelegator());
            List<String> contactList = EntityUtil.getFieldListFromEntityList(purposes, ContactMech.Fields.contactMechId.name(), true);
            return findList(TelecomNumber.class, Arrays.asList(EntityCondition.makeCondition(TelecomNumber.Fields.contactMechId.name(), EntityOperator.IN, contactList)));
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public Map<String, Object> getPartyNameForDate(Party party, Timestamp date) throws RepositoryException {
        try {
            GetPartyNameForDateService service = new GetPartyNameForDateService(getUser());
            service.setInPartyId(party.getPartyId());
            service.setInCompareDate(date);
            service.runSync(getInfrastructure());
            return service.outputMap();
        } catch (ServiceException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Get the contact mech purpose type identifier from the <code>ContactPurpose</code> enumeration.
     * @return contact mech purpose type identifier
     * @param purpose a <code>ContactPurpose</code> enumeration value
     */
    public String getOpentapsContactPurpose(Party.ContactPurpose purpose) {
        switch (purpose) {
            case PRIMARY_ADDRESS: return ContactMechPurposeTypeConstants.PRIMARY_LOCATION;
            case BILLING_ADDRESS: return ContactMechPurposeTypeConstants.BILLING_LOCATION;
            case GENERAL_ADDRESS: return ContactMechPurposeTypeConstants.GENERAL_LOCATION;
            case SHIPPING_ADDRESS: return ContactMechPurposeTypeConstants.SHIPPING_LOCATION;
            case PRIMARY_PHONE: return ContactMechPurposeTypeConstants.PRIMARY_PHONE;
            case FAX_NUMBER: return ContactMechPurposeTypeConstants.FAX_NUMBER;
            case BILLING_PHONE: return ContactMechPurposeTypeConstants.PHONE_BILLING;
            case PRIMARY_EMAIL: return ContactMechPurposeTypeConstants.PRIMARY_EMAIL;
            default: return null;
        }
    }

    /** {@inheritDoc} */
    public List<PaymentMethod> getPaymentMethods(Party party) throws RepositoryException {
        return getOrderRepository().getRelatedPaymentMethods(party);
    }

    /**
     * rewrite getPartyByPhoneNumber by HQL.
     * @param phoneNumber a <code>String</code> value
     * @return a <code>String</code> value
     * @throws RepositoryException if an error occurs
     */
    public Set<Party> getPartyByPhoneNumber(String phoneNumber) throws RepositoryException {
        Set<Party> resultSet = new FastSet<Party>();
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            // prepare the SQL to get PartyContactMech formatted full number (countryCode + areaCode + contactNumber) removing delimiters
            // this returns something like 'CCACCN' with CC country code, AC area code and CN contact number
            String formatedPhoneNumber = "concat(concat(case when pcm.contactMech.telecomNumber.countryCode is null then '' else pcm.contactMech.telecomNumber.countryCode end, case when pcm.contactMech.telecomNumber.areaCode is null then '' else pcm.contactMech.telecomNumber.areaCode end), pcm.contactMech.telecomNumber.contactNumber)";
            String formatedPhoneNumber1 = formatPhoneNumberSQL(formatedPhoneNumber);
            // note: there is a bug with HQL and postgres when the 'replace' from formatPhoneNumberSQL are inside a 'concat'
            // that's why we do it this way
            String formatedPhoneNumber2 = formatPhoneNumberSQL("concat('%'," + formatedPhoneNumber + ")");
            String hql = "select pcm.party from PartyContactMech pcm where"
                + " (pcm.thruDate is null or pcm.thruDate > current_timestamp())"
                + " and (pcm.id.fromDate is null or pcm.id.fromDate <= current_timestamp())"
                + " and (" + formatedPhoneNumber1 + " like :phoneNumber1"
                + " or " + formatedPhoneNumber2 + " like :phoneNumber2)";
            org.hibernate.Query query = session.createQuery(hql);
            query.setString("phoneNumber1", "%" + phoneNumber);
            query.setString("phoneNumber2", phoneNumber);
            List<org.opentaps.base.entities.Party> parties = query.list();
            List<String> partyIds = new ArrayList<String>();
            for (org.opentaps.base.entities.Party party : parties) {
                partyIds.add(party.getPartyId());
            }
            if (partyIds.size() > 0) {
                resultSet.addAll(getPartyByIds(partyIds));
            }
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return resultSet;
    }

    /**
     * Helper method to convert a phone number in SQL by removing usual delimiters <code> -.)(</code>.
     * @param phoneNumberField the phone number
     * @return a SQL <code>String</code>
     */
    private static String formatPhoneNumberSQL(String phoneNumberField) {
        return "replace(replace(replace(replace(replace(" + phoneNumberField + ",' ',''),'-',''),'.',''),'(',''),')','')";
    }


    /** {@inheritDoc} */
    public ExternalUser getExternalUserForUser(String externalUserTypeId, User user) throws RepositoryException, InfrastructureException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition(ExternalUser.Fields.partyId.name(), EntityOperator.EQUALS, user.getOfbizUserLogin().getString("partyId")),
                EntityCondition.makeCondition(ExternalUser.Fields.externalUserTypeId.name(), EntityOperator.EQUALS, externalUserTypeId),
                EntityUtil.getFilterByDateExpr());
        List<ExternalUser> externalUsers = findList(ExternalUser.class, conditions);

        // if not found, return null
        if (externalUsers.size() < 0) {
            Debug.logWarning("No ExternalUser was found for the userLoginId [" + user.getUserId() + "], return null.", MODULE);
        } else if (externalUsers.size() > 1) {
            Debug.logWarning("More than one ExternalUser was found for the userLoginId [" + user.getUserId() + "], returning the first match.", MODULE);
        }

        if (externalUsers.isEmpty()) {
            return null;
        } else {
            return externalUsers.get(0);
        }
    }

    /** {@inheritDoc} */
    public List<PartyNoteView> getNotes(Party party) throws RepositoryException {
        return findList(PartyNoteView.class, map(PartyNoteView.Fields.targetPartyId, party.getPartyId()), Arrays.asList(PartyNoteView.Fields.noteDateTime.desc()));
    }

    protected OrderRepositoryInterface getOrderRepository() throws RepositoryException {
        if (orderRepository == null) {
            orderRepository = getDomainsDirectory().getOrderDomain().getOrderRepository();
        }
        return orderRepository;
    }

    /** {@inheritDoc} */
    public PostalAddress getSupplierPostalAddress(Party party) throws RepositoryException, EntityNotFoundException {
        try {
            GenericValue supplierAddress = PartyContactHelper.getPostalAddressValueByPurpose(party.getPartyId(), ContactMechPurposeTypeConstants.GENERAL_LOCATION, true, getDelegator());
            if (supplierAddress == null) {
                supplierAddress = PartyContactHelper.getPostalAddressValueByPurpose(party.getPartyId(), ContactMechPurposeTypeConstants.BILLING_LOCATION, true, getDelegator());
            }
            if (supplierAddress == null) {
                supplierAddress = PartyContactHelper.getPostalAddressValueByPurpose(party.getPartyId(), ContactMechPurposeTypeConstants.PAYMENT_LOCATION, true, getDelegator());
            }
            if (supplierAddress != null) {
                String contactMechId = supplierAddress.getString(PostalAddress.Fields.contactMechId.name());
                return findOneNotNull(PostalAddress.class, map(PostalAddress.Fields.contactMechId, contactMechId), "PostalAddress [" + contactMechId + "] not found");
            }
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
        return null;
    }

    /** {@inheritDoc} */
    public Set<Party> getPartyByEmail(String email) throws RepositoryException {
        Set<Party> resultSet = new FastSet<Party>();
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            // prepare the HQL to get Party
            String hql = "select pcm.party from PartyContactMech pcm where"
                + " (pcm.thruDate is null or pcm.thruDate > current_timestamp())"
                + " and (pcm.id.fromDate is null or pcm.id.fromDate <= current_timestamp())"
                + " and lower(trim(pcm.contactMech.infoString)) like :email"
                + " and (pcm.party.statusId is null or 'PARTY_DISABLED' <> pcm.party.statusId)";
            org.hibernate.Query query = session.createQuery(hql);
            query.setString("email", email.trim().toLowerCase());
            List<org.opentaps.base.entities.Party> parties = query.list();
            List<String> partyIds = new ArrayList<String>();
            for (org.opentaps.base.entities.Party party : parties) {
                partyIds.add(party.getPartyId());
            }
            if (partyIds.size() > 0) {
                resultSet.addAll(getPartyByIds(partyIds));
            }
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return resultSet;
    }

    /** {@inheritDoc} */
    public Set<Party> getPartyByName(String firstName, String lastName) throws RepositoryException {
        Set<Party> resultSet = new FastSet<Party>();
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            // prepare the HQL to get Party
            String hql = "select eo.party from Person eo where lower(trim(eo.firstName)) like :firstName"
                + " and lower(trim(eo.lastName)) like :lastName"
                + " and (eo.party.statusId is null or 'PARTY_DISABLED' <> eo.party.statusId)";
            org.hibernate.Query query = session.createQuery(hql);
            query.setString("firstName", firstName.trim().toLowerCase());
            query.setString("lastName", lastName.trim().toLowerCase());
            List<org.opentaps.base.entities.Party> parties = query.list();
            List<String> partyIds = new ArrayList<String>();
            for (org.opentaps.base.entities.Party party : parties) {
                partyIds.add(party.getPartyId());
            }
            if (partyIds.size() > 0) {
                resultSet.addAll(getPartyByIds(partyIds));
            }
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return resultSet;
    }

    /** {@inheritDoc} */
    public Set<PartyGroup> getPartyGroupByGroupName(String groupName) throws RepositoryException {
        Set<PartyGroup> resultSet = new FastSet<PartyGroup>();
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            // prepare the HQL to get Party
            String hql = "from PartyGroup eo where lower(trim(eo.groupName)) like :groupName"
                + " and (eo.party.statusId is null or 'PARTY_DISABLED' <> eo.party.statusId)";
            org.hibernate.Query query = session.createQuery(hql);
            query.setString("groupName", groupName.trim().toLowerCase());
            List<org.opentaps.base.entities.PartyGroup> partyGroups = query.list();
            Debug.logInfo("partyGroups.size() : " + partyGroups.size(), MODULE);
            resultSet.addAll(partyGroups);
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return resultSet;
    }

    /** {@inheritDoc} */
    public Set<PartyGroup> getPartyGroupByGroupNameAndRoleType(String groupName, String roleTypeId) throws RepositoryException {
        Set<PartyGroup> resultSet = new FastSet<PartyGroup>();
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            // prepare the HQL to get Party
            String hql = "select distinct eo.party.partyGroup from PartyRole eo where eo.party.partyGroup is not null and lower(trim(eo.party.partyGroup.groupName)) like :groupName"
                + " and (eo.party.statusId is null or 'PARTY_DISABLED' <> eo.party.statusId)"
                + " and eo.id.roleTypeId = :roleTypeId";
            org.hibernate.Query query = session.createQuery(hql);
            query.setString("groupName", groupName.trim().toLowerCase());
            query.setString("roleTypeId", roleTypeId);
            List<org.opentaps.base.entities.PartyGroup> partyGroups = query.list();
            Debug.logInfo("partyGroups.size() : " + partyGroups.size(), MODULE);
            resultSet.addAll(partyGroups);
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return resultSet;
    }

    /** {@inheritDoc} */
    public List<PartyRelationship> getPartyRelationship(String partyIdFrom, String partyIdTo) throws RepositoryException {
        EntityCondition filterByDateCondition = EntityUtil.getFilterByDateExpr();
        EntityCondition indirectConditions = EntityCondition.makeCondition(EntityOperator.AND,
                                        EntityCondition.makeCondition(PartyRelationship.Fields.partyIdFrom.name(), partyIdFrom),
                                        EntityCondition.makeCondition(PartyRelationship.Fields.partyIdTo, EntityOperator.EQUALS, partyIdTo),
                                        filterByDateCondition);
        return this.findList(PartyRelationship.class, indirectConditions);
    }

    /** {@inheritDoc} */
    public EntityCondition makeLookupLeadsCondition() throws RepositoryException {
        return EntityCondition.makeCondition(
                          EntityCondition.makeCondition(PartyRelationship.Fields.roleTypeIdFrom.name(),
                                                        RoleTypeConstants.PROSPECT),
                          EntityCondition.makeCondition(Party.Fields.statusId.name(),
                                                        EntityOperator.NOT_EQUAL,
                                                        StatusItemConstants.PartyLeadStatus.PTYLEAD_CONVERTED));
    }

    /** {@inheritDoc} */
    public EntityCondition makeLookupLeadsUserIsAllowedToViewCondition() throws RepositoryException {
        return makeLookupLeadsPartyIsAllowedToViewCondition(getUser().getOfbizUserLogin().getString(UserLogin.Fields.partyId.name()));
    }

    /** {@inheritDoc} */
    public EntityCondition makeLookupLeadsPartyIsAllowedToViewCondition(String partyId) throws RepositoryException {

        // Search security groups that contains the security permission "CRMSFA_LEAD_VIEW"
        List<SecurityGroupPermission> securityGrs = findList(SecurityGroupPermission.class, map(SecurityGroupPermission.Fields.permissionId,
                                                                                                SecurityPermissionConstants.CRMSFA_LEAD_VIEW));

        // Make permission condition
        List<EntityCondition> securityGroupCong = new ArrayList<EntityCondition>();
        for (int i = 0; i < securityGrs.size(); i++) {
            SecurityGroupPermission securityGroupThatHasPerm = securityGrs.get(i);
            securityGroupCong.add(EntityCondition.makeCondition(PartyRelationship.Fields.securityGroupId.name(),
                                                                securityGroupThatHasPerm.getGroupId()));
        }
        EntityCondition crmFindSecFilter = EntityCondition.makeCondition(securityGroupCong, EntityOperator.OR);

        // Add general leads conditions
        EntityCondition leadsCond = EntityCondition.makeCondition(
                                             makeLookupLeadsCondition(),
                                             crmFindSecFilter,
                                             EntityCondition.makeCondition(PartyRelationship.Fields.partyIdTo.name(), partyId),
                                             EntityUtil.getFilterByDateExpr());

        return leadsCond;
    }

    /** {@inheritDoc} */
    public Set<String> getLeadIdsUserIsAllowedToView() throws RepositoryException {
        return getLeadIdsPartyIsAllowedToView(getUser().getOfbizUserLogin().getString(UserLogin.Fields.partyId.name()));
    }

    /** {@inheritDoc} */
    public Set<String> getLeadIdsPartyIsAllowedToView(String partyId) throws RepositoryException {

        EntityCondition leadsCond = makeLookupLeadsPartyIsAllowedToViewCondition(partyId);

        // Find the lead ids
        return Entity.getDistinctFieldValues(String.class,
                                             findList(PartyFromByRelnAndContactInfoAndPartyClassification.class,
                                                      leadsCond,
                                                      Arrays.asList(PartyFromByRelnAndContactInfoAndPartyClassification.Fields.partyIdFrom.name()), // select fields
                                                      Arrays.asList(PartyFromByRelnAndContactInfoAndPartyClassification.Fields.partyIdFrom.name())), // order by
                                             PartyFromByRelnAndContactInfoAndPartyClassification.Fields.partyIdFrom);
    }

    /** {@inheritDoc} */
    public Boolean isUserAssignedToLead(String leadPartyId) throws RepositoryException {
        return isUserAssignedToLead(getUser(), leadPartyId);
    }

    /** {@inheritDoc} */
    public Boolean isUserAssignedToLead(User user, String leadPartyId) throws RepositoryException {
        return isPartyAssignedToLead(user.getOfbizUserLogin().getString(UserLogin.Fields.partyId.name()), leadPartyId);
    }

    /** {@inheritDoc} */
    public Boolean isPartyAssignedToLead(String partyId, String leadPartyId) throws RepositoryException {
        List<PartyRelationship> rels = getLeadAssignedPartyRelationships(partyId, leadPartyId);
        return UtilValidate.isNotEmpty(rels);
    }

    /**
     * Gets the active <code>PartyRelationship</code> entities representing the parties assignment to the given lead in any team.
     * @param leadPartyId the ID of the lead
     * @param pending if true also return the pending LeadAssignmentRequest relationships
     * @return the list of active <code>PartyRelationship</code>
     * @throws RepositoryException if an error occurs
     */
    protected List<PartyRelationship> getLeadAssignedPartyRelationships(String leadPartyId) throws RepositoryException {
        return getLeadAssignedPartyRelationships(null, leadPartyId);
    }

    /**
     * Gets the active <code>PartyRelationship</code> entities representing the given party assignment to the given lead in any team.
     * @param partyId the ID of the party to check as assigned
     * @param leadPartyId the ID of the lead
     * @param pending if true also return the pending LeadAssignmentRequest relationships
     * @return the list of active <code>PartyRelationship</code>
     * @throws RepositoryException if an error occurs
     */
    protected List<PartyRelationship> getLeadAssignedPartyRelationships(String partyId, String leadPartyId) throws RepositoryException {
        List<EntityCondition> conditions = UtilMisc.toList(
                EntityCondition.makeCondition(PartyRelationship.Fields.partyIdFrom.name(), leadPartyId),
                EntityCondition.makeCondition(PartyRelationship.Fields.roleTypeIdFrom.name(), RoleTypeConstants.PROSPECT),
                EntityCondition.makeCondition(PartyRelationship.Fields.roleTypeIdTo.name(), RoleTypeConstants.ACCOUNT_REP),
                EntityCondition.makeCondition(PartyRelationship.Fields.partyRelationshipTypeId.name(), PartyRelationshipTypeConstants.ASSIGNED_TO),
                EntityUtil.getFilterByDateExpr()
        );

        if (UtilValidate.isNotEmpty(partyId)) {
            conditions.add(EntityCondition.makeCondition(PartyRelationship.Fields.partyIdTo.name(), partyId));
        }

        return findList(PartyRelationship.class, EntityCondition.makeCondition(conditions));
    }
}
