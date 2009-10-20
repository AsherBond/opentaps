/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.common.pagination;

import org.opentaps.common.util.UtilConfig;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

import java.util.List;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Map;

/**
 * The pagination state records in as few variables as possible what
 * part of the list the user is seing and what the order of the list is.
 * Whenever the pagination state changes, such as when the user advances
 * to the next page or increases the size of his viewport, the state
 * will be saved in PaginationPreference for later retrieval.
 *
 * Default viewSize for all paginators in all opentaps applications is
 * defined in opentaps.properties as follows,
 *
 * pagination.default.viewSize = 20
 *
 * An application may override this by defining pagination.default.viewSize
 * in the ${applicationName}.propeties file.
 * 
 * The default values for a particular paginator can be defined as follows,
 *
 * pagination.default.${paginatorName}.viewSize = 20
 * pagination.default.${paginatorName}.orderBy = fieldName1, fieldName2 DESC, fieldName3, ...
 *
 * @author Leon Torres (leon@opensourcestrategies.com)
 */
public class PaginationState {

    public static final String module = PaginationState.class.getName();
    public static final int DEFAULT_VIEW_SIZE = UtilConfig.getPropertyInt("opentaps", "pagination.default.viewSize", 20);

    protected String paginatorName = null;
    protected String applicationName = null;
    protected GenericValue userLogin = null;
    protected long viewSize = DEFAULT_VIEW_SIZE;
    protected long cursorIndex = 0;
    protected List orderBy = null;
    protected boolean rememberPage;
    protected boolean rememberOrderBy;

    protected PaginationState() {}

    /**
     * Initializes a pagination state.  This will load the last user state if exists, otherwise it determines
     * the default state for the list from the properties files. (See above for documentation.)
     */
    public PaginationState(String paginatorName, GenericValue userLogin, String applicationName, boolean rememberPage, boolean rememberOrderBy) {
        this.paginatorName = paginatorName;
        this.applicationName = applicationName;
        this.userLogin = userLogin;
        this.rememberPage = rememberPage;
        this.rememberOrderBy = rememberOrderBy;

        load();
    }


    public String getPaginatorName() {
        return paginatorName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public GenericValue getUserLogin() {
        return userLogin;
    }

    public List getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List orderBy) {
        this.orderBy = orderBy;
    }

    public long getViewSize() {
        return viewSize;
    }

    public void setViewSize(long viewSize) {
        if (viewSize <= 0) viewSize = DEFAULT_VIEW_SIZE;
        this.viewSize = viewSize;
    }

    public long getCursorIndex() {
        return cursorIndex;
    }

    public void setCursorIndex(long cursorIndex) {
        if (cursorIndex < 0) cursorIndex = 0;
        this.cursorIndex = cursorIndex;
    }

    public void setRememberPage(boolean rememberPage) {
        this.rememberPage = rememberPage;
    }

    public void setRememberOrderBy(boolean rememberOrderBy) {
        this.rememberOrderBy = rememberOrderBy;
    }

    /** Get the name as actually stored in the database, which is ${applicationNAme}.${paginatorName} */
    public String getNameForDatabase() {
        return applicationName + "." + paginatorName;
    }

    /** The paginator is responsible for deciding when to save. */
    public void save() {
        if (userLogin == null) return;
        try {
            GenericDelegator delegator = userLogin.getDelegator();
            if (delegator == null) return;

            boolean newPref = false;
            Map input = UtilMisc.toMap("userLoginId", userLogin.get("userLoginId"), "paginatorName", getNameForDatabase());
            GenericValue preference = delegator.findByPrimaryKey("PaginationPreference", input);
            if (preference == null) {
                newPref = true;
                preference = delegator.makeValue("PaginationPreference", input);
            }
            preference.set("viewSize", new Long(viewSize));
            if (rememberPage) {
                preference.set("cursorIndex", new Long(cursorIndex));
            }
            if (rememberOrderBy) {
                preference.set("orderBy", PaginationState.serializeOrderBy(orderBy));
            }
            if (newPref) {
                preference.create();
            } else {
                preference.store();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Not saving pagination state for user ["+userLogin.get("userLoginId")+"].", module);
        }
    }

    private void load() {

        // default view size
        int defaultViewSize = UtilConfig.getPropertyInt(applicationName, "pagination.default.viewSize", DEFAULT_VIEW_SIZE);
        setViewSize(defaultViewSize);

        // default order by
        String defaultOrderBy = UtilConfig.getPropertyValue(applicationName, "pagination.default.orderBy");
        setOrderBy(loadOrderBy(defaultOrderBy));

        if (userLogin == null) return;
        try {
            GenericDelegator delegator = userLogin.getDelegator();
            if (delegator == null) return;
            GenericValue preference = delegator.findByPrimaryKey("PaginationPreference", UtilMisc.toMap("userLoginId", userLogin.get("userLoginId"), "paginatorName", getNameForDatabase()));
            if (preference == null) return;

            Long viewSizeValue = preference.getLong("viewSize");
            if (viewSizeValue != null) setViewSize(viewSizeValue.longValue());

            if (rememberPage) {
                Long cursorIndexValue = preference.getLong("cursorIndex");
                if (cursorIndexValue != null) setCursorIndex(cursorIndexValue.longValue());
            }
            if (rememberOrderBy) {
                List orderByValue = PaginationState.loadOrderBy(preference.getString("orderBy"));
                if (orderByValue != null) setOrderBy(orderByValue);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Not saving pagination state for user ["+userLogin.get("userLoginId")+"].", module);
        }
    }

    /** Serializes the orderBy list into a string. */
    public static String serializeOrderBy(List orderBy) {
        if (orderBy == null) return null;
        StringBuffer buff = new StringBuffer();
        for (Iterator iter = orderBy.iterator(); iter.hasNext(); ) {
            buff.append(iter.next());
            if (iter.hasNext()) buff.append(',');
        }
        return buff.toString();
    }

    /** Loads an order by string into a List */
    public static List loadOrderBy(String orderByValue) {
        if (UtilValidate.isEmpty(orderByValue)) return null;
        return Arrays.asList(orderByValue.split("\\s*,\\s*"));
    }
}
