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

package org.opentaps.warehouse.shipment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.warehouse.shipment.packing.PackingSession;

/**
 * Services for Warehouse application Shipping section.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev: 8604 $
 */
public final class ShippingServices {

    private ShippingServices() { }

    private static final String MODULE = ShippingServices.class.getName();
    public static final String warehouseResource = "warehouse";
    public static final String errorResource = "OpentapsErrorLabels";
    public static final String resource = "WarehouseUiLabels";
    private static final String SHIPMENT_PROPS = "shipment.properties";
    
    /**
     * Schedules the shipment with a carrier such that the label is generated and the
     * pickup confirmed.  Once this service is completed, the shipment is ready
     * to be labeled and handed over to the carrier.
     *
     * This version runs the carrier confirm service synchronously so that errors from the carrier
     * may be printed to screen.  It is meant to be used when shipping individual route segments.
     *
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    public static Map<String, Object> quickScheduleShipmentRouteSegmentSynch(DispatchContext dctx, Map<String, ?> context) {
        return quickScheduleShipmentRouteSegment(dctx, context, true);
    }

    /**
     * Schedules the shipment with a carrier such that the label is generated and the
     * pickup confirmed.  Once this service is completed, the shipment is ready
     * to be labeled and handed over to the carrier.
     *
     * This version runs the carrier confirm service asynchronously and is meant to be
     * used by a batch scheduling operation.  Messages or errors from the carrier
     * will eventually be logged when the response is processed.
     *
     * @param dctx DispatchContext
     * @param context Map
     */
    public static Map<String, Object> quickScheduleShipmentRouteSegmentAsynch(DispatchContext dctx, Map<String, ?> context) {
        return quickScheduleShipmentRouteSegment(dctx, context, false);
    }

