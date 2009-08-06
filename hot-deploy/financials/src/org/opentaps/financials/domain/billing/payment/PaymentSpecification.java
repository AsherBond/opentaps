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
package org.opentaps.financials.domain.billing.payment;

import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentMethod;
import org.opentaps.domain.billing.payment.PaymentSpecificationInterface;
import org.opentaps.domain.billing.payment.PaymentTypeInterface;

/**
 * Implementation of payment related statuses, types and enumerations
 * for opentaps 1.x running on ofbiz.
 *
 * TODO: payment types are data, not workflow states.  the litmus test is to ask, can I add more
 * of these things without affecting the system?  If the answer is yes, then it's data.  As such,
 * they don't need to be represented with an Enum.
 */
public class PaymentSpecification implements PaymentSpecificationInterface {

    /**
     * Enumeration representing the global payment statuses.  Used by PaymentSpecification
     * to determine the status of a payment.
     *
     * This particular implementation encodes the payment statuses used by opentaps 1.0.
     * If you have your own set of types, then create a new implementation of
     * OrderSpecification and define a different PaymentStatusEnum.
     */
    public static enum PaymentStatusEnum {

        CANCELLED("PMNT_CANCELLED"),
        SENT("PMNT_SENT"),
        RECEIVED("PMNT_RECEIVED"),
        CONFIRMED("PMNT_CONFIRMED"),
        NOT_PAID("PMNT_NOT_PAID"),
        VOIDED("PMNT_VOID");

        private final String statusId;
        private PaymentStatusEnum(String statusId) {
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
    public Boolean isCancelled(Payment payment) {
        return PaymentStatusEnum.CANCELLED.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isNotPaid(Payment payment) {
        return PaymentStatusEnum.NOT_PAID.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isSent(Payment payment) {
        return PaymentStatusEnum.SENT.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isReceived(Payment payment) {
        return PaymentStatusEnum.RECEIVED.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isConfirmed(Payment payment) {
        return PaymentStatusEnum.CONFIRMED.equals(payment.getStatusId());
    }

    /** {@inheritDoc} */
    public Boolean isVoided(Payment payment) {
        return PaymentStatusEnum.VOIDED.equals(payment.getStatusId());
    }

    /**
     * Enumeration representing payment types.  Used by PaymentSpecification
     * to determine the types of payment.
     *
     * This particular implementation encodes the payment types used by opentaps 1.0.
     * If you have your own set of types, then create a new implementation of
     * PaymentSpecification and define a different PaymentTypeEnum that implements PaymentTypeInterface.
     *
     * TODO: expand this to the supported types
     */
    public static enum PaymentTypeEnum implements PaymentTypeInterface {

        DISBURSEMENT("DISBURSEMENT"),
        RECEIPT("RECEIPT"),
        CUSTOMER_REFUND("CUSTOMER_REFUND"),
        TAX_PAYMENT("TAX_PAYMENT"),
        PAY_CHECK("PAY_CHECK");

        private final String typeId;
        private PaymentTypeEnum(String typeId) {
            this.typeId = typeId;
        }

        /**
         * Gets the corresponding type id.
         * @return the type
         */
        public String getTypeId() {
            return typeId;
        }
    }

    /**
     * Enumeration representing the global payment method types.  Used by PaymentSpecification
     * to determine the method of payment.
     *
     * This particular implementation encodes the payment method types used by opentaps 1.0.
     * If you have your own set of types, then create a new implementation of
     * PaymentSpecification and define a different PaymentMethodTypeEnum.
     *
     * Note about the model:  Payments can sometimes have no paymentMethodId, but they can still
     * have a paymentMethodTypeId.  For example, simple cash payments usually don't have a payment method
     * tied to any specific card, account or person.
     */
    public static enum PaymentMethodTypeEnum {

        BILLING_ACCOUNT("EXT_BILLACT"),
        CASH_ON_DELIVERY("EXT_COD"),
        CREDIT_CARD("CREDIT_CARD"),
        ELECTRONIC_FUND_TRANSFER("EFT_ACCOUNT"),
        OFFLINE("EXT_OFFLINE"),
        PAYPAL("EXT_PAYPAL"),
        GIFT_CARD("GIFT_CARD");

        private final String typeId;
        private PaymentMethodTypeEnum(String typeId) {
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
         * Checks that the payment method type is equal to the given string.
         * @param typeId the status to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String typeId) {
            return this.typeId.equals(typeId);
        }
    }

    /** {@inheritDoc} */
    public Boolean isBillingAccountPayment(Payment payment) {
        return PaymentMethodTypeEnum.BILLING_ACCOUNT.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isCashOnDeliveryPayment(Payment payment) {
        return PaymentMethodTypeEnum.CASH_ON_DELIVERY.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isCreditCardPayment(Payment payment) {
        return PaymentMethodTypeEnum.CREDIT_CARD.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isGiftCardPayment(Payment payment) {
        return PaymentMethodTypeEnum.GIFT_CARD.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isElectronicFundTransferPayment(Payment payment) {
        return PaymentMethodTypeEnum.ELECTRONIC_FUND_TRANSFER.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isPayPalPayment(Payment payment) {
        return PaymentMethodTypeEnum.PAYPAL.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isOfflinePayment(Payment payment) {
        return PaymentMethodTypeEnum.OFFLINE.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isBillingAccountPayment(PaymentMethod payment) {
        return PaymentMethodTypeEnum.BILLING_ACCOUNT.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isCashOnDeliveryPayment(PaymentMethod payment) {
        return PaymentMethodTypeEnum.CASH_ON_DELIVERY.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isCreditCardPayment(PaymentMethod payment) {
        return PaymentMethodTypeEnum.CREDIT_CARD.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isGiftCardPayment(PaymentMethod payment) {
        return PaymentMethodTypeEnum.GIFT_CARD.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isElectronicFundTransferPayment(PaymentMethod payment) {
        return PaymentMethodTypeEnum.ELECTRONIC_FUND_TRANSFER.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isPayPalPayment(PaymentMethod payment) {
        return PaymentMethodTypeEnum.PAYPAL.equals(payment.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isOfflinePayment(PaymentMethod payment) {
        return PaymentMethodTypeEnum.OFFLINE.equals(payment.getPaymentMethodTypeId());
    }
}
