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
package org.opentaps.domain.product;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.GoodIdentification;
import org.opentaps.base.entities.ProductCategory;
import org.opentaps.base.entities.ProductFeatureAndAppl;
import org.opentaps.base.entities.ProductType;
import org.opentaps.foundation.entity.EntityFieldInterface;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Product entity, encapsulate product specific functionality.
 */
public class Product extends org.opentaps.base.entities.Product {

    private List<ProductFeatureAndAppl> features;
    private List<ProductFeatureAndAppl> stdFeatures;
    private Product variantOf;
    private ProductCategory primaryCategory;

    /**
     * Default public constructor.
     */
    public Product() {
        super();
    }

    /**
     * Tests product if this product is physical.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isPhysical() throws RepositoryException {
        return !("N".equals(getType().getIsPhysical()));
    }

    /**
     * Tests if the product is virtual.  A virtual product
     * is the parent product of a set of variants.
     * @return <code>true</code> if the product is virtual
     */
    public Boolean isVirtual() {
        return "Y".equals(getIsVirtual());
    }

    /**
     * Tests if the product is a variant.
     * @return <code>true</code> if the product is a variant
     */
    public Boolean isVariant() {
        return "Y".equals(getIsVariant());
    }

    /**
     * Gets this product type.
     * @return the <code>ProductType</code>
     * @throws RepositoryException if an error occurs
     */
    public ProductType getType() throws RepositoryException {
        return this.getProductType();
    }

    /**
     * Returns this product <code>GoodIdentification</code> for the given type.
     * @param goodIdentificationTypeId the type of good identifation to find
     * @return the <code>GoodIdentification</code> found, or null
     * @throws RepositoryException if an error occurs
     */
    public GoodIdentification getGoodIdentificationByType(String goodIdentificationTypeId) throws RepositoryException {
        return getRepository().getGoodIdentificationByType(this.getProductId(), goodIdentificationTypeId);
    }

    /**
     * Gets the unit price for this product.
     * @return the unit price for the default currency
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getUnitPrice() throws RepositoryException {
        return getRepository().getUnitPrice(this);
    }

    /**
     * Gets the unit price for this product.
     * @param currencyUomId the currency for which the cost is calculated
     * @return the unit price for the given currency
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getUnitPrice(String currencyUomId) throws RepositoryException {
        return getRepository().getUnitPrice(this, currencyUomId);
    }

    /**
     * Gets the standard cost for this product.
     * @return the standard cost for the default currency
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getStandardCost() throws RepositoryException {
        return getRepository().getStandardCost(this);
    }

    /**
     * Gets the standard cost for this product.
     * @param currencyUomId the currency for which the cost is calculated
     * @return the standard cost for the given currency
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getStandardCost(String currencyUomId) throws RepositoryException {
        return getRepository().getStandardCost(this, currencyUomId);
    }

    /**
     * Gets the variant products.  This product must be virtual.  If no
     * variants found, returns an empty list.
     * @return Variants or an empty list.
     * @throws RepositoryException if an error occurs
     */
    public List<Product> getVariants() throws RepositoryException {
        return getRepository().getVariants(this);
    }

    /**
     * Gets the Product this Product is a variant of. If no
     * variants found, returns null.
     * @return Product this is a Variant of or null
     * @throws RepositoryException if an error occurs
     */
    public Product getVariantOf() throws RepositoryException {
        if (variantOf == null) {
            variantOf = getRepository().getVariantOf(this);
        }
        return variantOf;
    }

    private ProductRepositoryInterface getRepository() {
        return ProductRepositoryInterface.class.cast(repository);
    }

    /**
     * Gets the sale price for this product.
     * @return the sale price for the default currency
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getSalePrice() throws RepositoryException {
        return getRepository().getSalePrice(this);
    }

    /**
     * Gets the sale price for this product.
     * @param currencyUomId the currency for which the sale price is calculated
     * @return the sale price for the given currency
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getSalePrice(String currencyUomId) throws RepositoryException {
        return getRepository().getSalePrice(this, currencyUomId);
    }

    /**
     * Gets the base price for this product.
     * @return the unit price for the default currency
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getBasePrice() throws RepositoryException {
        return getRepository().getBasePrice(this);
    }

    /**
     * Gets the base price for this product.
     * @param currencyUomId the currency for which the cost is calculated
     * @return the unit price for the given currency
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getBasePrice(String currencyUomId) throws RepositoryException {
        return getRepository().getBasePrice(this, currencyUomId);
    }

    /**
     * Gets all the features of this product.
     * @return a list of <code>ProductFeatureAndAppl</code> values
     * @exception RepositoryException if an error occurs
     * @see #getStandardFeatures()
     */
    public List<ProductFeatureAndAppl> getFeatures() throws RepositoryException {
        if (features == null) {
            features = getRepository().getProductFeatures(this);
        }
        return features;
    }

