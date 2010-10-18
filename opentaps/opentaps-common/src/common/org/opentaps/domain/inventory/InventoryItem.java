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
import java.util.List;

import org.opentaps.base.entities.InventoryItemValueHistory;
import org.opentaps.base.entities.InventoryTransfer;
import org.opentaps.base.entities.OrderItemShipGrpInvRes;
import org.opentaps.domain.product.Product;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Inventory Item entity and domain.
 */
public class InventoryItem extends org.opentaps.base.entities.InventoryItem {

    private List<InventoryTransfer> pendingInventoryTransfers;
    private List<InventoryItem> inventoryItemsWithNegativeATP;
    private List<OrderItemShipGrpInvRes> reservations;
    private Product product;
    private BigDecimal quantityPendingTransfer;
    private BigDecimal standardCost;

    /**
     * Default constructor.
     */
    public InventoryItem() {
        super();
    }

    /**
     * Checks if this inventory item is serialized.
     * @return a <code>Boolean</code> value
     */
    public Boolean isSerialized() {
        return InventorySpecification.INVENTORY_ITEM_TYPE_SERIALIZED.equals(this.getInventoryItemTypeId());
    }

    /**
     * Checks if this inventory item is available on hand.
     * @return a <code>Boolean</code> value
     */
    public Boolean isOnHand() {
        String statusId = this.getStatusId();
        return InventorySpecification.INVENTORY_ITEM_STATUSES_ON_HAND.contains(statusId);
    }

    /**
     * Checks if this inventory item is available to promise.
     * @return a <code>Boolean</code> value
     */
    public Boolean isAvailableToPromise() {
        return InventorySpecification.INVENTORY_ITEM_STATUS_AVAILABLE.equals(this.getStatusId());
    }

    /**
     * Gets the net quantity on hand for this inventory item.
     * @return the net quantity on hand
     */
    public BigDecimal getNetQOH() {
        if (isSerialized()) {
            if (isOnHand()) {
                return BigDecimal.ONE;
            }
        } else {
            BigDecimal qty = getQuantityOnHandTotal();
            if (qty != null) {
                return qty;
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the net quantity available to promise for this inventory item.
     * @return the net quantity available to promise
     */
    public BigDecimal getNetATP() {
        if (isSerialized()) {
            if (isAvailableToPromise()) {
                return BigDecimal.ONE;
            }
        } else {
            BigDecimal qty = getAvailableToPromiseTotal();
            if (qty != null) {
                return qty;
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the list of similar <code>InventoryItem</code> with negative available to promise quantity for this inventory item.
     * Similar <code>InventoryItem</code> have the same product and facility as this inventory item.
     * @return the list of similar <code>InventoryItem</code>
     * @throws RepositoryException if an error occurs
     */
    public List<InventoryItem> getSimilarInventoryItemsWithNegativeATP() throws RepositoryException {
        if (inventoryItemsWithNegativeATP == null) {
            inventoryItemsWithNegativeATP = getRepository().getInventoryItemsWithNegativeATP(this);
        }
        return inventoryItemsWithNegativeATP;
    }

    /**
     * Gets the list of <code>OrderItemShipGrpInvRes</code> related to this inventory item.
     * @return the list of <code>OrderItemShipGrpInvRes</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemShipGrpInvRes> getOrderItemShipGroupInventoryReservations() throws RepositoryException {
        if (reservations == null) {
            reservations = getRepository().getOrderItemShipGroupInventoryReservations(this);
        }
        return reservations;
    }

    /**
     * Gets the list of pending <code>InventoryTransfer</code> for this inventory item.
     * @return the list of pending <code>InventoryTransfer</code>
     * @throws RepositoryException if an error occurs
     */
    public List<InventoryTransfer> getPendingInventoryTransfers()  throws RepositoryException {
        if (pendingInventoryTransfers == null) {
            pendingInventoryTransfers = getRepository().getPendingInventoryTransfers(this);
        }
        return pendingInventoryTransfers;
    }

    /**
     * Gets the total quantity pending inventory transfer for this inventory item.
     * @return the quantity pending inventory transfer
     * @throws RepositoryException if an error occurs
     * @see #getPendingInventoryTransfers
     */
    public BigDecimal getPendingInventoryTransferQuantity()  throws RepositoryException {
        if (quantityPendingTransfer == null) {
            quantityPendingTransfer = BigDecimal.ZERO;
            if (!getPendingInventoryTransfers().isEmpty()) {
                quantityPendingTransfer = getNetQOH();
            }
        }
        return quantityPendingTransfer;
    }

    /**
     * Gets the <code>Product</code> for this inventory item.
     * Returns the product domain object instead of the base entity.
     * @return the <code>Product</code>, or <code>null</code> if it is not found
     * @throws RepositoryException if an error occurs
     */
    @Override
    public Product getProduct() throws RepositoryException {
        if (product == null) {
            product = getRepository().getRelatedProduct(this);
        }
        return product;
    }

    /**
     * Gets the standard cost for this inventory item.
     * @return the standard cost, or <code>null</code> if no product is related to this inventory item
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getStandardCost() throws RepositoryException {
        if (standardCost == null && this.getProduct() != null) {
            standardCost = this.getProduct().getStandardCost(this.getCurrencyUomId());
        }
        return standardCost;
    }

    private InventoryRepositoryInterface getRepository() {
        return InventoryRepositoryInterface.class.cast(repository);
    }

    /**
     * Gets the old unit cost for this inventory item.
     * @return the old unit cost of inventory item
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getOldUnitCost() throws RepositoryException {
        BigDecimal oldUnitCost = null;
        InventoryItemValueHistory inventoryItemValueHistory = this.getRepository().getLastInventoryItemValueHistoryByInventoryItem(this);
        if (inventoryItemValueHistory != null) {
            oldUnitCost = inventoryItemValueHistory.getUnitCost();
        }
        return oldUnitCost;
    }
}
