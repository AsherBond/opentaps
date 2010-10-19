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
package org.opentaps.financials.domain.billing.invoice;

import org.opentaps.base.constants.InvoiceTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceSpecificationInterface;

/**
 * Common specifications for the Invoice domain object.
 *
 * These specifications contain mapping of conceptual status to their database equivalents, as well as groupings into higher level concepts.
 *
 * This class may be expanded to include other invoice domain validation code.
 */
public class InvoiceSpecification implements InvoiceSpecificationInterface {

    /**
     * Enumeration representing the different types of invoices.
     * This particular implementation represents the supported
     * types in opentaps 1.0.  If you add your own type, create
     * a new implementation of InvoiceSpecificationInterface with
     * the InvoiceTypeEnum that includes your new type.  Then
     * ensure your InvoiceRepository returns this specification.
     */
    public static enum InvoiceTypeEnum {

        SALES(InvoiceTypeConstants.SALES_INVOICE),
        PURCHASE(InvoiceTypeConstants.PURCHASE_INVOICE),
        COMMISSION(InvoiceTypeConstants.COMMISSION_INVOICE),
        INTEREST(InvoiceTypeConstants.INTEREST_INVOICE),
        RETURN(InvoiceTypeConstants.CUST_RTN_INVOICE),
        PARTNER(InvoiceTypeConstants.PARTNER_INVOICE);

        private final String typeId;
        private InvoiceTypeEnum(String typeId) {
            this.typeId = typeId;
        }

        /**
         * Gets the corresponding type id.
         * @return the type
         */
        public String getTypeId() {
            return typeId;
        }

        /**
         * Checks that the invoice type is equal to the string from invoice.getInvoiceTypeId().
         * @param typeId the status to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String typeId) {
            return this.typeId.equals(typeId);
        }

        /**
         * Checks that the given status is of type Receivable.
         * It's better to put this check in the enum so it can be used outside of the Invoice domain.
         * @param typeId the status to check for
         * @return a <code>boolean</code>
         */
        public static boolean isReceivable(String typeId) {
            if (SALES.equals(typeId)) return true;
            if (INTEREST.equals(typeId)) return true;
            if (PARTNER.equals(typeId)) return true;
            return false;
        }

        /**
         * Checks that the given status is of type Payable.
         * It's better to put this check in the enum so it can be used outside of the Invoice domain.
         * @param typeId the status to check for
         * @return a <code>boolean</code>
         */
        public static boolean isPayable(String typeId) {
            if (PURCHASE.equals(typeId)) return true;
            if (COMMISSION.equals(typeId)) return true;
            if (RETURN.equals(typeId)) return true;
            return false;
        }
    }

    /** {@inheritDoc} **/
    public Boolean isReceivable(Invoice invoice) {
        return InvoiceTypeEnum.isReceivable(invoice.getInvoiceTypeId());
    }

    /** {@inheritDoc} **/
    public Boolean isPayable(Invoice invoice) {
        return InvoiceTypeEnum.isPayable(invoice.getInvoiceTypeId());
    }

    /** {@inheritDoc} **/
    public Boolean isSalesInvoice(Invoice invoice) {
        return InvoiceTypeEnum.SALES.equals(invoice.getInvoiceTypeId());
    }

    /** {@inheritDoc} **/
    public Boolean isPurchaseInvoice(Invoice invoice) {
        return InvoiceTypeEnum.PURCHASE.equals(invoice.getInvoiceTypeId());
    }

    /** {@inheritDoc} **/
    public Boolean isCommissionInvoice(Invoice invoice) {
        return InvoiceTypeEnum.COMMISSION.equals(invoice.getInvoiceTypeId());
    }

    /** {@inheritDoc} **/
    public Boolean isInterestInvoice(Invoice invoice) {
        return InvoiceTypeEnum.INTEREST.equals(invoice.getInvoiceTypeId());
    }

    /** {@inheritDoc} **/
    public Boolean isReturnInvoice(Invoice invoice) {
        return InvoiceTypeEnum.RETURN.equals(invoice.getInvoiceTypeId());
    }

    /** {@inheritDoc} **/
    public Boolean isPartnerInvoice(Invoice invoice) {
        return InvoiceTypeEnum.PARTNER.equals(invoice.getInvoiceTypeId());
    }

    /**
     * Enumeration representing the different statuses of invoices.
     */
    public static enum InvoiceStatusEnum {

        CANCELLED(StatusItemConstants.InvoiceStatus.INVOICE_CANCELLED),
        TO_PARTNER(StatusItemConstants.InvoiceStatus.INVOICE_INV_PTNR),
        IN_PROCESS(StatusItemConstants.InvoiceStatus.INVOICE_IN_PROCESS),
        PAID(StatusItemConstants.InvoiceStatus.INVOICE_PAID),
        READY(StatusItemConstants.InvoiceStatus.INVOICE_READY),
        RECEIVED(StatusItemConstants.InvoiceStatus.INVOICE_RECEIVED),
        VOIDED(StatusItemConstants.InvoiceStatus.INVOICE_VOIDED),
        WRITE_OFF(StatusItemConstants.InvoiceStatus.INVOICE_WRITEOFF);

        private final String statusId;
        private InvoiceStatusEnum(String statusId) {
            this.statusId = statusId;
        }

        /**
         * Gets the status ID string for this invoice status.
         * @return the status ID
         */
        public String getStatusId() {
            return statusId;
        }

        /**
         * Checks that the invoice status is equal to the string from invoice.getStatusId().
         * @param statusId the status to check for
         * @return a <code>boolean</code>
         */
        public boolean equals(String statusId) {
            return this.statusId.equals(statusId);
        }
    }

    /** {@inheritDoc} **/
    public Boolean isCancelled(Invoice invoice) {
        return InvoiceStatusEnum.CANCELLED.equals(invoice.getStatusId());
    }

    /** {@inheritDoc} **/
    public Boolean isInvoicedToPartner(Invoice invoice) {
        return InvoiceStatusEnum.TO_PARTNER.equals(invoice.getStatusId());
    }

    /** {@inheritDoc} **/
    public Boolean isInProcess(Invoice invoice) {
        return InvoiceStatusEnum.IN_PROCESS.equals(invoice.getStatusId());
    }

    /** {@inheritDoc} **/
    public Boolean isPaid(Invoice invoice) {
        return InvoiceStatusEnum.PAID.equals(invoice.getStatusId());
    }

    /** {@inheritDoc} **/
    public Boolean isReady(Invoice invoice) {
        return InvoiceStatusEnum.READY.equals(invoice.getStatusId());
    }

    /** {@inheritDoc} **/
    public Boolean isReceived(Invoice invoice) {
        return InvoiceStatusEnum.RECEIVED.equals(invoice.getStatusId());
    }

    /** {@inheritDoc} **/
    public Boolean isVoided(Invoice invoice) {
        return InvoiceStatusEnum.VOIDED.equals(invoice.getStatusId());
    }

    /** {@inheritDoc} **/
    public Boolean isWrittenOff(Invoice invoice) {
        return InvoiceStatusEnum.WRITE_OFF.equals(invoice.getStatusId());
    }
}
