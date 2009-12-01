/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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

import org.opentaps.base.entities.ProductType;
import org.opentaps.foundation.repository.RepositoryException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product entity, encapsulate product specific functionality.
 */
public class Product extends org.opentaps.base.entities.Product {

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
     * Gets this product type.
     * @return the <code>ProductType</code>
     * @throws RepositoryException if an error occurs
     */
    public ProductType getType() throws RepositoryException {
        return this.getProductType();
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

    private ProductRepositoryInterface getRepository() {
        return ProductRepositoryInterface.class.cast(repository);
    }
}
