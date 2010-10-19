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
package org.opentaps.common.domain.organization;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.domain.party.PartyRepository;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilDate;
import org.opentaps.base.constants.GlAccountTypeConstants;
import org.opentaps.base.constants.PeriodTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.entities.AcctgTagEnumType;
import org.opentaps.base.entities.AgreementTermTypesByDocumentType;
import org.opentaps.base.entities.CustomTimePeriod;
import org.opentaps.base.entities.Enumeration;
import org.opentaps.base.entities.EnumerationType;
import org.opentaps.base.entities.GlAccountTypeDefault;
import org.opentaps.base.entities.Party;
import org.opentaps.base.entities.PartyAcctgPreference;
import org.opentaps.base.entities.PartyGroup;
import org.opentaps.base.entities.PartyRole;
import org.opentaps.base.entities.PaymentMethod;
import org.opentaps.base.entities.TermType;
import org.opentaps.base.services.ConvertUomService;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * {@inheritDoc}
 */
public class OrganizationRepository extends PartyRepository implements OrganizationRepositoryInterface {

    private static final String MODULE = OrganizationRepositoryInterface.class.getName();

    /** List the available fiscal period types. */
    public static List<String> FISCAL_PERIOD_TYPES = Arrays.asList(PeriodTypeConstants.FISCAL_YEAR, PeriodTypeConstants.FISCAL_QUARTER, PeriodTypeConstants.FISCAL_MONTH, PeriodTypeConstants.FISCAL_WEEK, PeriodTypeConstants.FISCAL_BIWEEK);

    /**
     * Default constructor.
     */
    public OrganizationRepository() {
        super();
    }

    /**
     * Constructor with delegator.
     * @param delegator a <code>Delegator</code> value
     * @deprecated for legacy support only
     */
    @Deprecated
    public OrganizationRepository(Delegator delegator) {
        super(delegator);
    }

    /** {@inheritDoc} */
    public Organization getOrganizationById(String organizationPartyId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(organizationPartyId)) {
            return null;
        }

