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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.tests.OpentapsTestCase;

public class POJOServiceTests extends OpentapsTestCase {

    private GenericValue user = null;
    private String key1Value = "TEST";
    private List key2Values = null;
    private Timestamp testTimestamp = null;

    public void setUp() throws Exception {
        super.setUp();
        user = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
        //because different database timestamp not use same precision, so we just compare "yyyy-MM-dd HH:mm:ss" of timestamp.
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date timeDate = sdf.parse(sdf.format(UtilDateTime.nowTimestamp()));
        testTimestamp = new java.sql.Timestamp(timeDate.getTime());

        key2Values = UtilMisc.toList("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot");
        // just to be sure -- if test crashed somehow last time, these might still be hanging around
        removeTestingRecords(delegator);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        // delegator is reset to null by super.tearDown() so we have to get it again
        removeTestingRecords(DelegatorFactory.getDelegator(OpentapsTestCase.DELEGATOR_NAME));
    }

    private void removeTestingRecords(Delegator delegator) throws GenericEntityException {
        delegator.removeByCondition("ServiceTestRecord", EntityCondition.makeCondition("key1", EntityOperator.EQUALS, "TEST"));
    }

    public void testErrorDueToMissingUserLogin() {
        Map params = UtilMisc.toMap("key1Value", key1Value);
        runAndAssertServiceError("pojoTest", params);
    }


    public void testErrorDueToMissingRequiredValues() {
        Map params = UtilMisc.toMap("key1Value", key1Value, "userLogin", user);
        runAndAssertServiceError("pojoTest", params);
    }

    public void testErrorOnTrigger() {
        Map params = UtilMisc.toMap("key1Value", key1Value, "key2Values", key2Values, "errorTrigger", new Boolean(true), "userLogin", user);
        runAndAssertServiceError("pojoTest", params);
    }

    public void testFailureOnTrigger() {
        Map params = UtilMisc.toMap("key1Value", key1Value, "key2Values", key2Values, "failureTrigger", new Boolean(true), "userLogin", user);
        runAndAssertServiceFailure("pojoTest", params);
    }

    public void testBasicSuccessfulRun() throws GenericEntityException {
        Map params = UtilMisc.toMap("key1Value", key1Value, "key2Values", key2Values, "testTimestamp", testTimestamp, "userLogin", user);
        runAndAssertServiceSuccess("pojoTest", params);
        List<GenericValue> testValues = POJOTestServices.getAllValues(key1Value, key2Values, delegator);
        assertEachTestValueIsCorrect(testValues, new BigDecimal(1.0), null, testTimestamp, "admin", null);
    }


    public void testSuccessfulRunWithSECA() throws GenericEntityException {
        Map params = UtilMisc.toMap("key1Value", key1Value, "key2Values", key2Values, "testTimestamp", testTimestamp, "followupTrigger", new Boolean(true), "userLogin", user);
        runAndAssertServiceSuccess("pojoTest", params);
        List<GenericValue> testValues = POJOTestServices.getAllValues(key1Value, key2Values, delegator);
        assertEachTestValueIsCorrect(testValues, new BigDecimal(2.0), new BigDecimal(1.0), testTimestamp, "admin", "admin");
    }

    private void assertEachTestValueIsCorrect(List<GenericValue> testValues, BigDecimal expectedValue1, BigDecimal expectedValue2, Timestamp expectedTimestamp, String expectedCreateUserLogin, String expectedModifiedUserLogin) {
        for (GenericValue testValue: testValues) {
            assertEquals(testValue + " value1 correct ", testValue.getBigDecimal("value1"), expectedValue1);
            assertEquals(testValue + " value2 correct ", testValue.getBigDecimal("value2"), expectedValue2);
            assertEquals(testValue + " testTimestamp correct ", testValue.getTimestamp("testTimestamp"), expectedTimestamp);
            assertEquals(testValue + " created user login correct ", testValue.getString("createdByUserLogin"), expectedCreateUserLogin);
            assertEquals(testValue + " modified user login correct ", testValue.getString("modifiedByUserLogin"), expectedModifiedUserLogin);
        }
    }
}
