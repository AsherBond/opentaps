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

/* This file is partially based on an OFBIZ file and has been modified by Open Source Strategies, Inc. */

package org.opentaps.purchasing.mrp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.manufacturing.mrp.MrpServices;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.domain.manufacturing.OpentapsProductionRun;
import org.opentaps.domain.manufacturing.bom.BomNodeInterface;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.common.util.UtilMessage;

/**
 * Opentaps MRP related services.
 *
 * Some of the enhancements compared to the original ofbiz MRP:
 * <ul>
 *  <li>No inventory events created before now
 * </ul>
 */
public final class OpentapsMrpServices {

    private OpentapsMrpServices() { }

    private static final String MODULE = OpentapsMrpServices.class.getName();

    // some rounding properties for quantities
    private static int decimals = 2;
    private static RoundingMode defaultRoundingMode = RoundingMode.HALF_UP;

    /**
     * Service <code>opentaps.initInventoryEventPlanned</code>, initializes the table <code>InventoryEventPlanned</code> for MRP for a facility.
     *
     * Note on some parameters:
     *  <code>reInitialize</code> will cause existing values to be removed.
     *  <code>now</code> is used to synchronize current time for all MRP
     *  <code>defaultYearsOffset</code> is the number of years in the future sales order shipments are assumed to be required, if no explicit shipping date is supplied
     *  <code>receiptEventBufferMilliseconds</code> is the number of milliseconds inventory events which increase inventory are shifted to the past
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map initInventoryEventPlanned(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // allow calling service to set a now to synchronize multiple runs, or default to current time if none is supplied
        Timestamp now = (Timestamp) context.get("now");
        BigDecimal receiptEventBufferMilliseconds = (BigDecimal) context.get("receiptEventBufferMilliseconds");
        if (now == null) {
            now = UtilDateTime.nowTimestamp();
        }
        Integer defaultYearsOffset = (Integer) context.get("defaultYearsOffset");

        // whether to remove all InventoryEventPlanned variables
        Boolean reInitialize = (Boolean) context.get("reInitialize");

        String facilityId = (String) context.get("facilityId");
        String supplierPartyId = (String) context.get("supplierPartyId");
        String productStoreId = (String) context.get("productStoreId");
        String productStoreGroupId = (String) context.get("productStoreGroupId");
        String mrpTargetProductId = (String) context.get("productId");
        BigDecimal percentageOfSalesForecast = (BigDecimal) context.get("percentageOfSalesForecast");

        try {

            // get the productIds if we are in a product or supplier specific MRP run
            List mrpRunProductIds = getMrpRunProductIds(mrpTargetProductId, supplierPartyId, delegator);

            // remove zombie records from old ofbiz mrp runs
            removeOldMrpInventoryEventRecords(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, null), delegator);

            //Erases the old table for the moment unless reinitialize is turned off
            if (!(Boolean.FALSE.equals(reInitialize))) {
                Debug.logInfo("Reinitializing: removing all MrpInventoryEvent", MODULE);
                if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
                    // AG22012008 - if this is a productId specific MRP run then filter by target product Id
                    removeOldMrpInventoryEventRecords(EntityCondition.makeCondition(EntityOperator.AND,
                            EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                            EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds)), delegator);
                } else {
                    removeOldMrpInventoryEventRecords(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId), delegator);
                }
            } else {
                Debug.logInfo("Not reinitializing: only removing proposed MrpInventoryEvent", MODULE);
                if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
                    // AG22012008 - if this is a productId specific MRP run then filter by target product Id
                    removeOldMrpInventoryEventRecords(EntityCondition.makeCondition(EntityOperator.AND,
                            EntityCondition.makeCondition("inventoryEventPlanTypeId", EntityOperator.IN, Arrays.asList("INITIAL_QOH", "PEND_MANUF_O_RECP", "PROP_MANUF_O_RECP", "PROP_PUR_O_RECP", "PROP_INV_XFER_IN", "PROP_INV_XFER_OUT", "MRP_REQUIREMENT")),
                            EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds)), delegator);
                } else {
                    removeOldMrpInventoryEventRecords(EntityCondition.makeCondition("inventoryEventPlanTypeId", EntityOperator.IN, Arrays.asList("INITIAL_QOH", "PEND_MANUF_O_RECP", "PROP_MANUF_O_RECP", "PROP_PUR_O_RECP", "PROP_INV_XFER_IN", "PROP_INV_XFER_OUT", "MRP_REQUIREMENT")), delegator);
                }
            }

            // Proposed requirements and requirement commitments should always be deleted
            if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
                // AG22012008 - if this is a productId specific MRP run then filter by target product Id
                removeOldRequirementRecords(EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "PRODUCT_REQUIREMENT"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_PROPOSED"),
                        EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds)), true, delegator);
                removeOldRequirementRecords(EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "INTERNAL_REQUIREMENT"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_PROPOSED"),
                        EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds)), false, delegator);
                removeOldRequirementRecords(EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "PENDING_INTERNAL_REQ"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_PROPOSED"),
                        EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds)), false, delegator);
                removeOldRequirementRecords(EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "TRANSFER_REQUIREMENT"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_PROPOSED"),
                        EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds)), false, delegator);
            } else {
                removeOldRequirementRecords(EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "PRODUCT_REQUIREMENT"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_PROPOSED")), true, delegator);
                removeOldRequirementRecords(EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "PENDING_INTERNAL_REQ"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_PROPOSED")), false, delegator);
                removeOldRequirementRecords(EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "INTERNAL_REQUIREMENT"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_PROPOSED")), false, delegator);
                removeOldRequirementRecords(EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "TRANSFER_REQUIREMENT"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_PROPOSED")), false, delegator);
            }

            /*
            Now initialize inventory events for MRP, which is done by inserting all sales order, purchase order, production run inventory needed and produced,
            plus any other existing requirements.

            Note that the date of the inventory event is the later of now or the date of the order/production run/requirement, so that events are not created
            before the current time.  The "isLate" flag for createOrUpdateInventoryEventPlanned is set to true if the order/production/requirement date is earlier
            than the current timestamp.
             */
            Map parameters = null;
            Map mrpInventoryEventDetailInput = null;
            List<GenericValue> resultList;
            // ----------------------------------------
            // Loads all the approved sales order items and purchase order items
            // ----------------------------------------
            // This is the default required date for sales orders without dates specified
            Timestamp notAssignedDate = null;
            Calendar calendar = UtilDate.toCalendar(now, timeZone, locale);
            if (UtilValidate.isEmpty(defaultYearsOffset)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            } else {
                calendar.add(Calendar.YEAR, defaultYearsOffset.intValue());
            }
            notAssignedDate = new Timestamp(calendar.getTimeInMillis());

