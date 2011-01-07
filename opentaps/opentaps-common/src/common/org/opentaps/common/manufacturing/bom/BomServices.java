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

package org.opentaps.common.manufacturing.bom;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.manufacturing.bom.BomNode;
import org.opentaps.domain.manufacturing.bom.BomNodeInterface;
import org.opentaps.domain.manufacturing.bom.BomTree;
import org.opentaps.domain.manufacturing.bom.BomTreeInterface;

/**
 * BOM related services.
 */
public final class BomServices {

    private static final String MODULE = BomServices.class.getName();

    private BomServices() { }

    /**
     * Service that builds a <code>BomTree</code> from the given parameters.
     * Useful for tree traversal (breakdown, explosion, implosion).
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code> containing "tree" => <code>BomTree</code>
     */
    @SuppressWarnings("unchecked")
    public static Map getBOMTree(DispatchContext dctx, Map context) {

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productId = (String) context.get("productId");
        String fromDateStr = (String) context.get("fromDate");
        String bomType = (String) context.get("bomType");
        Integer type = (Integer) context.get("type");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        BigDecimal amount = (BigDecimal) context.get("amount");
        String routingId = (String) context.get("routingId");

        if (type == null) {
            type = BomTree.EXPLOSION;
        }

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }

        BomTree tree = null;
        try {
            tree = new BomTree(productId, bomType, fromDate, type.intValue(), routingId, delegator, dispatcher, userLogin);
        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError("Error creating bill of materials tree: " + gee.getMessage(), MODULE);
        }
        if (tree != null) {
            if (quantity != null) {
                tree.setRootQuantity(quantity);
            }
            if (amount != null) {
                tree.setRootAmount(amount);
            }
        }

        Debug.logInfo("Debugging BomTree for product [" + productId + "] and routing [" + routingId + "]", MODULE);
        tree.debug();

