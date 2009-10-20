/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

import com.opensourcestrategies.crmsfa.cases.UtilCase;

import java.util.List;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

/**
 * CRMSFA Report Helper
 * @author Ray Shi
 */

public class ReportHelper {

    public static final String module = ReportHelper.class.getName();
    
    public static List<String> DASHBOARD_IGNORED_LEAD_STATUSES = UtilMisc.toList("PTYLEAD_CONVERTED", "PTYLEAD_DEAD");
    public static List<String> DASHBOARD_IGNORED_SALES_OPPORTUNITY_STAGES = UtilMisc.toList("SOSTG_CLOSED", "SOSTG_LOST");
    public static List DASHBOARD_IGNORED_CASES_STAGES = UtilCase.CASE_STATUSES_COMPLETED;

    /**
     * Find all the current LEAD statues except those defined in the DASHBOARD_IGNORED_LEAD_STATUSES list.
     * @param GenericDelegator
     */
    public static List<GenericValue> findLeadStatusesForDashboardReporting(GenericDelegator delegator)
    	throws GenericEntityException {
    	EntityCondition conditions = new EntityConditionList(
    		UtilMisc.toList(
    			new EntityExpr("statusTypeId", EntityOperator.EQUALS, "PARTY_LEAD_STATUS"),
    			new EntityExpr("statusId", EntityOperator.NOT_IN, DASHBOARD_IGNORED_LEAD_STATUSES)
    		),
    		EntityOperator.AND
    	);
    	
    	return delegator.findByConditionCache("StatusItem", conditions, UtilMisc.toList("statusId", "description"), UtilMisc.toList("sequenceId"));
    }
    
    /**
     * Find all the sales opportunity stages except those defined in the DASHBOARD_IGNORED_SALES_OPPORTUNITY_STAGES list.
     * @param GenericDelegator
     */
    public static List<GenericValue> findSalesOpportunityStagesForDashboardReporting(GenericDelegator delegator)
    	throws GenericEntityException {
    	EntityCondition condition = new EntityExpr("opportunityStageId", EntityOperator.NOT_IN, DASHBOARD_IGNORED_SALES_OPPORTUNITY_STAGES);        	
        return delegator.findByConditionCache("SalesOpportunityStage", condition, UtilMisc.toList("opportunityStageId", "description"), UtilMisc.toList("sequenceNum"));    	
	}

    /**
     * Find all the case statuses except those defined in the DASHBOARD_IGNORED_CASES_STAGES list.
     * @param GenericDelegator
     */
    public static List<GenericValue> findCasesStagesForDashboardReporting(GenericDelegator delegator)
    	throws GenericEntityException {
        EntityCondition conditions = new EntityConditionList( UtilMisc.toList(
                new EntityExpr("statusTypeId", EntityOperator.EQUALS, "CUSTREQ_STTS"),
                new EntityExpr("statusId", EntityOperator.NOT_IN, DASHBOARD_IGNORED_CASES_STAGES)
        ), EntityOperator.AND);

        return delegator.findByConditionCache("StatusItem", conditions, UtilMisc.toList("statusId", "description"), UtilMisc.toList("sequenceId"));
	}

}
