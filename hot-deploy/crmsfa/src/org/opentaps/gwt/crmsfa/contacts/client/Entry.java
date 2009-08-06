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

package org.opentaps.gwt.crmsfa.contacts.client;

import com.google.gwt.user.client.ui.RootPanel;
import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.crmsfa.contacts.client.form.FindContactsForm;
import org.opentaps.gwt.crmsfa.contacts.client.form.QuickNewContactForm;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {

    private FindContactsForm findContactsForm;
    private FindContactsForm myContactsForm;
    private QuickNewContactForm quickNewContactForm;

    private static final String MY_CONTACTS_ID = "myContacts";
    private static final String FIND_CONTACTS_ID = "findContacts";
    private static final String LOOKUP_CONTACTS_ID = "lookupContacts";
    private static final String QUICK_CREATE_CONTACT_ID = "quickNewContact";

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found.
     */
    public void onModuleLoad() {

        if (RootPanel.get(FIND_CONTACTS_ID) != null) {
            loadFindContacts();
        }

        if (RootPanel.get(MY_CONTACTS_ID) != null) {
            loadMyContacts();
        }

        if (RootPanel.get(QUICK_CREATE_CONTACT_ID) != null) {
            loadQuickNewContact();
            // for handling refresh of lists on contact creation
            if (myContactsForm != null) {
                quickNewContactForm.register(myContactsForm.getListView());
            }
            if (findContactsForm != null) {
                quickNewContactForm.register(findContactsForm.getListView());
            }
        }

        if (RootPanel.get(LOOKUP_CONTACTS_ID) != null) {
            loadLookupContacts();
        }
    }

    private void loadFindContacts() {
        findContactsForm = new FindContactsForm();
        RootPanel.get(FIND_CONTACTS_ID).add(findContactsForm.getMainPanel());
    }

    private void loadLookupContacts() {
        findContactsForm = new FindContactsForm();
        findContactsForm.getListView().setLookupMode();
        RootPanel.get(LOOKUP_CONTACTS_ID).add(findContactsForm.getMainPanel());
    }

    private void loadMyContacts() {
        myContactsForm = new FindContactsForm(false);
        myContactsForm.hideFilters();
        myContactsForm.getListView().filterMyOrTeamParties(PartyLookupConfiguration.MY_VALUES);
        myContactsForm.getListView().applyFilters();
        RootPanel.get(MY_CONTACTS_ID).add(myContactsForm.getMainPanel());
    }

    private void loadQuickNewContact() {
        quickNewContactForm = new QuickNewContactForm();
        RootPanel.get(QUICK_CREATE_CONTACT_ID).add(quickNewContactForm);
    }

}
