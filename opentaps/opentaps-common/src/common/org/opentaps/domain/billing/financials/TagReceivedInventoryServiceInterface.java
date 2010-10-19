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

package org.opentaps.domain.billing.financials;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;

/**
 * POJO service to tag a inventory received from a PO.
 */
public interface TagReceivedInventoryServiceInterface  extends ServiceInterface {

    /**
     * Tag the received inventory item from the PO item.
     * @throws ServiceException if an error occurs
     */
    public void tagReceivedInventoryFromOrder() throws ServiceException;


    /**
     * Sets the required input parameter for service {@link #tagReceivedInventoryFromOrder}.
     * @param orderId the ID of the received Purchase Order
     */
    public void setOrderId(String orderId);

    /**
     * Sets the required input parameter for service {@link #tagReceivedInventoryFromOrder}.
     * @param orderItemSeqId the ID of the received Purchase Order Item
     */
    public void setOrderItemSeqId(String orderItemSeqId);

    /**
     * Sets the required input parameter for service {@link #tagReceivedInventoryFromOrder}.
     * @param inventoryItemId the ID of the received Inventory
     */
    public void setInventoryItemId(String inventoryItemId);
}
