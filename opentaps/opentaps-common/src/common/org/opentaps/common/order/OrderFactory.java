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

package org.opentaps.common.order;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * A simple helper for the storeOrder service.
 */
public abstract class OrderFactory {

    private class TaxProductInfo {
        public List<GenericValue> itemProductList;
        public List<BigDecimal> itemAmountList;
        public List<BigDecimal> itemPriceList;
        public List<String> itemSeqIdList;
        public List<BigDecimal> itemShippingList;

        public TaxProductInfo() {
            this.itemProductList = new ArrayList<GenericValue>();
            this.itemAmountList = new ArrayList<BigDecimal>();
            this.itemPriceList = new ArrayList<BigDecimal>();
            this.itemShippingList = new ArrayList<BigDecimal>();
            this.itemSeqIdList = new ArrayList<String>();
        }

        public void addProductInfo(GenericValue product, BigDecimal amount, BigDecimal price, String itemSeqId) {
            this.itemProductList.add(product);
            this.itemAmountList.add(amount);
            this.itemPriceList.add(price);
            this.itemSeqIdList.add(itemSeqId);
            this.itemShippingList.add(BigDecimal.ZERO);
        }
    }

    private static String MODULE = OrderFactory.class.getName();

    /** The <code>Delegator</code>. */
    protected Delegator delegator;
    /** The <code>LocalDispatcher</code>. */
    protected LocalDispatcher dispatcher;
    /** The user that creates the order. */
    protected GenericValue userLogin;
    /** The ProductStore used for the order. */
    protected GenericValue productStore;
    /** The order ship by date. */
    protected Timestamp shipByDate;
    /** The party which is ordered from. */
    protected String fromParty;
    /** The customer party. */
    protected String toParty;
    /** The type of order, SO or PO. */
    protected String orderType;
    /** The currency used for the order, taken from the ProductStore. */
    protected String currencyUomId;
    /** The product store. */
    protected String productStoreId;
    /** The order Id once created. */
    protected String orderId;
    /** The order name */
    protected String orderName;

    /** The list of <code>OrderPaymentPreference</code>. */
    protected List<GenericValue> orderPaymentPreferences;
    /** The list of <code>OrderItemShipGroupInfo</code>. */
    protected List<GenericValue> orderItemShipGroupInfos;
    /** The list of <code>OrderItem</code>. */
    protected List<GenericValue> orderItems;
    /** The list of <code>OrderTerm</code>. */
    protected List<GenericValue> orderTerms;
    /** The list of <code>OrderAdjustement</code>. */
    protected List<GenericValue> orderAdjustments;
    /** <code>OrderContactMechs</code> to create for the order. */
    protected List<GenericValue> orderContactMechs;

    // we keep those extra lists because the entities are merged in orderItemShipGroupInfos
    /** The list of <code>OrderItemShipGroup</code>. */
    protected List<GenericValue> orderItemShipGroups;
    /** The list of <code>OrderItemShipGroupAssoc</code>. */
    protected List<GenericValue> orderItemShipGroupAssocs;

    /** For tax calculation, associate shipping group id to product info. */
    protected Map<String, TaxProductInfo> taxInfoMap;
    /** A flag indicating of sales tax should be calculated. */
    protected Boolean calculateTaxes;

    /** Stores the last item sequence number. */
    protected Integer orderItemSeq;

    /**
     * Constructor for a new Order.
     * @param dctx a <code>DispatchContext</code> value
     * @param userLogin the user login <code>GenericValue</code>
     * @param fromParty the vendor party id
     * @param toParty the customer party id
     * @param productStoreId the product store id
     * @throws GenericEntityException if an error occurs
     */
    public OrderFactory(DispatchContext dctx, GenericValue userLogin, String fromParty, String toParty, String productStoreId) throws GenericEntityException {
        this(dctx.getDelegator(), dctx.getDispatcher(), userLogin, fromParty, toParty, productStoreId);
    }

