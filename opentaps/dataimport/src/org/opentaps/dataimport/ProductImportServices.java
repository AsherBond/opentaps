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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Iterator;
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
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.common.util.UtilCommon;

/**
 * Import products via intermediate DataImportProduct entity.
 */
public final class ProductImportServices {

    private ProductImportServices() { }

    private static String MODULE = ProductImportServices.class.getName();
    private static final int DECIMALS = UtilNumber.getBigDecimalScale("order.decimals");
    private static final int ROUNDING = UtilNumber.getBigDecimalRoundingMode("order.rounding");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(DECIMALS, ROUNDING);
    private static int acctgTransSeqNum = 1;

    /**
     * Import products using <code>DataImportProduct</code>.
     * Note that this service is not wrapped in a transaction.
     * Each product record imported is in its own transaction, so it can store as many good records as possible.
     * The goodIdentificationTypeIdN parameters correspond to the type of the customIdN fields in <code>DataImportProduct</code>.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> importProducts(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();

        String goodIdentificationTypeId1 = (String) context.get("goodIdentificationTypeId1");
        String goodIdentificationTypeId2 = (String) context.get("goodIdentificationTypeId2");
        Timestamp now = UtilDateTime.nowTimestamp();

        int imported = 0;

        // main try/catch block that traps errors related to obtaining data from delegator
        try {

            // make sure the supplied goodIdentificationTypes exist
            GenericValue goodIdentificationType1 = null;
            if (UtilValidate.isNotEmpty(goodIdentificationTypeId1)) {
                goodIdentificationType1 = delegator.findByPrimaryKey("GoodIdentificationType", UtilMisc.toMap("goodIdentificationTypeId", goodIdentificationTypeId1));
                if (goodIdentificationType1 == null) {
                    return ServiceUtil.returnError("Cannot import products: goodIdentificationType [" + goodIdentificationTypeId1 + "] does not exist.");
                }
            }
            GenericValue goodIdentificationType2 = null;
            if (UtilValidate.isNotEmpty(goodIdentificationTypeId2)) {
                goodIdentificationType2 = delegator.findByPrimaryKey("GoodIdentificationType", UtilMisc.toMap("goodIdentificationTypeId", goodIdentificationTypeId1));
                if (goodIdentificationType2 == null) {
                    return ServiceUtil.returnError("Cannot import products: goodIdentificationType [" + goodIdentificationTypeId2 + "] does not exist.");
                }
            }

            EntityCondition statusCond = EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_NOT_PROC),
                    EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_FAILED),
                    EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, null));
            // need to get an ELI because of possibly large number of records.  productId <> null will get all records
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("productId", EntityOperator.NOT_EQUAL, null),
                        statusCond   // leave out previously processed products
            );
            TransactionUtil.begin();   // since the service is not inside a transaction, this needs to be in its own transaction, or you'll get a harmless exception
            EntityListIterator importProducts = delegator.findListIteratorByCondition("DataImportProduct", conditions, null, null);
            List<GenericValue> products = importProducts.getCompleteList();
            TransactionUtil.commit();

            for (GenericValue product : products) {

                try {
                    // use the helper method to decode the product into a List of GenericValues
                    // todo this will never be null
                    List<GenericValue> toStore = decodeProduct(product, now, goodIdentificationTypeId1, goodIdentificationTypeId2, delegator);
                    if (toStore == null) {
                        Debug.logWarning("Faild to import product[" + product.get("productId") + "] because data was bad.  Check preceding warnings for reason.", MODULE);
                    }

                    // next we're going to store all each product's data in its own transaction, so if one product's data is bad, the others will still get stored
                    TransactionUtil.begin();

                    // store the results and mark this product as processed
                    delegator.storeAll(toStore);

                    // log the import
                    Debug.logInfo("Successfully imported product [" + product.get("productId") + "].", MODULE);
                    imported += 1;

                    TransactionUtil.commit();

                } catch (GenericEntityException e) {
                    // if there was an error, we'll just skip this product
                    TransactionUtil.rollback();
                    Debug.logError(e, "Failed to import product[" + product.get("productId") + "]. Error stack follows.", MODULE);

                    // store the import error
                    String message = "Failed to import product[" + product.get("productId") + "], Error message : " + e.getMessage();
                    storeImportProductError(product, message, delegator);
                } catch (Exception e) {
                    TransactionUtil.rollback();
                    Debug.logError(e, "Failed to import product[" + product.get("productId") + "]. Error stack follows.", MODULE);

                    // store the import error
                    String message = "Failed to import product[" + product.get("productId") + "], Error message : " + e.getMessage();
                    storeImportProductError(product, message, delegator);
                }
            }
            importProducts.close();

        } catch (GenericEntityException e) {
            String message = "Cannot import products: Unable to use delegator to retrieve data from the database.  Error is: " + e.getMessage();
            Debug.logError(e, message, MODULE);
            return ServiceUtil.returnError(message);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("productsImported", new Integer(imported));
        return results;
    }

    /**
     * Import product inventory using DataImportInventory.
     * The organizationPartyId is the ownerPartyId of the facilityId, and all inventory values in DataImportInventory must
     *   already be in the right currency.  One AcctgTrans plus many entries are created for each facility/ownerPartyId combination.
     *  Both gl accounts must be configured for each owner of the inventory in the warehouse facilities.
     * Note that this service is not wrapped in a transaction.  Each inventory record set imported is in its
     *  own transaction, so it can store as many good records as possible.
     * Will set a ProductFacility with minimumStock of 0 for the facilityId if no minimumStock is supplied and fill in reorderQuantity
     *  and daysToShip as well.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> importProductInventory(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String inventoryGlAccountId = (String) context.get("inventoryGlAccountId");
        String offsettingGlAccountId = (String) context.get("offsettingGlAccountId");

        int totalImported = 0;

        // run the facility-specific data importing routine for each unique facility
        try {
            EntityCondition statusCond = EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_NOT_PROC),
                    EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_FAILED),
                    EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, null));
            // Get a list of distinct facilityIds to import
            EntityListIterator facilitiesIterator = delegator.findListIteratorByCondition("DataImportInventory",
                        statusCond,   // unprocessed records
                        null, // havingEntityConditions
                        UtilMisc.toList("facilityId"),
                        UtilMisc.toList("facilityId"),

                        // This is the key part.  The first true here is for "specifyTypeAndConcur"
                        // the second true is for a distinct select.  Apparently this is the only way the entity engine can do a distinct query
                        new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));
            List<GenericValue> facilities = facilitiesIterator.getCompleteList();
            facilitiesIterator.close();
            for (Iterator<GenericValue> facilitiesIt = facilities.iterator(); facilitiesIt.hasNext();) {
                GenericValue facilityToImport = facilitiesIt.next();
                int importedFromFacility = importInventoryToFacility(facilityToImport.getString("facilityId"), inventoryGlAccountId, offsettingGlAccountId, userLogin, dispatcher);
                Debug.logInfo("Imported [" + importedFromFacility + "] inventory item records into facilityId [" + facilityToImport.getString("facilityId") + "]", MODULE);
                totalImported += importedFromFacility;
            }

        } catch (GenericServiceException se) {
            String errMsg = "Error in importProductInventory service: " + se.getMessage();
            Debug.logError(se, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericEntityException ee) {
            String errMsg = "Error in importProductInventory service: " + ee.getMessage();
            Debug.logError(ee, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("importedRecords", new Integer(totalImported));
        return results;
    }

    /**
     * A helper method to import all the inventory items into a particular facility, using its ownerPartyId and currency.
     * @param facilityId a <code>String</code> value
     * @param inventoryGlAccountId a <code>String</code> value
     * @param offsettingGlAccountId a <code>String</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @return an <code>int</code> value
     * @exception GenericEntityException if an error occurs
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static int importInventoryToFacility(String facilityId, String inventoryGlAccountId, String offsettingGlAccountId, GenericValue userLogin, LocalDispatcher dispatcher) throws GenericEntityException, GenericServiceException {
        Delegator delegator = dispatcher.getDelegator();

        GenericValue facility = delegator.findByPrimaryKeyCache("Facility", UtilMisc.toMap("facilityId", facilityId));

        if (UtilValidate.isEmpty(facility)) {
            String errMsg = "Error in importProductInventory service: facility [" + facilityId + "] does not exist";
            Debug.logError(errMsg, MODULE);
            return 0;
        }

        // Make sure the organizationParty exists
        GenericValue organizationParty = facility.getRelatedOneCache("OwnerParty");
        if (UtilValidate.isEmpty(organizationParty)) {
            String errMsg = "Error in importProductInventory service: owner party for facility [" + facilityId + "] does not exist";
            Debug.logError(errMsg, MODULE);
            return 0;
        }
        String organizationPartyId = organizationParty.getString("partyId");


        // Make sure the supplied GL accounts exist for the organization
        GenericValue glAccountOrganization = null;
        if (UtilValidate.isNotEmpty(inventoryGlAccountId)) {
            glAccountOrganization = delegator.findByPrimaryKeyCache("GlAccountOrganization", UtilMisc.toMap("glAccountId", inventoryGlAccountId, "organizationPartyId", organizationPartyId));
            if (glAccountOrganization == null) {
                Debug.logError("Cannot import inventory: organization [" + organizationPartyId + "] does not have inventory General Ledger account [" + inventoryGlAccountId + "] defined in GlAccountOrganization.", MODULE);
                return 0;
            }
        }
        if (UtilValidate.isNotEmpty(offsettingGlAccountId)) {
            glAccountOrganization = delegator.findByPrimaryKeyCache("GlAccountOrganization", UtilMisc.toMap("glAccountId", offsettingGlAccountId, "organizationPartyId", organizationPartyId));
            if (glAccountOrganization == null) {
                Debug.logError("Cannot import inventory: organization [" + organizationPartyId + "] does not have offsetting General Ledger account [" + offsettingGlAccountId + "] defined in GlAccountOrganization.", MODULE);
                return 0;
            }
        }

        String currencyUomId = UtilCommon.getOrgBaseCurrency(organizationPartyId, delegator);
        if (UtilValidate.isEmpty(currencyUomId)) {
            String errMsg = "Error in importProductInventory service: organization [" + organizationPartyId + "] does not have a baseCurrencyUomId defined in PartyAcctgPref";
            Debug.logError(errMsg, MODULE);
            return 0;
        }

        // All transaction entries have same timestamp
        Timestamp now = UtilDateTime.nowTimestamp();

        // Create the header of an AcctgTrans record for all the subsequent transaction entries
        Map createAcctgTransResults = dispatcher.runSync("createAcctgTrans", UtilMisc.toMap("acctgTransTypeId", "INVENTORY", "glFiscalTypeId", "ACTUAL", "transactionDate", now, "userLogin", userLogin));
        if (ServiceUtil.isError(createAcctgTransResults)) {
            return 0;
        }
        String acctgTransId = (String) createAcctgTransResults.get("acctgTransId");

        EntityCondition statusCond = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_NOT_PROC),
                EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_FAILED),
                EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, null));
        // need to get an ELI because of possibly large number of records.  productId <> null will get all records
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("productId", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                statusCond   // leave out previously processed orders
        );
        TransactionUtil.begin();
        EntityListIterator importProductInventory = delegator.findListIteratorByCondition("DataImportInventory", conditions, null, null);
        List<GenericValue> productInventoryList = importProductInventory.getCompleteList();
        TransactionUtil.commit();

        int imported = 0;
        for (GenericValue productInventory : productInventoryList) {

            try {
                List toStore = decodeInventory(productInventory, organizationPartyId, facilityId, inventoryGlAccountId, offsettingGlAccountId, acctgTransId, currencyUomId, now, delegator);
                if (toStore == null) {
                    Debug.logWarning("Import of product inventory [" + productInventory.get("itemId") + "] was unsuccessful.", MODULE);
                }

                TransactionUtil.begin();

                delegator.storeAll(toStore);

                Debug.logInfo("Successfully imported product inventory [" + productInventory.get("itemId") + "].", MODULE);
                imported++;

                TransactionUtil.commit();

            } catch (GenericEntityException e) {
                TransactionUtil.rollback();
                Debug.logError(e, "Failed to import product inventory [" + productInventory.get("itemId") + "]. Error stack follows.", MODULE);
                // store the import error
                String message = "Failed to import product inventory [" + productInventory.get("itemId") + "]. Error message : " + e.getMessage();
                storeImportInventoryError(productInventory, message, delegator);
            } catch (Exception e) {
                TransactionUtil.rollback();
                Debug.logWarning(e, "Import of product inventory [" + productInventory.get("itemId") + "] was unsuccessful.", MODULE);
                // store the import error
                String message = "Failed to import product inventory [" + productInventory.get("itemId") + "]. Error message : " + e.getMessage();
                storeImportInventoryError(productInventory, message, delegator);
            }

        }
        importProductInventory.close();
        return imported;


    }

    /**
     * Helper method to decode a <code>DataImportProduct</code> into a List of <code>GenericValue</code> modeling that product in the OFBiz schema.
     * If for some reason obtaining data via the delegator fails, this service throws that exception.
     * Note that everything is done with the delegator for maximum efficiency.
     * @param data a <code>GenericValue</code> value
     * @param now a <code>Timestamp</code> value
     * @param goodIdentificationTypeId1 a <code>String</code> value
     * @param goodIdentificationTypeId2 a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> value
     * @exception GenericEntityException if an error occurs
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static List<GenericValue> decodeProduct(GenericValue data, Timestamp now, String goodIdentificationTypeId1, String goodIdentificationTypeId2, Delegator delegator) throws GenericEntityException, Exception {

        Map input;
        List toStore = FastList.newInstance();
        Debug.logInfo("Now processing  data [" + data.get("productId") + "] description [" + data.get("description") + "]", MODULE);

        // product
        input = FastMap.newInstance();
        // check if we should import the product as inactive
        String isInactive = data.getString("isInactive");
        if ("Y".equalsIgnoreCase(isInactive)) {
            input.put("salesDiscontinuationDate", now);
        }
        input.put("productId", data.get("productId"));
        input.put("productTypeId", data.get("productTypeId"));
        input.put("internalName", data.get("internalName"));
        input.put("description", data.get("description"));
        input.put("longDescription", data.get("longDescription"));
        input.put("productName", data.get("productName"));
        input.put("brandName", data.get("brandName"));
        input.put("comments", data.get("comments"));
        input.put("smallImageUrl", data.get("smallImageUrl"));
        input.put("mediumImageUrl", data.get("mediumImageUrl"));
        input.put("largeImageUrl", data.get("largeImageUrl"));

        input.put("productHeight", data.get("height"));
        input.put("heightUomId", data.get("heightUomId"));
        input.put("productDepth", data.get("productLength"));
        input.put("depthUomId", data.get("productLengthUomId"));
        input.put("productWidth", data.get("width"));
        input.put("widthUomId", data.get("widthUomId"));
        input.put("taxable", data.get("taxable"));
        input.put("weight", data.get("weight"));
        input.put("weightUomId", data.get("weightUomId"));
        input.put("isVirtual", "N");
        input.put("isVariant", "N");
        if (data.get("createdDate") != null || data.get("createdDate") != "") {
            input.put("createdDate", data.get("createdDate"));
        } else {
            input.put("createdDate", now);
        }
        GenericValue product = delegator.makeValue("Product", input);
        toStore.add(product);

        // product price
        input = FastMap.newInstance();
        input.put("productId", product.get("productId"));
        input.put("productPriceTypeId", "DEFAULT_PRICE");
        input.put("productPricePurposeId", "PURCHASE");
        input.put("productStoreGroupId", "_NA_");
        if (UtilValidate.isNotEmpty(data.get("priceCurrencyUomId"))) {
           input.put("currencyUomId", data.get("priceCurrencyUomId"));
        } else {
           Debug.logWarning("Product [" + data.get("productId") + "] did not have a price currency, setting to default of [" + UtilProperties.getPropertyValue("general", "currency.uom.id.default") + "]", MODULE);
           input.put("currencyUomId", UtilProperties.getPropertyValue("general", "currency.uom.id.default"));
        }
        input.put("fromDate", now);
        input.put("price", data.get("price"));
        input.put("createdDate", now);
        GenericValue productPrice = delegator.makeValue("ProductPrice", input);
        toStore.add(productPrice);

        // make a list price as well, so that price rules and other features can work
        GenericValue listPrice = delegator.makeValue("ProductPrice", productPrice.getAllFields());
        listPrice.put("productPriceTypeId", "LIST_PRICE");
        toStore.add(listPrice);


        // good identification (this is per customIdN)
        if (!UtilValidate.isEmpty(data.getString("customId1")) && !UtilValidate.isEmpty(goodIdentificationTypeId1)) {
            input = FastMap.newInstance();
            input.put("goodIdentificationTypeId", goodIdentificationTypeId1);
            input.put("productId", product.get("productId"));
            input.put("idValue", data.get("customId1"));
            GenericValue goodIdentification = delegator.makeValue("GoodIdentification", input);
            toStore.add(goodIdentification);
        }
        if (!UtilValidate.isEmpty(data.getString("customId1")) && !UtilValidate.isEmpty(goodIdentificationTypeId2)) {
            input = FastMap.newInstance();
            input.put("goodIdentificationTypeId", goodIdentificationTypeId2);
            input.put("productId", product.get("productId"));
            input.put("idValue", data.get("customId2"));
            GenericValue goodIdentification = delegator.makeValue("GoodIdentification", input);
            toStore.add(goodIdentification);
        }

        // product features (this is per productFeatureN) all of these have type OTHER_FEATURE
        if (!UtilValidate.isEmpty(data.getString("productFeature1"))) {
            String productFeatureId = data.getString("productFeature1");
            productFeatureId = productFeatureId.toUpperCase().replaceAll("\\s", "_");
            GenericValue productFeature = delegator.findByPrimaryKey("ProductFeature", UtilMisc.toMap("productFeatureId", productFeatureId));
            if (productFeature == null) {
                input = FastMap.newInstance();
                input.put("productFeatureId", productFeatureId);
                input.put("description", data.get("productFeature1"));
                input.put("productFeatureTypeId", "OTHER_FEATURE");
                productFeature = delegator.makeValue("ProductFeature", input);
                toStore.add(productFeature);
            }
            input = FastMap.newInstance();
            input.put("productId", product.get("productId"));
            input.put("productFeatureId", productFeatureId);
            input.put("fromDate", now);
            GenericValue productFeatureAppl = delegator.makeValue("ProductFeatureAppl", input);
            toStore.add(productFeatureAppl);
        }


        // insert a purchase record in SupplierProduct
        if (UtilValidate.isNotEmpty(data.get("supplierPartyId"))) {
            String supplierPartyId = data.getString("supplierPartyId");
            input = FastMap.newInstance();
            input.put("productId", product.get("productId"));
            input.put("currencyUomId", UtilProperties.getPropertyValue("general", "currency.uom.id.default"));
            // try to find the supplier
            GenericValue supplier = delegator.findByPrimaryKeyCache("Party", UtilMisc.toMap("partyId", supplierPartyId));
            if (UtilValidate.isEmpty(supplier)) {
                 Debug.logInfo("Supplier with ID [" + supplierPartyId + "] not found, will be creating it", MODULE);

                 TransactionUtil.begin();
                 delegator.create("Party", UtilMisc.toMap("partyId", supplierPartyId, "partyTypeId", "PARTY_GROUP"));
                 delegator.create("PartyGroup", UtilMisc.toMap("partyId", supplierPartyId, "groupName", supplierPartyId));
                 delegator.create("PartyRole", UtilMisc.toMap("partyId", supplierPartyId, "roleTypeId", "SUPPLIER"));
                 TransactionUtil.commit();
            }
            input.put("partyId", supplierPartyId);
            input.put("availableFromDate", now);
            input.put("minimumOrderQuantity", new Double(0.0));
            input.put("supplierPrefOrderId", "10_MAIN_SUPPL");
            input.put("lastPrice", data.get("purchasePrice"));
            input.put("supplierProductId", data.get("productId"));   // vendor part number -- default to our productID for now
            GenericValue supplierProduct = delegator.makeValue("SupplierProduct", input);
            toStore.add(supplierProduct);
        }



        // ##########################################################################

        // mark the entity as processed
        data.set("importStatusId", StatusItemConstants.Dataimport.DATAIMP_IMPORTED);
        data.set("importError", null);
        // update the original data record with a timestamp which also denotes that it has been processed (processedTimestamp = null was original search condition)
        data.set("processedTimestamp", UtilDateTime.nowTimestamp());
        toStore.add(data);

        return toStore;
    }

    /**
     * Helper method to decode a DataImportInventory into a List of GenericValues modeling that product in the OFBiz schema.
     * If for some reason obtaining data via the delegator fails, this service throws that exception.
     * Note that everything is done with the delegator for maximum efficiency.
     * @param productInventory a <code>GenericValue</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param facilityId a <code>String</code> value
     * @param inventoryGlAccountId a <code>String</code> value
     * @param offsettingGlAccountId a <code>String</code> value
     * @param acctgTransId a <code>String</code> value
     * @param currencyUomId a <code>String</code> value
     * @param now a <code>Timestamp</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> value
     * @exception GenericEntityException if an error occurs
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static List<GenericValue> decodeInventory(GenericValue productInventory, String organizationPartyId, String facilityId, String inventoryGlAccountId, String offsettingGlAccountId, String acctgTransId, String currencyUomId, Timestamp now, Delegator delegator) throws GenericEntityException, Exception {
        Map input;
        List toStore = FastList.newInstance();
        Debug.logInfo("Now processing  data [" + productInventory.get("productId") + "]", MODULE);

        String productId = productInventory.getString("productId");
        Double onHand = productInventory.getDouble("onHand");
        if (UtilValidate.isEmpty(onHand)) {
            onHand = new Double(0);
        }

        Double availableToPromise = productInventory.getDouble("availableToPromise");
        if (UtilValidate.isEmpty(availableToPromise)) {
            availableToPromise = new Double(0);
        }

        Double inventoryValue = productInventory.getDouble("inventoryValue");
        if (UtilValidate.isEmpty(inventoryValue)) {
            inventoryValue = new Double(0);
        }

        BigDecimal averageCost = ZERO;
        if (onHand.doubleValue() > 0.0) {
            averageCost = new BigDecimal(inventoryValue.doubleValue() / onHand.doubleValue()).setScale(DECIMALS, ROUNDING);
        }

        // Verify that productId exists
        GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
        if (product == null) {
            Debug.logInfo("Could not find product [" + productId + "], not importing.", MODULE);
            return toStore;
        }

        String inventoryItemId = delegator.getNextSeqId("InventoryItem");

        // Create the inventory item
        input = FastMap.newInstance();
        input.put("inventoryItemId", inventoryItemId);
        input.put("unitCost", new Double(averageCost.doubleValue()));
        input.put("productId", productId);
        input.put("ownerPartyId", organizationPartyId);
        input.put("datetimeReceived", now);
        input.put("facilityId", facilityId);
        input.put("comments", "Auto-generated from Product Inventory Import.");
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        input.put("currencyUomId", currencyUomId);
        GenericValue inventoryItem = delegator.makeValue("InventoryItem", input);
        toStore.add(inventoryItem);

        // Create the inventory item detail
        input = FastMap.newInstance();
        input.put("inventoryItemId", inventoryItemId);
        input.put("inventoryItemDetailSeqId", UtilFormatOut.formatPaddedNumber(1, 4));
        input.put("quantityOnHandDiff", onHand);
        if (UtilValidate.isNotEmpty(availableToPromise)) {
            input.put("availableToPromiseDiff", availableToPromise);
        } else {
            input.put("availableToPromiseDiff", onHand);
        }
        GenericValue inventoryItemDetail = delegator.makeValue("InventoryItemDetail", input);
        toStore.add(inventoryItemDetail);

        // Create the product average cost
        input = FastMap.newInstance();
        input.put("organizationPartyId", organizationPartyId);
        input.put("fromDate", now);
        input.put("averageCost", new Double(averageCost.doubleValue()));
        input.put("productId", productId);
        input.put("productAverageCostId", delegator.getNextSeqId("ProductAverageCost"));
        GenericValue productAverageCost = delegator.makeValue("ProductAverageCost", input);
        toStore.add(productAverageCost);

        // Create the two AcctgTransEntries for this item
        input = FastMap.newInstance();
        input.put("acctgTransId", acctgTransId);
        input.put("acctgTransEntrySeqId", UtilFormatOut.formatPaddedNumber(acctgTransSeqNum, 5));
        input.put("acctgTransEntryTypeId", "_NA_");
        input.put("productId", productId);
        input.put("organizationPartyId", organizationPartyId);
        input.put("currencyUomId", currencyUomId);
        input.put("reconcileStatusId", "AES_NOT_RECONCILED");
        input.put("glAccountId", inventoryGlAccountId);
        input.put("debitCreditFlag", "D");
        input.put("amount", inventoryValue);
        GenericValue debitAcctgTransEntry = delegator.makeValue("AcctgTransEntry", input);
        toStore.add(debitAcctgTransEntry);
        acctgTransSeqNum++;

        input.put("acctgTransEntrySeqId", UtilFormatOut.formatPaddedNumber(acctgTransSeqNum, 5));
        input.put("glAccountId", offsettingGlAccountId);
        input.put("debitCreditFlag", "C");
        GenericValue creditAcctgTransEntry = delegator.makeValue("AcctgTransEntry", input);
        toStore.add(creditAcctgTransEntry);
        acctgTransSeqNum++;

        // Create the ProductFacility record which is now required for viewing inventory reports
        input = FastMap.newInstance();
        input.put("facilityId", facilityId);
        input.put("productId", productId);
        if (UtilValidate.isEmpty(productInventory.get("minimumStock"))) {
            input.put("minimumStock", new Double(0.0));
        } else {
            input.put("minimumStock", productInventory.get("minimumStock"));
        }
        input.put("reorderQuantity", productInventory.get("reorderQuantity"));
        input.put("daysToShip", productInventory.get("daysToShip"));
        toStore.add(delegator.makeValue("ProductFacility", input));

        // Update the original productInventory record with a timestamp which also denotes that it has been processed (processedTimestamp = null was original search condition)
        productInventory.set("processedTimestamp", UtilDateTime.nowTimestamp());
        productInventory.set("importStatusId", StatusItemConstants.Dataimport.DATAIMP_IMPORTED);
        productInventory.set("importError", null); // clear this out in case it had an exception originally
        toStore.add(productInventory);

        return toStore;
    }

    /**
     * Helper method to store import product error to DataImportProduct entities.
     * @param dataImportProduct a <code>GenericValue</code> value
     * @param message a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @exception GenericEntityException if an error occurs
     */
    private static void storeImportProductError(GenericValue dataImportProduct, String message, Delegator delegator) throws GenericEntityException {
        // store the exception and mark as failed
        dataImportProduct.set("importStatusId", StatusItemConstants.Dataimport.DATAIMP_FAILED);
        dataImportProduct.set("processedTimestamp", UtilDateTime.nowTimestamp());
        dataImportProduct.set("importError", message);
        dataImportProduct.store();
    }

    /**
     * Helper method to store import product error to DataImportInventory entities.
     * @param dataImportInventory a <code>GenericValue</code> value
     * @param message a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @exception GenericEntityException if an error occurs
     */
    private static void storeImportInventoryError(GenericValue dataImportInventory, String message, Delegator delegator) throws GenericEntityException {
        // store the exception and mark as failed
        dataImportInventory.set("importStatusId", StatusItemConstants.Dataimport.DATAIMP_FAILED);
        dataImportInventory.set("processedTimestamp", UtilDateTime.nowTimestamp());
        dataImportInventory.set("importError", message);
        dataImportInventory.store();
    }

}
