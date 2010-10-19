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

package com.opensourcestrategies.financials.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;

/**
 * UtilCOGS - Utilities for Cost of Goods Sold services and calculations.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 81 $
 * @since      2.2
 */
public final class UtilCOGS {

    private UtilCOGS() { }

    private static String MODULE = UtilCOGS.class.getName();

    public static int decimals = UtilNumber.getBigDecimalScale("fin_arithmetic.properties", "financial.statements.decimals");
    public static int rounding = UtilNumber.getBigDecimalRoundingMode("fin_arithmetic.properties", "financial.statements.rounding");
    public static final BigDecimal ZERO = BigDecimal.ZERO;

    /**
     * Attempts to find the average cost of a given product.  First it will look in ProductAverageCost.  If a non-zero ProductAverageCost is found, it wil return it.
     * Otherwise, it will call calculateProductCosts to find the average cost of the product in CostComponent.
     * A null will be returned if no cost was found in either ProductAverageCost or CostComponent, or if the find attempts failed for some other entity/service reason.
     * @param productId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @return a <code>BigDecimal</code> value
     */
    public static BigDecimal getProductAverageCost(String productId, String organizationPartyId, GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher) {
        BigDecimal cost = null;

        try {
            // first try using the first unexpired ProductAverageCost.averageCost as the cost
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityUtil.getFilterByDateExpr(),
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId));
            // we want the last ProductAverageCost entry with the latest fromDate.  productAverageCostId is included as a secondary ordering parameter for databases such
            // as MySQL which does not store timestamps with accuracy of better than 1 second, so you might have several ProductAverageCost with same fromDate.  In that case,
            // the latest productAverageCostId should prevail.  Note that productAverageCostId should NEVER be the first ordering criteria, because databases disagree as to
            // whether 100 precedes (MySQL) or follows (PostgreSQL) 21
            GenericValue entry = EntityUtil.getFirst(delegator.findByCondition("ProductAverageCost", conditions, UtilMisc.toList("averageCost"), UtilMisc.toList("fromDate DESC", "productAverageCostId DESC")));
            if (entry != null) {
                cost = entry.getBigDecimal("averageCost");
            }

            // if we found a non-zero cost via ProductAverageCost, then we're done
            if ((cost != null) && (cost.signum() != 0)) {
                return cost;
            }

            // otherwise we have to look up the standard costs (the CostComponents with prefix EST_STD_)  If we found a non-zero standard cost, then it becomes a cost
            String baseCurrencyUomId = UtilCommon.getOrgBaseCurrency(organizationPartyId, delegator);
            Map<String, Object> results = dispatcher.runSync("calculateProductCosts", UtilMisc.toMap("productId", productId, "currencyUomId", baseCurrencyUomId, "costComponentTypePrefix", "EST_STD", "userLogin", userLogin), -1, false); //  This service does not require a transaction
            if (!ServiceUtil.isError(results)) {
                BigDecimal totalCost = (BigDecimal) results.get("totalCost");
                if ((totalCost != null) && (!totalCost.equals(BigDecimal.ZERO))) {
                    cost = totalCost;
                }
            }

