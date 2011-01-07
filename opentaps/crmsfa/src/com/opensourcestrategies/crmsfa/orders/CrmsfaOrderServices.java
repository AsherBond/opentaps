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
/* Copyright (c) Open Source Strategies, Inc. */

package com.opensourcestrategies.crmsfa.orders;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.common.email.NotificationServices;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.order.UtilOrder;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.common.widget.screen.ScreenHelper;

/**
 * CRMSFA Order related services.
 */
public final class CrmsfaOrderServices {

    private CrmsfaOrderServices() { }

    private static final String MODULE = CrmsfaOrderServices.class.getName();
    private static final String RESOURCE = "crmsfa";

    public static Map<String, Object> getOrderPriorityList(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        Boolean onlyApprovedOrders = (Boolean) context.get("onlyApprovedOrders");
        String containsProductId = (String) context.get("containsProductId");

        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {

            List<Map<String, Object>> orderPriorityList = new ArrayList<Map<String, Object>>();

            List<EntityCondition> cond = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("orderId", EntityOperator.NOT_EQUAL, null),
                                                         EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"));
            if (onlyApprovedOrders != null && onlyApprovedOrders) {
                cond.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_APPROVED"));
            }

            // Find all records using the orderId != null expression
            EntityListIterator osgpit = delegator.findListIteratorByCondition("OrderShipGroupAndPriority", EntityCondition.makeCondition(cond, EntityOperator.AND), null, UtilMisc.toList("priorityValue", "orderId", "shipGroupSeqId"));

            // Construct a JSON object by making a map out of each GenericValue
            GenericValue gv = null;
            while ((gv = osgpit.next()) != null) {

                GenericValue orderItemShipGroup = gv.getRelatedOne("OrderItemShipGroup");

                if (UtilValidate.isNotEmpty(containsProductId)) {
                    List<GenericValue> orderItems = orderItemShipGroup.getRelated("OrderItemShipGroupAssoc");
                    boolean foundProduct = false;
                    for (GenericValue gv2 : orderItems) {
                        GenericValue orderItem = gv2.getRelatedOne("OrderItem");
                        if (containsProductId.equals(orderItem.getString("productId")) && ("ITEM_APPROVED".equals(orderItem.getString("statusId")) || "ITEM_CREATED".equals(orderItem.getString("statusId")))) {
                            foundProduct = true;
                            break;
                        }
                    }
                    if (!foundProduct) {
                        continue;
                    }

                }


                Map<String, Object> gvMap = gv.getAllFields();
                gvMap.putAll(orderItemShipGroup);

                if (UtilValidate.isNotEmpty(orderItemShipGroup.get("contactMechId"))) {
                    GenericValue contactMech = orderItemShipGroup.getRelatedOne("PostalAddress");
                    gvMap.put("address", contactMech.getAllFields());
                }

                String carrierPartyId = orderItemShipGroup.getString("carrierPartyId");
                if (UtilValidate.isNotEmpty(carrierPartyId)) {
                    String carrierPartyName = PartyHelper.getPartyName(delegator, carrierPartyId, false);
                    gvMap.put("carrierName", carrierPartyName);
                }

                if (UtilValidate.isNotEmpty(orderItemShipGroup.getString("shipmentMethodTypeId"))) {
                    GenericValue shipmentMethodType = orderItemShipGroup.getRelatedOne("ShipmentMethodType");
                    gvMap.put("shipmentMethodType", shipmentMethodType.getAllFields());
                }

                // Get the customer name and id
                GenericValue orderHeader = orderItemShipGroup.getRelatedOne("OrderHeader");
                String customerId = orderHeader.getString("billToPartyId");
                String customerName = PartyHelper.getPartyName(delegator, customerId, false);
                gvMap.put("customerId", customerId);
                gvMap.put("customerName", customerName);

                // Get the estimated ship date for the ship group
                Timestamp estShipDate = null;
                Map<String, Object> estShipDateContext = UtilMisc.toMap("orderId", orderItemShipGroup.get("orderId"), "shipGroupSeqId", orderItemShipGroup.get("shipGroupSeqId"), "userLogin", userLogin);
                Map<String, Object> estShipDateResult = dispatcher.runSync("getOrderItemShipGroupEstimatedShipDate", estShipDateContext);
                if (UtilValidate.isNotEmpty(estShipDateResult) && (!ServiceUtil.isError(estShipDateResult)) && estShipDateResult.containsKey("estimatedShipDate")) {
                    estShipDate = (Timestamp) estShipDateResult.get("estimatedShipDate");
                }
                gvMap.put("estimatedShipDate", estShipDate);

                // Get the status
                GenericValue statusItem = orderItemShipGroup.getRelatedOne("OrderHeader").getRelatedOneCache("StatusItem");
                gvMap.put("status", statusItem);

                // Get the back-ordered quantity from the inventory reservations
                BigDecimal backOrderedQuantity = BigDecimal.ZERO;
                List<GenericValue> oisgirs = orderItemShipGroup.getRelated("OrderItemShipGrpInvRes");
                if (UtilValidate.isNotEmpty(oisgirs)) {
                    Iterator<GenericValue> oisgirit = oisgirs.iterator();
                    while (oisgirit.hasNext()) {
                        GenericValue oisgir = oisgirit.next();
                        BigDecimal qtyNotAvailable = oisgir.getBigDecimal("quantityNotAvailable");
                        if (UtilValidate.isNotEmpty(qtyNotAvailable)) {
                            backOrderedQuantity = qtyNotAvailable;
                        }

                    }
                }
                gvMap.put("backOrderedQuantity", backOrderedQuantity);

                orderPriorityList.add(gvMap);
            }
            osgpit.close();

            result.put("orderPriorityList", orderPriorityList);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
        return result;
    }

    public static Map<String, Object> deleteOrderShipGroupPriority(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");

        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            delegator.removeByAnd("OrderShipGroupPriority", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
        return result;
    }

    public static Map<String, Object> createOrderShipGroupPriority(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");

        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {

            // Get the highest priorityValue in the OrderShipGroupPriority entity
            BigDecimal priorityValue = BigDecimal.ZERO;
            EntityFindOptions findOptions = new EntityFindOptions();
            findOptions.setMaxRows(1);
            GenericValue orderShipGroupPriority = EntityUtil.getFirst(delegator.findByCondition("OrderShipGroupPriority", EntityCondition.makeCondition("orderId", EntityOperator.NOT_EQUAL, null), null, null, UtilMisc.toList("-priorityValue"), findOptions));
            if (UtilValidate.isNotEmpty(orderShipGroupPriority)) {
                priorityValue = orderShipGroupPriority.getBigDecimal("priorityValue").add(BigDecimal.ONE);
            }

            // Get the ship groups for the order
            List<GenericValue> orderItemShipGroups = delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId), UtilMisc.toList("shipGroupSeqId"));
            List<String> orderShipGroups = EntityUtil.getFieldListFromEntityList(orderItemShipGroups, "shipGroupSeqId", true);

            if (UtilValidate.isEmpty(orderShipGroups)) {
                return result;
            }

            Iterator<String> sgit = orderShipGroups.iterator();
            while (sgit.hasNext()) {
                String shipGroupSeqId = sgit.next();

                GenericValue existingOrderShipGroupPriority = delegator.findByPrimaryKey("OrderShipGroupPriority", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));

                // Ignore ship groups that are already prioritized
                if (UtilValidate.isNotEmpty(existingOrderShipGroupPriority)) {
                    continue;
                }

                delegator.create("OrderShipGroupPriority", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId, "priorityValue", priorityValue));
                priorityValue = priorityValue.add(BigDecimal.ONE);
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
        return result;
    }

