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

import java.util.Map;

import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * This is the base class for the pojo service wrappers.
 * They wrap the input / output <code>Map</code> and give
 *  type safe accessors to the parameters.
 */
public abstract class ServiceWrapper {

    private User user;

    /**
     * Creates a new <code>ServiceWrapper</code> instance.
     */
    public ServiceWrapper() { }

    /**
     * Creates a new <code>ServiceWrapper</code> instance with the given <code>User</code>.
     * @param user an <code>User</code> value
     */
    public ServiceWrapper(User user) {
        super();
        this.setUser(user);
    }

    /**
     * Gets the <code>User</code> instance of this service.
     * @return an <code>User</code> value
     */
    public User getUser() {
        return this.user;
    }

    /**
     * Sets the <code>User</code> instance that can be used when running the service.
     * @param user an <code>User</code> value
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Gets the service name as used by the service engine.
     * @return the service engine name
     */
    public abstract String name();

    /**
     * Checks if the services uses a transaction.
     * @return if the service uses a transaction
     */
    public abstract Boolean usesTransaction();

    /**
     * Checks if the services requires a new transaction.
     * @return if the service requires a new transaction
     */
    public abstract Boolean requiresNewTransaction();

    /**
     * Checks if the services requires authentication.
     * @return if the service requires authentication
     */
    public abstract Boolean requiresAuthentication();

    /**
     * Gets the service input <code>Map</code> (can be passed to the dispatcher).
     * @return the service input <code>Map</code>
     */
    public abstract Map<String, Object> inputMap();

    /**
     * Gets the service output <code>Map</code>.
     * @return the service output <code>Map</code>
     */
    public abstract Map<String, Object> outputMap();

    /**
     * Sets all fields from the given input <code>Map</code>.
     * @param mapValue the service input <code>Map</code>
     */
    public abstract void putAllInput(Map<String, Object> mapValue);

    /**
     * Sets all fields from the given output <code>Map</code>.
     * @param mapValue the service output <code>Map</code>
     */
    public abstract void putAllOutput(Map<String, Object> mapValue);

    /**
     * Runs the service with the wrapper's inputs, and set the outputs.
     * @param infrastructure an <code>Infrastructure</code> value
     * @exception ServiceException if an error occurs
     */
    public void runSync(Infrastructure infrastructure) throws ServiceException {
        try {
            putAllOutput(infrastructure.getDispatcher().runSync(name(), inputMap()));
        } catch (GenericServiceException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Runs the service with the wrapper's inputs, and set the outputs.
     * @param infrastructure an <code>Infrastructure</code> value
     * @exception ServiceException if an error occurs
     */
    public void runSyncNoNewTransaction(Infrastructure infrastructure) throws ServiceException {
        runSync(infrastructure, -1, false);
    }

    /**
     * Runs the service with the wrapper's inputs, and set the outputs.
     * This overrides the default transaction timeout and require transaction settings.
     * @param infrastructure an <code>Infrastructure</code> value
     * @param transactionTimeout the overriding timeout for the transaction (if we started it), use <code>-1</code> to use the service default
     * @param requireNewTransaction if true we will suspend and create a new transaction so we are sure to start
     * @exception ServiceException if an error occurs
     */
    public void runSync(Infrastructure infrastructure, int transactionTimeout, boolean requireNewTransaction) throws ServiceException {
        try {
            putAllOutput(infrastructure.getDispatcher().runSync(name(), inputMap(), transactionTimeout, requireNewTransaction));
        } catch (GenericServiceException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Checks if the service response is an error.
     * @return a <code>boolean</code> value
     */
    public Boolean isError() {
        return ServiceUtil.isError(this.outputMap());
    }

    /**
     * Checks if the service response is a failure.
     * @return a <code>boolean</code> value
     */
    public Boolean isFailure() {
        return ServiceUtil.isFailure(this.outputMap());
    }

    /**
     * Checks if the service response is a success.
     * @return a <code>boolean</code> value
     */
    public Boolean isSuccess() {
        return UtilCommon.isSuccess(this.outputMap());
    }

    /**
     * Gets the service error message string if any.
     * @return a <code>String</code> value
     */
    public String getErrorMessage() {
        return ServiceUtil.getErrorMessage(this.outputMap());
    }

    
    /**
     * Runs the service with the wrapper's inputs
     * @param infrastructure an <code>Infrastructure</code> value
     * @exception ServiceException if an error occurs
     */
    public void runAsync(Infrastructure infrastructure) throws ServiceException {
        runAsync(infrastructure, true);
    }

    /**
     * Runs the service with the wrapper's inputs
     * This overrides the default transaction timeout and require transaction settings.
     * @param infrastructure an <code>Infrastructure</code> value
     * @param persist True for store/run; False for run.
     * @exception ServiceException if an error occurs
     */
    public void runAsync(Infrastructure infrastructure, boolean persist) throws ServiceException {
        try {
            infrastructure.getDispatcher().runAsync(name(), inputMap(), persist);
        } catch (GenericServiceException e) {
            throw new ServiceException(e);
        }
    }

}
