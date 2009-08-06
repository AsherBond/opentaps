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

package org.opentaps.common.custrequest;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.common.util.UtilView;

import java.util.Map;
import java.util.Locale;

/**
 * Custrequest services - Services for dealing with opentaps specific concepts of custumer requests, such as cases.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public class CustRequestServices {

    public static final String module = CustRequestServices.class.getName();

    public static Map markCustRequestAsUpdated(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String primaryKeyId = (String) context.get("custRequestId");
        try {
            UtilView.markAsUpdated(delegator, "CustRequest", primaryKeyId);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, (Locale) context.get("locale"), module);
        }
        return ServiceUtil.returnSuccess();
    }
}
