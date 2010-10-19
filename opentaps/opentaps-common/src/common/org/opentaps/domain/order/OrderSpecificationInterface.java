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
package org.opentaps.domain.order;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface for an order specification.  When you add your own status codes and other
 * specification related flags and logic, extend this interface, extend the order domain,
 * and create your own version of the OrderStatus enumeration.
 */
public interface OrderSpecificationInterface {

    /* Logical methods to determine status of order. */

    /**
     * Checks if the given <code>Order</code> is approved.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isApproved(Order order);

    /**
     * Checks if the given <code>Order</code> is completed.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCompleted(Order order);

    /**
     * Checks if the given <code>Order</code> is cancelled.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCancelled(Order order);

    /**
     * Checks if the given <code>Order</code> is created.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCreated(Order order);

    /**
     * Checks if the given <code>Order</code> is rejected.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isRejected(Order order);

    /**
     * Checks if the given <code>Order</code> is sent.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isSent(Order order);

    /**
     * Checks if the given <code>Order</code> is processing.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isProcessing(Order order);

    /**
     * Checks if the given <code>Order</code> is on hold.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isOnHold(Order order);

    /**
     * Checks if the given <code>Order</code> is pickable.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isPickable(Order order);

    /* Logical methods to determine type of order. */

    /**
     * Checks if the given <code>Order</code> is a sales order.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isSalesOrder(Order order);

    /**
     * Checks if the given <code>Order</code> is a purchase order.
     * @param order an <code>Order</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isPurchaseOrder(Order order);

    /* Logical methods to determine status of order item. */

    /**
     * Checks if the given <code>OrderItem</code> is approved.
     * @param orderItem an <code>OrderItem</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isApproved(OrderItem orderItem);

    /**
     * Checks if the given <code>OrderItem</code> is completed.
     * @param orderItem an <code>OrderItem</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCompleted(OrderItem orderItem);

    /**
     * Checks if the given <code>OrderItem</code> is cancelled.
     * @param orderItem an <code>OrderItem</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCancelled(OrderItem orderItem);

    /**
     * Checks if the given <code>OrderItem</code> is created.
     * @param orderItem an <code>OrderItem</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCreated(OrderItem orderItem);

    /**
     * Checks if the given <code>OrderItem</code> is rejected.
     * @param orderItem an <code>OrderItem</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isRejected(OrderItem orderItem);

    /**
     * Checks if the given <code>OrderItem</code> is performed.
     * @param orderItem an <code>OrderItem</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isPerformed(OrderItem orderItem);

    /**
     * Checks if the given <code>OrderItem</code> is a promo item.
     * @param orderItem an <code>OrderItem</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isPromo(OrderItem orderItem);

    /* For Return */

    /**
     * Gets the type or <code>Return</code> applicable to the given <code>Order</code>.
     * @param order an <code>Order</code> value
     * @return the type of return
     */
    public String getReturnType(Order order);

    /* Logical methods to determine status of return. */

    /**
     * Checks if the given <code>Return</code> is cancelled.
     * @param returnObj a <code>Return</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCancelled(Return returnObj);

    /**
     * Checks if the given <code>Return</code> is accepted.
     * @param returnObj a <code>Return</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isAccepted(Return returnObj);

    /**
     * Checks if the given <code>Return</code> is completed.
     * @param returnObj a <code>Return</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCompleted(Return returnObj);

    /**
     * Checks if the given <code>Return</code> is requested.
     * @param returnObj a <code>Return</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isRequested(Return returnObj);

    /**
     * Checks if the given <code>Return</code> is received.
     * @param returnObj a <code>Return</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isReceived(Return returnObj);

    /**
     * Checks if the given <code>Return</code> is requiring manual refund.
     * @param returnObj a <code>Return</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isRequiringManualRefund(Return returnObj);

    /* Logical methods to determine the type of order adjustment. */

