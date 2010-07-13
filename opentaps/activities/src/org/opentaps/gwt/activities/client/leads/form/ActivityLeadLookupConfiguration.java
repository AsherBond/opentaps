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

import java.util.Arrays;
import java.util.List;

/**
 * Defines the interface between the server and client for the ActivityLeadLookupConfiguration
 * Technically not a java interface, but it defines all the constants needed on both sides
 *  which makes the code more robust.
 */
public abstract class ActivityLeadLookupConfiguration {

    private ActivityLeadLookupConfiguration() { }

    // For activity filter
    public static final String IN_CUTOFF_DAYS = "cutoffDays";
    public static final String IN_SHOW_RECENT = "showRecent";
    public static final String IN_SHOW_OLD = "showOld";
    public static final String IN_SHOW_NO_ACTIVITY = "showNoActivity";

}
