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

/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.warehouse.manufacturing;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityDateFilterCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.manufacturing.jobshopmgt.ProductionRun;
import org.ofbiz.manufacturing.techdata.TechDataServices;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.workeffort.workeffort.WorkEffortSearch;
import org.opentaps.domain.manufacturing.OpentapsProductionRun;
import org.opentaps.domain.manufacturing.bom.BomNode;
import org.opentaps.domain.manufacturing.bom.BomTree;
import org.opentaps.common.product.UtilProduct;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.warehouse.security.WarehouseSecurity;
import org.opentaps.common.util.UtilCommon;

/**
 * Services for Warehouse application Production Runs.
 *
 * @version    $Rev$
 */
public final class ProductionRunServices {

    private ProductionRunServices() { }

    private static final String MODULE = ProductionRunServices.class.getName();

    private static BigDecimal ZERO = BigDecimal.ZERO;
    public static int decimals = UtilNumber.getBigDecimalScale("arithmetic.properties", "order.decimals");
    public static int rounding = UtilNumber.getBigDecimalRoundingMode("arithmetic.properties", "order.rounding");


    /**
     * Automatically create inventory item-level inventory transfers requests from one facility to another for a productId and quantity to be transferred.
     * Inventory items will be chosen based on the following criteria:
     *  (a) Pick/pack location items will be chosen first
     *  (b) Non serialized items will be chosen before serialized ones
     *  (c) They will be ordered by locationSeqId
     *  (d) The inventory item with the lowest ATP will be used first
     * Inventory items with ATP less than 0 will not be chosen.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createInventoryTransferForFacilityProduct(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String facilityIdFrom = (String) context.get("facilityIdFrom");
        String facilityIdTo = (String) context.get("facilityIdTo");
        String productId = (String) context.get("productId");
        Timestamp sendDate = (Timestamp) context.get("sendDate");
        BigDecimal transferQuantity = (BigDecimal) context.get("transferQuantity");
        BigDecimal quantityTransferred = BigDecimal.ZERO;
        List<String> inventoryTransferIds = new LinkedList<String>();

        if (transferQuantity.compareTo(BigDecimal.ZERO) < 0) {
            return UtilMessage.createAndLogServiceFailure("Request to transfer quantity [" + transferQuantity + "] of [" + productId + "] from [" + facilityIdFrom + "] to [" + facilityIdTo + "] cannot be completed because you cannot transfer a quantity less than zero", MODULE);
        }

        try {
            EntityCondition inventoryItemConds = EntityCondition.makeCondition(
                    EntityCondition.makeCondition("availableToPromiseTotal", EntityOperator.GREATER_THAN, BigDecimal.ZERO),
                    EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityIdFrom),
                    EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
            //The last parameter is very important -- it is the sequence of inventory items to be used.  See service definition for details.
            List<GenericValue> inventoryItemList = delegator.findByCondition("InventoryItemAndLocation", inventoryItemConds, null,
                    UtilMisc.toList("-locationTypeEnumId", "inventoryItemTypeId", "locationSeqId", "-availableToPromiseTotal"));

            if (UtilValidate.isEmpty(inventoryItemList)) {
                return UtilMessage.createAndLogServiceFailure("Product [" + productId + "] is not found in [" + facilityIdFrom + "] inventory and a transfer cannot be completed", MODULE);
            }

            Iterator<GenericValue> inventoryItemIt = inventoryItemList.iterator();
            while (inventoryItemIt.hasNext()) {
                GenericValue inventoryItem = inventoryItemIt.next();
                BigDecimal availableToPromiseTotal = inventoryItem.getBigDecimal("availableToPromiseTotal");
                BigDecimal xferQty = new BigDecimal("0.0");
                if (availableToPromiseTotal.compareTo(transferQuantity) >= 0) {
                    xferQty = transferQuantity;
                } else {
                    xferQty = availableToPromiseTotal;
                }
                try {
                    Map tmpInputMap = UtilMisc.toMap("inventoryItemId", inventoryItem.getString("inventoryItemId"),
                            "facilityId", facilityIdFrom,
                            "facilityIdTo", facilityIdTo,
                            "xferQty", xferQty,
                            "sendDate", sendDate);
                    tmpInputMap.put("statusId", "IXF_REQUESTED");
                    tmpInputMap.put("userLogin", userLogin);
                    Map tmpResults = dispatcher.runSync("createInventoryTransfer", tmpInputMap);
                    if (ServiceUtil.isError(tmpResults) || ServiceUtil.isFailure(tmpResults)) { return tmpResults; }
                    inventoryTransferIds.add((String) tmpResults.get("inventoryTransferId"));
                } catch (GenericServiceException e) {
                    return UtilMessage.createAndLogServiceError("Problem running the createInventoryTransferForFacilityProduct service: " + e.getMessage(), MODULE);
                }
                transferQuantity = transferQuantity.subtract(xferQty);
                quantityTransferred = quantityTransferred.add(xferQty);
                if (transferQuantity.compareTo(BigDecimal.ZERO) == 0) {
                    break;
                }
            }
            if (transferQuantity .compareTo(BigDecimal.ZERO) > 0) {
                return UtilMessage.createAndLogServiceFailure("Quantity [" + transferQuantity + "] was requested to be transferred from [" + facilityIdFrom + "] but [" + transferQuantity + "] could not be matched against actual inventory", MODULE);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return UtilMessage.createAndLogServiceError("Problem running the autoCreateInventoryTransfers service" + e.getMessage(), MODULE);
        }
        Map result = ServiceUtil.returnSuccess();
        result.put("quantityTransferred", quantityTransferred);
        result.put("inventoryTransferIds", inventoryTransferIds);
        return result;
    }

    /**
     * Auto create inventory transfers for parts needed by production runs during a time period, taking into account
     *  existing ATP inventory at the production facility and planned inventory transfers.
     * Inventory transfers will be from warehouseFacilityId to productionFacilityId.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> autoCreateInventoryTransfers(DispatchContext ctx, Map<String, Object> context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String warehouseFacilityId = (String) context.get("warehouseFacilityId");
        String productionFacilityId = (String) context.get("productionFacilityId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");

        Map products = FastMap.newInstance();

        try {
            // first find created, scheduled, and confirmed (DOC_PRINTED) production runs in the production facility and the parts required for them
            // in the WorkEffortAndGoods entity.  Create a Map of productId -> totalQuantity which is the quantity of parts needed for these runs
            EntityCondition findOutgoingProductionRunsStatusConds = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_CREATED"),
                EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_SCHEDULED"),
                EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_DOC_PRINTED"));

            EntityCondition findOutgoingProductionRunsConds = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUNT_PROD_NEEDED"),
                EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "WEGS_CREATED"),
                EntityCondition.makeCondition("estimatedStartDate", EntityOperator.GREATER_THAN, fromDate),
                EntityCondition.makeCondition("estimatedStartDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate),
                findOutgoingProductionRunsStatusConds);

            List<GenericValue> resultList = delegator.findByCondition("WorkEffortAndGoods", findOutgoingProductionRunsConds, null, UtilMisc.toList("-estimatedStartDate"));

            Iterator<GenericValue> iteratorResult = resultList.iterator();
            while (iteratorResult.hasNext()) {
                GenericValue genericResult = iteratorResult.next();
                BigDecimal estimatedQuantity = genericResult.getBigDecimal("estimatedQuantity");
                if (estimatedQuantity == null) {
                    estimatedQuantity = BigDecimal.ZERO;
                }
                String productId =  genericResult.getString("productId");
                if (!products.containsKey(productId)) {
                    products.put(productId, BigDecimal.ZERO);
                }
                BigDecimal totalQuantity = (BigDecimal) products.get(productId);
                totalQuantity = totalQuantity.add(estimatedQuantity);
                products.put(productId, totalQuantity);
            }
            Iterator productsIt = products.keySet().iterator();

            // Now deduct from the Map of productId -> quantity of parts needed the available-to-promise total of that part already in the production warehouse
            // and any planned inventory transfers (requested, scheduled, en route) which are coming into the production warehouse for that product
            while (productsIt.hasNext()) {
                String productId = (String) productsIt.next();
                BigDecimal totalQuantity = (BigDecimal) products.get(productId);  // total quantity required for production run
                double existingAtp = 0.0;
                try {
                    Map tmpResults = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", productionFacilityId, "userLogin", userLogin));
                    if (tmpResults.get("availableToPromiseTotal") != null) {
                        existingAtp = ((BigDecimal) tmpResults.get("availableToPromiseTotal")).doubleValue();
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Error counting inventory, assuming qoh = 0 for product [" + productId + "] in facility [" + productionFacilityId + "].", MODULE);
                }
                // get the outstanding transfers
                EntityCondition transferStatusConds = EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition("transferStatusId", EntityOperator.EQUALS, "IXF_REQUESTED"),
                    EntityCondition.makeCondition("transferStatusId", EntityOperator.EQUALS, "IXF_SCHEDULED"),
                    EntityCondition.makeCondition("transferStatusId", EntityOperator.EQUALS, "IXF_EN_ROUTE"));

                EntityCondition transferConds = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, warehouseFacilityId),
                    EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, productionFacilityId),
                    EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                    transferStatusConds);
                List<GenericValue> transferList = delegator.findByCondition("InventoryTransferAndItem", transferConds, null, UtilMisc.toList("inventoryTransferId"));
                Iterator<GenericValue> transferIt = transferList.iterator();
                double transferQuantityTotal = 0.0;
                while (transferIt.hasNext()) {
                    GenericValue transfer = transferIt.next();
                    double transferQuantity = 0.0;
                    if ("NON_SERIAL_INV_ITEM".equals(transfer.getString("inventoryItemTypeId"))) {
                        transferQuantity = transfer.getDouble("quantityOnHandTotal").doubleValue();
                    } else {
                        transferQuantity = 1;
                    }
                    transferQuantityTotal += transferQuantity;
                }
                BigDecimal netRequiredQuantity = new BigDecimal(totalQuantity.doubleValue() - existingAtp - transferQuantityTotal);   // quantity required after considering ATP and planned inventory transfers
                products.put(productId, netRequiredQuantity);
            }

            productsIt = products.keySet().iterator();
            while (productsIt.hasNext()) {
                String productId = (String) productsIt.next();
                BigDecimal netRequiredQuantity = ((BigDecimal) products.get(productId));
                if (netRequiredQuantity.compareTo(BigDecimal.ZERO) > 0) {
                    dispatcher.runSync("createInventoryTransferForFacilityProduct",
                            UtilMisc.toMap("productId", productId, "facilityIdFrom", warehouseFacilityId,
                            "facilityIdTo", productionFacilityId, "transferQuantity", netRequiredQuantity,
                            "userLogin", userLogin));

                }
            }

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Problem running the autoCreateInventoryTransfers service: " + e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError("Problem running the autoCreateInventoryTransfers service: " + e.getMessage());
        }
        return result;
    }

    /**
     * Create a production run for a product Bill of Materials.
     * This services overrides the ofbiz service <code>createProductionRunsForProductBom</code> to support multiple BOM.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createProductionRunsForProductBom(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productId = (String) context.get("productId");
        Timestamp startDate = (Timestamp) context.get("startDate");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        String facilityId = (String) context.get("facilityId");
        String workEffortName = (String) context.get("workEffortName");
        String description = (String) context.get("description");
        String routingId = (String) context.get("routingId");
        String workEffortId = null;

        // default quantity to produce to 1.0
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }

        try {
            List components = new ArrayList();
            BomTree tree = new BomTree(productId, "MANUF_COMPONENT", startDate, BomTree.EXPLOSION_MANUFACTURING, routingId, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.setRootAmount(BigDecimal.ZERO);
            Debug.logInfo("Debugging BomTree, for product [" + productId + "] and routing [" + routingId + "]", MODULE);
            tree.debug();
            tree.print(components);
            workEffortId = tree.createManufacturingOrders(facilityId, startDate, workEffortName, description, routingId, null, null, null, userLogin);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError("Error creating bill of materials tree: " + gee.getMessage());
        }
        if (workEffortId == null) {
            return ServiceUtil.returnError("No production run is required for product with id [" + productId + "] in date [" + startDate + "]; please verify the validity dates of the bill of materials and routing.");
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("productionRuns" , new ArrayList());
        result.put("productionRunId" , workEffortId);
        return result;
    }

    /**
     * Create a production run. This handles the creation of outsourced tasks and their requirements.
     * Requires WRHS_MFG_CREATE permission for the facility.
     *
     * NOTE:  Outsourced tasks are added only for the first node of the BOM tree.  That is, if you create a production
     *  run for PROD_COST which is composed of MAT_A_COST and MAT_A_COST has its own production run, only the
     *  PROD_COST production run itself can have outsourced tasks.  We can improve this in the future if it's actually
     *  needed, all we need to do is move most of this code to createProductionRunRefactored().
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createProductionRun(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String facilityId = (String) context.get("facilityId");
        WarehouseSecurity wsecurity = new WarehouseSecurity(security, userLogin, facilityId);

        if (!wsecurity.hasFacilityPermission("WRHS_MFG_CREATE") && !"Y".equals(userLogin.getString("isSystem"))) {
            return ServiceUtil.returnError(UtilMessage.getPermissionDeniedError(locale));
        }

        String productId = (String) context.get("productId");
        boolean disassemble = (context.get("disassemble") == null ? false : (Boolean) context.get("disassemble"));
        try {
            Map input;
            ModelService productionRunService;
            if (!disassemble) {
                // if we're assembling, then create the tree of production runs
                productionRunService = dctx.getModelService("createProductionRunsForProductBom");
                input = productionRunService.makeValid(context, "IN");
            } else {
                // otherwise run createProductionRun directly to disassemble the product (no tree support for this yet, would require refactoring BOMTree)
                productionRunService = dctx.getModelService("createProductionRun");
                input = productionRunService.makeValid(context, "IN");
                input.put("pRQuantity", context.get("quantity"));
            }

            // create our production run (or tree of runs)
            Map results = dispatcher.runSync(productionRunService.name, input);
            if (ServiceUtil.isError(results)) {
                return UtilMessage.createAndLogServiceError(results, "WarehouseError_CannotCreateProductionRun", locale, MODULE);
            }
            String productionRunId = (String) results.get("productionRunId");

            /*
             * The above service creates the outsource task from a template, but it does not create the
             * WorkEffortGoodStandards to link the outsource task to the outsource task products.  Since this is a
             * flat relationship, unlike the BOM tree, all we have to do is create these links directly
             * from the template.  However, this process is rather involved.  Briefly:
             *
             * 1.  Have the set of all tasks that belong to this particular production run
             * 2.  Have the set of all *outsourced* tasks for *all* production runs on this product
             * 3.  The intersection of these two sets are the outsourced tasks belonging to the production run
             * 4.  For each WorkEffortStandardGood template of type outsourced product, create an instance for the tasks identified by #3
             * 5.  Create requirements for each task from #3 linked to instance product from #4
             *
             * TODO: This can be moved into createProductionRunRefactored()
             */

