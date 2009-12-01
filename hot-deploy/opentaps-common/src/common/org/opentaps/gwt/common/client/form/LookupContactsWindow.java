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

import com.google.gwt.user.client.Window;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.FormPanel;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.grid.event.GridRowListenerAdapter;

public class LookupContactsWindow extends PopupFormWindow {

    private static final int FORM_WIDTH  = 800;
    private static final int FORM_HEIGHT = 700;

    private final FindContactsForm findContactsForm = new FindContactsForm();

    public LookupContactsWindow(String title, int width, int height, boolean modal, boolean resizable) {
        super(title, FORM_WIDTH, FORM_HEIGHT);
    }

    /** {@inheritDoc} */
    @Override
    protected Panel getInnerPanel() {
        return findContactsForm.getMainPanel();
    }


    @Override
    protected void initFields(FormPanel container) {
    }

    /** {@inheritDoc} */
    @Override
    protected void initComponent() {
        super.initComponent();

        findContactsForm.getListView().addGridRowListener(new GridRowListenerAdapter() {

            /** {@inheritDoc} */
            @Override
            public void onRowClick(GridPanel grid, int rowIndex, EventObject e) {
                Window.alert(findContactsForm.getListView().getStore().getRecordAt(rowIndex).getAsString("partyId"));
            }
            
        });
    }

}
