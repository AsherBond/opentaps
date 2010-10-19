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
package org.opentaps.foundation.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.ofbiz.security.Security;
import org.opentaps.foundation.infrastructure.DomainContextInterface;

/**
 * Common interface for all POJO services.
 */
public interface ServiceInterface extends DomainContextInterface {

    /**
     * Sets the <code>Locale</code>, if not set the service will use the system default locale.
     * @param locale a <code>Locale</code> value
     * @throws ServiceException if an error occurs
     */
    public void setLocale(Locale locale) throws ServiceException;

    /**
     * Sets the <code>TimeZone</code>, if not set the service will use the system default time zone.
     * @param timeZone a <code>TimeZone</code> value
     * @throws ServiceException if an error occurs
     */
    public void setTimeZone(TimeZone timeZone) throws ServiceException;

    /**
     * Sets the service success message to be returned.
     * Discouraged, messages returned to the user should be localized, use only for prototyping.
     * @param message a <code>String</code> value
     * @see #setSuccessMessage(String)
     * @see #setSuccessMessage(String, Map)
     */
    public void setSuccessMessageRaw(String message);

    /**
     * Sets the service success message to be returned.
     * @param label a label that will be expanded according to the current locale
     * @see #setSuccessMessage(String, Map)
     */
    public void setSuccessMessage(String label);

    /**
     * Sets the service success message to be returned.
     * @param label a label that will be expanded according to the current locale
     * @param context a <code>Map</code> to be used for substitution in the label
     * @see #setSuccessMessage(String)
     */
    public void setSuccessMessage(String label, Map<String, String> context);

    /**
     * Adds a service success message to the list to be returned.
     * Discouraged, messages returned to the user should be localized, use only for prototyping.
     * @param message a <code>String</code> value
     * @see #addSuccessMessage(String)
     * @see #addSuccessMessage(String, Map)
     */
    public void addSuccessMessageRaw(String message);

    /**
     * Adds a service success message to the list to be returned.
     * @param label a label that will be expanded according to the current locale
     * @see #addSuccessMessage(String, Map)
     */
    public void addSuccessMessage(String label);

    /**
     * Adds a service success message to the list to be returned.
     * @param label a label that will be expanded according to the current locale
     * @param context a <code>Map</code> to be used for substitution in the label
     * @see #addSuccessMessage(String)
     */
    public void addSuccessMessage(String label, Map<String, String> context);

    /**
     * Gets the service success messages.
     * @return a list of <code>String</code> value
     */
    public List<String> getSuccessMessages();

    /**
     * Gets the <code>Security</code> instance attached to this service.
     * @return a <code>Security</code> object
     */
    public Security getSecurity();

    /**
     * Checks if the current user has the requested permission.
     * @param permission the permission group to test for
     * @return <code>true</code> if the current user has the requested permission
     * @throws ServiceException if the <code>Security</code> or <code>User</code> is not set
     */
    public boolean hasPermission(String permission) throws ServiceException;

    /**
     * Checks if the current user has the requested entity permission.
     * Entity permissions are given in a form so that ENTITY + ACTION gives a permission ID,
     *  and if the user has the "_ADMIN" action permission, any action is allowed.
     *  for example <code>hasEntityPermission("MYAPP_CONTACT", "_VIEW")</code> succeeds
     *  if the user has <code>MYAPP_CONTACT_VIEW</code> or <code>MYAPP_CONTACT_ADMIN</code>.
     * @param entity the name of the Entity corresponding to the desired permission
     * @param action the action on the Entity corresponding to the desired permission
     * @return <code>true</code> if the current user has the requested permission
     * @throws ServiceException if the <code>Security</code> or <code>User</code> is not set
     */
    public boolean hasEntityPermission(String entity, String action) throws ServiceException;

    /**
     * Checks if the current user has the requested permission or throw a service exception
     * with a standard error message.
     * @param permission the permission group to test for
     * @throws ServiceException if the permission was denied, or if the <code>Security</code> or <code>User</code> is not set
     * @see #hasPermission(String)
     */
    public void checkPermission(String permission) throws ServiceException;

    /**
     * Checks if the current user has the requested entity permission or throw a service
     * exception with a standard error message.
     * Entity permissions are given in a form so that ENTITY + ACTION gives a permission ID,
     *  and if the user has the "_ADMIN" action permission, any action is allowed.
     *  for example <code>hasEntityPermission("MYAPP_CONTACT", "_VIEW")</code> succeeds
     *  if the user has <code>MYAPP_CONTACT_VIEW</code> or <code>MYAPP_CONTACT_ADMIN</code>.
     * @param entity the name of the Entity corresponding to the desired permission
     * @param action the action on the Entity corresponding to the desired permission
     * @throws ServiceException if the permission was denied, or if the <code>Security</code> or <code>User</code> is not set
     * @see #hasPermission(String)
     */
    public void checkEntityPermission(String entity, String action) throws ServiceException;
}
