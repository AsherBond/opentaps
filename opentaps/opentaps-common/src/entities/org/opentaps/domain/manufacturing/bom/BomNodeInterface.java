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

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.domain.manufacturing.bom;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * A Bill of material node in a BOM tree.
 */
public interface BomNodeInterface {

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
    public void loadChildren(String routingId, String partBomTypeId, Date inDate, List<GenericValue> productFeatures, int type) throws GenericEntityException;

    /**
     * Loads this Node parents.
     *
     * @param partBomTypeId a <code>String</code> value
     * @param inDate a <code>Date</code> value
     * @param productFeatures a <code>List</code> value
     * @exception GenericEntityException if an error occurs
     */
    public void loadParents(String partBomTypeId, Date inDate, List<GenericValue> productFeatures) throws GenericEntityException;

    public BomNodeInterface configurator(GenericValue node, List<GenericValue> productFeatures, String productIdForRules, Date inDate) throws GenericEntityException;

    /**
     * Substitutes this Node with the given Node.
     *
     * @param oneChildNode the <code>BomNodeInterface</code> to use for substitution
     * @param productFeatures a <code>List</code> value
     * @param productPartRules a <code>List</code> value
     * @return the new <code>BomNodeInterface</code>
     * @exception GenericEntityException if an error occurs
     */
    public BomNodeInterface substituteNode(BomNodeInterface oneChildNode, List<GenericValue> productFeatures, List<GenericValue> productPartRules) throws GenericEntityException;

    /**
     * Gets this Node parent Node in the BOM tree.
     * @return the parent Node in the BOM tree
     */
    public BomNodeInterface getParentNode();

    /**
     * Gets the root Node.
     * @return an <code>BomNode</code> value
     */
    public BomNodeInterface getRootNode();

    /**
     * Sets this Node parent Node.
     * @param parentNode the parent Node
     */
    public void setParentNode(BomNodeInterface parentNode);

    /**
     * Debugs the Nodes by writing in the log.
     * @param quantity a <code>BigDecimal</code> value
     * @param depth an <code>int</code> value
     */
    public void debug(BigDecimal quantity, int depth);

    /**
     * Method used for TEST and DEBUG purposes.
     *
     * @param sb a <code>StringBuffer</code> value
     * @param quantity a <code>BigDecimal</code> value
     * @param depth an <code>int</code> value
     */
    public void print(StringBuffer sb, BigDecimal quantity, int depth);

    /**
     * Describe <code>print</code> method here.
     *
     * @param arr an <code>List</code> value
     * @param quantity a <code>BigDecimal</code> value
     * @param depth an <code>int</code> value
     * @param excludeWIPs a <code>boolean</code> value
     */
    public void print(List<BomNodeInterface> arr, BigDecimal quantity, int depth, boolean excludeWIPs);

    /**
     * Collects Nodes in the given <code>List</code> for which the product has a <code>shipmentBoxTypeId</code>.
     * @param nodes the <code>List</code> where to collect the Nodes
     * @param quantity a <code>BigDecimal</code> value
     * @param depth an <code>int</code> value
     * @param excludeWIPs a <code>boolean</code> value
     */
    public void getProductsInPackages(List<BomNodeInterface> nodes, BigDecimal quantity, int depth, boolean excludeWIPs);

    /**
     * Collects the Nodes info in the given <code>Map</code> as productId => node.
     * @param nodes a <code>Map</code> value
     */
    public void sumQuantity(Map<String, BomNodeInterface> nodes);

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
    public Map<String, Object> createManufacturingOrder(String facilityId, Date date, String workEffortName, String description, String routingId, String orderId, String orderItemSeqId, String shipmentId, boolean useSubstitute, boolean ignoreSupplierProducts) throws GenericEntityException;

    /**
     * Gets the minimum start date of all Nodes proposed requirements.
     *
     * @param facilityId a <code>String</code> value
     * @param requiredBydate a <code>Timestamp</code> value
     * @param allNodes a <code>boolean</code> value
     * @return a <code>Timestamp</code> value
     */
    public Timestamp getStartDate(String facilityId, Timestamp requiredBydate, boolean allNodes);

    /**
     * Returns false if the product of this BOM Node is of type "WIP" or if it has no ProductFacility records defined for it,
     * meaning that no active stock targets are set for this product.
     * @return if this Node product is managed in a warehouse
     */
    public boolean isWarehouseManaged();

