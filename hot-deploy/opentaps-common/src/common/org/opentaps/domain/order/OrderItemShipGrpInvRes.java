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

import java.math.BigDecimal;

/**
 * Order Item Ship Group Inventory Reservation entity.
 */
public class OrderItemShipGrpInvRes extends org.opentaps.domain.base.entities.OrderItemShipGrpInvRes {

    private BigDecimal quantityNotAvailable;

    /**
     * Default constructor.
     */
    public OrderItemShipGrpInvRes() {
        super();
    }

    /**
     * Overrides the base class accessor in order to return 0 instead of <code>null</code>.
     * @return the quantity not available
     */
    @Override
    public BigDecimal getQuantityNotAvailable() {
        if (quantityNotAvailable == null) {
            quantityNotAvailable = super.getQuantityNotAvailable();
            if (quantityNotAvailable == null) {
               quantityNotAvailable  = BigDecimal.ZERO;
            }
        }

        return quantityNotAvailable;
    }
}
