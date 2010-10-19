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
package com.opensourcestrategies.financials.payroll;

import java.util.List;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Helper class for dealing with Payroll (paychecks).
 */
public class PayrollHelper {

    private static final String MODULE = PayrollHelper.class.getName();
    public static final String resource = "FinancialsUiLabels";

    protected String organizationPartyId = null;
    protected Delegator delegator = null;

    /**
     * Creates a new <code>PayrollHelper</code> instance.
     *
     * @param organizationPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     */
    public PayrollHelper(String organizationPartyId, Delegator delegator) {
        this.organizationPartyId = organizationPartyId;
        this.delegator = delegator;
    }

    /**
     * Returns paycheck paymentTypeIds which are currently configured in EmployeePaycheckType for the _NA_ employee and all the
     * partyIds in the passed in List.
     * @param employeePartyIds a list of party ids
     * @return the list of available paycheck paymentTypeIds for the given employee party ids
     * @throws GenericEntityException if an error occurs
     */
    public List<GenericValue> getAvailablePaycheckTypes(List<String> employeePartyIds) throws GenericEntityException {

        // base condition is just unexpired EmployeePaycheckTypes
        List<EntityCondition> searchConditions = UtilMisc.<EntityCondition>toList(EntityUtil.getFilterByDateExpr());

        // I assume we want only the paycheck types for the organization the user has selected to work with
        searchConditions.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId));

        // get the organization default paycheck types - those with employeePartyId = '_NA_' - even if partyIds are not specified
        List<String> eligiblePartyIds = UtilMisc.toList("_NA_");
        // if partyIds are specified, then we want to search on these partyIds and _NA_
        if (UtilValidate.isNotEmpty(employeePartyIds)) {
            eligiblePartyIds.addAll(employeePartyIds);
        }
        searchConditions.add(EntityCondition.makeCondition("employeePartyId", EntityOperator.IN, eligiblePartyIds));

        EntityCondition findCondition = EntityCondition.makeCondition(searchConditions, EntityOperator.AND);
        List<GenericValue> employeePaycheckTypeList = delegator.findByConditionCache("EmployeePaycheckType", findCondition, UtilMisc.toList("paymentTypeId"), UtilMisc.toList("paymentTypeId"));

        return employeePaycheckTypeList;
    }

    /**
     * Return the default list of paycheck paymentTypeIds for _NA_ employees configured in EmployeePaycheckType.
     * @return the list of available paycheck paymentTypeIds for the _NA_ employees
     * @throws GenericEntityException if an error occurs
     */
    public List<GenericValue> getAvailablePaycheckTypes() throws GenericEntityException {
        return getAvailablePaycheckTypes(null);
    }




}
