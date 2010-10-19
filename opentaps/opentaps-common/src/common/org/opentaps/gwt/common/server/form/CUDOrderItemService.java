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

package org.opentaps.gwt.common.server.form;

import java.math.BigDecimal;
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
            Product product = null;
            if (productId == null) {
                addMissingFieldError(OrderItemsCartLookupConfiguration.INOUT_PRODUCT);
            } else {
                product = repository.getProductById(productId);
                if (product == null) {
                    addFieldError(OrderItemsCartLookupConfiguration.INOUT_PRODUCT, UtilUi.MSG.crmErrorProductNotFound(productId));
                }
            }

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

            // check the unit price
            BigDecimal unitPrice = null;
            String priceStr = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE);
            if (priceStr != null) {
                try {
                    unitPrice = new BigDecimal(priceStr);
                } catch (NumberFormatException e) {
                    addFieldError(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE, UtilUi.MSG.opentapsFieldError_BadDoubleFormat());
                }
            }

            // check if any errors has been noted, and throw the appropriate exception which send them back to the UI
            checkValidationErrors();

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
            if (unitPrice != null) {
                ShoppingCartItem item = cart.findCartItem(index);
                item.setBasePrice(unitPrice);
                item.setDisplayPrice(unitPrice);
            }

        } catch (CustomServiceValidationException e) {
            throw e;
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
                    BigDecimal quantity = new BigDecimal(quantityString);
                    if (quantity.compareTo(item.getQuantity()) != 0) {
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
                    BigDecimal price = new BigDecimal(priceStr);
                    if (price.compareTo(item.getBasePrice()) != 0) {
                        item.setBasePrice(price);
                    }
                    if (price.compareTo(item.getDisplayPrice()) != 0) {
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
