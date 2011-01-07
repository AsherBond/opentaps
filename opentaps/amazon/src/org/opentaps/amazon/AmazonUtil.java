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
import java.sql.Timestamp;
import java.util.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

/**
 * Utilities for Amazon integration.
 */
public final class AmazonUtil {

    private AmazonUtil() { }

    private static final String MODULE = AmazonUtil.class.getName();

    /**
     * Parses a date string in XS date format into a Timestamp corrected to server time.
     *  Assumes that the XS date is for the Amazon timezone defined in AmazonConstants
     *
     * @param xsDateString a date string in XS date format
     * @return Timestamp corrected for server time
     */
    public static Timestamp convertAmazonXSDateToLocalTimestamp(String xsDateString) {
        if (xsDateString == null) {
            return null;
        }
        String[] dateTimeElements = xsDateString.split("T");
        String[] dateElements = dateTimeElements[0].split("-");
        String[] timeElements = dateTimeElements[1].substring(0, 8).split(":");
        Timestamp ts = UtilDateTime.toTimestamp(dateElements[1], dateElements[2], dateElements[0], timeElements[0], timeElements[1], timeElements[2]);
        long gmtMillis = ts.getTime() - AmazonConstants.amazonTimeZone.getRawOffset() - AmazonConstants.amazonTimeZone.getDSTSavings();
        long localMillis = gmtMillis + TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();
        return new Timestamp(localMillis);
    }

