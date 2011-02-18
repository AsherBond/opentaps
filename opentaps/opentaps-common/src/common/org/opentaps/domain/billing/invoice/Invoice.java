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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilNumber;
import org.opentaps.base.entities.InvoiceAdjustment;
import org.opentaps.base.entities.InvoiceAndInvoiceItem;
import org.opentaps.base.entities.InvoiceItem;
import org.opentaps.base.entities.InvoiceItemType;
import org.opentaps.base.entities.Party;
import org.opentaps.base.entities.PaymentAndApplication;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.common.util.UtilDate;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.base.constants.InvoiceItemTypeConstants;

/**
 * Object representing an Invoice.  This is an Entity extension of the Invoice GenericValue and offers you object methods in addition
 * to the .get("...") fields from the Invoice GenericValue.
 *
 * Even though it extends Entity, this object is really an "Aggregate" as defined in Domain Driven Design and should be used as the "root"
 * node for all Invoice-related Entities.  In other words, instead of accessing a particular InvoiceItem or InvoiceContactMech, etc., you
 * should go through Invoice and get its items, contact mechs, attributes, etc.
 */
public class Invoice extends org.opentaps.base.entities.Invoice {

    // this is actually the logical place for these constants.  if we add get methods for them, then we are good
    private static final int DECIMALS = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static final int ROUNDING = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
    private static final int TAX_DECIMALS = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    private static final int TAX_ROUNDING = UtilNumber.getBigDecimalRoundingMode("salestax.rounding");

    protected static String VALID_ENTITY_NAME = "Invoice";      // only GenericValue of ModelEntity "Invoice" can be instantiated into this Entity object

    // null is the right initial state for these values, so we know that they have not been set yet.
    private BigDecimal salesTaxTotal;
    private List<InvoiceItem> invoiceItems;

    /**
     * Default constructor.
     */
    public Invoice() {
        super();
    }

    /**
     * Gets the list of <code>InvoiceItem</code> ordered by <code>invoiceItemSeqId</code>.
     * @return the list of <code>InvoiceItem</code> ordered by <code>invoiceItemSeqId</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<? extends InvoiceItem> getInvoiceItems() throws RepositoryException {
        if (this.invoiceItems == null) {
            this.invoiceItems = getRelated(InvoiceItem.class, "InvoiceItem", Arrays.asList(InvoiceItem.Fields.invoiceItemSeqId.name()));
        }
        return this.invoiceItems;
    }

    /**
     * Gets the aging date of the invoice, which is either the due date or the invoice date if no due date is specified.
     * @return the aging date of the invoice
     */
    public Timestamp getAgingDate() {
        if (getDueDate() != null) {
            return getDueDate();
        }
        return getInvoiceDate();
    }

    /**
     * Gets the number of days aged of this invoice from the aging date through today.
     * @return the number of days aged of this invoice from the aging date through today, 0 if the aging date is after today
     */
    public Integer getDaysAged() {
        return getDaysAged(UtilDateTime.nowTimestamp());
    }

    /**
     * Gets the number of days aged of this invoice from the aging date through the given date.
     * @param asOfDateTime a <code>Timestamp</code> value
     * @return the number of days aged of this invoice from the aging date through the given date, 0 if the aging date is after the given date
     */
    public Integer getDaysAged(Timestamp asOfDateTime) {
        if (asOfDateTime.after(getAgingDate())) {
            return UtilDate.dateDifference(getAgingDate(), asOfDateTime);
        }
        return new Integer(0);
    }

    /**
     * Gets the days outstanding of the invoice as of today.
     * @return the days outstanding of the invoice as of today
     */
    public Integer getDaysOutstanding() {
        return getDaysOutstanding(UtilDateTime.nowTimestamp());
    }

    /**
     * Gets the days outstanding of the invoice as of the specified date.  The days outstanding is based on the original invoice date.
     * @param asOfDateTime a <code>Timestamp</code> value
     * @return an <code>Integer</code> value
     */
    public Integer getDaysOutstanding(Timestamp asOfDateTime) {
        return UtilDate.dateDifference(getInvoiceDate(), asOfDateTime);
    }


    /**
     * Checks if this invoice is past due as of right now.
     * @return a <code>boolean</code> value
     */
    public boolean isPastDue() {
        return isPastDue(UtilDateTime.nowTimestamp());
    }

