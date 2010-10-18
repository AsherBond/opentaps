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
package com.opensourcestrategies.activities.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.WorkEffort;
import org.opentaps.base.entities.WorkEffortPartyAssignment;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.activities.Activity;
import org.opentaps.domain.activities.ActivityRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

public class ActivityRepository extends Repository implements ActivityRepositoryInterface {

    private static final String MODULE = ActivityRepository.class.getName();

    /** {@inheritDoc} */
    public Activity getActivityById(String activityId) throws RepositoryException, EntityNotFoundException {
        if (UtilValidate.isEmpty(activityId)) {
            return null;
        }

        return findOneNotNull(Activity.class, map(Activity.Fields.workEffortId, activityId), "Activity [" + activityId + "] not found");
    }

    /** {@inheritDoc} */
    public List<Activity> getCompletedActivities() throws RepositoryException {
        DomainsLoader domainLoader = new DomainsLoader(getInfrastructure(), getUser());

        // Find all WorkEffort which are TASK_COMPLETED or EVENT_COMPLETED
        EntityCondition workEffortCond = EntityCondition.makeCondition(EntityOperator.OR,
             EntityCondition.makeCondition(WorkEffort.Fields.currentStatusId.name(), EntityOperator.EQUALS, StatusItemConstants.TaskStatus.TASK_COMPLETED),
             EntityCondition.makeCondition(WorkEffort.Fields.currentStatusId.name(), EntityOperator.EQUALS, StatusItemConstants.EventStatus.EVENT_COMPLETED)
        );

        List<Activity> activityList = domainLoader.getDomainsDirectory().getPartyDomain().getPartyRepository().findList(Activity.class, workEffortCond);

        return activityList;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("null")
    public List<Party> getParticipants(String workEffortId) throws RepositoryException, EntityNotFoundException {
        List<Party> listParty = null;
        DomainsLoader domainLoader = new DomainsLoader(getInfrastructure(), getUser());
        PartyRepositoryInterface partyRepository = domainLoader.getDomainsDirectory().getPartyDomain().getPartyRepository();

        // Get WorkEffortPartyAssign and WorkEffort data by workEffortId.
        WorkEffort workEffort = partyRepository.findOne(WorkEffort.class, partyRepository.map(WorkEffort.Fields.workEffortId, workEffortId));

        // Pass only completed workEfforts to do the transformation.
        if (Arrays.asList(StatusItemConstants.TaskStatus.TASK_COMPLETED, StatusItemConstants.EventStatus.EVENT_COMPLETED).contains(workEffort.getCurrentStatusId())) {

            List<WorkEffortPartyAssignment> assignments = partyRepository.findList(WorkEffortPartyAssignment.class, partyRepository.map(WorkEffortPartyAssignment.Fields.workEffortId, workEffortId));

            listParty =  new ArrayList<Party>();
            for (WorkEffortPartyAssignment assignment : assignments) {
                Party party = partyRepository.getPartyById(assignment.getPartyId());
                // Temporary store WorkEffortPartyAssignment Id into party Description
                party.setDescription(assignment.getRoleTypeId());
                listParty.add(party);
            }
        }
        else {
            Debug.logInfo("WorkEffort [" + workEffort.getWorkEffortId() + "] is not completed, not generating an activity fact", MODULE);
        }

        return listParty;
    }
}
