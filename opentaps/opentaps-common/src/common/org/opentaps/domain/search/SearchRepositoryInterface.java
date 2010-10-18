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

import org.opentaps.foundation.repository.RepositoryException;

/**
 * Interface for search repository.
 * A search repository contains the implementation of the base search logic.
 */
public interface SearchRepositoryInterface {

    /** The default max number of result per page. */
    public static final int DEFAULT_PAGE_SIZE = 50;

    /**
     * Search custom query and return the result/resultSize map.
     * @param entityClasses the <code>Set</code> of classes to query
     * @param queryString the search query <code>String</code> value
     * @param pageStart the page start index
     * @param pageSize the page size
     * @throws RepositoryException if an error occurs
     */
    public void searchInEntities(Set<Class<?>> entityClasses, String queryString, int pageStart, int pageSize) throws RepositoryException;

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
     * Makes a query string from the user given keywords.
     * @param queryString a custom query
     * @param keywords a user given string of the keywords he is looking for
     * @return the custom query string
     */
    public String makeQueryString(String queryString, String keywords);
}
