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
package org.opentaps.domain.shipping;

import java.util.List;

import org.opentaps.base.entities.CarrierShipmentBoxType;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for Shipping to handle interaction of Shipping-related domain with the entity engine (database) and the service engine.
 */
public interface ShippingRepositoryInterface extends RepositoryInterface {

    /**
     * Finds the default <code>CarrierShipmentBoxType</code> corresponding to the given list of order items.
     * @param orderItems the <code>List</code> of <code>OrderItem</code> to be shipped
     * @return the <code>CarrierShipmentBoxType</code> found, or <code>null</code>
     * @throws RepositoryException if an error occurs
     */
    public CarrierShipmentBoxType getDefaultBoxType(List<OrderItem> orderItems) throws RepositoryException;

}
