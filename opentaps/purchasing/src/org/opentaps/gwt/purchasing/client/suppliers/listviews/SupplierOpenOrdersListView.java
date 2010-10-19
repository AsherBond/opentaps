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
package org.opentaps.gwt.purchasing.client.suppliers.listviews;

import org.opentaps.gwt.common.client.listviews.PurchaseOrderListView;

/**
 * A list view of open purchase orders for a given supplier.
 */
public class SupplierOpenOrdersListView extends PurchaseOrderListView {

    /**
     * Public constructor.
     * @param partyId related supplier party ID
     */
    public SupplierOpenOrdersListView(String partyId) {
        super();
        init();
        filterBySupplierId(partyId);
        applyFilters();
    }

}
