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
package org.opentaps.tests.purchasing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Common methods for MRP tests.
 */
public class MrpTestCase extends OpentapsTestCase {

    private static final String MODULE = MrpTestCase.class.getName();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Creates a new <code>MrpTestCase</code> instance.
     */
    public MrpTestCase() {
        super();
    }

    /**
     * Creates a <code>SalesForecastItem</code> record.
     * @param productId
     * @param facilityId
     * @param forecastDatetime
     * @param quantity
     * @throws GeneralException
     */
    protected void createSalesForecastItem(String productId, String facilityId, Timestamp forecastDatetime, BigDecimal quantity) throws GeneralException {
        delegator.create("SalesForecastItem", UtilMisc.toMap("salesForecastItemId", delegator.getNextSeqId("SalesForecastItem"), "productId", productId, "facilityId", facilityId,
                                                             "forecastDatetime", forecastDatetime, "forecastQuantity", quantity));
    }

    /**
     * Create a <code>FacilityTransferPlan</code> record.
     * @param facilityIdFrom
     * @param facilityIdTo
     * @param scheduledTransferDatetime
     * @throws GeneralException
     */
    protected void createTransferPlan(String facilityIdFrom, String facilityIdTo, Timestamp scheduledTransferDatetime) throws GeneralException {
        delegator.create("FacilityTransferPlan", UtilMisc.toMap("facilityTransferPlanId", delegator.getNextSeqId("FacilityTransferPlan"), "facilityIdFrom", facilityIdFrom,
                                                                "facilityIdTo", facilityIdTo, "scheduledTransferDatetime", scheduledTransferDatetime));
    }

    @SuppressWarnings("unchecked")
    protected GenericValue createMrpProduct(String internalName, String productTypeId, Long billOfMaterialLevel, String facilityId, BigDecimal minimumStock, BigDecimal reorderQuantity, Long daysToShip, GenericValue userLogin) {
        // 1. create test product
        GenericValue product = createTestProduct(internalName, productTypeId, billOfMaterialLevel, userLogin);
        String productId = product.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(product, new BigDecimal("100.0"), userLogin);

        // 2. create a ProductFacility entry for this product with [minimumStock, reorderQuantity, daysToShip] (MRP needs this information to schedule proposed requirements)
        Map productFacilityContext = UtilMisc.toMap("userLogin", userLogin, "productId", productId, "facilityId", facilityId, "minimumStock", minimumStock, "reorderQuantity", reorderQuantity, "daysToShip", daysToShip);
        runAndAssertServiceSuccess("createProductFacility", productFacilityContext);

        return product;
    }

    /**
     * Creates <code>ProductFacility</code> with minimum stock and reorder quantity.
     * @param productId the product ID to configure for
     * @param facilityId the facility ID where it is configured
     * @param minimumStock the minimum stock below which it should reorder
     * @param reorderQuantity the quantity to reorder
     * @param userLogin a <code>UserLogin</code>
     */
    protected void createProductFacility(String productId, String facilityId, BigDecimal minimumStock, BigDecimal reorderQuantity, GenericValue userLogin) {
        createProductFacility(productId, facilityId, minimumStock, reorderQuantity, null, null, userLogin);
    }

    /**
     * Creates <code>ProductFacility</code> with minimum stock, reorder quantity and replenish method.
     * @param productId the product ID to configure for
     * @param facilityId the facility ID where it is configured
     * @param minimumStock the minimum stock below which it should reorder
     * @param reorderQuantity the quantity to reorder
     * @param replenishMethod the method to use when replenishing
     * @param userLogin a <code>UserLogin</code>
     */
    protected void createProductFacility(String productId, String facilityId, BigDecimal minimumStock, BigDecimal reorderQuantity, String replenishMethod, GenericValue userLogin) {
        createProductFacility(productId, facilityId, minimumStock, reorderQuantity, null, replenishMethod, userLogin);
    }

