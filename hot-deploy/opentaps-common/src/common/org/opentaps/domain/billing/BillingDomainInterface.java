/*
 * Copyright (c) 2007-2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.billing;

import org.opentaps.domain.billing.agreement.AgreementRepositoryInterface;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.invoice.InvoiceServiceInterface;
import org.opentaps.domain.billing.invoice.OrderInvoicingServiceInterface;
import org.opentaps.domain.billing.lockbox.LockboxRepositoryInterface;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is the interface of the Billing domain which handles invoices, payments, AR/AP, and so forth.
 */
public interface BillingDomainInterface extends DomainInterface {

    /**
     * Returns the invoice repository instance.
     * @return a <code>InvoiceRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public InvoiceRepositoryInterface getInvoiceRepository() throws RepositoryException;

    /**
     * Returns the payment repository instance.
     * @return a <code>PaymentRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public PaymentRepositoryInterface getPaymentRepository() throws RepositoryException;

    /**
     * Returns the invoice service instance.
     * @return an <code>InvoiceServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public InvoiceServiceInterface getInvoiceService() throws ServiceException;

    /**
     * Returns the order invoicing service instance.
     * @return an <code>OrderInvoicingServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public OrderInvoicingServiceInterface getOrderInvoicingService() throws ServiceException;

    /**
     * Returns the lockbox repository instance.
     * @return a <code>LockboxRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public LockboxRepositoryInterface getLockboxRepository() throws RepositoryException;

    /**
     * Returns the agreement repository instance.
     * @return a <code>AgreementRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public AgreementRepositoryInterface getAgreementRepository() throws RepositoryException;

}
