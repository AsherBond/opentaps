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
import java.util.Map;

import javolution.util.FastMap;

import com.opensourcestrategies.financials.util.UtilFinancial;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.organization.Organization;

/**
 * Test for accounting tag related operations.
 */
public class AccountingTagTests extends FinancialsTestCase {

    private static final String STATEMENT_DETAILS = "STATEMENT-DETAILS";

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests the Organization.getAccountingTagTypes method for preloaded test data.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testGetAccountingTags() throws GeneralException {
        Organization statementDetails = organizationRepository.getOrganizationById(STATEMENT_DETAILS);

        Map accountingTags = statementDetails.getAccountingTagTypes("FINANCIALS_REPORTS");
        assertEquals(STATEMENT_DETAILS + " accounting tag 1 is not correct", (String) accountingTags.get(new Integer(1)), "TEST_STMT_DTL_TAG1");
        assertEquals(STATEMENT_DETAILS + " accounting tag 2 is not correct", (String) accountingTags.get(new Integer(2)), "TEST_STMT_DTL_TAG2");
        assertEquals(STATEMENT_DETAILS + " accounting tag 3 is not correct", (String) accountingTags.get(new Integer(3)), "TEST_STMT_DTL_TAG3");
    }

    /**
     * tests supplier invoices and payments to suppliers with tags to make sure that the right amounts for tag combinations are posted to the ledger.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testSupplierInvoicePaymentCycle() throws GeneralException {
        // creates suppliers and organization
        String supplierPartyId1 = createPartyFromTemplate("DemoSupplier", "Test supplier 1 for accounting tags invoice and payments test " + UtilDateTime.nowTimestamp());
        String supplierPartyId2 = createPartyFromTemplate("DemoSupplier", "Test supplier 2 for accounting tags invoice and payments test " + UtilDateTime.nowTimestamp());
        String organizationPartyId = createOrganizationFromTemplate("Company", "Test organization for accounting tags invoice and payments test " + UtilDateTime.nowTimestamp());

        // standard costing needs to be set to null, or ledger posting accounts below may be different -- product item purchases will be fully charged to purchase price variance
        GenericValue partyAcctgPref = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", organizationPartyId));
        partyAcctgPref.set("costingMethodId", null);
        partyAcctgPref.store();

        // get GL accounts for all the invoice item types we will be using
        Map invoiceItemTypeGlAccounts = getGetInvoiceItemTypesGlAccounts(organizationPartyId, UtilMisc.toList("PINV_FPROD_ITEM", "PITM_SHIP_CHARGES", "PINV_SUPLPRD_ITEM", "PINV_SPROD_ITEM"));

        Timestamp start = UtilDateTime.nowTimestamp();
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // getting initial balances
        Map initialBalances_CONSUMER = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_CONSUMER"));
        Map initialBalances_GOV = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_GOV"));
        Map initialBalances_CONSUMER_SALES_TRAINING = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_CONSUMER", "tag2", "DPT_SALES", "tag3", "ACTI_TRAINING"), false);
        Map initialBalances_TRAINING = fa.getFinancialBalances(start, UtilMisc.toMap("tag3", "ACTI_TRAINING"), false);

        // create invoices and payments for different
        String invoiceId1 = fa.createInvoice(supplierPartyId1, "PURCHASE_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        fa.createInvoiceItem(invoiceId1, "PINV_FPROD_ITEM", "GZ-1000", new BigDecimal("100.0"), new BigDecimal("4.56"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER", "acctgTagEnumId2", "DPT_SALES", "acctgTagEnumId3", "ACTI_TRAINING"));
        fa.createInvoiceItem(invoiceId1, "PITM_SHIP_CHARGES", null, new BigDecimal("1.0"), new BigDecimal("13.95"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER", "acctgTagEnumId2", "DPT_SALES", "acctgTagEnumId3", "ACTI_TRAINING"));
        fa.createInvoiceItem(invoiceId1, "PINV_SUPLPRD_ITEM", null, new BigDecimal("1.0"), new BigDecimal("56.78"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER", "acctgTagEnumId2", "DPT_CUST_SERVICE", "acctgTagEnumId3", "ACTI_MARKETING"));

        fa.updateInvoiceStatus(invoiceId1, "INVOICE_READY");

        String invoiceId2 = fa.createInvoice(supplierPartyId1, "PURCHASE_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        fa.createInvoiceItem(invoiceId2, "PINV_SUPLPRD_ITEM", null, new BigDecimal("1.0"), new BigDecimal("5000.0"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER", "acctgTagEnumId2", "DPT_CORPORATE", "acctgTagEnumId3", "ACTI_RESEARCH"));
        fa.updateInvoiceStatus(invoiceId2, "INVOICE_READY");

        // this is the trick way to create the Payment and apply it to both invoices -- first createPaymentAndApplication but with a null invoiceId, then apply it to the 2 invoices
        String paymentId1 = fa.createPaymentAndApplication(new BigDecimal("5526.73"), organizationPartyId, supplierPartyId1,  "VENDOR_PAYMENT", "COMPANY_CHECK", "COCHECKING", null, "PMNT_NOT_PAID", UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
        runAndAssertServiceSuccess("createPaymentApplication", UtilMisc.toMap("paymentId", paymentId1, "invoiceId", invoiceId1, "amountApplied", new BigDecimal("526.73"), "userLogin", demofinadmin));
        runAndAssertServiceSuccess("createPaymentApplication", UtilMisc.toMap("paymentId", paymentId1, "invoiceId", invoiceId2, "amountApplied", new BigDecimal("5000.00"), "userLogin", demofinadmin));
        fa.updatePaymentStatus(paymentId1, "PMNT_SENT");

        String invoiceId3 = fa.createInvoice(supplierPartyId2, "PURCHASE_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        fa.createInvoiceItem(invoiceId3, "PINV_SPROD_ITEM", null, new BigDecimal("1.0"), new BigDecimal("5000.0"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_MANUFACTURING", "acctgTagEnumId3", "ACTI_PRODUCT"));
        fa.createInvoiceItem(invoiceId3, "PINV_SPROD_ITEM", null, new BigDecimal("1.0"), new BigDecimal("5000.0"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_WAREHOUSE", "acctgTagEnumId3", "ACTI_PRODUCT"));
        fa.createInvoiceItem(invoiceId3, "PINV_SPROD_ITEM", null, new BigDecimal("1.0"), new BigDecimal("5000.0"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_ACCOUNTING", "acctgTagEnumId3", "ACTI_TRAINING"));
        fa.createInvoiceItem(invoiceId3, "PINV_SPROD_ITEM", null, new BigDecimal("1.0"), new BigDecimal("10000.0"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE", "acctgTagEnumId2", "DPT_CORPORATE", "acctgTagEnumId3", "ACTI_TRAINING"));
        fa.updateInvoiceStatus(invoiceId3, "INVOICE_READY");

        String paymentId2 = fa.createPaymentAndApplication(new BigDecimal("10000.0"), organizationPartyId, supplierPartyId1,  "VENDOR_PAYMENT", "COMPANY_CHECK", "COCHECKING", invoiceId3, "PMNT_NOT_PAID", UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV"));
        fa.updatePaymentStatus(paymentId2, "PMNT_SENT");

        String paymentId3 = fa.createPaymentAndApplication(new BigDecimal("5000.0"), organizationPartyId, supplierPartyId1,  "VENDOR_PAYMENT", "COMPANY_CHECK", "COCHECKING", invoiceId3, "PMNT_NOT_PAID", UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        fa.updatePaymentStatus(paymentId3, "PMNT_SENT");

        // get the final accounting balances for different tag combinations.  Note that we can only check balance sheet consistency for the CONSUMER and GOV tags,
        // and not for the consumer, sales, training or training tags, because Accounts Payable is only configured to post for tag1 (division tag of consumer, government, enterprise, etc.)
        Timestamp finish = UtilDateTime.nowTimestamp();
        Map finalBalances_CONSUMER = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_CONSUMER"));
        Map finalBalances_GOV = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_GOV"));
        Map finalBalances_CONSUMER_SALES_TRAINING = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_CONSUMER", "tag2", "DPT_SALES", "tag3", "ACTI_TRAINING"), false);
        Map finalBalances_TRAINING = fa.getFinancialBalances(finish, UtilMisc.toMap("tag3", "ACTI_TRAINING"), false);

        // Check accounting changes for CONSUMER tag
        // the INV_FPROD_ITEM maps to uninvoiced shipment receipt when standard costing is not in use
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "0.0", "UNINVOICED_SHIP_RCPT", "456.0");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(invoiceItemTypeGlAccounts.get("PITM_SHIP_CHARGES"), "13.95");
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SUPLPRD_ITEM"), "5056.78");
        accountMap.put("111100", "-5526.73");
        assertMapDifferenceCorrect("Balance changes for CONSUMER tag is not correct", initialBalances_CONSUMER, finalBalances_CONSUMER, accountMap);

        // Check accounting changes for GOV
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "-5000.0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SPROD_ITEM"), "15000.0");
        accountMap.put("111100", "-10000.00");
        assertMapDifferenceCorrect("Balance changes for GOV tag is not correct", initialBalances_GOV, finalBalances_GOV, accountMap);

        // for these tag combinations, we only check the total expense amounts
        // Check accounting changes for consumer, sales, training tags combination
        expectedBalanceChanges = UtilMisc.toMap("UNINVOICED_SHIP_RCPT", "456.0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(invoiceItemTypeGlAccounts.get("PITM_SHIP_CHARGES"), "13.95");
        assertMapDifferenceCorrect("Balance changes for CONSUMER, SALES, TRAINING tag combination is not correct", initialBalances_CONSUMER_SALES_TRAINING, finalBalances_CONSUMER_SALES_TRAINING, accountMap);

        // check accounting changes for the training only tag
        expectedBalanceChanges = UtilMisc.toMap("UNINVOICED_SHIP_RCPT", "456.0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(invoiceItemTypeGlAccounts.get("PITM_SHIP_CHARGES"), "13.95");
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SPROD_ITEM"), "15000.00");
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SUPLPRD_ITEM"), "0.00");     //  no supplies products were purchased for training
        assertMapDifferenceCorrect("Balance changes for TRAINING tag is not correct", initialBalances_TRAINING, finalBalances_TRAINING, accountMap);

    }

    /**
     * Verifies that when payments are tagged for each application, they are posted to the correct accounting tags and GL accounts.
     * @throws GeneralException if an error occurs
     */
    public void testPaymentAccountingTagsAtAllocation() throws GeneralException {
        // create a copy of the Company organization
        String organizationPartyId = createOrganizationFromTemplate("Company", "Test organization for payment application accounting tags tests " + UtilDateTime.nowTimestamp());

        // set its PartyAcctgPreference.allocatePaymentTagsToApplications to "Y"
        GenericValue partyAcctgPref = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", organizationPartyId));
        partyAcctgPref.set("allocPaymentTagsToAppl", "Y");
        partyAcctgPref.store();

        // getting initial balances
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // create a purchase invoice from DemoSupplier for $1000 (PI1)
        String pi1 = fa.createInvoice("DemoSupplier", "PURCHASE_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        fa.createInvoiceItem(pi1, "PINV_SUPLPRD_ITEM", null, new BigDecimal("1.0"), new BigDecimal("1000.0"), null, null);
        fa.updateInvoiceStatus(pi1, "INVOICE_READY");
        // create a second purchase invoice from DemoSupplier for $2000 (PI2)
        String pi2 = fa.createInvoice("DemoSupplier", "PURCHASE_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        fa.createInvoiceItem(pi2, "PINV_SUPLPRD_ITEM", null, new BigDecimal("1.0"), new BigDecimal("2000.0"), null, null);
        fa.updateInvoiceStatus(pi2, "INVOICE_READY");

        // create a vendor payment of $2500 to DemoSupplier
        String paymentId = fa.createPaymentAndApplication(new BigDecimal("2500.0"), organizationPartyId, "DemoSupplier", "VENDOR_PAYMENT", "CASH", null, null, null);

        Timestamp start = UtilDateTime.nowTimestamp();
        Map halfBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map initialBalances_CONSUMER = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_CONSUMER"));
        Map initialBalances_GOV = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_GOV"));

        // apply $1000 of payment to PI1 with tags DIV_CONSUMER
        fa.updatePaymentApplication(new BigDecimal("1000.0"), paymentId, pi1, UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
        // verify that Payment.isReadyToPost() is false
        PaymentRepositoryInterface paymentRepository = billingDomain.getPaymentRepository();
        Payment payment = paymentRepository.getPaymentById(paymentId);
        assertFalse("Payment with ID [" + paymentId + "] should not ready to post.", payment.isReadyToPost());
        // apply $1500 of payment to PI2 with tags DIV_GOV
        fa.updatePaymentApplication(new BigDecimal("1500.0"), paymentId, pi2, UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV"));
        // reload payment
        payment = paymentRepository.getPaymentById(paymentId);
        // verify that Payment.isReadyToPost() is true
        assertTrue("Payment with ID [" + paymentId + "] should ready to post now.", payment.isReadyToPost());
        // Set payment status to SENT
        fa.updatePaymentStatus(paymentId, "PMNT_SENT");

        // get final balances
        Timestamp finish = UtilDateTime.nowTimestamp();
        Map finalBalances_CONSUMER = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_CONSUMER"));
        Map finalBalances_GOV = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_GOV"));

        // verify correct changes in balances.  See comment in PaymentTests.testVendorPaymentWithCheckingAccount: liabilities are always negative,
        // so a decrease in accounts payable is a positive amount.
        // ACCOUNTS_PAYABLE for DIV_CONSUMER, +1000
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "1000.0");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect("Balance changes for CONSUMER tag is not correct", initialBalances_CONSUMER, finalBalances_CONSUMER, accountMap);

        // ACCOUNTS_PAYABLE for DIV_GOV, +1500
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "1500.0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect("Balance changes for GOV tag is not correct", initialBalances_GOV, finalBalances_GOV, accountMap);

        // total ACCOUNTS_PAYABLE change, +2500
        Map finalBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "2500.0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(halfBalances, finalBalances, accountMap);
   }

    /**
     * Tests that if payments are at the GL account allocation level, the accounting tags are correctly associated with sales tax.
     * @throws GeneralException if an error occurs
     */
    public void testSalesTaxPaymentAccountingTagsAtAllocation() throws GeneralException {
        //create a copy of the Company organization
        String organizationPartyId = createOrganizationFromTemplate("Company", "Test organization for testSalesTaxPaymentAccountingTagsAtAllocation " + UtilDateTime.nowTimestamp());

        // set its PartyAcctgPreference.allocatePaymentTagsToApplications to "Y"
        GenericValue partyAcctgPref = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", organizationPartyId));
        partyAcctgPref.set("allocPaymentTagsToAppl", "Y");
        partyAcctgPref.store();

        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        Timestamp start = UtilDateTime.nowTimestamp();
        Map initialBalances_CONSUMER = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_CONSUMER"));
        Map initialBalances_GOV = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_GOV"));
        Map initialBalances = fa.getFinancialBalances(start);

        BigDecimal amount = new BigDecimal("3.21");
        String paymentId = fa.createPaymentAndApplication(amount, organizationPartyId, "CA_BOE", "SALES_TAX_PAYMENT", null, "COMMKT", null, null);

        //Create PaymentApplication with taxAuthGeoId=CA for this payment for the amount of 3.21
        Map<String, Object> ctxt = FastMap.newInstance();
        ctxt.put("userLogin", demofinadmin);
        ctxt.put("paymentId", paymentId);
        ctxt.put("amountApplied", amount);
        ctxt.put("taxAuthGeoId", "CA");
        ctxt.put("acctgTagEnumId1", "DIV_CONSUMER");
        runAndAssertServiceSuccess("createPaymentApplication", ctxt);

        // verify that Payment.isReadyToPost() is false
        PaymentRepositoryInterface paymentRepository = billingDomain.getPaymentRepository();
        Payment payment = paymentRepository.getPaymentById(paymentId);
        assertTrue("Payment with ID [" + paymentId + "] should ready to post now.", payment.isReadyToPost());

        // set payment to sent and cause it to be posted
        fa.updatePaymentStatus(paymentId, "PMNT_SENT");
        Timestamp finish = UtilDateTime.nowTimestamp();
        Map finalBalances_CONSUMER = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_CONSUMER"));
        Map finalBalances_GOV = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_GOV"));
        Map finalBalances = fa.getFinancialBalances(finish);

        GenericValue taxAuthGlAccount = delegator.findByPrimaryKey("TaxAuthorityGlAccount", UtilMisc.toMap("taxAuthGeoId", "CA", "taxAuthPartyId", "CA_BOE", "organizationPartyId", organizationPartyId));

        assertMapDifferenceCorrect(initialBalances, finalBalances, UtilMisc.toMap(taxAuthGlAccount.getString("glAccountId"), amount));
        assertMapDifferenceCorrect(initialBalances_CONSUMER, finalBalances_CONSUMER, UtilMisc.toMap(taxAuthGlAccount.getString("glAccountId"), amount));
        assertMapDifferenceCorrect(initialBalances_GOV, finalBalances_GOV, UtilMisc.toMap(taxAuthGlAccount.getString("glAccountId"), BigDecimal.ZERO));
    }

    /**
     * This test verifies that the correct accounting tag and GL account postings take place
     * when payments have accounting tags for payment applications,
     * and the payment is split between an override GL account and an invoice.
     * @throws GeneralException
     */
    public void testOverrideGlAccountPaymentWithAccountingTagAtPaymentApplication() throws GeneralException {
        //create a copy of the Company organization
        String organizationPartyId = createOrganizationFromTemplate("Company", "Organization for test override GlAccount Payment " + UtilDateTime.nowTimestamp());

        // set its PartyAcctgPreference.allocatePaymentTagsToApplications to "Y"
        GenericValue partyAcctgPref = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", organizationPartyId));
        partyAcctgPref.set("allocPaymentTagsToAppl", "Y");
        partyAcctgPref.store();

        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // this payment will be split between the invoice and an override gl account
        BigDecimal invoiceAmount = new BigDecimal("1000.0");
        BigDecimal overrideAmount = new BigDecimal("1500.0");
        BigDecimal paymentAmount = invoiceAmount.add(overrideAmount);

        // create the invoice
        String pi1 = fa.createInvoice("DemoSupplier", "PURCHASE_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        fa.createInvoiceItem(pi1, "PINV_SUPLPRD_ITEM", null, new BigDecimal("1.0"), invoiceAmount, null, null);
        fa.updateInvoiceStatus(pi1, "INVOICE_READY");

        // get the initial balances.  Payment will be split between consumer and gov, and enterprise should be zero throughout
        Timestamp start = UtilDateTime.nowTimestamp();
        Map initialBalances_CONSUMER = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_CONSUMER"));
        Map initialBalances_GOV = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_GOV"));
        Map initialBalances_ENTERPRISE = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_ENTERPRISE"));
        Map initialBalances = fa.getFinancialBalances(start);

        //create a vendor payment of $2500 to DemoSupplier
        String paymentId = fa.createPaymentAndApplication(paymentAmount, organizationPartyId, "DemoSupplier", "VENDOR_PAYMENT", "CASH", null, null, null);
        //apply $1000 of payment to PI1 with tags DIV_CONSUMER
        fa.updatePaymentApplication(invoiceAmount, paymentId, pi1, UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));

        // at this point the payment should not be ready to post yet
        PaymentRepositoryInterface paymentRepository = billingDomain.getPaymentRepository();
        Payment payment = paymentRepository.getPaymentById(paymentId);
        assertFalse("Payment with ID [" + paymentId + "] should not ready to post.", payment.isReadyToPost());

        // apply $1500 to 600000 with tag DIV_GOV
        String overrideGlAccountId = "600000";
        Map<String, Object> ctxt = FastMap.newInstance();
        ctxt.put("userLogin", demofinadmin);
        ctxt.put("paymentId", paymentId);
        ctxt.put("amountApplied", overrideAmount);
        ctxt.put("overrideGlAccountId", overrideGlAccountId);
        ctxt.put("acctgTagEnumId1", "DIV_GOV");
        runAndAssertServiceSuccess("createPaymentApplication", ctxt);

        // verify that now Payment.isReadyToPost() is true
        payment = paymentRepository.getPaymentById(paymentId);
        assertTrue("Payment with ID [" + paymentId + "] should ready to post now.", payment.isReadyToPost());

        // post the payment
        fa.updatePaymentStatus(paymentId, "PMNT_SENT");

        Timestamp finish = UtilDateTime.nowTimestamp();
        Map finalBalances_CONSUMER = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_CONSUMER"));
        Map finalBalances_GOV = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_GOV"));
        Map finalBalances_ENTERPRISE = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_ENTERPRISE"));
        Map finalBalances = fa.getFinancialBalances(finish);

        // overall, ACCOUNTS_PAYABLE +1000 and override GL account +1500
        Map expectedChange = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, UtilMisc.toMap("ACCOUNTS_PAYABLE", invoiceAmount), delegator);
        expectedChange.put(overrideGlAccountId, overrideAmount);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedChange);

        // for DIV_CONSUMER, ACCOUNTS_PAYABLE should be +1000 and 600000 should be unchanged
        expectedChange = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, UtilMisc.toMap("ACCOUNTS_PAYABLE", invoiceAmount), delegator);
        expectedChange.put(overrideGlAccountId, BigDecimal.ZERO);
        assertMapDifferenceCorrect(initialBalances_CONSUMER, finalBalances_CONSUMER, expectedChange);

        // for DIV_GOV, ACCOUNTS_PAYABLE should be unchanged and 600000 should be +1500
        expectedChange = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, UtilMisc.toMap("ACCOUNTS_PAYABLE", BigDecimal.ZERO), delegator);
        expectedChange.put(overrideGlAccountId, overrideAmount);
        assertMapDifferenceCorrect(initialBalances_GOV, finalBalances_GOV, expectedChange);

        // for DIV_ENTERPRISE, ACCOUNTS_PAYABLE and 600000 should be unchanged
        expectedChange = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, UtilMisc.toMap("ACCOUNTS_PAYABLE", BigDecimal.ZERO), delegator);
        expectedChange.put(overrideGlAccountId, BigDecimal.ZERO);
        assertMapDifferenceCorrect(initialBalances_ENTERPRISE, finalBalances_ENTERPRISE, expectedChange);

    }
}
