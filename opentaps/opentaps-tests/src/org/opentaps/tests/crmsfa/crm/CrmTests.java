/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.GenericServiceException;

import org.opentaps.tests.OpentapsTestCase;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.services.CrmsfaCreateLeadService;
import org.opentaps.base.services.ConvertLeadToContactService;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;

public class CrmTests extends OpentapsTestCase {
	private static final String MODULE = CrmTests.class.getName();
	
	private static final String TEST_LEAD_COMPANY_NAME = "TEST_Company ";	
    private static final String TEST_LEAD_01_FIRST_NAME = "Mark";
    private static final String TEST_LEAD_01_LAST_NAME = "Twain";    
    private static final String TEST_LEAD_02_FIRST_NAME = "William";
    private static final String TEST_LEAD_02_LAST_NAME = "Collins";
    	
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
     *    Test Leads creating and converts 
     *    using ConvertLeadToContactService
     *    
     * 1. Create  lead #1
     * 2. Qualify lead #1
     * 3. Convert lead #1 to an account
     * 4. Create  lead #2
     * 5. Qualify lead #2
     * 6. Convert lead #2 with same account as lead #1
     * 7. Verify the following:
     *    a) account name is the same as lead #1's company name
     *    b) account has two contacts with name of lead #1 and lead #2
     * @throws Exception
     */
    public void testCreateLeads0() throws Exception {   	
    	Debug.logInfo("START --- testCreateLeads0 --- ", MODULE);
    	
    	String companyName = TEST_LEAD_COMPANY_NAME + System.currentTimeMillis();
    	
    	CrmsfaCreateLeadService createLeadService = new CrmsfaCreateLeadService();
    	// First Lead
    	createLeadService.setInUserLogin(this.admin);    	    	    	
    	createLeadService.setInCompanyName(companyName);
    	createLeadService.setInFirstName(TEST_LEAD_01_FIRST_NAME);
    	createLeadService.setInLastName(TEST_LEAD_01_LAST_NAME);
    	
    	runAndAssertServiceSuccess(createLeadService);
    	
    	String partyIdFirst = createLeadService.getOutPartyId();    	
    	qualifyLead(partyIdFirst);
    	
    	ConvertLeadToContactService convertLead = new ConvertLeadToContactService();
    	convertLead.setInUserLogin(this.admin);
    	convertLead.setInPartyId(partyIdFirst);    	
    	
    	runAndAssertServiceSuccess(convertLead);
    	
    	String contactId = convertLead.getOutPartyId();    	    
    	
    	Contact contact = partyRep.getContactById(contactId);
    	Set<Account> setAccount = partyRep.getAccounts(contact);
    	Account account = setAccount.iterator().next();
    	
    	String accountId = account.getPartyId();
    	 	
    	// Second Lead    
    	createLeadService = new CrmsfaCreateLeadService();
    	createLeadService.setInUserLogin(this.admin);
    	createLeadService.setInCompanyName(companyName);
    	createLeadService.setInFirstName(TEST_LEAD_02_FIRST_NAME);
    	createLeadService.setInLastName(TEST_LEAD_02_LAST_NAME);
    	
    	runAndAssertServiceSuccess(createLeadService);
    	
    	String partyIdSecond = createLeadService.getOutPartyId();  	
    	qualifyLead(partyIdSecond);
    	
    	convertLead = new ConvertLeadToContactService();
    	convertLead.setInUserLogin(this.admin);
    	convertLead.setInPartyId(partyIdSecond);
    	convertLead.setInPartyGroupId(accountId);
    	    	
    	runAndAssertServiceSuccess(convertLead);
    	
    	// Verify     	    	
    	assertEquals("Account name is not as lead 1s company name", account.getName(), companyName);
    	
    	Set<Contact> setContact = account.getContacts();
    	
    	assertEquals("Account contacts not equals 2", setContact.size(), 2);
    	
    	Iterator<Contact> contactIterator = setContact.iterator(); 
    	
    	assertEquals("Does not match the name of the first contact", 
    			contactIterator.next().getName(), TEST_LEAD_01_FIRST_NAME + " " + TEST_LEAD_01_LAST_NAME);    	
    	assertEquals("Does not match the name of the second contact", 
    			contactIterator.next().getName(), TEST_LEAD_02_FIRST_NAME + " " + TEST_LEAD_02_LAST_NAME);
    	
    	Debug.logInfo("DOWN --- testCreateLeads0 --- ", MODULE);    	 
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
     *    Test Leads creating and converts 
     *    using rmsfa.convertLead service
     *    
     * 1. Create  lead #1
     * 2. Qualify lead #1
     * 3. Convert lead #1 to an account
     * 4. Create  lead #2
     * 5. Qualify lead #2
     * 6. Convert lead #2 with same account as lead #1
     * 7. Verify the following:
     *    a) account name is the same as lead #1's company name
     *    b) account has two contacts with name of lead #1 and lead #2
     * @throws Exception
     */
    public void testCreateLeads() throws Exception {
    	Debug.logInfo("START --- testCreateLeads --- ", MODULE);
    	
    	String companyName = TEST_LEAD_COMPANY_NAME + System.currentTimeMillis();
    	
    	CrmsfaCreateLeadService createLeadService = new CrmsfaCreateLeadService();
    	// First Lead
    	createLeadService.setInUserLogin(this.admin);    	    	    	
    	createLeadService.setInCompanyName(companyName);
    	createLeadService.setInFirstName(TEST_LEAD_01_FIRST_NAME);
    	createLeadService.setInLastName(TEST_LEAD_01_LAST_NAME);
    	
    	
    	runAndAssertServiceSuccess(createLeadService);
    	
    	String partyIdFirst = createLeadService.getOutPartyId();	    	    	
    	qualifyLead(partyIdFirst);
    	
    	String contactId = convertLead(partyIdFirst, null);    	    	
    	
    	Contact contact = partyRep.getContactById(contactId);
    	Set<Account> setAccount = partyRep.getAccounts(contact);
    	Account account = setAccount.iterator().next();
    	
    	String accountId = account.getPartyId();
    	
    	// Second Lead    
    	createLeadService = new CrmsfaCreateLeadService();
    	createLeadService.setInUserLogin(this.admin);
    	createLeadService.setInCompanyName(companyName);
    	createLeadService.setInFirstName(TEST_LEAD_02_FIRST_NAME);
    	createLeadService.setInLastName(TEST_LEAD_02_LAST_NAME);
    	
    	runAndAssertServiceSuccess(createLeadService);
    	
    	String partyIdSecond = createLeadService.getOutPartyId();  	
    	qualifyLead(partyIdSecond);
    	
    	convertLead(partyIdSecond, accountId);
    	
    	
    	// Verify         	
    	assertEquals("Account name is not as lead 1s company name", account.getName(), companyName);
    	
    	Set<Contact> setContact = account.getContacts();
    	
    	assertEquals("Account contacts not equals 2", setContact.size(), 2);
    	
    	Iterator<Contact> contactIterator = setContact.iterator(); 
    	
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
    
}
