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

package org.opentaps.common.domain.order;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.ofbiz.base.util.UtilNumber;
import org.opentaps.base.constants.OrderAdjustmentTypeConstants;
import org.opentaps.base.constants.OrderTypeConstants;
import org.opentaps.base.constants.PaymentMethodTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.domain.order.*;

/**
 * Common specifications for the Order domain.
 *
 * These specifications contain mapping of conceptual status to their database equivalents, as well as groupings into higher level concepts.
 * (ie, all open order status)
 *
 * This class may be expanded to include other order domain validation code.
 */
public final class OrderSpecification implements OrderSpecificationInterface {

    public static final String UNKNOWN_SHIPPING_ADDRESS = "_NA_";

    // scales and rounding modes for BigDecimal math
    public static final int scale = UtilNumber.getBigDecimalScale("order.decimals");
    public static final int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
    public static final int taxCalcScale = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    public static final int taxFinalScale = UtilNumber.getBigDecimalScale("salestax.final.decimals");
    public static final int taxRounding = UtilNumber.getBigDecimalRoundingMode("salestax.rounding");

    /**
     * The number of digit that should be printed when displaying the stripped
     * number for credit cards, gift cards, etc ...
     */
    public static final int STRIPPED_NUMBER_LENGTH = 4;

    /** {@inheritDoc} */
    public int getScale() {
        return OrderSpecification.scale;
    }

    /** {@inheritDoc} */
    public int getRounding() {
        return OrderSpecification.rounding;
    }

    /** {@inheritDoc} */
    public int getTaxCalculationScale() {
        return OrderSpecification.taxCalcScale;
    }

    /** {@inheritDoc} */
    public int getTaxFinalScale() {
        return OrderSpecification.taxFinalScale;
    }

    /** {@inheritDoc} */
    public int getTaxRounding() {
        return OrderSpecification.taxRounding;
    }

    /** {@inheritDoc} */
    public BigDecimal defaultRounding(BigDecimal amount) {
        return amount.setScale(getScale(), getRounding());
    }

    /** {@inheritDoc} */
    public BigDecimal taxCalculationRounding(BigDecimal amount) {
        return amount.setScale(getTaxCalculationScale(), getTaxRounding());
    }

    /** {@inheritDoc} */
    public BigDecimal taxFinalRounding(BigDecimal amount) {
        return amount.setScale(getTaxFinalScale(), getTaxRounding());
    }

    /**
     * Enumeration representing the global order status.  Used by OrderSpecification
     * to determine the state of an order domain.
     *
     * This particular implementation encodes the statusIds used by opentaps 1.0.
     * If you have your own set of statuses, then create a new implementation of
     * OrderSpecification and define a different OrderStatusEnum.
     */
    public static enum OrderStatusEnum {

        CREATED(StatusItemConstants.OrderStatus.ORDER_CREATED),
        APPROVED(StatusItemConstants.OrderStatus.ORDER_APPROVED),
        PROCESSING(StatusItemConstants.OrderStatus.ORDER_PROCESSING),
        HOLD(StatusItemConstants.OrderStatus.ORDER_HOLD),
        SENT(StatusItemConstants.OrderStatus.ORDER_SENT),
        COMPLETED(StatusItemConstants.OrderStatus.ORDER_COMPLETED),
        CANCELLED(StatusItemConstants.OrderStatus.ORDER_CANCELLED),
        REJECTED(StatusItemConstants.OrderStatus.ORDER_REJECTED),
        UNDELIVERABLE(StatusItemConstants.OrderStatus.ORDER_UNDELIVERABLE);

        private final String statusId;
        private OrderStatusEnum(String statusId) {
            this.statusId = statusId;
        }

        /**
         * Gets the corresponding status id.
         * @return the status
         */
        public String getStatusId() {
            return statusId;
        }

