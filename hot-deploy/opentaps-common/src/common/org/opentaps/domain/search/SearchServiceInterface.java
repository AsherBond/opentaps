/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.domain.search;

import java.util.List;
import java.util.Set;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;

/**
 * Interface for search services.
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
     * Gets the search results.
     * @return the results of the search, which is a <code>List<Object[]></code> where the first two fields are <code>{OBJECT_CLASS, ID}</code>
     */
    public List<Object[]> getResults();

    /**
     * Gets the total number of results for the search.
     * @return an <code>int</code> value
     */
    public int getResultSize();

    /**
     * Gets the list of projected fields this search service is using.
     * This implementation use the default fields <code>{OBJECT_CLASS, ID}</code>, override in sub classes to use additional fields
     * @return the list of projected fields this search service is using
     */
    public Set<String> getQueryProjectedFields();

}
