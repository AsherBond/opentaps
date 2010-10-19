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
package com.opensourcestrategies.crmsfa.teams;

import java.util.*;

import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.util.UtilCommon;

/**
 * Team Helper methods which are designed to provide a consistent set of APIs that can be reused by
 * higher level services.
 */
public final class TeamHelper {

    private TeamHelper() { }

    private static final String MODULE = TeamHelper.class.getName();

    /** the possible security groups for a team member (basically the contents of SalesTeamRoleSecurity). */
    public static final List<String> TEAM_SECURITY_GROUPS = UtilMisc.toList("SALES_MANAGER", "SALES_REP", "SALES_REP_LIMITED", "CSR");

    /** Find all active PartyRelationships that relates a partyId to a team or account. */
    public static List findActiveAccountOrTeamRelationships(String accountTeamPartyId, String roleTypeIdFrom, String teamMemberPartyId, Delegator delegator) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, roleTypeIdFrom),
                    EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, accountTeamPartyId),
                    EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, teamMemberPartyId),
                    EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO"),
                    EntityUtil.getFilterByDateExpr());
        return delegator.findByCondition("PartyRelationship",  conditions, null, null);
    }

    /**
     * Get all active team members of a given Collection of team partyIds.  Returns a list of PartyToSummaryByRelationship.
     */
    public static List<GenericValue> getActiveTeamMembers(Collection<String> teamPartyIds, Delegator delegator) throws GenericEntityException {
        // this might happen if there are no teams set up yet
        if (UtilValidate.isEmpty(teamPartyIds)) {
            Debug.logWarning("No team partyIds set, so getActiveTeamMembers returns null", MODULE);
            return null;
        }

        EntityCondition orConditions = EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition("securityGroupId", EntityOperator.EQUALS, "SALES_MANAGER"),
                    EntityCondition.makeCondition("securityGroupId", EntityOperator.EQUALS, "SALES_REP"),
                    EntityCondition.makeCondition("securityGroupId", EntityOperator.EQUALS, "SALES_REP_LIMITED"),
                    EntityCondition.makeCondition("securityGroupId", EntityOperator.EQUALS, "CSR"));
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT_TEAM"),
                    EntityCondition.makeCondition("partyIdFrom", EntityOperator.IN, teamPartyIds),
                    EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO"),
                    orConditions,
                    // EntityCondition.makeCondition("securityGroupId", EntityOperator.IN, TEAM_SECURITY_GROUPS),  XXX TODO: found bug in mysql: this is not equivalent to using the or condition!
                    EntityUtil.getFilterByDateExpr());
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
    public static List<GenericValue> getActiveTeamMembers(String teamPartyId, Delegator delegator) throws GenericEntityException {
        return getActiveTeamMembers(Arrays.asList(teamPartyId), delegator);
    }

    /** Get all active team members of all active teams.  Returns a list of PartyToSummaryByRelationship. */
    public static List<GenericValue> getActiveTeamMembers(Delegator delegator) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                           EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "ACCOUNT_TEAM"),
                                           EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED"));
        List<GenericValue> teams = delegator.findByCondition("PartyRoleAndPartyDetail", conditions, Arrays.asList("partyId"), null);
        List<String> teamPartyIds = null;
        if (UtilValidate.isNotEmpty(teams)) {
            teamPartyIds = EntityUtil.getFieldListFromEntityList(teams, "partyId", true);
        }

        List<GenericValue> teamMembers = getActiveTeamMembers(teamPartyIds, delegator);
        return teamMembers;
    }

    /**
     * Get the team members (as a list of partyId Strings) that the partyId currently shares a team with.  This is accomplished by finding all
     * active team members of teams that the partyId belongs to.  No security is checked here, that is the responsibility of upstream code.
     */
    public static Collection<String> getTeamMembersForPartyId(String partyId, Delegator delegator) throws GenericEntityException {
        Collection<String> teamPartyIds = getTeamsForPartyId(partyId, delegator);
        if (teamPartyIds.size() == 0) {
            return teamPartyIds;
        }

        List<GenericValue> relationships = getActiveTeamMembers(teamPartyIds, delegator);
        Set<String> partyIds = new HashSet<String>();
        for (Iterator<GenericValue> iter = relationships.iterator(); iter.hasNext();) {
            GenericValue relationship = iter.next();
            partyIds.add(relationship.getString("partyId"));
        }
        return partyIds;
    }

    /**
     *  Get the teams (as a list of PartyRelationship.partyIdFrom) that the partyId currently belongs to.  A team relationship is defined with a
     *  PartyRelationship  where the partyIdFrom is the team Party, roleTypeIdFrom is ACCOUNT_TEAM, partyIdTo is input, partyRelationshipTypeId
     *  is ASSIGNED_TO, and securityGroupId is either SALES_REP or SALES_MANAGER.
     */
    public static Collection<String> getTeamsForPartyId(String partyId, Delegator delegator) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT_TEAM"),
                    EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId),
                    EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO"),
                    EntityCondition.makeCondition("securityGroupId", EntityOperator.IN, TEAM_SECURITY_GROUPS),
                    EntityUtil.getFilterByDateExpr());
        List<GenericValue> relationships = delegator.findByCondition("PartyRelationship", conditions, null, null, null, UtilCommon.DISTINCT_READ_OPTIONS);
        Set<String> partyIds = new HashSet<String>();
        for (Iterator<GenericValue> iter = relationships.iterator(); iter.hasNext();) {
            GenericValue relationship = iter.next();
            partyIds.add(relationship.getString("partyIdFrom"));
        }
        return partyIds;
    }

    /**
     * Gets the active team members in an organization.  A party must be assigned to a team in order for them to be part of this list.
     * TODO there is no actual team -> organization relationship yet.
     */
    public static List<GenericValue> getTeamMembersForOrganization(Delegator delegator) throws GenericEntityException {

        // first let's look up all the ACCOUNT_TEAMs in the system (TODO: this would be constrained to the organizationPartyId via PartyRelationship or such)
        Set<String> accountTeamPartyIds = FastSet.newInstance();
        List<GenericValue> accountTeamRoles = delegator.findByAndCache("PartyRole", UtilMisc.toMap("roleTypeId", "ACCOUNT_TEAM"));
        for (GenericValue role : accountTeamRoles) {
            accountTeamPartyIds.add(role.getString("partyId"));
        }

        // then get all members related to these teams
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT_TEAM"),
                    EntityCondition.makeCondition("partyIdFrom", EntityOperator.IN, accountTeamPartyIds),
                    EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO"),
                    EntityCondition.makeCondition("securityGroupId", EntityOperator.IN, TEAM_SECURITY_GROUPS),
                    EntityUtil.getFilterByDateExpr());
        return delegator.findByCondition("PartyToSummaryByRelationship", conditions, null, null, UtilMisc.toList("firstName", "lastName"), UtilCommon.READ_ONLY_OPTIONS);
    }
}
