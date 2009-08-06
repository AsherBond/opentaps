/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.gwt.common.server.form;

import java.util.Collection;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * Generic RPC service used to create, update or delete entities.
 */
public abstract class GenericCUDService extends GenericService {

    private static final String MODULE = GenericCUDService.class.getName();

    // for CUD services
    private CUDAction requestedAction = CUDAction.NA;
    private boolean isBatchAction = false;

    /** Defines the possible CUD actions. */
    public static enum CUDAction {
        /** The service should create a new record. */
        CREATE(UtilLookup.PARAM_CUD_ACTION_CREATE),
        /** The service should update the given record. */
        UPDATE(UtilLookup.PARAM_CUD_ACTION_UPDATE),
        /** The service should delete the given record. */
        DELETE(UtilLookup.PARAM_CUD_ACTION_DELETE),
        /** Invalid action was given. */
        NA("");
        private final String value;
        private CUDAction(String value) {
            this.value = value;
        }
        /**
         * Used to compare an action and the given parameter.
         * @param value the parameter value
         * @return <code>true</code> if the parameter value corresponds to the action
         */
        public boolean equals(String value) {
            return this.value.equals(value);
        }
    }

    /**
     * Creates a <code>GenericCUDService</code> instance from an <code>InputProviderInterface</code>.
     * @param provider an <code>InputProviderInterface</code>
     */
    public GenericCUDService(InputProviderInterface provider) {
        super(provider);
    }

    /**
     * An utility method that can be used in {@link #validate()} to
     * check if the CUD action parameter is set and valid.
     * @return <code>true</code> if the CUD action is present and valid
     */
    public boolean validateCUDAction() {
        return validateCUDAction(getProvider().getParameter(UtilLookup.PARAM_CUD_ACTION));
    }

    /**
     * Checks if the given value is a valid CUD action parameter.
     * @param value the value to test
     * @return <code>true</code> if the CUD action is present and valid
     */
    protected boolean validateCUDAction(String value) {
        if (!UtilValidate.isEmpty(value)) {
            if (CUDAction.CREATE.equals(value)) {
                requestedAction = CUDAction.CREATE;
                return true;
            } else if (CUDAction.UPDATE.equals(value)) {
                requestedAction = CUDAction.UPDATE;
                return true;
            } else if (CUDAction.DELETE.equals(value)) {
                requestedAction = CUDAction.DELETE;
                return true;
            }
        }
        addMissingFieldError(UtilLookup.PARAM_CUD_ACTION);
        return false;
    }

    /**
     * Gets the requested CUD Action.
     * @return a <code>CUDAction</code> value
     */
    public CUDAction getRequestedCUDAction() {
        return requestedAction;
    }

    /**
     * Checks if a batch call was requested.
     * @return a <code>boolean</code> value
     */
    public boolean isBatchAction() {
        return isBatchAction;
    }

    /**
     * Calls the appropriate method according to the <code>CUDAction</code> set in the request.
     * An implementation of <code>GenericCUDService</code> may override this method (if no <code>CUDAction</code> is passed to the service) or
     * the <code>callCreateService</code>, <code>callUpdateService</code> and <code>callDeleteService</code> methods.
     * @see #getRequestedCUDAction
     * @see #callCreateService
     * @see #callUpdateService
     * @see #callDeleteService
     */
    @Override
    protected Map<String, Object> callService() throws GenericServiceException {
        if (CUDAction.CREATE.equals(getRequestedCUDAction())) {
            return callCreateService();
        } else if (CUDAction.UPDATE.equals(getRequestedCUDAction())) {
            return callUpdateService();
        } else if (CUDAction.DELETE.equals(getRequestedCUDAction())) {
            return callDeleteService();
        } else {
            throw new GenericServiceException("No action was given.");
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> callServiceBatch() throws GenericServiceException {
        Collection<Map<String, Object>> data = UtilHttp.parseMultiFormData(getProvider().getParameterMap());
        int counter = 0;
        isBatchAction = true;
        // wrap all in a transaction
        try {
            TransactionUtil.begin();
        } catch (GenericTransactionException e) {
            Debug.logError(e, MODULE);
            throw new GenericServiceException(e);
        }
        for (Map<String, Object> options : data) {
            // copy this record parameters to the provider
            Debug.logInfo("Batch posting data [" + options + "]", MODULE);
            getProvider().setParameterMap(options);
            if (!validateCUDAction()) {
                Debug.logError("No valid action was given.", MODULE);
                throw new GenericServiceException("No valid action was given.");
            }

            if (counter == 0) {
                prepareBatch();
            }

            Map<String, Object> results = callService();
            if (ServiceUtil.isError(results)) {
                Debug.logError("Service error while processing batch: " + ServiceUtil.getErrorMessage(results), MODULE);
                throw new GenericServiceException(ServiceUtil.getErrorMessage(results));
            }
            counter++;
        }
        finalizeBatch();
        try {
            TransactionUtil.commit();
        } catch (GenericTransactionException e) {
            Debug.logError(e, MODULE);
            throw new GenericServiceException(e);
        }
        Debug.logInfo("Posted " + counter + " actions.", MODULE);
        return ServiceUtil.returnSuccess();
    }

    /**
     * Placeholder method that is called before <code>callServiceBatch</code> starts iterating.
     * @exception GenericServiceException if an error occurs
     */
    protected void prepareBatch() throws GenericServiceException {
    }

    /**
     * Placeholder method that is called after <code>callServiceBatch</code> finished iterating, but before returning.
     * @exception GenericServiceException if an error occurs
     */
    protected void finalizeBatch() throws GenericServiceException {
    }

    /**
     * Placeholder method that is called when the requested action is <code>Create</code>.
     * @throws GenericServiceException if an error occurs
     * @return the service response <code>Map</code>
     * @see #callService
     */
    protected abstract Map<String, Object> callCreateService() throws GenericServiceException;

    /**
     * Placeholder method that is called when the requested action is <code>Update</code>.
     * @throws GenericServiceException if an error occurs
     * @return the service response <code>Map</code>
     * @see #callService
     */
    protected abstract Map<String, Object> callUpdateService() throws GenericServiceException;

    /**
     * Placeholder method that is called when the requested action is <code>Delete</code>.
     * @throws GenericServiceException if an error occurs
     * @return the service response <code>Map</code>
     * @see #callService
     */
    protected abstract Map<String, Object> callDeleteService() throws GenericServiceException;

}
