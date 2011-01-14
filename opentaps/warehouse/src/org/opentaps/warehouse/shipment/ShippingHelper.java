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
package org.opentaps.warehouse.shipment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.ShipmentPackageRouteDetail;
import org.opentaps.common.util.UtilCommon;

/**
 * Helper for Warehouse application Shipping section.
 * Principally focused on picked list.
 *
 * @author     Fabien Carrion
 */
public class ShippingHelper {

    private static final String MODULE = ShippingServices.class.getName();
    private Delegator delegator = null;
    private String facilityId = null;
    private int ordersTotalSize = -1;

    /**
     * Constructor initializations.
     *
     * @param delegator the delegator object to access to the database
     * @param facilityId The id of the warehouse to work on
     */
    public ShippingHelper(Delegator delegator, String facilityId) {
        this.delegator = delegator;
        this.facilityId = facilityId;
    }

    /**
     * Get a list of all OISGIRs reserved against the facility for Approved Sales Order.
     *
     * @throws GenericEntityException if a database exception occurred
     * @return the list of OISGIRs
     */
    public List<GenericValue> getOISGIRlist()
        throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
            EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_APPROVED"),
            EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"));
        return delegator.findByCondition("OdrItShpGrpHdrInvResAndInvItem", conditions, null, null);
    }

    /**
     * Get a list of all order items on active picklists.
     *
     * @throws GenericEntityException if a database exception occurred
     * @return the list of active picklists
     */
    public List<GenericValue> getActivePicklists()
        throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                   EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                   EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_CANCELLED"),
                   EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_PICKED"),
                   EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_COMPLETED"),
                   EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "PICKITEM_CANCELLED"),
                   EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "PICKITEM_COMPLETED"));

        return delegator.findByCondition("PicklistAndBinAndItem", conditions, null, null);
    }

    /**
     * Determine the total readyQty and shortfall for each order/shipGroup
     * combination by constructing a map.
     *
     * @return <code>Map</code> orderId -> {shipGroupSeqId -> {readyQty, shortfall, maySplit}}
     */
    public Map<String, Map<String, Map<String, Object>>> getShipFalls() {
        Map<String, Map<String, Map<String, Object>>> shortfalls = new HashMap<String, Map<String, Map<String, Object>>>();
        List<GenericValue> oisgirsAgainstFacility = null;
        try {
            oisgirsAgainstFacility = getOISGIRlist();
        } catch (GenericEntityException ex) {
            Debug.logError("Problem getting OISGIRS list " + ex.toString(), MODULE);
            return shortfalls;
        }

        Iterator<GenericValue> oafit = oisgirsAgainstFacility.iterator();
        while (oafit.hasNext()) {
            GenericValue oisgir = oafit.next();
            String orderId = oisgir.getString("orderId");
            String shipGroupSeqId = oisgir.getString("shipGroupSeqId");
            Map<String, Map<String, Object>> orderMap = shortfalls.containsKey(orderId) ? (Map<String, Map<String, Object>>) shortfalls.get(orderId) : new HashMap<String, Map<String, Object>>();
            shortfalls.put(orderId, orderMap);
            Map<String, Object> shipGroupMap = orderMap.containsKey(shipGroupSeqId) ? (Map<String, Object>) orderMap.get(shipGroupSeqId) : new HashMap<String, Object>();
            orderMap.put(shipGroupSeqId, shipGroupMap);
            shipGroupMap.put("maySplit", oisgir.get("maySplit"));
            shipGroupMap.put("contactMechId", oisgir.getString("contactMechId"));
            BigDecimal shortfall = BigDecimal.ZERO;
            BigDecimal oisgirShortfall = BigDecimal.ZERO;
            if (shipGroupMap.containsKey("shortfall")) {
                shortfall = (BigDecimal) shipGroupMap.get("shortfall");
            }
            if (UtilValidate.isNotEmpty(oisgir.get("quantityNotAvailable"))) {
                oisgirShortfall = oisgir.getBigDecimal("quantityNotAvailable");
                shortfall = shortfall.add(oisgirShortfall);
            }
            shipGroupMap.put("shortfall", shortfall);
            BigDecimal readyQty = oisgir.getBigDecimal("quantity").subtract(oisgirShortfall);
            if (shipGroupMap.containsKey("readyQty")) {
                readyQty = readyQty.add((BigDecimal) shipGroupMap.get("readyQty"));
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
    public List<EntityCondition> getOrderIsReadyConditionList() {
        List<EntityCondition> orderIsReadyConditionList = new ArrayList<EntityCondition>();
        Map<String, Map<String, Map<String, Object>>> shortfalls = getShipFalls();
        List<GenericValue> onActivePicklists = null;
        try {
            onActivePicklists = getActivePicklists();
        } catch (GenericEntityException ex) {
            Debug.logError("Problem getting Active Picklists list " + ex.toString(), MODULE);
            onActivePicklists = new ArrayList<GenericValue>();
        }

        Iterator<String> sfit = shortfalls.keySet().iterator();
        while (sfit.hasNext()) {
            String orderId = sfit.next();
            Map<String, Map<String, Object>> orderMap = shortfalls.get(orderId);
            Iterator<String> sgit = orderMap.keySet().iterator();
            while (sgit.hasNext()) {
                String shipGroupSeqId = sgit.next();
                Map<String, Object> shipGroupMap = orderMap.get(shipGroupSeqId);
                String maySplit = (String) shipGroupMap.get("maySplit");
                BigDecimal shortfall = (BigDecimal) shipGroupMap.get("shortfall");
                BigDecimal readyQty = (BigDecimal) shipGroupMap.get("readyQty");
                String contactMechId = (String) shipGroupMap.get("contactMechId");

                // Orders are ready if either: maySplit and readyQty > 0,
                // OR !maySplit and shortfall == 0
                // Split into two checks to ease maintenance later
                Boolean orderIsReady = false;
                if (maySplit.equals("Y") && readyQty.signum() > 0) {
                    orderIsReady = true;
                } else if (maySplit.equals("N") && shortfall.signum() == 0) {
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
                    EntityCondition osgCondition = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId));
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
    public List<GenericValue> findOrdersReadyToShip(int viewIndex, int viewSize) throws GenericEntityException {
        List<GenericValue> orders = new ArrayList<GenericValue>();
        List<EntityCondition> orderIsReadyConditionList = getOrderIsReadyConditionList();

        if (orderIsReadyConditionList.size() > 0) {
            Debug.logInfo("Found orderIsReadyConditionList: " + orderIsReadyConditionList, MODULE);
            // This condition list will result in a query like: ... WHERE (orderId=w AND shipGroupSeqId=x) OR (orderId=y AND shipGroupSeqId=z) ...
            EntityCondition orderIsReadyConditions = EntityCondition.makeCondition(orderIsReadyConditionList, EntityOperator.OR);

            // The value of these fields are the same for all rows for a given order/shipGroup, so limiting the query to these ensures that a DISTINCT select won't
            //  result in any duplicate rows
            List<String> fieldsToSelect = UtilMisc.toList("orderId", "shipGroupSeqId", "orderDate", "contactMechId");
            fieldsToSelect.addAll(UtilMisc.toList("carrierPartyId", "shipmentMethodTypeId", "shipByDate", "billToPartyId"));
            List<String> orderBy = UtilMisc.toList("orderDate");

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

    /**
     * Build condition list to find shipping labels to print.
     */
    public static List<EntityCondition> printLabelsConditions() {
        return UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition(ShipmentPackageRouteDetail.Fields.labelPrinted.name(), "N"),
                        EntityCondition.makeCondition(ShipmentPackageRouteDetail.Fields.labelPrinted.name(), null)
                ), EntityOperator.OR),
                EntityCondition.makeCondition(ShipmentPackageRouteDetail.Fields.carrierServiceStatusId.name(), EntityOperator.IN, UtilMisc.<String>toList(StatusItemConstants.ShprtsgCsStatus.SHRSCS_ACCEPTED, StatusItemConstants.ShprtsgCsStatus.SHRSCS_CONFIRMED))
        );
    }
}
