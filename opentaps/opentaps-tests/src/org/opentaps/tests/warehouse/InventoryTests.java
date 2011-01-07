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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.base.entities.InventoryItemTraceDetail;
import org.opentaps.base.entities.WorkEffortInventoryProduced;
import org.opentaps.domain.inventory.InventoryItem;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.manufacturing.ManufacturingRepositoryInterface;
import org.opentaps.domain.manufacturing.ProductionRun;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.tests.financials.FinancialAsserts;
import org.opentaps.tests.financials.FinancialsTestCase;

// testNonSerializedReceipt specification
final class NonSerializedTestSpecs {
    public static String inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
    public static BigDecimal itemCost = new BigDecimal("5.67");
    public static String currencyUomId = "USD";
    public static BigDecimal quantity = new BigDecimal("100.0");
};

// testSerializedReceipt specification
final class SerializedTestSpecs {
    public static String inventoryItemTypeId = "SERIALIZED_INV_ITEM";
    public static List<BigDecimal> itemCosts = Arrays.asList(new BigDecimal("8.0"), new BigDecimal("9.0"), new BigDecimal("10.0"));
    public static String currencyUomId = "USD";
    public static BigDecimal quantity = new BigDecimal("1.0");
};

public class InventoryTests extends FinancialsTestCase {
    private static final String MODULE = InventoryTests.class.getName();
    /** UserLogin for running the tests. */
    public GenericValue demowarehouse1;
    public GenericValue admin;
    public GenericValue DemoCustomer;

    /** Facility to operate in. */
    public final String facilityId = "WebStoreWarehouse";

    /** Company of the facility */
    public final String organizationPartyId = "Company";

