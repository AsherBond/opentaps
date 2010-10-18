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

package org.opentaps.gwt.common.client.form;

import com.gwtext.client.widgets.form.TextField;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.listviews.ContactListView;

/**
 * A combination of a contacts list view and a tabbed form used to filter that list view.
 */
public class FindContactsForm extends FindPartyForm {

    private TextField firstNameInput;
    private TextField lastNameInput;
    private final ContactListView contactListView;

    /**
     * Default constructor.
     */
    public FindContactsForm() {
        this(true);
    }

    /**
     * Constructor with autoLoad parameter, use this constructor if some filters need to be set prior to loading the grid data.
     * @param autoLoad sets the grid autoLoad parameter, set to <code>false</code> if some filters need to be set prior to loading the grid data
     */
    public FindContactsForm(boolean autoLoad) {
        this(autoLoad, false);
    }

    /**
     * Constructor with autoLoad and ingoreLinkColumn parameters. First one is useful if need to apply some filters
     * prior to loading the grid data. And use ignoreLinkColumn to rid of hyperlinks in partyId column. We should use
     * grid without links when it embed into another GWT window and selection is handled internally, in some listener,
     * without redirecting browser to new URL.
     *
     * @param autoLoad sets the grid autoLoad parameter, set to <code>false</code> if some filters need to be set prior to loading the grid data
     * @param ignoreLinkColumn creates id column as plain column w/o hypelinks if true.
     */
    public FindContactsForm(boolean autoLoad, boolean ignoreLinkColumn) {
        super(UtilUi.MSG.contactId(), UtilUi.MSG.crmFindContacts());
        contactListView = new ContactListView();
        contactListView.setAutoLoad(autoLoad);
        contactListView.setIgnoreLinkColumn(ignoreLinkColumn);
        contactListView.init();
        addListView(contactListView);
    }

    @Override
    protected void buildFilterByNameTab(SubFormPanel p) {
        firstNameInput = new TextField(UtilUi.MSG.partyFirstName(), "firstName", getInputLength());
        lastNameInput = new TextField(UtilUi.MSG.partyLastName(), "lastName", getInputLength());
        p.addField(firstNameInput);
        p.addField(lastNameInput);
    }

    @Override
    protected void filterByNames() {
        contactListView.filterByFirstName(firstNameInput.getText());
        contactListView.filterByLastName(lastNameInput.getText());
    }

}
