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

import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.base.entities.PaymentApplication;
import org.opentaps.domain.DomainsDirectory;
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
    
    /** {@inheritDoc} */
    public PaymentApplication getPaymentApplicationById(String paymentApplicationId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(PaymentApplication.class, map(PaymentApplication.Fields.paymentApplicationId, paymentApplicationId), "PaymentApplication [" + paymentApplicationId + "] not found");
    }

    protected PartyRepositoryInterface getPartyRepository() throws RepositoryException {
        if (partyRepository == null) {
            partyRepository = DomainsDirectory.getDomainsDirectory(this).getPartyDomain().getPartyRepository();
        }
        return partyRepository;
    }

}
