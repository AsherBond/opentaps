package org.opentaps.gwt.purchasing.server.search;

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
import org.opentaps.base.entities.OrderHeaderItemAndRolesAndInvPending;
import org.opentaps.base.entities.PartyGroup;
import org.opentaps.base.entities.PartyRoleNameDetailSupplementalData;
import org.opentaps.common.util.ConvertMapToString;
import org.opentaps.common.util.ICompositeValue;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderViewForListing;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.PurchaseOrderLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.SearchLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.gwt.common.server.lookup.EntityLookupAndSuggestService;
import org.opentaps.gwt.common.server.lookup.JsonResponse;
import org.opentaps.gwt.common.server.lookup.PartyLookupService;
import org.opentaps.purchasing.search.PurchasingSearchService;

public final class PurchasingSearchLookupService extends EntityLookupAndSuggestService {

    private static final String MODULE = PurchasingSearchLookupService.class.getName();

    /**
     * Creates a new <code>PurchasingSearchService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     * @param fields the list of fields that will be in the response
     */
    private PurchasingSearchLookupService(InputProviderInterface provider, List<String> fields) {
        super(provider, fields);
    }

    /**
     * Creates a new <code>PurchasingSearchService</code> instance for searching parties.
     * @param provider an <code>InputProviderInterface</code> value
     * @return a <code>PurchasingSearchLookupService</code> value
     */
    public static PurchasingSearchLookupService makePartySearchService(InputProviderInterface provider) {
        return new PurchasingSearchLookupService(provider, PartyLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * Creates a new <code>PurchasingSearchService</code> instance for searching sales orders.
     * @param provider an <code>InputProviderInterface</code> value
     * @return a <code>PurchasingSearchLookupService</code> value
     */
    public static PurchasingSearchLookupService makeSalesOrderSearchService(InputProviderInterface provider) {
        return new PurchasingSearchLookupService(provider, PurchaseOrderLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to perform a global CRMSFA search.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String purchasingSearch(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PurchasingSearchLookupService service = PurchasingSearchLookupService.makePartySearchService(provider);
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
    public static String purchasingSearchSuppliers(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PurchasingSearchLookupService service = PurchasingSearchLookupService.makePartySearchService(provider);
        service.searchSuppliers();
        return json.makeLookupResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service, request.getSession(true).getServletContext());
    }
    
    /**
     * Searches a List of Accounts.
     * @return a list of <code>PartyFromByRelnAndContactInfoAndPartyClassification</code>
     */
    public List<PartyRoleNameDetailSupplementalData> searchSuppliers() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().equals("")) {
            List<PartyRoleNameDetailSupplementalData> res = new ArrayList<PartyRoleNameDetailSupplementalData>();
            setResults(res);
            return res;
        }

        PartyLookupService.prepareFindParties(this);

        try {
            PurchasingSearchService purchasingSearch = new PurchasingSearchService();
            // set options on what is searched
            purchasingSearch.setSearchSuppliers(true);
            prepareSearch(purchasingSearch);
            return extractPartiesResults(purchasingSearch.getSuppliers());
        } catch (ServiceException e) {
            storeException(e);
            return null;
        }
    }


    /**
     * AJAX event to search Sales Orders.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String purchasingSearchPurchaseOrders(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PurchasingSearchLookupService service = PurchasingSearchLookupService.makeSalesOrderSearchService(provider);
        service.searchPurchaseOrders();
        return json.makeLookupResponse(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * Searches a List of Purchase Order.
     * @return a list of <code>OrderViewForListing</code>
     */
    public List<OrderViewForListing> searchPurchaseOrders() {

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
                return (String) value.get(PurchaseOrderLookupConfiguration.OUT_ORDER_NAME_ID);
            }
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(2);
                s.add(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);
                s.add(PurchaseOrderLookupConfiguration.INOUT_ORDER_NAME);
                return s;
            }
        }

