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
package org.opentaps.domain.inventory;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;


public interface OrderInventoryServiceInterface extends ServiceInterface {

    /**
     * Service <code>completeInventoryTransfer</code><br/>
     * Attribute <code>inventoryTransferId</code>, mandatory.
     *
     * @param inventoryTransferId Inventory transfer ID to set, should be primary key of the <code>InventoryTransfer</code> entity.
     */
    public void setInventoryTransferId(String inventoryTransferId);

    /**
     * Completes an Inventory Transfer, ensures the <code>balanceInventoryItems</code> gets called if necessary.
     * @exception ServiceException if an error occurs
     */
    public void completeInventoryTransfer() throws ServiceException;

    /**
     * Service <code>balanceInventoryItems</code><br/>
     * Attribute <code>inventoryItemId</code>, mandatory.
     *
     * @param inventoryItemId Inventory item ID to set, should be primary key of the <code>InventoryItem</code> entity.
     */
    public void setInventoryItemId(String inventoryItemId);

    /**
     * Service <code>balanceInventoryItems</code><br/>
     * Attribute <code>priorityOrderId</code>, optional.
     *
     * @param priorityOrderId Order ID to reserve in priority
     */
    public void setPriorityOrderId(String priorityOrderId);

    /**
     * Service <code>balanceInventoryItems</code><br/>
     * Attribute <code>priorityOrderItemSeqId</code>, optional.
     *
     * @param priorityOrderItemSeqId Order Item ID to reserve in priority
     */
    public void setPriorityOrderItemSeqId(String priorityOrderItemSeqId);

    /**
     * Balance inventory items based on the item passed.
     * @exception ServiceException if an error occurs
     */
    public void balanceInventoryItems() throws ServiceException;

    /**
     * Service <code>reserveProductInventory/reserveProductInventoryByFacility</code><br/>
     * Attribute <code>productId</code>, mandatory.
     *
     * @param productId Product ID to set, should be primary key of the Product entity.
     */
    public void setProductId(String productId);

    /**
     * Service <code>reserveProductInventory/reserveProductInventoryByFacility</code><br/>
     * Attribute <code>orderId</code>, mandatory.
     *
     * @param orderId Order ID to set, should be primary key of the OrderHeader entity.
     */
    public void setOrderId(String orderId);

    /**
     * Service <code>reserveProductInventory/reserveProductInventoryByFacility</code><br/>
     * Attribute <code>orderItemSeqId</code>, mandatory.
     *
     * @param orderItemSeqId Order item sequence number to set, OrderItem entity
     */
    public void setOrderItemSeqId(String orderItemSeqId);


    /**
     * Service <code>reserveProductInventory/reserveProductInventoryByFacility</code><br/>
     * Attribute <code>shipGroupSeqId</code>, mandatory.
     *
     * @param shipGroupSeqId Shipping group sequence number
     */
    public void setShipGroupSeqId(String shipGroupSeqId);

    /**
     * Service <code>reserveProductInventory/reserveProductInventoryByFacility</code><br/>
     * Attribute <code>quantity</code>, mandatory.
     *
     * @param quantity Quantity that is subject to reservation
     */
    public void setQuantity(BigDecimal quantity);

    /**
     * Service <code>reserveProductInventory/reserveProductInventoryByFacility</code><br/>
     * Attribute <code>reservedDatetime</code>, optional.
     *
     * @param reservedDatetime Reservation date
     */
    public void setReservedDatetime(Timestamp reservedDatetime);

    /**
     * Service <code>reserveProductInventory/reserveProductInventoryByFacility</code><br/>
     * Attribute <code>requireInventory</code>, mandatory.
     *
     * @param requireInventory Require inventory flag, may take on a value 'Y' or 'N'
     */
    public void setRequireInventory(String requireInventory);

    /**
     * Service <code>reserveProductInventory/reserveProductInventoryByFacility</code><br/>
     * Attribute <code>reserveOrderEnumId</code>, optional.
     *
     * @param reserveOrderEnumId The reserveOrderEnumId to set
     */
    public void setReserveOrderEnumId(String reserveOrderEnumId);

    /**
     * Service <code>reserveProductInventory/reserveProductInventoryByFacility</code><br/>
     * Attribute <code>sequenceId</code>, optional.
     *
     * @param sequenceId The sequenceId to set
     */
    public void setSequenceId(Long sequenceId);

    /**
     * Service <code>reserveProductInventory/reserveProductInventoryByFacility</code><br/>
     * Attribute <code>quantityNotReserved</code>, mandatory.
     *
     * @return
     *    If requireInventory is Y the quantity not reserved is returned, if N then a negative
     *    availableToPromise will be used to track quantity ordered beyond what is in stock.
     */
    public BigDecimal getQuantityNotReserved();

    /**
     * Service <code>reserveProductInventoryByFacility</code><br/>
     * Attribute <code>facilityId</code>, optional.
     *
     * @param facilityId The facility ID to set
     */
    public void setFacilityId(String facilityId);

    /**
     * Reserve Inventory for a Product<br/>
     * This method implements POJO services <code>reserveProductInventory</code> and <code>reserveProductInventoryByFacility</code>.
     *
     * @throws ServiceException if an error occurs
     */
    public void reserveProductInventory() throws ServiceException;

    /**
     * Cancel reservation and reserve the same product from another warehouse.
     *
     * @throws ServiceException if an error occurs
     */
    public void reReserveProductInventory() throws ServiceException;

    /**
     * Automatically taken apart Purchasing Package product.
     * @exception ServiceException if an error occurs
     */
    public void disassemblePurchasingPackage() throws ServiceException;

    /**
     * Service <code>reserveProductInventoryByFacility</code><br/>
     * Attribute <code>priority</code>, optional.
     *
     * @param priority the priority
     */
    public void setPriority(String priority);

    /**
     * Sets the OrderItemShipGroup estimatedShipDate according to the reservations promisedDatetime.
     * @exception ServiceException if an error occurs
     */
    public void setOrderItemShipGroupEstimatedShipDate() throws ServiceException;
}
