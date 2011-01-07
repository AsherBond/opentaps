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

package org.opentaps.domain.manufacturing.bom;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.manufacturing.mrp.ProposedOrder;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

/**
 * A custom BOM node to support multiple BOM.
 * It represents an (in-memory) component of a bill of materials.
 */
public class BomNode implements BomNodeInterface {

    private static final String MODULE = BomNode.class.getName();

    private LocalDispatcher dispatcher;
    private Delegator delegator;
    private GenericValue userLogin;

    private BomTreeInterface tree; // the tree to which this node belongs
    private BomNodeInterface parentNode; // the parent node (null if it's not present)
    private BomNodeInterface substitutedNode; // The virtual node (if any) that this instance substitutes
    private GenericValue ruleApplied; // The rule (if any) that that has been applied to configure the current node
    private String productForRules;
    private GenericValue product; // the current product (from Product entity)
    private GenericValue productAssoc; // the product assoc record (from ProductAssoc entity) in which the current product is in productIdTo
    private List<GenericValue> children = new ArrayList<GenericValue>(); // current node's children (ProductAssocs)
    private List<BomNodeInterface> childrenNodes = new ArrayList<BomNodeInterface>(); // current node's children nodes (BomNode)
    private BigDecimal quantityMultiplier = BigDecimal.ONE; // the necessary quantity as declared in the bom (from ProductAssocs or ProductManufacturingRule)
    private BigDecimal scrapFactor = BigDecimal.ONE; // the scrap factor as declared in the bom (from ProductAssocs)
    private int depth = 0; // the depth of this node in the current tree
    private BigDecimal quantity = BigDecimal.ZERO; // the quantity of this node in the current tree
    private String bomTypeId; // the type of the current tree

    /**
     * Creates a new <code>BomNode</code> instance.
     * @param product the node Product entity
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     */
    public BomNode(GenericValue product, LocalDispatcher dispatcher, GenericValue userLogin) {
        this.product = product;
        this.delegator = product.getDelegator();
        this.dispatcher = dispatcher;
        this.userLogin = userLogin;
    }

    /**
     * Creates a new <code>BomNode</code> instance.
     * @param product the node Product entity
     * @param tree the Tree in which this Node belongs
     */
    public BomNode(GenericValue product, BomTree tree) {
        this.product = product;
        this.setTree(tree);
        this.delegator = tree.getDelegator();
        this.dispatcher = tree.getDispatcher();
        this.userLogin = tree.getUserLogin();
    }

    /**
     * Creates a new <code>BomNode</code> instance.
     * @param productId the node Product entity ID
     * @param tree the Tree in which this Node belongs
     * @exception GenericEntityException if an error occurs
     */
    public BomNode(String productId, BomTree tree) throws GenericEntityException {
        this.setTree(tree);
        this.delegator = tree.getDelegator();
        this.dispatcher = tree.getDispatcher();
        this.userLogin = tree.getUserLogin();
        this.product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
    }

