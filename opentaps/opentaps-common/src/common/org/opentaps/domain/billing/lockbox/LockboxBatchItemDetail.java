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

package org.opentaps.domain.billing.lockbox;

import java.math.BigDecimal;

import org.opentaps.domain.party.Party;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Object representing an Lockbox Batch.
 */
public class LockboxBatchItemDetail extends org.opentaps.base.entities.LockboxBatchItemDetail {

    private LockboxBatchItem lockboxBatchItem;
    private Party customer;
    private Invoice invoice;
    private Status status;

    /** Customer ID for no customer. */
    public static final String NO_REF_CUSTOMER_ID = "NO REF";

    /**
     * Default constructor.
     */
    public LockboxBatchItemDetail() {
        super();
    }

    /**
     * Constructor with a repository.
     * @param repository a <code>LockboxRepositoryInterface</code> value
     */
    public LockboxBatchItemDetail(LockboxRepositoryInterface repository) {
        super();
        initRepository(repository);
    }

    /**
     * Gets the parent <code>LockboxBatchItem</code> for this <code>LockboxBatchItemDetail</code>.
     * Returns the order domain object instead of the base entity.
     * @return the <code>LockboxBatchItem</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public LockboxBatchItem getLockboxBatchItem() throws RepositoryException {
        if (lockboxBatchItem == null) {
            lockboxBatchItem = getRelatedOne(LockboxBatchItem.class);
        }
        return lockboxBatchItem;
    }

    /**
     * Checks if this line has already been applied.
     * The line is considered applied if it has a <code>Payment</code>.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isApplied() throws RepositoryException {
        return getPaymentId() != null;
    }

    /**
     * Checks if this line has any error preventing an application.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean canApply() throws RepositoryException {
        return !getStatus().isError() && !isApplied();
    }

    /**
     * Checks if this line was user entered.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isUserEntered() throws RepositoryException {
        return "Y".equals(getIsUserEntered());
    }

    /**
     * Checks if this line can have its amount to apply updated.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean canUpdate() throws RepositoryException {
        return !isApplied() && (canApply() || isUserEntered() || getAmountToApply().signum() > 0 || getCashDiscount().signum() > 0);
    }

    /**
     * Represents the status of the invoice number field.
     */
    public static enum InvoiceStatus {
        /** No invoice number set. */
        NoInvoiceNumber("No Invoice #", false),
        /** No invoice number set, and no valid customer. */
        NoInvoiceNumberAndBadCustomer("No Invoice #", true),
        /** The invoice number set does not match any invoice in the system. */
        BadInvoiceNumber("Bad Invoice #", true),
        /** The corresponding invoice in the system has no open amount. */
        InvoiceClosed("Referenced Invoice is Closed", true),
        /** The corresponding invoice open amount is lesser than the amount for this line. */
        ExceedOpenAmount("Lockbox Amount Greater Than Invoice Open Amount", true),
        /** The corresponding invoice open amount is lesser than the amount for this line. */
        TotalAppliedExceedOpenAmount("Lockbox Amount Greater Than Invoice Open Amount After Applying Other Lines", true),
        /** The corresponding invoice open amount is greater than the amount for this line. */
        PartialPayment("Partial Payment", false),
        /** The corresponding invoice open amount is matching the amount for this line. */
        Ready("", false);

        private String message;
        private Boolean isError;
        private InvoiceStatus(String message, boolean isError) {
            this.message = message;
            this.isError = isError;
        }

        /**
         * Gets the description of this status.
         * @return the description of this status
         */
        public String getMessage() { return message; }

        /**
         * Checks if this status represents a critical error.
         * @return a <code>Boolean</code> value
         */
        public Boolean isError() { return isError; }
        protected String append(String str) {
            if (message.length() > 0) {
                return str + " - " + message;
            }
            return str;
        }
    }

    /**
     * Represents the status of the customer id field.
     */
    public static enum CustomerStatus {
        /** No customer id set. */
        NoCustomerId("No Customer #", false),
        /** The customer id set does not match any party in the system. */
        BadCustomerId("Bad Customer #", false),
        /** No customer id or bad customer id, and no invoice number set. */
        NoInvoiceNumberAndBadCustomer("Bad Customer #", true),
        /** The customer id set does not match the line invoice. */
        MismatchInvoice("Customer and Invoice Not Associated", true),
        /** The customer id is valid is not conflicting the invoice. */
        Ready("", false);

