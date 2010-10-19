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
import org.opentaps.gwt.common.client.lookup.configuration.SalesOrderLookupConfiguration;

public class SalesOrdersSublistView extends SalesOrderListView {

    String partyId = null;

    /**
     * Public constructor.
     * @param partyId related contact or account identifier
     */
    public SalesOrdersSublistView(String partyId) {
        super();
        this.partyId = partyId;
        init();
    }

    public void filterForParty() {
        filterByCustomerId(partyId);
        setFilter(SalesOrderLookupConfiguration.IN_DESIRED, "Y");
    };

}