    /**
     * Checks if invoice is past due at the specified time.  If Invoice has no dueDate, it will never be past due.
     * @param asOfDateTime a <code>Timestamp</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isPastDue(Timestamp asOfDateTime) {
        return (getDueDate() != null) && (getDueDate().before(asOfDateTime));
    }

    private static final List<String> SALES_TAX_TYPES = Arrays.asList(InvoiceItemTypeConstants.ITM_SALES_TAX,
                                                                      InvoiceItemTypeConstants.PINV_SALES_TAX,
                                                                      InvoiceItemTypeConstants.SRT_SALES_TAX_ADJ,
                                                                      InvoiceItemTypeConstants.CRT_SALES_TAX_ADJ,
                                                                      InvoiceItemTypeConstants.INV_SALES_TAX,
                                                                      InvoiceItemTypeConstants.PITM_SALES_TAX);

    /**
     * Helpful internal method to calculate cached totals for this invoice.
     * Sets <code>invoiceTotal</code> to the sum of all <code>InvoiceItem</code> including Sales Tax items.
     * Sets <code>salesTaxTotal</code> to the sum of all Sales Tax <code>InvoiceItem</code>.
     * @throws RepositoryException if an error occurs
     */
    private void calculateTotals() throws RepositoryException {
        BigDecimal invoiceTotal = BigDecimal.ZERO;
        salesTaxTotal = BigDecimal.ZERO;

        for (InvoiceItem item : getInvoiceItems()) {
            BigDecimal amount = (item.get("amount") == null ? BigDecimal.ZERO : item.getBigDecimal("amount"));
            BigDecimal quantity = (item.get("quantity") == null ? BigDecimal.ONE : item.getBigDecimal("quantity"));
            if (SALES_TAX_TYPES.contains(item.getInvoiceItemTypeId())) {
                salesTaxTotal = salesTaxTotal.add(amount.multiply(quantity)).setScale(TAX_DECIMALS, TAX_ROUNDING);
                // round the intermediate values to one more decimal place than the final values.
                invoiceTotal = invoiceTotal.add(amount.multiply(quantity).setScale(TAX_DECIMALS, TAX_ROUNDING)).setScale(DECIMALS + 1, ROUNDING);
            } else {
                // round the intermediate values to one more decimal place than the final values.
                invoiceTotal = invoiceTotal.add(amount.multiply(quantity).setScale(DECIMALS, ROUNDING)).setScale(DECIMALS + 1, ROUNDING);
            }
        }
        invoiceTotal = invoiceTotal.setScale(DECIMALS, ROUNDING);
        setInvoiceTotal(invoiceTotal);
        setSalesTaxTotal(salesTaxTotal);
    }

