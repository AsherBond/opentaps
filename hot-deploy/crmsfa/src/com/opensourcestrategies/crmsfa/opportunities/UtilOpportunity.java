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
/* Copyright (c) 2005-2006 Open Source Strategies, Inc. */

/*
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
package com.opensourcestrategies.crmsfa.opportunities;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.common.period.PeriodWorker;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;

/**
 * Opportunity utility methods.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */

public class UtilOpportunity {

    public static final String module = UtilOpportunity.class.getName();

    /**
     * Helper method to get the principal account for an opportunity. This is a simplification of the
     * datamodel and should only be calld for non-critical uses. Returns null if no account was found,
     * which would be the case if there were a lead party Id instead.
     */
    public static String getOpportunityAccountPartyId(GenericValue opportunity) throws GenericEntityException {
        List candidates = opportunity.getRelatedByAnd("SalesOpportunityRole", UtilMisc.toMap("roleTypeId", "ACCOUNT"));
        if (candidates.size() == 0) return null;
        // we have two out of three primary keys, so the result is guaranteed to be the one with our partyId 
        GenericValue salesOpportunityRole = (GenericValue) candidates.get(0);
        return salesOpportunityRole.getString("partyId");
    }

    /**
     * Helper method to get the principal lead for an opportunity. This is a simplification of the
     * datamodel and should only be calld for non-critical uses. Returns null if no lead was found,
     * which would be the case if there were an account party Id instead.
     */
    public static String getOpportunityLeadPartyId(GenericValue opportunity) throws GenericEntityException {
        List candidates = opportunity.getRelatedByAnd("SalesOpportunityRole", UtilMisc.toMap("roleTypeId", "PROSPECT"));
        if (candidates.size() == 0) return null;
        // we have two out of three primary keys, so the result is guaranteed to be the one with our partyId 
        GenericValue salesOpportunityRole = (GenericValue) candidates.get(0);
        return salesOpportunityRole.getString("partyId");
    }

    /** Helper method to get the principal lead or account partyId of an opportunity. Use this to get one or the other. */
    public static String getOpportunityAccountOrLeadPartyId(GenericValue opportunity) throws GenericEntityException {
        List candidates = opportunity.getRelatedByAnd("SalesOpportunityRole", UtilMisc.toMap("roleTypeId", "ACCOUNT"));
        if (candidates.size() > 0) return ((GenericValue) candidates.get(0)).getString("partyId");
        candidates = opportunity.getRelatedByAnd("SalesOpportunityRole", UtilMisc.toMap("roleTypeId", "PROSPECT"));
        if (candidates.size() > 0) return ((GenericValue) candidates.get(0)).getString("partyId");
        return null;
    }

    /**
     * Helper method to get all account party Id's for an opportunity. This is a more serious version of the above
     * for use in critical logic, such as security or in complex methods that should use the whole list from the beginning.
     */
    public static List getOpportunityAccountPartyIds(GenericDelegator delegator, String salesOpportunityId) throws GenericEntityException {
        return getOpportunityPartiesByRole(delegator, salesOpportunityId, "ACCOUNT");
    }

    /** Helper method to get all lead party Id's for an opportunity. See comments for getOpportunityAccountPartyIds(). */
    public static List getOpportunityLeadPartyIds(GenericDelegator delegator, String salesOpportunityId) throws GenericEntityException {
        return getOpportunityPartiesByRole(delegator, salesOpportunityId, "PROSPECT");
    }

    /** Helper method to get all contact party Id's for an opportunity.  */
    public static List getOpportunityContactPartyIds(GenericDelegator delegator, String salesOpportunityId) throws GenericEntityException {
        return getOpportunityPartiesByRole(delegator, salesOpportunityId, "CONTACT");
    }

    /** Helper method to get all party Id's of a given role for an opportunity. It's better to use one of the more specific methods above. */
    public static List getOpportunityPartiesByRole(GenericDelegator delegator, String salesOpportunityId, String roleTypeId) throws GenericEntityException {
        List maps = delegator.findByAnd("SalesOpportunityRole", UtilMisc.toMap("roleTypeId", roleTypeId, "salesOpportunityId", salesOpportunityId));
        List results = new ArrayList();
        for (Iterator iter = maps.iterator(); iter.hasNext(); ) {
            GenericValue map = (GenericValue) iter.next();
            results.add(map.getString("partyId"));
        }
        return results;
    }

