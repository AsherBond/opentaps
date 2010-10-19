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

package org.opentaps.gwt.common.server.lookup;

import java.util.Arrays;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * Implements pagination for the entity lookups and autocompleters.
 */
public class Pager {

    private static final String MODULE = Pager.class.getName();

    private InputProviderInterface provider;

    // for pagination
    private Integer pageStartIndex; // the index of the first record in the page
    private Integer pageSize; // the max number of record on a page
    // for sorting
    private String sortDirection; // the sort direction, which should be either ASC or DESC
    private String sortFieldName; // the sort field


    /**
     * Creates a new <code>Pager</code> with an <code>InputProviderInterface</code> and starts reading the parameters.
     * @param provider an <code>InputProviderInterface</code> value
     */
    public Pager(InputProviderInterface provider) {
        this.provider = provider;

        // get pager parameters
        String pageSizeStr = provider.getParameter(UtilLookup.PARAM_PAGER_LIMIT);
        // get the default page size
        pageSize = UtilLookup.DEFAULT_LIST_PAGE_SIZE;
        if (pageSizeStr != null) {
            try {
                pageSize = Integer.parseInt(pageSizeStr);
            } catch (NumberFormatException e) {
                // nothing to do, keep the default value
                Debug.logWarning("NumberFormatException while parsing the page size: " + e.getMessage(), MODULE);
            }
        }

        String startStr = provider.getParameter(UtilLookup.PARAM_PAGER_START);
        // by default starts on the first record
        pageStartIndex = 0;
        if (startStr != null) {
            try {
                pageStartIndex = Integer.parseInt(startStr);
            } catch (NumberFormatException e) {
                // nothing to do, keep the default value
                Debug.logWarning("NumberFormatException while parsing the page starting index: " + e.getMessage(), MODULE);
            }
        }

        // get sort parameters
        sortDirection = provider.getParameter(UtilLookup.PARAM_SORT_DIRECTION);
        sortFieldName = provider.getParameter(UtilLookup.PARAM_SORT_FIELD);
    }

    /**
     * Gets the field name set as the sort field in the client pager.
     * @return a <code>String</code> value
     * @see #getSortDirection
     * @see #getSortList
     */
    public String getSortFieldName() {
        return sortFieldName;
    }

    /**
     * Gets the sort direction in the client pager.
     * @return a <code>String</code> value
     * @see #getSortFieldName
     * @see #getSortList
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * Checks if the pager was given any sort parameter.
     * @return <code>true</code> if one of the sort field name or sort direction was set
     */
    public boolean hasSortParameters() {
        return (sortFieldName != null && sortDirection != null);
    }

    /**
     * Gets a list of sort SQL parameters corresponding to the client pager.
     * This list can be directly passed to the query API.
     * @return a list of <code>String</code>, or <code>null</code> if not sort parameters were given
     * @see #getSortFieldName
     * @see #getSortDirection
     */
    public List<String> getSortList() {
        if (hasSortParameters()) {
            return Arrays.asList(getSortFieldName() + " " + getSortDirection());
        } else {
            return null;
        }
    }

    /**
     * Gets the requested starting index of the page.
     * @return an <code>int</code> value
     */
    public int getPageStart() {
        return pageStartIndex;
    }

    /**
     * Gets the requested page size.
     * @return an <code>int</code> value
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Gets the requested ending index of the page.
     * @return an <code>int</code> value
     */
    public int getPageEnd() {
        return pageStartIndex + pageSize;
    }

    /**
     * Gets the ending index of the page according to the given results count.
     * @param resultsCount an <code>int</code> value
     * @return an <code>int</code> value, the minimum of results count and the ending index
     */
    public int getPageEnd(int resultsCount) {
        if (getPageEnd() > resultsCount) {
            return resultsCount;
        } else {
            return getPageEnd();
        }
    }

}
