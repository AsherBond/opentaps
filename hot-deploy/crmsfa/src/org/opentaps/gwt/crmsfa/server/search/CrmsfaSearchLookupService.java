/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.crmsfa.server.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.CustRequest;
import org.opentaps.base.entities.CustRequestAndPartyRelationshipAndRole;
import org.opentaps.base.entities.PartyFromByRelnAndContactInfoAndPartyClassification;
import org.opentaps.base.entities.SalesOpportunity;
import org.opentaps.crmsfa.search.CrmsfaSearchService;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Lead;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.gwt.common.client.lookup.configuration.CaseLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.SearchLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.gwt.common.server.lookup.EntityLookupAndSuggestService;
import org.opentaps.gwt.common.server.lookup.JsonResponse;
import org.opentaps.gwt.common.server.lookup.PartyLookupService;

/**
 * The RPC service used to CRMSFA search results.
 */
public final class CrmsfaSearchLookupService extends EntityLookupAndSuggestService {

    private static final String MODULE = CrmsfaSearchLookupService.class.getName();

    /**
     * Creates a new <code>CrmsfaSearchService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     * @param fields the list of fields that will be in the response
     */
    private CrmsfaSearchLookupService(InputProviderInterface provider, List<String> fields) {
        super(provider, fields);
    }

