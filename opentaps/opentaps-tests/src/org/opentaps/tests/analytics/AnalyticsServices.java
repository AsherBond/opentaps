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

package org.opentaps.tests.analytics;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.party.PartyContactHelper;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.tests.analytics.tests.AbstractExpectedResultset;
import org.opentaps.tests.analytics.tests.CustomersByGeoLocation;
import org.opentaps.tests.analytics.tests.TestObjectGenerator;

public final class AnalyticsServices {

    private AnalyticsServices() { }

    private static final String MODULE = AnalyticsServices.class.getName();


    /**
     * Generate random accounts, products in categories and random orders.
     * This is mainly used for testing the Customer reports as only customers with orders will be in the customer dimension.
     * Accept a few optional parameters:
     * - organizationPartyId : the organization for which are made the generated orders
     * - ordersToGenerate : number of random orders to generate (the number of generated accounts will be this number / 2)
     * - fromDate/thruDate: accounts & orders should be created within given time lag.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a service response <code>Map</code> value
     */
    public static Map<String, Object> createCustomerDimensionTestData(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);

        // read parameters
        Integer ordersToGenerate = (Integer) context.get("ordersToGenerate");
        String organizationPartyId = (String) context.get("organizationPartyId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");

        Boolean testDataOnly = Boolean.TRUE; //(Boolean) context.get("testDataOnly");


        // set default values if not given
        if (UtilValidate.isEmpty(organizationPartyId)) {
            organizationPartyId = "Company";
        }

        if (ordersToGenerate == null || ordersToGenerate <= 0) {
            ordersToGenerate = 10;
        }

        boolean dataOnly = false;
        if (testDataOnly != null) {
            dataOnly = testDataOnly.booleanValue();
        }

        AbstractExpectedResultset<CustomersByGeoLocation> expectedResults = null;

        try {
            if (!dataOnly) {
                expectedResults = new AbstractExpectedResultset<CustomersByGeoLocation>("CustomersByGeoLocation", CustomersByGeoLocation.class);
            }

            TestObjectGenerator testObjects = new TestObjectGenerator(delegator, dispatcher);

            // generate 1000 products distributed among 100 product categories.
            List<String> productIds = testObjects.getProduct(1000, testObjects.getProductCategory(100, fromDate), fromDate);

            // generate N random orders made by a pool of N/2 random accounts
            List<String> orderIds = testObjects.getOrders(ordersToGenerate, organizationPartyId, fromDate, thruDate, productIds);

            if (dataOnly) {
                return ServiceUtil.returnSuccess();
            }

            // retrieve those orders with the customer address that will end up in the report
            EntityCondition conditionList = EntityCondition.makeCondition(EntityOperator.AND, EntityCondition.makeCondition("orderId", EntityOperator.IN, orderIds));
            List<GenericValue> orders = delegator.findByCondition("OrderHeader", conditionList, Arrays.asList("billToPartyId"), null);
            List<String> customerIds = EntityUtil.getFieldListFromEntityList(orders, "billToPartyId", true);
            List<GenericValue> customers = delegator.findByCondition("Party", EntityCondition.makeCondition("partyId", EntityOperator.IN, customerIds), null, null);
            for (GenericValue customer : customers) {
                GenericValue primaryAddress = PartyContactHelper.getPostalAddressValueByPurpose(customer.getString("partyId"), "GENERAL_LOCATION", false, delegator);
                GenericValue country = primaryAddress.getRelatedOne("CountryGeo");
                GenericValue state = primaryAddress.getRelatedOne("StateProvinceGeo");
                String countryName = country.getString("geoName");
                String stateName = state.getString("geoName");
                expectedResults.reset();
                boolean hasElement = false;
                while (expectedResults.hasNext()) {
                    CustomersByGeoLocation element = expectedResults.next();
                    if (countryName.equals(element.getCountry()) && stateName.equals(element.getState())) {
                        element.setQuantity(element.getQuantity() + 1);
                        hasElement = true;
                    }
                }
                if (!hasElement) {
                    CustomersByGeoLocation newElement = new CustomersByGeoLocation();
                    newElement.setCountry(countryName);
                    newElement.setState(stateName);
                    newElement.setQuantity(1);
                    expectedResults.add(newElement);
                }
            }
            expectedResults.store();

        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        } catch (GenericServiceException gse) {
            return UtilMessage.createAndLogServiceError(gse, locale, MODULE);
        } catch (Exception e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }
}
