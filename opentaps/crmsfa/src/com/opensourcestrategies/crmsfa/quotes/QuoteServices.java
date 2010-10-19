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

package com.opensourcestrategies.crmsfa.quotes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.services.CalculateProductPriceService;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.service.ServiceException;

/**
 * Accounts services. The service documentation is in services_quotes.xml.
 */
public final class QuoteServices {

    private QuoteServices() { }

    private static final String MODULE = QuoteServices.class.getName();
    private static final List<String> STAMPS = Arrays.asList("createdStamp", "createdTxStamp", "lastUpdatedStamp", "lastUpdatedTxStamp");

    /**
     * Adds a <code>QuoteItem</code> to a <code>Quote</code>.
     * Sets the given <code>QuoteItemOption</code>.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createQuoteItem(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        DomainsDirectory ddir =  new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin)).loadDomainsDirectory();
        ProductRepositoryInterface productRepository = null;
        Locale locale = UtilCommon.getLocale(context);

        String quoteId = (String) context.get("quoteId");
        String partyId = (String) context.get("partyId");

        if (!security.hasEntityPermission("ORDERMGR", "_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OrderSecurityErrorToRunCreateQuoteItem", locale, MODULE);
        }

        Debug.logInfo("Got quantities " + context.get("quantities"), MODULE);
        Debug.logInfo("Got unitPrices " + context.get("unitPrices"), MODULE);

        try {
            // make the QuoteItem
            GenericValue item = delegator.makeValue("QuoteItem");
            item.setPKFields(context);
            item.setNonPKFields(context);
            if (UtilValidate.isNotEmpty(item.getString("productId")) && UtilValidate.isEmpty(item.getString("description"))) {
                GenericValue product = item.getRelatedOne("Product");
                if (product == null) {
                    return UtilMessage.createAndLogServiceError(UtilMessage.expandLabel("CrmErrorProductNotFound", locale), MODULE);
                }
                item.set("description", product.getString("productName"));
            }

            delegator.setNextSubSeqId(item, "quoteItemSeqId", 5, 1);
            delegator.create(item);
            String quoteItemSeqId = item.getString("quoteItemSeqId");

            // make the QuoteItemOptions
            Map<String, String> quantities = (Map<String, String>) context.get("quantities");
            Map<String, String> unitPrices = (Map<String, String>) context.get("unitPrices");
            if (quantities != null && unitPrices != null) {
                GenericValue optionToSet = null;
                boolean hasMultipleOptions = false;
                for (String key : quantities.keySet()) {
                    BigDecimal quantity = null;
                    BigDecimal unitPrice = null;
                    Debug.logInfo("Got quantity [" + key + "] = " + quantities.get(key), MODULE);
                    GenericValue option = delegator.makeValue("QuoteItemOption");
                    option.set("quoteId", quoteId);
                    option.set("quoteItemSeqId", quoteItemSeqId);
                    delegator.setNextSubSeqId(option, "quoteItemOptionSeqId", 5, 1);
                    String qtyStr = quantities.get(key);
                    if (UtilValidate.isEmpty(qtyStr)) {
                        continue;
                    }

                    try {
                        quantity = new BigDecimal(qtyStr);
                    } catch (NumberFormatException e) {
                        return UtilMessage.createAndLogServiceError(qtyStr + " is not a valid quantity.", MODULE);
                    }

                    option.set("quantity", quantity);

                    String priceStr = unitPrices.get(key);
                    if (UtilValidate.isNotEmpty(priceStr)) {
                        try {
                            unitPrice = new BigDecimal(priceStr);
                        } catch (NumberFormatException e) {
                            return UtilMessage.createAndLogServiceError(priceStr + " is not a valid unit price.", MODULE);
                        }
                        option.set("quoteUnitPrice", unitPrice);
                    } else {
                        //user doesn't provide a price, try to calculate in standard way
                        if (productRepository == null) {
                            productRepository = ddir.getProductDomain().getProductRepository();
                        }

                        Product product = productRepository.getProductById((String) context.get("productId"));
                        if (product != null) {
                            CalculateProductPriceService service = new CalculateProductPriceService();
                            service.setInProduct(delegator.makeValue("Product", product.toMap()));
                            service.setInPartyId(partyId);
                            service.setInQuantity(quantity);
                            service.runSync(new Infrastructure(dispatcher));

                            BigDecimal price = service.getOutPrice();
                            if (price != null) {
                                option.set("quoteUnitPrice", price);
                            }
                        }
                    }

                    delegator.create(option);

                    if (optionToSet == null) {
                        optionToSet = option;
                    } else {
                        hasMultipleOptions = true;
                    }

                }

                // set the first valid quantity as the main quantity / unitPrice
                // unless there are multiple options
                if (!hasMultipleOptions && optionToSet != null) {
                    Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                    input.put("quoteId", quoteId);
                    input.put("quoteItemSeqId", quoteItemSeqId);
                    input.put("quoteItemOptionSeqId", optionToSet.get("quoteItemOptionSeqId"));
                    dispatcher.runSync("setQuoteItemOption", input);
                }
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("quoteId", quoteId);
            results.put("quoteItemSeqId", quoteItemSeqId);
            return results;

        } catch (GeneralException ex) {
            return UtilMessage.createAndLogServiceError("Cannot create QuoteItem for Quote [" + quoteId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Updates a <code>QuoteItem</code>.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> updateQuoteItem(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");
        String quoteItemSeqId = (String) context.get("quoteItemSeqId");

        if (!security.hasEntityPermission("ORDERMGR", "_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OrderSecurityErrorToRunUpdateQuoteItem", locale, MODULE);
        }

        try {
            // get the QuoteItem
            GenericValue item = delegator.findByPrimaryKey("QuoteItem", UtilMisc.toMap("quoteId", quoteId, "quoteItemSeqId", quoteItemSeqId));
            if (item == null) {
                return UtilMessage.createAndLogServiceError("Did not find the Quote Item [" + quoteId + "/" + quoteItemSeqId + "]", MODULE);
            }

            // check if the description is the current productName
            boolean productDescriptionToChange = false;
            if (UtilValidate.isNotEmpty(item.getString("productId")) && UtilValidate.isNotEmpty(context.get("description"))) {
                GenericValue product = item.getRelatedOne("Product");
                if (UtilObject.equalsHelper(product.get("productName"), context.get("description"))) {
                    // if the product id changed, reset the description
                    if (!UtilObject.equalsHelper(item.get("productId"), context.get("productId"))) {
                        productDescriptionToChange = true;
                    }
                }

            }

            item.setNonPKFields(context);

            // if the test from above found the product id changed but the description is still from the previous product
            if (productDescriptionToChange) {
                item.set("description", null);
            }


            // check if the product id is not empty and the description is empty
            if (UtilValidate.isNotEmpty(item.getString("productId")) && UtilValidate.isEmpty(item.getString("description"))) {
                GenericValue product = item.getRelatedOne("Product");
                item.set("description", product.getString("productName"));
            }

            delegator.store(item);

            return ServiceUtil.returnSuccess();

        } catch (GeneralException ex) {
            return UtilMessage.createAndLogServiceError("Cannot update QuoteItem [" + quoteId + "/" + quoteItemSeqId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Removes a <code>QuoteItem</code> an all related entities.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> removeQuoteItem(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");
        String quoteItemSeqId = (String) context.get("quoteItemSeqId");

        if (!security.hasEntityPermission("ORDERMGR", "_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OrderSecurityErrorToRunRemoveQuoteItem", locale, MODULE);
        }

        try {
            // get the QuoteItem
            GenericValue item = delegator.findByPrimaryKey("QuoteItem", UtilMisc.toMap("quoteId", quoteId, "quoteItemSeqId", quoteItemSeqId));

            // remove related entities
            item.removeRelated("QuoteAdjustment");
            item.removeRelated("QuoteItemOption");

            // remove the entity
            item.remove();

            return ServiceUtil.returnSuccess();

        } catch (GeneralException ex) {
            return UtilMessage.createAndLogServiceError("Cannot remove QuoteItem [" + quoteId + "/" + quoteItemSeqId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Sets a Quote status.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> setQuoteStatus(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");
        String statusId = (String) context.get("statusId");

        if (!security.hasEntityPermission("ORDERMGR", "_UPDATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        try {
            // get the Quote
            GenericValue quote = delegator.findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
            if (quote == null) {
                return UtilMessage.createAndLogServiceError("Did not find the Quote [" + quoteId + "]", MODULE);
            }

            // check it is a valid change
            if (!UtilCommon.isValidChange(quote.getString("statusId"), statusId, delegator)) {
                return UtilMessage.createAndLogServiceError("Invalid status change for Quote [" + quoteId + "] from " + quote.getString("statusId") + " to " + statusId, MODULE);
            }

            quote.set("statusId", statusId);
            delegator.store(quote);

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError("Cannot change status for Quote [" + quoteId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Sets a QuoteItemOption values in its related QuoteItem.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> setQuoteItemOption(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");

        if (!security.hasEntityPermission("ORDERMGR", "_UPDATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        try {
            // get the QuoteItemOption
            GenericValue pk = delegator.makeValue("QuoteItemOption");
            pk.setPKFields(context);
            GenericValue option = delegator.findByPrimaryKey("QuoteItemOption", pk);
            if (option == null) {
                return UtilMessage.createAndLogServiceError("Did not find QuoteItemOption for PK [" + pk.getPrimaryKey() + "]", MODULE);
            }

            // get the QuoteItem
            GenericValue item = option.getRelatedOne("QuoteItem");
            item.set("quantity", option.get("quantity"));
            item.set("quoteUnitPrice", option.get("quoteUnitPrice"));
            // make sure a price is set to zero
            if (item.get("quoteUnitPrice") == null) {
                item.set("quoteUnitPrice", BigDecimal.ZERO);
            }

            delegator.store(item);

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError("Cannot set QuoteItemOption for Quote [" + quoteId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Unsets the QuoteItemOption values from the given QuoteItem.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> unsetQuoteItemOption(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");
        String quoteItemSeqId = (String) context.get("quoteItemSeqId");

        if (!security.hasEntityPermission("ORDERMGR", "_UPDATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        try {
            // get the QuoteItem
            GenericValue item = delegator.findByPrimaryKey("QuoteItem", UtilMisc.toMap("quoteId", quoteId, "quoteItemSeqId", quoteItemSeqId));
            if (item == null) {
                return UtilMessage.createAndLogServiceError("Did not find QuoteItem [" + quoteId + "/" + quoteItemSeqId + "]", MODULE);
            }

            // unsets the option values
            item.set("quoteUnitPrice", null);
            item.set("quantity", null);

            delegator.store(item);

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError("Cannot unset QuoteItemOption for QuoteItem [" + quoteId + "/" + quoteItemSeqId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Adds a QuoteItemOption.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> addQuoteItemOption(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");
        String quoteItemSeqId = (String) context.get("quoteItemSeqId");

        if (!security.hasEntityPermission("ORDERMGR", "_UPDATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        try {
            GenericValue item = null;

            // get the QuoteItemOption
            GenericValue option = delegator.makeValue("QuoteItemOption");
            option.setPKFields(context);
            option.setNonPKFields(context);
            delegator.setNextSubSeqId(option, "quoteItemOptionSeqId", 5, 1);
            BigDecimal quoteUnitPrice = (BigDecimal) context.get("quoteUnitPrice");
            if (quoteUnitPrice == null) {
                // calculate price in standard way
                GenericValue quote = delegator.findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
                item = delegator.findByPrimaryKey("QuoteItem", UtilMisc.toMap("quoteId", quoteId, "quoteItemSeqId", quoteItemSeqId));
                GenericValue product = item.getRelatedOne("Product");
                if (product != null) {
                    CalculateProductPriceService service = new CalculateProductPriceService();
                    service.setInProduct(product);
                    service.setInPartyId(quote.getString("partyId"));
                    BigDecimal quantity = (BigDecimal) context.get("quantity");
                    service.setInQuantity(quantity);
                    service.runSync(new Infrastructure(dctx.getDispatcher()));

                    // use calculated price
                    BigDecimal price = service.getOutPrice();
                    if (price != null) {
                        option.set("quoteUnitPrice", price);
                    }
                }
            }
            delegator.create(option);

            if (item == null) {
                item = option.getRelatedOne("QuoteItem");
            }
            if (item.get("quantity") == null && item.get("quoteUnitPrice") == null) {
                item.set("quantity", option.get("quantity"));
                item.set("quoteUnitPrice", option.get("quoteUnitPrice"));
                delegator.store(item);
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("quoteId", quoteId);
            results.put("quoteItemSeqId", quoteItemSeqId);
            results.put("quoteItemOptionSeqId", option.get("quoteItemOptionSeqId"));
            return results;

        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError("Cannot add QuoteItemOption for QuoteItem [" + quoteId + "/" + quoteItemSeqId + "] " + ex.getMessage(), MODULE);
        } catch (ServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (IllegalArgumentException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Updates a QuoteItemOption.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> updateQuoteItemOption(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");
        String quoteItemSeqId = (String) context.get("quoteItemSeqId");

        if (!security.hasEntityPermission("ORDERMGR", "_UPDATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        try {
            // get the QuoteItemOption
            GenericValue pk = delegator.makeValue("QuoteItemOption");
            pk.setPKFields(context);
            GenericValue option = delegator.findByPrimaryKey("QuoteItemOption", pk);

            // update it
            option.setNonPKFields(context);
            delegator.store(option);

            // check if the option being updated is the currently set option
            GenericValue item = option.getRelatedOne("QuoteItem");
            List<GenericValue> options = item.getRelated("QuoteItemOption", UtilMisc.toList("quoteItemOptionSeqId"));
            boolean found = false;
            for (GenericValue o : options) {
                BigDecimal quantity = o.getBigDecimal("quantity");
                BigDecimal unitPrice = o.getBigDecimal("quoteUnitPrice");
                if (UtilObject.equalsHelper(quantity, item.getBigDecimal("quantity")) && UtilObject.equalsHelper(unitPrice, item.getBigDecimal("quoteUnitPrice"))) {
                    found = true;
                    break;
                }
            }

            // unset the current values if the option has been changed
            if (!found) {
                Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                input.put("quoteId", quoteId);
                input.put("quoteItemSeqId", quoteItemSeqId);
                dispatcher.runSync("unsetQuoteItemOption", input);
            }

            return ServiceUtil.returnSuccess();
        } catch (GeneralException ex) {
            return UtilMessage.createAndLogServiceError("Cannot update QuoteItemOption for Quote Item [" + quoteId + "/" + quoteItemSeqId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Removes a QuoteItemOption.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> removeQuoteItemOption(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");
        String quoteItemSeqId = (String) context.get("quoteItemSeqId");

        if (!security.hasEntityPermission("ORDERMGR", "_UPDATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        try {
            // get the QuoteItemOption
            GenericValue pk = delegator.makeValue("QuoteItemOption");
            pk.setPKFields(context);
            delegator.removeByPrimaryKey(pk.getPrimaryKey());

            GenericValue item = delegator.findByPrimaryKey("QuoteItem", UtilMisc.toMap("quoteId", quoteId, "quoteItemSeqId", quoteItemSeqId));
            List<GenericValue> options = item.getRelated("QuoteItemOption", UtilMisc.toList("quoteItemOptionSeqId"));
            boolean found = false;
            for (GenericValue option : options) {
                BigDecimal quantity = option.getBigDecimal("quantity");
                BigDecimal unitPrice = option.getBigDecimal("quoteUnitPrice");
                if (UtilObject.equalsHelper(quantity, item.getBigDecimal("quantity")) && UtilObject.equalsHelper(unitPrice, item.getBigDecimal("quoteUnitPrice"))) {
                    found = true;
                    break;
                }
            }

            // unset the current values if the option has been removed
            if (!found) {
                Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                input.put("quoteId", quoteId);
                input.put("quoteItemSeqId", quoteItemSeqId);
                dispatcher.runSync("unsetQuoteItemOption", input);
            }

            return ServiceUtil.returnSuccess();
        } catch (GeneralException ex) {
            return UtilMessage.createAndLogServiceError("Cannot remove QuoteItemOption for QuoteItem [" + quoteId + "/" + quoteItemSeqId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Copy a Quote.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> copyQuote(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");

        if (!security.hasEntityPermission("ORDERMGR", "_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OrderSecurityErrorToRunCopyQuote", locale, MODULE);
        }

        try {
            // get the Quote
            GenericValue quote = delegator.findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
            if (quote == null) {
                return UtilMessage.createAndLogServiceError("Did not find the Quote [" + quoteId + "]", MODULE);
            }

            // create a new Quote
            Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
            input.putAll(quote.getAllFields());
            if (UtilValidate.isNotEmpty(quote.get("quoteName"))) {
                input.put("quoteName", "Copy of " + quote.get("quoteName"));
            }

            input.remove("quoteId");
            for (String str : STAMPS) {
                input.remove(str);
            }
            input.put("statusId", "QUO_CREATED");

            Map<String, Object> result = dispatcher.runSync("createQuote", input);
            if (!UtilCommon.isSuccess(result)) {
                return UtilMessage.createAndLogServiceError(result, "Cannot copy Quote [" + quoteId + "]", locale, MODULE);
            }
            String newQuoteId = (String) result.get("quoteId");
            Debug.logInfo("Created Quote [" + quoteId + "]", MODULE);

            if ("Y".equals(context.get("copyQuoteItems"))) {
                List<GenericValue> quoteItems = quote.getRelated("QuoteItem");
                for (GenericValue item : quoteItems) {
                    input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                    input.putAll(item.getAllFields());
                    for (String str : STAMPS) {
                        input.remove(str);
                    }
                    input.put("quoteIdTo", newQuoteId);
                    input.put("copyQuoteItemOptions", context.get("copyQuoteItemOptions"));
                    input.put("copyQuoteAdjustments", context.get("copyQuoteAdjustments"));
                    result = dispatcher.runSync("copyQuoteItem", input);
                    if (!UtilCommon.isSuccess(result)) {
                        return UtilMessage.createAndLogServiceError(result, "Cannot copy Quote Item [" + quoteId + "/" + item.get("quoteItemSeqId") + "]", locale, MODULE);
                    }
                }
            }

            if ("Y".equals(context.get("copyQuoteAdjustments"))) {
                List<GenericValue> adjs = quote.getRelated("QuoteAdjustment");
                for (GenericValue adj : adjs) {
                    input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                    input.putAll(adj.getAllFields());
                    for (String str : STAMPS) {
                        input.remove(str);
                    }
                    input.put("quoteId", newQuoteId);
                    result = dispatcher.runSync("createQuoteAdjustment", input);
                    if (!UtilCommon.isSuccess(result)) {
                        return UtilMessage.createAndLogServiceError(result, "Cannot copy Quote Adjustment [" + quoteId + "/" + adj.get("quoteAdjustmentId") + "]", locale, MODULE);
                    }
                }
            }

            if ("Y".equals(context.get("copyQuoteRoles"))) {
                List<GenericValue> roles = quote.getRelated("QuoteRole");
                for (GenericValue role : roles) {
                    if ("REQ_TAKER".equals(role.get("roleTypeId"))) {
                        // skip this role, it is created by the createQuote service
                        continue;
                    }

                    input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                    input.putAll(role.getAllFields());
                    for (String str : STAMPS) {
                        input.remove(str);
                    }
                    input.put("quoteId", newQuoteId);
                    result = dispatcher.runSync("createQuoteRole", input);
                    if (!UtilCommon.isSuccess(result)) {
                        return UtilMessage.createAndLogServiceError(result, "Cannot copy Quote Role [" + quoteId + "/" + role.get("partyId") + "/" + role.get("roleTypeId") + "]", locale, MODULE);
                    }
                }
            }

            if ("Y".equals(context.get("copyQuoteCoefficients"))) {
                List<GenericValue> coeffs = quote.getRelated("QuoteCoefficient");
                for (GenericValue coeff : coeffs) {
                    input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                    input.putAll(coeff.getAllFields());
                    for (String str : STAMPS) {
                        input.remove(str);
                    }
                    input.put("quoteId", newQuoteId);
                    result = dispatcher.runSync("createQuoteCoefficient", input);
                    if (!UtilCommon.isSuccess(result)) {
                        return UtilMessage.createAndLogServiceError(result, "Cannot copy Quote Coefficient [" + quoteId + "/" + coeff.get("coeffName") + "]", locale, MODULE);
                    }
                }
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("quoteId", newQuoteId);
            return results;

        } catch (GeneralException ex) {
            return UtilMessage.createAndLogServiceError("Cannot copy Quote [" + quoteId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Copy a QuoteItem.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> copyQuoteItem(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");
        String quoteItemSeqId = (String) context.get("quoteItemSeqId");

        if (!security.hasEntityPermission("ORDERMGR", "_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OrderSecurityErrorToRunCopyQuoteItem", locale, MODULE);
        }

        try {
            // get the Quote
            GenericValue quoteItem = delegator.findByPrimaryKey("QuoteItem", UtilMisc.toMap("quoteId", quoteId, "quoteItemSeqId", quoteItemSeqId));
            if (quoteItem == null) {
                return UtilMessage.createAndLogServiceError("Did not find the Quote Item [" + quoteId + "/" + quoteItemSeqId + "]", MODULE);
            }

            // create a new Quote
            Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
            input.putAll(quoteItem.getAllFields());
            for (String str : STAMPS) {
                input.remove(str);
            }
            String newQuoteId = (String) context.get("quoteIdTo");
            String newQuoteItemSeqId = (String) context.get("quoteItemSeqIdTo");
            input.put("quoteId", newQuoteId);
            if (UtilValidate.isEmpty(newQuoteId) && UtilValidate.isEmpty(newQuoteItemSeqId)) {
                input.remove("quoteItemSeqId");
            }

            Map<String, Object> result = dispatcher.runSync("createQuoteItem", input);
            if (!UtilCommon.isSuccess(result)) {
                return UtilMessage.createAndLogServiceError(result, "Cannot copy Quote Item [" + quoteId + "]", locale, MODULE);
            }
            newQuoteId = (String) result.get("quoteId");
            newQuoteItemSeqId = (String) result.get("quoteItemSeqId");

            if ("Y".equals(context.get("copyQuoteAdjustments"))) {
                List<GenericValue> adjs = quoteItem.getRelated("QuoteAdjustment");
                for (GenericValue adj : adjs) {
                    input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                    input.putAll(adj.getAllFields());
                    for (String str : STAMPS) {
                        input.remove(str);
                    }
                    input.put("quoteId", newQuoteId);
                    input.put("quoteItemSeqId", newQuoteItemSeqId);
                    result = dispatcher.runSync("createQuoteAdjustment", input);
                    if (!UtilCommon.isSuccess(result)) {
                        return UtilMessage.createAndLogServiceError(result, "Cannot copy Quote Item Adjustment [" + quoteId + "/" + quoteItemSeqId + "/" + adj.get("quoteAdjustmentId") + "]", locale, MODULE);
                    }
                }
            }

            if ("Y".equals(context.get("copyQuoteItemOptions"))) {
                List<GenericValue> options = quoteItem.getRelated("QuoteItemOption");
                for (GenericValue option : options) {
                    input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                    input.putAll(option.getAllFields());
                    for (String str : STAMPS) {
                        input.remove(str);
                    }
                    input.put("quoteId", newQuoteId);
                    input.put("quoteItemSeqId", newQuoteItemSeqId);
                    result = dispatcher.runSync("addQuoteItemOption", input);
                    if (!UtilCommon.isSuccess(result)) {
                        return UtilMessage.createAndLogServiceError(result, "Cannot copy Quote Item Option [" + quoteId + "/" + quoteItemSeqId + "/" + option.get("quoteItemOptionSeqId") + "]", locale, MODULE);
                    }
                }
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("quoteId", newQuoteId);
            results.put("quoteItemSeqId", newQuoteItemSeqId);
            return results;

        } catch (GeneralException ex) {
            return UtilMessage.createAndLogServiceError("Cannot copy Quote Item [" + quoteId + "/" + quoteItemSeqId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Finalize a Quote, check all items have all required fields to proceed.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> finalizeQuote(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");

        if (!security.hasEntityPermission("ORDERMGR", "_UPDATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        try {
            // get the Quote
            GenericValue quote = delegator.findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
            if (quote == null) {
                return UtilMessage.createAndLogServiceError("Did not find the Quote [" + quoteId + "]", MODULE);
            }

            // check all QuoteItem, they must have a product ID, quantity and unit price
            List<GenericValue> items = quote.getRelated("QuoteItem");
            for (GenericValue item : items) {
                if (UtilValidate.isEmpty(item.get("productId"))) {
                    return UtilMessage.createAndLogServiceError("The product ID is required and not set for item [" + item.get("quoteItemSeqId") + "]", MODULE);
                }
                if (UtilValidate.isEmpty(item.get("quantity"))) {
                    return UtilMessage.createAndLogServiceError("The quantity is required and not set for item [" + item.get("quoteItemSeqId") + "]", MODULE);
                }
                if (UtilValidate.isEmpty(item.get("quoteUnitPrice"))) {
                    return UtilMessage.createAndLogServiceError("The unit price is required and not set for item [" + item.get("quoteItemSeqId") + "]", MODULE);
                }
            }

            return ServiceUtil.returnSuccess();

        } catch (GeneralException ex) {
            return UtilMessage.createAndLogServiceError("Cannot finalize Quote [" + quoteId + "] " + ex.getMessage(), MODULE);
        }
    }

    /**
     * Service to create a quote note.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> createQuoteNote(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String note = (String) context.get("note");
        String quoteId = (String) context.get("quoteId");
        String internalNote = (String) context.get("internalNote");

        Map<String, Object> noteCtx = UtilMisc.toMap("note", note, "userLogin", userLogin);

        try {
            Map<String, Object> noteRes = dispatcher.runSync("createNote", noteCtx);
            if (ServiceUtil.isError(noteRes)) {
                return noteRes;
            }

            String noteId = (String) noteRes.get("noteId");
            if (UtilValidate.isEmpty(noteId)) {
                return UtilMessage.createAndLogServiceError("OrderProblemCreatingTheNoteNoNoteIdReturned", locale, MODULE);
            }

            Map<String, String> fields = UtilMisc.toMap("quoteId", quoteId, "noteId", noteId, "internalNote", internalNote);
            GenericValue quoteNote = delegator.makeValue("QuoteNote", fields);
            delegator.create(quoteNote);

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

}
