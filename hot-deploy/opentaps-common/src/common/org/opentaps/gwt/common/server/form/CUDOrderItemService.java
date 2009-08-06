/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.gwt.common.server.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.order.OrderEvents;
import org.opentaps.common.order.UtilOrder;
import org.opentaps.common.order.shoppingcart.OpentapsShoppingCart;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.configuration.OrderItemsCartLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to create, update or delete an Order Item.
 */
public class CUDOrderItemService extends GenericCUDService {

    private static final String MODULE = CUDOrderItemService.class.getName();

    private String itemId;
    private OpentapsShoppingCart cart;
    private ProductRepositoryInterface repository;
    private HttpServletRequest request;

    private List<ShoppingCartItem> itemsToRemove;

    protected CUDOrderItemService(InputProviderInterface provider, OpentapsShoppingCart cart, HttpServletRequest request) throws RepositoryException {
        super(provider);
        this.cart = cart;
        this.repository = provider.getDomainsDirectory().getProductDomain().getProductRepository();
        this.request = request;
    }

    @Override
    public void validate() {
        // the CUD action is always required, checks it is present and valid
        validateCUDAction();
    }

    @Override
    protected Map<String, Object> callCreateService() throws GenericServiceException {
        itemId = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_ITEM_SEQUENCE);
        Debug.logInfo("Trying to add a new item in the cart", MODULE);
        if (itemId != null) {
            throw new GenericServiceException("Cannot give itemId for creation of a cart Order Item.");
        }

        try {
            // check the product ID
            String productId = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_PRODUCT);
            if (productId == null) {
                addMissingFieldError(OrderItemsCartLookupConfiguration.INOUT_PRODUCT);
            }
            Product product = repository.getProductById(productId);

