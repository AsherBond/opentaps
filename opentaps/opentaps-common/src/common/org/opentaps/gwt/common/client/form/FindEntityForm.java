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

import com.gwtext.client.widgets.Component;
import com.gwtext.client.widgets.form.event.FormPanelListenerAdapter;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.ListAndFormPanel;
import org.opentaps.gwt.common.client.form.base.TabbedFormPanel;
import org.opentaps.gwt.common.client.listviews.EntityEditableListView;
import org.opentaps.gwt.common.client.lookup.UtilLookup;

/**
 * Base class for the common Find form + list view pattern.
 * @param <TLIST> the list view class that is contained in this widget
 * @see FindPartyForm
 */
public abstract class FindEntityForm<TLIST extends EntityEditableListView> extends ListAndFormPanel<TabbedFormPanel, TLIST> {

    private final TabbedFormPanel mainForm;

    /**
     * Constructor.
     * @param findButtonLabel the label for the find button of the filter form
     */
    public FindEntityForm(String findButtonLabel) {
        super();
        setFormTitle(UtilUi.MSG.findBy());

        // main form panel that will contain the tab panel and the submit button
        // override the submit method to call filter() instead of trying to POST the form
        mainForm = new TabbedFormPanel() {
                @Override public void submit() { filter(); }
            };
        addMainForm(mainForm);

        mainForm.addStandardSubmitButton(findButtonLabel);

        // a special URL parameter to auto submit the form
        // used for in combination with passing the fields as URL parameter
        mainForm.addListener(new FormPanelListenerAdapter() {
                @Override public void onRender(Component c) {
                    if (UtilLookup.hasAutoSubmitParameter()) {
                        filter();
                    }
                }
            });

    }

    protected abstract void filter();

    /**
     * Hides the tabbed form used to filter the list view.
     */
    public void hideFilters() {
        getMainFormPanel().hide();
        getSpacerPanel().hide();
    }

    /**
     * Shows the tabbed form used to filter the list view.
     */
    public void showFilters() {
        getSpacerPanel().show();
        getMainFormPanel().show();
    }

}
