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

import org.opentaps.base.entities.SalesTeamRoleSecurity;
import org.opentaps.base.entities.SecurityGroup;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * A <code>TeamMember</code> with attribute from a specific <code>Team</code>.
 */
public class TeamMemberInTeam extends TeamMember {

    private String teamPartyId;
    private String securityGroupId;

    private Team team;
    private SecurityGroup securityGroup;
    private SalesTeamRoleSecurity salesTeamRoleSecurity;

    /**
     * Sets the <code>Team</code> ID where the TeamMember is in.
     * @param teamPartyId a <code>String</code> value
     */
    public void setTeamPartyId(String teamPartyId) {
        this.teamPartyId = teamPartyId;
    }

    /**
     * Gets the <code>Team</code> ID where the TeamMember is in.
     * @return a <code>String</code> value
     */
    public String getTeamPartyId() {
        return teamPartyId;
    }

    /**
     * Sets the <code>SecurityGroup</code> ID of the TeamMember in the Team.
     * @param securityGroupId a <code>String</code> value
     */
    public void setSecurityGroupId(String securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    /**
     * Gets the <code>SecurityGroup</code> ID of the TeamMember in the Team.
     * @return a <code>String</code> value
     */
    public String getSecurityGroupId() {
        return securityGroupId;
    }

    /**
     * Gets the <code>Team</code> where the TeamMember is in.
     * @return a <code>Team</code> value
     * @exception RepositoryException if an error occurs
     * @exception EntityNotFoundException if an error occurs
     */
    public Team getTeam() throws RepositoryException, EntityNotFoundException {
        if (team == null) {
            team = getRepository().getTeamById(teamPartyId);
        }
        return team;
    }

    /**
     * Gets the <code>SecurityGroup</code> related to this member in the team.
     * @return a <code>SecurityGroup</code> value
     * @exception RepositoryException if an error occurs
     */
    public SecurityGroup getSecurityGroup() throws RepositoryException {
        if (securityGroup == null && securityGroupId != null) {
            securityGroup = getRepository().getSecurityGroup(this);
        }
        return securityGroup;
    }

    /**
     * Gets the <code>SalesTeamRoleSecurity</code> related to this member in the team.
     * @return a <code>SalesTeamRoleSecurity</code> value
     * @exception RepositoryException if an error occurs
     */
    public SalesTeamRoleSecurity getSalesTeamRoleSecurity() throws RepositoryException {
        if (salesTeamRoleSecurity == null && securityGroupId != null) {
            salesTeamRoleSecurity = getRepository().getSalesTeamRoleSecurity(this);
        }
        return salesTeamRoleSecurity;
    }
}
