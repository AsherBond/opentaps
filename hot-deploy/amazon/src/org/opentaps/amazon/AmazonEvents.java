/*
Copyright (c) 2006 - 2009 Open Source Strategies, Inc.

This program is free software; you can redistribute it and/or modify
it under the terms of the Honest Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
Honest Public License for more details.

You should have received a copy of the Honest Public License
along with this program; if not, write to Funambol,
643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
*/

package org.opentaps.amazon;

import java.util.*;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.event.AjaxEvents;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Chris Liberty (cliberty@opensourcestrategies.com)
 * @version $Rev: 10645 $
 */
public class AmazonEvents {

    public static final String module = AmazonEvents.class.getName();

    public static String getValidAttributesForItemTypeJSON(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilMisc.ensureLocale( UtilHttp.getLocale(request));
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        String nodeId = (String) request.getParameter("nodeId");
        String itemTypeId = (String) request.getParameter("itemTypeId");
        Map<String, List<GenericValue>> validAttr = new HashMap<String, List<GenericValue>>();

        if ( UtilValidate.isEmpty(itemTypeId)) {
            List<GenericValue> itemTypes = null;
            try {
                List<GenericValue> validAttributes = delegator.findByAndCache("AmazonNodeValidAttribute", UtilMisc.toMap("nodeId", nodeId));
                List<String> itemTypeIds = EntityUtil.getFieldListFromEntityList(validAttributes, "itemTypeId", true);
                itemTypes = delegator.findByCondition("AmazonProductItemType", new EntityExpr("itemTypeId", EntityOperator.IN, itemTypeIds), null, null);
            } catch( GenericEntityException e ) {
                Debug.logError("Error retrieving Amazon browse node data in getValidAttributesForItemTypeJSON", module);
                itemTypes = new ArrayList();
            }
            for (GenericValue value : itemTypes) value.set("description", value.get("description", "AmazonUiLabels", locale));
            validAttr.put("itemTypes", itemTypes);
        }
        
        List<GenericValue> usedFor = AmazonUtil.getValidAttributesForItemType(delegator, "USED_FOR", nodeId, itemTypeId);
        if (usedFor == null) usedFor = new ArrayList<GenericValue>();
        for (GenericValue value : usedFor) value.set("description", value.get("description", "AmazonUiLabels", locale));
        validAttr.put("USED_FOR", usedFor);

        List<GenericValue> targetAudience = AmazonUtil.getValidAttributesForItemType(delegator, "TARGET_AUDIENCE", nodeId, itemTypeId);
        if (targetAudience == null) targetAudience = new ArrayList<GenericValue>();
        for (GenericValue value : targetAudience) value.set("description", value.get("description", "AmazonUiLabels", locale));
        validAttr.put("TARGET_AUDIENCE", targetAudience);

        List<GenericValue> otherItemAttributes = AmazonUtil.getValidAttributesForItemType(delegator, "OTHER_ITEM_ATTR", nodeId, itemTypeId);
        if (otherItemAttributes == null) otherItemAttributes = new ArrayList<GenericValue>();
        for (GenericValue value : otherItemAttributes) value.set("description", value.get("description", "AmazonUiLabels", locale));
        validAttr.put("OTHER_ITEM_ATTR", otherItemAttributes);

        return AjaxEvents.doJSONResponse(response, validAttr);
    }
}
