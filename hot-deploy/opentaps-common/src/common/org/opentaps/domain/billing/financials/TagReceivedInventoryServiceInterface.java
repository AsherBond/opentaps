/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
