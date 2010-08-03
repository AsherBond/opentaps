/*
 * Copyright (c) 2010 Open Source Strategies, Inc. 
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;

import org.opentaps.tests.OpentapsTestCase;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.services.CrmsfaCreateActivityService;
import org.opentaps.base.services.CrmsfaCreateLeadService;
import org.opentaps.base.services.CrmsfaFindActivitiesService;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;


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
     * Set Lead qualify
     * 
     * @param partyId
     * @throws GenericServiceException
     */
    private void qualifyLead(String partyId) throws GenericServiceException {
    	 dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", partyId, 
    			 "statusId", StatusItemConstants.PartyLeadStatus.PTYLEAD_QUALIFIED, "userLogin", this.admin));             	 
    }
    
    /**
     * Test Leads creating and converts 
     *
     * @throws Exception
     */
    public void testCreateLeads() throws Exception {
    	Debug.logInfo("START --- testCreateLeads --- ", MODULE);
    	
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
    	
    	Iterator<Contact> contactIterator = setContact.iterator(); 
    	
    	// Verify contacts name
    	assertEquals("Does not match the name of the first contact", 
    			contactIterator.next().getName(), TEST_LEAD_01_FIRST_NAME + " " + TEST_LEAD_01_LAST_NAME);    	
    	assertEquals("Does not match the name of the second contact", 
    			contactIterator.next().getName(), TEST_LEAD_02_FIRST_NAME + " " + TEST_LEAD_02_LAST_NAME);
    	
    	Debug.logInfo("DOWN --- testCreateLeads --- ", MODULE);
    }
   
    /**
     * Run rmsfa.convertLead service
     * 
     * @param leadPartyId
     * @param accountPartyId
     * @return
     * @throws GenericEntityException
     */
	private String convertLead(String leadPartyId, String accountPartyId) throws GenericEntityException {    	            
    	Map<String, Object> callCtxt = new HashMap<String, Object>();
    	
        callCtxt.put("userLogin", this.admin);
        callCtxt.put("leadPartyId", leadPartyId);
        if(accountPartyId != null) {
        	callCtxt.put("accountPartyId", accountPartyId);
        }
              
        Map<String, Object> callResults = null;
        
        try {
            callResults = dispatcher.runSync("crmsfa.convertLead", callCtxt);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return "error";
        }

        if (callResults == null) {
            return "error";
        }

        return (String) callResults.get("partyId");        
    }
	
	/**
	 * Test merge Leads
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void testMergeLeads() throws Exception { 
		Debug.logInfo("START --- testMergeLeads --- ", MODULE);
		
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
    	    	    	
    	
    	// Merge  lead#2 to lead#1
    	Map<String, Object> callCtxt = new HashMap<String, Object>();
    	callCtxt.put("userLogin", this.admin);
    	callCtxt.put("partyIdFrom", partyIdSecond);
    	callCtxt.put("partyIdTo", partyIdFirst);
    	
    	runAndAssertServiceSuccess("opentaps.mergeParties", callCtxt);
    	
    	// Verify that Event belongs lead#1
    	findActivitiesService = new CrmsfaFindActivitiesService();     	
    	findActivitiesService.setInUserLogin(this.admin);
    	findActivitiesService.setInPartyId(partyIdFirst);
    	
    	runAndAssertServiceSuccess(findActivitiesService);    	
    	activityList = findActivitiesService.getOutPendingActivities();
        
    	assertEquals("Event not belongs lead#1", activityList.get(0).getString("workEffortId"), eventId);
    	
    	// Verify that lead#2 is not present    
    	Party partyAfterMerge = null;
    	try {
    		partyAfterMerge = partyRep.getPartyById(partyIdSecond);  
    	}
    	catch (EntityNotFoundException e) {
    		partyAfterMerge = null;
    	}
    	assertNull("Lead#2 is present after merge", partyAfterMerge);
    	
		Debug.logInfo("DOWN --- testMergeLeads --- ", MODULE);
	}	
}
