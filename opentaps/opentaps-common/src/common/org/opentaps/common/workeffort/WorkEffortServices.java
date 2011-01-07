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

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

import java.util.Map;

/**
 * WorkEffort services - Services for dealing with opentaps specific concepts of work efforts, such as activities.
 */
public final class WorkEffortServices {

    private WorkEffortServices() { }

    private static final String MODULE = WorkEffortServices.class.getName();

    public static Map<String, Object> markWorkEffortAsUpdated(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String workEffortId = (String) context.get("workEffortId");
        try {
            WorkEffortHelper.markAsUpdated(delegator, workEffortId);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, UtilCommon.getLocale(context), MODULE);
        }
        return ServiceUtil.returnSuccess();
    }
}
