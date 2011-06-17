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
import java.util.Set;

import javolution.util.FastSet;
import org.hibernate.HibernateException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
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
import org.opentaps.base.entities.ProductCategory;
import org.opentaps.base.entities.ProductCategoryMember;
import org.opentaps.base.entities.ProductFeatureAndAppl;
import org.opentaps.base.entities.ProductPrice;
import org.opentaps.base.services.CalculateProductPriceService;
import org.opentaps.base.services.GetProductByComprehensiveSearchService;
import org.opentaps.base.services.GetProductCostService;
import org.opentaps.common.util.UtilCommon;
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

/**
 * Repository for Products to handle interaction of Product-related domain with the entity engine (database) and the service engine.
 */
public class ProductRepository extends Repository implements ProductRepositoryInterface {

    private static final String MODULE = ProductRepository.class.getName();

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
    public BigDecimal getUnitPrice(Product product) throws RepositoryException {
        return getUnitPrice(product, null, UtilCommon.getDefaultCurrencyUomId(), null);
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
    public BigDecimal getStandardCost(Product product) throws RepositoryException {
        return getStandardCost(product, UtilCommon.getDefaultCurrencyUomId());
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
    public Product getVariantOf(Product product) throws RepositoryException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition(ProductAssoc.Fields.productIdTo.name(), product.getProductId()),
                EntityCondition.makeCondition(ProductAssoc.Fields.productAssocTypeId.name(), ProductAssocTypeConstants.PRODUCT_VARIANT),
                EntityUtil.getFilterByDateExpr());

        List<ProductAssoc> variants = findList(ProductAssoc.class, conditions);
        return Entity.getFirst(findList(Product.class, EntityCondition.makeCondition(Product.Fields.productId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(variants, ProductAssoc.Fields.productId))));
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
    public GoodIdentification getGoodIdentificationByType(String productId, String goodIdentificationTypeId) throws RepositoryException {
        GoodIdentification gid = findOne(GoodIdentification.class, map(GoodIdentification.Fields.productId, productId, GoodIdentification.Fields.goodIdentificationTypeId, goodIdentificationTypeId));
        // consider an empty value as non existent
        if (gid == null || UtilValidate.isEmpty(gid.getIdValue())) {
            return null;
        }
        return gid;
    }

    /** {@inheritDoc} */
    public ProductCategory getProductCategoryById(String productCategoryId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(productCategoryId)) {
            return null;
        }
        return findOneNotNull(ProductCategory.class, map(ProductCategory.Fields.productCategoryId, productCategoryId), "OpentapsError_ProductCategoryNotFound", UtilMisc.toMap("productCategoryId", productCategoryId));
    }

    /** {@inheritDoc} */
    public List<ProductCategory> getRollupChildCategories(String parentCategoryId) throws RepositoryException {
        return getChildCategories(parentCategoryId, true, false);

    }

    /** {@inheritDoc} */
    public List<ProductCategory> getPrimaryParentChildCategories(String parentCategoryId) throws RepositoryException {
        return getChildCategories(parentCategoryId, false, true);
    }

    /** {@inheritDoc} */
    public List<ProductCategory> getChildCategories(String parentCategoryId) throws RepositoryException {
        return getChildCategories(parentCategoryId, true, true);
    }

