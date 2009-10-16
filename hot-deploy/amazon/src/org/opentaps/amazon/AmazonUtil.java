/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 * 
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.amazon;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.util.UtilMessage;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * Utilities for Amazon integration
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a> 
 * @version    $Rev: 10645 $
 */

public class AmazonUtil {

    public static String module = AmazonUtil.class.getName();

    /**
     * Parses a date string in XS date format into a Timestamp corrected to server time.
     *  Assumes that the XS date is for the Amazon timezone defined in AmazonConstants
     * 
     * @param xsDateString
     * @return Timestamp corrected for server time
     */
    public static Timestamp convertAmazonXSDateToLocalTimestamp(String xsDateString) {
        if (xsDateString == null) return null;
        String[] dateTimeElements = xsDateString.split("T");
        String[] dateElements = dateTimeElements[0].split("-");
        String[] timeElements = dateTimeElements[1].substring(0, 8).split(":");
        Timestamp ts = UtilDateTime.toTimestamp(dateElements[1], dateElements[2], dateElements[0], timeElements[0], timeElements[1], timeElements[2]);
        long gmtMillis = ts.getTime() - AmazonConstants.amazonTimeZone.getRawOffset() - AmazonConstants.amazonTimeZone.getDSTSavings();
        long localMillis = gmtMillis + TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();
        return new Timestamp(localMillis);
    }

