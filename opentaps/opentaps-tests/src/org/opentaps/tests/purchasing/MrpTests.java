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
import java.sql.Timestamp;
import java.util.*;

import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.order.PurchaseOrderFactory;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.tests.warehouse.InventoryAsserts;

/**
 * MRP Tests.
 */
public class MrpTests extends MrpTestCase {

    private static final String MODULE = MrpTests.class.getName();

    private GenericValue admin;
    private GenericValue demopurch1;
    private GenericValue DemoSalesManager;
    private GenericValue demowarehouse1;
    private GenericValue DemoCustomer;
    private GenericValue ProductStore;
    private GenericValue DemoSupplier;
    private static final String organizationPartyId = "Company";
    private static final String productStoreId = "9000";
    private static final String facilityId = "WebStoreWarehouse";
    private static final String facilityContactMechId = "9200";
    private static final String retailStoreFacilityId = "MyRetailStore";
    private static final String thirdPartyFacilityId = "Demo3PL";
    private TimeZone timeZone = TimeZone.getDefault();
    private Locale locale = Locale.getDefault();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        admin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
        demopurch1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demopurch1"));
        demowarehouse1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        DemoSalesManager = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        DemoCustomer = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoCustomer"));
        ProductStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
        DemoSupplier = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "DemoSupplier"));
        // test that the object have been retrieved
        assertTrue("demopurch1 not null", demopurch1 != null);
        assertTrue("DemoSalesManager not null", DemoSalesManager != null);
        assertTrue("ProductStore not null", ProductStore != null);
        assertTrue("DemoSupplier not null", DemoSupplier != null);
        // set a default User
        User = demopurch1;

    }

    @Override
    public void tearDown() throws Exception {
        admin = null;
        demopurch1 = null;
        demowarehouse1 = null;
        DemoSalesManager = null;
        DemoCustomer = null;

        // get rid of all FacilityTransferPlans created by the testMrpWithTransferPlanAndTransferRequirement test, or they may interfere with other MRP
        delegator.removeByCondition("FacilityTransferPlan", EntityCondition.makeCondition("facilityTransferPlanId", EntityOperator.NOT_EQUAL, null));

        super.tearDown();
    }

    /**
     * Test the creation of a proposed purchase order for a in-stock product against a sales order:
     *
     *  1. create test product
     *  2. create a ProductFacility entry for this product with [minimumStock : 10.0; reorderQuantity : 5; daysToShip: 1] (MRP needs this information to schedule proposed requirements)
     *  3. get initial inventory (QOH = 0.0, ATP = 0.0)
     *  4. receive 20.0 units of the product as non-serialized inventory
     *  5. check product inventory changed by +20.0 relative to initial inventory (QOH = +20.0, ATP = +20.0)
     *  6. create sales order of 14x product
     *  7. check product QOH changed by +20.0 and ATP by +6.0 relative to initial inventory (QOH = +20.0, ATP = +6.0)
     *  8. run the MRP for facilityId
     *  9. verify that a proposed purchase order requirement was allocated to the correct sales order with an allocated quantity of 5.0
     * 13. approve the proposed purchase order requirement
     * 14. run the MRP again
     * 15. verify that after running MRP again no new proposed requirements are created
     *
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMrpPurchasedProduct() throws GeneralException {
        InventoryAsserts inventoryAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demopurch1);

        // 1. create test product
        GenericValue product = createTestProduct("testMrpPurchasedProduct Test Product", demopurch1);
        String productId = product.getString("productId");

        // create default price as this product should be used in order later
        assignDefaultPrice(product, new BigDecimal("100.0"), admin);

        // 2. create a ProductFacility entry for this product with [minimumStock : 10.0; reorderQuantity : 5; daysToShip: 1] (MRP needs this information to schedule proposed requirements)
        Map productFacilityContext = UtilMisc.toMap("userLogin", admin, "productId", productId, "facilityId", facilityId, "minimumStock", new BigDecimal("10.0"), "reorderQuantity", new BigDecimal("5.0"), "daysToShip", new Long(1));
        runAndAssertServiceSuccess("createProductFacility", productFacilityContext);

        // 3. get initial inventory (QOH = 0.0, ATP = 0.0)
        Map initialInventory = inventoryAsserts.getInventory(productId);

        // 4. receive 20.0 units of the product as non-serialized inventory
        receiveInventoryProduct(product, new BigDecimal("20.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("99.0"), demowarehouse1);

        // 5. check product inventory changed by +20.0 relative to initial inventory (QOH = +20.0, ATP = +20.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("20.0"), initialInventory);

        // 6. create sales order of 14x product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("14.0"));
        User = DemoSalesManager;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, productStoreId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testCreateMrpProposedPurchaseOrderAgainstSalesOrder created order [" + salesOrder.getOrderId() + "]", MODULE);

        // 7. check product QOH changed by +20.0 and ATP by +6.0 relative to initial inventory (QOH = +20.0, ATP = +6.0)
        inventoryAsserts.assertInventoryChange(productId, new BigDecimal("20.0"), new BigDecimal("6.0"), initialInventory);

        // 8. run the MRP for facilityId
        Map runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 9. verify that the proposed purchase order requirement was allocated to the correct sales order with an allocated quantity of 5.0
        String requirementId = assertRequirementExists(productId, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("5.0"));
        assertRequirementAssignedToOrder(orderId, requirementId, new BigDecimal("5.0"));

        // 10. approve the proposed purchase order requirement
        Map approveRequirementContext = UtilMisc.toMap("userLogin", demopurch1, "requirementId", requirementId);
        runAndAssertServiceSuccess("approveRequirement", approveRequirementContext);

        // 11. run the MRP again
        runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 12. verify that after running MRP again no new proposed requirements are created
        assertNoRequirementExists(productId, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED");
    }

    /**
     * Test the creation of a proposed manufacturing requirement for a manufactured product and purchasing requirement of its raw materials against a sales order:
     *
     *  1. create finished good test product (finishedGoodTestProduct) with [minimumStock : 10.0; reorderQuantity : 5; daysToShip: 1]
     *  2. create raw material test product #1 (rawMaterialTestProduct1) with [minimumStock : 25.0; reorderQuantity : 15; daysToShip: 2]
     *  3. create raw material test product #2 (rawMaterialTestProduct2) with [minimumStock : 12.0; reorderQuantity : 7; daysToShip: 3]
     *  4. create a ProductAssoc entity between finishedGoodTestProduct and rawMaterialTestProduct1 with [productAssocTypeId : 'MANUF_COMPONENT'; quantity : 2.0]
     *  5. create a ProductAssoc entity between finishedGoodTestProduct and rawMaterialTestProduct2 with [productAssocTypeId : 'MANUF_COMPONENT'; quantity : 3.0]
     *  6. create a WorkEffortGoodStandard entity for finishedGoodTestProduct with [workEffortId : 'ROUTING_COST'; workEffortGoodStdTypeId : 'ROU_PROD_TEMPLATE'; statusId : 'WEGS_CREATED']
     *  7. get initial inventory for finishedGoodTestProduct [QOH = 0.0, ATP = 0.0] (WebStoreWarehouse)
     *  8. get initial inventory for rawMaterialTestProduct1 [QOH = 0.0, ATP = 0.0] (WebStoreWarehouse)
     *  9. get initial inventory for rawMaterialTestProduct2 [QOH = 0.0, ATP = 0.0] (WebStoreWarehouse)
     * 10. receive 20.0 units of the rawMaterialTestProduct1 as non-serialized inventory (WebStoreWarehouse)
     * 11. check rawMaterialTestProduct1 inventory changed by +20.0 relative to initial inventory [QOH = +20.0, ATP = +20.0] (WebStoreWarehouse)
     * 12. create sales order of 14x finishedTestProduct
     * 14. run the MRP for WebStoreWarehouse
     * 15. verify that the proposed manufacturing receipt requirement was allocated to the correct sales order with an allocated quantity of 14.0
     * 16. verify that a proposed purchase order (mrp inventory event) of 53 units of rawMaterialTestProduct1 was created by the MRP run
     * 17. verify that a proposed purchase order (mrp inventory event) of 84 units of rawMaterialTestProduct2 was created by the MRP run
     *
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMrpManufacturedProductWithBom() throws GeneralException {
        InventoryAsserts webStoreWarehouseInvAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demopurch1);

        // 1. create finished good test product (finishedGoodTestProduct) with [minimumStock : 10.0; reorderQuantity : 5; daysToShip: 1]
        GenericValue finishedGoodTestProduct = createMrpProduct("mrp finished good test product", "FINISHED_GOOD", new Long(0), facilityId, new BigDecimal(10.0), new BigDecimal(5.0), new Long(1), admin);
        String finishedGoodTestProductId = (String) finishedGoodTestProduct.get("productId");

        // 2. create raw material test product #1 (rawMaterialTestProduct1) with [minimumStock : 25.0; reorderQuantity : 15; daysToShip: 2]
        GenericValue rawMaterialTestProduct1 = createMrpProduct("mrp test raw material test product #1", "RAW_MATERIAL", new Long(1), facilityId, new BigDecimal(25.0), new BigDecimal(15.0), new Long(2), admin);
        String rawMaterialTestProduct1Id = (String) rawMaterialTestProduct1.get("productId");

        // 3. create raw material test product #2 (rawMaterialTestProduct2) with [minimumStock : 12.0; reorderQuantity : 7; daysToShip: 3]
        GenericValue rawMaterialTestProduct2 = createMrpProduct("mrp test raw material test product #2", "RAW_MATERIAL", new Long(1), facilityId, new BigDecimal(12.0), new BigDecimal(7.0), new Long(1), admin);
        String rawMaterialTestProduct2Id = (String) rawMaterialTestProduct2.get("productId");

        // 4. create a ProductAssoc entity between finishedGoodTestProduct and rawMaterialTestProduct1 with [productAssocTypeId : 'MANUF_COMPONENT'; quantity : 2.0]
        createBOMProductAssoc(finishedGoodTestProductId, rawMaterialTestProduct1Id, new Long(10), new BigDecimal("2.0"), admin);

        // 5. create a ProductAssoc entity between finishedGoodTestProduct and rawMaterialTestProduct2 with [productAssocTypeId : 'MANUF_COMPONENT'; quantity : 3.0]
        createBOMProductAssoc(finishedGoodTestProductId, rawMaterialTestProduct2Id, new Long(20), new BigDecimal("3.0"), admin);

        // 6. create a product routing definition for test purposes
        createTestAssemblingRouting("test Mrp Manufactured Product with BOM", finishedGoodTestProductId);

        // 7. get initial inventory for finishedGoodTestProduct [QOH = 0.0, ATP = 0.0] (WebStoreWarehouse)
        Map finishedGoodTestProductInitialInventoryWSW = webStoreWarehouseInvAsserts.getInventory(finishedGoodTestProductId);

        // 8. get initial inventory for rawMaterialTestProduct1 [QOH = 0.0, ATP = 0.0] (WebStoreWarehouse)
        Map rawMaterialTestProduct1InitialInventoryWSW = webStoreWarehouseInvAsserts.getInventory(rawMaterialTestProduct1Id);

        // 9. get initial inventory for rawMaterialTestProduct2 [QOH = 0.0, ATP = 0.0] (WebStoreWarehouse)
        Map rawMaterialTestProduct2InitialInventoryWSW = webStoreWarehouseInvAsserts.getInventory(rawMaterialTestProduct2Id);

        // 10. receive 20.0 units of the rawMaterialTestProduct1 as non-serialized inventory (WebStoreWarehouse)
        receiveInventoryProduct(rawMaterialTestProduct1, new BigDecimal("20.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), facilityId, demowarehouse1);

        // 11. check rawMaterialTestProduct1 inventory changed by +20.0 relative to initial inventory [QOH = +20.0, ATP = +20.0] (WebStoreWarehouse)
        webStoreWarehouseInvAsserts.assertInventoryChange(rawMaterialTestProduct1Id, new BigDecimal("20.0"), new BigDecimal("20.0"), rawMaterialTestProduct1InitialInventoryWSW);

        // 12. create sales order of 14x finishedGoodTestProduct
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(finishedGoodTestProduct, new BigDecimal("14.0"));
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, productStoreId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testMrpManufacturedProductWithBom created order [" + salesOrder.getOrderId() + "]", MODULE);

        // 13. check finishedGoodTestProduct QOH changed by 0.0 and ATP by -14.0 relative to initial inventory (WebStoreWarehouse)
        webStoreWarehouseInvAsserts.assertInventoryChange(finishedGoodTestProductId, new BigDecimal("0.0"), new BigDecimal("-14.0"), finishedGoodTestProductInitialInventoryWSW);

        // 14. run the MRP for WebStoreWarehouse
        Map runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 15. verify that a proposed manufacturing receipt (mrp inventory event) of 24 units of finishedGoodTestProduct was created by the MRP run
        String requirementId = assertRequirementExists(finishedGoodTestProductId, facilityId, "INTERNAL_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("24.0"));

        // 16. verify that the proposed manufacturing receipt requirement was allocated to the correct sales order with an allocated quantity of 14.0
        assertRequirementAssignedToOrder(orderId, requirementId, new BigDecimal("14.0"));

        // 17. verify that a proposed purchase order receipt (mrp inventory event) of 53 units of rawMaterialTestProduct1 was created by the MRP run
        assertRequirementExists(rawMaterialTestProduct1Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("53.0"));

        // 18. verify that a proposed purchase order (mrp inventory event) of 84 units of rawMaterialTestProduct2 was created by the MRP run
        assertRequirementExists(rawMaterialTestProduct2Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("84.0"));
    }

    /**
     * Test the creation of a proposed inventory transfer and a purchase requirement in a third party warehouse against a sales order:
     *
     *  1. create test product
     *  2. create a ProductFacility entry (Demo3PL) entry for this product with [minimumStock : 15.0; reorderQuantity : 7; daysToShip: 1] (MRP needs this information to schedule proposed requirements)
     *  3. get initial inventory [QOH = 0.0, ATP = 0.0] (Demo3PL)
     *  4. receive 20.0 units of the product as non-serialized inventory (Demo3PL)
     *  5. check product inventory changed by +20.0 relative to initial inventory [QOH = +20.0, ATP = +20.0] (Demo3PL)
     *  6. create another ProductFacility entry (WebStoreWarehouse) entry for this product with [minimumStock : 0.0; reorderQuantity : 5.0; daysToShip: 1] (MRP needs this information to schedule proposed requirements)
     *  7. get initial inventory [QOH = 0.0, ATP = 0.0] (WebStoreWarehouse)
     *  8. create sales order of 14x product
     *  9. check product QOH changed by 0.0 and ATP by -14.0 relative to initial inventory (WebStoreWarehouse)
     * 10. run the MRP for WebStoreWarehouse
     * 11. verify that a proposed inventory transfer of 14 units of test product was created by the MRP run
     * 12. cancel the proposed inventory transfer requirement
     * 13. run the MRP for WebStoreWarehouse again
     * 14. verify that after running MRP again a new proposed inventory transfer is created
     * 15. run the MRP for Demo3PL
     * 16. verify that a proposed purchase order (mrp inventory event) of 9 units of test product was created by the MRP run for Demo3PL
     *
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMrpProposedInventoryTransfer() throws GeneralException {
        InventoryAsserts webStoreWarehouseInvAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demopurch1);
        InventoryAsserts thirdPartyWarehouseInvAsserts = new InventoryAsserts(this, thirdPartyFacilityId, organizationPartyId, demopurch1);

        // 1. create test product
        GenericValue product = createTestProduct("testMrpProposedInventoryTransfer Test Product", demopurch1);
        String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("100.0"), admin);
        Debug.logInfo("create product [" + productId + "]", MODULE);
        // 2. create a ProductFacility entry (Demo3PL) entry for this product with [minimumStock : 15.0; reorderQuantity : 7; daysToShip: 1] (MRP needs this information to schedule proposed requirements)
        Map productFacilityContext = UtilMisc.toMap("userLogin", admin, "productId", productId, "facilityId", thirdPartyFacilityId, "minimumStock", new BigDecimal("15.0"), "reorderQuantity", new BigDecimal("7.0"), "daysToShip", new Long(1));
        runAndAssertServiceSuccess("createProductFacility", productFacilityContext);

        // 3. get initial inventory [QOH = 0.0, ATP = 0.0] (Demo3PL)
        Map initialInventoryTPW = thirdPartyWarehouseInvAsserts.getInventory(productId);

        // 4. receive 20.0 units of the product as non-serialized inventory (Demo3PL)
        receiveInventoryProduct(product, new BigDecimal("20.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), thirdPartyFacilityId, demowarehouse1);

        // 5. check product inventory changed by +20.0 relative to initial inventory [QOH = +20.0, ATP = +20.0] (Demo3PL)
        thirdPartyWarehouseInvAsserts.assertInventoryChange(productId, new BigDecimal("20.0"), new BigDecimal("20.0"), initialInventoryTPW);

        // 6. create another ProductFacility entry (WebStoreWarehouse) entry for this product with [minimumStock : 0.0; reorderQuantity : 5.0; daysToShip: 1]
        //     (MRP needs this information to schedule proposed requirements)
        productFacilityContext = UtilMisc.toMap("userLogin", admin, "productId", productId, "facilityId", facilityId, "minimumStock", new BigDecimal("0.0"), "reorderQuantity", new BigDecimal("5.0"), "daysToShip", new Long(1));
        runAndAssertServiceSuccess("createProductFacility", productFacilityContext);

        // 7. get initial inventory [QOH = 0.0, ATP = 0.0] (Demo3PL)
        Map initialInventoryWSW = webStoreWarehouseInvAsserts.getInventory(productId);

        // 8. create sales order of 14x product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("14.0"));
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, productStoreId);
        Debug.logInfo("testMrpProposedInventoryTransfer created order [" + salesOrder.getOrderId() + "]", MODULE);

        // 9. check product QOH changed by 0.0 and ATP by -14.0 relative to initial inventory (WebStoreWarehouse)
        webStoreWarehouseInvAsserts.assertInventoryChange(productId, new BigDecimal("0.0"), new BigDecimal("-14.0"), initialInventoryWSW);

        // 10. run the MRP for WebStoreWarehouse
        Map runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId);
        Debug.logInfo("before run opentaps.runMrp", MODULE);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 11. verify that a proposed inventory transfer of 14 units of test product was created by the MRP run
        String inventoryTransferId = assertInventoryTransferRequested(thirdPartyFacilityId, facilityId, productId, new BigDecimal("14.0"));

        // 12. cancel the proposed inventory transfer requirement
        runAndAssertServiceSuccess("cancelInventoryTransfer", UtilMisc.toMap("inventoryTransferId", inventoryTransferId, "userLogin", demopurch1));

        // check product inventory is still +20.0 relative to initial inventory [QOH = +20.0, ATP = +20.0] (Demo3PL)
        thirdPartyWarehouseInvAsserts.assertInventoryChange(productId, new BigDecimal("20.0"), new BigDecimal("20.0"), initialInventoryTPW);
        // check inventory in WebStoreWarehouse has not change either
        webStoreWarehouseInvAsserts.assertInventoryChange(productId, new BigDecimal("0.0"), new BigDecimal("-14.0"), initialInventoryWSW);

        // 13. run the MRP for WebStoreWarehouse again
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 14. verify that after running MRP again a new proposed inventory transfer is created
        assertInventoryTransferRequested(thirdPartyFacilityId, facilityId, productId, new BigDecimal("14.0"));

        // 15. run the MRP for Demo3PL
        runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", thirdPartyFacilityId);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 16. verify that a proposed purchase order (mrp inventory event) of 9 units of test product was created by the MRP run for Demo3PL
        // stock dropped from 20 -> 6, below minimum stock 15
        assertRequirementExists(productId, thirdPartyFacilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("9.0"));
    }

    /**
     * Test MRP for effects of pending inventory transfers and purchase orders, which should reduce the amount of purchasing that is required
     * Also verifies the reorderQuantity setting, the allocation of Requirements to the orders, and the association of Requirements to Supplier
     *
     * 1.  Create a product with a price of $10 and a SupplierProduct to purchase it from DemoSupplier for $5
     * 2.  Create a ProductFacility for this product in WebStoreWarehouse of minimumStock = 0, reorderQuantity = 25
     * 3.  Receive 100 units of this product at $5 into Demo3PL warehouse
     * 4.  Create an inventory transfer from Demo3PL to WebStoreWarehouse for 50 units of this product
     * 5.  Create 3 sales order for 15 units, 12, 23 of this product
     * 6.  Approve the last 2 sales orders; the first purchase order is in the CREATED state but should still be counted for MRP
     * 7.  Run MRP for WebStoreWarehouse
     * 8.  Verify that no Proposed Requirement is created for this Product
     * 9.  Cancel inventory transfer from #4
     * 10.  Run MRP for WebStoreWarehouse
     * 11.  Verify a Requirement of type PRODUCT_REQUIREMENT for 50 units of the product is created in the Proposed state,
     *      that it has DemoSupplier associated as SUPPLIER in RequirementRole,
     *      that it has OrderRequirementCommitment associated with all three sales orders for quantities 15, 12, and 23
     * 12.  Create a purchase order for 40 units of the product from DemoSupplier
     * 13.  Approve the purchase order
     * 14.  Run MRP for WebStoreWarehouse
     * 15.  Verify a Requirement of type PRODUCT_REQUIREMENT for 25 units (reorderQuantity) of the product is created in the Proposed state,
     *      that it has DemoSupplier associated as SUPPLIER in RequirementRole,
     *      that it has OrderRequirementCommitment associated with the third sales order for quantity 10
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMrpPendingInventoryTransfersAndPurchaseOrders() throws GeneralException {

        // 1. Create a product with a price of $10 and a SupplierProduct to purchase it from DemoSupplier for $5
        GenericValue testProduct = createTestProduct("testMrpPendInvTransAndPurchOrder Test Product", demopurch1);
        String productId = testProduct.getString("productId");
        assignDefaultPrice(testProduct, new BigDecimal("10.0"), admin);
        createMainSupplierForProduct(productId, "DemoSupplier", new BigDecimal("5.0"), "USD", new BigDecimal("1.0"), admin);

        // 2. Create a ProductFacility for this product in WebStoreWarehouse of minimumStock = 0, reorderQuantity = 25
        Map productFacilityContext = UtilMisc.toMap("userLogin", admin, "productId", productId, "facilityId", facilityId, "minimumStock", new BigDecimal("0.0"), "reorderQuantity", new BigDecimal("25.0"), "daysToShip", new Long(1));
        runAndAssertServiceSuccess("createProductFacility", productFacilityContext);

        // 3. Receive 100 units of this product at $5 into MyRetailWarehouse warehouse
        Map<String, Object> result = receiveInventoryProduct(testProduct, new BigDecimal("100.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), retailStoreFacilityId, demowarehouse1);
        String inventoryItemId = (String) result.get("inventoryItemId");

        // 4. Create an inventory transfer from Demo3PL to WebStoreWarehouse for 50 units of this product
        Map transferContext = UtilMisc.toMap("facilityId", retailStoreFacilityId, "facilityIdTo", facilityId, "inventoryItemId", inventoryItemId, "xferQty", new BigDecimal("50.0"), "statusId", "IXF_REQUESTED", "userLogin", demowarehouse1);
        transferContext.put("sendDate", UtilDateTime.nowTimestamp());  // critical - otherwise MRP will ignore this inventory transfer
        result = runAndAssertServiceSuccess("createInventoryTransfer", transferContext);
        String inventoryTransferId = (String) result.get("inventoryTransferId");

        // 5. Create 3 sales order for 15 units, 12, 23 of this product
        User = DemoSalesManager;
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(testProduct, new BigDecimal("15.0"));
        SalesOrderFactory salesOrder1 = testCreatesSalesOrder(order, DemoCustomer, productStoreId);
        pause("Workaround MYSQL Timestamps");
        order.put(testProduct, new BigDecimal("12.0"));
        SalesOrderFactory salesOrder2 = testCreatesSalesOrder(order, DemoCustomer, productStoreId);
        pause("Workaround MYSQL Timestamps");
        order.put(testProduct, new BigDecimal("23.0"));
        SalesOrderFactory salesOrder3 = testCreatesSalesOrder(order, DemoCustomer, productStoreId);
        pause("Workaround MYSQL Timestamps");

        // 6. Approve the last 2 sales orders; the first purchase order is in the CREATED state but should still be counted for MRP
        salesOrder2.approveOrder();
        salesOrder3.approveOrder();

        // 7. Run MRP for WebStoreWarehouse
        Map runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 8. Verify that no Proposed Requirement is created for this Product
        assertNoRequirementExists(productId, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED");

        // 9. Cancel inventory transfer from #4
        runAndAssertServiceSuccess("cancelInventoryTransfer", UtilMisc.toMap("inventoryTransferId", inventoryTransferId, "userLogin", demopurch1));

        // 10. Run MRP for WebStoreWarehouse
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 11. Verify a Requirement of type PRODUCT_REQUIREMENT for 50 units of the product is created in the Proposed state, that
        //      it has DemoSupplier associated as SUPPLIER in RequirementRole, that it has OrderRequirementCommitment associated with
        //      all three sales orders for quantities 15, 12, and 23
        String requirementId = assertRequirementExists(productId, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("50.0"));
        assertRequirementAssignedToSupplier("DemoSupplier", requirementId);
        assertRequirementAssignedToOrder(salesOrder1.getOrderId(), requirementId, new BigDecimal("15.0"));
        assertRequirementAssignedToOrder(salesOrder2.getOrderId(), requirementId, new BigDecimal("12.0"));
        assertRequirementAssignedToOrder(salesOrder3.getOrderId(), requirementId, new BigDecimal("23.0"));

        // 12. Create a purchase order for 40 units of the product from DemoSupplier
        User = demopurch1;
        order.put(testProduct, new BigDecimal("40.0"));
        PurchaseOrderFactory purchaseOrder = testCreatesPurchaseOrder(order, DemoSupplier, facilityContactMechId);

        // 13. Approve the purchase order
        purchaseOrder.approveOrder();

        // 14. Run MRP for WebStoreWarehouse
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 15. Verify a Requirement of type PRODUCT_REQUIREMENT for 25 units (reorderQuantity) of the product is created in the
        //      Proposed state, that it has DemoSupplier associated as SUPPLIER in RequirementRole, that it has OrderRequirementCommitment
        //      associated with the third sales order for quantity 10
        requirementId = assertRequirementExists(productId, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("25.0"));
        assertRequirementAssignedToSupplier("DemoSupplier", requirementId);
        assertRequirementAssignedToOrder(salesOrder3.getOrderId(), requirementId, new BigDecimal("10.0"));
    }


    /**
     * Tests backup warehouse and inventory transfer: MRP should create inventory transfer from backup warehouse, then requirement for surplus amount.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMrpBackupInventoryWarehouse1() throws GeneralException {

        // 1. Create a product with a price of $10 and a SupplierProduct to purchase it from DemoSupplier for $5
        GenericValue testProduct = createTestProduct("testMrpBackupInventoryWarehouse1 Test Product", demopurch1);
        String productId = testProduct.getString("productId");
        assignDefaultPrice(testProduct, new BigDecimal("10.0"), admin);
        createMainSupplierForProduct(productId, "DemoSupplier", new BigDecimal("5.0"), "USD", new BigDecimal("1.0"), admin);

        // 2. Create a ProductFacility for this product in WebStoreWarehouse of minimumStock = 0, reorderQuantity = 1
        Map productFacilityContext = UtilMisc.toMap("userLogin", admin, "productId", productId, "facilityId", facilityId, "minimumStock", new BigDecimal("0.0"), "reorderQuantity", new BigDecimal("1.0"), "daysToShip", new Long(1));
        runAndAssertServiceSuccess("createProductFacility", productFacilityContext);

        // 3. Receive 50 units of this product at $5 into MyRetailWarehouse warehouse
        receiveInventoryProduct(testProduct, new BigDecimal("50.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), thirdPartyFacilityId, demowarehouse1);

        // 4. Create sales order for 25 units of this product
        User = DemoSalesManager;
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(testProduct, new BigDecimal("25.0"));
        SalesOrderFactory salesOrder1 = testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // 6. Approve the sales orders
        salesOrder1.approveOrder();

        // 7. Run MRP for WebStoreWarehouse
        Map runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId);
        Debug.logInfo("before run opentaps.runMrp", MODULE);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 8. Verify that 25 units are to be transferred from Demo3PL to WebStoreWarehouse
        assertInventoryTransferRequested(thirdPartyFacilityId, facilityId, productId, new BigDecimal("25.0"));
    }


    /**
     * Tests backup warehouse and inventory transfer: MRP should create inventory transfer from backup warehouse, then requirement for surplus amount.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMrpBackupInventoryWarehouse2() throws GeneralException {

        // 1. Create a product with a price of $10 and a SupplierProduct to purchase it from DemoSupplier for $5
        GenericValue testProduct = createTestProduct("testMrpBackupInventoryWarehouse2 Test Product", demopurch1);
        String productId = testProduct.getString("productId");
        assignDefaultPrice(testProduct, new BigDecimal("10.0"), admin);
        createMainSupplierForProduct(productId, "DemoSupplier", new BigDecimal("5.0"), "USD", new BigDecimal("1.0"), admin);

        // 2. Create a ProductFacility for this product in WebStoreWarehouse of minimumStock = 0, reorderQuantity = 1
        Map productFacilityContext = UtilMisc.toMap("userLogin", admin, "productId", productId, "facilityId", facilityId, "minimumStock", new BigDecimal("0.0"), "reorderQuantity", new BigDecimal("1.0"), "daysToShip", new Long(1));
        runAndAssertServiceSuccess("createProductFacility", productFacilityContext);

        // 3. Receive 10 units of this product at $5 into MyRetailWarehouse warehouse
        receiveInventoryProduct(testProduct, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("1.0"), thirdPartyFacilityId, demowarehouse1);

        // 4. Create sales order for 25 units of this product
        User = DemoSalesManager;
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(testProduct, new BigDecimal("25.0"));
        SalesOrderFactory salesOrder1 = testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // 6. Approve the sales orders
        salesOrder1.approveOrder();

        // 7. Run MRP for WebStoreWarehouse
        Map runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId);
        Debug.logInfo("before run opentaps.runMrp", MODULE);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 8. Verify that 10 units are to be transferred from Demo3PL to WebStoreWarehouse
        assertInventoryTransferRequested(thirdPartyFacilityId, facilityId, productId, new BigDecimal("10.0"));

        // 9. Verify that a requirement for 15 units were created
        assertRequirementExists(productId, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("15.0"));
    }

    /**
     * 1.  Create 3-tier BOM: top level = 2x mid level = 5x bottom level.  Top level product is sold, bottom level product is purchased
     * 2.  Create sales order for top level product
     * 3.  Run MRP
     * 4.  Verify that requirements are now proposed: top and mid level production requirements; bottom level purchasing requirement
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMrpForMultiLevelProduct() throws GeneralException {

        // 1. Create a 3-tier BOM with top level, mid level, and bottom level product
        GenericValue topLevelProduct = createTestProduct("testMrpMultiLevelProduct Top Level Product", demopurch1);
        String topLevelProductId = topLevelProduct.getString("productId");
        assignDefaultPrice(topLevelProduct, new BigDecimal("10.0"), admin);

        GenericValue midLevelProduct = createTestProduct("testMrpMultiLevelProduct Mid Level Product", demopurch1);
        String midLevelProductId = midLevelProduct.getString("productId");

        GenericValue bottomLevelProduct = createTestProduct("testMrpMultiLevelProduct Bottom Level Product", demopurch1);
        String bottomLevelProductId = bottomLevelProduct.getString("productId");

        // all products have facility minimum thresholds
        createProductFacility(topLevelProductId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);
        createProductFacility(midLevelProductId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);
        createProductFacility(bottomLevelProductId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);

        // bottom level product can be purchased for $0.25
        createMainSupplierForProduct(bottomLevelProductId, "DemoSupplier", new BigDecimal("0.25"), "USD", new BigDecimal("0.0"), admin);

        // create BOM from top level to mid level to bottom level product
        createBOMProductAssoc(topLevelProductId, midLevelProductId, new Long(10), new BigDecimal("2.0"), admin);
        createBOMProductAssoc(midLevelProductId, bottomLevelProductId, new Long(10), new BigDecimal("5.0"), admin);

        // create sales order
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(topLevelProduct, new BigDecimal("10.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // run MRP
        Map runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // Validate requirements created
        assertRequirementExists(topLevelProductId, facilityId, "INTERNAL_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("10.0"));
        // if mid level product had been of type "WIP", then no requirement would have been created for it
        assertRequirementExists(midLevelProductId, facilityId, "INTERNAL_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("20.0"));
        assertRequirementExists(bottomLevelProductId, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("100.0"));

    }


    /**
     * Test MRP with a basic sales forecast at 75% of forecasted quantities
     * 1.  Create a product with minimum stock which could be purchased
     * 2.  Create a sales order of 10 for the product
     * 3.  Create a SalesForecastItem which is 1 day in the past for 10
     * 4.  Create a SalesForecastItem which is 10 days in the future for 20
     * 5.  Run MRP  with % of sales forecast = 75 and default years offset = 1
     * 6.  Verify that 2 requirements are created:
     * (a) Requirement for 15 ten days in the future
     * (b) Requirement for 10 one year in the future
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMrpWithBasicSalesForecast() throws GeneralException {
        // create a product
        GenericValue product = createTestProduct("testMrpWithBasicSalesForecast Product", demopurch1);
        String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);

        // set a facility minimum stock for it
        createProductFacility(productId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);

        // associate it with a supplier
        createMainSupplierForProduct(productId, "DemoSupplier", new BigDecimal("5.0"), "USD", new BigDecimal("0.0"), admin);

        // create a sales order
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("10.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // create an expired sales forecast
        createSalesForecastItem(productId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -1, timeZone, locale), new BigDecimal("100.0"));

        // create a good sales forecast in the future
        createSalesForecastItem(productId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 10, timeZone, locale), new BigDecimal("20.0"));

        // run MRP.  default years offset is 1 year when running MRP from the screens
        Map runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId, "defaultYearsOffset", new Integer(1), "percentageOfSalesForecast", new BigDecimal("75.00"));
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // cutoff date between the 2 requirements created
        Timestamp cutoffDate = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 11, timeZone, locale);

        // a requirement of 15 should have been created in 10 days, ie before the cutoff date
        assertRequirementExists(productId, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("15.0"), null, cutoffDate);

        // a requirement of 10 should have been created a year into the future (the default expectecd ship date for sales orders), or after the cutoff date
        assertRequirementExists(productId, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("10.0"), cutoffDate, null);

    }

    /**
     * Test inventory transfer plans and the transfer requirement life cycle
     * 1.  Create a product which can be purchased, with minimum stock in both main and backup warehouses
     * 2.  Receive 100 into backup warehouse
     * 3.  Create facility transfer plan of -30, 0, 30, 60 days
     * 4.  Create an approved transfer requirement to verify that it will be counted
     * 5.  Create sales forecasts
     * 6.  Verify that MRP creates transfer requirements on the dates of the facility transfer plan
     * 7.  Approve all the transfer requirements
     * 8.  Run MRP again on the backup warehouse and verify that it creates purchasing requirements for inventory to be transferred out
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMrpWithTransferPlanAndTransferRequirements() throws GeneralException {
        // create a product
        GenericValue product = createTestProduct("testMrpWithTransferPlanAndTransferRequirements Product", demopurch1);
        String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);
        createMainSupplierForProduct(productId, "DemoSupplier", new BigDecimal("5.0"), "USD", new BigDecimal("0.0"), admin);

        // set a facility minimum stock for it in both facilities, so MRP will generate requirements in both facilities
        createProductFacility(productId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);
        createProductFacility(productId, thirdPartyFacilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);

        // receive 300 into the backup warehouse
        receiveInventoryProduct(product, new BigDecimal("300.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), thirdPartyFacilityId, demowarehouse1);
        // receive 50 into the primary (shipping) warehouse, so we can see the interaction of existing inventory with MRP and transfers
        receiveInventoryProduct(product, new BigDecimal("50.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), facilityId, demowarehouse1);

        // create transfer plans
        createTransferPlan(thirdPartyFacilityId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, -30, timeZone, locale));
        createTransferPlan(thirdPartyFacilityId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 0, timeZone, locale));
        createTransferPlan(thirdPartyFacilityId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 30, timeZone, locale));
        createTransferPlan(thirdPartyFacilityId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 60, timeZone, locale));

        // create an approved requirement for an inventory transfer to make sure it is counted in MRP
        Map requirementValue = UtilMisc.toMap("requirementId", delegator.getNextSeqId("Requirement"), "requirementTypeId", "TRANSFER_REQUIREMENT", "facilityId", thirdPartyFacilityId, "facilityIdTo", facilityId, "productId", productId, "statusId", "REQ_APPROVED");
        requirementValue.put("description", "Manually created for testMrpWithTransferPlanAndTransferRequirements");
        Timestamp testRequirementDate = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 10, timeZone, locale);
        requirementValue.put("requirementStartDate", testRequirementDate);
        requirementValue.put("requiredByDate", testRequirementDate);
        requirementValue.put("quantity", new BigDecimal("100.0"));
        delegator.create("Requirement", requirementValue);

        // create staggered sales forecasts which will cause requirements to be created in MRP
        createSalesForecastItem(productId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 10, timeZone, locale), new BigDecimal("200.0"));
        createSalesForecastItem(productId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 20, timeZone, locale), new BigDecimal("120.0"));
        createSalesForecastItem(productId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 40, timeZone, locale), new BigDecimal("250.0"));
        createSalesForecastItem(productId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 50, timeZone, locale), new BigDecimal("110.0"));
        createSalesForecastItem(productId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 70, timeZone, locale), new BigDecimal("180.0"));

        // run MRP for both facilities, WebStoreWarehouse should run first
        Map runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId, "defaultYearsOffset", new Integer(1), "percentageOfSalesForecast", new BigDecimal("60.00"), "createTransferRequirements", Boolean.TRUE);
        Debug.logInfo("before run opentaps.runMrp", MODULE);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // these requirements are transfer requirements from thirdPartyFacilityId to main facilityId
        // 30 days from now, there should be a transfer requirement for (200+120)*0.6 - 100 - 50 = 42
        assertRequirementsTotalQuantityCorrect(productId, thirdPartyFacilityId, "TRANSFER_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("42"), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 29, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 31, timeZone, locale));
        // 60 days from now, transfer requirement should be created for (250+110)*0.6 = 216
        assertRequirementsTotalQuantityCorrect(productId, thirdPartyFacilityId, "TRANSFER_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("216"), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 59, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 61, timeZone, locale));
        // the last transfer requirement for 180*0.6 = 108 should happen when the sales forecast is created, or 70 days later, because there are no more transfer plans after 60 days
        assertRequirementsTotalQuantityCorrect(productId, thirdPartyFacilityId, "TRANSFER_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("108"), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 69, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 71, timeZone, locale));

        // approve the transfer requirements
        List<GenericValue> requirements = delegator.findByAnd("Requirement", UtilMisc.toMap("productId", productId, "requirementTypeId", "TRANSFER_REQUIREMENT", "statusId", "REQ_PROPOSED", "facilityId", thirdPartyFacilityId, "facilityIdTo", facilityId));
        for (GenericValue requirement : requirements) {
            runAndAssertServiceSuccess("updateRequirement", UtilMisc.toMap("requirementId", requirement.get("requirementId"), "statusId", "REQ_APPROVED", "userLogin", demopurch1));
        }

        // now run MRP again, and it should create purchasing requirements in the backup warehouse
        runMrpContext = UtilMisc.toMap("userLogin", demopurch1, "facilityId", thirdPartyFacilityId, "defaultYearsOffset", new Integer(1), "percentageOfSalesForecast", new BigDecimal("60.00"), "createTransferRequirements", Boolean.TRUE);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // verify that purchasing requirements are created
        // by day 30, inventory in backup facility is 300 - 100 (approved transfer requirement) - 42 = 158
        // by day 60, inventory in backup facility is 158 - 216 = -58, so 58 will be needed
        assertRequirementsTotalQuantityCorrect(productId, thirdPartyFacilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("58"), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 59, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 61, timeZone, locale));
        // the transfer of 108 on day 70 causes 108 to be needed in the backup facility
        assertRequirementsTotalQuantityCorrect(productId, thirdPartyFacilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("108"), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 69, timeZone, locale), UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 71, timeZone, locale));

    }

    /**
     * This test verifies that MRP will create transfer requirements from a specified warehouse rather than the pre-defined
     * backup warehouse if it is so defined in <code>ProductFacility</code>.
     * @throws GeneralException if an error occurs
     */
    public void testMrpWithTransferRequirementFromSpecifiedWarehouse() throws GeneralException {
        // create a product
        GenericValue product = createTestProduct("test Mrp With Transfer Requirement From Specified Warehouse Product", demopurch1);
        String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);
        createMainSupplierForProduct(productId, "DemoSupplier", new BigDecimal("5.0"), "USD", new BigDecimal("0.0"), admin);

        // set a facility minimum stock for it in both facilities, so MRP will generate requirements in both facilities
        // set its replenishment method in WebStoreWarehouse to "Transfer from specified warehouse" from retail store facility
        createProductFacility(productId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), retailStoreFacilityId, "PF_RM_SPECIF", admin);
        createProductFacility(productId, thirdPartyFacilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);

        // receive 10 into WebStoreWarehouse and 100 into retail store
        receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), facilityId, demowarehouse1);
        receiveInventoryProduct(product, new BigDecimal("100.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), retailStoreFacilityId, demowarehouse1);

        // create a sales order for 20
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("20.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // run MRP in WebStoreWarehouse with createTransferRequirements set to TRUE
        Map<String, Object> runMrpContext = UtilMisc.<String, Object>toMap("userLogin", demopurch1, "facilityId", facilityId, "createTransferRequirements", true);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // verify that a transfer requirement for 10 has been created from retail store facility to WebStoreWarehouse
        assertTransfertRequirementExists(productId, retailStoreFacilityId, facilityId, new BigDecimal("10.0"));
    }

    /**
     * This test verifies that if you configure <code>ProductFacility</code> to have no transfer, then no inventory transfers are created
     * and the product will be purchased at the facility where it is needed.
     * @throws GeneralException if an error occurs
     */
    public void testMrpWithNoTransfer() throws GeneralException {
        // create a product
        GenericValue product = createTestProduct("test Mrp With No Transfer Product", demopurch1);
        String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);
        createMainSupplierForProduct(productId, "DemoSupplier", new BigDecimal("5.0"), "USD", new BigDecimal("0.0"), admin);

        // set a facility minimum stock for it in both facilities, so MRP will generate requirements in both facilities
        // set its replenishment method in WebStoreWarehouse to "No transfer"
        createProductFacility(productId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), "PF_RM_NEVER", admin);
        createProductFacility(productId, thirdPartyFacilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);

        // receive 10 into WebStoreWarehouse and 100 into retail store
        receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), facilityId, demowarehouse1);
        receiveInventoryProduct(product, new BigDecimal("100.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), retailStoreFacilityId, demowarehouse1);

        // create a sales order for 20
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("20.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // run MRP in WebStoreWarehouse with createTransferRequirements set to TRUE
        Map<String, Object> runMrpContext = UtilMisc.<String, Object>toMap("userLogin", demopurch1, "facilityId", facilityId, "createTransferRequirements", true);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // verify that no transfer requirement has been created in the WebStoreWarehouse for the product
        assertNoTransfertRequirementExists(productId, null, facilityId);
        // verify that a product (purchasing) requirement for 10 has been created in the WebStoreWarehouse
        assertPurchasingRequirementExists(productId, facilityId, new BigDecimal("10.0"));
    }

    /**
     * Verify that if "always transfer from backup warehouse" is set, then a transfer from backup warehouse will be created
     * even when it has no inventory.
     * @throws GeneralException if an error occurs
     */
    public void testMrpWithTransferRequirementFromBackupWarehouseWithNoInventory() throws GeneralException {
        // create a product
        GenericValue product = createTestProduct("test Mrp With Transfer Requirement From Backup Warehouse With No Inventory Product", demopurch1);
        String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);
        createMainSupplierForProduct(productId, "DemoSupplier", new BigDecimal("5.0"), "USD", new BigDecimal("0.0"), admin);

        // set a facility minimum stock for it in both facilities, so MRP will generate requirements in both facilities
        // set its replenishment method in WebStoreWarehouse to "Always transfer from backup warehouse"
        createProductFacility(productId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), "PF_RM_BACKUP_ALW", admin);
        createProductFacility(productId, thirdPartyFacilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);

        // receive 10 into WebStoreWarehouse
        receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), facilityId, demowarehouse1);

        // create a sales order for 20
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("20.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // run MRP in WebStoreWarehouse with createTransferRequirements set to TRUE
        Map<String, Object> runMrpContext = UtilMisc.<String, Object>toMap("userLogin", demopurch1, "facilityId", facilityId, "createTransferRequirements", true);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // verify that a transfer requirement for 10 has been created from thirdPartyFacilityId to facilityId(WebStoreWarehouse)
        assertTransfertRequirementExists(productId, thirdPartyFacilityId, facilityId, new BigDecimal("10.0"));
    }


    /**
     * Verify that if "always transfer from backup warehouse" is set, then a transfer from backup warehouse will be created
     * even when it has no inventory.
     * @throws GeneralException if an error occurs
     */
    public void testMrpWithTransferRequirementFromSpecifiedWarehouseWithNoInventory() throws GeneralException {
        // create a product
        GenericValue product = createTestProduct("test Mrp With Transfer Requirement From Backup Warehouse With No Inventory Product", demopurch1);
        String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);
        createMainSupplierForProduct(productId, "DemoSupplier", new BigDecimal("5.0"), "USD", new BigDecimal("0.0"), admin);

        // set a facility minimum stock for it in both facilities, so MRP will generate requirements in both facilities
        // set its replenishment method in WebStoreWarehouse to "Always transfer from specified warehouse" which is Retail Store facility
        createProductFacility(productId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), retailStoreFacilityId, "PF_RM_SPECIF_ALW", admin);
        createProductFacility(productId, thirdPartyFacilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);

        // receive 10 into WebStoreWarehouse
        receiveInventoryProduct(product, new BigDecimal("10.0"), "NON_SERIAL_INV_ITEM", new BigDecimal("5.0"), facilityId, demowarehouse1);

        // create a sales order for 20
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("20.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // run MRP in WebStoreWarehouse with createTransferRequirements set to TRUE
        Map<String, Object> runMrpContext = UtilMisc.<String, Object>toMap("userLogin", demopurch1, "facilityId", facilityId, "createTransferRequirements", true);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // verify that a transfer requirement for 10 has been created from retail store facility to facilityId(WebStoreWarehouse)
        assertTransfertRequirementExists(productId, retailStoreFacilityId, facilityId, new BigDecimal("10.0"));
    }

    /**
     * Verify that Pending internal requirements are created and accounted for correctly.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMrpWithPendingInternalRequirements() throws GeneralException {
        // create a manufactured product
        final GenericValue product = createTestProduct("test Mrp With Pending Internal Requirements Product", demopurch1);
        final String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);

        // set a facility minimum stock for it so MRP will generate requirements
        createProductFacility(productId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);

        // create its component
        final GenericValue productComp1 = createTestProduct("test Mrp With Pending Internal Requirements Product - Component 1", demopurch1);
        final String productComp1Id = productComp1.getString("productId");
        final GenericValue productComp2 = createTestProduct("test Mrp With Pending Internal Requirements Product - Component 2", demopurch1);
        final String productComp2Id = productComp2.getString("productId");
        createBOMProductAssoc(productId, productComp1Id, new Long("10"), new BigDecimal("7.0"), admin);
        createBOMProductAssoc(productId, productComp2Id, new Long("10"), new BigDecimal("3.0"), admin);

        // creates an Assembly routing with one task for product
        createTestAssemblingRouting("test Mrp With Pending Internal Requirements", productId);

        // create a sales order for 1
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("1.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // create a sales order for 10
        order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("10.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // create a sales order for 100, for next month
        Timestamp order3Date = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.MONTH, 1);
        order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("100.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId, order3Date);

        // create a sales order for 1000, for 2 years in the future
        // we want to avoid the orders for 1 and 10 being grouped with this order on the same Requirement, which could happen
        // when MRP is run with defaultOffsetYears = 1
        Timestamp order4Date = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.YEAR, 2);
        order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("1000.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId, order4Date);

        // run the MRP
        Map<String, Object> runMrpContext = UtilMisc.<String, Object>toMap("userLogin", demopurch1, "facilityId", facilityId, "defaultYearsOffset", new Integer(1), "percentageOfSalesForecast", new BigDecimal("0.0"), "createPendingManufacturingRequirements", true);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // verify the proposed pending internal requirements
        assertRequirementsTotalQuantityCorrect(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("1111.0"), null, null);
        // check for requirements individually note that first two order creates a 11 requirement
        String req1Id = assertRequirementExistsWithQuantity(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("11.0"));
        assertRequirementExistsWithQuantity(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("100.0"));
        assertRequirementExistsWithQuantity(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("1000.0"));

        // approve the first requirement
        runAndAssertServiceSuccess("approveRequirement", UtilMisc.toMap("userLogin", demopurch1, "requirementId", req1Id));

        // run the MRP again and check this requirement is not re proposed
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);
        assertRequirementsTotalQuantityCorrect(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_APPROVED", new BigDecimal("11.0"), null, null);
        assertRequirementsTotalQuantityCorrect(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("1100.0"), null, null);
        assertNoRequirementExistsWithQuantity(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("11.0"));
        String req2Id = assertRequirementExistsWithQuantity(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("100.0"));
        assertRequirementExistsWithQuantity(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("1000.0"));

        // approve the second
        runAndAssertServiceSuccess("approveRequirement", UtilMisc.toMap("userLogin", demopurch1, "requirementId", req2Id));

        // create a production run from those two requirements
        Map produceService = runAndAssertServiceSuccess("createProductionRunsFromPendingInternalRequirements", UtilMisc.toMap("userLogin", demopurch1, "requirementIds", Arrays.asList(req1Id, req2Id)));
        List<String> productionRunIds = (List<String>) produceService.get("productionRunIds");
        assertEquals("Should have created only one production run from the pending internal requirements.", 1, productionRunIds.size());

        // get the production run
        GenericValue prun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunIds.get(0)));
        assertNotNull("Production run not found after createProductionRunsFromPendingInternalRequirements.", prun);

        // run the MRP again and check there is only 1 requirement
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);
        // check the previous requirements are marked closed
        assertRequirementsTotalQuantityCorrect(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_CLOSED", new BigDecimal("111.0"), null, null);
        // check the last requirement is still there (it may be split because of the timing of the production runs / mrp events)
        assertRequirementsTotalQuantityCorrect(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("1000.0"), null, null);
    }

    /**
     * Verify that MRP correctly generates requirements for the components of approved Pending internal requirements, and if
     * its quantity is changed, the component requirements' quantities will also be changed.
     * @throws GeneralException if an error occurs
     */
    public void testMrpWithApprovedPendingInternalRequirementsForComponents() throws GeneralException {
        // create a manufactured product
        final GenericValue product = createTestProduct("test Mrp With Approved Pending Internal Requirements For Components Product", demopurch1);
        final String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);

        // create its component
        final GenericValue productComp1 = createTestProduct("test Mrp With Approved Pending Internal Requirements For Components Product - Component 1", demopurch1);
        final String productComp1Id = productComp1.getString("productId");
        final GenericValue productComp2 = createTestProduct("test Mrp With Approved Pending Internal Requirements For Components Product - Component 2", demopurch1);
        final String productComp2Id = productComp2.getString("productId");
        createBOMProductAssoc(productId, productComp1Id, new Long("10"), new BigDecimal("7.0"), admin);
        createBOMProductAssoc(productId, productComp2Id, new Long("10"), new BigDecimal("3.0"), admin);
        // add supplier for the components, so the Mrp will generate PRODUCT_REQUIREMENT
        assignDefaultPrice(productComp1, new BigDecimal("7.5"), admin);
        createMainSupplierForProduct(productComp1Id, "DemoSupplier", new BigDecimal("7.0"), "USD", new BigDecimal("1.0"), admin);
        assignDefaultPrice(productComp2, new BigDecimal("3.5"), admin);
        createMainSupplierForProduct(productComp2Id, "DemoSupplier", new BigDecimal("3.0"), "USD", new BigDecimal("1.0"), admin);

        // creates an Assembly routing with one task for product
        createTestAssemblingRouting("test Mrp With Pending Internal Requirements", productId);

        // set a facility minimum stock for it and its components so MRP will generate requirements
        createProductFacility(productId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);
        createProductFacility(productComp1Id, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);
        createProductFacility(productComp2Id, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);

        // create a sales order for 10
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("10.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // run the MRP
        Map<String, Object> runMrpContext = UtilMisc.<String, Object>toMap("userLogin", demopurch1, "facilityId", facilityId, "defaultYearsOffset", new Integer(1), "percentageOfSalesForecast", new BigDecimal("0.0"), "createPendingManufacturingRequirements", true);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // verify the proposed pending internal requirements
        assertRequirementsTotalQuantityCorrect(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("10.0"), null, null);
        // check for requirements individually note that first two order creates a 11 requirement
        String req1Id = assertRequirementExistsWithQuantity(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("10.0"));
        assertRequirementExistsWithQuantity(productComp1Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("70.0"));
        assertRequirementExistsWithQuantity(productComp2Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("30.0"));

        // approve the requirement for the manufactured product
        runAndAssertServiceSuccess("approveRequirement", UtilMisc.toMap("userLogin", demopurch1, "requirementId", req1Id));

        // run the MRP again and check the component requirements are still generated
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);
        assertRequirementsTotalQuantityCorrect(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_APPROVED", new BigDecimal("10.0"), null, null);
        assertNoRequirementExistsWithQuantity(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("10.0"));
        assertRequirementsTotalQuantityCorrect(productComp1Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("70.0"), null, null);
        assertRequirementsTotalQuantityCorrect(productComp2Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("30.0"), null, null);
        assertRequirementExistsWithQuantity(productComp1Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("70.0"));
        assertRequirementExistsWithQuantity(productComp2Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("30.0"));

        // modify the Pending Internal Requirement's quantity to 100
        runAndAssertServiceSuccess("updateRequirement", UtilMisc.<String, Object>toMap("userLogin", demopurch1, "requirementId", req1Id, "quantity", new BigDecimal("100.0")));

        // run the MRP again and check the component requirements are still generated according to the new quantity
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);
        assertRequirementsTotalQuantityCorrect(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_APPROVED", new BigDecimal("100.0"), null, null);
        assertNoRequirementExistsWithQuantity(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("10.0"));
        assertRequirementsTotalQuantityCorrect(productComp1Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("700.0"), null, null);
        assertRequirementsTotalQuantityCorrect(productComp2Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("300.0"), null, null);
        String req2Id = assertRequirementExistsWithQuantity(productComp1Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("700.0"));
        assertRequirementExistsWithQuantity(productComp2Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("300.0"));

        // approve one of the component requirements
        runAndAssertServiceSuccess("approveRequirement", UtilMisc.toMap("userLogin", demopurch1, "requirementId", req2Id));

        // run the MRP again and check the component requirements are still generated according to the new quantity
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);
        assertRequirementsTotalQuantityCorrect(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_APPROVED", new BigDecimal("100.0"), null, null);
        assertNoRequirementExistsWithQuantity(productId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("10.0"));
        assertRequirementsTotalQuantityCorrect(productComp1Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_APPROVED", new BigDecimal("700.0"), null, null);
        assertRequirementsTotalQuantityCorrect(productComp2Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("300.0"), null, null);
        assertRequirementExistsWithQuantity(productComp1Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_APPROVED", new BigDecimal("700.0"));
        assertRequirementExistsWithQuantity(productComp2Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("300.0"));

    }

    /**
     *  This test verifies that MRP will create requirements based on the default BOM when a product has alternate BOMs.
     * @throws GeneralException if an error occurs
     */
    public void testMrpForProductWithMultipleBOM() throws GeneralException {
        // create a test product with a default BOM and an alternate BOM with a specific routing ID
        GenericValue product = createMrpProduct("mrp finished good for testMrpForProductWithMultipleBOM", "FINISHED_GOOD", new Long(0), facilityId, new BigDecimal(0.0), new BigDecimal(5.0), new Long(1), admin);
        String productId = (String) product.get("productId");

        // create raw material test product #1 (mat1) with [minimumStock : 0.0; reorderQuantity : 15; daysToShip: 2]
        GenericValue mat1 = createMrpProduct("mrp test raw material #1 for testMrpForProductWithMultipleBOM", "RAW_MATERIAL", new Long(1), facilityId, new BigDecimal(0.0), new BigDecimal(15.0), new Long(2), admin);
        String mat1Id = (String) mat1.get("productId");
        // create raw material test product #2 (mat2) with [minimumStock : 0.0; reorderQuantity : 7; daysToShip: 3]
        GenericValue mat2 = createMrpProduct("mrp test raw material #2 for testMrpForProductWithMultipleBOM", "RAW_MATERIAL", new Long(1), facilityId, new BigDecimal(0.0), new BigDecimal(7.0), new Long(3), admin);
        String mat2Id = (String) mat2.get("productId");
        // create a ProductAssoc entity between finishedGoodTestProduct and mat1 with [productAssocTypeId : 'MANUF_COMPONENT'; quantity : 2.0]
        createBOMProductAssoc(productId, mat1Id, null, new Long(10), new BigDecimal("2.0"), admin);
        // create a ProductAssoc entity between finishedGoodTestProduct and mat2 with [productAssocTypeId : 'MANUF_COMPONENT'; quantity : 3.0]
        createBOMProductAssoc(productId, mat2Id, null, new Long(20), new BigDecimal("3.0"), admin);

        // create raw material test product #3 (mat3) with [minimumStock : 0.0; reorderQuantity : 2; daysToShip: 2]
        GenericValue mat3 = createMrpProduct("mrp test raw material #3 for testMrpForProductWithMultipleBOM", "RAW_MATERIAL", new Long(1), facilityId, new BigDecimal(0.0), new BigDecimal(2.0), new Long(2), admin);
        String mat3Id = (String) mat3.get("productId");
        // create raw material test product #4 (mat4) with [minimumStock : 0.0; reorderQuantity : 5; daysToShip: 1]
        GenericValue mat4 = createMrpProduct("mrp test raw material #4 for testMrpForProductWithMultipleBOM", "RAW_MATERIAL", new Long(1), facilityId, new BigDecimal(0.0), new BigDecimal(5.0), new Long(1), admin);
        String mat4Id = (String) mat4.get("productId");
        // create a ProductAssoc entity between finishedGoodTestProduct and mat3 with [productAssocTypeId : 'MANUF_COMPONENT'; quantity : 5.0]
        createBOMProductAssoc(productId, mat3Id, "ROUT01", new Long(10), new BigDecimal("5.0"), admin);
        // create a ProductAssoc entity between finishedGoodTestProduct and mat4 with [productAssocTypeId : 'MANUF_COMPONENT'; quantity : 2.0]
        createBOMProductAssoc(productId, mat4Id, "ROUT01", new Long(20), new BigDecimal("2.0"), admin);

        Debug.logInfo("productId : " + productId + ", mat1Id : " + mat1Id  + ", mat2Id : " + mat2Id  + ", mat3Id : " + mat3Id  + ", mat4Id : " + mat4Id, MODULE);
        // create a product routing definition for test purposes
        createTestAssemblingRouting("test Mrp Manufactured Product with BOM", productId);

        // create a sales order for 10 of the test product
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("10.0"));
        User = DemoSalesManager;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, productStoreId);
        String orderId = salesOrder.getOrderId();
        Debug.logInfo("testMrpForProductWithMultipleBOM created order [" + salesOrder.getOrderId() + "]", MODULE);

        // approve the sales order
        salesOrder.approveOrder();

        // run MRP
        runAndAssertServiceSuccess("opentaps.runMrp", UtilMisc.toMap("userLogin", demopurch1, "facilityId", facilityId));

        // verify that
        // 1.  manufacturing requirements created for the test product
        // 2.  Purchasing requirements created for the components of the default BOM
        // 3.  No requirements created for components of the alternate BOM
        String requirementId = assertRequirementExists(productId, facilityId, "INTERNAL_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("10.0"));
        assertRequirementAssignedToOrder(orderId, requirementId, new BigDecimal("10.0"));

        assertRequirementExists(mat1Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("20.0"));
        assertRequirementExists(mat2Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("30.0"));
        assertNoRequirementExistsWithQuantity(mat3Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("50.0"));
        assertNoRequirementExistsWithQuantity(mat4Id, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("20.0"));
    }

    /**
     * This test verify that if the quantity required is below the minimum quantity of available routings, then
     * requirements are created with the minimum quantity and then used to create requirements for the BOM parts.
     *
     * @throws GeneralException if an error occurs
     */
    public void testMrpQuantityBelowRoutingMinimumQuantity() throws GeneralException {
        InventoryAsserts webStoreWarehouseInvAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demopurch1);

        // 1. create test mfrProduct with minimumStock 0
        GenericValue mfrProduct = createMrpProduct("Manufactured Product for MrpNoRoutingForQuantity tests", "FINISHED_GOOD", 0L, facilityId, new BigDecimal("0.0"), new BigDecimal("1.0"), 1L, admin);
        String mftProductId = mfrProduct.getString("productId");
        webStoreWarehouseInvAsserts.getInventory(mftProductId);

        // 2. create raw material test rawMaterial1 with minimumStock 0
        GenericValue rawMaterial1 = createMrpProduct("Raw material 1 for product [" + mftProductId + "]", "RAW_MATERIAL", 1L, facilityId, new BigDecimal("0.0"), new BigDecimal("1.0"), 1L, admin);
        String rawMaterialId1 = rawMaterial1.getString("productId");
        webStoreWarehouseInvAsserts.getInventory(rawMaterialId1);

        // 3. create raw material test rawMaterial2 with minimumStock 0
        GenericValue rawMaterial2 = createMrpProduct("Raw material 2 for product [" + mftProductId + "]", "RAW_MATERIAL", 1L, facilityId, new BigDecimal("0.0"), new BigDecimal("1.0"), 1L, admin);
        String rawMaterialId2 = rawMaterial2.getString("productId");
        webStoreWarehouseInvAsserts.getInventory(rawMaterialId2);

        // 4. associate raw materials and finished product
        createBOMProductAssoc(mftProductId, rawMaterialId1, 1L, new BigDecimal("2.0"), admin);
        createBOMProductAssoc(mftProductId, rawMaterialId2, 2L, new BigDecimal("1.0"), admin);

        // 5. create a WorkEffortGoodStandard entity for mfrProduct, minQuantity 10.0 & maxQuantity 15.0
        createTestAssemblingRouting("Assembling manufactured product for MrpNoRoutingForQuantity tests", mftProductId, new BigDecimal("300000.0"), new BigDecimal("600000.0"), new BigDecimal("10.0") /*min qty*/, new BigDecimal("15.0")/*max qty*/);

        // 6. create sales order of 7x mfrProduct
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(mfrProduct, new BigDecimal("7.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // 7. run the MRP for WebStoreWarehouse and product with flag to create pending internal requirements set to Y
        Map<String, Object> runMrpContext =
            UtilMisc.<String, Object>toMap(
                    "userLogin", demopurch1,
                    "facilityId", facilityId,
                    "createPendingManufacturingRequirements", true
            );
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 8. verify there are requirements for mfrProduct, rawMaterial1 & rawMaterial2
        // mfrProduct pending internal requirement is at the minimum quantity of 10
        assertRequirementExists(mftProductId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED", new BigDecimal("10.0"));
        // rawMaterial1 product requirement is 20
        assertRequirementExists(rawMaterialId1, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("20.0"));
        // rawMaterial2 product requirement is 10
        assertRequirementExists(rawMaterialId2, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("10.0"));
    }

    /**
     * This test verifies that when the quantity needed is above the maximum routing quantity, MRP will create
     * several requirements and then use them to create requirements for parts.
     * @exception GeneralException if an error occurs
     */
    public void testMrpQuantityAboveMaximumQuantity() throws GeneralException {
        InventoryAsserts webStoreWarehouseInvAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demopurch1);

        // create a test product with minimum stock 0
        GenericValue testProduct = createMrpProduct("Manufactured Product for testMrpQuantityAboveMaximumQuantity tests", "FINISHED_GOOD", 0L, facilityId, new BigDecimal("0.0"), new BigDecimal("1.0"), 1L, admin);
        String testProductId = testProduct.getString("productId");
        webStoreWarehouseInvAsserts.getInventory(testProductId);

        // create test part 1
        GenericValue testPart1 = createMrpProduct("Raw material for product [" + testProductId + "]", "RAW_MATERIAL", 1L, facilityId, new BigDecimal("0.0"), new BigDecimal("1.0"), 1L, admin);
        String testPartId1 = testPart1.getString("productId");
        webStoreWarehouseInvAsserts.getInventory(testPartId1);

        // create BOM so that 10 of testpart1 is required to manufacture a test product
        createBOMProductAssoc(testProductId, testPartId1, 1L, new BigDecimal("10.0"), admin);

        // create a routing for test product with minimum quantity 10 and max quantity 100
        createTestAssemblingRouting("Assembling manufactured product for testMrpQuantityAboveMaximumQuantity tests, low range", testProductId, new BigDecimal("300000.0"), new BigDecimal("600000.0"), new BigDecimal("10.0") /*min qty*/, new BigDecimal("100.0")/*max qty*/);

        // create a routing for test product with minimum quantity 101 and max quantity 500
        createTestAssemblingRouting("Assembling manufactured product for MrpNoRoutingForQuantity tests, high range.", testProductId, new BigDecimal("300000.0"), new BigDecimal("600000.0"), new BigDecimal("101.0") /*min qty*/, new BigDecimal("500.0")/*max qty*/);

        // create a sales order for 705 test product
        // this has a subtle test to make sure that the last requirement is created at the minimum quantity of 10, not the remnant quantity of 5
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(testProduct, new BigDecimal("705.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // run MRP with flag to create pending internal requirements set to Y
        Map<String, Object> runMrpContext = FastMap.newInstance();
        runMrpContext.put("userLogin", demopurch1);
        runMrpContext.put("facilityId", facilityId);
        runMrpContext.put("createPendingManufacturingRequirements", Boolean.TRUE);
        Debug.logInfo("before run opentaps.runMrp", MODULE);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // verify these requirements exist:
        //  1 Pending Internal Requirement for testproduct with quantity 500
        //  2 Pending Internal Requirement for testproduct with quantity 100 each
        //  1 Pending Internal Requirement for testproduct with quantity 10
        assertRequirementExists(testProductId, facilityId, "PENDING_INTERNAL_REQ", "REQ_PROPOSED",
                Arrays.asList(new BigDecimal("500.0"), new BigDecimal("100.0"), new BigDecimal("100.0"), new BigDecimal("10.0")));

        //  1 Product Requirement for testpart1 with quantity 7100
        assertPurchasingRequirementExists(testPartId1, facilityId, new BigDecimal("7100.0"));
    }

    /**
     * This test verify that MRP selects right routing and BOM among a few options based on product quantity.
     *
     * @throws GeneralException if an error occurs
     */
    public void testMrpSelectRoutingForQuantity() throws GeneralException {
        InventoryAsserts webStoreWarehouseInvAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demopurch1);

        // 1. create test mftProduct with minimumStock 1.0
        GenericValue mftProduct = createMrpProduct("Manufactured Product for MrpSelectRoutingForQuantity tests", "FINISHED_GOOD", 0L, facilityId, new BigDecimal("0.0"), new BigDecimal("1.0"), 7L, admin);
        String mftProductId = mftProduct.getString("productId");
        webStoreWarehouseInvAsserts.getInventory(mftProductId);

        // 2. create raw material test rawMaterial1 with minimumStock 1.0
        GenericValue rawMaterial1 = createMrpProduct("Raw material 1 for product [" + mftProductId + "]", "RAW_MATERIAL", 1L, facilityId, new BigDecimal("0.0"), new BigDecimal("1.0"), 1L, admin);
        String rawMaterialId1 = rawMaterial1.getString("productId");
        webStoreWarehouseInvAsserts.getInventory(rawMaterialId1);

        // 3. create raw material test rawMaterial2 with minimumStock 1.0
        GenericValue rawMaterial2 = createMrpProduct("Raw material 2 for product [" + mftProductId + "]", "RAW_MATERIAL", 1L, facilityId, new BigDecimal("0.0"), new BigDecimal("1.0"), 1L, admin);
        String rawMaterialId2 = rawMaterial2.getString("productId");
        webStoreWarehouseInvAsserts.getInventory(rawMaterialId2);

        // 4. create BOM that imply manufacturing 1 mftProduct from 2 rawMaterial2
        createBOMProductAssoc(mftProductId, rawMaterialId1, 1L, new BigDecimal("2.0"), admin);

        // 5. create a WorkEffortGoodStandard entity for mftProduct, minQuantity 10.0 & maxQuantity 15.0
        createTestAssemblingRouting("Assembling manufactured product for MrpNoRoutingForQuantity tests", mftProductId, new BigDecimal("300000.0"), new BigDecimal("600000.0"), new BigDecimal("10.0") /*min qty*/, new BigDecimal("15.0")/*max qty*/);

        // 6. create a WorkEffortGoodStandard entity for mftProduct, minQuantity 15.0 & maxQuantity 20.0
        String alternateRoutingId = createTestAssemblingRouting("Assembling manufactured product for MrpNoRoutingForQuantity tests", mftProductId, new BigDecimal("300000.0"), new BigDecimal("600000.0"), new BigDecimal("15.0") /*min qty*/, new BigDecimal("20.0")/*max qty*/);

        // 7. create BOM that imply manufacturing 1 mftProduct from 3 rawMaterial2
        createBOMProductAssoc(mftProductId, rawMaterialId2, alternateRoutingId, 1L, new BigDecimal("3.0"), admin);

        // 8. create sales order of 17x mftProduct
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(mftProduct, new BigDecimal("17.0"));
        User = DemoSalesManager;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(order, DemoCustomer, productStoreId);
        String orderId = salesOrder.getOrderId();

        // 9. run the MRP for WebStoreWarehouse
        Map<String, Object> runMrpContext =
            UtilMisc.toMap(
                    "userLogin", demopurch1,
                    "facilityId", facilityId
            );
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 10. verify purchasing requirements count, mftProduct 17, rawMaterial2 51, no requirements for rawMaterial1
        String requirementId1 = assertRequirementExists(mftProductId, facilityId, "INTERNAL_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("17.0"));
        assertRequirementAssignedToOrder(orderId, requirementId1, new BigDecimal("17.0"));
        assertRequirementExists(rawMaterialId2, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("51.0"));
        assertNoRequirementExists(rawMaterialId1, facilityId, "PRODUCT_REQUIREMENT", "REQ_PROPOSED");
    }

    /**
     * Verify when MRP use routing with minimal quantity for product, resulting requirements count
     * equals to minimal quantity and don't take into account Facility.minimalStock.
     *
     * @throws GeneralException if an error occurs
     */
    public void testMrpApplyMinimalQuantityIrrespectiveOfMinimumStock() throws GeneralException {
        InventoryAsserts webStoreWarehouseInvAsserts = new InventoryAsserts(this, facilityId, organizationPartyId, demopurch1);

        // 1. create test mftProduct with minimumStock 7.0
        GenericValue mftProduct = createMrpProduct(
                "Manufactured Product for testMrpApplyMinimalQuantityIrrespectiveOfMinimumStock tests",
                "FINISHED_GOOD", 0L, facilityId, new BigDecimal("7.0") /*minimalStock*/, new BigDecimal("1.0"), 7L, admin);
        String mftProductId = mftProduct.getString("productId");
        webStoreWarehouseInvAsserts.getInventory(mftProductId);

        // 2. create raw material test rawMaterial
        GenericValue rawMaterial = createMrpProduct(
                "Raw material for product [" + mftProductId + "]",
                "RAW_MATERIAL", 1L, facilityId, new BigDecimal("0.0"), new BigDecimal("1.0"), 1L, admin);
        String rawMaterialId = rawMaterial.getString("productId");
        webStoreWarehouseInvAsserts.getInventory(rawMaterialId);

        // 3. receive some of mftProduct
        Map<String, Object> ctxt = FastMap.newInstance();
        ctxt.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        ctxt.put("productId", mftProductId);
        ctxt.put("facilityId", facilityId);
        ctxt.put("unitCost", new BigDecimal("12.0"));
        ctxt.put("currencyUomId", "USD");
        ctxt.put("datetimeReceived", UtilDateTime.nowTimestamp());
        ctxt.put("quantityAccepted", new BigDecimal("9.0"));
        ctxt.put("quantityRejected", BigDecimal.ZERO);
        ctxt.put("userLogin", demowarehouse1);
        runAndAssertServiceSuccess("receiveInventoryProduct", ctxt);

        // 4. create BOM that imply manufacturing 1 mftProduct from 1 rawMaterial
        createBOMProductAssoc(mftProductId, rawMaterialId, 1L, new BigDecimal("1.0"), admin);

        // 5. create a WorkEffortGoodStandard entity for mftProduct, minQuantity 100.0
        createTestAssemblingRouting(
                "Assembling manufactured product for testMrpApplyMinimalQuantityIrrespectiveOfMinimumStock tests",
                mftProductId, new BigDecimal("300000.0"), new BigDecimal("600000.0"), new BigDecimal("100.0") /*min qty*/, new BigDecimal("150.0")/*max qty*/);

        // 6. prepare Manufacturing Order Receipt for 10 mftProduct
        Map<GenericValue, BigDecimal> order = FastMap.newInstance();
        order.put(mftProduct, new BigDecimal("5.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        Map<String, Object> runMrpContext =
            UtilMisc.toMap(
                    "userLogin", demopurch1,
                    "facilityId", facilityId
            );
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        Map<String, Object> conditions = UtilMisc.<String, Object>toMap(
                "requirementTypeId", "INTERNAL_REQUIREMENT",
                "productId", mftProductId,
                "facilityId", facilityId,
                "statusId", "REQ_PROPOSED"
        );
        List<GenericValue> requirements = delegator.findByAnd("Requirement", conditions);
        String requirementId = EntityUtil.getFirst(requirements).getString("requirementId");
        runAndAssertServiceSuccess("purchasing.updateRequirementSupplierAndQuantity", UtilMisc.toMap("requirementId", requirementId, "quantity", BigDecimal.valueOf(5.0), "userLogin", demopurch1));
        // this creates a production run and requires permission on the WebStoreWarehouse
        runAndAssertServiceSuccess("approveRequirement", UtilMisc.toMap("requirementId", requirementId, "userLogin", demowarehouse1));

        // 7. create outgoing transfer requirement
        Map<String, Object> requirement =
            UtilMisc.<String, Object>toMap(
                    "requirementId", delegator.getNextSeqId("Requirement"),
                    "requirementTypeId", "TRANSFER_REQUIREMENT",
                    "facilityId", facilityId,
                    "facilityIdTo", thirdPartyFacilityId,
                    "productId", mftProductId,
                    "statusId", "REQ_APPROVED"
            );
        requirement.put("description", "Manually created for testMrpApplyMinimalQuantityIrrespectiveOfMinimumStock");
        Timestamp reqDate = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 1, timeZone, locale);
        requirement.put("requirementStartDate", reqDate);
        requirement.put("requiredByDate", reqDate);
        requirement.put("quantity", new BigDecimal("10.0"));
        delegator.create("Requirement", requirement);

        // 8. create sales forecast
        createSalesForecastItem(mftProductId, facilityId, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DAY_OF_YEAR, 1, timeZone, locale), BigDecimal.valueOf(100.0));

        // 9. run the MRP for WebStoreWarehouse
        runMrpContext =
            UtilMisc.toMap(
                    "userLogin", demopurch1,
                    "facilityId", facilityId,
                    "percentageOfSalesForecast", new BigDecimal("35.00"),
                    "createTransferRequirements", Boolean.TRUE
            );
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);

        // 10. verify internal requirements count, should be one requirement for 100 mftProduct
        assertRequirementExists(mftProductId, facilityId, "INTERNAL_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("100.0"));
    }

    /**
     * This test verify that MRP works with production run after updating their quantity and partially producing them.
     * @throws GeneralException if an error occurs
     */
    public void testMrpWithUpdatedProductionRuns() throws GeneralException {
        // create a manufactured test product
        final GenericValue product = createTestProduct("test Mrp With Updated Production Run", demopurch1);
        final String productId = product.getString("productId");
        assignDefaultPrice(product, new BigDecimal("10.0"), admin);

        // create its component
        final GenericValue productComp1 = createTestProduct("test Mrp With Updated Production Run - Component 1", demopurch1);
        final String productComp1Id = productComp1.getString("productId");
        final GenericValue productComp2 = createTestProduct("test Mrp With Updated Production Run - Component 2", demopurch1);
        final String productComp2Id = productComp2.getString("productId");
        createBOMProductAssoc(productId, productComp1Id, new Long("10"), new BigDecimal("7.0"), admin);
        createBOMProductAssoc(productId, productComp2Id, new Long("10"), new BigDecimal("3.0"), admin);
        // add supplier for the components, so the Mrp will generate PRODUCT_REQUIREMENT
        assignDefaultPrice(productComp1, new BigDecimal("7.5"), admin);
        createMainSupplierForProduct(productComp1Id, "DemoSupplier", new BigDecimal("7.0"), "USD", new BigDecimal("1.0"), admin);
        assignDefaultPrice(productComp2, new BigDecimal("3.5"), admin);
        createMainSupplierForProduct(productComp2Id, "DemoSupplier", new BigDecimal("3.0"), "USD", new BigDecimal("1.0"), admin);

        // creates an Assembly routing with one task for product
        createTestAssemblingRouting("test Mrp With Pending Internal Requirements", productId);

        // set a facility minimum stock for it and its components so MRP will generate requirements
        createProductFacility(productId, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);
        createProductFacility(productComp1Id, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);
        createProductFacility(productComp2Id, facilityId, new BigDecimal(0.0), new BigDecimal(1.0), admin);

        // create a sales order of 10
        Map<GenericValue, BigDecimal> order = new HashMap<GenericValue, BigDecimal>();
        order.put(product, new BigDecimal("10.0"));
        User = DemoSalesManager;
        testCreatesSalesOrder(order, DemoCustomer, productStoreId);

        // run MRP, this should create a proposed Production run of 10
        Map<String, Object> runMrpContext = UtilMisc.<String, Object>toMap("userLogin", demopurch1, "facilityId", facilityId, "defaultYearsOffset", new Integer(1), "percentageOfSalesForecast", new BigDecimal("0.0"), "createPendingManufacturingRequirements", false);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);
        String req1Id = assertRequirementExistsWithQuantity(productId, facilityId, "INTERNAL_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("10.0"));
        // this creates a production run and requires permission on the WebStoreWarehouse
        runAndAssertServiceSuccess("approveRequirement", UtilMisc.toMap("userLogin", demowarehouse1, "requirementId", req1Id));

        // update the production run to produce 6
        GenericValue prun = EntityUtil.getOnly(delegator.findByAnd("WorkEffortAndGoods", UtilMisc.toMap("productId", productId, "currentStatusId", "PRUN_CREATED")));
        String workEffortId = prun.getString("workEffortId");
        Debug.logInfo("before run updateProductionRun", MODULE);
        runAndAssertServiceSuccess("updateProductionRun", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "productionRunId", workEffortId, "quantity", new BigDecimal("6.0"), "estimatedStartDate", prun.getTimestamp("estimatedStartDate")));
        // get the updated production run
        prun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        // check the quantity to produce was updated
        assertEquals("Production run [" + workEffortId + "] quantity to produce was not updated", new BigDecimal("6.0"), prun.getBigDecimal("quantityToProduce"));
        // start the production run (because the MRP Wegs are 1 year in the future we need to start it to set the start date right)
        runAndAssertServiceSuccess("changeProductionRunStatus", UtilMisc.toMap("userLogin", demowarehouse1, "productionRunId", workEffortId, "statusId", "PRUN_DOC_PRINTED"));
        // start the previous production run, and produce 2
        List<GenericValue> tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", workEffortId, "workEffortTypeId", "PROD_ORDER_TASK"));
        assertNotEmpty("Production run [" + workEffortId + "] has no routing tasks.  Cannot finish test.", tasks);
        assertEquals("Template for [" + workEffortId + "] has more than one task.  It should only have one task defined.", tasks.size(), 1);
        GenericValue task = EntityUtil.getFirst(tasks);
        String taskId = task.getString("workEffortId");
        // start the task
        runAndAssertServiceSuccess("changeProductionRunTaskStatus", UtilMisc.toMap("userLogin", demowarehouse1, "productionRunId", workEffortId, "workEffortId", taskId));

        // run MRP, and check:
        runMrpContext = UtilMisc.<String, Object>toMap("userLogin", demopurch1, "facilityId", facilityId, "defaultYearsOffset", new Integer(1), "percentageOfSalesForecast", new BigDecimal("0.0"), "createPendingManufacturingRequirements", false);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);
        // - it proposes a  Production run of 4
        assertRequirementExistsWithQuantity(productId, facilityId, "INTERNAL_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("4.0"));
        // - there is a receipt of 6 in the inventory event from the previous production run
        GenericValue event = EntityUtil.getOnly(delegator.findByAnd("MrpInventoryEvent", UtilMisc.<String, Object>toMap("productId", productId, "inventoryEventPlanTypeId", "MANUF_ORDER_RECP", "eventQuantity", new BigDecimal("6.0"))));
        assertNotNull("No MANUF_ORDER_RECP x 6.0 found for product [" + productId + "].", event);
        // issue inventory required, and produce 2
        runAndAssertServiceSuccess("issueProductionRunTask", UtilMisc.toMap("userLogin", demowarehouse1, "workEffortId", taskId));
        // produce 2 unit of product
        runAndAssertServiceSuccess("opentaps.productionRunProduce", UtilMisc.<String, Object>toMap("userLogin", demowarehouse1, "workEffortId", workEffortId, "productId", productId, "quantity", new BigDecimal("2.0")));

        // run MRP, and check:
        runMrpContext = UtilMisc.<String, Object>toMap("userLogin", demopurch1, "facilityId", facilityId, "defaultYearsOffset", new Integer(1), "percentageOfSalesForecast", new BigDecimal("0.0"), "createPendingManufacturingRequirements", false);
        runAndAssertServiceSuccess("opentaps.runMrp", runMrpContext);
        // - it proposes a  Production run of 4
        assertRequirementExistsWithQuantity(productId, facilityId, "INTERNAL_REQUIREMENT", "REQ_PROPOSED", new BigDecimal("4.0"));
        // - there is an INITIAL_QOH inventory event of 2
        event = EntityUtil.getOnly(delegator.findByAnd("MrpInventoryEvent", UtilMisc.<String, Object>toMap("productId", productId, "inventoryEventPlanTypeId", "INITIAL_QOH", "eventQuantity", new BigDecimal("2.0"))));
        assertNotNull("No INITIAL_QOH x 2.0 found for product [" + productId + "].", event);
        // - there is a receipt of 4 in the inventory event from the previous production run
        event = EntityUtil.getOnly(delegator.findByAnd("MrpInventoryEvent", UtilMisc.<String, Object>toMap("productId", productId, "inventoryEventPlanTypeId", "MANUF_ORDER_RECP", "eventQuantity", new BigDecimal("4.0"))));
        assertNotNull("No MANUF_ORDER_RECP x 4.0 found for product [" + productId + "].", event);
    }

}
