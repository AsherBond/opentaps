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
package org.opentaps.tests.warehouse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.opentaps.tests.financials.FinancialsTestCase;

/**
 * Test Case for production runs.
 */
public class ProductionRunTestCase extends FinancialsTestCase {

    private static final String MODULE = ProductionRunTestCase.class.getName();

    /** UserLogin for running the tests. */
    public GenericValue demowarehouse1;

    /** Facility to operate in. */
    public final String facilityId = "WebStoreWarehouse";

    /** Company of the facility. */
    public final String organizationPartyId = "Company";

    // Default unit costs and quantities for manufacturing.

    protected final double comp1UnitCost = 9.0; // PRUNTEST_COMP1
    protected final double comp2UnitCost = 7.0; // PRUNTEST_COMP2
    protected final int comp1RequiredQty = 2;   // PRUNTEST_COMP1 required for 1 x PRUNTEST_PROD1
    protected final int comp2RequiredQty = 3;   // PRUNTEST_COMP2 required for 1 x PRUNTEST_PROD1

    protected final double matAUnitCost = 9.0; // MAT_A_COST
    protected final double matBUnitCost = 7.0; // MAT_B_COST
    protected final int matARequiredQty = 2;   // MAT_A_COST required for 1 x PRUNTEST_PROD1
    protected final int matBRequiredQty = 3;   // MAT_B_COST required for 1 x PRUNTEST_PROD1

    @Override
    public void setUp() throws Exception {
        super.setUp();
        demowarehouse1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
    }

    @Override
    public void tearDown() throws Exception {
        demowarehouse1 = null;
        super.tearDown();
    }

    /**
     * Helper method to assert that inventory items' and GL inventory values equal for the finished good and all parts.
     * @param inventoryAsserts an <code>InventoryAsserts</code> value
     */
    protected void assertAllInventoryValuesEqual(InventoryAsserts inventoryAsserts) {
        inventoryAsserts.assertInventoryValuesEqual("PROD_MANUF");
        inventoryAsserts.assertInventoryValuesEqual("MAT_A_COST");
        inventoryAsserts.assertInventoryValuesEqual("MAT_B_COST");
    }

    /**
     * Creates a dis-assembly production run for the given quantity of the given product and confirms it.
     * Assumes the products are received in the calling method.
     * @param name name of the production run
     * @param quantity quantity of product to disassemble
     * @param productId the product to disassemble
     * @return the production run ID
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected String createDisassembly(String name, int quantity, String productId) throws GenericServiceException {

        // create the disassembly and confirm it
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("disassemble", Boolean.TRUE);
        input.put("productId", productId);
        input.put("quantity", new BigDecimal(quantity));
        input.put("startDate", UtilDateTime.nowTimestamp());
        input.put("facilityId", facilityId);
        input.put("workEffortName", name);
        input.put("description", name + ". Reverse Assembly created for unit testing purpose.  Quantity to disassemble: " + productId + " * " + quantity);
        Map results = runAndAssertServiceSuccess("opentaps.createProductionRun", input);
        String productionRunId = (String) results.get("productionRunId");
        return confirmProductionRun(productionRunId);
    }

    /**
     * Creates a dis-assembly production run for the given quantity of the given product and confirms it.
     * Assumes the products are received in the calling method.
     * @param name name of the production run
     * @param quantity quantity of product to disassemble
     * @param productId the product to disassemble
     * @param routingId the specific routing to use, optional
     * @return the production run ID
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected String createDisassembly(String name, int quantity, String productId, String routingId) throws GenericServiceException {

        // create the disassembly and confirm it
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("disassemble", Boolean.TRUE);
        input.put("productId", productId);
        input.put("quantity", new BigDecimal(quantity));
        input.put("startDate", UtilDateTime.nowTimestamp());
        input.put("facilityId", facilityId);
        input.put("workEffortName", name);
        if (routingId != null) {
            input.put("routingId", routingId);
        }
        input.put("description", name + ". Reverse Assembly created for unit testing purpose.  Quantity to disassemble: " + productId + " * " + quantity);
        Map results = runAndAssertServiceSuccess("opentaps.createProductionRun", input);
        String productionRunId = (String) results.get("productionRunId");
        return confirmProductionRun(productionRunId);
    }

    /**
     * Creates a dis-assembly production run for the given quantity of GZ-MANUFACTURED.
     * @param name name of the production run
     * @param quantity quantity of PROD_COST to disassemble
     * @return the production run ID
     * @exception GenericServiceException if an error occurs
     */
    protected String createDisassemblyGzManufactured(String name, int quantity) throws GenericServiceException {

        // receive quantity GZ-MANUFACTURED into the webstore warehouse TODO why is it 2 * quantity?
        final double gzManufacturedUnitCost = 164.0;
        receiveMaterial("GZ-MANUFACTURED", 2 * quantity, gzManufacturedUnitCost);

        // create the disassembly and confirm it
        return createDisassembly(name, quantity, "GZ-MANUFACTURED");
    }

