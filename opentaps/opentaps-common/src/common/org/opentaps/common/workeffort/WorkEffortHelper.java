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

package org.opentaps.common.workeffort;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

public class WorkEffortHelper {

    public static final String module = WorkEffortHelper.class.getName();

    /**
     * Checks if a work effort has been updated since the last view.  The WorkEffortViewHistory table stores the time
     * of the last view.  When an update happens, the entries in WorkEffortViewHistory table are cleared.
     * Thus, if no last view exists, an update has occured.  
     */
    public static boolean isUpdatedSinceLastView(Delegator delegator, String workEffortId, String userLoginId) throws GenericEntityException {
        GenericValue history = delegator.findByPrimaryKey("WorkEffortViewHistory", UtilMisc.toMap("workEffortId", workEffortId, "userLoginId", userLoginId));
        return (history == null ? true : false);
    }

    /** As above, but argument is a work effort value or any entity that has workEffortId. */
    public static boolean isUpdatedSinceLastView(GenericValue workEffort, String userLoginId) throws GenericEntityException {
        Delegator delegator = workEffort.getDelegator();
        return isUpdatedSinceLastView(delegator, workEffort.getString("workEffortId"), userLoginId);
    }

    /**
     * Mark the work effort as updated for all users.  This is mainly used by the service of the same name.
     */
    public static void markAsUpdated(Delegator delegator, String workEffortId) throws GenericEntityException {
        delegator.removeByAnd("WorkEffortViewHistory", UtilMisc.toMap("workEffortId", workEffortId));
    }

    /**
     * Mark the work effort as viewed by the given user login.
     */
    public static void markAsViewed(GenericValue workEffort, String userLoginId) throws GenericEntityException {
        try {
            Delegator delegator = workEffort.getDelegator();
            Map input = UtilMisc.toMap("workEffortId", workEffort.get("workEffortId"), "userLoginId", userLoginId);
            GenericValue history = delegator.findByPrimaryKey("WorkEffortViewHistory", input);
            if (history != null) {
                history.set("viewedTimestamp", UtilDateTime.nowTimestamp());
                history.store();
            } else {
                history = delegator.makeValue("WorkEffortViewHistory", input);
                history.put("viewedTimestamp", UtilDateTime.nowTimestamp());
                history.create();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
    }
}
