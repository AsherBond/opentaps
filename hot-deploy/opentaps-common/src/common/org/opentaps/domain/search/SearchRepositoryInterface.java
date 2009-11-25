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

import java.util.Map;
import java.util.Set;

import org.opentaps.foundation.service.ServiceException;
/**
 * Interface for search reposity.
 */
public interface SearchRepositoryInterface {
    public final static String RETURN_RESULTS = "results";
    public final static String RETURN_RESULT_SIZE = "resultSize";
    /** The default max number of result per page. */
    public static final int DEFAULT_PAGE_SIZE = 50;
    /** The default placeholder in Lucene queries. */
    public static final String DEFAULT_PLACEHOLDER = "?";
    /** The regular expression recognizing the default placeholder in Lucene queries. */
    public static final String DEFAULT_PLACEHOLDER_REGEX = "\\?";
    
    /**
     * Search custom query and return the result/resultSize map.
     * @param entityClasses a <code>Set</code> instance
     * @param projectedFields a <code>Set</code> instance
     * @param queryString a <code>String</code> value
     * @param pageStart the page start index
     * @param pageSize the page size
     * @throws ServiceException if an error occurs
     */
    public Map searchInEntities(Set<Class> entityClasses, Set<String> projectedFields, String queryString, int pageStart, int pageSize) throws ServiceException;
    /**
     * make a query string by user given keyword.
     * @param queryString a custom query
     * @param keywords a user given string of the keywords he is looking for
     * @return the custom query string
     */
    public String makeQueryString(String queryString, String keywords);
}
