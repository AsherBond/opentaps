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

package org.opentaps.gwt.common.server.lookup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.product.catalog.CatalogWorker;
import org.opentaps.common.order.OrderEvents;
import org.opentaps.common.order.shoppingcart.OpentapsShoppingCart;
import org.opentaps.base.entities.SupplierProduct;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.domain.purchasing.PurchasingRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.Permissions;
import org.opentaps.gwt.common.client.lookup.configuration.OrderItemsCartLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate the OrderItemsListEditor.
 */
public class OrderItemsLookupService extends EntityLookupService {

    private static final String MODULE = OrderItemsLookupService.class.getName();

    private OpentapsShoppingCart cart;
    
    private String productCatalogId;

    protected OrderItemsLookupService(InputProviderInterface provider, OpentapsShoppingCart cart) throws RepositoryException {
        super(provider, OrderItemsCartLookupConfiguration.LIST_OUT_FIELDS);
        this.cart = cart;
    }

    protected OrderItemsLookupService(InputProviderInterface provider, OpentapsShoppingCart cart, String productCatalogId) throws RepositoryException {
        this(provider, cart);
        this.productCatalogId = productCatalogId;
    }

    /**
     * AJAX event to perform lookups on Cart Order Items.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws FoundationException if an error occurs
     */
    public static String findOrderItemsCart(HttpServletRequest request, HttpServletResponse response) throws FoundationException {
        InputProviderInterface provider = new HttpInputProvider(request);
        OpentapsShoppingCart cart = OrderEvents.getCart(request);
        JsonResponse json = new JsonResponse(response);
        OrderItemsLookupService service = new OrderItemsLookupService(provider, cart);
        service.findOrderItems();
        return json.makeLookupResponse(OrderItemsCartLookupConfiguration.INOUT_ITEM_SEQUENCE, service, request.getSession(true).getServletContext());
    }

    /**
     * AJAX event to perform lookups on Cart Order Item Information.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws FoundationException if an error occurs
     */
    public static String getProductInfoForCart(HttpServletRequest request, HttpServletResponse response) throws FoundationException {
        InputProviderInterface provider = new HttpInputProvider(request);
        OpentapsShoppingCart cart = OrderEvents.getCart(request);
        JsonResponse json = new JsonResponse(response);
        String productCatalogId = CatalogWorker.getCurrentCatalogId(request);
        OrderItemsLookupService service = new OrderItemsLookupService(provider, cart, productCatalogId);
        service.findProductInfoForCart();
        return json.makeLookupResponse(OrderItemsCartLookupConfiguration.INOUT_PRODUCT, service, request.getSession(true).getServletContext());
    }

    /** A fake entity to hold Cart Order Item information. */
    public static class CartOrderItem extends Entity {
        private String productId;
        private String description;
        private String unitPrice;
        private String adjustment;
        private String quantity;
        private String itemId;
        private String isPromo;
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAdjustment() { return adjustment; }
        public void setAdjustment(String adjustment) { this.adjustment = adjustment; }
        public String getUnitPrice() { return unitPrice; }
        public void setUnitPrice(String unitPrice) { this.unitPrice = unitPrice; }
        public String getQuantity() { return quantity; }
        public void setQuantity(String quantity) { this.quantity = quantity; }
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getIsPromo() { return isPromo; }
        public void setIsPromo(String isPromo) { this.isPromo = isPromo; }
        @Override public Map<String, Object> toMap() {
            Map<String, Object> res = new HashMap<String, Object>();
            res.put(OrderItemsCartLookupConfiguration.INOUT_PRODUCT, productId);
            res.put(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION, description);
            res.put(OrderItemsCartLookupConfiguration.INOUT_ADJUSTMENT, adjustment);
            res.put(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE, unitPrice);
            res.put(OrderItemsCartLookupConfiguration.INOUT_QUANTITY, quantity);
            res.put(OrderItemsCartLookupConfiguration.INOUT_ITEM_SEQUENCE, itemId);
            res.put(OrderItemsCartLookupConfiguration.INOUT_IS_PROMO, isPromo);
            return res;
        }
    }

    /**
     * Finds the order items in the current cart.
     * @return the list of <code>CartOrderItem</code>, or <code>null</code> if an error occurred
     */
    public List<CartOrderItem> findProductInfoForCart() {
        getGlobalPermissions().setAll(true);
        List<CartOrderItem> items = new ArrayList<CartOrderItem>();
        if (cart == null) {
            Debug.logError("No cart was set in the request.", MODULE);
            return null;
        }

        String productId = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_PRODUCT);
        if (productId == null) {
            Debug.logError("Missing required parameter productId", MODULE);
            return null;
        }

