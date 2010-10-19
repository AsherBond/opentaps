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
package org.opentaps.purchasing.order;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.CheckOutEvents;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.opentaps.common.order.OrderEvents;
import org.opentaps.common.order.shoppingcart.OpentapsShoppingCart;
import org.opentaps.common.order.shoppingcart.OpentapsShoppingCartHelper;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * PurchasingOrderEvents - Order creation events (http servlets) specific to purchasing
 * TODO: refactor errors to use UtilMessage.createAndLogEventError()
 * TODO: This is a copy of CRMSFA order events, so we need to refactor the common code into opentaps-common.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 12 $
 */
public final class PurchasingOrderEvents {

    private PurchasingOrderEvents() { }

    private static final String MODULE = PurchasingOrderEvents.class.getName();

    /**
     * Event method called to initialize the cart after a form is submitted or link is clicked.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String initializeCart(HttpServletRequest request, HttpServletResponse response) {
        // initialize the cart in the session
        purchasingGetOrInitializeCart(request);
        return "success";
    }

    /**
     * Method to get a cart in purchasing.
     * @param request a <code>HttpServletRequest</code> value
     * @return an <code>OpentapsShoppingCart</code> value
     */
    public static OpentapsShoppingCart getCartObject(HttpServletRequest request) {
        HttpSession session = request.getSession(true);

        // if one already exists, return it
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
        if (cart == null) {
            return null;
        }
        if (cart instanceof OpentapsShoppingCart) {
            return (OpentapsShoppingCart) cart;
        } else {
            OpentapsShoppingCart opentapsCart = new OpentapsShoppingCart(cart);
            session.setAttribute("shoppingCart", opentapsCart);
            return opentapsCart;
        }
    }

    /**
     * Method to get or initialize a cart in purchasing.
     * @param request a <code>HttpServletRequest</code> value
     * @return an <code>OpentapsShoppingCart</code> value
     */
    public static OpentapsShoppingCart purchasingGetOrInitializeCart(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        // if one already exists, return it
        OpentapsShoppingCart cart = getCartObject(request);
        if (cart != null) {
            return cart;
        }

        // this is a "dummy" storeId -- purchase orders don't really need one but the cart won't work without one
        String productStoreId = "PURCHASING";
        session.setAttribute("productStoreId", productStoreId);

        // initialize a new cart
        String webSiteId = null;
        String billFromVendorPartyId = UtilCommon.getParameter(request, "supplierPartyId"); // this is the supplier partyId
        if (billFromVendorPartyId == null) {
            billFromVendorPartyId = UtilCommon.getParameter(request, "supplierPartyId_o_0"); // get it from the requirement multi form
        }
        String billToCustomerPartyId = UtilCommon.getOrganizationPartyId(request);
        if (UtilValidate.isEmpty(billToCustomerPartyId)) {
            throw new IllegalArgumentException("No organization found in session.  Cannot create purchase order.");
        }
        String currencyUomId = UtilCommon.getOrgBaseCurrency(billToCustomerPartyId, delegator);
        if (UtilValidate.isEmpty(currencyUomId)) {
            throw new IllegalArgumentException("No currency found for organization [" + billToCustomerPartyId + "].  Cannot create purchase order.");
        }
        String facilityId = UtilCommon.getParameter(request, "facilityId");
        if (facilityId == null) {
            facilityId = UtilCommon.getParameter(request, "facilityId_o_0"); // get it from the requirement multi form
        }

        OpentapsShoppingCart opentapsCart = new OpentapsShoppingCart(delegator, productStoreId, webSiteId, UtilHttp.getLocale(request), currencyUomId, billToCustomerPartyId, billFromVendorPartyId);
        // validate the supplierPartyId
        GenericValue supplier = null;
        try {
            supplier = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", billFromVendorPartyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (supplier == null) {
            throw new IllegalArgumentException("No supplier found [" + billFromVendorPartyId + "].  Cannot create purchase order.");
        }

        opentapsCart.setOrderPartyId(billFromVendorPartyId);  // XXX NOTE: The supplierPartyId is the order party for POs (this is a ShoppingCart quirk)
        opentapsCart.setOrderType("PURCHASE_ORDER");
        opentapsCart.setFacilityId(facilityId);
        session.setAttribute("shoppingCart", opentapsCart);

        // erase any pre-existing tracking code
        session.removeAttribute("trackingCodeId");

        return opentapsCart;
    }

