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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opensourcestrategies.financials.accounts.AccountsHelper;
import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.security.SecurityFactory;
import org.opentaps.base.constants.OrderAdjustmentTypeConstants;
import org.opentaps.base.entities.InvoiceItem;
import org.opentaps.base.services.CancelOrderItemInvResQtyService;
import org.opentaps.base.services.CancelOrderItemService;
import org.opentaps.base.services.ChangeOrderItemStatusService;
import org.opentaps.base.services.CompleteInventoryTransferService;
import org.opentaps.base.services.CreateInventoryTransferService;
import org.opentaps.base.services.CreateOrderAdjustmentService;
import org.opentaps.base.services.CreatePartyPostalAddressService;
import org.opentaps.base.services.CreatePaymentFromPreferenceService;
import org.opentaps.base.services.CreatePhysicalInventoryAndVarianceService;
import org.opentaps.base.services.CreatePicklistFromOrdersService;
import org.opentaps.base.services.FindOrdersToPickMoveService;
import org.opentaps.base.services.GetInventoryAvailableByFacilityService;
import org.opentaps.base.services.GetProductService;
import org.opentaps.base.services.OpentapsAppendOrderItemService;
import org.opentaps.base.services.OpentapsInvoiceNonPhysicalOrderItemsService;
import org.opentaps.base.services.OpentapsUpdateOrderItemsService;
import org.opentaps.base.services.QuickShipOrderByItemService;
import org.opentaps.base.services.ReserveProductInventoryByFacilityService;
import org.opentaps.base.services.ReserveStoreInventoryService;
import org.opentaps.base.services.SetInvoiceReadyAndCheckIfPaidService;
import org.opentaps.base.services.TestShipOrderManualService;
import org.opentaps.base.services.TestShipOrderService;
import org.opentaps.base.services.UpdateInventoryItemService;
import org.opentaps.base.services.UpdatePostalAddressService;
import org.opentaps.base.services.UpdateProductStoreService;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderItemShipGrpInvRes;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.lookup.configuration.SalesOrderLookupConfiguration;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.gwt.common.server.lookup.SalesOrderLookupService;
import org.opentaps.tests.gwt.TestInputProvider;
import org.opentaps.tests.warehouse.InventoryAsserts;

/**
 * Order related unit tests.
 */
public class OrderTests extends OrderTestCase {

    private static final String MODULE = OrderTests.class.getName();

    private GenericValue DemoAccount1;
    private GenericValue DemoCustomer;
    private GenericValue DemoCSR;
    private GenericValue DemoCSR2;
    private GenericValue demowarehouse1;
    private GenericValue DemoSalesManager;
    private GenericValue GZ1005;
    private GenericValue WG5569;
    private GenericValue WG1111;
    private GenericValue ProductStore;
    private GenericValue RetailStore;
    private GenericValue Facility;
    private Security security = null;
    private static final String organizationPartyId = "Company";
    private static final String productStoreId = "9000";
    private static final String productStoreId1 = "9100";   //retail store
    private static final String facilityId = "WebStoreWarehouse";
    private static final String facilityId1 = "MyRetailStore";

