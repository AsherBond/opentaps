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
package org.opentaps.domain.order;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;

/**
 * Order related services.
 */
public interface OrderServiceInterface extends ServiceInterface {

    /**
     * Sets the order ID, required parameter for {@link #recreateOrderAdjustments} and {@link #recalcOrderTax}.
     * @param orderId the order ID
     */
    public void setOrderId(String orderId);

    /**
     * Removes all existing order adjustments, recalculate them and persist in <code>OrderAdjustment</code>.
     * @exception ServiceException if an error occurs
     */
    public void recreateOrderAdjustments() throws ServiceException;

    /**
     * Adjusts the order tax amount.
     * @exception ServiceException if an error occurs
     */
    public void recalcOrderTax() throws ServiceException;

    /**
     * Resets the grandTotal of an existing order.
     * @exception ServiceException if an error occurs
     */
    public void resetGrandTotal() throws ServiceException;

    /**
     * Adds a note to the order.
     * @param noteText the text of the note
     * @param isInternal if the note is internal or not
     * @throws ServiceException if an error occurs
     */
    public void addNote(String noteText, boolean isInternal) throws ServiceException;

}
