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

package org.opentaps.dataimport;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * Import Shopping List via intermediate DataImportShoppingList entity.
 *
 * @author     <a href="mailto:jwickers@opensourcestrategies.com">Jeremy Wickersheimer</a>
 */
public final class ShoppingListImportServices {

    private static String MODULE = ShoppingListImportServices.class.getName();

    private ShoppingListImportServices() { }

    public static Map<String, Object> importShoppingLists(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        String productStoreId = (String) context.get("productStoreId");
        Timestamp now = UtilDateTime.nowTimestamp();
        int imported = 0;

        // main try/catch block that traps errors related to obtaining data from delegator
        try {
            // make sure the supplied productStoreId exists
            GenericValue productStore = null;
            if (UtilValidate.isNotEmpty(productStoreId)) {
                productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
                if (productStore == null) {
                    return ServiceUtil.returnError("Cannot import shopping lists: productStore [" + productStoreId + "] does not exist.");
                }
            }

            // need to get an ELI because of possibly large number of records.  partyId <> null will get all records
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("partyId", EntityOperator.NOT_EQUAL, null),
                        EntityCondition.makeCondition("processedTimestamp", EntityOperator.EQUALS, null)   // leave out previously processed
            );
            TransactionUtil.begin();   // since the service is not inside a transaction, this needs to be in its own transaction, or you'll get a harmless exception
            EntityListIterator importShoppingLists = delegator.findListIteratorByCondition("DataImportShoppingList", conditions, null, null);
            List<GenericValue> shoppingLists = importShoppingLists.getCompleteList();
            TransactionUtil.commit();

            for (GenericValue shoppingList : shoppingLists) {

                try {
                    // use the helper method to decode the product into a List of GenericValues
                    List<GenericValue> toStore = decodeShoppingList(shoppingList, now, productStoreId, delegator);
                    String shoppingListForPartId = shoppingList.getString("partyId");
                    if (toStore == null) {
                        Debug.logWarning("Faild to import shoppingList for party [" + shoppingListForPartId + "] because data was bad.  Check preceding warnings for reason.", MODULE);
                        continue;
                    }

                    // next we're going to store all each product's data in its own transaction, so if one product's data is bad, the others will still get stored
                    TransactionUtil.begin();

                    // store the results and mark this product as processed
                    delegator.storeAll(toStore);

                    // log the import
                    Debug.logInfo("Successfully imported shoppingList for party [" + shoppingListForPartId + "].", MODULE);
                    imported += 1;

                    TransactionUtil.commit();

                } catch (GenericEntityException e) {
                    // if there was an error, we'll just skip this shopping list
                    TransactionUtil.rollback();
                    Debug.logError(e, "Faild to import shoppingList. Error stack follows.", MODULE);
                } catch (Exception e) {
                    TransactionUtil.rollback();
                    Debug.logError(e, "Faild to import shoppingList. Error stack follows.", MODULE);
                }
            }
            importShoppingLists.close();
        } catch (GenericEntityException e) {
            String message = "Cannot import shopping lists: Unable to use delegator to retrieve data from the database.  Error is: " + e.getMessage();
            Debug.logError(e, message, MODULE);
            return ServiceUtil.returnError(message);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("shoppingListsImported", new Integer(imported));
        return results;
    }

    /**
     * Helper method to decode a DataImportShoppingList into a List of GenericValues modeling that the OFBiz schema.
     * If for some reason obtaining data via the delegator fails, this service throws that exception.
     * Note that everything is done with the delegator for maximum efficiency.
     */
    private static List<GenericValue> decodeShoppingList(GenericValue data, Timestamp now, String productStoreId, Delegator delegator) throws GenericEntityException, Exception {
        Map<String, Object> input;
        List<GenericValue> toStore = FastList.newInstance();
        String partyId = data.getString("partyId");
        Debug.logInfo("Now processing shopping list for party [" + partyId + "]", MODULE);

        // check that the party exists
        GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        if (party == null) {
            Debug.logInfo("Could not find party [" + partyId + "], not importing.", MODULE);
            return null;
        }

        // check that the party doesn't have a shopping list already
        List<GenericValue> shoppingList = delegator.findByAnd("ShoppingList", UtilMisc.toMap("partyId", partyId, "shoppingListTypeId", "SLT_SPEC_PURP", "productStoreId", productStoreId));
        if (UtilValidate.isNotEmpty(shoppingList)) {
            Debug.logInfo("Party [" + partyId + "], already has a ShoppingList defined, not importing.", MODULE);
            return null;
        }

        // find the items to import
        // need to get an ELI because of possibly large number of records
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
                        EntityCondition.makeCondition("processedTimestamp", EntityOperator.EQUALS, null)   // leave out previously processed
        );
        TransactionUtil.begin();   // since the service is not inside a transaction, this needs to be in its own transaction, or you'll get a harmless exception
        EntityListIterator importShoppingListItems = delegator.findListIteratorByCondition("DataImportShoppingListItem", conditions, null, null);
        List<GenericValue> shoppingListItems = importShoppingListItems.getCompleteList();
        TransactionUtil.commit();

        // check that there are items to import
        if (UtilValidate.isEmpty(shoppingListItems)) {
             Debug.logInfo("Could not find any item to import in the ShoppingList for [" + partyId + "], not importing.", MODULE);
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
        int itemImported = 0;

        for (GenericValue shoppingListItem : shoppingListItems) {

            // checking that the product exists
            String productId = shoppingListItem.getString("productId");
            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
            if (product == null) {
                Debug.logInfo("Could not find product [" + productId + "], not importing.", MODULE);
                return null;
            }

            // checking the quantity is valid
            Double quantity = null;
            try {
                quantity = Double.valueOf(shoppingListItem.getString("quantity"));
                if (quantity == null || quantity < 0.0) {
                    Debug.logInfo("Invalid quantity [" + quantity + "], not importing.", MODULE);
                    return null;
                }
            } catch (Exception e) {
                Debug.logInfo("Invalid quantity [" + quantity + "], not importing.", MODULE);
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
            itemImported++;
        }
        if (itemImported == 0) {
            Debug.logInfo("No item were imported successfully in the ShoppingList for [" + partyId + "].", MODULE);
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
            return padding.substring(0, paddingLength).concat(seqId);
        } else {
            return seqId;
        }
    }

}
