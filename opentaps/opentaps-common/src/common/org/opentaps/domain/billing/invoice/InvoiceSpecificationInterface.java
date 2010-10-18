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
package org.opentaps.domain.billing.invoice;

/**
 * Interface for an invoice specification.  When you add your own status codes and other
 * specification related flags and logic, extend this interface, extend the billing domain,
 * and create your own versions of the status and type enumerations.
 */
public interface InvoiceSpecificationInterface {

    /**
     * Checks whether the invoice type is a receivable.
     * For example, sales invoices to customers are receivables.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is a receivable type
     */
    public Boolean isReceivable(Invoice invoice);

    /**
     * Checks whether the invoice type is payable.
     * For example, purchase invoices are payable.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is a payable type
     */
    public Boolean isPayable(Invoice invoice);

    /**
     * Checks whether the invoice is a sales invoice.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is a sales invoice
     */
    public Boolean isSalesInvoice(Invoice invoice);

    /**
     * Checks whether the invoice is a purchase invoice.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is a purchase invoice
     */
    public Boolean isPurchaseInvoice(Invoice invoice);

    /**
     * Checks whether the invoice is a commission invoice.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is a commission invoice
     */
    public Boolean isCommissionInvoice(Invoice invoice);

    /**
     * Checks whether the invoice is an interest invoice.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is an interest invoice
     */
    public Boolean isInterestInvoice(Invoice invoice);

    /**
     * Checks whether the invoice is a return invoice.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is a return invoice
     */
    public Boolean isReturnInvoice(Invoice invoice);

    /**
     * Checks whether the invoice is a partner invoice.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is a partner invoice
     */
    public Boolean isPartnerInvoice(Invoice invoice);

    /**
     * Checks whether the invoice is approved.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is approved
     */
    public Boolean isCancelled(Invoice invoice);

    /**
     * Checks whether the invoice is approved.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is approved
     */
    public Boolean isInvoicedToPartner(Invoice invoice);

    /**
     * Checks whether the invoice is approved.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is approved
     */
    public Boolean isInProcess(Invoice invoice);

    /**
     * Checks whether the invoice is paid.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is paid
     */
    public Boolean isPaid(Invoice invoice);

    /**
     * Checks whether the invoice is ready.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is ready
     */
    public Boolean isReady(Invoice invoice);

    /**
     * Checks whether the invoice is received.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is received
     */
    public Boolean isReceived(Invoice invoice);

    /**
     * Checks whether the invoice is voided.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is voided
     */
    public Boolean isVoided(Invoice invoice);

    /**
     * Checks whether the invoice is written off.
     * @param invoice an <code>Invoice</code>
     * @return whether the invoice is written off
     */
    public Boolean isWrittenOff(Invoice invoice);
}
