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

package com.opensourcestrategies.financials.configuration;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDataSourceException;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * ConfigurationServices - Services for configuring GL Accounts.
 *
 * @author     <a href="mailto:ali@opensourcestrategies.com">Ali Afzal Malik</a>
 * @version    $Rev: 150 $
 * @since      2.2
 */
public final class ConfigurationServices {

    private ConfigurationServices() { }

    private static String MODULE = ConfigurationServices.class.getName();

    /**
     * Removes a GL Account from an organization if it is not associated with any other entity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map removeGlAccountFromOrganization(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String glAccountId = (String) context.get("glAccountId");
        String organizationPartyId = (String) context.get("organizationPartyId");
        List value = null;

        Map fields = UtilMisc.toMap("glAccountId", glAccountId, "organizationPartyId", organizationPartyId);

        try {
            //check for relation with GlAccountTypeDefault
            value = delegator.findByAnd("GlAccountTypeDefault", fields);
            if (!value.isEmpty()) {
                return ServiceUtil.returnError("Could not remove Gl Account from organization because it is associated with an account type through GlAccountTypeDefault.");
            }

            //check for relation with InvoiceItemTypeGlAccount
            value = delegator.findByAnd("InvoiceItemTypeGlAccount", fields);
            if (!value.isEmpty()) {
                return ServiceUtil.returnError("Could not remove Gl Account from organization because it is associated with an invoice item type through InvoiceItemTypeGlAccount.");
            }

            //check for relation with PaymentMethod
            fields = UtilMisc.toMap("glAccountId", glAccountId, "partyId", organizationPartyId);
            value = delegator.findByAnd("PaymentMethod", fields);
            if (!value.isEmpty()) {
                return ServiceUtil.returnError("Could not remove Gl Account from organization because it is associated with a payment method through PaymentMethod.");
            }

            //reset fields
            fields = UtilMisc.toMap("glAccountId", glAccountId, "organizationPartyId", organizationPartyId);

            //check for relation with PaymentMethodTypeGlAccount
            value = delegator.findByAnd("PaymentMethodTypeGlAccount", fields);
            if (!value.isEmpty()) {
                return ServiceUtil.returnError("Could not remove Gl Account from organization because it is associated with a payment method type through PaymentMethodTypeGlAccount.");
            }

            //check for relation with ProductGlAccount
            value = delegator.findByAnd("ProductGlAccount", fields);
            if (!value.isEmpty()) {
                return ServiceUtil.returnError("Could not remove Gl Account from organization because it is associated with a product through ProductGlAccount.");
            }

            //check for relation with VarianceReasonGlAccount
            value = delegator.findByAnd("VarianceReasonGlAccount", fields);
            if (!value.isEmpty()) {
                return ServiceUtil.returnError("Could not remove Gl Account from organization because it is associated with an inventory variance reason through VarianceReasonGlAccount.");
            }

            // make sure the posted balance is null or zero
            GenericValue val = delegator.findByPrimaryKey("GlAccountOrganization", fields);
            if (val.get("postedBalance") != null && val.getDouble("postedBalance") != 0) {
                return ServiceUtil.returnError("Could not remove Gl Account from organization because it has a non zero posted balance.");
            }

            //remove the GL Account by setting the thru date to now date
            Map updateGlAccountOrganizationContext = UtilMisc.toMap("glAccountId", glAccountId, "organizationPartyId", organizationPartyId, "thruDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
            Map updateGlAccountOrganizationResult = dispatcher.runSync("updateGlAccountOrganization", updateGlAccountOrganizationContext);
            if (ServiceUtil.isError(updateGlAccountOrganizationResult)) {
               return updateGlAccountOrganizationResult;
            }

            return ServiceUtil.returnSuccess();
        } catch (GeneralException e) {
            return ServiceUtil.returnError("Could not remove Gl Account from organization (" + e.getMessage() + ").");
        }
    }

    /**
     * Adds a new GL Account and associates it to an Organization if the specified account
     * code is unique i.e. no existing GL Account has the same account code.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map addNewGlAccount(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String glAccountId = null;
        String accountCode = (String) context.get("accountCode");
        String accountName = (String) context.get("accountName");
        String description = (String) context.get("description");
        String glAccountClassTypeKey = (String) context.get("glAccountClassTypeKey");
        String glResourceTypeId = (String) context.get("glResourceTypeId");
        String parentGlAccountId = (String) context.get("parentGlAccountId");
        Double postedBalance = (Double) context.get("postedBalance");
        String organizationPartyId = (String) context.get("organizationPartyId");
        List value = null;

        Map fields = UtilMisc.toMap("accountCode", accountCode);

        try {
            //check whether the account code is already present
            value = delegator.findByAnd("GlAccount", fields);
            if (!value.isEmpty()) {
                return ServiceUtil.returnError("The account code specified [" + accountCode + "] is already associated with an existing Gl Account.");
            } else {
                glAccountId = accountCode;
            }

            // extract glAccountClassId and glAccountTypeId from GlAccountClassType
            GenericValue gv = delegator.findByPrimaryKeyCache("GlAccountClassTypeMap", UtilMisc.toMap("glAccountClassTypeKey", glAccountClassTypeKey));

            String glAccountTypeId = gv.getString("glAccountTypeId");
            String glAccountClassId = gv.getString("glAccountClassId");

            context.put("glAccountTypeId", glAccountTypeId);
            context.put("glAccountClassId", glAccountClassId);

            //Add a new Gl Account
            Map addNewGlAccountContext = UtilMisc.toMap("glAccountId", glAccountId, "accountCode", accountCode, "accountName", accountName, "description", description, "glAccountTypeId", glAccountTypeId, "glAccountClassId", glAccountClassId);
            addNewGlAccountContext.put("glResourceTypeId", glResourceTypeId);
            addNewGlAccountContext.put("parentGlAccountId", parentGlAccountId);
            addNewGlAccountContext.put("postedBalance", postedBalance);
            addNewGlAccountContext.put("userLogin", userLogin);
            Map addNewGlAccountResult = dispatcher.runSync("createGlAccount", addNewGlAccountContext, -1, false);

            if (ServiceUtil.isError(addNewGlAccountResult)) {
                return addNewGlAccountResult;
            }

            // associate it with the organization
            Map addNewGlAccountOrganizationContext = UtilMisc.toMap("glAccountId", glAccountId, "organizationPartyId", organizationPartyId, "postedBalance", postedBalance, "userLogin", userLogin);
            Map addNewGlAccountOrganizationResult = dispatcher.runSync("createGlAccountOrganization", addNewGlAccountOrganizationContext, -1, false);
            if (ServiceUtil.isError(addNewGlAccountOrganizationResult)) {
                return addNewGlAccountOrganizationResult;
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Could not add the Gl Account (" + e.getMessage() + ").");
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError("Could not add the Gl Account (" + e.getMessage() + ").");
        }
    }

    /**
     * Update a GL Account taking as input a GlAccountClassTypeKey.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updateExistingGlAccount(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String glAccountClassTypeKey = (String) context.get("glAccountClassTypeKey");
        context.remove("glAccountClassTypeKey");

        try {
            // extract glAccountClassId and glAccountTypeId from GlAccountClassType
            GenericValue gv = delegator.findByPrimaryKeyCache("GlAccountClassTypeMap", UtilMisc.toMap("glAccountClassTypeKey", glAccountClassTypeKey));

            String glAccountTypeId = gv.getString("glAccountTypeId");
            String glAccountClassId = gv.getString("glAccountClassId");

            context.put("glAccountTypeId", glAccountTypeId);
            context.put("glAccountClassId", glAccountClassId);

            // forward to the original updateGlAccount service
            return dispatcher.runSync("updateGlAccount", context, -1, false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Could not update the Gl Account (" + e.getMessage() + ").");
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError("Could not update the Gl Account (" + e.getMessage() + ").");
        }
    }

    /**
     * Update PartyAcctgPreference for an organization.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updatePartyAcctgPreference(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();
        String partyId = (String) context.get("partyId");

        if (!security.hasEntityPermission("FINANCIALS", "_CONFIG", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorNoPermission", locale));
        }

        try {
            GenericValue partyAcctgPreference = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", partyId));
            if (UtilValidate.isEmpty(partyAcctgPreference)) {
                return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorPartyAcctgPrefNotFound", context, locale));
            }
            partyAcctgPreference.setNonPKFields(context, true);
            partyAcctgPreference.store();

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorUpdatingPartyAcctgPref", context, locale));
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Create or Update a GlAccount record for an organization.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map addGlAccountToOrganization(DispatchContext dctx, Map context) {

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // Mandatory input fields
        String glAccountId = (String) context.get("glAccountId");
        String organizationPartyId = (String) context.get("organizationPartyId");

        try {
            Map input = UtilMisc.toMap("glAccountId", glAccountId, "organizationPartyId", organizationPartyId);
            GenericValue glAccountOrganization = delegator.findByPrimaryKey("GlAccountOrganization", input);
            if (glAccountOrganization == null) {
                return dispatcher.runSync("createGlAccountOrganization", context);
            } else {
                input = UtilMisc.toMap("userLogin", userLogin);
                input.put("glAccountId", glAccountId);
                input.put("organizationPartyId", organizationPartyId);
                input.put("fromDate", UtilDateTime.nowTimestamp());
                input.put("thruDate", null);
                return dispatcher.runSync("updateGlAccountOrganization", input);
            }

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Creates an accounting tag Enumeration record.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createAccountingTag(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();

        try {
            String enumId = (String) context.get("enumId");
            GenericValue enumeration = delegator.makeValue("Enumeration");
            // if an id was given, check for duplicate
            if (UtilValidate.isNotEmpty(enumId)) {
                enumeration.setPKFields(context);
                GenericValue exists = delegator.findByPrimaryKey("Enumeration", enumeration);
                if (exists != null) {
                    return UtilMessage.createAndLogServiceError("An Accounting tag Enumeration already exists with ID [" + enumId + "]", MODULE);
                }
            }
            // else generate the id from the sequence
            enumeration.put("enumId", delegator.getNextSeqId("Enumeration"));

            enumeration.setNonPKFields(context);
            delegator.create(enumeration);
            return ServiceUtil.returnSuccess();

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Updates an accounting tag Enumeration record.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updateAccountingTag(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();

        try {
            GenericValue pk = delegator.makeValue("Enumeration");
            pk.setPKFields(context);
            GenericValue enumeration = delegator.findByPrimaryKey("Enumeration", pk);
            if (enumeration == null) {
                return UtilMessage.createAndLogServiceError("Did not find Accounting tag Enumeration value for PK [" + pk.getPrimaryKey() + "]", MODULE);
            }

            enumeration.setNonPKFields(context);
            delegator.store(enumeration);
            return ServiceUtil.returnSuccess();

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Deletes an accounting tag Enumeration record, this will only works if the tag is not in use in any other entity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map deleteAccountingTag(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        try {
            GenericValue pk = delegator.makeValue("Enumeration");
            pk.setPKFields(context);
            // get the tag, although not really necessary, in case there is a FK error we can use this to display a better error message (fetching after the error won't work because the transaction rolled back)
            GenericValue tag = delegator.findByPrimaryKey("Enumeration", pk.getPrimaryKey());
            try {
                delegator.removeByPrimaryKey(pk.getPrimaryKey());
                return ServiceUtil.returnSuccess();
            } catch (GenericDataSourceException e) {
                // this happens for a FK error
                return UtilMessage.createAndLogServiceError("FinancialsError_CannotDeleteInUseAccoutingTag", UtilMisc.toMap("tag", tag), locale, MODULE);
            }

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Updates or creates an accounting tag usage record for an organization.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updateAccountingTagUsage(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();

        try {
            GenericValue pk = delegator.makeValue("AcctgTagEnumType");
            pk.setPKFields(context);
            GenericValue usage = delegator.findByPrimaryKey("AcctgTagEnumType", pk);
            if (usage == null) {
                usage = pk;
            }
            usage.setNonPKFields(context);
            delegator.createOrStore(usage);
            return ServiceUtil.returnSuccess();

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Updates or creates the accounting tag posting check record for an organization.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updateAccountingTagPostingCheck(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();

        try {
            GenericValue pk = delegator.makeValue("AcctgTagPostingCheck");
            pk.setPKFields(context);
            GenericValue postingCheck = delegator.findByPrimaryKey("AcctgTagPostingCheck", pk);
            if (postingCheck == null) {
                postingCheck = pk;
            }
            postingCheck.setNonPKFields(context);
            delegator.createOrStore(postingCheck);
            return ServiceUtil.returnSuccess();

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }
}
