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
package org.opentaps.tests.crmsfa.activities;

import java.util.List;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.security.SecurityFactory;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Test case superclass for all activities security tests.  This defines asserts which are useful for testing
 * activity security scenarios.
 */
public class ActivitiesSecurityTestCase extends OpentapsTestCase {

    public static final String module = ActivitiesSecurityTestCase.class.getName();
    protected Security security = null;
    
    protected GenericValue demoSalesManager = null;    
    protected GenericValue demoSalesRep1 = null;
    protected GenericValue demoSalesRep2 = null;
    protected GenericValue demoSalesRep3 = null;
    protected GenericValue demoSalesRep4 = null;

    // DemoSalesRep1 private task
    protected String testPrivateTask1 = null;
    // DemoSalesRep1 public task
    protected String testPublicTask1 = null;    
    // DemoSalesRep2 private task
    protected String testPrivateTask2 = null;
    // DemoSalesRep2 public task
    protected String testPublicTask2 = null;
    // DemoSalesRep1 private task with DemoSalesRep2 as an assignee
    protected String testPrivateTask3 = null;
    // DemoSalesRep1 public task with DemoSalesRep2 as an assignee
    protected String testPublicTask3 = null;    
    // DemoSalesManager private task
    protected String testPrivateTask4 = null;
    // DemoSalesManager public task
    protected String testPublicTask4 = null;    
    
    public void setUp() throws Exception {
        super.setUp();
        security = SecurityFactory.getInstance(delegator);
        demoSalesManager = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));        
        demoSalesRep1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
        demoSalesRep2 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
        demoSalesRep3 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
        demoSalesRep4 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep4"));
        createTestData();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        security = null;
        demoSalesManager = null;
        demoSalesRep1 = null;
        demoSalesRep2 = null;
        demoSalesRep3 = null;        
        demoSalesRep4 = null;
        testPrivateTask1 = null;
        testPublicTask1 = null;        
        testPrivateTask2 = null;
        testPublicTask2 = null;        
        testPrivateTask3 = null;
        testPublicTask3 = null;
        testPrivateTask4 = null;
        testPublicTask4 = null;         
    }
    
    private void createTestData() throws Exception {
        testPrivateTask1 = createTask("DemoSalesRep1 private task", demoSalesRep1, "WES_CONFIDENTIAL");
        testPublicTask1 = createTask("DemoSalesRep1 public task", demoSalesRep1, "WES_PUBLIC");        
        testPrivateTask2 = createTask("DemoSalesRep2 private task", demoSalesRep2, "WES_CONFIDENTIAL");
        testPublicTask2 = createTask("DemoSalesRep2 public task", demoSalesRep2, "WES_PUBLIC");        
        testPrivateTask3 = createTask("DemoSalesRep1 private task with DemoSalesRep2 and DemoSalesManager as assignees", demoSalesRep1, "WES_CONFIDENTIAL");
        assignPartyToTask(testPrivateTask3, demoSalesRep2, demoSalesRep1);
        assignPartyToTask(testPrivateTask3, demoSalesManager, demoSalesRep1);        
        testPublicTask3 = createTask("DemoSalesRep1 public task with DemoSalesRep2 and DemoSalesManager as assignees", demoSalesRep1, "WES_PUBLIC");
        assignPartyToTask(testPublicTask3, demoSalesRep2, demoSalesRep1);        
        assignPartyToTask(testPrivateTask3, demoSalesManager, demoSalesRep1);        
        testPrivateTask4 = createTask("DemoSalesManager private task", demoSalesManager, "WES_CONFIDENTIAL");
        testPublicTask4 = createTask("DemoSalesManager public task", demoSalesManager, "WES_PUBLIC");                
    }
    
    private String createTask(String workEffortName, GenericValue userLogin, String scopeEnumId) throws Exception {
        Map createTaskContext = UtilMisc.toMap("workEffortName", workEffortName);
        createTaskContext.put("scopeEnumId", scopeEnumId);        
        createTaskContext.put("workEffortTypeId", "TASK");
        createTaskContext.put("workEffortPurposeTypeId", "WEPT_SUPPORT");
        createTaskContext.put("currentStatusId", "TASK_SCHEDULED");
        createTaskContext.put("estimatedStartDate",UtilDateTime.nowTimestamp());
        createTaskContext.put("estimatedCompletionDate", UtilDateTime.nowTimestamp());
        createTaskContext.put("workEffortName", workEffortName); 
        createTaskContext.put("forceIfConflicts", "Y");    
        createTaskContext.put("availabilityStatusId", "WEPA_AV_AVAILABLE");            
        createTaskContext.put("userLogin", userLogin);        
        Map serviceResults = runAndAssertServiceSuccess("crmsfa.createActivity", createTaskContext);
        String workEffortId = (String) serviceResults.get("workEffortId");
        
        return workEffortId;
    }
    
    private void assignPartyToTask(String workEffortId, GenericValue partyToAssignLogin, GenericValue userLogin) throws Exception {
        Map createWorkEffortPartyAssignmentContext = UtilMisc.toMap("partyId", (String)partyToAssignLogin.get("partyId"));
        createWorkEffortPartyAssignmentContext.put("workEffortId", workEffortId);
        createWorkEffortPartyAssignmentContext.put("statusId", "PRTYASGN_ASSIGNED");
        createWorkEffortPartyAssignmentContext.put("userLogin", userLogin);            
        runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", createWorkEffortPartyAssignmentContext);    	
    	
    }
}
