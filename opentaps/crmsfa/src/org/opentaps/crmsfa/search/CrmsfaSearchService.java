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
package org.opentaps.crmsfa.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentaps.base.entities.SalesOpportunity;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Lead;
import org.opentaps.domain.search.CommonSearchService;
import org.opentaps.domain.search.SearchDomainInterface;
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.domain.search.SearchResult;
import org.opentaps.domain.search.SearchServiceInterface;
import org.opentaps.domain.search.communication.CaseSearchServiceInterface;
import org.opentaps.domain.search.order.SalesOpportunitySearchServiceInterface;
import org.opentaps.domain.search.order.SalesOrderSearchServiceInterface;
import org.opentaps.domain.search.party.AccountSearchServiceInterface;
import org.opentaps.domain.search.party.ContactSearchServiceInterface;
import org.opentaps.domain.search.party.LeadSearchServiceInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.base.entities.CustRequest;

/**
 * The implementation of the Crmsfa search service.
 * This class does not actually implement any of the search logic, those are implemented in the specific domain
 *  search services eg: <code>AccountSearchService</code>, ...
 */
public class CrmsfaSearchService extends CommonSearchService implements SearchServiceInterface {

    private boolean searchAccounts = false;
    private boolean searchContacts = false;
    private boolean searchLeads = false;
    private boolean searchSalesOpportunities = false;
    private boolean searchSalesOrders = false;
    private boolean searchCases = false;

    private List<Contact> contacts = null;
    private List<Account> accounts = null;
    private List<Lead> leads = null;
    private List<SalesOpportunity> salesOpportunities = null;
    private List<Order> salesOrders = null;
    private List<CustRequest> cases = null;

    private AccountSearchServiceInterface accountSearch;
    private ContactSearchServiceInterface contactSearch;
    private LeadSearchServiceInterface leadSearch;
    private SalesOpportunitySearchServiceInterface salesOpportunitySearch;
    private SalesOrderSearchServiceInterface salesOrderSearch;
    private CaseSearchServiceInterface caseSearch;

    private SearchRepositoryInterface searchRepository;

    /**
     * Option to return Accounts from a search.
     * @param option a <code>boolean</code> value
     */
    public void setSearchAccounts(boolean option) {
        this.searchAccounts = option;
    }

    /**
     * Option to return Contacts from a search.
     * @param option a <code>boolean</code> value
     */
    public void setSearchContacts(boolean option) {
        this.searchContacts = option;
    }

    /**
     * Option to return Leads from a search.
     * @param option a <code>boolean</code> value
     */
    public void setSearchLeads(boolean option) {
        this.searchLeads = option;
    }

    /**
     * Option to return Sales Opportunities from a search.
     * @param option a <code>boolean</code> value
     */
    public void setSearchSalesOpportunities(boolean option) {
        this.searchSalesOpportunities = option;
    }

    /**
     * Option to return Sales Orders from a search.
     * @param option a <code>boolean</code> value
     */
    public void setSearchSalesOrders(boolean option) {
        this.searchSalesOrders = option;
    }

    /**
     * Option to return Cases from a search.
     * @param option a <code>boolean</code> value
     */
    public void setSearchCases(boolean option) {
        this.searchCases = option;
    }

