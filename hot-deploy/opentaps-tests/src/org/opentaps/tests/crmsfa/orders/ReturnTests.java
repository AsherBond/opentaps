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

package org.opentaps.tests.crmsfa.orders;

import java.math.BigDecimal;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.common.order.UtilOrder;

/**
 * Tests for returns.
 */
public class ReturnTests extends ReturnTestCase {

    private static final String MODULE = OrderTests.class.getName();

    GenericValue DemoCSR;
    GenericValue DemoCustomer;
    GenericValue DemoSalesManager;
    GenericValue demowarehouse1;
    GenericValue admin;
    static final String productStoreId = "9000";
    static final String facilityId = "WebStoreWarehouse";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        DemoCSR = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "DemoCSR"));
        DemoCustomer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoCustomer"));
        DemoSalesManager = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        demowarehouse1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        admin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
        // test that the object have been retrieved
        assertTrue("DemoCSR not null", DemoCSR != null);
        assertTrue("DemoCustomer not null", DemoCustomer != null);
        assertTrue("DemoSalesManager not null", DemoSalesManager != null);
        assertTrue("demowarehouse1 not null", demowarehouse1 != null);
        assertTrue("admin not null", admin != null);
        // set a default User
        User = DemoCSR;

        // set the value here since some tests are changing it:
        setProductStorePaymentService(productStoreId, "CREDIT_CARD", "PRDS_PAY_REFUND", "testCCRefund");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        DemoCSR = null;
        DemoCustomer = null;
        demowarehouse1 = null;
        admin = null;
    }

    /**
     * Basic returns test: verifies that a return with refund is correctly handled
     * 1.  Create a product
     * 2.  Receive 10 units of it
     * 3.  Create a sales order for 5 units to DemoCustomer with the credit card payment methodId 9015
     * 4.  Ship entire order (use the testShipOrder service)
     * 5.  Verify that the ATP and QOH inventory of the product is now 5.0
     * 6.  Create a return from the order for 2.0 of the items
     * 7.  Set the order's Product Store's ProductStorePaymentSetting for paymentMethodType=CREDIT CARD and paymentServiceTypeEnumId=ProductStorePaymentSetting to testCCRefund
     * 8.  Accept the return
     * 9.  Verify that:
     * 9a.   The return is completed
     * 9b.   Inventory of the item has increased by 2.0
     * 9c.   A payment of the type CUSTOMER_REFUND from Company to DemoCustomer for the amount of the return in the status of PMNT_SENT has been created
     * 10.  Try creating a second return for 5.0 items from the order in step (3)
     * 11.  Verify that the system will not allow creating a return for this quantity (because we have already returned 2, so only 3 are left)
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testBasicReturn() throws GeneralException {
        // 1. create test product
        GenericValue testProduct = createTestProduct("Product for Basic Return Test", demowarehouse1);
        String productId = testProduct.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(testProduct, new BigDecimal("100.0"), admin);

        // 2. Receive 10 units
        receiveInventoryProduct(testProduct, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);

        // 3. sales order of 5 units to DemoCustomer with payment method 9015
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(testProduct, new BigDecimal("5.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, productStoreId, "CREDIT_CARD", "9015", null);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testBasicReturn created order [" + orderId + "]", MODULE);

        // authorize payment for the order
        Map results = runAndAssertServiceSuccess("authOrderPayments", UtilMisc.toMap("orderId", orderId, "userLogin", admin));
        String authResult = (String) results.get("processResult");
        assertEquals("Auth result", "APPROVED", authResult);

        // capture payment for the order
        BigDecimal orderAmount = UtilOrder.getOrderOpenAmount(delegator, orderId);
        results = runAndAssertServiceSuccess("captureOrderPayments", UtilMisc.<String, Object>toMap("orderId", orderId, "captureAmount", orderAmount, "userLogin", admin));
        String captureResult = (String) results.get("processResult");
        assertEquals("Capture result", "COMPLETE", captureResult);

        // 4. ship the order
        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", orderId, "facilityId", facilityId, "userLogin", demowarehouse1));

        // 5. verify ATP / QOH is 5.0
        assertProductAvailability(testProduct, new BigDecimal("5.0"), new BigDecimal("5.0"));

        // needed later to check that no extra payment is created
        long initial_payments = countPayments("Company", "DemoCustomer", "CUSTOMER_REFUND", "PMNT_NOT_PAID");

        // 6. create a return for 2.0 items
        results = runAndAssertServiceSuccess("crmsfa.createReturnFromOrder", UtilMisc.toMap("orderId", orderId, "userLogin", User));
        String returnId = (String) results.get("returnId");
        Debug.logInfo("testBasicReturn created return [" + returnId + "]", MODULE);

        Map callCtx = new HashMap();
        callCtx.put("userLogin", User);
        callCtx.put("orderId", orderId);
        callCtx.put("returnReasonId", "RTN_NOT_WANT");
        callCtx.put("returnTypeId", "RTN_REFUND");
        callCtx.put("returnItemTypeId", "RET_PROD_ITEM");
        callCtx.put("returnId", returnId);
        callCtx.put("orderItemSeqId", "00001");
        callCtx.put("productId", productId);
        callCtx.put("returnPrice", new BigDecimal("100.0"));
        callCtx.put("returnQuantity", new BigDecimal("2.0"));
        callCtx.put("description", testProduct.get("description"));

        runAndAssertServiceSuccess("createReturnItem", callCtx);

        // 7. Set the order's Product Store's ProductStorePaymentSetting for paymentMethodType=CREDIT CARD and paymentServiceTypeEnumId=PRDS_PAY_REFUND to testCCRefund
        setProductStorePaymentService(productStoreId, "CREDIT_CARD", "PRDS_PAY_REFUND", "testCCRefund");

        // 8. Accept the return
        runAndAssertServiceSuccess("crmsfa.acceptReturn", UtilMisc.toMap("userLogin", admin, "returnId", returnId));

        // 9a. Verify that the return is completed
        assertReturnStatusEquals(returnId, "RETURN_COMPLETED");
        assertReturnItemsStatusEquals(returnId, "RETURN_COMPLETED");

        // 9b. Verify that the inventory of the item has increased by 2.0
        assertProductAvailability(testProduct, new BigDecimal("7.0"), new BigDecimal("7.0"));
        // check the InventoryItem entity
        List<GenericValue> inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.<String, Object>toMap("productId", productId, "statusId", "INV_RETURNED", "quantityOnHandTotal", 2, "availableToPromiseTotal", 2));
        assertEquals("InventoryItem INV_RETURNED for product [" + productId + "]", 1, inventoryItems.size());

        // 9c. Verify that a payment of the type CUSTOMER_REFUND from Company to DemoCustomer for the amount of the return in the status of PMNT_SENT has been created
        // The amount should be 2x 100 + 5.76% tax = 211.7
        assertRefundExists(orderId, "Company", "DemoCustomer", "211.70");

        // confirm that no extra filler payment was created
        long final_payments = countPayments("Company", "DemoCustomer", "CUSTOMER_REFUND", "PMNT_NOT_PAID");
        assertEquals("A new Payment from Company to DemoCustomer as CUSTOMER_REFUND / PMNT_NOT_PAID was found, it should not have been.", initial_payments, final_payments);

        // 10. Try creating a second return for 5.0 items from the order in step (3),
        // 11. Verify that the system will not allow creating a return for this quantity (because we have already returned 2, so only 3 are left)
        callCtx = new HashMap();
        callCtx.put("userLogin", User);
        callCtx.put("orderId", orderId);
        callCtx.put("returnReasonId", "RTN_NOT_WANT");
        callCtx.put("returnTypeId", "RTN_REFUND");
        callCtx.put("returnItemTypeId", "RET_PROD_ITEM");
        callCtx.put("returnId", returnId);
        callCtx.put("orderItemSeqId", "00001");
        callCtx.put("productId", productId);
        callCtx.put("returnPrice", new BigDecimal("100.0"));
        callCtx.put("returnQuantity", new BigDecimal("5.0"));
        callCtx.put("description", testProduct.get("description"));

        Debug.logInfo("Expecting an exception", MODULE);
        runAndAssertServiceError("createReturnItem", callCtx);
    }

    /**
     * Test what happens when there is a failure of the refund payment service
     * Same steps as the basic returns test, except
     * 7. Set the order's Product Store's ProductStorePaymentSetting for paymentMethodType=CREDIT CARD and paymentServiceTypeEnumId=PRDS_PAY_REFUND to testCCRefundFailure
     * 9. Verify that:
     * 9a.  The return header's status is RETURN_RECEIVED
     * 9b.   Inventory of the item has increased by 2.0
     * 9c.   A payment of the type CUSTOMER_REFUND from Company to DemoCustomer for the amount of the return in the status of PMNT_NOT_PAID has been created
     * 9d.   The status on the return item is RETURN_MAN_REFUND
     * No need to try steps 10 and 11 from above, but
     * 10.  Verify that DemoSalesManager can manually updateReturnHeader status to RETURN_COMPLETE
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testRefundPaymentFailure() throws GeneralException {
        // 1. create test product
        GenericValue testProduct = createTestProduct("Product for Refund Payment Failure Return Test", demowarehouse1);
        String productId = testProduct.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(testProduct, new BigDecimal("200.0"), admin);

        // 2. Receive 10 units
        receiveInventoryProduct(testProduct, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("199.0"), demowarehouse1);

        // 3. sales order of 5 units to DemoCustomer with payment method 9015
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(testProduct, new BigDecimal("5.0"));
        User = DemoCSR;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, productStoreId, "CREDIT_CARD", "9015", null);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testRefundPaymentFailure created order [" + orderId + "]", MODULE);

        // authorize payment for the order
        Map results = runAndAssertServiceSuccess("authOrderPayments", UtilMisc.toMap("orderId", orderId, "userLogin", admin));
        String authResult = (String) results.get("processResult");
        assertEquals("Auth result", "APPROVED", authResult);

        // capture payment for the order
        BigDecimal orderAmount = UtilOrder.getOrderOpenAmount(delegator, orderId);
        results = runAndAssertServiceSuccess("captureOrderPayments", UtilMisc.<String, Object>toMap("orderId", orderId, "captureAmount", orderAmount, "userLogin", admin));
        String captureResult = (String) results.get("processResult");
        assertEquals("Capture result", "COMPLETE", captureResult);

        // 4. ship the order
        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", orderId, "facilityId", facilityId, "userLogin", demowarehouse1));

        // 5. verify ATP / QOH is 5.0
        assertProductAvailability(testProduct, new BigDecimal("5.0"), new BigDecimal("5.0"));

        // 6. create a return for 2.0 items
        results = runAndAssertServiceSuccess("crmsfa.createReturnFromOrder", UtilMisc.toMap("orderId", orderId, "userLogin", User));
        String returnId = (String) results.get("returnId");
        Debug.logInfo("testRefundPaymentFailure created return [" + returnId + "]", MODULE);

        Map callCtx = new HashMap();
        callCtx.put("userLogin", User);
        callCtx.put("orderId", orderId);
        callCtx.put("returnReasonId", "RTN_NOT_WANT");
        callCtx.put("returnTypeId", "RTN_REFUND");
        callCtx.put("returnItemTypeId", "RET_PROD_ITEM");
        callCtx.put("returnId", returnId);
        callCtx.put("orderItemSeqId", "00001");
        callCtx.put("productId", productId);
        callCtx.put("returnPrice", new BigDecimal("200.0"));
        callCtx.put("returnQuantity", new BigDecimal("2.0"));
        callCtx.put("description", testProduct.get("description"));

        runAndAssertServiceSuccess("createReturnItem", callCtx);

        // 7. Set the order's Product Store's ProductStorePaymentSetting for paymentMethodType=CREDIT CARD and paymentServiceTypeEnumId=PRDS_PAY_REFUND to testCCRefundFailure
        setProductStorePaymentService(productStoreId, "CREDIT_CARD", "PRDS_PAY_REFUND", "testCCRefundFailure");

        // for 9c. count number of Payments now
        // The amount should be 2x 200 + 5.76% tax = 423.40
        long initial_payments = countPayments("Company", "DemoCustomer", "CUSTOMER_REFUND", "PMNT_NOT_PAID", "423.40");

        // 8. Accept the return
        runAndAssertServiceSuccess("crmsfa.acceptReturn", UtilMisc.toMap("userLogin", admin, "returnId", returnId));

        // 9a. Verify that the return is RETURN_RECEIVED
        assertReturnStatusEquals(returnId, "RETURN_RECEIVED");

        // 9b. Inventory of the item has increased by 2.0
        assertProductAvailability(testProduct, new BigDecimal("7.0"), new BigDecimal("7.0"));
        // check the InventoryItem entity
        List<GenericValue> inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.<String, Object>toMap("productId", productId, "statusId", "INV_RETURNED", "quantityOnHandTotal", 2, "availableToPromiseTotal", 2));
        assertEquals("InventoryItem INV_RETURNED for product [" + productId + "]", 1, inventoryItems.size());

        // 9c. A payment of the type CUSTOMER_REFUND from Company to DemoCustomer for the amount of the return in the status of PMNT_NOT_PAID has been created
        // The amount should be 2x 200 + 5.76% tax = 423.00
        long final_payments = countPayments("Company", "DemoCustomer", "CUSTOMER_REFUND", "PMNT_NOT_PAID", "423.40");
        assertEquals("Should be one new Payment from Company to DemoCustomer as CUSTOMER_REFUND / PMNT_NOT_PAID and of amount 423.40", initial_payments + 1, final_payments);

        // 9d. The status on the return item is RETURN_MAN_REFUND
        assertReturnItemsStatusEquals(returnId, "RETURN_MAN_REFUND");

        // No need to try steps 10 and 11 from above, but
        // 10. Verify that DemoSalesManager can manually updateReturnHeader status to RETURN_COMPLETED
        results = runAndAssertServiceSuccess("updateReturnHeader", UtilMisc.toMap("userLogin", DemoSalesManager, "returnId", returnId, "statusId", "RETURN_COMPLETED"));
        assertReturnStatusEquals(returnId, "RETURN_COMPLETED");
    }

    /**
     * Test what happens when there is a manual payment is required
     * Same steps as the basic returns test, except
     * 3.  Create a sales order for 5 units to DemoCustomer with payment method type EXT_OFFLINE
     * 3b.  Manually approve the sales order.
     * 9. Verify that:
     * 9a.  The return header's status is RETURN_RECEIVED
     * 9b.   Inventory of the item has increased by 2.0
     * 9c.   A payment of the type CUSTOMER_REFUND from Company to DemoCustomer for the amount of the return in the status of PMNT_NOT_PAID has been created
     * 9d.   The status on the return item is RETURN_MAN_REFUND
     * No need to try steps 10 and 11 from above, but
     * 10.  Verify that DemoSalesManager can manually updateReturnHeader status to RETURN_COMPLETED
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testManualRefundPayment() throws GeneralException {
        // 1. create test product
        GenericValue testProduct = createTestProduct("Product for Manual Refund Payment Return Test", demowarehouse1);
        String productId = testProduct.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(testProduct, new BigDecimal("300.0"), admin);

        // 2. Receive 10 units
        receiveInventoryProduct(testProduct, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("299.0"), demowarehouse1);

        // 3. sales order of 5 units to DemoCustomer with payment type EXT_OFFLINE
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(testProduct, new BigDecimal("5.0"));
        User = DemoCSR;
        // 3b. note, this method approves the order
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, productStoreId, "EXT_OFFLINE", null);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testManualRefundPayment created order [" + orderId + "]", MODULE);

        // 4. ship the order
        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", orderId, "facilityId", facilityId, "userLogin", demowarehouse1));

        // 5. verify ATP / QOH is 5.0
        assertProductAvailability(testProduct, new BigDecimal("5.0"), new BigDecimal("5.0"));

        // 6. create a return for 2.0 items
        Map results = runAndAssertServiceSuccess("crmsfa.createReturnFromOrder", UtilMisc.toMap("orderId", orderId, "userLogin", User));
        String returnId = (String) results.get("returnId");
        Debug.logInfo("testManualRefundPayment created return [" + returnId + "]", MODULE);

        Map callCtx = new HashMap();
        callCtx.put("userLogin", User);
        callCtx.put("orderId", orderId);
        callCtx.put("returnReasonId", "RTN_NOT_WANT");
        callCtx.put("returnTypeId", "RTN_REFUND");
        callCtx.put("returnItemTypeId", "RET_PROD_ITEM");
        callCtx.put("returnId", returnId);
        callCtx.put("orderItemSeqId", "00001");
        callCtx.put("productId", productId);
        callCtx.put("returnPrice", new BigDecimal("300.0"));
        callCtx.put("returnQuantity", new BigDecimal("2.0"));
        callCtx.put("description", testProduct.get("description"));

        runAndAssertServiceSuccess("createReturnItem", callCtx);

        // for 9c. count number of Payments now
        // Amount is 2x300 + 5.76% (35.10) tax = 635.10
        long initial_payments = countPayments("Company", "DemoCustomer", "CUSTOMER_REFUND", "PMNT_NOT_PAID", "635.10");

        // 8. Accept the return
        runAndAssertServiceSuccess("crmsfa.acceptReturn", UtilMisc.toMap("userLogin", admin, "returnId", returnId));

        // 9a. Verify that the return is RETURN_RECEIVED
        assertReturnStatusEquals(returnId, "RETURN_RECEIVED");

        // 9b. Verify that the inventory of the item has increased by 2.0
        assertProductAvailability(testProduct, new BigDecimal("7.0"), new BigDecimal("7.0"));
        // check the InventoryItem entity
        List<GenericValue> inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.<String, Object>toMap("productId", productId, "statusId", "INV_RETURNED", "quantityOnHandTotal", 2, "availableToPromiseTotal", 2));
        assertEquals("InventoryItem INV_RETURNED for product [" + productId + "]", 1, inventoryItems.size());

        // 9c. Verify that a payment of the type CUSTOMER_REFUND from Company to DemoCustomer for the amount of the return in the status of PMNT_NOT_PAID has been created
        // Amount is 2x300 + 5.76% (35.10) tax = 635.10
        long final_payments = countPayments("Company", "DemoCustomer", "CUSTOMER_REFUND", "PMNT_NOT_PAID", "635.10");
        assertEquals("Should be one new Payment from Company to DemoCustomer as CUSTOMER_REFUND / PMNT_NOT_PAID and of amount 634.50", initial_payments + 1, final_payments);

        // 9d. The status on the return item is RETURN_MAN_REFUND
        assertReturnItemsStatusEquals(returnId, "RETURN_MAN_REFUND");

        // No need to try steps 10 and 11 from above, but
        // 10. Verify that DemoSalesManager can manually updateReturnHeader status to RETURN_COMPLETED
        results = runAndAssertServiceSuccess("updateReturnHeader", UtilMisc.toMap("userLogin", DemoSalesManager, "returnId", returnId, "statusId", "RETURN_COMPLETED"));
        assertReturnStatusEquals(returnId, "RETURN_COMPLETED");
    }

    /**
     * Test what happens when there is a manual payment is required
     * Same steps as the testManualRefundPayment, except
     * 3c. Receive payment from the customer
     * @throws GeneralException
     */
    /*
    public void testManualRefundReceivedPayment() throws GeneralException {
        // 1. create test product
        GenericValue testProduct = createTestProduct("Product for Manual Refund received Payment Return Test", demowarehouse1);
        String productId = testProduct.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(testProduct, 1000.0, admin);

        // 2. Receive 10 units
        receiveInventoryProduct(testProduct, 10.0, "NON_SERIAL_INV_ITEM", 999.0, demowarehouse1);

        // 3. sales order of 5 units to DemoCustomer with payment type EXT_OFFLINE
        Map<GenericValue, Double> order = new HashMap<GenericValue, Double>();
        order.put(testProduct, 5.0);
        User = DemoCSR;

        // 3b. note, this method approves the order
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, productStoreId, "EXT_OFFLINE", null);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testManualRefundReceivedPayment created order ["+orderId+"]", MODULE);

        // 3c. receiving payment
        List<GenericValue> opps = salesOrder.getOrderPaymentPreferences();
        assertNotEmpty("No Order Payment Preference found for order ["+orderId+"]", opps);
        String opp_id = opps.get(0).getString("orderPaymentPreferenceId");

        // - first set maxAmount in the opp, this is needed
        Map callCtx = new HashMap();
        callCtx.put("userLogin", User);
        callCtx.put("orderPaymentPreferenceId", opp_id);
        callCtx.put("maxAmount", salesOrder.getGrandTotal());
        Map results = runAndAssertServiceSuccess("updateOrderPaymentPreference", callCtx);
        opp_id = (String)results.get("orderPaymentPreferenceId");

        // - create a payment
        callCtx = new HashMap();
        callCtx.put("userLogin", User);
        callCtx.put("orderPaymentPreferenceId", opp_id);
        callCtx.put("comments", "testManualRefundReceivedPayment");
        results = runAndAssertServiceSuccess("createPaymentFromPreference", callCtx);

        // 4. ship the order
        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", orderId, "facilityId", facilityId, "userLogin", demowarehouse1));

        // 5. verify ATP / QOH is 5.0
        assertProductAvailability(testProduct, 5.0, 5.0);

        // 6. create a return for 2.0 items
        results = runAndAssertServiceSuccess("crmsfa.createReturnFromOrder", UtilMisc.toMap("orderId", orderId, "userLogin", User));
        String returnId = (String)results.get("returnId");
        Debug.logInfo("testManualRefundReceivedPayment created return ["+returnId+"]", MODULE);

        callCtx = new HashMap();
        callCtx.put("userLogin", User);
        callCtx.put("orderId", orderId);
        callCtx.put("returnReasonId", "RTN_NOT_WANT");
        callCtx.put("returnTypeId", "RTN_REFUND");
        callCtx.put("returnItemTypeId", "RET_PROD_ITEM");
        callCtx.put("returnId", returnId);
        callCtx.put("orderItemSeqId", "00001");
        callCtx.put("productId", productId);
        callCtx.put("returnPrice", 1000.0);
        callCtx.put("returnQuantity", 2.0);
        callCtx.put("description", testProduct.get("description"));

        runAndAssertServiceSuccess("createReturnItem", callCtx);

        // for 9c. count number of Payments now
        long initial_payments = countPayments("Company", "DemoCustomer", "CUSTOMER_REFUND", "PMNT_NOT_PAID", "2115.00");

        // 8. Accept the return
        runAndAssertServiceSuccess("crmsfa.acceptReturn", UtilMisc.toMap("userLogin", admin, "returnId", returnId));

        // 9a. Verify that the return is RETURN_RECEIVED
        assertReturnStatusEquals(returnId, "RETURN_RECEIVED");

        // 9b. Verify that the inventory of the item has increased by 2.0
        assertProductAvailability(testProduct, 7.0, 7.0);
        // check the InventoryItem entity
        List<GenericValue> inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "statusId", "INV_RETURNED", "quantityOnHandTotal", 2, "availableToPromiseTotal", 2));
        assertEquals("InventoryItem INV_RETURNED for product ["+productId+"]", 1, inventoryItems.size());

        // 9c. Verify that a payment of the type CUSTOMER_REFUND from Company to DemoCustomer for the amount of the return in the status of PMNT_NOT_PAID has been created
        // Amount is 2x100 + 11.50 tax = 211.50
        long final_payments = countPayments("Company", "DemoCustomer", "CUSTOMER_REFUND", "PMNT_NOT_PAID", "2115.00");
        assertEquals("Should be one new Payment from Company to DemoCustomer as CUSTOMER_REFUND / PMNT_NOT_PAID and of amount 2115.00", initial_payments+1, final_payments);

        // 9d. The status on the return item is RETURN_MAN_REFUND
        assertReturnItemsStatusEquals(returnId, "RETURN_MAN_REFUND");

        // No need to try steps 10 and 11 from above, but
        // 10. Verify that DemoSalesManager can manually updateReturnHeader status to RETURN_COMPLETED
        results = runAndAssertServiceSuccess("updateReturnHeader", UtilMisc.toMap("userLogin", DemoSalesManager, "returnId", returnId, "statusId", "RETURN_COMPLETED"));
        assertReturnStatusEquals(returnId, "RETURN_COMPLETED");
    }
    */
}


// TODO: Also verify inventory values, ledger balances, and acctg trans entries as a result of the returns later
