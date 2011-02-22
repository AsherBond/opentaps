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

package org.opentaps.warehouse.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.opentaps.common.security.OpentapsSecurity;
import org.opentaps.domain.webapp.WebElementInterface;
import org.opentaps.foundation.entity.EntityInterface;

/**
 * Security methods for the Warehouse application.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public class WarehouseSecurity extends OpentapsSecurity {

    private static final String MODULE = WarehouseSecurity.class.getName();

    private GenericValue userLogin = null;
    private String facilityId = null;
    private Security security = null;

    static {
        OpentapsSecurity.registerApplicationSecurity("warehouse", WarehouseSecurity.class);
    }

    /**
     * Use this method if the facilityId is not known--ie before the user has selected a facility.
     * @param security
     * @param userLogin
     */
    public WarehouseSecurity(Security security, GenericValue userLogin) {
        this(security, userLogin, null);
    }

    /**
     * Use this method when the facilityId is known.
     * @param security
     * @param userLogin
     * @param facilityId
     */
    public WarehouseSecurity(Security security, GenericValue userLogin, String facilityId) {
        super(security, userLogin);
        setSecurity(security);
        setUserLogin(userLogin);
        setFacilityId(facilityId);
    }

    public Security getSecurity() {
        return security;
    }

    private void setSecurity(Security security) {
        this.security = security;
    }

    public GenericValue getUserLogin() {
        return userLogin;
    }

    private void setUserLogin(GenericValue userLogin) {
        this.userLogin = userLogin;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    public boolean hasFacilityPermission(String permissionId) {
        return hasFacilityPermission(getFacilityId(), getUserLogin(), permissionId);
    }

    /**
     * Determine whether a user has a given permission for a given facility.
     * @param facilityId The facility ID
     * @param userLogin The userLogin of the user
     * @param permissionId The permission in question
     * @return true if the user has the WRHS_ADMIN permission, or has the given permission against the given facility
     */
    public boolean hasFacilityPermission(String facilityId, GenericValue userLogin, String permissionId) {

        if (UtilValidate.isEmpty(userLogin) || UtilValidate.isEmpty(permissionId)) {
            return false;
        }

        // Users with Warehouse Admin permission are allowed to do anything to any facility
        if (this.security.hasPermission("WRHS_ADMIN", userLogin)) {
            return true;
        }

        Delegator delegator = userLogin.getDelegator();
        try {

            List<EntityCondition> facilityPartySecPermConditions = new ArrayList<EntityCondition>();
            if (UtilValidate.isNotEmpty(facilityId)) {

                // If facilityId is not specified, check to see if the user has permission for ANY facility
                facilityPartySecPermConditions.add(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId));
            }
            facilityPartySecPermConditions.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, userLogin.get("partyId")));
            facilityPartySecPermConditions.add(EntityCondition.makeCondition("permissionId", EntityOperator.EQUALS, permissionId));
            facilityPartySecPermConditions.add(EntityUtil.getFilterByDateExpr());

            List<GenericValue> facilityPartySecPerms = delegator.findByAnd("FacilityPartySecurityPermission", facilityPartySecPermConditions);
            if (UtilValidate.isNotEmpty(facilityPartySecPerms)) {
                return true;
            }

        } catch (GenericEntityException e) {
            Debug.logError("Error in WarehouseSecurity.hasFacilityPermission(): " + e.getMessage(), MODULE);
            return false;
        }

        Debug.logWarning("Permission [" + permissionId + "] denied for userLoginId [" + userLogin.get("userLoginId") + "] on facilityId [" + facilityId + "]", MODULE);
        return false;
    }

    public List<GenericValue> getUserFacilities() {
        return getUserFacilities(getUserLogin());
    }

    /**
     * Retrieve a list of facilities which a user has at least one permission for.
     * @param userLogin GenericValue
     * @return List of GenericValue facilities
     */
    public List<GenericValue> getUserFacilities(GenericValue userLogin) {
        Delegator delegator = DelegatorFactory.getDelegator("default");
        List<GenericValue> facilities = new ArrayList<GenericValue>();
        try {
            if (getSecurity().hasPermission("WRHS_ADMIN", getUserLogin())) {
                facilities.addAll(delegator.findAll("Facility"));
            } else {
                EntityCondition conditions = EntityCondition.makeCondition(
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, userLogin.get("partyId")),
                    EntityUtil.getFilterByDateExpr());
                facilities.addAll(delegator.findList("FacilityPartyPermissionDetail", conditions, null, UtilMisc.toList("facilityName"), null, false));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
        }
        return facilities;
    }

    public boolean checkSectionSecurity(String section, String module, HttpServletRequest request) {
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
        return hasFacilityPermission(getFacilityId(), userLogin, module + "_VIEW");
    }

    /**
     * A handler method, checks the facility permission for the current user.
     * Uses the handler parameter to specify the facility permission to check.
     * If the security check fails, the web element is set a disabled (instead of hidden) and may still display if its showIfDisabled is set.
     * @param <T> an <code>EntityInterface & WebElementInterface</code>
     * @param context a <code>Map</code> value
     * @param obj any object
     * @return an <code>EntityInterface & WebElementInterface</code> value
     */
    public static <T extends EntityInterface & WebElementInterface> T checkFacilityPermission(Map<String, Object> context, T obj) {
        String permission = obj.getString("handlerParameter");
        if (UtilValidate.isEmpty(permission)) {
            return obj;
        }

        WarehouseSecurity security = null;
        HttpSession session = (HttpSession) context.get("session");
        if (session != null) {
            security = (WarehouseSecurity) session.getAttribute("warehouseSecurity");
        } else {
            security = (WarehouseSecurity) context.get("warehouseSecurity");
        }

        if (security == null) {
            // without a security instance we cannot perform the check
            Debug.logError("Missing WarehouseSecurity instance in the session or context to perform the permission check", MODULE);
            obj.setDisabled(true);
            return obj;
        }

        if (!security.hasFacilityPermission(permission)) {
            obj.setDisabled(true);
        }
        return obj;
    }
}
