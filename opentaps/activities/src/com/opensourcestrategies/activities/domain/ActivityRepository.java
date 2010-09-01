/*
 * Copyright (c) opentaps Group LLC
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
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.constants.WorkEffortPurposeTypeConstants;
import org.opentaps.base.entities.ActivityFact;
import org.opentaps.base.entities.WorkEffort;
import org.opentaps.base.entities.WorkEffortPartyAssignment;
import org.opentaps.common.reporting.etl.UtilEtl;
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
    public void createActivityFact(String teamMemberPartyId, String targetPartyId, String teamMemberRoleTypeId, String targetRoleTypeId, Activity activity) throws RepositoryException {
        ActivityFact activityFact = new ActivityFact();
        try {

            DomainsLoader domainLoader = new DomainsLoader(getInfrastructure(), getUser());
            PartyRepositoryInterface partyRepository = domainLoader.getDomainsDirectory().getPartyDomain().getPartyRepository();

            Long dateDimId = null;
            dateDimId = UtilEtl.lookupDateDimensionForTimestamp(UtilDateTime.nowTimestamp(), partyRepository.getInfrastructure().getDelegator());

            activityFact.setActivityFactId(partyRepository.getNextSeqId(activityFact));
            activityFact.setTargetPartyId(targetPartyId);
            activityFact.setTeamMemberPartyId(teamMemberPartyId);
            activityFact.setDateDimId(dateDimId);
            activityFact.setTargetPartyRoleTypeId(targetRoleTypeId);
            activityFact.setTeamMemberPartyRoleTypeId(teamMemberRoleTypeId);

            activityFact.setEmailActivityCount(Long.valueOf(0));
            activityFact.setPhoneCallActivityCount(Long.valueOf(0));
            activityFact.setVisitActivityCount(Long.valueOf(0));
            activityFact.setOtherActivityCount(Long.valueOf(0));


            // Increase count according to WorkEffort workEffortPurposeTypeId.
            String purpose = activity.getWorkEffortPurposeTypeId();

            if (purpose == null) {
                activityFact.setOtherActivityCount(Long.valueOf(1));
            } else if (purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_TASK_EMAIL) == 0) {
                activityFact.setEmailActivityCount(Long.valueOf(1));
            } else if (purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL) == 0) {
                activityFact.setPhoneCallActivityCount(Long.valueOf(1));
            } else if (purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_MEETING) == 0) {
                activityFact.setVisitActivityCount(Long.valueOf(1));
            } else {
                activityFact.setOtherActivityCount(Long.valueOf(1));
            }

            partyRepository.createOrUpdate(activityFact);

            Debug.logInfo("ActivityFact entity record [" + activityFact.getActivityFactId() + "] created/updated.", MODULE);

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new RepositoryException(e);
        }

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

            if (assignments.size() >= 2) {
                listParty =  new ArrayList<Party>();
                for (WorkEffortPartyAssignment assignment : assignments) {
                    Party party = partyRepository.getPartyById(assignment.getPartyId());
                    // Temporary store WorkEffortPartyAssignment Id into party Description
                    party.setDescription(assignment.getRoleTypeId());
                    listParty.add(party);
                }
            }
            else {
                Debug.logInfo("WorkEffort [" + workEffort.getWorkEffortId() + "] has only " + assignments.size() + " parties assigned, not generating an activity fact", MODULE);
            }
        }
        else {
            Debug.logInfo("WorkEffort [" + workEffort.getWorkEffortId() + "] is not completed, not generating an activity fact", MODULE);
        }

        return listParty;
    }
}
