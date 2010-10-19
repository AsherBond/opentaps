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

// This file has been modified by Open Source Strategies, Inc.

package com.opensourcestrategies.crmsfa.returns;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;
import org.ofbiz.entity.condition.EntityCondition;

/**
 * OrderReturnServices, copied from ofbiz for modification.
 */
public final class OrderReturnServices {

    private OrderReturnServices() { }

    private static final String MODULE = OrderReturnServices.class.getName();

    /**
     * Get a map of returnable items (items not already returned) and quantities.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> getReturnableItems(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "OrderErrorUnableToGetReturnItemInformation", locale, MODULE);
        }

        Map<GenericValue, Map<String, Object>> returnable = new HashMap<GenericValue, Map<String, Object>>();
        if (orderHeader != null) {

            // OrderItems which have been issued may be returned.
            EntityCondition whereConditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderHeader.getString("orderId")),
                    EntityCondition.makeCondition("orderItemStatusId", EntityOperator.IN, UtilMisc.toList("ITEM_APPROVED", "ITEM_COMPLETED"))
                );
            EntityCondition havingConditions = EntityCondition.makeCondition("quantityIssued", EntityOperator.GREATER_THAN, BigDecimal.ZERO);
            List<GenericValue> orderItemQuantitiesIssued = null;
            try {
                orderItemQuantitiesIssued = delegator.findByCondition("OrderItemQuantityReportGroupByItem", whereConditions, havingConditions, UtilMisc.toList("orderId", "orderItemSeqId"), UtilMisc.toList("orderItemSeqId"), null);
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, "OrderErrorUnableToGetReturnHeaderFromItem", locale, MODULE);
            }
            if (orderItemQuantitiesIssued != null) {
                Iterator<GenericValue> i = orderItemQuantitiesIssued.iterator();
                while (i.hasNext()) {
                    GenericValue orderItemQuantityIssued = i.next();
                    GenericValue item = null;
                    try {
                        item = orderItemQuantityIssued.getRelatedOne("OrderItem");
                    } catch (GenericEntityException e) {
                        return UtilMessage.createAndLogServiceError(e, "OrderErrorUnableToGetOrderItemInformation", locale, MODULE);
                    }
                    Map<String, Object> serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync("getReturnableQuantity", UtilMisc.toMap("orderItem", item));
                    } catch (GenericServiceException e) {
                        return UtilMessage.createAndLogServiceError(e, "OrderErrorUnableToGetTheItemReturnableQuantity", locale, MODULE);
                    }
                    if (ServiceUtil.isError(serviceResult)) {
                        return serviceResult;
                    } else {

                        // Don't add the OrderItem to the map of returnable OrderItems if there isn't any returnable quantity.
                        if (((BigDecimal) serviceResult.get("returnableQuantity")).signum() == 0) {
                            continue;
                        }
                        Map<String, Object> returnInfo = new HashMap<String, Object>();
                        // first the return info (quantity/price)
                        returnInfo.put("returnableQuantity", serviceResult.get("returnableQuantity"));
                        returnInfo.put("returnablePrice", serviceResult.get("returnablePrice"));

                        // now the product type information
                        String itemTypeKey = "FINISHED_GOOD"; // default item type (same as invoice)
                        GenericValue product = null;
                        if (item.get("productId") != null) {
                            try {
                                product = item.getRelatedOne("Product");
                            } catch (GenericEntityException e) {
                                return UtilMessage.createAndLogServiceError(e, "OrderErrorUnableToGetOrderItemInformation", locale, MODULE);
                            }
                        }
                        if (product != null) {
                            itemTypeKey = product.getString("productTypeId");
                        } else if (item != null && item.getString("orderItemTypeId") != null) {
                            itemTypeKey = item.getString("orderItemTypeId");
                        }
                        returnInfo.put("itemTypeKey", itemTypeKey);

                        returnable.put(item, returnInfo);
                    }
                }
            } else {
                return UtilMessage.createAndLogServiceError("OrderErrorNoOrderItemsFound", locale, MODULE);
            }
        } else {
            return UtilMessage.createAndLogServiceError("OrderErrorUnableToFindOrderHeader", locale, MODULE);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("returnableItems", returnable);
        return result;
    }
}
