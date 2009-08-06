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

package org.opentaps.gwt.common.client.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.ListAndFormPanel;
import org.opentaps.gwt.common.client.form.base.TabbedFormPanel;
import org.opentaps.gwt.common.client.listviews.EntityEditableListView;

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

    }

    protected abstract void filter();

    /**
     * Hides the tabbed form used to filter the list view.
     */
    public void hideFilters() {
        getMainFormPanel().hide();
    }

    /**
     * Shows the tabbed form used to filter the list view.
     */
    public void showFilters() {
        getMainFormPanel().show();
    }

}
