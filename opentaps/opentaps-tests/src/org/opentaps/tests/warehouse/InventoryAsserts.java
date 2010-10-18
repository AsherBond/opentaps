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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opensourcestrategies.financials.util.UtilCOGS;
import junit.framework.TestCase;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Delegation object that holds assert() methods and util methods for
 * dealing with inventory.  If your tests need to create inventory,
 * count inventory, and so on, create an instance of this class and
 * invoke the assert() or util methods as needed.
 *
 * The reason for this pattern is that the assert() and util methods
 * have asserts of their own and must extend OpentapsTestCase and
 * be able to interact with the test suite.
 */
public class InventoryAsserts extends OpentapsTestCase {

    private static final String MODULE = InventoryAsserts.class.getName();

    private String facilityId;
    private String organizationPartyId;
    private GenericValue userLogin;

    /**
     * Creates a new <code>InventoryAsserts</code> instance.
     *
     * @param parent an <code>OpentapsTestCase</code> value
     * @param facilityId the facility used
     * @param organizationPartyId the organization used
     * @param userLogin the user to use (for calling services...)
     * @exception GenericEntityException if an error occurs
     */
    public InventoryAsserts(OpentapsTestCase parent, String facilityId, String organizationPartyId, GenericValue userLogin) throws GenericEntityException {
        this.delegator = parent.getDelegator();
        this.dispatcher = parent.getDispatcher();
        this.facilityId = facilityId;
        this.organizationPartyId = organizationPartyId;
        this.userLogin = userLogin;
    }

    /**
     * Asserts the inventory QoH/ATP has changed by the given amount.  The map of original inventory comes from getInventory().
     * @param productId the product ID to test for
     * @param qohChange the expected QOH change
     * @param atpChange the expected QTP change
     * @param originalInventories the <code>Map</code> containing a result from <code>getInventories</code> used as a point of comparison
     */
    public void assertInventoriesChange(String productId, BigDecimal qohChange, BigDecimal atpChange, Map<String, Map<String, Object>> originalInventories) {
        Map<String, Object> currentInventory = getInventory(productId);
        Map<String, BigDecimal> expectedDifference = UtilMisc.toMap("quantityOnHandTotal", qohChange, "availableToPromiseTotal", atpChange);
        assertNotNull("No reference inventory for product [" + productId + "]", originalInventories.get(productId));
        assertMapDifferenceCorrect("Inventory change for product [" + productId + "].", originalInventories.get(productId), currentInventory, expectedDifference);
    }

    /**
     * Asserts the inventory QoH/ATP has changed by the given amount.  The map of original inventory comes from getInventory().
     * @param productIds the <code>List</code> of product ID to test for
     * @param qohChanges the <code>List</code> of expected QOH change
     * @param atpChanges the <code>List</code> of expected QTP change
     * @param originalInventories the <code>Map</code> containing a result from <code>getInventories</code> used as a point of comparison
     */
    public void assertInventoriesChange(List<String> productIds, List<BigDecimal> qohChanges, List<BigDecimal> atpChanges, Map<String, Map<String, Object>> originalInventories) {
        for (int i = 0; i < productIds.size(); i++) {
            String productId = productIds.get(i);
            BigDecimal qohChange = qohChanges.get(i);
            BigDecimal atpChange = atpChanges.get(i);
            assertInventoriesChange(productId, qohChange, atpChange, originalInventories);
        }
    }

    /**
     * Asserts the inventory QoH / ATP has changed by the same given amount.  The map of original inventory comes from getInventories().
     * @param productIds the <code>List</code> of product ID to test for
     * @param quantityChanges the <code>List</code> of expected QOH and ATP change
     * @param originalInventories the <code>Map</code> containing a result from <code>getInventories</code> used as a point of comparison
     */
    public void assertInventoriesChange(List<String> productIds, List<BigDecimal> quantityChanges, Map<String, Map<String, Object>> originalInventories) {
        for (int i = 0; i < productIds.size(); i++) {
            String productId = productIds.get(i);
            BigDecimal quantityChange = quantityChanges.get(i);
            assertInventoriesChange(productId, quantityChange, originalInventories);
        }
    }

