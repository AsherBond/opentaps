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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opensourcestrategies.crmsfa.activities.UtilActivity;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.constants.WorkEffortPurposeTypeConstants;
import org.opentaps.base.constants.WorkEffortTypeConstants;
import org.opentaps.base.entities.ActivityFact;
import org.opentaps.base.entities.PartyContactMech;
import org.opentaps.base.entities.UserLogin;
import org.opentaps.base.entities.WorkEffort;
import org.opentaps.base.services.CrmsfaCreateActivityService;
import org.opentaps.base.services.CrmsfaCreateLeadService;
import org.opentaps.base.services.CrmsfaFindActivitiesService;
import org.opentaps.base.services.CrmsfaDeleteActivityEmailService;
import org.opentaps.base.services.CrmsfaSaveActivityEmailService;
import org.opentaps.base.services.CrmsfaLogTaskService;
import org.opentaps.common.reporting.etl.UtilEtl;
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

    private Timestamp testTimestamp1 = Timestamp.valueOf("2010-06-14 12:56:00");
    private Timestamp testTimestamp2 = Timestamp.valueOf("2010-06-15 12:56:00");
    private String internalPartyId1 = "DemoSalesManager";
    private String internalPartyId2 = "DemoSalesRep1";
    private String externalPartyId1 = "DemoLead1";
    private String externalPartyId2 = "DemoLead2";
    private String testWorkEffortName = "testWorkEffortName";

    private PartyDomainInterface partyDomain = null;
    private PartyRepositoryInterface partyRepository = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        partyDomain = domainsLoader.getDomainsDirectory().getPartyDomain();
        partyRepository = partyDomain.getPartyRepository();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        partyDomain = null;
        partyRepository = null;
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
        
        Party party = partyRepository.getPartyById(partyIdLead);
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
    }

    /**
     * Test deleteActivityEmail service with CommunicationEventId
     * Create Activity event = Save Activity Email
     * 
     * @throws Exception
     */
    public void testDeleteActivityAfterSaveEmailWithCommunicationEventId() throws Exception {
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

        Party party = partyRepository.getPartyById(partyIdLead);
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
    }

    /**
     * Test deleteActivityEmail service
     * Create Activity event = Create Log Call
     * 
     * @throws Exception
     */
    public void testDeleteActivityAfterLogCall() throws Exception {
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
    }

    /**
     * Test deleteActivityEmail service
     * Create Activity event = Create Log Call
     * 
     * @throws Exception
     */
    public void testDeleteActivityAfterLogEmail() throws Exception {
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
    }

    /**
     * Test deleteActivityEmail service
     * Create Activity event = Create Task
     * 
     * @throws Exception
     */
    public void testDeleteActivityAfterCreateTask() throws Exception {
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
    }

    /**
     * Test deleteActivityEmail service
     * Create Activity event = Create Event 
     * 
     * @throws Exception
     */
    public void testDeleteActivityAfterCreateEvent() throws Exception {
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

    /**
     * Performs data transformation to ActivityFacts table.
     * @exception Exception if an error occurs
     */
    public void testTransformToActivityFacts() throws Exception {

        // Get date dimention ID of work effort start date.
        Long dateDimId = UtilEtl.lookupDateDimensionForTimestamp(testTimestamp2, partyDomain.getInfrastructure().getDelegator());

        // Add the first visit work effor data to tranform from.

        UserLogin user = partyRepository.findOne(UserLogin.class, partyRepository.map(UserLogin.Fields.userLoginId, internalPartyId1));
        GenericValue userLogin = partyRepository.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map<String, Object> args = UtilMisc.toMap("userLogin", userLogin,
                "availabilityStatusId", StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE,
                "forceIfConflicts",  "Y",
                "workEffortName",  testWorkEffortName,
                "workEffortTypeId", WorkEffortTypeConstants.TASK,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_MEETING,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_STARTED,
                "estimatedStartDate", testTimestamp1,
                "estimatedCompletionDate", testTimestamp2
                );
        Map<String, Object> results = runAndAssertServiceSuccess("crmsfa.createActivity", args);
        String workEffortId1 = (String) results.get(WorkEffort.Fields.workEffortId.name());

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId1,
                "partyId",  externalPartyId1
                );
        runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId1,
                "partyId",  externalPartyId2
                );
        runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId1,
                "partyId",  internalPartyId2
                );
        runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId1,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_COMPLETED,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_MEETING,
                "actualCompletionDate", testTimestamp2
                );
        runAndAssertServiceSuccess("crmsfa.updateActivityWithoutAssoc", args);

        // Look up activity fact's before transformation

        EntityCondition partiesCond1 = EntityCondition.makeCondition(
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), externalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), internalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), RoleTypeConstants.LEAD),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), RoleTypeConstants.CAL_OWNER),
            EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
        List<ActivityFact> factsBefore1 = partyRepository.findList(ActivityFact.class, partiesCond1);

        EntityCondition partiesCond2 = EntityCondition.makeCondition(
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), externalPartyId2),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), internalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), RoleTypeConstants.LEAD),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), RoleTypeConstants.CAL_OWNER),
            EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
        List<ActivityFact> factsBefore2 = partyRepository.findList(ActivityFact.class, partiesCond2);

        EntityCondition partiesCond3 = EntityCondition.makeCondition(
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), externalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), internalPartyId2),
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), RoleTypeConstants.LEAD),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), RoleTypeConstants.CAL_ATTENDEE),
            EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
        List<ActivityFact> factsBefore3 = partyRepository.findList(ActivityFact.class, partiesCond3);

        EntityCondition partiesCond4 = EntityCondition.makeCondition(
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), externalPartyId2),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), internalPartyId2),
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), RoleTypeConstants.LEAD),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), RoleTypeConstants.CAL_ATTENDEE),
            EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
        List<ActivityFact> factsBefore4 = partyRepository.findList(ActivityFact.class, partiesCond4);

        // Execute transformation.
        args = UtilMisc.<String, Object>toMap("workEffortId", workEffortId1);
        args.put("userLogin", userLogin);
        runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.

        //==================================

        List<ActivityFact> facts1 = partyRepository.findList(ActivityFact.class, partiesCond1);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, facts1.size());

        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if (factsBefore1.size() > 0) {
            emailActivityCountBefore = factsBefore1.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore1.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore1.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore1.get(0).getOtherActivityCount();
        }

        ActivityFact fact1 = facts1.get(0);
        assertEquals("Email activity count is not good.", fact1.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact1.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact1.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact1.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

        //==================================

        List<ActivityFact> facts2 = partyRepository.findList(ActivityFact.class, partiesCond2);

        assertEquals("Record with target party [" + externalPartyId2 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, facts2.size());

        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if (factsBefore2.size() > 0) {
            emailActivityCountBefore = factsBefore2.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore2.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore2.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore2.get(0).getOtherActivityCount();
        }

        ActivityFact fact2 = facts2.get(0);
        assertEquals("Email activity count is not good.", fact2.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact2.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact2.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact2.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

        //==================================

        List<ActivityFact> facts3 = partyRepository.findList(ActivityFact.class, partiesCond3);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId2 + "] not added to ActivityFact entity.", 1, facts3.size());

        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if (factsBefore3.size() > 0) {
            emailActivityCountBefore = factsBefore3.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore3.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore3.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore3.get(0).getOtherActivityCount();
        }

        ActivityFact fact3 = facts3.get(0);
        assertEquals("Email activity count is not good.", fact3.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact3.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact3.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact3.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

        //==================================

        List<ActivityFact> facts4 = partyRepository.findList(ActivityFact.class, partiesCond4);

        assertEquals("Record with target party [" + externalPartyId2 + "], team party [" + internalPartyId2 + "] not added to ActivityFact entity.", 1, facts4.size());

        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if (factsBefore4.size() > 0) {
            emailActivityCountBefore = factsBefore4.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore4.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore4.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore4.get(0).getOtherActivityCount();
        }

        ActivityFact fact4 = facts4.get(0);
        assertEquals("Email activity count is not good.", fact4.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact4.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact4.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact4.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

        //==================================

        //Add the second work effor data to tranform from.

        args = UtilMisc.toMap("userLogin", userLogin,
                "availabilityStatusId", StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE,
                "forceIfConflicts",  "Y",
                "workEffortName",  testWorkEffortName,
                "workEffortTypeId", WorkEffortTypeConstants.TASK,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_STARTED,
                "estimatedStartDate", testTimestamp2,
                "estimatedCompletionDate", testTimestamp2
                );
        results = runAndAssertServiceSuccess("crmsfa.createActivity", args);
        String workEffortId2 = (String) results.get(WorkEffort.Fields.workEffortId.name());

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId2,
                "partyId",  externalPartyId1
                );
        runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId2,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_COMPLETED,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "actualCompletionDate", testTimestamp2
                );
        runAndAssertServiceSuccess("crmsfa.updateActivityWithoutAssoc", args);

        // Look up activity fact's before transformation
        EntityCondition partiesCond5 = EntityCondition.makeCondition(
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), externalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), internalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), RoleTypeConstants.LEAD),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), RoleTypeConstants.CAL_OWNER),
            EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
        List<ActivityFact> factsBefore5 = partyRepository.findList(ActivityFact.class, partiesCond5);

        //Execute transformation.
        args = UtilMisc.<String, Object>toMap("workEffortId", workEffortId2);
        args.put("userLogin", userLogin);
        runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.

        //==================================

        List<ActivityFact> facts5 = partyRepository.findList(ActivityFact.class, partiesCond5);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, facts5.size());

        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if (factsBefore5.size() > 0) {
            emailActivityCountBefore = factsBefore5.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore5.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore5.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore5.get(0).getOtherActivityCount();
        }

        ActivityFact fact5 = facts5.get(0);
        assertEquals("Email activity count is not good.", fact5.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact5.getPhoneCallActivityCount(), Long.valueOf(1 + phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact5.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact5.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

        //==================================

    }

    /**
     * Testing transformation of cancelled activities.
     * @exception Exception if an error occurs
     */
    public void testNotTransformCancelledActivities() throws Exception {

        // Get date dimention ID of work effort start date.
        Long dateDimId = UtilEtl.lookupDateDimensionForTimestamp(testTimestamp2, partyDomain.getInfrastructure().getDelegator());

        // Add the second work effor data to tranform from.

        UserLogin user = partyRepository.findOne(UserLogin.class, partyRepository.map(UserLogin.Fields.userLoginId, internalPartyId1));
        GenericValue userLogin = partyRepository.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map<String, Object> args = UtilMisc.toMap("userLogin", userLogin,
                "availabilityStatusId", StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE,
                "forceIfConflicts",  "Y",
                "workEffortName",  testWorkEffortName,
                "workEffortTypeId", WorkEffortTypeConstants.TASK,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_STARTED,
                "estimatedStartDate", testTimestamp2,
                "estimatedCompletionDate", testTimestamp2
                );
        Map<String, Object> results = runAndAssertServiceSuccess("crmsfa.createActivity", args);
        String workEffortId = (String) results.get(WorkEffort.Fields.workEffortId.name());

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId,
                "partyId",  externalPartyId1
                );
        runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_CANCELLED,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "actualCompletionDate", testTimestamp2
                );
        runAndAssertServiceSuccess("crmsfa.updateActivityWithoutAssoc", args);

        // Look up activity fact's before transformation
        EntityCondition partiesCond = EntityCondition.makeCondition(
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), externalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), internalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), RoleTypeConstants.LEAD),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), RoleTypeConstants.CAL_OWNER),
            EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
        List<ActivityFact> factsBefore = partyRepository.findList(ActivityFact.class, partiesCond);

        //Execute transformation.
        args = UtilMisc.<String, Object>toMap("workEffortId", workEffortId);
        runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.

        //==================================

        List<ActivityFact> facts = partyRepository.findList(ActivityFact.class, partiesCond);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] must be not added to ActivityFact entity, " + "because work effor is chancelled.", factsBefore.size(), facts.size());

        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if (factsBefore.size() > 0) {
            emailActivityCountBefore = factsBefore.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore.get(0).getOtherActivityCount();
        }

        ActivityFact fact = facts.get(0);
        assertEquals("Email activity count is not good.", fact.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

    }

    /**
     * Testing transformation of pending activities.
     * @exception Exception if an error occurs
     */
    public void testNotTransformPendingActivities() throws Exception {

        // Get date dimention ID of work effort start date.
        Long dateDimId = UtilEtl.lookupDateDimensionForTimestamp(testTimestamp2, partyDomain.getInfrastructure().getDelegator());

        // Add the second work effor data to tranform from.

        UserLogin user = partyRepository.findOne(UserLogin.class, partyRepository.map(UserLogin.Fields.userLoginId, internalPartyId1));
        GenericValue userLogin = partyRepository.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map<String, Object> args = UtilMisc.toMap("userLogin", userLogin,
                "availabilityStatusId", StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE,
                "forceIfConflicts",  "Y",
                "workEffortName",  testWorkEffortName,
                "workEffortTypeId", WorkEffortTypeConstants.TASK,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_SCHEDULED,
                "estimatedStartDate", testTimestamp2,
                "estimatedCompletionDate", testTimestamp2
                );
        Map<String, Object> results = runAndAssertServiceSuccess("crmsfa.createActivity", args);
        String workEffortId = (String) results.get(WorkEffort.Fields.workEffortId.name());

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId,
                "partyId",  externalPartyId1
                );
        runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_ON_HOLD,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "actualCompletionDate", testTimestamp2
                );
        runAndAssertServiceSuccess("crmsfa.updateActivityWithoutAssoc", args);

        // Look up activity fact's before transformation.
        EntityCondition partiesCond = EntityCondition.makeCondition(
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), externalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), internalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), RoleTypeConstants.LEAD),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), RoleTypeConstants.CAL_OWNER),
            EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
        List<ActivityFact> factsBefore = partyRepository.findList(ActivityFact.class, partiesCond);

        //Execute transformation.
        args = UtilMisc.<String, Object>toMap("workEffortId", workEffortId);
        runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.

        List<ActivityFact> facts = partyRepository.findList(ActivityFact.class, partiesCond);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] must be not added to ActivityFact entity, " + "because work effort is pending.", factsBefore.size(), facts.size());

        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if (factsBefore.size() > 0) {
            emailActivityCountBefore = factsBefore.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore.get(0).getOtherActivityCount();
        }

        ActivityFact fact = facts.get(0);
        assertEquals("Email activity count is not good.", fact.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

    }

    /**
     * Testing transformation after loging task.
     * @exception Exception if an error occurs
     */
    public void testTransformLogTaskActivity() throws Exception {

        // Get date dimention ID of work effort start date.
        Long dateDimId = UtilEtl.lookupDateDimensionForTimestamp(UtilDateTime.nowTimestamp(), partyDomain.getInfrastructure().getDelegator());

        // Look up activity fact's before transformation.
        EntityCondition partiesCond = EntityCondition.makeCondition(
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), externalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), internalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), RoleTypeConstants.LEAD),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), RoleTypeConstants.CAL_OWNER),
            EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
        List<ActivityFact> factsBefore = partyRepository.findList(ActivityFact.class, partiesCond);

        // Call logTask service.

        UserLogin user = partyRepository.findOne(UserLogin.class, partyRepository.map(UserLogin.Fields.userLoginId, internalPartyId1));
        GenericValue userLogin = partyRepository.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map<String, Object> args = UtilMisc.toMap("userLogin", userLogin,
                "internalPartyId", externalPartyId1,
                "fromPartyId", internalPartyId1 ,
                "outbound", "N" ,
                "workEffortName", testWorkEffortName);
        runAndAssertServiceSuccess("crmsfa.logTask", args);

        // Check if proper records was found.

        List<ActivityFact> facts = partyRepository.findList(ActivityFact.class, partiesCond);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, facts.size());

        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if (factsBefore.size() > 0) {
            emailActivityCountBefore = factsBefore.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore.get(0).getOtherActivityCount();
        }

        ActivityFact fact = facts.get(0);
        assertEquals("Email activity count is not good.", fact.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact.getOtherActivityCount(), Long.valueOf(1 + otherActivityCountBefore));

    }

    /**
     * Testing transformation after sending an email.
     * @exception Exception if an error occurs
     */
    public void testTransformSendEmailActivity() throws Exception {

        // Get date dimention ID of work effort start date.
        Long dateDimId = UtilEtl.lookupDateDimensionForTimestamp(UtilDateTime.nowTimestamp(), partyDomain.getInfrastructure().getDelegator());

        // Look up activity fact's before transformation.
        EntityCondition partiesCond = EntityCondition.makeCondition(
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), externalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), internalPartyId1),
            EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), RoleTypeConstants.LEAD),
            EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), RoleTypeConstants.CAL_OWNER),
            EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
        List<ActivityFact> factsBefore = partyRepository.findList(ActivityFact.class, partiesCond);

        // Call send email service.

        UserLogin user = partyRepository.findOne(UserLogin.class, partyRepository.map(UserLogin.Fields.userLoginId, internalPartyId1));
        GenericValue userLogin = partyRepository.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map<String, Object> args = UtilMisc.toMap("userLogin", userLogin,
                "contactMechIdFrom", "DemoMgrEmail1",
                "toEmail", "demo@demolead1.com",
                "content", "test content",
                "contentMimeTypeId", "text/plain",
                "subject", "test subject"
                );
        Map<String, Object> results = runAndAssertServiceSuccess("crmsfa.sendActivityEmail", args);

        String workEffortId = (String) results.get("workEffortId");
        Debug.logImportant("send email workEffortId = " + workEffortId, MODULE);

        // Check if proper records was found.

        List<ActivityFact> facts = partyRepository.findList(ActivityFact.class, partiesCond);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, facts.size());

        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if (factsBefore.size() > 0) {
            emailActivityCountBefore = factsBefore.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore.get(0).getOtherActivityCount();
        }

        ActivityFact fact = facts.get(0);
        assertEquals("Email activity count is not good.", fact.getEmailActivityCount(), Long.valueOf(1 + emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
    }

    /**
     * Test run service activities.transformAllActivities
     *
     * @throws Exception
     */
    public void testTransformAllActivities() throws Exception {
        Map<String, Object> callContext = new HashMap<String, Object>();
        callContext.put("userLogin", admin);

        runAndAssertServiceSuccess("activities.transformAllActivities", callContext);
    }

}