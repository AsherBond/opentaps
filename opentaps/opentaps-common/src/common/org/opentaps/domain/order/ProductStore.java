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
package org.opentaps.domain.order;

import java.util.List;

import org.opentaps.foundation.repository.RepositoryException;

import org.opentaps.base.entities.ProdCatalog;
import org.opentaps.base.entities.ProductStoreCatalog;
import org.opentaps.base.entities.ProductStoreShipmentMeth;
import org.opentaps.base.entities.ProductStoreShipmentMethView;

/**
 * Product Store entity.
 */
public class ProductStore extends org.opentaps.base.entities.ProductStore {

    private List<ProdCatalog> prodCatalogs;
    private List<ProductStoreCatalog> productStoreCatalogs;
    private List<ProductStoreShipmentMeth> productStoreShipmentMeths;
    private List<ProductStoreShipmentMethView> productStoreShipmentMethViews;

    /**
     * Default constructor.
     */
    public ProductStore() {
        super();
    }

    /**
     * Gets the list of <code>ProdCatalog</code> for this product store.
     * @return list of <code>ProdCatalog</code>
     * @throws RepositoryException if an error occurs
     */
    public List<ProdCatalog> getProdCatalogs() throws RepositoryException {
        if (prodCatalogs == null) {
            prodCatalogs = getRepository().getRelatedProdCatalogs(this);
        }
        return prodCatalogs;
    }

    /**
     * Gets the list of <code>ProductStoreShipmentMeth</code> for this product store.
     * @return list of <code>ProductStoreShipmentMeth</code>
     * @throws RepositoryException if an error occurs
     */
    public List<ProductStoreShipmentMeth> getProductStoreShipmentMeths() throws RepositoryException {
        if (productStoreShipmentMeths == null) {
            productStoreShipmentMeths = getRepository().getRelatedProductStoreShipmentMeths(this);
        }
        return productStoreShipmentMeths;
    }

    /**
     * Gets the list of <code>ProductStoreShipmentMethView</code> for this product store.
     * @return list of <code>ProductStoreShipmentMethView</code>
     * @throws RepositoryException if an error occurs
     */
    public List<ProductStoreShipmentMethView> getProductStoreShipmentMethViews() throws RepositoryException {
        if (productStoreShipmentMethViews == null) {
            productStoreShipmentMethViews = getRepository().getRelatedProductStoreShipmentMethViews(this);
        }
        return productStoreShipmentMethViews;
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