    /** Initialization for inventory transfer tests. */
    public static final String FROM_ORGANIZATION = "Company";
    public static final String TO_ORGANIZATION = "CompanySub1";
    public static final String FROM_FACILITY = "WebStoreWarehouse";
    public static final String TO_FACILTY = "MyRetailStore";
    public static final String PRODUCT_STORE = "9000";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        demowarehouse1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        admin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
        DemoCustomer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoCustomer"));
    }

    @Override
    public void tearDown() {
        demowarehouse1 = null;
    }

    /**
     * Default constructor.
     */
    public InventoryTests() {
        super();
    }

    /**
     * Tests inventory receipt for non-serialized product.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testNonSerializedReceipt() throws GeneralException {
        Map<String, Object> input = null;
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // create test product
        GenericValue product = createTestProduct("testNonSerializedReceipt Product", demowarehouse1);
        String productId = product.getString("productId");

        // Notes various initial data before tests run.
        Timestamp now = UtilDateTime.nowTimestamp();
        Map originalInventory = inventoryAsserts.getInventory(productId);
        Map initialBalances = financialAsserts.getFinancialBalances(now);

        // receive inventory item
        input = new HashMap<String, Object>();
        input.put("inventoryItemTypeId", NonSerializedTestSpecs.inventoryItemTypeId);
        input.put("productId", productId);
        input.put("facilityId", facilityId);
        input.put("unitCost", NonSerializedTestSpecs.itemCost);
        input.put("currencyUomId", NonSerializedTestSpecs.currencyUomId);
        input.put("datetimeReceived", now);
        input.put("quantityAccepted", NonSerializedTestSpecs.quantity);
        input.put("quantityRejected", BigDecimal.ZERO);
        input.put("userLogin", demowarehouse1);
        Map output = runAndAssertServiceSuccess("receiveInventoryProduct", input);

        // check if inventory grows as expected
        inventoryAsserts.assertInventoryChange(productId, NonSerializedTestSpecs.quantity, originalInventory);

        // make sure the inventory item produced has the right unit cost and currency
        String inventoryItemId = (String) output.get("inventoryItemId");
        inventoryAsserts.assertInventoryItemUnitCost(inventoryItemId, NonSerializedTestSpecs.itemCost, NonSerializedTestSpecs.currencyUomId);

        // make sure accounting and inventory items values equals
        inventoryAsserts.assertInventoryValuesEqual(productId);

        // assert transaction equivalence with the reference transaction
        List<EntityExpr> conditions = new ArrayList<EntityExpr>();
        conditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
        conditions.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
        conditions.add(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "SHIPMENT_RCPT_ATX"));
        Set<String> acctgTransIds = getAcctgTransSinceDate(EntityCondition.makeCondition(conditions, EntityOperator.AND), now, delegator);
        Set<String> acctgTransTemplateIds = UtilMisc.toSet("INV_RCV_NS_TEST-1");
        assertTransactionEquivalence(acctgTransIds, acctgTransTemplateIds);

        // gets current balances and check theirs correctness for given accounts by comparing with initial values.
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal expectedDelta = NonSerializedTestSpecs.itemCost.multiply(NonSerializedTestSpecs.quantity);
        Map expectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", expectedDelta.toString(), "UNINVOICED_SHIP_RCPT", expectedDelta.negate().toString());
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);

    }

    /**
     * Tests inventory receipt for serialized product.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testSerializedReceipt() throws GeneralException {
        Map<String, Object> template = null;
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // create test product
        GenericValue product = createTestProduct("testSerializedReceipt Product", demowarehouse1);
        String productId = product.getString("productId");

        // Note various initial data before tests run.
        Timestamp now = UtilDateTime.nowTimestamp();
        Map originalInventory = inventoryAsserts.getInventory(productId);
        Map initialBalances = financialAsserts.getFinancialBalances(now);

        // receive the same product tree times at different cost
        template = new HashMap<String, Object>();
        template.put("inventoryItemTypeId", SerializedTestSpecs.inventoryItemTypeId);
        template.put("productId", productId);
        template.put("facilityId", facilityId);
        template.put("currencyUomId", SerializedTestSpecs.currencyUomId);
        template.put("datetimeReceived", now);
        template.put("quantityRejected", BigDecimal.ZERO);
        template.put("quantityAccepted", SerializedTestSpecs.quantity);
        template.put("userLogin", demowarehouse1);

        Map<String, Object> input = null;

        List<Map<String, Object>> inventoryItems = new ArrayList<Map<String, Object>>();

        for (BigDecimal unitCost : SerializedTestSpecs.itemCosts) {
            input = template;
            input.put("unitCost", unitCost);
            Map output = runAndAssertServiceSuccess("receiveInventoryProduct", input);
            String inventoryItemId = (String) output.get("inventoryItemId");
            inventoryItems.add(UtilMisc.<String, Object>toMap("inventoryItemId", inventoryItemId, "unitCost", unitCost));
            // make sure accounting and inventory items values equals
            inventoryAsserts.assertInventoryValuesEqual(productId);
        }

        // check if inventory grows as expected
        inventoryAsserts.assertInventoryChange(productId, BigDecimal.valueOf(3), originalInventory);

        // make sure each inventory item produced has the right unit cost and currency
        for (Map<String, Object> inventoryItem : inventoryItems) {
            String inventoryItemId = (String) inventoryItem.get("inventoryItemId");
            BigDecimal unitCost = (BigDecimal) inventoryItem.get("unitCost");
            inventoryAsserts.assertInventoryItemUnitCost(inventoryItemId, unitCost, SerializedTestSpecs.currencyUomId);
        }

        // assert transactions equivalence with the reference transactions
        Set<String> acctgTransIds = getAcctgTransSinceDate(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "INVENTORY"), now, delegator);
        assertTransactionEquivalence(acctgTransIds, UtilMisc.toSet("INV_RCV_SER_TEST-1", "INV_RCV_SER_TEST-2", "INV_RCV_SER_TEST-3"));

        // gets current balances and check theirs correctness for given accounts by comparing with initial values.
        Map finalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        BigDecimal expectedDelta = BigDecimal.ZERO;
        for (BigDecimal theCost : SerializedTestSpecs.itemCosts) {
            expectedDelta = expectedDelta.add(theCost);
        }
        expectedDelta = expectedDelta.multiply(SerializedTestSpecs.quantity);
        Map expectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", expectedDelta.toString(), "UNINVOICED_SHIP_RCPT", expectedDelta.negate().toString());
        expectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, expectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedBalanceChanges);
    }

    /**
     * Test transferring a non serialized inventory item from one facility to another.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testNonSerializedInventoryTransfer() throws GeneralException {
        InventoryAsserts fromInventoryAsserts = new InventoryAsserts(this, FROM_FACILITY, organizationPartyId, demowarehouse1);
        InventoryAsserts toInventoryAsserts = new InventoryAsserts(this, TO_FACILTY, organizationPartyId, demowarehouse1);
        FinancialAsserts fromFinancialAsserts = new FinancialAsserts(this, FROM_ORGANIZATION, demofinadmin);
        FinancialAsserts toFinancialAsserts = new FinancialAsserts(this, TO_ORGANIZATION, demofinadmin);

        // create test product
        GenericValue product = createTestProduct("testNonSerializedInventoryTransfer Product", demowarehouse1);
        String productId = product.getString("productId");

        // note the initial inventory quantity and values
        Map originalFromFacilityInventory = fromInventoryAsserts.getInventory(productId);
        Map originalToFacilityInventory = toInventoryAsserts.getInventory(productId);

        // Receive 5.0 at $10 per unit into WebStoreWarehouse non-serialized
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("facilityId", FROM_FACILITY);
        input.put("productId", productId);
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        input.put("quantityAccepted", new BigDecimal("5.0"));
        input.put("quantityRejected", new BigDecimal("0.0"));
        input.put("datetimeReceived", UtilDateTime.nowTimestamp());
        input.put("unitCost", new BigDecimal("10.00"));
        Map result = runAndAssertServiceSuccess("receiveInventoryProduct", input);
        String inventoryItemId = (String) result.get("inventoryItemId");

        // make sure the inventory went up by 5 in WebStoreWarehouse
        fromInventoryAsserts.assertInventoryChange(productId, new BigDecimal("5.0"), originalFromFacilityInventory);
        pause("allow distinct transactions inventory timestamps");

        // note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();

        // note the initial balances
        Map fromInitialBalances = fromFinancialAsserts.getFinancialBalances(start);
        Map toInitialBalances = toFinancialAsserts.getFinancialBalances(start);

        // get the product inventory data again
        Map fromFacilityInventory = fromInventoryAsserts.getInventory(productId);

        // Create an inventory transfer for all 5.0 from WebStoreWarehouse to MyRetailStore
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("facilityId", FROM_FACILITY);
        input.put("facilityIdTo", TO_FACILTY);
        input.put("inventoryItemId", inventoryItemId);
        input.put("statusId", "IXF_SCHEDULED");
        input.put("xferQty", 5.0);
        result = runAndAssertServiceSuccess("createInventoryTransfer", input);
        String inventoryTransferId  = (String) result.get("inventoryTransferId");

        // verify that the ATP in FROM_FACILITY has decreased by 5, but the QOH is unchanged
        fromInventoryAsserts.assertInventoryChange(productId, BigDecimal.ZERO, new BigDecimal("-5.0"), fromFacilityInventory);

        // Complete the inventory transfer
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("inventoryTransferId", inventoryTransferId);
        runAndAssertServiceSuccess("completeInventoryTransfer", input);

        // make sure the inventory went up by 5 in RetailWarehouse and did not change in WebStoreWarehouse
        fromInventoryAsserts.assertInventoryChange(productId, new BigDecimal("0.0"), originalFromFacilityInventory);
        toInventoryAsserts.assertInventoryChange(productId, new BigDecimal("5.0"), originalToFacilityInventory);

        // get the transactions since starting
        Set<String> transactions = getAcctgTransSinceDate(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "INVENTORY"), start, delegator);
        assertNotEmpty("Inventory transfer transaction not created.", transactions);

        // assert transaction equivalence with the reference transaction
        assertTransactionEquivalence(transactions, UtilMisc.toList("INV_XFER_TEST-1"));

        // verify that the difference in balances work out
        Map fromFinalBalances = fromFinancialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map toFinalBalances = toFinancialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // For organization "Company", balance of INVENTORY changes by -50 and balance of INEVNTORY_XFER_OUT account changes by 50
        Map fromExpectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "-50", "INVENTORY_XFER_OUT", "50");
        fromExpectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(FROM_ORGANIZATION, fromExpectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(fromInitialBalances, fromFinalBalances, fromExpectedBalanceChanges);

        // For organization "CompanySub1", balance of INVENTORY account by 50 and balance of INVENTORY_XFER_IN account changes by -50
        Map toExpectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "50", "INVENTORY_XFER_IN", "-50");
        toExpectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(TO_ORGANIZATION, toExpectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(toInitialBalances, toFinalBalances, toExpectedBalanceChanges);
    }

    /**
     * Test transferring a non serialized inventory item from one facility to another generated the corresponding accounting variance with tags
     * on the transactions and destination inventory item, from the original inventory item tags.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testNonSerializedInventoryTransferWithAccountingTags() throws GeneralException {

        // 1. create test product
        GenericValue product1 = createTestProduct("testInventoryItemRevaluationWithAccountingTags Product", demowarehouse1);
        String productId1 = product1.getString("productId");

        InventoryAsserts fromInventoryAsserts = new InventoryAsserts(this, FROM_FACILITY, organizationPartyId, demowarehouse1);
        InventoryAsserts toInventoryAsserts = new InventoryAsserts(this, TO_FACILTY, organizationPartyId, demowarehouse1);
        FinancialAsserts fromFinancialAsserts = new FinancialAsserts(this, FROM_ORGANIZATION, demofinadmin);
        FinancialAsserts toFinancialAsserts = new FinancialAsserts(this, TO_ORGANIZATION, demofinadmin);

        // note the initial inventory quantity and values
        Map originalFromFacilityInventory = fromInventoryAsserts.getInventory(productId1);
        Map originalToFacilityInventory = toInventoryAsserts.getInventory(productId1);

        // Receive 5.0 at $10 per unit into WebStoreWarehouse non-serialized
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("facilityId", FROM_FACILITY);
        input.put("productId", productId1);
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        input.put("quantityAccepted", new BigDecimal("5.0"));
        input.put("quantityRejected", new BigDecimal("0.0"));
        input.put("datetimeReceived", UtilDateTime.nowTimestamp());
        input.put("unitCost", new BigDecimal("10.00"));
        // tags
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("acctgTagEnumId1", "DIV_GOV");
        tags.put("acctgTagEnumId2", "DPT_CORPORATE");
        input.putAll(tags);
        Map result = runAndAssertServiceSuccess("receiveInventoryProduct", input);
        String inventoryItemId = (String) result.get("inventoryItemId");

        // make sure the inventory went up by 5 in WebStoreWarehouse
        fromInventoryAsserts.assertInventoryChange(productId1, new BigDecimal("5.0"), originalFromFacilityInventory);
        pause("allow distinct transactions inventory timestamps");

        // check the accounting transactions are tagged
        List<GenericValue> entries1 = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("productId", productId1));
        assertNotEmpty("Accounting transaction entries not found for product " + product1, entries1);
        for (GenericValue entry : entries1) {
            assertAccountingTagsEqual(entry, tags);
        }

        // note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();

        // note the initial balances
        Map fromInitialBalances = fromFinancialAsserts.getFinancialBalances(start);
        Map toInitialBalances = toFinancialAsserts.getFinancialBalances(start);

        // get the product inventory data again
        Map fromFacilityInventory = fromInventoryAsserts.getInventory(productId1);

        // Create an inventory transfer for all 5.0 from WebStoreWarehouse to MyRetailStore
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("facilityId", FROM_FACILITY);
        input.put("facilityIdTo", TO_FACILTY);
        input.put("inventoryItemId", inventoryItemId);
        input.put("statusId", "IXF_SCHEDULED");
        input.put("xferQty", 5.0);
        result = runAndAssertServiceSuccess("createInventoryTransfer", input);
        String inventoryTransferId  = (String) result.get("inventoryTransferId");

        // verify that the ATP in FROM_FACILITY has decreased by 5, but the QOH is unchanged
        fromInventoryAsserts.assertInventoryChange(productId1, BigDecimal.ZERO, new BigDecimal("-5.0"), fromFacilityInventory);

        // Complete the inventory transfer
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("inventoryTransferId", inventoryTransferId);
        runAndAssertServiceSuccess("completeInventoryTransfer", input);

        // check the inventory items are tagged (destination item created by the transfer and original item)
        entries1 = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId1));
        assertNotEmpty("InventoryItem not found for product " + product1, entries1);
        for (GenericValue entry : entries1) {
            assertAccountingTagsEqual(entry, tags);
        }

        // make sure the inventory went up by 5 in RetailWarehouse and did not change in WebStoreWarehouse
        fromInventoryAsserts.assertInventoryChange(productId1, new BigDecimal("0.0"), originalFromFacilityInventory);
        toInventoryAsserts.assertInventoryChange(productId1, new BigDecimal("5.0"), originalToFacilityInventory);

        // get the transactions since starting
        Set<String> transactions = getAcctgTransSinceDate(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "INVENTORY"), start, delegator);
        assertNotEmpty("Inventory transfer transaction not created.", transactions);

        // check the accounting transactions are tagged
        entries1 = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("productId", productId1));
        assertNotEmpty("Accounting transaction entries not found for product " + product1, entries1);
        for (GenericValue entry : entries1) {
            assertAccountingTagsEqual(entry, tags);
        }

        // verify that the difference in balances work out
        Map fromFinalBalances = fromFinancialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map toFinalBalances = toFinancialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // For organization "Company", balance of INVENTORY changes by -50 and balance of INEVNTORY_XFER_OUT account changes by 50
        Map fromExpectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "-50", "INVENTORY_XFER_OUT", "50");
        fromExpectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(FROM_ORGANIZATION, fromExpectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(fromInitialBalances, fromFinalBalances, fromExpectedBalanceChanges);

        // For organization "CompanySub1", balance of INVENTORY account by 50 and balance of INVENTORY_XFER_IN account changes by -50
        Map toExpectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "50", "INVENTORY_XFER_IN", "-50");
        toExpectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(TO_ORGANIZATION, toExpectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(toInitialBalances, toFinalBalances, toExpectedBalanceChanges);
    }

    /**
     * Test transferring part of a non serialized inventory item from one facility to another.
     * This effectively split the original <code>InventoryItem</code> into two.
     * Also test that the created <code>InventoryItem</code> has its <code>parentInventoryItemId</code> set to the original <code>InventoryItem</code>.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testNonSerializedSplitInventoryTransfer() throws GeneralException {
        InventoryAsserts fromInventoryAsserts = new InventoryAsserts(this, FROM_FACILITY, organizationPartyId, demowarehouse1);
        InventoryAsserts toInventoryAsserts = new InventoryAsserts(this, TO_FACILTY, organizationPartyId, demowarehouse1);
        FinancialAsserts fromFinancialAsserts = new FinancialAsserts(this, FROM_ORGANIZATION, demofinadmin);
        FinancialAsserts toFinancialAsserts = new FinancialAsserts(this, TO_ORGANIZATION, demofinadmin);

        // test specifications
        final double receiveQty = 50.0;
        final double xferQty = 30.0;
        final double remainQty = receiveQty - xferQty;
        final double unitCost = 10.0;

        // create test product
        GenericValue product = createTestProduct("testNonSerializedSplitInventoryTransfer Product", demowarehouse1);
        String productId = product.getString("productId");

        // note the initial inventory quantity and values
        Map originalFromFacilityInventory = fromInventoryAsserts.getInventory(productId);
        Map originalToFacilityInventory = toInventoryAsserts.getInventory(productId);

        // Receive 50.0 of at $10 per unit into WebStoreWarehouse non-serialized
        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("facilityId", FROM_FACILITY);
        input.put("productId", productId);
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        input.put("quantityAccepted", new BigDecimal(receiveQty));
        input.put("quantityRejected", new BigDecimal("0.0"));
        input.put("datetimeReceived", UtilDateTime.nowTimestamp());
        input.put("unitCost", new BigDecimal(unitCost));
        Map result = runAndAssertServiceSuccess("receiveInventoryProduct", input);
        String inventoryItemId = (String) result.get("inventoryItemId");

        // make sure the inventory went up by the received quantity in WebStoreWarehouse
        fromInventoryAsserts.assertInventoryChange(productId, new BigDecimal(receiveQty), originalFromFacilityInventory);
        pause("allow distinct transactions inventory timestamps");

        // note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();

        // note the initial balances
        Map fromInitialBalances = fromFinancialAsserts.getFinancialBalances(start);
        Map toInitialBalances = toFinancialAsserts.getFinancialBalances(start);

        // get the product inventory data again
        Map fromFacilityInventory = fromInventoryAsserts.getInventory(productId);

        // Create an inventory transfer for 30.0 from WebStoreWarehouse to MyRetailStore
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("facilityId", FROM_FACILITY);
        input.put("facilityIdTo", TO_FACILTY);
        input.put("inventoryItemId", inventoryItemId);
        input.put("statusId", "IXF_SCHEDULED");
        input.put("xferQty", xferQty);
        result = runAndAssertServiceSuccess("createInventoryTransfer", input);
        String inventoryTransferId = (String) result.get("inventoryTransferId");

        // verify that the ATP in FROM_FACILITY has decreased by the transferred quantity, but the QOH is unchanged
        fromInventoryAsserts.assertInventoryChange(productId, BigDecimal.ZERO, new BigDecimal(-xferQty), fromFacilityInventory);

        // Complete the inventory transfer
        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("inventoryTransferId", inventoryTransferId);
        runAndAssertServiceSuccess("completeInventoryTransfer", input);

        // make sure the inventory went up by the transferred quantity in RetailWarehouse and up by the remaining quantity in WebStoreWarehouse
        fromInventoryAsserts.assertInventoryChange(productId, new BigDecimal(remainQty), originalFromFacilityInventory);
        toInventoryAsserts.assertInventoryChange(productId, new BigDecimal(xferQty), originalToFacilityInventory);

        // get the transactions since starting
        Set<String> transactions = getAcctgTransSinceDate(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "INVENTORY"), start, delegator);
        assertNotEmpty("Inventory transfer transaction not created.", transactions);

        // verify that the difference in balances work out
        Map fromFinalBalances = fromFinancialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map toFinalBalances = toFinancialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // For organization "Company", balance of INVENTORY changes by -(transferred quantity * unit cost) -300 and balance of INVENTORY_XFER_OUT account changes by (transferred quantity * unit cost) 300
        Map fromExpectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "-300", "INVENTORY_XFER_OUT", "300");
        fromExpectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(FROM_ORGANIZATION, fromExpectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(fromInitialBalances, fromFinalBalances, fromExpectedBalanceChanges);

        // For organization "CompanySub1", balance of INVENTORY account by (transferred quantity * unit cost) 300 and balance of INVENTORY_XFER_IN account changes by -(transferred quantity * unit cost) -300
        Map toExpectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "300", "INVENTORY_XFER_IN", "-300");
        toExpectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(TO_ORGANIZATION, toExpectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(toInitialBalances, toFinalBalances, toExpectedBalanceChanges);

        // verify that the transferred inventory item has its parentInventoryItemId set
        GenericValue splitItem = EntityUtil.getFirst(delegator.findByAnd("InventoryItem", UtilMisc.toMap("parentInventoryItemId", inventoryItemId)));
        assertNotNull("Split InventoryItem does not have the parentInventoryItemId set.", splitItem);
    }


    /**
     * Tests transferring serialized inventory items.
     *
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testSerializedInventoryTransfer() throws GeneralException {
        InventoryAsserts fromInventoryAsserts = new InventoryAsserts(this, FROM_FACILITY, organizationPartyId, demowarehouse1);
        InventoryAsserts toInventoryAsserts = new InventoryAsserts(this, TO_FACILTY, organizationPartyId, demowarehouse1);
        FinancialAsserts fromFinancialAsserts = new FinancialAsserts(this, FROM_ORGANIZATION, demofinadmin);
        FinancialAsserts toFinancialAsserts = new FinancialAsserts(this, TO_ORGANIZATION, demofinadmin);

        // create test product
        GenericValue product = createTestProduct("testSerializedSplitInventoryTransfer Product", demowarehouse1);
        String productId = product.getString("productId");

        // note the initial inventory quantity and values
        Map originalFromFacilityInventory = fromInventoryAsserts.getInventory(productId);
        Map originalToFacilityInventory = toInventoryAsserts.getInventory(productId);

        // Receive 5.0 of at $10 per unit into WebStoreWarehouse serialized
        Map template = UtilMisc.toMap("userLogin", demowarehouse1);
        template.put("facilityId", FROM_FACILITY);
        template.put("productId", productId);
        template.put("inventoryItemTypeId", "SERIALIZED_INV_ITEM");
        template.put("quantityAccepted", new BigDecimal("1.0"));
        template.put("quantityRejected", new BigDecimal("0.0"));
        template.put("datetimeReceived", UtilDateTime.nowTimestamp());
        template.put("unitCost", new BigDecimal("10.00"));
        List<String> inventoryItemIdList = new FastList();
        for (int idx = 1; idx <= 5; idx++) {
            Map input = template;
            input.put("serialNumber", "0000" + idx);
            Map result = runAndAssertServiceSuccess("receiveInventoryProduct", input);
            String inventoryItemId = (String) result.get("inventoryItemId");
            inventoryItemIdList.add(inventoryItemId);
        }

        // make sure the inventory went up by 5 in WebStoreWarehouse
        fromInventoryAsserts.assertInventoryChange(productId, new BigDecimal("5.0"), originalFromFacilityInventory);

        // note the time so we can easily find the transaction
        Timestamp start = UtilDateTime.nowTimestamp();

        // note the initial balances
        Map fromInitialBalances = fromFinancialAsserts.getFinancialBalances(start);
        Map toInitialBalances = toFinancialAsserts.getFinancialBalances(start);

        // to track the inventory between each transfer
        Map fromFacilityInventory;

        // Create an inventory transfer for all 5.0 from WebStoreWarehouse to MyRetailStore
        template = UtilMisc.toMap("userLogin", demowarehouse1);
        template.put("facilityId", FROM_FACILITY);
        template.put("facilityIdTo", TO_FACILTY);
        template.put("statusId", "IXF_SCHEDULED");
        template.put("xferQty", 1.0);
        for (String inventoryItemId : inventoryItemIdList) {
            pause("Workaround pause for MySQL");
            // note the time so we can easily find the transaction
            Timestamp startTransfer = UtilDateTime.nowTimestamp();
            // get inventory before the transfer
            fromFacilityInventory = fromInventoryAsserts.getInventory(productId);

            Map createTransferInput = template;
            createTransferInput.put("inventoryItemId", inventoryItemId);
            Map result = runAndAssertServiceSuccess("createInventoryTransfer", createTransferInput);
            String inventoryTransferId  = (String) result.get("inventoryTransferId");

            // verify that ATP in the from facility has decreased by 1.0 after creating this inventory transfer, but QOH is unchanged
            fromInventoryAsserts.assertInventoryChange(productId, BigDecimal.ZERO, new BigDecimal("-1.0"), fromFacilityInventory);

            Map completeTransferInput = UtilMisc.toMap("userLogin", demowarehouse1);
            completeTransferInput.put("inventoryTransferId", inventoryTransferId);

            // complete the inventory transfer
            runAndAssertServiceSuccess("completeInventoryTransfer", completeTransferInput);
            pause("allow distinct transactions inventory timestamps");

            // get the transactions since starting
            Set<String> transactions = getAcctgTransSinceDate(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "INVENTORY"), startTransfer, delegator);
            assertNotEmpty("Inventory transfer transaction not created.", transactions);

            // make sure accounting and inventory items values equals
            fromInventoryAsserts.assertInventoryValuesEqual(productId);
            toInventoryAsserts.assertInventoryValuesEqual(productId);

            // assert transaction equivalence with the reference transaction
            assertTransactionEquivalence(transactions, UtilMisc.toList("INV_XFER_TEST-2"));
        }

        // make sure the inventory went up by 5 in RetailWarehouse and did not change in WebStoreWarehouse
        fromInventoryAsserts.assertInventoryChange(productId, new BigDecimal("0.0"), originalFromFacilityInventory);
        toInventoryAsserts.assertInventoryChange(productId, new BigDecimal("5.0"), originalToFacilityInventory);

        // verify that the difference in balances work out
        Map fromFinalBalances = fromFinancialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Map toFinalBalances = toFinancialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());

        // For organization "Company", balance of INVENTORY changes by -50 and balance of INEVNTORY_XFER_OUT account changes by 50
        Map fromExpectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "-50", "INVENTORY_XFER_OUT", "50");
        fromExpectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(FROM_ORGANIZATION, fromExpectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(fromInitialBalances, fromFinalBalances, fromExpectedBalanceChanges);

        // For organization "CompanySub1", balance of INVENTORY account by 50 and balance of INVENTORY_XFER_IN account changes by -50
        Map toExpectedBalanceChanges = UtilMisc.toMap("INVENTORY_ACCOUNT", "50", "INVENTORY_XFER_IN", "-50");
        toExpectedBalanceChanges = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(TO_ORGANIZATION, toExpectedBalanceChanges, delegator);
        assertMapDifferenceCorrect(toInitialBalances, toFinalBalances, toExpectedBalanceChanges);
    }

    /**
     * Tests the consequences of non-serialized inventory receipt and variance.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testNonSerializedInventoryVariance() throws GeneralException {

        // create test product
        GenericValue product = createTestProduct("testNonSerializedInventoryVariance Product", demowarehouse1);
        String productId = product.getString("productId");

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demowarehouse1);

        String inventoryGlAccountId = financialAsserts.getInventoryGlAccountId();

        // Receive 10.0 non-serialized units at $9.87 per unit into WebStoreWarehouse
        Map receiveContext = UtilMisc.toMap("userLogin", demowarehouse1, "facilityId", FROM_FACILITY, "productId", productId, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        receiveContext.put("quantityAccepted", new BigDecimal("10.0"));
        receiveContext.put("quantityRejected", new BigDecimal("0.0"));
        receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
        receiveContext.put("unitCost", new BigDecimal("9.87"));
        Map receiveResult = runAndAssertServiceSuccess("receiveInventoryProduct", receiveContext);
        String receiveInventoryItemId = (String) receiveResult.get("inventoryItemId");

        // Determine inventory level and account balance before inventory variances
        Map originalInventory = inventoryAsserts.getInventory(productId);
        Map<String, Number> originalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number originalInventoryAccountBalance = originalBalances.get(inventoryGlAccountId) != null ? originalBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;

        // Create an inventory variance for -1 of ATP and -1 of QOH as "Damaged"
        Map<String, Object> varianceContext = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "availableToPromiseVar", new BigDecimal("-1.0"), "quantityOnHandVar", new BigDecimal("-1.0"), "varianceReasonId", "VAR_DAMAGED");
        Map varianceResult = runAndAssertServiceSuccess("createPhysicalInventoryAndVariance", varianceContext);
        String damagedPhysicalInventoryItemId = (String) varianceResult.get("physicalInventoryId");

        // Create an inventory variance for -2 of ATP and -2 of QOH as "Lost/Damaged in Transit"
        varianceContext = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "availableToPromiseVar", new BigDecimal("-2.0"), "quantityOnHandVar", new BigDecimal("-2.0"), "varianceReasonId", "VAR_TRANSIT");
        varianceResult = runAndAssertServiceSuccess("createPhysicalInventoryAndVariance", varianceContext);
        String transitPhysicalInventoryItemId = (String) varianceResult.get("physicalInventoryId");

        // Verify that ATP and QOH decreased by 3.0 in WebStoreWarehouse
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("-3.0"), originalInventory);

        // make sure accounting and inventory items values equals
        inventoryAsserts.assertInventoryValuesEqual(productId);

        // Verify that the INVENTORY_ACCOUNT value of balance sheet for Company decreased by $29.61
        Number expectedBalance = (new BigDecimal(originalInventoryAccountBalance.toString())).subtract(new BigDecimal("29.61")).setScale(DECIMALS, ROUNDING);
        Map<String, Number> newBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number newInventoryAccountBalance = newBalances.get(inventoryGlAccountId) != null ? newBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        Number newBalance = (new BigDecimal(newInventoryAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        inventoryAsserts.assertEquals("Unexpected balance for inventory GlAccount: ", expectedBalance, newBalance);

        // Verify that accounting transactions are equivalent to those of INV_VAR_TEST-1, INV_VAR_TEST-2
        /*

        This may cause problems because AcctTransEntries may have unpredictable amounts due to changes in product average cost
         */
        List cond = UtilMisc.toList(EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, receiveInventoryItemId),
                EntityCondition.makeCondition("physicalInventoryId", EntityOperator.IN, UtilMisc.toList(damagedPhysicalInventoryItemId, transitPhysicalInventoryItemId)));
        List<GenericValue> newAcctgTrans = delegator.findByCondition("AcctgTrans", EntityCondition.makeCondition(cond, EntityOperator.AND), null, null);
        List<String> referenceAcctgTrans = UtilMisc.toList("INV_VAR_TEST-1", "INV_VAR_TEST-2");
        assertTransactionEquivalence(newAcctgTrans, referenceAcctgTrans);
    }

    /**
     * Tests the consequences of non-serialized inventory receipt and variance with tags from the inventory item.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testNonSerializedInventoryVarianceWithAccountingTags() throws GeneralException {

        // 1. create test product
        GenericValue product1 = createTestProduct("testNonSerializedInventoryVariance WithAccountingTags Product", demowarehouse1);
        String productId1 = product1.getString("productId");

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demowarehouse1);

        String inventoryGlAccountId = financialAsserts.getInventoryGlAccountId();

        // Receive 10.0 non-serialized units at $10.0 per unit into WebStoreWarehouse
        Map receiveContext = UtilMisc.toMap("userLogin", demowarehouse1, "facilityId", FROM_FACILITY, "productId", productId1, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        receiveContext.put("quantityAccepted", new BigDecimal("10.0"));
        receiveContext.put("quantityRejected", new BigDecimal("0.0"));
        receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
        receiveContext.put("unitCost", new BigDecimal("10.00"));
        // tags
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("acctgTagEnumId1", "DIV_GOV");
        tags.put("acctgTagEnumId2", "DPT_CORPORATE");
        receiveContext.putAll(tags);
        Map receiveResult = runAndAssertServiceSuccess("receiveInventoryProduct", receiveContext);
        String receiveInventoryItemId = (String) receiveResult.get("inventoryItemId");

        // check the accounting transactions from the receipt are tagged
        List<GenericValue> entries1 = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("productId", productId1));
        assertNotEmpty("Accounting transaction entries not found for product " + product1, entries1);
        for (GenericValue entry : entries1) {
            assertAccountingTagsEqual(entry, tags);
        }

        // Determine inventory level and account balance before inventory variances
        Map originalInventory = inventoryAsserts.getInventory(productId1);
        Map<String, Number> originalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number originalInventoryAccountBalance = originalBalances.get(inventoryGlAccountId) != null ? originalBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;

        // Create an inventory variance for -1 of ATP and -1 of QOH as "Damaged"
        Map<String, Object> varianceContext = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "availableToPromiseVar", new BigDecimal("-1.0"), "quantityOnHandVar", new BigDecimal("-1.0"), "varianceReasonId", "VAR_DAMAGED");
        runAndAssertServiceSuccess("createPhysicalInventoryAndVariance", varianceContext);

        // Create an inventory variance for -2 of ATP and -2 of QOH as "Lost/Damaged in Transit"
        varianceContext = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "availableToPromiseVar", new BigDecimal("-2.0"), "quantityOnHandVar", new BigDecimal("-2.0"), "varianceReasonId", "VAR_TRANSIT");
        runAndAssertServiceSuccess("createPhysicalInventoryAndVariance", varianceContext);

        // check the accounting transactions from the variance are tagged
        entries1 = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("productId", productId1));
        assertNotEmpty("Accounting transaction entries not found for product " + product1, entries1);
        for (GenericValue entry : entries1) {
            assertAccountingTagsEqual(entry, tags);
        }

        // Verify that ATP and QOH decreased by 3.0 in WebStoreWarehouse
        inventoryAsserts.assertInventoryChange(productId1, new BigDecimal("-3.0"), originalInventory);

        // make sure accounting and inventory items values equals
        inventoryAsserts.assertInventoryValuesEqual(productId1);

        // Verify that the INVENTORY_ACCOUNT value of balance sheet for Company decreased by $30.00
        Number expectedBalance = (new BigDecimal(originalInventoryAccountBalance.toString())).subtract(new BigDecimal("30.00")).setScale(DECIMALS, ROUNDING);
        Map<String, Number> newBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number newInventoryAccountBalance = newBalances.get(inventoryGlAccountId) != null ? newBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        Number newBalance = (new BigDecimal(newInventoryAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        inventoryAsserts.assertEquals("Unexpected balance for inventory GlAccount: ", expectedBalance, newBalance);
    }

    /**
     * Tests the consequence of serialized inventory receipt and pseudo-variance (by reporting the InventoryItem
     *  as damaged).
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testSerializedInventoryVariance() throws GeneralException {

        // create test product
        GenericValue product = createTestProduct("testSerializedInventoryVariance Product", demowarehouse1);
        String productId = product.getString("productId");

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demowarehouse1);

        String inventoryGlAccountId = financialAsserts.getInventoryGlAccountId();

        // Receive 1.0 serialized units at $123.45 into WebStoreWarehouse
        Map receiveContext = UtilMisc.toMap("userLogin", demowarehouse1, "facilityId", FROM_FACILITY, "productId", productId, "inventoryItemTypeId", "SERIALIZED_INV_ITEM");
        receiveContext.put("quantityAccepted", new BigDecimal("1.0"));
        receiveContext.put("quantityRejected", new BigDecimal("0.0"));
        receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
        receiveContext.put("unitCost", new BigDecimal("123.45"));
        Map receiveResult = runAndAssertServiceSuccess("receiveInventoryProduct", receiveContext);
        String receiveInventoryItemId = (String) receiveResult.get("inventoryItemId");

        // Determine inventory level and account balance before inventory variance
        Map originalInventory = inventoryAsserts.getInventory(productId);
        Map<String, Number> originalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number originalInventoryAccountBalance = originalBalances.get(inventoryGlAccountId) != null ? originalBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        Debug.logInfo("inventoryItemId : " + receiveInventoryItemId, MODULE);
        // Use updateInventoryItem to set the inventory item's status to "Defective"
        Map updateInvItemContext = UtilMisc.toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "statusId", "INV_DEFECTIVE");
        runAndAssertServiceSuccess("updateInventoryItem", updateInvItemContext);

        // Verify that ATP and QOH decreased by 1.0 in WebStoreWarehouse
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("-1.0"), originalInventory);

        // make sure accounting and inventory items values equals
        inventoryAsserts.assertInventoryValuesEqual(productId);

        // Verify that the INVENTORY_ACCOUNT value of balance sheet for Company decreased by $29.61
        Number expectedBalance = (new BigDecimal(originalInventoryAccountBalance.toString())).subtract(new BigDecimal("123.45")).setScale(DECIMALS, ROUNDING);
        Map<String, Number> newBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number newInventoryAccountBalance = newBalances.get(inventoryGlAccountId) != null ? newBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        Number newBalance = (new BigDecimal(newInventoryAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        inventoryAsserts.assertEquals("Unexpected balance for inventory GlAccount: ", expectedBalance, newBalance);

        // Verify that accounting transactions are equivalent to those of INV_RCV_SER_TEST-4, INV_VAR_TEST-1
        List<GenericValue> newAcctgTrans = delegator.findByCondition("AcctgTrans", EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, receiveInventoryItemId), null, null);
        List<String> referenceAcctgTrans = UtilMisc.toList("INV_RCV_SER_TEST-4", "INV_SER_VAR_TEST-1");
        assertTransactionEquivalence(newAcctgTrans, referenceAcctgTrans);
    }

    /**
     * Tests the recording of <code>InventoryItemValueHistory</code> after an inventory item is received without giving an unit cost.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInventoryItemValueHistoryCreateWithoutUnitCost() throws GeneralException {

        // create test product
        GenericValue product = createTestProduct("testInventoryItemValueHistoryCreate WithoutUnitCost Product", demowarehouse1);
        String productId = product.getString("productId");

        // Receive inventory of a product into WebStoreWarehouse, with no unit cost specified
        Map receiveContext = UtilMisc.toMap("userLogin", demowarehouse1, "facilityId", FROM_FACILITY, "productId", productId, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        receiveContext.put("quantityAccepted", new BigDecimal("1.0"));
        receiveContext.put("quantityRejected", new BigDecimal("0.0"));
        receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
        Map receiveResult = runAndAssertServiceSuccess("receiveInventoryProduct", receiveContext);
        String receiveInventoryItemId = (String) receiveResult.get("inventoryItemId");

        // Should result in an InventoryItemValueHistory record being created with unitCost of zero
        List<GenericValue> invItemValHist = delegator.findByAnd("InventoryItemValueHistory", UtilMisc.toMap("inventoryItemId", receiveInventoryItemId));
        assertNotEmpty("InventoryItemValueHistory record was not created after inventory was received", UtilMisc.toList(invItemValHist));
        assertEquals("Too many InventoryItemValueHistory records were created after inventory was received", invItemValHist.size(), 1);
        assertTrue("InventoryItemValueHistory record was created with an empty unitCost", UtilValidate.isNotEmpty(invItemValHist.get(0).get("unitCost")));
        assertEquals("InventoryItemValueHistory record was created with an unexpected unitCost", BigDecimal.ZERO, invItemValHist.get(0).getBigDecimal("unitCost"));
    }

    /**
     * Tests the recording of <code>InventoryItemValueHistory</code> after an inventory item is received with a given unit cost.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInventoryItemValueHistoryCreateWithUnitCost() throws GeneralException {

        // create test product
        GenericValue product = createTestProduct("testInventoryItemValueHistoryCreate WithUnitCost Product", demowarehouse1);
        String productId = product.getString("productId");

        // Receive inventory of a product into WebStoreWarehouse, with a unit cost of $1.00 specified
        Map receiveContext = UtilMisc.toMap("userLogin", demowarehouse1, "facilityId", FROM_FACILITY, "productId", productId, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        receiveContext.put("quantityAccepted", new BigDecimal("1.0"));
        receiveContext.put("quantityRejected", new BigDecimal("0.0"));
        receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
        receiveContext.put("unitCost", new BigDecimal("1.00"));
        Map receiveResult = runAndAssertServiceSuccess("receiveInventoryProduct", receiveContext);
        String receiveInventoryItemId = (String) receiveResult.get("inventoryItemId");

        // Should result in an InventoryItemValueHistory record being created with unitCost of $1.00
        List<GenericValue> invItemValHist = delegator.findByAnd("InventoryItemValueHistory", UtilMisc.toMap("inventoryItemId", receiveInventoryItemId));
        assertNotEmpty("InventoryItemValueHistory record was not created after inventory was received", UtilMisc.toList(invItemValHist));
        assertEquals("Too many InventoryItemValueHistory records were created after inventory was received", invItemValHist.size(), 1);
        assertTrue("InventoryItemValueHistory record was created with an empty unitCost", UtilValidate.isNotEmpty(invItemValHist.get(0).get("unitCost")));
        assertEquals("InventoryItemValueHistory record was created with an unexpected unitCost", new BigDecimal("1.00"), (invItemValHist.get(0).getBigDecimal("unitCost")).setScale(DECIMALS, ROUNDING));
    }

    /**
     * Tests the updating of <code>InventoryItemValueHistory</code> after an inventory item is updated.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInventoryItemValueHistoryUpdate() throws GeneralException {

        // create test product
        GenericValue product = createTestProduct("testInventoryItemValueHistoryUpdate Product", demowarehouse1);
        String productId = product.getString("productId");

        // Receive inventory of a product into WebStoreWarehouse, with a unit cost of $1.00 specified
        Map receiveContext = UtilMisc.toMap("userLogin", demowarehouse1, "facilityId", FROM_FACILITY, "productId", productId, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        receiveContext.put("quantityAccepted", new BigDecimal("1.0"));
        receiveContext.put("quantityRejected", new BigDecimal("0.0"));
        receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
        receiveContext.put("unitCost", new BigDecimal("1.00"));
        Map receiveResult = runAndAssertServiceSuccess("receiveInventoryProduct", receiveContext);
        String receiveInventoryItemId = (String) receiveResult.get("inventoryItemId");

        // Update the InventoryItem with a null unitCost
        pause("Workaround pause for MySQL");
        runAndAssertServiceSuccess("updateInventoryItem", UtilMisc.toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "unitCost", null));

        // Should result in a two InventoryItemValueHistory records - the second with a unitCost of zero
        List<GenericValue> invItemValHist = delegator.findByAnd("InventoryItemValueHistory", UtilMisc.toMap("inventoryItemId", receiveInventoryItemId, "unitCost", BigDecimal.ZERO), UtilMisc.toList("dateTime DESC"));
        assertEquals("InventoryItemValueHistory record was not created or was created with an unexpected unitCost after updating InventoryItem with null unitCost", 1, invItemValHist.size());

        // Update the InventoryItem with a real unitCost
        pause("Workaround pause for MySQL");
        runAndAssertServiceSuccess("updateInventoryItem", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "unitCost", new BigDecimal("5.00")));

        // Should now be three InventoryItemValueHistory records - the third with a unitCost of $5.00
        invItemValHist = delegator.findByAnd("InventoryItemValueHistory", UtilMisc.toMap("inventoryItemId", receiveInventoryItemId, "unitCost", new BigDecimal("5.00")), UtilMisc.toList("dateTime DESC"));
        assertEquals("InventoryItemValueHistory was not created or was created with an unexpected unitCost after updating InventoryItem.unitCost to $5.00", 1, invItemValHist.size());

        // Update the InventoryItem with the same unitCost
        pause("Workaround pause for MySQL");
        runAndAssertServiceSuccess("updateInventoryItem", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "unitCost", new BigDecimal("5.00")));

        // Should still be three InventoryItemValueHistory records
        invItemValHist = delegator.findByAnd("InventoryItemValueHistory", UtilMisc.toMap("inventoryItemId", receiveInventoryItemId), UtilMisc.toList("dateTime DESC"));
        assertEquals("Incorrect number of InventoryItemValueHistory records were created after inventory was received and updated", invItemValHist.size(), 3);
    }

    /**
     * Test updating an inventory item accounting tags generates the correct accounting variance.
     * NOTE: this test is currently disabled because this feature is not supported.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void _testInventoryItemAccountingTagChanges() throws GeneralException {

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demowarehouse1);

        String inventoryGlAccountId = financialAsserts.getInventoryGlAccountId();

        // Determine inventory account balances before inventory changes
        Map<String, Number> originalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number originalInventoryAccountBalance = originalBalances.get(inventoryGlAccountId) != null ? originalBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;

        // create test product
        GenericValue product = createTestProduct("testInventoryItemAccountingTagChanges Product", demowarehouse1);
        String productId = product.getString("productId");

        // Receive 5 units of a product into WebStoreWarehouse, with a unit cost of $10 and initial tags
        Map receiveContext = UtilMisc.toMap("userLogin", demowarehouse1, "facilityId", FROM_FACILITY, "productId", productId, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        receiveContext.put("quantityAccepted", new BigDecimal("5.0"));
        receiveContext.put("quantityRejected", new BigDecimal("0.0"));
        receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
        receiveContext.put("unitCost", new BigDecimal("10.0"));
        // tags
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("acctgTagEnumId1", "DIV_GOV");
        tags.put("acctgTagEnumId2", "DPT_CORPORATE");
        receiveContext.putAll(tags);
        Map receiveResult = runAndAssertServiceSuccess("receiveInventoryProduct", receiveContext);
        String receiveInventoryItemId = (String) receiveResult.get("inventoryItemId");

        // Verify that the INVENTORY_ACCOUNT value of balance sheet for Company increased by $50.00
        Map<String, Number> newBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number expectedBalanceInv = (new BigDecimal(originalInventoryAccountBalance.toString())).add(new BigDecimal("50.00")).setScale(DECIMALS, ROUNDING);
        Number newInventoryAccountBalance = newBalances.get(inventoryGlAccountId) != null ? newBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        Number newBalanceInv = (new BigDecimal(newInventoryAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        inventoryAsserts.assertEquals("Unexpected balance for inventory GlAccount: ", expectedBalanceInv, newBalanceInv);

        originalInventoryAccountBalance = newInventoryAccountBalance;

        // note the time to find all generated transactions
        pause("Workaround pause for MySQL");
        Timestamp start = UtilDateTime.nowTimestamp();

        // Update the InventoryItem tag to DIV_CONSUMER / DPT_MANUFACTURING / ACTI_RESEARCH
        pause("Workaround pause for MySQL");
        Map<String, String> newTags = new HashMap<String, String>();
        newTags.put("acctgTagEnumId1", "DIV_CONSUMER");
        newTags.put("acctgTagEnumId2", "DPT_MANUFACTURING");
        newTags.put("acctgTagEnumId3", "ACTI_RESEARCH");
        Map updateContext = UtilMisc.toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId);
        updateContext.putAll(newTags);
        runAndAssertServiceSuccess("updateInventoryItem", updateContext);

        // check the new inventory item tags
        GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", receiveInventoryItemId));
        assertAccountingTagsEqual(inventoryItem, newTags);

        // Verify that the INVENTORY_ACCOUNT value of balance sheet for Company did not change since the inventory was received
        newBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        expectedBalanceInv = (new BigDecimal(originalInventoryAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        newInventoryAccountBalance = newBalances.get(inventoryGlAccountId) != null ? newBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        newBalanceInv = (new BigDecimal(newInventoryAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        inventoryAsserts.assertEquals("Unexpected balance for inventory GlAccount: ", expectedBalanceInv, newBalanceInv);

        // check the accounting transaction generated
        List<GenericValue> transactions = delegator.findByAnd("AcctgTrans", UtilMisc.toList(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN, start)));
        assertEquals("There should be one adjustment transaction after accounting tag update", 1, transactions.size());
        List<GenericValue> entries = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", transactions.get(0).get("acctgTransId")));
        assertEquals("There should be two adjustment transaction entries after accounting tag update", 2, entries.size());
        for (GenericValue entry : entries) {
            assertEquals("Transaction entry product ID should be the test product", productId, entry.get("productId"));
            assertEquals("Transaction entry GL account should be the INVENTORY_ACCOUNT", inventoryGlAccountId, entry.get("glAccountId"));
            assertEquals("Transaction entry amount should be the same as the inventory amount", new BigDecimal("50.0"), entry.getBigDecimal("amount"));
            if ("D".equals(entry.getString("debitCreditFlag"))) {
                // debit entry should be tagged with the original tags
                assertAccountingTagsEqual(entry, tags);
            } else {
                // credit entry should be tagged with the new tags
                assertAccountingTagsEqual(entry, newTags);
            }
        }

        // note the time to find all generated transactions
        pause("Workaround pause for MySQL");
        start = UtilDateTime.nowTimestamp();

        // Update the InventoryItem, with the same tags (DIV_CONSUMER / DPT_MANUFACTURING / ACTI_RESEARCH)
        pause("Workaround pause for MySQL");
        updateContext = UtilMisc.toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId);
        updateContext.putAll(newTags);
        runAndAssertServiceSuccess("updateInventoryItem", updateContext);

        // Verify that the INVENTORY_ACCOUNT value of balance sheet for Company did not change since the inventory was received
        newBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        expectedBalanceInv = (new BigDecimal(originalInventoryAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        newInventoryAccountBalance = newBalances.get(inventoryGlAccountId) != null ? newBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        newBalanceInv = (new BigDecimal(newInventoryAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        inventoryAsserts.assertEquals("Unexpected balance for inventory GlAccount: ", expectedBalanceInv, newBalanceInv);

        // check the accounting transaction generated, should be none
        transactions = delegator.findByAnd("AcctgTrans", UtilMisc.toList(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN, start)));
        assertEquals("There should be NO adjustment transaction after accounting tag update with the same tags", 0, transactions.size());

    }

    /**
     * Test updating an inventory item unit cost generated the corresponding accounting variance.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInventoryItemRevaluation() throws GeneralException {

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demowarehouse1);

        String inventoryGlAccountId = financialAsserts.getInventoryGlAccountId();
        String inventoryValAdjGlAccountId = financialAsserts.getInventoryValAdjGlAccountId();

        // create test product
        GenericValue product = createTestProduct("testInventoryItemRevaluation Product", demowarehouse1);
        String productId = product.getString("productId");

        // Determine inventory account balances before inventory changes
        Map<String, Number> originalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number originalInventoryAccountBalance = originalBalances.get(inventoryGlAccountId) != null ? originalBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        Number originalInventoryValAdjAccountBalance = originalBalances.get(inventoryValAdjGlAccountId) != null ? originalBalances.get(inventoryValAdjGlAccountId) : BigDecimal.ZERO;

        // Receive 5 units of a product into WebStoreWarehouse, with a unit cost of $4.56 specified
        Map receiveContext = UtilMisc.toMap("userLogin", demowarehouse1, "facilityId", FROM_FACILITY, "productId", productId, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        receiveContext.put("quantityAccepted", new BigDecimal("5.0"));
        receiveContext.put("quantityRejected", new BigDecimal("0.0"));
        receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
        receiveContext.put("unitCost", new BigDecimal("4.56"));
        Map receiveResult = runAndAssertServiceSuccess("receiveInventoryProduct", receiveContext);
        String receiveInventoryItemId = (String) receiveResult.get("inventoryItemId");

        // Update the InventoryItem to unitCost $6.00 and then to $3.00
        pause("Workaround pause for MySQL");
        runAndAssertServiceSuccess("updateInventoryItem", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "unitCost", new BigDecimal("6.00")));

        // make sure accounting and inventory items values equals
        inventoryAsserts.assertInventoryValuesEqual(productId);

        pause("Workaround pause for MySQL");
        runAndAssertServiceSuccess("updateInventoryItem", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "unitCost", new BigDecimal("3.00")));

        // make sure accounting and inventory items values equals
        inventoryAsserts.assertInventoryValuesEqual(productId);

        // Verify that accounting transactions are equivalent to those of INV_NS_REVAL_TEST-1 and INV_NS_REVAL_TEST-2
        List<GenericValue> newAcctgTrans = delegator.findByCondition("AcctgTrans", EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, receiveInventoryItemId), null, UtilMisc.toList("acctgTransId"));
        List<String> referenceAcctgTrans = UtilMisc.toList("INV_NS_REVAL_TEST-1", "INV_NS_REVAL_TEST-2");
        assertTransactionEquivalence(newAcctgTrans, referenceAcctgTrans);

        // Verify that the INVENTORY_ACCOUNT value of balance sheet for Company increased by $15.00
        Map<String, Number> newBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number expectedBalanceInv = (new BigDecimal(originalInventoryAccountBalance.toString())).add(new BigDecimal("15.00")).setScale(DECIMALS, ROUNDING);
        Number newInventoryAccountBalance = newBalances.get(inventoryGlAccountId) != null ? newBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        Number newBalanceInv = (new BigDecimal(newInventoryAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        inventoryAsserts.assertEquals("Unexpected balance for inventory GlAccount: ", expectedBalanceInv, newBalanceInv);

        // Verify that the INVENTORY_VAL_ADJ value of balance sheet for Company increased by $7.80 (credit of $7.20, debit of $15.00 is a net debit of $7.80, but
        //  INVENTORY_VALUE_ADJ is a liability account, so the balance goes UP with a net debit)
        Number expectedBalanceInvValAdj = (new BigDecimal(originalInventoryValAdjAccountBalance.toString())).add(new BigDecimal("7.80")).setScale(DECIMALS, ROUNDING);
        Number newInventoryValAdjAccountBalance = newBalances.get(inventoryValAdjGlAccountId) != null ? newBalances.get(inventoryValAdjGlAccountId) : BigDecimal.ZERO;
        Number newBalanceInvValAdj = (new BigDecimal(newInventoryValAdjAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        inventoryAsserts.assertEquals("Unexpected balance for inventory value adjustment GlAccount: ", expectedBalanceInvValAdj, newBalanceInvValAdj);
    }

    /**
     * Test updating an inventory item unit cost generated the corresponding accounting variance with tags from the inventory item.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInventoryItemRevaluationWithAccountingTags() throws GeneralException {

        // 1. create test product
        GenericValue product1 = createTestProduct("testInventoryItemRevaluation WithAccountingTags Product", demowarehouse1);
        String productId1 = product1.getString("productId");

        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demowarehouse1);

        String inventoryGlAccountId = financialAsserts.getInventoryGlAccountId();
        String inventoryValAdjGlAccountId = financialAsserts.getInventoryValAdjGlAccountId();

        // Determine inventory account balances before inventory changes
        Map<String, Number> originalBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number originalInventoryAccountBalance = originalBalances.get(inventoryGlAccountId) != null ? originalBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        Number originalInventoryValAdjAccountBalance = originalBalances.get(inventoryValAdjGlAccountId) != null ? originalBalances.get(inventoryValAdjGlAccountId) : BigDecimal.ZERO;

        // Receive 5 units of a product into WebStoreWarehouse, with a unit cost of $4.56 specified
        Map receiveContext = UtilMisc.toMap("userLogin", demowarehouse1, "facilityId", FROM_FACILITY, "productId", productId1, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        receiveContext.put("quantityAccepted", new BigDecimal("5.0"));
        receiveContext.put("quantityRejected", new BigDecimal("0.0"));
        receiveContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
        receiveContext.put("unitCost", new BigDecimal("4.56"));
        // tags
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("acctgTagEnumId1", "DIV_GOV");
        tags.put("acctgTagEnumId2", "DPT_CORPORATE");
        receiveContext.putAll(tags);
        Map receiveResult = runAndAssertServiceSuccess("receiveInventoryProduct", receiveContext);
        String receiveInventoryItemId = (String) receiveResult.get("inventoryItemId");

        // Update the InventoryItem to unitCost $6.00 and then to $3.00
        pause("Workaround pause for MySQL");
        runAndAssertServiceSuccess("updateInventoryItem", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "unitCost", new BigDecimal("6.00")));

        // make sure accounting and inventory items values equals
        inventoryAsserts.assertInventoryValuesEqual(productId1);

        // check the accounting transactions are tagged
        List<GenericValue> entries1 = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("productId", productId1));
        assertNotEmpty("Accounting transaction entries not found for product " + product1, entries1);
        for (GenericValue entry : entries1) {
            assertAccountingTagsEqual(entry, tags);
        }

        pause("Workaround pause for MySQL");
        runAndAssertServiceSuccess("updateInventoryItem", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "inventoryItemId", receiveInventoryItemId, "unitCost", new BigDecimal("3.00")));

        // make sure accounting and inventory items values equals
        inventoryAsserts.assertInventoryValuesEqual(productId1);

        // Verify that the INVENTORY_ACCOUNT value of balance sheet for Company increased by $15.00
        Map<String, Number> newBalances = financialAsserts.getFinancialBalances(UtilDateTime.nowTimestamp());
        Number expectedBalanceInv = (new BigDecimal(originalInventoryAccountBalance.toString())).add(new BigDecimal("15.00")).setScale(DECIMALS, ROUNDING);
        Number newInventoryAccountBalance = newBalances.get(inventoryGlAccountId) != null ? newBalances.get(inventoryGlAccountId) : BigDecimal.ZERO;
        Number newBalanceInv = (new BigDecimal(newInventoryAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        inventoryAsserts.assertEquals("Unexpected balance for inventory GlAccount: ", expectedBalanceInv, newBalanceInv);

        // Verify that the INVENTORY_VAL_ADJ value of balance sheet for Company increased by $7.80 (credit of $7.20, debit of $15.00 is a net debit of $7.80, but
        //  INVENTORY_VALUE_ADJ is a liability account, so the balance goes UP with a net debit)
        Number expectedBalanceInvValAdj = (new BigDecimal(originalInventoryValAdjAccountBalance.toString())).add(new BigDecimal("7.80")).setScale(DECIMALS, ROUNDING);
        Number newInventoryValAdjAccountBalance = newBalances.get(inventoryValAdjGlAccountId) != null ? newBalances.get(inventoryValAdjGlAccountId) : BigDecimal.ZERO;
        Number newBalanceInvValAdj = (new BigDecimal(newInventoryValAdjAccountBalance.toString())).setScale(DECIMALS, ROUNDING);
        inventoryAsserts.assertEquals("Unexpected balance for inventory value adjustment GlAccount: ", expectedBalanceInvValAdj, newBalanceInvValAdj);
    }

    /**
     * This test will verify that order reservations will not be made against pending inventory transfers of non-serialized inventory.
     * 1.  Create a new test product
     * 2.  Receive 5.0 of the test product to WebStoreWarehouse as non-serialized inventory
     * 3.  Transfer 5.0 of the received inventory item from WebStoreWarehouse to MyRetailStore
     * 4.  Create a sales order for 5.0 of the test product
     * 5.  Verify that the inventoryItemId reserved against the order from (4) is not the same as the one from step (2) -- ie, a different item
     *     was reserved against the order
     * 6.  Verify that the ATP of the test product is -5, the QOH is 5.0
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testOrderReservationOnNonSerializedInventoryTransfer() throws GeneralException {

        final BigDecimal inventoryQty = new BigDecimal("5.0");

        // 1. Create test product
        final GenericValue product = createTestProduct("Order Reservation On Non Serialized Inventory Transfer Test Product", demowarehouse1);
        assignDefaultPrice(product, new BigDecimal("20.0"), admin);

        // 2. Receive 5.0 units of the product as non serialized inventory
        Map receivedInventoryMap = receiveInventoryProduct(product, inventoryQty, "NON_SERIAL_INV_ITEM", demowarehouse1);
        String inventoryItemId = (String) receivedInventoryMap.get("inventoryItemId");

        // 3. Transfer 5.0 of the received inventory item from WebStoreWarehouse to MyRetailStore
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", demowarehouse1);
        input.put("facilityId", FROM_FACILITY);
        input.put("facilityIdTo", TO_FACILTY);
        input.put("statusId", "IXF_REQUESTED");
        input.put("xferQty", inventoryQty);
        input.put("inventoryItemId", inventoryItemId);
        runAndAssertServiceSuccess("createInventoryTransfer", input);

        // 4. Create a sales order for 5.0 of the test product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, inventoryQty);
        User = admin;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, PRODUCT_STORE, "EXT_OFFLINE", "DemoAddress2");

        // 5. Verify that the inventoryItemId reserved against the order from (4) is not the same as the one from step (2)
        List<GenericValue> orderReservations = delegator.findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", salesOrder.getOrderId()));
        for (GenericValue res : orderReservations) {
            assertNotEquals("The inventory item in transfer should not be reserved for the order", inventoryItemId, res.getString("inventoryItemId"));
        }

        // 6. Verify that the ATP of the test product is -5, the QOH is 5.0
        assertProductAvailability(product, inventoryQty.negate(), inventoryQty);
    }

    /**
     * This test will verify that order reservations will not be made against pending inventory transfers of serialized inventory.
     * 1.  Create a new test product
     * 2.  Receive 5.0 of the test product to WebStoreWarehouse as serialized inventory
     * 3.  Transfer 3 of the received inventory items from WebStoreWarehouse to MyRetailStore
     * 4.  Create a sales order for 5.0 of the test product
     * 5.  Verify that the 2 of the 5 inventoryItemIds received in (2) are now reserved against the order from (4), and another reservation has been
     *     created against a new inventoryItemId for quantity 3 of the test product
     * 6.  Verify that the ATP of the test product is -3, the QOH is 5.0
     * @throws GeneralException if an error occurs
     */
    public void testOrderReservationOnSerializedInventoryTransfer() throws GeneralException {

        final BigDecimal inventoryQty = new BigDecimal("5.0");
        final BigDecimal transferQty = new BigDecimal("3.0");

        // 1. Create test product
        final GenericValue product = createTestProduct("Order Reservation On Serialized Inventory Transfer Test Product", demowarehouse1);
        final String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("20.0"), admin);

        // 2. Receive 5.0 units of the product as serialized inventory
        receiveInventoryProduct(product, inventoryQty, "SERIALIZED_INV_ITEM", demowarehouse1);
        List<String> receivedInventoryItems = EntityUtil.getFieldListFromEntityList(delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId)), "inventoryItemId", true);
        assertEquals("There should be 5 inventory items received", inventoryQty.intValue(), receivedInventoryItems.size());

        // 3. Transfer 3.0 of the received inventory item from WebStoreWarehouse to MyRetailStore
        for (String inventoryItemId : receivedInventoryItems.subList(0, transferQty.intValue())) {
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("userLogin", demowarehouse1);
            input.put("facilityId", FROM_FACILITY);
            input.put("facilityIdTo", TO_FACILTY);
            input.put("statusId", "IXF_REQUESTED");
            input.put("xferQty", transferQty);
            input.put("inventoryItemId", inventoryItemId);
            runAndAssertServiceSuccess("createInventoryTransfer", input);
        }

        List<String> transferedInventoryItems = EntityUtil.getFieldListFromEntityList(delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "statusId", "INV_BEING_TRANSFERED")), "inventoryItemId", true);
        assertEquals("There should be 3 inventory items in transfer", 3, transferedInventoryItems.size());

        // 4. Create a sales order for 5.0 of the test product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, inventoryQty);
        User = admin;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, PRODUCT_STORE, "EXT_OFFLINE", "DemoAddress2");

        // 5. Verify that the 2 of the 5 inventoryItemIds received in (2) are now reserved against the order from (4), and
        // another reservation has been created against a new inventoryItemId for quantity 3 of the test product
        List<GenericValue> orderReservations = delegator.findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", salesOrder.getOrderId()));
        int reservationOnReceivedItems = 0;
        for (GenericValue reservation : orderReservations) {
            String reservationInventoryItemId = reservation.getString("inventoryItemId");
            if (transferedInventoryItems.contains(reservationInventoryItemId)) {
                fail("One of the inventory item in transfer was reserved for the order: " + reservationInventoryItemId);
            }
            if (receivedInventoryItems.contains(reservationInventoryItemId)) {
                reservationOnReceivedItems++;
            } else {
                // this should be reserved on a new non serialized inventory item
                assertEquals("The reserved quantity is incorrect", reservation.getBigDecimal("quantity"), transferQty);
                assertEquals("The reserved quantity not available is incorrect", reservation.getBigDecimal("quantityNotAvailable"), transferQty);
                GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", reservation.getString("inventoryItemId")));
                assertNotNull("Could not find the InventoryItem", inventoryItem);
                assertEquals("The the reserved inventoryItem should be non serialized", inventoryItem.get("inventoryItemTypeId"), "NON_SERIAL_INV_ITEM");
                assertEquals("The QOH for the reserved inventoryItem should be 0", inventoryItem.getBigDecimal("quantityOnHandTotal"), BigDecimal.ZERO);
                assertEquals("The ATP for the reserved inventoryItem is incorrect", inventoryItem.getBigDecimal("availableToPromiseTotal"), transferQty.negate());
            }
        }
        assertEquals("The number of reservation on the initially received serialized items is incorrect", reservationOnReceivedItems, inventoryQty.subtract(transferQty).intValue());

        // 6. Verify that the ATP of the test product is -3, the QOH is 5.0
        assertProductAvailability(product, transferQty.negate(), inventoryQty);
    }

    /**
     * This test will verify that order reservations will be made against  non-serialized inventory if the inventory transfer has been canceled.
     * 1.  Create a new test product
     * 2.  Receive 5.0 of the test product to WebStoreWarehouse as non-serialized inventory
     * 3.  Transfer 5.0 of the received inventory item from WebStoreWarehouse to MyRetailStore
     * 4.  Cancel the inventory transfer from (3)
     * 5.  Create a sales order for 6.0 of the test product
     * 6.  Verify that the inventoryItemId reserved against the order from (5) is the same as the one from step (2) -- ie, once the inventory transfer
     *     was canceled, the item can be reserved against orders again, and that the inventory item as ATP = -1 QOH = 5 (so no extra inventory item is created)
     * 7.  Verify that the ATP of the test product is -1.0, the QOH is 5.0
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testOrderReservationOnCanceledInventoryTransfer() throws GeneralException {

        final BigDecimal inventoryQty = new BigDecimal("5.0");

        // 1. Create test product
        final GenericValue product = createTestProduct("Order Reservation On Canceled Inventory Transfer Test Product", demowarehouse1);
        assignDefaultPrice(product, new BigDecimal("20.0"), admin);

        // 2. Receive 5.0 units of the product as non serialized inventory
        Map receivedInventoryMap = receiveInventoryProduct(product, inventoryQty, "NON_SERIAL_INV_ITEM", demowarehouse1);
        String inventoryItemId = (String) receivedInventoryMap.get("inventoryItemId");

        // 3. Transfer 5.0 of the received inventory item from WebStoreWarehouse to MyRetailStore
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", demowarehouse1);
        input.put("facilityId", FROM_FACILITY);
        input.put("facilityIdTo", TO_FACILTY);
        input.put("statusId", "IXF_REQUESTED");
        input.put("xferQty", inventoryQty);
        input.put("inventoryItemId", inventoryItemId);
        Map result = runAndAssertServiceSuccess("createInventoryTransfer", input);
        String inventoryTransferId = (String) result.get("inventoryTransferId");

        pause("Workarround for MySQL");

        // 4. Cancel the inventory transfer from (3)
        input = new HashMap<String, Object>();
        input.put("userLogin", demowarehouse1);
        input.put("inventoryTransferId", inventoryTransferId);
        runAndAssertServiceSuccess("cancelInventoryTransfer", input);

        // 5. Create a sales order for 6.0 of the test product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, inventoryQty.add(BigDecimal.ONE));
        User = admin;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, PRODUCT_STORE, "EXT_OFFLINE", "DemoAddress2");

        // 6. Verify that the inventoryItemId reserved against the order from (5) is the same as the one from step (2)
        // and that the inventory item as ATP = -1 QOH = 5 (so no extra inventory item is created)
        List<GenericValue> orderReservations = delegator.findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", salesOrder.getOrderId()));
        assertEquals("There should be only one OrderItemShipGrpInvRes", 1, orderReservations.size());
        GenericValue res = EntityUtil.getFirst(orderReservations);
        assertEquals("The inventory item in the canceled transfer should be reserved for the order", inventoryItemId, res.getString("inventoryItemId"));
        GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
        assertEquals("The inventory item ATP is incorrect", new BigDecimal("-1.0"), inventoryItem.getBigDecimal("availableToPromiseTotal"));
        assertEquals("The inventory item QOH is incorrect", inventoryQty, inventoryItem.getBigDecimal("quantityOnHandTotal"));

        // 7. Verify that the ATP of the test product is -1.0, the QOH is 5.0
        assertProductAvailability(product, new BigDecimal("-1.0"), inventoryQty);
    }

    /**
     * Test product and inventory domain method to get the standard cost.
     * 1. Test standard cost for products MAT_A_COST, MAT_B_COST and PROD_COST
     * 2. Receive 3 items of MAT_B_COST with a unit cost of 95$
     * 3. Check the standard cost for the inventory item is the same as the product standard cost
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testStandardCosts() throws Exception {

        final ProductRepositoryInterface productRepository = getProductRepository(admin);
        final InventoryRepositoryInterface inventoryRepository = getInventoryRepository(admin);

        // 1. Test standard cost for products MAT_A_COST, MAT_B_COST and PROD_COST
        final Product matAProduct = productRepository.getProductById("MAT_A_COST");
        final BigDecimal expectedMatAStdCost = new BigDecimal("9.0");
        assertEquals("Unexpected standardCost for product MAT_A_COST", matAProduct.getStandardCost("USD"), expectedMatAStdCost);

        final BigDecimal expectedMatBStdCost = new BigDecimal("7.0");
        final Product matBProduct = productRepository.getProductById("MAT_B_COST");
        assertEquals("Unexpected standardCost for product MAT_B_COST", matBProduct.getStandardCost("USD"), expectedMatBStdCost);

        final BigDecimal expectedProdCostStdCost = new BigDecimal("194.0");
        final Product prodCostProduct = productRepository.getProductById("PROD_MANUF");
        assertEquals("Unexpected standardCost for product PROD_COST", prodCostProduct.getStandardCost("USD"), expectedProdCostStdCost);

        // 2. Receive 3 items of MAT_B_COST with a unit cost of 95$
        final BigDecimal inventoryQty = new BigDecimal("3.0");
        final BigDecimal unitCost = new BigDecimal("95.0");
        Map result = receiveInventoryProduct(Repository.genericValueFromEntity(getDelegator(), matBProduct), inventoryQty, "NON_SERIAL_INV_ITEM", unitCost, demowarehouse1);

        // 3. Check the standard cost for the inventory item is the same as the product standard cost
        assertEquals("Unexpected standardCost for product MAT_B_COST", matBProduct.getStandardCost("USD"), expectedMatBStdCost);
        final InventoryItem inventoryItem = inventoryRepository.getInventoryItemById((String) result.get("inventoryItemId"));
        assertEquals("Unexpected standardCost for the received inventory item for MAT_B_COST", inventoryItem.getStandardCost(), expectedMatBStdCost);
    }

    /**
     * Receive inventory item and issue to sales order. Test forward and backward tracing.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInventoryItemIssuanceTracing() throws Exception {

        // creates test product
        GenericValue testProduct = createTestProduct("Product for testInventoryItemIssuanceTracing", admin);

        // receives 1 test product
        Map<String, Object> result = receiveInventoryProduct(testProduct, new BigDecimal("1.0"), "NON_SERIAL_INV_ITEM", demowarehouse1);
        String inventoryItemId = (String) result.get("inventoryItemId");

        // creates and approves sales order
        Map<GenericValue, BigDecimal> order = FastMap.newInstance();
        order.put(testProduct, new BigDecimal("1.0"));
        User = admin;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, PRODUCT_STORE, "EXT_OFFLINE", "DemoAddress2");
        salesOrder.approveOrder();

        // ships orders
        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", salesOrder.getOrderId(), "facilityId", facilityId, "userLogin", demowarehouse1));

        // builds inventory trace log for inventory item that was issued to order
        result = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("inventoryItemId", inventoryItemId, "traceDirection", "FORWARD", "userLogin", admin));

        // gets trace log in forward direction for original inventory item
        List<InventoryItemTraceDetail> usageLog = ((List<List<InventoryItemTraceDetail>>) result.get("usageLog")).get(0);

        // should be two records there
        assertEquals("Unexpected count of trace events", 2, usageLog.size());

        // ensures first record is RECEIPT and second one is ORDER_ISSUED
        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        usageLog, null, inventoryItemId, "RECEIPT", 1L
                ), inventoryItemId, null, "ORDER_ISSUED", 2L
        );

        // similar check for trace log created for final inventory item in backward direction
        result = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("inventoryItemId", inventoryItemId, "traceDirection", "BACKWARD", "userLogin", admin));
        usageLog = ((List<List<InventoryItemTraceDetail>>) result.get("usageLog")).get(0);

        //should be 2 records as well
        assertEquals("Unexpected count of trace events", 2, usageLog.size());

        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        usageLog, inventoryItemId, null, "ORDER_ISSUED", 2L
                ), null, inventoryItemId, "RECEIPT", 1L
        );

    }

    /**
     * Receive and transfer an inventory item. Test for them backward tracing.
     * Note: same as testInventoryTransferTracing below except here we transfer the whole 10 received items.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInventoryFullTransferTracing() throws Exception {

        // creates test product
        GenericValue testProduct = createTestProduct("Product for testInventoryFullTransferTracing", admin);

        // receives 10 test products
        Map<String, Object> result = receiveInventoryProduct(testProduct, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), demowarehouse1);
        String inventoryItemId = (String) result.get("inventoryItemId");

        // transfers 10 of them
        Map<String, Object> callCtxt = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1);
        callCtxt.put("facilityId", FROM_FACILITY);
        callCtxt.put("facilityIdTo", TO_FACILTY);
        callCtxt.put("inventoryItemId", inventoryItemId);
        callCtxt.put("statusId", "IXF_SCHEDULED");
        callCtxt.put("xferQty", 10.0);
        result = runAndAssertServiceSuccess("createInventoryTransfer", callCtxt);
        String inventoryTransferId  = (String) result.get("inventoryTransferId");

        callCtxt = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1);
        callCtxt.put("inventoryTransferId", inventoryTransferId);
        pause("Workaround pause for MySQL");
        runAndAssertServiceSuccess("completeInventoryTransfer", callCtxt);

        List<GenericValue> ii = delegator.findByAnd("InventoryItem", UtilMisc.toMap("parentInventoryItemId", inventoryItemId));
        String inventoryItemId1 = EntityUtil.getFirst(ii).getString("inventoryItemId");

        // builds inventory trace log for transfered inventory item
        pause("Workaround pause for MySQL");
        result = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("inventoryItemId", inventoryItemId, "traceDirection", "FORWARD", "userLogin", admin));

        // gets trace log in forward direction for original inventory item
        pause("Workaround pause for MySQL");
        List<InventoryItemTraceDetail> usageLog = ((List<List<InventoryItemTraceDetail>>) result.get("usageLog")).get(0);

        // should be two records there
        assertEquals("Unexpected count of trace events", 2, usageLog.size());

        // ensures first record is RECEIPT and second one is TRANSFER
        List usageLogClone = FastList.newInstance();
        usageLogClone.addAll(usageLog);
        // because assertInventoryTraceEvents will remove match entry, so use clone collection to assert.
        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        usageLogClone, null, inventoryItemId, "RECEIPT", 1L
                ), inventoryItemId, inventoryItemId1, "TRANSFER", 2L
        );

        // similar check for trace log created for final inventory item in backward direction
        pause("Workaround pause for MySQL");
        result = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("inventoryItemId", inventoryItemId1, "traceDirection", "BACKWARD", "userLogin", admin));
        usageLog = ((List<List<InventoryItemTraceDetail>>) result.get("usageLog")).get(0);

        // should be 2 records as well
        assertEquals("Unexpected count of trace events", 2, usageLog.size());

        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        usageLog, inventoryItemId, inventoryItemId1, "TRANSFER", 2L
                ), null, inventoryItemId, "RECEIPT", 1L
        );
    }

    /**
     * Receive and transfer an inventory item. Test for them backward tracing.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInventoryTransferTracing() throws Exception {

        // creates test product
        GenericValue testProduct = createTestProduct("Product for testInventoryTransferTracing", admin);

        // receives 10 test products
        Map<String, Object> result = receiveInventoryProduct(testProduct, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), demowarehouse1);
        String inventoryItemId = (String) result.get("inventoryItemId");

        // transfers 5 of them
        Map<String, Object> callCtxt = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1);
        callCtxt.put("facilityId", FROM_FACILITY);
        callCtxt.put("facilityIdTo", TO_FACILTY);
        callCtxt.put("inventoryItemId", inventoryItemId);
        callCtxt.put("statusId", "IXF_SCHEDULED");
        callCtxt.put("xferQty", new BigDecimal("5.0"));
        result = runAndAssertServiceSuccess("createInventoryTransfer", callCtxt);
        String inventoryTransferId  = (String) result.get("inventoryTransferId");

        callCtxt = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1);
        callCtxt.put("inventoryTransferId", inventoryTransferId);
        pause("Workaround pause for MySQL");
        runAndAssertServiceSuccess("completeInventoryTransfer", callCtxt);

        List<GenericValue> ii = delegator.findByAnd("InventoryItem", UtilMisc.toMap("parentInventoryItemId", inventoryItemId));
        String inventoryItemId1 = EntityUtil.getFirst(ii).getString("inventoryItemId");

        // gets trace log in forward direction for original inventory item
        pause("Workaround pause for MySQL");
        result = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("inventoryItemId", inventoryItemId, "traceDirection", "FORWARD", "userLogin", admin));
        List<InventoryItemTraceDetail> usageLog = ((List<List<InventoryItemTraceDetail>>) result.get("usageLog")).get(0);

        // should be two records there
        assertEquals("Unexpected count of trace events", 2, usageLog.size());

        // ensures first record is RECEIPT and second one is TRANSFER
        List usageLogClone = FastList.newInstance();
        usageLogClone.addAll(usageLog);
        // because assertInventoryTraceEvents will remove match entry, so use clone collection to assert.
        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        usageLogClone, null, inventoryItemId, "RECEIPT", 1L
                ), inventoryItemId, inventoryItemId1, "TRANSFER", 2L
        );

        // get transfered inventory item id
        InventoryItemTraceDetail transfer = usageLog.get(1);
        String finalInventoryItemId = transfer.getToInventoryItemId();

        // similar check for trace log created for final inventory item in backward direction
        pause("Workaround pause for MySQL");
        result = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("inventoryItemId", finalInventoryItemId, "traceDirection", "BACKWARD", "userLogin", admin));
        usageLog = ((List<List<InventoryItemTraceDetail>>) result.get("usageLog")).get(0);

        // should be 2 records as well
        assertEquals("Unexpected count of trace events", 2, usageLog.size());

        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        usageLog, inventoryItemId, inventoryItemId1, "TRANSFER", 2L
                ), null, inventoryItemId, "RECEIPT", 1L
        );
    }

    /**
     * Trace a lot.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testLotTracing() throws Exception {
        // create test products
        GenericValue testProduct1 = createTestProduct("Product 1 for testLotTracing", admin);
        GenericValue testProduct2 = createTestProduct("Product 2 for testLotTracing", admin);
        GenericValue testProduct3 = createTestProduct("Product 3 for testLotTracing", admin);
        GenericValue testProduct4 = createTestProduct("Product 4 for testLotTracing", admin);

        // 1. Receive test product to inventory items 1, 2, 3 and 4
        Map<String, Object> results = receiveInventoryProduct(testProduct1, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("15.0"), demowarehouse1);
        String inventoryItemId1 = (String) results.get("inventoryItemId");

        results = receiveInventoryProduct(testProduct2, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("25.0"), demowarehouse1);
        String inventoryItemId2 = (String) results.get("inventoryItemId");

        receiveInventoryProduct(testProduct3, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("35.0"), demowarehouse1);
        pause("Workaround pause for MySQL");

        receiveInventoryProduct(testProduct4, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("45.0"), demowarehouse1);
        pause("Workaround pause for MySQL");

        // 2. Create a lot, assign the lot id to inventory items 1 and 2
        Map<String, Object> callCtxt = UtilMisc.<String, Object>toMap("userLogin", admin);
        results = runAndAssertServiceSuccess("warehouse.createLot", callCtxt);
        String lotId = (String) results.get("lotId");

        delegator.storeByCondition("InventoryItem", UtilMisc.toMap("lotId", lotId), EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItemId1));
        delegator.storeByCondition("InventoryItem", UtilMisc.toMap("lotId", lotId), EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItemId2));

        // 2. Transfer inventory item 1 to item 5
        pause("Workaround pause for MySQL");
        callCtxt = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1);
        callCtxt.put("facilityId", FROM_FACILITY);
        callCtxt.put("facilityIdTo", TO_FACILTY);
        callCtxt.put("inventoryItemId", inventoryItemId1);
        callCtxt.put("statusId", "IXF_SCHEDULED");
        callCtxt.put("xferQty", 5.0);
        results = runAndAssertServiceSuccess("createInventoryTransfer", callCtxt);
        String inventoryTransferId  = (String) results.get("inventoryTransferId");
        pause("Workaround pause for MySQL");
        callCtxt = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1);
        callCtxt.put("inventoryTransferId", inventoryTransferId);
        runAndAssertServiceSuccess("completeInventoryTransfer", callCtxt);

        List<GenericValue> ii = delegator.findByAnd("InventoryItem", UtilMisc.toMap("parentInventoryItemId", inventoryItemId1));
        String inventoryItemId4 = EntityUtil.getFirst(ii).getString("inventoryItemId");

        // 2. Issue inventory items 2, 3 and 4 to an order
        pause("Workaround pause for MySQL");
        Map<GenericValue, BigDecimal> order = FastMap.newInstance();
        order.put(testProduct2, new BigDecimal("10.0"));
        order.put(testProduct3, new BigDecimal("5.0"));
        order.put(testProduct4, new BigDecimal("5.0"));
        User = admin;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, PRODUCT_STORE, "EXT_OFFLINE", "DemoAddress2");
        salesOrder.approveOrder();

        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", salesOrder.getOrderId(), "facilityId", facilityId, "userLogin", demowarehouse1));

        // 3. Check tracing for lot id.
        pause("Workaround pause for MySQL");
        results = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("lotId", lotId, "traceDirection", "FORWARD", "userLogin", admin));

        // there are three inventory item which have lotId assigned.
        // These are inventoryItemId1, inventoryItemId2 and inventoryItemId4, last one was generated after transfer
        // So, call to warehouse.traceInventoryUsage returns usageLog, actually List of List<InventoryItemTraceDetail>,
        // containing two traces. One for inventoryItemId2 and second for inventoryItem1 & inventoryItemId4 as they
        // are under common root inventory item.

        // NOTE: Although there are two lists in udageLog theirs order isn't guaranteed.
        // This is why selecting them we are based on item count.

        List<List<InventoryItemTraceDetail>> usageLog = (List<List<InventoryItemTraceDetail>>) results.get("usageLog");
        assertEquals("Unexpected count of trace logs", 2, usageLog.size());

        List<InventoryItemTraceDetail> log2Item = null;
        List<InventoryItemTraceDetail> log3Item = null;

        for (int i = 0; i < usageLog.size(); i++) {
            List<InventoryItemTraceDetail> log = usageLog.get(i);
            switch (log.size()) {
            case 2:
                log2Item = log;
                break;
            case 3:
                log3Item = log;
                break;
            default:
                assertFalse("Inventory item usage log has wrong number of items", true);
            }
        }

        // check usage of inventoryItemId2
        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        log2Item, null, inventoryItemId2, "RECEIPT", 1L),
                        inventoryItemId2, null, "ORDER_ISSUED", 2L
        );

        // check usage of inventoryItemId1 and inventoryItemId4
        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        assertInventoryTraceEvents(
                                log3Item, null, inventoryItemId1, "RECEIPT", 1L),
                                inventoryItemId1, inventoryItemId4, "TRANSFER", 2L),
                                inventoryItemId1, inventoryItemId4, "TRANSFER", 2L
        );

    }

    /**
     * Test inventory tracing feature based on more complex scenario.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testTraceComplexInventoryUsage() throws Exception {
        ManufacturingRepositoryInterface manufacturingRepsitory = getManufacturingRepository(admin);

        // create test products
        GenericValue testProduct1 = createTestProduct("Product 1 for testTraceComplexInventoryUsage", admin);
        GenericValue testProduct2 = createTestProduct("Product 2 for testTraceComplexInventoryUsage", "RAW_MATERIAL", admin);
        GenericValue testProduct3 = createTestProduct("Product 3 for testTraceComplexInventoryUsage", admin);

        // 1. Receive 20 of product 1. This should be inventoryItemId1.
        Map<String, Object> results = receiveInventoryProduct(testProduct1, new BigDecimal("20.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);
        String inventoryItemId1 = (String) results.get("inventoryItemId");
        pause("Workaround pause for MySQL");

        // 2. Receive 20 of product 2. This should be inventoryItemId2.
        results = receiveInventoryProduct(testProduct2, new BigDecimal("20.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("15.0"), demowarehouse1);
        String inventoryItemId2 = (String) results.get("inventoryItemId");
        pause("Workaround pause for MySQL");

        // 3. Receive 10 of product 3. This should be inventoryItemId3.
        results = receiveInventoryProduct(testProduct3, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("19.0"), demowarehouse1);
        String inventoryItemId3 = (String) results.get("inventoryItemId");
        pause("Workaround pause for MySQL");

        // 4. Transfer 5 of inventoryItemId3 to inventoryItemId4
        Map<String, Object> callCtxt = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1);
        callCtxt.put("facilityId", FROM_FACILITY);
        callCtxt.put("facilityIdTo", TO_FACILTY);
        callCtxt.put("inventoryItemId", inventoryItemId3);
        callCtxt.put("statusId", "IXF_SCHEDULED");
        callCtxt.put("xferQty", 5.0);
        results = runAndAssertServiceSuccess("createInventoryTransfer", callCtxt);
        String inventoryTransferId  = (String) results.get("inventoryTransferId");

        callCtxt = UtilMisc.<String, Object>toMap("userLogin", demowarehouse1);
        callCtxt.put("inventoryTransferId", inventoryTransferId);
        runAndAssertServiceSuccess("completeInventoryTransfer", callCtxt);

        List<GenericValue> ii = delegator.findByAnd("InventoryItem", UtilMisc.toMap("parentInventoryItemId", inventoryItemId3));
        String inventoryItemId4 = EntityUtil.getFirst(ii).getString("inventoryItemId");

        // 5. Issued 10 testProduct1 & 10 testProduct3 to an order.
        Map<GenericValue, BigDecimal> order = FastMap.newInstance();
        order.put(testProduct1, new BigDecimal("10.0"));
        order.put(testProduct3, new BigDecimal("10.0"));
        User = admin;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, PRODUCT_STORE, "EXT_OFFLINE", "DemoAddress2");
        salesOrder.approveOrder();
        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", salesOrder.getOrderId(), "facilityId", facilityId, "userLogin", demowarehouse1));

        // 6. Issue 10 item 1 and 10 item 2 to manufacturing and produce product 3 and inventoryItemId5.

        //
        // next large chunk of code (up to 7) is responsible for preparing and manufacturing 1 manufacturedProduct
        // and its content doesn't important in context of this unit tests.
        //
        GenericValue manufacturedProduct = createTestProduct("Product for manufacturing within framework of for testTraceComplexInventoryUsage", admin);
        assignDefaultPrice(manufacturedProduct, new BigDecimal("300.0"), admin);
        delegator.create("ProductFacility",
                UtilMisc.toMap(
                        "productId", manufacturedProduct.get("productId"),
                        "facilityId", "WebStoreWarehouse",
                        "minimumStock", Long.valueOf(0),
                        "reorderQuantity", new BigDecimal("1.0"),
                        "daysToShip", Long.valueOf(1)
                )
        );

        GenericValue testProduct5 = createTestProduct("Product 5 for testTraceComplexInventoryUsage", "RAW_MATERIAL", admin);
        results = receiveInventoryProduct(testProduct5, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);
        pause("Workaround pause for MySQL");
        String inventoryItemId5 = (String) results.get("inventoryItemId");
        createMainSupplierForProduct(testProduct5.getString("productId"), "DemoSupplier", new BigDecimal("50.0"), "USD", new BigDecimal("0.0"), admin);
        createMainSupplierForProduct(testProduct2.getString("productId"), "DemoSupplier", new BigDecimal("50.0"), "USD", new BigDecimal("0.0"), admin);
        delegator.create("ProductFacility",
                UtilMisc.toMap(
                        "productId", testProduct2.get("productId"),
                        "facilityId", "WebStoreWarehouse",
                        "minimumStock", Long.valueOf(0),
                        "reorderQuantity", new BigDecimal("1.0"),
                        "daysToShip", Long.valueOf(1)
                )
        );
        delegator.create("ProductFacility",
                UtilMisc.toMap(
                        "productId", testProduct5.get("productId"),
                        "facilityId", "WebStoreWarehouse",
                        "minimumStock", Long.valueOf(0),
                        "reorderQuantity", new BigDecimal("1.0"),
                        "daysToShip", Long.valueOf(1)
                )
        );

        delegator.create("ProductAssoc", UtilMisc.toMap(
                "productId", manufacturedProduct.get("productId"),
                "productIdTo", testProduct2.get("productId"),
                "productAssocTypeId", "MANUF_COMPONENT",
                "sequenceNum", Long.valueOf(1),
                "quantity", new BigDecimal("1.0"),
                "fromDate", UtilDateTime.nowTimestamp()
                ));
        delegator.create("ProductAssoc", UtilMisc.toMap(
                "productId", manufacturedProduct.get("productId"),
                "productIdTo", testProduct5.get("productId"),
                "productAssocTypeId", "MANUF_COMPONENT",
                "sequenceNum", Long.valueOf(2),
                "quantity", new BigDecimal("1.0"),
                "fromDate", UtilDateTime.nowTimestamp()
                ));

        createTestAssemblingRouting("testTraceComplexInventoryUsage", manufacturedProduct.getString("productId"));

        Map input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productId", manufacturedProduct.get("productId"));
        input.put("quantity", BigDecimal.ONE);
        input.put("startDate", UtilDateTime.nowTimestamp());
        input.put("facilityId", facilityId);
        input.put("workEffortName", "testTraceComplexInventoryUsage Production Run");
        results = runAndAssertServiceSuccess("opentaps.createProductionRun", input);
        String productionRunId = (String) results.get("productionRunId");

        ProductionRun productionRun = manufacturingRepsitory.getProductionRun(productionRunId);

        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("statusId", "PRUN_DOC_PRINTED");
        runAndAssertServiceSuccess("changeProductionRunStatus", input);

        List tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"));
        assertNotEmpty("Production run [" + productionRunId + "] has no routing tasks.  Cannot finish test.", tasks);
        assertEquals("Template for [" + productionRunId + "] has more than one task.  It should only have one task defined.", tasks.size(), 1);
        GenericValue task = EntityUtil.getFirst(tasks);
        String taskId = task.getString("workEffortId");

        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        input = UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId);
        runAndAssertServiceSuccess("issueProductionRunTask", input);

        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("productionRunTaskId", taskId);
        input.put("partyId", demowarehouse1.get("partyId"));
        input.put("addSetupTime", new BigDecimal("1000000.0"));
        input.put("addTaskTime", new BigDecimal("800000.0"));
        runAndAssertServiceSuccess("updateProductionRunTask", input);

        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        input.put("workEffortId", taskId);
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", input);

        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("workEffortId", productionRunId);
        input.put("productId", manufacturedProduct.getString("productId"));
        results = runAndAssertServiceSuccess("opentaps.productionRunProduce", input);

        input = UtilMisc.toMap("userLogin", demowarehouse1);
        input.put("productionRunId", productionRunId);
        runAndAssertServiceSuccess("changeProductionRunStatus", input);

        List<? extends WorkEffortInventoryProduced> producedInventoryRefs =
            productionRun.getWorkEffortInventoryProduceds();

        assertNotNull(producedInventoryRefs);
        String inventoryItemId6 = producedInventoryRefs.get(0).getInventoryItemId();

        // 7. Issue 1 inventory item 6 & 10 inventory item 1 to an order.
        order = FastMap.newInstance();
        order.put(manufacturedProduct, new BigDecimal("1.0"));
        order.put(testProduct1, new BigDecimal("10.0"));
        User = admin;
        salesOrder = testCreatesSalesOrder(order, DemoCustomer, PRODUCT_STORE, "EXT_OFFLINE", "DemoAddress2");
        salesOrder.approveOrder();
        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", salesOrder.getOrderId(), "facilityId", facilityId, "userLogin", demowarehouse1));

        // 8. Check forward tracing for inventoryItemId1
        results = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("inventoryItemId", inventoryItemId1, "traceDirection", "FORWARD", "userLogin", admin));
        List<InventoryItemTraceDetail> events = ((List<List<InventoryItemTraceDetail>>) results.get("usageLog")).get(0);
        assertEquals("Wrong trace events count", 3, events.size());

        //       Should be for inventoryItemId1:
        //           a. receipt to inventoryItemId1
        //           b. inventoryItemId1 issued to the order
        //           c. inventoryItemId1 issued to another order
        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        assertInventoryTraceEvents(
                                events, null, inventoryItemId1, "RECEIPT", 1L
                        ), inventoryItemId1, null, "ORDER_ISSUED", 2L
                ), inventoryItemId1, null, "ORDER_ISSUED", 2L
        );

        // 9. Check forward tracing for inventoryItemId3
        results = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("inventoryItemId", inventoryItemId3, "traceDirection", "FORWARD", "userLogin", admin));
        events = ((List<List<InventoryItemTraceDetail>>) results.get("usageLog")).get(0);
        assertEquals("Wrong trace events count", 3, events.size());

        //       Should be for inventoryItemId3:
        //           a. receipt to inventoryItemId3
        //           b. inventoryItemId3 issued to the order
        //              inventoryItemId3 transfered to inventoryItemId4
        //           c. inventoryItemId4 issued to an order

        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        assertInventoryTraceEvents(
                                events, null, inventoryItemId3, "RECEIPT", 1L
                        ), inventoryItemId3, null, "ORDER_ISSUED", 2L
                ), inventoryItemId3, inventoryItemId4, "TRANSFER", 2L
        );

        // 10. Check backward tracing for inventoryItemId6

        // begin build tracing data starting from intermediate item
        results = runAndAssertServiceSuccess("warehouse.traceInventoryUsage", UtilMisc.toMap("inventoryItemId", inventoryItemId6, "traceDirection", "BACKWARD", "userLogin", admin));
        events = ((List<List<InventoryItemTraceDetail>>) results.get("usageLog")).get(0);
        assertEquals("Wrong trace events count", 5, events.size());

        //       Should be for inventoryItemId6:
        //           a. inventoryItemId6 issued to an order
        //           b. inventoryItemId2 used for manufacturing inventoryItemId6
        //              inventoryItemId5 used for manufacturing inventoryItemId6
        //           c. receipt to inventoryItemId2
        //              receipt to inventoryItemId5
        assertInventoryTraceEvents(
                assertInventoryTraceEvents(
                        assertInventoryTraceEvents(
                                assertInventoryTraceEvents(
                                        assertInventoryTraceEvents(
                                                events, inventoryItemId6, null, "ORDER_ISSUED", 3L
                                        ), inventoryItemId2, inventoryItemId6, "MANUF_RAW_MAT", 2L
                                ), inventoryItemId5, inventoryItemId6, "MANUF_RAW_MAT", 2L
                        ), null, inventoryItemId2, "RECEIPT", 1L
                ), null, inventoryItemId5, "RECEIPT", 1L
        );

    };

    /**
     * Tests auto taken apart on receive purchasing package product.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testAutoTakenApartPurchasingPackageProduct() throws Exception {
        // Create a test product
        final GenericValue product = createTestProduct("test Auto Taken Apart Purchasing Package Product", "PURCH_PKG_AUTO", new Long(0), demowarehouse1);
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

        // get initial inventory
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demowarehouse1);
        Map<String, Map<String, Object>> origProductInventories = inventoryAsserts.getInventories(Arrays.asList(productId, productComp1Id, productComp2Id));

        // receive 10 x Product inventory item
        Timestamp now = UtilDateTime.nowTimestamp();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("inventoryItemTypeId", NonSerializedTestSpecs.inventoryItemTypeId);
        input.put("productId", productId);
        input.put("facilityId", facilityId);
        input.put("unitCost", new BigDecimal("10.0"));
        input.put("currencyUomId", NonSerializedTestSpecs.currencyUomId);
        input.put("datetimeReceived", now);
        input.put("quantityAccepted", new BigDecimal("10.0"));
        input.put("quantityRejected", BigDecimal.ZERO);
        input.put("userLogin", demowarehouse1);
        runAndAssertServiceSuccess("receiveInventoryProduct", input);
        // check inventory
        // verify product's ATP/QOH are unchanged, still zero
        inventoryAsserts.assertInventoriesChange(productId, new BigDecimal("0.0"), origProductInventories);
        // verify productComp1Id's ATP/QOH should be 70
        inventoryAsserts.assertInventoriesChange(productComp1Id, new BigDecimal("70.0"), origProductInventories);
        // verify productComp2Id's ATP/QOH should be 30
        inventoryAsserts.assertInventoriesChange(productComp2Id, new BigDecimal("30.0"), origProductInventories);
    }
}