    /** {@inheritDoc} */
    public void search() throws ServiceException {
        try {
            SearchDomainInterface searchDomain = getDomainsDirectory().getSearchDomain();

            accountSearch = searchDomain.getAccountSearchService();
            accountSearch.setPagination(this);
            contactSearch = searchDomain.getContactSearchService();
            contactSearch.setPagination(this);
            leadSearch = searchDomain.getLeadSearchService();
            leadSearch.setPagination(this);
            salesOpportunitySearch = searchDomain.getSalesOpportunitySearchService();
            salesOpportunitySearch.setPagination(this);
            salesOrderSearch = searchDomain.getSalesOrderSearchService();
            salesOrderSearch.setPagination(this);
            caseSearch = searchDomain.getCaseSearchService();
            caseSearch.setPagination(this);

            searchRepository = searchDomain.getSearchRepository();

            search(searchRepository);

        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void readSearchResults(List<SearchResult> results) throws ServiceException {
        // scroll each sub services results

        int s = getPageStart();
        int ps = getPageSize();
        int n = 0; // returned records
        int t = 0; // total records

        if (searchAccounts) {
            accountSearch.setPageStart(s);
            accountSearch.setPageSize(ps);
            accountSearch.readSearchResults(results);
            accounts = accountSearch.getAccounts();
            t += accountSearch.getResultSize();
            n += accounts.size();
        }

        if (searchContacts) {
            contactSearch.setPageStart(Math.max(s - t, 0));
            contactSearch.setPageSize(Math.max(ps - n, 0));
            contactSearch.readSearchResults(results);
            contacts = contactSearch.getContacts();
            t += contactSearch.getResultSize();
            n += contacts.size();
        }

        if (searchLeads) {
            leadSearch.setPageStart(Math.max(s - t, 0));
            leadSearch.setPageSize(Math.max(ps - n, 0));
            leadSearch.readSearchResults(results);
            leads = leadSearch.getLeads();
            t += leadSearch.getResultSize();
            n += leads.size();
        }

        if (searchSalesOpportunities) {
            salesOpportunitySearch.setPageStart(Math.max(s - t, 0));
            salesOpportunitySearch.setPageSize(Math.max(ps - n, 0));
            salesOpportunitySearch.readSearchResults(results);
            salesOpportunities = salesOpportunitySearch.getSalesOpportunities();
            t += salesOpportunitySearch.getResultSize();
            n += salesOpportunities.size();
        }

        if (searchSalesOrders) {
            salesOrderSearch.setPageStart(Math.max(s - t, 0));
            salesOrderSearch.setPageSize(Math.max(ps - n, 0));
            salesOrderSearch.readSearchResults(results);
            salesOrders = salesOrderSearch.getOrders();
            t += salesOrderSearch.getResultSize();
            n += salesOrders.size();
        }

        if (searchCases) {
            caseSearch.setPageStart(Math.max(s - t, 0));
            caseSearch.setPageSize(Math.max(ps - n, 0));
            caseSearch.readSearchResults(results);
            cases = caseSearch.getCases();
            t += caseSearch.getResultSize();
            n += cases.size();
        }

        // set the total number of results
        setResultSize(t);
    }

    /**
     * Gets the accounts results.
     * @return the <code>List</code> of <code>Account</code>
     */
    public List<Account> getAccounts() {
        return accounts;
    }

    /**
     * Gets the contacts results.
     * @return the <code>List</code> of <code>Contact</code>
     */
    public List<Contact> getContacts() {
        return contacts;
    }

    /**
     * Gets the leads results.
     * @return the <code>List</code> of <code>Lead</code>
     */
    public List<Lead> getLeads() {
        return leads;
    }

    /**
     * Gets the cases results.
     * @return the <code>List</code> of <code>CustRequest</code>
     */
    public List<CustRequest> getCases() {
        return cases;
    }

    /**
     * Gets the sales opportunities results.
     * @return the <code>List</code> of <code>SalesOpportunity</code>
     */
    public List<SalesOpportunity> getSalesOpportunities() {
        return salesOpportunities;
    }

    /**
     * Gets the sales orders results.
     * @return the <code>List</code> of <code>Order</code>
     */
    public List<Order> getSalesOrders() {
        return salesOrders;
    }

    /** {@inheritDoc} */
    public String getQueryString() {

        StringBuilder sb = new StringBuilder();

        if (searchAccounts) {
            sb.append(accountSearch.getQueryString());
        }
        if (searchContacts) {
            sb.append(contactSearch.getQueryString());
        }
        if (searchLeads) {
            sb.append(leadSearch.getQueryString());
        }
        if (searchSalesOpportunities) {
            sb.append(salesOpportunitySearch.getQueryString());
        }
        if (searchSalesOrders) {
            sb.append(salesOrderSearch.getQueryString());
        }
        if (searchCases) {
            sb.append(caseSearch.getQueryString());
        }

        return sb.toString();
    }

    /** {@inheritDoc} */
    public Set<Class<?>> getClassesToQuery() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        if (searchAccounts) {
            classes.addAll(accountSearch.getClassesToQuery());
        }
        if (searchContacts) {
            classes.addAll(contactSearch.getClassesToQuery());
        }
        if (searchLeads) {
            classes.addAll(leadSearch.getClassesToQuery());
        }
        if (searchSalesOpportunities) {
            classes.addAll(salesOpportunitySearch.getClassesToQuery());
        }
        if (searchSalesOrders) {
            classes.addAll(salesOrderSearch.getClassesToQuery());
        }
        if (searchCases) {
            classes.addAll(caseSearch.getClassesToQuery());
        }

        return classes;
    }

}
