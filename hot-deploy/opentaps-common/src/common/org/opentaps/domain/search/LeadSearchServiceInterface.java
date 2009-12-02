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
import java.util.Set;

import org.opentaps.domain.party.Lead;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is the interface of the Lead search service.
 */
public interface LeadSearchServiceInterface extends SearchServiceInterface {

    /**
     * Gets the leads results.
     * @return the <code>List</code> of <code>Lead</code>
     */
    public List<Lead> getLeads();

    /**
     * Builds part of the query to search for <code>Accounts</code> according to the set options.
     * @param sb the current <code>StringBuilder</code> instance
     */
    public void makeQuery(StringBuilder sb);

    /**
     * Filters the results of the search service to get the list of matching <code>Lead</code>.
     * @param results the list of results from the search service, it must be a list of <code>Object[]</code>, from the projection <code>{OBJECT_CLASS, ID}</code>
     * @return the list of <code>Lead</code> found from the results
     * @throws ServiceException if an error occurs
     */
    public List<Lead> filterSearchResults(List<Object[]> results) throws ServiceException;

    /**
     * Gets the <code>Set</code> of <code>Class</code> to query.
     * @return the <code>Set</code> of <code>Class</code> to query
     */
    @SuppressWarnings("unchecked")
    public Set<Class> getClassesToQuery();

}
