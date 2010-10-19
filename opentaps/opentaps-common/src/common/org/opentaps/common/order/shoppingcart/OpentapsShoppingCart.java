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
/* Copyright (c) Open Source Strategies, Inc. */
/*
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.common.order.shoppingcart;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.product.UtilProduct;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;

/**
 * Opentaps implementation of the Shopping Cart.
 */
public class OpentapsShoppingCart extends ShoppingCart {

    private Map<CartShipInfo, String> shipGroupThirdPartyAccountNumbers = new HashMap<CartShipInfo, String>();
    private Map<CartShipInfo, String> shipGroupThirdPartyPostalCodes = new HashMap<CartShipInfo, String>();
    private Map<CartShipInfo, String> shipGroupThirdPartyCountryCodes = new HashMap<CartShipInfo, String>();
    private Map<CartShipInfo, Boolean> shipGroupCOD = new HashMap<CartShipInfo, Boolean>();
    private Map<CartShipInfo, OpentapsShippingEstimateWrapper> shipEstimateWrappers = new HashMap<CartShipInfo, OpentapsShippingEstimateWrapper>();
    // allow to store the cancel quantities of each cart item, as the ShoppingCart does not store this information
    private Map<Integer, BigDecimal> cancelQuantities = new HashMap<Integer, BigDecimal>();

    /**
     * Creates a new <code>OpentapsShoppingCart</code> instance.
     * @param delegator a <code>Delegator</code> value
     * @param productStoreId the product store of this order
     * @param locale a <code>Locale</code> value
     * @param currencyUom the currency of this order
     */
    public OpentapsShoppingCart(Delegator delegator, String productStoreId, Locale locale, String currencyUom) {
        super(delegator, productStoreId, locale, currencyUom);
    }

    /**
     * Creates a new <code>OpentapsShoppingCart</code> instance.
     * @param delegator a <code>Delegator</code> value
     * @param productStoreId the product store of this order
     * @param webSiteId the web site of this order
     * @param locale a <code>Locale</code> value
     * @param currencyUom the currency of this order
     */
    public OpentapsShoppingCart(Delegator delegator, String productStoreId, String webSiteId, Locale locale, String currencyUom) {
        super(delegator, productStoreId, webSiteId, locale, currencyUom);
    }

    /**
     * Creates a new <code>OpentapsShoppingCart</code> instance.
     *
     * @param delegator a <code>Delegator</code> value
     * @param productStoreId the product store of this order
     * @param webSiteId the web site of this order
     * @param locale a <code>Locale</code> value
     * @param currencyUom the currency of this order
     * @param billToCustomerPartyId a <code>String</code> value
     * @param billFromVendorPartyId a <code>String</code> value
     */
    public OpentapsShoppingCart(Delegator delegator, String productStoreId, String webSiteId, Locale locale, String currencyUom, String billToCustomerPartyId, String billFromVendorPartyId) {
        super(delegator, productStoreId, webSiteId, locale, currencyUom, billToCustomerPartyId, billFromVendorPartyId);
        setAttribute("addpty", "N");
    }

