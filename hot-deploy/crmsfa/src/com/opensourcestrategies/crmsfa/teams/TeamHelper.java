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
package com.opensourcestrategies.crmsfa.teams;

import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.util.UtilCommon;

import java.util.*;

/**
 * Team Helper methods which are designed to provide a consistent set of APIs that can be reused by 
 * higher level services.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */

public class TeamHelper {

    public static final String module = TeamHelper.class.getName();

    /** the possible security groups for a team member (basically the contents of SalesTeamRoleSecurity) */
    public static final List TEAM_SECURITY_GROUPS = UtilMisc.toList("SALES_MANAGER", "SALES_REP", "SALES_REP_LIMITED", "CSR");

    /** Find all active PartyRelationships that relates a partyId to a team or account. */
    public static List findActiveAccountOrTeamRelationships(String accountTeamPartyId, String roleTypeIdFrom, String teamMemberPartyId, GenericDelegator delegator) throws GenericEntityException {
            EntityCondition conditions = new EntityConditionList( UtilMisc.toList(
                    new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, roleTypeIdFrom),
                    new EntityExpr("partyIdFrom", EntityOperator.EQUALS, accountTeamPartyId),
                    new EntityExpr("partyIdTo", EntityOperator.EQUALS, teamMemberPartyId),
                    new EntityExpr("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO"),
                    EntityUtil.getFilterByDateExpr()
                    ), EntityOperator.AND);
            return delegator.findByCondition("PartyRelationship",  conditions, null, null);
    }

    /**
     * Get all active team members of a given Collection of team partyIds.  Returns a list of PartyToSummaryByRelationship.
     */
    public static List<GenericValue> getActiveTeamMembers(Collection<String> teamPartyIds, GenericDelegator delegator) throws GenericEntityException {
        // this might happen if there are no teams set up yet
        if (UtilValidate.isEmpty(teamPartyIds)) {
            Debug.logWarning("No team partyIds set, so getActiveTeamMembers returns null", module);
            return null;
        }

        EntityCondition orConditions =  new EntityConditionList( UtilMisc.toList(
                    new EntityExpr("securityGroupId", EntityOperator.EQUALS, "SALES_MANAGER"),
                    new EntityExpr("securityGroupId", EntityOperator.EQUALS, "SALES_REP"),
                    new EntityExpr("securityGroupId", EntityOperator.EQUALS, "SALES_REP_LIMITED"),
                    new EntityExpr("securityGroupId", EntityOperator.EQUALS, "CSR")
                    ), EntityOperator.OR);
        EntityCondition conditions = new EntityConditionList( UtilMisc.toList(
                    new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT_TEAM"),
                    new EntityExpr("partyIdFrom", EntityOperator.IN, teamPartyIds),
                    new EntityExpr("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO"),
                    orConditions,
                    // new EntityExpr("securityGroupId", EntityOperator.IN, TEAM_SECURITY_GROUPS),  XXX TODO: found bug in mysql: this is not equivalent to using the or condition!
                    EntityUtil.getFilterByDateExpr()
                    ), EntityOperator.AND);
        EntityListIterator teamMembersIterator = delegator.findListIteratorByCondition(
                "PartyToSummaryByRelationship", 
                conditions, 
                null, 
                Arrays.asList("partyId", "firstName", "lastName"), 
                Arrays.asList("firstName", "lastName"), 
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true)
                );
        List<GenericValue> resultList = teamMembersIterator.getCompleteList();
        teamMembersIterator.close();
        return resultList;
    }

    /** As above, but for one team. */
    public static List<GenericValue> getActiveTeamMembers(String teamPartyId, GenericDelegator delegator) throws GenericEntityException {
        return getActiveTeamMembers(Arrays.asList(teamPartyId), delegator);
    }

    /** Get all active team members of all active teams.  Returns a list of PartyToSummaryByRelationship. */
    public static List<GenericValue> getActiveTeamMembers(GenericDelegator delegator) throws GenericEntityException {
        List<EntityExpr> conditions = new ArrayList<EntityExpr>();
        conditions.add(new EntityExpr("roleTypeId", EntityOperator.EQUALS, "ACCOUNT_TEAM"));
        conditions.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED"));
        List<GenericValue> teams = delegator.findByCondition("PartyRoleAndPartyDetail", new EntityConditionList(conditions, EntityOperator.AND), Arrays.asList("partyId"), null);
        List<String> teamPartyIds = null;
        if (UtilValidate.isNotEmpty(teams)) 
            teamPartyIds = EntityUtil.getFieldListFromEntityList(teams, "partyId", true);
        
        List<GenericValue> teamMembers = getActiveTeamMembers(teamPartyIds, delegator);
        return teamMembers;
    }
    
    /**
     * Get the team members (as a list of partyId Strings) that the partyId currently shares a team with.  This is accomplished by finding all
     * active team members of teams that the partyId belongs to.  No security is checked here, that is the responsibility of upstream code.
     */
    public static Collection getTeamMembersForPartyId(String partyId, GenericDelegator delegator) throws GenericEntityException {
        Collection teamPartyIds = getTeamsForPartyId(partyId, delegator);
        if (teamPartyIds.size() == 0) return teamPartyIds;

        List relationships = getActiveTeamMembers(teamPartyIds, delegator);
        Set partyIds = new HashSet();
        for (Iterator iter = relationships.iterator(); iter.hasNext(); ) {
            GenericValue relationship = (GenericValue) iter.next();
            partyIds.add(relationship.get("partyId"));
        }
        return partyIds;
    }

    /**
     *  Get the teams (as a list of PartyRelationship.partyIdFrom) that the partyId currently belongs to.  A team relationship is defined with a 
     *  PartyRelationship  where the partyIdFrom is the team Party, roleTypeIdFrom is ACCOUNT_TEAM, partyIdTo is input, partyRelationshipTypeId 
     *  is ASSIGNED_TO, and securityGroupId is either SALES_REP or SALES_MANAGER.
     */
    public static Collection getTeamsForPartyId(String partyId, GenericDelegator delegator) throws GenericEntityException {
        EntityCondition conditions = new EntityConditionList( UtilMisc.toList(
                    new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT_TEAM"),
                    new EntityExpr("partyIdTo", EntityOperator.EQUALS, partyId),
                    new EntityExpr("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO"),
                    new EntityExpr("securityGroupId", EntityOperator.IN, TEAM_SECURITY_GROUPS),
                    EntityUtil.getFilterByDateExpr()
                    ), EntityOperator.AND);
        List relationships = delegator.findByCondition("PartyRelationship", conditions, null, null, null, UtilCommon.DISTINCT_READ_OPTIONS);
        Set partyIds = new HashSet();
        for (Iterator iter = relationships.iterator(); iter.hasNext(); ) {
            GenericValue relationship = (GenericValue) iter.next();
            partyIds.add(relationship.get("partyIdFrom"));
        }
        return partyIds;
    }

    /**
     * Gets the active team members in an organization.  A party must be assigned to a team in order for them to be part of this list.
     * TODO there is no actual team -> organization relationship yet.
     */
    public static List<GenericValue> getTeamMembersForOrganization(GenericDelegator delegator) throws GenericEntityException {

        // first let's look up all the ACCOUNT_TEAMs in the system (TODO: this would be constrained to the organizationPartyId via PartyRelationship or such)
        Set<String> accountTeamPartyIds = FastSet.newInstance();
        List<GenericValue> accountTeamRoles = delegator.findByAndCache("PartyRole", UtilMisc.toMap("roleTypeId", "ACCOUNT_TEAM"));
        for (GenericValue role : accountTeamRoles) {
            accountTeamPartyIds.add(role.getString("partyId"));
        }

        // then get all members related to these teams
        EntityCondition conditions = new EntityConditionList( UtilMisc.toList(
                    new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT_TEAM"),
                    new EntityExpr("partyIdFrom", EntityOperator.IN, accountTeamPartyIds),
                    new EntityExpr("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO"),
                    new EntityExpr("securityGroupId", EntityOperator.IN, TEAM_SECURITY_GROUPS),
                    EntityUtil.getFilterByDateExpr()
                    ), EntityOperator.AND);
        return delegator.findByCondition("PartyToSummaryByRelationship", conditions, null, null, UtilMisc.toList("firstName", "lastName"), UtilCommon.READ_ONLY_OPTIONS);
    }
}
