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
package com.opensourcestrategies.crmsfa.returns;

import java.math.BigDecimal;
import java.util.*;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.security.Security;
import org.ofbiz.service.*;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilConfig;
import org.opentaps.common.util.UtilMessage;

public final class ReturnServices {

    private static final String MODULE = ReturnServices.class.getName();

    public static Map<String, Object> setOrderUndeliverableFromReturn(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String returnId = (String) context.get("returnId");

        try {
            // in case other types of returns are supported in the system in the future
            GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            if (!"CUSTOMER_RETURN".equals(returnHeader.getString("returnHeaderTypeId"))) {
                return ServiceUtil.returnFailure("Return [" + returnId + "] is not a customer return so it will not be checked for undeliverable or rejected items");
            }

            EntityCondition undeliverableReasonCond = EntityCondition.makeCondition(EntityOperator.OR,
                        EntityCondition.makeCondition("returnReasonId", EntityOperator.EQUALS, "RTN_UNDELIVERABLE"),
                        EntityCondition.makeCondition("returnReasonId", EntityOperator.EQUALS, "RTN_COD_REJECT"));
            EntityCondition returnItemCond = EntityCondition.makeCondition(EntityOperator.AND,
                        undeliverableReasonCond,
                        EntityCondition.makeCondition("returnId", EntityOperator.EQUALS, returnId));
            // find distinct orderIds in this return which were returned for undeliverable or COD reject reasons.
            EntityListIterator listIt = delegator.findListIteratorByCondition("ReturnItem", returnItemCond,
                        null, // havingEntityConditions
                        UtilMisc.toList("orderId", "orderItemSeqId"),
                        UtilMisc.toList("orderId", "orderItemSeqId"),
                        // Distinct select
                        UtilCommon.DISTINCT_READ_OPTIONS);
            List<GenericValue> returnItems = listIt.getCompleteList();
            listIt.close();

            Set<String> updatedOrderIds = new HashSet<String>();

            // go through the order items and set them to undeliverable, then set their orders to undeliverable, but check to make sure that
            // its status is not already set to that.  This will have the curious effect of potentially setting some items to undeliverable on
            // a second return created for an order
            if (UtilValidate.isNotEmpty(returnItems)) {
                for (Iterator<GenericValue> itemsIt = returnItems.iterator(); itemsIt.hasNext();) {
                    GenericValue item = itemsIt.next();
                    Map<String, Object> tmpResult = dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("orderId", item.getString("orderId"),
                                "orderItemSeqId", item.getString("orderItemSeqId"), "statusId", "ITEM_UNDELIVERABLE", "userLogin", userLogin));
                    if (ServiceUtil.isError(tmpResult)) {
                        return tmpResult;
                    }
                    // check that the orderId has not already been updated
                    if (!updatedOrderIds.contains(item.getString("orderId"))) {

                        // now check the previous status, in case a previous return changed it to undeliverable
                        GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", item.getString("orderId")));
                        if (!"ORDER_UNDELIVERABLE".equals(orderHeader.getString("statusId"))) {
                            tmpResult = dispatcher.runSync("changeOrderStatus", UtilMisc.toMap("orderId", item.getString("orderId"),
                                        "statusId", "ORDER_UNDELIVERABLE", "userLogin", userLogin));
                            if (ServiceUtil.isError(tmpResult)) {
                                return tmpResult;
                            }
                        }
                        updatedOrderIds.add(item.getString("orderId"));

                    }
                }
            }

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }


    public static Map<String, Object> createReturnFromOrder(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String orderId = (String) context.get("orderId");

        if (!security.hasEntityPermission("CRMSFA", "_RETURN_CREATE", userLogin)) {
            return UtilMessage.createServiceError("OpentapsError_PermissionDenied", locale);
        }
        try {
            GenericValue order = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (order == null) {
                return UtilMessage.createServiceError("OpentapsError_OrderNotFound", locale, UtilMisc.toMap("orderId", orderId));
            }
            OrderReadHelper orh = new OrderReadHelper(order);
            GenericValue fromParty = orh.getBillToParty();
            GenericValue productStore = orh.getProductStore();
            String toPartyId = productStore.getString("payToPartyId");
            String destinationFacilityId = order.getString("originFacilityId");
            if (UtilValidate.isEmpty(destinationFacilityId)) {
                destinationFacilityId = productStore.getString("inventoryFacilityId");
            }
            boolean autoReceiveOnAccept = UtilConfig.getPropertyBoolean("crmsfa", "crmsfa.order.return.autoReceiveOnAccept", true);

            Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin, "returnHeaderTypeId", "CUSTOMER_RETURN", "statusId", "RETURN_REQUESTED");
            input.put("entryDate", UtilDateTime.nowTimestamp());
            input.put("needsInventoryReceive", autoReceiveOnAccept ? "Y" : "N");
            input.put("fromPartyId", fromParty.get("partyId"));
            input.put("toPartyId", toPartyId);
            input.put("currencyUomId", orh.getCurrency());
            input.put("destinationFacilityId", destinationFacilityId);
            input.put("primaryOrderId", orderId); // this is a field that is extended in opentaps
            input.put("comments", context.get("comments")); // this one as well

            return dispatcher.runSync("createReturnHeader", input);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
    }

