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

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.services.RecalcPaymentAmountsService;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.billing.payment.PaymentServiceInterface;
import org.opentaps.foundation.entity.util.EntityListIterator;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Implementation of the payment services.
 */
public class PaymentService extends DomainService implements PaymentServiceInterface {

    private String paymentId = null;
    private Payment payment = null;

    private PaymentRepositoryInterface paymentRepository = null;

    private static final String MODULE = PaymentService.class.getName();

    /**
     * Default constructor.
     */
    public PaymentService() {
        super();
    }

    /** {@inheritDoc} */
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    /** {@inheritDoc} */
    public void recalcPaymentAmounts() throws ServiceException {
        try {
            paymentRepository = getPaymentRepository();
            if (payment == null) {
                payment = paymentRepository.getPaymentById(paymentId);
            }
            // recalculate the fields
            payment.calculateAppliedAndOpenAmount();
            Debug.logInfo("recalcPaymentAmounts: [" + payment.getPaymentId() + "] open amount = " + payment.getOpenAmount(), MODULE);
            Debug.logInfo("recalcPaymentAmounts: [" + payment.getPaymentId() + "] applied amount = " + payment.getAppliedAmount(), MODULE);
            // persist the updated values
            paymentRepository.update(payment);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private PaymentRepositoryInterface getPaymentRepository() throws RepositoryException {
        return getDomainsDirectory().getBillingDomain().getPaymentRepository();
    }

    /** {@inheritDoc} */
    public void recalcAllEmptyAmountsPayments() throws ServiceException {
        try {
            paymentRepository = getPaymentRepository();
            EntityCondition condition = EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition(Payment.Fields.openAmount.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(Payment.Fields.appliedAmount.name(), EntityOperator.EQUALS, null));
            EntityListIterator<Payment> paymentsIt = paymentRepository.findIterator(Payment.class, condition);
            Payment payment = null;
            while ((payment = paymentsIt.next()) != null) {
                RecalcPaymentAmountsService service = new RecalcPaymentAmountsService();
                service.setInPaymentId(payment.getPaymentId());
                runSync(service);
            }

        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
