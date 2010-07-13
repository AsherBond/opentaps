/*
 * Copyright (c) 2010 - 2011 Open Source Strategies, Inc.
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

package org.opentaps.gwt.activities.client.leads.form;

/**
 * A special FindLeadsForm that adds filters for activity cutoff and category (Recent / Old / No Activity).
 */
public class FindLeadsForm extends org.opentaps.gwt.crmsfa.client.leads.form.FindLeadsForm {

    private int cutoffDays = 0;
    private boolean viewRecent = true;
    private boolean viewOld = true;
    private boolean viewNoActivity = true;

    /**
     * Default constructor.
     */
    public FindLeadsForm() {
        super();
    }

    /**
     * Builds and add the list view in the form.
     * @return a <code>LeadListView</code> value
     */
    @Override
    protected LeadListView makeLeadListView() {
        LeadListView v = new LeadListView();
        v.init();
        addListView(v);
        // get default values from the list view
        this.viewRecent = v.getViewRecent();
        this.viewOld = v.getViewOld();
        this.viewNoActivity = v.getViewNoActivity();
        return v;
    }

    public void setCutoffDays(int cutoffDays) {
        this.cutoffDays = cutoffDays;
        ((LeadListView) getListView()).filterByCutoffDays(cutoffDays);
    }

    public void setViewRecent(boolean viewRecent) {
        this.viewRecent = viewRecent;
        ((LeadListView) getListView()).filterRecent(viewRecent);
    }

    public void setViewOld(boolean viewOld) {
        this.viewOld = viewOld;
        ((LeadListView) getListView()).filterOld(viewOld);
    }

    public void setViewNoActivity(boolean viewNoActivity) {
        this.viewNoActivity = viewNoActivity;
        ((LeadListView) getListView()).filterNoActivity(viewNoActivity);
    }

    public void setActivityFilters(int cutoffDays, boolean viewRecent, boolean viewOld, boolean viewNoActivity) {
        setCutoffDays(cutoffDays);
        setViewRecent(viewRecent);
        setViewOld(viewOld);
        setViewNoActivity(viewNoActivity);
        getListView().applyFilters();
    }

}
