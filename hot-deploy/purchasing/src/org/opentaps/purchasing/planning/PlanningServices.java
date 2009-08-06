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

package org.opentaps.purchasing.planning;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;


/**
 * Services specific to planning section of purchasing.
 *
 * @author Oleg Andreyev
 *
 */
public final class PlanningServices {

    private PlanningServices() { }

    private static final String MODULE = PlanningServices.class.getName();

    /**
     * Approve all requirements for all facilities of this organization.
     *
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map approveAllOpenRequirements(DispatchContext ctx, Map context) throws GenericEntityException {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String organizationPartyId = (String) context.get("organizationPartyId");

        try {
            List searchConditions = UtilMisc.toList(
                                    new EntityExpr("facilityId", EntityOperator.IN, UtilCommon.getOrgReceivingFacilityIds(organizationPartyId, delegator)),
                                    new EntityConditionList(
                                        UtilMisc.toList(new EntityExpr("statusId", EntityOperator.EQUALS, "REQ_CREATED"),
                                                        new EntityExpr("statusId", EntityOperator.EQUALS, "REQ_PROPOSED")),
                                       EntityOperator.OR));

            // this service is probably only called when there are lots of requirements, so best be safe and use a list iterator
            EntityListIterator eli = delegator.findListIteratorByCondition("Requirement", new EntityConditionList(searchConditions, EntityOperator.AND), UtilMisc.toList("requirementId"), UtilMisc.toList("requirementId"));
            GenericValue requirement = null;
            while ((requirement = (GenericValue) eli.next()) != null) {
                Map tmpResult = dispatcher.runSync("approveRequirement", UtilMisc.toMap("requirementId", requirement.getString("requirementId"), "userLogin", userLogin));
                if (ServiceUtil.isError(tmpResult)) {
                    return tmpResult;
                }
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        return ServiceUtil.returnSuccess();

    }

    /**
     * Method cancel (reject) all product requirements assigned to specific supplier.
     *
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    @SuppressWarnings("unchecked")
    public static Map cancelRequirementsApprovedByVendor(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        List requirements = FastList.newInstance();
        String partyId = (String) context.get("partyId");
        String requirementId = null;
        int canceledRequirements = 0;

        try {

            // Get list of requirementId with approved status and assigned to partyId
            List searchConditions = FastList.newInstance();
            searchConditions.add(new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
            searchConditions.add(new EntityExpr("roleTypeId", EntityOperator.EQUALS, "SUPPLIER"));
            searchConditions.add(new EntityExpr("requirementTypeId", EntityOperator.EQUALS, "PRODUCT_REQUIREMENT"));
            searchConditions.add(new EntityExpr("statusId", EntityOperator.EQUALS, "REQ_APPROVED"));

            List selectList = UtilMisc.toList("requirementId");

            requirements = delegator.findByCondition("RequirementAndRole", new EntityConditionList(searchConditions, EntityOperator.AND), selectList, null);

            List listReqIds = EntityUtil.getFieldListFromEntityList(requirements, "requirementId", true);
            canceledRequirements = listReqIds.size();
            Iterator iterator = listReqIds.iterator();

            while (iterator.hasNext()) {
                requirementId = (String) iterator.next();
                if (requirementId != null && requirementId.length() > 0) {
                    try {
                        dispatcher.runSync("purchasing.cancelRequirement", UtilMisc.toMap("requirementId", requirementId, "userLogin", userLogin));
                    } catch (GenericServiceException gse) {
                        // Log error and go to next requirement
                        canceledRequirements--;
                        UtilMessage.logServiceWarning("PurchError_RequirementNotUpdated", locale, MODULE);
                    }
                }
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "PurchError_RequirementFailToListBySupplier" + partyId, locale, MODULE);
        }

        return UtilMessage.createServiceSuccess("PurchSuccess_RequirementsBatchCancelSuccess", locale, UtilMisc.toMap("supplierName", PartyHelper.getPartyName(delegator, partyId, false), "numberOfRequirements", new Integer(canceledRequirements)));

    }
}
