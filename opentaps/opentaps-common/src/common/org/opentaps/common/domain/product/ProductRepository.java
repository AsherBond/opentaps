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
package org.opentaps.common.domain.product;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.constants.ProductAssocTypeConstants;
import org.opentaps.base.entities.GoodIdentification;
import org.opentaps.base.entities.ProductAssoc;
import org.opentaps.base.entities.ProductFeatureAndAppl;
import org.opentaps.base.entities.ProductPrice;
import org.opentaps.base.services.CalculateProductPriceService;
import org.opentaps.base.services.GetProductByComprehensiveSearchService;
import org.opentaps.base.services.GetProductCostService;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceException;

/** {@inheritDoc} */
public class ProductRepository extends Repository implements ProductRepositoryInterface {

    /**
     * Default constructor.
     */
    public ProductRepository() {
        super();
    }

    /** {@inheritDoc} */
    public Product getProductById(String productId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(productId)) {
            return null;
        }

        return findOneNotNull(Product.class, map(Product.Fields.productId, productId), "OpentapsError_ProductNotFound", UtilMisc.toMap("productId", productId));
    }

    /** {@inheritDoc} */
    public BigDecimal getUnitPrice(Product product, String currencyUomId) throws RepositoryException {
        return getUnitPrice(product, null, currencyUomId, null);
    }

    /** {@inheritDoc} */
    public BigDecimal getUnitPrice(Product product, BigDecimal quantity, String currencyUomId, String partyId) throws RepositoryException {
        return getUnitPrice(product, quantity, currencyUomId, partyId, null);
    }


    /** {@inheritDoc} */
    public BigDecimal getUnitPrice(Product product, BigDecimal quantity, String currencyUomId, String partyId, String productCatalogId) throws RepositoryException {

        try {
            CalculateProductPriceService service = new CalculateProductPriceService();
            service.setInProduct(Repository.genericValueFromEntity(product));
            service.setInPartyId(partyId);
            service.setInQuantity(quantity);
            service.setInCurrencyUomId(currencyUomId);
            service.setInCheckIncludeVat("Y");
            service.setInProdCatalogId(productCatalogId);
            service.runSyncNoNewTransaction(getInfrastructure());
            if (service.isError()) {
                throw new RepositoryException(service.getErrorMessage());
            }
            return service.getOutPrice();
        } catch (ServiceException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public BigDecimal getStandardCost(Product product, String currencyUomId) throws RepositoryException {
        try {
            GetProductCostService service = new GetProductCostService(getUser());
            service.setInProductId(product.getProductId());
            service.setInCurrencyUomId(currencyUomId);
            service.setInCostComponentTypePrefix("EST_STD");
            service.runSync(getInfrastructure());
            if (service.isError()) {
                throw new RepositoryException(service.getErrorMessage());
            }
            return service.getOutProductCost();
        } catch (ServiceException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public List<Product> getVariants(Product product) throws RepositoryException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition(ProductAssoc.Fields.productId.name(), product.getProductId()),
                EntityCondition.makeCondition(ProductAssoc.Fields.productAssocTypeId.name(), ProductAssocTypeConstants.PRODUCT_VARIANT),
                EntityUtil.getFilterByDateExpr());

        List<ProductAssoc> variants = findList(ProductAssoc.class, conditions);
        return findList(Product.class, EntityCondition.makeCondition(Product.Fields.productId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(variants, ProductAssoc.Fields.productIdTo)));
    }

    /** {@inheritDoc} */
    public Product getProductByComprehensiveSearch(String id) throws RepositoryException {
        try {
            Product product = null;
            GetProductByComprehensiveSearchService service = new GetProductByComprehensiveSearchService();
            service.setInProductId(id);
            service.runSync(getInfrastructure());
            if (service.isSuccess()) {
                GenericValue productGv = service.getOutProduct();
                if (productGv != null) {
                    // construct Product domain object and return
                    product = loadFromGeneric(Product.class, productGv, this);
                }
            }
            return product;
        } catch (ServiceException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public List<GoodIdentification> getAlternateProductIds(String productId) throws RepositoryException {
        String hql = "from GoodIdentification eo where eo.id.productId = :productId";
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            Query query = session.createQuery(hql);
            query.setParameter("productId", productId);
            List<GoodIdentification> goodIdentifications = query.list();
            return goodIdentifications;
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /** {@inheritDoc} */
    public BigDecimal getSalePrice(Product product, String currencyUomId) throws RepositoryException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition(ProductPrice.Fields.productId.name(), product.getProductId()),
                EntityCondition.makeCondition(ProductPrice.Fields.currencyUomId.name(), currencyUomId),
                EntityCondition.makeCondition(ProductPrice.Fields.productPriceTypeId.name(), "PROMO_PRICE"),
                EntityUtil.getFilterByDateExpr());
        List<ProductPrice> productPrices = findList(ProductPrice.class, conditions);
        return productPrices.size() == 0 ? null : productPrices.get(0).getPrice();
    }

    /** {@inheritDoc} */
    public BigDecimal getBasePrice(Product product, String currencyUomId) throws RepositoryException {
        try {
            CalculateProductPriceService service = new CalculateProductPriceService();
            service.setInProduct(Repository.genericValueFromEntity(product));
            service.setInCurrencyUomId(currencyUomId);
            service.runSyncNoNewTransaction(getInfrastructure());
            if (service.isError()) {
                throw new RepositoryException(service.getErrorMessage());
            }
            BigDecimal basePrice = UtilValidate.isNotEmpty(service.getOutListPrice()) ? service.getOutListPrice() : service.getOutDefaultPrice();
            return basePrice;
        } catch (ServiceException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public List<ProductFeatureAndAppl> getProductFeatures(Product product) throws RepositoryException {
        return getProductFeatures(product, null);
    }

    /** {@inheritDoc} */
    public List<ProductFeatureAndAppl> getProductFeatures(Product product, String productFeatureApplTypeId) throws RepositoryException {
        return findListCache(ProductFeatureAndAppl.class, map(ProductFeatureAndAppl.Fields.productId, product.getProductId(),
                                                              ProductFeatureAndAppl.Fields.productFeatureApplTypeId, productFeatureApplTypeId),
                             Arrays.asList(ProductFeatureAndAppl.Fields.sequenceNum.asc(),
                                           ProductFeatureAndAppl.Fields.productFeatureApplTypeId.asc()));
    }
}
