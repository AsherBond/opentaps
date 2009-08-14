/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
/* Copyright (c) 2005-2006 Open Source Strategies, Inc. */

package com.opensourcestrategies.crmsfa.orders;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.*;

import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.common.email.NotificationServices;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
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
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev: 630 $
 */

public class CrmsfaOrderServices {

    public static final String module = CrmsfaOrderServices.class.getName();
    public static final String resource = "crmsfa";

    public static Map getOrderPriorityList(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        Boolean onlyApprovedOrders = (Boolean) context.get("onlyApprovedOrders");
        String containsProductId = (String) context.get("containsProductId");

        Map result = ServiceUtil.returnSuccess();
        try {

            List orderPriorityList = new ArrayList();
            
            List cond = UtilMisc.toList( new EntityExpr("orderId", EntityOperator.NOT_EQUAL, null),
                    new EntityExpr("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"));
            if (onlyApprovedOrders != null && onlyApprovedOrders) {
                cond.add( new EntityExpr("statusId", EntityOperator.EQUALS, "ORDER_APPROVED") );
            }

            // Find all records using the orderId != null expression
            EntityListIterator osgpit = delegator.findListIteratorByCondition("OrderShipGroupAndPriority", new EntityConditionList(cond, EntityOperator.AND), null, UtilMisc.toList("priorityValue", "orderId", "shipGroupSeqId"));

            // Construct a JSON object by making a map out of each GenericValue
            GenericValue gv = null;
            while ( (gv = (GenericValue) osgpit.next()) != null) {

                GenericValue orderItemShipGroup = gv.getRelatedOne("OrderItemShipGroup");
                
                if (UtilValidate.isNotEmpty(containsProductId)) {
                    List<GenericValue> orderItems = orderItemShipGroup.getRelated("OrderItemShipGroupAssoc");
                    boolean foundProduct = false;
                    for (GenericValue gv2 : orderItems) {
                        GenericValue orderItem = gv2.getRelatedOne("OrderItem");
                        if ( containsProductId.equals(orderItem.getString("productId")) && ("ITEM_APPROVED".equals(orderItem.getString("statusId")) || "ITEM_CREATED".equals(orderItem.getString("statusId"))) ) {
                            foundProduct = true;
                            break;
                        }
                    }
                    if (!foundProduct) {
                        continue;
                    }

                }


                Map gvMap = gv.getAllFields();
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

                if (! UtilValidate.isEmpty(orderItemShipGroup.getString("shipmentMethodTypeId"))) {
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
                Map estShipDateContext = UtilMisc.toMap("orderId", orderItemShipGroup.get("orderId"), "shipGroupSeqId", orderItemShipGroup.get("shipGroupSeqId"), "userLogin", userLogin);
                Map estShipDateResult = dispatcher.runSync("getOrderItemShipGroupEstimatedShipDate", estShipDateContext);
                if (UtilValidate.isNotEmpty(estShipDateResult) && (! ServiceUtil.isError(estShipDateResult)) && estShipDateResult.containsKey("estimatedShipDate")) {
                    estShipDate = (Timestamp) estShipDateResult.get("estimatedShipDate");
                }
                gvMap.put("estimatedShipDate", estShipDate);

                // Get the status
                GenericValue statusItem = orderItemShipGroup.getRelatedOne("OrderHeader").getRelatedOneCache("StatusItem");
                gvMap.put("status", statusItem);

                // Get the back-ordered quantity from the inventory reservations
                double backOrderedQuantity = 0;
                List oisgirs = orderItemShipGroup.getRelated("OrderItemShipGrpInvRes");
                if (UtilValidate.isNotEmpty(oisgirs)) {
                    Iterator oisgirit = oisgirs.iterator();
                    while (oisgirit.hasNext()) {
                        GenericValue oisgir = (GenericValue) oisgirit.next();
                        Double qtyNotAvailable = oisgir.getDouble("quantityNotAvailable");
                        if (UtilValidate.isNotEmpty(qtyNotAvailable)) {
                            backOrderedQuantity += qtyNotAvailable.doubleValue();
                        }

                    }
                }
                gvMap.put("backOrderedQuantity", new Double(backOrderedQuantity));

                orderPriorityList.add(gvMap);
            }
            osgpit.close();

            result.put("orderPriorityList", orderPriorityList);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }
        return result;
    }

    public static Map deleteOrderShipGroupPriority(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");

        Map result = ServiceUtil.returnSuccess();
        try {
            delegator.removeByAnd("OrderShipGroupPriority", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }
        return result;
    }

    public static Map createOrderShipGroupPriority(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");

        Map result = ServiceUtil.returnSuccess();
        try {

            // Get the highest priorityValue in the OrderShipGroupPriority entity
            double priorityValue = 0;
            EntityFindOptions findOptions = new EntityFindOptions();
            findOptions.setMaxRows(1);
            GenericValue orderShipGroupPriority = EntityUtil.getFirst(delegator.findByCondition("OrderShipGroupPriority", new EntityExpr("orderId", EntityOperator.NOT_EQUAL, null), null, null, UtilMisc.toList("-priorityValue"), findOptions));
            if (! UtilValidate.isEmpty(orderShipGroupPriority)) {
                priorityValue = orderShipGroupPriority.getDouble("priorityValue").doubleValue() + 1;
            }

            // Get the ship groups for the order
            List orderItemShipGroups = delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId), UtilMisc.toList("shipGroupSeqId"));
            List orderShipGroups = EntityUtil.getFieldListFromEntityList(orderItemShipGroups, "shipGroupSeqId", true);

            if (UtilValidate.isEmpty(orderShipGroups)) {
                return result;
            }

            Iterator sgit = orderShipGroups.iterator();
            while (sgit.hasNext()) {
                String shipGroupSeqId = (String) sgit.next();

                GenericValue existingOrderShipGroupPriority = delegator.findByPrimaryKey("OrderShipGroupPriority", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));

                // Ignore ship groups that are already prioritized
                if (! UtilValidate.isEmpty(existingOrderShipGroupPriority)) {
                    continue;
                }

                delegator.create("OrderShipGroupPriority", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId, "priorityValue", new Double(priorityValue)));
                priorityValue++;
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }
        return result;
    }

    public static Map rescheduleOrderShipDates(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
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
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }
        return ServiceUtil.returnSuccess();
    }


    public static Map resequenceOrderShipGroupPriorities(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);

        // These maps will look like {"0": "WS10000", "1":"WS10010"}
        Map orderIds = (Map) context.get("orderIds");
        Map shipGroupSeqIds = (Map) context.get("shipGroupSeqIds");
        Map shipByDates = (Map) context.get("shipByDates");

        if (UtilValidate.isEmpty(orderIds) || UtilValidate.isEmpty(shipGroupSeqIds)) {
            return ServiceUtil.returnSuccess();
        }

        List toStore = new ArrayList();


        try {

            // Get all the records in the OrderShipGroupPriority entity
            List currentOrderShipGroupPriorities = delegator.findAll("OrderShipGroupPriority", UtilMisc.toList("priorityValue", "orderId", "shipGroupSeqId"));

            // Stepping through the orderIds map with a counter lets us retrieve the parameter values in the sequence that the user specified
            int newPriorityValue = 0;
            for (int x = 0; x < orderIds.size(); x++) {

                // Retrieve the orderId and shipGroupSeqId from the parameter maps (keys are strings)
                String orderId = (String) orderIds.get("" + newPriorityValue);
                String shipGroupSeqId = (String) shipGroupSeqIds.get("" + newPriorityValue);
                String shipByDate = (String) shipByDates.get("" + newPriorityValue);

                if (UtilValidate.isEmpty(orderId) || UtilValidate.isEmpty(shipGroupSeqId)) continue;

                // Check to see if the orderId/shipGroupId combination is still present in the OrderShipGroupPriority entity, in case the entity has been
                //  modified while the user was involved in reprioritizing. Ignore any orderId/shipGroupIds that no longer exist.
                List currentPrioritiesForOrderShipGroup = EntityUtil.filterByAnd(currentOrderShipGroupPriorities, UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
                if (UtilValidate.isEmpty(currentPrioritiesForOrderShipGroup)) continue;

                // For each found, set the new priorityValue to the current counter value
                Iterator cpoit = currentPrioritiesForOrderShipGroup.iterator();
                while (cpoit.hasNext()) {
                    GenericValue cp = (GenericValue) cpoit.next();
                    cp.set("priorityValue", new Double(newPriorityValue));
                    newPriorityValue++;
                }
                toStore.addAll(currentPrioritiesForOrderShipGroup);

                // Update the corresponding OrderItemShipGroup record with the new shipByDate, if necessary
                if (UtilValidate.isNotEmpty(shipByDate)) {
                    Timestamp newShipByDate = null;
                    newShipByDate = UtilDate.toTimestamp(shipByDate, timeZone, locale);
                    if (UtilValidate.isEmpty(newShipByDate)) {
                        Debug.logError("Invalid shipByDate [" + shipByDate + "] for orderId [" + orderId + "], shipGroupSeqId [" + shipGroupSeqId + "] in CrmsfaOrderServices.resequenceOrderShipGroupPriorities() - ignoring", module);
                    } else {
                        newShipByDate = UtilDateTime.getDayEnd(newShipByDate, timeZone, locale);
                    }
                    if (! UtilValidate.isEmpty(newShipByDate)) {
                        GenericValue orderItemShipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
                        if (! UtilValidate.isEmpty(orderItemShipGroup)) {
                            orderItemShipGroup.set("shipByDate", newShipByDate);
                            orderItemShipGroup.store();
                        }
                    }
                }
            }

            // Destroy all existing records in OrderShipGroupPriority and populate it with the updated GenericValues
            delegator.removeByCondition("OrderShipGroupPriority", new EntityExpr("orderId", EntityOperator.NOT_EQUAL, null));
            delegator.storeAll(toStore);

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }
        return ServiceUtil.returnSuccess();
    }


    public static Map reReserveInventoryOnSalesOrderStatusChange(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        Map result = ServiceUtil.returnSuccess();

        try {

            String orderId = (String) context.get("orderId");
            result.put("orderId", orderId);
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isEmpty(orderHeader) || ! "SALES_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                Debug.logInfo("Not calling crmsfa.reReserveInventoryByOrderPriority: orderId " + orderId + " not found or not sales order", module);
                return result;
            }

            boolean runSync = "true".equals(UtilProperties.getPropertyValue(resource, "crmsfa.order.reservations.rereserveSync", "true"));

            // Get all reservations against any inventory item for any product in the order
            List cond = UtilMisc.toList(
                new EntityExpr("orderId", EntityOperator.EQUALS, orderId),
                new EntityExpr("statusId", EntityOperator.IN, UtilMisc.toList("ITEM_CREATED", "ITEM_APPROVED"))
            );
            List orderItems = delegator.findByConditionCache("OrderItem", new EntityConditionList(cond, EntityOperator.AND), UtilMisc.toList("productId"), null);
            List productIds = EntityUtil.getFieldListFromEntityList(orderItems, "productId", true);
            cond = UtilMisc.toList(new EntityExpr("productId", EntityOperator.IN, productIds));
            EntityListIterator allReservations = delegator.findListIteratorByCondition("OrderItemShipGrpInvResAndItem", new EntityConditionList(cond, EntityOperator.AND), null, null);
            List<GenericValue> orderResvs = new ArrayList<GenericValue>();
            GenericValue res;
            while ((res = (GenericValue) allReservations.next()) != null) {

                // Add the reservation to the list of reservations for the order, if the orderId matches
                String resOrderId = res.getString("orderId");
                if (orderId.equals(resOrderId)) orderResvs.add(res);

                // Cancel every reservation against any product present in the order
                Map cancelReservationContext = UtilMisc.toMap("orderId", resOrderId, "orderItemSeqId", res.get("orderItemSeqId"), "inventoryItemId", res.get("inventoryItemId"), "shipGroupSeqId", res.get("shipGroupSeqId"), "userLogin", userLogin);
                if (runSync) {
                    Map cancelReservationResult = dispatcher.runSync("cancelOrderItemShipGrpInvRes", cancelReservationContext);
                    cancelReservationResult.put("orderId", orderId);
                    if (ServiceUtil.isError(cancelReservationResult)) return cancelReservationResult;
                } else {
                    dispatcher.runAsync("cancelOrderItemShipGrpInvRes", cancelReservationContext);
                }

                // Only rereserve if the reservation is for another order - we'll rereserve the order reservations last
                if (! orderId.equals(resOrderId)) {
                    Map reserveContext = UtilMisc.toMap("orderId", resOrderId, "orderItemSeqId", res.get("orderItemSeqId"), "quantity", res.get("quantity"), "shipGroupSeqId", res.get("shipGroupSeqId"), "userLogin", userLogin);
                    reserveContext.put("reserveOrderEnumId", res.get("reserveOrderEnumId"));
                    reserveContext.put("requireInventory", "N");
                    reserveContext.put("reservedDatetime", UtilDateTime.nowTimestamp());
                    reserveContext.put("productId", res.get("productId"));
                    if (runSync) {
                        Map reserveResult = dispatcher.runSync("reserveProductInventory", reserveContext);
                        reserveResult.put("orderId", orderId);
                        if (ServiceUtil.isError(reserveResult)) return reserveResult;
                    } else {
                        dispatcher.runAsync("reserveProductInventory", reserveContext);
                    }
                }
            }
            allReservations.close();

            // Rereserve the order reservations last
            for (GenericValue orderRes : orderResvs) {
                Map reserveContext = UtilMisc.toMap("orderId", orderRes.get("orderId"), "orderItemSeqId", orderRes.get("orderItemSeqId"), "quantity", orderRes.get("quantity"), "shipGroupSeqId", orderRes.get("shipGroupSeqId"), "userLogin", userLogin);
                reserveContext.put("reserveOrderEnumId", orderRes.get("reserveOrderEnumId"));
                reserveContext.put("requireInventory", "N");
                reserveContext.put("reservedDatetime", UtilDateTime.nowTimestamp());
                reserveContext.put("productId", orderRes.get("productId"));
                if (runSync) {
                    Map reserveResult = dispatcher.runSync("reserveProductInventory", reserveContext);
                    if (ServiceUtil.isError(reserveResult)) return reserveResult;
                } else {
                    dispatcher.runAsync("reserveProductInventory", reserveContext);
                }
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }
        return result;
    }

    public static Map reReserveInventoryByOrderPriority(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {

            // Get all currently existing inventory reservations
            List previousReservations = delegator.findAll("OrderItemShipGrpInvRes", UtilMisc.toList("reservedDatetime"));
            Iterator oisgirit = previousReservations.iterator();
            while (oisgirit.hasNext()) {
                GenericValue oisgir = (GenericValue) oisgirit.next();

                // Cancel each reservation - this releases the reserved inventory
                Map cancelReservationContext = UtilMisc.toMap("orderId", oisgir.get("orderId"), "orderItemSeqId", oisgir.get("orderItemSeqId"), "inventoryItemId", oisgir.get("inventoryItemId"), "shipGroupSeqId", oisgir.get("shipGroupSeqId"), "userLogin", userLogin);
                Map cancelReservationResult = dispatcher.runSync("cancelOrderItemShipGrpInvRes", cancelReservationContext);
                if (ServiceUtil.isError(cancelReservationResult)) {
                    return cancelReservationResult;
                }
            }

            // Get all records from the OrderShipGroupPriority entity via the orderId != null condition, ordered by priorityValue/orderId/shipGroupId
            EntityListIterator osgpit = delegator.findListIteratorByCondition("OrderShipGroupPriority", new EntityExpr("orderId", EntityOperator.NOT_EQUAL, null), null, UtilMisc.toList("priorityValue", "orderId", "shipGroupSeqId"));

            // Iterate through the records (in order of priority!)
            int sequenceId = 0;
            GenericValue gv = null;
            while ( (gv = (GenericValue) osgpit.next()) != null) {

                String orderId = gv.getString("orderId");
                String shipGroupSeqId = gv.getString("shipGroupSeqId");

                // Get the previous reservations for this order and shipGroup
                List previousOrderReservations = EntityUtil.filterByAnd(previousReservations, UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
                if (UtilValidate.isEmpty(previousOrderReservations)) continue;

                Iterator porit = previousOrderReservations.iterator();
                while (porit.hasNext()) {
                    GenericValue prevRes = (GenericValue) porit.next();

                    // For each previous reservation, re-reserve the same quantity
                    Map reserveContext = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", prevRes.get("orderItemSeqId"), "quantity", prevRes.get("quantity"), "shipGroupSeqId", shipGroupSeqId, "userLogin", userLogin);
                    reserveContext.put("reserveOrderEnumId", prevRes.get("reserveOrderEnumId"));
                    reserveContext.put("requireInventory", "N");

                    // Setting reservedDatetime to the current time ensures that reservations are created in priority sequence, so that later other services which
                    //  sort reservations by reservedDatetime will transparently allocate inventory to highest priority orders first
                    reserveContext.put("reservedDatetime", UtilDateTime.nowTimestamp());

                    // Setting sequenceId as a backup in case of reservedDatetime collisions
                    reserveContext.put("sequenceId", new Long(++sequenceId));

                    GenericValue inventoryItem = prevRes.getRelatedOneCache("InventoryItem");
                    reserveContext.put("productId", inventoryItem.get("productId"));

                    Map reserveResult = dispatcher.runSync("reserveProductInventory", reserveContext);
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
            Iterator lit = previousReservations.iterator();
            while (lit.hasNext()) {
                GenericValue prevRes = (GenericValue) lit.next();

                Map reserveContext = UtilMisc.toMap("orderId", prevRes.get("orderId"), "orderItemSeqId", prevRes.get("orderItemSeqId"), "quantity", prevRes.get("quantity"), "shipGroupSeqId", prevRes.get("shipGroupSeqId"), "userLogin", userLogin);
                reserveContext.put("reserveOrderEnumId", prevRes.get("reserveOrderEnumId"));
                reserveContext.put("requireInventory", "N");
                reserveContext.put("reservedDatetime", UtilDateTime.nowTimestamp());
                reserveContext.put("sequenceId", new Long(++sequenceId));

                GenericValue inventoryItem = prevRes.getRelatedOneCache("InventoryItem");
                reserveContext.put("productId", inventoryItem.get("productId"));

                Map reserveResult = dispatcher.runSync("reserveProductInventory", reserveContext);
                if (ServiceUtil.isError(reserveResult)) {
                    return reserveResult;
                }
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map createSalesOrderWithOneItem(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
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
            if (! (billToPartyId.equals(userLogin.get("partyId")) || dctx.getSecurity().hasEntityPermission("CRMSFA_ORDER", "_CREATE", userLogin))) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, module);
            }

            GenericValue productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
            if (productStore == null) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductStoreNotFound", UtilMisc.toMap("productStoreId", productStoreId), locale, module);
            }

            if (UtilValidate.isEmpty(currencyUomId)) {
                currencyUomId = productStore.getString("defaultCurrencyUomId");
            }

            GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            if (product == null) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductNotFound", UtilMisc.toMap("productId", productId), locale, module);
            }

            GenericValue paymentMethod = null;
            if (UtilValidate.isNotEmpty(paymentMethodId)) {
            paymentMethod = delegator.findByPrimaryKey("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId));
                if (paymentMethod == null) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_PaymentMethodNotFound", UtilMisc.toMap("paymentMethodId", paymentMethodId), locale, module);
                }
                paymentMethodTypeId = paymentMethod.getString("paymentMethodTypeId");
            }

            // if payment method is null, then the paymentMethodTypeId must be set, otherwise this is an error
            if (paymentMethod == null && UtilValidate.isEmpty(paymentMethodTypeId)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_MissingOrderPaymentMethod", locale, module);
            }

            // get the price of the producct
            Map results = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product, "currencyUomId", currencyUomId));
            if (ServiceUtil.isError(results)) return results;
            Double price = (Double) results.get("price");
            if (price == null || price.doubleValue() <= 0) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductPriceNotFound", UtilMisc.toMap("productId", productId, "currencyUomId", currencyUomId), locale, module);
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
            orderItem.set("quantity", new Double(1));
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

            Map input = UtilMisc.toMap("partyId", billToPartyId, "roleTypeId", "BILL_TO_CUSTOMER");
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

            input = UtilMisc.toMap("partyId", billToPartyId, "roleTypeId", "PLACING_CUSTOMER");
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
            return UtilMessage.createAndLogServiceError(e, locale, module );
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }
    }

    public static Map invoiceAndCaptureOrder(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");
        try {
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            OrderReadHelper orh = new OrderReadHelper(orderHeader);

            // check that userLogin has order create permission
            GenericValue billToParty = orh.getBillToParty();
            if (billToParty == null) return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
            if (! (userLogin.get("partyId").equals(billToParty.get("partyId")) || dctx.getSecurity().hasEntityPermission("CRMSFA_ORDER", "_CREATE", userLogin))) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
            }
            // check that userLogin has order create permission
            if (!dctx.getSecurity().hasEntityPermission("CRMSFA_ORDER", "_CREATE", userLogin)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
            }

            double amount = orh.getOrderOpenAmount().doubleValue();
            if (amount == 0) {
                return UtilMessage.createAndLogServiceError("CrmError_OrderHasNoValue", UtilMisc.toMap("orderId", orderId), locale, module);
            }

            // for now we'll get the order payment pref as created in the createSalesOrderWithOneItem service
            GenericValue opp = EntityUtil.getFirst( delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_NOT_AUTH")) );
            if (opp == null) {
                return UtilMessage.createAndLogServiceError("CrmError_MissingOrderPaymentPreference", UtilMisc.toMap("orderId", orderId), locale, module);
            }

            // create an invoice
            Map results = dispatcher.runSync("createInvoiceForOrderAllItems", UtilMisc.toMap("userLogin", userLogin, "orderId", orderId));
            if (ServiceUtil.isError(results)) return results;
            String invoiceId = (String) results.get("invoiceId");

            // authorize the payment pref
            results = dispatcher.runSync("authOrderPaymentPreference", UtilMisc.toMap("userLogin", userLogin, "orderPaymentPreferenceId", opp.get("orderPaymentPreferenceId")));
            if (ServiceUtil.isError(results)) return results;

            // capture
            results = dispatcher.runSync("captureOrderPayments", UtilMisc.toMap("userLogin", userLogin, "orderId", orderId, "captureAmount", new Double(amount), "invoiceId", invoiceId));
            if (ServiceUtil.isError(results)) return results;

            // mark order as complete (this is done the direct way to avoid a system hang when running changeOrderStatus service)
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            orderHeader.set("statusId", "ORDER_COMPLETED");
            orderHeader.store();

            List orderItems = orderHeader.getRelated("OrderItem");
            for (Iterator iter = orderItems.iterator(); iter.hasNext(); ) {
                GenericValue item = (GenericValue) iter.next();
                if ("ITEM_APPROVED".equals(item.get("statusId"))) {
                    item.set("statusId", "ITEM_COMPLETED");
                    item.store();
                }
            }

            results = ServiceUtil.returnSuccess();
            results.put("invoiceId", invoiceId);
            return results;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }
    }

    public static Map addCreditCardToOrder(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // first verify that the CVV is entered and 3-4 digits
        String securityCode = (String) context.get("securityCode");
        securityCode = securityCode.trim();
        if (securityCode.length() < 3 || securityCode.length() > 4) {
            return UtilMessage.createServiceError("CrmError_InvalidCVV", locale);
        }
        try {
            Integer.parseInt(securityCode);
        } catch (NumberFormatException e) {
            return UtilMessage.createServiceError("CrmError_InvalidCVV", locale);
        }

        String orderId = (String) context.get("orderId");
        BigDecimal amount = UtilCommon.asBigDecimal(context.get("amount"));
        String paymentMethodId = (String) context.get("paymentMethodId");
        try {
            // add payment method (using this instead of addPaymentMethodToOrder because it's less redundant)
            Map results = dispatcher.runSync("createOrderPaymentPreference", UtilMisc.toMap("userLogin", userLogin, "orderId", orderId, "paymentMethodTypeId", "CREDIT_CARD", "paymentMethodId", paymentMethodId, "maxAmount", amount));
            if (ServiceUtil.isError(results)) return results;
            String prefId = (String) results.get("orderPaymentPreferenceId");

            // save the security code directly because the updateOrderPaymentPrefernce does unwanted business logic
            GenericValue pref = delegator.findByPrimaryKey("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", prefId));
            pref.set("securityCode", securityCode);
            pref.store();

            // authorize the payment pref
            results = dispatcher.runSync("authOrderPaymentPreference", UtilMisc.toMap("userLogin", userLogin, "orderPaymentPreferenceId", prefId));
            if (ServiceUtil.isError(results)) return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }

        return ServiceUtil.returnSuccess();
    }


    public static Map prepareOrderConfirmationEmail(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Map result = ServiceUtil.returnSuccess();

        String orderId = (String) context.get("orderId");
        result.put("orderId", orderId);

        String sendTo = (String) context.get("sendTo");
        result.put("toEmail", sendTo);

        // Provide the correct order confirmation ProductStoreEmailSetting, if one exists
        GenericValue orderHeader = null;
        GenericValue productStoreEmailSetting = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }

        // set the baseUrl parameter, required by some email bodies
        NotificationServices.setBaseUrl(delegator, (String) context.get("webSiteId"), context);

        if (UtilValidate.isEmpty(orderHeader))
            return UtilMessage.createAndLogServiceError("CrmError_ConfirmationEmailWithoutOrder", locale, module);

        if (UtilValidate.isEmpty(orderHeader.getString("productStoreId")))
            return UtilMessage.createAndLogServiceError("CrmError_ConfirmationOrderWithoutProductStore", locale, module);

        if (UtilValidate.isEmpty(orderHeader.getString("webSiteId"))) {
            String webSiteId = (String) context.get("webSiteId");
            if (UtilValidate.isEmpty(webSiteId))
                webSiteId = "";
            orderHeader.setString("webSiteId", webSiteId);
            try {
                orderHeader.store();
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, locale, module );
            }
        }

        try {
            productStoreEmailSetting = delegator.findByPrimaryKeyCache("ProductStoreEmailSetting", UtilMisc.toMap("productStoreId", orderHeader.getString("productStoreId"), "emailType", "PRDS_ODR_CONFIRM"));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }

        if (UtilValidate.isNotEmpty(productStoreEmailSetting)) {
            String subjectPattern = productStoreEmailSetting.getString("subject");
            String subject = null;
            Map args = UtilMisc.toMap("orderId", orderId);
            if (UtilValidate.isNotEmpty(subjectPattern)) {
                subject = FlexibleStringExpander.expandString(subjectPattern, args);
            }
            result.put("subject", subject);

            String bodyScreen = productStoreEmailSetting.getString("bodyScreenLocation");
            String content = null;
            if (UtilValidate.isNotEmpty(bodyScreen)) {
                ResourceBundleMapWrapper uiLabelMap = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap("EcommerceUiLabels", locale);
                uiLabelMap.addBottomResourceBundle("BlogUiLabels");
                uiLabelMap.addBottomResourceBundle("OrderUiLabels");
                uiLabelMap.addBottomResourceBundle("CommonUiLabels");
                context.put("uiLabelMap", uiLabelMap);
                try {
                    content = ScreenHelper.renderScreenLocationAsText(bodyScreen, dctx, context, UtilMisc.toMap("orderId", orderId, "baseUrl", context.get("baseUrl")));
                } catch (Exception e) {
                    return UtilMessage.createAndLogServiceError(e, module);
                }
            }

            result.put("content", content);
        }

        //create a pend sales order email by opentaps.prepareSalesOrderEmail service
        String serviceName = "opentaps.prepareSalesOrderEmail";
        try {
            ModelService service = dctx.getModelService(serviceName);
            Map input = service.makeValid(result, "IN");
            input.put("userLogin", userLogin);
            Map serviceResults = dispatcher.runSync(serviceName, input);
            return serviceResults;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
    }


    public static Map markServicesAsPerformed(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
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
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }

        // check the order item is a non physical item
        try {
            if ( UtilOrder.isItemPhysical(orderItem) ) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductCannotBeMarkPerformed", UtilMisc.toMap("productId", orderItem.getString("productId")), locale, module );
            }

            // change the order item status
            ModelService modelService = dispatcher.getDispatchContext().getModelService("changeOrderItemStatus");
            Map input = modelService.makeValid(context, "IN");
            input.put("userLogin", userLogin);
            input.put("statusId", "ITEM_PERFORMED");
            Map result = dispatcher.runSync("changeOrderItemStatus", input);
            return result;            
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module );
        }
        
    }

    public static Map createShipGroup(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // service arguments
        String orderId = (String) context.get("orderId");
        String contactMechId = (String) context.get("contactMechId");
        String shippingMethod = (String) context.get("shippingMethod");
        String maySplit = (String) context.get("maySplit");
        String isGift = (String) context.get("isGift");
        // service arguments for order items
        Map orderIds = (Map) context.get("orderIds");
        Map orderItemSeqIds = (Map) context.get("orderItemSeqIds");
        Map shipGroupSeqIds = (Map) context.get("shipGroupSeqIds");
        Map qtiesToTransfer = (Map) context.get("qtiesToTransfer");
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
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, module);
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
            return UtilMessage.createAndLogServiceError(e, locale, module);
        }

        // validate status is not COMPLETED or CANCELLED
        if ("ORDER_COMPLETED".equals(orderHeader.getString("statusId")) || "ORDER_CANCELLED".equals(orderHeader.getString("statusId"))) {
            // TODO: implement as a label
            return UtilMessage.createAndLogServiceError("Cannot create a new ship group for this order because it is already Cancelled or Completed", module);
        }


        // build a list of order items to include as [ {'orderId' => orderId, 'orderItemSeqId' => orderItemSeqId, 'shipGroupSeqId' => shipGroupSeqId, 'qtyToTransfer' => qtyToTransfer}]
        List<Map> orderItems = new ArrayList<Map>();
        // validate:
        //  - orderId for the item is the same as the order orderId
        //  - qtyToTransfer is <= remaining (ordered - cancelled) for the OrderItemShipGroupAssoc
        //  - the order item is not in a picklist item
        //  - there is at least one item to transfer
        // - gets the related OrderItemShipGroupAssoc and OrderItemShipGrpInvRes
        Iterator keyIt = orderIds.keySet().iterator();
        while (keyIt.hasNext()) {
            String key = (String) keyIt.next();
            String itemOrderId = (String) orderIds.get(key);
            String itemOrderItemSeqId = (String) orderItemSeqIds.get(key);
            String itemShipGroupSeqId = (String) shipGroupSeqIds.get(key);
            String itemQtyToTransferStr = (String) qtiesToTransfer.get(key);
            Double itemQtyToTransfer;
            // empty means 0
            if (UtilValidate.isEmpty(itemQtyToTransferStr)) {
                itemQtyToTransfer = 0.0;
            } else {
                itemQtyToTransfer = Double.parseDouble(itemQtyToTransferStr);
            }
            // 0 qty, skip this item
            if (itemQtyToTransfer == 0.0) {
                continue;
            }
            // validate all parameters are present for this order item
            if (UtilValidate.isEmpty(itemOrderId)) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("Missing the order item orderId", module);
            }
            if (UtilValidate.isEmpty(itemOrderItemSeqId)) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("Missing the order item orderItemSeqId", module);
            }
            if (UtilValidate.isEmpty(itemShipGroupSeqId)) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("Missing the order item shipGroupSeqId", module);
            }
            // a string to id the order item in the debug messages
            String itemId = itemOrderId + "/" + itemShipGroupSeqId + "/" + itemOrderItemSeqId;
            // check the item is belonging to the same order we are editing
            if (!orderId.equals(itemOrderId)) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("The order item [" + itemId + "] does not belong to this order [" + orderId + "]", module);
            }
            // get the OrderItemShipGroupAssoc
            GenericValue orderItemShipGroupAssoc;
            try {
                orderItemShipGroupAssoc = delegator.findByPrimaryKey("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", itemOrderId, "orderItemSeqId", itemOrderItemSeqId, "shipGroupSeqId", itemShipGroupSeqId));
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, locale, module);
            }
            if (orderItemShipGroupAssoc == null) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("the order item [" + itemId + "] does not have any related OrderItemShipGroupAssoc", module);
            }
            Double qtyOrdered = orderItemShipGroupAssoc.getDouble("quantity");
            Double qtyCancelled = orderItemShipGroupAssoc.getDouble("cancelQuantity");
            if (qtyCancelled == null) {
                qtyCancelled = 0.0;
            }
            Double qtyRemaining = qtyOrdered - qtyCancelled;
            if (itemQtyToTransfer > qtyRemaining) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("Cannot transfer more than the remaining quantity [" + qtyRemaining + "] for this item [" + itemId + "]. (quantity given was [" + itemQtyToTransfer + "])", module);
            }
            // check if the item is on a picklist
            try {
                List picklistItems = delegator.findByAnd("PicklistItem", UtilMisc.toMap("orderId", itemOrderId, "orderItemSeqId", itemOrderItemSeqId, "shipGroupSeqId", itemShipGroupSeqId));
                if (UtilValidate.isNotEmpty(picklistItems)) {
                    // TODO: use label
                    return UtilMessage.createAndLogServiceError("The order item [" + itemId + "] already belongs to a Picklist, this operation is not supported.", module);
                }
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, locale, module);
            }

            // get the OrderItemShipGrpInvRes
            List orderItemShipGrpInvRess;
            try {
                orderItemShipGrpInvRess = orderItemShipGroupAssoc.getRelated("OrderItemShipGrpInvRes");
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, locale, module);
            }

            // all good, add the item to the list
            Map orderItem = UtilMisc.toMap("orderId", itemOrderId, "orderItemSeqId", itemOrderItemSeqId, "shipGroupSeqId", itemShipGroupSeqId);
            orderItem.put("itemId", itemId);
            orderItem.put("qtyToTransfer", itemQtyToTransfer);
            orderItem.put("orderItemShipGroupAssoc", orderItemShipGroupAssoc);
            orderItem.put("orderItemShipGrpInvRess", orderItemShipGrpInvRess);
            orderItems.add(orderItem);
        }

        // check that we have at least one item to transfer
        if (UtilValidate.isEmpty(orderItems)) {
            // TODO: use label
            return UtilMessage.createAndLogServiceError("No order items to transfer to the new ship group.", module);
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
            return UtilMessage.createAndLogServiceError(e, locale, module);
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

        for (Map oi : orderItems) {
            String itemId = (String) oi.get("itemId");
            // update or remove the original OrderItemShipGroupAssoc qty
            GenericValue assoc = (GenericValue) oi.get("orderItemShipGroupAssoc"); // not null from validation
            Double qtyToTransfer = (Double) oi.get("qtyToTransfer");
            Double quantity = (Double) assoc.get("quantity");
            quantity -= qtyToTransfer; // positive from validation
            if (quantity.equals(0.0)) {
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
            Double resQuantityProcessed = 0.0;
            List<GenericValue> orderItemShipGrpInvRess = (List<GenericValue>) oi.get("orderItemShipGrpInvRess");
            for (GenericValue reservation : orderItemShipGrpInvRess) {
                if (resQuantityProcessed >= qtyToTransfer) {
                    break;
                }
                Double resQuantity = (Double) reservation.get("quantity");
                Double resQuantityNotAvailable = (Double) reservation.get("quantityNotAvailable");
                // how much quantity we can remove from this OrderItemShipGrpInvRes
                Double diffQty = Math.min(qtyToTransfer - resQuantityProcessed, resQuantity - resQuantityProcessed);
                resQuantity -= diffQty;
                Double diffQtyNotAvailable = null;
                if (resQuantityNotAvailable != null && resQuantityNotAvailable > 0) {
                    diffQtyNotAvailable = Math.min(resQuantityNotAvailable, diffQty);
                    resQuantityNotAvailable -= diffQtyNotAvailable;
                }

                // set updated values or remove the OrderItemShipGrpInvRes if both quantity and quantityNotAvailable are zero or null
                if (resQuantity.equals(0.0) && (resQuantityNotAvailable == null || resQuantityNotAvailable.equals(0.0))) {
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
                newInventoryItemDetail.put("quantityOnHandDiff", 0.0);
                newInventoryItemDetail.put("shipGroupSeqId", reservation.get("shipGroupSeqId"));
                valuesToStore.add(newInventoryItemDetail);
                // the second removed that ATP  to a IDD associated to the new ship group
                inventoryItemDetailSeqId = delegator.getNextSeqId("InventoryItemDetail");
                newInventoryItemDetail = delegator.makeValue("InventoryItemDetail", UtilMisc.toMap("orderId", orderId, "inventoryItemId", reservation.get("inventoryItemId")));
                newInventoryItemDetail.put("inventoryItemDetailSeqId", inventoryItemDetailSeqId);
                newInventoryItemDetail.put("availableToPromiseDiff", (-diffQty));
                newInventoryItemDetail.put("quantityOnHandDiff", 0.0);
                newInventoryItemDetail.put("shipGroupSeqId", orderItemShipGroup.get("shipGroupSeqId"));
                valuesToStore.add(newInventoryItemDetail);

                // account the quantity we removed
                resQuantityProcessed += diffQty;
            }
            // if some quantity was unaccounted for, then something was wrong
            if (!resQuantityProcessed.equals(qtyToTransfer)) {
                // TODO: use label
                return UtilMessage.createAndLogServiceError("Error while updating OrderItemShipGrpInvRes quantities for item [" + itemId + "], qtyToTransfer was [" + qtyToTransfer + "] but could only remove [" + resQuantityProcessed + "]", module);
            }
        }

        // update / remove the entities
        try {
            delegator.storeAll(valuesToStore);
            delegator.removeAll(valuesToRemove);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module);
        }

        // final step, check if the OISG still has any OISGassoc with quantity, else cancel it (necessary for picking later)
        valuesToStore.clear();
        try {
            for (String oisgSeqId : shipGroupRemovedAssoc) {
                long n = delegator.findCountByCondition("OrderItemShipGroupAssoc", new EntityConditionList(UtilMisc.toList(
                                         new EntityExpr("orderId", EntityOperator.EQUALS, orderId),
                                         new EntityExpr("shipGroupSeqId", EntityOperator.EQUALS, oisgSeqId),
                                         new EntityExpr("quantity", EntityOperator.GREATER_THAN, 0.0)), EntityOperator.AND), null);
                if (n == 0) {
                    GenericValue oisg = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", oisgSeqId));
                    oisg.put("statusId", "OISG_CANCELLED");
                    valuesToStore.add(oisg);
                }
            }
            // update them
            delegator.storeAll(valuesToStore);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module);
        }

        return ServiceUtil.returnSuccess();
    }

}
