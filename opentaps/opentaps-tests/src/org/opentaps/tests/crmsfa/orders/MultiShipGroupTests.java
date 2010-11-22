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
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.infrastructure.Infrastructure;

/**
 * Tests on multiple ship groups.
 */
public class MultiShipGroupTests extends OrderTestCase {

    private static final String MODULE = MultiShipGroupTests.class.getName();

    GenericValue DemoSalesManager;
    GenericValue demowarehouse1;
    GenericValue admin;
    GenericValue DemoAccount1;
    String productStoreId = "9000";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        DemoAccount1 = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoAccount1"));
        DemoSalesManager = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        demowarehouse1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        admin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Comprehensive test of two ship groups from creation to end.  Tests the following things:
     * <ul>
     *   <li>Creation of two ship groups with different addresses is possible</li>
     *   <li>Packing each shipment produces invoice, packing slip and shipment with the correct shipping address.</li>
     *   <li>Two payments of 90% and 10% of order value is split correctly between the two ship group invoices.</li>
     *   <li>Both invoices are fully paid</li>
     *   <li>Domain test:  invoice.getShipmentAddress() is working as expected</li>
     * </ul>
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testTwoShipGroupLifeCycle() throws Exception {
        // create an order with offline payment
        Map<GenericValue, BigDecimal> orderItems = new HashMap<GenericValue, BigDecimal>();
        GenericValue testProduct = createTestProduct("testTwoShipGroupLifeCycle test product", demowarehouse1);
        assignDefaultPrice(testProduct, new BigDecimal("5.00"), admin);
        orderItems.put(testProduct, new BigDecimal("10.0"));
        User = DemoSalesManager;
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, DemoAccount1, productStoreId);
        String orderId = orderFactory.getOrderId();

        Debug.logInfo("testTwoShipGroupLifeCycle created order [" + orderId + "]", MODULE);

        // split the order in half to DemoAddress2 (note that orders are created by default to DemoAddress1)
        Map input = UtilMisc.toMap("userLogin", DemoSalesManager);
        input.put("orderId", orderId);
        input.put("maySplit", "N");
        input.put("isGift", "N");
        input.put("shippingMethod", "STANDARD@_NA_");
        input.put("contactMechId", SECONDARY_SHIPPING_ADDRESS);
        input.put("_rowSubmit", UtilMisc.toMap("0", "Y"));
        input.put("orderIds", UtilMisc.toMap("0", orderId));
        input.put("qtiesToTransfer", UtilMisc.toMap("0", "5.0"));
        input.put("shipGroupSeqIds", UtilMisc.toMap("0", shipGroupSeqId));
        input.put("orderItemSeqIds", UtilMisc.toMap("0", "00001"));
        runAndAssertServiceSuccess("crmsfa.createShipGroup", input);

        // ensure the groups have the right addresses and quantities
        assertShipGroupAssocsQuantities(orderId, UtilMisc.toList("00001", "00002"), UtilMisc.toList(new BigDecimal("5.0"), new BigDecimal("5.0")));
        assertShipGroupValidWithAddress(orderId, "00001", DEFAULT_SHIPPING_ADDRESS);
        assertShipGroupValidWithAddress(orderId, "00002", SECONDARY_SHIPPING_ADDRESS);

        // create two payments (90/10 split) for full value of order

        // Get the order total (note that after the split the taxes may have changed)
        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(orderId);
        BigDecimal total = order.getGrandTotal();
        total = total.setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal firstPayment = total.multiply(new BigDecimal("0.9")).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal secondPayment = total.subtract(firstPayment);
        Debug.logInfo("testTwoShipGroupLifeCycle firstPayment [" + firstPayment + "], secondPayment [" + secondPayment + "]", MODULE);

        // note that the Payment.paymentPreferenceId must be set in order for packing to automatically apply it to the invoices generated
        // we set it by hand here (normally this field is handled by OrderManagerEvents.receiveOfflinePayment())
        GenericValue paymentPref = EntityUtil.getFirst(orderFactory.getOrderPaymentPreferences());
        assertNotNull(paymentPref);
        String paymentPreferenceId = paymentPref.getString("orderPaymentPreferenceId");
        assertNotNull(paymentPreferenceId);

        // create the payments as received
        Map input1 = new FastMap();
        input1.put("paymentTypeId", "CUSTOMER_PAYMENT");
        input1.put("paymentMethodTypeId", "CASH");
        input1.put("paymentPreferenceId", paymentPreferenceId);
        input1.put("partyIdFrom", DemoAccount1.get("partyId"));
        input1.put("partyIdTo", organizationPartyId);
        input1.put("statusId", "PMNT_RECEIVED");
        input1.put("amount", firstPayment);
        input1.put("userLogin", DemoSalesManager);
        Map input2 = new FastMap(input1);
        input2.put("amount", secondPayment);
        Map result1 = runAndAssertServiceSuccess("createPayment", input1);
        Map result2 = runAndAssertServiceSuccess("createPayment", input2);
        String paymentId1 = (String) result1.get("paymentId");
        String paymentId2 = (String) result2.get("paymentId");
        assertNotNull(paymentId1);
        assertNotNull(paymentId2);

