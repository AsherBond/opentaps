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
package org.opentaps.tests.domains;

import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.financials.domain.billing.agreement.AgreementRepository;
import org.opentaps.financials.domain.billing.invoice.InvoiceRepository;
import org.opentaps.financials.domain.billing.invoice.InvoiceService;
import org.opentaps.financials.domain.billing.invoice.OrderInvoicingService;
import org.opentaps.financials.domain.billing.lockbox.LockboxRepository;
import org.opentaps.financials.domain.billing.payment.PaymentRepository;
import org.opentaps.financials.domain.billing.payment.PaymentService;
import org.opentaps.foundation.domain.Domain;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is an implementation of the Billing domain.
 */
public class TestBillingDomain extends Domain implements BillingDomainInterface {

    /** {@inheritDoc} */
    public InvoiceRepository getInvoiceRepository() throws RepositoryException {
        return instantiateRepository(InvoiceRepository.class);
    }

    /** {@inheritDoc} */
    public PaymentRepository getPaymentRepository() throws RepositoryException {
        return instantiateRepository(PaymentRepository.class);
    }

    /** {@inheritDoc} */
    public LockboxRepository getLockboxRepository() throws RepositoryException {
        return instantiateRepository(LockboxRepository.class);
    }

    /** {@inheritDoc} */
    public InvoiceService getInvoiceService() throws ServiceException {
        return instantiateService(InvoiceService.class);
    }

    /** {@inheritDoc} */
    public PaymentService getPaymentService() throws ServiceException {
        return instantiateService(PaymentService.class);
    }

    /** {@inheritDoc} */
    public OrderInvoicingService getOrderInvoicingService() throws ServiceException {
        return instantiateService(OrderInvoicingService.class);
    }

    /** {@inheritDoc} */
    public AgreementRepository getAgreementRepository() throws RepositoryException {
        return instantiateRepository(AgreementRepository.class);
    }
}
