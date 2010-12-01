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

package org.opentaps.tests.crmsfa.orders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.opentaps.base.entities.InventoryItem;
import org.opentaps.base.services.CreatePartyPostalAddressService;
import org.opentaps.base.services.CreatePhysicalInventoryAndVarianceService;
import org.opentaps.base.services.TestShipOrderManualService;
import org.opentaps.base.services.UpdateOrderItemShipGroupService;
import org.opentaps.common.domain.order.OrderSpecification;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.tests.financials.FinancialAsserts;

/**
 * Order Sales Tax related unit tests.
 */
public class SalesTaxTests extends OrderTestCase {

    private static final String MODULE = SalesTaxTests.class.getName();

    private GenericValue admin;
    private GenericValue demoCSR;
    private GenericValue ca1;
    private GenericValue ca2;
    private GenericValue demoAccount1;
    private GenericValue Product1;
    private GenericValue Product2;
    private GenericValue Product3;
    private static final String organizationPartyId = "Company";
    private static final String productStoreId = "9000";
    private static final String productId1 = "GZ-1005";
    private static final String productId2 = "WG-5569";
    private static final String productId3 = "WG-1111";
    private static final String facilityId = "WebStoreWarehouse";
    private OrderSpecification specification;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        admin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
        ca1 = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "ca1"));
        ca2 = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "ca2"));
        demoAccount1 = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoAccount1"));
        demoCSR = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "DemoCSR"));
        Product1 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId1));
        Product2 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId2));
        Product3 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId3));
        // set a default User
        User = demoCSR;

        specification = new OrderSpecification();

        assertNotNull("admin not null", admin);
        assertNotNull("ca1 not null", ca1);
        assertNotNull("ca2 not null", ca2);
        assertNotNull("DemoAccount1 not null", demoAccount1);
        assertNotNull("DemoCSR not null", demoCSR);
        assertNotNull("Product1 not null", Product1);
        assertNotNull("Product2 not null", Product2);
        assertNotNull("Product3 not null", Product3);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        admin = null;
        ca1 = null;
        ca2 = null;
        demoAccount1 = null;
        demoCSR = null;
        User = null;
        Product1 = null;
        Product2 = null;
        Product3 = null;
    }

    /**
     * Verify tax applied on an Order.
     * Checks for each order item that there is a tax adjustment with the given taxId and it's amount is correct according to the given percentage.
     *
     * @param orderId the order to check
     * @param taxId the primaryGeoId of the tax adjustments
     * @param percentage the expected tax percentage
     * @throws GeneralException in an error occurs
     */
    @SuppressWarnings("unchecked")
    public void assertTaxOrder(String orderId, String taxId, BigDecimal percentage) throws GeneralException {

        List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
        assertNotNull("There is no order item in the order " + orderId + ".", orderItems);
        List<GenericValue> orderAdjustments = delegator.findByAnd("OrderAdjustment", UtilMisc.toMap("orderId", orderId));
        assertNotNull("There is no order adjustment in the order " + orderId + ".", orderAdjustments);
        for (GenericValue orderItem : orderItems) {
            List<GenericValue> orderItemAdjustments = OrderReadHelper.getOrderItemAdjustmentList(orderItem, orderAdjustments);
            assertNotNull("There is no order item adjustment in the order " + orderId + " for the order item " + orderItem.getString("orderItemSeqId") + ".", orderItemAdjustments);

            GenericValue calatax = EntityUtil.getFirst(EntityUtil.filterByAnd(orderItemAdjustments, UtilMisc.toMap("primaryGeoId", taxId)));
            assertNotNull("There is no " + taxId + " order item adjustment in the order " + orderId + " for the order item " + orderItem.getString("orderItemSeqId") + ".", calatax);

            // expected tax amount, the percent divide has the same scaling as in the tax service
            BigDecimal amount = specification.taxCalculationRounding(orderItem.getBigDecimal("unitPrice").multiply(orderItem.getBigDecimal("quantity")).multiply(percentage).divide(new BigDecimal("100"), 3, ROUNDING));

            assertEquals("The tax calculation for order " + orderId + " and order item " + orderItem.getString("orderItemSeqId") + " is wrong.", calatax.getBigDecimal("amount"), amount);
        }
    }

    /**
     * Verify tax applied on a Return.
     * Checks for each order item that there is a tax adjustment with the given taxId and it's amount is correct according to the given percentage.
     *
     * @param returnId the return to check
     * @param taxId the primaryGeoId of the tax adjustments
     * @param percentage the expected tax percentage
     * @throws GeneralException in an error occurs
     */
    public void assertTaxReturn(String returnId, String taxId, BigDecimal percentage) throws GeneralException {

        List<GenericValue> returnAdjustments = delegator.findByAnd("ReturnAdjustment", UtilMisc.toMap("returnId", returnId, "primaryGeoId", taxId));
        assertNotNull("There is no return adjustment in the return " + returnId + " for tax " + taxId + ".", returnAdjustments);
        for (GenericValue returnAdjustment : returnAdjustments) {
            GenericValue returnItem = delegator.findByPrimaryKey("ReturnItem", UtilMisc.toMap("returnId", returnId, "returnItemSeqId", returnAdjustment.getString("returnItemSeqId")));

            BigDecimal amount = returnItem.getBigDecimal("returnPrice").multiply(returnItem.getBigDecimal("returnQuantity")).multiply(percentage).divide(new BigDecimal("100"), 3, ROUNDING);
            assertEquals("The tax calculate for return " + returnId + " and return item " + returnItem.getString("returnItemSeqId") + " is wrong.", returnAdjustment.getBigDecimal("amount"), amount);
        }
    }

    /**
     * Verify summary gross sales, discounts, taxable and tax amount values in TaxInvoiceItemFact entity
     * for an order.
     * 
     * @param orderId a <code>String</code> value
     * @param authPartyId a <code>String</code> value
     * @param authGeoId a <code>String</code> value
     * @param grossSales a <code>double</code> value
     * @param discounts a <code>double</code> value
     * @param refunds a <code>double</code> value
     * @param netAmount a <code>double</code> value
     * @param taxable a <code>double</code> value
     * @param tax a <code>double</code> value
     * @exception GeneralException if an error occurs
     */
    public void assertSalesTaxFact(String orderId, String authPartyId, String authGeoId, double grossSales, double discounts, double refunds, double netAmount, double taxable, double tax) throws GeneralException {

        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, admin);

        // find invoice id from order id and verify values
        List<GenericValue> orderItemBillings = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("orderId", orderId));
        String invoiceId = EntityUtil.getFirst(orderItemBillings).getString("invoiceId");
        fa.assertSalesTaxFact(invoiceId, authPartyId, authGeoId, grossSales, discounts, refunds, netAmount, taxable, tax);
    }

    /**
     * Verify summary gross sales, discounts, refunds and net amount values in SalesInvoiceItemFact entity
     * for an order.
     *
     * @param orderId a <code>String</code> value
     * @param grossSales a <code>double</code> value
     * @param discounts a <code>double</code> value
     * @param refunds a <code>double</code> value
     * @param netAmount a <code>double</code> value
     * @exception GeneralException if an error occurs
     */
    public void assertSalesFact(String orderId, double grossSales, double discounts, double refunds, double netAmount) throws GeneralException {

        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, admin);

        // find invoice id from order id and verify values
        List<GenericValue> orderItemBillings = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("orderId", orderId));
        GenericValue billing = EntityUtil.getFirst(orderItemBillings);
        assertNotNull("No OrderItemBilling found for orderId [" + orderId + "]", billing);
        String invoiceId = billing.getString("invoiceId");
        fa.assertSalesFact(invoiceId, grossSales, discounts, refunds, netAmount);
    }

    /**
     * State and County Sales Tax Tests: these tests will verify state and county taxes
     * The particular product, payment, shipping methods don't really matter as much for this test.
     *
     * 0.  Copy ca1 / ca2
     * 1.  Create a sales order for productStoreId=9000, partyId=customer1, address=customer1add1
     * 2.  Verify each order item from #1 has a tax of 6.25% of order item for taxAuthGeoId=CA and 1% of order item for taxAuthGeoId=CA-LA
     * 3.  Create a sales order for productStoreId=9000, partyId=customer1, address=customer1add2
     * 4.  Verify each order item from #3 has a tax of 6.25% of order item for taxAuthGeoId=CA and 0.125% of order item for taxAuthGeoId=CA-SOLANO
     * 5.  Create a sales order for productStoreId=9000, partyId=customer2, address=customer2add1
     * 6.  Verify each order item from #5 has a tax of 0 for taxAuthGeoId=CA and 0 for taxAuthGeoId=CA-LA
     * 7.  Create a sales order for productStoreId=9000, partyId=customer2, address=customer2add2
     * 8.  Verify each order item from #7 has a tax of 0 for taxAuthGeoId=CA and 0 for taxAuthGeoId=CA-SOLANO
     * 9.  Create a sales order for productStoreId=9000, partyId=customer1, address=customer1add1
     * 10.  Use testShipOrder to ship all 5 sales orders (#1, #3, #5, #7, #9)
     * 11.  Create a return for all items on sales order #9 and accept the return
     * 12.  Verify that the return item from #11 has a ReturnAdjustment of sales tax of 6.25% of item for taxAuthGeoId=CA and 1% for taxAuthGeoId=CA-LA
     * 13.  Perform ETL transformations for sales tax report and verify results
     * @throws Exception in an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCATaxApplication() throws Exception {

        // Copy user ca1
        String customerPartyId1 = createPartyFromTemplate(ca1.getString("partyId"), "Copy of ca1 for testCATaxApplication");
        GenericValue customer1 = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId1));

        // get the postal addresses
        GenericValue customer1add1 = EntityUtil.getOnly(delegator.findByAnd("PartyAndPostalAddress", UtilMisc.toMap("partyId", customerPartyId1, "postalCode", "90049")));
        GenericValue customer1add2 = EntityUtil.getOnly(delegator.findByAnd("PartyAndPostalAddress", UtilMisc.toMap("partyId", customerPartyId1, "postalCode", "94590")));
        assertNotNull("Could not find postal address for copy of ca1 with postal code 90049", customer1add1);
        assertNotNull("Could not find postal address for copy of ca1 with postal code 94590", customer1add2);

        // Copy user ca2
        String customerPartyId2 = createPartyFromTemplate(ca2.getString("partyId"), "Copy of ca2 for testCATaxApplication");
        GenericValue customer2 = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId2));

        // get the postal addresses
        GenericValue customer2add1 = EntityUtil.getOnly(delegator.findByAnd("PartyAndPostalAddress", UtilMisc.toMap("partyId", customerPartyId2, "postalCode", "90049")));
        GenericValue customer2add2 = EntityUtil.getOnly(delegator.findByAnd("PartyAndPostalAddress", UtilMisc.toMap("partyId", customerPartyId2, "postalCode", "94590")));
        assertNotNull("Could not find postal address for copy of ca2 with postal code 90049", customer2add1);
        assertNotNull("Could not find postal address for copy of ca2 with postal code 94590", customer2add2);

        /*
         * 1.  Create a sales order for productStoreId=9000, partyId=customer1, address=customer1add1
         */
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(Product1, new BigDecimal("1.0"));
        order.put(Product2, new BigDecimal("4.0"));
        User = demoCSR;
        SalesOrderFactory salesOrder1 = testCreatesSalesOrder(order, customer1, productStoreId, "EXT_OFFLINE", customer1add1.getString("contactMechId"));

        /*
         * 2.  Verify each order item from #1 has a tax of 6.25% of order total for taxAuthGeoId=CA and 1% of order item for taxAuthGeoId=CA-LA
         */
        assertTaxOrder(salesOrder1.getOrderId(), "CA", new BigDecimal("6.25"));
        assertTaxOrder(salesOrder1.getOrderId(), "CA-LA", BigDecimal.ONE);

        /*
         * 3.  Create a sales order for productStoreId=9000, partyId=customer1, address=customer1add2
         */
        order = new HashMap<GenericValue, BigDecimal>();
        order.put(Product1, new BigDecimal("1.0"));
        order.put(Product2, new BigDecimal("4.0"));
        User = demoCSR;
        SalesOrderFactory salesOrder3 = testCreatesSalesOrder(order, customer1, productStoreId, "EXT_OFFLINE", customer1add2.getString("contactMechId"));

        /*
         * 4.  Verify each order item from #3 has a tax of 6.25% of order total for taxAuthGeoId=CA and 0.125% of order item for taxAuthGeoId=CA-SOLANO
         */
        assertTaxOrder(salesOrder3.getOrderId(), "CA", new BigDecimal("6.25"));
        assertTaxOrder(salesOrder3.getOrderId(), "CA-SOLANO", new BigDecimal("0.125"));

        /*
         * 5.  Create a sales order for productStoreId=9000, partyId=customer2, address=customer2add1
         */
        order = new HashMap<GenericValue, BigDecimal>();
        order.put(Product1, new BigDecimal("1.0"));
        order.put(Product2, new BigDecimal("4.0"));
        User = demoCSR;
        SalesOrderFactory salesOrder5 = testCreatesSalesOrder(order, customer2, productStoreId, "EXT_OFFLINE", customer2add1.getString("contactMechId"));

        /*
         * 6.  Verify each order item from #5 has a tax of 0 for taxAuthGeoId=CA and 0 for taxAuthGeoId=CA-LA
         */
        assertTaxOrder(salesOrder5.getOrderId(), "CA", BigDecimal.ZERO);
        assertTaxOrder(salesOrder5.getOrderId(), "CA-LA", BigDecimal.ZERO);

        /*
         * 7.  Create a sales order for productStoreId=9000, partyId=customer2, address=customer2add2
         */
        order = new HashMap<GenericValue, BigDecimal>();
        order.put(Product1, new BigDecimal("1.0"));
        order.put(Product2, new BigDecimal("4.0"));
        User = demoCSR;
        SalesOrderFactory salesOrder7 = testCreatesSalesOrder(order, customer2, productStoreId, "EXT_OFFLINE", customer2add2.getString("contactMechId"));

        /*
         * 8.  Verify each order item from #7 has a tax of 0 for taxAuthGeoId=CA and 0 for taxAuthGeoId=CA-SOLANO
         */
        assertTaxOrder(salesOrder7.getOrderId(), "CA", BigDecimal.ZERO);
        assertTaxOrder(salesOrder7.getOrderId(), "CA-SOLANO", BigDecimal.ZERO);

        /*
         * 9.  Create a sales order for productStoreId=9000, partyId=customer1, address=customer1add1
         */
        order = new HashMap<GenericValue, BigDecimal>();
        order.put(Product1, new BigDecimal("1.0"));
        order.put(Product2, new BigDecimal("4.0"));
        User = demoCSR;
        SalesOrderFactory salesOrder9 = testCreatesSalesOrder(order, customer1, productStoreId, "EXT_OFFLINE", customer1add1.getString("contactMechId"));

        /*
         * 10.  Use testShipOrder to ship all 5 sales orders (#1, #3, #5, #7, #9)
         */
        Map input = UtilMisc.toMap("userLogin", admin);
        input.put("facilityId", facilityId);
        input.put("orderId", salesOrder1.getOrderId());
        runAndAssertServiceSuccess("testShipOrder", input);
        input = UtilMisc.toMap("userLogin", admin);
        input.put("facilityId", facilityId);
        input.put("orderId", salesOrder3.getOrderId());
        runAndAssertServiceSuccess("testShipOrder", input);
        input = UtilMisc.toMap("userLogin", admin);
        input.put("facilityId", facilityId);
        input.put("orderId", salesOrder5.getOrderId());
        runAndAssertServiceSuccess("testShipOrder", input);
        input = UtilMisc.toMap("userLogin", admin);
        input.put("facilityId", facilityId);
        input.put("orderId", salesOrder7.getOrderId());
        runAndAssertServiceSuccess("testShipOrder", input);
        input = UtilMisc.toMap("userLogin", admin);
        input.put("facilityId", facilityId);
        input.put("orderId", salesOrder9.getOrderId());
        runAndAssertServiceSuccess("testShipOrder", input);

        /*
         * 11.  Create a return for all items on sales order #9 and accept the return
         */
        input = UtilMisc.toMap("userLogin", admin);
        input.put("orderId", salesOrder9.getOrderId());
        Map<String, Object> output = runAndAssertServiceSuccess("crmsfa.createReturnFromOrder", input);
        String returnId = (String) output.get("returnId");

        List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", salesOrder9.getOrderId()));
        assertNotNull("There is no order item in the order " + salesOrder9.getOrderId() + ".", orderItems);
        List<GenericValue> returnItemTypes = delegator.findByAnd("ReturnItemTypeMap", UtilMisc.toMap("returnHeaderTypeId", "CUSTOMER_RETURN"));
        Map returnItemTypeMap = new HashMap<String, String>();
        for (GenericValue returnItemType : returnItemTypes) {
            returnItemTypeMap.put(returnItemType.getString("returnItemMapKey"), returnItemType.getString("returnItemTypeId"));
        }

        for (GenericValue orderItem : orderItems) {
            input = UtilMisc.toMap("userLogin", admin);
            input.put("returnId", returnId);
            input.put("returnItemTypeId", (returnItemTypeMap.get(orderItem.getString("orderItemTypeId")) != null ? returnItemTypeMap.get(orderItem.getString("orderItemTypeId")) : ""));
            input.put("orderId", salesOrder9.getOrderId());
            input.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
            input.put("description", orderItem.getString("itemDescription"));
            input.put("productId", orderItem.getString("productId"));
            input.put("returnQuantity", orderItem.getBigDecimal("quantity"));
            input.put("returnPrice", orderItem.getBigDecimal("unitPrice"));
            input.put("returnReasonId", "RTN_NOT_WANT");
            input.put("returnTypeId", "RTN_REFUND");
            runAndAssertServiceSuccess("createReturnItemOrAdjustment", input);
        }

        input = UtilMisc.toMap("userLogin", admin);
        input.put("returnId", returnId);
        runAndAssertServiceSuccess("crmsfa.acceptReturn", input);

        /*
         * 12.  Verify that the return item from #11 has a ReturnAdjustment of sales tax of 6.25% of item for taxAuthGeoId=CA and 1% for taxAuthGeoId=CA-LA
         */
        assertTaxReturn(returnId, "CA", new BigDecimal("6.25"));
        assertTaxReturn(returnId, "CA-LA", new BigDecimal("1.0"));

        /*
         * 13. Perform ETL transformations for sales tax report and verify results
         */
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        assertSalesFact(salesOrder1.getOrderId(), 2991.99, 0.0, 0.0, 2991.99);
        assertSalesTaxFact(salesOrder1.getOrderId(), "_NA_", "_NA_", 2991.99, 0.0, 0.0, 2991.99, 2991.99, 29.92);
        assertSalesTaxFact(salesOrder1.getOrderId(), "CA_BOE", "CA-LA", 2991.99, 0.0, 0.0, 2991.99, 2991.99, 29.92);
        assertSalesTaxFact(salesOrder1.getOrderId(), "CA_BOE", "CA", 2991.99, 0.0, 0.0, 2991.99, 2991.99, 187.00);

        assertSalesFact(salesOrder3.getOrderId(), 2991.99, 0.0, 0.0, 2991.99);
        assertSalesTaxFact(salesOrder3.getOrderId(), "_NA_", "_NA_", 2991.99, 0.0, 0.0, 2991.99, 2991.99, 29.92);
        assertSalesTaxFact(salesOrder3.getOrderId(), "CA_BOE", "CA-SOLANO", 2991.99, 0.0, 0.0, 2991.99, 2991.99, 3.74);
        assertSalesTaxFact(salesOrder3.getOrderId(), "CA_BOE", "CA", 2991.99, 0.0, 0.0, 2991.99, 2991.99, 187.00);

        assertSalesFact(salesOrder5.getOrderId(), 2991.99, 0.0, 0.0, 2991.99);
        assertSalesTaxFact(salesOrder5.getOrderId(), "_NA_", "_NA_", 2991.99, 0.0, 0.0, 2991.99, 2991.99, 29.92);

        assertSalesFact(salesOrder7.getOrderId(), 2991.99, 0.0, 0.0, 2991.99);
        assertSalesTaxFact(salesOrder7.getOrderId(), "_NA_", "_NA_", 2991.99, 0.0, 0.0, 2991.99, 2991.99, 29.92);

        assertSalesFact(salesOrder9.getOrderId(), 2991.99, 0.0, 2991.99, 0.0);
        assertSalesTaxFact(salesOrder9.getOrderId(), "_NA_", "_NA_", 2991.99, 0.0, 2991.99, 0.0, 0.0, 0.0);
        assertSalesTaxFact(salesOrder9.getOrderId(), "CA_BOE", "CA-LA", 2991.99, 0.0, 2991.99, 0.0, 0.0, 0.0);
        assertSalesTaxFact(salesOrder9.getOrderId(), "CA_BOE", "CA", 2991.99, 0.0, 2991.99, 0.0, 0.0, 0.0);
    }

    /**
     * Tests that the sales taxes are correct after updating and canceling order items.
     * Remainder on tax calculation:
     *  - tax authority rate when applied to an item sub total rounds HALF_UP to 3 decimals, this is according to the order specifications
     *  - item taxes for each tax authority are rounded HALF_UP to 3 decimals, this is according to the order specifications
     *  - item tax totals are rounded HALF_UP to 2 decimals, this is according to the order specifications
     * @throws GeneralException if an error occurs
     */
    public void testTaxesAfterOrderUpdate() throws GeneralException {

        // Copy user ca1
        String customerPartyId1 = createPartyFromTemplate(ca1.getString("partyId"), "Copy of ca1 for testTaxesAfterOrderUpdate");
        GenericValue customer1 = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId1));

        // get the postal addresses
        GenericValue customer1add1 = EntityUtil.getOnly(delegator.findByAnd("PartyAndPostalAddress", UtilMisc.toMap("partyId", customerPartyId1, "postalCodeGeoId", "USA-90049")));

        // Create two test products
        GenericValue testProduct1 = createTestProduct("testTaxesAfterOrderUpdate Product 1", admin);
        String productId1 = testProduct1.getString("productId");
        assignDefaultPrice(testProduct1, new BigDecimal("10.0"), admin);

        GenericValue testProduct2 = createTestProduct("testTaxesAfterOrderUpdate Product 2", admin);
        String productId2 = testProduct2.getString("productId");
        assignDefaultPrice(testProduct2, new BigDecimal("75.0"), admin);

        // create an order of 5 product1 and 3 product2
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(testProduct1, new BigDecimal("5.0"));
        orderItems.put(testProduct2, new BigDecimal("3.0"));
        User = demoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, customer1, productStoreId, "EXT_OFFLINE", customer1add1.getString("contactMechId"));
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testTaxesAfterOrderUpdate created order [" + orderId + "]", MODULE);
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(salesOrder.getOrderId());

        // find the order items
        OrderItem orderItem1 = null;
        OrderItem orderItem2 = null;
        for (OrderItem item : order.getOrderItems()) {
            if (productId1.equals(item.getProductId())) {
                orderItem1 = item;
            } else if (productId2.equals(item.getProductId())) {
                orderItem2 = item;
            }
        }
        assertNotNull("Did not find order item 1 in order [" + orderId + "]", orderItem1);
        assertNotNull("Did not find order item 2 in order [" + orderId + "]", orderItem2);

        // update order with same values, this is a hack to have all the promotions, taxes, shipping charges calculated
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", User);
        callCtxt.put("orderId", orderId);
        callCtxt.put("itemQtyMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId() + ":00001", "5.0", orderItem2.getOrderItemSeqId() + ":00001", "3.0"));
        callCtxt.put("itemPriceMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), "10.00", orderItem2.getOrderItemSeqId(), "75.00"));
        callCtxt.put("overridePriceMap", new HashMap<String, Object>());
        runAndAssertServiceSuccess("opentaps.updateOrderItems", callCtxt);

        // verify the details
        // Note: _NA_ taxes only applies to items with unit price > 25.00, so it only applies to order item 2.
        Debug.logInfo("testTaxesAfterOrderUpdate: step 1 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  5 x product1 = 50.0
        //  3 x product2 = 225.0
        //  = 275.00
        assertEquals("Order [" + orderId + "] item 1 sub total incorrect.", orderItem1.getSubTotal(), new BigDecimal("50.00"));
        assertEquals("Order [" + orderId + "] item 2 sub total incorrect.", orderItem2.getSubTotal(), new BigDecimal("225.00"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("275.00"));
        // Shipping amount for the STANDARD _NA_ shipping method is 20% of the order
        //  20 % of 275.00 = 55.00
        assertEquals("Order [" + orderId + "] shipping amount incorrect.", order.getShippingAmount(), new BigDecimal("55.00"));
        // 10% off promotion: 275.00 x 0.1 = 27.50
        assertEquals("Order [" + orderId + "] 10 % off promotion incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-27.50"));
        // Tax amount should be:
        //      1 + 6.25 % of 50.00  = 0.500 + 3.125 = 3.625 ~ 3.63
        //  1 + 1 + 6.25 % of 225.00 = 2.250 + 2.250 + 14.063 = 18.563 ~ 18.56
        //  1 % of 55.00 = 0.55 (note: only the CA-LA CA_BOE tax authority taxes shipping)
        //  1 % of -27.50 promotion = -0.275 ~ -0.27 (note: only the CA-LA CA_BOE tax authority taxes promotion)
        // = 22.4675 ~ 22.47
        assertEquals("Order [" + orderId + "] item 1 taxes incorrect.", orderItem1.getTaxAmount(), new BigDecimal("3.63"));
        assertEquals("Order [" + orderId + "] item 2 taxes incorrect.", orderItem2.getTaxAmount(), new BigDecimal("18.56"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("22.47"));

        // other item adjustments
        assertEquals("Order [" + orderId + "] item 1 other adjustments incorrect.", orderItem1.getOtherAdjustmentsAmount(), new BigDecimal("0.00"));
        assertEquals("Order [" + orderId + "] item 2 other adjustments incorrect.", orderItem2.getOtherAdjustmentsAmount(), new BigDecimal("0.00"));

        // update order:
        // 7 x product1 and 9 product2
        // change product1 unit price to 25.99 so that _NA_ taxes should now apply
        // set product2 price to 69.99
        callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", User);
        callCtxt.put("orderId", orderId);
        callCtxt.put("itemQtyMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId() + ":00001", "7.0", orderItem2.getOrderItemSeqId() + ":00001", "9.0"));
        callCtxt.put("itemPriceMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), "25.99", orderItem2.getOrderItemSeqId(), "69.99"));
        callCtxt.put("overridePriceMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), "25.99", orderItem2.getOrderItemSeqId(), "69.99"));
        runAndAssertServiceSuccess("opentaps.updateOrderItems", callCtxt);

        // reload entities
        order = repository.getOrderById(salesOrder.getOrderId());
        orderItem1 = order.getOrderItem(orderItem1.getOrderItemSeqId());
        orderItem2 = order.getOrderItem(orderItem2.getOrderItemSeqId());

        // verify the details
        Debug.logInfo("testTaxesAfterOrderUpdate: step 2 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  7 x 25.99 = 181.93
        //  9 x 69.99 = 629.91
        //  = 811.84
        assertEquals("Order [" + orderId + "] item 1 sub total incorrect.", orderItem1.getSubTotal(), new BigDecimal("181.93"));
        assertEquals("Order [" + orderId + "] item 2 sub total incorrect.", orderItem2.getSubTotal(), new BigDecimal("629.91"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("811.84"));
        // Shipping amount for the STANDARD _NA_ shipping method is 20% of the order
        //  20 % of 811.84 = 162.368
        assertEquals("Order [" + orderId + "] shipping amount incorrect.", order.getShippingAmount(), new BigDecimal("162.37"));
        // 10% off promotion: 811.84 x 0.1 = 81.18
        assertEquals("Order [" + orderId + "] 10 % off promotion incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-81.18"));
        // Tax amount should be:
        //  1 + 1 + 6.25 % of 181.93 = 1.820 + 1.820 + 11.371 = 15.011 ~ 15.01
        //  1 + 1 + 6.25 % of 629.91 = 6.300 + 6.300 + 39.370 = 51.970 ~ 51.97
        //  1 % of 162.368 = 1.624 ~ 1.62 (note: only the CA-LA CA_BOE tax authority taxes shipping)
        //  1 % of -81.18 promotion = -0.8118 ~ -0.81 (note: only the CA-LA CA_BOE tax authority taxes promotion)
        // = 67.79
        assertEquals("Order [" + orderId + "] item 1 taxes incorrect.", orderItem1.getTaxAmount(), new BigDecimal("15.01"));
        assertEquals("Order [" + orderId + "] item 2 taxes incorrect.", orderItem2.getTaxAmount(), new BigDecimal("51.97"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("67.79"));

        // other item adjustments
        assertEquals("Order [" + orderId + "] item 1 other adjustments incorrect.", orderItem1.getOtherAdjustmentsAmount(), new BigDecimal("0.00"));
        assertEquals("Order [" + orderId + "] item 2 other adjustments incorrect.", orderItem2.getOtherAdjustmentsAmount(), new BigDecimal("0.00"));

        // cancel 8 x product2
        callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", User);
        callCtxt.put("orderId", orderId);
        callCtxt.put("orderItemSeqId", orderItem2.getOrderItemSeqId());
        callCtxt.put("shipGroupSeqId", "00001");
        callCtxt.put("cancelQuantity", new BigDecimal("8.0"));
        runAndAssertServiceSuccess("cancelOrderItem", callCtxt);

        // reload entities
        order = repository.getOrderById(salesOrder.getOrderId());
        orderItem1 = order.getOrderItem(orderItem1.getOrderItemSeqId());
        orderItem2 = order.getOrderItem(orderItem2.getOrderItemSeqId());

        // verify the details
        Debug.logInfo("testTaxesAfterOrderUpdate: step 3 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  7 x 25.99 = 181.93
        //  1 x 69.99 = 69.99
        //  = 251.92
        assertEquals("Order [" + orderId + "] item 1 sub total incorrect.", orderItem1.getSubTotal(), new BigDecimal("181.93"));
        assertEquals("Order [" + orderId + "] item 2 sub total incorrect.", orderItem2.getSubTotal(), new BigDecimal("69.99"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("251.92"));
        // Shipping amount for the STANDARD _NA_ shipping method is 20% of the order
        //  20 % of 251.92 = 50.38
        assertEquals("Order [" + orderId + "] shipping amount incorrect.", order.getShippingAmount(), new BigDecimal("50.38"));
        // 10% off promotion: 251.92 x 0.1 = 25.19
        assertEquals("Order [" + orderId + "] 10 % off promotion incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-25.19"));
        // Tax amount should be:
        //  1 + 1 + 6.25 % of 181.93 = 1.820 + 1.820 + 11.371 = 15.011 ~ 15.01
        //  1 + 1 + 6.25 % of 69.99  = 0.700 + 0.700 + 4.375  = 5.774 ~ 5.77
        //  1 % of 50.38 = 0.504 ~ 0.50 (note: only the CA-LA CA_BOE tax authority taxes shipping)
        //  1 % of -25.19 promotion = -0.2519 ~ -0.25 (note: only the CA-LA CA_BOE tax authority taxes promotion)
        // = 21.03
        assertEquals("Order [" + orderId + "] item 1 taxes incorrect.", orderItem1.getTaxAmount(), new BigDecimal("15.01"));
        assertEquals("Order [" + orderId + "] item 2 taxes incorrect.", orderItem2.getTaxAmount(), new BigDecimal("5.77"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("21.03"));

        // other item adjustments
        assertEquals("Order [" + orderId + "] item 1 other adjustments incorrect.", orderItem1.getOtherAdjustmentsAmount(), new BigDecimal("0.00"));
        assertEquals("Order [" + orderId + "] item 2 other adjustments incorrect.", orderItem2.getOtherAdjustmentsAmount(), new BigDecimal("0.00"));

        // add 10 new order items of another test product
        GenericValue testProduct3 = createTestProduct("testTaxesAfterOrderUpdate Product 3", admin);
        String productId3 = testProduct3.getString("productId");
        assignDefaultPrice(testProduct3, new BigDecimal("15.66"), admin);

        callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("orderId", orderId);
        callCtxt.put("productId", productId3);
        callCtxt.put("quantity", new BigDecimal("10.0"));
        callCtxt.put("shipGroupSeqId", "00001");
        callCtxt.put("prodCatalogId", productStoreId);
        callCtxt.put("recalcOrder", "Y");
        runAndAssertServiceSuccess("opentaps.appendOrderItem", callCtxt);

        // reload entities
        order = repository.getOrderById(salesOrder.getOrderId());
        // find the order items
        orderItem1 = null;
        orderItem2 = null;
        OrderItem orderItem3 = null;
        for (OrderItem item : order.getOrderItems()) {
            if (productId1.equals(item.getProductId())) {
                orderItem1 = item;
            } else if (productId2.equals(item.getProductId())) {
                orderItem2 = item;
            } else if (productId3.equals(item.getProductId())) {
                orderItem3 = item;
            }
        }
        assertNotNull("Did not find order item 1 in order [" + orderId + "]", orderItem1);
        assertNotNull("Did not find order item 2 in order [" + orderId + "]", orderItem2);
        assertNotNull("Did not find order item 3 in order [" + orderId + "]", orderItem3);

        // verify the details
        Debug.logInfo("testTaxesAfterOrderUpdate: step 4 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  7  x 25.99 = 181.93
        //  1  x 69.99 =  69.99
        //  10 x 15.66 = 156.60
        //  = 408.52
        assertEquals("Order [" + orderId + "] item 1 sub total incorrect.", orderItem1.getSubTotal(), new BigDecimal("181.93"));
        assertEquals("Order [" + orderId + "] item 2 sub total incorrect.", orderItem2.getSubTotal(), new BigDecimal("69.99"));
        assertEquals("Order [" + orderId + "] item 3 sub total incorrect.", orderItem3.getSubTotal(), new BigDecimal("156.60"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("408.52"));
        // Shipping amount for the STANDARD _NA_ shipping method is 20% of the order
        //  20 % of 408.52 = 81.70
        assertEquals("Order [" + orderId + "] shipping amount incorrect.", order.getShippingAmount(), new BigDecimal("81.70"));
        // 10% off promotion: 408.52 x 0.1 = 40.852
        assertEquals("Order [" + orderId + "] 10 % off promotion incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-40.85"));
        // Tax amount should be:
        //  1 + 1 + 6.25 % of 181.93 = 1.820 + 1.820 + 11.371 = 15.011 ~ 15.01
        //  1 + 1 + 6.25 % of 69.99  = 0.700 + 0.700 + 4.375  = 5.774 ~ 5.77
        //  1 + 6.25 % of 156.60 = 11.354 ~ 11.35
        //  1 % of 81.70 = 0.82 (note: only the CA-LA CA_BOE tax authority taxes shipping)
        //  1 % of -40.85 promotion = -0.409 ~ -0.41 (note: only the CA-LA CA_BOE tax authority taxes promotion)
        // = 32.54
        assertEquals("Order [" + orderId + "] item 1 taxes incorrect.", orderItem1.getTaxAmount(), new BigDecimal("15.01"));
        assertEquals("Order [" + orderId + "] item 2 taxes incorrect.", orderItem2.getTaxAmount(), new BigDecimal("5.77"));
        assertEquals("Order [" + orderId + "] item 3 taxes incorrect.", orderItem3.getTaxAmount(), new BigDecimal("11.35"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("32.54"));

        // update order with:
        // - product1 unit price below the 25 limit, and quantity to 5
        // - product2 quantity to 0 to cancel them
        // - product3 unit price to above 25, and quantity to 1
        callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", User);
        callCtxt.put("orderId", orderId);
        callCtxt.put("itemQtyMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId() + ":00001", "5.0",
                orderItem2.getOrderItemSeqId() + ":00001", "0.0",
                orderItem3.getOrderItemSeqId() + ":00001", "1.0"));
        callCtxt.put("itemPriceMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), "15.88",
                orderItem2.getOrderItemSeqId(), "25.99",
                orderItem3.getOrderItemSeqId(), "75.77"));
        callCtxt.put("overridePriceMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), "15.88",
                orderItem2.getOrderItemSeqId(), "25.99",
                orderItem3.getOrderItemSeqId(), "75.77"));
        runAndAssertServiceSuccess("opentaps.updateOrderItems", callCtxt);

        // reload entities
        order = repository.getOrderById(salesOrder.getOrderId());
        // find the order items
        orderItem1 = null;
        orderItem2 = null;
        orderItem3 = null;
        for (OrderItem item : order.getOrderItems()) {
            if (productId1.equals(item.getProductId())) {
                orderItem1 = item;
            } else if (productId2.equals(item.getProductId())) {
                orderItem2 = item;
            } else if (productId3.equals(item.getProductId())) {
                orderItem3 = item;
            }
        }
        assertNotNull("Did not find order item 1 in order [" + orderId + "]", orderItem1);
        assertNotNull("Did not find order item 2 in order [" + orderId + "]", orderItem2);
        assertNotNull("Did not find order item 3 in order [" + orderId + "]", orderItem3);

        // verify the details
        Debug.logInfo("testTaxesAfterOrderUpdate: step 5 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  5 x 15.88 = 79.40
        //  1 x 75.77 = 75.77
        //  = 155.17
        assertEquals("Order [" + orderId + "] item 1 sub total incorrect.", orderItem1.getSubTotal(), new BigDecimal("79.40"));
        assertEquals("Order [" + orderId + "] item 2 sub total incorrect.", orderItem2.getSubTotal(), BigDecimal.ZERO);
        assertEquals("Order [" + orderId + "] item 3 sub total incorrect.", orderItem3.getSubTotal(), new BigDecimal("75.77"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("155.17"));
        // Shipping amount for the STANDARD _NA_ shipping method is 20% of the order
        //  20 % of 155.17 = 31.034
        assertEquals("Order [" + orderId + "] shipping amount incorrect.", order.getShippingAmount(), new BigDecimal("31.03"));
        // 10% off promotion: 155.17 x 0.1 = 15.52
        assertEquals("Order [" + orderId + "] 10 % off promotion incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-15.52"));
        // Tax amount should be:
        //  1 + 6.25 % of 79.40 = 0.794 + 4.963 = 5.757 ~ 5.76
        //  1 + 1 + 6.25 % of 75.77 = 0.758 + 0.758 + 4.736 = 6.252 ~ 6.25
        //  1 % of 31.03 = 0.31 (note: only the CA-LA CA_BOE tax authority taxes shipping)
        //  1 % of -15.52 promotion = -0.1552 ~ -0.15 (note: only the CA-LA CA_BOE tax authority taxes promotion)
        // = 12.17
        assertEquals("Order [" + orderId + "] item 1 taxes incorrect.", orderItem1.getTaxAmount(), new BigDecimal("5.76"));
        assertEquals("Order [" + orderId + "] item 2 taxes incorrect.", orderItem2.getTaxAmount(), BigDecimal.ZERO);
        assertEquals("Order [" + orderId + "] item 3 taxes incorrect.", orderItem3.getTaxAmount(), new BigDecimal("6.25"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("12.17"));

    }

    /**
     * Verify that sales tax transformations calculate correct taxable/tax amounts for an order
     * that has both order level promotions and item level promotions.
     * @throws Exception if an error occurs
     */
    public void testSalesTaxAndPromo() throws Exception {

        OrderRepositoryInterface repository = getOrderRepository(admin);

        // force a split inventory situation, make sure there is no inventory item with an atp > 6
        BigDecimal totalAtp = BigDecimal.ZERO;
        BigDecimal refAtp = new BigDecimal("6");
        for (InventoryItem ii : repository.findList(InventoryItem.class, repository.map(InventoryItem.Fields.productId, Product3.getString("productId")))) {
            BigDecimal atp = ii.getAvailableToPromiseTotal();
            if (atp == null) {
                continue;
            }
            if (atp.compareTo(refAtp) > 0) {
                // create a variance
                BigDecimal var = refAtp.subtract(atp);
                CreatePhysicalInventoryAndVarianceService varSer = new CreatePhysicalInventoryAndVarianceService();
                varSer.setInUserLogin(admin);
                varSer.setInInventoryItemId(ii.getInventoryItemId());
                varSer.setInQuantityOnHandVar(var);
                varSer.setInAvailableToPromiseVar(var);
                varSer.setInVarianceReasonId("VAR_DAMAGED");
                varSer.setInComments("test");
                runAndAssertServiceSuccess(varSer);
                totalAtp = totalAtp.add(refAtp);
            } else {
                totalAtp = totalAtp.add(atp);
            }
        }
        // make sure we have at least refAtp
        if (totalAtp.compareTo(refAtp) < 0) {
            // receive some inventory
            receiveInventoryProduct(Product3, refAtp, "NON_SERIAL_INV_ITEM", new BigDecimal("15.0"), facilityId, admin);
            totalAtp = totalAtp.add(refAtp);
        }
        // now make sure we have at least 10
        if (totalAtp.compareTo(new BigDecimal("10")) < 0) {
            // receive the missing inventory
            receiveInventoryProduct(Product3, new BigDecimal("10").subtract(totalAtp), "NON_SERIAL_INV_ITEM", new BigDecimal("14.0"), facilityId, admin);
            totalAtp = totalAtp.add(refAtp);
        }

        Debug.logInfo("Having " + totalAtp.toPlainString() + " x WG-1111", MODULE);
        pause("MySQL timestamp workaround pause", 1000);

        /*
         * 1. Create a sales order for productStoreId=9000, partyId=DemoAccount1, address=DemoAddress1 and 10 WG-1111
         */
        Map<GenericValue, BigDecimal> orderItems = FastMap.<GenericValue, BigDecimal>newInstance();
        orderItems.put(Product3, new BigDecimal("10.0"));
        User = demoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, demoAccount1, productStoreId, "EXT_OFFLINE", "DemoAddress1");

        pause("MySQL timestamp workaround pause", 1000);

        // we have to update order to get all adjustments recalculated
        Order order = repository.getOrderById(salesOrder.getOrderId());

        OrderItem orderItem = null;
        for (OrderItem item : order.getOrderItems()) {
            if (productId3.equals(item.getProductId())) {
                orderItem = item;
            }
        }

        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", User);
        callCtxt.put("orderId", salesOrder.getOrderId());
        callCtxt.put("itemQtyMap", UtilMisc.toMap(orderItem.getOrderItemSeqId() + ":00001", "10.0"));
        callCtxt.put("itemPriceMap", UtilMisc.toMap(orderItem.getOrderItemSeqId(), "59.99"));
        callCtxt.put("overridePriceMap", new HashMap<String, Object>());
        runAndAssertServiceSuccess("opentaps.updateOrderItems", callCtxt);

        pause("MySQL timestamp workaround pause", 1000);

        // ... and approve order because an free product item was added during previous step
        salesOrder.approveOrder();

        pause("MySQL timestamp workaround pause", 1000);

        /*
         * 2. Use testShipOrder to ship sales order
         */
        Map<String, Object> input = UtilMisc.toMap("userLogin", admin, "facilityId", facilityId, "orderId", salesOrder.getOrderId());
        runAndAssertServiceSuccess("testShipOrder", input);

        // reload the order
        order = repository.getOrderById(salesOrder.getOrderId());

        // check the order taxes
        checkSalesTax(order.getOrderId(), "_NA_", 1, new BigDecimal("3.599"));
        checkSalesTax(order.getOrderId(), "CA_BOE", 1, new BigDecimal("22.496"));

        // check the invoice total matches the order total
        List<Invoice> invoices = order.getInvoices();
        assertEquals("Should have exactly one invoice for the order [" + order.getOrderId() + "]", 1, invoices.size());
        Invoice invoice = invoices.get(0);
        Debug.logInfo("Order [" + order.getOrderId() + "] total = " + order.getTotal(), MODULE);
        assertEquals("Invoice [" + invoice.getInvoiceId() + "] total and order [" + order.getOrderId() + "] total did not match", order.getTotal(), invoice.getInvoiceTotal());

        /*
         * 3. Perform ETL transformations for sales tax report
         */
        runAndAssertServiceSuccess("loadSalesTaxData", UtilMisc.toMap("userLogin", admin));

        /*
         * 4. Verify transformation results for the order
         *
         * gross sales: $659.89
         * discounts: $-335.94
         * refunds: $0.0
         * net amount: gross sales - refunds + discounts
         * taxable amount: 599.9 - refunds + discounts
         * tax: $26.097
         */
        assertSalesFact(salesOrder.getOrderId(), 659.89, -335.94, 0.0, 323.95);
        assertSalesTaxFact(salesOrder.getOrderId(), "_NA_", "_NA_", 659.89, -335.94, 0.0, 323.95, 599.90, 3.60);
        assertSalesTaxFact(salesOrder.getOrderId(), "CA_BOE", "CA", 659.89, -335.94, 0.0, 323.95, 599.90, 22.50);

    }

    /**
     * Verify that sales tax is correctly recalculated after changing the shipping address on an existing order.
     * @throws Exception if an error occurs
     */
    public void testSalesTaxAfterAddressChange() throws Exception {
        // Copy user ca1
        String customerPartyId = createPartyFromTemplate(ca1.getString("partyId"), "Copy of ca1 for testSalesTaxAfterAddressChange");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));

        // get the postal addresses in CA
        GenericValue customerAddressCA = EntityUtil.getOnly(delegator.findByAnd("PartyAndPostalAddress", UtilMisc.toMap("partyId", customerPartyId, "postalCode", "90049")));

        // create a postal address in NY
        CreatePartyPostalAddressService createPartyPostalAddress = new CreatePartyPostalAddressService();
        createPartyPostalAddress.setInUserLogin(admin);
        createPartyPostalAddress.setInPartyId(customerPartyId);
        createPartyPostalAddress.setInToName("Shipping Address");
        createPartyPostalAddress.setInAddress1("Test Street");
        createPartyPostalAddress.setInCity("New York");
        createPartyPostalAddress.setInPostalCode("10001");
        createPartyPostalAddress.setInCountryGeoId("USA");
        createPartyPostalAddress.setInStateProvinceGeoId("NY");
        createPartyPostalAddress.setInContactMechPurposeTypeId("SHIPPING_LOCATION");
        runAndAssertServiceSuccess(createPartyPostalAddress);

        String customerAddressNYId = createPartyPostalAddress.getOutContactMechId();
        String customerAddressCAId = customerAddressCA.getString("contactMechId");

        // Create a test products
        GenericValue testProduct1 = createTestProduct("testSalesTaxAfterAddressChange Product 1", admin);
        String productId1 = testProduct1.getString("productId");
        assignDefaultPrice(testProduct1, new BigDecimal("10.0"), admin);

        // create an order of 5 product1
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(testProduct1, new BigDecimal("5.0"));
        User = demoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, customer, productStoreId, "EXT_OFFLINE", customerAddressCAId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testSalesTaxAfterAddressChange created order [" + orderId + "]", MODULE);
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(salesOrder.getOrderId());

        // find the order item
        OrderItem orderItem1 = null;
        for (OrderItem item : order.getOrderItems()) {
            if (productId1.equals(item.getProductId())) {
                orderItem1 = item;
            }
        }
        assertNotNull("Did not find order item 1 in order [" + orderId + "]", orderItem1);

        // update order with same values, this is a hack to have all the promotions, taxes, shipping charges calculated
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", User);
        callCtxt.put("orderId", orderId);
        callCtxt.put("itemQtyMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId() + ":00001", "5.0"));
        callCtxt.put("itemPriceMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), "10.00"));
        callCtxt.put("overridePriceMap", new HashMap<String, Object>());
        runAndAssertServiceSuccess("opentaps.updateOrderItems", callCtxt);

        // verify the details
        // Note: _NA_ taxes only applies to items with unit price > 25.00, so it does not apply here
        Debug.logInfo("testSalesTaxAfterAddressChange: step 1 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  5 x product1 = 50.0
        //  = 50.00
        assertEquals("Order [" + orderId + "] item 1 sub total incorrect.", orderItem1.getSubTotal(), new BigDecimal("50.00"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("50.00"));
        // Shipping amount for the STANDARD _NA_ shipping method is 20% of the order
        //  20 % of 50.00 = 10.00
        assertEquals("Order [" + orderId + "] shipping amount incorrect.", order.getShippingAmount(), new BigDecimal("10.00"));
        // 10% off promotion: 50.00 x 0.1 = 5.00
        assertEquals("Order [" + orderId + "] 10 % off promotion incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-5.00"));
        // Tax amount should be:
        //      1 + 6.25 % of 50.00  = 0.500 + 3.125 = 3.625 ~ 3.63
        //  1 % of 10.00 = 0.10 (note: only the CA-LA CA_BOE tax authority taxes shipping)
        //  1 % of -5.00 promotion = -0.05 (note: only the CA-LA CA_BOE tax authority taxes promotion)
        // = 3.675 ~ 3.56
        assertEquals("Order [" + orderId + "] item 1 taxes incorrect.", orderItem1.getTaxAmount(), new BigDecimal("3.63"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("3.68"));

        // other item adjustments
        assertEquals("Order [" + orderId + "] item 1 other adjustments incorrect.", orderItem1.getOtherAdjustmentsAmount(), new BigDecimal("0.00"));

        // Change the shipping address to the NY one
        UpdateOrderItemShipGroupService ser = new UpdateOrderItemShipGroupService();
        ser.setInUserLogin(admin);
        ser.setInOrderId(orderId);
        ser.setInShipGroupSeqId("00001");
        ser.setInCarrierPartyId("_NA_");
        ser.setInShipmentMethodTypeId("STANDARD");
        ser.setInContactMechId(customerAddressNYId);
        ser.setInContactMechPurposeTypeId("SHIPPING_LOCATION");
        runAndAssertServiceSuccess(ser);

        // reload entities
        order = repository.getOrderById(salesOrder.getOrderId());
        orderItem1 = order.getOrderItem(orderItem1.getOrderItemSeqId());

        // verify the details
        // Note: _NA_ taxes only applies to items with unit price > 25.00, so it does not apply here
        Debug.logInfo("testSalesTaxAfterAddressChange: step 2 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  5 x product1 = 50.0
        //  = 50.00
        assertEquals("Order [" + orderId + "] item 1 sub total incorrect.", orderItem1.getSubTotal(), new BigDecimal("50.00"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("50.00"));
        // Shipping amount for the STANDARD _NA_ shipping method is 20% of the order
        //  20 % of 50.00 = 10.00
        assertEquals("Order [" + orderId + "] shipping amount incorrect.", order.getShippingAmount(), new BigDecimal("10.00"));
        // 10% off promotion: 50.00 x 0.1 = 5.00
        assertEquals("Order [" + orderId + "] 10 % off promotion incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-5.00"));
        // Tax amount should be: (Note NY does not tax shipping or promotions)
        //      4.25 % of 50.00  = 2.125 ~ 2.13
        assertEquals("Order [" + orderId + "] item 1 taxes incorrect.", orderItem1.getTaxAmount(), new BigDecimal("2.13"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("2.13"));

        // now ship 3 items
        Map<String, Map<String, BigDecimal>> itemsToPack = FastMap.newInstance();
        itemsToPack.put("00001", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), new BigDecimal("3.0")));
        TestShipOrderManualService ship = new TestShipOrderManualService();
        ship.setInUserLogin(admin);
        ship.setInOrderId(order.getOrderId());
        ship.setInFacilityId(facilityId);
        ship.setInItems(itemsToPack);
        runAndAssertServiceSuccess(ship);

        // Change the shipping address back to the CA one
        ser = new UpdateOrderItemShipGroupService();
        ser.setInUserLogin(admin);
        ser.setInOrderId(orderId);
        ser.setInShipGroupSeqId("00001");
        ser.setInCarrierPartyId("_NA_");
        ser.setInShipmentMethodTypeId("STANDARD");
        ser.setInContactMechId(customerAddressCAId);
        ser.setInContactMechPurposeTypeId("SHIPPING_LOCATION");
        runAndAssertServiceSuccess(ser);

        // reload entities
        order = repository.getOrderById(salesOrder.getOrderId());
        orderItem1 = order.getOrderItem(orderItem1.getOrderItemSeqId());

        // verify the details
        // Note: _NA_ taxes only applies to items with unit price > 25.00, so it does not apply here
        Debug.logInfo("testSalesTaxAfterAddressChange: step 3 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  5 x product1 = 50.0
        //  = 50.00
        assertEquals("Order [" + orderId + "] item 1 sub total incorrect.", orderItem1.getSubTotal(), new BigDecimal("50.00"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("50.00"));
        // Shipping amount for the STANDARD _NA_ shipping method is 20% of the order
        //  20 % of 50.00 = 10.00
        assertEquals("Order [" + orderId + "] shipping amount incorrect.", order.getShippingAmount(), new BigDecimal("10.00"));
        // 10% off promotion: 50.00 x 0.1 = 5.00
        assertEquals("Order [" + orderId + "] 10 % off promotion incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-5.00"));
        // Tax amount should be:
        //      4.25 % of 30.00  = 1.275 ~ 1.28
        //  1 + 6.25 % of 20.00  = 1.45
        // = 2.725 ~ 2.73
        // Note global shipping / promo taxes are NOT pro rated
        //  1 % of 10.00 = 0.10 (note: only the CA-LA CA_BOE tax authority taxes shipping)
        //  1 % of -5.00 promotion = -0.05 (note: only the CA-LA CA_BOE tax authority taxes promotion)
        // = 2.775 ~ 2.78
        assertEquals("Order [" + orderId + "] item 1 taxes incorrect.", orderItem1.getTaxAmount(), new BigDecimal("2.73"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("2.78"));

        // complete the order
        itemsToPack = FastMap.newInstance();
        itemsToPack.put("00001", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), new BigDecimal("2.0")));
        ship = new TestShipOrderManualService();
        ship.setInUserLogin(admin);
        ship.setInOrderId(order.getOrderId());
        ship.setInFacilityId(facilityId);
        ship.setInItems(itemsToPack);
        runAndAssertServiceSuccess(ship);

        // check there are 2 invoices
        List<Invoice> invoices = order.getInvoices();
        assertEquals("Should have been 2 invoices for order [" + order.getOrderId() + "]", invoices.size(), 2);
        // check the invoices total match the order total
        BigDecimal invoiceTotal = BigDecimal.ZERO;
        for (Invoice i : invoices) {
            invoiceTotal = invoiceTotal.add(i.getInvoiceTotal());
        }
        assertEquals("Invoices [" + Entity.getDistinctFieldValues(String.class, invoices, Invoice.Fields.invoiceId) + "] total and order [" + order.getOrderId() + "] total did not match", order.getTotal(), invoiceTotal);

        // check the order is now completed
        assertOrderCompleted(order.getOrderId());
    }


    /**
     * Verify that sales tax is correctly recalculated after changing the shipping address on an existing order.
     * Same as above but ship to the CA address first (which taxes shipping and promotions)
     * @throws Exception if an error occurs
     */
    public void testSalesTaxAfterAddressChange2() throws Exception {
        // Copy user ca1
        String customerPartyId = createPartyFromTemplate(ca1.getString("partyId"), "Copy of ca1 for testSalesTaxAfterAddressChange");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));

        // get the postal addresses in CA
        GenericValue customerAddressCA = EntityUtil.getOnly(delegator.findByAnd("PartyAndPostalAddress", UtilMisc.toMap("partyId", customerPartyId, "postalCode", "90049")));

        // create a postal address in NY
        CreatePartyPostalAddressService createPartyPostalAddress = new CreatePartyPostalAddressService();
        createPartyPostalAddress.setInUserLogin(admin);
        createPartyPostalAddress.setInPartyId(customerPartyId);
        createPartyPostalAddress.setInToName("Shipping Address");
        createPartyPostalAddress.setInAddress1("Test Street");
        createPartyPostalAddress.setInCity("New York");
        createPartyPostalAddress.setInPostalCode("10001");
        createPartyPostalAddress.setInCountryGeoId("USA");
        createPartyPostalAddress.setInStateProvinceGeoId("NY");
        createPartyPostalAddress.setInContactMechPurposeTypeId("SHIPPING_LOCATION");
        runAndAssertServiceSuccess(createPartyPostalAddress);

        String customerAddressNYId = createPartyPostalAddress.getOutContactMechId();
        String customerAddressCAId = customerAddressCA.getString("contactMechId");

        // Create a test products
        GenericValue testProduct1 = createTestProduct("testSalesTaxAfterAddressChange Product 1", admin);
        String productId1 = testProduct1.getString("productId");
        assignDefaultPrice(testProduct1, new BigDecimal("10.0"), admin);

        // create an order of 5 product1
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(testProduct1, new BigDecimal("5.0"));
        User = demoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, customer, productStoreId, "EXT_OFFLINE", customerAddressCAId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testSalesTaxAfterAddressChange created order [" + orderId + "]", MODULE);
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(salesOrder.getOrderId());

        // find the order item
        OrderItem orderItem1 = null;
        for (OrderItem item : order.getOrderItems()) {
            if (productId1.equals(item.getProductId())) {
                orderItem1 = item;
            }
        }
        assertNotNull("Did not find order item 1 in order [" + orderId + "]", orderItem1);

        // update order with same values, this is a hack to have all the promotions, taxes, shipping charges calculated
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", User);
        callCtxt.put("orderId", orderId);
        callCtxt.put("itemQtyMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId() + ":00001", "5.0"));
        callCtxt.put("itemPriceMap", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), "10.00"));
        callCtxt.put("overridePriceMap", new HashMap<String, Object>());
        runAndAssertServiceSuccess("opentaps.updateOrderItems", callCtxt);

        // verify the details
        // Note: _NA_ taxes only applies to items with unit price > 25.00, so it does not apply here
        Debug.logInfo("testSalesTaxAfterAddressChange: step 1 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  5 x product1 = 50.0
        //  = 50.00
        assertEquals("Order [" + orderId + "] item 1 sub total incorrect.", orderItem1.getSubTotal(), new BigDecimal("50.00"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("50.00"));
        // Shipping amount for the STANDARD _NA_ shipping method is 20% of the order
        //  20 % of 50.00 = 10.00
        assertEquals("Order [" + orderId + "] shipping amount incorrect.", order.getShippingAmount(), new BigDecimal("10.00"));
        // 10% off promotion: 50.00 x 0.1 = 5.00
        assertEquals("Order [" + orderId + "] 10 % off promotion incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-5.00"));
        // Tax amount should be:
        //      1 + 6.25 % of 50.00  = 0.500 + 3.125 = 3.625 ~ 3.63
        //  1 % of 10.00 = 0.10 (note: only the CA-LA CA_BOE tax authority taxes shipping)
        //  1 % of -5.00 promotion = -0.05 (note: only the CA-LA CA_BOE tax authority taxes promotion)
        // = 3.675 ~ 3.56
        assertEquals("Order [" + orderId + "] item 1 taxes incorrect.", orderItem1.getTaxAmount(), new BigDecimal("3.63"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("3.68"));

        // other item adjustments
        assertEquals("Order [" + orderId + "] item 1 other adjustments incorrect.", orderItem1.getOtherAdjustmentsAmount(), new BigDecimal("0.00"));

        // now ship 3 items
        Map<String, Map<String, BigDecimal>> itemsToPack = FastMap.newInstance();
        itemsToPack.put("00001", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), new BigDecimal("3.0")));
        TestShipOrderManualService ship = new TestShipOrderManualService();
        ship.setInUserLogin(admin);
        ship.setInOrderId(order.getOrderId());
        ship.setInFacilityId(facilityId);
        ship.setInItems(itemsToPack);
        runAndAssertServiceSuccess(ship);

        // Change the shipping address to the NY one
        UpdateOrderItemShipGroupService ser = new UpdateOrderItemShipGroupService();
        ser.setInUserLogin(admin);
        ser.setInOrderId(orderId);
        ser.setInShipGroupSeqId("00001");
        ser.setInCarrierPartyId("_NA_");
        ser.setInShipmentMethodTypeId("STANDARD");
        ser.setInContactMechId(customerAddressNYId);
        ser.setInContactMechPurposeTypeId("SHIPPING_LOCATION");
        runAndAssertServiceSuccess(ser);

        // reload entities
        order = repository.getOrderById(salesOrder.getOrderId());
        orderItem1 = order.getOrderItem(orderItem1.getOrderItemSeqId());

        // verify the details
        // Note: _NA_ taxes only applies to items with unit price > 25.00, so it does not apply here
        Debug.logInfo("testSalesTaxAfterAddressChange: step 3 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  5 x product1 = 50.0
        //  = 50.00
        assertEquals("Order [" + orderId + "] item 1 sub total incorrect.", orderItem1.getSubTotal(), new BigDecimal("50.00"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("50.00"));
        // Shipping amount for the STANDARD _NA_ shipping method is 20% of the order
        //  20 % of 50.00 = 10.00
        assertEquals("Order [" + orderId + "] shipping amount incorrect.", order.getShippingAmount(), new BigDecimal("10.00"));
        // 10% off promotion: 50.00 x 0.1 = 5.00
        assertEquals("Order [" + orderId + "] 10 % off promotion incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-5.00"));
        // Tax amount should be:
        //      4.25 % of 20.00  = 0.85
        //  1 + 6.25 % of 30.00  = 2.175 ~ 2.18
        // = 3.025 ~ 3.03
        // Note global shipping / promo taxes are NOT pro rated, so they will be removed here
        // = 3.03
        assertEquals("Order [" + orderId + "] item 1 taxes incorrect.", orderItem1.getTaxAmount(), new BigDecimal("3.03"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("3.03"));

        // complete the order
        itemsToPack = FastMap.newInstance();
        itemsToPack.put("00001", UtilMisc.toMap(orderItem1.getOrderItemSeqId(), new BigDecimal("2.0")));
        ship = new TestShipOrderManualService();
        ship.setInUserLogin(admin);
        ship.setInOrderId(order.getOrderId());
        ship.setInFacilityId(facilityId);
        ship.setInItems(itemsToPack);
        runAndAssertServiceSuccess(ship);

        // check there are 2 invoices
        List<Invoice> invoices = order.getInvoices();
        assertEquals("Should have been 2 invoices for order [" + order.getOrderId() + "]", invoices.size(), 2);
        // check the invoices total match the order total
        BigDecimal invoiceTotal = BigDecimal.ZERO;
        for (Invoice i : invoices) {
            invoiceTotal = invoiceTotal.add(i.getInvoiceTotal());
        }
        assertEquals("Invoices [" + Entity.getDistinctFieldValues(String.class, invoices, Invoice.Fields.invoiceId) + "] total and order [" + order.getOrderId() + "] total did not match", order.getTotal(), invoiceTotal);

        // check the order is now completed
        assertOrderCompleted(order.getOrderId());

    }

}
