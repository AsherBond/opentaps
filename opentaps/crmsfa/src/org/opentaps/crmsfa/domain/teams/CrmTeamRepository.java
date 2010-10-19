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
package org.opentaps.crmsfa.domain.teams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.constants.PartyRelationshipTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.PartyRelationship;
import org.opentaps.base.entities.PartyRole;
import org.opentaps.base.entities.PartyRoleAndPartyDetail;
import org.opentaps.base.entities.PartyToSummaryByRole;
import org.opentaps.base.entities.SalesTeamRoleSecurity;
import org.opentaps.base.entities.SecurityGroup;
import org.opentaps.common.domain.party.PartyRepository;
import org.opentaps.domain.crmsfa.teams.CrmTeamRepositoryInterface;
import org.opentaps.domain.crmsfa.teams.Team;
import org.opentaps.domain.crmsfa.teams.TeamMember;
import org.opentaps.domain.crmsfa.teams.TeamMemberInTeam;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.util.EntityListIterator;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Repository for CRM team.
 */
public class CrmTeamRepository extends PartyRepository implements CrmTeamRepositoryInterface {

    private static final String MODULE = CrmTeamRepository.class.getName();

    /**
     * Default constructor.
     */
    public CrmTeamRepository() {
        super();
    }

    /**
     * Use this for domain Repositories.
     * @param infrastructure the domain infrastructure
     * @param user the domain user
     * @throws RepositoryException if an error occurs
     */
    public CrmTeamRepository(Infrastructure infrastructure, User user) throws RepositoryException {
        super(infrastructure, user);
    }

    /** {@inheritDoc} */
    public Team getTeamById(String teamPartyId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(teamPartyId)) {
            return null;
        }
        Team team = findOneNotNull(Team.class, map(Party.Fields.partyId, teamPartyId), "Team [" + teamPartyId + "] not found");

        // check it is actually a Team an not another type of party
        if (!isTeam(team)) {
            throw new EntityNotFoundException(Team.class, "Party [" + teamPartyId + "] is not a Team");
        }

