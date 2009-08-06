/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package com.opensourcestrategies.financials.util;

import java.util.*;
import java.sql.Timestamp;
import java.math.BigDecimal;

import javolution.util.FastMap;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.service.*;
import org.opentaps.common.util.UtilCommon;

import com.opensourcestrategies.financials.util.UtilFinancial;

/**
 * UtilCOGS - Utilities for Cost of Goods Sold services and calculations.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a> 
 * @version    $Rev: 81 $
 * @since      2.2
 */

public class UtilCOGS {
    
    public static String module = UtilCOGS.class.getName();

    public static int decimals = UtilNumber.getBigDecimalScale("fin_arithmetic.properties", "financial.statements.decimals");
    public static int rounding = UtilNumber.getBigDecimalRoundingMode("fin_arithmetic.properties", "financial.statements.rounding");
    public static final BigDecimal ZERO = new BigDecimal("0");

    /**
     * Attempts to find the average cost of a given product.  First it will look in ProductAverageCost.  If a non-zero ProductAverageCost is found, it wil return it.
     * Otherwise, it will call calculateProductCosts to find the average cost of the product in CostComponent.
     * A null will be returned if no cost was found in either ProductAverageCost or CostComponent, or if the find attempts failed for some other entity/service reason.
     */
    public static BigDecimal getProductAverageCost(String productId, String organizationPartyId, GenericValue userLogin, GenericDelegator delegator, LocalDispatcher dispatcher) {
        BigDecimal cost = null;

        try {
            // first try using the first unexpired ProductAverageCost.averageCost as the cost
            EntityConditionList conditions = new EntityConditionList(UtilMisc.toList(
                        EntityUtil.getFilterByDateExpr(),
                        new EntityExpr("productId", EntityOperator.EQUALS, productId),
                        new EntityExpr("organizationPartyId", EntityOperator.EQUALS, organizationPartyId)
                        ), EntityOperator.AND);
            // we want the last ProductAverageCost entry with the latest fromDate.  productAverageCostId is included as a secondary ordering parameter for databases such
            // as MySQL which does not store timestamps with accuracy of better than 1 second, so you might have several ProductAverageCost with same fromDate.  In that case,
            // the latest productAverageCostId should prevail.  Note that productAverageCostId should NEVER be the first ordering criteria, because databases disagree as to
            // whether 100 precedes (MySQL) or follows (PostgreSQL) 21
            GenericValue entry = EntityUtil.getFirst(delegator.findByCondition("ProductAverageCost", conditions, UtilMisc.toList("averageCost"), UtilMisc.toList("fromDate DESC", "productAverageCostId DESC")));
            if (entry != null) {
                cost = entry.getBigDecimal("averageCost");
            }

            // if we found a non-zero cost via ProductAverageCost, then we're done
            if ((cost != null) && (cost.signum() != 0)) return cost;

            // otherwise we have to look up the standard costs (the CostComponents with prefix EST_STD_)  If we found a non-zero standard cost, then it becomes a cost
            String baseCurrencyUomId = UtilCommon.getOrgBaseCurrency(organizationPartyId, delegator);
            Map results = dispatcher.runSync("calculateProductCosts", UtilMisc.toMap("productId", productId, "currencyUomId", baseCurrencyUomId, "costComponentTypePrefix", "EST_STD", "userLogin", userLogin), -1, false); //  This service does not require a transaction
            if (!ServiceUtil.isError(results)) {
                Double totalCost = (Double) results.get("totalCost");
                if ((totalCost != null) && (totalCost.doubleValue() != 0.0)) {
                    cost = new BigDecimal(totalCost.doubleValue());
                }
            }

            // at this point, return whatever cost we have, be it null or zero
            return cost;

        } catch (GenericEntityException e) {
            Debug.logError("Failed to get product average cost for productId [" + productId + "] and organization [" + organizationPartyId + "] due to entity error.", module);
            return null;
        } catch (GenericServiceException e) {
            Debug.logError("Failed to get product average cost for productId [" + productId + "] and organization [" + organizationPartyId + "] due to service error.", module);
            return null;
        }
    }


    /**
     * Gets the value of a product in the inventory of an organization.
     * The value is the sum of posted debits minus the sum of posted credits
     * in the INVENTORY_ACCOUNT or INV_ADJ_AVG_COST GL accounts for this product.
     *
     * @param   productId               The product to get the value of
     * @param   organizationPartyId     Organization of the transactions, which is required.
     * @param   transactionDate         An optional timestamp. If specified, the sum will be taken up to this date, inclusive.
     * @return  BigDecimal value of the product
     */
    public static BigDecimal getInventoryValueForProduct(String productId, String organizationPartyId, Timestamp transactionDate, GenericDelegator delegator)
        throws GenericEntityException {
        Map results = getNetInventoryValueHelper(productId, null, organizationPartyId, transactionDate, delegator);
        return (BigDecimal) results.get(productId);
    }

