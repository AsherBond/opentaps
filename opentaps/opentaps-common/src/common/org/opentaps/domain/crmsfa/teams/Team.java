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

import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * A special <code>Party</code> representing a Team.
 */
public class Team extends Party {

    private List<TeamMemberInTeam> members;

    /**
     * Default constructor.
     */
    public Team() {
        super();
    }

    /**
     * Gets the list of <code>TeamMemberInTeam</code> for of this team.
     * @return the list of <code>TeamMemberInTeam</code> for this team
     * @throws RepositoryException if an error occurs
     */
    public List<TeamMemberInTeam> getMembers() throws RepositoryException {
        if (members == null) {
            members = getRepository().getTeamMembers(this);
        }
        return members;
    }

    @Override
    protected CrmTeamRepositoryInterface getRepository() throws RepositoryException {
        try {
            return CrmTeamRepositoryInterface.class.cast(repository);
        } catch (ClassCastException e) {
            repository = DomainsDirectory.getDomainsDirectory(repository).getCrmTeamDomain().getCrmTeamRepository();
            return CrmTeamRepositoryInterface.class.cast(repository);
        }
    }
}
