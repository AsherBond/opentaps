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

package org.opentaps.common.order.shoppingcart;

import java.math.BigDecimal;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.CheckOutEvents;
import org.ofbiz.order.shoppingcart.ShoppingCart;

/**
 * Opentaps Check Out Events.
 */
public final class OpentapsCheckOutEvents {

    private OpentapsCheckOutEvents() { }

    private static final String MODULE = OpentapsCheckOutEvents.class.getName();

    /**
     * finalize order entry, process order terms & shipping destination.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String finalizeOrderEntry(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String optionType = request.getParameter("optionType");
        if (cart != null && optionType != null) {
            if (optionType.equals("updateAgreement")) {
                updateAgreement(request, delegator, cart);
            } else if (optionType.equals("addTerm")) {
                addTerm(request, delegator, cart);
            } else if (optionType.equals("updateTerm")) {
                updateTerm(request, delegator, cart);
            }  else if (optionType.equals("removeTerm")) {
                removeTerm(request, delegator, cart);
            } else if (optionType.equals("ShippingOption")) {
                shippingOption(request, delegator, cart);
            }
        }

        // call checkOutEvents.finalizeOrderEntry
        return CheckOutEvents.finalizeOrderEntry(request, response);
    }

    @SuppressWarnings("unchecked")
    private static void updateAgreement(HttpServletRequest request, Delegator delegator, ShoppingCart cart) {
        // update order agreement option
        String agreementId = request.getParameter("agreementId");
        cart.removeOrderTerms();
        cart.setAgreementId(agreementId);
        try {
            //get AgreementTerm List by agreementId
            List<GenericValue> agreementTerms = delegator.findByAnd("AgreementTerm", UtilMisc.toList(
                            EntityCondition.makeCondition("agreementId", agreementId),
                            EntityUtil.getFilterByDateExpr()));
            for (GenericValue agreementTerm : agreementTerms) {
                //put them into cart
                GenericValue orderTerm = GenericValue.create(delegator.getModelEntity("OrderTerm"));
                orderTerm.put("termTypeId", agreementTerm.get("termTypeId"));
                orderTerm.put("termValue", agreementTerm.get("termValue"));
                orderTerm.put("termDays", agreementTerm.get("termDays"));
                orderTerm.put("textValue", agreementTerm.get("textValue"));
                cart.addOrderTerm(orderTerm);
            }

        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
        }
    }

    private static void addTerm(HttpServletRequest request, Delegator delegator, ShoppingCart cart) {
        String termTypeId = request.getParameter("termTypeId");
        BigDecimal termValue = UtilValidate.isEmpty(request.getParameter("termValue")) ? null : new BigDecimal(request.getParameter("termValue"));
        Long termDays = UtilValidate.isEmpty(request.getParameter("termDays")) ? null : Long.parseLong(request.getParameter("termDays"));
        String textValue = request.getParameter("textValue");
        if (termTypeId != null) {
            cart.addOrderTerm(termTypeId, termValue, termDays, textValue);
        }
    }

    private static void updateTerm(HttpServletRequest request, Delegator delegator, ShoppingCart cart) {
        String termTypeId = request.getParameter("termTypeId");
        Double termValue = UtilValidate.isEmpty(request.getParameter("termValue")) ? null : Double.parseDouble(request.getParameter("termValue"));
        Long termDays = UtilValidate.isEmpty(request.getParameter("termDays")) ? null : Long.parseLong(request.getParameter("termDays"));
        String textValue = request.getParameter("textValue");
        for (int i = 0; i < cart.getOrderTerms().size(); i++) {
            GenericValue orderTerm = (GenericValue) cart.getOrderTerms().get(i);
            if (orderTerm.getString("termTypeId").equals(termTypeId)) {
                orderTerm.set("termValue", termValue);
                orderTerm.set("termDays", termDays);
                orderTerm.set("textValue", textValue);
                break;
            }
        }
    }

    private static void removeTerm(HttpServletRequest request, Delegator delegator, ShoppingCart cart) {
        String termTypeId = request.getParameter("termTypeId");
        for (int i = 0; i < cart.getOrderTerms().size(); i++) {
            GenericValue orderTerm = (GenericValue) cart.getOrderTerms().get(i);
            if (orderTerm.getString("termTypeId").equals(termTypeId)) {
                cart.getOrderTerms().remove(orderTerm);
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void shippingOption(HttpServletRequest request, Delegator delegator, ShoppingCart cart) {
        String facilityId = request.getParameter("facilityId");
        String contactMechId = request.getParameter("contactMechId");
        String shippingInstructions = request.getParameter("shippingInstructions");
        String maySplit = request.getParameter("maySplit");
        String giftMessage = request.getParameter("giftMessage");
        String isGift = request.getParameter("isGift");
        String shippingMethod = request.getParameter("shippingMethod");
        String carrierPartyId = request.getParameter("carrierPartyId");
        if (UtilValidate.isNotEmpty(contactMechId) && UtilValidate.isEmpty(facilityId)) {
            // update facilityId by shippingContactMechId
            try {
                List<GenericValue> facilityContactMechs = delegator.findByAnd("FacilityContactMech", UtilMisc.toList(
                                 EntityCondition.makeCondition("contactMechId", contactMechId),
                                 EntityUtil.getFilterByDateExpr()));
                if (facilityContactMechs.size() > 0) {
                    GenericValue facilityContactMechValue = facilityContactMechs.get(0);
                    cart.setFacilityId(facilityContactMechValue.getString("facilityId"));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        for (int i = 0; i < cart.getShipGroupSize(); i++) {
            // set shipping options
            cart.setShippingInstructions(i, shippingInstructions);
            cart.setShippingContactMechId(i, contactMechId);
            cart.setShipmentMethodTypeId(i, shippingMethod);
            cart.setCarrierPartyId(i, carrierPartyId);
            cart.setMaySplit(i, Boolean.valueOf(maySplit));
            cart.setIsGift(i, Boolean.valueOf(isGift));
            cart.setGiftMessage(i, giftMessage);
        }
    }

}
