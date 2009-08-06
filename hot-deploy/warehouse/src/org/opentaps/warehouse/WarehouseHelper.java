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

package org.opentaps.warehouse;

import java.util.List;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.util.UtilCommon;

/**
 * Helper routines for the various screens defined in the OpenTaps Warehouse application
 *
 * @author     <a href="mailto:asser.professional@yahoo.com">Asser Hassanain</a>
 * @version    $Rev: 1271 $
 */
public class WarehouseHelper {

    public static final String module = WarehouseHelper.class.getName();

    /**
     * A routine that returns a list of the lot ID's associated with the inventory reservation
     * associated with the given parameters
     * @param orderId String
     * @param orderItemSeqId String
     * @param shipGroupSeqId String
     * @param delegator GenericDelegator
     * @return List
     */
    public static List getReservationLotIdList(String orderId, String orderItemSeqId, String shipGroupSeqId, GenericDelegator delegator) {
        EntityCondition lotCondition = new EntityConditionList(UtilMisc.toList(
            new EntityExpr("orderId", EntityOperator.EQUALS, orderId),
            new EntityExpr("orderItemSeqId", EntityOperator.EQUALS, orderItemSeqId),
            new EntityExpr("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId),
            new EntityExpr("lotId", EntityOperator.NOT_EQUAL, null)), EntityOperator.AND);

        try {
            List items = delegator.findByCondition("OdrItShpGrpHdrInvResAndInvItem",
                lotCondition, null, UtilMisc.toList("lotId"), UtilMisc.toList("lotId"),
                UtilCommon.DISTINCT_READ_OPTIONS);
            if (UtilValidate.isNotEmpty(items)) {
                return EntityUtil.getFieldListFromEntityList(items, "lotId", true);
            } else {
                Debug.logWarning("Could not find OdrItShpGrpHdrInvResAndInvItem item with parameters orderId = " + orderId +
                    ", orderItemSeqId = " + orderItemSeqId + ", and shipGroupSeqId = " + shipGroupSeqId, module);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Could not OdrItShpGrpHdrInvResAndInvItem item with parameters orderId = " + orderId +
                ", orderItemSeqId = " + orderItemSeqId + ", and shipGroupSeqId = " + shipGroupSeqId, module);
        }

        return null;
    }

    /**
     * A routine that returns the lot ID associated with the inventory reservation associated with
     * the given parameters
     * @param orderId String
     * @param orderItemSeqId String
     * @param shipGroupSeqId String
     * @param delegator GenericDelegator
     * @return String
     */
    public static String getReservationLotId(String orderId, String orderItemSeqId, String shipGroupSeqId, GenericDelegator delegator) {
        List lotIdList = getReservationLotIdList(orderId, orderItemSeqId, shipGroupSeqId, delegator);

        if (UtilValidate.isNotEmpty(lotIdList)) {
            return (String)lotIdList.get(0);
        } else {
            return null;
        }
    }
    

    /**
     * A routine that returns a list of the lot ID associated with the item issuance associated with the
     * given parameters
     * @param shipmentId String
     * @param shipmentItemSeqId String
     * @param delegator GenericDelegator
     * @return List
     */
    public static List getIssuanceLotIdList(String shipmentId, String shipmentItemSeqId, GenericDelegator delegator) {
        EntityCondition lotCondition = new EntityConditionList(UtilMisc.toList(
            new EntityExpr("shipmentId", EntityOperator.EQUALS, shipmentId),
            new EntityExpr("shipmentItemSeqId", EntityOperator.EQUALS, shipmentItemSeqId),
            new EntityExpr("lotId", EntityOperator.NOT_EQUAL, null)), EntityOperator.AND);

        try {
            List items = delegator.findByCondition("ItemIssuanceInventoryItemAndProduct",
                lotCondition, null, UtilMisc.toList("lotId"), UtilMisc.toList("lotId"),
                UtilCommon.DISTINCT_READ_OPTIONS);
            if (UtilValidate.isNotEmpty(items)) {
                return EntityUtil.getFieldListFromEntityList(items, "lotId", true);
            } else {
                Debug.logWarning("Could not find issuance inventory item with parameters shipmentId = " + shipmentId +
                    ", and shipmentItemSeqId = " + shipmentItemSeqId, module);
            }
        } catch (GenericEntityException e) {
            Debug.logError("Could not retrieve issuance inventory item with parameters shipmentId = " + shipmentId +
                ", and shipmentItemSeqId = " + shipmentItemSeqId, module);
        }

        return null;
    }

    /**
     * A routine that returns the first lot ID associated with the item issuance associated with the
     * given parameters
     * @param shipmentId String
     * @param shipmentItemSeqId String
     * @param delegator GenericDelegator
     * @return String
     */
    public static String getIssuanceLotId(String shipmentId, String shipmentItemSeqId, GenericDelegator delegator) {
        List lotIdList = getIssuanceLotIdList(shipmentId, shipmentItemSeqId, delegator);

        if (UtilValidate.isNotEmpty(lotIdList)) {
            return (String)lotIdList.get(0);
        } else {
            return null;
        }
    }
}
