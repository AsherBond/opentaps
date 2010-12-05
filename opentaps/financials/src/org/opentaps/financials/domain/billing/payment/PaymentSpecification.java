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
package org.opentaps.financials.domain.billing.payment;

import org.opentaps.base.constants.PaymentMethodTypeConstants;
import org.opentaps.base.constants.PaymentTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
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

        CANCELLED(StatusItemConstants.PmntStatus.PMNT_CANCELLED),
        SENT(StatusItemConstants.PmntStatus.PMNT_SENT),
        RECEIVED(StatusItemConstants.PmntStatus.PMNT_RECEIVED),
        CONFIRMED(StatusItemConstants.PmntStatus.PMNT_CONFIRMED),
        NOT_PAID(StatusItemConstants.PmntStatus.PMNT_NOT_PAID),
        VOIDED(StatusItemConstants.PmntStatus.PMNT_VOID);

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

        DISBURSEMENT(PaymentTypeConstants.Disbursement.DISBURSEMENT),
        RECEIPT(PaymentTypeConstants.Receipt.RECEIPT),
        CUSTOMER_REFUND(PaymentTypeConstants.Disbursement.CUSTOMER_REFUND),
        TAX_PAYMENT(PaymentTypeConstants.Disbursement.TAX_PAYMENT),
        PAY_CHECK(PaymentTypeConstants.Disbursement.PAY_CHECK);

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
    public Boolean isBillingAccountPayment(PaymentMethod paymentMethod) {
        return PaymentMethodTypeEnum.BILLING_ACCOUNT.equals(paymentMethod.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isCashOnDeliveryPayment(PaymentMethod paymentMethod) {
        return PaymentMethodTypeEnum.CASH_ON_DELIVERY.equals(paymentMethod.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isCreditCardPayment(PaymentMethod paymentMethod) {
        return PaymentMethodTypeEnum.CREDIT_CARD.equals(paymentMethod.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isGiftCardPayment(PaymentMethod paymentMethod) {
        return PaymentMethodTypeEnum.GIFT_CARD.equals(paymentMethod.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isElectronicFundTransferPayment(PaymentMethod paymentMethod) {
        return PaymentMethodTypeEnum.ELECTRONIC_FUND_TRANSFER.equals(paymentMethod.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isPayPalPayment(PaymentMethod paymentMethod) {
        return PaymentMethodTypeEnum.PAYPAL.equals(paymentMethod.getPaymentMethodTypeId());
    }

    /** {@inheritDoc} */
    public Boolean isOfflinePayment(PaymentMethod paymentMethod) {
        return PaymentMethodTypeEnum.OFFLINE.equals(paymentMethod.getPaymentMethodTypeId());
    }
}