    public static Map<String, Object> updateReturnHeader(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String returnId = (String) context.get("returnId");
        String billingAccountId = (String) context.get("billingAccountId");

        try {
            // create a new account and use its billingAccountId for the update
            if ("NEW_ACCOUNT".equals(billingAccountId)) {
                GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
                Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                input.put("accountCurrencyUomId", returnHeader.get("currencyUomId"));
                input.put("description", UtilMessage.expandLabel("CrmNewBillingAccountDescription", locale, UtilMisc.toMap("returnId", returnId)));
                input.put("partyId", returnHeader.get("fromPartyId"));
                input.put("roleTypeId", "BILL_TO_CUSTOMER");
                input.put("accountLimit", BigDecimal.ZERO);
                Map<String, Object> results = dispatcher.runSync("createBillingAccount", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
                context.put("billingAccountId", results.get("billingAccountId"));
            }
            return dispatcher.runSync("updateReturnHeader", context);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
    }

    /**
     * Accepts a return and receives inventory, creating a shipment if necessary.
     */
    public static Map<String, Object> acceptReturn(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String returnId = (String) context.get("returnId");
        String shipmentId = (String) context.get("shipmentId");

        try {

            GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));

            Map<String, Object> serviceResult = dispatcher.runSync("updateReturnHeader", UtilMisc.toMap("returnId", returnId, "userLogin", userLogin, "statusId", "RETURN_ACCEPTED", "needsInventoryReceive", "N"));
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult;
            }

            if ("Y".equals(returnHeader.getString("needsInventoryReceive"))) {

                List<GenericValue> returnItems = delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId));
                if (UtilValidate.isEmpty(shipmentId)) {

                    // Create a shipment
                    serviceResult = dispatcher.runSync("createShipmentForReturn", UtilMisc.toMap("userLogin", userLogin, "locale", locale, "returnId", returnId));
                    if (ServiceUtil.isError(serviceResult)) {
                        return serviceResult;
                    }
                    shipmentId = (String) serviceResult.get("shipmentId");

                    // Update the shipment with the returnId
                    serviceResult = dispatcher.runSync("updateShipment", UtilMisc.toMap("userLogin", userLogin, "locale", locale, "shipmentId", shipmentId, "returnId", returnId));
                    if (ServiceUtil.isError(serviceResult)) {
                        return serviceResult;
                    }

                    // Create ShipmentItems
                    for (GenericValue returnItem : returnItems) {
                        serviceResult = dispatcher.runSync("createShipmentItem", UtilMisc.toMap("userLogin", userLogin, "locale", locale, "shipmentId", shipmentId, "productId", returnItem.getString("productId"), "quantity", returnItem.getBigDecimal("returnQuantity")));
                        if (ServiceUtil.isError(serviceResult)) {
                            return serviceResult;
                        }
                    }
                }

                GenericValue destinationFacility = returnHeader.getRelatedOne("Facility");
                for (GenericValue returnItem : returnItems) {
                    GenericValue orderItem = returnItem.getRelatedOne("OrderItem");
                    String productId = orderItem.getString("productId");
                    if (UtilValidate.isEmpty(productId)) {
                        continue;
                    }

                    serviceResult = dispatcher.runSync("getReturnItemInitialCost", UtilMisc.toMap("returnId", returnId, "returnItemSeqId", returnItem.get("returnItemSeqId")));
                    if (ServiceUtil.isError(serviceResult)) {
                        return serviceResult;
                    }
                    BigDecimal unitCost = (BigDecimal) serviceResult.get("initialItemCost");

                    long serializedInvItems = delegator.findCountByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "facilityId", destinationFacility.get("facilityId"), "inventoryItemTypeId", "SERIALIZED_INV_ITEM"));
                    boolean nonSerialized = serializedInvItems == 0 && "NON_SERIAL_INV_ITEM".equals(destinationFacility.getString("defaultInventoryItemTypeId"));

                    Map<String, Object> receiveContext = UtilMisc.<String, Object>toMap("userLogin", userLogin, "productId", productId, "returnId", returnId, "returnItemSeqId", returnItem.get("returnItemSeqId"));
                    receiveContext.put("inventoryItemTypeId", nonSerialized ? "NON_SERIAL_INV_ITEM" : "SERIALIZED_INV_ITEM");
                    receiveContext.put("statusId", UtilValidate.isEmpty(returnItem.getString("expectedItemStatus")) ? "INV_RETURNED" : UtilValidate.isEmpty(returnItem.getString("expectedItemStatus")));
                    receiveContext.put("facilityId", destinationFacility.get("facilityId"));
                    receiveContext.put("shipmentId", shipmentId);
                    receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                    receiveContext.put("quantityRejected", BigDecimal.ZERO);
                    receiveContext.put("comments", "Returned Item RA# " + returnId);
                    receiveContext.put("unitCost", unitCost);

                    if (nonSerialized) {

                        // Receive once for the full quantity
                        receiveContext.put("quantityAccepted", returnItem.getBigDecimal("returnQuantity"));
                        serviceResult = dispatcher.runSync("receiveInventoryProduct", receiveContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return serviceResult;
                        }
                    } else {

                        // Receive with quantity 1 until the full quantity is received
                        receiveContext.put("quantityAccepted", BigDecimal.ONE);
                        for (int x = 0; x < returnItem.getBigDecimal("returnQuantity").intValue(); x++) {
                            serviceResult = dispatcher.runSync("receiveInventoryProduct", receiveContext);
                            if (ServiceUtil.isError(serviceResult)) {
                                return serviceResult;
                            }
                        }
                    }
                }
            }

            returnHeader.refresh();

            if ("RETURN_COMPLETED".equals(returnHeader.getString("statusId"))) {
                Debug.logWarning("That would change the return status from RETURN_COMPLETED to RETURN_RECEIVED", MODULE);
            } else {
                serviceResult = dispatcher.runSync("updateReturnHeader", UtilMisc.toMap("returnId", returnId, "userLogin", userLogin, "statusId", "RETURN_RECEIVED"));
                if (ServiceUtil.isError(serviceResult)) {
                    return serviceResult;
                }
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }
}
