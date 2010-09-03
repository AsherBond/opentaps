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

import java.util.List;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.WorkEffort;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.activities.Activity;
import org.opentaps.domain.activities.ActivityRepositoryInterface;
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
}
