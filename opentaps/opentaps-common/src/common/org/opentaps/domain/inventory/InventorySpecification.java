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


import java.util.Arrays;
import java.util.List;


import org.opentaps.base.constants.InventoryItemTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;

/**
 * Common specifications for the Inventory domain.
 *
 * These specifications contain mapping of conceptual status to their database equivalents, as well as groupings into higher level concepts.
 * This also contain the general rules for rounding inventory quantities.
 */
public final class InventorySpecification {

    /* This class should not be constructed. */
    private InventorySpecification() { }

    /** Serialized inventory items are stored separately, each unit having its own <code>InventoryItem</code>. */
    public static final String INVENTORY_ITEM_TYPE_SERIALIZED = InventoryItemTypeConstants.SERIALIZED_INV_ITEM;

    public static final String INVENTORY_ITEM_STATUS_AVAILABLE = StatusItemConstants.InvSerializedStts.INV_AVAILABLE;
    public static final String INVENTORY_ITEM_STATUS_PROMISED = StatusItemConstants.InvSerializedStts.INV_PROMISED;
    public static final String INVENTORY_ITEM_STATUS_BEING_TRANSFERED = StatusItemConstants.InvSerializedStts.INV_BEING_TRANSFERED;
    public static final List<String> INVENTORY_ITEM_STATUSES_ON_HAND = Arrays.asList(INVENTORY_ITEM_STATUS_AVAILABLE, INVENTORY_ITEM_STATUS_PROMISED, INVENTORY_ITEM_STATUS_BEING_TRANSFERED);

}
