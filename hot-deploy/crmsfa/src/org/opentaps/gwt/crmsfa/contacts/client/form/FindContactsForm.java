/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.gwt.crmsfa.contacts.client.form;

import com.gwtext.client.widgets.form.TextField;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindPartyForm;
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
        super(UtilUi.MSG.contactId(), UtilUi.MSG.findContacts());
        contactListView = new ContactListView();
        contactListView.setAutoLoad(autoLoad);
        contactListView.init();
        addListView(contactListView);
    }

    @Override
    protected void buildFilterByNameTab(SubFormPanel p) {
        firstNameInput = new TextField(UtilUi.MSG.firstName(), "firstName", getInputLength());
        lastNameInput = new TextField(UtilUi.MSG.lastName(), "lastName", getInputLength());
        p.addField(firstNameInput);
        p.addField(lastNameInput);
    }

    @Override
    protected void filterByNames() {
        contactListView.filterByFirstName(firstNameInput.getText());
        contactListView.filterByLastName(lastNameInput.getText());
    }

}
