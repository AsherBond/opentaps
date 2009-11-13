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

package org.opentaps.warehouse.inventory;

import java.math.BigDecimal;
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
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.base.entities.GoodIdentification;
import org.opentaps.domain.base.entities.OrderItemShipGrpInvRes;
import org.opentaps.domain.base.entities.Product;
import org.opentaps.domain.base.entities.ProductFacilityLocation;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession(true).getAttribute("userLogin");
        String productId = UtilCommon.getParameter(request, "productId");
        String facilityId = UtilCommon.getParameter(request, "facilityId");
        TimeZone timeZone = UtilCommon.getTimeZone(request);
        Locale locale = UtilHttp.getLocale(request);
        
        GenericValue facility = delegator.findByPrimaryKeyCache("Facility", UtilMisc.toMap("facilityId", facilityId));
        Map<String, Object> resp = new HashMap<String, Object>();
        try {
            Infrastructure infrastructure = new Infrastructure(dispatcher);
            Session session = infrastructure.getSession();
            Map svcResult = dispatcher.runSync("getProductByComprehensiveSearch", UtilMisc.toMap("productId", productId));
            if (!(ServiceUtil.isError(svcResult) || ServiceUtil.isFailure(svcResult))) {
                GenericValue productGv = (GenericValue) svcResult.get("product");
                if (productGv != null) {
                    resp.put("internalName", productGv.getString("internalName"));
                    resp.put("productId", productGv.getString("productId"));
                    // fill the productFacilityLocations selections
                    Product product = (Product) session.get(Product.class, productGv.getString("productId"));
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
                    String hql = "from GoodIdentification eo where eo.id.productId = :productId";
                    Query query = session.createQuery(hql);
                    query.setParameter("productId", productGv.get("productId"));
                    List<GoodIdentification> goodIdentifications = query.list();
                    values = new ArrayList<Map<String, Object>>();
                    for (GoodIdentification goodIdentification : goodIdentifications) {
                        Map<String, Object> value = new HashMap<String, Object>();
                        value.put("goodIdentificationTypeId", goodIdentification.getGoodIdentificationTypeId());
                        value.put("idValue", goodIdentification.getIdValue());
                        values.add(value);
                    }
                    resp.put("goodIdentifications", values);
                    
                    GenericValue facilityOwnerAcctgPref = delegator.findByPrimaryKeyCache("PartyAcctgPreference", UtilMisc.toMap("partyId", facility.getString("ownerPartyId")));
                    
                    svcResult = dispatcher.runSync("getProductCost", UtilMisc.toMap("productId", productGv.getString("productId"),
                                    "costComponentTypePrefix", "EST_STD", "currencyUomId", facilityOwnerAcctgPref.get("baseCurrencyUomId"),
                                    "userLogin", userLogin));
                    if (!ServiceUtil.isError(svcResult)) {
                        BigDecimal unitCost = (BigDecimal) svcResult.get("productCost");
                        resp.put("unitCost", unitCost);
                    }
                    
                    // find back ordered items
                    // use product.productId in case the productId passed in parameters was a goodId which was used to look up the product
                    hql = "from OrderItemShipGrpInvRes eo where eo.inventoryItem.productId = :productId and eo.inventoryItem.facilityId = :facilityId and eo.quantityNotAvailable is not null order by eo.reservedDatetime, eo.sequenceId";
                    query = session.createQuery(hql);
                    query.setParameter("productId", productGv.get("productId"));
                    query.setParameter("facilityId", facilityId);
                    List<OrderItemShipGrpInvRes> backOrderedItems = query.list();
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
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            return doJSONResponse(response, FastList.newInstance());
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), MODULE);
            return doJSONResponse(response, FastList.newInstance());
        } catch (InfrastructureException e) {
            Debug.logError(e.getMessage(), MODULE);
            return doJSONResponse(response, FastList.newInstance());
        } catch (RepositoryException e) {
            Debug.logError(e.getMessage(), MODULE);
            return doJSONResponse(response, FastList.newInstance());
        }
        return doJSONResponse(response, resp);
    }
    

}
