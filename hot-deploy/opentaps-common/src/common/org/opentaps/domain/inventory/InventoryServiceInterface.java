/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.inventory;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;


/**
 * Interface for inventory services.
 */
public interface InventoryServiceInterface extends ServiceInterface {

    /**
     * Sets the product ID, required parameter for {@link #getProductInventoryAvailable}.
     * @param productId a <code>String</code> value
     */
    public void setProductId(String productId);

    /**
     * Sets the use cache setting, optional parameter for {@link #getProductInventoryAvailable}, defaults to <code>false</code>.
     * @param useCache a <code>Boolean</code> value
     */
    public void setUseCache(Boolean useCache);

    /**
     * Gets the quantity on hand total from {@link #getProductInventoryAvailable}.
     * @return a <code>Double</code> value
     */
    public Double getQuantityOnHandTotal();

    /**
     * Gets the quantity available to promise total from {@link #getProductInventoryAvailable}.
     * @return a <code>Double</code> value
     */
    public Double getAvailableToPromiseTotal();

    /**
     * Sets the inventory item ID, required parameter for {@link #prepareInventoryTransfer}.
     * @param inventoryItemId a <code>String</code> value
     */
    public void setInventoryItemId(String inventoryItemId);

    /**
     * Gets the inventory item ID, from {@link #prepareInventoryTransfer}.
     * @return the inventory item id
     */
    public String getInventoryItemId();

    /**
     * Sets the transfer quantity, required parameter for {@link #prepareInventoryTransfer}.
     * @param xferQty a <code>Double</code> value
     */
    public void setXferQty(Double xferQty);

    /**
     * Sets the inventory transfer identifier to cancel.
     * @param inventoryTransferId the inventoryTransferId to set
     */
    public void setInventoryTransferId(String inventoryTransferId);

    /**
     * Service to retrieve a product availability from the inventory.
     * @throws ServiceException if an error occurs
     * @see #setProductId required input <code>productId</code>
     * @see #setUseCache optional input <code>useCache</code>
     * @see #getQuantityOnHandTotal required output <code>quantityOnHandTotal</code>
     * @see #getAvailableToPromiseTotal required output <code>availableToPromiseTotal</code>
     */
    public void getProductInventoryAvailable() throws ServiceException;

    /**
     * Service to prepare an inventory transfer, returns the inventory item id of the destination inventory item.
     * @throws ServiceException if an error occurs
     * @see #setInventoryItemId required input <code>inventoryItemId</code>, the inventory item being transferred
     * @see #setXferQty required input <code>xferQty</code>, the quantity to transfer from the inventory item
     * @see #getInventoryItemId required output <code>inventoryItemId</code>, the destination inventory item which can be the same as the source or a new split inventory item
     */
    public void prepareInventoryTransfer() throws ServiceException;

    /**
     * Service to cancel inventory transfer.
     * @throws ServiceException if an error occurs
     * @see #setInventoryTransferId Attribute <code>inventoryTransferId</code> (required)
     */
    public void cancelInventoryTransfer() throws ServiceException;
}
