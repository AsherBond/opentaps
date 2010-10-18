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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.opensourcestrategies.financials.accounts.AccountsHelper;
import com.opensourcestrategies.financials.util.UtilCOGS;
import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.constants.AcctgTransTypeConstants;
import org.opentaps.base.constants.GlFiscalTypeConstants;
import org.opentaps.base.constants.InvoiceAdjustmentTypeConstants;
import org.opentaps.base.constants.InvoiceItemTypeConstants;
import org.opentaps.base.constants.InvoiceTypeConstants;
import org.opentaps.base.constants.PaymentMethodTypeConstants;
import org.opentaps.base.constants.PaymentTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.AccountBalanceHistory;
import org.opentaps.base.entities.AcctgTransEntry;
import org.opentaps.base.entities.CustomTimePeriod;
import org.opentaps.base.entities.GlAccountHistory;
import org.opentaps.base.entities.GlAccountOrganization;
import org.opentaps.base.entities.InventoryItemValueHistory;
import org.opentaps.base.entities.InvoiceAdjustmentGlAccount;
import org.opentaps.base.entities.InvoiceAdjustmentType;
import org.opentaps.base.entities.SupplierProduct;
import org.opentaps.base.services.CreateQuickAcctgTransService;
import org.opentaps.base.services.PostAcctgTransService;
import org.opentaps.common.order.PurchaseOrderFactory;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.inventory.InventoryDomainInterface;
import org.opentaps.domain.inventory.InventoryItem;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.ledger.AccountingTransaction;
import org.opentaps.domain.ledger.AccountingTransaction.TagBalance;
import org.opentaps.domain.ledger.GeneralLedgerAccount;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.purchasing.PurchasingRepositoryInterface;
import org.opentaps.financials.domain.billing.invoice.InvoiceRepository;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.tests.analytics.tests.TestObjectGenerator;
import org.opentaps.tests.warehouse.InventoryAsserts;

/**
 * General financial tests.  If there is a large chunk of test cases that are related, please
 * place them in a separate class.
 *
 * These tests are designed to run over and over again on the same database.
 */
public class FinancialsTests extends FinancialsTestCase {

    private static final String MODULE = FinancialsTests.class.getName();

    /** userLogin for inventory operations. */
    private GenericValue demowarehouse1 = null;

    /** Facility and inventory owner. */
    private static final String facilityContactMechId = "9200";

    private static final String testLedgerOrganizationPartyId = "LEDGER-TEST";
    private static final String testLedgerTransId = "LEDGER-TEST-1";  // pre-stored transactions

    private static final int LOOP_TESTS = 1000;

    private TimeZone timeZone = TimeZone.getDefault();
    private Locale locale = Locale.getDefault();


    @Override
    public void setUp() throws Exception {
        super.setUp();
        User = admin;
        demowarehouse1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
    }

    @Override
    public void tearDown() throws Exception {
        demowarehouse1 = null;
        super.tearDown();
    }

    /**
     * Test organizationRepository's getAllFiscalTimePeriods returns correct number of time periods.
     * @throws GeneralException if an error occurs
     */
    public void testGetAllCustomTimePeriods() throws GeneralException {
        OrganizationRepositoryInterface orgRepository = organizationDomain.getOrganizationRepository();
        List<CustomTimePeriod> timePeriods = orgRepository.getAllFiscalTimePeriods(testLedgerOrganizationPartyId);
        // check that the number of time periods is same as that in LedgerPostingTestData.xml
        assertEquals("Correct number of time periods found for [" + testLedgerOrganizationPartyId + "]", timePeriods.size(), 12);
    }

    /**
     * Verify that the AccountingTransaction getDebitTotal() and getCreditTotal() methods are correct.
     * @throws GeneralException if an error occurs
     */
    public void testAccountingTransactionDebitCreditTotals() throws GeneralException {
        LedgerRepositoryInterface ledgerRepository = ledgerDomain.getLedgerRepository();
        AccountingTransaction ledgerTestTrans = ledgerRepository.getAccountingTransaction(testLedgerTransId);
        assertEquals("Transaction [" + testLedgerTransId + "] debit total is not correct", ledgerTestTrans.getDebitTotal(), new BigDecimal("300.0"));
        assertEquals("Transaction [" + testLedgerTransId + "] credit total is not correct", ledgerTestTrans.getCreditTotal(), new BigDecimal("300.0"));
    }

    /**
     * Verify key ledger posting features:
     * 1.  transactions will not post before scheduled date
     * 2.  correct debit/credit balances will be set for all time periods
     * 3.  will not post transactions which have already been posted
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testLedgerPosting() throws GeneralException {
        DomainsDirectory dd = new DomainsLoader(new Infrastructure(dispatcher), new User(demofinadmin)).loadDomainsDirectory();
        LedgerRepositoryInterface ledgerRepository = ledgerDomain.getLedgerRepository();
        OrganizationRepositoryInterface organizationRepository = dd.getOrganizationDomain().getOrganizationRepository();
        AccountingTransaction ledgerTestTrans = ledgerRepository.getAccountingTransaction(testLedgerTransId);

        Map postToLedgerParams = UtilMisc.toMap("acctgTransId", testLedgerTransId, "userLogin", demofinadmin);

        // verify that a transaction which is supposed to post in the future cannot be posted
        ledgerTestTrans.setScheduledPostingDate(UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), 10));
        ledgerRepository.update(ledgerTestTrans);
        runAndAssertServiceError("postAcctgTrans", postToLedgerParams);

        // verify that it can post without the scheduled posting date
        postToLedgerParams = UtilMisc.toMap("acctgTransId", testLedgerTransId, "userLogin", demofinadmin);
        ledgerTestTrans.setScheduledPostingDate(null);
        ledgerRepository.update(ledgerTestTrans);
        runAndAssertServiceSuccess("postAcctgTrans", postToLedgerParams);

        // verify AcctgTrans.getPostedAmount() is 300.00
        ledgerTestTrans = ledgerRepository.getAccountingTransaction(testLedgerTransId);
        assertEquals("AcctgTrans.getPostedAmount() should be 300.00", new BigDecimal("300.0"), ledgerTestTrans.getPostedAmount());

        // the test transaction should only post to these time periods
        List timePeriodsWithPosting = UtilMisc.toList("LT2008", "LT2008Q1", "LT2008FEB");

        // verify that the GL Account History is correctly updated for each time period by checking each entry of the
        // AcctgTrans and verifying that its GL account is correct for all available time periods
        List<CustomTimePeriod> availableTimePeriods = organizationRepository.getAllFiscalTimePeriods(testLedgerOrganizationPartyId);
        List<? extends AcctgTransEntry> transEntries = ledgerTestTrans.getAcctgTransEntrys();
        for (AcctgTransEntry entry : transEntries) {
            for (CustomTimePeriod period : availableTimePeriods) {
                GlAccountHistory accountHistory = ledgerRepository.getAccountHistory(entry.getGlAccountId(), testLedgerOrganizationPartyId, period.getCustomTimePeriodId());
                // verify that only the correct time periods were posted to
                if (timePeriodsWithPosting.contains(period.getCustomTimePeriodId())) {
                    assertNotNull("Time period [" + period.getCustomTimePeriodId() + "] was posted to for gl account [" + entry.getGlAccountId(), accountHistory);
                    GeneralLedgerAccount glAccount = ledgerRepository.getLedgerAccount(entry.getGlAccountId(), testLedgerOrganizationPartyId);
                    if (glAccount.isDebitAccount()) {
                        assertEquals("Posted debits do not equal for " + accountHistory + " and " + entry, accountHistory.getPostedDebits(), entry.getAmount());
                    } else {
                        assertEquals("Posted credits do not equal for " + accountHistory + " and " + entry, accountHistory.getPostedCredits(), entry.getAmount());
                    }
                } else {
                    assertNull("Time period [" + period.getCustomTimePeriodId() + "] was not posted to for gl account [" + entry.getGlAccountId(), accountHistory);
                }
            }
        }

        // verify that we cannot post it again
        postToLedgerParams = UtilMisc.toMap("acctgTransId", testLedgerTransId, "userLogin", demofinadmin);
        runAndAssertServiceError("postAcctgTrans", postToLedgerParams);
    }

    /**
     * Tests posting a transaction that is globally balanced but unbalanced regarding tag1 because of a missing tag (which is configured for STATEMENT_DETAILS).
     * Specifically one entry has a tag 1 debit 5000, and the corresponding credit entry has no tag 1.
     * @throws GeneralException if an error occurs
     */
    public void testLedgerPostingTagBalanceMissing() throws GeneralException {
        // check the transaction cannot be posted
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", demofinadmin);
        input.put("acctgTransId", "BAD-STATEMENT-TEST-1");
        runAndAssertServiceError("postAcctgTrans", input);

        // check the accounting transaction canPost() method
        LedgerRepositoryInterface ledgerRepository = ledgerDomain.getLedgerRepository();
        AccountingTransaction trans = ledgerRepository.getAccountingTransaction("BAD-STATEMENT-TEST-1");
        assertFalse("Transaction BAD-STATEMENT-TEST-1 should not be marked as able to post.", trans.canPost());

        // check the error is related to the unbalanced tag 1
        TagBalance tagNotBalance = trans.accountingTagsBalance();
        assertEquals("Transaction BAD-STATEMENT-TEST-1 tag 1 should be unbalanced", 1, tagNotBalance.getIndex());
        assertEquals("Transaction BAD-STATEMENT-TEST-1 tag 1 should be unbalanced", new BigDecimal(5000), tagNotBalance.getBalance().abs());
    }

    /**
     * tests posting a transaction that is globally balanced but unbalanced regarding tag1 because of a mismatched tag (which is configured for STATEMENT_DETAILS).
     * Specifically the credit and debit entries both has a tag 1 but not the same tag value.
     * @throws GeneralException if an error occurs
     */
    public void testLedgerPostingTagBalanceMismatched() throws GeneralException {
        // check the transaction cannot be posted
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", demofinadmin);
        input.put("acctgTransId", "BAD-STATEMENT-TEST-2");
        runAndAssertServiceError("postAcctgTrans", input);

        // check the accounting transaction canPost() method
        LedgerRepositoryInterface ledgerRepository = ledgerDomain.getLedgerRepository();
        AccountingTransaction trans = ledgerRepository.getAccountingTransaction("BAD-STATEMENT-TEST-2");
        assertFalse("Transaction BAD-STATEMENT-TEST-2 should not be marked as able to post.", trans.canPost());

        // check the error is related to the unbalanced tag 1
        TagBalance tagNotBalance = trans.accountingTagsBalance();
        assertEquals("Transaction BAD-STATEMENT-TEST-2 tag 1 should be unbalanced", 1, tagNotBalance.getIndex());
        assertEquals("Transaction BAD-STATEMENT-TEST-2 tag 1 should be unbalanced", new BigDecimal(1200), tagNotBalance.getBalance().abs());
    }

    /**
     * Test getting the right GL Account for GlAccountTypeId and organization.
     * @throws GeneralException if an error occurs
     */
    public void testGlAccountTypeSetting() throws GeneralException {
        LedgerRepositoryInterface ledgerRepository = ledgerDomain.getLedgerRepository();
        GeneralLedgerAccount glAccount = ledgerRepository.getDefaultLedgerAccount("ACCOUNTS_RECEIVABLE", testLedgerOrganizationPartyId);
        // this hardcoded gl account ID Needs to be the same as that defined in opentaps/opentaps-tests/data/financials/LedgerPostingTestData.xml
        assertEquals("Incorrect Accounts Receivables account for [" + testLedgerOrganizationPartyId + "]", "120000", glAccount.getGlAccountId());
    }

    /**
     * Test the sending of a paycheck [DEMOPAYCHECK1] and compares the resulting GL transaction
     * to the reference transaction DEMOPAYCHECK1 in PayrollEntries.xml.
     *
     * Note that this is a test between an initial demo paycheck and an expected result set,
     * so we are not testing the logic in creating a paycheck, but rather the ledger posting
     * logic.  The idea is to test the part of the system which is difficult for humans to
     * validate immediately.  Bugs in the posting process may surface after many months, so
     * this makes for a perfect unit test subject.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPaycheckTransactions() throws GeneralException {

        // before we begin, note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        GenericValue paycheck = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", "DEMOPAYCHECK1"));
        assertNotNull("Paycheck with ID [DEMOPAYCHECK1] not found.", paycheck);

        // set the status to "not paid" so we can send it
        paycheck.set("statusId", "PMNT_NOT_PAID");
        paycheck.store();

        // mark the payment as sent
        fa.updatePaymentStatus(paycheck.getString("paymentId"), "PMNT_SENT");

        // get the transaction with the assistance of our start timestamp.  Note that this requires the DEMOPAYCHECK1 not to have an effectiveDate of its own
        // or it would be posted with transactionDate = payment.effectiveDate
        Set transactions = getAcctgTransSinceDate(UtilMisc.toList(EntityCondition.makeCondition("paymentId", EntityOperator.EQUALS, paycheck.get("paymentId"))), start, delegator);
        assertNotEmpty("Paycheck transaction not created.", transactions);

        // assert transaction equivalence with the reference transaction
        assertTransactionEquivalence(transactions, UtilMisc.toList("PAYCHECKTEST1"));
    }

    /**
     * Test receives a few serialized and non-serialized items of product, write off
     * some quantity of product as damaged and checks average cost after.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testProductAverageCost() throws GeneralException {

        // create product for inventory operations
        Map<String, Object> callContext = new HashMap<String, Object>();
        callContext.put("productTypeId", "FINISHED_GOOD");
        callContext.put("internalName", "Product for use in Average Cost Unit Tests");
        callContext.put("isVirtual", "N");
        callContext.put("isVariant", "N");
        callContext.put("userLogin", demowarehouse1);
        Map<String, Object> product = runAndAssertServiceSuccess("createProduct", callContext);
        String productId = (String) product.get("productId");
        assertEquals("Failed to create test product.", true, productId != null);

        Timestamp startTime = UtilDateTime.nowTimestamp();
        BigDecimal expectedAvgCost = null;
        BigDecimal calculatedAvgCost = null;
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, "WebStoreWarehouse", organizationPartyId, demowarehouse1);
        final BigDecimal acceptedDelta = new BigDecimal("0.009");

        // common parameters for receiveInventoryProduct service
        Map<String, Object> commonParameters = new HashMap<String, Object>();
        commonParameters.put("productId", productId);
        commonParameters.put("facilityId", "WebStoreWarehouse");
        commonParameters.put("currencyUomId", "USD");
        commonParameters.put("datetimeReceived", startTime);
        commonParameters.put("quantityRejected", BigDecimal.ZERO);
        commonParameters.put("userLogin", demowarehouse1);

        /*
         * receives 10 products at $15, average cost = 15  [10 x 15$]
         */
        Map<String, Object> input = new HashMap<String, Object>();
        input.putAll(commonParameters);
        input.putAll(UtilMisc.toMap("inventoryItemTypeId", "NON_SERIAL_INV_ITEM", "unitCost", new BigDecimal("15"), "quantityAccepted", new BigDecimal("10")));
        Map output = runAndAssertServiceSuccess("receiveInventoryProduct", input);

        calculatedAvgCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
        assertNotNull("The product [" + productId + "] average cost cannot be null", calculatedAvgCost);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("15");
        assertEquals("After receipt 10 products [" + productId + "] at $15 calculated average cost doesn't equal expected one with inadmissible error.", expectedAvgCost, calculatedAvgCost, acceptedDelta);

        inventoryAsserts.assertInventoryValuesEqual(productId);

        // we'll write this inventory off later
        String inventoryItemIdVar = (String) output.get("inventoryItemId");

        pause("allow distinct product average cost timestamps");

        /*
         * receives 10 products at $20, average cost = 17.5  [10 x 15$ + 10 x 20$]
         */
        input = new HashMap<String, Object>();
        input.putAll(commonParameters);
        input.putAll(UtilMisc.toMap("inventoryItemTypeId", "NON_SERIAL_INV_ITEM", "unitCost", new BigDecimal("20"), "quantityAccepted", new BigDecimal("10")));
        output = runAndAssertServiceSuccess("receiveInventoryProduct", input);

        calculatedAvgCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
        assertNotNull("The product [" + productId + "] average cost cannot be null", calculatedAvgCost);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("17.5");
        assertEquals("After receipt 10 products [" + productId + "] at $20 calculated average cost doesn't equal expected one with inadmissible error.", expectedAvgCost, calculatedAvgCost, acceptedDelta);

        inventoryAsserts.assertInventoryValuesEqual(productId);

        String inventoryItemIdForRevalue1 = (String) output.get("inventoryItemId");

        pause("allow distinct product average cost timestamps");

        /*
         * receives 1 serialized product at $10, average cost = 17.142857  [10 x 15$ + 10 x 20$ + 1 x 10$]
         */
        input = new HashMap<String, Object>();
        input.putAll(commonParameters);
        input.putAll(UtilMisc.toMap("inventoryItemTypeId", "SERIALIZED_INV_ITEM", "unitCost", new BigDecimal("10"), "quantityAccepted", BigDecimal.ONE));
        runAndAssertServiceSuccess("receiveInventoryProduct", input);

        calculatedAvgCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
        assertNotNull("The product [" + productId + "] average cost cannot be null", calculatedAvgCost);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("17.142");
        assertEquals("After receipt 1 serialized product [" + productId + "] at $10 calculated average cost doesn't equal expected one with inadmissible error.", expectedAvgCost, calculatedAvgCost, acceptedDelta);

        inventoryAsserts.assertInventoryValuesEqual(productId);

        pause("allow distinct product average cost timestamps");

        /*
         * receives 1 serialized product at $12, average cost = 16.909091  [10 x 15$ + 10 x 20$ + 1 x 10$ +1 x 12$]
         */
        input = new HashMap<String, Object>();
        input.putAll(commonParameters);
        input.putAll(UtilMisc.toMap("inventoryItemTypeId", "SERIALIZED_INV_ITEM", "unitCost", new BigDecimal("12"), "quantityAccepted", BigDecimal.ONE));
        runAndAssertServiceSuccess("receiveInventoryProduct", input);

        calculatedAvgCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
        assertNotNull("The product [" + productId + "] average cost cannot be null", calculatedAvgCost);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("16.909");
        assertEquals("After receipt 1 serialized product [" + productId + "] at $12 calculated average cost doesn't equal expected one with inadmissible error.", expectedAvgCost, calculatedAvgCost, acceptedDelta);

        inventoryAsserts.assertInventoryValuesEqual(productId);

        pause("allow distinct product average cost timestamps");

        /*
         * Create variance -5 products from inventory item at $15, average cost = 16.909091  [10 x 15$ + 10 x 20$ + 1 x 10$ + 1 x 12$ - 5 x avg] doesn't change the average cost
         */
        BigDecimal varianceQuantity = new BigDecimal("-5");
        input = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", inventoryItemIdVar, "quantityOnHandVar", varianceQuantity, "availableToPromiseVar", varianceQuantity,  "varianceReasonId", "VAR_DAMAGED");
        runAndAssertServiceSuccess("createPhysicalInventoryAndVariance", input);

        calculatedAvgCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
        assertNotNull("The product [" + productId + "] average cost cannot be null", calculatedAvgCost);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("16.909");
        assertEquals("After creating variance for -5 products [" + productId + "] calculated average cost doesn't equal expected one with inadmissible error.", expectedAvgCost, calculatedAvgCost, acceptedDelta);

        inventoryAsserts.assertInventoryValuesEqual(productId);

        pause("allow distinct product average cost timestamps");

        /*
         * Create variance -3 products from inventory item at $15, average cost = 16.909091  [10 x 15$ + 10 x 20$ + 1 x 10$ + 1 x 12$ - 8 x avg] doesn't change the average cost
         */
        varianceQuantity = new BigDecimal("-3");
        input = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", inventoryItemIdVar, "quantityOnHandVar", varianceQuantity, "availableToPromiseVar", varianceQuantity,  "varianceReasonId", "VAR_DAMAGED");
        runAndAssertServiceSuccess("createPhysicalInventoryAndVariance", input);

        calculatedAvgCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
        assertNotNull("The product [" + productId + "] average cost cannot be null", calculatedAvgCost);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("16.909");
        assertEquals("After creating variance for -3 products [" + productId + "] calculated average cost doesn't equal expected one with inadmissible error.", expectedAvgCost, calculatedAvgCost, acceptedDelta);

