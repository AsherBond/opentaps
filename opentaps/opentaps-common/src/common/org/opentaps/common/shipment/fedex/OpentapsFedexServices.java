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

package org.opentaps.common.shipment.fedex;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.axis.message.RPCElement;
import org.apache.axis.message.RPCParam;
import org.apache.axis.types.NonNegativeInteger;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.shipment.thirdparty.fedex.FedexServices;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.shipping.fedex.soap.axis.*;

/**
 * Service for retreiving shipping rate from Fedex.
 */
public final class OpentapsFedexServices {

    private OpentapsFedexServices() { }

    private static final String MODULE = FedexServices.class.getName();
    private static final String CONFIG_FILE = "shipment.properties";
    private static final String DROPOFF_TYPE = "shipment.fedex.default.dropoffType";
    private static final String PACKAGING_TYPE = "shipment.fedex.default.packagingType";

    private static URL fedexSoapUrl = null;
    private static RateServiceSoapBindingStub rateInterface = null;
    static {
        try {
            fedexSoapUrl = new URL(UtilProperties.getPropertyValue(CONFIG_FILE, "shipment.fedex.connect.soap.url"));
        } catch (MalformedURLException e) {
            Debug.logError("shipment.fedex.connect.soap.url is not configured in " + CONFIG_FILE + " - defaulting to test URL at https://gatewaybeta.fedex.com:443/web-services", MODULE);
            try {
                fedexSoapUrl = new URL("https://gatewaybeta.fedex.com:443/web-services");
            } catch (MalformedURLException e1) {
                Debug.logError(e, MODULE);
            }
        }
        try {
            rateInterface = (RateServiceSoapBindingStub) new RateServiceLocator().getRateServicePort(fedexSoapUrl);
        } catch (ServiceException e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * Requests the Rate Service.
     *
     * @param dctx service <code>DispatchContext</code>
     * @param context service context <code>Map</code>
     * @return service response <code>Map</code>
     */
    public static Map<String, Object> fedexRateRequest(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        // Creating the result map
        Map<String, Object> result = null;

        // the client detail account number
        String accountNumber = UtilProperties.getPropertyValue(CONFIG_FILE, "shipment.fedex.access.accountNbr");

        if (UtilValidate.isEmpty(accountNumber)) {
            return ServiceUtil.returnFailure("accountNbr not found for Fedex rate service request.");
        }

        // the client detail meter number
        String meterNumber = UtilProperties.getPropertyValue(CONFIG_FILE, "shipment.fedex.access.meterNumber");

        // setting the customer transaction id
        String customerTransactionId = "Rating and Service";

        // setting the user credentials
        String userCredentialKey = UtilProperties.getPropertyValue(CONFIG_FILE, "shipment.fedex.access.userCredential.key");
        String userCredentialPassword = UtilProperties.getPropertyValue(CONFIG_FILE, "shipment.fedex.access.userCredential.password");

        String weightUomId = UtilProperties.getPropertyValue(CONFIG_FILE, "shipment.default.weight.uom");
        if (UtilValidate.isEmpty(weightUomId)) {
            return ServiceUtil.returnFailure("Default weightUomId not found for Fedex rate request - should be in " + CONFIG_FILE + ":shipment.default.weight.uom.");
        } else if (!("WT_lb".equals(weightUomId) || "WT_kg".equals(weightUomId))) {
            return ServiceUtil.returnFailure("WeightUomId in " + CONFIG_FILE + ":shipment.default.weight.uom must be either WT_lb or WT_kg.");
        }
        String weightType = "WT_lb".equals(weightUomId) ? "LB" : "KG";

        // check for 0 weight
        BigDecimal shippableWeight = (BigDecimal) context.get("shippableWeight");
        if (shippableWeight.signum() == 0) {
            try {
                shippableWeight = new BigDecimal(UtilProperties.getPropertyValue(CONFIG_FILE, "shipment.default.weight.value"));
            } catch (NumberFormatException ne) {
                Debug.logWarning("Default shippable weight not configured (shipment.default.weight.value), assuming 1.0" + weightUomId , MODULE);
                shippableWeight = BigDecimal.ONE;
            }
        }
        GenericValue shipFromAddress = null;
        GenericValue shipToAddress = null;
        // get the origin address
        String originationZip = null;

        // load the product store
        GenericValue productStore = ProductStoreWorker.getProductStore(((String) context.get("productStoreId")), delegator);

        // gets the default currency
        String currency = (String) productStore.get("defaultCurrencyUomId");

        try {
            // origin address is supplied to us (ex. drop shipping)
            String shippingOriginContactMechId = (String) context.get("shippingOriginContactMechId");
            if (shippingOriginContactMechId != null) {
                shipFromAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", shippingOriginContactMechId));
                if (shipFromAddress == null) {
                    // this should not happen, so return hard error if it does
                    return ServiceUtil.returnError("Drop ship postal address [" + shippingOriginContactMechId + "] does not exist.  System error.");
                }
                originationZip = shipFromAddress.getString("postalCode");
            } else {
                // get the origin address from the facility of the product store
                if ((productStore != null) && (productStore.get("inventoryFacilityId") != null)) {
                    List<GenericValue> shipLocs = delegator.findByAnd("FacilityContactMechPurpose", UtilMisc.toMap(
                                                                       "facilityId", productStore.getString("inventoryFacilityId"),
                                                                       "contactMechPurposeTypeId", "SHIP_ORIG_LOCATION"),
                                                                      UtilMisc.toList("-fromDate"));

                    if (UtilValidate.isNotEmpty(shipLocs)) {
                        shipLocs = EntityUtil.filterByDate(shipLocs);

                        GenericValue purp = EntityUtil.getFirst(shipLocs);

                        if (purp != null) {
                            shipFromAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", purp.getString("contactMechId")));

                            if (shipFromAddress != null) {
                                originationZip = shipFromAddress.getString("postalCode");
                            }
                        }
                    }
                }
            }

            if (UtilValidate.isEmpty(originationZip)) {
                return ServiceUtil.returnFailure("Unable to determine the origination ZIP");
            }
            // retrieving the countryCode
            List<GenericValue> countryCodes = Collections.emptyList();
            String originCountryCode = null;
            countryCodes = delegator.findByAndCache("CountryCode",  UtilMisc.toMap("countryAbbr", shipFromAddress.getString("countryGeoId")));
            if (UtilValidate.isNotEmpty(countryCodes)) {
                originCountryCode = EntityUtil.getFirst(countryCodes).getString("countryCode");
            } else {
                return ServiceUtil.returnFailure("Unable to determine the origin country");
            }

            // get the destination ZIP
            String destinationZip = null;
            String shippingContactMechId = (String) context.get("shippingContactMechId");

            if (UtilValidate.isNotEmpty(shippingContactMechId)) {
                shipToAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", shippingContactMechId));

                if (shipToAddress != null) {
                    destinationZip = shipToAddress.getString("postalCode");
                }
            }

            if (UtilValidate.isEmpty(destinationZip)) {
                return ServiceUtil.returnFailure("Unable to determine the destination ZIP");
            }
            String destCountryCode = null;
            countryCodes = delegator.findByAndCache("CountryCode",  UtilMisc.toMap("countryAbbr", shipToAddress.getString("countryGeoId")));
            if (UtilValidate.isNotEmpty(countryCodes)) {
                destCountryCode = EntityUtil.getFirst(countryCodes).getString("countryCode");
            } else {
                return ServiceUtil.returnFailure("Unable to determine the destination country");
            }

            GenericValue carrierShipmentMethod = delegator.findByPrimaryKeyCache("CarrierShipmentMethod", UtilMisc.toMap("partyId", (String) context.get("carrierPartyId"), "roleTypeId", (String) context.get("carrierRoleTypeId"), "shipmentMethodTypeId", (String) context.get("shipmentMethodTypeId")));

            RateRequest rateRequest = new RateRequest();
            rateRequest.setClientDetail(new ClientDetail(accountNumber, meterNumber, new Localization(locale.getLanguage(), locale.getCountry())));
            rateRequest.setWebAuthenticationDetail(new WebAuthenticationDetail(new WebAuthenticationCredential(userCredentialKey, userCredentialPassword)));
            rateRequest.setVersion(new VersionId("crs", 2, 0, 0));
            rateRequest.setTransactionDetail(new TransactionDetail(customerTransactionId, new Localization(locale.getLanguage(), locale.getCountry())));

            Address destAddress = new Address();
            List<String> streetLines = new ArrayList<String>();
            streetLines.add(shipToAddress.getString("address1"));
            if (UtilValidate.isNotEmpty(shipToAddress.getString("address2"))) {
                streetLines.add(shipToAddress.getString("address2"));
            }
            destAddress.setStreetLines(streetLines.toArray(new String[]{}));
            destAddress.setCity(shipToAddress.getString("city"));
            if (UtilValidate.isNotEmpty(shipToAddress.getString("stateProvinceGeoId"))) {
                destAddress.setStateOrProvinceCode(delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", shipToAddress.getString("stateProvinceGeoId"))).getString("geoCode"));
            }
            destAddress.setPostalCode(destinationZip);
            destAddress.setCountryCode(destCountryCode);
            rateRequest.setDestination(destAddress);

            Address originAddress = new Address();
            streetLines = new ArrayList<String>();
            streetLines.add(shipFromAddress.getString("address1"));
            if (UtilValidate.isNotEmpty(shipFromAddress.getString("address2"))) {
                streetLines.add(shipFromAddress.getString("address2"));
            }
            originAddress.setStreetLines(streetLines.toArray(new String[]{}));
            originAddress.setCity(shipFromAddress.getString("city"));
            if (UtilValidate.isNotEmpty(shipFromAddress.getString("stateProvinceGeoId"))) {
                originAddress.setStateOrProvinceCode(delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", shipFromAddress.getString("stateProvinceGeoId"))).getString("geoCode"));
            }
            originAddress.setPostalCode(originationZip);
            originAddress.setCountryCode(originCountryCode);
            rateRequest.setOrigin(originAddress);

            BigDecimal shippableQuantity = (BigDecimal) context.get("shippableQuantity");
            if (UtilValidate.isEmpty(shippableQuantity)) {
                shippableQuantity = BigDecimal.ZERO;
            }
            Weight weight = new Weight(WeightUnits.fromString(weightType), shippableWeight.setScale(1, BigDecimal.ROUND_UP).doubleValue());
            Money totalAmount = new Money(currency, ((BigDecimal) context.get("shippableTotal")).setScale(2, BigDecimal.ROUND_HALF_UP));
            RequestedPackage pckg = new RequestedPackage();
            pckg.setInsuredValue(totalAmount);
            pckg.setWeight(weight);
            rateRequest.setPackages(new RequestedPackage[]{pckg});
            rateRequest.setPackageCount(new NonNegativeInteger("1"));

            rateRequest.setCurrencyType(currency);
            rateRequest.setDropoffType(getDropoffType(UtilProperties.getPropertyValue(CONFIG_FILE, DROPOFF_TYPE)));
            rateRequest.setPackagingType(getPackagingType(UtilProperties.getPropertyValue(CONFIG_FILE, PACKAGING_TYPE)));
            rateRequest.setServiceType(getServiceType(carrierShipmentMethod.getString("carrierServiceCode")));
            rateRequest.setShipDate(new Date(System.currentTimeMillis()));
            rateRequest.setPayment(new Payment(PaymentType.SENDER, null));
            rateRequest.setRateRequestTypes(new RateRequestType[]{RateRequestType.ACCOUNT});

            BigDecimal shippingEstimateAmount = null;
            RateReply reply = null;

            logCall("getRate", "RateRequest", rateRequest);

            try {
                reply = rateInterface.getRate(rateRequest);
            } catch (RemoteException e) {
                if (e instanceof AxisFault) {
                    String errMsg = e.getMessage() + ((AxisFault) e).dumpToString();
                    Debug.logError(e, errMsg, MODULE);
                    return ServiceUtil.returnFailure(errMsg);
                } else {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnFailure(e.getMessage());
                }
            }

            if (reply != null) {
                NotificationSeverityType notifType = reply.getHighestSeverity();
                if ("SUCCESS".equalsIgnoreCase(notifType.toString())) {
                    RatedShipmentDetail[] details = reply.getRatedShipmentDetails();
                    if (details.length > 0) {
                        ShipmentRateDetail shipmentDetail = details[0].getShipmentRateDetail();
                        Money totalNetCharge = shipmentDetail.getTotalNetCharge();
                        if (totalNetCharge != null) {
                            shippingEstimateAmount = totalNetCharge.getAmount();
                        }
                    }
                } else {
                    Notification[] notifs = reply.getNotifications();
                    if (notifs.length > 0) {
                        String errMsg =  notifs[0].getCode() + ": " + notifs[0].getMessage() + " (" + notifs[0].getSeverity().toString() + ")";
                        Debug.logError(errMsg, MODULE);
                        return ServiceUtil.returnFailure(errMsg);
                    }
                }
            }

            result = ServiceUtil.returnSuccess();
            if (shippingEstimateAmount != null) {
                result.put("shippingEstimateAmount", shippingEstimateAmount);
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnFailure(e.getMessage());
        }

        return result;

    }

    /**
     * Serializes and logs outgoing XML request.
     * @param bodyElementName a <code>String</code> value
     * @param paramElementName a <code>String</code> value
     * @param rateRequest a <code>RateRequest</code> value
     */
    private static void logCall(String bodyElementName, String paramElementName, RateRequest rateRequest) {
        RPCParam rpcParam = new RPCParam("", paramElementName, rateRequest);
        RPCElement body = new RPCElement("", bodyElementName, new Object[]{rpcParam});
        Debug.logVerbose(body.toString(), MODULE);
    }

    /**
     * Factory Method to retrieve the correct PackagingType.
     *
     * @param propertyValue The property value to be compared
     * @return PackagingType The correct DropoffType object
     */
    private static PackagingType getPackagingType(String propertyValue) {
        if ("FXENV".equals(propertyValue) || "FXENV_LGL".equals(propertyValue)) {
            return PackagingType.FEDEX_ENVELOPE;
        }
        if ("FXPAK_SM".equals(propertyValue) || "FXPAK_LRG".equals(propertyValue)) {
            return PackagingType.FEDEX_PAK;
        }
        if ("FXBOX_SM".equals(propertyValue) || "FXBOX_MED".equals(propertyValue) || "FXBOX_LRG".equals(propertyValue)) {
            return PackagingType.FEDEX_BOX;
        }
        if ("FXTUBE".equals(propertyValue)) {
            return PackagingType.FEDEX_TUBE;
        }
        if ("FX10KGBOX".equals(propertyValue)) {
            return PackagingType.FEDEX_10KG_BOX;
        }
        if ("FX25KGBOX".equals(propertyValue)) {
            return PackagingType.FEDEX_25KG_BOX;
        }
        if ("YOURPACKNG".equals(propertyValue)) {
            return PackagingType.YOUR_PACKAGING;
        }
        return null;
    }

    /**
     * Factory Method to retrieve the correct ServiceType.
     *
     * @param object The property value to be compared
     * @return ServiceType The correct ServiceType object
     */
    private static ServiceType getServiceType(String object) {
        if ("EUROPEFIRSTINTERNATIONALPRIORITY".equals(object)) {
            return ServiceType.EUROPE_FIRST_INTERNATIONAL_PRIORITY;
        }
        if ("FEDEX1DAYFREIGHT".equals(object)) {
            return ServiceType.FEDEX_1_DAY_FREIGHT;
        }
        if ("FEDEX2DAY".equals(object)) {
            return ServiceType.FEDEX_2_DAY;
        }
        if ("FEDEX2DAYFREIGHT".equals(object)) {
            return ServiceType.FEDEX_2_DAY_FREIGHT;
        }
        if ("FEDEX3DAYFREIGHT".equals(object)) {
            return ServiceType.FEDEX_3_DAY_FREIGHT;
        }
        if ("FEDEXEXPRESSSAVER".equals(object)) {
            return ServiceType.FEDEX_EXPRESS_SAVER;
        }
        if ("FEDEXGROUND".equals(object)) {
            return ServiceType.FEDEX_GROUND;
        }
        if ("FIRSTOVERNIGHT".equals(object)) {
            return ServiceType.FIRST_OVERNIGHT;
        }
        if ("GROUNDHOMEDELIVERY".equals(object)) {
            return ServiceType.GROUND_HOME_DELIVERY;
        }
        if ("INTERNATIONALDISTRIBUTIONFREIGHT".equals(object)) {
            return ServiceType.INTERNATIONAL_DISTRIBUTION_FREIGHT;
        }
        if ("INTERNATIONALECONOMY".equals(object)) {
            return ServiceType.INTERNATIONAL_ECONOMY;
        }
        if ("INTERNATIONALECONOMYDISTRIBUTION".equals(object)) {
            return ServiceType.INTERNATIONAL_ECONOMY_DISTRIBUTION;
        }
        if ("INTERNATIONALECONOMYFREIGHT".equals(object)) {
            return ServiceType.INTERNATIONAL_ECONOMY_FREIGHT;
        }
        if ("INTERNATIONALFIRST".equals(object)) {
            return ServiceType.INTERNATIONAL_FIRST;
        }
        if ("INTERNATIONALPRIORITY".equals(object)) {
            return ServiceType.INTERNATIONAL_PRIORITY;
        }
        if ("INTERNATIONALPRIORITYDISTRIBUTION".equals(object)) {
            return ServiceType.INTERNATIONAL_PRIORITY_DISTRIBUTION;
        }
        if ("INTERNATIONALPRIORITYFREIGHT".equals(object)) {
            return ServiceType.INTERNATIONAL_PRIORITY_FREIGHT;
        }
        if ("PRIORITYOVERNIGHT".equals(object)) {
            return ServiceType.PRIORITY_OVERNIGHT;
        }
        if ("STANDARDOVERNIGHT".equals(object)) {
            return ServiceType.STANDARD_OVERNIGHT;
        }
        return null;
    }

    /**
     * Factory Method to retrieve the correct DropoffType.
     *
     * @param propertyValue The property value to be compared
     * @return DropoffType The correct DropoffType object
     */
    private static DropoffType getDropoffType(String propertyValue) {
        if ("BUSINESSSERVICECENTER".equals(propertyValue)) {
            return DropoffType.BUSINESS_SERVICE_CENTER;
        }
        if ("DROPBOX".equals(propertyValue)) {
            return DropoffType.DROP_BOX;
        }
        if ("REGULARPICKUP".equals(propertyValue)) {
            return DropoffType.REGULAR_PICKUP;
        }
        if ("REQUESTCOURIER".equals(propertyValue)) {
            return DropoffType.REQUEST_COURIER;
        }
        if ("STATION".equals(propertyValue)) {
            return DropoffType.STATION;
        }
        return null;
    }

}
