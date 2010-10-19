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

/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

/* This file may contain code which has been modified from that included with the Apache-licensed OFBiz product application */
/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.warehouse.inventory;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.warehouse.security.WarehouseSecurity;

/**
 * Services for Warehouse application Inventory section.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public final class InventoryServices {

    private InventoryServices() { }

    private static final String MODULE = InventoryServices.class.getName();
    private static final String resource = "WarehouseUiLabels";
    private static final String opentapsErrorResource = "OpentapsErrorLabels";

    /**
     * Returns an error if InventoryItem.quantityOnHandTotal is less than zero.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map checkInventoryItemQOHOverZero(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String inventoryItemId = (String) context.get("inventoryItemId");

        GenericValue inventoryItem = null;
        try {

            inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            if (UtilValidate.isEmpty(inventoryItem)) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorInventoryItemNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Double quantityOnHandTotal = inventoryItem.getDouble("quantityOnHandTotal");
        if (UtilValidate.isNotEmpty(quantityOnHandTotal) && quantityOnHandTotal.doubleValue() < 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "WarehouseErrorInventoryItemQOHUnderZero", context, locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates a new "Lot" entity based on given service attributes.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map createLot(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String facilityId = (String) context.get("facilityId");
        WarehouseSecurity warehouseSecurity = new WarehouseSecurity(security, userLogin, facilityId);
        if (!warehouseSecurity.hasFacilityPermission("WRHS_INV_LOT_CREATE")) {
            String error = UtilProperties.getMessage(opentapsErrorResource, "OpentapsError_PermissionDenied", context, locale);
            return ServiceUtil.returnError(error);
        }

        String lotId = null;

        try {
            GenericValue lot = delegator.makeValidValue("Lot", context);

            if (UtilValidate.isEmpty(lot.get("lotId"))) {
                lot.set("lotId", delegator.getNextSeqId("Lot"));
            }

            if (UtilValidate.isEmpty(lot.get("creationDate"))) {
                lot.set("creationDate", UtilDateTime.nowTimestamp());
            }

            lot.create();
            lotId = (String) lot.get("lotId");
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("lotId", lotId);
        return result;
    }

    /**
     * Updates a given "Lot" entity.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map updateLot(DispatchContext dctx, Map context) {
        Delegator delegator  = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String facilityId = (String) context.get("facilityId");
        WarehouseSecurity warehouseSecurity = new WarehouseSecurity(security, userLogin, facilityId);
        if (!warehouseSecurity.hasFacilityPermission("WRHS_INV_LOT_UPDATE")) {
            String error = UtilProperties.getMessage(opentapsErrorResource, "OpentapsError_PermissionDenied", context, locale);
            return ServiceUtil.returnError(error);
        }

        String lotId = (String) context.get("lotId");

        try {
            GenericValue lot = delegator.findByPrimaryKey("Lot", UtilMisc.toMap("lotId", lotId));

            if (UtilValidate.isEmpty(lot)) {
                String error = UtilProperties.getMessage(resource, "WarehouseErrorLotIdNotFound", context, locale);
                return ServiceUtil.returnError(error);
            }

            lot.setNonPKFields(context);
            lot.store();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("lotId", lotId);
        return result;
    }

    /**
     * Issues order item quantity specified to the shipment, then receives inventory for that item and quantity,
     *  creating a new shipment if necessary. Overrides the OFBiz issueOrderItemToShipmentAndReceiveAgainstPO service.
     * If completePurchaseOrder is Y, then this will run the completePurchaseOrder service after receiving any specified
     *  inventory.  Unreserved inventory will be cancelled.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map issueOrderItemToShipmentAndReceiveAgainstPO(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        Map result = ServiceUtil.returnSuccess();

        Map orderItemSeqIds = (Map) context.get("orderItemSeqIds");
        Map quantitiesAccepted = (Map) context.get("quantitiesAccepted");
        Map quantitiesRejected = (Map) context.get("quantitiesRejected");
        Map lotIds = (Map) context.get("lotIds");
        Map productIds = (Map) context.get("productIds");
        Map unitCosts = (Map) context.get("unitCosts");
        Map inventoryItemTypeIds = (Map) context.get("inventoryItemTypeIds");
        String shipmentId = (String) context.get("shipmentId");
        Map rowSubmits = (Map) context.get("_rowSubmit");

        String purchaseOrderId = (String) context.get("purchaseOrderId");
        String facilityId = (String) context.get("facilityId");
        boolean completePurchaseOrder = "Y".equals(context.get("completePurchaseOrder"));

        try {
            // List of cancelled order items
            List<String> cancelleOrderItems = EntityUtil.getFieldListFromEntityList(delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", purchaseOrderId, "statusId", "ITEM_CANCELLED")), "orderItemSeqId", true);

            if (UtilValidate.isEmpty(shipmentId)) {

                // Create a new shipment
                Map createShipmentContext = dctx.getModelService("createShipment").makeValid(context, ModelService.IN_PARAM);
                createShipmentContext.put("primaryOrderId", purchaseOrderId);
                createShipmentContext.put("primaryShipGroupSeqId", context.get("shipGroupSeqId"));
                createShipmentContext.put("shipmentTypeId", "PURCHASE_SHIPMENT");
                createShipmentContext.put("statusId", "PURCH_SHIP_CREATED");
                createShipmentContext.put("destinationFacilityId", facilityId);
                createShipmentContext.put("estimatedArrivalDate", UtilDateTime.nowTimestamp());
                Map createShipmentResult = dispatcher.runSync("createShipment", createShipmentContext);
                if (ServiceUtil.isError(createShipmentResult)) {
                    Debug.logError(ServiceUtil.getErrorMessage(createShipmentResult), MODULE);
                    return createShipmentResult;
                }
                shipmentId = (String) createShipmentResult.get("shipmentId");
                context.put("shipmentId", shipmentId);
            }

            List toReceive = new ArrayList();

            Iterator rit = rowSubmits.keySet().iterator();
            while (rit.hasNext()) {

                String rowNumber = (String) rit.next();

                // Ignore unchecked rows
                if (!"Y".equals(rowSubmits.get(rowNumber))) {
                    continue;
                }

                // clear the reusable data because otherwise we end up doing things like receiving everything into one lot from the first line
                context.remove("lotId");

                String orderItemSeqId = (String) orderItemSeqIds.get(rowNumber);
                if (UtilValidate.isNotEmpty(orderItemSeqId)) {
                    if (cancelleOrderItems.contains(orderItemSeqId)) {
                        continue;
                    }
                    context.put("orderItemSeqId", orderItemSeqId);
                }
                if (UtilValidate.isNotEmpty(lotIds.get(rowNumber))) {
                    context.put("lotId", lotIds.get(rowNumber));
                }
                if (UtilValidate.isNotEmpty(productIds.get(rowNumber))) {
                    context.put("productId", productIds.get(rowNumber));
                }
                if (UtilValidate.isNotEmpty(inventoryItemTypeIds.get(rowNumber))) {
                    context.put("inventoryItemTypeId", inventoryItemTypeIds.get(rowNumber));
                }
                try {
                    if (UtilValidate.isNotEmpty((String) quantitiesAccepted.get(rowNumber))) {
                        context.put("quantityAccepted", UtilCommon.parseLocalizedNumber(locale, (String) quantitiesAccepted.get(rowNumber)));
                    }
                    if (UtilValidate.isNotEmpty((String) quantitiesAccepted.get(rowNumber))) {
                        context.put("quantity", UtilCommon.parseLocalizedNumber(locale, (String) quantitiesAccepted.get(rowNumber)));
                    }
                    if (UtilValidate.isNotEmpty((String) quantitiesRejected.get(rowNumber))) {
                        context.put("quantityRejected", UtilCommon.parseLocalizedNumber(locale, (String) quantitiesRejected.get(rowNumber)));
                    }
                    if (UtilValidate.isNotEmpty((String) unitCosts.get(rowNumber))) {
                        context.put("unitCost", UtilCommon.parseLocalizedNumber(locale, (String) unitCosts.get(rowNumber)));
                    }
                } catch (ParseException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                if (UtilValidate.isEmpty(context.get("quantityAccepted")) || ((BigDecimal) context.get("quantityAccepted")).doubleValue() <= 0) {
                    continue;
                }

                // Call the issueOrderItemToShipment service
                Map issueOrderItemContext = dctx.getModelService("issueOrderItemToShipment").makeValid(context, ModelService.IN_PARAM);
                issueOrderItemContext.put("orderId", purchaseOrderId);
                Map issueOrderItemResult = dispatcher.runSync("issueOrderItemToShipment", issueOrderItemContext);
                if (ServiceUtil.isError(issueOrderItemResult)) {
                    Debug.logError(ServiceUtil.getErrorMessage(issueOrderItemResult), MODULE);
                    return issueOrderItemResult;
                }
                String shipmentItemSeqId = (String) issueOrderItemResult.get("shipmentItemSeqId");
                context.put("shipmentItemSeqId", shipmentItemSeqId);
                String itemIssuanceId = (String) issueOrderItemResult.get("itemIssuanceId");
                context.put("itemIssuanceId", itemIssuanceId);

                toReceive.add(new HashMap(context));
            }

            Iterator trit = toReceive.iterator();
            while (trit.hasNext()) {

                // Call the receiveInventoryProduct service
                Map receiveInvContext = dctx.getModelService("receiveInventoryProduct").makeValid((Map) trit.next(), ModelService.IN_PARAM);
                receiveInvContext.put("orderId", purchaseOrderId);
                receiveInvContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                Map receiveInvResult = dispatcher.runSync("receiveInventoryProduct", receiveInvContext);
                if (ServiceUtil.isError(receiveInvResult)) {
                    Debug.logError(ServiceUtil.getErrorMessage(receiveInvResult), MODULE);
                    return receiveInvResult;
                }
            }

            if (completePurchaseOrder) {
                Map results = dispatcher.runSync("completePurchaseOrder", UtilMisc.toMap("orderId", purchaseOrderId, "userLogin", context.get("userLogin"), "facilityId", facilityId));
                if (ServiceUtil.isError(results)) {
                    return results;
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        result.put("shipmentId", shipmentId);
        return result;
    }

    /**
     * Adjust both ATP and QOH by the same given amount.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map adjustInventoryQuantity(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // ignoring 0 and empty variance quantity
        BigDecimal varianceQty = (BigDecimal) context.get("varianceQty");
        if (varianceQty == null || varianceQty.compareTo(BigDecimal.ZERO) == 0) {
            return ServiceUtil.returnSuccess();
        }

        try {
            Map results = dispatcher.runSync("createPhysicalInventoryAndVariance", UtilMisc.toMap(
                 "inventoryItemId", context.get("inventoryItemId"),
                 "varianceReasonId", context.get("varianceReasonId"),
                 "availableToPromiseVar", varianceQty,
                 "quantityOnHandVar", varianceQty,
                 "userLogin", userLogin));
            if (ServiceUtil.isError(results)) {
                return results;
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }
}
