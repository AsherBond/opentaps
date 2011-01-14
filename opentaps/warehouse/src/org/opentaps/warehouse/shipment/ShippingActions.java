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

import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.ShipmentPackageRouteDetail;
import org.opentaps.base.entities.ShipmentPackageRouteSeg;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.foundation.action.ActionContext;
import org.opentaps.foundation.entity.util.EntityListIterator;
import org.opentaps.foundation.repository.RepositoryInterface;


public class ShippingActions {
    @SuppressWarnings("unused")
    private static final String MODULE = ShippingActions.class.getName();

    private ShippingActions() {}

    public static void viewShipmentLabel(Map<String, Object> context) throws GeneralException {
        ActionContext ac = new ActionContext(context);
        DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(ac);
        RepositoryInterface repository = dd.getShippingDomain().getShippingRepository();

        ac.put("uiLabelMap", UtilMessage.getUiLabels(ac.getLocale()));

        String navigation = ac.getParameter("navigation");
        Boolean hasNavButtons = "Y".equals(navigation) ? Boolean.TRUE : Boolean.FALSE;
        ac.put("hasNavButtons", hasNavButtons);

        String shipmentId = ac.getParameter("shipmentId");
        ac.put("shipmentId", shipmentId);
        String shipmentRouteSegmentId = ac.getParameter("shipmentRouteSegmentId");
        ac.put("shipmentRouteSegmentId", shipmentRouteSegmentId);
        String shipmentPackageSeqId = ac.getParameter("shipmentPackageSeqId");
        ac.put("shipmentPackageSeqId", shipmentPackageSeqId);

        // some records have no actual image, put the flag.
        ShipmentPackageRouteSeg routeSeg = repository.findOne(ShipmentPackageRouteSeg.class,
                repository.map(
                        ShipmentPackageRouteSeg.Fields.shipmentId, shipmentId,
                        ShipmentPackageRouteSeg.Fields.shipmentRouteSegmentId, shipmentRouteSegmentId,
                        ShipmentPackageRouteSeg.Fields.shipmentPackageSeqId, shipmentPackageSeqId
                )
        );
        ac.put("hasLabelImage", Boolean.valueOf(UtilValidate.isNotEmpty(routeSeg.getLabelImage())));

        if (hasNavButtons) {
            EntityListIterator<ShipmentPackageRouteDetail> routes =
                repository.findIterator(ShipmentPackageRouteDetail.class, ShippingHelper.printLabelsConditions(), UtilMisc.<String>toList("shipmentId", "shipmentRouteSegmentId", "shipmentPackageSeqId"));

            ShipmentPackageRouteDetail spdr = null;
            while ((spdr = routes.next()) != null) {
                if (spdr.getShipmentId().equals(shipmentId) && spdr.getShipmentRouteSegmentId().equals(shipmentRouteSegmentId) && spdr.getShipmentPackageSeqId().equals(shipmentPackageSeqId)) {
                    ac.put("prev", routes.previous());
                    routes.next();
                    ac.put("next", routes.next());
                    break;
                }
            }

            routes.close();
        }
    }
}
