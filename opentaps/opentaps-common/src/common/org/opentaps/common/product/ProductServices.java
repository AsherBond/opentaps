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

package org.opentaps.common.product;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;

import org.ofbiz.base.location.ComponentLocationResolver;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.category.CategoryContentWrapper;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilConfig;
import org.opentaps.common.util.UtilMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import freemarker.ext.dom.NodeModel;
import freemarker.template.TemplateException;

/**
 * Common product services.
 */
public final class ProductServices {

    private ProductServices() { }

    private static final String MODULE = ProductServices.class.getName();
    public static final String errorResource = "OpentapsErrorLabels";

    public static Map<String, Object> getProductByComprehensiveSearch(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String productId = (String) context.get("productId");
        Boolean lookupSupplierProductsBoolean = (Boolean) context.get("lookupSupplierProducts");
        boolean lookupSupplierProducts = (lookupSupplierProductsBoolean == null ? false : lookupSupplierProductsBoolean.booleanValue());
        try {
            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));

            // if no product, check for an exact GoodIdentification match
            if (product == null) {
                GenericValue goodIdentification = null;
                List<GenericValue> goodIdentifications = delegator.findByAnd("GoodIdentification", UtilMisc.toMap("idValue", productId));
                goodIdentification = EntityUtil.getFirst(goodIdentifications);
                if (goodIdentifications.size() > 1) {
                    // check if all goodIdentifications are for the same product
                    String firstProductId = goodIdentification.getString("productId");
                    for (int i = 1; i < goodIdentifications.size(); i++) {
                        if (!firstProductId.equals(goodIdentifications.get(i).getString("productId"))) {
                            // if more than one match and not the same product, then this is an error we should report (use failure, since we don't want to cause rollback)
                            Map<String, String> map = UtilMisc.toMap("idValue", productId);
                            String msg = UtilProperties.getMessage(errorResource, "OpentapsError_GoodIdentificationDupe", map, locale);
                            return ServiceUtil.returnFailure(msg);
                        }
                    }
                }
                if (goodIdentification != null) {
                    product = goodIdentification.getRelatedOne("Product");
                    productId = product.getString("productId");
                }
            }

