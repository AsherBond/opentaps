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

package com.opensourcestrategies.crmsfa.reports;

import java.util.List;

import com.opensourcestrategies.crmsfa.cases.UtilCase;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

/**
 * CRMSFA Report Helper.
 */
public final class ReportHelper {

    private ReportHelper() { }

    public static List<String> DASHBOARD_IGNORED_LEAD_STATUSES = UtilMisc.toList("PTYLEAD_CONVERTED", "PTYLEAD_DEAD");
    public static List<String> DASHBOARD_IGNORED_SALES_OPPORTUNITY_STAGES = UtilMisc.toList("SOSTG_CLOSED", "SOSTG_LOST");
    public static List DASHBOARD_IGNORED_CASES_STAGES = UtilCase.CASE_STATUSES_COMPLETED;

    /**
     * Find all the current LEAD statuses except those defined in the DASHBOARD_IGNORED_LEAD_STATUSES list.
     * @param delegator a <code>Delegator</code> value
     * @return all the current LEAD statuses except those defined in the DASHBOARD_IGNORED_LEAD_STATUSES list
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> findLeadStatusesForDashboardReporting(Delegator delegator) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                           EntityCondition.makeCondition("statusTypeId", EntityOperator.EQUALS, "PARTY_LEAD_STATUS"),
                                           EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, DASHBOARD_IGNORED_LEAD_STATUSES));

        return delegator.findByConditionCache("StatusItem", conditions, UtilMisc.toList("statusId", "description"), UtilMisc.toList("sequenceId"));
    }

    /**
     * Find all the sales opportunity stages except those defined in the DASHBOARD_IGNORED_SALES_OPPORTUNITY_STAGES list.
     * @param delegator a <code>Delegator</code> value
     * @return all the sales opportunity stages except those defined in the DASHBOARD_IGNORED_SALES_OPPORTUNITY_STAGES list
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> findSalesOpportunityStagesForDashboardReporting(Delegator delegator) throws GenericEntityException {
        EntityCondition condition = EntityCondition.makeCondition("opportunityStageId", EntityOperator.NOT_IN, DASHBOARD_IGNORED_SALES_OPPORTUNITY_STAGES);
        return delegator.findByConditionCache("SalesOpportunityStage", condition, UtilMisc.toList("opportunityStageId", "description"), UtilMisc.toList("sequenceNum"));
    }

    /**
     * Find all the case statuses except those defined in the DASHBOARD_IGNORED_CASES_STAGES list.
     * @param delegator a <code>Delegator</code> value
     * @return all the case statuses except those defined in the DASHBOARD_IGNORED_CASES_STAGES list
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> findCasesStagesForDashboardReporting(Delegator delegator) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("statusTypeId", EntityOperator.EQUALS, "CUSTREQ_STTS"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, DASHBOARD_IGNORED_CASES_STAGES));

        return delegator.findByConditionCache("StatusItem", conditions, UtilMisc.toList("statusId", "description"), UtilMisc.toList("sequenceId"));
    }

}
