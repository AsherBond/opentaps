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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.opensourcestrategies.financials.accounts.AccountsHelper;
import org.ofbiz.accounting.invoice.InvoiceWorker;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

/**
 * Unit tests for creating and dealing with commission invoices.  They are based off the demo commissions
 * in CRMSFADemoCommission.xml.  These tests are overzealous in that all the commission agents are assigned
 * as InvoiceRoles to every invoice.  This allows us to test that certain invoices are NOT generated.
 *
 * Here's an overview of what each agent should be earning, where products are counted separately unless noted:
 * <ul>
 * <li>DemoSalesRep1 earns 7% flat commission for all products.</li>
 * <li>DemoSalesRep2 earns commission on products from categories A, B and C with rates of 25%, 20% and 15% respectively.</li>
 * <li>DemoSalesRep3 earns commission if invoice is to DemoCustCompany and the products are in category A, B and C.  The rates earned depend on quantity:
 *   <ul>
 *     <li>Category A:  30% for 1-9 products, 35% for 10 or more.</li>
 *     <li>Category B:  20% for 1-9 products, 25% for 10 or more.  All products from this category are counted as one.</li>
 *     <li>Category C:  No commission for 1-9 products, 15% for 10 or more.</li>
 *   </ul>
 * </li>
 * </ul>
 *
 * The commissionable invoice item types are defined as AgreementInvoiceItemType mappings in CommissionSeedData.xml.
 * The available sales invoice item types are defined in AccountingTypeData.xml and FinancialsSeedData.xml.
 *
 * Note that these tests are for commissions generated from sales invoices that are created manually in finanicals.
 *
 * TODO: following requires documentation
 * Invoices are sum( item amount * item quantity) over all items, so it does not match the items with their adjustments before multiplying by quantity, making it different from expected
 *     Commission is applied on the same principle, so the adding function on an invoice must be fixed first.
 * Example:  Order of 3 products worth 8.78 with a discount of 1.12 eah should be 22.98, but the system results in 22.99.
 *
 * Whole order adjustments are not commissioned unless it's a flat rate on everything.  This means that there has to be a special term.
 *    to process miscellaneous charges with a flat rate.
 *
 * Note that by default, (1.785) rounds to (1.79) due to ROUND_HALF_UP.
 *
 * If you enter an invoice line item for 10 GZ-1000 and there are adjustments on it, you can enter the adjustment
 * either as 10 * unit adjustment value or 1 * total adjustment value.  The algorithm for generating the commission
 * is smart enough to detect this, but make sure that item references the product GZ-1000, otherwise it won't get counted!
 */
public class CommissionInvoiceTests extends FinancialsTestCase {

    /** Organization that agents earn commission from. */
    public final String organizationPartyId = "Company";

