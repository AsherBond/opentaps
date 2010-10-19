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

package org.opentaps.amazon;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.amazon.soap.AmazonSoapClient;

/**
 * Constants for Amazon integration.
 */
public final class AmazonConstants {

    private AmazonConstants() { }

    // Resource bundles
    public static final String configResource = "amazon";
    public static final String labelResource = "AmazonUiLabels";
    public static final String errorResource = "AmazonErrorUiLabels";

    // Standard description field contents
    public static final String createdByAmazonApp = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.createdByAmazonApplication");

    // URL
    public static final String url = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.url");

    // Credentials
    public static final String merchantIdentifier = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.merchantIdentifier");
    public static final String merchantName = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.merchantName");
    public static final String userName = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.userName");
    public static final String password = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.password");

    // SOAP client
    public static final AmazonSoapClient soapClient = new AmazonSoapClient(userName, password);

    // Amazon's timezone
    public static final TimeZone amazonTimeZone = TimeZone.getTimeZone("PST");

    // Tax type data
    public static final String[] taxTypes = new String[]{"ItemTaxData", "ShippingTaxData"};
    public static final String[] taxAmountTypes = new String[]{"TaxableAmounts", "NonTaxableAmounts", "ZeroRatedAmounts", "TaxCollectedAmounts"};
    public static final String[] taxJurisdictionTypes = new String[]{"District", "City", "County", "State"};

