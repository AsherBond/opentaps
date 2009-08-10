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
package org.opentaps.tests.financials;

import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastList;
import javolution.util.FastMap;
import junit.framework.TestCase;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.financials.domain.billing.invoice.InvoiceSpecification;
import org.opentaps.tests.OpentapsTestCase;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * Delegation object that holds assert() methods and util methods for
 * dealing with ledgers.
 *
 * The reason for this pattern is that the assert() and util methods
 * have asserts of their own and must extend OpentapsTestCase and
 * be able to interact with the test suite.
 */
public class FinancialAsserts extends OpentapsTestCase {

    protected String organizationPartyId;
    protected GenericValue userLogin;

    /**
     * Creates a new <code>FinancialAsserts</code> instance.
     *
     * @param parent an <code>OpentapsTestCase</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @exception GenericEntityException if an error occurs
     */
    public FinancialAsserts(OpentapsTestCase parent, String organizationPartyId, GenericValue userLogin) throws GenericEntityException {
        this.delegator = parent.getDelegator();
        this.dispatcher = parent.getDispatcher();
        this.organizationPartyId = organizationPartyId;
        this.userLogin = userLogin;
    }


    /*************************************************************************/
    /***                                                                   ***/
    /***                        Helper Functions                           ***/
    /***                                                                   ***/
    /*************************************************************************/


    /**
     * Same as below, with tags = null and checkBalanceSheet = true
     * @param asOfDate the date to get the balance for
     * @return the financial balance <code>Map</code> of glAccountId => balance
     */
    public Map<String, Number> getFinancialBalances(Timestamp asOfDate) {
        return getFinancialBalances(asOfDate, null, true);
    }


    /**
     * Same as below, with checkBalanceSheet = true
     * @param asOfDate
     * @param tags
     * @return
     */
    public Map<String, Number> getFinancialBalances(Timestamp asOfDate, Map tags) {
        return getFinancialBalances(asOfDate, tags, true);
    }

    /**
     * Gets the balances for income statement and balance sheet accounts as a map
     * of glAccountId -> balance.  The asset account balances are assumed positive,
     * but the liability and equity account balances are multiplied by -1.
     * All income statement accounts are returned at their actual value.
     *
     * As a bonus, this method can also check if the balance sheet satisfies the
     * basic accounting equation,
     *
     *  Assets = Liabilities + Equity.
     *
     * However, this check may not balance for some combinations of accounting tags, so it can be turned off as well
     * @param asOfDate the date to get the balance for
     * @param tags optional accounting tag to use when getting the balances, as a Map of tag1 -> value1, tag2 -> value2, ...
     * @param checkBalanceSheet checks if balance sheet balances
     * @return the financial balance <code>Map</code> of glAccountId => balance
     */
    @SuppressWarnings("unchecked")
    public Map<String, Number> getFinancialBalances(Timestamp asOfDate, Map tags, boolean checkBalanceSheet) {
        Timestamp fromDate = getLastClosedDate();
        Map<String, Number> balances = FastMap.newInstance();

        // run the income statement, which returns the income statement accounts and their sums
        Map input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "fromDate", fromDate, "thruDate", asOfDate, "userLogin", userLogin, "glFiscalTypeId", "ACTUAL");
        if (tags != null) {
            input.putAll(tags);
        }
        Map results = runAndAssertServiceSuccess("getIncomeStatementAccountSumsByDate", input, -1, false);
        Map<GenericValue, Number> incomeStatement = (Map) results.get("glAccountSums");
        for (GenericValue account : incomeStatement.keySet()) {
            BigDecimal balance = (UtilCommon.asBigDecimal(incomeStatement.get(account))).setScale(DECIMALS, ROUNDING);
            balances.put(account.getString("glAccountId"), balance);
        }