    /**
     * Asserts the inventory QoH / ATP has changed by the same given amount.  The map of original inventory comes from getInventories().
     * @param productIds the <code>List</code> of product ID to test for
     * @param quantityChange the expected QOH and ATP change, which is the same for all given products
     * @param originalInventories the <code>Map</code> containing a result from <code>getInventories</code> used as a point of comparison
     */
    public void assertInventoriesChange(List<String> productIds, BigDecimal quantityChange, Map<String, Map<String, Object>> originalInventories) {
        for (String productId : productIds) {
            assertInventoriesChange(productId, quantityChange, originalInventories);
        }
    }

    /**
     * Asserts the inventory QoH has changed by the given amount.  The map of original inventory comes from getInventory().
     * @param productId the product ID to test for
     * @param quantityChange the expected QOH and ATP change
     * @param originalInventories the <code>Map</code> containing a result from <code>getInventories</code> used as a point of comparison
     */
    public void assertInventoriesChange(String productId, BigDecimal quantityChange, Map<String, Map<String, Object>> originalInventories) {
        Map<String, Object> currentInventory = getInventory(productId);
        Map<String, BigDecimal> expectedDifference = UtilMisc.toMap("quantityOnHandTotal", quantityChange, "availableToPromiseTotal", quantityChange);
        assertNotNull("No reference inventory for product [" + productId + "]", originalInventories.get(productId));
        assertMapDifferenceCorrect("Inventory change for product [" + productId + "].", originalInventories.get(productId), currentInventory, expectedDifference);
    }

    /**
     * Asserts the inventory QoH/ATP has changed by the given amount.  The map of original inventory comes from getInventory().
     * @param productId the product ID to test for
     * @param qohChange the expected QOH change
     * @param atpChange the expected QTP change
     * @param originalInventory the <code>Map</code> containing a result from <code>getInventory</code> used as a point of comparison
     */
    public void assertInventoryChange(String productId, BigDecimal qohChange, BigDecimal atpChange, Map<String, Object> originalInventory) {
        Map<String, Object> currentInventory = getInventory(productId);
        Map<String, BigDecimal> expectedDifference = UtilMisc.toMap("quantityOnHandTotal", qohChange, "availableToPromiseTotal", atpChange);
        assertMapDifferenceCorrect("Inventory change for product [" + productId + "].", originalInventory, currentInventory, expectedDifference);
    }

    /**
     * Asserts the inventory QoH has changed by the given amount.  The map of original inventory comes from getInventory().
     * @param productId the product ID to test for
     * @param quantityChange the expected QOH and ATP change
     * @param originalInventory the <code>Map</code> containing a result from <code>getInventory</code> used as a point of comparison
     */
    public void assertInventoryChange(String productId, BigDecimal quantityChange, Map<String, Object> originalInventory) {
        Map<String, Object> currentInventory = getInventory(productId);
        Map<String, BigDecimal> expectedDifference = UtilMisc.toMap("quantityOnHandTotal", quantityChange, "availableToPromiseTotal", quantityChange);
        assertMapDifferenceCorrect("Inventory change for product [" + productId + "].", originalInventory, currentInventory, expectedDifference);
    }

    /**
     * Asserts that the inventory value of a product calculated from the ledger is equal to the value calculated from the inventory item unit cost data.
     * @param productId the product ID to test for
     */
    public void assertInventoryValuesEqual(String productId) {
        BigDecimal ledgerInventoryCost = getInventoryValueForProduct(productId, UtilDateTime.nowTimestamp());
        BigDecimal inventoryItemCost = getInventoryValueFromItems(productId);
        assertEquals("Inventory value for product [" + productId + "] in ledger and inventory items should be equal.  (Expected => ledger) ", inventoryItemCost, ledgerInventoryCost);
    }

