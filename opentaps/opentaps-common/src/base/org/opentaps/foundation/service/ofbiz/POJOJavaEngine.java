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

/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

/* This file is based on StandardJavaEngine.java from Apache OFBiz and has been modified by Open Source Strategies, Inc. */
package org.opentaps.foundation.service.ofbiz;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.engine.GenericAsyncEngine;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.util.FoundationUtils;

/**
 * This service engine executes standard Java object services as specified by calling the set__ methods for each of the parameters,
 * then executing the void method without parameters for the service location, and then uses the get_ methods to retrieve the resulting
 * values before passing them back into the service dispatcher.
 */
public class POJOJavaEngine extends GenericAsyncEngine {

    private static final String module = POJOJavaEngine.class.getName();

    /**
     * A List of standard parameters for all ofbiz services.  We need this because ModelService doesn't return the parameters particular to the service
     * versus those used by the service engine, so we need to remove them from the ModelService parameter list before trying to call methods in the
     * Service object for them.
     */
    public static final List<String> SERVICE_ENGINE_OUT_PARAMS = Arrays.asList("responseMessage", "errorMessage", "errorMessageList", "successMessage", "successMessageList", "userLogin", "locale", "timeZone");

    /**
     * Default constructor.
     * @param dispatcher a <code>ServiceDispatcher</code> value
     */
    public POJOJavaEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * Calls an Ofbiz service and ignore the result.
     * @param localName a <code>String</code> value
     * @param modelService a <code>ModelService</code> value
     * @param context a <code>Map</code> value
     * @exception GenericServiceException if an error occurs
     * @see org.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    /**
     * Calls an Ofbiz service.
     * @param localName a <code>String</code> value
     * @param modelService a <code>ModelService</code> value
     * @param context a <code>Map</code> value
     * @return the result <code>Map</code>
     * @exception GenericServiceException if an error occurs
     * @see org.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        Object result = serviceInvoker(localName, modelService, context);

        if (result == null || !(result instanceof Map)) {
            throw new GenericServiceException("Service did not return expected result");
        }
        return (Map<String, Object>) result;
    }

    // this is so that a List of class parameters as displayed as their class names, rather than as java.lang.Class
    @SuppressWarnings("unchecked")
    private List getParameterClasses(Class[] parameterClasses) {
        List classes = new ArrayList();
        for (Class parameterClass : parameterClasses) {
            classes.add(parameterClass);
        }
        return classes;
    }

    // Invoke the object-oriented (POJO)) java service.
    @SuppressWarnings("unchecked")
    private Object serviceInvoker(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        DispatchContext dctx = dispatcher.getLocalContext(localName);

        if (modelService == null) {
            Debug.logError("ERROR: Null Model Service.", module);
        }
        if (dctx == null) {
            Debug.logError("ERROR: Null DispatchContext.", module);
        }
        if (context == null) {
            Debug.logError("ERROR: Null Service Context.", module);
        }

        // check the package and method names
        if (modelService.location == null || modelService.invoke == null) {
            throw new GenericServiceException("Cannot locate service to invoke (location or invoke name missing)");
        }

        String serviceClassName = modelService.location;
        String invokeMethodName = modelService.invoke;

        // get the classloader to use
        ClassLoader cl = null;

        if (dctx == null) {
            cl = this.getClass().getClassLoader();
        } else {
            cl = dctx.getClassLoader();
        }

        try {
            // instantiate the Java service object and set the Infrastructure and userLogin
            Class serviceClass = cl.loadClass(this.getLocation(modelService));
            Service service = (Service) serviceClass.newInstance();     // force cast so we can use the setInfrastructure and setUser methods, instead of calling them by reflection
            service.setInfrastructure(new Infrastructure(dctx.getDispatcher()));  // the dispatcher is a ServiceDispatcher, but dctx.getDispatcher() gets a LocalDispatcher
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            if (userLogin != null) {
                service.setUser(new User(userLogin));
            }
            // now remove userLogin, so it won't be a parameter which we will try to find a set__ method
            context.remove("userLogin");

            // get the locale as well from the context.
            Locale locale = UtilCommon.getLocale(context);
            service.setLocale(locale);
            context.remove("locale");

            // get the timezone from the context
            TimeZone timeZone = UtilCommon.getTimeZone(context);
            service.setTimeZone(timeZone);
            context.remove("timeZone");

            // now get the set method corresponding to each context parameter and call it to set up the service parameters
            Set<String> contextKeys = context.keySet();
            for (String contextKey : contextKeys) {
                if (UtilValidate.isNotEmpty(contextKey)) {
                    // get method name from parameter name: orderId -> setOrderId
                    String setMethodName = FoundationUtils.setterName(contextKey);
                    // get the value of this context parameter
                    Object contextValue = context.get(contextKey);
                    // empty values should be set to Null instead
                    if (UtilValidate.isEmpty(contextValue)) {
                        contextValue = null;
                    }
                    // now get the method from the method name and a single parameter of the class of the context value
                    // for example, if context has key-value pair "orderId":(String) "WS10000" then we will look for
                    // setOrderId(String s) method.  The type of the set__ method will be that declared in the services XML
                    ModelParam modelParam = modelService.getParam(contextKey);
                    // this will turn String into java.lang.String and Timestamp into java.sql.Timestamp
                    Class[] setMethodParams = {ObjectType.loadInfoClass(modelParam.type, cl)};
                    Method setMethod = null;
                    try {
                         setMethod = serviceClass.getMethod(setMethodName, setMethodParams);
                    } catch (NoSuchMethodException ex) {
                        throw new GenericServiceException("No method [" + setMethodName + "] with parameter [" + getParameterClasses(setMethodParams) + "] found for context key [" + contextKey + "] in service [" + localName + "]", ex);
                    }
                    // now call the set method
                    Object[] setMethodInvokeParams = {contextValue};
                    setMethod.invoke(service, setMethodInvokeParams);
                }
            }

            // now execute the main method, which should be a void method without parameters
            try {
                Method serviceInvokeMethod = serviceClass.getMethod(invokeMethodName);
                serviceInvokeMethod.invoke(service);
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    // handle exceptions only from the service
                	Throwable se =((InvocationTargetException) ex).getTargetException();
                    if (se != null && se instanceof ServiceException) {
                        ((ServiceException) se).setLocale(locale);  // set the locale from the service context, as the service might not set it
                        if (((ServiceException) se).isRequiresRollback()) {
                            return ServiceUtil.returnError(se.getMessage());
                        } else {
                            return ServiceUtil.returnFailure(se.getMessage());
                        }
                    } else {
                        throw ex;
                    }
                } else if (ex instanceof NoSuchMethodException) {
                    return ServiceUtil.returnError("No void method without parameters with name [" + invokeMethodName + "] found in [" + serviceClassName + "]");
                } else {
                    throw ex;
                }
            }

            // without any exceptions, we can assume the service was a success, so now we work on getting the output parameters
            // first we construct a standard service return Map
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("userLogin", userLogin);
            results.put("locale", locale);

            // now we get the out parameters which are not the standard service engine ones
            Set<String> serviceOutParams = modelService.getOutParamNames();
            serviceOutParams.removeAll(SERVICE_ENGINE_OUT_PARAMS);

            // next we go through each out parameter of the service and try to find their get__ method without parameters
            for (String outParam : serviceOutParams) {
                // ie, invoiceId becomes getInvoiceId()
                String getMethodName = FoundationUtils.getterName(outParam);
                try {
                    Method getMethod = serviceClass.getMethod(getMethodName);
                    results.put(outParam, getMethod.invoke(service));
                } catch (NoSuchMethodException ex) {
                    throw new GenericServiceException("No method [" + getMethodName + "] without parameters found for in [" + serviceClassName + "]", ex);
                }

            }

            // set success messages
            results.put(ModelService.SUCCESS_MESSAGE_LIST, service.getSuccessMessages());

            return results;

        } catch (ClassNotFoundException cnfe) {
            throw new GenericServiceException("Cannot find service location", cnfe);
        } catch (NoSuchMethodException nsme) {
            throw new GenericServiceException("Service method does not exist", nsme);
        } catch (SecurityException se) {
            throw new GenericServiceException("Access denied", se);
        } catch (IllegalAccessException iae) {
            throw new GenericServiceException("Method not accessible", iae);
        } catch (IllegalArgumentException iarge) {
            throw new GenericServiceException("Invalid parameter match", iarge);
        } catch (InvocationTargetException ite) {
            throw new GenericServiceException("Service target threw an unexpected exception", ite.getTargetException());
        } catch (NullPointerException npe) {
            throw new GenericServiceException("Specified object is null", npe);
        } catch (ExceptionInInitializerError eie) {
            throw new GenericServiceException("Initialization failed", eie);
        } catch (Throwable th) {
            throw new GenericServiceException("Error or unknown exception", th);
        }

    }
}
