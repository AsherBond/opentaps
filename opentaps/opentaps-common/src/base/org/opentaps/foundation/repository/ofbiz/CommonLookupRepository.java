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
package org.opentaps.foundation.repository.ofbiz;

import org.opentaps.foundation.repository.LookupRepositoryInterface;

/**
 * Base class for the lookup repositories implementations.
 * Provides accessors for the pagination settings.
 */
public abstract class CommonLookupRepository extends Repository implements LookupRepositoryInterface {

    private int pageSize = 10;
    private int pageStart = 0;
    private boolean usePagination = true;
    private int resultSize = 0;

    /**
     * Default constructor.
     */
    public CommonLookupRepository() {
        super();
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
    public int getResultSize() {
        return resultSize;
    }

    /**
     * Sets the result size.
     * @param resultSize the total number of objects that matched the search, could be more than the actual results size because of pagination
     */
    protected void setResultSize(int resultSize) {
        this.resultSize = resultSize;
    }

}
