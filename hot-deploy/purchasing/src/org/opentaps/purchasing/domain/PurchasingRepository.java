/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.purchasing.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.domain.base.entities.SupplierProduct;
import org.opentaps.domain.purchasing.PurchasingRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

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
        Map<String, Object> params = UtilMisc.<String, Object>toMap("productId", productId,
                                    "partyId", supplierPartyId,
                                    "currencyUomId", currencyUomId,
                                    "quantity", quantityToPurchase);
        try {
            Map<String, Object> result = getDispatcher().runSync("getSuppliersForProduct", params);
            List<GenericValue> productSuppliers = (List<GenericValue>) result.get("supplierProducts");
            if ((productSuppliers != null) && (productSuppliers.size() > 0)) {
                supplierProduct = productSuppliers.get(0);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), MODULE);
        }

        if (supplierProduct == null) {
            return null;
        }

        return loadFromGeneric(SupplierProduct.class, supplierProduct);
    }

    /** {@inheritDoc} */
    public void createSupplierProduct(SupplierProduct supplierProduct) throws RepositoryException {
        Map<String, Object> supplierProductMap = UtilMisc.<String, Object>toMap("productId", supplierProduct.getProductId());
        supplierProductMap.put("supplierProductId", supplierProduct.getSupplierProductId());
        // contruct parameters for call service
        supplierProductMap.put("partyId", supplierProduct.getPartyId());
        supplierProductMap.put("minimumOrderQuantity",  supplierProduct.getMinimumOrderQuantity());
        supplierProductMap.put("lastPrice",  supplierProduct.getLastPrice());
        supplierProductMap.put("currencyUomId",  supplierProduct.getCurrencyUomId());
        supplierProductMap.put("availableFromDate",  supplierProduct.getAvailableFromDate());
        supplierProductMap.put("comments",  supplierProduct.getComments());
        if (supplierProduct.getAvailableThruDate() != null) {
            supplierProductMap.put("availableThruDate",  supplierProduct.getAvailableThruDate());
        }
        if (supplierProduct.getCanDropShip() != null) {
            supplierProductMap.put("canDropShip",  supplierProduct.getCanDropShip());
        }
        if (supplierProduct.getOrderQtyIncrements() != null) {
            supplierProductMap.put("orderQtyIncrements",  supplierProduct.getOrderQtyIncrements());
        }
        if (supplierProduct.getQuantityUomId() != null) {
            supplierProductMap.put("quantityUomId",  supplierProduct.getQuantityUomId());
        }
        if (supplierProduct.getStandardLeadTimeDays() != null) {
            supplierProductMap.put("standardLeadTimeDays",  supplierProduct.getStandardLeadTimeDays());
        }
        if (supplierProduct.getSupplierProductName() != null) {
            supplierProductMap.put("supplierProductName",  supplierProduct.getSupplierProductName());
        }
        if (supplierProduct.getSupplierPrefOrderId() != null) {
            supplierProductMap.put("supplierPrefOrderId",  supplierProduct.getSupplierPrefOrderId());
        }
        if (supplierProduct.getSupplierRatingTypeId() != null) {
            supplierProductMap.put("supplierRatingTypeId",  supplierProduct.getSupplierRatingTypeId());
        }
        if (supplierProduct.getUnitsIncluded() != null) {
            supplierProductMap.put("unitsIncluded",  supplierProduct.getUnitsIncluded());
        }
        supplierProductMap.put("userLogin", getInfrastructure().getSystemUserLogin());
        try {
            //call service to create supplierProduct
            Map<String, Object> results = getDispatcher().runSync("createSupplierProduct", supplierProductMap);
            if (ServiceUtil.isError(results)) {
                throw new RepositoryException("can not create supplier product");
            }
        } catch (GenericServiceException e) {
            throw new RepositoryException(e);
        }
    }
}
