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

package org.opentaps.warehouse.inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.base.entities.Facility;
import org.opentaps.base.entities.GoodIdentification;
import org.opentaps.base.entities.PartyAcctgPreference;
import org.opentaps.base.entities.ProductFacilityLocation;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.order.OrderItemShipGrpInvRes;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Warehouse ajax events to be invoked by the controller.
 */
public final class AjaxEvents {
    private AjaxEvents() { }

    private static final String MODULE = AjaxEvents.class.getName();

    /**
     * Using common method to return json response.
     */
    private static String doJSONResponse(HttpServletResponse response, Collection<?> collection) {
        return org.opentaps.common.event.AjaxEvents.doJSONResponse(response, JSONArray.fromObject(collection).toString());
    }

    /**
     * Using common method to return json response.
     */
    private static String doJSONResponse(HttpServletResponse response, Map map) {
        return org.opentaps.common.event.AjaxEvents.doJSONResponse(response, JSONObject.fromObject(map));
    }

    /** Return the objects which related with product in receive inventory form.
     * @throws GenericEntityException
     */
    @SuppressWarnings("unchecked")
    public static String getReceiveInventoryProductRelatedsJSON(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession(true).getAttribute("userLogin");
        String productId = UtilCommon.getParameter(request, "productId");
        String facilityId = UtilCommon.getParameter(request, "facilityId");
        TimeZone timeZone = UtilCommon.getTimeZone(request);
        Locale locale = UtilHttp.getLocale(request);

        Map<String, Object> resp = new HashMap<String, Object>();
        try {
            // initial domain objects
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            ProductRepositoryInterface productRepository = dl.loadDomainsDirectory().getProductDomain().getProductRepository();
            OrderRepositoryInterface orderRepository = dl.loadDomainsDirectory().getOrderDomain().getOrderRepository();
            InventoryRepositoryInterface inventoryRepository = dl.loadDomainsDirectory().getInventoryDomain().getInventoryRepository();
            OrganizationRepositoryInterface organizationRepository = dl.loadDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
            // get Product domain object by product id
            Product product = productRepository.getProductByComprehensiveSearch(productId);
            // get Facility domain object
            Facility facility = inventoryRepository.getFacilityById(facilityId);
            if (product != null) {
                Organization organization = organizationRepository.getOrganizationById(facility.getOwnerPartyId());
                PartyAcctgPreference facilityOwnerAcctgPref = organization.getPartyAcctgPreference();
                resp.put("internalName", product.getInternalName());
                resp.put("productId", product.getProductId());
                resp.put("unitCost", product.getStandardCost(facilityOwnerAcctgPref.getBaseCurrencyUomId()));
                // retrieve ProductFacilityLocation entities and set it to response
                List<ProductFacilityLocation> productFacilityLocations = (List<ProductFacilityLocation>) product.getProductFacilityLocations();
                List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
                for (ProductFacilityLocation productFacilityLocation: productFacilityLocations) {
                    if (facilityId.equals(productFacilityLocation.getFacilityId())) {
                        Map<String, Object> value = new HashMap<String, Object>();
                        value.put("locationSeqId", productFacilityLocation.getLocationSeqId());
                        if (productFacilityLocation.getFacilityLocation() != null) {
                            value.put("facilityLocationAreaId", productFacilityLocation.getFacilityLocation().getAreaId());
                            value.put("facilityLocationAisleId", productFacilityLocation.getFacilityLocation().getAisleId());
                            value.put("facilityLocationSectionId", productFacilityLocation.getFacilityLocation().getSectionId());
                            value.put("facilityLocationLevelId", productFacilityLocation.getFacilityLocation().getLevelId());
                            value.put("facilityLocationPositionId", productFacilityLocation.getFacilityLocation().getPositionId());
                            if (productFacilityLocation.getFacilityLocation().getTypeEnumeration() != null) {
                                value.put("facilityLocationTypeEnumDescription", productFacilityLocation.getFacilityLocation().getTypeEnumeration().get("description", locale));
                            }
                        }
                        values.add(value);
                    }
                }
                resp.put("productFacilityLocations", values);
                // retrieve GoodIdentification entities and set it to response
                List<GoodIdentification> goodIdentifications = productRepository.getAlternateProductIds(product.getProductId());
                values = new ArrayList<Map<String, Object>>();
                for (GoodIdentification goodIdentification : goodIdentifications) {
                    Map<String, Object> value = new HashMap<String, Object>();
                    value.put("goodIdentificationTypeId", goodIdentification.getGoodIdentificationTypeId());
                    value.put("idValue", goodIdentification.getIdValue());
                    values.add(value);
                }
                resp.put("goodIdentifications", values);


                // find back ordered items
                // use product.productId in case the productId passed in parameters was a goodId which was used to look up the product
                List<OrderItemShipGrpInvRes> backOrderedItems = orderRepository.getBackOrderedInventoryReservations(product.getProductId(), facilityId);
                values = new ArrayList<Map<String, Object>>();
                for (OrderItemShipGrpInvRes backOrderedItem : backOrderedItems) {
                    Map<String, Object> value = new HashMap<String, Object>();
                    value.put("reservedDatetime", UtilDateTime.timeStampToString(backOrderedItem.getReservedDatetime(), timeZone, locale));
                    value.put("sequenceId", backOrderedItem.getSequenceId());
                    value.put("orderId", backOrderedItem.getOrderId());
                    value.put("orderItemSeqId", backOrderedItem.getOrderItemSeqId());
                    value.put("quantity", backOrderedItem.getQuantity());
                    value.put("quantityNotAvailable", backOrderedItem.getQuantityNotAvailable());
                    values.add(value);
                }
                resp.put("backOrderedItems", values);
            }
        } catch (RepositoryException e) {
            Debug.logError(e.getMessage(), MODULE);
            return doJSONResponse(response, FastList.newInstance());
        } catch (EntityNotFoundException e) {
            Debug.logError(e.getMessage(), MODULE);
            return doJSONResponse(response, FastList.newInstance());
        }
        return doJSONResponse(response, resp);
    }


}