    /**
     * Creates a dis-assembly production run for the given quantity of PROD_COST.
     * @param name name of the production run
     * @param quantity quantity of PROD_COST to disassemble
     * @return the production run ID
     * @exception GenericServiceException if an error occurs
     */
    protected String createDisassemblyProdCost(String name, int quantity) throws GenericServiceException {

        // receive quantity PROD_COST into the warehouse TODO why is it 2 * quantity?
        final double prodCostUnitCost = 164.0;
        receiveMaterial("PROD_MANUF", 2 * quantity, prodCostUnitCost);

        // create the disassembly and confirm it
        return createDisassembly(name, quantity, "PROD_MANUF");
    }

    /**
     * Creates a fresh new production run for the given product with the given quantity (without confirming it).
     * Assumes the components are received in the calling method.
     * @param name name of the production run
     * @param quantity quantity to produce
     * @param productId product to produce
     * @return the production run ID
     * @exception GenericServiceException if an error occurs
     */
    protected String createProductionRun(String name, int quantity, String productId) throws GenericServiceException {
        return createProductionRun(name, quantity, productId, null);
    }

    /**
     * Creates a fresh new production run for the given product with the given quantity (without confirming it).
     * Assumes the components are received in the calling method.
     * @param name name of the production run
     * @param quantity quantity to produce
     * @param productId product to produce
     * @param routingId the specific routing to use, optional
     * @return the production run ID
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected String createProductionRun(String name, int quantity, String productId, String routingId) throws GenericServiceException {
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productId", productId);
        input.put("quantity", new BigDecimal(quantity));
        input.put("startDate", UtilDateTime.nowTimestamp());
        input.put("facilityId", facilityId);
        input.put("workEffortName", name);
        if (routingId != null) {
            input.put("routingId", routingId);
        }
        input.put("description", name + ". Production run created for unit testing purpose.  Quantity to produce: " + productId + " * " + quantity);
        Map results = runAndAssertServiceSuccess("opentaps.createProductionRun", input);
        return (String) results.get("productionRunId");
    }

    /**
     * Creates a fresh new production run for the given product with the given quantity
     * and confirms it.
     * Assumes the components are received in the calling method.
     * @param name name of the production run
     * @param quantity quantity to produce
     * @param productId product to produce
     * @return the production run ID
     * @exception GenericServiceException if an error occurs
     */
    protected String createProductionRunAndConfirm(String name, int quantity, String productId) throws GenericServiceException {
        String productionRunId = createProductionRun(name, quantity, productId);
        return confirmProductionRun(productionRunId);
    }

    /**
     * Creates a fresh new production run for the given product with the given quantity
     * and confirms it.
     * Assumes the components are received in the calling method.
     * @param name name of the production run
     * @param quantity quantity to produce
     * @param productId product to produce
     * @param routingId the specific routing to use, optional
     * @return the production run ID
     * @exception GenericServiceException if an error occurs
     */
    protected String createProductionRunAndConfirm(String name, int quantity, String productId, String routingId) throws GenericServiceException {
        String productionRunId = createProductionRun(name, quantity, productId, routingId);
        return confirmProductionRun(productionRunId);
    }

