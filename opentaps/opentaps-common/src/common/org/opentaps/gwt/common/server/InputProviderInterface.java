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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.opentaps.domain.DomainsDirectory;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * Interface for GWT services input providers.
 *
 * Existing implementations are <code>HttpInputProvider</code> which reads from the
 *  HTTP request, and <code>TestInputProvider</code> which reads parameters from a <code>Map</code>.
 */
public interface InputProviderInterface {

    /**
     * Gets the domain <code>User</code>.
     * @return the domain <code>User</code>
     */
    public User getUser();

    /**
     * Gets the domain <code>Infrastructure</code>.
     * @return the domain <code>Infrastructure</code>
     */
    public Infrastructure getInfrastructure();

    /**
     * Gets the <code>DomainsDirectory</code>.
     * @return the <code>DomainsDirectory</code>
     */
    public DomainsDirectory getDomainsDirectory();

    /**
     * Gets the current <code>Locale</code>.
     * @return the current <code>Locale</code>
     */
    public Locale getLocale();

    /**
     * Gets the current <code>TimeZone</code>.
     * @return the current <code>TimeZone</code>
     */
    public TimeZone getTimeZone();

    /**
     * Gets the <code>Map</code> of all given parameters.
     * Original parameters always have string values, but this is normally used to create the services call
     * context and this is why it returns <code>Map<String, Object></code>.
     * @return a <code>Map</code> value
     */
    public Map<String, Object> getParameterMap();

    /**
     * Sets all the given parameters.
     * @param parameters a <code>Map</code> of name: value
     */
    public void setParameterMap(Map<String, Object> parameters);

    /**
     * Gets the parameter for the given name.
     * @param name a <code>String</code> value
     * @return the parameter <code>String</code> value, or <code>null</code> if the parameter was not found or empty
     */
    public String getParameter(String name);

    /**
     * Sets the parameter for the given name.
     * @param name a <code>String</code> value
     * @param value a <code>String</code> value
     */
    public void setParameter(String name, String value);

    /**
     * Checks if the parameter for the given name is present and not empty.
     * @param name a <code>String</code> value
     * @return <code>true</code> if the parameter is found and not empty
     */
    public boolean parameterIsPresent(String name);

    /**
     * Checks if one of the parameters for the given names is present and not empty.
     * @param names a <code>List</code> of <code>String</code> values
     * @return <code>true</code> if one of the parameter is found and not empty
     */
    public boolean oneParameterIsPresent(List<String> names);
}
