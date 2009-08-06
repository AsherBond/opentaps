/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
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
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.opentaps.common.party.PartyContactHelper;
import org.opentaps.domain.base.entities.AsteriskUser;
import org.opentaps.domain.base.entities.PartyContactDetailByPurpose;
import org.opentaps.domain.base.entities.PartyFromSummaryByRelationship;
import org.opentaps.domain.base.entities.PartyNoteView;
import org.opentaps.domain.base.entities.PartySummaryCRMView;
import org.opentaps.domain.base.entities.PostalAddress;
import org.opentaps.domain.base.entities.TelecomNumber;
import org.opentaps.domain.base.entities.UserLogin;
import org.opentaps.domain.billing.payment.PaymentMethod;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/** {@inheritDoc} */
public class PartyRepository extends Repository implements PartyRepositoryInterface {

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
    public PartyRepository(GenericDelegator delegator) {
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
                  EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.roleTypeIdFrom.name(), "CONTACT"),
                  EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.roleTypeIdTo.name(), "ACCOUNT"),
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
                       EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.roleTypeIdFrom.name(), "CONTACT"),
                       EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.roleTypeIdTo.name(), "ACCOUNT"),
                       EntityCondition.makeCondition(PartyFromSummaryByRelationship.Fields.partyIdTo.name(), parentAccount.getPartyId()),
                       EntityUtil.getFilterByDateExpr());

        List<PartyFromSummaryByRelationship> contactValues = findList(PartyFromSummaryByRelationship.class, conditions, Arrays.asList(PartyFromSummaryByRelationship.Fields.partyIdFrom.name()));
        if (UtilValidate.isEmpty(contactValues)) {
            return null;
        }

        resultSet.addAll(findList(Contact.class, EntityCondition.makeCondition("partyId", EntityOperator.IN, Entity.getDistinctFieldValues(contactValues, PartyFromSummaryByRelationship.Fields.partyIdFrom))));

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
    @SuppressWarnings("unchecked")
    public List<PostalAddress> getPostalAddresses(Party party, Party.ContactPurpose purpose) throws RepositoryException {
        String purposeId = getOpentapsContactPurpose(purpose);

        if (purposeId == null) {
            return new FastList<PostalAddress>();
        }

        try {
            List<GenericValue> purposes = PartyContactHelper.getContactMechsByPurpose(party.getPartyId(), "POSTAL_ADDRESS", purposeId, true, getDelegator());
            List contactList = EntityUtil.getFieldListFromEntityList(purposes, "contactMechId", true);
            return findList(PostalAddress.class, Arrays.asList(EntityCondition.makeCondition("contactMechId", EntityOperator.IN, contactList)));
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public List<TelecomNumber>  getPhoneNumbers(Party party) throws RepositoryException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition(PartyContactDetailByPurpose.Fields.partyId.name(), party.getPartyId()),
                        EntityCondition.makeCondition(PartyContactDetailByPurpose.Fields.contactMechTypeId.name(), "TELECOM_NUMBER"),
                        EntityUtil.getFilterByDateExpr(),
                        EntityUtil.getFilterByDateExpr(PartyContactDetailByPurpose.Fields.purposeFromDate.name(), PartyContactDetailByPurpose.Fields.purposeThruDate.name()));

        List<PartyContactDetailByPurpose> partyContactPurposes = findList(PartyContactDetailByPurpose.class, conditions);
        return findList(TelecomNumber.class, Arrays.asList(EntityCondition.makeCondition("contactMechId", EntityOperator.IN, Entity.getDistinctFieldValues(partyContactPurposes, PartyContactDetailByPurpose.Fields.contactMechId))));
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<TelecomNumber> getPhoneNumbers(Party party, Party.ContactPurpose purpose) throws RepositoryException {
        String purposeId = getOpentapsContactPurpose(purpose);

        if (purposeId == null) {
            return new FastList<TelecomNumber>();
        }

        try {
            List<GenericValue> purposes = PartyContactHelper.getContactMechsByPurpose(party.getPartyId(), "TELECOM_NUMBER", purposeId, true, getDelegator());
            List contactList = EntityUtil.getFieldListFromEntityList(purposes, "contactMechId", true);
            return findList(TelecomNumber.class, Arrays.asList(EntityCondition.makeCondition("contactMechId", EntityOperator.IN, contactList)));
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public Map<String, Object> getPartyNameForDate(Party party, Timestamp date) throws RepositoryException {
        try {
            return getDispatcher().runSync("getPartyNameForDate", UtilMisc.toMap("userLogin", getUser().getOfbizUserLogin(), "partyId", party.getPartyId(), "compareDate", date));
        } catch (GenericServiceException e) {
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
            case PRIMARY_ADDRESS: return "PRIMARY_LOCATION";
            case BILLING_ADDRESS: return "BILLING_LOCATION";
            case GENERAL_ADDRESS: return "GENERAL_LOCATION";
            case SHIPPING_ADDRESS: return "SHIPPING_LOCATION";
            case PRIMARY_PHONE: return "PRIMARY_PHONE";
            case FAX_NUMBER: return "FAX_NUMBER";
            case BILLING_PHONE: return "PHONE_BILLING";
            case PRIMARY_EMAIL: return "PRIMARY_EMAIL";
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
        try {
            Session session = getInfrastructure().getSession();
            // prepare the SQL to get PartyContactMech formatted full number (countryCode + areaCode + contactNumber) removing delimiters
            // this returns something like 'CCACCN' with CC country code, AC area code and CN contact number
            String formatedPhoneNumber = "concat(concat(case when pcm.contactMech.telecomNumber.countryCode is null then '' else pcm.contactMech.telecomNumber.countryCode end, case when pcm.contactMech.telecomNumber.areaCode is null then '' else pcm.contactMech.telecomNumber.areaCode end), pcm.contactMech.telecomNumber.contactNumber)";
            String formatedPhoneNumber1 = formatPhoneNumberSQL(formatedPhoneNumber);
            // note: there is a bug with HQL and postgres when the 'replace' from formatPhoneNumberSQL are inside a 'concat'
            // that's why we do it this way
            String formatedPhoneNumber2 = formatPhoneNumberSQL("concat('%'," + formatedPhoneNumber + ")");
            String hql = "select pcm.party from PartyContactMech pcm where"
                + " (pcm.thruDate is null or pcm.thruDate > current_date())"
                + " and (pcm.id.fromDate is null or pcm.id.fromDate <= current_date())"
                + " and (" + formatedPhoneNumber1 + " like :phoneNumber1"
                + " or " + formatedPhoneNumber2 + " like :phoneNumber2)";
            org.hibernate.Query query = session.createQuery(hql);
            query.setString("phoneNumber1", "%" + phoneNumber);
            query.setString("phoneNumber2", phoneNumber);
            List<org.opentaps.domain.base.entities.Party> parties = query.list();
            List<String> partyIds = new ArrayList<String>();
            for (org.opentaps.domain.base.entities.Party party : parties) {
                partyIds.add(party.getPartyId());
            }
            if (partyIds.size() > 0) {
                resultSet.addAll(getPartyByIds(partyIds));
            }
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
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
    public UserLogin getUserLoginByExtension(String extension) throws RepositoryException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                         EntityCondition.makeCondition(AsteriskUser.Fields.extension.name(), extension),
                         EntityUtil.getFilterByDateExpr());
        List<AsteriskUser> asteriskUsers = findList(AsteriskUser.class, conditions);

        // if not found, return null
        if (asteriskUsers.size() < 0) {
            Debug.logWarning("No AsteriskUser was found for the extension [" + extension + "], return null.", MODULE);
        } else if (asteriskUsers.size() > 1) {
            Debug.logWarning("More than one AsteriskUser was found for the extension [" + extension + "], returning the first match.", MODULE);
        }

        if (asteriskUsers.isEmpty()) {
            return null;
        } else {
            return asteriskUsers.get(0).getUserLogin();
        }
    }

    /** {@inheritDoc} */
    public AsteriskUser getAsteriskUserForUser(User user) throws RepositoryException, InfrastructureException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition(AsteriskUser.Fields.userLoginId.name(), user.getUserId()),
                        EntityUtil.getFilterByDateExpr());
        List<AsteriskUser> asteriskUsers = findList(AsteriskUser.class, conditions);

        // if not found, return null
        if (asteriskUsers.size() < 0) {
            Debug.logWarning("No AsteriskUser was found for the userLoginId [" + user.getUserId() + "], return null.", MODULE);
        } else if (asteriskUsers.size() > 1) {
            Debug.logWarning("More than one AsteriskUser was found for the userLoginId [" + user.getUserId() + "], returning the first match.", MODULE);
        }

        if (asteriskUsers.isEmpty()) {
            return null;
        } else {
            return asteriskUsers.get(0);
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
            GenericValue supplierAddress = PartyContactHelper.getPostalAddressValueByPurpose(party.getPartyId(), "GENERAL_LOCATION", true, getDelegator());
            if (supplierAddress == null) {
                supplierAddress = PartyContactHelper.getPostalAddressValueByPurpose(party.getPartyId(), "BILLING_LOCATION", true, getDelegator());
            }
            if (supplierAddress == null) {
                supplierAddress = PartyContactHelper.getPostalAddressValueByPurpose(party.getPartyId(), "PAYMENT_LOCATION", true, getDelegator());
            }
            if (supplierAddress != null) {
                String contactMechId = supplierAddress.getString("contactMechId");
                return findOneNotNull(PostalAddress.class, map(PostalAddress.Fields.contactMechId, contactMechId), "PostalAddress [" + contactMechId + "] not found");
            }
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
        return null;
    }
}
