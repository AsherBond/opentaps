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

package org.opentaps.gwt.common.server.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * This is the wrapper RPC service to call ofbiz services.
 */
public abstract class GenericService {

    private static final String MODULE = GenericService.class.getName();

    private InputProviderInterface provider;
    // can be used to hold temporary service parameters instead of getting them from the provider
    private Map<String, Object> serviceParameters;
    // store errors related to input
    private Map<String, String> customErrors; // custom errors, used for special validation, for example a field that must have a specific format
    private List<String> missingFields; // missing field error, a required field was missing
    private List<String> extraFields; // extra field error, an unexpected field was given
    // store service results
    private Map<String, Object> callResults;

    /**
     * Creates a <code>GenericService</code> instance from an <code>InputProviderInterface</code>.
     * @param provider an <code>InputProviderInterface</code>
     */
    public GenericService(InputProviderInterface provider) {
        this.provider = provider;
        customErrors = new HashMap<String, String>();
        extraFields = new ArrayList<String>();
        missingFields = new ArrayList<String>();
    }

    /**
     * Gets the <code>InputProviderInterface</code>.
     * @return the <code>InputProviderInterface</code>
     */
    public InputProviderInterface getProvider() {
        return this.provider;
    }

    /**
     * A placeholder method that can be overriden in derived class to implement
     *  input validation complementary to the service engine validation.
     * @see #addFieldError(String, String)
     * @see #addExtraFieldError(String)
     * @see #addMissingFieldError(String)
     */
    public void validate() { }

    /**
     * An utility method that can be used in {@link #validate()} to
     * check if a required parameter is present.
     * @param fieldName the parameter name
     * @return <code>true</code> if the parameter is present
     */
    public final boolean validateParameterPresent(String fieldName) {
        if (!provider.parameterIsPresent(fieldName)) {
            addMissingFieldError(fieldName);
            return false;
        }
        return true;
    }

    /**
     * An utility method to mark an extra (unexpected) field.
     * There is visual clue or error message in the client UI currently.
     * @param fieldName the name of the missing field in the client form
     */
    public final void addExtraFieldError(String fieldName) {
        extraFields.add(fieldName);
    }

    /**
     * An utility method to mark a missing required field.
     * There will be a visual clue and an error message in the client UI
     *  (simply because there is no such field in the UI).
     * @param fieldName the name of the missing field in the client form
     */
    public final void addMissingFieldError(String fieldName) {
        missingFields.add(fieldName);
    }

    /**
     * An utility method to set an arbitrary error message to the client field.
     * There will be a visual clue and an error message in the client UI if the
     *  field is present in the form.
     * @param fieldName the name of the missing field in the client form
     * @param error the error message
     */
    public final void addFieldError(String fieldName, String error) {
        customErrors.put(fieldName, error);
    }

    /**
     * Checks if there is any kind of validation error.
     * @return <code>true</code> if there is any kind of validation error
     */
    public final boolean hasValidationErrors() {
        return !(customErrors.isEmpty() && missingFields.isEmpty() && extraFields.isEmpty());
    }

    /**
     * Sets the service parameters to use instead of getting them from the provider; you need to reset them to null
     * in order to use the provider parameters again.
     * @param serviceParameters the service parameters to use in the <code>callService</code> method, <code>null</code> to use the provider parameters
     */
    public void setServiceParameters(Map<String, Object> serviceParameters) {
        this.serviceParameters = serviceParameters;
    }

    /**
     * The placeholder where derived classes make the call to the service.
     * @return the service result <code>Map</code>
     * @throws GenericServiceException if an error occurs
     * @see #call()
     */
    protected abstract Map<String, Object> callService() throws GenericServiceException;

    /**
     * Most basic implementation of <code>callService</code>.
     * Makes the service input valid from the parameters (except if some parameters are missing)
     *  with the addition of the userLogin, and return the service response <code>Map</code>.
     * @param serviceName name of the service to call
     * @return the service result <code>Map</code>
     * @throws GenericServiceException if an error occurs, such as a validation error or a service error
     */
    protected final Map<String, Object> callService(String serviceName) throws GenericServiceException {
        Map<String, Object> params;
        if (serviceParameters == null) {
            params = provider.getParameterMap();
            Debug.logInfo("Using provider's parameter map", MODULE);
        } else {
            params = serviceParameters;
            Debug.logInfo("Using specific parameter map", MODULE);
        }
        return callService(serviceName, params);
    }

    /**
     * Most basic implementation of <code>callService</code>.
     * Makes the service input valid from the parameters (except if some parameters are missing)
     *  with the addition of the userLogin, and return the service response <code>Map</code>.
     * @param serviceName name of the service to call
     * @param parameters a <code>Map</code> of parameters from the service, it will passed to <code>makeValidContext</code>
     * @return the service result <code>Map</code>
     * @throws GenericServiceException if an error occurs, such as a validation error or a service error
     */
    @SuppressWarnings("unchecked")
    protected final Map<String, Object> callService(String serviceName, Map<String, Object> parameters) throws GenericServiceException {
        DispatchContext dctx = provider.getInfrastructure().getDispatcher().getDispatchContext();
        Map callCtxt = dctx.makeValidContext(serviceName, "IN", interceptParameters(parameters));
        callCtxt.put("userLogin", provider.getUser().getOfbizUserLogin());
        Debug.logInfo("Calling service [" + serviceName + "] with input [" + callCtxt + "]", MODULE);
        callResults = provider.getInfrastructure().getDispatcher().runSync(serviceName, callCtxt);
        if (callResults == null) {
            throw new GenericServiceException("Empty service response");
        }
        return callResults;
    }

    /**
     * Placeholder method that allow processing of parameters before they are passed to the service call.
     * @param params a <code>Map</code> of parameter name to parameter value
     * @return the <code>Map</code> of parameter name to parameter value that is passed to the service
     */
    @SuppressWarnings("unchecked")
    protected Map interceptParameters(Map params) {
        return params;
    }

    /**
     * Entry method for the service calling class.
     * First validates the parameters, and throw an exception if a validation error is found.
     * Then calls the implementation of <code>callService</code> which should return the service
     * response <code>Map</code> or throw a <code>GenericServiceException</code>.
     * @return the service result <code>Map</code>
     * @throws GenericServiceException if an error occurs, such as a validation error or a service error
     */
    public final Map<String, Object> call() throws GenericServiceException {
        validate();
        checkValidationErrors();
        return callService();
    }

    /**
     * Checks if any validation errors are present.
     * Those are added by the <code>addFieldError</code> methods.
     * @throws CustomServiceValidationException if a validation error is present
     */
    public final void checkValidationErrors() throws CustomServiceValidationException {
        if (hasValidationErrors()) {
            throw new CustomServiceValidationException(missingFields, extraFields, customErrors);
        }
    }

}
