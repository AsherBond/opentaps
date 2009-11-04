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
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.domain.base.entities.TaxAuthorityDim;
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

        BigDecimal totalSales = null;
        BigDecimal taxableNV = null;
        BigDecimal taxableOR = null;
        BigDecimal taxNV = null;
        BigDecimal taxOR = null;

        //1. run sales-tax transformation
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        // find tax authority dimensions
        Long taxNVAuthDimId = findTaxAuthDimension(session, "NV", "NV_TAXMAN");
        Long taxORAuthDimId = findTaxAuthDimension(session, "OR", "OR_TAXMAN");

        // prepare query that help us to collect summary amounts
        Query totalSalesQry =
            session.createQuery("select sum(grossAmount) from SalesInvoiceItemFact");
        Query taxAuthQry =
            session.createQuery("select sum(taxable), sum(taxDue) from TaxInvoiceItemFact where taxAuthorityDimId = :taxAuthDimId");

        // keep initial values
        totalSales = (BigDecimal) (totalSalesQry.list().get(0));
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        Object[] results = (Object[]) taxAuthQry.list().get(0);
        taxableNV = (BigDecimal) results[0];
        taxNV = (BigDecimal) results[1];

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        taxableOR = (BigDecimal) results[0];
        taxOR = (BigDecimal) results[1];

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
        assertEquals("Incorrect total sales", totalSales, (BigDecimal) (totalSalesQry.list().get(0)));
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableNV, results[0]);
        assertEquals("Incorrect sales tax amount", taxNV, results[1]);

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableOR, results[0]);
        assertEquals("Incorrect sales tax amount", taxOR, results[1]);

        //7. cancel sales invoice
        fa.updateInvoiceStatus(invoiceId, "INVOICE_CANCELLED");

        //8. run sales tax transformation  a third time
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        //9. verify that  total sales, taxable sales for NV and OR, and sales tax for NV and OR are unchanged from second to third run of sales-tax transformation
        assertEquals("Incorrect total sales", totalSales, (BigDecimal) (totalSalesQry.list().get(0)));
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableNV, results[0]);
        assertEquals("Incorrect sales tax amount", taxNV, results[1]);

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableOR, results[0]);
        assertEquals("Incorrect sales tax amount", taxOR, results[1]);
    }

    /**
     * 
     * @throws GeneralException
     */
    public void testTaxTransformationForReadyConfirmedAndPaidInvoice() throws GeneralException {
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Test customer");
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        Session session = new Infrastructure(dispatcher).getSession();

        BigDecimal totalSales = null;
        BigDecimal taxableNV = null;
        BigDecimal taxableOR = null;
        BigDecimal taxNV = null;
        BigDecimal taxOR = null;

        //1. run sales-tax transformation
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        // find tax authority dimensions
        Long taxNVAuthDimId = findTaxAuthDimension(session, "NV", "NV_TAXMAN");
        Long taxORAuthDimId = findTaxAuthDimension(session, "OR", "OR_TAXMAN");

        // prepare query that help us to collect summary amounts
        Query totalSalesQry =
            session.createQuery("select sum(grossAmount) from SalesInvoiceItemFact");
        Query taxAuthQry =
            session.createQuery("select sum(taxable), sum(taxDue) from TaxInvoiceItemFact where taxAuthorityDimId = :taxAuthDimId");

        // keep initial values
        totalSales = (BigDecimal) (totalSalesQry.list().get(0));
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        Object[] results = (Object[]) taxAuthQry.list().get(0);
        taxableNV = (BigDecimal) results[0];
        taxNV = (BigDecimal) results[1];

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        taxableOR = (BigDecimal) results[0];
        taxOR = (BigDecimal) results[1];

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

        //5. set invoice as READY
        fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        //6. run sales tax transformation a second time
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        //7. verify that:
        // total sales has increased by $200,
        // taxable sales for NV has increased by $100
        // taxable sales for OR has increased by $100
        // sales tax for NV  has increased by $5
        // sales tax for OR has increased by $8

        // calculate new values
        totalSales = totalSales.add(BigDecimal.valueOf(300.0));
        taxableNV = taxableNV.add(BigDecimal.valueOf(100.0));
        taxableOR = taxableOR.add(BigDecimal.valueOf(200.0));
        taxNV = taxNV.add(BigDecimal.valueOf(5.0));
        taxOR = taxOR.add(BigDecimal.valueOf(8.0));

        assertEquals("Incorrect total sales", totalSales, (BigDecimal) (totalSalesQry.list().get(0)));
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableNV, results[0]);
        assertEquals("Incorrect sales tax amount", taxNV, results[1]);

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableOR, results[0]);
        assertEquals("Incorrect sales tax amount", taxOR, results[1]);

        //7. set invoice as paid
        fa.createPaymentAndApplication(BigDecimal.valueOf(313.0), customerPartyId, organizationPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD", null, invoiceId, "PMNT_RECEIVED");

        //8. run sales tax transformation  a third time
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        //9. verify that  total sales, taxable sales for NV and OR, and sales tax for NV and OR are unchanged from second to third run of sales-tax transformation
        assertEquals("Incorrect total sales", totalSales, (BigDecimal) (totalSalesQry.list().get(0)));
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableNV, results[0]);
        assertEquals("Incorrect sales tax amount", taxNV, results[1]);

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableOR, results[0]);
        assertEquals("Incorrect sales tax amount", taxOR, results[1]);
        
    }

    public void testTaxTransformationForVoidedInvoice() throws GeneralException {
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Test customer");
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        Session session = new Infrastructure(dispatcher).getSession();

        BigDecimal totalSales = null;
        BigDecimal taxableNV = null;
        BigDecimal taxableOR = null;
        BigDecimal taxNV = null;
        BigDecimal taxOR = null;

        //1. run sales-tax transformation
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        // find tax authority dimensions
        Long taxNVAuthDimId = findTaxAuthDimension(session, "NV", "NV_TAXMAN");
        Long taxORAuthDimId = findTaxAuthDimension(session, "OR", "OR_TAXMAN");

        // prepare query that help us to collect summary amounts
        Query totalSalesQry =
            session.createQuery("select sum(grossAmount) from SalesInvoiceItemFact");
        Query taxAuthQry =
            session.createQuery("select sum(taxable), sum(taxDue) from TaxInvoiceItemFact where taxAuthorityDimId = :taxAuthDimId");

        // keep initial values
        totalSales = (BigDecimal) (totalSalesQry.list().get(0));
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        Object[] results = (Object[]) taxAuthQry.list().get(0);
        taxableNV = (BigDecimal) results[0];
        taxNV = (BigDecimal) results[1];

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        taxableOR = (BigDecimal) results[0];
        taxOR = (BigDecimal) results[1];

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

        //5. set invoice as ready
        fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        //6. void invoice
        Map<String, Object> ctx = UtilMisc.toMap(
                "userLogin", demofinadmin,
                "invoiceId", invoiceId
        );
        runAndAssertServiceSuccess("opentaps.voidInvoice", ctx);

        //7. run sales tax transformation a second time
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        //8. verify that  total sales, taxable sales for NV and OR, and sales tax for NV and OR are unchanged
        assertEquals("Incorrect total sales", totalSales, (BigDecimal) (totalSalesQry.list().get(0)));
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableNV, results[0]);
        assertEquals("Incorrect sales tax amount", taxNV, results[1]);

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableOR, results[0]);
        assertEquals("Incorrect sales tax amount", taxOR, results[1]);

    }

    public void testTaxTransformationForWrittenOffInvoice() throws GeneralException {

        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Test customer");
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        Session session = new Infrastructure(dispatcher).getSession();

        BigDecimal totalSales = null;
        BigDecimal taxableNV = null;
        BigDecimal taxableOR = null;
        BigDecimal taxNV = null;
        BigDecimal taxOR = null;

        //1. run sales-tax transformation
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        // find tax authority dimensions
        Long taxNVAuthDimId = findTaxAuthDimension(session, "NV", "NV_TAXMAN");
        Long taxORAuthDimId = findTaxAuthDimension(session, "OR", "OR_TAXMAN");

        // prepare query that help us to collect summary amounts
        Query totalSalesQry =
            session.createQuery("select sum(grossAmount) from SalesInvoiceItemFact");
        Query taxAuthQry =
            session.createQuery("select sum(taxable), sum(taxDue) from TaxInvoiceItemFact where taxAuthorityDimId = :taxAuthDimId");

        // keep initial values
        totalSales = (BigDecimal) (totalSalesQry.list().get(0));
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        Object[] results = (Object[]) taxAuthQry.list().get(0);
        taxableNV = (BigDecimal) results[0];
        taxNV = (BigDecimal) results[1];

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        taxableOR = (BigDecimal) results[0];
        taxOR = (BigDecimal) results[1];

        if (taxableNV ==  null) {
            taxableNV = BigDecimal.ZERO;
        }
        if (taxableOR ==  null) {
            taxableOR = BigDecimal.ZERO;
        }
        if (taxNV ==  null) {
            taxNV = BigDecimal.ZERO;
        }
        if (taxOR ==  null) {
            taxOR = BigDecimal.ZERO;
        }

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

        //5. set invoice as ready
        fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        //6. write off invoice
        fa.updateInvoiceStatus(invoiceId, "INVOICE_WRITEOFF");

        //7. run sales tax transformation a second time
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        //8. verify that  total sales, taxable sales for NV and OR, and sales tax for NV and OR are unchanged
        assertEquals("Incorrect total sales", totalSales, (BigDecimal) (totalSalesQry.list().get(0)));
        taxAuthQry.setLong("taxAuthDimId", taxNVAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableNV, results[0]);
        assertEquals("Incorrect sales tax amount", taxNV, results[1]);

        taxAuthQry.setLong("taxAuthDimId", taxORAuthDimId);
        results = (Object[]) taxAuthQry.list().get(0);
        assertEquals("Incorrect taxable amount", taxableOR, results[0]);
        assertEquals("Incorrect sales tax amount", taxOR, results[1]);
    }

    @SuppressWarnings("unchecked")
    public Long findTaxAuthDimension(Session session, String taxAuthGeoId, String taxAuthPartyId) {
        Query q = session.createQuery("from TaxAuthorityDim where taxAuthGeoId = :taxAuthGeoId and taxAuthPartyId = :taxAuthPartyId");
        q.setString("taxAuthGeoId", taxAuthGeoId);
        q.setString("taxAuthPartyId", taxAuthPartyId);

        List<TaxAuthorityDim> resultSet = q.list();
        if (UtilValidate.isNotEmpty(resultSet)) {
            return resultSet.get(0).getTaxAuthorityDimId();
        }

        return null;
    }

}
