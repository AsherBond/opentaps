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
package org.opentaps.tests.crmsfa.crm;

import java.util.List;
import java.util.Set;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.services.CrmsfaConvertLeadService;
import org.opentaps.base.services.CrmsfaCreateActivityService;
import org.opentaps.base.services.CrmsfaCreateLeadService;
import org.opentaps.base.services.CrmsfaFindActivitiesService;
import org.opentaps.base.services.CrmsfaLogTaskService;
import org.opentaps.base.services.OpentapsMergePartiesService;
import org.opentaps.base.services.SetPartyStatusService;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.tests.OpentapsTestCase;


public class CrmTests extends OpentapsTestCase {

    private static final String MODULE = CrmTests.class.getName();

    private static final String TEST_LEAD_COMPANY_NAME = "TEST_Company ";
    private static final String TEST_LEAD_01_FIRST_NAME = "Mark";
    private static final String TEST_LEAD_01_LAST_NAME = "Twain";
    private static final String TEST_LEAD_02_FIRST_NAME = "William";
    private static final String TEST_LEAD_02_LAST_NAME = "Collins";
    private static final String TEST_EVENT_NAME = "Test Event";

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

    /**
     * Qualifies a Lead using the "setPartyStatus" service.
     * @param leadPartyId the party ID of the lead to qualify
     * @throws GenericServiceException if an error occurs
     */
    private void qualifyLead(String leadPartyId) throws GenericServiceException {
        SetPartyStatusService ser = new SetPartyStatusService();
        ser.setInPartyId(leadPartyId);
        ser.setInStatusId(StatusItemConstants.PartyLeadStatus.PTYLEAD_QUALIFIED);
        ser.setInUserLogin(this.admin);
        runAndAssertServiceSuccess(ser);
    }

    /**
     * Converts a Lead using the "crmsfa.convertLead" service.
     * @param leadPartyId the party ID of the lead to convert
     * @param accountPartyId optional account party ID to use when converting
     * @return the partyId of the converted Lead
     */
    private String convertLead(String leadPartyId, String accountPartyId) {
        CrmsfaConvertLeadService ser = new CrmsfaConvertLeadService();
        ser.setInUserLogin(this.admin);
        ser.setInLeadPartyId(leadPartyId);
        if (UtilValidate.isNotEmpty(accountPartyId)) {
            ser.setInAccountPartyId(accountPartyId);
        }
        runAndAssertServiceSuccess(ser);
        return ser.getOutPartyId();
    }

    /**
     * Tests Leads creating and converts.
     * @throws Exception if an error occurs
     */
    public void testCreateAndConvertLeads() throws Exception {

        String companyName = TEST_LEAD_COMPANY_NAME + System.currentTimeMillis();

        // Create lead #1
        CrmsfaCreateLeadService createLeadService = new CrmsfaCreateLeadService();
        createLeadService.setInUserLogin(this.admin);
        createLeadService.setInCompanyName(companyName);
        createLeadService.setInFirstName(TEST_LEAD_01_FIRST_NAME);
        createLeadService.setInLastName(TEST_LEAD_01_LAST_NAME);

        runAndAssertServiceSuccess(createLeadService);

        String partyIdFirst = createLeadService.getOutPartyId();

        // Qualify lead #1
        qualifyLead(partyIdFirst);

        // Convert lead #1 to an account
        String contactId = convertLead(partyIdFirst, null);
        assertNotNull("Error occurred while converting the lead [" + partyIdFirst + "]", contactId);

        Contact contact = partyRep.getContactById(contactId);
        Set<Account> setAccount = partyRep.getAccounts(contact);
        Account account = setAccount.iterator().next();

        String accountId = account.getPartyId();

        // Create lead #2
        createLeadService = new CrmsfaCreateLeadService();
        createLeadService.setInUserLogin(this.admin);
        createLeadService.setInCompanyName(companyName);
        createLeadService.setInFirstName(TEST_LEAD_02_FIRST_NAME);
        createLeadService.setInLastName(TEST_LEAD_02_LAST_NAME);

        runAndAssertServiceSuccess(createLeadService);

        String partyIdSecond = createLeadService.getOutPartyId();

        // Qualify lead #2
        qualifyLead(partyIdSecond);

        // Convert lead #2 with same account as lead #1
        convertLead(partyIdSecond, accountId);

        // Verify that account name is the same as lead #1's company name
        assertEquals("Account name is not as lead 1s company name", account.getName(), companyName);

        Set<Contact> setContact = account.getContacts();

        // Verify that account has two contacts
        assertEquals("Account contacts not equals 2", setContact.size(), 2);

        // Verify contacts name, note: there is no guarantee of the order
        boolean foundFirst = false;
        boolean foundSecond = false;
        for (Contact c : setContact) {
            if ((TEST_LEAD_01_FIRST_NAME + " " + TEST_LEAD_01_LAST_NAME).equals(c.getName())) {
                foundFirst = true;
            } else if ((TEST_LEAD_02_FIRST_NAME + " " + TEST_LEAD_02_LAST_NAME).equals(c.getName())) {
                foundSecond = true;
            } else {
                fail("Does not match the name of the first or second contact : " + c.getName());
            }
        }
        assertTrue("Did not match the name of the first contact", foundFirst);
        assertTrue("Did not match the name of the second contact", foundSecond);
    }

