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

package com.opensourcestrategies.financials.cogs;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.opensourcestrategies.financials.util.UtilCOGS;
import com.opensourcestrategies.financials.util.UtilFinancial;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * COGSServices - Services for handling Cost of Goods Sold.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 102 $
 * @since      2.2
 */
public final class COGSServices {

    private COGSServices() { }

    private static final String MODULE = COGSServices.class.getName();

    /** Number of decimals to round to, this is from financials settings in "fin_arithmetic.properties". */
    public static int decimals = UtilNumber.getBigDecimalScale("fin_arithmetic.properties", "financial.statements.decimals");
    /** Rounding method to use, this is from financials settings in "fin_arithmetic.properties". */
    public static int rounding = UtilNumber.getBigDecimalRoundingMode("fin_arithmetic.properties", "financial.statements.rounding");

    /**
     * Updates the average cost entry for a product, using inventory value and quantity.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updateProductAverageCost(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productId = (String) context.get("productId");
        String organizationPartyId = (String) context.get("organizationPartyId");
        try {

            // Skip if the organization is not an internal organization
            if (UtilValidate.isNotEmpty(organizationPartyId) && !UtilFinancial.hasPartyRole(organizationPartyId, "INTERNAL_ORGANIZATIO", delegator)) {
                Debug.logInfo("updateProductAverageCost:  Organization party [" + organizationPartyId + "] is not an internal organization.  Not updating product average cost.", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // get the inventory value for this product - it is important to use this method because it will account for Average Cost adjustments in past transactions as well
            BigDecimal inventoryValue = UtilCOGS.getInventoryValueForProduct(productId, organizationPartyId, UtilDateTime.nowTimestamp(), delegator);
            // get the quantity of this product
            BigDecimal inventoryQuantity = UtilCOGS.getInventoryQuantityForProduct(productId, organizationPartyId, delegator, dispatcher);

            // average cost = value / quantity.  this should not be rounded.
            // TODO: put the # of decimal places in fin_arithmetic.properties
            BigDecimal averageCost = BigDecimal.ZERO;
            if (inventoryValue == null) {
                Debug.logWarning("Inventory value is null for product [" + productId + "], setting average cost to zero", MODULE);
                averageCost = BigDecimal.ZERO;
            } else if (!(inventoryQuantity.compareTo(BigDecimal.ZERO) == 0)) {
                averageCost = inventoryValue.divide(inventoryQuantity, 100, rounding);
            } else {
                Debug.logWarning("Inventory quantity is zero for product [" + productId + "], setting average cost to zero", MODULE);
            }

            // create a new product average cost record
            Map input = UtilMisc.toMap("productId", productId, "organizationPartyId", organizationPartyId, "userLogin", userLogin);
            input.put("averageCost", new Double(averageCost.doubleValue()));

            Debug.logInfo("value = " + inventoryValue + " quantity = " + inventoryQuantity + " avg = " + averageCost, MODULE);

            // run inside same transaction
            Map serviceResults = dispatcher.runSync("createProductAverageCost", input, -1, false);
            if (ServiceUtil.isError(serviceResults)) {
                return ServiceUtil.returnError("Failed to update product average cost.", null, null, serviceResults);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Updates average cost for every product item on a purchase invoice.
     * Only works for purchase invoices.
     * No longer in use - now using <code>updateReceiptAverageCosts</code> instead.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updateInvoiceAverageCosts(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String invoiceId = (String) context.get("invoiceId");   // input parameter
        try {
            GenericValue invoice = delegator.findByPrimaryKeyCache("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            if (invoice.getString("invoiceTypeId").equals("PURCHASE_INVOICE")) {
                // Determine the organization party and get the Party
                GenericValue organizationParty = invoice.getRelatedOne("Party");

                // calls updateProductAverageCost on every invoice item which is a product item and has a productId
                // TODO: Maybe we should not restrict it to product items, but other types of invoice items as well?  In that case, build a Set of productIds?
                List<GenericValue> invoiceItems = invoice.getRelatedByAndCache("InvoiceItem", UtilMisc.toMap("invoiceItemTypeId", "PINV_FPROD_ITEM"));
                for (Iterator<GenericValue> iIi = invoiceItems.iterator(); iIi.hasNext();) {
                    GenericValue invoiceItem = iIi.next();
                    if (invoiceItem.getString("productId") != null) {
                        Debug.logInfo("calling updateProductAverageCost with " + invoiceItem.getString("productId") + " " + organizationParty.getString("partyId"), MODULE);
                        Map result = dispatcher.runSync("updateProductAverageCost", UtilMisc.toMap("organizationPartyId", organizationParty.getString("partyId"),
                                "productId", invoiceItem.getString("productId"), "userLogin", userLogin));
                    }
                }
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Updates average cost for the inventory item on a shipment receipt (there is only one item per receipt).
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updateReceiptAverageCost(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String receiptId = (String) context.get("receiptId");   // input parameter
        try {
            GenericValue receipt = delegator.findByPrimaryKeyCache("ShipmentReceipt", UtilMisc.toMap("receiptId", receiptId));
            GenericValue inventoryItem = receipt.getRelatedOne("InventoryItem");

            // For now, skip serialized inventory items
            if ("SERIALIZED_INV_ITEM".equals(inventoryItem.get("inventoryItemTypeId"))) {
                Debug.logInfo("updateProductAverageCost:  Encountered serialized InventoryItem [" + inventoryItem.get("inventoryItemId") + "].  Not updating average cost.", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // update average cost for the productId, ownerPartyId from inventory item
            Map result = dispatcher.runSync("updateProductAverageCost", UtilMisc.toMap("organizationPartyId", inventoryItem.getString("ownerPartyId"),
                    "productId", inventoryItem.getString("productId"), "userLogin", userLogin));

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Creates a CostComponent for the workEffortId of the difference between the inventory item's average cost for its owner party
     *  and the item's unit cost, multiplied by the quantity issued, if the item's owner uses average cost method of accounting.
     * If average cost is below item cost, it should create a negative CostComponent.
     * Note that workEffortId is the workEffortId of a production run task.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map addAvgCostAdjToProductionRunCosts(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String workEffortId = (String) context.get("workEffortId");
        String inventoryItemId = (String) context.get("inventoryItemId");
        BigDecimal quantityIssued = new BigDecimal((Double) context.get("quantity"));
        try {
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            String ownerPartyId = inventoryItem.getString("ownerPartyId");
            String productId = inventoryItem.getString("productId");
            BigDecimal unitCost = inventoryItem.getBigDecimal("unitCost");
            if (unitCost == null) {
                return ServiceUtil.returnError("Cannot add average cost adjustment to production run costs :  No unit cost for product [" + productId + "] defined for inventory item [" + inventoryItemId + "].");
            }

            // get the production run task
            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (UtilValidate.isEmpty(workEffort)) {
                return ServiceUtil.returnError("No workeffort found for workEffortId [" + workEffortId + "]");
            }

            // convert the inventory item's unit cost into the owner's currency
            BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, ownerPartyId, inventoryItem.getString("currencyUomId"));
            unitCost = unitCost.multiply(conversionFactor).setScale(decimals, rounding);

            // Get owner's party COGS method.  If method is COGS_AVG_COST, also compute the inventory adjustment amount = (prodAvgCost - unitCost) * quantityIssued
            BigDecimal inventoryAdjAmount = null;
            if ("COGS_AVG_COST".equals(UtilCommon.getOrgCOGSMethodId(ownerPartyId, delegator))) {
                BigDecimal prodAvgCost = UtilCOGS.getProductAverageCost(productId, ownerPartyId, userLogin, delegator, dispatcher);
                if (prodAvgCost == null) {
                   Debug.logWarning("Unable to find a product average cost for product [" + productId + "] in organization [" + ownerPartyId + "], no adjustment will be made for item issuance", MODULE);
                } else {
                   // TODO: there could be rounding issues here; maybe it's better to do something like this:
                   //       (prodAvgCost - unitCost) * quantityOnHandVar and then set the scale.
                   inventoryAdjAmount = prodAvgCost.subtract(unitCost).multiply(quantityIssued).setScale(decimals, rounding);
                }
            }

            if ((inventoryAdjAmount == null) || (inventoryAdjAmount.compareTo(BigDecimal.ZERO) == 0)) {
                Debug.logInfo("Cost adjustment amount is null or zero for product [" + productId + "] in organization [" + ownerPartyId + "], no adjustment will be made for item issuance.", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // Create the cost component with the average cost adjustment
            String baseCurrencyUomId = UtilCommon.getOrgBaseCurrency(ownerPartyId, delegator);
            Map serviceParams = UtilMisc.toMap("workEffortId", workEffortId, "cost", inventoryAdjAmount,
                    "costComponentTypeId", "ACTUAL_MAT_COST", "costUomId", baseCurrencyUomId,
                    "userLogin", userLogin);

            return dispatcher.runSync("createCostComponent", serviceParams);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Create an updated product average cost entry for a product, using inventory value and quantity.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createProductAverageCost(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String productId = (String) context.get("productId");
        String organizationPartyId = (String) context.get("organizationPartyId");
        Double averageCost = (Double) context.get("averageCost");
        try {
            EntityCondition conditionList = EntityCondition.makeCondition(EntityOperator.AND,
                            EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                            EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                            EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
            List<GenericValue> productAverageCosts = delegator.findByCondition("ProductAverageCost", conditionList, null, null);
            for (GenericValue productAverageCost : productAverageCosts) {
                productAverageCost.set("thruDate", UtilDateTime.nowTimestamp());
                delegator.store(productAverageCost);
            }
            String getNextProductAverageCostId = delegator.getNextSeqId("ProductAverageCost");
            GenericValue newProductAverageCost = delegator.create("ProductAverageCost",
                    UtilMisc.toMap("productAverageCostId", getNextProductAverageCostId,
                            "organizationPartyId", organizationPartyId,
                            "productId", productId,
                            "averageCost", new BigDecimal(averageCost),
                            "fromDate", UtilDateTime.nowTimestamp()
                            ));
            delegator.store(newProductAverageCost);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
}
