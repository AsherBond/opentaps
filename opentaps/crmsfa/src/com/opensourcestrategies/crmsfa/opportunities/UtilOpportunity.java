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
/* Copyright (c) Open Source Strategies, Inc. */

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
 */
public final class UtilOpportunity {

    private UtilOpportunity() { }

    private static final String MODULE = UtilOpportunity.class.getName();

    /**
     * Helper method to get the principal account for an opportunity. This is a simplification of the
     * datamodel and should only be calld for non-critical uses. Returns null if no account was found,
     * which would be the case if there were a lead party Id instead.
     */
    public static String getOpportunityAccountPartyId(GenericValue opportunity) throws GenericEntityException {
        List<GenericValue> candidates = opportunity.getRelatedByAnd("SalesOpportunityRole", UtilMisc.toMap("roleTypeId", "ACCOUNT"));
        if (candidates.size() == 0) {
            return null;
        }
        // we have two out of three primary keys, so the result is guaranteed to be the one with our partyId
        GenericValue salesOpportunityRole = candidates.get(0);
        return salesOpportunityRole.getString("partyId");
    }

    /**
     * Helper method to get the principal lead for an opportunity. This is a simplification of the
     * datamodel and should only be calld for non-critical uses. Returns null if no lead was found,
     * which would be the case if there were an account party Id instead.
     */
    public static String getOpportunityLeadPartyId(GenericValue opportunity) throws GenericEntityException {
        List<GenericValue> candidates = opportunity.getRelatedByAnd("SalesOpportunityRole", UtilMisc.toMap("roleTypeId", "PROSPECT"));
        if (candidates.size() == 0) {
            return null;
        }
        // we have two out of three primary keys, so the result is guaranteed to be the one with our partyId
        GenericValue salesOpportunityRole = candidates.get(0);
        return salesOpportunityRole.getString("partyId");
    }

    /** Helper method to get the principal lead or account partyId of an opportunity. Use this to get one or the other. */
    public static String getOpportunityAccountOrLeadPartyId(GenericValue opportunity) throws GenericEntityException {
        List<GenericValue> candidates = opportunity.getRelatedByAnd("SalesOpportunityRole", UtilMisc.toMap("roleTypeId", "ACCOUNT"));
        if (candidates.size() > 0) {
            return candidates.get(0).getString("partyId");
        }
        candidates = opportunity.getRelatedByAnd("SalesOpportunityRole", UtilMisc.toMap("roleTypeId", "PROSPECT"));
        if (candidates.size() > 0) {
            return candidates.get(0).getString("partyId");
        }
        return null;
    }

    /**
     * Helper method to get all account party Id's for an opportunity. This is a more serious version of the above
     * for use in critical logic, such as security or in complex methods that should use the whole list from the beginning.
     */
    public static List<String> getOpportunityAccountPartyIds(Delegator delegator, String salesOpportunityId) throws GenericEntityException {
        return getOpportunityPartiesByRole(delegator, salesOpportunityId, "ACCOUNT");
    }

    /** Helper method to get all lead party Id's for an opportunity. See comments for getOpportunityAccountPartyIds(). */
    public static List<String> getOpportunityLeadPartyIds(Delegator delegator, String salesOpportunityId) throws GenericEntityException {
        return getOpportunityPartiesByRole(delegator, salesOpportunityId, "PROSPECT");
    }

    /** Helper method to get all contact party Id's for an opportunity.  */
    public static List<String> getOpportunityContactPartyIds(Delegator delegator, String salesOpportunityId) throws GenericEntityException {
        return getOpportunityPartiesByRole(delegator, salesOpportunityId, "CONTACT");
    }

    /** Helper method to get all party Id's of a given role for an opportunity. It's better to use one of the more specific methods above. */
    public static List<String> getOpportunityPartiesByRole(Delegator delegator, String salesOpportunityId, String roleTypeId) throws GenericEntityException {
        List<GenericValue> maps = delegator.findByAnd("SalesOpportunityRole", UtilMisc.toMap("roleTypeId", roleTypeId, "salesOpportunityId", salesOpportunityId));
        List<String> results = new ArrayList<String>();
        for (Iterator<GenericValue> iter = maps.iterator(); iter.hasNext();) {
            GenericValue map = iter.next();
            results.add(map.getString("partyId"));
        }
        return results;
    }

    /**
     * Helper method to make a sales opportunity history, which should be done whenever an opp is created, updated or deleted.
     * @return  The created SalesOpportunityHistory
     */
    public static GenericValue createSalesOpportunityHistory(GenericValue opportunity, Delegator delegator, Map context) throws GenericEntityException {
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
     * @param additionalConditions - if not null, this EntityCondition will be added as well
     * @param orderBy - List of fields to order results by, can be null
     * @param delegator
     * @return
     * @throws GenericEntityException
     */
    public static EntityListIterator getOpportunitiesForMyAccounts(String organizationPartyId, String internalPartyId, String customTimePeriodId,
            EntityCondition additionalConditions, List<String> orderBy, Delegator delegator)
        throws GenericEntityException {

        // build condition to get list of PROSPECT or ACCOUNT opportunities that the user is RESPONSIBLE_FOR
        List<EntityCondition> combinedConditions = UtilMisc.toList(
                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, internalPartyId),
                EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "RESPONSIBLE_FOR"),
                EntityCondition.makeCondition(EntityOperator.OR,
                        EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "PROSPECT"),
                        EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT")),
                EntityUtil.getFilterByDateExpr()); // filter out expired accounts