            // get the production run template for this product.  If a routingId was passed in, then that's it, otherwise we use the first one found, similar to the ofbiz service
            GenericValue productionRunTempl = null;
            String routingId = (String) context.get("routingId");
            String templateId = "DEFAULT_ROUTING";   // default in case no routing is found
            if (UtilValidate.isNotEmpty(routingId)) {
                productionRunTempl = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", routingId));
            } else {
                List<EntityCondition> conditions = Arrays.asList(
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "ROU_PROD_TEMPLATE"),
                        EntityUtil.getFilterByDateExpr()
                );
                productionRunTempl = EntityUtil.getFirst(delegator.findByAnd("WorkEffortGoodStandard", conditions));
            }
            if (productionRunTempl == null) {
                Debug.logWarning(UtilMessage.expandLabel("WarehouseError_CannotFindProductionRunTemplate", locale, UtilMisc.toMap("productId", productId, "defaultRoutingId", templateId)), MODULE);
            } else {
                templateId = productionRunTempl.getString("workEffortId");
            }
            // get the routing components of the template
            List<GenericValue> routingComponents = delegator.findByAnd("WorkEffortAssoc", UtilMisc.toMap("workEffortIdFrom", templateId, "workEffortAssocTypeId", "ROUTING_COMPONENT"));
            List routingComponentIds = EntityUtil.getFieldListFromEntityList(routingComponents, "workEffortIdTo", true);

            // the outsourced products for this template are the WorkEffortGoodStandards of type ROU_OUTSOURCE_PROD where workEffortId is in routingComponentsId
            List<EntityCondition> conditions = Arrays.asList(
                    EntityCondition.makeCondition("workEffortId", EntityOperator.IN, routingComponentIds),
                    EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "ROU_OUTSOURCE_PROD"),
                    EntityUtil.getFilterByDateExpr()
            );
            List<GenericValue> templateProducts = delegator.findByAnd("WorkEffortGoodStandard", conditions);

            // We'll need a list of all the taskIds for this production run
            GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
            List tasks = productionRun.getRelated("ChildWorkEffort");
            List taskIds = EntityUtil.getFieldListFromEntityList(tasks, "workEffortId", true);

            /*
             * Next, we're going to create an instance of these templateProducts for the outsourced tasks in the new
             * production run.  We rely on the fact that the outsourced tasks are associated with their template
             * definitions in WorkEffortAssoc.  All we have to do is cherry pick the ones that belong to our production run.
             */

            // loop through the outsource task template definitions
            int created = 0;
            for (Iterator<GenericValue> iter = templateProducts.iterator(); iter.hasNext();) {
                GenericValue templateProduct = iter.next();
                String templateWorkEffortId = templateProduct.getString("workEffortId");
                String templateProductId = templateProduct.getString("productId");
                BigDecimal templateQuantity = (templateProduct.get("estimatedQuantity") == null ? new BigDecimal("1.0") : templateProduct.getBigDecimal("estimatedQuantity"));

                // get the supplier for this product (used later for requirement)
                results = dispatcher.runSync("getSuppliersForProduct", UtilMisc.toMap("productId", templateProductId, "quantity", templateQuantity));
                if (ServiceUtil.isError(results)) {
                    return UtilMessage.createAndLogServiceError(results, "WarehouseError_CannotCreateProductionRun", locale, MODULE);
                }
                List supplierProducts = (List) results.get("supplierProducts");
                if (supplierProducts == null || supplierProducts.size() == 0) {
                    String errorMsg = UtilMessage.expandLabel("WarehouseError_CannotCreateProductionRun", locale);
                    errorMsg += " " + UtilMessage.expandLabel("OpentapsError_NoSuppliersForProductAndQty", locale, UtilMisc.toMap("productId", templateProductId, "quantity", templateProduct));
                    return ServiceUtil.returnError(errorMsg);
                }
                GenericValue supplierProduct = EntityUtil.getFirst(supplierProducts);

                // loop through all instances of outsourced tasks for this definition
                List<GenericValue> assocs = delegator.findByAnd("WorkEffortAssoc", UtilMisc.toMap("workEffortIdFrom", templateWorkEffortId, "workEffortAssocTypeId", "WORK_EFF_TEMPLATE"));
                for (Iterator<GenericValue> assocIter = assocs.iterator(); assocIter.hasNext();) {
                    GenericValue assoc = assocIter.next();
                    GenericValue outsourcedTask = assoc.getRelatedOne("ToWorkEffort");
                    String outsourcedTaskId = assoc.getString("workEffortIdTo");

                    // skip this if not part of our production run
                    if (!taskIds.contains(outsourcedTaskId)) {
                        continue;
                    }

                    // create an instance
                    results = createOutsourcedTaskInstance(dctx, context, outsourcedTask, templateQuantity, productionRun, supplierProduct);
                    if (ServiceUtil.isError(results)) {
                        return UtilMessage.createAndLogServiceError(results, "WarehouseError_CannotCreateProductionRun", locale, MODULE);
                    }

                    created += 1;
                }
            }

            // just a helpful message if no outsourced requirements were created
            if (created == 0) {
                String message = UtilMessage.expandLabel("WarehouseNoOutsourcedRequirementsForProductionRun", locale, UtilMisc.toMap("productionRunId", productionRunId));
                Debug.logInfo(message, MODULE);
            }

            results = ServiceUtil.returnSuccess();
            results.put("productionRunId", productionRunId);
            return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map createOutsourcedTaskInstance(DispatchContext dctx, Map<String, Object> context,
            GenericValue outsourcedTask, BigDecimal templateQuantity, GenericValue productionRun, GenericValue supplierProduct)
            throws GenericEntityException, GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Map<String, Object> input;
        Map results;
        String productId = supplierProduct.getString("productId");

        // The WorkEffortGoodStandard.estimatedQuantity is task.quantityToProduce * template.estimatedQuantity?default(1)
        BigDecimal quantityToProduce = outsourcedTask.getBigDecimal("quantityToProduce"); // guaranteed not null
        BigDecimal quantityTotal = quantityToProduce.multiply(templateQuantity);

        input = FastMap.newInstance();
        input.put("workEffortId", outsourcedTask.get("workEffortId"));
        input.put("productId", productId);
        input.put("workEffortGoodStdTypeId", "PRUN_OUTSRC_PURCH");
        input.put("statusId", "WEGS_CREATED");
        input.put("estimatedQuantity", quantityTotal);
        input.put("fromDate", productionRun.get("estimatedStartDate"));
        input.put("userLogin", userLogin);
        results = dispatcher.runSync("createWorkEffortGoodStandard", input);
        if (ServiceUtil.isError(results)) {
            return results;
        }

        // Also create a product requirement for this quantity
        input = FastMap.newInstance();
        input.put("requirementTypeId", "PRODUCT_REQUIREMENT"); // WARNING: If this were a INTERNAL_REQUIREMENT, it would trigger create production run.
        input.put("facilityId", outsourcedTask.get("facilityId"));
        input.put("productId", productId);
        input.put("description", UtilMessage.expandLabel("WarehouseOutsourcedRequirementDescription", locale, UtilMisc.toMap("productionRunId", productionRun.get("workEffortId"))));
        input.put("quantity", quantityTotal);
        input.put("statusId", "REQ_CREATED");
        input.put("orderItemTypeId", "MFG_CONTRACT"); // NOTE: We extended the Requirement entity to let it know what kind of orderItemTypeId to use
        input.put("requirementStartDate", productionRun.get("estimatedStartDate"));
        input.put("requiredByDate", productionRun.get("estimatedCompletionDate"));
        input.put("userLogin", userLogin);
        results = dispatcher.runSync("createRequirement", input);
        if (ServiceUtil.isError(results)) {
            return results;
        }
        String requirementId = (String) results.get("requirementId");

        // Create a RequirementRole between requirementId and supplier partyId
        input = FastMap.newInstance();
        input.put("requirementId", requirementId);
        input.put("partyId", supplierProduct.get("partyId"));
        input.put("roleTypeId", "SUPPLIER");
        List<GenericValue> requirementRoles = EntityUtil.filterByDate(delegator.findByAnd("RequirementRole", input));
        if (requirementRoles.size() == 0) {
            input.put("userLogin", userLogin);
            results = dispatcher.runSync("createRequirementRole", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
        }

        // finally associate the requirement to this task (creating this by hand to avoid legacy landmines)
        GenericValue workRequirement = delegator.makeValue("WorkRequirementFulfillment", UtilMisc.toMap("requirementId", requirementId, "workEffortId", outsourcedTask.get("workEffortId")));
        workRequirement.create();

        return ServiceUtil.returnSuccess();
    }

    /**
     * This is run after the service in ofbiz manufacturing of the same name to create instance data for outsourced tasks.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> addProductionRunRoutingTask(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);

        // this task is the new one created from a template, so we will have to find the template this is associated with
        String routingTaskId = (String) context.get("routingTaskId");
        try {
            GenericValue outsourcedTask = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", routingTaskId));
            GenericValue productionRun = outsourcedTask.getRelatedOne("ParentWorkEffort");

            List<EntityCondition> conditions = Arrays.asList(
                    EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, routingTaskId),
                    EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "ROU_OUTSOURCE_PROD"),
                    EntityUtil.getFilterByDateExpr()
            );
            List<GenericValue> templateProducts = delegator.findByAnd("WorkEffortGoodStandard", conditions);

            for (Iterator<GenericValue> iter = templateProducts.iterator(); iter.hasNext();) {
                GenericValue templateProduct = iter.next();
                String templateProductId = templateProduct.getString("productId");
                BigDecimal templateQuantity = (templateProduct.get("estimatedQuantity") == null ? BigDecimal.ONE : templateProduct.getBigDecimal("estimatedQuantity"));

                // get the supplier for this product (used later for requirement)
                Map results = dispatcher.runSync("getSuppliersForProduct", UtilMisc.toMap("productId", templateProductId, "quantity", templateQuantity));
                if (ServiceUtil.isError(results)) {
                    return UtilMessage.createAndLogServiceError(results, "WarehouseError_CannotAddRoutingTask", locale, MODULE);
                }
                List supplierProducts = (List) results.get("supplierProducts");
                if (supplierProducts == null || supplierProducts.size() == 0) {
                    String errorMsg = UtilMessage.expandLabel("WarehouseError_CannotCreateProductionRun", locale);
                    errorMsg += " " + UtilMessage.expandLabel("OpentapsError_NoSuppliersForProductAndQty", locale, UtilMisc.toMap("productId", templateProductId, "quantity", templateQuantity));
                    return ServiceUtil.returnError(errorMsg);
                }
                GenericValue supplierProduct = EntityUtil.getFirst(supplierProducts);

                // create an instance
                results = createOutsourcedTaskInstance(dctx, context, outsourcedTask, templateQuantity, productionRun, supplierProduct);
                if (ServiceUtil.isError(results)) {
                    return UtilMessage.createAndLogServiceError(results, "WarehouseError_CannotAddRoutingTask", locale, MODULE);
                }
            }

            Map results = ServiceUtil.returnSuccess();
            results.put("routingTaskId", routingTaskId);
            return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Approve all outsourced product requirements for the given productionRunId.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> approveOutsourcedProductRequirements(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");
        try {
            GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
            List<GenericValue> tasks = productionRun.getRelated("ChildWorkEffort");
            List taskIds = EntityUtil.getFieldListFromEntityList(tasks, "workEffortId", true);

            List<GenericValue> assocs = delegator.findByAnd("WorkRequirementFulfillment", EntityCondition.makeCondition("workEffortId", EntityOperator.IN, taskIds));
            for (Iterator<GenericValue> iter = assocs.iterator(); iter.hasNext();) {
                GenericValue assoc = iter.next();
                GenericValue requirement = assoc.getRelatedOne("Requirement");
                String statusId = requirement.getString("statusId");
                if ("REQ_CREATED".equals(statusId) || "REQ_PROPOSED".equals(statusId)) {
                    Map results = dispatcher.runSync("approveRequirement", UtilMisc.toMap("userLogin", userLogin, "requirementId", requirement.get("requirementId")));
                    if (ServiceUtil.isError(results)) {
                        return UtilMessage.createAndLogServiceError(results, "WarehouseError_CannotUpdateProductionRun", locale, MODULE);
                    }
                }
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Marks all outsourced tasks belonging to the production run as PRUN_OUTSRC_PEND.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> setOutsourcedTasksToPending(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        String productionRunId = (String) context.get("productionRunId");
        try {
            // get the created, scheduled or confirmed tasks that need to be changed to pending
            EntityCondition conditions = EntityCondition.makeCondition(
                    EntityCondition.makeCondition("workEffortParentId", EntityOperator.EQUALS, productionRunId),
                    EntityCondition.makeCondition(EntityOperator.OR,
                            EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_CREATED"),
                            EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_PROPOSED"),
                            EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_DOC_PRINTED"),
                            EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_SCHEDULED"))
            );
            List<GenericValue> tasks = delegator.findByAnd("WorkEffort", conditions);
            if (UtilValidate.isEmpty(tasks)) {
                return ServiceUtil.returnFailure("No tasks found for production run [" + productionRunId + "] so none will be set to PO Pending");
            }
            List taskIds = EntityUtil.getFieldListFromEntityList(tasks, "workEffortId", true);

            // of these tasks, choose only the ones that are production tasks by seeing if they have a WEGS with PRUN_OUTSRC_PURCH
            conditions = EntityCondition.makeCondition(
                    EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUN_OUTSRC_PURCH"),
                    EntityCondition.makeCondition("workEffortId", EntityOperator.IN, taskIds));
            List<GenericValue> wegsList = delegator.findByAnd("WorkEffortGoodStandard", conditions);
            for (Iterator<GenericValue> iter = wegsList.iterator(); iter.hasNext();) {
                GenericValue wegs = iter.next();
                GenericValue task = wegs.getRelatedOne("WorkEffort");
                task.set("currentStatusId", "PRUN_OUTSRC_PEND");
                task.store();
                Debug.logInfo("Outsourced task [" + task.get("workEffortId") + "] for production run [" + productionRunId + "] has been marked pending.", MODULE);
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * If all tasks in a production run are outsourced and pending, then this marks the production run as running.
     * This step is required so that the production run can then be completed when the tasks are received.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> runOutsourcedProductionRun(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");
        try {
            // make sure the production run is confirmed, because this is when the outsourced tasks are marked pending (this should be guaranteed by SECA, I'm just being paranoid here)
            GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
            if (!"PRUN_DOC_PRINTED".equals(productionRun.get("currentStatusId"))) {
                Debug.logInfo("Checking Outsourced Tasks: Production run [" + productionRunId + "] cannot be marked running yet, it is currently in status [" + productionRun.get("statusId") + "].", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // count the number of pending outsourced tasks, if it's equal to the total tasks, then we can mark as running
            List<GenericValue> tasks = productionRun.getRelated("ChildWorkEffort");
            int totalTasks = tasks.size();
            List<GenericValue> pendingTasks = EntityUtil.filterByAnd(tasks, UtilMisc.toMap("currentStatusId", "PRUN_OUTSRC_PEND"));
            if (pendingTasks.size() == totalTasks) {
                Map input = UtilMisc.toMap("userLogin", userLogin, "productionRunId", productionRunId, "statusId", "PRUN_RUNNING");
                Map results = dispatcher.runSync("changeProductionRunStatus", input);
                if (ServiceUtil.isError(results)) {
                    return UtilMessage.createAndLogServiceError(results, MODULE);
                }
                Debug.logInfo("Checking Outsourced Tasks: Production run [" + productionRunId + "] has been marked running because all tasks are outsourced and pending.", MODULE);
            } else {
                Debug.logInfo("Checking Outsourced Tasks: Production run [" + productionRunId + "] cannot be marked running yet, it has non-outsourced tasks.", MODULE);
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Cancel any unprocessed outsourced product requirements and order items.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> cancelProductionRun(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");
        try {
            GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
            List tasks = productionRun.getRelated("ChildWorkEffort");
            List taskIds = EntityUtil.getFieldListFromEntityList(tasks, "workEffortId", true);

            // cancel any requirements
            List conditions = UtilMisc.toList(EntityCondition.makeCondition("workEffortId", EntityOperator.IN, taskIds));
            List<GenericValue> assocs = delegator.findByAnd("WorkRequirementFulfillment", conditions);
            for (Iterator<GenericValue> iter = assocs.iterator(); iter.hasNext();) {
                GenericValue assoc = iter.next();
                GenericValue requirement = assoc.getRelatedOne("Requirement");
                String statusId = requirement.getString("statusId");
                if ("REQ_CREATED".equals(statusId) || "REQ_PROPOSED".equals(statusId) || "REQ_APPROVED".equals(statusId)) {
                    Map results = dispatcher.runSync("purchasing.cancelRequirement", UtilMisc.toMap("userLogin", userLogin, "requirementId", requirement.get("requirementId")));
                    if (ServiceUtil.isError(results)) {
                        return UtilMessage.createAndLogServiceError(results, "WarehouseError_CannotUpdateProductionRun", locale, MODULE);
                    }
                }
            }

            // cancel any order items not yet approved
            // TODO: what about the quantityFulfilled field in this?
            assocs = delegator.findByAnd("WorkOrderItemFulfillment", conditions);
            for (Iterator<GenericValue> iter = assocs.iterator(); iter.hasNext();) {
                GenericValue assoc = iter.next();
                GenericValue item = assoc.getRelatedOne("OrderItem");
                if ("ITEM_CREATED".equals(item.get("statusId"))) {
                    Map results = dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("userLogin", userLogin, "orderId", item.get("orderId"), "orderItemSeqId", item.get("orderItemSeqId"), "statusId", "ITEM_CANCELLED"));
                    if (ServiceUtil.isError(results)) {
                        return UtilMessage.createAndLogServiceError(results, "WarehouseError_CannotUpdateProductionRun", locale, MODULE);
                    }
                }
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Sets the status of outsourced production tasks to PRUN_OUTSRCD, which allows them to be completed when the related PO is received.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> qualifyOutsourcedTasks(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        // this service sould be triggered by a seca that checks that this is an approved PURCHASE_ORDER
        String orderId = (String) context.get("orderId");
        try {
            List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemTypeId", "MFG_CONTRACT", "statusId", "ITEM_APPROVED"));
            for (Iterator<GenericValue> iter = orderItems.iterator(); iter.hasNext();) {
                GenericValue item = iter.next();
                List<GenericValue> assocs = item.getRelated("WorkOrderItemFulfillment");
                List tasks = EntityUtil.getRelatedByAnd("WorkEffort", UtilMisc.toMap("currentStatusId", "PRUN_OUTSRC_PEND"), assocs);
                for (Iterator<GenericValue> taskIter = tasks.iterator(); taskIter.hasNext();) {
                    GenericValue task = taskIter.next();

                    // now we have to make sure this task is an outsourced product by looking up its WorkEffortGoodStandard
                    List conditions = UtilMisc.toList(
                        EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, task.get("workEffortId")),
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, item.get("productId")),
                        EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUN_OUTSRC_PURCH"),
                        EntityUtil.getFilterByDateExpr()
                    );
                    List wegs = delegator.findByAnd("WorkEffortGoodStandard", conditions);
                    if (wegs.size() > 0) {
                        task.set("currentStatusId", "PRUN_OUTSRCD");
                        task.store();
                    }
                }
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }


    /**
     * Calculate the cost of the outsourced task from the purchase order using WorkOrderFulfillment.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> calcOutSourcedTaskCost(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue workEffort = (GenericValue) context.get("workEffort");
        GenericValue workEffortCostCalc = (GenericValue) context.get("workEffortCostCalc");
        GenericValue costComponentCalc = (GenericValue) context.get("costComponentCalc");
        try {
            // Calculate the cost of the outsourced task from the purchase order using WorkOrderFulfillment
            // what are the right order items?  the item should be completed, or the production run task would not be completed
            // the order status could be approved or completed--usually it should be approved but the last item could cause the order to be completed
            List<GenericValue> workEffortAndOrderItems = delegator.findByAnd("WorkOrderAndOrderItem", UtilMisc.toList(
                    EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffort.getString("workEffortId")),
                    EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "PURCHASE_ORDER"),
                    EntityCondition.makeCondition("orderStatusId", EntityOperator.IN, UtilMisc.toList("ORDER_APPROVED", "ORDER_COMPLETED")),
                    EntityCondition.makeCondition("orderItemTypeId", EntityOperator.EQUALS, "MFG_CONTRACT"),
                    EntityCondition.makeCondition("itemStatusId", EntityOperator.EQUALS, "ITEM_COMPLETED")));
            if (UtilValidate.isEmpty(workEffortAndOrderItems)) {
                return ServiceUtil.returnFailure("No purchase order items found for [" + workEffort.get("workEffortId") + "], will not be able to calculate costs");
            }

            // because we are just creating usually just one purchase order item per workeffort (outsourcing a task is workeffort -> PO item only)
            // this should suffice but for more generalized use we may have to loop through both sides to get ratios
            BigDecimal cost = ZERO;
            for (Iterator<GenericValue> weoiIt = workEffortAndOrderItems.iterator(); weoiIt.hasNext();) {
                GenericValue workEffortAndOrderItem = weoiIt.next();
                OrderReadHelper orh = new OrderReadHelper(workEffortAndOrderItem.getRelatedOne("OrderHeader"));
                cost = cost.add(orh.getOrderItemTotal(workEffortAndOrderItem.getRelatedOne("OrderItem"))).setScale(decimals + 1, rounding);
                Debug.logInfo("after item [ " + workEffortAndOrderItem + "] cost is now [" + cost + "]", MODULE);
            }

            // record the calculated cost for the outsourced task
            Map serviceParams = UtilMisc.toMap("workEffortId", workEffort.getString("workEffortId"), "cost", cost,
                    "costComponentTypeId", "ACTUAL_" + workEffortCostCalc.getString("costComponentTypeId"), "costUomId", costComponentCalc.getString("currencyUomId"),
                    "costComponentCalcId", costComponentCalc.getString("costComponentCalcId"), "userLogin", userLogin);

            Map tmpResult = dispatcher.runSync("createCostComponent", serviceParams);
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            } else {
                Map results = ServiceUtil.returnSuccess();
                results.put("costComponentId", tmpResult.get("costComponentId"));
                return results;
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Reduce the estimated quantity of a production run task component..
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> reduceWorkEffortGoodStandard(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String workEffortId = (String) context.get("workEffortId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = context.get("quantity") == null ? BigDecimal.ZERO : (BigDecimal) context.get("quantity");
        try {
            // make sure the task is started
            GenericValue task = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (!"PRUN_RUNNING".equals(task.get("currentStatusId")) && !"PRUN_OUTSRC_PEND".equals(task.get("currentStatusId"))) {
                 return UtilMessage.createServiceError("WarehouseError_CannotIssueProductionRunTask", locale);
            }

            Map input = UtilMisc.toMap("workEffortId", workEffortId, "productId", productId, "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED");
            List<GenericValue> wegsList = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortGoodStandard", input, UtilMisc.toList("fromDate DESC")));

            // If no wegs exists yet, then new material has been issued directly from the issue additional material form.
            // We can track this by creating a completed WEGS for estimated quantity 0.
            if (wegsList.size() == 0) {
                GenericValue wegs = delegator.makeValue("WorkEffortGoodStandard", input);
                wegs.set("estimatedQuantity", BigDecimal.ZERO);
                wegs.set("fromDate", UtilDateTime.nowTimestamp());
                wegs.set("statusId", "WEGS_COMPLETED");
                wegs.create();
                return ServiceUtil.returnSuccess();
            }

            // reduce each WEGS by the issued quantity until we have exhausted the requirements
            BigDecimal issued = quantity;
            for (GenericValue wegs : wegsList) {
                if (wegs.get("estimatedQuantity") == null) {
                    Debug.logWarning("WorkEffortGoodStandard " + input + " has null estimatedQuantity, this will cause problems with production run.", MODULE);
                    continue;
                }
                if ("WEGS_COMPLETED".equals(wegs.get("statusId"))) {
                    if (wegs.getDouble("estimatedQuantity").doubleValue() != 0.0) {
                        wegs.set("estimatedQuantity", new BigDecimal("0.0"));
                        wegs.store();
                    }
                } else {
                    BigDecimal requiredByWegs = wegs.getBigDecimal("estimatedQuantity");
                    if (requiredByWegs.compareTo(issued) <= 0 ) {
                        wegs.set("estimatedQuantity", new BigDecimal("0"));
                        wegs.set("statusId", "WEGS_COMPLETED");
                        wegs.store();
                    } else {
                        wegs.set("estimatedQuantity", requiredByWegs.subtract(issued));
                        wegs.store();
                    }
                    issued = issued.subtract(requiredByWegs);
                }
                if (issued.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }

            return ServiceUtil.returnSuccess();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Creates a WorkEffortGoodStandard for the given production run, productId and quantity.
     *
     * TODO: validate productionRunId is a production run? Is this method used ?
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> addProductToProduce(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String productionRunId = (String) context.get("productionRunId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        try {
            GenericValue wegs = delegator.makeValue("WorkEffortGoodStandard");
            wegs.set("workEffortId", productionRunId);
            wegs.set("productId", productId);
            wegs.set("estimatedQuantity", quantity);
            wegs.set("fromDate", UtilDateTime.nowTimestamp());
            wegs.set("workEffortGoodStdTypeId", "PRUNT_PROD_DELIV");
            wegs.set("statusId", "WEGS_CREATED");
            wegs.create();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Cancels the WorkEffortGoodStandard for the given production run and productId.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> removeProductToProduce(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String productionRunId = (String) context.get("productionRunId");
        String productId = (String) context.get("productId");
        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            List<EntityCondition> conditions = Arrays.asList(
                    EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, productionRunId),
                    EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                    EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUNT_PROD_DELIV"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "WEGS_COMPLETED"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "WEGS_CANCELLED"),
                    EntityUtil.getFilterByDateExpr()
            );
            List<GenericValue> wegsList = delegator.findByAnd("WorkEffortGoodStandard", conditions);
            for (GenericValue wegs : wegsList) {
                wegs.set("thruDate", now);
                wegs.set("statusId", "WEGS_CANCELLED");
                wegs.store();
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Describe <code>addMaterialToProdRunTask</code> method here.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> addMaterialToProdRunTask(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String productionRunTaskId = (String) context.get("productionRunTaskId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        try {
            GenericValue wegs = delegator.makeValue("WorkEffortGoodStandard");
            wegs.set("workEffortId", productionRunTaskId);
            wegs.set("productId", productId);
            wegs.set("estimatedQuantity", quantity);
            wegs.set("fromDate", UtilDateTime.nowTimestamp());
            wegs.set("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED");
            wegs.set("statusId", "WEGS_CREATED");
            wegs.create();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Describe <code>removeMaterialFrmProdRunTask</code> method here.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> removeMaterialFrmProdRunTask(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String productionRunTaskId = (String) context.get("productionRunTaskId");
        String productId = (String) context.get("productId");
        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            List<EntityCondition> conditions = UtilMisc.toList(
                    EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, productionRunTaskId),
                    EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                    EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUNT_PROD_NEEDED"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "WEGS_COMPLETED"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "WEGS_CANCELLED"),
                    EntityUtil.getFilterByDateExpr()
            );
            List<GenericValue> wegsList = delegator.findByAnd("WorkEffortGoodStandard", conditions);
            for (GenericValue wegs : wegsList) {
                wegs.set("thruDate", now);
                wegs.set("statusId", "WEGS_CANCELLED");
                wegs.store();
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Describe <code>removeAllMateriaslFrmProdRunTask</code> method here.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> removeAllMateriaslFrmProdRunTask(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String productionRunTaskId = (String) context.get("productionRunTaskId");
        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            List<Map<String, Object>> productsRemoved = FastList.newInstance();
            List<EntityCondition> conditions = UtilMisc.toList(
                    EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, productionRunTaskId),
                    EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUNT_PROD_NEEDED"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "WEGS_COMPLETED"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "WEGS_CANCELLED"),
                    EntityUtil.getFilterByDateExpr()
            );
            List<GenericValue> wegsList = delegator.findByAnd("WorkEffortGoodStandard", conditions);
            for (GenericValue wegs : wegsList) {
                BigDecimal quantity = wegs.getBigDecimal("estimatedQuantity");
                if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
                    productsRemoved.add(UtilMisc.toMap("productId", wegs.get("productId"), "quantity", wegs.getBigDecimal("estimatedQuantity")));
                }
                wegs.set("thruDate", now);
                wegs.set("statusId", "WEGS_CANCELLED");
                wegs.store();
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("productsRemoved", productsRemoved);
            return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Creates a Production Run.  This is the original service from manufacturing ProductionRunServices.java.  It has
     * been refactored to allow disassembly from a disassembly template.  Disassembly is triggered by an additional
     * Boolean parameter disassemble which defaults to false.  At some point, we're going to merge this into
     * opentaps.createProductionRun defined above as createProductionRun() so that outsourced tasks are part of the service.
     *
     *  <li> check if routing - product link exist
     *  <li> check if product have a Bill Of Material
     *  <li> check if routing have routingTask
     *  <li> create the workEffort for ProductionRun
     *  <li> create the WorkEffortGoodStandard for link between ProductionRun and the product it will produce
     *  <li> for each valid routingTask of the routing create a workeffort-task
     *  <li> for the first routingTask, create for all the valid productIdTo with no associateRoutingTask  a WorkEffortGoodStandard
     *  <li> for each valid routingTask of the routing and valid productIdTo associate with this RoutingTask create a WorkEffortGoodStandard
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productId, routingId, pRQuantity, startDate, workEffortName, description
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createProductionRunRefactored(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // Mandatory input fields
        String productId = (String) context.get("productId");
        Timestamp startDate = (Timestamp) context.get("startDate");
        BigDecimal pRQuantity = (BigDecimal) context.get("pRQuantity");



        String facilityId = (String) context.get("facilityId");
        // Optional input fields
        String workEffortId = (String) context.get("routingId");
        String workEffortName = (String) context.get("workEffortName");
        String description = (String) context.get("description");
        String routingId = (String) context.get("routingId");

        // disassembly trigger
        boolean disassemble = (context.get("disassemble") == null ? false : (Boolean) context.get("disassemble"));

        GenericValue routing = null;
        GenericValue product = null;
        List<GenericValue> routingTaskAssocs = null;

        try {
            // Find the product
            product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
            if (product == null) {
                return UtilMessage.createAndLogServiceError("ManufacturingProductNotExist", locale, MODULE);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // -------------------
        // Routing and routing tasks
        // -------------------
        // Select the product's routing
        try {
            String getRoutingService = disassemble ? "getProductRoutingDisassemble" : "getProductRouting";
            Map<String, Object> routingInMap = UtilMisc.toMap("productId", productId, "applicableDate", startDate, "userLogin", userLogin);
            if (workEffortId != null) {
                routingInMap.put("workEffortId", workEffortId);
            }
            Map<String, Object> routingOutMap = dispatcher.runSync(getRoutingService, routingInMap);
            routing = (GenericValue) routingOutMap.get("routing");
            routingTaskAssocs = (List<GenericValue>) routingOutMap.get("tasks");
        } catch (GenericServiceException gse) {
            Debug.logWarning(gse.getMessage(), MODULE);
        }
        // =================================
        if (routing == null) {
            return UtilMessage.createAndLogServiceError("ManufacturingProductRoutingNotExist", locale, MODULE);
        }
        if (UtilValidate.isEmpty(routingTaskAssocs)) {
            return UtilMessage.createAndLogServiceError("ManufacturingRoutingHasNoRoutingTask", locale, MODULE);
        }

        Debug.logInfo("createProductionRunRefactored: using routing [" + routing.get("workEffortId") + "] : " + routing, MODULE);

        // -------------------
        // Components
        // -------------------
        // The components are retrieved using the getManufacturingComponents service
        // (that performs a bom breakdown and if needed runs the configurator).
        List<BomNode> components = null;
        Map<String, Object> serviceContext = new HashMap<String, Object>();
        serviceContext.put("productId", productId); // the product that we want to manufacture or disassemble
        serviceContext.put("quantity", pRQuantity); // the quantity that we want to manufacture
        serviceContext.put("userLogin", userLogin);
        if (disassemble && UtilValidate.isNotEmpty(routingId) && "DEF_DISASMBL_TMP".equals(routing.get("workEffortId"))) {
            // if it is a disassembly, and a routing was given, getProductRoutingDisassemble might have returned
            // the default template DEF_DISASMBL_TMP. In that case we allow to pass the given routing when getting the BOM
            // in order to get a specific BOM instead of the default BOM (should one be setup)..
            Debug.logInfo("Found default disassembly template, getting BOM for the specified routing [" + routingId + "] instead.", MODULE);
            serviceContext.put("routingId", routingId); // the routing to use for the BOM
        } else if (!disassemble && UtilValidate.isNotEmpty(routingId) && "DEFAULT_ROUTING".equals(routing.get("workEffortId"))) {
            // else if it is an assembly, and a routing was given, getProductRouting might have returned
            // the default template DEFAULT_ROUTING. In that case we allow to pass the given routing when getting the BOM
            // in order to get a specific BOM instead of the default BOM (should one be setup).
            Debug.logInfo("Found default assembly template, getting BOM for the specified routing [" + routingId + "] instead.", MODULE);
            serviceContext.put("routingId", routingId); // the routing to use for the BOM
        } else {
            serviceContext.put("routingId", routing.get("workEffortId")); // the routing to use for the BOM
        }

        Map<String, Object> resultService = null;
        try {
            resultService = dispatcher.runSync("getManufacturingComponents", serviceContext);
            components = (List<BomNode>) resultService.get("components"); // a list of objects representing the product's components
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the getManufacturingComponents service", MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Debug.logInfo("Found components: " + components, MODULE);

        // ProductionRun header creation,
        if (workEffortName == null) {
            String prdName = product.getString("productName");
            if (UtilValidate.isEmpty(prdName)) {
                prdName = product.getString("productId");
            }

            String wefName = routing.getString("workEffortName");
            if (UtilValidate.isEmpty(wefName)) {
                wefName = routing.getString("workEffortId");
            }

            workEffortName =  prdName + "-" + wefName;
            // work effort name is limited to 100 chars
            if (workEffortName.length() > 100) {
                workEffortName = workEffortName.substring(0, 100);
            }

        }

        serviceContext.clear();
        serviceContext.put("workEffortTypeId", "PROD_ORDER_HEADER");
        serviceContext.put("workEffortPurposeTypeId", disassemble ? "WEPT_DISASSEMBLY" : "WEPT_PRODUCTION_RUN");
        serviceContext.put("currentStatusId", "PRUN_CREATED");
        serviceContext.put("workEffortName", workEffortName);
        serviceContext.put("description", description);
        serviceContext.put("facilityId", facilityId);
        serviceContext.put("estimatedStartDate", startDate);
        serviceContext.put("quantityToProduce", pRQuantity);
        serviceContext.put("userLogin", userLogin);
        try {
            resultService = dispatcher.runSync("createWorkEffort", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffort service", MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        String productionRunId = (String) resultService.get("workEffortId");
        Debug.logInfo("ProductionRun created: " + productionRunId, MODULE);

        // ProductionRun,  product will be produce creation = WorkEffortGoodStandard for the productId
        // if disassembling, then the produced products are the components
        List<Map<String, Object>> productionList = FastList.newInstance();
        if (disassemble) {
            for (Iterator<BomNode> iter = components.iterator(); iter.hasNext();) {
                BomNode node = iter.next();
                productionList.add(UtilMisc.toMap("productId", node.getProduct().get("productId"), "quantity", node.getQuantity()));
            }
        } else {
            productionList.add(UtilMisc.<String, Object>toMap("productId", productId, "quantity", pRQuantity));
        }
        for (Map<String, Object> production : productionList) {
            String producedProductId = (String) production.get("productId");
            BigDecimal producedQuantity = (BigDecimal) production.get("quantity");
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("productId", producedProductId);
            serviceContext.put("workEffortGoodStdTypeId", "PRUN_PROD_DELIV");
            serviceContext.put("statusId", "WEGS_CREATED");
            serviceContext.put("estimatedQuantity", producedQuantity);
            serviceContext.put("fromDate", startDate);
            serviceContext.put("userLogin", userLogin);
            try {
                resultService = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the createWorkEffortGoodStandard service", MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // if disassembling, note the product being disassembled and quantity using a special WEGS type
        if (disassemble) {
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("productId", productId);
            serviceContext.put("workEffortGoodStdTypeId", "PRUN_PROD_DISASMBL");
            serviceContext.put("statusId", "WEGS_CREATED");
            serviceContext.put("estimatedQuantity", pRQuantity);
            serviceContext.put("fromDate", startDate);
            serviceContext.put("userLogin", userLogin);
            try {
                resultService = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the createWorkEffortGoodStandard service", MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // Multi creation (like clone) ProductionRunTask and GoodAssoc
        Iterator<GenericValue> rt = routingTaskAssocs.iterator();
        boolean first = true;
        while (rt.hasNext()) {
            GenericValue routingTaskAssoc = rt.next();

            if (EntityUtil.isValueActive(routingTaskAssoc, startDate)) {
                GenericValue routingTask = null;
                try {
                    routingTask = routingTaskAssoc.getRelatedOne("ToWorkEffort");
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(), MODULE);
                }
                // Calculate the estimatedCompletionDate
                long totalTime = ProductionRun.getEstimatedTaskTime(routingTask, pRQuantity, dispatcher);
                Timestamp endDate = TechDataServices.addForward(TechDataServices.getTechDataCalendar(routingTask), startDate, totalTime);

                serviceContext.clear();
                serviceContext.put("priority", routingTaskAssoc.get("sequenceNum"));
                serviceContext.put("workEffortPurposeTypeId", routingTask.get("workEffortPurposeTypeId"));
                serviceContext.put("workEffortName", routingTask.get("workEffortName"));
                serviceContext.put("description", routingTask.get("description"));
                serviceContext.put("fixedAssetId", routingTask.get("fixedAssetId"));
                serviceContext.put("workEffortTypeId", "PROD_ORDER_TASK");
                serviceContext.put("currentStatusId", "PRUN_CREATED");
                serviceContext.put("workEffortParentId", productionRunId);
                serviceContext.put("facilityId", facilityId);
                serviceContext.put("estimatedStartDate", startDate);
                serviceContext.put("estimatedCompletionDate", endDate);
                serviceContext.put("estimatedSetupMillis", routingTask.get("estimatedSetupMillis"));
                serviceContext.put("estimatedMilliSeconds", routingTask.get("estimatedMilliSeconds"));
                serviceContext.put("quantityToProduce", pRQuantity);
                serviceContext.put("userLogin", userLogin);
                resultService = null;
                try {
                    resultService = dispatcher.runSync("createWorkEffort", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the createWorkEffort service", MODULE);
                }
                String productionRunTaskId = (String) resultService.get("workEffortId");
                Debug.logInfo("ProductionRunTaskId created: " + productionRunTaskId, MODULE);

                // The newly created production run task is associated to the routing task
                // to keep track of the template used to generate it.
                serviceContext.clear();
                serviceContext.put("userLogin", userLogin);
                serviceContext.put("workEffortIdFrom", routingTask.getString("workEffortId"));
                serviceContext.put("workEffortIdTo", productionRunTaskId);
                serviceContext.put("workEffortAssocTypeId", "WORK_EFF_TEMPLATE");
                try {
                    resultService = dispatcher.runSync("createWorkEffortAssoc", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the createWorkEffortAssoc service", MODULE);
                }
                // copy date valid WorkEffortPartyAssignments from the routing task to the run task
                List<GenericValue> workEffortPartyAssignments = null;
                try {
                    workEffortPartyAssignments = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortPartyAssignment",
                            UtilMisc.toMap("workEffortId", routingTaskAssoc.getString("workEffortIdTo"))));
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(), MODULE);
                }
                if (workEffortPartyAssignments != null) {
                    Iterator<GenericValue> i = workEffortPartyAssignments.iterator();
                    while (i.hasNext()) {
                        GenericValue workEffortPartyAssignment = i.next();
                        Map<String, Object> partyToWorkEffort = UtilMisc.toMap(
                                "workEffortId",  productionRunTaskId,
                                "partyId",  workEffortPartyAssignment.getString("partyId"),
                                "roleTypeId",  workEffortPartyAssignment.getString("roleTypeId"),
                                "fromDate",  workEffortPartyAssignment.getTimestamp("fromDate"),
                                "statusId",  workEffortPartyAssignment.getString("statusId"),
                                "userLogin", userLogin
                        );
                        try {
                            resultService = dispatcher.runSync("assignPartyToWorkEffort", partyToWorkEffort);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Problem calling the assignPartyToWorkEffort service", MODULE);
                        }
                        Debug.logInfo("ProductionRunPartyassigment for party: " + workEffortPartyAssignment.get("partyId") + " created", MODULE);
                    }
                }

                // if disassembling and the task is identified as the disassebmly task, then this one should receive the material issuance
                if (disassemble && "ROUTING_DISASSEMBLY".equals(routingTaskAssoc.get("workEffortAssocTypeId"))) {
                    serviceContext.clear();
                    serviceContext.put("workEffortId", productionRunTaskId);
                    serviceContext.put("productId", productId);
                    serviceContext.put("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED");
                    serviceContext.put("statusId", "WEGS_CREATED");
                    serviceContext.put("fromDate", startDate);
                    serviceContext.put("estimatedQuantity", pRQuantity);
                    serviceContext.put("userLogin", userLogin);
                    resultService = null;
                    try {
                        resultService = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Problem calling the createWorkEffortGoodStandard service", MODULE);
                    }
                    Debug.logInfo("Disassembling qty [" + pRQuantity + "] of product [" + productId + "] for task [" + productionRunTaskId + "] of production run [" + productionRunId + "].", MODULE);
                } else {
                    // Now we iterate thru the components returned by the getManufacturingComponents service
                    // TODO: if in the BOM a routingWorkEffortId is specified, but the task is not in the routing
                    //       the component is not added to the production run.
                    Iterator<BomNode> pb = components.iterator();
                    while (pb.hasNext()) {
                        // The components variable contains a list of BOMNodes:
                        // each node represents a product (component).
                        BomNode node = pb.next();
                        GenericValue productBom = node.getProductAssoc();
                        if ((productBom.getString("routingWorkEffortId") == null && first) || (productBom.getString("routingWorkEffortId") != null && productBom.getString("routingWorkEffortId").equals(routingTask.getString("workEffortId")))) {
                            serviceContext.clear();
                            serviceContext.put("workEffortId", productionRunTaskId);
                            // Here we get the ProductAssoc record from the BOMNode
                            // object to be sure to use the
                            // right component (possibly configured).
                            serviceContext.put("productId", node.getProduct().get("productId"));
                            serviceContext.put("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED");
                            serviceContext.put("statusId", "WEGS_CREATED");
                            serviceContext.put("fromDate", productBom.get("fromDate"));
                            // Here we use the getQuantity method to get the quantity already
                            // computed by the getManufacturingComponents service
                            serviceContext.put("estimatedQuantity", node.getQuantity());
                            serviceContext.put("userLogin", userLogin);
                            resultService = null;
                            try {
                                resultService = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Problem calling the createWorkEffortGoodStandard service", MODULE);
                            }
                            Debug.logInfo("ProductLink created for productId: " + productBom.getString("productIdTo"), MODULE);
                        }
                    }
                }
                first = false;
                startDate = endDate;
            }
        }

        // update the estimatedCompletionDate field for the productionRun
        serviceContext.clear();
        serviceContext.put("workEffortId", productionRunId);
        serviceContext.put("estimatedCompletionDate", startDate);
        serviceContext.put("userLogin", userLogin);
        resultService = null;
        try {
            resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
        }
        Map<String, Object> result = UtilMessage.createServiceSuccess("ManufacturingProductionRunCreated", locale, UtilMisc.toMap("productionRunId", productionRunId));
        result.put("productionRunId", productionRunId);
        result.put("estimatedCompletionDate", startDate);
        return result;
    }

    /**
     * Retrieves the template for a product, otherwise a default template.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getProductRouting(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        String productId = (String) context.get("productId");
        String workEffortId = (String) context.get("workEffortId");
        Timestamp applicableDate = (Timestamp) context.get("applicableDate");
        boolean ignoreDefaultRouting = "Y".equals(context.get("ignoreDefaultRouting"));
        BigDecimal quantity = (BigDecimal) context.get("quantity");

        // if applicableDate is not given defaults to Now
        if (applicableDate == null) {
            applicableDate = UtilDateTime.nowTimestamp();
        }

        Debug.logInfo("getProductRouting: finding routing for product [" + productId + "], workEffortId [" + workEffortId + "], date [" + applicableDate + "] and quantity [" + (quantity != null ? quantity.toString() : "Not specified") + "]", MODULE);

        try {
            // find the routing from the WEGS
            // There are three options:
            //     routinWegsId equals to
            //     1. DEFAULT_ROUTING: routing isn't found, use default one
            //     2. null: routing isn't found, we don't need default routing, return null
            //     3. a String that is workEffortId
            String routingWegsId = getProductRouting(productId, workEffortId, applicableDate, quantity, ignoreDefaultRouting, delegator);

            // find the routing WorkEffort
            GenericValue routing = null;
            if (UtilValidate.isNotEmpty(routingWegsId)) {
                if ("DEFAULT_ROUTING".equals(routingWegsId)) {
                    if (!ignoreDefaultRouting) {
                        // use default routing
                        routing = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", "DEFAULT_ROUTING"));
                    }
                } else {
                    // find the WorkEffort
                    routing = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", routingWegsId));
                }
            }

            // find the associated tasks, ordered by sequence
            List<GenericValue> tasks = null;
            if (routing != null) {
                EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                  EntityCondition.makeCondition("workEffortIdFrom", EntityOperator.EQUALS, routing.get("workEffortId")),
                                  EntityCondition.makeCondition("workEffortAssocTypeId", EntityOperator.EQUALS, "ROUTING_COMPONENT"),
                                  EntityDateFilterCondition.makeCondition(applicableDate, "fromDate", "thruDate"));
                tasks = delegator.findList("WorkEffortAssoc", conditions, null, Arrays.asList("sequenceNum"), null, false);
            }

            Debug.logInfo("getProductRouting: returning routing [" + routing + "]", MODULE);

            // return success
            Map result = ServiceUtil.returnSuccess();
            result.put("routing", routing);
            result.put("tasks", tasks);
            return result;

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    private static String getProductRouting(String productId, String workEffortId, Timestamp applicableDate, BigDecimal quantity, boolean ignoreDefRouting, Delegator delegator) throws GenericEntityException {
        // find active routings for the productId and workEffortId (optional)
        List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                                        EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "ROU_PROD_TEMPLATE"),
                                        EntityDateFilterCondition.makeCondition(applicableDate, "fromDate", "thruDate"));
        if (UtilValidate.isNotEmpty(workEffortId)) {
            conditions.add(EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId));
        }

        List<GenericValue> routings =
            delegator.findByConditionCache("WorkEffortGoodStandard", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, Arrays.asList("minQuantity"));

        // if no routing was found, check if the product has any Variant product
        if (routings.isEmpty()) {
            Debug.logInfo("No routing found for product [" + productId + "] and routing [" + workEffortId + "] checking routing for its variants.", MODULE);
            conditions = UtilMisc.<EntityCondition>toList(
                            EntityCondition.makeCondition("productIdTo", EntityOperator.EQUALS, productId),
                            EntityCondition.makeCondition("productAssocTypeId", EntityOperator.EQUALS, "PRODUCT_VARIANT"),
                            EntityDateFilterCondition.makeCondition(applicableDate, "fromDate", "thruDate"));
            List<GenericValue> virtuals = delegator.findByAnd("ProductAssoc", conditions);
            // get the virtual product ids
            List<String> virtualIds = EntityUtil.getFieldListFromEntityList(virtuals, "productId", true);
            // find routings applicable to those Variants
            conditions = UtilMisc.<EntityCondition>toList(
                            EntityCondition.makeCondition("productId", EntityOperator.IN, virtualIds),
                            EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "ROU_PROD_TEMPLATE"),
                            EntityDateFilterCondition.makeCondition(applicableDate, "fromDate", "thruDate"));
            if (UtilValidate.isNotEmpty(workEffortId)) {
                conditions.add(EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId));
            }

            routings = delegator.findByCondition("WorkEffortGoodStandard", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, Arrays.asList("minQuantity"));
        }

        // check routings for quantity
        if (quantity != null) {
            List<GenericValue> routingsWithQuantities =
                EntityUtil.filterByCondition(routings,
                               EntityCondition.makeCondition(EntityOperator.OR,
                                    EntityCondition.makeCondition("minQuantity", EntityOperator.NOT_EQUAL, null),
                                    EntityCondition.makeCondition("maxQuantity", EntityOperator.NOT_EQUAL, null))
                );
            if (UtilValidate.isNotEmpty(routingsWithQuantities)) {
                // there are routings that have mixQuantity or maxQuantity on value
                List<GenericValue> matchedRoutings = FastList.newInstance();
                for (GenericValue routing : routingsWithQuantities) {
                    BigDecimal minQuantity = routing.getBigDecimal("minQuantity");
                    BigDecimal maxQuantity = routing.getBigDecimal("maxQuantity");
                    if (minQuantity != null && maxQuantity != null) {
                        if (quantity.compareTo(minQuantity) >= 0 && quantity.compareTo(maxQuantity) <= 0) {
                            matchedRoutings.add(routing);
                            continue;
                        }
                    } else if (minQuantity != null) {
                        if (quantity.compareTo(minQuantity) >= 0) {
                            matchedRoutings.add(routing);
                            continue;
                        }
                    } else if (maxQuantity != null) {
                        if (quantity.compareTo(maxQuantity) <= 0) {
                            matchedRoutings.add(routing);
                            continue;
                        }
                    }
                }

                // doesn't return any routing if we have WEGS with quantities but nothing match given quantity
                if (UtilValidate.isEmpty(matchedRoutings)) {
                    return null;
                }

                routings = Arrays.asList(matchedRoutings.get(0));
            }
        }

        // if more than one routing is found, try to figure which one is best suited:
        //  if no workEffortId was given then we are probably looking for a default routing, so filter out routings involved with special BOMs
        //  else if a workEffortId was given take the first one found
        // TODO: we could also use the WorkEffort priority
        if (routings.size() > 1) {
            if (UtilValidate.isNotEmpty(workEffortId)) {
                Debug.logWarning("Found more than one routing applicable for product [" + productId + "], workEffortId [" + workEffortId + "] and date [" + applicableDate + "], using the first one found.", MODULE);
            } else {

                // if no workEffortId was given, remove routings that are involved in alternate BOMs as we are looking for a default routing
                List<GenericValue> alternateBomRoutings = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "MANUF_COMPONENT"));
                List<String> alternateBomRoutingIds = EntityUtil.getFieldListFromEntityList(alternateBomRoutings, "specificRoutingWorkEffortId", true);
                List<GenericValue> routingsNoSpecialBom = EntityUtil.filterByAnd(routings, Arrays.asList(EntityCondition.makeCondition("workEffortId", EntityOperator.NOT_IN, alternateBomRoutingIds)));

                // do we have a routing with no special BOM ?
                if (routingsNoSpecialBom.size() > 1) {
                    Debug.logWarning("Found more than one routing with no special BOM applicable for product [" + productId + "] and date [" + applicableDate + "], using the first one found.", MODULE);
                }
                if (routingsNoSpecialBom.size() >= 1) {
                    return routingsNoSpecialBom.get(0).getString("workEffortId");
                }

                // else all the found routings have special BOMs, well just return the first one of the list
                Debug.logWarning("Found more than one routing applicable for product [" + productId + "] and date [" + applicableDate + "], using the first one found.", MODULE);
            }
        }

        GenericValue routingProductLink = EntityUtil.getFirst(routings);
        return (routingProductLink != null ? routingProductLink.getString("workEffortId") : "DEFAULT_ROUTING");
    }

    /**
     * Retrieves the disassemble template for a product, otherwise a default disassemble template.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getProductRoutingDisassemble(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        String productId = (String) context.get("productId");
        String workEffortId = (String) context.get("workEffortId");
        Timestamp applicableDate = (Timestamp) context.get("applicableDate");
        boolean ignoreDefault = "Y".equals(context.get("ignoreDefaultRouting"));
        try {
            GenericValue routing = null;

            // Look for a disassembly template (routing) for the product if the routing is specified (workEffortId)
            if (workEffortId != null) {
                List conditions = UtilMisc.toList(
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId),
                        EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "ROU_DISASMBL_TEMPL")
                );
                if (applicableDate != null) {
                    conditions.add(EntityUtil.getFilterByDateExpr(applicableDate));
                }
                GenericValue wegs = EntityUtil.getFirst(delegator.findByAnd("WorkEffortGoodStandard", conditions));

                // if not found, see if this product is a variant of a real product
                if (wegs == null) {
                    List assocs = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productIdTo", productId, "productAssocTypeId", "PRODUCT_VARIANT"));
                    GenericValue assoc = EntityUtil.getFirst(EntityUtil.filterByDate(assocs, applicableDate));
                    if (assoc != null) {
                        conditions.remove(0);
                        conditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, assoc.get("productId")));
                        wegs = EntityUtil.getFirst(delegator.findByAnd("WorkEffortGoodStandard", conditions));
                    }
                }

                // get the routing definition if found
                if (wegs != null) {
                    routing = wegs.getRelatedOne("WorkEffort");
                }
            }

            // if no routing yet, get the default disassembly template (routing)
            if (!ignoreDefault && routing == null) {
                routing = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", "DEF_DISASMBL_TMP"));
            }

            // if still no routing, then we're done
            if (routing == null) {
                Debug.logInfo("No disassembly routing (production run template) found for product [" + productId + "]", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // get the disassembly task association
            List disassemblyTasks = delegator.findByAnd("WorkEffortAssoc", UtilMisc.toMap("workEffortAssocTypeId", "ROUTING_DISASSEMBLY", "workEffortIdFrom", routing.get("workEffortId")));
            GenericValue disassemblyTask = EntityUtil.getFirst(EntityUtil.filterByDate(disassemblyTasks, applicableDate));
            if (disassemblyTask == null) {
                String error = "Cannot disassemble product [" + productId + "] from template [" + routing.get("workEffortId") + "] because the disassembly task template (identified by work effort assoc type ROUTING_DISASSEMBLY) is not defined.";
                Debug.logWarning(error, MODULE);
                return ServiceUtil.returnError(error);
            }
            List tasks = UtilMisc.toList(disassemblyTask);

            // any additional task templates are simply the active ROUTING_COMPONENT work efforts, just like for assembly
            List additionalTasks = routing.getRelatedByAnd("FromWorkEffortAssoc", UtilMisc.toMap("workEffortAssocTypeId", "ROUTING_COMPONENT"));
            additionalTasks = EntityUtil.filterByDate(additionalTasks, UtilDateTime.nowTimestamp());
            tasks.addAll(additionalTasks);

            Map results = ServiceUtil.returnSuccess();
            results.put("routing", routing);
            results.put("tasks", tasks);
            return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Refactoring of productionRunProduce.
     * This service will allocate the given quantity of product to the inventory.
     * It uses WorkEffortGoodStandard of type PRUN_PROD_PRODUCED to keep track of what
     * was produced and PRUN_PROD_DELIV for what is intended to be produced.
     * This service allows production runs that have multiple products produced, such as disassemblies.
     * It is reverse compatible with productionRunProduced, but requires a data migration for existing production runs.
     * If quantity is not specified, then this service will produce remaining for the product.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> productionRunProduceRefactored(DispatchContext ctx, Map<String, Object> context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // Mandatory input fields
        String productionRunId = (String) context.get("workEffortId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");

        // Optional input fields
        String inventoryItemTypeId = (String) context.get("inventoryItemTypeId");
        String lotId = (String) context.get("lotId");
        Boolean createLotIfNeeded = (Boolean) context.get("createLotIfNeeded");
        Boolean autoCreateLot = (Boolean) context.get("autoCreateLot");

        // The default is non-serialized inventory item
        if (UtilValidate.isEmpty(inventoryItemTypeId)) {
            inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
        }
        // The default is to create a lot if the lotId is given, but the lot doesn't exist
        if (createLotIfNeeded == null) {
            createLotIfNeeded = Boolean.TRUE;
        }
        if (autoCreateLot == null) {
            autoCreateLot = Boolean.FALSE;
        }

        List inventoryItemIds = new ArrayList();
        result.put("inventoryItemIds", inventoryItemIds);

        // The production run is loaded
        OpentapsProductionRun productionRun = new OpentapsProductionRun(productionRunId, dispatcher);
        if (productId == null) {
            productId = productionRun.getProductProduced().getString("productId");
        }

        try {

            // domain repository
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            OrganizationRepositoryInterface organizationRepository = dl.loadDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
            ProductRepositoryInterface productRepository = dl.loadDomainsDirectory().getProductDomain().getProductRepository();
            String facilityId = productionRun.getGenericValue().getString("facilityId");
            GenericValue facility = delegator.findByPrimaryKey("Facility", UtilMisc.toMap("facilityId", facilityId));
            Organization organization = organizationRepository.getOrganizationById(facility.getString("ownerPartyId"));
            Product product = productRepository.getProductById(productId);

            // use the WEGS type PRUN_PROD_PRODUCED to figure out what has already been produced
            List conditions = UtilMisc.toList(
                    EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                    EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, productionRunId),
                    EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUN_PROD_PRODUCED"),
                    EntityUtil.getFilterByDateExpr() // not really used per se, but might be in future... if it is used please note how here
            );
            List<GenericValue> wegsList = delegator.findByAnd("WorkEffortGoodStandard", conditions);

            // because a production run can produce several lines of the same produce, we'll need to add up the wegs
            BigDecimal produced = BigDecimal.ZERO;
            for (GenericValue wegs : wegsList) {
                produced = produced.add(wegs.get("estimatedQuantity") == null ? BigDecimal.ZERO : wegs.getBigDecimal("estimatedQuantity"));
            }
            BigDecimal quantityProduced = produced;

            // get the quantity declared for this product
            BigDecimal quantityDeclared = productionRun.getQuantityToProduce(productId);
            if (quantityDeclared == null) {
                return ServiceUtil.returnError("Produce [" + productId + "] is not being produced in production run [" + productionRunId + "].");
            }

            // If the quantity already produced is not lower than the quantity declared, no inventory is created.
            BigDecimal maxQuantity = quantityDeclared.subtract(quantityProduced);
            if (maxQuantity.signum() <= 0) {
                Debug.logInfo("Attempt to produce [" + quantity + "] of product [" + productId + "] when it has been fully produced in production run [" + productionRunId + "].  Not producing anything.", MODULE);
                return result;
            }

            // if quantity not supplied, assume issue all of it
            if (quantity == null) {
                quantity = maxQuantity;
            }

            // cannot produce more than what's allowed
            if (quantity.compareTo(maxQuantity) > 0) {
                return UtilMessage.createAndLogServiceError("ManufacturingProductionRunProductProducedNotStillAvailable", locale, MODULE);
            }

            if (lotId == null && autoCreateLot.booleanValue()) {
                lotId = delegator.getNextSeqId("Lot");
                createLotIfNeeded = Boolean.TRUE;
            }
            if (lotId != null) {
                try {
                    // Find the lot
                    GenericValue lot = delegator.findByPrimaryKey("Lot", UtilMisc.toMap("lotId", lotId));
                    if (lot == null) {
                        if (createLotIfNeeded.booleanValue()) {
                            lot = delegator.makeValue("Lot", UtilMisc.toMap("lotId", lotId, "creationDate", UtilDateTime.nowDate()));
                            lot.create();
                        } else {
                            return UtilMessage.createAndLogServiceError("ManufacturingLotNotExists", locale, MODULE);
                        }
                    }
                } catch (GenericEntityException e) {
                    return UtilMessage.createAndLogServiceError(e, MODULE);
                }
            }

            GenericValue orderItem = null;
            try {
                // Find the related order item (if exists)
                List orderItems = productionRun.getGenericValue().getRelated("WorkOrderItemFulfillment");
                orderItem = EntityUtil.getFirst(orderItems);
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, MODULE);
            }
            // calculate the inventory item unit cost
            BigDecimal unitCost = ZERO;
            Debug.logInfo("productionRunProduceRefactored : Calculating product unitCost", MODULE);
            try {
                if (productionRun.isAssembly()) {
                    if (organization.usesStandardCosting()) {
                        unitCost = product.getStandardCost(organization.getPartyAcctgPreference().getBaseCurrencyUomId());
                        Debug.logInfo("productionRunProduceRefactored : unitCost using standard costing", MODULE);
                    } else {
                        Map outputMap = dispatcher.runSync("getProductionRunCost", UtilMisc.toMap("userLogin", userLogin, "workEffortId", productionRunId));
                        BigDecimal totalCost = (BigDecimal) outputMap.get("totalCost");
                        unitCost = totalCost.divide(quantity, decimals, rounding);
                        Debug.logInfo("productionRunProduceRefactored : unitCost using production run cost", MODULE);
                    }
                    Debug.logInfo("productionRunProduceRefactored : unitCost = " + unitCost, MODULE);
                } else {
                    unitCost = UtilProduct.getConservativeValueForOrg(productId, facility.getString("ownerPartyId"), dispatcher, userLogin);
                    if (unitCost == null) {
                        Debug.logWarning("No conservative value found for product [" + productId + "].  Defaulting to zero value for inventory.", MODULE);
                        unitCost = ZERO;
                    }
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            if ("SERIALIZED_INV_ITEM".equals(inventoryItemTypeId)) {
                try {
                    int numOfItems = quantity.intValue();
                    for (int i = 0; i < numOfItems; i++) {
                        Map serviceContext = UtilMisc.toMap("productId", productId,
                                                            "inventoryItemTypeId", "SERIALIZED_INV_ITEM",
                                                            "statusId", "INV_AVAILABLE");
                        serviceContext.put("facilityId", productionRun.getGenericValue().getString("facilityId"));
                        serviceContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                        serviceContext.put("comments", "Created by production run " + productionRunId);
                        if (unitCost.compareTo(ZERO) != 0) {
                            serviceContext.put("unitCost", unitCost);
                        }
                        //serviceContext.put("serialNumber", productionRunId);
                        serviceContext.put("lotId", lotId);
                        serviceContext.put("userLogin", userLogin);
                        Map resultService = dispatcher.runSync("createInventoryItem", serviceContext);
                        String inventoryItemId = (String) resultService.get("inventoryItemId");
                        inventoryItemIds.add(inventoryItemId);
                        GenericValue inventoryProduced = delegator.makeValue("WorkEffortInventoryProduced", UtilMisc.toMap("workEffortId", productionRunId , "inventoryItemId", inventoryItemId));
                        inventoryProduced.create();
                        serviceContext.clear();
                        serviceContext.put("inventoryItemId", inventoryItemId);
                        serviceContext.put("workEffortId", productionRunId);
                        serviceContext.put("availableToPromiseDiff", BigDecimal.ONE);
                        serviceContext.put("quantityOnHandDiff", BigDecimal.ONE);
                        serviceContext.put("userLogin", userLogin);
                        resultService = dispatcher.runSync("createInventoryItemDetail", serviceContext);
                        // Recompute reservations
                        serviceContext = new HashMap();
                        serviceContext.put("inventoryItemId", inventoryItemId);
                        serviceContext.put("userLogin", userLogin);
                        resultService = dispatcher.runSync("balanceInventoryItems", serviceContext);
                    }
                } catch (Exception exc) {
                    return ServiceUtil.returnError(exc.getMessage());
                }
            } else {
                try {
                    Map serviceContext = UtilMisc.toMap("productId", productId,
                                                        "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
                    serviceContext.put("facilityId", productionRun.getGenericValue().getString("facilityId"));
                    serviceContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                    serviceContext.put("comments", "Created by production run " + productionRunId);
                    serviceContext.put("lotId", lotId);
                    if (unitCost.compareTo(ZERO) != 0) {
                        serviceContext.put("unitCost", unitCost);
                    }
                    serviceContext.put("userLogin", userLogin);
                    Map resultService = dispatcher.runSync("createInventoryItem", serviceContext);
                    String inventoryItemId = (String) resultService.get("inventoryItemId");
                    inventoryItemIds.add(inventoryItemId);
                    GenericValue inventoryProduced = delegator.makeValue("WorkEffortInventoryProduced", UtilMisc.toMap("workEffortId", productionRunId , "inventoryItemId", inventoryItemId));
                    inventoryProduced.create();
                    serviceContext.clear();
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceContext.put("workEffortId", productionRunId);
                    serviceContext.put("availableToPromiseDiff", quantity);
                    serviceContext.put("quantityOnHandDiff", quantity);
                    serviceContext.put("userLogin", userLogin);
                    resultService = dispatcher.runSync("createInventoryItemDetail", serviceContext);
                    // Recompute reservations
                    serviceContext = new HashMap();
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceContext.put("userLogin", userLogin);
                    if (orderItem != null) {
                        // the reservations of this order item are privileged reservations
                        serviceContext.put("priorityOrderId", orderItem.getString("orderId"));
                        serviceContext.put("priorityOrderItemSeqId", orderItem.getString("orderItemSeqId"));
                    }
                    resultService = dispatcher.runSync("balanceInventoryItems", serviceContext);
                } catch (Exception exc) {
                    return ServiceUtil.returnError(exc.getMessage());
                }
                // update completion date (in the old method this was updating the quantity produced, but we now use WEGS for that)
                Map serviceContext = new HashMap();
                serviceContext.clear();
                serviceContext.put("workEffortId", productionRunId);
                serviceContext.put("actualCompletionDate", UtilDateTime.nowTimestamp());
                serviceContext.put("userLogin", userLogin);
                try {
                    dispatcher.runSync("updateWorkEffort", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                    return UtilMessage.createAndLogServiceError("ManufacturingProductionRunStatusNotChanged", locale, MODULE);
                }
            }

            // update WEGS for quantity produced, which involves adding another line item for it rather than adding an existing item in place
            GenericValue wegs = delegator.makeValue("WorkEffortGoodStandard");
            wegs.set("workEffortId", productionRunId);
            wegs.set("productId", productId);
            wegs.set("statusId", "WEGS_COMPLETED");
            wegs.set("estimatedQuantity", quantity);
            wegs.set("fromDate", UtilDateTime.nowTimestamp());
            wegs.set("workEffortGoodStdTypeId", "PRUN_PROD_PRODUCED");
            wegs.create();

            result.put("quantity", quantity);
            return result;
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Reverting a production Run.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> revertProductionRunAndSaveAllParts(DispatchContext ctx, Map<String, Object> context) {

        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // Mandatory input fields
        String productionRunId = (String) context.get("productionRunId");
        Debug.logInfo("Revert all parts of production run [" + productionRunId + "].", MODULE);

        try {
            // Assembles all parts that require reverting from WorkEffortInventoryAssign
            // we make a List of Map, with workEffortId, InventoryItem and quantity to be reverted
            Set<String> workEffortIdSet = FastSet.newInstance();
            WorkEffortSearch.getAllSubWorkEffortIds(productionRunId, workEffortIdSet, delegator, null);
            List<String> workEffortIds = UtilMisc.toList(workEffortIdSet);

            List<Map> savedParts = FastList.newInstance();
            for (String workEffortId : workEffortIds) {
                Map savedPart = UtilMisc.toMap("workEffortId", workEffortId);
                List<GenericValue> parts = delegator.findByAnd("WorkEffortInventoryAssign", savedPart);
                if (UtilValidate.isNotEmpty(parts)) {
                    for (GenericValue part : parts) {
                        savedPart = UtilMisc.toMap("workEffortId", workEffortId);
                        savedPart.put("inventoryItemId", part.getString("inventoryItemId"));
                        savedPart.put("quantity", part.getBigDecimal("quantity"));
                        savedParts.add(savedPart);
                    }
                } else {
                    savedParts.add(savedPart);
                }
            }
            Debug.logInfo("All the part to revert are " + savedParts, MODULE);

            // calls revertProductionRun
            Map input = UtilMisc.toMap("userLogin", userLogin);
            input.put("productionRunId", productionRunId);
            input.put("savedParts", savedParts);
            return dispatcher.runSync("revertProductionRun", input);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Revert only part of the production run.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> revertProductionRun(DispatchContext ctx, Map<String, Object> context) {

        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map result = ServiceUtil.returnSuccess();

        // Mandatory input fields
        String productionRunId = (String) context.get("productionRunId");
        Debug.logInfo("Revert part of the production run [" + productionRunId + "].", MODULE);
        List<Map> savedParts = (List) context.get("savedParts");

        result.put("productionRunId", productionRunId);
        result.put("savedParts", savedParts);

        try {

            Map input = null;
            // Create an InventoryItemDetail from the workEffortInventoryAssign
            // remove the WorkEffortInventoryAssign if its quantity is 0
            for (Map savedPart : savedParts) {
                String inventoryItemId = (String) savedPart.get("inventoryItemId");
                String workEffortId = (String) savedPart.get("workEffortId");
                GenericValue workEffortInventoryAssign = delegator.findByPrimaryKey("WorkEffortInventoryAssign", UtilMisc.toMap("workEffortId", workEffortId, "inventoryItemId", inventoryItemId));

                if (UtilValidate.isEmpty(workEffortInventoryAssign)) {
                    Debug.logInfo("WorkEffortInventoryAssign for part [inventoryItemId," + inventoryItemId + "],[workEffortId," + workEffortId + "] doesn't exist.", MODULE);
                } else {
                    BigDecimal partQuantity = (BigDecimal) savedPart.get("quantity");
                    BigDecimal workQuantity = workEffortInventoryAssign.getBigDecimal("quantity");
                    BigDecimal quantity = workQuantity;

                    if (partQuantity.compareTo(workQuantity) < 0) {
                        quantity = partQuantity;
                    }

                    input = UtilMisc.toMap("userLogin", userLogin);
                    input.put("inventoryItemId", inventoryItemId);
                    input.put("quantityOnHandDiff", quantity);
                    input.put("availableToPromiseDiff", quantity);
                    input.put("workEffortId", workEffortId);
                    Map output = dispatcher.runSync("createInventoryItemDetail", input);
                    if (ServiceUtil.isError(output) || ServiceUtil.isFailure(output)) {
                        return output;
                    }

                    if (partQuantity.compareTo(workQuantity) < 0) {
                        workEffortInventoryAssign.set("quantity", workQuantity.subtract(quantity));
                        workEffortInventoryAssign.store();
                    } else {
                        workEffortInventoryAssign.remove();
                    }

                }

                GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));

                if (UtilValidate.isEmpty(workEffort)) {
                    Debug.logInfo("WorkEffort for part [inventoryItemId," + inventoryItemId + "],[workEffortId," + workEffortId + "] doesn't exist.", MODULE);
                } else {
                    // Cancel or revert the production run tasks
                    // which has not been done or cancelled yet
                    if ("PRUN_COMPLETED".equals(workEffort.getString("currentStatusId"))
                        || "PRUN_CLOSED".equals(workEffort.getString("currentStatusId"))
                        || "PRUN_CANCELLED".equals(workEffort.getString("currentStatusId"))
                        || "PRUN_REVERTED".equals(workEffort.getString("currentStatusId"))) {
                        continue;
                    }

                    input = UtilMisc.toMap("userLogin", userLogin);
                    input.put("workEffortId", workEffort.getString("workEffortId"));
                    input.put("actualCompletionDate", UtilDateTime.nowTimestamp());

                    if ("PRUN_RUNNING".equals(workEffort.getString("currentStatusId"))) {
                        input.put("currentStatusId", "PRUN_REVERTED");
                    } else {
                        input.put("currentStatusId", "PRUN_CANCELLED");
                    }
                    Map output = dispatcher.runSync("updateWorkEffort", input);
                    if (ServiceUtil.isError(output) || ServiceUtil.isFailure(output)) {
                        return output;
                    }
                }
            }

            // Set production run to reverted
            input = UtilMisc.toMap("userLogin", userLogin);
            input.put("workEffortId", productionRunId);
            input.put("currentStatusId", "PRUN_REVERTED");
            input.put("actualCompletionDate", UtilDateTime.nowTimestamp());
            Map output = dispatcher.runSync("updateWorkEffort", input);
            if (ServiceUtil.isError(output) || ServiceUtil.isFailure(output)) {
                return output;
            }

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return result;
    }

    /**
     * Set the WEGS from date from the production run actual start date.
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> resetWegsFromDate(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String productionRunId = (String) context.get("productionRunId");

        try {
            GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
            List<GenericValue> wegss = productionRun.getRelated("WorkEffortGoodStandard");
            for (GenericValue wegs : wegss) {
                // since fromDate is part of the PK, first remove the entity and recreate it with the new fromDate
                wegs.remove();
                wegs.set("fromDate", UtilDateTime.nowTimestamp());
                wegs.create();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem setting the production run WEGS fromDate", MODULE);
            return ServiceUtil.returnError("Problem setting the production run WEGS fromDate");
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates a production run for a marketing package when the ordered item is out of stock (ATP
     * quantity less than zero).<br/>
     * The quantity produced is either the quantity ordered or to bring total ATP quantity of the
     * product back up to zero,  whichever is less.
     * NOTE: This is based on the ofbiz version of the same service, but uses the opentaps production run services
     * so that inventory is counted correctly.
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> createProductionRunForMktgPkg(DispatchContext ctx, Map<String, Object> context) {
        final String resource = "ManufacturingUiLabels";
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // Mandatory input fields
        String facilityId = (String) context.get("facilityId");
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");

        Map<String, Object> result = FastMap.newInstance();

        GenericValue orderItem = null;
        try {
            orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError("Error creating a production run for marketing package for order [" + orderId + " " + orderItemSeqId + "]: " + e.getMessage(), MODULE);
        }

        if (orderItem == null) {
            return UtilMessage.createAndLogServiceError("Error creating a production run for marketing package for order [" + orderId + " " + orderItemSeqId + "]: order item not found.", MODULE);
        }
        if (orderItem.get("quantity") == null) {
            Debug.logWarning("No quantity found for orderItem [" + orderItem + "], skipping production run of this marketing package", MODULE);
            return ServiceUtil.returnSuccess();
        }

        try {
            // first figure out how much of this product we already have in stock (ATP)
            Map<String, Object> tmpResults = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", orderItem.getString("productId"), "facilityId", facilityId, "userLogin", userLogin));
            BigDecimal existingAtp = (BigDecimal) tmpResults.get("availableToPromiseTotal");
            if (existingAtp == null) {
                existingAtp = BigDecimal.ZERO;
            }

            if (Debug.verboseOn()) {
                Debug.logVerbose("Order item [" + orderItem + "] Existing ATP = [" + existingAtp + "]", MODULE);
            }

            // we only need to produce more marketing packages if it is out of stock.  note that the ATP quantity already includes this order item
            if (existingAtp.compareTo(ZERO) < 0) {
                // how much should we produce?  If there already is some inventory, then just produce enough to bring ATP back up to zero, which may be less than the quantity ordered.
                // Otherwise, the ATP might be more negative due to previous orders, so just produce the quantity on this order
                Double qtyToProduce = Math.min((0 - existingAtp.doubleValue()), orderItem.getDouble("quantity"));
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Order quantity = [" + orderItem.getDouble("quantity").doubleValue() + "] quantity to produce = [" + qtyToProduce + "]", MODULE);
                }

                Map<String, Object> serviceContext = FastMap.newInstance();
                serviceContext.put("productId", orderItem.getString("productId"));
                serviceContext.put("pRQuantity", new BigDecimal(qtyToProduce));
                serviceContext.put("startDate", UtilDateTime.nowTimestamp());
                serviceContext.put("facilityId", facilityId);
                serviceContext.put("userLogin", userLogin);
                Map<String, Object> resultService = dispatcher.runSync("createProductionRun", serviceContext);

                String productionRunId = (String) resultService.get("productionRunId");
                result.put("productionRunId", productionRunId);

                try {

                    delegator.create("WorkOrderItemFulfillment", UtilMisc.toMap("workEffortId", productionRunId, "orderId", orderId, "orderItemSeqId", orderItemSeqId));

                } catch (GenericEntityException e) {
                    return UtilMessage.createAndLogServiceError("Error creating a production run for marketing package for order [" + orderId + " " + orderItemSeqId + "]: " + e.getMessage(), MODULE);
                }

                try {

                    serviceContext.clear();
                    serviceContext.put("productionRunId", productionRunId);
                    serviceContext.put("statusId", "PRUN_DOC_PRINTED");
                    serviceContext.put("userLogin", userLogin);
                    resultService = dispatcher.runSync("changeProductionRunStatus", serviceContext);

                    serviceContext.clear();
                    serviceContext.put("productionRunId", productionRunId);
                    serviceContext.put("userLogin", userLogin);
                    resultService = dispatcher.runSync("quickRunAllProductionRunTasks", serviceContext);

                    serviceContext.clear();
                    serviceContext.put("workEffortId", productionRunId);
                    serviceContext.put("userLogin", userLogin);
                    resultService = dispatcher.runSync("opentaps.productionRunProduce", serviceContext);

                    serviceContext.clear();
                    serviceContext.put("productionRunId", productionRunId);
                    serviceContext.put("statusId", "PRUN_CLOSED");
                    serviceContext.put("userLogin", userLogin);
                    resultService = dispatcher.runSync("changeProductionRunStatus", serviceContext);

                } catch (GenericServiceException e) {
                    return UtilMessage.createAndLogServiceError("ManufacturingProductionRunNotCreated", locale, MODULE);
                }

                result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunCreated", UtilMisc.toMap("productionRunId", productionRunId), locale));
                return result;

            } else {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Order item [" + orderItem + "] does not need to be produced - ATP is [" + existingAtp + "]", MODULE);
                }
                return ServiceUtil.returnSuccess();
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError("ManufacturingProductionRunNotCreated", locale, MODULE);
        }
    }

    /**
     * Replace legacy service of the same name.
     * Called if marketing package order item is canceled and disassemble product.
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> decomposeInventoryItem(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = UtilCommon.getLocale(context);

        String inventoryItemId = (String) context.get("inventoryItemId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");

        List<String> inventoryItemIds = FastList.newInstance();

        try {
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            if (inventoryItem == null) {
                return UtilMessage.createAndLogServiceError("WarehouseError_DecomposedInventoryNotFound", UtilMisc.toMap("inventoryItemId", inventoryItemId), locale, MODULE);
            }

            if (quantity == null) {
                quantity = inventoryItem.getBigDecimal("quantityOnHandTotal");
            }

            // use reverse assembly to decompose inventory item into its parts
            Map<String, Object> ctxt = FastMap.newInstance();
            GenericValue systemUser = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            ctxt.put("userLogin", systemUser);
            ctxt.put("disassemble", Boolean.TRUE);
            ctxt.put("productId", inventoryItem.getString("productId"));
            ctxt.put("quantity", quantity);
            ctxt.put("startDate", now);
            ctxt.put("facilityId", inventoryItem.getString("facilityId"));
            ctxt.put("routingId", "DEF_DISASMBL_TMP");
            ctxt.put("workEffortName", UtilMessage.expandLabel("ManufacturingDecomposingInventoryItem", locale, UtilMisc.toMap("productId", inventoryItem.getString("productId"), "inventoryItemId", inventoryItem.getString("inventoryItemId"))));
            Map<String, Object> results = dispatcher.runSync("opentaps.createProductionRun", ctxt, -1, false);
            String productionRunId = (String) results.get("productionRunId");

            // set the production run as confirmed
            ctxt.clear();
            ctxt.put("userLogin", userLogin);
            ctxt.put("productionRunId", productionRunId);
            ctxt.put("statusId", "PRUN_DOC_PRINTED");
            dispatcher.runSync("changeProductionRunStatus", ctxt, -1, false);

            List<GenericValue> tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"));
            GenericValue task = EntityUtil.getFirst(tasks);
            String taskId = task.getString("workEffortId");

            // clear and complete tasks
            ctxt.clear();
            ctxt.put("userLogin", userLogin);
            ctxt.put("productionRunId", productionRunId);
            ctxt.put("workEffortId", taskId);
            dispatcher.runSync("changeProductionRunTaskStatus", ctxt, -1, false);

            ctxt.clear();
            ctxt.put("userLogin", userLogin);
            ctxt.put("workEffortId", taskId);
            ctxt.put("productId", inventoryItem.getString("productId"));
            ctxt.put("quantity", quantity);
            dispatcher.runSync("issueProductionRunTaskComponent", ctxt, -1, false);

            ctxt.clear();
            ctxt.put("userLogin", userLogin);
            ctxt.put("productionRunId", productionRunId);
            ctxt.put("workEffortId", taskId);
            dispatcher.runSync("changeProductionRunTaskStatus", ctxt, -1, false);

            // produce the component parts
            List<GenericValue> wegs = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", productionRunId, "workEffortGoodStdTypeId", "PRUN_PROD_DELIV"));
            for (GenericValue productInfo : wegs) {
                ctxt.clear();
                ctxt.put("userLogin", userLogin);
                ctxt.put("workEffortId", productionRunId);
                ctxt.put("productId", productInfo.getString("productId"));
                ctxt.put("quantity", productInfo.getBigDecimal("estimatedQuantity"));
                results = dispatcher.runSync("opentaps.productionRunProduce", ctxt, -1, false);
                List<String> inventories = (List<String>) results.get("inventoryItemIds");
                if (UtilValidate.isNotEmpty(inventories)) {
                    inventoryItemIds.addAll(inventories);
                }
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e.getMessage(), MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e.getMessage(), MODULE);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("inventoryItemIds", inventoryItemIds);
        return result;
    }
}
