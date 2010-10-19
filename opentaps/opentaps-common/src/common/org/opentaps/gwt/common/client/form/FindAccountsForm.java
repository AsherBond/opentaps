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

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.listviews.AccountListView;

import com.gwtext.client.widgets.form.TextField;

/**
 * A combination of a accounts list view and a tabbed form used to filter that list view.
 */
public class FindAccountsForm extends FindPartyForm {

    private TextField accountNameInput;
    private final AccountListView accountListView;

    /**
     * Default constructor.
     */
    public FindAccountsForm() {
        super(UtilUi.MSG.crmAccountId(), UtilUi.MSG.crmFindAccounts());
        accountListView = new AccountListView();
        accountListView.init();
        addListView(accountListView);
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
    public FindAccountsForm(boolean autoLoad, boolean ignoreLinkColumn) {
        super(UtilUi.MSG.crmAccountId(), UtilUi.MSG.crmFindAccounts());
        accountListView = new AccountListView();
        accountListView.setAutoLoad(autoLoad);
        accountListView.setIgnoreLinkColumn(ignoreLinkColumn);
        accountListView.init();
        addListView(accountListView);
    }


    @Override
    protected void buildFilterByNameTab(SubFormPanel p) {
        accountNameInput = new TextField(UtilUi.MSG.crmAccountName(), "accountName", getInputLength());
        p.addField(accountNameInput);
    }

    @Override
    protected void filterByNames() {
        accountListView.filterByAccountName(accountNameInput.getText());
    }

}
