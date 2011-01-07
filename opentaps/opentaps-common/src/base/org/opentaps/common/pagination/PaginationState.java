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

package org.opentaps.common.pagination;

import org.opentaps.common.util.UtilConfig;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.Delegator;
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

    private static final String MODULE = PaginationState.class.getName();
    /** The pagination default view size. */
    public static final int DEFAULT_VIEW_SIZE = UtilConfig.getPropertyInt("opentaps", "pagination.default.viewSize", 20);

    private String paginatorName = null;
    private String applicationName = null;
    private GenericValue userLogin = null;
    private long viewSize = DEFAULT_VIEW_SIZE;
    private long cursorIndex = 0;
    private List<String> orderBy = null;
    private boolean rememberPage;
    private boolean rememberOrderBy;
    private boolean viewAll = false; // used to bypass pagination

    /**
     * Creates a new <code>PaginationState</code> instance.
     */
    protected PaginationState() { }

    /**
     * Initializes a pagination state.  This will load the last user state if exists, otherwise it determines
     * the default state for the list from the properties files. (See above for documentation.)
     * @param paginatorName a <code>String</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param applicationName a <code>String</code> value
     * @param rememberPage a <code>boolean</code> value
     * @param rememberOrderBy a <code>boolean</code> value
     */
    public PaginationState(String paginatorName, GenericValue userLogin, String applicationName, boolean rememberPage, boolean rememberOrderBy) {
        this.paginatorName = paginatorName;
        this.applicationName = applicationName;
        this.userLogin = userLogin;
        this.rememberPage = rememberPage;
        this.rememberOrderBy = rememberOrderBy;

        load();
    }


    /**
     * Gets the paginator name.
     * @return a <code>String</code> value
     */
    public String getPaginatorName() {
        return paginatorName;
    }

    /**
     * Gets the paginator application name.
     * @return a <code>String</code> value
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Gets the <code>UserLogin</code>.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getUserLogin() {
        return userLogin;
    }

    /**
     * Gets the list of order by strings.
     * @return a <code>List</code> of <code>String</code>
     */
    public List<String> getOrderBy() {
        return orderBy;
    }

    /**
     * Sets the list of order by strings.
     * @param orderBy a <code>List</code> of <code>String</code>
     */
    public void setOrderBy(List<String> orderBy) {
        this.orderBy = orderBy;
    }

    /**
     * Gets the page view size.
     * @return a <code>long</code> value
     */
    public long getViewSize() {
        return viewSize;
    }

    /**
     * Sets the page view size.
     * @param viewSize a <code>long</code> value
     */
    public void setViewSize(long viewSize) {
        if (viewSize <= 0) {
            viewSize = DEFAULT_VIEW_SIZE;
        }
        this.viewSize = viewSize;
    }

    /**
     * Gets the page view all flag.
     * @return a <code>boolean</code> value
     */
    public boolean getViewAll() {
        return viewAll;
    }

    /**
     * Sets the page view all flag.
     * @param viewAll a <code>boolean</code> value
     */
    public void setViewAll(boolean viewAll) {
        this.viewAll = viewAll;
    }

    /**
     * Switch the page view all flag.
     */
    public void toggleViewAll() {
        this.viewAll = !this.viewAll;
    }

    /**
     * Gets the current cursor index.
     * @return a <code>long</code> value
     */
    public long getCursorIndex() {
        return cursorIndex;
    }

    /**
     * Sets the cursor index.
     * @param cursorIndex a <code>long</code> value
     */
    public void setCursorIndex(long cursorIndex) {
        if (cursorIndex < 0) {
            cursorIndex = 0;
        }
        this.cursorIndex = cursorIndex;
    }

    /**
     * Sets the remember page option.
     * @param rememberPage a <code>boolean</code> value
     */
    public void setRememberPage(boolean rememberPage) {
        this.rememberPage = rememberPage;
    }

    /**
     * Sets the remember order by option.
     * @param rememberOrderBy a <code>boolean</code> value
     */
    public void setRememberOrderBy(boolean rememberOrderBy) {
        this.rememberOrderBy = rememberOrderBy;
    }

    /**
     * Get the name as actually stored in the database, which is ${applicationNAme}.${paginatorName}.
     * @return the name stored in the database
     */
    public String getNameForDatabase() {
        return applicationName + "." + paginatorName;
    }

    /** The paginator is responsible for deciding when to save. */
    public void save() {
        if (userLogin == null) {
            return;
        }
        try {
            Delegator delegator = userLogin.getDelegator();
            if (delegator == null) {
                return;
            }

            boolean newPref = false;
            Map<String, Object> input = UtilMisc.<String, Object>toMap("userLoginId", userLogin.get("userLoginId"), "paginatorName", getNameForDatabase());
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
            Debug.logError(e, "Not saving pagination state for user [" + userLogin.get("userLoginId") + "].", MODULE);
        }
    }

    private void load() {

        // default view size
        int defaultViewSize = UtilConfig.getPropertyInt(applicationName, "pagination.default.viewSize", DEFAULT_VIEW_SIZE);
        setViewSize(defaultViewSize);

        // default order by
        String defaultOrderBy = UtilConfig.getPropertyValue(applicationName, "pagination.default.orderBy");
        setOrderBy(loadOrderBy(defaultOrderBy));

        if (userLogin == null) {
            return;
        }
        try {
            Delegator delegator = userLogin.getDelegator();
            if (delegator == null) {
                return;
            }
            GenericValue preference = delegator.findByPrimaryKey("PaginationPreference", UtilMisc.toMap("userLoginId", userLogin.get("userLoginId"), "paginatorName", getNameForDatabase()));
            if (preference == null) {
                return;
            }

            Long viewSizeValue = preference.getLong("viewSize");
            if (viewSizeValue != null) {
                setViewSize(viewSizeValue.longValue());
            }

            if (rememberPage) {
                Long cursorIndexValue = preference.getLong("cursorIndex");
                if (cursorIndexValue != null) {
                    setCursorIndex(cursorIndexValue.longValue());
                }
            }
            if (rememberOrderBy) {
                List<String> orderByValue = PaginationState.loadOrderBy(preference.getString("orderBy"));
                if (orderByValue != null) {
                    setOrderBy(orderByValue);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Not saving pagination state for user [" + userLogin.get("userLoginId") + "].", MODULE);
        }
    }

    /**
     * Serializes the orderBy list into a string.
     * @param orderBy the list of order by strings
     * @return a <code>String</code>
     */
    public static String serializeOrderBy(List<String> orderBy) {
        if (orderBy == null) {
            return null;
        }
        StringBuffer buff = new StringBuffer();
        for (Iterator<String> iter = orderBy.iterator(); iter.hasNext();) {
            buff.append(iter.next());
            if (iter.hasNext()) {
                buff.append(',');
            }
        }
        return buff.toString();
    }

    /**
     * Loads an order by string into a List.
     * @param orderByValue a <code>String</code> value
     * @return a <code>List</code> value
     */
    public static List<String> loadOrderBy(String orderByValue) {
        if (UtilValidate.isEmpty(orderByValue)) {
            return null;
        }
        return Arrays.asList(orderByValue.split("\\s*,\\s*"));
    }
}
