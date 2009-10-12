/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

import com.opensourcestrategies.financials.util.UtilFinancial;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
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
     * Verifies that when payments are tagged for each application, they are posted to the correct accounting tags and GL accounts
     */
    public void testPaymentAccountingTagsAtAllocation() throws GeneralException {
        // create a copy of the Company organization 
        String organizationPartyId = createOrganizationFromTemplate("Company", "Test organization for accounting tags payment application alloation " + UtilDateTime.nowTimestamp());
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        
        // set its PartyAcctgPreference.allocatePaymentTagsToApplications to "Y"
        // create a purchase invoice from DemoSupplier for $1000 (PI1)
        // create a second purchase invoice from DemoSupplier for $2000 (PI2)
        // create a vendor payment of $2500 to DemoSupplier
        
        // getting initial balances
        Timestamp start = UtilDateTime.nowTimestamp();
        Map initialBalances_CONSUMER = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_CONSUMER"));
        Map initialBalances_GOV = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_GOV"));
        Map initialBalances_CONSUMER_SALES_TRAINING = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_CONSUMER", "tag2", "DPT_SALES", "tag3", "ACTI_TRAINING"), false);
        Map initialBalances_TRAINING = fa.getFinancialBalances(start, UtilMisc.toMap("tag3", "ACTI_TRAINING"), false);

        // apply $1000 of payment to PI1 with tags DIV_CONSUMER, DPT_SALES, ACTI_TRAINING
        // verify that Payment.isReadyToPost() is false
        // apply $1500 of payment to PI2 with tags DIV_GOV, DPT_MANUFACTURING, ACTI_TRAINING
        // verify that Payment.isReadyToPost() is true
        // Set payment status to SENT
        
        // get final balances
        Timestamp finish = UtilDateTime.nowTimestamp();
        Map finalBalances_CONSUMER = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_CONSUMER"));
        Map finalBalances_GOV = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_GOV"));
        Map finalBalances_CONSUMER_SALES_TRAINING = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_CONSUMER", "tag2", "DPT_SALES", "tag3", "ACTI_TRAINING"), false);
        Map finalBalances_TRAINING = fa.getFinancialBalances(finish, UtilMisc.toMap("tag3", "ACTI_TRAINING"), false);

        // verify correct changes in balances:
        // ACCOUNTS_PAYABLE for DIV_CONSUMER, -1000
        // ACCOUNTS_PAYABLE for DIV_GOV, -1500
        // ACCOUNTS_PAYABLE for DIV_CONSUMER, DPT_SALES, ACTI_TRANINIG -1000
        // ACCOUNTS_PAYABLE for ACTI_TRAINING, -2500
        //
        // total ACCOUNTS_PAYABLE change, -2500
   }
}
