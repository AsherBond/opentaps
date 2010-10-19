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

package com.opensourcestrategies.financials.util;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;

/**
 * UtilFinancial - Utilities for financials.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 81 $
 * @since      2.2
 */
public final class UtilFinancial {

    private UtilFinancial() { }

    private static String MODULE = UtilFinancial.class.getName();

    public static int decimals = UtilNumber.getBigDecimalScale("fin_arithmetic.properties", "financial.statements.decimals");
    public static int rounding = UtilNumber.getBigDecimalRoundingMode("fin_arithmetic.properties", "financial.statements.rounding");
    public static final String DEFAULT_PRODUCT_ID = "_NA_"; // productId to use in various Maps in case there is no product (ie, bulk items)

    /** InvoiceItemTypes that identify which invoice line items are the base (non-adjustment) product values. */
    public static final List<String> PRODUCT_INVOICE_ITEM_TYPES;
    static {
        List<String> list = FastList.newInstance();
        list.add("INV_PROD_ITEM");
        list.add("INV_DPROD_ITEM");
        list.add("INV_FPROD_ITEM");
        list.add("INV_FDPROD_ITEM");
        list.add("INV_SPROD_ITEM");
        PRODUCT_INVOICE_ITEM_TYPES = list;
    }

    /** Invoice statuses to be considered when computing the billed amount of something. */
    public static final List<String> BILLED_INVOICE_STATUSES;
    static {
        List<String> list = FastList.newInstance();
        list.add("INVOICE_IN_PROCESS");
        list.add("INVOICE_READY");
        list.add("INVOICE_APPROVED");
        list.add("INVOICE_SENT");
        list.add("INVOICE_RECEIVED");
        BILLED_INVOICE_STATUSES = list;
    }

    /**
     * Return either the field productId from the value or the DEFAULT_PRODUCT_ID (usually "_NA_").
     * @param value a <code>GenericValue</code>
     * @return the productId of <code>DEFAULT_PRODUCT_ID</code>
     */
    public static String getProductIdOrDefault(GenericValue value) {
        if (value.getString("productId") != null) {
            return value.getString("productId");
        } else {
            return DEFAULT_PRODUCT_ID;
        }
    }

