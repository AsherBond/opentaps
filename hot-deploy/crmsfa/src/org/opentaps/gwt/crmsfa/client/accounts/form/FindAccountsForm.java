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
package org.opentaps.gwt.crmsfa.client.accounts.form;

import com.gwtext.client.widgets.form.TextField;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindPartyForm;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.listviews.AccountListView;

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
        super(UtilUi.MSG.accountId(), UtilUi.MSG.findAccounts());
        accountListView = new AccountListView();
        accountListView.init();
        addListView(accountListView);
    }

    @Override
    protected void buildFilterByNameTab(SubFormPanel p) {
        accountNameInput = new TextField(UtilUi.MSG.accountName(), "accountName", getInputLength());
        p.addField(accountNameInput);
    }

    @Override
    protected void filterByNames() {
        accountListView.filterByAccountName(accountNameInput.getText());
    }

}
