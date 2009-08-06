/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.financials.domain.billing;

import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.financials.domain.billing.payment.PaymentRepository;
import org.opentaps.financials.domain.billing.agreement.AgreementRepository;
import org.opentaps.financials.domain.billing.invoice.InvoiceRepository;
import org.opentaps.financials.domain.billing.invoice.InvoiceService;
import org.opentaps.financials.domain.billing.invoice.OrderInvoicingService;
import org.opentaps.financials.domain.billing.lockbox.LockboxRepository;
import org.opentaps.foundation.domain.Domain;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is an implementation of the Billing domain.
 */
public class BillingDomain extends Domain implements BillingDomainInterface {

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
    public OrderInvoicingService getOrderInvoicingService() throws ServiceException {
        return instantiateService(OrderInvoicingService.class);
    }

    /** {@inheritDoc} */
    public AgreementRepository getAgreementRepository() throws RepositoryException {
        return instantiateRepository(AgreementRepository.class);
    }
}
