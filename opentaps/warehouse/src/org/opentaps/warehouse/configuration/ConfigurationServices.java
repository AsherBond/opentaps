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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.warehouse.security.WarehouseSecurity;

/**
 * ConfigurationServices for configuration section.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public final class ConfigurationServices {

    private ConfigurationServices() { }

    private static final String MODULE = ConfigurationServices.class.getName();

    public static Map<String, Object> createFacilityManager(DispatchContext dctx, Map<String, Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String facilityId = (String) context.get("facilityId");
        Map<String, Object> input = UtilMisc.toMap("facilityId", facilityId, "partyId", userLogin.get("partyId"), "securityGroupId", "WRHS_MANAGER", "userLogin", userLogin, "locale", UtilCommon.getLocale(context));
        Map<String, Object> results = addFacilityTeamMember(dctx, input);
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("facilityId", facilityId);
        return results;
    }

    public static Map<String, Object> addFacilityTeamMember(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String facilityId = (String) context.get("facilityId");
        WarehouseSecurity warehouseSecurity = new WarehouseSecurity(security, userLogin, facilityId);
        if (!warehouseSecurity.hasFacilityPermission("WRHS_CONFIG")) {
            return ServiceUtil.returnError("Sorry, you do not have permission to perform this action."); // TODO use the UtilCommon error system
        }

        String partyId = (String) context.get("partyId");
        String securityGroupId = (String) context.get("securityGroupId");
        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            Map<String, Object> input = UtilMisc.toMap("facilityId", facilityId, "partyId", partyId, "fromDate", now, "securityGroupId", securityGroupId);
            GenericValue permission = delegator.makeValue("FacilityPartyPermission", input);
            permission.create();
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    public static Map<String, Object> removeFacilityTeamMember(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String facilityId = (String) context.get("facilityId");
        WarehouseSecurity warehouseSecurity = new WarehouseSecurity(security, userLogin, facilityId);
        if (!warehouseSecurity.hasFacilityPermission("WRHS_CONFIG")) {
            return ServiceUtil.returnError("Sorry, you do not have permission to perform this action."); // TODO use the UtilCommon error system
        }

        String partyId = (String) context.get("partyId");
        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            List<GenericValue> permissions = delegator.findByAnd("FacilityPartyPermission", UtilMisc.toMap("facilityId", facilityId, "partyId", partyId));
            for (GenericValue permission : permissions) {
                permission.set("thruDate", now);
                permission.store();
            }
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
}
