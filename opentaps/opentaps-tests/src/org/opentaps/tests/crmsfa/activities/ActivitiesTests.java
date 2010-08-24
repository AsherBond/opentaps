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
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.constants.WorkEffortPurposeTypeConstants;
import org.opentaps.base.constants.WorkEffortTypeConstants;
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
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.activities.ActivitiesDomainInterface;
import org.opentaps.domain.activities.ActivityFactRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
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

    private DomainsLoader domainLoader = null;
    private PartyDomainInterface partyDomain = null;
    private PartyRepositoryInterface partyRepository = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        partyDomain = domainsLoader.getDomainsDirectory().getPartyDomain();
        partyRepository = partyDomain.getPartyRepository();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        domainLoader = null;
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
        ActivitiesDomainInterface activitiesDomain = domainLoader.getDomainsDirectory().getActivitiesDomain();
        ActivityFactRepositoryInterface activityFactBefore1 = activitiesDomain.getActivityFactRepository();
        ActivityFactRepositoryInterface activityFactBefore2 = activitiesDomain.getActivityFactRepository();
        ActivityFactRepositoryInterface activityFactBefore3 = activitiesDomain.getActivityFactRepository();
        ActivityFactRepositoryInterface activityFactBefore4 = activitiesDomain.getActivityFactRepository();
        ActivityFactRepositoryInterface activityFactBefore5 = activitiesDomain.getActivityFactRepository();

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
        activityFactBefore1.setTargetPartyId(externalPartyId1);
        activityFactBefore1.setTeamMemberPartyId(internalPartyId1);
        activityFactBefore1.setTargetRoleTypeId(RoleTypeConstants.LEAD);
        activityFactBefore1.setTeamMemeberRoleTypeId(RoleTypeConstants.CAL_OWNER);
        activityFactBefore1.setDateDimensionId(dateDimId);

        activityFactBefore1.findActivityFacts();

        activityFactBefore2.setTargetPartyId(externalPartyId2);
        activityFactBefore2.setTeamMemberPartyId(internalPartyId1);
        activityFactBefore2.setTargetRoleTypeId(RoleTypeConstants.LEAD);
        activityFactBefore2.setTeamMemeberRoleTypeId(RoleTypeConstants.CAL_OWNER);
        activityFactBefore2.setDateDimensionId(dateDimId);

        activityFactBefore2.findActivityFacts();

        activityFactBefore3.setTargetPartyId(externalPartyId1);
        activityFactBefore3.setTeamMemberPartyId(internalPartyId2);
        activityFactBefore3.setTargetRoleTypeId(RoleTypeConstants.LEAD);
        activityFactBefore3.setTeamMemeberRoleTypeId(RoleTypeConstants.CAL_ATTENDEE);
        activityFactBefore3.setDateDimensionId(dateDimId);

        activityFactBefore3.findActivityFacts();

        activityFactBefore4.setTargetPartyId(externalPartyId2);
        activityFactBefore4.setTeamMemberPartyId(internalPartyId2);
        activityFactBefore4.setTargetRoleTypeId(RoleTypeConstants.LEAD);
        activityFactBefore4.setTeamMemeberRoleTypeId(RoleTypeConstants.CAL_ATTENDEE);
        activityFactBefore4.setDateDimensionId(dateDimId);

        activityFactBefore4.findActivityFacts();

        // Execute transformation.
        args = UtilMisc.<String, Object>toMap("workEffortId", workEffortId1);
        args.put("userLogin", userLogin);
        runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.

        //==================================

        long emailActivityCountBefore = activityFactBefore1.getEmailActivityCount();
        long phoneCallActivityCountBefore = activityFactBefore1.getPhoneCallActivityCount();
        long visitActivityCountBefore = activityFactBefore1.getVisitActivityCount();
        long otherActivityCountBefore = activityFactBefore1.getOtherActivityCount();
        long totalActivityCountBefore = activityFactBefore1.getTotalActivityCount();

        int factsSize = activityFactBefore1.findActivityFacts().size();

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, factsSize);

        assertEquals("Email activity count is not good.", (Long) activityFactBefore1.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", (Long) activityFactBefore1.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", (Long) activityFactBefore1.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", (Long) activityFactBefore1.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        assertEquals("Total activity count is not good.", (Long) activityFactBefore1.getTotalActivityCount(), Long.valueOf(1 + totalActivityCountBefore));

        //==================================

        emailActivityCountBefore = activityFactBefore2.getEmailActivityCount();
        phoneCallActivityCountBefore = activityFactBefore2.getPhoneCallActivityCount();
        visitActivityCountBefore = activityFactBefore2.getVisitActivityCount();
        otherActivityCountBefore = activityFactBefore2.getOtherActivityCount();
        totalActivityCountBefore = activityFactBefore2.getTotalActivityCount();

        factsSize = activityFactBefore2.findActivityFacts().size();

        assertEquals("Record with target party [" + externalPartyId2 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, factsSize);

        assertEquals("Email activity count is not good.", (Long) activityFactBefore2.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", (Long) activityFactBefore2.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", (Long) activityFactBefore2.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", (Long) activityFactBefore2.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        assertEquals("Total activity count is not good.", (Long) activityFactBefore2.getTotalActivityCount(), Long.valueOf(1 + totalActivityCountBefore));

        //==================================

        emailActivityCountBefore = activityFactBefore3.getEmailActivityCount();
        phoneCallActivityCountBefore = activityFactBefore3.getPhoneCallActivityCount();
        visitActivityCountBefore = activityFactBefore3.getVisitActivityCount();
        otherActivityCountBefore = activityFactBefore3.getOtherActivityCount();
        totalActivityCountBefore = activityFactBefore3.getTotalActivityCount();

        factsSize = activityFactBefore3.findActivityFacts().size();

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId2 + "] not added to ActivityFact entity.", 1, factsSize);

        assertEquals("Email activity count is not good.", (Long) activityFactBefore3.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", (Long) activityFactBefore3.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", (Long) activityFactBefore3.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", (Long) activityFactBefore3.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        assertEquals("Total activity count is not good.", (Long) activityFactBefore3.getTotalActivityCount(), Long.valueOf(1 + totalActivityCountBefore));

        //==================================

        emailActivityCountBefore = activityFactBefore4.getEmailActivityCount();
        phoneCallActivityCountBefore = activityFactBefore4.getPhoneCallActivityCount();
        visitActivityCountBefore = activityFactBefore4.getVisitActivityCount();
        otherActivityCountBefore = activityFactBefore4.getOtherActivityCount();
        totalActivityCountBefore = activityFactBefore4.getTotalActivityCount();

        factsSize = activityFactBefore4.findActivityFacts().size();

        assertEquals("Record with target party [" + externalPartyId2 + "], team party [" + internalPartyId2 + "] not added to ActivityFact entity.", 1, factsSize);

        assertEquals("Email activity count is not good.", (Long) activityFactBefore4.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", (Long) activityFactBefore4.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", (Long) activityFactBefore4.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", (Long) activityFactBefore4.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        assertEquals("Total activity count is not good.", (Long) activityFactBefore4.getTotalActivityCount(), Long.valueOf(1 + totalActivityCountBefore));

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
        activityFactBefore5.setTargetPartyId(externalPartyId1);
        activityFactBefore5.setTeamMemberPartyId(internalPartyId1);
        activityFactBefore5.setTargetRoleTypeId(RoleTypeConstants.LEAD);
        activityFactBefore5.setTeamMemeberRoleTypeId(RoleTypeConstants.CAL_OWNER);
        activityFactBefore5.setDateDimensionId(dateDimId);

        activityFactBefore5.findActivityFacts();

        //Execute transformation.
        args = UtilMisc.<String, Object>toMap("workEffortId", workEffortId2);
        args.put("userLogin", userLogin);
        runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.

        //==================================

        emailActivityCountBefore = activityFactBefore5.getEmailActivityCount();
        phoneCallActivityCountBefore = activityFactBefore5.getPhoneCallActivityCount();
        visitActivityCountBefore = activityFactBefore5.getVisitActivityCount();
        otherActivityCountBefore = activityFactBefore5.getOtherActivityCount();
        totalActivityCountBefore = activityFactBefore5.getTotalActivityCount();

        factsSize = activityFactBefore5.findActivityFacts().size();

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, factsSize);

        assertEquals("Email activity count is not good.", (Long) activityFactBefore5.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", (Long) activityFactBefore5.getPhoneCallActivityCount(), Long.valueOf(1 + phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", (Long) activityFactBefore5.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", (Long) activityFactBefore5.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        assertEquals("Total activity count is not good.", (Long) activityFactBefore5.getTotalActivityCount(), Long.valueOf(1 + totalActivityCountBefore));

        //==================================
    }

    /**
     * Testing transformation of cancelled activities.
     * @exception Exception if an error occurs
     */
    public void testNotTransformCancelledActivities() throws Exception {
        ActivitiesDomainInterface activitiesDomain = domainLoader.getDomainsDirectory().getActivitiesDomain();
        ActivityFactRepositoryInterface activityFact = activitiesDomain.getActivityFactRepository();

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
        activityFact.setTargetPartyId(externalPartyId1);
        activityFact.setTeamMemberPartyId(internalPartyId1);
        activityFact.setTargetRoleTypeId(RoleTypeConstants.LEAD);
        activityFact.setTeamMemeberRoleTypeId(RoleTypeConstants.CAL_OWNER);
        activityFact.setDateDimensionId(dateDimId);

        int sizefactsBefore = activityFact.findActivityFacts().size();

        long emailActivityCountBefore = activityFact.getEmailActivityCount();
        long phoneCallActivityCountBefore = activityFact.getPhoneCallActivityCount();
        long visitActivityCountBefore = activityFact.getVisitActivityCount();
        long otherActivityCountBefore = activityFact.getOtherActivityCount();
        long totalActivityCountBefore = activityFact.getTotalActivityCount();


        //Execute transformation.
        args = UtilMisc.<String, Object>toMap("workEffortId", workEffortId);
        runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.
        int factsSize = activityFact.findActivityFacts().size();

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] must be not added to ActivityFact entity, " + "because work effor is chancelled.", sizefactsBefore, factsSize);

        assertEquals("Email activity count is not good.", (Long) activityFact.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", (Long) activityFact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", (Long) activityFact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", (Long) activityFact.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        assertEquals("Total activity count is not good.", (Long) activityFact.getTotalActivityCount(), Long.valueOf(totalActivityCountBefore));
    }

    /**
     * Testing transformation of pending activities.
     * @exception Exception if an error occurs
     */
    public void testNotTransformPendingActivities() throws Exception {
        ActivitiesDomainInterface activitiesDomain = domainLoader.getDomainsDirectory().getActivitiesDomain();
        ActivityFactRepositoryInterface activityFact = activitiesDomain.getActivityFactRepository();

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
        activityFact.setTargetPartyId(externalPartyId1);
        activityFact.setTeamMemberPartyId(internalPartyId1);
        activityFact.setTargetRoleTypeId(RoleTypeConstants.LEAD);
        activityFact.setTeamMemeberRoleTypeId(RoleTypeConstants.CAL_OWNER);
        activityFact.setDateDimensionId(dateDimId);

        int sizefactsBefore = activityFact.findActivityFacts().size();

        long emailActivityCountBefore = activityFact.getEmailActivityCount();
        long phoneCallActivityCountBefore = activityFact.getPhoneCallActivityCount();
        long visitActivityCountBefore = activityFact.getVisitActivityCount();
        long otherActivityCountBefore = activityFact.getOtherActivityCount();
        long totalActivityCountBefore = activityFact.getTotalActivityCount();

        //Execute transformation.
        args = UtilMisc.<String, Object>toMap("workEffortId", workEffortId);
        runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.
        int factsSize = activityFact.findActivityFacts().size();

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] must be not added to ActivityFact entity, " + "because work effort is pending.", sizefactsBefore, factsSize);

        assertEquals("Email activity count is not good.", (Long) activityFact.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", (Long) activityFact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", (Long) activityFact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", (Long) activityFact.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        assertEquals("Total activity count is not good.", (Long) activityFact.getTotalActivityCount(), Long.valueOf(totalActivityCountBefore));
    }

    /**
     * Testing transformation after loging task.
     * @exception Exception if an error occurs
     */
    public void testTransformLogTaskActivity() throws Exception {
        ActivitiesDomainInterface activitiesDomain = domainLoader.getDomainsDirectory().getActivitiesDomain();
        ActivityFactRepositoryInterface activityFact = activitiesDomain.getActivityFactRepository();

        // Get date dimention ID of work effort start date.
        Long dateDimId = UtilEtl.lookupDateDimensionForTimestamp(UtilDateTime.nowTimestamp(), partyDomain.getInfrastructure().getDelegator());

        // Look up activity fact's before transformation.
        activityFact.setTargetPartyId(externalPartyId1);
        activityFact.setTeamMemberPartyId(internalPartyId1);
        activityFact.setTargetRoleTypeId(RoleTypeConstants.LEAD);
        activityFact.setTeamMemeberRoleTypeId(RoleTypeConstants.CAL_OWNER);
        activityFact.setDateDimensionId(dateDimId);

        activityFact.findActivityFacts();

        long emailActivityCountBefore = activityFact.getEmailActivityCount();
        long phoneCallActivityCountBefore = activityFact.getPhoneCallActivityCount();
        long visitActivityCountBefore = activityFact.getVisitActivityCount();
        long otherActivityCountBefore = activityFact.getOtherActivityCount();
        long totalActivityCountBefore = activityFact.getTotalActivityCount();

        // Call logTask service.
        UserLogin user = partyRepository.findOne(UserLogin.class, partyRepository.map(UserLogin.Fields.userLoginId, internalPartyId1));
        GenericValue userLogin = partyRepository.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map<String, Object> args = UtilMisc.toMap("userLogin", userLogin,
                "internalPartyId", externalPartyId1,
                "fromPartyId", internalPartyId1 ,
                "outbound", "N" ,
                "workEffortName", testWorkEffortName);

        Map<String, Object> results = runAndAssertServiceSuccess("crmsfa.logTask", args);

        String workEffortId = (String) results.get("workEffortId");
        Debug.logImportant("task workEffortId = " + workEffortId, MODULE);

        // Check if proper records was found.
        int factsSize = activityFact.findActivityFacts().size();

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, factsSize);

        assertEquals("Email activity count is not good.", (Long) activityFact.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", (Long) activityFact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", (Long) activityFact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", (Long) activityFact.getOtherActivityCount(), Long.valueOf(1 + otherActivityCountBefore));
        assertEquals("Total activity count is not good.", (Long) activityFact.getTotalActivityCount(), Long.valueOf(1 + totalActivityCountBefore));
    }

    /**
     * Testing transformation after sending an email.
     * @exception Exception if an error occurs
     */
    public void testTransformSendEmailActivity() throws Exception {
        ActivitiesDomainInterface activitiesDomain = domainLoader.getDomainsDirectory().getActivitiesDomain();
        ActivityFactRepositoryInterface activityFact = activitiesDomain.getActivityFactRepository();

        // Get date dimention ID of work effort start date.
        Long dateDimId = UtilEtl.lookupDateDimensionForTimestamp(UtilDateTime.nowTimestamp(), partyDomain.getInfrastructure().getDelegator());

        activityFact.setTargetPartyId(externalPartyId1);
        activityFact.setTeamMemberPartyId(internalPartyId1);
        activityFact.setTargetRoleTypeId(RoleTypeConstants.LEAD);
        activityFact.setTeamMemeberRoleTypeId(RoleTypeConstants.CAL_OWNER);
        activityFact.setDateDimensionId(dateDimId);

        activityFact.findActivityFacts();

        long emailActivityCountBefore = activityFact.getEmailActivityCount();
        long phoneCallActivityCountBefore = activityFact.getPhoneCallActivityCount();
        long visitActivityCountBefore = activityFact.getVisitActivityCount();
        long otherActivityCountBefore = activityFact.getOtherActivityCount();
        long totalActivityCountBefore = activityFact.getTotalActivityCount();

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
        int factsSize = activityFact.findActivityFacts().size();

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, factsSize);

        assertEquals("Email activity count is not good.", (Long) activityFact.getEmailActivityCount(), Long.valueOf(1 + emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", (Long) activityFact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", (Long) activityFact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", (Long) activityFact.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        assertEquals("Total activity count is not good.", (Long) activityFact.getTotalActivityCount(), Long.valueOf(1 + totalActivityCountBefore));
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