        Debug.logInfo("testTwoShipGroupLifeCycle created payments [" + paymentId1 + "] and [" + paymentId2 + "]", MODULE);

        // receive the product to ensure enough available
        receiveInventoryProduct(testProduct, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("3.55"), demowarehouse1);

        // ship the whole thing, which has the same effect as packing each ship group separately
        Map results = runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", orderId, "facilityId", facilityId, "userLogin", demowarehouse1));
        List<String> shipmentIds = (List<String>) results.get("shipmentIds");
        assertNotNull(shipmentIds);
        assertEquals("Order [" + order.getOrderId() + "] should have been packed in 2 shipments", 2, shipmentIds.size());

        // find the shipments with matching primary order and ship group
        GenericValue shipment1 = EntityUtil.getFirst(delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", orderId, "primaryShipGroupSeqId", "00001")));
        GenericValue shipment2 = EntityUtil.getFirst(delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", orderId, "primaryShipGroupSeqId", "00002")));
        assertNotNull("Shipment for ship group 1 exists", shipment1);
        assertNotNull("Shipment for ship group 2 exists", shipment2);
        assertTrue(shipmentIds.contains(shipment1.getString("shipmentId")));
        assertTrue(shipmentIds.contains(shipment2.getString("shipmentId")));

        Debug.logInfo("testTwoShipGroupLifeCycle created shipments [" + shipment1.getString("shipmentId")  + "] and [" + shipment2.getString("shipmentId") + "]", MODULE);

        // ensure the shipment destination addresses are correct
        assertEquals("Shipment for ship group 1 has destination address DemoAddress1", shipment1.getString("destinationContactMechId"), DEFAULT_SHIPPING_ADDRESS);
        assertEquals("Shipment for ship group 2 has destination address DemoAddress2", shipment2.getString("destinationContactMechId"), SECONDARY_SHIPPING_ADDRESS);

        // get the invoices for these shipments
        List<GenericValue> billings1 = shipment1.getRelated("ShipmentItemBilling");
        List<GenericValue> billings2 = shipment2.getRelated("ShipmentItemBilling");
        assertEquals("Only one invoice should exist for shipment 1", 1, EntityUtil.getFieldListFromEntityList(billings1, "invoiceId", true).size());
        assertEquals("Only one invoice should exist for shipment 2", 1, EntityUtil.getFieldListFromEntityList(billings2, "invoiceId", true).size());
        GenericValue invoice1 = billings1.get(0);
        GenericValue invoice2 = billings2.get(0);

        // load the invoice domain  TODO User conflicts with domain class, also try to use domain as much as possible for all checks
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new org.opentaps.foundation.infrastructure.User(admin));
        DomainsDirectory dir = dl.loadDomainsDirectory();
        BillingDomainInterface billingDomain = dir.getBillingDomain();
        InvoiceRepositoryInterface invoiceRepository = billingDomain.getInvoiceRepository();
        Invoice invoiceObj1 = invoiceRepository.getInvoiceById(invoice1.getString("invoiceId"));
        Invoice invoiceObj2 = invoiceRepository.getInvoiceById(invoice2.getString("invoiceId"));
        assertNotNull(invoiceObj1);
        assertNotNull(invoiceObj2);

        // verify the invoices are paid
        assertEquals("Invoice 1 is paid", "INVOICE_PAID", invoiceObj1.getStatusId());
        assertEquals("Invoice 2 is paid", "INVOICE_PAID", invoiceObj2.getStatusId());

        // verify that the invoice shipping addresses (and by extension the InvoiceContactMechs) are correct
        PostalAddress address1 = invoiceObj1.getShippingAddress();
        PostalAddress address2 = invoiceObj2.getShippingAddress();
        assertNotNull(address1);
        assertNotNull(address2);
        assertEquals("Invoice for ship group 1 has shipping address DemoAddress1", address1.getContactMechId(), DEFAULT_SHIPPING_ADDRESS);
        assertEquals("Invoice for ship group 2 has shipping address DemoAddress2", address2.getContactMechId(), SECONDARY_SHIPPING_ADDRESS);

        // the order should be completed
        assertOrderCompleted(orderId);

        // verify that the invoices total match the order total
        List<Invoice> invoices = order.getInvoices();
        assertEquals("Should have exactly 2 invoices for the order [" + order.getOrderId() + "]", 2, invoices.size());
        BigDecimal invoiceTotal = BigDecimal.ZERO;
        for (Invoice i : invoices) {
            invoiceTotal = invoiceTotal.add(i.getInvoiceTotal());
        }
        Debug.logInfo("Order [" + order.getOrderId() + "] total = " + order.getTotal(), MODULE);
        assertEquals("Invoices [" + Entity.getDistinctFieldValues(String.class, invoices, Invoice.Fields.invoiceId) + "] total and order [" + order.getOrderId() + "] total did not match", order.getTotal(), invoiceTotal);

    }

}