        return getOpportunitiesForPartyHelper(customTimePeriodId, combinedConditions, additionalConditions, orderBy, delegator);
    }

    /**
     * As getOpportunitiesForMyAccounts but gets all account opportunities for all teams the party belongs to.
     * Also includes lead opportunities that the internalPartyId is RESPONSIBLE_FOR.
     */
    public static EntityListIterator getOpportunitiesForMyTeams(String organizationPartyId, String internalPartyId, String customTimePeriodId,
            EntityCondition additionalConditions, List<String> orderBy, Delegator delegator)  throws GenericEntityException {

        // strategy: find all the accounts of the internalPartyId, then find all the opportunities of those accounts
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, internalPartyId),
                    EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList("ACCOUNT", "PROSPECT")),
                    EntityCondition.makeCondition(EntityOperator.OR,
                            EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "RESPONSIBLE_FOR"),
                            EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO")),
                    EntityUtil.getFilterByDateExpr());
        List<GenericValue> accounts = delegator.findByCondition("PartyRelationship", conditions, null, null);
        ArrayList<String> accountIds = new ArrayList<String>();
        for (Iterator<GenericValue> iter = accounts.iterator(); iter.hasNext();) {
            GenericValue account = iter.next();
            accountIds.add(account.getString("partyIdFrom"));
        }

        // if no accounts are found, then return a null
        if (accountIds.size() < 1) {
            return null;
        }

        // build the condition to find opportunitied belonging to these accounts
        List<EntityCondition> combinedConditions = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.IN, accountIds),
                EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList("ACCOUNT", "PROSPECT"))
                );

        return getOpportunitiesForPartyHelper(customTimePeriodId, combinedConditions, additionalConditions, orderBy, delegator);
    }

    /**
     * As getOpportunitiesForMyAccounts but returns Account and Lead opportunities that the internalPartyId is assigned to.
     * Note that this is a superset of getOpportunitiesForMyAccounts, which returns the opportunities that the internalPartyId
     * is directly responsible for.  Use this method to get all opportunities that the internalPartyId can see.
     */
    public static EntityListIterator getOpportunitiesForInternalParty(String organizationPartyId, String internalPartyId, String customTimePeriodId,
            EntityCondition additionalConditions, List<String> orderBy, Delegator delegator)
        throws GenericEntityException {

        // build condition to get list of ACCOUNT or PROSPECT opportunities for the supplied internal party
        List<EntityCondition> combinedConditions = UtilMisc.toList(
                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, internalPartyId),
                EntityCondition.makeCondition(EntityOperator.OR,
                        EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT"),
                        EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "PROSPECT")),
                EntityUtil.getFilterByDateExpr()); // filter out expired accounts

        return getOpportunitiesForPartyHelper(customTimePeriodId, combinedConditions, additionalConditions, orderBy, delegator);
    }

    /**
     * Returns all account and lead opportunities.
     */
    public static EntityListIterator getOpportunities(String organizationPartyId, String customTimePeriodId,
            EntityCondition additionalConditions, List<String> orderBy, Delegator delegator)
        throws GenericEntityException {
        // build condition to get list of ACCOUNT or PROSPECT opportunities for all the parties
        List<EntityCondition> combinedConditions = UtilMisc.toList(
                EntityCondition.makeCondition(EntityOperator.OR,
                        EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "PROSPECT"),
                        EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT")),
                EntityUtil.getFilterByDateExpr()); // filter out expired accounts

        return getOpportunitiesForPartyHelper(customTimePeriodId, combinedConditions, additionalConditions, orderBy, delegator);
    }


    private static EntityListIterator getOpportunitiesForPartyHelper(String customTimePeriodId, List<EntityCondition> combinedConditions,
            EntityCondition additionalConditions, List<String> orderBy, Delegator delegator) throws GenericEntityException {
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
        EntityCondition conditionList = EntityCondition.makeCondition(combinedConditions, EntityOperator.AND);

        // fields to select
        List<String> fields =  UtilMisc.toList("salesOpportunityId", "partyIdFrom", "opportunityName", "opportunityStageId", "estimatedAmount", "estimatedCloseDate");
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
    public static List getOpportunityTeamMembers(String salesOpportunityId, Delegator delegator) throws GenericEntityException {
        // At this point, it is sufficient to traverse the directly related primary account
        // We'll ignore accounts associated through related contacts for now.
        GenericValue opportunity = delegator.findByPrimaryKey("SalesOpportunity", UtilMisc.toMap("salesOpportunityId", salesOpportunityId));
        String accountPartyId = getOpportunityAccountPartyId(opportunity);

        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, accountPartyId),
                EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT"),
                EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO"),
                EntityUtil.getFilterByDateExpr());
        return delegator.findByConditionCache("PartyToSummaryByRelationship", conditions, null, null);
    }


}
