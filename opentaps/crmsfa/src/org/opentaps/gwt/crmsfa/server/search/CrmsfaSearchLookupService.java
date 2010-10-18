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
package org.opentaps.gwt.crmsfa.server.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.entities.CustRequest;
import org.opentaps.base.entities.CustRequestAndPartyRelationshipAndRole;
import org.opentaps.base.entities.OrderHeaderItemAndRolesAndInvPending;
import org.opentaps.base.entities.PartyRoleNameDetailSupplementalData;
import org.opentaps.base.entities.SalesOpportunity;
import org.opentaps.base.entities.SalesOpportunityAndPartyRelationshipAndStage;
import org.opentaps.common.util.ConvertMapToString;
import org.opentaps.common.util.ICompositeValue;
import org.opentaps.crmsfa.search.CrmsfaSearchService;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderViewForListing;
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
import org.opentaps.gwt.common.client.lookup.configuration.OpportunityLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.SalesOrderLookupConfiguration;
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
     * Creates a new <code>CrmsfaSearchService</code> instance for searching sales orders.
     * @param provider an <code>InputProviderInterface</code> value
     * @return a <code>CrmsfaSearchLookupService</code> value
     */
    public static CrmsfaSearchLookupService makeSalesOrderSearchService(InputProviderInterface provider) {
        return new CrmsfaSearchLookupService(provider, SalesOrderLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * Creates a new <code>CrmsfaSearchService</code> instance for searching sales opportunities.
     * @param provider an <code>InputProviderInterface</code> value
     * @return a <code>CrmsfaSearchLookupService</code> value
     */
    public static CrmsfaSearchLookupService makeSalesOpportunitiesSearchService(InputProviderInterface provider) {
        return new CrmsfaSearchLookupService(provider, OpportunityLookupConfiguration.LIST_OUT_FIELDS);
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
        return json.makeLookupResponse(CaseLookupConfiguration.INOUT_CUST_REQUEST_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * AJAX event to search Sales Orders.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String crmsfaSearchSalesOpportunities(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CrmsfaSearchLookupService service = CrmsfaSearchLookupService.makeSalesOpportunitiesSearchService(provider);
        service.searchSalesOpportunities();
        return json.makeLookupResponse(OpportunityLookupConfiguration.INOUT_SALES_OPPORTUNITY_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * AJAX event to search Sales Orders.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String crmsfaSearchSalesOrders(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CrmsfaSearchLookupService service = CrmsfaSearchLookupService.makeSalesOrderSearchService(provider);
        service.searchSalesOrders();
        return json.makeLookupResponse(SalesOrderLookupConfiguration.INOUT_ORDER_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * Searches a List of Sales Order.
     * @return a list of <code>OrderViewForListing</code>
     */
    public List<OrderViewForListing> searchSalesOrders() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            List<OrderViewForListing> res = new ArrayList<OrderViewForListing>();
            setResults(res);
            return res;
        }

        class OrderNameIdSortable extends ConvertMapToString implements ICompositeValue {
            @Override
            public String convert(Map<String, ?> value) {
                if (value == null) {
                    return null;
                }
                return (String) value.get(SalesOrderLookupConfiguration.OUT_ORDER_NAME_ID);
            }
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(2);
                s.add(SalesOrderLookupConfiguration.INOUT_ORDER_ID);
                s.add(SalesOrderLookupConfiguration.INOUT_ORDER_NAME);
                return s;
            }
        }

        class StatusDecriptionSortable extends ConvertMapToString implements ICompositeValue {
            @Override
            public String convert(Map<String, ?> value) {
                if (value == null) {
                    return null;
                }
                return (String) value.get(SalesOrderLookupConfiguration.OUT_STATUS_DESCRIPTION);
            }
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(1);
                s.add(SalesOrderLookupConfiguration.INOUT_STATUS_ID);
                return s;
            }
        }

        class ShipByDateSortable extends ConvertMapToString implements ICompositeValue {
            @Override
            public String convert(Map<String, ?> value) {
                if (value == null) {
                    return null;
                }
                return (String) value.get(SalesOrderLookupConfiguration.OUT_SHIP_BY_DATE_STRING);
            }
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(1);
                // dummy field, this is really not sortable
                s.add(SalesOrderLookupConfiguration.INOUT_ORDER_DATE);
                return s;
            }
        }

        class OrderDateSortable extends ConvertMapToString implements ICompositeValue {
            @Override
            public String convert(Map<String, ?> value) {
                if (value == null) {
                    return null;
                }
                return (String) value.get(SalesOrderLookupConfiguration.OUT_ORDER_DATE_STRING);
            }
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(1);
                s.add(SalesOrderLookupConfiguration.INOUT_ORDER_DATE);
                return s;
            }
        }

        class CustomerNameSortable extends ConvertMapToString implements ICompositeValue {
            @Override
            public String convert(Map<String, ?> value) {
                if (value == null) {
                    return null;
                }
                return (String) value.get(SalesOrderLookupConfiguration.OUT_CUSTOMER_NAME);
            }
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(1);
                s.add(SalesOrderLookupConfiguration.INOUT_PARTY_ID);
                return s;
            }
        }

        // some fields in the view are not in the DB, setup the mapping for the order by
        Map<String, ConvertMapToString> calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(SalesOrderLookupConfiguration.OUT_ORDER_NAME_ID, new OrderNameIdSortable());
        makeCalculatedField(calcField);

        calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(SalesOrderLookupConfiguration.OUT_STATUS_DESCRIPTION, new StatusDecriptionSortable());
        makeCalculatedField(calcField);

        calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(SalesOrderLookupConfiguration.OUT_SHIP_BY_DATE_STRING, new ShipByDateSortable());
        makeCalculatedField(calcField);

        calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(SalesOrderLookupConfiguration.OUT_ORDER_DATE_STRING, new OrderDateSortable());
        makeCalculatedField(calcField);

        calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(SalesOrderLookupConfiguration.OUT_CUSTOMER_NAME, new CustomerNameSortable());
        makeCalculatedField(calcField);

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            // set options on what is searched
            crmSearch.setSearchSalesOrders(true);
            prepareSearch(crmSearch);
            // we need to lookup OrderHeaderItemAndRolesAndInvPending and convert those into OrderViewForListing
            // to get the OrderHeaderItemAndRolesAndInvPending with the customer we filter by role
            // pagination is still correct though as this only convert the entities
            List<OrderHeaderItemAndRolesAndInvPending> res = getRepository().findList(OrderHeaderItemAndRolesAndInvPending.class,
                            EntityCondition.makeCondition(EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.roleTypeId.name(),
                                                                                        EntityOperator.EQUALS,
                                                                                        "BILL_TO_CUSTOMER"),
                                                          EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.orderId.name(),
                                                                                        EntityOperator.IN,
                                                                                        Entity.getDistinctFieldValues(String.class, crmSearch.getSalesOrders(), Order.Fields.orderId))),
                            SalesOrderLookupConfiguration.LIST_QUERY_FIELDS, getOrderBy());
            // convert
            setResultTotalCount(crmSearch.getResultSize());
            setResults(OrderViewForListing.makeOrderView(res, getProvider().getInfrastructure().getDelegator(), getProvider().getTimeZone(), getProvider().getLocale()));
            return getResults();
        } catch (ServiceException e) {
            storeException(e);
            return null;
        } catch (RepositoryException e) {
            storeException(e);
            return null;
        }
    }

    /**
     * Searches a List of Cases.
     * @return a list of <code>CustRequestAndPartyRelationshipAndRole</code>
     */
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
            prepareSearch(crmSearch);
            return findList(CustRequestAndPartyRelationshipAndRole.class,
                        EntityCondition.makeCondition(CustRequestAndPartyRelationshipAndRole.Fields.custRequestId.name(),
                                                      EntityOperator.IN,
                                                      Entity.getDistinctFieldValues(String.class, crmSearch.getCases(), CustRequest.Fields.custRequestId))
                            , false); // do not paginate as this was done by the search service already
        } catch (ServiceException e) {
            storeException(e);
            return null;
        }
    }

    /**
     * Searches a List of Sales Opportunities.
     * @return a list of <code>SalesOpportunityAndPartyRelationshipAndStage</code>
     */
    public List<SalesOpportunityAndPartyRelationshipAndStage> searchSalesOpportunities() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            List<SalesOpportunityAndPartyRelationshipAndStage> res = new ArrayList<SalesOpportunityAndPartyRelationshipAndStage>();
            setResults(res);
            return res;
        }

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            // set options on what is searched
            crmSearch.setSearchSalesOpportunities(true);
            prepareSearch(crmSearch);
            return findList(SalesOpportunityAndPartyRelationshipAndStage.class,
                        EntityCondition.makeCondition(SalesOpportunityAndPartyRelationshipAndStage.Fields.salesOpportunityId.name(),
                                                      EntityOperator.IN,
                                                      Entity.getDistinctFieldValues(String.class, crmSearch.getSalesOpportunities(), SalesOpportunity.Fields.salesOpportunityId))
                            , false); // do not paginate as this was done by the search service already
        } catch (ServiceException e) {
            storeException(e);
            return null;
        }
    }

    /**
     * Searches a List of Accounts.
     * @return a list of <code>PartyFromByRelnAndContactInfoAndPartyClassification</code>
     */
    public List<PartyRoleNameDetailSupplementalData> searchAccounts() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            List<PartyRoleNameDetailSupplementalData> res = new ArrayList<PartyRoleNameDetailSupplementalData>();
            setResults(res);
            return res;
        }

        PartyLookupService.prepareFindParties(this);

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            // set options on what is searched
            crmSearch.setSearchAccounts(true);
            prepareSearch(crmSearch);
            return extractPartiesResults(crmSearch.getAccounts(), RoleTypeConstants.ACCOUNT);
        } catch (ServiceException e) {
            storeException(e);
            return null;
        }
    }

    /**
     * Searches a List of Contacts.
     * @return a list of <code>PartyFromByRelnAndContactInfoAndPartyClassification</code>
     */
    public List<PartyRoleNameDetailSupplementalData> searchContacts() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            List<PartyRoleNameDetailSupplementalData> res = new ArrayList<PartyRoleNameDetailSupplementalData>();
            setResults(res);
            return res;
        }

        PartyLookupService.prepareFindParties(this);

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            // set options on what is searched
            crmSearch.setSearchContacts(true);
            prepareSearch(crmSearch);
            return extractPartiesResults(crmSearch.getContacts(), RoleTypeConstants.CONTACT);
        } catch (ServiceException e) {
            storeException(e);
            return null;
        }
    }

    /**
     * Searches a List of Leads.
     * @return a list of <code>PartyFromByRelnAndContactInfoAndPartyClassification</code>
     */
    public List<PartyRoleNameDetailSupplementalData> searchLeads() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            List<PartyRoleNameDetailSupplementalData> res = new ArrayList<PartyRoleNameDetailSupplementalData>();
            setResults(res);
            return res;
        }

        PartyLookupService.prepareFindParties(this);

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            // set options on what is searched
            crmSearch.setSearchLeads(true);
            prepareSearch(crmSearch);
            return extractPartiesResults(crmSearch.getLeads(), RoleTypeConstants.PROSPECT);
        } catch (ServiceException e) {
            storeException(e);
            return null;
        }
    }

    private void prepareSearch(CrmsfaSearchService crmSearch) throws ServiceException {
        // set the common parameters
        crmSearch.setInfrastructure(getProvider().getInfrastructure());
        crmSearch.setUser(getProvider().getUser());
        crmSearch.setKeywords(getSuggestQuery());
        // pass the pagination parameters to the service
        crmSearch.setPageStart(getPager().getPageStart());
        crmSearch.setPageSize(getPager().getPageSize());
        crmSearch.search();
        // retrieve the number of hits
        setResultTotalCount(crmSearch.getResultSize());
    }

    private List<PartyRoleNameDetailSupplementalData> extractPartiesResults(List<? extends Party> parties, String roleTypeId) {
        // convert the list of Parties to PartyFromByRelnAndContactInfoAndPartyClassification
        return findList(PartyRoleNameDetailSupplementalData.class,
                        EntityCondition.makeCondition(
                            EntityCondition.makeCondition(PartyRoleNameDetailSupplementalData.Fields.partyId.name(),
                                                          EntityOperator.IN,
                                                          Entity.getDistinctFieldValues(String.class, parties, Party.Fields.partyId)),
                            EntityCondition.makeCondition(PartyRoleNameDetailSupplementalData.Fields.roleTypeId.name(), roleTypeId))
                        , false); // do not paginate as this was done by the search service already
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
