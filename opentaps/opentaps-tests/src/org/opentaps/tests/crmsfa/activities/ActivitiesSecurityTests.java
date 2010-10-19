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
/* Copyright (c) Open Source Strategies, Inc. */

package org.opentaps.tests.crmsfa.activities;

import junit.framework.TestCase;
import org.opentaps.tests.crmsfa.activities.ActivitiesSecurityTestCase;
import org.ofbiz.base.util.GeneralException;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * This is a class of unit tests for CRM/SFA application's security permissions.
 * To run these, from the command line do 
 * $ java -jar ofbiz.jar -test
 * 
 * or
 * $ ant run-tests
 * 
 * This will be run along with other test suites, such as those for the entity engine or the service engine
 *   
 * This test suite requires crmsfa/data/CRMSFADemoData.xml and CRMSFASecurityData.xml to be installed  
 *
 * Also, make sure that your "test" delegator is set to the correct datasource in framework/entity/config/entityengine.xml
 *  
 * @author Alexandre Gomes
 *
 */
public class ActivitiesSecurityTests extends ActivitiesSecurityTestCase {
    
    
    // the view permission for public activities is set by default. The hasSecurityScopePermission() method is used
    // to check permission if the task or event has private security scope. If it has public security scope no tests are made.
    // these commented tests should be considered in a future activity security refactoring
    // It will be important, in the future refactoring, to distinguish the security scope (or activity visibility) permission
    // from the permission to set the security scope of an activity. At this moment, hasSecurityScopePermission() does not 
    // clearly separate those two distinct security features
    /**
     * DemoSalesRep1 tries to view his own public task, TestPublicTask1.
     * The task is public so he should be allowed to view the task
     */
    /*public void testDemoSalesRep1TestPublicTask1View() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesRep1, testPublicTask1, false);
        assertTrue("DemoSalesRep1 has permission to view his public task, TestPublicTask1.", hasPermission);
    }*/
    
    /**
     * DemoSalesRep1 tries to view DemoSalesRep2 public task, TestPublicTask2.
     * The task is public so he should be allowed to view the task
     */
    /*public void testDemoSalesRep1TestPublicTask2View() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesRep1, testPublicTask2, false);
        assertTrue("DemoSalesRep1 has permission to view DemoSalesRep2 public task, TestPublicTask2.", hasPermission);        
    }*/      
    
    /**
     * DemoSalesManager tries to view DemoSalesRep1 public task, TestPublicTask1.
     * DemoSalesManager is super user so he should be allowed to see any task, including this one
     */
    /*public void testDemoSalesManagerTestPublicTask1View() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesManager, testPublicTask1, false);
        assertTrue("DemoSalesManager has permission to view DemoSalesRep1 public task, TestPublicTask1.", hasPermission);        
    }*/
    
