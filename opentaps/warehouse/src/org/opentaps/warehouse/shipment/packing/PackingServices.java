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

/* This file may contain code which has been modified from that included with the Apache-licensed OFBiz product application */
/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.warehouse.shipment.packing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.entities.Facility;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.shipping.ShippingRepositoryInterface;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * Services for Warehouse application Shipping section.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public final class PackingServices {

    private PackingServices() { }

    private static final String MODULE = PackingServices.class.getName();

    /**
     * Wrapper service for the OFBiz completePack service, plus additional warehouse-app-specific logic. Uses an
     * org.opentaps.warehouse.shipment.packing.PackingSession object which extends the OFBiz PackingSession class.
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> warehouseCompletePack(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        org.opentaps.warehouse.shipment.packing.PackingSession session = (PackingSession) context.get("packingSession");

        DomainsDirectory dd = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin)).loadDomainsDirectory();
        ShippingRepositoryInterface repo;
        Facility facility;
        try {
            repo = dd.getShippingDomain().getShippingRepository();
            facility = repo.findOneNotNullCache(Facility.class, repo.map(Facility.Fields.facilityId, session.getFacilityId()));
        } catch (FoundationException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        context.put("orderId", session.getPrimaryOrderId());
        Map<String, String> packageWeights = (Map<String, String>) context.get("packageWeights");
        org.ofbiz.shipment.packing.PackingServices.setSessionPackageWeights(session, packageWeights);
        context.remove("packageWeights");

        Map<String, String> packageTrackingCodes = (Map<String, String>) context.get("packageTrackingCodes");
        setSessionPackageTrackingCodes(session, packageTrackingCodes);
        context.remove("packageTrackingCodes");

        Map<String, String> packageBoxTypeIds = (Map<String, String>) context.get("packageBoxTypeIds");
        setSessionPackageBoxTypeIds(session, packageBoxTypeIds);
        context.remove("packageBoxTypeIds");

        String additionalShippingChargeDescription = (String) context.get("additionalShippingChargeDescription");
        session.setAdditionalShippingChargeDescription(additionalShippingChargeDescription);
        context.remove("additionalShippingChargeDescription");

        session.setHandlingInstructions((String) context.get("handlingInstructions"));

        Boolean force = (Boolean) context.get("forceComplete");
        if ("Y".equals(facility.getSkipPackOrderInventoryCheck())) {
            force = true;
            // passing it to the ofbiz service will also skip reservation checks
            context.put("forceComplete", Boolean.TRUE);
        }

        if (force == null || !force.booleanValue()) {
            List<String> errMsgs = FastList.newInstance();
            Map<String, BigDecimal> productQuantities = session.getProductQuantities();
            Set<String> keySet = productQuantities.keySet();
            for (String productId : keySet) {
                BigDecimal quantity = productQuantities.get(productId);
                Map<String, Object> serviceResult = null;
                try {
                    serviceResult = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", session.getFacilityId(), "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResult)) {
                        return serviceResult;
                    }
                } catch (GenericServiceException e) {
                    return UtilMessage.createAndLogServiceError(e, MODULE);
                }
                BigDecimal quantityOnHandTotal = (BigDecimal) serviceResult.get("quantityOnHandTotal");
                if ((UtilValidate.isNotEmpty(quantityOnHandTotal)) && quantityOnHandTotal.subtract(quantity).signum() < 0) {
                    errMsgs.add(UtilMessage.expandLabel("WarehouseErrorInventoryItemProductQOHUnderZero", locale, UtilMisc.toMap("productId", productId)));
                }
            }
            if (UtilValidate.isNotEmpty(errMsgs)) {
                return ServiceUtil.returnError(errMsgs);
            }
        }

        // Call the OFBiz completePack service. The PackingSession object passed is an opentaps PackingSession, so that when
        // PackingSession.complete() is called by completePack, the overridden method is used instead, and additional steps are performed.
        Map<String, Object> completePackResult = null;
        try {
            completePackResult = dispatcher.runSync("completePack", context);
        } catch (GenericServiceException e) {
            Debug.logError("Error calling completePack service in warehouseCompletePack", MODULE);
        }
        if (ServiceUtil.isError(completePackResult)) {
            return completePackResult;
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shipmentId", completePackResult.get("shipmentId"));
        return result;
    }

    /**
     * Sets package tracking codes in an org.opentaps.warehouse.shipment.packing.PackingSession object.
     * @param session An org.opentaps.warehouse.shipment.packing.PackingSession
     * @param packageTrackingCodes
     */
    private static void setSessionPackageTrackingCodes(PackingSession session, Map<String, String> packageTrackingCodes) {
        if (!UtilValidate.isEmpty(packageTrackingCodes)) {
            Set<String> keySet = packageTrackingCodes.keySet();
            for (String packageSeqId : keySet) {
                String packageTrackingCode = packageTrackingCodes.get(packageSeqId);
                if (UtilValidate.isNotEmpty(packageTrackingCodes)) {
                    session.setPackageTrackingCode(packageSeqId, packageTrackingCode);
                } else {
                    session.setPackageTrackingCode(packageSeqId, null);
                }
            }
        }
    }

    /**
     * Sets package boxTypeId in an org.opentaps.warehouse.shipment.packing.PackingSession object.
     * @param session An org.opentaps.warehouse.shipment.packing.PackingSession
     * @param packageBoxTypeIds
     */
    private static void setSessionPackageBoxTypeIds(PackingSession session, Map<String, String> packageBoxTypeIds) {
        if (UtilValidate.isNotEmpty(packageBoxTypeIds)) {
            Set<String> keySet = packageBoxTypeIds.keySet();
            for (String packageSeqId : keySet) {
                String packageBoxTypeId = packageBoxTypeIds.get(packageSeqId);
                if (UtilValidate.isNotEmpty(packageBoxTypeId)) {
                    session.setPackageBoxTypeId(packageSeqId, packageBoxTypeId);
                } else {
                    session.setPackageBoxTypeId(packageSeqId, null);
                }
            }
        }
    }
}
