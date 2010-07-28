/*
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
 */
/* This file has been modified by Open Source Strategies, Inc. */

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.RunningService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.engine.GenericEngine;
import org.opentaps.foundation.infrastructure.Infrastructure;

savedSyncResult = null;
if (session.getAttribute("_SAVED_SYNC_RESULT_") != null) {
    savedSyncResult = session.getAttribute("_SAVED_SYNC_RESULT_");
}

serviceName = parameters.SERVICE_NAME;
context.POOL_NAME = ServiceConfigUtil.getSendPool();

scheduleOptions = [];
serviceParameters = [];
e = request.getParameterNames();
while (e.hasMoreElements()) {
    paramName = e.nextElement();
    paramValue = parameters[paramName];
    scheduleOptions.add([name : paramName, value : paramValue]);
}

context.scheduleOptions = scheduleOptions;

if (serviceName) {
    dctx = dispatcher.getDispatchContext();
    infrastructure = new Infrastructure(dispatcher);
    model = null;
    try {
        model = dctx.getModelService(serviceName);
    } catch (Exception exc) {
        context.errorMessageList = [exc.getMessage()];
    }
    if (model != null) {
        model.getInParamNames().each { paramName ->
            par = model.getParam(paramName);
            if (par.internal) {
                return;
            }
            serviceParam = null;

            // for each parameter try to get the default value from OpentapsConfiguration value for "service_name::parameter_name"
            confType = serviceName + "::" + par.name;
            defaultValue = infrastructure.getConfigurationValue(confType);
            description = infrastructure.getConfigurationDescription(confType);
            if (defaultValue == null) {
               defaultValue = par.defaultValue;
            }

            if (savedSyncResult?.get(par.name)) {
                serviceParam = [description : description, name : par.name, type : par.type, optional : par.optional ? "Y" : "N", defaultValue : defaultValue, value : savedSyncResult.get(par.name)];
            } else {
                serviceParam = [description : description, name : par.name, type : par.type, optional : par.optional ? "Y" : "N", defaultValue : defaultValue];
            }
            serviceParameters.add(serviceParam);
        }
    }
}
context.serviceParameters = serviceParameters;
