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
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import org.ofbiz.accounting.invoice.InvoiceWorker;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

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
     * @exception GeneralException if an error occurs
     */
    public void testCreatePartnerSalesInvoiceFromAgreement() throws GeneralException {
        List<String> invoiceIdsInput = createPartnerInvoices();
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("invoiceIds", invoiceIdsInput);

        // verify that not specifying an agreement causes a service error
        runAndAssertServiceError("opentaps.createPartnerSalesInvoice", input);

        // create the partner sales invoice by specifying the agreement
        input.put("agreementId", "PARTNER_COMM_AGR1");
        Map<String, Object> results = runAndAssertServiceSuccess("opentaps.createPartnerSalesInvoice", input);
        String invoiceId = (String) results.get("invoiceId");
        assertNotNull("Invoice ID generated for partner sales invoice.", invoiceId);

        // verify that this invoice is a sales invoice from company to partner
        GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        assertEquals("Partner sales invoice is a SALES_INVOICE", invoice.getString("invoiceTypeId"), "SALES_INVOICE");
        assertEquals("Partner sales invoice is from Company", invoice.getString("partyIdFrom"), "Company");
        assertEquals("Partner sales invoice is to demopartner1", invoice.getString("partyId"), "demopartner1");

        // verify the total is 70% of the partner invoice totals (133.33 * 3 * 0.70) = 279.993 -> 279.99
        BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice);
        BigDecimal expectedAmount = asBigDecimal("279.99");
        assertEquals("Sales invoice to partner has expected total value.", invoiceTotal, expectedAmount);

        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("invoiceId", EntityOperator.IN, invoiceIdsInput),
                EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "INVOICE_INV_PTNR"));
        List<GenericValue> partnerInvoices = delegator.findByAnd("Invoice", conditions);
        assertTrue("Partner invoices marked as invoiced to partner", partnerInvoices.size() == 3);
    }

    /**
     * Creates a bunch of PARTNER_INVOICEs and then creates a SALES_INVOICE from them based on a PARTNER_AGREEMENT.
     * Tests that the invoice total of the sales invoice is correct and that the parties are correct.
     * This method specifies the parties to be used.
     * @exception GeneralException if an error occurs
     */
    public void testCreatePartnerSalesInvoiceFromParties() throws GeneralException {
        List<String> invoiceIdsInput = createPartnerInvoices();
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("invoiceIds", invoiceIdsInput);

        // verify that not specifying the parties causes a service error
        runAndAssertServiceError("opentaps.createPartnerSalesInvoice", input);

        // create the partner sales invoice by specifying the parties
        input.put("organizationPartyId", organizationPartyId);
        input.put("partnerPartyId", "demopartner1");
        Map<String, Object> results = runAndAssertServiceSuccess("opentaps.createPartnerSalesInvoice", input);
        String invoiceId = (String) results.get("invoiceId");
        assertNotNull("Invoice ID generated for partner sales invoice.", invoiceId);

        // verify that this invoice is a sales invoice from company to partner
        GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        assertEquals("Partner sales invoice is a SALES_INVOICE", invoice.getString("invoiceTypeId"), "SALES_INVOICE");
        assertEquals("Partner sales invoice is from Company", invoice.getString("partyIdFrom"), "Company");
        assertEquals("Partner sales invoice is to demopartner1", invoice.getString("partyId"), "demopartner1");

        // verify the total is 70% of the partner invoice totals (133.33 * 3 * 0.70) = 279.993 -> 279.99
        BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice);
        BigDecimal expectedAmount = asBigDecimal("279.99");
        assertEquals("Sales invoice to partner has expected total value.", invoiceTotal, expectedAmount);

        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("invoiceId", EntityOperator.IN, invoiceIdsInput),
                EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "INVOICE_INV_PTNR"));
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
                                          "Test of partner invoice.  This invoice is between our partner demopartner1 and its customer dp1acct1.");
        invoiceIds.add(invoice);

        invoice = fa.createInvoice("dp1acct2", "PARTNER_INVOICE",
                                   UtilDateTime.nowTimestamp(),
                                   "Test Partner Invoice #2",
                                   "testpartner2",
                                   "Test of partner invoice.  This invoice is between our partner demopartner1 and its customer dp1acct2.");
        invoiceIds.add(invoice);

        invoice = fa.createInvoice("dp1a1contact1", "PARTNER_INVOICE",
                                   UtilDateTime.nowTimestamp(),
                                   "Test Partner Invoice #3",
                                   "testpartner3",
                                   "Test of partner invoice.  This invoice is between our partner demopartner1 and its customer dp1a1contact1.");
        invoiceIds.add(invoice);

        // add a basic product line item amount to each invoice and mark as ready (note quantity is null to see if anything crashes)
        BigDecimal amount = new BigDecimal("100.0");
        for (String invoiceId : invoiceIds) {
            fa.createInvoiceItem(invoiceId, "INV_PROD_ITEM", "GZ-1000", null, amount, "Test Product Item for partner invoice.");
            amount = amount.add(new BigDecimal("33.33"));
        }

        // add positive and negative adjustments that cancel out
        for (String invoiceId : invoiceIds) {
            fa.createInvoiceItem(invoiceId, "ITM_DISCOUNT_ADJ", null, null, new BigDecimal("12.14"), "Test Product Item for partner invoice.");
            fa.createInvoiceItem(invoiceId, "ITM_DISCOUNT_ADJ", null, null, new BigDecimal("-12.14"), "Test Product Item for partner invoice.");
        }

        // set all invoices to ready
        for (String invoiceId : invoiceIds) {
            fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");
        }

        return invoiceIds;
    }
}