        Map result = ServiceUtil.returnSuccess();
        result.put("tree", tree);
        return result;
    }

    /**
     * Service to read the product's bill of materials, if necessary configures it, and it returns its (possibly configured) components in a List of <code>BomNode</code>.
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map getManufacturingComponents(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        BigDecimal amount = (BigDecimal) context.get("amount");
        String fromDateStr = (String) context.get("fromDate");
        Boolean excludeWIPs = (Boolean) context.get("excludeWIPs");
        String routingId = (String) context.get("routingId");

        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }
        if (excludeWIPs == null) {
            excludeWIPs = Boolean.TRUE;
        }

        Map result = ServiceUtil.returnSuccess();

        //
        // Product routing
        //
        String workEffortId = null;
        try {
            Map routingInMap = UtilMisc.toMap("productId", productId, "ignoreDefaultRouting", "Y", "userLogin", userLogin);
            if (UtilValidate.isNotEmpty(routingId)) {
                routingInMap.put("workEffortId", routingId);
            }
            if (quantity != null) {
                routingInMap.put("quantity", quantity);
            }

            Map routingOutMap = dispatcher.runSync("getProductRouting", routingInMap);
            GenericValue routing = (GenericValue) routingOutMap.get("routing");
            if (routing == null) {
                // try to find a routing linked to the virtual product
                routingInMap = UtilMisc.toMap("productId", productId, "userLogin", userLogin);
                if (quantity != null) {
                    routingInMap.put("quantity", quantity);
                }
                routingOutMap = dispatcher.runSync("getProductRouting", routingInMap);
                routing = (GenericValue) routingOutMap.get("routing");
            }
            if (routing != null) {
                workEffortId = routing.getString("workEffortId");
                Debug.logInfo("getManufacturingComponents: using routing [" + routing.get("workEffortId") + "] : " + routing, MODULE);
            }
        } catch (GenericServiceException gse) {
            Debug.logWarning(gse.getMessage(), MODULE);
        }
        if (workEffortId != null) {
            result.put("workEffortId", workEffortId);
        }

        // if it is an assembly, and a routing was given, getProductRouting might have returned
        // the default template DEFAULT_ROUTING. In that case we allow to pass the given routing when getting the BOM
        // in order to get a specific BOM instead of the default BOM (should one be setup).
        // for a disassembly the default routing returned would be DEF_DISASMBL_TMP
        String bomRoutingId = workEffortId;
        if (UtilValidate.isNotEmpty(routingId) && "DEFAULT_ROUTING".equals(bomRoutingId)) {
            bomRoutingId = routingId;
        } else if (UtilValidate.isNotEmpty(routingId) && "DEF_DISASMBL_TMP".equals(bomRoutingId)) {
            bomRoutingId = routingId;
        }

        //
        // Components
        //
        BomTree tree = null;
        List<BomNodeInterface> components = new ArrayList<BomNodeInterface>();
        try {
            tree = new BomTree(productId, "MANUF_COMPONENT", fromDate, BomTree.EXPLOSION_SINGLE_LEVEL, bomRoutingId, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.setRootAmount(amount);
            Debug.logInfo("Debugging BomTree for product [" + productId + "] and routing [" + routingId + "]", MODULE);
            tree.debug();
            tree.print(components, excludeWIPs.booleanValue());
            // this removes the manufactured product from the list of components
            if (components.size() > 0) {
                components.remove(0);
            }
        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError("Error creating bill of materials tree: " + gee.getMessage(), MODULE);
        }

        result.put("components", components);

        // also return a componentMap (useful in scripts and simple language code)
        List componentsMap = new ArrayList();
        for (BomNodeInterface node : components) {
            Map componentMap = new HashMap();
            componentMap.put("product", node.getProduct());
            componentMap.put("quantity", node.getQuantity());
            componentsMap.add(componentMap);
        }
        result.put("componentsMap", componentsMap);
        return result;
    }

    /**
     * Service that updates the product's low level code (llc).
     * Given a product id, computes and updates the product's low level code (field billOfMaterialLevel in Product entity).
     * It also updates the llc of all the product's descendants.
     * For the llc only the manufacturing bom ("MANUF_COMPONENT") is considered.
     * @param dctx the service <code>DispatchContext</code>
     * @param context the service context <code>Map</code>
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map updateLowLevelCode(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get("productIdTo");
        Boolean alsoComponents = (Boolean) context.get("alsoComponents");
        if (alsoComponents == null) {
            alsoComponents = Boolean.TRUE;
        }
        Boolean alsoVariants = (Boolean) context.get("alsoVariants");
        if (alsoVariants == null) {
            alsoVariants = Boolean.TRUE;
        }

        Long llc = null;
        try {
            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
            Map depthResult = dispatcher.runSync("getMaxDepth", UtilMisc.toMap("productId", productId, "bomType", "MANUF_COMPONENT"));
            llc = (Long) depthResult.get("depth");
            // If the product is a variant of a virtual, then the billOfMaterialLevel cannot be
            // lower than the billOfMaterialLevel of the virtual product.
            List<GenericValue> virtualProductAssocs = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productIdTo", productId, "productAssocTypeId", "PRODUCT_VARIANT"));
            int virtualMaxDepth = 0;
            for (GenericValue virtualProductAssoc : virtualProductAssocs) {
                int virtualDepth = 0;
                GenericValue virtualProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", virtualProductAssoc.getString("productId")));
                if (virtualProduct.get("billOfMaterialLevel") != null) {
                    virtualDepth = virtualProduct.getLong("billOfMaterialLevel").intValue();
                } else {
                    virtualDepth = 0;
                }
                if (virtualDepth > virtualMaxDepth) {
                    virtualMaxDepth = virtualDepth;
                }
            }
            if (virtualMaxDepth > llc.intValue()) {
                llc = new Long(virtualMaxDepth);
            }
            product.set("billOfMaterialLevel", llc);
            product.store();
            if (alsoComponents.booleanValue()) {
                Map treeResult = dispatcher.runSync("getBOMTree", UtilMisc.toMap("productId", productId, "bomType", "MANUF_COMPONENT"));
                BomTree tree = (BomTree) treeResult.get("tree");
                List products = new ArrayList();
                tree.print(products, llc.intValue());
                for (int i = 0; i < products.size(); i++) {
                    BomNode node = (BomNode) products.get(i);
                    GenericValue nodeProduct = node.getProduct();
                    int lev = 0;
                    if (nodeProduct.get("billOfMaterialLevel") != null) {
                        lev = nodeProduct.getLong("billOfMaterialLevel").intValue();
                    }
                    if (lev < node.getDepth()) {
                        nodeProduct.set("billOfMaterialLevel", new Long(node.getDepth()));
                        nodeProduct.store();
                    }
                }
            }
            if (alsoVariants.booleanValue()) {
                List<GenericValue> variantProductAssocs = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "PRODUCT_VARIANT"));
                for (GenericValue variantProductAssoc : variantProductAssocs) {
                    GenericValue variantProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", variantProductAssoc.getString("productId")));
                    variantProduct.set("billOfMaterialLevel", llc);
                    variantProduct.store();
                }
            }
        } catch (Exception e) {
            return UtilMessage.createAndLogServiceError("Error running updateLowLevelCode: " + e.getMessage(), MODULE);
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("lowLevelCode", llc);
        return result;
    }

}