    /**
     * Gets the total value of this invoice based on all the InvoiceItems.  Assume 1 for any item whose quantity or amount is null.
     * This total includes sales tax line items.
     * @return the total value
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal calculateInvoiceTotal() throws RepositoryException {
        calculateTotals();
        return getInvoiceTotal();
    }

    /**
     * Gets the total of payments applied to this invoice as of right now.
     * @return the total of payments applied
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal calculateAppliedAmount() throws RepositoryException {
        setAppliedAmount(getAppliedAmount(UtilDateTime.nowTimestamp()));
        return getAppliedAmount();
    }

    /**
     * Gets the total of payments applied to this invoice as of specified date/time.  The currency of the payments
     * are always in the currency of the invoice.  Payments are considered applied when they are received, sent or
     * otherwise in a final paid state.
     *
     * @param asOfDateTime a <code>Timestamp</code> value
     * @return the total of payments applied as of specified date/time
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getAppliedAmount(Timestamp asOfDateTime) throws RepositoryException {
        BigDecimal appliedAmount = BigDecimal.ZERO;

        List<PaymentAndApplication> applications = getRepository().getPaymentsApplied(this, asOfDateTime);
        for (PaymentAndApplication application : applications) {
            appliedAmount = appliedAmount.add(application.getAmountApplied()).setScale(DECIMALS, ROUNDING);
        }
        return appliedAmount;
    }
    /**
     * Gets the total payments applied to this invoice that are pending.  This complements the getAppliedAmount() method
     * by letting us know what payments have been applied to the invoice but have not been marked as paid.
     * @return the total of payments applied
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal calculatePendingAppliedAmount() throws RepositoryException {
        setPendingAppliedAmount(getPendingAppliedAmount(UtilDateTime.nowTimestamp()));
        return getPendingAppliedAmount();
    }

    /**
     * Gets the total payments applied to this invoice that are pending.  This complements the getAppliedAmount() method
     * by letting us know what payments have been applied to the invoice but have not been marked as paid.
     * @param asOfDateTime a <code>Timestamp</code> value
     * @return the total of applied payments pending as of specified date/time
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getPendingAppliedAmount(Timestamp asOfDateTime) throws RepositoryException {
        BigDecimal appliedAmount = BigDecimal.ZERO; // this unfortunately can't be memorized like invoiceTotal due to the parametrization on asOfDateTime

        List<PaymentAndApplication> applications = getRepository().getPendingPaymentsApplied(this, asOfDateTime);
        for (PaymentAndApplication application : applications) {
            appliedAmount = appliedAmount.add(application.getAmountApplied()).setScale(DECIMALS, ROUNDING);
        }
        return appliedAmount;
    }

    /**
     * Gets the adjusted amount applied to this invoice as of now.
     * @return the adjusted amount
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal calculateAdjustedAmount() throws RepositoryException {
        setAdjustedAmount(getAdjustedAmount(UtilDateTime.nowTimestamp()));
        return getAdjustedAmount();
    }

    /**
     * Gets the total of adjustments applied to this invoice as of a specified date/time.
     *
     * Note that adjustments are not considered part of an invoice total.  Instead, they behave like payments on the
     * invoice and are used for discounts such as early payment bonuses and fees such as late payment penalties.
     * A positive adjustment counts towards paying the invoice, a negative adjustment increases the amount owed.
     *
     * @param asOfDateTime a <code>Timestamp</code> value
     * @return the adjusted amount
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getAdjustedAmount(Timestamp asOfDateTime) throws RepositoryException {
        BigDecimal adjustedAmount = BigDecimal.ZERO; // this unfortunately can't be memorized like invoiceTotal due to the parametrization on asOfDateTime

        List<InvoiceAdjustment> adjustments = getRepository().getAdjustmentsApplied(this, asOfDateTime);
        for (InvoiceAdjustment adjustment : adjustments) {
            adjustedAmount = adjustedAmount.add(adjustment.getAmount().setScale(DECIMALS, ROUNDING));
        }
        return adjustedAmount;
    }

    /**
     * Gets the unapplied or open amount as of right now, which is the invoice adjusted total minus the payments made.
     * This represents the actual amount that must be paid to close the invoice.
     *
     * @return the invoice total plus adjustments, minus applied amounts
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal calculateOpenAmount() throws RepositoryException {
        setOpenAmount(getOpenAmount(UtilDateTime.nowTimestamp()));
        return getOpenAmount();
    }

    /**
     * Gets the open amount as of a specified date/time, which is the invoice adjusted total minus the payments made.
     * This represents the actual amount that must be paid to close the invoice.
     *
     * @param asOfDateTime a <code>Timestamp</code> value
     * @return the invoice total plus adjustments, minus applied amounts
     * @throws RepositoryException if an error occurs
     * @see #getPendingOpenAmount(Timestamp)
     */
    public BigDecimal getOpenAmount(Timestamp asOfDateTime) throws RepositoryException {
        return getInvoiceAdjustedTotal(asOfDateTime).subtract(getAppliedAmount(asOfDateTime));
    }

    /**
     * Gets the pending open amount as of right now, which is the invoice adjusted total minus the payments made minus the pending payments.
     *
     * @return the invoice total plus adjustments, minus pending and applied amounts
     * @see #getOpenAmount
     * @see #getPendingOpenAmount(Timestamp)
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal calculatePendingOpenAmount() throws RepositoryException {
        setPendingOpenAmount(getPendingOpenAmount(UtilDateTime.nowTimestamp()));
        return getPendingOpenAmount();
    }

    /**
     * Gets the pending open amount as of a specified date/time, which is the invoice adjusted total minus the payments made
     * and minus the pending applied payments.  This represents the actual amount that must be paid to close the invoice
     * less the payments that are on file but not yet received/paid.
     *
     * @param asOfDateTime a <code>Timestamp</code> value
     * @return the invoice total plus adjustments, minus pending and applied amounts
     * @throws RepositoryException if an error occurs
     * @see #getOpenAmount(Timestamp)
     * @see #getPendingOpenAmount
     */
    public BigDecimal getPendingOpenAmount(Timestamp asOfDateTime) throws RepositoryException {
        return getOpenAmount().subtract(getPendingAppliedAmount(asOfDateTime));
    }

