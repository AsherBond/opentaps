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

import org.opentaps.foundation.repository.RepositoryException;

/**
 * Return Item entity.
 */
public class ReturnItem extends org.opentaps.domain.base.entities.ReturnItem {

    private Return ret;
    private BigDecimal returnQuantity;

    /**
     * Default constructor.
     */
    public ReturnItem() {
        super();
    }

    /**
     * Overrides the base class accessor in order to return 0 instead of <code>null</code>.
     * @return the return quantity
     */
    @Override
    public BigDecimal getReturnQuantity() {
        if (returnQuantity == null) {
            returnQuantity = super.getReturnQuantity();
            if (returnQuantity == null) {
               returnQuantity  = BigDecimal.ZERO;
            }
        }

        return returnQuantity;
    }

    /**
     * Gets the <code>Return</code> parent object for this return item.
     * @return the parent <code>Return</code>
     * @throws RepositoryException if an error occurs
     */
    public Return getReturn() throws RepositoryException {
        if (ret == null) {
            ret = getRepository().getRelatedReturn(this);
        }
        return ret;
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
