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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartHelper;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.security.Security;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * Extending Ofbiz ShoppingCartHelper to support Opentaps additional cart fields, such as the accounting tags.
 */
public class OpentapsShoppingCartHelper extends ShoppingCartHelper {

    private static final String MODULE = OpentapsShoppingCartHelper.class.getName();

    private OpentapsShoppingCart cart;
    private Delegator delegator;
    private LocalDispatcher dispatcher;

    /**
     * Changes will be made to the cart directly, as opposed
     * to a copy of the cart provided.
     *
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param cart The cart to manipulate
     */
    public OpentapsShoppingCartHelper(Delegator delegator, LocalDispatcher dispatcher, ShoppingCart cart) {
        super(delegator, dispatcher, cart);
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        if (delegator == null) {
            this.delegator = dispatcher.getDelegator();
        }
        setCart(cart);
    }

    /**
     * Changes will be made to the cart directly, as opposed
     * to a copy of the cart provided.
     *
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param cart The cart to manipulate
     */
    public OpentapsShoppingCartHelper(LocalDispatcher dispatcher, ShoppingCart cart) {
        this(null, dispatcher, cart);
    }

    protected void setCart(ShoppingCart cart) {
        if (cart instanceof OpentapsShoppingCart) {
            this.cart = (OpentapsShoppingCart) cart;
        } else {
            this.cart = new OpentapsShoppingCart(cart);
        }
    }
 
    public OpentapsShoppingCart getCart() {
        return this.cart;
    }

    public Delegator getDelegator() {
        return this.delegator;
    }

    public LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }

    /**
     * Updates the items in the shopping cart.
     * @param security a <code>Security</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param context a <code>Map</code> value
     * @param removeSelected a <code>boolean</code> value
     * @param selectedItems a <code>String</code> value
     * @param locale a <code>Locale</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map modifyCart(Security security, GenericValue userLogin, Map context, boolean removeSelected, String[] selectedItems, Locale locale) {

        // process the accounting tags first, we then need to remove them from the parameters map passed to the ofbiz helper
        // as it would try to parse them as numbers
        // also process the shipBeforeDate / shipAfterDate as they are not parsed properly in the ofbiz helper
        // TODO: This should be refactored to use UtilHttp.parseMultiFormData(parameters), and so does the parent helper method
        Set<String> parameterNames = context.keySet();
        Iterator<String> parameterNameIter = parameterNames.iterator();
        if (locale == null) {
            locale = cart.getLocale();
        }
        // track which items have accounting tags updated, so we can validate them
        Set<Integer> cartWithTags = new HashSet<Integer>();
        while (parameterNameIter.hasNext()) {
            String parameterName = parameterNameIter.next();

            // skip the remove selected checkbox param
            if ("selectedItem".equals(parameterName)) {
                continue;
            }

            String parameterValue = (String) context.get(parameterName);
            int underscorePos = parameterName.lastIndexOf('_');

            // for validating custom field
            ModelEntity model = delegator.getModelEntity("OrderItem");

            if (underscorePos >= 0) {
                try {
                    String indexStr = parameterName.substring(underscorePos + 1);
                    String indexParameterName = parameterName.substring(0, underscorePos);
                    int index = Integer.parseInt(indexStr);
                    // get the cart item
                    ShoppingCartItem item = cart.findCartItem(index);
                    if (item == null) {
                        continue;
                    }

                    if (parameterName.toUpperCase().startsWith("TAG")) {
                        // avoid storing empty strings, they would trigger FK errors
                        if (UtilValidate.isEmpty(parameterValue)) {
                            parameterValue = null;
                        }
                        Debug.logInfo("Setting cart item attribute for tag [" + indexParameterName + "] to: [" + parameterValue + "]", MODULE);
                        cartWithTags.add(index);
                        item.setAttribute(indexParameterName, parameterValue);
                        // remove the parameter
                        parameterNameIter.remove();
                    } else if (parameterName.startsWith("shipBeforeDate")) {
                        if (parameterValue.length() > 0) {
                            item.setShipBeforeDate(UtilDate.toTimestamp(parameterValue, TimeZone.getDefault(), locale));
                        }
                        // remove the parameter
                        parameterNameIter.remove();
                    } else if (parameterName.startsWith("shipAfterDate")) {
                        if (parameterValue.length() > 0) {
                            item.setShipAfterDate(UtilDate.toTimestamp(parameterValue, TimeZone.getDefault(), locale));
                        }
                        // remove the parameter
                        parameterNameIter.remove();
                    } else if (UtilCommon.isCustomEntityField(parameterName)) {
                        if (parameterValue.length() > 0) {
                            // validate
                            try {
                                model.convertFieldValue(indexParameterName, parameterValue, delegator);
                            } catch (IllegalArgumentException e) {
                                return UtilMessage.createAndLogServiceError(e, locale, MODULE);
                            }
                        }
                        item.setAttribute(indexParameterName, parameterValue);
                    }
                } catch (NumberFormatException nfe) {
                    Debug.logWarning(nfe, UtilProperties.getMessage(resource_error, "OrderCaughtNumberFormatExceptionOnCartUpdate", locale), MODULE);
                } catch (Exception e) {
                    Debug.logWarning(e, UtilProperties.getMessage(resource_error, "OrderCaughtExceptionOnCartUpdate", locale), MODULE);
                }
            }
        }

        // validate the tags of each updated item
        try {
            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory dd = domainLoader.loadDomainsDirectory();
            OrderRepositoryInterface orderRepository = dd.getOrderDomain().getOrderRepository();
            for (Integer index : cartWithTags) {
                ShoppingCartItem item = cart.findCartItem(index);
                // validate the accounting tags
                List<AccountingTagConfigurationForOrganizationAndUsage> missings = orderRepository.validateTagParameters(cart, item);
                if (!missings.isEmpty()) {
                    for (AccountingTagConfigurationForOrganizationAndUsage missingTag : missings) {
                        return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missingTag.getDescription()), locale, MODULE);
                    }
                }
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return super.modifyCart(security, userLogin, context, removeSelected, selectedItems, locale);
    }
}
