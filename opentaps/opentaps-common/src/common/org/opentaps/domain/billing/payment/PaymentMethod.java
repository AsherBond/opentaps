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
package org.opentaps.domain.billing.payment;

import org.opentaps.base.entities.PostalAddress;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Payment Method entity.
 */
public class PaymentMethod extends org.opentaps.base.entities.PaymentMethod {

    private CreditCard creditCard;
    private GiftCard giftCard;
    private PostalAddress postalAddress;

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
     * Gets the postal address related to this payment method.
     * Note that only Electronic Fund Transfers and Credit Cards can have a postal address.
     * @return a <code>PostalAddress</code> value
     * @exception RepositoryException if an error occurs
     */
    public PostalAddress getPostalAddress() throws RepositoryException {
        // only EFT and credit card have postal addresses
        if (postalAddress == null) {
            if (isCreditCard() && getCreditCard() != null) {
                postalAddress = getCreditCard().getPostalAddress();
            } else if (isElectronicFundTransfer() && getEftAccount() != null) {
                postalAddress = getEftAccount().getPostalAddress();
            }
        }
        return postalAddress;
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
            repository = DomainsDirectory.getDomainsDirectory(repository).getBillingDomain().getPaymentRepository();
            return PaymentRepositoryInterface.class.cast(repository);
        }
    }
}
