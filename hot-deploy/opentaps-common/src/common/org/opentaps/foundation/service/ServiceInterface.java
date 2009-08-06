/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.foundation.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.ofbiz.security.Security;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * Common interface for all POJO services.
 */
public interface ServiceInterface {

    /**
     * Sets the domain <code>Infrastructure</code> and the ofbiz <code>Security</code> object.
     * @param infrastructure an <code>Infrastructure</code> value
     * @throws ServiceException if an error occurs
     */
    void setInfrastructure(Infrastructure infrastructure) throws ServiceException;

    /**
     * Sets the <code>User</code> which define permissions.
     * @param user an <code>User</code> value
     * @throws ServiceException if an error occurs
     */
    void setUser(User user) throws ServiceException;

    /**
     * Sets the <code>Locale</code>, if not set the service will use the system default locale.
     * @param locale a <code>Locale</code> value
     * @throws ServiceException if an error occurs
     */
    void setLocale(Locale locale) throws ServiceException;

    /**
     * Sets the <code>TimeZone</code>, if not set the service will use the system default time zone.
     * @param timeZone a <code>TimeZone</code> value
     * @throws ServiceException if an error occurs
     */
    void setTimeZone(TimeZone timeZone) throws ServiceException;

    /**
     * Gets the <code>User</code>.
     * @return an <code>User</code> value
     */
    User getUser();

    /**
     * Gets the domain <code>Infrastructure</code>.
     * @return an <code>Infrastructure</code> value
     */
    Infrastructure getInfrastructure();

    /**
     * Loads the <code>DomainsDirectory</code> from the current <code>User</code> and <code>Infrastructure</code>.
     * @see #getDomainsDirectory
     */
    void loadDomainsDirectory();

    /**
     * Gets the <code>DomainsDirectory</code> which can be used to access other domains.
     * @return a <code>DomainsDirectory</code> value
     */
    DomainsDirectory getDomainsDirectory();

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
     * @param entity the name of the Entity corresponding to the desired permission
     * @param action the action on the Entity corresponding to the desired permission
     * @return <code>true</code> if the current user has the requested permission
     * @throws ServiceException if the <code>Security</code> or <code>User</code> is not set
     */
    public boolean hasEntityPermission(String entity, String action) throws ServiceException;
}
