/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.crmsfa.opportunities.client;

import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.crmsfa.opportunities.client.form.FindOpportunitiesForm;
import org.opentaps.gwt.crmsfa.opportunities.client.form.QuickNewOpportunityForm;

import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {
    private static final String QUICK_CREATE_OPPORTUNITY_ID = "quickNewOpportunity";
    private static final String MY_OPPORTUNITIES_ID = "myOpportunities";
    private static final String FIND_OPPORTUNITIES_ID = "findOpportunities";
    private QuickNewOpportunityForm quickNewOpportunityForm = null;
    private FindOpportunitiesForm findOpportunitiesForm;
    private FindOpportunitiesForm myOpportunitiesForm;
    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found.
     */
    public void onModuleLoad() {
        if (RootPanel.get(MY_OPPORTUNITIES_ID) != null) {
            loadMyOpportunities();
        }

        if (RootPanel.get(FIND_OPPORTUNITIES_ID) != null) {
            loadFindOpportunities();
        }
        if (RootPanel.get(QUICK_CREATE_OPPORTUNITY_ID) != null) {
            loadQuickNewOpportunity();
            if (findOpportunitiesForm != null) {
                quickNewOpportunityForm.register(findOpportunitiesForm.getListView());
            }
            if (myOpportunitiesForm != null) {
                quickNewOpportunityForm.register(myOpportunitiesForm.getListView());
            }
        }
    }

    private void loadQuickNewOpportunity() {
        quickNewOpportunityForm = new QuickNewOpportunityForm();
        RootPanel.get(QUICK_CREATE_OPPORTUNITY_ID).add(quickNewOpportunityForm);
    }

    private void loadFindOpportunities() {
        findOpportunitiesForm = new FindOpportunitiesForm(true);
        RootPanel.get(FIND_OPPORTUNITIES_ID).add(findOpportunitiesForm.getMainPanel());
    }


    private void loadMyOpportunities() {
        myOpportunitiesForm = new FindOpportunitiesForm(false);
        myOpportunitiesForm.hideFilters();
        myOpportunitiesForm.getListView().filterMyOrTeamParties(getViewPref());
        myOpportunitiesForm.getListView().applyFilters();
        RootPanel.get(MY_OPPORTUNITIES_ID).add(myOpportunitiesForm.getMainPanel());
    }

    /**
     * Retrieve GWT parameter viewPref. Parameter is optional.
     * @return
     *     Possible values are <code>MY_VALUES</code> (or <code>CaseLookupConfiguration.MY_VALUES</code>) and
     *     <code>TEAM_VALUES</code> (or <code>CaseLookupConfiguration.TEAM_VALUES</code>)
     */
    private static native String getViewPref()/*-{
        return $wnd.viewPref;
    }-*/;
}