    /**
     * Constructor for a new Order.
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin the user login <code>GenericValue</code>
     * @param fromParty the vendor party id
     * @param toParty the customer party id
     * @param productStoreId the product store id
     * @throws GenericEntityException if an error occurs
     */
    public OrderFactory(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, String fromParty, String toParty, String productStoreId) throws GenericEntityException {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.userLogin = userLogin;
        this.fromParty = fromParty;
        this.toParty = toParty;
        if (UtilValidate.isNotEmpty(productStoreId)) {
            this.productStoreId = productStoreId;
            this.productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
            this.currencyUomId = (String) productStore.get("defaultCurrencyUomId");
            if (this.currencyUomId == null) {
                Debug.logWarning("defaultCurrencyUomId is NULL for the store " + this.productStoreId + ", you will need to set it manually.", MODULE);
            }
        } else {
            this.productStoreId = null;
            this.currencyUomId = null;
        }


        this.orderItemSeq = 0;
        this.orderId = null;

        this.orderPaymentPreferences = new ArrayList<GenericValue>();
        this.orderItemShipGroupInfos = new ArrayList<GenericValue>();
        this.orderItemShipGroups = new ArrayList<GenericValue>();
        this.orderItemShipGroupAssocs = new ArrayList<GenericValue>();
        this.orderItems = new ArrayList<GenericValue>();
        this.orderTerms = new ArrayList<GenericValue>();
        this.orderAdjustments = new ArrayList<GenericValue>();
        this.orderContactMechs = new ArrayList<GenericValue>();

        this.taxInfoMap = new HashMap<String, TaxProductInfo>();
        this.calculateTaxes = true;
    }

    /**
     * Gets this order currency UOM id.
     * @return this order currency UOM id
     */
    public String getCurrencyUomId() {
        return this.currencyUomId;
    }

    /**
     * Sets this order currency UOM id.
     * @param currencyUomId this order currency UOM id
     */
    public void setCurrencyUomId(String currencyUomId) {
        this.currencyUomId = currencyUomId;
    }

    /**
     * Sets this order ship by date.
     * @param shipByDate this order ship by date
     */
    public void setShipByDate(Timestamp shipByDate) {
        this.shipByDate = shipByDate;
    }

    /**
     * Sets this order to calculate tax when it gets created, defaults to <code>true</code>.
     * @param flag a <code>Boolean</code> value
     */
    public void setCalculateTaxes(Boolean flag) {
        this.calculateTaxes = flag;
    }

    /**
     * Sets this order name.
     * @param orderName name of the order
     */
    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    /**
     * Gets this order <code>orderId</code>.
     * @return this order <code>orderId</code>
     */
    public String getOrderId() {
        return this.orderId;
    }

