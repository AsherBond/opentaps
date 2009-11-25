/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.gwt.crmsfa.accounts.client.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindPartyForm;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.listviews.ContactListView;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;

/**
 * A combination of a contacts list view and a tabbed form used to filter that list view.
 */
public class AccountContactsSubview extends FindPartyForm {

    private final ContactListView contactListView;

    /**
     * Default constructor.
     */
    public AccountContactsSubview() {
        this(true);
    }

    /**
     * Constructor with autoLoad parameter, use this constructor if some filters need to be set prior to loading the grid data.
     * @param autoLoad sets the grid autoLoad parameter, set to <code>false</code> if some filters need to be set prior to loading the grid data
     */
    public AccountContactsSubview(boolean autoLoad) {
        super(UtilUi.MSG.contactId(), UtilUi.MSG.findContacts());
        contactListView = new ContactListView() {

            /** {@inheritDoc} */
            @Override
            public void init() {
                super.init(PartyLookupConfiguration.URL_FIND_CONTACTS, "/crmsfa/control/viewContact?partyId={0}", UtilUi.MSG.contactId(), new String[]{
                    PartyLookupConfiguration.INOUT_FIRST_NAME, UtilUi.MSG.firstName(),
                    PartyLookupConfiguration.INOUT_LAST_NAME, UtilUi.MSG.lastName()
                });
                setColumnHidden(PartyLookupConfiguration.INOUT_STATE, true);
                setColumnHidden(PartyLookupConfiguration.INOUT_ADDRESS, true);
            }

        };
        contactListView.setAutoLoad(autoLoad);
        contactListView.init();
        addListView(contactListView);
    }

    @Override
    protected void buildFilterByNameTab(SubFormPanel p) {
        // do nothing
    }

    @Override
    protected void filterByNames() {
        // do nothing
    }

}