        return team;
    }

    /** {@inheritDoc} */
    public TeamMember getTeamMemberById(String partyId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(partyId)) {
            return null;
        }
        return findOneNotNull(TeamMember.class, map(Party.Fields.partyId, partyId), "TeamMember [" + partyId + "] not found");
    }

    /**
     * Gets a <code>TeamMemberInTeam</code> by the given IDs.
     * @param relationship the <code>PartyRelationship</code> representing the member association to the team
     * @return the <code>TeamMemberInTeam</code> found
     * @throws RepositoryException if an error occurs
     */
    protected TeamMemberInTeam getTeamMemberInTeam(PartyRelationship relationship) throws RepositoryException {
        // note: member cannot be null here because of the FK on PartyRelationship
        TeamMemberInTeam member = findOne(TeamMemberInTeam.class, map(Party.Fields.partyId, relationship.getPartyIdTo()));
        member.setSecurityGroupId(relationship.getSecurityGroupId());
        member.setTeamPartyId(relationship.getPartyIdFrom());
        return member;
    }

    /** {@inheritDoc} */
    public TeamMemberInTeam getTeamMemberInTeam(String partyId, String teamPartyId) throws RepositoryException, EntityNotFoundException {
        PartyRelationship rel = Entity.getFirst(getTeamMemberPartyRelationships(partyId, teamPartyId));
        if (rel == null) {
            throw new EntityNotFoundException(TeamMemberInTeam.class, "TeamMember [" + partyId + "] not found in team [" + teamPartyId + "]");
        }
        return getTeamMemberInTeam(rel);
    }

    /** {@inheritDoc} */
    public Boolean isTeam(Party party) throws RepositoryException {
        return isTeam(party.getPartyId());
    }

    /** {@inheritDoc} */
    public Boolean isTeam(String partyId) throws RepositoryException {
        // check that the party has the ACCOUNT_TEAM role
        return findOneCache(PartyRole.class, map(PartyRole.Fields.partyId, partyId, PartyRole.Fields.roleTypeId, RoleTypeConstants.ACCOUNT_TEAM)) != null;
    }

    /** {@inheritDoc} */
    public List<TeamMemberInTeam> getTeamMembers(Team team) throws RepositoryException {
        return getTeamMembers(team.getPartyId());
    }

    /** {@inheritDoc} */
    public List<TeamMemberInTeam> getTeamMembers(String teamPartyId) throws RepositoryException {
        List<PartyRelationship> rels = getTeamMembersPartyRelationships(teamPartyId);
        Debug.logInfo("Got members relationships for team [" + teamPartyId + "] : " + rels, MODULE);
        List<TeamMemberInTeam> members = new ArrayList<TeamMemberInTeam>();
        for (PartyRelationship rel : rels) {
            members.add(getTeamMemberInTeam(rel));
        }
        Debug.logInfo("Got members for team [" + teamPartyId + "] : " + members, MODULE);
        return members;
    }

    /**
     * Gets the active <code>PartyRelationship</code> entities representing the given member association to the given team.
     * @param partyId the ID of the member
     * @param teamPartyId the ID of the team
     * @return the list of active <code>PartyRelationship</code>
     * @throws RepositoryException if an error occurs
     */
    protected List<PartyRelationship> getTeamMemberPartyRelationships(String partyId, String teamPartyId) throws RepositoryException {
        return findList(PartyRelationship.class, EntityCondition.makeCondition(
                                     EntityCondition.makeCondition(PartyRelationship.Fields.partyIdFrom.name(), teamPartyId),
                                     EntityCondition.makeCondition(PartyRelationship.Fields.roleTypeIdFrom.name(), RoleTypeConstants.ACCOUNT_TEAM),
                                     EntityCondition.makeCondition(PartyRelationship.Fields.partyIdTo.name(), partyId),
                                     EntityCondition.makeCondition(PartyRelationship.Fields.partyRelationshipTypeId.name(), PartyRelationshipTypeConstants.ASSIGNED_TO),
                                     EntityUtil.getFilterByDateExpr()));
    }

    /**
     * Gets the active <code>PartyRelationship</code> entities representing the members of the given team.
     * @param teamPartyId the ID of the team
     * @return the list of active <code>PartyRelationship</code>
     * @throws RepositoryException if an error occurs
     */
    protected List<PartyRelationship> getTeamMembersPartyRelationships(String teamPartyId) throws RepositoryException {
        return findList(PartyRelationship.class, EntityCondition.makeCondition(
                                     EntityCondition.makeCondition(PartyRelationship.Fields.partyIdFrom.name(), teamPartyId),
                                     EntityCondition.makeCondition(PartyRelationship.Fields.roleTypeIdFrom.name(), RoleTypeConstants.ACCOUNT_TEAM),
                                     EntityCondition.makeCondition(PartyRelationship.Fields.partyRelationshipTypeId.name(), PartyRelationshipTypeConstants.ASSIGNED_TO),
                                     EntityUtil.getFilterByDateExpr()),
                                Arrays.asList(PartyRelationship.Fields.partyIdTo.asc()));
    }

    /** {@inheritDoc} */
    public SecurityGroup getSecurityGroup(TeamMemberInTeam member) throws RepositoryException {
        return findOne(SecurityGroup.class, map(SecurityGroup.Fields.groupId, member.getSecurityGroupId()));
    }

    /** {@inheritDoc} */
    public SalesTeamRoleSecurity getSalesTeamRoleSecurity(TeamMemberInTeam member) throws RepositoryException {
        return findOne(SalesTeamRoleSecurity.class, map(SalesTeamRoleSecurity.Fields.securityGroupId, member.getSecurityGroupId()));
    }

    /** {@inheritDoc} */
    public EntityCondition makeLookupTeamsCondition(String teamName) throws RepositoryException {

        // build search conditions
        List<EntityCondition> search = new ArrayList<EntityCondition>();
        if (teamName != null) {
            search.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(PartyRoleAndPartyDetail.Fields.groupName.name()), EntityOperator.LIKE, EntityFunction.UPPER(teamName + "%")));
        }

        // required conditions
        search.add(EntityCondition.makeCondition(PartyRoleAndPartyDetail.Fields.roleTypeId.name(), RoleTypeConstants.ACCOUNT_TEAM));
        search.add(EntityCondition.makeCondition(PartyRoleAndPartyDetail.Fields.statusId.name(), EntityOperator.NOT_EQUAL, StatusItemConstants.PartyStatus.PARTY_DISABLED));

        return EntityCondition.makeCondition(search);
    }

    /** {@inheritDoc} */
    public EntityListIterator<PartyRoleAndPartyDetail> lookupTeams(String teamName) throws RepositoryException {

        return findIterator(PartyRoleAndPartyDetail.class, makeLookupTeamsCondition(teamName),
                            Arrays.asList(PartyRoleAndPartyDetail.Fields.partyId.name(),
                                          PartyRoleAndPartyDetail.Fields.groupName.name(),
                                          PartyRoleAndPartyDetail.Fields.partyGroupComments.name()),
                            Arrays.asList(PartyRoleAndPartyDetail.Fields.groupName.desc()));
    }

    /** {@inheritDoc} */
    public EntityCondition makeLookupTeamMembersCondition(String firstName, String lastName) throws RepositoryException {

        List<EntityCondition> conditions = new ArrayList<EntityCondition>();

        // construct role conditions
        conditions.add(EntityCondition.makeCondition(PartyToSummaryByRole.Fields.roleTypeId.name(), EntityOperator.IN, PartyHelper.TEAM_MEMBER_ROLES));

        // construct search conditions
        if (lastName != null) {
            conditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(PartyToSummaryByRole.Fields.lastName.name()),
                                                         EntityOperator.LIKE,
                                                         EntityFunction.UPPER("%" + lastName + "%")));
        }
        if (firstName != null) {
            conditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(PartyToSummaryByRole.Fields.firstName.name()),
                                                         EntityOperator.LIKE,
                                                         EntityFunction.UPPER("%" + firstName + "%")));
        }

        // remove disabled parties
        conditions.add(EntityCondition.makeCondition(EntityOperator.OR,
                                                     EntityCondition.makeCondition(PartyToSummaryByRole.Fields.statusId.name(),
                                                                                   EntityOperator.NOT_EQUAL,
                                                                                   StatusItemConstants.PartyStatus.PARTY_DISABLED),
                                                     EntityCondition.makeCondition(PartyToSummaryByRole.Fields.statusId.name(),
                                                                                   EntityOperator.EQUALS,
                                                                                   null)));

        return EntityCondition.makeCondition(conditions);
    }

    /** {@inheritDoc} */
    public EntityListIterator<PartyToSummaryByRole> lookupTeamMembers(String firstName, String lastName) throws RepositoryException {

        return findIterator(PartyToSummaryByRole.class, makeLookupTeamMembersCondition(firstName, lastName),
                            Arrays.asList(PartyToSummaryByRole.Fields.partyId.name(),
                                          PartyToSummaryByRole.Fields.firstName.name(),
                                          PartyToSummaryByRole.Fields.lastName.name()),
                            Arrays.asList(PartyToSummaryByRole.Fields.firstName.desc(),
                                          PartyToSummaryByRole.Fields.lastName.desc()));
    }
}
