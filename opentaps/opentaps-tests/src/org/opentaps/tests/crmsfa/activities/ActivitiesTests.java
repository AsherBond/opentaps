/*
 * Copyright (c) 2006 - 2010 Open Source Strategies, Inc.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opensourcestrategies.crmsfa.activities.UtilActivity;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.PartyContactMech;
import org.opentaps.base.services.CrmsfaCreateActivityService;
import org.opentaps.base.services.CrmsfaCreateLeadService;
import org.opentaps.base.services.CrmsfaFindActivitiesService;
import org.opentaps.base.services.CrmsfaDeleteActivityEmailService;
import org.opentaps.base.services.CrmsfaSaveActivityEmailService;
import org.opentaps.base.services.CrmsfaLogTaskService;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.tests.OpentapsTestCase;
import org.opentaps.tests.crmsfa.crm.CrmTests;

public class ActivitiesTests extends OpentapsTestCase {
	private static final String MODULE = CrmTests.class.getName();
	
	private static final String TEST_LEAD_COMPANY_NAME = "TEST_Company ";	
    private static final String TEST_LEAD_01_FIRST_NAME = "Mark";
    private static final String TEST_LEAD_01_LAST_NAME = "Twain";
    private static final String TEST_EVENT_NAME = "Test Event";
    private static final String TEST_TASK_NAME = "Test Task";
    
    private static final String TEST_EMAIL = "test@ua.fm";
    private static final String TEST_EMAIL_SUBJECT = "test_subject";
    private static final String TEST_EMAIL_BODY = "test_body";
    private static final String TEST_EMAIL_MIME_TYPE = "text/plain";
    
    private PartyDomainInterface partyDomain = null;	
	private PartyRepositoryInterface partyRep = null;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
   
        partyDomain = domainsLoader.getDomainsDirectory().getPartyDomain();
        partyRep = partyDomain.getPartyRepository();       
    }
    
    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        partyDomain = null;
        partyRep = null;
    }  
    
    public void testOwnerPartyId() throws GeneralException {
        GenericValue activityOwner = UtilActivity.getActivityOwner("DemoMeeting1", delegator);
        assertEquals("DemoSalesRep1 is the owner of Demo Meeting 1", activityOwner.getString("partyId"), "DemoSalesRep1");
    }
    
    public void testChangeOwnerPartyId() throws GeneralException {
        
        GenericValue DemoSalesManager = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        GenericValue DemoSalesRep1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
        GenericValue DemoSalesRep2 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
        
        /*
         * DemoSalesRep1 changes owner of activity DemoMeeting1 to DemoSalesManager.
         * Should be success. Calendar owner about to change owner to superuser. 
         */
        Map<String, Object> callContext = new HashMap<String, Object>();
        callContext.put("userLogin", DemoSalesRep1);
        callContext.put("workEffortId", "DemoMeeting1");
        callContext.put("newOwnerPartyId", DemoSalesManager.getString("partyId"));
        
        runAndAssertServiceSuccess("crmsfa.changeActivityOwner", callContext);
        
        /*
         * DemoSalesRep1 changes owner of activity DemoMeeting to DemoSalesRep2.
         * Should be fail. DemoSalesRep1 isn't owner any more and isn't superuser. 
         */
        callContext = new HashMap<String, Object>();
        callContext.put("userLogin", DemoSalesRep1);
        callContext.put("workEffortId", "DemoMeeting1");
        callContext.put("newOwnerPartyId", DemoSalesRep2.getString("partyId"));
        
        runAndAssertServiceError("crmsfa.changeActivityOwner", callContext);
        
        /*
         * DemoSalesManager changes owner of activity DemoMeeting1 to DemoSalesRep2.
         * Should be success. DemoSalesManager is owner and superuser. 
         */
        callContext = new HashMap<String, Object>();
        callContext.put("userLogin", DemoSalesManager);
        callContext.put("workEffortId", "DemoMeeting1");
        callContext.put("newOwnerPartyId", DemoSalesRep2.getString("partyId"));
        
        runAndAssertServiceSuccess("crmsfa.changeActivityOwner", callContext);

        /*
         * DemoSalesManager changes owner of activity DemoMeeting1 to DemoSalesRep1.
         * Should be success. DemoSalesManager isn't owner of the activity but superuser.
         */
        callContext = new HashMap<String, Object>();
        callContext.put("userLogin", DemoSalesManager);
        callContext.put("workEffortId", "DemoMeeting1");
        callContext.put("newOwnerPartyId", DemoSalesRep1.getString("partyId"));
        
        runAndAssertServiceSuccess("crmsfa.changeActivityOwner", callContext);
        
    }
    
    /**
     * Test deleteActivityEmail service
     * Create Activity event = Save Activity Email 
     * 
     * @throws Exception
     */
    public void testDeleteActivityAfterSaveEmail() throws Exception {
    	Debug.logInfo("START --- testDeleteActivityAfterSaveEmail --- ", MODULE);
    	
    	// Create lead 
    	CrmsfaCreateLeadService createLeadService = new CrmsfaCreateLeadService();
    	createLeadService.setInUserLogin(this.admin);    	    	    	
    	createLeadService.setInCompanyName(TEST_LEAD_COMPANY_NAME);
    	createLeadService.setInFirstName(TEST_LEAD_01_FIRST_NAME);
    	createLeadService.setInLastName(TEST_LEAD_01_LAST_NAME);
    	createLeadService.setInPrimaryEmail(TEST_EMAIL);
    	
    	runAndAssertServiceSuccess(createLeadService);
    	
    	String partyIdLead = createLeadService.getOutPartyId();
    	    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumber = countPendingActivities(partyIdLead);
    	
    	Party party = partyRep.getPartyById(partyIdLead);
    	List<? extends PartyContactMech> contactMeches = party.getPartyContactMeches();
    	String contactMechId = contactMeches.iterator().next().getContactMechId();
    	
    	// Save Activity Email
    	CrmsfaSaveActivityEmailService saveActivityEmailService= new CrmsfaSaveActivityEmailService();
    	saveActivityEmailService.setInUserLogin(this.admin);   
    	saveActivityEmailService.setInContactMechIdFrom(contactMechId);
    	saveActivityEmailService.setInToEmail(TEST_EMAIL);
    	saveActivityEmailService.setInSubject(TEST_EMAIL_SUBJECT);
    	saveActivityEmailService.setInContent(TEST_EMAIL_BODY);
    	saveActivityEmailService.setInContentMimeTypeId(TEST_EMAIL_MIME_TYPE);
    	saveActivityEmailService.setInInternalPartyId(partyIdLead);
    	
    	runAndAssertServiceSuccess(saveActivityEmailService);
    	
    	String workEffortId = saveActivityEmailService.getOutWorkEffortId();
    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumberAfterAdd = countPendingActivities(partyIdLead);
    	assertEquals("Number activities must be higher on 1 then before adding", activityNumberAfterAdd, activityNumber + 1);    	    	    	
    	
    	// Delete Activity
    	CrmsfaDeleteActivityEmailService  deleteActivityEmailService = new CrmsfaDeleteActivityEmailService();
    	deleteActivityEmailService.setInUserLogin(this.admin);
    	deleteActivityEmailService.setInWorkEffortId(workEffortId);
    	
    	runAndAssertServiceSuccess(deleteActivityEmailService);
    	
    	// Verify that the number of activities is the same as before adding 
    	int activityNumberAfterDelete = countPendingActivities(partyIdLead);
    	assertEquals("Number activities must be same as before adding", activityNumberAfterDelete, activityNumber);
    	    	
    	Debug.logInfo("DOWN --- testDeleteActivityAfterSaveEmail --- ", MODULE);
    }
    
    /**
     * Test deleteActivityEmail service with CommunicationEventId
     * Create Activity event = Save Activity Email
     * 
     * @throws Exception
     */
    public void testDeleteActivityAfterSaveEmailWithCommunicationEventId() throws Exception {
    	Debug.logInfo("START --- testDeleteActivityAfterSaveEmailWithCommunicationEventId --- ", MODULE);
    	
    	// Create lead 
    	CrmsfaCreateLeadService createLeadService = new CrmsfaCreateLeadService();
    	createLeadService.setInUserLogin(this.admin);    	    	    	
    	createLeadService.setInCompanyName(TEST_LEAD_COMPANY_NAME);
    	createLeadService.setInFirstName(TEST_LEAD_01_FIRST_NAME);
    	createLeadService.setInLastName(TEST_LEAD_01_LAST_NAME);
    	createLeadService.setInPrimaryEmail(TEST_EMAIL);
    	
    	runAndAssertServiceSuccess(createLeadService);
    	
    	String partyIdLead = createLeadService.getOutPartyId();
    	    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumber = countPendingActivities(partyIdLead);
    	
    	Party party = partyRep.getPartyById(partyIdLead);
    	List<? extends PartyContactMech> contactMeches = party.getPartyContactMeches();
    	String contactMechId = contactMeches.iterator().next().getContactMechId();
    	
    	// Save Activity Email
    	CrmsfaSaveActivityEmailService  saveActivityEmailService= new CrmsfaSaveActivityEmailService();
    	saveActivityEmailService.setInUserLogin(this.admin);
    	saveActivityEmailService.setInContactMechIdFrom(contactMechId);    	
    	saveActivityEmailService.setInToEmail(TEST_EMAIL);
    	saveActivityEmailService.setInSubject(TEST_EMAIL_SUBJECT);
    	saveActivityEmailService.setInContent(TEST_EMAIL_BODY);
    	saveActivityEmailService.setInContentMimeTypeId(TEST_EMAIL_MIME_TYPE);
    	saveActivityEmailService.setInInternalPartyId(partyIdLead);
    		
    	runAndAssertServiceSuccess(saveActivityEmailService);
    	
    	String workEffortId = saveActivityEmailService.getOutWorkEffortId();
    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumberAfterAdd = countPendingActivities(partyIdLead);
    	assertEquals("Number activities must be higher on 1 then before adding", activityNumberAfterAdd, activityNumber + 1);    	    	
    	
    	// Delete Activity
    	CrmsfaDeleteActivityEmailService  deleteActivityEmailService = new CrmsfaDeleteActivityEmailService();
    	deleteActivityEmailService.setInUserLogin(this.admin);
    	deleteActivityEmailService.setInWorkEffortId(workEffortId);
    	    	    	
    	GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
    	List<GenericValue> communicationEvent = workEffort.getRelated("CommunicationEventWorkEff");    	
    	String communicationEventId = communicationEvent.get(0).getString("communicationEventId");
    	
    	deleteActivityEmailService.setInCommunicationEventId(communicationEventId);
    	    	    	
    	runAndAssertServiceSuccess(deleteActivityEmailService);
    	
    	// Verify that the number of activities is the same as before adding 
    	int activityNumberAfterDelete = countPendingActivities(partyIdLead);
    	assertEquals("Number activities must be same as before adding", activityNumberAfterDelete, activityNumber);
    	
    	Debug.logInfo("DOWN --- testDeleteActivityAfterSaveEmailWithCommunicationEventId --- ", MODULE);    	    	  
    }
    
    /**
     * Test deleteActivityEmail service
     * Create Activity event = Create Log Call
     * 
     * @throws Exception
     */    
	public void testDeleteActivityAfterLogCall() throws Exception {
    	Debug.logInfo("START --- testDeleteActivityAfterLogCall --- ", MODULE);
    	
    	// Create lead 
    	CrmsfaCreateLeadService createLeadService = new CrmsfaCreateLeadService();
    	createLeadService.setInUserLogin(this.admin);    	    	    	
    	createLeadService.setInCompanyName(TEST_LEAD_COMPANY_NAME);
    	createLeadService.setInFirstName(TEST_LEAD_01_FIRST_NAME);
    	createLeadService.setInLastName(TEST_LEAD_01_LAST_NAME);
    	
    	runAndAssertServiceSuccess(createLeadService);
    	
    	String partyIdLead = createLeadService.getOutPartyId();
    	    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumber = countCompletedActivities(partyIdLead);    	
    	
    	// Create LogCall
    	CrmsfaLogTaskService logTaskService = new CrmsfaLogTaskService();
    	logTaskService.setInUserLogin(this.admin);
    	logTaskService.setInFromPartyId("admin");
    	logTaskService.setInInternalPartyId(partyIdLead);
    	logTaskService.setInOutbound("N");
    	logTaskService.setInWorkEffortPurposeTypeId("WEPT_TASK_PHONE_CALL"); 
    	logTaskService.setInWorkEffortName("WorkEffortName_2");
    	    	
    	runAndAssertServiceSuccess(logTaskService);
    	
    	String workEffortId = logTaskService.getOutWorkEffortId();
    	    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumberAfterAdd = countCompletedActivities(partyIdLead);
    	assertEquals("Number activities must be higher on 1 then before adding", activityNumberAfterAdd, activityNumber + 1);    	    	
    	
    	// Delete Activity
    	CrmsfaDeleteActivityEmailService  deleteActivityEmailService = new CrmsfaDeleteActivityEmailService();
    	deleteActivityEmailService.setInUserLogin(this.admin);
    	deleteActivityEmailService.setInWorkEffortId(workEffortId);
    	
    	runAndAssertServiceSuccess(deleteActivityEmailService);
    	
    	// Verify that the number of activities is the same as before adding 
    	int activityNumberAfterDelete = countCompletedActivities(partyIdLead);
    	assertEquals("Number activities must be same as before adding", activityNumberAfterDelete, activityNumber);
    	    
    	Debug.logInfo("DOWN --- testDeleteActivityAfterLogCall --- ", MODULE);
	}	
	
	/**
     * Test deleteActivityEmail service
     * Create Activity event = Create Log Call
     * 
     * @throws Exception
     */    
	public void testDeleteActivityAfterLogEmail() throws Exception {
    	Debug.logInfo("START --- testDeleteActivityAfterLogEmail --- ", MODULE);
    	
    	// Create lead 
    	CrmsfaCreateLeadService createLeadService = new CrmsfaCreateLeadService();
    	createLeadService.setInUserLogin(this.admin);    	    	    	
    	createLeadService.setInCompanyName(TEST_LEAD_COMPANY_NAME);
    	createLeadService.setInFirstName(TEST_LEAD_01_FIRST_NAME);
    	createLeadService.setInLastName(TEST_LEAD_01_LAST_NAME);
    	
    	runAndAssertServiceSuccess(createLeadService);
    	
    	String partyIdLead = createLeadService.getOutPartyId();
    	    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumber = countCompletedActivities(partyIdLead);    	
    	
    	// Create LogCall
    	CrmsfaLogTaskService logTaskService = new CrmsfaLogTaskService();
    	logTaskService.setInUserLogin(this.admin);
    	logTaskService.setInFromPartyId("admin");
    	logTaskService.setInInternalPartyId(partyIdLead);
    	logTaskService.setInOutbound("N");
    	logTaskService.setInWorkEffortPurposeTypeId("WEPT_TASK_EMAIL"); 
    	logTaskService.setInWorkEffortName("WorkEffortName");
    	   	
    	runAndAssertServiceSuccess(logTaskService);
    	
    	String workEffortId = logTaskService.getOutWorkEffortId();
    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumberAfterAdd = countCompletedActivities(partyIdLead);
    	assertEquals("Number activities must be higher on 1 then before adding", activityNumberAfterAdd, activityNumber + 1);    	    	
    	
    	// Delete Activity
    	CrmsfaDeleteActivityEmailService  deleteActivityEmailService = new CrmsfaDeleteActivityEmailService();
    	deleteActivityEmailService.setInUserLogin(this.admin);
    	deleteActivityEmailService.setInWorkEffortId(workEffortId);
    	
    	runAndAssertServiceSuccess(deleteActivityEmailService);
    	
    	// Verify that the number of activities is the same as before adding 
    	int activityNumberAfterDelete = countCompletedActivities(partyIdLead);
    	assertEquals("Number activities must be same as before adding", activityNumberAfterDelete, activityNumber);
    	  
    	Debug.logInfo("DOWN --- testDeleteActivityAfterLogEmail --- ", MODULE);
	}
			
    /**
     * Test deleteActivityEmail service
     * Create Activity event = Create Task
     * 
     * @throws Exception
     */    
	public void testDeleteActivityAfterCreateTask() throws Exception {
    	Debug.logInfo("START --- testDeleteActivityAfterCreateTask --- ", MODULE);
		
		// Create lead 
    	CrmsfaCreateLeadService createLeadService = new CrmsfaCreateLeadService();
    	createLeadService.setInUserLogin(this.admin);    	    	    	
    	createLeadService.setInCompanyName(TEST_LEAD_COMPANY_NAME);
    	createLeadService.setInFirstName(TEST_LEAD_01_FIRST_NAME);
    	createLeadService.setInLastName(TEST_LEAD_01_LAST_NAME);
    	
    	runAndAssertServiceSuccess(createLeadService);
    	
    	String partyIdLead = createLeadService.getOutPartyId();
    	    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumber = countPendingActivities(partyIdLead);    	
    	
    	// Create Task
    	CrmsfaCreateActivityService createActivityService =  new CrmsfaCreateActivityService();
    	createActivityService.setInUserLogin(this.admin);
    	createActivityService.setInEstimatedStartDate(UtilDateTime.nowTimestamp());
    	createActivityService.setInEstimatedCompletionDate(UtilDateTime.nowTimestamp());
    	createActivityService.setInWorkEffortName(TEST_TASK_NAME);
    	createActivityService.setInWorkEffortTypeId("TASK");
    	createActivityService.setInInternalPartyId(partyIdLead);    	
    	createActivityService.setInAvailabilityStatusId(StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE);
    	createActivityService.setInCurrentStatusId(StatusItemConstants.TaskStatus.TASK_STARTED);
    	createActivityService.setInForceIfConflicts("Y");
    	
    	runAndAssertServiceSuccess(createActivityService);
    	
    	String eventId = createActivityService.getOutWorkEffortId();
    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumberAfterAdd = countPendingActivities(partyIdLead);
    	assertEquals("Number activities must be higher on 1 then before adding", activityNumberAfterAdd, activityNumber + 1);    	    	
    	
    	// Delete Activity
    	CrmsfaDeleteActivityEmailService  deleteActivityEmailService = new CrmsfaDeleteActivityEmailService();
    	deleteActivityEmailService.setInUserLogin(this.admin);
    	deleteActivityEmailService.setInWorkEffortId(eventId);
    	
    	runAndAssertServiceSuccess(deleteActivityEmailService);
    	
    	// Verify that the number of activities is the same as before adding 
    	int activityNumberAfterDelete = countPendingActivities(partyIdLead);
    	assertEquals("Number activities must be same as before adding", activityNumberAfterDelete, activityNumber);
    	
    	Debug.logInfo("DOWN --- testDeleteActivityAfterCreateTask --- ", MODULE);
    }
    
    /**
     * Test deleteActivityEmail service
     * Create Activity event = Create Event 
     * 
     * @throws Exception
     */    
	public void testDeleteActivityAfterCreateEvent() throws Exception {
    	Debug.logInfo("START --- testDeleteActivityAfterCreateEvent --- ", MODULE);
		
		// Create lead 
    	CrmsfaCreateLeadService createLeadService = new CrmsfaCreateLeadService();
    	createLeadService.setInUserLogin(this.admin);    	    	    	
    	createLeadService.setInCompanyName(TEST_LEAD_COMPANY_NAME);
    	createLeadService.setInFirstName(TEST_LEAD_01_FIRST_NAME);
    	createLeadService.setInLastName(TEST_LEAD_01_LAST_NAME);
    	
    	runAndAssertServiceSuccess(createLeadService);
    	
    	String partyIdLead = createLeadService.getOutPartyId();
    	    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumber = countPendingActivities(partyIdLead);    	
    	
    	// Create Event
    	CrmsfaCreateActivityService createActivityService =  new CrmsfaCreateActivityService();
    	createActivityService.setInUserLogin(this.admin);
    	createActivityService.setInEstimatedStartDate(UtilDateTime.nowTimestamp());
    	createActivityService.setInEstimatedCompletionDate(UtilDateTime.nowTimestamp());
    	createActivityService.setInWorkEffortName(TEST_EVENT_NAME);
    	createActivityService.setInWorkEffortTypeId("EVENT");
    	createActivityService.setInInternalPartyId(partyIdLead);    	
    	createActivityService.setInAvailabilityStatusId(StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE);
    	createActivityService.setInCurrentStatusId(StatusItemConstants.EventStatus.EVENT_STARTED);
    	createActivityService.setInForceIfConflicts("Y");
    	
    	runAndAssertServiceSuccess(createActivityService);
    	
    	String eventId = createActivityService.getOutWorkEffortId();
    	
    	// Count the number of activities related to the lead    	    	
    	int activityNumberAfterAdd = countPendingActivities(partyIdLead);
    	assertEquals("Number activities must be higher on 1 then before adding", activityNumberAfterAdd, activityNumber + 1);    	    	
    	
    	// Delete Activity
    	CrmsfaDeleteActivityEmailService  deleteActivityEmailService = new CrmsfaDeleteActivityEmailService();
    	deleteActivityEmailService.setInUserLogin(this.admin);
    	deleteActivityEmailService.setInWorkEffortId(eventId);
    	
    	runAndAssertServiceSuccess(deleteActivityEmailService);
    	
    	// Verify that the number of activities is the same as before adding 
    	int activityNumberAfterDelete = countPendingActivities(partyIdLead);
    	assertEquals("Number activities must be same as before adding", activityNumberAfterDelete, activityNumber);
    	
    	Debug.logInfo("DOWN --- testDeleteActivityAfterCreateEvent --- ", MODULE);
    }
    	
	/**
     * Count the number of pending activities related to the lead
     * 
     * @param partyIdLead
     * @return Activities number
     */
	@SuppressWarnings("unchecked")
	private int countPendingActivities(String partyIdLead) {
    	int result = 0;
    	
    	CrmsfaFindActivitiesService findActivitiesService = new CrmsfaFindActivitiesService();     	
    	findActivitiesService.setInUserLogin(this.admin);
    	findActivitiesService.setInPartyId(partyIdLead);
    	
    	runAndAssertServiceSuccess(findActivitiesService);
    	
    	List<GenericValue> activityList = findActivitiesService.getOutPendingActivities();
    	    	
    	result = activityList.size();
    	
    	return result;
    }
	
	/**
	 * Count the number of completed activities related to the lead
	 * 
	 * @param partyIdLead
	 * @return Activities number
	 */
	@SuppressWarnings("unchecked")
	private int countCompletedActivities(String partyIdLead) {
		int result = 0;
    	
    	CrmsfaFindActivitiesService findActivitiesService = new CrmsfaFindActivitiesService();     	
    	findActivitiesService.setInUserLogin(this.admin);
    	findActivitiesService.setInPartyId(partyIdLead);
    	
    	runAndAssertServiceSuccess(findActivitiesService);    	
    	
    	List<GenericValue> activityList = findActivitiesService.getOutCompletedActivities();
    	
    	result = activityList.size();
    	
    	return result;
	}
}