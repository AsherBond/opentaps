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

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.OrderHeaderAndRoles;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.search.order.PurchaseOrderSearchRepositoryInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.PurchaseOrderLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
/**
 * The RPC service used to populate the PurchaseOrderListView and PurchaseOrder autocompleters widgets.
 */
public class PurchaseOrderLookupService extends EntityLookupAndSuggestService {
    private static final String MODULE = PurchaseOrderLookupService.class.getName();

    /**
     * Creates a new <code>PartyLookupService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     */
    public PurchaseOrderLookupService(InputProviderInterface provider) {
        super(provider, PurchaseOrderLookupConfiguration.LIST_OUT_FIELDS);
    }

 
    /**
     * AJAX event to perform lookups on purchasing Orders.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findOrders(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PurchaseOrderLookupService service = new PurchaseOrderLookupService(provider);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        String facilityId = (String) request.getSession().getAttribute("facilityId");

        // use Locale.US for change gwt date input
        service.findOrders(Locale.US, timeZone, organizationPartyId, facilityId);
        return json.makeLookupResponse(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * Finds a list of <code>Order</code>.
     * @param locale a <code>Locale</code> value
     * @param timeZone a <code>TimeZone</code> value
     * @return the list of <code>Order</code>, or <code>null</code> if an error occurred
     */
    public List<OrderHeaderAndRoles> findOrders(Locale locale, TimeZone timeZone, String organizationPartyId, String facilityId) {
        try {
            PurchaseOrderSearchRepositoryInterface purchaseOrderSearchRepository = getDomainsDirectory().getOrderDomain().getPurchaseOrderSearchRepository();
            InventoryRepositoryInterface inventoryRepository = getDomainsDirectory().getInventoryDomain().getInventoryRepository();
            if (UtilValidate.isEmpty(organizationPartyId)) {
                organizationPartyId = inventoryRepository.getFacilityById(facilityId).getOwnerPartyId();
            }
            // pass locale and timeZone instances for format the date string
            purchaseOrderSearchRepository.setLocale(locale);
            purchaseOrderSearchRepository.setTimeZone(timeZone);
            // pass parameters into repository
            purchaseOrderSearchRepository.setOrganizationPartyId(organizationPartyId);

            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_FROM_DATE))) {
                purchaseOrderSearchRepository.setFromDate(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_FROM_DATE));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_THRU_DATE))) {
                purchaseOrderSearchRepository.setThruDate(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_THRU_DATE));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID))) {
                purchaseOrderSearchRepository.setOrderId(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_ORDER_NAME))) {
                purchaseOrderSearchRepository.setOrderName(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_ORDER_NAME));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_PRODUCT_PARTTERN))) {
                purchaseOrderSearchRepository.setProductPattern(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_PRODUCT_PARTTERN));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_PARTY_ID))) {
                purchaseOrderSearchRepository.setSupplierPartyId(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_PARTY_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_STATUS_ID))) {
                purchaseOrderSearchRepository.setStatusId(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_STATUS_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_CREATED_BY))) {
                purchaseOrderSearchRepository.setCreatedBy(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_CREATED_BY));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_FIND_ALL))) {
                purchaseOrderSearchRepository.setFindAll(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_FIND_ALL));
            }

            // set sort conditions
            purchaseOrderSearchRepository.setOrderBy(getOrderBy());
            // return the matching result
            return paginateResults(purchaseOrderSearchRepository.findOrders());
        } catch (RepositoryException e) {
            storeException(e);
            return null;
        } catch (EntityNotFoundException e) {
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