    /**
     * Test merge Leads.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testMergeLeads() throws Exception {

        // Create lead #1
        CrmsfaCreateLeadService createLeadService = new CrmsfaCreateLeadService();
        createLeadService.setInUserLogin(this.admin);
        createLeadService.setInCompanyName(TEST_LEAD_COMPANY_NAME);
        createLeadService.setInFirstName(TEST_LEAD_01_FIRST_NAME);
        createLeadService.setInLastName(TEST_LEAD_01_LAST_NAME);

        runAndAssertServiceSuccess(createLeadService);

        String partyIdFirst = createLeadService.getOutPartyId();

        // Create lead #2
        createLeadService = new CrmsfaCreateLeadService();
        createLeadService.setInUserLogin(this.admin);
        createLeadService.setInCompanyName(TEST_LEAD_COMPANY_NAME);
        createLeadService.setInFirstName(TEST_LEAD_02_FIRST_NAME);
        createLeadService.setInLastName(TEST_LEAD_02_LAST_NAME);
        createLeadService.setInPrimaryEmail("primary@test.org");

        runAndAssertServiceSuccess(createLeadService);

        String partyIdSecond = createLeadService.getOutPartyId();

        // Create Event for lead#2
        CrmsfaCreateActivityService createActivityService =  new CrmsfaCreateActivityService();
        createActivityService.setInUserLogin(this.admin);
        createActivityService.setInEstimatedStartDate(UtilDateTime.nowTimestamp());
        createActivityService.setInEstimatedCompletionDate(UtilDateTime.nowTimestamp());
        createActivityService.setInWorkEffortName(TEST_EVENT_NAME);
        createActivityService.setInWorkEffortTypeId("EVENT");
        createActivityService.setInInternalPartyId(partyIdSecond);
        createActivityService.setInAvailabilityStatusId(StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE);
        createActivityService.setInCurrentStatusId(StatusItemConstants.EventStatus.EVENT_STARTED);
        createActivityService.setInForceIfConflicts("Y");

        runAndAssertServiceSuccess(createActivityService);

        String eventId = createActivityService.getOutWorkEffortId();

        // Verify that Event belongs lead#2
        CrmsfaFindActivitiesService findActivitiesService = new CrmsfaFindActivitiesService();
        findActivitiesService.setInUserLogin(this.admin);
        findActivitiesService.setInPartyId(partyIdSecond);

        runAndAssertServiceSuccess(findActivitiesService);

        List<GenericValue> activityList = findActivitiesService.getOutPendingActivities();
        assertEquals("Event not belongs lead#2", activityList.get(0).getString("workEffortId"), eventId);

        // Create LogEmail from partyIdSecond
        CrmsfaLogTaskService logTaskService = new CrmsfaLogTaskService();
        logTaskService.setInUserLogin(this.admin);
        logTaskService.setInFromPartyId("admin");
        logTaskService.setInInternalPartyId(partyIdSecond);
        logTaskService.setInOutbound("N");
        logTaskService.setInWorkEffortPurposeTypeId("WEPT_TASK_EMAIL");
        logTaskService.setInWorkEffortName("WorkEffortName subj");

        runAndAssertServiceSuccess(logTaskService);

        String workEffortId = logTaskService.getOutWorkEffortId();

        GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        List<GenericValue> communicationEvents = workEffort.getRelated("CommunicationEventWorkEff");
        String communicationEventId = communicationEvents.get(0).getString("communicationEventId");

        GenericValue communicationEvent = delegator.findByPrimaryKey("CommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventId));
        //GenericValue communicationEventRole = delegator.findByPrimaryKey("CommunicationEventRole", UtilMisc.toMap("communicationEventId", communicationEventId));
        List<GenericValue> communicationEventRole = delegator.findByAnd("CommunicationEventRole", UtilMisc.toMap("communicationEventId", communicationEventId, "roleTypeId", "ORIGINATOR"));

        // Verify CommunicationEvent partyIdFrom before merge
        assertEquals("CommunicationEvent partyIdFrom must be " + partyIdSecond, partyIdSecond, communicationEvent.getString("partyIdFrom"));

        // Verify CommunicationEventRole partyId before merge
        assertEquals("CommunicationEventRole partyId must be " + partyIdSecond, partyIdSecond, communicationEventRole.get(0).getString("partyId"));

        // Create LogEmail to partyIdSecond
        logTaskService = new CrmsfaLogTaskService();
        logTaskService.setInUserLogin(this.admin);
        logTaskService.setInFromPartyId("admin");
        logTaskService.setInInternalPartyId(partyIdSecond);
        logTaskService.setInOutbound("Y");
        logTaskService.setInWorkEffortPurposeTypeId("WEPT_TASK_EMAIL");
        logTaskService.setInWorkEffortName("WorkEffortName subj");

        runAndAssertServiceSuccess(logTaskService);

        String workEffortToId = logTaskService.getOutWorkEffortId();

        GenericValue workEffortTo = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortToId));
        List<GenericValue> communicationEventsTo = workEffortTo.getRelated("CommunicationEventWorkEff");
        String communicationEventToId = communicationEventsTo.get(0).getString("communicationEventId");

        GenericValue communicationEventTo = delegator.findByPrimaryKey("CommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventToId));

        // Verify CommunicationEvent partyIdTo before merge
        assertEquals("CommunicationEvent partyIdTo must be " + partyIdSecond, partyIdSecond, communicationEventTo.getString("partyIdTo"));

        // This pause for avoid merge leads error
        pause("Wait while automatic notification e-mail will be sent", 10000);

        // Merge  lead#2 to lead#1
        OpentapsMergePartiesService mergeService = new OpentapsMergePartiesService();
        mergeService.setInUserLogin(this.admin);
        mergeService.setInPartyIdFrom(partyIdSecond);
        mergeService.setInPartyIdTo(partyIdFirst);
        runAndAssertServiceSuccess(mergeService);

        // Verify that Event belongs lead#1
        findActivitiesService = new CrmsfaFindActivitiesService();
        findActivitiesService.setInUserLogin(this.admin);
        findActivitiesService.setInPartyId(partyIdFirst);

        runAndAssertServiceSuccess(findActivitiesService);
        activityList = findActivitiesService.getOutPendingActivities();

        assertEquals("Event not belongs lead#1", activityList.get(0).getString("workEffortId"), eventId);

        // Verify CommunicationEven and CommunicationEventRole
        workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        communicationEvents = workEffort.getRelated("CommunicationEventWorkEff");
        communicationEventId = communicationEvents.get(0).getString("communicationEventId");

        communicationEvent = delegator.findByPrimaryKey("CommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventId));
        communicationEventRole = delegator.findByAnd("CommunicationEventRole", UtilMisc.toMap("communicationEventId", communicationEventId, "roleTypeId", "ORIGINATOR"));

        // Verify CommunicationEvent partyIdFrom after merge
        assertEquals("Communication Event partyIdFrom after merge must be " + partyIdFirst, partyIdFirst, communicationEvent.getString("partyIdFrom"));

        // Verify CommunicationEventRole partyId after merge
        assertEquals("CommunicationEventRole partyId after merge must be " + partyIdFirst, partyIdFirst, communicationEventRole.get(0).getString("partyId"));

        // Verify CommunicationEvent partyIdTo after merge
        workEffortTo = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortToId));
        communicationEventsTo = workEffortTo.getRelated("CommunicationEventWorkEff");
        communicationEventToId = communicationEventsTo.get(0).getString("communicationEventId");

        communicationEventTo = delegator.findByPrimaryKey("CommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventToId));

        // Verify CommunicationEvent partyIdTo after merge
        assertEquals("Communication Event partyIdTo after merge must be " + partyIdFirst, partyIdFirst, communicationEventTo.getString("partyIdTo"));

        // Verify that lead#2 is not present
        Party partyAfterMerge = null;
        try {
            partyAfterMerge = partyRep.getPartyById(partyIdSecond);
        } catch (EntityNotFoundException e) {
            partyAfterMerge = null;
        }
        assertNull("Lead#2 is present after merge", partyAfterMerge);
    }
}
