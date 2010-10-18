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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.InvoiceAdjustment;
import org.opentaps.base.entities.InvoiceItem;
import org.opentaps.base.entities.OrderItem;
import org.opentaps.base.entities.OrderItemBilling;
import org.opentaps.base.entities.PaymentApplication;
import org.opentaps.base.services.RecalcInvoiceAmountsService;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.invoice.InvoiceServiceInterface;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.ledger.InvoiceLedgerServiceInterface;
import org.opentaps.domain.ledger.LedgerSpecificationInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.util.EntityListIterator;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/** {@inheritDoc} */
public class InvoiceService extends DomainService implements InvoiceServiceInterface {

    private String invoiceId = null;
    protected String paymentId;
    protected String invoiceAdjustmentTypeId;
    protected BigDecimal adjustmentAmount;
    protected String comment;
    protected String invoiceAdjustmentId;

    private InvoiceRepositoryInterface invoiceRepository = null;
    private Invoice invoice = null;

    private static final String MODULE = InvoiceService.class.getName();

    /**
     * Default constructor.
     */
    public InvoiceService() {
        super();
    }

    /** {@inheritDoc} */
    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    /** {@inheritDoc} */
    public void setInvoiceAdjustmentTypeId(String invoiceAdjustmentTypeId) {
        this.invoiceAdjustmentTypeId = invoiceAdjustmentTypeId;
    }

    /** {@inheritDoc} */
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    /** {@inheritDoc} */
    public void setAdjustmentAmount(BigDecimal adjustmentAmount) {
        this.adjustmentAmount = adjustmentAmount;
    }

    /** {@inheritDoc} */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /** {@inheritDoc} */
    public String getInvoiceAdjustmentId() {
        return this.invoiceAdjustmentId;
    }