    /**
     * Gets this order grand total.
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getGrandTotal() {
        if (orderId == null) {
            return null;
        }

        try {
            OrderRepositoryInterface orderRepository = getOrderRepository();

            Order order = orderRepository.getOrderById(orderId);
            return order.getGrandTotal();
        } catch (FoundationException fe) {
            Debug.logError(fe, MODULE);
        }

        return null;
    }

    private OrderRepositoryInterface getOrderRepository() throws InfrastructureException, RepositoryException {
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
        OrderDomainInterface orderDomain = dl.loadDomainsDirectory().getOrderDomain();
        return orderDomain.getOrderRepository();
    }

    /**
     * Gets the list of associated OPP.
     * @return the <code>List</code> of <code>GenericValue</code> representing this order <code>OrderPaymentPreference</code>, or <code>null</code> if the order was not created or an error occurs
     */
    public List<GenericValue> getOrderPaymentPreferences() {
        if (orderId == null) {
            return null;
        }

        try {
            return delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        return null;
    }

    /**
     * Adds a payment preference for the order.
     * @param paymentMethodTypeId the payment method type, ie: <code>CREDIT_CARD</code>
     */
    public void addPaymentMethod(String paymentMethodTypeId) {
        GenericValue opp = delegator.makeValue("OrderPaymentPreference");
        opp.set("paymentMethodTypeId", paymentMethodTypeId);
        if ("CREDIT_CARD".equals(paymentMethodTypeId)) {
            opp.set("statusId", "PAYMENT_NOT_AUTH");
        }
        orderPaymentPreferences.add(opp);
    }

    /**
     * Adds a payment preference for the order.
     * @param paymentMethodTypeId the payment method type, ie: <code>CREDIT_CARD</code>
     * @param paymentMethodId the payment method id
     */
    public void addPaymentMethod(String paymentMethodTypeId, String paymentMethodId) {
        GenericValue opp = delegator.makeValue("OrderPaymentPreference");
        opp.set("paymentMethodTypeId", paymentMethodTypeId);
        opp.set("paymentMethodId", paymentMethodId);
        if ("CREDIT_CARD".equals(paymentMethodTypeId)) {
            opp.set("statusId", "PAYMENT_NOT_AUTH");
        }
        orderPaymentPreferences.add(opp);
    }

    /**
     * Adds a <code>ContachMech</code> to this order for the given purpose.
     * @param contactMechId the contact mech to add
     * @param contactMechPurposeTypeId the purpose, ie: SHIPPING_LOCATION
     */
    public void addOrderContactMech(String contactMechId, String contactMechPurposeTypeId) {
        GenericValue ocm = delegator.makeValue("OrderContactMech");
        ocm.set("contactMechId", contactMechId);
        ocm.set("contactMechPurposeTypeId", contactMechPurposeTypeId);
        orderContactMechs.add(ocm);
    }

    /**
     * Adds a shipping group to the order, the shipping address is taken the fromParty shipping addresses.
     * @param carrierPartyId the carrier party, ie: FEDEX
     * @param shipmentMethodTypeId the shipment method type, ie: GROUND, EXPRESS ...
     * @return the shipping group id which can be used later to add items
     */
    public String addShippingGroup(String carrierPartyId, String shipmentMethodTypeId) {
        String shippingContactMechId = getPartyShippingAddressId(toParty);
        return addShippingGroup(carrierPartyId, shipmentMethodTypeId, shippingContactMechId);
    }

    /**
     * Adds a shipping group to the order with the given shipping address.
     * @param carrierPartyId the carrier party, ie: FEDEX
     * @param shipmentMethodTypeId the shipment method type, ie: GROUND, EXPRESS ...
     * @param contactMechId the shipping address contact mech, should be a Postal Address
     * @return the shipping group id which can be used later to add items
     */
    public String addShippingGroup(String carrierPartyId, String shipmentMethodTypeId, String contactMechId) {
        return addShippingGroup(carrierPartyId, shipmentMethodTypeId, contactMechId, null);
    }

    /**
     * Adds a shipping group to the order.
     * @param carrierPartyId the carrier party, ie: FEDEX
     * @param shipmentMethodTypeId the shipment method type, ie: GROUND, EXPRESS ...
     * @param contactMechId the shipping address contact mech, should be a Postal Address
     * @param supplierPartyId the supplier party id
     * @return the shipping group id which can be used later to add items
     */
    public String addShippingGroup(String carrierPartyId, String shipmentMethodTypeId, String contactMechId, String supplierPartyId) {
        // generate a sequence id for this shipping group
        String shipGroupSeqId = sequencify(orderItemShipGroupInfos.size() + 1);

        GenericValue oisg = delegator.makeValue("OrderItemShipGroup");
        oisg.set("carrierPartyId", carrierPartyId);
        oisg.set("shipGroupSeqId", shipGroupSeqId);
        oisg.set("shipmentMethodTypeId", shipmentMethodTypeId);
        oisg.set("maySplit", Boolean.FALSE); // needed for ShoppingcartItem.loadCartFromOrder
        oisg.set("isGift", Boolean.FALSE); // needed for ShoppingcartItem.loadCartFromOrder
        oisg.set("supplierPartyId", supplierPartyId);
        if (contactMechId != null) {
            oisg.set("contactMechId", contactMechId);
        }
        if (shipByDate != null) {
            oisg.set("shipByDate", shipByDate);
        }
        orderItemShipGroupInfos.add(oisg);
        orderItemShipGroups.add(oisg);

        taxInfoMap.put(shipGroupSeqId, new TaxProductInfo());

        return shipGroupSeqId;
    }

    /**
     * Gets the first ship group's shipGroupSeqId, or if there are no ship groups, create it and return its ship GroupSeqId.
     * @return the first ship group's shipGroupSeqId
     */
    public String getFirstShipGroup() {
        if (orderItemShipGroupInfos.size() > 0) {
            return (String) orderItemShipGroupInfos.get(0).get("shipGroupSeqId");
        } else {
            // else create a default shipping group
            return addShippingGroup("_NA_", "STANDARD");
        }
    }

    private String getOrderItemTypeForProduct(GenericValue product) throws GenericEntityException {
        // the correct order item type is related to the product type
        String productTypeId = "PRODUCT_ORDER_ITEM";
        GenericValue productOrderItemType = delegator.findByPrimaryKeyCache("ProductOrderItemType", UtilMisc.toMap("productTypeId", product.getString("productTypeId"), "orderTypeId", orderType));
        if (UtilValidate.isNotEmpty(productOrderItemType)) {
            productTypeId = productOrderItemType.getString("orderItemTypeId");
        }
        return productTypeId;
    }

    /**
     * Adds the given rental product to the order with specified term and put it in the first shipping group.
     * @param product a product <code>GenericValue</code>
     * @param quantity the quantity of the product to add
     * @param termUomId the rental term
     * @throws GenericServiceException if an error occurs
     */
    public void addRentalProduct(GenericValue product, BigDecimal quantity, String termUomId) throws GenericServiceException {
        addRentalProduct(product, quantity, getFirstShipGroup(), termUomId);
    }

    /**
     * Adds a rental product with specified term to the specified shipping group, using the RECURRING_CHARGE price.
     * @param product a product <code>GenericValue</code>
     * @param quantity the quantity of the product to add
     * @param shipGroupSeqId the ship group for which to add the product
     * @param termUomId the rental term
     * @throws GenericServiceException if an error occurs
     */

    public void addRentalProduct(GenericValue product, BigDecimal quantity, String shipGroupSeqId, String termUomId) throws GenericServiceException {
        // get the product prices
        Map<String, BigDecimal> prices = getProductPrices(product, quantity, "RECURRING_CHARGE", termUomId);

        // do not use the ofbiz RENTAL_ORDER_ITEM type: it only works for room rentals and requires workeffort and fixedasset
        addProduct(product, "RENTAL", quantity, shipGroupSeqId, prices);
    }

    /**
     * Adds the given product to the order and put it in the first shipping group.
     * @param product a product <code>GenericValue</code>
     * @param quantity the quantity of the product to add
     * @throws GenericServiceException if an error occurs
     */
    public void addProduct(GenericValue product, BigDecimal quantity) throws GenericServiceException {
        addProduct(product, quantity, getFirstShipGroup());
    }

    /**
     * Adds the given product to the order and put it in the given shipping group.
     * @param product a product <code>GenericValue</code>
     * @param quantity the quantity of the product to add
     * @param shipGroupSeqId the ship group for which to add the product
     * @throws GenericServiceException if an error occurs
     */
    public void addProduct(GenericValue product, BigDecimal quantity, String shipGroupSeqId) throws GenericServiceException {
        addProduct(product, quantity, shipGroupSeqId, null);
    }

    /**
     * Adds the given product to the order and put it in the given shipping group.
     * @param product a product <code>GenericValue</code>
     * @param quantity the quantity of the product to add
     * @param shipGroupSeqId the ship group for which to add the product
     * @param accountingTags the accounting tags for which to add the product
     * @throws GenericServiceException if an error occurs
     */
    public void addProduct(GenericValue product, BigDecimal quantity, String shipGroupSeqId, Map accountingTags) throws GenericServiceException {
        // get the product prices
        Map<String, BigDecimal> prices = getProductPrices(product, quantity, null, null);

        // the correct order item type is related to the product type
        try {
            String orderItemTypeId = getOrderItemTypeForProduct(product);
            addProduct(product, orderItemTypeId, quantity, shipGroupSeqId, prices, accountingTags);
        } catch (GenericEntityException e) {
            throw new GenericServiceException(e);
        }
    }

    /**
     * Fundamental method for adding a product to the order as an order item and associating with a ship group.
     * The list price and actual price are in the prices Map.
     * @param product a product <code>GenericValue</code>
     * @param orderItemTypeId the order item type
     * @param quantity the quantity of the product to add
     * @param shipGroupSeqId the ship group for which to add the product
     * @param prices the price <code>Map</code> which must have the keys: "price", "listPrice"
     * @throws GenericServiceException if an error occurs
     */
    public void addProduct(GenericValue product, String orderItemTypeId, BigDecimal quantity, String shipGroupSeqId, Map<String, BigDecimal> prices) throws GenericServiceException {
        addProduct(product, orderItemTypeId, quantity, shipGroupSeqId, prices, null);
    }

    /**
     * Fundamental method for adding a product to the order as an order item and associating with a ship group.
     * The list price and actual price are in the prices Map.
     * @param product a product <code>GenericValue</code>
     * @param orderItemTypeId the order item type
     * @param quantity the quantity of the product to add
     * @param shipGroupSeqId the ship group for which to add the product
     * @param prices the price <code>Map</code> which must have the keys: "price", "listPrice"
     * @param accountingTags the accountingTag <code>Map</code>
     * @throws GenericServiceException if an error occurs
     */
    public void addProduct(GenericValue product, String orderItemTypeId, BigDecimal quantity, String shipGroupSeqId, Map<String, BigDecimal> prices, Map accountingTags) throws GenericServiceException {
        // get the order item sequence
        orderItemSeq++;
        String orderItemSeqId = sequencify(orderItemSeq);

        // make the order item
        GenericValue newItem = delegator.makeValue("OrderItem");
        newItem.set("orderItemTypeId", orderItemTypeId);
        newItem.set("productId", product.get("productId"));
        newItem.set("quantity", quantity);
        newItem.set("unitPrice", prices.get("price"));
        newItem.set("unitListPrice", prices.get("listPrice"));
        newItem.set("itemDescription", product.get("productName"));
        newItem.set("orderItemSeqId", orderItemSeqId);
        newItem.set("statusId", "ITEM_CREATED");
        //put accounting tags
        if (accountingTags != null) {
            newItem.putAll(accountingTags);
        }
        orderItems.add(newItem);

        // associate to the given ship group
        GenericValue oisga = delegator.makeValue("OrderItemShipGroupAssoc");
        oisga.set("shipGroupSeqId", shipGroupSeqId);
        oisga.set("orderItemSeqId", orderItemSeqId);
        oisga.set("quantity", quantity);
        orderItemShipGroupInfos.add(oisga);
        orderItemShipGroupAssocs.add(oisga);

        // put in taxInfoMap
        taxInfoMap.get(shipGroupSeqId).addProductInfo(product, quantity.multiply(prices.get("price")), prices.get("price"), orderItemSeqId);

        Debug.logInfo("Added [" + product.get("productId") + "] x " + quantity +  " at price " + prices.get("price") + " with shipGroupSeqId " + shipGroupSeqId + " and orderItemSeqId " + orderItemSeqId, MODULE);
    }

    /**
     * Cancels the given quantity of the given product in the order by automatically finding a corresponding <code>OrderItem</code>.
     * Uses the <code>cancelOrderItemNoAction</code> service to avoid re adding promotions to the order.  Note that this has consequences.
     * @param product a product <code>GenericValue</code>
     * @param quantity the quantity of the product to add
     * @return <code>true</code> on success, else <code>false</code>
     */
    public boolean cancelProduct(GenericValue product, BigDecimal quantity) {
        try {
            String orderItemTypeId = getOrderItemTypeForProduct(product);
            return cancelProduct(product, quantity, orderItemTypeId);
        } catch (GenericEntityException e) {
            return false;
        }
    }

    private boolean cancelProduct(GenericValue product, BigDecimal quantity, String orderItemTypeId) {
        try {
            // find a fitting OrderItem
            List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId, "productId", product.get("productId"), "orderItemTypeId", orderItemTypeId));
            for (GenericValue orderItem : orderItems) {
                BigDecimal orderedQuantity = orderItem.getBigDecimal("quantity");
                String orderItemSeqId = orderItem.getString("orderItemSeqId");
                Debug.logInfo("Found orderItem [" + orderItemSeqId + "] qty " + orderedQuantity + " canceled " + orderItem.get("cancelQuantity") + " want to cancel " + quantity, MODULE);
                if (orderedQuantity.compareTo(quantity) >= 0) {
                    Map<String, Object> callCtxt = new HashMap<String, Object>();
                    callCtxt.put("userLogin", userLogin);
                    callCtxt.put("orderId", orderId);
                    callCtxt.put("orderItemSeqId", orderItemSeqId);
                    callCtxt.put("cancelQuantity", quantity);
                    dispatcher.runSync("cancelOrderItemNoActions", callCtxt);
                    return true;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
        Debug.logError("Did not find any matching OrderItem in sufficient quantity to cancel " + quantity + " Product [" + product.get("productId") + "] from Order [" + orderId + "]", MODULE);
        return false;
    }

    /**
     * Cancels the order.
     * @return <code>true</code> on success, else <code>false</code>
     */
    public boolean cancelOrder() {
        try {
            Map<String, Object> callCtxt = FastMap.newInstance();
            callCtxt.put("orderId", orderId);
            callCtxt.put("userLogin", userLogin);
            dispatcher.runSync("cancelOrderItem", callCtxt);
            return true;
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
        return false;
    }

    /**
     * Appends the given quantity of the given product in the order by calling the opentaps.appendOrderItemBasic service.
     * This will add the item in the first shipping group.
     * @param product a product <code>GenericValue</code>
     * @param quantity the quantity of the product to add
     * @return <code>true</code> on success, else <code>false</code>
     */
    public boolean appendProduct(GenericValue product, BigDecimal quantity) {
        return appendProduct(product, quantity, "00001");
    }

    /**
     * Appends the given quantity of the given product in the given shipGroupSeqId
     * in the order by calling the opentaps.appendOrderItemBasic service.
     * @param product a product <code>GenericValue</code>
     * @param quantity the quantity of the product to add
     * @param shipGroupSeqId the ship group for which to add the product
     * @return <code>true</code> on success, else <code>false</code>
     */
    public boolean appendProduct(GenericValue product, BigDecimal quantity, String shipGroupSeqId) {
        try {
            // get unit price
            Map<String, BigDecimal> prices = getProductPrices(product, quantity, null, null);

            Map<String, Object> callCtxt = new HashMap<String, Object>();
            callCtxt.put("userLogin", userLogin);
            callCtxt.put("orderId", orderId);
            callCtxt.put("shipGroupSeqId", shipGroupSeqId);
            callCtxt.put("productId", product.getString("productId"));
            callCtxt.put("quantity", quantity);
            callCtxt.put("unitPrice", prices.get("price"));
            callCtxt.put("listPrice", prices.get("listPrice"));
            dispatcher.runSync("opentaps.appendOrderItemBasic", callCtxt);
            return true;
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
        Debug.logError("Could not append " + quantity + " Product [" + product.get("productId") + "] to Order [" + orderId + "]", MODULE);
        return false;
    }

    /**
     * Gets the unit prices for a product, assuming null (ie, "PURCHASE") price purpose.
     * @param product a product <code>GenericValue</code>
     * @return a <code>Map</code> containing the defaultPrice, listPrice and price of the product
     * @throws GenericServiceException if an error occurs
     */
    protected Map<String, BigDecimal> getProductPrices(GenericValue product) throws GenericServiceException {
        return getProductPrices(product, new BigDecimal("1.0"), null, null);
    }


    /**
     * Gets unit prices for product given the price purpose.  Term is relevant for rentals only--ie, daily, monthly rate
     * @param product a product <code>GenericValue</code>
     * @param quantity the quantity of the product to add
     * @param productPricePurposeId the price purpose
     * @param termUomId the rental term
     * @return a <code>Map</code> containing the defaultPrice, listPrice and price of the product
     * @throws GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected Map<String, BigDecimal> getProductPrices(GenericValue product, BigDecimal quantity, String productPricePurposeId, String termUomId) throws GenericServiceException {
        Map<String, BigDecimal> results = new HashMap<String, BigDecimal>();
        if (orderType.equals("PURCHASE_ORDER")) {
            // if it is purchase order, then call calculatePurchasePrice service to get supplier product price.
            Map priceContext = FastMap.newInstance();
            priceContext.put("userLogin", userLogin);
            priceContext.put("currencyUomId", currencyUomId);
            priceContext.put("product", product);
            priceContext.put("partyId", fromParty);
            priceContext.put("quantity", quantity);
            priceContext.put("currencyUomId", currencyUomId);
            Map priceResult = dispatcher.runSync("calculatePurchasePrice", priceContext);
            if (!ServiceUtil.isError(priceResult)) {
                Boolean validPriceFound = (Boolean) priceResult.get("validPriceFound");
                if (validPriceFound.booleanValue()) {
                    //if have any matched price, then use it as price
                    BigDecimal price = (BigDecimal) priceResult.get("price");
                    results.put("defaultPrice", price);
                    results.put("listPrice", price);
                    results.put("price", price);
                    return results;
                }
            }
        }
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", userLogin);
        callCtxt.put("partyId", toParty);
        callCtxt.put("product", product);
        callCtxt.put("quantity", quantity);
        callCtxt.put("currencyUomId", currencyUomId);
        callCtxt.put("productPricePurposeId", productPricePurposeId);
        callCtxt.put("termUomId", termUomId);

        Map<String, Object> callResults = dispatcher.runSync("calculateProductPrice", callCtxt);

        BigDecimal defaultPrice = (BigDecimal) callResults.get("defaultPrice");
        BigDecimal listPrice = (BigDecimal) callResults.get("listPrice");
        BigDecimal price = (BigDecimal) callResults.get("price");
        results.put("defaultPrice", defaultPrice);
        results.put("listPrice", listPrice);
        results.put("price", price);
        return results;
    }

    /**
     * Get the first <code>POSTAL_ADDRESS</code> for the given party.
     * @param partyId the party id
     * @return the corresponding <code>contactMechId</code> or null if no <code>POSTAL_ADDRESS</code> is found
     */
    protected String getPartyShippingAddressId(String partyId) {
        try {
            GenericValue postalAddress = EntityUtil.getFirst(delegator.findByAnd("PartyAndContactMech", UtilMisc.toMap("partyId", partyId, "contactMechTypeId", "POSTAL_ADDRESS"), UtilMisc.toList("contactMechId")));
            if (UtilValidate.isNotEmpty(postalAddress)) {
                String pa = (String) postalAddress.get("contactMechId");
                Debug.logInfo("Using postal address [" + pa + "] for party [" + partyId + "]" , MODULE);
                return pa;
            }
        } catch (GenericEntityException e) {
            return null;
        }

        return null;
    }

    /**
     * Converts an <code>Integer</code> to a <code>String</code> sequence Id with normal padding.
     * @param idx the Integer index
     * @return the sequence id as a padded String, for example "00010"
     */
    protected String sequencify(Integer idx) {
        String seqId = idx.toString();
        String padding = "00000";
        int paddingLength = padding.length() - seqId.length();
        if (paddingLength > 0) {
            return padding.substring(0, paddingLength).concat(seqId);
        } else {
            return seqId;
        }
    }

    /**
     * Calculates the taxes and store them in orderAdjustments.
     * This should be called before storeOrder if the flag calculateTaxes is set.
     */
    @SuppressWarnings("unchecked")
    protected void calcTax() {
        Map<String, Object> callCtxt;
        Map<String, Object> callResults;
        for (GenericValue oisg : orderItemShipGroups) {
            String shipGroupSeqId = (String) oisg.get("shipGroupSeqId");
            callCtxt = new HashMap<String, Object>();
            callCtxt.put("userLogin", userLogin);
            callCtxt.put("productStoreId", productStoreId);
            callCtxt.put("payToPartyId", fromParty);
            callCtxt.put("billToPartyId", toParty);

            callCtxt.put("itemProductList", taxInfoMap.get(shipGroupSeqId).itemProductList);
            callCtxt.put("itemAmountList", taxInfoMap.get(shipGroupSeqId).itemAmountList);
            callCtxt.put("itemPriceList", taxInfoMap.get(shipGroupSeqId).itemPriceList);
            callCtxt.put("itemShippingList", taxInfoMap.get(shipGroupSeqId).itemShippingList);
            //callCtxt.put("orderShippingAmount", taxInfoMap.get(shipGroupSeqId).itemAmountList);
            String shippingContactMechId = (String) oisg.get("contactMechId");
            if (UtilValidate.isNotEmpty(shippingContactMechId)) {
                try {
                    GenericValue shippingAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", shippingContactMechId));
                    callCtxt.put("shippingAddress", shippingAddress);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
            }
            try {
                callResults = dispatcher.runSync("calcTax", callCtxt);
                List<GenericValue> resOrderAdjustments = (List<GenericValue>) callResults.get("orderAdjustments");
                List<List<GenericValue>> resItemAdjustments = (List<List<GenericValue>>) callResults.get("itemAdjustments");
                this.orderAdjustments.addAll(resOrderAdjustments);
                // one list per item
                for (int p = 0; p < resItemAdjustments.size(); p++) {
                    List<GenericValue> list = resItemAdjustments.get(p);
                    // then one OrderAdjustment per Tax Auth
                    for (int i = 0; i < list.size(); i++) {
                        GenericValue orderAdjustment = list.get(i);
                        orderAdjustment.set("shipGroupSeqId", shipGroupSeqId);
                        orderAdjustment.set("orderItemSeqId", taxInfoMap.get(shipGroupSeqId).itemSeqIdList.get(p));
                        this.orderAdjustments.add(orderAdjustment);
                    }
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
            }
        }
    }

    /**
     * Process the payments attached to an order.
     * @throws GenericServiceException if an error occurs
     */
    public void processPayments() throws GenericServiceException {
        if (orderId == null) {
            throw new GenericServiceException("Order payments can't be processed until the order is stored");
        }
        Map<String, Object> callResults = dispatcher.runSync("processOrderPayments", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
        if (ServiceUtil.isError(callResults)) {
            throw new GenericServiceException(ServiceUtil.getErrorMessage(callResults));
        }
    }

}
