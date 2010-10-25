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
package org.opentaps.tests.financials;

import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import junit.framework.TestCase;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.financials.domain.billing.invoice.InvoiceSpecification;
import org.opentaps.tests.OpentapsTestCase;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Same as below, with tags = null and checkBalanceSheet = true.
     * @param asOfDate the date to get the balance for
     * @return the financial balance <code>Map</code> of glAccountId => balance
     */
    public Map<String, Number> getFinancialBalances(Timestamp asOfDate) {
        return getFinancialBalances(asOfDate, null, true);
    }


    /**
     * Same as below, with checkBalanceSheet = true.
     * @param asOfDate the date to get the balance for
     * @param tags optional accounting tag to use when getting the balances, as a Map of tag1 -> value1, tag2 -> value2, ...
     * @return the financial balance <code>Map</code> of glAccountId => balance
     */
    public Map<String, Number> getFinancialBalances(Timestamp asOfDate, Map<String, String> tags) {
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
    public Map<String, Number> getFinancialBalances(Timestamp asOfDate, Map<String, String> tags, boolean checkBalanceSheet) {
        Timestamp fromDate = getLastClosedDate();
        Map<String, Number> balances = FastMap.newInstance();

        // run the income statement, which returns the income statement accounts and their sums
        Map<String, Object> input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "fromDate", fromDate, "thruDate", asOfDate, "userLogin", userLogin, "glFiscalTypeId", "ACTUAL");
        if (tags != null) {
            input.putAll(tags);
        }
        Map<String, Object> results = runAndAssertServiceSuccess("getIncomeStatementAccountSumsByDate", input, -1, false);
        Map<GenericValue, Number> incomeStatement = (Map<GenericValue, Number>) results.get("glAccountSums");
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
        Map<GenericValue, Number> assetAccountBalances = (Map<GenericValue, Number>) results.get("assetAccountBalances");
        Map<GenericValue, Number> liabilityAccountBalances = (Map<GenericValue, Number>) results.get("liabilityAccountBalances");
        Map<GenericValue, Number> equityAccountBalances = (Map<GenericValue, Number>) results.get("equityAccountBalances");

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
    public String createInvoice(String partyIdTo, String invoiceTypeId, Timestamp invoiceDate, Timestamp dueDate, String invoiceMessage, String referenceNumber, String description) {
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
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
        Map<String, Object> output = runAndAssertServiceSuccess("createInvoice", input);

        return (String) output.get("invoiceId");
    }

    /**
     * Adds an Invoice Item to an Invoice.
     *
     * @param invoiceId of the invoice to add to
     * @param invoiceItemTypeId is the type of the invoice item
     * @param quantity to add on the invoice
     * @param amount price of the invoice item
     * @return new invoice item sequence identifier
     */
    public String createInvoiceItem(String invoiceId, String invoiceItemTypeId, BigDecimal quantity, BigDecimal amount) {
        return createInvoiceItem(invoiceId, invoiceItemTypeId, null, quantity, amount, null);
    }

    /**
     * Adds an Invoice Item to an Invoice.
     *
     * @param invoiceId of the invoice to add to
     * @param invoiceItemTypeId is the type of the invoice item
     * @param quantity to add on the invoice
     * @param amount price of the invoice item
     * @param description the invoice item description
     * @return new invoice item sequence identifier
     */
    public String createInvoiceItem(String invoiceId, String invoiceItemTypeId, BigDecimal quantity, BigDecimal amount, String description) {
        return createInvoiceItem(invoiceId, invoiceItemTypeId, null, quantity, amount, description);
    }

    /**
     * Adds an Invoice Item to an Invoice.
     *
     * @param invoiceId of the invoice to add to
     * @param invoiceItemTypeId is the type of the invoice item
     * @param productId the invoice item product
     * @param quantity to add on the invoice
     * @param amount price of the invoice item
     * @return new invoice item sequence identifier
     */
    public String createInvoiceItem(String invoiceId, String invoiceItemTypeId, String productId, BigDecimal quantity, BigDecimal amount) {
        return createInvoiceItem(invoiceId, invoiceItemTypeId, productId, quantity, amount, null);
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
     * @return new invoice item sequence identifier
     */
    public String createInvoiceItem(String invoiceId, String invoiceItemTypeId, String productId, BigDecimal quantity, BigDecimal amount, String description) {
        return createInvoiceItem(invoiceId, invoiceItemTypeId, productId, quantity, amount, description, null);
    }

    /**
     * Creates an invoice item with optional accountingTags.
     * @param invoiceId of the invoice to add to
     * @param invoiceItemTypeId is the type of the invoice item
     * @param productId the invoice item product
     * @param quantity to add on the invoice
     * @param amount price of the invoice item
     * @param description the invoice item description
     * @param accountingTags optional accounting tag to use when getting the balances, as a Map of tag1 -> value1, tag2 -> value2, ...
     * @return new invoice item sequence identifier
     */
    public String createInvoiceItem(String invoiceId, String invoiceItemTypeId, String productId, BigDecimal quantity, BigDecimal amount, String description, Map<String, String> accountingTags) {
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
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
        Map<String, Object> output = runAndAssertServiceSuccess("createInvoiceItem", input);
        return (String) output.get("invoiceItemSeqId");
    }

    /**
     * Creates sales tax invoice item.
     *
     * @param invoiceId an invoice identifier
     * @param parentItemSeqId an product invoice item on which taxes are calculated
     * @param taxAuthPartyId tax authority geographical location
     * @param taxAuthGeoId tax authority party
     * @param amount tax amount
     * @return new invoice item sequence identifier
     * @throws GenericEntityException
     */
    public String createTaxInvoiceItem(String invoiceId, String parentItemSeqId, String taxAuthPartyId, String taxAuthGeoId, BigDecimal amount) throws GenericEntityException {
        assertNotNull(invoiceId);
        assertNotNull(taxAuthPartyId);
        assertNotNull(taxAuthGeoId);

        GenericValue taxAuth = delegator.findByPrimaryKey("TaxAuthority", UtilMisc.toMap("taxAuthGeoId", taxAuthGeoId, "taxAuthPartyId", taxAuthPartyId));
        assertNotNull("Tax authority isn't found.", taxAuth);

        GenericValue parentInvoiceItem = null;
        if (UtilValidate.isNotEmpty(parentItemSeqId)) {
            parentInvoiceItem = delegator.findByPrimaryKey("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", parentItemSeqId));
        }

        Map<String, Object> ctx = FastMap.<String, Object>newInstance();
        ctx.put("userLogin", userLogin);
        ctx.put("invoiceId", invoiceId);
        ctx.put("invoiceItemTypeId", "ITM_SALES_TAX");
        ctx.put("quantity", BigDecimal.ONE);
        ctx.put("amount", amount);
        ctx.put("taxAuthGeoId", taxAuthGeoId);
        ctx.put("taxAuthPartyId", taxAuthPartyId);
        if (parentInvoiceItem != null) {
            ctx.put("parentInvoiceId", invoiceId);
            ctx.put("parentInvoiceItemSeqId", parentItemSeqId);
            ctx.put("productId", parentInvoiceItem.getString("productId"));
        }

        Map<String, Object> results = runAndAssertServiceSuccess("createInvoiceItem", ctx);
        return (String) results.get("invoiceItemSeqId");
    }

    /**
     * Update an invoice item with optional accountingTags.
     * @param invoiceId of the invoice to update
     * @param invoiceItemTypeId is the type of the invoice item
     * @param productId the invoice item product
     * @param quantity of the invoice item product
     * @param amount price of the invoice item
     * @param description the invoice item description
     * @param accountingTags optional accounting tag to use when getting the balances, as a Map of tag1 -> value1, tag2 -> value2, ...
     */
    public void updateInvoiceItem(String invoiceId, String invoiceItemSeqId, String invoiceItemTypeId, String productId, BigDecimal quantity, BigDecimal amount, String description, Map<String, String> accountingTags) {
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
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
    public void updateInvoiceStatus(String invoiceId, String invoiceStatus, Timestamp invoiceDate) {
        // set invoice status
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
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
    public String createPayment(BigDecimal amount, String partyIdFrom, String paymentTypeId, String paymentMethodTypeId) {
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
    public String createPayment(BigDecimal amount, String partyIdFrom, String paymentTypeId, String paymentMethodTypeId, String endStatus) {
        return createPaymentAndApplication(amount, partyIdFrom, organizationPartyId, paymentTypeId, paymentMethodTypeId, null, null, endStatus);
    }

    /**
     * Creates a Payment.
     *
     * @param amount the Payment and PaymentApplication amount
     * @param partyIdFrom the Payment party from
     * @param paymentTypeId the Payment type
     * @param paymentMethodTypeId the Payment method type, optional
     * @param endStatus is the status to update the payment after creation
     * @param accountingTags optional accounting tag to use when getting the balances, as a Map of tag1 -> value1, tag2 -> value2, ...
     * @return the created payment ID
     */
    public String createPayment(BigDecimal amount, String partyIdFrom, String paymentTypeId, String paymentMethodTypeId, String endStatus, Map<String, String> accountingTags) {
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
    public String createPaymentAndApplication(BigDecimal amount, String partyIdFrom, String partyIdTo, String paymentTypeId, String paymentMethodTypeId, String paymentMethodId, String invoiceId, String endStatus) {
        return createPaymentAndApplication(amount, partyIdFrom, partyIdTo, paymentTypeId, paymentMethodTypeId, paymentMethodId, invoiceId, endStatus, null);
    }

    /**
     * Creates a Payment and PaymentApplication, with accountingTags for the Payment.
     *
     * @param amount the Payment and PaymentApplication amount
     * @param partyIdFrom the Payment party from
     * @param partyIdTo the Payment party to, if <code>null</code> uses the current organization
     * @param paymentTypeId the Payment type
     * @param paymentMethodTypeId the Payment method type, optional
     * @param paymentMethodId the Payment method, optional
     * @param invoiceId the Invoice to apply the Payment to, if <code>null</code> no PaymentApplication is created
     * @param endStatus is the status to update the payment after creation
     * @param accountingTags optional accounting tag to use when getting the balances, as a Map of tag1 -> value1, tag2 -> value2, ...
     * @return the created payment ID
     */
    public String createPaymentAndApplication(BigDecimal amount, String partyIdFrom, String partyIdTo, String paymentTypeId, String paymentMethodTypeId, String paymentMethodId, String invoiceId, String endStatus, Map<String, String> accountingTags) {
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
        input.put("amount", amount);
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
        Map<String, Object> output = runAndAssertServiceSuccess("financials.createPayment", input);
        String paymentId = (String) output.get("paymentId");

        if (UtilValidate.isNotEmpty(invoiceId)) {
            input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
            input.put("paymentId", paymentId);
            input.put("invoiceId", invoiceId);
            input.put("amountApplied", amount);
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
    public void updatePaymentStatus(String paymentId, String paymentStatus) {
        // set invoice status
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
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

    /**
     * Creates a PaymentApplication, with accountingTags for the PaymentApplication.
     * @param amount the payment application amount
     * @param paymentId the payment ID to apply
     * @param invoiceId the invoice ID to apply to
     * @param accountingTags the <code>Map</code> of accounting tags to set
     */
    public void updatePaymentApplication(BigDecimal amount, String paymentId, String invoiceId, Map<String, String> accountingTags) {

        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", amount);
        if (accountingTags != null) {
            input.putAll(accountingTags);
        }
        runAndAssertServiceSuccess("updatePaymentApplication", input);
    }

    /**
     * Verify summary gross sales, discounts, taxable and tax amount values in TaxInvoiceItemFact entity
     * for an organization or for particular invoice.
     *
     * @param invoiceId a <code>String</code> value
     * @param authPartyId a <code>String</code> value
     * @param authGeoId a <code>String</code> value
     * @param grossSales a <code>double</code> value
     * @param discounts a <code>double</code> value
     * @param refunds a <code>double</code> value
     * @param netAmount a <code>double</code> value
     * @param taxable a <code>double</code> value
     * @param tax a <code>double</code> value
     * @exception GeneralException if an error occurs
     */
    public void assertSalesTaxFact(String invoiceId, String authPartyId, String authGeoId, double grossSales, double discounts, double refunds, double netAmount, double taxable, double tax) throws GeneralException {

        BigDecimal tempGrossSales = BigDecimal.ZERO;
        BigDecimal tempDiscounts = BigDecimal.ZERO;
        BigDecimal tempRefunds = BigDecimal.ZERO;
        BigDecimal tempNetAmount = BigDecimal.ZERO;
        BigDecimal tempTaxable = BigDecimal.ZERO;
        BigDecimal tempTax = BigDecimal.ZERO;

        // find organization dimension
        List<GenericValue> orgDimList = delegator.findByAnd("OrganizationDim", UtilMisc.toMap("organizationPartyId", organizationPartyId));
        assertNotEmpty("There is no any company with ID [" + organizationPartyId + "] in organization dimension.", orgDimList);
        Long organizationDimKey = EntityUtil.getFirst(orgDimList).getLong("organizationDimId");

        // find tax authority
        Long taxAuthDimId = -1L; //nonexistent key
        GenericValue taxAuthDim = EntityUtil.getFirst(delegator.findByAnd("TaxAuthorityDim", UtilMisc.toMap("taxAuthPartyId", authPartyId, "taxAuthGeoId", authGeoId)));
        if (taxAuthDim != null) {
            taxAuthDimId = taxAuthDim.getLong("taxAuthorityDimId");
        }

        // select tax invoice item facts filtered out by organization and invoice (optionally)
        Map<String, Object> conditions = UtilMisc.<String, Object>toMap("organizationDimId", organizationDimKey);
        if (UtilValidate.isNotEmpty(invoiceId)) {
            conditions.put("invoiceId", invoiceId);
        }
        List<GenericValue> taxFacts = delegator.findByAnd("TaxInvoiceItemFact", conditions);
        // accumulate values taking into consideration several facts may exist for the same
        // invoice item but for different tax authorities
        Set<String> uniqueItemId = FastSet.<String>newInstance();
        for (GenericValue fact : taxFacts) {
            String invoiceItemId = fact.getString("invoiceItemSeqId");
            if (uniqueItemId.contains(invoiceItemId)) {
                continue;
            }
            uniqueItemId.add(invoiceItemId);
            tempGrossSales = tempGrossSales.add(fact.getBigDecimal("grossAmount"));
            tempDiscounts = tempDiscounts.add(fact.getBigDecimal("discounts"));
            tempRefunds = tempRefunds.add(fact.getBigDecimal("refunds"));
            tempNetAmount = tempNetAmount.add(fact.getBigDecimal("netAmount"));
            BigDecimal taxDue = fact.getBigDecimal("taxDue");
            if (taxDue != null && taxDue.compareTo(BigDecimal.ZERO) != 0) {
                tempTaxable = tempTaxable.add(fact.getBigDecimal("taxable"));
            }
        }

        // find and accumulate tax amount for given tax authority
        conditions.put("taxAuthorityDimId", taxAuthDimId);
        taxFacts = delegator.findByAnd("TaxInvoiceItemFact", conditions);
        for (GenericValue fact : taxFacts) {
            tempTax = tempTax.add(fact.getBigDecimal("taxDue"));
        }

        // verify summaries
        assertEquals("TaxInvoiceItemFact entity contains wrong gross sales amount for invoice " + invoiceId, tempGrossSales, BigDecimal.valueOf(grossSales));
        assertEquals("TaxInvoiceItemFact entity contains wrong discounts for invoice " + invoiceId, tempDiscounts, BigDecimal.valueOf(discounts));
        assertEquals("TaxInvoiceItemFact entity contains wrong refunds for invoice " + invoiceId, tempRefunds, BigDecimal.valueOf(refunds));
        assertEquals("TaxInvoiceItemFact entity contains wrong net amounts for invoice " + invoiceId, tempNetAmount, BigDecimal.valueOf(netAmount));
        assertEquals("TaxInvoiceItemFact entity contains wrong taxable amounts for invoice " + invoiceId, tempTaxable, BigDecimal.valueOf(taxable));
        assertEquals("TaxInvoiceItemFact entity contains wrong taxes for invoice " + invoiceId, tempTax, BigDecimal.valueOf(tax));
    }

    /**
     * Verify summary gross sales, discounts, refunds and net amount values in SalesInvoiceItemFact entity
     * for an organization or for particular invoice.
     *
     * @param invoiceId a <code>String</code> value
     * @param grossSales a <code>double</code> value
     * @param discounts a <code>double</code> value
     * @param refunds a <code>double</code> value
     * @param netAmount a <code>double</code> value
     * @exception GeneralException if an error occurs
     */
    public void assertSalesFact(String invoiceId, double grossSales, double discounts, double refunds, double netAmount) throws GeneralException {

        BigDecimal tempGrossSales = BigDecimal.ZERO;
        BigDecimal tempDiscounts = BigDecimal.ZERO;
        BigDecimal tempRefunds = BigDecimal.ZERO;
        BigDecimal tempNetAmount = BigDecimal.ZERO;

        // find organization dimension
        List<GenericValue> orgDimList = delegator.findByAnd("OrganizationDim", UtilMisc.toMap("organizationPartyId", organizationPartyId));
        assertNotEmpty("There is no any company with ID [" + organizationPartyId + "] in organization dimension.", orgDimList);
        Long organizationDimKey = EntityUtil.getFirst(orgDimList).getLong("organizationDimId");

        // select sales invoice item facts filtered out by organization and invoice (optionally)
        Map<String, Object> conditions = UtilMisc.<String, Object>toMap("organizationDimId", organizationDimKey);
        if (UtilValidate.isNotEmpty(invoiceId)) {
            conditions.put("invoiceId", invoiceId);
        }
        List<GenericValue> saleFacts = delegator.findByAnd("SalesInvoiceItemFact", conditions);
        // accumulate values
        for (GenericValue fact : saleFacts) {
            tempGrossSales = tempGrossSales.add(fact.getBigDecimal("grossAmount"));
            tempDiscounts = tempDiscounts.add(fact.getBigDecimal("discounts"));
            tempRefunds = tempRefunds.add(fact.getBigDecimal("refunds"));
            tempNetAmount = tempNetAmount.add(fact.getBigDecimal("netAmount"));
        }

        // verify summaries
        assertEquals("SalesInvoiceItemFact entity contains wrong gross sales amount for invoice " + invoiceId, tempGrossSales, BigDecimal.valueOf(grossSales));
        assertEquals("SalesInvoiceItemFact entity contains wrong discounts for invoice " + invoiceId, tempDiscounts, BigDecimal.valueOf(discounts));
        assertEquals("SalesInvoiceItemFact entity contains wrong refunds for invoice " + invoiceId, tempRefunds, BigDecimal.valueOf(refunds));
        assertEquals("SalesInvoiceItemFact entity contains wrong net amounts for invoice " + invoiceId, tempNetAmount, BigDecimal.valueOf(netAmount));
    }

}
