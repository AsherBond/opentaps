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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.ofbiz.security.Security;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.foundation.infrastructure.DomainContextInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
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
    public void setInfrastructure(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
        security = infrastructure.getSecurity();
    }

    /** {@inheritDoc} */
    public void setUser(User user) {
        this.user = user;
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
     * Convenience method to call an Ofbiz service wrapper, setting the user and infrastructure.
     * The service will NOT be run in a seprate transaction, since your service is probably inside of a transaction.
     * @param service the service to run
     * @throws ServiceException if an error occurs
     */
    public void runSync(ServiceWrapper service) throws ServiceException {
        runSync(service, getUser());
    }

    /**
     * Convenience method to call an Ofbiz service wrapper, setting the user and infrastructure.
     * The service will NOT be run in a seprate transaction, since your service is probably inside of a transaction.
     * @param service the service to run
     * @param user the <code>User</code> to run the service as
     * @throws ServiceException if an error occurs
     */
    public void runSync(ServiceWrapper service, User user) throws ServiceException {
        // run the service without creating a new transaction
        service.setUser(user);
        service.runSyncNoNewTransaction(getInfrastructure());
        if (service.isError()) {
            throw new ServiceException(service.getErrorMessage());
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

    /** {@inheritDoc} */
    public void checkPermission(String permission) throws ServiceException {
        if (!hasPermission(permission)) {
            try {
                String err = UtilMessage.getPermissionDeniedError(locale) + ": user [" + getUser().getUserId() + "] does not have permission " + permission;
                throw new ServiceException(err);
            } catch (InfrastructureException e) {
                throw new ServiceException(e);
            }
        }
    }

    /** {@inheritDoc} */
    public void checkEntityPermission(String entity, String action) throws ServiceException {
        if (!hasEntityPermission(entity, action)) {
            try {
                String err = UtilMessage.getPermissionDeniedError(locale) + ": user [" + getUser().getUserId() + "] does not have permission " + entity + " " + action;
                throw new ServiceException(err);
            } catch (InfrastructureException e) {
                throw new ServiceException(e);
            }
        }
    }
    
    /**
     * Convenience method to call an Ofbiz service wrapper, setting the user and infrastructure.
     * The service will NOT be run in a seprate transaction, since your service is probably inside of a transaction.
     * @param service the service to run
     * @throws ServiceException if an error occurs
     */
    public void runAsync(ServiceWrapper service) throws ServiceException {
        runAsync(service, getUser());
    }

    /**
     * Convenience method to call an Ofbiz service wrapper, setting the user and infrastructure.
     * The service will NOT be run in a seprate transaction, since your service is probably inside of a transaction.
     * @param service the service to run
     * @param user the <code>User</code> to run the service as
     * @throws ServiceException if an error occurs
     */
    public void runAsync(ServiceWrapper service, User user) throws ServiceException {
        // run the service without creating a new transaction
        service.setUser(user);
        service.runAsync(getInfrastructure());
        if (service.isError()) {
            throw new ServiceException(service.getErrorMessage());
        }
    }
}
