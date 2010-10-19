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

import org.opentaps.base.entities.Facility;
import org.opentaps.base.entities.InventoryItemTraceDetail;
import org.opentaps.base.entities.InventoryItemValueHistory;
import org.opentaps.base.entities.InventoryTransfer;
import org.opentaps.base.entities.Lot;
import org.opentaps.base.entities.OrderItemShipGrpInvRes;
import org.opentaps.domain.product.Product;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for Inventory to handle interaction of Inventory-related domain with the entity engine (database) and the service engine.
 */
public interface InventoryRepositoryInterface extends RepositoryInterface {

    /**
     * Facility Location Types enumeration.
     */
    public static enum FacilityLocationType {
        PRIMARY,
        BULK;
    }

    /**
     * Generally accepted methods for recording the value of inventory.
     */
    public static enum InventoryReservationOrder {
        FIFO_RECEIVED,
        LIFO_RECEIVED,
        FIFO_EXPIRE,
        LIFO_EXPIRE,
        GREATER_UNIT_COST,
        LESS_UNIT_COST
    }

    /**
     * Finds an <code>InventoryItem</code> by ID from the database.
     * @param inventoryItemId the inventory item ID
     * @return the <code>InventoryItem</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>InventoryItem</code> is found for the given id
     */
    public InventoryItem getInventoryItemById(String inventoryItemId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds an <code>InventoryItem</code> by ID from the database taking into account its class.
     * @param inventoryItemId the inventory item ID
     * @param clazz requested class of <code>InventoryItem</code>
     * @return the <code>InventoryItem</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>InventoryItem</code> is found for the given id
     */
    public org.opentaps.base.entities.InventoryItem getInventoryItemById(String inventoryItemId, Class<? extends org.opentaps.base.entities.InventoryItem> clazz) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds the <code>Facility</code> for to the given facility ID.
     * @param facilityId the facility ID
     * @return the corresponding <code>Facility</code>
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>Facility</code> is found for the given id
     */
    public Facility getFacilityById(String facilityId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds the list of <code>InventoryItem</code> for the given product id.
     * @param productId the <code>Product</code> ID.
     * @return the list of <code>InventoryItem</code>
     * @throws RepositoryException if an error occurs
     */
    public List<InventoryItem> getInventoryItemsForProductId(String productId) throws RepositoryException;

    /**
     * Finds the list of <code>InventoryItem</code> with negative availableToPromise with the same product and facility as the given <code>InventoryItem</code>.
     * @param inventoryItem the <code>InventoryItem</code>
     * @return the list of <code>InventoryItem</code> with negative availableToPromise
     * @throws RepositoryException if an error occurs
     */
    public List<InventoryItem> getInventoryItemsWithNegativeATP(InventoryItem inventoryItem) throws RepositoryException;

    /**
     * Finds the list of <code>InventoryItem</code> with negative availableToPromise for the given product and facility.
     * @param facilityId the facility ID
     * @param productId the product ID
     * @return the list of <code>InventoryItem</code> with negative availableToPromise
     * @throws RepositoryException if an error occurs
     */
    public List<InventoryItem> getInventoryItemsWithNegativeATP(String facilityId, String productId) throws RepositoryException;

    /**
     * Finds the list of <code>OrderItemShipGrpInvRes</code> for the given <code>InventoryItem</code>.
     * @param inventoryItem an <code>InventoryItem</code>
     * @return the list of <code>OrderItemShipGrpInvRes</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemShipGrpInvRes> getOrderItemShipGroupInventoryReservations(InventoryItem inventoryItem) throws RepositoryException;

    /**
     * Finds the list of <code>OrderItemShipGrpInvRes</code> for the given order item and inventory.
     * 
     * @param orderId an order identifier
     * @param orderItemSeqId an order item identifier
     * @param inventoryItemId an inventory item identifier. This argument is optional and may be <code>null</code>.
     * @param shipGroupSeqId an ship group sequence id. This argument is optional and may be <code>null</code>.
     * @return the list of <code>OrderItemShipGrpInvRes</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemShipGrpInvRes> getOrderItemShipGroupInventoryReservations(String orderId, String orderItemSeqId, String inventoryItemId, String shipGroupSeqId) throws RepositoryException;

    /**
     * Finds the list of <code>PicklistAndBinAndItem</code> related to the given <code>OrderItemShipGrpInvRes</code> with status neither CANCELLED nor PICKED.
     * @param reservation the <code>OrderItemShipGrpInvRes</code>
     * @return the list of open <code>PicklistAndBinAndItem</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PicklistAndBinAndItem> getOpenPicklistBinItems(OrderItemShipGrpInvRes reservation) throws RepositoryException;

    /**
     * Finds the list of <code>PicklistAndBinAndItem</code> with status neither CANCELLED nor PICKED for the given order, order item, ship group, and inventory item IDs.
     * @param orderId an <code>Order</code> ID
     * @param shipGroupSeqId an <code>OrderItemShipGroup</code> ID
     * @param orderItemSeqId an <code>OrderItem</code> ID
     * @param inventoryItemId an <code>InventoryItem</code> ID
     * @return the list of open <code>PicklistAndBinAndItem</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PicklistAndBinAndItem> getOpenPicklistBinItems(String orderId, String shipGroupSeqId, String orderItemSeqId, String inventoryItemId) throws RepositoryException;

    /**
     * Finds the list of <code>InventoryItem</code> matching the given parameters.
     * The <code>facilityId</code> and <code>containerId</code> arguments are optional and can be <code>null</code>.
     * @param product the <code>Product</code> of the returned inventory items
     * @param locationType the <code>FacilityLocationType</code> of the returned inventory items
     * @param method the <code>InventoryReservationOrder</code> of the returned inventory items
     * @param facilityId the facility ID of the returned inventory items, this is optional and can be <code>null</code>
     * @param containerId the container ID of the returned inventory items, this is optional and can be <code>null</code>
     * @return the list of <code>InventoryItem</code>
     * @throws RepositoryException if an error occurs
     */
    public List<InventoryItem> getInventoryItems(Product product, FacilityLocationType locationType, InventoryReservationOrder method, String facilityId, String containerId) throws RepositoryException;

    /**
     * Finds the list of pending <code>InventoryTransfer</code> for the given <code>InventoryItem</code>.
     * @param inventoryItem the <code>InventoryItem</code>
     * @return the list of pending <code>InventoryTransfer</code>
     * @throws RepositoryException if an error occurs
     */
    public List<InventoryTransfer> getPendingInventoryTransfers(InventoryItem inventoryItem) throws RepositoryException;

    /**
     * Finds the related <code>Product</code> for the given <code>InventoryItem</code>.
     * Returns the product domain object instead of the base entity.
     * @param inventoryItem the <code>InventoryItem</code>
     * @return the related <code>Product</code>
     * @throws RepositoryException if an error occurs
     */
    public Product getRelatedProduct(InventoryItem inventoryItem) throws RepositoryException;

    /**
     * Finds an <code>Lot</code> by ID from the database.
     * 
     * @param lotId the lot identifier
     * @return the <code>Lot</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>Lot</code> is found for the given id
     * 
     */
    public Lot getLotById(String lotId) throws RepositoryException, EntityNotFoundException;

    /**
     * Creates <code>InventoryItemTrace</code> record that is based on an inventory item.
     * 
     * @param inventoryItem an <code>inventoryItemId</code> value
     * @return Instance of <code>org.opentaps.domain.inventory.InventoryItemTrace</code>
     * @throws RepositoryException
     * @throws InfrastructureException
     */
    public InventoryItemTrace createInventoryTrace(org.opentaps.base.entities.InventoryItem inventoryItem) throws RepositoryException, InfrastructureException;

    /**
     * Persist in database given <code>InventoryItemTraceDetail</code> object.
     * 
     * @param event instance of <code>InventoryItemTraceDetail</code>
     * @param traceEntry parent <code>InventoryItemTrace</code>. Used to get proper sequence id.
     * @throws RepositoryException
     */
    public void createInventoryTraceEvent(InventoryItemTraceDetail event, InventoryItemTrace traceEntry) throws RepositoryException;

    /**
     * Finds inventory events which are related to direct child of the specified item. 
     * 
     * @param inventoryItem instance of <code>InventoryItem</code>
     * @return List of <code>InventoryItemTraceDetail</code>
     * @throws RepositoryException
     */
    public List<InventoryItemTraceDetail> getDerivativeInventoryTraceEvents(org.opentaps.base.entities.InventoryItem inventoryItem) throws RepositoryException;

    /**
     * Finds and returns respective <code>InventoryItemTraceDetail</code> for given inventory item id. 
     * 
     * @param inventoryItemId inventory item id value for search 
     * @param forward 
     *     Direction flag, <code>true</code> if sought InventoryItemTraceDetail is used 
     *     while traceInventoryUsageForward service collect trace events in forward direction, 
     *     <code>false</code> for traceInventoryUsageBackward
     * @return
     * @throws RepositoryException
     */
    public InventoryItemTraceDetail getSoughtTraceEntry(String inventoryItemId, boolean forward) throws RepositoryException;

    /**
     * Finds inventory item trace detail records of VARIANCE usage type which correspond to processing inventory item.
     * 
     * @param traceDetail current inventory item event
     * @param desc order direction, ascend or descend.
     * @return
     *     List of inventory usage events
     * @throws RepositoryException
     */
    public List<InventoryItemTraceDetail> findTraceEventAdjustments(InventoryItemTraceDetail traceDetail, boolean desc) throws RepositoryException;

    /**
     * Methods collects and returns collection of InventoryItemTraceDetal in backward direction (from higher levels to lower).
     *
     * @param traceDetail Starting point
     * @return
     *     List of inventory usage events
     * @throws RepositoryException
     */
    public List<InventoryItemTraceDetail> collectTraceEventsBackward(InventoryItemTraceDetail traceDetail) throws RepositoryException;

    /**
     * Methods collects and returns collection of InventoryItemTraceDetal in backward direction (from higher levels to lower).
     *
     * @param traceDetail Starting point
     * @return
     *     List of inventory usage events
     * @throws RepositoryException
     */
    public List<InventoryItemTraceDetail> collectTraceEventsForward(InventoryItemTraceDetail traceDetail) throws RepositoryException;

    /**
     * Finds the last recorded <code>InventoryItemValueHistory</code> from the database.
     * @param inventoryItem an <code>InventoryItem</code>
     * @return the <code>InventoryItemValueHistory</code> found
     * @throws RepositoryException if an error occurs
     */
    public InventoryItemValueHistory getLastInventoryItemValueHistoryByInventoryItem(InventoryItem inventoryItem) throws RepositoryException;

    /**
     * Find an <code>InventoryTransfer</code> by ID.
     * @param inventoryTransferId transfer identifier 
     * @return
     *     Instance of <code>InventoryTransfer</code>
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if transfer isn't found
     */
    public InventoryTransfer getInventoryTransferById(String inventoryTransferId) throws RepositoryException, EntityNotFoundException;

}
