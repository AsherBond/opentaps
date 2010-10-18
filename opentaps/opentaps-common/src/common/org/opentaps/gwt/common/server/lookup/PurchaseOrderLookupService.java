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
package org.opentaps.gwt.common.server.lookup;

import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.UtilValidate;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.order.OrderViewForListing;
import org.opentaps.domain.order.PurchaseOrderLookupRepositoryInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.lookup.configuration.PurchaseOrderLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * Repository to lookup Purchase Orders.
 */
public class PurchaseOrderLookupService extends EntityLookupAndSuggestService {

    @SuppressWarnings("unused")
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
        String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        String facilityId = (String) request.getSession().getAttribute("facilityId");

        // use Locale.US for change gwt date input
        service.findOrders(Locale.US, organizationPartyId, facilityId);
        return json.makeLookupResponse(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * Finds a list of <code>Order</code>.
     * @param organizationPartyId a <code>String</code> value
     * @param facilityId a <code>String</code> value
     * @return the list of <code>Order</code>, or <code>null</code> if an error occurred
     */
    public List<OrderViewForListing> findOrders(String organizationPartyId, String facilityId) {
        return findOrders(getProvider().getLocale(), organizationPartyId, facilityId);
    }

    /**
     * Finds a list of <code>Order</code>.
     * @param locale a <code>Locale</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param facilityId a <code>String</code> value
     * @return the list of <code>Order</code>, or <code>null</code> if an error occurred
     */
    public List<OrderViewForListing> findOrders(Locale locale, String organizationPartyId, String facilityId) {
        try {
            PurchaseOrderLookupRepositoryInterface purchaseOrderLookupRepository = getDomainsDirectory().getOrderDomain().getPurchaseOrderLookupRepository();
            InventoryRepositoryInterface inventoryRepository = getDomainsDirectory().getInventoryDomain().getInventoryRepository();
            if (UtilValidate.isEmpty(organizationPartyId)) {
                organizationPartyId = inventoryRepository.getFacilityById(facilityId).getOwnerPartyId();
            }
            // pass locale and timeZone instances for format the date string
            purchaseOrderLookupRepository.setLocale(locale);
            purchaseOrderLookupRepository.setTimeZone(getProvider().getTimeZone());
            // pass parameters into repository
            purchaseOrderLookupRepository.setOrganizationPartyId(organizationPartyId);

            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_FROM_DATE))) {
                purchaseOrderLookupRepository.setFromDate(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_FROM_DATE));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_THRU_DATE))) {
                purchaseOrderLookupRepository.setThruDate(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_THRU_DATE));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID))) {
                purchaseOrderLookupRepository.setOrderId(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_ORDER_NAME))) {
                purchaseOrderLookupRepository.setOrderName(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_ORDER_NAME));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_PRODUCT_PARTTERN))) {
                purchaseOrderLookupRepository.setProductPattern(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_PRODUCT_PARTTERN));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_PARTY_ID))) {
                purchaseOrderLookupRepository.setSupplierPartyId(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_PARTY_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_STATUS_ID))) {
                purchaseOrderLookupRepository.setStatusId(getProvider().getParameter(PurchaseOrderLookupConfiguration.INOUT_STATUS_ID));
            }
            if (UtilValidate.isNotEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_CREATED_BY))) {
                purchaseOrderLookupRepository.setCreatedBy(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_CREATED_BY));
            }
            if (UtilValidate.isEmpty(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_FIND_ALL)) || "N".equals(getProvider().getParameter(PurchaseOrderLookupConfiguration.IN_FIND_ALL))) {
                purchaseOrderLookupRepository.setFindDesiredOnly(true);
            }

            // set sort conditions
            purchaseOrderLookupRepository.setOrderBy(getOrderBy());

            // set the pagination
            if (!"Y".equals(getProvider().getParameter(UtilLookup.PARAM_EXPORT_EXCEL))) { 
            	purchaseOrderLookupRepository.setPageStart(getPager().getPageStart());
            	purchaseOrderLookupRepository.setPageSize(getPager().getPageSize());
            } else {
            	purchaseOrderLookupRepository.enablePagination(false);
            }

            // return the matching result
            List<OrderViewForListing> results = purchaseOrderLookupRepository.findOrders();
            setResults(results);
            setResultTotalCount(purchaseOrderLookupRepository.getResultSize());
            return results;
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