    /**
     * Get the invoice total plus the adjustments.  This amount represents the total that must be paid to close the
     * invoice as if there were no payments made.  For example, if the invoice were $10 and the billed party gets a $2
     * discount (-2) for paying early by agreement, then this method will return $8.  (If a payment of $4 is made, this
     * method still returns $8 since it is the total before/without payments.)
     *
     * @param asOfDateTime a <code>Timestamp</code> value
     * @return Invoice total plus adjustments
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getInvoiceAdjustedTotal(Timestamp asOfDateTime) throws RepositoryException {
        return getInvoiceTotal().add(getAdjustedAmount(asOfDateTime));
    }

    /**
     * Gets the invoice total with adjustments as of right now.
     * @return Invoice total plus adjustments
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal calculateInvoiceAdjustedTotal() throws RepositoryException {
        setInvoiceAdjustedTotal(getInvoiceAdjustedTotal(UtilDateTime.nowTimestamp()));
        return getInvoiceAdjustedTotal();
    }

    /**
     * Gets the total interest charges assessed against this invoice already.
     * @return the total interest charges
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal calculateInterestCharged() throws RepositoryException {
        BigDecimal interestCharged = BigDecimal.ZERO;
        List<InvoiceAndInvoiceItem> interestInvoiceItems = getRepository().getRelatedInterestInvoiceItems(this);
        if (interestInvoiceItems != null) {
            for (InvoiceAndInvoiceItem interestInvoiceItem : interestInvoiceItems) {
                if (interestInvoiceItem.getItemAmount() != null) {
                    interestCharged = interestCharged.add(interestInvoiceItem.getItemAmount().setScale(DECIMALS, ROUNDING));
                }
            }
        }
        setInterestCharged(interestCharged);
        return getInterestCharged();
    }

    /**
     * Returns the shipping address of the invoice, if it has one.
     * @return the shipping address
     * @throws RepositoryException if an error occurs
     */
    public PostalAddress getShippingAddress() throws RepositoryException {
        return getRepository().getShippingAddress(this);
    }

    /**
     * Returns billing address of this invoice.
     * @return the billing address
     * @throws RepositoryException if an error occurs
     */
    public PostalAddress getBillingAddress() throws RepositoryException {
        return getRepository().getBillingAddress(this);
    }

    /**
     * Sets the shipping address of this invoice.
     * @param address the <code>PostalAddress</code> to set as shipping address
     * @throws RepositoryException if an error occurs
     */
    public void setShippingAddress(PostalAddress address) throws RepositoryException {
        getRepository().setShippingAddress(this, address);
    }

    /**
     * Sets the billing address of this invoice.
     * @param address the <code>PostalAddress</code> to set as billing address
     * @throws RepositoryException if an error occurs
     */
    public void setBillingAddress(PostalAddress address) throws RepositoryException {
        getRepository().setBillingAddress(this, address);
    }

    /**
     * Gets the list of <code>InvoiceItemType</code> that are applicable for this invoice.
     * @return list of applicable <code>InvoiceItemType</code>
     * @throws RepositoryException if an error occurs
     */
    public List<InvoiceItemType> getApplicableInvoiceItemTypes() throws RepositoryException {
        return getRepository().getApplicableInvoiceItemTypes(this);
    }

    /**
     * Checks if the given <code>Party</code> is involved in this invoice.
     * @param party a <code>Party</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean hasParty(Party party) {
        return party.getPartyId().equals(getPartyId()) || party.getPartyId().equals(getPartyIdFrom());
    }

    /**
     * Gets the organization party ID of the invoice, which is the from party of outbound (ie, sales) invoices
     * and the partyId of the other invoices.
     * Note: on a partner invoice this returns the partner party ID.
     * @return the organization party ID
     */
    public String getOrganizationPartyId() {
        if (getRepository().getInvoiceSpecification().isReceivable(this)) {
            return getPartyIdFrom();
        } else {
            return getPartyId();
        }
    }

