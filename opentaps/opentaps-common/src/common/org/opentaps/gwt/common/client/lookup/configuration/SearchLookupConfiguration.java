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

package org.opentaps.gwt.common.client.lookup.configuration;

import org.opentaps.gwt.common.client.lookup.UtilLookup;

/**
 * Defines the interface between the server and client for the Search lookups.
 * Technically not a java interface, but it defines all the constantes needed on both sides
 *  which makes the code more robust.
 */
public abstract class SearchLookupConfiguration {

    private SearchLookupConfiguration() { }

    /** The query field of the search. */
    public static final String IN_QUERY = UtilLookup.PARAM_SUGGEST_QUERY;

    /** Field containing the JSON record ID, normally it sohuld be the type + the real ID. */
    public static final String RESULT_ID = UtilLookup.SUGGEST_ID;
    /** Field containing the result title, a short string depending of the type (a person name, an email subject, etc ...). */
    public static final String RESULT_TITLE = UtilLookup.SUGGEST_TEXT;
    /** Field containing the result real ID (partyId, orderId, etc ...), differs from <code>RESULT_ID</code> as it may not be unique if the results contain multiple types. */
    public static final String RESULT_REAL_ID = "realId";
    /** Field containing the result type, for example a domain object class name. */
    public static final String RESULT_TYPE = "type";
    /** Field containing the result description, the can contain additional information on the result. */
    public static final String RESULT_DESCRIPTION = "description";

}
