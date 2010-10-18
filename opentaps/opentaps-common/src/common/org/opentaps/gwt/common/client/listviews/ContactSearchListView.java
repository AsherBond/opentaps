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

package org.opentaps.gwt.common.client.listviews;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;

/**
 * A list of Contacts, results from a search.
 */
public class ContactSearchListView extends ContactListView implements SearchResultsListViewInterface {

    /**
     * Default constructor.
     * Set the title to the default contact list title.
     */
    public ContactSearchListView() {
        super();
        setTitle(UtilUi.MSG.crmContacts());
        init();
    }

    @Override
    public void init() {

        // the grid should not try to load before a query is made
        setAutoLoad(false);

        init(PartyLookupConfiguration.URL_SEARCH_CONTACTS, "/crmsfa/control/viewContact?partyId={0}", UtilUi.MSG.crmContactId(), new String[]{
                PartyLookupConfiguration.INOUT_FIRST_NAME, UtilUi.MSG.partyFirstName(),
                PartyLookupConfiguration.INOUT_LAST_NAME, UtilUi.MSG.partyLastName()
            });
    }

    /** {@inheritDoc} */
    public void search(String query) {
        setFilter(UtilLookup.PARAM_SUGGEST_QUERY, query);
        applyFilters();
        getView().setEmptyText(UtilUi.MSG.searchNoResults(query));
    }
}