    /**
     * Creates a new <code>OpentapsShoppingCart</code> instance from a <code>ShoppingCart</code> value.
     * @param oldCart the <code>ShoppingCart</code> value to create from
     */
    @SuppressWarnings("unchecked")
    public OpentapsShoppingCart(ShoppingCart oldCart) {
        super(oldCart);
        orderPartyId = oldCart.getOrderPartyId();
        if (UtilValidate.isNotEmpty(oldCart.getOrderName())) {
            setOrderName("Copy of " + oldCart.getOrderName());
        } else {
            setOrderName(null);
        }
        setOrderType(oldCart.getOrderType());
        setChannelType(oldCart.getChannelType());
        setInternalCode(oldCart.getInternalCode());
        setUserLogin(oldCart.getUserLogin());
        setAttribute("addpty", "N");
        setPlacingCustomerPartyId(oldCart.getPlacingCustomerPartyId());
        setBillFromVendorPartyId(oldCart.getBillFromVendorPartyId());
        setBillToCustomerPartyId(oldCart.getBillToCustomerPartyId());
        setShipToCustomerPartyId(oldCart.getShipToCustomerPartyId());
        setEndUserCustomerPartyId(oldCart.getEndUserCustomerPartyId());

        List oldShipGroups = oldCart.getShipGroups();
        Iterator osgit = oldShipGroups.iterator();
        while (osgit.hasNext()) {
            CartShipInfo oldShipGroup = (CartShipInfo) osgit.next();
            int newShipGroupSeqId = addShipInfo();
            CartShipInfo newShipGroup = (CartShipInfo) getShipGroups().get(newShipGroupSeqId);
            newShipGroup.shipTaxAdj = oldShipGroup.shipTaxAdj;
            newShipGroup.setContactMechId(oldShipGroup.getContactMechId());
            newShipGroup.shipmentMethodTypeId = oldShipGroup.shipmentMethodTypeId;
            newShipGroup.supplierPartyId = oldShipGroup.supplierPartyId;
            newShipGroup.carrierPartyId = oldShipGroup.carrierPartyId;
            newShipGroup.carrierRoleTypeId = oldShipGroup.carrierRoleTypeId;
            newShipGroup.giftMessage = oldShipGroup.giftMessage;
            newShipGroup.shippingInstructions = oldShipGroup.shippingInstructions;
            newShipGroup.maySplit = oldShipGroup.maySplit;
            newShipGroup.isGift = oldShipGroup.isGift;
            newShipGroup.shipEstimate = oldShipGroup.shipEstimate;
            newShipGroup.shipBeforeDate = oldShipGroup.shipBeforeDate;
            newShipGroup.shipAfterDate = oldShipGroup.shipAfterDate;
            if (UtilValidate.isNotEmpty(oldShipGroup.carrierRoleTypeId)) {
                newShipGroup.carrierRoleTypeId = oldShipGroup.carrierRoleTypeId;
            } else {
                newShipGroup.carrierRoleTypeId = "CARRIER";
            }

            if (oldShipGroup.shipItemInfo != null) {
                Iterator osiiit = oldShipGroup.shipItemInfo.keySet().iterator();
                while (osiiit.hasNext()) {
                    ShoppingCartItem oldCartItem = (ShoppingCartItem) osiiit.next();
                    CartShipInfo.CartShipItemInfo oldItemInfo = (CartShipInfo.CartShipItemInfo) oldShipGroup.shipItemInfo.get(oldCartItem);
                    int oldCartItemIndex = oldCart.getItemIndex(oldCartItem);
                    ShoppingCartItem newCartItem = (ShoppingCartItem) items().get(oldCartItemIndex);
                    CartShipInfo.CartShipItemInfo newItemInfo = new CartShipInfo.CartShipItemInfo();
                    newItemInfo.item = newCartItem;
                    newItemInfo.quantity = oldItemInfo.quantity;
                    newShipGroup.shipItemInfo.put(newCartItem, newItemInfo);
                }
            }
        }

        for (int paymentInfoIndex = 0; paymentInfoIndex < oldCart.selectedPayments(); paymentInfoIndex++) {
            CartPaymentInfo oldPayInfo = oldCart.getPaymentInfo(paymentInfoIndex);
            String id = oldPayInfo.paymentMethodTypeId != null ? oldPayInfo.paymentMethodTypeId : oldPayInfo.paymentMethodId;
            CartPaymentInfo newPayInfo = addPayment(id);
            newPayInfo.amount = oldPayInfo.amount;
            newPayInfo.finAccountId = oldPayInfo.finAccountId;
            newPayInfo.isPresent = oldPayInfo.isPresent;
            newPayInfo.overflow = oldPayInfo.overflow;
            newPayInfo.paymentMethodId = oldPayInfo.paymentMethodId;
            newPayInfo.paymentMethodTypeId = oldPayInfo.paymentMethodTypeId;
            newPayInfo.postalCode = oldPayInfo.postalCode;
            newPayInfo.refNum = oldPayInfo.refNum;
            newPayInfo.securityCode = oldPayInfo.securityCode;
            newPayInfo.singleUse = oldPayInfo.singleUse;
        }

    }

