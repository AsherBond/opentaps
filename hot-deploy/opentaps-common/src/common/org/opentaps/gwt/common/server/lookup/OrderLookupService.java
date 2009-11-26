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
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.domain.base.entities.OrderHeaderItemAndRolesAndInvPending;
import org.opentaps.domain.search.SalesOrderSearchRepositoryInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.OrderLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate the OrderListView and Order autocompleters widgets.
 */
public class OrderLookupService extends EntityLookupAndSuggestService {
    private static final String MODULE = OrderLookupService.class.getName();

    /**
     * Creates a new <code>PartyLookupService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     */
    public OrderLookupService(InputProviderInterface provider) {
        super(provider, OrderLookupConfiguration.LIST_OUT_FIELDS);
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
        OrderLookupService service = new OrderLookupService(provider);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        // use Locale.US for change gwt date input
        service.findOrders(Locale.US, timeZone);
        return json.makeLookupResponse(OrderLookupConfiguration.INOUT_ORDER_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * Finds a list of <code>Order</code>.
     * @param locale a <code>Locale</code> value
     * @param timeZone a <code>TimeZone</code> value
     * @return the list of <code>Order</code>, or <code>null</code> if an error occurred
     */
    public List<OrderHeaderItemAndRolesAndInvPending> findOrders(Locale locale, TimeZone timeZone) {
        try {
            SalesOrderSearchRepositoryInterface salesOrderSearchRepository = getDomainsDirectory().getSearchDomain().getSalesOrderSearchRepository();
            String organizationPartyId = UtilProperties.getPropertyValue("opentaps", "organizationPartyId");
            String userLoginId = null;
            if (getProvider().getUser().getOfbizUserLogin() != null) {
                userLoginId = getProvider().getUser().getOfbizUserLogin().getString("userLoginId");
            } else {
                Debug.logError("Current session do not have any UserLogin set.", MODULE);
            }
            // pass locale and timeZone instances for format the date string
            salesOrderSearchRepository.setLocale(locale);
            salesOrderSearchRepository.setTimeZone(timeZone);
            // pass parameters into repository
            salesOrderSearchRepository.setUserLoginId(userLoginId);
            salesOrderSearchRepository.setOrganizationPartyId(organizationPartyId);
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.IN_FROM_DATE))) {
                salesOrderSearchRepository.setFromDate(getProvider().getParameter(OrderLookupConfiguration.IN_FROM_DATE));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.IN_THRU_DATE))) {
                salesOrderSearchRepository.setThruDate(getProvider().getParameter(OrderLookupConfiguration.IN_THRU_DATE));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.IN_RESPONSIBILTY))) {
                salesOrderSearchRepository.setViewPref(getProvider().getParameter(OrderLookupConfiguration.IN_RESPONSIBILTY));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.INOUT_ORDER_ID))) {
                salesOrderSearchRepository.setOrderId(getProvider().getParameter(OrderLookupConfiguration.INOUT_ORDER_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.IN_EXTERNAL_ID))) {
                salesOrderSearchRepository.setExteralOrderId(getProvider().getParameter(OrderLookupConfiguration.IN_EXTERNAL_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.INOUT_ORDER_NAME))) {
                salesOrderSearchRepository.setOrderName(getProvider().getParameter(OrderLookupConfiguration.INOUT_ORDER_NAME));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.INOUT_PARTY_ID))) {
                salesOrderSearchRepository.setCustomerPartyId(getProvider().getParameter(OrderLookupConfiguration.INOUT_PARTY_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.IN_PRDOUCT_STORE_ID))) {
                salesOrderSearchRepository.setProductStoreId(getProvider().getParameter(OrderLookupConfiguration.IN_PRDOUCT_STORE_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.INOUT_STATUS_ID))) {
                salesOrderSearchRepository.setStatusId(getProvider().getParameter(OrderLookupConfiguration.INOUT_STATUS_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.INOUT_CORRESPONDING_PO_ID))) {
                salesOrderSearchRepository.setPurchaseOrderId(getProvider().getParameter(OrderLookupConfiguration.INOUT_CORRESPONDING_PO_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.IN_CREATED_BY))) {
                salesOrderSearchRepository.setCreatedBy(getProvider().getParameter(OrderLookupConfiguration.IN_CREATED_BY));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.IN_LOT_ID))) {
                salesOrderSearchRepository.setLotId(getProvider().getParameter(OrderLookupConfiguration.IN_LOT_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(OrderLookupConfiguration.IN_SERIAL_NUMBER))) {
                salesOrderSearchRepository.setSerialNumber(getProvider().getParameter(OrderLookupConfiguration.IN_SERIAL_NUMBER));
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
        String orderName = value.getString("orderName");
        String orderId = value.getString("orderId");
        if (UtilValidate.isNotEmpty(orderName)) {
            sb.append(orderName);
        }
        sb.append(" (").append(orderId).append(")");

        return sb.toString();
    }

}
