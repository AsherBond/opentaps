/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.common.workeffort;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

public class WorkEffortHelper {

    public static final String module = WorkEffortHelper.class.getName();

    /**
     * Checks if a work effort has been updated since the last view.  The WorkEffortViewHistory table stores the time
     * of the last view.  When an update happens, the entries in WorkEffortViewHistory table are cleared.
     * Thus, if no last view exists, an update has occured.  
     */
    public static boolean isUpdatedSinceLastView(GenericDelegator delegator, String workEffortId, String userLoginId) throws GenericEntityException {
        GenericValue history = delegator.findByPrimaryKey("WorkEffortViewHistory", UtilMisc.toMap("workEffortId", workEffortId, "userLoginId", userLoginId));
        return (history == null ? true : false);
    }

    /** As above, but argument is a work effort value or any entity that has workEffortId. */
    public static boolean isUpdatedSinceLastView(GenericValue workEffort, String userLoginId) throws GenericEntityException {
        GenericDelegator delegator = workEffort.getDelegator();
        return isUpdatedSinceLastView(delegator, workEffort.getString("workEffortId"), userLoginId);
    }

    /**
     * Mark the work effort as updated for all users.  This is mainly used by the service of the same name.
     */
    public static void markAsUpdated(GenericDelegator delegator, String workEffortId) throws GenericEntityException {
        delegator.removeByAnd("WorkEffortViewHistory", UtilMisc.toMap("workEffortId", workEffortId));
    }

    /**
     * Mark the work effort as viewed by the given user login.
     */
    public static void markAsViewed(GenericValue workEffort, String userLoginId) throws GenericEntityException {
        try {
            GenericDelegator delegator = workEffort.getDelegator();
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
