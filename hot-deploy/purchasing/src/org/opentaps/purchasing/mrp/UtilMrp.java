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
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 * 
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.purchasing.mrp;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Iterator;

/**
 * Utility methods for working with mrp
 */
public class UtilMrp {

    public static final String module = UtilMrp.class.getName();

    /**
     * Returns List of productStoreIds from productStoreGroupId for Mrp purposes
     * @param productStoreGroupId
     * @param delegator
     * @return
     * @throws GenericEntityException
     */
    public static List getMrpProductStoreIdsFromGroup(String productStoreGroupId, GenericDelegator delegator) throws GenericEntityException {
        List productStoreIds = null;
        if (UtilValidate.isNotEmpty(productStoreGroupId)) {
            List productStoreGroupConditions = UtilMisc.toList( new EntityExpr( "productStoreGroupId" , EntityOperator.EQUALS , productStoreGroupId ) ) ;
            List productStoreGroupFieldsToSelect = UtilMisc.toList( "productStoreId" );
            List productStoreGroups = delegator.findByCondition("MrpProductStoreGroupAndProductStore",
                                        new EntityConditionList(productStoreGroupConditions, EntityOperator.OR),
                                        null,
                                        productStoreGroupFieldsToSelect,
                                        null,
                                        UtilCommon.DISTINCT_READ_OPTIONS);
            productStoreIds = EntityUtil.getFieldListFromEntityList(productStoreGroups, "productStoreId", true);
        }
        return productStoreIds;
    }
    
    @SuppressWarnings("unchecked")
    public static List<String> getProductIdsFromSupplier(String supplierPartyId, GenericDelegator delegator) throws GenericEntityException {
        List<String> productIds = null;
        if (UtilValidate.isNotEmpty(supplierPartyId)) {
            List<EntityCondition> conditions = UtilMisc.toList(new EntityExpr( "partyId" , EntityOperator.EQUALS , supplierPartyId));
            conditions.add(EntityUtil.getFilterByDateExpr("availableFromDate", "availableThruDate"));
            List<String> fieldsToSelect = Arrays.asList("productId");
            List<GenericValue> products = delegator.findByCondition("SupplierProduct",
                    new EntityConditionList(conditions, EntityOperator.AND),
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
