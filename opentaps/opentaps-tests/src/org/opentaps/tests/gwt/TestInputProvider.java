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

package org.opentaps.tests.gwt;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * Implementation of the <code>InputProviderInterface</code> for testing GWT services.
 */
public class TestInputProvider implements InputProviderInterface {

    private Locale locale;
    private TimeZone timeZone;
    private User user;
    private Infrastructure infrastructure;
    private DomainsDirectory domainsDirectory;
    private Map<String, String> parameters;

    /**
     * Creates a new <code>HttpInputProvider</code> instance.
     * @param userLogin a <code>GenericValue</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @exception InfrastructureException if an error occurs
     */
    public TestInputProvider(GenericValue userLogin, LocalDispatcher dispatcher) throws InfrastructureException {
        this.user = new User(userLogin);
        this.locale = Locale.getDefault();
        this.timeZone = TimeZone.getDefault();
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
        Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(parameters);
        return map;
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
        return parameters.containsKey(name) ? parameters.get(name) : null;
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
