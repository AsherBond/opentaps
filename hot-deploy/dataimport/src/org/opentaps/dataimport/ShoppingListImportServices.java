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

package org.opentaps.dataimport;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import org.opentaps.common.util.UtilCommon;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Import Shopping List via intermediate DataImportShoppingList entity.
 *
 * @author     <a href="mailto:jwickers@opensourcestrategies.com">Jeremy Wickersheimer</a>
 */
public class ShoppingListImportServices {
    public static String module = ShoppingListImportServices.class.getName();

    public static Map importShoppingLists(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productStoreId = (String) context.get("productStoreId");
        Timestamp now = UtilDateTime.nowTimestamp();
        int imported = 0;

        // main try/catch block that traps errors related to obtaining data from delegator
        try {
            // make sure the supplied productStoreId exists
            GenericValue productStore = null;
            if (! UtilValidate.isEmpty(productStoreId)) {
                productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
                if (productStore == null) {
                    return ServiceUtil.returnError("Cannot import shopping lists: productStore ["+productStoreId+"] does not exist.");
                }
            }

            // need to get an ELI because of possibly large number of records.  partyId <> null will get all records
            EntityConditionList conditions = new EntityConditionList( UtilMisc.toList(
                        new EntityExpr("partyId", EntityOperator.NOT_EQUAL, null),
                        new EntityExpr("processedTimestamp", EntityOperator.EQUALS, null)   // leave out previously processed
                        ), EntityOperator.AND);
            TransactionUtil.begin();   // since the service is not inside a transaction, this needs to be in its own transaction, or you'll get a harmless exception
            EntityListIterator importShoppingLists = delegator.findListIteratorByCondition("DataImportShoppingList", conditions, null, null);
            List<GenericValue> shoppingLists = importShoppingLists.getCompleteList();
            TransactionUtil.commit();

            for (GenericValue shoppingList : shoppingLists) {

                try {
                    // use the helper method to decode the product into a List of GenericValues
                    List toStore = decodeShoppingList(shoppingList, now, productStoreId, delegator);
                    String shoppingListForPartId = shoppingList.getString("partyId");
                    if (toStore == null) {
                        Debug.logWarning("Faild to import shoppingList for party ["+shoppingListForPartId+"] because data was bad.  Check preceding warnings for reason.", module);
                        continue;
                    }

                    // next we're going to store all each product's data in its own transaction, so if one product's data is bad, the others will still get stored
                    TransactionUtil.begin();

                    // store the results and mark this product as processed
                    delegator.storeAll(toStore);

                    // log the import
                    Debug.logInfo("Successfully imported shoppingList for party ["+shoppingListForPartId+"].", module);
                    imported += 1;

                    TransactionUtil.commit();

                } catch (GenericEntityException e) {
                    // if there was an error, we'll just skip this shopping list
                    TransactionUtil.rollback();
                    Debug.logError(e, "Faild to import shoppingList. Error stack follows.", module);
                } catch (Exception e) {
                    TransactionUtil.rollback();
                    Debug.logError(e, "Faild to import shoppingList. Error stack follows.", module);
                }
            }
            importShoppingLists.close();
        } catch (GenericEntityException e) {
            String message = "Cannot import shopping lists: Unable to use delegator to retrieve data from the database.  Error is: " + e.getMessage();
            Debug.logError(e, message, module);
            return ServiceUtil.returnError(message);
        }

        Map results = ServiceUtil.returnSuccess();
        results.put("shoppingListsImported", new Integer(imported));
        return results;
    }

