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

package org.opentaps.webapp.handlers;

import java.util.Map;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.opentaps.common.party.ViewPrefWorker;
import org.opentaps.foundation.entity.EntityInterface;

public abstract class CommonHandlers {

    private CommonHandlers() { }

    /**
     * A generic handler method, checks the session does NOT have the given handlerParameter.
     * @param <T> an <code>EntityInterface</code>
     * @param context a <code>Map</code> value
     * @param obj any object
     * @return a T value or null
     */
    public static <T extends EntityInterface> T checkSessionValueAbsent(Map<String, Object> context, T obj) {
        HttpSession session = (HttpSession) context.get("session");
        String varName = obj.getString("handlerParameter");
        if (session == null || session.getAttribute(varName) == null) {
            return obj;
        }
        return null;
    }

    /**
     * A generic handler method, checks the session does have the given handlerParameter (as a non null value).
     * @param <T> an <code>EntityInterface</code>
     * @param context a <code>Map</code> value
     * @param obj any object
     * @return a T value or null
     */
    public static <T extends EntityInterface> T checkSessionValuePresent(Map<String, Object> context, T obj) {
        HttpSession session = (HttpSession) context.get("session");
        String varName = obj.getString("handlerParameter");
        if (session != null && session.getAttribute(varName) != null) {
            return obj;
        }
        return null;
    }

    /**
     * A generic handler method, checks if there is NO cart in progress.
     * @param <T> an <code>EntityInterface</code>
     * @param context a <code>Map</code> value
     * @param obj any object
     * @return a T value or null
     */
    public static <T extends EntityInterface> T checkHasNoCart(Map<String, Object> context, T obj) {
        HttpSession session = (HttpSession) context.get("session");
        if (session == null || session.getAttribute("shoppingCart") == null) {
            return obj;
        }
        return null;
    }

    /**
     * A generic handler method, checks if there is a cart in progress.
     * @param <T> an <code>EntityInterface</code>
     * @param context a <code>Map</code> value
     * @param obj any object
     * @return a T value or null
     */
    public static <T extends EntityInterface> T checkHasCart(Map<String, Object> context, T obj) {
        HttpSession session = (HttpSession) context.get("session");
        if (session != null && session.getAttribute("shoppingCart") != null) {
            return obj;
        }
        return null;
    }

    /**
     * A generic handler method, checks a variable indicated by the handler parameter.
     * @param <T> an <code>EntityInterface</code> that has an handlerParameter field
     * @param context a <code>Map</code> value
     * @param obj any object
     * @return a T value or null
     */
    public static <T extends EntityInterface> T checkBoolean(Map<String, Object> context, T obj) {
        String varName = obj.getString("handlerParameter");
        if (UtilValidate.isNotEmpty(varName)) {
            if ("Y".equals(context.get(varName)) || "y".equals(context.get(varName)) || Boolean.TRUE.equals(context.get(varName))) {
                return obj;
            }
        }

        return null;
    }

    /**
     * A generic handler method, checks an user preference as specified in the handlerParameter.
     * Split the handlerParameter as <code>prefType:valueToCheck:default</code>
     *   prefType is the preference name (like: MY_OR_TEAM_ACCOUNTS)
     *   valueToCheck is the value to test agains (eg: TEAM_VALUES or MY_VALUES)
     *   default is optional and used to set the value when empty
     * @param <T> an <code>EntityInterface</code> that has an handlerParameter field
     * @param context a <code>Map</code> value
     * @param obj any object
     * @return a T value or null
     * @throws GenericEntityException if an error occurs
     */
    public static <T extends EntityInterface> T checkViewPreferenceForTab(Map<String, Object> context, T obj) throws GenericEntityException {
        String varName = obj.getString("handlerParameter");
        if (UtilValidate.isEmpty(varName)) {
            return null;
        }
        String[] vars = varName.split(":");
        if (vars.length < 2) {
            return null;
        }
        String prefType = vars[0];
        String valueToCheck = vars[1];
        String defaultValue = (vars.length > 2) ? vars[2] : null;

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // the following does not handle null users
        if (userLogin == null) {
            return null;
        }

        Map viewPreferences = ViewPrefWorker.getViewPreferencesByLocation(userLogin, (String) context.get("opentapsApplicationName"), (String) context.get("sectionName"));
        String prefValue = (String) viewPreferences.get(prefType);
        if (UtilValidate.isEmpty(prefValue)) {
            prefValue = defaultValue;
        }
        if (valueToCheck.equals(prefValue)) {
            return obj;
        }

        return null;
    }

}