        class StatusDecriptionSortable extends ConvertMapToString implements ICompositeValue {
            @Override
            public String convert(Map<String, ?> value) {
                if (value == null) {
                    return null;
                }
                return (String) value.get(PurchaseOrderLookupConfiguration.OUT_STATUS_DESCRIPTION);
            }
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(1);
                s.add(PurchaseOrderLookupConfiguration.INOUT_STATUS_ID);
                return s;
            }
        }


        class OrderDateSortable extends ConvertMapToString implements ICompositeValue {
            @Override
            public String convert(Map<String, ?> value) {
                if (value == null) {
                    return null;
                }
                return (String) value.get(PurchaseOrderLookupConfiguration.OUT_ORDER_DATE_STRING);
            }
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(1);
                s.add(PurchaseOrderLookupConfiguration.INOUT_ORDER_DATE);
                return s;
            }
        }

        class SupplierNameSortable extends ConvertMapToString implements ICompositeValue {
            @Override
            public String convert(Map<String, ?> value) {
                if (value == null) {
                    return null;
                }
                return (String) value.get(PurchaseOrderLookupConfiguration.OUT_SUPPLIER_NAME);
            }
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(1);
                s.add(PurchaseOrderLookupConfiguration.INOUT_PARTY_ID);
                return s;
            }
        }

        // some fields in the view are not in the DB, setup the mapping for the order by
        Map<String, ConvertMapToString> calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(PurchaseOrderLookupConfiguration.OUT_ORDER_NAME_ID, new OrderNameIdSortable());
        makeCalculatedField(calcField);

        calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(PurchaseOrderLookupConfiguration.OUT_STATUS_DESCRIPTION, new StatusDecriptionSortable());
        makeCalculatedField(calcField);


        calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(PurchaseOrderLookupConfiguration.OUT_ORDER_DATE_STRING, new OrderDateSortable());
        makeCalculatedField(calcField);

        calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(PurchaseOrderLookupConfiguration.OUT_SUPPLIER_NAME, new SupplierNameSortable());
        makeCalculatedField(calcField);

        try {
            PurchasingSearchService purchaseSearch = new PurchasingSearchService();
            // set options on what is searched
            purchaseSearch.setSearchPurchaseOrders(true);
            prepareSearch(purchaseSearch);
            // we need to lookup OrderHeaderItemAndRolesAndInvPending and convert those into OrderViewForListing
            // to get the OrderHeaderItemAndRolesAndInvPending with the customer we filter by role
            List<OrderHeaderItemAndRolesAndInvPending> res = getRepository().findList(OrderHeaderItemAndRolesAndInvPending.class,
                            EntityCondition.makeCondition(EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.roleTypeId.name(),
                                                                                        EntityOperator.EQUALS,
                                                                                        RoleTypeConstants.BILL_FROM_VENDOR),
                                                          EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.orderId.name(),
                                                                                        EntityOperator.IN,
                                                                                        Entity.getDistinctFieldValues(String.class, purchaseSearch.getPurchaseOrders(), Order.Fields.orderId))),
                            PurchaseOrderLookupConfiguration.LIST_QUERY_FIELDS, getOrderBy());
            // convert and paginate
            return paginateResults(OrderViewForListing.makeOrderView(res, getProvider().getInfrastructure().getDelegator(), getProvider().getTimeZone(), getProvider().getLocale()));
        } catch (ServiceException e) {
            storeException(e);
            return null;
        } catch (RepositoryException e) {
            storeException(e);
            return null;
        }
    }


    private void prepareSearch(PurchasingSearchService crmSearch) throws ServiceException {
        // set the common parameters
        crmSearch.setInfrastructure(getProvider().getInfrastructure());
        crmSearch.setUser(getProvider().getUser());
        crmSearch.setKeywords(getSuggestQuery());
        // pass the pagination parameters to the service
        crmSearch.setPageStart(getPager().getPageStart());
        crmSearch.setPageSize(getPager().getPageSize());
        crmSearch.search();
    }

    private List<PartyRoleNameDetailSupplementalData> extractPartiesResults(List<? extends PartyGroup> parties) {
        // convert the list of Parties to PartyFromByRelnAndContactInfoAndPartyClassification
        return findList(PartyRoleNameDetailSupplementalData.class,
                        EntityCondition.makeCondition(PartyRoleNameDetailSupplementalData.Fields.partyId.name(),
                                                      EntityOperator.IN,
                                                      Entity.getDistinctFieldValues(String.class, parties, PartyGroup.Fields.partyId)));
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface result) {
        if (result instanceof Party) {
            return makeDisplayedText((Party) result);
        }  else if (result instanceof Order) {
            return makeDisplayedText((Order) result);
        } else {
            return null;
        }
    }

    @Override
    public Map<String, String> makeExtraSuggestValues(EntityInterface result) {
        if (result instanceof Party) {
            return makeExtraValues((Party) result);
        } else if (result instanceof Order) {
            return makeExtraValues((Order) result);
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
            extras.put(SearchLookupConfiguration.RESULT_DESCRIPTION, "Status:" + order.getStatus().getDescription() + " Vendor: " + order.getBillFromVendor().getName());
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
            PurchasingSearchService purchasingSearch = new PurchasingSearchService();
            purchasingSearch.setInfrastructure(getProvider().getInfrastructure());
            purchasingSearch.setUser(getProvider().getUser());
            purchasingSearch.setKeywords(getSuggestQuery());
            // pass the pagination parameters to the service
            purchasingSearch.setPageStart(getPager().getPageStart());
            purchasingSearch.setPageSize(getPager().getPageSize());
            // set options on what is searched
            purchasingSearch.setSearchSuppliers(true);
            purchasingSearch.setSearchPurchaseOrders(true);
            purchasingSearch.search();
            List<Order> orders = purchasingSearch.getPurchaseOrders();
            List<PartyGroup> suppliers = purchasingSearch.getSuppliers();
            // get results
            List<EntityInterface> r = new ArrayList<EntityInterface>();
            r.addAll(suppliers);
            r.addAll(orders);

            // no pagination needed, already done in the service (see above)
            setResultTotalCount(purchasingSearch.getResultSize());
            setResults(r);

        } catch (ServiceException e) {
            storeException(e);
            return null;
        }

        return getResults();
    }

}
