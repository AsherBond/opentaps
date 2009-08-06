/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.financials.domain.billing.invoice;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.domain.order.OrderSpecification;
import org.opentaps.domain.billing.invoice.OrderInvoicingServiceInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;


/**
 * POJO implementation of services which create invoices from orders using the opentaps Service foundation class.
 */
public class OrderInvoicingService extends Service implements OrderInvoicingServiceInterface {

    private static final String module = OrderInvoicingService.class.getName();

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
            Map<String, Object> tmpResult = getInfrastructure().getDispatcher().runSync("createInvoiceForOrder", UtilMisc.toMap("orderId", orderId, "billItems", Repository.genericValueFromEntity(getInfrastructure().getDelegator(), itemsToInvoice), "userLogin", getUser().getOfbizUserLogin()), 7200, false);  // no new transaction
            if (ServiceUtil.isError(tmpResult)) {
                throw new ServiceException(ServiceUtil.getErrorMessage(tmpResult));
            }

            // change the status of the order items to COMPLETED
            order.setItemsStatus(itemsToInvoice, OrderSpecification.OrderItemStatusEnum.COMPLETED.getStatusId());

            // set the invoiceId of new invoice created
            this.invoiceId = (String) tmpResult.get("invoiceId");

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

}