    /**
     * Converts a timestamp to XS date format.
     *
     * @param ts a <code>Timestamp</code> value
     * @return XS date string
     */
    public static String convertTimestampToXSDate(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(ts.getTime());
        DatatypeFactory df = null;
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            Debug.logError(e, MODULE);
        }
        XMLGregorianCalendar xc = df.newXMLGregorianCalendar(cal);
        return xc.toXMLFormat();
    }

    /**
     * Sends email notification of a failed operation.
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param params a <code>Map</code> value
     * @param subject a <code>String</code> value
     * @param screenUri a <code>String</code> value
     * @exception GenericServiceException if an error occurs
     */
    public static void sendErrorEmail(LocalDispatcher dispatcher, GenericValue userLogin, Map params, String subject, String screenUri) throws GenericServiceException {
        Map<String, Object> sendMailContext = new HashMap<String, Object>();
        sendMailContext.put("locale", AmazonConstants.errorEmailLocale);
        sendMailContext.put("bodyScreenUri", screenUri);
        sendMailContext.put("bodyParameters", params);
        sendMailContext.put("sendTo", AmazonConstants.errorEmailTo);
        sendMailContext.put("sendFrom", AmazonConstants.errorEmailFrom);
        sendMailContext.put("subject", subject);
        sendMailContext.put("contentType", "text/html");
        sendMailContext.put("userLogin", userLogin);

        // Call sendMailFromScreen async so that failed emails are retried
        dispatcher.runAsync("sendMailFromScreen", sendMailContext);
    }

    /**
     * Send bulk error notification emails, with a number of lines defined by AmazonConstants.linesForBulkErrorEmails.
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param errorMessages a <code>Map</code> value
     * @param subject a <code>String</code> value
     * @param screenUri a <code>String</code> value
     * @exception GenericServiceException if an error occurs
     */
    public static void sendBulkErrorEmail(LocalDispatcher dispatcher, GenericValue userLogin, LinkedHashMap<GenericValue, String> errorMessages, String subject, String screenUri) throws GenericServiceException {
        Map<GenericValue, String> emailErrors = new LinkedHashMap<GenericValue, String>();
        int count = 0;
        int messageCount = 0;
        int max = errorMessages.size();
        int totalMessages = new BigDecimal(max / new Double(AmazonConstants.linesForBulkErrorEmails).doubleValue()).setScale(0, BigDecimal.ROUND_UP).intValue();
        for (Map.Entry<GenericValue, String> entry : errorMessages.entrySet()) {
            emailErrors.put(entry.getKey(), entry.getValue());
            count++;
            if ((count != 0 && count % AmazonConstants.linesForBulkErrorEmails == 0) || count == max) {
                messageCount++;
                String postFix = " " + UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_BulkPostfix", UtilMisc.toMap("message", messageCount, "total", totalMessages), AmazonConstants.errorEmailLocale);
                sendErrorEmail(dispatcher, userLogin, UtilMisc.toMap("errorMessages", emailErrors), subject + postFix, screenUri);
                emailErrors.clear();
            }
        }
    }

    public static enum Strings/*Amazon string types*/ {
        STR                     (0, 50),
        STR_N_NULL              (1, 50),
        SUPER_LONG_STR_N_NULL   (1, 1000),
        THIRTY_STR_N_NULL       (1, 30),
        TWENTY_STR_N_NULL       (1, 20),
        TWO_FIFTY_STR_N_NULL    (1, 250),
        MEDIUM_STR_N_NULL       (1, 200),
        LONG_STR_N_NULL         (1, 500),
        LONG_STR                (0, 500),
        FOURTY_STR_N_NULL       (1, 40),
        ADDRESS_LINE            (0, 60),
        STD_PRODUCT_ID          (10, 14),
        SKU_TYPE                (1, 40),
        DESCRIPTION             (0, 2000),
        DISCLAIMER              (0, 1000);

        private final int minLength;
        private final int maxLength;

        Strings(int minLength, int maxLength) {
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        /**
         * Check if string length is within a valid range.
         *
         * @param s a <code>String</code> to check
         * @return is the <code>String</code> valid
         */
        public boolean isValid(String s) {
            if (minLength != 0 && s == null) {
                return false;
            }
            return (s.length() >= minLength) && (s.length() <= maxLength) ? true : false;
        };

        /**
         * Truncate string to max valid length of given type and log warning.
         *
         * @param s a <code>String</code> to normalize
         * @param locale a <code>Locale</code> value
         * @return the normalized <code>String</code>
         */
        public String normalize(String s, Locale locale) {
            if (s == null) {
                return s;
            }

            // Hack to work around the non-breaking space bug
            s = s.replaceAll("\\u00A0", " ");

            if (s.length() > maxLength) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_WarningStringTruncated", UtilMisc.<String, Object>toMap("str", s, "maxLength", maxLength), locale), MODULE);
                return s.substring(0, maxLength - 1);
            }
            return s;
        }

    };

    /**
     * Marks an AmazonProduct as updated, which will cause it to be published to Amazon.
     * @param value a <code>GenericValue</code> value
     */
    public static void markAmazonProductAsUpdated(GenericValue value) {
        markAmazonValueAsUpdated(value);
        value.set("statusId", AmazonConstants.statusProductChanged);
        value.set("ackStatusId", AmazonConstants.statusProductNotAcked);
    }

    /**
     * Marks an AmazonProduct as deleted, which will cause it to be deleted from Amazon.
     * @param value a <code>GenericValue</code> value
     */
    public static void markAmazonProductAsDeleted(GenericValue value) {
        markAmazonValueAsUpdated(value);
        value.set("statusId", AmazonConstants.statusProductDeleted);
        value.set("ackStatusId", AmazonConstants.statusProductNotAcked);
    }

    /**
     * Marks an AmazonProductImage as updated, which will cause it to be published to Amazon.
     * @param value a <code>GenericValue</code> value
     */
    public static void markAmazonProductImageAsUpdated(GenericValue value) {
        value.set("postTimestamp", null);
        value.set("postErrorMessage", null);
        value.set("postFailures", new Long(0));
        value.set("processingDocumentId", null);
        value.set("statusId", AmazonConstants.statusProductChanged);
    }

    /**
     * Marks an AmazonProductPrice as updated, which will cause it to be published to Amazon.
     * @param value a <code>GenericValue</code> value
     */
    public static void markAmazonProductPriceAsUpdated(GenericValue value) {
        markAmazonValueAsUpdated(value);
        value.set("statusId", AmazonConstants.statusProductChanged);
        value.set("ackStatusId", AmazonConstants.statusProductNotAcked);
    }

    /**
     * Marks an AmazonProductInventory as updated, which will cause it to be published to Amazon.
     * @param value a <code>GenericValue</code> value
     */
    public static void markAmazonProductInventoryAsUpdated(GenericValue value) {
        markAmazonValueAsUpdated(value);
        value.set("statusId", AmazonConstants.statusProductChanged);
        value.set("ackStatusId", AmazonConstants.statusProductNotAcked);
    }

    /**
     * Creates the necessary set of records for a new Amazon product.
     * @param delegator a <code>Delegator</code> value
     * @param productId a <code>String</code> value
     * @return a <code>List</code> value
     */
    public static List<GenericValue> createAmazonProductRecords(Delegator delegator, String productId) {
        List<GenericValue> records = new ArrayList<GenericValue>();
        records.add(createAmazonProductRecord(delegator, productId));
        records.addAll(createAmazonProductRelatedRecords(delegator, productId));
        return records;
    }

    /**
     * Creates the AmazonProduct record for a new Amazon product.
     * @param delegator a <code>Delegator</code> value
     * @param productId a <code>String</code> value
     * @return a <code>GenericValue</code> value
     */
    public static GenericValue createAmazonProductRecord(Delegator delegator, String productId) {
        GenericValue amazonProduct = delegator.makeValue("AmazonProduct", UtilMisc.toMap("productId", productId));
        markAmazonValueAsUpdated(amazonProduct);
        amazonProduct.set("statusId", AmazonConstants.statusProductCreated);
        amazonProduct.set("ackStatusId", AmazonConstants.statusProductNotAcked);
        return amazonProduct;
    }

    /**
     * Creates the AmazonProduct* records for a new Amazon product.
     * @param delegator a <code>Delegator</code> value
     * @param productId a <code>String</code> value
     * @return a <code>List</code> value
     */
    public static List<GenericValue> createAmazonProductRelatedRecords(Delegator delegator, String productId) {
        return UtilMisc.toList(createAmazonProductPrice(delegator, productId), createAmazonProductInventory(delegator, productId), createAmazonProductImage(delegator, productId));
    }

    /**
     * Creates a new AmazonProductImage record.
     * @param delegator a <code>Delegator</code> value
     * @param productId a <code>String</code> value
     * @return a <code>GenericValue</code> value
     */
    public static GenericValue createAmazonProductImage(Delegator delegator, String productId) {
        GenericValue amazonProductImage = delegator.makeValue("AmazonProductImage", UtilMisc.toMap("productId", productId));
        markAmazonValueAsUpdated(amazonProductImage);
        amazonProductImage.set("statusId", AmazonConstants.statusProductCreated);
        return amazonProductImage;
    }

    /**
     * If the amazonProductImage parameter is not null, flags as updated and stores. Otherwise, creates, flags as updated and stores.
     * @param delegator a <code>Delegator</code> value
     * @param productId a <code>String</code> value
     * @param amazonProductImage a <code>GenericValue</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static void createOrUpdateAmazonProductImage(Delegator delegator, String productId, GenericValue amazonProductImage) throws GenericEntityException {
        if (amazonProductImage != null) {
            markAmazonProductImageAsUpdated(amazonProductImage);
            amazonProductImage.store();
            delegator.removeRelated("AmazonProductImageAck", amazonProductImage);
        } else {
            amazonProductImage = createAmazonProductImage(delegator, productId);
            markAmazonProductImageAsUpdated(amazonProductImage);
            amazonProductImage.create();
        }
    }

    /**
     * Creates a new AmazonProductPrice record.
     * @param delegator a <code>Delegator</code> value
     * @param productId a <code>String</code> value
     * @return a <code>GenericValue</code> value
     */
    public static GenericValue createAmazonProductPrice(Delegator delegator, String productId) {
        GenericValue amazonProductPrice = delegator.makeValue("AmazonProductPrice", UtilMisc.toMap("productId", productId));
        markAmazonValueAsUpdated(amazonProductPrice);
        amazonProductPrice.set("statusId", AmazonConstants.statusProductCreated);
        amazonProductPrice.set("ackStatusId", AmazonConstants.statusProductNotAcked);
        return amazonProductPrice;
    }

    /**
     * Creates a new AmazonProductInventory record.
     * @param delegator a <code>Delegator</code> value
     * @param productId a <code>String</code> value
     * @return a <code>GenericValue</code> value
     */
    public static GenericValue createAmazonProductInventory(Delegator delegator, String productId) {
        GenericValue amazonProductInventory = delegator.makeValue("AmazonProductInventory", UtilMisc.toMap("productId", productId));
        markAmazonValueAsUpdated(amazonProductInventory);
        amazonProductInventory.set("statusId", AmazonConstants.statusProductCreated);
        amazonProductInventory.set("ackStatusId", AmazonConstants.statusProductNotAcked);
        return amazonProductInventory;
    }

    private static void markAmazonValueAsUpdated(GenericValue value) {
        value.set("postTimestamp", null);
        value.set("postErrorMessage", null);
        value.set("postFailures", new Long(0));
        value.set("processingDocumentId", null);
        ModelEntity modelEntity = value.getModelEntity();
        if ((modelEntity != null) && (modelEntity.getField("acknowledgeTimestamp") != null)) {
            value.set("acknowledgeTimestamp", null);
        }
        if ((modelEntity != null) && (modelEntity.getField("acknowledgeErrorMessage") != null)) {
            value.set("acknowledgeErrorMessage", null);
        }
        if ((modelEntity != null) && (modelEntity.getField("acknowledgeMessageId") != null)) {
            value.set("acknowledgeMessageId", null);
        }
    }

    /**
     * Checks if the given AmazonProduct record is deleted.
     * @param amazonProduct a <code>GenericValue</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isAmazonProductDeleted(GenericValue amazonProduct) {
        return AmazonConstants.statusProductDeleted.equalsIgnoreCase(amazonProduct.getString("statusId")) || AmazonConstants.statusProductDeleteError.equalsIgnoreCase(amazonProduct.getString("statusId"));
    }

    /**
     * Checks if the given AmazonProduct record is deleted.
     * @param delegator a <code>Delegator</code> value
     * @param productId a <code>String</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static boolean isAmazonProductDeleted(Delegator delegator, String productId) throws GenericEntityException {
        GenericValue amazonProduct = delegator.findByPrimaryKey("AmazonProduct", UtilMisc.toMap("productId", productId));
        return isAmazonProductDeleted(amazonProduct);
    }

    /**
     * Returns ItemType for given node. For each node can be only ItemType.
     * @param delegator a <code>Delegator</code> value
     * @param nodeId a <code>String</code> value
     * @return a <code>String</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static String getValidItemType(Delegator delegator, String nodeId) throws GenericEntityException {
        List<GenericValue> validItem = delegator.findByAnd("AmazonNodeValidAttribute", UtilMisc.toMap("nodeId", nodeId));
        if (UtilValidate.isEmpty(validItem)) {
            return null;
        }
        return EntityUtil.getFirst(validItem).getString("itemTypeId");
    }

    /**
     * Describe <code>findAndSetValidProductElements</code> method here.
     *
     * @param delegator a <code>Delegator</code> value
     * @param productId a <code>String</code> value
     * @param nodeId a <code>String</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static void findAndSetValidProductElements(Delegator delegator, String productId, String nodeId) throws GenericEntityException {
        List<GenericValue> validElements = delegator.findByAnd("AmazonNodeValidAttribute", UtilMisc.toMap("nodeId", nodeId));
        if (UtilValidate.isEmpty(validElements)) {
            return;
        }

        // look for OtherItemAttributes
        List<GenericValue> otherItemAttributes = EntityUtil.filterByAnd(validElements, UtilMisc.toMap("nodeMappingTypeId", "OTHER_ITEM_ATTR"));
        if (UtilValidate.isNotEmpty(otherItemAttributes) && otherItemAttributes.size() == 1) {
            GenericValue otherItemAttribute = EntityUtil.getFirst(otherItemAttributes);
            List<String> andValues = new ArrayList<String>();
            if (UtilValidate.isNotEmpty(otherItemAttribute.getString("relatedToId"))) {
                andValues.add(otherItemAttribute.getString("relatedToId"));
            }
            if (UtilValidate.isNotEmpty(otherItemAttribute.getString("relatedTo1Id"))) {
                andValues.add(otherItemAttribute.getString("relatedTo1Id"));
            }
            if (UtilValidate.isNotEmpty(otherItemAttribute.getString("relatedTo2Id"))) {
                andValues.add(otherItemAttribute.getString("relatedTo2Id"));
            }
            if (UtilValidate.isNotEmpty(otherItemAttribute.getString("relatedTo3Id"))) {
                andValues.add(otherItemAttribute.getString("relatedTo3Id"));
            }
            if (UtilValidate.isNotEmpty(otherItemAttribute.getString("relatedTo4Id"))) {
                andValues.add(otherItemAttribute.getString("relatedTo4Id"));
            }
            for (String andValue : andValues) {
                GenericValue value = delegator.makeValue("AmazonOtherItemAttrValue", UtilMisc.toMap("productId", productId, "otherItemAttrId", andValue));
                value.create();
            }
        }

        // look for UsedFor
        List<GenericValue> usedForItems = EntityUtil.filterByAnd(validElements, UtilMisc.toMap("nodeMappingTypeId", "USED_FOR"));
        if (UtilValidate.isNotEmpty(usedForItems) && usedForItems.size() == 1) {
            GenericValue usedForItem = EntityUtil.getFirst(usedForItems);
            List<String> andValues = new ArrayList<String>();
            if (UtilValidate.isNotEmpty(usedForItem.getString("relatedToId"))) {
                andValues.add(usedForItem.getString("relatedToId"));
            }
            if (UtilValidate.isNotEmpty(usedForItem.getString("relatedTo1Id"))) {
                andValues.add(usedForItem.getString("relatedTo1Id"));
            }
            if (UtilValidate.isNotEmpty(usedForItem.getString("relatedTo2Id"))) {
                andValues.add(usedForItem.getString("relatedTo2Id"));
            }
            if (UtilValidate.isNotEmpty(usedForItem.getString("relatedTo3Id"))) {
                andValues.add(usedForItem.getString("relatedTo3Id"));
            }
            if (UtilValidate.isNotEmpty(usedForItem.getString("relatedTo4Id"))) {
                andValues.add(usedForItem.getString("relatedTo4Id"));
            }
            for (String andValue : andValues) {
                GenericValue value = delegator.makeValue("AmazonUsedForValue", UtilMisc.toMap("productId", productId, "usedForId", andValue));
                value.create();
            }
        }

        // look for TargetAudience
        List<GenericValue> targetAudienceItems = EntityUtil.filterByAnd(validElements, UtilMisc.toMap("nodeMappingTypeId", "TARGET_AUDIENCE"));
        if (UtilValidate.isNotEmpty(targetAudienceItems) && targetAudienceItems.size() == 1) {
            GenericValue targetAudienceItem = EntityUtil.getFirst(targetAudienceItems);
            List<String> andValues = new ArrayList<String>();
            if (UtilValidate.isNotEmpty(targetAudienceItem.getString("relatedToId"))) {
                andValues.add(targetAudienceItem.getString("relatedToId"));
            }
            if (UtilValidate.isNotEmpty(targetAudienceItem.getString("relatedTo1Id"))) {
                andValues.add(targetAudienceItem.getString("relatedTo1Id"));
            }
            if (UtilValidate.isNotEmpty(targetAudienceItem.getString("relatedTo2Id"))) {
                andValues.add(targetAudienceItem.getString("relatedTo2Id"));
            }
            if (UtilValidate.isNotEmpty(targetAudienceItem.getString("relatedTo3Id"))) {
                andValues.add(targetAudienceItem.getString("relatedTo3Id"));
            }
            if (UtilValidate.isNotEmpty(targetAudienceItem.getString("relatedTo4Id"))) {
                andValues.add(targetAudienceItem.getString("relatedTo4Id"));
            }
            for (String andValue : andValues) {
                GenericValue value = delegator.makeValue("AmazonTargetAudienceValue", UtilMisc.toMap("productId", productId, "targetAudienceId", andValue));
                value.create();
            }
        }

    }

    /**
     * Helper method return valid ItemTypes for given browse node.
     * @param delegator a <code>Delegator</code> value
     * @param nodeId a node ID
     * @return the <code>List</code> of <code>GenericValue</code> valid items for the given node
     */
    public static List<GenericValue> getValidItemTypesForNode(Delegator delegator, String nodeId) {

        if (UtilValidate.isEmpty(nodeId) || delegator == null) {
            return null;
        }

        try {

            List<GenericValue> validAttributes = delegator.findByAnd("AmazonNodeValidAttribute", UtilMisc.toMap("nodeId", nodeId));
            List<String> validItemTypeIds = EntityUtil.getFieldListFromEntityList(validAttributes, "itemTypeId", true);
            if (UtilValidate.isNotEmpty(validItemTypeIds)) {
                return delegator.findByCondition("AmazonProductItemType", EntityCondition.makeCondition("itemTypeId", EntityOperator.IN, validItemTypeIds), null, Arrays.asList("itemTypeId"));
            }

        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), MODULE);
        } catch (NullPointerException npe) {
            Debug.logError(npe.getMessage(), MODULE);
        }

        return null;
    }

    /**
     *
     * Method return attributes of some type (UsedFor, OtherItemAttribute, TargetAudience) which can be combined
     * for the ItemType by OR.
     *
     * @param delegator a <code>Delegator</code> value
     * @param nodeMappingTypeId a node mapping type ID
     * @param nodeId a node ID
     * @param itemTypeId an item type ID
     * @return the <code>List</code> of <code>GenericValue</code> valid attributes for the given node and item type
     */
    public static List<GenericValue> getValidAttributesForItemType(Delegator delegator, String nodeMappingTypeId, String nodeId, String itemTypeId) {

        if (UtilValidate.isEmpty(nodeId) || delegator == null) {
            return null;
        }

        try {

            Map<String, Object> fields = UtilMisc.<String, Object>toMap("nodeId", nodeId, "nodeMappingTypeId", nodeMappingTypeId);
            if (itemTypeId != null) {
                fields.put("itemTypeId", itemTypeId);
            }
            List<GenericValue> validAttributes = delegator.findByAnd("AmazonNodeValidAttribute", fields);
            List<String> validIds = EntityUtil.getFieldListFromEntityList(validAttributes, "relatedToId", true);
            List<GenericValue> result = null;
            if (UtilValidate.isEmpty(validIds)) {
                if ("USED_FOR".equals(nodeMappingTypeId)) {
                    result = delegator.findAll("AmazonProductUsedFor", Arrays.asList("usedForId"));
                } else if ("OTHER_ITEM_ATTR".equals(nodeMappingTypeId)) {
                    result = delegator.findAll("AmazonProductOtherItemAttr", Arrays.asList("otherItemAttrId"));
                } else if ("TARGET_AUDIENCE".equals(nodeMappingTypeId)) {
                    result = delegator.findAll("AmazonProductTargetAudience", Arrays.asList("targetAudienceId"));
                }
            } else {
                if ("USED_FOR".equals(nodeMappingTypeId)) {
                    result = delegator.findByCondition("AmazonProductUsedFor", EntityCondition.makeCondition("usedForId", EntityOperator.IN, validIds), null, Arrays.asList("usedForId"));
                } else if ("OTHER_ITEM_ATTR".equals(nodeMappingTypeId)) {
                    result = delegator.findByCondition("AmazonProductOtherItemAttr", EntityCondition.makeCondition("otherItemAttrId", EntityOperator.IN, validIds), null, Arrays.asList("otherItemAttrId"));
                } else if ("TARGET_AUDIENCE".equals(nodeMappingTypeId)) {
                    result = delegator.findByCondition("AmazonProductTargetAudience", EntityCondition.makeCondition("targetAudienceId", EntityOperator.IN, validIds), null, Arrays.asList("targetAudienceId"));
                }
            }

            return result;

        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), MODULE);
        } catch (NullPointerException npe) {
            Debug.logError(npe.getMessage(), MODULE);
        }

        return null;
    }

    /**
     * Aggregates a possibly empty original error message with a new error message using the system's line separator.
     * @param originalError a <code>String</code> value
     * @param newError a <code>String</code> value
     * @return a <code>String</code> value
     */
    public static String compoundError(String originalError, String newError) {
        if (UtilValidate.isEmpty(originalError)) {
            return newError;
        }
        return originalError + System.getProperty("line.separator") + newError;
    }
}
