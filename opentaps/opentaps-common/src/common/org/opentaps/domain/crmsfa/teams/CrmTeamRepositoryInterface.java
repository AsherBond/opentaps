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
package org.opentaps.domain.crmsfa.teams;

import java.util.List;

import org.ofbiz.entity.condition.EntityCondition;
//import org.opentaps.base.entities.PartyRelationship;
import org.opentaps.base.entities.PartyRoleAndPartyDetail;
import org.opentaps.base.entities.PartyToSummaryByRole;
import org.opentaps.base.entities.SalesTeamRoleSecurity;
import org.opentaps.base.entities.SecurityGroup;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.util.EntityListIterator;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Repository for CRM teams.
 */
public interface CrmTeamRepositoryInterface extends PartyRepositoryInterface {

    /**
     * Gets a <code>Team</code> by the given ID.
     * @param teamPartyId the team party ID
     * @return the <code>Team</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if the entity was not found or if it was not a team
     */
    public Team getTeamById(String teamPartyId) throws RepositoryException, EntityNotFoundException;

    /**
     * Gets a <code>TeamMember</code> by the given ID.
     * @param partyId the team member party ID
     * @return the <code>TeamMember</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if the entity was not found
     */
    public TeamMember getTeamMemberById(String partyId) throws RepositoryException, EntityNotFoundException;

    /**
     * Gets a <code>TeamMemberInTeam</code> by the given IDs.
     * @param partyId the team member party ID
     * @param teamPartyId the team party ID
     * @return the <code>TeamMemberInTeam</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if the party has no association to the given team
     */
    public TeamMemberInTeam getTeamMemberInTeam(String partyId, String teamPartyId) throws RepositoryException, EntityNotFoundException;

    /**
     * Checks if the given <code>Party</code> is a Team.
     * @param party the <code>Party</code> to check
     * @return <code>True</code> if the given party is a Team
     * @exception RepositoryException if an error occurs
     */
    public Boolean isTeam(Party party) throws RepositoryException;

    /**
     * Checks if the given <code>Party</code> is a Team.
     * @param partyId the ID of the <code>Party</code> to check
     * @return <code>True</code> if the given party is a Team
     * @exception RepositoryException if an error occurs
     */
    public Boolean isTeam(String partyId) throws RepositoryException;

    /**
     * Gets the list of <code>TeamMemberInTeam</code> for the given team.
     * @param team the team to get the members for
     * @return the list of <code>TeamMemberInTeam</code>
     * @throws RepositoryException if an error occurs
     */
    public List<TeamMemberInTeam> getTeamMembers(Team team) throws RepositoryException;

    /**
     * Gets the list of <code>TeamMemberInTeam</code> for the given team.
     * @param teamPartyId the ID of the team to get the members for
     * @return the list of <code>TeamMemberInTeam</code>
     * @throws RepositoryException if an error occurs
     */
    public List<TeamMemberInTeam> getTeamMembers(String teamPartyId) throws RepositoryException;

    /**
     * Gets the <code>SecurityGroup</code> for the given team member.
     * @param member a <code>TeamMemberInTeam</code> value
     * @return a <code>SecurityGroup</code> value
     * @exception RepositoryException if an error occurs
     */
    public SecurityGroup getSecurityGroup(TeamMemberInTeam member) throws RepositoryException;

    /**
     * Gets the <code>SalesTeamRoleSecurity</code> for the given team member.
     * @param member a <code>TeamMemberInTeam</code> value
     * @return a <code>SecurityGroup</code> value
     * @exception RepositoryException if an error occurs
     */
    public SalesTeamRoleSecurity getSalesTeamRoleSecurity(TeamMemberInTeam member) throws RepositoryException;

    /**
     * Make the <code>EntityCondition</code> to use for a team lookup.
     * This can be used instead of <code>lookupTeams</code> for doing a paginated lookup using <code>EntityListBuilder</code>.
     * @param teamName the name to lookup
     * @return an <code>EntityCondition</code>
     * @exception RepositoryException if an error occurs
     */
    public EntityCondition makeLookupTeamsCondition(String teamName) throws RepositoryException;

    /**
     * Perform a simple team lookup and returns an iterator of matching <code>PartyRoleAndPartyDetail</code>.
     * @param teamName the name to lookup
     * @return an <code>EntityListIterator</code> of <code>PartyRoleAndPartyDetail</code>
     * @exception RepositoryException if an error occurs
     */
    public EntityListIterator<PartyRoleAndPartyDetail> lookupTeams(String teamName) throws RepositoryException;

    /**
     * Make the <code>EntityCondition</code> to use for a team lookup.
     * This can be used instead of <code>lookupTeamMembers</code> for doing a paginated lookup using <code>EntityListBuilder</code>.
     * @param firstName the first name to lookup
     * @param lastName the last name to lookup
     * @return an <code>EntityCondition</code>
     * @exception RepositoryException if an error occurs
     */
    public EntityCondition makeLookupTeamMembersCondition(String firstName, String lastName) throws RepositoryException;

    /**
     * Perform a simple team members lookup and returns an iterator of matching <code>PartyToSummaryByRole</code>.
     * @param firstName the first name to lookup
     * @param lastName the last name to lookup
     * @return an <code>EntityListIterator</code> of <code>PartyToSummaryByRole</code>
     * @exception RepositoryException if an error occurs
     */
    public EntityListIterator<PartyToSummaryByRole> lookupTeamMembers(String firstName, String lastName) throws RepositoryException;
}
