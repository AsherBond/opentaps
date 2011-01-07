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
package org.opentaps.amazon.sync;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
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
public final class AmazonSyncServices {

    private AmazonSyncServices() { }

    private static final String MODULE = AmazonSyncServices.class.getName();

    public static final String batchUpdateAmazonService = "opentaps.amazon.batchUpdateAmazon";

    /**
     * Batch updating service that initializes new products
     *   in the Opentaps Amazon system and checks if any Amazon
     *   published products should be updated.  This does not
     *   handle price or inventory updates.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> batchUpdateAmazonProducts(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");

        try {
            List<GenericValue> runs = delegator.findByAnd("AmazonBatchUpdateHistory", UtilMisc.toMap("serviceName", batchUpdateAmazonService), UtilMisc.toList("completedTimestamp DESC"));
            GenericValue lastRunHistory = EntityUtil.getFirst(runs);

            // get the last time this service was completed, otherwise we're creating all amazon records from scratch
            Timestamp lastRun = null;
            if (lastRunHistory == null) {
                Debug.logInfo("First batch update for Amazon model.", MODULE);
            } else {
                lastRun = lastRunHistory.getTimestamp("completedTimestamp");
                Debug.logInfo("Last batch update for Amazon model completed on [" + lastRun + "].  Starting next batch update.", MODULE);
            }

            // If the service has never been run, assume the earliest possible date
            if (lastRun == null) {
                lastRun = new Timestamp(0);
            }

            // build a static condition for searching ProductContent images by the configured types
            EntityCondition baseConditions = EntityConditionList.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("productContentTypeId", EntityOperator.IN, AmazonConstants.imageTypes.values()),
                    EntityCondition.makeCondition("lastUpdatedStamp", EntityOperator.GREATER_THAN_EQUAL_TO, lastRun),
                    EntityUtil.getFilterByDateExpr()
            );

            // create or update AmazonProduct and AmazonProductImage
            // find all products
            EntityListIterator iterator = delegator.findListIteratorByCondition("Product", null, null, null);
            GenericValue product;
            while ((product = iterator.next()) != null) {

                String productId = product.getString("productId");
                Timestamp salesDiscontinuationDate = product.getTimestamp("salesDiscontinuationDate");
                Timestamp lastModified = product.getTimestamp("lastUpdatedStamp");

                GenericValue amazonProduct = product.getRelatedOne("AmazonProduct");
                GenericValue amazonProductImage = delegator.findByPrimaryKey("AmazonProductImage", UtilMisc.toMap("productId", productId));

                boolean discontinue = UtilValidate.isNotEmpty(salesDiscontinuationDate) && salesDiscontinuationDate.before(now);

                // we'll update the Amazon model if the product has been discontinued or created/updated since the last run
                if (amazonProduct != null) {
                    if (AmazonUtil.isAmazonProductDeleted(amazonProduct)) {
                        Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProduct_ProductDeleted", locale), MODULE);
                        continue;
                    } else if (discontinue) {
                        AmazonUtil.markAmazonProductAsDeleted(amazonProduct);
                    } else if (lastModified.after(lastRun)) {
                        AmazonUtil.markAmazonProductAsUpdated(amazonProduct);

                        // Assume the images changed
                        AmazonUtil.createOrUpdateAmazonProductImage(delegator, productId, amazonProductImage);
                    } else {

                        // check if the ProductContent changed
                        List<EntityCondition> contentConditions = UtilMisc.toList(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId), baseConditions);
                        List<GenericValue> contents = delegator.findByAnd("ProductContent", contentConditions);
                        if (UtilValidate.isNotEmpty(contents)) {
                            AmazonUtil.createOrUpdateAmazonProductImage(delegator, productId, amazonProductImage);
                        }
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
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Updates AmazonProductPrice statusId to reflect changes to corresponding ProductPrice records.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> updateAmazonProductPrices(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String productId = (String) context.get("productId");
        String productStoreGroupId = (String) context.get("productStoreGroupId");

        Map<String, Object> result = ServiceUtil.returnSuccess();

        try {

            // Ignore if the productStoreGroup isn't correct for Amazon
            if (!AmazonConstants.priceProductStoreGroup.equalsIgnoreCase(productStoreGroupId)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductPriceUpdate_WrongStoreGroup", UtilMisc.toMap("productId", productId), locale), MODULE);
                return result;
            }

            // Ignore if no AmazonProductPrice record exists
            GenericValue amazonProductPrice = delegator.findByPrimaryKey("AmazonProductPrice", UtilMisc.toMap("productId", productId));
            if (UtilValidate.isEmpty(amazonProductPrice)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductPriceUpdate_NoRecord", UtilMisc.toMap("productId", productId), locale), MODULE);
                return result;
            }

            // Ignore if the AmazonProduct is marked deleted
            if (AmazonUtil.isAmazonProductDeleted(delegator, productId)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductPriceUpdate_ProductDeleted", UtilMisc.toMap("productId", productId), locale), MODULE);
                return result;
            }

            AmazonUtil.markAmazonProductPriceAsUpdated(amazonProductPrice);
            amazonProductPrice.store();

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Updates AmazonProductInventory statusId to reflect changes to corresponding product inventory levels or ProductFacility records.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object>  updateAmazonProductInventory(DispatchContext dctx, Map<String, Object>  context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String productId = (String) context.get("productId");
        String facilityId = (String) context.get("facilityId");

        Map<String, Object>  result = ServiceUtil.returnSuccess();

        try {

            // Ignore if no AmazonProductInventory record exists
            GenericValue amazonProductInventory = delegator.findByPrimaryKey("AmazonProductInventory", UtilMisc.toMap("productId", productId));
            if (UtilValidate.isEmpty(amazonProductInventory)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductInventoryUpdate_NoRecord", UtilMisc.toMap("productId", productId), locale), MODULE);
                return result;
            }

            // Ignore if the AmazonProduct is marked deleted
            if (AmazonUtil.isAmazonProductDeleted(delegator, productId)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductInventoryUpdate_ProductDeleted", UtilMisc.toMap("productId", productId), locale), MODULE);
                return result;
            }

            // Sanity check on the Amazon setup
            GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", AmazonConstants.productStoreId));
            if (productStore == null) {
                // this is a failure for this service, but should not cause a global rollback
                return UtilMessage.createAndLogServiceFailure(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ProductStoreNotConfigured", UtilMisc.toMap("productStoreId", AmazonConstants.productStoreId), locale), MODULE);
            } else if (UtilValidate.isEmpty(productStore.getString("inventoryFacilityId"))) {
                // this is a serious configuration error.  If there is an amazon store but it has no facility, then it should cause a global rollback
                return UtilMessage.createAndLogServiceError(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_InvalidAmazonProductStore", UtilMisc.toMap("productStoreId", AmazonConstants.productStoreId), locale), MODULE);
            }

            // Ignore if the facility is incorrect for Amazon
            if (!productStore.getString("inventoryFacilityId").equals(facilityId)) {
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductInventoryUpdate_WrongFacility", UtilMisc.toMap("productId", productId), locale), MODULE);
                return result;
            }

            AmazonUtil.markAmazonProductInventoryAsUpdated(amazonProductInventory);
            amazonProductInventory.store();

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }
}
