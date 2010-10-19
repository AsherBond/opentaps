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

package org.opentaps.amazon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.event.AjaxEvents;

/**
 * Events for the Amazon integration.
 */
public final class AmazonEvents {

    private AmazonEvents() { }

    private static final String MODULE = AmazonEvents.class.getName();

    /**
     * Describe <code>getValidAttributesForItemTypeJSON</code> method here.
     *
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code> value
     */
    public static String getValidAttributesForItemTypeJSON(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilMisc.ensureLocale(UtilHttp.getLocale(request));
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String nodeId = request.getParameter("nodeId");
        String itemTypeId = request.getParameter("itemTypeId");
        Map<String, List<GenericValue>> validAttr = new HashMap<String, List<GenericValue>>();

        if (UtilValidate.isEmpty(itemTypeId)) {
            List<GenericValue> itemTypes = null;
            try {
                List<GenericValue> validAttributes = delegator.findByAndCache("AmazonNodeValidAttribute", UtilMisc.toMap("nodeId", nodeId));
                List<String> itemTypeIds = EntityUtil.getFieldListFromEntityList(validAttributes, "itemTypeId", true);
                itemTypes = delegator.findByCondition("AmazonProductItemType", EntityCondition.makeCondition("itemTypeId", EntityOperator.IN, itemTypeIds), null, null);
            } catch (GenericEntityException e) {
                Debug.logError("Error retrieving Amazon browse node data in getValidAttributesForItemTypeJSON", MODULE);
                itemTypes = new ArrayList<GenericValue>();
            }
            for (GenericValue value : itemTypes) {
                value.set("description", value.get("description", "AmazonUiLabels", locale));
            }
            validAttr.put("itemTypes", itemTypes);
        }

        List<GenericValue> usedFor = AmazonUtil.getValidAttributesForItemType(delegator, "USED_FOR", nodeId, itemTypeId);
        if (usedFor == null) {
            usedFor = new ArrayList<GenericValue>();
        }
        for (GenericValue value : usedFor) {
            value.set("description", value.get("description", "AmazonUiLabels", locale));
        }
        validAttr.put("USED_FOR", usedFor);

        List<GenericValue> targetAudience = AmazonUtil.getValidAttributesForItemType(delegator, "TARGET_AUDIENCE", nodeId, itemTypeId);
        if (targetAudience == null) {
            targetAudience = new ArrayList<GenericValue>();
        }
        for (GenericValue value : targetAudience) {
            value.set("description", value.get("description", "AmazonUiLabels", locale));
        }
        validAttr.put("TARGET_AUDIENCE", targetAudience);

        List<GenericValue> otherItemAttributes = AmazonUtil.getValidAttributesForItemType(delegator, "OTHER_ITEM_ATTR", nodeId, itemTypeId);
        if (otherItemAttributes == null) {
            otherItemAttributes = new ArrayList<GenericValue>();
        }
        for (GenericValue value : otherItemAttributes) {
            value.set("description", value.get("description", "AmazonUiLabels", locale));
        }
        validAttr.put("OTHER_ITEM_ATTR", otherItemAttributes);

        return AjaxEvents.doJSONResponse(response, validAttr);
    }
}
