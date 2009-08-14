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

package org.opentaps.purchasing.order;

import java.sql.Timestamp;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;

public class PurchasingOrderServices {

    public static final String module = PurchasingOrderServices.class.getName();
    public static final String resource = "PurchasingErrorLabels";

    /**
     * Update a Purchasing Order Item estimated delivery date.
     *  This is done by setting a OrderDeliverySchedule entity with the matching
     *  orderId and orderItemSeqId
     *  The order status must be either ORDER_APPROVED, ORDER_HOLD or ORDER_CREATED
     *  else the service will return failure.
     * 
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map updateOrderItemEstimatedDeliveryDate(DispatchContext ctx, Map context) throws GenericEntityException {
        GenericDelegator delegator = ctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        // Service parameters
        String orderId = (String)context.get("orderId");
        String orderItemSeqId = (String)context.get("orderItemSeqId");
        // delivery date is optional so we can get null here which is ok
        Timestamp deliveryDate = (Timestamp)context.get("estimatedReadyDate");

        // Check the Order Status
        GenericValue order = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        if (UtilValidate.isEmpty(order) ||
                !( "ORDER_APPROVED".equals(order.get("statusId")) || 
                   "ORDER_HOLD".equals(order.get("statusId")) || 
                   "ORDER_CREATED".equals(order.get("statusId")))) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "PurchError_UpdateOrderItemEstimatedDeliveryDateFail", locale));
        }

        // Get the order item
        GenericValue orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
        if (orderItem == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "PurchError_OrderItemNotExists", locale));
        }

        // Get the associated OrderDeliverySchedule if any
        GenericValue orderDeliverySchedule = delegator.findByPrimaryKey("OrderDeliverySchedule", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
        // create one if not found, else update the date
        if (orderDeliverySchedule == null) {
            orderDeliverySchedule = delegator.create("OrderDeliverySchedule", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "estimatedReadyDate", deliveryDate));
        } else {
            // if the given date is empty delete the existing record
            if (UtilValidate.isEmpty(deliveryDate)) {
                orderDeliverySchedule.remove();
            } else {
                orderDeliverySchedule.put("estimatedReadyDate", deliveryDate);
                orderDeliverySchedule.store();
            }
        }

        return ServiceUtil.returnSuccess();

    }

}
