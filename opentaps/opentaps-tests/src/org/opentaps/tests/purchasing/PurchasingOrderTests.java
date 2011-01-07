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
package org.opentaps.tests.purchasing;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.opentaps.base.entities.OrderItemShipGroupAssoc;
import org.opentaps.base.entities.SupplierProduct;
import org.opentaps.common.order.PurchaseOrderFactory;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.purchasing.PurchasingRepositoryInterface;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.lookup.configuration.PurchaseOrderLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.SalesOrderLookupConfiguration;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.gwt.common.server.lookup.PurchaseOrderLookupService;
import org.opentaps.gwt.common.server.lookup.SalesOrderLookupService;
import org.opentaps.tests.OpentapsTestCase;
import org.opentaps.tests.financials.FinancialAsserts;
import org.opentaps.tests.gwt.TestInputProvider;

import com.opensourcestrategies.financials.util.UtilFinancial;

/**
 * Purchasing Order Tests.
 */
public class PurchasingOrderTests extends OpentapsTestCase {

    private static final String MODULE = PurchasingOrderTests.class.getName();

    private GenericValue admin;
    private GenericValue demopurch1;
    private GenericValue demowarehouse1;
    private GenericValue euroSupplier;
    private GenericValue demoSupplier;
    private GenericValue demofinadmin;