    @SuppressWarnings("unchecked")
    private List<ProductCategory> getChildCategories(String parentCategoryId, boolean useRollup, boolean usePrimaryParent) throws RepositoryException {
        if (UtilValidate.isEmpty(parentCategoryId)) {
            return null;
        }

        try {

            Session session = getInfrastructure().getSession();

            // collect IDs for categories which are child of parentCategoryId
            Set<String> childCategoryIds = FastSet.<String>newInstance();

            if (useRollup) {
                StringBuilder rollupsStmt = new StringBuilder("select distinct r.id.productCategoryId from ProductCategoryRollup r where ");
                rollupsStmt.append("r.id.parentProductCategoryId = :parentProductCategoryId").append(" and ");
                rollupsStmt.append("((r.thruDate IS NULL OR r.thruDate > :now) AND (r.id.fromDate IS NULL OR r.id.fromDate <= :now))");
                Query query = getInfrastructure().getSession().createQuery(rollupsStmt.toString());
                query.setString("parentProductCategoryId", parentCategoryId);
                query.setTimestamp("now", UtilDateTime.nowTimestamp());

                List<String> productCategoryRollups = query.list();
                if (UtilValidate.isNotEmpty(productCategoryRollups)) {
                    childCategoryIds.addAll(productCategoryRollups);
                }
            }

            if (usePrimaryParent) {
                String categoriesStmt = "select pc.productCategoryId from ProductCategory pc where pc.primaryParentCategoryId = :primaryParentCategoryId";
                Query catQuery = session.createQuery(categoriesStmt);
                catQuery.setString("primaryParentCategoryId", parentCategoryId);
                List<String> productCategories = catQuery.list();
                if (UtilValidate.isNotEmpty(productCategories)) {
                    childCategoryIds.addAll(productCategories);
                }
            }

            // get all categories if any
            if (UtilValidate.isNotEmpty(childCategoryIds)) {
                String neighborsStmt = "from ProductCategory pc where pc.productCategoryId in (:categoryIds)";
                Query neighborsQuery = session.createQuery(neighborsStmt);
                neighborsQuery.setParameterList("categoryIds", childCategoryIds.toArray());
                List<ProductCategory> neighbourCategories = neighborsQuery.list();
                return neighbourCategories;
            }

        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } catch (HibernateException e) {
            throw new RepositoryException(e);
        }

        return null;
    }

    /** {@inheritDoc} */
    public BigDecimal getSalePrice(Product product) throws RepositoryException {
        return getSalePrice(product, UtilCommon.getDefaultCurrencyUomId());
    }

    /** {@inheritDoc} */
    public BigDecimal getSalePrice(Product product, String currencyUomId) throws RepositoryException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition(ProductPrice.Fields.productId.name(), product.getProductId()),
                EntityCondition.makeCondition(ProductPrice.Fields.currencyUomId.name(), currencyUomId),
                EntityCondition.makeCondition(ProductPrice.Fields.productPriceTypeId.name(), "PROMO_PRICE"),
                EntityUtil.getFilterByDateExpr());
        List<ProductPrice> productPrices = findList(ProductPrice.class, conditions);
        if (UtilValidate.isEmpty(productPrices)) {
            // check if the product has a parent
            if (product.isVariant() && product.getVariantOf() != null) {
                return getSalePrice(product.getVariantOf(), currencyUomId);
            } else {
                return null;
            }
        } else {
            return productPrices.get(0).getPrice();
        }
    }

    /** {@inheritDoc} */
    public BigDecimal getBasePrice(Product product) throws RepositoryException {
        return getBasePrice(product, UtilCommon.getDefaultCurrencyUomId());
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

    /** {@inheritDoc} */
    public ProductCategory getPrimaryParentCategory(String productId) throws RepositoryException, EntityNotFoundException {
        return getPrimaryParentCategory(getProductById(productId));
    }

    /** {@inheritDoc} */
    public ProductCategory getPrimaryParentCategory(Product product) throws RepositoryException {

        ProductCategory cat = null;

        String primaryProductCategoryId = product.getStringAndFallbackToParent(Product.Fields.primaryProductCategoryId);
        if (UtilValidate.isNotEmpty(primaryProductCategoryId)) {
            cat = findOneCache(ProductCategory.class, map(ProductCategory.Fields.productCategoryId, primaryProductCategoryId));
            if (cat == null) {
                Debug.logError("Missing product category [" + primaryProductCategoryId + "] for product [" + product.getProductId() + "]", MODULE);
                return null;
            } else {
                return cat;
            }
        }

        // else take the first parent in the ProductCategoryMember of this product (or its parent if it is a variant)
        ProductCategoryMember member = getPrimaryMember(product);
        if (member == null) {
            return null;
        }
        return findOneCache(ProductCategory.class, map(ProductCategory.Fields.productCategoryId, member.getProductCategoryId()));
    }

    private ProductCategoryMember getPrimaryMember(Product product) throws RepositoryException {
        if (product == null) {
            return null;
        }

        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition(ProductCategoryMember.Fields.productId.name(), product.getProductId()),
                EntityUtil.getFilterByDateExpr());
        // use sequence to order, else take the oldest category
        ProductCategoryMember member = Entity.getFirst(findListCache(ProductCategoryMember.class, conditions, UtilMisc.toList(ProductCategoryMember.Fields.sequenceNum.asc(), ProductCategoryMember.Fields.fromDate.asc())));
        if (member == null && product.isVariant()) {
            Product parent = product.getVariantOf();
            return getPrimaryMember(parent);
        } else {
            return member;
        }
    }
}