    /**
     * See below, plus add a step to confirm the production run.
     * @param name name of the production run
     * @param quantity quantity to produce
     * @param productIds is a List of the product IDs of the finished product and the raw materials.  The finished product's product ID will always be the first one in this list.
     * @return the productionRunId
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    protected String createProductionRunSimilarToGzManufacturedAndConfirm(String name, int quantity, List<String> productIds) throws GenericServiceException, GenericEntityException {
        String productionRunId = createProductionRunSimilarToGzManufactured(name, quantity, productIds);
        return confirmProductionRun(productionRunId);
    }

    /**
     * Creates a fresh new product with BOM and routing association based on GZ-MANUFACTURED with 6 component products
     * The productId of the finished product will be first in the List.
     * @param name name of the test, will be used to set the names of the products
     * @return the list of components product Id
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected List<String> createProductBOMAndRoutingSimilarToGzManufactured(String name) throws GenericEntityException {
        List productIds = new ArrayList();
        GenericValue finishedProduct = createTestProduct(name + " Finished Product", demowarehouse1);
        String finishedProductId = finishedProduct.getString("productId");
        productIds.add(finishedProductId);

        // associate the finished product with a routing, so that production runs for it can be created
        // this routing should have already been defined for GZ-MANUFACTURED
        associateProductWithRouting(finishedProductId, "P_GZ_MANUF");

        // create all the component materials and associate them as components of the finished product.
        // The BOM quantity is the same as that defined for GZ-MANUFACTURED and MAT-MANUFACTRED-*
        GenericValue productComp1 = createTestProduct(name + " Component Material 1", demowarehouse1);
        String productComp1Id = productComp1.getString("productId");
        createBOMProductAssoc(finishedProductId, productComp1Id, new Long("10"), new BigDecimal("2.0"), admin);
        productIds.add(productComp1Id);

        GenericValue productComp2 = createTestProduct(name + " Component Material 2", demowarehouse1);
        String productComp2Id = productComp2.getString("productId");
        createBOMProductAssoc(finishedProductId, productComp2Id, new Long("11"), new BigDecimal("3.0"), admin);
        productIds.add(productComp2Id);

        GenericValue productComp3 = createTestProduct(name + " Component Material 3", demowarehouse1);
        String productComp3Id = productComp3.getString("productId");
        createBOMProductAssoc(finishedProductId, productComp3Id, new Long("12"), new BigDecimal("4.0"), admin);
        productIds.add(productComp3Id);

        GenericValue productComp4 = createTestProduct(name + " Component Material 4", demowarehouse1);
        String productComp4Id = productComp4.getString("productId");
        createBOMProductAssoc(finishedProductId, productComp4Id, new Long("13"), new BigDecimal("1.0"), admin);
        productIds.add(productComp4Id);

        GenericValue productComp5 = createTestProduct(name + " Component Material 5", demowarehouse1);
        String productComp5Id = productComp5.getString("productId");
        createBOMProductAssoc(finishedProductId, productComp5Id, new Long("14"), new BigDecimal("6.0"), admin);
        productIds.add(productComp5Id);

        GenericValue productComp6 = createTestProduct(name + " Component Material 6", demowarehouse1);
        String productComp6Id = productComp6.getString("productId");
        createBOMProductAssoc(finishedProductId, productComp6Id, new Long("15"), new BigDecimal("5.0"), admin);
        productIds.add(productComp6Id);


        return productIds;
    }

    /**
     * Receives material for the production run, based on finished product and raw material product IDs in the List.
     * @param name name of the test, will be used to set the name of the production run
     * @param quantity quantity to produce
     * @param productIds is a List of the product IDs of the finished product and the raw materials.  See createProductBOMAndRoutingSimilarToGzManufactured
     * @return the productionRunId
     * @exception GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    protected String createProductionRunSimilarToGzManufactured(String name, int quantity, List<String> productIds) throws GenericServiceException, GenericEntityException {
        // get the productIds back from the List
        String finishedProductId = productIds.get(0);
        String productComp1Id = productIds.get(1);
        String productComp2Id = productIds.get(2);
        String productComp3Id = productIds.get(3);
        String productComp4Id = productIds.get(4);
        String productComp5Id = productIds.get(5);
        String productComp6Id = productIds.get(6);

        // receive the material components of quantity * GZ-MANUFACTURED into the webstore warehouse
        receiveMaterial(productComp1Id, 2 * quantity, 9.0);
        receiveMaterial(productComp2Id, 3 * quantity, 5.0);
        receiveMaterial(productComp3Id, 4 * quantity, 3.0);
        receiveMaterial(productComp4Id, 1 * quantity, 6.0);

        // this is for testing average costs.
        receiveMaterial(productComp5Id, 1 * quantity, 1.0);
        receiveMaterial(productComp5Id, 1 * quantity, 2.0);
        receiveMaterial(productComp5Id, 2 * quantity, 3.0);
        receiveMaterial(productComp5Id, 2 * quantity, 4.0);

        receiveMaterial(productComp6Id, 5 * quantity, 9.0);

        // create the production run
        return createProductionRun(name, quantity, finishedProductId);
    }

    /**
     * Creates a fresh norew production run for PRUNTEST_PROD1 with the given quantity
     * and confirms it.  Receives the correct amount of its components
     * for the run.
     * @param name name of the production run
     * @param quantity quantity of PROD_COST to produce
     * @return the production run ID
     * @throws GenericServiceException if an error occurs
     */
    protected String createProductionRunProd1(String name, int quantity) throws GenericServiceException {

        // receive the material components of quantity * PRUNTEST_PROD1 into the warehouse
        receiveMaterial("PRUNTEST_COMP1", comp1RequiredQty * quantity, comp1UnitCost);
        receiveMaterial("PRUNTEST_COMP2", comp2RequiredQty * quantity, comp2UnitCost);

        return createProductionRunAndConfirm(name, quantity, "PRUNTEST_PROD1");
    }