    /**
     * Creates <code>ProductFacility</code> with minimum stock, reorder quantity and replenish facility and method (only useful if the method is one of the SPECIF type).
     * @param productId the product ID to configure for
     * @param facilityId the facility ID where it is configured
     * @param minimumStock the minimum stock below which it should reorder
     * @param reorderQuantity the quantity to reorder
     * @param replenishFromFacilityId the backup warehouse to replenish the stock from
     * @param replenishMethod the method to use when replenishing
     * @param userLogin a <code>UserLogin</code>
     */
    @SuppressWarnings("unchecked")
    protected void createProductFacility(String productId, String facilityId, BigDecimal minimumStock, BigDecimal reorderQuantity, String replenishFromFacilityId, String replenishMethod, GenericValue userLogin) {
        Map productFacilityContext = UtilMisc.toMap("userLogin", userLogin, "productId", productId, "facilityId", facilityId, "minimumStock", minimumStock, "reorderQuantity", reorderQuantity, "daysToShip", new Long(1), "replenishFromFacilityId", replenishFromFacilityId, "replenishMethodEnumId", replenishMethod);
        runAndAssertServiceSuccess("createProductFacility", productFacilityContext);
    }

    /**
     * Uses the quantity < 0 case of {@link #assertRequirementExists} to verify that no requirements exist.
     * @param productId the product ID required
     * @param facilityId the facility ID for which the <code>Requirement</code> is to be found
     * @param requirementTypeId the <code>Requirement</code> type (eg: TRANSFER_REQUIREMENT, PRODUCT_REQUIREMENT, INTERNAL_REQUIREMENT)
     * @param statusId optional, status ID of the <code>Requirement</code> (eg: REQ_PROPOSED, REQ_APPROVED)
     * @exception GenericEntityException if an error occurs
     */
    protected void assertNoRequirementExists(String productId, String facilityId, String requirementTypeId, String statusId) throws GenericEntityException {
        assertNull("No requirements for [" + productId + "] in facility [" + facilityId + "] should exist in status [" + statusId + "]", assertRequirementExists(productId, facilityId, requirementTypeId, statusId, new BigDecimal("-1")));
    }

    /**
     * Method to check that one and only one Purchasing Requirement (PRODUCT_REQUIREMENT) of the productId, facilityId combination exists and has the quantity specified.
     * If quantity < 0 it will test if no requirements exist.  If this is the case a null will be returned.
     * This version is not conditioned on the time
     * Returns the requirementId
     * @param productId the product ID required
     * @param facilityId the facility ID for which the <code>Requirement</code> is to be found
     * @param quantity the product quantity in the <code>Requirement</code>, use <code>-1.0</code> for testing that no requirement exist
     * @return the requirement ID or <code>null</code> if we test that no requirement should exist
     * @throws GenericEntityException
     */
    protected String assertPurchasingRequirementExists(String productId, String facilityId, BigDecimal quantity) throws GenericEntityException {
        return assertRequirementExists(productId, facilityId, "PRODUCT_REQUIREMENT", null, quantity, null, null, null);
    }

    /**
     * Method to check that no Transfer Requirement (TRANSFER_REQUIREMENT) of the productId, facilityId combination exists.
     * This version is not conditioned on the time
     * @param productId the product ID required
     * @param fromFacilityId the origin facility ID for the tranfer
     * @param toFacilityId the origin facility ID for the tranfer
     * @throws GenericEntityException if an error occurs
     */
    protected void assertNoTransfertRequirementExists(String productId, String fromFacilityId, String toFacilityId) throws GenericEntityException {
        assertRequirementExists(productId, fromFacilityId, "TRANSFER_REQUIREMENT", null, new BigDecimal("-1.0"), null, null, toFacilityId);
    }

    /**
     * Method to check that one and only one Transfer Requirement (TRANSFER_REQUIREMENT) of the productId, facilityId combination exists and has the quantity specified.
     * If quantity < 0 it will test if no requirements exist.  If this is the case a null will be returned.
     * This version is not conditioned on the time
     * Returns the requirementId
     * @param productId the product ID required
     * @param fromFacilityId the origin facility ID for the tranfer
     * @param toFacilityId the destination facility ID for the transfer
     * @param quantity the product quantity in the <code>Requirement</code>, use <code>-1.0</code> for testing that no requirement exist
     * @return the requirement ID or <code>null</code> if we test that no requirement should exist
     * @throws GenericEntityException if an error occurs
     */
    protected String assertTransfertRequirementExists(String productId, String fromFacilityId, String toFacilityId, BigDecimal quantity) throws GenericEntityException {
        return assertRequirementExists(productId, fromFacilityId, "TRANSFER_REQUIREMENT", null, quantity, null, null, toFacilityId);
    }

