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
package org.opentaps.warehouse.domain.shipping;


import java.util.List;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.opentaps.base.entities.CarrierShipmentBoxType;
import org.opentaps.base.entities.Facility;
import org.opentaps.base.entities.Party;
import org.opentaps.base.entities.WarehouseDefaultBoxType;
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
    public ShippingRepository(Delegator delegator) {
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
