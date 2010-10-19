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
package org.opentaps.domain.search.order;

import java.util.List;

import org.opentaps.domain.order.Order;
import org.opentaps.domain.search.SearchServiceInterface;

/**
 * This is the interface of the Sales Order search service.
 */
public interface SalesOrderSearchServiceInterface extends SearchServiceInterface {

    /**
     * Gets the resulting <code>List</code> of Sales <code>Order</code> that matched the query.
     * @return the <code>List</code> of <code>Order</code>
     */
    public List<Order> getOrders();

}
