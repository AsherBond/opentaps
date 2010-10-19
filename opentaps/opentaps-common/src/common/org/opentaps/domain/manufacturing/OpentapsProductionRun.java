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

package org.opentaps.domain.manufacturing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.manufacturing.jobshopmgt.ProductionRun;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Opentaps extension of the base <code>ProductionRun</code>.
 */
public class OpentapsProductionRun extends ProductionRun {

    private  Map<String, BigDecimal> productsToProduce = null;
    private  Map<String, BigDecimal> productsProduced = null;

    private  Map<String, BigDecimal> productsPlannedToProduce = null;

    private User user;
    private Infrastructure infrastructure;
    private DomainsLoader domainLoader;
    private ProductRepositoryInterface productRepository;
    private OrganizationRepositoryInterface organizationRepository;

    private Organization organization;
    private Product productProduced;

    /**
     * In this implementation, a production run must be created with real data or this crashes.
     * @param productionRunId the production run ID, which is really the <code>WorkEffort</code> ID
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @exception IllegalArgumentException if an error occurs
     */
    public OpentapsProductionRun(String productionRunId, LocalDispatcher dispatcher) throws IllegalArgumentException {
        super(productionRunId, dispatcher.getDelegator(), dispatcher);
        if (productionRun == null) {
            throw new IllegalArgumentException("No Production Run with ID [" + productionRunId + "] found.");
        }

        // initialize domain infrastructure, and get the organization and product produce as domain objects
        // we will use those to determine if we can early receive products, see canProduce()
        try {
            infrastructure = new Infrastructure(dispatcher);
            user = new User(infrastructure.getSystemUserLogin());
            domainLoader = new DomainsLoader(infrastructure, user);
            DomainsDirectory domainsDirectory = domainLoader.loadDomainsDirectory();
            productRepository = domainsDirectory.getProductDomain().getProductRepository();
            organizationRepository = domainsDirectory.getOrganizationDomain().getOrganizationRepository();

            String facilityId = productionRun.getString("facilityId");
            GenericValue facility = dispatcher.getDelegator().findByPrimaryKey("Facility", UtilMisc.toMap("facilityId", facilityId));
            organization = organizationRepository.getOrganizationById(facility.getString("ownerPartyId"));

            GenericValue productGv = getProductProduced();
            if (productGv != null) {
                productProduced = productRepository.getProductById(productGv.getString("productId"));
            }
        } catch (FoundationException e) {
            Debug.logError("Problem getting domain infrastructure: " + e.getMessage(), module);
        } catch (GenericEntityException e) {
            Debug.logError("Problem getting domain infrastructure: " + e.getMessage(), module);
        }

    }

    /**
     * Gets the <code>GenericValue</code> <code>WorkEffort</code> representing this production run.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getProductionRun() {
        return productionRun;
    }

    /**
     * Checks if this production run is an assembly.
     * @return a <code>boolean</code> value
     */
    public boolean isAssembly() {
        return "WEPT_PRODUCTION_RUN".equals(productionRun.get("workEffortPurposeTypeId"));
    }

    /**
     * Checks if this production run is an disassembly.
     * @return a <code>boolean</code> value
     */
    public boolean isDisassembly() {
        return "WEPT_DISASSEMBLY".equals(productionRun.get("workEffortPurposeTypeId"));
    }

    /**
     * Gets the products being produced in this production run, with keys <code>productId</code> and values <code>quantity</code>.
     * Products actually produced is a separate concept.
     * @return a <code>Map</code> of products being produced in this production run, with keys <code>productId</code> and values <code>quantity</code>
     * @exception GenericEntityException if an error occurs
     * @see #getProductsProduced
     */
    public Map<String, BigDecimal> getProductsToProduce() throws GenericEntityException {
        if (productsToProduce != null) {
            return productsToProduce;
        }
        productsToProduce = getWegsProductSums("PRUN_PROD_DELIV");
        return productsToProduce;
    }

