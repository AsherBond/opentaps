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

package org.opentaps.gwt.common.client.listviews;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;

/**
 * A list of Accounts.
 */
public class AccountListView extends PartyListView {

    /**
     * Default constructor.
     * Set the title to the default account list title.
     */
    public AccountListView() {
        super();
        // the msg is built in the contructor of EntityListView
        // so we cannot pas the title directly
        setTitle(UtilUi.MSG.accountList());
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title of the list
     */
    public AccountListView(String title) {
        super(title);
        init();
    }

    @Override
    public void init() {

        init(PartyLookupConfiguration.URL_FIND_ACCOUNTS, "/crmsfa/control/viewAccount?partyId={0}", UtilUi.MSG.accountId(), new String[]{
                PartyLookupConfiguration.INOUT_GROUP_NAME, UtilUi.MSG.accountName()
            });
    }

    /**
     * Filters the records of the list by name of the account matching the given sub string.
     * @param accountName a <code>String</code> value
     */
    public void filterByAccountName(String accountName) {
        setFilter(PartyLookupConfiguration.INOUT_GROUP_NAME, accountName);
    }

}