            // check the quantity
            String quantityString = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_QUANTITY);
            Double quantity = 1.0;
            if (quantityString != null) {
                try {
                    quantity = Double.parseDouble(quantityString);
                } catch (NumberFormatException e) {
                    addFieldError(OrderItemsCartLookupConfiguration.INOUT_QUANTITY, UtilUi.MSG.opentapsFieldError_BadDoubleFormat());
                }
            }

            Map<String, Object> attributes = new HashMap<String, Object>();

            String itemType = UtilOrder.getOrderItemTypeId(product.getProductTypeId(), cart.getOrderType(), cart.getDelegator());
            // add item to the cart
            int index = OrderEvents.addItemToOrder(cart, productId, null, quantity, null, null, null, null, null, null, attributes, null, null, itemType, null, null, request);

            // set the description if given, it overrides the product name
            String description = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION);
            if (description != null) {
                ShoppingCartItem item = cart.findCartItem(index);
                item.setName(description);
            }

            // override the unit price if given
            String priceStr = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE);
            if (priceStr != null) {
                try {
                    Double price = Double.parseDouble(priceStr);
                    ShoppingCartItem item = cart.findCartItem(index);
                    item.setBasePrice(price);
                    item.setDisplayPrice(price);
                } catch (NumberFormatException e) {
                    addFieldError(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE, UtilUi.MSG.opentapsFieldError_BadDoubleFormat());
                }
            }

        } catch (GeneralException e) {
            throw new GenericServiceException(e);
        }
        return ServiceUtil.returnSuccess();
    }

    @Override
    protected Map<String, Object> callUpdateService() throws GenericServiceException {
        itemId = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_ITEM_SEQUENCE);
        Debug.logInfo("Trying to update the cart item [" + itemId + "]", MODULE);
        if (itemId == null) {
            throw new GenericServiceException("itemId is required for Update of a cart Order Item.");
        }

        try {
            // get the cart item
            Integer index = Integer.parseInt(itemId);
            ShoppingCartItem item = cart.findCartItem(index);

            // check and set the quantity
            String quantityString = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_QUANTITY);
            if (quantityString != null) {
                try {
                    Double quantity = Double.parseDouble(quantityString);
                    if (quantity != item.getQuantity()) {
                        item.setQuantity(quantity, getProvider().getInfrastructure().getDispatcher(), cart);
                    }
                } catch (NumberFormatException e) {
                    addFieldError(OrderItemsCartLookupConfiguration.INOUT_QUANTITY, UtilUi.MSG.opentapsFieldError_BadDoubleFormat());
                }
            }

            // get the unit price
            String priceStr = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE);
            if (priceStr != null) {
                try {
                    Double price = Double.parseDouble(priceStr);
                    if (price != item.getBasePrice()) {
                        item.setBasePrice(price);
                    }
                    if (price != item.getDisplayPrice()) {
                        item.setDisplayPrice(price);
                    }
                } catch (NumberFormatException e) {
                    addFieldError(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE, UtilUi.MSG.opentapsFieldError_BadDoubleFormat());
                }
            }

            // set the description
            String description = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION);
            item.setName(description);

        } catch (GeneralException e) {
            throw new GenericServiceException(e);
        }
        return ServiceUtil.returnSuccess();
    }

    @Override
    protected Map<String, Object> callDeleteService() throws GenericServiceException {
        itemId = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_ITEM_SEQUENCE);
        Debug.logInfo("Trying to delete the cart item [" + itemId + "]", MODULE);
        if (itemId == null) {
            throw new GenericServiceException("itemId is required for Delete of a cart Order Item.");
        }

        try {
            // get the cart item
            Integer index = Integer.parseInt(itemId);
            // check if the item exists
            try {
                ShoppingCartItem item = cart.findCartItem(index);
                if (isBatchAction()) {
                    itemsToRemove.add(item);
                }
            } catch (Exception e) {
                UtilMessage.logServiceWarning("OrderProblemsGettingTheCartItemByIndex", cart.getLocale(), MODULE);
            }
            if (!isBatchAction()) {
                cart.removeCartItem(index, getProvider().getInfrastructure().getDispatcher());
            }
        } catch (GeneralException e) {
            throw new GenericServiceException(e);
        }
        return ServiceUtil.returnSuccess();
    }

    @Override
    protected void prepareBatch() throws GenericServiceException {
        if (CUDAction.DELETE.equals(getRequestedCUDAction())) {
            itemsToRemove = new ArrayList<ShoppingCartItem>();
        }
    }

    // when in a batch delete, we need to save the list of items first them remove them
    // since the IDs change after each delete
    @Override
    protected void finalizeBatch() throws GenericServiceException {
        if (CUDAction.DELETE.equals(getRequestedCUDAction())) {
            for (ShoppingCartItem item : itemsToRemove) {
                int index = cart.getItemIndex(item);
                try {
                    cart.removeCartItem(index, getProvider().getInfrastructure().getDispatcher());
                } catch (GeneralException e) {
                    throw new GenericServiceException(e);
                }
            }
        }
    }

    /**
     * AJAX event for Cart Order Item CUD.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     */
    public static String postOrderItemsCart(HttpServletRequest request, HttpServletResponse response) {
        JsonResponse json = new JsonResponse(response);
        try {
            InputProviderInterface provider = new HttpInputProvider(request);
            OpentapsShoppingCart cart = OrderEvents.getCart(request);
            CUDOrderItemService service = new CUDOrderItemService(provider, cart, request);
            return json.makeResponse(service.call());
        } catch (Throwable e) {
            return json.makeResponse(e);
        }
    }

    /**
     * AJAX event for batch Cart Order Item CUD.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     */
    public static String postOrderItemsCartBatch(HttpServletRequest request, HttpServletResponse response) {
        JsonResponse json = new JsonResponse(response);
        try {
            InputProviderInterface provider = new HttpInputProvider(request);
            OpentapsShoppingCart cart = OrderEvents.getCart(request);
            CUDOrderItemService service = new CUDOrderItemService(provider, cart, request);
            return json.makeResponse(service.callServiceBatch());
        } catch (Throwable e) {
            return json.makeResponse(e);
        }
    }
}
