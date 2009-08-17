/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
import java.util.Map;

import com.opensourcestrategies.financials.accounts.BillingAccountWorker;
import javolution.util.FastMap;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;

/**
 * Test cases for billing and credit accounts.
 */
public class BillingAccountTests extends FinancialsTestCase  {

    public final String organizationPartyId = "Company";

    /**
     * Creates a billing account from a credit memo and
     * pays a sales invoice with it.  Verifies balances
     * are correct at each logical stage.
     */
    public void testCreditMemoBillingAccountPayInvoice() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String customerPartyId = "DemoAccount1";

        // create a credit memo for $2000
        String creditInvoiceId = fa.createInvoice(customerPartyId, "CUST_RTN_INVOICE", UtilDateTime.nowTimestamp(), "Test Billing Account", "testCreditMemoBillingAccountPayInvoice", "Test Billing Account");
        fa.createInvoiceItem(creditInvoiceId, "CRT_ADD_FEATURE_ADJ", new BigDecimal("1.0"), new BigDecimal("2000.0"));
        fa.updateInvoiceStatus(creditInvoiceId, "INVOICE_READY");

        // convert to billing account
        Map results = runAndAssertServiceSuccess("convertToBillingAccount", UtilMisc.toMap("invoiceId", creditInvoiceId, "userLogin", demofinadmin));
        String billingAccountId = (String) results.get("billingAccountId");

        // ensure that the billing account balance is $2000
        GenericValue billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));
        BigDecimal balance = BillingAccountWorker.getBillingAccountAvailableBalance(billingAccount);
        assertEquals("net balance of billing account is $2000", balance, new BigDecimal("2000"));

        // create a sales invoice for $1000
        String invoiceId = fa.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), "Test Billing Account", "testCreditMemoBillingAccountPayInvoice", "Test Billing Account");
        fa.createInvoiceItem(invoiceId, "INV_PROD_ITEM", "GZ-8544", new BigDecimal("1.0"), new BigDecimal("1000.0"));
        fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        // pay the invoice with our billing account
        Map<String, Object> input = new FastMap<String, Object>();
        input.put("billingAccountId", billingAccountId);
        input.put("invoiceId", invoiceId);
        input.put("captureAmount", new Double(1000.0));
        input.put("userLogin", demofinadmin);
        results = runAndAssertServiceSuccess("captureBillingAccountPayment", input);
        // String paymentId = (String) results.get("paymentId");

        // ensure the sales invoice is paid
        fa.assertInvoiceStatus(invoiceId, "INVOICE_PAID");

        // ensure the billing account balance is reduced by $1000
        balance = BillingAccountWorker.getBillingAccountAvailableBalance(billingAccount);
        assertEquals("net balance of billing account is $1000", balance, new BigDecimal("1000"));
    }

}
