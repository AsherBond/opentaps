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
package org.opentaps.gwt.common.client.form;

import org.opentaps.gwt.common.client.listviews.PartyListView;

import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.grid.event.GridRowListenerAdapter;
import com.gwtext.client.widgets.layout.FitLayout;

public class LookupContactsWindow extends Window {

    private static final int FORM_WIDTH  = 800;
    private static final int FORM_HEIGHT = 700;

    private final FindContactsForm findContactsForm = new FindContactsForm();
    private final PartyListView listView;

    public LookupContactsWindow(boolean modal, boolean resizable) {
        super("Lookup Contacts", FORM_WIDTH, FORM_HEIGHT, modal, resizable);
        listView = findContactsForm.getListView();
        listView.setLookupMode();
    }


    /** {@inheritDoc} */
    @Override
    protected void initComponent() {
        super.initComponent();

        // setup window properties
        setModal(true);
        setResizable(false);
        setLayout(new FitLayout());
        setPaddings(5);
        setButtonAlign(Position.RIGHT);

        setCloseAction(Window.HIDE);
        setPlain(false);

        // setup inner form
        Panel innerPanel = findContactsForm.getMainPanel();
        innerPanel.setPaddings(15);
        innerPanel.setBaseCls("x-plain");

        findContactsForm.getListView().addGridRowListener(new GridRowListenerAdapter() {

            /** {@inheritDoc} */
            @Override
            public void onRowClick(GridPanel grid, int rowIndex, EventObject e) {
                com.google.gwt.user.client.Window.alert(findContactsForm.getListView().getStore().getRecordAt(rowIndex).getAsString("partyId"));
            }
            
        });
    }

    public void create() {
        add(findContactsForm.getMainPanel());
    }

}
