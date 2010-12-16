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
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opensourcestrategies.financials.util.UtilCOGS;
import com.opensourcestrategies.financials.util.UtilFinancial;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.domain.manufacturing.OpentapsProductionRun;
import org.opentaps.domain.manufacturing.bom.BomTree;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.base.entities.InventoryItemTraceDetail;
import org.opentaps.domain.inventory.InventoryItem;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.tests.financials.FinancialAsserts;

/**
 * Tests for production run integrity, including postings to the GL.
 */
public class ProductionRunTests extends ProductionRunTestCase {

    private static final String MODULE = ProductionRunTests.class.getName();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests the transactions that occur when a production run for 2 PROD_COST is run.
     * Also ensures that 2 PROD_COST are added to inventory and that the inventory
     * values are balanced correctly.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testProductionRunTransactions() throws Exception {
        String productionRunId = createProductionRunProdCost("Production Run for Transaction Testing", 2);
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        pause("isolate the transaction that we will get after this point");

        // note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();

        // note the initial inventory quantities
        Map origProdCostInventory = inventoryAsserts.getInventory("PROD_MANUF");
        Map origMatACostInventory = inventoryAsserts.getInventory("MAT_A_COST");
        Map origMatBCostInventory = inventoryAsserts.getInventory("MAT_B_COST");
        assertAllInventoryValuesEqual(inventoryAsserts);

        // note the initial inventory values
        Map initInvValues = FastMap.newInstance();
        initInvValues.put("MAT_A_COST", inventoryAsserts.getInventoryValueForProduct("MAT_A_COST", start));
        initInvValues.put("MAT_B_COST", inventoryAsserts.getInventoryValueForProduct("MAT_B_COST", start));
        initInvValues.put("PROD_MANUF", inventoryAsserts.getInventoryValueForProduct("PROD_MANUF", start));

        // note the initial balances
        Map initialBalances = financialAsserts.getFinancialBalances(start);

        // get the one and only task
        String taskId = getOnlyTask(productionRunId);

        startTaskAndIssueInventory(productionRunId, taskId);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // complete the task, which should trigger production run completed
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // create and assign inventory for the products, which should trigger a posting
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        Map results = runAndAssertServiceSuccess("productionRunProduce", input);
        List<String> inventoryItemIds = (List<String>) results.get("inventoryItemIds");

        // get the transactions since starting
        Set<String> transactions = getAcctgTransSinceDate(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "MANUFACTURING_ATX"), start, delegator);
        assertNotEmpty("Production run transaction not created.", transactions);

        // assert transaction equivalence with the reference transaction
        assertTransactionEquivalence(transactions, UtilMisc.toList("MFGTEST-1", "MFGTEST-2", "MFGTEST-3"));

        // make sure the inventory quantity changes are correct_
        inventoryAsserts.assertInventoryChange("PROD_MANUF", new BigDecimal("2.0"), origProdCostInventory);
        inventoryAsserts.assertInventoryChange("MAT_A_COST", new BigDecimal("-4.0"), origMatACostInventory);
        inventoryAsserts.assertInventoryChange("MAT_B_COST", new BigDecimal("-6.0"), origMatBCostInventory);

        // make sure the ledger and inventory item costs are the same for all products involved
        assertAllInventoryValuesEqual(inventoryAsserts);

        // make sure every inventory item produced has the right unit cost
        for (String inventoryItemId : inventoryItemIds) {
            inventoryAsserts.assertInventoryItemUnitCost(inventoryItemId, new BigDecimal("164.00"), "USD");
        }

        // verify the expected cost change
        Map finalInvValues = FastMap.newInstance();
        finalInvValues.put("MAT_A_COST", inventoryAsserts.getInventoryValueForProduct("MAT_A_COST", UtilDateTime.nowTimestamp()));
        finalInvValues.put("MAT_B_COST", inventoryAsserts.getInventoryValueForProduct("MAT_B_COST", UtilDateTime.nowTimestamp()));
        finalInvValues.put("PROD_MANUF", inventoryAsserts.getInventoryValueForProduct("PROD_MANUF", UtilDateTime.nowTimestamp()));
        Map expectedInvValues = UtilMisc.toMap("MAT_A_COST", "-36", "MAT_B_COST", "-42", "PROD_MANUF", "328");
        assertMapDifferenceCorrect(initInvValues, finalInvValues, expectedInvValues);