            // at this point, return whatever cost we have, be it null or zero
            return cost;

        } catch (GenericEntityException e) {
            Debug.logError("Failed to get product average cost for productId [" + productId + "] and organization [" + organizationPartyId + "] due to entity error.", MODULE);
            return null;
        } catch (GenericServiceException e) {
            Debug.logError("Failed to get product average cost for productId [" + productId + "] and organization [" + organizationPartyId + "] due to service error.", MODULE);
            return null;
        }
    }


    /**
     * Gets the value of a product in the inventory of an organization.
     * The value is the sum of posted debits minus the sum of posted credits
     * in the INVENTORY_ACCOUNT or INV_ADJ_AVG_COST GL accounts for this product.
     *
     * @param productId               The product to get the value of
     * @param organizationPartyId     Organization of the transactions, which is required.
     * @param transactionDate         An optional timestamp. If specified, the sum will be taken up to this date, inclusive.
     * @param delegator a <code>Delegator</code> value
     * @return  BigDecimal value of the product
     * @exception GenericEntityException if an error occurs
     */
    public static BigDecimal getInventoryValueForProduct(String productId, String organizationPartyId, Timestamp transactionDate, Delegator delegator) throws GenericEntityException {
        Map<String, BigDecimal> results = getNetInventoryValueHelper(productId, null, organizationPartyId, transactionDate, delegator);
        return results.get(productId);
    }

    /**
     * Gets the value of all products in the inventory of an organization.
     * The value is the sum of posted debits minus the sum of posted credits
     * in the INVENTORY_ACCOUNT or INV_ADJ_AVG_COST GL accounts for each product.
     *
     * @param organizationPartyId     Organization of the transactions, always specify this.
     * @param transactionDate         Specify this to sum over all transactions before the transactionDate or set to null to sum over all dates
     * @param delegator a <code>Delegator</code> value
     * @return  Map of productIds keyed to their net (debit - credit) amounts
     * @exception GenericEntityException if an error occurs
     */
    public static Map<String, BigDecimal> getInventoryValueForAllProducts(String organizationPartyId, Timestamp transactionDate, Delegator delegator) throws GenericEntityException {
        return getNetInventoryValueHelper(null, null, organizationPartyId, transactionDate, delegator);
    }

    /**
     * Gets the value of all products in the inventory of an organization with the given condition.
     * The value is the sum of posted debits minus the sum of posted credits
     * in the INVENTORY_ACCOUNT or INV_ADJ_AVG_COST GL accounts for each product.
     *
     * @param condition               EntityCondition to constrain the results using AcctgTransEntryProdSums
     * @param organizationPartyId     Organization of the transactions, always specify this.
     * @param transactionDate         Specify this to sum over all transactions before the transactionDate or set to null to sum over all dates
     * @param delegator a <code>Delegator</code> value
     * @return  Map of productIds keyed to their net (debit - credit) amounts
     * @exception GenericEntityException if an error occurs
     */
    public static Map<String, BigDecimal> getInventoryValueForProductsByCondition(EntityCondition condition, String organizationPartyId, Timestamp transactionDate, Delegator delegator) throws GenericEntityException {
        return getNetInventoryValueHelper(null, condition, organizationPartyId, transactionDate, delegator);
    }

    /**
     * Helper method to get product inventory values of glFiscalTypeId ACTUAL. Use one of the other methods that implements this,
     * getInventoryValueForProduct() or getInventoryValueForAllProducts().
     *
     * @param productId               Specify this to limit the query to one product or match on productId (to get match behavior, specify condition)
     * @param condition               EntityCondition to constrain the results using AcctgTransEntryProdSums
     * @param organizationPartyId     Organization of the transactions, always specify this.
     * @param transactionDate         Specify this to sum over all transactions before the transactionDate or set to null to sum over all dates
     * @param delegator a <code>Delegator</code> value
     * @return  Map of productIds keyed to their net (debit - credit) amounts
     * @exception GenericEntityException if an error occurs
     */
    private static Map<String, BigDecimal> getNetInventoryValueHelper(String productId, EntityCondition condition, String organizationPartyId, Timestamp transactionDate, Delegator delegator) throws GenericEntityException {
        return getNetInventoryValueHelper(productId, condition, organizationPartyId, "ACTUAL", transactionDate, delegator);
    }

    /**
     * Helper method to get product inventory values.  Passes in glAccountTypeId in ("INVENTORY_ACCOUNT", "INV_ADJ_AVG_COST", "RAWMAT_INVENTORY", "WIP_INVENTORY")
     *
     * @param productId               Specify this to limit the query to one product or match on productId (to get match behavior, specify condition)
     * @param condition               EntityCondition to constrain the results using AcctgTransEntryProdSums
     * @param organizationPartyId     Organization of the transactions, always specify this.
     * @param glFiscalTypeId          Fiscal type -- ACTUAL, BUDGET, FORECAST
     * @param transactionDate         Specify this to sum over all transactions before the transactionDate or set to null to sum over all dates
     * @param delegator a <code>Delegator</code> value
     * @return  Map of productIds keyed to their net (debit - credit) amounts
     * @exception GenericEntityException if an error occurs
     */
    private static Map<String, BigDecimal> getNetInventoryValueHelper(String productId, EntityCondition condition, String organizationPartyId, String glFiscalTypeId, Timestamp transactionDate, Delegator delegator) throws GenericEntityException {
        return getNetInventoryValueHelper(productId,
                    EntityCondition.makeCondition(EntityOperator.OR,
                      EntityCondition.makeCondition("glAccountTypeId", EntityOperator.EQUALS, "INVENTORY_ACCOUNT"),
                      EntityCondition.makeCondition("glAccountTypeId", EntityOperator.EQUALS, "INV_ADJ_AVG_COST"),
                      EntityCondition.makeCondition("glAccountTypeId", EntityOperator.EQUALS, "RAWMAT_INVENTORY"),
                      EntityCondition.makeCondition("glAccountTypeId", EntityOperator.EQUALS, "WIP_INVENTORY")
                    ),
                    condition, organizationPartyId, glFiscalTypeId, transactionDate, delegator);
    }

    private static Map<String, BigDecimal> getNetInventoryValueHelper(String productId, EntityCondition glAccountTypeIds, EntityCondition condition, String organizationPartyId, String glFiscalTypeId, Timestamp transactionDate, Delegator delegator) throws GenericEntityException {

        if (organizationPartyId == null) {
            throw new GenericEntityException("No organizationPartyId specified for getting product inventory value(s).");
        }

        // fields to select: exclude the transaction date from select list, otherwise we can't use the special view
        List<String> selectFields = UtilMisc.toList("productId", "amount", "glAccountTypeId");

        // common AND conditions
        List<EntityCondition> commonConditions = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"),
                EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, glFiscalTypeId)
                );

        // select all the inventory account types
        commonConditions.add(glAccountTypeIds);

        // add optional constraints to common conditions
        if (productId != null) {
            commonConditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
        }
        if (condition != null) {
            commonConditions.add(condition);
        }
        if (transactionDate != null) {
            commonConditions.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, transactionDate));
        }

        // build condition for debits
        List<EntityCondition> debitConditions = new ArrayList<EntityCondition>(commonConditions);
        debitConditions.add(EntityCondition.makeCondition("debitCreditFlag", EntityOperator.EQUALS, "D"));
        EntityCondition debitConditionList = EntityCondition.makeCondition(debitConditions, EntityOperator.AND);

        // perform the query
        TransactionUtil.begin();
        EntityListIterator listIt = delegator.findListIteratorByCondition("AcctgTransEntryProdSums", debitConditionList, null, selectFields,
                UtilMisc.toList("productId"), // fields to order by
                // the first true here is for "specifyTypeAndConcur" the second true is for a distinct select
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));

        // get all values from the entity list iterator
        List<GenericValue> debitSums = listIt.getCompleteList();
        listIt.close();
        TransactionUtil.commit();

        // build condition for credits
        List<EntityCondition> creditConditions = new ArrayList<EntityCondition>(commonConditions);
        creditConditions.add(EntityCondition.makeCondition("debitCreditFlag", EntityOperator.EQUALS, "C"));
        EntityCondition creditConditionList = EntityCondition.makeCondition(creditConditions, EntityOperator.AND);

        // perform the query
        TransactionUtil.begin();
        listIt = delegator.findListIteratorByCondition("AcctgTransEntryProdSums", creditConditionList, null, selectFields,
                UtilMisc.toList("productId"), // fields to order by
                // the first true here is for "specifyTypeAndConcur" the second true is for a distinct select
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));

        // get all values from the entity list iterator
        List<GenericValue> creditSums = listIt.getCompleteList();
        listIt.close();
        TransactionUtil.commit();

        // make a map of the product Id to the (debit - credit) amount
        Map<String, BigDecimal> inventoryValueByProduct = FastMap.newInstance();

        // the strategy is to store the negative of the credit amount in the map first (because debits might not exist for a particular product)
        for (Iterator<GenericValue> iter = creditSums.iterator(); iter.hasNext();) {
            GenericValue value = iter.next();
            BigDecimal creditAmount = inventoryValueByProduct.get(UtilFinancial.getProductIdOrDefault(value));
            if (creditAmount == null) {
                creditAmount = ZERO;
            }
            inventoryValueByProduct.put(UtilFinancial.getProductIdOrDefault(value), creditAmount.subtract(value.getBigDecimal("amount")).setScale(decimals, rounding));
        }

        // then go through debits and add the debit amounts
        for (Iterator<GenericValue> iter = debitSums.iterator(); iter.hasNext();) {
            GenericValue value = iter.next();
            BigDecimal debitAmount = inventoryValueByProduct.get(UtilFinancial.getProductIdOrDefault(value));
            if (debitAmount == null) {
                debitAmount = ZERO;  // if debit didn't exist, set to zero
            }
            BigDecimal difference = value.getBigDecimal("amount").add(debitAmount).setScale(decimals, rounding);
            inventoryValueByProduct.put(UtilFinancial.getProductIdOrDefault(value), difference);
        }

        return inventoryValueByProduct;
    }

    /**
     * Method to get total inventory quantity on hand in all facilities for a given product and organizataiton.
     * @param productId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @return a <code>BigDecimal</code> value
     * @exception GenericServiceException if an error occurs
     * @exception GenericEntityException if an error occurs
     */
    public static BigDecimal getInventoryQuantityForProduct(String productId, String organizationPartyId, Delegator delegator, LocalDispatcher dispatcher) throws GenericServiceException, GenericEntityException {
        BigDecimal quantity = ZERO;

        // the strategy is to loop through the organization facilities and get the product inventory for each facility
        List<GenericValue> facilities = delegator.findByAnd("Facility", UtilMisc.toMap("ownerPartyId", organizationPartyId));
        if (UtilValidate.isNotEmpty(facilities)) {
            for (Iterator<GenericValue> iter = facilities.iterator(); iter.hasNext();) {
                String facilityId = iter.next().getString("facilityId");
                Map<String, Object> serviceResults = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId), -1, false);
                if (ServiceUtil.isError(serviceResults)) {
                    throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
                }
                BigDecimal facilityQuantity = (BigDecimal) serviceResults.get("quantityOnHandTotal");
                quantity = quantity.add(facilityQuantity).setScale(decimals, rounding); // TODO: quantity sum needs its own decimals and rounding
            }
        } else {
            // alternatively, add up InventoryItem.quantityOnHandTotal
            Debug.logWarning("No facilities found for owner party ID [" + organizationPartyId + "].  Will use InventoryItem.ownerPartyId directly to find QOH total", MODULE);
            List<GenericValue> items = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "ownerPartyId", organizationPartyId));
            for (Iterator<GenericValue> iter = items.iterator(); iter.hasNext();) {
                GenericValue item = iter.next();
                if (item.getBigDecimal("quantityOnHandTotal") != null) {
                    quantity = quantity.add(item.getBigDecimal("quantityOnHandTotal")).setScale(decimals, rounding);
                }
            }

        }
        return quantity;
    }

    /**
     * Returns the total inventory value based on data from InventoryItem rather than accounting ledger data, but not including any AVG COST adjustments
     * or any WIP INVENTORY which has not been received into inventory as items yet.
     *
     * @param productId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @return a <code>BigDecimal</code> value
     * @exception GenericEntityException if an error occurs
     * @exception GenericServiceException if an error occurs
     */
    public static BigDecimal getInventoryValueFromItems(String productId, String organizationPartyId, Delegator delegator, LocalDispatcher dispatcher) throws GenericEntityException, GenericServiceException {
        // get the non-serialized items first
        List<GenericValue> items = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "ownerPartyId", organizationPartyId, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM"));
        // now add all the serialized items: these are the states which are consistent with a QOH = 1.0
        List<GenericValue> serializedItems = delegator.findByAnd("InventoryItem", UtilMisc.toList(
                EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                EntityCondition.makeCondition("inventoryItemTypeId", EntityOperator.EQUALS, "SERIALIZED_INV_ITEM"),
                EntityCondition.makeCondition("ownerPartyId", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("statusId", EntityOperator.IN, UtilMisc.toList("INV_AVAILABLE", "INV_PROMISED", "INV_BEING_TRANSFERED"))));
        if (UtilValidate.isNotEmpty(serializedItems)) {
            items.addAll(serializedItems);
        }
        BigDecimal total = ZERO;

        for (Iterator<GenericValue> it = items.iterator(); it.hasNext();) {
            GenericValue item = it.next();
            if ((item.get("unitCost") != null) && (item.get("quantityOnHandTotal") != null)) {
                BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, organizationPartyId, item.getString("currencyUomId"));
                // this precision is probably OK since we're multiplying
                total = total.add(item.getBigDecimal("unitCost").multiply(item.getBigDecimal("quantityOnHandTotal")).multiply(conversionFactor)).setScale(decimals, rounding);
            }
        }

        return total;
    }

    /**
     * Gets the value of inventory items plus any value in WIP inventory and average cost adjustment accounts
     * This **should** equal to the value of inventory on the GL.
     * @param productId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @return a <code>BigDecimal</code> value
     * @exception GenericEntityException if an error occurs
     * @exception GenericServiceException if an error occurs
     */
    public static BigDecimal getNetInventoryValueFromItems(String productId, String organizationPartyId, Delegator delegator, LocalDispatcher dispatcher)
            throws GenericEntityException, GenericServiceException {
        // get the net value of inventory items
        BigDecimal valueOfItems = getInventoryValueFromItems(productId, organizationPartyId, delegator, dispatcher);
        // get the value of the average cost adjustments and wip inventory items for this product, no other conditions, on the ACTUAL ledger as of the current timestamp
        // note that WIP inventory is added because the value is recorded but no finished InventoryItems are created yet
        Map<String, BigDecimal> results = getNetInventoryValueHelper(productId, EntityCondition.makeCondition("glAccountTypeId", EntityOperator.IN, UtilMisc.toList("INV_ADJ_AVG_COST", "WIP_INVENTORY")), null, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        BigDecimal valueOfAdjustments = BigDecimal.ZERO;
        if (results != null) {
            valueOfAdjustments = results.get(productId);
            if (valueOfAdjustments == null) {
                valueOfAdjustments = BigDecimal.ZERO;
            }
        }

        return valueOfItems.add(valueOfAdjustments).setScale(decimals, rounding);
    }
}
