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
package org.opentaps.warehouse.domain.shipping;


import java.util.List;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.opentaps.domain.base.entities.CarrierShipmentBoxType;
import org.opentaps.domain.base.entities.Facility;
import org.opentaps.domain.base.entities.Party;
import org.opentaps.domain.base.entities.WarehouseDefaultBoxType;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.shipping.ShippingRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;


/** {@inheritDoc} */
public class ShippingRepository extends Repository implements ShippingRepositoryInterface {

    /**
     * Default constructor.
     */
    public ShippingRepository() {
        super();
    }

    /**
     * Use this for Repositories which will only access the database via the delegator.
     * @param delegator the delegator
     */
    public ShippingRepository(GenericDelegator delegator) {
        super(delegator);
    }

    /**
     * Use this for domain Repositories.
     * @param infrastructure the domain infrastructure
     * @param user the domain user
     * @throws RepositoryException if an error occurs
     */
    public ShippingRepository(Infrastructure infrastructure, User user) throws RepositoryException {
        super(infrastructure, user);
    }

    /** {@inheritDoc} */
    public CarrierShipmentBoxType getDefaultBoxType(List<OrderItem> orderItems) throws RepositoryException {
        // the facility is from the first order item, first inventory item reserved
        Facility facility = null;
        Party carrier = null;
        for (OrderItem oi : orderItems) {
            if (UtilValidate.isEmpty(oi.getOrderItemShipGrpInvReses())) {
                continue;
            }
            if (oi.getOrderItemShipGrpInvReses().get(0).getInventoryItem() == null) {
                continue;
            }
            facility = oi.getOrderItemShipGrpInvReses().get(0).getInventoryItem().getFacility();
            carrier = oi.getOrderItemShipGrpInvReses().get(0).getOrderItemShipGroup().getCarrierParty();
        }

        // if no facility is found return null
        if (facility == null || carrier == null) {
            return null;
        }

        // get the corresponding box if any
        WarehouseDefaultBoxType defaultBox = findOne(WarehouseDefaultBoxType.class, map(WarehouseDefaultBoxType.Fields.facilityId, facility.getFacilityId(), WarehouseDefaultBoxType.Fields.partyId, carrier.getPartyId()));

        if (defaultBox == null) {
            return null;
        }

        return defaultBox.getCarrierShipmentBoxType();
    }

}
