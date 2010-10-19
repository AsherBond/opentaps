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
import java.util.List;
import java.util.Map;

import org.ofbiz.accounting.invoice.InvoiceWorker;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.service.GenericServiceException;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.tests.warehouse.InventoryAsserts;

public class DropShipTests extends OrderTestCase {

    private static final String MODULE = OrderTests.class.getName();

    private GenericValue demoCustomer;
    private GenericValue demoCSR;
    private GenericValue demowarehouse1;
    private GenericValue demopurch1;
    private GenericValue dropShip1;
    private GenericValue dropShip2;
    private GenericValue postalAddress;
    private GenericValue paymentMethod;
    private GenericValue productStore;
    private static final String facilityId = "WebStoreWarehouse";
    private static final String organizationPartyId = "Company";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        demoCustomer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoCustomer"));
        assertNotNull("DemoCustomer not null", demoCustomer);
        dropShip1 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "dropShip1"));
        assertNotNull("Product dropShip1 not null", dropShip1);
        dropShip2 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "dropShip2"));
        assertNotNull("Product dropShip2 not null", dropShip2);
        demoCSR = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "DemoCSR"));
        assertNotNull("DemoCSR not null", demoCSR);
        demowarehouse1 = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        assertNotNull("demowarehouse1 not null", demowarehouse1);
        demopurch1 = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "demopurch1"));
        assertNotNull("demopurch1 not null", demopurch1);
        postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", "9015"));
        assertNotNull("PostalAddress 9015 not null", postalAddress);
        paymentMethod = delegator.findByPrimaryKey("CreditCard", UtilMisc.toMap("paymentMethodId", "9015"));
        assertNotNull("CreditCard 9015 not null", paymentMethod);
        productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", "9000"));
        assertNotNull("ProductStore 9000 not null", productStore);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        demoCustomer = null;
        dropShip1 = null;
        dropShip2 = null;
        demoCSR = null;
        demowarehouse1 = null;
        demopurch1 = null;
        postalAddress = null;
        paymentMethod = null;
        productStore = null;
    }

    /**
     * Verify drop shipping is handled correctly: purchase orders are created, invoices and payments are created, and no inventory changes hands.
     * @exception GeneralException if an error occurs
     */
    public void testDropShipOrdering() throws GeneralException {

        InventoryAsserts invAss = new InventoryAsserts(this, facilityId, organizationPartyId, User);
        Map<String, Object> initialDropShip1Inventory = invAss.getInventory(dropShip1.getString("productId"));
        Map<String, Object> initialDropShip2Inventory = invAss.getInventory(dropShip2.getString("productId"));

        // Create a sales order for 1 dropShip1 and 2 dropShip2 for demoCustomer using credit card paymentMethodId 9015
        SalesOrderFactory sof = null;
        try {
            sof = new SalesOrderFactory(delegator, dispatcher, demoCSR, organizationPartyId, demoCustomer.getString("partyId"), productStore.getString("productStoreId"));
        } catch (
            GenericEntityException e) {
            assertTrue("GenericEntityException:" + e.toString(), false);
        }

        String currencyUomId = productStore.getString("defaultCurrencyUomId");

        // get the drop ship supplier for each of the products
        String dropShip1SupplierPartyId = null;
        Map<String, Object> getSuppliersForProductResult = runAndAssertServiceSuccess("getSuppliersForProduct", UtilMisc.<String, Object>toMap("productId", dropShip1.getString("productId"), "quantity", new BigDecimal("1.0"), "canDropShip", "Y", "currencyUomId", currencyUomId));
        List<GenericValue> supplierProducts = (List<GenericValue>) getSuppliersForProductResult.get("supplierProducts");
        if (UtilValidate.isNotEmpty(supplierProducts)) {
            dropShip1SupplierPartyId = EntityUtil.getFirst(supplierProducts).getString("partyId");
        }
        assertNotNull("No supplier found for product dropShip1", dropShip1SupplierPartyId);

        String dropShip2SupplierPartyId = null;
        getSuppliersForProductResult = runAndAssertServiceSuccess("getSuppliersForProduct", UtilMisc.<String, Object>toMap("productId", dropShip2.getString("productId"), "quantity", new BigDecimal("2.0"), "canDropShip", "Y", "currencyUomId", currencyUomId));
        supplierProducts = (List<GenericValue>) getSuppliersForProductResult.get("supplierProducts");
        if (UtilValidate.isNotEmpty(supplierProducts)) {
            dropShip2SupplierPartyId = EntityUtil.getFirst(supplierProducts).getString("partyId");
        }
        assertNotNull("No supplier found for product dropShip2", dropShip2SupplierPartyId);

        sof.addPaymentMethod("CREDIT_CARD", paymentMethod.getString("paymentMethodId"));

        // set the shipping method and the drop shipping supplier to the two ship groups
        sof.addShippingGroup("UPS", "NEXT_DAY", postalAddress.getString("contactMechId"), dropShip1SupplierPartyId);
        sof.addShippingGroup("UPS", "NEXT_DAY", postalAddress.getString("contactMechId"), dropShip2SupplierPartyId);

        try {
            sof.addProduct(dropShip1, new BigDecimal("1.0"), "00001");
            sof.addProduct(dropShip2, new BigDecimal("2.0"), "00002");
        } catch (GenericServiceException e) {
            fail("GenericServiceException:" + e.toString());
        }

        // Create and approve the order
        String orderId = null;
        try {
            orderId = sof.storeOrder();
            sof.approveOrder();
            sof.processPayments();
        } catch (GenericServiceException e) {
            fail("GenericServiceException:" + e.toString());
        }
        Debug.logInfo("testDropShipOrdering created sales order ID " + orderId, MODULE);

        // Verify that dropShip1 and dropShip2 are each linked to a different purchase order item
        GenericValue orderItem1Assoc = EntityUtil.getFirst(delegator.findByAnd("OrderItemAssoc", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "00001", "orderItemAssocTypeId", "DROP_SHIPMENT")));
        GenericValue orderItem2Assoc = EntityUtil.getFirst(delegator.findByAnd("OrderItemAssoc", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "00002", "orderItemAssocTypeId", "DROP_SHIPMENT")));
        assertNotNull("dropShip1 orderItem (orderItemSeqId 00001) for order ID " + orderId + " is not linked to a purchase order item", orderItem1Assoc);
        assertNotNull("dropShip2 orderItem (orderItemSeqId 00002) for order ID " + orderId + " is not linked to a purchase order item", orderItem2Assoc);
        String dropShip1PurchaseOrderId = orderItem1Assoc.getString("toOrderId");
        String dropShip2PurchaseOrderId = orderItem2Assoc.getString("toOrderId");
        if (!dropShip1SupplierPartyId.equalsIgnoreCase(dropShip2SupplierPartyId)) {
            assertNotSame("dropShip1 orderItem (orderItemSeqId 00001) for order ID " + orderId + " is linked to the same purchase order (" + dropShip1PurchaseOrderId + ") as dropShip2 orderItem (00002)", dropShip1PurchaseOrderId, dropShip2PurchaseOrderId);
        }

        // Approve and call service quickDropShipOrder on the purchase orders linked to dropShip1
        // Note that shipGroupSeqId is the shipGroupSeqId of the purchase order, not the sales order, so it should be 00001 in both cases
        runAndAssertServiceSuccess("changeOrderItemStatus", UtilMisc.toMap("orderId", dropShip1PurchaseOrderId, "statusId", "ITEM_APPROVED", "userLogin", demoCSR));
        runAndAssertServiceSuccess("quickDropShipOrder", UtilMisc.toMap("orderId", dropShip1PurchaseOrderId, "shipGroupSeqId", "00001", "userLogin", demowarehouse1));

        // Sleep to get the service captureOrderPayments in PaymentGatewayServices and
        // the service processCaptureSplitPayment fired by captureOrderPayments finished
        try {
            Thread.sleep(1000 * 60 * 3);
        } catch (InterruptedException e) {
            fail("InterruptedException: " + e.toString());
        }
        // Approve and call service quickDropShipOrder on the purchase orders linked to dropShip2
        runAndAssertServiceSuccess("changeOrderItemStatus", UtilMisc.toMap("orderId", dropShip2PurchaseOrderId, "statusId", "ITEM_APPROVED", "userLogin", demoCSR));
        runAndAssertServiceSuccess("quickDropShipOrder", UtilMisc.toMap("orderId", dropShip2PurchaseOrderId, "shipGroupSeqId", "00001", "userLogin", demowarehouse1));

        // Verify that the sales order and purchase orders are now completed
        GenericValue salesOrder = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        assertNotNull("Can't find sales order " + orderId, salesOrder);
        assertEquals("Sales order " + orderId + " status is not ORDER_COMPLETED", "ORDER_COMPLETED", salesOrder.getString("statusId"));
        GenericValue dropShip1PurchaseOrder = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", dropShip1PurchaseOrderId));
        assertNotNull("Can't find purchase order " + dropShip1PurchaseOrderId, dropShip1PurchaseOrder);
        assertEquals("Purchase order " + dropShip1PurchaseOrderId + " status is not ORDER_COMPLETED", "ORDER_COMPLETED", dropShip1PurchaseOrder.getString("statusId"));
        GenericValue dropShip2PurchaseOrder = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", dropShip2PurchaseOrderId));
        assertNotNull("Can't find purchase order " + dropShip2PurchaseOrderId, dropShip2PurchaseOrder);
        assertEquals("Purchase order " + dropShip2PurchaseOrderId + " status is not ORDER_COMPLETED", "ORDER_COMPLETED", dropShip2PurchaseOrder.getString("statusId"));

        // Verify that no new InventoryItem (since the beginning of this unit test) have been created for dropShip1 and dropShip2
        invAss.assertInventoryChange(dropShip1.getString("productId"), BigDecimal.ZERO, initialDropShip1Inventory);
        invAss.assertInventoryChange(dropShip2.getString("productId"), BigDecimal.ZERO, initialDropShip2Inventory);

        // Verify that a sales invoice and received customer payment for dropShip1 is created, and this sales invoice is paid
        // Verify that a sales invoice and received customer payment for dropShip2 is created, and this sales invoice is paid

        List<GenericValue> dropShip1OrderItemBillingList = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "00001"));
        assertEquals("There is only one invoice corresponding to orderId " + orderId, 1, dropShip1OrderItemBillingList.size());
        GenericValue dropShip1OrderItemBilling = EntityUtil.getFirst(dropShip1OrderItemBillingList);
        List<GenericValue> salesInvoice1List = dropShip1OrderItemBilling.getRelated("Invoice");
        assertEquals("There is only one invoice corresponding to orderId " + orderId, 1, salesInvoice1List.size());
        GenericValue salesInvoice1 = EntityUtil.getFirst(salesInvoice1List);

        assertEquals("Invoice should be a SALES_INVOICE", "SALES_INVOICE", salesInvoice1.getString("invoiceTypeId"));
        assertEquals("Invoice partyIdFrom should be Company", "Company", salesInvoice1.getString("partyIdFrom"));
        assertEquals("Invoice partyId should be DemoCustomer", "DemoCustomer", salesInvoice1.getString("partyId"));
        assertEquals("Invoice statusId should be INVOICE_PAID", "INVOICE_PAID", salesInvoice1.getString("statusId"));

        List<GenericValue> paymentApplication1List = delegator.findByAnd("PaymentApplication", UtilMisc.toMap("invoiceId", salesInvoice1.getString("invoiceId")));
        assertEquals("There is only one payment corresponding to invoiceId " + salesInvoice1.getString("invoiceId"), 1, paymentApplication1List.size());
        GenericValue paymentApplication1 = EntityUtil.getFirst(paymentApplication1List);
        List<GenericValue> payment1List = paymentApplication1.getRelated("Payment");
        assertEquals("There is only one payment corresponding to invoiceId " + salesInvoice1.getString("invoiceId"), 1, payment1List.size());
        GenericValue payment1 = EntityUtil.getFirst(payment1List);

        assertEquals("Payment statusId should be PMNT_RECEIVED", "PMNT_RECEIVED", payment1.getString("statusId"));
        assertEquals("Payment partyIdFrom should be DemoCustomer", "DemoCustomer", payment1.getString("partyIdFrom"));
        assertEquals("Payment partyIdTo should be Company", "Company", payment1.getString("partyIdTo"));

        List<GenericValue> dropShip2OrderItemBillingList = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "00002"));
        assertEquals("There is only one invoice corresponding to orderId " + orderId, 1, dropShip2OrderItemBillingList.size());
        GenericValue dropShip2OrderItemBilling = EntityUtil.getFirst(dropShip2OrderItemBillingList);
        List<GenericValue> salesInvoice2List = dropShip2OrderItemBilling.getRelated("Invoice");
        assertEquals("There is only one invoice corresponding to orderId " + orderId, 1, salesInvoice2List.size());
        GenericValue salesInvoice2 = EntityUtil.getFirst(salesInvoice2List);

        assertEquals("Invoice should be a SALES_INVOICE", "SALES_INVOICE", salesInvoice2.getString("invoiceTypeId"));
        assertEquals("Invoice partyIdFrom should be Company", "Company", salesInvoice2.getString("partyIdFrom"));
        assertEquals("Invoice partyId should be DemoCustomer", "DemoCustomer", salesInvoice2.getString("partyId"));
        assertEquals("Invoice statusId should be INVOICE_PAID", "INVOICE_PAID", salesInvoice2.getString("statusId"));

        List<GenericValue> paymentApplication2List = delegator.findByAnd("PaymentApplication", UtilMisc.toMap("invoiceId", salesInvoice2.getString("invoiceId")));
        assertEquals("There is only one payment corresponding to invoiceId " + salesInvoice2.getString("invoiceId"), 1, paymentApplication2List.size());
        GenericValue paymentApplication2 = EntityUtil.getFirst(paymentApplication2List);
        List<GenericValue> payment2List = paymentApplication2.getRelated("Payment");
        assertEquals("There is only one payment corresponding to invoiceId " + salesInvoice2.getString("invoiceId"), 1, payment2List.size());
        GenericValue payment2 = EntityUtil.getFirst(payment2List);

        assertEquals("Payment statusId should be PMNT_RECEIVED", "PMNT_RECEIVED", payment2.getString("statusId"));
        assertEquals("Payment partyIdFrom should be DemoCustomer", "DemoCustomer", payment2.getString("partyIdFrom"));
        assertEquals("Payment partyIdTo should be Company", "Company", payment2.getString("partyIdTo"));

        assertEquals("Payment amount should be grand total of the sales order", sof.getGrandTotal(), payment1.getDouble("amount") + payment2.getDouble("amount"));

        // Verify that a purchase invoice for dropShip1 from DemoSupplier is created in the "In Process" state
        List<GenericValue> dropShip1PurchaseOrderItemBillingList = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("orderId", dropShip1PurchaseOrderId, "orderItemSeqId", "00001"));
        assertEquals("There is only one invoice corresponding to orderId " + dropShip1PurchaseOrderId, 1, dropShip1PurchaseOrderItemBillingList.size());
        GenericValue dropShip1PurchaseOrderItemBilling = EntityUtil.getFirst(dropShip1PurchaseOrderItemBillingList);
        List<GenericValue> dropShipInvoice1List = dropShip1PurchaseOrderItemBilling.getRelated("Invoice");
        assertEquals("There is only one invoice corresponding to orderId " + dropShip1PurchaseOrderId, 1, dropShipInvoice1List.size());
        GenericValue dropShipInvoice1 = EntityUtil.getFirst(dropShipInvoice1List);

        assertEquals("Invoice partyIdFrom should be DemoSupplier", "DemoSupplier", dropShipInvoice1.getString("partyIdFrom"));
        assertEquals("Invoice partyId should be Company", "Company", dropShipInvoice1.getString("partyId"));
        assertEquals("Invoice statusId should be INVOICE_IN_PROCESS", "INVOICE_IN_PROCESS", dropShipInvoice1.getString("statusId"));

        // Verify that a purchase invoice for dropShip2 from BigSupplier is created in the "In Process" state
        List<GenericValue> dropShip2PurchaseOrderItemBillingList = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("orderId", dropShip2PurchaseOrderId, "orderItemSeqId", "00001"));
        assertEquals("There is only one invoice corresponding to orderId " + dropShip2PurchaseOrderId, 1, dropShip2PurchaseOrderItemBillingList.size());
        GenericValue dropShip2PurchaseOrderItemBilling = EntityUtil.getFirst(dropShip2PurchaseOrderItemBillingList);
        List<GenericValue> dropShipInvoice2List = dropShip2PurchaseOrderItemBilling.getRelated("Invoice");
        assertEquals("There is only one invoice corresponding to orderId " + dropShip2PurchaseOrderId, 1, dropShipInvoice2List.size());
        GenericValue dropShipInvoice2 = EntityUtil.getFirst(dropShipInvoice2List);

        assertEquals("Invoice partyIdFrom should be BigSupplier", "BigSupplier", dropShipInvoice2.getString("partyIdFrom"));
        assertEquals("Invoice partyId should be Company", "Company", dropShipInvoice2.getString("partyId"));
        assertEquals("Invoice statusId should be INVOICE_IN_PROCESS", "INVOICE_IN_PROCESS", dropShipInvoice2.getString("statusId"));

        // Verify that the total of salesInvoice1 + salesInvoice2 == grand total of the sales order (use InvoiceWorker.getInvoiceTotalBd for invoice totals)
        double invoiceTotal = InvoiceWorker.getInvoiceTotal(delegator, salesInvoice1.getString("invoiceId")).doubleValue() + InvoiceWorker.getInvoiceTotal(delegator, salesInvoice2.getString("invoiceId")).doubleValue();
        assertEquals("salesInvoice1 + salesInvoice2 == grand total of the sales order", sof.getGrandTotal(), invoiceTotal);

        // Verify that the total of dropShipInvoice1 + dropShipInvoice2 == (grand total of dropShip1PurchaseOrderId) + (grand total of dropShip2PurchaseOrderId)
        double purchaseInvoiceTotal = InvoiceWorker.getInvoiceTotal(delegator, dropShipInvoice1.getString("invoiceId")).doubleValue() + InvoiceWorker.getInvoiceTotal(delegator, dropShipInvoice2.getString("invoiceId")).doubleValue();

        OrderReadHelper orh1 = new OrderReadHelper(dropShip1PurchaseOrder);
        OrderReadHelper orh2 = new OrderReadHelper(dropShip2PurchaseOrder);
        double purchaseOrderTotal = orh1.getOrderGrandTotal().doubleValue() + orh2.getOrderGrandTotal().doubleValue();
        assertEquals("total of dropShipInvoice1 + dropShipInvoice2 == (grand total of dropShip1PurchaseOrderId) + (grand total of dropShip2PurchaseOrderId)", purchaseOrderTotal, purchaseInvoiceTotal);

    }
}
