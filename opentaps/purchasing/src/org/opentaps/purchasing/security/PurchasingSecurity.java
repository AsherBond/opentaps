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

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.opentaps.common.security.OpentapsSecurity;
import org.opentaps.common.util.UtilCommon;

/**
 * Security methods for the Purchasing application.
 */
public class PurchasingSecurity extends OpentapsSecurity {

    public static final String module = PurchasingSecurity.class.getName();

    static {
        OpentapsSecurity.registerApplicationSecurity("purchasing", PurchasingSecurity.class);
    }

    // prevent use of no argument constructor
    protected PurchasingSecurity() {}

    /**
     * Create a new PurchasingSecurity for the given Security and userLogin.
     */
    public PurchasingSecurity(Security security, GenericValue userLogin) {
        super(security, userLogin);
    }

    /**
     * Checks section security.  For purchasing, this is based on party relation security between the organizationPartyId and userLogin.
     */
    public boolean checkSectionSecurity(String section, String module, HttpServletRequest request) {
        String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        return hasPartyRelationSecurity(module, "_VIEW", organizationPartyId);
    }
}
