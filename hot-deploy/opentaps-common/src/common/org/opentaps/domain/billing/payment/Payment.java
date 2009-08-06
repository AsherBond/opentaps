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

import org.opentaps.domain.base.entities.PaymentApplication;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.ofbiz.base.util.UtilNumber;

import java.math.BigDecimal;
import java.util.List;

/**
 * Order Payment Preference entity.
 */
public class Payment extends org.opentaps.domain.base.entities.Payment {

    private static int DECIMALS = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int ROUNDING = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

    private PaymentMethod paymentMethod;
    private Party partyFrom;
    private Party partyTo;

    /**
     * Default constructor.
     */
    public Payment() {
        super();
    }

    /**
     * Get the specification object which contains enumerations and logical checking for Payments.
     * @return the <code>OrderSpecificationInterface</code>
     * @exception RepositoryException if an error occurs
     */
    public PaymentSpecificationInterface getPaymentSpecification() throws RepositoryException {
        return getRepository().getPaymentSpecification();
    }

    /**
     * Gets the <code>Party</code> that is sending this payment.
     * @return the <code>Party</code> that is sending this payment
     * @throws RepositoryException if an error occurs
     */
    public Party getPartyFrom() throws RepositoryException {
        if (partyFrom == null) {
            try {
                partyFrom = getRepository().getPartyById(this.getPartyIdFrom());
            } catch (EntityNotFoundException e) {
                partyFrom = null;
            }
        }
        return partyFrom;
    }

    /**
     * Gets the <code>Party</code> that is receiving this payment.
     * @return the <code>Party</code> that is receiving this payment
     * @throws RepositoryException if an error occurs
     */
    public Party getPartyTo() throws RepositoryException {
        if (partyTo == null) {
            try {
                partyTo = getRepository().getPartyById(this.getPartyIdTo());
            } catch (EntityNotFoundException e) {
                partyTo = null;
            }
        }
        return partyTo;
    }

    /**
     * Gets the amount applied from this payment.
     * @return a <code>BigDecimal</code> value
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getAppliedAmount() throws RepositoryException {
        BigDecimal appliedAmount = BigDecimal.ZERO;
        List<PaymentApplication> applications = getRelated(PaymentApplication.class);
        for (PaymentApplication application : applications) {
            appliedAmount = appliedAmount.add(application.getAmountApplied()).setScale(DECIMALS, ROUNDING);
        }
        return appliedAmount;
    }

    /**
     * Gets the organization party ID of the invoice, which is the from partyIdFrom of disbursements
     * and the partyIdTo of the other payments.
     * @return the organization party ID
     * @throws RepositoryException if an error occurs
     */
    public String getOrganizationPartyId() throws RepositoryException {
        if (isDisbursement()) {
            return getPartyIdFrom();
        } else {
            return getPartyIdTo();
        }
    }

    /**
     * Gets the transaction party ID of the payment, which is the partyIdTo of disbursements
     * and the partyIdFrom of the other payemnts.
     * @return the transaction party ID
     * @throws RepositoryException if an error occurs
     */
    public String getTransactionPartyId() throws RepositoryException {
        if (isDisbursement()) {
            return getPartyIdTo();
        } else {
            return getPartyIdFrom();
        }
    }
    /**
     * Checks if the payment is a disbursement.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isDisbursement() throws RepositoryException {
        return getRepository().isDisbursement(this);
    }
    /**
     * Checks if the payment is a receipt.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isReceipt() throws RepositoryException {
        return getRepository().isReceipt(this);
    }

    /**
     * Checks if the payment is a customer refund.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isCustomerRefund() throws RepositoryException {
        return getRepository().isCustomerRefund(this);
    }

    /**
     * Checks if the payment is a tax payment.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isTaxPayment() throws RepositoryException {
        return getRepository().isTaxPayment(this);
    }

    /**
     * Checks if the payment is a pay check.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isPayCheck() throws RepositoryException {
        return getRepository().isPayCheck(this);
    }

    /**
     * Is this payment status "cancelled".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isCancelled() throws RepositoryException {
        return getPaymentSpecification().isCancelled(this);
    }

    /**
     * Is this payment status "not paid".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isNotPaid() throws RepositoryException {
        return getPaymentSpecification().isNotPaid(this);
    }

    /**
     * Is this payment status "confirmed".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isConfirmed() throws RepositoryException {
        return getPaymentSpecification().isConfirmed(this);
    }

    /**
     * Is this payment status "voided".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isVoided() throws RepositoryException {
        return getPaymentSpecification().isVoided(this);
    }

    /**
     * Is this payment status "sent".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isSent() throws RepositoryException {
        return getPaymentSpecification().isSent(this);
    }

    /**
     * Is this payment status "received".
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isReceived() throws RepositoryException {
        return getPaymentSpecification().isReceived(this);
    }

    /**
     * Checks if the payment method is a billing account.
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isBillingAccountPayment() throws RepositoryException {
        return getPaymentSpecification().isBillingAccountPayment(this);
    }

    /**
     * Gets the <code>PaymentMethod</code> for this payment.
     * Returns the order domain object instead of the base entity.
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

    private PaymentRepositoryInterface getRepository() throws RepositoryException {
        try {
            return PaymentRepositoryInterface.class.cast(repository);
        } catch (ClassCastException e) {
            repository = repository.getDomainsDirectory().getBillingDomain().getPaymentRepository();
            return PaymentRepositoryInterface.class.cast(repository);
        }
    }
}
