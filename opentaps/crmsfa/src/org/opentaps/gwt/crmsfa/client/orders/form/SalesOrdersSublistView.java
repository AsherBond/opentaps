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
package org.opentaps.gwt.crmsfa.client.orders.form;

import org.opentaps.gwt.common.client.listviews.SalesOrderListView;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.lookup.configuration.SalesOrderLookupConfiguration;

/**
 * A list of orders for a given party.
 */
public class SalesOrdersSublistView extends SalesOrderListView {

    /**
     * Public constructor.
     */
    public SalesOrdersSublistView() {
        super();
        init();
    }

    /**
     * Sets the partyId for which to list the orders.
     * @param partyId a <code>String</code> value
     */
    public void filterByParty(String partyId) {
        setFilter(SalesOrderLookupConfiguration.INOUT_PARTY_ID, partyId, UtilLookup.OP_EQUALS);
        setFilter(SalesOrderLookupConfiguration.IN_DESIRED, "Y");
    }

}