    /**
     * Tests the generation of commissions from a complex sales invoice.
     * Verifies the commission invoicing, balance, and payment cycle.
     *
     * The sales invoice is made to DemoCustCompany for a set of products in
     * categories A, B and C, as well as outside.
     * @exception GeneralException if an error occurs
     */
    public void testComplexCommissionInvoiceLifecycle() throws GeneralException {
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Map initialBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // create sales invoice to DemoCustCompany
        String invoiceId = financialAsserts.createInvoice("DemoCustCompany", "SALES_INVOICE", UtilDateTime.nowTimestamp(), "Complex Test Sales Invoice for Commission", "testcomm1", "Complex test of creating commission invoices.  This sales invoice should trigger the creation of commission invoices when set to ready.");

        // invoice two products from category A for less than 10 to ensure rep 3 earns 30% on these
        financialAsserts.createInvoiceItem(invoiceId, "INV_PROD_ITEM", "GZ-7000", new BigDecimal("7.0"), new BigDecimal("10.11"), "Sales item which should be commissioned.");
        financialAsserts.createInvoiceItem(invoiceId, "INV_PROD_ITEM", "GZ-8544", new BigDecimal("4.0"), new BigDecimal("11.10"));

        // invoice two products from category B whose combined quantity triggers 25% earnings
        financialAsserts.createInvoiceItem(invoiceId, "INV_DPROD_ITEM", "GZ-2002", new BigDecimal("2.0"), new BigDecimal("12.34"));
        financialAsserts.createInvoiceItem(invoiceId, "INV_FDPROD_ITEM", "GZ-5005", new BigDecimal("10.0"), new BigDecimal("15.01"));

        // discounts for the category B products that should also apply to the 25% discount
        financialAsserts.createInvoiceItem(invoiceId, "ITM_DISCOUNT_ADJ", "GZ-2002", new BigDecimal("2.0"), new BigDecimal("-0.42"));
        financialAsserts.createInvoiceItem(invoiceId, "ITM_DISCOUNT_ADJ", "GZ-5005", new BigDecimal("10.0"), new BigDecimal("-2.55"));

        // invoice a product in category C for a quantity that triggers no earnings for rep 3
        financialAsserts.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1000", new BigDecimal("3.0"), new BigDecimal("8.78"));

        // discount for the last item that should trigger no commission for rep 3
        financialAsserts.createInvoiceItem(invoiceId, "ITM_PROMOTION_ADJ", "GZ-1000", new BigDecimal("3.0"), new BigDecimal("-1.12"));

        // invoice a product outside the categories, of which only rep 1 should be commissioning
        financialAsserts.createInvoiceItem(invoiceId, "INV_SPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("3.19"));

        // whole order promotion, which gets processed only for rep 1
        financialAsserts.createInvoiceItem(invoiceId, "ITM_PROMOTION_ADJ", new BigDecimal("1.0"), new BigDecimal("-45.67"));

        // create other adjustments and items that should not be invoiced
        financialAsserts.createInvoiceItem(invoiceId, "ITM_FEE", null, null, new BigDecimal("133.33"), "Sales Item which should NOT be commissioned.");
        financialAsserts.createInvoiceItem(invoiceId, "ITM_MISC_CHARGE", null, null, new BigDecimal("0.15"));
        financialAsserts.createInvoiceItem(invoiceId, "ITM_SALES_TAX", null, null, new BigDecimal("155.56"));
        financialAsserts.createInvoiceItem(invoiceId, "ITM_SHIPPING_CHARGES", null, null, new BigDecimal("77.99"));

        // DemoSalesRep1 and _2 must be named as commission agents on the order -> invoice to get a commission but _3 does not:
        // he has an agreement to get commission on any order/invoice with this customer
        // note that this implies both rep 1 and 2 created the order, something that cannot really happen but is useful for testing
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("invoiceId", invoiceId);
        input.put("partyId", "DemoSalesRep1");
        input.put("roleTypeId", "COMMISSION_AGENT");
        runAndAssertServiceSuccess("createInvoiceRole", input);
        input.put("partyId", "DemoSalesRep2");
        runAndAssertServiceSuccess("createInvoiceRole", input);

        // note the time so we can easily fetch any invoices created
        Timestamp start = UtilDateTime.nowTimestamp();

        pause("Mysql timestamp workaround pause");
        // set the invoice to ready, which should trigger creation of commissions
        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        // get the commission invoices created
        List<EntityCondition> conditions = Arrays.<EntityCondition>asList(
                EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "COMMISSION_INVOICE"),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, "DemoSalesRep1"),
                EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("invoiceDate", EntityOperator.GREATER_THAN, start)
        );
        List<GenericValue> commissionInvoices = delegator.findByAnd("Invoice", conditions);
        assertEquals("Incorrect number commission invoice created for DemoSalesRep1.", new BigDecimal(commissionInvoices.size()), new BigDecimal("1.0"));
        GenericValue commissionInvoice1 = commissionInvoices.get(0);
        String commissionInvoiceId1 = commissionInvoice1.getString("invoiceId");