    /**
     * Get the <code>EntityExpr</code> for finding GL account classes that are a child of the ASSET GL Account class ID.
     * @param delegator a <code>Delegator</code> value
     * @return an <code>EntityExpr</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static EntityExpr getAssetExpr(Delegator delegator) throws GenericEntityException {
        return getGlAccountClassExpr("ASSET", delegator);
    }

    /**
     * Get the <code>EntityExpr</code> for finding GL account classes that are a child of the LIABILITY GL Account class ID.
     * @param delegator a <code>Delegator</code> value
     * @return an <code>EntityExpr</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static EntityExpr getLiabilityExpr(Delegator delegator) throws GenericEntityException {
        return getGlAccountClassExpr("LIABILITY", delegator);
    }

    /**
     * Get the <code>EntityExpr</code> for finding GL account classes that are a child of the EQUITY GL Account class ID.
     * @param delegator a <code>Delegator</code> value
     * @return an <code>EntityExpr</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static EntityExpr getEquityExpr(Delegator delegator) throws GenericEntityException {
        return getGlAccountClassExpr("EQUITY", delegator);
    }

    /**
     * Gets a condition that will constrain paymentTypeId to the set of types which have root parentPaymentTypeId.
     * @param delegator a <code>Delegator</code> value
     * @param parentPaymentTypeId a <code>String</code> value
     * @return an <code>EntityExpr</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static EntityExpr getPaymentTypeExpr(Delegator delegator, String parentPaymentTypeId) throws GenericEntityException {
        return UtilCommon.getEntityChildrenExpr(delegator, "PaymentType", "paymentTypeId", parentPaymentTypeId);
    }


    /**
     * Determines if the party has an active ledger.  Current just uses the Internal Organization role: if the party has this role, we keep a ledger for it.
     * @param delegator a <code>Delegator</code> value
     * @param partyId a <code>String</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static boolean hasActiveLedger(Delegator delegator, String partyId) throws GenericEntityException {
        return UtilValidate.isNotEmpty(delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", "INTERNAL_ORGANIZATIO")));

    }

    /**
     * Gets a condition that will constrain paymentTypeId to the complement set of types which have root parentPaymentTypeId.
     * (E.g., paymentTypeId NOT related to parentPaymentTypeId)
     * @param delegator a <code>Delegator</code> value
     * @param parentPaymentTypeId a <code>String</code> value
     * @return an <code>EntityExpr</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static EntityExpr getPaymentTypeComplementExpr(Delegator delegator, String parentPaymentTypeId) throws GenericEntityException {
        return UtilCommon.getEntityChildrenComplementExpr(delegator, "PaymentType", "paymentTypeId", parentPaymentTypeId);
    }

    /**
     * Gets an entity expression where the field glAccountClassId is a child of the given rootGlAccountClassId.
     * The intent is to assist with building queries such as 'get all accounts of type ASSET'.
     * If the class is not defined, an expression that always evaluates to true is returned instead.
     * Use one of the shortcut methods, like getAssetExpr() instead.
     *
     * Ex. Submitting "DEBIT" results in the expression,
     *     glAccountClassId IN ('DEBIT', 'ASSET', 'DISTRIBUTION', 'EXPENSE', 'INCOME', 'NON_POSTING')
     *
     * @param   rootGlAccountClassId The ancestor class to check that the field glAccountClassId is a member of
     * @param   delegator a <code>Delegator</code> value
     * @return  A suitable EntityExpr for checking that the glAccountClassId field is a member of this tree
     * @throws GenericEntityException if an error occurs
     */
    public static EntityExpr getGlAccountClassExpr(String rootGlAccountClassId, Delegator delegator) throws GenericEntityException {

        // first get the gl class root value
        GenericValue glAccountClass = delegator.findByPrimaryKeyCache("GlAccountClass", UtilMisc.toMap("glAccountClassId", rootGlAccountClassId));
        if (glAccountClass == null) {
            Debug.logWarning("Cannot find GlAccountClass [" + rootGlAccountClassId + "]", MODULE);
            return EntityCondition.makeCondition(new Integer(1), EntityOperator.EQUALS, new Integer(1));
        }

        // recursively build the list of ids that are of this class
        List<String> ids = new ArrayList<String>();
        recurseGetGlAccountClassIds(glAccountClass, ids);

        // make a WHERE glAccountId IN (list of ids) expression
        return EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, ids);
    }

    /**
     * Recursively obtains the IDs of all children of a given glAccountClass.
     * @param glAccountClass a <code>GenericValue</code> value
     * @param ids a List to populate with the children IDs
     * @exception GenericEntityException if an error occurs
     */
    public static void recurseGetGlAccountClassIds(GenericValue glAccountClass, List<String> ids) throws GenericEntityException {
        ids.add(glAccountClass.getString("glAccountClassId"));
        List<GenericValue> children = glAccountClass.getRelatedCache("ChildGlAccountClass");
        for (Iterator<GenericValue> iter = children.iterator(); iter.hasNext();) {
            GenericValue child = iter.next();
            recurseGetGlAccountClassIds(child, ids);
        }
    }

    /**
     * For a Map of <GenericValue glAccount, BigDecimal value>, returns <String glAccountId, BigDecimal value>.
     * @param balances a Map of <GenericValue glAccount, BigDecimal balanceValue>
     * @return a Map of <String glAccountId, BigDecimal balanceValue>
     */
    public static Map<String, BigDecimal> getBalancesByGlAccountId(Map<GenericValue, BigDecimal> balances) {
        Map<String, BigDecimal> balancesByGlAccountId = new HashMap<String, BigDecimal>();
        Set<GenericValue> accounts = balances.keySet();
        for (GenericValue account : accounts) {
            balancesByGlAccountId.put(account.getString("glAccountId"), balances.get(account));
        }
        return balancesByGlAccountId;
    }

    /**
     * Determines conversion factor given a source currencyUomId and the organizationPartyId's accounting preference.
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param currencyUomId a <code>String</code> value
     * @return a <code>double</code> value
     * @exception GenericEntityException if an error occurs
     * @exception GenericServiceException if an error occurs
     */
    public static BigDecimal determineUomConversionFactor(Delegator delegator, LocalDispatcher dispatcher, String organizationPartyId, String currencyUomId) throws GenericEntityException, GenericServiceException {
        return determineUomConversionFactor(delegator, dispatcher, organizationPartyId, currencyUomId, UtilDateTime.nowTimestamp());
    }

    /**
     * Determines conversion factor given a source currencyUomId, the organizationPartyId's accounting preference and a date.
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param currencyUomId a <code>String</code> value
     * @param asOfDate a <code>Timestamp</code> value
     * @return a <code>double</code> value
     * @exception GenericEntityException if an error occurs
     * @exception GenericServiceException if an error occurs
     */
    public static BigDecimal determineUomConversionFactor(Delegator delegator, LocalDispatcher dispatcher, String organizationPartyId, String currencyUomId, Timestamp asOfDate) throws GenericEntityException, GenericServiceException {
        try {
            // default conversion factor
            BigDecimal conversionFactor = BigDecimal.ONE;
            // if currencyUomId is null, return default
            if (currencyUomId == null) {
                return conversionFactor;
            }

            GenericValue org = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", organizationPartyId));
            if (org == null) {
                String msg = "Currency conversion failed: No Party found for organizationPartyId " + organizationPartyId;
                Debug.logError(msg, MODULE);
                throw new GenericServiceException(msg);
            }

            // get our organization's accounting preference
            String baseCurrencyUomId = UtilCommon.getOrgBaseCurrency(organizationPartyId, delegator);
            if (baseCurrencyUomId == null) {
                String msg = "Currency conversion failed: No PartyAcctgPreference entity data for organizationPartyId " + organizationPartyId;
                Debug.logError(msg, MODULE);
                throw new GenericServiceException(msg);
            }

            // if the currencies are equal, return default
            if (currencyUomId.equals(baseCurrencyUomId)) {
                return conversionFactor;
            }

            // this does a currency conversion, based on currencyUomId and the party's accounting preferences.  conversionFactor will be used for postings
            Map<String, Object> tmpResult = dispatcher.runSync("convertUom", UtilMisc.toMap("originalValue", conversionFactor, "uomId", currencyUomId, "uomIdTo", baseCurrencyUomId, "asOfDate", asOfDate));

            if (((String) tmpResult.get(ModelService.RESPONSE_MESSAGE)).equals(ModelService.RESPOND_SUCCESS)) {
                conversionFactor = (BigDecimal) tmpResult.get("convertedValue");
            } else {
                String msg = "Currency conversion failed: No currencyUomId defined in PartyAcctgPreference entity for organizationPartyId " + organizationPartyId;
                Debug.logError(msg, MODULE);
                throw new GenericServiceException(msg);
            }

            Debug.logInfo("currency conversion factor is = " + conversionFactor, MODULE);
            return conversionFactor;

        } catch (GenericEntityException ex) {
            Debug.logError(ex.getMessage(), MODULE);
            throw new GenericEntityException(ex);
        } catch (GenericServiceException ex) {
            Debug.logError(ex.getMessage(), MODULE);
            throw new GenericServiceException(ex);
        }
    }

    /**
     * Returns a list of PaymentMethodTypes suitable for simple customer receipts.
     * The goal of this is to purposely exclude payment methods which are automatically created by the system, such as FIN_ACCOUNT, EXT_BILLACT, or ones without meaning,
     * such as COMPANY_ACCOUNT
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> of <code>PaymentMethodType</code> entities
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getSimpleCustomerPaymentMethodTypes(Delegator delegator) throws GenericEntityException {
        List<String> excludedPaymentMethodTypes = UtilMisc.toList("EXT_BILLACT", "GIFT_CARD", "GIFT_CERTIFICATE", "EXT_WORLDPAY", "FIN_ACCOUNT", "COMPANY_ACCOUNT");
        excludedPaymentMethodTypes.add("EXT_BILL_3RDPTY"); // internal
        excludedPaymentMethodTypes.add("EXT_OFFLINE");     // internal and for orders only, not for payments

        EntityExpr condition = EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.NOT_IN, excludedPaymentMethodTypes);
        return delegator.findByConditionCache("PaymentMethodType", condition, null, null);
    }

    /**
     * See if a party has a role.  TODO: This should be a party util method in our common api.
     * @param partyId a <code>String</code> value
     * @param roleTypeId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static boolean hasPartyRole(String partyId, String roleTypeId, Delegator delegator) throws GenericEntityException {
        GenericValue role = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId));
        return (role != null);
    }

    /**
     * Gets the organizationPartyId's bank settlement account's GL account Id.
     * @param organizationPartyId a <code>String</code> value
     * @param glAccountTypeId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>String</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static String getOrgGlAccountId(String organizationPartyId, String glAccountTypeId, Delegator delegator) throws GenericEntityException {
        String glAccountId = null;

        GenericValue settlementAccount = delegator.findByPrimaryKey("GlAccountTypeDefault", UtilMisc.toMap("organizationPartyId", organizationPartyId,
                    "glAccountTypeId", glAccountTypeId));
        if (settlementAccount != null) {
            glAccountId = settlementAccount.getString("glAccountId");
        }
        return glAccountId;
    }

    /**
     * Given an organizationPartyId and a map where the keys are account types such as ACCOUNTS_RECEIVABLES,
     * returns a new map where the key is replaced with the glAccountId of the account type for the organization.
     * For instance, if the organization's ACCOUNTS_RECEIVABLES is glAccountId 120000, then the
     * map {ACCOUNTS_RECEIVABLES => object} becomes {"120000" => object}.  The values of the keys are preserved.
     * @param organizationPartyId a <code>String</code> value
     * @param accountTypes a <code>Map</code> of account type to something
     * @param delegator a <code>Delegator</code> value
     * @return a <code>Map</code> value
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map replaceGlAccountTypeWithGlAccountForOrg(String organizationPartyId, Map<String, ?> accountTypes, Delegator delegator) throws GeneralException {
        Map accountMap = FastMap.newInstance();
        for (String glAccountTypeId : accountTypes.keySet()) {
            String glAccountId = getOrgGlAccountId(organizationPartyId, glAccountTypeId, delegator);
            if (glAccountId == null) {
                throw new GeneralException("No GL Account found in organization [" + organizationPartyId + "] for account type [" + glAccountTypeId + "]");
            }
            Debug.logInfo("Mapped " + glAccountTypeId + " to GL account " + glAccountId, MODULE);
            accountMap.put(glAccountId, accountTypes.get(glAccountTypeId));
        }
        return accountMap;
    }

    /**
     * Given an organizationPartyId and an account type such as ACCOUNTS_RECEIVABLES,
     * returns the glAccountId of the account type for the organization.
     * For instance, if the organization's ACCOUNTS_RECEIVABLES is glAccountId 120000.
     * @param organizationPartyId a <code>String</code> value
     * @param glAccountTypeId the account type to replace
     * @param delegator a <code>Delegator</code> value
     * @return the GL account ID
     * @exception GeneralException if an error occurs
     */
    public static String replaceGlAccountTypeWithGlAccountForOrg(String organizationPartyId, String glAccountTypeId, Delegator delegator) throws GeneralException {
        String glAccountId = getOrgGlAccountId(organizationPartyId, glAccountTypeId, delegator);
        if (glAccountId == null) {
            throw new GeneralException("No GL Account found in organization [" + organizationPartyId + "] for account type [" + glAccountTypeId + "]");
        }
        Debug.logInfo("Mapped " + glAccountTypeId + " to GL account " + glAccountId, MODULE);
        return glAccountId;
    }

    /**
     * Get the organizationPartyId's main payment method based on its bank settlement account.
     * @param organizationPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return the paymentMethodId of the first <code>PaymentMethod</code> found, or null if none is found
     * @exception GenericEntityException if an error occurs
     */
    public static String getBankSettlementPaymentMethodId(String organizationPartyId, Delegator delegator) throws GenericEntityException {
        String paymentMethodId = null;
        List<GenericValue> paymentMethods = delegator.findByAnd("PaymentMethod", UtilMisc.toList(
                        EntityCondition.makeCondition("glAccountId", EntityOperator.EQUALS, getOrgGlAccountId(organizationPartyId, "BANK_STLMNT_ACCOUNT", delegator)),
                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, organizationPartyId),
                        EntityUtil.getFilterByDateExpr()));
        if (UtilValidate.isNotEmpty(paymentMethods)) {
            paymentMethodId = paymentMethods.get(0).getString("paymentMethodId"); // should be safe - no PaymentMethod should be without a paymentMethodId
        }

        return paymentMethodId;
    }

    /**
     * Parses a timestamp into fields suitable for selection of default values for AM/PM based form widgets
     *
     *  Please note that this routine can also take a time-only string (in the format HH:MM),
     *  under which case the returned date field will be set to null; this can be useful
     *  for setting a default time, without having to specify a default date.
     *
     * @param timestamp a <code>String</code> value
     * @return a <code>Map</code> value
     * @deprecated Use <code>org.opentaps.common.util.UtilDate.timestampToAmPm(String, TimeZone, Locale)</code>
     */
    @SuppressWarnings("unchecked")
    public static Map timestampToAmPm(String timestamp) {
        Map map = FastMap.newInstance();

        int hour, minute;
        String ampm;
        String date = null;

        if (timestamp == null) {
            return map;
        }

        if (timestamp.length() >= 19) {
            date = timestamp.substring(0, 10);
            hour = Integer.valueOf(timestamp.substring(11, 13)).intValue();
            minute = Integer.valueOf(timestamp.substring(14, 16)).intValue();
        } else if (timestamp.length() >= 5) {
            hour = Integer.valueOf(timestamp.substring(0, 2)).intValue();
            minute = Integer.valueOf(timestamp.substring(3, 5)).intValue();
        } else {
            return map;
        }

        if (hour == 0) {
            hour = 12;
            ampm = "AM";
        } else if (hour >= 1 && hour <= 11) {
            ampm = "AM";
        } else if (hour == 12) {
            hour = 12;
            ampm = "PM";
        } else {
            hour = hour - 12;
            ampm = "PM";
        }

        if (date != null) {
            map.put("date", date);
        }
        map.put("hour", new Integer(hour));
        map.put("ampm", ampm);
        map.put("minute", new Integer(minute));

        return map;
    }

    // TODO we need some kind of exception if the invoice type is not supported, and perhaps also a form of this method which accepts invoiceId and delegator
    public static final List<String> INVOICE_TYPES_RECEIVABLE = UtilMisc.toList("SALES_INVOICE", "INTEREST_INVOICE");
    public static boolean isReceivableInvoice(GenericValue invoice) {
        return INVOICE_TYPES_RECEIVABLE.contains(invoice.getString("invoiceTypeId"));
    }

    /**
     * Gets the billing PostalAddress of the given partyId.  Note that for invoices, check for the address in InvoiceContactMech first.
     * If none is found, then fall back to this method.  This method looks for the BILLING_LOCATION then the GENERAL_LOCATION addresses
     * of the party.
     * @param partyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>GenericValue</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static GenericValue getBillingAddress(String partyId, Delegator delegator) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
                EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "POSTAL_ADDRESS"),
                EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.IN, UtilMisc.toList("BILLING_LOCATION", "GENERAL_LOCATION")),
                EntityUtil.getFilterByDateExpr(),
                EntityUtil.getFilterByDateExpr("purposeFromDate", "purposeThruDate")
                );
        List<String> orderBy = UtilMisc.toList("contactMechPurposeTypeId"); // this will cause BILLING_LOCATION to be ordered before GENERAL_LOCATION
        List<GenericValue> partyContactPurposes = delegator.findByCondition("PartyContactDetailByPurpose", conditions, null, orderBy);
        return EntityUtil.getFirst(partyContactPurposes);
    }

    /**
     * Returns whether a Payment's paymentTypeId is a child of PAY_CHECK.
     * @param payment a <code>GenericValue</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static boolean isPaycheck(GenericValue payment) throws GenericEntityException {
        return UtilAccounting.isPaymentType(payment, "PAY_CHECK");
    }

    /**
     * For each transaction entry, determine if the amount to be added needs to have its sign reversed, which is the
     * case if the account and the debit/credit flag are opposite (ie, debit to a credit account or credit to a debit account.)
     * then, add that amount to the glAccountSums map.  Note that the key of glAccountSums is actually the GlAccount entity,
     * not a glAccountId.
     * @param glAccountSums a <code>Map</code> of <code>GlAccount</code> to <code>BigDecimal</code> (sum for this gl account), used as the output of this method
     * @param transactionEntries a <code>List</code> of transaction entries
     * @exception GenericEntityException if an error occurs
     */
    public static void sumBalancesByAccount(Map<GenericValue, BigDecimal> glAccountSums, List<GenericValue> transactionEntries) throws GenericEntityException {
        for (Iterator<GenericValue> tEi = transactionEntries.iterator(); tEi.hasNext();) {
            GenericValue transactionEntry = tEi.next();
            GenericValue account = transactionEntry.getRelatedOne("GlAccount");

            BigDecimal amountToAdd = transactionEntry.getBigDecimal("amount");
            if (glAccountAndTransactionEntryInverted(account, transactionEntry)) {
                amountToAdd = amountToAdd.negate();
            }

            UtilCommon.addInMapOfBigDecimal(glAccountSums, account, amountToAdd);
        }
    }

    /**
     * For each transaction entry, determine if the amount to be added needs to have its sign reversed, which is the
     * case if the account and the debit/credit flag are opposite (ie, debit to a credit account or credit to a debit account.)
     * then, add that amount to the glAccountSums map.  Note that the key of glAccountSums is actually the GlAccount entity,
     * not a glAccountId.
     * This is like {@link #sumBalancesByAccount} but uses three <code>Map</code> for the sum of Balance, Debits and Credits (balance is credit - debit).
     * @param glAccountBalancesSums a <code>Map</code> of <code>GlAccount</code> to <code>BigDecimal</code> (sum the balance for this gl account), used as the output of this method
     * @param glAccountDebitsSums a <code>Map</code> of <code>GlAccount</code> to <code>BigDecimal</code> (sum the debits for this gl account), used as the output of this method
     * @param glAccountCreditsSums a <code>Map</code> of <code>GlAccount</code> to <code>BigDecimal</code> (sum the credits for this gl account), used as the output of this method
     * @param transactionEntries a <code>List</code> of transaction entries
     * @exception GenericEntityException if an error occurs
     */
    public static void sumBalancesByAccountWithDetail(Map<GenericValue, BigDecimal> glAccountBalancesSums, Map<GenericValue, BigDecimal> glAccountDebitsSums, Map<GenericValue, BigDecimal> glAccountCreditsSums, List<GenericValue> transactionEntries) throws GenericEntityException {
        for (Iterator<GenericValue> tEi = transactionEntries.iterator(); tEi.hasNext();) {
            GenericValue transactionEntry = tEi.next();
            GenericValue account = transactionEntry.getRelatedOne("GlAccount");

            BigDecimal amount = transactionEntry.getBigDecimal("amount");
            boolean invert = glAccountAndTransactionEntryInverted(account, transactionEntry);

            // record the amount according to the account credit / debit
            //  for example a Credit to a Debit account is recorded in the Debits total map
            //  and accounted negatively in the account balance total map
            // this way we always have balance = credits - debits
            if (invert) {
                UtilCommon.addInMapOfBigDecimal(glAccountBalancesSums, account, amount.negate());
                UtilCommon.addInMapOfBigDecimal(glAccountDebitsSums, account, amount);
            } else {
                UtilCommon.addInMapOfBigDecimal(glAccountBalancesSums, account, amount);
                UtilCommon.addInMapOfBigDecimal(glAccountCreditsSums, account, amount);
            }

            // check balance = credits - debits
            BigDecimal balance = glAccountBalancesSums.get(account);
            BigDecimal debits = glAccountDebitsSums.get(account);
            BigDecimal credits = glAccountCreditsSums.get(account);
            if (credits == null) {
                credits = BigDecimal.ZERO;
            }
            if (debits == null) {
                debits = BigDecimal.ZERO;
            }
            if (balance == null) {
                balance = BigDecimal.ZERO;
            }


            if (balance.subtract(credits.subtract(debits)).signum() != 0) {
                Debug.logError("sumBalancesByAccountWithDetail: for account [" + account.get("glAccountId") + "] has balance = " + balance + ", expected balance = " + credits.subtract(debits) + ", credits = " + credits + ", debits = " + debits, MODULE);
            }
        }
    }

    /**
     * For each transaction entry, determine if the amount to be added needs to have its sign reversed, which is the
     * case if the account and the debit/credit flag are opposite (ie, debit to a credit account or credit to a debit account.)
     * then, add that amount to the glAccountSums map.
     * @param glAccountSums a <code>Map</code> of GlAccount + Tags to <code>BigDecimal</code> (sum for this gl account), used as the output of this method
     * @param transactionEntries a <code>List</code> of transaction entries
     * @param accountingTags a <code>Map</code> of tags
     * @exception GenericEntityException if an error occurs
     */
    public static void sumBalancesByTag(Map<String, BigDecimal> glAccountSums, List<GenericValue> transactionEntries, Map accountingTags) throws GenericEntityException {
        for (Iterator<GenericValue> tEi = transactionEntries.iterator(); tEi.hasNext();) {
            GenericValue transactionEntry = tEi.next();
            GenericValue account = transactionEntry.getRelatedOne("GlAccount");

            BigDecimal amountToAdd = transactionEntry.getBigDecimal("amount");
            if (glAccountAndTransactionEntryInverted(account, transactionEntry)) {
                amountToAdd = amountToAdd.negate();
            }

            // the key is composite by glAccountId and all tags
            String key = transactionEntry.getString("glAccountId");
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                String tag = (String) accountingTags.get(new Integer(i));
                if (tag != null) {
                    key += "," + (transactionEntry.getString(UtilAccountingTags.ENTITY_TAG_PREFIX + i) == null ? "" : transactionEntry.getString(UtilAccountingTags.ENTITY_TAG_PREFIX + i));
                }
            }
            UtilCommon.addInMapOfBigDecimal(glAccountSums, key, amountToAdd);
        }
    }

    /**
     * Verify that the trial balance from GlAccountOrganization balances on debit and credit sides.
     * @param organizationPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @param decimals an <code>int</code> value
     * @param rounding a <code>RoundingMode</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static boolean isGlAccountOrganizationInBalance(String organizationPartyId, Delegator delegator, int decimals, RoundingMode rounding) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("postedBalance", EntityOperator.NOT_EQUAL, null));
        List<GenericValue> trialBalances = delegator.findByCondition("GlAccountOrganization", conditions, UtilMisc.toList("glAccountId", "postedBalance"), UtilMisc.toList("glAccountId"));
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        // go through and add up the debit and credit totals
        for (GenericValue accountBalance : trialBalances) {
            GenericValue glAccount = accountBalance.getRelatedOneCache("GlAccount");
            if (UtilAccounting.isDebitAccount(glAccount)) {
                debitTotal = debitTotal.add(accountBalance.getBigDecimal("postedBalance")).setScale(decimals + 1, rounding);
                Debug.logVerbose("[" + glAccount.get("glAccountId") + "] is a debit account, so added [" + accountBalance.get("postedBalance") + "] and the debit total is now [" + debitTotal + "]", MODULE);
            } else {
                creditTotal = creditTotal.add(accountBalance.getBigDecimal("postedBalance")).setScale(decimals + 1, rounding);
                Debug.logVerbose("[" + glAccount.get("glAccountId") + "] is a credit account, so added [" + accountBalance.get("postedBalance") + "] and the credit total is now [" + creditTotal + "]", MODULE);
            }
        }

        // now round down to the specificed decimal places
        debitTotal = debitTotal.setScale(decimals, rounding);
        creditTotal = creditTotal.setScale(decimals, rounding);
        if (debitTotal.compareTo(creditTotal) == 0) {
            return true;
        } else {
            Debug.logError("GlAccountOrganization balance for [" + organizationPartyId + "] do not equal: debit total is [" + debitTotal + "] and credit total is [" + creditTotal + "]", MODULE);
            return false;
        }
    }

    /**
     * Verify that the trial balance stored in GlAccountHistory balances on debit and credit sides for a given time period.
     * @param organizationPartyId a <code>String</code> value
     * @param customTimePeriodId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @param decimals an <code>int</code> value
     * @param rounding a <code>RoundingMode</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static boolean isGlAccountHistoryInBalance(String organizationPartyId, String customTimePeriodId, Delegator delegator, int decimals, RoundingMode rounding) throws GenericEntityException {
        GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
        List<GenericValue> glAccountHistories = delegator.findByAnd("GlAccountHistory", UtilMisc.toMap("organizationPartyId", organizationPartyId, "customTimePeriodId", customTimePeriodId));

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        for (GenericValue accountHistory : glAccountHistories) {
            GenericValue glAccount = accountHistory.getRelatedOneCache("GlAccount");
            Debug.logVerbose("account history = " + accountHistory, MODULE);
            if ("Y".equals(timePeriod.getString("isClosed"))) {
                // if closed, then use the endingBalance and add it to the debit or credit total as needed
                if (UtilAccounting.isDebitAccount(glAccount)) {
                    debitTotal = debitTotal.add(accountHistory.getBigDecimal("endingBalance")).setScale(decimals + 1, rounding);
                    Debug.logVerbose("[" + glAccount.get("glAccountId") + "] is a debit account, so added [" + accountHistory.get("endingBalance") + "] and the debit total is now [" + debitTotal + "]", MODULE);
                } else {
                    creditTotal = creditTotal.add(accountHistory.getBigDecimal("endingBalance")).setScale(decimals + 1, rounding);
                    Debug.logVerbose("[" + glAccount.get("glAccountId") + "] is a not debit account, so added [" + accountHistory.get("endingBalance") + "] and the credit total is now [" + creditTotal + "]", MODULE);
                }
            } else {
                // otherwise, just add the posted debits and credits
                debitTotal = debitTotal.add(accountHistory.getBigDecimal("postedDebits")).setScale(decimals + 1, rounding);
                creditTotal = creditTotal.add(accountHistory.getBigDecimal("postedCredits")).setScale(decimals + 1, rounding);
            }
        }

        // now round down to the specificed decimal places
        debitTotal = debitTotal.setScale(decimals, rounding);
        creditTotal = creditTotal.setScale(decimals, rounding);
        if (debitTotal.compareTo(creditTotal) == 0) {
            return true;
        } else {
            Debug.logError("GlAccountHistory balance for [" + organizationPartyId + "] and time period [" + customTimePeriodId + "] do not equal: debit total is [" + debitTotal + "] and credit total is [" + creditTotal + "]", MODULE);
            return false;
        }
    }

    /**
     * Verify that all trial balances for GlAccountOrganization and GlAccountHistory of all time periods equal.
     * @param organizationPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @param decimals an <code>int</code> value
     * @param rounding a <code>RoundingMode</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static boolean areAllTrialBalancesEqual(String organizationPartyId, Delegator delegator, int decimals, RoundingMode rounding) throws GenericEntityException {
        // first check GlAccountOrganization.  If it's not in balance, then return false
        if (!isGlAccountOrganizationInBalance(organizationPartyId, delegator, decimals, rounding)) {
            return false;
        }

        // then check if all timem periods are in balance and return false if any one of them is not
        List<GenericValue> timePeriods = delegator.findByAndCache("CustomTimePeriod", UtilMisc.toMap("organizationPartyId", organizationPartyId));
        for (GenericValue timePeriod : timePeriods) {
            if (!isGlAccountHistoryInBalance(organizationPartyId, timePeriod.getString("customTimePeriodId"), delegator, decimals, rounding)) {
                return false;
            }
        }

        // at this point, every balance checked out, so return true
        return true;
    }

    /**
     * Get the timestamp of the last posted transaction's transactionDate for the organization, or null if there are no such transactions.
     * @param organizationPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>Timestamp</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static Timestamp getTransactionDateForLastPostedTransaction(String organizationPartyId, Delegator delegator) throws GenericEntityException {
        List<GenericValue> transactions = delegator.findByAnd("AcctgTransAndEntriesPostedTransDate", UtilMisc.toMap("organizationPartyId", organizationPartyId, "isPosted", "Y"));
        if (UtilValidate.isEmpty(transactions)) {
            return null;
        } else {
            GenericValue transaction = EntityUtil.getFirst(transactions);
            return transaction.getTimestamp("transactionDate");
        }
    }

    /**
     * Checks the transaction entry debit / credit flag and compares it to the GLAccount credit / debit.
     *
     * @param glAccount a <code>GenericValue</code> value
     * @param transactionEntry a <code>GenericValue</code> value
     * @return <code>true</code> if the transaction entry and GLAccount debit / credit flags are inversed
     * @exception GenericEntityException if an error occurs
     */
    public static boolean glAccountAndTransactionEntryInverted(GenericValue glAccount, GenericValue transactionEntry) throws GenericEntityException {
        return ((UtilAccounting.isDebitAccount(glAccount) && "C".equals(transactionEntry.get("debitCreditFlag")))
                || (UtilAccounting.isCreditAccount(glAccount) && "D".equals(transactionEntry.get("debitCreditFlag"))));
    }
}
