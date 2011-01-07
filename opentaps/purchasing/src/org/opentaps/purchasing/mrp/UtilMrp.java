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

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.purchasing.mrp;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.util.UtilCommon;

import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for working with MRP.
 */
public final class UtilMrp {

    private UtilMrp() { }

    private static final String MODULE = UtilMrp.class.getName();

    /**
     * Returns List of productStoreIds from productStoreGroupId for Mrp purposes.
     * @param productStoreGroupId the product store group ID
     * @param delegator a <code>Delegator</code> value
     * @return the list of productStoreIds from the given productStoreGroupId
     * @throws GenericEntityException if an error occurs
     */
    public static List<String> getMrpProductStoreIdsFromGroup(String productStoreGroupId, Delegator delegator) throws GenericEntityException {
        List<String> productStoreIds = null;
        if (UtilValidate.isNotEmpty(productStoreGroupId)) {
            EntityCondition productStoreGroupCondition = EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeCondition("productStoreGroupId", EntityOperator.EQUALS, productStoreGroupId));
            List<String> productStoreGroupFieldsToSelect = UtilMisc.toList("productStoreId");
            List<GenericValue> productStoreGroups = delegator.findByCondition("MrpProductStoreGroupAndProductStore",
                                        productStoreGroupCondition,
                                        null,
                                        productStoreGroupFieldsToSelect,
                                        null,
                                        UtilCommon.DISTINCT_READ_OPTIONS);
            productStoreIds = EntityUtil.getFieldListFromEntityList(productStoreGroups, "productStoreId", true);
        }
        return productStoreIds;
    }

    public static List<String> getProductIdsFromSupplier(String supplierPartyId, Delegator delegator) throws GenericEntityException {
        List<String> productIds = null;
        if (UtilValidate.isNotEmpty(supplierPartyId)) {
            EntityCondition condition = EntityCondition.makeCondition(EntityOperator.AND,
                                                                      EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, supplierPartyId),
                                                                      EntityUtil.getFilterByDateExpr("availableFromDate", "availableThruDate"));
            List<String> fieldsToSelect = Arrays.asList("productId");
            List<GenericValue> products = delegator.findByCondition("SupplierProduct",
                    condition,
                    null,
                    fieldsToSelect,
                    null,
                    UtilCommon.DISTINCT_READ_OPTIONS
            );
            productIds = EntityUtil.getFieldListFromEntityList(products, "productId", true);
        }
        return productIds;
    }

}
