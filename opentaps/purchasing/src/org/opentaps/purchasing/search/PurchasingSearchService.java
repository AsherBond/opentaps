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
package org.opentaps.purchasing.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentaps.base.entities.PartyGroup;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.search.CommonSearchService;
import org.opentaps.domain.search.SearchDomainInterface;
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.domain.search.SearchResult;
import org.opentaps.domain.search.SearchServiceInterface;
import org.opentaps.domain.search.order.PurchaseOrderSearchServiceInterface;
import org.opentaps.domain.search.party.SupplierSearchServiceInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * The implementation of the Purchasing search service.
 */
public class PurchasingSearchService extends CommonSearchService implements SearchServiceInterface  {

    private boolean searchSuppliers = false;
    private boolean searchPurchaseOrders = false;

    private List<PartyGroup> suppliers = null;
    private List<Order> orders = null;

    private PurchaseOrderSearchServiceInterface purchaseOrderSearch;
    private SupplierSearchServiceInterface supplierSearch;

    private SearchRepositoryInterface searchRepository;

    /**
     * Option to return Suppliers from a search.
     * @param option a <code>boolean</code> value
     */
    public void setSearchSuppliers(boolean option) {
        this.searchSuppliers = option;
    }

    /**
     * Option to return Purchase Orders from a search.
     * @param option a <code>boolean</code> value
     */
    public void setSearchPurchaseOrders(boolean option) {
        this.searchPurchaseOrders = option;
    }

    /** {@inheritDoc} */
    public void search() throws ServiceException {
        try {
            SearchDomainInterface searchDomain = getDomainsDirectory().getSearchDomain();
            supplierSearch = searchDomain.getSupplierSearchService();
            purchaseOrderSearch = searchDomain.getPurchaseOrderSearchService();

            searchRepository = searchDomain.getSearchRepository();

            search(searchRepository);

        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void readSearchResults(List<SearchResult> results) throws ServiceException {
        purchaseOrderSearch.readSearchResults(results);
        supplierSearch.readSearchResults(results);

        orders = purchaseOrderSearch.getOrders();
        suppliers = supplierSearch.getSuppliers();
    }

    /**
     * Gets the suppliers results.
     * @return the <code>List</code> of <code>PartyGroup</code>
     */
    public List<PartyGroup> getSuppliers() {
        return suppliers;
    }

    /**
     * Gets the pruchase orders results.
     * @return the <code>List</code> of <code>Order</code>
     */
    public List<Order> getPurchaseOrders() {
        return orders;
    }

    /** {@inheritDoc} */
    public String getQueryString() {

        StringBuilder sb = new StringBuilder();

        if (searchSuppliers) {
            sb.append(supplierSearch.getQueryString());
        }
        if (searchPurchaseOrders) {
            sb.append(purchaseOrderSearch.getQueryString());
        }

        return sb.toString();
    }

    /** {@inheritDoc} */
    public Set<Class<?>> getClassesToQuery() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        if (searchSuppliers) {
            classes.addAll(supplierSearch.getClassesToQuery());
        }
        if (searchPurchaseOrders) {
            classes.addAll(purchaseOrderSearch.getClassesToQuery());
        }

        return classes;
    }
}