        // verify that the difference in balances work out
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map expectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "328", "RAWMAT_INVENTORY", "-78", "MFG_EXPENSE_INTERNAL", "-250");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);
    }

    /**
     * Tests partial issuances to make sure they balance out and mark task completed when fulfilled.
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPartialIssuance() throws Exception {
        String productionRunId = createProductionRunProdCost("Production Run for Partial Issuance Testing", 2);
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        pause("isolate the transaction that we will get after this point");

        // note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();

        // note the initial inventory quantity and values
        Map originalProdCostInventory = inventoryAsserts.getInventory("PROD_MANUF");
        Map initInvValues = FastMap.newInstance();
        initInvValues.put("MAT_A_COST", inventoryAsserts.getInventoryValueForProduct("MAT_A_COST", start));
        initInvValues.put("MAT_B_COST", inventoryAsserts.getInventoryValueForProduct("MAT_B_COST", start));
        initInvValues.put("PROD_MANUF", inventoryAsserts.getInventoryValueForProduct("PROD_MANUF", start));
        assertAllInventoryValuesEqual(inventoryAsserts);

        // note the initial balances
        Map initialBalances = financialAsserts.getFinancialBalances(start);

        // get the one and only task and start it
        String taskId = startOnlyTask(productionRunId);

        // issue 3 of the 4 MAT_A_COST products
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", taskId);
        input.put("productId", "MAT_A_COST");
        input.put("quantity", new BigDecimal("3.0"));
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // verify it was issued properly by checking the WEGS (reduced by 3 to 1)
        List wegsList = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", taskId, "productId", "MAT_A_COST", "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"));
        assertTrue(wegsList.size() == 1);
        GenericValue wegs = EntityUtil.getFirst(wegsList);
        assertEquals("Partial issuance failed when issuing 3 MAT_A_COST out of 4.", wegs.getDouble("estimatedQuantity").doubleValue(), 1.0);

        // issue remainder
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", taskId);
        input.put("productId", "MAT_A_COST");
        input.put("quantity", new BigDecimal("1.0"));
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", taskId);
        input.put("productId", "MAT_B_COST");
        input.put("quantity", new BigDecimal("6.0"));
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", input);

        // make sure both WEGS are completed
        wegsList = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", taskId, "productId", "MAT_A_COST", "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"));
        assertTrue(wegsList.size() == 1);
        wegs = EntityUtil.getFirst(wegsList);
        assertEquals("Partial issuance failed:  MAT_A_COST WorkEffortGoodStandard was not marked completed.", wegs.getString("statusId"), "WEGS_COMPLETED");

        wegsList = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", taskId, "productId", "MAT_B_COST", "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"));
        assertTrue(wegsList.size() == 1);
        wegs = EntityUtil.getFirst(wegsList);
        assertEquals("Partial issuance failed:  MAT_B_COST WorkEffortGoodStandard was not marked completed.", wegs.getString("statusId"), "WEGS_COMPLETED");

        // finish off the production run (see previous test for what's going on here)
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        Map results = runAndAssertServiceSuccess("productionRunProduce", input);
        List<String> inventoryItemIds = (List<String>) results.get("inventoryItemIds");

        // get the transactions since starting
        Set<String> transactions = getAcctgTransSinceDate(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "MANUFACTURING_ATX"), start, delegator);
        assertNotEmpty("Production run transaction not created.", transactions);

        // assert transaction equivalence with the reference transaction
        assertTransactionEquivalence(transactions, UtilMisc.toList("MFGTEST-1", "MFGTEST-2", "MFGTEST-3"));

        // make sure the PROD_COST inventory went up by 2
        inventoryAsserts.assertInventoryChange("PROD_MANUF", new BigDecimal("2.0"), originalProdCostInventory);

        // make sure the ledger and inventory item costs are the same for all products involved
        assertAllInventoryValuesEqual(inventoryAsserts);

        // make sure every inventory item produced has the right unit cost
        for (String inventoryItemId : inventoryItemIds) {
            inventoryAsserts.assertInventoryItemUnitCost(inventoryItemId, new BigDecimal("164.00"), "USD");
        }

        // verify the expected cost change
        Map finalInvValues = FastMap.newInstance();
        finalInvValues.put("MAT_A_COST", inventoryAsserts.getInventoryValueForProduct("MAT_A_COST", UtilDateTime.nowTimestamp()));
        finalInvValues.put("MAT_B_COST", inventoryAsserts.getInventoryValueForProduct("MAT_B_COST", UtilDateTime.nowTimestamp()));
        finalInvValues.put("PROD_MANUF", inventoryAsserts.getInventoryValueForProduct("PROD_MANUF", UtilDateTime.nowTimestamp()));
        Map expectedInvValues = UtilMisc.toMap("MAT_A_COST", "-36", "MAT_B_COST", "-42", "PROD_MANUF", "328");
        assertMapDifferenceCorrect(initInvValues, finalInvValues, expectedInvValues);

        // verify that the difference in balances work out
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map expectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "328", "RAWMAT_INVENTORY", "-78", "MFG_EXPENSE_INTERNAL", "-250");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);
    }

    /**
     * Tests the disassembly of PROD_COST into MAT_A_COST and MAT_B_COST.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testDisassembly() throws Exception {
        String productionRunId = createDisassemblyProdCost("Test disassembly of 2 PROD_COST", 2);
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        pause("isolate the transaction that we will get after this point");

        // note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();

        // note the initial inventory quantities
        Map origProdCostInventory = inventoryAsserts.getInventory("PROD_MANUF");
        Map origMatACostInventory = inventoryAsserts.getInventory("MAT_A_COST");
        Map origMatBCostInventory = inventoryAsserts.getInventory("MAT_B_COST");
        assertAllInventoryValuesEqual(inventoryAsserts);

        // note the initial inventory values
        Map initInvValues = FastMap.newInstance();
        initInvValues.put("PROD_MANUF", inventoryAsserts.getInventoryValueForProduct("PROD_MANUF", start));
        initInvValues.put("MAT_A_COST", inventoryAsserts.getInventoryValueForProduct("MAT_A_COST", start));
        initInvValues.put("MAT_B_COST", inventoryAsserts.getInventoryValueForProduct("MAT_B_COST", start));

        // note the initial balances
        Map initialBalances = financialAsserts.getFinancialBalances(start);

        // get the one and only task and start it
        String taskId = startOnlyTask(productionRunId);

        pause("changed production run status (started)");

        assertAllInventoryValuesEqual(inventoryAsserts);

        // issue 2 of the PROD_COST
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", taskId);
        input.put("productId", "PROD_MANUF");
        input.put("quantity", new BigDecimal("2.0"));
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", input);

        pause("issued production run task component");

        assertAllInventoryValuesEqual(inventoryAsserts);

        // make sure WEGS is completed
        List wegsList = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", taskId, "productId", "PROD_MANUF", "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"));
        assertTrue("WEGS for material requirement of 2 PROD_COST exists.", wegsList.size() == 1);
        GenericValue wegs = EntityUtil.getFirst(wegsList);
        assertEquals("Material Issuance failed:  PROD_COST WorkEffortGoodStandard was not marked completed.", wegs.getString("statusId"), "WEGS_COMPLETED");

        // complete the task, which should trigger production run completed
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        pause("changed production run status");

        // produce MAT_A_COST and ensure that the inventory unit costs are correct
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", "MAT_A_COST");
        Map results = runAndAssertServiceSuccess("opentaps.productionRunProduce", input);
        List<String> inventoryItemIds = (List<String>) results.get("inventoryItemIds");

        pause("produced MAT_A_COST");

        for (String inventoryItemId : inventoryItemIds) {
            inventoryAsserts.assertInventoryItemUnitCost(inventoryItemId, new BigDecimal("9.00"), "USD");
        }
        assertAllInventoryValuesEqual(inventoryAsserts);

        // produce MAT_B_COST and ensure that the inventory unit costs are correct
        input.put("productId", "MAT_B_COST");

        pause("produced MAT_B_COST");

        results = runAndAssertServiceSuccess("opentaps.productionRunProduce", input);
        inventoryItemIds = (List<String>) results.get("inventoryItemIds");
        for (String inventoryItemId : inventoryItemIds) {
            inventoryAsserts.assertInventoryItemUnitCost(inventoryItemId, new BigDecimal("7.00"), "USD");
        }
        assertAllInventoryValuesEqual(inventoryAsserts);

        // ensure the inventory quantity changes are correct
        inventoryAsserts.assertInventoryChange("PROD_MANUF", new BigDecimal("-2.0"), origProdCostInventory);
        inventoryAsserts.assertInventoryChange("MAT_A_COST", new BigDecimal("4.0"), origMatACostInventory);
        inventoryAsserts.assertInventoryChange("MAT_B_COST", new BigDecimal("6.0"), origMatBCostInventory);

        // get the transactions since starting
        Set<String> transactions = getAcctgTransSinceDate(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "MANUFACTURING_ATX"), start, delegator);
        assertNotEmpty("Production run transaction not created.", transactions);

        // assert transaction equivalence with the reference transaction
        assertTransactionEquivalence(transactions, UtilMisc.toList("MFGTEST-4", "MFGTEST-5", "MFGTEST-6", "MFGTEST-7"));

        // make sure the ledger and inventory item costs are the same for all products involved
        assertAllInventoryValuesEqual(inventoryAsserts);

        // verify the expected cost change
        Map finalInvValues = FastMap.newInstance();
        finalInvValues.put("PROD_MANUF", inventoryAsserts.getInventoryValueForProduct("PROD_MANUF", UtilDateTime.nowTimestamp()));
        finalInvValues.put("MAT_A_COST", inventoryAsserts.getInventoryValueForProduct("MAT_A_COST", UtilDateTime.nowTimestamp()));
        finalInvValues.put("MAT_B_COST", inventoryAsserts.getInventoryValueForProduct("MAT_B_COST", UtilDateTime.nowTimestamp()));
        Map expectedInvValues = UtilMisc.toMap("MAT_A_COST", "36", "MAT_B_COST", "42", "PROD_MANUF", "-328");
        assertMapDifferenceCorrect(initInvValues, finalInvValues, expectedInvValues);

        // verify that the difference in balances work out
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        // Finished goods inventory should have declined by 328, raw materials increased by 78, and the difference debited (a plus) for Manufacturing Expense Variance
        Map expectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "-328", "MFG_EXPENSE_VARIANCE", "+250", "RAWMAT_INVENTORY", "+78");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);
    }

    /**
     * TODO: Describe <code>testProductionRunDeclaration</code> method here.
     *
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testProductionRunDeclaration() throws Exception {
        String productionRunId = createProductionRunProdCost("Production Run for Declaring Production Run Times", 1);
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // note the initial balances
        Map initialBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // get the one and only task, and starts it
        String taskId = startOnlyTask(productionRunId);

        // issue inventory required for the first (and only) task, so we can complete the run
        Map input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId);
        runAndAssertServiceSuccess("issueProductionRunTask", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // declare 1,000,000 of setup time and 800,000 of run time for the task
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("productionRunTaskId", taskId);
        input.put("partyId", demowarehouse1.get("partyId"));
        input.put("addSetupTime", new BigDecimal("1000000.0"));
        input.put("addTaskTime", new BigDecimal("800000.0"));
        runAndAssertServiceSuccess("updateProductionRunTask", input);

        // complete the task, which should trigger production run completed
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        // produce the products, which should trigger a posting
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", "PROD_MANUF");
        runAndAssertServiceSuccess("opentaps.productionRunProduce", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // get the final balances
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // this is the crux of the test
        Map expectedBalanceChanges = UtilMisc.toMap("MFG_EXPENSE_INTERNAL", "-350", "INVENTORY_ACCOUNT", "389", "RAWMAT_INVENTORY", "-39");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);
    }

    /**
     * Test issuing additional materials which are not defective to production runs
     * Create a production run for 1 PROD_COST.  Confirm it and issue its default materials.
     * Issue 1 additional MAT_A_COST and 1 additional MAT_B_COST
     * Complete production run
     * Verify that:
     *  1.  ATP/QOH of MAT_A_COST drops by 3, ATP and QOH of MAT_B_COST drops by 4, ATP/QOH of PROD_COST increases by 1
     *  2.  Unit cost of PROD_COST produced is $255 ($200 manufacturing time, 3*9 + 4*7 = $55 raw materials
     *  3.  Verify transaction entries for manufacturing are equivalent to MFGTEST-20, -21, -22, -23, -24, -25
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testIssueAdditionalMaterials() throws Exception {
        String productionRunId = createProductionRunProdCost("Production Run for Testing Issuing Additional Materials", 1);
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);

        // receive 1 more of each raw product for the additional item issuance
        receiveMaterial("MAT_A_COST", 1, matAUnitCost);
        receiveMaterial("MAT_B_COST", 1, matBUnitCost);

        // note the initial inventory quantities
        Map origProdCostInventory = inventoryAsserts.getInventory("PROD_MANUF");
        Map origMatACostInventory = inventoryAsserts.getInventory("MAT_A_COST");
        Map origMatBCostInventory = inventoryAsserts.getInventory("MAT_B_COST");

        pause("isolate the transaction that we will get after this point");

        // note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();

        // get the one and only task and start it
        String taskId = startOnlyTask(productionRunId);

        // issue inventory required for the first (and only) task, so we can complete the run
        Map input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId);
        runAndAssertServiceSuccess("issueProductionRunTask", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // issue additional material, which is the key of this test
        input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId, "productId", "MAT_A_COST", "quantity", new BigDecimal(1.0), "reasonEnumId", "IID_DEFECT");
        input.put("description", "Additional material issuance for unit test.");
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", input);
        input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId, "productId", "MAT_B_COST", "quantity", new BigDecimal(1.0), "reasonEnumId", "IID_DEFECT");
        input.put("description", "Additional material issuance for unit test.");
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // complete the task, which should trigger production run completed
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        // produce the products, which should trigger a posting
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", "PROD_MANUF");
        Map results = runAndAssertServiceSuccess("opentaps.productionRunProduce", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        List<String> inventoryItemIds = (List<String>) results.get("inventoryItemIds");

        // get the transactions since starting
        Set<String> transactions = getAcctgTransSinceDate(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "MANUFACTURING_ATX"), start, delegator);
        assertNotEmpty("Production run transaction not created.", transactions);

        // assert transaction equivalence with the reference transaction
        assertTransactionEquivalence(transactions, UtilMisc.toList("MFGTEST-20", "MFGTEST-21", "MFGTEST-22", "MFGTEST-23", "MFGTEST-24", "MFGTEST-25"));

        // verify that inventory changes include additional issuances
        inventoryAsserts.assertInventoryChange("PROD_MANUF", new BigDecimal("1.0"), origProdCostInventory);
        inventoryAsserts.assertInventoryChange("MAT_A_COST", new BigDecimal("-3.0"), origMatACostInventory);
        inventoryAsserts.assertInventoryChange("MAT_B_COST", new BigDecimal("-4.0"), origMatBCostInventory);

        // make sure every inventory item produced has the right unit cost
        for (String inventoryItemId : inventoryItemIds) {
            inventoryAsserts.assertInventoryItemUnitCost(inventoryItemId, new BigDecimal("255.00"), "USD");
        }
    }

    /**
     * Test early receiving of manufactured products.
     * Verify that if a product has standard costing it is possible to early receive (produce)
     *  from a running production run.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testProductionRunEarlyReceive() throws Exception {
        // set the organization to use standard costing
        GenericValue org = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", organizationPartyId));
        assertNotNull(org);
        org.set("costingMethodId", "STANDARD_COSTING");
        org.store();

        // Create a production run with createProductionRunProd1 and quantity 5
        String productionRunId = createProductionRunProd1("Production Run for Test Early Receive", 5);

        //  Note the initial balances
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Map initialBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // Check that OpentapsProductionRun.canProduce() returns false (since it is not running yet)
        OpentapsProductionRun opentapsProductionRun = new OpentapsProductionRun(productionRunId, dispatcher);
        assertFalse("OpentapsProductionRun should return false on canProduce() since it is not running yet.", opentapsProductionRun.canProduce());

        // Start the task
        String taskId = startOnlyTask(productionRunId);

        // Check that OpentapsProductionRun.canProduce() returns true
        opentapsProductionRun = new OpentapsProductionRun(productionRunId, dispatcher);
        assertTrue("OpentapsProductionRun should return true on canProduce() since it is running now.", opentapsProductionRun.canProduce());

        // Produce 1 unit of product
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", "PRUNTEST_PROD1");
        input.put("quantity", BigDecimal.ONE);
        Map result = runAndAssertServiceSuccess("opentaps.productionRunProduce", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // Verify the unit cost of the product is $194
        InventoryRepositoryInterface repo = getInventoryRepository(demowarehouse1);
        List<String> inventoryItemIds = (List<String>) result.get("inventoryItemIds");
        assertNotEmpty("Should have produced at least one inventory item.", inventoryItemIds);
        assertEquals("Should have produced exactly one inventory item.", inventoryItemIds.size(), 1);
        InventoryItem inventoryItem = repo.getInventoryItemById(inventoryItemIds.get(0));
        assertEquals("Unexpected PRUNTEST_PROD1 unitCost for the produced inventory item", inventoryItem.getUnitCost(), new BigDecimal("194"));

        // Issue inventory required
        input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId);
        runAndAssertServiceSuccess("issueProductionRunTask", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // Produce 2 more of the product
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", "PRUNTEST_PROD1");
        input.put("quantity", new BigDecimal("2"));
        pause("Workaround MYSQL Timestamp PK collision");
        result = runAndAssertServiceSuccess("opentaps.productionRunProduce", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // Verify the unit cost of the product is $194
        inventoryItemIds = (List<String>) result.get("inventoryItemIds");
        assertNotEmpty("Should have produced at least one inventory item.", inventoryItemIds);
        assertEquals("Should have produced exactly one inventory item.", inventoryItemIds.size(), 1);
        inventoryItem = repo.getInventoryItemById(inventoryItemIds.get(0));
        assertEquals("Unexpected PRUNTEST_PROD1 unitCost for the produced inventory item", inventoryItem.getUnitCost(), new BigDecimal("194"));

        // Declare 1,000,000 of setup time and 2,600,000 of run time for the task
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("productionRunTaskId", taskId);
        input.put("partyId", demowarehouse1.get("partyId"));
        input.put("addSetupTime", new BigDecimal("1000000.0"));
        input.put("addTaskTime",  new BigDecimal("2600000.0"));
        runAndAssertServiceSuccess("updateProductionRunTask", input);

        // Complete the task, which should trigger production run completed
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        // Produce 2 more of the product
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", "PRUNTEST_PROD1");
        input.put("quantity", new BigDecimal("2"));
        pause("Workaround MYSQL Timestamp PK collision");
        result = runAndAssertServiceSuccess("opentaps.productionRunProduce", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // Verify the unit cost of the product is $194
        inventoryItemIds = (List<String>) result.get("inventoryItemIds");
        assertNotEmpty("Should have produced at least one inventory item.", inventoryItemIds);
        assertEquals("Should have produced exactly one inventory item.", inventoryItemIds.size(), 1);
        inventoryItem = repo.getInventoryItemById(inventoryItemIds.get(0));
        assertEquals("Unexpected PRUNTEST_PROD1 unitCost for the produced inventory item", inventoryItem.getUnitCost(), new BigDecimal("194"));

        // Complete production run (close it)
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        runAndAssertServiceSuccess("changeProductionRunStatus", input);

        /* Verify the GL account balances are:
                MFG_EXPENSE_INTERNAL, -650,
                INVENTORY_ACCOUNT, 970,
                RAWMAT_INVENTORY", -195
                MFG_EXPENSE_VARIANCE, -125
                WIP_INVENTORY, 0
           These costs are also calculated with EXAMPLE_COST CostComponent (see below).
         */
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map expectedBalanceChanges = UtilMisc.toMap("MFG_EXPENSE_INTERNAL", "-650",
                                                    "INVENTORY_ACCOUNT", "970",
                                                    "RAWMAT_INVENTORY", "-195",
                                                    "MFG_EXPENSE_VARIANCE", "-125",
                                                    "WIP_INVENTORY", "0");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        printMapDifferences(initialBalances, finalBalances);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);

        // set PartyAcctgPreference.costingMethodId back to null
        org.set("costingMethodId", null);
        org.store();
    }

    /**
     * Test early receiving of manufactured products.
     * Verify that if a product do not have standard costing it is not possible to early receive (produce).
     * Same as testProductionRunEarlyReceive, but we use a product that do not have
     *  standard costs: GZ-MANUFACTURED using createProductionRunGzManufactured, and verify that canEarlyReceive() returns false.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testProductionRunEarlyReceiveWithoutStdCost() throws Exception {
        // 1. Create a production run with quantity 1
        String productionRunId = createProductionRunProd1("Production Run for Test Early Receive No Std Cost", 1);

        // 2. Check that OpentapsProductionRun.canProduce() returns false (since it is not running yet)
        OpentapsProductionRun opentapsProductionRun = new OpentapsProductionRun(productionRunId, dispatcher);
        assertFalse("OpentapsProductionRun should return false on canProduce() since it is not running yet.", opentapsProductionRun.canProduce());

        // 3. Start the task
        startOnlyTask(productionRunId);

        // 4. Check that OpentapsProductionRun.canProduce() still returns false (since no std cost)
        opentapsProductionRun = new OpentapsProductionRun(productionRunId, dispatcher);
        assertFalse("OpentapsProductionRun should return false on canProduce() since not using standard costs.", opentapsProductionRun.canProduce());

        // 5. Revert the production run (cannot cancel a running production run)
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        runAndAssertServiceSuccess("revertProductionRunAndSaveAllParts", input);
    }

    /**
     * Test standard costing for manufacturing.
     * Verify that if the organization uses standard costing, the unit costs of inventory
     *  produced are booked at standard rather than actual costs, and the difference is written off
     * 1. Set PartyAcctgPreference.costingMethodId=STANDARD_COSTING for Company
     * 2. Duplicate steps in testProductionRunDeclaration
     * 3. Verify that the unit cost of PROD_COST produced is $194
     * 4. Verify that the changes in GL account balances are:
     *   MFG_EXPENSE_INTERNAL, -350,
     *   INVENTORY_ACCOUNT, 194,
     *   RAWMAT_INVENTORY", -39
     *   MFG_EXPENSE_VARIANCE, 195
     *   WIP_INVENTORY, 0
     * 5. Set PartyAcctgPreference.costingMethodId=null again for Company
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testStandardCostingForManufacturing() throws Exception {
        // 1. Set PartyAcctgPreference.costingMethodId=STANDAR_COSTING for Company
        GenericValue org = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", organizationPartyId));
        assertNotNull(org);
        org.set("costingMethodId", "STANDARD_COSTING");
        org.store();

        // 2. Duplicate steps in testProductionRunDeclaration
        String productionRunId = createProductionRunProd1("Production Run for Standard Cost Testing", 1);
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // note the initial balances
        Map initialBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // get the one and only task and start it
        String taskId = startOnlyTask(productionRunId);

        // issue inventory required for the first (and only) task, so we can complete the run
        Map input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId);
        runAndAssertServiceSuccess("issueProductionRunTask", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // declare 1,000,000 of setup time and 800,000 of run time for the task
        // using the EXAMPLE_COST CostComponent defined in applications/ecommerce/data/DemoStandardCosting.xml, the cost is
        // Fixed $50 + Variable 1800000/60000*10 = 350
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("productionRunTaskId", taskId);
        input.put("partyId", demowarehouse1.get("partyId"));
        input.put("addSetupTime", new BigDecimal("1000000.0"));
        input.put("addTaskTime", new BigDecimal("800000.0"));
        runAndAssertServiceSuccess("updateProductionRunTask", input);

        // complete the task, which should trigger production run completed
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        // produce the products, which should trigger a posting
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", "PRUNTEST_PROD1");
        Map result = runAndAssertServiceSuccess("opentaps.productionRunProduce", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // complete production run (close it)
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        runAndAssertServiceSuccess("changeProductionRunStatus", input);

        // get the final balances
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // 3. Verify that the unit cost of PRUNTEST_PROD1 produced is $194
        InventoryRepositoryInterface repo = getInventoryRepository(demowarehouse1);
        List inventoryItemIds = (List) result.get("inventoryItemIds");
        assertNotEmpty("No Inventory Item was produced", inventoryItemIds);
        assertEquals("Unexpected number of Inventory Item produced", inventoryItemIds.size(), 1);

        String inventoryItemId = (String) inventoryItemIds.get(0);
        InventoryItem inventoryItem = repo.getInventoryItemById(inventoryItemId);
        assertEquals("Unexpected PRUNTEST_PROD1 unitCost for the produced inventory item", inventoryItem.getUnitCost(), new BigDecimal("194"));

        // 4. Verify GL account variance
        Map expectedBalanceChanges = UtilMisc.toMap("MFG_EXPENSE_INTERNAL", "-350",
                                                    "INVENTORY_ACCOUNT", "194",
                                                    "RAWMAT_INVENTORY", "-39",
                                                    "MFG_EXPENSE_VARIANCE", "195",
                                                    "WIP_INVENTORY", "0");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        printMapDifferences(initialBalances, finalBalances);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);

        // set PartyAcctgPreference.costingMethodId back to null
        org.set("costingMethodId", null);
        org.store();
    }

    /**
     * More complex test for production run under standard costing, including issuing additional materials and production run time declarations,
     * and a case of a production run coming under budget.
     * 1.  Set PartyAcctgPreference.costingMethodId = STANDARD_COSTING
     * 2.  Receive materials and create production run for 10 PRUNTEST_PROD1
     * 3.  Get initial gl account balances
     * 4.  Declare 2 000 000 milliseconds of setup time
     * 5.  Declare 2 500 000 milliseconds of run time
     * 6.  Receive 5 PRUNTEST_PROD1
     * 7.  Issue additional 3 PRUNTEST_COMP1 and 5 PRUNTEST_COMP2 to production run (see testIssueAdditionalMaterials above)
     * 8.  Add 1 500 000 milliseconds of run time
     * 9.  Receive 5 PRUNTEST_PROD1
     * 10. Complete production run
     * 11. Total actual cost:
     *  PRUNTEST_PROD1: 23 * 9 = 207
     *  PRUNTEST_PROD2: 35 * 7 = 245
     *  Runtime = 6 000 000 / 60 000 * 10 + 50 = 1050
     *  Total = 1502
     * 12. Verify accounting entries:
     *  MFG_EXPENSE_INTERNAL  -1050
     *  INVENTORY_ACCOUNT     1940
     *  RAWMAT_INVENTORY      -452
     *  MFG_EXPENSE_VARIANCE  -438
     *  WIP_INVENTORY           0
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testComplexStandardCostingProductionRun() throws Exception {
        // 1. Set PartyAcctgPreference.costingMethodId=STANDAR_COSTING for Company
        GenericValue org = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", organizationPartyId));
        assertNotNull(org);
        org.set("costingMethodId", "STANDARD_COSTING");
        org.store();

        // 2. Receive materials and create production run for 10 PRUNTEST_PROD1
        String productionRunId = createProductionRunProd1("Production Run for complex Standard Cost Testing", 10);
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // We're going to be issuing some additional materials, so receive them now
        receiveMaterial("PRUNTEST_COMP1", 3, comp1UnitCost);
        receiveMaterial("PRUNTEST_COMP2", 5, comp2UnitCost);

        // 3. Get initial gl account balances
        Map initialBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // get the one and only task and starts it
        String taskId = startOnlyTask(productionRunId);

        // issue inventory required for the first (and only) task, so we can complete the run
        Map input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId);
        runAndAssertServiceSuccess("issueProductionRunTask", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // 4. Declare 2 000 000 milliseconds of setup time
        // 5. Declare 2 500 000 milliseconds of run time
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("productionRunTaskId", taskId);
        input.put("partyId", demowarehouse1.get("partyId"));
        input.put("addSetupTime", new BigDecimal("2000000.0"));
        input.put("addTaskTime",  new BigDecimal("2500000.0"));
        runAndAssertServiceSuccess("updateProductionRunTask", input);

        // 6. Receive 5 PRUNTEST_PROD1
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("productionRunTaskId", taskId);
        input.put("partyId", demowarehouse1.get("partyId"));
        input.put("addQuantityProduced",  new BigDecimal("5.0"));
        runAndAssertServiceSuccess("updateProductionRunTask", input);

        // 7. Issue additional 3 PRUNTEST_COMP1 and 5 PRUNTEST_COMP2 to production run (see testIssueAdditionalMaterials above)
        input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId, "productId", "PRUNTEST_COMP1", "quantity", new BigDecimal("3.0"), "reasonEnumId", "IID_DEFECT");
        input.put("description", "Additional material issuance for unit test.");
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", input);
        input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId, "productId", "PRUNTEST_COMP2", "quantity", new BigDecimal("5.0"), "reasonEnumId", "IID_DEFECT");
        input.put("description", "Additional material issuance for unit test.");
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // 8. Add 1 500 000 milliseconds of run time
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("productionRunTaskId", taskId);
        input.put("partyId", demowarehouse1.get("partyId"));
        input.put("addTaskTime",  new BigDecimal("1500000.0"));
        runAndAssertServiceSuccess("updateProductionRunTask", input);

        // 9. Receive 5 PRUNTEST_PROD1
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("productionRunTaskId", taskId);
        input.put("partyId", demowarehouse1.get("partyId"));
        input.put("addQuantityProduced",  new BigDecimal("5.0"));
        runAndAssertServiceSuccess("updateProductionRunTask", input);

        // 10. Complete production run
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        // produce the products, which should trigger a posting
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", "PRUNTEST_PROD1");
        runAndAssertServiceSuccess("opentaps.productionRunProduce", input);
        assertAllInventoryValuesEqual(inventoryAsserts);

        // Close the production run so that the cost variances will be posted
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        runAndAssertServiceSuccess("changeProductionRunStatus", input);

        // 11. Total actual cost:
        //  PRUNTEST_PROD1: 23 * 9 = 207
        //  PRUNTEST_PROD2: 35 * 7 = 245
        //  Runtime = 6 000 000 / 60 000 * 10 + 50 = 1050
        //  Total = 1502
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        Map result = runAndAssertServiceSuccess("getProductionRunCost", input);
        BigDecimal totalCost = (BigDecimal) result.get("totalCost");
        Debug.logInfo("totalCost = " + totalCost, MODULE);

        // 12. Verify accounting entries:
        //  MFG_EXPENSE_INTERNAL  -1050
        //  INVENTORY_ACCOUNT     1940
        //  RAWMAT_INVENTORY      -452
        //  MFG_EXPENSE_VARIANCE  -438
        //  WIP_INVENTORY           0
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map expectedBalanceChanges = UtilMisc.toMap("MFG_EXPENSE_INTERNAL", "-1050",
                                                    "INVENTORY_ACCOUNT", "1940",
                                                    "RAWMAT_INVENTORY", "-452",
                                                    "MFG_EXPENSE_VARIANCE", "-438",
                                                    "WIP_INVENTORY", "0");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        printMapDifferences(initialBalances, finalBalances);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);

        // set PartyAcctgPreference.costingMethodId back to null
        org.set("costingMethodId", null);
        org.store();
    }

    /**
     * Test issuing additional materials which are defective
     * Receive 10 of MAT_A_COST at $9 and 10 of MAT_B_COST at $7
     * Create a production run for 1 PROD_COST.  Confirm it.
     * Issue 2 MAT_A_COST and 3 MAT_B_COST
     * Issue 1 additional MAT_A_COST and 1 additional MAT_B_COST.  Both issuances are to replace defective part (reason = IID_DEFECT)
     * Complete production run
     * Verify that:
     *  1.  ATP/QOH of MAT_A_COST is now 7, ATP and QOH of MAT_B_COST is now 6, ATP/QOH of PROD_COST is 1
     *  2.  Unit cost of PROD_COST produced is $239 ($200 manufacturing time, 2*9 + 3*7 = $39 raw materials
     */

    /**
     * Tests average cost adjustment to production run costs.
     * IMPORTANT NOTE : This test uses the MAT_A_COST, MAT_B_COST unit cost values from previous tests as the reference product average cost.
     * If the unit costs from previous tests change the average costs for this test will have to change accordingly.
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testAddAvgCostAdjToProductionRunCosts() throws Exception {
        BigDecimal expectedAvgCost = null;
        BigDecimal calculatedAvgCost = null;

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        pause("product average calculation");

        // receive the material components of quantity * PROD_COST into the webstore warehouse
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        input.put("productId", "MAT_A_COST");
        input.put("facilityId", facilityId);
        input.put("quantityAccepted", new BigDecimal("2.0"));
        input.put("quantityRejected", new BigDecimal("0.0"));
        input.put("currencyUomId", "USD");
        input.put("unitCost", new BigDecimal("5.0"));
        runAndAssertServiceSuccess("receiveInventoryProduct", input);

        pause("product average calculation");

        input.put("unitCost", new BigDecimal("13.0"));
        runAndAssertServiceSuccess("receiveInventoryProduct", input);

        // Check MAT_A_COST productAverageCost is now of 9.0
        calculatedAvgCost = UtilCOGS.getProductAverageCost("MAT_A_COST", organizationPartyId, demowarehouse1, delegator, dispatcher);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("9.0");
        assertEquals("MAT_A_COST average cost is different from the expected value.", expectedAvgCost.doubleValue(), calculatedAvgCost.doubleValue(), new Double("0.009"));

        pause("product average calculation");

        input.put("productId", "MAT_B_COST");
        input.put("quantityAccepted", new BigDecimal("3.0"));
        input.put("unitCost", new BigDecimal("3.0"));
        runAndAssertServiceSuccess("receiveInventoryProduct", input);

        pause("product average calculation");

        input.put("unitCost", new BigDecimal("11.0"));
        runAndAssertServiceSuccess("receiveInventoryProduct", input);

        // Check MAT_B_COST productAverageCost is now of 7.0
        calculatedAvgCost = UtilCOGS.getProductAverageCost("MAT_B_COST", organizationPartyId, demowarehouse1, delegator, dispatcher);
        calculatedAvgCost = calculatedAvgCost.setScale(DECIMALS, ROUNDING);
        expectedAvgCost = new BigDecimal("7.0");
        assertEquals("MAT_B_COST average cost is different from the expected value.", expectedAvgCost.doubleValue(), calculatedAvgCost.doubleValue(), new Double("0.009"));

        // create the production run and confirm it
        String name = "Production run to test average cost adjustment to production run costs.";
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productId", "PROD_MANUF");
        input.put("quantity", new BigDecimal("2.0"));
        input.put("startDate", UtilDateTime.nowTimestamp());
        input.put("facilityId", facilityId);
        input.put("workEffortName", name);
        input.put("description", name + ". Production run created for unit testing purpose.  Quantity to produce: PROD_COST * 1");
        Map results = runAndAssertServiceSuccess("opentaps.createProductionRun", input);
        String productionRunId = (String) results.get("productionRunId");

        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("statusId", "PRUN_DOC_PRINTED");
        runAndAssertServiceSuccess("changeProductionRunStatus", input);

        // note the initial inventory quantities
        Map origProdCostInventory = inventoryAsserts.getInventory("PROD_MANUF");
        Map origMatACostInventory = inventoryAsserts.getInventory("MAT_A_COST");
        Map origMatBCostInventory = inventoryAsserts.getInventory("MAT_B_COST");
        assertAllInventoryValuesEqual(inventoryAsserts);

        pause("isolate the transaction that we will get after this point");

        // note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();

        // note the initial inventory values
        Map initInvValues = FastMap.newInstance();
        initInvValues.put("MAT_A_COST", inventoryAsserts.getInventoryValueForProduct("MAT_A_COST", start));
        initInvValues.put("MAT_B_COST", inventoryAsserts.getInventoryValueForProduct("MAT_B_COST", start));
        initInvValues.put("PROD_MANUF", inventoryAsserts.getInventoryValueForProduct("PROD_MANUF", start));

        // note the initial balances
        Map initialBalances = financialAsserts.getFinancialBalances(start);

        // get the one and only task and starts it
        String taskId = startOnlyTask(productionRunId);

        pause("changed production run status");

        // issue inventory required for the first (and only) task, so we can complete the run
        input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId);
        runAndAssertServiceSuccess("issueProductionRunTask", input);

        pause("issued production run task");

        assertAllInventoryValuesEqual(inventoryAsserts);

        // complete the task, which should trigger production run completed
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        pause("changed production run status");

        assertAllInventoryValuesEqual(inventoryAsserts);

        // create and assign inventory for the products, which should trigger a posting
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", "PROD_MANUF");
        results = runAndAssertServiceSuccess("opentaps.productionRunProduce", input);

        pause("ran production");

        List<String> inventoryItemIds = (List) results.get("inventoryItemIds");

        // make sure the inventory quantity changes are correct_
        inventoryAsserts.assertInventoryChange("PROD_MANUF", new BigDecimal("2.0"), origProdCostInventory);
        inventoryAsserts.assertInventoryChange("MAT_A_COST", new BigDecimal("-4.0"), origMatACostInventory);
        inventoryAsserts.assertInventoryChange("MAT_B_COST", new BigDecimal("-6.0"), origMatBCostInventory);

        // make sure the ledger and inventory item costs are the same for all products involved
        assertAllInventoryValuesEqual(inventoryAsserts);

        // make sure every inventory item produced has the right unit cost
        for (String inventoryItemId : inventoryItemIds) {
            inventoryAsserts.assertInventoryItemUnitCost(inventoryItemId, new BigDecimal("164.00"), "USD");
        }

        // verify the expected cost change
        Map finalInvValues = FastMap.newInstance();
        finalInvValues.put("MAT_A_COST", inventoryAsserts.getInventoryValueForProduct("MAT_A_COST", UtilDateTime.nowTimestamp()));
        finalInvValues.put("MAT_B_COST", inventoryAsserts.getInventoryValueForProduct("MAT_B_COST", UtilDateTime.nowTimestamp()));
        finalInvValues.put("PROD_MANUF", inventoryAsserts.getInventoryValueForProduct("PROD_MANUF", UtilDateTime.nowTimestamp()));

        Map expectedInvValues = null;
        expectedInvValues = UtilMisc.toMap("MAT_A_COST", "-36", "MAT_B_COST", "-42", "PROD_MANUF", "328");
        assertMapDifferenceCorrect(initInvValues, finalInvValues, expectedInvValues);

        // verify that the difference in balances work out
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map expectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "328", "RAWMAT_INVENTORY", "-78", "MFG_EXPENSE_INTERNAL", "-250");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);
    }

    /**
     * Create a production run for 5 x GZ-MANUFACTURED and try to revert it from status PRUN_CREATED. Should fail.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testRevertProductionInCreatedStatusFails() throws GeneralException {
        List productIds = createProductBOMAndRoutingSimilarToGzManufactured("To be Revert from status PRUN_CREATED, should fail");
        String productionRunId = createProductionRunSimilarToGzManufactured("To be Revert from status PRUN_CREATED, should fail", 5, productIds);

        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        runAndAssertServiceError("revertProductionRunAndSaveAllParts", input);
    }

    /**
     * Create a production run for GZ-MANUFACTURED, confirm it, issue everything, complete it and try to revert it. Should fail.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testRevertProductionInCompletedStatusFails() throws GeneralException {
        /**
         * 2. Create a production run for 3 x GZ-MANUFACTURED, confirm it, issue everything, complete it and try to revert it. Should fail.
         */
        List productIds = createProductBOMAndRoutingSimilarToGzManufactured("To be Revert after ending, should fail");
        String productionRunId = createProductionRunSimilarToGzManufacturedAndConfirm("To be Revert after ending, should fail", 3, productIds);
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);

        // get the two tasks
        List<GenericValue> tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"), UtilMisc.toList("+workEffortId"));
        assertNotEmpty("Production run created for GZ-MANUFACTURED has no routing tasks. Cannot finish test.", tasks);
        assertEquals("Template for GZ-MANUFACTURED has two tasks.", tasks.size(), 2);

        for (GenericValue task : tasks) {
            String taskId = task.getString("workEffortId");

            startTaskAndIssueInventory(productionRunId, taskId);

            // complete the task, which should trigger production run completed
            input = UtilMisc.toMap("userLogin", demowarehouse1);
            input.put("productionRunId", productionRunId);
            input.put("workEffortId", taskId);
            runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);
        }

        // create and assign inventory for the products, which should trigger a posting
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", productIds.get(0));  // the first product ID is that of the finished product
        runAndAssertServiceSuccess("opentaps.productionRunProduce", input);

        // Revert the production run
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        runAndAssertServiceError("revertProductionRunAndSaveAllParts", input);
    }

    /**
     * Convenience method to create a Map of productId -> expectedValue .
     * @param productIds a list of product ID
     * @param expectedValue the expected value to put in the map for each product ID
     * @return the map of product ID -> expected value
     */
    private Map<String, String> makeProductIdValueMap(List<String> productIds, String expectedValue) {
        Map<String, String> expectedValues = FastMap.newInstance();
        for (int i = 0; i < productIds.size(); i++) {
            expectedValues.put(productIds.get(i), expectedValue);
        }
        return expectedValues;
    }

    /**
     * 3. Create a production run for GZ-MANUFACTURED, start the first task, issue all, then revert it. Verify PRUN_REVERTED.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testRevertingProductionRunAfterFirstTask() throws GeneralException {
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String rawmatInventory = UtilFinancial.getOrgGlAccountId(organizationPartyId, "RAWMAT_INVENTORY", delegator);
        String wipInventory = UtilFinancial.getOrgGlAccountId(organizationPartyId, "WIP_INVENTORY", delegator);
        String mfgExpenseInternal = UtilFinancial.getOrgGlAccountId(organizationPartyId, "MFG_EXPENSE_INTERNAL", delegator);
        String mfgExpenseRevprun = UtilFinancial.getOrgGlAccountId(organizationPartyId, "MFG_EXPENSE_REVPRUN", delegator);

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);

        /**
         * 3. Create a production run for 3 x GZ-MANUFACTURED, start the first task, issue all, then revert it. Verify PRUN_REVERTED.
         */
        // Get the RAWMAT_INVENTORY, WIP_INVENTORY, MFG_EXPENSE_INTERNAL, MFG_EXPENSE_REVPRUN gl account balances before start of production run
        Map<String, Number> initialBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // set up original inventory values and average costs
        List<String> productIds = createProductBOMAndRoutingSimilarToGzManufactured("To be Revert after issue all of the first task, should success");
        Map[] origInventory = new Map[productIds.size()];
        Map initInvValues = null;
        Timestamp start = null;
        int i = 0;

        BigDecimal[] initAvgCost = new BigDecimal[productIds.size()];
        Map expectedInvValues = makeProductIdValueMap(productIds, "0");

        String productionRunId = createProductionRunSimilarToGzManufacturedAndConfirm("To be Revert after issue all of the first task, should success", 3, productIds);
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);

        String finishedProductId = productIds.get(0);  // the first product ID in the list is that of the finished product

        // get the two tasks
        List<GenericValue> tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"), UtilMisc.toList("+workEffortId"));
        assertNotEmpty("Production run created for [" + finishedProductId + "] has no routing tasks. Cannot finish test.", tasks);
        assertEquals("Template for [" + finishedProductId + "] has two tasks.", tasks.size(), 2);

        // note the time so we can easily find the transaction
        start = UtilDateTime.nowTimestamp();

        // note the initial inventory quantities
        for (i = 0; i < productIds.size(); i++) {
            origInventory[i] = inventoryAsserts.getInventory(productIds.get(i));
        }

        // note the initial inventory values
        initInvValues = FastMap.newInstance();
        for (i = 0; i < productIds.size(); i++) {
            initInvValues.put(productIds.get(i), inventoryAsserts.getInventoryValueForProduct(productIds.get(i), start));
        }

        // get the average costs of all the products
        for (i = 0; i < productIds.size(); i++) {
            initAvgCost[i] = UtilCOGS.getProductAverageCost(productIds.get(i), organizationPartyId, demowarehouse1, delegator, dispatcher);
        }

        GenericValue task1 = EntityUtil.getFirst(tasks);
        String taskId = task1.getString("workEffortId");

        startTaskAndIssueInventory(productionRunId, taskId);

        // Get the RAWMAT_INVENTORY, WIP_INVENTORY, MFG_EXPENSE_INTERNAL, MFG_EXPENSE_REVPRUN gl account balances before production run is reverted
        Map<String, Number> beforeRevertBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // verify that change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_INTERNAL) = 0
        List initialList = UtilMisc.toList(initialBalances.get(wipInventory), initialBalances.get(rawmatInventory), initialBalances.get(mfgExpenseInternal));
        List beforeRevertList = UtilMisc.toList(beforeRevertBalances.get(wipInventory), beforeRevertBalances.get(rawmatInventory), beforeRevertBalances.get(mfgExpenseInternal));
        assertDifferenceCorrect("change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_INTERNAL)", initialList, beforeRevertList, BigDecimal.ZERO);

        // Revert the production run
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        runAndAssertServiceSuccess("revertProductionRunAndSaveAllParts", input);

        // verify status of production run header is reverted
        GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
        assertEquals("Production Run [" + productionRunId + "] has wrong status.", "PRUN_REVERTED", productionRun.getString("currentStatusId"));
        // verify that the first task is completed but the second task is cancelled
        tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"), UtilMisc.toList("+workEffortId"));
        task1 = tasks.get(0);
        taskId = task1.getString("workEffortId");
        assertEquals("Production Run [" + productionRunId + "] task [" + taskId + "] has wrong status.", "PRUN_REVERTED", task1.getString("currentStatusId"));
        GenericValue task2 = tasks.get(1);
        taskId = task2.getString("workEffortId");
        assertEquals("Production Run [" + productionRunId + "] task [" + taskId + "] has wrong status.", "PRUN_CANCELLED", task2.getString("currentStatusId"));

        // make sure the inventory quantity changes are correct
        for (i = 0; i < productIds.size(); i++) {
            inventoryAsserts.assertInventoryChange(productIds.get(i), new BigDecimal("0.0"), origInventory[i]);
        }

        // verify that inventory values have not changed
        Map<String, BigDecimal> finalInvValues = FastMap.newInstance();
        for (i = 0; i < productIds.size(); i++) {
            finalInvValues.put(productIds.get(i), inventoryAsserts.getInventoryValueForProduct(productIds.get(i), UtilDateTime.nowTimestamp()));
        }
        assertMapDifferenceCorrect(initInvValues, finalInvValues, expectedInvValues);

        // verify all the average costs have not changed
        for (i = 0; i < productIds.size(); i++) {
            BigDecimal avgCost = UtilCOGS.getProductAverageCost(productIds.get(i), organizationPartyId, demowarehouse1, delegator, dispatcher);
            assertEquals("Production Run [" + productionRunId + "] for product [" + productIds.get(i) + "] get a modified average cost.", initAvgCost[i], avgCost);
        }

        // Get the RAWMAT_INVENTORY, WIP_INVENTORY, MFG_EXPENSE_INTERNAL, MFG_EXPENSE_REVPRUN gl account balances after production run is reverted
        Map<String, Number> finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // verify that from before start of production run to after the production run is reverted:
        //   change(WIP_INVENTORY) = 0
        //   change(RAWMAT_INVENTORY) = 0
        Map expectedBalanceChanges = UtilMisc.toMap("WIP_INVENTORY", "0", "RAWMAT_INVENTORY", "0");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);

        //   change(MFG_EXPENSE_INTERNAL) + change(MFG_EXPENSE_REVPRUN) = 0
        initialList = UtilMisc.toList(initialBalances.get(mfgExpenseInternal), initialBalances.get(mfgExpenseRevprun));
        List finalList = UtilMisc.toList(finalBalances.get(mfgExpenseInternal), finalBalances.get(mfgExpenseRevprun));
        assertDifferenceCorrect("change(MFG_EXPENSE_INTERNAL) + change(MFG_EXPENSE_REVPRUN)", initialList, finalList, BigDecimal.ZERO);

        // verify that from before production run is reverted to after the production run is reverted:
        //   change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_REVPRUN) = 0
        beforeRevertList = UtilMisc.toList(beforeRevertBalances.get(wipInventory), beforeRevertBalances.get(rawmatInventory), beforeRevertBalances.get(mfgExpenseInternal));
        finalList = UtilMisc.toList(finalBalances.get(wipInventory), finalBalances.get(rawmatInventory), finalBalances.get(mfgExpenseInternal));
        assertDifferenceCorrect("change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_REVPRUN)", beforeRevertList, finalList, BigDecimal.ZERO);
    }

    /**
     * Create a production run for GZ-MANUFACTURED, issue the first and second task, then revert it. Verify PRUN_REVERTED.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testRevertingProductionRunAfterSecondTask() throws GeneralException {
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String rawmatInventory = UtilFinancial.getOrgGlAccountId(organizationPartyId, "RAWMAT_INVENTORY", delegator);
        String wipInventory = UtilFinancial.getOrgGlAccountId(organizationPartyId, "WIP_INVENTORY", delegator);
        String mfgExpenseInternal = UtilFinancial.getOrgGlAccountId(organizationPartyId, "MFG_EXPENSE_INTERNAL", delegator);
        String mfgExpenseRevprun = UtilFinancial.getOrgGlAccountId(organizationPartyId, "MFG_EXPENSE_REVPRUN", delegator);

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);

        // set up original inventory values and average costs
        List<String> productIds = createProductBOMAndRoutingSimilarToGzManufactured("To be Revert after issue all of the first and second task, should success");
        String finishedProductId = productIds.get(0);  // the first product ID in the list is that of the finished product

        Map[] origInventory = new Map[productIds.size()];
        Map initInvValues = null;
        Timestamp start = null;
        int i = 0;

        BigDecimal[] initAvgCost = new BigDecimal[productIds.size()];
        Map expectedInvValues = makeProductIdValueMap(productIds, "0");

        /**
         * 4. Create a production run for GZ-MANUFACTURED, issue the first and second task, then revert it. Verify PRUN_REVERTED.
         */
        // Get the RAWMAT_INVENTORY, WIP_INVENTORY, MFG_EXPENSE_INTERNAL, MFG_EXPENSE_REVPRUN gl account balances before start of production run
        Map<String, Number> initialBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        String productionRunId = createProductionRunSimilarToGzManufacturedAndConfirm("To be Revert after issue all of the first and second task, should success", 3, productIds);
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);

        // get the two tasks
        List<GenericValue> tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"), UtilMisc.toList("+workEffortId"));
        assertNotEmpty("Production run created for [" + finishedProductId + "] has no routing tasks. Cannot finish test.", tasks);
        assertEquals("Template for [" + finishedProductId + "] has two tasks.", tasks.size(), 2);

        // note the time so we can easily find the transaction
        start = UtilDateTime.nowTimestamp();

        // note the initial inventory quantities
        for (i = 0; i < productIds.size(); i++) {
            origInventory[i] = inventoryAsserts.getInventory(productIds.get(i));
        }

        // note the initial inventory values
        initInvValues = FastMap.newInstance();
        for (i = 0; i < productIds.size(); i++) {
            initInvValues.put(productIds.get(i), inventoryAsserts.getInventoryValueForProduct(productIds.get(i), start));
        }

        // get the average costs of all the products
        for (i = 0; i < productIds.size(); i++) {
            initAvgCost[i] = UtilCOGS.getProductAverageCost(productIds.get(i), organizationPartyId, demowarehouse1, delegator, dispatcher);
        }

        i = 0;
        for (GenericValue task : tasks) {
            String taskId = task.getString("workEffortId");

            startTaskAndIssueInventory(productionRunId, taskId);

            // complete only the first task
            if (i == 0) {
                // complete the task, which should trigger production run completed
                input = UtilMisc.toMap("userLogin", demowarehouse1);
                input.put("productionRunId", productionRunId);
                input.put("workEffortId", taskId);
                runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);
            }
            i++;
        }

        // Get the RAWMAT_INVENTORY, WIP_INVENTORY, MFG_EXPENSE_INTERNAL, MFG_EXPENSE_REVPRUN gl account balances before production run is reverted
        Map beforeRevertBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // verify that change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_INTERNAL) = 0
        List initialList = UtilMisc.toList(initialBalances.get(wipInventory), initialBalances.get(rawmatInventory), initialBalances.get(mfgExpenseInternal));
        List beforeRevertList = UtilMisc.toList(beforeRevertBalances.get(wipInventory), beforeRevertBalances.get(rawmatInventory), beforeRevertBalances.get(mfgExpenseInternal));
        assertDifferenceCorrect("change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_INTERNAL)", initialList, beforeRevertList, BigDecimal.ZERO);

        // Revert the production run
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        runAndAssertServiceSuccess("revertProductionRunAndSaveAllParts", input);
        GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
        assertEquals("Production Run [" + productionRunId + "] has wrong status.", "PRUN_REVERTED", productionRun.getString("currentStatusId"));
        // verify that the first task is completed but the second task is cancelled
        tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"), UtilMisc.toList("+workEffortId"));
        GenericValue task1 = tasks.get(0);
        String taskId = task1.getString("workEffortId");
        assertEquals("Production Run [" + productionRunId + "] task [" + taskId + "] has wrong status.", "PRUN_COMPLETED", task1.getString("currentStatusId"));
        GenericValue task2 = tasks.get(1);
        taskId = task2.getString("workEffortId");
        assertEquals("Production Run [" + productionRunId + "] task [" + taskId + "] has wrong status.", "PRUN_REVERTED", task2.getString("currentStatusId"));

        // make sure the inventory quantity changes are correct
        for (i = 0; i < productIds.size(); i++) {
            inventoryAsserts.assertInventoryChange(productIds.get(i), new BigDecimal("0.0"), origInventory[i]);
        }

        // verify the accounting values of inventory have not changed
        Map finalInvValues = FastMap.newInstance();
        for (i = 0; i < productIds.size(); i++) {
            finalInvValues.put(productIds.get(i), inventoryAsserts.getInventoryValueForProduct(productIds.get(i), UtilDateTime.nowTimestamp()));
        }
        assertMapDifferenceCorrect(initInvValues, finalInvValues, expectedInvValues);

        // verify all the average costs have not changed
        for (i = 0; i < productIds.size(); i++) {
            BigDecimal avgCost = UtilCOGS.getProductAverageCost(productIds.get(i), organizationPartyId, demowarehouse1, delegator, dispatcher);
            assertEquals("Production Run [" + productionRunId + "] for product [" + productIds.get(i) + "] get a modified average cost.", initAvgCost[i], avgCost);
        }

        // Get the RAWMAT_INVENTORY, WIP_INVENTORY, MFG_EXPENSE_INTERNAL, MFG_EXPENSE_REVPRUN gl account balances after production run is reverted
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // verify that from before start of production run to after the production run is reverted:
        //   change(WIP_INVENTORY) = 0
        //   change(RAWMAT_INVENTORY) = 0
        Map expectedBalanceChanges = UtilMisc.toMap("WIP_INVENTORY", "0", "RAWMAT_INVENTORY", "0");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);

        //   change(MFG_EXPENSE_INTERNAL) + change(MFG_EXPENSE_REVPRUN) = 0
        initialList = UtilMisc.toList(initialBalances.get(mfgExpenseInternal), initialBalances.get(mfgExpenseRevprun));
        List finalList = UtilMisc.toList(finalBalances.get(mfgExpenseInternal), finalBalances.get(mfgExpenseRevprun));
        assertDifferenceCorrect("change(MFG_EXPENSE_INTERNAL) + change(MFG_EXPENSE_REVPRUN)", initialList, finalList, BigDecimal.ZERO);

        // verify that from before production run is reverted to after the production run is reverted:
        //   change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_REVPRUN) = 0
        beforeRevertList = UtilMisc.toList(beforeRevertBalances.get(wipInventory), beforeRevertBalances.get(rawmatInventory), beforeRevertBalances.get(mfgExpenseInternal));
        finalList = UtilMisc.toList(finalBalances.get(wipInventory), finalBalances.get(rawmatInventory), finalBalances.get(mfgExpenseInternal));
        assertDifferenceCorrect("change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_REVPRUN)", beforeRevertList, finalList, BigDecimal.ZERO);
    }

    /**
     * Reverting Production Runs
     * 5. Create a production run for GZ-MANUFACTURED, issue part of the first task, then revert it. Verify PRUN_REVERTED.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testRevertingProductionRunAfterPartialTask() throws GeneralException {

        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String rawmatInventory = UtilFinancial.getOrgGlAccountId(organizationPartyId, "RAWMAT_INVENTORY", delegator);
        String wipInventory = UtilFinancial.getOrgGlAccountId(organizationPartyId, "WIP_INVENTORY", delegator);
        String mfgExpenseInternal = UtilFinancial.getOrgGlAccountId(organizationPartyId, "MFG_EXPENSE_INTERNAL", delegator);
        String mfgExpenseRevprun = UtilFinancial.getOrgGlAccountId(organizationPartyId, "MFG_EXPENSE_REVPRUN", delegator);

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);


        // set up original inventory values and average costs
        List<String> productIds = createProductBOMAndRoutingSimilarToGzManufactured("To be Revert after part issue of the first task, should success");
        Map[] origInventory = new Map[productIds.size()];
        Map initInvValues = null;
        Timestamp start = null;
        int i = 0;

        BigDecimal[] initAvgCost = new BigDecimal[productIds.size()];
        Map expectedInvValues = makeProductIdValueMap(productIds, "0");

        /**
         * 5. Create a production run for GZ-MANUFACTURED, issue part of the first task, then revert it. Verify PRUN_REVERTED.
         */
        // Get the RAWMAT_INVENTORY, WIP_INVENTORY, MFG_EXPENSE_INTERNAL, MFG_EXPENSE_REVPRUN gl account balances before start of production run
        Map initialBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        String productionRunId = createProductionRunSimilarToGzManufacturedAndConfirm("To be Revert after part issue of the first task, should success", 3, productIds);
        String finishedProductId = productIds.get(0);  // the first product ID in the list is that of the finished product
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);

        // get the two tasks
        List<GenericValue> tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"), UtilMisc.toList("+workEffortId"));
        assertNotEmpty("Production run created for [" + finishedProductId + "] has no routing tasks. Cannot finish test.", tasks);
        assertEquals("Template for [" + finishedProductId + "] has two tasks.", tasks.size(), 2);

        GenericValue task1 = EntityUtil.getFirst(tasks);
        String taskId = task1.getString("workEffortId");

        // note the time so we can easily find the transaction
        start = UtilDateTime.nowTimestamp();

        // note the initial inventory quantities
        for (i = 0; i < productIds.size(); i++) {
            origInventory[i] = inventoryAsserts.getInventory(productIds.get(i));
        }

        // note the initial inventory values
        initInvValues = FastMap.newInstance();
        for (i = 0; i < productIds.size(); i++) {
            initInvValues.put(productIds.get(i), inventoryAsserts.getInventoryValueForProduct(productIds.get(i), start));
        }

        // get the average costs of all the products
        for (i = 0; i < productIds.size(); i++) {
            initAvgCost[i] = UtilCOGS.getProductAverageCost(productIds.get(i), organizationPartyId, demowarehouse1, delegator, dispatcher);
        }

        // start the task
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        // issue part of the inventory required
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", taskId);
        input.put("productId", productIds.get(1));
        input.put("quantity", new BigDecimal("2.0"));
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", input);

        // Get the RAWMAT_INVENTORY, WIP_INVENTORY, MFG_EXPENSE_INTERNAL, MFG_EXPENSE_REVPRUN gl account balances before production run is reverted
        Map beforeRevertBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // verify that change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_INTERNAL) = 0
        List initialList = UtilMisc.toList(initialBalances.get(wipInventory), initialBalances.get(rawmatInventory), initialBalances.get(mfgExpenseInternal));
        List beforeRevertList = UtilMisc.toList(beforeRevertBalances.get(wipInventory), beforeRevertBalances.get(rawmatInventory), beforeRevertBalances.get(mfgExpenseInternal));
        assertDifferenceCorrect("change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_INTERNAL)", initialList, beforeRevertList, BigDecimal.ZERO);

        // Revert the production run
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        runAndAssertServiceSuccess("revertProductionRunAndSaveAllParts", input);
        GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
        assertEquals("Production Run [" + productionRunId + "] has wrong status.", "PRUN_REVERTED", productionRun.getString("currentStatusId"));
        // verify that the first task is completed but the second task is cancelled
        tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"), UtilMisc.toList("+workEffortId"));
        task1 = tasks.get(0);
        taskId = task1.getString("workEffortId");
        assertEquals("Production Run [" + productionRunId + "] task [" + taskId + "] has wrong status.", "PRUN_REVERTED", task1.getString("currentStatusId"));
        GenericValue task2 = tasks.get(1);
        taskId = task2.getString("workEffortId");
        assertEquals("Production Run [" + productionRunId + "] task [" + taskId + "] has wrong status.", "PRUN_CANCELLED", task2.getString("currentStatusId"));

        // make sure the inventory quantity changes are correct
        for (i = 0; i < productIds.size(); i++) {
            inventoryAsserts.assertInventoryChange(productIds.get(i), new BigDecimal("0.0"), origInventory[i]);
        }

        // verify that the inventory value has not changed
        Map finalInvValues = FastMap.newInstance();
        for (i = 0; i < productIds.size(); i++) {
            finalInvValues.put(productIds.get(i), inventoryAsserts.getInventoryValueForProduct(productIds.get(i), UtilDateTime.nowTimestamp()));
        }
        assertMapDifferenceCorrect(initInvValues, finalInvValues, expectedInvValues);

        // verify all the average costs have not changed
        for (i = 0; i < productIds.size(); i++) {
            BigDecimal avgCost = UtilCOGS.getProductAverageCost(productIds.get(i), organizationPartyId, demowarehouse1, delegator, dispatcher);
            assertEquals("Production Run [" + productionRunId + "] for product [" + productIds.get(i) + "] get a modified average cost.", initAvgCost[i], avgCost);
        }

        // Get the RAWMAT_INVENTORY, WIP_INVENTORY, MFG_EXPENSE_INTERNAL, MFG_EXPENSE_REVPRUN gl account balances after production run is reverted
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // verify that from before start of production run to after the production run is reverted:
        //   change(WIP_INVENTORY) = 0
        //   change(RAWMAT_INVENTORY) = 0
        Map expectedBalanceChanges = UtilMisc.toMap("WIP_INVENTORY", "0", "RAWMAT_INVENTORY", "0");
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);

        //   change(MFG_EXPENSE_INTERNAL) + change(MFG_EXPENSE_REVPRUN) = 0
        initialList = UtilMisc.toList(initialBalances.get(mfgExpenseInternal), initialBalances.get(mfgExpenseRevprun));
        List finalList = UtilMisc.toList(finalBalances.get(mfgExpenseInternal), finalBalances.get(mfgExpenseRevprun));
        assertDifferenceCorrect("change(MFG_EXPENSE_INTERNAL) + change(MFG_EXPENSE_REVPRUN)", initialList, finalList, BigDecimal.ZERO);

        // verify that from before production run is reverted to after the production run is reverted:
        //   change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_REVPRUN) = 0
        beforeRevertList = UtilMisc.toList(beforeRevertBalances.get(wipInventory), beforeRevertBalances.get(rawmatInventory), beforeRevertBalances.get(mfgExpenseInternal));
        finalList = UtilMisc.toList(finalBalances.get(wipInventory), finalBalances.get(rawmatInventory), finalBalances.get(mfgExpenseInternal));
        assertDifferenceCorrect("change(WIP_INVENTORY) + change(RAWMAT_INVENTORY) + change(MFG_EXPENSE_REVPRUN)", beforeRevertList, finalList, BigDecimal.ZERO);
    }

    /**
     * Create a disassemble production run for GZ-MANUFACTURED, issue part of the first task, then revert it. Verify PRUN_REVERTED.
     * @throws GeneralException if an error occurs
     */
    public void testRevertingDisassemblyProductionRun() throws GeneralException {

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);

        // set up original inventory values and average costs
        List<String> productIds = UtilMisc.toList("GZ-MANUFACTURED", "MAT-MANUFACTURED-1", "MAT-MANUFACTURED-2", "MAT-MANUFACTURED-3", "MAT-MANUFACTURED-4", "MAT-MANUFACTURED-5");
        productIds.add("MAT-MANUFACTURED-6");
        Map[] origInventory = new Map[productIds.size()];
        Map<String, BigDecimal> initInvValues = null;
        Timestamp start = null;
        int i = 0;

        BigDecimal[] initAvgCost = new BigDecimal[productIds.size()];
        Map<String, String> expectedInvValues = makeProductIdValueMap(productIds, "0");

        /**
         * 6. Create a disassembly production run for GZ-MANUFACTURED, issue part of the first task, then revert it. Verify PRUN_REVERTED.
         */
        String productionRunId = createDisassemblyGzManufactured("Test disassembly of 2 GZ-MANUFACTURED", 2);
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "productionRunId", productionRunId);

        // get the two tasks
        List<GenericValue> tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"), UtilMisc.toList("+workEffortId"));
        assertNotEmpty("Production run created for GZ-MANUFACTURED has no routing tasks. Cannot finish test.", tasks);
        assertEquals("Disassemble GZ-MANUFACTURED has one tasks.", tasks.size(), 1);

        // note the time so we can easily find the transaction
        start = UtilDateTime.nowTimestamp();

        // note the initial inventory quantities
        for (i = 0; i < productIds.size(); i++) {
            origInventory[i] = inventoryAsserts.getInventory(productIds.get(i));
        }

        // note the initial inventory values
        initInvValues = FastMap.newInstance();
        for (i = 0; i < productIds.size(); i++) {
            initInvValues.put(productIds.get(i), inventoryAsserts.getInventoryValueForProduct(productIds.get(i), start));
        }

        // get the average costs of all the products
        for (i = 0; i < productIds.size(); i++) {
            initAvgCost[i] = UtilCOGS.getProductAverageCost(productIds.get(i), organizationPartyId, demowarehouse1, delegator, dispatcher);
        }

        GenericValue task1 = EntityUtil.getFirst(tasks);
        String taskId = task1.getString("workEffortId");

        startTaskAndIssueInventory(productionRunId, taskId);

        // Revert the production run
        input = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "productionRunId", productionRunId);
        runAndAssertServiceSuccess("revertProductionRunAndSaveAllParts", input);
        GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
        assertEquals("Production Run [" + productionRunId + "] has wrong status.", "PRUN_REVERTED", productionRun.getString("currentStatusId"));
        // verify that the first task is completed but the second task is cancelled
        tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"), UtilMisc.toList("+workEffortId"));
        task1 = tasks.get(0);
        taskId = task1.getString("workEffortId");
        assertEquals("Production Run [" + productionRunId + "] task [" + taskId + "] has wrong status.", "PRUN_REVERTED", task1.getString("currentStatusId"));

        // make sure the inventory quantity changes are correct
        for (i = 0; i < productIds.size(); i++) {
            inventoryAsserts.assertInventoryChange(productIds.get(i), new BigDecimal("0.0"), origInventory[i]);
        }

        // verify the final value of the inventory has not changed
        Map<String, BigDecimal> finalInvValues = FastMap.newInstance();
        for (i = 0; i < productIds.size(); i++) {
            finalInvValues.put(productIds.get(i), inventoryAsserts.getInventoryValueForProduct(productIds.get(i), UtilDateTime.nowTimestamp()));
        }
        assertMapDifferenceCorrect(initInvValues, finalInvValues, expectedInvValues);

        // verify all the average costs have not changed
        for (i = 0; i < productIds.size(); i++) {
            BigDecimal avgCost = UtilCOGS.getProductAverageCost(productIds.get(i), organizationPartyId, demowarehouse1, delegator, dispatcher);
            assertEquals("Production Run [" + productionRunId + "] for product [" + productIds.get(i) + "] get a modified average cost.", initAvgCost[i], avgCost);
        }

    }

    /**
     * Tests backward inventory tracing for inventory item produced as result of manufacturing.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testTraceProducedInventory() throws Exception {
        // create test materials and product for manufacturing
        GenericValue mat1 = createTestProduct("Material 1 for testTraceProducedInventory", "RAW_MATERIAL", admin);
        GenericValue mat2 = createTestProduct("Material 1 for testTraceProducedInventory", "RAW_MATERIAL", admin);
        GenericValue manufacturedProduct = createTestProduct("Product for manufacturing within framework of for testTraceProducedInventory", admin);
        assignDefaultPrice(manufacturedProduct, new BigDecimal("120.0"), admin);
        pause("Workaround pause for MySQL");

        // 1. Receive 2 of material 1. This should be inventoryItemId1.
        Map<String, Object> results = receiveInventoryProduct(mat1, new BigDecimal("2.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);
        String inventoryItemId1 = (String) results.get("inventoryItemId");
        pause("Workaround pause for MySQL");

        // 2. Receive 2 of material 2. This should be inventoryItemId2.
        results = receiveInventoryProduct(mat2, new BigDecimal("2.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("15.0"), demowarehouse1);
        String inventoryItemId2 = (String) results.get("inventoryItemId");
        pause("Workaround pause for MySQL");

        delegator.create("ProductFacility",
                UtilMisc.toMap(
                        "productId", manufacturedProduct.get("productId"),
                        "facilityId", "WebStoreWarehouse",
                        "minimumStock", Long.valueOf(0),
                        "reorderQuantity", new BigDecimal("1.0"),
                        "daysToShip", Long.valueOf(1)
                )
        );

        createMainSupplierForProduct(mat1.getString("productId"), "DemoSupplier", new BigDecimal("50.0"), "USD", new BigDecimal("0.0"), admin);
        pause("Workaround pause for MySQL");
        createMainSupplierForProduct(mat2.getString("productId"), "DemoSupplier", new BigDecimal("13.0"), "USD", new BigDecimal("0.0"), admin);
        pause("Workaround pause for MySQL");

        delegator.create("ProductFacility",
                UtilMisc.toMap(
                        "productId", mat1.get("productId"),
                        "facilityId", "WebStoreWarehouse",
                        "minimumStock", Long.valueOf(0),
                        "reorderQuantity", new BigDecimal("1.0"),
                        "daysToShip", Long.valueOf(1)
                )
        );
        delegator.create("ProductFacility",
                UtilMisc.toMap(
                        "productId", mat2.get("productId"),
                        "facilityId", "WebStoreWarehouse",
                        "minimumStock", Long.valueOf(0),
                        "reorderQuantity", new BigDecimal("1.0"),
                        "daysToShip", Long.valueOf(1)
                )
        );

        delegator.create("ProductAssoc", UtilMisc.toMap(
                "productId", manufacturedProduct.get("productId"),
                "productIdTo", mat1.get("productId"),
                "productAssocTypeId", "MANUF_COMPONENT",
                "sequenceNum", Long.valueOf(1),
                "quantity", new BigDecimal("1.0"),
                "fromDate", UtilDateTime.nowTimestamp()
                ));
        delegator.create("ProductAssoc", UtilMisc.toMap(
                "productId", manufacturedProduct.get("productId"),
                "productIdTo", mat2.get("productId"),
                "productAssocTypeId", "MANUF_COMPONENT",
                "sequenceNum", Long.valueOf(2),
                "quantity", new BigDecimal("1.0"),
                "fromDate", UtilDateTime.nowTimestamp()
                ));
        pause("Workaround pause for MySQL");

        createTestAssemblingRouting("testTraceProducedInventory", manufacturedProduct.getString("productId"));
        pause("Workaround pause for MySQL");

        // create production run and start task
        String productionRunId = createProductionRunAndConfirm("Production Run for inventory trace tests", 1, manufacturedProduct.getString("productId"));
        pause("Workaround pause for MySQL");
        String taskId = getOnlyTask(productionRunId);
        pause("Workaround pause for MySQL");

        startTaskAndIssueInventory(productionRunId, taskId);
        pause("Workaround pause for MySQL");

        // complete the task, which should trigger production run completed
        Map<String, Object> callCtxt = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1);
        callCtxt.put("productionRunId", productionRunId);
        callCtxt.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", callCtxt);
        pause("Workaround pause for MySQL");

        // create and assign inventory for the product
        callCtxt = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1);
        callCtxt.put("workEffortId", productionRunId);
        results = runAndAssertServiceSuccess("productionRunProduce", callCtxt);
        List<String> inventoryItemIds = (List<String>) results.get("inventoryItemIds");
        String producedInventoryId = inventoryItemIds.get(0);
        pause("Workaround pause for MySQL");

        // get and verify backward tracing data
        results = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("inventoryItemId", producedInventoryId, "traceDirection", "BACKWARD", "userLogin", admin));
        List<List<InventoryItemTraceDetail>> usageLogs = ((List<List<InventoryItemTraceDetail>>) results.get("usageLog"));
        List<InventoryItemTraceDetail> usageLog = UtilValidate.isEmpty(usageLogs) ? FastList.<InventoryItemTraceDetail>newInstance() : usageLogs.get(0);
        // should be six records there, 2 RECEIPTs of raw materials and 2 MANUF_RAW_MAT
        assertEquals("Unexpected count of trace events", 4, usageLog.size());

        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        assertInventoryTraceEvents(
                                assertInventoryTraceEvents(
                                        usageLog, inventoryItemId1, producedInventoryId, "MANUF_RAW_MAT", 2L
                                ), inventoryItemId2, producedInventoryId, "MANUF_RAW_MAT", 2L
                        ), null, inventoryItemId1, "RECEIPT", 1L
                ), null, inventoryItemId2, "RECEIPT", 1L
        );

    }

    /**
     * Tests BOM simulation for products with multiple BOM.
     * Creates a manufactured product with two BOM and test the default BOM simulation, and the simulation specifying the alternate routing BOM.
     * @throws Exception if an error occurs
     */
    public void testMultipleBomSimulation() throws Exception {
        // Create a test product
        final GenericValue product = createTestProduct("test Multiple BOM Simulation Product", demowarehouse1);
        final String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);

        // Create a default routing, using createTestAssemblingRouting
        createTestAssemblingRouting("Default BOM Simulation for product [" + productId + "]", productId);

        // Create a default BOM with two components, using createBOMProductAssoc
        final GenericValue productComp1 = createTestProduct("test Default Material for [" + productId + "] - Component 1", demowarehouse1);
        final String productComp1Id = productComp1.getString("productId");
        final GenericValue productComp2 = createTestProduct("test Default Material for [" + productId + "] - Component 2", demowarehouse1);
        final String productComp2Id = productComp2.getString("productId");
        createBOMProductAssoc(productId, productComp1Id, new Long("10"), new BigDecimal("7.0"), admin);
        createBOMProductAssoc(productId, productComp2Id, new Long("11"), new BigDecimal("3.0"), admin);

        // Create an alternative routing, using createBOMProductAssoc
        final String alternateRoutingId = createTestAssemblingRouting("Alternate BOM for product [" + productId + "]", productId);
        final GenericValue productComp3 = createTestProduct("test Alternate Material for [" + productId + "] - Component 3", demowarehouse1);
        final String productComp3Id = productComp3.getString("productId");
        final GenericValue productComp4 = createTestProduct("test Alternate Material for [" + productId + "] - Component 4", demowarehouse1);
        final String productComp4Id = productComp4.getString("productId");
        final GenericValue productComp5 = createTestProduct("test Alternate Material for [" + productId + "] - Component 5", demowarehouse1);
        final String productComp5Id = productComp5.getString("productId");

        createBOMProductAssoc(productId, productComp3Id, alternateRoutingId, new Long("10"), new BigDecimal("1.0"), admin);
        createBOMProductAssoc(productId, productComp4Id, alternateRoutingId, new Long("11"), new BigDecimal("2.0"), admin);
        createBOMProductAssoc(productId, productComp5Id, alternateRoutingId, new Long("12"), new BigDecimal("5.0"), admin);

        // Run a BOM simulation for the default BOM (calling getBOMTree service)
        Map<String, Object> result = runAndAssertServiceSuccess("getBOMTree", UtilMisc.<String, Object>toMap("productId", productId, "type", BomTree.EXPLOSION, "bomType", "MANUF_COMPONENT", "quantity", new BigDecimal("1.0"), "userLogin", demowarehouse1));
        BomTree tree = (BomTree) result.get("tree");
        // check the result correspond the to the default BOM components
        assertBomTreeCorrect(tree, UtilMisc.toMap(productId, new BigDecimal("1.0"), productComp1Id, new BigDecimal("7.0"), productComp2Id, new BigDecimal("3.0")));

        // Run a BOM simulation for the alternative BOM (calling getBOMTree service)
        result = runAndAssertServiceSuccess("getBOMTree", UtilMisc.<String, Object>toMap("productId", productId, "type", BomTree.EXPLOSION, "bomType", "MANUF_COMPONENT", "quantity", new BigDecimal("1.0"), "routingId", alternateRoutingId, "userLogin", demowarehouse1));
        tree = (BomTree) result.get("tree");
        // check the result correspond the to the alternative BOM components
        assertBomTreeCorrect(tree, UtilMisc.toMap(productId, new BigDecimal("1.0"), productComp3Id, new BigDecimal("1.0"), productComp4Id, new BigDecimal("2.0"), productComp5Id, new BigDecimal("5.0")));
    }

    /**
     * Tests production for products with multiple BOM.
     * Creates a manufactured product with two BOM and test the default production run, and the production run specifying the alternate routing BOM.
     * Note: to test the correct quantity of raw materials used, we get the inventory of all possible raw materials, then receive the ones.
     * which should be consumed based on the BOM  for the routing, and at the end checked the quantity of all the raw material products again.
     * Since the correct raw materials should have been consumed, their quantities should go back to zero again.
     * The quantities of the other raw materials should not have changed.  Therefore, the net change of all the raw materials quantity should be zero.
     * @throws Exception if an error occurs
     */
    public void testMultipleBomProduction() throws Exception {
        // Create a test product
        final GenericValue product = createTestProduct("test Multiple BOM Production Product", demowarehouse1);
        final String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);

        // Create a default routing, using createTestAssemblingRouting
        createTestAssemblingRouting("Default BOM for product [" + productId + "]", productId);

        // Create a default BOM with two components, using createBOMProductAssoc
        final GenericValue productComp1 = createTestProduct("test Default Material for [" + productId + "] - Component 1", demowarehouse1);
        final String productComp1Id = productComp1.getString("productId");
        final GenericValue productComp2 = createTestProduct("test Default Material for [" + productId + "] - Component 2", demowarehouse1);
        final String productComp2Id = productComp2.getString("productId");
        createBOMProductAssoc(productId, productComp1Id, new Long("10"), new BigDecimal("7.0"), admin);
        createBOMProductAssoc(productId, productComp2Id, new Long("11"), new BigDecimal("3.0"), admin);

        // Create an alternative routing, using createBOMProductAssoc
        final String alternateRoutingId = createTestAssemblingRouting("Alternate BOM for product [" + productId + "]", productId);

        // Create an alternative BOM with three components, using createBOMProductAssoc
        final GenericValue productComp3 = createTestProduct("test Alternate Material for [" + productId + "] - Component 3", demowarehouse1);
        final String productComp3Id = productComp3.getString("productId");
        final GenericValue productComp4 = createTestProduct("test Alternate Material for [" + productId + "] - Component 4", demowarehouse1);
        final String productComp4Id = productComp4.getString("productId");
        final GenericValue productComp5 = createTestProduct("test Alternate Material for [" + productId + "] - Component 5", demowarehouse1);
        final String productComp5Id = productComp5.getString("productId");

        createBOMProductAssoc(productId, productComp3Id, alternateRoutingId, new Long("10"), new BigDecimal("1.0"), admin);
        createBOMProductAssoc(productId, productComp4Id, alternateRoutingId, new Long("11"), new BigDecimal("2.0"), admin);
        createBOMProductAssoc(productId, productComp5Id, alternateRoutingId, new Long("12"), new BigDecimal("5.0"), admin);

        // note the initial inventory quantities
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        Map<String, Map<String, Object>> origProductInventories = inventoryAsserts.getInventories(Arrays.asList(productId, productComp1Id, productComp2Id, productComp3Id, productComp4Id, productComp5Id));

        // Create a production run of 5 with default routing (not giving any routing ID)
        String prunId = createProductionRunAndConfirm("test Default BOM Prun", 5, productId);
        // get the one and only task, and starts it
        String taskId = startOnlyTask(prunId);
        // receive the material components into the warehouse
        receiveMaterial(productComp1Id, 35, 2.5);
        receiveMaterial(productComp2Id, 15, 2.5);
        inventoryAsserts.assertInventoriesChange(productComp1Id, new BigDecimal("35.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp2Id, new BigDecimal("15.0"), origProductInventories);
        // issue required quantities of components
        runAndAssertServiceSuccess("issueProductionRunTask", UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId));
        // complete the task
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunId, "workEffortId", taskId, "userLogin", demowarehouse1));
        // produce the products
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productId, "workEffortId", prunId, "quantity", new BigDecimal("5.0"), "userLogin", demowarehouse1));
        // complete production run (close it)
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunId, "workEffortId", taskId, "userLogin", demowarehouse1));
        // check inventory
        inventoryAsserts.assertInventoriesChange(productId, new BigDecimal("5.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp1Id, productComp2Id, productComp3Id, productComp4Id, productComp5Id), new BigDecimal("0.0"), origProductInventories);

        // Create a production run of 10 with alternate routing (giving its routing ID)
        prunId = createProductionRunAndConfirm("test Alternate BOM Prun", 10, productId, alternateRoutingId);
        // get the one and only task, and starts it
        taskId = startOnlyTask(prunId);
        // receive the material components into the warehouse
        receiveMaterial(productComp3Id, 10, 2.5);
        receiveMaterial(productComp4Id, 20, 2.5);
        receiveMaterial(productComp5Id, 50, 2.5);
        inventoryAsserts.assertInventoriesChange(productComp3Id, new BigDecimal("10.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp4Id, new BigDecimal("20.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp5Id, new BigDecimal("50.0"), origProductInventories);
        // issue required quantities of components for alternative BOM
        runAndAssertServiceSuccess("issueProductionRunTask", UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId));
        // complete the task
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunId, "workEffortId", taskId, "userLogin", demowarehouse1));
        // produce the products
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productId, "workEffortId", prunId, "quantity", new BigDecimal("10.0"), "userLogin", demowarehouse1));
        // complete production run (close it)
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunId, "workEffortId", taskId, "userLogin", demowarehouse1));
        // check inventory
        inventoryAsserts.assertInventoriesChange(productId, new BigDecimal("15.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp1Id, productComp2Id, productComp3Id, productComp4Id, productComp5Id), new BigDecimal("0.0"), origProductInventories);
    }

    /**
     * Tests BOM simulations and production runs for products with multiple BOM, where components also have multiple BOMs.
     * Creates a manufactured product with two BOMs with multiple levels
     * The product default BOM is a normal 2 levels BOM.
     * The product alternate BOM used one component with a specific alternate routing (which not link to the product alternate routing) and therefore should be used
     *  and a component with an alternate routing linked to the manufactured product alternate routing, which therefore should be used.
     * Note: this is a big test, but it not split to test that the correct routing is chosen.
     * See note about testing the correct quantity of raw materials from testMultipleBomProduction()
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testComplexMultipleBomProduction() throws Exception {
        // Create a test product
        final GenericValue product = createTestProduct("test Complex Multiple BOM Product", demowarehouse1);
        final String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);

        // Create a default routing, using createTestAssemblingRouting
        createTestAssemblingRouting("Default BOM for product [" + productId + "]", productId);

        // a default BOM for product
        //   7 x productComp1
        //     7 x productComp11  (7 x 1)
        //     14 x productComp12 (7 x 2)
        //   3 x productComp2
        //     9 x productComp21  (3 x 3)
        //     12 x productComp22 (3 x 4)

        // Create a default BOM with two components, using createBOMProductAssoc
        final GenericValue productComp1 = createTestProduct("test Default Material for [" + productId + "] - Component 1", demowarehouse1);
        final String productComp1Id = productComp1.getString("productId");
        final GenericValue productComp2 = createTestProduct("test Default Material for [" + productId + "] - Component 2", demowarehouse1);
        final String productComp2Id = productComp2.getString("productId");
        createBOMProductAssoc(productId, productComp1Id, new Long("10"), new BigDecimal("7.0"), admin);
        createBOMProductAssoc(productId, productComp2Id, new Long("11"), new BigDecimal("3.0"), admin);

        // Create default BOMs for both components
        final GenericValue productComp11 = createTestProduct("test Default Material for [" + productComp1Id + "] - Component 1/1", demowarehouse1);
        final String productComp11Id = productComp11.getString("productId");
        final GenericValue productComp12 = createTestProduct("test Default Material for [" + productComp1Id + "] - Component 1/2", demowarehouse1);
        final String productComp12Id = productComp12.getString("productId");
        createBOMProductAssoc(productComp1Id, productComp11Id, new Long("10"), new BigDecimal("1.0"), admin);
        createBOMProductAssoc(productComp1Id, productComp12Id, new Long("11"), new BigDecimal("2.0"), admin);

        final GenericValue productComp21 = createTestProduct("test Default Material for [" + productComp2Id + "] - Component 2/1", demowarehouse1);
        final String productComp21Id = productComp21.getString("productId");
        final GenericValue productComp22 = createTestProduct("test Default Material for [" + productComp2Id + "] - Component 2/2", demowarehouse1);
        final String productComp22Id = productComp22.getString("productId");
        createBOMProductAssoc(productComp2Id, productComp21Id, new Long("10"), new BigDecimal("3.0"), admin);
        createBOMProductAssoc(productComp2Id, productComp22Id, new Long("11"), new BigDecimal("4.0"), admin);

        // Run a BOM simulation for the default BOM (calling getBOMTree service)
        Map<String, Object> result = runAndAssertServiceSuccess("getBOMTree", UtilMisc.<String, Object>toMap("productId", productId, "type", BomTree.EXPLOSION, "bomType", "MANUF_COMPONENT", "quantity", BigDecimal.ONE, "userLogin", demowarehouse1));
        BomTree tree = (BomTree) result.get("tree");
        // check the result correspond the to the default BOM components
        Map expectedBom = UtilMisc.toMap(productId, new BigDecimal("1.0"), productComp1Id, new BigDecimal("7.0"), productComp11Id, new BigDecimal("7.0"), productComp12Id, new BigDecimal("14.0"));
        expectedBom.putAll(UtilMisc.toMap(productComp2Id, new BigDecimal("3.0"), productComp21Id, new BigDecimal("9.0"), productComp22Id, new BigDecimal("12.0")));
        assertBomTreeCorrect(tree, expectedBom);

        // an alternate BOM for product
        //   8 x productComp1 (also uses the alternate routing)
        //     96 x productComp13  (8 x 12)
        //     120 x productComp14 (8 x 15)
        //   9 x productComp2 (have an alternate routing but should use the default)
        //     27 x productComp21 (9 x 3)
        //     36 x productComp22 (9 x 4)
        //   5 x productComp3

        // Create an alternative routing, using createBOMProductAssoc, note we also need a default routing to be created for it or
        // the production run services will always use the alternate routing (since it would be the only one defined).
        createTestAssemblingRouting("Default BOM for component [" + productComp2Id + "]", productComp2Id);
        final String alternateRoutingId = createTestAssemblingRouting("Alternate BOM for product [" + productId + "]", productId);

        // Create an alternative BOM with one components + the same components as the default routing , using createBOMProductAssoc
        final GenericValue productComp3 = createTestProduct("test Alternate Material for [" + productId + "] - Component 3", demowarehouse1);
        final String productComp3Id = productComp3.getString("productId");

        createBOMProductAssoc(productId, productComp1Id, alternateRoutingId, new Long("10"), new BigDecimal("8.0"), admin);
        createBOMProductAssoc(productId, productComp2Id, alternateRoutingId, new Long("11"), new BigDecimal("9.0"), admin);
        createBOMProductAssoc(productId, productComp3Id, alternateRoutingId, new Long("12"), new BigDecimal("5.0"), admin);

        // Create an alternative routing for productComp2, should not be used when using the alternate BOM for product
        final String alternateComp2RoutingId = createTestAssemblingRouting("Alternate BOM for product [" + productComp2Id + "]", productComp2Id);

        // Create an alternative BOM for component 2
        final GenericValue productComp23 = createTestProduct("test Alternate Material for [" + productComp2Id + "] - Component 2/3", demowarehouse1);
        final String productComp23Id = productComp23.getString("productId");
        final GenericValue productComp24 = createTestProduct("test Alternate Material for [" + productComp2Id + "] - Component 2/4", demowarehouse1);
        final String productComp24Id = productComp24.getString("productId");
        createBOMProductAssoc(productComp2Id, productComp23Id, alternateComp2RoutingId, new Long("10"), new BigDecimal("11.0"), admin);
        createBOMProductAssoc(productComp2Id, productComp24Id, alternateComp2RoutingId, new Long("11"), new BigDecimal("33.0"), admin);

        // Create an alternative BOM for component 1, link to the manufactured product alternate routing
        final GenericValue productComp13 = createTestProduct("test Alternate Material for [" + productComp1Id + "] - Component 1/3", demowarehouse1);
        final String productComp13Id = productComp13.getString("productId");
        final GenericValue productComp14 = createTestProduct("test Alternate Material for [" + productComp1Id + "] - Component 1/4", demowarehouse1);
        final String productComp14Id = productComp14.getString("productId");
        createBOMProductAssoc(productComp1Id, productComp13Id, alternateRoutingId, new Long("10"), new BigDecimal("12.0"), admin);
        createBOMProductAssoc(productComp1Id, productComp14Id, alternateRoutingId, new Long("11"), new BigDecimal("15.0"), admin);

        // Run a BOM simulation for the alternate BOM (calling getBOMTree service)
        result = runAndAssertServiceSuccess("getBOMTree", UtilMisc.<String, Object>toMap("productId", productId, "type", BomTree.EXPLOSION, "bomType", "MANUF_COMPONENT", "quantity", new BigDecimal("1.0"), "routingId", alternateRoutingId, "userLogin", demowarehouse1));
        tree = (BomTree) result.get("tree");
        // check the result correspond the to the alternate BOM components
        expectedBom = UtilMisc.toMap(productId, 1.0, productComp1Id, 8.0, productComp13Id, 96.0, productComp14Id, 120.0);
        expectedBom.putAll(UtilMisc.toMap(productComp2Id, 9.0, productComp21Id, 27.0, productComp22Id, 36.0));
        expectedBom.put(productComp3Id, 5.0);
        assertBomTreeCorrect(tree, expectedBom);

        // note the initial inventory quantities
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        Map<String, Map<String, Object>> origProductInventories = inventoryAsserts.getInventories(Arrays.asList(productId, productComp1Id, productComp2Id, productComp3Id, productComp11Id, productComp12Id, productComp13Id, productComp14Id, productComp21Id, productComp22Id, productComp23Id, productComp24Id));

        // Create a production run of 5 with default routing (not giving any routing ID)
        String prunId = createProductionRunAndConfirm("test Default BOM Prun", 5, productId);
        // since components also have a BOM this should have created two mandatory production runs for them
        List<GenericValue> mandatoryWorkEfforts = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortAssoc", UtilMisc.toMap("workEffortIdTo", prunId, "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY")));
        assertEquals("Should have found 2 mandatory production run for the products manufactured components.", 2, mandatoryWorkEfforts.size());
        List<String> mandatoryWorkEffortIds = EntityUtil.getFieldListFromEntityList(mandatoryWorkEfforts, "workEffortIdFrom", true);

        // get the production run for component 1
        GenericValue mandatoryWorkEffortComp = EntityUtil.getOnly(delegator.findByAnd("WorkEffortGoodStandard", Arrays.asList(EntityCondition.makeCondition("workEffortId", EntityOperator.IN, mandatoryWorkEffortIds), EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUN_PROD_DELIV"), EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productComp1Id))));
        // get first component production run
        String prunIdComp = mandatoryWorkEffortComp.getString("workEffortId");
        confirmProductionRun(prunIdComp);
        // get the one and only task, and starts it
        String taskId = startOnlyTask(prunIdComp);
        // check the WEGS for the materials to issue (5x7x1 x productComp11Id, 5x14x1 x productComp12Id)
        assertWorkEffortHasWegs(taskId, "PRUNT_PROD_NEEDED", UtilMisc.toMap(productComp11Id, new BigDecimal("35.0"), productComp12Id, new BigDecimal("70.0")));
        // receive the material components into the warehouse
        receiveMaterial(productComp11Id, 35, 2.5);
        receiveMaterial(productComp12Id, 70, 2.5);
        inventoryAsserts.assertInventoriesChange(productComp11Id, new BigDecimal("35.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp12Id, new BigDecimal("70.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp1Id, productComp2Id, productComp3Id, productComp13Id, productComp14Id, productComp21Id, productComp22Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);
        // issue required quantities of components
        runAndAssertServiceSuccess("issueProductionRunTask", UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId));
        // complete the task
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunIdComp, "workEffortId", taskId, "userLogin", demowarehouse1));
        // produce the products
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productComp1Id, "workEffortId", prunIdComp, "quantity", new BigDecimal("35.0"), "userLogin", demowarehouse1));
        // complete production run (close it)
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunIdComp, "workEffortId", taskId, "userLogin", demowarehouse1));
        // check inventory
        inventoryAsserts.assertInventoriesChange(productComp1Id, new BigDecimal("35.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp2Id, productComp3Id, productComp11Id, productComp12Id, productComp13Id, productComp14Id, productComp21Id, productComp22Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);

        // get the production run for component 2
        mandatoryWorkEffortComp = EntityUtil.getOnly(delegator.findByAnd("WorkEffortGoodStandard", Arrays.asList(EntityCondition.makeCondition("workEffortId", EntityOperator.IN, mandatoryWorkEffortIds), EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUN_PROD_DELIV"), EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productComp2Id))));
        // get first component production run
        prunIdComp = mandatoryWorkEffortComp.getString("workEffortId");
        confirmProductionRun(prunIdComp);
        // get the one and only task, and starts it
        taskId = startOnlyTask(prunIdComp);
        // check the WEGS for the materials to issue (5x3x3 x productComp21Id, 5x3x4 x productComp22Id)
        assertWorkEffortHasWegs(taskId, "PRUNT_PROD_NEEDED", UtilMisc.toMap(productComp21Id, new BigDecimal("45.0"), productComp22Id, new BigDecimal("60.0")));
        // receive the material components into the warehouse
        receiveMaterial(productComp21Id, 45, 2.5);
        receiveMaterial(productComp22Id, 60, 2.5);
        inventoryAsserts.assertInventoriesChange(productComp1Id, new BigDecimal("35.0"), origProductInventories); // from before
        inventoryAsserts.assertInventoriesChange(productComp21Id, new BigDecimal("45.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp22Id, new BigDecimal("60.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp2Id, productComp3Id, productComp13Id, productComp14Id, productComp11Id, productComp12Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);
        // issue required quantities of components
        runAndAssertServiceSuccess("issueProductionRunTask", UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId));
        // complete the task
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunIdComp, "workEffortId", taskId, "userLogin", demowarehouse1));
        // produce the products
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productComp2Id, "workEffortId", prunIdComp, "quantity", new BigDecimal("15.0"), "userLogin", demowarehouse1));
        // complete production run (close it)
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunIdComp, "workEffortId", taskId, "userLogin", demowarehouse1));
        // check inventory
        inventoryAsserts.assertInventoriesChange(productComp1Id, new BigDecimal("35.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp2Id, new BigDecimal("15.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp3Id, productComp11Id, productComp12Id, productComp13Id, productComp14Id, productComp21Id, productComp22Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);

        // finally we can get the main production run going
        // get the one and only task, and starts it
        taskId = startOnlyTask(prunId);
        // check the WEGS for the materials to issue (5x7 x productComp1Id, 5x3 x productComp2Id)
        assertWorkEffortHasWegs(taskId, "PRUNT_PROD_NEEDED", UtilMisc.toMap(productComp1Id, new BigDecimal("35.0"), productComp2Id, new BigDecimal("15.0")));
        // issue required quantities of components
        runAndAssertServiceSuccess("issueProductionRunTask", UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId));
        // complete the task
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunId, "workEffortId", taskId, "userLogin", demowarehouse1));
        // produce the products
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productId, "workEffortId", prunId, "quantity", new BigDecimal("5.0"), "userLogin", demowarehouse1));
        // complete production run (close it)
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunId, "workEffortId", taskId, "userLogin", demowarehouse1));
        // check inventory
        inventoryAsserts.assertInventoriesChange(productId, new BigDecimal("5.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp1Id, productComp2Id, productComp3Id, productComp11Id, productComp12Id, productComp13Id, productComp14Id, productComp21Id, productComp22Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);

        /** Alternate routing **/

        // Create a production run of 10 with alternate routing (giving its routing ID)
        prunId = createProductionRunAndConfirm("test Alternate BOM Prun", 10, productId, alternateRoutingId);
        // since components also have a BOM this should have created two mandatory production runs for them
        mandatoryWorkEfforts = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortAssoc", UtilMisc.toMap("workEffortIdTo", prunId, "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY")));
        assertEquals("Should have found 2 mandatory production run for the products manufactured components.", 2, mandatoryWorkEfforts.size());
        mandatoryWorkEffortIds = EntityUtil.getFieldListFromEntityList(mandatoryWorkEfforts, "workEffortIdFrom", true);

        // get the production run for component 1
        mandatoryWorkEffortComp = EntityUtil.getOnly(delegator.findByAnd("WorkEffortGoodStandard", Arrays.asList(EntityCondition.makeCondition("workEffortId", EntityOperator.IN, mandatoryWorkEffortIds), EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUN_PROD_DELIV"), EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productComp1Id))));
        // get first component production run
        prunIdComp = mandatoryWorkEffortComp.getString("workEffortId");
        confirmProductionRun(prunIdComp);
        // get the one and only task, and starts it
        taskId = startOnlyTask(prunIdComp);
        // check the WEGS for the materials to issue (10x8x12 x productComp13Id, 10x8x15 x productComp14Id)
        assertWorkEffortHasWegs(taskId, "PRUNT_PROD_NEEDED", UtilMisc.toMap(productComp13Id, new BigDecimal("960.0"), productComp14Id, new BigDecimal("1200.0")));
        // receive the material components into the warehouse (for the alternate BOM of this component)
        receiveMaterial(productComp13Id, 960, 2.5);
        receiveMaterial(productComp14Id, 1200, 2.5);
        inventoryAsserts.assertInventoriesChange(productComp13Id, new BigDecimal("960.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp14Id, new BigDecimal("1200.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp1Id, productComp2Id, productComp3Id, productComp11Id, productComp12Id, productComp21Id, productComp22Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);
        // issue required quantities of components
        runAndAssertServiceSuccess("issueProductionRunTask", UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId));
        // complete the task
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunIdComp, "workEffortId", taskId, "userLogin", demowarehouse1));
        // produce the products
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productComp1Id, "workEffortId", prunIdComp, "quantity", new BigDecimal("80.0"), "userLogin", demowarehouse1));
        // complete production run (close it)
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunIdComp, "workEffortId", taskId, "userLogin", demowarehouse1));
        // check inventory
        inventoryAsserts.assertInventoriesChange(productComp1Id, new BigDecimal("80.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp2Id, productComp3Id, productComp11Id, productComp12Id, productComp13Id, productComp14Id, productComp21Id, productComp22Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);

        // get the production run for component 2
        mandatoryWorkEffortComp = EntityUtil.getOnly(delegator.findByAnd("WorkEffortGoodStandard", Arrays.asList(EntityCondition.makeCondition("workEffortId", EntityOperator.IN, mandatoryWorkEffortIds), EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUN_PROD_DELIV"), EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productComp2Id))));
        // get first component production run
        prunIdComp = mandatoryWorkEffortComp.getString("workEffortId");
        confirmProductionRun(prunIdComp);
        // get the one and only task, and starts it
        taskId = startOnlyTask(prunIdComp);
        // check the WEGS for the materials to issue (10x9x3 x productComp21Id, 10x9x4 x productComp22Id)
        assertWorkEffortHasWegs(taskId, "PRUNT_PROD_NEEDED", UtilMisc.toMap(productComp21Id, new BigDecimal("270.0"), productComp22Id, new BigDecimal("360.0")));
        // receive the material components into the warehouse
        receiveMaterial(productComp21Id, 270, 2.5);
        receiveMaterial(productComp22Id, 360, 2.5);
        inventoryAsserts.assertInventoriesChange(productComp1Id, new BigDecimal("80.0"), origProductInventories); // from before
        inventoryAsserts.assertInventoriesChange(productComp21Id, new BigDecimal("270.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp22Id, new BigDecimal("360.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp2Id, productComp3Id, productComp13Id, productComp14Id, productComp11Id, productComp12Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);
        // issue required quantities of components
        runAndAssertServiceSuccess("issueProductionRunTask", UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId));
        // complete the task
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunIdComp, "workEffortId", taskId, "userLogin", demowarehouse1));
        // produce the products
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productComp2Id, "workEffortId", prunIdComp, "quantity", new BigDecimal("90.0"), "userLogin", demowarehouse1));
        // complete production run (close it)
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunIdComp, "workEffortId", taskId, "userLogin", demowarehouse1));
        // check inventory
        inventoryAsserts.assertInventoriesChange(productComp1Id, new BigDecimal("80.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp2Id, new BigDecimal("90.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp3Id, productComp11Id, productComp12Id, productComp13Id, productComp14Id, productComp21Id, productComp22Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);

        // finally we can get the main production run going
        // get the one and only task, and starts it
        taskId = startOnlyTask(prunId);
        // check the WEGS for the materials to issue (10x8 x productComp1Id, 10x9 x productComp2Id, 10x5 x productComp3Id)
        assertWorkEffortHasWegs(taskId, "PRUNT_PROD_NEEDED", UtilMisc.toMap(productComp1Id, new BigDecimal("80.0"), productComp2Id, new BigDecimal("90.0"), productComp3Id, new BigDecimal("50.0")));
        // receive the material components into the warehouse
        receiveMaterial(productComp3Id, 50, 2.5);
        inventoryAsserts.assertInventoriesChange(productComp3Id,  new BigDecimal("50.0"),   origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp11Id, productComp12Id, productComp13Id, productComp14Id, productComp21Id, productComp22Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);
        // issue required quantities of components for alternative BOM
        runAndAssertServiceSuccess("issueProductionRunTask", UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId));
        // complete the task
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunId, "workEffortId", taskId, "userLogin", demowarehouse1));
        // produce the products
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productId, "workEffortId", prunId, "quantity", new BigDecimal("10.0"), "userLogin", demowarehouse1));
        // complete production run (close it)
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunId, "workEffortId", taskId, "userLogin", demowarehouse1));
        // check inventory
        inventoryAsserts.assertInventoriesChange(productId, new BigDecimal("15.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(Arrays.asList(productComp1Id, productComp2Id, productComp3Id, productComp11Id, productComp12Id, productComp13Id, productComp14Id, productComp21Id, productComp22Id, productComp23Id, productComp24Id), new BigDecimal("0.0"), origProductInventories);
    }

    /**
     * Tests disassembly for products with multiple BOM.
     * Creates a manufactured product with two BOM and test the default disassembly, and the disassembly specifying the alternate routing BOM,
     * to make sure that the raw materials from the correct BOM are received after disassembly.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMultipleBomDisassemble() throws Exception {
        // Create a test product
        final GenericValue product = createTestProduct("test Multiple BOM Disassemble Product", demowarehouse1);
        final String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);

        // Create a default routing, using createTestAssemblingRouting
        createTestAssemblingRouting("Default BOM for product [" + productId + "]", productId);

        // Create a default BOM with two components, using createBOMProductAssoc
        final GenericValue productComp1 = createTestProduct("test Default Material for [" + productId + "] - Component 1", demowarehouse1);
        final String productComp1Id = productComp1.getString("productId");
        final GenericValue productComp2 = createTestProduct("test Default Material for [" + productId + "] - Component 2", demowarehouse1);
        final String productComp2Id = productComp2.getString("productId");
        createBOMProductAssoc(productId, productComp1Id, new Long("10"), new BigDecimal("7.0"), admin);
        createBOMProductAssoc(productId, productComp2Id, new Long("11"), new BigDecimal("3.0"), admin);

        // Create an alternative routing, using createBOMProductAssoc
        final String alternateRoutingId = createTestAssemblingRouting("Alternate BOM for product [" + productId + "]", productId);
        final GenericValue productComp3 = createTestProduct("test Alternate Material for [" + productId + "] - Component 3", demowarehouse1);
        final String productComp3Id = productComp3.getString("productId");
        final GenericValue productComp4 = createTestProduct("test Alternate Material for [" + productId + "] - Component 4", demowarehouse1);
        final String productComp4Id = productComp4.getString("productId");
        final GenericValue productComp5 = createTestProduct("test Alternate Material for [" + productId + "] - Component 5", demowarehouse1);
        final String productComp5Id = productComp5.getString("productId");

        createBOMProductAssoc(productId, productComp3Id, alternateRoutingId, new Long("10"), new BigDecimal("1.0"), admin);
        createBOMProductAssoc(productId, productComp4Id, alternateRoutingId, new Long("11"), new BigDecimal("2.0"), admin);
        createBOMProductAssoc(productId, productComp5Id, alternateRoutingId, new Long("12"), new BigDecimal("5.0"), admin);

        // receive 13 x Product so we can disassemble them
        receiveMaterial(productId, 13, 100.0);

        // get initial inventory
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        Map<String, Map<String, Object>> origProductInventories = inventoryAsserts.getInventories(Arrays.asList(productId, productComp1Id, productComp2Id, productComp3Id, productComp4Id, productComp5Id));

        // Create a disassembly with default routing (not giving any routing ID) for 5 product
        String prunId = createDisassembly("test multiple BOM disassembly", 5, productId);
        // get the one and only task, and starts it
        String taskId = startOnlyTask(prunId);
        // issue 5 product
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "workEffortId", taskId, "productId", productId, "quantity", new BigDecimal("5.0")));
        // make sure WEGS is completed
        List wegsList = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", taskId, "productId", productId, "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"));
        assertTrue("WEGS for material requirement of 5 [" + productId + "] exists.", wegsList.size() == 1);
        GenericValue wegs = EntityUtil.getFirst(wegsList);
        assertEquals("Material Issuance failed:  [" + productId + "] WorkEffortGoodStandard was not marked completed.", wegs.getString("statusId"), "WEGS_COMPLETED");

        // complete the task, which should trigger production run completed
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("userLogin", demowarehouse1, "productionRunId", prunId, "workEffortId", taskId));
        // produce the default BOM components
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productComp1Id, "workEffortId", prunId, "quantity", new BigDecimal("35.0"), "userLogin", demowarehouse1));
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productComp2Id, "workEffortId", prunId, "quantity", new BigDecimal("15.0"), "userLogin", demowarehouse1));
        // complete production run (close it)
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("productionRunId", prunId, "workEffortId", taskId, "userLogin", demowarehouse1));

        // check inventory
        inventoryAsserts.assertInventoriesChange(productId, new BigDecimal("-5.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp1Id, new BigDecimal("35.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp2Id, new BigDecimal("15.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp3Id, new BigDecimal("0.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp4Id, new BigDecimal("0.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp5Id, new BigDecimal("0.0"), origProductInventories);

        // Create a disassembly with alternate routing (giving its routing ID)
        prunId = createDisassembly("test multiple BOM disassembly", 8, productId, alternateRoutingId);
        // get the one and only task, and starts it
        taskId = startOnlyTask(prunId);
        // issue 8 product
        runAndAssertServiceSuccess("issueProductionRunTaskComponent", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "workEffortId", taskId, "productId", productId, "quantity", new BigDecimal("8.0")));
        // make sure WEGS is completed
        wegsList = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", taskId, "productId", productId, "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"));
        assertTrue("WEGS for material requirement of 8 [" + productId + "] exists.", wegsList.size() == 1);
        wegs = EntityUtil.getFirst(wegsList);
        assertEquals("Material Issuance failed:  [" + productId + "] WorkEffortGoodStandard was not marked completed.", wegs.getString("statusId"), "WEGS_COMPLETED");
        // complete the task, which should trigger production run completed
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "productionRunId", prunId, "workEffortId", taskId));
        // produce the alternate BOM components
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productComp3Id, "workEffortId", prunId, "quantity", new BigDecimal("8.0"), "userLogin", demowarehouse1));
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productComp4Id, "workEffortId", prunId, "quantity", new BigDecimal("16.0"), "userLogin", demowarehouse1));
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("productId", productComp5Id, "workEffortId", prunId, "quantity", new BigDecimal("40.0"), "userLogin", demowarehouse1));
        // check inventory
        inventoryAsserts.assertInventoriesChange(productId, new BigDecimal("-13.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp1Id, new BigDecimal("35.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp2Id, new BigDecimal("15.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp3Id, new BigDecimal("8.0"),  origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp4Id, new BigDecimal("16.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange(productComp5Id, new BigDecimal("40.0"), origProductInventories);
    }

    /**
     * Produce marketing package GZ-BRACKET and verify inventory changes.
     * @throws Exception if an error occurs
     */
    public void testProduceMarketingPackage() throws Exception {
        // remember original inventory value for GZ-BRACKET, GZ-1000, GZ-1001 and GZ-1004
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        Map<String, Map<String, Object>> origProductInventories = inventoryAsserts.getInventories(Arrays.asList("GZ-BASKET", "GZ-1000", "GZ-1001", "GZ-1004"));

        // create production run of 2 GZ-BRACKET
        Map<String, Object> context = FastMap.newInstance();
        context.put("productId", "GZ-BASKET");
        context.put("pRQuantity", new BigDecimal("2.0"));
        context.put("startDate", UtilDateTime.nowTimestamp());
        context.put("facilityId", facilityId);
        context.put("userLogin", demowarehouse1);
        Map<String, Object> results = runAndAssertServiceSuccess("createProductionRun", context);
        String productionRunId = (String) results.get("productionRunId");

        // confirm production run
        confirmProductionRun(productionRunId);

        // run its task
        context.clear();
        context.put("productionRunId", productionRunId);
        context.put("userLogin", demowarehouse1);
        runAndAssertServiceSuccess("quickRunAllProductionRunTasks", context);

        // produce required number of GZ-BASKET and close production run
        context.clear();
        context.put("workEffortId", productionRunId);
        context.put("userLogin", demowarehouse1);
        runAndAssertServiceSuccess("opentaps.productionRunProduce", context);

        context.clear();
        context.put("productionRunId", productionRunId);
        context.put("statusId", "PRUN_CLOSED");
        context.put("userLogin", demowarehouse1);
        runAndAssertServiceSuccess("changeProductionRunStatus", context);

        // verify inventory changes
        // initially we have no materials, that's why ATP changes are zero and QOH changes are negative.
        inventoryAsserts.assertInventoriesChange("GZ-BASKET", new BigDecimal("2.0"), new BigDecimal("2.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange("GZ-1000", new BigDecimal("2.0").negate(), new BigDecimal("0.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange("GZ-1001", new BigDecimal("4.0").negate(), new BigDecimal("0.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange("GZ-1004", new BigDecimal("6.0").negate(), new BigDecimal("0.0"), origProductInventories);

        // create order for 2 produced marketing packages
        Map<GenericValue, BigDecimal> orderItems = FastMap.newInstance();
        orderItems.put(delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "GZ-BASKET")), new BigDecimal("2.0"));
        User = admin;
        SalesOrderFactory orderFactory = testCreatesSalesOrder(orderItems, delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoAccount1")), "9000");
        String orderId = orderFactory.getOrderId();
        orderFactory.approveOrder();

        // cancel GZ-BASKET item
        cancelOrderItem(orderId, "00001", "00001", new BigDecimal("2.0"), delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager")));

        // verify final inventory changes
        inventoryAsserts.assertInventoriesChange("GZ-BASKET", new BigDecimal("0.0"), new BigDecimal("0.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange("GZ-1000", new BigDecimal("0.0"), new BigDecimal("2.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange("GZ-1001", new BigDecimal("0.0"), new BigDecimal("4.0"), origProductInventories);
        inventoryAsserts.assertInventoriesChange("GZ-1004", new BigDecimal("0.0"), new BigDecimal("6.0"), origProductInventories);

    }

}
