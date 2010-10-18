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
package org.opentaps.domain.activities;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentaps.base.entities.ActivityFact;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for Activity Fact to handle interaction of Activities-related domain with
 * the entity engine (database) and the service engine.
 */
public interface ActivityFactRepositoryInterface extends RepositoryInterface {

    /**
     * Set the target role type identifier
     *
     * @param roleTypeId target role type identifier
     */
    public void setTargetRoleTypeId(String roleTypeId);

    /**
     * Set the list of allowed target party identifiers
     *
     * @param partyIds list of allowed target party identifiers
     */
    public void setAllowedTargetPartyIds(Set<String> partyIds);

    /**
     * Set the date dimension
     *
     * @param dateDimId date dimension
     */
    public void setDateDimensionId(long dateDimId);

    /**
     * Set the team member party identifier
     * @param partyId team member party identifier
     */
    public void setTeamMemberPartyId(String partyId);

    /**
     * Set the target party identifier
     *
     * @param partyId target party identifier
     */
    public void setTargetPartyId(String partyId);

    /**
     * Set the team member role type identifier
     *
     * @param memberRoleTypeId team member role type identifier
     */
    public void setTeamMemeberRoleTypeId(String memberRoleTypeId);

    /**
     * Find the ActivityFact records
     *
     * @return the list of ActivityFact
     * @throws RepositoryException if an error occurs
     */
    public List<ActivityFact> findActivityFacts() throws RepositoryException;

    /**
     * Find the ActivityFact records grouped by team member or by lead
     *
     * @param groupedByField the team member or lead to find
     * @return the Map grouped ActivityFact records
     * @throws RepositoryException if an error occurs
     */
    public Map<String, List<ActivityFact>> findLeadsActivitiesGroupedBy(ActivityFact.Fields groupedByField) throws RepositoryException;

    /**
     * Create ActivityFact with target party id and member party id
     *
     * @param teamMemberPartyId the target party identifier
     * @param targetPartyId the target party identifier
     * @param teamMemberRoleTypeId the team member role type identifier
     * @param targetRoleTypeId the target role type identifier
     * @param activity the current activity
     * @throws RepositoryException if an error occurs
     */
    public void createActivityFact(String teamMemberPartyId, String targetPartyId, String teamMemberRoleTypeId, String targetRoleTypeId, Activity activity, int count) throws RepositoryException;

    /**
     * Get the count of the Email Activity
     *
     * @return a long count
     * @throws RepositoryException if an error occurs
     */
    public long getEmailActivityCount() throws RepositoryException;

    /**
     * Get the count of the Phone Call Activity
     *
     * @return a long count
     * @throws RepositoryException if an error occurs
     */
    public long getPhoneCallActivityCount() throws RepositoryException;

    /**
     * Get the count of the Visit Activity
     *
     * @return a long count
     * @throws RepositoryException if an error occurs
     */
    public long getVisitActivityCount() throws RepositoryException;

    /**
     * Get the count of the Other Activity
     *
     * @return a long count
     * @throws RepositoryException if an error occurs
     */
    public long getOtherActivityCount() throws RepositoryException;

    /**
     * Get the count of the Total Activity
     *
     * @return a long count
     * @throws RepositoryException if an error occurs
     */
    public long getTotalActivityCount() throws RepositoryException;

    /**
     * Transform data from Activity and list of Participants to ActivityFact entities.
     *
     * @param activity the current activity
     * @param participants list of Participants
     * @param count activity counter
     * @throws RepositoryException if an error occurs
     */
    public void transformToActivityFacts(Activity activity, List<Party> participants, int count) throws RepositoryException;
}
