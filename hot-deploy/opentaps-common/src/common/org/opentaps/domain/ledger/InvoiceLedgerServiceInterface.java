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
package org.opentaps.domain.ledger;

import org.opentaps.foundation.service.ServiceInterface;
import org.opentaps.foundation.service.ServiceException;

import java.math.BigDecimal;

/**
 * Interface for invoice ledger services.
 */
public interface InvoiceLedgerServiceInterface extends ServiceInterface {

    /**
     * Sets the invoice ID, required parameter for {@link #postInvoiceWriteoffToGl}.
     * @param invoiceId the invoice ID
     */
    public void setInvoiceId(String invoiceId);

    /**
     * Sets the invoiceAdjustmentId, required parameter for {@link #postInvoiceAdjustmentToLedger}
     * @param invoiceAdjustmentId
     */
    public void setInvoiceAdjustmentId(String invoiceAdjustmentId);

    /**
     * Posts an InvoiceAdjustment to the ledger based on configuration in the
     * InvoiceAdjustmentGlAccount entity.
     * @throws ServiceException if an error occurs
     * @see #postInvoiceWriteoffToGl
     */
    public void postInvoiceAdjustmentToLedger() throws ServiceException;

    /**
     * Creates an adjustment for the full outstanding amount of an invoice as a writeoff and post it to the GL using createAndPostAdjustmentToInvoice.
     * @throws ServiceException if an error occurs
     * @see #postInvoiceAdjustmentToLedger
     * @see #setInvoiceId required input <code>invoiceId</code>
     */
    public void postInvoiceWriteoffToGl() throws ServiceException;

}