    /**
     * Sets the third party account number for the specified ship group.
     * @param idx the index of the ship group to modify
     * @param thirdPartyAccountNumber the new value for the third party account number
     */
    public void setThirdPartyAccountNumber(int idx, String thirdPartyAccountNumber) {
        CartShipInfo csi = getShipInfo(idx);
        shipGroupThirdPartyAccountNumbers.put(csi, thirdPartyAccountNumber);
    }

    /**
     * Gets the third party account number for the specified ship group.
     * @param idx the index of the ship group to get the value for
     * @return the third party account number for the specified ship group
     */
    public String getThirdPartyAccountNumber(int idx) {
        CartShipInfo csi = getShipInfo(idx);
        return shipGroupThirdPartyAccountNumbers.get(csi);
    }

    /**
     * Sets the third party postal code for the specified ship group.
     * @param idx the index of the ship group to modify
     * @param thirdPartyPostalCode the new value for the third party postal code
     */
    public void setThirdPartyPostalCode(int idx, String thirdPartyPostalCode) {
        CartShipInfo csi = getShipInfo(idx);
        shipGroupThirdPartyPostalCodes.put(csi, thirdPartyPostalCode);
    }

    /**
     * Gets the third party postal code for the specified ship group.
     * @param idx the index of the ship group to get the value for
     * @return the third party postal code for the specified ship group
     */
    public String getThirdPartyPostalCode(int idx) {
        CartShipInfo csi = getShipInfo(idx);
        return shipGroupThirdPartyPostalCodes.get(csi);
    }

    /**
     * Sets the third party country code for the specified ship group.
     * @param idx the index of the ship group to modify
     * @param thirdPartyCountryCode the new value for the third party country code
     */
    public void setThirdPartyCountryCode(int idx, String thirdPartyCountryCode) {
        CartShipInfo csi = getShipInfo(idx);
        shipGroupThirdPartyCountryCodes.put(csi, thirdPartyCountryCode);
    }

    /**
     * Gets the third party country code for the specified ship group.
     * @param idx the index of the ship group to get the value for
     * @return the third party country code for the specified ship group
     */
    public String getThirdPartyCountryCode(int idx) {
        CartShipInfo csi = getShipInfo(idx);
        return shipGroupThirdPartyCountryCodes.get(csi);
    }

    /**
     * Sets the cash on delivery flag for the specified ship group.
     * @param idx the index of the ship group to modify
     * @param isCOD the new value for the cash on delivery flag
     */
    public void setCOD(int idx, boolean isCOD) {
        CartShipInfo csi = getShipInfo(idx);
        shipGroupCOD.put(csi, new Boolean(isCOD));
    }

    /**
     * Gets the cash on delivery flag for the specified ship group.
     * @param idx the index of the ship group to get the value for
     * @return the cash on delivery flag for the specified ship group
     */
    public boolean getCOD(int idx) {
        CartShipInfo csi = getShipInfo(idx);
        return shipGroupCOD.containsKey(csi) && shipGroupCOD.get(csi);
    }

    /**
     * Sets the shipping estimate wrapper instance for the specified ship group.
     * @param idx the index of the ship group to modify
     * @param wrapper the new shipping estimate wrapper instance
     */
    public void setShipEstimateWrapper(int idx, OpentapsShippingEstimateWrapper wrapper) {
        CartShipInfo csi = getShipInfo(idx);
        shipEstimateWrappers.put(csi, wrapper);
    }

    /**
     * Gets the shipping estimate wrapper instance for the specified ship group.
     * @param idx the index of the ship group to get the value for
     * @return the shipping estimate wrapper instance for the specified ship group
     */
    public OpentapsShippingEstimateWrapper getShipEstimateWrapper(int idx) {
        CartShipInfo csi = getShipInfo(idx);
        return shipEstimateWrappers.get(csi);
    }

