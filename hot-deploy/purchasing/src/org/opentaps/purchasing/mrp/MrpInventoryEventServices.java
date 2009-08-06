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

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.purchasing.mrp;

import java.util.*;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

/**
 * MrpInventoryEventServices - MrpInventoryEvent related Services.
 */
public final class MrpInventoryEventServices {

    private MrpInventoryEventServices() { }

    private static int MRP_INVENTORY_EVENT_DETAIL_PADDING = 5;

    /**
     * Describe <code>createOrUpdateMrpInventoryEvent</code> method here.
     *
     * @param mrpInventoryEventKeyMap a <code>Map</code> value
     * @param newQuantity a <code>Double</code> value
     * @param netQoh a <code>Double</code> value
     * @param eventName a <code>String</code> value
     * @param isLate a <code>boolean</code> value
     * @param mrpInventoryEventDetailInput a <code>Map</code> value
     * @param delegator a <code>GenericDelegator</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static void createOrUpdateMrpInventoryEvent(Map mrpInventoryEventKeyMap, Double newQuantity, Double netQoh, String eventName, boolean isLate, Map mrpInventoryEventDetailInput, GenericDelegator delegator) throws GenericEntityException {
        GenericValue mrpInventoryEvent = null;
        mrpInventoryEvent = delegator.findByPrimaryKey("MrpInventoryEvent", mrpInventoryEventKeyMap);
        if (mrpInventoryEvent == null) {
            mrpInventoryEvent = delegator.makeValue("MrpInventoryEvent", mrpInventoryEventKeyMap);
            mrpInventoryEvent.put("eventQuantity", newQuantity);
            mrpInventoryEvent.put("netQoh", netQoh);
            mrpInventoryEvent.put("eventName", eventName);
            mrpInventoryEvent.put("isLate", (isLate ? "Y" : "N"));
            mrpInventoryEvent.create();
        } else {
            if (newQuantity != null) {
                double qties = newQuantity.doubleValue() + ((Double) mrpInventoryEvent.get("eventQuantity")).doubleValue();
                mrpInventoryEvent.put("eventQuantity", new Double(qties));
            }
            if (netQoh != null) {
                //double qties = netQoh.doubleValue() + ((Double)mrpInventoryEvent.get("netQoh")).doubleValue();
                mrpInventoryEvent.put("netQoh", netQoh);
            }
            if (UtilValidate.isNotEmpty(eventName)) {
                String existingEventName = mrpInventoryEvent.getString("eventName");
                mrpInventoryEvent.put("eventName", (UtilValidate.isEmpty(existingEventName) ? eventName : existingEventName + ", " + eventName));
            }
            if (isLate) {
                mrpInventoryEvent.put("isLate", "Y");
            }
            mrpInventoryEvent.store();
        }

        // create MrpInventoryEventDetail. We use info stored in this entity to associate Orders to Requirements through OrderRequirementCommitment.
        if (mrpInventoryEventDetailInput != null) {
            createMrpInventoryEventDetail(mrpInventoryEventKeyMap, mrpInventoryEventDetailInput, newQuantity, delegator);
        }
    }

    /**
     * Describe <code>createMrpInventoryEventDetail</code> method here.
     *
     * @param mrpInventoryEventKeyMap a <code>Map</code> value
     * @param mrpInventoryEventDetailInput a <code>Map</code> value
     * @param newQuantity a <code>Double</code> value
     * @param delegator a <code>GenericDelegator</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static void createMrpInventoryEventDetail(Map mrpInventoryEventKeyMap, Map mrpInventoryEventDetailInput, Double newQuantity, GenericDelegator delegator) throws GenericEntityException {
        String mrpInventoryEventTypeId = (String) mrpInventoryEventKeyMap.get("inventoryEventPlanTypeId");
        if ("SALES_ORDER_SHIP".equals(mrpInventoryEventTypeId) && UtilValidate.isNotEmpty(mrpInventoryEventDetailInput)) {
            mrpInventoryEventKeyMap.putAll(mrpInventoryEventDetailInput);
            List mrpInventoryEventDetails = delegator.findByAnd("MrpInventoryEventDetail", mrpInventoryEventKeyMap);
            GenericValue mrpInventoryEventDetail = EntityUtil.getFirst(mrpInventoryEventDetails);
            if (mrpInventoryEventDetail == null) {
                mrpInventoryEventDetail = delegator.makeValue("MrpInventoryEventDetail", mrpInventoryEventKeyMap);
                mrpInventoryEventDetail.putAll(mrpInventoryEventDetailInput);
                mrpInventoryEventDetail.put("quantity", newQuantity);
                delegator.setNextSubSeqId(mrpInventoryEventDetail, "mrpInvEvtDetSeqId", MRP_INVENTORY_EVENT_DETAIL_PADDING, 1);
                mrpInventoryEventDetail.create();
            } else {
                double qties = newQuantity.doubleValue() + ((Double) mrpInventoryEventDetail.get("quantity")).doubleValue();
                mrpInventoryEventDetail.put("quantity", new Double(qties));
                mrpInventoryEventDetail.store();
            }
        }
    }
}
