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

package org.opentaps.gwt.common.server;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import java.util.TimeZone;

/**
 * The HTTP implementation of the <code>InputProviderInterface</code>.
 * This class is used for providing parameters to GWT services that are called
 * from an HTTP request handler.
 */
public class HttpInputProvider implements InputProviderInterface {

    private HttpServletRequest request;
    private HttpSession session;

    private TimeZone timeZone;
    private Locale locale;
    private User user;
    private Infrastructure infrastructure;
    private DomainsDirectory domainsDirectory;

    private Map<String, String> parameters;

    /**
     * Creates a new <code>HttpInputProvider</code> instance.
     * @param request a <code>HttpServletRequest</code> value
     * @throws InfrastructureException if an error occurs
     */
    public HttpInputProvider(HttpServletRequest request) throws InfrastructureException {
        this.request = request;
        this.session = request.getSession(true);
        this.user = new User((GenericValue) session.getAttribute("userLogin"));
        this.locale = UtilHttp.getLocale(request);
        this.timeZone = UtilHttp.getTimeZone(request);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.infrastructure = new Infrastructure(dispatcher);
        this.parameters = new HashMap<String, String>();
    }

    /** {@inheritDoc} */
    public Infrastructure getInfrastructure() {
        return infrastructure;
    }

    /** {@inheritDoc} */
    public User getUser() {
        return user;
    }

    /** {@inheritDoc} */
    public DomainsDirectory getDomainsDirectory() {
        if (domainsDirectory == null) {
            DomainsLoader domainsLoader = new DomainsLoader(getInfrastructure(), getUser());
            domainsDirectory = domainsLoader.loadDomainsDirectory();
        }
        return domainsDirectory;
    }

    /** {@inheritDoc} */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /** {@inheritDoc} */
    public Locale getLocale() {
        return locale;
    }

    /** {@inheritDoc} */
    public Map<String, Object> getParameterMap() {
        Map<String, Object> parameterMap = UtilHttp.getParameterMap(request);
        parameterMap.putAll(parameters);
        return parameterMap;
    }

    /** {@inheritDoc} */
    public void setParameterMap(Map<String, Object> parameters) {
        this.parameters.clear();
        for (String name : parameters.keySet()) {
            Object o = parameters.get(name);
            if (o instanceof String) {
                String s = (String) o;
                if (UtilValidate.isNotEmpty(s)) {
                    this.parameters.put(name, s);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public String getParameter(String name) {
        if (parameters.containsKey(name)) {
            return parameters.get(name);
        } else {
            String p = UtilCommon.getParameter(request, name);
            parameters.put(name, p);
            return p;
        }
    }

    /** {@inheritDoc} */
    public void setParameter(String name, String value) {
        if (UtilValidate.isNotEmpty(value)) {
            parameters.put(name, value);
        }
    }

    /** {@inheritDoc} */
    public boolean parameterIsPresent(String name) {
        return UtilValidate.isNotEmpty(getParameter(name));
    }

    /** {@inheritDoc} */
    public boolean oneParameterIsPresent(List<String> names) {
        for (String name : names) {
            if (parameterIsPresent(name)) {
                return true;
            }
        }
        return false;
    }

}
