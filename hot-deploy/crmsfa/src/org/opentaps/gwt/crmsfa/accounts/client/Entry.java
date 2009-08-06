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

package org.opentaps.gwt.crmsfa.accounts.client;

import com.google.gwt.user.client.ui.RootPanel;
import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.crmsfa.accounts.client.form.FindAccountsForm;
import org.opentaps.gwt.crmsfa.accounts.client.form.QuickNewAccountForm;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {

    private FindAccountsForm findAccountsForm;
    private FindAccountsForm myAccountsForm;
    private QuickNewAccountForm quickNewAccountForm;

    private static final String MY_ACCOUNTS_ID = "myAccounts";
    private static final String FIND_ACCOUNTS_ID = "findAccounts";
    private static final String LOOKUP_ACCOUNTS_ID = "lookupAccounts";
    private static final String QUICK_CREATE_ACCOUNT_ID = "quickNewAccount";

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found
     */
    public void onModuleLoad() {

        if (RootPanel.get(FIND_ACCOUNTS_ID) != null) {
            loadFindAccounts();
        }

        if (RootPanel.get(MY_ACCOUNTS_ID) != null) {
            loadMyAccounts();
        }

        if (RootPanel.get(QUICK_CREATE_ACCOUNT_ID) != null) {
            loadQuickNewAccount();
            if (myAccountsForm != null) {
                quickNewAccountForm.register(myAccountsForm.getListView());
            }
            // for handling refresh of lists on contact creation
            if (findAccountsForm != null) {
                quickNewAccountForm.register(findAccountsForm.getListView());
            }
        }

        if (RootPanel.get(LOOKUP_ACCOUNTS_ID) != null) {
            loadLookupAccounts();
        }
    }

    private void loadFindAccounts() {
        findAccountsForm = new FindAccountsForm();
        RootPanel.get(FIND_ACCOUNTS_ID).add(findAccountsForm.getMainPanel());
    }

    private void loadLookupAccounts() {
        findAccountsForm = new FindAccountsForm();
        findAccountsForm.getListView().setLookupMode();
        RootPanel.get(LOOKUP_ACCOUNTS_ID).add(findAccountsForm.getMainPanel());
    }

    private void loadQuickNewAccount() {
        quickNewAccountForm = new QuickNewAccountForm();
        RootPanel.get(QUICK_CREATE_ACCOUNT_ID).add(quickNewAccountForm);
    }

    private void loadMyAccounts() {
        myAccountsForm = new FindAccountsForm();
        myAccountsForm.hideFilters();
        myAccountsForm.getListView().filterMyOrTeamParties(getViewPref());
        myAccountsForm.getListView().applyFilters();
        RootPanel.get(MY_ACCOUNTS_ID).add(myAccountsForm.getMainPanel());
    }

    /**
     * Retrieve GWT parameter viewPref. Parameter is optional.
     * @return
     *     Possible values are <code>MY_VALUES</code> (or <code>PartyLookupConfiguration.MY_VALUES</code>) and
     *     <code>TEAM_VALUES</code> (or <code>PartyLookupConfiguration.TEAM_VALUES</code>)
     */
    private static native String getViewPref()/*-{
        return $wnd.viewPref;
    }-*/;

}
