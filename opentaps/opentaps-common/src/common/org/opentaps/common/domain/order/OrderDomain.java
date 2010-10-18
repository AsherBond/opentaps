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
package org.opentaps.common.domain.order;

import org.opentaps.common.domain.inventory.OrderInventoryService;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.order.OrderServiceInterface;
import org.opentaps.domain.order.PurchaseOrderLookupRepositoryInterface;
import org.opentaps.domain.order.SalesOrderLookupRepositoryInterface;
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
    public SalesOrderLookupRepositoryInterface getSalesOrderLookupRepository() throws RepositoryException {
        return instantiateRepository(SalesOrderLookupRepository.class);
    }

    /** {@inheritDoc} */
    public PurchaseOrderLookupRepositoryInterface getPurchaseOrderLookupRepository() throws RepositoryException {
        return instantiateRepository(PurchaseOrderLookupRepository.class);
    }
}
