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
package org.opentaps.foundation.action;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.foundation.infrastructure.DomainContextInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * A wrapper for the Action methods.
 * This class wraps around the context <code>Map</code> to provide type safe and
 *  convenient methods.
 */
public class ActionContext implements DomainContextInterface {

    private static final String MODULE = ActionContext.class.getName();

    private Map<String, Object> context;
    private User user;
    private Infrastructure infrastructure;
    private TimeZone timeZone;
    private ResourceBundleMapWrapper uiLabelMap;

    /**
     * Default constructor, wraps around the context <code>Map</code>.
     * @param context the original context <code>Map</code>
     */
    public ActionContext(Map<String, Object> context) {
        this.context = context;
    }

    /**
     * Gets the original context <code>Map</code>.
     * @return a <code>Map</code> value
     */
    public final Map<String, Object> getContext() {
        return context;
    }

    /**
     * Simple wrapper for context get method.
     * @param key a <code>String</code> value
     * @return an <code>Object</code> value
     */
    public Object get(String key) {
        return context.get(key);
    }

    /**
     * Simple wrapper for context put method.
     * @param key a <code>String</code> value
     * @param value an <code>Object</code> value
     * @return an <code>Object</code> value
     */
    public Object put(String key, Object value) {
        return context.put(key, value);
    }

    /**
     * Gets the <code>HttpServletRequest</code> from the context.
     * @return a <code>HttpServletRequest</code> value
     */
    public HttpServletRequest getRequest() {
        return (HttpServletRequest) get("request");
    }

    /**
     * Gets the Delegator from the context.
     * @return a <code>Delegator</code> value
     */
    public Delegator getDelegator() {
        return (Delegator) get("delegator");
    }

    /**
     * Gets the Dispatcher from the context.
     * @return a <code>LocalDispatcher</code> value
     */
    public LocalDispatcher getDispatcher() {
        return (LocalDispatcher) get("dispatcher");
    }

    /**
     * Gets the <code>Locale</code> from the context.
     * @return a <code>Locale</code> value
     */
    public Locale getLocale() {
        return UtilCommon.getLocale(context);
    }

    /**
     * Gets the <code>TimeZone</code> from the context, falling back to the request.
     * @return a <code>TimeZone</code> value
     */
    public TimeZone getTimeZone() {
        if (timeZone == null) {
            timeZone = (TimeZone) get("timeZone");
            if (timeZone == null) {
                if (getRequest() != null) {
                    timeZone = UtilCommon.getTimeZone(getRequest());
                }
            }
        }
        return timeZone;
    }

    /**
     * Gets the <code>UserLogin</code> from the context.
     * @return a <code>GenericValue</code> value
     */
    public GenericValue getUserLogin() {
        return (GenericValue) get("userLogin");
    }

    /**
     * Gets the <code>User</code> from the context.
     * @return a <code>GenericValue</code> value
     */
    public User getUser() {
        if (user == null) {
            GenericValue ul = getUserLogin();
            if (ul != null) {
                user = new User(getUserLogin());
            }
        }
        return user;
    }

    /** {@inheritDoc} */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Gets the <code>Infrastructure</code> from the context.
     * @return an <code>Infrastructure</code> value
     */
    public Infrastructure getInfrastructure() {
        if (infrastructure == null) {
            if (getDispatcher() == null) {
                Debug.logError("No dispatcher set in the context, cannot build an Infrastructure instance.", MODULE);
            }
            infrastructure = new Infrastructure(getDispatcher());
        }
        return infrastructure;
    }

    /** {@inheritDoc} */
    public void setInfrastructure(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }

    /** {@inheritDoc} */
    public void setDomainContext(DomainContextInterface context) {
        this.setDomainContext(context.getInfrastructure(), context.getUser());
    }

    /** {@inheritDoc} */
    public void setDomainContext(Infrastructure infrastructure, User user) {
        this.setInfrastructure(infrastructure);
        this.setUser(user);
    }

    /**
     * Gets the <code>Security</code> from the context.
     * @return a <code>Security</code> value
     */
    public Security getSecurity() {
        return (Security) get("security");
    }

    /**
     * Checks if the current user has the requested permission.
     * Simple wrapper to the commonly used security method.
     * @param permission a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasPermission(String permission) {
        return getSecurity().hasPermission(permission, getUserLogin());
    }

    /**
     * Checks if the current user has the requested permission.
     * Simple wrapper to the commonly used security method.
     * @param entity a <code>String</code> value
     * @param action a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasEntityPermission(String entity, String action) {
        return getSecurity().hasEntityPermission(entity, action, getUserLogin());
    }

    /**
     * Gets a parameter from the request using <code>UtilCommon.getParameter</code>.
     * @param parameterName a <code>String</code> value
     * @return a <code>String</code> value or null if the parameter value is empty
     */
    public String getParameter(String parameterName) {
        return UtilCommon.getParameter(getRequest(), parameterName);
    }

    /**
     * Gets the <code>uiLabelMap</code> for the context locale.
     * @return a <code>ResourceBundleMapWrapper</code> value
     */
    public ResourceBundleMapWrapper getUiLabels() {
        if (uiLabelMap == null) {
            uiLabelMap = UtilMessage.getUiLabels(getLocale());
        }
        return uiLabelMap;
    }

    /**
     * Gets a label expanded for the context locale.
     * For more complex label operations see <code>UtilMessage</code>.
     * @param label a <code>String</code> value
     * @return a <code>String</code> value
     */
    public String getUiLabel(String label) {
        return (String) getUiLabels().get(label);
    }

    // some autoboxing getter

    /**
     * Gets a value from the context as a <code>String</code>.
     * @param key a <code>String</code> value
     * @return a <code>String</code> value
     */
    public String getString(String key) {
        return (String) get(key);
    }

    /**
     * Gets a value from the context as a <code>Timestamp</code>.
     * @param key a <code>String</code> value
     * @return a <code>String</code> value
     */
    public Timestamp getTimestamp(String key) {
        return (Timestamp) get(key);
    }

    /**
     * Gets a value from the context as a <code>BigDecimal</code>.
     * @param key a <code>String</code> value
     * @return a <code>String</code> value
     */
    public BigDecimal getBigDecimal(String key) {
        return (BigDecimal) get(key);
    }

    /**
     * Gets a value from context as a <code>Boolean</code> if it is instance of
     * <code>Boolean</code> or a string convertible to boolean.
     * @param key a <code>String</code> value
     * @return a <code>Boolean</code> value
     */
    public Boolean getBoolean(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.valueOf((String) value);
        }

        throw new IllegalArgumentException(String.format("There is %1$s parameter in the context but it can not be considered a boolean value.", key));
    }

    /**
     * Gets a composite parameter from the request using <code>UtilHttp.makeParamValueFromComposite</code>.
     * @param parameterName a <code>String</code> value
     * @return a <code>String</code> value or null if the parameter value is empty
     */
    public String getCompositeParameter(String parameterName) {
        return UtilHttp.makeParamValueFromComposite(getRequest(), parameterName, getLocale());
    }
}