    /**
     * Gets the list of product warnings.
     * @param delegator a <code>Delegator</code> value
     * @param productId the product to the get the warnings for
     * @return a <code>List</code> of <code>String</code>
     * @exception GenericEntityException if an error occurs
     */
    public List<String> getProductWarnings(Delegator delegator, String productId) throws GenericEntityException {
        List<String> warnings = new LinkedList<String>();
        try {
            warnings = UtilProduct.getProductWarnings(delegator, productId);
         } catch (GenericEntityException e) {
            throw new GenericEntityException("Cannot find if there are warnings for productId" + productId, e);
        }
        return warnings;
    }

    /**
     * Clears the Commission Agent for this order.
     */
    @SuppressWarnings("unchecked")
    public void clearCommissionAgents() {
        // clear previous commission agent
        getAdditionalPartyRoleMap().put("COMMISSION_AGENT", new LinkedList());
    }

    /**
     * Sets the party which should have the Commission Agent role for this order.
     * @param partyId the party which should have the Commission Agent role for this order
     */
    public void setCommissionAgent(String partyId) {
        // clear previous commission agent
        clearCommissionAgents();
        // set new commision agent
        addAdditionalPartyRole(partyId, "COMMISSION_AGENT");
    }

    @SuppressWarnings("unchecked")
    @Override
    public List makeOrderItems(boolean explodeItems, boolean replaceAggregatedId, LocalDispatcher dispatcher) {
        // do the explosion
        if (explodeItems && dispatcher != null) {
            explodeItems(dispatcher);
        }

        // now build the lines
        synchronized (cartLines) {
            List result = FastList.newInstance();

            Iterator itemIter = cartLines.iterator();
            while (itemIter.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) itemIter.next();

                if (UtilValidate.isEmpty(item.getOrderItemSeqId())) {
                    String orderItemSeqId = UtilFormatOut.formatPaddedNumber(nextItemSeq, 5);
                    item.setOrderItemSeqId(orderItemSeqId);
                } else {
                    try {
                        int thisSeqId = Integer.parseInt(item.getOrderItemSeqId());
                        if (thisSeqId > nextItemSeq) {
                            nextItemSeq = thisSeqId;
                        }
                    } catch (NumberFormatException e) {
                        Debug.logError(e, module);
                    }
                }
                nextItemSeq++;

                // the initial status for all item types
                String initialStatus = "ITEM_CREATED";
                String status = item.getStatusId();
                if (status == null) {
                    status = initialStatus;
                }
                //check for aggregated products
                String aggregatedInstanceId = null;
                if (replaceAggregatedId && UtilValidate.isNotEmpty(item.getConfigWrapper())) {
                    aggregatedInstanceId = getAggregatedInstanceId(item, dispatcher);
                }

                GenericValue orderItem = getDelegator().makeValue("OrderItem");
                orderItem.set("orderItemSeqId", item.getOrderItemSeqId());
                orderItem.set("externalId", item.getExternalId());
                orderItem.set("orderItemTypeId", item.getItemType());
                if (item.getItemGroup() != null) {
                    orderItem.set("orderItemGroupSeqId", item.getItemGroup().getGroupNumber());
                }
                orderItem.set("productId", UtilValidate.isNotEmpty(aggregatedInstanceId) ? aggregatedInstanceId : item.getProductId());
                orderItem.set("prodCatalogId", item.getProdCatalogId());
                orderItem.set("productCategoryId", item.getProductCategoryId());
                // the cart has been loaded with the ordered quantity in order to have promotions
                // being correctly applied, but in that case when the cart is remade into an Order
                // we want to preserve the cancel quantities
                BigDecimal cancelQuantity = cancelQuantities.get(getItemIndex(item));
                if (cancelQuantity != null) {
                    orderItem.set("quantity", item.getQuantity().add(cancelQuantity));
                    orderItem.set("cancelQuantity", cancelQuantity);
                } else {
                    orderItem.set("quantity", item.getQuantity());
                    orderItem.set("cancelQuantity", null);
                }

                orderItem.set("selectedAmount", item.getSelectedAmount());
                orderItem.set("unitPrice", item.getBasePrice());
                orderItem.set("unitListPrice", item.getListPrice());
                orderItem.set("isModifiedPrice",item.getIsModifiedPrice() ? "Y" : "N");
                orderItem.set("isPromo", item.getIsPromo() ? "Y" : "N");

                orderItem.set("shoppingListId", item.getShoppingListId());
                orderItem.set("shoppingListItemSeqId", item.getShoppingListItemSeqId());

                orderItem.set("itemDescription", item.getName());
                orderItem.set("comments", item.getItemComment());
                orderItem.set("estimatedDeliveryDate", item.getDesiredDeliveryDate());
                orderItem.set("correspondingPoId", this.getPoNumber());
                orderItem.set("quoteId", item.getQuoteId());
                orderItem.set("quoteItemSeqId", item.getQuoteItemSeqId());
                orderItem.set("statusId", status);

                orderItem.set("shipBeforeDate", item.getShipBeforeDate());
                orderItem.set("shipAfterDate", item.getShipAfterDate());

                // set the accounting tags
                for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                    orderItem.put(UtilAccountingTags.ENTITY_TAG_PREFIX + i, item.getAttribute(UtilAccountingTags.TAG_PARAM_PREFIX + i));
                }

                // copy the other custom fields from the attributes in the orderItem
                ModelEntity model = orderItem.getModelEntity();
                for (Object o : item.getAttributes().keySet()) {
                    if (o == null || !(o instanceof String)) {
                        continue;
                    }
                    String n = (String) o;
                    if (UtilCommon.isCustomEntityField(n) && model.isField(n)) {
                        orderItem.set(n, model.convertFieldValue(n, item.getAttribute(n), getDelegator()));
                    }
                }


                result.add(orderItem);
                // don't do anything with adjustments here, those will be added below in makeAllAdjustments
            }
            return result;
        }
    }

    /**
     * Simpler add or increase item to cart.
     *
     * @param productId product to add
     * @param quantity quantity of the product to add
     * @param requiredByDate item required by date
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @return the index of the created or increased item
     * @exception CartItemModifyException if an error occurs
     * @exception ItemNotFoundException if an error occurs
     */
    public int addOrIncreaseItem(String productId, BigDecimal quantity, Timestamp requiredByDate, LocalDispatcher dispatcher) throws CartItemModifyException, ItemNotFoundException {
        return super.addOrIncreaseItem(productId, null, quantity, null, null, null, requiredByDate, null, null, null, null, null, null, null, null, dispatcher);
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultCheckoutOptions(LocalDispatcher dispatcher) {
        super.setDefaultCheckoutOptions(dispatcher);

        // reset unwanted flag that Ofbiz ShoppingCart considers true for purchase orders by default.
        // otherwise, creating POs from Requirements will consider order terms set, even if they are not, and not allow you to go to [Options] page any more.
        setOrderTermSet(false);
    }

    /**
     * Sets the cancel quantity for a cart item.
     * By default the cancel quantity is considered to be 0, but when transforming an Order into a Cart and vice-versa we sometimes want to keep that information.
     *
     * @param index a <code>ShoppingCartItem</code> index
     * @param cancelQuantity the cancel quantity
     */
    public void setCancelQuantity(Integer index, BigDecimal cancelQuantity) {
        this.cancelQuantities.put(index, cancelQuantity);
    }

    /**
     * Gets the <code>organizationPartyId</code> for this cart.
     * @return the customer party Id for a purchase order cart, the vendor party Id for a sales order cart
     */
    public String getOrganizationPartyId() {
        if (isPurchaseOrder()) {
            return getBillToCustomerPartyId();
        } else {
            return getBillFromVendorPartyId();
        }
    }
}
