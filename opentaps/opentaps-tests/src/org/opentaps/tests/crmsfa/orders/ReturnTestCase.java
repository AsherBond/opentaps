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
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

/**
 * Test for Returns.
 */
public class ReturnTestCase extends OrderTestCase {

    private static final String MODULE = ReturnTestCase.class.getName();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Assert a Status for the given return.
     * @param returnId the return ID
     * @param statusId the status ID to check
     */
    public void assertReturnStatusEquals(String returnId, String statusId) {
        try {
            GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            assertEquals("Return status " + statusId, statusId, (String) returnHeader.get("statusId"));
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException: " + e.toString());
        }
    }

    /**
     * Assert that the order payment was refunded.
     * - the OrderPaymentPreference status is PAYMENT_REFUNDED
     * - the Payment status is PMNT_SENT
     * - a Payment of type CUSTOMER_REFUND and the given status exists for it
     *
     * @param orderId the order that should be refunded
     * @param fromParty for the Payment
     * @param toParty for the Payment
     * @param amount amount of the refund payment
     */
    @SuppressWarnings("unchecked")
    public void assertRefundExists(String orderId, String fromParty, String toParty, String amount) {
        try {
            List<GenericValue> payments = delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_REFUNDED"));
            assertTrue("Should be at least one OrderPaymentPreference with status 'PAYMENT_REFUNDED' for order [" + orderId + "]", payments.size() > 0);
            String paymentPreferenceId = (String) payments.get(0).get("orderPaymentPreferenceId");
            Map input = UtilMisc.toMap("paymentPreferenceId", paymentPreferenceId,
                                       "partyIdFrom", fromParty,
                                       "partyIdTo", toParty,
                                       "paymentTypeId", "CUSTOMER_REFUND",
                                       "statusId", "PMNT_SENT");
            String amountMessage = "";
            if (amount != null) {
                input.put("amount", new BigDecimal(amount));
                amountMessage = " and amount = " + amount;
            }
            payments = delegator.findByAnd("Payment", input);

            assertTrue("Should be at least one 'CUSTOMER_REFUND' Payment from " + fromParty + " to " + toParty + " for OrderPaymentPreference [" + paymentPreferenceId + "] with status PMNT_SENT" + amountMessage, payments.size() > 0);
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException: " + e.toString());
        }
    }

    /**
     * Assert that the order payment was refunded.
     * - the OrderPaymentPreference status is PAYMENT_REFUNDED
     * - a Payment of type CUSTOMER_REFUND and the given status exists for it
     *
     * @param orderId the order that should be refunded
     * @param fromParty for the Payment
     * @param toParty for the Payment
     */
    public void assertRefundExists(String orderId, String fromParty, String toParty) {
        assertRefundExists(orderId, fromParty, toParty, null);
    }

    /**
     * Assert that a payment exists for a given Refund and associated
     * to an Invoice.
     * - also check that the Payment is of type CUSTOMER_REFUND
     *
     * @param returnId the order that should be refunded
     * @param fromParty for the Payment
     * @param toParty for the Payment
     * @param paymentStatus for the Payment
     * @param amount for the Payment
     */
    public void assertRefundPaymentExists(String returnId, String fromParty, String toParty, String paymentStatus, String amount) {
        try {
            // first find the invoices for the return by getting the ReturnItemBilling
            List<GenericValue> ribs = delegator.findByAnd("ReturnItemBilling", UtilMisc.toMap("returnId", returnId));
            List<String> invoiceIds = new ArrayList<String>();
            for (GenericValue rib : ribs) {
                invoiceIds.add(rib.getString("invoiceId"));
            }

            // find a PaymentAndApplication related to those invoices that match the given parameters
            List<EntityCondition> conds = UtilMisc.<EntityCondition>toList(
                    EntityCondition.makeCondition("partyIdFrom",   EntityOperator.EQUALS, fromParty),
                    EntityCondition.makeCondition("partyIdTo",     EntityOperator.EQUALS, toParty),
                    EntityCondition.makeCondition("paymentTypeId", EntityOperator.EQUALS, "CUSTOMER_REFUND"),
                    EntityCondition.makeCondition("invoiceId",     EntityOperator.IN,     invoiceIds)
                );
            if (amount != null) {
                conds.add(EntityCondition.makeCondition("amount",  EntityOperator.EQUALS, amount));
            }

            List<GenericValue> payments = delegator.findByCondition("PaymentAndApplication", EntityCondition.makeCondition(conds , EntityOperator.AND), null, null);
            assertTrue("Should be at least one 'CUSTOMER_REFUND' Payment from " + fromParty + " to " + toParty + " for Return [" + returnId + "] with status " + paymentStatus, payments.size() > 0);
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException: " + e.toString());
        }
    }

