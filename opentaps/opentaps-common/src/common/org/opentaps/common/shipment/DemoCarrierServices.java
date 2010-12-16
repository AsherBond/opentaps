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
package org.opentaps.common.shipment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Base64;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * Provides the shipment estimate service for the demo carrier.
 */
public class DemoCarrierServices {

    public static final String module = DemoCarrierServices.class.getName();

    /**
     * This service is responsible for obtaining a rate estimate for a given set of shipment data.
     * It should poll the carrier's remote API and return a map containing the estimates.
     * If there is a problem obtaining the estimate, the service should return a failure.  This is
     * because we want to allow printing "Not Available" for the rate if we can't obtain it yet.
     * However, if there is a fatal error that should provoke a crash, then an error should be returned.
     *
     * The service is invoked by a lookup on ProductStoreShipmentMeth, thus we can assume that the
     * carrier shipment methods are already configured for the store.
     *
     * The input parameters are defined in the calcShipmentEstimateInterface service definition.  For purposes
     * of demonstration, we will charge 1 USD per pound shipped via ground and 2 USD for express.  If no weight
     * is specified, we assume 1 pound.
     */
    public static Map<String, Object> demoCarrierShipmentEstimate(DispatchContext dctx, Map<String, ?> context) {
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
        BigDecimal shippableWeight = (BigDecimal) context.get("shippableWeight");

        BigDecimal weight = shippableWeight == null ? BigDecimal.ONE : shippableWeight;
        BigDecimal estimate = "GROUND".equals(shipmentMethodTypeId) ? weight : weight.multiply(new BigDecimal("2"));

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("shippingEstimateAmount", estimate);
        return results;
    }

    /**
     * This service is responsible for confirming a shipment with a carrier.  The result
     * should be an error if the shipment is not confirmed.  Otherwise the shipment route
     * segment is marked as confirmed and a tracking code, if any, is saved.
     *
     * Generally the responses include a label image which can be printed.  Here, a mock
     * label is generated for purposes of testing the label printing services in warehouse. 
     */
    public static Map<String, Object> demoCarrierConfirmShipment(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        try {
            GenericValue segment = delegator.findByPrimaryKey("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            List<GenericValue> packages = segment.getRelated("ShipmentPackageRouteSeg");
            for (GenericValue p : packages) {
                p.setBytes("labelImage", generateLabel());
                p.store();
            }
            segment.set("carrierServiceStatusId", "SHRSCS_CONFIRMED");
            segment.store();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Failed to generate a label for the demo carrier: " + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    private static byte[] generateLabel() {
        return Base64.base64Decode(LABEL_IMAGE_ENCODED.getBytes());
    }

    // this is simply a sample GIF label image encoded in base 64
    private static final String LABEL_IMAGE_ENCODED =
         "R0lGODlhaAHKAeYAAAAAAAgICBAQEBgYGCEhISkpKTExMTIyMjk5OTw8PEJCQkZGRkdHR0pKSlBQ" +
         "UFJSUlpaWlxcXF5eXmNjY2RkZGZmZmdnZ2lpaWtra2xsbG5ubnFxcXJycnNzc3R0dHV1dXl5eXt7" +
         "e319fX9/f4CAgIGBgYKCgoODg4SEhIaGhoeHh4iIiImJiYqKiouLi4yMjI2NjY6Ojo+Pj5CQkJGR" +
         "kZOTk5SUlJWVlZaWlpiYmJycnJ2dnZ6enqGhoaKioqOjo6SkpKWlpaampqenp6ioqKqqqqysrK2t" +
         "ra6urq+vr7GxsbOzs7S0tLW1tba2tre3t7i4uLu7u729vcHBwcLCwsXFxcbGxsfHx8jIyMnJycrK" +
         "ysvLy83Nzc7Ozs/Pz9XV1dbW1tjY2NnZ2dvb297e3uDg4OLi4uTk5Obm5ufn5+7u7u/v7/Dw8PHx" +
         "8fLy8vPz8/X19fb29vf39/j4+Pr6+v7+/v///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACwA" +
         "AAAAaAHKAQAH/4B2goOEhYaHiImKi4yNjo+QkZKTlJWWl5iZmpucnZ6foKGio6SlpqeoqaqrrK2u" +
         "r7CxsrO0tba3uLm6u7y9vr/AwcLDxMXGx8jJysvMzc7P0NHS09TV1tfY2drb3N3e3+Dh4uPk5ebn" +
         "6OmwRezq7u+kD/Lw9PWb8g/2+vuS+Pz/ABP5C0iQ4MCCCPcdTChtjJQoECNGhAJFIkQpTXrgwKFD" +
         "C0M7Cz868/IDyI+TKFOq/BEkR4kRJ06QGBHl0RckPnJGOWPpTJScPnZa4sKOHRc0l8QUdYK0UUhG" +
         "XnzYmKqkDKYySqbaqBppylQfVkUOOiJkiNmzaNOaLTKjBIq3KP9O0GB05oYDBnjzMoDgo42iDXix" +
         "DEICQW9evn4foakRAZ9jDk4YiZDHRdCbIo0dPxBR+ZDmzyISrbGx4IDp0wce+Bg0xbQEQxJMTxnk" +
         "ozRq0wtsrGE0xfZpELvFFlFLPC0RGCbgvmWxyMddw4Yh1EQEmIHgLxWgRxfciMfnzxbEKJr8oDKX" +
         "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";

}