    public static Map<String, Object> rescheduleOrderShipDates(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);

        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String shipByDate = (String) context.get("shipByDate");

        if (UtilValidate.isEmpty(orderId) || UtilValidate.isEmpty(shipGroupSeqId) || UtilValidate.isEmpty(shipByDate)) {
            return ServiceUtil.returnSuccess();
        }

        try {
            GenericValue orderItemShipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
            Timestamp newShipByDate = UtilDate.toTimestamp(shipByDate, timeZone, locale);
            if (UtilValidate.isNotEmpty(orderItemShipGroup)) {
                orderItemShipGroup.set("shipByDate", newShipByDate);
                orderItemShipGroup.store();
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }


    public static Map<String, Object> resequenceOrderShipGroupPriorities(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);

        // These maps will look like {"0": "WS10000", "1":"WS10010"}
        Map<String, String> orderIds = (Map<String, String>) context.get("orderIds");
        Map<String, String> shipGroupSeqIds = (Map<String, String>) context.get("shipGroupSeqIds");
        Map<String, String> shipByDates = (Map<String, String>) context.get("shipByDates");

        if (UtilValidate.isEmpty(orderIds) || UtilValidate.isEmpty(shipGroupSeqIds)) {
            return ServiceUtil.returnSuccess();
        }

        List<GenericValue> toStore = new ArrayList<GenericValue>();

        try {

            // Get all the records in the OrderShipGroupPriority entity
            List<GenericValue> currentOrderShipGroupPriorities = delegator.findAll("OrderShipGroupPriority", UtilMisc.toList("priorityValue", "orderId", "shipGroupSeqId"));

            // Stepping through the orderIds map with a counter lets us retrieve the parameter values in the sequence that the user specified
            int newPriorityValue = 0;
            for (int x = 0; x < orderIds.size(); x++) {

                // Retrieve the orderId and shipGroupSeqId from the parameter maps (keys are strings)
                String orderId = orderIds.get("" + newPriorityValue);
                String shipGroupSeqId = shipGroupSeqIds.get("" + newPriorityValue);
                String shipByDate = shipByDates.get("" + newPriorityValue);

                if (UtilValidate.isEmpty(orderId) || UtilValidate.isEmpty(shipGroupSeqId)) {
                    continue;
                }

                // Check to see if the orderId/shipGroupId combination is still present in the OrderShipGroupPriority entity, in case the entity has been
                //  modified while the user was involved in reprioritizing. Ignore any orderId/shipGroupIds that no longer exist.
                List<GenericValue> currentPrioritiesForOrderShipGroup = EntityUtil.filterByAnd(currentOrderShipGroupPriorities, UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
                if (UtilValidate.isEmpty(currentPrioritiesForOrderShipGroup)) {
                    continue;
                }

                // For each found, set the new priorityValue to the current counter value
                Iterator<GenericValue> cpoit = currentPrioritiesForOrderShipGroup.iterator();
                while (cpoit.hasNext()) {
                    GenericValue cp = cpoit.next();
                    cp.set("priorityValue", new Double(newPriorityValue));
                    newPriorityValue++;
                }
                toStore.addAll(currentPrioritiesForOrderShipGroup);

                // Update the corresponding OrderItemShipGroup record with the new shipByDate, if necessary
                if (UtilValidate.isNotEmpty(shipByDate)) {
                    Timestamp newShipByDate = null;
                    newShipByDate = UtilDate.toTimestamp(shipByDate, timeZone, locale);
                    if (UtilValidate.isEmpty(newShipByDate)) {
                        Debug.logError("Invalid shipByDate [" + shipByDate + "] for orderId [" + orderId + "], shipGroupSeqId [" + shipGroupSeqId + "] in CrmsfaOrderServices.resequenceOrderShipGroupPriorities() - ignoring", MODULE);
                    } else {
                        newShipByDate = UtilDateTime.getDayEnd(newShipByDate, timeZone, locale);
                    }
                    if (UtilValidate.isNotEmpty(newShipByDate)) {
                        GenericValue orderItemShipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
                        if (UtilValidate.isNotEmpty(orderItemShipGroup)) {
                            orderItemShipGroup.set("shipByDate", newShipByDate);
                            orderItemShipGroup.store();
                        }
                    }
                }
            }

            // Destroy all existing records in OrderShipGroupPriority and populate it with the updated GenericValues
            delegator.removeByCondition("OrderShipGroupPriority", EntityCondition.makeCondition("orderId", EntityOperator.NOT_EQUAL, null));
            delegator.storeAll(toStore);

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }


    public static Map<String, Object> reReserveInventoryOnSalesOrderStatusChange(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        try {

            String orderId = (String) context.get("orderId");
            result.put("orderId", orderId);
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isEmpty(orderHeader) || !"SALES_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                Debug.logInfo("Not calling crmsfa.reReserveInventoryByOrderPriority: orderId " + orderId + " not found or not sales order", MODULE);
                return result;
            }

            boolean runSync = "true".equals(UtilProperties.getPropertyValue(RESOURCE, "crmsfa.order.reservations.rereserveSync", "true"));

            // Get all reservations against any inventory item for any product in the order
            List<EntityCondition> cond = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                EntityCondition.makeCondition("statusId", EntityOperator.IN, UtilMisc.toList("ITEM_CREATED", "ITEM_APPROVED"))
            );
            List<GenericValue> orderItems = delegator.findByConditionCache("OrderItem", EntityCondition.makeCondition(cond, EntityOperator.AND), UtilMisc.toList("productId"), null);
            List<String> productIds = EntityUtil.getFieldListFromEntityList(orderItems, "productId", true);
            cond = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("productId", EntityOperator.IN, productIds),
                                                    EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
            List<GenericValue> allReservations = delegator.findByCondition("OrderItemShipGrpInvResAndItem", EntityCondition.makeCondition(cond, EntityOperator.AND), null, null);

            // Cancel every reservation against any product present in the order
            for (GenericValue res : allReservations) {

                String resOrderId = res.getString("orderId");

                Map<String, Object> cancelReservationContext = UtilMisc.toMap("orderId", resOrderId, "orderItemSeqId", res.get("orderItemSeqId"), "inventoryItemId", res.get("inventoryItemId"), "shipGroupSeqId", res.get("shipGroupSeqId"), "userLogin", userLogin);
                Debug.logInfo("CANCEL reservation of product [" + res.get("productId") + "] for order [" + res.get("orderId") + "] on inventory [" + res.get("inventoryItemId") + "]", MODULE);
                if (runSync) {
                    Map<String, Object> cancelReservationResult = dispatcher.runSync("cancelOrderItemShipGrpInvRes", cancelReservationContext);
                    cancelReservationResult.put("orderId", orderId);
                    if (ServiceUtil.isError(cancelReservationResult)) {
                        return cancelReservationResult;
                    }
                } else {
                    dispatcher.runAsync("cancelOrderItemShipGrpInvRes", cancelReservationContext);
                }

                // re balance the inventory items
                Map<String, Object> balanceInventoryItemsContext = UtilMisc.toMap("inventoryItemId", res.get("inventoryItemId"), "userLogin", userLogin);
                Debug.logInfo("Balance inventory for product [" + res.get("productId") + "] on inventory [" + res.get("inventoryItemId") + "]", MODULE);
                if (runSync) {
                    Map<String, Object> balanceInventoryItemsResult = dispatcher.runSync("balanceInventoryItems", balanceInventoryItemsContext);
                    balanceInventoryItemsResult.put("orderId", orderId);
                    if (ServiceUtil.isError(balanceInventoryItemsResult)) {
                        return balanceInventoryItemsResult;
                    }
                } else {
                    dispatcher.runAsync("balanceInventoryItems", balanceInventoryItemsContext);
                }
            }

            // Rereserve the order reservations
            for (GenericValue res : allReservations) {
                Debug.logInfo("RE reservation of product [" + res.get("productId") + "] for changed order [" + res.get("orderId") + "]", MODULE);
                Map<String, Object> reserveContext = UtilMisc.toMap("orderId", res.get("orderId"), "orderItemSeqId", res.get("orderItemSeqId"), "quantity", res.get("quantity"), "shipGroupSeqId", res.get("shipGroupSeqId"), "userLogin", userLogin);
                reserveContext.put("reserveOrderEnumId", res.get("reserveOrderEnumId"));
                reserveContext.put("requireInventory", "N");
                reserveContext.put("reservedDatetime", UtilDateTime.nowTimestamp());
                reserveContext.put("productId", res.get("productId"));
                if (runSync) {
                    Map<String, Object> reserveResult = dispatcher.runSync("reserveProductInventory", reserveContext);
                    reserveResult.put("orderId", orderId);
                    if (ServiceUtil.isError(reserveResult)) {
                        return reserveResult;
                    }
                } else {
                    dispatcher.runAsync("reserveProductInventory", reserveContext);
                }
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
        return result;
    }

    public static Map<String, Object> reReserveInventoryByOrderPriority(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {

            // Get all currently existing inventory reservations
            List<GenericValue> previousReservations = delegator.findAll("OrderItemShipGrpInvRes", UtilMisc.toList("reservedDatetime"));
            Iterator<GenericValue> oisgirit = previousReservations.iterator();
            while (oisgirit.hasNext()) {
                GenericValue oisgir = oisgirit.next();

                // Cancel each reservation - this releases the reserved inventory
                Map<String, Object> cancelReservationContext = UtilMisc.toMap("orderId", oisgir.get("orderId"), "orderItemSeqId", oisgir.get("orderItemSeqId"), "inventoryItemId", oisgir.get("inventoryItemId"), "shipGroupSeqId", oisgir.get("shipGroupSeqId"), "userLogin", userLogin);
                Map<String, Object> cancelReservationResult = dispatcher.runSync("cancelOrderItemShipGrpInvRes", cancelReservationContext);
                if (ServiceUtil.isError(cancelReservationResult)) {
                    return cancelReservationResult;
                }
            }

            // Get all records from the OrderShipGroupPriority entity via the orderId != null condition, ordered by priorityValue/orderId/shipGroupId
            EntityListIterator osgpit = delegator.findListIteratorByCondition("OrderShipGroupPriority", EntityCondition.makeCondition("orderId", EntityOperator.NOT_EQUAL, null), null, UtilMisc.toList("priorityValue", "orderId", "shipGroupSeqId"));

            // Iterate through the records (in order of priority!)
            int sequenceId = 0;
            GenericValue gv = null;
            while ((gv = osgpit.next()) != null) {

                String orderId = gv.getString("orderId");
                String shipGroupSeqId = gv.getString("shipGroupSeqId");

                // Get the previous reservations for this order and shipGroup
                List<GenericValue> previousOrderReservations = EntityUtil.filterByAnd(previousReservations, UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
                if (UtilValidate.isEmpty(previousOrderReservations)) {
                    continue;
                }

                Iterator<GenericValue> porit = previousOrderReservations.iterator();
                while (porit.hasNext()) {
                    GenericValue prevRes = porit.next();

                    // For each previous reservation, re-reserve the same quantity
                    Map<String, Object> reserveContext = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", prevRes.get("orderItemSeqId"), "quantity", prevRes.get("quantity"), "shipGroupSeqId", shipGroupSeqId, "userLogin", userLogin);
                    reserveContext.put("reserveOrderEnumId", prevRes.get("reserveOrderEnumId"));
                    reserveContext.put("requireInventory", "N");

                    // Setting reservedDatetime to the current time ensures that reservations are created in priority sequence, so that later other services which
                    //  sort reservations by reservedDatetime will transparently allocate inventory to highest priority orders first
                    reserveContext.put("reservedDatetime", UtilDateTime.nowTimestamp());

                    // Setting sequenceId as a backup in case of reservedDatetime collisions
                    reserveContext.put("sequenceId", new Long(++sequenceId));

                    GenericValue inventoryItem = prevRes.getRelatedOneCache("InventoryItem");
                    reserveContext.put("productId", inventoryItem.get("productId"));

                    Map<String, Object> reserveResult = dispatcher.runSync("reserveProductInventory", reserveContext);
                    if (ServiceUtil.isError(reserveResult)) {
                        return reserveResult;
                    }

                    // Remove the re-reserved reservation from the main list
                    previousReservations.remove(prevRes);
                }
            }
            osgpit.close();

            // Any records left over in the list were ones that existed as reservations but weren't present in the OrderShipGroupPriority entity. If that was
            //  the case, they can't be very important, so they get re-reserved last.
            Iterator<GenericValue> lit = previousReservations.iterator();
            while (lit.hasNext()) {
                GenericValue prevRes = lit.next();

                Map<String, Object> reserveContext = UtilMisc.toMap("orderId", prevRes.get("orderId"), "orderItemSeqId", prevRes.get("orderItemSeqId"), "quantity", prevRes.get("quantity"), "shipGroupSeqId", prevRes.get("shipGroupSeqId"), "userLogin", userLogin);
                reserveContext.put("reserveOrderEnumId", prevRes.get("reserveOrderEnumId"));
                reserveContext.put("requireInventory", "N");
                reserveContext.put("reservedDatetime", UtilDateTime.nowTimestamp());
                reserveContext.put("sequenceId", new Long(++sequenceId));

                GenericValue inventoryItem = prevRes.getRelatedOneCache("InventoryItem");
                reserveContext.put("productId", inventoryItem.get("productId"));

                Map<String, Object> reserveResult = dispatcher.runSync("reserveProductInventory", reserveContext);
                if (ServiceUtil.isError(reserveResult)) {
                    return reserveResult;
                }
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createSalesOrderWithOneItem(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String billToPartyId = (String) context.get("billToPartyId");
        String productStoreId = (String) context.get("productStoreId");
        String productId = (String) context.get("productId");
        String paymentMethodId = (String) context.get("paymentMethodId");
        String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
        String currencyUomId = (String) context.get("currencyUomId");
        try {
            // check that userLogin has order create permission
            if (!(billToPartyId.equals(userLogin.get("partyId")) || dctx.getSecurity().hasEntityPermission("CRMSFA_ORDER", "_CREATE", userLogin))) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
            }

            GenericValue productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
            if (productStore == null) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductStoreNotFound", UtilMisc.toMap("productStoreId", productStoreId), locale, MODULE);
            }

            if (UtilValidate.isEmpty(currencyUomId)) {
                currencyUomId = productStore.getString("defaultCurrencyUomId");
            }

            GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            if (product == null) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductNotFound", UtilMisc.toMap("productId", productId), locale, MODULE);
            }

            GenericValue paymentMethod = null;
            if (UtilValidate.isNotEmpty(paymentMethodId)) {
            paymentMethod = delegator.findByPrimaryKey("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId));
                if (paymentMethod == null) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_PaymentMethodNotFound", UtilMisc.toMap("paymentMethodId", paymentMethodId), locale, MODULE);
                }
                paymentMethodTypeId = paymentMethod.getString("paymentMethodTypeId");
            }

            // if payment method is null, then the paymentMethodTypeId must be set, otherwise this is an error
            if (paymentMethod == null && UtilValidate.isEmpty(paymentMethodTypeId)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_MissingOrderPaymentMethod", locale, MODULE);
            }

            // get the price of the product
            Map<String, Object> results = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product, "currencyUomId", currencyUomId));
            if (ServiceUtil.isError(results)) {
                return results;
            }
            BigDecimal price = (BigDecimal) results.get("price");
            if (price == null || price.signum() <= 0) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductPriceNotFound", UtilMisc.toMap("productId", productId, "currencyUomId", currencyUomId), locale, MODULE);
            }

            String orderId = delegator.getNextSeqId("OrderHeader");
            String orderItemSeqId = UtilFormatOut.formatPaddedNumber(1, 5);
            String orderPaymentPreferenceId = delegator.getNextSeqId("OrderPaymentPreference");
            Timestamp now = UtilDateTime.nowTimestamp();

            GenericValue orderHeader = delegator.makeValue("OrderHeader");
            orderHeader.set("orderId", orderId);
            orderHeader.set("orderTypeId", "SALES_ORDER");
            orderHeader.set("orderDate", now);
            orderHeader.set("entryDate", now);
            orderHeader.set("statusId", "ORDER_CREATED");
            orderHeader.set("createdBy", userLogin.get("userLoginId"));
            orderHeader.set("currencyUom", currencyUomId);
            orderHeader.set("productStoreId", productStoreId);
            orderHeader.set("remainingSubTotal", price);
            orderHeader.set("grandTotal", price);
            orderHeader.set("billFromPartyId", productStore.get("payToPartyId"));
            orderHeader.set("billToPartyId", billToPartyId);
            orderHeader.create();

            GenericValue orderItem = delegator.makeValue("OrderItem");
            orderItem.set("orderId", orderId);
            orderItem.set("orderItemSeqId", orderItemSeqId);
            orderItem.set("orderItemTypeId", "PRODUCT_ORDER_ITEM");
            orderItem.set("productId", productId);
            orderItem.set("isPromo", "N");
            orderItem.set("quantity", BigDecimal.ONE);
            orderItem.set("unitPrice", price);
            orderItem.set("unitListPrice", price);
            orderItem.set("isModifiedPrice", "N");
            orderItem.set("itemDescription", product.getString("productName"));  // TODO: use ProductContentWrapper
            orderItem.set("statusId", "ITEM_CREATED");
            orderItem.create();

            GenericValue vendorRole = delegator.makeValue("OrderRole");
            vendorRole.set("orderId", orderId);
            vendorRole.set("partyId", productStore.get("payToPartyId"));
            vendorRole.set("roleTypeId", "BILL_FROM_VENDOR");
            vendorRole.create();

            Map<String, Object> input = UtilMisc.<String, Object>toMap("partyId", billToPartyId, "roleTypeId", "BILL_TO_CUSTOMER");
            GenericValue partyRole = delegator.findByPrimaryKey("PartyRole", input);
            if (partyRole == null) {
                partyRole = delegator.makeValue("PartyRole", input);
                partyRole.create();
            }
            GenericValue orderRole = delegator.makeValue("OrderRole");
            orderRole.set("orderId", orderId);
            orderRole.set("partyId", billToPartyId);
            orderRole.set("roleTypeId", "BILL_TO_CUSTOMER");
            orderRole.create();

            input = UtilMisc.<String, Object>toMap("partyId", billToPartyId, "roleTypeId", "PLACING_CUSTOMER");
            // TODO: use ensurePartyRole service instead for all the order roles and put into a new routine
            partyRole = delegator.findByPrimaryKey("PartyRole", input);
            if (partyRole == null) {
                partyRole = delegator.makeValue("PartyRole", input);
                partyRole.create();
            }
            orderRole = delegator.makeValue("OrderRole");
            orderRole.set("orderId", orderId);
            orderRole.set("partyId", billToPartyId);
            orderRole.set("roleTypeId", "PLACING_CUSTOMER");
            orderRole.create();

            // TODO: create END_USER_CUSTOMER

            GenericValue paymentPref = delegator.makeValue("OrderPaymentPreference");
            paymentPref.set("orderPaymentPreferenceId", orderPaymentPreferenceId);
            paymentPref.set("orderId", orderId);
            paymentPref.set("presentFlag", "N");
            paymentPref.set("overflowFlag", "N");
            paymentPref.set("statusId", "PAYMENT_NOT_AUTH");
            paymentPref.set("createdDate", now);
            paymentPref.set("createdByUserLogin", userLogin.get("userLoginId"));
            paymentPref.set("paymentMethodTypeId", paymentMethodTypeId);
            if (paymentMethod != null) {
                paymentPref.set("paymentMethodId", paymentMethodId);
            }
            paymentPref.create();

            results = ServiceUtil.returnSuccess();
            results.put("orderId", orderId);
            return results;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
    }

    public static Map<String, Object> invoiceAndCaptureOrder(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");
        try {
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            OrderReadHelper orh = new OrderReadHelper(orderHeader);

            // check that userLogin has order create permission
            GenericValue billToParty = orh.getBillToParty();
            if (billToParty == null) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }
            if (!(userLogin.get("partyId").equals(billToParty.get("partyId")) || dctx.getSecurity().hasEntityPermission("CRMSFA_ORDER", "_CREATE", userLogin))) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }
            // check that userLogin has order create permission
            if (!dctx.getSecurity().hasEntityPermission("CRMSFA_ORDER", "_CREATE", userLogin)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            BigDecimal amount = orh.getOrderOpenAmount();
            if (amount.signum() == 0) {
                return UtilMessage.createAndLogServiceError("CrmError_OrderHasNoValue", UtilMisc.toMap("orderId", orderId), locale, MODULE);
            }

            // for now we'll get the order payment pref as created in the createSalesOrderWithOneItem service
            GenericValue opp = EntityUtil.getFirst(delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_NOT_AUTH")));
            if (opp == null) {
                return UtilMessage.createAndLogServiceError("CrmError_MissingOrderPaymentPreference", UtilMisc.toMap("orderId", orderId), locale, MODULE);
            }

            // create an invoice
            Map<String, Object> results = dispatcher.runSync("createInvoiceForOrderAllItems", UtilMisc.toMap("userLogin", userLogin, "orderId", orderId));
            if (ServiceUtil.isError(results)) {
                return results;
            }
            String invoiceId = (String) results.get("invoiceId");

            // authorize the payment pref
            results = dispatcher.runSync("authOrderPaymentPreference", UtilMisc.toMap("userLogin", userLogin, "orderPaymentPreferenceId", opp.get("orderPaymentPreferenceId")));
            if (ServiceUtil.isError(results)) {
                return results;
            }

            // capture
            results = dispatcher.runSync("captureOrderPayments", UtilMisc.toMap("userLogin", userLogin, "orderId", orderId, "captureAmount", amount, "invoiceId", invoiceId));
            if (ServiceUtil.isError(results)) {
                return results;
            }

            // mark order as complete (this is done the direct way to avoid a system hang when running changeOrderStatus service)
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            orderHeader.set("statusId", "ORDER_COMPLETED");
            orderHeader.store();

            List<GenericValue> orderItems = orderHeader.getRelated("OrderItem");
            for (Iterator<GenericValue> iter = orderItems.iterator(); iter.hasNext();) {
                GenericValue item = iter.next();
                if ("ITEM_APPROVED".equals(item.get("statusId"))) {
                    item.set("statusId", "ITEM_COMPLETED");
                    item.store();
                }
            }

            results = ServiceUtil.returnSuccess();
            results.put("invoiceId", invoiceId);
            return results;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
    }

    public static Map<String, Object> addCreditCardToOrder(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // first verify that the CVV is entered and 3-4 digits
        String securityCode = (String) context.get("securityCode");
        if (securityCode != null) {
            securityCode = securityCode.trim();
            if (securityCode.length() < 3 || securityCode.length() > 4) {
                return UtilMessage.createServiceError("CrmError_InvalidCVV", locale);
            }
            try {
                Integer.parseInt(securityCode);
            } catch (NumberFormatException e) {
                return UtilMessage.createServiceError("CrmError_InvalidCVV", locale);
            }
        }

        String orderId = (String) context.get("orderId");
        BigDecimal amount = UtilCommon.asBigDecimal(context.get("amount"));
        String paymentMethodId = (String) context.get("paymentMethodId");
        try {
            // add payment method (using this instead of addPaymentMethodToOrder because it's less redundant)
            Map<String, Object> results = dispatcher.runSync("createOrderPaymentPreference", UtilMisc.toMap("userLogin", userLogin, "orderId", orderId, "paymentMethodTypeId", "CREDIT_CARD", "paymentMethodId", paymentMethodId, "maxAmount", amount));
            if (ServiceUtil.isError(results)) {
                return results;
            }
            String prefId = (String) results.get("orderPaymentPreferenceId");

            // save the security code directly because the updateOrderPaymentPrefernce does unwanted business logic
            if (securityCode != null) {
                GenericValue pref = delegator.findByPrimaryKey("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", prefId));
                pref.set("securityCode", securityCode);
                pref.store();
            }
            
            // authorize the payment pref
            results = dispatcher.runSync("authOrderPaymentPreference", UtilMisc.toMap("userLogin", userLogin, "orderPaymentPreferenceId", prefId));
            if (ServiceUtil.isError(results)) {
                return results;
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }


    public static Map<String, Object> prepareOrderConfirmationEmail(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");
        String sendTo = (String) context.get("sendTo");

        Map<String, Object> mailServiceInput = UtilMisc.<String, Object>toMap("orderId", orderId, "sendTo", sendTo);

        // Provide the correct order confirmation ProductStoreEmailSetting, if one exists
        GenericValue orderHeader = null;
        GenericValue productStoreEmailSetting = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        // set the baseUrl parameter, required by some email bodies
        NotificationServices.setBaseUrl(delegator, (String) context.get("webSiteId"), context);

        if (UtilValidate.isEmpty(orderHeader)) {
            return UtilMessage.createAndLogServiceError("CrmError_ConfirmationEmailWithoutOrder", locale, MODULE);
        }

        if (UtilValidate.isEmpty(orderHeader.getString("productStoreId"))) {
            return UtilMessage.createAndLogServiceError("CrmError_ConfirmationOrderWithoutProductStore", locale, MODULE);
        }

        if (UtilValidate.isEmpty(orderHeader.getString("webSiteId"))) {
            String webSiteId = (String) context.get("webSiteId");
            if (UtilValidate.isEmpty(webSiteId)) {
                webSiteId = "";
            }
            orderHeader.setString("webSiteId", webSiteId);
            try {
                orderHeader.store();
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, locale, MODULE);
            }
        }

        try {
            productStoreEmailSetting = delegator.findByPrimaryKeyCache("ProductStoreEmailSetting", UtilMisc.toMap("productStoreId", orderHeader.getString("productStoreId"), "emailType", "PRDS_ODR_CONFIRM"));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        if (UtilValidate.isNotEmpty(productStoreEmailSetting)) {
            String subjectPattern = productStoreEmailSetting.getString("subject");
            String subject = null;
            Map args = UtilMisc.toMap("orderId", orderId);
            if (UtilValidate.isNotEmpty(subjectPattern)) {
                subject = FlexibleStringExpander.expandString(subjectPattern, args);
            }
            mailServiceInput.put("subject", subject);

            String bodyScreen = productStoreEmailSetting.getString("bodyScreenLocation");
            String content = null;
            if (UtilValidate.isNotEmpty(bodyScreen)) {
                ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);
                context.put("uiLabelMap", uiLabelMap);
                try {
                    content = ScreenHelper.renderScreenLocationAsText(bodyScreen, dctx, context, UtilMisc.toMap("orderId", orderId, "baseUrl", context.get("baseUrl")));
                } catch (Exception e) {
                    return UtilMessage.createAndLogServiceError(e, MODULE);
                }
            }

            mailServiceInput.put("content", content);

            if ("N".equals(productStoreEmailSetting.getString("withAttachment"))) {
                mailServiceInput.put("skipAttachment", "Y");
            }
        }

        //create a pend sales order email by opentaps.prepareSalesOrderEmail service
        String serviceName = "opentaps.prepareSalesOrderEmail";
        try {
            ModelService service = dctx.getModelService(serviceName);
            Map<String, Object> input = service.makeValid(mailServiceInput, "IN");
            input.put("userLogin", userLogin);
            Map<String, Object> serviceResults = dispatcher.runSync(serviceName, input);
            return serviceResults;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }


    public static Map<String, Object> markServicesAsPerformed(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");

        // find the order item
        GenericValue orderItem;
        try {
            orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        // check the order item is a non physical item
        try {
            if (UtilOrder.isItemPhysical(orderItem)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductCannotBeMarkPerformed", UtilMisc.toMap("productId", orderItem.getString("productId")), locale, MODULE);
            }

            // change the order item status
            ModelService modelService = dispatcher.getDispatchContext().getModelService("changeOrderItemStatus");
            Map<String, Object> input = modelService.makeValid(context, "IN");
            input.put("userLogin", userLogin);
            input.put("statusId", "ITEM_PERFORMED");
            Map<String, Object> result = dispatcher.runSync("changeOrderItemStatus", input);
            return result;
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

    }

    public static Map<String, Object> createShipGroup(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // service arguments
        String orderId = (String) context.get("orderId");
        String contactMechId = (String) context.get("contactMechId");
        String shippingMethod = (String) context.get("shippingMethod");
        String maySplit = (String) context.get("maySplit");
        String isGift = (String) context.get("isGift");
        // service arguments for order items
        Map<String, String> orderIds = (Map<String, String>) context.get("orderIds");
        Map<String, String> orderItemSeqIds = (Map<String, String>) context.get("orderItemSeqIds");
        Map<String, String> shipGroupSeqIds = (Map<String, String>) context.get("shipGroupSeqIds");
        Map<String, String> qtiesToTransfer = (Map<String, String>) context.get("qtiesToTransfer");
        // service optional arguments
        String thirdPartyAccountNumber = (String) context.get("thirdPartyAccountNumber");
        String thirdPartyPostalCode = (String) context.get("thirdPartyPostalCode");
        String thirdPartyCountryCode = (String) context.get("thirdPartyCountryCode");
        String giftMessage = (String) context.get("giftMessage");
        String shippingInstructions = (String) context.get("shippingInstructions");
        Timestamp shipByDate = (Timestamp) context.get("shipByDate");

        // be sure to have a value
        if (!"Y".equals(maySplit)) {
            maySplit = "N";
        }
        if (!"Y".equals(isGift)) {
            isGift = "N";
        }

        // security permission, check that the user has order create permission
        if (!dctx.getSecurity().hasEntityPermission("CRMSFA_ORDER", "_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        // 0. Validation:
        //    - the order should not be COMPLETED or CANCELLED
        //    - the order items should not be COMPLETED, CANCELLED or REJECTED
        //    - the qtyToTransfer should be <= to the remaining qty for the given OrderItemShipGroupAssoc
        //    - the order items should NOT be related to a PicklistItem

        // get the order
        GenericValue orderHeader;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        // validate status is not COMPLETED or CANCELLED
        if ("ORDER_COMPLETED".equals(orderHeader.getString("statusId")) || "ORDER_CANCELLED".equals(orderHeader.getString("statusId"))) {
            // TODO: implement as a label
            return UtilMessage.createAndLogServiceError("Cannot create a new ship group for this order because it is already Cancelled or Completed", MODULE);
        }


        // build a list of order items to include as [ {'orderId' => orderId, 'orderItemSeqId' => orderItemSeqId, 'shipGroupSeqId' => shipGroupSeqId, 'qtyToTransfer' => qtyToTransfer}]
        List<Map<String, Object>> orderItems = new ArrayList<Map<String, Object>>();
        // validate:
        //  - orderId for the item is the same as the order orderId
        //  - qtyToTransfer is <= remaining (ordered - cancelled) for the OrderItemShipGroupAssoc
        //  - the order item is not in a picklist item
        //  - there is at least one item to transfer
        // - gets the related OrderItemShipGroupAssoc and OrderItemShipGrpInvRes
        Iterator<String> keyIt = orderIds.keySet().iterator();
        while (keyIt.hasNext()) {
            String key = keyIt.next();
            String itemOrderId = orderIds.get(key);
            String itemOrderItemSeqId = orderItemSeqIds.get(key);
            String itemShipGroupSeqId = shipGroupSeqIds.get(key);
            String itemQtyToTransferStr = qtiesToTransfer.get(key);
            BigDecimal itemQtyToTransfer;
            // empty means 0
            if (UtilValidate.isEmpty(itemQtyToTransferStr)) {
                itemQtyToTransfer = BigDecimal.ZERO;
            } else {
                itemQtyToTransfer = new BigDecimal(itemQtyToTransferStr);
            }
            // 0 qty, skip this item
            if (itemQtyToTransfer.signum() == 0) {
                continue;
            }
            // validate all parameters are present for this order item
            if (UtilValidate.isEmpty(itemOrderId)) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("Missing the order item orderId", MODULE);
            }
            if (UtilValidate.isEmpty(itemOrderItemSeqId)) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("Missing the order item orderItemSeqId", MODULE);
            }
            if (UtilValidate.isEmpty(itemShipGroupSeqId)) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("Missing the order item shipGroupSeqId", MODULE);
            }
            // a string to id the order item in the debug messages
            String itemId = itemOrderId + "/" + itemShipGroupSeqId + "/" + itemOrderItemSeqId;
            // check the item is belonging to the same order we are editing
            if (!orderId.equals(itemOrderId)) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("The order item [" + itemId + "] does not belong to this order [" + orderId + "]", MODULE);
            }
            // get the OrderItemShipGroupAssoc
            GenericValue orderItemShipGroupAssoc;
            try {
                orderItemShipGroupAssoc = delegator.findByPrimaryKey("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", itemOrderId, "orderItemSeqId", itemOrderItemSeqId, "shipGroupSeqId", itemShipGroupSeqId));
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, locale, MODULE);
            }
            if (orderItemShipGroupAssoc == null) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("the order item [" + itemId + "] does not have any related OrderItemShipGroupAssoc", MODULE);
            }
            BigDecimal qtyOrdered = orderItemShipGroupAssoc.getBigDecimal("quantity");
            BigDecimal qtyCancelled = orderItemShipGroupAssoc.getBigDecimal("cancelQuantity");
            if (qtyCancelled == null) {
                qtyCancelled = BigDecimal.ZERO;
            }
            BigDecimal qtyRemaining = qtyOrdered.subtract(qtyCancelled);
            if (itemQtyToTransfer.compareTo(qtyRemaining) > 0) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("Cannot transfer more than the remaining quantity [" + qtyRemaining + "] for this item [" + itemId + "]. (quantity given was [" + itemQtyToTransfer + "])", MODULE);
            }
            // check if the item is on a picklist
            try {
                List<GenericValue> picklistItems = delegator.findByAnd("PicklistItem", UtilMisc.toMap("orderId", itemOrderId, "orderItemSeqId", itemOrderItemSeqId, "shipGroupSeqId", itemShipGroupSeqId));
                if (UtilValidate.isNotEmpty(picklistItems)) {
                    // TODO: use label
                    return UtilMessage.createAndLogServiceError("The order item [" + itemId + "] already belongs to a Picklist, this operation is not supported.", MODULE);
                }
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, locale, MODULE);
            }

