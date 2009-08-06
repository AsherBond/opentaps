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
package org.opentaps.domain.purchasing;

import java.math.BigDecimal;

import org.opentaps.domain.base.entities.SupplierProduct;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for Purchasing to handle interaction of Purchasing-related domain with the entity engine (database) and the service engine.
 */
public interface PurchasingRepositoryInterface extends RepositoryInterface {

    /**
     * Finds the <code>SupplierProduct</code> for this combination of parameters.
     * The <code>SupplierProduct</code> will be the one with with the lowest lastPrice.
     * @param supplierPartyId a <code>String</code> value
     * @param productId a <code>String</code> value
     * @param quantityToPurchase a <code>String</code> value
     * @param currencyUomId a <code>String</code> value
     * @return the list of related <code>Product</code>
     * @throws RepositoryException if an error occurs
     */
    public SupplierProduct getSupplierProduct(String supplierPartyId, String productId, BigDecimal quantityToPurchase, String currencyUomId) throws RepositoryException;

    /**
     * Creates a <code>SupplierProduct</code>.
     * @param supplierProduct a <code>SupplierProduct</code> value
     * @throws RepositoryException if an error occurs
     */
    public void createSupplierProduct(SupplierProduct supplierProduct) throws RepositoryException;


}