            resultList = getMrpOrderInfoForSalesOrders(facilityId, mrpRunProductIds, productStoreId, productStoreGroupId, delegator);
            for (GenericValue genericResult : resultList) {
                String productId =  genericResult.getString("productId");
                // usually we want to use the reserved quantity, because facilityId is only available on the InventoryItem record, so only OISGIR tells us how many of the II is reserved
                // if it's not available then we'll try OrderItem quantities but that's dangerous because the item could have been partially shipped already
                BigDecimal eventQuantityTmp = BigDecimal.ZERO;
                if (genericResult.get("quantityReserved") == null) {
                    Debug.logWarning("No quantity reserved found for order [" + genericResult.get("orderId") + "] item [" + genericResult.get("orderItemSeqId") + "].  Will be using order quantity of [" + genericResult.get("quantity") + "] and cancel quantity [" + genericResult.get("cancelQuantity") + "]", MODULE);
                    eventQuantityTmp = getNetOrderedQuantity(genericResult).negate();
                } else {
                    eventQuantityTmp = genericResult.getBigDecimal("quantityReserved").negate();
                }
                if (eventQuantityTmp.signum() == 0) {
                    continue;
                }
                // This is the order in which order dates are considered.  Item dates override ship group dates:
                //   OrderItem.shipBeforeDate
                //   OrderItem.shipAfterDate
                //   OrderItem.estimatedDeliveryDate
                //   OrderItemShipGroup.shipByDate
                //   OrderItemShipGroup.shipAfterDate
                Timestamp requiredByDate = genericResult.getTimestamp("itemShipBeforeDate");
                if (UtilValidate.isEmpty(requiredByDate)) {
                    requiredByDate = genericResult.getTimestamp("itemShipAfterDate");
                    if (UtilValidate.isEmpty(requiredByDate)) {
                        requiredByDate = genericResult.getTimestamp("itemEstimatedDeliveryDate");
                        if (UtilValidate.isEmpty(requiredByDate)) {
                            requiredByDate = genericResult.getTimestamp("shipByDate");
                            if (UtilValidate.isEmpty(requiredByDate)) {
                                requiredByDate = genericResult.getTimestamp("shipAfterDate");
                                if (requiredByDate == null) {
                                    Debug.logWarning("No date found for " + genericResult + " so will be using not assigned date of [" + notAssignedDate + "]", MODULE);
                                    requiredByDate = notAssignedDate;
                                }
                            }
                        }
                    }
                }
                parameters = UtilMisc.toMap("productId", productId, "eventDate", UtilCommon.laterOf(requiredByDate, now), "inventoryEventPlanTypeId", "SALES_ORDER_SHIP", "facilityId", facilityId);
                mrpInventoryEventDetailInput = UtilMisc.toMap("orderId", genericResult.getString("orderId"), "orderItemSeqId", genericResult.getString("orderItemSeqId"));
                MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters, eventQuantityTmp, null, genericResult.getString("orderId") + "-" + genericResult.getString("orderItemSeqId"), requiredByDate.before(now), mrpInventoryEventDetailInput, delegator);
            }

            // ----------------------------------------
            // Loads all the approved requirements
            // ----------------------------------------
            initInventoryEventPlanForApprovedRequirements(facilityId, mrpRunProductIds, now, receiptEventBufferMilliseconds, userLogin, delegator, dispatcher);

            // -------------------------------------------------------
            // Loads all the created and approved purchase order items
            // -------------------------------------------------------
            String orderId = null;
            GenericValue orderDeliverySchedule = null;
            List searchConditions = UtilMisc.toList(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "PURCHASE_ORDER"),
                    EntityCondition.makeCondition("itemStatusId", EntityOperator.IN, UtilMisc.toList("ITEM_CREATED", "ITEM_APPROVED")),
                    EntityCondition.makeCondition("shipGroupContactMechId", EntityOperator.IN, UtilCommon.getFacilityContactMechIds(facilityId, delegator)));

            // AG22012008 - if this is a productId specific MRP run then filter by target product Id
            if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
                searchConditions.add(EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds));
            }
            List fieldsToSelect = UtilMisc.toList("orderId", "orderItemSeqId", "productId", "quantity", "cancelQuantity", "itemEstimatedDeliveryDate");
            resultList = delegator.findByCondition("MrpOrderInfo", EntityCondition.makeCondition(searchConditions, EntityOperator.AND), fieldsToSelect, UtilMisc.toList("orderDate")); // order list of PO's by OrderDate
            for (GenericValue genericResult : resultList) {
                String newOrderId =  genericResult.getString("orderId");
                if (!newOrderId.equals(orderId)) {
                    orderDeliverySchedule = null;
                    orderId = newOrderId;
                    orderDeliverySchedule = delegator.findByPrimaryKey("OrderDeliverySchedule", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "_NA_"));
                }
                String productId =  genericResult.getString("productId");
                // this will net out received quantities with ItemIssuances
                BigDecimal eventQuantityTmp = getNetPurchaseOrderItemQuantity(genericResult);
                GenericValue orderItemDeliverySchedule = null;
                orderItemDeliverySchedule = delegator.findByPrimaryKey("OrderDeliverySchedule", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", genericResult.getString("orderItemSeqId")));
                Timestamp estimatedShipDate = null;
                if (orderItemDeliverySchedule != null && orderItemDeliverySchedule.get("estimatedReadyDate") != null) {
                    estimatedShipDate = orderItemDeliverySchedule.getTimestamp("estimatedReadyDate");
                } else if (orderDeliverySchedule != null && orderDeliverySchedule.get("estimatedReadyDate") != null) {
                    estimatedShipDate = orderDeliverySchedule.getTimestamp("estimatedReadyDate");
                } else {
                    estimatedShipDate = genericResult.getTimestamp("itemEstimatedDeliveryDate");
                }
                if (estimatedShipDate == null) {
                    Debug.logWarning("No date found for " + genericResult + " so will be using current date and time", MODULE);
                    estimatedShipDate = now;
                }

                parameters = UtilMisc.toMap("productId", productId, "eventDate", UtilCommon.beforeMillisecs(UtilCommon.laterOf(estimatedShipDate, now), receiptEventBufferMilliseconds),
                        "inventoryEventPlanTypeId", "PUR_ORDER_RECP",  "facilityId", facilityId);
                MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters, eventQuantityTmp, null, genericResult.getString("orderId") + "-" + genericResult.getString("orderItemSeqId"), estimatedShipDate.before(now), null, delegator);

                // validate MrpInventoryEvent and log errors to be shown in findInventoryEventPlan.ftl screen
                // check if there is a ProductFacility entry for this MrpInventoryEvent. If there is none this event will not be processed by the MRP algorithm
                GenericValue productFacility = delegator.findByPrimaryKey("ProductFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
                if (UtilValidate.isEmpty(productFacility)) {
                    Map errorParameters = UtilMisc.toMap("productId", productId, "eventDate", UtilCommon.beforeMillisecs(UtilCommon.laterOf(estimatedShipDate, now), receiptEventBufferMilliseconds),
                            "inventoryEventPlanTypeId", "ERROR",  "facilityId", facilityId);
                    MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(errorParameters, null, null, UtilMessage.expandLabel("PurchError_CannotFindProductFacilityForMrpInventoryEvent", locale, UtilMisc.toMap("inventoryEventPlanTypeId", "PUR_ORDER_RECP")), false, null, delegator);
                }
            }

            // ----------------------------------------
            // PRODUCTION Run: components
            // ----------------------------------------
            // active production runs which have inventory requirement in WorkEffortGoodStandard of PRUNT_PROD_NEEDED (product needed, not template)
            List validProductionRunStatuses = Arrays.asList("PRUN_CREATED", "PRUN_RUNNING", "PRUN_STARTED", "PRUN_SCHEDULED", "PRUN_DOC_PRINTED", "PRUN_OUTSRCD", "PRUN_OUTSRCD_PEND");
            searchConditions = UtilMisc.toList(
                    EntityCondition.makeCondition("currentStatusId", EntityOperator.IN, validProductionRunStatuses),
                    EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUNT_PROD_NEEDED"),
                    EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                    EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "WEGS_CREATED"));

            // AG22012008 - if this is a productId specific MRP run then filter by target product Id
            if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
                searchConditions.add(EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds));
            }

            resultList = delegator.findByAnd("WorkEffortAndGoods", searchConditions);

            for (GenericValue genericResult : resultList) {
                String productId =  genericResult.getString("productId");
                BigDecimal eventQuantityTmp = genericResult.getBigDecimal("estimatedQuantity").negate();
                Timestamp estimatedShipDate = genericResult.getTimestamp("estimatedStartDate");
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }

                parameters = UtilMisc.toMap("productId", productId, "eventDate", UtilCommon.laterOf(estimatedShipDate, now), "inventoryEventPlanTypeId", "MANUF_ORDER_REQ", "facilityId", genericResult.getString("facilityId"));
                String eventName = (UtilValidate.isEmpty(genericResult.getString("workEffortParentId")) ? genericResult.getString("workEffortId") : genericResult.getString("workEffortParentId") + "-" + genericResult.getString("workEffortId") + " (" + UtilDateTime.timeStampToString(estimatedShipDate, UtilDateTime.getDateFormat(locale), timeZone, locale) + ")");
                MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters, eventQuantityTmp, null, eventName, estimatedShipDate.before(now), null, delegator);
            }

            // ----------------------------------------
            // PRODUCTION Run: product produced
            // ----------------------------------------
            validProductionRunStatuses = Arrays.asList("PRUN_COMPLETED", "PRUN_CREATED", "PRUN_RUNNING", "PRUN_STARTED", "PRUN_SCHEDULED", "PRUN_DOC_PRINTED", "PRUN_OUTSRCD", "PRUN_OUTSRCD_PEND");
            List productionRunConditions = UtilMisc.toList(EntityCondition.makeCondition("currentStatusId", EntityOperator.IN, validProductionRunStatuses),
                    EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUN_PROD_DELIV"),
                    EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                    EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "WEGS_CREATED"));

            // AG22012008 - if this is a productId specific MRP run then filter by target product Id
            if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
                productionRunConditions.add(EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds));
            }
            resultList = delegator.findByCondition("WorkEffortAndGoods", EntityCondition.makeCondition(productionRunConditions, EntityOperator.AND), UtilMisc.toList("workEffortId", "productId", "facilityId", "estimatedCompletionDate"), UtilMisc.toList("workEffortId"));

            for (GenericValue genericResult : resultList) {
                Debug.logInfo("initInventoryEventPlanForApprovedRequirements: Found WEGS pending production: " + genericResult, MODULE);
                OpentapsProductionRun prun = new OpentapsProductionRun(genericResult.getString("workEffortId"), dispatcher);
                String prunProductId = genericResult.getString("productId");
                BigDecimal qtyToProduce = prun.getQuantityPlannedToProduce(prunProductId);
                if (qtyToProduce == null) {
                    qtyToProduce = BigDecimal.ZERO;
                }
                BigDecimal qtyProduced = prun.getQuantityProduced(prunProductId);
                if (qtyProduced == null) {
                    qtyProduced = BigDecimal.ZERO;
                }
                Debug.logInfo("initInventoryEventPlanForApprovedRequirements: qtyToProduce: " + qtyToProduce + " qtyProduced: " + qtyProduced, MODULE);
                if (qtyProduced.compareTo(qtyToProduce) >= 0) {
                    continue;
                }
                BigDecimal qtyDiff = qtyToProduce.subtract(qtyProduced);
                BigDecimal eventQuantityTmp = qtyDiff;
                Timestamp estimatedShipDate = genericResult.getTimestamp("estimatedCompletionDate");
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }

                parameters = UtilMisc.toMap("productId", prunProductId, "eventDate", UtilCommon.beforeMillisecs(UtilCommon.laterOf(estimatedShipDate, now), receiptEventBufferMilliseconds),
                        "inventoryEventPlanTypeId", "MANUF_ORDER_RECP", "facilityId", genericResult.getString("facilityId"));
                MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters, eventQuantityTmp, null, genericResult.getString("workEffortId") + " (" + UtilDateTime.timeStampToString(estimatedShipDate, UtilDateTime.getDateFormat(locale), timeZone, locale) + ")", estimatedShipDate.before(now), null, delegator);

            }

            // inbound inventory transfers
            List inboundInventoryTransfersConditions = UtilMisc.toList(EntityCondition.makeCondition("transferStatusId", EntityOperator.IN, UtilMisc.toList("IXF_REQUESTED", "IXF_SCHEDULED", "IXF_EN_ROUTE")),
                    EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityId),
                    EntityCondition.makeCondition("sendDate", EntityOperator.NOT_EQUAL, null));

            // AG22012008 - if this is a productId specific MRP run then filter by target product Id
            if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
                inboundInventoryTransfersConditions.add(EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds));
            }

            List<GenericValue> tansfersAndItems = delegator.findByAnd("InventoryTransferAndItem",  inboundInventoryTransfersConditions);
            for (GenericValue transferItem : tansfersAndItems) {
                Timestamp sendDate = transferItem.getTimestamp("sendDate");
                parameters = UtilMisc.toMap("productId", transferItem.getString("productId"), "eventDate", UtilCommon.beforeMillisecs(UtilCommon.laterOf(sendDate, now), receiptEventBufferMilliseconds),
                        "inventoryEventPlanTypeId", "INVENTORY_XFER_IN", "facilityId", facilityId);
                MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters, getInventoryTransferQuantity(transferItem), null, transferItem.getString("inventoryTransferId") + ": " + UtilDateTime.timeStampToString(transferItem.getTimestamp("sendDate"), UtilDateTime.getDateFormat(locale), timeZone, locale), transferItem.getTimestamp("sendDate").before(now), null, delegator);
            }


            // outbound inventory transfers
            List outboundInventoryTransfersConditions = UtilMisc.toList(
                    EntityCondition.makeCondition("transferStatusId", EntityOperator.IN, UtilMisc.toList("IXF_REQUESTED", "IXF_SCHEDULED", "IXF_EN_ROUTE")),
                    EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                    EntityCondition.makeCondition("sendDate", EntityOperator.NOT_EQUAL, null));

            // AG22012008 - if this is a productId specific MRP run then filter by target product Id
            if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
                outboundInventoryTransfersConditions.add(EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds));
            }

            tansfersAndItems = delegator.findByAnd("InventoryTransferAndItem", outboundInventoryTransfersConditions);
            for (GenericValue transferItem : tansfersAndItems) {
                Timestamp sendDate = transferItem.getTimestamp("sendDate");
                parameters = UtilMisc.toMap("productId", transferItem.getString("productId"), "eventDate", UtilCommon.laterOf(sendDate, now),
                        "inventoryEventPlanTypeId", "INVENTORY_XFER_OUT", "facilityId", facilityId);
                MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters, getInventoryTransferQuantity(transferItem).negate(), null, transferItem.getString("inventoryTransferId") + ": " + UtilDateTime.timeStampToString(transferItem.getTimestamp("sendDate"), UtilDateTime.getDateFormat(locale), timeZone, locale), transferItem.getTimestamp("sendDate").before(now), null, delegator);
            }


            // Use a percentage of salesforecast from the SalesForecastItem entity.
            if (percentageOfSalesForecast != null) {
                if (percentageOfSalesForecast.signum() != 0) {
                    // find sales forecast items which are after current timestamp
                    searchConditions = UtilMisc.toList(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                            EntityCondition.makeCondition("forecastDatetime", EntityOperator.GREATER_THAN, now));
                    List<GenericValue> salesForecastItems = delegator.findByCondition("SalesForecastItem", EntityCondition.makeCondition(searchConditions, EntityOperator.AND), null, UtilMisc.toList("salesForecastId", "forecastDatetime"));
                    for (GenericValue nextForecastItem : salesForecastItems) {
                        String details = "Sales forecast item [" + nextForecastItem.getString("salesForecastItemId") + "] at [" + nextForecastItem.getString("forecastDatetime") + "]: quantity [" + nextForecastItem.getBigDecimal("forecastQuantity") + "] for product [" + nextForecastItem.getString("productId") + "]";

                        // skip forecast items which have no forecast quantity
                        if (UtilValidate.isEmpty(nextForecastItem.getBigDecimal("forecastQuantity"))) {
                            Debug.logWarning("Skipping " + details + " because the forecastQuantity is null", MODULE);
                            continue;
                        }
                        parameters = UtilMisc.toMap("productId", nextForecastItem.getString("productId"),
                                "eventDate", nextForecastItem.getTimestamp("forecastDatetime"),
                                "inventoryEventPlanTypeId", "SALES_FORECAST", "facilityId", facilityId);
                        MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters,
                                percentageOfSalesForecast.negate().divide(new BigDecimal(100)).multiply(nextForecastItem.getBigDecimal("forecastQuantity")),
                                null,    // don't set the netQOH -- it will be calculated for us
                                details,    // name of the event
                                false,   // this is not late -- we do not have old sales forecast items included in MRP
                                null,   // No MrpInventoryEventDetail
                                delegator);
                    }

                }
            }


            Map result = ServiceUtil.returnSuccess();
            result.put("mrpRunProductIds", mrpRunProductIds);
            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Get the list of requirements which should be included in MRP if their status is approved.
     * This is a function of the requirement type.
     * @param facilityId a <code>String</code> value
     * @param facilityIdTo a <code>String</code> value
     * @param mrpRunProductIds a <code>List</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static List<GenericValue> getMrpInfoForApprovedRequirements(String facilityId, String facilityIdTo, List mrpRunProductIds, Delegator delegator) throws GenericEntityException {
        return getMrpInfoForRequirements(facilityId, facilityIdTo, UtilMisc.toList("PRODUCT_REQUIREMENT", "TRANSFER_REQUIREMENT", "PENDING_INTERNAL_REQ"), UtilMisc.toList("REQ_APPROVED"), mrpRunProductIds, delegator);
    }

    /**
     * Get all Requirements satisfying the status.
     * @param facilityId a <code>String</code> value
     * @param facilityIdTo a <code>String</code> value
     * @param requirementTypeIds a <code>List</code> value
     * @param statusIds a <code>List</code> value
     * @param mrpRunProductIds a <code>List</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static List<GenericValue> getMrpInfoForRequirements(String facilityId, String facilityIdTo, List requirementTypeIds, List statusIds, List mrpRunProductIds, Delegator delegator) throws GenericEntityException {
        List conditions = UtilMisc.toList(EntityCondition.makeCondition("requirementTypeId", EntityOperator.IN, requirementTypeIds),
                EntityCondition.makeCondition("statusId", EntityOperator.IN, statusIds),
                EntityCondition.makeCondition("quantity", EntityOperator.NOT_EQUAL, null));

        // AG24012008 - if this is a product or supplier specific MRP run then filter by the associated product Ids
        if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
            conditions.add(EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds));
        } else {
            // otherwise -- always exclude null products
            conditions.add(EntityCondition.makeCondition("productId", EntityOperator.NOT_EQUAL, null));
        }

        // add facilityId to the condition based on whether facilityId or facilityIdTo is required
        if (facilityId != null) {
            conditions.add(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId));
        }
        if (facilityIdTo != null) {
            conditions.add(EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityIdTo));
        }

        List<GenericValue> resultList = delegator.findByCondition("Requirement", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, null);
        return resultList;
    }

    /**
     * Create MRP timeline inventory events for Requirements which have been approved.
     * @param facilityId a <code>String</code> value
     * @param mrpRunProductIds a <code>List</code> value
     * @param now a <code>Timestamp</code> value
     * @param receiptEventBufferMilliseconds a <code>Double</code> value
     * @param userLogin the userLogin <code>GenericValue</code>
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @exception GenericEntityException if an error occurs
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static void initInventoryEventPlanForApprovedRequirements(String facilityId, List mrpRunProductIds, Timestamp now, BigDecimal receiptEventBufferMilliseconds, GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher) throws GenericEntityException, GenericServiceException {
        Map parameters = null;

        // first create inventory event plans for approved requirements from this facility
        List<GenericValue> resultList = getMrpInfoForApprovedRequirements(facilityId, null, mrpRunProductIds, delegator);
        for (GenericValue genericResult : resultList) {
            String productId =  genericResult.getString("productId");
            String requirementTypeId = genericResult.getString("requirementTypeId");
            BigDecimal eventQuantityTmp = genericResult.getBigDecimal("quantity");
            Timestamp requiredByDate = genericResult.getTimestamp("requiredByDate");
            if (requiredByDate == null) {
                requiredByDate = now;
            }

            Timestamp inventoryEventTimestamp = UtilCommon.beforeMillisecs(UtilCommon.laterOf(requiredByDate, now), receiptEventBufferMilliseconds);
            String inventoryEventPlanTypeId = "PROD_REQ_RECP";  // default is for approved product requirement
            if ("TRANSFER_REQUIREMENT".equals(requirementTypeId)) {
                inventoryEventPlanTypeId = "INV_XFER_REQ_OUT";
                eventQuantityTmp = eventQuantityTmp.negate();           // need to negate the quantity for outbound transfers
            } else if ("PENDING_INTERNAL_REQ".equals(requirementTypeId)) {
                inventoryEventPlanTypeId = "PEND_MANUF_O_RECP";
            } else { // other requirements may have their own plans
                Debug.logWarning("No inventoryEventPlanTypeId for [" + genericResult + "] found, assuming [" + inventoryEventPlanTypeId + "]", MODULE);
            }
            parameters = UtilMisc.toMap("productId", productId, "eventDate", inventoryEventTimestamp, "inventoryEventPlanTypeId", inventoryEventPlanTypeId, "facilityId", genericResult.getString("facilityId"));
            MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters, eventQuantityTmp, null, genericResult.getString("requirementId"), requiredByDate.before(now), null, delegator);

            // if it is an INTERNAL_REQUIREMENT, then add event for its components
            if ("PENDING_INTERNAL_REQ".equals(requirementTypeId)) {
                Debug.logInfo("initInventoryEventPlanForApprovedRequirements: found internal requirement", MODULE);
                // get the components
                Map<String, Object> serviceResponse = dispatcher.runSync("getManufacturingComponents", UtilMisc.toMap("productId", productId, "quantity", eventQuantityTmp, "excludeWIPs", Boolean.FALSE, "userLogin", userLogin));
                List<BomNodeInterface> components = (List<BomNodeInterface>) serviceResponse.get("components");
                Debug.logInfo("initInventoryEventPlanForApprovedRequirements: adding events for components: " + components, MODULE);
                for (BomNodeInterface node : components) {
                    // add events for each component, the event date begin the same as the internal requirement date
                    parameters = UtilMisc.toMap("productId", node.getProductId(), "eventDate", inventoryEventTimestamp, "inventoryEventPlanTypeId", "MANUF_ORDER_REQ", "facilityId", genericResult.getString("facilityId"));
                    MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters, node.getQuantity().negate(), null, genericResult.getString("requirementId"), requiredByDate.before(now), null, delegator);
                }
            }
        }

        // create inventory event plans for approved requirements to this facility
        resultList = getMrpInfoForApprovedRequirements(null, facilityId, mrpRunProductIds, delegator);
        for (GenericValue genericResult : resultList) {
            String productId =  genericResult.getString("productId");
            BigDecimal eventQuantityTmp = genericResult.getBigDecimal("quantity");
            Timestamp requiredByDate = genericResult.getTimestamp("requiredByDate");
            if (requiredByDate == null) {
                requiredByDate = now;
            }

            Timestamp inventoryEventTimestamp = UtilCommon.beforeMillisecs(UtilCommon.laterOf(requiredByDate, now), receiptEventBufferMilliseconds);
            String inventoryEventPlanTypeId = "INV_XFER_REQ_IN";  // default for inventory transfer requirement
            if (!"TRANSFER_REQUIREMENT".equals(genericResult.getString("requirementTypeId"))) {
                Debug.logWarning("No inventoryEventPlanTypeId for [" + genericResult + "] found, assuming [" + inventoryEventPlanTypeId + "]", MODULE);
            }
            parameters = UtilMisc.toMap("productId", productId, "eventDate", inventoryEventTimestamp, "inventoryEventPlanTypeId", inventoryEventPlanTypeId, "facilityId", genericResult.getString("facilityIdTo"));
            MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters, eventQuantityTmp, null, genericResult.getString("requirementId"), requiredByDate.before(now), null, delegator);
        }
    }

    @SuppressWarnings("unchecked")
    private static List getMrpRunProductIds(String mrpTargetProductId, String supplierPartyId, Delegator delegator) throws GenericEntityException {
        List productIds = null;
        if (UtilValidate.isNotEmpty(mrpTargetProductId)) {
            productIds = new LinkedList();
            productIds.add(mrpTargetProductId);
        } else {
            if (UtilValidate.isNotEmpty(supplierPartyId)) {
                productIds = UtilMrp.getProductIdsFromSupplier(supplierPartyId, delegator);
            }
        }
        return productIds;
    }

    @SuppressWarnings("unchecked")
    private static List getMrpOrderInfoForSalesOrders(String facilityId, List mrpRunProductIds, String productStoreId, String productStoreGroupId, Delegator delegator) throws GenericEntityException {
        List resultList = new LinkedList();
        List fieldsToSelect = UtilMisc.toList("orderId", "orderItemSeqId", "productId", "quantity", "cancelQuantity", "quantityReserved");
        fieldsToSelect.addAll(UtilMisc.toList("itemShipBeforeDate", "itemShipAfterDate", "itemEstimatedDeliveryDate", "shipByDate", "shipAfterDate"));
        List searchConditions = UtilMisc.toList(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"),
                EntityCondition.makeCondition("itemStatusId", EntityOperator.EQUALS, "ITEM_APPROVED"),
                EntityCondition.makeCondition("orderStatusId", EntityOperator.EQUALS, "ORDER_APPROVED"),
                EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                EntityUtil.getFilterByDateExpr("introductionDate", "salesDiscontinuationDate"));

        // AG23012008 - if this is a product or supplier specific MRP run then filter by these product Ids
        if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
            searchConditions.add(EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds));
        }

        // If a productStore specific MRP run is to be executed then filter sales orders by the productStoreId.  Otherwise, and only if productStoreId is empty,
        // if a productStoreGroup specific MRP run is to be executed, filter sales orders by the productStores belonging to this productStoreGroup
        List productStoreConditions = null;
        boolean isProductStoreSpecificMrpRun = false;
        if (UtilValidate.isNotEmpty(productStoreId)) {
            isProductStoreSpecificMrpRun = true;
            productStoreConditions = UtilMisc.toList(EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId));
        } else if (UtilValidate.isNotEmpty(productStoreGroupId)) {
            isProductStoreSpecificMrpRun = true;
            List productStoreIds = UtilMrp.getMrpProductStoreIdsFromGroup(productStoreGroupId, delegator);
            if (UtilValidate.isNotEmpty(productStoreIds)) {
                productStoreConditions = UtilMisc.toList(EntityCondition.makeCondition("productStoreId", EntityOperator.IN, productStoreIds));
            }
        }
        if (UtilValidate.isNotEmpty(productStoreConditions)) {
            searchConditions.addAll(productStoreConditions);
        }

        // get mrp sales order inventory events
        if (!isProductStoreSpecificMrpRun || (isProductStoreSpecificMrpRun && UtilValidate.isNotEmpty(productStoreConditions))) {
            resultList = delegator.findByCondition("MrpOrderInfo", EntityCondition.makeCondition(searchConditions, EntityOperator.AND), fieldsToSelect, UtilMisc.toList("reservedDatetime", "reserveSequenceId", "orderDate")); // order sales orders based on inventory reservation, then order date
        }
        return resultList;
    }

    @SuppressWarnings("unchecked")
    private static void removeOldRequirementRecords(EntityCondition condition, boolean removeRoles, Delegator delegator)    throws GenericEntityException {
        List listResult = null;
        List listResultRoles = new ArrayList();
        List listResultOrderReqCommitments = new ArrayList();
        listResult = delegator.findByCondition("Requirement", condition, null, null);
        if (listResult != null) {
            Iterator listResultIt = listResult.iterator();
            while (listResultIt.hasNext()) {
                GenericValue tmpRequirement = (GenericValue) listResultIt.next();
                if (removeRoles) {
                    listResultRoles.addAll(tmpRequirement.getRelated("RequirementRole"));
                }
                listResultOrderReqCommitments.addAll(tmpRequirement.getRelated("OrderRequirementCommitment"));
                //int numOfRecordsRemoved = delegator.removeRelated("RequirementRole", tmpRequirement);
            }
            if (removeRoles) {
                delegator.removeAll(listResultRoles);
            }
            delegator.removeAll(listResultOrderReqCommitments);
            delegator.removeAll(listResult);
        }
    }

    private static void removeOldMrpInventoryEventRecords(EntityCondition condition, Delegator delegator) throws GenericEntityException {
        /*
         * The ideal solution is to remove the MrpInventoryEvent and then use DELETE ON CASCADE to remove the _Detail records.  However, the Delegator does not support that.
         * The "correct" solution is then to find all the MrpInventoryEvent and then remove the _Detail first, then the MrpInventoryEvent.  However, that is extremely
         * inefficient at large number of records.  Fortunately, the _Detail record has the event type Id as well, so we can do a remove this way.
         */

        delegator.removeByCondition("MrpInventoryEventDetail", condition);
        delegator.removeByCondition("MrpInventoryEvent", condition);
    }

    /**
     * This method calculates a "rounded" quantity to stock, based on the un-rounded double qtyToStock and decimals, rounding mode for interim and final inventory events.
     * What it does is look for inventory events happening 1 second after the inventoryEventForMRP and depending on whether they exist or not round with the interim or final rounding modes.
     * @param qtyToStock
     * @param inventoryEventForMRP
     * @param decimals
     * @param interimRequirementRoundingMode
     * @param finalRequirementRoundingMode
     * @return the rounded quantity to stock
     * @throws GenericEntityException
     */
    @SuppressWarnings("unchecked")
    private static BigDecimal getRoundedQuantityToStock(BigDecimal qtyToStock, GenericValue inventoryEventForMRP, int decimals, RoundingMode interimRequirementRoundingMode, RoundingMode finalRequirementRoundingMode) throws GenericEntityException {
        Delegator delegator = inventoryEventForMRP.getDelegator();

        Debug.logInfo("Getting rounded quantity for inventory event " + inventoryEventForMRP, MODULE);
        Debug.logInfo("quantity to stock [" + qtyToStock + "]", MODULE);
        BigDecimal qtyToStockRounded = qtyToStock;

        // are there future inventory events?

        /**
         * Important Note: for some reason unknown to me (Si),
         * EntityCondition.makeCondition("eventDate", EntityOperator.GREATER_THAN, inventoryEventForMRP.getTimestamp("eventDate"))
         *
         * does not work!  It will return the event at 2008-11-30 23:59:59.998 even if the eventDate is 2008-11-30 23:59:59.998
         *
         * Hence we use GREATER_THAN_EQUAL_TO and add 1000 milliseconds (1 second).
         *
         */

        // if this event doesn't exist or has no eventDate, then this will cause the rest of the code to assume it does not have future events
        List futureInventoryEvents = null;
        if (UtilValidate.isNotEmpty(inventoryEventForMRP) && UtilValidate.isNotEmpty(inventoryEventForMRP.get("eventDate"))) {
            futureInventoryEvents = delegator.findByAnd("MrpFacilityInventoryEventPlanned", UtilMisc.toList(
                    EntityCondition.makeCondition("productId", EntityOperator.EQUALS, inventoryEventForMRP.getString("productId")),
                    EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, inventoryEventForMRP.getString("facilityId")),
                    EntityCondition.makeCondition("eventDate", EntityOperator.GREATER_THAN_EQUAL_TO, UtilCommon.afterMillisecs(inventoryEventForMRP.getTimestamp("eventDate"), UtilCommon.MSEC_IN_1_SEC))));
        }

        // Debug.logInfo("Future inventory events: " + futureInventoryEvents, MODULE);

        if (UtilValidate.isNotEmpty(futureInventoryEvents)) {
            qtyToStockRounded = qtyToStockRounded.setScale(decimals, interimRequirementRoundingMode);
            Debug.logInfo("There are still more inventory events for [ " + inventoryEventForMRP.get("productId") + "] in facility [" + inventoryEventForMRP.get("facilityId") + "] after event timestamp [" + inventoryEventForMRP.get("eventDate") + " ], so rounded [" + qtyToStock + "] to [" + qtyToStockRounded + "]", MODULE);
        } else {
            qtyToStockRounded = qtyToStockRounded.setScale(decimals, finalRequirementRoundingMode);
            Debug.logInfo("There are no more inventory events for [ " + inventoryEventForMRP.get("productId") + "] in facility [" + inventoryEventForMRP.get("facilityId") + "] after event timestamp [" + inventoryEventForMRP.get("eventDate") + " ], so rounded [" + qtyToStock + "] to [" + qtyToStockRounded + "]", MODULE);
        }

        return qtyToStockRounded;
    }

    /**
     * Get net PO orderItem quantity, net of ItemIssuances.
     * @param genericResult
     * @return
     * @throws GenericEntityException
     */
    private static BigDecimal getNetPurchaseOrderItemQuantity(GenericValue genericResult) throws GenericEntityException {
        OrderReadHelper orh = new OrderReadHelper(genericResult.getDelegator(), genericResult.getString("orderId"));
        BigDecimal netOrderedQuantity = getNetOrderedQuantity(genericResult);
        BigDecimal shippedQuantity = orh.getItemShippedQuantity(genericResult.getRelatedOne("OrderItem"));
        if (shippedQuantity != null) {
            netOrderedQuantity = netOrderedQuantity.subtract(shippedQuantity);
        }
        return netOrderedQuantity;
    }

    /**
     * Convenience method to get net quantity of an inventory transfer item, based on its status.
     * @param transferItem
     * @return
     */
    private static BigDecimal getInventoryTransferQuantity(GenericValue transferItem) {
        if ("SERIALIZED_INV_ITEM".equals(transferItem.getString("inventoryItemTypeId"))) {
            // the item should be transferred, but we add all these states to be sured
            if (("INV_AVAILABLE".equals(transferItem.getString("statusId")))
                    || ("INV_BEING_TRANSFERRED".equals(transferItem.getString("statusId")))
                    || ("INV_BEING_TRANS_PRM".equals(transferItem.getString("statusId")))) {
                return BigDecimal.ONE;
            } else {
                return BigDecimal.ZERO;
            }
        } else {
            if (transferItem.get("quantityOnHandTotal") != null) {
                return transferItem.getBigDecimal("quantityOnHandTotal");
            } else {
                return BigDecimal.ZERO;
            }
        }
    }

    /**
     * Gets the net ordered quantity from the given <code>OrderItem</code>: quantity - cancelQuantity.
     * @param item an <code>OrderItem</code> <code>GenericValue</code>
     * @return the net ordered quantity
     */
    private static BigDecimal getNetOrderedQuantity(GenericValue item) {
        BigDecimal shipGroupQuantity = item.getBigDecimal("quantity");
        BigDecimal cancelledQuantity = item.getBigDecimal("cancelQuantity");
        if (UtilValidate.isEmpty(shipGroupQuantity)) {
            shipGroupQuantity = BigDecimal.ZERO;
        }
        if (UtilValidate.isNotEmpty(cancelledQuantity)) {
            shipGroupQuantity = shipGroupQuantity.subtract(cancelledQuantity);
        }
        return shipGroupQuantity;
    }

    /**
     * Process the bill of material (bom) of the product  to insert components in the InventoryEventPlanned table.
     *   Before inserting in the entity, test if there is the record already existing to add quantity rather to create a new one.
     * @param product a <code>GenericValue</code> value
     * @param facilityId a <code>String</code> value
     * @param eventQuantity a <code>BigDecimal</code> value
     * @param startDate a <code>Timestamp</code> value
     * @param now a <code>Timestamp</code> value
     * @param routingTaskStartDate a <code>Map</code> value
     * @param listComponent a <code>List</code> value
     * @param timeZone a <code>TimeZone</code> value
     * @param locale a <code>Locale</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static void processBomComponent(GenericValue product, String facilityId, BigDecimal eventQuantity, Timestamp startDate, Timestamp now, Map routingTaskStartDate, List listComponent, TimeZone timeZone, Locale locale) throws GenericEntityException {
        Delegator delegator = product.getDelegator();

        if (listComponent != null && listComponent.size() > 0) {
            Iterator listComponentIter = listComponent.iterator();
            while (listComponentIter.hasNext()) {
                BomNodeInterface node = (BomNodeInterface) listComponentIter.next();
                GenericValue productComponent = node.getProductAssoc();
                // read the startDate for the component
                String routingTask = node.getProductAssoc().getString("routingWorkEffortId");
                Timestamp eventDate = (routingTask == null || !routingTaskStartDate.containsKey(routingTask)) ? startDate : (Timestamp) routingTaskStartDate.get(routingTask);
                // if the components is valid at the event Date create the Mrp requirement in the InventoryEventPlanned entity
                if (EntityUtil.isValueActive(productComponent, eventDate)) {
                    //Map parameters = UtilMisc.toMap("productId", productComponent.getString("productIdTo"));
                    Map parameters = UtilMisc.toMap("productId", node.getProduct().getString("productId"));
                    parameters.put("eventDate", UtilCommon.laterOf(eventDate, now));
                    parameters.put("inventoryEventPlanTypeId", "MRP_REQUIREMENT");
                    parameters.put("facilityId", facilityId);
                    BigDecimal componentEventQuantity = node.getQuantity();
                    MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(parameters, componentEventQuantity.negate(), null, product.get("productId") + ": " + UtilDateTime.timeStampToString(eventDate, UtilDateTime.getDateFormat(locale), timeZone, locale), eventDate.before(now), null, delegator);

                }
            }
        }
    }

    /**
     * Runs MRP from the web form. We use this as a wrapper to "opentaps.runMrp" which converts boolean from the form drop down.
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map runMrpFromForm(DispatchContext ctx, Map context) {
        // convert booleans from the context
        Boolean createTransferRequirements = new Boolean("Y".equalsIgnoreCase((String) context.get("createTransferRequirements")));
        Boolean createPendingManufacturingRequirements = new Boolean("Y".equalsIgnoreCase((String) context.get("createPendingManufacturingRequirements")));
        context.put("createTransferRequirements", createTransferRequirements);
        context.put("createPendingManufacturingRequirements", createPendingManufacturingRequirements);
        return runMrp(ctx, context);
    }

    /**
     * Service <code>opentaps.runMrp</code>, runs MRP.
     * Note on parameters:
     * <ul>
     *  <li><code>facilityGroupId</code> and <code>facilityId</code> are the facility group or facility where requirements are created. For a group of facilities MRP is run in sequence of their FacilityGroupMember.sequenceNum.
     *  <li><code>mrpName</code> is descriptive
     *  <li><code>defaultYearsOffset</code> is the number of years in the future sales order shipments are assumed to be required, if no explicit shipping date is supplied
     *  <li><code>receiptEventBuffer</code> and <code>receiptBufferTimeUomId</code> are used to specify the amount of time inventory events which increase inventory are shifted to the past.
     *  <li><code>requirementQuantityDecimals</code> is the number of decimal places to round resulting Requirement quantities.  By default, it is 2 or whatever "decimals" is set to in the Java code
     *  <li><code>interim_</code> and <code>finalRequirementRoundingMode</code> are used to control how fractional quantities are rounded for interim.
     *  <li><code>percentageOfSalesForecast</code> is the percentage of sales forecast to use.  0 means forecasts are excluded.  100 means forecasts are included in full.
     *  <li><code>createTransferRequirements</code>: if set to TRUE, then TRANSFER_REQUIREMENT will be created, instead of inventory transfers directly
     * </ul>
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> runMrp(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();

        String facilityGroupId = (String) context.get("facilityGroupId");
        String facilityId = (String) context.get("facilityId");

        if (UtilValidate.isEmpty(facilityId) && UtilValidate.isEmpty(facilityGroupId)) {
            return ServiceUtil.returnError("facilityId and facilityGroupId cannot be both null");
        }

        try {
            // validate product specific run
            String productId = (String) context.get("productId");
            if (UtilValidate.isNotEmpty(productId)) {
                GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
                if (UtilValidate.isEmpty(product)) {
                    return ServiceUtil.returnError("Product Id does not exist.");
                }
            }

            // validate supplier specific run
            String supplierPartyId = (String) context.get("supplierPartyId");
            if (UtilValidate.isNotEmpty(supplierPartyId)) {
                List<String> productIds = UtilMrp.getProductIdsFromSupplier(supplierPartyId, delegator);
                if (UtilValidate.isEmpty(productIds)) {
                    return ServiceUtil.returnError("Supplier does not have associated products.");
                }
            }

            // construct List of facilityIds to run MRP for based on their sequence in FacilityGroupMember
            List<String> facilityIds = null;
            if (UtilValidate.isEmpty(facilityGroupId)) {
                facilityIds = Arrays.asList(facilityId);
            } else {
                List<GenericValue> facilities = delegator.findByAnd("FacilityGroupMember", Arrays.asList(EntityCondition.makeCondition("facilityGroupId", EntityOperator.EQUALS, facilityGroupId),
                        EntityUtil.getFilterByDateExpr()), Arrays.asList("sequenceNum"));
                facilityIds = EntityUtil.getFieldListFromEntityList(facilities, "facilityId", true);
            }

            if (UtilValidate.isEmpty(facilityIds)) {
                return ServiceUtil.returnError("No valid facilityIds found for facilityId [" + facilityId + "] and facilityGroupId [" + facilityGroupId + "].  MRP will not run");
            }

            // run the MRP for each facility
            ModelService runMrpForFacility = ctx.getModelService("opentaps.runMrpForFacility");
            Map<String, Object> serviceParams = runMrpForFacility.makeValid(context, "IN");
            for (String mrpFacilityId : facilityIds) {
                serviceParams.put("facilityId", mrpFacilityId);
                Map<String, Object> tmpResult = dispatcher.runSync("opentaps.runMrpForFacility", serviceParams, UtilCommon.SEC_IN_2_HOURS, false);  // run in same transaction
                if (ServiceUtil.isError(tmpResult) || ServiceUtil.isFailure(tmpResult)) {
                    return tmpResult;
                }
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service <code>opentaps.runMrpForFacility</code>, runs MRP for a facility.
     * Called by {@link #runMrp} -- see that service for parameter documentation.
     * The parameter <code>mrpConfiguration</code> is a new, experimental configuration class, see {@link org.opentaps.purchasing.mrp.MrpConfiguration}.
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> runMrpForFacility(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String mrpName = (String) context.get("mrpName");
        Integer defaultYearsOffset = (Integer) context.get("defaultYearsOffset");
        String facilityId = (String) context.get("facilityId");
        String supplierPartyId = (String) context.get("supplierPartyId");
        String productStoreId = (String) context.get("productStoreId");
        String productStoreGroupId = (String) context.get("productStoreGroupId");
        String mrpTargetProductId = (String) context.get("productId");
        BigDecimal receiptEventBuffer = (BigDecimal) context.get("receiptEventBuffer");
        String receiptBufferTimeUomId = (String) context.get("receiptBufferTimeUomId");
        Boolean reinitializeInventoryEvents = (Boolean) context.get("reInitializeInventoryEvents");
        MrpConfiguration mrpConfiguration = (MrpConfiguration) context.get("mrpConfiguration");
        Integer requirementQuantityDecimals = (Integer) context.get("requirementQuantityDecimals");
        RoundingMode interimRequirementRoundingMode = (RoundingMode) context.get("interimRequirementRoundingMode");
        RoundingMode finalRequirementRoundingMode = (RoundingMode) context.get("finalRequirementRoundingMode");
        BigDecimal percentageOfSalesForecast = (BigDecimal) context.get("percentageOfSalesForecast");
        Boolean createTransferRequirements = (Boolean) context.get("createTransferRequirements");
        Boolean createPendingManufacturingRequirements = (Boolean) context.get("createPendingManufacturingRequirements");

        if (reinitializeInventoryEvents == null) {
            Debug.logInfo("reinitialize inventory events was null, assuming TRUE", MODULE);
            reinitializeInventoryEvents = Boolean.TRUE;
        }

        if (createPendingManufacturingRequirements == null) {
            Debug.logInfo("create pending manufacturing requirements was null, assuming FALSE", MODULE);
            createPendingManufacturingRequirements = Boolean.FALSE;
        }


        // initialization
        int reqQtyDecimals = decimals;
        if (requirementQuantityDecimals == null) {
            Debug.logInfo("Using a default of [" + reqQtyDecimals + "] for decimals of MRP Requirements", MODULE);
        } else {
            reqQtyDecimals = requirementQuantityDecimals.intValue();
        }
        if (interimRequirementRoundingMode == null) {
            Debug.logInfo("Using a default of [" + defaultRoundingMode + "] for interim requirement rounding mode of MRP Requirements", MODULE);
            interimRequirementRoundingMode = defaultRoundingMode;
        }
        if (finalRequirementRoundingMode == null) {
            Debug.logInfo("Using a default of [" + defaultRoundingMode + "] for final requirement rounding mode of MRP Requirements", MODULE);
            finalRequirementRoundingMode = defaultRoundingMode;
        }

        try {
            int bomLevelWithNoEvent = 0;
            BigDecimal stockTmp = BigDecimal.ZERO;
            BigDecimal initialQoh = BigDecimal.ZERO;
            String oldProductId = null;
            String productId = null;
            GenericValue product = null;
            BigDecimal eventQuantity = BigDecimal.ZERO;
            Timestamp eventDate = null;
            BigDecimal reorderQuantity = BigDecimal.ZERO;
            BigDecimal minimumStock = BigDecimal.ZERO;
            int daysToShip = 0;
            List components = null;
            boolean isBuilt = false;
            GenericValue routing = null;

            Map<String, Object> parameters = null;
            List listInventoryEventForMRP = null;
            ListIterator iteratorListInventoryEventForMRP = null;
            GenericValue inventoryEventForMRP = null;

            BigDecimal receiptEventBufferMilliseconds = null;
            Map serviceResponse = null;

            // convert the inventory receipt event buffer to milliseconds
            if ((receiptEventBuffer != null) && (receiptBufferTimeUomId != null)) {
                serviceResponse = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", receiptBufferTimeUomId, "originalValue", receiptEventBuffer, "uomIdTo", "TF_ms"));
                if ((ServiceUtil.isError(serviceResponse)) || (ServiceUtil.isFailure(serviceResponse) || (serviceResponse.get("convertedValue") == null))) {
                    return serviceResponse;
                }
                receiptEventBufferMilliseconds = (BigDecimal) serviceResponse.get("convertedValue");
            }

            // IMPORTANT: MRP is VERY timestamp sensitive, so it's important to synch all the now on all operations
            Timestamp now = UtilDateTime.nowTimestamp();
            Debug.logInfo("Running MRP with warehouse facility [" + facilityId + "] and current timestamp of [" + now
                    + "] and default years offset [" + defaultYearsOffset + "] and inventory receipt event buffer of [" + receiptEventBufferMilliseconds + "] ms", MODULE);

            // Initialization of the InventoryEventPlanned table, This table will contain the products we want to buy or build.
            parameters = UtilMisc.toMap("facilityId", facilityId, "reInitialize", reinitializeInventoryEvents, "defaultYearsOffset", defaultYearsOffset, "now", now,
                    "receiptEventBufferMilliseconds", receiptEventBufferMilliseconds, "userLogin", userLogin);
            // MRP run can be supplier, productStore, productStoreGroup or product specific
            parameters.put("supplierPartyId", supplierPartyId);
            parameters.put("productStoreId", productStoreId);
            parameters.put("productStoreGroupId", productStoreGroupId);
            parameters.put("productId", mrpTargetProductId);
            parameters.put("percentageOfSalesForecast", percentageOfSalesForecast);

            Map<String, Object> result = dispatcher.runSync("opentaps.initInventoryEventPlanned", parameters, UtilCommon.SEC_IN_2_HOURS, false); // use same transaction
            int bomLevel = 0;
            do {
                // AG23012008 - if this is a supplier or product specific MRP run then filter by the associated productIds
                List<String> mrpRunProductIds = (List<String>) result.get("mrpRunProductIds");

                //get the products from the InventoryEventPlanned table for the current billOfMaterialLevel (ie. BOM)
                listInventoryEventForMRP = getInventoryEventPlanned(facilityId, productStoreId, productStoreGroupId, mrpRunProductIds, bomLevel, delegator);

                if (UtilValidate.isNotEmpty(listInventoryEventForMRP)) {
                    bomLevelWithNoEvent = 0;
                    iteratorListInventoryEventForMRP = listInventoryEventForMRP.listIterator();

                    Map<String, BigDecimal> qtyCoveredByReq = FastMap.newInstance();
                    oldProductId = "";
                    while (iteratorListInventoryEventForMRP.hasNext()) {
                        inventoryEventForMRP = (GenericValue) iteratorListInventoryEventForMRP.next();
                        List<GenericValue> mrpInventoryEventDetails = inventoryEventForMRP.getRelated("MrpInventoryEventDetail", UtilMisc.toList("mrpInvEvtDetSeqId"));
                        productId = inventoryEventForMRP.getString("productId");

                        // assume a default event quantity of 0.  This will force the stockTmp vs minimumStock check later and create requirements
                        eventQuantity = (inventoryEventForMRP.get("eventQuantity") != null ? inventoryEventForMRP.getBigDecimal("eventQuantity") : BigDecimal.ZERO);

                        if (!productId.equals(oldProductId)) {
                            // It's a new product, so it's necessary to get the product's QOH and create an InventoryEventPlanned for it
                            product = inventoryEventForMRP.getRelatedOneCache("Product");
                            stockTmp = MrpServices.findProductMrpQoh(/* mrpId */ null, product, facilityId, dispatcher, delegator);
                            MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(UtilMisc.toMap("productId", product.getString("productId"), "inventoryEventPlanTypeId", "INITIAL_QOH", "eventDate", now, "facilityId", facilityId), stockTmp, stockTmp, null, false, null, delegator);
                            // days to ship is only relevant for sales order to plan for preparatory days to ship.  Otherwise MRP will push event dates for manufacturing parts
                            // as well and cause problems
                            daysToShip = 0;
                            reorderQuantity = (inventoryEventForMRP.get("reorderQuantity") != null ? inventoryEventForMRP.getBigDecimal("reorderQuantity") : new BigDecimal("-1"));
                            minimumStock = (inventoryEventForMRP.get("minimumStock") != null ? inventoryEventForMRP.getBigDecimal("minimumStock") : BigDecimal.ZERO);
                            if ("SALES_ORDER_SHIP".equals(inventoryEventForMRP.getString("inventoryEventPlanTypeId"))) {
                                daysToShip = (inventoryEventForMRP.getLong("daysToShip") != null ? inventoryEventForMRP.getLong("daysToShip").intValue() : 0);
                            }

                            oldProductId = productId;
                        }

                        initialQoh = stockTmp;
                        stockTmp = stockTmp.add(eventQuantity);


                        Debug.logInfo("before creating backup inventory transfers, product [" + productId + "] has stock ["  + stockTmp + "] and minimum stock [" + minimumStock + "] and days to ship [" + daysToShip + "]" , MODULE);


                        // First try to create inventory transfers from backup inventory facilities.
                        // If the reorderQuantity from ProductFacility is greater than the shortfall (minStock - current stock)
                        // then transfer the full reorderQuantity.  The quantity to transfer is rounded according to MRP parameters.
                        if (stockTmp.compareTo(minimumStock) < 0) {
                            BigDecimal totalTransferredQuantity = transferInventoryForMrp(productId, facilityId, getRoundedQuantityToStock((minimumStock.subtract(stockTmp)), inventoryEventForMRP, reqQtyDecimals, interimRequirementRoundingMode, finalRequirementRoundingMode).max(reorderQuantity),
                                    inventoryEventForMRP.getTimestamp("eventDate"), receiptEventBufferMilliseconds, initialQoh, now, createTransferRequirements, dispatcher, locale, userLogin);
                            Debug.logInfo("transferred quantity = [" + totalTransferredQuantity + "]", MODULE);
                            stockTmp = stockTmp.add(totalTransferredQuantity);
                        }

                        Debug.logInfo("after creating backup inventory transfers, product [" + productId + "] has stock ["  + stockTmp + "] and minimum stock [" + minimumStock + "] and days to ship [" + daysToShip + "]" , MODULE);

                        // now use MRP to create requirements
                        if (stockTmp.compareTo(minimumStock) < 0) {

                            /**
                             * If at any time as we recurse through inventory events, our stock falls below minimum stock, then we need to build or order the product and
                             * create an InventoryEventPlanned to account for it.
                             * Set the time to the later of now or when the event is needed and set isLate to true if the event is before the current time
                             */

                            // Si: Not sure why there are two of these blocks, but I moved it into this loop (it was outside of it from the ofbiz mrp) for efficiency purposes: otherwise the system
                            // would get manufacturing components for all products, even those that have no inventory event planned records
                            // -----------------------------------------------------
                            // The components are also loaded thru the configurator
                            BigDecimal positiveEventQuantity = (eventQuantity.signum() > 0 ? eventQuantity : eventQuantity.negate());
                            serviceResponse = dispatcher.runSync("getManufacturingComponents", UtilMisc.<String, Object>toMap("productId", product.getString("productId"), "quantity", positiveEventQuantity, "excludeWIPs", Boolean.FALSE, "userLogin", userLogin));

                            // Lack of workEffortId signify some routings for the product and
                            // with min/max quantity exist but don't match requested quantity.
                            boolean handleMinMaxQty = UtilValidate.isEmpty((String) serviceResponse.get("workEffortId"));
                            if (handleMinMaxQty) {
                                // check all such routings to find minimal quantity
                                stockTmp = ensureMinQuantity(stockTmp.negate(), productId, delegator).negate();
                            }

                            components = (List) serviceResponse.get("components");
                            if (components != null && components.size() > 0) {
                                BomNodeInterface node = ((BomNodeInterface) components.get(0)).getParentNode();
                                isBuilt = node.isManufactured();
                            } else {
                                isBuilt = false;
                            }
                            // #####################################################

                            /**
                             *  The system will round the quantity required down to the required decimal places, then added the actual ordered/manufactured quantity
                             * to the stockTmp for the next inventory event, and hence "accrue" fractional quantities forward.  For example, if the requirementQuantityDecimals is 0
                             * interimEventRoundingMode = RoundingMode.OWN, and finalEventRoundingMode = RoundingMode.HALF_UP
                             * (ie, only integer quantities will be created), and you have shortfalls of
                             * Month 1 0.4
                             * Month 2 0.4
                             * Month 3 1.1
                             * Month 4 1.4
                             * Month 5 1.3
                             *
                             * It will produce
                             * Month 1  0
                             * Month 2  0
                             * Month 3  1
                             * Month 4  2
                             * Month 5  2
                             *
                             * In the final period will it round normally.  In all other periods, it will round down.
                             */

                            BigDecimal qtyToStock = getRoundedQuantityToStock((handleMinMaxQty ? BigDecimal.ZERO : minimumStock).subtract(stockTmp), inventoryEventForMRP, reqQtyDecimals, interimRequirementRoundingMode, finalRequirementRoundingMode);

                            eventDate = (inventoryEventForMRP.get("eventDate") != null ? inventoryEventForMRP.getTimestamp("eventDate") : now);
                            // to be just before the requirement
                            eventDate.setTime(eventDate.getTime() - 1);

                            //

                            // if product quantity bigger that any max quantity, split it into smaller chunks.
                            // each of them should be between smallest min quantity and biggest max quantity
                            // for the product.
                            List<OpentapsProposedOrder> proposedOrders = FastList.newInstance();
                            if (handleMinMaxQty) {
                                List<BigDecimal> proposedOrderQuantities = splitJob(qtyToStock, productId, delegator);
                                if (UtilValidate.isNotEmpty(proposedOrderQuantities)) {
                                    for (BigDecimal chunkSize : proposedOrderQuantities) {
                                        proposedOrders.add(new OpentapsProposedOrder(product, facilityId, facilityId, isBuilt, eventDate, chunkSize, createPendingManufacturingRequirements));
                                    }
                                }
                            }

                            if (UtilValidate.isEmpty(proposedOrders)) {
                                proposedOrders.add(new OpentapsProposedOrder(product, facilityId, facilityId, isBuilt, eventDate, qtyToStock, createPendingManufacturingRequirements));
                            }

                            for (OpentapsProposedOrder proposedOrder : proposedOrders) {
                                proposedOrder.setMrpName(mrpName);
                                // calculate the ProposedOrder quantity and update the quantity object property.
                                proposedOrder.calculateQuantityToSupply(reorderQuantity, minimumStock, iteratorListInventoryEventForMRP);

                                // -----------------------------------------------------
                                // The components are also loaded thru the configurator
                                serviceResponse = dispatcher.runSync("getManufacturingComponents", UtilMisc.<String, Object>toMap("productId", product.getString("productId"), "quantity", proposedOrder.getQuantity(), "excludeWIPs", Boolean.FALSE, "userLogin", userLogin));
                                components = (List) serviceResponse.get("components");
                                String routingId = (String) serviceResponse.get("workEffortId");
                                if (routingId != null) {
                                    routing = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", routingId));
                                } else {
                                    routing = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", "DEFAULT_ROUTING"));
                                }
                                if (components != null && components.size() > 0) {
                                    BomNodeInterface node = ((BomNodeInterface) components.get(0)).getParentNode();
                                    isBuilt = node.isManufactured();
                                } else {
                                    isBuilt = false;
                                }

                                BigDecimal productQtyInReq = qtyCoveredByReq.get(productId);
                                // ensure initial value is initialized
                                if (productQtyInReq == null) {
                                    productQtyInReq = BigDecimal.ZERO;
                                }

                                String requirementId = null;
                                // Create requirement for Proposed Manufacturing Order Receipt and process BOM
                                // components if proposed quantity less than quantity of existent requirements.
                                // Requirements for quantity greater than currently required may be created
                                // if minimal quantity to order is specified for product.
                                if (productQtyInReq.compareTo(positiveEventQuantity) <= 0) {

                                    // #####################################################
                                    // calculate the ProposedOrder requirementStartDate and update the requirementStartDate object property.
                                    Map<String, Timestamp> routingTaskStartDate = proposedOrder.calculateStartDate(daysToShip, routing, delegator, dispatcher, userLogin);
                                    if (isBuilt) {
                                        // process the product components
                                        processBomComponent(product, facilityId, proposedOrder.getQuantity(), proposedOrder.getRequirementStartDate(), now, routingTaskStartDate, components, timeZone, locale);
                                    }

                                    // create the  ProposedOrder (only if the product is warehouse managed), and the InventoryEventPlanned associated
                                    requirementId = proposedOrder.create(ctx, userLogin);
                                    productQtyInReq = productQtyInReq.add(proposedOrder.getQuantity());
                                }

                                // decrease and store allocated quantity to use in next iteration 
                                productQtyInReq = productQtyInReq.subtract(positiveEventQuantity);
                                qtyCoveredByReq.put(productId, productQtyInReq);

                                // create OrderRequirementCommitments to associate this requirement to the original sales order shipment that caused it
                                // here we use the info we saved in MrpInventoryEventDetail when we initialized the mrp inventory events horizon (in initInventoryEventPlanned)
                                if ("SALES_ORDER_SHIP".equals(inventoryEventForMRP.getString("inventoryEventPlanTypeId"))) {
                                    // get the inventory event details associated with this mrp inventory event ordered by insert order (OISGIR priority order)
                                    if (UtilValidate.isNotEmpty(mrpInventoryEventDetails)) {
                                        BigDecimal initialQohTmp = initialQoh;
                                        BigDecimal unAllocatedQuantity = proposedOrder.getQuantity();
                                        for (GenericValue mrpInventoryEventDetail : mrpInventoryEventDetails) {
                                            boolean skipAllocation = false;
                                            Map<String, String> orderRequirementCommitmentInput = new HashMap<String, String>();
                                            orderRequirementCommitmentInput.put("orderId", mrpInventoryEventDetail.getString("orderId"));
                                            orderRequirementCommitmentInput.put("orderItemSeqId", mrpInventoryEventDetail.getString("orderItemSeqId"));
                                            orderRequirementCommitmentInput.put("requirementId", requirementId);
                                            GenericValue orderRequirementCommitment = delegator.makeValue("OrderRequirementCommitment", orderRequirementCommitmentInput);

                                            // allocate requirement quantities according with OISGIR priority order which is the default order by which MrpInventoryEventDetails where created
                                            BigDecimal inventoryEventDetailQuantity  = mrpInventoryEventDetail.getBigDecimal("quantity");
                                            BigDecimal positiveInventoryEventDetailQuantity = inventoryEventDetailQuantity.abs();
                                            BigDecimal deltaQoh = initialQohTmp.subtract(positiveInventoryEventDetailQuantity);
                                            BigDecimal quantityToAllocate = BigDecimal.ZERO;
                                            if ((initialQohTmp.signum() > 0) && (deltaQoh.subtract(minimumStock).signum() >= 0)) {
                                                // if there is stock and if the stock is sufficient to fulfill the order
                                                // then skip allocation to purchase/manufacturing requirement
                                                skipAllocation = true;
                                            } else if ((initialQohTmp.signum() > 0) && (deltaQoh.subtract(minimumStock).signum() < 0)) {
                                                // if there is stock and the existing stock does not completely fulfill the order
                                                // then try to allocate the order quantity not fulfilled by the stock to the purchase/manufacturing requirement
                                                BigDecimal positiveDeltaQoh = deltaQoh.abs();
                                                quantityToAllocate = positiveDeltaQoh;
                                            } else if (initialQohTmp.signum() <= 0) {
                                                // if there is no stock then try to allocate the full order quantity amount
                                                quantityToAllocate =  positiveInventoryEventDetailQuantity;
                                            } else {
                                                skipAllocation = true;
                                            }

                                            if ((unAllocatedQuantity.compareTo(quantityToAllocate) >= 0) && (quantityToAllocate.signum() > 0)) {
                                                orderRequirementCommitment.put("quantity", quantityToAllocate);
                                            } else if ((unAllocatedQuantity.compareTo(quantityToAllocate) < 0) && (unAllocatedQuantity.signum() > 0)) {
                                                orderRequirementCommitment.put("quantity", unAllocatedQuantity);
                                            } else {
                                                Debug.logWarning("Order [" + mrpInventoryEventDetail.getString("orderId") + "] item [" + mrpInventoryEventDetail.getString("orderItemSeqId") + "] "
                                                        + " had open quantity of [" + inventoryEventDetailQuantity + "] but unallocated quantity is now [" + unAllocatedQuantity + "], so the Requirement [" + requirementId
                                                        + "] will not be assigned to this order", MODULE);
                                                skipAllocation = true;
                                            }

                                            if (!skipAllocation && UtilValidate.isNotEmpty(requirementId)) {
                                                orderRequirementCommitment.create();
                                                unAllocatedQuantity = unAllocatedQuantity.subtract(quantityToAllocate);
                                            }
                                            initialQohTmp = initialQohTmp.subtract(positiveInventoryEventDetailQuantity);
                                        }
                                    } else {
                                        Debug.logWarning("Failed to create OrderRequirementCommitment link between sales order items and requirement [" + requirementId + "]", MODULE);
                                    }
                                }

                                String eventName = null;
                                if (UtilValidate.isNotEmpty(requirementId)) {
                                    eventName = "*" + requirementId + " (" + proposedOrder.getRequirementStartDate() + ")*";
                                }
                                Timestamp plannedEventDate = eventDate;
                                String inventoryEventPlanTypeId = (isBuilt ? "PROP_MANUF_O_RECP" : "PROP_PUR_O_RECP");
                                if (mrpConfiguration != null) {
                                    plannedEventDate = mrpConfiguration.getPlannedEventDate(product.getString("productId"), eventDate, inventoryEventPlanTypeId);
                                }
                                // Debug.logInfo("event date [" + eventDate + "] plannedEventDate [" + plannedEventDate + "]", MODULE);

                                Map<String, Object> eventMap = UtilMisc.toMap(
                                        "productId", product.getString("productId"),
                                        "eventDate", UtilCommon.laterOf(plannedEventDate, now),
                                        "inventoryEventPlanTypeId", inventoryEventPlanTypeId,
                                        "facilityId", facilityId
                                );
                                // TODO: Should this be shifted forward as well as it's a positive inventory event?
                                 Debug.logInfo("about to create inventory event " + eventMap + ", quantity " + proposedOrder.getQuantity() + ", requirementId " + requirementId, MODULE);

                                // don't create event if requirement hasn't been created
                                if (UtilValidate.isNotEmpty(requirementId)) {
                                    MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(eventMap, proposedOrder.getQuantity(), initialQoh.add(proposedOrder.getQuantity()), eventName, proposedOrder.getRequirementStartDate().before(now), null, delegator);
                                    stockTmp = stockTmp.add(proposedOrder.getQuantity());
                                }
                            }

                            // store netQoh
                            /*
                        if (mrpInventoryEvent != null) {
                            MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(UtilMisc.toMap("productId", mrpInventoryEvent.getString("productId"),
                                    "inventoryEventPlanTypeId", mrpInventoryEvent.getString("inventoryEventPlanTypeId"),
                                    "eventDate", mrpInventoryEvent.getTimestamp("eventDate"), "facilityId", facilityId),
                                    null, new Double(stockTmp), null, false,
                                    null, delegator);
                        }
                             */
                        }
                    }
                } else {
                    bomLevelWithNoEvent += 1;
                }

                bomLevel += 1;
                // if there are 3 levels with no inventoryEvenPanned we stop
            } while (bomLevelWithNoEvent < 3);

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Returns the list of inventory event planned for MRP.  They are ordered by billOfMaterialLevel.
     * @param facilityId
     * @param productStoreId
     * @param productStoreGroupId
     * @param mrpRunProductIds TODO
     * @param bomLevel
     * @param delegator
     * @return
     * @throws GenericEntityException
     */
    @SuppressWarnings("unchecked")
    private static List getInventoryEventPlanned(String facilityId, String productStoreId, String productStoreGroupId, List mrpRunProductIds, int bomLevel, Delegator delegator) throws GenericEntityException {
        EntityCondition bomLevelCondition = null;
        if (bomLevel == 0) {
            bomLevelCondition = EntityCondition.makeCondition(EntityCondition.makeCondition("billOfMaterialLevel", EntityOperator.EQUALS, null),
                    EntityOperator.OR,
                    EntityCondition.makeCondition("billOfMaterialLevel", EntityOperator.EQUALS, bomLevel));
        } else {
            bomLevelCondition = EntityCondition.makeCondition("billOfMaterialLevel", EntityOperator.EQUALS, bomLevel);
        }
        // (billOfMaterialLevel condition) AND (facilityId=facilityId)
        List lookupConditions = UtilMisc.toList(bomLevelCondition,
                EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                EntityUtil.getFilterByDateExpr("introductionDate", "salesDiscontinuationDate"));

        if (UtilValidate.isNotEmpty(mrpRunProductIds)) {
            lookupConditions.add(EntityCondition.makeCondition("productId", EntityOperator.IN, mrpRunProductIds));
        }

        List orderBy = UtilMisc.toList("productId", "eventDate");

        // AG111207 - the filtering by product store (group) assumes initInventoryEventPlanned(...) processed only sales orders for this product store (group)
        // the product store (group) filtering basically filters the products that have associated sales orders, hence the importance of the fact stated above
        List mrpInventoryEvents = null;
        ModelEntity mrpFacilityInventoryEventPlanned = delegator.getModelEntity("MrpFacilityInventoryEventPlanned");
        List fieldsToSelect = mrpFacilityInventoryEventPlanned.getAllFieldNames();
        fieldsToSelect.remove("eventName");
        if (UtilValidate.isNotEmpty(productStoreId)) {
            lookupConditions.add(EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId));
            mrpInventoryEvents = delegator.findByCondition("FacilityProductAndMrpEventAndDetailAndOrder", EntityCondition.makeCondition(lookupConditions, EntityOperator.AND), null, fieldsToSelect, orderBy, UtilCommon.DISTINCT_READ_OPTIONS);
        } else if (UtilValidate.isNotEmpty(productStoreGroupId)) {
            lookupConditions.add(EntityCondition.makeCondition("productStoreGroupId", EntityOperator.EQUALS, productStoreGroupId));
            mrpInventoryEvents = delegator.findByCondition("FacilityProductAndMrpEventAndDetailAndOrderAndProductStoreGroup", EntityCondition.makeCondition(lookupConditions, EntityOperator.AND), null, fieldsToSelect, orderBy, UtilCommon.DISTINCT_READ_OPTIONS);
        } else {
            mrpInventoryEvents = delegator.findByCondition("MrpFacilityInventoryEventPlanned", EntityCondition.makeCondition(lookupConditions, EntityOperator.AND), null, orderBy);
        }

        return mrpInventoryEvents;
    }

    /**
     * Make inventory transfer requests or inventory transfer requirements (depending on createTransferRequirement flag) for productId to the parameter facilityId from all BACKUP_INVENTORY associated facilities in their enumerated sequence.
     * The inventory transfer or transfer requirement is scheduled right before the eventDate (minus 1 millisecond) if there are no FacilityTransferPlans after the required date.
     * Otherwise, the transfer will take place at the time of the NEXT FacilityTransferPlan after the eventDate.
     * InventoryEventPlanned will be created in both the incoming and outgoing warehouses
     * The isLate flag is set based on how eventDate compares to the now Timestamp, which is passed in for standardizing all MRP.
     * @param productId the product ID to transfer
     * @param facilityId the facility ID where to transfer
     * @param maxTransferQuantity a <code>BigDecimal</code> value
     * @param eventDate a <code>Timestamp</code> value
     * @param receiptEventBufferMilliseconds a <code>Double</code> value
     * @param initialQoh a <code>BigDecimal</code> value
     * @param now a <code>Timestamp</code> value
     * @param createTransferRequirements a <code>Boolean</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param locale a <code>Locale</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @return the quantity transferred
     * @exception GenericEntityException if an error occurs
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static BigDecimal transferInventoryForMrp(String productId, String facilityId, BigDecimal maxTransferQuantity, Timestamp eventDate, BigDecimal receiptEventBufferMilliseconds, BigDecimal initialQoh, Timestamp now, Boolean createTransferRequirements, LocalDispatcher dispatcher, Locale locale, GenericValue userLogin) throws GenericEntityException, GenericServiceException {

        Delegator delegator = dispatcher.getDelegator();

        // get the replenish method as configured in the ProductFacility.  See http://opentaps.org/docs/index.php/Inventory_Stock_Levels for more information.
        //  * PF_RM_NEVER:      never transfer from backup warehouse
        //  * PF_RM_BACKUP:     transfer from the backup warehouse if they have inventory (this is the default, and was the previous implementation)
        //  * PF_RM_SPECIF:     transfer only from the specified backup warehouse if inventory is available
        //  * PF_RM_BACKUP_ALW:  always transfer from the backup warehouses, even if they have no inventory
        //  * PF_RM_SPECIF_ALW: always transfer from the specified backup warehouse, even if it has no inventory
        Debug.logInfo("transferInventoryForMrp maxTransferQuantity : " + maxTransferQuantity, MODULE);
        // get the configured option
        String replenishMethod = "PF_RM_BACKUP";
        String specifiedFacilityId = null;
        GenericValue productFacility = delegator.findByPrimaryKeyCache("ProductFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
        if (productFacility != null) {
            replenishMethod = productFacility.getString("replenishMethodEnumId");
            specifiedFacilityId = productFacility.getString("replenishFromFacilityId");
            Debug.logInfo("Found replenish settings: method = [" + replenishMethod + "] from facility [" + specifiedFacilityId + "]", MODULE);
        }

        // if configured to never transfer from backup, return 0 now
        if ("PF_RM_NEVER".equals(replenishMethod)) {
            return BigDecimal.ZERO;
        }

        boolean hasSpecifiedFacility = (UtilValidate.isNotEmpty(specifiedFacilityId) && ("PF_RM_SPECIF".equals(replenishMethod) || "PF_RM_SPECIF_ALW".equals(replenishMethod)));
        boolean forceTransferWithNoInventory = ("PF_RM_BACKUP_ALW".equals(replenishMethod) || "PF_RM_SPECIF_ALW".equals(replenishMethod));

        List<String> backupFacilitiesIds;

        if (!hasSpecifiedFacility) {
            // get the associated backup warehouses facilities where inventory could be transferred from
            List backupWarehousesConditions = UtilMisc.toList(
                    EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityId),
                    EntityCondition.makeCondition("facilityAssocTypeId", EntityOperator.EQUALS, "BACKUP_INVENTORY"),
                    EntityUtil.getFilterByDateExpr()
            );
            List<GenericValue> backupFacilities = delegator.findByAnd("FacilityAssoc", backupWarehousesConditions, UtilMisc.toList("sequenceNum"));
            backupFacilitiesIds = EntityUtil.getFieldListFromEntityList(backupFacilities, "facilityId", true);
        } else {
            // if configured for a specified facility, then get this facility instead
            backupFacilitiesIds = UtilMisc.toList(specifiedFacilityId);
        }

        BigDecimal quantityTransferred = BigDecimal.ZERO;
        BigDecimal quantityToTransfer = maxTransferQuantity;
        Map tmpResult = null;

        // This happens if there is no real inventory event planned, so we don't need to transfer anything
        if (eventDate == null) {
            return quantityTransferred;
        }

        // by default, time of inventory transfer is right before the required event timestamp
        Timestamp defaultTransferTimestamp = eventDate;
        defaultTransferTimestamp.setTime(defaultTransferTimestamp.getTime() - 1);

        // iterate and transfer inventory from each facility associated facility
        for (String backupFacilityId : backupFacilitiesIds) {
            if (quantityToTransfer.compareTo(BigDecimal.ZERO) == 1) {

                // now see if there are any FacilityTransferPlan which are scheduled from the backupFacilityId to the desired facility
                // after the event date, or the current default transfer timestamp
                // order search results in increasing order of the scheduled transfer date/time so that we can pick the first one
                Timestamp transferTimestamp = defaultTransferTimestamp;
                List<GenericValue> facilityTransferPlans = delegator.findByAnd("FacilityTransferPlan", UtilMisc.toList(
                        EntityCondition.makeCondition("facilityIdFrom", EntityOperator.EQUALS, backupFacilityId),
                        EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityId),
                        EntityCondition.makeCondition("scheduledTransferDatetime", EntityOperator.GREATER_THAN, transferTimestamp)
                ), UtilMisc.toList("scheduledTransferDatetime"));
                if (UtilValidate.isNotEmpty(facilityTransferPlans)) {
                    GenericValue earliestTransferPlan = EntityUtil.getFirst(facilityTransferPlans);
                    transferTimestamp = earliestTransferPlan.getTimestamp("scheduledTransferDatetime");
                }

                double transferQuantity = Math.min(quantityToTransfer.doubleValue(), maxTransferQuantity.doubleValue());

                // if not configured to always transfer, check the backup warehouse ATP and only transfer inventory up to the amount available in the backup warehouse
                if (!forceTransferWithNoInventory) {
                    // find out the inventory available in the backup facility and set the quantity to transfer to it
                    tmpResult = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("facilityId", backupFacilityId, "productId", productId, "userLogin", userLogin));
                    if (ServiceUtil.isError(tmpResult) || (tmpResult.get("availableToPromiseTotal") == null)) {
                        return BigDecimal.ZERO;  // not necessary to log -- service engine will log errors
                    }
                    double backupFacilityAtp = ((BigDecimal) tmpResult.get("availableToPromiseTotal")).doubleValue();
                    transferQuantity = Math.min(transferQuantity, backupFacilityAtp);

                    if (transferQuantity <= 0) {
                        Debug.logInfo("Nothing to transfer from [" + backupFacilityId + "] to [" + facilityId + "] for product [" + productId + "]: backup faciltiy atp = [" + backupFacilityAtp + "] max transfer quantity = [" + maxTransferQuantity + "] quantity to transfer = [" + quantityToTransfer + "] so transferQuantity = [" + transferQuantity + "]", MODULE);
                        continue; // try next warehouse
                    }
                }

                GenericValue fromFacility = delegator.findByPrimaryKeyCache("Facility", UtilMisc.toMap("facilityId", backupFacilityId));
                GenericValue toFacility = delegator.findByPrimaryKeyCache("Facility", UtilMisc.toMap("facilityId", facilityId));
                String transferFromEventDescription = null;
                String transferToEventDescription = null;

                if ((createTransferRequirements != null) && (createTransferRequirements.booleanValue())) {
                    Debug.logInfo("Creating transfer requirement is turned on", MODULE);
                    // if we're creating transfer requirements, then we don't need to check quantities, because the transfer requirement happens in the future
                    // and inventory might have changed by then
                    String requirementId = delegator.getNextSeqId("Requirement");
                    Map requirementValues = UtilMisc.toMap("requirementId", requirementId, "requirementTypeId", "TRANSFER_REQUIREMENT", "facilityId", backupFacilityId, "facilityIdTo", facilityId, "productId", productId, "statusId", "REQ_PROPOSED");
                    requirementValues.put("description", "Automatically generated by MRP");
                    requirementValues.put("requirementStartDate", transferTimestamp);
                    requirementValues.put("requiredByDate", transferTimestamp);
                    requirementValues.put("quantity", new BigDecimal(transferQuantity));
                    delegator.create("Requirement", requirementValues);

                    transferFromEventDescription = UtilMessage.expandLabel("PurchMrpTransferRequirementFromToAt", locale, UtilMisc.toMap("requirementId", requirementId, "fromFacilityName", fromFacility.getString("facilityName"), "toFacilityName", toFacility.getString("facilityName"), "transferTime", transferTimestamp));
                    transferToEventDescription = UtilMessage.expandLabel("PurchMrpTransferRequirementFromToAt", locale, UtilMisc.toMap("requirementId", requirementId, "fromFacilityName", fromFacility.getString("facilityName"), "toFacilityName", toFacility.getString("facilityName"), "transferTime", transferTimestamp));

                    // quantity "transferred" is the same as that created for the transfer requirement
                    quantityTransferred = new BigDecimal(transferQuantity).setScale(decimals + 1, defaultRoundingMode);
                } else {
                    Debug.logInfo("Creating transfer requirement is not turned on.  Will be creating inventory transfers directly", MODULE);
                    Debug.logInfo("facilityIdFrom [" + backupFacilityId + "] facilityIdTo [" + facilityId + "] productId [" + productId + "] sendDate [" + transferTimestamp + "] transferQuantity" + new BigDecimal(transferQuantity), MODULE);
                    // the inventory transfer is created from the backup facility to our MRP facility.  The quantity is the lower of remaining quantity to transfer and the maxTransferQuantity
                    // the transfer is scheduled to take place right before required event time stamp (see above)
                    Map inventoryTransferParams = UtilMisc.toMap("facilityIdFrom", backupFacilityId, "facilityIdTo", facilityId,
                            "productId", productId, "sendDate", transferTimestamp, "transferQuantity", new BigDecimal(transferQuantity), "userLogin", userLogin);
                    tmpResult = dispatcher.runSync("createInventoryTransferForFacilityProduct", inventoryTransferParams);
                    if (ServiceUtil.isError(tmpResult)) { return BigDecimal.ZERO; }  // not necessary to log -- service engine will log errors
                    // if the service returns a failure then it probably didn't transfer the inventory so keep on going
                    if (tmpResult.get("quantityTransferred") == null) {
                        Debug.logInfo("No quantity transferred for [" + productId + "] from [" + backupFacilityId + "] to [" + facilityId + "]", MODULE);
                        continue; // so we can try again with the next warehouse
                    }

                    BigDecimal quantityThisTransfer = (BigDecimal) tmpResult.get("quantityTransferred");
                    List inventoryTransferIds = (List) tmpResult.get("inventoryTransferIds");
                    quantityToTransfer = quantityToTransfer.subtract(quantityThisTransfer).setScale(decimals + 1, defaultRoundingMode);
                    quantityTransferred = quantityTransferred.add(quantityThisTransfer).setScale(decimals + 1, defaultRoundingMode);

                    transferFromEventDescription = UtilMessage.expandLabel("PurchMrpInventoryTransferFromToAt", locale, UtilMisc.toMap("inventoryTransferIds", inventoryTransferIds, "fromFacilityName", fromFacility.getString("facilityName"), "toFacilityName", toFacility.getString("facilityName"), "transferTime", transferTimestamp));
                    transferToEventDescription = UtilMessage.expandLabel("PurchMrpInventoryTransferFromToAt", locale, UtilMisc.toMap("inventoryTransferIds", inventoryTransferIds, "fromFacilityName", fromFacility.getString("facilityName"), "toFacilityName", toFacility.getString("facilityName"), "transferTime", transferTimestamp));

                }

                // create inventory events for other MRP planning.  Except for changes in description, it should be the same
                MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(UtilMisc.toMap("productId", productId, "eventDate", UtilCommon.beforeMillisecs(UtilCommon.laterOf(transferTimestamp, now), receiptEventBufferMilliseconds), "inventoryEventPlanTypeId", "PROP_INV_XFER_IN", "facilityId", facilityId),
                                                                          quantityTransferred, quantityTransferred.add(initialQoh), transferFromEventDescription, transferTimestamp.before(now), null, delegator);

                MrpInventoryEventServices.createOrUpdateMrpInventoryEvent(UtilMisc.toMap("productId", productId, "eventDate", UtilCommon.laterOf(transferTimestamp, now), "inventoryEventPlanTypeId", "PROP_INV_XFER_OUT", "facilityId", backupFacilityId),
                                                                          quantityTransferred.negate(), quantityTransferred.negate().add(initialQoh),
                                                                          transferToEventDescription, transferTimestamp.before(now), null, delegator);



            }
        }

        return quantityTransferred;
    }

    /**
     * Service <code>createInventoryTransferFromRequirement</code>, creates an Inventory Transfer from a requirement.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createInventoryTransferFromRequirement(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();

        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String requirementId = (String) context.get("requirementId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");

        GenericValue requirement = null;
        try {
            requirement = delegator.findByPrimaryKey("Requirement", UtilMisc.toMap("requirementId", requirementId));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "PurchError_RequirementNotExists", locale, MODULE);
        }

        if (requirement == null) {
            return UtilMessage.createAndLogServiceError("PurchError_RequirementNotExists", locale, MODULE);
        }

        // ignore non transfer requirements
        if (!"TRANSFER_REQUIREMENT".equals(requirement.getString("requirementTypeId"))) {
            return ServiceUtil.returnSuccess();
        }

        // get quantity from the requirement it if was overridden by the service parameter
        if (quantity == null) {
            quantity = requirement.getBigDecimal("quantity");
        }

        // get the transfer parameters from the requirement
        String facilityFromId = requirement.getString("facilityId");
        String facilityToId = requirement.getString("facilityIdTo");
        String productId = requirement.getString("productId");
        Timestamp transferTimestamp = requirement.getTimestamp("requirementStartDate");

        Map inventoryTransferParams = UtilMisc.toMap("facilityIdFrom", facilityFromId, "facilityIdTo", facilityToId, "productId", productId, "sendDate", transferTimestamp, "transferQuantity", quantity, "userLogin", userLogin);

        try {
            // create the transfer
            Debug.logInfo("Creating transfer [" + requirementId + "] ...", MODULE);
            Map tmpResult = dispatcher.runSync("createInventoryTransferForFacilityProduct", inventoryTransferParams);
            // the service can return error or failure, in which case we return its error message
            if (!UtilCommon.isSuccess(tmpResult)) {
                Debug.logError("createInventoryTransferForFacilityProduct error : " + tmpResult, MODULE);
                return UtilMessage.createAndLogServiceError(tmpResult, "PurchError_RequirementNotTransferred", locale, MODULE);
            }

            // set the requirement to closed
            Debug.logInfo("Closing requirement [" + requirementId + "] ...", MODULE);
            Map result2 = dispatcher.runSync("updateRequirement", UtilMisc.toMap("userLogin", userLogin, "requirementId", requirementId, "statusId", "REQ_CLOSED"));
            if (!UtilCommon.isSuccess(result2)) {
                return UtilMessage.createAndLogServiceError(tmpResult, "PurchError_RequirementNotTransferred", locale, MODULE);
            }

            // return the created transfer ID
            List inventoryTransferIds = (List) tmpResult.get("inventoryTransferIds");
            Map result = ServiceUtil.returnSuccess();
            result.put("inventoryTransferIds", inventoryTransferIds);
            return result;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "PurchError_RequirementNotTransferred", locale, MODULE);
        }

    }

    /**
     * Creates Production Runs from a list of Pending Internal Requirements.
     *
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    @SuppressWarnings("unchecked")
    public static Map createProductionRunsFromPendingInternalRequirements(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        List<String> requirementIds = (List<String>) context.get("requirementIds");
        Map<String, String> facilityIds = (Map<String, String>) context.get("facilityIds");
        Map<String, String> routingIds = (Map<String, String>) context.get("routingIds");

        // get the Requirement entity and group them by facilityId, productId and routing
        Map<String, Map<String, Map<String, List<GenericValue>>>> groupedRequirements = new HashMap<String, Map<String, Map<String, List<GenericValue>>>>();
        try {
            for (String requirementId : requirementIds) {
                GenericValue requirement = delegator.findByPrimaryKey("Requirement", UtilMisc.toMap("requirementId", requirementId));
                // check the requirement was found and is of the correct type
                if (requirement == null) {
                    return UtilMessage.createAndLogServiceError("PurchError_RequirementNotExists", locale, MODULE);
                }
                if (!"PENDING_INTERNAL_REQ".equals(requirement.get("requirementTypeId"))) {
                    return UtilMessage.createAndLogServiceError("PurchError_RequirementNotExpectedType", UtilMisc.toMap("requirementId", requirementId, "requirementTypeId", "Pending Internal Requirement"), locale, MODULE);
                }

                // facility can be given to the service as argument, else use the requirement facility
                String facilityId = requirement.getString("facilityId");
                if (UtilValidate.isNotEmpty(facilityIds) && facilityIds.containsKey(requirementId)) {
                    facilityId = facilityIds.get(requirementId);
                }

                String productId = requirement.getString("productId");

                // get the optional routing ID
                String routingId = null;
                if (UtilValidate.isNotEmpty(routingIds)) {
                    routingId = routingIds.get(requirementId);
                }

                Map<String, Map<String, List<GenericValue>>> productMap = groupedRequirements.get(facilityId);
                // initialize the product map if it does not exist yet
                if (productMap == null) {
                    productMap = new HashMap<String, Map<String, List<GenericValue>>>();
                    groupedRequirements.put(facilityId, productMap);
                }

                Map<String, List<GenericValue>> routingMap = productMap.get(productId);
                // initialize the routing map if it does not exist yet
                if (routingMap == null) {
                    routingMap = new HashMap<String, List<GenericValue>>();
                    productMap.put(productId, routingMap);
                }

                List<GenericValue> requirementList = routingMap.get(routingId);
                // initialize the requirement list if it does not exist yet
                if (requirementList == null) {
                    requirementList = new ArrayList<GenericValue>();
                    routingMap.put(routingId, requirementList);
                }

                // finally add this requirement to the list
                requirementList.add(requirement);

            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "PurchError_CreateProdRunFromPendingRequirementsFail", locale, MODULE);
        }

        // store the list of created production run Ids
        List<String> productionRunIds = new ArrayList<String>();

        // then for each facility / product / routing create a production run, effectively aggregating all the requirements of that product into the same production run
        for (String facilityId : groupedRequirements.keySet()) {
            Map<String, Map<String, List<GenericValue>>> productMap = groupedRequirements.get(facilityId);
            for (String productId : productMap.keySet()) {
                Map<String, List<GenericValue>> routingMap = productMap.get(productId);
                for (String routingId : routingMap.keySet()) {
                    List<GenericValue> requirementList = routingMap.get(routingId);
                    // get the total quantity to produce
                    BigDecimal totalToProduce = BigDecimal.ZERO;
                    // get the list of requirement Id so we can put it as reference in the production run description
                    List<String> requirementIdsToProduce = new ArrayList<String>();
                    // get the earliest start date from the aggregated requirements
                    Timestamp startDate = null;
                    for (GenericValue requirement : requirementList) {
                        totalToProduce = totalToProduce.add(requirement.getBigDecimal("quantity"));
                        requirementIdsToProduce.add(requirement.getString("requirementId"));
                        if (startDate == null) {
                            startDate = requirement.getTimestamp("requirementStartDate");
                        } else {
                            startDate = UtilCommon.earlierOf(startDate, requirement.getTimestamp("requirementStartDate"));
                        }
                    }

                    // prepare parameters
                    Map<String, Object> serviceContext = new HashMap<String, Object>();
                    serviceContext.put("routingId", routingId);
                    serviceContext.put("productId", productId);
                    serviceContext.put("facilityId", facilityId);
                    serviceContext.put("quantity", totalToProduce);
                    serviceContext.put("startDate", startDate);
                    String description;
                    // if there is only one requirement, use the same name as if it was created by createProductionRunFromRequirement
                    if (requirementIds.size() == 1) {
                        description = "Created from requirement " + requirementIdsToProduce.get(0);
                    } else {
                        description = "Aggregated from requirements " + requirementIdsToProduce;
                    }
                    serviceContext.put("description", description);

                    // make sure not to overflow the name field
                    String workEffortName = description;
                    if (workEffortName.length() > 50) {
                        workEffortName = workEffortName.substring(0, 50);
                    }
                    serviceContext.put("workEffortName", workEffortName);
                    serviceContext.put("userLogin", userLogin);

                    // create the production run
                    Map resultService = null;
                    try {
                        resultService = dispatcher.runSync("createProductionRunsForProductBom", serviceContext);
                        if (ServiceUtil.isError(resultService)) {
                            return UtilMessage.createAndLogServiceError(resultService, "PurchError_CreateProdRunFromPendingRequirementsFail", locale, MODULE);
                        }

                        // get the created production run id
                        productionRunIds.add((String) resultService.get("productionRunId"));

                        // set the requirements to Closed
                        for (GenericValue requirement : requirementList) {
                            Map statusResult = dispatcher.runSync("updateRequirement", UtilMisc.toMap("userLogin", userLogin, "requirementId", requirement.get("requirementId"), "statusId", "REQ_CLOSED"));
                            if (!UtilCommon.isSuccess(statusResult)) {
                                return UtilMessage.createAndLogServiceError(statusResult, "PurchError_CreateProdRunFromPendingRequirementsFail", locale, MODULE);
                            }
                        }
                    } catch (GenericServiceException e) {
                        return UtilMessage.createAndLogServiceError(e, "PurchError_CreateProdRunFromPendingRequirementsFail", locale, MODULE);
                    }
                }
            }
        }

        Map results = UtilMessage.createServiceSuccess("PurchSuccess_CreatedProductionRunsFromPendingRequirementsSuccess", locale);
        results.put("productionRunIds", productionRunIds);
        return results;
    }

    /**
     * Method finds minimal possible quantity of manufactured product
     * if initial requested quantity doesn't match any WEGS but there
     * are some WEGS for this product.
     *
     * @param qty initial quantity of product
     * @param productId product id to produce
     * @param delegator <code>Delegator</code> value
     * @return
     *     Requirement minimal quantity allowed for given product.
     * @throws GenericEntityException
     */
    private static BigDecimal ensureMinQuantity(BigDecimal qty, String productId, Delegator delegator) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "ROU_PROD_TEMPLATE"),
                EntityCondition.makeCondition("minQuantity", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("minQuantity", EntityOperator.GREATER_THAN, qty)
        );

        List<GenericValue> prodLinks = delegator.findByCondition(
                "WorkEffortGoodStandard",
                conditions,
                null, Arrays.asList("minQuantity ASC")
        );

        // iterate over all possible records skipping those
        // have null minQuantity.
        if (UtilValidate.isNotEmpty(prodLinks)) {
            BigDecimal minValue = null;
            for (GenericValue wegs : prodLinks) {
                BigDecimal minQuantity = wegs.getBigDecimal("minQuantity");
                if (minQuantity != null && (minValue == null || minQuantity.compareTo(minValue) < 0)) {
                    minValue = minQuantity;
                }
            }
            if (minValue != null) {
                // minimal quantity found
                return minValue;
            }
        }

        return qty;
    }

    /**
     * Method split product quantity into chunks so as each of them will be between WEGS minimal and max quantities.
     * Remainder is aligned to minimal quantity.
     *
     * @param qty Initial quantity.
     * @param productId a product to select appropriate WEGS
     * @param delegator delegator
     * @return list of quantities for every requirement
     * @throws GenericEntityException on error
     */
    private static List<BigDecimal> splitJob(BigDecimal qty, String productId, Delegator delegator)  throws GenericEntityException {
        // select WEGS for product which has max quantity in descending order
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "ROU_PROD_TEMPLATE"),
                EntityCondition.makeCondition("maxQuantity", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("maxQuantity", EntityOperator.LESS_THAN, qty)
        );

        List<GenericValue> prodLinks = delegator.findByCondition(
                "WorkEffortGoodStandard",
                conditions,
                null, Arrays.asList("maxQuantity DESC")
        );

        // no info for splitting, return null
        if (UtilValidate.isEmpty(prodLinks)) {
            return null;
        }

        BigDecimal remainingQuantity = qty;

        // last chunk should be aligned to minimal quantity, so we have to find its value.
        conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "ROU_PROD_TEMPLATE"),
                EntityCondition.makeCondition("minQuantity", EntityOperator.NOT_EQUAL, null)
        );

        List<GenericValue> prodLinksForMinQty = delegator.findByCondition(
                "WorkEffortGoodStandard",
                conditions,
                null, Arrays.asList("minQuantity ASC")
        );

        BigDecimal minQuantity = null;
        if (UtilValidate.isNotEmpty(prodLinksForMinQty)) {
            minQuantity = EntityUtil.getFirst(prodLinksForMinQty).getBigDecimal("minQuantity");
        }

        List<BigDecimal> result = FastList.newInstance();

        for (GenericValue wegs : prodLinks) {
            BigDecimal maxQuantity = wegs.getBigDecimal("maxQuantity");

            // for each max quantity as many chunks as possible
            while (remainingQuantity.compareTo(maxQuantity) >= 0) {
                if (remainingQuantity.compareTo(maxQuantity) > 0) {
                    result.add(maxQuantity);
                    remainingQuantity = remainingQuantity.subtract(maxQuantity);
                    continue;
                } else if (remainingQuantity.compareTo(minQuantity) >= 0) {
                    remainingQuantity = BigDecimal.ZERO;
                    result.add(remainingQuantity);
                    break;
                } else {
                    remainingQuantity = BigDecimal.ZERO;
                    result.add(minQuantity);
                    break;
                }
            }
        }

        // handle remainder
        if (remainingQuantity.signum() > 0) {
            result.add((minQuantity == null || remainingQuantity.compareTo(minQuantity) >= 0) ? remainingQuantity : minQuantity);
        }

        return UtilValidate.isNotEmpty(result) ? result : null;
    }
}
