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

import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.sql.Timestamp;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.security.Security;
import org.opentaps.warehouse.security.WarehouseSecurity;

/**
 * ConfigurationServices for configuration section.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public class ConfigurationServices {

    public static final String module = ConfigurationServices.class.getName();

    public static Map createFacilityManager(DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String facilityId = (String) context.get("facilityId");
        String partyId = userLogin.getString("partyId");
        Map input = UtilMisc.toMap("facilityId", facilityId, "partyId", userLogin.get("partyId"), "securityGroupId", "WRHS_MANAGER", "userLogin", userLogin, "locale", context.get("locale"));
        Map results = addFacilityTeamMember(dctx, input);
        if (ServiceUtil.isError(results)) return results;
        results.put("facilityId", facilityId);
        return results;
    }

    public static Map addFacilityTeamMember(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String facilityId = (String) context.get("facilityId");
        WarehouseSecurity warehouseSecurity = new WarehouseSecurity(security, userLogin, facilityId);
        if (! warehouseSecurity.hasFacilityPermission("WRHS_CONFIG")) {
            return ServiceUtil.returnError("Sorry, you do not have permission to perform this action."); // TODO use the UtilCommon error system
        }

        String partyId = (String) context.get("partyId");
        String securityGroupId = (String) context.get("securityGroupId");
        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            Map input = UtilMisc.toMap("facilityId", facilityId, "partyId", partyId, "fromDate", now, "securityGroupId", securityGroupId);
            GenericValue permission = delegator.makeValue("FacilityPartyPermission", input);
            permission.create();
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    public static Map removeFacilityTeamMember(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String facilityId = (String) context.get("facilityId");
        WarehouseSecurity warehouseSecurity = new WarehouseSecurity(security, userLogin, facilityId);
        if (! warehouseSecurity.hasFacilityPermission("WRHS_CONFIG")) {
            return ServiceUtil.returnError("Sorry, you do not have permission to perform this action."); // TODO use the UtilCommon error system
        }

        String partyId = (String) context.get("partyId");
        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            List permissions = delegator.findByAnd("FacilityPartyPermission", UtilMisc.toMap("facilityId", facilityId, "partyId", partyId));
            for (Iterator iter = permissions.iterator(); iter.hasNext(); ) {
                GenericValue permission = (GenericValue) iter.next();
                permission.set("thruDate", now);
                permission.store();
            }
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
}
