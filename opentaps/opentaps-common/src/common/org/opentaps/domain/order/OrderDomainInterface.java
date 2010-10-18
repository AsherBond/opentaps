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
package org.opentaps.domain.order;

import org.opentaps.domain.inventory.OrderInventoryServiceInterface;
import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

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
     * Returns the sales order search repository.
     * @return a <code>SalesOrderLookupRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public SalesOrderLookupRepositoryInterface getSalesOrderLookupRepository() throws RepositoryException;

    /**
     * Returns the sales order search repository.
     * @return a <code>SalesOrderLookupRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public PurchaseOrderLookupRepositoryInterface getPurchaseOrderLookupRepository() throws RepositoryException;

}