        // run the balance sheet and get the assets, liabilities and equity account sums
        input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "asOfDate", asOfDate, "userLogin", userLogin);
        if (tags != null) {
            input.putAll(tags);
        }
        results = runAndAssertServiceSuccess("getBalanceSheetForDate", input, -1, false);
        Map<GenericValue, Number> assetAccountBalances = (Map) results.get("assetAccountBalances");
        Map<GenericValue, Number> liabilityAccountBalances = (Map) results.get("liabilityAccountBalances");
        Map<GenericValue, Number> equityAccountBalances = (Map) results.get("equityAccountBalances");

        // Assets = Liabilities + Equity
        if (checkBalanceSheet) {
            BigDecimal assetSum = (UtilCommon.mapSum(assetAccountBalances)).setScale(DECIMALS, ROUNDING);
            BigDecimal liabilitySum = (UtilCommon.mapSum(liabilityAccountBalances)).setScale(DECIMALS, ROUNDING);
            BigDecimal equitySum = (UtilCommon.mapSum(equityAccountBalances)).setScale(DECIMALS, ROUNDING);
            assertTrue("Balance Sheet must be balanced:  Assets [" + assetSum + "] = Liabilities [" + liabilitySum + "] + Equity [" + equitySum + "]", assetSum.compareTo(liabilitySum.add(equitySum)) == 0);
        }

        // merge everything together
        for (GenericValue glAccount : assetAccountBalances.keySet()) {
            String glAccountId = glAccount.getString("glAccountId");
            if (balances.get(glAccountId) != null) {
                TestCase.fail("GL Account [" + glAccountId + "] is both an asset account and an income statement account.");
            }
            BigDecimal balance = (UtilCommon.asBigDecimal(assetAccountBalances.get(glAccount))).setScale(DECIMALS, ROUNDING);
            balances.put(glAccountId, balance);
        }
        for (GenericValue glAccount : liabilityAccountBalances.keySet()) {
            String glAccountId = glAccount.getString("glAccountId");
            if (balances.get(glAccountId) != null) {
                TestCase.fail("GL Account [" + glAccountId + "] is both a liability account and an income statement account.");
            }
            BigDecimal balance = (UtilCommon.asBigDecimal(liabilityAccountBalances.get(glAccount))).setScale(DECIMALS, ROUNDING);
            balance = balance.negate().setScale(DECIMALS, ROUNDING);
            balances.put(glAccountId, balance);
        }
        for (GenericValue glAccount : equityAccountBalances.keySet()) {
            String glAccountId = glAccount.getString("glAccountId");
            if (balances.get(glAccountId) != null) {
                TestCase.fail("GL Account [" + glAccountId + "] is both an equity account and an income statement account.");
            }
            BigDecimal balance = UtilCommon.asBigDecimal(equityAccountBalances.get(glAccount));
            balance = balance.negate().setScale(DECIMALS, ROUNDING);
            balances.put(glAccountId, balance);
        }

        return balances;
    }

    /**
     * TODO: For now we're running the tests from an arbitrary fixed point in the past. This should really get the date from the last closed time period.
     * @return a <code>Timestamp</code> value
     */
    public Timestamp getLastClosedDate() {
        return Timestamp.valueOf("2007-01-01 00:00:00.00");
    }

    /**
     * Gets the glAccountId for the organizationParty from the GlAccountTypeDefault for INVENTORY_ACCOUNT.
     * @return glAccountId
     */
    public String getInventoryGlAccountId() {
        String glAccountId = null;
        try {
            glAccountId = UtilFinancial.getOrgGlAccountId(organizationPartyId, "INVENTORY_ACCOUNT", delegator);
        } catch (GenericEntityException e) {
            TestCase.fail("Error retrieving glAccountId for INVENTORY_ACCOUNT for organizationPartyId " + organizationPartyId + ": " + e.getMessage());
        }
        if (glAccountId == null) {
            TestCase.fail("organizationPartyId " + organizationPartyId + "has no GlAccountTypeDefault configured for glAccountTypeId INVENTORY_ACCOUNT");
        }
        return glAccountId;
    }

    /**
     * Gets the glAccountId for INVENTORY_VAL_ADJ and the current organization.
     * @return the INVENTORY_VAL_ADJ glAccountId
     */
    public String getInventoryValAdjGlAccountId() {
        String glAccountId = null;
        try {
            glAccountId = UtilFinancial.getOrgGlAccountId(organizationPartyId, "INVENTORY_VAL_ADJ", delegator);
        } catch (GenericEntityException e) {
            TestCase.fail("Error retrieving glAccountId for INVENTORY_VAL_ADJ for organizationPartyId " + organizationPartyId + ": " + e.getMessage());
        }
        if (glAccountId == null) {
            TestCase.fail("organizationPartyId " + organizationPartyId + "has no GlAccountTypeDefault configured for glAccountTypeId INVENTORY_VAL_ADJ");
        }
        return glAccountId;
    }

    /**
     * Creates Invoice and InvoiceItem.
     *
     * @param partyIdTo is the party to bill to the invoice
     * @param invoiceTypeId is the type of the invoice
     * @return invoiceId created
     */
    public String createInvoice(String partyIdTo, String invoiceTypeId) {
        return createInvoice(partyIdTo, invoiceTypeId, UtilDateTime.nowTimestamp(), null, null, null, null);
    }

    /**
     * Creates Invoice and InvoiceItem.
     *
     * @param partyIdTo is the party to bill to the invoice
     * @param invoiceTypeId is the type of the invoice
     * @param invoiceDate is the invoiceDate to use. If none is given, set date to now.
     * @return invoiceId created
     */
    public String createInvoice(String partyIdTo, String invoiceTypeId, Timestamp invoiceDate) {
        return createInvoice(partyIdTo, invoiceTypeId, invoiceDate, null, null, null, null);
    }

    /**
     * Creates Invoice and InvoiceItem.
     *
     * @param partyIdTo is the party to bill to the invoice
     * @param invoiceTypeId is the type of the invoice
     * @param invoiceDate is the invoiceDate to use. If none is given, set date to now.
     * @param message to set as invoice description, reference and message
     * @return invoiceId created
     */
    public String createInvoice(String partyIdTo, String invoiceTypeId, Timestamp invoiceDate, String message) {
        return createInvoice(partyIdTo, invoiceTypeId, invoiceDate, null, message, message, message);
    }

    /**
     * Creates Invoice and InvoiceItem.
     *
     * @param partyIdTo is the party to bill to the invoice
     * @param invoiceTypeId is the type of the invoice
     * @param invoiceDate is the invoiceDate to use. If none is given, set date to now.
     * @param invoiceMessage is the invoice message
     * @param referenceNumber is the invoice reference
     * @param description is the invoice description
     * @return invoiceId created
     */
    public String createInvoice(String partyIdTo, String invoiceTypeId, Timestamp invoiceDate, String invoiceMessage, String referenceNumber, String description) {
        return createInvoice(partyIdTo, invoiceTypeId, invoiceDate, null, invoiceMessage, referenceNumber, description);
    }

    /**
     * Creates Invoice and InvoiceItem.
     *
     * @param partyIdTo is the party to bill to the invoice
     * @param invoiceTypeId is the type of the invoice
     * @param invoiceDate is the invoiceDate to use. If none is given, set date to now.
     * @param dueDate is the due date to use
     * @return invoiceId created
     */
    public String createInvoice(String partyIdTo, String invoiceTypeId, Timestamp invoiceDate, Timestamp dueDate) {
        return createInvoice(partyIdTo, invoiceTypeId, invoiceDate, dueDate, null, null, null);
    }

    /**
     * Creates Invoice and InvoiceItem.
     *
     * @param partyIdTo is the party to bill to the invoice
     * @param invoiceTypeId is the type of the invoice
     * @param invoiceDate is the invoiceDate to use. If none is given, set date to now.
     * @param dueDate is the due date to use.
     * @param invoiceMessage is the invoice message
     * @param referenceNumber is the invoice reference
     * @param description is the invoice description
     * @return invoiceId created
     */
    @SuppressWarnings("unchecked")
    public String createInvoice(String partyIdTo, String invoiceTypeId, Timestamp invoiceDate, Timestamp dueDate, String invoiceMessage, String referenceNumber, String description) {
        Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin);
        input.put("invoiceTypeId", invoiceTypeId);
        input.put("statusId", "INVOICE_IN_PROCESS");
        if (InvoiceSpecification.InvoiceTypeEnum.isReceivable(invoiceTypeId)) {
            input.put("partyIdFrom", organizationPartyId);
            input.put("partyId", partyIdTo);
        } else if (InvoiceSpecification.InvoiceTypeEnum.isPayable(invoiceTypeId)) {
            input.put("partyIdFrom", partyIdTo);
            input.put("partyId", organizationPartyId);
        } else {
            TestCase.fail("Cannot create invoice for type [" + invoiceTypeId + "].  Unsupported invoice type.");
        }
        input.put("currencyUomId", "USD");
        input.put("invoiceDate", invoiceDate);
        input.put("dueDate", dueDate);
        input.put("invoiceMessage", invoiceMessage);
        input.put("referenceNumber", referenceNumber);
        input.put("description", description);
        Map<String, String> output = runAndAssertServiceSuccess("createInvoice", input);

        return output.get("invoiceId");
    }

    /**
     * Adds an Invoice Item to an Invoice.
     *
     * @param invoiceId of the invoice to add to
     * @param invoiceItemTypeId is the type of the invoice item
     * @param quantity to add on the invoice
     * @param amount price of the invoice item
     */
    public void createInvoiceItem(String invoiceId, String invoiceItemTypeId, double quantity, double amount) {
        createInvoiceItem(invoiceId, invoiceItemTypeId, null, new Double(quantity), new Double(amount), null);
    }

    /**
     * Adds an Invoice Item to an Invoice.
     *
     * @param invoiceId of the invoice to add to
     * @param invoiceItemTypeId is the type of the invoice item
     * @param quantity to add on the invoice
     * @param amount price of the invoice item
     * @param description the invoice item description
     */
    public void createInvoiceItem(String invoiceId, String invoiceItemTypeId, double quantity, double amount, String description) {
        createInvoiceItem(invoiceId, invoiceItemTypeId, null, new Double(quantity), new Double(amount), description);
    }

    /**
     * Adds an Invoice Item to an Invoice.
     *
     * @param invoiceId of the invoice to add to
     * @param invoiceItemTypeId is the type of the invoice item
     * @param productId the invoice item product
     * @param quantity to add on the invoice
     * @param amount price of the invoice item
     */
    public void createInvoiceItem(String invoiceId, String invoiceItemTypeId, String productId, double quantity, double amount) {
        createInvoiceItem(invoiceId, invoiceItemTypeId, productId, new Double(quantity), new Double(amount), null);
    }

    /**
     * Adds an Invoice Item to an Invoice.
     *
     * @param invoiceId of the invoice to add to
     * @param invoiceItemTypeId is the type of the invoice item
     * @param productId the invoice item product
     * @param quantity to add on the invoice
     * @param amount price of the invoice item
     */
    public void createInvoiceItem(String invoiceId, String invoiceItemTypeId, String productId, Double quantity, Double amount) {
        createInvoiceItem(invoiceId, invoiceItemTypeId, productId, quantity, amount, null);
    }

    /**
     * Adds an Invoice Item to an Invoice.
     *
     * @param invoiceId of the invoice to add to
     * @param invoiceItemTypeId is the type of the invoice item
     * @param productId the invoice item product
     * @param quantity to add on the invoice
     * @param amount price of the invoice item
     * @param description the invoice item description
     */
    public void createInvoiceItem(String invoiceId, String invoiceItemTypeId, String productId, double quantity, double amount, String description) {
        createInvoiceItem(invoiceId, invoiceItemTypeId, productId, new Double(quantity), new Double(amount), description);
    }

    /**
     * Adds an Invoice Item to an Invoice.
     *
     * @param invoiceId of the invoice to add to
     * @param invoiceItemTypeId is the type of the invoice item
     * @param productId the invoice item product
     * @param quantity to add on the invoice
     * @param amount price of the invoice item
     * @param description the invoice item description
     */
    @SuppressWarnings("unchecked")
    public void createInvoiceItem(String invoiceId, String invoiceItemTypeId, String productId, Double quantity, Double amount, String description) {
        createInvoiceItem(invoiceId, invoiceItemTypeId, productId, quantity, amount, description, null);
    }

    /**
     * Creates an invoice item with optional accountingTags
     * @param invoiceId
     * @param invoiceItemTypeId
     * @param productId
     * @param quantity
     * @param amount
     * @param description
     * @param accountingTags
     */
    public void createInvoiceItem(String invoiceId, String invoiceItemTypeId, String productId, Double quantity, Double amount, String description, Map accountingTags) {
        Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin);
        input.put("invoiceId", invoiceId);
        input.put("invoiceItemTypeId", invoiceItemTypeId);
        input.put("productId", productId);
        input.put("uomId", "USD");
        input.put("taxableFlag", "N");
        input.put("quantity", quantity);
        input.put("amount", amount);
        input.put("description", description);
        if (accountingTags != null) {
            input.putAll(accountingTags);
        }
        runAndAssertServiceSuccess("createInvoiceItem", input);
    }

    
    /**
     * Update an invoice item with optional accountingTags
     * @param invoiceId
     * @param invoiceItemTypeId
     * @param productId
     * @param quantity
     * @param amount
     * @param description
     * @param accountingTags
     */
    public void updateInvoiceItem(String invoiceId, String invoiceItemSeqId, String invoiceItemTypeId, String productId, Double quantity, Double amount, String description, Map accountingTags) {
        Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin);
        input.put("invoiceId", invoiceId);
        input.put("invoiceItemSeqId", invoiceItemSeqId);
        input.put("invoiceItemTypeId", invoiceItemTypeId);
        input.put("productId", productId);
        input.put("uomId", "USD");
        input.put("taxableFlag", "N");
        input.put("quantity", quantity);
        input.put("amount", amount);
        input.put("description", description);
        if (accountingTags != null) {
            input.putAll(accountingTags);
        }
        runAndAssertServiceSuccess("updateInvoiceItem", input);
    }
    
    /**
     * Updates Invoice status.
     *
     * @param invoiceId the invoice to update
     * @param invoiceStatus the new status
     */
    public void updateInvoiceStatus(String invoiceId, String invoiceStatus) {
        updateInvoiceStatus(invoiceId, invoiceStatus, UtilDateTime.nowTimestamp());
    }

    /**
     * Updates Invoice status.
     *
     * @param invoiceId the invoice to update
     * @param invoiceStatus the new status
     * @param invoiceDate is the invoiceDate to use. If none is given, set date to now.
     */
    @SuppressWarnings("unchecked")
    public void updateInvoiceStatus(String invoiceId, String invoiceStatus, Timestamp invoiceDate) {
        // set invoice status
        Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin);
        input.put("invoiceId", invoiceId);
        if ("INVOICE_VOIDED".equals(invoiceStatus)) {
            // voiding invoice requires a special service call since there is no status valid change for it by design
            runAndAssertServiceSuccess("opentaps.voidInvoice", input);
        } else {
            input.put("statusId", invoiceStatus);
            input.put("statusDate", invoiceDate);
            runAndAssertServiceSuccess("setInvoiceStatus", input);
        }
    }

    /**
     * Creates a Payment.
     *
     * @param amount the Payment amount
     * @param partyIdFrom the Payment party from
     * @param paymentTypeId the Payment type
     * @param paymentMethodTypeId the Payment method type, optional
     * @return the created payment ID
     */
    public String createPayment(double amount, String partyIdFrom, String paymentTypeId, String paymentMethodTypeId) {
        return createPaymentAndApplication(amount, partyIdFrom, organizationPartyId, paymentTypeId, paymentMethodTypeId, null, null, null);
    }

    /**
     * Creates a Payment.
     *
     * @param amount the Payment and PaymentApplication amount
     * @param partyIdFrom the Payment party from
     * @param paymentTypeId the Payment type
     * @param paymentMethodTypeId the Payment method type, optional
     * @param endStatus is the status to update the payment after creation
     * @return the created payment ID
     */
    public String createPayment(double amount, String partyIdFrom, String paymentTypeId, String paymentMethodTypeId, String endStatus) {
        return createPaymentAndApplication(amount, partyIdFrom, organizationPartyId, paymentTypeId, paymentMethodTypeId, null, null, endStatus);
    }

    public String createPayment(double amount, String partyIdFrom, String paymentTypeId, String paymentMethodTypeId, String endStatus, Map accountingTags) {
        return createPaymentAndApplication(amount, partyIdFrom, organizationPartyId, paymentTypeId, paymentMethodTypeId, null, null, endStatus, accountingTags);
    }

    /**
     * Creates a Payment and PaymentApplication.
     *
     * @param amount the Payment and PaymentApplication amount
     * @param partyIdFrom the Payment party from
     * @param partyIdTo the Payment party to, if <code>null</code> uses the current organization
     * @param paymentTypeId the Payment type
     * @param paymentMethodTypeId the Payment method type, optional
     * @param paymentMethodId the Payment method, optional
     * @param invoiceId the Invoice to apply the Payment to, if <code>null</code> no PaymentApplication is created
     * @param endStatus is the status to update the payment after creation
     * @return the created payment ID
     */
    @SuppressWarnings("unchecked")
    public String createPaymentAndApplication(double amount, String partyIdFrom, String partyIdTo, String paymentTypeId, String paymentMethodTypeId, String paymentMethodId, String invoiceId, String endStatus) {
        return createPaymentAndApplication(amount, partyIdFrom, partyIdTo, paymentTypeId, paymentMethodTypeId, paymentMethodId, invoiceId, endStatus, null);
    }

    /**
     * Creates a Payment and PaymentApplication, with accountingTags for the Payment.
     * @param amount
     * @param partyIdFrom
     * @param partyIdTo
     * @param paymentTypeId
     * @param paymentMethodTypeId
     * @param paymentMethodId
     * @param invoiceId
     * @param endStatus
     * @param accountingTags
     * @return
     */
    public String createPaymentAndApplication(double amount, String partyIdFrom, String partyIdTo, String paymentTypeId, String paymentMethodTypeId, String paymentMethodId, String invoiceId, String endStatus, Map accountingTags) {
        Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin);
        input.put("amount", new Double(amount));
        input.put("currencyUomId", "USD");
        input.put("partyIdFrom", partyIdFrom);
        if (UtilValidate.isEmpty(partyIdTo)) {
            input.put("partyIdTo", organizationPartyId);
        } else {
            input.put("partyIdTo", partyIdTo);
        }
        input.put("paymentTypeId", paymentTypeId);
        if (UtilValidate.isNotEmpty(paymentMethodTypeId)) {
            input.put("paymentMethodTypeId", paymentMethodTypeId);
        }
        if (UtilValidate.isNotEmpty(paymentMethodId)) {
            input.put("paymentMethodId", paymentMethodId);
        }
        input.put("statusId", "PMNT_NOT_PAID");
        if (accountingTags != null) {
            input.putAll(accountingTags);
        }
        Map<String, String> output = runAndAssertServiceSuccess("financials.createPayment", input);
        String paymentId = output.get("paymentId");

        if (UtilValidate.isNotEmpty(invoiceId)) {
            input = UtilMisc.toMap("userLogin", userLogin);
            input.put("paymentId", paymentId);
            input.put("invoiceId", invoiceId);
            input.put("amountApplied", new Double(amount));
            runAndAssertServiceSuccess("createPaymentApplication", input);
        }

        if (UtilValidate.isNotEmpty(endStatus)) {
            updatePaymentStatus(paymentId, endStatus);
        }

        return paymentId;
    }

    /**
     * Updates a Payment status.
     *
     * @param paymentId the Payment to update
     * @param paymentStatus the Payment status to set
     */
    @SuppressWarnings("unchecked")
    public void updatePaymentStatus(String paymentId, String paymentStatus) {
        // set invoice status
        Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin);
        input.put("paymentId", paymentId);
        input.put("statusId", paymentStatus);
        runAndAssertServiceSuccess("setPaymentStatus", input);
    }

    /**
     * Tests a Payment status.
     * @param paymentId the Payment to check
     * @param statusId the Payment status to assert
     */
    public void assertPaymentStatus(String paymentId, String statusId) {
        try {
            GenericValue payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            if (payment == null) {
                TestCase.fail("Payment [" + paymentId + "] not found.");
            }
            assertEquals("Unexpected Payment status for payment [" + paymentId + "]", statusId, payment.get("statusId"));
        } catch (GenericEntityException e) {
            TestCase.fail("Error retrieving Payment [" + paymentId + "] : " + e.getMessage());
        }
    }

    /**
     * Tests an Invoice status.
     * @param invoiceId the Invoice to check
     * @param statusId the Invoice status to assert
     */
    public void assertInvoiceStatus(String invoiceId, String statusId) {
        try {
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            if (invoice == null) {
                TestCase.fail("Invoice [" + invoiceId + "] not found.");
            }
            assertEquals("Unexpected Invoice status for invoice [" + invoiceId + "]", statusId, invoice.get("statusId"));
        } catch (GenericEntityException e) {
            TestCase.fail("Error retrieving Invoice [" + invoiceId + "] : " + e.getMessage());
        }
    }

    public void assertFinanceCharges(Map<Invoice, Map<String, BigDecimal>> financeCharges, Map<String, BigDecimal> expectedFinanceCharges) {
        int size = expectedFinanceCharges.size();
        Boolean[] inv = new Boolean[size];

        for (int i = 0; i < size; i++) {
            inv[i] = new Boolean(false);
        }

        for (Invoice invoice : financeCharges.keySet()) {
            int i = 0;
            for (String expectedInvoiceId : expectedFinanceCharges.keySet()) {
                if (expectedInvoiceId.equals(invoice.getInvoiceId())) {
                    Map<String, BigDecimal> financeCharge = financeCharges.get(invoice);
                    assertEquals("invoice [" + expectedInvoiceId + "] basic interest calculations", expectedFinanceCharges.get(expectedInvoiceId), financeCharge.get("interestAmount"));
                    inv[i] = new Boolean(true);
                }
                i++;
            }
        }

        int i = 0;
        for (String expectedInvoiceId : expectedFinanceCharges.keySet()) {
            if (expectedFinanceCharges.get(expectedInvoiceId) != null) {
                assertTrue("invoice [" + expectedInvoiceId + "] basic interest calculations was not verified", inv[i]);
            } else {
                assertFalse("invoice [" + expectedInvoiceId + "] should be null", inv[i]);
            }
            i++;
        }
    }

    /**
     * Given an invoice from the test or demo data XML, constructs a new invoice that is a direct copy of it.
     * Note: this isn't a very deep copy.  It simply copies the items and adjustments.
     * @param templateInvoiceId the Invoice to use a template
     * @param partyId the Invoice party
     * @return the created Invoice ID
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public String createInvoiceFromTemplate(String templateInvoiceId, String partyId) throws GenericEntityException {
        GenericValue invoiceTemplate = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", templateInvoiceId));
        assertNotNull("Failed to find invoice template with ID [" + templateInvoiceId + "]", invoiceTemplate);

        // make the invoice
        String invoiceId = delegator.getNextSeqId("Invoice");
        GenericValue invoice = delegator.makeValue("Invoice", invoiceTemplate);
        invoice.put("invoiceId", invoiceId);
        if (InvoiceSpecification.InvoiceTypeEnum.isPayable(invoice.getString("invoiceTypeId"))) {
            invoice.put("partyIdFrom", partyId);
        } else {
            invoice.put("partyId", partyId);
        }

        List<GenericValue> copies = new FastList<GenericValue>();
        copies.add(invoice);

        // get all related values and make copies
        List<GenericValue> related = new FastList<GenericValue>();
        related.addAll(invoiceTemplate.getRelated("InvoiceItem"));
        related.addAll(invoiceTemplate.getRelated("InvoiceAdjustment"));
        for (GenericValue template : related) {
            GenericValue copy = delegator.makeValue(template.getEntityName(), template);
            copy.put("invoiceId", invoiceId);
            copies.add(copy);
        }

        delegator.storeAll(copies);
        return invoiceId;
    }
}