    /**
     * Count the number of Payments for the given Parties
     *  paymentTypeId and status.
     * Can be used to check for a Payment creation.
     *
     * @param fromParty for the Payment
     * @param toParty for the Payment
     * @param paymentTypeId for the Payment
     * @param paymentStatus for the Payment
     * @return the number of payments found
     */
    public long countPayments(String fromParty, String toParty, String paymentTypeId, String paymentStatus) {
        try {
            return delegator.findCountByAnd("Payment", UtilMisc.toMap("partyIdFrom", fromParty, "partyIdTo", toParty, "paymentTypeId", paymentTypeId, "statusId", paymentStatus));
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException: " + e.toString());
            return 0;
        }
    }

    /**
     * Count the number of Payments for the given Parties
     *  paymentTypeId, status and Amount.
     * Can be used to check for a Payment creation.
     *
     * @param fromParty for the Payment
     * @param toParty for the Payment
     * @param paymentTypeId for the Payment
     * @param paymentStatus for the Payment
     * @param amount for the Payment
     * @return the number of payments found
     */
    public long countPayments(String fromParty, String toParty, String paymentTypeId, String paymentStatus, String amount) {
        try {
            return delegator.findCountByAnd("Payment", UtilMisc.toMap("partyIdFrom", fromParty, "partyIdTo", toParty, "paymentTypeId", paymentTypeId, "statusId", paymentStatus, "amount", new BigDecimal(amount)));
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException: " + e.toString());
            return 0;
        }
    }

    /**
     * Assert that all ReturnItem for the given returnId have the given status.
     * @param returnId the return ID
     * @param status the status of the ReturnItems to check
     */
    public void assertReturnItemsStatusEquals(String returnId, String status) {
       try {
            List<GenericValue> items = delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId));
            for (GenericValue item : items) {
                String itemStatus = (String) item.get("statusId");
                assertEquals("ReturnItem [" + item + "] status", status, itemStatus);
            }
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException: " + e.toString());
        }
    }

    /**
     * Change the productStorePaymentSetting.
     * @param productStoreId the product store ID
     * @param paymentMethodTypeId the payment method type ID to set
     * @param paymentServiceTypeEnumId the payment service type ID to set
     * @param paymentService value to set
     */
    public void setProductStorePaymentService(String productStoreId, String paymentMethodTypeId, String paymentServiceTypeEnumId, String paymentService) {
        setProductStorePaymentService(productStoreId, paymentMethodTypeId, paymentServiceTypeEnumId, paymentService, null);
    }

    /**
     * Change the productStorePaymentSetting.
     * @param productStoreId the product store ID
     * @param paymentMethodTypeId the payment method type ID to set
     * @param paymentServiceTypeEnumId the payment service type ID to set
     * @param paymentService value to set
     * @param customMethodId the custom method id, can be used in place of service name, else set to null
     */
    public void setProductStorePaymentService(String productStoreId, String paymentMethodTypeId, String paymentServiceTypeEnumId, String paymentService, String customMethodId) {
        try {
            GenericValue setting = delegator.findByPrimaryKey("ProductStorePaymentSetting", UtilMisc.toMap("productStoreId", productStoreId,
                                                                                                           "paymentMethodTypeId", paymentMethodTypeId,
                                                                                                           "paymentServiceTypeEnumId", paymentServiceTypeEnumId));
            setting.put("paymentService", paymentService);
            setting.put("paymentCustomMethodId", customMethodId);
            Debug.logInfo("setProductStorePaymentService " + productStoreId + " / " + paymentMethodTypeId + " / " + paymentServiceTypeEnumId + " -> serviceName = " + paymentService + ", customMethodId = " + customMethodId, MODULE);
            setting.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * Check payment entries and invoices for a return.
     * -> list ReturnItemBilling for the returnId
     * -> check the InvoiceItem entities and make total due
     * -> check the PaymentAndApplication entities for the invoiceIds
     * we can also check that invoice match payments and status of payment / status of invoice and status of the order payment preference
     * are consistent.
     *
     * @param returnId a <code>String</code> value
     * @param fromParty a <code>String</code> value
     * @param toParty a <code>String</code> value
     * @param invoiceStatus the status that must have all related invoices (most of the time there will be only one)
     *
     */
    public void checkReturnPayments(String returnId, String fromParty, String toParty, String invoiceStatus) {
        // TODO
    }
}
