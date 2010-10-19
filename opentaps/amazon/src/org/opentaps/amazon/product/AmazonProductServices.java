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

package org.opentaps.amazon.product;

import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.*;

import javolution.util.FastList;
import org.apache.commons.lang.StringEscapeUtils;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.amazon.AmazonConstants;
import org.opentaps.amazon.AmazonUtil;
import org.opentaps.common.product.UtilProduct;
import org.opentaps.common.util.UtilMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Services for Amazon integration product management.
 */
public final class AmazonProductServices {

    private AmazonProductServices() { }

    private static final String MODULE = AmazonProductServices.class.getName();

    /**
     * Service create Amazon product and related entities.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> createOrUpdateAmazonProduct(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String productId = (String) context.get("productId");
        String productTaxCode = AmazonUtil.Strings.STR_N_NULL.normalize((String) context.get("productTaxCode"), locale);
        String nodeId = AmazonUtil.Strings.STR_N_NULL.normalize((String) context.get("nodeId"), locale);
        String itemTypeId = AmazonUtil.Strings.LONG_STR_N_NULL.normalize((String) context.get("itemTypeId"), locale);
        Timestamp releaseDate = (Timestamp) context.get("releaseDate");
        Long tier = (Long) context.get("tier");
        String purchasingCategory = AmazonUtil.Strings.STR_N_NULL.normalize((String) context.get("purchasingCategory"), locale);
        String purchasingSubCategory = AmazonUtil.Strings.STR_N_NULL.normalize((String) context.get("purchasingSubCategory"), locale);
        String packagingType = AmazonUtil.Strings.STR_N_NULL.normalize((String) context.get("packagingType"), locale);
        String underlyingAvailability = (String) context.get("underlyingAvailability");
        String replenishmentCategory = (String) context.get("replenishmentCategory");
        String dropShipStatus = (String) context.get("dropShipStatus");
        String outOfStockWebsiteMessage = (String) context.get("outOfStockWebsiteMessage");
        String registeredParameter = (String) context.get("registeredParameter");
        Long priority = (Long) context.get("priority");
        String browseExclusion = (String) context.get("browseExclusion");
        String recommendationExclusion = (String) context.get("recommendationExclusion");
        List<String> usedForIds = (List<String>) context.get("usedForId");
        List<String> targetAudienceIds = (List<String>) context.get("targetAudienceId");
        List<String> otherItemAttrIds = (List<String>) context.get("otherItemAttrId");
        List<String> bulletPoints = (List<String>) context.get("bulletPoint");
        List<String> searchTerms = (List<String>) context.get("searchTerm");

        Map<String, Object> resultMap = ServiceUtil.returnSuccess();

        try {
            // Check if Product with ID exists
            long productCountById = delegator.findCountByAnd("Product", UtilMisc.toMap("productId", productId));
            if (productCountById != 1) {
                return UtilMessage.createAndLogServiceError("AmazonError_ErrorProductDoesntExists", context, locale, MODULE);
            }

            GenericValue product = delegator.findByPrimaryKey("AmazonProduct", UtilMisc.toMap("productId", productId));
            boolean create = UtilValidate.isEmpty(product);
            if (create) {
                product = AmazonUtil.createAmazonProductRecord(delegator, productId);
            } else if (!AmazonUtil.isAmazonProductDeleted(product)) {
                product.set("statusId", AmazonConstants.statusProductChanged);
            }

            /*
            * Set AmazonProduct values
            */
            product.set("productTaxCode", productTaxCode);
            product.set("nodeId", nodeId);
            product.set("itemTypeId", itemTypeId);
            product.set("releaseDate", releaseDate);
            product.set("priority", priority);
            product.set("browseExclusion", browseExclusion);
            product.set("recommendationExclusion", recommendationExclusion);
            product.set("tier", tier);
            product.set("purchasingCategory", purchasingCategory);
            product.set("purchasingSubCategory", purchasingSubCategory);
            product.set("packagingType", packagingType);
            product.set("underlyingAvailability", underlyingAvailability);
            product.set("replenishmentCategory", replenishmentCategory);
            product.set("dropShipStatus", dropShipStatus);
            product.set("outOfStockWebsiteMessage", outOfStockWebsiteMessage);
            product.set("registeredParameter", registeredParameter);
            product.set("ackStatusId", AmazonConstants.statusProductNotAcked);
            product.set("postFailures", new Long(0));
            product.set("postTimestamp", null);
            product.set("postErrorMessage", null);
            product.set("acknowledgeTimestamp", null);
            product.set("acknowledgeErrorMessage", null);
            product.set("acknowledgeMessageId", null);
            product.set("processingDocumentId", null);
            delegator.createOrStore(product);

            if (usedForIds != null) {
                delegator.removeByAnd("AmazonUsedForValue", UtilMisc.toMap("productId", productId));
                if (UtilValidate.isNotEmpty(usedForIds)) {
                    for (String usedForId : usedForIds) {
                        if (UtilValidate.isNotEmpty(usedForId)) {
                            delegator.create("AmazonUsedForValue", UtilMisc.toMap("productId", productId, "usedForId", usedForId));
                        }
                    }
                }
            }
            if (targetAudienceIds != null) {
                delegator.removeByAnd("AmazonTargetAudienceValue", UtilMisc.toMap("productId", productId));
                if (UtilValidate.isNotEmpty(targetAudienceIds)) {
                    for (String targetAudienceId : targetAudienceIds) {
                        if (UtilValidate.isNotEmpty(targetAudienceId)) {
                            delegator.create("AmazonTargetAudienceValue", UtilMisc.toMap("productId", productId, "targetAudienceId", targetAudienceId));
                        }
                    }
                }
            }
            if (otherItemAttrIds != null) {
                delegator.removeByAnd("AmazonOtherItemAttrValue", UtilMisc.toMap("productId", productId));
                if (UtilValidate.isNotEmpty(otherItemAttrIds)) {
                    for (String otherItemAttrId : otherItemAttrIds) {
                        if (UtilValidate.isNotEmpty(otherItemAttrId)) {
                            delegator.create("AmazonOtherItemAttrValue", UtilMisc.toMap("productId", productId, "otherItemAttrId", otherItemAttrId));
                        }
                    }
                }
            }
            if (bulletPoints != null) {
                delegator.removeByAnd("AmazonProductBulletPoint", UtilMisc.toMap("productId", productId));
                if (UtilValidate.isNotEmpty(bulletPoints)) {
                    for (String bulletPoint : bulletPoints) {
                        if (UtilValidate.isNotEmpty(bulletPoint)) {
                            delegator.create("AmazonProductBulletPoint", UtilMisc.toMap("productId", productId, "description", bulletPoint, "bulletPointId", delegator.getNextSeqId("AmazonProductBulletPoint")));
                        }
                    }
                }
            }
            if (searchTerms != null) {
                delegator.removeByAnd("AmazonProductSearchTerms", UtilMisc.toMap("productId", productId));
                if (UtilValidate.isNotEmpty(searchTerms)) {
                    for (String searchTerm : searchTerms) {
                        if (UtilValidate.isNotEmpty(searchTerm)) {
                            delegator.create("AmazonProductSearchTerms", UtilMisc.toMap("productId", productId, "description", searchTerm, "searchTermId", delegator.getNextSeqId("AmazonProductSearchTerms")));
                        }
                    }
                }
            }

            if (create) {
                delegator.storeAll(AmazonUtil.createAmazonProductRelatedRecords(delegator, productId));
            }

