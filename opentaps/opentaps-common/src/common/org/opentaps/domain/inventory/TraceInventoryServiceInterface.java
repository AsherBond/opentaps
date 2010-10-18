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

import java.util.List;

import org.opentaps.base.entities.InventoryItemTraceDetail;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;


/**
 * Interface for inventory tracing services.
 */
public interface TraceInventoryServiceInterface extends ServiceInterface {

    /**
     * Sets lot identifier.
     * Optional attribute from:<br/> 
     * {@link #traceInventoryUsage()}<br/>
     * {@link #traceInventoryUsageForward()}<br/>
     * {@link #traceInventoryUsageBackward()}.
     * @param lotId a <code>String</code> value, PK of <code>Lot</code> entity.
     */
    public void setLotId(String lotId);

    /**
     * Sets inventory item identifier.
     * Optional attribute from:<br/> 
     * {@link #traceInventoryUsage()}<br/>
     * {@link #traceInventoryUsageForward()}<br/>
     * {@link #traceInventoryUsageBackward()}.
     * @param inventoryItemId a <code>String</code> value, PK of <code>InventoryItem</code> entity.  
     */
    public void setInventoryItemId(String inventoryItemId);

    /**
     * Sets trace direction with starting point at given inventoryItemId(s).<br/>
     * Required attribute for {@link #traceInventoryUsage()}
     * @param direction may take either "FORWARD" or "BACKWARD" on value
     */
    public void setTraceDirection(String direction);

    /**
     * Gets the list of items, which are events in inventory item usage history. <br/>
     * Optional attribute from:<br/> 
     * {@link #traceInventoryUsage()}<br/>
     * {@link #traceInventoryUsageForward()}<br/>
     * {@link #traceInventoryUsageBackward()}.
     * @return Every item is instance of <code>InventoryItemTraceDetail</code> entity.
     * @see org.opentaps.base.entities.InventoryItemTraceDetail
     */
    public List<List<InventoryItemTraceDetail>> getUsageLog();

    /**
     * This service wrap calls to other inventory tracing services for convenience' sake.
     * @throws ServiceException if an error occurs
     * @see #setDocumentType required input <code>documentType</code>
     * @see #setDocumentId required input <code>documentId</code>
     * @see #getUsageForward required output <code>usageForward</code>
     * @see #getUsageBackward required output <code>usageBackward</code>
     */
    public void traceInventoryUsage() throws ServiceException;

    /**
     * Clear the InventoryItemTrace and InventoryItemTraceDetail entities.<br/>
     * If no inventoryItemTraceId is supplied, then clear all of them. 
     * @throws ServiceException if an error occurs
     * @see #setInventoryItemTraceId(String) optional input <code>inventoryItemTraceId</code>
     */
    public void clearInventoryTrace() throws ServiceException;

    /**
     * Builds the InventoryItemTrace and InventoryItemTraceDetail entities.<br/> 
     * It can build trace data for an inventoryItemId or a lotId, or a can just 
     * do it for all inventory items.
     * @throws ServiceException if an error occurs
     * @see #setInventoryItemId(String) optional input <code>inventoryItemId</code>
     * @see #setLotId(String) optional input <code>lotId</code>
     */
    public void buildInventoryTrace() throws ServiceException;

    /**
     * Trace all the inventory usage of starting with an inventory item or a lot ID
     * in backward direction.
     * @throws ServiceException if an error occurs
     * @see #setInventoryItemId(String) optional input <code>inventoryItemId</code>
     * @see #setLotId(String) optional input <code>lotId</code>
     * @see #getUsageBackward() optional output <code>usageBackward</code>
     */
    public void traceInventoryUsageBackward() throws ServiceException;

    /**
     * Trace all the inventory usage of starting with an inventory item or a lot ID
     * in forward direction.
     * @throws ServiceException if an error occurs
     * @see #setInventoryItemId(String) optional input <code>inventoryItemId</code>
     * @see #setLotId(String) optional input <code>lotId</code>
     * @see #getUsageForward() optional output <code>usageForward</code>
     */
    public void traceInventoryUsageForward() throws ServiceException;

}
