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

import java.util.List;
import java.util.Locale;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.common.domain.order.OrderSpecification;
import org.opentaps.domain.DomainService;
import org.opentaps.base.services.CreateInvoiceForOrderService;
import org.opentaps.domain.billing.invoice.OrderInvoicingServiceInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceException;


/**
 * POJO implementation of services which create invoices from orders using the opentaps Service foundation class.
 */
public class OrderInvoicingService extends DomainService implements OrderInvoicingServiceInterface {

    private static final String MODULE = OrderInvoicingService.class.getName();

    private String orderId = null;
    private String invoiceId = null;
    // by default, non-physical order items in this state will be invoiced
    private String statusIdForNonPhysicalItemsToInvoice = OrderSpecification.OrderItemStatusEnum.PERFORMED.getStatusId();

    /**
     * Default constructor.
     */
    public OrderInvoicingService() {
        super();
    }

    /**
     * Domain constructor.
     * @param infrastructure an <code>Infrastructure</code> value
     * @param user an <code>User</code> value
     * @param locale a <code>Locale</code> value
     * @exception ServiceException if an error occurs
     */
    public OrderInvoicingService(Infrastructure infrastructure, User user, Locale locale) throws ServiceException {
        super(infrastructure, user, locale);
    }

    /** {@inheritDoc} */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /** {@inheritDoc} */
    public void setOrderItemStatusId(String statusId) {
        if (statusId != null) {
            statusIdForNonPhysicalItemsToInvoice = statusId;
        }
    }

    /** {@inheritDoc} */
    public String getInvoiceId() {
        return this.invoiceId;
    }

    /** {@inheritDoc} */
    public void invoiceNonPhysicalOrderItems() throws ServiceException {
        try {
            // validate that the order actually exists and get list of non-physical
            OrderDomainInterface orderDomain = getDomainsDirectory().getOrderDomain();
            OrderRepositoryInterface orderRepository = orderDomain.getOrderRepository();

            Order order = orderRepository.getOrderById(orderId);
            List<OrderItem> itemsToInvoice = order.getNonPhysicalItemsForStatus(statusIdForNonPhysicalItemsToInvoice);

            // check if there are items to invoice
            if (UtilValidate.isEmpty(itemsToInvoice)) {
                throw new ServiceException("OpentapsError_PerformedItemsToInvoiceNotFound", UtilMisc.toMap("orderId", orderId));
            }

            // create a new invoice for the order items
            // because of the way createInvoiceForOrder is written (665 lines of code!) we'd have to do some re-factoring before we can add the items to an existing invoice
            CreateInvoiceForOrderService service = new CreateInvoiceForOrderService(getUser());
            service.setInOrderId(orderId);
            service.setInBillItems(Repository.genericValueFromEntity(getInfrastructure().getDelegator(), itemsToInvoice));
            service.runSyncNoNewTransaction(getInfrastructure());
            if (service.isError()) {
                throw new ServiceException(service.getErrorMessage());
            }

            // change the status of the order items to COMPLETED
            order.setItemsStatus(itemsToInvoice, OrderSpecification.OrderItemStatusEnum.COMPLETED.getStatusId());

            // set the invoiceId of new invoice created
            this.invoiceId = service.getOutInvoiceId();

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

}