    /**
     * Creates a new <code>BomNode</code> instance.
     * @param productId the node Product entity ID
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @exception GenericEntityException if an error occurs
     */
    public BomNode(String productId, Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin) throws GenericEntityException {
        this(delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId)), dispatcher, userLogin);
    }

    /**
     * Loads this Node children.
     *
     * @param routingId the specific routing ID, optional, if not <code>null</code> loads the alternate BOM related to the given routing ID
     * @param partBomTypeId a <code>String</code> value
     * @param inDate a <code>Date</code> value
     * @param productFeatures a <code>List</code> value
     * @param type an <code>int</code> value
     * @exception GenericEntityException if an error occurs
     */
    public void loadChildren(String routingId, String partBomTypeId, Date inDate, List<GenericValue> productFeatures, int type) throws GenericEntityException {
        if (product == null) {
            throw new GenericEntityException("product is null");
        }
        // If the date is null, set it to today.
        if (inDate == null) {
            inDate = new Date();
        }
        bomTypeId = partBomTypeId;

        // find the components of this Node product, if a routing is specified get the components specific to this routing, else
        // we are building the default BOM and it that case get the components where the routing is null
        List<GenericValue> rows = getProductAssocs(product.getString("productId"), routingId, partBomTypeId, inDate);
        if (UtilValidate.isEmpty(rows) && substitutedNode != null) {
            // If no child is found and this is a substituted node
            // we try to search for substituted node's children.
            rows = getProductAssocs(substitutedNode.getProduct().getString("productId"), routingId, partBomTypeId, inDate);
        }

        Debug.logInfo("loadChildren found: " + rows, MODULE);

        // if that did not work either, then drop the specificRoutingWorkEffortId
        if (UtilValidate.isEmpty(rows) && UtilValidate.isNotEmpty(routingId)) {
             rows = getProductAssocs(product.getString("productId"), null, partBomTypeId, inDate);
             if (UtilValidate.isEmpty(rows) && substitutedNode != null) {
                 // If no child is found and this is a substituted node
                 // we try to search for substituted node's children.
                 rows = getProductAssocs(substitutedNode.getProduct().getString("productId"), null, partBomTypeId, inDate);
             }
             Debug.logInfo("loadChildren found without specific routing: " + rows, MODULE);
        }

        children = new ArrayList<GenericValue>(rows);

        // reload children Nodes
        childrenNodes.clear();
        BomNodeInterface node = null;
        for (GenericValue child : children) {
            // Configurator
            node = configurator(child, productFeatures, getRootNode().getProductForRules(), inDate);
            // If the node is null this means that the node has been discarded by the rules.
            if (node != null) {
                node.setParentNode(this);
                switch (type) {
                case BomTree.EXPLOSION:
                    node.loadChildren(routingId, partBomTypeId, inDate, productFeatures, BomTree.EXPLOSION);
                    break;
                case BomTree.EXPLOSION_MANUFACTURING:
                    // for manfacturing trees, do not look through and create production runs for children unless there is no warehouse stocking of this node item
                    if (!node.isWarehouseManaged()) {
                        node.loadChildren(routingId, partBomTypeId, inDate, productFeatures, type);
                    }
                    break;
                default:
                    // nothing to be done here
                    break;
                }
            }
            childrenNodes.add(node);
        }
    }

    /**
     * Substitutes this Node with the given Node.
     *
     * @param oneChildNode the <code>BomNodeInterface</code> to use for substitution
     * @param productFeatures a <code>List</code> value
     * @param productPartRules a <code>List</code> value
     * @return the new <code>BomNodeInterface</code>
     * @exception GenericEntityException if an error occurs
     */
    public BomNodeInterface substituteNode(BomNodeInterface oneChildNode, List<GenericValue> productFeatures, List<GenericValue> productPartRules) throws GenericEntityException {
        if (productPartRules != null) {
            for (GenericValue rule : productPartRules) {
                String ruleCondition = rule.getString("productFeature");
                String ruleOperator = rule.getString("ruleOperator");
                String newPart = rule.getString("productIdInSubst");
                BigDecimal ruleQuantity = BigDecimal.ZERO;
                try {
                    ruleQuantity = rule.getBigDecimal("quantity");
                } catch (Exception exc) {
                    ruleQuantity = BigDecimal.ZERO;
                }

                boolean ruleSatisfied = false;
                if (ruleCondition == null || ruleCondition.equals("")) {
                    ruleSatisfied = true;
                } else {
                    if (productFeatures != null) {
                        for (GenericValue feature : productFeatures) {
                            if (ruleCondition.equals(feature.get("productFeatureId"))) {
                                ruleSatisfied = true;
                                break;
                            }
                        }
                    }
                }
                if (ruleSatisfied && ruleOperator.equals("OR")) {
                    BomNodeInterface tmpNode = oneChildNode;
                    if (newPart == null || newPart.equals("")) {
                        oneChildNode = null;
                    } else {
                        BomNodeInterface origNode = oneChildNode;
                        oneChildNode = new BomNode(newPart, delegator, dispatcher, userLogin);
                        oneChildNode.setTree(tree);
                        oneChildNode.setSubstitutedNode(tmpNode);
                        oneChildNode.setRuleApplied(rule);
                        oneChildNode.setProductAssoc(origNode.getProductAssoc());
                        oneChildNode.setScrapFactor(origNode.getScrapFactor());
                        if (ruleQuantity.signum() > 0) {
                            oneChildNode.setQuantityMultiplier(ruleQuantity);
                        } else {
                            oneChildNode.setQuantityMultiplier(origNode.getQuantityMultiplier());
                        }
                    }
                    break;
                }
                // FIXME: AND operator still not implemented
            } // end of for

        }
        return oneChildNode;
    }

    public BomNodeInterface configurator(GenericValue node, List<GenericValue> productFeatures, String productIdForRules, Date inDate) throws GenericEntityException {
        BomNodeInterface oneChildNode = new BomNode((String) node.get("productIdTo"), delegator, dispatcher, userLogin);
        oneChildNode.setTree(tree);
        oneChildNode.setProductAssoc(node);
        try {
            oneChildNode.setQuantityMultiplier(node.getBigDecimal("quantity"));
        } catch (Exception nfe) {
            oneChildNode.setQuantityMultiplier(BigDecimal.ONE);
        }
        try {
            BigDecimal percScrapFactor = node.getBigDecimal("scrapFactor");
            BigDecimal hundred = new BigDecimal(100);
            // A negative scrap factor is a salvage factor
            if (percScrapFactor.compareTo(hundred.negate()) > 0 && percScrapFactor.compareTo(hundred) < 0) {
                percScrapFactor = BigDecimal.ONE.add(percScrapFactor.divide(hundred));
            } else {
                Debug.logWarning("A scrap factor of [" + percScrapFactor + "] was ignored", MODULE);
                percScrapFactor = BigDecimal.ONE;
            }
            oneChildNode.setScrapFactor(percScrapFactor);
        } catch (Exception nfe) {
            oneChildNode.setScrapFactor(BigDecimal.ONE);
        }
        BomNodeInterface newNode = oneChildNode;
        // CONFIGURATOR
        if (oneChildNode.isVirtual()) {
            // If the part is VIRTUAL and
            // productFeatures and productPartRules are not null
            // we have to substitute the part with the right part's variant
            List productPartRules = delegator.findByAnd("ProductManufacturingRule",
                                                    UtilMisc.toMap("productId", productIdForRules,
                                                    "productIdFor", node.get("productId"),
                                                    "productIdIn", node.get("productIdTo")));
            if (substitutedNode != null) {
                productPartRules.addAll(delegator.findByAnd("ProductManufacturingRule",
                                                    UtilMisc.toMap("productId", productIdForRules,
                                                    "productIdFor", substitutedNode.getProduct().getString("productId"),
                                                    "productIdIn", node.get("productIdTo"))));
            }
            productPartRules = EntityUtil.filterByDate(productPartRules, inDate);
            newNode = substituteNode(oneChildNode, productFeatures, productPartRules);
            if (newNode == oneChildNode) {
                // If no substitution has been done (no valid rule applied),
                // we try to search for a generic link-rule
                List genericLinkRules = delegator.findByAnd("ProductManufacturingRule",
                                                        UtilMisc.toMap("productIdFor", node.get("productId"),
                                                        "productIdIn", node.get("productIdTo")));
                if (substitutedNode != null) {
                    genericLinkRules.addAll(delegator.findByAnd("ProductManufacturingRule",
                                                        UtilMisc.toMap("productIdFor", substitutedNode.getProduct().getString("productId"),
                                                        "productIdIn", node.get("productIdTo"))));
                }
                genericLinkRules = EntityUtil.filterByDate(genericLinkRules, inDate);
                newNode = null;
                newNode = substituteNode(oneChildNode, productFeatures, genericLinkRules);
                if (newNode == oneChildNode) {
                    // If no substitution has been done (no valid rule applied),
                    // we try to search for a generic node-rule
                    List genericNodeRules = delegator.findByAnd("ProductManufacturingRule",
                                                            UtilMisc.toMap("productIdIn", node.get("productIdTo")),
                                                            UtilMisc.toList("ruleSeqId"));
                    genericNodeRules = EntityUtil.filterByDate(genericNodeRules, inDate);
                    newNode = null;
                    newNode = substituteNode(oneChildNode, productFeatures, genericNodeRules);
                    if (newNode == oneChildNode) {
                        // If no substitution has been done (no valid rule applied),
                        // we try to set the default (first) node-substitution
                        //if (genericNodeRules != null && genericNodeRules.size() > 0) {
                            // FIXME
                            //...
                        //}
                        // -----------------------------------------------------------
                        // We try to apply directly the selected features
                        if (newNode == oneChildNode) {
                            Map selectedFeatures = new HashMap();
                            if (productFeatures != null) {
                                GenericValue feature = null;
                                for (int j = 0; j < productFeatures.size(); j++) {
                                    feature = (GenericValue) productFeatures.get(j);
                                    selectedFeatures.put(feature.get("productFeatureTypeId"), feature.get("productFeatureId")); // FIXME
                                }
                            }

                            if (selectedFeatures.size() > 0) {
                                Map context = new HashMap();
                                context.put("productId", node.get("productIdTo"));
                                context.put("selectedFeatures", selectedFeatures);
                                Map storeResult = null;
                                GenericValue variantProduct = null;
                                try {
                                    storeResult = dispatcher.runSync("getProductVariant", context);
                                    List variantProducts = (List) storeResult.get("products");
                                    if (variantProducts.size() == 1) {
                                        variantProduct = (GenericValue) variantProducts.get(0);
                                    }
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, "Error calling getProductVariant service", MODULE);
                                }
                                if (variantProduct != null) {
                                    newNode = new BomNode(variantProduct, dispatcher, userLogin);
                                    newNode.setTree(tree);
                                    newNode.setSubstitutedNode(oneChildNode);
                                    newNode.setQuantityMultiplier(oneChildNode.getQuantityMultiplier());
                                    newNode.setScrapFactor(oneChildNode.getScrapFactor());
                                    newNode.setProductAssoc(oneChildNode.getProductAssoc());
                                }
                            }

                        }
                        // -----------------------------------------------------------
                    }
                }
            }
        } // end of if (isVirtual())
        return newNode;
    }

    @SuppressWarnings("unchecked")
    public void loadParents(String partBomTypeId, Date inDate, List productFeatures) throws GenericEntityException {
        if (product == null) {
            throw new GenericEntityException("product is null");
        }
        // If the date is null, set it to today.
        if (inDate == null) {
            inDate = new Date();
        }

        bomTypeId = partBomTypeId;
        // Delegator delegator = product.getDelegator();
        List rows = delegator.findByAnd("ProductAssoc",
                                            UtilMisc.toMap("productIdTo", product.get("productId"),
                                                       "productAssocTypeId", partBomTypeId),
                                            UtilMisc.toList("sequenceNum"));
        rows = EntityUtil.filterByDate(rows, inDate);
        if ((UtilValidate.isEmpty(rows)) && substitutedNode != null) {
            // If no parent is found and this is a substituted node
            // we try to search for substituted node's parents.
            rows = delegator.findByAnd("ProductAssoc",
                                        UtilMisc.toMap("productIdTo", substitutedNode.getProduct().get("productId"),
                                                       "productAssocTypeId", partBomTypeId),
                                        UtilMisc.toList("sequenceNum"));
            rows = EntityUtil.filterByDate(rows, inDate);
        }
        children = new ArrayList<GenericValue>(rows);
        childrenNodes = new ArrayList<BomNodeInterface>();
        // build the children Nodes from the found ProductAssoc
        BomNodeInterface childNode = null;
        for (GenericValue child : children) {
            childNode = new BomNode(child.getString("productId"), delegator, dispatcher, userLogin);
            // Configurator
            //oneChildNode = configurator(oneChild, productFeatures, getRootNode().getProductForRules(), delegator);
            // If the node is null this means that the node has been discarded by the rules.
            if (childNode != null) {
                childNode.setParentNode(this);
                childNode.setTree(tree);
                childNode.loadParents(partBomTypeId, inDate, productFeatures);
            }
            childrenNodes.add(childNode);
        }
    }

    /**
     * Gets this Node parent Node in the BOM tree.
     * @return the parent Node in the BOM tree
     */
    public BomNodeInterface getParentNode() {
        return parentNode;
    }

    /**
     * Gets the root Node.
     * @return a <code>BomNodeInterface</code> value
     */
    public BomNodeInterface getRootNode() {
        if (parentNode != null) {
            // TODO: this seems not correct
            return getParentNode();
        } else {
            return this;
        }
    }

    /**
     * Sets this Node parent Node.
     * @param parentNode the parent Node
     */
    public void setParentNode(BomNodeInterface parentNode) {
        this.parentNode = parentNode;
    }

    /**
     * Debugs the Nodes by writing in the log.
     * @param quantity a <code>BigDecimal</code> value
     * @param depth an <code>int</code> value
     */
    public void debug(BigDecimal quantity, int depth) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < depth; i++) {
            sb.append(" ");
        }
        sb.append(product.get("productId"));
        sb.append(" - ");
        sb.append("" + quantity);
        Debug.logInfo(sb.toString(), MODULE);

        GenericValue oneChild = null;
        BomNodeInterface oneChildNode = null;
        depth++;
        for (int i = 0; i < children.size(); i++) {
            oneChild = children.get(i);
            BigDecimal bomQuantity = BigDecimal.ZERO;
            try {
                bomQuantity = oneChild.getBigDecimal("quantity");
            } catch (Exception exc) {
                bomQuantity = BigDecimal.ONE;
            }
            oneChildNode = childrenNodes.get(i);
            if (oneChildNode != null) {
                oneChildNode.debug(quantity.multiply(bomQuantity), depth);
            }
        }
    }

    /**
     * Method used for TEST and DEBUG purposes.
     *
     * @param sb a <code>StringBuffer</code> value
     * @param quantity a <code>BigDecimal</code> value
     * @param depth an <code>int</code> value
     */
    public void print(StringBuffer sb, BigDecimal quantity, int depth) {
        for (int i = 0; i < depth; i++) {
            sb.append("<b>&nbsp;*&nbsp;</b>");
        }
        sb.append(product.get("productId"));
        sb.append(" - ");
        sb.append("" + quantity);
        GenericValue oneChild = null;
        BomNodeInterface oneChildNode = null;
        depth++;
        for (int i = 0; i < children.size(); i++) {
            oneChild = children.get(i);
            BigDecimal bomQuantity = BigDecimal.ZERO;
            try {
                bomQuantity = oneChild.getBigDecimal("quantity");
            } catch (Exception exc) {
                bomQuantity = BigDecimal.ONE;
            }
            oneChildNode = childrenNodes.get(i);
            sb.append("<br/>");
            if (oneChildNode != null) {
                oneChildNode.print(sb, quantity.multiply(bomQuantity), depth);
            }
        }
    }

    /**
     * Describe <code>print</code> method here.
     *
     * @param arr an <code>List</code> value
     * @param quantity a <code>BigDecimal</code> value
     * @param depth an <code>int</code> value
     * @param excludeWIPs a <code>boolean</code> value
     */
    @SuppressWarnings("unchecked")
    public void print(List<BomNodeInterface> arr, BigDecimal quantity, int depth, boolean excludeWIPs) {
        // Now we set the depth and quantity of the current node
        // in this breakdown.
        this.depth = depth;
        String serviceName = null;
        if (this.productAssoc != null && this.productAssoc.getString("estimateCalcMethod") != null) {
            try {
                GenericValue genericService = productAssoc.getRelatedOne("CustomMethod");
                if (genericService != null && genericService.getString("customMethodName") != null) {
                    serviceName = genericService.getString("customMethodName");
                }
            } catch (Exception exc) {
                Debug.logError(exc, "Error executing the CustomMethod service.", MODULE);
            }
        }
        if (serviceName != null) {
            Map arguments = UtilMisc.toMap("neededQuantity", quantity.multiply(quantityMultiplier).doubleValue(), "amount", (tree != null ? tree.getRootAmount().doubleValue() : 0.0));
            Double width = null;
            if (getProduct().get("productWidth") != null) {
                width = getProduct().getDouble("productWidth");
            }
            if (width == null) {
                width = new Double(0);
            }
            arguments.put("width", width);
            Map inputContext = UtilMisc.toMap("arguments", arguments, "userLogin", userLogin);
            try {
                Map result = dispatcher.runSync(serviceName, inputContext);
                BigDecimal calcQuantity = (BigDecimal) result.get("quantity");
                if (calcQuantity != null) {
                    this.quantity = calcQuantity;
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the getManufacturingComponents service", MODULE);
            }
        } else {
            this.quantity = quantity.multiply(quantityMultiplier).multiply(scrapFactor);
        }
        // First of all we visit the current node.
        arr.add(this);
        // Now (recursively) we visit the children.
        depth++;
        for (BomNodeInterface node : childrenNodes) {
            if (excludeWIPs && "WIP".equals(node.getProduct().getString("productTypeId"))) {
                continue;
            }
            if (node != null) {
                node.print(arr, this.quantity, depth, excludeWIPs);
            }
        }
    }

    /**
     * Collects Nodes in the given <code>List</code> for which the product has a <code>shipmentBoxTypeId</code>.
     * @param nodes the <code>List</code> where to collect the Nodes
     * @param quantity a <code>BigDecimal</code> value
     * @param depth an <code>int</code> value
     * @param excludeWIPs a <code>boolean</code> value
     */
    public void getProductsInPackages(List<BomNodeInterface> nodes, BigDecimal quantity, int depth, boolean excludeWIPs) {
        // Now we set the depth and quantity of the current node
        // in this breakdown.
        this.depth = depth;
        this.quantity = quantity.multiply(quantityMultiplier).multiply(scrapFactor);
        // First of all we visit the current node.
        if (this.getProduct().getString("shipmentBoxTypeId") != null) {
            nodes.add(this);
        } else {
            depth++;
            for (BomNodeInterface node : childrenNodes) {
                if (excludeWIPs && "WIP".equals(node.getProduct().getString("productTypeId"))) {
                    continue;
                }
                if (node != null) {
                    node.getProductsInPackages(nodes, this.quantity, depth, excludeWIPs);
                }
            }
        }
    }

    /**
     * Collects the Nodes info in the given <code>Map</code> as productId => node.
     * @param nodes a <code>Map</code> value
     */
    public void sumQuantity(Map<String, BomNodeInterface> nodes) {
        // First of all, we try to fetch a node with the same partId
        BomNodeInterface sameNode = nodes.get(product.getString("productId"));
        // If the node is not found we create a new node for the current product
        if (sameNode == null) {
            sameNode = new BomNode(product, dispatcher, userLogin);
            nodes.put(product.getString("productId"), sameNode);
        }

        // Now we add the current quantity to the node
        sameNode.setQuantity(sameNode.getQuantity().add(quantity));

        // Now (recursively) we visit the children.
        for (BomNodeInterface node : childrenNodes) {
            if (node != null) {
                node.sumQuantity(nodes);
            }
        }
    }

    /**
     * Creates a manufacturing order for each of the nodes that needs to be manufactured.
     *
     * @param facilityId a <code>String</code> value
     * @param date a <code>Date</code> value
     * @param workEffortName a <code>String</code> value
     * @param description a <code>String</code> value
     * @param routingId a <code>String</code> value
     * @param orderId a <code>String</code> value
     * @param orderItemSeqId a <code>String</code> value
     * @param shipmentId a <code>String</code> value
     * @param useSubstitute a <code>boolean</code> value
     * @param ignoreSupplierProducts a <code>boolean</code> value
     * @return a <code>Map</code> of "productionRunId" -> productionRunId, "endDate" -> endDate representing the created production run
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createManufacturingOrder(String facilityId, Date date, String workEffortName, String description, String routingId, String orderId, String orderItemSeqId, String shipmentId, boolean useSubstitute, boolean ignoreSupplierProducts) throws GenericEntityException {
        String productionRunId = null;
        Timestamp endDate = null;
        if (isManufactured(ignoreSupplierProducts)) {
            List<String> childProductionRuns = new ArrayList<String>();
            Timestamp maxEndDate = null;
            for (BomNodeInterface node : childrenNodes) {
                if (node != null) {
                    Map tmpResult = node.createManufacturingOrder(facilityId, date, null, null, routingId, null, null, shipmentId, false, false);
                    String childProductionRunId = (String) tmpResult.get("productionRunId");
                    Timestamp childEndDate = (Timestamp) tmpResult.get("endDate");
                    if (maxEndDate == null) {
                        maxEndDate = childEndDate;
                    }
                    if (childEndDate != null && maxEndDate.compareTo(childEndDate) < 0) {
                        maxEndDate = childEndDate;
                    }

                    if (childProductionRunId != null) {
                        childProductionRuns.add(childProductionRunId);
                    }
                }
            }

            // prepare parameters and call the createProductionRun service for this Node
            Timestamp startDate = UtilDateTime.toTimestamp(UtilDateTime.toDateTimeString(date));
            Map<String, Object> serviceContext = new HashMap<String, Object>();
            if (!useSubstitute) {
                serviceContext.put("productId", getProduct().getString("productId"));
                serviceContext.put("facilityId", getProduct().getString("facilityId"));
            } else {
                serviceContext.put("productId", getSubstitutedNode().getProduct().getString("productId"));
                serviceContext.put("facilityId", getSubstitutedNode().getProduct().getString("facilityId"));
            }
            if (UtilValidate.isNotEmpty(facilityId)) {
                serviceContext.put("facilityId", facilityId);
            }
            if (UtilValidate.isNotEmpty(workEffortName)) {
                serviceContext.put("workEffortName", workEffortName);
            }
            if (UtilValidate.isNotEmpty(description)) {
                serviceContext.put("description", description);
            }
            if (UtilValidate.isNotEmpty(routingId)) {
                serviceContext.put("routingId", routingId);
            }
            if (UtilValidate.isNotEmpty(shipmentId) && UtilValidate.isEmpty(workEffortName)) {
                serviceContext.put("workEffortName", "SP_" + shipmentId + "_" + serviceContext.get("productId"));
            }

            serviceContext.put("pRQuantity", getQuantity());
            if (UtilValidate.isNotEmpty(maxEndDate)) {
                serviceContext.put("startDate", maxEndDate);
            } else {
                serviceContext.put("startDate", startDate);
            }
            serviceContext.put("userLogin", userLogin);
            Map resultService = null;
            try {
                Debug.logInfo("createManufacturingOrder: createProductionRun with parameters: " + serviceContext, MODULE);
                resultService = dispatcher.runSync("createProductionRun", serviceContext);
                productionRunId = (String) resultService.get("productionRunId");
                endDate = (Timestamp) resultService.get("estimatedCompletionDate");
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the createProductionRun service", MODULE);
            }
            try {
                if (productionRunId != null) {
                    if (orderId != null && orderItemSeqId != null) {
                        delegator.create("WorkOrderItemFulfillment", UtilMisc.toMap("workEffortId", productionRunId, "orderId", orderId, "orderItemSeqId", orderItemSeqId));
                    }
                    for (String childProductionRunId : childProductionRuns) {
                        delegator.create("WorkEffortAssoc", UtilMisc.toMap("workEffortIdFrom", childProductionRunId, "workEffortIdTo", productionRunId, "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY", "fromDate", startDate));
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem calling the getManufacturingComponents service", MODULE);
            }
        }
        // return the created production run and end date, which would be null if the node is not manufactured
        return UtilMisc.toMap("productionRunId", productionRunId, "endDate", endDate);
    }

    /**
     * Gets the minimum start date of all Nodes proposed requirements.
     *
     * @param facilityId a <code>String</code> value
     * @param requiredBydate a <code>Timestamp</code> value
     * @param allNodes a <code>boolean</code> value
     * @return a <code>Timestamp</code> value
     */
    public Timestamp getStartDate(String facilityId, Timestamp requiredBydate, boolean allNodes) {
        Timestamp minStartDate = requiredBydate;
        if ("WIP".equals(getProduct().getString("productTypeId")) || allNodes) {
            ProposedOrder proposedOrder = new ProposedOrder(getProduct(), facilityId, facilityId, true, requiredBydate, getQuantity());
            proposedOrder.calculateStartDate(0, null, delegator, dispatcher, userLogin);
            Timestamp startDate = proposedOrder.getRequirementStartDate();
            minStartDate = startDate;
            for (BomNodeInterface node : childrenNodes) {
                if (node != null) {
                    Timestamp childStartDate = node.getStartDate(facilityId, startDate, false);
                    if (childStartDate.compareTo(minStartDate) < 0) {
                        minStartDate = childStartDate;
                    }
                }
            }
        }
        return minStartDate;
    }

    /**
     * Returns false if the product of this BOM Node is of type "WIP" or if it has no ProductFacility records defined for it,
     * meaning that no active stock targets are set for this product.
     * @return if this Node product is managed in a warehouse
     */
    public boolean isWarehouseManaged() {
        boolean isWarehouseManaged = false;
        try {
            if ("WIP".equals(getProduct().getString("productTypeId"))) {
                return false;
            }
            List<GenericValue> pfs = getProduct().getRelatedCache("ProductFacility");
            if (UtilValidate.isNotEmpty(pfs)) {
                isWarehouseManaged = true;
            } else {
                if (getSubstitutedNode() != null && getSubstitutedNode().getProduct() != null) {
                    pfs = getSubstitutedNode().getProduct().getRelatedCache("ProductFacility");
                }
                if (UtilValidate.isNotEmpty(pfs)) {
                    isWarehouseManaged = true;
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee, "Problem in BomNode.isWarehouseManaged()", MODULE);
        }
        return isWarehouseManaged;
    }

    /**
     * A part is considered manufactured if it has child nodes AND unless ignoreSupplierProducts is set, if it also has no unexpired SupplierProducts defined.
     * @param ignoreSupplierProducts if set will not check for expired SupplierProducts
     * @return if this Node product is considered manufactured
     */
    public boolean isManufactured(boolean ignoreSupplierProducts) {
        List<GenericValue> supplierProducts = null;
        if (!ignoreSupplierProducts) {
            try {
                supplierProducts = product.getRelated("SupplierProduct", UtilMisc.toMap("supplierPrefOrderId", "10_MAIN_SUPPL"), UtilMisc.toList("minimumOrderQuantity"));
            } catch (GenericEntityException gee) {
                Debug.logError(gee, "Problem in BomNode.isManufactured()", MODULE);
            }
            supplierProducts = EntityUtil.filterByDate(supplierProducts, UtilDateTime.nowTimestamp(), "availableFromDate", "availableThruDate", true);
        }
        return childrenNodes.size() > 0 && (ignoreSupplierProducts || UtilValidate.isEmpty(supplierProducts));
    }

    /**
     * By default, a part is manufactured if it has child nodes and it has NO SupplierProducts defined.
     * @return if this Node product is considered manufactured
     */
    public boolean isManufactured() {
        return isManufactured(false);
    }

    /**
     * Checks if this Node product is virtual.
     * @return if this Node product is virtual
     */
    public boolean isVirtual() {
        return "Y".equals(product.get("isVirtual"));
    }

    /**
     * Checks if this Node is completely configured or not by checking if the node is for a virtual product,
     * then recursively checking each children Node.
     * @param arr the not configured Nodes are added to this List
     */
    public void isConfigured(List<BomNodeInterface> arr) {
        // First of all we visit the current node.
        if (isVirtual()) {
            arr.add(this);
        }
        // Now (recursively) we visit the children.
        for (BomNodeInterface node : childrenNodes) {
            if (node != null) {
                node.isConfigured(arr);
            }
        }
    }

    /**
     * Gets this Node quantity.
     * @return this Node quantity
     */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * Sets this Node quantity.
     * @param quantity this Node quantity
     */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    /**
     * Gets this Node depth.
     * @return this Node depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Gets this Node product entity.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getProduct() {
        return product;
    }

    /**
     * Gets this Node product id.
     * @return the product Id
     */
    public String getProductId() {
        return product.getString("productId");
    }

    /**
     * Gets this Node substitutedNode if any.
     * @return this Node substitutedNode or <code>null</code>
     */
    public BomNodeInterface getSubstitutedNode() {
        return substitutedNode;
    }

    /**
     * Sets this Node substitutedNode.
     * @param substitutedNode this Node substitutedNode
     */
    public void setSubstitutedNode(BomNodeInterface substitutedNode) {
        this.substitutedNode = substitutedNode;
    }

    /**
     * Gets the product for rules for the parent of this node.
     * @return the product for rules for the parent of this nod
     */
    public String getRootProductForRules() {
        return getParentNode().getProductForRules();
    }

    /**
     * Gets the product for rules for this Node.
     * @return the product for rules for this Node
     */
    public String getProductForRules() {
        return productForRules;
    }

    /**
     * Sets the product for rules for this Node.
     * @param productForRules the Product For Rules
     */
    public void setProductForRules(String productForRules) {
        this.productForRules = productForRules;
    }

    /**
     * Gets the BOM type for this Node.
     * @return the BOM type for this Node
     */
    public String getBomTypeId() {
        return bomTypeId;
    }

    /**
     * Gets the quantity multiplier for this Node.
     * @return the quantity multiplier for this Node
     */
    public BigDecimal getQuantityMultiplier() {
        return quantityMultiplier;
    }

    /**
     * Sets the quantity multiplier for this Node.
     * @param quantityMultiplier the quantity multiplier
     */
    public void setQuantityMultiplier(BigDecimal quantityMultiplier) {
        this.quantityMultiplier = quantityMultiplier;
    }

    /**
     * Gets the rule applied for the Node, a <code>ProductManufacturingRule</code> entity.
     * @return the rule applied for the Node, a <code>ProductManufacturingRule</code> entity
     */
    public GenericValue getRuleApplied() {
        return ruleApplied;
    }

    /**
     * Sets the rule applied for the Node, a <code>ProductManufacturingRule</code> entity.
     * @param ruleApplied the rule applied, a <code>ProductManufacturingRule</code> entity
     */
    public void setRuleApplied(GenericValue ruleApplied) {
        this.ruleApplied = ruleApplied;
    }

    /**
     * Gets the scrap factor for this Node, defaults to 1.
     * @return the scrap factor for this Node, defaults to 1
     */
    public BigDecimal getScrapFactor() {
        return scrapFactor;
    }

    /**
     * Sets the scrap factor for this Node, defaults to 1.
     * @param scrapFactor the scrap factor
     */
    public void setScrapFactor(BigDecimal scrapFactor) {
        this.scrapFactor = scrapFactor;
    }

    /**
     * Gets the <code>List</code> of children Nodes for this Node.
     * @return the <code>List</code> of children Nodes for this Node
     */
    public List<BomNodeInterface> getChildrenNodes() {
        return childrenNodes;
    }

    /**
     * Sets the <code>List</code> of children Nodes for this Node.
     * @param childrenNodes <code>List</code> of children Nodes
     */
    public void setChildrenNodes(List<BomNodeInterface> childrenNodes) {
        this.childrenNodes = childrenNodes;
    }

    /**
     * Gets this Node <code>ProductAssoc</code>.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getProductAssoc() {
        return productAssoc;
    }

    /**
     * Sets this Node <code>ProductAssoc</code>.
     * @param productAssoc a <code>GenericValue</code> value
     */
    public void setProductAssoc(GenericValue productAssoc) {
        this.productAssoc = productAssoc;
    }

    /**
     * Sets this Node related BOM Tree.
     * @param tree an <code>BomTree</code> value
     */
    public void setTree(BomTreeInterface tree) {
        this.tree = tree;
    }

    /**
     * Gets this Node related BOM Tree.
     * @return an <code>BomTree</code> value
     */
    public BomTreeInterface getTree() {
        return tree;
    }

    private List<GenericValue> getProductAssocs(String productId, String routingId, String partBomTypeId, Date inDate) throws GenericEntityException {
        List<GenericValue> rows = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", partBomTypeId, "specificRoutingWorkEffortId", routingId), UtilMisc.toList("sequenceNum"));
        return EntityUtil.filterByDate(rows, inDate);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BomNode for ");
        if (product == null) {
            sb.append(" no product.");
            return sb.toString();
        }
        sb.append("product [").append(product.getString("productId")).append("]");
        sb.append(" quantity = ").append(quantity);
        sb.append(" scrapFactor = ").append(scrapFactor);
        sb.append(" bomTypeId = ").append(bomTypeId);
        sb.append(" depth = ").append(depth);
        return sb.toString();
    }

}
