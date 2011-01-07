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
package org.opentaps.dataimport.netsuite;

import org.opentaps.dataimport.ImportDecoder;
import org.opentaps.common.product.UtilProduct;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;

import java.util.Map;
import java.util.List;
import java.sql.Timestamp;

import javolution.util.FastMap;
import javolution.util.FastList;

/** maps NetSuiteItem into the equivalent set of opentaps entities */
public class NetSuiteItemDecoder implements ImportDecoder {

    public static final String module = NetSuiteItemDecoder.class.getName();

    // maps to store the productFeatureId of the color and size features generated
    protected Map<String, String> colorFeatures;
    protected Map<String, String> sizeFeatures;

    // map to store created productCategoryIds, keyed to the productCatagory field
    protected Map<String, String> categories;
    protected String parentCategoryId;

    public NetSuiteItemDecoder(String parentCategoryId) {
        colorFeatures = new FastMap<String, String>();
        sizeFeatures = new FastMap<String, String>();
        categories = new FastMap<String, String>();
        this.parentCategoryId = parentCategoryId;
    }

    public List<GenericValue> decode(GenericValue entry, Timestamp importTimestamp, Delegator delegator, LocalDispatcher dispatcher, Object... args) throws Exception {
        List<GenericValue> toBeStored = FastList.newInstance();
        String productId = entry.getString("itemId");
        Debug.logInfo("Importing NetSuiteItem with itemId [" + productId +"]", module);

        // scrub the category name
        String categoryName = entry.getString("productCatagory");
        if (categoryName != null) categoryName = categoryName.trim();
        if (categoryName != null && categoryName.length() == 0) categoryName = null; // just to make sure empty strings are null

        // check if we need to create a category
        String categoryId = null;
        boolean hasCategory = categoryName != null;
        if (hasCategory) {
            categoryId = categories.get(categoryName);
            if (categoryId == null) {
                GenericValue category = EntityUtil.getFirst( delegator.findByAndCache("ProductCategory", UtilMisc.toMap("categoryName", categoryName)) );
                if (category == null) {
                    categoryId = delegator.getNextSeqId("ProductCategory");
                    category = delegator.makeValue("ProductCategory");
                    category.put("productCategoryId", categoryId);
                    category.put("productCategoryTypeId", "CATALOG_CATEGORY");
                    category.put("categoryName", categoryName);
                    category.put("primaryParentCategoryId", parentCategoryId);
                    toBeStored.add(category);

                    // also make it a rollup of parent category
                    if (parentCategoryId != null) {
                        GenericValue rollup = delegator.makeValue("ProductCategoryRollup");
                        rollup.put("productCategoryId", categoryId);
                        rollup.put("parentProductCategoryId", parentCategoryId);
                        rollup.put("fromDate", importTimestamp);
                        toBeStored.add(rollup);
                    }
                } else {
                    categoryId = category.getString("productCategoryId");
                }
                categories.put(categoryName, categoryId);
            }
        }

        // create a product based on the item values
        GenericValue product = delegator.makeValue("Product");
        product.put("productId", productId);
        product.put("internalName", entry.get("itemName"));
        product.put("productName", entry.get("fullName"));
        product.put("description", entry.get("salesdescription"));

        product.put("weight", entry.get("weight"));
        product.put("weightUomId", getWeightUomId(entry));

        // by default, a product is not a virtual or variant product
        // if there is a parentId, then the item is a variant of a virtual product.  This means that its isVariant = Y, and also
        // we must go find its parent product and set its isVirtual to Y
        boolean isVariant = entry.get("parentId") != null;
        product.put("isVariant", isVariant ? "Y" : null);

        String taxable = "Yes".equals(entry.get("istaxable")) ? "Y" : "No".equals(entry.get("istaxable")) ? "N" : null;
        product.put("taxable", taxable);

        if ("Yes".equals(entry.get("isinactive"))) {
            product.put("salesDiscontinuationDate", importTimestamp);
        }

        String productTypeId = null;
        if ("Inventory Item".equals(entry.get("typeName"))) {
            productTypeId = "FINISHED_GOOD";
        } else if ("Service".equals(entry.get("typeName"))) {
            productTypeId = "SERVICE";
        } else {
            throw new IllegalArgumentException("Expecting type_name to be either 'Inventory Item' or 'Service'.  Was '"+entry.get("typeName")+"' instead.");
        }
        product.put("productTypeId", productTypeId);
        if (hasCategory) product.put("primaryProductCategoryId", categoryId);
        toBeStored.add(product);

        // add virtual products to the product category
        if (hasCategory && entry.get("parentId") == null) {
            GenericValue pcm = delegator.makeValue("ProductCategoryMember");
            pcm.put("productId", productId);
            pcm.put("productCategoryId", categoryId);
            pcm.put("fromDate", importTimestamp);
            toBeStored.add(pcm);
        }

        // import UPCA/UPCE or if it isn't a valid UPC, then OTHER_ID
        String upcValue = entry.getString("upcitemCode");
        if (upcValue != null) {
            GenericValue upca = delegator.makeValue("GoodIdentification");
            upca.put("goodIdentificationTypeId", UtilProduct.isValidUPC(upcValue) ? (upcValue.length() == 12 ? "UPCA" : "UPCE") : "OTHER_ID");
            upca.put("productId", productId);
            upca.put("idValue", upcValue);
            toBeStored.add(upca);
        }

        // variant products require extra decoding for the features
        if (isVariant) toBeStored.addAll(createVariantProductInfo(entry, delegator, importTimestamp));

        return toBeStored;
    }

