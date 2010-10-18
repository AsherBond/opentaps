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
