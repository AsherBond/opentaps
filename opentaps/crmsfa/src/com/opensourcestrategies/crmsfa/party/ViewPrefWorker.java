/*
 *
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
package com.opensourcestrategies.crmsfa.party;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * ViewPreference helper methods
 *
 * @author     <a href="mailto:leon@opensourcestrategies.org">Leon Torres</a>
 * @version    $Rev: $
 * @since      3.1
 */
public class ViewPrefWorker {
    
    public static String module = ViewPrefWorker.class.getName();

    /** Gets the view preferences as a Map of String values keyed to the preference type for the given location. */
    public static Map getViewPreferencesByLocation(GenericValue userLogin, String application, String applicationSection, String screenName, String formName) 
        throws GenericEntityException {
        Map conditions = UtilMisc.toMap("userLoginId", userLogin.get("userLoginId"), "application", application, "applicationSection", applicationSection, "screenName", screenName, "formName", formName);
        List prefs = userLogin.getDelegator().findByAnd("ViewPrefAndLocation", conditions);
        Map results = FastMap.newInstance();
        for (Iterator iter = prefs.iterator(); iter.hasNext(); ) {
            GenericValue pref = (GenericValue) iter.next();
            if ("VPREF_VALTYPE_ENUM".equals(pref.get("viewPrefValueTypeId"))) {
                results.put(pref.get("viewPrefTypeId"), pref.get("viewPrefEnumId"));
            } else {
                results.put(pref.get("viewPrefTypeId"), pref.get("viewPrefString"));
            }
        }
        return results;
    }

    /** As above, but for application and section */
    public static Map getViewPreferencesByLocation(GenericValue userLogin, String application, String applicationSection) throws GenericEntityException {
        return getViewPreferencesByLocation(userLogin, application, applicationSection, null, null);
    }

    /** Gets the value of the preference as a String. Speficy the userLogin and viewPrefTypeId. */
    public static String getViewPreferenceString(GenericValue userLogin, String viewPrefTypeId) throws GenericEntityException {
        GenericValue pref = getViewPreferenceValue(userLogin, viewPrefTypeId);
        if (pref == null) return null;
        if ("VPREF_VALTYPE_ENUM".equals(pref.get("viewPrefValueTypeId"))) return pref.getString("viewPrefEnumId");
        return pref.getString("viewPrefString");
    }

    /** Fetch the user login's active view preference as a GenericValue given a preference type.  */
    public static GenericValue getViewPreferenceValue(GenericValue userLogin, String viewPrefTypeId) throws GenericEntityException {
        Delegator delegator = userLogin.getDelegator();
        return delegator.findByPrimaryKey("ViewPreference", 
                    UtilMisc.toMap("viewPrefTypeId", viewPrefTypeId, "userLoginId", userLogin.get("userLoginId")));
    }
}