        private String message;
        private Boolean isError;
        private CustomerStatus(String message, boolean isError) {
            this.message = message;
            this.isError = isError;
        }

        /**
         * Gets the description of this status.
         * @return the description of this status
         */
        public String getMessage() { return message; }

        /**
         * Checks if this status represents a critical error.
         * @return a <code>Boolean</code> value
         */
        public Boolean isError() { return isError; }

        protected String append(String str) {
            if (message.length() > 0) {
                return str + " - " + message;
            }
            return str;
        }
    }

    /**
     * Combines the <code>InvoiceStatus</code> and <code>CustomerStatus</code> to represent the status of a <code>LockboxBatchItemDetail</code>.
     */
    public static class Status {
        private InvoiceStatus invoiceStatus;
        private CustomerStatus customerStatus;
        protected Status(InvoiceStatus invoiceStatus, CustomerStatus customerStatus) {
            this.invoiceStatus = invoiceStatus;
            this.customerStatus = customerStatus;
        }

        /**
         * Gets the <code>InvoiceStatus</code>.
         * @return the <code>InvoiceStatus</code>
         */
        public InvoiceStatus getInvoiceStatus() { return invoiceStatus; }

        /**
         * Gets the <code>CustomerStatus</code>.
         * @return the <code>CustomerStatus</code>
         */
        public CustomerStatus getCustomerStatus() { return customerStatus; }

        /**
         * Checks if this status represents a critical error.
         * @return a <code>Boolean</code> value
         */
        public Boolean isError() { return invoiceStatus.isError() || customerStatus.isError(); }

        /**
         * Checks that this status represents neither an error nor a noteworthy warning.
         * @return a <code>Boolean</code> value
         */
        public Boolean isReady() { return invoiceStatus.equals(InvoiceStatus.Ready) && customerStatus.equals(CustomerStatus.Ready); }

        /**
         * Checks if this status represents a warning, but do not prevent the line from being applied.
         * @return a <code>Boolean</code> value
         */
        public Boolean isNote() { return !isError() && !isReady(); }

        /**
         * Gets the description of this status.
         * @return the description of this status
         */
        public String getMessage() {
            if (isError()) {
                return invoiceStatus.append(customerStatus.append("Error"));
            } else if (isReady()) {
                return "Ready";
            } else {
                return invoiceStatus.append(customerStatus.append("Note"));
            }
        }
    }

    /**
     * Gets the status for this line if there is an error to report.
     * @return a <code>Status</code> value
     * @throws RepositoryException if an error occurs
     */
    public Status getStatus() throws RepositoryException {
        if (status == null) {
            status = new Status(getInvoiceStatus(), getCustomerStatus());
        }
        return status;
    }

    private InvoiceStatus getInvoiceStatus() throws RepositoryException {
        if (!hasInvoiceNumber()) {
            if (!hasValidCustomer()) {
                return InvoiceStatus.NoInvoiceNumberAndBadCustomer;
            } else {
                return InvoiceStatus.NoInvoiceNumber;
            }
        }
        if (!hasValidInvoice()) {
            return InvoiceStatus.BadInvoiceNumber;
        }
        BigDecimal openAmount = getInvoice().getOpenAmount();
        if (openAmount.compareTo(BigDecimal.ZERO) == 0) {
            return InvoiceStatus.InvoiceClosed;
        }
        if (openAmount.compareTo(getInvoiceAmount().add(getCashDiscount())) < 0) {
            return InvoiceStatus.ExceedOpenAmount;
        }
        if (openAmount.compareTo(getLockboxBatchItem().getLockboxBatch().getTotalAppliedToInvoice(getInvoiceNumber())) < 0) {
            return InvoiceStatus.TotalAppliedExceedOpenAmount;
        }
        if (openAmount.compareTo(getInvoiceAmount().add(getCashDiscount())) > 0) {
            return InvoiceStatus.PartialPayment;
        }
        return InvoiceStatus.Ready;
    }

    private CustomerStatus getCustomerStatus() throws RepositoryException {
        if (!hasCustomerId()) {
            return CustomerStatus.NoCustomerId;
        }
        if (!hasValidCustomer()) {
            if (!hasValidInvoice()) {
                return CustomerStatus.NoInvoiceNumberAndBadCustomer;
            } else {
                return CustomerStatus.BadCustomerId;
            }
        }
        if (hasValidInvoice() && !getInvoice().hasParty(getCustomer())) {
            return CustomerStatus.MismatchInvoice;
        }
        return CustomerStatus.Ready;
    }

