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

package org.opentaps.gwt.crmsfa.leads.client;

import com.google.gwt.user.client.ui.RootPanel;
import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.crmsfa.leads.client.form.FindLeadsForm;
import org.opentaps.gwt.crmsfa.leads.client.form.QuickNewLeadForm;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {

    private FindLeadsForm findLeadsForm;
    private FindLeadsForm myLeadsForm;
    private QuickNewLeadForm quickNewLeadForm;

    private static final String FIND_LEADS_ID = "findLeads";
    private static final String MY_LEADS_ID = "myLeads";
    private static final String LOOKUP_LEADS_ID = "lookupLeads";
    private static final String QUICK_CREATE_LEAD_ID = "quickNewLead";

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found
     */
    public void onModuleLoad() {

        if (RootPanel.get(MY_LEADS_ID) != null) {
            loadMyLeads();
        }

        if (RootPanel.get(FIND_LEADS_ID) != null) {
            loadFindLeads();
        }

        if (RootPanel.get(QUICK_CREATE_LEAD_ID) != null) {
            loadQuickNewLead();
            // for handling refresh of lists on contact creation
            if (findLeadsForm != null) {
                quickNewLeadForm.register(findLeadsForm.getListView());
            }
            if (myLeadsForm != null) {
                quickNewLeadForm.register(myLeadsForm.getListView());
            }
        }

        if (RootPanel.get(LOOKUP_LEADS_ID) != null) {
            loadLookupLeads();
        }
    }

    private void loadFindLeads() {
        findLeadsForm = new FindLeadsForm();
        RootPanel.get(FIND_LEADS_ID).add(findLeadsForm.getMainPanel());
    }

    private void loadLookupLeads() {
        findLeadsForm = new FindLeadsForm();
        findLeadsForm.getListView().setLookupMode();
        RootPanel.get(LOOKUP_LEADS_ID).add(findLeadsForm.getMainPanel());
    }

    private void loadQuickNewLead() {
        quickNewLeadForm = new QuickNewLeadForm();
        RootPanel.get(QUICK_CREATE_LEAD_ID).add(quickNewLeadForm);
    }

    private void loadMyLeads() {
        myLeadsForm = new FindLeadsForm();
        myLeadsForm.hideFilters();
        myLeadsForm.getListView().filterMyOrTeamParties(PartyLookupConfiguration.MY_VALUES);
        myLeadsForm.getListView().applyFilters();
        RootPanel.get(MY_LEADS_ID).add(myLeadsForm.getMainPanel());
    }
}
