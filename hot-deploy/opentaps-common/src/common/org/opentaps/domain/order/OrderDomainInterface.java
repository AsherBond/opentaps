/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.order;

import org.opentaps.domain.inventory.OrderInventoryServiceInterface;
import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * This is the interface of the Order domain which handles orders.
 */
public interface OrderDomainInterface extends DomainInterface {

    /**
     * Returns the order repository.
     * @return an <code>OrderRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public OrderRepositoryInterface getOrderRepository() throws RepositoryException;

    /**
     * Returns the order service.
     * @return an <code>OrderServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public OrderServiceInterface getOrderService() throws ServiceException;

    /**
     * Returns the order inventory service.
     * @return an <code>OrderInventoryServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public OrderInventoryServiceInterface getOrderInventoryService() throws ServiceException;

    /**
     * Returns the sales opportunity search service.
     * @return a <code>SalesOpportunitySearchServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public SalesOpportunitySearchServiceInterface getSalesOpportunitySearchService() throws ServiceException;
}
