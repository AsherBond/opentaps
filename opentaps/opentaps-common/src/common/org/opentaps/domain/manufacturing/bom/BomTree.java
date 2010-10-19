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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.LocalDispatcher;

/**
 * A custom BOM tree to support multiple BOM.
 * It represents an (in-memory) bill of materials (in which each component is an BomNode)
 * Useful for tree traversal (breakdown, explosion, implosion).
 */
public class BomTree implements BomTreeInterface {

    /** Builds the BOM from the input product and each of its component, a downward visit is performed (explosion). */
    public static final int EXPLOSION = 0;
    /** Builds the BOM from the input product only, a single level explosion is performed. TODO: this seems to be not supported. */
    public static final int EXPLOSION_SINGLE_LEVEL = 1;
    /** Builds the BOM from the input product and each of its component, a downward visit is performed (explosion), including only the product that needs manufacturing. */
    public static final int EXPLOSION_MANUFACTURING = 2;
    /** Builds the BOM from the input product and each product it is a component of, an upward visit is performed. */
    public static final int IMPLOSION = 3;

    private LocalDispatcher dispatcher;
    private Delegator delegator;
    private GenericValue userLogin;

    // same as BOMTree, need to redefine
    private BomNodeInterface root;
    private BigDecimal rootQuantity;
    private BigDecimal rootAmount;
    private Date inDate;
    private String bomTypeId;
    private GenericValue inputProduct;
    private GenericValue manufacturedProduct;
    private String routingId;

    /**
     * Creates a new instance of <code>BomTree</code> by reading downward
     * the productId's bill of materials (explosion).
     * If virtual products are found, it tries to configure them by running
     * the Product Configurator.
     * @param productId the product for which we want to get the bom
     * @param bomTypeId the bill of materials type (e.g. manufacturing, engineering, ...)
     * @param inDate validity date (if null, today is used)
     * @param delegator the delegator used
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @exception GenericEntityException If a db problem occurs
     *
     */
    public BomTree(String productId, String bomTypeId, Date inDate, Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin) throws GenericEntityException {
        this(productId, bomTypeId, inDate, EXPLOSION, delegator, dispatcher, userLogin);
    }

    /**
     * Creates a new instance of <code>BOMTree</code> by reading
     * the productId's bill of materials (upward or downward).
     * If virtual products are found, it tries to configure them by running
     * the Product Configurator.
     * @param productId The product for which we want to get the bom.
     * @param bomTypeId The bill of materials type (e.g. manufacturing, engineering, ...)
     * @param inDate Validity date (if null, today is used).
     *
     * @param type if equals to EXPLOSION, a downward visit is performed (explosion);
     * if equals to EXPLOSION_SINGLE_LEVEL, a single level explosion is performed;
     * if equals to EXPLOSION_MANUFACTURING, a downward visit is performed (explosion), including only the product that needs manufacturing;
     * if equals to IMPLOSION an upward visit is done (implosion);
     *
     * @param delegator The delegator used.
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @exception GenericEntityException If a db problem occurs.
     *
     */
    public BomTree(String productId, String bomTypeId, Date inDate, int type, Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin) throws GenericEntityException {
        this(productId, bomTypeId, inDate, type, null, delegator, dispatcher, userLogin);
    }

