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
package org.opentaps.purchasing.security;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.opentaps.common.security.OpentapsSecurity;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.webapp.WebElementInterface;
import org.opentaps.foundation.entity.EntityInterface;

/**
 * Security methods for the Purchasing application.
 */
public class PurchasingSecurity extends OpentapsSecurity {

    private static final String MODULE = PurchasingSecurity.class.getName();

    static {
        OpentapsSecurity.registerApplicationSecurity("purchasing", PurchasingSecurity.class);
    }

    // prevent use of no argument constructor
    protected PurchasingSecurity() {}

    /**
     * Create a new PurchasingSecurity for the given Security and userLogin.
     * @param security a <code>Security</code> value
     * @param userLogin a <code>GenericValue</code> value
     */
    public PurchasingSecurity(Security security, GenericValue userLogin) {
        super(security, userLogin);
    }

    /**
     * Checks section security.  For purchasing, this is based on party relation security between the organizationPartyId and userLogin.
     * @param section a <code>String</code> value
     * @param module a <code>String</code> value
     * @param request a <code>HttpServletRequest</code> value
     * @return a <code>boolean</code> value
     */
    public boolean checkSectionSecurity(String section, String module, HttpServletRequest request) {
        String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        return hasPartyRelationSecurity(module, "_VIEW", organizationPartyId);
    }

    /**
     * A handler method, checks the permission for the selected organization and the current user.
     * Uses the handler parameter to specify the permission to check, can be given as <module>:<action> or <module>_<action>.
     * If the security check fails, the web element is set a disabled (instead of hidden) and may still display if its showIfDisabled is set.
     * @param <T> an <code>EntityInterface & WebElementInterface</code>
     * @param context a <code>Map</code> value
     * @param obj any object
     * @return an <code>EntityInterface & WebElementInterface</code> value
     */
    public static <T extends EntityInterface & WebElementInterface> T checkOrganizationPermission(Map<String, Object> context, T obj) {
        String permission = obj.getString("handlerParameter");
        if (UtilValidate.isEmpty(permission)) {
            return obj;
        }

        HttpSession session = (HttpSession) context.get("session");

        // get the current organization
        String organizationPartyId = (String) context.get("organizationPartyId");
        if (UtilValidate.isEmpty(organizationPartyId)) {
            GenericValue organizationParty = (GenericValue) context.get("applicationSetupOrganization");
            if (organizationParty == null && session != null) {
                organizationParty = (GenericValue) session.getAttribute("organizationParty");
            }
            // if still no organization found at this point, give up
            if (organizationParty == null) {
                Debug.logError("Missing Organization party ID in the session or context to perform the permission check", MODULE);
                // returning null in that case
                return null;
            }
            organizationPartyId = organizationParty.getString("partyId");
        }

        // get the purchasing security instance
        PurchasingSecurity security = null;
        if (session != null) {
            security = (PurchasingSecurity) session.getAttribute("purchasingSecurity");
        }
        if (security == null) {
            security = (PurchasingSecurity) context.get("purchasingSecurity");
        }
        // if not found, make a new one with the current user and Security instances
        if (security == null) {
            Security sec = (Security) context.get("security");
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            if (sec != null && userLogin != null) {
                security = new PurchasingSecurity(sec, userLogin);
            }
        }

        if (security == null) {
            // without a security instance we cannot perform the check
            Debug.logError("Missing PurchasingSecurity instance in the session or context to perform the permission check", MODULE);
            obj.setDisabled(true);
            return obj;
        }

        // now split the given permission into module + action
        String[] s = permission.split(":");
        String module = null;
        String action = null;
        if (s.length >= 2) {
            module = s[0];
            action = s[1];
        } else {
            module = permission.substring(0, permission.lastIndexOf("_"));
            action = permission.substring(permission.lastIndexOf("_") + 1);
        }

        // note: action should always start with _
        if (UtilValidate.isNotEmpty(action)) {
            if (!action.startsWith("_")) {
                action = "_" + action;
            }
        }

        if (!security.hasPartyRelationSecurity(module, action, organizationPartyId)) {
            obj.setDisabled(true);
        }

        return obj;
    }
}
