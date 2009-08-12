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

package org.opentaps.warehouse.facility;

import java.util.List;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;


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
    public static List<GenericValue> findFacilityTransfer(String facilityId, boolean activeOnly, boolean completeRequested, boolean toTransfer, GenericDelegator delegator) throws GenericEntityException {

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
            return delegator.findByAnd("InventoryTransfer", exprsTo, UtilMisc.toList("sendDate"));
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
            return delegator.findByAnd("InventoryTransfer", exprsFrom, UtilMisc.toList("sendDate"));
        }

    }

    /**
     * Conveniance method to call findFacilityTransfer.
     *
     * @param   facilityId        The Id of the warehouse to look on
     * @param   delegator         The delegator object to look up on
     * @return  The list of the transfers elements
     */
    public static List<GenericValue> findFacilitytoTransfer(String facilityId, GenericDelegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, false, false, true, delegator);
    }

    public static List<GenericValue> findFacilityfromTransfer(String facilityId, GenericDelegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, false, false, false, delegator);
    }

    public static List<GenericValue> findFacilityActiveOnlytoTransfer(String facilityId, GenericDelegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, true, false, true, delegator);
    }

    public static List<GenericValue> findFacilityActiveOnlyfromTransfer(String facilityId, GenericDelegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, true, false, false, delegator);
    }

    public static List<GenericValue> findFacilityCompleteReqtoTransfer(String facilityId, GenericDelegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, false, true, true, delegator);
    }

    public static List<GenericValue> findFacilityCompleteReqfromTransfer(String facilityId, GenericDelegator delegator) throws GenericEntityException {
        return UtilWarehouse.findFacilityTransfer(facilityId, false, true, false, delegator);
    }

}
