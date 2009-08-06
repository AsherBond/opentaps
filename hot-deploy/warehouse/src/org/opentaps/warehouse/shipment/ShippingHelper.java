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

package org.opentaps.warehouse.shipment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;

import org.opentaps.common.util.UtilCommon;

/**
 * Helper for Warehouse application Shipping section.
 * Principaly focused on picked list.
 *
 * @author     Fabien Carrion
 */
public class ShippingHelper {

    private static final String MODULE = ShippingServices.class.getName();
    private GenericDelegator delegator = null;
    private String facilityId = null;
    private int ordersTotalSize = -1;

    /**
     * Constructor initialisations.
     *
     * @param delegator the delegator object to access to the database
     * @param facilityId The id of the warehouse to work on
     */
    public ShippingHelper(GenericDelegator delegator, String facilityId) {
        this.delegator = delegator;
        this.facilityId = facilityId;
    }

    /**
     * Get a list of all OISGIRs reserved against the facility for Approved Sales Order.
     *
     * @throws GenericEntityException if a database exception occurred
     * @return the list of OISGIRs
     */
    @SuppressWarnings("unchecked")
    public List<GenericValue> getOISGIRlist()
        throws GenericEntityException {
        List<EntityExpr> generalConditions = new ArrayList<EntityExpr>();
        generalConditions.add(new EntityExpr("facilityId", EntityOperator.EQUALS, facilityId));
        generalConditions.add(new EntityExpr("statusId", EntityOperator.EQUALS, "ORDER_APPROVED"));
        generalConditions.add(new EntityExpr("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"));
        EntityConditionList conditions = new EntityConditionList(generalConditions, EntityOperator.AND);
        return delegator.findByCondition("OdrItShpGrpHdrInvResAndInvItem", conditions, null, null);
    }

    /**
     * Get a list of all order items on active picklists.
     *
     * @throws GenericEntityException if a database exception occurred
     * @return the list of active picklists
     */
    @SuppressWarnings("unchecked")
    public List<GenericValue> getActivePicklists()
        throws GenericEntityException {
        EntityConditionList conditions =  new EntityConditionList(UtilMisc.toList(
                   new EntityExpr("facilityId", EntityOperator.EQUALS, facilityId),
                   new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_CANCELLED"),
                   new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_PICKED"),
                   new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_COMPLETED"),
                   new EntityExpr("itemStatusId", EntityOperator.NOT_EQUAL, "PICKITEM_CANCELLED"),
                   new EntityExpr("itemStatusId", EntityOperator.NOT_EQUAL, "PICKITEM_COMPLETED")), EntityOperator.AND);

        return delegator.findByCondition("PicklistAndBinAndItem", conditions, null, null);
    }

    /**
     * Determine the total readyQty and shortfall for each order/shipGroup
     * combination by constructing a map.
     *
     * @return <code>Map</code> orderId -> {shipGroupSeqId -> {readyQty, shortfall, maySplit}}
     */
    @SuppressWarnings("unchecked")
    public Map getShipFalls() {
        Map shortfalls = new HashMap();
        List oisgirsAgainstFacility = null;
        try {
            oisgirsAgainstFacility = getOISGIRlist();
        } catch (GenericEntityException ex) {
            Debug.logError("Problem getting OISGIRS list " + ex.toString(), MODULE);
            return shortfalls;
        }

        Iterator oafit = oisgirsAgainstFacility.iterator();
        while (oafit.hasNext()) {
            GenericValue oisgir = (GenericValue) oafit.next();
            String orderId = oisgir.getString("orderId");
            String shipGroupSeqId = oisgir.getString("shipGroupSeqId");
            Map orderMap = shortfalls.containsKey(orderId) ? (Map) shortfalls.get(orderId) : new HashMap();
            shortfalls.put(orderId, orderMap);
            Map shipGroupMap = orderMap.containsKey(shipGroupSeqId) ? (Map) orderMap.get(shipGroupSeqId) : new HashMap();
            orderMap.put(shipGroupSeqId, shipGroupMap);
            shipGroupMap.put("maySplit", oisgir.get("maySplit"));
            shipGroupMap.put("contactMechId", oisgir.getString("contactMechId"));
            Double shortfall = new Double(0);
            Double oisgirShortfall = new Double(0);
            if (shipGroupMap.containsKey("shortfall")) {
                shortfall = (Double) shipGroupMap.get("shortfall");
            }
            if (UtilValidate.isNotEmpty(oisgir.get("quantityNotAvailable"))) {
                oisgirShortfall = oisgir.getDouble("quantityNotAvailable");
                shortfall += oisgirShortfall;
            }
            shipGroupMap.put("shortfall", shortfall);
            Double readyQty = oisgir.getDouble("quantity") - oisgirShortfall;
            if (shipGroupMap.containsKey("readyQty")) {
                readyQty += (Double) shipGroupMap.get("readyQty");
            }
            shipGroupMap.put("readyQty", readyQty);
        }

        Debug.logInfo("Shortfalls: " + shortfalls, MODULE);
        return shortfalls;
    }