    /**
     * Checks if any sort of customer id is available.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean hasCustomerId() throws RepositoryException {
        return  getCustomerId() != null && !NO_REF_CUSTOMER_ID.equals(getCustomerId()) && getCustomerId().length() > 0;
    }

    /**
     * Checks if any sort of invoice id is available.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean hasInvoiceNumber() throws RepositoryException {
        if (getInvoiceNumber() == null) {
            return false;
        }
        return getInvoiceNumber().length() > 0;
    }

    /**
     * Checks if this line has a valid customer in the system (that the customer id matches a party in the system).
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean hasValidCustomer() throws RepositoryException {
        return getCustomer() != null;
    }

    /**
     * Checks if this line has a valid invoice in the system (that the invoice number matches an invoice in the system).
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean hasValidInvoice() throws RepositoryException {
        return getInvoice() != null;
    }

    /**
     * Gets the <code>Invoice</code> total amount if one is found in the system.
     * @return the <code>Invoice</code> total amount, <code>null</code> if none is found in the system
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getErpInvoiceAmount() throws RepositoryException {
        Invoice invoice = getInvoice();

        if (invoice == null) {
            return null;
        }

        return invoice.getInvoiceTotal();
    }

    /**
     * Gets the <code>Invoice</code> open amount if one is found in the system.
     * @return the <code>Invoice</code> open amount, <code>null</code> if none is found in the system
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getErpInvoiceOpenAmount() throws RepositoryException {
        Invoice invoice = getInvoice();

        if (invoice == null) {
            return null;
        }

        return invoice.getOpenAmount();
    }

    /**
     * Gets the <code>Invoice</code> open amount if one is found in the system accounting the other <code>LockboxBatchItemDetail</code>.
     * @return the <code>Invoice</code> open amount, <code>null</code> if none is found in the system
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getErpInvoiceOpenAmountInBatch() throws RepositoryException {
        Invoice invoice = getInvoice();

        if (invoice == null) {
            return null;
        }

        return invoice.getOpenAmount().subtract(getLockboxBatchItem().getLockboxBatch().getTotalAppliedToInvoice(invoice.getInvoiceId()));
    }

    /**
     * Gets the <code>Invoice</code> related to this <code>LockboxBatchItemDetail</code>, if the invoice number match one in the system.
     * @return an <code>Invoice</code> value, or <code>null</code> if none is found matching the invoice number
     * @throws RepositoryException if an error occurs
     */
    public Invoice getInvoice() throws RepositoryException {
        if (invoice == null) {
            invoice = getRepository().getRelatedInvoice(this);
        }
        return invoice;
    }

    /**
     * Gets the <code>Party</code> related to this <code>LockboxBatchItemDetail</code>, if the customer number match one in the system.
     * @return an <code>Party</code> value, or <code>null</code> if none is found matching the customer number
     * @throws RepositoryException if an error occurs
     */
    public Party getCustomer() throws RepositoryException {
        if (customer == null) {
            customer = getRepository().getRelatedCustomer(this);
        }
        return customer;
    }

    /**
     * Gets the amount to apply.
     * @return a <code>BigDecimal</code> value, never null
     */
    @Override
    public BigDecimal getAmountToApply() {
        BigDecimal val = super.getAmountToApply();
        if (val == null) {
            return BigDecimal.ZERO;
        }
        return val;
    }

    /**
     * Gets the amount to apply to the invoice.
     * @return a <code>BigDecimal</code> value, never null
     */
    public BigDecimal getAmountToApplyToInvoice() {
        BigDecimal val = getAmountToApply();
        // negative cash discounts are applied to a GL account
        // so we have to offset by that amount
        if (getCashDiscount().signum() < 0) {
            return val.add(getCashDiscount());
        }
        // positive cash discounts are created as adjustments
        // so the value is included in this value
        return val;
    }

    /**
     * Gets the cash discount.
     * @return a <code>BigDecimal</code> value, never null
     */
    @Override
    public BigDecimal getCashDiscount() {
        BigDecimal val = super.getCashDiscount();
        if (val == null) {
            return BigDecimal.ZERO;
        }
        return val;
    }

    /**
     * Gets the total amount to apply, including both the amountToApply and cashDiscount.
     * @return a <code>BigDecimal</code> value, never null
     */
    public BigDecimal getTotal() {
        return getAmountToApply().add(getCashDiscount());
    }

    private LockboxRepositoryInterface getRepository() {
        return LockboxRepositoryInterface.class.cast(repository);
    }
}
