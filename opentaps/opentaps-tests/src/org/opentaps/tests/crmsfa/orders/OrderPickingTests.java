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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderItemShipGrpInvRes;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.tests.warehouse.InventoryAsserts;

/**
 * Order Picking related unit tests.
 */
public class OrderPickingTests  extends OrderTestCase {

    private static final String MODULE = OrderTests.class.getName();

    private GenericValue demoCustomer;
    private GenericValue demoCsr;
    private GenericValue demowarehouse1;
    private static final String productStoreId = "9000";
    private static final String facilityId = "WebStoreWarehouse";
    private static final String facilityId1 = "MyRetailStore";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        demoCustomer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoCustomer"));
        demoCsr = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "DemoCSR"));
        demowarehouse1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        assertNotNull("DemoCustomer not null", demoCustomer);
        assertNotNull("DemoCSR not null", demoCsr);
        assertNotNull("demowarehouse1 not null", demowarehouse1);
        User = demoCsr;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        demowarehouse1 = null;
        demoCsr = null;
        demoCustomer = null;
    }

    /**
     * This test verifies creating picklists from orders with allowOrderSplit = N.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testBasicOrderPicking() throws GeneralException {
        OrderRepositoryInterface repository = getOrderRepository(admin);
        // create 2 test products
        GenericValue productA = createTestProduct("Test Product A for testMultiFacilityOrder()", demowarehouse1);
        GenericValue productB = createTestProduct("Test Product B for testMultiFacilityOrder()", demowarehouse1);
        // create a customer from template of DemoCustomer
        String customerPartyId = createPartyFromTemplate(demoCustomer.getString("partyId"), "Customer for testMultiFacilityOrder");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));
        // create a sales order of 5 x productA and 8 x productB, approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(productA, new BigDecimal("5.0"));
        orderItems.put(productB, new BigDecimal("8.0"));
        User = demoCsr;
        // record each store findOrdersToPickMove initial number
        Map results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long webStoreReadyToPickInitNumber = (Long) results.get("nReturnedOrders");

        // create a sales order and approve it
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, customer, productStoreId);
        salesOrder.approveOrder();
        Order order = repository.getOrderById(salesOrder.getOrderId());
        Debug.logInfo("testMultiFacilityOrder() create orderId: " + salesOrder.getOrderId(), MODULE);

        // at this point, there is no inventory for this order, so it should not appear on the picklist.
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should not have increased in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", webStoreReadyToPickInitNumber, webStoreReadyToPickCurrentNumber);

        // receive inventory for the first product
        receiveInventoryProduct(productA, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);

        // at this point, there is some inventory for this order but allowOrderSplit = N, so it should not appear on the picklist.
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should not have increased in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", webStoreReadyToPickInitNumber, webStoreReadyToPickCurrentNumber);

        // receive inventory for the second product
        receiveInventoryProduct(productB, new BigDecimal("8.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);

        // at this point, there is all the needed inventory for this order, so it should appear on the picklist.
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should have increased by 1 in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", new Long(webStoreReadyToPickInitNumber + 1), webStoreReadyToPickCurrentNumber);

        // create the picklist
        results = runAndAssertServiceSuccess("createPicklistFromOrders", UtilMisc.toMap("orderIdList", UtilMisc.toList(order.getOrderId()), "facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        String picklistId  = (String) results.get("picklistId");

        // check the picklist contains items for productA and productB and with correct quantity
        List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("pPicklistId", EntityOperator.EQUALS, picklistId));
        List<GenericValue> picklistItems = delegator.findByCondition("PicklistItemAndOdrItmShipGrp", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, UtilMisc.toList("piOrderId", "piShipGroupSeqId", "oiProductId"));
        boolean have5ProductA = false;
        boolean have8ProductB = false;

        for (GenericValue item : picklistItems) {
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productA.getString("productId"))
                && item.getBigDecimal("piQuantity").compareTo(new BigDecimal("5.0")) == 0) {
                        have5ProductA = true;
            }
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productB.getString("productId"))
                && item.getBigDecimal("piQuantity").compareTo(new BigDecimal("8.0")) == 0) {
                have8ProductB = true;
            }
         }
        assertTrue("should have 5 x productA in the picklist", have5ProductA);
        assertTrue("should have 8 x productB in the picklist", have8ProductB);
    }

    /**
     * This test verifies after a picklist is created, the order does not appear any more in the ready to pick, but once the picklist is canceled it appears again.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCancelPicklistOrderPicking() throws GeneralException {
        OrderRepositoryInterface repository = getOrderRepository(admin);
        // create 2 test products
        GenericValue productA = createTestProduct("Test Product A for testMultiFacilityOrder()", demowarehouse1);
        GenericValue productB = createTestProduct("Test Product B for testMultiFacilityOrder()", demowarehouse1);
        // create a customer from template of DemoCustomer
        String customerPartyId = createPartyFromTemplate(demoCustomer.getString("partyId"), "Customer for testMultiFacilityOrder");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));
        // create a sales order of 5 x productA and 8 x productB, approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(productA, new BigDecimal("5.0"));
        orderItems.put(productB, new BigDecimal("8.0"));
        User = demoCsr;
        // record each store findOrdersToPickMove initial number
        Map results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long webStoreReadyToPickInitNumber = (Long) results.get("nReturnedOrders");

        // create a sales order and approve it
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, customer, productStoreId);
        salesOrder.approveOrder();
        Order order = repository.getOrderById(salesOrder.getOrderId());
        Debug.logInfo("testMultiFacilityOrder() create orderId: " + salesOrder.getOrderId(), MODULE);

        // receive inventory
        receiveInventoryProduct(productA, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        receiveInventoryProduct(productB, new BigDecimal("8.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);

        // at this point, there is all the needed inventory for this order, so it should appear on the picklist.
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should have increased by 1 in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", new Long(webStoreReadyToPickInitNumber + 1), webStoreReadyToPickCurrentNumber);

        // create the picklist
        results = runAndAssertServiceSuccess("createPicklistFromOrders", UtilMisc.toMap("orderIdList", UtilMisc.toList(order.getOrderId()), "facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        String picklistId  = (String) results.get("picklistId");

        // check the picklist contains items for productA and productB and with correct quantity
        List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("pPicklistId", EntityOperator.EQUALS, picklistId));
        List<GenericValue> picklistItems = delegator.findByCondition("PicklistItemAndOdrItmShipGrp", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, UtilMisc.toList("piOrderId", "piShipGroupSeqId", "oiProductId"));
        boolean have5ProductA = false;
        boolean have8ProductB = false;

        for (GenericValue item : picklistItems) {
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productA.getString("productId"))
                    && item.getBigDecimal("piQuantity").compareTo(new BigDecimal("5.0")) == 0) {
                        have5ProductA = true;
            }
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productB.getString("productId"))
                    && item.getBigDecimal("piQuantity").compareTo(new BigDecimal("8.0")) == 0) {
                have8ProductB = true;
            }
        }
        assertTrue("should have 5 x productA in the picklist", have5ProductA);
        assertTrue("should have 8 x productB in the picklist", have8ProductB);

        // now that the picklist is created, the order should not appear as ready to pick
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should have not have increased in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", webStoreReadyToPickInitNumber, webStoreReadyToPickCurrentNumber);

        // cancel the previous picklist
        results = runAndAssertServiceSuccess("updatePicklist", UtilMisc.toMap("picklistId", picklistId, "statusId", "PICKLIST_CANCELLED", "userLogin", admin));

        // check the picklist contains the order again
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should have increased by 1 in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", new Long(webStoreReadyToPickInitNumber + 1), webStoreReadyToPickCurrentNumber);
    }

    /**
     * This test verifies creating picklists from orders with allowOrderSplit = Y and where only some items are available.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPartialOrderPicking() throws GeneralException {
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        OrderRepositoryInterface repository = getOrderRepository(admin);
        // create 2 test products
        GenericValue productA = createTestProduct("Test Product A for testPartialOrderPicking()", demowarehouse1);
        GenericValue productB = createTestProduct("Test Product B for testPartialOrderPicking()", demowarehouse1);
        assignDefaultPrice(productA, new BigDecimal("1.0"), admin);
        assignDefaultPrice(productB, new BigDecimal("1.0"), admin);
        final String productAId = productA.getString("productId");
        final String productBId = productB.getString("productId");
        // create a customer from template of DemoCustomer
        String customerPartyId = createPartyFromTemplate(demoCustomer.getString("partyId"), "Customer for testMultiFacilityOrder");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));
        // create a sales order of 5 x productA and 8 x productB, approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(productA, new BigDecimal("5.0"));
        orderItems.put(productB, new BigDecimal("8.0"));
        User = demoCsr;
        // record each store findOrdersToPickMove initial number
        Map results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long webStoreReadyToPickInitNumber = (Long) results.get("nReturnedOrders");
        // get initial inventory
        final Map originalInventoryA = inventoryAsserts.getInventory(productAId);
        final Map originalInventoryB = inventoryAsserts.getInventory(productBId);

        // create a sales order and approve it
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, customer, productStoreId);
        salesOrder.approveOrder();
        Order order = repository.getOrderById(salesOrder.getOrderId());
        Debug.logInfo("testMultiFacilityOrder() create orderId: " + salesOrder.getOrderId(), MODULE);

        // get the order items for each product
        OrderItem itemA = null, itemB = null;
        for (OrderItem item : order.getItems()) {
            if (productAId.equals(item.getProductId())) {
                itemA = item;
            } else if (productBId.equals(item.getProductId())) {
                itemB = item;
            }
        }
        assertNotNull("Did not find order item for product A", itemA);
        assertNotNull("Did not find order item for product B", itemB);

        // set allowOrderSplit to Y
        // get the ship group first
        GenericValue shipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId", "00001"));
        runAndAssertServiceSuccess("updateOrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId", "00001", "contactMechId", shipGroup.get("contactMechId"), "contactMechPurposeTypeId", "SHIPPING_LOCATION", "maySplit", "Y", "userLogin", admin));

        // at this point, there is no inventory for this order, so it should not appear on the picklist.
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should not have increased in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", webStoreReadyToPickInitNumber, webStoreReadyToPickCurrentNumber);

        // check the order reservations before receiving the items

        // Web Store 00001: 5 x productA, 8 x productB
        Map<String, Map<String, List<BigDecimal>>> expectedWebStoreItems = new HashMap<String, Map<String, List<BigDecimal>>>();
        Map<String, List<BigDecimal>> expectedProducts = new HashMap<String, List<BigDecimal>>();
        expectedProducts.put(productAId, Arrays.asList(new BigDecimal("5.0"), new BigDecimal("5.0")));
        expectedProducts.put(productBId, Arrays.asList(new BigDecimal("8.0"), new BigDecimal("8.0")));
        expectedWebStoreItems.put("00001", expectedProducts);
        assertShipGroupReservationsAndQuantities(order.getOrderId(), facilityId, expectedWebStoreItems);
        // check inventories ATP changed due to the reservations
        inventoryAsserts.assertInventoryChange(productAId, BigDecimal.ZERO, new BigDecimal("-5.0"), originalInventoryA);
        inventoryAsserts.assertInventoryChange(productBId, BigDecimal.ZERO, new BigDecimal("-8.0"), originalInventoryB);

        // receive part of the inventory
        receiveInventoryProduct(productA, new BigDecimal("2.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        receiveInventoryProduct(productB, new BigDecimal("3.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        // check inventory
        inventoryAsserts.assertInventoryChange(productAId, new BigDecimal("2.0"), new BigDecimal("-3.0"), originalInventoryA);
        inventoryAsserts.assertInventoryChange(productBId, new BigDecimal("3.0"), new BigDecimal("-5.0"), originalInventoryB);

        // at this point, there is some inventory for this order and allowOrderSplit = Y, so it should appear on the picklist.
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should have increased by 1 in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", new Long(webStoreReadyToPickInitNumber + 1), webStoreReadyToPickCurrentNumber);

        // it should also appear on ready to ship
        assertOrderReadyToShip(order, facilityId);

        // create the picklist
        results = runAndAssertServiceSuccess("createPicklistFromOrders", UtilMisc.toMap("orderIdList", UtilMisc.toList(order.getOrderId()), "facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        String picklistId  = (String) results.get("picklistId");

        // check the picklist contains items with correct quantities
        assertPicklistItems(picklistId, UtilMisc.toMap(productAId, new BigDecimal("2.0"), productBId, new BigDecimal("3.0")));

        // at this point the order should not appear as ready to ship, as it is on an active picklist
        assertOrderNotReadyToShip(order, facilityId);

        // set the picklist as picked
        runAndAssertServiceSuccess("updatePicklist", UtilMisc.toMap("picklistId", picklistId, "statusId", "PICKLIST_PICKED", "userLogin", admin));

        // now it should be back on the ready to ship list
        assertOrderReadyToShip(order, facilityId);

        // pack the partial order
        Map<String, Map<String, BigDecimal>> toPackItems = new HashMap<String, Map<String, BigDecimal>>();
        toPackItems.put("00001", UtilMisc.toMap(itemA.getOrderItemSeqId(), new BigDecimal("2.0"), itemB.getOrderItemSeqId(), new BigDecimal("3.0")));
        runAndAssertServiceSuccess("testShipOrderManual", UtilMisc.toMap("orderId", order.getOrderId(), "facilityId", facilityId, "items", toPackItems, "userLogin", admin));

        // check it packed all the picklist items
        assertPicklistItemsIssued(picklistId);

        // check the reservations
        // Web Store 00001: 3 x productA, 5 x productB
        expectedWebStoreItems = new HashMap<String, Map<String, List<BigDecimal>>>();
        expectedProducts = new HashMap<String, List<BigDecimal>>();
        expectedProducts.put(productAId, Arrays.asList(new BigDecimal("3.0"), new BigDecimal("3.0")));
        expectedProducts.put(productBId, Arrays.asList(new BigDecimal("5.0"), new BigDecimal("5.0")));
        expectedWebStoreItems.put("00001", expectedProducts);
        assertShipGroupReservationsAndQuantities(order.getOrderId(), facilityId, expectedWebStoreItems);
        // check inventory after packing
        inventoryAsserts.assertInventoryChange(productAId, BigDecimal.ZERO, new BigDecimal("-3.0"), originalInventoryA);
        inventoryAsserts.assertInventoryChange(productBId, BigDecimal.ZERO, new BigDecimal("-5.0"), originalInventoryB);

        // it is not anymore on ready to ship
        assertOrderNotReadyToShip(order, facilityId);

        // receive some more inventory
        receiveInventoryProduct(productB, new BigDecimal("3.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        // check inventory
        inventoryAsserts.assertInventoryChange(productAId, BigDecimal.ZERO, new BigDecimal("-3.0"), originalInventoryA);
        inventoryAsserts.assertInventoryChange(productBId, new BigDecimal("3.0"), new BigDecimal("-2.0"), originalInventoryB);

        // create a second picklist
        results = runAndAssertServiceSuccess("createPicklistFromOrders", UtilMisc.toMap("orderIdList", UtilMisc.toList(order.getOrderId()), "facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        String picklistId2  = (String) results.get("picklistId");

        // check the picklist only contains an item for productB and with correct quantity
        assertPicklistItems(picklistId2, UtilMisc.toMap(productBId, new BigDecimal("3.0")));

        // set the picklist as picked
        runAndAssertServiceSuccess("updatePicklist", UtilMisc.toMap("picklistId", picklistId2, "statusId", "PICKLIST_PICKED", "userLogin", admin));

        // pack the partial order
        toPackItems = new HashMap<String, Map<String, BigDecimal>>();
        toPackItems.put("00001", UtilMisc.toMap(itemB.getOrderItemSeqId(), new BigDecimal("3.0")));
        runAndAssertServiceSuccess("testShipOrderManual", UtilMisc.toMap("orderId", order.getOrderId(), "facilityId", facilityId, "items", toPackItems, "userLogin", admin));

        // check it packed all the picklist items
        assertPicklistItemsIssued(picklistId2);

        // Web Store 00001: 3 x productA, 2 x productB
        expectedWebStoreItems = new HashMap<String, Map<String, List<BigDecimal>>>();
        expectedProducts = new HashMap<String, List<BigDecimal>>();
        expectedProducts.put(productAId, Arrays.asList(new BigDecimal("3.0"), new BigDecimal("3.0")));
        expectedProducts.put(productBId, Arrays.asList(new BigDecimal("2.0"), new BigDecimal("2.0")));
        expectedWebStoreItems.put("00001", expectedProducts);
        assertShipGroupReservationsAndQuantities(order.getOrderId(), facilityId, expectedWebStoreItems);
        // check inventory
        inventoryAsserts.assertInventoryChange(productAId, BigDecimal.ZERO, new BigDecimal("-3.0"), originalInventoryA);
        inventoryAsserts.assertInventoryChange(productBId, BigDecimal.ZERO, new BigDecimal("-2.0"), originalInventoryB);

        // receive the last items in inventory
        receiveInventoryProduct(productA, new BigDecimal("3.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        receiveInventoryProduct(productB, new BigDecimal("2.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        // check inventory
        inventoryAsserts.assertInventoryChange(productAId, new BigDecimal("3.0"), BigDecimal.ZERO, originalInventoryA);
        inventoryAsserts.assertInventoryChange(productBId, new BigDecimal("2.0"), BigDecimal.ZERO, originalInventoryB);

        // create the last picklist
        results = runAndAssertServiceSuccess("createPicklistFromOrders", UtilMisc.toMap("orderIdList", UtilMisc.toList(order.getOrderId()), "facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        String picklistId3  = (String) results.get("picklistId");

        // check the picklist contains items correct quantities
        assertPicklistItems(picklistId3, UtilMisc.toMap(productAId, new BigDecimal("3.0"), productBId, new BigDecimal("2.0")));

        // pack the partial order
        toPackItems = new HashMap<String, Map<String, BigDecimal>>();
        toPackItems.put("00001", UtilMisc.toMap(itemA.getOrderItemSeqId(), new BigDecimal("3.0"), itemB.getOrderItemSeqId(), new BigDecimal("2.0")));
        runAndAssertServiceSuccess("testShipOrderManual", UtilMisc.toMap("orderId", order.getOrderId(), "facilityId", facilityId, "items", toPackItems, "userLogin", admin));

        // check it packed all the picklist items
        assertPicklistItemsIssued(picklistId3);

        // there should be no more reservations on this order
        assertNoShipGroupReservations(order.getOrderId(), facilityId);
        // check inventory is all back to initial values
        inventoryAsserts.assertInventoryChange(productAId, BigDecimal.ZERO, BigDecimal.ZERO, originalInventoryA);
        inventoryAsserts.assertInventoryChange(productBId, BigDecimal.ZERO, BigDecimal.ZERO, originalInventoryB);

        // order should now be completed
        assertOrderStatus("The order [" + order.getOrderId() + "] should be completed.", order.getOrderId(), "ORDER_COMPLETED", "ITEM_COMPLETED");
    }

    /**
     * This test verifies creating picklists from orders with allowOrderSplit = N and multiple ship groups with different shipping methods.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMultipleShippingMethodsOrderPicking() throws GeneralException {
        OrderRepositoryInterface repository = getOrderRepository(admin);
        // create 2 test products
        GenericValue productA = createTestProduct("Test Product A for testMultiFacilityOrder()", demowarehouse1);
        GenericValue productB = createTestProduct("Test Product B for testMultiFacilityOrder()", demowarehouse1);
        assignDefaultPrice(productA, new BigDecimal("1.0"), admin);
        assignDefaultPrice(productB, new BigDecimal("1.0"), admin);
        // create a customer from template of DemoCustomer
        String customerPartyId = createPartyFromTemplate(demoCustomer.getString("partyId"), "Customer for testMultiFacilityOrder");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));
        // create a sales order of 5 x productA and 8 x productB, approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(productA, new BigDecimal("5.0"));
        orderItems.put(productB, new BigDecimal("8.0"));
        User = demoCsr;
        // record each store findOrdersToPickMove initial number
        Map results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long webStoreReadyToPickInitNumber = (Long) results.get("nReturnedOrders");

        // create a sales order and approve it
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, customer, productStoreId);
        salesOrder.approveOrder();
        Order order = repository.getOrderById(salesOrder.getOrderId());
        Debug.logInfo("testMultiFacilityOrder() create orderId: " + salesOrder.getOrderId(), MODULE);

        // get the order items
        OrderItem orderItemProductA = null;
        OrderItem orderItemProductB = null;
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getProductId().equals(productA.getString("productId"))) {
                orderItemProductA = orderItem;
            }
            if (orderItem.getProductId().equals(productB.getString("productId"))) {
                orderItemProductB = orderItem;
            }
        }

        assertNotNull("Order Item for product A not found.", orderItemProductA);
        assertNotNull("Order Item for product B not found.", orderItemProductB);

        // Split all product B in a second ship group using EXPRESS / DemoCarrier as the shipping method
        assertSplitOrderSuccess(admin, order.getOrderId(), "8.0", "00001", orderItemProductB.getOrderItemSeqId(), null, "EXPRESS@DemoCarrier");

        // make sure allowOrderSplit is set to N for both ship groups
        runAndAssertServiceSuccess("updateOrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId", "00001", "maySplit", "N", "userLogin", admin));
        runAndAssertServiceSuccess("updateOrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId", "00002", "maySplit", "N", "userLogin", admin));

        // at this point, there is no inventory for this order, so it should not appear on the picklist.
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should not have increased in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", webStoreReadyToPickInitNumber, webStoreReadyToPickCurrentNumber);

        // receive inventory for the first product
        receiveInventoryProduct(productA, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);

        // at this point, there is inventory for the first ship group so it should appear
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should have increased by 1 in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", new Long(webStoreReadyToPickInitNumber + 1), webStoreReadyToPickCurrentNumber);

        // receive inventory for the second product
        receiveInventoryProduct(productB, new BigDecimal("8.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);

        // at this point, there is all the needed inventory for this order, so both ship groups should appear on the picklist.
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        List<Map> pickMoveByShipmentMethodInfoList = (List<Map>) results.get("pickMoveByShipmentMethodInfoList");
        assertEquals("quantity of order ready to pick should have increased by 2 in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]", new Long(webStoreReadyToPickInitNumber + 2), webStoreReadyToPickCurrentNumber);
        // check they are in the pickMoveByShipmentMethodInfoList for their shipment method
        boolean foundStandard = false;
        boolean foundExpress = false;
        for (Map info : pickMoveByShipmentMethodInfoList) {
            GenericValue shipmentMethodType = (GenericValue) info.get("shipmentMethodType");
            if (shipmentMethodType != null && "STANDARD".equals(shipmentMethodType.getString("shipmentMethodTypeId")) || "EXPRESS".equals(shipmentMethodType.getString("shipmentMethodTypeId"))) {
                List<Map> orders = (List<Map>) info.get("orderReadyToPickInfoList");
                for (Map orderMap : orders) {
                    GenericValue oisg = (GenericValue) orderMap.get("orderItemShipGroup");
                    if (oisg != null && order.getOrderId().equals(oisg.getString("orderId"))) {
                        if ("STANDARD".equals(shipmentMethodType.getString("shipmentMethodTypeId"))) {
                            assertEquals("Only the first ship group of order [" + order.getOrderId() + "] should be in STANDARD picklist.", oisg.getString("shipGroupSeqId"), "00001");
                            foundStandard = true;
                        } else if ("EXPRESS".equals(shipmentMethodType.getString("shipmentMethodTypeId"))) {
                            assertEquals("Only the second ship group of order [" + order.getOrderId() + "] should be in EXPRESS picklist.", oisg.getString("shipGroupSeqId"), "00002");
                            foundExpress = true;
                        }
                    }
                }
            }
        }

        assertTrue("Did not find expected order ship group in STANDARD picklist", foundStandard);
        assertTrue("Did not find expected order ship group in EXPRESS picklist", foundExpress);

        // create the picklist
        results = runAndAssertServiceSuccess("createPicklistFromOrders", UtilMisc.toMap("orderIdList", UtilMisc.toList(order.getOrderId()), "facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        String picklistId  = (String) results.get("picklistId");

        // check the picklist contains items for productA and productB and with correct quantity
        List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("pPicklistId", EntityOperator.EQUALS, picklistId));
        List<GenericValue> picklistItems = delegator.findByCondition("PicklistItemAndOdrItmShipGrp", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, UtilMisc.toList("piOrderId", "piShipGroupSeqId", "oiProductId"));
        boolean have5ProductA = false;
        boolean have8ProductB = false;

        for (GenericValue item : picklistItems) {
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productA.getString("productId"))
                    && item.getBigDecimal("piQuantity").compareTo(new BigDecimal("5.0")) == 0) {
                        have5ProductA = true;
            }
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productB.getString("productId"))
                    && item.getBigDecimal("piQuantity").compareTo(new BigDecimal("8.0")) == 0) {
                have8ProductB = true;
            }
         }
        assertTrue("should have 5 x productA in the picklist", have5ProductA);
        assertTrue("should have 8 x productB in the picklist", have8ProductB);
    }

    /**
     * This test verifies order work in multi facility.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMultiFacilityOrder() throws GeneralException {
        OrderRepositoryInterface repository = getOrderRepository(admin);
        // create 2 test products
        GenericValue productA = createTestProduct("Test Product A for testMultiFacilityOrder()", demowarehouse1);
        GenericValue productB = createTestProduct("Test Product B for testMultiFacilityOrder()", demowarehouse1);
        // create a customer from template of DemoCustomer
        String customerPartyId = createPartyFromTemplate(demoCustomer.getString("partyId"), "Customer for testMultiFacilityOrder");
        GenericValue customer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));
        // create a sales order of 5 x productA and 8 x productB, approve order
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        orderItems.put(productA, new BigDecimal("5.0"));
        orderItems.put(productB, new BigDecimal("8.0"));
        User = demoCsr;
        // record each store findOrdersToPickMove initial number
        Map results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long webStoreReadyToPickInitNumber = (Long) results.get("nReturnedOrders");
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId1, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long myRetailStoreReadyToPickInitNumber = (Long) results.get("nReturnedOrders");

        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderItems, customer, productStoreId);
        salesOrder.approveOrder();
        Order order = repository.getOrderById(salesOrder.getOrderId());
        Debug.logInfo("testMultiFacilityOrder() create orderId: " + salesOrder.getOrderId(), MODULE);

        // re-reserve 5 of productB in My Retail Store
        OrderItem orderItemProductB = null;
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getProductId().equals(productB.getString("productId"))) {
                orderItemProductB = orderItem;
            }
        }
        List<OrderItemShipGrpInvRes> reservations = orderItemProductB.getOrderItemShipGrpInvReses();
        assertEquals("Should be only reservation for the order item", 1, reservations.size());
        reReserveOrderItemInventory(
                order.getOrderId(),
                orderItemProductB.getOrderItemSeqId(),
                reservations.get(0).getInventoryItemId(),
                reservations.get(0).getShipGroupSeqId(),
                facilityId1,
                new BigDecimal("5.0")
        );

        // receive in Web Store 5 x productA and 3 x productB
        receiveInventoryProduct(productA, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);
        receiveInventoryProduct(productB, new BigDecimal("3.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId, demowarehouse1);

        // receive in My Retail Store 5 x ProductB
        receiveInventoryProduct(productB, new BigDecimal("5.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("10.0"), facilityId1, demowarehouse1);

        // check the order appears as ready to ship on both warehouse
        // the process is the same as what the BSH on the ready to ship page does:
        // - shippinghelper = new ShippingHelper(delegator, facilityId);
        // - shippinghelper.findAllOrdersReadyToShip() => should contain the corresponding OdrItShpGrpHdrInvResAndInvItem entities
        // need to be done for each facility, and check that the correct OdrItShpGrpHdrInvResAndInvItem are returned in each case (note this might also contain other orders from other tests)
        // - check the orderId / shipGroupSeqId / productId / quantity are correct:
        // Web Store 00001: 5 x productA and 3 x productB
        // My Retail Store 00001: 5 x productB
        Map<String, Map<String, BigDecimal>> expectedWebStoreItems = new HashMap<String, Map<String, BigDecimal>>();
        Map<String, BigDecimal> expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(productA.getString("productId"), new BigDecimal("5.0"));
        expectedProducts.put(productB.getString("productId"), new BigDecimal("3.0"));
        expectedWebStoreItems.put("00001", expectedProducts);

        assertShipGroupReservations(order.getOrderId(), facilityId, expectedWebStoreItems);
        assertOrderReadyToShip(order, facilityId, expectedWebStoreItems);

        Map<String, Map<String, BigDecimal>> expectedRetailStoreItems = new HashMap<String, Map<String, BigDecimal>>();
        expectedProducts = new HashMap<String, BigDecimal>();
        expectedProducts.put(productB.getString("productId"), new BigDecimal("5.0"));
        expectedRetailStoreItems.put("00001", expectedProducts);

        assertShipGroupReservations(order.getOrderId(), facilityId1, expectedRetailStoreItems);
        assertOrderReadyToShip(order, facilityId1, expectedRetailStoreItems);

        // check the order appears as ready to pick on both warehouse
        // this should test the findOrdersToPickMove service which return the number of order ready to pick
        // so we need to test this value before creating the order, and now, and check the quantity increased by 1
        // this should be checked on both facilities
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long webStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should increased by 1 in Facility [" + facilityId + "] for order [" + order.getOrderId() + "]"
                , (int) (webStoreReadyToPickInitNumber + 1), webStoreReadyToPickCurrentNumber.intValue());
        results = runAndAssertServiceSuccess("findOrdersToPickMove", UtilMisc.toMap("facilityId", facilityId1, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        Long myRetailStoreReadyToPickCurrentNumber = (Long) results.get("nReturnedOrders");
        assertEquals("quantity of order ready to pick should increased by 1 in Facility [" + facilityId1 + "] for order [" + order.getOrderId() + "]"
                , (int) (myRetailStoreReadyToPickInitNumber + 1), myRetailStoreReadyToPickCurrentNumber.intValue());

        // create picklists in both facilities and test the correct items are in each picklist
        // for each facility:
        // - call the createPicklistFromOrders service for the order (see orderIdList service parameter)
        // - check the created picklist by checking the PicklistItemAndOdrItmShipGrp entities that were created with the returned picklistId
        // - check the orderId / productId / quantity are correct:
        // -- should have 5 x productA and 3 x productB in the picklist for Web Store
        // -- should have 5 x ProductB in the picklist for My Retail Store
        List orderIdList = UtilMisc.toList(order.getOrderId());
        results = runAndAssertServiceSuccess("createPicklistFromOrders", UtilMisc.toMap("orderIdList", orderIdList, "facilityId", facilityId, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        String picklistIdForWebStore  = (String) results.get("picklistId");
        List conditions = new ArrayList();
        conditions.add(EntityCondition.makeCondition("pPicklistId", EntityOperator.EQUALS, picklistIdForWebStore));
        List<GenericValue> picklistItems = delegator.findByCondition("PicklistItemAndOdrItmShipGrp", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, UtilMisc.toList("piOrderId", "piShipGroupSeqId", "oiProductId"));
        boolean have5ProductAForWebStore = false;
        boolean have3ProductBForWebStore = false;

        for (GenericValue item : picklistItems) {
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productA.getString("productId"))
                    && item.getBigDecimal("piQuantity").compareTo(new BigDecimal("5.0")) == 0) {
                        have5ProductAForWebStore = true;
            }
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId)
                    && item.getString("oiProductId").equals(productB.getString("productId"))
                    && item.getBigDecimal("piQuantity").compareTo(new BigDecimal("3.0")) == 0) {
                have3ProductBForWebStore = true;
            }
         }
        assertTrue("should have 5 x productA in the picklist for Web Store", have5ProductAForWebStore);
        assertTrue("should have 3 x productB in the picklist for Web Store", have3ProductBForWebStore);

        results = runAndAssertServiceSuccess("createPicklistFromOrders", UtilMisc.toMap("orderIdList", orderIdList, "facilityId", facilityId1, "maxNumberOfOrders", new Long(1000), "userLogin", admin));
        String picklistIdForMyRetail  = (String) results.get("picklistId");
        conditions = new ArrayList();
        conditions.add(EntityCondition.makeCondition("pPicklistId", EntityOperator.EQUALS, picklistIdForMyRetail));
        picklistItems = delegator.findByCondition("PicklistItemAndOdrItmShipGrp", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, UtilMisc.toList("piOrderId", "piShipGroupSeqId", "oiProductId"));
        boolean have5ProductBForMyRetail = false;

        for (GenericValue item : picklistItems) {
            if (item.getString("pbPrimaryOrderId").equals(order.getOrderId())
                    && item.getString("pFacilityId").equals(facilityId1)
                    && item.getString("oiProductId").equals(productB.getString("productId"))
                    && item.getBigDecimal("piQuantity").compareTo(new BigDecimal("5.0")) == 0) {
                have5ProductBForMyRetail = true;
            }
        }
        assertTrue("should have 5 x ProductB in the picklist for My Retail Store", have5ProductBForMyRetail);

        // set both picklist to picked with service updatePicklist
        runAndAssertServiceSuccess("updatePicklist", UtilMisc.toMap("picklistId", picklistIdForWebStore, "facilityId", facilityId, "userLogin", admin));
        runAndAssertServiceSuccess("updatePicklist", UtilMisc.toMap("picklistId", picklistIdForMyRetail, "facilityId", facilityId1, "userLogin", admin));

        // check reservations are still correct before packing
        assertShipGroupReservations(order.getOrderId(), facilityId, expectedWebStoreItems);
        assertShipGroupReservations(order.getOrderId(), facilityId1, expectedRetailStoreItems);

        // use testShipOrder for the order and facility Web Store
        // check that the OrderItemShipGroup status is NOT OISG_PACKED
        // check a shipment was created, and contains the correct items: 5 x productA and 3 x productB
        // check the invoice was created
        results = runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", order.getOrderId(), "facilityId", facilityId, "userLogin", demowarehouse1));
        List<String> shipmentIds = (List<String>) results.get("shipmentIds");
        assertEquals("Check a shipment was created", 1, shipmentIds.size());
        GenericValue orderItemShipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId" , "00001"));
        assertNotEquals("OrderItemShipGroup status should NOT be OISG_PACKED for [" + order.getOrderId() + "/00001]", "OISG_PACKED", orderItemShipGroup.getString("statusId"));
        GenericValue shipment1 = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentIds.get(0)));
        List orderShipments = shipment1.getRelated("OrderShipment");
        Iterator orderShipmentsIt = orderShipments.iterator();
        while (orderShipmentsIt.hasNext()) {
            GenericValue orderShipment = (GenericValue) orderShipmentsIt.next();
            GenericValue orderItem = orderShipment.getRelatedOne("OrderItem");
            if (orderItem.getString("productId").equals(productA.getString("productId"))) {
                // check a shipment was created, and contains the correct items: 5 x productA
                assertEquals("Assert contains 5 x productA", new BigDecimal("5.0"), orderShipment.getBigDecimal("quantity"));
            }
            if (orderItem.getString("productId").equals(productB.getString("productId"))) {
                // check a shipment was created, and contains the correct items: 5 x productA
                assertEquals("Assert contains 3 x productB", new BigDecimal("3.0"), orderShipment.getBigDecimal("quantity"));
            }
        }
        List<GenericValue> billings1 = shipment1.getRelated("ShipmentItemBilling");
        assertEquals("the invoice should be created", 1, EntityUtil.getFieldListFromEntityList(billings1, "invoiceId", true).size());

        // check reservations are still correct before second packing
        assertShipGroupReservations(order.getOrderId(), facilityId, null);
        assertShipGroupReservations(order.getOrderId(), facilityId1, expectedRetailStoreItems);

        // use testShipOrder for the order and facility My Retail Store
        // check that the OrderItemShipGroup status IS now OISG_PACKED
        // check another shipment was created, and contains the correct items: 5 x ProductB
        // check another invoice was created
        results = runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", order.getOrderId(), "facilityId", facilityId1, "userLogin", demowarehouse1));
        shipmentIds = (List<String>) results.get("shipmentIds");
        assertEquals("Check a shipment was created", 1, shipmentIds.size());
        orderItemShipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", order.getOrderId(), "shipGroupSeqId" , "00001"));
        assertEquals("OrderItemShipGroup status should be OISG_PACKED for [" + order.getOrderId() + "/00001]", "OISG_PACKED", orderItemShipGroup.getString("statusId"));
        GenericValue shipment2 = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentIds.get(0)));
        orderShipments = shipment2.getRelated("OrderShipment");
        orderShipmentsIt = orderShipments.iterator();
        while (orderShipmentsIt.hasNext()) {
            GenericValue orderShipment = (GenericValue) orderShipmentsIt.next();
            GenericValue orderItem = orderShipment.getRelatedOne("OrderItem");
            if (orderItem.getString("productId").equals(productB.getString("productId"))) {
                // check a shipment was created, and contains the correct items: 5 x productA
                assertEquals("Assert contains 5 x productB", new BigDecimal("5.0"), orderShipment.getBigDecimal("quantity"));
            }
        }
        List<GenericValue> billings2 = shipment2.getRelated("ShipmentItemBilling");
        assertEquals("the invoice should be created", 1, EntityUtil.getFieldListFromEntityList(billings2, "invoiceId", true).size());

        // check the order status is now COMPLETED
        assertOrderCompleted(order.getOrderId());
    }

}
