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

package org.opentaps.common.order;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.DataModelConstants;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.entities.OrderAdjustmentBilling;
import org.opentaps.base.entities.OrderItemShipGroupAssoc;
import org.opentaps.base.entities.SupplierProduct;
import org.opentaps.common.order.shoppingcart.OpentapsShoppingCart;
import org.opentaps.common.product.UtilProduct;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.purchasing.PurchasingRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Common order services.
 */
public final class OrderServices {

    private OrderServices() { }

    private static String MODULE = OrderServices.class.getName();
    private static int decimals = UtilNumber.getBigDecimalScale("order.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");

    private static int ORDER_ITEM_PADDING = 5;

    /**
     * Sets the OrderHeader.billFromPartyId = OrderRole BILL_FROM_VENDOR and OH.billToPartyId = OrderRole BILL_TO_CUSTOMER.
     * Meant to be run as a SECA on storeOrder in the background.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map setOrderHeaderPartiesFromRoles(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (!security.hasEntityPermission("ORDERMGR", "_UPDATE", userLogin)) {
            return ServiceUtil.returnError("This service requires ORDERMGR_UPDATE permission and is only meant for the [system] user to run as part of an automated SECA.");
        }

        try {
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            OrderReadHelper orh = new OrderReadHelper(orderHeader);

            if (UtilValidate.isNotEmpty(orh.getBillFromParty())) {
                orderHeader.set("billFromPartyId", orh.getBillFromParty().getString("partyId"));
            } else {
                Debug.logWarning("Order [" + orderId + "] has no bill from party", MODULE);

            }
            if (UtilValidate.isNotEmpty(orh.getBillToParty())) {
                orderHeader.set("billToPartyId", orh.getBillToParty().getString("partyId"));
            } else {
                Debug.logWarning("Order [" + orderId + "] has no bill to party", MODULE);

            }
            orderHeader.store();

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError("Cannot update OrderHeader for [" + orderId + "] " + ex.getMessage());
        }
    }

    /**
     * Approves third party orders when created.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map autoApproveThirdPartyOrder(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String orderId = (String) context.get("orderId");
        try {
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isEmpty(orderHeader)) {
                return ServiceUtil.returnError("Order [" + orderId + "] not found for approving this order based on third-party billing");
            }
            if (!"ORDER_CREATED".equals(orderHeader.get("statusId"))) {
                Debug.logWarning("Order [" + orderId + "] is not in created state, not approving this order based on third-party billing", MODULE);
                return ServiceUtil.returnSuccess();
            }

            List prefs = orderHeader.getRelated("OrderPaymentPreference");
            if (prefs.size() == 0) {
                Debug.logWarning("Order [" + orderId + "] has no OrderPaymentPreferences, not approving this order based on third-party billing", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // tally up all the payment methods which are not yet final for this order
            // TODO: There should actually be an authorize step somewhere in here
            int unfulfilled = 0;
            for (Iterator iter = prefs.iterator(); iter.hasNext();) {
                GenericValue pref = (GenericValue) iter.next();
                if ("EXT_BILL_3RDPTY".equals(pref.get("paymentMethodTypeId")) && (
                        ("PAYMENT_NOT_RECEIVED".equals(pref.get("statusId")))
                        || ("PAYMENT_NOT_AUTH".equals(pref.get("statusId")))
                        || ("PAYMENT_AUTHORIZED".equals(pref.get("statusId"))))) {
                    unfulfilled += 1;
                }
            }

            if (unfulfilled == prefs.size()) {
                Map results = dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin, "statusId", "ITEM_APPROVED"));
                if (ServiceUtil.isError(results)) {
                    return results;
                }
            } else {
                Debug.logInfo("No payments which are bill-to-third party and not yet received for order [" + orderId + "], not approving this order based on third-party billing", MODULE);
            }

            return ServiceUtil.returnSuccess();
        } catch (GeneralException ex) {
            return ServiceUtil.returnError("Cannot fullfill third party order [" + orderId + "]: " + ex.getMessage());
        }
    }

    /**
     * Creates a cash customer refund against an order.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map disburseChangeForOrder(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");
        BigDecimal disbursementAmount = (BigDecimal) context.get("disbursementAmount");

        Map result = ServiceUtil.returnSuccess();

        try {

            if (UtilValidate.isEmpty(disbursementAmount) || disbursementAmount.doubleValue() == 0) {
                return result;
            }
            disbursementAmount = disbursementAmount.abs();

            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isEmpty(orderHeader)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_OrderNotFound", UtilMisc.toMap("orderId", orderId), locale, MODULE);
            }

            OrderReadHelper orh = new OrderReadHelper(orderHeader);

            // Construct a payment of type CUSTOMER_REFUND and paymentMethodTypeId CASH
            GenericValue billToParty = orh.getBillToParty();
            GenericValue billFromParty = orh.getBillFromParty();
            Map paymentContext = UtilMisc.toMap("paymentMethodTypeId", "CASH", "statusId", "PMNT_SENT", "currencyUomId", orh.getCurrency(), "userLogin", userLogin);
            paymentContext.put("paymentTypeId", "CUSTOMER_REFUND");
            paymentContext.put("amount", disbursementAmount);
            paymentContext.put("partyIdTo", billToParty.get("partyId"));
            paymentContext.put("partyIdFrom", billFromParty.get("partyId"));
            Map createPaymentResult = dispatcher.runSync("createPayment", paymentContext);
            if (ServiceUtil.isError(createPaymentResult)) {
                return createPaymentResult;
            }
            String disbursementPaymentId = (String) createPaymentResult.get("paymentId");
            result.put("disbursementPaymentId", disbursementPaymentId);

            // Create an OrderPaymentPreference for a negative maxAmount
            String orderPaymentPreferenceId = delegator.getNextSeqId("OrderPaymentPreference");
            GenericValue paymentPref = delegator.makeValue("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreferenceId));
            paymentPref.set("paymentMethodTypeId", "CASH");
            paymentPref.set("maxAmount", disbursementAmount.negate());
            paymentPref.set("statusId", "PAYMENT_RECEIVED");
            paymentPref.set("orderId", orderId);
            paymentPref.set("createdDate", UtilDateTime.nowTimestamp());
            paymentPref.set("createdByUserLogin", userLogin.getString("userLoginId"));
            delegator.create(paymentPref);
            result.put("disbursementOrderPaymentPreferenceId", orderPaymentPreferenceId);

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return result;
    }

    /**
     * Gets the user's current cash drawer for the currency, or creates a new cash drawer.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map getOrCreateCashDrawer(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String currencyUomId = (String) context.get("currencyUomId");
        String operatorUserLoginId = (String) context.get("operatorUserLoginId");

        Map result = ServiceUtil.returnSuccess();

        if (!security.hasEntityPermission("OPENTAPS", "_CSHDRWR", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        try {

            GenericValue operatorUserLogin = null;
            if (UtilValidate.isNotEmpty(operatorUserLoginId)) {
                operatorUserLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", operatorUserLoginId));
            } else {
                operatorUserLogin = userLogin;
            }
            if (UtilValidate.isEmpty(operatorUserLogin)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_UserLoginNotFound", UtilMisc.toMap("userLoginId", operatorUserLoginId), locale, MODULE);
            }

            // If the user has a currently open cash drawer, return it
            String cashDrawerId = UtilOrder.getCurrentCashDrawerId(operatorUserLogin, currencyUomId);

            if (UtilValidate.isEmpty(cashDrawerId)) {

                // If not, create one
                cashDrawerId = delegator.getNextSeqId("CashDrawer");
                GenericValue cashDrawer = delegator.makeValue("CashDrawer", UtilMisc.toMap("cashDrawerId", cashDrawerId));
                cashDrawer.setNonPKFields(context);
                cashDrawer.set("openUserLoginId", userLogin.get("userLoginId"));
                cashDrawer.set("operatorUserLoginId", operatorUserLogin.get("userLoginId"));
                if (UtilValidate.isEmpty(cashDrawer.get("openTimestamp"))) {
                    cashDrawer.set("openTimestamp", UtilDateTime.nowTimestamp());
                }
                if (UtilValidate.isEmpty(cashDrawer.get("initialAmount"))) {
                    cashDrawer.set("initialAmount", new Double(0));
                }
                delegator.create(cashDrawer);
            }

            result.put("cashDrawerId", cashDrawerId);

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "OpentapsError_CreateCashDrawerFail", locale, MODULE);
        }
        return result;
    }

    /**
     * Determines variance between calculated final value and user-entered final value, and closes drawer if no variance
     * or if forced.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map closeCashDrawer(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String cashDrawerId = (String) context.get("cashDrawerId");
        BigDecimal finalAmount = ((BigDecimal) context.get("finalAmount")).setScale(decimals, rounding);
        String closingComments = (String) context.get("closingComments");
        Boolean forceClose = (Boolean) context.get("forceClose");
        if (UtilValidate.isEmpty(forceClose)) {
            forceClose = new Boolean(false);
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("cashDrawerId", cashDrawerId);

        if (!security.hasEntityPermission("OPENTAPS", "_CSHDRWR", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        try {

            GenericValue cashDrawer = delegator.findByPrimaryKey("CashDrawer", UtilMisc.toMap("cashDrawerId", cashDrawerId));
            if (UtilValidate.isEmpty(cashDrawer)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_CashDrawerNotFound", UtilMisc.toMap("cashDrawerId", cashDrawerId), locale, MODULE);
            }

            BigDecimal netCash = UtilOrder.calculateCashDrawerBalance(cashDrawer);
            BigDecimal cashVariance = finalAmount.subtract(netCash).setScale(decimals, rounding);

            if (forceClose.booleanValue() || cashVariance.signum() == 0) {
                cashDrawer.set("closingComments", closingComments);
                cashDrawer.set("closeUserLoginId", userLogin.get("userLoginId"));
                cashDrawer.set("closeTimestamp", UtilDateTime.nowTimestamp());
                cashDrawer.set("finalAmount", new Double(finalAmount.doubleValue()));
                cashDrawer.set("closingVarianceAmount", new Double(cashVariance.doubleValue()));
                cashDrawer.set("forcedClose", forceClose.booleanValue() ? "Y" : "N");
                cashDrawer.store();
            } else {
                result.put("cashVariance", new Double(cashVariance.doubleValue()));
                UtilMessage.logServiceWarning("OpentapsError_CashDrawerVarianceExists", UtilMisc.toMap("cashDrawerId", cashDrawerId, "cashVariance", cashVariance.toString()), locale, MODULE);
                return result;
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "OpentapsError_CloseCashDrawerFail", locale, MODULE);
        }
        return result;
    }

    /**
     * Creates a CashDrawerTransaction record for a payment for the user's currently open CashDrawer.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map recordCashDrawerTransaction(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        Map result = ServiceUtil.returnSuccess();

        String paymentId = (String) context.get("paymentId");

        try {

            GenericValue payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            if (UtilValidate.isEmpty(payment)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PaymentNotFound", UtilMisc.toMap("paymentId", paymentId), locale, MODULE);
            }

            List paymentMethodTypeIds = UtilMisc.toList("CASH", "CERTIFIED_CHECK", "COMPANY_CHECK", "MONEY_ORDER", "PERSONAL_CHECK");
            List disbursementMethodTypeIds = UtilMisc.toList("CASH");

            List validTypeIds = null;
            if (UtilAccounting.isReceipt(payment)) {
                validTypeIds = paymentMethodTypeIds;
            }
            if (UtilAccounting.isDisbursement(payment)) {
                validTypeIds = disbursementMethodTypeIds;
            }
            if (UtilValidate.isEmpty(validTypeIds)) {
                UtilMessage.logServiceWarning("OpentapsError_CashDrawerTrans_skipInvalidPaymentType", UtilMisc.toMap("paymentId", paymentId, "paymentTypeId", payment.get("paymentTypeId")), locale, MODULE);
                return result;
            }
            if (!validTypeIds.contains(payment.getString("paymentMethodTypeId"))) {
                UtilMessage.logServiceWarning("OpentapsError_CashDrawerTrans_skipInvalidMethodType", UtilMisc.toMap("paymentId", paymentId, "paymentMethodTypeId", payment.get("paymentMethodTypeId")), locale, MODULE);
                return result;
            }
            if (UtilValidate.isEmpty(payment.getString("currencyUomId"))) {
                UtilMessage.logServiceWarning("OpentapsError_CashDrawerTrans_skipMissingPaymentCurrency", UtilMisc.toMap("paymentId", paymentId), locale, MODULE);
                return result;
            }

            String cashDrawerId = UtilOrder.getCurrentCashDrawerId(userLogin, payment.getString("currencyUomId"));
            if (UtilValidate.isEmpty(cashDrawerId)) {
                UtilMessage.logServiceInfo("OpentapsError_CashDrawerTrans_skipNoOpenDrawerForCurrency", UtilMisc.toMap("paymentId", paymentId, "userLoginId", userLogin.get("userLoginId"), "currencyUomId", payment.get("currencyUomId")), locale, MODULE);
                return result;
            }

            List existingCashDrawerTrans = delegator.findByAnd("CashDrawerTransaction", UtilMisc.toMap("cashDrawerId", cashDrawerId), UtilMisc.toList("cashDrawerItemSeqId DESC"));
            String cashDrawerItemSeqId = UtilFormatOut.formatPaddedNumber(1, 5);
            if (UtilValidate.isNotEmpty(existingCashDrawerTrans)) {
                long lastSeqId = Long.parseLong(EntityUtil.getFirst(existingCashDrawerTrans).getString("cashDrawerItemSeqId"));
                cashDrawerItemSeqId = UtilFormatOut.formatPaddedNumber(++lastSeqId, 5);
            }
            GenericValue cashDrawerTrans = delegator.makeValue("CashDrawerTransaction", UtilMisc.toMap("cashDrawerId", cashDrawerId, "cashDrawerItemSeqId", cashDrawerItemSeqId));
            cashDrawerTrans.set("paymentId", paymentId);
            delegator.create(cashDrawerTrans);

            result.put("cashDrawerId", cashDrawerId);
            result.put("cashDrawerItemSeqId", cashDrawerItemSeqId);

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "OpentapsError_CreateCashDrawerTransFail", locale, MODULE);
        }

        return result;
    }

    /**
     * Checks to see if an OrderItem added to an order should be marked as approved.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map approveAppendedOrderItems(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String orderId = (String) context.get("orderId");
        try {
            OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
            if ("ORDER_APPROVED".equals(orh.getOrderHeader().get("statusId"))) {
                List<GenericValue> prefs = orh.getPaymentPreferences();
                for (GenericValue pref : prefs) {
                    if ("CREDIT_CARD".equals(pref.get("paymentMethodTypeId"))) {
                        Debug.logInfo("OrderItem added to order [" + orderId + "].  The order has Credit Card payment methods, so not approving the item.", MODULE);
                    }
                }

                // let's update only the created items to avoid any future problems with the change status method
                List<GenericValue> items = orh.getOrderHeader().getRelatedByAnd("OrderItem", UtilMisc.toMap("statusId", "ITEM_CREATED"));
                for (GenericValue item : items) {
                    Map input = UtilMisc.toMap("userLogin", userLogin);
                    input.put("orderId", orderId);
                    input.put("orderItemSeqId", item.get("orderItemSeqId"));
                    input.put("statusId", "ITEM_APPROVED");
                    Map changeStatusResults = dispatcher.runSync("changeOrderItemStatus", input);
                    if (ServiceUtil.isError(changeStatusResults)) {
                        return changeStatusResults;
                    }
                }
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Add an item to an existing order without re-calculating the rest of the order.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map appendOrderItemBasic(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        BigDecimal unitPrice = (BigDecimal) context.get("unitPrice");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String surveyResponseId = (String) context.get("surveyResponseId");
        String comments = (String) context.get("comments");
        String description = (String) context.get("description");
        String correspondingPoId = (String) context.get("correspondingPoId");
        Map customFieldsMap = (Map) context.get("customFieldsMap");

        try {
            // verify the order exists and can be added to
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            if (UtilValidate.isEmpty(orderHeader)) {
                return UtilMessage.createAndLogServiceError("OrderNoOrderFound", locale, MODULE);
            }
            if ("ORDER_COMPLETED".equals(orderHeader.getString("statusId")) || "ORDER_REJECTED".equals(orderHeader.getString("statusId")) || "ORDER_CANCELLED".equals(orderHeader.getString("statusId"))) {
                return UtilMessage.createAndLogServiceError("OrderCannotBeChanged", locale, MODULE);
            }

            // validate the ship group
            GenericValue shipGroup = delegator.findByPrimaryKeyCache("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
            if (UtilValidate.isEmpty(shipGroup)) {
                return UtilMessage.createAndLogServiceError("OpentapsShipGroupNotFound", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId), locale, MODULE);
            }

            // validate the product
            GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            if (UtilValidate.isEmpty(product)) {
                return UtilMessage.createAndLogServiceError("ProductErrorProductNotFound", locale, MODULE);
            }
            if (UtilProduct.isDiscontinued(product)) {
                return UtilMessage.createAndLogServiceError("OpentapsProductIsDiscontinued", UtilMisc.toMap("productId", productId, "productName", product.get("internalName")), locale, MODULE);
            } else if (!UtilProduct.isIntroduced(product)) {
                return UtilMessage.createAndLogServiceError("OpentapsProductIsNotIntroduced", UtilMisc.toMap("productId", productId, "productName", product.get("internalName")), locale, MODULE);
            }

            // for the accounting tags
            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory dd = domainLoader.loadDomainsDirectory();
            OrderRepositoryInterface orderRepository = dd.getOrderDomain().getOrderRepository();
            Order order = orderRepository.getOrderById(orderId);

            // add the item
            OrderItem orderItem = new OrderItem();
            orderItem.initRepository(orderRepository);
            orderItem.setOrderId(orderId);
            orderItem.setOrderItemTypeId(UtilOrder.getOrderItemTypeId(product.getString("productTypeId"), orderHeader.getString("orderTypeId"), delegator));
            orderItem.setProductId(productId);
            orderItem.setQuantity(quantity);
            BigDecimal amount = (BigDecimal) context.get("amount");
            if (amount != null) {
                orderItem.setSelectedAmount(amount);
            }
            orderItem.setUnitPrice(unitPrice);
            BigDecimal listPrice = (BigDecimal) context.get("listPrice");
            if (listPrice != null) {
                orderItem.setUnitListPrice(listPrice);
            }
            orderItem.setIsModifiedPrice("N");   // I think this means the price has not been changed
            if (UtilValidate.isEmpty(description)) {
                orderItem.setItemDescription(ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", locale, dispatcher));
            } else {
                orderItem.setItemDescription(description);
            }
            orderItem.setStatusId("ITEM_CREATED");
            orderItem.setComments(comments);
            orderItem.setCorrespondingPoId(correspondingPoId);

            // set the tags
            UtilAccountingTags.putAllAccountingTags(context, orderItem, UtilAccountingTags.TAG_PARAM_PREFIX);

            // validate tags
            List<AccountingTagConfigurationForOrganizationAndUsage> missings = orderRepository.validateTagParameters(order, orderItem);
            if (!missings.isEmpty()) {
                for (AccountingTagConfigurationForOrganizationAndUsage missingTag : missings) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missingTag.getDescription()), locale, MODULE);
                }
            }

            // custom fields
            UtilCommon.setCustomFieldsFromServiceMap(orderItem, customFieldsMap, null, delegator);

            // persist the order item
            orderItem.setNextSubSeqId(OrderItem.Fields.orderItemSeqId.name(), ORDER_ITEM_PADDING, 1);
            orderRepository.createOrUpdate(orderItem);

            // record the status
            delegator.create("OrderStatus", UtilMisc.toMap("orderStatusId", delegator.getNextSeqId("OrderStatus"), "statusId", "ITEM_CREATED", "orderId", orderId, "orderItemSeqId", orderItem.getOrderItemSeqId(), "statusDatetime", UtilDateTime.nowTimestamp(), "statusUserLogin", userLogin.getString("userLoginId")));

            // if was given a surveyResponseId, associate it to te orderItem
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = delegator.findByPrimaryKey("SurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                surveyResponse.put("orderItemSeqId", orderItem.getOrderItemSeqId());
                surveyResponse.store();
            }

            Map tmpResult = null;
            // if the order is approved then we can
            if ("ORDER_APPROVED".equals(orderHeader.getString("statusId")) || "ORDER_HOLD".equals(orderHeader.getString("statusId"))) {
                tmpResult = dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.getOrderItemSeqId(), "statusId", "ITEM_APPROVED", "userLogin", userLogin));
            }

            // re-calculate taxes for this item
            Map serviceParams = UtilMisc.toMap("productStoreId", orh.getProductStoreId(),
                    "shippingAddress", orh.getShippingAddress(shipGroupSeqId),
                    "itemProductList", UtilMisc.toList(product),
                    "itemAmountList", UtilMisc.toList(quantity.multiply(unitPrice)),
                    "itemPriceList", UtilMisc.toList(unitPrice),
                    "userLogin", userLogin);
            if (orh.getBillFromParty() != null) {
                serviceParams.put("payToPartyId", orh.getBillFromParty().getString("partyId"));
            }
            if (orh.getBillToParty() != null) {
                serviceParams.put("billToPartyId", orh.getBillToParty().getString("partyId"));
            }
            serviceParams.put("itemShippingList", UtilMisc.toList(BigDecimal.ZERO));   // since we're not changing shipping, just pass a zero
            serviceParams.put("orderShippingAmount", orh.getShippingTotal()); // yes, I know, this will break on the current ofbiz svn, but maybe they'll fix it for us before we upgrade :)
            tmpResult = dispatcher.runSync("calcTax", serviceParams);
            if (ServiceUtil.isFailure(tmpResult)) {
                return tmpResult;
            }  // isError should automatically rollback this service too

            // store the taxes created
            List orderAdjustments = new ArrayList();
            List resOrderAdjustments = (List) tmpResult.get("orderAdjustments");
            if (resOrderAdjustments != null) {
                orderAdjustments.add(resOrderAdjustments);
                storeAdjustmentsForOrderItem(orderAdjustments, 0, orderId, "_NA_", "_NA_",  delegator);
            } else {
                Debug.logError("No orderAdjustments returned by the calcTax service.", MODULE);
            }

            List resItemAdjustments = (List) tmpResult.get("itemAdjustments");
            if (resItemAdjustments != null) {
                storeAdjustmentsForOrderItem(resItemAdjustments, 0, orderId, orderItem.getOrderItemSeqId(), shipGroupSeqId, delegator);
            } else {
                Debug.logError("No itemAdjustments returned by the calcTax service.", MODULE);
            }

            // do not call recalcTaxTotal here -- for some reason it was giving me the wrong results

            // add it to ship group
            delegator.create("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId, "orderItemSeqId", orderItem.getOrderItemSeqId(), "quantity", quantity));

            // recalc total
            tmpResult = dispatcher.runSync("resetGrandTotal", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
            if (ServiceUtil.isFailure(tmpResult)) { return tmpResult; }  // isError should automatically rollback this service too

            // reserve inventory for it
            if ("SALES_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                serviceParams = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.getOrderItemSeqId(), "productStoreId", orderHeader.getString("productStoreId"), "productId", orderItem.getProductId(), "shipGroupSeqId", shipGroupSeqId, "quantity", quantity);
                serviceParams.put("userLogin", userLogin);
                tmpResult = dispatcher.runSync("reserveStoreInventory", serviceParams);
                if (ServiceUtil.isFailure(tmpResult)) { return tmpResult; }  // isError should automatically rollback this service too
            }

            // make a note of it
            tmpResult = dispatcher.runSync("createOrderNote", UtilMisc.toMap("orderId", orderId, "internalNote", "Y", "note", UtilMessage.expandLabel("OpentapsOrderAddedItem", locale, UtilMisc.toMap("productId", productId, "shipGroupSeqId", shipGroupSeqId, "quantity", quantity)), "userLogin", userLogin));
            if (ServiceUtil.isFailure(tmpResult)) { return tmpResult; }  // isError should automatically rollback this service too

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        Map results = ServiceUtil.returnSuccess();
        results.put("orderId", orderId);
        return results;
    }

    /**
     * Update the quantities/prices for an existing order, wrapper for the ofbiz updateOrderItems but that can cancel the items when quantity is 0.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map updateApprovedOrderItems(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Map overridePriceMap = (Map) context.get("overridePriceMap");
        Map itemDescriptionMap = (Map) context.get("itemDescriptionMap");
        Map itemPriceMap = (Map) context.get("itemPriceMap");
        Map itemQtyMap = (Map) context.get("itemQtyMap");
        // for accounting tags
        Map[] tagsMaps = UtilAccountingTags.getTagMapParameters(context);

        List<String> cancelItems = new ArrayList<String>();
        Map<String, String> filteredItemDescriptionMap = new HashMap<String, String>();
        Map<String, String> filteredItemPriceMap = new HashMap<String, String>();
        Map<String, String> filteredItemQtyMap = new HashMap<String, String>();
        Map<String, String> filteredOverridePriceMap = new HashMap<String, String>();
        // as a map of {itemId: {tagId: tagValue}}
        Map<String, Map<String, String>> filteredTagsMap = new HashMap<String, Map<String, String>>();

        OrderRepositoryInterface orderRepository = null;
        Order order = null;

        try {
            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory dd = domainLoader.loadDomainsDirectory();
            orderRepository = dd.getOrderDomain().getOrderRepository();
            order = orderRepository.getOrderById(orderId);
        } catch (RepositoryException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // go through the item map and filter those which qty is set to 0, we will cancel them
        Iterator i = itemQtyMap.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            String quantityStr = (String) itemQtyMap.get(key);
            double groupQty = 0.0;
            try {
                groupQty = Double.parseDouble(quantityStr);
            } catch (NumberFormatException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (groupQty == 0) {
                cancelItems.add(key);
            } else {
                filteredItemQtyMap.put(key, quantityStr);
                String[] itemInfo = key.split(":");
                String itemId = itemInfo[0];
                if (itemPriceMap.get(itemId) != null) {
                    filteredItemPriceMap.put(itemId, (String) itemPriceMap.get(itemId));
                }
                if (itemDescriptionMap != null && itemDescriptionMap.get(itemId) != null) {
                    filteredItemDescriptionMap.put(itemId, (String) itemDescriptionMap.get(itemId));
                }
                if (overridePriceMap.get(itemId) != null) {
                    filteredOverridePriceMap.put(itemId, (String) overridePriceMap.get(itemId));
                }
                Map<String, String> tagMap = new HashMap<String, String>();
                for (int j = 1; j <= UtilAccountingTags.TAG_COUNT; j++) {
                    Map tags = tagsMaps[j - 1];
                    if (tags != null && tags.get(itemId) != null) {
                        String tagValue = (String) tags.get(itemId);
                        // avoid empty strings, they cause FK errors
                        if (UtilValidate.isEmpty(tagValue)) {
                            tagValue = null;
                        }
                        tagMap.put(UtilAccountingTags.ENTITY_TAG_PREFIX + j, tagValue);
                    }
                }
                filteredTagsMap.put(itemId, tagMap);
            }
        }

        Boolean filteredItemsOnlyPromoOrEmpty = true; // if only promo items remain, do not call updateOrderItems as it will cause a bug. They are cancelled automatically anyway.
        if (filteredItemQtyMap.size() > 0) {
            for (String key : filteredItemQtyMap.keySet()) {
                String[] itemInfo = key.split(":");
                String itemId = itemInfo[0];
                GenericValue orderItem = null;
                try {
                    orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", itemId));
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
                Debug.logInfo("updateOrderItems: found filtered item = " + orderItem, MODULE);
                if (!"Y".equals(orderItem.getString("isPromo"))) {
                    filteredItemsOnlyPromoOrEmpty = false;
                    break;
                }
            }
        }

        Map<String, Object> callCtx;

        // cancel the items set with 0 quantity if needed
        if (cancelItems.size() > 0) {
            if (!filteredItemsOnlyPromoOrEmpty) {
                for (String item : cancelItems) {
                    String[] itemInfo = item.split(":");
                    String orderItemSeqId = itemInfo[0];
                    String shipGroupSeqId = itemInfo[1];
                    callCtx = new HashMap<String, Object>();
                    callCtx.put("userLogin", userLogin);
                    callCtx.put("orderId", orderId);
                    callCtx.put("orderItemSeqId", orderItemSeqId);
                    callCtx.put("shipGroupSeqId", shipGroupSeqId);
                    try {
                        dispatcher.runSync("cancelOrderItem", callCtx);
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            } else {
                // cancel the whole order
                callCtx = new HashMap<String, Object>();
                callCtx.put("userLogin", userLogin);
                callCtx.put("orderId", orderId);
                try {
                    dispatcher.runSync("cancelOrderItem", callCtx);
                } catch (GenericServiceException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }

        Map results;
        if (!filteredItemsOnlyPromoOrEmpty) {
            String serviceName = "updateOrderItems";
            if (order.isPurchaseOrder()) {
                serviceName = "updatePurchaseOrderItems";
            }
            // calling the ofbiz updateApprovedOrderItems service if needed
            callCtx = new HashMap<String, Object>();
            callCtx.put("userLogin", userLogin);
            callCtx.put("orderId", orderId);
            callCtx.put("recalcAdjustments", context.get("recalcAdjustments"));
            callCtx.put("forceComplete", context.get("forceComplete"));
            callCtx.put("overridePriceMap", filteredOverridePriceMap);
            callCtx.put("itemDescriptionMap", filteredItemDescriptionMap);
            callCtx.put("itemPriceMap", filteredItemPriceMap);
            callCtx.put("itemQtyMap", filteredItemQtyMap);
            try {
                results = dispatcher.runSync(serviceName, callCtx);
            } catch (GenericServiceException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(results) || ServiceUtil.isFailure(results)) {
                return results;
            }

            // check if all the order items are now completed
            try {
            	i = filteredItemQtyMap.keySet().iterator();
                while (i.hasNext()) {
                    // the filteredItemQtyMap key is orderItemSeqId:shipGroupSeqId or 00001:00001
                    String key = (String) i.next();
                    String[] itemInfo = key.split(":");
                    String orderItemSeqId = itemInfo[0];
                    OrderItem item = orderRepository.getOrderItem(order, orderItemSeqId);
                    Debug.logVerbose("item [" + item.getOrderId() + "/" + item.getOrderItemSeqId() + "] remaining to ship [" + item.getRemainingToShipQuantity() + "]", MODULE);
                    if (item.getRemainingToShipQuantity().signum() == 0) {
                    	Map tmpResults = dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "fromStatusId", item.getStatusId(), "statusId", "ITEM_COMPLETED", "userLogin", userLogin), -1, false);
                    	if (ServiceUtil.isError(tmpResults) || ServiceUtil.isFailure(tmpResults)) {
                            return tmpResults;
                        }
                    } 
                    
                }
                
            } catch (Exception e) {
            	return ServiceUtil.returnError(e.getMessage());
            }
            
            // manually updates the accounting tags since the ofbiz service does not support them
            try {
                for (String itemId : filteredTagsMap.keySet()) {
                    // get the order item
                    OrderItem orderItem = orderRepository.getOrderItem(order, itemId);
                    // validate the tags, only for non promo items, as promo items cannot have tags
                    if (!orderItem.isPromo()) {
                        // set the tags
                        UtilAccountingTags.putAllAccountingTags(filteredTagsMap.get(itemId), orderItem);
                        // validate
                        List<AccountingTagConfigurationForOrganizationAndUsage> missings = orderRepository.validateTagParameters(order, orderItem);
                        if (!missings.isEmpty()) {
                            for (AccountingTagConfigurationForOrganizationAndUsage missingTag : missings) {
                                return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missingTag.getDescription()), locale, MODULE);
                            }
                        }
                        // store
                        orderRepository.update(orderItem);
                    }
                }
            } catch (GeneralException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            results = ServiceUtil.returnSuccess();
            results.put("orderId", orderId);
        }
        return results;
    }

    /**
     * Update the quantities/prices for an existing order, this is copied from the ofbiz service.
     * Note: the only difference is that the cancel quantity is no longer added to the cart item quantity,
     *  as this is now handled by the OpentapsShoppingCart, loadCartFromOrder will store the cancel quantity
     *  and the OpentapsShoppingCart makeOrderItems will reset the value.
     *  This is for a workaround of a bug involving promotion calculation.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> updateApprovedOrderItemsLegacy(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String orderId = (String) context.get("orderId");
        Map<String, String> overridePriceMap = (Map<String, String>) context.get("overridePriceMap");
        Map<String, String> itemDescriptionMap = (Map<String, String>) context.get("itemDescriptionMap");
        Map<String, String> itemPriceMap = (Map<String, String>) context.get("itemPriceMap");
        Map<String, String> itemQtyMap = (Map<String, String>) context.get("itemQtyMap");

        Map<String, String> itemReasonMap = (Map<String, String>) context.get("itemReasonMap");
        Map<String, String> itemCommentMap = (Map<String, String>) context.get("itemCommentMap");
        Map<String, String> itemAttributesMap = (Map<String, String>) context.get("itemAttributesMap");
        Map<String, String> itemEstimatedShipDateMap  = (Map<String, String>) context.get("itemShipDateMap");
        Map<String, String> itemEstimatedDeliveryDateMap = (Map<String, String>) context.get("itemDeliveryDateMap");

        // default to True which is the default behavior in ofbiz
        boolean recalcAdjustments = !"N".equals(context.get("recalcAdjustments"));
        boolean forceComplete = "Y".equals(context.get("forceComplete"));

        // those new inputs above could be null
        if (itemReasonMap == null) {
            itemReasonMap = new HashMap<String, String>();
        }
        if (itemCommentMap == null) {
            itemCommentMap = new HashMap<String, String>();
        }
        if (itemAttributesMap == null) {
            itemAttributesMap = new HashMap<String, String>();
        }
        if (itemEstimatedShipDateMap == null) {
            itemEstimatedShipDateMap = new HashMap<String, String>();
        }
        if (itemEstimatedDeliveryDateMap == null) {
            itemEstimatedDeliveryDateMap = new HashMap<String, String>();
        }


        // obtain a shopping cart object for updating
        OpentapsShoppingCart cart = null;
        try {
            cart = loadCartForUpdate(dispatcher, delegator, userLogin, orderId);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        if (cart == null) {
            return ServiceUtil.returnError("ERROR: Null shopping cart object returned!");
        }

        // go through the item attributes map once to get a list of key names
        Set<String> attributeNames = FastSet.newInstance();
        Set<String> keys = itemAttributesMap.keySet();
        for (String key : keys) {
            String[] attributeInfo = key.split(":");
            attributeNames.add(attributeInfo[0]);
        }

        // go through the item map and obtain the totals per item
        Map<String, BigDecimal> itemTotals = new HashMap<String, BigDecimal>();
        Map tmp = getItemTotalsFromItemQtyMap(itemTotals, itemQtyMap);
        if (tmp != null) {
            return tmp;
        }

        // set the items quantity/price/description
        for (String itemSeqId : itemTotals.keySet()) {
            ShoppingCartItem cartItem = cart.findCartItem(itemSeqId);

            if (cartItem != null) {
                BigDecimal qty = itemTotals.get(itemSeqId);
                BigDecimal priceSave = cartItem.getBasePrice();

                // set quantity
                try {
                    cartItem.setQuantity(qty, dispatcher, cart, false, false); // do not trigger external ops (promotions), don't reset ship groups (and update prices for both PO and SO items)
                } catch (CartItemModifyException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
                Debug.logVerbose("Set item quantity: [" + itemSeqId + "] " + qty, MODULE);

                if (cartItem.getIsModifiedPrice()) {
                    // set price
                    cartItem.setBasePrice(priceSave);
                    Debug.logVerbose("Reset item base price as it was modified: [" + itemSeqId + "] " + priceSave, MODULE);
                }

                if (overridePriceMap.containsKey(itemSeqId)) {
                    String priceStr = itemPriceMap.get(itemSeqId);
                    if (UtilValidate.isNotEmpty(priceStr)) {
                        BigDecimal price = BigDecimal.ZERO;
                        //parse the price
                        NumberFormat nf = null;
                        if (locale != null) {
                            nf = NumberFormat.getNumberInstance(locale);
                        } else {
                            nf = NumberFormat.getNumberInstance();
                        }
                        try {
                            price = BigDecimal.valueOf(nf.parse(priceStr).doubleValue());
                        } catch (ParseException e) {
                            return UtilMessage.createAndLogServiceError(e, MODULE);
                        }
                        cartItem.setBasePrice(price);
                        cartItem.setIsModifiedPrice(true);
                        Debug.logVerbose("Set item price: [" + itemSeqId + "] " + price, MODULE);
                    }
                }

                // Update the item description
                if (itemDescriptionMap != null && itemDescriptionMap.containsKey(itemSeqId)) {
                    String description = itemDescriptionMap.get(itemSeqId);
                    if (UtilValidate.isNotEmpty(description)) {
                        cartItem.setName(description);
                        Debug.log("Set item description: [" + itemSeqId + "] " + description, MODULE);
                    } else {
                        return UtilMessage.createAndLogServiceError("Item description must not be empty", MODULE);
                    }
                }

                // update the order item attributes
                if (itemAttributesMap != null) {
                    String attrValue = null;
                    for (String attrName : attributeNames) {
                        attrValue = itemAttributesMap.get(attrName + ":" + itemSeqId);
                        if (UtilValidate.isNotEmpty(attrName)) {
                            cartItem.setOrderItemAttribute(attrName, attrValue);
                            Debug.log("Set item attribute Name: [" + itemSeqId + "] " + attrName + " , Value:" + attrValue, MODULE);
                        }
                    }
                }

            } else {
                Debug.logInfo("Unable to locate shopping cart item for seqId #" + itemSeqId, MODULE);
            }
        }

        // Create Estimated Delivery dates
        for (Map.Entry<String, String> entry : itemEstimatedDeliveryDateMap.entrySet()) {
            String itemSeqId =  entry.getKey();
            String estimatedDeliveryDate = entry.getValue();
            if (UtilValidate.isNotEmpty(estimatedDeliveryDate)) {
                Timestamp deliveryDate = Timestamp.valueOf(estimatedDeliveryDate);
                ShoppingCartItem cartItem = cart.findCartItem(itemSeqId);
                cartItem.setDesiredDeliveryDate(deliveryDate);
            }
        }

        // Create Estimated ship dates
        for (Map.Entry<String, String> entry : itemEstimatedShipDateMap.entrySet()) {
            String itemSeqId =  entry.getKey();
            String estimatedShipDate = entry.getValue();
            if (UtilValidate.isNotEmpty(estimatedShipDate)) {
                Timestamp shipDate = Timestamp.valueOf(estimatedShipDate);
                ShoppingCartItem cartItem = cart.findCartItem(itemSeqId);
                cartItem.setEstimatedShipDate(shipDate);
            }

        }

        // update the group amounts
        Iterator gai = itemQtyMap.keySet().iterator();
        while (gai.hasNext()) {
            String key = (String) gai.next();
            String quantityStr = itemQtyMap.get(key);
            BigDecimal groupQty = BigDecimal.ZERO;
            try {
                groupQty = new BigDecimal(quantityStr);
            } catch (NumberFormatException e) {
                return UtilMessage.createAndLogServiceError(e, MODULE);
            }

            String[] itemInfo = key.split(":");
            int groupIdx = -1;
            try {
                groupIdx = Integer.parseInt(itemInfo[1]);
            } catch (NumberFormatException e) {
                return UtilMessage.createAndLogServiceError(e, MODULE);
            }

            // set the group qty
            ShoppingCartItem cartItem = cart.findCartItem(itemInfo[0]);
            if (cartItem != null) {
                cart.setItemShipGroupQty(cartItem, groupQty, groupIdx - 1);
            }
        }

        // run promotions to handle all changes in the cart
        ProductPromoWorker.doPromotions(cart, dispatcher);
        // save all the updated information
        try {
            boolean res = saveUpdatedCartToOrder(dispatcher, delegator, cart, locale, userLogin, orderId, UtilMisc.toMap("itemReasonMap", itemReasonMap, "itemCommentMap", itemCommentMap), forceComplete, recalcAdjustments);
            if (!res) {
                return UtilMessage.createAndLogServiceError("The order has adjustments that are already billed, please specify whether or not to recalculate the remaining adjustments.", MODULE);
            }

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        // log an order note
        try {
            dispatcher.runSync("createOrderNote", UtilMisc.toMap("orderId", orderId, "note", "Updated order.", "internalNote", "Y", "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        result.put("orderId", orderId);
        return result;
    }

    /**
     * Set the IDs for the adjustments returned by ofbiz calcTax service and store them
     * Note that OFBIZ calcTax returns a List of List of OrderAdjustments.  The top level list corresponds to the List of products passed into calcTax.
     * This method is designed to match an index for that top level list to a particular orderItemSeqId
     * @param topLevelAdjustmentsList
     * @param index an <code>int</code> value
     * @param orderId
     * @param orderItemSeqId
     * @param shipGroupSeqId a <code>String</code> value
     * @param delegator
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static void storeAdjustmentsForOrderItem(List<List> topLevelAdjustmentsList, int index, String orderId, String orderItemSeqId, String shipGroupSeqId, Delegator delegator) throws GenericEntityException {
        if (UtilValidate.isNotEmpty(topLevelAdjustmentsList)) {
            List<GenericValue> adjustments = topLevelAdjustmentsList.get(index);
            for (GenericValue adjustment : adjustments) {
                adjustment.set("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                adjustment.set("orderId", orderId);
                adjustment.set("orderItemSeqId", orderItemSeqId);
                adjustment.set("shipGroupSeqId", shipGroupSeqId);
                adjustment.create();
            }
        }
    }

    /**
     * Does a comprehensive product lookup for given productId and appends to the order if found.
     * Otherwise returns a service error.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map appendOrderItem(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productId = (String) context.get("productId");
        String recalcOrder = (String) context.get("recalcOrder");
        BigDecimal basePrice = (BigDecimal) context.get("basePrice");

        try {
            // get the productId, which could be
            Map input = UtilMisc.toMap("productId", productId);
            Map results = dispatcher.runSync("getProductByComprehensiveSearch", input);
            if (ServiceUtil.isError(results) || ServiceUtil.isFailure(results)) {
                return results;
            }
            productId = (String) results.get("productId");
            if (productId == null) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ProductNotFound", input, locale, MODULE);
            }
            context.put("productId", productId);

            if (recalcOrder != null) {
                // user requested recalc order - run the ofbiz service which does it
                ModelService service = dctx.getModelService("appendOrderItem");
                Map serviceContext = FastMap.newInstance();
                serviceContext.putAll(context);
                serviceContext.put("productId", productId);
                input = service.makeValid(serviceContext, "IN");
                results = dispatcher.runSync(service.name, input);
                if (ServiceUtil.isError(results) || ServiceUtil.isFailure(results)) {
                    return results;
                }

                Map returnResults = ServiceUtil.returnSuccess();
                returnResults.put("orderId", results.get("orderId"));
                returnResults.put("shoppingCart", results.get("shoppingCart"));
                return returnResults;
            } else {
                // add item without modifying it

                String orderId = (String) context.get("orderId");
                String prodCatalogId = (String) context.get("prodCatalogId");

                ModelService service = dctx.getModelService("opentaps.appendOrderItemBasic");
                Map appendItemParams = service.makeValid(context, "IN");

                // calculate the product price.  This is here so we'll always have the list price
                GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
                // validate that the product can be added, e.g.: virtual products cannot be added (or the update order will not work)
                if ("Y".equals(product.get("isVirtual"))) {
                    return UtilMessage.createAndLogServiceError("item.cannot_add_product_virtual", UtilMisc.toMap("productName", product.get("internalName"), "productId", productId), locale, MODULE);
                }

                OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
                input = UtilMisc.toMap("product", product, "prodCatalogId", prodCatalogId, "productStoreId", orh.getProductStoreId(), "quantity", context.get("quantity"),
                        "currencyUomId", orh.getCurrency(), "userLogin", userLogin);
                if (orh.getBillToParty() != null) {
                    input.put("partyId", orh.getBillToParty().getString("partyId"));
                }
                results = dispatcher.runSync("calculateProductPrice", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }

                // list price is always from calculateProductPrice
                if (results.get("listPrice") != null) {
                    appendItemParams.put("listPrice", results.get("listPrice"));
                }

                // calculate price
                String overridePrice = (String) context.get("overridePrice");
                if (overridePrice != null) {
                    // overriding the price
                    appendItemParams.put("unitPrice", basePrice);
                    // list price will null -- only basePrice is input
                } else {
                    if (basePrice != null) {
                        Debug.logWarning("Override price was NOT selected. Input price of [" + basePrice + "] will be ignored, using the result of calculateProductPrice [" + results.get("price") + "] instead", MODULE);
                    }
                    appendItemParams.put("unitPrice", results.get("price"));
                    appendItemParams.put("isSalePrice", results.get("isSale"));
                }

                results = dispatcher.runSync(service.name, appendItemParams);
                if (ServiceUtil.isError(results) || ServiceUtil.isFailure(results)) {
                    return results;
                }

                Map returnResults = ServiceUtil.returnSuccess();
                returnResults.put("orderId", results.get("orderId"));
                return returnResults;

            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
    }

    /**
     * From ofbiz service appendOrderItem.
     * Added accounting tag support.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map addItemToApprovedOrder(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String orderId = (String) context.get("orderId");
        String productId = (String) context.get("productId");
        String prodCatalogId = (String) context.get("prodCatalogId");
        BigDecimal basePrice = (BigDecimal) context.get("basePrice");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        BigDecimal amount = (BigDecimal) context.get("amount");
        String overridePrice = (String) context.get("overridePrice");
        String comments = (String) context.get("comments");
        String description = (String) context.get("description");
        Map customFieldsMap = (Map) context.get("customFieldsMap");

        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        int shipGroupIdx = -1;
        try {
            shipGroupIdx = Integer.parseInt(shipGroupSeqId);
            shipGroupIdx--;
        } catch (NumberFormatException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        if (shipGroupIdx < 0) {
            return UtilMessage.createAndLogServiceError("Invalid shipGroupSeqId [" + shipGroupSeqId + "]", MODULE);
        }

        // obtain a shopping cart object for updating
        OpentapsShoppingCart cart = null;
        try {
            cart = loadCartForUpdate(dispatcher, delegator, userLogin, orderId);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        if (cart == null) {
            return UtilMessage.createAndLogServiceError("ERROR: Null shopping cart object returned!", MODULE);
        }

        // add in the new product
        try {
            ShoppingCartItem item = ShoppingCartItem.makeItem(null, productId, null, quantity, null, null, null, null, null, null, null, null, prodCatalogId, null, null, null, dispatcher, cart, null, null, null, Boolean.FALSE, Boolean.FALSE);
            if (basePrice != null && overridePrice != null) {
                item.setBasePrice(basePrice);
                // special hack to make sure we re-calc the promos after a price change
                item.setQuantity(quantity.add(BigDecimal.ONE), dispatcher, cart, false);
                item.setQuantity(quantity, dispatcher, cart, false);
                item.setBasePrice(basePrice);
                item.setIsModifiedPrice(true);
            }
            if (UtilValidate.isNotEmpty(comments)) {
                item.setItemComment(comments);
            }
            if (UtilValidate.isNotEmpty(description)) {
                item.setName(description);
            }

            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory dd = domainLoader.loadDomainsDirectory();
            OrderRepositoryInterface orderRepository = dd.getOrderDomain().getOrderRepository();

            // add accounting tags
            Map tags = UtilAccountingTags.getTagParameters(context);
            // set the tags to the cart item
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                item.setAttribute(UtilAccountingTags.TAG_PARAM_PREFIX + i, tags.get(UtilAccountingTags.TAG_PARAM_PREFIX + i));
            }
            // validate tags
            List<AccountingTagConfigurationForOrganizationAndUsage> missings = orderRepository.validateTagParameters(cart, item);
            if (!missings.isEmpty()) {
                for (AccountingTagConfigurationForOrganizationAndUsage missingTag : missings) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missingTag.getDescription()), locale, MODULE);
                }
            }

            // customized fields
            try {
                Map<String, Object> customFields = UtilCommon.getCustomFieldsFromServiceMap(delegator.getModelEntity("OrderItem"), customFieldsMap, null, delegator);
                for (String k : customFields.keySet()) {
                    item.setAttribute(k, customFields.get(k));
                }
            } catch (IllegalArgumentException e) {
                return UtilMessage.createAndLogServiceError(e, locale, MODULE);
            }

            // set the item in the selected ship group
            cart.setItemShipGroupQty(item, item.getQuantity(), shipGroupIdx);
        } catch (CartItemModifyException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (ItemNotFoundException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        // save all the updated information
        try {
           saveUpdatedCartToOrder(dispatcher, delegator, cart, locale, userLogin, orderId);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        // log an order note
        try {
            dispatcher.runSync("createOrderNote", UtilMisc.toMap("orderId", orderId, "note", "Added item to order: " + productId + " (" + quantity + ")", "internalNote", "Y", "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        result.put("orderId", orderId);
        return result;
    }

    /**
     * Set the quantity on all order item billing records of the invoice to zero when invoice is canceled or voided.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> cancelOrderItemBilling(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String invoiceId = (String) context.get("invoiceId");

        try {

            delegator.storeByCondition("OrderItemBilling", UtilMisc.toMap("quantity", BigDecimal.ZERO), EntityCondition.makeCondition("invoiceId", invoiceId));

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service designed to run after updatePostalAddress.  Its purpose is to copy any ProductStoreFacilityByAddress entries for the updated address.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> updatePostalAddressForProductStoreFacilityByAddress(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String oldContactMechId = (String) context.get("oldContactMechId");
        String contactMechId = (String) context.get("contactMechId");

        // don't do anything if the old address wasn't turned into a new address
        if (oldContactMechId == null || contactMechId.equals(oldContactMechId)) {
            return ServiceUtil.returnSuccess();
        }

        try {
            // copy each of the ProductStoreFacilityByAddress from the old contact mech to the new
            List<GenericValue> facilityAddresses = delegator.findByAnd("ProductStoreFacilityByAddress", UtilMisc.toMap("contactMechId", oldContactMechId));
            for (GenericValue facilityAddress : facilityAddresses) {
                GenericValue newFacilityAddress = delegator.makeValue("ProductStoreFacilityByAddress", facilityAddress);
                newFacilityAddress.put("contactMechId", contactMechId);
                newFacilityAddress.create();
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /*
     *  Warning: this method will remove all the existing reservations of the order
     *           before returning the ShoppingCart object; for this reason, the cart
     *           must be stored back using the method saveUpdatedCartToOrder(...).
     * Note: no special changes from the ofbiz implementation, just copied for use in the addItemToApprovedOrder service
     *  accounting tags are loaded from the loadCartFromOrder service
     */
    @SuppressWarnings("unchecked")
    public static OpentapsShoppingCart loadCartForUpdate(LocalDispatcher dispatcher, Delegator delegator, GenericValue userLogin, String orderId) throws GeneralException {
        // find ship group associations
        List<GenericValue> shipGroupAssocs = null;
        try {
            shipGroupAssocs = delegator.findByAnd("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new GeneralException(e.getMessage());
        }

        // cancel existing inventory reservations
        if (shipGroupAssocs != null) {
            for (GenericValue shipGroupAssoc : shipGroupAssocs) {
                String orderItemSeqId = shipGroupAssoc.getString("orderItemSeqId");
                String shipGroupSeqId = shipGroupAssoc.getString("shipGroupSeqId");

                Map cancelCtx = UtilMisc.toMap("userLogin", userLogin, "orderId", orderId);
                cancelCtx.put("orderItemSeqId", orderItemSeqId);
                cancelCtx.put("shipGroupSeqId", shipGroupSeqId);

                Map cancelResp = null;
                try {
                    cancelResp = dispatcher.runSync("cancelOrderInventoryReservation", cancelCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    throw new GeneralException(e.getMessage());
                }
                if (ServiceUtil.isError(cancelResp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(cancelResp));
                }
            }
        }

        // load the order into a shopping cart
        Map loadCartResp = null;
        try {
            loadCartResp = dispatcher.runSync("loadCartFromOrder", UtilMisc.toMap("orderId", orderId, "skipInventoryChecks", Boolean.TRUE, "skipProductChecks", Boolean.TRUE, "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            throw new GeneralException(e.getMessage());
        }
        if (ServiceUtil.isError(loadCartResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(loadCartResp));
        }

        ShoppingCart oldCart = (ShoppingCart) loadCartResp.get("shoppingCart");

        if (oldCart == null) {
            throw new GeneralException("Error loading shopping cart from order [" + orderId + "]");
        }

        OpentapsShoppingCart cart;
        if (oldCart instanceof OpentapsShoppingCart) {
            cart = (OpentapsShoppingCart) oldCart;
        } else {
            cart = new OpentapsShoppingCart(oldCart);
        }

        // check if the order is COD
        boolean isCod = false;
        try {
            List<GenericValue> codPaymentPrefs = delegator.findByAnd("OrderPaymentPreference",
                                                                     UtilMisc.toList(EntityCondition.makeCondition("orderId", orderId),
                                                                                     EntityCondition.makeCondition("paymentMethodTypeId", "EXT_COD"),
                                                                                     EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED")));

            isCod = UtilValidate.isNotEmpty(codPaymentPrefs);
        } catch (GeneralException e) {
            Debug.logError(e, "A problem occurred while getting the order payment preferences, assuming NOT COD..", MODULE);
        }

        int shipGroups = cart.getShipGroupSize();
        for (int gi = 0; gi < shipGroups; gi++) {
            cart.setCOD(gi, isCod);
        }

        cart.setOrderId(orderId);
        return cart;
    }

    /*
     * Added accounting tags support.
     * Return false if the change need to recalculate the adjustments but some are already billed, and forceComplete was not set
     */
    public static boolean saveUpdatedCartToOrder(LocalDispatcher dispatcher, Delegator delegator, OpentapsShoppingCart cart, Locale locale, GenericValue userLogin, String orderId) throws GeneralException {
        return saveUpdatedCartToOrder(dispatcher, delegator, cart, locale, userLogin, orderId, null, false, true);
    }

    /*
     * Added accounting tags support.
     * (TODO: changeMap unused)
     */
    @SuppressWarnings("unchecked")
    public static boolean saveUpdatedCartToOrder(LocalDispatcher dispatcher, Delegator delegator, OpentapsShoppingCart cart, Locale locale, GenericValue userLogin, String orderId, Map changeMap, boolean forceComplete, boolean recalcAdjustments) throws GeneralException {

        // find out early if there are billed adjustment, this would need forceComplete to be set or fail
        boolean hasBilledAdjustments = false;
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
        DomainsDirectory dd = domainLoader.loadDomainsDirectory();
        Session session = dd.getInfrastructure().getSession();
        String hql = "from OrderAdjustmentBilling eo where eo.orderAdjustment.orderId = :orderId ";
        Query query = session.createQuery(hql);
        query.setString("orderId", orderId);
        List<OrderAdjustmentBilling> orderAdjustmentBillingList = query.list();

        // adjustments to remove or offset
        if (orderAdjustmentBillingList.size() > 0) {
            // if order adjustment billings already exist for the order adjustments, do not make any changes to the order adjustments .
            // return false if forceComplete is not set
            hasBilledAdjustments = true;
            if (!forceComplete) {
                session.close();
                return false;
            }
        }
        session.close();

        // get/set the shipping estimates.  if it's a SALES ORDER, then return an error if there are no ship estimates
        int shipGroups = cart.getShipGroupSize();
        for (int gi = 0; gi < shipGroups; gi++) {
            String shipmentMethodTypeId = cart.getShipmentMethodTypeId(gi);
            String carrierPartyId = cart.getCarrierPartyId(gi);
            Debug.log("Getting ship estimate for group #" + gi + " [" + shipmentMethodTypeId + " / " + carrierPartyId + "]", MODULE);
            Map result = OrderEvents.getShipGroupEstimate(dispatcher, delegator, cart, gi);
            if (("SALES_ORDER".equals(cart.getOrderType())) && (ServiceUtil.isError(result))) {
                Debug.logError(ServiceUtil.getErrorMessage(result), MODULE);
                throw new GeneralException(ServiceUtil.getErrorMessage(result));
            }

            BigDecimal shippingTotal = (BigDecimal) result.get("shippingTotal");
            if (shippingTotal == null) {
                shippingTotal = BigDecimal.ZERO;
            }
            Debug.logInfo("Setting ship estimate for group #" + gi + " [" + shipmentMethodTypeId + " / " + carrierPartyId + "] isCod (" + cart.getCOD(gi) + ") = " + shippingTotal, MODULE);
            cart.setItemShipGroupEstimate(shippingTotal, gi);
        }

        // calc the sales tax
        CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
        try {
            coh.calcAndAddTax();
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            throw new GeneralException(e.getMessage());
        }

        // validate the payment methods
        Map validateResp = coh.validatePaymentMethods();
        if (ServiceUtil.isError(validateResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(validateResp));
        }

        // get the new orderItems, adjustments, shipping info and payments from the cart
        List toStore = new LinkedList();
        List orderItems = cart.makeOrderItems();
        toStore.addAll(orderItems);
        if (recalcAdjustments) {
            toStore.addAll(cart.makeAllAdjustments());
        }
        toStore.addAll(cart.makeAllShipGroupInfos());
        toStore.addAll(cart.makeAllOrderPaymentInfos(dispatcher));

        // set the orderId & other information on all new value objects
        List dropShipGroupIds = FastList.newInstance(); // this list will contain the ids of all the ship groups for drop shipments (no reservations)
        Iterator tsi = toStore.iterator();
        while (tsi.hasNext()) {
            GenericValue valueObj = (GenericValue) tsi.next();
            valueObj.set("orderId", orderId);
            if ("OrderItemShipGroup".equals(valueObj.getEntityName())) {
                // ship group
                if (valueObj.get("carrierRoleTypeId") == null) {
                    valueObj.set("carrierRoleTypeId", "CARRIER");
                }
                if (!UtilValidate.isEmpty(valueObj.get("supplierPartyId"))) {
                    dropShipGroupIds.add(valueObj.getString("shipGroupSeqId"));
                }
            } else if ("OrderAdjustment".equals(valueObj.getEntityName())) {
                if (recalcAdjustments) {
                    // shipping / tax adjustment(s)
                    if (valueObj.get("orderItemSeqId") == null || valueObj.getString("orderItemSeqId").length() == 0) {
                        valueObj.set("orderItemSeqId", DataModelConstants.SEQ_ID_NA);
                    }
                    valueObj.set("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                    valueObj.set("createdDate", UtilDateTime.nowTimestamp());
                    valueObj.set("createdByUserLogin", userLogin.getString("userLoginId"));
                } else {
                    tsi.remove();
                }
            } else if ("OrderPaymentPreference".equals(valueObj.getEntityName())) {
                if (valueObj.get("orderPaymentPreferenceId") == null) {
                    valueObj.set("orderPaymentPreferenceId", delegator.getNextSeqId("OrderPaymentPreference"));
                    valueObj.set("createdDate", UtilDateTime.nowTimestamp());
                    valueObj.set("createdByUserLogin", userLogin.getString("userLoginId"));
                }
                if (valueObj.get("statusId") == null) {
                    valueObj.set("statusId", "PAYMENT_NOT_RECEIVED");
                }
            } else if ("OrderItemShipGroupAssoc".equals(valueObj.getEntityName())) {
                // the quantity in the assoc was updated to the actual quantity
                // check existing value
                Debug.logInfo("saveUpdatedCartToOrder: check OrderItemShipGroupAssoc: " + valueObj, MODULE);
                GenericValue assoc = null;
                try {
                    assoc = delegator.findByPrimaryKey("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", valueObj.get("orderId"), "orderItemSeqId", valueObj.get("orderItemSeqId"), "shipGroupSeqId", valueObj.get("shipGroupSeqId")));
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }

                if (assoc != null) {
                    BigDecimal cancelQty = assoc.getBigDecimal("cancelQuantity");
                    Debug.logInfo("found existing assoc: " + assoc, MODULE);
                    if (cancelQty != null) {
                        BigDecimal newQuantity = valueObj.getBigDecimal("quantity");
                        newQuantity = newQuantity.add(cancelQty);
                        Debug.logInfo("set new quantity = " + newQuantity, MODULE);
                        valueObj.set("quantity", newQuantity);
                        valueObj.set("cancelQuantity", cancelQty);
                    }
                }
            }
        }
        Debug.log("To Store Contains: " + toStore, MODULE);

        // cancel promo items -- if the promo still qualifies it will be added by the cart
        List promoItems = null;
        try {
            promoItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId, "isPromo", "Y"));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new GeneralException(e.getMessage());
        }
        if (promoItems != null) {
            Iterator pii = promoItems.iterator();
            while (pii.hasNext()) {
                GenericValue promoItem = (GenericValue) pii.next();
                // Skip if the promo is already cancelled
                if ("ITEM_CANCELLED".equals(promoItem.get("statusId"))) {
                    continue;
                }
                Map cancelPromoCtx = UtilMisc.toMap("orderId", orderId);
                cancelPromoCtx.put("orderItemSeqId", promoItem.getString("orderItemSeqId"));
                cancelPromoCtx.put("userLogin", userLogin);
                Map cancelResp = null;
                try {
                    cancelResp = dispatcher.runSync("cancelOrderItemNoActions", cancelPromoCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    throw new GeneralException(e.getMessage());
                }
                if (ServiceUtil.isError(cancelResp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(cancelResp));
                }
            }
        }

        // cancel exiting authorizations
        Map releaseResp = null;
        try {
            releaseResp = dispatcher.runSync("releaseOrderPayments", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            throw new GeneralException(e.getMessage());
        }
        if (ServiceUtil.isError(releaseResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(releaseResp));
        }

        // cancel other (non-completed and non-cancelled) payments
        List paymentPrefsToCancel = null;
        try {
            EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                 EntityCondition.makeCondition("orderId", orderId),
                 EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_RECEIVED"),
                 EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"),
                 EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_DECLINED"),
                 EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_SETTLED"),
                 EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_REFUNDED"));
            paymentPrefsToCancel = delegator.findByCondition("OrderPaymentPreference", cond, null, null);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new GeneralException(e.getMessage());
        }
        if (paymentPrefsToCancel != null) {
            Iterator oppi = paymentPrefsToCancel.iterator();
            while (oppi.hasNext()) {
                GenericValue opp = (GenericValue) oppi.next();
                try {
                    opp.set("statusId", "PAYMENT_CANCELLED");
                    opp.store();
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    throw new GeneralException(e.getMessage());
                }
            }
        }

        // remove or offset the adjustments
        if (recalcAdjustments) {
            try {
                EntityCondition adjustmentCondition = EntityCondition.makeCondition(EntityOperator.AND,
                                                             EntityCondition.makeCondition("orderId", orderId),
                                                             EntityCondition.makeCondition(EntityOperator.OR,
                                                                                           EntityCondition.makeCondition("orderAdjustmentTypeId", "PROMOTION_ADJUSTMENT"),
                                                                                           EntityCondition.makeCondition("orderAdjustmentTypeId", "SHIPPING_CHARGES"),
                                                                                           EntityCondition.makeCondition("orderAdjustmentTypeId", "SALES_TAX")));
                if (hasBilledAdjustments) {
                    // if order adjustment billings already exist for the order adjustments, do not remove the existing order adjustments, but
                    // offset them instead
                    // at this point we know forceComplete is true
                    List<GenericValue> offsetAdjustments = new ArrayList<GenericValue>();
                    for (GenericValue oa : delegator.findByCondition("OrderAdjustment", adjustmentCondition, null, null)) {
                        BigDecimal oaAmount = oa.getBigDecimal("amount");
                        if (oaAmount != null) {
                            GenericValue offset = delegator.makeValue("OrderAdjustment", oa.getAllFields());
                            offset.set("amount", oaAmount.negate());
                            offset.set("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                            offsetAdjustments.add(offset);
                        }
                    }
                    toStore.addAll(offsetAdjustments);
                } else {
                    // else just remove the adjustments
                    delegator.removeByCondition("OrderAdjustment", adjustmentCondition);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                throw new GeneralException(e.getMessage());
            }
        }

        // store the new items/adjustments
        try {
            delegator.storeAll(toStore);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new GeneralException(e.getMessage());
        }

        // make the order item object map & the ship group assoc list
        List orderItemShipGroupAssoc = new LinkedList();
        Map itemValuesBySeqId = new HashMap();
        Iterator oii = toStore.iterator();
        while (oii.hasNext()) {
            GenericValue v = (GenericValue) oii.next();
            if ("OrderItem".equals(v.getEntityName())) {
                itemValuesBySeqId.put(v.getString("orderItemSeqId"), v);
            } else if ("OrderItemShipGroupAssoc".equals(v.getEntityName())) {
                orderItemShipGroupAssoc.add(v);
            }
        }

        // reserve the inventory
        String productStoreId = cart.getProductStoreId();
        String orderTypeId = cart.getOrderType();
        List resErrorMessages = new LinkedList();
        try {
            Debug.log("Calling reserve inventory...", MODULE);
            org.ofbiz.order.order.OrderServices.reserveInventory(delegator, dispatcher, userLogin, locale, orderItemShipGroupAssoc, dropShipGroupIds, itemValuesBySeqId, orderTypeId, productStoreId, resErrorMessages);
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            throw new GeneralException(e.getMessage());
        }

        if (resErrorMessages.size() > 0) {
            throw new GeneralException(ServiceUtil.getErrorMessage(ServiceUtil.returnError(resErrorMessages)));
        }

        return true;
    }

    /**
     * Service for checking and re-calc the shipping amount.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map recalcOrderShipping(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // check and make sure we have permission to change the order
        Security security = dctx.getSecurity();
        if (!security.hasEntityPermission("ORDERMGR", "_UPDATE", userLogin)) {
            GenericValue placingCustomer = null;
            try {
                Map placingCustomerFields = UtilMisc.toMap("orderId", orderId, "partyId", userLogin.getString("partyId"), "roleTypeId", "PLACING_CUSTOMER");
                placingCustomer = delegator.findByPrimaryKey("OrderRole", placingCustomerFields);
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, "OrderErrorCannotGetOrderRoleEntity", locale, MODULE);
            }
            if (placingCustomer == null) {
                return UtilMessage.createAndLogServiceError("OrderYouDoNotHavePermissionToChangeThisOrdersStatus", locale, MODULE);
            }
        }

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "OrderErrorCannotGetOrderHeaderEntity", locale, MODULE);
        }

        if (orderHeader == null) {
            return UtilMessage.createAndLogServiceError("OrderErrorNoValidOrderHeaderFoundForOrderId", UtilMisc.toMap("orderId", orderId), locale, MODULE);
        }

        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        List shipGroups = orh.getOrderItemShipGroups();
        if (shipGroups != null) {
            Iterator i = shipGroups.iterator();
            while (i.hasNext()) {
                GenericValue shipGroup = (GenericValue) i.next();
                String shipGroupSeqId = shipGroup.getString("shipGroupSeqId");

                if (shipGroup.get("contactMechId") == null || shipGroup.get("shipmentMethodTypeId") == null) {
                    // not shipped (face-to-face order)
                    continue;
                }

                Map shippingEstMap = OrderEvents.getShipEstimate(dispatcher, delegator, orh, shipGroupSeqId);
                BigDecimal shippingTotal = null;
                if (orh.getValidOrderItems(shipGroupSeqId) == null || orh.getValidOrderItems(shipGroupSeqId).size() == 0) {
                    shippingTotal = BigDecimal.ZERO;
                    Debug.log("No valid order items found - " + shippingTotal, MODULE);
                } else {
                    shippingTotal = UtilValidate.isEmpty(shippingEstMap.get("shippingTotal")) ? BigDecimal.ZERO : (BigDecimal) shippingEstMap.get("shippingTotal");
                    shippingTotal = shippingTotal.setScale(decimals, rounding);
                }
                if (Debug.infoOn()) {
                    Debug.log("New Shipping Total [" + orderId + " / " + shipGroupSeqId + "] : " + shippingTotal, MODULE);
                }

                BigDecimal currentShipping = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orh.getOrderItemAndShipGroupAssoc(shipGroupSeqId), orh.getAdjustments(), false, false, true);
                currentShipping = currentShipping.add(OrderReadHelper.calcOrderAdjustments(orh.getOrderHeaderAdjustments(shipGroupSeqId), orh.getOrderItemsSubTotal(), false, false, true));

                if (Debug.infoOn()) {
                    Debug.log("Old Shipping Total [" + orderId + " / " + shipGroupSeqId + "] : " + currentShipping, MODULE);
                }

                List errorMessageList = (List) shippingEstMap.get(ModelService.ERROR_MESSAGE_LIST);
                if (errorMessageList != null) {
                    Debug.logWarning("Problem finding shipping estimates for [" + orderId + "/ " + shipGroupSeqId + "] = " + errorMessageList, MODULE);
                    continue;
                }

                if ((shippingTotal != null) && (shippingTotal.compareTo(currentShipping) != 0)) {
                    // place the difference as a new shipping adjustment
                    BigDecimal adjustmentAmount = shippingTotal.subtract(currentShipping);
                    Debug.log("Creating adjustment, old shipping = " + currentShipping + " new shipping = " + shippingTotal + ", adjustment amount = " + adjustmentAmount, MODULE);
                    String adjSeqId = delegator.getNextSeqId("OrderAdjustment");
                    GenericValue orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentId", adjSeqId));
                    orderAdjustment.set("orderAdjustmentTypeId", "SHIPPING_CHARGES");
                    orderAdjustment.set("amount", adjustmentAmount);
                    orderAdjustment.set("orderId", orh.getOrderId());
                    orderAdjustment.set("shipGroupSeqId", shipGroupSeqId);
                    orderAdjustment.set("orderItemSeqId", DataModelConstants.SEQ_ID_NA);
                    orderAdjustment.set("createdDate", UtilDateTime.nowTimestamp());
                    orderAdjustment.set("createdByUserLogin", userLogin.getString("userLoginId"));
                    try {
                        orderAdjustment.create();
                        Debug.log("Created shipping adjustment " + adjSeqId + " for order " + orh.getOrderId(), MODULE);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Problem creating shipping re-calc adjustment : " + orderAdjustment, MODULE);
                        return UtilMessage.createAndLogServiceError("OrderErrorCannotCreateAdjustment", locale, MODULE);
                    }
                }

            }
        }

        return ServiceUtil.returnSuccess();

    }

    /**
     * Update the quantities/prices for an existing purchase order, the only differ between ofbiz updateOrderItems service is not try to recalculate shipping.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map updatePurchaseOrderItems(DispatchContext dctx, Map context) {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String orderId = (String) context.get("orderId");
        Map overridePriceMap = (Map) context.get("overridePriceMap");
        Map itemDescriptionMap = (Map) context.get("itemDescriptionMap");
        Map itemPriceMap = (Map) context.get("itemPriceMap");
        Map itemQtyMap = (Map) context.get("itemQtyMap");
        Map customFieldsMap = (Map) context.get("customFieldsMap");
        // for accounting tags
        Map[] tagsMaps = UtilAccountingTags.getTagMapParameters(context);

        PurchasingRepositoryInterface purchasingRepository = null;
        OrderRepositoryInterface orderRepository = null;
        Order order = null;

        try {
            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory dd = domainLoader.loadDomainsDirectory();
            purchasingRepository = dd.getPurchasingDomain().getPurchasingRepository();
            // load order by orderId
            orderRepository = dd.getOrderDomain().getOrderRepository();
            order = orderRepository.getOrderById(orderId);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (EntityNotFoundException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        // go through the item map and obtain the totals per item
        Map<String, BigDecimal> itemTotals = new HashMap<String, BigDecimal>();
        Map tmp = getItemTotalsFromItemQtyMap(itemTotals, itemQtyMap);
        if (tmp != null) {
            return tmp;
        }

        // set the items quantity/price/description/custom fields
        try {
            List<OrderItem> updatedOrderItems = new ArrayList<OrderItem>();
            for (OrderItem orderItem : order.getItems()) {
                boolean update = false;
                if (itemTotals.containsKey(orderItem.getOrderItemSeqId())) {
                    BigDecimal qty = itemTotals.get(orderItem.getOrderItemSeqId());
                    qty = qty.add(orderItem.getCancelQuantity());
                    orderItem.setQuantity(qty);
                    Debug.logVerbose("Set item quantity: [" + orderItem.getOrderItemSeqId() + "] " + qty, MODULE);
                    update = true;
                }
                if (overridePriceMap.containsKey(orderItem.getOrderItemSeqId())) {
                    String priceStr = (String) itemPriceMap.get(orderItem.getOrderItemSeqId());
                    if (UtilValidate.isNotEmpty(priceStr)) {
                        double price = -1;
                        //parse the price
                        NumberFormat nf = null;
                        if (locale != null) {
                            nf = NumberFormat.getNumberInstance(locale);
                        } else {
                            nf = NumberFormat.getNumberInstance();
                        }
                        try {
                            price = nf.parse(priceStr).doubleValue();
                        } catch (ParseException e) {
                            return UtilMessage.createAndLogServiceError(e, MODULE);
                        }
                        orderItem.setUnitPrice(orderItem.convertToBigDecimal(price));
                        orderItem.setIsModifiedPrice("Y");
                        Debug.logVerbose("Set item price: [" + orderItem.getOrderItemSeqId() + "] " + price, MODULE);
                        update = true;
                    }
                }

                // Update the item description
                if (itemDescriptionMap != null && itemDescriptionMap.containsKey(orderItem.getOrderItemSeqId())) {
                    String description = (String) itemDescriptionMap.get(orderItem.getOrderItemSeqId());
                    orderItem.setItemDescription(description);
                    Debug.logVerbose("Set item description: [" + orderItem.getOrderItemSeqId() + "] " + description, MODULE);
                    update = true;
                }

                // Update the custom fields
                //  the Map contains the custom fields without their cust_ prefix, so for example cust_fooBar_seqid
                //  becomes fooBar_seqid -> value
                UtilCommon.setCustomFieldsFromServiceMap(orderItem, customFieldsMap, "_" + orderItem.getOrderItemSeqId(), delegator);

                // set accounting tags
                for (int j = 1; j <= UtilAccountingTags.TAG_COUNT; j++) {
                    Map tags = tagsMaps[j - 1];
                    if (tags != null && tags.get(orderItem.getOrderItemSeqId()) != null) {
                        String tagValue = (String) tags.get(orderItem.getOrderItemSeqId());
                        // avoid empty strings, they cause FK errors
                        if (UtilValidate.isEmpty(tagValue)) {
                            tagValue = null;
                        }
                        orderItem.set(UtilAccountingTags.ENTITY_TAG_PREFIX + j, tagValue);
                    }
                }

                // validate tags
                List<AccountingTagConfigurationForOrganizationAndUsage> missings = orderRepository.validateTagParameters(order, orderItem);
                if (!missings.isEmpty()) {
                    for (AccountingTagConfigurationForOrganizationAndUsage missingTag : missings) {
                        return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missingTag.getDescription()), locale, MODULE);
                    }
                }

                // if update, then put it into list which will be saved
                if (update) {
                    // make sure there is a SupplierProduct entry else the loadCartFromOrder will fail
                    SupplierProduct supplierProduct = purchasingRepository.getSupplierProduct(order.getBillFromVendorPartyId(), orderItem.getProductId(), orderItem.getQuantity(), order.getCurrencyUom());
                    if (supplierProduct == null) {
                        // create a supplier product
                        supplierProduct = new SupplierProduct();
                        supplierProduct.setProductId(orderItem.getProductId());
                        supplierProduct.setSupplierProductId(orderItem.getProductId());
                        supplierProduct.setPartyId(order.getBillFromVendorPartyId());
                        supplierProduct.setMinimumOrderQuantity(BigDecimal.ZERO);
                        supplierProduct.setLastPrice(BigDecimal.ZERO);
                        supplierProduct.setCurrencyUomId(order.getCurrencyUom());
                        supplierProduct.setAvailableFromDate(UtilDateTime.nowTimestamp());
                        supplierProduct.setComments(UtilMessage.expandLabel("PurchOrderCreateSupplierProductByUserLogin", UtilMisc.toMap("userLoginId", userLogin.getString("userLoginId")), locale));
                        //use purchasingRepository to create new SupplierProduct
                        purchasingRepository.createSupplierProduct(supplierProduct);
                        Debug.logInfo("Created SupplierProduct for productId [" + orderItem.getProductId() + "], supplier [" + order.getBillFromVendorPartyId() + "]", MODULE);
                    }
                    updatedOrderItems.add(orderItem);
                }
            }

            // save all the updated information
            if (!UtilValidate.isEmpty(updatedOrderItems)) {
                purchasingRepository.update(updatedOrderItems);
            }

            // reload order to load the change
            order = orderRepository.getOrderById(orderId);

            List<OrderItemShipGroupAssoc> updatedOrderItemShipGroupAssocs = new ArrayList<OrderItemShipGroupAssoc>();
            for (OrderItemShipGroupAssoc orderItemShipGroupAssoc : order.getOrderItemShipGroupAssocs()) {
                // iterate all OrderItemShipGroupAssoc
                boolean update = false;
                BigDecimal cancelQty = orderItemShipGroupAssoc.getCancelQuantity();
                BigDecimal newQuantity = orderItemShipGroupAssoc.getOrderItem().getQuantity();
                if (cancelQty != null) {
                    newQuantity = cancelQty.add(newQuantity);
                }
                // set orderItemShipGroupAssoc.getQuantity() = orderItemShipGroupAssoc.getOrderItem().getQuantity() + orderItemShipGroupAssoc.getCancelQuantity()
                if (orderItemShipGroupAssoc.getQuantity().compareTo(newQuantity) != 0) {
                    orderItemShipGroupAssoc.setQuantity(newQuantity);
                    update = true;
                }
                // if update, then put it into list which will be saved
                if (update) {
                    updatedOrderItemShipGroupAssocs.add(orderItemShipGroupAssoc);
                }
            }
            // save all the updated information
            if (!UtilValidate.isEmpty(updatedOrderItemShipGroupAssocs)) {
                purchasingRepository.update(updatedOrderItemShipGroupAssocs);
            }

        } catch (RepositoryException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (EntityNotFoundException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }


        // log an order note
        try {
            dispatcher.runSync("createOrderNote", UtilMisc.toMap("orderId", orderId, "note", "Updated order.", "internalNote", "Y", "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }

        // obtain a shopping cart object for updating
        OpentapsShoppingCart cart = null;
        try {
            cart = loadCartForUpdate(dispatcher, delegator, userLogin, orderId);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        if (cart == null) {
            return UtilMessage.createAndLogServiceError("ERROR: Null shopping cart object returned!", MODULE);
        }

        // re-calculate total
        try {
        Map tmpResult = dispatcher.runSync("resetGrandTotal", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
            if (ServiceUtil.isFailure(tmpResult)) {
                // isError should automatically rollback this service too
                return tmpResult;
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, "Cannot reset grand total for order [" + orderId + "]", MODULE);
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        result.put("orderId", orderId);
        return result;
    }

    /**
     * Describe <code>getItemTotalsFromItemQtyMap</code> method here.
     * @param itemTotals an initialized <code>Map</code> instance where the method will store the orderItemSeqId -> quantity
     * @param itemQtyMap the input <code>Map</code> containing the itemInfo -> quantity
     * @return <code>null</code>, or a service error response <code>Map</code> if an error occurred
     */
    @SuppressWarnings("unchecked")
    public static Map getItemTotalsFromItemQtyMap(Map<String, BigDecimal> itemTotals, Map<String, String> itemQtyMap) {
        for (String key : itemQtyMap.keySet()) {
            String quantityStr = itemQtyMap.get(key);
            BigDecimal groupQty = BigDecimal.ZERO;
            try {
                groupQty = new BigDecimal(quantityStr);
            } catch (NumberFormatException e) {
                return UtilMessage.createAndLogServiceError(e, MODULE);
            }

            if (groupQty.signum() == 0) {
                return UtilMessage.createAndLogServiceError("Quantity must be >0, use cancel item to cancel completely!", MODULE);
            }

            String[] itemInfo = key.split(":");
            BigDecimal tally = itemTotals.get(itemInfo[0]);
            if (tally == null) {
                tally = groupQty;
            } else {
                tally = tally.add(groupQty);
            }
            // the first String in itemInfo is the orderItemSeqId
            itemTotals.put(itemInfo[0], tally);
        }
        return null;
    }

}