    /**
     * Converts a timestamp to XS date format
     * 
     * @param ts
     * @return XS date string
     */
    public static String convertTimestampToXSDate(Timestamp ts) {
        if (ts == null) return null;
        GregorianCalendar cal = new GregorianCalendar(); 
        cal.setTimeInMillis(ts.getTime());
        DatatypeFactory df = null;
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            Debug.logError(e, module);
        }
        XMLGregorianCalendar xc = df.newXMLGregorianCalendar(cal);
        return xc.toXMLFormat();
    }

    /**
     * Sends email notification of a failed operation
     * 
     * @param dispatcher
     * @param userLogin
     * @param params
     * @param subject
     * @param screenUri
     * @throws GenericServiceException
     */
    public static void sendErrorEmail(LocalDispatcher dispatcher, GenericValue userLogin, Map params, String subject, String screenUri) throws GenericServiceException {
        Map sendMailContext = new HashMap();
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
     * Send bulk error notification emails, with a number of lines defined by AmazonConstants.linesForBulkErrorEmails
     * @param dispatcher
     * @param userLogin
     * @param errorMessages
     * @param subject
     * @param screenUri
     * @throws GenericServiceException
     */
    public static void sendBulkErrorEmail(LocalDispatcher dispatcher, GenericValue userLogin, LinkedHashMap<GenericValue, String> errorMessages, String subject, String screenUri) throws GenericServiceException {
        Map emailErrors = new LinkedHashMap();
        int count = 0;
        int messageCount = 0;
        int max = errorMessages.size();
        int totalMessages = new BigDecimal(max / new Double(AmazonConstants.linesForBulkErrorEmails).doubleValue()).setScale(0, BigDecimal.ROUND_UP).intValue();
        for (Map.Entry entry : errorMessages.entrySet()) {
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
        
        private final int _minLength;
        private final int _maxLength;
        
        Strings(int minLength, int maxLength) {
            _minLength = minLength;
            _maxLength = maxLength;
        }
        
        /**
         * Check if string length is within a valid range.
         * 
         * @param s
         * @return boolean
         */
        public boolean isValid(String s) {
            if (_minLength != 0 && s == null) {
                return false;
            }
            return (s.length() >= _minLength) && (s.length() <= _maxLength) ? true : false;
        };
        
        /**
         * Truncate string to max valid length of given type and log warning.
         * 
         * @param s
         * @param locale
         * @return String
         */
        public String normalize(String s, Locale locale) {
            if (s == null) return s;
            
            // Hack to work around the non-breaking space bug
            s = s.replaceAll("\\u00A0", " ");
            
            if (s.length() > _maxLength) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_WarningStringTruncated", UtilMisc.toMap("str", s, "maxLength", _maxLength), locale), module);
                return s.substring(0, _maxLength - 1);
            }
            return s;
        }
        
    };

    /**
     * Marks an AmazonProduct as updated, which will cause it to be published to Amazon.
     *
     * @param value
     */
    public static void markAmazonProductAsUpdated(GenericValue value) {
        markAmazonValueAsUpdated(value);
        value.set("statusId", AmazonConstants.statusProductChanged);
        value.set("ackStatusId", AmazonConstants.statusProductNotAcked);
    }

    /**
     * Marks an AmazonProduct as deleted, which will cause it to be deleted from Amazon.
     *
     * @param value
     */
    public static void markAmazonProductAsDeleted(GenericValue value) {
        markAmazonValueAsUpdated(value);
        value.set("statusId", AmazonConstants.statusProductDeleted);
        value.set("ackStatusId", AmazonConstants.statusProductNotAcked);
    }

    /**
     * Marks an AmazonProductImage as updated, which will cause it to be published to Amazon.
     *
     * @param value
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
     *
     * @param value
     */
    public static void markAmazonProductPriceAsUpdated(GenericValue value) {
        markAmazonValueAsUpdated(value);
        value.set("statusId", AmazonConstants.statusProductChanged);
        value.set("ackStatusId", AmazonConstants.statusProductNotAcked);
    }

    /**
     * Marks an AmazonProductInventory as updated, which will cause it to be published to Amazon.
     *
     * @param value
     */
    public static void markAmazonProductInventoryAsUpdated(GenericValue value) {
        markAmazonValueAsUpdated(value);
        value.set("statusId", AmazonConstants.statusProductChanged);
        value.set("ackStatusId", AmazonConstants.statusProductNotAcked);
    }

    /**
     * Creates the necessary set of records for a new Amazon product
     * 
     * @param delegator
     * @param productId
     * @return
     */
    public static List<GenericValue> createAmazonProductRecords(GenericDelegator delegator, String productId) {
        List<GenericValue> records = new ArrayList<GenericValue>(); 
        records.add(createAmazonProductRecord(delegator, productId));
        records.addAll(createAmazonProductRelatedRecords(delegator, productId));
        return records;
    }
    
    /**
     * Creates the AmazonProduct record for a new Amazon product
     * 
     * @param delegator
     * @param productId
     * @return
     */
    public static GenericValue createAmazonProductRecord(GenericDelegator delegator, String productId) {
        GenericValue amazonProduct = delegator.makeValue("AmazonProduct", UtilMisc.toMap("productId", productId));
        markAmazonValueAsUpdated(amazonProduct);
        amazonProduct.set("statusId", AmazonConstants.statusProductCreated);
        amazonProduct.set("ackStatusId", AmazonConstants.statusProductNotAcked);
        return amazonProduct;
    }
    
    /**
     * Creates the AmazonProduct* records for a new Amazon product
     * 
     * @param delegator
     * @param productId
     * @return
     */
    public static List<GenericValue> createAmazonProductRelatedRecords(GenericDelegator delegator, String productId) {
        return UtilMisc.toList(createAmazonProductPrice(delegator, productId), createAmazonProductInventory(delegator, productId), createAmazonProductImage(delegator, productId));
    }

    public static GenericValue createAmazonProductImage(GenericDelegator delegator, String productId) {
        GenericValue amazonProductImage = delegator.makeValue("AmazonProductImage", UtilMisc.toMap("productId", productId));
        markAmazonValueAsUpdated(amazonProductImage);
        amazonProductImage.set("statusId", AmazonConstants.statusProductCreated);
        return amazonProductImage;
    }

    /**
     * If the amazonProductImage parameter is not null, flags as updated and stores. Otherwise, creates, flags as updated and stores.
     */
    public static void createOrUpdateAmazonProductImage(GenericDelegator delegator, String productId, GenericValue amazonProductImage) throws GenericEntityException {
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

    public static GenericValue createAmazonProductPrice(GenericDelegator delegator, String productId) {
        GenericValue amazonProductPrice = delegator.makeValue("AmazonProductPrice", UtilMisc.toMap("productId", productId));
        markAmazonValueAsUpdated(amazonProductPrice);
        amazonProductPrice.set("statusId", AmazonConstants.statusProductCreated);
        amazonProductPrice.set("ackStatusId", AmazonConstants.statusProductNotAcked);
        return amazonProductPrice;
    }

    public static GenericValue createAmazonProductInventory(GenericDelegator delegator, String productId) {
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
        if ((modelEntity != null) && (modelEntity.getField("acknowledgeTimestamp") != null)) value.set("acknowledgeTimestamp", null);
        if ((modelEntity != null) && (modelEntity.getField("acknowledgeErrorMessage") != null)) value.set("acknowledgeErrorMessage", null);
        if ((modelEntity != null) && (modelEntity.getField("acknowledgeMessageId") != null)) value.set("acknowledgeMessageId", null);
    }

    public static boolean isAmazonProductDeleted(GenericValue amazonProduct) {
        return AmazonConstants.statusProductDeleted.equalsIgnoreCase(amazonProduct.getString("statusId")) || AmazonConstants.statusProductDeleteError.equalsIgnoreCase(amazonProduct.getString("statusId"));
    }
    
    public static boolean isAmazonProductDeleted(GenericDelegator delegator, String productId) throws GenericEntityException {
        GenericValue amazonProduct = delegator.findByPrimaryKey("AmazonProduct", UtilMisc.toMap("productId", productId));
        return isAmazonProductDeleted(amazonProduct);
    }
    
    /**
     * Returns ItemType for given node. For each node can be only ItemType.
     * 
     * @param delegator
     * @param nodeId
     * @return String 
     * @throws GenericEntityException
     */
    public static String getValidItemType(GenericDelegator delegator, String nodeId) throws GenericEntityException {
        List<GenericValue> validItem = delegator.findByAnd("AmazonNodeValidAttribute", UtilMisc.toMap("nodeId", nodeId));
        if (UtilValidate.isEmpty(validItem)) {
            return null;
        }
        return EntityUtil.getFirst(validItem).getString("itemTypeId");
    }
    
    public static void findAndSetValidProductElements(GenericDelegator delegator, String productId, String nodeId) throws GenericEntityException {
        List<GenericValue> validElements = delegator.findByAnd("AmazonNodeValidAttribute", UtilMisc.toMap("nodeId", nodeId));
        if (UtilValidate.isEmpty(validElements)) {
            return;
        }
        
        // look for OtherItemAttributes
        List<GenericValue> otherItemAttributes = EntityUtil.filterByAnd(validElements, UtilMisc.toMap("nodeMappingTypeId", "OTHER_ITEM_ATTR"));
        if (UtilValidate.isNotEmpty(otherItemAttributes) && otherItemAttributes.size() == 1) {
            GenericValue otherItemAttribute = EntityUtil.getFirst(otherItemAttributes);
            List<String> andValues = new ArrayList<String>();
            if (UtilValidate.isNotEmpty(otherItemAttribute.getString("relatedToId"))) andValues.add(otherItemAttribute.getString("relatedToId"));
            if (UtilValidate.isNotEmpty(otherItemAttribute.getString("relatedTo1Id"))) andValues.add(otherItemAttribute.getString("relatedTo1Id"));
            if (UtilValidate.isNotEmpty(otherItemAttribute.getString("relatedTo2Id"))) andValues.add(otherItemAttribute.getString("relatedTo2Id"));
            if (UtilValidate.isNotEmpty(otherItemAttribute.getString("relatedTo3Id"))) andValues.add(otherItemAttribute.getString("relatedTo3Id"));
            if (UtilValidate.isNotEmpty(otherItemAttribute.getString("relatedTo4Id"))) andValues.add(otherItemAttribute.getString("relatedTo4Id"));
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
            if (UtilValidate.isNotEmpty(usedForItem.getString("relatedToId"))) andValues.add(usedForItem.getString("relatedToId"));
            if (UtilValidate.isNotEmpty(usedForItem.getString("relatedTo1Id"))) andValues.add(usedForItem.getString("relatedTo1Id"));
            if (UtilValidate.isNotEmpty(usedForItem.getString("relatedTo2Id"))) andValues.add(usedForItem.getString("relatedTo2Id"));
            if (UtilValidate.isNotEmpty(usedForItem.getString("relatedTo3Id"))) andValues.add(usedForItem.getString("relatedTo3Id"));
            if (UtilValidate.isNotEmpty(usedForItem.getString("relatedTo4Id"))) andValues.add(usedForItem.getString("relatedTo4Id"));
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
            if (UtilValidate.isNotEmpty(targetAudienceItem.getString("relatedToId"))) andValues.add(targetAudienceItem.getString("relatedToId"));
            if (UtilValidate.isNotEmpty(targetAudienceItem.getString("relatedTo1Id"))) andValues.add(targetAudienceItem.getString("relatedTo1Id"));
            if (UtilValidate.isNotEmpty(targetAudienceItem.getString("relatedTo2Id"))) andValues.add(targetAudienceItem.getString("relatedTo2Id"));
            if (UtilValidate.isNotEmpty(targetAudienceItem.getString("relatedTo3Id"))) andValues.add(targetAudienceItem.getString("relatedTo3Id"));
            if (UtilValidate.isNotEmpty(targetAudienceItem.getString("relatedTo4Id"))) andValues.add(targetAudienceItem.getString("relatedTo4Id"));
            for (String andValue : andValues) {
                GenericValue value = delegator.makeValue("AmazonTargetAudienceValue", UtilMisc.toMap("productId", productId, "targetAudienceId", andValue));
                value.create();
            }
        }
        
    }
    
    /**
     * Helper method return valid ItemTypes for given browse node. 
     * 
     * @param delegator
     * @param nodeId
     * @return List<GenericValue>
     */
    public static List<GenericValue> getValidItemTypesForNode(GenericDelegator delegator, String nodeId) {
        
        if (UtilValidate.isEmpty(nodeId) || delegator == null) {
            return null;
        }
        
        try {
            
            List<GenericValue> validAttributes = delegator.findByAnd("AmazonNodeValidAttribute", UtilMisc.toMap("nodeId", nodeId));
            List<String> validItemTypeIds = EntityUtil.getFieldListFromEntityList(validAttributes, "itemTypeId", true);
            if (UtilValidate.isNotEmpty(validItemTypeIds)) {
                return delegator.findByCondition("AmazonProductItemType", new EntityExpr("itemTypeId", EntityOperator.IN, validItemTypeIds), null, Arrays.asList("itemTypeId"));
            }
            
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
        } catch (NullPointerException npe) {
            Debug.logError(npe.getMessage(), module);
        }

        return null;
    }
    
    /**
     * 
     * Method return attributes of some type (UsedFor, OtherItemAttribute, TargetAudience) which can be combined 
     * for the ItemType by OR.
     * 
     * @param delegator
     * @param nodeMappingTypeId
     * @param nodeId
     * @param itemTypeId
     * @return List<GenericValue>
     */
    public static List<GenericValue> getValidAttributesForItemType(GenericDelegator delegator, String nodeMappingTypeId, String nodeId, String itemTypeId) {

        if (UtilValidate.isEmpty(nodeId) || delegator == null) {
            return null;
        }
        
        try {
            
            Map fields = UtilMisc.toMap("nodeId", nodeId, "nodeMappingTypeId", nodeMappingTypeId);
            if (itemTypeId != null) fields.put("itemTypeId", itemTypeId);
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
                    result = delegator.findByCondition("AmazonProductUsedFor", new EntityExpr("usedForId", EntityOperator.IN, validIds), null, Arrays.asList("usedForId"));
                } else if ("OTHER_ITEM_ATTR".equals(nodeMappingTypeId)) {
                    result = delegator.findByCondition("AmazonProductOtherItemAttr", new EntityExpr("otherItemAttrId", EntityOperator.IN, validIds), null, Arrays.asList("otherItemAttrId"));
                } else if ("TARGET_AUDIENCE".equals(nodeMappingTypeId)) {
                    result = delegator.findByCondition("AmazonProductTargetAudience", new EntityExpr("targetAudienceId", EntityOperator.IN, validIds), null, Arrays.asList("targetAudienceId"));
                }
            }
            
            return result;
            
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
        } catch (NullPointerException npe) {
            Debug.logError(npe.getMessage(), module);
        }
        
        return null;
    }

    /**
     * Aggregates a possibly empty original error message with a new error message using the system's line separator
     */
    public static String compoundError(String originalError, String newError) {
        if (UtilValidate.isEmpty(originalError)) return newError;
        return originalError + System.getProperty("line.separator") + newError;
    }
}
