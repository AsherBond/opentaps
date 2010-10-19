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
package org.opentaps.foundation.repository;

/**
 * Base interface for the lookup repositories.
 * Provides accessors for the pagination settings.
 */
public interface LookupRepositoryInterface extends RepositoryInterface {

    /**
     * Sets if the service should paginate the results, default to <code>true</code>.
     * @param enable a <code>boolean</code> value
     */
    public void enablePagination(boolean enable);

    /**
     * Checks if the pagination is enabled for this service.
     * @return a <code>boolean</code> value
     */
    public boolean usePagination();

    /**
     * Sets the number of search results to return per page.
     * @param pageSize an <code>int</code> value
     */
    public void setPageSize(int pageSize);

    /**
     * Gets the number of search results to return per page.
     * @return the number of search results to return per page
     */
    public int getPageSize();

    /**
     * Sets the starting index of the search results.
     * @param pageStart an <code>int</code> value
     */
    public void setPageStart(int pageStart);

    /**
     * Gets the starting index of the search results.
     * @return the starting index of the search results.
     */
    public int getPageStart();

    /**
     * Gets the ending index of the search results.
     * @return the ending index of the search results.
     */
    public int getPageEnd();

    /**
     * Gets the total number of results for the search.
     * @return an <code>int</code> value
     */
    public int getResultSize();

}
