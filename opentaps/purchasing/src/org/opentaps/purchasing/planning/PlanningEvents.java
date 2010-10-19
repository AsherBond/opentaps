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

package org.opentaps.purchasing.planning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;


/**
 * Events specific to planning section of purchasing.
 */
public final class PlanningEvents {

    private PlanningEvents() { }

    private static final String MODULE = PlanningEvents.class.getName();

    /**
     * Event method that creates Production Runs from a list of Pending Internal Requirements.
     * This actually prepare the parameters and call the <code>createProductionRunsFromPendingInternalRequirements</code> service.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String createProductionRunsFromPendingInternalRequirements(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Locale locale = UtilHttp.getLocale(request);
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        // transform the multi form data into a set of iteratable maps
        Collection data = UtilHttp.parseMultiFormData(UtilHttp.getParameterMap(request));

        // each map corresponds to one line with requirementId, facilityId and routingId
        // then prepare the parameters for the createProductionRunsFromPendingInternalRequirements service
        List<String> requirementIds = new ArrayList<String>();
        Map<String, String> facilities = new HashMap<String, String>();
        Map<String, String> routings = new HashMap<String, String>();
        try {
            for (Iterator iter = data.iterator(); iter.hasNext();) {
                Map options = (Map) iter.next();
                String requirementId = (String) options.get("requirementId");
                requirementIds.add(requirementId);
                String facilityId = (String) options.get("facilityId");
                if (UtilValidate.isNotEmpty(facilityId)) {
                    facilities.put(requirementId, facilityId);
                }
                String routingId = (String) options.get("routingId");
                if (UtilValidate.isNotEmpty(routingId)) {
                    routings.put(requirementId, routingId);
                }
            }

            // call the service
            Map results = dispatcher.runSync("createProductionRunsFromPendingInternalRequirements", UtilMisc.toMap("userLogin", userLogin, "requirementIds", requirementIds, "facilityIds", facilities, "routingIds", routings));
            if (!UtilCommon.isSuccess(results)) {
                return UtilMessage.createAndLogEventError(request, results, locale, MODULE);
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return "success";
    }
}