    /**
     * DemoSalesRep1 tries to update the security of his own public task, TestPublicTask1.
     * He is the owner of the task so he should be allowed to update the security of the task
     */
    public void testDemoSalesRep1TestPublicTask1Update() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesRep1, testPublicTask1, true);
        assertTrue("DemoSalesRep1 has permission to update the security of his public task, TestPublicTask1.", hasPermission);
    }
    
    /**
     * DemoSalesRep1 tries to update the security of DemoSalesRep2 public task, TestPublicTask2.
     * He is neither super user nor the owner of the task so he should no have permission to update the security of the task
     */
    public void testDemoSalesRep1TestPublicTask2Update() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesRep1, testPublicTask2, true);
        assertFalse("DemoSalesRep1 does NOT have permission to update the security of DemoSalesRep2 public task, TestPublicTask2.", hasPermission);
    }      
    
    /**
     * DemoSalesManager tries to update the security of DemoSalesRep1 public task, TestPublicTask1.
     * DemoSalesManager is super user so he should be allowed to set the security of any task, including this one
     */
    public void testDemoSalesManagerTestPublicTask1Update() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesManager, testPublicTask1, true);
        assertTrue("DemoSalesManager has permission to set the security of DemoSalesRep1 public task, TestPublicTask1.", hasPermission);
    }    
    
    /**
     * DemoSalesRep1 tries to view his own private task, TestPrivateTask1.
     * DemoSalesRep1 is the task's owner so he should be allowed to view the task
     */
    public void testDemoSalesRep1TestPrivateTask1View() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesRep1, testPrivateTask1, false);
        assertTrue("DemoSalesRep1 has permission to view his private task, TestPrivateTask1.", hasPermission);
    }  
    
    /**
     * DemoSalesRep1, tries to update his private task, TestPrivateTask1.
     * DemoSalesRep1 is the tasks's owner so he should be allowed to update the task
     */
    public void testDemoSalesRep1TestPrivateTask1Update() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesRep1, testPrivateTask1, true);
        assertTrue("DemoSalesRep1 has permission to update DemoSaleRep1 private task.", hasPermission);
    }      
    
    /**
     * DemoSalesRep2 tries to view DemoSalesRep1 private task, TestPrivateTask1. DemoSalesRep2 is not an assignee
     * for the task so he should no be allowed to view the task
     */
    public void testDemoSalesRep2TestPrivateTask1View() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesRep2, testPrivateTask1, false);
        assertFalse("DemoSalesRep2 does not have permission to view DemoSaleRep1 private task.", hasPermission);
    }
    
    /**
     * DemoSalesRep2, tries to update DemoSalesRep1 private task, TestPrivateTask1.
     * DemoSalesRep2 is not the tasks's owner so he should NOT be allowed to update the task
     */
    public void testDemoSalesRep2TestPrivateTask1Update() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesRep2, testPrivateTask1, true);
        assertFalse("DemoSalesRep2 does not have permission to update DemoSaleRep1 private task.", hasPermission);
    }      
    
    /**
     * DemoSalesManager, who has CRMSFA_ACT_ADMIN permission tries to view DemoSalesRep1 private task, TestPrivateTask1.
     * DemoSalesManager is super user so he should be allowed to view the task
     */
    public void testDemoSalesManagerTestPrivateTask1View() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesManager, testPrivateTask1, false);
        assertTrue("DemoSalesManager has permission to view DemoSaleRep1 private task.", hasPermission);
    }  
    
    /**
     * DemoSalesManager, who has CRMSFA_ACT_ADMIN permission tries to update DemoSalesRep1 private task, TestPrivateTask1.
     * DemoSalesManager is super user so he should be allowed to update the task
     */
    public void testDemoSalesManagerTestPrivateTask1Update() {
        boolean hasPermission = CrmsfaSecurity.hasSecurityScopePermission(security, demoSalesManager, testPrivateTask1, true);
        assertTrue("DemoSalesManager has permission to update DemoSaleRep1 private task.", hasPermission);
    }
    
    /**
     * Activities Assignment Restriction Tests
     * The tests for assigning parties to activities also test the permission to remove a party from an activity
     */

    /**
     * DemoSalesRep1 tries to assign someone a task he owns (testPublicTask)
     * DemoSalesRep1 is owner to the task so he should be granted permission to assign someone the task
     */
    public void testDemoSalesRep1TestPublicTask1AssignParty() throws GeneralException {
        boolean hasPermission = CrmsfaSecurity.hasActivityUpdatePartiesPermission(security, demoSalesRep1, testPublicTask1, false);
        assertTrue("DemoSalesRep1 has permission to assign someone to a public task owned by him (testPublicTask1).", hasPermission);
    }    
    
    /**
     * DemoSalesRep2 tries to assign someone a task to which he is assigned (testPublicTask3)
     * DemoSalesRep1 is an assignee to the task so he should be granted permission to assign someone the task
     */
    public void testDemoSalesRep2TestPublicTask3AssignParty() throws GeneralException {
        boolean hasPermission = CrmsfaSecurity.hasActivityUpdatePartiesPermission(security, demoSalesRep2, testPublicTask3, false);
        assertTrue("DemoSalesRep2 has permission to assign someone to a public task to which he is an assignee (testPublicTask3).", hasPermission);
    }        
    
    /**
     * DemoSalesRep1 tries to assign someone a task to which he is NOT assigned (testPublicTask2)
     * DemoSalesRep1 is NOT an assignee to the task so he should NOT be granted permission to assign someone the task
     */
    public void testDemoSalesRep1TestPublicTask2AssignParty() throws GeneralException {
        boolean hasPermission = CrmsfaSecurity.hasActivityUpdatePartiesPermission(security, demoSalesRep1, testPublicTask2, false);
        assertFalse("DemoSalesRep1 does not have permission to assign someone to a public task to which he is NOT an assignee (testPublicTask2).", hasPermission);
    }
    
    /**
     * DemoSalesManager tries to assign someone a task he owns (testPublicTask4)
     * DemoSalesManager is super-user so he should be granted permission to assign someone the task
     */
    public void testDemoSalesManagerTestPublicTask4AssignParty() throws GeneralException {
        boolean hasPermission = CrmsfaSecurity.hasActivityUpdatePartiesPermission(security, demoSalesManager, testPublicTask4, false);
        assertTrue("DemoSalesManager has permission to assign someone to his own public task (testPublicTask4).", hasPermission);
    }
    
    /**
     * DemoSalesManager tries to assign someone a task to which he is assigned (testPublicTask3)
     * DemoSalesManager is super-user so he should be granted permission to assign someone the task
     */
    public void testDemoSalesManagerTestPublicTask3AssignParty() throws GeneralException {
        boolean hasPermission = CrmsfaSecurity.hasActivityUpdatePartiesPermission(security, demoSalesManager, testPublicTask3, false);
        assertTrue("DemoSalesManager has permission to assign someone to a task to which he is assigned (testPublicTask3).", hasPermission);
    }
    
    /**
     * DemoSalesManager tries to assign someone a task to which he is NOT assigned (testPublicTask1)
     * DemoSalesManager is super-user so he should be granted permission to assign someone the task
     */
    public void testDemoSalesManagerTestPublicTask1AssignParty() throws GeneralException {
        boolean hasPermission = CrmsfaSecurity.hasActivityUpdatePartiesPermission(security, demoSalesManager, testPublicTask1, false);
        assertTrue("DemoSalesManager has permission to assign someone to a task to which he is NOT assigned (testPublicTask1).", hasPermission);
    }      
    
}
