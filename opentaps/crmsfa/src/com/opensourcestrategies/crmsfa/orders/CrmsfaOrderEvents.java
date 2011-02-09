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

package com.opensourcestrategies.crmsfa.orders;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.opensourcestrategies.crmsfa.util.OfbizErrorMessages;
import com.opensourcestrategies.crmsfa.util.OfbizMessages;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
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
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.order.OrderManagerEvents;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.CheckOutEvents;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.order.OrderEvents;
import org.opentaps.common.order.shoppingcart.OpentapsShoppingCart;
import org.opentaps.common.party.PartyNotFoundException;
import org.opentaps.common.party.PartyReader;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * CrmsfaOrderEvents - Order creation events (http servlets) specific to crmsfa.
 * TODO: refactor errors to use UtilMessage.createAndLogEventError()
 */
public final class CrmsfaOrderEvents {

    private CrmsfaOrderEvents() { }

    private static final String MODULE = CrmsfaOrderEvents.class.getName();
    private static final String errorResource = "CRMSFAUiLabels";

    /**
     * Method to get or initialize a cart in crmsfa.
     * @param request a <code>HttpServletRequest</code> value
     * @return an <code>OpentapsShoppingCart</code> value
     */
    public static OpentapsShoppingCart crmsfaGetOrInitializeCart(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        // if one already exists, return it
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
        if (cart != null) {
            if (cart instanceof OpentapsShoppingCart) {
                return (OpentapsShoppingCart) cart;
            } else {
                OpentapsShoppingCart opentapsCart = new OpentapsShoppingCart(cart);
                session.setAttribute("shoppingCart", opentapsCart);
                return opentapsCart;
            }
        }

        // get or initialize the product store, first from session, then from requeset, then from crmsfa.properties for default
        // I think (but am not sure) that once an item has been added, the productStoreId should be in session, so this prevents someone
        // from over-writing it with a URL parameter
        String productStoreId = (String) session.getAttribute("productStoreId");
        if (productStoreId == null) {
            productStoreId = request.getParameter("productStoreId");
        }
        if (productStoreId == null) {
            Properties configProperties = UtilProperties.getProperties("crmsfa.properties");
            productStoreId = (String) configProperties.get("crmsfa.order.productStoreId");
        }

        GenericValue productStore = null;
        try {
            productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return null;
        }

        if (productStore == null) {
            throw new IllegalArgumentException("Product Store with ID [" + productStoreId + "] not found.  Please configure the default productStoreId in crmsfa.properties.");
        }
        session.setAttribute("productStoreId", productStoreId);

        // initialize a new cart
        String webSiteId = null;
        String billFromVendorPartyId = productStore.getString("payToPartyId");
        String currencyUomId = productStore.getString("defaultCurrencyUomId");
        String billToCustomerPartyId = request.getParameter("partyId");

        OpentapsShoppingCart opentapsCart = new OpentapsShoppingCart(delegator, productStoreId, webSiteId, UtilHttp.getLocale(request), currencyUomId, billToCustomerPartyId, billFromVendorPartyId);
        session.setAttribute("shoppingCart", opentapsCart);
        opentapsCart.setProductStoreId(productStoreId);

        changeOrderParty(request, opentapsCart);

        // set sales channel first from request parameter, then from product store
        if (UtilValidate.isNotEmpty(request.getParameter("salesChannelEnumId"))) {
            opentapsCart.setChannelType(request.getParameter("salesChannelEnumId"));
        } else if ((productStore != null) && (UtilValidate.isNotEmpty(productStore.getString("defaultSalesChannelEnumId")))) {
            opentapsCart.setChannelType(productStore.getString("defaultSalesChannelEnumId"));
        }

        // erase any pre-existing tracking code
        session.removeAttribute("trackingCodeId");

        // set the current user as the default sales rep
        opentapsCart.setCommissionAgent(userLogin.getString("partyId"));

        return opentapsCart;
    }

