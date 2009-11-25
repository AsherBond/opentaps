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
package org.opentaps.purchasing.search;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentaps.domain.base.entities.PartyGroup;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.search.CommonSearchService;
import org.opentaps.domain.search.SearchDomainInterface;
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.domain.search.SearchServiceInterface;
import org.opentaps.domain.search.SupplierSearchServiceInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;

/**
 * The implementation of the Purchasing search service.
 */
public class PurchasingSearchService extends CommonSearchService {

    private boolean searchSuppliers = false;
    private List<PartyGroup> suppliers = null;

    private PartyRepositoryInterface partyRepository;
    private SupplierSearchServiceInterface supplierSearch;
    private SearchRepositoryInterface searchRepository;
    private int pageSize = SearchRepositoryInterface.DEFAULT_PAGE_SIZE;
    private int pageStart = 0;
    private String keywords;
    private List<Object[]> results;
    private int resultSize = 0;

    /**
     * Option to return Suppliers from a search.
     * @param option a <code>boolean</code> value
     */
    public void setSearchSuppliers(boolean option) {
        this.searchSuppliers = option;
    }

    /** {@inheritDoc} */
    public void search() throws ServiceException {
        try {
            PartyDomainInterface partyDomain = getDomainsDirectory().getPartyDomain();
            partyRepository = partyDomain.getPartyRepository();
            
            SearchDomainInterface searchDomain = getDomainsDirectory().getSearchDomain();
            supplierSearch = searchDomain.getSupplierSearchService();
            // make the query
            searchRepository = searchDomain.getSearchRepository();
            Map output = searchRepository.searchInEntities(makeEntityClassList(), getQueryProjectedFields(), makeQuery(), getPageStart(), getPageSize());
            this.results = (List<Object[]>) output.get(SearchRepositoryInterface.RETURN_RESULTS);
            this.resultSize =  (Integer) output.get(SearchRepositoryInterface.RETURN_RESULT_SIZE);
            // note: the filterSearchResults methods expect getResults to return a list of EntityInterface, which means
            //  a non projected search result, so do not override setQueryProjection unless you also override this method
            suppliers = supplierSearch.filterSearchResults(getResults(), partyRepository);
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public Set<String> getQueryProjectedFields() {
        Set<String> fields = new LinkedHashSet<String>();
        fields.addAll(supplierSearch.getQueryProjectedFields());
        return fields;
    }

    /**
     * Gets the suppliers results.
     * @return the <code>List</code> of <code>PartyGroup</code>
     */
    public List<PartyGroup> getSuppliers() {
        return suppliers;
    }

    /**
     * Builds the Lucene query according to the options set and the user given keywords.
     * @return a Lucene query string
     * @throws ServiceException if no search option is set
     */
    private String makeQuery() throws ServiceException {

        StringBuilder sb = new StringBuilder();

        if (searchSuppliers) {
            supplierSearch.makeQuery(sb);
        }

        if (sb.length() == 0) {
            throw new ServiceException("Cannot perform search, no search option was set");
        }

        return searchRepository.makeQueryString(sb.toString(), getKeywords());
    }

    /**
     * Builds the list of entity classes that should be searched according to the options set.
     * @return a <code>List</code> of entity classes
     * @throws ServiceException if no search option is set
     */
    @SuppressWarnings("unchecked")
    private Set<Class> makeEntityClassList() throws ServiceException {
        Set<Class> classes = new HashSet<Class>();
        if (searchSuppliers) {
            classes.addAll(supplierSearch.getClassesToQuery());
        }

        if (classes.isEmpty()) {
            throw new ServiceException("Cannot perform search, no search option was set");
        }

        return classes;
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
}