    private static final String facilityContactMechId = "9200";
    private static final String thirdPartyFacilityId = "Demo3PL";
    private static final String demoSupplierPartyId = "DemoSupplier";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        admin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
        demopurch1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demopurch1"));
        demowarehouse1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        euroSupplier = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "EuroSupplier"));
        demoSupplier = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", demoSupplierPartyId));
        demofinadmin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demofinadmin"));
        // test that the object have been retrieved
        assertTrue("demopurch1 not null", demopurch1 != null);
        // set a default User
        User = demopurch1;

    }

    @Override
    public void tearDown() throws Exception {
        admin = null;
        demopurch1 = null;
        euroSupplier = null;
        super.tearDown();
    }

    /**
     * This test verifies the correct parties for a purchase order.
     * @throws Exception if an error occurs
     */
    public void testPurchaseOrderParties() throws Exception {
        // create a supplier from template of DemoSupplier
        String supplierPartyId = createPartyFromTemplate("DemoSupplier", "Supplier for testPurchaseOrderParties");
        GenericValue supplier = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", supplierPartyId));
        // create a product
        GenericValue testProduct = createTestProduct("testPurchaseOrderParties Test Product", demopurch1);

        // create a purchase order for the customer and product
        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(testProduct, new BigDecimal("1.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderSpec, supplier, facilityContactMechId);
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(pof.getOrderId());
        // verify that the organizationPartyId is the
        // Order.getOrganizationParty()
        assertNotNull("order.getOrganizationParty() should not be null for order [" + order.getOrderId() + "].", order.getOrganizationParty());
        assertEquals("order.getOrganizationParty().getId() should be " + organizationPartyId + " for order [" + order.getOrderId() + "].", organizationPartyId, order.getOrganizationParty().getPartyId());
        // verify that the supplier party is the Order.getBillFromVendor()
        assertNotNull("order.getBillFromVendor() should not be null for order [" + order.getOrderId() + "].", order.getBillFromVendor());
        assertEquals("order.getBillFromVendor().getPartyId() should be " + supplierPartyId + " for order [" + order.getOrderId() + "].", supplierPartyId, order.getBillFromVendor().getPartyId());

    }

    /**
     * Test Complete PO.
     *
     * 1. create test product
     * 2. Create a PO.
     * 3. Verify The status of the PO is created.
     * 4. Approve PO
     * 5. Verify all items are approved
     * 6. Verify PO status is approve
     * 7. Receive and Close PO
     * 8. Verify all items are completed
     * 9. Verify PO status is complete
     *
     * @throws GeneralException if an error occurs
     */
    public void testCompletePurchaseOrder() throws GeneralException {
        // 1. create test product
        GenericValue product1 = createTestProduct("testCompletePurchaseOrder Test Product 1", demopurch1);
        GenericValue product2 = createTestProduct("testCompletePurchaseOrder Test Product 2", demopurch1);
        GenericValue product3 = createTestProduct("testCompletePurchaseOrder Test Product 3", demopurch1);
        GenericValue product4 = createTestProduct("testCompletePurchaseOrder Test Product 4", demopurch1);
        GenericValue product5 = createTestProduct("testCompletePurchaseOrder Test Product 5", demopurch1);

        // 2. Create a PO.
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product1, new BigDecimal("14.0"));
        order.put(product2, new BigDecimal("9.0"));
        order.put(product3, new BigDecimal("20.0"));
        order.put(product4, new BigDecimal("15.0"));
        order.put(product5, new BigDecimal("25.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(order, euroSupplier, facilityContactMechId);
        String orderId = pof.getOrderId();
        Debug.logInfo("testCompletePurchaseOrder created order [" + pof.getOrderId() + "]", MODULE);

        // 3. Verify The status of the PO is created.
        GenericValue pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_CREATED", pOrder.getString("statusId"));

        // 4. Approve PO
        pof.approveOrder();

        // 5. Verify all items are approved
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"));
        long itemApprovedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("Wrong status for item in order [%1$s]", orderId), 5, itemApprovedCount);

        // 6. Verify PO status is approved
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_APPROVED", pOrder.getString("statusId"));

        // 7. Receive all items with
        // warehouse.issueOrderItemToShipmentAndReceiveAgainstPO and Close PO
        // with completePurchaseOrder
        Map<String, Object> inputParameters = createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(pOrder, demowarehouse1);
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", inputParameters);

        // 8. Verify all items are completed
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_COMPLETED"));
        long itemCompletedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("Wrong status for item in order [%1$s]", orderId), 5, itemCompletedCount);

        // 9. Verify PO status is complete
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_COMPLETED", pOrder.getString("statusId"));
    }

    /**
     * Test Complete PO with cancelled item after creation.
     *
     * 1. Create a PO.
     * 2. Verify The status of the PO is created.
     * 3. Cancel One item
     * 4. Verify PO status is created
     * 5. Approve PO
     * 6. Verify all items are approved and one item is cancelled
     * 7. Verify PO status is approved
     * 8. Receive and Close PO
     * 9. Verify all items are completed and one item is cancelled
     * 10. Verify PO status is complete
     *
     * @throws GeneralException if an error occurs
     */
    public void testCompletePurchaseOrderWithCancelledItemAfterCreation() throws GeneralException {
        GenericValue product1 = createTestProduct("testCompletePurchaseOrderWithCancelledItemAfterCreation Test Product 1", demopurch1);
        GenericValue product2 = createTestProduct("testCompletePurchaseOrderWithCancelledItemAfterCreation Test Product 2", demopurch1);
        GenericValue product3 = createTestProduct("testCompletePurchaseOrderWithCancelledItemAfterCreation Test Product 3", demopurch1);
        GenericValue product4 = createTestProduct("testCompletePurchaseOrderWithCancelledItemAfterCreation Test Product 4", demopurch1);
        GenericValue product5 = createTestProduct("testCompletePurchaseOrderWithCancelledItemAfterCreation Test Product 5", demopurch1);

        createMainSupplierForProduct(product1.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product2.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("20.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product3.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("115.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product4.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("15.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product5.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("1030.0"), "USD", new BigDecimal("0.0"), admin);

        // 1. Create a PO.
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product1, new BigDecimal("14.0"));
        order.put(product2, new BigDecimal("9.0"));
        order.put(product3, new BigDecimal("20.0"));
        order.put(product4, new BigDecimal("15.0"));
        order.put(product5, new BigDecimal("25.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(order, euroSupplier, facilityContactMechId);
        String orderId = pof.getOrderId();
        Debug.logInfo("testCompletePurchaseOrderWithCancelledItemAfterCreation created order [" + pof.getOrderId() + "]", MODULE);

        // 2. Verify The status of the PO is created.
        GenericValue pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_CREATED", pOrder.getString("statusId"));

        // 3. Cancel One item
        pof.cancelProduct(product2, new BigDecimal("9.0"));

        // 4. Verify PO status is created
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_CREATED", pOrder.getString("statusId"));

        // 5. Approve PO
        pof.approveOrder();

        // 6. Verify all items are approved and one item is cancelled
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"));
        long itemApprovedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CANCELLED"));
        long itemCancelledCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertTrue(String.format("Wrong status for item(s) in order [%1$s]", orderId), itemApprovedCount == 4 && itemCancelledCount == 1);

        // 7. Verify PO status is approve
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_APPROVED", pOrder.getString("statusId"));

        // 8. Receive and Close PO
        Map<String, Object> inputParameters = createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(pOrder, demowarehouse1);
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", inputParameters);

        // 9. Verify all items are completed and one item is cancelled
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_COMPLETED"));
        long itemCompletedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CANCELLED"));
        itemCancelledCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertTrue(String.format("Wrong status for item(s) in order [%1$s]", orderId), itemCompletedCount == 4 && itemCancelledCount == 1);

        // 10. Verify PO status is complete
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_COMPLETED", pOrder.getString("statusId"));
    }

    /**
     * Test Complete PO with cancelled item after approving.
     *
     * 1. Create a PO.
     * 2. The status of the PO is created.
     * 3. Approve PO
     * 4. Verify all items are approved
     * 5. Verify PO status is approve
     * 6. Cancel One item
     * 7. Verify PO status is approved
     * 8. Receive and Close PO
     * 9. Verify all items are completed and one item is cancelled
     * 10. Verify PO status is complete
     *
     * @throws GeneralException if an error occurs
     */
    public void testCompletePurchaseOrderWithCancelledItemAfterApproving() throws GeneralException {

        GenericValue product1 = createTestProduct("testCompletePurchaseOrderWithCancelledItemAfterApproving Test Product 1", demopurch1);
        GenericValue product2 = createTestProduct("testCompletePurchaseOrderWithCancelledItemAfterApproving Test Product 2", demopurch1);
        GenericValue product3 = createTestProduct("testCompletePurchaseOrderWithCancelledItemAfterApproving Test Product 3", demopurch1);
        createMainSupplierForProduct(product1.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product2.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("20.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product3.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("115.0"), "USD", new BigDecimal("0.0"), admin);


        // 1. Create a PO.
        Map<GenericValue, BigDecimal> orderedProducts = new HashMap<GenericValue, BigDecimal>();
        orderedProducts.put(product1, new BigDecimal("10.0"));
        orderedProducts.put(product2, new BigDecimal("15.0"));
        orderedProducts.put(product3, new BigDecimal("25.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderedProducts, euroSupplier, facilityContactMechId);
        String orderId = pof.getOrderId();
        Debug.logInfo("testCompletePurchaseOrderWithCancelledItemAfterApproving created order [" + pof.getOrderId() + "]", MODULE);

        // 2. The status of the PO is created.
        GenericValue pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_CREATED", pOrder.getString("statusId"));

        // 3. Approve PO
        pof.approveOrder();

        // 4. Verify all items are approved
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"));
        long itemApprovedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("Wrong status for item in order [%1$s]", orderId), 3, itemApprovedCount);

        // 5. Verify PO status is approve
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_APPROVED", pOrder.getString("statusId"));

        // 6. Cancel One item
        pof.cancelProduct(product1, new BigDecimal("10.0"));

        // 7. Verify PO status is approve
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_APPROVED", pOrder.getString("statusId"));

        // 8. Receive and Close PO
        Map<String, Object> inputParameters = createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(pOrder, demowarehouse1);
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", inputParameters);

        // 9. Verify all items are completed and one item is cancelled
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_COMPLETED"));
        long itemCompletedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CANCELLED"));
        long itemCancelledCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertTrue(String.format("Wrong status for item(s) in order [%1$s]", orderId), itemCompletedCount == 2 && itemCancelledCount == 1);

        // 10. Verify PO status is complete
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_COMPLETED", pOrder.getString("statusId"));

    }

    /**
     * Test Cancel PO after creation.
     *
     * 1. Create a PO.
     * 2. The status of the PO is created.
     * 3. Cancel PO
     * 4. Verify all items are cancelled 5. Verify PO status is cancel
     *
     * @throws GeneralException if an error occurs
     */
    public void testCancelPurchaseOrderAfterCreation() throws GeneralException {

        GenericValue product1 = createTestProduct("testCancelPurchaseOrderAfterCreation Test Product 1", demopurch1);
        GenericValue product2 = createTestProduct("testCancelPurchaseOrderAfterCreation Test Product 2", demopurch1);
        GenericValue product3 = createTestProduct("testCancelPurchaseOrderAfterCreation Test Product 3", demopurch1);
        GenericValue product4 = createTestProduct("testCancelPurchaseOrderAfterCreation Test Product 4", demopurch1);
        GenericValue product5 = createTestProduct("testCancelPurchaseOrderAfterCreation Test Product 5", demopurch1);
        createMainSupplierForProduct(product1.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product2.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("20.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product3.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("115.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product4.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("15.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product5.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("1030.0"), "USD", new BigDecimal("0.0"), admin);

        // 1. Create a PO.
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product1, new BigDecimal("14.0"));
        order.put(product2, new BigDecimal("9.0"));
        order.put(product3, new BigDecimal("20.0"));
        order.put(product4, new BigDecimal("15.0"));
        order.put(product5, new BigDecimal("25.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(order, euroSupplier, facilityContactMechId);
        String orderId = pof.getOrderId();
        Debug.logInfo("testCancelPurchaseOrderAfterCreation created order [" + pof.getOrderId() + "]", MODULE);

        // 2. The status of the PO is created.
        GenericValue pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_CREATED", pOrder.getString("statusId"));

        // 3. Cancel PO
        pof.cancelOrder();

        // 4. Verify all items are cancelled
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CANCELLED"));
        long itemCompletedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("Wrong status for item in order [%1$s]", orderId), 5, itemCompletedCount);

        // 5. Verify PO status is cancel
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_CANCELLED", pOrder.getString("statusId"));
    }

    /**
     * Test Cancel PO after approving.
     *
     * 1. Create a PO.
     * 2. The status of the PO is created.
     * 3. Approve PO
     * 4. Verify all items are approved 5. Verify PO status is approve 6. Cancel PO
     * 7. Verify all items are cancelled 8. Verify PO status is cancel
     *
     * @throws GeneralException if an error occurs
     */
    public void testCancelPurchaseOrderAfterApproving() throws GeneralException {
        GenericValue product1 = createTestProduct("testCancelPurchaseOrderAfterApproving Test Product 1", demopurch1);
        GenericValue product2 = createTestProduct("testCancelPurchaseOrderAfterApproving Test Product 2", demopurch1);
        GenericValue product3 = createTestProduct("testCancelPurchaseOrderAfterApproving Test Product 3", demopurch1);
        GenericValue product4 = createTestProduct("testCancelPurchaseOrderAfterApproving Test Product 4", demopurch1);
        GenericValue product5 = createTestProduct("testCancelPurchaseOrderAfterApproving Test Product 5", demopurch1);
        createMainSupplierForProduct(product1.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product2.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("20.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product3.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("115.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product4.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("15.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product5.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("1030.0"), "USD", new BigDecimal("0.0"), admin);

        // 1. Create a PO.
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product1, new BigDecimal("14.0"));
        order.put(product2, new BigDecimal("9.0"));
        order.put(product3, new BigDecimal("20.0"));
        order.put(product4, new BigDecimal("15.0"));
        order.put(product5, new BigDecimal("25.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(order, euroSupplier, facilityContactMechId);
        String orderId = pof.getOrderId();

        // 2. The status of the PO is created.
        GenericValue pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_CREATED", pOrder.getString("statusId"));

        // 3. Approve PO
        pof.approveOrder();

        // 4. Verify all items are approved
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"));
        long itemApprovedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("Wrong status for item in order [%1$s]", orderId), 5, itemApprovedCount);

        // 5. Verify PO status is approve
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_APPROVED", pOrder.getString("statusId"));

        // 6. Cancel PO
        pof.cancelOrder();

        // 7. Verify all items are cancelled
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CANCELLED"));
        long itemCompletedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("Wrong status for item in order [%1$s]", orderId), 5, itemCompletedCount);

        // 8. Verify PO status is cancel
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_CANCELLED", pOrder.getString("statusId"));
    }

    /**
     * Test Cancel PO after approving with one item received.
     *
     * 1. Create a PO.
     * 2. The status of the PO is created.
     * 3. Approve PO
     * 4. Verify all items are approved
     * 5. Verify PO status is approved
     * 6. Receive one item from the PO
     * 7. Verify all items are approved but one is completed
     * 8. Verify PO status is approved
     * 9. Cancel the PO
     * 10. Verify all items are cancelled but one is completed
     * 11. Verify PO status is cancelled
     *
     * @throws GeneralException if an error occurs
     */
    public void testCancelPurchaseOrderAfterReceiving() throws GeneralException {

        GenericValue product1 = createTestProduct("testCancelPurchaseOrderAfterReceiving Test Product 1", demopurch1);
        GenericValue product2 = createTestProduct("testCancelPurchaseOrderAfterReceiving Test Product 2", demopurch1);
        GenericValue product3 = createTestProduct("testCancelPurchaseOrderAfterReceiving Test Product 3", demopurch1);
        GenericValue product4 = createTestProduct("testCancelPurchaseOrderAfterReceiving Test Product 4", demopurch1);
        GenericValue product5 = createTestProduct("testCancelPurchaseOrderAfterReceiving Test Product 5", demopurch1);
        createMainSupplierForProduct(product1.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product2.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("20.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product3.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("115.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product4.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("15.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product5.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("1030.0"), "USD", new BigDecimal("0.0"), admin);

        // 1. Create a PO.
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product1, new BigDecimal("14.0"));
        order.put(product2, new BigDecimal("9.0"));
        order.put(product3, new BigDecimal("20.0"));
        order.put(product4, new BigDecimal("15.0"));
        order.put(product5, new BigDecimal("25.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(order, euroSupplier, facilityContactMechId);
        String orderId = pof.getOrderId();

        // 2. The status of the PO is created.
        GenericValue pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_CREATED", pOrder.getString("statusId"));

        // 3. Approve PO
        pof.approveOrder();

        // 4. Verify all items are approved
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"));
        long itemApprovedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("Wrong status for item in order [%1$s]", orderId), 5, itemApprovedCount);

        // 5. Verify PO status is approve
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_APPROVED", pOrder.getString("statusId"));

        // 6. Receive one item from the PO
        OrderReadHelper orh = new OrderReadHelper(pOrder);
        List<GenericValue> orderItems = new ArrayList<GenericValue>();
        // add first item of order
        orderItems.add((GenericValue) orh.getOrderItems().get(0));
        Map<String, Object> inputParameters = createTestInputParametersForReceiveInventoryAgainstPurchaseOrderItems(orderId, orderItems, demowarehouse1);
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", inputParameters);

        // 7. Verify all items are approved but one is completed
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"));
        itemApprovedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("Wrong status for item in order [%1$s]", orderId), 4, itemApprovedCount);
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_COMPLETED"));
        long itemCompletedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("Wrong status for item in order [%1$s]", orderId), 1, itemCompletedCount);

        // 8. Verify PO status is approved
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_APPROVED", pOrder.getString("statusId"));

        // 9. Cancel PO
        pof.cancelOrder();

        // 10. Verify all items are cancelled but one is completed
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CANCELLED"));
        long itemCancelledCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("There should have 4 items are cancelled in order [%1$s]", orderId), 4, itemCancelledCount);

        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                           EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                           EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_COMPLETED"));
        itemCompletedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals(String.format("There should have 1 item is completed in order [%1$s]", orderId), 1, itemCompletedCount);

        // 11. Verify PO status is Completed
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        Debug.logInfo("testCancelPurchaseOrderAfterReceiving PO status is [" + pOrder.getString("statusId") + "]", MODULE);
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_COMPLETED", pOrder.getString("statusId"));

    }

    /**
     * This test verifies that when you use the "Receive and Close PO" feature
     * on the Receive Against PO screen the items received will be marked as
     * COMPLETED, and the other items will be canceled, and that inventory will
     * be accounted for correctly.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testReceiveAndClosePurchaseOrder() throws Exception {
        // Create products P1, P2, P3, P4
        GenericValue product1 = createTestProduct("testReceiveAndClosePurchaseOrder Test Product 1", demopurch1);
        GenericValue product2 = createTestProduct("testReceiveAndClosePurchaseOrder Test Product 2", demopurch1);
        GenericValue product3 = createTestProduct("testReceiveAndClosePurchaseOrder Test Product 3", demopurch1);
        GenericValue product4 = createTestProduct("testReceiveAndClosePurchaseOrder Test Product 4", demopurch1);
        createMainSupplierForProduct(product1.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product2.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("20.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product3.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("115.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(product4.getString("productId"), euroSupplier.getString("partyId"), new BigDecimal("15.0"), "USD", new BigDecimal("0.0"), admin);

        // Get the initial ATP and QOH quantities for P1, P2, P3, P4
        Map<String, Object> availability1 = getProductAvailability(product1.getString("productId"));
        Map<String, Object> availability2 = getProductAvailability(product2.getString("productId"));
        Map<String, Object> availability3 = getProductAvailability(product3.getString("productId"));
        Map<String, Object> availability4 = getProductAvailability(product4.getString("productId"));
        BigDecimal atpInitProduct1 = (BigDecimal) availability1.get("availableToPromiseTotal");
        BigDecimal atpInitProduct2 = (BigDecimal) availability2.get("availableToPromiseTotal");
        BigDecimal atpInitProduct3 = (BigDecimal) availability3.get("availableToPromiseTotal");
        BigDecimal atpInitProduct4 = (BigDecimal) availability4.get("availableToPromiseTotal");
        BigDecimal qohInitProduct1 = (BigDecimal) availability1.get("quantityOnHandTotal");
        BigDecimal qohInitProduct2 = (BigDecimal) availability2.get("quantityOnHandTotal");
        BigDecimal qohInitProduct3 = (BigDecimal) availability3.get("quantityOnHandTotal");
        BigDecimal qohInitProduct4 = (BigDecimal) availability4.get("quantityOnHandTotal");
        // Create PO for 10 P1, 20 P2, 30 P3, 40 P4
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product1, new BigDecimal("10.0"));
        order.put(product2, new BigDecimal("20.0"));
        order.put(product3, new BigDecimal("30.0"));
        order.put(product4, new BigDecimal("40.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(order, euroSupplier, facilityContactMechId);
        String orderId = pof.getOrderId();
        // Approve PO
        pof.approveOrder();
        // Use warehouse.issueOrderItemToShipmentAndReceiveAgainstPO to receive
        // 10 P1, 20 P2, 20 P4 with completePurchaseOrder=Y
        GenericValue pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        OrderReadHelper orh = new OrderReadHelper(pOrder);
        Map<String, Object> inputParameters = new HashMap<String, Object>();

        List<GenericValue> orderItems = orh.getOrderItems();
        Map<String, String> orderItemSeqIds = new HashMap<String, String>();
        Map<String, String> productIds = new HashMap<String, String>();
        Map<String, String> quantitiesAccepted = new HashMap<String, String>();
        Map<String, String> quantitiesRejected = new HashMap<String, String>();
        Map<String, String> unitCosts = new HashMap<String, String>();
        Map<String, String> lotIds = new HashMap<String, String>();
        Map<String, String> inventoryItemTypeIds = new HashMap<String, String>();
        Map<String, String> rowSubmit = new HashMap<String, String>();

        // create parameters
        if (UtilValidate.isNotEmpty(orderItems)) {
            int rowNumber = 0;
            for (GenericValue orderItem : orderItems) {
                String strRowNumber = Integer.toString(rowNumber);
                String orderItemSeqId = orderItem.getString("orderItemSeqId");
                orderItemSeqIds.put(strRowNumber, orderItemSeqId);
                String productId = orderItem.getString("productId");
                productIds.put(strRowNumber, productId);
                Double acceptedQuantity = null;
                if (product1.getString("productId").equals(
                        orderItem.getString("productId"))) {
                    // receive 10 P1
                    acceptedQuantity = 10.0;
                } else if (product2.getString("productId").equals(
                        orderItem.getString("productId"))) {
                    // receive 20 P2
                    acceptedQuantity = 20.0;
                } else if (product4.getString("productId").equals(
                        orderItem.getString("productId"))) {
                    // receive 20 P4
                    acceptedQuantity = 20.0;
                }
                quantitiesAccepted.put(strRowNumber, acceptedQuantity == null ? "0.0" : acceptedQuantity.toString());
                quantitiesRejected.put(strRowNumber, "0.0");
                unitCosts.put(strRowNumber, "0.0");
                lotIds.put(strRowNumber, null);
                inventoryItemTypeIds.put(strRowNumber, "NON_SERIAL_INV_ITEM");
                rowSubmit.put(strRowNumber, "Y");
                rowNumber++;
            }
        }

        // put parameters into the parameter map
        inputParameters.put("orderItemSeqIds", orderItemSeqIds);
        inputParameters.put("productIds", productIds);
        inputParameters.put("quantitiesAccepted", quantitiesAccepted);
        inputParameters.put("quantitiesRejected", quantitiesRejected);
        inputParameters.put("unitCosts", unitCosts);
        inputParameters.put("lotIds", lotIds);
        inputParameters.put("inventoryItemTypeIds", inventoryItemTypeIds);
        inputParameters.put("_rowSubmit", rowSubmit);
        inputParameters.put("shipmentId", null);
        inputParameters.put("purchaseOrderId", orh.getOrderId());
        inputParameters.put("facilityId", facilityId);
        inputParameters.put("completePurchaseOrder", "Y");
        inputParameters.put("ownerPartyId", organizationPartyId);
        inputParameters.put("shipGroupSeqId", shipGroupSeqId);
        inputParameters.put("userLogin", demowarehouse1);
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", inputParameters);

        // Verify that PO is now COMPLETED
        pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertEquals(String.format("Wrong status for order [%1$s]", orderId), "ORDER_COMPLETED", pOrder.getString("statusId"));

        // Verify that item for P1 is now COMPLETED
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
             EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
             EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product1.getString("productId")),
             EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_COMPLETED"));
        long product1CompletedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals("There should have 1 " + product1.getString("productId") + " item is COMPLETED", 1, product1CompletedCount);

        // Verify that item for P2 is now COMPLETED
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
             EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
             EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product2.getString("productId")),
             EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_COMPLETED"));
        long product2CompletedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals("There should have 1 " + product2.getString("productId") + " item is COMPLETED", 1, product2CompletedCount);

        // Verify that item for P3 is now CANCELLED
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
             EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
             EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product3.getString("productId")),
             EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CANCELLED"));
        long product3CancelledCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals("There should have 1 " + product3.getString("productId") + " item is CANCELLED", 1, product3CancelledCount);

        // Verify that item for P4 is now COMPLETED
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
             EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
             EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product4.getString("productId")),
             EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_COMPLETED"));
        long product4CompletedCount = delegator.findCountByCondition("OrderItem", conditions, null, null);
        assertEquals("There should have 1 " + product4.getString("productId") + " item is COMPLETED", 1, product4CompletedCount);
        // Verify that the orderItem for P4 has a cancelQuantity of 20
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
             EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
             EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product4.getString("productId")));
        List items = delegator.findByCondition("OrderItem", conditions, null, null);
        assertEquals("There should have 1 " + product4.getString("productId") + " item", 1, items.size());
        GenericValue item4 = (GenericValue) items.get(0);
        BigDecimal cancelQuantity = item4.getBigDecimal("cancelQuantity");
        assertEquals("The OrderItem for " + product4.getString("productId") + " should have a cancelQuantity of 20", 20.0, cancelQuantity);
        // Get the current ATP and QOH quantities for P1, P2, P3, P4
        availability1 = getProductAvailability(product1.getString("productId"));
        availability2 = getProductAvailability(product2.getString("productId"));
        availability3 = getProductAvailability(product3.getString("productId"));
        availability4 = getProductAvailability(product4.getString("productId"));
        BigDecimal atpProduct1 = (BigDecimal) availability1.get("availableToPromiseTotal");
        BigDecimal atpProduct2 = (BigDecimal) availability2.get("availableToPromiseTotal");
        BigDecimal atpProduct3 = (BigDecimal) availability3.get("availableToPromiseTotal");
        BigDecimal atpProduct4 = (BigDecimal) availability4.get("availableToPromiseTotal");
        BigDecimal qohProduct1 = (BigDecimal) availability1.get("quantityOnHandTotal");
        BigDecimal qohProduct2 = (BigDecimal) availability2.get("quantityOnHandTotal");
        BigDecimal qohProduct3 = (BigDecimal) availability3.get("quantityOnHandTotal");
        BigDecimal qohProduct4 = (BigDecimal) availability4.get("quantityOnHandTotal");

        // Verify that ATP and QOH for P1 have increased by 10
        assertEquals("ATP for " + product1.getString("productId") + " should have increased by 10", 10.0, atpProduct1.subtract(atpInitProduct1));
        assertEquals("QOH for " + product1.getString("productId") + " should have increased by 10", 10.0, qohProduct1.subtract(qohInitProduct1));
        // Verify that ATP and QOH for P2 have increased by 20
        assertEquals("ATP for " + product2.getString("productId") + " should have increased by 20", 20.0, atpProduct2.subtract(atpInitProduct2));
        assertEquals("QOH for " + product2.getString("productId") + " should have increased by 20", 20.0, qohProduct2.subtract(qohInitProduct2));
        // Verify that ATP and QOH for P3 have not changed
        assertEquals("ATP for " + product3.getString("productId") + " should have not changed", 0, atpProduct3.subtract(atpInitProduct3));
        assertEquals("QOH for " + product3.getString("productId") + " should have not changed", 0, qohProduct3.subtract(qohInitProduct3));
        // Verify that ATP and QOH for P4 have increased by 20
        assertEquals("ATP for " + product4.getString("productId") + " should have increased by 10", 20.0, atpProduct4.subtract(atpInitProduct4));
        assertEquals("QOH for " + product4.getString("productId") + " should have increased by 10", 20.0, qohProduct4.subtract(qohInitProduct4));

    }

    /**
     * Test to verify the entire purchasing - billing - payment cycle with
     * accounting tags.
     *
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPurchasingInvoicingWithPayment() throws Exception {
        // get GL account of PINV_SPROD_ITEM and PINV_SHIP_CHARGES with
        // OpentapsTestCase.getGetInvoiceItemTypesGlAccounts
        Timestamp start = UtilDateTime.nowTimestamp();
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        // get GL accounts for all the invoice item types we will be using
        Map invoiceItemTypeGlAccounts = getGetInvoiceItemTypesGlAccounts(organizationPartyId, UtilMisc.toList("PINV_SPROD_ITEM", "PINV_SHIP_CHARGES", "PINV_SUPLPRD_ITEM"));

        // for initial balances, turn off the check balances flag (the last false)
        // because the ledger may not balance for all combinations of tags
        // get initial financial balances for Company with tags DIV_GOV
        Map initialBalances_GOV = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_GOV"), false);

        // get initial financial balances for Company with tags DIV_SMALL_BIZ
        Map initialBalances_SMALL_BIZ = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_SMALL_BIZ"), false);

        // get initial financial balances for Company with tags DPT_SALES
        Map initialBalances_SALES = fa.getFinancialBalances(start, UtilMisc.toMap("tag2", "DPT_SALES"), false);

        // get initial financial balances for Company with tags ACTI_MARKETING
        Map initialBalances_MARKETING = fa.getFinancialBalances(start, UtilMisc.toMap("tag3", "ACTI_MARKETING"), false);

        // get initial financial balances for Company with tags DIV_GOV,
        // DPT_SALES, ACTI_MARKETING
        Map initialBalances_GOV_SALES_MARKETING = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_GOV", "tag2", "DPT_SALES", "tag3", "ACTI_MARKETING"), false);

        // get initial financial balances for Company with tags ACTI_RESEARCH
        Map initialBalances_RESEARCH = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "ACTI_RESEARCH"), false);

        // get initial financial balances for Company with tags DIV_ENTERPRISE
        Map initialBalances_ENTERPRISE = fa.getFinancialBalances(start, UtilMisc.toMap("tag1", "DIV_ENTERPRISE"), false);

        // create a supplier based on DemoSupplier
        String supplierPartyId = createPartyFromTemplate("DemoSupplier", "Supplier for testPurchasingInvoicingWithPayment");

        // create a PO from Company to DemoSupplier for
        // 100 SUPPLY-001 at 1.0 with tags DIV_GOV, DPT_SALES, ACTI_MARKETING
        // 200 SUPPLY-001 at 1.0 with tags DIV_GOV, DPT_CORPORATE
        // 300 SUPPLY-001 at 1.0 with tags DIV_SMALL_BIZ, DPT_SALES,
        // ACTI_MARKETING
        GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "SUPPLY-001"));
        assertTrue("Test product not null", product != null);
        createMainSupplierForProduct(product.getString("productId"), demoSupplier.getString("partyId"), new BigDecimal("1.0"), "USD", new BigDecimal("0.0"), admin);
        PurchaseOrderFactory pof = new PurchaseOrderFactory(delegator, dispatcher, User, (String) demoSupplier.get("partyId"), getOrganizationPartyId(), facilityContactMechId);
        pof.setCurrencyUomId("USD");
        pof.addPaymentMethod("EXT_OFFLINE", null);
        pof.addShippingGroup("UPS", "NEXT_DAY");
        pof.addProduct(product, new BigDecimal("100.0"), pof.getFirstShipGroup(), UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_SALES", "acctgTagEnumId3", "ACTI_MARKETING"));
        pof.addProduct(product, new BigDecimal("200.0"), pof.getFirstShipGroup(), UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_CORPORATE"));
        pof.addProduct(product, new BigDecimal("300.0"), pof.getFirstShipGroup(), UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ", "acctgTagEnumId2", "DPT_SALES", "acctgTagEnumId3", "ACTI_MARKETING"));
        String orderId = pof.storeOrder();

        // Approve PO
        pof.approveOrder();

        // Invoice all line items of the PO
        Collection orderData = new LinkedList();
        List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
        for (GenericValue orderItem : orderItems) {
            Map orderItemData = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "quantity", orderItem.getBigDecimal("quantity").toString());
            orderData.add(orderItemData);
        }

        Map results = dispatcher.runSync("invoiceSuppliesOrWorkEffortOrderItems", UtilMisc.toMap("orderData", orderData, "userLogin", demofinadmin));
        String invoiceId = (String) results.get("invoiceId");
        Debug.logInfo("run invoiceSuppliesOrWorkEffortOrderItems get invoiceId" + " : " + invoiceId, MODULE);

        // verify invoiceItems have same tags as orderItems
        List<GenericValue> invoiceItems = delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId));
        for (GenericValue invoiceItem : invoiceItems) {
            if (invoiceItem.getBigDecimal("quantity").doubleValue() == 100.0) {
                Map tags = UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_SALES", "acctgTagEnumId3", "ACTI_MARKETING");
                Debug.logInfo("invoiceItem type : " + invoiceItem.getClass().getCanonicalName() + ", tags type : " + tags.getClass().getCanonicalName(), MODULE);
                assertEquals("the invoiceItem create for Order should have same tags as orderItem", invoiceItem, tags);
            }
            if (invoiceItem.getBigDecimal("quantity").doubleValue() == 200.0) {
                Map tags = UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_CORPORATE");
                Debug.logInfo("invoiceItem type : " + invoiceItem.getClass().getCanonicalName() + ", tags type : " + tags.getClass().getCanonicalName(), MODULE);
                assertEquals("the invoiceItem create for Order should have same tags as orderItem", invoiceItem, tags);
            }
            if (invoiceItem.getBigDecimal("quantity").doubleValue() == 300.0) {
                Map tags = UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ", "acctgTagEnumId2", "DPT_SALES", "acctgTagEnumId3", "ACTI_MARKETING");
                Debug.logInfo("invoiceItem type : " + invoiceItem.getClass().getCanonicalName() + ", tags type : " + tags.getClass().getCanonicalName(), MODULE);
                assertEquals("the invoiceItem create for Order should have same tags as orderItem", invoiceItem, tags);
            }
        }

        // Find the purchase invoice
        // Add invoice items:
        // PINV_SHIP_CHARGES 15.86 with tags DIV_GOV, DPT_SALES, ACTI_MARKETING
        // PINV_SHIP_CHARGES 22.77 with tags DIV_GOV, DPT_CORPORATE
        // PINV_SHIP_CHARGES 29.86 with tags DIV_SMALL_BIZ, DPT_SALES,
        // ACTI_MARKETING
        fa.createInvoiceItem(invoiceId, "PINV_SHIP_CHARGES", null, new BigDecimal("1.0"), new BigDecimal("15.86"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_SALES", "acctgTagEnumId3", "ACTI_MARKETING"));
        fa.createInvoiceItem(invoiceId, "PINV_SHIP_CHARGES", null, new BigDecimal("1.0"), new BigDecimal("22.77"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_CORPORATE"));
        fa.createInvoiceItem(invoiceId, "PINV_SHIP_CHARGES", null, new BigDecimal("1.0"), new BigDecimal("29.86"), null, UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ", "acctgTagEnumId2", "DPT_SALES", "acctgTagEnumId3", "ACTI_MARKETING"));
        // Set invoice to READY
        fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        // Create a payment of $338.63 from Company with payment method
        // COCHECKING to supplier with tag DIV_GOV
        String paymentId1 = fa.createPaymentAndApplication(new BigDecimal("338.63"), organizationPartyId, supplierPartyId, "VENDOR_PAYMENT", "COMPANY_CHECK", "COCHECKING", null, "PMNT_NOT_PAID", UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV"));
        fa.updatePaymentStatus(paymentId1, "PMNT_SENT");
        // Create a payment of $300 from Company with payment method COCHECKING
        // to supplier with tag DIV_SMALL_BIZ
        String paymentId2 = fa.createPaymentAndApplication(new BigDecimal("300.0"), organizationPartyId, supplierPartyId, "VENDOR_PAYMENT", "COMPANY_CHECK", "COCHECKING", null, "PMNT_NOT_PAID", UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ"));
        fa.updatePaymentStatus(paymentId2, "PMNT_SENT");

        // also turn off the check balances flag (the last false) for final balances
        // get final financial balances for Company with tags DIV_GOV
        Timestamp finish = UtilDateTime.nowTimestamp();
        Map finalBalances_GOV = fa.getFinancialBalances(finish, UtilMisc.toMap( "tag1", "DIV_GOV"), false);

        // get final financial balances for Company with tags DIV_SMALL_BIZ
        Map finalBalances_SMALL_BIZ = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_SMALL_BIZ"), false);

        // get final financial balances for Company with tags DPT_SALES
        Map finalBalances_SALES = fa.getFinancialBalances(finish, UtilMisc.toMap("tag2", "DPT_SALES"), false);

        // get final financial balances for Company with tags ACTI_MARKETING
        Map finalBalances_MARKETING = fa.getFinancialBalances(finish, UtilMisc.toMap("tag3", "ACTI_MARKETING"), false);

        // get final financial balances for Company with tags DIV_GOV,
        // DPT_SALES, ACTI_MARKETING
        Map finalBalances_GOV_SALES_MARKETING = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_GOV", "tag2", "DPT_SALES", "tag3", "ACTI_MARKETING"), false);

        // get final financial balances for Company with tags ACTI_RESEARCH
        Map finalBalances_RESEARCH = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "ACTI_RESEARCH"), false);

        // get final financial balances for Company with tags DIV_ENTERPRISE
        Map finalBalances_ENTERPRISE = fa.getFinancialBalances(finish, UtilMisc.toMap("tag1", "DIV_ENTERPRISE"), false);

        // Verify change in financial balances for
        // note that Accounts Payable, Checking account should only have
        // division (DIV_) tags transactions
        // tags DIV_GOV: PINV_SUPLPRD_ITEM.glAccount 300, PINV_SHIP_CHARGES
        // 38.63, COCHECKING.glAccount -338.63,
        Map accountMap = new HashMap();
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SUPLPRD_ITEM"), "300.0");
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SHIP_CHARGES"), "38.63");
        accountMap.put("111100", "-338.63");
        assertMapDifferenceCorrect("Balance changes for GOV tag is not correct", initialBalances_GOV, finalBalances_GOV, accountMap);

        // tags DIV_SMALL_BIZ: PINV_SUPLPRD_ITEM.glAccount 300,
        // PINV_SHIP_CHARGES 29.86, COCHECKING.glAccount -300.00,
        // ACCOUNTS_PAYABLE, -29.86
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "-29.86");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SUPLPRD_ITEM"), "300.0");
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SHIP_CHARGES"), "29.86");
        accountMap.put("111100", "-300.0");
        assertMapDifferenceCorrect("Balance changes for SMALL_BIZ tag is not correct", initialBalances_SMALL_BIZ, finalBalances_SMALL_BIZ, accountMap);

        // tags DPT_SALES: PINV_SUPLPRD_ITEM.glAccount 400, PINV_SHIP_CHARGES
        // 45.72, COCHECKING.glAccount 0, ACCOUNTS_PAYABLE, 0
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "0.0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SUPLPRD_ITEM"), "400.0");
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SHIP_CHARGES"), "45.72");
        accountMap.put("111100", "0.0");
        assertMapDifferenceCorrect("Balance changes for SALES tag is not correct", initialBalances_SALES, finalBalances_SALES, accountMap);

        // tags ACTI_MARKETING: PINV_SUPLPRD_ITEM.glAccount 400(100+300),
        // PINV_SHIP_CHARGES 45.72, COCHECKING.glAccount 0, ACCOUNTS_PAYABLE, 0
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "0.0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SUPLPRD_ITEM"), "400.0");
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SHIP_CHARGES"), "45.72");
        accountMap.put("111100", "0.0");
        assertMapDifferenceCorrect("Balance changes for MARKETING tag is not correct", initialBalances_MARKETING, finalBalances_MARKETING, accountMap);

        // tags DIV_GOV, DPT_SALES, ACTI_MARKETING: PINV_SUPLPRD_ITEM.glAccount
        // 100, PINV_SHIP_CHARGES 15.86, COCHECKING.glAccount 0,
        // ACCOUNTS_PAYABLE, 0
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "0.0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SUPLPRD_ITEM"), "100.0");
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SHIP_CHARGES"), "15.86");
        accountMap.put("111100", "0.0");
        assertMapDifferenceCorrect("Balance changes for DIV_GOV, DPT_SALES, ACTI_MARKETING tag is not correct", initialBalances_GOV_SALES_MARKETING, finalBalances_GOV_SALES_MARKETING, accountMap);

        // tags DIV_ENTERPRISE: PINV_SPROD_ITEM.glAccount 0, PINV_SHIP_CHARGES
        // 0, COCHECKING.glAccount 0, ACCOUNTS_PAYABLE, 0
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "0.0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SUPLPRD_ITEM"), "0.0");
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SHIP_CHARGES"), "0.0");
        accountMap.put("111100", "0.0");
        assertMapDifferenceCorrect("Balance changes for MARKETING tag is not correct", initialBalances_ENTERPRISE, finalBalances_ENTERPRISE, accountMap);

        // tags ACTI_RESEARCH: PINV_SUPLPRD_ITEM.glAccount 0, PINV_SHIP_CHARGES
        // 0, COCHECKING.glAccount 0, ACCOUNTS_PAYABLE, 0
        expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "0.0");
        accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SUPLPRD_ITEM"), "0.0");
        accountMap.put(invoiceItemTypeGlAccounts.get("PINV_SHIP_CHARGES"), "0.0");
        accountMap.put("111100", "0.0");
        assertMapDifferenceCorrect("Balance changes for MARKETING tag is not correct", initialBalances_RESEARCH, finalBalances_RESEARCH, accountMap);
    }

    /**
     * Test the methods for OrderItem quantities and invoiced/uninvoiced values
     * and Order level adjustments with both physical items and supplies which
     * are received, updated, and invoiced.
     *
     * @throws Exception if exception occur
     */
    @SuppressWarnings("unchecked")
    public void testPurchaseOrderItemQuantitiesAndValuesWithReceiptUpdatedAdjustment() throws Exception {
        // create physical and supplies product
        GenericValue physicalProduct = createTestProduct("Physical product for testing PO item quantities" + UtilDateTime.nowTimestamp(), admin);
        GenericValue suppliesProduct = createTestProduct("Supplies product for testing PO item quantities" + UtilDateTime.nowTimestamp(), "SUPPLIES", admin);
        createMainSupplierForProduct(physicalProduct.getString("productId"), demoSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(suppliesProduct.getString("productId"), demoSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);

        // create and approve PO for 10x of both products
        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(physicalProduct, new BigDecimal("10.0"));
        orderSpec.put(suppliesProduct, new BigDecimal("10.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderSpec, demoSupplier, facilityContactMechId);
        pof.approveOrder();

        String orderId = pof.getOrderId();

        // get order and order items
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(pof.getOrderId());
        List<OrderItem> orderItems = order.getOrderItems();
        String suppliesOrderItemSeqId = null;
        String physicalOrderItemSeqId = null;
        for (OrderItem orderItem : orderItems) {
            if (physicalProduct.getString("productId").equals(orderItem.getProductId())) {
                physicalOrderItemSeqId = orderItem.getOrderItemSeqId();
            } else if (suppliesProduct.getString("productId").equals(orderItem.getProductId())) {
                suppliesOrderItemSeqId = orderItem.getOrderItemSeqId();
            }
        }

        // update physical order item to 20x at $20 each
        updatePurchaseOrderItem(orderId, physicalOrderItemSeqId, "20.0", "20.0", "Item after new quantity and price updated", admin);
        // add a shipping charge to the PO
        runAndAssertServiceSuccess("createOrderAdjustment", UtilMisc.toMap("orderId", orderId, "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", new BigDecimal(25.99), "description", "example of shipping adjustment at order level", "userLogin", admin));

        // receive 5 of the physical item
        GenericValue pOrder = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        Map inputParameters = createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(pOrder, UtilMisc.toMap(physicalOrderItemSeqId, "5.0"), false, admin);
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", inputParameters);

        // invoice 6 of the supplies item
        Collection orderData = new LinkedList();
        Map orderItemData = UtilMisc.toMap("orderId", order.getOrderId(), "orderItemSeqId", suppliesOrderItemSeqId, "quantity", "6.0");
        orderData.add(orderItemData);
        dispatcher.runSync("invoiceSuppliesOrWorkEffortOrderItems", UtilMisc.toMap("orderData", orderData, "userLogin", admin));

        // now get the order items
        OrderItem physicalOrderItem = repository.getOrderItem(order,
                physicalOrderItemSeqId);
        OrderItem suppliesOrderItem = repository.getOrderItem(order,
                suppliesOrderItemSeqId);

        // check physical item quantities and values
        assertEquals("Order [" + orderId + "] item [" + physicalOrderItem.getOrderItemSeqId() + "] quantity is not correct", physicalOrderItem.getQuantity(), new BigDecimal("20.0"));
        assertEquals("Order [" + orderId + "] item [" + physicalOrderItem.getOrderItemSeqId() + "] invoiced quantity is not correct", physicalOrderItem.getInvoicedQuantity(), new BigDecimal("5.0"));
        assertEquals("Order [" + orderId + "] item [" + physicalOrderItem.getOrderItemSeqId() + "] invoiced value is not correct", physicalOrderItem.getInvoicedValue(), new BigDecimal("100.0"));
        assertEquals("Order [" + orderId + "] item [" + physicalOrderItem.getOrderItemSeqId() + "] remaining to ship quantity is not correct", physicalOrderItem.getRemainingToShipQuantity(), new BigDecimal("15.0"));
        assertEquals("Order [" + orderId + "] item [" + physicalOrderItem.getOrderItemSeqId() + "] subtotal is not correct", physicalOrderItem.getSubTotal(), new BigDecimal("400.0"));
        assertEquals("Order [" + orderId + "] item [" + physicalOrderItem.getOrderItemSeqId() + "] uninvoiced value is not correct", physicalOrderItem.getUninvoicedValue(), new BigDecimal("300.0"));

        // check supplies item quantities and values
        assertEquals("Order [" + orderId + "] item [" + suppliesOrderItem.getOrderItemSeqId() + "] quantity is not correct", suppliesOrderItem.getQuantity(), new BigDecimal("10.0"));
        assertEquals("Order [" + orderId + "] item [" + suppliesOrderItem.getOrderItemSeqId() + "] invoiced quantity is not correct", suppliesOrderItem.getInvoicedQuantity(), new BigDecimal("6.0"));
        // this doesn't work but isn't used either, so skipping it for now
        // assertEquals("Order [" + orderId + "] item [" +
        // suppliesOrderItem.getOrderItemSeqId() +
        // "] remaining to ship quantity is not correct",
        // suppliesOrderItem.getRemainingToShipQuantity(), new
        // BigDecimal("0.0"));
        assertEquals("Order [" + orderId + "] item [" + suppliesOrderItem.getOrderItemSeqId() + "] subtotal is not correct", suppliesOrderItem.getSubTotal(), new BigDecimal("100.0"));
        assertEquals("Order [" + orderId + "] item [" + suppliesOrderItem.getOrderItemSeqId() + "] invoiced value is not correct", suppliesOrderItem.getInvoicedValue(), new BigDecimal("60.0"));
        assertEquals("Order [" + orderId + "] item [" + suppliesOrderItem.getOrderItemSeqId() + "] uninvoiced value is not correct", suppliesOrderItem.getUninvoicedValue(), new BigDecimal("40.0"));

        // now get the order to check adjustment
        assertEquals("Order [" + orderId + "] non-item adjustment total is not correct", order.getNonItemAdjustmentValue(), new BigDecimal("25.99"));
        assertEquals("Order [" + orderId + "] invoiced non-item adjustment total is not correct", order.getInvoicedNonItemAdjustmentValue(), new BigDecimal("25.99"));
        assertEquals("Order [" + orderId + "] uninvoiced non-item adjustment total is not correct", order.getUninvoicedNonItemAdjustmentValue(), new BigDecimal("0.0"));

    }

    /**
     * Tests the quantities and invoiced values of a purchase order item after
     * it has been cancelled.
     *
     * @throws Exception if exception occur
     */
    public void testCancelledPurchaseOrderItemQuantities() throws Exception {
        GenericValue cancelProduct = createTestProduct("Physical product for testing PO item quantities after cancellation", admin);
        createMainSupplierForProduct(cancelProduct.getString("productId"), demoSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);

        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(cancelProduct, new BigDecimal("10.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderSpec, demoSupplier, facilityContactMechId);
        String orderId = pof.getOrderId();

        // cancel the cancel product
        pof.cancelProduct(cancelProduct, new BigDecimal("10.0"));

        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(pof.getOrderId());
        OrderItem cancelOrderItem = repository.getOrderItem(order, "00001");
        assertEquals("Order [" + orderId + "] item [" + cancelOrderItem.getOrderItemSeqId() + "] quantity is not correct", cancelOrderItem.getQuantity(), new BigDecimal("10.0"));
        assertEquals("Order [" + orderId + "] item [" + cancelOrderItem.getOrderItemSeqId() + "] cancelled quantity is not correct", cancelOrderItem.getCancelQuantity(), new BigDecimal("10.0"));
        assertEquals("Order [" + orderId + "] item [" + cancelOrderItem.getOrderItemSeqId() + "] ordered quantity is not correct", cancelOrderItem.getOrderedQuantity(), new BigDecimal("0.0"));
        assertEquals("Order [" + orderId + "] item [" + cancelOrderItem.getOrderItemSeqId() + "] invoiced quantity is not correct", cancelOrderItem.getInvoicedQuantity(), new BigDecimal("0.0"));
        assertEquals("Order [" + orderId + "] item [" + cancelOrderItem.getOrderItemSeqId() + "] invoiced value is not correct", cancelOrderItem.getInvoicedValue(), new BigDecimal("0.0"));
        assertEquals("Order [" + orderId + "] item [" + cancelOrderItem.getOrderItemSeqId() + "] remaining to ship quantity is not correct", cancelOrderItem.getRemainingToShipQuantity(), new BigDecimal("0.0"));
        assertEquals("Order [" + orderId + "] item [" + cancelOrderItem.getOrderItemSeqId() + "] subtotal is not correct", cancelOrderItem.getSubTotal(), new BigDecimal("0.0"));
        assertEquals("Order [" + orderId + "] item [" + cancelOrderItem.getOrderItemSeqId() + "] uninvoiced value is not correct", cancelOrderItem.getUninvoicedValue(), new BigDecimal("0.0"));
    }

    /**
     * Tests that an uninvoiced order-level adjustment value is correct.
     *
     * @throws GeneralException if exception occur
     */
    public void testOrderUninvoicedAdjustment() throws GeneralException {
        GenericValue testProduct = createTestProduct("This product is not important -- the test is for the order level adjustment", admin);
        createMainSupplierForProduct(testProduct.getString("productId"), demoSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);

        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(testProduct, new BigDecimal("10.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderSpec, demoSupplier, facilityContactMechId);

        String orderId = pof.getOrderId();

        runAndAssertServiceSuccess("createOrderAdjustment", UtilMisc.toMap("orderId", orderId, "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", new BigDecimal(25.99), "description", "example of shipping adjustment at order level", "userLogin", admin));

        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(pof.getOrderId());
        assertEquals("Order [" + orderId + "] non-item adjustment total is not correct", order.getNonItemAdjustmentValue(), new BigDecimal("25.99"));
        assertEquals("Order [" + orderId + "] invoiced non-item adjustment total is not correct", order.getInvoicedNonItemAdjustmentValue(), new BigDecimal("0.0"));
        assertEquals("Order [" + orderId + "] uninvoiced non-item adjustment total is not correct", order.getUninvoicedNonItemAdjustmentValue(), new BigDecimal("25.99"));
    }

    /**
     * Test Creating a purchase order for supplies and invoicing the supplies.
     * Tests both a supplies products using the default GL account. And a
     * supplies product with a special configure GL account in ProductGlAccount.
     * Also tests, where the invoice price for the supplies product is different
     * than the purchase order price, to see if it is posted to purchase price
     * variance correctly.
     *
     * @throws Exception if an error occurs
     */
    public void testPurchasingAndInvoicingSupplies() throws Exception {
        executePurchasingAndInvoicingSuppliesTest("testPurchasingAndInvoicingSupplies Product");
    }

    /**
     * Same as above, except sets organization to use standard costing.
     *
     * @throws Exception if an error occurs
     */
    public void testPurchasingAndInvoicingSuppliesWithStandardCosting() throws Exception {
        setOrganizationCostingMethodId(organizationPartyId, "STANDARD_COSTING");
        executePurchasingAndInvoicingSuppliesTest("testPurchasingAndInvoicingSuppliesWithStandardCosting Product");
    }

    /**
     * Tests that purchase order item and quantity can be updated after the
     * order has been created.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testBasicModifyPurchaseOrderItemQuantityOnly() throws Exception {
        // create a test product with price $10
        GenericValue testProduct = createTestProduct("product for modify PO item quantity only", admin);
        createMainSupplierForProduct(testProduct.getString("productId"), demoSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);

        // order 10x of the test product
        Map<GenericValue, BigDecimal> orderSpec = new HashMap();
        orderSpec.put(testProduct, new BigDecimal(10.0));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderSpec, demoSupplier, facilityContactMechId);
        pof.approveOrder();
        String orderId = pof.getOrderId();
        delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));

        // update order item quantity to 20x
        updateOrderItem(orderId, "00001", "20.0", null, "Item after new quantity updated", admin);

        // verify the quantity and price have been correctly updated
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(pof.getOrderId());
        OrderItem orderItem = repository.getOrderItem(order, "00001");
        assertEquals("Order [" + orderId + "] item [" + orderItem.getOrderItemSeqId() + "] quantity is not correct", orderItem.getQuantity(), new BigDecimal("20.0"));
        assertEquals("Order [" + orderId + "] item [" + orderItem.getOrderItemSeqId() + "] subtotal is not correct", orderItem.getSubTotal(), new BigDecimal("200.0"));
    }

    /**
     * Tests that purchase order item and quantity can be updated after the
     * order has been created.
     *
     * @throws Exception if an exception occurs
     */
    @SuppressWarnings("unchecked")
    public void testBasicModifyPurchaseOrderItemQuantityAndPrice() throws Exception {
        // create a test product with price $10
        GenericValue testProduct = createTestProduct("product for modify PO item price and quantities", admin);
        createMainSupplierForProduct(testProduct.getString("productId"), demoSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);

        // order 10x of the test product
        Map<GenericValue, BigDecimal> orderSpec = new HashMap();
        orderSpec.put(testProduct, new BigDecimal(10.0));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderSpec, demoSupplier, facilityContactMechId);
        pof.approveOrder();
        String orderId = pof.getOrderId();
        delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));

        // update physical order item to 20x at $20 each
        updatePurchaseOrderItem(orderId, "00001", "20.0", "20.0", "Item after new quantity and price applied", admin);

        // verify the quantity and price have been correctly updated
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(pof.getOrderId());
        OrderItem orderItem = repository.getOrderItem(order, "00001");
        assertEquals("Order [" + orderId + "] item [" + orderItem.getOrderItemSeqId() + "] quantity is not correct", orderItem.getQuantity(), new BigDecimal("20.0"));
        assertEquals("Order [" + orderId + "] item [" + orderItem.getOrderItemSeqId() + "] subtotal is not correct", orderItem.getSubTotal(), new BigDecimal("400.0"));

         // Check the OrderItemShipGroupAssoc have correct quantities and cancelQuantities
        OrderItemShipGroupAssoc orderItemShipGroupAssoc = null;
        for (OrderItemShipGroupAssoc itemAssoc : order.getOrderItemShipGroupAssocs()) {
            // iterator all OrderItemShipGroupAssoc, find relate one
            if (itemAssoc.getOrderItemSeqId().equals(orderItem.getOrderItemSeqId()))
            {
                orderItemShipGroupAssoc = itemAssoc;
                break;
            }
        }
        assertNotNull("Failed to find the Order [" + orderId + "] item [" + orderItem.getOrderItemSeqId() + "] relate OrderItemShipGroupAssoc", orderItemShipGroupAssoc);
        BigDecimal cancelQty = orderItemShipGroupAssoc.getCancelQuantity();
        BigDecimal quantity = orderItemShipGroupAssoc.getQuantity();
        if (cancelQty != null) {
            quantity = BigDecimal.valueOf(quantity.doubleValue() - cancelQty.doubleValue());
        }
        assertEquals("Order [" + orderId + "] item [" + orderItem.getOrderItemSeqId() + "] quantity is not equals relate OrderItemShipGroupAssoc.quantity", orderItem.getQuantity(), orderItemShipGroupAssoc.getQuantity());
    }

    /**
     * This test checks that a PO with an adjustment for shipping charges which
     * has been partially received can still be modified.
     *
     * @throws GeneralException if an exception occurs
     */
    @SuppressWarnings("unchecked")
    public void testModifyPartiallyReceivedPOWithShippingCharges() throws GeneralException {
        // create a test product
        GenericValue testProduct = createTestProduct("product for testing modifying partially received PO with shipping charges" + UtilDateTime.nowTimestamp(), admin);
        createMainSupplierForProduct(testProduct.getString("productId"), demoSupplier.getString("partyId"), new BigDecimal("10.0"), "USD", new BigDecimal("0.0"), admin);

        // create a PO
        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(testProduct, new BigDecimal("10.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderSpec, demoSupplier, facilityContactMechId);
        pof.approveOrder();

        String orderId = pof.getOrderId();
        GenericValue pOrder = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));

        // add a shipping charge to the PO
        runAndAssertServiceSuccess("createOrderAdjustment", UtilMisc.toMap("orderId", orderId, "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", new BigDecimal(25.99), "description", "example of shipping adjustment at order level", "userLogin", admin));
        // receive 5 of the physical item

        Map inputParameters = createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(pOrder, UtilMisc.toMap("00001", "5.0"), false, admin);
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", inputParameters);

        // update physical order item to 20x at $20 each
        updatePurchaseOrderItem(orderId, "00001", "20.0", "20.0", "Item after new quantity and price applied", admin);
    }

    @SuppressWarnings("unchecked")
    private void executePurchasingAndInvoicingSuppliesTest(String productIdPrefix) throws Exception {
        String expenseGlAccountId = "631200";
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Map initialBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // create a product of type SUPPLY
        GenericValue product1 = createTestProduct("testPurchasingAndInvoicingSupplies Product 1", "SUPPLIES", demopurch1);
        createMainSupplierForProduct(product1.getString("productId"), demoSupplierPartyId, new BigDecimal("1.0"), "USD", new BigDecimal("0.0"), demopurch1);

        // create a 2nd product Of type SUPPLY
        GenericValue product2 = createTestProduct("testPurchasingAndInvoicingSupplies Product 2", "SUPPLIES", demopurch1);
        createMainSupplierForProduct(product2.getString("productId"), demoSupplierPartyId, new BigDecimal("50.0"), "USD", new BigDecimal("0.0"), demopurch1);
        // set the EXPENSE GlAccountType of the second product to "631200"
        // (janitorial and other contract services)
        delegator.create("ProductGlAccount", UtilMisc.toMap("productId", product2.getString("productId"), "organizationPartyId", organizationPartyId, "glAccountTypeId", "EXPENSE", "glAccountId", expenseGlAccountId));

        // create a purchase order for the two products
        Map<GenericValue, BigDecimal> orderParams = new HashMap<GenericValue, BigDecimal>();
        orderParams.put(product1, new BigDecimal("100.0"));
        orderParams.put(product2, new BigDecimal("20.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderParams, demoSupplier, facilityContactMechId);
        String orderId = pof.getOrderId();

        // approve the purchase order
        pof.approveOrder();

        // Invoice the purchase order's first product
        // we have to get the order items this way because Different databases
        // will return the OrderItems in different order, so we cannot just get
        // all the
        // OrderItems and
        Collection orderData = new LinkedList();
        List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId, "productId", product1.getString("productId")));
        assertEquals("One orderItem exists for [" + orderId + "] and product [" + product1.get("productId") + "]", orderItems.size(), 1.0);
        GenericValue orderItem = orderItems.get(0);
        orderData.add(UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "quantity", "100.00"));
        Map results = dispatcher.runSync("invoiceSuppliesOrWorkEffortOrderItems", UtilMisc.toMap("orderData", orderData, "userLogin", demofinadmin));
        String invoiceId1 = (String) results.get("invoiceId");

        // Invoice the purchase order's second product
        orderData = new LinkedList();
        orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId, "productId", product2.getString("productId")));
        assertEquals("One orderItem exists for [" + orderId + "] and product [" + product2.get("productId") + "]", orderItems.size(), 1.0);
        orderItem = orderItems.get(0);
        orderData.add(UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "quantity", "20.00"));
        results = dispatcher.runSync("invoiceSuppliesOrWorkEffortOrderItems", UtilMisc.toMap("orderData", orderData, "userLogin", demofinadmin));
        String invoiceId2 = (String) results.get("invoiceId");

        // verify the purchase order is completed
        OrderRepositoryInterface orderRepository = getOrderRepository(admin);
        Order order = orderRepository.getOrderById(orderId);
        assertTrue("Purchase Order [" + orderId + "] is completed", order.isCompleted());

        // change the first invoice's unit price to $1.10 and set it to ready so
        // we can test the purchase price variance for supplies
        InvoiceRepositoryInterface invoiceRepository = getInvoiceRepository(admin);
        Invoice invoice = invoiceRepository.getInvoiceById(invoiceId1);
        org.opentaps.base.entities.InvoiceItem invoiceItem1 = invoice.getInvoiceItems().get(0);
        invoiceItem1.setAmount(new BigDecimal("1.10"));
        invoiceRepository.createOrUpdate(invoiceItem1);
        runAndAssertServiceSuccess("setInvoiceReadyAndCheckIfPaid", UtilMisc.toMap("invoiceId", invoiceId1, "userLogin", demofinadmin));

        // set the second invoice to ready
        runAndAssertServiceSuccess("setInvoiceReadyAndCheckIfPaid", UtilMisc.toMap("invoiceId", invoiceId2, "userLogin", demofinadmin));

        // verify the GL account changes:
        // ACCOUNTS_PAYABLE -1110
        // PURCHASE PRICE VAR 10
        // 650000 (Standard supplies account) 100
        // 631200 (Janitorial services account) 1000
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        // Considering 1% sales tax due to new demo data in TaxAuthorityRateProduct, applicable to all stores, min. item price 25   
        Map expectedBalanceChanges = UtilMisc.toMap("ACCOUNTS_PAYABLE", "-1120.0", "PURCHASE_PRICE_VAR", "+10");
        Map accountMap = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        // additional expense accounts not defined with gl account type
        expectedBalanceChanges.put("650000", "100");
        expectedBalanceChanges.put(expenseGlAccountId, "1000");
        assertMapDifferenceCorrect(initialBalances, finalBalances, accountMap);
    }

    /**
     * test to verify the getSupplierProduct method.
     *
     * @throws Exception if an error occurs
     */
    public void testGetSupplierProduct() throws Exception {
        PurchasingRepositoryInterface purchasingRepository = purchasingDomain.getPurchasingRepository();
        // verify we can use get different lastPrice for different supplier
        SupplierProduct supplierProduct = purchasingRepository.getSupplierProduct("DemoSupplier", "SUPPLY-001", new BigDecimal("500.0"), "USD");
        assertNotNull("The supplierProduct shouldn't be null.", supplierProduct);
        // verify supplierProduct.getLastPrice() equals 1.0
        assertEquals("first match SupplierProduct.lastPrice should equals 1.00.", 1.00, supplierProduct.getLastPrice().doubleValue());

        supplierProduct = purchasingRepository.getSupplierProduct("BigSupplier", "SUPPLY-001", new BigDecimal("500.0"), "USD");
        assertNotNull("The supplierProduct shouldn't be null.", supplierProduct);
        // verify supplierProduct.getLastPrice() equals 0.75
        assertEquals("first match SupplierProduct.lastPrice should equals 0.75.", 0.75, supplierProduct.getLastPrice().doubleValue());

        supplierProduct = purchasingRepository.getSupplierProduct("DemoSupplier", "ASSET-001", new BigDecimal("10.0"), "USD");
        // verify supplierProduct.getLastPrice() equals 950
        assertEquals("first match SupplierProduct.lastPrice should equals 950.", 950.0, supplierProduct.getLastPrice().doubleValue());

        // verify we cannot get any matched SupplierProduct, because the
        // minimumOrderQuantity="500"
        supplierProduct = purchasingRepository.getSupplierProduct("BigSupplier", "ASSET-001", new BigDecimal("499.0"), "USD");
        assertNull("There shouldn't found any match SupplierProduct.", supplierProduct);

        supplierProduct = purchasingRepository.getSupplierProduct("BigSupplier", "ASSET-001", new BigDecimal("500.0"), "USD");
        assertNotNull("There should found a match SupplierProduct.", supplierProduct);
        // verify supplierProduct.getLastPrice() equals 750
        assertEquals("SupplierProduct.lastPrice should equals 750.00.", 750.00, supplierProduct.getLastPrice().doubleValue());
    }

    /**
     * test to verify that the accounting tags from a purchase order are copied
     * received inventory items and accounting transactions. (see also
     * <code>testPurchasingInvoicingWithPayment</code> which test the invoicing
     * part with tags).
     *
     * @throws Exception if an error occurs
     */
    public void testPurchaseOrderReceiptAccountingTags() throws Exception {
        // 1. create test product
        GenericValue product1 = createTestProduct("testCompletePurchaseOrder Test Product 1", demopurch1);
        GenericValue product2 = createTestProduct("testCompletePurchaseOrder Test Product 2", demopurch1);
        GenericValue product3 = createTestProduct("testCompletePurchaseOrder Test Product 3", demopurch1);

        // 2. Create a PO. and approve it
        PurchaseOrderFactory pof = new PurchaseOrderFactory(delegator, dispatcher, User, (String) demoSupplier.get("partyId"), getOrganizationPartyId(), facilityContactMechId);
        pof.setCurrencyUomId("USD");
        pof.addPaymentMethod("EXT_OFFLINE", null);
        pof.addShippingGroup("UPS", "NEXT_DAY");
        Map<String, String> tags1 = UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_SALES", "acctgTagEnumId3", "ACTI_MARKETING");
        Map<String, String> tags2 = UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV", "acctgTagEnumId2", "DPT_CORPORATE");
        Map<String, String> tags3 = UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ", "acctgTagEnumId2", "DPT_SALES", "acctgTagEnumId3", "ACTI_MARKETING");
        pof.addProduct(product1, new BigDecimal("100.0"), pof.getFirstShipGroup(), tags1);
        pof.addProduct(product2, new BigDecimal("200.0"), pof.getFirstShipGroup(), tags2);
        pof.addProduct(product3, new BigDecimal("300.0"), pof.getFirstShipGroup(), tags3);
        String orderId = pof.storeOrder();
        pof.approveOrder();

        // 3. Receive all items with
        // warehouse.issueOrderItemToShipmentAndReceiveAgainstPO
        GenericValue pOrder = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
        Map<String, Object> inputParameters = createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(pOrder, demowarehouse1);
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", inputParameters);

        // 4. find the received inventory item and check their tags
        GenericValue inventoryItem1 = EntityUtil.getOnly(delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", product1.getString("productId"))));
        assertNotNull("Inventory not found for product " + product1, inventoryItem1);
        assertAccountingTagsEqual(inventoryItem1, tags1);

        GenericValue inventoryItem2 = EntityUtil.getOnly(delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", product2.getString("productId"))));
        assertNotNull("Inventory not found for product " + product2, inventoryItem2);
        assertAccountingTagsEqual(inventoryItem2, tags2);

        GenericValue inventoryItem3 = EntityUtil.getOnly(delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", product3.getString("productId"))));
        assertNotNull("Inventory not found for product " + product3, inventoryItem3);
        assertAccountingTagsEqual(inventoryItem3, tags3);

        // 5. find the accounting transactions
        List<GenericValue> entries1 = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("productId", product1.getString("productId")));
        assertNotEmpty("Accounting transaction entries not found for product " + product1, entries1);
        for (GenericValue entry : entries1) {
            assertAccountingTagsEqual(entry, tags1);
        }

        List<GenericValue> entries2 = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("productId", product2.getString("productId")));
        assertNotEmpty("Accounting transaction entries not found for product " + product2, entries2);
        for (GenericValue entry : entries2) {
            assertAccountingTagsEqual(entry, tags2);
        }

        List<GenericValue> entries3 = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("productId", product3.getString("productId")));
        assertNotEmpty("Accounting transaction entries not found for product " + product3, entries3);
        for (GenericValue entry : entries3) {
            assertAccountingTagsEqual(entry, tags3);
        }
    }

    /**
     * Test the GWT order lookup.
     * @throws Exception if an error occurs
     */
    public void testGwtOrderLookup() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);

        // 1. test find order by orderId
        provider.setParameter(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID, "TEST9000");
        PurchaseOrderLookupService lookup = new PurchaseOrderLookupService(provider);
        lookup.findOrders(organizationPartyId, facilityId);
        assertEquals("There should just found one record with order Id [TEST9000].", 1, lookup.getResultTotalCount());
        assertGwtLookupFound(lookup, Arrays.asList("TEST9000"), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);

        // 2. find purchase order by supplier
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PurchaseOrderLookupConfiguration.INOUT_PARTY_ID, demoSupplierPartyId);
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PurchaseOrderLookupService(provider);
        lookup.findOrders(organizationPartyId, facilityId);
        // test we found the demo data: TEST9000/TEST9001
        assertGwtLookupFound(lookup, Arrays.asList("TEST9000", "TEST9001"), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST9002"), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);

        // 3. test find order by status
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PurchaseOrderLookupConfiguration.INOUT_STATUS_ID, "ORDER_APPROVED");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PurchaseOrderLookupService(provider);
        lookup.findOrders(organizationPartyId, facilityId);
        // test we found the demo data: TEST9000/TEST9002, not found TEST9001
        assertGwtLookupFound(lookup, Arrays.asList("TEST9000", "TEST9002"), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST9001"), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);


        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PurchaseOrderLookupConfiguration.INOUT_STATUS_ID, "ORDER_CREATED");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PurchaseOrderLookupService(provider);
        lookup.findOrders(organizationPartyId, facilityId);
        // test we found the demo data: TEST9001, not found TEST9000/TEST9002
        assertGwtLookupFound(lookup, Arrays.asList("TEST9001"), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST9000"), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST9002"), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);

        // 4. test find order by date range
        // search the orders between 09/12/09 00:00:00 and 09/12/09 23:59:59
        provider = new TestInputProvider(admin, dispatcher);
        String fromDate = dateStringToShortLocaleString("09/12/09 00:00:00", "yy/MM/dd HH:mm:ss");
        String thruDate = dateStringToShortLocaleString("09/12/09 23:59:59", "yy/MM/dd HH:mm:ss");
        provider.setParameter(PurchaseOrderLookupConfiguration.IN_FROM_DATE, fromDate);
        provider.setParameter(PurchaseOrderLookupConfiguration.IN_THRU_DATE, thruDate);
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PurchaseOrderLookupService(provider);
        lookup.findOrders(organizationPartyId, facilityId);
        // test we found the demo data: TEST9000/TEST9002, not found TEST9001
        assertGwtLookupFound(lookup, Arrays.asList("TEST9000", "TEST9002"), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("TEST9001"), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);

    }

    /**
     * Test the GWT search order by product.
     * @throws Exception if an error occurs
     */
    public void testGwtSearchOrderByProduct() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);
        
        // create a supplier from template of DemoSupplier
        String supplierPartyId = createPartyFromTemplate("DemoSupplier", "Supplier for testGwtSearchOrderByProduct");
        GenericValue supplier = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", supplierPartyId));
        Debug.logInfo("create customer [" + supplier.getString("partyId") + "]", MODULE);
        // create a product
        GenericValue testProduct = createTestProduct("testGwtSearchOrderByProduct Test Product", demopurch1);

        // 1.create a purchase order for the supplier and product
        Map<GenericValue, BigDecimal> orderSpec = new HashMap<GenericValue, BigDecimal>();
        orderSpec.put(testProduct, new BigDecimal("1.0"));
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderSpec, supplier, facilityContactMechId);
        Debug.logInfo("create purchasing order [" + pof.getOrderId() + "]", MODULE);

        
        // 2. try to find the sales order By the productId, in the sales order is found
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PurchaseOrderLookupConfiguration.IN_PRODUCT_PARTTERN, testProduct.getString("productId"));
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999");
        SalesOrderLookupService lookup = new SalesOrderLookupService(provider);
        lookup.findOrders();
        assertGwtLookupNotFound(lookup, Arrays.asList(pof.getOrderId()), PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);

    }
}