            // still no product, check supplier products
            if (product == null && lookupSupplierProducts) {
                EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("supplierProductId", productId),
                        EntityUtil.getFilterByDateExpr("availableFromDate", "availableThruDate")
                );
                List<GenericValue> supplierProducts = delegator.findByAnd("SupplierProduct", conditions);
                if (supplierProducts.size() == 1) {
                    // TODO: do we also flag an error if more than one result?
                    product = EntityUtil.getFirst(supplierProducts).getRelatedOne("Product");
                    productId = product.getString("productId");
                }
            }

            // return the productId and product we found
            if (product == null) {
                productId = null;
            }
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("product", product);
            results.put("productId", productId);
            return results;

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    public static Map<String, Object> removeProduct(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasEntityPermission("CATALOG", "_ADMIN", userLogin)) {
            return ServiceUtil.returnError("You do not have permission to remove a product.  CATALOG_ADMIN permission is required.");
        }

        String productId = (String) context.get("productId");
        Map<String, Object> removeParams = UtilMisc.<String, Object>toMap("productId", productId);
        try {
            // WARNING: DO NOT ADD MORE ENTITIES TO THIS LIST WITHOUT THINKING AND ASKING ABOUT ITS IMPLICATIONS
            delegator.removeByAnd("InventoryEventPlanned", removeParams);
            delegator.removeByAnd("ProductContent", removeParams);
            delegator.removeByAnd("SupplierProduct", removeParams);
            delegator.removeByAnd("ProductPrice", removeParams);
            delegator.removeByAnd("GoodIdentification", removeParams);
            delegator.removeByAnd("ProductFeatureAppl", removeParams);
            delegator.removeByAnd("ProductAttribute", removeParams);
            delegator.removeByAnd("ProductCategoryMember", removeParams);
            delegator.removeByAnd("ProductKeyword", removeParams);
            delegator.removeByAnd("ProductReview", removeParams);
            delegator.removeByAnd("ProductAttribute", removeParams);
            delegator.removeByAnd("CartAbandonedLine", removeParams);
            delegator.removeByAnd("ProductGlAccount", removeParams);
            delegator.removeByAnd("ProductGeo", removeParams);
            delegator.removeByAnd("ProductFacility", removeParams);
            delegator.removeByAnd("ProductFacilityLocation", removeParams);
            delegator.removeByAnd("ProductAssoc", removeParams);
            delegator.removeByAnd("ProductAssoc", UtilMisc.toMap("productIdTo", productId));
            delegator.removeByAnd("ProductCalculatedInfo", removeParams);
            delegator.removeByAnd("Product", removeParams);

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }


    public static Map<String, Object> removeProductCategory(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasEntityPermission("CATALOG", "_ADMIN", userLogin)) {
            return ServiceUtil.returnError("You do not have permission to remove a product category.  CATALOG_ADMIN permission is required.");
        }

        String productCategoryId = (String) context.get("productCategoryId");
        Map<String, Object> removeParams = UtilMisc.<String, Object>toMap("productCategoryId", productCategoryId);
        try {
            // WARNING: DO NOT ADD MORE ENTITIES TO THIS LIST WITHOUT THINKING AND ASKING ABOUT ITS IMPLICATIONS
            delegator.removeByAnd("ProductCategoryContent", removeParams);
            delegator.removeByAnd("ProductCategoryLink", removeParams);
            delegator.removeByAnd("ProductCategoryMember", removeParams);
            delegator.removeByAnd("ProductCategoryRole", removeParams);
            delegator.removeByAnd("ProductCategoryRollup", removeParams);
            delegator.removeByAnd("ProductCategoryRollup", UtilMisc.toMap("parentProductCategoryId", productCategoryId));
            delegator.removeByAnd("ProductCategory", removeParams);

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Return error message in case when user tries assign to product not unique identifier.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> checkGoodIdentifierUniqueness(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String goodIdentificationTypeId = (String) context.get("goodIdentificationTypeId");
        String productId = (String) context.get("productId");
        String idValue = (String) context.get("idValue");

        if (Arrays.asList("UPCA", "UPCE").contains(goodIdentificationTypeId) && !UtilProduct.isValidUPC(idValue)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_ProductUpcCodeNotValid", UtilMisc.toMap("idValue", idValue), locale, MODULE);
        }

        try {
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("goodIdentificationTypeId", goodIdentificationTypeId),
                    EntityCondition.makeCondition("idValue", idValue),
                    EntityCondition.makeCondition("productId", EntityOperator.NOT_EQUAL, productId)
            );
            List<GenericValue> products = delegator.findByCondition("GoodIdentification", conditions, null, null);
            if (UtilValidate.isNotEmpty(products)) {
                GenericValue product = EntityUtil.getFirst(products);
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductUpcCodeNotUnique", UtilMisc.toMap("goodIdentificationTypeId", goodIdentificationTypeId, "idValue", idValue, "productId", product.getString("productId")), locale, MODULE);
            }

        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service goes over product catalog and generate site map in HTML format
     * for given product store. HTML output bases on FreeMarker template.
     *
     * @param DispatchContext dctx
     * @param Map context
     * @return Map
     */
    public static Map<String, Object> generateSiteMapFile(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);

        String productStoreId = (String) context.get("productStoreId");;
        Locale outputLocale = UtilMisc.ensureLocale(context.get("outputLocale"));
        Boolean excludeProducts = (Boolean) context.get("excludeProducts");
        if (excludeProducts == null) {
            excludeProducts = Boolean.FALSE;
        }
        String templateLocation = (String) context.get("templateLocation");
        if (UtilValidate.isEmpty(templateLocation)) {
            templateLocation = UtilConfig.getPropertyValue("opentaps", "opentaps.sitemap.default.template");
        }

        String fileOutputLocation = (String) context.get("fileOutputLocation");
        if (UtilValidate.isEmpty(fileOutputLocation)) {
            String defaultDir = UtilConfig.getPropertyValue("opentaps", "opentaps.sitemap.default.output.dir");
            fileOutputLocation = String.format("%1$s%2$s%3$s%4$shtml",
                    System.getProperty("ofbiz.home"),
                    defaultDir.startsWith("/") ? defaultDir : ("/" + defaultDir),
                            templateLocation.substring(templateLocation.lastIndexOf('/'), templateLocation.lastIndexOf('.')),
                            outputLocale.equals(Locale.getDefault()) ? "." : ("_" + outputLocale.toString() + ".")
            );
        }

        try {

            GenericValue store = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));

            List<GenericValue> storeCatalogs = CatalogWorker.getStoreCatalogs(delegator, productStoreId);
            if (UtilValidate.isEmpty(storeCatalogs)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductStoreHaveNoCatalogs", UtilMisc.toMap("productStoreId", productStoreId), locale, MODULE);
            }

            Document catalogXMLMap = UtilXml.makeEmptyXmlDocument("ProductCatalog");
            Element rootElement = catalogXMLMap.getDocumentElement();
            Element storeElement = catalogXMLMap.createElement("store");
            storeElement.setAttribute("id", productStoreId);
            if (store != null) {
                storeElement.setAttribute("name", store.getString("storeName"));
            }
            rootElement.appendChild(storeElement);

            for (GenericValue currentCatalog : storeCatalogs) {

                Element catalogElement = catalogXMLMap.createElement("catalog");
                catalogElement.setAttribute("id", currentCatalog.getString("prodCatalogId"));
                catalogElement.setAttribute("name", currentCatalog.getRelatedOne("ProdCatalog").getString("catalogName"));
                storeElement.appendChild(catalogElement);

                List<GenericValue> prodCatalogCategories = CatalogWorker.getProdCatalogCategories(delegator, currentCatalog.getString("prodCatalogId"), "PCCT_BROWSE_ROOT");
                String rootCategoryId = null;
                if (UtilValidate.isNotEmpty(prodCatalogCategories)) {
                    rootCategoryId = EntityUtil.getFirst(prodCatalogCategories).getString("productCategoryId");
                }

                // recursive call writeChildCategories to go over product catalog. A result XML document
                // holding catalog data should be created.
                writeChildCategories(delegator, dispatcher, outputLocale, rootCategoryId, catalogXMLMap, catalogElement, excludeProducts.booleanValue());

            }

            // render template
            Map<String, Object> templateContext = new HashMap<String, Object>();

            templateContext.put("uiLabelMap", UtilMessage.getUiLabels(outputLocale));
            templateContext.put("locale", outputLocale);
            String targetHost = UtilConfig.getPropertyValue("opentaps", "opentaps.sitemap.host");
            templateContext.put("catalogHost", targetHost);
            templateContext.put("productLink", String.format(UtilConfig.getPropertyValue("opentaps.properties", "opentaps.sitemap.link.product"), targetHost));
            templateContext.put("categoryLink", String.format(UtilConfig.getPropertyValue("opentaps.properties", "opentaps.sitemap.link.category"), targetHost));

            // wrap XML document and make it accessible for FreeMarker
            NodeModel map = NodeModel.wrap(catalogXMLMap);
            templateContext.put("catalogMap", map);

            String outputFile = UtilValidate.isUrl(fileOutputLocation) ? ComponentLocationResolver.getBaseLocation(fileOutputLocation).toString() : fileOutputLocation;
            Writer writer = new FileWriter(outputFile, false);
            FreeMarkerWorker.renderTemplateAtLocation(templateLocation, templateContext, writer);

        } catch (MalformedURLException mue) {
            return UtilMessage.createAndLogServiceError(mue, locale, MODULE);
        } catch (TemplateException te) {
            return UtilMessage.createAndLogServiceError(te, locale, MODULE);
        } catch (IOException ioe) {
            return UtilMessage.createAndLogServiceError(ioe, locale, MODULE);
        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Utility method obtains list of categories which are child of parentCategoryId and fill out
     * category elements for XML catalog. For each category call writeCategoryProducts to get products
     * of the category.
     * @param excludeProducts TODO
     * @param Delegator delegator
     * @param LocalDispatcher dispatcher
     * @param Locale locale
     * @param String parentCategoryId
     * @param Document catalogXMLMap
     * @param Element parentElement
     *
     * @throws GenericEntityException
     */
    protected static void writeChildCategories(Delegator delegator, LocalDispatcher dispatcher, Locale locale, String parentCategoryId, Document catalogXMLMap, Element parentElement, boolean excludeProducts) throws GenericEntityException {

        // collect IDs for categories which are child of parentCategoryId
        Set<String> childCategoryIds = new LinkedHashSet<String>();

        List<GenericValue> productCategoryRollups = delegator.findByCondition("ProductCategoryRollup", EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("parentProductCategoryId", EntityOperator.EQUALS, parentCategoryId),
                EntityUtil.getFilterByDateExpr()),
                Arrays.asList("productCategoryId"), Arrays.asList("sequenceNum"));
        childCategoryIds.addAll(EntityUtil.<String>getFieldListFromEntityList(productCategoryRollups, "productCategoryId", true));

        List<GenericValue> productCategories = delegator.findByAnd("ProductCategory", UtilMisc.toMap("productCategoryTypeId", "CATALOG_CATEGORY", "primaryParentCategoryId", parentCategoryId));
        childCategoryIds.addAll(EntityUtil.<String>getFieldListFromEntityList(productCategories, "productCategoryId", true));
        if (UtilValidate.isEmpty(childCategoryIds)) {
            return;
        }

        // get all categories
        List<GenericValue> neighbourCategories = delegator.findByCondition("ProductCategory", EntityCondition.makeCondition("productCategoryId", EntityOperator.IN, Arrays.asList(childCategoryIds.toArray())), null, Arrays.asList("categoryName"));
        if (UtilValidate.isEmpty(neighbourCategories)) {
            return;
        }

        for (GenericValue currentCategory : neighbourCategories) {

            // add category element and attributes: id, name
            Element categoryElement = catalogXMLMap.createElement("category");
            parentElement.appendChild(categoryElement);
            categoryElement.setAttribute("id", currentCategory.getString("productCategoryId"));
            String categoryName = CategoryContentWrapper.getProductCategoryContentAsText(currentCategory, "CATEGORY_NAME", null, null);
            if (UtilValidate.isEmpty(categoryName)) {
                categoryName = CategoryContentWrapper.getProductCategoryContentAsText(currentCategory, "DESCRIPTION", null, null);
            }
            categoryElement.setAttribute("name", categoryName);

            List<GenericValue> categoryRollups = currentCategory.getRelatedByAnd("CurrentProductCategoryRollup", UtilMisc.toMap("parentProductCategoryId", parentCategoryId));
            EntityUtil.filterByDate(categoryRollups);
            if (categoryRollups.size() > 0) {
                String sequenceNumber = EntityUtil.getFirst(categoryRollups).getString("sequenceNum");
                if (UtilValidate.isNotEmpty(sequenceNumber)) {
                    categoryElement.setAttribute("sequence", sequenceNumber);
                }
            }

            // add product elements for current category if any
            int numberOfProductsInCategory = writeCategoryProducts(delegator, dispatcher, locale, currentCategory.getString("productCategoryId"), catalogXMLMap, categoryElement, excludeProducts);
            categoryElement.setAttribute("numberOfProducts", String.valueOf(numberOfProductsInCategory));

            // recursively call itself to get categories which are child to current
            writeChildCategories(delegator, dispatcher, locale, currentCategory.getString("productCategoryId"), catalogXMLMap, categoryElement, excludeProducts);
        }

    }

    /**
     * Obtains products from productCategoryId and fill out product elements in XML catalog.
     * @param excludeProducts TODO
     * @param Delegator delegator
     * @param LocalDispatcher dispatcher
     * @param Locale locale
     * @param String productCategoryId
     * @param Document catalogXMLMap
     * @param Element parentElement
     *
     * @return TODO
     * @throws GenericEntityException
     */
    protected static int writeCategoryProducts(Delegator delegator, LocalDispatcher dispatcher, Locale locale, String productCategoryId, Document catalogXMLMap, Element parentElement, boolean excludeProducts) throws GenericEntityException {

        int productsCount = 0;

        // collect IDs of category products
        Set<String> productIds = new LinkedHashSet<String>();
        List<GenericValue> productCategoryMembers = delegator.findByCondition("ProductCategoryMember", EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("productCategoryId", productCategoryId),
                EntityUtil.getFilterByDateExpr()),
                Arrays.asList("productId"), null);
        productIds.addAll(EntityUtil.<String>getFieldListFromEntityList(productCategoryMembers, "productId", true));
        List<GenericValue> products = delegator.findByAnd("Product", UtilMisc.toMap("primaryProductCategoryId", productCategoryId));
        productIds.addAll(EntityUtil.<String>getFieldListFromEntityList(products, "productId", true));
        if (UtilValidate.isEmpty(productIds)) {
            return productsCount;
        }

        // get list of products
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("productId", EntityOperator.IN, new ArrayList<String>(productIds)),
                EntityCondition.makeCondition("isVirtual", "N"),
                EntityUtil.getFilterByDateExpr(UtilDateTime.nowTimestamp(), "introductionDate", "salesDiscontinuationDate"));
        if (excludeProducts) {
            return (int) delegator.findCountByCondition("Product", conditions, null);
        }
        List<GenericValue> categoryProducts = delegator.findByCondition("Product", conditions, null, Arrays.asList("productName"));
        if (UtilValidate.isEmpty(categoryProducts)) {
            return productsCount;
        }

        productsCount = categoryProducts.size();

        for (GenericValue product : categoryProducts) {

            // add product element and attributes: id, name
            Element productElement = catalogXMLMap.createElement("product");
            parentElement.appendChild(productElement);
            productElement.setAttribute("id", product.getString("productId"));
            ProductContentWrapper wrapper = new ProductContentWrapper(dispatcher, product, locale, null);
            productElement.setAttribute("name", wrapper.get("PRODUCT_NAME").toString());

            List<GenericValue> categoryMember = product.getRelatedByAnd("ProductCategoryMember", UtilMisc.toMap("productCategoryId", productCategoryId));
            if (UtilValidate.isNotEmpty(categoryMember)) {
                EntityUtil.filterByDate(categoryMember);
                if (categoryMember.size() > 0) {
                    String sequenceNumber = EntityUtil.getFirst(categoryMember).getString("sequenceNum");
                    if (UtilValidate.isNotEmpty(sequenceNumber)) {
                        productElement.setAttribute("sequence", sequenceNumber);
                    }
                }
            }
        }

        return productsCount;
    }

    /**
     * Service calculate the productPrice using the calculateProductPrice ofbiz service
     * and adding the MIN_ADV_PRICE
     *
     * @param DispatchContext dctx
     * @param Map context
     * @return Map
     */
    public static Map<String, Object> calculateProductPrice(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = null;
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        try {
            result = dispatcher.runSync("calculateProductPrice", context, 0, false);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problems calculating product price with ofbiz method", MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        GenericValue product = (GenericValue) context.get("product");
        String productId = product.getString("productId");

        String productStoreId = (String) context.get("productStoreId");
        String productStoreGroupId = (String) context.get("productStoreGroupId");
        GenericValue productStore = null;
        try {
            // we have a productStoreId, if the corresponding ProductStore.primaryStoreGroupId is not empty, use that
            productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
        } catch (GenericEntityException e) {
            String errMsg = "Error getting product store info from the database while calculating price" + e.toString();
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }
        if (UtilValidate.isEmpty(productStoreGroupId)) {
            if (productStore != null) {
                try {
                    if (UtilValidate.isNotEmpty(productStore.getString("primaryStoreGroupId"))) {
                        productStoreGroupId = productStore.getString("primaryStoreGroupId");
                    } else {
                        // no ProductStore.primaryStoreGroupId, try ProductStoreGroupMember
                        List<GenericValue> productStoreGroupMemberList = delegator.findByAndCache("ProductStoreGroupMember", UtilMisc.toMap("productStoreId", productStoreId), UtilMisc.toList("sequenceNum", "-fromDate"));
                        productStoreGroupMemberList = EntityUtil.filterByDate(productStoreGroupMemberList, true);
                        if (productStoreGroupMemberList.size() > 0) {
                            GenericValue productStoreGroupMember = EntityUtil.getFirst(productStoreGroupMemberList);
                            productStoreGroupId = productStoreGroupMember.getString("productStoreGroupId");
                        }
                    }
                } catch (GenericEntityException e) {
                    String errMsg = "Error getting product store info from the database while calculating price" + e.toString();
                    Debug.logError(e, errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
            }

            // still empty, default to _NA_
            if (UtilValidate.isEmpty(productStoreGroupId)) {
                productStoreGroupId = "_NA_";
            }
        }

        // if currencyUomId is null get from properties file, if nothing there assume USD (USD: American Dollar) for now
        String currencyUomId = (String) context.get("currencyUomId");
        if (UtilValidate.isEmpty(currencyUomId)) {
            currencyUomId = UtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD");
        }

        // productPricePurposeId is null assume "PURCHASE", which is equivalent to what prices were before the purpose concept
        String productPricePurposeId = (String) context.get("productPricePurposeId");
        if (UtilValidate.isEmpty(productPricePurposeId)) {
            productPricePurposeId = "PURCHASE";
        }

        // termUomId, for things like recurring prices specifies the term (time/frequency measure for example) of the recurrence
        // if this is empty it will simply not be used to constrain the selection
        String termUomId = (String) context.get("termUomId");

        // if this product is variant, find the virtual product and apply checks to it as well
        String virtualProductId = null;
        if ("Y".equals(product.getString("isVariant"))) {
            try {
                virtualProductId = ProductWorker.getVariantVirtualId(product);
            } catch (GenericEntityException e) {
                String errMsg = "Error getting virtual product id from the database while calculating price" + e.toString();
                Debug.logError(e, errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }
        }

        // get prices for virtual product if one is found; get all ProductPrice entities for this productId and currencyUomId
        List<GenericValue> virtualProductPrices = null;
        if (virtualProductId != null) {
            try {
                virtualProductPrices = delegator.findByAndCache("ProductPrice", UtilMisc.toMap("productId", virtualProductId, "currencyUomId", currencyUomId, "productStoreGroupId", productStoreGroupId), UtilMisc.toList("-fromDate"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "An error occurred while getting the product prices", MODULE);
            }
            virtualProductPrices = EntityUtil.filterByDate(virtualProductPrices, true);
        }

        List<EntityCondition> productPriceEcList = FastList.newInstance();
        productPriceEcList.add(EntityCondition.makeCondition("productId", productId));
        // this funny statement is for backward compatibility purposes; the productPricePurposeId is a new pk field on the ProductPrice entity and in order databases may not be populated, until the pk is updated and such; this will ease the transition somewhat
        if ("PURCHASE".equals(productPricePurposeId)) {
            productPriceEcList.add(EntityCondition.makeCondition(
                    EntityCondition.makeCondition("productPricePurposeId", EntityOperator.EQUALS, productPricePurposeId),
                    EntityOperator.OR,
                    EntityCondition.makeCondition("productPricePurposeId", EntityOperator.EQUALS, null)));
        } else {
            productPriceEcList.add(EntityCondition.makeCondition("productPricePurposeId", EntityOperator.EQUALS, productPricePurposeId));
        }
        productPriceEcList.add(EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, currencyUomId));
        productPriceEcList.add(EntityCondition.makeCondition("productStoreGroupId", EntityOperator.EQUALS, productStoreGroupId));
        if (UtilValidate.isNotEmpty(termUomId)) {
            productPriceEcList.add(EntityCondition.makeCondition("termUomId", EntityOperator.EQUALS, termUomId));
        }
        productPriceEcList.add(EntityCondition.makeCondition("productPriceTypeId", EntityOperator.EQUALS, "MIN_ADV_PRICE"));
        EntityCondition productPriceEc = EntityCondition.makeCondition(productPriceEcList, EntityOperator.AND);

        // for prices, get all MIN_ADV_PRICE ProductPrice entities for this productId and currencyUomId
        List<GenericValue> minAdvPrices = null;
        try {
            minAdvPrices = delegator.findByConditionCache("ProductPrice", productPriceEc, null, UtilMisc.toList("-fromDate"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "An error occurred while getting the MIN_ADV_PRICE product prices", MODULE);
        }
        minAdvPrices = EntityUtil.filterByDate(minAdvPrices, true);
        if (minAdvPrices == null) {
            return result;
        }

        GenericValue minAdvPriceValue = EntityUtil.getFirst(minAdvPrices);
        if (minAdvPrices != null && minAdvPrices.size() > 1) {
            if (Debug.infoOn()) {
                Debug.logInfo("There is more than one MIN_ADV_PRICE with the currencyUomId " + currencyUomId + " and productId " + productId + ", using the latest found with price: " + minAdvPriceValue.getDouble("price"), MODULE);
            }
        }

        // if any of these prices is missing and this product is a variant, default to the corresponding price on the virtual product
        if (virtualProductPrices != null && virtualProductPrices.size() > 0) {
            if (minAdvPriceValue == null) {
                List<GenericValue> virtualTempPrices = EntityUtil.filterByAnd(virtualProductPrices, UtilMisc.toMap("productPriceTypeId", "MIN_ADV_PRICE"));
                minAdvPriceValue = EntityUtil.getFirst(virtualTempPrices);
                if (virtualTempPrices != null && virtualTempPrices.size() > 1) {
                    if (Debug.infoOn()) {
                        Debug.logInfo("There is more than one MIN_ADV_PRICE with the currencyUomId " + currencyUomId + " and productId " + virtualProductId + ", using the latest found with price: " + minAdvPriceValue.getDouble("price"), MODULE);
                    }
                }
            }
        }


        // now if this is a virtual product check each price type, if doesn't exist get from variant with lowest MIN_ADV_PRICE
        if ("Y".equals(product.getString("isVirtual"))) {
            // only do this if there is no default price, consider the others optional for performance reasons
            if (minAdvPriceValue == null) {
                // Debug.logInfo("Product isVirtual and there is no default price for ID " + productId + ", trying variant prices", MODULE);

                //use the cache to find the variant with the lowest min adv price
                try {
                    List<GenericValue> variantAssocList = EntityUtil.filterByDate(delegator.findByAndCache("ProductAssoc", UtilMisc.toMap("productId", product.get("productId"), "productAssocTypeId", "PRODUCT_VARIANT"), UtilMisc.toList("-fromDate")));
                    double minDefaultPrice = Double.MAX_VALUE;
                    List<GenericValue> variantProductPrices = null;
                    String variantProductId = null;
                    for (GenericValue variantAssoc : variantAssocList) {
                        String curVariantProductId = variantAssoc.getString("productIdTo");
                        List<GenericValue> curVariantPriceList = EntityUtil.filterByDate(delegator.findByAndCache("ProductPrice", UtilMisc.toMap("productId", curVariantProductId), UtilMisc.toList("-fromDate")), nowTimestamp);
                        List<GenericValue> tempDefaultPriceList = EntityUtil.filterByAnd(curVariantPriceList, UtilMisc.toMap("productPriceTypeId", "MIN_ADV_PRICE"));
                        GenericValue curDefaultPriceValue = EntityUtil.getFirst(tempDefaultPriceList);
                        if (curDefaultPriceValue != null) {
                            Double curDefaultPrice = curDefaultPriceValue.getDouble("price");
                            if (curDefaultPrice.doubleValue() < minDefaultPrice) {
                                // check to see if the product is discontinued for sale before considering it the lowest price
                                GenericValue curVariantProduct = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", curVariantProductId));
                                if (curVariantProduct != null) {
                                    Timestamp salesDiscontinuationDate = curVariantProduct.getTimestamp("salesDiscontinuationDate");
                                    if (salesDiscontinuationDate == null || salesDiscontinuationDate.after(nowTimestamp)) {
                                        minDefaultPrice = curDefaultPrice.doubleValue();
                                        variantProductPrices = curVariantPriceList;
                                        variantProductId = curVariantProductId;
                                        // Debug.logInfo("Found new lowest price " + minDefaultPrice + " for variant with ID " + variantProductId, MODULE);
                                    }
                                }
                            }
                        }
                    }

                    if (variantProductPrices != null) {
                        // we have some other options, give 'em a go...
                        if (minAdvPriceValue == null) {
                            List<GenericValue> virtualTempPrices = EntityUtil.filterByAnd(variantProductPrices, UtilMisc.toMap("productPriceTypeId", "MIN_ADV_PRICE"));
                            minAdvPriceValue = EntityUtil.getFirst(virtualTempPrices);
                            if (virtualTempPrices != null && virtualTempPrices.size() > 1) {
                                if (Debug.infoOn()) {
                                    Debug.logInfo("There is more than one MIN_ADV_PRICE with the currencyUomId " + currencyUomId + " and productId " + variantProductId + ", using the latest found with price: " + minAdvPriceValue.getDouble("price"), MODULE);
                                }
                            }
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "An error occurred while getting the product prices", MODULE);
                }
            }
        }

        double minAdvPrice = 0;
        if (minAdvPriceValue != null && minAdvPriceValue.get("price") != null) {
            minAdvPrice = minAdvPriceValue.getDouble("price").doubleValue();
            result.put("minAdvPrice", minAdvPrice);
        }

        return result;
    }
}

