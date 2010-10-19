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
package org.opentaps.domain.search;

import java.util.List;
import java.util.Set;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;

/**
 * Interface for search services.
 * A search is defined by a <code>keywords</code> string to search for
 *  and the pagination settings.
 * Once those are set, the {@link #search} method performs the search
 *  and the client can use {@link #getResults} and {@link #getResultSize}.
 */
public interface SearchServiceInterface extends ServiceInterface {

    /**
     * Sets the keywords to search for.
     * @param keywords a <code>String</code> value
     */
    public void setKeywords(String keywords);

    /**
     * Gets the keywords to search for.
     * @return the keywords, a <code>String</code> value
     */
    public String getKeywords();

    /**
     * Copies the pagination settings (page start and page size) from the given service.
     * @param service a <code>ServiceInterface</code> instanct
     */
    public void setPagination(SearchServiceInterface service);

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
     * Perform the search according to the set parameters.
     * @throws ServiceException if an error occurs
     */
    public void search() throws ServiceException;

    /**
     * Gets the total number of results for the search.
     * @return an <code>int</code> value
     */
    public int getResultSize();

    /**
     * Gets the search results.
     * Because a search service is not bound to one particular object type
     *  the results are encapsulated as <code>SearchResult</code>.
     * @return the results of the search as a list of <code>SearchResult</code>
     */
    public List<SearchResult> getResults();

    /**
     * Reads a search result list and process it.
     * Normally the service will use this to process its own results from {@link #getResults} and
     *  load domain objects to be returned to the end user via additional getters.
     * @param results the list of results from the search service, it can be {@link #getResults} from this or another service
     * @throws ServiceException if an error occurs
     */
    public void readSearchResults(List<SearchResult> results) throws ServiceException;

    /**
     * Gets the <code>Set</code> of <code>Class</code> to query.
     * Those classes will be in the {@link #getResults} <code>SearchResult</code>.
     * @return the <code>Set</code> of <code>Class</code> to query
     */
    public Set<Class<?>> getClassesToQuery();

    /**
     * Gets the query string to as it would be passed to a {@link org.opentaps.domain.search.SearchRepositoryInterface}.
     * @return a <code>String</code> value
     */
    public String getQueryString();

}
