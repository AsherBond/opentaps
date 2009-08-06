/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.shipping;

import java.util.List;

import org.opentaps.domain.base.entities.CarrierShipmentBoxType;
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
