package org.opentaps.common.domain.organization;

import java.math.BigDecimal;
import java.util.List;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.organization.OrganizationServiceInterface;
import org.opentaps.foundation.service.ServiceException;

/** {@inheritDoc} */
public class OrganizationService extends DomainService implements OrganizationServiceInterface {

    private static final String MODULE = OrganizationService.class.getName();

    protected String organizationPartyId = null;
    protected String templateOrganizationPartyId = null;
    protected Delegator delegator = null;
    
    /** {@inheritDoc} 
     * @throws ServiceException */
    public void copyOrganizationLedgerSetup() throws ServiceException {
        delegator = this.getInfrastructure().getDelegator();
        
        // check the security permission 
        if (!getSecurity().hasPermission("ORG_CONFIG", getUser().getOfbizUserLogin())) {
            throw new ServiceException(UtilMessage.expandLabel("OpentapsError_SecurityErrorToRunCopyOrganizationLedgerSetup", locale));
        }

        GenericValue partyAcctgPreference;
        try {
            // TODO use Java base entities.  But this code was already there.
            partyAcctgPreference = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", templateOrganizationPartyId));
            List<GenericValue> glAccountOrganizations = delegator.findByCondition("GlAccountOrganization", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> customTimePeriods = delegator.findByCondition("CustomTimePeriod", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> glAccountTypeDefaults = delegator.findByCondition("GlAccountTypeDefault", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> paymentGlAccountTypeMaps = delegator.findByCondition("PaymentGlAccountTypeMap", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> paymentMethodTypeGlAccounts = delegator.findByCondition("PaymentMethodTypeGlAccount", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> creditCardTypeGlAccounts = delegator.findByCondition("CreditCardTypeGlAccount", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> varianceReasonGlAccounts = delegator.findByCondition("VarianceReasonGlAccount", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> invoiceGlAccountTypes = delegator.findByCondition("InvoiceGlAccountType", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> invoiceItemTypeGlAccounts = delegator.findByCondition("InvoiceItemTypeGlAccount", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> invoiceAdjustmentGlAccounts = delegator.findByCondition("InvoiceAdjustmentGlAccount", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> acctgTagEnumTypes = delegator.findByCondition("AcctgTagEnumType", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> taxAuthorityGlAccounts = delegator.findByCondition("TaxAuthorityGlAccount", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, templateOrganizationPartyId), null, null);
            List<GenericValue> copies = new FastList<GenericValue>();
            // copy PartyAcctgPreference
            if (partyAcctgPreference != null) {
                GenericValue copy = delegator.makeValue(partyAcctgPreference.getEntityName(), partyAcctgPreference);
                copy.setString("partyId", organizationPartyId);
                copies.add(copy);
            }
            // create new CustomTimePeriods with the same characteristics as the old organization's, except it's not closed yet
            for (GenericValue customTimePeriod : customTimePeriods) {
                GenericValue copy = delegator.makeValue(customTimePeriod.getEntityName(), customTimePeriod);
                copy.setString("isClosed", "N");
                copy.setString("organizationPartyId", organizationPartyId);
                copy.put("customTimePeriodId", delegator.getNextSeqId("CustomTimePeriod"));
                copies.add(copy);
            }
            // create new GlAccountOrganization values, but it's important to reset the postedBalances for the new organization
            for (GenericValue glAccountOrganization : glAccountOrganizations) {
                GenericValue copy = delegator.makeValue(glAccountOrganization.getEntityName(), glAccountOrganization);
                copy.setString("organizationPartyId", organizationPartyId);
                copy.set("postedBalance", BigDecimal.ZERO);
                copies.add(copy);
            }
            // copy all the others
            copies.addAll(copyEntitiesWithNewOrganizationPartyId(glAccountTypeDefaults, organizationPartyId));
            copies.addAll(copyEntitiesWithNewOrganizationPartyId(paymentGlAccountTypeMaps, organizationPartyId));
            copies.addAll(copyEntitiesWithNewOrganizationPartyId(paymentMethodTypeGlAccounts, organizationPartyId));
            copies.addAll(copyEntitiesWithNewOrganizationPartyId(creditCardTypeGlAccounts, organizationPartyId));
            copies.addAll(copyEntitiesWithNewOrganizationPartyId(varianceReasonGlAccounts, organizationPartyId));
            copies.addAll(copyEntitiesWithNewOrganizationPartyId(invoiceGlAccountTypes, organizationPartyId));
            copies.addAll(copyEntitiesWithNewOrganizationPartyId(invoiceItemTypeGlAccounts, organizationPartyId));
            copies.addAll(copyEntitiesWithNewOrganizationPartyId(invoiceAdjustmentGlAccounts, organizationPartyId));
            copies.addAll(copyEntitiesWithNewOrganizationPartyId(acctgTagEnumTypes, organizationPartyId));
            copies.addAll(copyEntitiesWithNewOrganizationPartyId(taxAuthorityGlAccounts, organizationPartyId));
            delegator.storeAll(copies);
        } catch (Exception e) {
            throw new ServiceException(e);
        }

    }

    /**
     * Convenience method to copy over a list of entities and substitute the organization party ID.
     * @param entities a <code>List</code> of <code>GenericValue</code>
     * @param newOrganizationPartyId the new organization ID
     * @return the modified list of entities
     * @throws GeneralException if an error occurs
     */
    private List<GenericValue> copyEntitiesWithNewOrganizationPartyId(List<GenericValue> entities, String newOrganizationPartyId) throws GeneralException {
        List<GenericValue> newEntities = new FastList<GenericValue>();
        for (GenericValue entity : entities) {
            GenericValue copy = delegator.makeValue(entity.getEntityName(), entity);
            copy.setString("organizationPartyId", newOrganizationPartyId);
            newEntities.add(copy);
        }
        return newEntities;
    }

    /** {@inheritDoc} */
    public void setOrganizationPartyId(String organizationPartyId) {
        this.organizationPartyId = organizationPartyId;

    }
    
    /** {@inheritDoc} */
    public void setTemplateOrganizationPartyId(
            String templateOrganizationPartyId) {
        this.templateOrganizationPartyId = templateOrganizationPartyId;
    }

}
