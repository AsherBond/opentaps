/*
 * Copyright (c) 2006 - 2008 Open Source Strategies, Inc.
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
package org.opentaps.aspect.secas;

import java.util.Map;
import java.util.Properties;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

public class IndexForDelegatorAspects {

    public static final String MODULE = IndexForDelegatorAspects.class.getName();
    // hibernate search configuration file
    private static Properties entitySearchProperties;
    // the user to run opentaps.createIndexForGenericEntity service
    private static String runAsUser = "system";

    /**
     * @Expression execution(* org.ofbiz.entity.GenericDelegator.evalEcaRules(..)) && args(event, currentOperation, value, eventMap, noEventMapFound, isError)
     */
    void pointcut(String event, String currentOperation, GenericEntity value, Map eventMap, boolean noEventMapFound, boolean isError) {}

    /**
     * @After pointcut(event, currentOperation, value, eventMap, noEventMapFound, isError)
    */
    public void createIndexForEca(String event, String currentOperation, GenericEntity value, Map eventMap, boolean noEventMapFound, boolean isError) {
        if (entitySearchProperties == null) {
            // if entitySearchProperties not initial, then initial.
            entitySearchProperties = UtilProperties.getProperties("entitysearch.properties");
        }
        // if event = return and operation = create|store|remove
        if ("return".equals(event) && ("create".equals(currentOperation) || "store".equals(currentOperation) || "remove".equals(currentOperation))) {
            // if entitySearchProperties contain the entityName then create index for it
            if (entitySearchProperties.containsKey(value.getEntityName())) {
                if (entitySearchProperties.getProperty(value.getEntityName()).equals("index")) {
                    // call service to create index
                    Map inputParams = UtilMisc.toMap("value", value);
                    try {
                        GenericValue userLoginToRunAs = value.getDelegator().findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", runAsUser));
                        if (userLoginToRunAs != null) {
                            inputParams.put("userLogin", userLoginToRunAs);
                        }
                        LocalDispatcher dispatcher = GenericDispatcher.getLocalDispatcher("entity-" + value.getDelegator().getDelegatorName(), value.getDelegator());
                        // call opentaps.createIndexForGenericEntity in sync mode (don't open new new transaction)
                        // we can't call domain code here because this compiles with the delegator, and the domain code is not available here
                        dispatcher.runSync("opentaps.createIndexForGenericEntity", inputParams, -1, false);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, MODULE);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }
                }
            }
        }
    }
}
