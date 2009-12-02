/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

import org.opentaps.domain.DomainService;

/**
 * Base class for the Search Service implementations.
 */
public abstract class CommonSearchService extends DomainService implements SearchServiceInterface {

    private int pageSize = SearchRepositoryInterface.DEFAULT_PAGE_SIZE;
    private int pageStart = 0;
    private String keywords;

    private List<Object[]> results;

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
    public List<Object[]> getResults() {
        return results;
    }

    /** {@inheritDoc} */
    public int getResultSize() {
        return resultSize;
    }

    public void setResults(List<Object[]> results) {
        this.results = results;
    }

    public void setResultSize(int resultSize) {
        this.resultSize = resultSize;
    }

}