    /**
     * Method to check that one and only one Requirements of the productId, facilityId, statusId combination exists and has the quantity specified.
     * If quantity < 0 it will test if no requirements exist.  If this is the case a null will be returned.
     * This version is not conditioned on the time
     * Returns the requirementId
     * @param productId the product ID required
     * @param facilityId the facility ID for which the <code>Requirement</code> is to be found
     * @param requirementTypeId the <code>Requirement</code> type (eg: TRANSFER_REQUIREMENT, PRODUCT_REQUIREMENT, INTERNAL_REQUIREMENT)
     * @param statusId optional, status ID of the <code>Requirement</code> (eg: REQ_PROPOSED, REQ_APPROVED)
     * @param quantity the product quantity in the <code>Requirement</code>, use <code>-1.0</code> for testing that no requirement exist
     * @return the requirement ID or <code>null</code> if we test that no requirement should exist
     * @throws GenericEntityException if an error occurs
     */
    protected String assertRequirementExists(String productId, String facilityId, String requirementTypeId, String statusId, BigDecimal quantity) throws GenericEntityException {
        return assertRequirementExists(productId, facilityId, requirementTypeId, statusId, quantity, null, null, null);
    }

    /**
     * Method to check that one and only one Requirements of the productId, facilityId, statusId combination exists and has the quantity specified.
     * If quantity < 0 it will test if no requirements exist.  If this is the case a null will be returned.
     * This version is not conditioned on the time
     * Returns the requirementId
     * @param productId the product ID required
     * @param facilityId the facility ID for which the <code>Requirement</code> is to be found
     * @param requirementTypeId the <code>Requirement</code> type (eg: TRANSFER_REQUIREMENT, PRODUCT_REQUIREMENT, INTERNAL_REQUIREMENT)
     * @param statusId optional, status ID of the <code>Requirement</code> (eg: REQ_PROPOSED, REQ_APPROVED)
     * @param quantity the product quantity in the <code>Requirement</code>, use <code>-1.0</code> for testing that no requirement exist
     * @param afterStartDate optional, a <code>Timestamp</code>
     * @param beforeStartDate optional, a <code>Timestamp</code>
     * @return the requirement ID or <code>null</code> if we test that no requirement should exist
     * @throws GenericEntityException if an error occurs
     */
    protected String assertRequirementExists(String productId, String facilityId, String requirementTypeId, String statusId, BigDecimal quantity, Timestamp afterStartDate, Timestamp beforeStartDate) throws GenericEntityException {
        return assertRequirementExists(productId, facilityId, requirementTypeId, statusId, quantity, afterStartDate, beforeStartDate, null);
    }

