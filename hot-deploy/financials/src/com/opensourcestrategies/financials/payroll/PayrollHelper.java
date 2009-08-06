package com.opensourcestrategies.financials.payroll;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

import java.util.List;

/*
* Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
public class PayrollHelper {

    public static final String module = PayrollHelper.class.getName();
    public static final String resource = "FinancialsUiLabels";

    protected String organizationPartyId = null;
    protected GenericDelegator delegator = null;

    public PayrollHelper(String organizationPartyId, GenericDelegator delegator) {
        this.organizationPartyId = organizationPartyId;
        this.delegator = delegator;
    }

    /**
     * Returns paycheck paymentTypeIds which are currently configured in EmployeePaycheckType for the _NA_ employee and all the
     * partyIds in the passed in List
     * @param employeePartyIds
     * @return
     * @throws GenericEntityException
     */
    public List getAvailablePaycheckTypes(List employeePartyIds) throws GenericEntityException {
    	
        // base condition is just unexpired EmployeePaycheckTypes
        List searchConditions = UtilMisc.toList(EntityUtil.getFilterByDateExpr());
        
        // I assume we want only the paycheck types for the organization the user has selected to work with
        searchConditions.add(new EntityExpr("organizationPartyId", EntityOperator.EQUALS, organizationPartyId));        
        
        // get the organization default paycheck types - those with employeePartyId = '_NA_' - even if partyIds are not specified
        List eligiblePartyIds = UtilMisc.toList("_NA_");        
        // if partyIds are specified, then we want to search on these partyIds and _NA_
        if (UtilValidate.isNotEmpty(employeePartyIds)) {
            eligiblePartyIds.addAll(employeePartyIds);
        }
        searchConditions.add(new EntityExpr("employeePartyId", EntityOperator.IN, eligiblePartyIds));        
       
        EntityCondition findCondition = new EntityConditionList(searchConditions,
            EntityOperator.AND);
        List employeePaycheckTypeList = delegator.findByConditionCache("EmployeePaycheckType", findCondition, UtilMisc.toList("paymentTypeId"), UtilMisc.toList("paymentTypeId"));
        
        return employeePaycheckTypeList;
    }

    /**
     * Return the default list of paycheck paymentTypeIds for _NA_ employees configured in EmployeePaycheckType
     * @return
     * @throws GenericEntityException
     */
    public List getAvailablePaycheckTypes() throws GenericEntityException {
        return getAvailablePaycheckTypes(null);
    }
    
    


}
