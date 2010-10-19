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

import org.opentaps.domain.DomainService;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Base class for the Search Service implementations.
 * Provides accessors for the search <code>keywords</code>, pagination settings and search results.
 */
public abstract class CommonSearchService extends DomainService implements SearchServiceInterface {

    private int pageSize = SearchRepositoryInterface.DEFAULT_PAGE_SIZE;
    private int pageStart = 0;
    private boolean usePagination = true;
    private String keywords;

    private List<SearchResult> results;
    private int resultSize = 0;

    /**
     * Default constructor.
     */
    public CommonSearchService() {
        super();
    }

    /** {@inheritDoc} */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /** {@inheritDoc} */
    public String getKeywords() {
        return keywords;
    }

    /** {@inheritDoc} */
    public void enablePagination(boolean enable) {
        this.usePagination = enable;
    }

    /** {@inheritDoc} */
    public boolean usePagination() {
        return usePagination;
    }

    /** {@inheritDoc} */
    public void setPagination(SearchServiceInterface service) {
        setPageStart(service.getPageStart());
        setPageSize(service.getPageSize());
        enablePagination(service.usePagination());
    }

    /** {@inheritDoc} */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /** {@inheritDoc} */
    public int getPageSize() {
        return pageSize;
    }

    /** {@inheritDoc} */
    public void setPageStart(int pageStart) {
        this.pageStart = pageStart;
    }

    /** {@inheritDoc} */
    public int getPageStart() {
        return pageStart;
    }

    /** {@inheritDoc} */
    public int getPageEnd() {
        return pageStart + pageSize;
    }

    /** {@inheritDoc} */
    public List<SearchResult> getResults() {
        return results;
    }

    /** {@inheritDoc} */
    public int getResultSize() {
        return resultSize;
    }

    /**
     * Sets the list of results.
     * @param results a list of <code>SearchResult</code> to set as the results
     */
    protected void setResults(List<SearchResult> results) {
        this.results = results;
    }

    /**
     * Sets the result size.
     * @param resultSize the total number of objects that matched the search, could be more than the actual results size because of pagination
     */
    protected void setResultSize(int resultSize) {
        this.resultSize = resultSize;
    }

    /**
     * Convenience method to perform the search action given a <code>SearchRepositoryInterface</code>.
     *
     * @param searchRepository a <code>SearchRepositoryInterface</code> value
     * @exception ServiceException if an error occurs
     */
    protected void search(SearchRepositoryInterface searchRepository) throws ServiceException {
        try {
            // make the aggregated query from all the services
            // set to be used, this will make the pagination consistent
            Set<Class<?>> classes = getClassesToQuery();
            if (classes.isEmpty()) {
                throw new ServiceException("Cannot perform search, no class to search.");
            }
            searchRepository.searchInEntities(classes, searchRepository.makeQueryString(getQueryString(), getKeywords()), getPageStart(), getPageSize());
            setResults(searchRepository.getResults());
            setResultSize(searchRepository.getResultSize());

            // send the result to each search service
            // which will fetch the domain objects
            readSearchResults(getResults());
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

}