    /**
     * Helper method to make a sales opportunity history, which should be done whenever an opp is created, updated or deleted.
     * @return  The created SalesOpportunityHistory
     */
    public static GenericValue createSalesOpportunityHistory(GenericValue opportunity, GenericDelegator delegator, Map context) throws GenericEntityException {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String historyId = delegator.getNextSeqId("SalesOpportunityHistory");
        GenericValue history = delegator.makeValue("SalesOpportunityHistory", UtilMisc.toMap("salesOpportunityHistoryId", historyId));

        // we assume the opportunity has all fields set as desired already, especially the probability
        history.setNonPKFields(opportunity.getAllFields());
        history.set("changeNote", context.get("changeNote"));
        history.set("modifiedByUserLogin", userLogin.getString("userLoginId"));
        history.set("modifiedTimestamp", UtilDateTime.nowTimestamp());
        history.create();
        return history;
    }

    /**
     * Returns all account and lead opportunities for an internalPartyId. This is done by looking for all opportunities
     * belonging to accounts and leads that the internalPartyId is RESPONSIBLE_FOR.
     *
     * @param organizationPartyId - filter by organization TODO: not implemented
     * @param internalPartyId - lookup opportunities for this party
     * @param customTimePeriodId - if not null, will only get them for this time period
     * @param additionalConditions - if not null, this EntityConditionList will be added as well
     * @param orderBy - List of fields to order results by, can be null
     * @param delegator
     * @return
     * @throws GenericEntityException
     */
    public static EntityListIterator getOpportunitiesForMyAccounts(String organizationPartyId, String internalPartyId, String customTimePeriodId, 
            EntityConditionList additionalConditions, List orderBy, GenericDelegator delegator) 
        throws GenericEntityException {
        
        // build condition to get list of PROSPECT or ACCOUNT opportunities that the user is RESPONSIBLE_FOR
        List combinedConditions = UtilMisc.toList(
                new EntityExpr("partyIdTo", EntityOperator.EQUALS, internalPartyId), 
                new EntityExpr("partyRelationshipTypeId", EntityOperator.EQUALS, "RESPONSIBLE_FOR"),
                new EntityConditionList( UtilMisc.toList( 
                        new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "PROSPECT"), 
                        new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT")
                        ), EntityOperator.OR),
                EntityUtil.getFilterByDateExpr()); // filter out expired accounts

