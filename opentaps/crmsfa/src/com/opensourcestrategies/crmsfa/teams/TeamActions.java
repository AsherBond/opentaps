/*
 * Copyright (c) 2010 - 2010 Open Source Strategies, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.PartyToSummaryByRole;
import org.opentaps.base.entities.SalesTeamRoleSecurity;
import org.opentaps.common.builder.EntityListBuilder;
import org.opentaps.common.builder.PageBuilder;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.crmsfa.teams.CrmTeamRepositoryInterface;
import org.opentaps.domain.crmsfa.teams.Team;
import org.opentaps.foundation.action.ActionContext;
import org.opentaps.foundation.infrastructure.DomainContextInterface;
import org.opentaps.foundation.repository.RepositoryException;


/**
 * TeamActions - Java Actions for leadmgmt teams.
 */
public final class TeamActions {

    private TeamActions() { }

    private static CrmTeamRepositoryInterface getRepository(DomainContextInterface context) throws RepositoryException {
        return DomainsDirectory.getDomainsDirectory(context).getCrmTeamDomain().getCrmTeamRepository();
    }

    /**
     * Action for the view team screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void viewTeam(Map<String, Object> context) throws GeneralException {

        final ActionContext ac = new ActionContext(context);

        String partyId = ac.getParameter("partyId");

        CrmTeamRepositoryInterface repository = getRepository(ac);

        // get the team, this also checks that the party is a valid team
        // switch the validView to true after the team is loaded successfully
        ac.put("validView", false);
        Team team = repository.getTeamById(partyId);
        ac.put("validView", true);

        ac.put("team", team);

        // check if deactivated
        if (StatusItemConstants.PartyStatus.PARTY_DISABLED.equals(team.getStatusId())) {
            ac.put("validView", true);
            ac.put("teamDeactivated", true);
            return;
        }

        // for updating the team members role
        Boolean hasTeamUpdatePermission = (Boolean) ac.get("hasTeamUpdatePermission");
        if (hasTeamUpdatePermission) {
            ac.put("salesTeamRoleSecurities", repository.findAllCache(SalesTeamRoleSecurity.class, Arrays.asList(SalesTeamRoleSecurity.Fields.sequenceNum.desc())));
        }
    }

    /**
     * Action to get the permissions related to the current team.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void getTeamPermissions(Map<String, Object> context) throws GeneralException {

        final ActionContext ac = new ActionContext(context);

        // get the general team permissions
        ac.put("hasTeamCreatePermission", ac.hasPermission("CRMSFA_TEAM_CREATE"));

        // get the team specific permissions
        String partyId = ac.getParameter("partyId");
        if (UtilValidate.isNotEmpty(partyId)) {

            // permission to change team member roles
            boolean hasTeamUpdatePermission = false; // this needs to be set so that a form-widget can test it with "use-when"
            if (CrmsfaSecurity.hasPartyRelationSecurity(ac.getSecurity(), "CRMSFA_TEAM", "_UPDATE", ac.getUserLogin(), partyId)) {
                hasTeamUpdatePermission = true;
            }
            ac.put("hasTeamUpdatePermission", hasTeamUpdatePermission);

            // permission to remove team members
            if (CrmsfaSecurity.hasPartyRelationSecurity(ac.getSecurity(), "CRMSFA_TEAM", "_REMOVE", ac.getUserLogin(), partyId)) {
                ac.put("hasTeamRemovePermission", true);
            }

            // permission to assign team members
            if (CrmsfaSecurity.hasPartyRelationSecurity(ac.getSecurity(), "CRMSFA_TEAM", "_ASSIGN", ac.getUserLogin(), partyId)) {
                ac.put("hasTeamAssignPermission", true);
            }

            // permission to deactivate the team
            if (CrmsfaSecurity.hasPartyRelationSecurity(ac.getSecurity(), "CRMSFA_TEAM", "_DEACTIVATE", ac.getUserLogin(), partyId)) {
                ac.put("hasTeamDeactivatePermission", true);
            }
        }
    }

    /**
     * Action for the find / list teams screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void findTeams(Map<String, Object> context) throws GeneralException {

        final ActionContext ac = new ActionContext(context);

        // get the search parameters
        String groupName = ac.getParameter("groupName");

        // perform the search
        ac.put("teams", getRepository(ac).lookupTeams(groupName));
    }

    /**
     * Action for the team member lookup.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void lookupTeamMembers(Map<String, Object> context) throws GeneralException {

        final ActionContext ac = new ActionContext(context);

        CrmTeamRepositoryInterface repository = getRepository(ac);

        // get the search parameters
        String lastName = ac.getParameter("lastName");
        String firstName = ac.getParameter("firstName");

        List<EntityCondition> conditions = new ArrayList<EntityCondition>();

        // construct role conditions
        conditions.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, PartyHelper.TEAM_MEMBER_ROLES));

        // construct search conditions
        if (lastName != null) {
            conditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("lastName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + lastName + "%")));
        }
        if (firstName != null) {
            conditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("firstName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + firstName + "%")));
        }

        // remove disabled parties
        conditions.add(EntityCondition.makeCondition(UtilMisc.toList(
                                     EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, StatusItemConstants.PartyStatus.PARTY_DISABLED),
                                     EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null))
                            , EntityOperator.OR));

        // use a page builder to fetch the crm party list, because we need some extra data
        EntityListBuilder teamMemberListBuilder = new EntityListBuilder(repository, PartyToSummaryByRole.class, EntityCondition.makeCondition(conditions), Arrays.asList("firstName", "lastName", "partyId"), UtilMisc.toList("lastName", "firstName"));
        PageBuilder teamMemberListPageBuilder = new PageBuilder<PartyToSummaryByRole>() {
            public List<Map<String, Object>> build(List<PartyToSummaryByRole> page) throws Exception {
                GenericDelegator delegator = ac.getDelegator();
                List<Map<String, Object>> newPage = new ArrayList<Map<String, Object>>();
                for (PartyToSummaryByRole member : page) {
                    Map<String, Object> newRow = new HashMap<String, Object>();
                    // TODO: check view permission for this party first
                    newRow.putAll(member.toMap());
                    newPage.add(newRow);
                }
                return newPage;
            }
        };
        teamMemberListBuilder.setPageBuilder(teamMemberListPageBuilder);
        context.put("crmPartyListBuilder", teamMemberListBuilder);


        // this debugging code is kind of helpful so I'll keep it around for now 

        /*
          listIt = context.get(listIteratorNameToUse);
          print("******* list iterator values: ***********");
          if (listIt != null) { while ((next = listIt.next()) != null) { print(next); } }
          else { print("No list iterator found"); }
          print("*****************************************");
        */

        // perform the search
        //        ac.put("teams", getRepository(ac).lookupTeamMembers(groupName));
    }
}
