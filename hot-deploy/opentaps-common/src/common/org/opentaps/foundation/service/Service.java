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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javolution.util.FastMap;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * This is  the master Service class for all frameworks, giving you the locale, User, and the Infrastructure.  It also automatically
 * provides you with the domains directory.
 * DO NOT MAKE THIS CLASS ABSTRACT.  in the POJOJavaEngine service engine, we will need to cast a service class to this super class
 * to access the setUser, setLocale, setTimezone methods.
 */
public class Service implements ServiceInterface {
    protected Locale locale = Locale.getDefault();
    protected TimeZone timeZone = TimeZone.getDefault();
    protected User user = null;
    protected Infrastructure infrastructure = null;
    protected Security security = null;
    protected DomainsDirectory domains = null;
    private List<String> successMessages = new ArrayList<String>();

    /**
     * Default constructor.
     */
    public Service() { }

    /**
     * Domain constructor.  Also sets the <code>security</code> object from <code>Infrastructure</code>
     * @param infrastructure an <code>Infrastructure</code> value
     * @param user an <code>User</code> value
     * @param locale a <code>Locale</code> value
     * @exception ServiceException if an error occurs
     */
    public Service(Infrastructure infrastructure, User user, Locale locale) throws ServiceException {
        setInfrastructure(infrastructure);
        security = infrastructure.getSecurity();
        setUser(user);
        setLocale(locale);
    }

    /** {@inheritDoc} */
    public void setInfrastructure(Infrastructure infrastructure) throws ServiceException {
        this.infrastructure = infrastructure;
        security = infrastructure.getSecurity();
    }

    /** {@inheritDoc} */
    public void setUser(User user) throws ServiceException {
        this.user = user;
    }

    /** {@inheritDoc} */
    public void setLocale(Locale locale) throws ServiceException {
        this.locale = locale;
    }

    /** {@inheritDoc} */
    public void setTimeZone(TimeZone timeZone) throws ServiceException {
        this.timeZone = timeZone;
    }

    /** {@inheritDoc} */
    public User getUser() {
        return user;
    }

    /** {@inheritDoc} */
    public Infrastructure getInfrastructure() {
        return infrastructure;
    }

    /** {@inheritDoc} */
    public void loadDomainsDirectory() {
        DomainsLoader dl = new DomainsLoader(getInfrastructure(), getUser());
        domains = dl.loadDomainsDirectory();
    }

    /** {@inheritDoc} */
    public DomainsDirectory getDomainsDirectory() {
        return domains;
    }

    /** {@inheritDoc} */
    public void setSuccessMessageRaw(String message) {
        successMessages.clear();
        successMessages.add(message);
    }

    /** {@inheritDoc} */
    public void setSuccessMessage(String label) {
        setSuccessMessageRaw(expandLabel(label));
    }

    /** {@inheritDoc} */
    public void setSuccessMessage(String label, Map<String, String> context) {
        setSuccessMessageRaw(expandLabel(label, context));
    }

    /** {@inheritDoc} */
    public void addSuccessMessageRaw(String message) {
        successMessages.add(message);
    }

    /** {@inheritDoc} */
    public void addSuccessMessage(String label) {
        addSuccessMessageRaw(expandLabel(label));
    }

    /** {@inheritDoc} */
    public void addSuccessMessage(String label, Map<String, String> context) {
        addSuccessMessageRaw(expandLabel(label, context));
    }

    /** {@inheritDoc} */
    public List<String> getSuccessMessages() {
        return successMessages;
    }

    /**
     * Convenience method to generate an input <code>Map</code> to be passed to an Ofbiz service which sets the current <code>UserLogin</code>.
     * @return a <code>Map</code> with the current <code>User</code> <code>UserLogin</code>
     */
    public Map<String, Object> createInputMap() {
        Map<String, Object> input = FastMap.newInstance();
        input.put("userLogin", getUser().getOfbizUserLogin());
        return input;
    }

    /**
     * Convenience method to call an Ofbiz service and return the results <code>Map</code>.
     * The service will NOT be run in a seprate transaction, since your service is probably inside of a transaction.
     * @param serviceName the service to run
     * @param input the service input <code>Map</code>
     * @return the service results <code>Map</code>
     * @throws ServiceException if an error occurs
     */
    public Map<String, Object> runSync(String serviceName, Map<String, Object> input) throws ServiceException {
        try {
            // run the service without creating a new transaction
            Map<String, Object> results = getInfrastructure().getDispatcher().runSync(serviceName, input, -1, false);
            if (ServiceUtil.isError(results)) {
                throw new ServiceException(ServiceUtil.getErrorMessage(results));
            }
            return results;
        } catch (GenericServiceException e) {
            throw new ServiceException(e);
        }
    }

    protected String expandLabel(String label) {
        return UtilMessage.expandLabel(label, locale);
    }

    protected String expandLabel(String label, Map<String, String> context) {
        return UtilMessage.expandLabel(label, locale, context);
    }

    /** {@inheritDoc} */
    public Security getSecurity() {
        return security;
    }

    /** {@inheritDoc} */
    public boolean hasPermission(String permission) throws ServiceException {
        if (getSecurity() == null) {
            throw new ServiceException("No Security set");
        }
        if (getUser() == null) {
            throw new ServiceException("No User set");
        }

        return getSecurity().hasPermission(permission, getUser().getOfbizUserLogin());
    }

    /** {@inheritDoc} */
    public boolean hasEntityPermission(String entity, String action) throws ServiceException {
        if (getSecurity() == null) {
            throw new ServiceException("No Security set");
        }
        if (getUser() == null) {
            throw new ServiceException("No User set");
        }

        return getSecurity().hasEntityPermission(entity, action, getUser().getOfbizUserLogin());
    }
}