    /**
     * Gets the quantity of the given product being produced, or null if it's not being produced.
     * @param productId a <code>String</code> value
     * @return the quantity of the given product being produced
     * @exception GenericEntityException if an error occurs
     */
    public BigDecimal getQuantityToProduce(String productId) throws GenericEntityException {
        Map<String, BigDecimal> map = getProductsToProduce();
        return map.get(productId);
    }

    /**
     * Gets the quantity planned to produce of the given product, or null if it's not planned to be produced.
     * This quantity includes the WEGS which validity date is in the future.
     * @param productId a <code>String</code> value
     * @return the quantity of the given product being produced
     * @exception GenericEntityException if an error occurs
     * @see #getQuantityToProduce
     */
    public BigDecimal getQuantityPlannedToProduce(String productId) throws GenericEntityException {
        Map<String, BigDecimal> map = getProductsPlannedToProduce();
        return map.get(productId);
    }

    /**
     * Gets the products planned to be produced in this production run, with keys <code>productId</code> and values <code>quantity</code>.
     * These quantities includes the WEGS which validity date is in the future.
     * @return a <code>Map</code> of products being produced in this production run, with keys <code>productId</code> and values <code>quantity</code>
     * @exception GenericEntityException if an error occurs
     * @see #getProductsToProduce
     */
    public Map<String, BigDecimal> getProductsPlannedToProduce() throws GenericEntityException {
        if (productsPlannedToProduce == null) {
            productsPlannedToProduce = getWegsProductSums("PRUN_PROD_DELIV", false);
        }
        return productsPlannedToProduce;
    }

    /**
     * Gets the products actually produced in this run, with keys <code>productId</code> and values <code>quantity</code>.
     * @return a <code>Map</code> of products actually produced in this production run, with keys <code>productId</code> and values <code>quantity</code>
     * @exception GenericEntityException if an error occurs
     * @see #getProductsToProduce
     */
    public Map<String, BigDecimal> getProductsProduced() throws GenericEntityException {
        if (productsProduced != null) {
            return productsProduced;
        }
        productsProduced = getWegsProductSums("PRUN_PROD_PRODUCED");
        return productsProduced;
    }

    /**
     * Gets the quantity of the given product that was produced, or zero if it isn't being produced or has not been produced yet.  Note this is a primitive that cannot be null.
     * @param productId a <code>String</code> value
     * @return the quantity of the given product that was produced
     * @exception GenericEntityException if an error occurs
     */
    public BigDecimal getQuantityProduced(String productId) throws GenericEntityException {
        Map<String, BigDecimal> map = getProductsProduced();
        BigDecimal produced = map.get(productId);
        if (produced == null) {
            return BigDecimal.ZERO;
        } else {
            return produced;
        }
    }

    // helper method for sum of all active wegs (status is ignored)
    private Map<String, BigDecimal> getWegsProductSums(String workEffortGoodStdTypeId) throws GenericEntityException {
        return getWegsProductSums(workEffortGoodStdTypeId, true);
    }

    // helper method for sum of all active wegs (status is ignored)
    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> getWegsProductSums(String workEffortGoodStdTypeId, boolean filterFutureWegs) throws GenericEntityException {
        List<GenericValue> wegsList = productionRun.getRelatedByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortGoodStdTypeId", workEffortGoodStdTypeId));
        if (filterFutureWegs) {
            wegsList = EntityUtil.filterByDate(wegsList);
        }
        Map productSums = FastMap.newInstance();
        for (GenericValue produced : wegsList) {
            // still remove expired Wegs
            if (!filterFutureWegs && UtilValidate.isNotEmpty(produced.get("thruDate"))) {
                continue;
            }
            UtilMisc.addToBigDecimalInMap(productSums, produced.get("productId"), produced.getBigDecimal("estimatedQuantity"));
        }
        return productSums;
    }