        return getOpportunitiesForPartyHelper(customTimePeriodId, combinedConditions, additionalConditions, orderBy, delegator);
    }

    /**
     * As getOpportunitiesForMyAccounts but gets all account opportunities for all teams the party belongs to.
     * Also includes lead opportunities that the internalPartyId is RESPONSIBLE_FOR.
     */
    public static EntityListIterator getOpportunitiesForMyTeams(String organizationPartyId, String internalPartyId, String customTimePeriodId, 
            EntityConditionList additionalConditions, List orderBy, GenericDelegator delegator)  throws GenericEntityException {

        // strategy: find all the accounts of the internalPartyId, then find all the opportunities of those accounts
        EntityConditionList conditions = new EntityConditionList( UtilMisc.toList(
                    new EntityExpr("partyIdTo", EntityOperator.EQUALS, internalPartyId),
                    new EntityExpr("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList("ACCOUNT", "PROSPECT")),
                    new EntityConditionList( UtilMisc.toList(
                            new EntityExpr("partyRelationshipTypeId", EntityOperator.EQUALS, "RESPONSIBLE_FOR"),
                            new EntityExpr("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO")
                            ), EntityOperator.OR),
                    EntityUtil.getFilterByDateExpr()
                    ), EntityOperator.AND);
        List accounts = delegator.findByCondition("PartyRelationship", conditions, null, null);
        ArrayList accountIds = new ArrayList();
        for (Iterator iter = accounts.iterator(); iter.hasNext(); ) {
            GenericValue account = (GenericValue) iter.next();
            accountIds.add(account.get("partyIdFrom"));
        }

        // if no accounts are found, then return a null
        if (accountIds.size() < 1) {
            return null;
        }
        
        // build the condition to find opportunitied belonging to these accounts
        List combinedConditions = UtilMisc.toList(
                new EntityExpr("partyIdFrom", EntityOperator.IN, accountIds), 
                new EntityExpr("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList("ACCOUNT", "PROSPECT"))
                );
        
        return getOpportunitiesForPartyHelper(customTimePeriodId, combinedConditions, additionalConditions, orderBy, delegator);
    }

    /**
     * As getOpportunitiesForMyAccounts but returns Account and Lead opportunities that the internalPartyId is assigned to.
     * Note that this is a superset of getOpportunitiesForMyAccounts, which returns the opportunities that the internalPartyId
     * is directly responsible for.  Use this method to get all opportunities that the internalPartyId can see.
     */
    public static EntityListIterator getOpportunitiesForInternalParty(String organizationPartyId, String internalPartyId, String customTimePeriodId, 
            EntityConditionList additionalConditions, List orderBy, GenericDelegator delegator) 
        throws GenericEntityException {
        
        // build condition to get list of ACCOUNT or PROSPECT opportunities for the supplied internal party
        List combinedConditions = UtilMisc.toList(
                new EntityExpr("partyIdTo", EntityOperator.EQUALS, internalPartyId), 
                new EntityConditionList( UtilMisc.toList(
                        new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT"), 
                        new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "PROSPECT")
                        ), EntityOperator.OR),
                EntityUtil.getFilterByDateExpr()); // filter out expired accounts

        return getOpportunitiesForPartyHelper(customTimePeriodId, combinedConditions, additionalConditions, orderBy, delegator);
    }

    /**
     * Returns all account and lead opportunities.
     */
    public static EntityListIterator getOpportunities(String organizationPartyId, String customTimePeriodId, 
            EntityConditionList additionalConditions, List orderBy, GenericDelegator delegator) 
        throws GenericEntityException {
        // build condition to get list of ACCOUNT or PROSPECT opportunities for all the parties         
        List combinedConditions = UtilMisc.toList(                
                new EntityConditionList( UtilMisc.toList( 
                        new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "PROSPECT"), 
                        new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT")
                        ), EntityOperator.OR),
                EntityUtil.getFilterByDateExpr()); // filter out expired accounts
 
        return getOpportunitiesForPartyHelper(customTimePeriodId, combinedConditions, additionalConditions, orderBy, delegator);
    }
    

    private static EntityListIterator getOpportunitiesForPartyHelper(String customTimePeriodId, List combinedConditions, 
            EntityConditionList additionalConditions, List orderBy, GenericDelegator delegator) throws GenericEntityException {
        // if a time period is supplied, use it as a condition as well
        if ((customTimePeriodId != null)) {
            GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
            if (timePeriod != null) {
                combinedConditions.add(PeriodWorker.getFilterByPeriodExpr("estimatedCloseDate", timePeriod));
            }
        }
        
        // if additional conditions are passed in, add them as well
        if (additionalConditions != null) {
            combinedConditions.add(additionalConditions);
        }
        EntityConditionList conditionList = new EntityConditionList(combinedConditions, EntityOperator.AND);

        // fields to select
        List fields =  UtilMisc.toList("salesOpportunityId", "partyIdFrom", "opportunityName", "opportunityStageId", "estimatedAmount", "estimatedCloseDate");
        fields.add("estimatedProbability");
        fields.add("currencyUomId");
 
        // get the SalesOpportunityAndRoles for these accounts
        EntityListIterator opportunities = delegator.findListIteratorByCondition("PartyRelationshipAndSalesOpportunity", conditionList, null, 
                fields,
                orderBy, // fields to order by (can't use fromDate here because it's part of multiple tables => need the alias.fromDate hack)
                // the first true here is for "specifyTypeAndConcur"
                // the second true is for a distinct select.  Apparently this is the only way the entity engine can do a distinct query
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));

        return opportunities;
    }

    /**
     * Gets a List of team members for a given opportunity.
     * @return  List of GenericValue PartyToSummaryByRelationship for team members
     */
    public static List getOpportunityTeamMembers(String salesOpportunityId, GenericDelegator delegator) throws GenericEntityException {
        // At this point, it is sufficient to traverse the directly related primary account
        // We'll ignore accounts associated through related contacts for now.
        GenericValue opportunity = delegator.findByPrimaryKey("SalesOpportunity", UtilMisc.toMap("salesOpportunityId", salesOpportunityId));
        String accountPartyId = getOpportunityAccountPartyId(opportunity);

        EntityConditionList conditions = new EntityConditionList(UtilMisc.toList(
                new EntityExpr("partyIdFrom", EntityOperator.EQUALS, accountPartyId), 
                new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT"), 
                new EntityExpr("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO"),
                EntityUtil.getFilterByDateExpr()
                ), EntityOperator.AND);
        return delegator.findByConditionCache("PartyToSummaryByRelationship", conditions, null, null);
    }


}
