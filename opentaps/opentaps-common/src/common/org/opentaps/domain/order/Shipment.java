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

import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.base.entities.StatusItem;

/**
 * Shipment entity.
 */
public class Shipment extends org.opentaps.base.entities.Shipment {

    /**
     * Default constructor.
     */
    public Shipment() {
        super();
    }

    /**
     * Is this shipment status "cancelled".
     * @return a <code>Boolean</code> value
     */
    public Boolean isCancelled() {
        return getOrderSpecification().isCancelled(this);
    }

    /**
     * Gets this shipment current <code>StatusItem</code>.
     * This is an alias for {@link org.opentaps.base.entities.Shipment#getStatusItem}.
     * @return the current <code>StatusItem</code>
     * @throws RepositoryException if an error occurs
     */
    public StatusItem getStatus() throws RepositoryException {
        return this.getStatusItem();
    }

    /**
     * Get the specification object which contains enumerations and logical checking for Orders.
     * @return the <code>OrderSpecificationInterface</code>
     */
    public OrderSpecificationInterface getOrderSpecification() {
        return getRepository().getOrderSpecification();
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
