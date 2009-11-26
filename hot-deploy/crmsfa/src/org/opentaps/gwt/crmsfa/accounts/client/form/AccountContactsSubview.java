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

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.StringFieldDef;

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

                String entityViewUrl = "/crmsfa/control/viewContact?partyId={0}";
                StringFieldDef idDefinition = new StringFieldDef(PartyLookupConfiguration.INOUT_PARTY_ID);

                makeLinkColumn(UtilUi.MSG.contactId(), idDefinition, entityViewUrl, true);
                makeLinkColumn(UtilUi.MSG.crmContactName(), idDefinition, new StringFieldDef(PartyLookupConfiguration.INOUT_FRIENDLY_PARTY_NAME), entityViewUrl, true);
                makeColumn(UtilUi.MSG.city(), new StringFieldDef(PartyLookupConfiguration.INOUT_CITY));
                makeColumn(UtilUi.MSG.crmPrimaryEmail(), new StringFieldDef(PartyLookupConfiguration.OUT_EMAIL));
                makeColumn(UtilUi.MSG.crmPrimaryPhone(), new StringFieldDef(PartyLookupConfiguration.INOUT_FORMATED_PHONE_NUMBER));
                makeColumn(UtilUi.MSG.toName(), new StringFieldDef(PartyLookupConfiguration.OUT_TO_NAME));
                makeColumn(UtilUi.MSG.attnName(), new StringFieldDef(PartyLookupConfiguration.OUT_ATTENTION_NAME));
                makeColumn(UtilUi.MSG.address(), new StringFieldDef(PartyLookupConfiguration.INOUT_ADDRESS));
                makeColumn(UtilUi.MSG.address2(), new StringFieldDef(PartyLookupConfiguration.OUT_ADDRESS_2));
                makeColumn(UtilUi.MSG.stateOrProvince(), new StringFieldDef(PartyLookupConfiguration.INOUT_STATE));
                makeColumn(UtilUi.MSG.country(), new StringFieldDef(PartyLookupConfiguration.INOUT_COUNTRY));
                makeColumn(UtilUi.MSG.postalCode(), new StringFieldDef(PartyLookupConfiguration.INOUT_POSTAL_CODE));
                makeColumn(UtilUi.MSG.postalCodeExt(), new StringFieldDef(PartyLookupConfiguration.OUT_POSTAL_CODE_EXT));

                configure(PartyLookupConfiguration.URL_FIND_CONTACTS, PartyLookupConfiguration.INOUT_PARTY_ID, SortDir.ASC);

                // by default, hide non essential columns
                setColumnHidden(PartyLookupConfiguration.INOUT_PARTY_ID, true);
                setColumnHidden(PartyLookupConfiguration.INOUT_STATE, true);
                setColumnHidden(PartyLookupConfiguration.INOUT_COUNTRY, true);
                setColumnHidden(PartyLookupConfiguration.OUT_TO_NAME, true);
                setColumnHidden(PartyLookupConfiguration.OUT_ATTENTION_NAME, true);
                setColumnHidden(PartyLookupConfiguration.INOUT_ADDRESS, true);
                setColumnHidden(PartyLookupConfiguration.OUT_ADDRESS_2, true);
                setColumnHidden(PartyLookupConfiguration.INOUT_POSTAL_CODE, true);
                setColumnHidden(PartyLookupConfiguration.OUT_POSTAL_CODE_EXT, true);
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