    /**
     * Gets the standard features of this product.
     * @return a list of <code>ProductFeatureAndAppl</code> values
     * @exception RepositoryException if an error occurs
     * @see #getFeatures()
     */
    public List<ProductFeatureAndAppl> getStandardFeatures() throws RepositoryException {
        if (stdFeatures == null) {
            stdFeatures = getRepository().getProductFeatures(this, "STANDARD_FEATURE");
        }
        return stdFeatures;
    }

    /**
     * Gets the field value, check the parent if this product is variant and its field value is empty.
     * @param field a <code>EntityFieldInterface</code> value
     * @return a <code>String</code> value, or null if not found
     */
    public Object getAndFallbackToParent(EntityFieldInterface<? extends org.opentaps.base.entities.Product> field) {
        return getAndFallbackToParent(field.getName());
    }

    /**
     * Gets the field value, check the parent if this product is variant and its field value is empty.
     * @param fieldName a <code>String</code> value
     * @return a <code>String</code> value, or null if not found
     */
    public Object getAndFallbackToParent(String fieldName) {
        if (UtilValidate.isEmpty(super.get(fieldName)) && isVariant()) {
            try {
                Product parent = getVariantOf();
                if (parent != null) {
                    return parent.getAndFallbackToParent(fieldName);
                }
            } catch (RepositoryException e) {
                return null;
            }
        }
        return super.get(fieldName);
    }

    /**
     * Gets the field value, check the parent if this product is variant and its field value is empty.
     * @param field a <code>EntityFieldInterface</code> value
     * @return a <code>String</code> value, or null if not found
     */
    public String getStringAndFallbackToParent(EntityFieldInterface<? extends org.opentaps.base.entities.Product> field) {
        return getStringAndFallbackToParent(field.getName());
    }

    /**
     * Gets the field value, check the parent if this product is variant and its field value is empty.
     * @param fieldName a <code>String</code> value
     * @return a <code>String</code> value, or null if not found
     */
    public String getStringAndFallbackToParent(String fieldName) {
        if (UtilValidate.isEmpty(super.getString(fieldName)) && isVariant()) {
            try {
                Product parent = getVariantOf();
                if (parent != null) {
                    return parent.getStringAndFallbackToParent(fieldName);
                }
            } catch (RepositoryException e) {
                return null;
            }
        }
        return super.getString(fieldName);
    }

    /**
     * Gets the field value, check the parent if this product is variant and its field value is empty.
     * @param field a <code>EntityFieldInterface</code> value
     * @return a <code>String</code> value, or null if not found
     */
    public Timestamp getTimestampAndFallbackToParent(EntityFieldInterface<? extends org.opentaps.base.entities.Product> field) {
        return getTimestampAndFallbackToParent(field.getName());
    }

    /**
     * Gets the field value, check the parent if this product is variant and its field value is empty.
     * @param fieldName a <code>String</code> value
     * @return a <code>String</code> value, or null if not found
     */
    public Timestamp getTimestampAndFallbackToParent(String fieldName) {
        if (UtilValidate.isEmpty(super.getTimestamp(fieldName)) && isVariant()) {
            try {
                Product parent = getVariantOf();
                if (parent != null) {
                    return parent.getTimestampAndFallbackToParent(fieldName);
                }
            } catch (RepositoryException e) {
                return null;
            }
        }
        return super.getTimestamp(fieldName);
    }

    /**
     * Gets the field value, check the parent if this product is variant and its field value is empty.
     * @param field a <code>EntityFieldInterface</code> value
     * @return a <code>String</code> value, or null if not found
     */
    public BigDecimal getBigDecimalAndFallbackToParent(EntityFieldInterface<? extends org.opentaps.base.entities.Product> field) {
        return getBigDecimalAndFallbackToParent(field.getName());
    }

    /**
     * Gets the field value, check the parent if this product is variant and its field value is empty.
     * @param fieldName a <code>String</code> value
     * @return a <code>String</code> value, or null if not found
     */
    public BigDecimal getBigDecimalAndFallbackToParent(String fieldName) {
        if (UtilValidate.isEmpty(super.getBigDecimal(fieldName)) && isVariant()) {
            try {
                Product parent = getVariantOf();
                if (parent != null) {
                    return parent.getBigDecimalAndFallbackToParent(fieldName);
                }
            } catch (RepositoryException e) {
                return null;
            }
        }
        return super.getBigDecimal(fieldName);
    }

    /**
     * Gets the product primary category.
     * @return a <code>ProductCategory</code> value, or null if not found
     * @exception RepositoryException if an error occurs
     */
    public ProductCategory getPrimaryCategory() throws RepositoryException {
        if (primaryCategory == null) {
            primaryCategory = getRepository().getPrimaryParentCategory(this);
        }
        return primaryCategory;
    }
}

