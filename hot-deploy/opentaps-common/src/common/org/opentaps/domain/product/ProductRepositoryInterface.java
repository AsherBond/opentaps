/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.product;

import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for Products to handle interaction of Product-related domain with the entity engine (database) and the service engine.
 */
public interface ProductRepositoryInterface extends RepositoryInterface {

    /**
     * Finds the <code>Product</code> with the given ID.
     * @return never null unless the given Product identifier is null
     * @param productId Product identifier
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>Product</code> is found for the given id
     */
    public Product getProductById(String productId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds the <code>Product</code> standard cost.
     * @param product the <code>Product</code> for which the cost is calculated
     * @param currencyUomId the currency for which the cost is calculated
     * @return the standard cost for the given currency
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getStandardCost(Product product, String currencyUomId) throws RepositoryException;

    /**
     * Finds the <code>Product</code> unit price.
     * @param product the <code>Product</code> for which the cost is calculated
     * @param currencyUomId the currency for which the cost is calculated
     * @return the unit price for the given currency
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getUnitPrice(Product product, String currencyUomId) throws RepositoryException;

    /**
     * Finds the <code>Product</code> unit price.
     * @param product the <code>Product</code> for which the cost is calculated
     * @param quantity the quantity to get the unit price for
     * @param currencyUomId the currency for which the cost is calculated
     * @param partyId the party for which to get the price
     * @return the unit price for the given currency
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getUnitPrice(Product product, BigDecimal quantity, String currencyUomId, String partyId) throws RepositoryException;

    /**
     * Finds the variants of a Product.  Returns empty list if none found.
     * @param product
     * @return variants of product
     * @throws RepositoryException
     */
    public List<Product> getVariants(Product product) throws RepositoryException;
}