    // Order import
    public static final String productStoreId = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.productStoreId");
    public static final boolean approveOrders = "true".equalsIgnoreCase(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.approveOrders", ""));
    public static final boolean requireTaxAuthority = "true".equalsIgnoreCase(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.requireTaxAuthority", ""));
    public static final String[] emailContactMechPurposes = new String[]{"PRIMARY_EMAIL"};
    public static final String[] customerPhoneContactMechPurposes = new String[]{"PRIMARY_PHONE", "PHONE_SHIPPING"};
    public static final String[] shippingAddressContactMechPurposes = new String[]{"GENERAL_LOCATION", "PRIMARY_LOCATION", "SHIPPING_LOCATION"};
    public static final String[] orderPartyRoleTypeIds = new String[]{"BILL_TO_CUSTOMER", "END_USER_CUSTOMER", "PLACING_CUSTOMER", "SHIP_TO_CUSTOMER"};
    public static final List<String> taxComponentTypes = Arrays.asList("Tax", "ShippingTax");
    public static final int docDownloadRetryThreshold = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.document.retry.threshold.download"));
    public static final int docExtractRetryThreshold = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.document.retry.threshold.extract"));
    public static final int docAcknowledgeRetryThreshold = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.document.retry.threshold.acknowledge"));
    public static final int orderImportRetryThreshold = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.order.retry.threshold.import"));
    public static final int orderAcknowledgeRetryThreshold = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.order.retry.threshold.acknowledge"));

    // Product export
    public static final int productPostRetryThreshold = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.export.product.retry.threshold.post"));
    public static final int productFeedMaxBulletPoints = 5;
    public static final int productFeedMaxSearchTerms = 4; // Technically 5, but one is used for the product name
    public static final int productFeedMaxUsedFor = 5;
    public static final int productFeedMaxOtherItemAttributes = 5;
    public static final int productFeedMaxTargetAudience = 3;
    public static final String productConditionType = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.export.product.conditionType");
    public static final boolean useProductIdAsSKU = "true".equalsIgnoreCase(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.export.product.useProductIdAsSKU", ""));
    public static final boolean useUPCAsSKU = "true".equalsIgnoreCase(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.export.product.useUPCAsSKU", ""));
    public static final String productDescriptionField = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.product.descriptionField");

    // Regular expression for parsing telephone numbers
    private static String patternString = null;
    static {
        patternString = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.phone.regexp");

        // Backslashes need to be doubled in the properties file, so undouble them here
        patternString = patternString.replaceAll("\\\\\\\\", "\\");
    }
    public static final Pattern phoneNumberPattern = Pattern.compile(patternString);
    public static final int phoneNumberPatternCountryCodeGroup = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.phone.regexp.group.countryCode"));
    public static final int phoneNumberPatternAreaCodeGroup = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.phone.regexp.group.areaCode"));
    public static final int phoneNumberPatternPhoneNumberGroup = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.phone.regexp.group.phoneNumber"));
    public static final int phoneNumberPatternExtensionGroup = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.phone.regexp.group.extension"));

    // Precision and rounding
    public static final int decimals = UtilNumber.getBigDecimalScale(configResource, "opentaps.amazon.import.decimalPrecision");
    public static final int rounding = UtilNumber.getBigDecimalRoundingMode(configResource, "opentaps.amazon.import.decimalRounding");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(decimals, rounding);

    // Shipping
    public static final Map<String, String> shipmentMethodTypeIds = new HashMap<String, String>();
    static {
        shipmentMethodTypeIds.put("Standard", UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.shipmentMethodTypeId.Standard"));
        shipmentMethodTypeIds.put("Expedited", UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.shipmentMethodTypeId.Expedited"));
    }
    public static final Map<String, String> carrierPartyIds = new HashMap<String, String>();
    static {
        carrierPartyIds.put("Standard", UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.carrierPartyId.Standard"));
        carrierPartyIds.put("Expedited", UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.carrierPartyId.Expedited"));
    }
    public static final Map<String, Integer> maxDaysToShip = new HashMap<String, Integer>();
    static {
        maxDaysToShip.put("Standard", Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.maxDaysToShip.Standard")));
        maxDaysToShip.put("Expedited", Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.import.maxDaysToShip.Expedited")));
    }
    public static final String partyIdFedex = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.shipping.carrierPartyId.fedEx");
    public static final String partyIdUPS = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.shipping.carrierPartyId.ups");
    public static final String partyIdUSPS = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.shipping.carrierPartyId.usps");
    public static final Map<String, String> carrierPartyIdToCode = new HashMap<String, String>();
    static {
        carrierPartyIdToCode.put(partyIdFedex, "FedEx");
        carrierPartyIdToCode.put(partyIdUPS, "UPS");
        carrierPartyIdToCode.put(partyIdUSPS, "USPS");
    }

    // Feed MessageType values
    public static final String messageTypeProduct = "Product";
    public static final String messageTypePrice = "Price";
    public static final String messageTypeProductImage = "ProductImage";
    public static final String messageTypeInventory = "Inventory";

    // StatusIds
    public static String statusDocDownloaded = "AMZN_DOC_DLED";
    public static String statusDocDownloadError = "AMZN_DOC_DL_ERR";
    public static String statusDocExtracted = "AMZN_DOC_XTRED";
    public static String statusDocExtractedError = "AMZN_DOC_XTRED_ERR";
    public static String statusOrderCreated = "AMZN_ORDR_CREATED";
    public static String statusOrderImported = "AMZN_ORDR_IMPTD";
    public static String statusOrderImportedError = "AMZN_ORDR_IMPTD_ERR";
    public static String statusOrderCancelled = "AMZN_ORDR_CANCELLED";
    public static String downloadAckSuccessResult = "_SUCCESSFUL_";
    public static String docProcessingDoneResult = "_DONE_";
    public static String procReportResultCodeError = "Error";

    public static String statusProductCreated = "AMZN_PROD_CREATED";
    public static String statusProductChanged = "AMZN_PROD_CHANGED";
    public static String statusProductPosted = "AMZN_PROD_POSTED";
    public static String statusProductSuccess = "AMZN_PROD_SUCCESS";
    public static String statusProductError = "AMZN_PROD_ERROR";
    public static String statusProductAckRecv = "AMZN_PROD_ACK_RECV";
    public static String statusProductAckError = "AMZN_PROD_ACK_ERR";
    public static String statusProductNotAcked = "AMZN_PROD_NOT_ACKED";
    public static String statusProductDeleted = "AMZN_PROD_DELETED";
    public static String statusProductDeleteError = "AMZN_PROD_DEL_ERR";

    // Acknowledgement statusIds
    public static String statusDocNotAcknowledged = "AMZN_DOC_NOT_ACKED";
    public static String statusDocAcknowledged = "AMZN_DOC_ACKED";
    public static String statusDocAcknowledgedError = "AMZN_DOC_ACK_ERR";
    public static String statusOrderNotAcknowledged = "AMZN_ORDR_NOT_ACKED";
    public static String statusOrderAckSent = "AMZN_ORDR_ACK_SENT";
    public static String statusOrderAckFailureSent = "AMZN_ORDR_ACK_FL_ST";
    public static String statusOrderSuccessAcknowledged = "AMZN_ORDR_SCSS_ACKED";
    public static String statusOrderFailureAcknowledged = "AMZN_ORDR_FAIL_ACKED";
    public static String statusOrderSuccessAcknowledgementError = "AMZN_ORDR_SC_ACK_ERR";
    public static String statusOrderFailureAcknowledgementError = "AMZN_ORDR_FL_ACK_ERR";
    public static String statusOrderShipNotAcked = "AMZN_SHIP_NOT_ACKED";
    public static String statusOrderShipAckSent = "AMZN_SHIP_ACK_SENT";
    public static String statusOrderShipAcknowledged = "AMZN_SHIP_ACKED";
    public static String statusOrderShipAcknowledgedError = "AMZN_SHIP_ACK_ERR";

    // Email info
    public static final boolean sendErrorEmails = "true".equalsIgnoreCase(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.sendErrorEmails", ""));
    public static Locale errorEmailLocale = UtilMisc.parseLocale(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.errorEmailLocale"));
    public static String errorEmailScreenUriOrderAckValidate = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.errorEmailScreenUri.orders.orderAckValidate");
    public static String errorEmailScreenUriOrderItemFulfillValidate = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.errorEmailScreenUri.orders.orderItemFulfillValidate");
    public static String errorEmailScreenUriOrders = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.errorEmailScreenUri.orders");
    public static String errorEmailScreenUriProducts = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.errorEmailScreenUri.products");
    public static String errorEmailTo = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.toAddress");
    public static String errorEmailFrom = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.fromAddress");
    public static final int linesForBulkErrorEmails = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.linesForBulkErrorEmails"));

    // Age threshold for warning of the last successful product feed processing document download
    public static final int lastProcDocCheckAge = Integer.parseInt(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.productFeedProcessingAgeWarning.thresholdHours"));
    public static String errorEmailScreenUriProcDocAgeWarning = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.error.email.errorEmailScreenUri.productFeedProcessingAgeWarning");

    // Maping between Amazon and Opentaps prices
    public static String priceStandard = "LIST_PRICE";
    public static String priceMAP = null;
    public static String priceSale = "DEFAULT_PRICE";
    public static String priceProductStoreGroup = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.export.product.price.productStoreGroupId");
    public static final boolean delistProductIfNoSalePrice = "true".equalsIgnoreCase(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.export.product.price.delistProductIfNoSalePrice", ""));

    // Units of measure and default units
    public static final Map<String, String> units = new HashMap<String, String>();
    static {
        units.put("LEN_cm", "CM");
        units.put("LEN_ft", "FT");
        units.put("LEN_in", "IN");
        units.put("LEN_m", "M");
        units.put("LEN_mm", "MM");
        units.put("WT_g", "GR");
        units.put("WT_kg", "KG");
        units.put("WT_oz", "OZ");
        units.put("WT_lb", "LB");
    }
    public static String defaultLengthUom = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.uom.length");
    public static String defaultWeightUom = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.uom.weight");

    // ImageTypes for product images
    public static Map<String, String> imageTypes = new HashMap<String, String>();
    static {
        String main = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.image.main", null);
        if (UtilValidate.isNotEmpty(main)) imageTypes.put("Main", main);
        String pt1 = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.image.pt1", null);
        if (UtilValidate.isNotEmpty(pt1)) imageTypes.put("PT1", pt1);
        String pt2 = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.image.pt2", null);
        if (UtilValidate.isNotEmpty(pt2)) imageTypes.put("PT2", pt2);
        String pt3 = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.image.pt3", null);
        if (UtilValidate.isNotEmpty(pt3)) imageTypes.put("PT3", pt3);
        String pt4 = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.image.pt4", null);
        if (UtilValidate.isNotEmpty(pt4)) imageTypes.put("PT4", pt4);
        String pt5 = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.image.pt5", null);
        if (UtilValidate.isNotEmpty(pt5)) imageTypes.put("PT5", pt5);
        String pt6 = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.image.pt6", null);
        if (UtilValidate.isNotEmpty(pt6)) imageTypes.put("PT6", pt6);
        String pt7 = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.image.pt7", null);
        if (UtilValidate.isNotEmpty(pt7)) imageTypes.put("PT7", pt7);
        String pt8 = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.image.pt8", null);
        if (UtilValidate.isNotEmpty(pt8)) imageTypes.put("PT8", pt8);
        String search = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.default.image.search", null);
        if (UtilValidate.isNotEmpty(search)) imageTypes.put("Search", search);
    }

    // Default prefix for image URL
    public static final String imageUrlPrefix = UtilProperties.getPropertyValue(configResource, "opentaps.amazon.image.urlRoot", "");

    // Inventory posting controls
    public static final boolean postActualInventory = "true".equalsIgnoreCase(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.export.inventory.postActualInventory", ""));
    public static final boolean postInventoryDaysToShip = "true".equalsIgnoreCase(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.export.inventory.postInventoryDaysToShip", ""));
    public static final boolean inventoryIsAvailableIfMinimumStock = "true".equalsIgnoreCase(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.export.inventory.inventoryIsAvailableIfMinimumStock", ""));

    // GoodIdentificationTypeId -> Amazon StandardProductId mapping. They're the same at the moment.
    public static final Map<String, String> goodIdentTypeIds = UtilMisc.toMap("EAN", "EAN", "ISBN", "ISBN", "GTIN", "GTIN");

    // Require UPC codes for product posting
    public static final boolean requireUpcCodes = "true".equalsIgnoreCase(UtilProperties.getPropertyValue(configResource, "opentaps.amazon.export.product.requireUpcCodes", ""));

    // Output location for the product feed XML posts
    public static final String xmlOutputLocation = System.getProperty("ofbiz.home") + "/runtime/output/";
    public static final SimpleDateFormat xmlOutputDateFormat = new SimpleDateFormat("yy-MM-dd_HHmmss");
}
