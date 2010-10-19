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
package org.opentaps.aspect.secas;

import java.util.Map;
import java.util.Properties;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.eca.EntityEcaHandler;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

public class IndexForDelegatorAspects {

    private static final String MODULE = IndexForDelegatorAspects.class.getName();
    // hibernate search configuration file
    private static Properties entitySearchProperties;
    // the user to run opentaps.createIndexForGenericEntity service
    private static String runAsUser = "system";

    /**
     * @Expression execution(* org.ofbiz.entity.eca.EntityEcaHandler+.evalRules(..)) && args(currentOperation, eventMap, event, value, isError)
     */
    void pointcut(String currentOperation, Map eventMap, String event, GenericEntity value, boolean isError) { }

    /**
     * @After pointcut(currentOperation, eventMap, event, value, isError)
     */
    public void createIndexForEca(String currentOperation, Map eventMap, String event, GenericEntity value, boolean isError) {
        if (entitySearchProperties == null) {
            // if entitySearchProperties not initial, then initial.
            entitySearchProperties = UtilProperties.getProperties("entitysearch.properties");
        }
        // if event = return and operation = create|store|remove
        if (entitySearchProperties != null && EntityEcaHandler.EV_RETURN.equals(event) && (EntityEcaHandler.OP_CREATE.equals(currentOperation) || EntityEcaHandler.OP_STORE.equals(currentOperation) || EntityEcaHandler.OP_REMOVE.equals(currentOperation))) {
            // if entitySearchProperties contain the entityName then create index for it
            if (entitySearchProperties.containsKey(value.getEntityName())) {
                if (entitySearchProperties.getProperty(value.getEntityName()).equals("index")) {
                    // call service to create index
                    Map<String, Object> inputParams = UtilMisc.<String, Object>toMap("value", value);
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
