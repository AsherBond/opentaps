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

import javolution.util.FastList;
import org.ofbiz.accounting.invoice.InvoiceWorker;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for creating and dealing with partner invoices.
 */
public class PartnerInvoiceTests extends FinancialsTestCase {

    /** Organization with partners */
    public final String organizationPartyId = "Company";

    /**
     * Creates a bunch of PARTNER_INVOICEs and then creates a SALES_INVOICE from them based on a PARTNER_AGREEMENT.
     * Tests that the invoice total of the sales invoice is correct and that the parties are correct.
     * This method specifies the agreement to be used.
     */
    public void testCreatePartnerSalesInvoiceFromAgreement() throws GeneralException {
        List<String> invoiceIdsInput = createPartnerInvoices();
        Map input = UtilMisc.toMap("userLogin", demofinadmin);
        input.put("invoiceIds", invoiceIdsInput);

        // verify that not specifying an agreement causes a service error
        runAndAssertServiceError("opentaps.createPartnerSalesInvoice", input);

        // create the partner sales invoice by specifying the agreement
        input.put("agreementId", "PARTNER_COMM_AGR1");
        Map results = runAndAssertServiceSuccess("opentaps.createPartnerSalesInvoice", input);
        String invoiceId = (String) results.get("invoiceId");
        assertNotNull("Invoice ID generated for partner sales invoice.", invoiceId);

        // verify that this invoice is a sales invoice from company to partner
        GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        assertEquals("Partner sales invoice is a SALES_INVOICE", invoice.getString("invoiceTypeId"), "SALES_INVOICE");
        assertEquals("Partner sales invoice is from Company", invoice.getString("partyIdFrom"), "Company");
        assertEquals("Partner sales invoice is to demopartner1", invoice.getString("partyId"), "demopartner1");

        // verify the total is 70% of the partner invoice totals (133.33 * 3 * 0.70) = 279.993 -> 279.99
        BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotalBd(invoice);
        BigDecimal expectedAmount = asBigDecimal("279.99");
        assertEquals("Sales invoice to partner has expected total value.", invoiceTotal, expectedAmount);

        List conditions = UtilMisc.toList(
                new EntityExpr("invoiceId", EntityOperator.IN, invoiceIdsInput),
                new EntityExpr("statusId", EntityOperator.EQUALS, "INVOICE_INV_PTNR")
        );
        List<GenericValue> partnerInvoices = delegator.findByAnd("Invoice", conditions);
        assertTrue("Partner invoices marked as invoiced to partner", partnerInvoices.size() == 3);
    }

    /**
     * Creates a bunch of PARTNER_INVOICEs and then creates a SALES_INVOICE from them based on a PARTNER_AGREEMENT.
     * Tests that the invoice total of the sales invoice is correct and that the parties are correct.
     * This method specifies the parties to be used.
     */
    public void testCreatePartnerSalesInvoiceFromParties() throws GeneralException {
        List<String> invoiceIdsInput = createPartnerInvoices();
        Map input = UtilMisc.toMap("userLogin", demofinadmin);
        input.put("invoiceIds", invoiceIdsInput);

        // verify that not specifying the parties causes a service error
        runAndAssertServiceError("opentaps.createPartnerSalesInvoice", input);

        // create the partner sales invoice by specifying the parties
        input.put("organizationPartyId", organizationPartyId);
        input.put("partnerPartyId", "demopartner1");
        Map results = runAndAssertServiceSuccess("opentaps.createPartnerSalesInvoice", input);
        String invoiceId = (String) results.get("invoiceId");
        assertNotNull("Invoice ID generated for partner sales invoice.", invoiceId);

        // verify that this invoice is a sales invoice from company to partner
        GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        assertEquals("Partner sales invoice is a SALES_INVOICE", invoice.getString("invoiceTypeId"), "SALES_INVOICE");
        assertEquals("Partner sales invoice is from Company", invoice.getString("partyIdFrom"), "Company");
        assertEquals("Partner sales invoice is to demopartner1", invoice.getString("partyId"), "demopartner1");

        // verify the total is 70% of the partner invoice totals (133.33 * 3 * 0.70) = 279.993 -> 279.99
        BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotalBd(invoice);
        BigDecimal expectedAmount = asBigDecimal("279.99");
        assertEquals("Sales invoice to partner has expected total value.", invoiceTotal, expectedAmount);

        List conditions = UtilMisc.toList(
                new EntityExpr("invoiceId", EntityOperator.IN, invoiceIdsInput),
                new EntityExpr("statusId", EntityOperator.EQUALS, "INVOICE_INV_PTNR")
        );
        List<GenericValue> partnerInvoices = delegator.findByAnd("Invoice", conditions);
        assertTrue("Partner invoices marked as invoiced to partner", partnerInvoices.size() == 3);
    }

    // Creates three PARTNER_INVOICEs with one product line item.  The invoice totals are 100, 133.33, 166.66 or (133.33 * 3)
    private List<String> createPartnerInvoices() throws GeneralException {
        List<String> invoiceIds = FastList.newInstance();

        // create several invoices between partner [demopartner1] and its customers [dp1acct1], [dp1acct2], and [dp1a1contact1]
        FinancialAsserts fa = new FinancialAsserts(this, "demopartner1", demofinadmin);

        String invoice = fa.createInvoice("dp1acct1", "PARTNER_INVOICE",
                                          UtilDateTime.nowTimestamp(),
                                          "Test Partner Invoice #1",
                                          "testpartner1",
                                          "Test of partner invoice.  This invoice is " +
                                          "between our partner demopartner1 and its customer " +
                                          "dp1acct1.");
        invoiceIds.add(invoice);

        invoice = fa.createInvoice("dp1acct2", "PARTNER_INVOICE",
                                   UtilDateTime.nowTimestamp(),
                                   "Test Partner Invoice #2",
                                   "testpartner2",
                                   "Test of partner invoice.  This invoice is " +
                                   "between our partner demopartner1 and its customer " +
                                   "dp1acct2.");
        invoiceIds.add(invoice);

        invoice = fa.createInvoice("dp1a1contact1", "PARTNER_INVOICE",
                                   UtilDateTime.nowTimestamp(),
                                   "Test Partner Invoice #3",
                                   "testpartner3",
                                   "Test of partner invoice.  This invoice is " +
                                   "between our partner demopartner1 and its customer " +
                                   "dp1a1contact1.");
        invoiceIds.add(invoice);

        // add a basic product line item amount to each invoice and mark as ready (note quantity is null to see if anything crashes)
        double amount = 100;
        for (String invoiceId : invoiceIds) {
            fa.createInvoiceItem(invoiceId, "INV_PROD_ITEM", "GZ-1000", null, new Double(amount), "Test Product Item for partner invoice.");
            amount += 33.33;
        }

        // add positive and negative adjustments that cancel out
        for (String invoiceId : invoiceIds) {
            fa.createInvoiceItem(invoiceId, "ITM_DISCOUNT_ADJ", null, null, new Double(12.14), "Test Product Item for partner invoice.");
            fa.createInvoiceItem(invoiceId, "ITM_DISCOUNT_ADJ", null, null, new Double(-12.14), "Test Product Item for partner invoice.");
        }

        // set all invoices to ready
        for (String invoiceId : invoiceIds) {
            fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");
        }

        return invoiceIds;
    }
}
