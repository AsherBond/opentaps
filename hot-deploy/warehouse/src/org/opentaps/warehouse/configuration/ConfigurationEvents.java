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
package org.opentaps.warehouse.configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");

        String facilityId = request.getParameter("facilityId");
        if (UtilValidate.isEmpty(facilityId)) {
            facilityId = (String) request.getAttribute("facilityId");
            if (UtilValidate.isEmpty(facilityId)) {
                facilityId = (String) session.getAttribute("facilityId");
                if (UtilValidate.isEmpty(facilityId)) {
                    return "selectFacility";
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
        return "success";
    }
}
