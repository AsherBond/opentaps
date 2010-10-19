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
package org.opentaps.tests.service;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class POJOTestServices extends Service {

    private static final String MODULE = POJOTestServices.class.getName();

    private String key1Value = null;
    private List key2Values = null;
    private Boolean failTrigger = false;
    private Boolean errorTrigger = false;
    private Boolean followupTrigger = false;
    private Timestamp testTimestamp = null;
    private static BigDecimal increment = new BigDecimal("1.0");

    public POJOTestServices() { }

    public void setKey1Value(String s) {
        key1Value = s;
    }

    public void setKey2Values(List l) {
        key2Values = l;
    }

    public void setFailTrigger(Boolean b) {
        failTrigger = b;
    }

    public void setErrorTrigger(Boolean b) {
        errorTrigger = b;
    }

    public void setFollowupTrigger(Boolean b) {
        followupTrigger = b;
    }

    public void setTestTimestamp(Timestamp ts) {
        testTimestamp = ts;
    }
    public String getKey1Value() {
        return key1Value;
    }

    public List getKey2Values() {
        return key2Values;
    }

    public String getTriggerEca() {
        if (followupTrigger.booleanValue()) {
            return "Y";
        }
        return null;
    }

    public void pojoTest() throws ServiceException{
        if (failTrigger) {
            ServiceException ex = new ServiceException("Service failure on trigger");
            ex.setRequiresRollback(false);
            throw ex;
        }
        if (errorTrigger) {
            throw new ServiceException("Service error on trigger");
        }
        List<String> key2s = getKey2Values();
        try {
            Delegator delegator = getInfrastructure().getDelegator();
            for (String key2 : key2s) {
                delegator.create("ServiceTestRecord", UtilMisc.toMap("key1", getKey1Value(), "key2", key2, "value1", increment, "testTimestamp", testTimestamp,
                        "createdByUserLogin", getUser().getOfbizUserLogin().getString("userLoginId")));
            }
        } catch (GenericEntityException e) {
            throw new ServiceException(e);
        }
    }

    // get every row of key1  key2 combination
    public static List getAllValues(String key1, List key2s, Delegator delegator) throws GenericEntityException {
        List<GenericValue> values = delegator.findByAnd("ServiceTestRecord", UtilMisc.toList(
                EntityCondition.makeCondition("key1", EntityOperator.EQUALS, key1),
                EntityCondition.makeCondition("key2", EntityOperator.IN, key2s)));
        return values;


    }

    public void pojoTest2() throws ServiceException {
        try {
            List<GenericValue> values = this.getAllValues(getKey1Value(), getKey2Values(), getInfrastructure().getDelegator());
            if (UtilValidate.isNotEmpty(values)) {
                for (GenericValue value : values) {
                    BigDecimal newValue1 = value.getBigDecimal("value1").add(increment).setScale(0, RoundingMode.HALF_UP);
                    value.set("value1", newValue1);
                    value.store();
                }
            }
        } catch (GenericEntityException e) {
            throw new ServiceException(e);
        }
    }

    // old school static Java service
    public static Map pojoTestFollowup(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            List<GenericValue> values = getAllValues((String) context.get("key1Value"), (List) context.get("key2Values"), delegator);
            if (UtilValidate.isNotEmpty(values)) {
                for (GenericValue value : values) {
                    value.set("value2", increment);
                    value.set("modifiedByUserLogin", userLogin.getString("userLoginId"));
                    value.store();
                }
            }
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }

}
