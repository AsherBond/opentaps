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
/* Copyright (c) 2005-2006 Open Source Strategies, Inc. */

/*
 *  $Id:$
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.opensourcestrategies.crmsfa.activities;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Activity utility methods.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 488 $
 */
public class UtilActivity {

    public static final String module = UtilActivity.class.getName();

    // list the activity statuses in one place in case the model changes
    public static List ACT_STATUSES_PENDING = null;
    public static List ACT_STATUSES_COMPLETED = null;
    static {
        ACT_STATUSES_COMPLETED = UtilMisc.toList("TASK_COMPLETED", "TASK_CANCELLED", "TASK_REJECTED", "EVENT_COMPLETED", "EVENT_CANCELLED", "EVENT_REJECTED");
        ACT_STATUSES_PENDING = UtilMisc.toList("TASK_SCHEDULED", "TASK_CONFIRMED", "TASK_ON_HOLD", "EVENT_SCHEDULED", "EVENT_CONFIRMED", "EVENT_ON_HOLD");
        ACT_STATUSES_PENDING.add("TASK_STARTED");
        ACT_STATUSES_PENDING.add("EVENT_STARTED");
    }

    /**
     * gets all unexpired parties related to the work effort. The result is a list of WorkEffortPartyAssignments containing
     * the partyIds we need.
     */
    public static List getActivityParties(GenericDelegator delegator, String workEffortId, List partyRoles) throws GenericEntityException {
        // add each role type id (ACCOUNT, CONTACT, etc) to an OR condition list
        List roleCondList = new ArrayList();
        for (Iterator iter = partyRoles.iterator(); iter.hasNext(); ) {
            String roleTypeId = (String) iter.next();
            roleCondList.add(new EntityExpr("roleTypeId", EntityOperator.EQUALS, roleTypeId));
        }
        EntityConditionList roleEntityCondList = new EntityConditionList(roleCondList, EntityOperator.OR);

        // roleEntityCondList AND workEffortId = ${workEffortId} AND filterByDateExpr
        EntityConditionList mainCondList = new EntityConditionList(UtilMisc.toList(
                    roleEntityCondList,
                    new EntityExpr("workEffortId", EntityOperator.EQUALS, workEffortId), 
                    EntityUtil.getFilterByDateExpr()
                    ), EntityOperator.AND);

        EntityListIterator partiesIt = delegator.findListIteratorByCondition("WorkEffortPartyAssignment", mainCondList, null,
                null,
                null, // fields to order by (unimportant here)
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));
        List parties = partiesIt.getCompleteList();
        partiesIt.close();

