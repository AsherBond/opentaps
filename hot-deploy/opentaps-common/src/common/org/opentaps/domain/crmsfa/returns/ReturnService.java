/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.InvoiceItem;
import org.opentaps.base.entities.OrderItem;
import org.opentaps.base.entities.Payment;
import org.opentaps.base.entities.PaymentApplication;
import org.opentaps.base.entities.ReturnItemBilling;
import org.opentaps.base.entities.ReturnItemResponse;
import org.opentaps.domain.DomainService;
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
	private String invoiceItemSeqId;
    
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
     * Sets the invoice item seq ID.
     * @param invoiceItemSeqId the invoice item seq ID
     */
    public void setInvoiceItemSeqId(String invoiceItemSeqId) {
        this.invoiceItemSeqId = invoiceItemSeqId;
    }

    /**
     * Copies all the accounting tags from the return order item to the payment application.
     * @throws ServiceException if an error occurs
     */
    public void updatePaymentApplicationAccountTagsByReturnOrder() throws ServiceException {
    	try {
			PaymentRepositoryInterface paymentRepository = getDomainsDirectory().getBillingDomain().getPaymentRepository();
			OrderRepositoryInterface orderRepository = getDomainsDirectory().getOrderDomain().getOrderRepository();
			ReturnItemResponse returnItemResponse = orderRepository.getReturnItemResponseById(returnItemResponseId);
			Payment payment = returnItemResponse.getPayment();
			Debug.logInfo("getPaymentTypeId : " + payment.getPaymentTypeId()
					+ ", payment.getOrderPaymentPreference() : " + payment.getOrderPaymentPreference(), MODULE);
			if ("CUSTOMER_REFUND".equals(payment.getPaymentTypeId())) {
				for (PaymentApplication paymentApplication : payment.getPaymentApplications()) {
					OrderItem orderItem = null;
					if (UtilValidate.isNotEmpty(payment.getOrderPaymentPreference())) {
						orderItem = payment.getOrderPaymentPreference().getOrderItem();
						Debug.logInfo("orderItem : " + orderItem, MODULE);
					}
					// gets order item with relate invoice
					if (UtilValidate.isEmpty(orderItem) && UtilValidate.isNotEmpty(paymentApplication.getInvoice())) {
						for (InvoiceItem invoiceItem : paymentApplication.getInvoice().getInvoiceItems()) {
							if (UtilValidate.isNotEmpty(invoiceItem.getReturnItemBillings().size())) {
								ReturnItemBilling returnItemBilling = invoiceItem.getReturnItemBillings().get(0);
								orderItem = returnItemBilling.getReturnItem().getOrderItem();
								break;
							}
						}
					}
					Debug.logInfo("orderItem : " + orderItem, MODULE);
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
     * Copies all the accounting tags from the return order item to the invoice item.
     * @throws ServiceException if an error occurs
     */
    public void updateInvoiceItemAccountTagsByReturnOrder() throws ServiceException {
    	try {
			InvoiceRepositoryInterface invoiceRepository = getDomainsDirectory().getBillingDomain().getInvoiceRepository();
			InvoiceItem invoiceItem = invoiceRepository.getInvoiceItemById(invoiceId, invoiceItemSeqId);
			// if the invoice type equals CUST_RTN_INVOICE and the invoice item have relate return item billing entities
			// then copies all the accounting tags from the return order item to the invoice item.
			if ("CUST_RTN_INVOICE".equals(invoiceItem.getInvoice().getInvoiceTypeId()) && invoiceItem.getReturnItemBillings().size() > 0) {
				ReturnItemBilling returnItemBilling = invoiceItem.getReturnItemBillings().get(0);
				OrderItem orderItem = returnItemBilling.getReturnItem().getOrderItem();
				if (UtilValidate.isNotEmpty(orderItem)) {
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
			}
			
		} catch (RepositoryException e) {
			throw new ServiceException(e);
		} catch (EntityNotFoundException e) {
			throw new ServiceException(e);
		}
    }


}
