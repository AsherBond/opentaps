/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;

/**
 * Unit tests for extracting sales tax data.
 */
public class SalesTaxTests extends FinancialsTestCase {

    /**
     * These tests verify that created (and canceled) invoice doesn't affect sales tax facts
     * because these invoices should not be taken into account calculating taxes.
     * 
     * @throws GeneralException
     */
    public void testTaxTransformationForInProcessAndCanceledInvoice() throws GeneralException {

        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Test customer");
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        Session session = new Infrastructure(dispatcher).getSession();

        BigDecimal totalSalesNV = null;
        BigDecimal totalSalesOR = null;
        BigDecimal taxableNV = null;
        BigDecimal taxableOR = null;
        BigDecimal taxNV = null;
        BigDecimal taxOR = null;

        //1. run sales-tax transformation
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        // find tax authority dimensions
        // find tax authority
        Long taxNVAuthDimId = -1L; //nonexistent key
        GenericValue taxAuthDim = EntityUtil.getFirst(delegator.findByAnd("TaxAuthorityDim", UtilMisc.toMap("taxAuthPartyId", "NV_TAXMAN", "taxAuthGeoId", "NV")));
        if (taxAuthDim != null) {
            taxNVAuthDimId = taxAuthDim.getLong("taxAuthorityDimId");
        }
        Long taxORAuthDimId = -1L; //nonexistent key
        taxAuthDim = EntityUtil.getFirst(delegator.findByAnd("TaxAuthorityDim", UtilMisc.toMap("taxAuthPartyId", "OR_TAXMAN", "taxAuthGeoId", "OR")));
        if (taxAuthDim != null) {
            taxORAuthDimId = taxAuthDim.getLong("taxAuthorityDimId");
        }

        // prepare query that help us to collect summary amounts
        Query taxAuthQry =
            session.createQuery("select sum(grossAmount), sum(taxable), sum(taxDue) from TaxInvoiceItemFact where taxAuthorityDimId = :taxAuthDimId");

        // keep initial values
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        Object[] results = (Object[]) taxAuthQry.list().get(0);
        totalSalesNV = (BigDecimal) results[0];
        taxableNV = (BigDecimal) results[1];
        taxNV = (BigDecimal) results[2];

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        totalSalesOR = (BigDecimal) results[0];
        taxableOR = (BigDecimal) results[1];
        taxOR = (BigDecimal) results[2];

        //2. create a sales invoice with
        String invoiceId = fa.createInvoice(customerPartyId, "SALES_INVOICE");
        //3. invoice item 1 for $100
        String firstItemSeqId = fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1000", BigDecimal.ONE, BigDecimal.valueOf(100.0));
        // sales tax invoice item for $5, tax authority geo = NV, tax auth party = NV_TAXMAN, parent invoice item = invoice item 1
        fa.createTaxInvoiceItem(invoiceId, firstItemSeqId, "NV_TAXMAN", "NV", BigDecimal.valueOf(5.0));

        //4. invoice item 2 for $100
        String secondItemSeqId = fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "GZ-1001", BigDecimal.valueOf(2.0), BigDecimal.valueOf(100.0));
        // sales tax invoice item for $8, tax authority geo = OR, tax auth party = OR_TAXMAN, parent invoice item = invoice item 2
        fa.createTaxInvoiceItem(invoiceId, secondItemSeqId, "OR_TAXMAN", "OR", BigDecimal.valueOf(8.0));

        //5. run sales tax transformation a second time
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        //6. verify that  total sales, taxable sales for NV and OR, and sales tax for NV and OR are unchanged
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect total sales", totalSalesNV, results[0]);
        assertEquals("Incorrect taxable amount", taxableNV, results[1]);
        assertEquals("Incorrect sales tax amount", taxNV, results[2]);

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect total sales", totalSalesOR, results[0]);
        assertEquals("Incorrect taxable amount", taxableOR, results[1]);
        assertEquals("Incorrect sales tax amount", taxOR, results[2]);

        //7. cancel sales invoice
        fa.updateInvoiceStatus(invoiceId, "INVOICE_CANCELLED");

        //8. run sales tax transformation  a third time
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        //9. verify that  total sales, taxable sales for NV and OR, and sales tax for NV and OR are unchanged from second to third run of sales-tax transformation
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect total sales", totalSalesNV, results[0]);
        assertEquals("Incorrect taxable amount", taxableNV, results[1]);
        assertEquals("Incorrect sales tax amount", taxNV, results[2]);

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect total sales", totalSalesOR, results[0]);
        assertEquals("Incorrect taxable amount", taxableOR, results[1]);
        assertEquals("Incorrect sales tax amount", taxOR, results[2]);
    }
}