        inventoryAsserts.assertInventoryValuesEqual(productId);

        pause("allow distinct product average cost timestamps");

        /*
         * receives 3 products at $11 average cost = 15.865882  [10 x 15$ + 10 x 20$ + 1 x 10$ + 1 x 12$ + 3 x 11$ - 8 x  16.91]
         */
        input = new HashMap<String, Object>();
        input.putAll(commonParameters);
        input.putAll(UtilMisc.toMap("inventoryItemTypeId", "NON_SERIAL_INV_ITEM", "unitCost", new BigDecimal("11"), "quantityAccepted", new BigDecimal("3")));
        runAndAssertServiceSuccess("receiveInventoryProduct", input);

        calculatedAvgCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
        assertNotNull("The product [" + productId + "] average cost cannot be null", calculatedAvgCost);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("15.866");
        assertEquals("After receipt 3 products [" + productId + "] at $11 calculated average cost doesn't equal expected one with inadmissible error.", expectedAvgCost, calculatedAvgCost, acceptedDelta);

        inventoryAsserts.assertInventoryValuesEqual(productId);

        pause("allow distinct product average cost timestamps");

        /*
         * receives 1 serialized products at $22 average cost = 16.206667  [10 x 15$ + 10 x 20$ + 1 x 10$ + 1 x 12$ + 3 x 11$ + 1 x 22$ - 8 x  16.91]
         */
        input = new HashMap<String, Object>();
        input.putAll(commonParameters);
        input.putAll(UtilMisc.toMap("inventoryItemTypeId", "SERIALIZED_INV_ITEM", "unitCost", new BigDecimal("22"), "quantityAccepted", new BigDecimal("1")));
        output = runAndAssertServiceSuccess("receiveInventoryProduct", input);

        calculatedAvgCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
        assertNotNull("The product [" + productId + "] average cost cannot be null", calculatedAvgCost);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("16.207");
        assertEquals("After receipt 1 serialized product [" + productId + "] at $22 calculated average cost doesn't equal expected one with inadmissible error.", expectedAvgCost, calculatedAvgCost, acceptedDelta);
        inventoryAsserts.assertInventoryValuesEqual(productId);

        String inventoryItemIdForRevalue2 = (String) output.get("inventoryItemId");

        pause("allow distinct product average cost timestamps");

        /*
         * revalue inventoryItemIdForRevalue1 to $10 average cost = 10.651111 [10 x 15$ + 10 x 10$ + 1 x 10$ + 1 x 12$ + 3 x 11$ + 1 x 22$ - 8 x  16.91]
         */
        input = new HashMap<String, Object>();
        input.put("inventoryItemId", inventoryItemIdForRevalue1);
        input.put("productId", productId);
        input.put("ownerPartyId", organizationPartyId);
        input.put("userLogin", demowarehouse1);
        input.put("unitCost", new BigDecimal("10.0"));
        input.put("currencyUomId", "USD");
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        runAndAssertServiceSuccess("updateInventoryItem", input);

        calculatedAvgCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
        assertNotNull("The product [" + productId + "] average cost cannot be null", calculatedAvgCost);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("10.651");
        assertEquals("After revalue 10 products [" + productId + "] at $20 to $10 calculated average cost doesn't equal expected one with inadmissible error.", expectedAvgCost, calculatedAvgCost, acceptedDelta);
        inventoryAsserts.assertInventoryValuesEqual(productId);

        pause("allow distinct product average cost timestamps");

        /*
         * revalue inventoryItemIdForRevalue2 to $10
         */
        input = new HashMap<String, Object>();
        input.put("inventoryItemId", inventoryItemIdForRevalue2);
        input.put("productId", productId);
        input.put("ownerPartyId", organizationPartyId);
        input.put("userLogin", demowarehouse1);
        input.put("unitCost", new BigDecimal("10.0"));
        input.put("currencyUomId", "USD");
        input.put("inventoryItemTypeId", "SERIALIZED_INV_ITEM");
        runAndAssertServiceSuccess("updateInventoryItem", input);

        calculatedAvgCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
        assertNotNull("The product [" + productId + "] average cost cannot be null", calculatedAvgCost);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("9.984");
        assertEquals("After revalue 1 serialized product [" + productId + "] at $22 to $10 calculated average cost doesn't equal expected one with inadmissible error.", expectedAvgCost, calculatedAvgCost, acceptedDelta);
        inventoryAsserts.assertInventoryValuesEqual(productId);
    }

    /**
     * This test verifies that products average cost will be correct after
     * a large number of transactions in rapid succession of each other.
     * @throws GeneralException if an error occurs
     */
    public void testProductAverageCostLongRunning() throws GeneralException {
        // Create a new product
        Map<String, Object> callContext = new HashMap<String, Object>();
        callContext.put("productTypeId", "FINISHED_GOOD");
        callContext.put("internalName", "Product for use in Average Cost Long Running Tests");
        callContext.put("isVirtual", "N");
        callContext.put("isVariant", "N");
        callContext.put("userLogin", demowarehouse1);
        Map<String, Object> product = runAndAssertServiceSuccess("createProduct", callContext);
        String productId = (String) product.get("productId");

        //  track two values: total quantity and total value of the product.  Initially, both should be zero
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        // common parameters for receiveInventoryProduct service
        Map<String, Object> commonParameters = new HashMap<String, Object>();
        commonParameters.put("productId", productId);
        commonParameters.put("facilityId", "WebStoreWarehouse");
        commonParameters.put("currencyUomId", "USD");
        commonParameters.put("quantityRejected", BigDecimal.ZERO);
        commonParameters.put("userLogin", demowarehouse1);

        // for i = 1 to 1000
        for (int i = 1; i < LOOP_TESTS; i++) {
            //    q (quantity) = i, uc (unit cost) = i mod 1000
            //    receive q of product at unit cost = uc
            BigDecimal q = new BigDecimal(i);
            BigDecimal uc = new BigDecimal(i % 1000);
            Map<String, Object> input = new HashMap<String, Object>();
            input.putAll(commonParameters);
            input.putAll(UtilMisc.toMap("inventoryItemTypeId", "NON_SERIAL_INV_ITEM"
                    , "unitCost", uc, "quantityAccepted" , q
                    , "datetimeReceived", UtilDateTime.nowTimestamp()));
            runAndAssertServiceSuccess("receiveInventoryProduct", input);
            // update total quantity (add q) and total value (add q*uc)
            totalQuantity = totalQuantity.add(q);
            totalValue = totalValue.add(q.multiply(uc));
            // Verify that the average cost of the product from UtilCOGS.getProductAverageCost is equal to (total value)/(total quantity) to within 5 decimal places
            BigDecimal productAverageCost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, demowarehouse1, delegator, dispatcher);
            BigDecimal expectedAvgCost = totalValue.divide(totalQuantity, DECIMALS, BigDecimal.ROUND_HALF_DOWN);
            assertEquals("Product [" + productId + "] average cost should be " + expectedAvgCost + ".", expectedAvgCost, productAverageCost.setScale(DECIMALS, BigDecimal.ROUND_HALF_DOWN));
        }

    }

    /**
     * This test verifies that products average cost will be correct after
     * a large number of transactions in rapid succession of each other.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInventoryItemValueHistoryLongRunning() throws GeneralException {
        // Create a new product
        Map<String, Object> callContext = new HashMap<String, Object>();
        callContext.put("productTypeId", "FINISHED_GOOD");
        callContext.put("internalName", "Product for use in testInventoryItemValueHistoryLongRunning");
        callContext.put("isVirtual", "N");
        callContext.put("isVariant", "N");
        callContext.put("userLogin", demowarehouse1);
        Map<String, Object> product = runAndAssertServiceSuccess("createProduct", callContext);
        String productId = (String) product.get("productId");

        //  receive 1  of the product at unit cost 0.1
        Map<String, Object> commonParameters = new HashMap<String, Object>();
        commonParameters.put("productId", productId);
        commonParameters.put("facilityId", "WebStoreWarehouse");
        commonParameters.put("currencyUomId", "USD");
        commonParameters.put("quantityRejected", BigDecimal.ZERO);
        commonParameters.put("userLogin", demowarehouse1);
        Map<String, Object> input = new HashMap<String, Object>();
        input.putAll(commonParameters);
        input.putAll(UtilMisc.toMap("inventoryItemTypeId", "NON_SERIAL_INV_ITEM"
                , "unitCost", new BigDecimal("0.1"), "quantityAccepted" , new BigDecimal("1.0")
                , "datetimeReceived", UtilDateTime.nowTimestamp()));
        Map output = runAndAssertServiceSuccess("receiveInventoryProduct", input);
        String inventoryItemId = (String) output.get("inventoryItemId");
        //  track the previous inventory item unit cost
        List<GenericValue> inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
        GenericValue firstInventoryItem = inventoryItems.get(0);
        BigDecimal previousUnitCost = firstInventoryItem.getBigDecimal("unitCost");
        Debug.logInfo("previousUnitCost : " + previousUnitCost, MODULE);
        pause("allow distinct product average cost timestamps");


        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(User));
        InventoryDomainInterface inventoryDomain = dl.loadDomainsDirectory().getInventoryDomain();
        InventoryRepositoryInterface inventoryRepository = inventoryDomain.getInventoryRepository();

        // for i = 1 to 1000
        //     update the inventory item.unit cost to i
        for (int i = 1; i < LOOP_TESTS; i++) {
            firstInventoryItem.set("unitCost", new BigDecimal(i));
            firstInventoryItem.store();
            // Verify that the  previous inventory item unit cost is the same as the one retrieved from InventoryItem
            InventoryItem item = inventoryRepository.getInventoryItemById(firstInventoryItem.getString("inventoryItemId"));
            InventoryItemValueHistory inventoryItemValueHistory = inventoryRepository.getLastInventoryItemValueHistoryByInventoryItem(item);
            BigDecimal inventoryItemValueHistoryUnitCost = inventoryItemValueHistory.getUnitCost();
            assertEquals("Product [" + productId + "]  previous inventory item unit cost should be " + previousUnitCost + ".", previousUnitCost, inventoryItemValueHistoryUnitCost);
        }
    }

    /**
     * Test that the transactionDate of accounting transaction will be the invoice's invoiceDate, not the current timestamp.
     * @throws GeneralException if an error occurs
     */
    public void testInvoiceTransactionPostingDate() throws GeneralException {
        // create a customer
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "account for testing invoice posting transaction date");

        // create a sales invoice to the customer 30 days in the future
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Timestamp invoiceDate = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, timeZone, locale);
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", invoiceDate, null, null, null);
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1000", new BigDecimal("2.0"), new BigDecimal("15.0"));

        // set invoice to READY
        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        // check that the accounting transaction's transactionDate is the same as the invoice's invoiceDate
        LedgerRepositoryInterface ledgerRepository = ledgerDomain.getLedgerRepository();
        List<AccountingTransaction> invoiceAcctgTransList = ledgerRepository.findList(AccountingTransaction.class, ledgerRepository.map(org.opentaps.base.entities.AcctgTrans.Fields.invoiceId, invoiceId));
        assertNotNull("An accounting transaction was not found for invoice [" + invoiceId + "]", invoiceAcctgTransList);
        AccountingTransaction acctgTrans = invoiceAcctgTransList.get(0);

        // note: it is important to get the actual invoiceDate stored in the Invoice entity, not the invoiceDate from above
        // for example, mysql might store 2009-06-12 12:46:30.309 as 2009-06-12 12:46:30.0, so your comparison won't equal
        InvoiceRepositoryInterface repository = billingDomain.getInvoiceRepository();
        Invoice invoice = repository.getInvoiceById(invoiceId);
        assertEquals("Transaction date and invoice date do not equal", acctgTrans.getTransactionDate(), invoice.getInvoiceDate());
    }

    /**
     * Test to verify that payments are posted to the effectiveDate of the payment.
     * @throws GeneralException if an error occurs
     */
    public void testPaymentTransactionPostingDate() throws GeneralException {
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "account for testing payment posting transaction date");
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Timestamp paymentDate = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, timeZone, locale);
        String paymentId = financialAsserts.createPayment(new BigDecimal("1.0"), customerPartyId, "CUSTOMER_PAYMENT", "CASH");

        PaymentRepositoryInterface paymentRepository = billingDomain.getPaymentRepository();
        Payment payment = paymentRepository.getPaymentById(paymentId);
        payment.setEffectiveDate(paymentDate);
        paymentRepository.createOrUpdate(payment);

        financialAsserts.updatePaymentStatus(paymentId, "PMNT_RECEIVED");

        LedgerRepositoryInterface ledgerRepository = ledgerDomain.getLedgerRepository();
        List<AccountingTransaction> paymentAcctgTransList = ledgerRepository.findList(AccountingTransaction.class, ledgerRepository.map(org.opentaps.base.entities.AcctgTrans.Fields.paymentId, paymentId));
        assertNotNull("An accounting transaction was not found for payment [" + paymentId + "]", paymentAcctgTransList);
        AccountingTransaction acctgTrans = paymentAcctgTransList.get(0);

        payment = paymentRepository.getPaymentById(paymentId);

        assertEquals("Transaction date and invoice date do not equal", acctgTrans.getTransactionDate(), payment.getEffectiveDate());
    }

    /**
     * Tests basic methods of Invoice class for sales invoice.
     * @exception GeneralException if an error occurs
     */
    public void testInvoiceMethodsForSalesInvoice() throws GeneralException {
        //create a SALES_INVOICE from Company to another party
        InvoiceRepositoryInterface repository = billingDomain.getInvoiceRepository();

        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Account for testInvoiceMethodsForSalesInvoice");
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // create invoice
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1000", new BigDecimal("2.0"), new BigDecimal("15.0"));

        // Check the Invoice.isReceivable() is true
        Invoice invoice = repository.getInvoiceById(invoiceId);
        assertTrue("Invoice with ID [" + invoiceId + "] should be receivable.", invoice.isReceivable());

        // Check the Invoice.isPayable() is false
        assertFalse("Invoice with ID [" + invoiceId + "] should not payable.", invoice.isPayable());

        // Check the Invoice.getOrganizationPartyId() is Company
        assertEquals("Invoice.getOrganizationPartyId() should be " + organizationPartyId, organizationPartyId, invoice.getOrganizationPartyId());
        // Check the Invoice.getTransactionPartyId() is the other party
        assertEquals("Invoice.getTransactionPartyId() should be " + customerPartyId, customerPartyId, invoice.getTransactionPartyId());

    }

    /**
     * Tests basic methods of Invoice class for purchase invoice.
     * @exception GeneralException if an error occurs
     */
    public void testInvoiceMethodsForPurchaseInvoice() throws GeneralException {
        InvoiceRepositoryInterface repository = billingDomain.getInvoiceRepository();
        String supplierPartyId = createPartyFromTemplate("DemoSupplier", "Account for testInvoiceMethodsForPurchaseInvoice");
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // create a PURCHASE_INVOICE from another party to Company
        String invoiceId = financialAsserts.createInvoice(supplierPartyId, "PURCHASE_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        financialAsserts.createInvoiceItem(invoiceId, "PINV_FPROD_ITEM", "GZ-1000", new BigDecimal("100.0"), new BigDecimal("4.56"));

        // Check the Invoice.isReceivable() is false
        Invoice invoice = repository.getInvoiceById(invoiceId);
        assertFalse("Invoice with ID [" + invoiceId + "] should not receivable.", invoice.isReceivable());

        // Check the Invoice.isPayable() is true
        assertTrue("Invoice with ID [" + invoiceId + "] should be payable.", invoice.isPayable());

        // Check Invoice.getOrganizationPartyId() is Company
        assertEquals("Invoice.getOrganizationPartyId() should be " + organizationPartyId, organizationPartyId, invoice.getOrganizationPartyId());

        // Check the Invoice.getTransactionPartyId() is the other party
        assertEquals("Invoice.getTransactionPartyId() should be " + supplierPartyId, supplierPartyId, invoice.getTransactionPartyId());

    }

    /**
     * Test for sales invoice and payment.
     * Note: this test relies on the status of previously created Invoices so it will fail if you run
     *  it a second time without rebuilding the DB
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testSalesInvoicePayment() throws GeneralException {

        DomainsDirectory dd = new DomainsLoader(new Infrastructure(dispatcher), new User(demofinadmin)).loadDomainsDirectory();
        InvoiceRepositoryInterface repository = dd.getBillingDomain().getInvoiceRepository();

        // before we begin, note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();
        Timestamp now = start;

        // note the initial balances
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Account for testSalesInvoicePayment");
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Map initialBalances = financialAsserts.getFinancialBalances(start);
        BigDecimal balance1 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", now, delegator);

        // create invoice and set it to ready
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);

        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1000", new BigDecimal("2.0"), new BigDecimal("15.0"));
        financialAsserts.createInvoiceItem(invoiceId, "ITM_PROMOTION_ADJ", "GZ-1000", new BigDecimal("1.0"), new BigDecimal("-10.0"));
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1001", new BigDecimal("1.0"), new BigDecimal("45.0"));
        financialAsserts.createInvoiceItem(invoiceId, "ITM_SHIPPING_CHARGES", null, null, new BigDecimal("12.95"));

        // Check the invoice current status
        Invoice invoice = repository.getInvoiceById(invoiceId);
        assertNotNull("Invoice with ID [" + invoiceId + "] not found.", invoice);
        assertEquals("Invoice with ID [" + invoiceId + "] should have the INVOICE_IN_PROCESS status", "INVOICE_IN_PROCESS", invoice.getString("statusId"));

        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        now = UtilDateTime.nowTimestamp();
        BigDecimal balance2 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + customerPartyId + " has not increased by 77.95", balance2.subtract(new BigDecimal("77.95")), balance1);

        Map halfBalances = financialAsserts.getFinancialBalances(now);
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "77.95");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, halfBalances, accountMap);

        // create payment and set it to received
        String paymentId = financialAsserts.createPaymentAndApplication(new BigDecimal("77.95"), customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "COMPANY_CHECK", null, invoiceId, "PMNT_NOT_PAID");
        financialAsserts.updatePaymentStatus(paymentId, "PMNT_RECEIVED");

        invoice = repository.getInvoiceById(invoiceId);
        assertNotNull("Invoice with ID [" + invoiceId + "] not found.", invoice);
        assertEquals("Invoice with ID [" + invoiceId + "] should have the INVOICE_PAID status", "INVOICE_PAID", invoice.getString("statusId"));

        now = UtilDateTime.nowTimestamp();
        BigDecimal balance3 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + customerPartyId + " has not decreased by 77.95", balance3.add(new BigDecimal("77.95")), balance2);

        // get the transactions for the payment with the assistance of our start timestamp
        Set<String> transactions = getAcctgTransSinceDate(UtilMisc.toList(EntityCondition.makeCondition("paymentId", EntityOperator.EQUALS, paymentId)), start, delegator);
        assertNotEmpty(paymentId + " transaction not created.", transactions);

        // assert transaction equivalence with the reference PMT_CUST_TEST-1 transaction
        assertTransactionEquivalenceIgnoreParty(transactions, UtilMisc.toList("PMT_CUST_TEST-1"));

        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "-77.95", "UNDEPOSITED_RECEIPTS", "77.95");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(halfBalances, finalBalances, accountMap);
    }

    /**
     * These tests verify what happens when you void a payment.
     * 1 - get the initial balance for the customer and financial balances
     * 2 - create a payment and set status to PMNT_VOID
     * 3 - get the new balance for the customer and financial balances
     * 4 - verify that the balance for the customer has increased by 77.95
     * 5 - verify that the balance for ACCOUNTS_RECEIVABLE has increased by 77.95, and the balance for UNDEPOSITED_RECEIPTS has decreased by 77.95
     *
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testVoidPayment() throws GeneralException {
        DomainsDirectory dd = new DomainsLoader(new Infrastructure(dispatcher), new User(demofinadmin)).loadDomainsDirectory();
        InvoiceRepositoryInterface repository = dd.getBillingDomain().getInvoiceRepository();

        // before we begin, note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();
        Timestamp now = start;

        // note the initial balances
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Account for testVoidPayment");
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Map initialBalances = financialAsserts.getFinancialBalances(start);
        BigDecimal balance1 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", now, delegator);

        // create invoice and set it to ready
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);

        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1000", new BigDecimal("2.0"), new BigDecimal("15.0"));
        financialAsserts.createInvoiceItem(invoiceId, "ITM_PROMOTION_ADJ", "GZ-1000", new BigDecimal("1.0"), new BigDecimal("-10.0"));
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1001", new BigDecimal("1.0"), new BigDecimal("45.0"));
        financialAsserts.createInvoiceItem(invoiceId, "ITM_SHIPPING_CHARGES", null, null, new BigDecimal("12.95"));

        // Update the Invoice status
        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        now = UtilDateTime.nowTimestamp();
        BigDecimal balance2 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + customerPartyId + " has not increased by 77.95", balance2.subtract(new BigDecimal("77.95")), balance1);

        Map afterSettingInvoiceReadyBalances = financialAsserts.getFinancialBalances(now);
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "77.95");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, afterSettingInvoiceReadyBalances, accountMap);


        // create payment and set it to received
        String paymentId = financialAsserts.createPaymentAndApplication(new BigDecimal("77.95"), customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "COMPANY_CHECK", null, invoiceId, "PMNT_NOT_PAID");
        financialAsserts.updatePaymentStatus(paymentId, "PMNT_RECEIVED");


        Invoice invoice = repository.getInvoiceById(invoiceId);
        assertNotNull("Invoice with ID [" + invoiceId + "] not found.", invoice);
        assertEquals("Invoice with ID [" + invoiceId + "] should have the INVOICE_PAID status", "INVOICE_PAID", invoice.getString("statusId"));

        now = UtilDateTime.nowTimestamp();
        BigDecimal balance3 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + customerPartyId + " has not decrease by 77.95", balance3.add(new BigDecimal("77.95")), balance2);

        // get the transactions for payment with the assistance of our start timestamp
        Set<String> transactions = getAcctgTransSinceDate(UtilMisc.toList(EntityCondition.makeCondition("paymentId", EntityOperator.EQUALS, paymentId)), start, delegator);
        assertNotEmpty(paymentId + " transaction not created.", transactions);

        // assert transaction equivalence with the reference payment transaction
        assertTransactionEquivalenceIgnoreParty(transactions, UtilMisc.toList("PMT_CUST_TEST-2"));

        Map afterSettingPaymentReceivedBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "-77.95", "UNDEPOSITED_RECEIPTS", "77.95");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(afterSettingInvoiceReadyBalances, afterSettingPaymentReceivedBalances, accountMap);

        pause("avoid having two invoice status with same timestamp");

        // void the payment
        start = UtilDateTime.nowTimestamp();
        Map<String, Object> input = UtilMisc.toMap("userLogin", demofinadmin, "paymentId", paymentId);
        runAndAssertServiceSuccess("voidPayment", input);

        // check that the reference invoice status changed back to INVOICE_READY
        invoice = repository.getInvoiceById(invoiceId);
        assertNotNull("Invoice with ID [" + invoiceId + "] not found.", invoice);
        assertEquals("Invoice with ID [" + invoiceId + "] should have the INVOICE_READY status", "INVOICE_READY", invoice.getString("statusId"));

        // verify that the balance for the customer has increased by 77.95
        now = UtilDateTime.nowTimestamp();
        BigDecimal balance4 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + customerPartyId + " has not increased by 77.95", balance3, balance4.subtract(new BigDecimal("77.95")));

        // get the reversed transactions for payment, their date will be the same as the date of the reversed transaction
        // so checking the created stamp field instead of the transaction date here
        transactions = getAcctgTransCreatedSinceDate(UtilMisc.toList(EntityCondition.makeCondition("paymentId", EntityOperator.EQUALS, paymentId)), start, delegator);
        assertNotEmpty(paymentId + " payment void transactions not created.", transactions);

        // assert transaction equivalence with the reference PMT_CUST_TEST-2R transaction
        assertTransactionEquivalenceIgnoreParty(transactions, UtilMisc.toList("PMT_CUST_TEST-2R"));

        // verify that the balance for ACCOUNTS_RECEIVABLE has increased by 77.95, and the balance for UNDEPOSITED_RECEIPTS has decreased by 77.95
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "77.95", "UNDEPOSITED_RECEIPTS", "-77.95");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(afterSettingPaymentReceivedBalances, finalBalances, accountMap);
    }

    /**
     * This test verifies what happens when you write off an invoice.
     * 1 - get the initial balance for the customer and financial balances
     * 2 - create a invoice and set status to INVOICE_WRITEOFF
     * 3 - get the new balance for customer DemoAccount1 and financial balances
     * 4 - verify that the balance for the customer has decreased by 77.95
     * 5 - verify that the balance for ACCOUNTS_RECEIVABLE has decreased by 77.95, and the balance for ACCTRECV_WRITEOFF has increased by 77.95
     *
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInvoiceWriteOff() throws GeneralException {
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Account for testInvoiceWriteOff");

        // before we begin, note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();
        Timestamp now = start;

        // note the initial balances
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Map initialBalances = financialAsserts.getFinancialBalances(start);
        BigDecimal balance1 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", now, delegator);

        // create invoice and set it to ready
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1000", new BigDecimal("2.0"), new BigDecimal("15.0"));
        financialAsserts.createInvoiceItem(invoiceId, "ITM_PROMOTION_ADJ", "GZ-1000", new BigDecimal("1.0"), new BigDecimal("-10.0"));
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1001", new BigDecimal("1.0"), new BigDecimal("45.0"));
        financialAsserts.createInvoiceItem(invoiceId, "ITM_SHIPPING_CHARGES", null, null,  new BigDecimal("12.95"));
        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        now = UtilDateTime.nowTimestamp();
        BigDecimal balance2 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + customerPartyId + " should increase by 77.95", balance2.subtract(balance1), new BigDecimal("77.95"));

        Map afterSettingInvoiceReadyBalances = financialAsserts.getFinancialBalances(now);
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "77.95");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, afterSettingInvoiceReadyBalances, accountMap);

        // Update the Invoice status to INVOICE_WRITEOFF
        start = UtilDateTime.nowTimestamp();
        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_WRITEOFF");

        // verify that the balance for the customer has decreased by 77.95
        now = UtilDateTime.nowTimestamp();
        BigDecimal balance3 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + customerPartyId + " has not decrease by 77.95", balance3.subtract(balance2), new BigDecimal("-77.95"));

        // get the transactions for INV_SALES_TEST-3 with the assistance of our start timestamp
        Set<String> transactions = getAcctgTransSinceDate(UtilMisc.toList(EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, invoiceId),
                EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "WRITEOFF"),
                EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.NOT_EQUAL, "REFERENCE")), start, delegator);
        assertNotEmpty(invoiceId + " invoice write off transactions not created.", transactions);

        // assert transaction equivalence with the reference INV_SALES_TEST-3WO transaction
        assertTransactionEquivalence(transactions, UtilMisc.toList("INV_SALES_TEST-3WO"));

        // figure out what the sales invoice writeoff account is using the domain
        DomainsDirectory dd = new DomainsLoader(new Infrastructure(dispatcher), new User(demofinadmin)).loadDomainsDirectory();
        LedgerRepositoryInterface ledgerRepository = dd.getLedgerDomain().getLedgerRepository();
        InvoiceAdjustmentGlAccount writeoffGlAcct = ledgerRepository.getInvoiceAdjustmentGlAccount(organizationPartyId, "SALES_INVOICE", "WRITEOFF");
        assertNotNull(writeoffGlAcct);

        // verify that the balance for ACCOUNTS_RECEIVABLE has decreased by 77.95, and the balance for ACCTRECV_WRITEOFF has increased by 77.95
        Map afterWritingOffInvoiceBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "-77.95");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(writeoffGlAcct.getGlAccountId(), "77.95");
        assertMapDifferenceCorrect(afterSettingInvoiceReadyBalances, afterWritingOffInvoiceBalances, accountMap);
    }

    /**
     * Test for purchase invoice and vendor payment.
     * Note: this test relies on the status of previously created Invoices so it will fail if you run
     *  it a second time without rebuilding the DB
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPurchaseInvoicePayment() throws GeneralException {
        DomainsDirectory dd = new DomainsLoader(new Infrastructure(dispatcher), new User(demofinadmin)).loadDomainsDirectory();
        InvoiceRepositoryInterface repository = dd.getBillingDomain().getInvoiceRepository();
        // before we begin, note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();
        Timestamp now = start;

        // note the initial balances
        String supplierPartyId = createPartyFromTemplate("DemoSupplier", "Supplier for testPurchaseInvoicePayment");
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Map initialBalances = financialAsserts.getFinancialBalances(start);
        BigDecimal balance1 = AccountsHelper.getBalanceForCustomerPartyId(supplierPartyId, organizationPartyId, "ACTUAL", now, delegator);

        // create invoice
        String invoiceId = financialAsserts.createInvoice(supplierPartyId, "PURCHASE_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        financialAsserts.createInvoiceItem(invoiceId, "PINV_FPROD_ITEM", "GZ-1000", new BigDecimal("100.0"), new BigDecimal("4.56"));
        financialAsserts.createInvoiceItem(invoiceId, "PITM_SHIP_CHARGES", new BigDecimal("1.0"), new BigDecimal("13.95"));
        financialAsserts.createInvoiceItem(invoiceId, "PINV_SUPLPRD_ITEM", new BigDecimal("1.0"), new BigDecimal("56.78"));

        // Check the invoice current status
        Invoice invoice = repository.getInvoiceById(invoiceId);
        assertNotNull("Invoice with ID [" + invoiceId + "] not found.", invoice);
        assertEquals("Invoice with ID [" + invoiceId + "] should have the INVOICE_IN_PROCESS status", "INVOICE_IN_PROCESS", invoice.getString("statusId"));

        // Update the Invoice status
        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        now = UtilDateTime.nowTimestamp();
        BigDecimal balance2 = AccountsHelper.getBalanceForVendorPartyId(supplierPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + supplierPartyId + " has not increase by 526.73", balance1, balance2.subtract(new BigDecimal("526.73")));

        // get the transactions for INV_PURCH_TEST-1 with the assistance of our start timestamp
        Set<String> transactions = getAcctgTransSinceDate(UtilMisc.toList(EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, invoiceId)), start, delegator);
        assertNotEmpty(invoiceId + " transaction not created.", transactions);

        // assert transaction equivalence with the reference INV_PURCH_TEST-1 transaction
        assertTransactionEquivalenceIgnoreParty(transactions, UtilMisc.toList("INV_PURCH_TEST-1"));

        Map halfBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "-526.73", "UNINVOICED_SHIP_RCPT", "456.0");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.putAll(UtilMisc.toMap("510000", "13.95", "650000", "56.78"));
        assertMapDifferenceCorrect(initialBalances, halfBalances, accountMap);


        // create payment and set it to sent
        String paymentId = financialAsserts.createPaymentAndApplication(new BigDecimal("526.73"), organizationPartyId, supplierPartyId, "VENDOR_PAYMENT", "COMPANY_CHECK", "COCHECKING", invoiceId, "PMNT_NOT_PAID");
        financialAsserts.updatePaymentStatus(paymentId, "PMNT_SENT");

        invoice = repository.getInvoiceById(invoiceId);
        assertNotNull("Invoice with ID [" + invoiceId + "] not found.", invoice);
        assertEquals("Invoice with ID [" + invoiceId + "] should have the INVOICE_PAID status", "INVOICE_PAID", invoice.getString("statusId"));

        now = UtilDateTime.nowTimestamp();
        BigDecimal balance3 = AccountsHelper.getBalanceForVendorPartyId(supplierPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + supplierPartyId + " has not decrease by 526.73", balance2, balance3.add(new BigDecimal("526.73")));

        // get the transactions for PMT_VEND_TEST-1 with the assistance of our start timestamp
        transactions = getAcctgTransSinceDate(UtilMisc.toList(EntityCondition.makeCondition("paymentId", EntityOperator.EQUALS, paymentId)), start, delegator);
        assertNotEmpty(paymentId + " transaction not created.", transactions);

        // assert transaction equivalence with the reference PMT_VEND_TEST-1 transaction
        assertTransactionEquivalenceIgnoreParty(transactions, UtilMisc.toList("PMT_VEND_TEST-1"));

        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "526.73");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(halfBalances, finalBalances, accountMap);

    }


    /**
     * Finance charges test:
     * Setup
     * 1.  Create sales invoice (#1) from Company to customer1PartyId for 1 item of $100, invoiceDate = 60 days before today.  Set invoice to ready.
     * 2.  Create sales invoice (#2) from Company to customer2PartyId for 2 items of $250 each, invoiceDate = 45 days before today.  Set invoice to ready.
     * 3.  Create sales invoice (#3) from Company to customer1PartyId for 1 item of $250, invoiceDate = 20 days before today.  Set invoice to ready.
     * 4.  Create sales invoice (#4) from Company to DemoPrivilegedCust for 2 items of $567.89 each, invoiceDate = 60 days before today.  Set invoice to ready
     * Verify basic interest calculations
     * 5.  Run AccountHelper.calculateFinanceCharges with grace period = 30 days and interest rate = 8.0 and verify that the results are:
     *     Invoice #1 - finance charge of $0.66 ((60 - 30) / 365.25 * 0.08 * 100)
     *     Invoice #2 - finance charge of $1.64 ((45 - 30) / 365.25 * 0.08 * 500)
     *     Invoice #4 - finance charge of $7.46 ((60 - 30) / 365.25 * 0.08 * 1135.78)
     * Verify party classification filtering
     * 6.  Run AccountsHelper.calculateFinanceCharges with grace period = 30 days, interest rate = 8.0, and partyClassificationGroup = Prileged Customers, verify that:
     *     Only Invoice #4 shows up as before
     * Verify party filtering
     * 7.  Run AccountsHelper.calculateFinanceCharges with grace period = 30 days, interest rate = 8.0, and partyId = DemoAccount1, verify that:
     *     Only Invoice #1 shows up as before
     * Verify that writing off an invoice will not cause more finance charges to be collected
     * 8.  Set Invoice #1 status to INVOICE_WRITEOFF, Run AccountHelper.calculateFinanceCharges with grace period = 30 days and interest rate = 8.0 and verify that the results are:
     *     Verify only Invoice #2 and #4 show up now with amounts as above
     * Verify that creating finance charges will create the right financial balance transactions
     * 9.  Get INTRSTINC_RECEIVABLE and INTEREST_INCOME balance for the Company and the Accounts balance for DemoCustCompany and DemoPrivilegedCust
     *     Run financials.createInterestInvoice on each of the invoice from step (8).
     *     Verify that INTRSTINC_RECEIVABLE and INTEREST_INCOME have both increased by $9.10, the balance of DemoCustCompany has increased by $1.64, the balance of DemoPrivilegedCust has increassed by $7.46
     * Verify the next cycle's interest calculations are correct
     * 10.  Run AccountsHelper.calculateFinanceCharges 30 days further in the future (asOfDateTime parameter), and verify that the results are:
     *      Invoice #2 - previous finance charges = $1.64, new finance charges of $3.29 ((75 - 30) * 365.25 * 0.08 * 500 - 1.64)
     *      Invoice #4 - previous finance charges = $7.46, new finance charges of $7.47 ((90 - 30) * 365.25 * 0.08 * 1135.78 - 7.46)
     * Verify that writing off a finance charge has the right effect on both accounting and on interest calculations
     * 11.  Get INTRSTINC_RECEIVABLE and the interest invoice writeoff account balances for Company and the Accounts balance for DemoPrivilegedCust.
     *      Write off the finance charge (invoice) created for DemoPrivilegedCust in step (9)
     *      Verify that both INTRSTINC_RECEIVABLE and invoice writeoff account balances have increased by $7.46
     * 12.  Run AccountsHelper.calculateFinanceCharges 30 days further in the future (asOfDateTime parameter), and verify that the results are:
     *      Invoice #2 - previous finance charges = $1.64, new finance charges of $3.29 ((75 - 30) * 365.25 * 0.08 * 500 - 1.64)
     *      Invoice #4 - previous finance charges = $0.00, new finance charges of $14.93 ((90 - 30) * 365.25 * 0.08 * 1135.78)
     * Verify that payments would serve to reduce invoice finance charges //
     * 13.  Create customer payment for $250 from DemoPrivilegedCust to Company and apply it to invoice #4.  Set the payment to RECEIVED.
     *      Run AccountsHelper.calculateFinanceCharges 30 days further in the future (asOfDateTime parameter), and verify that the results are:
     *      Invoice #2 - previous finance charges = $1.64, new finance charges of $3.29 ((75 - 30) * 365.25 * 0.08 * 500 - 1.64)
     *      Invoice #4 - previous finance charges = $0, new finance charges of $11.64 ((90 - 30) * 365.25 * 0.08 * 885.78)
     *
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testFinanceCharges() throws GeneralException {

        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        /*
         * Create sales invoice (#1) from Company to DemoAccount1
         * for 1 item of $100, invoiceDate = 60 days before today.
         * Set invoice to ready.
         */
        String customer1PartyId = createPartyFromTemplate("DemoAccount1", "Account1 for testFinanceCharges");
        String customer2PartyId = createPartyFromTemplate("DemoCustCompany", "Account2 for testFinanceCharges");

        String invoiceId1 = fa.createInvoice(customer1PartyId, "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -60, timeZone, locale));
        fa.createInvoiceItem(invoiceId1, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("100.0"));

        // set invoice status to READY is successful
        fa.updateInvoiceStatus(invoiceId1, "INVOICE_READY");

        /*
         * Create sales invoice (#2) from Company to DemoCustCompany
         * for 2 items of $250 each, invoiceDate = 45 days before today.
         * Set invoice to ready.
         */
        String invoiceId2 = fa.createInvoice(customer2PartyId, "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -45, timeZone, locale));
        fa.createInvoiceItem(invoiceId2, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("2.0"), new BigDecimal("250.0"));

        // set invoice status to READY is successful
        fa.updateInvoiceStatus(invoiceId2, "INVOICE_READY");

        /*
         * Create sales invoice (#3) from Company to DemoAccount1
         * for 1 item of $250, invoiceDate = 20 days before today.
         * Set invoice to ready.
         */
        String invoiceId3 = fa.createInvoice(customer1PartyId, "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -20, timeZone, locale));
        fa.createInvoiceItem(invoiceId3, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("250.0"));

        // set invoice status to READY is successful
        fa.updateInvoiceStatus(invoiceId3, "INVOICE_READY");

        /*
         * Create sales invoice (#4) from Company to DemoPrivilegedCust
         * for 2 items of $567.89 each, invoiceDate = 60 days before today.
         * Set invoice to ready
         */
        String invoiceId4 = fa.createInvoice("DemoPrivilegedCust", "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -60, timeZone, locale));
        fa.createInvoiceItem(invoiceId4, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("2.0"), new BigDecimal("567.89"));

        // set invoice status to READY is successful
        fa.updateInvoiceStatus(invoiceId4, "INVOICE_READY");

        /*
         * Verify basic interest calculations
         * Run AccountHelper.calculateFinanceCharges with grace
         * period = 30 days and interest rate = 8.0
         */
        InvoiceRepository repository = new InvoiceRepository(delegator);
        Map<Invoice, Map<String, BigDecimal>> financeCharges = AccountsHelper.calculateFinanceCharges(delegator, organizationPartyId, null, null, new BigDecimal("8.0"), UtilDateTime.nowTimestamp(), 30, timeZone, locale);
        Invoice invoice1 = repository.getInvoiceById(invoiceId1);
        Invoice invoice2 = repository.getInvoiceById(invoiceId2);
        Invoice invoice4 = repository.getInvoiceById(invoiceId4);

        // Note that these and all the tests below have a subtle test of the hashCode() function for the Invoice object, as
        // the Invoices are not the same Java objects but are two objects of the same invoice, but if their hashCode() are
        // equal, then they should be equal, and the Map get method should still work.

        /*
         * verify that the results are:
         * Invoice #1 - finance charge of $0.66 ((60 - 30) / 365.25 * 0.08 * 100)
         * Invoice #2 - finance charge of $1.64 ((45 - 30) / 365.25 * 0.08 * 500)
         * Invoice #4 - finance charge of $7.46 ((60 - 30) / 365.25 * 0.08 * 1135.78)
         */
        Map<String, BigDecimal> expectedFinanceCharges = UtilMisc.toMap(invoice1.getInvoiceId(), new BigDecimal("0.66"), invoice2.getInvoiceId(), new BigDecimal("1.64"), invoice4.getInvoiceId(), new BigDecimal("7.46"));
        fa.assertFinanceCharges(financeCharges, expectedFinanceCharges);

        /*
         * Verify party classification filtering
         * Run AccountsHelper.calculateFinanceCharges with grace
         * period = 30 days, interest rate = 8.0, and
         * partyClassificationGroup = Prileged Customers
         */
        financeCharges = AccountsHelper.calculateFinanceCharges(delegator, organizationPartyId, null, "PRIVILEGED_CUSTOMERS", new BigDecimal("8.0"), UtilDateTime.nowTimestamp(), 30, timeZone, locale);

        /*
         * verify that:
         * Only Invoice #4 shows up as before
         */
        expectedFinanceCharges = UtilMisc.toMap(invoice1.getInvoiceId(), null, invoice2.getInvoiceId(), null, invoice4.getInvoiceId(), new BigDecimal("7.46"));
        fa.assertFinanceCharges(financeCharges, expectedFinanceCharges);

        /*
         * Verify party filtering
         * Run AccountsHelper.calculateFinanceCharges with grace
         * period = 30 days, interest rate = 8.0, and
         * partyId = DemoAccount1
         */
        financeCharges = AccountsHelper.calculateFinanceCharges(delegator, organizationPartyId, customer1PartyId, null, new BigDecimal("8.0"), UtilDateTime.nowTimestamp(), 30, timeZone, locale);

        /*
         * verify that:
         * Only Invoice #1 shows up as before
         */
        expectedFinanceCharges = UtilMisc.toMap(invoice1.getInvoiceId(), new BigDecimal("0.66"), invoice2.getInvoiceId(), null, invoice4.getInvoiceId(), null);
        fa.assertFinanceCharges(financeCharges, expectedFinanceCharges);

        /*
         * Verify that writing off an invoice will not
         * cause more finance charges to be collected
         * Set Invoice #1 status to INVOICE_WRITEOFF,
         * Run AccountHelper.calculateFinanceCharges with grace
         * period = 30 days and interest rate = 8.0
         */
        fa.updateInvoiceStatus(invoiceId1, "INVOICE_WRITEOFF");

        financeCharges = AccountsHelper.calculateFinanceCharges(delegator, organizationPartyId, null, null, new BigDecimal("8.0"), UtilDateTime.nowTimestamp(), 30, timeZone, locale);

        /*
         * Verify only Invoice #2 and #4
         * show up now with amounts as above
         */
        expectedFinanceCharges = UtilMisc.toMap(invoice1.getInvoiceId(), null, invoice2.getInvoiceId(), new BigDecimal("1.64"), invoice4.getInvoiceId(), new BigDecimal("7.46"));
        fa.assertFinanceCharges(financeCharges, expectedFinanceCharges);

        /*
         * Verify that creating finance charges will
         * create the right financial balance transactions
         * Get ACCOUNTS_RECEIVABLE and INTEREST_INCOME
         * balance for the Company and the Accounts balance
         * for DemoCustCompany and DemoPrivilegedCust
         * Run financials.createInterestInvoice on each
         * of the invoice from step (8).
         */
        Timestamp now = UtilDateTime.nowTimestamp();
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        BigDecimal balance1_democustcompany = AccountsHelper.getBalanceForCustomerPartyId(customer2PartyId, organizationPartyId, "ACTUAL", now, delegator);
        BigDecimal balance1_demoprivilegedcust = AccountsHelper.getBalanceForCustomerPartyId("DemoPrivilegedCust", organizationPartyId, "ACTUAL", now, delegator);

        Map initialBalances = financialAsserts.getFinancialBalances(now);

        financeCharges = AccountsHelper.calculateFinanceCharges(delegator, organizationPartyId, null, null, new BigDecimal("8.0"), now, 30, timeZone, locale);

        Map<String, BigDecimal> financeCharge = financeCharges.get(invoice2);
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", demofinadmin);
        input.put("partyIdFrom", invoice2.get("partyIdFrom"));
        input.put("partyIdTo", invoice2.get("partyId"));
        input.put("amount", financeCharge.get("interestAmount"));
        input.put("currencyUomId", invoice2.get("currencyUomId"));
        input.put("invoiceDate", now);
        input.put("dueDate", invoice2.get("dueDate"));
        input.put("description", invoice2.get("description"));
        input.put("parentInvoiceId", invoice2.get("invoiceId"));
        Map<String, Object> output = runAndAssertServiceSuccess("financials.createInterestInvoice", input);

        financeCharge = financeCharges.get(invoice4);
        input = new HashMap<String, Object>();
        input.put("userLogin", demofinadmin);
        input.put("partyIdFrom", invoice4.get("partyIdFrom"));
        input.put("partyIdTo", invoice4.get("partyId"));
        input.put("amount", financeCharge.get("interestAmount"));
        input.put("currencyUomId", invoice4.get("currencyUomId"));
        input.put("invoiceDate", now);
        input.put("dueDate", invoice4.get("dueDate"));
        input.put("description", invoice4.get("description"));
        input.put("parentInvoiceId", invoice4.get("invoiceId"));
        output = runAndAssertServiceSuccess("financials.createInterestInvoice", input);

        String invoiceId4Interest = (String) output.get("invoiceId");

        // reload invoices (to get the updated interest charged)
        invoice2 = repository.getInvoiceById(invoiceId2);
        invoice4 = repository.getInvoiceById(invoiceId4);

        /*
         * Verify that INTRSTINC_RECEIVABLE and INTEREST_INCOME
         * have both increased by $9.10, the balance of
         * DemoCustCompany has increased by $1.64, the
         * balance of DemoPrivilegedCust has increassed by $7.46
         */
        now = UtilDateTime.nowTimestamp();

        BigDecimal balance2_democustcompany = AccountsHelper.getBalanceForCustomerPartyId(customer2PartyId, organizationPartyId, "ACTUAL", now, delegator);
        BigDecimal balance2_demoprivilegedcust = AccountsHelper.getBalanceForCustomerPartyId("DemoPrivilegedCust", organizationPartyId, "ACTUAL", now, delegator);

        assertEquals("the balance of DemoCustCompany should increased by $1.64",
                balance2_democustcompany, balance1_democustcompany.add(new BigDecimal("1.64")));
        assertEquals("the balance of DemoPrivilegedCust should increased by $7.46",
                balance2_demoprivilegedcust, balance1_demoprivilegedcust.add(new BigDecimal("7.46")));

        Map finalBalances = financialAsserts.getFinancialBalances(now);

        Map expectedBalanceChanges = UtilMisc.toMap("INTRSTINC_RECEIVABLE", "9.10", "INTEREST_INCOME", "9.10");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);

        /*
         * Verify the next cycle's interest calculations are
         * correct. Run AccountsHelper.calculateFinanceCharges 30
         * days further in the future (asOfDateTime parameter)
         */
        financeCharges = AccountsHelper.calculateFinanceCharges(delegator, organizationPartyId, null, null, new BigDecimal("8.0"),
                UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, timeZone, locale), 30, timeZone, locale);

        /*
         * verify that the results are:
         * Invoice #2 - previous finance charges = $1.64,
         * new finance charges of $3.29 ((75 - 30) * 365.25 * 0.08 * 500 - 1.64)
         * Invoice #4 - previous finance charges = $7.46,
         * new finance charges of $7.47 ((90 - 30) * 365.25 * 0.08 * 1135.78 - 7.46)
         */
        financeCharge = financeCharges.get(invoice2);
        assertNotNull("invoice #2 [" + invoice2.getInvoiceId() + "] basic interest calculations", financeCharge);
        assertEquals("invoice #2 [" + invoice2.getInvoiceId() + "] basic interest calculations", new BigDecimal("3.29"), financeCharge.get("interestAmount"));
        financeCharge = financeCharges.get(invoice4);
        assertNotNull("invoice #4 [" + invoice4.getInvoiceId() + "] basic interest calculations", financeCharge);
        assertEquals("invoice #4 [" + invoice4.getInvoiceId() + "] basic interest calculations", new BigDecimal("7.47"), financeCharge.get("interestAmount"));

        /*
         * Verify that writing off a finance charge has the right
         * effect on both accounting and on interest calculations
         * Get INTRSTINC_RECEIVABLE and interest invoice writeoff
         * account balances for Company and the Accounts balance
         * for DemoPrivilegedCust. Write off the finance charge
         * (invoice) created for DemoPrivilegedCust in step (9)
         */
        now = UtilDateTime.nowTimestamp();

        balance1_demoprivilegedcust = AccountsHelper.getBalanceForCustomerPartyId("DemoPrivilegedCust", organizationPartyId, "ACTUAL", now, delegator);

        initialBalances = financialAsserts.getFinancialBalances(now);

        fa.updateInvoiceStatus(invoiceId4Interest, "INVOICE_WRITEOFF");

        /*
         * Verify that both INTRSTINC_RECEIVABLE and interest invoice writeoff account
         * balances have increased by $7.46
         */
        now = UtilDateTime.nowTimestamp();

        finalBalances = financialAsserts.getFinancialBalances(now);

        // figure out what the interest invoice writeoff account is using the domain
        DomainsDirectory dd = new DomainsLoader(new Infrastructure(dispatcher), new User(demofinadmin)).loadDomainsDirectory();
        LedgerRepositoryInterface ledgerRepository = dd.getLedgerDomain().getLedgerRepository();
        InvoiceAdjustmentGlAccount writeoffGlAcct = ledgerRepository.getInvoiceAdjustmentGlAccount(organizationPartyId, "INTEREST_INVOICE", "WRITEOFF");
        assertNotNull(writeoffGlAcct);

        // interest income receivable is a debit account, so an increase is positive;
        // interest income is a credit account, so an increase is negative
        expectedBalanceChanges = UtilMisc.toMap("INTRSTINC_RECEIVABLE", "-7.46");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(writeoffGlAcct.getGlAccountId(), "7.46");
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);

        /*
         * Run AccountsHelper.calculateFinanceCharges 30 days further
         * in the future (asOfDateTime parameter)
         */
        invoice2 = repository.getInvoiceById(invoiceId2);
        invoice4 = repository.getInvoiceById(invoiceId4);
        financeCharges = AccountsHelper.calculateFinanceCharges(delegator, organizationPartyId, null, null, new BigDecimal("8.0"),
                UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, timeZone, locale), 30, timeZone, locale);

        /*
         * Verify that the results are:
         * Invoice #2 - previous finance charges = $1.64,
         * new finance charges of $3.29 ((75 - 30) * 365.25 * 0.08 * 500 - 1.64)
         * Invoice #4 - previous finance charges = $0.00,
         * new finance charges of $14.93 ((90 - 30) * 365.25 * 0.08 * 1135.78)
         */
        financeCharge = financeCharges.get(invoice2);
        assertNotNull("invoice #2 [" + invoice2.getInvoiceId() + "] basic interest calculations", financeCharge);
        assertEquals("invoice #2 [" + invoice2.getInvoiceId() + "] basic interest calculations", new BigDecimal("3.29"), financeCharge.get("interestAmount"));
        financeCharge = financeCharges.get(invoice4);
        assertNotNull("invoice #4 [" + invoice4.getInvoiceId() + "] basic interest calculations", financeCharge);
        assertEquals("invoice #4 [" + invoice4.getInvoiceId() + "] basic interest calculations", new BigDecimal("14.93"), financeCharge.get("interestAmount"));

        /*
         * Verify that payments would serve to reduce invoice finance charges
         * Create customer payment for $250 from DemoPrivilegedCust to Company
         * and apply it to invoice #4.  Set the payment to RECEIVED.
         * Run AccountsHelper.calculateFinanceCharges 30 days further in the
         * future (asOfDateTime parameter)
         */
        financialAsserts.createPaymentAndApplication(new BigDecimal("250.0"), "DemoPrivilegedCust", organizationPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD", null, invoiceId4, "PMNT_RECEIVED");

        invoice2 = repository.getInvoiceById(invoiceId2);
        invoice4 = repository.getInvoiceById(invoiceId4);
        financeCharges = AccountsHelper.calculateFinanceCharges(delegator, organizationPartyId, null, null, new BigDecimal("8.0"),
                UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, timeZone, locale), 30, timeZone, locale);

        /*
         * Verify that the results are:
         * Invoice #2 - previous finance charges = $1.64,
         * new finance charges of $3.29 ((75 - 30) * 365.25 * 0.08 * 500 - 1.64)
         * Invoice #4 - previous finance charges = $0,
         * new finance charges of $11.64 ((90 - 30) * 365.25 * 0.08 * 885.78)
         */
        financeCharge = financeCharges.get(invoice2);
        assertNotNull("invoice #2 [" + invoice2.getInvoiceId() + "] basic interest calculations", financeCharge);
        assertEquals("invoice #2 [" + invoice2.getInvoiceId() + "] basic interest calculations", new BigDecimal("3.29"), financeCharge.get("interestAmount"));
        financeCharge = financeCharges.get(invoice4);
        assertNotNull("invoice #4 [" + invoice4.getInvoiceId() + "] basic interest calculations", financeCharge);
        assertEquals("invoice #4 [" + invoice2.getInvoiceId() + "] basic interest calculations", new BigDecimal("11.64"), financeCharge.get("interestAmount"));
    }

    /**
     * Void invoice test:
     * 1.  Create a sales invoice to DemoCustCompany with 2 invoice items: (a) 2.0 x "Finished good" type for $50 and (b) 1.0 "Shipping and handling" for $9.95
     * 2.  Get the AccountsHelper customer receivable balance of DemoCustCompany
     * 3.  Get the initial financial balances
     * 4.  Set invoice to READY
     * 5.  Verify that the AccountsHelper customer balance of DemoCustCompany has increased by 109.95
     * 6.  Verify that the balance of gl accounts 120000 increased by 109.95, 400000 increased by 100, 408000 increased by 9.95
     * 7.  Verify AccountsHelper.getUnpaidInvoicesForCustomer returns this invoice of list of unpaid invoices
     * 8.  Set invoice to VOID using opentaps.VoidInvoice service
     * 9.  Verify that AccountsHelper customer balance of DemoCustCompany is back to value in #2
     * 10.  Verify that balance of gl accounts 120000, 400000, 408000 are backed to values of #3
     * 11.  Verify that AccountsHelper.getUnpaidInvoicesForCustomer no longer returns this invoice in list of unpaid invoices
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testVoidInvoice() throws GeneralException {

        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        String companyPartyId = createPartyFromTemplate("DemoCustCompany", "Account for testVoidInvoice");

        /*
         * 1.  Create a sales invoice to the Party with 2
         * invoice items: (a) 2.0 x "Finished good" type for $50
         * and (b) 1.0 "Shipping and handling" for $9.95
         */
        String invoiceId = fa.createInvoice(companyPartyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("2.0"), new BigDecimal("50.0"));
        fa.createInvoiceItem(invoiceId, "ITM_SHIPPING_CHARGES", new BigDecimal("1.0"), new BigDecimal("9.95"));

        /*
         * 2.  Get the AccountsHelper customer receivable
         * balance of the Party
         */
        Timestamp now = UtilDateTime.nowTimestamp();
        BigDecimal balance1 = AccountsHelper.getBalanceForCustomerPartyId(companyPartyId, organizationPartyId, "ACTUAL", now, delegator);

        /*
         * 3.  Get the initial financial balances
         */
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Map initialBalances = financialAsserts.getFinancialBalances(now);

        /*
         * 4.  Set invoice to READY
         */
        fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        /*
         * 5.  Verify that the AccountsHelper customer balance of
         * the Party has increased by 109.95
         */
        now = UtilDateTime.nowTimestamp();
        BigDecimal balance2 = AccountsHelper.getBalanceForCustomerPartyId(companyPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + companyPartyId + " has not increased by 109.95", balance1, balance2.subtract(new BigDecimal("109.95")));

        /*
         * 6.  Verify that the balance of gl accounts 120000 = ACCOUNTS_RECEIVABLE
         * increased by 109.95, 400000 = SALES_ACCOUNT increased by 100,
         * 408000 = ITM_SHIPPING_CHARGES increased by 9.95
         */
        GenericValue invoiceItemTypeForShippingCharges = delegator.findByPrimaryKey("InvoiceItemType", UtilMisc.toMap("invoiceItemTypeId", "ITM_SHIPPING_CHARGES"));
        String itemShippingChargesGlAccountId = invoiceItemTypeForShippingCharges.getString("defaultGlAccountId");
        Map halfBalances = financialAsserts.getFinancialBalances(now);
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "109.95", "SALES_ACCOUNT", "100");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(itemShippingChargesGlAccountId, "9.95");
        assertMapDifferenceCorrect(initialBalances, halfBalances, accountMap);

        /*
         * 7.  Verify AccountsHelper.getUnpaidInvoicesForCustomer returns this
         * invoice of list of unpaid invoices
         */
        Map invoices = AccountsHelper.getUnpaidInvoicesForCustomers(organizationPartyId, UtilMisc.toList(new Integer(0)), now, delegator, timeZone, locale);
        assertNotNull("Unpaid Invoices For Customers not found.", invoices);
        List invoicesList = (List) invoices.get(0);
        assertNotNull("Unpaid Invoices For Customers not found.", invoicesList);
        Iterator invoicesIterator = invoicesList.iterator();
        boolean isPresent = false;
        while (invoicesIterator.hasNext()) {
            Invoice invoice = (Invoice) invoicesIterator.next();
            if (invoiceId.equals(invoice.getString("invoiceId"))) {
                isPresent = true;
                break;
            }
        }
        assertTrue("Invoice [" + invoiceId + "] is not in the list of unpaid invoiced and shouldn't", isPresent);

        /*
         * 8.  Set invoice to VOID using opentaps.VoidInvoice service
         */
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", demofinadmin);
        input.put("invoiceId", invoiceId);
        runAndAssertServiceSuccess("opentaps.voidInvoice", input);

        /*
         * 9.  Verify that AccountsHelper customer balance of
         * DemoCustCompany is back to value in #2
         */
        now = UtilDateTime.nowTimestamp();
        BigDecimal balance3 = AccountsHelper.getBalanceForCustomerPartyId(companyPartyId, organizationPartyId, "ACTUAL", now, delegator);
        assertEquals("Balance for " + companyPartyId + " is not back to original value", balance1, balance3);

        /*
         * 10.  Verify that balance of gl accounts 120000, 400000, 408000
         * are backed to values of #3
         */
        Map finalBalances = financialAsserts.getFinancialBalances(now);
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "0", "SALES_ACCOUNT", "0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(itemShippingChargesGlAccountId, "0");
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);

        /*
         * 11.  Verify that AccountsHelper.getUnpaidInvoicesForCustomer no
         * longer returns this invoice in list of unpaid invoices
         */
        now = UtilDateTime.nowTimestamp();
        invoices = AccountsHelper.getUnpaidInvoicesForCustomers(organizationPartyId, UtilMisc.toList(new Integer(0)), now, delegator, timeZone, locale);
        assertNotNull("Unpaid Invoices For Customers not found.", invoices);
        invoicesList = (List) invoices.get(0);
        assertNotNull("Unpaid Invoices For Customers not found.", invoicesList);
        invoicesIterator = invoicesList.iterator();
        isPresent = false;
        while (invoicesIterator.hasNext()) {
            Invoice invoice = (Invoice) invoicesIterator.next();
            if (invoiceId.equals(invoice.getString("invoiceId"))) {
                isPresent = true;
                break;
            }
        }
        assertFalse("Invoice [" + invoiceId + "] is in the list of unpaid invoiced and shouldn't", isPresent);

    }

    /**
     * TestBasicInvoiceAdjustment
     * 1.  Create purchase invoice for $10
     * 2.  Set invoice to READY
     * 3.  Get financial balances and the balances
     * 4.  Create payment of type COMPANY_CHECK for $8 and apply payment to the invoice
     * 5.  Set payment to SENT
     * 6.  Verify that the outstanding amount of the invoice is $2 and the status of the invoice is still READY
     * 7.  Use createInvoiceAdjustment to create an adjustment of type EARLY_PAY_DISCT for -$2 for the invoice and call postAdjustmentToInvoice
     * 8.  Verify that the outstanding amount of the invoice is $0 and the status of the invoice is PAID
     * 8.  Find InvoiceAdjustmentGlAccount for PURCHASE_INVOICE and EARLY_PAY_DISCT and get the glAccountId
     * 9.  Get the financial balances
     * 10.  Verify the following changes in financial balances: ACCOUNTS_PAYABLE +10, BANK_STLMNT_ACCOUNT -8, InvoiceAdjustmentGlAccount.glAccountId -2
     * 11. Verify that the vendor balance has decreased by $10 for the supplier since (4)
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testBasicInvoiceAdjustment() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        DomainsDirectory dd = new DomainsLoader(new Infrastructure(dispatcher), new User(demofinadmin)).loadDomainsDirectory();
        String supplierPartyId = createPartyFromTemplate("DemoSupplier", "Supplier for testBasicInvoiceAdjustment");

        // create the purchase invoice
        String invoiceId = fa.createInvoice(supplierPartyId, "PURCHASE_INVOICE");
        fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("10.0"));
        fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");
        InvoiceRepositoryInterface repository = dd.getBillingDomain().getInvoiceRepository();

        Map initialBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal vendorBalance1 = AccountsHelper.getBalanceForVendorPartyId(supplierPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        fa.createPaymentAndApplication(new BigDecimal("8.0"), organizationPartyId, supplierPartyId, "VENDOR_PAYMENT", "COMPANY_CHECK", "COCHECKING", invoiceId, "PMNT_SENT");
        Invoice invoice = repository.getInvoiceById(invoiceId);
        assertEquals("Invoice [" + invoice.getInvoiceId() + "] is still ready", invoice.getStatusId(), "INVOICE_READY");
        assertEquals("Invoice [" + invoice.getInvoiceId() + "] outstanding amt is $2", invoice.getOpenAmount(), new BigDecimal("2.0"));

        // post an adjustment of -$2 of type EARLY_PAY_DISCT to the invoice
        Map results = runAndAssertServiceSuccess("createInvoiceAdjustment", UtilMisc.toMap("userLogin", demofinadmin, "invoiceId", invoiceId, "invoiceAdjustmentTypeId", "EARLY_PAY_DISCT", "adjustmentAmount", new BigDecimal("-2.0")));
        String invoiceAdjustmentId = (String) results.get("invoiceAdjustmentId");
        assertNotNull(invoiceAdjustmentId);

        invoice = repository.getInvoiceById(invoiceId);
        assertEquals("Invoice [" + invoice.getInvoiceId() + "] outstanding amt is $0", BigDecimal.ZERO, invoice.getOpenAmount());
        assertEquals("Invoice [" + invoice.getInvoiceId() + "] is paid", "INVOICE_PAID", invoice.getStatusId());

        GenericValue mapping = delegator.findByPrimaryKey("InvoiceAdjustmentGlAccount", UtilMisc.toMap("invoiceTypeId", "PURCHASE_INVOICE", "invoiceAdjustmentTypeId", "EARLY_PAY_DISCT", "organizationPartyId", organizationPartyId));
        assertNotNull(mapping);
        String glAccountId = mapping.getString("glAccountId");
        assertNotNull(glAccountId);

        // ensure the balances are correct
        Map finalBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "10.0", "BANK_STLMNT_ACCOUNT", "-8");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(glAccountId, "-2");
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);

        BigDecimal vendorBalance2 = AccountsHelper.getBalanceForVendorPartyId(supplierPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Vendor balance has decreased by $10", vendorBalance2.subtract(vendorBalance1), new BigDecimal("-10"));
    }

    /**
     * TestInvoiceAdjustmentWithPayment
     * 1.  Create sales invoice for $10
     * 2.  Set invoice to READY
     * 3.  Get financial balances and customer balances
     * 4.  Create payment of type COMPANY_CHECK for $6 and apply payment to the invoice
     * 5.  Set payment to RECEIVED
     * 6.  Use createInvoiceAdjustment to create an adjustment of type CASH_DISCOUNT for -$2 for the invoice and call postAdjustmentToInvoice
     * 7.  Create payment of type CASH for $2 and apply payment to the invoice
     * 8.  Verify that the outstanding amount of the invoice is $0 and the status of the invoice is PAID
     * 9.  Find InvoiceAdjustmentGlAccount for SALES_INVOICE and CASH_DISCOUNT and get the glAccountId
     * 10.  Get the financial balances and customer balances
     * 11.  Verify the following changes in financial balances: ACCOUNTS_RECEIVABLE -10, UNDEPOSITED_RECEIPT +8, InvoiceAdjustmentGlAccount.glAccountId -2
     * 12. Verify that the customer balance has decreased by $10 for the customer since (4)
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInvoiceAdjustmentWithPayment() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String customerPartyId = createPartyFromTemplate("DemoCustomer", "Customer for testInvoiceAdjustmentWithPayment");
        String invoiceId = fa.createInvoice(customerPartyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("10.0"));
        fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");
        InvoiceRepository repository = new InvoiceRepository(delegator);

        Map initialBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal custBalance1 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        // company check payment from customer of $6
        fa.createPaymentAndApplication(new BigDecimal("6.0"), customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "COMPANY_CHECK", null, invoiceId, "PMNT_RECEIVED");
        Invoice invoice = repository.getInvoiceById(invoiceId);
        assertEquals("Invoice [" + invoice.getInvoiceId() + "] is still ready", invoice.getStatusId(), "INVOICE_READY");
        assertEquals("Invoice [" + invoice.getInvoiceId() + "] outstanding amt is $4", invoice.getOpenAmount(), new BigDecimal("4.0"));

        // post an adjustment of -$2 of type CASH_DISCOUNT to the invoice
        Map results = runAndAssertServiceSuccess("createInvoiceAdjustment", UtilMisc.toMap("userLogin", demofinadmin, "invoiceId", invoiceId, "invoiceAdjustmentTypeId", "CASH_DISCOUNT", "adjustmentAmount", new BigDecimal("-2.0")));
        String invoiceAdjustmentId = (String) results.get("invoiceAdjustmentId");
        assertNotNull(invoiceAdjustmentId);

        // another customer payment for the remaining $2, which should pay off the invoice
        fa.createPaymentAndApplication(new BigDecimal("2.0"), customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "CASH", null, invoiceId, "PMNT_RECEIVED");
        invoice = repository.getInvoiceById(invoiceId);
        assertEquals("Invoice [" + invoice.getInvoiceId() + "] is paid", "INVOICE_PAID", invoice.getStatusId());
        assertEquals("Invoice [" + invoice.getInvoiceId() + "] outstanding amt is $0", BigDecimal.ZERO, invoice.getOpenAmount());

        // get the cash discount gl account for sales invoices
        GenericValue mapping = delegator.findByPrimaryKey("InvoiceAdjustmentGlAccount", UtilMisc.toMap("invoiceTypeId", "SALES_INVOICE", "invoiceAdjustmentTypeId", "CASH_DISCOUNT", "organizationPartyId", organizationPartyId));
        assertNotNull(mapping);
        String glAccountId = mapping.getString("glAccountId");
        assertNotNull(glAccountId);

        // ensure the balances are correct
        Map finalBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "-10.0", "UNDEPOSITED_RECEIPTS", "+8");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(glAccountId, "+2");
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);

        BigDecimal custBalance2 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer balance has decreased by $10", custBalance2.subtract(custBalance1), new BigDecimal("-10"));
    }

    /**
     *This test checks that payment applications to an adjusted invoice is based on the invoice total including the adjusted amount
     * @throws Exception 
     */
    public void testPaymentApplicationToAdjustedInvoice() throws Exception {
        // create a customer party
        String customerPartyId =
            createPartyFromTemplate("DemoCustomer", "Customer for testPaymentApplicationToAdjustedInvoice");

        // create sales invoice to the customer for $10
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String invoiceId = fa.createInvoice(customerPartyId, InvoiceTypeConstants.SALES_INVOICE);
        fa.createInvoiceItem(invoiceId, InvoiceItemTypeConstants.INV_FPROD_ITEM, "WG-1111", new BigDecimal("1.0"), new BigDecimal("10.0"));

        //  set the invoice to READY
        fa.updateInvoiceStatus(invoiceId, StatusItemConstants.InvoiceStatus.INVOICE_READY);

        // create an adjustment of CASH_DISCOUNT of -2 for the invoice
        Map<String, Object> results =
            runAndAssertServiceSuccess("createInvoiceAdjustment",
                    UtilMisc.toMap(
                            "userLogin", demofinadmin,
                            "invoiceId", invoiceId,
                            "invoiceAdjustmentTypeId", InvoiceAdjustmentTypeConstants.CASH_DISCOUNT,
                            "adjustmentAmount", new BigDecimal("-2.0")
                    )
            );
        String invoiceAdjustmentId = (String) results.get("invoiceAdjustmentId");
        assertNotNull(invoiceAdjustmentId);

        InvoiceRepositoryInterface repository = getInvoiceRepository(demofinadmin);
        Invoice invoice = repository.getInvoiceById(invoiceId);

        // verify that the invoice adjusted total amount is $8
        assertEquals(String.format("Invoice total amount for [%1$s] is incorrect.", invoiceId), new BigDecimal("8.0"), invoice.getInvoiceAdjustedTotal());

        // verify that the invoice outstanding amount is $8
        assertEquals(String.format("Invoice outstanding(open) amount for [%1$s] is incorrect.", invoiceId), new BigDecimal("8.0"), invoice.getOpenAmount());

        // create a payment (paymentId1) of $4 and apply it to the invoice
        String paymentId1 = 
            fa.createPayment(new BigDecimal("4.0"), customerPartyId, PaymentTypeConstants.Receipt.CUSTOMER_PAYMENT, PaymentMethodTypeConstants.CASH);
        runAndAssertServiceSuccess("createPaymentApplication",
                UtilMisc.<String, Object>toMap(
                        "paymentId", paymentId1,
                        "invoiceId", invoiceId,
                        "amountApplied", new BigDecimal("4.0"),
                        "userLogin", demofinadmin
                )
        );
        fa.updatePaymentStatus(paymentId1, StatusItemConstants.PmntStatus.PMNT_RECEIVED);

        // create a second payment (paymentId2) of $5 and apply $4 to the invoice
        String paymentId2 = 
            fa.createPayment(new BigDecimal("5.0"), customerPartyId, PaymentTypeConstants.Receipt.CUSTOMER_PAYMENT, PaymentMethodTypeConstants.CASH);
        runAndAssertServiceSuccess("createPaymentApplication",
                UtilMisc.<String, Object>toMap(
                        "paymentId", paymentId2,
                        "invoiceId", invoiceId,
                        "amountApplied", new BigDecimal("4.0"),
                        "userLogin", demofinadmin
                )
        );
        fa.updatePaymentStatus(paymentId2, StatusItemConstants.PmntStatus.PMNT_RECEIVED);

        invoice = repository.getInvoiceById(invoiceId);

        // verify that the invoice adjusted total is still $8 by the outstanding amount is $0
        assertEquals(String.format("Invoice total amount for [%1$s] is incorrect.", invoiceId), new BigDecimal("8.0"), invoice.getInvoiceAdjustedTotal());
        assertEquals(String.format("Invoice outstanding(open) amount for [%1$s] is incorrect.", invoiceId), BigDecimal.ZERO, invoice.getOpenAmount());

        // verify the status of the invoice is PAID
        assertEquals("Invoice status should be PAID in this point.", StatusItemConstants.InvoiceStatus.INVOICE_PAID, invoice.getStatusId());
    }

    /**
     * TestAdjustmentToCancelledInvoice: this test should verify that adjustments to cancelled invoices are not posted to the ledger
     * 1.  Create sales invoice for $10 (do not set to ready)
     * 2.  Create adjustment of EARLY_PAY_DISCOUNT for $2 on invoice from (1)
     * 3.  Get financials balances
     * 4.  Get accounts receivable balances for customer of invoice from (1)
     * 5.  Set status of invoice from (1) to INVOICE_CANCELLED
     * 6.  Get financial balances
     * 7.  Get accounts receivable balances for customer of invoice from (1)
     * 8.  Verify that the changes in ACCOUNTS_RECEIVABLE and InvoiceAdjustmentGlAccount.glAccountId for Company, EARLY_PAY_DISCOUNT are both 0
     * 9.  Verify that the change in customer balance for customer is 0
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testAdjustmentToCancelledInvoice() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String customerPartyId = createPartyFromTemplate("DemoCustCompany", "Customer for testAdjustmentToCancelledInvoice");
        String invoiceId = fa.createInvoice(customerPartyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("10.0"));

        // post an adjustment of $2 of type EARLY_PAY_DISCT to the invoice
        Map results = runAndAssertServiceSuccess("createInvoiceAdjustment", UtilMisc.toMap("userLogin", demofinadmin, "invoiceId", invoiceId, "invoiceAdjustmentTypeId", "EARLY_PAY_DISCT", "adjustmentAmount", new BigDecimal("2.0")));
        String invoiceAdjustmentId = (String) results.get("invoiceAdjustmentId");
        assertNotNull(invoiceAdjustmentId);

        // get balances, cancel
        Map initialBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal custBalances1 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        fa.updateInvoiceStatus(invoiceId, "INVOICE_CANCELLED");

        // get the early pay discount gl account for sales invoices
        GenericValue mapping = delegator.findByPrimaryKey("InvoiceAdjustmentGlAccount", UtilMisc.toMap("invoiceTypeId", "SALES_INVOICE", "invoiceAdjustmentTypeId", "EARLY_PAY_DISCT", "organizationPartyId", organizationPartyId));
        assertNotNull(mapping);
        String glAccountId = mapping.getString("glAccountId");
        assertNotNull(glAccountId);

        // the pause above should be sufficient to get the final balances
        Map finalBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal custBalances2 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        // check balances are 0.0 throughout
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "0");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(glAccountId, "0.0");
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);
        assertEquals("Customer balance has not changed", custBalances2.subtract(custBalances1), BigDecimal.ZERO);
    }

    /**
     * TestAdjustPotVoidForInvoice: test alternate workflow: adjust invoice, post invoice, and void the invoice
     * 1.  Create sales invoice for $10
     * 2.  Create adjustment of CASH_DISCOUNT for -$2 on invoice
     * 3.  Create adjustment of EARLY_PAY_DISCOUNT of -$0.50 on invoice
     * 4.  Get financials balances
     * 5.  Get accounts receivables balance for customer of invoice from (1)
     * 6.  Get InvoiceAdjustmentGlAccount for Company, EARLY_PAY_DISCOUNT and Company, CASH_DISCOUNT
     * 7.  Set invoice to INVOICE_READY
     * 8.  Verify ACCOUNTS_RECEIVABLES +7.50, InvoiceAdjustmentGlAccount(Company, CASH_DISCOUNT) +2.0, InvoiceAdjustmentGlAccount(Company, EARLY_PAY_DISCOUNT) +0.50 since (4)
     * 9.  Verify that accounts receivable balance of customer has increased by 7.50
     * 10.  Set Invoice to INVOICE_VOID
     * 11.  Verify ACCOUNTS_RECEIVABLES +0, InvoiceAdjustmentGlAccount(Company, EARLY_PAY_DISCOUNT) +0, InvoiceAdjustmentGlAccount(Company, CASH_DISCOUNT) 0 since (4)
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testAdjustPostVoidForInvoice() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String customerPartyId = createPartyFromTemplate("DemoCustCompany", "Customer for testAdjustPostVoidForInvoice");
        String invoiceId = fa.createInvoice(customerPartyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("10.0"));

        // post an adjustment of $2 of type CASH_DISCOUNT to the invoice
        Map results = runAndAssertServiceSuccess("createInvoiceAdjustment", UtilMisc.toMap("userLogin", demofinadmin, "invoiceId", invoiceId, "invoiceAdjustmentTypeId", "CASH_DISCOUNT", "adjustmentAmount", new BigDecimal("-2.0")));
        String invoiceAdjustmentId1 = (String) results.get("invoiceAdjustmentId");
        assertNotNull(invoiceAdjustmentId1);

        // post an adjustment of $0.50 of type EARLY_PAY_DISCT to the invoice
        results = runAndAssertServiceSuccess("createInvoiceAdjustment", UtilMisc.toMap("userLogin", demofinadmin, "invoiceId", invoiceId, "invoiceAdjustmentTypeId", "EARLY_PAY_DISCT", "adjustmentAmount", new BigDecimal("-0.50")));
        String invoiceAdjustmentId2 = (String) results.get("invoiceAdjustmentId");
        assertNotNull(invoiceAdjustmentId2);

        // get balances, set to ready
        Map initialBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal custBalances1 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        // get the early pay discount and cash gl accounts for sales invoices (note: they might be the same account)
        GenericValue mapping = delegator.findByPrimaryKey("InvoiceAdjustmentGlAccount", UtilMisc.toMap("invoiceTypeId", "SALES_INVOICE", "invoiceAdjustmentTypeId", "EARLY_PAY_DISCT", "organizationPartyId", organizationPartyId));
        assertNotNull(mapping);
        String earlyPayGlAccountId = mapping.getString("glAccountId");
        assertNotNull(earlyPayGlAccountId);

        mapping = delegator.findByPrimaryKey("InvoiceAdjustmentGlAccount", UtilMisc.toMap("invoiceTypeId", "SALES_INVOICE", "invoiceAdjustmentTypeId", "CASH_DISCOUNT", "organizationPartyId", organizationPartyId));
        assertNotNull(mapping);
        String cashGlAccountId = mapping.getString("glAccountId");
        assertNotNull(cashGlAccountId);

        // the pause above should be sufficient to get the final balances
        Map middleBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal custBalances2 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        // check balances are +7.50 to AR, +2 to early pay, -0.5 to cash and that the AR balance of customer is 7.50
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "7.50");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        if (cashGlAccountId.equals(earlyPayGlAccountId)) {
            accountMap.put(cashGlAccountId, "2.50");
        } else {
            accountMap.put(cashGlAccountId, "2.0");
            accountMap.put(earlyPayGlAccountId, "0.50");
        }
        assertMapDifferenceCorrect(initialBalances, middleBalances, accountMap);
        assertEquals("Customer balance as expected", custBalances2.subtract(custBalances1), new BigDecimal("7.50"));

        // void invoices and get balances again
        fa.updateInvoiceStatus(invoiceId, "INVOICE_VOIDED");
        Map finalBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal custBalances3 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        // check balances are all 0 compared to initial
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(earlyPayGlAccountId, "0.0");
        accountMap.put(cashGlAccountId, "0");
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);
        assertEquals("Customer balance net 0", custBalances3.subtract(custBalances1), BigDecimal.ZERO);
    }

    /**
     * Test for accounts receivable invoice aging and customer balances
     *
     * 1.  Create parties Customer A, Customer B, Customer C
     * 2.  Create invoices:
     *     (a) Create sales invoice #1 from Company to Customer A for $100, invoice date 91 days before current date, due date 30 days after invoice date
     *     (b) Create sales invoice #2 from Company to Customer A for $50, invoice date 25 days before current date, due date 30 days after invoice date
     *     (c) Create sales invoice #3 from Company to Customer B for $150, invoice date 55 days before current date, due date 60 days after invoice date
     *     (d) Create sales invoice #4 from Company to Customer C for $170, invoice date 120 days before current date, due date 30 days after invoice date
     *     (e) Create sales invoice #5 from Company to Customer B for $210, invoice date 15 days before current date, due date 7 days after invoice date
     *     (f) Create sales invoice #6 from Company to Customer A for $500, invoice date 36 days before current date, due date 30 days after invoice date
     *     (g) Create sales invoice #7 from Company to Customer C for $228, invoice date 42 days before current date, due date 45 days after invoice date
     *     (h) Create sales invoice #8 from Company to Customer B for $65, invoice date 15 days before current date, due date 30 days after invoice date
     *     (i) Create sales invoice #9 from Company to Customer A for $156, invoice date 6 days before current date, due date 15 days after invoice date
     *     (j) Create sales invoice #10 from Company to Customer C for $550, invoice date 33 days before current date, due date 15 days after invoice date
     *     (k) Create sales invoice #11 from Company to Customer B for $90, invoice date 62 days before current date, due date 90 days after invoice date
     * 3.  Cancel invoice #2
     * 4.  Set all other invoices to READY
     * 5.  Set invoice #6 to VOID
     * 6.  Set invoice #10 to WRITEOFF
     * 7.  Receive a payment of $65 for invoice #8
     * 8.  Receive a payment of $65 for invoice #11
     * 9.  Create sales invoice #12 for Company to Customer A for $1000, invoice date now and due 30 days after invoicing, but do not set this invoice to READY
     * 10. Run AccountsHelper.getBalancesForAllCustomers and verify the following:
     *      (a) balance of Customer A is $256
     *      (b) balance of Customer B is $385
     *      (c) balance of Customer C is $398
     * 11.  Run AccountsHelper.getUnpaidInvoicesForCustomers and verify:
     *      (a)  0-30 day bucket has invoices #5 and #9
     *      (b) 30-60 day bucket has invoices #3 and #7
     *      (c) 60-90 day bucket has invoice #11
     *      (d) 90+ day bucket has invoices #1 and #4
     * 12.  Create parties Customer D and Customer E
     * 13.  Create more sales invoices:
     *      (a) Invoice #13 from Company to Customer D for $200, invoice date today, due in 30 days after invoice date
     *      (b) Invoice #14 from Company to Customer D for $300, invoice date today, due in 60 days after invoice date
     *      (c) Invoice #15 from Company to Customer E for $155, invoice date 58 days before today, due in 50 days after invoice date
     *      (d) Invoice #16 from Company to Customer E for $266, invoice date 72 days before today, due in 30 days after invoice date
     *      (e) Invoice #17 from Company to Customer E for $377, invoice date 115 days before today, due in 30 days after invoice date
     *      (f) Invoice #18 from Company to Customer E for $488, invoice date 135 days before today, due in 30 days after invoice date
     *      (g) Invoice #19 from Company to Customer E for $599, invoice date 160 days before today, due in 30 days after invoice date
     *      (h) Invoice #20 from Company to Customer E for $44, invoice date 20 days before today, no due date (null)
     * 14.  Set all invoices from (13) to ready.
     * 15.  Get customer statement (AccountsHelper.customerStatement) with useAgingDate=true and verify
     *      (a) Customer A: isPastDue = true, current = $156, 30 - 60 days =  0, 60 - 90 days = $100, 90 - 120 days = 0, over 120 days = 0, total open amount = $256
     *      (b) Customer B: isPastDue = false, current = $385, 30 - 60 days =  0, 60 - 90 days = 0, 90 - 120 days = 0, over 120 days = 0, total open amount = $385
     *      (c) Customer C: isPastDue = true, current = $228, 30 - 60 days =  0, 60 - 90 days = 0,  90 - 120 days = $170, over 120 days = 0, total open amount = $398
     *      (d) Customer D: isPastDue = false, current = $500, 30 - 60 days =  0, 60 - 90 days = 0,  90 - 120 days = 0, over 120 days = 0, total open amount = $500
     *      (e) Customer E: isPastDue = true, current = $199, 30 - 60 days = 266, 60 - 90 days = 377, 90 - 120 days = 488, over 120 days = 599, total open amount = $1929
     * 16.  Get customer statement (AccountsHelper.customerStatement) with useAgingDate=false and verify
     *      (a) Customer A: isPastDue = true, current = $156, 30 - 60 days =  0, 60 - 90 days = 0, 90 - 120 days = $100, over 120 days = 0, total open amount = $256
     *      (b) Customer B: isPastDue = true, current = $210, 30 - 60 days =  150, 60 - 90 days = 25, 90 - 120 days = 0, over 120 days = 0, total open amount = $385
     *      (c) Customer C: isPastDue = true, current = 0, 30 - 60 days =  228, 60 - 90 days = 0,  90 - 120 days = 0, over 120 days = $170, total open amount = $398
     *      (d) Customer D: isPastDue = false, current = $500, 30 - 60 days =  0, 60 - 90 days = 0,  90 - 120 days = 0, over 120 days = 0, total open amount = $500
     *      (e) Customer E: isPastDue = true, current = $44, 30 - 60 days = 155, 60 - 90 days = 266, 90 - 120 days = 377, over 120 days = 1087, total open amount = $1929
     * @throws GeneralException if an error occurs
     */
    public void testAccountsReceivableInvoiceAgingAndCustomerBalances() throws GeneralException {

        /*
         * 1.  Create parties Customer A, Customer B, Customer C
         */
        TestObjectGenerator generator = new TestObjectGenerator(delegator, dispatcher);
        List<String> customerId = generator.getContacts(3);
        assertNotNull("Three customers should be generated", customerId);

        /*
         * 2. Create invoices
         */
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        /*
         * (a) Create sales invoice #1 from Company to Customer A for $100, invoice date 91 days before current date, due date 30 days after invoice date
         */
        String invoiceId1 = fa.createInvoice(customerId.get(0), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -91, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -91 + 30, timeZone, locale), null, null, "invoiceId1");
        fa.createInvoiceItem(invoiceId1, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("100.0"));

        /*
         * (b) Create sales invoice #2 from Company to Customer A for $50, invoice date 25 days before current date, due date 30 days after invoice date
         */
        String invoiceId2 = fa.createInvoice(customerId.get(0), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -25, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -25 + 30, timeZone, locale), null, null, "invoiceId2");
        fa.createInvoiceItem(invoiceId2, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("50.0"));

        /*
         * (c) Create sales invoice #3 from Company to Customer B for $150, invoice date 55 days before current date, due date 60 days after invoice date
         */
        String invoiceId3 = fa.createInvoice(customerId.get(1), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -55, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -55 + 60, timeZone, locale), null, null, "invoiceId3");
        fa.createInvoiceItem(invoiceId3, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("150.0"));

        /*
         * (d) Create sales invoice #4 from Company to Customer C for $170, invoice date 120 days before current date, due date 30 days after invoice date
         */
        String invoiceId4 = fa.createInvoice(customerId.get(2), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -120, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -120 + 30, timeZone, locale), null, null, "invoiceId4");
        fa.createInvoiceItem(invoiceId4, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("170.0"));

        /*
         * (e) Create sales invoice #5 from Company to Customer B for $210, invoice date 15 days before current date, due date 7 days after invoice date
         */
        String invoiceId5 = fa.createInvoice(customerId.get(1), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -15, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -15 + 7, timeZone, locale), null, null, "invoiceId5");
        fa.createInvoiceItem(invoiceId5, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("210.0"));

        /*
         * (f) Create sales invoice #6 from Company to Customer A for $500, invoice date 36 days before current date, due date 30 days after invoice date
         */
        String invoiceId6 = fa.createInvoice(customerId.get(0), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -36, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -36 + 30, timeZone, locale), null, null, "invoiceId6");
        fa.createInvoiceItem(invoiceId6, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("500.0"));

        /*
         * (g) Create sales invoice #7 from Company to Customer C for $228, invoice date 42 days before current date, due date 45 days after invoice date
         */
        String invoiceId7 = fa.createInvoice(customerId.get(2), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -42, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -42 + 45, timeZone, locale), null, null, "invoiceId7");
        fa.createInvoiceItem(invoiceId7, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("228.0"));

        /*
         * (h) Create sales invoice #8 from Company to Customer B for $65, invoice date 15 days before current date, due date 30 days after invoice date
         */
        String invoiceId8 = fa.createInvoice(customerId.get(1), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -15, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -15 + 30, timeZone, locale), null, null, "invoiceId8");
        fa.createInvoiceItem(invoiceId8, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("65.0"));

        /*
         * (i) Create sales invoice #9 from Company to Customer A for $156, invoice date 6 days before current date, due date 15 days after invoice date
         */
        String invoiceId9 = fa.createInvoice(customerId.get(0), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -6, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -6 + 15, timeZone, locale), null, null, "invoiceId9");
        fa.createInvoiceItem(invoiceId9, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("156.0"));

        /*
         * (j) Create sales invoice #10 from Company to Customer C for $550, invoice date 33 days before current date, due date 15 days after invoice date
         */
        String invoiceId10 = fa.createInvoice(customerId.get(2), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -33, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -33 + 15, timeZone, locale), null, null, "invoiceId10");
        fa.createInvoiceItem(invoiceId10, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("550.0"));

        /*
         * (k) Create sales invoice #11 from Company to Customer B for $90, invoice date 62 days before current date, due date 90 days after invoice date
         */
        String invoiceId11 = fa.createInvoice(customerId.get(1), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -62, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -62 + 90, timeZone, locale), null, null, "invoiceId11");
        fa.createInvoiceItem(invoiceId11, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("90.0"));

        /*
         * 3.  Cancel invoice #2
         */
        fa.updateInvoiceStatus(invoiceId2, "INVOICE_CANCELLED");

        /*
         * 4.  Set all other invoices to READY
         */
        fa.updateInvoiceStatus(invoiceId1, "INVOICE_READY");
        fa.updateInvoiceStatus(invoiceId3, "INVOICE_READY");
        fa.updateInvoiceStatus(invoiceId4, "INVOICE_READY");
        fa.updateInvoiceStatus(invoiceId5, "INVOICE_READY");
        fa.updateInvoiceStatus(invoiceId6, "INVOICE_READY");
        fa.updateInvoiceStatus(invoiceId7, "INVOICE_READY");
        fa.updateInvoiceStatus(invoiceId8, "INVOICE_READY");
        fa.updateInvoiceStatus(invoiceId9, "INVOICE_READY");
        fa.updateInvoiceStatus(invoiceId10, "INVOICE_READY");
        fa.updateInvoiceStatus(invoiceId11, "INVOICE_READY");

        /*
         * 5.  Set invoice #6 to VOID
         */
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", demofinadmin);
        input.put("invoiceId", invoiceId6);
        runAndAssertServiceSuccess("opentaps.voidInvoice", input);

        /*
         * 6.  Set invoice #10 to WRITEOFF
         */
        fa.updateInvoiceStatus(invoiceId10, "INVOICE_WRITEOFF");

        /*
         * 7.  Receive a payment of $65 for invoice #8
         */
        fa.createPaymentAndApplication(new BigDecimal("65.0"), customerId.get(1), organizationPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD", null, invoiceId8, "PMNT_RECEIVED");

        /*
         * 8.  Receive a payment of $65 for invoice #11
         */
        fa.createPaymentAndApplication(new BigDecimal("65.0"), customerId.get(1), organizationPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD", null, invoiceId11, "PMNT_RECEIVED");

        /*
         * 9.  Create sales invoice #12 for Company to Customer A for $1000, invoice date now and due 30 days after invoicing, but do not set this invoice to READY
         */
        String invoiceId12 = fa.createInvoice(customerId.get(0), "SALES_INVOICE", UtilDateTime.nowTimestamp(), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, timeZone, locale), null, null, "invoiceId12");
        fa.createInvoiceItem(invoiceId12, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("1000.0"));

        /*
         * 10. Run AccountsHelper.getBalancesForAllCustomers and verify the following:
         */
        Map<String, BigDecimal> balances = AccountsHelper.getBalancesForAllCustomers(organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        /*
         * (a) balance of Customer A is $256
         */
        assertEquals("balance of Customer " + customerId.get(0) + " is " + balances.get(customerId.get(0)) + " and not $256", balances.get(customerId.get(0)), new BigDecimal("256.00"));

        /*
         * (b) balance of Customer B is $385
         */
        assertEquals("balance of Customer " + customerId.get(1) + " is " + balances.get(customerId.get(1)) + " and not $385", balances.get(customerId.get(1)), new BigDecimal("385.00"));

        /*
         * (c) balance of Customer C is $398
         */
        assertEquals("balance of Customer " + customerId.get(2) + " is " + balances.get(customerId.get(0)) + " and not $398", balances.get(customerId.get(2)), new BigDecimal("398.00"));

        /*
         * 11.  Run AccountsHelper.getUnpaidInvoicesForCustomers and verify:
         */
        Map<Integer, List<Invoice>> invoices = AccountsHelper.getUnpaidInvoicesForCustomers(organizationPartyId, UtilMisc.toList(new Integer(30), new Integer(60), new Integer(90), new Integer(9999)), UtilDateTime.nowTimestamp(), delegator, timeZone, locale);
        assertNotNull("Unpaid Invoices For Customers not found.", invoices);

        /*
         * (b) 0-30 day bucket has invoices #5 and #9
         */
        List<Invoice> invoicesList = invoices.get(30);
        assertNotNull("Unpaid Invoices For Customers not found.", invoicesList);
        boolean isPresent5 = false;
        boolean isPresent9 = false;
        for (Invoice invoice : invoicesList) {
            if (invoiceId5.equals(invoice.getString("invoiceId"))) {
                isPresent5 = true;
            }
            if (invoiceId9.equals(invoice.getString("invoiceId"))) {
                isPresent9 = true;
            }
        }
        assertTrue("Invoice " + invoiceId5 + " is not present in 0-30 day bucket", isPresent5);
        assertTrue("Invoice " + invoiceId9 + " is not present in 0-30 day bucket", isPresent9);

        /*
         * (c) 30-60 day bucket has invoice #3 and #7
         */
        invoicesList = invoices.get(60);
        assertNotNull("Unpaid Invoices For Customers not found.", invoicesList);
        boolean isPresent3 = false;
        boolean isPresent7 = false;
        for (Invoice invoice : invoicesList) {
            if (invoiceId3.equals(invoice.getString("invoiceId"))) {
                isPresent3 = true;
            }
            if (invoiceId7.equals(invoice.getString("invoiceId"))) {
                isPresent7 = true;
            }
        }
        assertTrue("Invoice " + invoiceId3 + " is not present in 30-60 day bucket", isPresent3);
        assertTrue("Invoice " + invoiceId7 + " is not present in 30-60 day bucket", isPresent7);

        /*
         * (d) 60-90 day bucket has invoices #11
         */
        invoicesList = invoices.get(90);
        assertNotNull("Unpaid Invoices For Customers not found.", invoicesList);
        boolean isPresent11 = false;
        for  (Invoice invoice : invoicesList) {
            if (invoiceId11.equals(invoice.getString("invoiceId"))) {
                isPresent11 = true;
            }
        }
        assertTrue("Invoice " + invoiceId11 + " is not present in 60-90 day bucket", isPresent11);

        /*
         * (d) 90+ day bucket has invoices #1 and #4
         */
        invoicesList = invoices.get(9999);
        assertNotNull("Unpaid Invoices For Customers not found.", invoicesList);
        boolean isPresent1 = false;
        boolean isPresent4 = false;
        for  (Invoice invoice : invoicesList) {
            if (invoiceId1.equals(invoice.getString("invoiceId"))) {
                isPresent1 = true;
            }
            if (invoiceId4.equals(invoice.getString("invoiceId"))) {
                isPresent4 = true;
            }
        }
        assertTrue("Invoice " + invoiceId1 + " is not present in 90+ day bucket", isPresent1);
        assertTrue("Invoice " + invoiceId4 + " is not present in 90+ day bucket", isPresent4);

        /*
         * 12.  Create parties Customer D and Customer E
         */
        List<String> extraCustomerIds = generator.getContacts(2);
        assertNotNull("Two customers should be generated", extraCustomerIds);
        assertEquals(2, extraCustomerIds.size());

        customerId.addAll(extraCustomerIds);

        /*
         * 13.  Create more sales invoices
         *
         * (a) Invoice #13 from Company to Customer D for $200, invoice date today, due in 30 days after invoice date
         */
        String invoice13 = fa.createInvoice(customerId.get(3), "SALES_INVOICE", UtilDateTime.nowTimestamp(), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, timeZone, locale), null, null, "invoiceId13");
        fa.createInvoiceItem(invoice13, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("2.0"), new BigDecimal("100.0"));

        /*
         * (b) Invoice #14 from Company to Customer D for $300, invoice date today, due in 60 days after invoice date
         */
        String invoice14 = fa.createInvoice(customerId.get(3), "SALES_INVOICE", UtilDateTime.nowTimestamp(), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 60, timeZone, locale), null, null, "invoice14");
        fa.createInvoiceItem(invoice14, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("3.0"), new BigDecimal("100.0"));

        /*
         * (c) Invoice #15 from Company to Customer E for $155, invoice date 58 days before today, due in 50 days after invoice date
         */
        String invoice15 = fa.createInvoice(customerId.get(4), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -58, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -58 + 50, timeZone, locale), null, null, "invoice15");
        fa.createInvoiceItem(invoice15, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("155.0"));

        /*
         * (d) Invoice #16 from Company to Customer E for $266, invoice date 72 days before today, due in 30 days after invoice date
         */
        String invoice16 = fa.createInvoice(customerId.get(4), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -72, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -72 + 30, timeZone, locale), null, null, "invoice16");
        fa.createInvoiceItem(invoice16, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("266.0"));

        /*
         * (e) Invoice #17 from Company to Customer E for $377, invoice date 115 days before today, due in 30 days after invoice date
         */
        String invoice17 = fa.createInvoice(customerId.get(4), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -115, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -115 + 30, timeZone, locale), null, null, "invoice17");
        fa.createInvoiceItem(invoice17, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("377.0"));

        /*
         * (f) Invoice #18 from Company to Customer E for $488, invoice date 135 days before today, due in 30 days after invoice date
         */
        String invoice18 = fa.createInvoice(customerId.get(4), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -135, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -135 + 30, timeZone, locale), null, null, "invoice18");
        fa.createInvoiceItem(invoice18, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("488.0"));

        /*
         * (g) Invoice #19 from Company to Customer E for $599, invoice date 160 days before today, due in 30 days after invoice date
         */
        String invoice19 = fa.createInvoice(customerId.get(4), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -160, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -160 + 30, timeZone, locale), null, null, "invoice19");
        fa.createInvoiceItem(invoice19, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("599.0"));

        /*
         * (h) Invoice #20 from Company to Customer E for $44, invoice date 20 days before today, no due date (null)
         */
        String invoice20 = fa.createInvoice(customerId.get(4), "SALES_INVOICE", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -20, timeZone, locale), null, null, null, "invoice20");
        fa.createInvoiceItem(invoice20, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("44.0"));


        /*
         * 14. Set all invoices from (13) to ready.
         */
        fa.updateInvoiceStatus(invoice13, "INVOICE_READY");
        fa.updateInvoiceStatus(invoice14, "INVOICE_READY");
        fa.updateInvoiceStatus(invoice15, "INVOICE_READY");
        fa.updateInvoiceStatus(invoice16, "INVOICE_READY");
        fa.updateInvoiceStatus(invoice17, "INVOICE_READY");
        fa.updateInvoiceStatus(invoice18, "INVOICE_READY");
        fa.updateInvoiceStatus(invoice19, "INVOICE_READY");
        fa.updateInvoiceStatus(invoice20, "INVOICE_READY");

        /*
         * 15.  Get customer statement (AccountsHelper.customerStatement) with useAgingDate=true and verify
         */
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(demofinadmin));
        Map<String, Object> partyData = FastMap.newInstance();
        AccountsHelper.customerStatement(dl, organizationPartyId, UtilMisc.toSet(customerId), UtilDateTime.nowTimestamp(), 30, true, partyData, locale, timeZone);
        assertNotNull("Failed to create customer statement.", partyData);

        /*
         * (a) Customer A: isPastDue = true, current = $156, 30 - 60 days =  0, 60 - 90 days = $100, 90 - 120 days = 0, over 120 days = 0, total open amount = $256
         */
        verifyCustomerStatement(customerId.get(0), partyData, true, new BigDecimal("156.0"), new BigDecimal("0.0"), new BigDecimal("100.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("256.0"));

        /*
         * (b) Customer B: isPastDue = false, current = $385, 30 - 60 days =  0, 60 - 90 days = 0, 90 - 120 days = 0, over 120 days = 0, total open amount = $385
         */
        verifyCustomerStatement(customerId.get(1), partyData, false, new BigDecimal("385.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("385.0"));

        /*
         * (c) Customer C: isPastDue = true, current = $228, 30 - 60 days =  0, 60 - 90 days = 0,  90 - 120 days = $170, over 120 days = 0, total open amount = $398
         */
        verifyCustomerStatement(customerId.get(2), partyData, true, new BigDecimal("228.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("170.0"), new BigDecimal("0.0"), new BigDecimal("398.0"));

        /*
         * (d) Customer D: isPastDue = false, current = $500, 30 - 60 days =  0, 60 - 90 days = 0,  90 - 120 days = 0, over 120 days = 0, total open amount = $500
         */
        verifyCustomerStatement(customerId.get(3), partyData, false, new BigDecimal("500.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("500.0"));

        /*
         * (e) Customer E: isPastDue = true, current = $199, 30 - 60 days = 266, 60 - 90 days = 377, 90 - 120 days = 488, over 120 days = 599, total open amount = $1929
         */
        verifyCustomerStatement(customerId.get(4), partyData, true, new BigDecimal("199.0"), new BigDecimal("266.0"), new BigDecimal("377.0"), new BigDecimal("488.0"), new BigDecimal("599.0"), new BigDecimal("1929.0"));

        /*
         * 16.  Get customer statement (AccountsHelper.customerStatement) with useAgingDate=false and verify
         */
        partyData = FastMap.newInstance();
        AccountsHelper.customerStatement(dl, organizationPartyId, UtilMisc.toSet(customerId), UtilDateTime.nowTimestamp(), 30, false, partyData, locale, timeZone);
        assertNotNull("Failed to create customer statement.", partyData);

        /*
         *       (a) Customer A: isPastDue = true, current = $156, 30 - 60 days =  0, 60 - 90 days = 0, 90 - 120 days = $100, over 120 days = 0, total open amount = $256
         */
        verifyCustomerStatement(customerId.get(0), partyData, true, new BigDecimal("156.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("100.0"), new BigDecimal("0.0"), new BigDecimal("256.0"));

        /*
         *       (b) Customer B: isPastDue = true, current = $210, 30 - 60 days =  150, 60 - 90 days = 25, 90 - 120 days = 0, over 120 days = 0, total open amount = $385
         */
        verifyCustomerStatement(customerId.get(1), partyData, true, new BigDecimal("210.0"), new BigDecimal("150.0"), new BigDecimal("25.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("385.0"));

        /*
         *       (c) Customer C: isPastDue = true, current = 0, 30 - 60 days =  228, 60 - 90 days = 0,  90 - 120 days = 0, over 120 days = $170, total open amount = $398
         *       Note that invoice #4 from above is in the over 120 day bucket because it is dated 120 days before current timestamp, but aging calculation start at beginning of today
         */
        verifyCustomerStatement(customerId.get(2), partyData, true, new BigDecimal("0.0"), new BigDecimal("228.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("170.0"), new BigDecimal("398.0"));

        /*
         *       (d) Customer D: isPastDue = false, current = $500, 30 - 60 days =  0, 60 - 90 days = 0,  90 - 120 days = 0, over 120 days = 0, total open amount = $500
         */
        verifyCustomerStatement(customerId.get(3), partyData, false, new BigDecimal("500.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("500.0"));

        /*
         *       (e) Customer E: isPastDue = true, current = $44, 30 - 60 days = 155, 60 - 90 days = 266, 90 - 120 days = 377, over 120 days = 1087, total open amount = $1929
         */
        verifyCustomerStatement(customerId.get(4), partyData, true, new BigDecimal("44.0"), new BigDecimal("155.0"), new BigDecimal("266.0"), new BigDecimal("377.0"), new BigDecimal("1087.0"), new BigDecimal("1929.0"));

    }

    private void verifyCustomerStatement(String partyId, Map<String, Object> partyData, boolean isPastDue, BigDecimal current, BigDecimal over30, BigDecimal over60, BigDecimal over90, BigDecimal over120, BigDecimal totalOpen) {
        assertEquals("Customer " + partyId + " should have past due invoices but isPastDue flag incorrect.", Boolean.valueOf(isPastDue), partyData.get(partyId + "is_past_due"));
        assertEquals("Current value for customer " + partyId + " is incorrect.", current, (BigDecimal) partyData.get(String.format("%1$scurrent", partyId)));
        assertEquals("Over 30 days past due amount is wrong.", over30, (BigDecimal) partyData.get(String.format("%1$sover_1N", partyId)));
        assertEquals("Over 60 days past due amount is wrong.", over60, (BigDecimal) partyData.get(String.format("%1$sover_2N", partyId)));
        assertEquals("Over 90 days past due amount is wrong.", over90, (BigDecimal) partyData.get(String.format("%1$sover_3N", partyId)));
        assertEquals("Over 120 days past due amount is wrong.", over120, (BigDecimal) partyData.get(String.format("%1$sover_4N", partyId)));
        assertEquals("Total open amount is wrong.", totalOpen, (BigDecimal) partyData.get(String.format("%1$stotal_open", partyId)));
    }

    /**
     * Tests setInvoiceReadyAndCheckIfPaid service on an in process invoice, verifying that it sets it to INVOICE_READY,
     * and the GL and customer outstanding balance are increased.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testSetInvoiceReadyAndCheckIfPaidToInvoiceReady() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String customerPartyId = createPartyFromTemplate("DemoCustCompany", "Customer for testSetInvoiceReadyAndCheckIfPaidToInvoiceReady");
        BigDecimal invoiceAmount = new BigDecimal("10.0");

        // create the invoice
        String invoiceId = fa.createInvoice(customerPartyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), invoiceAmount);

        // get initial balances
        Map initialBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal initialCustomerBalance = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        // set to ready and check if  paid
        runAndAssertServiceSuccess("setInvoiceReadyAndCheckIfPaid", UtilMisc.toMap("invoiceId", invoiceId, "userLogin", demofinadmin));

        // get final balances
        Map finalBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal finalCustomerBalance = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        // check customer outstanding balance is right
        assertEquals("Customer balance has increased by the right amount", (finalCustomerBalance.subtract(initialCustomerBalance)).setScale(DECIMALS, ROUNDING), invoiceAmount);

        // check AR has increased by $10
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "10.0");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);

        // check invoice is PAID
        Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertTrue("Invoice [" + invoiceId + "] is ready", invoice.isReady());
    }

    /**
     * Tests setInvoiceReadyAndCheckIfPaid service on a cancelled invoice, verifying that it returns an error,
     * and the GL and customer outstanding balance are unchanged.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testSetInvoiceReadyAndCheckIfPaidForCancelledInvoice() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String customerPartyId = createPartyFromTemplate("DemoCustCompany", "Customer for testSetInvoiceReadyAndCheckIfPaidForCancelledInvoice");
        BigDecimal invoiceAmount = new BigDecimal("10.0");

        // create the invoice
        String invoiceId = fa.createInvoice(customerPartyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), invoiceAmount);

        // get initial balances
        Map initialBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal initialCustomerBalance = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        // now cancel the invoice
        fa.updateInvoiceStatus(invoiceId, "INVOICE_CANCELLED");

        // this should fail
        runAndAssertServiceError("setInvoiceReadyAndCheckIfPaid", UtilMisc.toMap("invoiceId", invoiceId, "userLogin", demofinadmin));

        // get the final balances
        Map finalBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal finalCustomerBalance = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        // but the customer balance and accounts receivable totals should not have changed
        assertEquals("Customer balance is unchanged", (finalCustomerBalance.subtract(initialCustomerBalance)).setScale(DECIMALS, ROUNDING), BigDecimal.ZERO);

        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "0.0");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);

        // and the invoice should stay cancelled
        Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertTrue("Invoice [" + invoiceId + "] is cancelled", invoice.isCancelled());
    }

    /**
     * Tests setInvoiceReadyAndCheckIfPaid service on an invoice with payments already applied, verifying that it causes invoice to be PAID
     * and the GL and customer outstanding balance are unchanged, but there is now money received (in undeposited receipts).
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testSetInvoiceReadyAndCheckIfPaidForInvoiceWithPayments() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String customerPartyId = createPartyFromTemplate("DemoCustCompany", "Customer for testSetInvoiceReadyAndCheckIfPaidForInvoiceWithPayments");
        BigDecimal invoiceAmount = new BigDecimal("10.0");

        // create the invoice
        String invoiceId = fa.createInvoice(customerPartyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), invoiceAmount);

        // get initial balances
        Map initialBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal initialCustomerBalance = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        // now create a payment and apply it to the invoice
        fa.createPaymentAndApplication(invoiceAmount, customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "PERSONAL_CHECK", null, invoiceId, "PMNT_RECEIVED");

        // now set the invoice ready
        runAndAssertServiceSuccess("setInvoiceReadyAndCheckIfPaid", UtilMisc.toMap("invoiceId", invoiceId, "userLogin", demofinadmin));

        // get final balances
        Map finalBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal finalCustomerBalance = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        // customer's balance should be unchanged
        assertEquals("Customer balance is unchanged", (finalCustomerBalance.subtract(initialCustomerBalance)).setScale(DECIMALS, ROUNDING), BigDecimal.ZERO);

        // AR should be unchanged (invoice is already paid), but we should have $10 in undeposited receipts
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_RECEIVABLE", "0.0", "UNDEPOSITED_RECEIPTS", "+10");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);

        // and invoice should be paid
        Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertTrue("Invoice [" + invoiceId + "] is paid", invoice.isPaid());
    }

    /**
     * This test verifies that when the organization uses standard costing, the difference between the standard cost and
     * purchase invoice price for the item is charged off as a purchase price variance.   There should be no change to
     * uninvoiced shipment receipt.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPurchasingVarianceWithStandardCost() throws GeneralException {
        // Set the organization costing method to standard costing
        setStandardCostingMethod("STANDARD_COSTING");

        // Get initial Financial balances
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Map initialBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // Create a new product
        GenericValue testProduct = createTestProduct("Test Purchasing Variance With Standard Cost Product", demowarehouse1);
        String productId = testProduct.getString("productId");

        // set its supplier product record for DemoSupplier to $10
        createMainSupplierForProduct(productId, "DemoSupplier", new BigDecimal("10.0"), "USD", new BigDecimal("1.0"), admin);

        // set its standard cost (EST_STD_MAT_COST) to $35
        runAndAssertServiceSuccess("createCostComponent", UtilMisc.<String, Object>toMap("userLogin", admin, "productId", productId, "costComponentTypeId", "EST_STD_MAT_COST", "cost", new BigDecimal("35.0"), "costUomId", "USD"));

        // Create a purchase order for 75 of this product from DemoSupplier
        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(testProduct, new BigDecimal("75.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderSpec, delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoSupplier")), facilityContactMechId);
        String orderId = pof.getOrderId();
        GenericValue pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));

        // approve the purchase order
        pof.approveOrder();

        // receive all 75 units of the product from the purchase order
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(pOrder, demowarehouse1));

        // Find the invoice
        OrderRepositoryInterface orderRepository = orderDomain.getOrderRepository();
        Order order = orderRepository.getOrderById(orderId);
        List<Invoice> invoices = order.getInvoices();
        assertEquals("Should only have one invoice.", invoices.size(), 1);
        Invoice invoice = invoices.get(0);

        // set the amount of the product on the invoice to $11.25
        GenericValue invoiceItem = EntityUtil.getOnly(delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoice.getInvoiceId(), "productId", productId, "invoiceItemTypeId", "PINV_FPROD_ITEM")));
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", admin, "invoiceId", invoice.getInvoiceId(), "productId", productId, "invoiceItemTypeId", "PINV_FPROD_ITEM", "amount", new BigDecimal("11.25"));
        input.put("invoiceItemSeqId", invoiceItem.get("invoiceItemSeqId"));
        input.put("quantity", invoiceItem.get("quantity"));
        runAndAssertServiceSuccess("updateInvoiceItem", input);

        // add a second invoice item of "PITM_SHIP_CHARGES" for $58.39
        input = UtilMisc.<String, Object>toMap("userLogin", admin, "invoiceId", invoice.getInvoiceId(), "invoiceItemTypeId", "PITM_SHIP_CHARGES", "amount", new BigDecimal("58.39"));
        runAndAssertServiceSuccess("createInvoiceItem", input);

        // set the invoice as ready
        financialAsserts.updateInvoiceStatus(invoice.getInvoiceId(), "INVOICE_READY");

        // Get the final financial balances
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // verify the following changes:
        //   ACCOUNTS_PAYABLE       -902.14
        //   glAccountId=510000       58.39
        //   INVENTORY_ACCOUNT      2625.00
        //   PURCHASE_PRICE_VAR    -1781.25
        //   UNINVOICED_SHIP_RCPT      0.00
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE",    "-902.14",
                                                    "INVENTORY_ACCOUNT",   "2625.00",
                                                    "PURCHASE_PRICE_VAR", "-1781.25",
                                                    "UNINVOICED_SHIP_RCPT",    "0.0");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        expectedBalanceChanges.put("510000", "58.39");
        printMapDifferences(initialBalances, finalBalances);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);
    }


    /**
     * This test verifies that when creating an invoice item, if the productId is not null, then If invoiceItemTypeId is null, then use the ProductInvoiceItemType to fill in the invoiceItemTypeId (see below for this entity)
     * If description is null, then use Product productName to fill in the description
     * If amount is null, then call calculateProductPrice and fill in then amount.
     * when updating an invoice item, if the new productId is different than the old productId
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCreateAndUpdateInvoiceItem() throws GeneralException {
        //create a SALES_INVOICE from Company to another party
        InvoiceRepositoryInterface repository = billingDomain.getInvoiceRepository();
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Account for testCreateAndUpdateInvoiceItem");
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String productId1 = "GZ-1000";
        String productId2 = "GZ-1001";

        // 1 creating invoice item with productId but without invoiceItemTypeId, description, price gets the right values
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        financialAsserts.createInvoiceItem(invoiceId, null, productId1, new BigDecimal("1.0"), null);

        Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
        GenericValue product1 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId1));
        String invoiceItemTypeId1 = repository.getInvoiceItemTypeIdForProduct(invoice, domainsDirectory.getProductDomain().getProductRepository().getProductById(productId1));
        String description1 = product1.getString("productName");
        String uomId = invoice.getCurrencyUomId();
        Map results = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product1, "currencyUomId", uomId));
        BigDecimal price1 = (BigDecimal) results.get("price");
        BigDecimal amount1 = price1.setScale(DECIMALS, ROUNDING);

        GenericValue product2 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId2));
        String invoiceItemTypeId2 = repository.getInvoiceItemTypeIdForProduct(invoice, domainsDirectory.getProductDomain().getProductRepository().getProductById("GZ-1001"));
        String description2 = product2.getString("productName");
        results = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product2, "currencyUomId", uomId));
        BigDecimal price2 = (BigDecimal) results.get("price");
        BigDecimal amount2 = price2.setScale(DECIMALS, ROUNDING);

        GenericValue invoiceItem = EntityUtil.getOnly(delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoice.getInvoiceId(), "productId", productId1)));
        assertEquals("invoiceItemTypeId is wrong.", invoiceItemTypeId1, invoiceItem.getString("invoiceItemTypeId"));
        assertEquals("description is wrong.", description1, invoiceItem.getString("description"));
        assertEquals("amount is wrong.", amount1, invoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING));


        // 2 you can also create invoice item with override invoiceItemTypeId, description, price
        financialAsserts.createInvoiceItem(invoiceId, "ITM_SHIPPING_CHARGES", null, new BigDecimal("1.0"), new BigDecimal("45.0"), "testUpdateInvoiceItem description");
        invoiceItem = EntityUtil.getOnly(delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoice.getInvoiceId(), "invoiceItemTypeId", "ITM_SHIPPING_CHARGES")));
        assertEquals("invoiceItemTypeId is wrong.", "ITM_SHIPPING_CHARGES", invoiceItem.getString("invoiceItemTypeId"));
        assertEquals("description is wrong.", "testUpdateInvoiceItem description", invoiceItem.getString("description"));
        assertEquals("amount is wrong.", new BigDecimal("45.0"), invoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING));

        // 3 updating invoice item causes the right values to be filled in
        financialAsserts.updateInvoiceItem(invoiceId, invoiceItem.getString("invoiceItemSeqId"), null, productId2, new BigDecimal("2.0"), null, null, null);
        invoiceItem = EntityUtil.getOnly(delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoice.getInvoiceId(), "productId", productId2)));
        assertEquals("invoiceItemTypeId is wrong.", invoiceItemTypeId2, invoiceItem.getString("invoiceItemTypeId"));
        assertEquals("description is wrong.", description2, invoiceItem.getString("description"));
        assertEquals("amount is wrong.", amount2, invoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING));

        // 4 you can update invoice item afterwards with override values
        financialAsserts.updateInvoiceItem(invoiceId, invoiceItem.getString("invoiceItemSeqId"), "INV_FPROD_ITEM", productId2, new BigDecimal("2.0"), new BigDecimal("51.99"), "testUpdateInvoiceItem description", null);
        invoiceItem = EntityUtil.getOnly(delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoice.getInvoiceId(), "productId", productId2)));
        assertEquals("invoiceItemTypeId is wrong.", "INV_FPROD_ITEM", invoiceItem.getString("invoiceItemTypeId"));
        assertEquals("description is wrong.", "testUpdateInvoiceItem description", invoiceItem.getString("description"));
        assertEquals("amount is wrong.", new BigDecimal("51.99"), invoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING));

        // verify for PURCHASE_INVOICE
        String supplierProductId1 = "SUPPLY-001";
        String supplierProductId2 = "ASSET-001";
        product1 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", supplierProductId1));
        product2 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", supplierProductId2));
        PurchasingRepositoryInterface purchasingRepository = purchasingDomain.getPurchasingRepository();
        SupplierProduct demoSupplierProduct1 = purchasingRepository.getSupplierProduct("DemoSupplier", supplierProductId1, new BigDecimal("500.0"), "USD");
        SupplierProduct demoSupplierProduct2 = purchasingRepository.getSupplierProduct("DemoSupplier", supplierProductId2, new BigDecimal("500.0"), "USD");

        // 5 create a PURCHASE_INVOICE from Company to supplierParty
        String purchaseInvoiceId = financialAsserts.createInvoice("DemoSupplier", "PURCHASE_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        financialAsserts.createInvoiceItem(purchaseInvoiceId, null, supplierProductId1, new BigDecimal("500.0"), null);

        Invoice purchaseInvoice = repository.getInvoiceById(purchaseInvoiceId);
        String purchaseInvoiceItemTypeId1 = repository.getInvoiceItemTypeIdForProduct(purchaseInvoice, domainsDirectory.getProductDomain().getProductRepository().getProductById(supplierProductId1));
        String purchaseDescription1 = demoSupplierProduct1.getSupplierProductName()==null ? product1.getString("productName") : demoSupplierProduct1.getSupplierProductId() + " " + demoSupplierProduct1.getSupplierProductName();
        BigDecimal purchaseAmount1 = demoSupplierProduct1.getLastPrice();

        String purchaseInvoiceItemTypeId2 = repository.getInvoiceItemTypeIdForProduct(purchaseInvoice, domainsDirectory.getProductDomain().getProductRepository().getProductById(supplierProductId2));
        String purchaseDescription2 = demoSupplierProduct2.getSupplierProductName()==null ? product2.getString("productName") : demoSupplierProduct2.getSupplierProductId() + " " + demoSupplierProduct2.getSupplierProductName();
        BigDecimal purchaseAmount2 = demoSupplierProduct2.getLastPrice();

        invoiceItem = EntityUtil.getOnly(delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", purchaseInvoice.getInvoiceId(), "productId", supplierProductId1)));
        assertEquals("invoiceItemTypeId is wrong.", purchaseInvoiceItemTypeId1, invoiceItem.getString("invoiceItemTypeId"));
        assertEquals("description is wrong.", purchaseDescription1, invoiceItem.getString("description"));
        assertEquals("amount is wrong.", purchaseAmount1, invoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING));


        // 6 you can also create invoice item with override invoiceItemTypeId, description, price
        financialAsserts.createInvoiceItem(purchaseInvoiceId, "ITM_SHIPPING_CHARGES", null, new BigDecimal("1.0"), new BigDecimal("45.0"), "testUpdateInvoiceItem description");
        invoiceItem = EntityUtil.getOnly(delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", purchaseInvoice.getInvoiceId(), "invoiceItemTypeId", "ITM_SHIPPING_CHARGES")));
        assertEquals("invoiceItemTypeId is wrong.", "ITM_SHIPPING_CHARGES", invoiceItem.getString("invoiceItemTypeId"));
        assertEquals("description is wrong.", "testUpdateInvoiceItem description", invoiceItem.getString("description"));
        assertEquals("amount is wrong.", new BigDecimal("45.0"), invoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING));

        // 7 updating invoice item causes the right values to be filled in
        financialAsserts.updateInvoiceItem(purchaseInvoiceId, invoiceItem.getString("invoiceItemSeqId"), null, supplierProductId2, new BigDecimal("500.0"), null, null, null);
        invoiceItem = EntityUtil.getOnly(delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", purchaseInvoice.getInvoiceId(), "productId", supplierProductId2)));
        assertEquals("invoiceItemTypeId is wrong.", purchaseInvoiceItemTypeId2, invoiceItem.getString("invoiceItemTypeId"));
        assertEquals("description is wrong.", purchaseDescription2, invoiceItem.getString("description"));
        assertEquals("amount is wrong.", purchaseAmount2, invoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING));

        // 8 you can update invoice item afterwards with override values
        financialAsserts.updateInvoiceItem(purchaseInvoiceId, invoiceItem.getString("invoiceItemSeqId"), "PINV_FPROD_ITEM", supplierProductId2, new BigDecimal("2.0"), new BigDecimal("199.99"), "testUpdateInvoiceItem description", null);
        invoiceItem = EntityUtil.getOnly(delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", purchaseInvoice.getInvoiceId(), "productId", supplierProductId2)));
        assertEquals("invoiceItemTypeId is wrong.", "PINV_FPROD_ITEM", invoiceItem.getString("invoiceItemTypeId"));
        assertEquals("description is wrong.", "testUpdateInvoiceItem description", invoiceItem.getString("description"));
        assertEquals("amount is wrong.", new BigDecimal("199.99"), invoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING));

    }

    /**
     * This test verifies that transaction posted with a glFiscalTypeId not ACTUAL are not changing the GlAccountHistory
     * or the GlAccountOrganization posted amount.
     * @exception Exception if an error occurs
     */
    public void testAccountHistoryUpdates() throws Exception {
        // 1. create AcctgTrans of glFiscalTypeId = BUDGET
        //      with debitCreditFlag = D, amount = 100, glAccountId = 110000
        //      with debitCreditFlag = C, amount = 100, glAccountId = 300000
        String debitAccount  = "110000";
        String creditAccount = "300000";
        CreateQuickAcctgTransService service = new CreateQuickAcctgTransService();
        service.setInUserLogin(admin);
        service.setInAcctgTransTypeId(AcctgTransTypeConstants.BUDGET);
        service.setInGlFiscalTypeId(GlFiscalTypeConstants.BUDGET);
        service.setInAmount(100.0);
        service.setInDebitGlAccountId(debitAccount);
        service.setInCreditGlAccountId(creditAccount);
        service.setInDescription("testAccountHistoryUpdates");
        service.setInTransactionDate(UtilDateTime.nowTimestamp());
        service.setInOrganizationPartyId(testLedgerOrganizationPartyId);
        runAndAssertServiceSuccess(service);
        String transId = service.getOutAcctgTransId();

        // 2. get all GlAccountHistory records
        LedgerRepositoryInterface ledgerRepository = ledgerDomain.getLedgerRepository();
        List<String> orderBy = UtilMisc.toList(GlAccountHistory.Fields.glAccountId.name(),
                                               GlAccountHistory.Fields.customTimePeriodId.name());
        List<GlAccountHistory> creditHists = ledgerRepository.findList(GlAccountHistory.class,
                                                                       ledgerRepository.map(GlAccountHistory.Fields.glAccountId, creditAccount,
                                                                                            GlAccountHistory.Fields.organizationPartyId, testLedgerOrganizationPartyId),
                                                                       orderBy);
        List<GlAccountHistory> debitHists = ledgerRepository.findList(GlAccountHistory.class,
                                                                      ledgerRepository.map(GlAccountHistory.Fields.glAccountId, debitAccount,
                                                                                           GlAccountHistory.Fields.organizationPartyId, testLedgerOrganizationPartyId),
                                                                      orderBy);
        // 3. get the GlAccountOrganization record
        GlAccountOrganization creditAcctOrg = ledgerRepository.findOneNotNull(GlAccountOrganization.class,
                                                                       ledgerRepository.map(GlAccountOrganization.Fields.glAccountId, creditAccount,
                                                                                            GlAccountOrganization.Fields.organizationPartyId, testLedgerOrganizationPartyId));
        GlAccountOrganization debitAcctOrg = ledgerRepository.findOneNotNull(GlAccountOrganization.class,
                                                                       ledgerRepository.map(GlAccountOrganization.Fields.glAccountId, debitAccount,
                                                                                            GlAccountOrganization.Fields.organizationPartyId, testLedgerOrganizationPartyId));

        // 4. postAcctgTrans
        PostAcctgTransService postService = new PostAcctgTransService();
        postService.setInUserLogin(admin);
        postService.setInAcctgTransId(transId);
        runAndAssertServiceSuccess(postService);

        // 5. verify GlAccountHistory for 100000 and 300000 have not changed.
        List<GlAccountHistory> creditHists2 = ledgerRepository.findList(GlAccountHistory.class,
                                                                        ledgerRepository.map(GlAccountHistory.Fields.glAccountId, creditAccount,
                                                                                             GlAccountHistory.Fields.organizationPartyId, testLedgerOrganizationPartyId),
                                                                        orderBy);
        List<GlAccountHistory> debitHists2 = ledgerRepository.findList(GlAccountHistory.class,
                                                                       ledgerRepository.map(GlAccountHistory.Fields.glAccountId, debitAccount,
                                                                                            GlAccountHistory.Fields.organizationPartyId, testLedgerOrganizationPartyId),
                                                                       orderBy);

        assertEquals("The GlAccountHistory for the debit account [" + debitAccount + "] should not have changed.", debitHists, debitHists2);
        assertEquals("The GlAccountHistory for the credit account [" + creditAccount + "] should not have changed.", creditHists, creditHists2);

        // 6. verify GlAccountOrganization for both accounts have not changed
        GlAccountOrganization creditAcctOrg2 = ledgerRepository.findOneNotNull(GlAccountOrganization.class,
                                                                       ledgerRepository.map(GlAccountOrganization.Fields.glAccountId, creditAccount,
                                                                                            GlAccountOrganization.Fields.organizationPartyId, testLedgerOrganizationPartyId));
        GlAccountOrganization debitAcctOrg2 = ledgerRepository.findOneNotNull(GlAccountOrganization.class,
                                                                       ledgerRepository.map(GlAccountOrganization.Fields.glAccountId, debitAccount,
                                                                                            GlAccountOrganization.Fields.organizationPartyId, testLedgerOrganizationPartyId));

        assertEquals("The GlAccountOrganization for the debit account [" + debitAccount + "] should not have changed.", debitAcctOrg, debitAcctOrg2);
        assertEquals("The GlAccountOrganization for the credit account [" + creditAccount + "] should not have changed.", creditAcctOrg, creditAcctOrg2);

    }

    /**
     * This test verifies captureAccountBalancesSnapshot service can take snapshot for account balance.
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testAccountBalancesSnapshot() throws Exception {
        // create a test party from DemoAccount1 template
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Account for testAccountBalancesSnapshot");
        Debug.logInfo("testAccountBalancesSnapshot method create partyId " + customerPartyId, MODULE);
        // run captureAccountBalancesSnapshot
        Map inputParams = UtilMisc.toMap("userLogin", demofinadmin);
        runAndAssertServiceSuccess("captureAccountBalancesSnapshot", inputParams);
        // verify that there is no account balance history record for test party
        Session session = new Infrastructure(dispatcher).getSession();
        String hql = "from AccountBalanceHistory eo where eo.partyId = :partyId order by eo.asOfDatetime desc";
        Query query = session.createQuery(hql);
        query.setString("partyId", customerPartyId);
        List<AccountBalanceHistory> list = query.list();
        assertEmpty("There is no account balance history record for " + customerPartyId, list);
        // create a SALES_INVOICE from Company to test party for $100 and set it to READY
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1000", new BigDecimal("1.0"), new BigDecimal("100.0"));
        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_READY");
        // run captureAccountBalancesSnapshot
        inputParams = UtilMisc.toMap("userLogin", demofinadmin);
        runAndAssertServiceSuccess("captureAccountBalancesSnapshot", inputParams);
        // verify that there is a new account balance history record for test party and Company for 100
        list = query.list();
        assertEquals("There is a new account balance history record for Company and " + customerPartyId, 1, list.size());
        AccountBalanceHistory accountBalanceHistory = list.get(0);
        assertEquals("There is a new account balance history record for Company and " + customerPartyId + " for 100.0", new BigDecimal("100.0"), accountBalanceHistory.getTotalBalance());

        pause("mysql timestamp pause");

        // create a CUSTOMER_PAYMENT from Company to test party for $50 and set it to RECEIVED
        String paymentId = financialAsserts.createPaymentAndApplication(new BigDecimal("50.0"), customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "COMPANY_CHECK", null, invoiceId, "PMNT_NOT_PAID");
        financialAsserts.updatePaymentStatus(paymentId, "PMNT_RECEIVED");
        // run captureAccountBalancesSnapshot
        inputParams = UtilMisc.toMap("userLogin", demofinadmin);
        runAndAssertServiceSuccess("captureAccountBalancesSnapshot", inputParams);
        // verify that there is a new account balance history record for test party and Company for 50
        list = query.list();
        assertEquals("There are two account balance history records for Company and " + customerPartyId, 2, list.size());
        accountBalanceHistory = list.get(0);
        assertEquals("There is a new account balance history record for Company and " + customerPartyId + " for 50.0", new BigDecimal("50.0"), accountBalanceHistory.getTotalBalance());
    }

    /**
     * This test verifies that an unbalanced transaction cannot be posted, this uses the unbalanced test transaction LEDGER-TEST-2.
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCannotPostAcctgTrans() throws Exception {
        LedgerRepositoryInterface ledgerRepository = ledgerDomain.getLedgerRepository();
        AccountingTransaction transaction = ledgerRepository.getAccountingTransaction("LEDGER-TEST-2");
        // check canPost()
        assertFalse("canPost() should not return true for LEDGER-TEST-2.", transaction.canPost());
        // check that the service returns an error
        Map input = UtilMisc.toMap("userLogin", demofinadmin, "acctgTransId", "LEDGER-TEST-2");
        runAndAssertServiceError("postAcctgTrans", input);
    }

    /**
     * This test verifies that we can get correct InvoiceAdjustmentType by InvoiceRepository.getInvoiceAdjustmentTypes method.
     * @exception Exception if an error occurs
     */
    public void testGetInvoiceAdjustmentTypes() throws Exception {
        // create a sales invoice
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "test for InvoiceRepository.getInvoiceAdjustmentTypes");
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Timestamp invoiceDate = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, timeZone, locale);
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", invoiceDate, null, null, null);
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1000", new BigDecimal("2.0"), new BigDecimal("15.0"));

        //verify that the correct list (three rec) of invoice adjustment types are returned for Company
        Organization organizationCompany = organizationRepository.getOrganizationById(organizationPartyId);
        Organization organizationCompanySub2 = organizationRepository.getOrganizationById("CompanySub2");
        InvoiceRepositoryInterface invoiceRepository = billingDomain.getInvoiceRepository();
        Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
        List<InvoiceAdjustmentType> invoiceAdjustmentTypes = invoiceRepository.getInvoiceAdjustmentTypes(organizationCompany, invoice);
        assertEquals("There should have three InvoiceAdjustmentType records for [" + organizationPartyId + "]", 3, invoiceAdjustmentTypes.size());

        //verify none invoice adjustment types are returned for Company
        invoiceAdjustmentTypes = invoiceRepository.getInvoiceAdjustmentTypes(organizationCompanySub2, invoice);
        assertEquals("There should hasn't any InvoiceAdjustmentType record for [CompanySub2]", 0, invoiceAdjustmentTypes.size());

    }

    /**
     * Test the Invoice fields calculation.
     * @exception Exception if an error occurs
     */
    public void testInvoiceFieldsCalculation() throws Exception {
        // create a sales invoice
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "test for InvoiceFieldsCalculation");
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        InvoiceRepositoryInterface invoiceRepository = billingDomain.getInvoiceRepository();

        // create an invoice with one item and total 24
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE");
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1000", new BigDecimal("2.0"), new BigDecimal("12.0"));

        // check the invoice amounts at this point
        Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertEquals("Invoice total incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceTotal(), new BigDecimal("24"));
        assertEquals("Open amount incorrect for invoice [" + invoiceId + "]", invoice.getOpenAmount(), new BigDecimal("24"));
        assertEquals("Pending Open amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingOpenAmount(), new BigDecimal("24"));
        assertEquals("Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getAppliedAmount(), BigDecimal.ZERO);
        assertEquals("Pending Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingAppliedAmount(), BigDecimal.ZERO);
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getAdjustedAmount(), BigDecimal.ZERO);
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceAdjustedTotal(), new BigDecimal("24"));

        // create a payment and application of 5
        String paymentId = financialAsserts.createPaymentAndApplication(new BigDecimal("5"), customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "COMPANY_CHECK", null, invoiceId, "PMNT_NOT_PAID");

        // check the invoice amounts at this point
        // since an application is created but the payment was not received yet, it should only modify the pending amounts
        invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertEquals("Invoice total incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceTotal(), new BigDecimal("24"));
        assertEquals("Open amount incorrect for invoice [" + invoiceId + "]", invoice.getOpenAmount(), new BigDecimal("24"));
        assertEquals("Pending Open amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingOpenAmount(), new BigDecimal("19"));
        assertEquals("Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getAppliedAmount(), BigDecimal.ZERO);
        assertEquals("Pending Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingAppliedAmount(), new BigDecimal("5"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getAdjustedAmount(), BigDecimal.ZERO);
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceAdjustedTotal(), new BigDecimal("24"));

        // create an invoice adjustment as a -2 discount
        runAndAssertServiceSuccess("createInvoiceAdjustment", UtilMisc.toMap("userLogin", demofinadmin, "invoiceId", invoiceId, "invoiceAdjustmentTypeId", "EARLY_PAY_DISCT", "adjustmentAmount", new BigDecimal("-2.0")));

        // check the invoice amounts at this point
        // this modifies the adjusted amount, adjusted total and open amounts
        invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertEquals("Invoice total incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceTotal(), new BigDecimal("24"));
        assertEquals("Open amount incorrect for invoice [" + invoiceId + "]", invoice.getOpenAmount(), new BigDecimal("22"));
        assertEquals("Pending Open amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingOpenAmount(), new BigDecimal("17"));
        assertEquals("Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getAppliedAmount(), BigDecimal.ZERO);
        assertEquals("Pending Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingAppliedAmount(), new BigDecimal("5"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getAdjustedAmount(), new BigDecimal("-2"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceAdjustedTotal(), new BigDecimal("22"));

        // create a second payment and application of 7
        String paymentId2 = financialAsserts.createPaymentAndApplication(new BigDecimal("7"), customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "COMPANY_CHECK", null, invoiceId, "PMNT_NOT_PAID");

        // check the invoice amounts at this point
        invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertEquals("Invoice total incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceTotal(), new BigDecimal("24"));
        assertEquals("Open amount incorrect for invoice [" + invoiceId + "]", invoice.getOpenAmount(), new BigDecimal("22"));
        assertEquals("Pending Open amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingOpenAmount(), new BigDecimal("10"));
        assertEquals("Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getAppliedAmount(), BigDecimal.ZERO);
        assertEquals("Pending Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingAppliedAmount(), new BigDecimal("12"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getAdjustedAmount(), new BigDecimal("-2"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceAdjustedTotal(), new BigDecimal("22"));

        // mark the first payment as received
        financialAsserts.updatePaymentStatus(paymentId, "PMNT_RECEIVED");

        // check the invoice amounts at this point
        invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertEquals("Invoice total incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceTotal(), new BigDecimal("24"));
        assertEquals("Open amount incorrect for invoice [" + invoiceId + "]", invoice.getOpenAmount(), new BigDecimal("17"));
        assertEquals("Pending Open amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingOpenAmount(), new BigDecimal("10"));
        assertEquals("Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getAppliedAmount(), new BigDecimal("5"));
        assertEquals("Pending Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingAppliedAmount(), new BigDecimal("7"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getAdjustedAmount(), new BigDecimal("-2"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceAdjustedTotal(), new BigDecimal("22"));

        // add an invoice item of 3x5 15
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1005", new BigDecimal("3.0"), new BigDecimal("5.0"));

        // check the invoice amounts at this point
        invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertEquals("Invoice total incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceTotal(), new BigDecimal("39"));
        assertEquals("Open amount incorrect for invoice [" + invoiceId + "]", invoice.getOpenAmount(), new BigDecimal("32"));
        assertEquals("Pending Open amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingOpenAmount(), new BigDecimal("25"));
        assertEquals("Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getAppliedAmount(), new BigDecimal("5"));
        assertEquals("Pending Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingAppliedAmount(), new BigDecimal("7"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getAdjustedAmount(), new BigDecimal("-2"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceAdjustedTotal(), new BigDecimal("37"));

        // mark the second payment as received
        financialAsserts.updatePaymentStatus(paymentId2, "PMNT_RECEIVED");
        // set the invoice ready
        runAndAssertServiceSuccess("setInvoiceReadyAndCheckIfPaid", UtilMisc.toMap("invoiceId", invoiceId, "userLogin", demofinadmin));

        // check the invoice amounts at this point
        invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertEquals("Invoice total incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceTotal(), new BigDecimal("39"));
        assertEquals("Open amount incorrect for invoice [" + invoiceId + "]", invoice.getOpenAmount(), new BigDecimal("25"));
        assertEquals("Pending Open amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingOpenAmount(), new BigDecimal("25"));
        assertEquals("Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getAppliedAmount(), new BigDecimal("12"));
        assertEquals("Pending Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingAppliedAmount(), BigDecimal.ZERO);
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getAdjustedAmount(), new BigDecimal("-2"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceAdjustedTotal(), new BigDecimal("37"));

        // create a payment and application of 8
        String paymentId3 = financialAsserts.createPaymentAndApplication(new BigDecimal("8"), customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "COMPANY_CHECK", null, invoiceId, "PMNT_NOT_PAID");

        // check the invoice amounts at this point
        invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertEquals("Invoice total incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceTotal(), new BigDecimal("39"));
        assertEquals("Open amount incorrect for invoice [" + invoiceId + "]", invoice.getOpenAmount(), new BigDecimal("25"));
        assertEquals("Pending Open amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingOpenAmount(), new BigDecimal("17"));
        assertEquals("Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getAppliedAmount(), new BigDecimal("12"));
        assertEquals("Pending Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingAppliedAmount(), new BigDecimal("8"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getAdjustedAmount(), new BigDecimal("-2"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceAdjustedTotal(), new BigDecimal("37"));

        // cancel the payment
        financialAsserts.updatePaymentStatus(paymentId3, "PMNT_CANCELLED");

        // check the invoice amounts at this point
        invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertEquals("Invoice total incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceTotal(), new BigDecimal("39"));
        assertEquals("Open amount incorrect for invoice [" + invoiceId + "]", invoice.getOpenAmount(), new BigDecimal("25"));
        assertEquals("Pending Open amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingOpenAmount(), new BigDecimal("25"));
        assertEquals("Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getAppliedAmount(), new BigDecimal("12"));
        assertEquals("Pending Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingAppliedAmount(), BigDecimal.ZERO);
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getAdjustedAmount(), new BigDecimal("-2"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceAdjustedTotal(), new BigDecimal("37"));

        // create a final payment and application of 25, and receive it
        String paymentId4 = financialAsserts.createPaymentAndApplication(new BigDecimal("25"), customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "COMPANY_CHECK", null, invoiceId, "PMNT_NOT_PAID");
        financialAsserts.updatePaymentStatus(paymentId4, "PMNT_RECEIVED");

        // check the invoice amounts at this point
        invoice = invoiceRepository.getInvoiceById(invoiceId);
        assertEquals("Invoice total incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceTotal(), new BigDecimal("39"));
        assertEquals("Open amount incorrect for invoice [" + invoiceId + "]", invoice.getOpenAmount(), BigDecimal.ZERO);
        assertEquals("Pending Open amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingOpenAmount(), BigDecimal.ZERO);
        assertEquals("Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getAppliedAmount(), new BigDecimal("37"));
        assertEquals("Pending Applied amount incorrect for invoice [" + invoiceId + "]", invoice.getPendingAppliedAmount(), BigDecimal.ZERO);
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getAdjustedAmount(), new BigDecimal("-2"));
        assertEquals("Adjusted amount incorrect for invoice [" + invoiceId + "]", invoice.getInvoiceAdjustedTotal(), new BigDecimal("37"));

        // check that the invoice is marked as PAID
        assertTrue("Invoice [" + invoiceId + "] is paid", invoice.isPaid());
    }
}