    /**
     * Checks if the given <code>OrderAdjustment</code> is a sales tax.
     * @param adjustment an <code>OrderAdjustment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isSalesTax(OrderAdjustment adjustment);

    /**
     * Checks if the given <code>OrderAdjustment</code> is a shipping charge.
     * @param adjustment an <code>OrderAdjustment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isShippingCharge(OrderAdjustment adjustment);

    /* Logical methods to determine the status of payments. */

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is cancelled.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCancelled(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is authorized.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isAuthorized(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is not authorized.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isNotAuthorized(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is declined.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isDeclined(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is received.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isReceived(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is not received.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isNotReceived(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is refunded.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isRefunded(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is settled.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isSettled(OrderPaymentPreference payment);

    /* Logical methods to determine the type of payments. */

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is a billing account payment.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isBillingAccountPayment(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is a cash on delivery payment.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCashOnDeliveryPayment(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is a credit card payment.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCreditCardPayment(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is a gift card payment.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isGiftCardPayment(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is an electronic transfer payment.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isElectronicFundTransferPayment(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is a paypal payment.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isPayPalPayment(OrderPaymentPreference payment);

    /**
     * Checks if the given <code>OrderPaymentPreference</code> is an offline payment.
     * @param payment an <code>OrderPaymentPreference</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isOfflinePayment(OrderPaymentPreference payment);

    /* Return lists of role type Ids. */

    /**
     * Gets the list of party role types applicable for an order commission agent.
     * @return the list of party role types applicable
     */
    public List<String> commissionAgentRoleTypeIds();

    /**
     * Gets the list of party role types applicable for an order placing customer.
     * @return the list of party role types applicable
     */
    public List<String> placingCustomerRoleTypeIds();

    /**
     * Gets the list of party role types applicable for an order bill to customer.
     * @return the list of party role types applicable
     */
    public List<String> billToCustomerRoleTypeIds();

    /**
     * Gets the list of party role types applicable for an order supplier agent.
     * @return the list of party role types applicable
     */
    public List<String> supplierAgentRoleTypeIds();

    /**
     * Gets the list of party role types applicable for an order bill from vendor.
     * @return the list of party role types applicable
     */
    public List<String> billFromVendorRoleTypeIds();

    /**
     * Gets the list of party role types applicable for an order ship to customer.
     * @return the list of party role types applicable
     */
    public List<String> shipToCustomerRoleTypeIds();

    /**
     * Gets the list of party role types applicable for an order distributor.
     * @return the list of party role types applicable
     */
    public List<String> distributorRoleTypeIds();

    /**
     * Gets the list of party role types applicable for an order affiliate.
     * @return the list of party role types applicable
     */
    public List<String> affiliateRoleTypeIds();

    /* Logical methods to determine the status of shipments. */

    /**
     * Checks if the given <code>Shipment</code> is input.
     * @param shipment an <code>Shipment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isInput(Shipment shipment);

    /**
     * Checks if the given <code>Shipment</code> is scheduled.
     * @param shipment an <code>Shipment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isScheduled(Shipment shipment);

    /**
     * Checks if the given <code>Shipment</code> is picked.
     * @param shipment an <code>Shipment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isPicked(Shipment shipment);

    /**
     * Checks if the given <code>Shipment</code> is packed.
     * @param shipment an <code>Shipment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isPacked(Shipment shipment);

    /**
     * Checks if the given <code>Shipment</code> is shipped.
     * @param shipment an <code>Shipment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isShipped(Shipment shipment);

    /**
     * Checks if the given <code>Shipment</code> is delivered.
     * @param shipment an <code>Shipment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isDelivered(Shipment shipment);

    /**
     * Checks if the given <code>Shipment</code> is cancelled.
     * @param shipment an <code>Shipment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCancelled(Shipment shipment);

    /* Shipping. */

    /**
     * Gets the unknown shipping address contact mech id.
     * @return the unknown shipping address contact mech id
     */
    public String getUnknownShippingAddress();

    /* Scaling methods. */

    /**
     * Gets the default scale used for rounding <code>BigDecimal</code>.
     * @return the scale
     */
    public int getScale();

    /**
     * Gets the tax calculation scale used for rounding <code>BigDecimal</code>.
     * The tax calculation is used for intermediate results and is larger than the tax finale scale.
     * @return the scale
     * @see #getTaxFinalScale
     */
    public int getTaxCalculationScale();

    /**
     * Gets the tax final scale used for rounding <code>BigDecimal</code>.
     * The tax final is used for final results.
     * @return the scale
     * @see #getTaxCalculationScale
     */
    public int getTaxFinalScale();

    /**
     * Gets the default rounding method used for rounding <code>BigDecimal</code>.
     * @return the rounding
     */
    public int getRounding();

    /**
     * Gets the tax rounding method used for rounding <code>BigDecimal</code>.
     * @return the rounding
     */
    public int getTaxRounding();

    /**
     * Rounds a <code>BigDecimal</code> using the default scale and rounding.
     * @param amount the amount to round
     * @return the rounded <code>BigDecimal</code>
     */
    public BigDecimal defaultRounding(BigDecimal amount);

    /**
     * Rounds a <code>BigDecimal</code> using the tax calculation scale and rounding.
     * @param amount the amount to round
     * @return the rounded <code>BigDecimal</code>
     */
    public BigDecimal taxCalculationRounding(BigDecimal amount);

    /**
     * Rounds a <code>BigDecimal</code> using the tax final scale and rounding.
     * @param amount the amount to round
     * @return the rounded <code>BigDecimal</code>
     */
    public BigDecimal taxFinalRounding(BigDecimal amount);

    /**
     * Gets the stripped length that is used when displaying card numbers.
     * This is used for example to print out credit card number and not to reveal them completely.
     * @return the size of the number string that should be revealed
     */
    public int cardsStrippedNumberLength();

}