        return parties;
    }

    /**
     * Gets owner party id of activity
     */
    public static GenericValue getActivityOwner(String workEffortId, GenericDelegator delegator) throws GenericEntityException {
        List<GenericValue> ownerParties = EntityUtil.filterByDate(getActivityParties(delegator, workEffortId, UtilMisc.toList("CAL_OWNER")));
        if (UtilValidate.isEmpty(ownerParties)) {
            Debug.logWarning("No owner parties found for activity [" + workEffortId + "]", module);
            return null;
        } else if (ownerParties.size() > 1) {
            Debug.logWarning("More than one owner party found for activity [" + workEffortId + "].  Only the first party will be returned, but the parties are " + EntityUtil.getFieldListFromEntityList(ownerParties, "partyId", false), module);
        }

        return EntityUtil.getFirst(ownerParties);

    }

    /**
     * Helper method to delete all the associations related to a work effort.
     * TODO: make this more intelligent so it doesn't delete if the association has not changed
     *       and then rename this to removeUpdatedAssociationsForWorkEffort
     */
    public static void removeAllAssociationsForWorkEffort(String workEffortId, GenericDelegator delegator) throws GenericEntityException {

        // delete any existing opportunity relationships
        List oldAssocs = delegator.findByAnd("SalesOpportunityWorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        for (Iterator iter = oldAssocs.iterator(); iter.hasNext(); ) {
            GenericValue old = (GenericValue) iter.next();
            // TODO: deleted by hand because we don't have a service yet
            old.remove(); 
        }

        // delete any existing case relationships
        oldAssocs = delegator.findByAnd("CustRequestWorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        for (Iterator iter = oldAssocs.iterator(); iter.hasNext(); ) {
            GenericValue old = (GenericValue) iter.next();
            // TODO: add permissions to deleteWorkEffortRequest and use that service, otherwise it's exactly identical to the following line
            old.remove(); 
        }

        // delete existing party relationships with roles like ACCOUNT, CONTACT, PROSPECT
        for (Iterator iter = PartyHelper.CLIENT_PARTY_ROLES.iterator(); iter.hasNext(); ) {
            oldAssocs = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortPartyAssignment",
                        UtilMisc.toMap("workEffortId", workEffortId, "roleTypeId", iter.next())));
            for (Iterator inner = oldAssocs.iterator(); inner.hasNext(); ) {
                GenericValue assoc = (GenericValue) inner.next();
                assoc.set("thruDate", UtilDateTime.nowTimestamp());
                assoc.store();
            }
        }
    }

    /**
     * Helper method to check if a userLogin has any TASK or EVENT conflicts between two timestamps.
     * It will search the WorkEffortAndPartyAssign view for all EVENT_SCHEDULED events
     * that the user marked as availabilityStatusId=WEPT_AV_BUSY or WEPT_AV_AWAY, and
     * statusId=PRTYASGN_ASSIGNED, and filter out those that fall outside the specified duration. 
     *
     * @param   workEffortId    if specified, filters out the work effort ID
     * @return  List of events/tasks that the user conflicts with
     */
    public static List getActivityConflicts(GenericValue userLogin, Timestamp start, Timestamp end, String workEffortId) throws GenericEntityException {
        GenericDelegator delegator = userLogin.getDelegator();
        List conditions = UtilMisc.toList(
                new EntityExpr("partyId", EntityOperator.EQUALS, userLogin.getString("partyId")),
                new EntityConditionList(
                    UtilMisc.toList(
                        new EntityExpr("currentStatusId", EntityOperator.EQUALS, "EVENT_SCHEDULED"),
                        new EntityExpr("currentStatusId", EntityOperator.EQUALS, "TASK_SCHEDULED")
                        ), EntityOperator.OR
                    ),
                new EntityExpr("statusId", EntityOperator.EQUALS, "PRTYASGN_ASSIGNED"),
                new EntityConditionList(
                    UtilMisc.toList(
                        new EntityExpr("availabilityStatusId", EntityOperator.EQUALS, "WEPA_AV_BUSY"),
                        new EntityExpr("availabilityStatusId", EntityOperator.EQUALS, "WEPA_AV_AWAY")
                        ), EntityOperator.OR
                    ),
                new EntityConditionList(
                    UtilMisc.toList(
                        EntityUtil.getFilterByDateExpr(start, "estimatedStartDate", "estimatedCompletionDate"),
                        EntityUtil.getFilterByDateExpr(end, "estimatedStartDate", "estimatedCompletionDate")
                        ), EntityOperator.OR
                    )
                );
        if (workEffortId != null) conditions.add(new EntityExpr("workEffortId", EntityOperator.NOT_EQUAL, workEffortId));
        EntityConditionList cond = new EntityConditionList(conditions, EntityOperator.AND);
        return delegator.findByCondition("WorkEffortAndPartyAssign", cond, UtilMisc.toList("workEffortId"), null);
    }

    /**
     * As above, but for all work efforts.
     */
    public static List getActivityConflicts(GenericValue userLogin, Timestamp start, Timestamp end) throws GenericEntityException {
        return getActivityConflicts(userLogin, start, end, null);
    }

    /**
     * Determine if the activity has a status which should be considered "finished" or "history" or "done with".
     * It is recommended to use this instead of checking ACT_STATUSES_COMPLETED directly or testing the statuses by hand
     */
    public static boolean activityIsInactive(GenericValue activity) {
        for (Iterator iter = ACT_STATUSES_COMPLETED.iterator(); iter.hasNext(); ) {
            if (iter.next().equals(activity.getString("currentStatusId"))) {
                return true;
            }
        }
        return false;
    }
    
    public static EntityConditionList getSecurityScopeCondition(GenericValue userLogin) {
        ArrayList nonPublicScopeExprList = new ArrayList();
        EntityConditionList nonPublicScopeCond = new EntityConditionList(UtilMisc.toList(
                            new EntityExpr( "scopeEnumId" , EntityOperator.EQUALS , "WES_PRIVATE" ),
                            new EntityExpr( "scopeEnumId" , EntityOperator.EQUALS , "WES_CONFIDENTIAL" )),
                            EntityOperator.OR);
        nonPublicScopeExprList.add(nonPublicScopeCond);
      
        // condition to check if user is an assignee for the activity
        EntityConditionList roleCond = new EntityConditionList(UtilMisc.toList(
                        new EntityExpr( "roleTypeId" , EntityOperator.IN , UtilMisc.toList("CAL_OWNER", "CAL_ATTENDEE",
                            "CAL_DELEGATE", "CAL_ORGANIZER")),
                        new EntityExpr( "partyId", EntityOperator.EQUALS, (String)userLogin.get("partyId"))),
                        EntityOperator.AND);
        nonPublicScopeExprList.add(roleCond);
        EntityConditionList nonPublicScopeCondition = new EntityConditionList(nonPublicScopeExprList, EntityOperator.AND);
        
        // ok to view activities with public or null security scope (scopeEnumId)
        ArrayList securityScopeExprList = new ArrayList();    
        securityScopeExprList.add(new EntityExpr( "scopeEnumId" , EntityOperator.EQUALS , null ));    
        securityScopeExprList.add(new EntityExpr( "scopeEnumId" , EntityOperator.EQUALS , "WES_PUBLIC" ));
        securityScopeExprList.add(nonPublicScopeCondition);
        
        // SELECT FROM WorkEffortAndPartyAssign WHERE ((scopeEnumId="PUBLIC" OR scopeEnumId=null) OR
        //      ((scopeEnumId="WES_PRIVATE" OR scopeEnumId="WES_CONFIDENTIAL") AND
        //      roleTypeId IN ("CAL_OWNER", "CAL_ATTENDEE", "CAL_DELEGATE", "CAL_ORGANIZER")AND partyId=userLogin.get("partyId")))
        EntityConditionList securityScopeMainCond = new EntityConditionList(securityScopeExprList, EntityOperator.OR);
        
    	return securityScopeMainCond;
    }
}