    /**
     * Helper method to decode a DataImportShoppingList into a List of GenericValues modeling that the OFBiz schema.
     * If for some reason obtaining data via the delegator fails, this service throws that exception.
     * Note that everything is done with the delegator for maximum efficiency.
     */
    private static List decodeShoppingList(GenericValue data, Timestamp now, String productStoreId, GenericDelegator delegator) throws GenericEntityException, Exception {
        Map input;
        List toStore = FastList.newInstance();
        String partyId = data.getString("partyId");
        Debug.logInfo("Now processing shopping list for party ["+partyId+"]", module);

        // check that the party exists
        GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        if (party == null) {
            Debug.logInfo("Could not find party ["+partyId+"], not importing.", module);
            return null;
        }

        // check that the party doesn't have a shopping list already
        List<GenericValue> shoppingList = delegator.findByAnd("ShoppingList", UtilMisc.toMap("partyId", partyId, "shoppingListTypeId", "SLT_SPEC_PURP", "productStoreId", productStoreId ));
        if (UtilValidate.isNotEmpty(shoppingList)) {
            Debug.logInfo("Party ["+partyId+"], already has a ShoppingList defined, not importing.", module);
            return null;
        }

        // find the items to import
        // need to get an ELI because of possibly large number of records
        EntityConditionList conditions = new EntityConditionList( UtilMisc.toList(
                        new EntityExpr("partyId", EntityOperator.EQUALS, partyId),
                        new EntityExpr("processedTimestamp", EntityOperator.EQUALS, null)   // leave out previously processed
                        ), EntityOperator.AND);
        TransactionUtil.begin();   // since the service is not inside a transaction, this needs to be in its own transaction, or you'll get a harmless exception
        EntityListIterator importShoppingListItems = delegator.findListIteratorByCondition("DataImportShoppingListItem", conditions, null, null);
        List<GenericValue> shoppingListItems = importShoppingListItems.getCompleteList();
        TransactionUtil.commit();

        // check that there are items to import
        if (UtilValidate.isEmpty(shoppingListItems)) {
             Debug.logInfo("Could not find any item to import in the ShoppingList for ["+partyId+"], not importing.", module);
             return null;
        }

        // create the ShoppingList entity
        String slId = delegator.getNextSeqId("ShoppingList");
        input = FastMap.newInstance();
        input.put("partyId", partyId);
        input.put("shoppingListTypeId", "SLT_SPEC_PURP");
        input.put("productStoreId", productStoreId);
        input.put("isPublic", "N");
        input.put("isActive", "Y");
        input.put("shoppingListId", slId);
        GenericValue shoppinglist = delegator.makeValue("ShoppingList", input);

        Integer seq = 1;
        int item_imported = 0;

        for (GenericValue shoppingListItem : shoppingListItems) {

            // checking that the product exists
            String productId = shoppingListItem.getString("productId");
            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
            if (product == null) {
                Debug.logInfo("Could not find product ["+productId+"], not importing.", module);
                return null;
            }

            // checking the quantity is valid
            Double quantity = null;
            try {
                quantity = Double.valueOf(shoppingListItem.getString("quantity"));
                if (quantity == null || quantity < 0.0) {
                    Debug.logInfo("Invalid quantity ["+quantity+"], not importing.", module);
                    return null;
                }
            } catch (Exception e) {
                Debug.logInfo("Invalid quantity ["+quantity+"], not importing.", module);
                return null;
            }

            // make the seqId
            String seqId = sequencify(seq);
            seq++;

            // finally adding the ShoppingListItem entity to the import list
            input = FastMap.newInstance();
            input.put("shoppingListId", slId);
            input.put("shoppingListItemSeqId", seqId);
            input.put("productId", productId);
            input.put("quantity", quantity);
            input.put("reservLength", 0.0);
            input.put("reservPersons", 0.0);

            GenericValue item = delegator.makeValue("ShoppingListItem", input);
            toStore.add(item);

            shoppingListItem.set("processedTimestamp", UtilDateTime.nowTimestamp());
            toStore.add(shoppingListItem);
            item_imported++;
        }
        if (item_imported == 0) {
            Debug.logInfo("No item were imported successfully in the ShoppingList for ["+partyId+"].", module);
            return null;
        }

        // finally create the ShoppingList
        delegator.create(shoppinglist);

        importShoppingListItems.close();
        data.set("processedTimestamp", UtilDateTime.nowTimestamp());
        toStore.add(data);

        return toStore;
    }

    /**
     * Convert an Integer to a String sequence Id with normal padding.
     * @param idx the Integer index
     * @return the sequence id as a padded String, for example "00010"
     */
    private static String sequencify(Integer idx) {
        String seqId = idx.toString();
        String padding = "00000";
        int paddingLength = padding.length() - seqId.length();
        if (paddingLength > 0) {
            return padding.substring(0,paddingLength).concat(seqId);
        } else {
            return seqId;
        }
    }

}