    /**
     * Creates a new <code>CrmsfaSearchService</code> instance for searching parties.
     * @param provider an <code>InputProviderInterface</code> value
     * @return a <code>CrmsfaSearchLookupService</code> value
     */
    public static CrmsfaSearchLookupService makePartySearchService(InputProviderInterface provider) {
        return new CrmsfaSearchLookupService(provider, PartyLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * Creates a new <code>CrmsfaSearchService</code> instance for searching cases.
     * @param provider an <code>InputProviderInterface</code> value
     * @return a <code>CrmsfaSearchLookupService</code> value
     */
    public static CrmsfaSearchLookupService makeCaseSearchService(InputProviderInterface provider) {
        return new CrmsfaSearchLookupService(provider, CaseLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to perform a global CRMSFA search.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String crmsfaSearch(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CrmsfaSearchLookupService service = CrmsfaSearchLookupService.makePartySearchService(provider);
        service.search();
        return json.makeSuggestResponse(SearchLookupConfiguration.RESULT_ID, service);
    }

    /**
     * AJAX event to search Accounts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String crmsfaSearchAccounts(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CrmsfaSearchLookupService service = CrmsfaSearchLookupService.makePartySearchService(provider);
        service.searchAccounts();
        return json.makeLookupResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * AJAX event to search Contacts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String crmsfaSearchContacts(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CrmsfaSearchLookupService service = CrmsfaSearchLookupService.makePartySearchService(provider);
        service.searchContacts();
        return json.makeLookupResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * AJAX event to search Leads.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String crmsfaSearchLeads(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CrmsfaSearchLookupService service = CrmsfaSearchLookupService.makePartySearchService(provider);
        service.searchLeads();
        return json.makeLookupResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * AJAX event to search Cases.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String crmsfaSearchCases(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CrmsfaSearchLookupService service = CrmsfaSearchLookupService.makeCaseSearchService(provider);
        service.searchCases();
        return json.makeLookupResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service, request.getSession(true).getServletContext());
    }

    public List<CustRequestAndPartyRelationshipAndRole> searchCases() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            List<CustRequestAndPartyRelationshipAndRole> res = new ArrayList<CustRequestAndPartyRelationshipAndRole>();
            setResults(res);
            return res;
        }

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            // set options on what is searched
            crmSearch.setSearchCases(true);
            prepareSearchParties(crmSearch);
            return findList(CustRequestAndPartyRelationshipAndRole.class,
                        EntityCondition.makeCondition(CustRequestAndPartyRelationshipAndRole.Fields.custRequestId.name(),
                                                      EntityOperator.IN,
                                                      Entity.getDistinctFieldValues(String.class, crmSearch.getCases(), CustRequest.Fields.custRequestId)));
        } catch (ServiceException e) {
            storeException(e);
            return null;
        }
    }

    public List<PartyFromByRelnAndContactInfoAndPartyClassification> searchAccounts() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            List<PartyFromByRelnAndContactInfoAndPartyClassification> res = new ArrayList<PartyFromByRelnAndContactInfoAndPartyClassification>();
            setResults(res);
            return res;
        }

        PartyLookupService.prepareFindParties(this);

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            // set options on what is searched
            crmSearch.setSearchAccounts(true);
            prepareSearchParties(crmSearch);
            return extractPartiesResults(crmSearch.getAccounts());
        } catch (ServiceException e) {
            storeException(e);
            return null;
        }
    }

    public List<PartyFromByRelnAndContactInfoAndPartyClassification> searchContacts() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            List<PartyFromByRelnAndContactInfoAndPartyClassification> res = new ArrayList<PartyFromByRelnAndContactInfoAndPartyClassification>();
            setResults(res);
            return res;
        }

        PartyLookupService.prepareFindParties(this);

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            // set options on what is searched
            crmSearch.setSearchContacts(true);
            prepareSearchParties(crmSearch);
            return extractPartiesResults(crmSearch.getContacts());
        } catch (ServiceException e) {
            storeException(e);
            return null;
        }
    }

    public List<PartyFromByRelnAndContactInfoAndPartyClassification> searchLeads() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            List<PartyFromByRelnAndContactInfoAndPartyClassification> res = new ArrayList<PartyFromByRelnAndContactInfoAndPartyClassification>();
            setResults(res);
            return res;
        }

        PartyLookupService.prepareFindParties(this);

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            // set options on what is searched
            crmSearch.setSearchLeads(true);
            prepareSearchParties(crmSearch);
            return extractPartiesResults(crmSearch.getLeads());
        } catch (ServiceException e) {
            storeException(e);
            return null;
        }
    }

    private void prepareSearchParties(CrmsfaSearchService crmSearch) throws ServiceException {
        // set the common parameters
        crmSearch.setInfrastructure(getProvider().getInfrastructure());
        crmSearch.setUser(getProvider().getUser());
        crmSearch.setKeywords(getSuggestQuery());
        // pass the pagination parameters to the service
        crmSearch.setPageStart(getPager().getPageStart());
        crmSearch.setPageSize(getPager().getPageSize());
        crmSearch.search();
    }

    private List<PartyFromByRelnAndContactInfoAndPartyClassification> extractPartiesResults(List<? extends Party> parties) {
        // convert the list of Parties to PartyFromByRelnAndContactInfoAndPartyClassification
        return findList(PartyFromByRelnAndContactInfoAndPartyClassification.class,
                        EntityCondition.makeCondition(PartyFromByRelnAndContactInfoAndPartyClassification.Fields.partyId.name(),
                                                      EntityOperator.IN,
                                                      Entity.getDistinctFieldValues(String.class, parties, Party.Fields.partyId)));
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface result) {
        if (result instanceof Party) {
            return makeDisplayedText((Party) result);
        } else if (result instanceof SalesOpportunity) {
            return makeDisplayedText((SalesOpportunity) result);
        } else if (result instanceof Order) {
            return makeDisplayedText((Order) result);
        } else if (result instanceof CustRequest) {
            return makeDisplayedText((CustRequest) result);
        } else {
            return null;
        }
    }

    @Override
    public Map<String, String> makeExtraSuggestValues(EntityInterface result) {
        if (result instanceof Party) {
            return makeExtraValues((Party) result);
        } else if (result instanceof SalesOpportunity) {
            return makeExtraValues((SalesOpportunity) result);
        } else if (result instanceof Order) {
            return makeExtraValues((Order) result);
        } else if (result instanceof CustRequest) {
            return makeExtraValues((CustRequest) result);
        } else {
            return null;
        }
    }

    private Map<String, String> makeExtraValues(Order order) {
        Map<String, String> extras = new HashMap<String, String>();
        String type = Order.class.getSimpleName();
        extras.put(SearchLookupConfiguration.RESULT_ID, type + "_" + order.getOrderId());
        extras.put(SearchLookupConfiguration.RESULT_TYPE, type);
        extras.put(SearchLookupConfiguration.RESULT_REAL_ID, order.getOrderId());
        try {
            extras.put(SearchLookupConfiguration.RESULT_DESCRIPTION, "Status:" + order.getStatus().getDescription() + " Customer: " + order.getBillToCustomer().getName());
        } catch (RepositoryException e) {
            Debug.logError(e, MODULE);
        }

        return extras;
    }

    private String makeDisplayedText(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append(order.getOrderName());
        sb.append(" (").append(order.getOrderId()).append(")");
        return sb.toString();
    }

    private Map<String, String> makeExtraValues(SalesOpportunity opportunity) {
        Map<String, String> extras = new HashMap<String, String>();
        String type = SalesOpportunity.class.getSimpleName();
        extras.put(SearchLookupConfiguration.RESULT_ID, type + "_" + opportunity.getSalesOpportunityId());
        extras.put(SearchLookupConfiguration.RESULT_TYPE, type);
        extras.put(SearchLookupConfiguration.RESULT_REAL_ID, opportunity.getSalesOpportunityId());
        extras.put(SearchLookupConfiguration.RESULT_DESCRIPTION, opportunity.getDescription());
        return extras;
    }

    private String makeDisplayedText(SalesOpportunity opportunity) {
        StringBuilder sb = new StringBuilder();
        sb.append(opportunity.getOpportunityName());
        sb.append(" (").append(opportunity.getSalesOpportunityId()).append(")");
        return sb.toString();
    }

    private Map<String, String> makeExtraValues(CustRequest request) {
        Map<String, String> extras = new HashMap<String, String>();
        String type = CustRequest.class.getSimpleName();
        extras.put(SearchLookupConfiguration.RESULT_ID, type + "_" + request.getCustRequestId());
        extras.put(SearchLookupConfiguration.RESULT_TYPE, type);
        extras.put(SearchLookupConfiguration.RESULT_REAL_ID, request.getCustRequestId());
        extras.put(SearchLookupConfiguration.RESULT_DESCRIPTION, request.getDescription());
        return extras;
    }

    private String makeDisplayedText(CustRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getCustRequestName());
        sb.append(" (").append(request.getCustRequestId()).append(")");
        return sb.toString();
    }

    private Map<String, String> makeExtraValues(Party party) {
        Map<String, String> extras = new HashMap<String, String>();
        String type = party.getClass().getSimpleName();
        extras.put(SearchLookupConfiguration.RESULT_ID, type + "_" + party.getPartyId());
        extras.put(SearchLookupConfiguration.RESULT_TYPE, type);
        extras.put(SearchLookupConfiguration.RESULT_REAL_ID, party.getPartyId());
        return extras;
    }

    private String makeDisplayedText(Party party) {
        StringBuilder sb = new StringBuilder();
        sb.append(party.getName());
        sb.append(" (").append(party.getPartyId()).append(")");
        return sb.toString();
    }

    private List<EntityInterface> search() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            return new ArrayList<EntityInterface>();
        }

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            crmSearch.setInfrastructure(getProvider().getInfrastructure());
            crmSearch.setUser(getProvider().getUser());
            crmSearch.setKeywords(getSuggestQuery());
            // pass the pagination parameters to the service
            crmSearch.setPageStart(getPager().getPageStart());
            crmSearch.setPageSize(getPager().getPageSize());
            // set options on what is searched
            crmSearch.setSearchAccounts(true);
            crmSearch.setSearchContacts(true);
            crmSearch.setSearchLeads(true);
            crmSearch.setSearchSalesOpportunities(true);
            crmSearch.setSearchSalesOrders(true);
            crmSearch.setSearchCases(true);
            crmSearch.search();
            List<Account> accounts = crmSearch.getAccounts();
            List<Contact> contacts = crmSearch.getContacts();
            List<Lead> leads = crmSearch.getLeads();
            List<SalesOpportunity> opportunities = crmSearch.getSalesOpportunities();
            List<Order> orders = crmSearch.getSalesOrders();
            List<CustRequest> cases = crmSearch.getCases();

            // get results
            List<EntityInterface> r = new ArrayList<EntityInterface>();
            r.addAll(accounts);
            r.addAll(contacts);
            r.addAll(leads);
            r.addAll(cases);
            r.addAll(opportunities);
            r.addAll(orders);

            // no pagination needed, already done in the service (see above)
            setResultTotalCount(crmSearch.getResultSize());
            setResults(r);

        } catch (ServiceException e) {
            storeException(e);
            return null;
        }

        return getResults();
    }

}
