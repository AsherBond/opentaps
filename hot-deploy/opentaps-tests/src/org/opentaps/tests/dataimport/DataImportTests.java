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
package org.opentaps.tests.dataimport;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.tests.OpentapsTestCase;
import org.opentaps.tests.crmsfa.orders.OrderTests;
import org.opentaps.tests.financials.AgreementTests;
import org.opentaps.tests.financials.FinancialAsserts;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DataImportTests extends OpentapsTestCase {

    public static final String module = DataImportTests.class.getName();

    GenericValue demofinadmin       = null;
    GenericValue DemoSalesManager   = null;
    GenericValue demowarehouse1     = null;
    GenericValue demopurch1         = null;
    GenericValue product            = null;

    String organizationPartyId          = "Company";
    static final String productStoreId  = "9000";
    static final String productId       = "GZ-1005";


    @Override
    public void setUp() throws Exception {
        super.setUp();
        demofinadmin        = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demofinadmin"));
        DemoSalesManager    = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        demowarehouse1      = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        demopurch1          = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demopurch1"));
        product             = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
        User                = DemoSalesManager;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        demofinadmin        = null;
        DemoSalesManager    = null;
        demowarehouse1      = null;
        demopurch1          = null;
        product             = null;
    }

    /**
     * Performs import of customers, prepares data for rest of the tests.
     */
    public void testImportCustomers() {
        // run import of customers
        runAndAssertServiceSuccess("importCustomers", UtilMisc.toMap("initialResponsiblePartyId", DemoSalesManager.getString("userLoginId"), "organizationPartyId", organizationPartyId, "userLogin", User));
    }

    /**
     * Shortened version of credit limit test. Its purpose is verify if credit limit feature
     * works for some imported customer.
     */
    @SuppressWarnings("unchecked")
    public void testImportedCustomerCreditLimit() throws Exception  {

        AgreementTests agreementUnitTests = new AgreementTests();
        agreementUnitTests.setUp();

        List<GenericValue> candidateParties =  delegator.findByAnd("PartyGroup", UtilMisc.toMap("groupName", "Import Customer for Automated Testing (keep this name unique)"));
        String partyId = null;
        if (UtilValidate.isNotEmpty(candidateParties))
            partyId = EntityUtil.getFirst(candidateParties).getString("partyId");

        agreementUnitTests.performCreditLimitTest(partyId);
    }

    /**
     * The method verifies if sales agreement & net payment days term created and work correctly
     * for imported customer. 
     */
    @SuppressWarnings("unchecked")
    public void testImportedCustomerSalesAgreementNetDaysToParty() throws Exception {

        AgreementTests agreementUnitTests = new AgreementTests();
        agreementUnitTests.setUp();

        List<GenericValue> candidateParties =  delegator.findByAnd("PartyGroup", UtilMisc.toMap("groupName", "Import Customer for Automated Testing (keep this name unique)"));
        String partyId = null;
        if (UtilValidate.isNotEmpty(candidateParties))
            partyId = EntityUtil.getFirst(candidateParties).getString("partyId");

        GenericValue customer = delegator.findByPrimaryKey("DataImportCustomer", UtilMisc.toMap("customerId", "9007"));
        Long netPaymentDays = customer.getLong("netPaymentDays");
        agreementUnitTests.performSalesAgreementNetDaysToPartyTest(partyId, netPaymentDays.intValue());

    }

    /**
     * Test impossibility of shipment for imported customer in don't ship classification
     */
    @SuppressWarnings("unchecked")
    public void testImportedDontShipCustomer() throws Exception {

        OrderTests orderTests = new OrderTests();
        orderTests.setUp();

        // find a customer with disabled shipping
        List<GenericValue> dontshipParties = delegator.findByAnd("PartyClassification", UtilMisc.toMap("partyClassificationGroupId", "DONOTSHIP_CUSTOMERS"));
        List<GenericValue> candidateParties = delegator.findByCondition("PartyGroup", new EntityConditionList(Arrays.asList(new EntityExpr("groupName", EntityOperator.EQUALS, " "), new EntityExpr("partyId", EntityOperator.IN, EntityUtil.getFieldListFromEntityList(dontshipParties, "partyId", true))), EntityOperator.AND), null, null);
        GenericValue customer = null;
        if (UtilValidate.isNotEmpty(candidateParties))
            customer = EntityUtil.getFirst(candidateParties);

        // run test
        orderTests.performDoNotShipTest(customer.getString("partyId"));

    }

    /***
     * Import Commissions Tests
     * 1.  Run importCustomers
     * 2.  Run importCustomerCommissions
     * 3.  For each primaryPartyId of customerIds 9005, 9006, 9007
     * 3a.  Create sales invoice and set it ready
     * 3b.  Verify that a sales commission invoice from primaryPartyId of customerId 9010 to Company is created for 10% of the value of sales invoice in 3a
     */
    public void testImportCustomersCommissions() throws Exception {
        // 1.  Run importCustomers, in case they have not been
        runAndAssertServiceSuccess("importCustomers", UtilMisc.toMap("initialResponsiblePartyId", DemoSalesManager.getString("userLoginId"), "organizationPartyId", organizationPartyId, "userLogin", User));

        // 2.  Run importCustomerCommissions
        // Importing services should check the importedRecords, because they always return success
        // here, we just check that non-zero records were imported, so we don't have to keep modifying this test as the data changes
        Map results = dispatcher.runSync("importCustomersCommissions", UtilMisc.toMap("userLogin", demofinadmin, "organizationPartyId", organizationPartyId));
        assertTrue("importCustomersCommissions did not run successfully", ServiceUtil.isSuccess(results));
        assertNotEquals("importCustomersCommissions did not import any records", new BigDecimal((Integer) results.get("importedRecords")), new BigDecimal("0"));
         
        // 3.  For each primaryPartyId of customerIds 9005, 9006, 9007
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String[] customerIds = { "9005", "9006", "9007" };
        GenericValue commissionBroker = delegator.findByPrimaryKey("DataImportCustomer", UtilMisc.toMap("customerId", "9010"));

        for (String customerId : customerIds) {
            GenericValue dataImportCustomer = delegator.findByPrimaryKey("DataImportCustomer", UtilMisc.toMap("customerId", customerId));
            // 3a.  Create sales invoice and set it oready
            String invoiceId = fa.createInvoice(dataImportCustomer.getString("primaryPartyId"), "SALES_INVOICE", UtilDateTime.nowTimestamp());
            fa.createInvoiceItem(invoiceId, "INV_FPROD_ITEM", "WG-1111", 1.0, 100.0);
            fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");

            // 3b.  Verify that a sales commission invoice from primaryPartyId of customerId 9010 to Company is created for 10% of the value of sales invoice in 3a
            List<GenericValue> commissionInvoiceItems = delegator.findByAnd("InvoiceItem", UtilMisc.toMap("parentInvoiceId", invoiceId));
            assertNotNull("There is no commission invoice item for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "]", commissionInvoiceItems);
            assertEquals("Incorrect number of commission invoice items for party [" + commissionBroker.getString("primaryPartyId") + "] and parent invoice [" + invoiceId + "] of customer party[" + 
            		dataImportCustomer.getString("primaryPartyId") + "] imported from DataImportCustomer customerId [" + customerId + "]", 1, commissionInvoiceItems.size());
            GenericValue commissionInvoiceItem = EntityUtil.getFirst(commissionInvoiceItems);
            assertEquals("Commission invoice item [" + commissionInvoiceItem.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] is not on the right product", "WG-1111", commissionInvoiceItem.getString("productId"));
            assertEquals("Commission invoice item [" + commissionInvoiceItem.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] has not the right quantity", 1.0, commissionInvoiceItem.getDouble("quantity"));
            assertEquals("Commission invoice item [" + commissionInvoiceItem.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] has not on the amount", 10.0, commissionInvoiceItem.getDouble("amount"));

            GenericValue commissionInvoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", commissionInvoiceItem.getString("invoiceId")));
            assertEquals("Commission invoice [" + commissionInvoice.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] is not an commission invoice", "COMMISSION_INVOICE", commissionInvoice.getString("invoiceTypeId"));
            assertEquals("Commission invoice [" + commissionInvoice.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] has not the right role", "COMMISSION_AGENT", commissionInvoice.getString("roleTypeId"));
            assertEquals("Commission invoice [" + commissionInvoice.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] is not for the right party", organizationPartyId, commissionInvoice.getString("partyId"));
            assertEquals("Commission invoice [" + commissionInvoice.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] has not the right party from", commissionBroker.getString("primaryPartyId"), commissionInvoice.getString("partyIdFrom"));
            
        }

    }

}
