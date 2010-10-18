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
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;

/**
 * A list of Contacts.
 */
public class ContactListView extends PartyListView {

    /**
     * Default constructor.
     * Set the title to the default contact list title.
     */
    public ContactListView() {
        super();
        // the msg is built in the contructor of EntityListView
        // so we cannot pass the title directly
        setTitle(UtilUi.MSG.contactList());
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title of the list
     */
    public ContactListView(String title) {
        super(title);
        init();
    }

    @Override
    public void init() {
        init(PartyLookupConfiguration.URL_FIND_CONTACTS, "/crmsfa/control/viewContact?partyId={0}", UtilUi.MSG.crmContactId(), new String[]{
                PartyLookupConfiguration.INOUT_FIRST_NAME, UtilUi.MSG.partyFirstName(),
                PartyLookupConfiguration.INOUT_LAST_NAME, UtilUi.MSG.partyLastName()
            });
    }

    /**
     * Filters the records of the list by first name of the contact matching the given sub string.
     * @param firstName a <code>String</code> value
     */
    public void filterByFirstName(String firstName) {
        setFilter(PartyLookupConfiguration.INOUT_FIRST_NAME, firstName);
    }

    /**
     * Filters the records of the list by last name of the contact matching the given sub string.
     * @param lastName a <code>String</code> value
     */
    public void filterByLastName(String lastName) {
        setFilter(PartyLookupConfiguration.INOUT_LAST_NAME, lastName);
    }

    public void filterByAccount(String accountPartyId) {
        setFilter("partyIdTo", accountPartyId);
    }

    public void filterByOpportunity(String salesOpportunityId) {
        setFilter("salesOpportunityId", salesOpportunityId);
    }
}
