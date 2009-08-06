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

import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.domain.base.entities.StatusItem;

/**
 * Shipment entity.
 */
public class Shipment extends org.opentaps.domain.base.entities.Shipment {

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
     * This is an alias for {@link org.opentaps.domain.base.entities.Shipment#getStatusItem}.
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
