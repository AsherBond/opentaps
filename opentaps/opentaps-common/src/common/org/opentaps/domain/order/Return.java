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

import java.util.List;

import org.opentaps.foundation.repository.RepositoryException;

/**
 * Return entity.
 */
public class Return extends org.opentaps.base.entities.ReturnHeader {

    private List<ReturnItem> items;

    /**
     * Default constructor.
     */
    public Return() {
        super();
    }

    /**
     * Get the specification object which contains enumerations and logical checking for Orders.
     * @return the <code>OrderSpecificationInterface</code>
     */
    public OrderSpecificationInterface getOrderSpecification() {
        return getRepository().getOrderSpecification();
    }

    /**
     * Checks if this order current status is "accepted".
     * @return a <code>Boolean</code> value
     */
    public Boolean isAccepted() {
        return getOrderSpecification().isAccepted(this);
    }

    /**
     * Checks if this order current status is "cancelled".
     * @return a <code>Boolean</code> value
     */
    public Boolean isCancelled() {
        return getOrderSpecification().isCancelled(this);
    }

    /**
     * Checks if this order current status is "completed".
     * @return a <code>Boolean</code> value
     */
    public Boolean isCompleted() {
        return getOrderSpecification().isCompleted(this);
    }

    /**
     * Checks if this order current status is "requested".
     * @return a <code>Boolean</code> value
     */
    public Boolean isRequested() {
        return getOrderSpecification().isRequested(this);
    }

    /**
     * Checks if this order current status is "received".
     * @return a <code>Boolean</code> value
     */
    public Boolean isReceived() {
        return getOrderSpecification().isReceived(this);
    }

    /**
     * Checks if this order current status is "manual refund required".
     * @return a <code>Boolean</code> value
     */
    public Boolean isRequiringManualRefund() {
        return getOrderSpecification().isRequiringManualRefund(this);
    }

    /**
     * Gets the list of <code>ReturnItem</code> related to this return.
     * Returns the order domain object instead of the base entity.
     * @return list of <code>ReturnItem</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<ReturnItem> getReturnItems() throws RepositoryException {
        if (items == null) {
            items = getRelated(ReturnItem.class, "ReturnItem");
        }
        return items;
    }

    /**
     * Gets the list of <code>ReturnItem</code> related to this return.
     * This is an alias for {@link #getReturnItems}.
     * @return list of <code>ReturnItem</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<ReturnItem> getItems() throws RepositoryException {
        return this.getReturnItems();
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
