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
package org.opentaps.common.domain.order;

import org.opentaps.common.domain.inventory.OrderInventoryService;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.order.OrderServiceInterface;
import org.opentaps.domain.order.SalesOpportunitySearchServiceInterface;
import org.opentaps.foundation.domain.Domain;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is an implementation of the Order domain.
 */
public class OrderDomain extends Domain implements OrderDomainInterface {

    /** {@inheritDoc} */
    public OrderRepository getOrderRepository() throws RepositoryException {
        return instantiateRepository(OrderRepository.class);
    }

    /** {@inheritDoc} */
    public OrderServiceInterface getOrderService() throws ServiceException {
        return instantiateService(OrderService.class);
    }

    /** {@inheritDoc} */
    public OrderInventoryService getOrderInventoryService() throws ServiceException {
        return instantiateService(OrderInventoryService.class);
    }

    /** {@inheritDoc} */
    public SalesOpportunitySearchServiceInterface getSalesOpportunitySearchService() throws ServiceException {
        return instantiateService(SalesOpportunitySearchService.class);
    }

}