        try {
            ProductRepositoryInterface repository = getDomainsDirectory().getProductDomain().getProductRepository();
            Product product = repository.getProductById(productId);

            CartOrderItem item = new CartOrderItem();
            if ("PURCHASE_ORDER".equals(cart.getOrderType())) {
                String quantityStr = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_QUANTITY);
                if (quantityStr == null) {
                    Debug.logError("Missing required parameter quantity for purchase order item", MODULE);
                    return null;
                }

                BigDecimal quantity = new BigDecimal(quantityStr);
                PurchasingRepositoryInterface purchasingRepository = getDomainsDirectory().getPurchasingDomain().getPurchasingRepository();
                SupplierProduct supplierProduct = purchasingRepository.getSupplierProduct(cart.getPartyId(), productId, quantity, cart.getCurrency());

                String description = "";
                String supplierProductName = "";

                if (supplierProduct != null) {
                    BigDecimal lastPrice = supplierProduct.getLastPrice();
                    if (lastPrice != null) {
                        item.setUnitPrice(lastPrice.toString());
                    } else {
                        item.setUnitPrice("0.0");
                        Debug.logWarning("No lastPrice found for SupplierProduct with party [" + cart.getPartyId() + "], product [" + productId + "], quantity [" + quantity + "] and currency [" + cart.getCurrency() + "]", MODULE);
                    }
                    description = supplierProduct.getSupplierProductId() + " ";
                    supplierProductName = supplierProduct.getSupplierProductName();
                } else {
                    // if not find any matching SupplierProduct, then set 0.0 as default unit price
                    // it for fix the endless loop when adding a product without SupplierProduct to a purchase order
                    item.setUnitPrice("0.0");
                    Debug.logWarning("No SupplierProduct found with party [" + cart.getPartyId() + "], product [" + productId + "], quantity [" + quantity + "] and currency [" + cart.getCurrency() + "]", MODULE);
                }

                if (UtilValidate.isNotEmpty(supplierProductName)) {
                    description += supplierProductName;
                } else if (UtilValidate.isNotEmpty(product.getProductName())) {
                    description += product.getProductName();
                }
                item.setDescription(description);
            } else {
                String quantityStr = getProvider().getParameter(OrderItemsCartLookupConfiguration.INOUT_QUANTITY);
                BigDecimal quantity = null;
                if (quantityStr != null) {
                    quantity = new BigDecimal(quantityStr);
                }
                item.setUnitPrice(repository.getUnitPrice(product, quantity, cart.getCurrency(), cart.getOrderPartyId(), productCatalogId).toString());
                item.setDescription(product.getProductName());
            }

            item.setProductId(product.getProductId());
            items.add(item);

            setResults(items);
            setResultTotalCount(items.size());
            return items;
        } catch (FoundationException e) {
            storeException(e);
            return null;
        }
    }

    /**
     * Finds the order items in the current cart.
     * @return the list of <code>CartOrderItem</code>, or <code>null</code> if an error occurred
     */
    @SuppressWarnings("unchecked")
    public List<CartOrderItem> findOrderItems() {
        getGlobalPermissions().setAll(true);
        List<CartOrderItem> items = new ArrayList<CartOrderItem>();
        if (cart == null) {
            Debug.logError("No cart was set in the request.", MODULE);
            return null;
        }

        // get cart items
        Iterator<ShoppingCartItem> iter = cart.iterator();
        while (iter.hasNext()) {
            ShoppingCartItem i = iter.next();
            CartOrderItem item = new CartOrderItem();
            item.setQuantity(i.getQuantity().toString());
            item.setUnitPrice(i.getBasePrice().toString());
            item.setDescription(i.getName());
            item.setProductId(i.getProductId());
            item.setItemId(new Integer(cart.getItemIndex(i)).toString());
            item.setAdjustment(i.getOtherAdjustments().toString());
            if (i.getIsPromo()) {
                item.setIsPromo("Y");
                setEntityPermissions(item, new Permissions(false));
            } else {
                item.setIsPromo("N");
            }

            items.add(item);
        }

        // get cart adjustments (that are not related to a specific item)
        Iterator<GenericValue> iterAdj = cart.getAdjustments().iterator();
        while (iterAdj.hasNext()) {
            GenericValue adj = iterAdj.next();
            CartOrderItem item = new CartOrderItem();
            item.setItemId("Adj");
            item.setAdjustment(org.ofbiz.order.order.OrderReadHelper.calcOrderAdjustment(adj, cart.getSubTotal()).toString());
            setEntityPermissions(item, new Permissions(false));

            items.add(item);
        }

        Debug.logInfo("Found cart items: " + items, MODULE);
        setResults(items);
        setResultTotalCount(items.size());
        return items;
    }
}

