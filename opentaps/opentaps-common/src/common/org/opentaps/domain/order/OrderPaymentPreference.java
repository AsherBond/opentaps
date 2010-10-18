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

import java.util.List;

import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.base.entities.StatusItem;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentGatewayResponse;
import org.opentaps.domain.billing.payment.PaymentMethod;

/**
 * Order Payment Preference entity.
 */
public class OrderPaymentPreference extends org.opentaps.base.entities.OrderPaymentPreference {

    private PaymentMethod paymentMethod;
    private List<PaymentGatewayResponse> responses;
    private List<Payment> payments;

    /**
     * Default constructor.
     */
    public OrderPaymentPreference() {
        super();
    }

    /**
     * Get the specification object which contains enumerations and logical checking for Orders.
     * @return the <code>OrderSpecificationInterface</code>
     */
    public OrderSpecificationInterface getOrderSpecification() {
        return getRepository().getOrderSpecification();
    }

    /**
     * Is this order payment preference status "cancelled".
     * @return a <code>Boolean</code> value
     */
    public Boolean isCancelled() {
        return getOrderSpecification().isCancelled(this);
    }

    /**
     * Is this order payment preference status "settled".
     * @return a <code>Boolean</code> value
     */
    public Boolean isSettled() {
        return getOrderSpecification().isSettled(this);
    }

    /**
     * Is this order payment preference status "authorized".
     * @return a <code>Boolean</code> value
     */
    public Boolean isAuthorized() {
        return getOrderSpecification().isAuthorized(this);
    }

    /**
     * Is this order payment preference status "declined".
     * @return a <code>Boolean</code> value
     */
    public Boolean isDeclined() {
        return getOrderSpecification().isDeclined(this);
    }

    /**
     * Is this order payment preference status "received".
     * @return a <code>Boolean</code> value
     */
    public Boolean isReceived() {
        return getOrderSpecification().isReceived(this);
    }

    /**
     * Is this order payment preference type "billing account".
     * @return a <code>Boolean</code> value
     */
    public Boolean isBillingAccountPayment() throws RepositoryException {
        return getOrderSpecification().isBillingAccountPayment(this);
    }

    /**
     * Is this order payment preference type "cash on delivery".
     * @return a <code>Boolean</code> value
     */
    public Boolean isCashOnDeliveryPayment() {
        return getOrderSpecification().isCashOnDeliveryPayment(this);
    }

    /**
     * Is this order payment preference type "credit card".
     * @return a <code>Boolean</code> value
     */
    public Boolean isCreditCardPayment() {
        return getOrderSpecification().isCreditCardPayment(this);
    }

    /**
     * Is this order payment preference type "gift card".
     * @return a <code>Boolean</code> value
     */
    public Boolean isGiftCardPayment() {
        return getOrderSpecification().isGiftCardPayment(this);
    }

    /**
     * Is this order payment preference type "electronic fund transfer".
     * @return a <code>Boolean</code> value
     */
    public Boolean isElectronicFundTransferPayment() {
        return getOrderSpecification().isElectronicFundTransferPayment(this);
    }

    /**
     * Is this order payment preference type "paypal".
     * @return a <code>Boolean</code> value
     */
    public Boolean isPayPalPayment() {
        return getOrderSpecification().isPayPalPayment(this);
    }

    /**
     * Is this order payment preference type "offline".
     * @return a <code>Boolean</code> value
     */
    public Boolean isOfflinePayment() {
        return getOrderSpecification().isOfflinePayment(this);
    }

    /**
     * Gets this order payment preference current <code>StatusItem</code>.
     * This is an alias for {@link org.opentaps.base.entities.OrderPaymentPreference#getStatusItem}.
     * @return the current <code>StatusItem</code>
     * @throws RepositoryException if an error occurs
     */
    public StatusItem getStatus() throws RepositoryException {
        return this.getStatusItem();
    }

    /**
     * Gets the list of <code>Payment</code> for this order payment preference.
     * Returns the order domain object instead of the base entity.
     * @return the list of <code>Payment</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<Payment> getPayments() throws RepositoryException {
        if (payments == null) {
            payments = getRelated(Payment.class, "Payment");
        }
        return payments;
    }

    /**
     * Gets the <code>PaymentMethod</code> of this order payment preference.
     * Returns the order domain object instead of the base entity.
     *
     * WARNING:  This object acts like a pseudo payment.  It may or may not have
     * a PaymentMethod associated with it.  If you want to check whether this
     * preference is a certain PaymentMethodType, do not determine it by using
     * this method to fetch a PaymentMethod and testing the type of that PaymentMethod.
     * Instead, use the class methods of this object, such as isBillingAccount().
     * @return the <code>PaymentMethod</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public PaymentMethod getPaymentMethod() throws RepositoryException {
        if (paymentMethod == null) {
            paymentMethod = getRelatedOne(PaymentMethod.class, "PaymentMethod");
        }
        return paymentMethod;
    }

    /**
     * Gets the list of <code>PaymentGatewayResponse</code> for this order payment preference.
     * Returns the order domain object instead of the base entity.
     * @return the list of <code>PaymentGatewayResponse</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<PaymentGatewayResponse> getPaymentGatewayResponses() throws RepositoryException {
        if (responses == null) {
            responses = getRelated(PaymentGatewayResponse.class, "PaymentGatewayResponse");
        }
        return responses;
    }

    /**
     * Gets the list of <code>PaymentGatewayResponse</code> for this order payment preference.
     * This is an alias for {@link #getPaymentGatewayResponses}.
     * @return the list of <code>PaymentGatewayResponse</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PaymentGatewayResponse> getResponses() throws RepositoryException {
        return this.getPaymentGatewayResponses();
    }

    /**
     * Gets the list of captured <code>PaymentGatewayResponse</code> for this order payment preference.
     * @return the list of captured <code>PaymentGatewayResponse</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PaymentGatewayResponse> getCapturedPaymentResponses() throws RepositoryException {
        return getRepository().getRelatedPaymentGatewayResponse(this, "PGT_CAPTURE");
    }

    /**
     * Gets the list of refunded <code>PaymentGatewayResponse</code> for this order payment preference.
     * @return the list of refunded <code>PaymentGatewayResponse</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PaymentGatewayResponse> getRefundedPaymentResponses() throws RepositoryException {
        return getRepository().getRelatedPaymentGatewayResponse(this, "PGT_REFUND");
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