    // decode a variant where the features are delimited by -
    private List<GenericValue> createVariantProductInfo(GenericValue entry, Delegator delegator, Timestamp importTimestamp) throws GenericEntityException {
        List<GenericValue> toBeStored = new FastList<GenericValue>();

        // make sure the parent product has isVirtual=Y, we can also validate the existence of the parent here
        GenericValue parentProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", entry.get("parentId")));
        if (parentProduct == null) {
            throw new IllegalArgumentException("Cannot import item ["+entry.get("itemId")+"]. The parent item ["+entry.get("parentId")+"] must be imported first.");
        }
        if (! "Y".equals(parentProduct.get("isVirtual"))) {
            parentProduct.put("isVirtual", "Y");
            toBeStored.add(parentProduct);
        }

        // create a variant relationship
        GenericValue assoc = delegator.makeValue("ProductAssoc");
        assoc.put("productId", entry.get("parentId"));
        assoc.put("productIdTo", entry.getString("itemId"));
        assoc.put("productAssocTypeId", "PRODUCT_VARIANT");
        assoc.put("fromDate", importTimestamp);
        assoc.put("quantity", 1.0);
        toBeStored.add(assoc);

        // split out the features from the name
        String[] tokens = entry.getString("itemName").split("-");
        if (tokens.length < 3) return toBeStored;
        String colorFeature = tokens[1];
        String sizeFeature = tokens[2];

        // create the features if they don't exist yet
        String colorFeatureId = colorFeatures.get(colorFeature);
        if (colorFeatureId == null) {
            Map findMap = UtilMisc.toMap("productFeatureTypeId", "COLOR", "description", colorFeature);
            GenericValue feature = EntityUtil.getFirst( delegator.findByAndCache("ProductFeature", findMap) );
            if (feature == null) {
                colorFeatureId = delegator.getNextSeqId("ProductFeature");
                feature = delegator.makeValue("ProductFeature", findMap);
                feature.put("productFeatureId", colorFeatureId);
                // TODO: what about productFeatureCategoryId?
                toBeStored.add(feature);
            } else {
                colorFeatureId = feature.getString("productFeatureId");
            }
            colorFeatures.put(colorFeature, colorFeatureId);
        }
        String sizeFeatureId = sizeFeatures.get(sizeFeature);
        if (sizeFeatureId == null) {
            Map findMap = UtilMisc.toMap("productFeatureTypeId", "SIZE", "description", sizeFeature);
            GenericValue feature = EntityUtil.getFirst( delegator.findByAndCache("ProductFeature", findMap) );
            if (feature == null) {
                sizeFeatureId = delegator.getNextSeqId("ProductFeature");
                feature = delegator.makeValue("ProductFeature", findMap);
                feature.put("productFeatureId", sizeFeatureId);
                // TODO: what about productFeatureCategoryId?
                toBeStored.add(feature);
            } else {
                sizeFeatureId = feature.getString("productFeatureId");
            }
            sizeFeatures.put(sizeFeature, sizeFeatureId);
        }

        // create the product feature applications
        GenericValue colorAppl = delegator.makeValue("ProductFeatureAppl");
        colorAppl.put("productId", entry.getString("itemId"));
        colorAppl.put("productFeatureId", colorFeatureId);
        colorAppl.put("productFeatureApplTypeId", "STANDARD_FEATURE");
        colorAppl.put("fromDate", importTimestamp);
        colorAppl.put("sequenceNum", 1);
        toBeStored.add(colorAppl);

        GenericValue sizeAppl = delegator.makeValue("ProductFeatureAppl");
        sizeAppl.put("productId", entry.getString("itemId"));
        sizeAppl.put("productFeatureId", sizeFeatureId);
        sizeAppl.put("productFeatureApplTypeId", "STANDARD_FEATURE");
        sizeAppl.put("fromDate", importTimestamp);
        sizeAppl.put("sequenceNum", 1);
        toBeStored.add(sizeAppl);

        // get all the selecatble features of the parent for purposes of filling the sequenceNum
        Map findMap = UtilMisc.toMap("productId", entry.get("parentId"), "productFeatureApplTypeId", "SELECTABLE_FEATURE");
        List<GenericValue> parentAppls = delegator.findByAndCache("ProductFeatureAppl", findMap);

        // figure out if we also have to associate these features with the parent
        boolean newColorFeature = true;
        boolean newSizeFeature = true;
        for (GenericValue appl : parentAppls) {
            if (colorFeatureId.equals(appl.get("productFeatureId"))) {
                newColorFeature = false;
            }
            if (sizeFeatureId.equals(appl.get("productFeatureId"))) {
                newSizeFeature = false;
            }
        }

        // create the parent features as selectable if not yet associated
        if (newColorFeature) {
            GenericValue appl = delegator.makeValue("ProductFeatureAppl", findMap);
            appl.put("productFeatureId", colorFeatureId);
            appl.put("fromDate", importTimestamp);
            appl.put("sequenceNum", parentAppls.size() + 1);
            toBeStored.add(appl);
        }
        if (newSizeFeature) {
            GenericValue appl = delegator.makeValue("ProductFeatureAppl", findMap);
            appl.put("productFeatureId", sizeFeatureId);
            appl.put("fromDate", importTimestamp);
            appl.put("sequenceNum", parentAppls.size() + 1);
            toBeStored.add(appl);
        }

        return toBeStored;
    }

    public String getWeightUomId(GenericValue entry) {
        String index = entry.getString("weightUnitIndex");
        if (index == null) return null;
        if ("1".equals(index)) return "WT_lb";
        if ("2".equals(index)) return "WT_oz";
        if ("3".equals(index)) return "WT_kg";
        if ("4".equals(index)) return "WT_g";
        return null;
    }
}