    private OrderRepositoryInterface orderRepository;
    private InvoiceRepositoryInterface invoiceRepository;
    private String defaultProductStoreAutoApproveInvoice;
    private String defaultProductStoreInventoryReservationEnum;
    private String defaultFacilityInventoryReservationEnum;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        security = SecurityFactory.getInstance(delegator);
        DemoAccount1 = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoAccount1"));
        DemoCustomer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoCustomer"));
        DemoCSR = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "DemoCSR"));
        DemoCSR2 = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "DemoCSR2"));
        DemoSalesManager = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        demowarehouse1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        GZ1005 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "GZ-1005"));
        WG5569 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "WG-5569"));
        WG1111 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "WG-1111"));
        ProductStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
        RetailStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId1));
        Facility = delegator.findByPrimaryKey("Facility", UtilMisc.toMap("facilityId", facilityId));

        // save some default parameters which might be modified in the tests
        defaultProductStoreAutoApproveInvoice = ProductStore.getString("autoApproveInvoice");
        defaultProductStoreInventoryReservationEnum = ProductStore.getString("reserveOrderEnumId");
        defaultFacilityInventoryReservationEnum = Facility.getString("inventoryReserveOrderEnumId");

        // test that the object have been retrieved
        assertTrue("DemoAccount1 not null", DemoAccount1 != null);
        assertTrue("DemoCustomer not null", DemoCustomer != null);
        assertTrue("DemoCSR not null", DemoCSR != null);
        assertTrue("DemoCSR2 not null", DemoCSR2 != null);
        assertTrue("DemoSalesManager not null", DemoSalesManager != null);
        assertTrue("demowarehouse1 not null", demowarehouse1 != null);
        assertTrue("admin not null", admin != null);
        assertTrue("GZ1005 not null", GZ1005 != null);
        assertTrue("WG5569 not null", WG5569 != null);
        assertTrue("WG1111 not null", WG1111 != null);
        assertTrue("ProductStore not null", ProductStore != null);
        assertTrue("Facility not null", Facility != null);
        // set a default User
        User = DemoCSR;

        // from domains setup in OpentapsTestCase
        orderRepository = orderDomain.getOrderRepository();
        invoiceRepository = billingDomain.getInvoiceRepository();
    }

    @Override
    public void tearDown() throws Exception {
        // restore ProductStore.autoApproveInvoice to its default value, in case it was changed by testOrderPaymentWithNoAutoApproveInvoice
        setProductStoreAutoApproveInvoice(productStoreId, defaultProductStoreAutoApproveInvoice, delegator);
        // also restore the ProductStore.reserveOrderEnumId and Facility.inventoryReserveOrderEnumId to their original value
        setProductStoreInventoryReservationEnum(productStoreId, defaultProductStoreInventoryReservationEnum, delegator);
        setFacilityInventoryReservationEnum(facilityId, defaultFacilityInventoryReservationEnum, delegator);

        super.tearDown();
        DemoSalesManager = null;
        demowarehouse1 = null;
        admin = null;
    }

    /**
     * This test verifies the correct parties for a sales order.
     * @throws Exception in an error occurs
     */
    public void testSalesOrderParties() throws Exception {
        // create a product
        BigDecimal productQty = new BigDecimal("1.0");
        BigDecimal productUnitPrice = new BigDecimal("11.11");
        final GenericValue testProduct = createTestProduct("testSalesOrderParties Test Product", demowarehouse1);
        assignDefaultPrice(testProduct, productUnitPrice, admin);
        // create a customer from template of DemoCustomer
        String customerPartyId = createPartyFromTemplate(DemoCustomer.getString("partyId"), "Customer for testSalesOrderParties");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));
        // create a sales order for the customer and product
        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(testProduct, productQty);
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderSpec, customer, productStoreId, "EXT_OFFLINE", "DemoAddress2");
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(salesOrder.getOrderId());
        // verify that the organizationPartyId is the Order.getOrganizationParty()
        assertEquals("order.getOrganizationParty().getPartyId() should be " + organizationPartyId, organizationPartyId, order.getOrganizationParty().getPartyId());
        // verify that the customer is the Order.getBillToCustomer()
        assertEquals("order.getBillToCustomer().getPartyId() should be " + customerPartyId, customerPartyId, order.getBillToCustomer().getPartyId());
    }

    /**
     * Creates an sales order and checks results.
     * Use storeOrder to create a sales order for DemoAccount1 by DemoCSR of 1.0 of GZ-1005 and 4.0 of WG-5569. Verify that:
     *  (a) The ATP of GZ-1005 has declined by 1.0 but the QOH is unchanged.
     *  (b) The ATP of WG-5569 has declined by 4.0 but the QOH is unchanged.
     *  (c) There is a Requirement created for GZ-1005 of quantity 1.0 after the order is approved.
     *  (d) Requirement is created for WG-5569 as needed
     * @throws Exception in an error occurs
     */
    public void testCreateSalesOrder() throws Exception {
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(GZ1005, new BigDecimal("1.0"));
        order.put(WG5569, new BigDecimal("4.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId, "EXT_OFFLINE", "DemoAddress2");
        Debug.logInfo("testCreateSalesOrder created order [" + salesOrder.getOrderId() + "]", MODULE);

        // Call opentaps.appendOrderItem to add 1 additional GZ-1000
        // append item to order by UPC
        OpentapsAppendOrderItemService service = new OpentapsAppendOrderItemService();
        service.setInUserLogin(admin);
        service.setInOrderId(salesOrder.getOrderId());
        service.setInProductId("043000285213");
        service.setInQuantity(new BigDecimal("1.0"));
        service.setInShipGroupSeqId("00001");
        service.setInProdCatalogId(productStoreId);
        runAndAssertServiceSuccess(service);

        // Call opentaps.appendOrderItemBasic to add 1 additional GZ-2644
        // Verify that
        // (e) the order total has increased by the value of the item plus the correct tax amount (unit price is 38.40)
        // (f) the product's ATP has declined by 1

        ProductRepositoryInterface productRepository = getProductRepository(admin);
        Product additionalProduct = productRepository.getProductById("GZ-2644");

        // get initial grand total
        BigDecimal grandTotal = salesOrder.getGrandTotal();
        Debug.logInfo("Initial Grand total: " + grandTotal, MODULE);
        // get initial atp
        Map<String, Object> callResults = getProductAvailability(additionalProduct.getProductId());
        BigDecimal atp = (BigDecimal) callResults.get("availableToPromiseTotal");
        Debug.logInfo("Initial ATP:" + atp, MODULE);

        // append item to order
        GenericValue genericValueOfAdditionalProduct = Repository.genericValueFromEntity(delegator, additionalProduct);
        assertTrue("Append 1.0 GZ-2644 to order", salesOrder.appendProduct(genericValueOfAdditionalProduct, new BigDecimal("1.0")));

        // get final grand total
        BigDecimal grandTotal2 = salesOrder.getGrandTotal();
        Debug.logInfo("Final Grand total: " + grandTotal2, MODULE);
        // get final atp
        callResults = getProductAvailability(additionalProduct.getProductId());
        BigDecimal atp2 = (BigDecimal) callResults.get("availableToPromiseTotal");
        Debug.logInfo("Final ATP: " + atp2, MODULE);

        // check that the ATP has declined by 1
        BigDecimal expectedATP = atp.subtract(BigDecimal.ONE);
        assertEquals("ATP has declined by 1.0", atp2, expectedATP);

        // check that tax order adjustments have been created for the new items
        BigDecimal taxGrandTotal = ZERO_BASE;
        String orderId = salesOrder.getOrderId();

        // there should now be one SALES_TAX OrderAdjustment from _NA_
        // checking the total tax amount, it should be 1% from _NA_ of the total product prices
        // which is 38.40
        BigDecimal expectedTax = new BigDecimal("38.40").multiply(new BigDecimal("1.0")).divide(PERCENT_SCALE, 3, BigDecimal.ROUND_CEILING);
        BigDecimal taxTotal = checkSalesTax(orderId, "_NA_", "00004", 1, expectedTax);
        taxGrandTotal = taxGrandTotal.add(taxTotal);

        // there should now be one SALES_TAX OrderAdjustment from NY_DTF
        // checking the total tax amount, it should be 4.25% from NY_DTF of the total product prices
        // which is 38.40
        expectedTax = new BigDecimal("38.40").multiply(new BigDecimal("4.25")).divide(PERCENT_SCALE, 3, BigDecimal.ROUND_CEILING);
        taxTotal = checkSalesTax(orderId, "NY_DTF", "00004", 1, expectedTax);
        taxGrandTotal = taxGrandTotal.add(taxTotal);

        // checking that the order Grand total variance is equal to the additional product price + taxes (note rounding to cents because taxes have a scale of 3)
        BigDecimal expectedGrandTotal = (grandTotal.add(taxGrandTotal).add(new BigDecimal("38.40"))).setScale(DECIMALS, ROUNDING);

        assertEquals("Order Grand Total", grandTotal2, expectedGrandTotal);
    }

    /**
     * This test verifies the <code>Order</code> domain object.
     * @exception Exception if an error occurs
     */
    public void testOrderDomain() throws Exception {
        final BigDecimal product1Qty = new BigDecimal("1.0");
        final BigDecimal product2Qty = new BigDecimal("4.0");
        final BigDecimal product3Qty = new BigDecimal("2.0");

        final BigDecimal product1UnitPrice = new BigDecimal("11.11");
        final BigDecimal product2UnitPrice = new BigDecimal("22.22");
        final BigDecimal product3UnitPrice = new BigDecimal("33.33");

        final GenericValue testGZ1005 = createTestProduct("testOrderDomain Test Product 1", demowarehouse1);
        final GenericValue testWG5569 = createTestProduct("testOrderDomain Test Product 2", demowarehouse1);
        final GenericValue testWG1111 = createTestProduct("testOrderDomain Test Product 3", demowarehouse1);

        assignDefaultPrice(testGZ1005, product1UnitPrice, admin);
        assignDefaultPrice(testWG5569, product2UnitPrice, admin);
        assignDefaultPrice(testWG1111, product3UnitPrice, admin);

        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(testGZ1005, product1Qty);
        orderSpec.put(testWG5569, product2Qty);
        orderSpec.put(testWG1111, product3Qty);
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderSpec, DemoAccount1, productStoreId, "EXT_OFFLINE", "DemoAddress2");
        Debug.logInfo("testOrderDomain created order [" + salesOrder.getOrderId() + "]", MODULE);
        salesOrder.approveOrder();

        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(salesOrder.getOrderId());

        // check order type
        assertEquals("Order type should be SALES_ORDER", order.getType().getOrderTypeId(), "SALES_ORDER");
        assertTrue("Order type should be SALES_ORDER", order.isSalesOrder());

        // check order status
        assertTrue("Order should be APPROVED", order.isApproved());

        // check product ids
        Set<String> orderProductIds = order.getProductIds();
        Boolean hasGZ1005 = false;
        Boolean hasWG5569 = false;
        Boolean hasWG1111 = false;
        for (String productId : orderProductIds) {
            assertNotNull("Unexpected null product id found", productId);
            if (productId.equals(testGZ1005.getString("productId"))) {
                hasGZ1005 = true;
            } else if (productId.equals(testWG5569.getString("productId"))) {
                hasWG5569 = true;
            } else if (productId.equals(testWG1111.getString("productId"))) {
                hasWG1111 = true;
            } else {
                fail("Unexpected product id found: " + productId);
            }
        }
        if (!(hasGZ1005 && hasWG5569 && hasWG1111)) {
            fail("Did not find all expected product ids from the order");
        }

        // check order items
        List<OrderItem> orderItems = order.getItems();
        assertNotEmpty("Could not find any order item for the order", orderItems);
        assertEquals("Unexpected number of items in the order", orderSpec.keySet().size(), orderItems.size());
        for (OrderItem i : orderItems) {
            if (testGZ1005.getString("productId").equals(i.getProductId())) {
                assertEquals("Unexpected ordered quantity for product 1 order item", product1Qty, i.getOrderedQuantity());
                assertEquals("Unexpected price for product 1 order item", product1UnitPrice, i.getUnitPrice());
            } else if (testWG5569.getString("productId").equals(i.getProductId())) {
                assertEquals("Unexpected ordered quantity for product 2 order item", product2Qty, i.getOrderedQuantity());
                assertEquals("Unexpected price for product 2 order item", product2UnitPrice, i.getUnitPrice());
            } else if (testWG1111.getString("productId").equals(i.getProductId())) {
                assertEquals("Unexpected ordered quantity for product 3 order item", product3Qty, i.getOrderedQuantity());
                assertEquals("Unexpected price for product 3 order item", product3UnitPrice, i.getUnitPrice());
            } else {
                fail("Unexpected product id found in the order items.");
            }
        }

        // check order totals
        BigDecimal expectedItemsSubTotal = product1UnitPrice.multiply(product1Qty);
        Debug.logInfo("Expected product 1 price: " + product1UnitPrice + " x " + product1Qty + " = " + product1UnitPrice.multiply(product1Qty), MODULE);
        expectedItemsSubTotal = expectedItemsSubTotal.add(product2UnitPrice.multiply(product2Qty));
        Debug.logInfo("Expected product 2 price: " + product2UnitPrice + " x " + product2Qty + " = " + product2UnitPrice.multiply(product2Qty), MODULE);
        expectedItemsSubTotal = expectedItemsSubTotal.add(product3UnitPrice.multiply(product3Qty));
        Debug.logInfo("Expected product 3 price: " + product3UnitPrice + " x " + product3Qty + " = " + product3UnitPrice.multiply(product3Qty), MODULE);

        Debug.logInfo("order sub total = " + order.getItemsSubTotal(), MODULE);
        assertEquals("Unexpected items sub total", expectedItemsSubTotal, order.getItemsSubTotal());

        List<GenericValue> adjustments = delegator.findByAnd("OrderAdjustment", UtilMisc.toMap("orderId", order.getOrderId()));
        BigDecimal adjustmentsTotal = BigDecimal.ZERO;
        for (GenericValue adj : adjustments) {
            adjustmentsTotal = (adjustmentsTotal.add(adj.getBigDecimal("amount"))).setScale(DECIMALS, ROUNDING);
        }

        BigDecimal expectedOrderTotal = expectedItemsSubTotal.add(adjustmentsTotal);

        Debug.logInfo("order total = " + order.getTotal(), MODULE);
        assertEquals("Unexpected order total", expectedOrderTotal, order.getTotal());

    }

    /**
     * This test verifies that inventory reservations are created and released correctly for serialized inventory.
     * Create a new product
     * Receive 10 units of it as serialized inventory
     * Set the ProductStore's reserveOrderEnumId to FIFO Received
     * Create a sales order for 5 units of the product
     * Verify that the ATP has decreased by 5.0
     * Verify that the first 5 serialized inventory items (in the order of their received date field) are the ones which are now reserved in the OrderItemShipGrpInvRes table.
     * Change the order item's quantity to 2 units and verify that the ATP has increased by 2.0
     * Cancel the order and verify that the ATP has increased by another 3.0 and is back to 10.0 again
     *
     *
     * Set the product store's reserveOrderEnumId to LIFO Received
     * Create another sales order for 5 units of the product
     * Verify that the ATP has decreased by 5.0
     * Verify that the last 5 serialized inventory items (in the order of their received date field) are the ones which are now reserved in the OrderItemShipGrpInvRes table.
     * @exception GeneralException if an error occurs
     */
    public void testSerializedInventoryReservation() throws GeneralException {

        // create test product
        GenericValue testProduct = createTestProduct("Serialized Product for Reservation Test", demowarehouse1);
        String productId = testProduct.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(testProduct, new BigDecimal("200.0"), admin);

        // Receive 10 units of the product as serialized inventory.
        // Products are receiving separately specially to assign different datetimeReceived.
        List<String> firstFive = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> res = receiveInventoryProduct(testProduct, new BigDecimal("1.0"), "SERIALIZED_INV_ITEM", new BigDecimal("199.0"), demowarehouse1);
            // memorize first 5 inventory items
            if (i < 5) {
                firstFive.add((String) res.get("inventoryItemId"));
            }
        }

        // set the ProductStore's reserveOrderEnumId to FIFO Received
        UpdateProductStoreService service = new UpdateProductStoreService();
        service.setInUserLogin(admin);
        service.setInProductStoreId("9000");
        service.setInReserveOrderEnumId("INVRO_FIFO_REC");
        runAndAssertServiceSuccess(service);

        // create and approve sales order for 5 units of the product
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(testProduct, new BigDecimal("5.0"));
        User = DemoCSR;
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        String orderId = orderFactory.getOrderId();
        Debug.logInfo("testSerializedInventoryReservation created order [" + orderId + "]", MODULE);

        // verify ATP = 5
        GetProductService getProduct = new GetProductService();
        getProduct.setInUserLogin(demowarehouse1);
        getProduct.setInProductId(productId);
        runAndAssertServiceSuccess(getProduct);
        GenericValue product = getProduct.getOutProduct();

        Map<String, Object> productAvailability = getProductAvailability(product.getString("productId"));
        BigDecimal availableToPromis = (BigDecimal) productAvailability.get("availableToPromiseTotal");
        assertEquals(String.format("Wrong ATP value of product [%1$s]", productId), 5, availableToPromis.longValue());

        // find first 5 inventory of the products
        EntityCondition conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition("inventoryItemId", EntityOperator.IN, firstFive)
                ),
                EntityOperator.AND);
        long reservedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvResAndItem", conditionList, null);
        assertEquals("It's may be problem. Real count of reserved inventory items with right time doesn't equals to expected.", 5, reservedInventoryItemsCount);

        // change the order item's quantity to 2 units
        updateOrderItem(orderId, "00001", "2.0", null, "Item after new quantity applied", DemoCSR);

        // verify ATP = 8
        assertProductATP(product, new BigDecimal("8.0"));

        // cancel order
        cancelOrderItem(orderId, "00001", "00001", new BigDecimal("2.0"), DemoCSR);

        // verify ATP = 10
        assertProductATP(product, new BigDecimal("10.0"));

        // set the ProductStore's reserveOrderEnumId to LIFO Received
        service = new UpdateProductStoreService();
        service.setInUserLogin(admin);
        service.setInProductStoreId("9000");
        service.setInReserveOrderEnumId("INVRO_LIFO_REC");
        runAndAssertServiceSuccess(service);

        // create and approve another sales order for 5 units of the product
        orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId)), new BigDecimal("5.0"));
        User = DemoCSR;
        orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        orderId = orderFactory.getOrderId();
        Debug.logInfo("testSerializedInventoryReservation created order [" + orderId + "]", MODULE);

        // verify ATP = 5
        assertProductATP(product, new BigDecimal("5.0"));

        // verify reservation of last received inventory items
        conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition("inventoryItemTypeId", EntityOperator.EQUALS, "SERIALIZED_INV_ITEM")
                )
                , EntityOperator.AND);
        List<GenericValue> inventoryItems = delegator.findByCondition("InventoryItem", conditionList, null, Arrays.asList("datetimeReceived DESC"));
        // get inventory item ids which should be reserved.
        List<String> invIds = EntityUtil.<String>getFieldListFromEntityList(inventoryItems, "inventoryItemId", false).subList(0, 5);

        conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition("inventoryItemId", EntityOperator.IN, invIds)
                ),
                EntityOperator.AND);
        reservedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvResAndItem", conditionList, null);
        assertEquals("It's may be problem. Real count of reserved inventory items with right time doesn't equals to expected.", 5, reservedInventoryItemsCount);
    }

    /**
     * This test will test the balancing of inventory service for non-serialized inventory
     * 1. Create a new product
     * 2. Create a sales order for 1 unit of the product
     * 3. Verify that a new InventoryItem was created with an ATP quantity of -1.0 and a QOH of 0.0
     * 4. Receive 5.0 units of this product as non-serialized inventory
     * 5. Verify that the InventoryItem from step (3) now has ATP and QOH quantity of 0.0, and a new InventoryItem has a quantity of ATP=4.0, QOH=5.0
     * @exception GeneralException if an error occurs
     */
    public void testNonSerInventoryItemBalancing() throws GeneralException {
        // 1. create test product
        GenericValue product = createTestProduct("Non-Serialized Balancing Test Product", demowarehouse1);
        String productId = product.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(product, new BigDecimal("100.0"), admin);

        // 2. create sales order of 1x product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("1.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId);
        Debug.logInfo("testNonSerInventoryItemBalancing created order [" + salesOrder.getOrderId() + "]", MODULE);

        // 3. check there is a new InventoryItem with an ATP of -1.0 and a QOH of 0.0
        List<GenericValue> inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId));
        assertEquals("InventoryItem for product [" + productId + "]", 1, inventoryItems.size());
        GenericValue firstInventoryItem = inventoryItems.get(0);
        String firstInventoryItemId = firstInventoryItem.getString("inventoryItemId");
        assertInventoryItemQuantities("InventoryItem for product [" + productId + "]", firstInventoryItemId, -1.0, 0.0);

        // 4. receive 5.0 units of the product as non serialized inventory
        receiveInventoryProduct(product, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);

        // 5. Verify that the InventoryItem from (3) now has ATP and QOH of 0.0,
        // and a new InventoryItem was created with ATP=4.0, QOH=5.0
        inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId));
        assertEquals("InventoryItem for product [" + productId + "]", 2, inventoryItems.size());
        for (GenericValue inventoryItem : inventoryItems) {
            if (firstInventoryItemId.equals(inventoryItem.getString("inventoryItemId"))) {
                assertInventoryItemQuantities("First InventoryItem for product [" + productId + "]", inventoryItem.getString("inventoryItemId"), 0.0, 0.0);
            } else {
                assertInventoryItemQuantities("First InventoryItem for product [" + productId + "]", inventoryItem.getString("inventoryItemId"), 4.0, 5.0);
            }
        }
    }

    /**
     * This test will verify the balancing of inventory for serialized inventory and is identical to the one above for non-serialized inventory,
     * except that step (4) should be to receive 5.0 unit of the product as serialized inventory.
     * @exception GeneralException if an error occurs
     */
    public void testSerInventoryItemBalancing() throws GeneralException {
        // 1. create test product
        GenericValue product = createTestProduct("Serialized Balancing Test Product", demowarehouse1);
        String productId = product.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(product, new BigDecimal("100.0"), admin);

        // 2. create sales order of 1x product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("1.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId);
        Debug.logInfo("testSerInventoryItemBalancing created order [" + salesOrder.getOrderId() + "]", MODULE);

        // 3. check there is a new InventoryItem with an ATP of -1.0 and a QOH of 0.0
        List<GenericValue> inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId));
        assertEquals("InventoryItem for product [" + productId + "]", 1, inventoryItems.size());
        GenericValue inventoryItem = inventoryItems.get(0);
        String firstInventoryItemId = inventoryItem.getString("inventoryItemId");
        assertInventoryItemQuantities("InventoryItem for product [" + productId + "]", firstInventoryItemId, -1.0, 0.0);

        // 4. receive 5.0 units of the product as serialized inventory
        receiveInventoryProduct(product, new BigDecimal("5.0"), "SERIALIZED_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);

        // 5. Verify that the InventoryItem from (3) now has ATP and QOH of 0.0,
        inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM", "inventoryItemId", firstInventoryItemId));
        inventoryItem = inventoryItems.get(0);
        assertInventoryItemQuantities("InventoryItem for product [" + productId + "]", inventoryItem.getString("inventoryItemId"), 0.0, 0.0);

        // verify that there is 1 SERIALIZED_INV_ITEM INV_PROMISED with QOH=1.0 and ATP=0.0
        inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "inventoryItemTypeId", "SERIALIZED_INV_ITEM", "statusId", "INV_PROMISED"));
        assertEquals("Promised InventoryItem for product [" + productId + "]", 1, inventoryItems.size());
        inventoryItem = inventoryItems.get(0);
        assertInventoryItemQuantities("InventoryItem for product [" + productId + "]", inventoryItem.getString("inventoryItemId"), 0.0, 1.0);

        // verify that there are 4 SERIALIZED_INV_ITEM INV_AVAILABLE with QOH=1.0 and ATP=1.0
        inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "inventoryItemTypeId", "SERIALIZED_INV_ITEM", "statusId", "INV_AVAILABLE"));
        assertEquals("Available InventoryItem for product [" + productId + "]", 4, inventoryItems.size());
        for (GenericValue item : inventoryItems) {
            assertInventoryItemQuantities("Available InventoryItem for product [" + productId + "]", item.getString("inventoryItemId"), 1.0, 1.0);
        }
    }

    /**
     * This test will verify that the system handles reservations for defective serialized inventory items and back orders correctly
     * 1.  Create a new product
     * 2.  Receive 5.0 units of this product as serialized inventory
     * 3.  Find the first two InventoryItems received by their received date and change their status to "Defective"
     * 4.  Verify that the ATP and QOH of this product are both now 3.0
     * 5.  Create and approve a sales order for 5.0 of the product
     * 6.  Verify that only the inventory items whose status is still "Available" are reserved against the order
     * 7.  Verify a new InventoryItem with ATP = -2.0 and QOH = 0 has been created for this product
     * @exception GeneralException if an error occurs
     */
    public void testDefectiveSerInventoryItemReservation() throws GeneralException {

        // create test product
        GenericValue testProduct = createTestProduct("Serialized Test Product", demowarehouse1);
        String productId = testProduct.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(testProduct, new BigDecimal("100.0"), admin);

        // receive 5.0 units of the product as serialized inventory
        receiveInventoryProduct(testProduct, new BigDecimal("5.0"), "SERIALIZED_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);

        List<GenericValue> inventories = delegator.findByCondition("ProductInventoryItem", EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId), Arrays.asList("inventoryItemId"), Arrays.asList("inventoryItemId"));
        assertEquals(String.format("Wrong count inventory items for product [%1$s]", productId), 5, inventories.size());

        // set status of first two inventory items to Defective
        int i = 0;
        UpdateInventoryItemService updateInventoryItem;
        for (GenericValue inventoryItem : inventories) {
            updateInventoryItem = new UpdateInventoryItemService();
            updateInventoryItem.setInUserLogin(demowarehouse1);
            updateInventoryItem.setInInventoryItemId(inventoryItem.getString("inventoryItemId"));
            updateInventoryItem.setInStatusId("INV_DEFECTIVE");
            runAndAssertServiceSuccess(updateInventoryItem);
            i++;
            if (i > 1) {
                break;
            }
        }

        // get inventory item ids for test product having status Available. This
        // list we will use later.
        List<GenericValue> availableInventories = delegator.findByAnd("ProductInventoryItem", UtilMisc.toMap("productId", productId, "statusId", "INV_AVAILABLE"));
        assertEquals("At this point should be 3 available items", 3, availableInventories.size());
        List<String> availableInventoryIds = EntityUtil.getFieldListFromEntityList(availableInventories, "inventoryItemId", true);

        // check availability for the product. Expected QOH/ATP = 3
        GetProductService getProduct = new GetProductService();
        getProduct.setInUserLogin(demowarehouse1);
        getProduct.setInProductId(productId);
        runAndAssertServiceSuccess(getProduct);
        GenericValue product = getProduct.getOutProduct();

        Map<String, Object> productAvailability = getProductAvailability(product.getString("productId"));
        BigDecimal quantityOnHand = (BigDecimal) productAvailability.get("quantityOnHandTotal");
        BigDecimal availableToPromis = (BigDecimal) productAvailability.get("availableToPromiseTotal");
        assertEquals(String.format("Wrong QOH value of product [%1$s]", productId), 3, quantityOnHand.longValue());
        assertEquals(String.format("Wrong ATP value of product [%1$s]", productId), 3, availableToPromis.longValue());

        // create and approve sales order for 5 products
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId)), new BigDecimal("5.0"));
        User = DemoCSR;
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        String orderId = orderFactory.getOrderId();
        Debug.logInfo("testDefectiveSerInventoryItemReservation created order [" + orderId + "]", MODULE);

        // get list of reserved inventory items and check if they had status Available before order approval.
        EntityCondition conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, null)
                ), EntityOperator.AND
        );
        List<GenericValue> reservedInventoryItems = delegator.findByCondition("OrderItemShipGrpInvRes", conditionList, Arrays.asList("orderId", "inventoryItemId", "quantity"), null);
        assertEquals(String.format("Wrong number of reserved items of product [%1$s] against order [%1$s]", productId, orderId), 3, reservedInventoryItems.size());

        for (GenericValue reservation : reservedInventoryItems) {
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", reservation.getString("inventoryItemId")));
            assertTrue(String.format("Wrong inventory item [%1$s] has been reserved against order [%2$s].", inventoryItem.getString("inventoryItemId"), orderId), availableInventoryIds.contains(inventoryItem.getString("inventoryItemId")));
        }

        // for back ordered items an inventory item with ATP=-2.0, QOH=0 must exist.
        long backOrderedInvItemCount = delegator.findCountByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "quantityOnHandTotal", BigDecimal.ZERO, "availableToPromiseTotal", new BigDecimal("-2.0")));
        assertEquals(String.format("No valid record for backordered items of product %1$s", productId), 1, backOrderedInvItemCount);
    }

    /**
     * This test will verify that the system handles reservations for defective non-serialized inventory items and back orders correctly
     * 1.  Create a new product
     * 2.  Receive 5.0 units of this product as non-serialized inventory
     * 3.  Create a physical inventory variance for -2.0 QOH and -2.0 ATP for this product with reason "Damaged"
     * 4.  Verify that the ATP and QOH of this product are both now 3.0
     * 5.  Create and approve a sales order for 5.0 of the product
     * 6.  Verify that inventory item from (2) is reserved against the order with quantity = 5 and quantityNotAvailable = -5
     * 7.  Verify that inventory item from (2) now has ATP = -2.0 and QOH = 3
     * 8.  Verify that the product has ATP = -2 and QOH = 3
     * @exception GeneralException if an error occurs
     */
    public void testDefectiveInventoryItemReservation() throws GeneralException {
        // create test product
        GenericValue testProduct = createTestProduct("Non-serialized Test Product", demowarehouse1);
        String productId = testProduct.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(testProduct, new BigDecimal("100.0"), admin);

        // receive 5.0 units of the product as non-serialized inventory
        Map<String, Object> inventory = receiveInventoryProduct(testProduct, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);
        String inventoryItemId = (String) inventory.get("inventoryItemId");

        // create a physical inventory variance for -2.0 QOH and -2.0 ATP with reason "Damaged"
        CreatePhysicalInventoryAndVarianceService call = new CreatePhysicalInventoryAndVarianceService();
        call.setInUserLogin(demowarehouse1);
        call.setInInventoryItemId(inventoryItemId);
        call.setInAvailableToPromiseVar(new BigDecimal("-2.0"));
        call.setInQuantityOnHandVar(new BigDecimal("-2.0"));
        call.setInVarianceReasonId("VAR_DAMAGED");
        runAndAssertServiceSuccess(call);

        // check availability for the product. Expected QOH/ATP = 3
        GetProductService getProduct = new GetProductService();
        getProduct.setInUserLogin(demowarehouse1);
        getProduct.setInProductId(productId);
        runAndAssertServiceSuccess(getProduct);
        GenericValue product = getProduct.getOutProduct();

        assertProductAvailability(product, new BigDecimal("3.0"), new BigDecimal("3.0"));

        // create and approve sales order for 5 products
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId)), new BigDecimal("5.0"));
        User = DemoCSR;
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        String orderId = orderFactory.getOrderId();
        Debug.logInfo("testDefectiveInventoryItemReservation created order [" + orderId + "]", MODULE);

        // get count of reserved inventory items. Should be 1 inventory item and quantity = 5, quantityNotAvaible = -2.
        EntityCondition conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItemId),
                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, new BigDecimal("2.0")),
                        EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, new BigDecimal("5.0"))
                ),
                EntityOperator.AND
        );
        long reservedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvRes", conditionList, null);
        assertEquals(String.format("Wrong number of reserved items and number of product [%1$s] against order [%1$s]", productId, orderId), 1, reservedInventoryItemsCount);

        // verify the item's ATP and QOH
        GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
        assertInventoryItemQuantities("InventoryItem for product [" + productId + "]", inventoryItem.getString("inventoryItemId"), -2.0, 3.0);

        // verify the product's inventory
        assertProductAvailability(product, new BigDecimal("-2.0"), new BigDecimal("3.0"));
    }

    /**
     * NOTE: Another test should be for whether the system handles inventory transfers correctly by not reserving inventory against them,
     * but alas it does not.  This is an open JIRA issue and I'm hoping it gets fixed at some point.
     */

    /**
     * Creates a second sales order and checks results.
     * Use storeOrder to create a second sales order for DemoCustomer by DemoCSR2 of 4.0 of WG-5569. Verify that:
     *  (a) ATP of WG-5569 has declined by 4.0 again but QOH is unchanged.
     *  (b) Requirement is created for WG-5569 as needed
     * @exception GeneralException if an error occurs
     */
    public void testCreateSecondSalesOrder() throws GeneralException {
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(WG5569, new BigDecimal("4.0"));
        User = DemoCSR2;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, productStoreId);
        Debug.logInfo("testCreateSecondSalesOrder created order [" + salesOrder.getOrderId() + "]", MODULE);
    }

    /**
     * Test cancel sales order.
     * From the first order above, cancel GZ-1005 and 2.0 of the 4.0 WG-5569
     * Verify that the order total has changed by the correct amount
     * Verify that the ATP of GZ-1005 has increased by 1.0, of WG-5569 has increased by 2.0
     * @exception GeneralException if an error occurs
     */
    public void testCancelSalesOrder() throws GeneralException {
        // making an order
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(GZ1005, new BigDecimal("1.0"));
        order.put(WG5569, new BigDecimal("4.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId);
        Debug.logInfo("testCancelSalesOrder created order [" + salesOrder.getOrderId() + "]", MODULE);

        // to store ATP before and after we cancel order items
        Map<GenericValue, BigDecimal> product_ATP_initial, product_ATP_final;
        product_ATP_initial = new HashMap<GenericValue, BigDecimal>();
        product_ATP_final = new HashMap<GenericValue, BigDecimal>();
        // store service call results
        Map<String, Object> callResults;

        Set<GenericValue> items = order.keySet();

        // get initial products ATP
        for (Iterator<GenericValue> iter = items.iterator(); iter.hasNext();) {
            GenericValue product = iter.next();
            callResults = getProductAvailability(product.getString("productId"));
            product_ATP_initial.put(product, (BigDecimal) callResults.get("availableToPromiseTotal"));
            Debug.logInfo("Initial availability of [" + product.get("productId") + "] : ATP=" + product_ATP_initial.get(product), MODULE);
        }

        // cancel 2.0 or product2
        assertTrue("Cancel 2.0 of WG5569", salesOrder.cancelProduct(WG5569, new BigDecimal("2.0")));

        // cancel 1.0 of product1
        assertTrue("Cancel 1.0 of GZ1005", salesOrder.cancelProduct(GZ1005, new BigDecimal("1.0")));

        // get final products ATP
        for (Iterator<GenericValue> iter = items.iterator(); iter.hasNext();) {
            GenericValue product = iter.next();
            callResults = getProductAvailability(product.getString("productId"));
            product_ATP_final.put(product, (BigDecimal) callResults.get("availableToPromiseTotal"));
            Debug.logInfo("Final availability of [" + product.get("productId") + "] : ATP=" + product_ATP_final.get(product), MODULE);
        }

        // product1 ATP should have increased by 1
        BigDecimal expected_ATP = product_ATP_initial.get(GZ1005).add(new BigDecimal("1.0"));
        assertEquals("ATP of " + GZ1005.get("productId"), product_ATP_final.get(GZ1005), expected_ATP);

        // product2 ATP should have increased by 2
        expected_ATP = product_ATP_initial.get(WG5569).add(new BigDecimal(2.0));
        assertEquals("ATP of " + WG5569.get("productId"), product_ATP_final.get(WG5569), expected_ATP);
    }

    /**
     * Test cancel sales order with promo items, test a bug in the way promo items are handled.
     * The following promo applies:
     * - Promo 9000 -> get a free WG-1111 for spending more than $100 in GZ/WG products
     * - Promo 9013 -> buy 3 get 2 free from WG (limit = 2)
     * - Promo 9010 -> 20% off a GZ-1005
     * - Promo 9011 -> 10% off the entire order
     * - Promo 9017 -> get a free GZ-1006 for spending more than $50 in GZ products (limit = 2)
     * - Promo 9018 -> get a free GZ-1006 for spending more than $150 in GZ products (limit = 1)
     *
     * 1. create an order of 1x GZ-1005 and 10x WG-1111
     * 2. check that the following promo items are created:
     *  - 1x GZ-1006-4 (because of more than $150 in GZ products)
     *  - 1x WG-1111 (because of more than $100 in GZ/WG products)
     * 3. cancel 8 x WG-1111
     * 4. check that the following promo items are created:
     *  - 1x GZ-1006-4 (because of more than $150 in GZ products)
     *  - 1x WG-1111 (because of more than $100 in GZ/WG products)
     * 5. cancel the GZ-1005
     * 6. check that the following promo item is created:
     *  - 1x WG-1111 (because of more than $100 in GZ/WG products)
     * 7. cancel the remaining item (2x WG-1111)
     * 8. check the whole order is now cancelled
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCancelSalesOrderWithPromoItems() throws GeneralException {
        // 1. make the order 1x GZ-1005 + 10x WG-1111
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(GZ1005, new BigDecimal("1.0"));
        orderItems.put(WG1111, new BigDecimal("10.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, DemoCustomer, productStoreId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testCancelSalesOrderWithPromoItems created order [" + orderId + "]", MODULE);
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(salesOrder.getOrderId());

        // Find the order items
        OrderItem GZ1005Item = null;
        OrderItem WG1111Item = null;
        for (OrderItem item : order.getOrderItems()) {
            if (GZ1005.get("productId").equals(item.getProductId())) {
                GZ1005Item = item;
            } else if (WG1111.get("productId").equals(item.getProductId())) {
                WG1111Item = item;
            }
        }
        assertNotNull("Did not find order item for GZ-1005 in order [" + orderId + "]", GZ1005Item);
        assertNotNull("Did not find order item for WG-1111 in order [" + orderId + "]", WG1111Item);

        // This is a hack to force the creation of promo items:
        //  Update the order with same quantities
        OpentapsUpdateOrderItemsService updateItems = new OpentapsUpdateOrderItemsService();
        updateItems.setInUserLogin(User);
        updateItems.setInOrderId(orderId);
        updateItems.setInItemQtyMap(UtilMisc.toMap(GZ1005Item.getOrderItemSeqId() + ":00001", "1.0", WG1111Item.getOrderItemSeqId() + ":00001", "10.0"));
        updateItems.setInItemPriceMap(UtilMisc.toMap(GZ1005Item.getOrderItemSeqId(), "2799.99", WG1111Item.getOrderItemSeqId(), "59.99"));
        updateItems.setInOverridePriceMap(new HashMap());
        runAndAssertServiceSuccess(updateItems);

        // 2. check promo items 1x GZ-1006 (promo 9017 or 9018) + 1x WG-1111 (promo 9000)
        // note: this only works if the item are in stock !
        // note2: promo items have status ITEM_CREATED instead of ITEM_APPROVED because we don't
        // manually approve those after the initial order, this is to spot them more easily
        assertPromoItemExists(orderId, "GZ-1006", new BigDecimal("1.0"), "9018", "ITEM_CREATED");
        assertPromoItemExists(orderId, "WG-1111", new BigDecimal("1.0"), "9000", "ITEM_CREATED");

        // check items total quantities
        assertNormalOrderItems(orderId, UtilMisc.toMap("GZ-1005", new BigDecimal("1.0"), "WG-1111", new BigDecimal("10.0")));
        // verify if a variant of GZ-1006 and WG-1111 are still on the order
        assertPromoItemExists(orderId, "GZ-1006", new BigDecimal("1.0"), "9018", "ITEM_CREATED");
        assertPromoItemExists(orderId, "WG-1111", new BigDecimal("1.0"), "9000", "ITEM_CREATED");

        // check order totals
        Debug.logInfo("testCancelSalesOrderWithPromoItems: step 1 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  10 - 4 free WG-1111 = 359.94
        //  1 x GZ-1005 - 20% off = 2239.992
        //  = 2599.932 -> rounded to 2599.93
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("2599.93"));
        // Tax amount should be 5.85% of this total, minus 0.1% of the 10% off global promotion amount (the UT_UTAH_TAXMAN 0.1% applies to promotions)
        //  5.85 % of 2599.932 = 152.096022 -> rounded to 152.10 | minus 0.1% of 283.99 ~ -0.284 ==> 151.816 -> rounded to 151.82
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("151.82"));
        // Order global promotion should be:
        //  10% off order total = 10 % of (2239.992 + 10 x 59.99) = 283.989 -> 283.99
        assertEquals("Order [" + orderId + "] global adjustment incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-283.99"));

        // 3. cancel 8 x WG-1111
        CancelOrderItemService cancelItem = new CancelOrderItemService();
        cancelItem.setInUserLogin(User);
        cancelItem.setInOrderId(orderId);
        cancelItem.setInOrderItemSeqId(WG1111Item.getOrderItemSeqId());
        cancelItem.setInShipGroupSeqId("00001");
        cancelItem.setInCancelQuantity(new BigDecimal("8.0"));
        runAndAssertServiceSuccess(cancelItem);

        // 4. check order items 1x GZ-1005, 2x WG-1111, 1x WG-1111 (Promo) + 1x GZ-1006 (Promo)
        // note: promotions adjustments  do not follow the newly created items and stays associated to the previously canceled promo items instead.
        // check items total quantities
        assertNormalOrderItems(orderId, UtilMisc.toMap("GZ-1005", new BigDecimal("1.0"), "WG-1111", new BigDecimal("2.0")));
        assertPromoItemExists(orderId, "GZ-1006", new BigDecimal("1.0"), "9018", "ITEM_CREATED");
        assertPromoItemExists(orderId, "WG-1111", new BigDecimal("1.0"), "9000", "ITEM_CREATED");

        // check new order totals
        order = repository.getOrderById(salesOrder.getOrderId());
        Debug.logInfo("testCancelSalesOrderWithPromoItems: step 2 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  2 x WG-1111 = 119.98
        //  1 x GZ-1005 - 20% off = 2239.992
        //  = 2359.972 rounded to 2359.97
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("2359.97"));
        // Tax amount should be 5.85% of this total:
        //  5.85 % of 2359.972 = 138.058362 -> rounded to 138.058 | minus 0.1% of 236.00 ~ -0.236 ==> 137.822 -> rounded to 137.82
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("137.82"));
        // Order global promotion should be:
        //  10% off order total = 10 % of (2239.992 + 2 x 59.99) = 235.9972 -> 236.00
        assertEquals("Order [" + orderId + "] global adjustment incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-236.00"));

        // 5. cancel 1x GZ-1005
        cancelItem = new CancelOrderItemService();
        cancelItem.setInUserLogin(User);
        cancelItem.setInOrderId(orderId);
        cancelItem.setInOrderItemSeqId(GZ1005Item.getOrderItemSeqId());
        cancelItem.setInShipGroupSeqId("00001");
        cancelItem.setInCancelQuantity(new BigDecimal("1.0"));
        runAndAssertServiceSuccess(cancelItem);

        // 6. check order items 2x WG-1111 + 1x WG-1111 (Promo)
        // check items total quantities
        assertNormalOrderItems(orderId, UtilMisc.toMap("WG-1111", new BigDecimal("2.0")));
        assertPromoOrderItems(orderId, UtilMisc.toMap("WG-1111", new BigDecimal("1.0")));

        // check new order totals
        order = repository.getOrderById(salesOrder.getOrderId());
        Debug.logInfo("testCancelSalesOrderWithPromoItems: step 3 order total [" + order.getTotal() + "], adjustments total [" + order.getOtherAdjustmentsAmount() + "], tax amount [" + order.getTaxAmount() + "], shipping [" + order.getShippingAmount() + "], items [" + order.getItemsSubTotal() + "]", MODULE);
        // Order items sub total should be:
        //  2 x WG-1111 = 119.98
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("119.98"));
        // Tax amount should be 5.85% of this total:
        //  5.85 % of 119.98 = 7.01883 -> rounded to 7.019 | minus 0.1% of 12.00 ~ -0.012 ==> 7.007 -> rounded to 7.01
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("7.01"));
        // Order global promotion should be:
        //  10% off order total = 10 % of (2 x 59.99) = 12.00
        assertEquals("Order [" + orderId + "] global adjustment incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-12.00"));

        // 7. cancel remaining 2x WG-1111
        cancelItem = new CancelOrderItemService();
        cancelItem.setInUserLogin(User);
        cancelItem.setInOrderId(orderId);
        cancelItem.setInOrderItemSeqId(WG1111Item.getOrderItemSeqId());
        cancelItem.setInShipGroupSeqId("00001");
        cancelItem.setInCancelQuantity(new BigDecimal("2.0"));
        runAndAssertServiceSuccess(cancelItem);

        // 8. check the whole order is now cancelled
        assertOrderCancelled(orderId);
    }

    /**
     * Test tax calculation
     * 1. Creates an order of 1.0 of GZ-1005 and 4.0 of WG-5569
     * 2. Checks that orderAdjustments are recorded properly
     *  (a) one OrderAdjustment for TaxAuth CA_BOE per item
     *  (b) one OrderAdjustment for TaxAuth _NA_ per item
     *  (c) that the calculated tax amounts are correct (6.25% for CA_BOE and 1% for _NA_)
     * @exception GeneralException if an error occurs
     */
    public void testTaxCalculation() throws GeneralException {
        // making an order
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(GZ1005, new BigDecimal("1.0"));
        order.put(WG5569, new BigDecimal("4.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId, null, "DemoAddress1");
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testTaxCalculation created order [" + orderId + "]", MODULE);

        // service call context
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", User);
        callCtxt.put("orderId", salesOrder.getOrderId());

        // there should now be two SALES_TAX OrderAdjustment from CA
        // checking the total tax amount, it should be 6.25% from CA_BOE of the total product prices
        // which is 1.0 x 2799.99 + 4.0 x 48 = 2991.99 -> 186.999375 ~ 187.000
        BigDecimal expectedTax = new BigDecimal("2991.99").multiply(new BigDecimal("6.25")).divide(PERCENT_SCALE, SALES_TAX_CALC_DECIMALS, SALES_TAX_ROUNDING);
        checkSalesTax(orderId, "CA_BOE", 2, expectedTax);

        // and two SALES_TAX OrderAdjustment from _NA_ (one per product)
        // checking the total tax amount, it should be 1% from _NA_ of the total product prices
        // which is 1.0 x 2799.99 + 4.0 x 48 = 2991.99
        expectedTax = new BigDecimal("2991.99").multiply(new BigDecimal("1.0")).divide(PERCENT_SCALE, SALES_TAX_CALC_DECIMALS, SALES_TAX_ROUNDING);
        checkSalesTax(orderId, "_NA_", 2, expectedTax);
    }

    /**
     * Test cancelling an order by setting quantities to 0 using the opentaps.updateOrderItems service.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCancelByUpdateOrderItems()  throws GeneralException {
        // making an order
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(GZ1005, new BigDecimal("1.0"));
        order.put(WG5569, new BigDecimal("4.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testCancelByUpdateOrderItems created order [" + orderId + "]", MODULE);

        // cancel the items by changing its quantity to 0
        OpentapsUpdateOrderItemsService updateItems = new OpentapsUpdateOrderItemsService();
        updateItems.setInUserLogin(User);
        updateItems.setInOrderId(orderId);
        updateItems.setInItemQtyMap(UtilMisc.toMap("00001:00001", "0.0", "00002:00001", ".0"));
        updateItems.setInItemPriceMap(UtilMisc.toMap("00001", "2799.99", "00002", "48.0"));
        updateItems.setInOverridePriceMap(new HashMap());
        runAndAssertServiceSuccess(updateItems);

        assertOrderCancelled("testCancelByUpdateOrderItems", orderId);

        // try another order with payment method EXT_COD
        salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId, "EXT_COD", null);
        orderId = salesOrder.getOrderId();
        Debug.logInfo("testCancelByUpdateOrderItems created second order [" + orderId + "]", MODULE);

        // cancel the items by changing its quantity to 0
        updateItems = new OpentapsUpdateOrderItemsService();
        updateItems.setInUserLogin(User);
        updateItems.setInOrderId(orderId);
        updateItems.setInItemQtyMap(UtilMisc.toMap("00001:00001", "0.0", "00002:00001", ".0"));
        updateItems.setInItemPriceMap(UtilMisc.toMap("00001", "2799.99", "00002", "48.0"));
        updateItems.setInOverridePriceMap(new HashMap());
        runAndAssertServiceSuccess(updateItems);

        assertOrderCancelled("testCancelByUpdateOrderItems", orderId);
    }

    /**
     * Tests it is possible to update quantity after an order item has been partially shipped
     * and the inventory quantities are correct.
     * @throws GeneralException if an error occurs
     */
    public void testUpdateQuantityOfPartiallyShippedOrderItem() throws GeneralException {
        // create a product
        BigDecimal productUnitPrice = new BigDecimal("11.11");
        final GenericValue testProduct = createTestProduct("testSalesOrderParties Test Product", demowarehouse1);
        assignDefaultPrice(testProduct, productUnitPrice, admin);

        // receive 5.0 units of the product as non serialized inventory
        receiveInventoryProduct(testProduct, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);

        // get initial ATP and QOH
        Map<String, Object> callResults = getProductAvailability(testProduct.getString("productId"));
        BigDecimal initAtp = (BigDecimal) callResults.get("availableToPromiseTotal");
        BigDecimal initQoh = (BigDecimal) callResults.get("quantityOnHandTotal");
        Debug.logInfo("Initial ATP : " + initAtp + ", Initial QOH : " + initQoh, MODULE);

        // create a sales order for 5
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(testProduct, new BigDecimal("5.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId, null, "DemoAddress1");
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testUpdateQuantityOfPartiallyShippedOrderItem created order [" + orderId + "]", MODULE);

        // approve the sales order
        salesOrder.approveOrder();

        // check the tax adjustments

        // there should be one SALES_TAX OrderAdjustment from CA (6.25%) for the 5 products
        BigDecimal expectedTax = new BigDecimal("55.55").multiply(new BigDecimal("6.25")).divide(PERCENT_SCALE, SALES_TAX_CALC_DECIMALS, SALES_TAX_ROUNDING);
        checkSalesTax(orderId, "CA_BOE", 1, expectedTax);

        // pack and ship 2 out of 5
        Map<String, Map<String, BigDecimal>> toPackItems = new HashMap<String, Map<String, BigDecimal>>();
        toPackItems.put("00001", UtilMisc.toMap("00001", new BigDecimal("2.0")));
        TestShipOrderManualService ship = new TestShipOrderManualService();
        ship.setInUserLogin(admin);
        ship.setInOrderId(orderId);
        ship.setInFacilityId(facilityId);
        ship.setInItems(toPackItems);
        runAndAssertServiceSuccess(ship);

        // try to update the order item to 3, this should fail as we do not specify what to do with the adjustments
        // and there are already some adjustments billed
        OpentapsUpdateOrderItemsService updateItems = new OpentapsUpdateOrderItemsService();
        updateItems.setInUserLogin(DemoCSR);
        updateItems.setInOrderId(orderId);
        updateItems.setInItemDescriptionMap(UtilMisc.toMap("00001", "updated quantity to 3"));
        updateItems.setInItemQtyMap(UtilMisc.toMap("00001:1", "3.0"));
        updateItems.setInItemPriceMap(new HashMap<String, String>());
        updateItems.setInOverridePriceMap(new HashMap<String, String>());
        runAndAssertServiceError(updateItems);

        // update quantity of the order item to 3, specify forceComplete and NOT to recalculate the adjustments
        updateItems = new OpentapsUpdateOrderItemsService();
        updateItems.setInUserLogin(DemoCSR);
        updateItems.setInOrderId(orderId);
        updateItems.setInItemDescriptionMap(UtilMisc.toMap("00001", "updated quantity to 3"));
        updateItems.setInItemQtyMap(UtilMisc.toMap("00001:1", "3.0"));
        updateItems.setInItemPriceMap(new HashMap<String, String>());
        updateItems.setInOverridePriceMap(new HashMap<String, String>());
        updateItems.setInForceComplete("Y");
        updateItems.setInRecalcAdjustments("N");
        runAndAssertServiceSuccess(updateItems);

        // the tax should not have changed
        // there should be one SALES_TAX OrderAdjustment from CA (6.25%) for the 5 products
        expectedTax = new BigDecimal("55.55").multiply(new BigDecimal("6.25")).divide(PERCENT_SCALE, SALES_TAX_CALC_DECIMALS, SALES_TAX_ROUNDING);
        checkSalesTax(orderId, "CA_BOE", 1, expectedTax);

        // get final ATP and QOH
        callResults = getProductAvailability(testProduct.getString("productId"));
        BigDecimal finalAtp = (BigDecimal) callResults.get("availableToPromiseTotal");
        BigDecimal finalQoh = (BigDecimal) callResults.get("quantityOnHandTotal");
        Debug.logInfo("Final ATP : " + finalAtp + ", Final QOH : " + finalQoh, MODULE);

        // verify that ATP has changed by -3 and QOH has changed by -2
        assertEquals("ATP has changed by -3", finalAtp, initAtp.subtract(new BigDecimal("3.0")));
        assertEquals("QOH has changed by -2", finalQoh, initQoh.subtract(new BigDecimal("2.0")));

        updateItems = new OpentapsUpdateOrderItemsService();
        updateItems.setInUserLogin(DemoCSR);
        updateItems.setInOrderId(orderId);
        updateItems.setInItemDescriptionMap(UtilMisc.toMap("00001", "updated quantity to 3"));
        updateItems.setInItemQtyMap(UtilMisc.toMap("00001:1", "3.0"));
        updateItems.setInItemPriceMap(new HashMap<String, String>());
        updateItems.setInOverridePriceMap(new HashMap<String, String>());
        updateItems.setInForceComplete("Y");
        updateItems.setInRecalcAdjustments("Y");
        runAndAssertServiceSuccess(updateItems);

        // check the final tax adjustments
        // Note: the updateOrderItem service will not delete the previous adjustments since they are
        //  already billed

        // there should be the original SALES_TAX OrderAdjustment from CA (6.25%) for the 5 products
        // there should be the original SALES_TAX OrderAdjustment from CA (6.25%) for the 5 products negated
        // there should be the new SALES_TAX OrderAdjustment from CA (6.25%) for the 3 products
        expectedTax = new BigDecimal("33.33").multiply(new BigDecimal("6.25")).divide(PERCENT_SCALE, SALES_TAX_CALC_DECIMALS, SALES_TAX_ROUNDING);
        checkSalesTax(orderId, "CA_BOE", 3, expectedTax);

        // get final ATP and QOH
        callResults = getProductAvailability(testProduct.getString("productId"));
        finalAtp = (BigDecimal) callResults.get("availableToPromiseTotal");
        finalQoh = (BigDecimal) callResults.get("quantityOnHandTotal");
        Debug.logInfo("Final ATP : " + finalAtp + ", Final QOH : " + finalQoh, MODULE);

        // verify that ATP has changed by -3 and QOH has changed by -2 (same as before)
        assertEquals("ATP has changed by -3", finalAtp, initAtp.subtract(new BigDecimal("3.0")));
        assertEquals("QOH has changed by -2", finalQoh, initQoh.subtract(new BigDecimal("2.0")));
    }

    /**
     * Tests over reservations of inventory to an order.
     * @exception GeneralException if an error occurs
     */
    public void testInventoryOverReservation() throws GeneralException {
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(GZ1005, new BigDecimal("5.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId);
        Debug.logInfo("testInventoryOverReservation created order [" + salesOrder.getOrderId() + "]", MODULE);

        // try to reserve more inventory using various methods
        ReserveProductInventoryByFacilityService commonInput = new ReserveProductInventoryByFacilityService();
        commonInput.setInOrderId(salesOrder.getOrderId());
        commonInput.setInOrderItemSeqId("00001");
        commonInput.setInQuantity(new BigDecimal("1.0"));
        commonInput.setInUserLogin(admin);
        commonInput.setInShipGroupSeqId("00001");
        commonInput.setInProductId(GZ1005.getString("productId"));

        ReserveStoreInventoryService reserveStoreInventory = ReserveStoreInventoryService.fromInput(commonInput.inputMap());
        reserveStoreInventory.setInProductStoreId(productStoreId);
        runAndAssertServiceError(reserveStoreInventory);

        ReserveProductInventoryByFacilityService reserveProductInventoryByFacility = ReserveProductInventoryByFacilityService.fromInput(commonInput);
        reserveProductInventoryByFacility.setInFacilityId(facilityId);
        reserveProductInventoryByFacility.setInRequireInventory("N");
        runAndAssertServiceError(reserveProductInventoryByFacility);

        // cancel the reservation
        CancelOrderItemInvResQtyService cancelOrderItemInvResQty = new CancelOrderItemInvResQtyService();
        cancelOrderItemInvResQty.setInUserLogin(admin);
        cancelOrderItemInvResQty.setInOrderId(salesOrder.getOrderId());
        cancelOrderItemInvResQty.setInOrderItemSeqId("00001");
        cancelOrderItemInvResQty.setInShipGroupSeqId("00001");
        runAndAssertServiceSuccess(cancelOrderItemInvResQty);

        // reserve one item
        reserveProductInventoryByFacility = ReserveProductInventoryByFacilityService.fromInput(commonInput);
        reserveProductInventoryByFacility.setInFacilityId(facilityId);
        reserveProductInventoryByFacility.setInRequireInventory("N");
        runAndAssertServiceSuccess(reserveProductInventoryByFacility);

        // reserve 5 more items (total 6), causing an error due to over reservation of 1
        reserveProductInventoryByFacility = ReserveProductInventoryByFacilityService.fromInput(commonInput);
        reserveProductInventoryByFacility.setInFacilityId(facilityId);
        reserveProductInventoryByFacility.setInRequireInventory("N");
        reserveProductInventoryByFacility.setInQuantity(new BigDecimal("5"));
        runAndAssertServiceError(reserveProductInventoryByFacility);

        // reserve 4 more
        reserveProductInventoryByFacility = ReserveProductInventoryByFacilityService.fromInput(commonInput);
        reserveProductInventoryByFacility.setInFacilityId(facilityId);
        reserveProductInventoryByFacility.setInRequireInventory("N");
        reserveProductInventoryByFacility.setInQuantity(new BigDecimal("4"));
        runAndAssertServiceSuccess(reserveProductInventoryByFacility);

        // ensure error on any more reservations
        reserveProductInventoryByFacility = ReserveProductInventoryByFacilityService.fromInput(commonInput);
        reserveProductInventoryByFacility.setInFacilityId(facilityId);
        reserveProductInventoryByFacility.setInRequireInventory("N");
        reserveProductInventoryByFacility.setInQuantity(BigDecimal.ONE);
        runAndAssertServiceError(reserveProductInventoryByFacility);
    }

    /**
     * Tests creation of a backordered item.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPhysicalVarianceCreatesBackorderedItemNonSerial() throws GeneralException {
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);

        // 1. create test product
        GenericValue product = createTestProduct("testCreateBackorderedItem Test Product", demowarehouse1);
        String productId = product.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(product, new BigDecimal("100.0"), admin);

        // 2. get initial inventory (QOH = 0.0, ATP = 0.0)
        Map initialInventory = inventoryAsserts.getInventory(productId);

        // 3. receive 10.0 units of the product as non-serialized inventory
        Map<String, Object> inventory = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);
        String inventoryItemId = (String) inventory.get("inventoryItemId");

        // 4. check product inventory changed by +10.0 relative to initial inventory (QOH = +10.0, ATP = +10.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("10.0"), initialInventory);

        // 5. create sales order of 7x product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("7.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testCreateBackorderedItem created order [" + salesOrder.getOrderId() + "]", MODULE);

        // 6. check product QOH changed by +10.0 and ATP changed by +3.0 relative to initial inventory (QOH = +10.0, ATP = +3.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("10.0"), new BigDecimal("3.0"), initialInventory);

        // 7. Use createPhysicalInventoryVariance to change ATP and QOH of the inventoryItem from +3 by -7 and from +10 by 0
        CreatePhysicalInventoryAndVarianceService createPhysicalInventoryAndVariance = new CreatePhysicalInventoryAndVarianceService();
        createPhysicalInventoryAndVariance.setInUserLogin(demowarehouse1);
        createPhysicalInventoryAndVariance.setInInventoryItemId(inventoryItemId);
        createPhysicalInventoryAndVariance.setInAvailableToPromiseVar(new BigDecimal("-10.0"));
        createPhysicalInventoryAndVariance.setInQuantityOnHandVar(new BigDecimal("-10.0"));
        createPhysicalInventoryAndVariance.setInVarianceReasonId("VAR_DAMAGED");
        runAndAssertServiceSuccess(createPhysicalInventoryAndVariance);

        // 8. check product QOH changed by +3.0 and ATP changed by -7.0 relative to initial inventory (QOH = 0.0, ATP = -7.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("0.0"), new BigDecimal("-7.0"), initialInventory);

        // 9. check that the order item became backordered
        // TODO: replace block below with assert that OrderItem.getShortfalledQuantity() is 7.0
        EntityCondition conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItemId),
                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, new BigDecimal("7.0")),
                        EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, new BigDecimal("7.0"))
                ),
                EntityOperator.AND
        );
        long backorderedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvRes", conditionList, null);
        assertEquals(String.format("Wrong number of backordered items of product [%1$s] against order [%2$s]", productId, orderId), 1, backorderedInventoryItemsCount);
    }

    /**
     * Tests creation of a backordered item after the item's quantity has been reduced by a physical variance operation..
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPhysicalVarianceCreatesBackorderedItemSerial() throws GeneralException {
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);

        // 1. create test product
        GenericValue product = createTestProduct("testCreateBackorderedItem Test Product", demowarehouse1);
        String productId = product.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(product, new BigDecimal("100.0"), admin);

        // 2. get initial inventory (QOH = 0.0, ATP = 0.0)
        Map initialInventory = inventoryAsserts.getInventory(productId);

        // 3. receive 10.0 units of the product as non-serialized inventory
        receiveInventoryProduct(product, new BigDecimal("10.0"), "SERIALIZED_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);

        // 4. check product inventory changed by +10.0 relative to initial inventory (QOH = +10.0, ATP = +10.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("10.0"), initialInventory);

        // 5. create sales order of 7x product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("7.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testCreateBackorderedItem created order [" + salesOrder.getOrderId() + "]", MODULE);

        // 6. check product QOH changed by +10.0 and ATP changed by +3.0 relative to initial inventory (QOH = +10.0, ATP = +3.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("10.0"), new BigDecimal("3.0"), initialInventory);

        // 7. check that the order item became backordered
        EntityCondition conditionList = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, BigDecimal.ONE),
                EntityCondition.makeCondition(EntityOperator.OR,
                                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, null),
                                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, BigDecimal.ZERO))
        );
        long backorderedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvRes", conditionList, null);
        assertEquals(String.format("Wrong number of backordered items of product [%1$s] against order [%2$s]", productId, orderId), 7, backorderedInventoryItemsCount);
    }

    /**
     * Tests receive inventory against a backordered item.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testReceiveInventoryAndPhysicalVarianceAgainstBackorderedItemNonSerial() throws GeneralException {
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);

        // 1. create test product
        GenericValue product = createTestProduct("testReceiveInventoryAgainstBackorderedItem Test Product", demowarehouse1);
        String productId = product.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(product, new BigDecimal("100.0"), admin);

        // 2. get initial inventory (QOH = 0.0, ATP = 0.0)
        Map initialInventory = inventoryAsserts.getInventory(productId);

        // 3. receive 3.0 units of the product as non-serialized inventory
        Map<String, Object> inventory = receiveInventoryProduct(product, new BigDecimal("3.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);
        String inventoryItemId = (String) inventory.get("inventoryItemId");
        List<String> inventoryItemIds = UtilMisc.toList(inventoryItemId);

        // check product inventory changed by +3.0 relative to initial inventory (QOH = +3.0, ATP = +3.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("3.0"), initialInventory);

        // 4. create sales order of 10x product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("10.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testReceiveInventoryAgainstBackorderedItem created order [" + salesOrder.getOrderId() + "]", MODULE);

        // 5. check product QOH changed by 3.0 and ATP by -7.0 relative to initial inventory (QOH = +3.0, ATP = -7.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("3.0"), new BigDecimal("-7.0"), initialInventory);

        // 6. check that the order item became backordered
        // TODO: replace block below with assert that OrderItem.getShortfalledQuantity() is 7.0
        EntityCondition conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItemId),
                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, new BigDecimal("7.0")),
                        EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, new BigDecimal("10.0"))
                ),
                EntityOperator.AND
        );
        long backorderedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvRes", conditionList, null);
        assertEquals(String.format("Wrong number of backordered items of product [%1$s] against order [%2$s]", productId, orderId), 1, backorderedInventoryItemsCount);

        // 7. receive 5 units of the product as non-serialized inventory
        inventory = receiveInventoryProduct(product, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);
        inventoryItemId = (String) inventory.get("inventoryItemId");
        inventoryItemIds.add(inventoryItemId);

        // 8. use createPhysicalInventoryVariance service to increase ATP and QOH of newly received item by +5 and +5
        CreatePhysicalInventoryAndVarianceService createPhysicalInventoryAndVariance = new CreatePhysicalInventoryAndVarianceService();
        createPhysicalInventoryAndVariance.setInUserLogin(demowarehouse1);
        createPhysicalInventoryAndVariance.setInInventoryItemId(inventoryItemId);
        createPhysicalInventoryAndVariance.setInAvailableToPromiseVar(new BigDecimal("5.0"));
        createPhysicalInventoryAndVariance.setInQuantityOnHandVar(new BigDecimal("5.0"));
        createPhysicalInventoryAndVariance.setInVarianceReasonId("VAR_DAMAGED");
        runAndAssertServiceSuccess(createPhysicalInventoryAndVariance);

        // 9. check product QOH changed by +13.0 and ATP changed by +3.0 relative to initial inventory (QOH = +13.0, ATP = +3.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("13.0"), new BigDecimal("3.0"), initialInventory);

        // 10. check that the order item is no longer backordered
        // TODO: replace block below with assert that OrderItem.getShortfalledQuantity() is 0.0
        conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("inventoryItemId", EntityOperator.IN, inventoryItemIds),
                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.NOT_EQUAL, null),
                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.NOT_EQUAL, BigDecimal.ZERO)
                ),
                EntityOperator.AND
        );
        backorderedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvRes", conditionList, null);
        assertEquals(String.format("Wrong number of backordered items of product [%1$s] against order [%2$s]", productId, orderId), 0, backorderedInventoryItemsCount);
    }

    /**
     * Tests receive inventory against a backordered item.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testReceiveInventoryAndPhysicalVarianceAgainstBackorderedItemSerial() throws GeneralException {
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);

        // 1. create test product
        GenericValue product = createTestProduct("testReceiveInventoryAgainstBackorderedItem Test Product", demowarehouse1);
        String productId = product.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(product, new BigDecimal("100.0"), admin);

        // 2. get initial inventory (QOH = 0.0, ATP = 0.0)
        Map initialInventory = inventoryAsserts.getInventory(productId);

        // 3. receive 3.0 units of the product as serialized inventory
        receiveInventoryProduct(product, new BigDecimal("3.0"), "SERIALIZED_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);

        // check product inventory changed by +3.0 relative to initial inventory (QOH = +3.0, ATP = +3.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("3.0"), initialInventory);

        // 4. create sales order of 10x product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("10.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoAccount1, productStoreId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testReceiveInventoryAgainstBackorderedItem created order [" + salesOrder.getOrderId() + "]", MODULE);

        // 5. check product QOH changed by 3.0 and ATP by -7.0 relative to initial inventory (QOH = +3.0, ATP = -7.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("3.0"), new BigDecimal("-7.0"), initialInventory);

        // 6. check that the order item became backordered
        EntityCondition conditionList = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, BigDecimal.ONE),
                        EntityCondition.makeCondition(EntityOperator.OR,
                                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, null),
                                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, BigDecimal.ZERO))
        );
        long backorderedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvRes", conditionList, null);
        assertEquals(String.format("Wrong number of backordered items available of product [%1$s] against order [%2$s]", productId, orderId), 3, backorderedInventoryItemsCount);

        conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, new BigDecimal("7.0")),
                        EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, new BigDecimal("7.0"))
                ),
                EntityOperator.AND
        );
        backorderedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvRes", conditionList, null);
        assertEquals(String.format("Wrong number of backordered items non available of product [%1$s] against order [%2$s]", productId, orderId), 1, backorderedInventoryItemsCount);

        // 7. receive 5 units of the product as serialized inventory
        receiveInventoryProduct(product, new BigDecimal("5.0"), "SERIALIZED_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);

        // 8. check product QOH changed by +8.0 and ATP changed by -2.0 relative to initial inventory (QOH = +8.0, ATP = -2.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("8.0"), new BigDecimal("-2.0"), initialInventory);

        // 9. check that the order item is no longer backordered
        conditionList = EntityCondition.makeCondition(EntityOperator.AND,
                          EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                          EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, BigDecimal.ONE),
                          EntityCondition.makeCondition(EntityOperator.OR,
                                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, null),
                                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, BigDecimal.ZERO))
        );
        long nonBackorderedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvRes", conditionList, null);
        assertEquals(String.format("Wrong number of nonbackordered items of product [%1$s] against order [%2$s]", productId, orderId), 8, nonBackorderedInventoryItemsCount);

        conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, new BigDecimal("2.0")),
                        EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, new BigDecimal("2.0"))
                ),
                EntityOperator.AND
        );
        backorderedInventoryItemsCount = delegator.findCountByCondition("OrderItemShipGrpInvRes", conditionList, null);
        assertEquals(String.format("Wrong number of backordered items non available of product [%1$s] against order [%2$s]", productId, orderId), 1, backorderedInventoryItemsCount);

    }

    /**
     * Test the invoicing of orders for services
     * 1.  Create product A of product type "SERVICE" with price 10.0
     * 2.  Create second product B of product type "SERVICE with price 20.0
     * 3.  Create product C of type "FINISHED_GOOD" with price 30.0
     * 4.  Create a customer with an address in California
     * 5.  Create a sales order of 1.0 Product A, 2.0 Product B, and 3.0 Product C
     * 6.  Approve sales order
     * 7.  Cancel order item for product A
     * 8.  Set order item for product B to ITEM_PERFORMED
     * 9.  Call opentaps.invoiceNonPhysicalOrderItems for this order
     * 10.  Verify that one sales invoice has been created for the customer, and there is an invoice item for quantity 2.0 of Product B with price 20.0 and no invoice items for A or C
     * 11.  Ship product C from the order and set shipment to Packed
     * 12.  Verify that order items for B and C are now both ITEM_COMPLETED, and the order's status is ORDER_COMPLETED8
     * @exception GeneralException if an error occurs
     */
    public void testServiceInvoicing() throws GeneralException {
        GenericValue serviceA = createTestProduct("Service Product A", "SERVICE", admin);
        assignDefaultPrice(serviceA, new BigDecimal("10.0"), admin);

        GenericValue serviceB = createTestProduct("Service Product B", "SERVICE", admin);
        assignDefaultPrice(serviceB, new BigDecimal("20.0"), admin);

        GenericValue product = createTestProduct("Product", admin);
        assignDefaultPrice(product, new BigDecimal("30.0"), admin);

        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(serviceA, new BigDecimal("1.0"));
        orderItems.put(serviceB, new BigDecimal("2.0"));
        orderItems.put(product, new BigDecimal("3.0"));
        SalesOrderFactory order = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        String orderId = order.getOrderId();

        assertTrue("Cancel serviceA", order.cancelProduct(serviceA, new BigDecimal("1.0")));

        // perform service B
        GenericValue serviceItemB = EntityUtil.getFirst(delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId, "productId", serviceB.get("productId"))));
        assertNotNull(serviceItemB);
        ChangeOrderItemStatusService changeOrderItemStatus = new ChangeOrderItemStatusService();
        changeOrderItemStatus.setInUserLogin(admin);
        changeOrderItemStatus.setInOrderId(orderId);
        changeOrderItemStatus.setInOrderItemSeqId(serviceItemB.getString("orderItemSeqId"));
        changeOrderItemStatus.setInStatusId("ITEM_PERFORMED");
        runAndAssertServiceSuccess(changeOrderItemStatus);

        // invoice the order
        OpentapsInvoiceNonPhysicalOrderItemsService invoiceNonPhysicalOrderItems = new OpentapsInvoiceNonPhysicalOrderItemsService();
        invoiceNonPhysicalOrderItems.setInUserLogin(admin);
        invoiceNonPhysicalOrderItems.setInOrderId(orderId);
        runAndAssertServiceSuccess(invoiceNonPhysicalOrderItems);
        String invoiceId = invoiceNonPhysicalOrderItems.getOutInvoiceId();
        assertNotNull(invoiceId);

        // use our invoice domain
        InvoiceRepositoryInterface repository = getInvoiceRepository(admin);
        Invoice invoice = repository.getInvoiceById(invoiceId);
        assertNotNull(invoice);
        for (InvoiceItem invoiceItem : invoice.getInvoiceItems()) {
            if (!"INV_FPROD_ITEM".equals(invoiceItem)) {
                continue;
            }
            String productId = invoiceItem.getString("productId");
            assertTrue("Invoice is only for service B", productId.equals(serviceB.get("productId")));
            assertEquals("Invoice is for 2 of service B", 2.0, invoiceItem.getBigDecimal("quantity"));
            assertEquals("Service B is invoiced at $20 unit cost", 20.0, invoiceItem.getBigDecimal("amount"));
        }

        // ship the order and ensure header and items are completed
        TestShipOrderService ship = new TestShipOrderService();
        ship.setInUserLogin(demowarehouse1);
        ship.setInOrderId(orderId);
        ship.setInFacilityId(facilityId);
        runAndAssertServiceSuccess(ship);

        assertOrderCompleted(orderId);
    }

    /**
     * Checks that the service <code>testShipOrder</code> fails for an order for the given party.
     *
     * @param partyId a <code>String</code> value
     * @exception GeneralException if an error occurs
     */
    public void performDoNotShipTest(String partyId) throws GeneralException {

        // find a party with given id
        GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));

        // place an order
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(GZ1005, new BigDecimal("1.0"));
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, party, productStoreId);
        Debug.logInfo("testDontShipOrder has created order [" + salesOrder.getOrderId() + "]", MODULE);

        // try ship test order
        TestShipOrderService ship = new TestShipOrderService();
        ship.setInUserLogin(demowarehouse1);
        ship.setInOrderId(salesOrder.getOrderId());
        ship.setInFacilityId(facilityId);
        runAndAssertServiceError(ship);
    }

    /**
     * Test split shipment feature:
     * 1.  Create a product
     * 2.  Receive 100 units of product into stock
     * 3.  Create an order for 100 units of the product.
     * 4.  Approve order
     * 5.  Ship 25 units
     * 6.  Cancel 25 units
     * 7.  Get the ATP/QOH inventory of the product
     * 8.  Try to split the order and put 70 of the product into a new ship group -- it should fial
     * 9.  Split 20 units of the product into a new ship group
     * 10.  Get ATP/QOH inventory of the product
     * 11.  Verify that there are 2 ship groups with 30 and 20
     * 12.  Verify that ATP/QOH of product have not changed from (7) and (10)
     * @exception GeneralException if an error occurs
     */
    public void testSplitShipment() throws GeneralException {

        // 1.  Create a product
        GenericValue testProduct = createTestProduct("Product for split shipment Test", demowarehouse1);
        testProduct.getString("productId");
        assignDefaultPrice(testProduct, new BigDecimal("50.0"), admin);

        // 2.  Receive 100 units of product into stock
        Map<String, Object> inventory = receiveInventoryProduct(testProduct, new BigDecimal("100.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);
        String inventoryItemId = (String) inventory.get("inventoryItemId");

        // 3.  Create an order for 100 units of the product.
        // 4.  Approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(testProduct, new BigDecimal("100.0"));
        User = DemoCSR;
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        String orderId = orderFactory.getOrderId();
        Debug.logInfo("testSplitShipment created order [" + orderId + "]", MODULE);

        // 5.  Ship 25 units
        QuickShipOrderByItemService quickShipOrderByItem = new QuickShipOrderByItemService();
        quickShipOrderByItem.setInUserLogin(admin);
        quickShipOrderByItem.setInOrderId(orderId);
        quickShipOrderByItem.setInShipGroupSeqId("00001");
        quickShipOrderByItem.setInItemShipList(UtilMisc.toList(UtilMisc.<String, Object>toMap("orderItemSeqId", "00001", "inventoryItemId", inventoryItemId, "qtyShipped", new BigDecimal("25.0"))));
        quickShipOrderByItem.setInOriginFacilityId(facilityId);
        runAndAssertServiceSuccess(quickShipOrderByItem);

        // 6.  Cancel 25 units
        CancelOrderItemService cancelItem = new CancelOrderItemService();
        cancelItem.setInUserLogin(DemoCSR);
        cancelItem.setInOrderId(orderId);
        cancelItem.setInOrderItemSeqId("00001");
        cancelItem.setInShipGroupSeqId("00001");
        cancelItem.setInCancelQuantity(new BigDecimal("25.0"));
        runAndAssertServiceSuccess(cancelItem);

        // 7.  Get the ATP/QOH inventory of the product
        Map<String, Object> productAvailability = getProductAvailability(testProduct.getString("productId"));
        BigDecimal initatp = (BigDecimal) productAvailability.get("availableToPromiseTotal");
        BigDecimal initqoh = (BigDecimal) productAvailability.get("quantityOnHandTotal");

        // 8.  Try to split the order and put 70 of the product into a new ship group -- it should fail
        assertSplitOrderError(DemoCSR, orderId, "70.0");

        // 9.  Split 20 units of the product into a new ship group
        assertSplitOrderSuccess(DemoCSR, orderId, "20.0");

        // 10.  Get ATP/QOH inventory of the product
        productAvailability = getProductAvailability(testProduct.getString("productId"));
        BigDecimal finatp = (BigDecimal) productAvailability.get("availableToPromiseTotal");
        BigDecimal finqoh = (BigDecimal) productAvailability.get("quantityOnHandTotal");

        // 11.  Verify that there are 2 ship groups reservations with 30 and 20
        assertShipGroupReservationsQuantities(orderId, UtilMisc.toList("00001", "00002"), UtilMisc.toList(new BigDecimal("30.0"), new BigDecimal("20.0")));

        // 12.  Verify that ATP/QOH of product have not changed from (7) and (10)
        assertEquals("ATP should not have changed during ship group splitting", initatp, finatp);
        assertEquals("QOH should not have changed during ship group splitting", initqoh, finqoh);
    }

    /**
     * Test updating quantities in split orders:
     * 1.  Create a product
     * 2.  Receive 100 units of product into stock
     * 3.  Create an order for 100 units of the product.
     * 4.  Approve order
     * 5.  Split 20 units of the product into a new ship group, and check ship group quantities
     * 6.  Update second shipment quantity to 50, and check ship group quantities
     * 7.  Split 20 units of the product from the first sg into a third ship group
     * 8.  Check the OrderItem quantity is now 130, and the OrderItemShipGroupAssoc have correct quantities
     * 9.  Cancel the third ship group
     * 10. Check the OrderItemShipGroupAssoc have correct quantities and cancelQuantities
     * 11. Check the OrderItem quantity did not change, its cancelQuantity is 20, and status still approved
     * 12. Update the first ship group quantity to 200
     * 13. Check the OrderItem quantity is now 270, and its cancelQuantity is still 20
     * 14. Cancel the first ship group
     * 15. Check the OrderItem quantity is still 270, and its cancelQuantity is now 270, and the status is Canceled
     * - after cancelling, also check that the OrderItemShipGroup entities are still present and valid
     * - during the test also check that OrderItem and OrderItemShipGroupAssoc are consistent
     * @exception GeneralException if an error occurs
     */
    public void testUpdateSplitShipment() throws GeneralException {
        // 1.  Create a product
        GenericValue testProduct = createTestProduct("Product for update split shipment Test", demowarehouse1);
        testProduct.getString("productId");
        assignDefaultPrice(testProduct, new BigDecimal("50.0"), admin);

        // 2.  Receive 100 units of product into stock
        Map<String, Object> inventory = receiveInventoryProduct(testProduct, new BigDecimal("100.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);
        inventory.get("inventoryItemId");

        // 3.  Create an order for 100 units of the product.
        // 4.  Approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(testProduct, new BigDecimal("100.0"));
        User = DemoCSR;
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        String orderId = orderFactory.getOrderId();
        Debug.logInfo("testUpdateSplitShipment created order [" + orderId + "]", MODULE);
        assertOrderItemQuantity(orderId, "00001", new BigDecimal("100.0"), null);
        assertShipGroupValid(orderId, "00001");

        // 5.  Split 20 units of the product into a new ship group
        assertSplitOrderSuccess(DemoCSR, orderId, "20.0");
        assertShipGroupAssocsQuantities(orderId, UtilMisc.toList("00001", "00002"), UtilMisc.toList(new BigDecimal("80.0"), new BigDecimal("20.0")));
        assertOrderItemQuantity(orderId, "00001", new BigDecimal("100.0"), null);
        assertShipGroupValid(orderId, "00002");

        // 6.  Update second shipment quantity to 50
        assertUpdateOrderItemSuccess(DemoCSR, orderId, "00001", UtilMisc.toList("00001", "00002"), UtilMisc.toList(new BigDecimal("80.0"), new BigDecimal("50.0")));
        assertShipGroupAssocsQuantities(orderId, UtilMisc.toList("00001", "00002"), UtilMisc.toList(new BigDecimal("80.0"), new BigDecimal("50.0")));
        assertOrderItemQuantity(orderId, "00001", new BigDecimal("130.0"), null);
        assertOrderItemsHaveShipGroupAssoc(orderId);

        // 7.  Split 20 units of the product from the first sg into a third ship group
        assertSplitOrderSuccess(DemoCSR, orderId, "20.0");
        assertOrderItemsHaveShipGroupAssoc(orderId);
        assertShipGroupValid(orderId, "00003");

        // 8.  Check the OrderItem quantity is now 130, and the OrderItemShipGroupAssoc have correct quantities
        assertOrderItemQuantity(orderId, "00001", new BigDecimal("130.0"), null);
        assertShipGroupAssocsQuantities(orderId, UtilMisc.toList("00001", "00002", "00003"), UtilMisc.toList(new BigDecimal("60.0"), new BigDecimal("50.0"), new BigDecimal("20.0")));
        assertOrderItemsHaveShipGroupAssoc(orderId);

        // 9.  Cancel the third ship group
        Debug.logInfo("Cancelling third ship group ......", MODULE);
        assertUpdateOrderItemSuccess(DemoCSR, orderId, "00001", UtilMisc.toList("00001", "00002", "00003"), UtilMisc.toList(new BigDecimal("60.0"), new BigDecimal("50.0"), new BigDecimal("0.0")));
        assertOrderItemsHaveShipGroupAssoc(orderId);
        assertShipGroupValid(orderId);

        // 10. Check the OrderItemShipGroupAssoc have correct quantities and cancelQuantities
        assertShipGroupAssocsQuantities(orderId, UtilMisc.toList("00001", "00002", "00003"), UtilMisc.toList(new BigDecimal("60.0"), new BigDecimal("50.0"), new BigDecimal("20.0")), UtilMisc.toList(new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("20.0")));

        // 11. Check the OrderItem quantity did not change, its cancelQuantity is 20, and status still approved
        assertOrderItemQuantity(orderId, "00001", new BigDecimal("130.0"), new BigDecimal("20.0"));
        assertOrderItemStatus("Order item should still be approved after cancelling third ship groups", orderId, "00001", "ITEM_APPROVED");

        // 12. Update the first ship group quantity to 200
        assertUpdateOrderItemSuccess(DemoCSR, orderId, "00001", UtilMisc.toList("00001", "00002"), UtilMisc.toList(new BigDecimal("200.0"), new BigDecimal("50.0")));
        assertOrderItemsHaveShipGroupAssoc(orderId);

        // 13. Check the OrderItem quantity is now 270, and its cancelQuantity is still 20
        assertOrderItemQuantity(orderId, "00001", new BigDecimal("270.0"), new BigDecimal("20.0"));

        // 14. Cancel the first and second ship groups
        Debug.logInfo("Cancelling last two ship groups ......", MODULE);
        assertUpdateOrderItemSuccess(DemoCSR, orderId, "00001", UtilMisc.toList("00001", "00002"), UtilMisc.toList(new BigDecimal("0.0"), new BigDecimal("0.0")));
        assertShipGroupValid(orderId);

        // 15. Check the OrderItem quantity is still 270, and its cancelQuantity is now 270, and the status is Canceled
        assertShipGroupAssocsQuantities(orderId, UtilMisc.toList("00001", "00002", "00003"), UtilMisc.toList(new BigDecimal("200.0"), new BigDecimal("50.0"), new BigDecimal("20.0")), UtilMisc.toList(new BigDecimal("200.0"), new BigDecimal("50.0"), new BigDecimal("20.0")));
        assertOrderItemsHaveShipGroupAssoc(orderId);
        assertOrderItemQuantity(orderId, "00001", new BigDecimal("270.0"), new BigDecimal("270.0"));
        assertOrderItemStatus("Order item should be cancelled after cancelling all ship groups", orderId, "00001", "ITEM_CANCELLED");
        assertOrderCancelled("Order should be cancelled after cancelling all items", orderId);
    }

    /**
     * Test cancelling an order
     * 1.  Create a test product
     * 2.  Receive 100 units of product into stock
     * 3.  Create an order for 100 units of the product.
     * 4.  Approve order
     * 5.  Update the order item qty to generate a promo item
     * 6.  Cancel the order item by setting its qty to 0
     * 7.  Check the order ship group, status and quantities
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCancelOrderWithPromoItems() throws GeneralException {
        // 1.  Create a product
        GenericValue testProduct = createTestProduct("Product for cancel order with promo items Test", demowarehouse1);
        testProduct.getString("productId");
        assignDefaultPrice(testProduct, new BigDecimal("50.0"), admin);

        // 2.  Receive 100 units of product into stock
        Map<String, Object> inventory = receiveInventoryProduct(testProduct, new BigDecimal("100.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);
        inventory.get("inventoryItemId");

        // 3.  Create an order for 100 units of the product.
        // 4.  Approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(testProduct, new BigDecimal("1000.0"));
        User = DemoCSR;
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        String orderId = orderFactory.getOrderId();
        orderFactory.approveOrder();
        Debug.logInfo("testCancelOrderWithPromoItems created order [" + orderId + "]", MODULE);
        assertOrderItemQuantity(orderId, "00001", new BigDecimal("1000.0"), null);
        assertShipGroupValid(orderId, "00001");

        // 5. Update shipment quantity to 100 (this should generate promo items)
        assertUpdateOrderItemSuccess(DemoCSR, orderId, "00001", UtilMisc.toList("00001"), UtilMisc.toList(new BigDecimal("100.0")));
        assertShipGroupAssocsQuantities(orderId, UtilMisc.toList("00001"), UtilMisc.toList(new BigDecimal("100.0")));
        assertOrderItemQuantity(orderId, "00001", new BigDecimal("100.0"), null);
        assertOrderItemsHaveShipGroupAssoc(orderId);

        // 6. Cancel the ship group (manullay building the whole map to match what the web interface would send)
        Debug.logInfo("testCancelOrderWithPromoItems Cancelling ship group ......", MODULE);
        OpentapsUpdateOrderItemsService updateOrderItems = new OpentapsUpdateOrderItemsService();
        updateOrderItems.setInUserLogin(DemoCSR);
        updateOrderItems.setInOrderId(orderId);
        updateOrderItems.setInOverridePriceMap(new HashMap());
        updateOrderItems.setInItemDescriptionMap(UtilMisc.toMap("00001", "Product for cancel order with promo items Test", "00002", "Micro Chrome Widget"));
        updateOrderItems.setInItemPriceMap(UtilMisc.toMap("00001", "50.0", "00002", "59.99"));
        updateOrderItems.setInItemQtyMap(UtilMisc.toMap("00001:00001", "0", "00002:00001", "1"));
        runAndAssertServiceSuccess(updateOrderItems);

        // 7. Check the order ship group, status and quantities
        assertShipGroupValid(orderId);
        assertOrderItemQuantity(orderId, "00001", new BigDecimal("100.0"), new BigDecimal("100.0"));
        assertOrderItemStatus("Order item should be cancelled", orderId, "00001", "ITEM_CANCELLED");
        assertOrderCancelled("Order should be cancelled", orderId);
    }


    /**
     * This test verifies the process of creating an order, then using Receive Payment screen in CRMSFA to receive manual payments,
     * and then when the order is shipped, that the invoice is PAID and the customer's accounts receivable balances are correct.
     * 1.  check the customer balance
     * 2.  Create a product
     * 3.  Receive 10 units of product into stock
     * 4.  Create an order for 20 units of the product.
     * 5.  Approve order
     * 6.  Receive a check and a credit card payment for $100 and the balance of the order from the customer and set both to received
     *     this simulates the receive payments screen from order view
     * 7.  Ship the order
     * 8.  verify that the invoice is PAID
     * 9.  verify that the customer balance has not changed
     * @exception GeneralException if an error occurs
     */
    public void testOrderPayments() throws GeneralException {
        String customerPartyId = createPartyFromTemplate(DemoAccount1.getString("partyId"), "Account for testOrderPayments");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));

        // 1.  check the customer balance
        BigDecimal initialCustomerBalance = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);

        // Next 6 steps are performed in this method
        Order order = createTestOrderWithPayments(customer, "Product for order payments test");

        // 8.  verify that the invoice is PAID
        List<Invoice> invoices = order.getInvoices();
        for (Invoice invoice : invoices) {
            assertTrue("Invoice [" + invoice.getInvoiceId() + "] from order [" + order.getOrderId() + "] should be PAID", invoiceRepository.getInvoiceSpecification().isPaid(invoice));
        }

        // 9.  verify that the customer balance has not changed
        BigDecimal finalCustomerBalance = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer balance has not changed", (finalCustomerBalance.subtract(initialCustomerBalance)).setScale(DECIMALS, ROUNDING), BigDecimal.ZERO);

    }

    /**
     * Same as testOrderPayments, but with ProductStore.autoApproveInvoice set to N.
     * Then, each invoice is manually set to READY, and we verify that it is then automatically set to PAID.
     * @throws GeneralException if an error occurs
     */
    public void testOrderPaymentsWithNoAutoApproveInvoice() throws GeneralException {
        // set ProductStore.autoApproveInvoice=N
        setProductStoreAutoApproveInvoice(productStoreId, "N", delegator);

        String customerPartyId = createPartyFromTemplate(DemoAccount1.getString("partyId"), "Account for testOrderPaymentsWithNoApproveInvoice");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));

        // 1.  check the customer balance
        BigDecimal initialCustomerBalance = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);

        // Next 6 steps are performed in this method
        Order order = createTestOrderWithPayments(customer, "Product for order payments test with no auto approve invoice");

        // 8.  Manually set each invoice to READY, and verify that the invoice is automatically set to PAID
        List<Invoice> invoices = order.getInvoices();
        for (Invoice invoice : invoices) {
            // this special method will check for payments applied to invoice and mark it after it has been PAID
            // otherwise, setting invoice to READY won't do it.
            SetInvoiceReadyAndCheckIfPaidService setInvoiceReadyAndCheckIfPaid = new SetInvoiceReadyAndCheckIfPaidService();
            setInvoiceReadyAndCheckIfPaid.setInUserLogin(admin);
            setInvoiceReadyAndCheckIfPaid.setInInvoiceId(invoice.getInvoiceId());
            runAndAssertServiceSuccess(setInvoiceReadyAndCheckIfPaid);
        }

        for (Invoice invoice : invoices) {
            // reload the invoice object to get the new status
            invoice = invoiceRepository.getInvoiceById(invoice.getInvoiceId());
            assertTrue("Invoice [" + invoice.getInvoiceId() + "] from order [" + order.getOrderId() + "] should be PAID", invoiceRepository.getInvoiceSpecification().isPaid(invoice));
        }
        // 9.  verify that the customer balance has not changed
        BigDecimal finalCustomerBalance = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer balance has not changed", (finalCustomerBalance.subtract(initialCustomerBalance)).setScale(DECIMALS, ROUNDING), BigDecimal.ZERO);
    }

    /**
     * Ensures that inventory gets reserved from the facility configured for an address in ProductStoreFacilityByAddress.
     * Also checks that when the address is updated, the behavior is preserved.
     * @throws GeneralException if an error occurs
     */
    public void testProductStoreFacilityByAddress() throws GeneralException {
        String partyId = createPartyFromTemplate(DemoCustomer.getString("partyId"), "Customer for testProductStoreFacilityByAddress");

        // create a product for our use (should be 0 products in inventory)
        GenericValue testProduct = createTestProduct("Product for testProductStoreFacilityByAddress", demowarehouse1);
        assignDefaultPrice(testProduct, new BigDecimal("50.0"), admin);

        // create a new shipping address
        CreatePartyPostalAddressService createPartyPostalAddress = new CreatePartyPostalAddressService();
        createPartyPostalAddress.setInUserLogin(admin);
        createPartyPostalAddress.setInPartyId(partyId);
        createPartyPostalAddress.setInToName("Shipping Address");
        createPartyPostalAddress.setInAddress1("Test Street Version 1");
        createPartyPostalAddress.setInCity("New York");
        createPartyPostalAddress.setInPostalCode("10001");
        createPartyPostalAddress.setInCountryGeoId("USA");
        createPartyPostalAddress.setInStateProvinceGeoId("NY");
        createPartyPostalAddress.setInContactMechPurposeTypeId("SHIPPING_LOCATION");
        runAndAssertServiceSuccess(createPartyPostalAddress);
        String contactMechId = createPartyPostalAddress.getOutContactMechId();

        // store it manually in ProductStoreFacilityByAddress, link it to my retail store warehouse
        GenericValue facilityAddress = delegator.makeValue("ProductStoreFacilityByAddress");
        facilityAddress.put("facilityId", "MyRetailStore");
        facilityAddress.put("productStoreId", productStoreId);
        facilityAddress.put("contactMechId", contactMechId);
        facilityAddress.create();

        // Create a sales order for 2 of our product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(testProduct, new BigDecimal("2.0"));
        User = DemoCSR;
        testCreatesSalesOrder(order, partyId, productStoreId, "EXT_OFFLINE", null, contactMechId);

        // verify the ATP went down by 2 for this product in my retail store warehouse
        GetInventoryAvailableByFacilityService getInventoryAvailableByFacility = new GetInventoryAvailableByFacilityService();
        getInventoryAvailableByFacility.setInProductId(testProduct.getString("productId"));
        getInventoryAvailableByFacility.setInFacilityId("MyRetailStore");
        runAndAssertServiceSuccess(getInventoryAvailableByFacility);
        BigDecimal atp = getInventoryAvailableByFacility.getOutAvailableToPromiseTotal();
        assertNotNull("ATP of product is not null", atp);
        assertEquals("ATP of product went down by expected amount in MyRetailStore", -2.0, atp);

        // update the address and repeat test (we'll use updatePostalAddress since that's the core service called by all update address services)
        UpdatePostalAddressService updatePostalAddress = new UpdatePostalAddressService();
        updatePostalAddress.setInUserLogin(admin);
        updatePostalAddress.setInAddress1("Test Street Version 2");
        updatePostalAddress.setInCity("New York");
        updatePostalAddress.setInPostalCode("10002");
        updatePostalAddress.setInContactMechId(contactMechId);
        runAndAssertServiceSuccess(updatePostalAddress);
        String oldContactMechId = updatePostalAddress.getOutOldContactMechId();
        String newContactMechId = updatePostalAddress.getOutContactMechId();

        // just make sure the service works as intended
        assertEquals("Updating postal address changes correct contactMechId", contactMechId, oldContactMechId);
        assertNotEquals("Updating postal address creates a new contactMechId", contactMechId, newContactMechId);

        // now check if the SECA to copy ProductStoreFacilityByAddress was triggered
        GenericValue facilityAddress2 = delegator.findByPrimaryKey("ProductStoreFacilityByAddress", UtilMisc.toMap("productStoreId", productStoreId, "contactMechId", newContactMechId));
        assertNotNull("SECA to copy ProductStoreFacilityByAddress when address is updated did what was expected", facilityAddress2);

        // create another order for this new address and ensure retail facility atp went down another 2
        order = new HashMap<GenericValue, BigDecimal>();
        order.put(testProduct, new BigDecimal("2.0"));
        User = DemoCSR;
        testCreatesSalesOrder(order, partyId, productStoreId, "EXT_OFFLINE", null, contactMechId);
        getInventoryAvailableByFacility = new GetInventoryAvailableByFacilityService();
        getInventoryAvailableByFacility.setInProductId(testProduct.getString("productId"));
        getInventoryAvailableByFacility.setInFacilityId("MyRetailStore");
        runAndAssertServiceSuccess(getInventoryAvailableByFacility);
        atp = getInventoryAvailableByFacility.getOutAvailableToPromiseTotal();
        assertNotNull("ATP of product is not null", atp);
        assertEquals("ATP of product went down by expected amount in MyRetailStore", -4.0, atp);
    }

    /**
     * Common method for doing most of the work for testOrderPayments_ methods.
     * @param customer the order customer
     * @param productName name of the test product to create for the order
     * @return an <code>Order</code> domain object
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private Order createTestOrderWithPayments(GenericValue customer, String productName) throws GeneralException {
        String customerPartyId = customer.getString("partyId");

        // 1.  Create a product
        GenericValue testProduct = createTestProduct(productName, admin);
        testProduct.getString("productId");
        assignDefaultPrice(testProduct, new BigDecimal("10.0"), admin);

        // 2.  Receive 10 units of product into stock
        receiveInventoryProduct(testProduct, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), admin);

        // 3.  Create an order for 20 units of the product.
        // 4.  Approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(testProduct, new BigDecimal("20.0"));
        User = DemoCSR;
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, customer, productStoreId);
        String orderId = orderFactory.getOrderId();
        Debug.logInfo("testOrderPayments created order [" + orderId + "]", MODULE);
        orderFactory.approveOrder();

        // 6.  Receive a check and a credit card payment for $100 and the balance of the order from the customer and set both to received
        // this simulates the receive payments screen from order view
        Map oppParams = UtilMisc.toMap("orderId", orderId, "maxAmount", new BigDecimal("100.0"), "statusId", "PAYMENT_RECEIVED", "createdDate", UtilDateTime.nowTimestamp(), "createdByUserLogin", admin.get("userLoginId"));
        String paymentPref1 = delegator.getNextSeqId("OrderPaymentPreference");
        oppParams.put("orderPaymentPreferenceId", paymentPref1);
        oppParams.put("paymentMethodTypeId", "CREDIT_CARD");
        delegator.create("OrderPaymentPreference", oppParams);

        CreatePaymentFromPreferenceService createPaymentFromPreference = new CreatePaymentFromPreferenceService();
        createPaymentFromPreference.setInUserLogin(admin);
        createPaymentFromPreference.setInOrderPaymentPreferenceId(paymentPref1);
        createPaymentFromPreference.setInPaymentFromId(customerPartyId);
        createPaymentFromPreference.setInPaymentRefNum(customerPartyId + "-1");
        createPaymentFromPreference.setInComments("Simulated manually received payment");
        runAndAssertServiceSuccess(createPaymentFromPreference);

        String paymentPref2 = delegator.getNextSeqId("OrderPaymentPreference");
        oppParams.put("orderPaymentPreferenceId", paymentPref2);
        oppParams.put("paymentMethodTypeId", "PERSONAL_CHECK");
        oppParams.put("maxAmount", orderFactory.getGrandTotal().subtract(new BigDecimal("100")));
        delegator.create("OrderPaymentPreference", oppParams);

        createPaymentFromPreference = new CreatePaymentFromPreferenceService();
        createPaymentFromPreference.setInUserLogin(admin);
        createPaymentFromPreference.setInOrderPaymentPreferenceId(paymentPref2);
        createPaymentFromPreference.setInPaymentFromId(customerPartyId);
        createPaymentFromPreference.setInPaymentRefNum(customerPartyId + "-2");
        createPaymentFromPreference.setInComments("Simulated manually received payment");
        runAndAssertServiceSuccess(createPaymentFromPreference);

        // 7.  Ship the order
        TestShipOrderService ship = new TestShipOrderService();
        ship.setInUserLogin(demowarehouse1);
        ship.setInOrderId(orderId);
        ship.setInFacilityId(facilityId);
        runAndAssertServiceSuccess(ship);

        assertOrderCompleted(orderId);

        Order order = orderRepository.getOrderById(orderId);
        return order;
    }

    /**
     * This test verifies LIFO received set at the ProductStore will cause the inventory reservation to be
     * reserved in LIFO order.
     * @throws GeneralException in an error occurs
     */
    public void testReserveInventoryForStoreLIFOReceived() throws GeneralException {

        // set ProductStore's reserveOrderEnumId to INVRO_LIFO_REC
        setProductStoreInventoryReservationEnum(productStoreId, "INVRO_LIFO_REC", delegator);

        // create a product
        GenericValue product = createTestProduct("testReserveInventoryForStoreLIFOReceived Test Product", demowarehouse1);

        // receive 10 into the warehouse and store the inventoryItemId as inventoryItemId1
        Map<String, Object> results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), demowarehouse1);
        String inventoryItemId1 = (String) results.get("inventoryItemId");

        pause("Workaround pause for MySQL");
        // receive another 10 into the warehouse and store the inventoryItemId as inventoryItemId2
        results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), demowarehouse1);
        String inventoryItemId2 = (String) results.get("inventoryItemId");

        // create a sales order for 15
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(product, new BigDecimal("15.0"));
        testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);

        // verify that:
        //   inventoryItemId1 ATP = 5, QOH = 10
        assertInventoryItemQuantities("Wrong inventory", inventoryItemId1, 5, 10);

        //   inventoryItemId2 ATP = 0, QOH =10
        assertInventoryItemQuantities("Wrong inventory", inventoryItemId2, 0, 10);
    }


    /**
     * This test verifies FIFO received set at the ProductStore will cause the inventory reservation to be
     * reserved in FIFO order.
     * @throws GeneralException in an error occurs
     */
    public void testReserveInventoryForStoreFIFOReceived() throws GeneralException {

        // set ProductStore's reserveOrderEnumId to INVRO_FIFO_REC
        setProductStoreInventoryReservationEnum(productStoreId, "INVRO_FIFO_REC", delegator);

        // create a product
        GenericValue product = createTestProduct("testReserveInventoryForStoreFIFOReceived Test Product", demowarehouse1);

        // receive 10 into the warehouse and store the inventoryItemId as inventoryItemId1
        Map<String, Object> results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.1"), demowarehouse1);
        String inventoryItemId1 = (String) results.get("inventoryItemId");

        // we need to take a pause so that the two inventory items have different received daytime timestamps even in mysql
        // which does not record timestamps with subsecond precision
        pause("Workaround pause for MySQL");

        // receive another 10 into the warehouse and store the inventoryItemId as inventoryItemId2
        results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("2.2"), demowarehouse1);
        String inventoryItemId2 = (String) results.get("inventoryItemId");

        // create a sales order for 15
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(product, new BigDecimal("15.0"));

        SalesOrderFactory sof = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        sof.getOrderId();

        // verify that:
        //   inventoryItemId1 ATP = 0, QOH = 10
        assertInventoryItemQuantities("Wrong inventory", inventoryItemId1, 0, 10);

        //   inventoryItemId2 ATP = 5, QOH = 10
        assertInventoryItemQuantities("Wrong inventory", inventoryItemId2, 5, 10);
    }

    /**
     * This test verifies inventory reservation sequence set at the Facility will override the ProductStore, and that
     * will Facility level LIFO setting will cause the inventory to be reserved in LIFO order.
     * @throws GeneralException in an error occurs
     */
    public void testReserveInventoryForFacilityLIFOReceived() throws GeneralException {

        // set ProductStore's reserveOrderEnumId to INVRO_FIFO_REC
        setProductStoreInventoryReservationEnum(productStoreId, "INVRO_FIFO_REC", delegator);

        // set Facility's reserveOrderEnumId to INVRO_LIFO_REC
        setFacilityInventoryReservationEnum(facilityId, "INVRO_LIFO_REC", delegator);

        // create a product
        GenericValue product = createTestProduct("testReserveInventoryForFacilityLIFOReceived Test Product", demowarehouse1);

        // receive 10 into the warehouse and store the inventoryItemId as inventoryItemId1
        Map<String, Object> results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), demowarehouse1);
        String inventoryItemId1 = (String) results.get("inventoryItemId");

        pause("Workaround pause for MySQL");
        // receive another 10 into the warehouse and store the inventoryItemId as inventoryItemId2
        results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), demowarehouse1);
        String inventoryItemId2 = (String) results.get("inventoryItemId");

        // create a sales order for 15
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(product, new BigDecimal("15.0"));
        testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);

        // verify that:
        //   inventoryItemId1 ATP = 5, QOH = 10
        assertInventoryItemQuantities(inventoryItemId1, 5, 10);

        //   inventoryItemId2 ATP = 0, QOH =10
        assertInventoryItemQuantities(inventoryItemId2, 0, 10);

        // set ProductStore's reserveOrderEnumId back to INVRO_FIFO_REC
        setProductStoreInventoryReservationEnum(productStoreId, "INVRO_FIFO_REC", delegator);
    }

    /**
     * This test verifies inventory reservation sequence set at the Facility will override the ProductStore, and that
     * will Facility level FIFO setting will cause the inventory to be reserved in FIFO order.
     * @throws GeneralException in an error occurs
     */
    public void testReserveInventoryForFacilityFIFOReceived() throws GeneralException {

        // set ProductStore's reserveOrderEnumId to INVRO_LIFO_REC
        setProductStoreInventoryReservationEnum(productStoreId, "INVRO_LIFO_REC", delegator);

        // set Facility's reserveOrderEnumId to INVRO_FIFO_REC
        setFacilityInventoryReservationEnum(facilityId, "INVRO_FIFO_REC", delegator);

        // create a product
        GenericValue product = createTestProduct("testReserveInventoryForFacilityFIFOReceived Test Product", demowarehouse1);

        // receive 10 into the warehouse and store the inventoryItemId as inventoryItemId1
        Map<String, Object> results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), demowarehouse1);
        String inventoryItemId1 = (String) results.get("inventoryItemId");

        pause("Workaround pause for MySQL");
        // receive another 10 into the warehouse and store the inventoryItemId as inventoryItemId2
        results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), demowarehouse1);
        String inventoryItemId2 = (String) results.get("inventoryItemId");

        // create a sales order for 15
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(product, new BigDecimal("15.0"));
        testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);

        // verify that:
        //   inventoryItemId1 ATP = 0, QOH = 10
        assertInventoryItemQuantities(inventoryItemId1, 0, 10);

        //   inventoryItemId2 ATP = 5, QOH =10
        assertInventoryItemQuantities(inventoryItemId2, 5, 10);
    }

    /**
     * This test verifies re-reserving inventory in a new facility and also what happens when you cancel a re-reserved order.
     * @throws GeneralException in an error occurs
     */
    public void testReReserveInventory() throws GeneralException {

        OrderRepositoryInterface repository = getOrderRepository(admin);
        // create a product
        GenericValue product = createTestProduct("testReReserveInventoryCreatesBackOrder Test Product", demowarehouse1);

        // receive 10 into the warehouse.  This is inventoryItemId1
        Map<String, Object> results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), demowarehouse1);
        String inventoryItemId1 = (String) results.get("inventoryItemId");

        // set ProductStore's reserveOrderEnumId to INVRO_LIFO_REC
        setProductStoreInventoryReservationEnum(productStoreId, "INVRO_LIFO_REC", delegator);

        // set RetailStore warehouse reserveOrderEnumId to INVRO_FIFO_REC
        setFacilityInventoryReservationEnum(facilityId1, "INVRO_FIFO_REC", delegator);

        // receive 2 into the RetailStore warehouse.  This is inventoryItemId2
        results = receiveInventoryProduct(product, new BigDecimal("2.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), facilityId1, demowarehouse1);
        String inventoryItemId2 = (String) results.get("inventoryItemId");

        pause("Workaround pause for MySQL");
        // receive 8 into the RetailStore warehouse.  This is inventoryItemId3
        results = receiveInventoryProduct(product, new BigDecimal("8.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), facilityId1, demowarehouse1);
        String inventoryItemId3 = (String) results.get("inventoryItemId");

        // create a sales order for 8
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(product, new BigDecimal("8.0"));
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);

        // verify that:
        //   inventoryItemId1 ATP = 2, QOH = 10
        assertInventoryItemQuantities(inventoryItemId1, 2.0, 10.0);

        //   inventoryItemId2 ATP = 2, QOH = 2
        assertInventoryItemQuantities(inventoryItemId2, 2.0, 2.0);

        //   inventoryItemId3 ATP = 8, QOH = 8
        assertInventoryItemQuantities(inventoryItemId3, 8.0, 8.0);

        // get initial OrderItem.getReservedQuantity()
        Order order = repository.getOrderById(orderFactory.getOrderId());
        OrderItem orderItem = repository.getOrderItem(order, "00001");
        BigDecimal beforeReservedQuantity = orderItem.getReservedQuantity();
        Debug.logInfo("Initial ReservedQuantity: " + beforeReservedQuantity, MODULE);

        // call reReserveInventoryProduct to re-reserve the inventory for 5 of the order item in the RetailStore warehouse
        reReserveInventory(orderFactory, inventoryItemId1, new BigDecimal("5.0"));

        // verify that:
        //   inventoryItemId1 ATP = 7, QOH = 10
        assertInventoryItemQuantities(inventoryItemId1, 7.0, 10.0);

        //   inventoryItemId2 ATP = 0, QOH = 2
        assertInventoryItemQuantities(inventoryItemId2, 0.0, 2.0);

        //   inventoryItemId3 ATP = 5, QOH = 8
        assertInventoryItemQuantities(inventoryItemId3, 5.0, 8.0);

        // verify current OrderItem.getReservedQuantity() equals OrderItem.getReservedQuantity() of before re-reserved to another facility.
        orderItem = repository.getOrderItem(order, "00001");
        BigDecimal afterReservedQuantity = orderItem.getReservedQuantity();
        Debug.logInfo("After re-reserved to another facility ReservedQuantity: " + afterReservedQuantity, MODULE);
        assertEquals("after product re-reserved to another facility, OrderItem.getReservedQuantity() should not changed.", afterReservedQuantity, beforeReservedQuantity);
        // cancel the order
        CancelOrderItemService cancelOrderItem = new CancelOrderItemService();
        cancelOrderItem.setInUserLogin(DemoCSR);
        cancelOrderItem.setInOrderId(orderFactory.getOrderId());
        cancelOrderItem.setInOrderItemSeqId("00001");
        cancelOrderItem.setInShipGroupSeqId("00001");
        cancelOrderItem.setInCancelQuantity(new BigDecimal("8.0"));
        runAndAssertServiceSuccess(cancelOrderItem);

        // verify that:
        //   inventoryItemId1 ATP = 10, QOH = 10
        assertInventoryItemQuantities(inventoryItemId1, 10.0, 10.0);

        //   inventoryItemId2 ATP = 2, QOH = 2
        assertInventoryItemQuantities(inventoryItemId2, 2.0, 2.0);

        //   inventoryItemId3 ATP = 8, QOH = 8
        assertInventoryItemQuantities(inventoryItemId3, 8.0, 8.0);
    }

    /**
     * This test verifies re-reserving inventory in a new facility and also what happens when an inventory transfer should fulfill the reservation.
     * @throws GeneralException in an error occurs
     */
    public void testReReserveInventoryAndInventoryTransfer() throws GeneralException {

        OrderRepositoryInterface repository = getOrderRepository(admin);
        // create a product
        GenericValue product = createTestProduct("testReReserveInventoryCreatesBackOrder Test Product", demowarehouse1);

        // receive 10 into the warehouse.  This is inventoryItemId1
        Map<String, Object> results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), demowarehouse1);
        String inventoryItemId1 = (String) results.get("inventoryItemId");

        // create a sales order for 5
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(product, new BigDecimal("5.0"));
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        Order order = repository.getOrderById(orderFactory.getOrderId());

        // call reReserveInventoryProduct to re-reserve the inventory for 2 of the order item in the RetailStore warehouse
        reReserveInventory(orderFactory, inventoryItemId1, new BigDecimal("2.0"));

        // check the order reservations before transferring the item

        // Web Store 00001: 3 x product
        Map<String, Map<String, BigDecimal>> expectedWebStoreItems = new HashMap<String, Map<String, BigDecimal>>();
        Map<String, BigDecimal> expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(product.getString("productId"), new BigDecimal("3.0"));
        expectedWebStoreItems.put("00001", expectedProducts);
        assertShipGroupReservations(orderFactory.getOrderId(), facilityId, expectedWebStoreItems);

        // My Retail Store 00001: 2 x product
        Map<String, Map<String, BigDecimal>> expectedRetailStoreItems = new HashMap<String, Map<String, BigDecimal>>();
        expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(product.getString("productId"), new BigDecimal("2.0"));
        expectedRetailStoreItems.put("00001", expectedProducts);
        assertShipGroupReservations(orderFactory.getOrderId(), facilityId1, expectedRetailStoreItems);

        // check the inventory backordered
        OrderItem orderItem = repository.getOrderItem(order, "00001");
        assertEquals("Quantity reserved for the order item.", orderItem.getReservedQuantity(), BigDecimal.valueOf(5.0));
        assertEquals("Quantity backordered for the order item.", orderItem.getShortfalledQuantity(), BigDecimal.valueOf(2.0));

        // transfer 5 from Web Store to My Retail Store
        CreateInventoryTransferService createInventoryTransfer = new CreateInventoryTransferService();
        createInventoryTransfer.setInUserLogin(demowarehouse1);
        createInventoryTransfer.setInFacilityId(facilityId);
        createInventoryTransfer.setInFacilityIdTo(facilityId1);
        createInventoryTransfer.setInInventoryItemId(inventoryItemId1);
        createInventoryTransfer.setInStatusId("IXF_SCHEDULED");
        createInventoryTransfer.setInXferQty(new BigDecimal("5.0"));
        runAndAssertServiceSuccess(createInventoryTransfer);
        String inventoryTransferId  = createInventoryTransfer.getOutInventoryTransferId();

        CompleteInventoryTransferService completeInventoryTransfer = new CompleteInventoryTransferService();
        completeInventoryTransfer.setInUserLogin(demowarehouse1);
        completeInventoryTransfer.setInInventoryTransferId(inventoryTransferId);
        pause("Workaround pause for MySQL");
        runAndAssertServiceSuccess(completeInventoryTransfer);

        // check the order reservations after transfer, the quantities reserved did not change

        // Web Store 00001: 3 x product
        expectedWebStoreItems = new HashMap<String, Map<String, BigDecimal>>();
        expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(product.getString("productId"), new BigDecimal("3.0"));
        expectedWebStoreItems.put("00001", expectedProducts);
        assertShipGroupReservations(orderFactory.getOrderId(), facilityId, expectedWebStoreItems);

        // My Retail Store 00001: 2 x product
        expectedRetailStoreItems = new HashMap<String, Map<String, BigDecimal>>();
        expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(product.getString("productId"), new BigDecimal("2.0"));
        expectedRetailStoreItems.put("00001", expectedProducts);
        assertShipGroupReservations(orderFactory.getOrderId(), facilityId1, expectedRetailStoreItems);

        // check the inventory backordered
        orderItem = repository.getOrderItem(order, "00001");
        assertEquals("Quantity reserved for the order item.", orderItem.getReservedQuantity(), BigDecimal.valueOf(5.0));
        assertEquals("Quantity backordered for the order item.", orderItem.getShortfalledQuantity(), BigDecimal.valueOf(0.0));
    }

    /**
     * This test verifies that re-reserving inventory in a new facility can also create back orders.
     * @throws GeneralException if an error occurs
     */
    public void testReReserveInventoryCreatesBackOrder() throws GeneralException {
        OrderRepositoryInterface repository = getOrderRepository(admin);
        // create a product
        GenericValue product = createTestProduct("testReReserveInventoryCreatesBackOrder Test Product", demowarehouse1);

        // create a sales order for 5
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(product, new BigDecimal("5.0"));
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        // get initial OrderItem.getReservedQuantity()
        Order order = repository.getOrderById(orderFactory.getOrderId());
        OrderItem orderItem = repository.getOrderItem(order, "00001");
        BigDecimal beforeReservedQuantity = orderItem.getReservedQuantity();
        Debug.logInfo("Initial ReservedQuantity: " + beforeReservedQuantity, MODULE);

        // verify that in the WebStoreWarehouse there is an inventory item with ATP = -5, QOH = 0 for this product.  This is inventoryItemId1
        List<GenericValue> inventoryItems = delegator.findByAnd(
                "InventoryItem",
                UtilMisc.toMap(
                        "productId", product.getString("productId"),
                        "availableToPromiseTotal", BigDecimal.valueOf(-5.0),
                        "quantityOnHandTotal", BigDecimal.valueOf(0.0),
                        "facilityId", facilityId
                )
        );
        assertFalse("", UtilValidate.isEmpty(inventoryItems));
        GenericValue inventoryItem = EntityUtil.getFirst(inventoryItems);
        String inventoryItemId1 = inventoryItem.getString("inventoryItemId");

        // reReserveInventoryProduct into RetailStore warehouse
        reReserveInventory(orderFactory, inventoryItemId1, new BigDecimal("5.0"));

        // verify current OrderItem.getReservedQuantity() equals OrderItem.getReservedQuantity() of before re-reserved to another facility.
        orderItem = repository.getOrderItem(order, "00001");
        BigDecimal afterReservedQuantity = orderItem.getReservedQuantity();
        Debug.logInfo("After re-reserved to another facility ReservedQuantity: " + afterReservedQuantity, MODULE);
        assertEquals("after product re-reserved to another facility, OrderItem.getReservedQuantity() should not changed.", afterReservedQuantity, beforeReservedQuantity);

        // verify that inventoryItemId1 has ATP = 0, QOH = 0
        assertInventoryItemQuantities(inventoryItemId1, 0.0, 0.0);

        // verify that in the RetailStore there is an inventory item with ATP = -5, QOH = 0 for this product.  This is inventoryItemId2
        inventoryItems = delegator.findByAnd(
                "InventoryItem",
                UtilMisc.toMap(
                        "productId", product.getString("productId"),
                        "availableToPromiseTotal", BigDecimal.valueOf(-5.0),
                        "quantityOnHandTotal", BigDecimal.valueOf(0.0),
                        "facilityId", facilityId1
                )
        );
        assertFalse("", UtilValidate.isEmpty(inventoryItems));
        inventoryItem = EntityUtil.getFirst(inventoryItems);
        String inventoryItemId2 = inventoryItem.getString("inventoryItemId");

        // cancel the order
        CancelOrderItemService cancelOrderItem = new CancelOrderItemService();
        cancelOrderItem.setInUserLogin(DemoCSR);
        cancelOrderItem.setInOrderId(orderFactory.getOrderId());
        cancelOrderItem.setInOrderItemSeqId("00001");
        cancelOrderItem.setInShipGroupSeqId("00001");
        cancelOrderItem.setInCancelQuantity(new BigDecimal("5.0"));
        runAndAssertServiceSuccess(cancelOrderItem);

        // verify that inventoryItemId1 has ATP = 0, QOH = 0
        assertInventoryItemQuantities(inventoryItemId1, 0.0, 0.0);

        // verify that inventoryItemId2 has ATP = 0, QOH = 0
        assertInventoryItemQuantities(inventoryItemId2, 0.0, 0.0);
    }

    /**
     * This test verifies re-reserving inventory in a new facility in the case when order ship-to address has associated facility.
     * @throws GeneralException if an error occurs
     */
    public void testReReserveInventoryAndAddressFacilityAssoc() throws GeneralException {
        OrderRepositoryInterface repository = getOrderRepository(admin);
        // create a product
        GenericValue product = createTestProduct("testReReserveInventoryAndAddressFacilityAssoc Test Product", demowarehouse1);

        // receive 10 into the warehouse.  This is inventoryItemId1
        Map<String, Object> results = receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), demowarehouse1);
        String inventoryItemId1 = (String) results.get("inventoryItemId");

        // create a sales order for 8
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(product, new BigDecimal("10.0"));
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        Order order = repository.getOrderById(orderFactory.getOrderId());

        // ensure DemoAddress1 has associated facility the same as source facility for re-reservation.
        Map<String, Object> conditions = UtilMisc.<String, Object>toMap("productStoreId", productStoreId, "facilityId", facilityId, "contactMechId", "DemoAddress1");
        GenericValue psfa = delegator.makeValue("ProductStoreFacilityByAddress", conditions);
        delegator.removeValue(psfa);
        psfa.create();

        // verify that:
        //   inventoryItemId1 ATP = 0, QOH = 10
        assertInventoryItemQuantities(inventoryItemId1, 0.0, 10.0);

        // get initial OrderItem.getReservedQuantity()
        OrderItem orderItem = repository.getOrderItem(order, "00001");
        BigDecimal beforeReservedQuantity = orderItem.getReservedQuantity();
        Debug.logInfo("Initial ReservedQuantity: " + beforeReservedQuantity, MODULE);

        // call reReserveInventoryProduct to re-reserve the inventory for 5 of the order item in the RetailStore warehouse
        reReserveInventory(orderFactory, inventoryItemId1, new BigDecimal("5.0"));

        // verify that:
        //   inventoryItemId1 ATP = 5, QOH = 10

        // during common reserve operations facility associated with ship-to address
        // has precedence of requested one. This isn't the case for re-reservation.
        // Increasing ATP to 5.0 is evidence of successful re-reservation in spite of
        // mapping for shipping address.
        assertInventoryItemQuantities(inventoryItemId1, 5.0, 10.0);

        // verify current OrderItem.getReservedQuantity() equals OrderItem.getReservedQuantity() of before re-reserved to another facility.
        orderItem = repository.getOrderItem(order, "00001");
        BigDecimal afterReservedQuantity = orderItem.getReservedQuantity();
        Debug.logInfo("After re-reserved to another facility ReservedQuantity: " + afterReservedQuantity, MODULE);
        assertEquals("after product re-reserved to another facility, OrderItem.getReservedQuantity() should not changed.", afterReservedQuantity, beforeReservedQuantity);

    }

    /**
     * This test verifies re-reserving inventory in a new facility for a split order.
     * @throws GeneralException in an error occurs
     */
    public void testReReserveInventoryOnSplitOrder() throws GeneralException {
        OrderRepositoryInterface orderRepository = getOrderRepository(admin);
        InventoryRepositoryInterface inventoryRepository = getInventoryRepository(admin);

        // create 3 test products
        GenericValue productA = createTestProduct("Test Product A for testReReserveInventoryOnSplitOrder", demowarehouse1);
        GenericValue productB = createTestProduct("Test Product B for testReReserveInventoryOnSplitOrder", demowarehouse1);
        GenericValue productC = createTestProduct("Test Product C for testReReserveInventoryOnSplitOrder", demowarehouse1);
        assignDefaultPrice(productA, new BigDecimal("1.0"), admin);
        assignDefaultPrice(productB, new BigDecimal("1.0"), admin);
        assignDefaultPrice(productC, new BigDecimal("1.0"), admin);
        // create a customer from template of DemoCustomer
        String customerPartyId = createPartyFromTemplate(DemoCustomer.getString("partyId"), "Customer for testMultiFacilityOrder");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));

        // create a sales order of 5 x productA, 8 x productB, 10 productC approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(productA, new BigDecimal("5.0"));
        orderItems.put(productB, new BigDecimal("8.0"));
        orderItems.put(productC, new BigDecimal("10.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, customer, productStoreId);
        salesOrder.approveOrder();
        Order order = orderRepository.getOrderById(salesOrder.getOrderId());

        OrderItem orderItemProductA = null;
        OrderItem orderItemProductB = null;
        OrderItem orderItemProductC = null;
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getProductId().equals(productA.getString("productId"))) {
                orderItemProductA = orderItem;
            }
            if (orderItem.getProductId().equals(productB.getString("productId"))) {
                orderItemProductB = orderItem;
            }
            if (orderItem.getProductId().equals(productC.getString("productId"))) {
                orderItemProductC = orderItem;
            }
        }

        assertNotNull("Order Item for product A not found.", orderItemProductA);
        assertNotNull("Order Item for product B not found.", orderItemProductB);
        assertNotNull("Order Item for product C not found.", orderItemProductC);
        Debug.logInfo("testReReserveInventoryOnSplitOrder() create orderId: " + salesOrder.getOrderId(), MODULE);

        // re-reserve 5 of productB in My Retail Store
        //reReserveOrderItemInventory(order.getOrderId(), orderItemProductB.getOrderItemSeqId(), facilityId1, 5.0);
        List<org.opentaps.base.entities.OrderItemShipGrpInvRes> orderItemReservations = inventoryRepository.getOrderItemShipGroupInventoryReservations(order.getOrderId(), orderItemProductB.getOrderItemSeqId(), null, "00001");
        assertEquals("Should have only one reservation of productC in the second ship group.", orderItemReservations.size(), 1);
        reReserveOrderItemInventory(order.getOrderId(), orderItemProductB.getOrderItemSeqId(), orderItemReservations.get(0).getInventoryItemId(), shipGroupSeqId, facilityId1, new BigDecimal("5.0"));
        // split 7 x productC in a second ship group
        assertSplitOrderSuccess(admin, order.getOrderId(), "7.0", "00001", orderItemProductC.getOrderItemSeqId(), SECONDARY_SHIPPING_ADDRESS);

        // re-reserve 3 of productC of the second ship group in My Retail Store
        // - find the reservation in the second ship group
        orderItemReservations = inventoryRepository.getOrderItemShipGroupInventoryReservations(order.getOrderId(), orderItemProductC.getOrderItemSeqId(), null, "00002");
        assertEquals("Should have only one reservation of productC in the second ship group.", orderItemReservations.size(), 1);
        reReserveOrderItemInventory(order.getOrderId(), orderItemProductC.getOrderItemSeqId(), orderItemReservations.get(0).getString("inventoryItemId"), "00002", facilityId1, new BigDecimal("3.0"));

        // check the order reservations before receiving the items

        // Web Store 00001: 5 x productA, 3 x productB and 3 x productC
        Map<String, Map<String, BigDecimal>> expectedWebStoreItems = new HashMap<String, Map<String, BigDecimal>>();
        Map<String, BigDecimal> expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(productA.getString("productId"), new BigDecimal("5.0"));
        expectedProducts.put(productB.getString("productId"), new BigDecimal("3.0"));
        expectedProducts.put(productC.getString("productId"), new BigDecimal("3.0"));
        expectedWebStoreItems.put("00001", expectedProducts);
        // Web Store 00002: 4 x productC
        expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(productC.getString("productId"), new BigDecimal("4.0"));
        expectedWebStoreItems.put("00002", expectedProducts);

        assertShipGroupReservations(order.getOrderId(), facilityId, expectedWebStoreItems);

        // My Retail Store 00001: 5 x productB
        Map<String, Map<String, BigDecimal>> expectedRetailStoreItems = new HashMap<String, Map<String, BigDecimal>>();
        expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(productB.getString("productId"), new BigDecimal("5.0"));
        expectedRetailStoreItems.put("00001", expectedProducts);
        // My Retail Store 00002: 3 x productC
        expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(productC.getString("productId"), new BigDecimal("3.0"));
        expectedRetailStoreItems.put("00002", expectedProducts);

        assertShipGroupReservations(order.getOrderId(), facilityId1, expectedRetailStoreItems);

        // receive in Web Store 5 x productA, 3 x productB and 7 x productC
        receiveInventoryProduct(productA, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        receiveInventoryProduct(productB, new BigDecimal("3.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        receiveInventoryProduct(productC, new BigDecimal("7.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);

        assertShipGroupReservations(order.getOrderId(), facilityId, expectedWebStoreItems);
        assertShipGroupReservations(order.getOrderId(), facilityId1, expectedRetailStoreItems);

        // receive in My Retail Store 5 x ProductB and 3 x productC
        receiveInventoryProduct(productB, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId1, demowarehouse1);
        receiveInventoryProduct(productC, new BigDecimal("3.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId1, demowarehouse1);

        // check the order appears as ready to ship on both warehouse
        // the process is the same as what the BSH on the ready to ship page does:
        // - shippinghelper = new ShippingHelper(delegator, facilityId);
        // - shippinghelper.findAllOrdersReadyToShip() => should contain the corresponding OdrItShpGrpHdrInvResAndInvItem entities
        // need to be done for each facility, and check that the correct OdrItShpGrpHdrInvResAndInvItem are returned in each case (note this might also contain other orders from other tests)
        // - check the orderId / shipGroupSeqId / productId / quantity are correct

        assertShipGroupReservations(order.getOrderId(), facilityId, expectedWebStoreItems);
        assertOrderReadyToShip(order, facilityId, expectedWebStoreItems);

        assertShipGroupReservations(order.getOrderId(), facilityId1, expectedRetailStoreItems);
        assertOrderReadyToShip(order, facilityId1, expectedRetailStoreItems);
    }

    /**
     * Perform re-reservation an inventory item that are reserved against order item 00001 in ship group 00001.
     *
     * @param orderFactory an order
     * @param inventoryItemId inventory item id to re-reserve
     * @param quantity quantity of product
     * @throws GeneralException if an error occurs
     */
    private void reReserveInventory(SalesOrderFactory orderFactory, String inventoryItemId, BigDecimal quantity) throws GeneralException {
        reReserveOrderItemInventory(orderFactory.getOrderId(), "00001", inventoryItemId, "00001", facilityId1, quantity);
    }

    /**
     * This test verifies multi ship group order work for multi factility.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMultiFacilityMultiShipGroupOrder() throws GeneralException {
        OrderRepositoryInterface orderRepository = getOrderRepository(admin);
        InventoryRepositoryInterface inventoryRepository = getInventoryRepository(admin);
        // create 3 test products
        GenericValue productA = createTestProduct("Test Product A for testMultiFacilityMultiShipGroupOrder", demowarehouse1);
        GenericValue productB = createTestProduct("Test Product B for testMultiFacilityMultiShipGroupOrder", demowarehouse1);
        GenericValue productC = createTestProduct("Test Product C for testMultiFacilityMultiShipGroupOrder", demowarehouse1);
        assignDefaultPrice(productA, new BigDecimal("1.0"), admin);
        assignDefaultPrice(productB, new BigDecimal("1.0"), admin);
        assignDefaultPrice(productC, new BigDecimal("1.0"), admin);
        // create a customer from template of DemoCustomer
        String customerPartyId = createPartyFromTemplate(DemoCustomer.getString("partyId"), "Customer for testMultiFacilityOrder");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));

        // record each store findOrdersToPickMove initial number
        FindOrdersToPickMoveService findOrdersToPickMove = new FindOrdersToPickMoveService();
        findOrdersToPickMove.setInUserLogin(admin);
        findOrdersToPickMove.setInFacilityId(facilityId);
        findOrdersToPickMove.setInMaxNumberOfOrders(new Long(1000));
        runAndAssertServiceSuccess(findOrdersToPickMove);
        Long webStoreReadyToPickInitNumber = findOrdersToPickMove.getOutNReturnedOrders();

        findOrdersToPickMove = new FindOrdersToPickMoveService();
        findOrdersToPickMove.setInUserLogin(admin);
        findOrdersToPickMove.setInFacilityId(facilityId1);
        findOrdersToPickMove.setInMaxNumberOfOrders(new Long(1000));
        runAndAssertServiceSuccess(findOrdersToPickMove);
        Long myRetailStoreReadyToPickInitNumber = findOrdersToPickMove.getOutNReturnedOrders();

        // receive in Web Store 5 x productA, 3 x productB and 7 x productC
        receiveInventoryProduct(productA, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        receiveInventoryProduct(productB, new BigDecimal("3.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        receiveInventoryProduct(productC, new BigDecimal("7.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);

        // receive in My Retail Store 5 x ProductB and 3 x productC
        receiveInventoryProduct(productB, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId1, demowarehouse1);
        receiveInventoryProduct(productC, new BigDecimal("3.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId1, demowarehouse1);

        // create a sales order of 5 x productA, 8 x productB, 10 productC approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(productA, new BigDecimal("5.0"));
        orderItems.put(productB, new BigDecimal("8.0"));
        orderItems.put(productC, new BigDecimal("10.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, customer, productStoreId);
        salesOrder.approveOrder();
        Order order = orderRepository.getOrderById(salesOrder.getOrderId());
        Debug.logInfo("testMultiFacilityMultiShipGroupOrder() create orderId: " + salesOrder.getOrderId(), MODULE);

        OrderItem orderItemProductA = null;
        OrderItem orderItemProductB = null;
        OrderItem orderItemProductC = null;
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getProductId().equals(productA.getString("productId"))) {
                orderItemProductA = orderItem;
            }
            if (orderItem.getProductId().equals(productB.getString("productId"))) {
                orderItemProductB = orderItem;
            }
            if (orderItem.getProductId().equals(productC.getString("productId"))) {
                orderItemProductC = orderItem;
            }
        }

        assertNotNull("Order Item for product A not found.", orderItemProductA);
        assertNotNull("Order Item for product B not found.", orderItemProductB);
        assertNotNull("Order Item for product C not found.", orderItemProductC);

        // re-reserve 5 of productB in My Retail Store
        List<OrderItemShipGrpInvRes> reservations = orderItemProductB.getOrderItemShipGrpInvReses();
        assertEquals("Should be only reservation for the order item at this point", 1, reservations.size());
        reReserveOrderItemInventory(
                order.getOrderId(),
                orderItemProductB.getOrderItemSeqId(),
                reservations.get(0).getInventoryItemId(),
                reservations.get(0).getShipGroupSeqId(),
                facilityId1,
                new BigDecimal("5.0")
        );
        // split 7 x productC in a second ship group
        assertSplitOrderSuccess(admin, order.getOrderId(), "7.0", "00001", orderItemProductC.getOrderItemSeqId(), SECONDARY_SHIPPING_ADDRESS);

        // re-reserve 3 of productC of the second ship group in My Retail Store
        // - find the reservation in the second ship group
        List<org.opentaps.base.entities.OrderItemShipGrpInvRes> res = inventoryRepository.getOrderItemShipGroupInventoryReservations(order.getOrderId(), orderItemProductC.getOrderItemSeqId(), null, "00002");
        assertEquals("Should have only one reservation of productC in the second ship group.", res.size(), 1);
        reReserveOrderItemInventory(order.getOrderId(), orderItemProductC.getOrderItemSeqId(), res.get(0).getString("inventoryItemId"), "00002", facilityId1, new BigDecimal("3.0"));

        // check the order appears as ready to ship on both warehouse
        // the process is the same as what the BSH on the ready to ship page does:
        // - shippinghelper = new ShippingHelper(delegator, facilityId);
        // - shippinghelper.findAllOrdersReadyToShip() => should contain the corresponding OdrItShpGrpHdrInvResAndInvItem entities
        // need to be done for each facility, and check that the correct OdrItShpGrpHdrInvResAndInvItem are returned in each case (note this might also contain other orders from other tests)
        // - check the orderId / shipGroupSeqId / productId / quantity are correct

        // Web Store 00001: 5 x productA, 3 x productB and 3 x productC
        Map<String, Map<String, BigDecimal>> expectedWebStoreItems = new HashMap<String, Map<String, BigDecimal>>();
        Map<String, BigDecimal> expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(productA.getString("productId"), new BigDecimal("5.0"));
        expectedProducts.put(productB.getString("productId"), new BigDecimal("3.0"));
        expectedProducts.put(productC.getString("productId"), new BigDecimal("3.0"));
        expectedWebStoreItems.put("00001", expectedProducts);
        // Web Store 00002: 4 x productC
        expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(productC.getString("productId"), new BigDecimal("4.0"));
        expectedWebStoreItems.put("00002", expectedProducts);

        assertShipGroupReservations(order.getOrderId(), facilityId, expectedWebStoreItems);
        assertOrderReadyToShip(order, facilityId, expectedWebStoreItems);

        // My Retail Store 00001: 5 x productB
        Map<String, Map<String, BigDecimal>> expectedRetailStoreItems = new HashMap<String, Map<String, BigDecimal>>();
        expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(productB.getString("productId"), new BigDecimal("5.0"));
        expectedRetailStoreItems.put("00001", expectedProducts);
        // My Retail Store 00002: 3 x productC
        expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(productC.getString("productId"), new BigDecimal("3.0"));
        expectedRetailStoreItems.put("00002", expectedProducts);

        assertShipGroupReservations(order.getOrderId(), facilityId1, expectedRetailStoreItems);
        assertOrderReadyToShip(order, facilityId1, expectedRetailStoreItems);

        // check the order appears as ready to pick on both warehouse
        // this should test the findOrdersToPickMove service which return the number of order ready to pick
        // so we need to test this value before creating the order, and now, and check the quantity increased by 2 (because of the two ship groups)
        // this should be checked on both facilities
        findOrdersToPickMove = new FindOrdersToPickMoveService();
        findOrdersToPickMove.setInUserLogin(admin);
        findOrdersToPickMove.setInFacilityId(facilityId);
        findOrdersToPickMove.setInMaxNumberOfOrders(new Long(1000));
        runAndAssertServiceSuccess(findOrdersToPickMove);
        Long webStoreReadyToPickCurrentNumber = findOrdersToPickMove.getOutNReturnedOrders();
        assertEquals("quantity of order ready to pick should increased by 1 in Facility [" + facilityId + "]", (int) (webStoreReadyToPickInitNumber + 2), webStoreReadyToPickCurrentNumber.intValue());

        findOrdersToPickMove = new FindOrdersToPickMoveService();
        findOrdersToPickMove.setInUserLogin(admin);
        findOrdersToPickMove.setInFacilityId(facilityId1);
        findOrdersToPickMove.setInMaxNumberOfOrders(new Long(1000));
        runAndAssertServiceSuccess(findOrdersToPickMove);
        Long myRetailStoreReadyToPickCurrentNumber = findOrdersToPickMove.getOutNReturnedOrders();
        assertEquals("quantity of order ready to pick should increased by 1 in Facility [" + facilityId1 + "]", (int) (myRetailStoreReadyToPickInitNumber + 2), myRetailStoreReadyToPickCurrentNumber.intValue());

        // create picklists in both facilities and test the correct items are in each picklist
        // for each facility:
        // - call the createPicklistFromOrders service for the order (see orderIdList service parameter)
        // - check the created picklist by checking the PicklistItemAndOdrItmShipGrp entities that were created with the returned picklistId
        // - check the orderId / shipGroupSeqId / productId / quantity are correct:
        // -- should have 5 x productA, 3 x productB and 7 x productC in the picklist for Web Store
        // -- should have 5 x ProductB and 3 x productC in the picklist for My Retail Store
        List orderIdList = UtilMisc.toList(order.getOrderId());
        CreatePicklistFromOrdersService createPicklistFromOrders = new CreatePicklistFromOrdersService();
        createPicklistFromOrders.setInUserLogin(admin);
        createPicklistFromOrders.setInOrderIdList(orderIdList);
        createPicklistFromOrders.setInFacilityId(facilityId);
        createPicklistFromOrders.setInMaxNumberOfOrders(new Long(1000));
        runAndAssertServiceSuccess(createPicklistFromOrders);
        String picklistIdForWebStore  = createPicklistFromOrders.getOutPicklistId();
        List<GenericValue> picklistItems = delegator.findByCondition("PicklistItemAndOdrItmShipGrp", EntityCondition.makeCondition(EntityOperator.AND, EntityCondition.makeCondition("pPicklistId", EntityOperator.EQUALS, picklistIdForWebStore)), null, UtilMisc.toList("piOrderId", "piShipGroupSeqId", "oiProductId"));
        boolean have5ProductAForWebStore = false;
        boolean have3ProductBForWebStore = false;
        // productC is split in two ship groups
        boolean have4ProductCForWebStore = false;
        boolean have3ProductCForWebStore = false;

        for (GenericValue item : picklistItems) {
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productA.getString("productId"))
                    && item.getBigDecimal("piQuantity").doubleValue() == 5.0) {
                        have5ProductAForWebStore = true;
            }
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productB.getString("productId"))
                    && item.getBigDecimal("piQuantity").doubleValue() == 3.0) {
                have3ProductBForWebStore = true;
            }
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productC.getString("productId"))
                    && item.getBigDecimal("piQuantity").doubleValue() == 4.0) {
                have4ProductCForWebStore = true;
            }
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productC.getString("productId"))
                    && item.getBigDecimal("piQuantity").doubleValue() == 3.0) {
                have3ProductCForWebStore = true;
            }
         }
        assertTrue("should have 5 x productA in the picklist for Web Store", have5ProductAForWebStore);
        assertTrue("should have 3 x productB in the picklist for Web Store", have3ProductBForWebStore);
        assertTrue("should have 3 x productC in the picklist for Web Store", have3ProductCForWebStore);
        assertTrue("should have 4 x productC in the picklist for Web Store", have4ProductCForWebStore);

        createPicklistFromOrders = new CreatePicklistFromOrdersService();
        createPicklistFromOrders.setInUserLogin(admin);
        createPicklistFromOrders.setInOrderIdList(orderIdList);
        createPicklistFromOrders.setInFacilityId(facilityId1);
        createPicklistFromOrders.setInMaxNumberOfOrders(new Long(1000));
        runAndAssertServiceSuccess(createPicklistFromOrders);
        String picklistIdForMyRetail = createPicklistFromOrders.getOutPicklistId();
        picklistItems = delegator.findByCondition("PicklistItemAndOdrItmShipGrp", EntityCondition.makeCondition(EntityOperator.AND, EntityCondition.makeCondition("pPicklistId", EntityOperator.EQUALS, picklistIdForMyRetail)), null, UtilMisc.toList("piOrderId", "piShipGroupSeqId", "oiProductId"));
        boolean have5ProductBForMyRetail = false;
        boolean have3ProductCForMyRetail = false;

        for (GenericValue item : picklistItems) {
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId1)
                    && item.getString("oiProductId").equals(productB.getString("productId"))
                    && item.getBigDecimal("piQuantity").doubleValue() == 5.0) {
                have5ProductBForMyRetail = true;
            }
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId1)
                    && item.getString("oiProductId").equals(productC.getString("productId"))
                    && item.getBigDecimal("piQuantity").doubleValue() == 3.0) {
                have3ProductCForMyRetail = true;
            }
        }
        assertTrue("should have 5 x ProductB in the picklist for My Retail Store", have5ProductBForMyRetail);
        assertTrue("should have 3 x ProductC in the picklist for My Retail Store", have3ProductCForMyRetail);

        // set both picklist to picked with service updatePicklist
        runAndAssertServiceSuccess("updatePicklist", UtilMisc.toMap("picklistId", picklistIdForWebStore, "facilityId", facilityId, "userLogin", admin));
        runAndAssertServiceSuccess("updatePicklist", UtilMisc.toMap("picklistId", picklistIdForMyRetail, "facilityId", facilityId1, "userLogin", admin));

        // use testShipOrder for the order and facility Web Store
        // check that both OrderItemShipGroup status are NOT OISG_PACKED
        // check 2 shipments were created (one per ship group involved), and contains the correct items:
        // -- ship group 1: 5 x productA, 3 x productB and 3 x productC
        // -- ship group 2: 4 x productC
        // check that 2 invoices were created
        TestShipOrderService testShipOrder = new TestShipOrderService();
        testShipOrder.setInUserLogin(demowarehouse1);
        testShipOrder.setInOrderId(order.getOrderId());
        testShipOrder.setInFacilityId(facilityId);
        runAndAssertServiceSuccess(testShipOrder);

        GenericValue orderItemShipGroup2 = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId", "00002"));
        assertNotEquals("OrderItemShipGroup status should NOT be OISG_PACKED", "OISG_PACKED", orderItemShipGroup2.getString("statusId"));

        GenericValue orderItemShipGroup1 = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId", "00001"));
        assertNotEquals("OrderItemShipGroup status should NOT be OISG_PACKED", "OISG_PACKED", orderItemShipGroup1.getString("statusId"));

        List<String> shipmentIds = testShipOrder.getOutShipmentIds();
        assertEquals("Check 2 shipment was created", 2, shipmentIds.size());
        GenericValue shipment1 = EntityUtil.getFirst(delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", order.getOrderId(), "primaryShipGroupSeqId", "00001")));
        GenericValue shipment2 = EntityUtil.getFirst(delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", order.getOrderId(), "primaryShipGroupSeqId", "00002")));
        List orderShipment1s = delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipment1.getString("shipmentId")));
        Iterator orderShipments1It = orderShipment1s.iterator();
        while (orderShipments1It.hasNext()) {
            GenericValue orderShipment = (GenericValue) orderShipments1It.next();
            GenericValue orderItem = orderShipment.getRelatedOne("OrderItem");
            if (orderItem.getString("productId").equals(productA.getString("productId"))) {
                // check a shipment was created, and contains the correct items: 5 x productA
                assertEquals("Assert contains 5 x productA", 5.0, orderShipment.getBigDecimal("quantity").doubleValue());
            }
            if (orderItem.getString("productId").equals(productB.getString("productId"))) {
                // check a shipment was created, and contains the correct items: 3 x productA
                assertEquals("Assert contains 3 x productB", 3.0, orderShipment.getBigDecimal("quantity").doubleValue());
            }
            if (orderItem.getString("productId").equals(productC.getString("productId"))) {
                // check a shipment was created, and contains the correct items: 3 x productA
                assertEquals("Assert contains 3 x productC", 3.0, orderShipment.getBigDecimal("quantity").doubleValue());
            }
        }
        List orderShipment2s = delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipment2.getString("shipmentId")));
        Iterator orderShipments2It = orderShipment2s.iterator();
        while (orderShipments2It.hasNext()) {
            GenericValue orderShipment = (GenericValue) orderShipments2It.next();
            GenericValue orderItem = orderShipment.getRelatedOne("OrderItem");
            if (orderItem.getString("productId").equals(productC.getString("productId"))) {
                // check a shipment was created, and contains the correct items: 4 x productA
                   assertEquals("Assert contains 4 x productC", 4.0, orderShipment.getBigDecimal("quantity").doubleValue());
               }
        }
        List<GenericValue> billings = shipment1.getRelated("ShipmentItemBilling");
        billings.addAll(shipment2.getRelated("ShipmentItemBilling"));
        assertEquals("2 invoice should be created", 2, EntityUtil.getFieldListFromEntityList(billings, "invoiceId", true).size());

        // use testShipOrder for the order and facility My Retail Store and ship group 1
        // check that OrderItemShipGroup status is NOT OISG_PACKED for ship group 2
        // check that OrderItemShipGroup status IS OISG_PACKED for ship group 1
        // check 1 shipments was created and contains the correct items: 5 x productB
        // check that 1 invoices was created
        testShipOrder = new TestShipOrderService();
        testShipOrder.setInUserLogin(demowarehouse1);
        testShipOrder.setInOrderId(order.getOrderId());
        testShipOrder.setInFacilityId(facilityId1);
        testShipOrder.setInShipGroupSeqId("00001");
        runAndAssertServiceSuccess(testShipOrder);

        orderItemShipGroup2 = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId", "00002"));
        assertNotEquals("OrderItemShipGroup status should NOT be OISG_PACKED", "OISG_PACKED", orderItemShipGroup2.getString("statusId"));

        orderItemShipGroup1 = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId", "00001"));
        assertEquals("OrderItemShipGroup status should be OISG_PACKED", "OISG_PACKED", orderItemShipGroup1.getString("statusId"));

        shipmentIds = testShipOrder.getOutShipmentIds();
        assertEquals("Check 1 shipment was created", 1, shipmentIds.size());
        shipment1 = EntityUtil.getFirst(delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", order.getOrderId(), "primaryShipGroupSeqId", "00001", "shipmentId", shipmentIds.get(0))));
        orderShipment1s = delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipment1.getString("shipmentId")));
        orderShipments1It = orderShipment1s.iterator();
        while (orderShipments1It.hasNext()) {
            GenericValue orderShipment = (GenericValue) orderShipments1It.next();
            GenericValue orderItem = orderShipment.getRelatedOne("OrderItem");
            if (orderItem.getString("productId").equals(productB.getString("productId"))) {
                    // check a shipment was created, and contains the correct items: 5 x productB
                   assertEquals("Assert contains 5 x productB", 5.0, orderShipment.getBigDecimal("quantity").doubleValue());
               }
        }
        billings = shipment1.getRelated("ShipmentItemBilling");
        assertEquals("1 invoice should be created", 1, EntityUtil.getFieldListFromEntityList(billings, "invoiceId", true).size());

        // use testShipOrder for the order and facility My Retail Store and ship group 2
        // check that OrderItemShipGroup status IS OISG_PACKED for ship group 1 and 2
        // check 1 shipments was created and contains the correct items: 3 x productC
        // check that 1 invoices was created
        testShipOrder = new TestShipOrderService();
        testShipOrder.setInUserLogin(demowarehouse1);
        testShipOrder.setInOrderId(order.getOrderId());
        testShipOrder.setInFacilityId(facilityId1);
        testShipOrder.setInShipGroupSeqId("00002");
        runAndAssertServiceSuccess(testShipOrder);

        orderItemShipGroup2 = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId", "00002"));
        assertEquals("OrderItemShipGroup status should be OISG_PACKED", "OISG_PACKED", orderItemShipGroup2.getString("statusId"));

        orderItemShipGroup1 = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId", "00001"));
        assertEquals("OrderItemShipGroup status should be OISG_PACKED", "OISG_PACKED", orderItemShipGroup1.getString("statusId"));

        shipmentIds = testShipOrder.getOutShipmentIds();
        assertEquals("Check 1 shipment was created", 1, shipmentIds.size());
        shipment1 = EntityUtil.getFirst(delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", order.getOrderId(), "primaryShipGroupSeqId", "00001")));
        orderShipment1s = delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipment1.getString("shipmentId")));
        orderShipments1It = orderShipment1s.iterator();
        while (orderShipments1It.hasNext()) {
            GenericValue orderShipment = (GenericValue) orderShipments1It.next();
            GenericValue orderItem = orderShipment.getRelatedOne("OrderItem");
            if (orderItem.getString("productId").equals(productC.getString("productId"))) {
                    // check a shipment was created, and contains the correct items: 3 x productC
                   assertEquals("Assert contains 3 x productC", 3.0, orderShipment.getBigDecimal("quantity").doubleValue());
               }
        }
        billings = shipment1.getRelated("ShipmentItemBilling");
        assertEquals("1 invoice should be created", 1, EntityUtil.getFieldListFromEntityList(billings, "invoiceId", true).size());

        // check the order status is now COMPLETED
        assertOrderCompleted(order.getOrderId());
    }

    /**
     * Tests making a sales order and check that accounting tags are copied correctly.
     * - inventory tags, should be copied to the inventory related accounting transactions
     * - order item tags, should be copied to the related invoice items and related accounting transactions
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testOrderAccountingTags() throws Exception {
        OrderRepositoryInterface orderRepository = getOrderRepository(admin);

        // accounting tags used during the test
        Map<String, String> inventoryTagsA = new HashMap<String, String>();
        inventoryTagsA.put("acctgTagEnumId1", "DIV_CONSUMER");
        inventoryTagsA.put("acctgTagEnumId2", "DPT_WAREHOUSE");
        inventoryTagsA.put("acctgTagEnumId3", "ACTI_PRODUCT");
        Map<String, String> inventoryTagsB = new HashMap<String, String>();
        inventoryTagsB.put("acctgTagEnumId1", "DIV_ENTERPRISE");
        inventoryTagsB.put("acctgTagEnumId2", "DPT_WAREHOUSE");
        inventoryTagsB.put("acctgTagEnumId3", "ACTI_MARKETING");
        Map<String, String> orderItemTagsA = new HashMap<String, String>();
        orderItemTagsA.put("acctgTagEnumId1", "DIV_GOV");
        orderItemTagsA.put("acctgTagEnumId2", "DPT_SALES");
        Map<String, String> orderItemTagsB = new HashMap<String, String>();
        orderItemTagsB.put("acctgTagEnumId1", "DIV_NONPROFIT");
        orderItemTagsB.put("acctgTagEnumId3", "ACTI_TRAINING");
        Map<String, String> noTags = new HashMap<String, String>();

        // create 2 test products
        GenericValue productA = createTestProduct("Test Product A for testOrderAccountingTags", demowarehouse1);
        GenericValue productB = createTestProduct("Test Product B for testOrderAccountingTags", demowarehouse1);
        String productAId = productA.getString("productId");
        String productBId = productB.getString("productId");
        assignDefaultPrice(productA, new BigDecimal("5.0"), admin);
        assignDefaultPrice(productB, new BigDecimal("7.0"), admin);

        // create a customer from template of DemoCustomer
        String customerPartyId = createPartyFromTemplate(DemoCustomer.getString("partyId"), "Customer for testOrderAccountingTags");

        // receive in Web Store 5 x productA, 3 x productB
        Map<String, Object> results = receiveInventoryProduct(productA, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, inventoryTagsA, demowarehouse1);
        String inventoryAId = (String) results.get("inventoryItemId");
        results = receiveInventoryProduct(productB, new BigDecimal("3.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, inventoryTagsB, demowarehouse1);
        String inventoryBId = (String) results.get("inventoryItemId");

        // check the inventory tags (the receipt transactions tags are tested in the inventory tests)
        GenericValue inventoryA = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryAId));
        assertAccountingTagsEqual(inventoryA, inventoryTagsA);
        GenericValue inventoryB = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryBId));
        assertAccountingTagsEqual(inventoryB, inventoryTagsB);

        // create a sales order of 5 x productA, 3 x productB
        SalesOrderFactory salesOrder = new SalesOrderFactory(delegator, dispatcher, User, getOrganizationPartyId(), customerPartyId, productStoreId);
        salesOrder.addProduct(productA, new BigDecimal("5.0"), salesOrder.getFirstShipGroup(), orderItemTagsA);
        salesOrder.addProduct(productB, new BigDecimal("3.0"), salesOrder.getFirstShipGroup(), orderItemTagsB);
        salesOrder.approveOrder();
        Order order = orderRepository.getOrderById(salesOrder.getOrderId());

        // add a manual adjustment which will be tagged automatically in the invoice
        CreateOrderAdjustmentService adjService = new CreateOrderAdjustmentService();
        adjService.setInUserLogin(admin);
        adjService.setInOrderId(order.getOrderId());
        adjService.setInAmount(new BigDecimal("35.0"));
        adjService.setInOrderAdjustmentTypeId(OrderAdjustmentTypeConstants.SHIPPING_CHARGES);
        adjService.setInDescription("test manual adjustment tagging");
        runAndAssertServiceSuccess(adjService);

        // pack order
        TestShipOrderService testShipOrder = new TestShipOrderService();
        testShipOrder.setInUserLogin(demowarehouse1);
        testShipOrder.setInOrderId(order.getOrderId());
        testShipOrder.setInFacilityId(facilityId);
        runAndAssertServiceSuccess(testShipOrder);
        List<String> shipmentIds = testShipOrder.getOutShipmentIds();

        assertEquals("Should have exactly one shipment for the order [" + order.getOrderId() + "]", 1, shipmentIds.size());
        String shipmentId = shipmentIds.get(0);

        // get order Items, and check their tags
        OrderItem itemA = null;
        OrderItem itemB = null;
        for (OrderItem item : order.getOrderItems()) {
            if (productAId.equals(item.getProductId())) {
                itemA = item;
                assertAccountingTagsEqual(item, orderItemTagsA);
            } else if (productBId.equals(item.getProductId())) {
                itemB = item;
                assertAccountingTagsEqual(item, orderItemTagsB);
            } else {
                fail("Unknown order item found with product [" + item.getProductId() + "]");
            }
        }
        assertNotNull("Could not find productA in order [" + order.getOrderId() + "]", itemA);
        assertNotNull("Could not find productB in order [" + order.getOrderId() + "]", itemB);

        // get the invoices
        List<Invoice> invoices = order.getInvoices();
        assertEquals("Should have exactly one invoice for the order [" + order.getOrderId() + "]", 1, invoices.size());
        Invoice invoice = invoices.get(0);

        // check the invoice items, those related to order items should be tagged the same, others should not be tagged
        InvoiceItem invoiceItemFirst = null;
        InvoiceItem invoiceItemA = null;
        InvoiceItem invoiceItemB = null;
        boolean firstInvoiceItemIsLikeA = true;
        for (InvoiceItem invoiceItem : invoice.getInvoiceItems()) {
            if (invoiceItemFirst == null) {
                invoiceItemFirst = invoiceItem;
                // determine how it was tagged
                if (orderItemTagsA.get("acctgTagEnumId1").equals(invoiceItem.getAcctgTagEnumId1())) {
                    firstInvoiceItemIsLikeA = true;
                } else if (orderItemTagsB.get("acctgTagEnumId1").equals(invoiceItem.getAcctgTagEnumId1())) {
                    firstInvoiceItemIsLikeA = false;
                } else {
                    // not tagged like either A or B ?
                    fail("Unexpected accounting tags on the first invoice item of invoice [], expected either [" + orderItemTagsA.get("acctgTagEnumId1") + "] or [" + orderItemTagsB.get("acctgTagEnumId1") + "] but found [" + invoiceItem.getAcctgTagEnumId1() + "]");
                }
            }

            // invoice item related to an order item are tagged according
            // to that order item
            String itemProductId = invoiceItem.getProductId();
            if (productAId.equals(itemProductId)) {
                invoiceItemA = invoiceItem;
                assertAccountingTagsEqual(invoiceItemA, orderItemTagsA);
            } else if (productBId.equals(itemProductId)) {
                invoiceItemB = invoiceItem;
                assertAccountingTagsEqual(invoiceItemB, orderItemTagsB);
            } else {
                // all other invoice items are automatically tagged the same as the first invoice item
                assertAccountingTagsEqual(invoiceItem, invoiceItemFirst);
            }
        }
        assertNotNull("Could not find sales invoice item for productA in invoice [" + invoice.getInvoiceId() + "]", invoiceItemA);
        assertNotNull("Could not find sales invoice item for productB in invoice [" + invoice.getInvoiceId() + "]", invoiceItemB);

        // now check the accounting transactions for the sales invoice
        GenericValue invoiceTransaction = EntityUtil.getOnly(delegator.findByAnd("AcctgTrans", UtilMisc.toMap("invoiceId", invoice.getInvoiceId())));
        assertNotNull("Could not find the accounting transaction related to the invoice [" + invoice.getInvoiceId() + "]", invoiceTransaction);
        List<GenericValue> entries = invoiceTransaction.getRelated("AcctgTransEntry");
        List<String> accountedEntryIds = new ArrayList<String>();
        // find the specific transaction entries corresponding to the tagged invoice items in the SALES account and tax authorities
        // we have multiple entries per product, one being the order item itself, the others are its adjustments (taxes)
        String salesAccountId = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, "SALES_ACCOUNT", delegator);
        GenericValue transInvoiceItemAsales = EntityUtil.getOnly(delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", invoiceTransaction.get("acctgTransId"), "glAccountId", salesAccountId, "debitCreditFlag", "C", "productId", productAId)));
        GenericValue transInvoiceItemBsales = EntityUtil.getOnly(delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", invoiceTransaction.get("acctgTransId"), "glAccountId", salesAccountId, "debitCreditFlag", "C", "productId", productBId)));
        assertNotNull("Could not find the SALES accounting transaction entries for the sales invoice item [" + invoiceItemA + "]", transInvoiceItemAsales);
        assertNotNull("Could not find the SALES accounting transaction entries for the sales invoice item [" + invoiceItemB + "]", transInvoiceItemBsales);
        List<GenericValue> transInvoiceItemAs = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", invoiceTransaction.get("acctgTransId"), "roleTypeId", "TAX_AUTHORITY", "debitCreditFlag", "C", "productId", productAId));
        List<GenericValue> transInvoiceItemBs = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", invoiceTransaction.get("acctgTransId"), "roleTypeId", "TAX_AUTHORITY", "debitCreditFlag", "C", "productId", productBId));
        assertNotEmpty("Could not find the tax accounting transaction entries for the sales invoice item [" + invoiceItemA + "]", transInvoiceItemAs);
        assertNotEmpty("Could not find the tax accounting transaction entries for the sales invoice item [" + invoiceItemB + "]", transInvoiceItemBs);
        transInvoiceItemBs.add(transInvoiceItemBsales);
        transInvoiceItemAs.add(transInvoiceItemAsales);

        // find the global adjustments transaction entries, they would be tagged the same as the first invoice item
        List<GenericValue> transInvoiceItemOthers = delegator.findByCondition("AcctgTransEntry", EntityCondition.makeCondition(
                                                                EntityCondition.makeCondition("acctgTransId", invoiceTransaction.get("acctgTransId")),
                                                                EntityCondition.makeCondition("productId", null),
                                                                EntityCondition.makeCondition("debitCreditFlag", "C"))
                                                                              , null, null);
        if (firstInvoiceItemIsLikeA) {
            transInvoiceItemAs.addAll(transInvoiceItemOthers);
        } else {
            transInvoiceItemBs.addAll(transInvoiceItemOthers);
        }

        BigDecimal totalA = BigDecimal.ZERO;
        for (GenericValue entry : transInvoiceItemAs) {
            assertAccountingTagsEqual(entry, orderItemTagsA);
            totalA = totalA.add(entry.getBigDecimal("amount"));
            accountedEntryIds.add(entry.getString("acctgTransEntrySeqId"));
            Debug.logInfo("Added transaction from invoice item A : " + entry, MODULE);
        }
        BigDecimal totalB = BigDecimal.ZERO;
        for (GenericValue entry : transInvoiceItemBs) {
            assertAccountingTagsEqual(entry, orderItemTagsB);
            totalB = totalB.add(entry.getBigDecimal("amount"));
            accountedEntryIds.add(entry.getString("acctgTransEntrySeqId"));
            Debug.logInfo("Added transaction from invoice item B : " + entry, MODULE);
        }
        Debug.logInfo("totalA = " + totalA + ", totalB = " + totalB, MODULE);

        // find the specific transaction corresponding to the tagged invoice items in the ACCOUNTS_RECEIVABLE account
        // note: those do not have a product id set, instead they balance the previous two set of transaction entries and have the same amounts total
        String arAccountId = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, "ACCOUNTS_RECEIVABLE", delegator);
        GenericValue transArA = EntityUtil.getOnly(delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", invoiceTransaction.get("acctgTransId"), "glAccountId", arAccountId, "debitCreditFlag", "D", "amount", totalA)));
        GenericValue transArB = EntityUtil.getOnly(delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", invoiceTransaction.get("acctgTransId"), "glAccountId", arAccountId, "debitCreditFlag", "D", "amount", totalB)));
        assertNotNull("Could not find the ACCOUNTS_RECEIVABLE accounting transaction for the sales invoice item [" + invoiceItemA + "]", transArA);
        assertNotNull("Could not find the ACCOUNTS_RECEIVABLE accounting transaction for the sales invoice item [" + invoiceItemB + "]", transArB);
        // those transactions only have the tags defined in the ACCOUNTS_RECEIVABLE usage type (which is only the first tag)
        assertAccountingTagsEqual(transArA, UtilMisc.toMap("acctgTagEnumId1", orderItemTagsA.get("acctgTagEnumId1")));
        assertAccountingTagsEqual(transArB, UtilMisc.toMap("acctgTagEnumId1", orderItemTagsB.get("acctgTagEnumId1")));
        accountedEntryIds.add(transArA.getString("acctgTransEntrySeqId"));
        accountedEntryIds.add(transArB.getString("acctgTransEntrySeqId"));
        // we should have accounted for all the transaction entries
        for (GenericValue entry : entries) {
            String id = entry.getString("acctgTransEntrySeqId");
            assertTrue("Found unexpected transaction entry: " + entry, accountedEntryIds.contains(id));
        }

        // now check the accounting tags for the sent shipment, they should have the inventory items tags
        GenericValue shipmentTransaction = EntityUtil.getOnly(delegator.findByAnd("AcctgTrans", UtilMisc.toMap("shipmentId", shipmentId)));
        assertNotNull("Could not find the accounting transaction related to the shipment [" + shipmentId + "]", shipmentTransaction);
        entries = shipmentTransaction.getRelated("AcctgTransEntry");
        accountedEntryIds = new ArrayList<String>();
        // find the specific transaction corresponding to the tagged invoice items in the COGS account
        String cogsAccountId = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, "COGS_ACCOUNT", delegator);
        GenericValue transCogsA = EntityUtil.getOnly(delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", shipmentTransaction.get("acctgTransId"), "glAccountId", cogsAccountId, "debitCreditFlag", "D", "productId", productAId)));
        GenericValue transCogsB = EntityUtil.getOnly(delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", shipmentTransaction.get("acctgTransId"), "glAccountId", cogsAccountId, "debitCreditFlag", "D", "productId", productBId)));
        assertNotNull("Could not find the COGS accounting transaction for the sales invoice item [" + invoiceItemA + "]", transCogsA);
        assertNotNull("Could not find the COGS accounting transaction for the sales invoice item [" + invoiceItemB + "]", transCogsB);
        assertAccountingTagsEqual(transCogsA, inventoryA);
        assertAccountingTagsEqual(transCogsB, inventoryB);
        accountedEntryIds.add(transCogsA.getString("acctgTransEntrySeqId"));
        accountedEntryIds.add(transCogsB.getString("acctgTransEntrySeqId"));
        // find the specific transaction corresponding to the tagged inventory items in the INVENTORY account
        String inventoryAccountId = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, "INVENTORY_ACCOUNT", delegator);
        GenericValue transInventoryA = EntityUtil.getOnly(delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", shipmentTransaction.get("acctgTransId"), "glAccountId", inventoryAccountId, "debitCreditFlag", "C", "productId", productAId)));
        GenericValue transInventoryB = EntityUtil.getOnly(delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", shipmentTransaction.get("acctgTransId"), "glAccountId", inventoryAccountId, "debitCreditFlag", "C", "productId", productBId)));
        assertNotNull("Could not find the INVENTORY accounting transaction for the inventory item [" + inventoryA + "]", transInventoryA);
        assertNotNull("Could not find the INVENTORY accounting transaction for the inventory item [" + inventoryB + "]", transInventoryB);
        assertAccountingTagsEqual(transInventoryA, inventoryA);
        assertAccountingTagsEqual(transInventoryB, inventoryB);
        accountedEntryIds.add(transInventoryA.getString("acctgTransEntrySeqId"));
        accountedEntryIds.add(transInventoryB.getString("acctgTransEntrySeqId"));
        // for all other transactions, they should be un-tagged
        for (GenericValue entry : entries) {
            String id = entry.getString("acctgTransEntrySeqId");
            if (accountedEntryIds.contains(id)) {
                continue;
            } else {
                assertAccountingTagsEqual(entry, noTags);
            }
        }
    }

    /**
     * Check if we cancel order when some of order items w/ promo billed.
     * The following promo applies:
     * - Promo 9000 -> get a free WG-1111 for spending more than $100 in GZ/WG products
     * - Promo 9013 -> buy 3 get 2 free from WG (limit = 2)
     * - Promo 9011 -> 10% off the entire order
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCancelPartiallyBilledSalesOrderWithPromoItems() throws GeneralException {

        receiveInventoryProduct(WG1111, new BigDecimal("50.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("50.0"), demowarehouse1);

        // 1. make the order 10x WG-1111 + 10x WG-5569
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(WG1111, new BigDecimal("10.0"));
        orderItems.put(WG5569, new BigDecimal("10.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, DemoCustomer, productStoreId);
        String orderId = salesOrder.getOrderId();
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(salesOrder.getOrderId());

        // Find the order items
        OrderItem WG1111Item = null;
        OrderItem WG5569Item = null;
        for (OrderItem item : order.getOrderItems()) {
            if (WG1111.get("productId").equals(item.getProductId())) {
                WG1111Item = item;
            } else if (WG5569.get("productId").equals(item.getProductId())) {
                WG5569Item = item;
            }
        }
        assertNotNull("Did not find order item for WG-1111 in order [" + orderId + "]", WG1111Item);
        assertNotNull("Did not find order item for WG-5569 in order [" + orderId + "]", WG5569Item);

        // This is a hack to force the creation of promo items:
        //  Update the order with same quantities
        OpentapsUpdateOrderItemsService updateItems = new OpentapsUpdateOrderItemsService();
        updateItems.setInUserLogin(User);
        updateItems.setInOrderId(orderId);
        updateItems.setInItemQtyMap(UtilMisc.toMap(WG1111Item.getOrderItemSeqId() + ":00001", "10.0", WG5569Item.getOrderItemSeqId() + ":00001", "10.0"));
        updateItems.setInItemPriceMap(UtilMisc.toMap(WG1111Item.getOrderItemSeqId(), "59.99", WG5569Item.getOrderItemSeqId(), "48.0"));
        updateItems.setInOverridePriceMap(new HashMap());
        runAndAssertServiceSuccess(updateItems);

        assertNormalOrderItems(orderId, UtilMisc.toMap("WG-5569", new BigDecimal("10.0"), "WG-1111", new BigDecimal("10.0")));
        assertPromoItemExists(orderId, "WG-1111", new BigDecimal("1.0"), "9000", "ITEM_CREATED");

        // Find the order items
        order = repository.getOrderById(salesOrder.getOrderId());
        OrderItem WG1111ItemPromo = null;
        WG1111Item = null;
        WG5569Item = null;
        for (OrderItem item : order.getOrderItems()) {
            if (WG1111.get("productId").equals(item.getProductId())) {
                if (item.isPromo()) {
                    WG1111ItemPromo = item;
                } else {
                    WG1111Item = item;
                }
            } else if (WG5569.get("productId").equals(item.getProductId())) {
                WG5569Item = item;
            }
        }
        assertNotNull("Did not find order item for WG-1111 in order [" + orderId + "]", WG1111Item);
        assertNotNull("Did not find promo order item for WG-1111 in order [" + orderId + "]", WG1111ItemPromo);
        assertNotNull("Did not find order item for WG-5569 in order [" + orderId + "]", WG5569Item);

        // check order adjustments at this point
        // Order items sub total should be:
        //  10 - 4 free WG-1111 = 359.94
        //  10 x WG-5569 = 480.00
        //  = 839.94
        assertEquals("Order [" + orderId + "] item WG1111 sub total incorrect.", WG1111Item.getSubTotal(), new BigDecimal("359.94"));
        assertEquals("Order [" + orderId + "] item WG5569 sub total incorrect.", WG5569Item.getSubTotal(), new BigDecimal("480.00"));
        assertEquals("Order [" + orderId + "] item WG1111 (promo) sub total incorrect.", WG1111ItemPromo.getSubTotal(), BigDecimal.ZERO);
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("839.94"));
        // Tax amount should be 5.75% of this total:
        //  1 + 4.75 + 0.1 % of 359.94 = 3.599 + 17.097 + 0.360 -> 21.056 rounded to 21.06
        //  1 + 4.75 + 0.1 % of 480.00 = 4.800 + 22.800 + 0.480 -> 28.080 rounded to 28.08
        //= 1 + 4.75 + 0.1 % of 839.94 = 8.399 + 39.897 + 0.840 -> 49.14 | minus 0.1% of 83.99 -> 0.08 ==> 49.06
        assertEquals("Order [" + orderId + "] item WG1111 taxes incorrect.", WG1111Item.getTaxAmount(), new BigDecimal("21.06"));
        assertEquals("Order [" + orderId + "] item WG5569 taxes incorrect.", WG5569Item.getTaxAmount(), new BigDecimal("28.08"));
        assertEquals("Order [" + orderId + "] item WG1111 (promo) taxes incorrect.", WG1111ItemPromo.getTaxAmount(), BigDecimal.ZERO);
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("49.06"));
        // Order global promotion should be:
        //  10% off order total = 10 % of (839.94) = 83.99
        assertEquals("Order [" + orderId + "] global adjustment incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-83.99"));

        // 2. ship 1x WG-1111
        Map<String, Map<String, BigDecimal>> itemsToPack = FastMap.newInstance();
        itemsToPack.put("00001", UtilMisc.toMap(WG1111Item.getOrderItemSeqId(), new BigDecimal("1.0")));
        TestShipOrderManualService ship = new TestShipOrderManualService();
        ship.setInUserLogin(admin);
        ship.setInOrderId(order.getOrderId());
        ship.setInFacilityId(facilityId);
        ship.setInItems(itemsToPack);
        runAndAssertServiceSuccess(ship);

        // 3. cancel 8x WG-1111
        CancelOrderItemService cancelOrderItem = new CancelOrderItemService();
        cancelOrderItem.setInUserLogin(User);
        cancelOrderItem.setInOrderId(orderId);
        cancelOrderItem.setInOrderItemSeqId(WG1111Item.getOrderItemSeqId());
        cancelOrderItem.setInShipGroupSeqId("00001");
        cancelOrderItem.setInCancelQuantity(new BigDecimal("8.0"));
        runAndAssertServiceSuccess(cancelOrderItem);

        // Find the order items
        order = repository.getOrderById(salesOrder.getOrderId());
        WG1111ItemPromo = null;
        WG1111Item = null;
        WG5569Item = null;
        for (OrderItem item : order.getOrderItems()) {
            if (WG1111.get("productId").equals(item.getProductId())) {
                if (item.isPromo()) {
                    WG1111ItemPromo = item;
                } else {
                    WG1111Item = item;
                }
            } else if (WG5569.get("productId").equals(item.getProductId())) {
                WG5569Item = item;
            }
        }
        assertNotNull("Did not find order item for WG-1111 in order [" + orderId + "]", WG1111Item);
        assertNotNull("Did not find promo order item for WG-1111 in order [" + orderId + "]", WG1111ItemPromo);
        assertNotNull("Did not find order item for WG-5569 in order [" + orderId + "]", WG5569Item);

        // check order adjustments at this point
        //  after the cancel the promo giving 4 free WG-1111 should be canceled
        // Order items sub total should be:
        //  2  x WG-1111 = 119.98
        //  10 x WG-5569 = 480.00
        //  = 599.98
        assertEquals("Order [" + orderId + "] item WG1111 sub total incorrect.", WG1111Item.getSubTotal(), new BigDecimal("119.98"));
        assertEquals("Order [" + orderId + "] item WG5569 sub total incorrect.", WG5569Item.getSubTotal(), new BigDecimal("480.00"));
        assertEquals("Order [" + orderId + "] item WG1111 (promo) sub total incorrect.", WG1111ItemPromo.getSubTotal(), BigDecimal.ZERO);
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("599.98"));
        // Tax amount should be 5.75% of this total:
        //  1 + 4.75 + 0.1 % of 119.98 = 1.200 + 5.699 + 0.120  -> 7.019 rounded to 7.02
        //  1 + 4.75 + 0.1 % of 480.00 = 4.800 + 22.800 + 0.480 -> 28.08 rounded to 28.08
        //= 1 + 4.75 + 0.1 % of 599.98 = 6.000 + 28.499 + 0.600 -> 35.10 | minus 0.1% of 60.0 -> 0.06 ==> 35.04
        assertEquals("Order [" + orderId + "] item WG1111 taxes incorrect.", WG1111Item.getTaxAmount(), new BigDecimal("7.02"));
        assertEquals("Order [" + orderId + "] item WG5569 taxes incorrect.", WG5569Item.getTaxAmount(), new BigDecimal("28.08"));
        assertEquals("Order [" + orderId + "] item WG1111 (promo) taxes incorrect.", WG1111ItemPromo.getTaxAmount(), BigDecimal.ZERO);
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("35.04"));
        // Order global promotion should be:
        //  10% off order total = 10 % of (599.98) = 60.00
        assertEquals("Order [" + orderId + "] global adjustment incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-60.00"));

        // 4. cancel the remaining normal WG-1111
        cancelOrderItem = new CancelOrderItemService();
        cancelOrderItem.setInUserLogin(User);
        cancelOrderItem.setInOrderId(orderId);
        cancelOrderItem.setInOrderItemSeqId(WG1111Item.getOrderItemSeqId());
        cancelOrderItem.setInShipGroupSeqId("00001");
        cancelOrderItem.setInCancelQuantity(new BigDecimal("1.0"));
        runAndAssertServiceSuccess(cancelOrderItem);

        // Find the order items
        order = repository.getOrderById(salesOrder.getOrderId());
        WG1111ItemPromo = null;
        WG1111Item = null;
        WG5569Item = null;
        for (OrderItem item : order.getOrderItems()) {
            if (WG1111.get("productId").equals(item.getProductId())) {
                if (item.isPromo()) {
                    WG1111ItemPromo = item;
                } else {
                    WG1111Item = item;
                }
            } else if (WG5569.get("productId").equals(item.getProductId())) {
                WG5569Item = item;
            }
        }
        assertNotNull("Did not find order item for WG-1111 in order [" + orderId + "]", WG1111Item);
        assertNotNull("Did not find promo order item for WG-1111 in order [" + orderId + "]", WG1111ItemPromo);
        assertNotNull("Did not find order item for WG-5569 in order [" + orderId + "]", WG5569Item);

        // check order adjustments at this point
        // Order items sub total should be:
        //  1  x WG-1111 = 59.99
        //  10 x WG-5569 = 480.00
        //  = 539.99
        assertEquals("Order [" + orderId + "] item WG1111 sub total incorrect.", WG1111Item.getSubTotal(), new BigDecimal("59.99"));
        assertEquals("Order [" + orderId + "] item WG5569 sub total incorrect.", WG5569Item.getSubTotal(), new BigDecimal("480.00"));
        assertEquals("Order [" + orderId + "] item WG1111 (promo) sub total incorrect.", WG1111ItemPromo.getSubTotal(), BigDecimal.ZERO);
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("539.99"));
        // Tax amount should be 5.75% of this total:
        //  1 + 4.75 + 0.1 % of 59.99  = 0.600 + 2.850 + 0.06  -> 3.51   rounded to 3.51
        //  1 + 4.75 + 0.1 % of 480.00 = 4.800 + 22.800 + 0.48 -> 28.08  rounded to 28.08
        //= 1 + 4.75 + 0.1 % of 539.99 = 5.400 + 25.650 + 0.54 -> 31.59 | minus 0.1% of 54.0 -> 0.05 ==> 31.54 rounded to 31.54
        assertEquals("Order [" + orderId + "] item WG1111 taxes incorrect.", WG1111Item.getTaxAmount(), new BigDecimal("3.51"));
        assertEquals("Order [" + orderId + "] item WG5569 taxes incorrect.", WG5569Item.getTaxAmount(), new BigDecimal("28.08"));
        assertEquals("Order [" + orderId + "] item WG1111 (promo) taxes incorrect.", WG1111ItemPromo.getTaxAmount(), BigDecimal.ZERO);
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("31.54"));
        // Order global promotion should be:
        //  10% off order total = 10 % of (539.99) = 54.00
        assertEquals("Order [" + orderId + "] global adjustment incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-54.00"));

        // 5. cancel 10 remaining WG-5569
        cancelOrderItem = new CancelOrderItemService();
        cancelOrderItem.setInUserLogin(User);
        cancelOrderItem.setInOrderId(orderId);
        cancelOrderItem.setInOrderItemSeqId(WG5569Item.getOrderItemSeqId());
        cancelOrderItem.setInShipGroupSeqId("00001");
        cancelOrderItem.setInCancelQuantity(new BigDecimal("10.0"));
        runAndAssertServiceSuccess(cancelOrderItem);

        // Find the order items
        order = repository.getOrderById(salesOrder.getOrderId());
        WG1111ItemPromo = null;
        WG1111Item = null;
        WG5569Item = null;
        for (OrderItem item : order.getOrderItems()) {
            if (WG1111.get("productId").equals(item.getProductId())) {
                if (item.isPromo()) {
                    WG1111ItemPromo = item;
                } else {
                    WG1111Item = item;
                }
            } else if (WG5569.get("productId").equals(item.getProductId())) {
                WG5569Item = item;
            }
        }
        assertNotNull("Did not find order item for WG-1111 in order [" + orderId + "]", WG1111Item);
        assertNotNull("Did not find promo order item for WG-1111 in order [" + orderId + "]", WG1111ItemPromo);
        assertNotNull("Did not find order item for WG-5569 in order [" + orderId + "]", WG5569Item);

        // check order adjustments at this point
        //  after the cancel the promo giving 1 WG-1111 should be canceled, and the promo item should be canceled too
        // Order items sub total should be:
        //  1 x WG-1111 = 59.99
        //  = 59.99
        assertTrue("Order [" + orderId + "] item WG1111 (promo) should be canceled.", WG1111ItemPromo.isCancelled());
        assertTrue("Order [" + orderId + "] item WG5569 should be canceled.", WG5569Item.isCancelled());

        assertEquals("Order [" + orderId + "] item WG1111 sub total incorrect.", WG1111Item.getSubTotal(), new BigDecimal("59.99"));
        assertEquals("Order [" + orderId + "] items sub total incorrect.", order.getItemsSubTotal(), new BigDecimal("59.99"));
        // Tax amount should be 5.75% of this total:
        //  1 + 4.75 + 0.1 % of 119.98 = 0.600 + 2.850 + 0.06 -> 3.51 rounded to 3.51
        //  | minus 0.1% of 6.00 -> 0.01 ==> 3.5
        assertEquals("Order [" + orderId + "] item WG1111 taxes incorrect.", WG1111Item.getTaxAmount(), new BigDecimal("3.51"));
        assertEquals("Order [" + orderId + "] tax incorrect.", order.getTaxAmount(), new BigDecimal("3.50"));
        // Order global promotion should be:
        //  10% off order total = 10 % of (59.99) = 6.00 rounded to 6.00
        assertEquals("Order [" + orderId + "] global adjustment incorrect.", order.getOtherAdjustmentsAmount(), new BigDecimal("-6.00"));

        // check the whole order is now completed
        assertOrderCompleted(orderId);
    }

    /**
     * Test that the cancelOrderItem service correctly account for already shipped item.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCancelRemainingItems() throws GeneralException {

        receiveInventoryProduct(WG1111, new BigDecimal("50.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("50.0"), demowarehouse1);

        // get initial ATP and QOH
        Map<String, Object> callResults = getProductAvailability(WG1111.getString("productId"));
        BigDecimal initAtp = (BigDecimal) callResults.get("availableToPromiseTotal");
        BigDecimal initQoh = (BigDecimal) callResults.get("quantityOnHandTotal");
        Debug.logInfo("Initial ATP : " + initAtp + ", Initial QOH : " + initQoh, MODULE);

        // 1. make the order 10x WG-1111
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(WG1111, new BigDecimal("10.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, DemoCustomer, productStoreId);
        String orderId = salesOrder.getOrderId();
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(salesOrder.getOrderId());

        // Find the order item
        OrderItem WG1111Item = null;
        for (OrderItem item : order.getOrderItems()) {
            if (WG1111.get("productId").equals(item.getProductId())) {
                WG1111Item = item;
            }
        }
        assertNotNull("Did not find order item for WG-1111 in order [" + orderId + "]", WG1111Item);

        // This is a hack to force the creation of promo items:
        //  Update the order with same quantities
        OpentapsUpdateOrderItemsService updateItems = new OpentapsUpdateOrderItemsService();
        updateItems.setInUserLogin(User);
        updateItems.setInOrderId(orderId);
        updateItems.setInItemQtyMap(UtilMisc.toMap(WG1111Item.getOrderItemSeqId() + ":00001", "10.0"));
        updateItems.setInItemPriceMap(UtilMisc.toMap(WG1111Item.getOrderItemSeqId(), "59.99"));
        updateItems.setInOverridePriceMap(new HashMap());
        runAndAssertServiceSuccess(updateItems);

        assertNormalOrderItems(orderId, UtilMisc.toMap("WG-1111", new BigDecimal("10.0")));
        assertPromoItemExists(orderId, "WG-1111", new BigDecimal("1.0"), "9000", "ITEM_CREATED");

        // Find the order items
        order = repository.getOrderById(salesOrder.getOrderId());
        OrderItem WG1111ItemPromo = null;
        WG1111Item = null;
        for (OrderItem item : order.getOrderItems()) {
            if (WG1111.get("productId").equals(item.getProductId())) {
                if (item.isPromo()) {
                    WG1111ItemPromo = item;
                } else {
                    WG1111Item = item;
                }
            }
        }
        assertNotNull("Did not find order item for WG-1111 in order [" + orderId + "]", WG1111Item);
        assertNotNull("Did not find promo order item for WG-1111 in order [" + orderId + "]", WG1111ItemPromo);

        // 2. ship 1x WG-1111
        Map<String, Map<String, BigDecimal>> itemsToPack = FastMap.newInstance();
        itemsToPack.put("00001", UtilMisc.toMap(WG1111Item.getOrderItemSeqId(), new BigDecimal("1.0")));
        TestShipOrderManualService ship = new TestShipOrderManualService();
        ship.setInUserLogin(admin);
        ship.setInOrderId(order.getOrderId());
        ship.setInFacilityId(facilityId);
        ship.setInItems(itemsToPack);
        runAndAssertServiceSuccess(ship);

        // 3. cancel the remaining by not specifying the quantity
        CancelOrderItemService cancelOrderItem = new CancelOrderItemService();
        cancelOrderItem.setInUserLogin(User);
        cancelOrderItem.setInOrderId(orderId);
        cancelOrderItem.setInOrderItemSeqId(WG1111Item.getOrderItemSeqId());
        cancelOrderItem.setInShipGroupSeqId("00001");
        runAndAssertServiceSuccess(cancelOrderItem);

        // Find the order items
        order = repository.getOrderById(salesOrder.getOrderId());
        WG1111ItemPromo = null;
        WG1111Item = null;
        for (OrderItem item : order.getOrderItems()) {
            if (WG1111.get("productId").equals(item.getProductId())) {
                if (item.isPromo()) {
                    WG1111ItemPromo = item;
                } else {
                    WG1111Item = item;
                }
            }
        }
        assertNotNull("Did not find order item for WG-1111 in order [" + orderId + "]", WG1111Item);
        assertNotNull("Did not find promo order item for WG-1111 in order [" + orderId + "]", WG1111ItemPromo);

        assertOrderItemStatus("Order item WG-1111 should be COMPLETED once we canceled the remaining items", orderId, "00001", "ITEM_COMPLETED");
        assertOrderItemStatus("Promo Order item should be cancelled after cancelling the remaining items", orderId, WG1111ItemPromo.getOrderItemSeqId(), "ITEM_CANCELLED");

        // check the whole order is now completed
        assertOrderCompleted(orderId);

        // check the reservations correctly accounted for the issued quantity as well
        callResults = getProductAvailability(WG1111.getString("productId"));
        BigDecimal finalAtp = (BigDecimal) callResults.get("availableToPromiseTotal");
        BigDecimal finalQoh = (BigDecimal) callResults.get("quantityOnHandTotal");
        Debug.logInfo("Final ATP : " + finalAtp + ", Final QOH : " + finalQoh, MODULE);

        // verify that ATP has changed by -1 and QOH has changed by -1
        assertEquals("ATP has changed by -1", finalAtp, initAtp.subtract(new BigDecimal("1.0")));
        assertEquals("QOH has changed by -1", finalQoh, initQoh.subtract(new BigDecimal("1.0")));
    }

    /**
     * Test the GWT order lookup.
     * @throws Exception if an error occurs
     */
    public void testGwtOrderLookup() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);

        // 1. test find order by orderId
        provider.setParameter(SalesOrderLookupConfiguration.INOUT_ORDER_ID, "TEST10000");
        SalesOrderLookupService lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertEquals("There should just found one record with order Id [TEST10000].", 1, lookup.getResultTotalCount());
        assertGwtLookupFound(lookup, Arrays.asList("TEST10000"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 2. test find order by customer
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.INOUT_PARTY_ID, "DemoAccount1");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        // test we found the demo data: TEST10000/TEST10001
        assertGwtLookupFound(lookup, Arrays.asList("TEST10000", "TEST10001"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST10002"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 3. test find order by PO#
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.INOUT_CORRESPONDING_PO_ID, "PO10001");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        // test we found the demo data: TEST10002, not found TEST10000/TEST10001
        assertGwtLookupFound(lookup, Arrays.asList("TEST10002"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST10000"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST10001"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 4. test find order by status
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.INOUT_STATUS_ID, "ORDER_APPROVED");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        // test we found the demo data: TEST10000/TEST10002, not found TEST10001
        assertGwtLookupFound(lookup, Arrays.asList("TEST10000", "TEST10002"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST10001"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);


        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.INOUT_STATUS_ID, "ORDER_CREATED");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        // test we found the demo data: TEST10001, not found TEST10000/TEST10002
        assertGwtLookupFound(lookup, Arrays.asList("TEST10001"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST10000"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST10002"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 5. test find order by date range
        // search the orders between 09/10/15 00:00:00 and 09/10/15 23:59:59
        provider = new TestInputProvider(admin, dispatcher);
        String fromDate = dateStringToShortLocaleString("09/10/15 00:00:00", "yy/MM/dd HH:mm:ss");
        String thruDate = dateStringToShortLocaleString("09/10/15 23:59:59", "yy/MM/dd HH:mm:ss");
        provider.setParameter(SalesOrderLookupConfiguration.IN_FROM_DATE, fromDate);
        provider.setParameter(SalesOrderLookupConfiguration.IN_THRU_DATE, thruDate);
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        // test we found the demo data: TEST10000/TEST10002, not found TEST10001
        assertGwtLookupFound(lookup, Arrays.asList("TEST10000", "TEST10002"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST10001"), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

    }

    /**
     * Test the GWT search order by shipping address.
     * @throws Exception if an error occurs
     */
    public void testGwtSearchOrderByShippingAddress() throws Exception {

        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);
        // 1. Create a sales order with a billing address in New York and a shipping address in California

        // create a product
        BigDecimal productQty = new BigDecimal("1.0");
        BigDecimal productUnitPrice = new BigDecimal("11.11");
        final GenericValue testProduct = createTestProduct("testGwtSearchOrderByShippingAddress Test Product", demowarehouse1);
        assignDefaultPrice(testProduct, productUnitPrice, admin);
        Debug.logInfo("create tests product [" + testProduct.getString("productId") + "] for testGwtSearchOrderByShippingAddress", MODULE);
        // create a sales order for the DemoAccount1 and product
        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(testProduct, productQty);
        User = DemoCSR;

        String customerPartyId = createPartyFromTemplate(DemoCustomer.getString("partyId"), "Customer for testGwtSearchOrderByShippingAddress");
        Debug.logInfo("create customer [" + customerPartyId + "]", MODULE);
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));
        SalesOrderFactory salesOrder =  testCreatesSalesOrder(orderSpec, customer.getString("partyId"), productStoreId, null, "EXT_OFFLINE", null, "DemoAddress1", "DemoAddress2");
        Debug.logInfo("create sales order [" + salesOrder.getOrderId() + "]", MODULE);

        // 2. try to find the sales order By shipping address with state = NY, in the sales order is not found
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.IN_SHIPPING_STATE, "NY");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999");
        SalesOrderLookupService lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertGwtLookupNotFound(lookup, Arrays.asList(salesOrder.getOrderId()), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 3. try to find the sales order by shipping address with state = CA, and the sales order is found
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.IN_SHIPPING_STATE, "CA");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999");
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertGwtLookupFound(lookup, Arrays.asList(salesOrder.getOrderId()), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 4. try to find the sales order By shipping address with toName = Demo Account No. 1, in the sales order is found
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.IN_SHIPPING_TO_NAME, "Demo Account No. 1");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999");
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertGwtLookupFound(lookup, Arrays.asList(salesOrder.getOrderId()), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 5. try to find the sales order by shipping address with city = Los Angeles, and the sales order is found
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.IN_SHIPPING_CITY, "Los Angeles");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999");
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertGwtLookupFound(lookup, Arrays.asList(salesOrder.getOrderId()), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 6. try to find the sales order By shipping address with address = 251 West 30th Street, in the sales order is not found
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.IN_SHIPPING_ADDRESS, "251 West 30th Street");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999");
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertGwtLookupNotFound(lookup, Arrays.asList(salesOrder.getOrderId()), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 7. try to find the sales order by shipping address with address = 12345 Wilshire Blvd, and the sales order is found
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.IN_SHIPPING_ADDRESS, "12345 Wilshire Blvd");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999");
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertGwtLookupFound(lookup, Arrays.asList(salesOrder.getOrderId()), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 8. try to find the sales order By shipping address with postal code = 10001, in the sales order is not found
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.IN_SHIPPING_POSTAL_CODE, "10001");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999");
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertGwtLookupNotFound(lookup, Arrays.asList(salesOrder.getOrderId()), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        // 9. try to find the sales order by shipping address with postal code = 90025, and the sales order is found
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.IN_SHIPPING_POSTAL_CODE, "90025");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999");
        lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertGwtLookupFound(lookup, Arrays.asList(salesOrder.getOrderId()), SalesOrderLookupConfiguration.INOUT_ORDER_ID);
    }

    /**
     * Test the GWT search order by product.
     * @throws Exception if an error occurs
     */
    public void testGwtSearchOrderByProduct() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);
        // 1. Create a sales order with a billing address in New York and a shipping address in California

        // create a product
        BigDecimal productQty = new BigDecimal("1.0");
        BigDecimal productUnitPrice = new BigDecimal("11.11");
        final GenericValue testProduct = createTestProduct("testGwtSearchOrderByProduct Test Product", demowarehouse1);
        assignDefaultPrice(testProduct, productUnitPrice, admin);
        Debug.logInfo("create tests product [" + testProduct.getString("productId") + "] for testGwtSearchOrderByProduct", MODULE);
        // create a sales order for the DemoAccount1 and product
        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(testProduct, productQty);
        User = DemoCSR;

        String customerPartyId = createPartyFromTemplate(DemoCustomer.getString("partyId"), "Customer for testGwtSearchOrderByShippingAddress");
        Debug.logInfo("create customer [" + customerPartyId + "]", MODULE);
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));
        SalesOrderFactory salesOrder =  testCreatesSalesOrder(orderSpec, customer.getString("partyId"), productStoreId, null, "EXT_OFFLINE", null, "DemoAddress1", "DemoAddress2");
        Debug.logInfo("create sales order [" + salesOrder.getOrderId() + "]", MODULE);
        
        // 2. try to find the sales order By the productId, in the sales order is found
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(SalesOrderLookupConfiguration.IN_PRODUCT_ID, testProduct.getString("productId"));
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999");
        SalesOrderLookupService lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertGwtLookupFound(lookup, Arrays.asList(salesOrder.getOrderId()), SalesOrderLookupConfiguration.INOUT_ORDER_ID);

    }
}
