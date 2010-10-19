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

package org.opentaps.warehouse.picking;

import java.sql.Timestamp;
import java.util.*;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.warehouse.security.WarehouseSecurity;
import org.opentaps.common.util.UtilCommon;
import java.math.BigDecimal;

/**
 * Services for Warehouse application Picking section.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev: 8604 $
 */
public final class PickingServices {

    private PickingServices() { }

    private static final String MODULE = PickingServices.class.getName();
    public static final String warehouseResource = "warehouse";
    public static final String errorResource = "OpentapsErrorLabels";
    public static final String resource = "WarehouseUiLabels";

    // pick list status IDs
    private static final String PICKLIST_STATUS_ID_PICKED = "PICKLIST_PICKED";
    private static final String PICKLIST_STATUS_ID_COMPLETED = "PICKLIST_COMPLETED";

    /**
     * Prints a picklist to a physical printer.
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map printPicklist(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String picklistId = (String) context.get("picklistId");
        String printerName = (String) context.get("printerName");

        String printScreenLocation = UtilProperties.getPropertyValue(warehouseResource, "warehouse.shipping.picklists.printing.screenLocation");
        if (UtilValidate.isEmpty(printScreenLocation)) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_PropertyNotConfigured", UtilMisc.toMap("propertyName", "warehouse.shipping.picklists.printing.screenLocation", "resource", errorResource), locale);
            Debug.logError(errorMessage, MODULE);
            return ServiceUtil.returnError(errorMessage);
        }

        try {

            GenericValue picklist = delegator.findByPrimaryKeyCache("Picklist", UtilMisc.toMap("picklistId", picklistId));
            if (UtilValidate.isEmpty(picklist)) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorPicklistNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            // Assemble the parameters for the PicklistReport FO
            Map parameters = new HashMap();
            parameters.put("picklistId", picklistId);
            parameters.put("facilityId", picklist.getString("facilityId"));
            parameters.put("warehouseSecurity", new WarehouseSecurity(security, userLogin, picklist.getString("facilityId")));

            // Call the sendPrintFromScreen service on the PickListReport screen, which will print the picklist
            // Service is called synchronously so that the service will fail if the picklist fails to print
            Map sendPrintFromScreenContext = UtilMisc.toMap("screenLocation", printScreenLocation, "screenContext", UtilMisc.toMap("parameters", parameters, "userLogin", userLogin), "printerName", printerName, "locale", locale, "userLogin", userLogin);
            Map sendPrintFromScreenResult = dispatcher.runSync("sendPrintFromScreen", sendPrintFromScreenContext);
            if (ServiceUtil.isError(sendPrintFromScreenResult)) {
                return sendPrintFromScreenResult;
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException se) {
            Debug.logError(se, se.getMessage(), MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Closes off a picklist. This will set the picklist status to "PICKLIST_COMPLETED".
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map closePicklist(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String picklistId = (String) context.get("picklistId");

        try {
            GenericValue picklist = delegator.findByPrimaryKey("Picklist", UtilMisc.toMap("picklistId", picklistId));
            if (UtilValidate.isEmpty(picklist)) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorPicklistNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            String statusId = picklist.getString("statusId");
            if (UtilValidate.isEmpty(statusId) || !PICKLIST_STATUS_ID_PICKED.equals(statusId)) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorInvalidPicklistStatus", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            // TODO: check for permission

            // update the picklist status
            Map updateContext = new HashMap();
            updateContext.put("statusId", PICKLIST_STATUS_ID_COMPLETED);

            picklist.setNonPKFields(updateContext);
            picklist.store();

            // register with the picklist status history
            Map createContext = new HashMap();
            createContext.put("picklistId", picklistId);
            createContext.put("changeDate", UtilDateTime.nowTimestamp());
            createContext.put("changeUserLoginId", userLogin.getString("userLoginId"));
            createContext.put("statusId", PICKLIST_STATUS_ID_PICKED);
            createContext.put("statusIdTo", PICKLIST_STATUS_ID_COMPLETED);

            GenericValue picklistStatusHistory = delegator.makeValue("PicklistStatusHistory");
            picklistStatusHistory.setPKFields(createContext);
            picklistStatusHistory.setNonPKFields(createContext);
            picklistStatusHistory.create();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        String successMessage = UtilProperties.getMessage(resource, "WarehousePicklistIsClosed", context, locale);
        return ServiceUtil.returnSuccess(successMessage);
    }

    /**
     * Rewrite of the ofbiz simple method.
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> findOrdersToPickMove(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // service input parameters
        String facilityId = (String) context.get("facilityId");
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
        String isRushOrder = (String) context.get("isRushOrder");
        Long maxNumberOfOrders = (Long) context.get("maxNumberOfOrders");
        List<GenericValue> orderHeaderList = (List<GenericValue>) context.get("orderHeaderList");

        // control grouping of the OUT map pickMoveInfoList
        // TODO: current implementation always group by shipment method (see pickMoveByShipmentMethodInfoList)
        String groupByShippingMethod = (String) context.get("groupByShippingMethod");
        String groupByNoOfOrderItems = (String) context.get("groupByNoOfOrderItems");
        String groupByWarehouseArea = (String) context.get("groupByWarehouseArea");

        // check permission
        WarehouseSecurity warehouseSecurity = new WarehouseSecurity(security, userLogin, facilityId);
        if (!warehouseSecurity.hasFacilityPermission("FACILITY_VIEW")) {
            String error = UtilProperties.getMessage(errorResource, "OpentapsError_PermissionDenied", context, locale);
            return ServiceUtil.returnError(error);
        }

        Timestamp now = UtilDateTime.nowTimestamp();
        boolean hasMaxNumberofOrders = maxNumberOfOrders != null;
        Long numberSoFar = new Long(0);

        // out params
        List pickMoveByShipmentMethodInfoList = new ArrayList();
        Map pickMoveByShipmentMethodInfoMap = new HashMap();
        Map rushOrderInfo = new HashMap();

        // populate the order list if it was not given
        if (UtilValidate.isEmpty(orderHeaderList)) {
            Debug.logInfo("No order header list found in parameters; finding orders to pick.", MODULE);
            Map cond = UtilMisc.toMap("orderTypeId", "SALES_ORDER", "statusId", "ORDER_APPROVED");
            if (UtilValidate.isNotEmpty(isRushOrder)) {
                cond.put("isRushOrder", isRushOrder);
            }
            // oldest first
            orderHeaderList = delegator.findByAnd("OrderHeader", cond, UtilMisc.toList("+orderDate"));
        } else {
            Debug.logInfo("Found orderHeaderList in parameters; using: " + orderHeaderList, MODULE);
        }

        for (GenericValue oh : orderHeaderList) {
            String orderId = oh.getString("orderId");
            Debug.logInfo("Checking order #" + orderId + " to add to picklist", MODULE);

            // get all ship groups, and iterate over them for each order
            // Skip OISG_CANCELLED, OISG_COMPLETED and OISG_PACKED
            // Also skip those that have _NA_ as their shipping address (contactMechId)
            EntityCondition conditions = EntityCondition.makeCondition(
                   EntityCondition.makeCondition(EntityOperator.OR,
                       EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, UtilMisc.toList("OISG_CANCELLED", "OISG_PACKED", "OISG_COMPLETED")),
                       EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null)),
                   EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                   EntityCondition.makeCondition("contactMechId", EntityOperator.NOT_EQUAL, "_NA_"));
            List<GenericValue> orderItemShipGroupList = delegator.findByCondition("OrderItemShipGroup", conditions, null, UtilMisc.toList("shipGroupSeqId"));

            if (UtilValidate.isEmpty(orderItemShipGroupList)) {
                Debug.logWarning("No OISG found for that order.", MODULE);
            }

            for (GenericValue oisg : orderItemShipGroupList) {
                Debug.logInfo("Checking OISG: " + oisg.get("shipGroupSeqId"), MODULE);
                // get the order items and the order item inventory res entries

                // skip that entity if shipmentMethodTypeId was given and is different
                String oisgShipmentMethodTypeId = oisg.getString("shipmentMethodTypeId");
                if (UtilValidate.isNotEmpty(shipmentMethodTypeId) && !shipmentMethodTypeId.equals(oisgShipmentMethodTypeId)) {
                    Debug.logInfo("Skipping oisg beause shipmentMethodTypeId (" + oisgShipmentMethodTypeId + ") different than given: " + shipmentMethodTypeId, MODULE);
                    continue;
                }
                // skip if shipAfterDate is given and before now
                Timestamp shipAfterDate = (Timestamp) oisg.get("shipAfterDate");
                if (shipAfterDate != null && shipAfterDate.before(now)) {
                    Debug.logInfo("Skipping oisg beause shipAfterDate (" + shipAfterDate + ") greater than now: " + now, MODULE);
                    continue;
                }
                String shipGroupSeqId = oisg.getString("shipGroupSeqId");
                // get related OrderItemShipGrpInvRes and OrderItemAndShipGroupAssoc
                List<GenericValue> orderItemShipGrpInvResList = delegator.findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
                List<GenericValue> orderItemAndShipGroupAssocList = delegator.findByAnd("OrderItemAndShipGroupAssoc", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId), UtilMisc.toList("+orderItemSeqId"));

                // only add to picklist if inventory is not available (quantityNotAvailable on OISGIR greater than 0) when maySplit is N (wait until all available to ship)
                boolean pickThisOrder = true;
                boolean pickThisShipGroup = true;
                boolean allPickStarted = true;
                boolean needsStockMove = false;
                boolean hasStockToPick = false;
                List<Map> orderItemShipGrpInvResInfoList = new ArrayList<Map>();

                for (GenericValue oisgir : orderItemShipGrpInvResList) {
                    // early check on the flags from the previous iteration
                    if (!pickThisOrder || !pickThisShipGroup) {
                        break;
                    }

                    // check related order item status
                    GenericValue orderItem = oisgir.getRelatedOne("OrderItem");
                    if (!"ITEM_APPROVED".equals(orderItem.getString("statusId"))) {
                        pickThisOrder = false;
                        Debug.logInfo("OISG: " + shipGroupSeqId + " has unapproved items, skipping", MODULE);
                        break;
                    }

                    if (pickThisOrder && pickThisShipGroup) {
                        GenericValue inventoryItem = oisgir.getRelatedOne("InventoryItem");
                        /* Look for other picklists which might include this order item ship group inventory reservation.  If it is on another picklist, then
                           we should not include it again.  We screen out picklists which are either cancelled or already picked or packed, so that we can re-pick items if
                           (1) the previous picklist was cancelled, or
                           (2) the previous picklist was picked or packed, and there is still an OrderItemShipGrpInvRes, which means that some of the order item must not
                           have shipped yet.  (OrderItemShipGrpInvRes is removed when an order item has been fully shipped.
                           We are using entity-condition instead of get-related because we want to exclude some picklists by status
                        */
                        List cond = UtilMisc.toList(
                                                    EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                                                    EntityCondition.makeCondition("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId),
                                                    EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, oisgir.getString("orderItemSeqId")),
                                                    EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, oisgir.getString("inventoryItemId")),
                                                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_CANCELLED"),
                                                    EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "PICKITEM_CANCELLED")
                                                    );
                        List<GenericValue> picklistItemList = delegator.findByAnd("PicklistAndBinAndItem", cond);

                        Debug.logInfo("Pick list ITEMS - " + picklistItemList, MODULE);

                        // if all picklistItemList are not empty, don't include order as a pick candidate; keep a flag to see
                        if (UtilValidate.isEmpty(picklistItemList)) {
                            Debug.logInfo("The pick list item list is empty!", MODULE);
                            // note that this is separate because we can't really use it as a break condition, must check all of them before any useful information is to be had
                            allPickStarted = false;

                            // check all OISGIRs and if quantityNotAvailable is not empty and > 0 for any, don't pick order
                            // and make sure the inventoryItem is in the specified facility
                            BigDecimal quantityNotAvailable = oisgir.getBigDecimal("quantityNotAvailable");
                            BigDecimal quantity = oisgir.getBigDecimal("quantity");
                            boolean hasQtyNotAvailable = (quantityNotAvailable != null && quantityNotAvailable.signum() > 0);
                            boolean maySplit = "Y".equals(oisg.getString("maySplit"));
                            if (!facilityId.equals(inventoryItem.getString("facilityId"))) {
                                Debug.logInfo("Item reservation reservation (" + printOisgir(oisgir) + ") facility id does not match, ignoring this item.", MODULE);
                                continue;
                            } else if (!maySplit && hasQtyNotAvailable) {
                                pickThisShipGroup = false;
                                Debug.logInfo("Item reservation (" + printOisgir(oisgir) + ") does not have stock and the order may not split, not picking this ship group.", MODULE);
                                break;
                            } else {
                                Debug.logInfo("Found item to pick: " + printOisgir(oisgir), MODULE);
                                // see if there is stock to pick.  Items without stock (back ordered items) are not added to picklists.
                                if (!hasQtyNotAvailable || (quantity.compareTo(quantityNotAvailable) > 0 && maySplit)) {
                                    Debug.logInfo("Item has stock; flagging order (" + orderId + ") as OK", MODULE);
                                    hasStockToPick = true;
                                } else {
                                    Debug.logInfo("Item reservation (" + printOisgir(oisgir) + ") does not have stock and will not be flagged as hasStockToPick", MODULE);
                                }
                            }

                            // check InventoryItem->FacilityLocation (if exists), if it is of type FLT_BULK set needs stock move to true
                            GenericValue facilityLocation = inventoryItem.getRelatedOne("FacilityLocation");
                            if (facilityLocation != null && "FLT_BULK".equals(facilityLocation.getString("locationTypeEnumId"))) {
                                needsStockMove = true;
                            }

                            // make the orderItemShipGrpInvResInfo and add it to the orderItemShipGrpInvResInfoList
                            orderItemShipGrpInvResInfoList.add(UtilMisc.toMap("orderItemShipGrpInvRes", oisgir, "inventoryItem", inventoryItem, "facilityLocation", facilityLocation));
                        }
                    } // if pickThisOrder && pickThisShipGroup
                } // looping the orderItemShipGrpInvResList

                if (!pickThisShipGroup) {
                    continue;
                }

                // another check to see if we should pick this order
                if (!hasStockToPick) {
                    pickThisShipGroup = false;
                    Debug.logInfo("OISG: " + shipGroupSeqId + " has no stock to pick, skipping", MODULE);
                    continue;
                }

                // check our iteration counter
                if (hasMaxNumberofOrders && numberSoFar >= maxNumberOfOrders) {
                    Debug.logInfo("We have passed the max number of orders!", MODULE);
                    pickThisOrder = false;
                    break;
                } else {
                    Debug.logInfo("We have not passed the max number of orders yet...", MODULE);
                }

                if (pickThisOrder && !allPickStarted) {

                    // make the info map for this orderHeader
                    Debug.logInfo("++ building orderHeaderInfo map with\n orderItemAndShipGroupAssocList: " + orderItemAndShipGroupAssocList, MODULE);
                    Map orderHeaderInfo = UtilMisc.toMap("orderHeader", oh, "orderItemShipGroup", oisg, "orderItemAndShipGroupAssocList", orderItemAndShipGroupAssocList, "orderItemShipGrpInvResList", orderItemShipGrpInvResList, "orderItemShipGrpInvResInfoList", orderItemShipGrpInvResInfoList);

                    // pick now, or needs stock move first?
                    // put in pick or move lists for the given shipmentMethodTypeId
                    Map info = (Map) pickMoveByShipmentMethodInfoMap.get(oisgShipmentMethodTypeId);
                    if (info == null) {
                        info = new HashMap();
                        pickMoveByShipmentMethodInfoMap.put(oisgShipmentMethodTypeId, info);
                    }

                    if (UtilValidate.isEmpty(info)) {
                        GenericValue smt = oisg.getRelatedOne("ShipmentMethodType");
                        info.put("shipmentMethodType", smt);
                    }

                    List orderNeedsStockMoveInfoList = (List) info.get("orderNeedsStockMoveInfoList");
                    if (orderNeedsStockMoveInfoList == null) {
                        orderNeedsStockMoveInfoList = new ArrayList();
                        info.put("orderNeedsStockMoveInfoList", orderNeedsStockMoveInfoList);
                    }
                    List orderReadyToPickInfoList = (List) info.get("orderReadyToPickInfoList");
                    if (orderReadyToPickInfoList == null) {
                        orderReadyToPickInfoList = new ArrayList();
                        info.put("orderReadyToPickInfoList", orderReadyToPickInfoList);
                    }

                    Debug.logInfo("+ is recorded for shipmentMethodTypeId [" + oisgShipmentMethodTypeId + "]", MODULE);

                    if (needsStockMove) {
                        orderNeedsStockMoveInfoList.add(orderHeaderInfo);
                        if ("Y".equals(oh.getString("isRushOrder"))) {
                            rushOrderInfo.put("orderNeedsStockMoveInfoList", orderHeaderInfo);
                        }
                    } else {
                        orderReadyToPickInfoList.add(orderHeaderInfo);
                        if ("Y".equals(oh.getString("isRushOrder"))) {
                            rushOrderInfo.put("orderReadyToPickInfoList", orderHeaderInfo);
                        }
                    }

                    // increment iteration counter
                    numberSoFar++;
                    Debug.logInfo("Added order #" + orderId + " to pick list [" + numberSoFar + " of " + maxNumberOfOrders + "] - pickThisOrder=" + pickThisOrder + " / allPickStarted=" + allPickStarted, MODULE);

                } else {
                    Debug.logInfo("Order #" + orderId + " was not added to pick list [" + numberSoFar + " of " + maxNumberOfOrders + "] - pickThisOrder=" + pickThisOrder + " / allPickStarted=" + allPickStarted, MODULE);
                }

            } // looping the orderItemShipGroupList

            // check our iteration counter
            if (hasMaxNumberofOrders && numberSoFar >= maxNumberOfOrders) {
                Debug.logInfo("We have really passed the max number of orders!", MODULE);
                break;
            }

        }

        // find all ShipmentMethodType in order by sequenceNum, for each one get the value from
        // the pickMoveByShipmentMethodInfoMap and add it to the pickMoveByShipmentMethodInfoList
        List<GenericValue> shipmentMethodTypeList = delegator.findAll("ShipmentMethodType", UtilMisc.toList("+sequenceNum"));
        for (GenericValue smt : shipmentMethodTypeList) {
            String smtId = smt.getString("shipmentMethodTypeId");
            Map info = (Map) pickMoveByShipmentMethodInfoMap.get(smtId);
            if (UtilValidate.isNotEmpty(info)) {
                pickMoveByShipmentMethodInfoList.add(info);
            }
        }

        Map results = ServiceUtil.returnSuccess();
        results.put("pickMoveInfoList", pickMoveByShipmentMethodInfoList);
        // TODO: deprecated, use pickMoveInfoList and group according to user inputs
        results.put("pickMoveByShipmentMethodInfoList", pickMoveByShipmentMethodInfoList);
        results.put("rushOrderInfo", rushOrderInfo);
        results.put("nReturnedOrders", numberSoFar);
        return results;
    }

    private static String printOisgir(GenericValue oisgir) {
        return oisgir.get("orderId") + "/" + oisgir.get("orderItemSeqId") + " x " + oisgir.get("quantity") + " in group [" + oisgir.get("shipGroupSeqId") + "] on inventory " + oisgir.get("inventoryItemId");
    }

}