    /**
     * Method to check that one and only one Requirements of the productId, facilityId, statusId combination exists and has the quantity specified.
     * If quantity < 0 it will test if no requirements exist.  If this is the case a null will be returned.
     * This version checks that the requirement.requirementStartDate  be between afterStartDate and beforeStartDate
     * Returns the requirementId
     * @param productId the product ID required
     * @param facilityId the facility ID for which the <code>Requirement</code> is to be found
     * @param requirementTypeId the <code>Requirement</code> type (eg: TRANSFER_REQUIREMENT, PRODUCT_REQUIREMENT, INTERNAL_REQUIREMENT)
     * @param statusId optional, status ID of the <code>Requirement</code> (eg: REQ_PROPOSED, REQ_APPROVED)
     * @param quantity the product quantity in the <code>Requirement</code>, use <code>-1.0</code> for testing that no requirement exist
     * @param afterStartDate optional, a <code>Timestamp</code>
     * @param beforeStartDate optional, a <code>Timestamp</code>
     * @param toFacilityId optional, only used for transfer requirements, the destination facility ID
     * @return the requirement ID or <code>null</code> if we test that no requirement should exist
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected String assertRequirementExists(String productId, String facilityId, String requirementTypeId, String statusId, BigDecimal quantity, Timestamp afterStartDate, Timestamp beforeStartDate, String toFacilityId) throws GenericEntityException {
        List requirements = getRequirements(productId, facilityId, requirementTypeId, statusId, afterStartDate, beforeStartDate, toFacilityId);

        // if quantity < 0 then the assert will check that no requirements exist
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            assertTrue("No requirements for [" + productId + "] in facility [" + facilityId + "] should exist in status [" + statusId + "] between [" + afterStartDate + "] and [" + beforeStartDate + "] and there is [" + requirements.size() + "]", UtilValidate.isEmpty(requirements));
            return null;
        } else {
            // there should be only one
            Debug.logInfo("Requirements found " + requirements, MODULE);
            assertEquals("One new requirement was created for product [" + productId + "]", 1, requirements.size());
            GenericValue newProposedRequirement = (GenericValue) requirements.get(0);
            assertEquals("Correct quantity was created for in requirement [" + newProposedRequirement.getString("requirementId") + "] for product [" + productId + "] in status [" + statusId + "] between [" + afterStartDate + "] and [" + beforeStartDate + "]", quantity, newProposedRequirement.getBigDecimal("quantity"));

            return newProposedRequirement.getString("requirementId");
        }
    }

    /**
     * Method to check that some number of requirements exist with given product quantity, productId,
     * facilityId and statusId combination. We expect number of requirements equals to size of list of quantities.
     *
     * @param productId the product ID required
     * @param facilityId the facility ID for which the <code>Requirement</code> is to be found
     * @param requirementTypeId the <code>Requirement</code> type (eg: TRANSFER_REQUIREMENT, PRODUCT_REQUIREMENT, INTERNAL_REQUIREMENT)
     * @param statusId optional, status ID of the <code>Requirement</code> (eg: REQ_PROPOSED, REQ_APPROVED)
     * @param quantities list of the product quantities
     * @throws GenericEntityException
     */
    protected void assertRequirementExists(String productId, String facilityId, String requirementTypeId, String statusId, List<BigDecimal> quantities) throws GenericEntityException {
        List<GenericValue> requirements = getRequirements(productId, facilityId, requirementTypeId, statusId, null, null, null);

        Debug.logInfo("Requirements found " + requirements, MODULE);
        assertEquals("New requirements were created for product [" + productId + "]", quantities.size(), requirements.size());
        for (GenericValue req : requirements) {
            assertFalse("Requirement for product [" + productId + "] in status [" + statusId + "] for quantity [" + req.getBigDecimal("quantity").toString() + "] is wrong and should not be here", !assertNumberExistsInList(quantities, req.getBigDecimal("quantity")));
        }
    }