    /** {@inheritDoc} */
    public void createInvoiceAdjustment() throws ServiceException {
        try {
            invoiceRepository = getInvoiceRepository();

            // if the invoice is not set, such as by postInvoiceWriteoffToGl already, then find the invoice or throw an exception
            if (invoice == null) {
                invoice = invoiceRepository.getInvoiceById(invoiceId);
            }

            // store the adjustment
            InvoiceAdjustment invoiceAdjustment = new InvoiceAdjustment();
            invoiceAdjustmentId = invoiceRepository.getNextSeqId(invoiceAdjustment);
            invoiceAdjustment.setInvoiceId(invoiceId);
            invoiceAdjustment.setInvoiceAdjustmentTypeId(invoiceAdjustmentTypeId);
            invoiceAdjustment.setPaymentId(paymentId);
            invoiceAdjustment.setAmount(adjustmentAmount);
            invoiceAdjustment.setCreatedByUserLogin(getUser().getUserId());
            invoiceAdjustment.setComment(comment);
            invoiceAdjustment.setInvoiceAdjustmentId(invoiceAdjustmentId);
            invoiceAdjustment.setEffectiveDate(UtilDateTime.nowTimestamp());
            invoiceRepository.createOrUpdate(invoiceAdjustment);

            // if the invoice is already posted, then we need to post the adjustment as well
            if (getLedgerSpecification().isPosted(invoice)) {
                InvoiceLedgerServiceInterface invoiceLedgerService = getInvoiceLedgerService();
                invoiceLedgerService.setInvoiceAdjustmentId(invoiceAdjustmentId);
                invoiceLedgerService.postInvoiceAdjustmentToLedger();
            }

            // finally check if the invoice is paid TODO make check more robust with specification use, also use status valid change
            // TODO should this be in the isPosted() block above?
            if (!invoice.isWrittenOff()) {
                // force a reload as the calculated fields changed
                invoice = null;
                checkInvoicePaid();
            }

        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void checkInvoicePaid() throws ServiceException {
        try {
            invoiceRepository = getInvoiceRepository();
            if (invoice == null) {
                invoice = invoiceRepository.getInvoiceById(invoiceId);
            }
            if (invoice.isModifiable()) {
                Debug.logWarning("Invoice [" + invoice.getInvoiceId() + "] is still modifiable with status [" + invoice.getStatusId() + "] so cannot be set to PAID", MODULE);
            } else {
                // if the outstanding balance is now zero, set it as PAID
                if (invoice.getOpenAmount().signum() == 0) {
                    invoiceRepository.setPaid(invoice);
                }
            }
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void setAccountingTags() throws ServiceException {
        try {
            invoiceRepository = getInvoiceRepository();
            if (invoice == null) {
                invoice = invoiceRepository.getInvoiceById(invoiceId);
            }
            List<? extends OrderItemBilling> billings = invoice.getOrderItemBillings();
            for (OrderItemBilling billing : billings) {
                InvoiceItem invoiceItem = billing.getInvoiceItem();
                OrderItem orderItem = billing.getOrderItem();
                invoiceItem.setAcctgTagEnumId1(orderItem.getAcctgTagEnumId1());
                invoiceItem.setAcctgTagEnumId2(orderItem.getAcctgTagEnumId2());
                invoiceItem.setAcctgTagEnumId3(orderItem.getAcctgTagEnumId3());
                invoiceItem.setAcctgTagEnumId4(orderItem.getAcctgTagEnumId4());
                invoiceItem.setAcctgTagEnumId5(orderItem.getAcctgTagEnumId5());
                invoiceItem.setAcctgTagEnumId6(orderItem.getAcctgTagEnumId6());
                invoiceItem.setAcctgTagEnumId7(orderItem.getAcctgTagEnumId7());
                invoiceItem.setAcctgTagEnumId8(orderItem.getAcctgTagEnumId8());
                invoiceItem.setAcctgTagEnumId9(orderItem.getAcctgTagEnumId9());
                invoiceItem.setAcctgTagEnumId10(orderItem.getAcctgTagEnumId10());
                invoiceRepository.update(invoiceItem);
            }
            Debug.logInfo("Setting invoice [" + invoiceId  + "] accounting tags", MODULE);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void recalcInvoiceAmounts() throws ServiceException {
        try {
            invoiceRepository = getInvoiceRepository();
            if (invoice == null) {
                invoice = invoiceRepository.getInvoiceById(invoiceId);
            }
            // recalculate each field
            Debug.logInfo("recalcInvoiceAmounts: [" + invoice.getInvoiceId() + "] invoice total = " + invoice.calculateInvoiceTotal(), MODULE);
            Debug.logInfo("recalcInvoiceAmounts: [" + invoice.getInvoiceId() + "] applied amount = " + invoice.calculateAppliedAmount(), MODULE);
            Debug.logInfo("recalcInvoiceAmounts: [" + invoice.getInvoiceId() + "] adjusted amount = " + invoice.calculateAdjustedAmount(), MODULE);
            Debug.logInfo("recalcInvoiceAmounts: [" + invoice.getInvoiceId() + "] open amount = " + invoice.calculateOpenAmount(), MODULE);
            Debug.logInfo("recalcInvoiceAmounts: [" + invoice.getInvoiceId() + "] pending applied amount = " + invoice.calculatePendingAppliedAmount(), MODULE);
            Debug.logInfo("recalcInvoiceAmounts: [" + invoice.getInvoiceId() + "] pending open amount = " + invoice.calculatePendingOpenAmount(), MODULE);
            Debug.logInfo("recalcInvoiceAmounts: [" + invoice.getInvoiceId() + "] invoice adjusted total = " + invoice.calculateInvoiceAdjustedTotal(), MODULE);
            Debug.logInfo("recalcInvoiceAmounts: [" + invoice.getInvoiceId() + "] interest charged = " + invoice.calculateInterestCharged(), MODULE);
            // persist the updated values
            invoiceRepository.update(invoice);

            // cascade the update to children invoices, this applies to interest charged for example
            Set<String> invoiceIds = Entity.getDistinctFieldValues(String.class, invoice.getInvoiceItems(), InvoiceItem.Fields.parentInvoiceId);
            invoiceIds.remove(invoice.getInvoiceId());
            for (String id : invoiceIds) {
                if (id != null) {
                    invoice = invoiceRepository.getInvoiceById(id);
                    recalcInvoiceAmounts();
                }
            }

        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void recalcInvoiceAmountsFromPayment() throws ServiceException {
        try {
            invoiceRepository = getInvoiceRepository();
            PaymentRepositoryInterface repository = getPaymentRepository();
            Payment payment = repository.getPaymentById(paymentId);
            Set<String> invoiceIds = new HashSet<String>();
            List<? extends PaymentApplication> applications = payment.getPaymentApplications();
            for (PaymentApplication application : applications) {
                if (UtilValidate.isNotEmpty(application.getInvoiceId())) {
                    invoiceIds.add(application.getInvoiceId());
                }
            }
            for (String invoiceId : invoiceIds) {
                invoice = invoiceRepository.getInvoiceById(invoiceId);
                recalcInvoiceAmounts();
            }
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void recalcAllEmptyAmountsInvoices() throws ServiceException {
        try {
            invoiceRepository = getInvoiceRepository();
            EntityCondition condition = EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition(Invoice.Fields.invoiceTotal.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(Invoice.Fields.appliedAmount.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(Invoice.Fields.adjustedAmount.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(Invoice.Fields.openAmount.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(Invoice.Fields.pendingAppliedAmount.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(Invoice.Fields.pendingOpenAmount.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(Invoice.Fields.invoiceAdjustedTotal.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(Invoice.Fields.interestCharged.name(), EntityOperator.EQUALS, null)
            );
            EntityListIterator<Invoice> invoicesIt = invoiceRepository.findIterator(Invoice.class, condition);
            Invoice invoice = null;
            while ((invoice = invoicesIt.next()) != null) {
                RecalcInvoiceAmountsService service = new RecalcInvoiceAmountsService();
                service.setInInvoiceId(invoice.getInvoiceId());
                runSync(service);
            }
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }


    private InvoiceRepositoryInterface getInvoiceRepository() throws RepositoryException {
        return getDomainsDirectory().getBillingDomain().getInvoiceRepository();
    }

    private PaymentRepositoryInterface getPaymentRepository() throws RepositoryException {
        return getDomainsDirectory().getBillingDomain().getPaymentRepository();
    }

    private LedgerSpecificationInterface getLedgerSpecification() throws RepositoryException {
        return getDomainsDirectory().getLedgerDomain().getLedgerRepository().getSpecification();
    }

    private InvoiceLedgerServiceInterface getInvoiceLedgerService() throws ServiceException {
        return getDomainsDirectory().getLedgerDomain().getInvoiceLedgerService();
    }
}
