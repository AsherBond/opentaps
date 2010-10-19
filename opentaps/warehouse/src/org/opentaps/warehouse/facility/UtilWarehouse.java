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

package org.opentaps.warehouse.facility;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilConfig;


/**
 * UtilWarehouse.
 */
public final class UtilWarehouse {

    private UtilWarehouse() { }

    private static String MODULE = UtilWarehouse.class.getName();

    /**
     * Copy of the bsh located at
     * component://product/webapp/facility/WEB-INF/actions/facility/FindFacilityTransfers.bsh.
     *
     * @param   facilityId        The Id of the warehouse to look on
     * @param   activeOnly        If true, get all the transfers which are not completed and not canceled, else get all the transfers
     * @param   completeRequested If true, get all the requested transfers
     * @param   toTransfer        If true, get the toTransfer list else get the fromTransfer list
     * @param   delegator         The delegator object to look up on
     * @return  The list of the transfers elements
     */
    public static List<GenericValue> findFacilityTransfer(String facilityId, boolean activeOnly, boolean completeRequested, boolean toTransfer, Delegator delegator) throws GenericEntityException {

        if (facilityId == null) {
            return null;
        }

        GenericValue facility = delegator.findByPrimaryKey("Facility", UtilMisc.toMap("facilityId", facilityId));
        if (facility == null) {
            return null;
        }

        if (toTransfer) {
            // get the 'to' this facility transfers
            EntityCondition exprsTo = null;
            if (activeOnly) {
                exprsTo = EntityCondition.makeCondition(EntityOperator.AND,
                               EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityId),
                               EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "IXF_COMPLETE"),
                               EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "IXF_CANCELLED"));
            } else {
                exprsTo = EntityCondition.makeCondition(EntityOperator.AND,
                               EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityId));
            }
            if (completeRequested) {
                exprsTo = EntityCondition.makeCondition(EntityOperator.AND,
                               EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityId),
                               EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "IXF_REQUESTED"));
            }
            return delegator.findList("InventoryTransfer", exprsTo, null, UtilMisc.toList("sendDate"), null, false);
        } else {
            // get the 'from' this facility transfers
            EntityCondition exprsFrom = null;
            if (activeOnly) {
                exprsFrom = EntityCondition.makeCondition(EntityOperator.AND,
                               EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                               EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "IXF_COMPLETE"),
                               EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "IXF_CANCELLED"));
            } else {
                exprsFrom = EntityCondition.makeCondition(EntityOperator.AND,
                               EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId));
            }
            if (completeRequested) {
                exprsFrom = EntityCondition.makeCondition(EntityOperator.AND,
                               EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                               EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "IXF_REQUESTED"));
            }
            return delegator.findList("InventoryTransfer", exprsFrom, null, UtilMisc.toList("sendDate"), null, false);
        }

    }

    /**
     * Convenience method to call findFacilityTransfer.
     *
     * @param   facilityId        The Id of the warehouse to look on
     * @param   delegator         The delegator object to look up on
     * @return  The list of the transfers elements
     */
    public static List<GenericValue> findFacilitytoTransfer(String facilityId, Delegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, false, false, true, delegator);
    }

    public static List<GenericValue> findFacilityfromTransfer(String facilityId, Delegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, false, false, false, delegator);
    }

    public static List<GenericValue> findFacilityActiveOnlytoTransfer(String facilityId, Delegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, true, false, true, delegator);
    }

    public static List<GenericValue> findFacilityActiveOnlyfromTransfer(String facilityId, Delegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, true, false, false, delegator);
    }

    public static List<GenericValue> findFacilityCompleteReqtoTransfer(String facilityId, Delegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, false, true, true, delegator);
    }

    public static List<GenericValue> findFacilityCompleteReqfromTransfer(String facilityId, Delegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, false, true, false, delegator);
    }

    /**
     * Get facility id from session taking into consideration user's preferences.
     */
    public static String getFacilityId(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (session == null) {
            return null;
        }

        Boolean applicationContextSet = (Boolean) session.getAttribute("applicationContextSet");
        if (applicationContextSet == null) {
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            try {
                String facilityId = UtilCommon.getUserLoginViewPreference(request, UtilConfig.SYSTEM_WIDE, UtilConfig.SET_FACILITY_FORM, UtilConfig.OPTION_DEF_FACILITY);
                if (UtilValidate.isNotEmpty(facilityId)) {
                    GenericValue facility = delegator.findByPrimaryKeyCache("Facility", UtilMisc.toMap("facilityId", facilityId));
                    if (facility != null) {
                        session.setAttribute("facility", facility);
                        session.setAttribute("facilityId", facilityId);
                        session.setAttribute("applicationContextSet", Boolean.TRUE);
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), MODULE);
            }
        }

        String facilityId = (String) session.getAttribute("facilityId");
        if (UtilValidate.isEmpty(facilityId)) {
            return null;
        }

        return facilityId;
    }
}
