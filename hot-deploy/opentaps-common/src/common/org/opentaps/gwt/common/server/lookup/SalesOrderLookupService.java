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
package org.opentaps.gwt.common.server.lookup;

import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.common.domain.order.OrderViewForListing;
import org.opentaps.domain.search.order.SalesOrderSearchRepositoryInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.SalesOrderLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate the OrderListView and Order autocompleters widgets.
 * @author <a href="mailto:jeremy@iznogoud">Wickersheimer Jeremy</a>
 * @version 1.0
 */
public class SalesOrderLookupService extends EntityLookupAndSuggestService {
    private static final String MODULE = SalesOrderLookupService.class.getName();

    /**
     * Creates a new <code>PartyLookupService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     */
    public SalesOrderLookupService(InputProviderInterface provider) {
        super(provider, SalesOrderLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to perform lookups on Orders.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findOrders(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        SalesOrderLookupService service = new SalesOrderLookupService(provider);
        // The GWT date input is always using the US locale -- (it should be using the user locale though ...)
        service.findOrders(Locale.US);
        return json.makeLookupResponse(SalesOrderLookupConfiguration.INOUT_ORDER_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * Finds a list of <code>Order</code>.
     * @return the list of <code>Order</code>, or <code>null</code> if an error occurred
     */
    public List<OrderViewForListing> findOrders() {
        return findOrders(getProvider().getLocale());
    }

    /**
     * Finds a list of <code>Order</code>.
     * @param locale force a <code>Locale</code> value
     * @return the list of <code>Order</code>, or <code>null</code> if an error occurred
     */
    public List<OrderViewForListing> findOrders(Locale locale) {
        try {
            SalesOrderSearchRepositoryInterface salesOrderSearchRepository = getDomainsDirectory().getOrderDomain().getSalesOrderSearchRepository();

            String organizationPartyId = UtilProperties.getPropertyValue("opentaps", "organizationPartyId");

            String userLoginId = null;
            if (getProvider().getUser().getOfbizUserLogin() != null) {
                userLoginId = getProvider().getUser().getOfbizUserLogin().getString("userLoginId");
            } else {
                Debug.logError("Current session do not have any UserLogin set.", MODULE);
            }

            // pass locale and timeZone instances for format the date string
            // use Locale.US for change gwt date input -- is this required to be US ??
            salesOrderSearchRepository.setLocale(locale);
            salesOrderSearchRepository.setTimeZone(getProvider().getTimeZone());

            // pass parameters into repository
            salesOrderSearchRepository.setUserLoginId(userLoginId);
            salesOrderSearchRepository.setOrganizationPartyId(organizationPartyId);

            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.IN_FROM_DATE))) {
                salesOrderSearchRepository.setFromDate(getProvider().getParameter(SalesOrderLookupConfiguration.IN_FROM_DATE));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.IN_THRU_DATE))) {
                salesOrderSearchRepository.setThruDate(getProvider().getParameter(SalesOrderLookupConfiguration.IN_THRU_DATE));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.IN_RESPONSIBILTY))) {
                salesOrderSearchRepository.setViewPref(getProvider().getParameter(SalesOrderLookupConfiguration.IN_RESPONSIBILTY));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.INOUT_ORDER_ID))) {
                salesOrderSearchRepository.setOrderId(getProvider().getParameter(SalesOrderLookupConfiguration.INOUT_ORDER_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.IN_EXTERNAL_ID))) {
                salesOrderSearchRepository.setExteralOrderId(getProvider().getParameter(SalesOrderLookupConfiguration.IN_EXTERNAL_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.INOUT_ORDER_NAME))) {
                salesOrderSearchRepository.setOrderName(getProvider().getParameter(SalesOrderLookupConfiguration.INOUT_ORDER_NAME));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.INOUT_PARTY_ID))) {
                salesOrderSearchRepository.setCustomerPartyId(getProvider().getParameter(SalesOrderLookupConfiguration.INOUT_PARTY_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.IN_PRODUCT_STORE_ID))) {
                salesOrderSearchRepository.setProductStoreId(getProvider().getParameter(SalesOrderLookupConfiguration.IN_PRODUCT_STORE_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.INOUT_STATUS_ID))) {
                salesOrderSearchRepository.setStatusId(getProvider().getParameter(SalesOrderLookupConfiguration.INOUT_STATUS_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.INOUT_CORRESPONDING_PO_ID))) {
                salesOrderSearchRepository.setPurchaseOrderId(getProvider().getParameter(SalesOrderLookupConfiguration.INOUT_CORRESPONDING_PO_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.IN_CREATED_BY))) {
                salesOrderSearchRepository.setCreatedBy(getProvider().getParameter(SalesOrderLookupConfiguration.IN_CREATED_BY));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.IN_LOT_ID))) {
                salesOrderSearchRepository.setLotId(getProvider().getParameter(SalesOrderLookupConfiguration.IN_LOT_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(SalesOrderLookupConfiguration.IN_SERIAL_NUMBER))) {
                salesOrderSearchRepository.setSerialNumber(getProvider().getParameter(SalesOrderLookupConfiguration.IN_SERIAL_NUMBER));
            }

            // takes into account order statuses
            // activeOnly & desired flags aren't mutually exclusive, activeOnly statuses is a superset 
            // of desired statuses. 
            String isActiveOnly = getProvider().getParameter(SalesOrderLookupConfiguration.IN_FIND_ALL);
            if (UtilValidate.isNotEmpty(isActiveOnly) && "Y".equals(isActiveOnly)) {
                salesOrderSearchRepository.setFindActiveOnly(true);
            }
            String isDesired = getProvider().getParameter(SalesOrderLookupConfiguration.IN_DESIRED);
            if (UtilValidate.isNotEmpty(isDesired) && "Y".equals(isDesired)) {
                salesOrderSearchRepository.setFindDesiredOnly(true);
            }

            // set sort conditions
            salesOrderSearchRepository.setOrderBy(getOrderBy());

            // return the matching result
            return paginateResults(salesOrderSearchRepository.findOrders());

        } catch (RepositoryException e) {
            storeException(e);
            return null;
        }
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface value) {
        StringBuffer sb = new StringBuffer();
        String orderName = value.getString(OrderViewForListing.Fields.orderName.name());
        String orderId = value.getString(OrderViewForListing.Fields.orderId.name());
        if (UtilValidate.isNotEmpty(orderName)) {
            sb.append(orderName);
        }
        sb.append(" (").append(orderId).append(")");

        return sb.toString();
    }

}
