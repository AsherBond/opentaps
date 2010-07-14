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

package org.opentaps.gwt.activities.client;

import com.google.gwt.user.client.ui.RootPanel;
import org.opentaps.gwt.activities.client.leads.form.FindLeadsForm;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.common.client.BaseEntry;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {

    private static final String MY_LEADS_ID = "myLeadsWithActivities";
    private FindLeadsForm myLeadsForm;

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found.
     */
    public void onModuleLoad() {
        if (RootPanel.get(MY_LEADS_ID) != null) {
            loadMyLeads();
        }
    }

    private void loadMyLeads() {
        myLeadsForm = new FindLeadsForm();
        myLeadsForm.hideFilters();
        myLeadsForm.getListView().filterMyOrTeamParties(PartyLookupConfiguration.MY_VALUES);
        myLeadsForm.getListView().applyFilters();
        RootPanel.get(MY_LEADS_ID).add(myLeadsForm.getMainPanel());
        setActivitiesFiltersMethods(myLeadsForm);
    }

    // this registers a callable Javascript method
    private native void setActivitiesFiltersMethods(FindLeadsForm f) /*-{
        $wnd.gwtLeadsListSetActivityFilters = function(cutoffDays, viewRecent, viewOld, viewNoActivity) {
            f.@org.opentaps.gwt.activities.client.leads.form.FindLeadsForm::setActivityFilters(IZZZ)(cutoffDays, viewRecent, viewOld, viewNoActivity);
        };
    }-*/;

}
