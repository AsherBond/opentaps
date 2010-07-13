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

import org.opentaps.gwt.common.client.UtilUi;

/**
 * An extended list of Leads that adds filters for activity cutoff and category (Recent / Old / No Activity).
 */
public class LeadListView extends org.opentaps.gwt.common.client.listviews.LeadListView {

    private final String originalTitle;
    private Integer cutoffDays;
    private boolean viewRecent = true;
    private boolean viewOld = true;
    private boolean viewNoActivity = true;

    /**
     * Default constructor.
     */
    public LeadListView() {
        super();
        originalTitle = getTitle();
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title of the list
     */
    public LeadListView(String title) {
        super(title);
        originalTitle = getTitle();
        setTitleAccordingToCategories();
        init();
    }

    private void setTitleAccordingToCategories() {
        StringBuilder sb = new StringBuilder(originalTitle);

        if (cutoffDays != null && cutoffDays > 0 && (!viewRecent || !viewOld || !viewNoActivity) && (viewRecent || viewOld || viewNoActivity)) {
            sb.append(" -");
            if (viewRecent) {
                sb.append(" ").append(UtilUi.MSG.activitiesRecent());
            }
            if (viewOld) {
                sb.append(" ").append(UtilUi.MSG.activitiesOlder());
            }
            if (viewNoActivity) {
                sb.append(" ").append(UtilUi.MSG.activitiesNoActivity());
            }
        }
        setTitle(sb.toString());
    }

    @Override
    public void applyFilters(boolean resetPager) {
        super.applyFilters(resetPager);
        setTitleAccordingToCategories();
    }

    public boolean getViewRecent() {
        return viewRecent;
    }

    public boolean getViewOld() {
        return viewOld;
    }

    public boolean getViewNoActivity() {
        return viewNoActivity;
    }

    /**
     * Filters the records of the list by cutoff days.
     * @param cutoffDays an <code>int</code> value
     */
    public void filterByCutoffDays(int cutoffDays) {
        filterByCutoffDays(Integer.toString(cutoffDays));
    }

    /**
     * Filters the records of the list by cutoff days.
     * @param cutoffDays a <code>String</code> value
     */
    public void filterByCutoffDays(String cutoffDays) {
        this.cutoffDays = Integer.parseInt(cutoffDays);
        setFilter(ActivityLeadLookupConfiguration.IN_CUTOFF_DAYS, cutoffDays);
    }

    /**
     * Filters the records of the list in the Recent category.
     * @param viewRecent a <code>boolean</code> value
     */
    public void filterRecent(boolean viewRecent) {
        this.viewRecent = viewRecent;
        filterRecent(viewRecent ? "Y" : "N");
    }

    /**
     * Filters the records of the list in the Recent category.
     * @param viewRecent a <code>String</code> value
     */
    public void filterRecent(String viewRecent) {
        this.viewRecent = "Y".equalsIgnoreCase(viewRecent);
        setFilter(ActivityLeadLookupConfiguration.IN_SHOW_RECENT, viewRecent);
    }

    /**
     * Filters the records of the list in the Old category.
     * @param viewOld a <code>boolean</code> value
     */
    public void filterOld(boolean viewOld) {
        this.viewOld = viewOld;
        filterOld(viewOld ? "Y" : "N");
    }

    /**
     * Filters the records of the list in the Old category.
     * @param viewOld a <code>String</code> value
     */
    public void filterOld(String viewOld) {
        this.viewOld = "Y".equalsIgnoreCase(viewOld);
        setFilter(ActivityLeadLookupConfiguration.IN_SHOW_OLD, viewOld);
    }

    /**
     * Filters the records of the list in the NoActivity category.
     * @param viewNoActivity a <code>boolean</code> value
     */
    public void filterNoActivity(boolean viewNoActivity) {
        this.viewNoActivity = viewNoActivity;
        filterNoActivity(viewNoActivity ? "Y" : "N");
    }

    /**
     * Filters the records of the list in the NoActivity category.
     * @param viewNoActivity a <code>String</code> value
     */
    public void filterNoActivity(String viewNoActivity) {
        this.viewNoActivity = "Y".equalsIgnoreCase(viewNoActivity);
        setFilter(ActivityLeadLookupConfiguration.IN_SHOW_NO_ACTIVITY, viewNoActivity);
    }
}
