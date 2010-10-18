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

/*
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
 */

/* This file has been modified by Open Source Strategies, Inc. */

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.service.*;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.product.config.ProductConfigWorker;
import org.ofbiz.product.catalog.*;
import org.ofbiz.product.store.*;
import org.ofbiz.order.shoppingcart.*;


//either optProduct, optProductId or productId must be specified
product = request.getAttribute("optProduct");
optProductId = request.getAttribute("optProductId");
productId = product?.productId ?: optProductId ?: request.getAttribute("productId");

webSiteId = CatalogWorker.getWebSiteId(request);
catalogId = CatalogWorker.getCurrentCatalogId(request);
productStore = ProductStoreWorker.getProductStore(request);
facilityId = productStore.inventoryFacilityId;
productStoreId = productStore.productStoreId;
autoUserLogin = session.getAttribute("autoUserLogin");
userLogin = session.getAttribute("userLogin");
cart = ShoppingCartEvents.getCartObject(request);

context.remove("daysToShip");
context.remove("averageRating");
context.remove("numRatings");
context.remove("totalPrice");

// get the product entity
if (!product && productId) {
    product = delegator.findByPrimaryKeyCache("Product", [productId : productId]);
}
if (product) {
    resultOutput = dispatcher.runSync("getProductInventoryAvailable", [productId : product.productId, useCache : true]);
    totalAvailableToPromise = resultOutput.availableToPromiseTotal;
    if (totalAvailableToPromise && totalAvailableToPromise.doubleValue() > 0) {
        productFacility = delegator.findByPrimaryKeyCache("ProductFacility", [productId : product.productId, facilityId : facilityId]);
        if (productFacility?.daysToShip != null) {
            context.daysToShip = productFacility.daysToShip;
        }
    } else {
       supplierProducts = delegator.findByAndCache("SupplierProduct", [productId : product.productId], ["-availableFromDate"]);
       supplierProduct = EntityUtil.getFirst(supplierProducts);
       if (supplierProduct?.standardLeadTimeDays != null) {
           standardLeadTimeDays = supplierProduct.standardLeadTimeDays;
           daysToShip = standardLeadTimeDays + 1;
           context.daysToShip = daysToShip;
       }
    }
    // make the productContentWrapper
    productContentWrapper = new ProductContentWrapper(product, request);
    context.productContentWrapper = productContentWrapper;
}

categoryId = null;
reviews = null;
if (product) {
    categoryId = parameters.category_id ?: request.getAttribute("productCategoryId");

    // get the product price
    if (cart.isSalesOrder()) {
        // sales order: run the "calculateProductPrice" service
        priceContext = [product : product, currencyUomId : cart.getCurrency(),
                autoUserLogin : autoUserLogin, userLogin : userLogin];
        priceContext.webSiteId = webSiteId;
        priceContext.prodCatalogId = catalogId;
        priceContext.productStoreId = productStoreId;
        priceContext.agreementId = cart.getAgreementId();
        priceContext.partyId = cart.getPartyId();  // IMPORTANT: otherwise it'll be calculating prices using the logged in user which could be a CSR instead of the customer
        priceContext.checkIncludeVat = "Y";
        priceMap = dispatcher.runSync("calculateProductPrice", priceContext);

        context.price = priceMap;
    } else {
        // purchase order: run the "calculatePurchasePrice" service
        priceContext = [product : product, currencyUomId : cart.getCurrency(),
                partyId : cart.getPartyId(), userLogin : userLogin];
        priceMap = dispatcher.runSync("calculatePurchasePrice", priceContext);

        context.price = priceMap;
    }

    // get aggregated product totalPrice
    if ("AGGREGATED".equals(product.productTypeId)) {
        configWrapper = ProductConfigWorker.getProductConfigWrapper(productId, cart.getCurrency(), request);
        if (configWrapper) {
            configWrapper.setDefaultConfig();
            context.totalPrice = configWrapper.getTotalPrice();
        }
    }

    // get the product review(s)
    reviews = product.getRelatedCache("ProductReview", null, ["-postedDateTime"]);
}

// get the average rating
if (reviews) {
    totalProductRating = 0;
    numRatings = 0;
    reviews.each { productReview ->
        productRating = productReview.productRating;
        if (productRating) {
            totalProductRating += productRating;
            numRatings++;
        }
    }
    if (numRatings) {
        context.averageRating = totalProductRating/numRatings;
        context.numRatings = numRatings;
    }
}

// an example of getting features of a certain type to show
sizeProductFeatureAndAppls = delegator.findByAnd("ProductFeatureAndAppl", [productId : productId, productFeatureTypeId : "SIZE"], ["sequenceNum", "defaultSequenceNum"]);

context.product = product;
context.productStoreId = productStoreId;
context.categoryId = categoryId;
context.productReviews = reviews;
context.sizeProductFeatureAndAppls = sizeProductFeatureAndAppls;