        PartyRole role = findOneNotNull(PartyRole.class, map(PartyRole.Fields.partyId, organizationPartyId, PartyRole.Fields.roleTypeId, RoleTypeConstants.INTERNAL_ORGANIZATIO), "Organization [" + organizationPartyId + "] not found with role INTERNAL_ORGANIZATIO");
        return role.getRelatedOne(Organization.class, "Party");
    }

    /** {@inheritDoc} */
    public List<CustomTimePeriod> getAllFiscalTimePeriods(String organizationPartyId) throws RepositoryException {
        return findList(CustomTimePeriod.class, map(CustomTimePeriod.Fields.organizationPartyId, organizationPartyId));
    }

    /** {@inheritDoc} */
    public List<CustomTimePeriod> getOpenFiscalTimePeriods(String organizationPartyId) throws RepositoryException {
        return getOpenFiscalTimePeriods(organizationPartyId, UtilDateTime.nowTimestamp());
    }

    /** {@inheritDoc} */
    public List<CustomTimePeriod> getOpenFiscalTimePeriods(String organizationPartyId, Timestamp asOfDate) throws RepositoryException {
        return getOpenFiscalTimePeriods(organizationPartyId, FISCAL_PERIOD_TYPES, asOfDate);
    }

    /** {@inheritDoc} */
    public List<CustomTimePeriod> getOpenFiscalTimePeriods(String organizationPartyId, List<String> fiscalPeriodTypes, Timestamp asOfDate) throws RepositoryException {
        // isClosed must either be null or N.  This or conditions prevents an issue where rows with null values are not selected
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                  EntityCondition.makeCondition(CustomTimePeriod.Fields.organizationPartyId.name(), organizationPartyId),
                  EntityCondition.makeCondition(CustomTimePeriod.Fields.periodTypeId.name(), EntityOperator.IN, fiscalPeriodTypes),
                  EntityUtil.getFilterByDateExpr(UtilDate.timestampToSqlDate(asOfDate), CustomTimePeriod.Fields.fromDate.name(), CustomTimePeriod.Fields.thruDate.name()),
                  EntityCondition.makeCondition(EntityOperator.OR,
                           EntityCondition.makeCondition(CustomTimePeriod.Fields.isClosed.name(), null),
                           EntityCondition.makeCondition(CustomTimePeriod.Fields.isClosed.name(), "N")));
        return findList(CustomTimePeriod.class, conditions);
    }

    /** {@inheritDoc} */
    public PaymentMethod getDefaultPaymentMethod(String organizationPartyId) throws RepositoryException {
        PaymentMethod defaultPaymentMethod = null;
        GlAccountTypeDefault glAccountTypeDefault = findOne(GlAccountTypeDefault.class, map(GlAccountTypeDefault.Fields.organizationPartyId, organizationPartyId, GlAccountTypeDefault.Fields.glAccountTypeId, GlAccountTypeConstants.BANK_STLMNT_ACCOUNT));
        if (glAccountTypeDefault != null) {
            defaultPaymentMethod = getFirst(findList(PaymentMethod.class, map(PaymentMethod.Fields.partyId, organizationPartyId, PaymentMethod.Fields.glAccountId, glAccountTypeDefault.getGlAccountId())));
        }
        return defaultPaymentMethod;
    }

    /** {@inheritDoc} */
    public BigDecimal determineUomConversionFactor(String organizationPartyId, String currencyUomId) throws RepositoryException {
        return determineUomConversionFactor(organizationPartyId, currencyUomId, UtilDateTime.nowTimestamp());
    }

    /** {@inheritDoc} */
    public BigDecimal determineUomConversionFactor(String organizationPartyId, String currencyUomId, Timestamp asOfDate) throws RepositoryException {
        try {
            Organization organization = getOrganizationById(organizationPartyId);
            // default conversion factor
            BigDecimal conversionFactor = BigDecimal.ONE;
            // if currencyUomId is null, return default
            if (currencyUomId == null) {
                return conversionFactor;
            }

            // get our organization's accounting preference
            PartyAcctgPreference accountingPreference = organization.getPartyAcctgPreference();
            if (accountingPreference == null) {
                throw new RepositoryException("Currency conversion failed: No PartyAcctgPreference entity data for organizationPartyId " + organization.getPartyId());
            }

            // if the currencies are equal, return default
            if (currencyUomId.equals(accountingPreference.getBaseCurrencyUomId())) {
                return conversionFactor;
            }

            // this does a currency conversion, based on currencyUomId and the party's accounting preferences.  conversionFactor will be used for postings
            ConvertUomService service = new ConvertUomService();
            service.setInOriginalValue(conversionFactor);
            service.setInUomId(currencyUomId);
            service.setInUomIdTo(accountingPreference.getBaseCurrencyUomId());
            service.setInAsOfDate(asOfDate);
            service.runSyncNoNewTransaction(getInfrastructure());

            if (service.isSuccess()) {
                conversionFactor = service.getOutConvertedValue();
            } else {
                throw new RepositoryException("Currency conversion failed: No currencyUomId defined in PartyAcctgPreference entity for organizationPartyId " + organization.getPartyId());
            }

            return conversionFactor;

        } catch (ServiceException e) {
            throw new RepositoryException(e);
        } catch (EntityNotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public Map<Integer, String> getAccountingTagTypes(String organizationPartyId, String accountingTagUsageTypeId) throws RepositoryException {
        Map<Integer, String> tagTypes = new TreeMap<Integer, String>();
        AcctgTagEnumType conf = findOneCache(AcctgTagEnumType.class, map(AcctgTagEnumType.Fields.organizationPartyId, organizationPartyId, AcctgTagEnumType.Fields.acctgTagUsageTypeId, accountingTagUsageTypeId));
        if (conf == null) {
            Debug.logInfo("No tag configuration found for organization [" + organizationPartyId + "]", MODULE);
            return tagTypes;
        }

        // find each non null configured tag type
        for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
            String type = conf.getString("enumTypeId" + i);
            if (type != null) {
                tagTypes.put(new Integer(i), type);
            }
        }
        return tagTypes;
    }

    /** {@inheritDoc} */
    public List<AccountingTagConfigurationForOrganizationAndUsage> getAccountingTagConfiguration(String organizationPartyId, String accountingTagUsageTypeId) throws RepositoryException {

        Map<Integer, String> tagTypes = getAccountingTagTypes(organizationPartyId, accountingTagUsageTypeId);
        AcctgTagEnumType acctgTagEnumType = findOneCache(AcctgTagEnumType.class, map(AcctgTagEnumType.Fields.organizationPartyId, organizationPartyId, AcctgTagEnumType.Fields.acctgTagUsageTypeId, accountingTagUsageTypeId));
        List<AccountingTagConfigurationForOrganizationAndUsage> tagTypesAndValues = new ArrayList<AccountingTagConfigurationForOrganizationAndUsage>();

        for (Integer index : tagTypes.keySet()) {
            String type = tagTypes.get(index);
            //get if required field value, default is N
            String isRequired = acctgTagEnumType.getString("isTagEnum" + index + "Required") == null ? "N" : acctgTagEnumType.getString("isTagEnum" + index + "Required");
            String defaultValue = acctgTagEnumType.getString("defaultTagEnumId" + index);
            AccountingTagConfigurationForOrganizationAndUsage tag = new AccountingTagConfigurationForOrganizationAndUsage(this);
            tag.setIndex(index);
            tag.setType(type);
            tag.setDescription(findOneCache(EnumerationType.class, map(EnumerationType.Fields.enumTypeId, type)).getDescription());
            tag.setTagValues(findListCache(Enumeration.class,
                                           Arrays.asList(
                                               EntityCondition.makeCondition(Enumeration.Fields.enumTypeId.name(), type)),
                                           Arrays.asList(Enumeration.Fields.sequenceId.asc())));
            // filter out disabled tags
            tag.setActiveTagValues(findList(Enumeration.class,
                                           Arrays.asList(
                                               EntityCondition.makeCondition(Enumeration.Fields.enumTypeId.name(), type),
                                               EntityCondition.makeCondition(EntityOperator.OR,
                                                   EntityCondition.makeCondition(Enumeration.Fields.disabled.name(), "N"),
                                                   EntityCondition.makeCondition(Enumeration.Fields.disabled.name(), null))),
                                           Arrays.asList(Enumeration.Fields.sequenceId.asc())));
            // add if required property for tag
            tag.setIsRequired(isRequired);
            // add its default value
            tag.setDefaultValue(defaultValue);
            if (UtilValidate.isNotEmpty(defaultValue)) {
                tag.setDefaultValueTag(findOneCache(Enumeration.class, map(Enumeration.Fields.enumId, defaultValue)));
            }

            tagTypesAndValues.add(tag);
        }

        return tagTypesAndValues;
    }

    /** {@inheritDoc} */
    public List<String> getValidTermTypeIds(String documentTypeId) throws RepositoryException {
        List<AgreementTermTypesByDocumentType> types = findListCache(AgreementTermTypesByDocumentType.class, map(AgreementTermTypesByDocumentType.Fields.documentTypeId, documentTypeId));
        return Entity.getFieldValues(String.class, types, AgreementTermTypesByDocumentType.Fields.termTypeId);
    }

    /** {@inheritDoc} */
    public List<TermType> getValidTermTypes(String documentTypeId) throws RepositoryException {
        return findListCache(TermType.class, EntityCondition.makeCondition(TermType.Fields.termTypeId.name(), EntityOperator.IN, getValidTermTypeIds(documentTypeId)));
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<Organization> getAllValidOrganizations() throws RepositoryException {
        String hql = "select eo.party from PartyRole eo where eo.id.roleTypeId = 'INTERNAL_ORGANIZATIO'";
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            Query query = session.createQuery(hql);
            List<Party> parties = query.list();
            return findList(Organization.class, Arrays.asList(EntityCondition.makeCondition(Party.Fields.partyId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(parties, Party.Fields.partyId))));
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        }  finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(Map tags, String organizationPartyId, String accountingTagUsageTypeId, String prefix) throws RepositoryException {
        Debug.logInfo("validateTagParameters: for organization [" + organizationPartyId + "] and usage [" + accountingTagUsageTypeId + "]", MODULE);
        List<AccountingTagConfigurationForOrganizationAndUsage> missings = new ArrayList<AccountingTagConfigurationForOrganizationAndUsage>();
        for (AccountingTagConfigurationForOrganizationAndUsage tag : getAccountingTagConfiguration(organizationPartyId, accountingTagUsageTypeId)) {
            // if the tag is forced set the value, else if it is required, then validate its input
            String tagName = prefix + tag.getIndex();
            Debug.logInfo("validateTagParameters: tag current value = " + tags.get(tagName) + ", is required ? " + tag.isRequired() + ", has forced value ? " + tag.hasDefaultValue() + " = " + tag.getDefaultValue(), MODULE);
            if (tag.isRequired()) {
                String tagValue = (String) tags.get(tagName);
                if (UtilValidate.isEmpty(tagValue)) {
                    if (tag.hasDefaultValue()) {
                        tags.put(tagName, tag.getDefaultValue());
                    } else {
                        missings.add(tag);
                    }
                }
            }
        }
        return missings;
    }

    /** {@inheritDoc} */
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(EntityInterface entity, String organizationPartyId, String accountingTagUsageTypeId) throws RepositoryException {
        Debug.logInfo("validateTagParameters: for organization [" + organizationPartyId + "] and usage [" + accountingTagUsageTypeId + "]", MODULE);
        List<AccountingTagConfigurationForOrganizationAndUsage> missings = new ArrayList<AccountingTagConfigurationForOrganizationAndUsage>();
        for (AccountingTagConfigurationForOrganizationAndUsage tag : getAccountingTagConfiguration(organizationPartyId, accountingTagUsageTypeId)) {
            // if the tag is forced set the value, else if it is required, then validate its input
            String tagName = tag.getEntityFieldName();
            Debug.logInfo("validateTagParameters: tag current value = " + entity.getString(tagName) + ", is required ? " + tag.isRequired() + ", has forced value ? " + tag.hasDefaultValue() + " = " + tag.getDefaultValue(), MODULE);
            if (tag.isRequired()) {
                String tagValue = entity.getString(tagName);
                if (UtilValidate.isEmpty(tagValue)) {
                    if (tag.hasDefaultValue()) {
                        entity.set(tagName, tag.getDefaultValue());
                    } else {
                        missings.add(tag);
                    }
                }
            }
        }
        return missings;
    }

    /** {@inheritDoc} */
    public List<PartyGroup> getOrganizationTemplates() throws RepositoryException {
        String hql = "select eo.party.partyGroup from PartyRole eo where eo.id.roleTypeId = 'ORGANIZATION_TEMPL'";
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            Query query = session.createQuery(hql);
            List<PartyGroup> partyGroups = query.list();
            return partyGroups;
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }       
    }
    
    /** {@inheritDoc} */
    public List<PartyGroup> getOrganizationWithoutLedgerSetup() throws RepositoryException {
        String hql = "select eo.party.partyGroup from PartyRole eo where eo.id.roleTypeId='INTERNAL_ORGANIZATIO'";
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            Query query = session.createQuery(hql);
            List<PartyGroup> partyGroups1 = query.list();
            hql = "select eo.party.partyGroup from PartyAcctgPreference eo";
            query = session.createQuery(hql);
            List<PartyGroup> partyGroups2 = query.list();
            List<PartyGroup> partyGroups = new ArrayList<PartyGroup>();
            for (PartyGroup partyGroup : partyGroups1) {
                // filter the party group with role type INTERNAL_ORGANIZATIO and not have relate PartyAcctgPreference
                if (!partyGroups2.contains(partyGroup)) {
                    partyGroups.add(partyGroup);
                }
            }
            return partyGroups;
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        }  finally {
            if (session != null) {
                session.close();
            }
        }       
    }
}
