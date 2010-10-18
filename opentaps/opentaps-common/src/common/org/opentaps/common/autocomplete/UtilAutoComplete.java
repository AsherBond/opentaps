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
package org.opentaps.common.autocomplete;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.common.event.AjaxEvents;

import org.opentaps.common.util.UtilMessage;

/**
 * Auto Complete constants and utility methods.
 */
public final class UtilAutoComplete {

    private UtilAutoComplete() { }

    /** Common EntityFindOptions for distinct search. */
    public static final EntityFindOptions AC_FIND_OPTIONS = new EntityFindOptions(true, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, true);

    /** How many results to show in the autocomplete list. */
    public static final int AC_DEFAULT_RESULTS_SIZE = 10;

    /** The fields to reteive to build party names autocompleted items. */
    public static final List<String> AC_PARTY_NAME_FIELDS = Arrays.asList("partyId", "groupName", "firstName", "lastName");
    /** The party autocompleters sort. */
    public static final List<String> AP_PARTY_ORDER_BY = Arrays.asList("groupName", "firstName", "lastName");

    /** The fields to reteive to build GL accounts autocompleted items. */
    public static final List<String> AC_ACCOUNT_FIELDS = Arrays.asList("glAccountId", "accountCode", "accountName");
    /** The GL account autocompleters sort. */
    public static final List<String> AC_ACCOUNT_ORDER_BY = Arrays.asList("accountCode", "accountName");

    /** The fields to reteive to build products autocompleted items. */
    public static final List<String> AC_PRODUCT_FIELDS = Arrays.asList("productId", "internalName");
    /** The product autocompleters sort. */
    public static final List<String> AC_PRODUCT_ORDER_BY = Arrays.asList("productId");

    /** Some role conditions used by the autocompleters. */
    public static final EntityCondition ac_accountRoleCondition, ac_contactRoleCondition, ac_prospectRoleCondition, ac_clientRoleCondition, ac_crmPartyRoleCondition, ac_accountOrProspectRoleCondition, ac_activePartyCondition;
    static {
        ac_activePartyCondition = EntityCondition.makeCondition(EntityOperator.OR,
                                                                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, StatusItemConstants.PartyStatus.PARTY_DISABLED),
                                                                EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null));
        ac_accountRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, RoleTypeConstants.ACCOUNT);
        ac_contactRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, RoleTypeConstants.CONTACT);
        ac_prospectRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, RoleTypeConstants.PROSPECT);
        ac_clientRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList(RoleTypeConstants.ACCOUNT, RoleTypeConstants.CONTACT, RoleTypeConstants.PROSPECT));
        ac_crmPartyRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList(RoleTypeConstants.ACCOUNT, RoleTypeConstants.CONTACT, RoleTypeConstants.PROSPECT, RoleTypeConstants.PARTNER));
        ac_accountOrProspectRoleCondition = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList(RoleTypeConstants.ACCOUNT, RoleTypeConstants.PROSPECT));
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
     * the description of the option and a value field.  TODO: maybe an object is a better way to do this.
     * @param response a <code>HttpServletResponse</code> value
     * @param collection a <code>Collection</code> value
     * @param objectKey a <code>String</code> value
     * @param builder a <code>SelectionBuilder</code> value
     * @return a <code>String</code> value
     */
    public static String makeSelectionJSONResponse(HttpServletResponse response, Collection collection, String objectKey, SelectionBuilder builder, Locale locale) {
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

        if (collection == null || collection.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", UtilMessage.expandLabel("OpentapsAutocompletionNoMatch", locale));
            jsonObject.put(objectKey, "");
            jsonArray.element(jsonObject.toString());
        }

        Map<String, Object> retval = new HashMap<String, Object>();
        retval.put("items", jsonArray);
        retval.put("identifier", objectKey);

        return AjaxEvents.doJSONResponse(response, JSONObject.fromObject(retval));
    }

}
