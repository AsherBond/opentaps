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

import java.math.BigDecimal;

import org.opentaps.foundation.repository.RepositoryException;

/**
 * Return Item entity.
 */
public class ReturnItem extends org.opentaps.base.entities.ReturnItem {

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