    /**
     * Return the <code>Facility.ownerPartyId</code> from <code>WorkEffort.facilityId</code>, or null if there is no facilityId on <code>WorkEffort</code>.
     * @return the <code>ownerPartyId</code> <code>String</code> of the facility of this production run, or <code>null</code> if there is no facilityId on <code>WorkEffort</code>.
     * @throws GenericEntityException if an error occurs
     */
    public String getOwnerPartyId() throws GenericEntityException {
        GenericValue facility = productionRun.getRelatedOne("Facility");
        if (UtilValidate.isNotEmpty(facility)) {
            return facility.getString("ownerPartyId");
        } else {
            Debug.logError("Production run [" + productionRun.get("workEffortId") + "] has no facility, cannot find its owner", module);
            return null;
        }
    }

    /**
     * Checks if we can produce the production run.
     * The run must be completed and there must be products remaining to be produced.
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public boolean canProduce() throws GenericEntityException {
        if (!"PRUN_COMPLETED".equals(productionRun.get("currentStatusId"))) {
            // support early receiving of produced items if the product has standard costs
            if ("PRUN_RUNNING".equals(productionRun.get("currentStatusId")) && isAssembly() && productProduced != null) {
                try {
                    if (!organization.usesStandardCosting()) {
                        Debug.logInfo("Organization [" + organization.getPartyId() + "] is not using standard costing, not offering early receiving.", module);
                        return false;
                    }
                    BigDecimal stdCost = productProduced.getStandardCost(organization.getPartyAcctgPreference().getBaseCurrencyUomId());
                    Debug.logInfo("Found standard cost for produced product [" + productProduced.getProductId() + "] : " + stdCost, module);
                    return true;
                } catch (RepositoryException e) {
                    Debug.logError("Error while checking the product standard cost, not offering early receiving, " + e.getMessage(), module);
                    return false;
                }
            }
            return false;
        }
        Map<String, BigDecimal> toProduce = getProductsToProduce();
        Map<String, BigDecimal> produced = getProductsProduced();
        for (String productId : toProduce.keySet()) {
            BigDecimal quantityToProduce = toProduce.get(productId);
            if (quantityToProduce.signum() <= 0) {
                continue;
            }
            BigDecimal quantityProduced = produced.get(productId);
            if (quantityProduced == null || quantityToProduce.compareTo(quantityProduced) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the product for this production run as specified in the creation of it.  If this is an assembly, then the
     * product being assembled.  If this is a disassembly, then the product being disassembled.
     * @return a <code>GenericValue</code> value of the product for this production run
     * @exception GenericEntityException if an error occurs
     */
    public GenericValue getProductOfProductionRun() throws GenericEntityException {
        if (isAssembly()) {
            return getProductProduced();
        }
        GenericValue wegs = getDisassemblyWegs();
        if (wegs == null) {
            return null;
        }
        return wegs.getRelatedOne("Product");
    }

    /**
     * Gets the WorkEffortGoodStandard that stores what product is being disassembled and the quantity to disassemble.
     * @return a <code>GenericValue</code> value
     * @exception GenericEntityException if an error occurs
     */
    public GenericValue getDisassemblyWegs() throws GenericEntityException {
        Delegator delegator = productionRun.getDelegator();
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition("workEffortId", productionRun.get("workEffortId")),
            EntityCondition.makeCondition("workEffortGoodStdTypeId", "PRUN_PROD_DISASMBL")
        );
        GenericValue wegs = EntityUtil.getFirst(delegator.findByAnd("WorkEffortGoodStandard", conditions));
        if (wegs == null) {
            Debug.logWarning("No disassembly product found for reverse assembly [" + productionRun.get("productId") + "].", module);
        }
        return wegs;
    }

    /**
     * Convenience function to get a product by ID.
     * @param productId a <code>String</code> value
     * @return a <code>GenericValue</code> value
     * @exception GenericEntityException if an error occurs
     */
    public GenericValue getProduct(String productId) throws GenericEntityException {
        return productionRun.getDelegator().findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
    }

}
