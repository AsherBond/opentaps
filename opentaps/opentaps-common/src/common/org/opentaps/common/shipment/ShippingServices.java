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

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.common.shipment;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.*;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.shipment.thirdparty.ups.UpsServices;
import org.opentaps.common.order.UtilOrder;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.party.PartyReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.Map;
import java.util.Locale;
import java.util.List;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Shipment-related services for Opentaps-Common.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public final class ShippingServices {

    private ShippingServices() { }

    private static final String MODULE = ShippingServices.class.getName();
    private static final String errorResource = "OpentapsErrorLabels";
    public static boolean shipmentUpsSaveCertificationInfo = "true".equals(UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.info"));
    public static String shipmentUpsSaveCertificationPath = UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.path");

    /**
     * Populate ShipmentRouteSegment information from the related OrderItemShipGroup.
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map setShipmentRouteSegmentFromShipGroup(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        try {

            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if (UtilValidate.isEmpty(shipment)) {
                String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_ShipmentNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            if (UtilValidate.isEmpty(shipment.get("primaryOrderId")) && UtilValidate.isEmpty(shipment.get("primaryShipGroupSeqId"))) {
                return ServiceUtil.returnSuccess();
            }

            GenericValue orderItemShipGroup = shipment.getRelatedOne("PrimaryOrderItemShipGroup");
            if (UtilValidate.isEmpty(orderItemShipGroup)) {
                return ServiceUtil.returnSuccess();
            }

            GenericValue shipmentRouteSegment = delegator.findByPrimaryKeyCache("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            if (UtilValidate.isEmpty(shipmentRouteSegment)) {
                String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_ShipmentRouteSegmentNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            Map updateSegmentContext = UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "userLogin", userLogin);
            updateSegmentContext.put("thirdPartyAccountNumber", orderItemShipGroup.get("thirdPartyAccountNumber"));
            updateSegmentContext.put("thirdPartyCountryGeoCode", orderItemShipGroup.get("thirdPartyCountryGeoCode"));
            updateSegmentContext.put("thirdPartyPostalCode", orderItemShipGroup.get("thirdPartyPostalCode"));

            Map updateShipmentRouteSegmentResult = dispatcher.runSync("updateShipmentRouteSegment", updateSegmentContext);
            if (ServiceUtil.isError(updateShipmentRouteSegmentResult)) {
                return updateShipmentRouteSegmentResult;
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map upperCasePostalAddress(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();

        String contactMechId = (String) context.get("contactMechId");
        try {
            GenericValue postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", contactMechId));
            if (postalAddress == null) {
                return ServiceUtil.returnSuccess();
            }
            if (postalAddress.get("address1") != null) {
                postalAddress.set("address1", postalAddress.getString("address1").toUpperCase());
            }
            if (postalAddress.get("address2") != null) {
                postalAddress.set("address2", postalAddress.getString("address2").toUpperCase());
            }
            if (postalAddress.get("city") != null) {
                postalAddress.set("city", postalAddress.getString("city").toUpperCase());
            }
            if (postalAddress.get("toName") != null) {
                postalAddress.set("toName", postalAddress.getString("toName").toUpperCase());
            }
            if (postalAddress.get("postalCode") != null) {
                postalAddress.set("postalCode", postalAddress.getString("postalCode").toUpperCase());
            }
            if (postalAddress.get("postalCodeExt") != null) {
                postalAddress.set("postalCodeExt", postalAddress.getString("postalCodeExt").toUpperCase());
            }
            postalAddress.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates a PartyCarrierAccount record if the distinct combination of partyId/carrierPartyId/accountNumber/postalCode/countryGeoCode doesn't already exist.
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map updatePartyCarrierAccountFromShipGroup(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");

        try {

            GenericValue orderItemShipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
            if (UtilValidate.isEmpty(orderItemShipGroup)) {
                String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_ShipGroupNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            String partyId = UtilOrder.getPlacingCustomerPartyId(delegator, orderItemShipGroup.getString("orderId"));
            String carrierPartyId = orderItemShipGroup.getString("carrierPartyId");
            String accountNumber = orderItemShipGroup.getString("thirdPartyAccountNumber");
            String postalCode = orderItemShipGroup.getString("thirdPartyPostalCode");
            String countryGeoCode = orderItemShipGroup.getString("thirdPartyCountryGeoCode");

            if (UtilValidate.isEmpty(accountNumber) || UtilValidate.isEmpty(partyId) || UtilValidate.isEmpty(carrierPartyId)) {
                return ServiceUtil.returnSuccess();
            }

            Map createPCAContext = UtilMisc.toMap("partyId", partyId, "carrierPartyId", carrierPartyId, "accountNumber", accountNumber,"postalCode", postalCode,"countryGeoCode", countryGeoCode);
            List partyCarrierAccounts = delegator.findByAnd("PartyCarrierAccount", createPCAContext);
            partyCarrierAccounts = EntityUtil.filterByDate(partyCarrierAccounts);

            if (UtilValidate.isNotEmpty(partyCarrierAccounts)) {
                return ServiceUtil.returnSuccess();
            }

            createPCAContext.put("userLogin", userLogin);
            Map createPCAResult = dispatcher.runSync("createPartyCarrierAccount", createPCAContext);
            if (ServiceUtil.isError(createPCAResult)) {
                return createPCAResult;
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * For a shipment checks what Ship Groups are fully packed and set their status to "OISG_PACKED".
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map setShipmentOrderShipGroupsPacked(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        try {
            List<GenericValue> shipmentOrderShipGroups = UtilOrder.getShipmentOrderShipGroups(delegator, shipmentId);
            for (GenericValue shipmentOrderShipGroup : shipmentOrderShipGroups) {
                Debug.logInfo("setShipmentOrderShipGroupsPacked: checking ShipGroup [" + shipmentOrderShipGroup.get("shipGroupSeqId") + "]", MODULE);
                if (UtilValidate.isNotEmpty(shipmentOrderShipGroup.getString("supplierPartyId"))) {
                    continue;
                }
                // check that all items of this ship group are indeed packed
                boolean fullyPacked = true;
                List<GenericValue> items = delegator.findByAnd("OrderItemAndShipGroupAssoc", UtilMisc.toMap("orderId", shipmentOrderShipGroup.get("orderId"), "shipGroupSeqId", shipmentOrderShipGroup.get("shipGroupSeqId")));
                items = UtilOrder.filterNonShippableProducts(items);
                for (GenericValue item : items) {
                    List<GenericValue> issuances = item.getRelated("ItemIssuance");
                    Debug.logInfo("setShipmentOrderShipGroupsPacked: checking item [" + item + "] found issuances: " + issuances, MODULE);
                    if (UtilValidate.isEmpty(issuances)) {
                        fullyPacked = false;
                        break;
                    }
                    // check total quantity packed match the ordered quantity
                    Double orderedQty = item.getDouble("quantity");
                    Double cancelQty = item.getDouble("cancelQuantity");
                    if (cancelQty == null) {
                        cancelQty = 0.0;
                    }
                    orderedQty -= cancelQty;
                    Double issuedQty = 0.0;
                    for (GenericValue issuance : issuances) {
                        Debug.logInfo("setShipmentOrderShipGroupsPacked found issuance: " + issuance, MODULE);
                        issuedQty += issuance.getDouble("quantity");
                    }
                    Debug.logInfo("setShipmentOrderShipGroupsPacked: ordered=" + orderedQty + " issued=" + issuedQty, MODULE);
                    if (issuedQty < orderedQty) {
                        fullyPacked = false;
                        break;
                    }
                }

                if (fullyPacked) {
                    Debug.logInfo("setShipmentOrderShipGroupsPacked: ship group fully packed.", MODULE);
                    shipmentOrderShipGroup.set("statusId", "OISG_PACKED");
                    shipmentOrderShipGroup.store();
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * This service must be run on pre-invoke of updateShipment where statusId is being changed to SHIPMENT_PACKED.
     * Note however that updateShipment has not run yet, so the shipment entity is in the pre-update state
     * and the input variables are what will be changed.
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map checkCanPack(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String shipmentId = (String) context.get("shipmentId");
        Map input = UtilMisc.toMap("shipmentId", shipmentId);
        try {
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", input);
            if (shipment == null) {
                return UtilMessage.createServiceError("OpentapsError_ShipmentNotFound", locale, input);
            }

            // if shipment is already packed, then we can ignore the rest of the checks
            if ("SHIPMENT_PACKED".equals(shipment.get("statusId"))) {
                return ServiceUtil.returnSuccess();
            }

            // make sure partyIdTo is set or is being set
            String partyIdTo = (String) context.get("partyIdTo");
            if (UtilValidate.isEmpty(partyIdTo)) {
                partyIdTo = shipment.getString("partyIdTo");
            }
            if (UtilValidate.isEmpty(partyIdTo)) {
                return UtilMessage.createServiceError("OpentapsError_ShipmentRecepientNotFound", locale);
            }

            PartyReader partyReader = new PartyReader(partyIdTo, delegator);
            if (partyReader.hasClassification("DONOTSHIP_CUSTOMERS")) {
                return ServiceUtil.returnError(UtilMessage.expandLabel("OpentapsDoNotShipCustomer", locale));
            }

            // all checks pass, so we can proceed with updating the shipment
            return ServiceUtil.returnSuccess();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map createAndConfirmReturnShipment(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String returnId = (String) context.get("returnId");

        Map result = ServiceUtil.returnSuccess();
        String shipmentId = null;

        try {

            GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));

            Map serviceResult = dispatcher.runSync("createShipmentForReturn", context);
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult;
            }
            shipmentId = (String) serviceResult.get("shipmentId");

            // Update the shipment with the returnId
            serviceResult = dispatcher.runSync("updateShipment", UtilMisc.toMap("userLogin", userLogin, "locale", locale, "shipmentId", shipmentId, "returnId", returnId));
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult;
            }

            // ShipmentPackage
            Map createShipmentPackageContext = UtilMisc.toMap("userLogin", userLogin, "locale", locale, "shipmentId", shipmentId);
            createShipmentPackageContext.put("weight", returnHeader.getDouble("estimatedWeight"));
            createShipmentPackageContext.put("weightUomId", returnHeader.getString("estimatedWeightUomId"));
            serviceResult = dispatcher.runSync("createShipmentPackage", createShipmentPackageContext);
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult;
            }
            String shipmentPackageSeqId = (String) serviceResult.get("shipmentPackageSeqId");

            // Create the ShipmentRouteSegment
            GenericValue carrierReturnService = returnHeader.getRelatedOne("CarrierReturnService");
            String carrierPartyId = carrierReturnService.getString("carrierPartyId");
            Map createShipmentRouteSegContext = UtilMisc.toMap("userLogin", userLogin, "locale", locale, "shipmentId", shipmentId);
            createShipmentRouteSegContext.put("destFacilityId", returnHeader.getString("destinationFacilityId"));
            createShipmentRouteSegContext.put("originContactMechId", returnHeader.getString("originContactMechId"));
            createShipmentRouteSegContext.put("originTelecomNumberId", returnHeader.getString("originPhoneContactMechId"));
            createShipmentRouteSegContext.put("destContactMechId", UtilCommon.getFacilityPostalAddress(delegator, returnHeader.getString("destinationFacilityId")).get("contactMechId"));
            createShipmentRouteSegContext.put("carrierPartyId", carrierPartyId);
            createShipmentRouteSegContext.put("shipmentMethodTypeId", carrierReturnService.getString("shipmentMethodTypeId"));
            createShipmentRouteSegContext.put("carrierServiceStatusId", "SHRSCS_NOT_STARTED");
            createShipmentRouteSegContext.put("currencyUomId", returnHeader.getString("currencyUomId"));
            serviceResult = dispatcher.runSync("createShipmentRouteSegment", createShipmentRouteSegContext);
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult;
            }
            String shipmentRouteSegmentId = (String) serviceResult.get("shipmentRouteSegmentId");

            // ShipmentItems and ShipmentPackageContents
            List<GenericValue> returnItems = returnHeader.getRelated("ReturnItem");
            for (GenericValue returnItem : returnItems) {
                serviceResult = dispatcher.runSync("createShipmentItem", UtilMisc.toMap("userLogin", userLogin, "locale", locale, "shipmentId", shipmentId, "productId", returnItem.getString("productId"), "quantity", returnItem.getDouble("returnQuantity")));
                if (ServiceUtil.isError(serviceResult)) {
                    return serviceResult;
                }
                String shipmentItemSeqId = (String) serviceResult.get("shipmentItemSeqId");
                serviceResult = dispatcher.runSync("createShipmentPackageContent", UtilMisc.toMap("userLogin", userLogin, "locale", locale, "shipmentId", shipmentId, "shipmentPackageSeqId", shipmentPackageSeqId, "quantity", returnItem.getDouble("returnQuantity"), "shipmentItemSeqId", shipmentItemSeqId));
                if (ServiceUtil.isError(serviceResult)) {
                    return serviceResult;
                }
            }

            // Schedule and accept the ShipmentRouteSegment
            Map confirmShipmentContext = UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "returnId", returnId, "userLogin", userLogin);
            Map confirmReturnShipmentResult = null;
            if ("UPS".equalsIgnoreCase(carrierPartyId)) {
                confirmReturnShipmentResult = dispatcher.runSync("opentaps.scheduleReturnShipmentUPS", confirmShipmentContext);
            } else {
                return ServiceUtil.returnError("Carrier not supported: " + carrierPartyId);
            }
            if (ServiceUtil.isError(confirmReturnShipmentResult)) {
                return confirmReturnShipmentResult;
            }

            // Accept the shipment
            Map acceptShipmentContext = UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "userLogin", userLogin);
            Map acceptShipmentResult = null;
            if ("UPS".equalsIgnoreCase(carrierPartyId)) {
                acceptShipmentResult = dispatcher.runSync("upsShipmentAccept", acceptShipmentContext);
            }
            if (ServiceUtil.isError(acceptShipmentResult)) {
                return acceptShipmentResult;
            }

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        result.put("shipmentId", shipmentId);
        return result;
    }

    /**
     * Creates a UPS ShipRequest tailored for a return shipment service and confirms it with UPS.
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map scheduleReturnShipmentUPS(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        if (shipmentUpsSaveCertificationInfo) {
            File shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        String returnId = (String) context.get("returnId");

        try {

            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if (UtilValidate.isEmpty(shipment)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ShipmentNotFound", context, locale, MODULE);
            }

            GenericValue shipmentRouteSegment = delegator.findByPrimaryKey("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            if (UtilValidate.isEmpty(shipmentRouteSegment)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ShipmentRouteSegmentNotFound", context, locale, MODULE);
            }

            GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            if (UtilValidate.isEmpty(returnHeader)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ReturnNotFound", context, locale, MODULE);
            }

            GenericValue carrierReturnService = returnHeader.getRelatedOne("CarrierReturnService");
            if (UtilValidate.isEmpty(carrierReturnService)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_CarrierReturnServiceNotFound", UtilMisc.toMap("carrierReturnServiceId", returnHeader.getString("carrierReturnServiceId")), locale, MODULE);
            }

            GenericValue carrierShipmentMethod = delegator.findByPrimaryKey("CarrierShipmentMethod", UtilMisc.toMap("partyId", carrierReturnService.get("carrierPartyId"), "shipmentMethodTypeId", carrierReturnService.get("shipmentMethodTypeId"), "roleTypeId", "CARRIER"));
            if (UtilValidate.isEmpty(carrierShipmentMethod)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_CarrierShipmentMethodNotFound", UtilMisc.toMap("partyId", returnHeader.getString("carrierPartyId"), "shipmentMethodTypeId", returnHeader.getString("shipmentMethodTypeId")), locale, MODULE);
            }

            if (UtilValidate.isEmpty(returnHeader.get("estimatedWeight")) || UtilValidate.isEmpty(returnHeader.get("estimatedWeightUomId"))) {
                return UtilMessage.createAndLogServiceError("OpentapsError_WeightRequiredForUPSShipments", locale, MODULE);
            }

            // Origin postal address
            GenericValue originAddress = returnHeader.getRelatedOne("PostalAddress");
            if (UtilValidate.isEmpty(originAddress)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_OriginPostalAddressRequiredForUPSReturnShipments", locale, MODULE);
            }
            GenericValue originStateProvinceGeo = originAddress.getRelatedOne("StateProvinceGeo");
            GenericValue originCountryGeo = originAddress.getRelatedOne("CountryGeo");

            // Origin phone number
            GenericValue originPhoneNumber = returnHeader.getRelatedOne("OriginTelecomNumber");
            if (UtilValidate.isEmpty(originAddress)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_OriginPhoneNumberRequiredForUPSReturnShipments", locale, MODULE);
            }

            // Destination facility and postal address
            GenericValue facility = returnHeader.getRelatedOne("Facility");
            GenericValue destAddress = UtilCommon.getFacilityPostalAddress(delegator, returnHeader.getString("destinationFacilityId"));
            GenericValue stateProvinceGeo = destAddress.getRelatedOne("StateProvinceGeo");
            GenericValue countryGeo = destAddress.getRelatedOne("CountryGeo");

            Document shipmentConfirmRequestDoc = UtilXml.makeEmptyXmlDocument("ShipmentConfirmRequest");

            // ShipmentConfirmRequest
            Element shipmentConfirmRequestElement = shipmentConfirmRequestDoc.getDocumentElement();
            shipmentConfirmRequestElement.setAttribute("xml:lang", "en-US");
            Element requestElement = UtilXml.addChildElement(shipmentConfirmRequestElement, "Request", shipmentConfirmRequestDoc);
            Element transactionReferenceElement = UtilXml.addChildElement(requestElement, "TransactionReference", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, "XpciVersion", "1.0001", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(requestElement, "RequestAction", "ShipConfirm", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(requestElement, "RequestOption", "nonvalidate", shipmentConfirmRequestDoc);

            // ShipmentConfirmRequest/Shipment
            Element shipmentElement = UtilXml.addChildElement(shipmentConfirmRequestElement, "Shipment", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipmentElement, "Description", "Return service request for return " + returnId, shipmentConfirmRequestDoc);

            // ShipmentConfirmRequest/Shipment/ReturnService
            Element returnServiceElement = UtilXml.addChildElement(shipmentElement, "ReturnService", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(returnServiceElement, "Code", carrierReturnService.getString("carrierServiceCode"), shipmentConfirmRequestDoc);

            // ShipmentConfirmRequest/Shipment/Shipper and ShipmentConfirmRequest/Shipment/ShipTo must be the same for Return Service shipments
            Element shipperElement = UtilXml.addChildElement(shipmentElement, "Shipper", shipmentConfirmRequestDoc);
            String orgPartyName = PartyHelper.getPartyName(delegator, facility.getString("ownerPartyId"), false);
            UtilXml.addChildElementValue(shipperElement, "Name", orgPartyName, shipmentConfirmRequestDoc);
            Element shipToElement = UtilXml.addChildElement(shipmentElement, "ShipTo", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, "CompanyName", orgPartyName, shipmentConfirmRequestDoc);
            for (Element el : new Element[]{shipperElement, shipToElement}) {
                UtilXml.addChildElementValue(el, "ShipperNumber", UtilProperties.getPropertyValue("shipment", "shipment.ups.shipper.number"), shipmentConfirmRequestDoc);
                Element addressElement = UtilXml.addChildElement(el, "Address", shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(addressElement, "AddressLine1", destAddress.getString("address1"), shipmentConfirmRequestDoc);
                if (UtilValidate.isNotEmpty(destAddress.getString("address2"))) {
                    UtilXml.addChildElementValue(addressElement, "AddressLine2", destAddress.getString("address2"), shipmentConfirmRequestDoc);
                }
                UtilXml.addChildElementValue(addressElement, "City", destAddress.getString("city"), shipmentConfirmRequestDoc);
                if (UtilValidate.isNotEmpty(stateProvinceGeo)) {
                    UtilXml.addChildElementValue(addressElement, "StateProvinceCode", stateProvinceGeo.getString("geoCode"), shipmentConfirmRequestDoc);
                }
                UtilXml.addChildElementValue(addressElement, "PostalCode", destAddress.getString("postalCode"), shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(addressElement, "CountryCode", countryGeo.getString("geoCode"), shipmentConfirmRequestDoc);
            }

            // ShipmentConfirmRequest/Shipment/ShipFrom
            Element shipFromElement = UtilXml.addChildElement(shipmentElement, "ShipFrom", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, "CompanyName", PartyHelper.getPartyName(delegator, returnHeader.getString("fromPartyId"), false), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPhoneNumber)) {
                String phoneNumber = originPhoneNumber.getString("contactNumber");
                if (UtilValidate.isNotEmpty(originPhoneNumber.getString("areaCode"))) {
                    phoneNumber = originPhoneNumber.getString("areaCode") + phoneNumber;
                }

                // UPS doesn't want phone country code outside North America
                if (UtilValidate.isNotEmpty(originPhoneNumber.getString("countryCode")) && !"001".equals(originPhoneNumber.getString("countryCode"))) {
                    phoneNumber = originPhoneNumber.getString("countryCode") + phoneNumber;
                }
            }
            UtilXml.addChildElementValue(shipFromElement, "PhoneNumber", PartyHelper.getPartyName(delegator, returnHeader.getString("fromPartyId"), false), shipmentConfirmRequestDoc);
            Element addressElement = UtilXml.addChildElement(shipFromElement, "Address", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(addressElement, "AddressLine1", originAddress.getString("address1"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originAddress.getString("address2"))) {
                UtilXml.addChildElementValue(addressElement, "AddressLine2", originAddress.getString("address2"), shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(addressElement, "City", originAddress.getString("city"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originStateProvinceGeo)) {
                UtilXml.addChildElementValue(addressElement, "StateProvinceCode", originStateProvinceGeo.getString("geoCode"), shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(addressElement, "PostalCode", originAddress.getString("postalCode"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(addressElement, "CountryCode", originCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);

            // ShipmentConfirmRequest/Shipment/PaymentInformation
            Element paymentInformationElement = UtilXml.addChildElement(shipmentElement, "PaymentInformation", shipmentConfirmRequestDoc);
            Element prepaidElement = UtilXml.addChildElement(paymentInformationElement, "Prepaid", shipmentConfirmRequestDoc);
            Element billShipperElement = UtilXml.addChildElement(prepaidElement, "BillShipper", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(billShipperElement, "AccountNumber", UtilProperties.getPropertyValue("shipment", "shipment.ups.bill.shipper.account.number"), shipmentConfirmRequestDoc);

            // ShipmentConfirmRequest/Shipment/Service
            Element serviceElement = UtilXml.addChildElement(shipmentElement, "Service", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(serviceElement, "Code", carrierShipmentMethod.getString("carrierServiceCode"), shipmentConfirmRequestDoc);

            // ShipmentConfirmRequest/Shipment/Package (Return Service shipments can have only one package)
            Element packageElement = UtilXml.addChildElement(shipmentElement, "Package", shipmentConfirmRequestDoc);
            Element packagingTypeElement = UtilXml.addChildElement(packageElement, "PackagingType", shipmentConfirmRequestDoc);

            // Use Customer packaging type
            UtilXml.addChildElementValue(packagingTypeElement, "Code", "02", shipmentConfirmRequestDoc);

            Element packageWeightElement = UtilXml.addChildElement(packageElement, "PackageWeight", shipmentConfirmRequestDoc);
            Element packageWeightUnitOfMeasurementElement = UtilXml.addChildElement(packageElement, "UnitOfMeasurement", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, "Code", (String) UpsServices.unitsOfbizToUps.get(returnHeader.get("estimatedWeightUomId")), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(packageWeightElement, "Weight", "" + returnHeader.getDouble("estimatedWeight").intValue(), shipmentConfirmRequestDoc);

            // ShipmentConfirmRequest/Shipment/Package/Description is required for Return Service shipments
            UtilXml.addChildElementValue(packageElement, "Description", returnId, shipmentConfirmRequestDoc);

            // ShipmentConfirmRequest/Shipment/Package/ReferenceNumber
            Element referenceNumberElement = UtilXml.addChildElement(packageElement, "ReferenceNumber", shipmentConfirmRequestDoc);
            UtilXml.addChildElement(referenceNumberElement, "BarCodeIndicator", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(referenceNumberElement, "Value", returnId, shipmentConfirmRequestDoc);

            // Assemble the request
            StringBuffer xmlString = new StringBuffer();
            xmlString.append(UtilXml.writeXmlDocument(UpsServices.createAccessRequestDocument()));
            xmlString.append(UtilXml.writeXmlDocument(shipmentConfirmRequestDoc));

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentConfirmRequest" + shipmentId + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".xml";
                FileOutputStream fileOut = new FileOutputStream(outFileName);
                fileOut.write(xmlString.toString().getBytes());
                fileOut.flush();
                fileOut.close();
            }

            // Send the request to UPS
            if (Debug.verboseOn()) {
                Debug.logVerbose(xmlString.toString(), MODULE);
            }
            String shipmentConfirmResponse = UpsServices.sendUpsRequest("ShipConfirm", xmlString.toString());

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentConfirmResponse" + shipmentId + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".xml";
                FileOutputStream fileOut = new FileOutputStream(outFileName);
                fileOut.write(shipmentConfirmResponse.getBytes());
                fileOut.flush();
                fileOut.close();
            }

            // Handle the response
            Document shipmentConfirmResponseDocument = UtilXml.readXmlDocument(shipmentConfirmResponse, false);
            return UpsServices.handleUpsShipmentConfirmResponse(shipmentConfirmResponseDocument, shipmentRouteSegment);

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (SAXException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (ParserConfigurationException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

 }
