/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.manufacturing;

import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.domain.base.entities.InventoryItemDetail;

import java.math.BigDecimal;

/**
 * Production Run entity.
 */
public class ProductionRun extends org.opentaps.domain.base.entities.WorkEffort {

    private BigDecimal totalCost;
    private BigDecimal itemsProducedTotalValue;

    /**
     * Default constructor.
     */
    public ProductionRun() {
        super();
    }

    /**
     * Gets this production run totalCost.
     * @return this production run totalCost
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getTotalCost() throws RepositoryException {
        if (totalCost == null) {
            totalCost = getRepository().getProductionRunCost(this);
        }
        return totalCost;
    }

    /**
     * Gets the total value of items produced by this production run.
     * The production run is actually creating <code>InventoryItemDetail</code> but the value depends
     *  of the parent <code>InventoryItem</code> unit cost.
     * @return the total value produced
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getItemsProducedTotalValue() throws RepositoryException {
        if (itemsProducedTotalValue == null) {
            itemsProducedTotalValue = BigDecimal.ZERO;
            for (InventoryItemDetail iid : getInventoryItemDetails()) {
                BigDecimal value = iid.getQuantityOnHandDiff().multiply(iid.getInventoryItem().getUnitCost());
                itemsProducedTotalValue = itemsProducedTotalValue.add(value);
            }
        }
        return itemsProducedTotalValue;
    }

    private ManufacturingRepositoryInterface getRepository() {
        return ManufacturingRepositoryInterface.class.cast(repository);
    }
}
