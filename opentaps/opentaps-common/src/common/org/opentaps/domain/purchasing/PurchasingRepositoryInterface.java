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
package org.opentaps.domain.purchasing;

import java.math.BigDecimal;

import org.opentaps.base.entities.SupplierProduct;
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