    /**
     * Creates a fresh new production run for PROD_COST with the given quantity
     * and confirms it.  Receives the correct amount of MAT_A_COST and MAT_B_COST
     * for the run.
     * @param name name of the production run
     * @param quantity quantity of PROD_COST to produce
     * @return the production run ID
     * @exception GenericServiceException if an error occurs
     */
    protected String createProductionRunProdCost(String name, int quantity) throws GenericServiceException {

        // receive the material components of quantity * PROD_COST into the warehouse
        receiveMaterial("MAT_A_COST", matARequiredQty * quantity, matAUnitCost);
        receiveMaterial("MAT_B_COST", matBRequiredQty * quantity, matBUnitCost);

        return createProductionRunAndConfirm(name, quantity, "PROD_MANUF");
    }

    /**
     * Confirms a production run.
     * Actually call the change status service to set the production run status to PRUN_DOC_PRINTED.
     * @param productionRunId production run to confirm
     * @return the production run ID
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected String confirmProductionRun(String productionRunId) throws GenericServiceException {
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("statusId", "PRUN_DOC_PRINTED");
        runAndAssertServiceSuccess("changeProductionRunStatus", input);

        return productionRunId;
    }

    /**
     * Receives material for use in production runs in the warehouse.
     *
     * @param productId the product to receive
     * @param quantity the quantity to receive
     * @param unitCost the unit cost of the received product in USD
     */
    @SuppressWarnings("unchecked")
    protected void receiveMaterial(String productId, int quantity, double unitCost) {
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        input.put("productId", productId);
        input.put("facilityId", facilityId);
        input.put("quantityAccepted", new BigDecimal(quantity));
        input.put("quantityRejected", new BigDecimal("0.0"));
        input.put("currencyUomId", "USD");
        input.put("unitCost", new BigDecimal(unitCost));
        runAndAssertServiceSuccess("receiveInventoryProduct", input);
    }

    /**
     * Starts the given task in the given production run.
     * @param productionRunId the production run ID
     * @param taskId the task ID
     */
    @SuppressWarnings("unchecked")
    protected void startTaskAndIssueInventory(String productionRunId, String taskId) {

        // start the task
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        // issue inventory required
        input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId);
        runAndAssertServiceSuccess("issueProductionRunTask", input);
    }

    /**
     * Asserts the given production run has only one task, and returns it.
     * @param productionRunId the production run ID
     * @return the task ID
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected String getOnlyTask(String productionRunId) throws Exception {
        List tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"));
        assertNotEmpty("Production run [" + productionRunId + "] has no routing tasks.  Cannot finish test.", tasks);
        assertEquals("Template for [" + productionRunId + "] has more than one task.  It should only have one task defined.", tasks.size(), 1);
        GenericValue task = EntityUtil.getFirst(tasks);
        String taskId = task.getString("workEffortId");
        return taskId;
    }

    /**
     * Asserts the given production run has only one task, and starts it.
     * @param productionRunId the production run ID
     * @return the task ID
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected String startOnlyTask(String productionRunId) throws Exception {
        String taskId = getOnlyTask(productionRunId);
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);
        Debug.logInfo("startOnlyTask: started task: " + taskId, MODULE);
        return taskId;
    }

    /**
     * Asserts the given work effort has exactly the specified WEGS.
     * @param workEffortId the work effort to test
     * @param wegsTypeId the type of WEGS to check
     * @param wegsQuantities the <code>Map</code> of productId: quantity that should be found
     * @throws GenericEntityException if an error occurs
     */
    protected void assertWorkEffortHasWegs(String workEffortId, String wegsTypeId, Map<String, BigDecimal> wegsQuantities) throws GenericEntityException {
        List<GenericValue> allWegs = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", workEffortId, "workEffortGoodStdTypeId", wegsTypeId));
        Set<String> productIds = wegsQuantities.keySet();
        for (String productId : productIds) {
            BigDecimal qty = wegsQuantities.get(productId);
            List<GenericValue> expectedWegs = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", workEffortId, "workEffortGoodStdTypeId", wegsTypeId, "productId", productId, "estimatedQuantity", qty));
            assertNotEmpty("Did not find expected WEGS for workEffort [" + workEffortId + "] product " + productId + " x " + qty + ", instead found WEGS: " + allWegs, expectedWegs);
        }
        List<GenericValue> unexpectedWegs = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toList(
                 EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId),
                 EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, wegsTypeId),
                 EntityCondition.makeCondition("productId", EntityOperator.NOT_IN, productIds)));
        assertEmpty("Found unexpected WEGS for workEffort [" + workEffortId + "]", unexpectedWegs);
    }

    /**
     * Associates a product with a routing, so that production runs for it can be created.
     * @param productId the product to associate
     * @param routingId is the work effort ID of a routing
     * @throws GenericEntityException if an error occurs
     */
    protected void associateProductWithRouting(String productId, String routingId) throws GenericEntityException {
        delegator.create("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", routingId, "productId", productId,
                "workEffortGoodStdTypeId", "ROU_PROD_TEMPLATE", "statusId", "WEGS_CREATED",
                "fromDate", UtilDateTime.nowTimestamp()));
    }

}
