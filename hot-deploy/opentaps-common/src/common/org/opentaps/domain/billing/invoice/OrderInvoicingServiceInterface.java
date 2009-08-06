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

package org.opentaps.domain.billing.invoice;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;

/**
 * POJO service which creates invoices from orders using the opentaps Service foundation class.
 */
public interface OrderInvoicingServiceInterface extends ServiceInterface  {

    /**
     * Sets the required input parameter for service {@link #invoiceNonPhysicalOrderItems}.
     * @param orderId the order ID to invoice items for
     */
    public void setOrderId(String orderId);

    /**
     * Sets the status id of non-physical order items to be invoiced by {@link #invoiceNonPhysicalOrderItems}, or <code>OrderSpecification.OrderItemStatusEnum.PERFORMED</code> will be used.
     * @param statusId the status of order items to invoice for
     */
    public void setOrderItemStatusId(String statusId);

    /**
     * Gets the invoice ID created by the service {@link #invoiceNonPhysicalOrderItems}.
     * @return the invoice ID
     */
    public String getInvoiceId();

    /**
     * Creates an invoice for non physical order items of the given order.
     * The invoice will only consider the order items which status match the given status ID.
     * @throws ServiceException if an error occurs
     * @see #setOrderId
     * @see #setOrderItemStatusId
     * @see #getInvoiceId
     */
    public void invoiceNonPhysicalOrderItems() throws ServiceException;
}