            resultMap.put("productId", productId);

        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        }

        return resultMap;
    }

    /**
     * Service looks over AmzonProduct and collect new products (and those which aren't posted due error as well),
     * creates XML document for Product Feed and post it to Amazon.com.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> publishProductsToAmazon(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String prodId = (String) context.get("productId");

        try {
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("statusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusProductCreated, AmazonConstants.statusProductError, AmazonConstants.statusProductChanged)));
            if (UtilValidate.isNotEmpty(prodId)) {
                conditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, prodId));
            }

            EntityListIterator amazonProductsIt = delegator.findListIteratorByCondition("ViewAmazonProducts", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, Arrays.asList("productId"));

            // Prepare Product Feed document
            Document productFeed = AmazonConstants.soapClient.createDocumentHeader(AmazonConstants.messageTypeProduct);
            Element root = productFeed.getDocumentElement();
            GenericValue viewAmazonProduct = null;
            long messageId = 1;
            Map<GenericValue, String> invalidAmazonProducts = new HashMap<GenericValue, String>();
            List<GenericValue> validAmazonProducts = new ArrayList<GenericValue>();
            while ((viewAmazonProduct = amazonProductsIt.next()) != null) {

                GenericValue amazonProduct = delegator.findByPrimaryKey("AmazonProduct", UtilMisc.toMap("productId", viewAmazonProduct.get("productId")));

                if ((viewAmazonProduct.get("postFailures") != null) && (AmazonConstants.productPostRetryThreshold <= viewAmazonProduct.getLong("postFailures").intValue())) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostProductAttemptsOverThreshold", UtilMisc.<String, Object>toMap("productId", viewAmazonProduct.getString("productId"), "threshold", AmazonConstants.productPostRetryThreshold), locale);
                    Debug.logInfo(errorLog, MODULE);
                    continue;
                }

                String errMessage = null;

                /*
                 * Some elements are required. So, we get it first and go to next iteration
                 * if some of these is absent.
                 */
                String title = AmazonUtil.Strings.LONG_STR_N_NULL.normalize(viewAmazonProduct.getString("productName"), locale);
                if (UtilValidate.isEmpty(title)) {
                    Debug.logWarning(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NoRequiredParameter", UtilMisc.toMap("parameterName", "Title", "productName", viewAmazonProduct.getString("productId")), locale), MODULE);
                }

                List<GenericValue> goodIdents = viewAmazonProduct.getRelated("GoodIdentification", Arrays.asList("lastUpdatedStamp DESC"));
                goodIdents = EntityUtil.filterOutByCondition(goodIdents, EntityCondition.makeCondition(EntityOperator.OR,
                                                  EntityCondition.makeCondition("idValue", EntityOperator.EQUALS, ""),
                                                  EntityCondition.makeCondition("idValue", EntityOperator.EQUALS, null)));

                String upc = null;
                if (AmazonConstants.requireUpcCodes || AmazonConstants.useUPCAsSKU) {

                    // Establish and validate the UPC
                    upc = getProductUPC(delegator, viewAmazonProduct.getString("productId"), locale);
                    if (UtilValidate.isEmpty(upc) && AmazonConstants.requireUpcCodes) {
                         errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_MissingCodeUPC", UtilMisc.toMap("productId", viewAmazonProduct.getString("productId")), locale));
                    } else if (UtilValidate.isNotEmpty(upc) && !UtilProduct.isValidUPC(upc)) {
                        errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_InvalidCodeUPC", UtilMisc.toMap("productId", viewAmazonProduct.getString("productId")), locale));
                    }
                }

                // Establish and validate the SKU
                String sku = getProductSKU(delegator, viewAmazonProduct, upc);

                // Establish the manufacturer name and ID #
                String manufacturerName = AmazonUtil.Strings.STR_N_NULL.normalize(PartyHelper.getPartyName(delegator, viewAmazonProduct.getString("manufacturerPartyId"), false), locale);
                String manufacturerId = null;
                GenericValue manufacturerIdValue = EntityUtil.getFirst(EntityUtil.filterByAnd(goodIdents, UtilMisc.toMap("goodIdentificationTypeId", "MANUFACTURER_ID_NO")));
                if (UtilValidate.isNotEmpty(manufacturerIdValue)) {
                    manufacturerId = AmazonUtil.Strings.FOURTY_STR_N_NULL.normalize(manufacturerIdValue.getString("idValue"), locale);
                }
                if (UtilValidate.isEmpty(manufacturerId)) {
                    manufacturerId = AmazonUtil.Strings.FOURTY_STR_N_NULL.normalize(upc, locale);
                }

                // Limit the remaining goodIdentifications to EAN, ISBN and GTIN
                goodIdents = EntityUtil.filterByCondition(goodIdents, EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.IN, AmazonConstants.goodIdentTypeIds.keySet()));

                // Add errors if some required elements are missing.
                if (UtilValidate.isEmpty(sku) && !AmazonConstants.useUPCAsSKU) {
                    errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NoRequiredParameter", UtilMisc.toMap("parameterName", "SKU", "productName", title), locale));
                }
                String productTaxCode = viewAmazonProduct.getString("productTaxCode");
                if (UtilValidate.isEmpty(productTaxCode)) {
                    errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NoRequiredParameter", UtilMisc.toMap("parameterName", "ProductTaxCode", "productName", title), locale));
                }

                // Check for errors
                if (UtilValidate.isNotEmpty(errMessage)) {
                    invalidAmazonProducts.put(amazonProduct, errMessage);
                    continue;
                }

                /*
                 * Create and add elements and values to XML document
                 */
                Element message = productFeed.createElement("Message");
                root.appendChild(message);
                UtilXml.addChildElementValue(message, "MessageID", "" + messageId, productFeed);
                UtilXml.addChildElementValue(message, "OperationType", "Update", productFeed);
                Element product = productFeed.createElement("Product");
                message.appendChild(product);
                UtilXml.addChildElementValue(product, "SKU", sku, productFeed);
                if (UtilValidate.isNotEmpty(upc)) {
                    Element upcEl = UtilXml.addChildElement(product, "StandardProductID", productFeed);
                    UtilXml.addChildElementValue(upcEl, "Type", "UPC", productFeed);
                    UtilXml.addChildElementValue(upcEl, "Value", upc, productFeed);
                }
                GenericValue goodIdent = EntityUtil.getFirst(goodIdents);
                if (UtilValidate.isNotEmpty(goodIdent)) {
                    Element standardProductId = productFeed.createElement("StandardProductID");
                    product.appendChild(standardProductId);
                    UtilXml.addChildElementValue(standardProductId, "Type", AmazonConstants.goodIdentTypeIds.get(goodIdent.getString("goodIdentificationType")), productFeed);
                    UtilXml.addChildElementValue(standardProductId, "Value", goodIdent.getString("idValue"), productFeed);
                }
                UtilXml.addChildElementValue(product, "ProductTaxCode", productTaxCode, productFeed);
                if (UtilValidate.isNotEmpty(viewAmazonProduct.get("introductionDate"))) {
                    UtilXml.addChildElementValue(product, "LaunchDate", AmazonUtil.convertTimestampToXSDate((Timestamp) viewAmazonProduct.get("introductionDate")), productFeed);
                }
                if (UtilValidate.isNotEmpty(viewAmazonProduct.get("salesDiscontinuationDate"))) {
                    UtilXml.addChildElementValue(product, "DiscontinueDate", AmazonUtil.convertTimestampToXSDate((Timestamp) viewAmazonProduct.get("salesDiscontinuationDate")), productFeed);
                }
                if (UtilValidate.isNotEmpty(viewAmazonProduct.get("releaseDate"))) {
                    UtilXml.addChildElementValue(product, "ReleaseDate", AmazonUtil.convertTimestampToXSDate((Timestamp) viewAmazonProduct.get("releaseDate")), productFeed);
                }
                Element conditionInfo = productFeed.createElement("Condition");
                product.appendChild(conditionInfo);
                UtilXml.addChildElementValue(conditionInfo, "ConditionType", AmazonConstants.productConditionType, productFeed);
                Element descriptionData = productFeed.createElement("DescriptionData");
                product.appendChild(descriptionData);
                UtilXml.addChildElementValue(descriptionData, "Title", UtilValidate.isNotEmpty(title) ? title : sku, productFeed);
                String brandName = AmazonUtil.Strings.STR_N_NULL.normalize(viewAmazonProduct.getString("brandName"), locale);
                if (UtilValidate.isEmpty(brandName)) {
                    EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                                                EntityCondition.makeCondition("productId", EntityOperator.EQUALS, viewAmazonProduct.getString("productId")),
                                                EntityCondition.makeCondition("productFeatureTypeId", EntityOperator.EQUALS, "BRAND"),
                                                EntityCondition.makeCondition("productFeatureApplTypeId", EntityOperator.EQUALS, "STANDARD_FEATURE"),
                                                EntityUtil.getFilterByDateExpr());
                    GenericValue pf = EntityUtil.getFirst(delegator.findByCondition("ProductFeatureAndAppl", cond, null, Arrays.asList("fromDate DESC")));
                    if (UtilValidate.isNotEmpty(pf)) {
                        brandName = AmazonUtil.Strings.STR_N_NULL.normalize(pf.getString("description"), locale);
                    }
                }
                if (UtilValidate.isNotEmpty(brandName)) {
                    UtilXml.addChildElementValue(descriptionData, "Brand", brandName, productFeed);
                }

                String description = viewAmazonProduct.getString(AmazonConstants.productDescriptionField);
                description = UtilValidate.isEmpty(description) ? null : description.replaceAll("<[^ ].*?>", "");
                description = StringEscapeUtils.escapeHtml(description);
                if (UtilValidate.isNotEmpty(description)) {

                    // Put the XML reserved characters back, since the parser will escape them again and we get things like &amp;amp;
                    description = description.replaceAll("&amp;", "&").replaceAll("&gt;", ">").replaceAll("&lt;", "<").replaceAll("&quot;", "\"");
                    UtilXml.addChildElementValue(descriptionData, "Description", AmazonUtil.Strings.DESCRIPTION.normalize(description, locale), productFeed);
                }
                List<GenericValue> bulletPoints = viewAmazonProduct.getRelated("AmazonProductBulletPoint");
                if (bulletPoints != null) {
                    if (bulletPoints.size() > AmazonConstants.productFeedMaxBulletPoints) {
                        String infoMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_TooMuchElementsInFeed", UtilMisc.<String, Object>toMap("max", AmazonConstants.productFeedMaxBulletPoints, "elementName", "Bullet Points", "elementsCount", bulletPoints.size(), "productId", viewAmazonProduct.getString("productId")), locale);
                        Debug.logInfo(infoMessage, MODULE);
                    }
                    int index = 0;
                    for (GenericValue bulletPoint : bulletPoints) {
                        UtilXml.addChildElementValue(descriptionData, "BulletPoint", AmazonUtil.Strings.LONG_STR_N_NULL.normalize(bulletPoint.getString("description"), locale), productFeed);
                        index++;
                        if (index == AmazonConstants.productFeedMaxBulletPoints) {
                            break;
                        }
                    }
                }

                Double lengthValue = viewAmazonProduct.getDouble("productDepth");
                Double widthValue = viewAmazonProduct.getDouble("productWidth");
                Double heightValue = viewAmazonProduct.getDouble("productHeight");
                Double weightValue = viewAmazonProduct.getDouble("weight");
                String currentUomId = null;
                if (lengthValue != null || widthValue != null || heightValue != null || weightValue != null) {
                    Element itemDimensions = productFeed.createElement("ItemDimensions");
                    descriptionData.appendChild(itemDimensions);
                    if (lengthValue != null) {
                        Element itemLength = UtilXml.addChildElementValue(itemDimensions, "Length", lengthValue.toString(), productFeed);
                        currentUomId = AmazonConstants.units.get(viewAmazonProduct.getString("depthUomId"));
                        itemLength.setAttribute("unitOfMeasure", UtilValidate.isNotEmpty(currentUomId) ? currentUomId : AmazonConstants.defaultLengthUom);
                    }
                    if (widthValue != null) {
                        Element itemWidth = UtilXml.addChildElementValue(itemDimensions, "Width", widthValue.toString(), productFeed);
                        currentUomId = AmazonConstants.units.get(viewAmazonProduct.getString("widthUomId"));
                        itemWidth.setAttribute("unitOfMeasure", UtilValidate.isNotEmpty(currentUomId) ? currentUomId : AmazonConstants.defaultLengthUom);
                    }
                    if (heightValue != null) {
                        Element itemHeight = UtilXml.addChildElementValue(itemDimensions, "Height", heightValue.toString(), productFeed);
                        currentUomId = AmazonConstants.units.get(viewAmazonProduct.getString("heightUomId"));
                        itemHeight.setAttribute("unitOfMeasure", UtilValidate.isNotEmpty(currentUomId) ? currentUomId : AmazonConstants.defaultLengthUom);
                    }
                    if (weightValue != null) {
                        Element itemWeight = UtilXml.addChildElementValue(itemDimensions, "Weight", weightValue.toString(), productFeed);
                        currentUomId = AmazonConstants.units.get(viewAmazonProduct.getString("weightUomId"));
                        itemWeight.setAttribute("unitOfMeasure", UtilValidate.isNotEmpty(currentUomId) ? currentUomId : AmazonConstants.defaultWeightUom);
                    }
                }

                lengthValue = viewAmazonProduct.getDouble("shippingDepth");
                widthValue = viewAmazonProduct.getDouble("shippingWidth");
                heightValue = viewAmazonProduct.getDouble("shippingHeight");
                if (lengthValue != null || widthValue != null || heightValue != null || weightValue != null) {
                    Element packageDimensions = productFeed.createElement("PackageDimensions");
                    descriptionData.appendChild(packageDimensions);
                    if (lengthValue != null) {
                        Element packageLength = UtilXml.addChildElementValue(packageDimensions, "Length", lengthValue.toString(), productFeed);
                        currentUomId = AmazonConstants.units.get(viewAmazonProduct.getString("depthUomId"));
                        packageLength.setAttribute("unitOfMeasure", UtilValidate.isNotEmpty(currentUomId) ? currentUomId : AmazonConstants.defaultLengthUom);
                    }
                    if (widthValue != null) {
                        Element packageWidth = UtilXml.addChildElementValue(packageDimensions, "Width", widthValue.toString(), productFeed);
                        currentUomId = AmazonConstants.units.get(viewAmazonProduct.getString("widthUomId"));
                        packageWidth.setAttribute("unitOfMeasure", UtilValidate.isNotEmpty(currentUomId) ? currentUomId : AmazonConstants.defaultLengthUom);
                    }
                    if (heightValue != null) {
                        Element packageHeight = UtilXml.addChildElementValue(packageDimensions, "Height", heightValue.toString(), productFeed);
                        currentUomId = AmazonConstants.units.get(viewAmazonProduct.getString("heightUomId"));
                        packageHeight.setAttribute("unitOfMeasure", UtilValidate.isNotEmpty(currentUomId) ? currentUomId : AmazonConstants.defaultLengthUom);
                    }
                    if (weightValue != null) {
                        Element packageWeight = UtilXml.addChildElementValue(packageDimensions, "Weight", weightValue.toString(), productFeed);
                        currentUomId = AmazonConstants.units.get(viewAmazonProduct.getString("weightUomId"));
                        packageWeight.setAttribute("unitOfMeasure", UtilValidate.isNotEmpty(currentUomId) ? currentUomId : AmazonConstants.defaultWeightUom);
                    }
                }

                UtilXml.addChildElementValue(descriptionData, "MerchantCatalogNumber", AmazonUtil.Strings.FOURTY_STR_N_NULL.normalize(viewAmazonProduct.getString("productId"), locale), productFeed);

                // Try to find a price for the Amazon productStoreGroup first
                EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                                          EntityCondition.makeCondition("productId", EntityOperator.EQUALS, amazonProduct.getString("productId")),
                                          EntityCondition.makeCondition("productPriceTypeId", EntityOperator.EQUALS, AmazonConstants.priceStandard),
                                          EntityCondition.makeCondition("productStoreGroupId", EntityOperator.EQUALS, AmazonConstants.priceProductStoreGroup),
                                          EntityUtil.getFilterByDateExpr());
                GenericValue msrpValue = EntityUtil.getFirst(delegator.findByCondition("ProductPrice", cond, null, Arrays.asList("lastUpdatedStamp DESC")));
                if (UtilValidate.isEmpty(msrpValue)) {

                    // If there's no price for the Amazon productStoreGroup, try _NA_
                    cond = EntityCondition.makeCondition(EntityOperator.AND,
                                         EntityCondition.makeCondition("productId", EntityOperator.EQUALS, amazonProduct.getString("productId")),
                                         EntityCondition.makeCondition("productPriceTypeId", EntityOperator.EQUALS, AmazonConstants.priceStandard),
                                         EntityCondition.makeCondition("productStoreGroupId", EntityOperator.EQUALS, "_NA_"),
                                         EntityUtil.getFilterByDateExpr());
                    msrpValue = EntityUtil.getFirst(delegator.findByCondition("ProductPrice", cond, null, Arrays.asList("lastUpdatedStamp DESC")));
                }
                BigDecimal msrp = null;
                String msrpCurrency = null;
                if (UtilValidate.isNotEmpty(msrpValue)) {
                    msrp = msrpValue.getBigDecimal("price").setScale(AmazonConstants.decimals, AmazonConstants.rounding);
                    msrpCurrency = msrpValue.getString("currencyUomId");
                }

                if (UtilValidate.isNotEmpty(msrp)) {
                Element msrpEl = UtilXml.addChildElementValue(descriptionData, "MSRP", msrp.toString(), productFeed);
                msrpEl.setAttribute("currency", UtilValidate.isNotEmpty(msrpCurrency) ? msrpCurrency : UtilProperties.getPropertyValue("opentaps.properties", "defaultCurrencyUomId"));
                }

                if (UtilValidate.isEmpty(manufacturerName) && UtilValidate.isNotEmpty(brandName)) {
                    manufacturerName = brandName;
                }
                if (UtilValidate.isNotEmpty(manufacturerName)) {
                    UtilXml.addChildElementValue(descriptionData, "Manufacturer", manufacturerName, productFeed);
                }
                if (UtilValidate.isNotEmpty(manufacturerId)) {
                    UtilXml.addChildElementValue(descriptionData, "MfrPartNumber", manufacturerId, productFeed);
                }

                // Add a search term for the product name.  Amazon does not like null <SearchTerms/> tag so make sure it is not empty
                if (UtilValidate.isNotEmpty(title)) {
                    UtilXml.addChildElementValue(descriptionData, "SearchTerms", AmazonUtil.Strings.STR_N_NULL.normalize(title, locale), productFeed);
                }

                // Add any other search terms
                List<GenericValue> searchTerms = viewAmazonProduct.getRelated("AmazonProductSearchTerms");
                if (searchTerms != null) {
                    if (searchTerms.size() > AmazonConstants.productFeedMaxSearchTerms) {
                        String infoMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_TooMuchElementsInFeed", UtilMisc.<String, Object>toMap("max", AmazonConstants.productFeedMaxSearchTerms, "elementName", "Search Terms", "elementsCount", searchTerms.size(), "productId", viewAmazonProduct.getString("productId")), locale);
                        Debug.logInfo(infoMessage, MODULE);
                    }
                    int index = 0;
                    for (GenericValue searchTerm : searchTerms) {
                        UtilXml.addChildElementValue(descriptionData, "SearchTerms", AmazonUtil.Strings.STR_N_NULL.normalize(searchTerm.getString("description"), locale), productFeed);
                        index++;
                        if (index == AmazonConstants.productFeedMaxSearchTerms) {
                            break;
                        }
                    }
                }

                List<GenericValue> usedForList = viewAmazonProduct.getRelated("AmazonUsedForValue");
                if (usedForList != null) {
                    if (usedForList.size() > AmazonConstants.productFeedMaxUsedFor) {
                        String infoMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_TooMuchElementsInFeed", UtilMisc.<String, Object>toMap("max", AmazonConstants.productFeedMaxUsedFor, "elementName", "Used For", "elementsCount", searchTerms.size(), "productId", viewAmazonProduct.getString("productId")), locale);
                        Debug.logInfo(infoMessage, MODULE);
                    }
                    int index = 0;
                    for (GenericValue usedFor : usedForList) {
                        UtilXml.addChildElementValue(descriptionData, "UsedFor", AmazonUtil.Strings.STR_N_NULL.normalize(usedFor.getString("usedForId"), locale), productFeed);
                        index++;
                        if (index == AmazonConstants.productFeedMaxUsedFor) {
                            break;
                        }
                    }
                }

                String itemType = viewAmazonProduct.getString("itemTypeId");
                if (UtilValidate.isNotEmpty(itemType)) {
                    UtilXml.addChildElementValue(descriptionData, "ItemType", itemType, productFeed);
                }

                List<GenericValue> otherItemAttributes = viewAmazonProduct.getRelated("AmazonOtherItemAttrValue");
                if (otherItemAttributes != null) {
                    if (otherItemAttributes.size() > AmazonConstants.productFeedMaxOtherItemAttributes) {
                        String infoMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_TooMuchElementsInFeed", UtilMisc.<String, Object>toMap("max", AmazonConstants.productFeedMaxOtherItemAttributes, "elementName", "Other Item Attribute", "elementsCount", searchTerms.size(), "productId", viewAmazonProduct.getString("productId")), locale);
                        Debug.logInfo(infoMessage, MODULE);
                    }
                    int index = 0;
                    for (GenericValue attribute : otherItemAttributes) {
                        UtilXml.addChildElementValue(descriptionData, "OtherItemAttributes", AmazonUtil.Strings.LONG_STR_N_NULL.normalize(attribute.getString("otherItemAttrId"), locale), productFeed);
                        index++;
                        if (index == AmazonConstants.productFeedMaxOtherItemAttributes) {
                            break;
                        }
                    }
                }

                List<GenericValue> targetAudience = viewAmazonProduct.getRelated("AmazonTargetAudienceValue");
                if (targetAudience != null) {
                    if (targetAudience.size() > AmazonConstants.productFeedMaxTargetAudience) {
                        String infoMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_TooMuchElementsInFeed", UtilMisc.<String, Object>toMap("max", AmazonConstants.productFeedMaxTargetAudience, "elementName", "Target Audience", "elementsCount", searchTerms.size(), "productId", viewAmazonProduct.getString("productId")), locale);
                        Debug.logInfo(infoMessage, MODULE);
                    }
                    int index = 0;
                    for (GenericValue audience : targetAudience) {
                        UtilXml.addChildElementValue(descriptionData, "TargetAudience", AmazonUtil.Strings.STR_N_NULL.normalize(audience.getString("targetAudienceId"), locale), productFeed);
                        index++;
                        if (index == AmazonConstants.productFeedMaxTargetAudience) {
                            break;
                        }
                    }
                }

                amazonProduct.set("acknowledgeMessageId", "" + messageId);
                validAmazonProducts.add(amazonProduct);
                messageId++;
                if (messageId % 500 == 0) {
                    Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_Processed_Records_Product", UtilMisc.toMap("count", messageId), locale), MODULE);
                }
            }
            amazonProductsIt.close();

            LinkedHashMap<GenericValue, String> emailErrorMessages = new LinkedHashMap<GenericValue, String>();

            if (UtilValidate.isEmpty(validAmazonProducts)) {
                String infoMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostNoNewProducts", locale);
                Debug.logInfo(infoMessage, MODULE);
            } else {

                /*
                 * Post product document and get transaction ID. Store ID with product for later use.
                 */
                boolean success = true;
                String postErrorMessage = null;
                long processingDocumentId = -1;
                try {
                    String xml = UtilXml.writeXmlDocument(productFeed);
                    Debug.logVerbose(xml, MODULE);
                    Writer writer = new OutputStreamWriter(new FileOutputStream(AmazonConstants.xmlOutputLocation + "AmazonProductFeed_" + AmazonConstants.xmlOutputDateFormat.format(new Date()) + ".xml"), "UTF-8");
                    writer.write(xml);
                    writer.close();
                    processingDocumentId = AmazonConstants.soapClient.postProducts(xml);
                    Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ProcessingDocumentId_Product", UtilMisc.toMap("processingDocumentId", processingDocumentId), locale), MODULE);
                } catch (RemoteException e) {
                    success = false;
                    postErrorMessage = e.getMessage();
                    List<String> productIds = EntityUtil.getFieldListFromEntityList(validAmazonProducts, "productId", true);
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostProductError", UtilMisc.toMap("productIds", productIds, "errorMessage", postErrorMessage), locale);
                    Debug.logError(errorLog, MODULE);
                }

                // Store operational data of the post attempt
                for (GenericValue validAmazonProduct : validAmazonProducts) {
                    validAmazonProduct.set("statusId", success ? AmazonConstants.statusProductPosted : AmazonConstants.statusProductError);
                    validAmazonProduct.set("postTimestamp", UtilDateTime.nowTimestamp());
                    validAmazonProduct.set("postErrorMessage", success ? null : postErrorMessage);
                    long postFailures = 0;
                    if (validAmazonProduct.getLong("postFailures") != null) {
                        postFailures = validAmazonProduct.getLong("postFailures");
                    }
                    if (!success) {
                        validAmazonProduct.set("postFailures", postFailures + 1);
                    }
                    validAmazonProduct.set("processingDocumentId", success ? processingDocumentId : null);
                    validAmazonProduct.set("ackStatusId", AmazonConstants.statusProductNotAcked);
                    validAmazonProduct.set("acknowledgeTimestamp", null);
                    validAmazonProduct.set("acknowledgeErrorMessage", null);
                    validAmazonProduct.store();
                    if (AmazonConstants.sendErrorEmails && !success) {
                        emailErrorMessages.put(validAmazonProduct, postErrorMessage);
                    }
                }
            }

            for (GenericValue invalidAmazonProduct : invalidAmazonProducts.keySet()) {
                String errorMessage = invalidAmazonProducts.get(invalidAmazonProduct);
                invalidAmazonProduct.set("statusId", AmazonConstants.statusProductError);
                invalidAmazonProduct.set("postTimestamp", UtilDateTime.nowTimestamp());
                invalidAmazonProduct.set("postErrorMessage", errorMessage);
                invalidAmazonProduct.set("postFailures", invalidAmazonProduct.getLong("postFailures") + 1);
                invalidAmazonProduct.set("processingDocumentId", null);
                invalidAmazonProduct.set("ackStatusId", AmazonConstants.statusProductNotAcked);
                invalidAmazonProduct.set("acknowledgeTimestamp", null);
                invalidAmazonProduct.set("acknowledgeErrorMessage", null);
                invalidAmazonProduct.store();
                if (AmazonConstants.sendErrorEmails) {
                    emailErrorMessages.put(invalidAmazonProduct, errorMessage);
                }
            }

            if (AmazonConstants.sendErrorEmails && UtilValidate.isNotEmpty(emailErrorMessages)) {
                AmazonUtil.sendBulkErrorEmail(dispatcher, userLogin, emailErrorMessages, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_PostProduct", AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriProducts);
            }

        } catch (GenericEntityException gee) {
            UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        } catch (IOException ioe) {
            UtilMessage.createAndLogServiceError(ioe, locale, MODULE);
        } catch (GenericServiceException gse) {
            UtilMessage.createAndLogServiceError(gse, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    private static String getProductSKU(Delegator delegator, GenericValue amazonProductValue, String upc) throws GenericEntityException {
        String sku = null;
        if (AmazonConstants.useProductIdAsSKU) {
            sku = amazonProductValue.getString("productId");
        } else if (AmazonConstants.useUPCAsSKU && UtilValidate.isNotEmpty(upc)) {
            sku = upc;
        } else {
            sku = UtilProduct.getProductSKU(amazonProductValue.getString("productId"), delegator);
        }
        return sku;
    }

    private static String getProductUPC(Delegator delegator, String productId, Locale locale) throws GenericEntityException {

        // Amazon only accepts UPC-A codes, so try to find one first
        EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                                    EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                                    EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.EQUALS, "UPCA"),
                                    EntityCondition.makeCondition("idValue", EntityOperator.NOT_EQUAL, ""));
        GenericValue upcValue = EntityUtil.getFirst(delegator.findByCondition("GoodIdentification", cond, null, Arrays.asList("lastUpdatedStamp DESC")));
        if (UtilValidate.isNotEmpty(upcValue)) {
            return upcValue.getString("idValue");
        }

        // If there's no UPC-A, try to find a UPC-E and expand it to a UPC-A
        cond = EntityCondition.makeCondition(EntityOperator.AND,
                                    EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                                    EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.EQUALS, "UPCE"),
                                    EntityCondition.makeCondition("idValue", EntityOperator.NOT_EQUAL, ""));
        upcValue = EntityUtil.getFirst(delegator.findByCondition("GoodIdentification", cond, null, Arrays.asList("lastUpdatedStamp DESC")));
        if (UtilValidate.isNotEmpty(upcValue)) {
            String upce = upcValue.getString("idValue");
            String upca = UtilProduct.expandUPCE(upce);
            Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ExpandingUPCE", UtilMisc.toMap("productId", productId, "upce", upce, "upca", upca), locale), MODULE);
            return upca;
        }
        return null;
    }

    /**
     * Service looks over AmzonProductPrice and collect product prices that haven't been posted yet,
     * creates XML document for Price Feed and post it to Amazon.com.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> publishProductPriceToAmazon(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String prodId = (String) context.get("productId");

        try {
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("statusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusProductCreated, AmazonConstants.statusProductChanged, AmazonConstants.statusProductError)));
            if (UtilValidate.isNotEmpty(prodId)) {
                conditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, prodId));
            }

            TransactionUtil.begin();
            EntityListIterator amazonPriceIt = delegator.findListIteratorByCondition("AmazonProductPrice", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, Arrays.asList("productId"));
            TransactionUtil.commit();

            // Prepare Price Feed document
            long messageId = 1;
            Map<GenericValue, String> invalidAmazonPrices = new HashMap<GenericValue, String>();
            List<GenericValue> validAmazonPrices = new ArrayList<GenericValue>();
            Document priceFeed = AmazonConstants.soapClient.createDocumentHeader(AmazonConstants.messageTypePrice);
            Element root = priceFeed.getDocumentElement();
            GenericValue amazonPrice = null;
            while ((amazonPrice = amazonPriceIt.next()) != null) {

                String errMessage = null;

                if (AmazonConstants.productPostRetryThreshold <= amazonPrice.getLong("postFailures").intValue()) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostPriceAttemptsOverThreshold", UtilMisc.<String, Object>toMap("productId", amazonPrice.getString("productId"), "threshold", AmazonConstants.productPostRetryThreshold), locale);
                    Debug.logInfo(errorLog, MODULE);
                    continue;
                }

                // Ignore products marked deleted
                if (AmazonUtil.isAmazonProductDeleted(delegator, amazonPrice.getString("productId"))) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductPrice_ProductDeleted", UtilMisc.toMap("productId", amazonPrice.getString("productId")), locale);
                    Debug.logError(errorLog, MODULE);
                    continue;
                }

                // check if this product was exported and acknowledged earlier
                if (delegator.findCountByAnd("AmazonProduct", UtilMisc.toMap("productId", amazonPrice.getString("productId"), "statusId", AmazonConstants.statusProductPosted, "ackStatusId", AmazonConstants.statusProductAckRecv)) != 1) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostPriceNonExistentProduct", UtilMisc.toMap("productId", amazonPrice.getString("productId")), locale);
                    Debug.logError(errorLog, MODULE);
                    continue;
                }

                /*
                 * Some elements are required. So, we get it first and go to next iteration
                 * if some of these is absent.
                 */

                String upc = null;
                if (AmazonConstants.requireUpcCodes || AmazonConstants.useUPCAsSKU) {

                    // Establish and validate the UPC
                    upc = getProductUPC(delegator, amazonPrice.getString("productId"), locale);
                    if (UtilValidate.isEmpty(upc) && AmazonConstants.requireUpcCodes) {
                         errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_MissingCodeUPC", UtilMisc.toMap("productId", amazonPrice.getString("productId")), locale));
                    } else if (UtilValidate.isNotEmpty(upc) && !UtilProduct.isValidUPC(upc)) {
                        errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_InvalidCodeUPC", UtilMisc.toMap("productId", amazonPrice.getString("productId")), locale));
                    }
                }

                // Establish and validate the SKU
                String sku = getProductSKU(delegator, amazonPrice, upc);
                if (UtilValidate.isEmpty(sku) && !AmazonConstants.useUPCAsSKU) {
                    errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NoRequiredParameter", UtilMisc.toMap("parameterName", "SKU", "productName", amazonPrice.getString("productId")), locale));
                }

                if (UtilValidate.isNotEmpty(errMessage)) {
                    invalidAmazonPrices.put(amazonPrice, errMessage);
                    continue;
                }

                // Standard price - filtered by date
                BigDecimal standardPrice = null;
                String standardPriceCurrency = null;

                // Try to find a price for the Amazon productStoreGroup first
                EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                                          EntityCondition.makeCondition("productId", EntityOperator.EQUALS, amazonPrice.getString("productId")),
                                          EntityCondition.makeCondition("productPriceTypeId", EntityOperator.EQUALS, AmazonConstants.priceStandard),
                                          EntityCondition.makeCondition("productStoreGroupId", EntityOperator.EQUALS, AmazonConstants.priceProductStoreGroup),
                                          EntityUtil.getFilterByDateExpr());
                GenericValue standardPriceVal = EntityUtil.getFirst(delegator.findByCondition("ProductPrice", cond, null, Arrays.asList("lastUpdatedStamp DESC")));
                if (UtilValidate.isEmpty(standardPriceVal)) {

                    // If there's no price for the Amazon productStoreGroup, try _NA_
                    cond = EntityCondition.makeCondition(EntityOperator.AND,
                                         EntityCondition.makeCondition("productId", EntityOperator.EQUALS, amazonPrice.getString("productId")),
                                         EntityCondition.makeCondition("productPriceTypeId", EntityOperator.EQUALS, AmazonConstants.priceStandard),
                                         EntityCondition.makeCondition("productStoreGroupId", EntityOperator.EQUALS, "_NA_"),
                                         EntityUtil.getFilterByDateExpr());
                    standardPriceVal = EntityUtil.getFirst(delegator.findByCondition("ProductPrice", cond, null, Arrays.asList("lastUpdatedStamp DESC")));
                }
                if (UtilValidate.isNotEmpty(standardPriceVal)) {
                    standardPrice = standardPriceVal.getBigDecimal("price").setScale(AmazonConstants.decimals, AmazonConstants.rounding);
                    standardPriceCurrency = standardPriceVal.getString("currencyUomId");
                }

                // Sale price - NOT filtered by date
                BigDecimal salePrice = null;
                String salePriceCurrency = null;
                String saleStartDate = null;
                String saleEndDate = null;
                cond = EntityCondition.makeCondition(EntityOperator.AND,
                                     EntityCondition.makeCondition("productId", EntityOperator.EQUALS, amazonPrice.getString("productId")),
                                     EntityCondition.makeCondition("productPriceTypeId", EntityOperator.EQUALS, AmazonConstants.priceSale),
                                     EntityCondition.makeCondition("productStoreGroupId", EntityOperator.EQUALS, AmazonConstants.priceProductStoreGroup));
                GenericValue salePriceVal = EntityUtil.getFirst(delegator.findByCondition("ProductPrice", cond, null, Arrays.asList("lastUpdatedStamp DESC")));
                if (UtilValidate.isNotEmpty(salePriceVal)) {
                    salePrice = salePriceVal.getBigDecimal("price").setScale(AmazonConstants.decimals, AmazonConstants.rounding);
                    salePriceCurrency = salePriceVal.getString("currencyUomId");
                    Timestamp fromDate = salePriceVal.getTimestamp("fromDate");
                    Timestamp thruDate = salePriceVal.getTimestamp("thruDate");
                    Timestamp now = UtilDateTime.nowTimestamp();
                    if (UtilValidate.isEmpty(thruDate)) {
                        Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostPriceNoSaleEndDate", UtilMisc.toMap("productId", amazonPrice.getString("productId")), locale), MODULE);

                        // Amazon requires an end date for the sale, so add twenty years or so
                        saleEndDate = AmazonUtil.convertTimestampToXSDate(new Timestamp(now.getTime() + 631152000000L));
                    } else if (thruDate.before(now)) {
                        Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostPriceSalePriceSkipped", UtilMisc.toMap("productId", amazonPrice.getString("productId"), "thruDate", thruDate), locale), MODULE);

                        // The sale is over, so leave out the sale price so that it will be deleted in the Amazon system
                        salePrice = null;
                    }
                    if (UtilValidate.isEmpty(fromDate)) {

                        // Amazon requires a start date for the sale, so use the current time
                        saleStartDate = AmazonUtil.convertTimestampToXSDate(now);
                    } else {

                        // Amazon requires dates to be in the future, so use the current time if the start date for the sale is past
                        saleStartDate = AmazonUtil.convertTimestampToXSDate(fromDate.before(now) ? now : fromDate);
                    }
                }

                /*
                * Create and add required elements and values
                */
                Element message = priceFeed.createElement("Message");
                root.appendChild(message);
                UtilXml.addChildElementValue(message, "MessageID", "" + messageId, priceFeed);
                Element price = priceFeed.createElement("Price");
                message.appendChild(price);
                UtilXml.addChildElementValue(price, "SKU", sku, priceFeed);

                // Delist the product (set standard price to zero) if certain conditions are true
                boolean deListProduct = UtilValidate.isEmpty(standardPrice) || ((UtilValidate.isEmpty(salePriceVal) && AmazonConstants.delistProductIfNoSalePrice));

                Element standardPriceElement = UtilXml.addChildElementValue(price, "StandardPrice", deListProduct ? "0.0" : standardPrice.toString(), priceFeed);
                standardPriceElement.setAttribute("currency", UtilValidate.isNotEmpty(standardPriceCurrency) ? standardPriceCurrency : UtilProperties.getPropertyValue("opentaps.properties", "defaultCurrencyUomId"));
                if (UtilValidate.isEmpty(standardPrice)) {
                    standardPriceElement.setAttribute("zero", "true");
                }
                if (UtilValidate.isNotEmpty(salePrice)) {
                    Element sale = priceFeed.createElement("Sale");
                    price.appendChild(sale);
                    UtilXml.addChildElementValue(sale, "StartDate", saleStartDate, priceFeed);
                    UtilXml.addChildElementValue(sale, "EndDate", saleEndDate, priceFeed);
                    Element salePriceElement = UtilXml.addChildElementValue(sale, "SalePrice", salePrice.toString(), priceFeed);
                    salePriceElement.setAttribute("currency", salePriceCurrency);
                }

                amazonPrice.set("acknowledgeMessageId", "" + messageId);
                validAmazonPrices.add(amazonPrice);
                messageId++;
                if (messageId % 500 == 0) {
                    Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_Processed_Records_Price", UtilMisc.toMap("count", messageId), locale), MODULE);
                }
            }
            amazonPriceIt.close();

            LinkedHashMap<GenericValue, String> emailErrorMessages = new LinkedHashMap<GenericValue, String>();

            if (UtilValidate.isEmpty(validAmazonPrices)) {
                String infoMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostNoNewPrices", locale);
                Debug.logInfo(infoMessage, MODULE);
            } else {

                boolean success = true;
                String postErrorMessage = null;
                long processingDocumentId = -1;
                try {
                    String xml = UtilXml.writeXmlDocument(priceFeed);
                    Debug.logVerbose(xml, MODULE);
                    Writer writer = new OutputStreamWriter(new FileOutputStream(AmazonConstants.xmlOutputLocation + "AmazonPriceFeed_" + AmazonConstants.xmlOutputDateFormat.format(new Date()) + ".xml"), "UTF-8");
                    writer.write(xml);
                    writer.close();
                    processingDocumentId = AmazonConstants.soapClient.postProductPrices(xml);
                    Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ProcessingDocumentId_Price", UtilMisc.toMap("processingDocumentId", processingDocumentId), locale), MODULE);
                } catch (RemoteException e) {
                    success = false;
                    postErrorMessage = e.getMessage();
                    List<String> productIds = EntityUtil.getFieldListFromEntityList(validAmazonPrices, "productId", true);
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostPriceError", UtilMisc.toMap("productIds", productIds, "errorMessage", postErrorMessage), locale);
                    Debug.logError(errorLog, MODULE);
                }

                // Store operational data of the post attempt
                for (GenericValue validAmazonPrice : validAmazonPrices) {
                    validAmazonPrice.set("statusId", success ? AmazonConstants.statusProductPosted : AmazonConstants.statusProductError);
                    validAmazonPrice.set("postTimestamp", UtilDateTime.nowTimestamp());
                    validAmazonPrice.set("postErrorMessage", success ? null : postErrorMessage);
                    if (!success) {
                        validAmazonPrice.set("postFailures", validAmazonPrice.getLong("postFailures") + 1);
                    }
                    validAmazonPrice.set("processingDocumentId", success ? processingDocumentId : null);
                    validAmazonPrice.set("ackStatusId", AmazonConstants.statusProductNotAcked);
                    validAmazonPrice.set("acknowledgeTimestamp", null);
                    validAmazonPrice.set("acknowledgeErrorMessage", null);
                    validAmazonPrice.store();
                    if (AmazonConstants.sendErrorEmails && !success) {
                        emailErrorMessages.put(validAmazonPrice, postErrorMessage);
                    }
                }
            }

            for (GenericValue invalidAmazonPrice : invalidAmazonPrices.keySet()) {
                String errorMessage = invalidAmazonPrices.get(invalidAmazonPrice);
                invalidAmazonPrice.set("statusId", AmazonConstants.statusProductError);
                invalidAmazonPrice.set("postTimestamp", UtilDateTime.nowTimestamp());
                invalidAmazonPrice.set("postErrorMessage", errorMessage);
                invalidAmazonPrice.set("postFailures", invalidAmazonPrice.getLong("postFailures") + 1);
                invalidAmazonPrice.set("processingDocumentId", null);
                invalidAmazonPrice.set("ackStatusId", AmazonConstants.statusProductNotAcked);
                invalidAmazonPrice.set("acknowledgeTimestamp", null);
                invalidAmazonPrice.set("acknowledgeErrorMessage", null);
                invalidAmazonPrice.store();
                if (AmazonConstants.sendErrorEmails) {
                    emailErrorMessages.put(invalidAmazonPrice, errorMessage);
                }
            }

            if (AmazonConstants.sendErrorEmails && UtilValidate.isNotEmpty(emailErrorMessages)) {
                AmazonUtil.sendBulkErrorEmail(dispatcher, userLogin, emailErrorMessages, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_PostPrice", AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriProducts);
            }

        } catch (GenericEntityException gee) {
            UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        } catch (IOException ioe) {
            UtilMessage.createAndLogServiceError(ioe, locale, MODULE);
        } catch (GenericServiceException gse) {
            UtilMessage.createAndLogServiceError(gse, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service looks over AmazonProductImage and collect product images that haven't been posted yet,
     * creates XML document for ProductImage Feed and post it to Amazon.com.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> publishProductImagesToAmazon(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String prodId = (String) context.get("productId");

        try {
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("statusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusProductCreated, AmazonConstants.statusProductChanged, AmazonConstants.statusProductError)));
            if (UtilValidate.isNotEmpty(prodId)) {
                conditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, prodId));
            }

            TransactionUtil.begin();
            EntityListIterator amazonImageIt = delegator.findListIteratorByCondition("AmazonProductImage", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, Arrays.asList("productId"));
            TransactionUtil.commit();

            long messageId = 1;
            long processedProductImages = 0;
            Map<GenericValue, String> invalidAmazonImages = new HashMap<GenericValue, String>();
            List<GenericValue> validAmazonImages = new ArrayList<GenericValue>();
            List<GenericValue> amazonProductImageAcks = new ArrayList<GenericValue>();
            Document imageFeed = AmazonConstants.soapClient.createDocumentHeader(AmazonConstants.messageTypeProductImage);
            Element root = imageFeed.getDocumentElement();
            GenericValue amazonProductImage = null;
            while ((amazonProductImage = amazonImageIt.next()) != null) {

                String errMessage = null;

                if (AmazonConstants.productPostRetryThreshold <= amazonProductImage.getLong("postFailures").intValue()) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostImageAttemptsOverThreshold", UtilMisc.<String, Object>toMap("productId", amazonProductImage.getString("productId"), "threshold", AmazonConstants.productPostRetryThreshold), locale);
                    Debug.logInfo(errorLog, MODULE);
                    continue;
                }

                // check if this product was exported and acknowledged earlier
                if (delegator.findCountByAnd("AmazonProduct", UtilMisc.toMap("productId", amazonProductImage.getString("productId"), "statusId", AmazonConstants.statusProductPosted, "ackStatusId", AmazonConstants.statusProductAckRecv)) != 1) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostImageNonExistentProduct", UtilMisc.toMap("productId", amazonProductImage.getString("productId")), locale);
                    Debug.logError(errorLog, MODULE);
                    continue;
                }

                // Ignore products marked deleted
                if (AmazonUtil.isAmazonProductDeleted(delegator, amazonProductImage.getString("productId"))) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductImage_ProductDeleted", UtilMisc.toMap("productId", amazonProductImage.getString("productId")), locale);
                    Debug.logError(errorLog, MODULE);
                    continue;
                }

                String upc = null;
                if (AmazonConstants.requireUpcCodes || AmazonConstants.useUPCAsSKU) {

                    // Establish and validate the UPC
                    upc = getProductUPC(delegator, amazonProductImage.getString("productId"), locale);
                    if (UtilValidate.isEmpty(upc) && AmazonConstants.requireUpcCodes) {
                         errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_MissingCodeUPC", UtilMisc.toMap("productId", amazonProductImage.getString("productId")), locale));
                    } else if (UtilValidate.isNotEmpty(upc) && !UtilProduct.isValidUPC(upc)) {
                        errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_InvalidCodeUPC", UtilMisc.toMap("productId", amazonProductImage.getString("productId")), locale));
                    }
                }

                // Establish and validate the SKU
                String sku = getProductSKU(delegator, amazonProductImage, upc);

                if (UtilValidate.isEmpty(sku) && !AmazonConstants.useUPCAsSKU) {
                    errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NoRequiredParameter", UtilMisc.toMap("parameterName", "SKU", "productName", amazonProductImage.getString("productId")), locale));
                }

                ProductContentWrapper contentWrapper = UtilProduct.getProductContentWrapper(delegator, dispatcher, amazonProductImage.getString("productId"), locale);

                // Products must have a Main image
                if (UtilValidate.isEmpty(AmazonConstants.imageTypes.get("Main")) || UtilValidate.isEmpty(contentWrapper.get(AmazonConstants.imageTypes.get("Main")))) {
                    errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_MissingProductImage", UtilMisc.toMap("productId", amazonProductImage.getString("productId"), "productContentTypeId", AmazonConstants.imageTypes.get("Main")), locale));
                }

                LinkedHashMap<String, URI> imageTypeUrls = new LinkedHashMap<String, URI>();

                for (String imageType : AmazonConstants.imageTypes.keySet()) {
                    String fieldName = AmazonConstants.imageTypes.get(imageType);

                    StringUtil.StringWrapper imageUrlWrapper = contentWrapper.get(fieldName);
                    if (imageUrlWrapper == null) {
                        Debug.logInfo("No image url found for product [" + prodId + "] and field [" + fieldName + "]", MODULE);
                        continue;
                    }
                    String imageUrlString = imageUrlWrapper.toString();
                    if (UtilValidate.isEmpty(imageUrlString)) {
                        imageTypeUrls.put(imageType, null);
                        continue;
                    }
                    try {
                        int slashPos = imageUrlString.lastIndexOf("/");
                        if (slashPos != -1 && slashPos + 1 != imageUrlString.length()) {
                            String filePath = imageUrlString.substring(0, slashPos  + 1);
                            String fileName = imageUrlString.substring(slashPos + 1, imageUrlString.length());
                            imageUrlString = filePath + URLEncoder.encode(fileName, "UTF-8");
                        }
                        imageTypeUrls.put(imageType, new URI(AmazonConstants.imageUrlPrefix + imageUrlString));
                    } catch (URISyntaxException e) {
                        errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_InvalidProductImageUri", UtilMisc.toMap("productId", amazonProductImage.getString("productId"), "fieldName", fieldName), locale));
                        break;
                    } catch (UnsupportedEncodingException e) {
                        errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_InvalidProductImageUri", UtilMisc.toMap("productId", amazonProductImage.getString("productId"), "fieldName", fieldName), locale));
                        break;
                    }
                }

                if (UtilValidate.isNotEmpty(errMessage)) {
                    invalidAmazonImages.put(amazonProductImage, errMessage);
                    continue;
                }

                for (String imageType : imageTypeUrls.keySet()) {
                    URI imageUri = imageTypeUrls.get(imageType);

                    String operationType = UtilValidate.isEmpty(imageUri) ? "Delete" : "Update";

                    Element message = imageFeed.createElement("Message");
                    root.appendChild(message);
                    UtilXml.addChildElementValue(message, "MessageID", "" + messageId, imageFeed);
                    UtilXml.addChildElementValue(message, "OperationType", operationType, imageFeed);
                    Element productImage = imageFeed.createElement("ProductImage");
                    message.appendChild(productImage);
                    UtilXml.addChildElementValue(productImage, "SKU", sku, imageFeed);
                    UtilXml.addChildElementValue(productImage, "ImageType", imageType, imageFeed);
                    if (UtilValidate.isNotEmpty(imageUri)) {
                        UtilXml.addChildElementValue(productImage, "ImageLocation", imageUri.toString(), imageFeed);
                    }

                    amazonProductImageAcks.add(delegator.makeValue("AmazonProductImageAck", UtilMisc.toMap("productId", amazonProductImage.getString("productId"), "productContentTypeId", AmazonConstants.imageTypes.get(imageType), "acknowledgeMessageId", "" + messageId, "ackStatusId", AmazonConstants.statusProductNotAcked, "acknowledgeTimestamp", UtilDateTime.nowTimestamp())));
                    messageId++;
                }

                validAmazonImages.add(amazonProductImage);
                processedProductImages++;
                if (processedProductImages % 500 == 0) {
                    Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_Processed_Records_Image", UtilMisc.toMap("count", processedProductImages), locale), MODULE);
                }
            }
            amazonImageIt.close();

            LinkedHashMap<GenericValue, String> emailErrorMessages = new LinkedHashMap<GenericValue, String>();

            if (UtilValidate.isEmpty(validAmazonImages)) {
                String infoMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostNoNewImages", locale);
                Debug.logInfo(infoMessage, MODULE);
            } else {

                boolean success = true;
                String postErrorMessage = null;
                long processingDocumentId = -1;
                try {
                    String xml = UtilXml.writeXmlDocument(imageFeed);
                    Debug.logVerbose(xml, MODULE);
                    Writer writer = new OutputStreamWriter(new FileOutputStream(AmazonConstants.xmlOutputLocation + "AmazonImageFeed_" + AmazonConstants.xmlOutputDateFormat.format(new Date()) + ".xml"), "UTF-8");
                    writer.write(xml);
                    writer.close();
                    processingDocumentId = AmazonConstants.soapClient.postProductImages(xml);
                    Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ProcessingDocumentId_Image", UtilMisc.toMap("processingDocumentId", processingDocumentId), locale), MODULE);
                } catch (RemoteException e) {
                    success = false;
                    postErrorMessage = e.getMessage();
                    List<String> productIds = EntityUtil.getFieldListFromEntityList(validAmazonImages, "productId", true);
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostImageError", UtilMisc.toMap("productIds", productIds, "errorMessage", postErrorMessage), locale);
                    Debug.logError(errorLog, MODULE);
                }

                // Store operational data of the post attempt
                for (GenericValue validAmazonImage : validAmazonImages) {
                    validAmazonImage.set("statusId", success ? AmazonConstants.statusProductPosted : AmazonConstants.statusProductError);
                    validAmazonImage.set("postTimestamp", UtilDateTime.nowTimestamp());
                    validAmazonImage.set("postErrorMessage", success ? null : postErrorMessage);
                    if (!success) {
                        validAmazonImage.set("postFailures", validAmazonImage.getLong("postFailures") + 1);
                    }
                    validAmazonImage.set("processingDocumentId", success ? processingDocumentId : null);
                    validAmazonImage.store();
                    if (AmazonConstants.sendErrorEmails && !success) {
                        emailErrorMessages.put(validAmazonImage, postErrorMessage);
                    }

                    // Remove the old AmazonProductImageAcks from the database and store the new ones
                    delegator.removeRelated("AmazonProductImageAck", validAmazonImage);
                    delegator.storeAll(EntityUtil.filterByCondition(amazonProductImageAcks, EntityCondition.makeCondition("productId", EntityOperator.EQUALS, validAmazonImage.getString("productId"))));
                }
            }

            for (GenericValue invalidAmazonImage : invalidAmazonImages.keySet()) {
                String errorMessage = invalidAmazonImages.get(invalidAmazonImage);
                invalidAmazonImage.set("statusId", AmazonConstants.statusProductError);
                invalidAmazonImage.set("postTimestamp", UtilDateTime.nowTimestamp());
                invalidAmazonImage.set("postErrorMessage", errorMessage);
                invalidAmazonImage.set("postFailures", invalidAmazonImage.getLong("postFailures") + 1);
                invalidAmazonImage.set("processingDocumentId", null);
                invalidAmazonImage.store();
                delegator.removeRelated("AmazonProductImageAck", invalidAmazonImage);
                if (AmazonConstants.sendErrorEmails) {
                    emailErrorMessages.put(invalidAmazonImage, errorMessage);
                }
            }

            if (AmazonConstants.sendErrorEmails && UtilValidate.isNotEmpty(emailErrorMessages)) {
                AmazonUtil.sendBulkErrorEmail(dispatcher, userLogin, emailErrorMessages, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_PostImage", AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriProducts);
            }

        } catch (GenericEntityException gee) {
            UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        } catch (IOException ioe) {
            UtilMessage.createAndLogServiceError(ioe, locale, MODULE);
        } catch (GenericServiceException gse) {
            UtilMessage.createAndLogServiceError(gse, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Posts inventory data relating to any new or changed AmazonProductInventory records to Amazon.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> publishProductInventoryToAmazon(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String prodId = (String) context.get("productId");
        boolean postActualInventory = AmazonConstants.postActualInventory;

        try {

            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("statusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusProductCreated, AmazonConstants.statusProductError, AmazonConstants.statusProductChanged)));
            if (UtilValidate.isNotEmpty(prodId)) {
                conditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, prodId));
            }

            TransactionUtil.begin();
            EntityListIterator amazonInventoryIt = delegator.findListIteratorByCondition("AmazonProductInventory", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, Arrays.asList("productId"));
            TransactionUtil.commit();

            GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", AmazonConstants.productStoreId));

            long messageId = 1;
            Map<GenericValue, String> invalidAmazonInventory = new HashMap<GenericValue, String>();
            List<GenericValue> validAmazonInventory = new ArrayList<GenericValue>();
            Document inventoryDoc = AmazonConstants.soapClient.createDocumentHeader(AmazonConstants.messageTypeInventory);
            GenericValue amazonProductInventory = null;
            while ((amazonProductInventory = amazonInventoryIt.next()) != null) {

                String errMessage = null;

                // Check that the failure threshold has not been reached previously
                if (AmazonConstants.productPostRetryThreshold <= amazonProductInventory.getLong("postFailures").intValue()) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostImageAttemptsOverThreshold", UtilMisc.<String, Object>toMap("productId", amazonProductInventory.getString("productId"), "threshold", AmazonConstants.productPostRetryThreshold), locale);
                    Debug.logInfo(errorLog, MODULE);
                    continue;
                }

                // Check if this product was exported and acknowledged earlier
                if (delegator.findCountByAnd("AmazonProduct", UtilMisc.toMap("productId", amazonProductInventory.getString("productId"), "statusId", AmazonConstants.statusProductPosted, "ackStatusId", AmazonConstants.statusProductAckRecv)) != 1) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostInventoryNonExistentProduct", UtilMisc.toMap("productId", amazonProductInventory.getString("productId")), locale);
                    Debug.logError(errorLog, MODULE);
                    continue;
                }

                // Ignore products marked deleted
                if (AmazonUtil.isAmazonProductDeleted(delegator, amazonProductInventory.getString("productId"))) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoringProductInventory_ProductDeleted", UtilMisc.toMap("productId", amazonProductInventory.getString("productId")), locale);
                    Debug.logError(errorLog, MODULE);
                    continue;
                }

                String upc = null;
                if (AmazonConstants.requireUpcCodes || AmazonConstants.useUPCAsSKU) {

                    // Establish and validate the UPC
                    upc = getProductUPC(delegator, amazonProductInventory.getString("productId"), locale);
                    if (UtilValidate.isEmpty(upc) && AmazonConstants.requireUpcCodes) {
                         errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_MissingCodeUPC", UtilMisc.toMap("productId", amazonProductInventory.getString("productId")), locale));
                    } else if (UtilValidate.isNotEmpty(upc) && !UtilProduct.isValidUPC(upc)) {
                        errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_InvalidCodeUPC", UtilMisc.toMap("productId", amazonProductInventory.getString("productId")), locale));
                    }
                }

                // Establish and validate the SKU
                String sku = getProductSKU(delegator, amazonProductInventory, upc);
                if (UtilValidate.isEmpty(sku) && !AmazonConstants.useUPCAsSKU) {
                    errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NoRequiredParameter", UtilMisc.toMap("parameterName", "SKU", "productName", amazonProductInventory.getString("productId")), locale));
                }

                if (UtilValidate.isNotEmpty(errMessage)) {
                    invalidAmazonInventory.put(amazonProductInventory, errMessage);
                    continue;
                }

                Element message = UtilXml.addChildElement(inventoryDoc.getDocumentElement(), "Message", inventoryDoc);
                UtilXml.addChildElementValue(message, "MessageID", "" + messageId, inventoryDoc);
                UtilXml.addChildElementValue(message, "OperationType", "Update", inventoryDoc);
                Element invElement = UtilXml.addChildElement(message, "Inventory", inventoryDoc);
                UtilXml.addChildElementValue(invElement, "SKU", sku, inventoryDoc);

                Double atp = new Double(0);
                TransactionUtil.begin();
                Map<String, Object> serviceResult = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", amazonProductInventory.getString("productId"), "facilityId", productStore.getString("inventoryFacilityId"), "userLogin", userLogin));
                TransactionUtil.commit();
                if (serviceResult.containsKey("availableToPromiseTotal")) {
                    atp = (Double) serviceResult.get("availableToPromiseTotal");
                }

                // Amazon doesn't like inventory values < 0
                if (atp.doubleValue() < 0) {
                    atp = new Double(0);
                }

                // Amazon doesn't like inventory values > 99,999,999
                if (atp.doubleValue() > 99999999) {
                    postActualInventory = false;
                }

                GenericValue productFacility = delegator.findByPrimaryKey("ProductFacility", UtilMisc.toMap("productId", amazonProductInventory.getString("productId"), "facilityId", productStore.getString("inventoryFacilityId")));

                // Post inventory as Available if ProductFacility.minimumStock > 0 and AmazonConstants.inventoryIsAvailableIfMinimumStock, even if there is no actual inventory
                boolean available = atp.intValue() > 0;
                if ((!available) && AmazonConstants.inventoryIsAvailableIfMinimumStock && UtilValidate.isNotEmpty(productFacility) && UtilValidate.isNotEmpty(productFacility.getDouble("minimumStock")) && productFacility.getDouble("minimumStock").doubleValue() > 0) {
                    postActualInventory = false;
                    available = true;
                }

                if (postActualInventory) {
                    UtilXml.addChildElementValue(invElement, "Quantity", "" + atp.intValue(), inventoryDoc);
                } else {
                    UtilXml.addChildElementValue(invElement, "Available", available ? "true" : "false", inventoryDoc);
                }

                if (AmazonConstants.postInventoryDaysToShip && UtilValidate.isNotEmpty(productFacility) && UtilValidate.isNotEmpty(productFacility.get("daysToShip"))) {
                    long daysToShip = productFacility.getLong("daysToShip").longValue();

                    // Amazon doesn't like FulfillmentLatency > 30
                    if (daysToShip > 30) {
                        daysToShip = 30;
                    }

                    // Amazon doesn't like FulfillmentLatency <= 0
                    if (daysToShip > 0) {
                        UtilXml.addChildElementValue(invElement, "FulfillmentLatency", "" + daysToShip, inventoryDoc);
                    }
                }

                amazonProductInventory.set("acknowledgeMessageId", "" + messageId);
                validAmazonInventory.add(amazonProductInventory);
                messageId++;
                if (messageId % 500 == 0) Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_Processed_Records_Inventory", UtilMisc.toMap("count", messageId), locale), MODULE);
            }
            amazonInventoryIt.close();

            LinkedHashMap<GenericValue, String> emailErrorMessages = new LinkedHashMap<GenericValue, String>();

            if (UtilValidate.isEmpty(validAmazonInventory)) {
                String infoMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostNoNewProductInventory", locale);
                Debug.logInfo(infoMessage, MODULE);
            } else {

                boolean success = true;
                String postErrorMessage = null;
                long processingDocumentId = -1;
                try {
                    String xml = UtilXml.writeXmlDocument(inventoryDoc);
                    Debug.logVerbose(xml, MODULE);
                    Writer writer = new OutputStreamWriter(new FileOutputStream(AmazonConstants.xmlOutputLocation + "AmazonProductInventoryFeed_" + AmazonConstants.xmlOutputDateFormat.format(new Date()) + ".xml"), "UTF-8");
                    writer.write(xml);
                    writer.close();
                    processingDocumentId = AmazonConstants.soapClient.postProductInventory(xml);
                    Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ProcessingDocumentId_Inventory", UtilMisc.toMap("processingDocumentId", processingDocumentId), locale), MODULE);
                } catch (RemoteException e) {
                    success = false;
                    postErrorMessage = e.getMessage();
                    List<String> productIds = EntityUtil.getFieldListFromEntityList(validAmazonInventory, "productId", true);
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostInventoryError", UtilMisc.toMap("productIds", productIds, "errorMessage", postErrorMessage), locale);
                    Debug.logError(errorLog, MODULE);
                }

                // Store operational data of the post attempt
                for (GenericValue validAmazonInv : validAmazonInventory) {
                    validAmazonInv.set("statusId", success ? AmazonConstants.statusProductPosted : AmazonConstants.statusProductError);
                    validAmazonInv.set("postTimestamp", UtilDateTime.nowTimestamp());
                    validAmazonInv.set("postErrorMessage", success ? null : postErrorMessage);
                    if (!success) {
                        validAmazonInv.set("postFailures", validAmazonInv.getLong("postFailures") + 1);
                    }
                    validAmazonInv.set("processingDocumentId", success ? processingDocumentId : null);
                    validAmazonInv.set("ackStatusId", AmazonConstants.statusProductNotAcked);
                    validAmazonInv.set("acknowledgeTimestamp", null);
                    validAmazonInv.set("acknowledgeErrorMessage", null);
                    validAmazonInv.store();
                    if (AmazonConstants.sendErrorEmails && !success) {
                        emailErrorMessages.put(validAmazonInv, postErrorMessage);
                    }
                }
            }

            for (GenericValue invalidAmazonInv : invalidAmazonInventory.keySet()) {
                String errorMessage = invalidAmazonInventory.get(invalidAmazonInv);
                invalidAmazonInv.set("statusId", AmazonConstants.statusProductError);
                invalidAmazonInv.set("postTimestamp", UtilDateTime.nowTimestamp());
                invalidAmazonInv.set("postErrorMessage", errorMessage);
                invalidAmazonInv.set("postFailures", invalidAmazonInv.getLong("postFailures") + 1);
                invalidAmazonInv.set("processingDocumentId", null);
                invalidAmazonInv.set("ackStatusId", AmazonConstants.statusProductNotAcked);
                invalidAmazonInv.set("acknowledgeTimestamp", null);
                invalidAmazonInv.set("acknowledgeErrorMessage", null);
                invalidAmazonInv.store();
                if (AmazonConstants.sendErrorEmails) {
                    emailErrorMessages.put(invalidAmazonInv, errorMessage);
                }
            }

            if (AmazonConstants.sendErrorEmails && UtilValidate.isNotEmpty(emailErrorMessages)) {
                AmazonUtil.sendBulkErrorEmail(dispatcher, userLogin, emailErrorMessages, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_PostInventory", AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriProducts);
            }

        } catch (GenericEntityException gee) {
            UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        } catch (IOException ioe) {
            UtilMessage.createAndLogServiceError(ioe, locale, MODULE);
        } catch (GenericServiceException gse) {
            UtilMessage.createAndLogServiceError(gse, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service deletes products which status is AMZN_PROD_DELETED from Amazon.com.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> deleteProductsFromAmazon(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            List<EntityCondition> conditions = FastList.newInstance();
            conditions.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusProductDeleted, AmazonConstants.statusProductDeleteError)));
            conditions.add(EntityCondition.makeCondition("ackStatusId", EntityOperator.EQUALS, AmazonConstants.statusProductNotAcked));

            TransactionUtil.begin();
            EntityListIterator amazonProductsIt = delegator.findListIteratorByCondition("AmazonProduct", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, Arrays.asList("productId"));
            TransactionUtil.commit();

            // Prepare Product Feed document only element SKU and OperationType = Delete
            Document productDeleteFeed = AmazonConstants.soapClient.createDocumentHeader(AmazonConstants.messageTypeProduct);
            Element root = productDeleteFeed.getDocumentElement();
            GenericValue amazonProduct = null;
            long messageId = 1;
            Map<GenericValue, String> invalidAmazonProducts = new HashMap<GenericValue, String>();
            List<GenericValue> validAmazonProducts = new ArrayList<GenericValue>();
            while ((amazonProduct = amazonProductsIt.next()) != null) {

                if (AmazonConstants.productPostRetryThreshold <= amazonProduct.getLong("postFailures").intValue()) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostProductAttemptsOverThreshold", UtilMisc.<String, Object>toMap("productId", amazonProduct.getString("productId"), "threshold", AmazonConstants.productPostRetryThreshold), locale);
                    Debug.logInfo(errorLog, MODULE);
                    continue;
                }

                String errMessage = null;

                String upc = null;
                if (AmazonConstants.requireUpcCodes || AmazonConstants.useUPCAsSKU) {

                    // Establish and validate the UPC
                    upc = getProductUPC(delegator, amazonProduct.getString("productId"), locale);
                    if (UtilValidate.isEmpty(upc) && AmazonConstants.requireUpcCodes) {
                         errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_MissingCodeUPC", UtilMisc.toMap("productId", amazonProduct.getString("productId")), locale));
                    } else if (UtilValidate.isNotEmpty(upc) && !UtilProduct.isValidUPC(upc)) {
                        errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_InvalidCodeUPC", UtilMisc.toMap("productId", amazonProduct.getString("productId")), locale));
                    }
                }

                // Establish and validate the SKU
                String sku = getProductSKU(delegator, amazonProduct, upc);

                // Add errors if SKU is missing.
                if (UtilValidate.isEmpty(sku) && !AmazonConstants.useUPCAsSKU) {
                    errMessage = AmazonUtil.compoundError(errMessage, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NoRequiredParameter", UtilMisc.toMap("parameterName", "SKU", "internalName", amazonProduct.getString("productId")), locale));
                }

                // Check for errors
                if (UtilValidate.isNotEmpty(errMessage)) {
                    invalidAmazonProducts.put(amazonProduct, errMessage);
                    continue;
                }

                /*
                 * Create and add elements and values to XML document
                 */
                Element message = productDeleteFeed.createElement("Message");
                root.appendChild(message);
                UtilXml.addChildElementValue(message, "MessageID", "" + messageId, productDeleteFeed);
                UtilXml.addChildElementValue(message, "OperationType", "Delete", productDeleteFeed);
                Element product = productDeleteFeed.createElement("Product");
                message.appendChild(product);
                UtilXml.addChildElementValue(product, "SKU", sku, productDeleteFeed);

                amazonProduct.set("acknowledgeMessageId", "" + messageId);
                validAmazonProducts.add(amazonProduct);
                messageId++;
                if (messageId % 500 == 0) {
                    Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_Processed_Records_Product", UtilMisc.toMap("count", messageId), locale), MODULE);
                }
            }
            amazonProductsIt.close();

            LinkedHashMap<GenericValue, String> emailErrorMessages = new LinkedHashMap<GenericValue, String>();

            if (UtilValidate.isEmpty(validAmazonProducts)) {
                String infoMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostNoDeletedProducts", locale);
                Debug.logInfo(infoMessage, MODULE);
            } else {

                /*
                 * Post product document and get transaction ID. Store ID with product for later use.
                 */
                boolean success = true;
                String postErrorMessage = null;
                long processingDocumentId = -1;
                try {
                    String xml = UtilXml.writeXmlDocument(productDeleteFeed);
                    Debug.logVerbose(xml, MODULE);
                    Writer writer = new OutputStreamWriter(new FileOutputStream(AmazonConstants.xmlOutputLocation + "AmazonProductDeleteFeed_" + AmazonConstants.xmlOutputDateFormat.format(new Date()) + ".xml"), "UTF-8");
                    writer.write(xml);
                    writer.close();
                    processingDocumentId = AmazonConstants.soapClient.postProducts(xml);
                    Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ProcessingDocumentId_Product", UtilMisc.toMap("processingDocumentId", processingDocumentId), locale), MODULE);
                } catch (RemoteException e) {
                    success = false;
                    postErrorMessage = e.getMessage();
                    List<String> productIds = EntityUtil.getFieldListFromEntityList(validAmazonProducts, "productId", true);
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PostProductError", UtilMisc.toMap("productIds", productIds, "errorMessage", postErrorMessage), locale);
                    Debug.logError(errorLog, MODULE);
                }

                // Store operational data of the post attempt
                for (GenericValue validAmazonProduct : validAmazonProducts) {
                    validAmazonProduct.set("statusId", success ? AmazonConstants.statusProductDeleted : AmazonConstants.statusProductDeleteError);
                    validAmazonProduct.set("postTimestamp", UtilDateTime.nowTimestamp());
                    validAmazonProduct.set("postErrorMessage", success ? null : postErrorMessage);
                    if (!success) {
                        validAmazonProduct.set("postFailures", validAmazonProduct.getLong("postFailures") + 1);
                    }
                    validAmazonProduct.set("processingDocumentId", success ? processingDocumentId : null);
                    validAmazonProduct.set("ackStatusId", AmazonConstants.statusProductNotAcked);
                    validAmazonProduct.set("acknowledgeTimestamp", null);
                    validAmazonProduct.set("acknowledgeErrorMessage", null);
                    validAmazonProduct.store();
                    if (AmazonConstants.sendErrorEmails && !success) {
                        emailErrorMessages.put(validAmazonProduct, postErrorMessage);
                    }
                }
            }

            for (GenericValue invalidAmazonProduct : invalidAmazonProducts.keySet()) {
                String errorMessage = invalidAmazonProducts.get(invalidAmazonProduct);
                invalidAmazonProduct.set("statusId", AmazonConstants.statusProductDeleteError);
                invalidAmazonProduct.set("postTimestamp", UtilDateTime.nowTimestamp());
                invalidAmazonProduct.set("postErrorMessage", errorMessage);
                invalidAmazonProduct.set("postFailures", invalidAmazonProduct.getLong("postFailures") + 1);
                invalidAmazonProduct.set("processingDocumentId", null);
                invalidAmazonProduct.set("ackStatusId", AmazonConstants.statusProductNotAcked);
                invalidAmazonProduct.set("acknowledgeTimestamp", null);
                invalidAmazonProduct.set("acknowledgeErrorMessage", null);
                invalidAmazonProduct.store();
                if (AmazonConstants.sendErrorEmails) {
                    emailErrorMessages.put(invalidAmazonProduct, errorMessage);
                }
            }

            if (AmazonConstants.sendErrorEmails && UtilValidate.isNotEmpty(emailErrorMessages)) {
                AmazonUtil.sendBulkErrorEmail(dispatcher, userLogin, emailErrorMessages, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_PostProduct", AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriProducts);
            }

        } catch (GenericEntityException gee) {
            UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        } catch (IOException ioe) {
            UtilMessage.createAndLogServiceError(ioe, locale, MODULE);
        } catch (GenericServiceException gse) {
            UtilMessage.createAndLogServiceError(gse, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Checks the time of the last successful feed processing document download against a configurable threshold and
     * sends a warning email if the last success was too long in the past (threshold is configurable in the
     * opentaps.amazon.error.email.productFeedProcessingAgeWarning.thresholdHours property.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> checkLastFeedProcessingDocumentDownloadSuccess(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        TimeZone timeZone = (TimeZone) context.get("timeZone");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (!AmazonConstants.sendErrorEmails) {
            String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NotCheckingLastProcDocSuccessNoEmail", locale);
            Debug.logInfo(errorLog, MODULE);
            return ServiceUtil.returnSuccess();
        }

        try {

            GenericValue amazonProductFeedProcessing = EntityUtil.getFirst(delegator.findAll("AmazonProductFeedProcessing", Arrays.asList("acknowledgeTimestamp DESC")));

            if (UtilValidate.isEmpty(amazonProductFeedProcessing)) {
                String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NoSuccesfulProcDocs", locale);
                Debug.logInfo(errorLog, MODULE);
                return ServiceUtil.returnSuccess();
            }

            Timestamp lastSuccess = amazonProductFeedProcessing.getTimestamp("acknowledgeTimestamp");

            Calendar cal = Calendar.getInstance(timeZone, locale);
            cal.add(Calendar.HOUR, 0 - Math.abs(AmazonConstants.lastProcDocCheckAge));

            if (lastSuccess.before(new Timestamp(cal.getTimeInMillis()))) {
                Map<String, String> emailMap = UtilMisc.toMap("thresholdHours", "" + AmazonConstants.lastProcDocCheckAge);
                AmazonUtil.sendErrorEmail(dispatcher, userLogin, emailMap, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_ProcDocAgeWarning", AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriProcDocAgeWarning);
            }

        } catch (GenericEntityException gee) {
            UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        } catch (GenericServiceException gse) {
            UtilMessage.createAndLogServiceError(gse, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Returns an error if the Amazon component is using GoodIdentifications.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> checkSKUChangeAllowed(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");

        if (!AmazonConstants.useProductIdAsSKU) {
            String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_SKUChangesNotAllowed", locale);
            Debug.logInfo(errorLog, MODULE);
            return ServiceUtil.returnError(errorLog);
        }

        return ServiceUtil.returnSuccess();
    }
}
