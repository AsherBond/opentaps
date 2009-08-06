package com.opensourcestrategies.crmsfa.returns;

/*
* Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.security.Security;
import org.ofbiz.service.*;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilConfig;
import org.opentaps.common.util.UtilMessage;

import java.util.*;

public class ReturnServices {

    public static final String module = ReturnServices.class.getName();

        public static Map setOrderUndeliverableFromReturn(DispatchContext dctx, Map context) {
            GenericDelegator delegator = dctx.getDelegator();
            LocalDispatcher dispatcher = dctx.getDispatcher();
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String returnId = (String) context.get("returnId");

            try {
                // in case other types of returns are supported in the system in the future
                GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
                if (!"CUSTOMER_RETURN".equals(returnHeader.getString("returnHeaderTypeId"))) {
                    return ServiceUtil.returnFailure("Return [" + returnId + "] is not a customer return so it will not be checked for undeliverable or rejected items");
                }

                EntityConditionList undeliverableReasonCond = new EntityConditionList(UtilMisc.toList(
                        new EntityExpr("returnReasonId", EntityOperator.EQUALS, "RTN_UNDELIVERABLE"),
                        new EntityExpr("returnReasonId", EntityOperator.EQUALS, "RTN_COD_REJECT")),
                        EntityOperator.OR);
                EntityConditionList returnItemCond = new EntityConditionList(UtilMisc.toList(
                        undeliverableReasonCond,
                        new EntityExpr("returnId", EntityOperator.EQUALS, returnId)),
                        EntityOperator.AND);
                // find distinct orderIds in this return which were returned for undeliverable or COD reject reasons.
                EntityListIterator listIt = delegator.findListIteratorByCondition("ReturnItem", returnItemCond,
                        null, // havingEntityConditions
                        UtilMisc.toList("orderId", "orderItemSeqId"),
                        UtilMisc.toList("orderId", "orderItemSeqId"),
                        // Distinct select
                        UtilCommon.DISTINCT_READ_OPTIONS);
                List returnItems = listIt.getCompleteList();
                listIt.close();

                Set updatedOrderIds = new HashSet();

                // go through the order items and set them to undeliverable, then set their orders to undeliverable, but check to make sure that
                // its status is not already set to that.  This will have the curious effect of potentially setting some items to undeliverable on
                // a second return created for an order
                if (UtilValidate.isNotEmpty(returnItems)) {
                    for (Iterator itemsIt = returnItems.iterator(); itemsIt.hasNext(); ) {
                        GenericValue item = (GenericValue) itemsIt.next();
                        Map tmpResult = dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("orderId", item.getString("orderId"),
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


    public static Map createReturnFromOrder(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String orderId = (String) context.get("orderId");

        if (! security.hasEntityPermission("CRMSFA", "_RETURN_CREATE", userLogin)) {
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

            Map input = UtilMisc.toMap("userLogin", userLogin, "returnHeaderTypeId", "CUSTOMER_RETURN", "statusId", "RETURN_REQUESTED");
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
            return UtilMessage.createAndLogServiceError(e, locale, module);
        }
    }

    public static Map updateReturnHeader(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String returnId = (String) context.get("returnId");
        String billingAccountId = (String) context.get("billingAccountId");

        try {
            // create a new account and use its billingAccountId for the update
            if ("NEW_ACCOUNT".equals(billingAccountId)) {
                GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
                Map input = UtilMisc.toMap("userLogin", userLogin);
                input.put("accountCurrencyUomId", returnHeader.get("currencyUomId"));
                input.put("description", UtilMessage.expandLabel("CrmNewBillingAccountDescription", locale, UtilMisc.toMap("returnId", returnId)));
                input.put("partyId", returnHeader.get("fromPartyId"));
                input.put("roleTypeId", "BILL_TO_CUSTOMER");
                input.put("accountLimit", new Double(0.0));
                Map results = dispatcher.runSync("createBillingAccount", input);
                if (ServiceUtil.isError(results)) return results;
                context.put("billingAccountId", results.get("billingAccountId"));
            }
            return dispatcher.runSync("updateReturnHeader", context);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module);
        }
    }

    /**
     * Accepts a return and receives inventory, creating a shipment if necessary
     * 
     * @param dctx
     * @param context
     * @return
     */
    public static Map acceptReturn(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String returnId = (String) context.get("returnId");
        String shipmentId = (String) context.get("shipmentId");

        try {
            
            GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));

            Map serviceResult = dispatcher.runSync("updateReturnHeader", UtilMisc.toMap("returnId", returnId, "userLogin", userLogin, "statusId", "RETURN_ACCEPTED", "needsInventoryReceive", "N"));
            if (ServiceUtil.isError(serviceResult)) return serviceResult;

            if ("Y".equals(returnHeader.getString("needsInventoryReceive"))) {
    
                List<GenericValue> returnItems = delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId));
                if (UtilValidate.isEmpty(shipmentId)) {
    
                    // Create a shipment
                    serviceResult = dispatcher.runSync("createShipmentForReturn", UtilMisc.toMap("userLogin", userLogin, "locale", locale, "returnId", returnId));
                    if (ServiceUtil.isError(serviceResult)) return serviceResult;
                    shipmentId = (String) serviceResult.get("shipmentId");
        
                    // Update the shipment with the returnId
                    serviceResult = dispatcher.runSync("updateShipment", UtilMisc.toMap("userLogin", userLogin, "locale", locale, "shipmentId", shipmentId, "returnId", returnId));
                    if (ServiceUtil.isError(serviceResult)) return serviceResult;
    
                    // Create ShipmentItems
                    for (GenericValue returnItem : returnItems) {
                        serviceResult = dispatcher.runSync("createShipmentItem", UtilMisc.toMap("userLogin", userLogin, "locale", locale, "shipmentId", shipmentId, "productId", returnItem.getString("productId"), "quantity", returnItem.getDouble("returnQuantity")));
                        if (ServiceUtil.isError(serviceResult)) return serviceResult;
                    }
                }
                
                GenericValue destinationFacility = returnHeader.getRelatedOne("Facility");
                for (GenericValue returnItem : returnItems) {
                    GenericValue orderItem = returnItem.getRelatedOne("OrderItem");
                    String productId = orderItem.getString("productId");
                    if (UtilValidate.isEmpty(productId)) continue;
                    
                    serviceResult = dispatcher.runSync("getReturnItemInitialCost", UtilMisc.toMap("returnId", returnId, "returnItemSeqId", returnItem.get("returnItemSeqId")));
                    if (ServiceUtil.isError(serviceResult)) return serviceResult;
                    Double unitCost = (Double) serviceResult.get("initialItemCost");
                    
                    long serializedInvItems = delegator.findCountByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "facilityId", destinationFacility.get("facilityId"), "inventoryItemTypeId", "SERIALIZED_INV_ITEM"));
                    boolean nonSerialized = serializedInvItems == 0 && "NON_SERIAL_INV_ITEM".equals(destinationFacility.getString("defaultInventoryItemTypeId"));
                    
                    Map receiveContext = UtilMisc.toMap("userLogin", userLogin, "productId", productId, "returnId", returnId, "returnItemSeqId", returnItem.get("returnItemSeqId"));
                    receiveContext.put("inventoryItemTypeId", nonSerialized ? "NON_SERIAL_INV_ITEM" : "SERIALIZED_INV_ITEM");
                    receiveContext.put("statusId", UtilValidate.isEmpty(returnItem.getString("expectedItemStatus")) ? "INV_RETURNED" : UtilValidate.isEmpty(returnItem.getString("expectedItemStatus")));
                    receiveContext.put("facilityId", destinationFacility.get("facilityId"));
                    receiveContext.put("shipmentId", shipmentId);
                    receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                    receiveContext.put("quantityRejected", new Double(0));
                    receiveContext.put("comments", "Returned Item RA# " + returnId);
                    receiveContext.put("unitCost", unitCost);
                    
                    if (nonSerialized) {
                        
                        // Receive once for the full quantity
                        receiveContext.put("quantityAccepted", returnItem.getDouble("returnQuantity"));
                        serviceResult = dispatcher.runSync("receiveInventoryProduct", receiveContext);
                        if (ServiceUtil.isError(serviceResult)) return serviceResult;
                    } else {
                        
                        // Receive with quantity 1 until the full quantity is received
                        receiveContext.put("quantityAccepted", new Double(1));
                        for (int x = 0; x < returnItem.getDouble("returnQuantity").intValue(); x++) {
                            serviceResult = dispatcher.runSync("receiveInventoryProduct", receiveContext);
                            if (ServiceUtil.isError(serviceResult)) return serviceResult;
                        }
                    }
                }
            }
            
            returnHeader.refresh();

            if ("RETURN_COMPLETED".equals(returnHeader.getString("statusId"))) {
                Debug.logWarning("That would change the return status from RETURN_COMPLETED to RETURN_RECEIVED", module);
            } else {
                serviceResult = dispatcher.runSync("updateReturnHeader", UtilMisc.toMap("returnId", returnId, "userLogin", userLogin, "statusId", "RETURN_RECEIVED"));
                if (ServiceUtil.isError(serviceResult)) return serviceResult;
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module);
        }
        
        return ServiceUtil.returnSuccess();
    }
}