    /**
     * Gets the transaction party ID of the invoice, which is the partyId of outbound (ie, sales) invoices
     * and the fromPartyId of the other invoices.
     * @return the transaction party ID
     */
    public String getTransactionPartyId() {
        if (getRepository().getInvoiceSpecification().isReceivable(this)) {
            return getPartyId();
        } else {
            return getPartyIdFrom();
        }
    }

    /**
     * Checks whether the invoice is a sales invoice.
     * @return whether the invoice is a sales invoice
     */
    public Boolean isSalesInvoice() {
        return getRepository().getInvoiceSpecification().isSalesInvoice(this);
    }

    /**
     * Checks whether the invoice is a purchase invoice.
     * @return whether the invoice is a purchase invoice
     */
    public Boolean isPurchaseInvoice() {
        return getRepository().getInvoiceSpecification().isPurchaseInvoice(this);
    }

    /**
     * Checks whether the invoice is a return invoice.
     * @return whether the invoice is a return invoice
     */
    public Boolean isReturnInvoice() {
        return getRepository().getInvoiceSpecification().isReturnInvoice(this);
    }

    /**
     * Checks whether the invoice is a commission invoice.
     * @return whether the invoice is a commission invoice
     */
    public Boolean isCommissionInvoice() {
        return getRepository().getInvoiceSpecification().isCommissionInvoice(this);
    }

    /**
     * Checks whether the invoice is an interest invoice.
     * @return whether the invoice is an interest invoice
     */
    public Boolean isInterestInvoice() {
        return getRepository().getInvoiceSpecification().isInterestInvoice(this);
    }

    /**
     * Checks whether the invoice is a partner invoice.
     * @return whether the invoice is a partner invoice
     */
    public Boolean isPartnerInvoice() {
        return getRepository().getInvoiceSpecification().isPartnerInvoice(this);
    }

    /**
     * Checks whether the invoice type is a receivable.
     * For example, sales invoices to customers are receivables.
     * @return whether the invoice is a receivable type
     */
    public Boolean isReceivable() {
        return getRepository().getInvoiceSpecification().isReceivable(this);
    }

    /**
     * Checks whether the invoice type is payable.
     * For example, purchase invoices are payable.
     * @return whether the invoice is a payable type
     */
    public Boolean isPayable() {
        return getRepository().getInvoiceSpecification().isPayable(this);
    }

    /**
     * Checks whether the invoice is ready.
     * @return whether the invoice is ready
     */
    public Boolean isReady() {
        return getRepository().getInvoiceSpecification().isReady(this);
    }

    /**
     * Checks whether the invoice is cancelled.
     * @return whether the invoice is cancelled
     */
    public Boolean isCancelled() {
        return getRepository().getInvoiceSpecification().isCancelled(this);
    }

    /**
     * Checks whether the invoice is in process.
     * @return whether the invoice is in process
     */
    public Boolean isInProcess() {
        return getRepository().getInvoiceSpecification().isInProcess(this);
    }

    /**
     * Checks whether the invoice is written off.
     * @return whether the invoice is written off
     */
    public Boolean isWrittenOff() {
        return getRepository().getInvoiceSpecification().isWrittenOff(this);
    }

    /**
     * Checks whether the invoice is voided.
     * @return whether the invoice is voided
     */
    public Boolean isVoided() {
        return getRepository().getInvoiceSpecification().isVoided(this);
    }

    /**
     * Checks whether the invoice is paid.
     * @return whether the invoice is paid
     */
    public Boolean isPaid() {
        return getRepository().getInvoiceSpecification().isPaid(this);
    }

    /**
     * Checks whether we can modify the invoice header, line items and terms.
     * Items can only be modified when the invoice is in a processing state.
     * @return Whether invoice items and terms can be added, updated or removed.
     */
    public Boolean isModifiable() {
        return isInProcess();
    }

    /**
     * Checks whether adjustments can be made to the invoice.  This can only be
     * done after the invoice is ready and posted.
     * @return Whether the invoice can be adjusted.
     */
    public Boolean isAdjustable() {
        return isReady();
    }

    private InvoiceRepositoryInterface getRepository() {
        return InvoiceRepositoryInterface.class.cast(repository);
    }
}
