/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.base.entities.InvoiceAdjustment;
import org.opentaps.domain.base.entities.InvoiceItem;
import org.opentaps.domain.base.entities.OrderItem;
import org.opentaps.domain.base.entities.OrderItemBilling;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.invoice.InvoiceServiceInterface;
import org.opentaps.domain.ledger.InvoiceLedgerServiceInterface;
import org.opentaps.domain.ledger.LedgerSpecificationInterface;
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
    private LedgerSpecificationInterface ledgerSpecification = null;
    private InvoiceLedgerServiceInterface invoiceLedgerService = null;
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

    private InvoiceRepositoryInterface getInvoiceRepository() throws RepositoryException {
        return getDomainsDirectory().getBillingDomain().getInvoiceRepository();
    }

    private LedgerSpecificationInterface getLedgerSpecification() throws RepositoryException {
        return getDomainsDirectory().getLedgerDomain().getLedgerRepository().getSpecification();
    }

    private InvoiceLedgerServiceInterface getInvoiceLedgerService() throws ServiceException {
        return getDomainsDirectory().getLedgerDomain().getInvoiceLedgerService();
    }
}
