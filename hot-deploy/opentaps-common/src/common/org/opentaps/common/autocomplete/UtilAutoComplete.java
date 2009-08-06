/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.common.autocomplete;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.event.AjaxEvents;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.sql.ResultSet;

/**
 * Auto Complete constants and utility methods.
 */
public final class UtilAutoComplete {

    private UtilAutoComplete() { }

    /** Common EntityFindOptions for distinct search. */
    public static EntityFindOptions AC_FIND_OPTIONS = new EntityFindOptions(true, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, true);

    /** How many results to show in the autocomplete list. */
    public static final int AC_DEFAULT_RESULTS_SIZE = 10;

    /** The field list should only contain what is needed to build the response  */
    public static List<String> AC_PARTY_NAME_FIELDS = Arrays.asList("partyId", "groupName", "firstName", "lastName");
    public static List<String> AP_PARTY_ORDER_BY = Arrays.asList("groupName", "firstName", "lastName");

    public static List<String> AC_ACCOUNT_FIELDS = Arrays.asList("glAccountId", "accountCode", "accountName");
    public static List<String> AC_ACCOUNT_ORDER_BY = Arrays.asList("accountCode", "accountName");

    public static List<String> AC_PRODUCT_FIELDS = Arrays.asList("productId", "internalName");
    public static List<String> AC_PRODUCT_ORDER_BY = Arrays.asList("productId");

    public static EntityCondition ac_accountRoleCondition, ac_contactRoleCondition, ac_prospectRoleCondition, ac_clientRoleCondition;
    public static EntityCondition ac_accountOrProspectRoleCondition;
    static {
        ac_accountRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT");
        ac_contactRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "CONTACT");
        ac_prospectRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "PROSPECT");
        ac_clientRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList("ACCOUNT", "CONTACT", "PROSPECT"));
        ac_accountOrProspectRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList("ACCOUNT", "PROSPECT"));
    }

    public static EntityCondition getActiveRelationshipCondition(GenericValue party, EntityCondition otherConditions) {
        return EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("partyIdTo", party.get("partyId")),
                    EntityUtil.getFilterByDateExpr(),
                    otherConditions);
    }

    /**
     * Make an autocomplete selection list in JSON.  The idea is we pass in the key field to be used for the option value (objectKey)
     * and a SectionBuilder for constructing a map representing each of the elements.  The map must return a name field containing
     * the description of the option and a value field.  TODO: maybe an object is a better way to do this
     */
    public static String makeSelectionJSONResponse(HttpServletResponse response, Collection collection, String objectKey, SelectionBuilder builder) {
        JSONArray jsonArray = new JSONArray();
        if (collection != null) {
            for (Object element : collection) {
                Map<String, Object> map = builder.buildRow(element);
                JSONObject jsonObject = new JSONObject();
                for (String key : map.keySet()) {
                    jsonObject.put(key, map.get(key));
                }
                jsonArray.element(jsonObject.toString());
            }
        }

        Map<String, Object> retval = new HashMap<String, Object>();
        retval.put("items", jsonArray);
        retval.put("identifier", objectKey);

        return AjaxEvents.doJSONResponse(response, JSONObject.fromObject(retval));
    }

}
