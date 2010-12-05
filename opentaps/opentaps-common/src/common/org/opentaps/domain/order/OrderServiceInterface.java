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
     * Sets a new contact mech id that should be used as new shipping location.<br>
     * updateOrderShippingAddress and updateOrderBillingAddress input attribute.
     * @param newShippingContactMechId new contact mech identifier, should be a postal address
     */
    public void setNewOrderContactMechId(String newShippingContactMechId);

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

    /**
     * Service changes order shipping address.
     * @throws ServiceException if an error occurs
     */
    public void updateOrderShippingAddress() throws ServiceException;

    /**
     * Service changes order billing address.
     * @throws ServiceException if an error occurs
     */
    public void updateOrderBillingAddress() throws ServiceException;

    /**
     * Guesses and sets the order billing address, to avoid order created without a billing address.
     * First checks the order payment methods billing addresses (eg: credit card), else defaults to
     *  customer first billing address found.
     * @throws ServiceException if an error occurs
     */
    public void autoSetOrderBillingAddress() throws ServiceException;
}