    public static Map<String, Object> quickScheduleShipmentRouteSegment(DispatchContext dctx, Map<String, ?> context, boolean runSynchronously) {
        // TODO: ideally this can use a FacililtyShipmentSetting where the confirm services can be set up for a facility and carrierPartyId
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        String carrierPartyId = (String) context.get("carrierPartyId");

        try {

            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if (UtilValidate.isEmpty(shipment)) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorShipmentNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            // Check the shipment status
            if (!"SHIPMENT_PACKED".equals(shipment.getString("statusId"))) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorShipmentNotPacked", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            GenericValue shipmentRouteSegment = delegator.findByPrimaryKeyCache("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            if (UtilValidate.isEmpty(shipmentRouteSegment)) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorShipmentRouteSegmentNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            // Check the shipmentRouteSegment carrierServiceStatus
            if (!"SHRSCS_NOT_STARTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorShipmentRouteSegmentAlreadyStarted", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            if (UtilValidate.isEmpty(carrierPartyId)) {
                carrierPartyId = shipmentRouteSegment.getString("carrierPartyId");
            }

            // If the carrierPartyId is different, update the shipmentRouteSegment
            if (!carrierPartyId.equals(shipmentRouteSegment.getString("carrierPartyId"))) {

                // Make sure the carrierPartyId represents a valid carrier party
                GenericValue carrierPartyRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", carrierPartyId, "roleTypeId", "CARRIER"));
                if (UtilValidate.isEmpty(carrierPartyRole)) {
                    String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorInvalidCarrier", context, locale);
                    Debug.logError(errorMessage, MODULE);
                    return ServiceUtil.returnError(errorMessage);
                }

                Map<String, Object> updateShipmentRouteSegmentResult = dispatcher.runSync("updateShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "carrierPartyId", carrierPartyId, "userLogin", userLogin));
                if (ServiceUtil.isError(updateShipmentRouteSegmentResult)) {
                    return updateShipmentRouteSegmentResult;
                }
            }

            // if we're doing this asynchronously, write some helpful info in the log
            if (!runSynchronously) {
                Debug.logInfo("Asynchronously confirming " + carrierPartyId + " ShipmentRouteSegment with shipmentId [" + shipmentId + "] shipmentRouteSegmentId [" + shipmentRouteSegmentId + "] ", MODULE);
            }

            // confirm the shipment with the carrier, which should result in the label being generated and pickup confirmed
            Map<String, Object> confirmShipmentContext = UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "userLogin", userLogin);
            Map<String, Object> confirmShipmentResult = null;
            if (carrierPartyId.equals("DHL")) {
                if (runSynchronously) {
                    confirmShipmentResult = dispatcher.runSync("dhlShipmentConfirm", confirmShipmentContext);
                } else {
                    dispatcher.runAsync("dhlShipmentConfirm", confirmShipmentContext);
                }
            } else if (carrierPartyId.equals("FEDEX")) {
                if (runSynchronously) {
                    confirmShipmentResult = dispatcher.runSync("fedexShipRequest", confirmShipmentContext);
                } else {
                    dispatcher.runAsync("fedexShipRequest", confirmShipmentContext);
                }
            } else if (carrierPartyId.equals("UPS")) {
                if (runSynchronously) {
                    confirmShipmentResult = dispatcher.runSync("upsShipmentConfirmAndAccept", confirmShipmentContext);
                } else {
                    dispatcher.runAsync("upsShipmentConfirmAndAccept", confirmShipmentContext);
                }
            } else if (carrierPartyId.equals("DemoCarrier")) {
                if (runSynchronously) {
                    confirmShipmentResult = dispatcher.runSync("opentaps.demoCarrierConfirmShipment", confirmShipmentContext);
                } else {
                    dispatcher.runAsync("opentaps.demoCarrierConfirmShipment", confirmShipmentContext);
                }
            } else {
                return ServiceUtil.returnError("Cannot schedule shipment due to unsupported carrier: " + PartyHelper.getPartyName(delegator, carrierPartyId, false) + "  Only UPS, FEDEX, and DHL are supported");
            }
            if (runSynchronously && ServiceUtil.isError(confirmShipmentResult)) {
                return confirmShipmentResult;
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException se) {
            Debug.logError(se, se.getMessage(), MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service group to run UPS shipment confirm and accept synchronously.  Meant to be called asynchronously by scheduling service above.
     * Putting this here for now since we don't have a general place to place UPS services in opentaps.
     */
    public static Map<String, Object> upsShipmentConfirmAndAccept(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            // copy the context to avoid re-using a polluted input map
            Map<String, Object> context2 = new FastMap<String, Object>();
            context2.putAll(context);

            // confirm first, check for errors
            Map<String, Object> results = dispatcher.runSync("upsShipmentConfirm", context);
            if (ServiceUtil.isError(results)) {
                return results;
            }

            // accept second, return error or success
            return dispatcher.runSync("upsShipmentAccept", context2);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Prints shipping labels for each package of a shipment.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    public static Map<String, Object> printPackageShippingLabels(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        String printerName = (String) context.get("printerName");

        // this field really is required, but I just wanted a custom error message
        if (UtilValidate.isEmpty(printerName)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "WarehouseNoPrinterForLabel", locale));
        }

        String batchPrintScreenLocation = UtilProperties.getPropertyValue(warehouseResource, "warehouse.shipping.labels.printing.batchPrintingScreenLocation");
        if (UtilValidate.isEmpty(batchPrintScreenLocation)) {
            String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorBatchPrintScreenNotConfigured", locale);
            return ServiceUtil.returnError(errorMessage);
        }

        try {

            GenericValue shipmentRouteSegment = delegator.findByPrimaryKeyCache("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            if (UtilValidate.isEmpty(shipmentRouteSegment)) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorShipmentRouteSegmentNotFound", context, locale);
                return ServiceUtil.returnError(errorMessage);
            }

            // Retrieve the shipment packages
            EntityCondition cond = EntityCondition.makeCondition(
                    EntityCondition.makeCondition("shipmentId", EntityOperator.EQUALS, shipmentId),
                    EntityCondition.makeCondition("shipmentRouteSegmentId", EntityOperator.EQUALS, shipmentRouteSegmentId),
                    EntityCondition.makeCondition("labelImage", EntityOperator.NOT_EQUAL, null)
            );
            List<GenericValue> shipmentPackages = delegator.findByCondition("ShipmentPackageRouteSeg", cond, null, UtilMisc.toList("shipmentPackageSeqId"));

            // Assemble the parameters for the BatchPrintShippingLabels FO
            Map<String, Object> parameters = new HashMap<String, Object>();
            for (int x = 0; x < shipmentPackages.size(); x++) {
                GenericValue shipmentPackage = shipmentPackages.get(x);
                parameters.put("_rowSubmit_o_" + x, "Y");
                parameters.put("shipmentId_o_" + x, shipmentId);
                parameters.put("shipmentRouteSegmentId_o_" + x, shipmentRouteSegmentId);
                parameters.put("shipmentPackageSeqId_o_" + x, shipmentPackage.get("shipmentPackageSeqId"));
            }

            // Prefix for the label image URLs in the screen
            String urlPrefix = UtilProperties.getPropertyValue(warehouseResource, "warehouse.shipping.labels.printing.labelImage.urlPrefix");

            // determine label content type
            String labelContentType = "image/gif";
            String carrierPartyId = shipmentRouteSegment.getString("carrierPartyId");
            if ("DHL".equals(carrierPartyId)) {
                labelContentType = "image/png"; //default for DHL
                // get preference (PNG or GIF)
                String labelImagePreference = UtilProperties.getPropertyValue(SHIPMENT_PROPS, "shipment.dhl.label.image.format");
                if (UtilValidate.isNotEmpty(labelImagePreference)) {
                    if ("GIF".equals(labelImagePreference)) {
                        labelContentType = "image/gif";
                    }
                }
            } else if ("FEDEX".equals(carrierPartyId)) {
                labelContentType = "image/png";
                String labelImageType = UtilProperties.getPropertyValue(SHIPMENT_PROPS, "shipment.fedex.labelImageType");
                if ("PDF".equals(labelImageType)) {
                    return UtilMessage.createAndLogServiceError("Carrier's label in PDF is not supported.", MODULE);
                }
            }

            // Call the sendPrintFromScreen service on the BatchPrintShippingLabels screen, which will print the aggregated package labels
            // Service is called synchronously so that the service will fail if the labels fail to print
            Map<String, Object> sendPrintFromScreenContext = UtilMisc.toMap("screenLocation", batchPrintScreenLocation, "screenContext", UtilMisc.toMap("parameters", parameters, "urlPrefix", urlPrefix, "imageContentType", labelContentType), "printerName", printerName, "locale", locale, "userLogin", userLogin);
            Map<String, Object> sendPrintFromScreenResult = dispatcher.runSync("sendPrintFromScreen", sendPrintFromScreenContext);
            if (ServiceUtil.isError(sendPrintFromScreenResult)) {
                return sendPrintFromScreenResult;
            }

            // Update the labelPrinted flag of the ShipmentPackageRouteSeg
            for (int x = 0; x < shipmentPackages.size(); x++) {
                GenericValue shipmentPackage = shipmentPackages.get(x);
                Map<String, Object> updateContext = UtilMisc.toMap("labelPrinted", "Y", "shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentPackageSeqId", shipmentPackage.get("shipmentPackageSeqId"), "locale", locale, "userLogin", userLogin);
                Map<String, Object> updateResult = dispatcher.runSync("updateShipmentPackageRouteSeg", updateContext);
                if (ServiceUtil.isError(updateResult)) {
                    return updateResult;
                }
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException se) {
            Debug.logError(se, se.getMessage(), MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> insurePackedShipment(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        boolean setValues = "true".equals(UtilProperties.getPropertyValue(warehouseResource, "warehouse.package.insured.setPackageInsuredValues"));
        if (!setValues) {
            String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorNotSetPackageValuesTurnedOff", context, locale);
            Debug.logInfo(errorMessage, MODULE);
            return ServiceUtil.returnSuccess();
        }

        String shipmentId = (String) context.get("shipmentId");

        try {

            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if (UtilValidate.isEmpty(shipment)) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorShipmentNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            if (!"SALES_SHIPMENT".equals(shipment.getString("shipmentTypeId"))) {
                return ServiceUtil.returnSuccess();
            }

            String currencyUomId = shipment.getString("currencyUomId");
            if (UtilValidate.isEmpty(currencyUomId)) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorNotSetPackageValuesNoCurrency", context, locale);
                Debug.logInfo(errorMessage, MODULE);
                return ServiceUtil.returnFailure(errorMessage);
            }

            // first get the threshold
            String thresholdStr = UtilProperties.getPropertyValue("warehouse.properties", "warehouse.package.insured.threshold");
            if (thresholdStr == null) {
                return ServiceUtil.returnSuccess();
            }
            BigDecimal threshold = null;
            try {
                threshold = new BigDecimal(thresholdStr);
            } catch (NumberFormatException e) {
                Debug.logError("Cannot insure package:  warehouse.package.insured.threshold defines unkown currency amount [" + thresholdStr + "]", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // get the package values and set the insured value if it is greater than the threshold
            List<GenericValue> packages = delegator.findByAnd("ShipmentPackage", UtilMisc.toMap("shipmentId", shipmentId));
            Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "shipmentId", shipmentId, "currencyUomId", currencyUomId);
            for (Iterator<GenericValue> iter = packages.iterator(); iter.hasNext();) {
                GenericValue pkg = iter.next();
                input.put("shipmentPackageSeqId", pkg.get("shipmentPackageSeqId"));
                Map<String, Object> results = dispatcher.runSync("getShipmentPackageValueFromOrders", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
                if (UtilValidate.isEmpty(results.get("packageValue"))) {
                    String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorNotSetPackageValueNoValue", context, locale);
                    Debug.logInfo(errorMessage, MODULE);
                    return ServiceUtil.returnFailure(errorMessage);
                }
                BigDecimal packageValue = (BigDecimal) results.get("packageValue");
                if (packageValue.compareTo(threshold) < 0) {
                    String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorNotSetPackageValueTooLow", context, locale);
                    Debug.logInfo(errorMessage, MODULE);
                    Debug.logInfo("packageValue = " + packageValue + ", threshold = " + threshold, MODULE);
                    return ServiceUtil.returnSuccess();
                }

                Map<String, Object> updateShipmentPackageContext = UtilMisc.<String, Object>toMap("shipmentId", shipmentId, "shipmentPackageSeqId", pkg.getString("shipmentPackageSeqId"), "insuredValue", packageValue, "userLogin", userLogin, "locale", locale);
                Map<String, Object> updateShipmentPackageResult = dispatcher.runSync("updateShipmentPackage", updateShipmentPackageContext);
                if (ServiceUtil.isError(updateShipmentPackageResult)) {
                    return updateShipmentPackageResult;
                }
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Sets shipment currency. Defaults to the baseCurrencyUomId of the PartyAcctgPreference of the owner party of the shipment's origin facility.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    public static Map<String, Object> setShipmentCurrency(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String shipmentId = (String) context.get("shipmentId");
        String currencyUomId = (String) context.get("currencyUomId");

        try {

            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if (UtilValidate.isEmpty(shipment)) {
                String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorShipmentNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            if (UtilValidate.isEmpty(currencyUomId)) {
                GenericValue originFacility = shipment.getRelatedOne("OriginFacility");
                if (UtilValidate.isEmpty(originFacility)) {
                    String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorNotSetCurrencyNoFacility", context, locale);
                    Debug.logInfo(errorMessage, MODULE);
                    return ServiceUtil.returnFailure(errorMessage);
                }

                String ownerPartyId = originFacility.getString("ownerPartyId");
                GenericValue partyAcctgPreference = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", ownerPartyId));
                if (UtilValidate.isEmpty(partyAcctgPreference) || UtilValidate.isEmpty(partyAcctgPreference.getString("baseCurrencyUomId"))) {
                    String errorMessage = UtilProperties.getMessage(resource, "WarehouseErrorNotSetCurrencyNoCurrency", context, locale);
                    Debug.logInfo(errorMessage, MODULE);
                    return ServiceUtil.returnFailure(errorMessage);
                }
                currencyUomId = partyAcctgPreference.getString("baseCurrencyUomId");
            }

            Map<String, Object> updateShipmentResult = dispatcher.runSync("updateShipment", UtilMisc.toMap("shipmentId", shipmentId, "currencyUomId", currencyUomId, "userLogin", userLogin, "locale", locale));
            if (ServiceUtil.isError(updateShipmentResult)) {
                return updateShipmentResult;
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException se) {
            Debug.logError(se, se.getMessage(), MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> calcPackSessionAdditionalShippingCharge(DispatchContext dctx, Map<String, Object> context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        String weightUomId = (String) context.get("weightUomId");
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        String carrierRoleTypeId = (String) context.get("carrierRoleTypeId");
        String productStoreId = (String) context.get("productStoreId");

        session.setWeightUomId(weightUomId);
        BigDecimal estimatedShipCost = session.getShipmentCostEstimate(shippingContactMechId, shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId, productStoreId);
        if (UtilValidate.isNotEmpty(estimatedShipCost)) {
            session.setAdditionalShippingCharge(estimatedShipCost);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("additionalShippingCharge", estimatedShipCost);
        return result;
    }


}
