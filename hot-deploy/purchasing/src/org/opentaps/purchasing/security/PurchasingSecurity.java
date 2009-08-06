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
package org.opentaps.purchasing.security;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.opentaps.common.security.OpentapsSecurity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
        HttpSession session = request.getSession();
        String organizationPartyId = (String) session.getAttribute("organizationPartyId");
        return hasPartyRelationSecurity(module, "_VIEW", organizationPartyId);
    }
}