    /**
     * Gets the value of all products in the inventory of an organization.
     * The value is the sum of posted debits minus the sum of posted credits
     * in the INVENTORY_ACCOUNT or INV_ADJ_AVG_COST GL accounts for each product.
     *
     * @param   productId               Specify this to limit the query to one product
     * @param   organizationPartyId     Organization of the transactions, always specify this.
     * @param   transactionDate         Specify this to sum over all transactions before the transactionDate or set to null to sum over all dates
     * @return  Map of productIds keyed to their net (debit - credit) amounts
     */
    public static Map getInventoryValueForAllProducts(String organizationPartyId, Timestamp transactionDate, GenericDelegator delegator)
        throws GenericEntityException {
        return getNetInventoryValueHelper(null, null, organizationPartyId, transactionDate, delegator);
    }

    /**
     * Gets the value of all products in the inventory of an organization with the given condition.
     * The value is the sum of posted debits minus the sum of posted credits
     * in the INVENTORY_ACCOUNT or INV_ADJ_AVG_COST GL accounts for each product.
     *
     * @param   condition               EntityCondition to constrain the results using AcctgTransEntryProdSums
     * @param   organizationPartyId     Organization of the transactions, always specify this.
     * @param   transactionDate         Specify this to sum over all transactions before the transactionDate or set to null to sum over all dates
     * @return  Map of productIds keyed to their net (debit - credit) amounts
     */
    public static Map getInventoryValueForProductsByCondition(EntityCondition condition, String organizationPartyId, Timestamp transactionDate, GenericDelegator delegator)
        throws GenericEntityException {
        return getNetInventoryValueHelper(null, condition, organizationPartyId, transactionDate, delegator);
    }

    /**
     * Helper method to get product inventory values of glFiscalTypeId ACTUAL. Use one of the other methods that implements this,
     * getInventoryValueForProduct() or getInventoryValueForAllProducts().
     *
     * @param   productId               Specify this to limit the query to one product or match on productId (to get match behavior, specify condition)
     * @param   condition               EntityCondition to constrain the results using AcctgTransEntryProdSums
     * @param   organizationPartyId     Organization of the transactions, always specify this.
     * @param   transactionDate         Specify this to sum over all transactions before the transactionDate or set to null to sum over all dates
     * @return  Map of productIds keyed to their net (debit - credit) amounts
     */
    private static Map getNetInventoryValueHelper(String productId, EntityCondition condition, String organizationPartyId, Timestamp transactionDate, GenericDelegator delegator)
            throws GenericEntityException {
        return getNetInventoryValueHelper(productId, condition, organizationPartyId, "ACTUAL", transactionDate, delegator);
    }