    /**
     * Creates a new instance of <code>BOMTree</code> by reading
     * the productId's bill of materials (upward or downward).
     * If virtual products are found, it tries to configure them by running
     * the Product Configurator.
     * @param productId The product for which we want to get the bom.
     * @param bomTypeId The bill of materials type (e.g. manufacturing, engineering, ...)
     * @param inDate Validity date (if null, today is used).
     *
     * @param type if equals to <ul>
     * <li>EXPLOSION, a downward visit is performed (explosion);
     * <li>EXPLOSION_SINGLE_LEVEL, a single level explosion is performed;
     * <li>EXPLOSION_MANUFACTURING, a downward visit is performed (explosion), including only the product that needs manufacturing;
     * <li>IMPLOSION an upward visit is done (implosion);
     * </ul>
     *
     * @param routingId a <code>WorkEffort</code> ID corresponding to the chosen routing which can define an alternative BOM.
     *
     * @param delegator The delegator used.
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @exception GenericEntityException If a db problem occurs.
     */
    @SuppressWarnings("unchecked")
    public BomTree(String productId, String bomTypeId, Date inDate, int type, String routingId, Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin) throws GenericEntityException {

        // If the parameters are not valid, return.
        if (productId == null || bomTypeId == null || delegator == null || dispatcher == null) {
            return;
        }

        // If the date is null, set it to today.
        if (inDate == null) {
            inDate = new Date();
        }

        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.userLogin = userLogin;

        this.routingId = routingId;

        inputProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));

        String productIdForRules = productId;
        // The selected product features are loaded
        List productFeaturesAppl = delegator.findByAnd("ProductFeatureAppl", UtilMisc.toMap("productId", productId, "productFeatureApplTypeId", "STANDARD_FEATURE"));
        List productFeatures = new ArrayList();
        GenericValue oneProductFeatureAppl = null;
        for (int i = 0; i < productFeaturesAppl.size(); i++) {
            oneProductFeatureAppl = (GenericValue) productFeaturesAppl.get(i);
            productFeatures.add(delegator.findByPrimaryKey("ProductFeature", UtilMisc.toMap("productFeatureId", oneProductFeatureAppl.getString("productFeatureId"))));

        }

        // If the product is manufactured as a different product, load the new product
        GenericValue manufacturedAsProduct = manufacturedAsProduct(productId, inDate);
        // We load the information about the product that needs to be manufactured from Product entity
        GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", (manufacturedAsProduct != null ? manufacturedAsProduct.getString("productIdTo") : productId)));
        if (product == null) {
            return;
        }
        BomNodeInterface originalNode = new BomNode(product, dispatcher, userLogin);
        originalNode.setTree(this);

        // If the product hasn't a bill of materials we try to retrieve
        // the bill of materials of its virtual product (if the current
        // product is variant).
        if (!hasBom(product, inDate)) {
            List virtualProducts = product.getRelatedByAnd("AssocProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"));
            if (virtualProducts != null && virtualProducts.size() > 0) {
                virtualProducts = EntityUtil.filterByDate(virtualProducts, inDate);
                if (virtualProducts != null && virtualProducts.size() > 0) {
                    GenericValue virtualProduct = (GenericValue) virtualProducts.get(0);
                    // If the virtual product is manufactured as a different product,
                    // load the new product
                    productIdForRules = virtualProduct.getString("productId");
                    manufacturedAsProduct = manufacturedAsProduct(virtualProduct.getString("productId"), inDate);
                    if (manufacturedAsProduct != null) {
                        product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", manufacturedAsProduct.getString("productIdTo")));
                    } else {
                        product = virtualProduct;
                    }
                }
            }
        }

        // If still no product is found for the BOM return now
        if (product == null) {
            return;
        }

        // set the product for which this BOM is actually for
        manufacturedProduct = product;

        // Load the BOM Tree
        try {
            root = new BomNode(product, dispatcher, userLogin);
            root.setTree(this);
            root.setProductForRules(productIdForRules);
            root.setSubstitutedNode(originalNode);
            if (type == IMPLOSION) {
                root.loadParents(bomTypeId, inDate, productFeatures);
            } else {
                root.loadChildren(routingId, bomTypeId, inDate, productFeatures, type);
            }
        } catch (GenericEntityException gee) {
            root = null;
        }
        this.bomTypeId = bomTypeId;
        this.inDate = inDate;
        rootQuantity = BigDecimal.ONE;
        rootAmount = BigDecimal.ZERO;
    }

    /**
     * Gets this Tree delegator instance.
     * @return a <code>Delegator</code> value
     */
    public Delegator getDelegator() {
        return delegator;
    }

    /**
     * Gets this Tree dispatcher instance.
     * @return a <code>LocalDispatcher</code> value
     */
    public LocalDispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * Gets this Tree user login instance.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getUserLogin() {
        return userLogin;
    }

    /**
     * Gets the routing ID for which the BOM is to be calculated.
     * @return a <code>String</code> value
     */
    public String getRoutingId() {
        return routingId;
    }

    /**
     * Gets the Product that was given to build this BOM tree.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getInputProduct() {
        return inputProduct;
    }

    /**
     * Gets the Product that this BOM tree id for, which might be different from the input Product.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getManufacturedProduct() {
        return manufacturedProduct;
    }

    /**
     * Gets the first <code>Product</code> entity for which the given product is manufactured as for the given date.
     * This is configured as a PRODUCT_MANUFACTURED <code>ProductAssoc</code>.
     * @param productId a <code>String</code> value
     * @param inDate a <code>Date</code> value
     * @return a <code>GenericValue</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private GenericValue manufacturedAsProduct(String productId, Date inDate) throws GenericEntityException {
        List manufacturedAsProducts = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "PRODUCT_MANUFACTURED"));
        manufacturedAsProducts = EntityUtil.filterByDate(manufacturedAsProducts, inDate);
        GenericValue manufacturedAsProduct = null;
        if (manufacturedAsProducts != null && manufacturedAsProducts.size() > 0) {
            manufacturedAsProduct = (GenericValue) manufacturedAsProducts.get(0);
        }
        return manufacturedAsProduct;
    }

    /**
     * Checks if the given <code>Product</code> has a Bill of Materials for the given date.
     *
     * @param product a <code>GenericValue</code> value
     * @param inDate a <code>Date</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private boolean hasBom(GenericValue product, Date inDate) throws GenericEntityException {
        List children = product.getRelatedByAnd("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", bomTypeId));
        children = EntityUtil.filterByDate(children, inDate);
        return (children != null && children.size() > 0);
    }

    /**
     * Checks if this Bill of Materials is completely configured or not.
     * Works by recursively checking each Node.
     * @return true if no virtual nodes (products) are present in the tree
     */
    public boolean isConfigured() {
        List<BomNodeInterface> notConfiguredParts = new ArrayList<BomNodeInterface>();
        root.isConfigured(notConfiguredParts);
        return (notConfiguredParts.size() == 0);
    }

    /**
     * Gets the root quantity for this BOM Tree.
     * @return the root quantity for this BOM Tree
     */
    public BigDecimal getRootQuantity() {
        return rootQuantity;
    }

    /**
     * Sets the root quantity for this BOM Tree.
     * @param rootQuantity the root quantity
     */
    public void setRootQuantity(BigDecimal rootQuantity) {
        this.rootQuantity = rootQuantity;
        // cascade the quantity to the nodes according to the BOM
        print(new ArrayList<BomNodeInterface>());
    }

    /**
     * Gets the root amount for this BOM Tree.
     * @return the root amount for this BOM Tree
     */
    public BigDecimal getRootAmount() {
        return rootAmount;
    }

    /**
     * Sets the root amount for this BOM Tree.
     * @param rootAmount the root amount
     */
    public void setRootAmount(BigDecimal rootAmount) {
        this.rootAmount = rootAmount;
    }

    /**
     * Gets the root Node for this BOM Tree.
     * @return  the root Node for this BOM Tree
     */
    public BomNodeInterface getRoot() {
        return root;
    }

    /**
     * Gets the date considered for this BOM Tree.
     * @return the date considered for this BOM Tree
     */
    public Date getInDate() {
        return inDate;
    }

    /**
     * Gets the BOM type for this BOM Tree.
     * @return the BOM type for this BOM Tree
     */
    public String getBomTypeId() {
        return bomTypeId;
    }

    /**
     * Debugs the Nodes by writing in the log.
     */
    public void debug() {
        if (root != null) {
            root.debug(getRootQuantity(), 0);
        }
    }

    /**
     * Debug the Nodes in the given <code>StringBuffer</code>.
     * Method used for debug purposes.
     * @param sb the <code>StringBuffer</code> used to collect tree info
     */
    public void print(StringBuffer sb) {
        if (root != null) {
            root.print(sb, getRootQuantity(), 0);
        }
    }

    /**
     * Collects info of each Node in the List.
     * Method used for bom breakdown (explosion/implosion).
     * @param arr the <code>List</code> used to collect tree info
     * @param initialDepth the depth of the root node
     */
    public void print(List<BomNodeInterface> arr, int initialDepth) {
        print(arr, initialDepth, true);
    }

    /**
     * Collects info of each Node in the List.
     * Method used for bom breakdown (explosion/implosion).
     * @param arr the <code>List</code> used to collect tree info
     * @param initialDepth the depth of the root node
     * @param excludeWIPs if set to true will ignore WIP Nodes
     */
    public void print(List<BomNodeInterface> arr, int initialDepth, boolean excludeWIPs) {
        if (root != null) {
            root.print(arr, getRootQuantity(), initialDepth, excludeWIPs);
        }
    }

    /**
     * Collects info of its nodes in the List.
     * Method used for bom breakdown (explosion/implosion).
     * @param arr the <code>List</code> used to collect tree info
     */
    public void print(List<BomNodeInterface> arr) {
        print(arr, 0, false);
    }

    /**
     * Collects info of each Node in the List.
     * Method used for bom breakdown (explosion/implosion).
     * @param arr the <code>List</code> used to collect tree info
     * @param excludeWIPs if set to true will ignore WIP Nodes
     */
    public void print(List<BomNodeInterface> arr, boolean excludeWIPs) {
        print(arr, 0, excludeWIPs);
    }

    /**
     * Visits the bill of materials and collects info of its nodes in the given <code>Map</code>.
     * Method used for bom summarized explosion.
     * @param quantityPerNode The <code>Map</code> that will contain the summarized quantities per productId.
     */
    public void sumQuantities(Map<String, BomNodeInterface> quantityPerNode) {
        if (root != null) {
            root.sumQuantity(quantityPerNode);
        }
    }

    /**
     * Collects all the productId in this Bill of Materials.
     * @return the <code>List</code> of productId contained in this Bill of Materials
     */
    public List<String> getAllProductsId() {
        List<BomNodeInterface> nodes = new ArrayList<BomNodeInterface>();
        List<String> productsId = new ArrayList<String>();
        // collect nodes in a List
        print(nodes);
        for (BomNodeInterface node : nodes) {
            productsId.add(node.getProduct().getString("productId"));
        }
        return productsId;
    }

    /**
     * Creates a manufacturing order for each of the nodes that needs to be manufactured.
     * @param facilityId a <code>String</code> value
     * @param date a <code>Date</code> value
     * @param workEffortName a <code>String</code> value
     * @param description a <code>String</code> value
     * @param routingId a <code>String</code> value
     * @param orderId the (sales) order id for which the manufacturing orders are created. If specified (together with orderItemSeqId) a link between the two order lines is created. If null, no link is created.
     * @param orderItemSeqId the (sales) order item id for which the manufacturing orders are created. If specified (together with orderId) a link between the two order lines is created. If null, no link is created.
     * @param shipmentId a <code>String</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @return the created work effort ID
     * @exception GenericEntityException If a db problem occurs
     */
    public String createManufacturingOrders(String facilityId, Date date, String workEffortName, String description, String routingId, String orderId, String orderItemSeqId, String shipmentId, GenericValue userLogin)  throws GenericEntityException {
        String workEffortId = null;
        if (root != null) {
            if (UtilValidate.isEmpty(facilityId)) {
                if (orderId != null) {
                    GenericValue order = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
                    String productStoreId = order.getString("productStoreId");
                    if (productStoreId != null) {
                        GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
                        if (productStore != null) {
                            facilityId = productStore.getString("inventoryFacilityId");
                        }
                    }

                }
                if (facilityId == null && shipmentId != null) {
                    GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
                    facilityId = shipment.getString("originFacilityId");
                }
            }
            Map tmpMap = root.createManufacturingOrder(facilityId, date, workEffortName, description, routingId, orderId, orderItemSeqId, shipmentId, true, true);
            workEffortId = (String) tmpMap.get("productionRunId");
        }
        return workEffortId;
    }

    /**
     * Collects Nodes in the given <code>List</code> for which the product has a <code>shipmentBoxTypeId</code>.
     * @param nodes the <code>List</code> where to collect the Nodes
     */
    public void getProductsInPackages(List<BomNodeInterface> nodes) {
        if (root != null) {
            root.getProductsInPackages(nodes, getRootQuantity(), 0, false);
        }
    }

}