    /**
     * Destroys the current cart.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String crmsfaDestroyCart(HttpServletRequest request, HttpServletResponse response) {
        return OrderEvents.destroyCart(request, response, "crmsfa");
    }

    /**
     * Counts the matching products with the same condition used in findMatchingSalesProducts.bsh.  This is here because we want to redirect to entry if none found.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String countMatchingProducts(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String searchString = UtilCommon.getParameter(request, "productId");
        EntityCondition condition = getMatchingProductsCondition(searchString);
        try {
            List<GenericValue> matches = delegator.findByCondition("ProductAndGoodIdentification", condition, null, null);
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
     * Gets an entity condition to find products when user enters a string into the add item form.
     * @param searchString a <code>String</code> value
     * @return an <code>EntityCondition</code> value
     */
    public static EntityCondition getMatchingProductsCondition(String searchString) {
        return EntityCondition.makeCondition(EntityOperator.AND,
                         EntityCondition.makeCondition(EntityOperator.OR,
                                                       EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("productId"), EntityOperator.LIKE, EntityFunction.UPPER(searchString + "%")),
                                                       EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("idValue"), EntityOperator.LIKE, EntityFunction.UPPER(searchString + "%"))),
                         EntityCondition.makeCondition("productTypeId", EntityOperator.NOT_EQUAL, "AGGREGATED"),
                         EntityCondition.makeCondition(EntityOperator.OR,
                                                       EntityCondition.makeCondition("isVirtual", EntityOperator.EQUALS, null),
                                                       EntityCondition.makeCondition("isVirtual", EntityOperator.EQUALS, "N"),
                                                       EntityCondition.makeCondition("isVirtual", EntityOperator.NOT_EQUAL, "Y")));
    }

    /**
     * Resume an order and set the partyId of the order.  This is similar to changeOrderParty with one
     * subtle difference:  It does not create a cart if none exists and does not do anything if partyId
     * is missing.  The idea is that a resume action can either resume the cart as is, with its current
     * partyId intact (or lack thereof), or it can resume but change the partyId to the one supplied.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String resumeOrder(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
        String partyId = request.getParameter("partyId");
        if (cart != null && partyId != null) {
            return changeOrderParty(request, cart);
        }
        return "success";
    }

    /**
     * Quick create an order.  Input is orderName, accountPartyId, shipBeforeDate, optional productId.
     * In order for this to work, the productStoreId must be configured in crmsfa.properties.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String quickCreateOrder(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        TimeZone timeZone = UtilCommon.getTimeZone(request);
        Locale locale = UtilHttp.getLocale(request);

        // get the cart first, otherwise product store might not be set in session
        ShoppingCart oldCart = (ShoppingCart) session.getAttribute("shoppingCart");

        // destroy and recreate the cart if it exists (requires product store in session)
        if (oldCart != null) {
            ShoppingCartEvents.destroyCart(request, response);
        }
        OpentapsShoppingCart cart = crmsfaGetOrInitializeCart(request);

        // to store input errors (on error we still have a new cart with good values)
        OfbizMessages errors = new OfbizErrorMessages("CRMSFAErrorLabels", UtilHttp.getLocale(request));

        // validate the shipBeforeDate
        String shipBeforeDateString = request.getParameter("shipBeforeDate");
        if (UtilValidate.isNotEmpty(shipBeforeDateString)) {
            try {
                Timestamp shipBeforeDate = UtilDateTime.getDayEnd(UtilDateTime.stringToTimeStamp(shipBeforeDateString, UtilDateTime.getDateFormat(locale), timeZone, locale), timeZone, locale);
                cart.setDefaultShipBeforeDate(shipBeforeDate);
            } catch (IllegalArgumentException e) {
                errors.add("CrmErrorQuickCreateOrderIllegalDate", UtilMisc.toMap("shipBeforeDate", shipBeforeDateString));
            } catch (ParseException pe) {
                errors.add(pe.getLocalizedMessage());
            }
        }

        // set the order name only if given
        String orderName = request.getParameter("orderName");
        if ((orderName != null) && (orderName.trim().length() > 0)) {
            cart.setOrderName(orderName);
        }

        // parse quantity
        String quantityStr = request.getParameter("quantity");
        BigDecimal quantity = new BigDecimal(1.0);
        if ((quantityStr != null) && (quantityStr.trim().length() > 0)) {
            try {
                quantity = new BigDecimal(quantityStr);
            } catch (NumberFormatException e) {
                quantity = new BigDecimal(1.0);
            }
        }
        // add 1.0 of productId to cart if given
        String productId = request.getParameter("productId");
        if ((productId != null) && (productId.trim().length() > 0)) {
            // if accounting tags are required, add an error (since we do not provide them here)
            Map<String, String> tags = new HashMap<String, String>();
            UtilAccountingTags.addTagParameters(request, tags);
            // cart item attributes, used to provide the accounting tags
            Map attributes = FastMap.newInstance();
            try {
                DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
                DomainsDirectory dd = domainLoader.loadDomainsDirectory();
                OrderRepositoryInterface orderRepository = dd.getOrderDomain().getOrderRepository();
                List<AccountingTagConfigurationForOrganizationAndUsage> missings = orderRepository.validateTagParameters(cart, tags, UtilAccountingTags.TAG_PARAM_PREFIX, productId);
                if (!missings.isEmpty()) {
                    for (AccountingTagConfigurationForOrganizationAndUsage missingTag : missings) {
                        UtilMessage.addError(request, "OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missingTag.getDescription()));
                        UtilMessage.addRequiredFieldError(request, missingTag.getPrefixedName(UtilAccountingTags.TAG_PARAM_PREFIX));
                    }
                    return "error";
                }
                // get the validated accounting tags and set them as cart item attributes
                UtilAccountingTags.addTagParameters(tags, attributes);
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
                return "error";
            }
            // check for survey
            String productStoreId = ProductStoreWorker.getProductStoreId(request);
            List productSurvey = ProductStoreWorker.getProductSurveys(delegator, productStoreId, productId, "CART_ADD");
            if (productSurvey != null && productSurvey.size() > 0) {
                return "survey";
            } else {
                try {
                    cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, attributes, null, null, null, null, null, dispatcher);
                } catch (CartItemModifyException e) {
                    errors.add("CrmErrorModifyCart", UtilMisc.toMap("productId", productId, "message", e.getMessage()));
                } catch (ItemNotFoundException e) {
                    errors.add("CrmErrorProductNotFound", UtilMisc.toMap("productId", productId));
                }
            }
        }

        if (errors.size() > 0) {
            request.setAttribute("_ERROR_MESSAGE_", errors.toHtmlList());
            return "error";
        }

        return "success";
    }

    // This is used for updating or revalidating the order party id
    public static String changeOrderParty(HttpServletRequest request, String partyId, ShoppingCart cart) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        if (partyId == null) {
            partyId = cart.getOrderPartyId();
        }


        if (UtilValidate.isNotEmpty(partyId)) {

            // Reset partyId
            cart.setOrderPartyId(null);
            cart.setBillToCustomerPartyId(null);
            cart.setPlacingCustomerPartyId(null);
            cart.setShipToCustomerPartyId(null);
            cart.setEndUserCustomerPartyId(null);

            partyId = partyId.trim();

            if (partyId.length() == 0) {
                request.setAttribute("_ERROR_MESSAGE_", "Please enter a valid party ID.");
                return "error";
            }

            try {
                PartyReader partyReader = new PartyReader(partyId, delegator);
                if (partyReader.hasClassification("DONOTSHIP_CUSTOMERS")) {
                    String productStoreId = cart.getProductStoreId();
                    if (UtilValidate.isNotEmpty(productStoreId)) {
                        GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));

                        if ("N".equals(productStore.getString("allowDoNotShipOrders"))) {
                            UtilMessage.addError(request, "CrmErrorPartyCannotOrder");
                            return "error";
                        }
                    }
                }
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogEventError(request, e, UtilHttp.getLocale(request), MODULE);
            } catch (PartyNotFoundException e) {
                return UtilMessage.createAndLogEventError(request, "OpentapsError_PartyNotFound", UtilMisc.toMap("partyId", partyId), UtilHttp.getLocale(request), MODULE);
            }

            // TODO: make sure userLogin is permitted to make orders for this account
            cart.setOrderPartyId(partyId);
            cart.setBillToCustomerPartyId(partyId);
            cart.setPlacingCustomerPartyId(partyId);
            cart.setShipToCustomerPartyId(partyId);
            cart.setEndUserCustomerPartyId(partyId);
        }
        return "success";
    }

    // This used to be a stand alone request, but now serves as a subroutine for updateOrderHeaderInfo
    public static String changeOrderParty(HttpServletRequest request) {
        return changeOrderParty(request, null);
    }

    // This used to be a stand alone request, but now serves as a subroutine for updateOrderHeaderInfo
    public static String changeOrderParty(HttpServletRequest request, ShoppingCart cart) {
        if (cart == null) {
            cart = crmsfaGetOrInitializeCart(request);
        }

        // validate partyId input
        String partyId = request.getParameter("partyId");
        return changeOrderParty(request, partyId, cart);
    }

    /**
     * Organizes the ship groups by address and cart item from a multi form.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String crmsfaSetShipmentOptionMulti(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = crmsfaGetOrInitializeCart(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        // transform the multi form data into a set of iteratable maps
        java.util.Collection data = UtilHttp.parseMultiFormData(UtilHttp.getParameterMap(request));

        // each map corresponds to one cart line and its options
        for (Iterator iter = data.iterator(); iter.hasNext();) {
            Map options = (Map) iter.next();

            // the "row" key is the index of the shopping cart item to update
            Integer row = (Integer) options.get("row");
            //if (row == null); // TODO: make an error list if for some reason this happens
            int index = row.intValue();

            // use the helper method to decode and set the options
            processSetShipmentOption(cart, delegator, index, options);
        }

        // return "finished" if the user has selected a shipment address for every item
        Map selectedOptions = getSelectedShipmentOptions(cart);
        Boolean canContinue = (Boolean) selectedOptions.get("canContinue");
        if (canContinue) {
            return "finished";
        }

        return "success";
    }

    /**
     * Process shipment settings for one cart item.  Pass in the parameters, either from a single form or a parsed multi form.
     * Also pass in the index of the shopping cart item to update.
     *
     * @param cart a <code>ShoppingCart</code> value
     * @param delegator a <code>Delegator</code> value
     * @param index the index of ShoppingCartItem to process
     * @param parameters a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    private static void processSetShipmentOption(ShoppingCart cart, Delegator delegator, int index, Map parameters) {

        // get the contact mech id, set to null if empty string submitted
        String contactMechId = (String) parameters.get("contactMechId");
        if ((contactMechId != null) && (contactMechId.trim().length() == 0)) {
            contactMechId = null;
        }

        // get the carrier and method input, which are joined by ^
        String[] matches = ((String) parameters.get("carrierPartyAndShipmentMethodTypeId")).split("\\^");
        String carrierPartyId = matches[0];
        String shipmentMethodTypeId = matches[1];

        // get the item at this index
        ShoppingCartItem item = cart.findCartItem(index);

        /*
         * The strategy is to remove the item from any existing ship groups, and then find the
         * ship group to add it to.  If no group is found, then create a new ship group.
         */

        // remove item from any ship groups
        cart.clearItemShipInfo(item);

        // look for an existing group with the desired contactMechId, carrier party and shipment method
        ShoppingCart.CartShipInfo shipInfo = null;
        for (Iterator iter = cart.getShipGroups().iterator(); iter.hasNext();) {
            ShoppingCart.CartShipInfo candidateInfo = (ShoppingCart.CartShipInfo) iter.next();
            if (candidateInfo.getContactMechId() != null && candidateInfo.getContactMechId().equals(contactMechId)
                    && candidateInfo.carrierPartyId != null && candidateInfo.carrierPartyId.equals(carrierPartyId)
                    && candidateInfo.shipmentMethodTypeId != null && candidateInfo.shipmentMethodTypeId.equals(shipmentMethodTypeId)) {
                shipInfo = candidateInfo;
                break;
            }
        }

        // if not found, create a new one
        if (shipInfo == null) {
            shipInfo = new ShoppingCart.CartShipInfo();
            shipInfo.carrierPartyId = carrierPartyId;
            shipInfo.setContactMechId(contactMechId);
            shipInfo.shipmentMethodTypeId = shipmentMethodTypeId;
            cart.getShipGroups().add(shipInfo);
        }

        // add the item to this ship group
        shipInfo.setItemInfo(item, item.getQuantity());
        shipInfo.resetShipBeforeDateIfAfter(item.getShipBeforeDate());
        shipInfo.resetShipAfterDateIfBefore(item.getShipAfterDate());

        return;
    }

    /**
     * Get maps of selected shipment options keyed to shopping cart item index.
     *
     * There are three selectable options: contactMechId, carrier partyId, and
     * shipmentMethodTypeId.  The cart item index serves as an Integer key.  One
     * map called "selectedContactMechIds" is returned for {index, contactMechId}.
     * Another map called "selectedCarrierMethods" is returned for
     * {index, carrierPartyId + "^" + shipmentMethodTypeId}.
     *
     * There is one more parameter in the map, "canContinue" which
     * contains a Boolean indicating whether all items are assigned to ship
     * groups and thus the user can continue with the checkout process.
     * @param cart a <code>ShoppingCart</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getSelectedShipmentOptions(ShoppingCart cart) {
        Map result = FastMap.newInstance();
        Map selectedContactMechIds = FastMap.newInstance();
        Map selectedCarrierMethods = FastMap.newInstance();

        // keep track of cart items assigned to ship groups
        int numAssignedToGroups = 0;

        for (Iterator iter = cart.items().iterator(); iter.hasNext();) {
            ShoppingCartItem item = (ShoppingCartItem) iter.next();
            int index = cart.getItemIndex(item);

            // ShoppingCartItem can be split among several ship groups, but for this order system we keep each in one ship group
            ShoppingCart.CartShipInfo shipInfo = null;
            Map groupQtyMap = cart.getShipGroups(item);
            if (groupQtyMap.size() > 1) {
                Debug.logWarning("ShoppingCartItem " + index + " is split among " + groupQtyMap.size() + " ship groups.  This is unsupported in CRMSFA.", MODULE);
            }
            for (Iterator groupQtyIter = groupQtyMap.keySet().iterator(); groupQtyIter.hasNext();) {
                Integer shipGrpIndex = (Integer) groupQtyIter.next();
                shipInfo = cart.getShipInfo(shipGrpIndex.intValue());
                break;
            }
            if ((shipInfo != null) && (shipInfo.getContactMechId() != null) && (shipInfo.carrierPartyId != null) && (shipInfo.shipmentMethodTypeId != null)) {
                selectedContactMechIds.put(new Integer(index), shipInfo.getContactMechId());
                selectedCarrierMethods.put(new Integer(index), shipInfo.carrierPartyId + "^" + shipInfo.shipmentMethodTypeId);
                numAssignedToGroups += 1;
            }
        }
        result.put("selectedContactMechIds", selectedContactMechIds);
        result.put("selectedCarrierMethods", selectedCarrierMethods);

        // if all items have been assigned to ship groups, then we can reveal the next step in the process
        boolean canContinue = false;
        if ((numAssignedToGroups > 0) && (numAssignedToGroups == cart.items().size())) {
            canContinue = true;
        }
        result.put("canContinue", new Boolean(canContinue));

        return result;
    }

    /**
     * Updates information in the Order Header Info box in the upper left.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String updateOrderHeaderInfo(HttpServletRequest request, HttpServletResponse response) {
        OpentapsShoppingCart cart = crmsfaGetOrInitializeCart(request);
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        // either set the order name if it changed, or erase it if user submits an empty field
        String orderName = request.getParameter("orderName");
        if (orderName != null && (orderName.trim().length() > 0)) {
            String orderNameTrimmed = orderName.trim();
            if (!orderNameTrimmed.equals(cart.getOrderName())) {
                cart.setOrderName(orderNameTrimmed);
            }
        } else {
            cart.setOrderName(null);
        }
        clearCheckoutInfo(cart, dispatcher);

        // either set the po number if it changed, or erase it if user submits an empty field
        String poNumber = request.getParameter("poNumber");
        if (poNumber != null && (poNumber.trim().length() > 0)) {
            String poNumberTrimmed = poNumber.trim();
            if (!poNumberTrimmed.equals(cart.getPoNumber())) {
                cart.setPoNumber(poNumberTrimmed);
            }
        } else {
            cart.setPoNumber(null);
        }

        // set the sales channel
        cart.setChannelType(request.getParameter("salesChannelEnumId"));

        // if user is changing product store ID, make sure the cart has no items first
        String productStoreId = request.getParameter("productStoreId");
        String sessionProductStoreId = (String) session.getAttribute("productStoreId");
        if (productStoreId != null && !productStoreId.equals(sessionProductStoreId) && cart.size() == 0) {
            try {
                GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
                if (productStore != null) {
                    cart.setProductStoreId(productStoreId);
                    session.setAttribute("productStoreId", productStoreId);

                    // also change the bill from vendor and currency
                    cart.setBillFromVendorPartyId(productStore.getString("payToPartyId"));
                    try {
                        cart.setCurrency(dispatcher, productStore.getString("defaultCurrencyUomId"));
                    } catch (CartItemModifyException e) {
                        // shouldn't happen since there's no items in cart
                        Debug.logError(e, MODULE);
                    }

                    // revalidate the order party id
                    changeOrderParty(request, null, cart);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }

        // set the tracking code
        session.setAttribute("trackingCodeId", request.getParameter("trackingCodeId"));

        String result = changeOrderParty(request, cart);
        if ("error".equals(result)) {
            return result;
        }

        // clear or set the sales rep
        String salesRepPartyId = UtilCommon.getParameter(request, "salesRepPartyId");
        if (salesRepPartyId != null) {
            try {
                GenericValue party = cart.getDelegator().findByPrimaryKey("Party", UtilMisc.toMap("partyId", salesRepPartyId));
                if (party == null) {
                    UtilMessage.addFieldError(request, "salesRepPartyId", "CrmErrorPartyNotFound", UtilMisc.toMap("partyId", salesRepPartyId));
                    return "error";
                }
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogEventError(request, e, UtilHttp.getLocale(request), MODULE);
            }
            // then set the given sales rep
            cart.setCommissionAgent(salesRepPartyId);
        } else {
            // clear out the sales reps
            cart.clearCommissionAgents();
        }

        return "success";
    }

    /**
     * Removes checkout information such as shipping, payment method, etc.  The idea is when
     * the user changes something that these depend on, such as partyId, the cart should remove
     * all partyId related things.  These are mainly in checkout, hence the name.
     * @param cart a <code>ShoppingCart</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     */
    @SuppressWarnings("unchecked")
    public static void clearCheckoutInfo(ShoppingCart cart, LocalDispatcher dispatcher) {
        cart.clearPayments();
        cart.setBillingAccount(null, BigDecimal.ZERO);
        for (Iterator iter = cart.items().iterator(); iter.hasNext();) {
            ShoppingCartItem item = (ShoppingCartItem) iter.next();
            cart.clearItemShipInfo(item);
        }
        // remove any empty ship groups
        cart.cleanUpShipGroups();
        ShoppingCart.CartShipInfo newShipInfo = (ShoppingCart.CartShipInfo) cart.getShipGroups().get(cart.addShipInfo());
        int newIndex = cart.getShipGroups().indexOf(newShipInfo);
        for (Iterator iter = cart.items().iterator(); iter.hasNext();) {
            ShoppingCartItem item = (ShoppingCartItem) iter.next();
            cart.setItemShipGroupQty(item, item.getQuantity(), newIndex);
        }
        try {
            cart.createDropShipGroups(dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * Recalculates all price- or promotion-related portions of the cart.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String recalculatePrices(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = crmsfaGetOrInitializeCart(request);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        cart.clearAllAdjustments();
        cart.clearAllPromotionAdjustments();
        cart.clearAllPromotionInformation();
        cart.clearCartItemUseInPromoInfo();
        cart.clearProductPromoUseInfo();
        cart.removeAllFreeShippingProductPromoActions();
        for (Iterator iter = cart.items().iterator(); iter.hasNext();) {
            ShoppingCartItem item = (ShoppingCartItem) iter.next();
            try {
                item.updatePrice(dispatcher, cart);
            } catch (CartItemModifyException e) {
                Debug.logError(e, MODULE);
            }
        }
        ProductPromoWorker.doPromotions(cart, dispatcher);
        return "success";
    }

    /**
     * Wrapper around the CheckOutEvents.createOrder() method.  This method is to be
     * used in crmsfa instead.  The reason is we wish to perform some actions right after the order
     * is created as part of the order creation process.  This is the only sure way to hook up
     * pre-creation and post-creation logic without using controller request chains.
     *
     * No matter what kind of errors occur in our own post order creation logic, this method should not
     * abort with "error" on its own.  It should always return the results of CheckOutEvents.createOrder()
     * so that the controller chain may continue and do things like process payments and destroy the cart.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String crmsfaCreateOrder(HttpServletRequest request, HttpServletResponse response) {
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

        // the cart should still be around because destroying it is the last part of the controller chain after this event
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");

        // some services have to be run as system
        GenericValue system = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));

        // if a tracking code is in session and we have a sales order, associate it with the order
        if ("SALES_ORDER".equals(cart.getOrderType())) {
            String trackingCodeId = (String) session.getAttribute("trackingCodeId");
            if (trackingCodeId != null && trackingCodeId.trim().length() > 0) {
                GenericValue trackingCode = delegator.findByPrimaryKey("TrackingCode", UtilMisc.toMap("trackingCodeId", trackingCodeId));

                // Associate tracking code with order, which has to be done by hand because no such service exists in ofbiz (yet)
                Map input = UtilMisc.toMap("orderId", cart.getOrderId(), "trackingCodeId", trackingCodeId, "trackingCodeTypeId", trackingCode.get("trackingCodeTypeId"));
                GenericValue trackingCodeOrder = delegator.makeValue("TrackingCodeOrder", input);
                trackingCodeOrder.create();


                // Associate the billing party with the marketing campaign of the tracking code
                GenericValue campaign = trackingCode.getRelatedOne("MarketingCampaign");
                if (campaign != null) {
                    String billToPartyId = cart.getBillToCustomerPartyId();
                    input = UtilMisc.toMap("marketingCampaignId", campaign.get("marketingCampaignId"), "partyId", billToPartyId, "roleTypeId", "BILL_TO_CUSTOMER");
                    input.put("userLogin", system);
                    Map results = dispatcher.runSync("createMarketingCampaignRole", input);
                    if (ServiceUtil.isError(results)) {
                        Debug.logError(ServiceUtil.getErrorMessage(results), MODULE);
                    }
                }

                session.removeAttribute("trackingCodeId");
            }
        }

        // Update the OrderItemShipGroup records with any third-party billing information for the ship groups
        if (cart instanceof OpentapsShoppingCart) {
            OpentapsShoppingCart opentapsCart = (OpentapsShoppingCart) cart;
            List orderItemShipGroups = delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", cart.getOrderId()));
            Iterator oisgit = orderItemShipGroups.iterator();
            if (UtilValidate.isNotEmpty(oisgit)) {
                while (oisgit.hasNext()) {
                    GenericValue orderItemShipGroup = (GenericValue) oisgit.next();
                    int shipGroupSeqId = Integer.valueOf(orderItemShipGroup.getString("shipGroupSeqId")).intValue() - 1; // DB value is 1-indexed
                    orderItemShipGroup.set("thirdPartyAccountNumber", opentapsCart.getThirdPartyAccountNumber(shipGroupSeqId));
                    orderItemShipGroup.set("thirdPartyCountryGeoCode", opentapsCart.getThirdPartyCountryCode(shipGroupSeqId));
                    orderItemShipGroup.set("thirdPartyPostalCode", opentapsCart.getThirdPartyPostalCode(shipGroupSeqId));
                }
            }
            delegator.storeAll(orderItemShipGroups);
        }
    }

    /**
     * Validates input for the createOrderParty event.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String createOrderPartyValidation(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = CrmsfaOrderEvents.crmsfaGetOrInitializeCart(request);
        Locale locale = cart.getLocale();
        List errorLabels = FastList.newInstance();

        String groupName = UtilCommon.getParameter(request, "groupName");
        String firstName = UtilCommon.getParameter(request, "firstName");
        String lastName = UtilCommon.getParameter(request, "lastName");
        if (groupName == null && firstName == null && lastName == null) {
            errorLabels.add("CrmError_MissingNames");
        } else if (firstName != null && lastName == null) {
            errorLabels.add("CrmError_MissingLastName");
        } else if (firstName == null && lastName != null) {
            errorLabels.add("CrmError_MissingFirstName");
        }

        if (null == UtilCommon.getParameter(request, "billingToName")) {
            errorLabels.add("CrmError-MissingBillingToName");
        }
        if (null == UtilCommon.getParameter(request, "billingAddress1")) {
            errorLabels.add("CrmError_MissingBillingAddress1");
        }
        if (null == UtilCommon.getParameter(request, "billingCity")) {
            errorLabels.add("CrmError_MissingBillingCity");
        }
        String billingCountryGeoId = UtilCommon.getParameter(request, "billingCountryGeoId");
        if (billingCountryGeoId == null) {
            errorLabels.add("CrmError_MissingBillingCountry");
        } else if ("USA".equals(billingCountryGeoId)) {
            if (null == UtilCommon.getParameter(request, "billingStateProvinceGeoId")) {
                errorLabels.add("CrmError_MissingBillingState");
            }
        }
        if (null == UtilCommon.getParameter(request, "billingPostalCode")) {
            errorLabels.add("CrmError_MissingBillingPostalCode");
        }

        // Only validate the general address if the user has opted for it to be different from the billing address
        if (!"Y".equals(UtilCommon.getParameter(request, "generalSameAsBillingAddress"))) {
            if (null == UtilCommon.getParameter(request, "generalToName")) {
                errorLabels.add("CrmError-MissingShippingToName");
            }
            if (null == UtilCommon.getParameter(request, "generalAddress1")) {
                errorLabels.add("CrmError_MissingShippingAddress1");
            }
            if (null == UtilCommon.getParameter(request, "generalCity")) {
                errorLabels.add("CrmError_MissingShippingCity");
            }
            String generalCountryGeoId = UtilCommon.getParameter(request, "generalCountryGeoId");
            if (generalCountryGeoId == null) {
                errorLabels.add("CrmError_MissingShippingCountry");
            } else if ("USA".equals(generalCountryGeoId)) {
                if (null == UtilCommon.getParameter(request, "generalStateProvinceGeoId")) {
                    errorLabels.add("CrmError_MissingShippingState");
                }
            }
            if (null == UtilCommon.getParameter(request, "generalPostalCode")) {
                errorLabels.add("CrmError_MissingShippingPostalCode");
            }
        }

        String cardNumber = UtilCommon.getParameter(request, "cardNumber");
        if (cardNumber != null) {
            if (groupName != null) {
                if (firstName == null) {
                    errorLabels.add("CrmError_MissingCCFirstName");
                }
                if (lastName == null) {
                    errorLabels.add("CrmError_MissingCCLastName");
                }
            }
            if (null == UtilCommon.getParameter(request, "cardType")) {
                errorLabels.add("CrmError_MissingCardType");
            }
            if (null == UtilCommon.getParameter(request, "expMonth")) {
                errorLabels.add("CrmError_MissingExpMonth");
            }
            if (null == UtilCommon.getParameter(request, "expYear")) {
                errorLabels.add("CrmError_MissingExpYear");
            }
        }

        if (errorLabels.size() > 0) {
            return UtilMessage.createAndLogEventErrors(request, errorLabels, locale, MODULE);
        }

        return "success";
    }

    /**
     * Creates a new party for the order and any address, credit card info and whatever else the form specifies.
     * This method will also create CRMSFA objects as follows:
     * <ul>
     * <li>If a Company Name (groupName) is specified, an Account will be created.</li>
     * <li>If the customer name (firstName + lastName) is specified, a Contact will be created.</li>
     * <li>If both company name and customer name are specified, the contact will be associated with the account.</li>
     * <li>The order will be associated with the account.  If not present, then with the contact.</li>
     * <li>Creates a credit card and associates it with account and contact.</li>
     * </ul>
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String createOrderParty(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = CrmsfaOrderEvents.crmsfaGetOrInitializeCart(request);
        boolean transaction = false;
        try {
            transaction = TransactionUtil.begin();
            if (!transaction) {
                return "error"; // TODO: return an opentaps generic error, which I thought was created but can't find it
            }
            try {
                Map results = createOrderPartyPrivate(request, response, cart);
                if (ServiceUtil.isError(results)) {
                    TransactionUtil.rollback();
                    return UtilMessage.createAndLogEventError(request, results, cart.getLocale(), MODULE);
                } else {
                    TransactionUtil.commit();
                    return "success";
                }
            } catch (GenericEntityException e) {
                TransactionUtil.rollback();
                return UtilMessage.createAndLogEventError(request, e, cart.getLocale(), MODULE);
            } catch (GenericServiceException e) {
                TransactionUtil.rollback();
                return UtilMessage.createAndLogEventError(request, e, cart.getLocale(), MODULE);
            }
        } catch (GenericTransactionException e) {
            return UtilMessage.createAndLogEventError(request, e, cart.getLocale(), MODULE);
        }
    }

    // The actual method is here because of the way it's wrapped in a transaction
    @SuppressWarnings("unchecked")
    private static Map createOrderPartyPrivate(HttpServletRequest request, HttpServletResponse response, ShoppingCart cart) throws GenericServiceException, GenericEntityException {
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = cart.getLocale();

        String groupName = UtilCommon.getParameter(request, "groupName");
        String firstName = UtilCommon.getParameter(request, "firstName");
        String lastName = UtilCommon.getParameter(request, "lastName");
        String primaryPhoneCountryCode = UtilCommon.getParameter(request, "primaryPhoneCountryCode");
        String primaryPhoneAreaCode = UtilCommon.getParameter(request, "primaryPhoneAreaCode");
        String primaryPhoneNumber = UtilCommon.getParameter(request, "primaryPhoneNumber");
        String primaryPhoneExtension = UtilCommon.getParameter(request, "primaryPhoneExtension");
        String primaryEmail = UtilCommon.getParameter(request, "primaryEmail");
        boolean generalSameAsBillingAddress = "Y".equals(UtilCommon.getParameter(request, "generalSameAsBillingAddress"));

        String accountPartyId = null;
        String contactPartyId = null;

        if (groupName != null) {
            Map results = dispatcher.runSync("crmsfa.createAccount", UtilMisc.toMap("userLogin", userLogin, "accountName", groupName));
            if (ServiceUtil.isError(results)) {
                return results;
            }
            accountPartyId = (String) results.get("partyId");
        }
        if (firstName != null && lastName != null) {
            Map results = dispatcher.runSync("crmsfa.createContact", UtilMisc.toMap("userLogin", userLogin, "firstName", firstName, "lastName", lastName));
            if (ServiceUtil.isError(results)) {
                return results;
            }
            contactPartyId = (String) results.get("partyId");
        }
        if (contactPartyId == null && accountPartyId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(errorResource, "CrmErrorCreateOrderPartyRequiredFields", locale));
        }
        if (contactPartyId != null && accountPartyId != null) {
            Map results = dispatcher.runSync("crmsfa.assignContactToAccount", UtilMisc.toMap("userLogin", userLogin, "contactPartyId", contactPartyId, "accountPartyId", accountPartyId));
            if (ServiceUtil.isError(results)) {
                return results;
            }
        }

        // associate the order with the account, otherwise the contact
        String orderPartyId = (accountPartyId == null ? contactPartyId : accountPartyId);
        cart.setOrderPartyId(orderPartyId);
        cart.setBillToCustomerPartyId(orderPartyId);

        // Create the postal address as general + shipping for the order party.
        Map input = FastMap.newInstance();
        input.put("userLogin", userLogin);
        input.put("partyId", orderPartyId);
        if (generalSameAsBillingAddress) {
            input.put("generalToName", UtilCommon.getParameter(request, "billingToName"));
            input.put("generalAttnName", UtilCommon.getParameter(request, "billingAttnName"));
            input.put("generalAddress1", UtilCommon.getParameter(request, "billingAddress1"));
            input.put("generalAddress2", UtilCommon.getParameter(request, "billingAddress2"));
            input.put("generalCity", UtilCommon.getParameter(request, "billingCity"));
            input.put("generalPostalCode", UtilCommon.getParameter(request, "billingPostalCode"));
            input.put("generalPostalCodeExt", UtilCommon.getParameter(request, "billingPostalCodeExt"));
            input.put("generalStateProvinceGeoId", UtilCommon.getParameter(request, "billingStateProvinceGeoId"));
            input.put("generalCountryGeoId", UtilCommon.getParameter(request, "billingCountryGeoId"));
        } else {
            input.put("generalToName", UtilCommon.getParameter(request, "generalToName"));
            input.put("generalAttnName", UtilCommon.getParameter(request, "generalAttnName"));
            input.put("generalAddress1", UtilCommon.getParameter(request, "generalAddress1"));
            input.put("generalAddress2", UtilCommon.getParameter(request, "generalAddress2"));
            input.put("generalCity", UtilCommon.getParameter(request, "generalCity"));
            input.put("generalPostalCode", UtilCommon.getParameter(request, "generalPostalCode"));
            input.put("generalPostalCodeExt", UtilCommon.getParameter(request, "generalPostalCodeExt"));
            input.put("generalStateProvinceGeoId", UtilCommon.getParameter(request, "generalStateProvinceGeoId"));
            input.put("generalCountryGeoId", UtilCommon.getParameter(request, "generalCountryGeoId"));
        }
        if (UtilValidate.isNotEmpty(primaryEmail)) {
            input.put("primaryEmail", primaryEmail);
        }
        if (UtilValidate.isNotEmpty(primaryPhoneNumber)) {
            input.put("primaryPhoneCountryCode", primaryPhoneCountryCode);
            input.put("primaryPhoneAreaCode", primaryPhoneAreaCode);
            input.put("primaryPhoneNumber", primaryPhoneNumber);
            input.put("primaryPhoneExtension", primaryPhoneExtension);
        }
        Map results = dispatcher.runSync("crmsfa.createBasicContactInfoForParty", input);
        if (ServiceUtil.isError(results)) {
            return results;
        }
        String contactMechId = (String) results.get("generalAddressContactMechId");

        // If address created for the Account and the Contact also exists, then associate this address with the Contact.
        if (contactPartyId != null && accountPartyId != null) {
            input = UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "contactMechId", contactMechId);
            input.put("contactMechPurposeTypeId", "GENERAL_LOCATION");
            input.put("contactMechTypeId", "POSTAL_ADDRESS");
            results = dispatcher.runSync("createPartyContactMech", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
            input.put("contactMechPurposeTypeId", "SHIPPING_LOCATION");
            input.remove("contactMechTypeId");
            results = dispatcher.runSync("createPartyContactMechPurpose", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
        }

        // if billing address is same as shipping, then add a billing purpose
        if (generalSameAsBillingAddress && contactPartyId != null) {
            input = UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "contactMechId", contactMechId);
            input.put("contactMechPurposeTypeId", "BILLING_LOCATION");
            results = dispatcher.runSync("createPartyContactMechPurpose", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
        }
        if (generalSameAsBillingAddress && accountPartyId != null) {
            input = UtilMisc.toMap("userLogin", userLogin, "partyId", accountPartyId, "contactMechId", contactMechId);
            input.put("contactMechPurposeTypeId", "BILLING_LOCATION");
            results = dispatcher.runSync("createPartyContactMechPurpose", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
        }

        // otherwise make a new postal address and associate it as billing to the Account and Contact
        if (!generalSameAsBillingAddress) {
            input = UtilMisc.toMap("userLogin", userLogin);
            input.put("toName", UtilCommon.getParameter(request, "billingToName"));
            input.put("attnName", UtilCommon.getParameter(request, "billingAttnName"));
            input.put("address1", UtilCommon.getParameter(request, "billingAddress1"));
            input.put("address2", UtilCommon.getParameter(request, "billingAddress2"));
            input.put("city", UtilCommon.getParameter(request, "billingCity"));
            input.put("postalCode", UtilCommon.getParameter(request, "billingPostalCode"));
            input.put("postalCodeExt", UtilCommon.getParameter(request, "billingPostalCodeExt"));
            input.put("stateProvinceGeoId", UtilCommon.getParameter(request, "billingStateProvinceGeoId"));
            input.put("countryGeoId", UtilCommon.getParameter(request, "billingCountryGeoId"));
            results = dispatcher.runSync("createPostalAddress", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
            String billingContactMechId = (String) results.get("contactMechId");

            if (contactPartyId != null) {
                input = UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "contactMechId", billingContactMechId);
                input.put("contactMechPurposeTypeId", "BILLING_LOCATION");
                input.put("contactMechTypeId", "POSTAL_ADDRESS");
                results = dispatcher.runSync("createPartyContactMech", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
            }
            if (accountPartyId != null) {
                input = UtilMisc.toMap("userLogin", userLogin, "partyId", accountPartyId, "contactMechId", billingContactMechId);
                input.put("contactMechPurposeTypeId", "BILLING_LOCATION");
                input.put("contactMechTypeId", "POSTAL_ADDRESS");
                results = dispatcher.runSync("createPartyContactMech", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
            }
        }

        // Create a credit card for the orderPartyId if supplied, but only if contactPartyId exists (which implies first and last names)
        String cardNumber = UtilCommon.getParameter(request, "cardNumber");
        String expYear = UtilCommon.getParameter(request, "expYear");
        String expMonth = UtilCommon.getParameter(request, "expMonth");
        String cardType = UtilCommon.getParameter(request, "cardType");
        if (cardNumber != null && expYear != null && expMonth != null && contactPartyId != null && cardType != null && contactPartyId != null) {
            input = UtilMisc.toMap("userLogin", userLogin, "cardType", cardType, "cardNumber", cardNumber, "expYear", expYear, "expMonth", expMonth);
            input.put("firstNameOnCard", firstName);
            input.put("lastNameOnCard", lastName);
            input.put("partyId", orderPartyId); // again, note that we're assigning to the account preferentially, the model is limited in that no two parties can share the same CC
            results = dispatcher.runSync("createCreditCard", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Receives a Payment for the order as an <code>OrderPaymentPreference</code>, disburse the over payment.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String receiveOfflinePaymentsAndDisburseChange(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);

        String orderId = request.getParameter("orderId");
        String disbursementAmountStr = request.getParameter("disbursementAmount");
        BigDecimal disbursementAmount = null;
        try {
            disbursementAmount = UtilValidate.isNotEmpty(disbursementAmountStr) ? new BigDecimal(disbursementAmountStr) : new BigDecimal("0");
        } catch (NumberFormatException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        boolean transaction = false;

        try {

            transaction = TransactionUtil.begin();

            try {

                String returnMessage = OrderManagerEvents.receiveOfflinePayment(request, response);
                if ("error".equals(returnMessage)) {
                    TransactionUtil.rollback();
                    return UtilMessage.createAndLogEventError(request, "OpentapsError_ReceiveOfflinePayment", locale, MODULE);
                }
                if (disbursementAmount.compareTo(BigDecimal.ZERO) != 0) {
                    Map disburseChangeResult = dispatcher.runSync("opentaps.disburseChangeForOrder", UtilMisc.toMap("orderId", orderId, "disbursementAmount", disbursementAmount, "userLogin", userLogin));
                    if (ServiceUtil.isError(disburseChangeResult)) {
                        TransactionUtil.rollback();
                        return UtilMessage.createAndLogEventError(request, disburseChangeResult, locale, MODULE);
                    }
                }

            } catch (GenericEntityException e) {
                TransactionUtil.rollback();
                return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
            } catch (GenericServiceException e) {
                TransactionUtil.rollback();
                return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
            }

            TransactionUtil.commit(transaction);
            return "success";

        } catch (GenericTransactionException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }
    }

    /**
     * Sets the <code>CVV</code> code for Credit Card payment.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String setCVV(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = CrmsfaOrderEvents.crmsfaGetOrInitializeCart(request);
        String paymentMethodId = UtilCommon.getParameter(request, "paymentMethodId");
        String cvv = UtilCommon.getParameter(request, "cvv");

        // find the payment info, if selected, and set its cvv value
        for (int i = 0; i < cart.selectedPayments(); i++) {
            ShoppingCart.CartPaymentInfo info = cart.getPaymentInfo(i);
            if ("CREDIT_CARD".equals(info.paymentMethodTypeId) && info.paymentMethodId.equals(paymentMethodId)) {
                info.securityCode = cvv;
                break;
            }
        }

        return "success";
    }

    /**
     * Checks CRMSFA_INVOICE_VIEW permission for user and resolves organizationPartyId
     * which is simply the crmsfa organization.
     * This event handler has to be
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String checkInvoiceReportPermissions(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        Security security = (Security) request.getAttribute("security");
        if (!security.hasEntityPermission("CRMSFA_INVOICE", "_VIEW", userLogin)) {
            return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionPrintInvoice", locale, MODULE);
        }

        return "success";
    }

}