    /**
     * Helper method to get product inventory values.  Passes in glAccountTypeId in ("INVENTORY_ACCOUNT", "INV_ADJ_AVG_COST", "RAWMAT_INVENTORY", "WIP_INVENTORY")
     *
     * @param   productId               Specify this to limit the query to one product or match on productId (to get match behavior, specify condition)
     * @param   condition               EntityCondition to constrain the results using AcctgTransEntryProdSums
     * @param   organizationPartyId     Organization of the transactions, always specify this.
     * @param   glFiscalTypeId          Fiscal type -- ACTUAL, BUDGET, FORECAST
     * @param   transactionDate         Specify this to sum over all transactions before the transactionDate or set to null to sum over all dates
     * @return  Map of productIds keyed to their net (debit - credit) amounts
     */
    private static Map getNetInventoryValueHelper(String productId, EntityCondition condition, String organizationPartyId, String glFiscalTypeId, Timestamp transactionDate, GenericDelegator delegator)
            throws GenericEntityException {
        return getNetInventoryValueHelper(productId,
                new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("glAccountTypeId", EntityOperator.EQUALS, "INVENTORY_ACCOUNT"),
                    new EntityExpr("glAccountTypeId", EntityOperator.EQUALS, "INV_ADJ_AVG_COST"),
                    new EntityExpr("glAccountTypeId", EntityOperator.EQUALS, "RAWMAT_INVENTORY"),
                    new EntityExpr("glAccountTypeId", EntityOperator.EQUALS, "WIP_INVENTORY")
                    ), EntityOperator.OR),
                condition, organizationPartyId, glFiscalTypeId, transactionDate, delegator);
    }

    private static Map getNetInventoryValueHelper(String productId, EntityCondition glAccountTypeIds, EntityCondition condition, String organizationPartyId, String glFiscalTypeId, Timestamp transactionDate, GenericDelegator delegator)
            throws GenericEntityException {

        if (organizationPartyId == null) {
            throw new GenericEntityException("No organizationPartyId specified for getting product inventory value(s).");
        }

        // fields to select: exclude the transaction date from select list, otherwise we can't use the special view
        List selectFields = UtilMisc.toList("productId", "amount", "glAccountTypeId");

        // common AND conditions
        List commonConditions = UtilMisc.toList(
                new EntityExpr("isPosted", EntityOperator.EQUALS, "Y"),
                new EntityExpr("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                new EntityExpr("glFiscalTypeId", EntityOperator.EQUALS, glFiscalTypeId)
                );

        // select all the inventory account types
        commonConditions.add(glAccountTypeIds);

        // add optional constraints to common conditions
        if (productId != null) commonConditions.add(new EntityExpr("productId", EntityOperator.EQUALS, productId));
        if (condition != null) commonConditions.add(condition);
        if (transactionDate != null) commonConditions.add(new EntityExpr("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, transactionDate));

        // build condition for debits
        List debitConditions = new ArrayList(commonConditions);
        debitConditions.add(new EntityExpr("debitCreditFlag", EntityOperator.EQUALS, "D"));
        EntityConditionList debitConditionList = new EntityConditionList(debitConditions, EntityOperator.AND);

        // perform the query
        TransactionUtil.begin();
        EntityListIterator listIt = delegator.findListIteratorByCondition("AcctgTransEntryProdSums", debitConditionList, null, selectFields,
                UtilMisc.toList("productId"), // fields to order by
                // the first true here is for "specifyTypeAndConcur" the second true is for a distinct select
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));

        // get all values from the entity list iterator
        List debitSums = listIt.getCompleteList();
        listIt.close();
        TransactionUtil.commit();              
        
        // build condition for credits
        List creditConditions = new ArrayList(commonConditions);
        creditConditions.add(new EntityExpr("debitCreditFlag", EntityOperator.EQUALS, "C"));
        EntityConditionList creditConditionList = new EntityConditionList(creditConditions, EntityOperator.AND);

        // perform the query
        TransactionUtil.begin();
        listIt = delegator.findListIteratorByCondition("AcctgTransEntryProdSums", creditConditionList, null, selectFields,
                UtilMisc.toList("productId"), // fields to order by
                // the first true here is for "specifyTypeAndConcur" the second true is for a distinct select
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));

        // get all values from the entity list iterator
        List creditSums = listIt.getCompleteList();
        listIt.close();
        TransactionUtil.commit();
        
        // make a map of the product Id to the (debit - credit) amount
        Map inventoryValueByProduct = FastMap.newInstance();

        // the strategy is to store the negative of the credit amount in the map first (because debits might not exist for a particular product)
        for (Iterator iter = creditSums.iterator(); iter.hasNext(); ) {
            GenericValue value = (GenericValue) iter.next();
            BigDecimal creditAmount = (BigDecimal) inventoryValueByProduct.get(UtilFinancial.getProductIdOrDefault(value));            
            if (creditAmount == null) creditAmount = ZERO;
            inventoryValueByProduct.put(UtilFinancial.getProductIdOrDefault(value), creditAmount.subtract(value.getBigDecimal("amount")).setScale(decimals, rounding));
        }

        // then go through debits and add the debit amounts
        for (Iterator iter = debitSums.iterator(); iter.hasNext(); ) {
            GenericValue value = (GenericValue) iter.next();
            BigDecimal debitAmount = (BigDecimal) inventoryValueByProduct.get(UtilFinancial.getProductIdOrDefault(value));
            if (debitAmount == null) debitAmount = ZERO;  // if debit didn't exist, set to zero
            BigDecimal difference = value.getBigDecimal("amount").add(debitAmount).setScale(decimals, rounding);
            inventoryValueByProduct.put(UtilFinancial.getProductIdOrDefault(value), difference);
        }

        return inventoryValueByProduct;
    }
    
    /**
     * Method to get total inventory quantity on hand in all facilities for a given product and organizataiton.
     */
    public static BigDecimal getInventoryQuantityForProduct(String productId, String organizationPartyId, GenericDelegator delegator, LocalDispatcher dispatcher) 
        throws GenericServiceException, GenericEntityException {
        BigDecimal quantity = ZERO;

        // the strategy is to loop through the organization facilities and get the product inventory for each facility
        List facilities = delegator.findByAnd("Facility", UtilMisc.toMap("ownerPartyId", organizationPartyId));
        if (UtilValidate.isNotEmpty(facilities)) {
        	for (Iterator iter = facilities.iterator(); iter.hasNext(); ) {
	            String facilityId = ((GenericValue) iter.next()).getString("facilityId");
	            Map serviceResults = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId), -1, false);
	            if (ServiceUtil.isError(serviceResults)) {
	                throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
	            }
	            BigDecimal facilityQuantity = new BigDecimal(((Double) serviceResults.get("quantityOnHandTotal")).doubleValue());
	            quantity = quantity.add(facilityQuantity).setScale(decimals, rounding); // TODO: quantity sum needs its own decimals and rounding
	        }
        } else {
        	// alternatively, add up InventoryItem.quantityOnHandTotal
        	Debug.logWarning("No facilities found for owner party ID [" + organizationPartyId + "].  Will use InventoryItem.ownerPartyId directly to find QOH total", module);
        	List items = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "ownerPartyId", organizationPartyId));
        	for (Iterator iter = items.iterator(); iter.hasNext(); ) {
        		GenericValue item = (GenericValue) iter.next();
        		if (item.getBigDecimal("quantityOnHandTotal") != null) {
        			quantity = quantity.add(item.getBigDecimal("quantityOnHandTotal")).setScale(decimals, rounding); 
        		}
        	}
    	        
        }
    	return quantity;
    }
    
    /**
     * Returns the total inventory value based on data from InventoryItem rather than accounting ledger data, but not including any AVG COST adjustments
     * or any WIP INVENTORY which has not been received into inventory as items yet
     * 
     * @param productId
     * @param organizationPartyId
     * @param delegator
     * @param dispatcher
     * @return
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    public static BigDecimal getInventoryValueFromItems(String productId, String organizationPartyId, GenericDelegator delegator, LocalDispatcher dispatcher) 
        throws GenericEntityException, GenericServiceException {
        // get the non-serialized items first
        List items = delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "ownerPartyId", organizationPartyId, "inventoryItemTypeId", "NON_SERIAL_INV_ITEM"));
        // now add all the serialized items: these are the states which are consistent with a QOH = 1.0
        List serializedItems = delegator.findByAnd("InventoryItem", UtilMisc.toList(
                new EntityExpr("productId", EntityOperator.EQUALS, productId),
                new EntityExpr("inventoryItemTypeId", EntityOperator.EQUALS, "SERIALIZED_INV_ITEM"),
                new EntityExpr("ownerPartyId", EntityOperator.EQUALS, organizationPartyId),
                new EntityExpr("statusId", EntityOperator.IN, UtilMisc.toList("INV_AVAILABLE", "INV_PROMISED", "INV_BEING_TRANSFERED"))));
        if (UtilValidate.isNotEmpty(serializedItems)) {
            items.addAll(serializedItems);
        }
        BigDecimal total = ZERO;
        
        for (Iterator it = items.iterator(); it.hasNext(); ) {
            GenericValue item = (GenericValue) it.next();
            if ((item.get("unitCost") != null) && (item.get("quantityOnHandTotal") != null)) {
                BigDecimal conversionFactor = new BigDecimal(UtilFinancial.determineUomConversionFactor(delegator, dispatcher, organizationPartyId, item.getString("currencyUomId")));
                // this precision is probably OK since we're multiplying
                total = total.add(item.getBigDecimal("unitCost").multiply(item.getBigDecimal("quantityOnHandTotal")).multiply(conversionFactor)).setScale(decimals, rounding);
            }
        }
        
        return total;
    }

    /**
     * Gets the value of inventory items plus any value in WIP inventory and average cost adjustment accounts
     * This **should** equal to the value of inventory on the GL
     * @param productId
     * @param organizationPartyId
     * @param delegator
     * @param dispatcher
     * @return
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    public static BigDecimal getNetInventoryValueFromItems(String productId, String organizationPartyId, GenericDelegator delegator, LocalDispatcher dispatcher)
            throws GenericEntityException, GenericServiceException {
        // get the net value of inventory items
        BigDecimal valueOfItems = getInventoryValueFromItems(productId, organizationPartyId, delegator, dispatcher);
        // get the value of the average cost adjustments and wip inventory items for this product, no other conditions, on the ACTUAL ledger as of the current timestamp
        // note that WIP inventory is added because the value is recorded but no finished InventoryItems are created yet
        Map results = getNetInventoryValueHelper(productId, new EntityExpr("glAccountTypeId", EntityOperator.IN, UtilMisc.toList("INV_ADJ_AVG_COST", "WIP_INVENTORY")), null, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        BigDecimal valueOfAdjustments = BigDecimal.ZERO;
        if (results != null) {
            valueOfAdjustments = (BigDecimal) results.get(productId);
            if (valueOfAdjustments == null) valueOfAdjustments = BigDecimal.ZERO;
        }

        return valueOfItems.add(valueOfAdjustments).setScale(decimals, rounding);
    }
}