    /**
     * Construct the conditions to get the orders which are ready to ship.
     *
     * @return List of conditions
     */
    @SuppressWarnings("unchecked")
    public List getOrderIsReadyConditionList() {
        List orderIsReadyConditionList = new ArrayList();
        Map shortfalls = getShipFalls();
        List onActivePicklists = null;
        try {
            onActivePicklists = getActivePicklists();
        } catch (GenericEntityException ex) {
            Debug.logError("Problem getting Active Picklists list " + ex.toString(), MODULE);
            onActivePicklists = new ArrayList();
        }

        Iterator sfit = shortfalls.keySet().iterator();
        while (sfit.hasNext()) {
            String orderId = (String) sfit.next();
            Map orderMap = (Map) shortfalls.get(orderId);
            Iterator sgit = orderMap.keySet().iterator();
            while (sgit.hasNext()) {
                String shipGroupSeqId = (String) sgit.next();
                Map shipGroupMap = (Map) orderMap.get(shipGroupSeqId);
                String maySplit = (String) shipGroupMap.get("maySplit");
                Double shortfall = (Double) shipGroupMap.get("shortfall");
                Double readyQty = (Double) shipGroupMap.get("readyQty");
                String contactMechId = (String) shipGroupMap.get("contactMechId");

                // Orders are ready if either: maySplit and readyQty > 0,
                // OR !maySplit and shortfall == 0
                // Split into two checks to ease maintenance later
                Boolean orderIsReady = false;
                if (maySplit.equals("Y") && readyQty > 0) {
                    orderIsReady = true;
                } else if (maySplit.equals("N") && shortfall == 0) {
                    orderIsReady = true;
                }
                if ("_NA_".equals(contactMechId)) {
                    orderIsReady = false;
                }

                // Check if any of the ship group's items are on an active picklist
                Boolean onActivePicklist = UtilValidate.isNotEmpty(EntityUtil.filterByAnd(onActivePicklists, UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId)));

                Debug.logInfo("Found order ship group [" + orderId + "/" + shipGroupSeqId + "] isReady=" + orderIsReady + " onActivePicklist=" + onActivePicklist, MODULE);

                if (orderIsReady && !onActivePicklist) {
                    // Create an EntityCondition for this order/shipGroup
                    Debug.logInfo("Adding condition for [" + orderId + "/" + shipGroupSeqId + "]", MODULE);
                    EntityConditionList osgCondition = new EntityConditionList(UtilMisc.toList(new EntityExpr("orderId", EntityOperator.EQUALS, orderId), new EntityExpr("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId)), EntityOperator.AND);
                    orderIsReadyConditionList.add(osgCondition);
                }
            }
        }

        return orderIsReadyConditionList;
    }

    /**
     * Get the total of orders ready to ship.
     *
     * @return total of orders ready to ship
     */
    public int getOrdersTotalSize() {
        if (ordersTotalSize == -1) {
            try {
                findOrdersReadyToShip(0, 0);
            } catch (GenericEntityException ex) {
                Debug.logError("Problem getting orders ready to ship list " + ex.toString(), MODULE);
            }
        }
        return ordersTotalSize;
    }

    /**
     * Get the whole list of orders ready to ship.
     *
     * @throws GenericEntityException if a database exception occurred
     * @return List of orders ready to ship
     */
    public List<GenericValue> findAllOrdersReadyToShip() throws GenericEntityException {
        return findOrdersReadyToShip(-1, -1);
    }

    /**
     * Get the list of orders ready to ship.
     *
     * @param viewIndex the list from viewIndex, -1 returns the whole list
     * @param viewSize viewSize orders, -1 returns the whole list
     * @throws GenericEntityException if a database exception occurred
     * @return List of orders ready to ship
     */
    @SuppressWarnings("unchecked")
    public List<GenericValue> findOrdersReadyToShip(int viewIndex, int viewSize) throws GenericEntityException {
        List<GenericValue> orders = new ArrayList<GenericValue>();
        List orderIsReadyConditionList = getOrderIsReadyConditionList();

        if (orderIsReadyConditionList.size() > 0) {
            Debug.logInfo("Found orderIsReadyConditionList: " + orderIsReadyConditionList, MODULE);
            // This condition list will result in a query like: ... WHERE (orderId=w AND shipGroupSeqId=x) OR (orderId=y AND shipGroupSeqId=z) ...
            EntityConditionList orderIsReadyConditions = new EntityConditionList(orderIsReadyConditionList, EntityOperator.OR);

            // The value of these fields are the same for all rows for a given order/shipGroup, so limiting the query to these ensures that a DISTINCT select won't
            //  result in any duplicate rows
            List fieldsToSelect = UtilMisc.toList("orderId", "shipGroupSeqId", "orderDate", "contactMechId");
            fieldsToSelect.addAll(UtilMisc.toList("carrierPartyId", "shipmentMethodTypeId", "shipByDate", "billToPartyId"));
            List orderBy = UtilMisc.toList("orderDate");

            EntityListIterator readyToShipIt = delegator.findListIteratorByCondition("OdrItShpGrpHdrInvResAndInvItem", orderIsReadyConditions, null, fieldsToSelect, orderBy, UtilCommon.DISTINCT_READ_OPTIONS);

            // Bad enough that we have to read the whole table once
            // to perform the sums and filter by facilityId, so get
            // only the partial resultset here
            if (viewIndex == -1 || viewSize == -1) {
                orders = readyToShipIt.getCompleteList();
            } else {
                orders = readyToShipIt.getPartialList(viewIndex, viewSize);
            }

            readyToShipIt.last();
            ordersTotalSize = readyToShipIt.currentIndex();
            readyToShipIt.close();
        } else {
            // If there are no conditions, then we have no applicable orders in the system, so report 0
            Debug.logWarning("orderIsReadyConditionList was empty, will return an empty list.", MODULE);
            ordersTotalSize = 0;
        }

        return orders;
    }

}
