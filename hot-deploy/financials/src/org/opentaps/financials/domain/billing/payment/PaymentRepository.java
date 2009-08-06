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

import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.billing.payment.PaymentSpecificationInterface;
import org.opentaps.domain.billing.payment.PaymentTypeInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/**
 * Implementation of payment repository for opentaps 1.x running on ofbiz.
 */
public class PaymentRepository extends Repository implements PaymentRepositoryInterface {

    private PartyRepositoryInterface partyRepository;
    private PaymentSpecificationInterface specification = new PaymentSpecification();

    /**
     * Default constructor.
     */
    public PaymentRepository() {
        super();
    }

    /** {@inheritDoc} */
    public PaymentSpecificationInterface getPaymentSpecification() {
        return specification;
    }

    /** {@inheritDoc} */
    public Payment getPaymentById(String paymentId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(Payment.class, map(Payment.Fields.paymentId, paymentId), "Payment [" + paymentId + "] not found");
    }

    /** {@inheritDoc} */
    public Boolean isPaymentType(Payment payment, PaymentTypeInterface paymentType) throws RepositoryException {
        try {
            return UtilAccounting.isPaymentType(Repository.genericValueFromEntity(getDelegator(), payment), paymentType.getTypeId());
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public Boolean isDisbursement(Payment payment) throws RepositoryException {
        return isPaymentType(payment, PaymentSpecification.PaymentTypeEnum.DISBURSEMENT);
    }

    /** {@inheritDoc} */
    public Boolean isReceipt(Payment payment) throws RepositoryException {
        return isPaymentType(payment, PaymentSpecification.PaymentTypeEnum.RECEIPT);
    }

    /** {@inheritDoc} */
    public Boolean isCustomerRefund(Payment payment) throws RepositoryException {
        return isPaymentType(payment, PaymentSpecification.PaymentTypeEnum.CUSTOMER_REFUND);
    }

    /** {@inheritDoc} */
    public Boolean isTaxPayment(Payment payment) throws RepositoryException {
        return isPaymentType(payment, PaymentSpecification.PaymentTypeEnum.TAX_PAYMENT);
    }

    /** {@inheritDoc} */
    public Boolean isPayCheck(Payment payment) throws RepositoryException {
        return isPaymentType(payment, PaymentSpecification.PaymentTypeEnum.PAY_CHECK);
    }

    /** {@inheritDoc} */
    public Party getPartyById(String partyId) throws RepositoryException, EntityNotFoundException {
        return getPartyRepository().getPartyById(partyId);
    }

    protected PartyRepositoryInterface getPartyRepository() throws RepositoryException {
        if (partyRepository == null) {
            partyRepository = getDomainsDirectory().getPartyDomain().getPartyRepository();
        }
        return partyRepository;
    }

}
