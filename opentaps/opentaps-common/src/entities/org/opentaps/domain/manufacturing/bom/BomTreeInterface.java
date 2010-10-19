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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;

/**
 * A bill of material Tree.
 */
public interface BomTreeInterface {

    /**
     * Gets this Tree delegator instance.
     * @return a <code>Delegator</code> value
     */
    public Delegator getDelegator();

    /**
     * Gets this Tree dispatcher instance.
     * @return a <code>LocalDispatcher</code> value
     */
    public LocalDispatcher getDispatcher();

    /**
     * Gets this Tree user login instance.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getUserLogin();

    /**
     * Gets the routing ID for which the BOM is to be calculated.
     * @return a <code>String</code> value
     */
    public String getRoutingId();

    /**
     * Gets the Product that was given to build this BOM tree.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getInputProduct();

    /**
     * Gets the Product that this BOM tree id for, which might be different from the input Product.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getManufacturedProduct();

    /**
     * Checks if this Bill of Materials is completely configured or not.
     * Works by recursively checking each Node.
     * @return true if no virtual nodes (products) are present in the tree
     */
    public boolean isConfigured();

    /**
     * Gets the root quantity for this BOM Tree.
     * @return the root quantity for this BOM Tree
     */
    public BigDecimal getRootQuantity();

    /**
     * Sets the root quantity for this BOM Tree.
     * @param rootQuantity the root quantity
     */
    public void setRootQuantity(BigDecimal rootQuantity);

    /**
     * Gets the root amount for this BOM Tree.
     * @return the root amount for this BOM Tree
     */
    public BigDecimal getRootAmount();

    /**
     * Sets the root amount for this BOM Tree.
     * @param rootAmount the root amount
     */
    public void setRootAmount(BigDecimal rootAmount);

    /**
     * Gets the root Node for this BOM Tree.
     * @return  the root Node for this BOM Tree
     */
    public BomNodeInterface getRoot();

    /**
     * Gets the date considered for this BOM Tree.
     * @return the date considered for this BOM Tree
     */
    public Date getInDate();

    /**
     * Gets the BOM type for this BOM Tree.
     * @return the BOM type for this BOM Tree
     */
    public String getBomTypeId();

    /**
     * Debugs the Nodes by writing in the log.
     */
    public void debug();

    /**
     * Debug the Nodes in the given <code>StringBuffer</code>.
     * Method used for debug purposes.
     * @param sb the <code>StringBuffer</code> used to collect tree info
     */
    public void print(StringBuffer sb);

    /**
     * Collects info of each Node in the List.
     * Method used for bom breakdown (explosion/implosion).
     * @param arr the <code>List</code> used to collect tree info
     * @param initialDepth the depth of the root node
     */
    public void print(List<BomNodeInterface> arr, int initialDepth);

    /**
     * Collects info of each Node in the List.
     * Method used for bom breakdown (explosion/implosion).
     * @param arr the <code>List</code> used to collect tree info
     * @param initialDepth the depth of the root node
     * @param excludeWIPs if set to true will ignore WIP Nodes
     */
    public void print(List<BomNodeInterface> arr, int initialDepth, boolean excludeWIPs);

    /**
     * Collects info of its nodes in the List.
     * Method used for bom breakdown (explosion/implosion).
     * @param arr the <code>List</code> used to collect tree info
     */
    public void print(List<BomNodeInterface> arr);

    /**
     * Collects info of each Node in the List.
     * Method used for bom breakdown (explosion/implosion).
     * @param arr the <code>List</code> used to collect tree info
     * @param excludeWIPs if set to true will ignore WIP Nodes
     */
    public void print(List<BomNodeInterface> arr, boolean excludeWIPs);

    /**
     * Visits the bill of materials and collects info of its nodes in the given <code>Map</code>.
     * Method used for bom summarized explosion.
     * @param quantityPerNode The <code>Map</code> that will contain the summarized quantities per productId.
     */
    public void sumQuantities(Map<String, BomNodeInterface> quantityPerNode);

    /**
     * Collects all the productId in this Bill of Materials.
     * @return the <code>List</code> of productId contained in this Bill of Materials
     */
    public List<String> getAllProductsId();

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
    public String createManufacturingOrders(String facilityId, Date date, String workEffortName, String description, String routingId, String orderId, String orderItemSeqId, String shipmentId, GenericValue userLogin)  throws GenericEntityException;

    /**
     * Collects Nodes in the given <code>List</code> for which the product has a <code>shipmentBoxTypeId</code>.
     * @param nodes the <code>List</code> where to collect the Nodes
     */
    public void getProductsInPackages(List<BomNodeInterface> nodes);

}
