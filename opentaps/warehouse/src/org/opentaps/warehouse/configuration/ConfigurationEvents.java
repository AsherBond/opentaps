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
package org.opentaps.warehouse.configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilConfig;

/**
 * ConfigurationEvents for configuration section.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public final class ConfigurationEvents {

    private ConfigurationEvents() { }

    private static final String MODULE = ConfigurationEvents.class.getName();

    /**
     * Sets the facilityId in the session.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response string, "success" if a facility is set, else "selectFacility"
     */
    public static String setFacility(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String facilityId = request.getParameter("facilityId");
        if (UtilValidate.isEmpty(facilityId)) {
            facilityId = (String) request.getAttribute("facilityId");
            if (UtilValidate.isEmpty(facilityId)) {
                facilityId = (String) session.getAttribute("facilityId");
                if (UtilValidate.isEmpty(facilityId)) {
                    try {
                        facilityId = UtilCommon.getUserLoginViewPreference(request, UtilConfig.SYSTEM_WIDE, UtilConfig.SET_FACILITY_FORM, UtilConfig.OPTION_DEF_FACILITY);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error while retrieve default facility", MODULE);
                        return "selectFacility";
                    }

                    if (UtilValidate.isEmpty(facilityId)) {
                        return "selectFacility";
                    }
                }
            }
        }

        GenericValue facility = null;
        try {
            facility = delegator.findByPrimaryKeyCache("Facility", UtilMisc.toMap("facilityId", facilityId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Could not get the facility.", MODULE);
        }

        if (facility == null) {
            return "selectFacility";
        }

        session.setAttribute("facility", facility);
        session.setAttribute("facilityId", facilityId);
        session.setAttribute("applicationContextSet", Boolean.TRUE);

        try {
            UtilCommon.setUserLoginViewPreference(request, UtilConfig.SYSTEM_WIDE, UtilConfig.SET_FACILITY_FORM, UtilConfig.OPTION_DEF_FACILITY, facilityId);
        } catch (GenericEntityException e) {
            // log message and go ahead, application may work w/o default value
            Debug.logWarning(e.getMessage(), MODULE);
        }

        return "success";
    }
}
