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
package org.opentaps.tests.warehouse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.warehouse.shipment.packing.PackingSession;

/**
 * Ofbiz services to test shipping, in particular packing.
 */
public final class TestShippingServices {

    private TestShippingServices() { }

    private static final String MODULE = TestShippingServices.class.getName();

    /**
     * Packs all ship groups in an order and triggers the creation of invoices
     * and packing slips for them.  Uses the packing session object to accomplish
     * these goals.
     *
     * Functionally emulates the role of the legacy quickShipEntireOrder, but using
     * a process that is standard to opentaps.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map testShipOrder(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String orderId = (String) context.get("orderId");
        String facilityId = (String) context.get("facilityId");

        // this is optional, if set only pack the given ship group
        String onlyShipGroupSeqId = (String) context.get("shipGroupSeqId");

        try {
            List<String> shipmentIds = new FastList<String>();

            // go through the reservations for this order / facility and ship all of them -- this should be sufficient to pack the entire order
            List<GenericValue> reservations = delegator.findByAnd("OrderItemShipGrpInvResAndItem", UtilMisc.toMap("orderId", orderId, "facilityId", facilityId), UtilMisc.toList("shipGroupSeqId"));

            int packageSeqId = 1;
            String lastShipGroupSeqId = "";
            List<PackingSession> packingSessions = new FastList<PackingSession>();
            PackingSession packing = null;

            // note that the reservations are in order of ship group, which is a pseudo-grouping to help the logic
            for (GenericValue reservation : reservations) {
                Debug.logInfo("Packing reservation: " + reservation, MODULE);
                String shipGroupSeqId = reservation.getString("shipGroupSeqId");
                String orderItemSeqId = reservation.getString("orderItemSeqId");
                BigDecimal quantity = reservation.getBigDecimal("quantity");

                if (UtilValidate.isNotEmpty(onlyShipGroupSeqId) && !onlyShipGroupSeqId.equals(shipGroupSeqId)) {
                    continue;
                }

                // every time the ship group rolls over to a new one, create a new packing session for this group
                if (packing == null || !lastShipGroupSeqId.equals(shipGroupSeqId)) {
                    packing = new PackingSession(dctx.getDispatcher(), userLogin, facilityId, null, orderId, shipGroupSeqId);
                    packingSessions.add(packing);
                    packageSeqId = 1;
                }

                // pack whole reservation
                Debug.logInfo("Packing item [" + orderId + "/" + shipGroupSeqId + "/" + orderItemSeqId + "] x " + quantity, MODULE);
                packing.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, reservation.getString("productId"), quantity, packageSeqId++, BigDecimal.ZERO, false);
                lastShipGroupSeqId = shipGroupSeqId;  // this better work as a copy rather than a reference copy
            }

            // complete each packing session (persists data and triggers all related services)
            for (PackingSession session : packingSessions) {
                shipmentIds.add(session.complete(true));
            }

            Map results = ServiceUtil.returnSuccess();
            results.put("shipmentIds", shipmentIds);
            return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Packs items from the given map of shipGroup=>itemSeqId=>quantity and triggers the creation of invoices
     * and packing slips for them.  Uses the packing session object to accomplish
     * these goals.
     *
     * Functionally emulates the role of the legacy quickShipEntireOrder, but using
     * a process that is standard to opentaps.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map testShipOrderManual(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String orderId = (String) context.get("orderId");
        String facilityId = (String) context.get("facilityId");
        Map<String, Map<String, BigDecimal>> items = (Map<String, Map<String, BigDecimal>>) context.get("items");

        try {
            List<String> shipmentIds = new FastList<String>();

            int packageSeqId = 1;
            List<PackingSession> packingSessions = new FastList<PackingSession>();
            PackingSession packing = null;

            // note that the reservations are in order of ship group, which is a pseudo-grouping to help the logic
            for (String shipGroupSeqId : items.keySet()) {
                Debug.logInfo("Packing items for shipGroupSeqId : " + shipGroupSeqId, MODULE);

                // every time the ship group rolls over to a new one, create a new packing session for this group
                packing = new PackingSession(dctx.getDispatcher(), userLogin, facilityId, null, orderId, shipGroupSeqId);
                packingSessions.add(packing);
                packageSeqId = 1;

                Map<String, BigDecimal> itemMap = items.get(shipGroupSeqId);
                for (String orderItemSeqId : itemMap.keySet()) {
                    BigDecimal quantity = itemMap.get(orderItemSeqId);
                    if (quantity.signum() <= 0) {
                        continue;
                    }

                    Debug.logInfo("Packing item [" + orderId + "/" + shipGroupSeqId + "/" + orderItemSeqId + "] x " + quantity, MODULE);
                    GenericValue orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
                    String productId = orderItem.getString("productId");
                    packing.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, packageSeqId++, BigDecimal.ZERO, false);
                }
            }

            // complete each packing session (persists data and triggers all related services)
            for (PackingSession session : packingSessions) {
                shipmentIds.add(session.complete(true));
            }

            Map results = ServiceUtil.returnSuccess();
            results.put("shipmentIds", shipmentIds);
            return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

}