        conditions = Arrays.<EntityCondition>asList(
                EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "COMMISSION_INVOICE"),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, "DemoSalesRep2"),
                EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("invoiceDate", EntityOperator.GREATER_THAN, start)
        );
        commissionInvoices = delegator.findByAnd("Invoice", conditions);
        assertEquals("Incorrect number commission invoice created for DemoSalesRep2.", new BigDecimal(commissionInvoices.size()), new BigDecimal("1.0"));
        GenericValue commissionInvoice2 = commissionInvoices.get(0);
        String commissionInvoiceId2 = commissionInvoice2.getString("invoiceId");

        conditions = Arrays.<EntityCondition>asList(
                EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "COMMISSION_INVOICE"),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, "DemoSalesRep3"),
                EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("invoiceDate", EntityOperator.GREATER_THAN, start)
        );
        commissionInvoices = delegator.findByAnd("Invoice", conditions);
        assertEquals("Incorrect number commission invoice created for DemoSalesRep3.", new BigDecimal(commissionInvoices.size()), new BigDecimal("1.0"));
        GenericValue commissionInvoice3 = commissionInvoices.get(0);
        String commissionInvoiceId3 = commissionInvoice3.getString("invoiceId");

        // add a commission adjustment to all invoices
        financialAsserts.createInvoiceItem(commissionInvoiceId1, "COMM_INV_ADJ", new BigDecimal("1.0"), new BigDecimal("-2.13"), "Processing fee for this commission.");
        financialAsserts.createInvoiceItem(commissionInvoiceId2, "COMM_INV_ADJ", new BigDecimal("1.0"), new BigDecimal("-2.13"), "Processing fee for this commission.");
        financialAsserts.createInvoiceItem(commissionInvoiceId3, "COMM_INV_ADJ", new BigDecimal("1.0"), new BigDecimal("-2.13"), "Processing fee for this commission.");

        BigDecimal commissionTotal1 = InvoiceWorker.getInvoiceTotal(commissionInvoice1);
        BigDecimal commissionTotal2 = InvoiceWorker.getInvoiceTotal(commissionInvoice2);
        BigDecimal commissionTotal3 = InvoiceWorker.getInvoiceTotal(commissionInvoice3);

        // these are the expected totals (these are calculated by hand)
        BigDecimal expectedTotal1 = asBigDecimal("14.94");
        BigDecimal expectedTotal2 = asBigDecimal("59.80");
        BigDecimal expectedTotal3 = asBigDecimal("69.53");
        BigDecimal expectedTotal = expectedTotal1.add(expectedTotal2.add(expectedTotal3));

        assertEquals("Commission earned by DemoSalesRep1 is as expected.", commissionTotal1, expectedTotal1);
        assertEquals("Commission earned by DemoSalesRep2 is as expected.", commissionTotal2, expectedTotal2);
        assertEquals("Commission earned by DemoSalesRep3 is as expected.", commissionTotal3, expectedTotal3);

        // ensure that the commission balances go up by the correct amount when the invoice is set to ready
        BigDecimal initialCommBalance1 = AccountsHelper.getBalanceForCommissionPartyId("DemoSalesRep1", organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        BigDecimal initialCommBalance2 = AccountsHelper.getBalanceForCommissionPartyId("DemoSalesRep2", organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        BigDecimal initialCommBalance3 = AccountsHelper.getBalanceForCommissionPartyId("DemoSalesRep3", organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        pause("Mysql timestamp workaround pause");
        financialAsserts.updateInvoiceStatus(commissionInvoiceId1, "INVOICE_READY");
        financialAsserts.updateInvoiceStatus(commissionInvoiceId2, "INVOICE_READY");
        financialAsserts.updateInvoiceStatus(commissionInvoiceId3, "INVOICE_READY");

        BigDecimal finalCommBalance1 = AccountsHelper.getBalanceForCommissionPartyId("DemoSalesRep1", organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        BigDecimal finalCommBalance2 = AccountsHelper.getBalanceForCommissionPartyId("DemoSalesRep2", organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        BigDecimal finalCommBalance3 = AccountsHelper.getBalanceForCommissionPartyId("DemoSalesRep3", organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);

        BigDecimal commissionBalance1 = finalCommBalance1.subtract(initialCommBalance1);
        BigDecimal commissionBalance2 = finalCommBalance2.subtract(initialCommBalance2);
        BigDecimal commissionBalance3 = finalCommBalance3.subtract(initialCommBalance3);

        assertEquals("Commission balance increases by $" + expectedTotal1 + " for DemoSalesRep1 after commission invoice is set to ready", commissionBalance1, expectedTotal1);
        assertEquals("Commission balance increases by $" + expectedTotal2 + " for DemoSalesRep2 after commission invoice is set to ready", commissionBalance2, expectedTotal2);
        assertEquals("Commission balance increases by $" + expectedTotal3 + " for DemoSalesRep3 after commission invoice is set to ready", commissionBalance3, expectedTotal3);

        // create a commission payment from the company checking account for each agent
        FinancialAsserts fa = new FinancialAsserts(this, "DemoSalesRep1", demofinadmin);
        fa.createPaymentAndApplication(expectedTotal1, organizationPartyId, organizationPartyId, "COMMISSION_PAYMENT", "COMPANY_CHECK", null, commissionInvoiceId1, "PMNT_SENT");
        fa = new FinancialAsserts(this, "DemoSalesRep2", demofinadmin);
        fa.createPaymentAndApplication(expectedTotal2, organizationPartyId, organizationPartyId, "COMMISSION_PAYMENT", "COMPANY_CHECK", null, commissionInvoiceId2, "PMNT_SENT");
        fa = new FinancialAsserts(this, "DemoSalesRep3", demofinadmin);
        fa.createPaymentAndApplication(expectedTotal3, organizationPartyId, organizationPartyId, "COMMISSION_PAYMENT", "COMPANY_CHECK", null, commissionInvoiceId3, "PMNT_SENT");

        // verify change in sales commission (601300) and commission adjustment (601400) accounts
        // note that the sales commission will be the expectedTotal without the adjustments
        BigDecimal commissionAdjTotal = new BigDecimal("-24.04"); // since this is painful to calculate by hand, this came from a reference value
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map expectedBalanceChanges = UtilMisc.toMap("601300", expectedTotal.subtract(commissionAdjTotal), "601400", commissionAdjTotal);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);

        // verify that the agreement term billing report entity has the right values for the sales invoice
        // note that these amounts do not include manual adjustments as above, so the invoice expected totals are different
        List<String> fields = UtilMisc.toList("partyIdFrom", "agentPartyId", "amount", "origInvoiceId");
        conditions = Arrays.<EntityCondition>asList(
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("origInvoiceId", EntityOperator.EQUALS, invoiceId)
        );
        List<GenericValue> report = delegator.findByCondition("AgreementBillingAndInvoiceSum", EntityCondition.makeCondition(conditions, EntityOperator.AND), fields, null);
        BigDecimal rep1Total = BigDecimal.ZERO;
        BigDecimal rep2Total = BigDecimal.ZERO;
        BigDecimal rep3Total = BigDecimal.ZERO;
        for (GenericValue line : report) {
            if ("DemoSalesRep1".equals(line.get("agentPartyId"))) {
                rep1Total = rep1Total.add(line.getBigDecimal("amount"));
            }
            if ("DemoSalesRep2".equals(line.get("agentPartyId"))) {
                rep2Total = rep2Total.add(line.getBigDecimal("amount"));
            }
            if ("DemoSalesRep3".equals(line.get("agentPartyId"))) {
                rep3Total = rep3Total.add(line.getBigDecimal("amount"));
            }
        }
        assertEquals("Commission report for DemoSalesRep1 is as expected.", rep1Total, expectedTotal1.add(asBigDecimal("2.13")));
        assertEquals("Commission report for DemoSalesRep2 is as expected.", rep2Total, expectedTotal2.add(asBigDecimal("2.13")));
        assertEquals("Commission report for DemoSalesRep3 is as expected.", rep3Total, expectedTotal3.add(asBigDecimal("2.13")));
    }

    /**
     * Test for commissions which are only earned when the customer's invoices are paid.
     * 1.  Create a sales invoice from Company to DemoAccount1 for $100
     * 2.  Mark invoice as READY
     * 3.  Create a sales invoice from Company to DemoAccount1 for $200
     * 4.  Mark invoice as READY
     * 5.  Check that no commission invoice has been created for DemoSalesRep4
     * 6.  Receive Payment from DemoAccount1 to Company for $100 and apply $50 of it to invoice from (1) and $30 of it to invoice from (2)
     * 7.  Set Payment to RECEIVED
     * 8.  Check that no commission invoice has been created for DemoSalesRep4
     * 9.  Set Payment to CONFIRMED
     * 10.  Check that 1 commission invoice has been created from DemoSalesRep4 to Company for $2.50 (5% * $50) and
     *     1 commission invoice has been created from DemoSalesRep4 to Company for $1.50 (5% * $30)
     * 11.  Receive second Payment from DemoAccount1 to Company for $30, apply $30 of it to invoice from (1)
     * 12.  Set Payment to Cancelled
     * 13.  Receive third Payment from DemoAccount1 to Company for $100, apply $50 of it to invoice from (1)
     * 14.  Set Payment to Received, then set Payment to VOID
     * 15.  Verify that no new commission invoice has been created for DemoSalesRep4 since (8) above.
     * @exception GeneralException if an error occurs
     */
    public void testCommissionsEarnedAtPayment() throws GeneralException {
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Timestamp now = UtilDateTime.nowTimestamp();
        pause("Mysql timestamp workaround pause");

        // copy DemoSalesRep4, which has a commission agreement we want to test
        String agentPartyId = createPartyFromTemplate("DemoSalesRep4", "FirstName for testCommissionsEarnedAtPayment", "LastName for testCommissionsEarnedAtPayment");

        // 1. Create a sales invoice from Company to DemoAccount1 for $100
        String invoiceId1 = financialAsserts.createInvoice("DemoAccount1", "SALES_INVOICE");//, UtilDateTime.nowTimestamp(), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, TimeZone.getDefault(), Locale.US));
        financialAsserts.createInvoiceItem(invoiceId1, "INV_PROD_ITEM", "GZ-1001", new BigDecimal("3.0"), new BigDecimal("10.00"), "Item 1");
        financialAsserts.createInvoiceItem(invoiceId1, "INV_PROD_ITEM", "GZ-1005", new BigDecimal("1.0"), new BigDecimal("70.00"), "Item 2");

        // 2. Mark invoice as READY
        financialAsserts.updateInvoiceStatus(invoiceId1, "INVOICE_READY");

        // 3. Create a sales invoice from Company to DemoAccount1 for $200
        String invoiceId2 = financialAsserts.createInvoice("DemoAccount1", "SALES_INVOICE");//, UtilDateTime.nowTimestamp(), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, TimeZone.getDefault(), Locale.US));
        financialAsserts.createInvoiceItem(invoiceId2, "INV_PROD_ITEM", new BigDecimal("2.0"), new BigDecimal("100.0"), "Item 1");

        // 4. Mark invoice as READY
        financialAsserts.updateInvoiceStatus(invoiceId2, "INVOICE_READY");

        // 5. Check that no commission invoice has been created for DemoSalesRep4
        List<EntityCondition> conditions = Arrays.<EntityCondition>asList(
                EntityCondition.makeCondition("invoiceDate", EntityOperator.GREATER_THAN, now),
                EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "COMMISSION_INVOICE"),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, agentPartyId)
        );
        long count = delegator.findCountByCondition("Invoice", EntityCondition.makeCondition(conditions, EntityOperator.AND), null);
        assertEquals("Some commission invoices are created for DemoSalesRep4 by mistake", 0, count);

        // 6. Receive Payment from DemoAccount1 to Company for $100 and apply $50 of it to invoice from (1) and $30 of it to invoice from (2)
        String paymentId1 = financialAsserts.createPayment(new BigDecimal("100.0"), "DemoAccount1", "CUSTOMER_PAYMENT", "COMPANY_CHECK");
        runAndAssertServiceSuccess("createPaymentApplication", UtilMisc.<String, Object>toMap("userLogin", demofinadmin, "paymentId", paymentId1, "invoiceId", invoiceId1, "amountApplied", new BigDecimal("50.0")));
        runAndAssertServiceSuccess("createPaymentApplication", UtilMisc.<String, Object>toMap("userLogin", demofinadmin, "paymentId", paymentId1, "invoiceId", invoiceId2, "amountApplied", new BigDecimal("30.0")));

        // 7. Set Payment to RECEIVED
        financialAsserts.updatePaymentStatus(paymentId1, "PMNT_RECEIVED");

        // 8. Check that no commission invoice has been created for DemoSalesRep4
        count = delegator.findCountByCondition("Invoice", EntityCondition.makeCondition(conditions, EntityOperator.AND), null);
        assertEquals("Some commission invoices are created for DemoSalesRep4 by mistake", 0, count);

        // 9. Set Payment to CONFIRMED
        financialAsserts.updatePaymentStatus(paymentId1, "PMNT_CONFIRMED");

        // 10. Check that 1 commission invoice has been created from DemoSalesRep4 to Company for $2.50 (5% * $50) and
        //     1 commission invoice has been created from DemoSalesRep4 to Company for $1.50 (5% * $30)
        conditions = Arrays.<EntityCondition>asList(
                EntityCondition.makeCondition("invoiceDate", EntityOperator.GREATER_THAN, now),
                EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "COMMISSION_INVOICE"),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, agentPartyId)
        );
        List<GenericValue> invoices = delegator.findByCondition("Invoice", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, null);
        assertEquals("Wrong count of comission invoices", 2, invoices.size());

        BigDecimal commInv1Total = InvoiceWorker.getInvoiceTotal(invoices.get(0));
        BigDecimal commInv2Total = InvoiceWorker.getInvoiceTotal(invoices.get(1));

        assertEquals("Wrong commission amount", commInv1Total.max(commInv2Total), new BigDecimal("2.5"));
        assertEquals("Wrong commission amount", commInv1Total.min(commInv2Total), new BigDecimal("1.5"));

        now = UtilDateTime.nowTimestamp();
        pause("Mysql timestamp workaround pause");

        // 11. Receive second Payment from DemoAccount1 to Company for $30, apply $30 of it to invoice from (1)
        String paymentId2 = financialAsserts.createPayment(new BigDecimal("30.0"), "DemoAccount1", "CUSTOMER_PAYMENT", "COMPANY_CHECK");
        runAndAssertServiceSuccess("createPaymentApplication", UtilMisc.<String, Object>toMap("userLogin", demofinadmin, "paymentId", paymentId2, "invoiceId", invoiceId1, "amountApplied", new BigDecimal("30.0")));

        // 12. Set Payment to Cancelled
        financialAsserts.updatePaymentStatus(paymentId2, "PMNT_CANCELLED");

        // 13. Receive third Payment from DemoAccount1 to Company for $100, apply $50 of it to invoice from (1)
        String paymentId3 = financialAsserts.createPayment(new BigDecimal("100.0"), "DemoAccount1", "CUSTOMER_PAYMENT", "COMPANY_CHECK");
        runAndAssertServiceSuccess("createPaymentApplication", UtilMisc.<String, Object>toMap("userLogin", demofinadmin, "paymentId", paymentId3, "invoiceId", invoiceId1, "amountApplied", new BigDecimal("50.0")));

        // 14. Set Payment to Received, then set Payment to VOID
        financialAsserts.updatePaymentStatus(paymentId3, "PMNT_RECEIVED");
        financialAsserts.updatePaymentStatus(paymentId3, "PMNT_VOID");

        // 15. Verify that no new commission invoice has been created for DemoSalesRep4 since (8) above.
        conditions = Arrays.<EntityCondition>asList(
                EntityCondition.makeCondition("invoiceDate", EntityOperator.GREATER_THAN, now),
                EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "COMMISSION_INVOICE"),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, agentPartyId)
        );
        count = delegator.findCountByCondition("Invoice", EntityCondition.makeCondition(conditions, EntityOperator.AND), null);
        assertEquals("No new commission invoices should be created from last ckeck.", 0, count);

        // verify that the agreement term billing report entity has the right values for the agent
        // note that these amounts do not include manual adjustments as above, so the invoice expected totals are different
        List<String> fields = UtilMisc.toList("partyIdFrom", "agentPartyId", "amount", "origInvoiceId");
        conditions = Arrays.<EntityCondition>asList(
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("origInvoiceId", EntityOperator.IN, UtilMisc.toList(invoiceId1, invoiceId2)),
                EntityCondition.makeCondition("agentPartyId", EntityOperator.EQUALS, agentPartyId)
        );
        List<GenericValue> report = delegator.findByCondition("AgreementBillingAndInvoiceSum", EntityCondition.makeCondition(conditions, EntityOperator.AND), fields, null);
        BigDecimal agentTotal = BigDecimal.ZERO;
        for (GenericValue line : report) {
            agentTotal = agentTotal.add(line.getBigDecimal("amount"));
        }
        assertEquals("DemoSalesRep4 commission report as expected", agentTotal, asBigDecimal("4.0"));
    }

}
