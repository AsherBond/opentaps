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

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
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
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String organizationPartyId = (String) context.get("organizationPartyId");

        try {
            EntityCondition searchConditions = EntityCondition.makeCondition(EntityOperator.AND,
                                    EntityCondition.makeCondition("facilityId", EntityOperator.IN, UtilCommon.getOrgReceivingFacilityIds(organizationPartyId, delegator)),
                                    EntityCondition.makeCondition(EntityOperator.OR,
                                                                  EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_CREATED"),
                                                                  EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_PROPOSED")));

            // this service is probably only called when there are lots of requirements, so best be safe and use a list iterator
            EntityListIterator eli = delegator.findListIteratorByCondition("Requirement", searchConditions, UtilMisc.toList("requirementId"), UtilMisc.toList("requirementId"));
            GenericValue requirement = null;
            while ((requirement = eli.next()) != null) {
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
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        List requirements = FastList.newInstance();
        String partyId = (String) context.get("partyId");
        String requirementId = null;
        int canceledRequirements = 0;

        try {

            // Get list of requirementId with approved status and assigned to partyId
            List<EntityCondition> searchConditions = FastList.newInstance();
            searchConditions.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
            searchConditions.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "SUPPLIER"));
            searchConditions.add(EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "PRODUCT_REQUIREMENT"));
            searchConditions.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_APPROVED"));

            List selectList = UtilMisc.toList("requirementId");

            requirements = delegator.findByCondition("RequirementAndRole", EntityCondition.makeCondition(searchConditions, EntityOperator.AND), selectList, null);

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
