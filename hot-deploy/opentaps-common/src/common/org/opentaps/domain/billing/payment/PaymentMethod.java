/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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

import org.opentaps.foundation.repository.RepositoryException;

/**
 * Payment Method entity.
 */
public class PaymentMethod extends org.opentaps.domain.base.entities.PaymentMethod {

    private CreditCard creditCard;
    private GiftCard giftCard;

    /**
     * Default constructor.
     */
    public PaymentMethod() {
        super();
    }

    /**
     * Get the specification object which contains enumerations and logical checking for Payments.
     * @return the <code>PaymentSpecificationInterface</code>
     * @exception RepositoryException if an error occurs
     */
    public PaymentSpecificationInterface getPaymentSpecification() throws RepositoryException {
        return getRepository().getPaymentSpecification();
    }

    /**
     * Is this payment method type "billing account".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isBillingAccount() throws RepositoryException {
        return getPaymentSpecification().isBillingAccountPayment(this);
    }

    /**
     * Is this payment method type "cash on delivery".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isCashOnDelivery() throws RepositoryException {
        return getPaymentSpecification().isCashOnDeliveryPayment(this);
    }

    /**
     * Is this payment method type "credit card".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isCreditCard() throws RepositoryException {
        return getPaymentSpecification().isCreditCardPayment(this);
    }

    /**
     * Is this payment method type "gift card".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isGiftCard() throws RepositoryException {
        return getPaymentSpecification().isGiftCardPayment(this);
    }

    /**
     * Is this payment method type "electronic fund transfer".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isElectronicFundTransfer() throws RepositoryException {
        return getPaymentSpecification().isElectronicFundTransferPayment(this);
    }

    /**
     * Is this payment method type "paypal".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isPayPal() throws RepositoryException {
        return getPaymentSpecification().isPayPalPayment(this);
    }

    /**
     * Is this payment method type "offline".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isOffline() throws RepositoryException {
        return getPaymentSpecification().isOfflinePayment(this);
    }

    /**
     * Gets the related <code>CreditCard</code> if this payment method is a credit card payment.
     * @return a <code>CreditCard</code> value
     * @exception RepositoryException if an error occurs
     */
    @Override
    public CreditCard getCreditCard() throws RepositoryException {
        if (creditCard == null && isCreditCard()) {
            creditCard = getRelatedOne(CreditCard.class);
        }
        return creditCard;
    }

    /**
     * Gets the related <code>GiftCard</code> if this payment method is a gift card payment.
     * @return a <code>GiftCard</code> value
     * @exception RepositoryException if an error occurs
     */
    @Override
    public GiftCard getGiftCard() throws RepositoryException {
        if (giftCard == null && isGiftCard()) {
            giftCard = getRelatedOne(GiftCard.class);
        }
        return giftCard;
    }

    private PaymentRepositoryInterface getRepository() throws RepositoryException {
        try {
            return PaymentRepositoryInterface.class.cast(repository);
        } catch (ClassCastException e) {
            repository = repository.getDomainsDirectory().getBillingDomain().getPaymentRepository();
            return PaymentRepositoryInterface.class.cast(repository);
        }
    }
}