    /**
     * Asserts that the inventory item has a specific unit cost and currency.
     * @param inventoryItemId the inventory item ID to test for
     * @param unitCost the expected unit cost
     * @param currencyUomId the expected currency
     * @exception GenericEntityException if an error occurs
     */
    public void assertInventoryItemUnitCost(String inventoryItemId, BigDecimal unitCost, String currencyUomId) throws GenericEntityException {
        GenericValue item = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
        assertNotNull("Could not find InventoryItem [" + inventoryItemId + "].", item);
        assertEquals("InventoryItem.unitCost check.", item.getDouble("unitCost"), unitCost);
        assertEquals("InventoryItem.currencyUomId check.", item.getString("currencyUomId"), currencyUomId);
    }


    /*************************************************************************/
    /***                                                                   ***/
    /***                        Helper Functions                           ***/
    /***                                                                   ***/
    /*************************************************************************/


    /**
     * Get the inventory quantity for a product.
     * @param productId the product ID to get inventory for
     * @return the <code>Map</code> result of the <code>getInventoryAvailableByFacility</code> service
     */
    public Map<String, Object> getInventory(String productId) {
        Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "facilityId", facilityId, "productId", productId, "useCache", new Boolean(false));
        Map<String, Object> output = null;
        try {
            TransactionUtil.begin();
            output = runAndAssertServiceSuccess("getInventoryAvailableByFacility", input);
            TransactionUtil.commit();
        } catch (GenericTransactionException e) {
            UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return output;
    }

    /**
     * Get the inventory quantity for a list of products.
     * @param productIds the <code>List</code> of product ID to get inventory for
     * @return a <code>Map</code> of productId => result of <code>getInventory</code>
     */
    public Map<String, Map<String, Object>> getInventories(List<String> productIds) {
        Map<String, Map<String, Object>> output = new HashMap<String, Map<String, Object>>();
        for (String productId : productIds) {
            output.put(productId, getInventory(productId));
        }
        return output;
    }

    /**
     * Calculates the average cost of a product using the method configured for it.
     * @param productId the product ID to get cost for
     * @return the average cost of the given product
     */
    public BigDecimal getProductAverageCost(String productId) {
        BigDecimal cost = UtilCOGS.getProductAverageCost(productId, organizationPartyId, userLogin, delegator, dispatcher);
        assertNotNull("Failed to calculate average cost for product [" + productId + "].  Value returned by UtilCOGS.getProductAverageCost() was null.", cost);
        return cost;
    }

    /**
     * Gest the value of the inventory for a product from the ledger.
     * @param productId the product ID to get inventory value for
     * @param transactionDate the time to get the inventory value for
     * @return the inventory value for the given product and date
     */
    public BigDecimal getInventoryValueForProduct(String productId, Timestamp transactionDate) {
        BigDecimal value = null;
        try {
            value = UtilCOGS.getInventoryValueForProduct(productId, organizationPartyId, transactionDate, delegator);
        } catch (GenericEntityException e) {
            TestCase.fail("Encountered exception while getting inventory value for product: " + e.getMessage());
        }
        // if zero, means no entries exist for the product, especially with new data or database
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        return value;
    }

    /**
     * Gets the value of the inventory for a product from the inventory item unit cost data.
     * @param productId the product ID to get net inventory value for
     * @return the net inventory value for the given product
     */
    public BigDecimal getInventoryValueFromItems(String productId) {
        BigDecimal value = null;
        try {
            // IMPORTANT: must get the net value which includes WIP and AVG COST adjustments or it won't necessarily equal ledger values from above method
            value = UtilCOGS.getNetInventoryValueFromItems(productId, organizationPartyId, delegator, dispatcher);
        } catch (GeneralException e) {
            TestCase.fail("Encountered exception while getting inventory value for product: " + e.getMessage());
        }
        assertNotNull("Failed to get inventory value for product [" + productId + "].  Value returned by UtilCOGS.getNetInventoryValueFromItems() was null.", value);
        return value;
    }

}
