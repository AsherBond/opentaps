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
package org.opentaps.purchasing.domain;

import java.math.BigDecimal;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.opentaps.base.entities.SupplierProduct;
import org.opentaps.base.services.CreateSupplierProductService;
import org.opentaps.base.services.GetSuppliersForProductService;
import org.opentaps.domain.purchasing.PurchasingRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceException;

/** {@inheritDoc} */
public class PurchasingRepository extends Repository implements PurchasingRepositoryInterface {

    private static final String MODULE = PurchasingRepository.class.getName();

    /**
     * Default constructor.
     */
    public PurchasingRepository() {
        super();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public SupplierProduct getSupplierProduct(String supplierPartyId, String productId, BigDecimal quantityToPurchase, String currencyUomId) throws RepositoryException {
        GenericValue supplierProduct = null;
        try {
            GetSuppliersForProductService service = new GetSuppliersForProductService();
            service.setInProductId(productId);
            service.setInPartyId(supplierPartyId);
            service.setInCurrencyUomId(currencyUomId);
            service.setInQuantity(quantityToPurchase);
            service.runSync(getInfrastructure());
            List<GenericValue> productSuppliers = service.getOutSupplierProducts();
            if ((productSuppliers != null) && (productSuppliers.size() > 0)) {
                supplierProduct = productSuppliers.get(0);
            }
        } catch (ServiceException e) {
            Debug.logError(e.getMessage(), MODULE);
        }

        if (supplierProduct == null) {
            return null;
        }

        return loadFromGeneric(SupplierProduct.class, supplierProduct);
    }

    /** {@inheritDoc} */
    public void createSupplierProduct(SupplierProduct supplierProduct) throws RepositoryException {
        CreateSupplierProductService service = new CreateSupplierProductService();
        service.setInProductId(supplierProduct.getProductId());
        service.setInSupplierProductId(supplierProduct.getSupplierProductId());
        // contruct parameters for call service
        service.setInPartyId(supplierProduct.getPartyId());
        service.setInMinimumOrderQuantity(supplierProduct.getMinimumOrderQuantity());
        service.setInLastPrice(supplierProduct.getLastPrice());
        service.setInCurrencyUomId(supplierProduct.getCurrencyUomId());
        service.setInAvailableFromDate(supplierProduct.getAvailableFromDate());
        service.setInComments(supplierProduct.getComments());
        if (supplierProduct.getAvailableThruDate() != null) {
            service.setInAvailableThruDate(supplierProduct.getAvailableThruDate());
        }
        if (supplierProduct.getCanDropShip() != null) {
            service.setInCanDropShip(supplierProduct.getCanDropShip());
        }
        if (supplierProduct.getOrderQtyIncrements() != null) {
            service.setInOrderQtyIncrements(supplierProduct.getOrderQtyIncrements());
        }
        if (supplierProduct.getQuantityUomId() != null) {
            service.setInQuantityUomId(supplierProduct.getQuantityUomId());
        }
        if (supplierProduct.getStandardLeadTimeDays() != null) {
            service.setInStandardLeadTimeDays(supplierProduct.getStandardLeadTimeDays());
        }
        if (supplierProduct.getSupplierProductName() != null) {
            service.setInSupplierProductName(supplierProduct.getSupplierProductName());
        }
        if (supplierProduct.getSupplierPrefOrderId() != null) {
            service.setInSupplierPrefOrderId(supplierProduct.getSupplierPrefOrderId());
        }
        if (supplierProduct.getSupplierRatingTypeId() != null) {
            service.setInSupplierRatingTypeId(supplierProduct.getSupplierRatingTypeId());
        }
        if (supplierProduct.getUnitsIncluded() != null) {
            service.setInUnitsIncluded(supplierProduct.getUnitsIncluded());
        }
        service.setInUserLogin(getInfrastructure().getSystemUserLogin());
        try {
            //call service to create supplierProduct
            service.runSync(getInfrastructure());
            if (service.isError()) {
                throw new RepositoryException("can not create supplier product");
            }
        } catch (ServiceException e) {
            throw new RepositoryException(e);
        }
    }
}
