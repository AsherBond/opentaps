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
package org.opentaps.purchasing.search;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.opentaps.domain.base.entities.PartyGroup;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.party.SupplierSearchServiceInterface;
import org.opentaps.domain.search.SearchService;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * The implementation of the Purchasing search service.
 */
public class PurchasingSearchService extends SearchService {

    private boolean searchSuppliers = false;
    private List<PartyGroup> suppliers = null;

    private PartyRepositoryInterface partyRepository;
    private SupplierSearchServiceInterface supplierSearch;

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
            supplierSearch = partyDomain.getSupplierSearchService();
            // make the query
            searchInEntities(makeEntityClassList(), makeQuery());
            // note: the filterSearchResults methods expect getResults to return a list of EntityInterface, which means
            //  a non projected search result, so do not override setQueryProjection unless you also override this method
            suppliers = supplierSearch.filterSearchResults(getResults(), partyRepository);
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public Set<String> getQueryProjectedFields() {
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

        return makeQueryString(sb.toString(), getKeywords());
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
}
