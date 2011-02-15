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

import java.util.ArrayList;
import java.util.List;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.listviews.PartyListView;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;

import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Position;
import com.gwtext.client.data.Record;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.grid.ColumnModel;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.grid.event.GridCellListenerAdapter;
import com.gwtext.client.widgets.layout.FitLayout;


public class LookupAccountsWindow extends Window {
    private static final String MODULE = LookupAccountsWindow.class.getName();

    private static final int FORM_WIDTH  = 800;
    private static final int FORM_HEIGHT = 680;

    private final FindAccountsForm findAccountsForm = new FindAccountsForm(true, true);
    private final PartyListView listView;

    private List<FormNotificationInterface<String>> formNotificationListeners = new ArrayList<FormNotificationInterface<String>>();

    public LookupAccountsWindow(boolean modal, boolean resizable) {
        super(UtilUi.MSG.crmFindAccounts(), FORM_WIDTH, FORM_HEIGHT, modal, resizable);
        listView = findAccountsForm.getListView();
        listView.setLookupMode();
    }


    /** {@inheritDoc} */
    @Override
    protected void initComponent() {
        super.initComponent();

        // setup window properties
        setModal(true);
        setResizable(true);
        setLayout(new FitLayout());
        setPaddings(5);
        setButtonAlign(Position.RIGHT);

        setCloseAction(Window.HIDE);
        setPlain(false);

        // setup inner form
        Panel innerPanel = findAccountsForm.getMainPanel();
        innerPanel.setPaddings(15);
        innerPanel.setBaseCls("x-plain");

        findAccountsForm.getListView().addGridCellListener(new GridCellListenerAdapter() {

            /** {@inheritDoc} */
            @Override
            public void onCellClick(GridPanel grid, int row, int col, EventObject e) {
                ColumnModel model = grid.getColumnModel();
                if (!PartyLookupConfiguration.INOUT_EMAIL.equals(model.getColumnId(col))) {
                    Record record = findAccountsForm.getListView().getStore().getRecordAt(row);
                    if (record != null) {
                        // retrieve selected record and call event handler
                        LookupAccountsWindow.this.onSelection(record.getAsString(PartyLookupConfiguration.INOUT_PARTY_ID));
                    }
                }
            }
            
        });

    }

    /**
     * Insert inner panel into frame window.
     */
    public void create() {
        add(findAccountsForm.getMainPanel());
    }

    /**
     * Setup listener that will be used later to fire selection.
     * @param listener An instance of the FormNotificationInterface
     */
    public void register(FormNotificationInterface<String> listener) {
        if (listener != null) {
            formNotificationListeners.add(listener);
        }
    }

    /**
     * Handle user's selection.
     * The method close lookup window, look over registered listeners and notify each about
     * user's selection.
     * @param partyId selected contact party identifier
     */
    private void onSelection(String partyId) {
        for (FormNotificationInterface<String> listener : formNotificationListeners) {
            listener.notifySuccess(partyId);
        }
        hide();
    }
}