    /**
     * A part is considered manufactured if it has child nodes AND unless ignoreSupplierProducts is set, if it also has no unexpired SupplierProducts defined.
     * @param ignoreSupplierProducts if set will not check for expired SupplierProducts
     * @return if this Node product is considered manufactured
     */
    public boolean isManufactured(boolean ignoreSupplierProducts);

    /**
     * By default, a part is manufactured if it has child nodes and it has NO SupplierProducts defined.
     * @return if this Node product is considered manufactured
     */
    public boolean isManufactured();

    /**
     * Checks if this Node product is virtual.
     * @return if this Node product is virtual
     */
    public boolean isVirtual();

    /**
     * Checks if this Node is completely configured or not by checking if the node is for a virtual product,
     * then recursively checking each children Node.
     * @param arr the not configured Nodes are added to this List
     */
    public void isConfigured(List<BomNodeInterface> arr);

    /**
     * Gets this Node quantity.
     * @return this Node quantity
     */
    public BigDecimal getQuantity();

    /**
     * Sets this Node quantity.
     * @param quantity this Node quantity
     */
    public void setQuantity(BigDecimal quantity);

    /**
     * Gets this Node depth.
     * @return this Node depth
     */
    public int getDepth();

    /**
     * Gets this Node product entity.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getProduct();

    /**
     * Gets this Node product id.
     * @return the product Id
     */
    public String getProductId();

    /**
     * Gets this Node substitutedNode if any.
     * @return this Node substitutedNode or <code>null</code>
     */
    public BomNodeInterface getSubstitutedNode();

    /**
     * Sets this Node substitutedNode.
     * @param substitutedNode this Node substitutedNode
     */
    public void setSubstitutedNode(BomNodeInterface substitutedNode);

    /**
     * Gets the product for rules for the parent of this node.
     * @return the product for rules for the parent of this nod
     */
    public String getRootProductForRules();

    /**
     * Gets the product for rules for this Node.
     * @return the product for rules for this Node
     */
    public String getProductForRules();

    /**
     * Sets the product for rules for this Node.
     * @param productForRules the Product For Rules
     */
    public void setProductForRules(String productForRules);

    /**
     * Gets the BOM type for this Node.
     * @return the BOM type for this Node
     */
    public String getBomTypeId();

    /**
     * Gets the quantity multiplier for this Node.
     * @return the quantity multiplier for this Node
     */
    public BigDecimal getQuantityMultiplier();

    /**
     * Sets the quantity multiplier for this Node.
     * @param quantityMultiplier the quantity multiplier
     */
    public void setQuantityMultiplier(BigDecimal quantityMultiplier);

    /**
     * Gets the rule applied for the Node, a <code>ProductManufacturingRule</code> entity.
     * @return the rule applied for the Node, a <code>ProductManufacturingRule</code> entity
     */
    public GenericValue getRuleApplied();

    /**
     * Sets the rule applied for the Node, a <code>ProductManufacturingRule</code> entity.
     * @param ruleApplied the rule applied, a <code>ProductManufacturingRule</code> entity
     */
    public void setRuleApplied(GenericValue ruleApplied);

    /**
     * Gets the scrap factor for this Node, defaults to 1.
     * @return the scrap factor for this Node, defaults to 1
     */
    public BigDecimal getScrapFactor();

    /**
     * Sets the scrap factor for this Node, defaults to 1.
     * @param scrapFactor the scrap factor
     */
    public void setScrapFactor(BigDecimal scrapFactor);

    /**
     * Gets the <code>List</code> of children Nodes for this Node.
     * @return the <code>List</code> of children Nodes for this Node
     */
    public List<BomNodeInterface> getChildrenNodes();

    /**
     * Sets the <code>List</code> of children Nodes for this Node.
     * @param childrenNodes <code>List</code> of children Nodes
     */
    public void setChildrenNodes(List<BomNodeInterface> childrenNodes);

    /**
     * Gets this Node <code>ProductAssoc</code>.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getProductAssoc();

    /**
     * Sets this Node <code>ProductAssoc</code>.
     * @param productAssoc a <code>GenericValue</code> value
     */
    public void setProductAssoc(GenericValue productAssoc);

    /**
     * Sets this Node related BOM Tree.
     * @param tree an <code>BomTree</code> value
     */
    public void setTree(BomTreeInterface tree);

    /**
     * Gets this Node related BOM Tree.
     * @return an <code>BomTree</code> value
     */
    public BomTreeInterface getTree();


}
