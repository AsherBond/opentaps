/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.crmsfa.cases.client;

import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.crmsfa.cases.client.form.FindCasesForm;
import org.opentaps.gwt.crmsfa.cases.client.form.QuickNewCaseForm;

import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {

    private static final String QUICK_CREATE_CASE_ID = "quickNewCase";
    private static final String MY_CASES_ID = "myCases";
    private static final String FIND_CASES_ID = "findCases";
    private QuickNewCaseForm quickNewCaseForm = null;

    private FindCasesForm findCasesForm;
    private FindCasesForm myCasesForm;
    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found.
     */
    public void onModuleLoad() {
        if (RootPanel.get(FIND_CASES_ID) != null) {
            loadFindCases();
        }
        if (RootPanel.get(MY_CASES_ID) != null) {
            loadMyCases();
        }
        if (RootPanel.get(QUICK_CREATE_CASE_ID) != null) {
            loadQuickNewCase();
            if (findCasesForm != null) {
                quickNewCaseForm.register(findCasesForm.getListView());
            }
            if (myCasesForm != null) {
                quickNewCaseForm.register(myCasesForm.getListView());
            }
        }
    }

    private void loadQuickNewCase() {
        quickNewCaseForm = new QuickNewCaseForm();
        RootPanel.get(QUICK_CREATE_CASE_ID).add(quickNewCaseForm);
    }

    private void loadFindCases() {
        findCasesForm = new FindCasesForm(true);
        RootPanel.get(FIND_CASES_ID).add(findCasesForm.getMainPanel());
    }


    private void loadMyCases() {
        myCasesForm = new FindCasesForm(false);
        myCasesForm.hideFilters();
        myCasesForm.getListView().filterMyOrTeamParties(getViewPref());
        myCasesForm.getListView().applyFilters();
        RootPanel.get(MY_CASES_ID).add(myCasesForm.getMainPanel());
    }

    /**
     * Retrieve GWT parameter viewPref. Parameter is optional.
     * @return
     *     Possible values are <code>MY_VALUES</code> (or <code>OpportunityLookupConfiguration.MY_VALUES</code>) and
     *     <code>TEAM_VALUES</code> (or <code>OpportunityLookupConfiguration.TEAM_VALUES</code>)
     */
    private static native String getViewPref()/*-{
        return $wnd.viewPref;
    }-*/;
}