        /**
         * Checks that the order status is equal to the string from order.getStatusId().
         * @param statusId the status to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String statusId) {
            return this.statusId.equals(statusId);
        }
    }

    /** {@inheritDoc} */
    public Boolean isApproved(Order order) {
        return OrderStatusEnum.APPROVED.equals(order.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isCompleted(Order order) {
        return OrderStatusEnum.COMPLETED.equals(order.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isCancelled(Order order) {
        return OrderStatusEnum.CANCELLED.equals(order.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isCreated(Order order) {
        return OrderStatusEnum.CREATED.equals(order.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isRejected(Order order) {
        return OrderStatusEnum.REJECTED.equals(order.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isSent(Order order) {
        return OrderStatusEnum.SENT.equals(order.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isProcessing(Order order) {
        return OrderStatusEnum.PROCESSING.equals(order.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isOnHold(Order order) {
        return OrderStatusEnum.HOLD.equals(order.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isPickable(Order order) {
        return isApproved(order);
    }

    /**
     * Enumeration representing the global order types.  Used by OrderSpecification
     * to determine the type of an order domain.
     *
     * This particular implementation encodes the orderTypeIds used by opentaps 1.0.
     * If you have your own set of types, then create a new implementation of
     * OrderSpecification and define a different OrderTypeEnum.
     */
    public static enum OrderTypeEnum {

        SALES(OrderTypeConstants.SALES_ORDER),
        PURCHASE(OrderTypeConstants.PURCHASE_ORDER);

        private final String orderTypeId;
        private OrderTypeEnum(String orderTypeId) {
            this.orderTypeId = orderTypeId;
        }

        /**
         * Gets the corresponding order type id.
         * @return the order type
         */
        public String getOrderTypeId() {
            return orderTypeId;
        }

        /**
         * Checks that the order type is equal to the string from order.getOrderTypeId().
         * @param orderTypeId the type to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String orderTypeId) {
            return this.orderTypeId.equals(orderTypeId);
        }
    }

    /** {@inheritDoc} */
    public Boolean isSalesOrder(Order order) {
        return OrderTypeEnum.SALES.equals(order.getOrderTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isPurchaseOrder(Order order) {
        return OrderTypeEnum.PURCHASE.equals(order.getOrderTypeId());
    }

    /**
     * Enumeration representing the global return types.  Used by OrderSpecification
     * to determine the type of an order domain.
     *
     * This particular implementation encodes the returnTypeIds used by opentaps 1.0.
     * If you have your own set of types, then create a new implementation of
     * OrderSpecification and define a different ReturnTypeEnum.
     */
    public static enum ReturnTypeEnum {

        CUSTOMER("CUSTOMER_RETURN"),
        VENDOR("VENDOR_RETURN");

        private final String returnTypeId;
        private ReturnTypeEnum(String returnTypeId) {
            this.returnTypeId = returnTypeId;
        }

        /**
         * Gets the corresponding return type id.
         * @return the return type
         */
        public String getReturnTypeId() {
            return returnTypeId;
        }

        /**
         * Checks that the return type is equal to the string from return.getReturnHeaderTypeId().
         * @param returnTypeId the type to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String returnTypeId) {
            return this.returnTypeId.equals(returnTypeId);
        }
    }

    /** {@inheritDoc} */
    public String getReturnType(Order order) {
        if (order.isSalesOrder()) {
            return ReturnTypeEnum.CUSTOMER.getReturnTypeId();
        } else if (order.isPurchaseOrder()) {
            return ReturnTypeEnum.VENDOR.getReturnTypeId();
        }
        return null;
    }

    /**
     * Enumeration representing the global return status.  Used by OrderSpecification
     * to determine the state of an order domain.
     *
     * This particular implementation encodes the statusIds used by opentaps 1.0.
     * If you have your own set of statuses, then create a new implementation of
     * OrderSpecification and define a different ReturnStatusEnum.
     */
    public static enum ReturnStatusEnum {

        ACCEPTED(StatusItemConstants.OrderReturnStts.RETURN_ACCEPTED),
        COMPLETED(StatusItemConstants.OrderReturnStts.RETURN_COMPLETED),
        CANCELLED(StatusItemConstants.OrderReturnStts.RETURN_CANCELLED),
        RECEIVED(StatusItemConstants.OrderReturnStts.RETURN_RECEIVED),
        REQUESTED(StatusItemConstants.OrderReturnStts.RETURN_REQUESTED),
        MANUAL_REFUND_REQUIRED(StatusItemConstants.OrderReturnStts.RETURN_MAN_REFUND);

        private final String statusId;
        private ReturnStatusEnum(String statusId) {
            this.statusId = statusId;
        }

        /**
         * Gets the corresponding status id.
         * @return the status
         */
        public String getStatusId() {
            return statusId;
        }

        /**
         * Checks that the order status is equal to the string from return.getStatusId().
         * @param statusId the status to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String statusId) {
            return this.statusId.equals(statusId);
        }
    }

    /** {@inheritDoc} */
    public Boolean isCancelled(Return returnObj) {
        return ReturnStatusEnum.CANCELLED.equals(returnObj.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isAccepted(Return returnObj) {
        return ReturnStatusEnum.ACCEPTED.equals(returnObj.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isCompleted(Return returnObj) {
        return ReturnStatusEnum.COMPLETED.equals(returnObj.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isRequested(Return returnObj) {
        return ReturnStatusEnum.REQUESTED.equals(returnObj.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isReceived(Return returnObj) {
        return ReturnStatusEnum.RECEIVED.equals(returnObj.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isRequiringManualRefund(Return returnObj) {
        return ReturnStatusEnum.MANUAL_REFUND_REQUIRED.equals(returnObj.getStatusId());
    }

    /**
     * Enumeration representing the global order item status.  Used by OrderSpecification
     * to determine the state of an order item domain.
     *
     * This particular implementation encodes the statusIds used by opentaps 1.0.
     * If you have your own set of statuses, then create a new implementation of
     * OrderSpecification and define a different OrderItemStatusEnum.
     */
    public static enum OrderItemStatusEnum {

        CREATED(StatusItemConstants.OrderItemStatus.ITEM_CREATED),
        APPROVED(StatusItemConstants.OrderItemStatus.ITEM_APPROVED),
        PERFORMED(StatusItemConstants.OrderItemStatus.ITEM_PERFORMED),
        COMPLETED(StatusItemConstants.OrderItemStatus.ITEM_COMPLETED),
        CANCELLED(StatusItemConstants.OrderItemStatus.ITEM_CANCELLED),
        REJECTED(StatusItemConstants.OrderItemStatus.ITEM_REJECTED);

        private final String statusId;
        private OrderItemStatusEnum(String statusId) {
            this.statusId = statusId;
        }

        /**
         * Gets the corresponding status id.
         * @return the status
         */
        public String getStatusId() {
            return statusId;
        }

        /**
         * Checks that the order status is equal to the string from orderItem.getStatusId().
         * @param statusId the status to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String statusId) {
            return this.statusId.equals(statusId);
        }
    }

    /** {@inheritDoc} */
    public Boolean isApproved(OrderItem orderItem) {
        return OrderItemStatusEnum.APPROVED.equals(orderItem.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isCompleted(OrderItem orderItem) {
        return OrderItemStatusEnum.COMPLETED.equals(orderItem.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isCancelled(OrderItem orderItem) {
        return OrderItemStatusEnum.CANCELLED.equals(orderItem.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isCreated(OrderItem orderItem) {
        return OrderItemStatusEnum.CREATED.equals(orderItem.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isRejected(OrderItem orderItem) {
        return OrderItemStatusEnum.REJECTED.equals(orderItem.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isPerformed(OrderItem orderItem) {
        return OrderItemStatusEnum.PERFORMED.equals(orderItem.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isPromo(OrderItem orderItem) {
        return "Y".equals(orderItem.getIsPromo());
    }

    /**
     * Enumeration representing the global order adjustment type.  Used by OrderSpecification
     * to determine the state of an order adjustment domain.
     *
     * This particular implementation encodes the orderAdjustmentTypeIds used by opentaps 1.0.
     * If you have your own set of types, then create a new implementation of
     * OrderSpecification and define a different OrderAdjustmentTypeEnum.
     */
    public static enum OrderAdjustmentTypeEnum {

        SALES_TAX(OrderAdjustmentTypeConstants.SALES_TAX),
        SHIPPING_CHARGES(OrderAdjustmentTypeConstants.SHIPPING_CHARGES);

        private final String typeId;
        private OrderAdjustmentTypeEnum(String typeId) {
            this.typeId = typeId;
        }

        /**
         * Gets the corresponding type id.
         * @return the type
         */
        public String getTypeId() {
            return typeId;
        }

        /**
         * Checks that the order adjustment type is equal to the string from adjustment.getOrderAdjustmentTypeId().
         * @param typeId the type to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String typeId) {
            return this.typeId.equals(typeId);
        }
    }

    /** {@inheritDoc} */
    public Boolean isSalesTax(OrderAdjustment adjustment) {
        return OrderAdjustmentTypeEnum.SALES_TAX.equals(adjustment.getOrderAdjustmentTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isShippingCharge(OrderAdjustment adjustment) {
        return OrderAdjustmentTypeEnum.SHIPPING_CHARGES.equals(adjustment.getOrderAdjustmentTypeId());
    }

    /**
     * Enumeration representing the global payment preference statuses.  Used by OrderSpecification
     * to determine the state of an order adjustment domain.
     *
     * This particular implementation encodes the payment statuses used by opentaps 1.0.
     * If you have your own set of types, then create a new implementation of
     * OrderSpecification and define a different PaymentPreferenceStatusEnum.
     */
    public static enum PaymentPreferenceStatusEnum {

        CANCELLED(StatusItemConstants.PaymentPrefStatus.PAYMENT_CANCELLED),
        AUTHORIZED(StatusItemConstants.PaymentPrefStatus.PAYMENT_AUTHORIZED),
        NOT_AUTHORIZED(StatusItemConstants.PaymentPrefStatus.PAYMENT_NOT_AUTH),
        DECLINED(StatusItemConstants.PaymentPrefStatus.PAYMENT_DECLINED),
        RECEIVED(StatusItemConstants.PaymentPrefStatus.PAYMENT_RECEIVED),
        NOT_RECEIVED(StatusItemConstants.PaymentPrefStatus.PAYMENT_NOT_RECEIVED),
        REFUNDED(StatusItemConstants.PaymentPrefStatus.PAYMENT_REFUNDED),
        SETTLED(StatusItemConstants.PaymentPrefStatus.PAYMENT_SETTLED);

        private final String statusId;
        private PaymentPreferenceStatusEnum(String statusId) {
            this.statusId = statusId;
        }

        /**
         * Gets the corresponding status id.
         * @return the type
         */
        public String getStatusId() {
            return statusId;
        }

        /**
         * Checks that the payment status is equal to the given string.
         * @param statusId the status to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String statusId) {
            return this.statusId.equals(statusId);
        }
    }

    /** {@inheritDoc} */
    public Boolean isCancelled(OrderPaymentPreference payment) {
        return PaymentPreferenceStatusEnum.CANCELLED.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isAuthorized(OrderPaymentPreference payment) {
        return PaymentPreferenceStatusEnum.AUTHORIZED.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isNotAuthorized(OrderPaymentPreference payment) {
        return PaymentPreferenceStatusEnum.NOT_AUTHORIZED.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isDeclined(OrderPaymentPreference payment) {
        return PaymentPreferenceStatusEnum.DECLINED.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isReceived(OrderPaymentPreference payment) {
        return PaymentPreferenceStatusEnum.RECEIVED.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isNotReceived(OrderPaymentPreference payment) {
        return PaymentPreferenceStatusEnum.NOT_RECEIVED.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isRefunded(OrderPaymentPreference payment) {
        return PaymentPreferenceStatusEnum.REFUNDED.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isSettled(OrderPaymentPreference payment) {
        return PaymentPreferenceStatusEnum.SETTLED.equals(payment.getStatusId());
    }

    /**
     * Enumeration representing the global shipment status.  Used by OrderSpecification
     * to determine the state of a shipment domain.
     *
     * This particular implementation encodes the statusIds used by opentaps 1.0.
     * If you have your own set of statuses, then create a new implementation of
     * OrderSpecification and define a different ShipmentStatusEnum.
     */
    public static enum ShipmentStatusEnum {

        INPUT(StatusItemConstants.ShipmentStatus.SHIPMENT_INPUT),
        SCHEDULED(StatusItemConstants.ShipmentStatus.SHIPMENT_SCHEDULED),
        PICKED(StatusItemConstants.ShipmentStatus.SHIPMENT_PICKED),
        PACKED(StatusItemConstants.ShipmentStatus.SHIPMENT_PACKED),
        SHIPPED(StatusItemConstants.ShipmentStatus.SHIPMENT_SHIPPED),
        DELIVERED(StatusItemConstants.ShipmentStatus.SHIPMENT_DELIVERED),
        CANCELLED(StatusItemConstants.ShipmentStatus.SHIPMENT_CANCELLED);

        private final String statusId;
        private ShipmentStatusEnum(String statusId) {
            this.statusId = statusId;
        }

        /**
         * Gets the corresponding status id.
         * @return the status
         */
        public String getStatusId() {
            return statusId;
        }

        /**
         * Checks that the order status is equal to the string from orderItem.getStatusId().
         * @param statusId the status to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String statusId) {
            return this.statusId.equals(statusId);
        }
    }

    /** {@inheritDoc} */
    public Boolean isInput(Shipment shipment) {
        return ShipmentStatusEnum.INPUT.equals(shipment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isScheduled(Shipment shipment) {
        return ShipmentStatusEnum.SCHEDULED.equals(shipment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isPicked(Shipment shipment) {
        return ShipmentStatusEnum.PICKED.equals(shipment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isPacked(Shipment shipment) {
        return ShipmentStatusEnum.PACKED.equals(shipment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isShipped(Shipment shipment) {
        return ShipmentStatusEnum.SHIPPED.equals(shipment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isDelivered(Shipment shipment) {
        return ShipmentStatusEnum.DELIVERED.equals(shipment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isCancelled(Shipment shipment) {
        return ShipmentStatusEnum.CANCELLED.equals(shipment.getStatusId());
    }

    /** {@inheritDoc} */
    public List<String> commissionAgentRoleTypeIds() {
        return Arrays.asList(RoleTypeConstants.COMMISSION_AGENT);
    }

    /** {@inheritDoc} */
    public List<String> placingCustomerRoleTypeIds() {
        return Arrays.asList(RoleTypeConstants.PLACING_CUSTOMER);
    }

    /** {@inheritDoc} */
    public List<String> billToCustomerRoleTypeIds() {
        return Arrays.asList(RoleTypeConstants.BILL_TO_CUSTOMER);
    }

    /** {@inheritDoc} */
    public List<String> billFromVendorRoleTypeIds() {
        return Arrays.asList(RoleTypeConstants.BILL_FROM_VENDOR);
    }

    /** {@inheritDoc} */
    public List<String> supplierAgentRoleTypeIds() {
        return Arrays.asList(RoleTypeConstants.SUPPLIER_AGENT);
    }

    /** {@inheritDoc} */
    public List<String> shipToCustomerRoleTypeIds() {
        return Arrays.asList(RoleTypeConstants.SHIP_TO_CUSTOMER);
    }

    /** {@inheritDoc} */
    public List<String> distributorRoleTypeIds() {
        return Arrays.asList(RoleTypeConstants.DISTRIBUTOR);
    }

    /** {@inheritDoc} */
    public List<String> affiliateRoleTypeIds() {
        return Arrays.asList(RoleTypeConstants.AFFILIATE);
    }

    /** {@inheritDoc} */
    public String getUnknownShippingAddress() {
        return OrderSpecification.UNKNOWN_SHIPPING_ADDRESS;
    }

    /** {@inheritDoc} */
    public int cardsStrippedNumberLength() {
        return OrderSpecification.STRIPPED_NUMBER_LENGTH;
    }

    /**
     * Enumeration representing the global payment method types.  Used by PaymentSpecification
     * to determine the method of payment.
     *
     * This particular implementation encodes the payment method types used by opentaps 1.0.
     * If you have your own set of types, then create a new implementation of
     * OrderSpecification and define a different PaymentMethodTypeEnum.
     */
    public static enum PaymentMethodTypeEnum {

        BILLING_ACCOUNT(PaymentMethodTypeConstants.EXT_BILLACT),
        CASH_ON_DELIVERY(PaymentMethodTypeConstants.EXT_COD),
        CREDIT_CARD(PaymentMethodTypeConstants.CREDIT_CARD),
        ELECTRONIC_FUND_TRANSFER(PaymentMethodTypeConstants.EFT_ACCOUNT),
        OFFLINE(PaymentMethodTypeConstants.EXT_OFFLINE),
        PAYPAL(PaymentMethodTypeConstants.EXT_PAYPAL),
        GIFT_CARD(PaymentMethodTypeConstants.GIFT_CARD);

        private final String typeId;
        private PaymentMethodTypeEnum(String typeId) {
            this.typeId = typeId;
        }

        /**
         * Gets the corresponding status id.
         * @return the type
         */
        public String getTypeId() {
            return typeId;
        }

        /**
         * Checks that the payment method type is equal to the given string.
         * @param typeId the status to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String typeId) {
            return this.typeId.equals(typeId);
        }
    }

    /** {@inheritDoc} */
    public Boolean isBillingAccountPayment(OrderPaymentPreference payment) {
        return PaymentMethodTypeEnum.BILLING_ACCOUNT.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isCashOnDeliveryPayment(OrderPaymentPreference payment) {
        return PaymentMethodTypeEnum.CASH_ON_DELIVERY.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isCreditCardPayment(OrderPaymentPreference payment) {
        return PaymentMethodTypeEnum.CREDIT_CARD.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isGiftCardPayment(OrderPaymentPreference payment) {
        return PaymentMethodTypeEnum.GIFT_CARD.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isElectronicFundTransferPayment(OrderPaymentPreference payment) {
        return PaymentMethodTypeEnum.ELECTRONIC_FUND_TRANSFER.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isPayPalPayment(OrderPaymentPreference payment) {
        return PaymentMethodTypeEnum.PAYPAL.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isOfflinePayment(OrderPaymentPreference payment) {
        return PaymentMethodTypeEnum.OFFLINE.equals(payment.getPaymentMethodTypeId());
    }

}
