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

import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Domain class for CRMSFA Activity.
 */
public class Activity extends org.opentaps.base.entities.WorkEffort {

    /**
     * Default constructor.
     */
    public Activity() {
        super();
    }

    /**
     * Finds the Participants for this Activity.
     *
     * @return the list of Party
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    public List<Party> getParticipants() throws EntityNotFoundException, RepositoryException {
        return  getRepository().getParticipants(this.getWorkEffortId());
    }

    protected ActivityRepositoryInterface getRepository() throws RepositoryException {
        try {
            return ActivityRepositoryInterface.class.cast(repository);
        } catch (ClassCastException e) {
            repository = (RepositoryInterface) DomainsDirectory.getDomainsDirectory(repository).getActivitiesDomain().getActivityRepository();
            return ActivityRepositoryInterface.class.cast(repository);
        }
    }
}