            // get the OrderItemShipGrpInvRes
            List<GenericValue> orderItemShipGrpInvRess;
            try {
                orderItemShipGrpInvRess = orderItemShipGroupAssoc.getRelated("OrderItemShipGrpInvRes");
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, locale, MODULE);
            }

            // all good, add the item to the list
            Map<String, Object> orderItem = UtilMisc.<String, Object>toMap("orderId", itemOrderId, "orderItemSeqId", itemOrderItemSeqId, "shipGroupSeqId", itemShipGroupSeqId);
            orderItem.put("itemId", itemId);
            orderItem.put("qtyToTransfer", itemQtyToTransfer);
            orderItem.put("orderItemShipGroupAssoc", orderItemShipGroupAssoc);
            orderItem.put("orderItemShipGrpInvRess", orderItemShipGrpInvRess);
            orderItems.add(orderItem);
        }

        // check that we have at least one item to transfer
        if (UtilValidate.isEmpty(orderItems)) {
            // TODO: use label
            return UtilMessage.createAndLogServiceError("No order items to transfer to the new ship group.", MODULE);
        }


        // keep track of entities to update or remove
        List<GenericValue> valuesToStore = new ArrayList<GenericValue>();
        List<GenericValue> valuesToRemove = new ArrayList<GenericValue>();

        // 1. create a ShipGroup
        // split the shipping method string, format is method@carrier
        String[] splited = shippingMethod.split("@");
        String shipmentMethodTypeId = splited[0];
        String carrierPartyId = splited[1];

        // -> create a OrderItemShipGroup entity
        // the shipGroupSeqId is expected to be a sequence (1, 2, 3, ...)
        String shipGroupSeqId;
        try {
            long shipGroupCount = delegator.findCountByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId));
            shipGroupSeqId = UtilFormatOut.formatPaddedNumber(shipGroupCount + 1, 5);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        GenericValue orderItemShipGroup = delegator.makeValue("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
        orderItemShipGroup.set("carrierPartyId", carrierPartyId);
        orderItemShipGroup.set("shipmentMethodTypeId", shipmentMethodTypeId);
        orderItemShipGroup.set("carrierRoleTypeId", "CARRIER");
        orderItemShipGroup.set("contactMechId", contactMechId);
        orderItemShipGroup.set("thirdPartyAccountNumber", thirdPartyAccountNumber);
        orderItemShipGroup.set("thirdPartyPostalCode", thirdPartyPostalCode);
        orderItemShipGroup.set("thirdPartyCountryGeoCode", thirdPartyCountryCode);
        orderItemShipGroup.set("maySplit", maySplit);
        orderItemShipGroup.set("isGift", isGift);
        if ("Y".equals(isGift)) {
            orderItemShipGroup.set("giftMessage", giftMessage);
        }
        orderItemShipGroup.set("shippingInstructions", shippingInstructions);
        orderItemShipGroup.set("shipByDate", shipByDate);
        valuesToStore.add(orderItemShipGroup);

        // create the OrderContactMech, it doesn't matter if the entry already exist in the database
        GenericValue orderContactMech = delegator.makeValue("OrderContactMech", UtilMisc.toMap("orderId", orderId, "contactMechPurposeTypeId", "SHIPPING_LOCATION", "contactMechId", contactMechId));
        valuesToStore.add(orderContactMech);

        // 2. update or remove the origin OrderItemShipGroupAssoc qty
        // -> decrease the qty byt the qtyToTransfer
        // -> update or remove the corresponding OrderItemShipGrpInvRes, decrease quantity and quantityNotAvailable to the new qty
        // -> create InventoryItemDetail to adjust atp
        // -> ItemIssuance ?
        // 3. create destination OrderItemShipGroupAssoc
        // -> associated to the new OrderItemShipGroup
        // -> set qty to qtyToTransfer
        // -> create a corresponding InventoryItemDetail with correct qohDif atpDiff associated to the new OrderItemShipGroup
        // -> create a corresponding OrderItemShipGrpInvRes then set the qty / qtyNotAvailable to this qty associated to the new OrderItemShipGroup
        //     use the same type and reserved date

        // track the set of shipGroupSeqId from which we removed assocs, we will check them later and cancell them if they have no more assoc
        Set<String> shipGroupRemovedAssoc = new HashSet<String>();

        for (Map<String, Object> oi : orderItems) {
            String itemId = (String) oi.get("itemId");
            // update or remove the original OrderItemShipGroupAssoc qty
            GenericValue assoc = (GenericValue) oi.get("orderItemShipGroupAssoc"); // not null from validation
            BigDecimal qtyToTransfer = (BigDecimal) oi.get("qtyToTransfer");
            BigDecimal quantity = (BigDecimal) assoc.get("quantity");
            quantity = quantity.subtract(qtyToTransfer); // positive from validation
            if (quantity.signum() == 0) {
                valuesToRemove.add(assoc);
                shipGroupRemovedAssoc.add(assoc.getString("shipGroupSeqId"));
            } else {
                assoc.set("quantity", quantity);
                valuesToStore.add(assoc);
            }

            // create the OrderItemShipGroupAssoc associated to the new OrderItemShipGroup
            GenericValue newOrderItemShipGroupAssoc = delegator.makeValue("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", assoc.get("orderItemSeqId"), "shipGroupSeqId", orderItemShipGroup.get("shipGroupSeqId")));
            newOrderItemShipGroupAssoc.put("quantity", qtyToTransfer);
            valuesToStore.add(newOrderItemShipGroupAssoc);

            // loop on and update the original OrderItemShipGrpInvRes qty
            BigDecimal resQuantityProcessed = BigDecimal.ZERO;
            List<GenericValue> orderItemShipGrpInvRess = (List<GenericValue>) oi.get("orderItemShipGrpInvRess");
            for (GenericValue reservation : orderItemShipGrpInvRess) {
                if (resQuantityProcessed.compareTo(qtyToTransfer) >= 0) {
                    break;
                }
                BigDecimal resQuantity = reservation.getBigDecimal("quantity");
                BigDecimal resQuantityNotAvailable = reservation.getBigDecimal("quantityNotAvailable");
                // how much quantity we can remove from this OrderItemShipGrpInvRes
                BigDecimal diffQty = qtyToTransfer.subtract(resQuantityProcessed).min(resQuantity.subtract(resQuantityProcessed));
                resQuantity = resQuantity.subtract(diffQty);
                BigDecimal diffQtyNotAvailable = null;
                if (resQuantityNotAvailable != null && resQuantityNotAvailable.signum() > 0) {
                    diffQtyNotAvailable = resQuantityNotAvailable.min(diffQty);
                    resQuantityNotAvailable = resQuantityNotAvailable.subtract(diffQtyNotAvailable);
                }

                // set updated values or remove the OrderItemShipGrpInvRes if both quantity and quantityNotAvailable are zero or null
                if (resQuantity.signum() == 0 && (resQuantityNotAvailable == null || resQuantityNotAvailable.signum() == 0)) {
                    valuesToRemove.add(reservation);
                } else {
                    reservation.set("quantity", resQuantity);
                    reservation.set("quantityNotAvailable", resQuantityNotAvailable);
                    valuesToStore.add(reservation);
                }

                // create new OrderItemShipGrpInvRes
                GenericValue newOrderItemShipGrpInvRes = delegator.makeValue("OrderItemShipGrpInvRes",  reservation);
                newOrderItemShipGrpInvRes.put("shipGroupSeqId", orderItemShipGroup.get("shipGroupSeqId"));
                newOrderItemShipGrpInvRes.put("quantity", diffQty);
                newOrderItemShipGrpInvRes.put("quantityNotAvailable", diffQtyNotAvailable);
                valuesToStore.add(newOrderItemShipGrpInvRes);

                // adjust the inventory item ATP by creating two new inventoryItemDetails
                // the first re add as ATP the quantity transfer to a IDD associated to the original ship group
                String inventoryItemDetailSeqId = delegator.getNextSeqId("InventoryItemDetail");
                GenericValue newInventoryItemDetail = delegator.makeValue("InventoryItemDetail", UtilMisc.toMap("orderId", orderId, "inventoryItemId", reservation.get("inventoryItemId")));
                newInventoryItemDetail.put("inventoryItemDetailSeqId", inventoryItemDetailSeqId);
                newInventoryItemDetail.put("availableToPromiseDiff", diffQty);
                newInventoryItemDetail.put("quantityOnHandDiff", new BigDecimal("0.0"));
                newInventoryItemDetail.put("shipGroupSeqId", reservation.get("shipGroupSeqId"));
                valuesToStore.add(newInventoryItemDetail);
                // the second removed that ATP  to a IDD associated to the new ship group
                inventoryItemDetailSeqId = delegator.getNextSeqId("InventoryItemDetail");
                newInventoryItemDetail = delegator.makeValue("InventoryItemDetail", UtilMisc.toMap("orderId", orderId, "inventoryItemId", reservation.get("inventoryItemId")));
                newInventoryItemDetail.put("inventoryItemDetailSeqId", inventoryItemDetailSeqId);
                newInventoryItemDetail.put("availableToPromiseDiff", diffQty.negate());
                newInventoryItemDetail.put("quantityOnHandDiff", BigDecimal.ZERO);
                newInventoryItemDetail.put("shipGroupSeqId", orderItemShipGroup.get("shipGroupSeqId"));
                valuesToStore.add(newInventoryItemDetail);

                // account the quantity we removed
                resQuantityProcessed = resQuantityProcessed.add(diffQty);
            }
            // if some quantity was unaccounted for, then something was wrong
            if (!resQuantityProcessed.equals(qtyToTransfer)) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("Error while updating OrderItemShipGrpInvRes quantities for item [" + itemId + "], qtyToTransfer was [" + qtyToTransfer + "] but could only remove [" + resQuantityProcessed + "]", MODULE);
            }
        }

        // update / remove the entities
        try {
            delegator.storeAll(valuesToStore);
            delegator.removeAll(valuesToRemove);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        // final step, check if the OISG still has any OISGassoc with quantity, else cancel it (necessary for picking later)
        valuesToStore.clear();
        try {
            for (String oisgSeqId : shipGroupRemovedAssoc) {
                long n = delegator.findCountByCondition("OrderItemShipGroupAssoc", EntityCondition.makeCondition(UtilMisc.toList(
                                         EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                                         EntityCondition.makeCondition("shipGroupSeqId", EntityOperator.EQUALS, oisgSeqId),
                                         EntityCondition.makeCondition("quantity", EntityOperator.GREATER_THAN, 0.0)), EntityOperator.AND), null);
                if (n == 0) {
                    GenericValue oisg = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", oisgSeqId));
                    oisg.put("statusId", "OISG_CANCELLED");
                    valuesToStore.add(oisg);
                }
            }
            // update them
            delegator.storeAll(valuesToStore);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

}
