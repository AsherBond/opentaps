/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.billing.payment;

/**
 * Interface for a payment specification.  When you add your own status codes and other
 * specification related flags and logic, extend this interface, extend the billing domain,
 * and create your own versions of the status and type enumerations.
 */
public interface PaymentSpecificationInterface {

    /**
     * Checks if the given <code>Payment</code> is cancelled.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCancelled(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is not paid.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isNotPaid(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is sent.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isSent(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is received.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isReceived(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is confirmed.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isConfirmed(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is voided.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isVoided(Payment payment);

    /**
     * Checks if the given <code>PaymentMethod</code> is a billing account payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isBillingAccountPayment(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is a cash on delivery payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCashOnDeliveryPayment(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is a credit card payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCreditCardPayment(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is a gift card payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isGiftCardPayment(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is an electronic transfer payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isElectronicFundTransferPayment(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is a paypal payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isPayPalPayment(Payment payment);

    /**
     * Checks if the given <code>Payment</code> is an offline payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isOfflinePayment(Payment payment);

    /**
     * Checks if the given <code>PaymentMethod</code> is a billing account payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isBillingAccountPayment(PaymentMethod payment);

    /**
     * Checks if the given <code>Payment</code> is a cash on delivery payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCashOnDeliveryPayment(PaymentMethod payment);

    /**
     * Checks if the given <code>Payment</code> is a credit card payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isCreditCardPayment(PaymentMethod payment);

    /**
     * Checks if the given <code>Payment</code> is a gift card payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isGiftCardPayment(PaymentMethod payment);

    /**
     * Checks if the given <code>Payment</code> is an electronic transfer payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isElectronicFundTransferPayment(PaymentMethod payment);

    /**
     * Checks if the given <code>Payment</code> is a paypal payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isPayPalPayment(PaymentMethod payment);

    /**
     * Checks if the given <code>Payment</code> is an offline payment.
     * @param payment an <code>Payment</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean isOfflinePayment(PaymentMethod payment);

}
