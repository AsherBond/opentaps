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
package org.opentaps.domain.crmsfa.returns;

import java.util.ArrayList;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.InvoiceItem;
import org.opentaps.base.entities.OrderItem;
import org.opentaps.base.entities.Payment;
import org.opentaps.base.entities.PaymentApplication;
import org.opentaps.base.entities.ReturnItemBilling;
import org.opentaps.base.entities.ReturnItemResponse;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Implementation of the return services.
 */
public class ReturnService extends DomainService {
    private String returnItemResponseId = null;
    private String invoiceId;

    private static final String MODULE = ReturnService.class.getName();


    /**
     * Default constructor.
     */
    public ReturnService() {
        super();
    }

    /**
     * Sets the return item response ID.
     * @param returnItemResponseId the return item response ID.
     */
    public void setReturnItemResponseId(String returnItemResponseId) {
        this.returnItemResponseId = returnItemResponseId;
    }

    /**
     * Sets the invoice ID.
     * @param invoiceId the invoice ID
     */
    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    /**
     * Copies all the accounting tags from the return order item to the payment application.
     * This is called as a SECA on createPaymentApplicationsFromReturnItemResponse.
     * @throws ServiceException if an error occurs
     */
    public void updatePaymentApplicationAccountTagsByReturnOrder() throws ServiceException {
        try {
            PaymentRepositoryInterface paymentRepository = getDomainsDirectory().getBillingDomain().getPaymentRepository();
            OrderRepositoryInterface orderRepository = getDomainsDirectory().getOrderDomain().getOrderRepository();
            ReturnItemResponse returnItemResponse = orderRepository.getReturnItemResponseById(returnItemResponseId);
            Payment payment = returnItemResponse.getPayment();
            Debug.logInfo("getPaymentTypeId : " + payment.getPaymentTypeId() + ", payment.getOrderPaymentPreference() : " + payment.getOrderPaymentPreference(), MODULE);
            if ("CUSTOMER_REFUND".equals(payment.getPaymentTypeId())) {
                for (PaymentApplication paymentApplication : payment.getPaymentApplications()) {
                    OrderItem orderItem = null;
                    if (UtilValidate.isNotEmpty(payment.getOrderPaymentPreference())) {
                        orderItem = payment.getOrderPaymentPreference().getOrderItem();
                        Debug.logInfo("Found orderItem from OrderPaymentPreference : " + orderItem, MODULE);
                    }
                    // gets order item from a ReturnItemBilling of related invoice
                    if (UtilValidate.isEmpty(orderItem) && UtilValidate.isNotEmpty(paymentApplication.getInvoice())) {
                        for (InvoiceItem invoiceItem : paymentApplication.getInvoice().getInvoiceItems()) {
                            if (UtilValidate.isNotEmpty(invoiceItem.getReturnItemBillings())) {
                                ReturnItemBilling returnItemBilling = invoiceItem.getReturnItemBillings().get(0);
                                orderItem = returnItemBilling.getReturnItem().getOrderItem();
                                Debug.logInfo("Found orderItem from ReturnItemBilling : " + orderItem, MODULE);
                                break;
                            }
                        }
                    }
                    Debug.logInfo("Using orderItem : " + orderItem, MODULE);
                    // copies all the accounting tags from the return order item to the payment application.
                    if (UtilValidate.isNotEmpty(orderItem)) {
                        paymentApplication.setAcctgTagEnumId1(orderItem.getAcctgTagEnumId1());
                        paymentApplication.setAcctgTagEnumId2(orderItem.getAcctgTagEnumId2());
                        paymentApplication.setAcctgTagEnumId3(orderItem.getAcctgTagEnumId3());
                        paymentApplication.setAcctgTagEnumId4(orderItem.getAcctgTagEnumId4());
                        paymentApplication.setAcctgTagEnumId5(orderItem.getAcctgTagEnumId5());
                        paymentApplication.setAcctgTagEnumId6(orderItem.getAcctgTagEnumId6());
                        paymentApplication.setAcctgTagEnumId7(orderItem.getAcctgTagEnumId7());
                        paymentApplication.setAcctgTagEnumId8(orderItem.getAcctgTagEnumId8());
                        paymentApplication.setAcctgTagEnumId9(orderItem.getAcctgTagEnumId9());
                        paymentApplication.setAcctgTagEnumId10(orderItem.getAcctgTagEnumId10());
                        paymentRepository.update(paymentApplication);

                        //update payment accounting tags as well
                        payment.setAcctgTagEnumId1(orderItem.getAcctgTagEnumId1());
                        payment.setAcctgTagEnumId2(orderItem.getAcctgTagEnumId2());
                        payment.setAcctgTagEnumId3(orderItem.getAcctgTagEnumId3());
                        payment.setAcctgTagEnumId4(orderItem.getAcctgTagEnumId4());
                        payment.setAcctgTagEnumId5(orderItem.getAcctgTagEnumId5());
                        payment.setAcctgTagEnumId6(orderItem.getAcctgTagEnumId6());
                        payment.setAcctgTagEnumId7(orderItem.getAcctgTagEnumId7());
                        payment.setAcctgTagEnumId8(orderItem.getAcctgTagEnumId8());
                        payment.setAcctgTagEnumId9(orderItem.getAcctgTagEnumId9());
                        payment.setAcctgTagEnumId10(orderItem.getAcctgTagEnumId10());
                        paymentRepository.update(payment);
                    }
                }
            }

        } catch (RepositoryException e) {
            throw new ServiceException(e);
        } catch (EntityNotFoundException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Copies all the accounting tags from the return order items to the invoice items.
     * This is called as a SECA on createInvoiceFromReturn.
     * @throws ServiceException if an error occurs
     */
    public void updateInvoiceItemsAccountingTagsForReturn() throws ServiceException {
        try {
            // get the invoice that was created from the return
            InvoiceRepositoryInterface invoiceRepository = getDomainsDirectory().getBillingDomain().getInvoiceRepository();
            Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
            List<? extends InvoiceItem> items = invoice.getInvoiceItems();

            // the first order item found will be used to tag the invoice items that have no related order item
            // for example the global adjustments on the invoice such as Shipping charges.
            OrderItem firstOrderItem = null;
            // we process those order items in a second pass
            List<InvoiceItem> noOrderItems = new ArrayList<InvoiceItem>();

            for (InvoiceItem invoiceItem : items) {
                List<? extends ReturnItemBilling> billings = invoiceItem.getReturnItemBillings();
                // one invoice item has only one related return item if any
                if (UtilValidate.isNotEmpty(billings)) {
                    ReturnItemBilling billing = billings.get(0);
                    OrderItem orderItem = billing.getReturnItem().getOrderItem();
                    if (orderItem != null) {
                        if (firstOrderItem == null) {
                            firstOrderItem = orderItem;
                        }
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
                        continue;
                    }
                }
                // if we are here then no order item was found related to the invoice item
                noOrderItems.add(invoiceItem);
            }

            // second pass for the invoice items we could not tag earlier
            if (firstOrderItem != null) {
                for (InvoiceItem invoiceItem : noOrderItems) {
                    invoiceItem.setAcctgTagEnumId1(firstOrderItem.getAcctgTagEnumId1());
                    invoiceItem.setAcctgTagEnumId2(firstOrderItem.getAcctgTagEnumId2());
                    invoiceItem.setAcctgTagEnumId3(firstOrderItem.getAcctgTagEnumId3());
                    invoiceItem.setAcctgTagEnumId4(firstOrderItem.getAcctgTagEnumId4());
                    invoiceItem.setAcctgTagEnumId5(firstOrderItem.getAcctgTagEnumId5());
                    invoiceItem.setAcctgTagEnumId6(firstOrderItem.getAcctgTagEnumId6());
                    invoiceItem.setAcctgTagEnumId7(firstOrderItem.getAcctgTagEnumId7());
                    invoiceItem.setAcctgTagEnumId8(firstOrderItem.getAcctgTagEnumId8());
                    invoiceItem.setAcctgTagEnumId9(firstOrderItem.getAcctgTagEnumId9());
                    invoiceItem.setAcctgTagEnumId10(firstOrderItem.getAcctgTagEnumId10());
                    invoiceRepository.update(invoiceItem);
                }
            } else {
                // this should not happen as it is not possible to create a return
                // without an item from the order (eg: just refunding an adjustment)
                // log this as an error
                Debug.logError("updateInvoiceItemsAccountingTagsForReturn: did not find any related order item to take the accounting tags from for Return invoice [" + invoiceId + "]", MODULE);
            }

        } catch (RepositoryException e) {
            throw new ServiceException(e);
        } catch (EntityNotFoundException e) {
            throw new ServiceException(e);
        }
    }
}