    /**
     * Event method to destroy the cart in purchasing.
     *
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String purchasingDestroyCart(HttpServletRequest request, HttpServletResponse response) {
        return OrderEvents.destroyCart(request, response, "purchasing");
    }

    /**
     * Counts the matching products with the same condition used in findMatchingSupplierProducts.bsh.
     * This is here because we want to redirect to entry if none found.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String countMatchingProducts(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        ShoppingCart cart = purchasingGetOrInitializeCart(request);
        String searchString = UtilCommon.getParameter(request, "productId");
        EntityCondition where =  EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition(EntityOperator.OR,
                        EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("productId"), EntityOperator.LIKE, EntityFunction.UPPER(searchString + "%")),
                        EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("supplierProductId"), EntityOperator.LIKE, EntityFunction.UPPER(searchString + "%")),
                        EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("idValue"), EntityOperator.LIKE, EntityFunction.UPPER(searchString + "%"))),
                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, cart.getOrderPartyId()),
                        EntityUtil.getFilterByDateExpr("availableFromDate", "availableThruDate"));
        try {
            List<GenericValue> matches = delegator.findByAnd("SupplierProductGoodIdentification", where);
            if (matches.size() == 0) {
                UtilMessage.addFieldError(request, "productId", "OpentapsError_ProductNotFound", UtilMisc.toMap("productId", searchString));
                return "error";
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, UtilHttp.getLocale(request), MODULE);
        }
        return "success";
    }

    /**
     * Event method to construct automatic OrderRoles for CRMSFA purposes.  If this method detects that
     * the Financials commission system is implemented, it will check to see if the COMMISSION_AGENT role
     * should be created for the agent.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String createPurchasingOrderRoles(HttpServletRequest request, HttpServletResponse response) {
        // TODO: this does nothing for now
        return "success";
    }

    /**
     * Wrapper around the CheckOutEvents.createOrder() method.  This method is to be
     * used in purchasing instead.  The reason is we wish to perform some actions right after the order
     * is created as part of the order creation process.  This is the only sure way to hook up
     * pre-creation and post-creation logic without using controller request chains.
     *
     * No matter what kind of errors occur in our own post order creation logic, this method should not
     * abort with "error" on its own.  It should always return the results of CheckOutEvents.createOrder()
     * so that the controller chain may continue and do things like process payments and destroy the cart.
     *
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String purchasingCreateOrder(HttpServletRequest request, HttpServletResponse response) {
        String createOrderResult = CheckOutEvents.createOrder(request, response);
        if ("error".equals(createOrderResult)) {
            return createOrderResult;
        }

        // run post order creation logic
        try {
            postOrderCreation(request);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }

        // always return the result of CheckOutEvents.createOrder so the controller chain can continue, despite errors in our own logic here
        return createOrderResult;
    }

    @SuppressWarnings("unchecked")
    private static void postOrderCreation(HttpServletRequest request) throws GenericEntityException, GenericServiceException {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        // the cart should still be around because destroying it is the last part of the controller chain after this event
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");

        // Create WorkOrderItemFulfillment for MFG_CONTRACT items.  These are normally created by BOM system, but for
        // non-BOM tasks, such as outsourced, we need to do these by hand
        // also make the accounting tags from the cart item attributes
        for (Iterator<ShoppingCartItem> iter = cart.iterator(); iter.hasNext();) {
            ShoppingCartItem item = iter.next();
            String requirementId = item.getRequirementId();
            String orderItemSeqId = item.getOrderItemSeqId();

            // get the work efforts from the requirement to work effort map (see opentaps.createProductionRun)
            List<GenericValue> workEffortAssocs = delegator.findByAnd("WorkRequirementFulfillment", UtilMisc.toMap("requirementId", requirementId));
            for (Iterator<GenericValue> assocIter = workEffortAssocs.iterator(); assocIter.hasNext();) {
                GenericValue assoc = assocIter.next();
                String workEffortId = assoc.getString("workEffortId");
                Map<String, Object> input = UtilMisc.<String, Object>toMap("orderId", cart.getOrderId(), "orderItemSeqId", orderItemSeqId, "workEffortId", workEffortId);
                GenericValue fulfillment = delegator.findByPrimaryKey("WorkOrderItemFulfillment", input);
                if (fulfillment == null) {
                    fulfillment = delegator.makeValue("WorkOrderItemFulfillment", input);
                    fulfillment.create();
                }
            }

            // make the accounting tags from the cart item attributes
            GenericValue orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", cart.getOrderId(), "orderItemSeqId", orderItemSeqId));
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                if (item.getAttributes().containsKey("tag" + i)) {
                    orderItem.put("acctgTagEnumId" + i, item.getAttribute("tag" + i));
                }
            }
            orderItem.store();

            // set the extra requirements to closed
            Set<String> requirementIds = (Set<String>) item.getAttribute("requirementIds");
            if (UtilValidate.isNotEmpty(requirementIds)) {
                for (String reqId : requirementIds) {
                    // the requirement from item.getRequirementId() was already updated
                    if (reqId != null && reqId != requirementId) {
                        dispatcher.runSync("updateRequirement", UtilMisc.toMap("userLogin", userLogin, "requirementId", reqId, "statusId", "REQ_CLOSED"));
                    }
                }
            }
        }
    }

    /**
     * Event method that creates a cart for all approved requirements of a supplier party id and facilityId.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String createCartForAllSupplierRequirements(HttpServletRequest request, HttpServletResponse response) {

        // destroy the cart first, because we are creating an order from scratch
        String result = purchasingDestroyCart(request, response);
        if ("error".equals(result)) {
            return "error";
        }

        // flag to consolidate items or not, if set it will group the items by product id/required by date
        boolean groupItems = "Y".equals(UtilCommon.getParameter(request, "consolidateFlag"));

        // this should get a new cart with the correct supplierPartyId from the multi form
        OpentapsShoppingCart cart = purchasingGetOrInitializeCart(request);
        Delegator delegator = cart.getDelegator();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        EntityListIterator eli = null;

        try {
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, cart.getOrderPartyId()),
                    EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_APPROVED"),
                    EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "PRODUCT_REQUIREMENT"));
            TransactionUtil.begin();
            eli = delegator.findListIteratorByCondition("RequirementAndRole", conditions, null, UtilMisc.toList("productId", "requiredByDate"));
            TransactionUtil.commit();
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogEventError(request, ex, cart.getLocale(), MODULE);
        }

        Debug.logWarning("createCartForAllSupplierRequirements ...", MODULE);

        // loop through the requirements and group those for the same product in the same cart item
        if (eli != null) {

            GenericValue requirement = null;
            Timestamp groupDate = null;
            String groupProductId = null;
            BigDecimal groupQuantity = BigDecimal.ZERO;
            LinkedHashSet<String> requirementIds = new LinkedHashSet<String>();
            try {
                while ((requirement = eli.next()) != null) {

                    String requirementId = requirement.getString("requirementId");
                    String productId = requirement.getString("productId");
                    BigDecimal quantity = requirement.getBigDecimal("quantity");
                    Timestamp requiredByDate = requirement.getTimestamp("requiredByDate");

                    Debug.logInfo("createCartForAllSupplierRequirements, req [" + requirementId + "] of product " + productId + " x " + quantity, MODULE);

                    if (quantity == null) {
                        continue;
                    }

                    if (groupItems && productId.equals(groupProductId)) {
                        // same product, group
                        groupQuantity = groupQuantity.add(quantity);
                        requirementIds.add(requirementId);
                        // get the new required by date for the group as the earliest of all requirements
                        if (requiredByDate != null) {
                            if (groupDate == null || groupDate.after(requiredByDate)) {
                                groupDate = requiredByDate;
                            }
                        }
                        continue;
                    }

                    // different product, add previous grouped product to cart
                    addRequirementItemToCart(request, cart, requirementIds, groupProductId, groupQuantity, groupDate);

                    // reset group values
                    groupQuantity = quantity;
                    groupProductId = productId;
                    groupDate = requiredByDate;
                    requirementIds = new LinkedHashSet<String>();
                    requirementIds.add(requirementId);
                }

                // finally add the last one
                addRequirementItemToCart(request, cart, requirementIds, groupProductId, groupQuantity, groupDate);

                // close the ELI
                eli.close();

                updateCartWithRequirement(cart, delegator);

                // set the default checkout options
                cart.setDefaultCheckoutOptions(dispatcher);

            } catch (CartItemModifyException e) {
                return UtilMessage.createAndLogEventError(request, e, cart.getLocale(), MODULE);
            } catch (ItemNotFoundException e) {
                return UtilMessage.createAndLogEventError(request, e, cart.getLocale(), MODULE);
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogEventError(request, e, cart.getLocale(), MODULE);
            }

        } else {
            Debug.logInfo("No Requirement found for reset processing", MODULE);
        }

        return "success";

    }

    @SuppressWarnings("unchecked")
    private static void addRequirementItemToCart(HttpServletRequest request, OpentapsShoppingCart cart, LinkedHashSet<String> requirementIds, String productId, BigDecimal quantity, Timestamp requiredByDate) throws CartItemModifyException, ItemNotFoundException {
        // skip item if no quantity
        if (quantity == null || quantity.signum() == 0) {
            return;
        }

        GenericDispatcher dispatcher = (GenericDispatcher) request.getAttribute("dispatcher");

        // check if the requirements item is already in the cart
        // this might be a consolidated item or a simple item
        Iterator<ShoppingCartItem> items = cart.iterator();
        boolean requirementAlreadyInCart = false;
        while (items.hasNext() && !requirementAlreadyInCart) {
            ShoppingCartItem sci = items.next();
            Set<String> requirementIds2 = (Set<String>) sci.getAttribute("requirementIds");
            if (requirementIds2 == null) {
                requirementIds2 = new HashSet<String>();
                requirementIds2.add(sci.getRequirementId());
            }

            if (requirementIds2 != null && requirementIds2.equals(requirementIds)) {
                requirementAlreadyInCart = true;
                continue;
            }
        }
        if (requirementAlreadyInCart) {
            Debug.logWarning(UtilProperties.getMessage("OrderErrorUiLabels", "OrderTheRequirementIsAlreadyInTheCartNotAdding", UtilMisc.toMap("requirementId", requirementIds), cart.getLocale()), MODULE);
            return;
        }

        // add the requirements item in the cart
        Debug.logInfo("Bulk Adding to cart requirement [" + quantity + "] of [" + productId + "] ship before [" + requiredByDate + "]", MODULE);
        int index = cart.addOrIncreaseItem(productId, quantity, requiredByDate, dispatcher);

        // set the cart item requirement ID, and requirement IDs (in case of a consolidated item)
        ShoppingCartItem item = (ShoppingCartItem) cart.items().get(index);
        if (!requirementIds.isEmpty()) {
            item.setRequirementId((String) requirementIds.toArray()[0]);
        }
        item.setAttribute("requirementIds", requirementIds);
    }

    /**
     * Event method that creates a cart and load the purchase order requirements into it.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String createCartFromRequirements(HttpServletRequest request, HttpServletResponse response) {

        // destroy the cart first, because we are creating an order from scratch
        String result = purchasingDestroyCart(request, response);
        if ("error".equals(result)) {
            return "error";
        }

        // this should get a new cart with the correct supplierPartyId from the multi form
        OpentapsShoppingCart cart = purchasingGetOrInitializeCart(request);
        Delegator delegator = cart.getDelegator();

        // get the list of selected requirements to use for the order
        Set<String> requirements = new HashSet<String>();
        // associate the input quantities to the requirements ids
        Map<String, BigDecimal> quantities = new HashMap<String, BigDecimal>();
        // loop the posted parameters from the form
        Collection<Map<String, Object>> data = UtilHttp.parseMultiFormData(UtilHttp.getParameterMap(request));
        try {
            for (Map<String, Object> options : data) {

                String requirementId = (String) options.get("requirementId");

                // find the requirement
                GenericValue requirement = null;
                requirement = delegator.findByPrimaryKey("Requirement", UtilMisc.toMap("requirementId", requirementId));
                if (requirement == null) {
                    return UtilMessage.createAndLogEventError(request, "PurchRequirementNotExist", UtilMisc.toMap("requirementId", requirementId), cart.getLocale(), MODULE);
                }
                String quantStr = (String) options.get("quantity");

                // parse the quantity
                if (UtilValidate.isEmpty(quantStr)) {
                    continue;
                }
                Double quantDbl = 0.0;
                try {
                    quantDbl = Double.parseDouble(quantStr);
                } catch (NumberFormatException e) {
                    Debug.logWarning(e, MODULE);
                    continue;
                }
                if (quantDbl <= 0.0) {
                    continue;
                }

                // add requirement to the list
                requirements.add(requirementId);
                quantities.put(requirementId, BigDecimal.valueOf(quantDbl));
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, cart.getLocale(), MODULE);
        }

        // flag to consolidate items or not, if set it will group the items by product id/required by date
        boolean groupItems = "Y".equals(UtilCommon.getParameter(request, "consolidateFlag"));

        // for grouping
        Timestamp groupDate = null;
        String groupProductId = null;
        BigDecimal groupQuantity = BigDecimal.ZERO;
        LinkedHashSet<String> requirementIds = new LinkedHashSet<String>();

        // loop through the requirements
        try {
            List<GenericValue> requirementValues = delegator.findList("Requirement", EntityCondition.makeCondition("requirementId", EntityOperator.IN, requirements), null, UtilMisc.toList("productId", "requiredByDate"), null, false);
            for (GenericValue requirement : requirementValues) {
                String productId = requirement.getString("productId");
                String requirementId = requirement.getString("requirementId");
                BigDecimal quantity = quantities.get(requirementId);
                Timestamp requiredByDate = requirement.getTimestamp("requiredByDate");

                if (groupItems && productId.equals(groupProductId)) {
                    // same product, group
                    groupQuantity = groupQuantity.add(quantity);
                    requirementIds.add(requirementId);
                    // get the new required by date for the group as the earliest of all requirements
                    if (requiredByDate != null) {
                        if (groupDate == null || groupDate.after(requiredByDate)) {
                            groupDate = requiredByDate;
                        }
                    }
                    continue;
                }

                // different product, add previous grouped product to cart
                addRequirementItemToCart(request, cart, requirementIds, groupProductId, groupQuantity, groupDate);

                // reset group values
                groupQuantity = quantity;
                groupProductId = productId;
                groupDate = requiredByDate;
                requirementIds = new LinkedHashSet<String>();
                requirementIds.add(requirementId);
            }

            // finally add the last one
            addRequirementItemToCart(request, cart, requirementIds, groupProductId, groupQuantity, groupDate);

            updateCartWithRequirement(cart, delegator);

        } catch (CartItemModifyException e) {
            return UtilMessage.createAndLogEventError(request, e, cart.getLocale(), MODULE);
        } catch (ItemNotFoundException e) {
            return UtilMessage.createAndLogEventError(request, e, cart.getLocale(), MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, cart.getLocale(), MODULE);
        }

        return "success";
    }

    /**
     * Adds a set of requirements to the cart.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String addToCartBulkRequirements(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        OpentapsShoppingCartHelper cartHelper = new OpentapsShoppingCartHelper(delegator, dispatcher, cart);
        String controlDirective;
        Map<String, Object> result;

        // Convert the params to a map to pass in
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String catalogId = CatalogWorker.getCurrentCatalogId(request);
        result = cartHelper.addToCartBulkRequirements(catalogId, paramMap);
        controlDirective = processResult(result, request);

        // Determine where to send the browser
        if (controlDirective.equals(ERROR)) {
            return "error";
        } else {
            return "success";
        }
    }

    /**
     * Updates each of the cart items with the orderItemType according to the requirement type.
     *
     * @param cart a <code>ShoppingCart</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static boolean updateCartWithRequirement(ShoppingCart cart, Delegator delegator) throws GenericEntityException {
        // update each item to the correct orderItemType for the requirement type
        for (Iterator<ShoppingCartItem> iter = cart.iterator(); iter.hasNext();) {
            ShoppingCartItem item = iter.next();
            String requirementId = item.getRequirementId();
            if (UtilValidate.isEmpty(requirementId)) {
                continue;
            }

            GenericValue requirement = delegator.findByPrimaryKey("Requirement", UtilMisc.toMap("requirementId", requirementId));
            String orderItemTypeId = requirement.getString("orderItemTypeId");
            if (UtilValidate.isNotEmpty(orderItemTypeId)) {
                item.setItemType(orderItemTypeId);
            }
        }

        return true;
    }

    private static final String NO_ERROR = "noerror";
    private static final String NON_CRITICAL_ERROR = "noncritical";
    private static final String ERROR = "error";

    /**
     * This should be called to translate the error messages of the
     * <code>ShoppingCartHelper</code> to an appropriately formatted
     * <code>String</code> in the request object and indicate whether
     * the result was an error or not and whether the errors were
     * critical or not.
     *
     * Ported from ShoppingCartEvents.
     *
     * @param result    The result returned from the
     * <code>ShoppingCartHelper</code>
     * @param request The servlet request instance to set the error messages
     * in
     * @return one of NON_CRITICAL_ERROR, ERROR or NO_ERROR.
     */
    @SuppressWarnings("unchecked")
    public static String processResult(Map result, HttpServletRequest request) {
        //Check for errors
        StringBuffer errMsg = new StringBuffer();
        if (result.containsKey(ModelService.ERROR_MESSAGE_LIST)) {
            List<String> errorMsgs = (List<String>) result.get(ModelService.ERROR_MESSAGE_LIST);
            Iterator<String> iterator = errorMsgs.iterator();
            errMsg.append("<ul>");
            while (iterator.hasNext()) {
                errMsg.append("<li>");
                errMsg.append(iterator.next());
                errMsg.append("</li>");
            }
            errMsg.append("</ul>");
        } else if (result.containsKey(ModelService.ERROR_MESSAGE)) {
            errMsg.append(result.get(ModelService.ERROR_MESSAGE));
            request.setAttribute("_ERROR_MESSAGE_", errMsg.toString());
        }

        //See whether there was an error
        if (errMsg.length() > 0) {
            request.setAttribute("_ERROR_MESSAGE_", errMsg.toString());
            if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS)) {
                return NON_CRITICAL_ERROR;
            } else {
                return ERROR;
            }
        } else {
            return NO_ERROR;
        }
    }

    /**
     * Purchase order initialization.<br/>
     * Creates <code>OpentapsShoppingCart</code> beforehand to <code>ShoppingCartEvents.setOrderCurrencyAgreementShipDates(HttpServletRequest, HttpServletResponse)</code> call.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String initializePurchaseOrder(HttpServletRequest request, HttpServletResponse response) {

        OpentapsShoppingCart cart = purchasingGetOrInitializeCart(request);
        request.getSession().setAttribute("shoppingCart", cart);

        return ShoppingCartEvents.setOrderCurrencyAgreementShipDates(request, response);
    }
}
