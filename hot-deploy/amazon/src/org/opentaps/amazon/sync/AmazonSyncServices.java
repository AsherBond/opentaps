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
package org.opentaps.amazon.sync;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.amazon.AmazonConstants;
import org.opentaps.amazon.AmazonUtil;
import org.opentaps.common.util.UtilMessage;

/**
 * Batch updating services to synchronize Opentaps model with Amazon.
 * These services populate and flag as updated the AmazonProduct and
 * AmazonProductPrice.  These serve as an alternative to using ECAs
 * to update the Amazon flag tables.
 */
public class AmazonSyncServices {

    public static final String module = AmazonSyncServices.class.getName();

    public static final String batchUpdateAmazonService = "opentaps.amazon.batchUpdateAmazon";

    public static Map batchUpdateAmazonProducts(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");

        try {
            List runs = delegator.findByAnd("AmazonBatchUpdateHistory", UtilMisc.toMap("serviceName", batchUpdateAmazonService), UtilMisc.toList("completedTimestamp DESC"));
            GenericValue lastRunHistory = EntityUtil.getFirst( runs );

            // get the last time this service was completed, otherwise we're creating all amazon records from scratch
            Timestamp lastRun = null;
            if (lastRunHistory == null) {
                Debug.logInfo("First batch update for Amazon model.", module);
            } else {
                lastRun = lastRunHistory.getTimestamp("completedTimestamp");
                Debug.logInfo("Last batch update for Amazon model completed on ["+lastRun+"].  Starting next batch update.", module);
            }

            // If the service has never been run, assume the earliest possible date
            if (lastRun == null) lastRun = new Timestamp(0);

            // build a static condition for searching ProductContent images by the configured types
            List baseConditions = UtilMisc.toList(
                    new EntityExpr("productContentTypeId", EntityOperator.IN, AmazonConstants.imageTypes.values()),
                    new EntityExpr("lastUpdatedStamp", EntityOperator.GREATER_THAN_EQUAL_TO, lastRun),
                    EntityUtil.getFilterByDateExpr()
            );
            EntityCondition contentBaseConditions = new EntityConditionList(baseConditions, EntityOperator.AND);

            // create or update AmazonProduct and AmazonProductImage
            EntityListIterator iterator = delegator.findListIteratorByCondition("Product", new EntityConditionList(FastList.newInstance(), EntityOperator.AND), null, null);
            GenericValue product;
            while ((product = (GenericValue) iterator.next()) != null) {

                String productId = product.getString("productId");
                Timestamp salesDiscontinuationDate = product.getTimestamp("salesDiscontinuationDate");
                Timestamp lastModified = product.getTimestamp("lastUpdatedStamp");

                GenericValue amazonProduct = product.getRelatedOne("AmazonProduct");
                GenericValue amazonProductImage = delegator.findByPrimaryKey("AmazonProductImage", UtilMisc.toMap("productId", productId));

                boolean discontinue = UtilValidate.isNotEmpty(salesDiscontinuationDate) && salesDiscontinuationDate.before(now);

                // we'll update the Amazon model if the product has been discontinued or created/updated since the last run
                if (amazonProduct != null) {
                    if (AmazonUtil.isAmazonProductDeleted(amazonProduct)) {
                        Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProduct_ProductDeleted", locale), module);
                        continue;
                    } else if (discontinue) {
                        AmazonUtil.markAmazonProductAsDeleted(amazonProduct);
                    } else if (lastModified.after(lastRun)) {
                        AmazonUtil.markAmazonProductAsUpdated(amazonProduct);

                        // Assume the images changed
                        AmazonUtil.createOrUpdateAmazonProductImage(delegator, productId, amazonProductImage);
                    } else {

                        // check if the ProductContent changed
                        List contentConditions = UtilMisc.toList(new EntityExpr("productId", EntityOperator.EQUALS, productId), contentBaseConditions);
                        List contents = delegator.findByAnd("ProductContent", contentConditions);
                        if (UtilValidate.isNotEmpty(contents)) AmazonUtil.createOrUpdateAmazonProductImage(delegator, productId, amazonProductImage);
                    }
                    amazonProduct.store();
                } else {

                    // TODO: Evaluate whether this is a good idea or not... companies will each have their own logic which governs whether a product should be posted to Amazon. Maybe we need an AMZN_PROD_PENDING statusId?
                    // delegator.storeAll(AmazonUtil.createAmazonProductRecords(delegator, productId));
                }
            }
            iterator.close();

            // mark service as completed
            GenericValue history = delegator.makeValue("AmazonBatchUpdateHistory", 
            	UtilMisc.toMap(
                    "historyId", delegator.getNextSeqId("AmazonBatchUpdateHistory"),
                    "serviceName", batchUpdateAmazonService,
                    "userLoginId", userLogin.get("userLoginId"),
                    "completedTimestamp", UtilDateTime.nowTimestamp())
                );
            history.create();

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Updates AmazonProductPrice statusId to reflect changes to corresponding ProductPrice records
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map updateAmazonProductPrices(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String productId = (String) context.get("productId");
        String productStoreGroupId = (String) context.get("productStoreGroupId");

        Map result = ServiceUtil.returnSuccess();

        try {

            // Ignore if the productStoreGroup isn't correct for Amazon
            if (!AmazonConstants.priceProductStoreGroup.equalsIgnoreCase(productStoreGroupId)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductPriceUpdate_WrongStoreGroup", UtilMisc.toMap("productId", productId), locale), module);
                return result;
            }

            // Ignore if no AmazonProductPrice record exists
            GenericValue amazonProductPrice = delegator.findByPrimaryKey("AmazonProductPrice", UtilMisc.toMap("productId", productId));
            if (UtilValidate.isEmpty(amazonProductPrice)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductPriceUpdate_NoRecord", UtilMisc.toMap("productId", productId), locale), module);
                return result;
            }

            // Ignore if the AmazonProduct is marked deleted
            if (AmazonUtil.isAmazonProductDeleted(delegator, productId)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductPriceUpdate_ProductDeleted", UtilMisc.toMap("productId", productId), locale), module);
                return result;
            }

            AmazonUtil.markAmazonProductPriceAsUpdated(amazonProductPrice);
            amazonProductPrice.store();

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Updates AmazonProductInventory statusId to reflect changes to corresponding product inventory levels or ProductFacility records
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map updateAmazonProductInventory(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String productId = (String) context.get("productId");
        String facilityId = (String) context.get("facilityId");

        Map result = ServiceUtil.returnSuccess();

        try {

            // Ignore if no AmazonProductInventory record exists
            GenericValue amazonProductInventory = delegator.findByPrimaryKey("AmazonProductInventory", UtilMisc.toMap("productId", productId));
            if (UtilValidate.isEmpty(amazonProductInventory)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductInventoryUpdate_NoRecord", UtilMisc.toMap("productId", productId), locale), module);
                return result;
            }

            // Ignore if the AmazonProduct is marked deleted
            if (AmazonUtil.isAmazonProductDeleted(delegator, productId)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductInventoryUpdate_ProductDeleted", UtilMisc.toMap("productId", productId), locale), module);
                return result;
            }

            // Sanity check on the Amazon setup
            GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", AmazonConstants.productStoreId));
            if (productStore == null || UtilValidate.isEmpty(productStore.getString("inventoryFacilityId"))) {
                return UtilMessage.createAndLogServiceError(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_InvalidAmazonProductStore", UtilMisc.toMap("productStoreId", AmazonConstants.productStoreId), locale), module);
            }

            // Ignore if the facility is incorrect for Amazon
            if (!productStore.getString("inventoryFacilityId").equals(facilityId)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductInventoryUpdate_WrongFacility", UtilMisc.toMap("productId", productId), locale), module);
                return result;
            }

            AmazonUtil.markAmazonProductInventoryAsUpdated(amazonProductInventory);
            amazonProductInventory.store();

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }
}