    /**
     * Method to check that no Requirements of the productId, facilityId, statusId and quantity.
     * Contrary to <code>assertRequirementExists</code> this lookup one requirement also matching the given quantity.
     * Returns the requirementId
     * @param productId the product ID required
     * @param facilityId the facility ID for which the <code>Requirement</code> is to be found
     * @param requirementTypeId the <code>Requirement</code> type (eg: TRANSFER_REQUIREMENT, PRODUCT_REQUIREMENT, INTERNAL_REQUIREMENT)
     * @param statusId optional, status ID of the <code>Requirement</code> (eg: REQ_PROPOSED, REQ_APPROVED)
     * @param quantity the product quantity in the <code>Requirement</code>
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected void assertNoRequirementExistsWithQuantity(String productId, String facilityId, String requirementTypeId, String statusId, BigDecimal quantity) throws GenericEntityException {
        List conditions = UtilMisc.toList(
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, requirementTypeId),
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId),
                        EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, quantity));
        List requirements = delegator.findByAnd("Requirement", conditions);
        assertEquals("Should not have found any Requirement with conditions: productId=" + productId + " facilityId=" + facilityId + " requirementTypeId=" + requirementTypeId + " statusId=" + statusId + " quantity=" + quantity, 0, requirements.size());
    }

    /**
     * Method to check that one and only one Requirements of the productId, facilityId, statusId and quantity.
     * Contrary to <code>assertRequirementExists</code> this lookup one requirement also matching the given quantity.
     * Returns the requirementId
     * @param productId the product ID required
     * @param facilityId the facility ID for which the <code>Requirement</code> is to be found
     * @param requirementTypeId the <code>Requirement</code> type (eg: TRANSFER_REQUIREMENT, PRODUCT_REQUIREMENT, INTERNAL_REQUIREMENT)
     * @param statusId optional, status ID of the <code>Requirement</code> (eg: REQ_PROPOSED, REQ_APPROVED)
     * @param quantity the product quantity in the <code>Requirement</code>
     * @return the requirement ID or <code>null</code> if we test that no requirement should exist
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected String assertRequirementExistsWithQuantity(String productId, String facilityId, String requirementTypeId, String statusId, BigDecimal quantity) throws GenericEntityException {
        List conditions = UtilMisc.toList(
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, requirementTypeId),
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId),
                        EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, quantity));
        GenericValue requirement = EntityUtil.getOnly(delegator.findByAnd("Requirement", conditions));
        assertNotNull("No Requirement found with conditions: productId=" + productId + " facilityId=" + facilityId + " requirementTypeId=" + requirementTypeId + " statusId=" + statusId + " quantity=" + quantity, requirement);
        return requirement.getString("requirementId");
    }

    /**
     * Check that the total of the  quantity of many Requirements during the time period add up to the desired quantity.
     * If desired quantity < 0, then it will test that no requirements exist during this time period.
     * @param productId
     * @param facilityId
     * @param requirementTypeId
     * @param statusId
     * @param quantity
     * @param afterStartDate
     * @param beforeStartDate
     * @throws GenericEntityException
     */
    @SuppressWarnings("unchecked")
    protected void assertRequirementsTotalQuantityCorrect(String productId, String facilityId, String requirementTypeId, String statusId, BigDecimal quantity,
                                             Timestamp afterStartDate, Timestamp beforeStartDate) throws GenericEntityException {
        List requirements = getRequirements(productId, facilityId, requirementTypeId, statusId, afterStartDate, beforeStartDate, null);

        // if quantity < 0 then the assert will check that no requirements exist
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            assertTrue("No requirements for [" + productId + "] in facility [" + facilityId + "] should exist in status [" + statusId + "] between [" + afterStartDate + "] and [" + beforeStartDate + "] and there is [" + requirements.size() + "]", UtilValidate.isEmpty(requirements));
        } else {
            // now need to check all the Requirements to see if their quantity add up
            assertTrue("Requirements for [" + productId + "] in facility [" + facilityId + "] should exist in status [" + statusId + "] between [" + afterStartDate + "] and [" + beforeStartDate + "] but none were found", UtilValidate.isNotEmpty(requirements));
            BigDecimal requirementsQuantity = BigDecimal.ZERO;
            for (Iterator it = requirements.iterator(); it.hasNext();) {
                GenericValue requirement = (GenericValue) it.next();
                if (requirement.get("quantity") != null) {
                    requirementsQuantity = requirementsQuantity.add(requirement.getBigDecimal("quantity"));
                }
            }

            requirementsQuantity.setScale(2, RoundingMode.HALF_UP); // this is kind of ugly but then what other rounding scenarios do we need?
            assertEquals("Correct total quantity of requirements for [" + productId + "] in facility [" + facilityId + "] in status [" + statusId + "] for product [" + productId + "] between [" + afterStartDate + "] and [" + beforeStartDate + "]", quantity, requirementsQuantity);
        }
    }

    /**
     * Get a list of Requirements satisfying the criteria.
     * @param productId the product ID required
     * @param facilityId the facility ID for which the <code>Requirement</code> is to be found
     * @param requirementTypeId the <code>Requirement</code> type (eg: TRANSFER_REQUIREMENT, PRODUCT_REQUIREMENT, INTERNAL_REQUIREMENT)
     * @param statusId optional, status ID of the <code>Requirement</code> (eg: REQ_PROPOSED, REQ_APPROVED)
     * @param afterStartDate optional, a <code>Timestamp</code>
     * @param beforeStartDate optional, a <code>Timestamp</code>
     * @param toFacilityId optional, only used for transfer requirements, the destination facility ID
     * @return the list of Requirements found matching the parameters
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private List getRequirements(String productId, String facilityId, String requirementTypeId, String statusId, Timestamp afterStartDate, Timestamp beforeStartDate, String toFacilityId) throws GenericEntityException {
        List conditions = UtilMisc.toList(
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, requirementTypeId),
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId));
        if (statusId != null) {
            conditions.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId));
        }
        if (toFacilityId != null) {
            conditions.add(EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, toFacilityId));
        }
        if (afterStartDate != null) {
            conditions.add(EntityCondition.makeCondition("requirementStartDate", EntityOperator.GREATER_THAN, afterStartDate));
        }
        if (beforeStartDate != null) {
            conditions.add(EntityCondition.makeCondition("requirementStartDate", EntityOperator.LESS_THAN, beforeStartDate));
        }
        return delegator.findByAnd("Requirement", conditions);
    }



    /**
     * Check the correct quantity was allocated to orderId from requirementId.
     * @param orderId a <code>String</code> value
     * @param requirementId a <code>String</code> value
     * @param quantity a <code>BigDecimal</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected void assertRequirementAssignedToOrder(String orderId, String requirementId, BigDecimal quantity) throws GenericEntityException {
        List conditions = UtilMisc.toList(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                EntityCondition.makeCondition("requirementId", EntityOperator.EQUALS, requirementId),
                EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, quantity));
            List orderRequirementCommitements = delegator.findByCondition("OrderRequirementCommitment", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, null);
            long orderRequirementAllocationCount = orderRequirementCommitements.size();
            assertEquals(String.format("Wrong number of proposed purchased order receipt (mrp inventory event) of requirement [%1$s] against order [%2$s]", requirementId, orderId), 1, orderRequirementAllocationCount);
    }

    /**
     * Check that the requirement is assigned to a supplier.
     * @param supplierPartyId a <code>String</code> value
     * @param requirementId a <code>String</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected void assertRequirementAssignedToSupplier(String supplierPartyId, String requirementId) throws GenericEntityException {
        List conditions = UtilMisc.toList(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, supplierPartyId),
                EntityCondition.makeCondition("requirementId", EntityOperator.EQUALS, requirementId),
                EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "SUPPLIER"),
                EntityUtil.getFilterByDateExpr());
            List supplierRoles = delegator.findByCondition("RequirementRole", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, null);
            assertTrue("Requirement " + requirementId + " is not assigned to supplier " + supplierPartyId, UtilValidate.isNotEmpty(supplierRoles));
    }

    /**
     * Checks that one inventory transfer in the requested quantity was created and returns the inventoryTransferId.
     * @param facilityIdFrom the transfer origin facility ID
     * @param facilityIdTo the transfer destination facility ID
     * @param productId the product ID of the transfer
     * @param quantity the quantity transferred
     * @return the <code>InventoryTransferAndItem</code> ID
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected String assertInventoryTransferRequested(String facilityIdFrom, String facilityIdTo, String productId, BigDecimal quantity) throws GenericEntityException {
        List invTransCond = UtilMisc.toList(EntityCondition.makeCondition("transferStatusId", EntityOperator.EQUALS, "IXF_REQUESTED"),
                            EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityIdFrom),
                            EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityIdTo),
                            EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                            EntityCondition.makeCondition("sendDate", EntityOperator.NOT_EQUAL, null),
                            EntityCondition.makeCondition("quantityOnHandTotal", EntityOperator.EQUALS, quantity));
        Debug.logInfo("assertInventoryTransferRequested facilityIdFrom : [" + facilityIdFrom
                + "], facilityIdTo : [" + facilityIdTo + "]"
                + "], productId : [" + productId + "]"
                + "], sendDate : [not null]"
                + "], quantityOnHandTotal : [" + quantity + "]"
                , MODULE);
        List<GenericValue> invTransfers = delegator.findByAnd("InventoryTransferAndItem",  invTransCond);
        assertEquals(String.format("Wrong number of requested inbound inventory transfers of product [%1$s]", productId), 1, invTransfers.size());
        return EntityUtil.getFirst(invTransfers).getString("inventoryTransferId");

    }
}
