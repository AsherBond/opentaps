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
package com.opensourcestrategies.activities;

import java.util.List;

import org.ofbiz.base.util.Debug;
import org.opentaps.base.entities.ActivityFact;
import org.opentaps.base.services.ActivitiesTransformToActivityFactsService;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.activities.Activity;
import org.opentaps.domain.activities.ActivityFactRepositoryInterface;
import org.opentaps.domain.activities.ActivityRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Do data warehouse operations for activities.
 */
public class ActivitiesDataWarehouseService extends DomainService {

    private static final String MODULE = ActivitiesDataWarehouseService.class.getName();
    private static final int COUNT = 1;

    private String workEffortId;

    /**
     * Sets the required input parameter for service {@link #transformToActivityFacts}.
     * @param workEffortId the ID of work effort
     */
    public void setWorkEffortId(String workEffortId) {
        this.workEffortId = workEffortId;
    }


    /**
     * Transform all Activities which are TASK_COMPLETED or EVENT_COMPLETED
     * into ActivityFact
     *
     * @throws ServiceException
     */
    public void transformAllActivities() throws ServiceException {
        ActivitiesTransformToActivityFactsService activitiesTransform = null;

        try {
            ActivityRepositoryInterface activityRepository = getDomainsDirectory().getActivitiesDomain().getActivityRepository();
            PartyRepositoryInterface patryRepository = getDomainsDirectory().getPartyDomain().getPartyRepository();

            // Remove all ActivityFacts
            List<ActivityFact> activityFacts = patryRepository.findAll(ActivityFact.class);
            patryRepository.remove(activityFacts);

            // Find all Activities which are TASK_COMPLETED or EVENT_COMPLETED
            List<Activity> activityList = activityRepository.getCompletedActivities();

            // Each found activity transform into ActivityFact entities.
            for(Activity activity : activityList) {
                String workEffortId = activity.getWorkEffortId();

                activitiesTransform = new ActivitiesTransformToActivityFactsService();
                activitiesTransform.setInWorkEffortId(workEffortId);
                activitiesTransform.setUser(getUser());
                activitiesTransform.runSync(infrastructure);
            }

        } catch (RepositoryException e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        }
    }

    /**
     * Transform data from Activity and list of Participants to ActivityFact entities.
     * Counter for adding activities always equals 1
     *
     * @throws ServiceException if an error occurs
     */
    public void transformToActivityFacts() throws ServiceException {
        try {
            ActivityRepositoryInterface activityRepository = getDomainsDirectory().getActivitiesDomain().getActivityRepository();
            ActivityFactRepositoryInterface activityFactRepository = getDomainsDirectory().getActivitiesDomain().getActivityFactRepository();

            // Get Activity
            Activity activity = activityRepository.getActivityById(workEffortId);
            List<Party> parties = activity.getParticipants();

            // Transform to ActivityFact with positive counter equals 1
            activityFactRepository.transformToActivityFacts(activity, parties, COUNT);
        } catch (RepositoryException ex) {
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex);
        } catch (EntityNotFoundException ex) {
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex);
        }
    }
}

