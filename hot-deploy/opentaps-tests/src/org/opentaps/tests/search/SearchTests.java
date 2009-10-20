/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.tests.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentaps.crmsfa.search.CrmsfaSearchService;
import org.opentaps.domain.base.entities.PartyGroup;
import org.opentaps.domain.base.entities.SalesOpportunity;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Lead;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.purchasing.search.PurchasingSearchService;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Test case for the search services.
 */
public class SearchTests extends OpentapsTestCase {

    private static final String MODULE = SearchTests.class.getName();
    private static final long INDEX_PAUSE = 5000;

    /**
     * Test party search.
     * - create an Account and a Supplier and check the CrmsfaSearchService and PurchasingSearchService can find them.
     * @throws Exception if an error occurs
     */
    public void testPartySearch() throws Exception {
        // first we try to create a supplier and a customer, they have same name "Sangfroid Paper Ltd."
        // create a supplier from template of DemoSupplier
        String partyGroupName = "Sangfroid Paper Ltd.";

        // create a supplier by service purchasing.createSupplier
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("groupName", partyGroupName);
        callCtxt.put("requires1099", "Y");
        runAndAssertServiceSuccess("purchasing.createSupplier", callCtxt);

        // create a account by service crmsfa.createAccount
        callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("accountName", partyGroupName);
        runAndAssertServiceSuccess("crmsfa.createAccount", callCtxt);

        // note: the indexing is async, so need to wait a little for the index to be in sync
        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        String keyWord = "Sangfroid";
        CrmsfaSearchService crmSearch = new CrmsfaSearchService();
        crmSearch.setInfrastructure(new Infrastructure(dispatcher));
        crmSearch.setUser(new User(admin));
        crmSearch.loadDomainsDirectory();
        crmSearch.setKeywords(keyWord);
        crmSearch.setSearchAccounts(true);
        crmSearch.search();
        List<Account> parties = crmSearch.getAccounts();
        assertNotEmpty("Should have found some Accounts by keyword " + keyWord, parties);
        for (Party party : parties) {
            // assert we find the customer one
            assertIsCustomerNotSupplier(party);
        }

        PurchasingSearchService purchasingSearch = new PurchasingSearchService();
        purchasingSearch.setInfrastructure(new Infrastructure(dispatcher));
        purchasingSearch.setUser(new User(admin));
        purchasingSearch.loadDomainsDirectory();
        purchasingSearch.setKeywords(keyWord);
        purchasingSearch.setSearchSuppliers(true);
        purchasingSearch.search();
        List<PartyGroup> partyGroups = purchasingSearch.getSuppliers();
        assertNotEmpty("Should found some Suppliers by keyword " + keyWord, partyGroups);
        for (PartyGroup party : partyGroups) {
            // assert we find the supplier one
            assertIsSupplierNotCustomer(party);
        }
    }

    /**
     * Test that an account will show up in the list of search results for accounts, but not for other party types.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testAccountSearch() throws Exception {
        //  create a new account
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("accountName", "searchaccount");
        Map<String, Object> results = runAndAssertServiceSuccess("crmsfa.createAccount", callCtxt);
        String partyId = (String) results.get("partyId");

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        //  call the CRM search with the name of the account
        CrmsfaSearchService crmSearch = crmsfaSearchParties("searchaccount");

        // verify new account shows up in the list of Account
        List<Account> accounts = crmSearch.getAccounts();
        Set<String> accountIds = Entity.getDistinctFieldValues(String.class, accounts, Account.Fields.partyId);
        assertTrue("Did not find the new account [" + partyId + "]", accountIds.contains(partyId));

        // verify new account does not shows up in the list of Contact or Lead
        List<Contact> contacts = crmSearch.getContacts();
        Set<String> contactIds = Entity.getDistinctFieldValues(String.class, contacts, Contact.Fields.partyId);
        List<Lead> leads = crmSearch.getLeads();
        Set<String> leadIds = Entity.getDistinctFieldValues(String.class, leads, Lead.Fields.partyId);
        assertFalse("Should not have found the new account [" + partyId + "] in the Contacts results", contactIds.contains(partyId));
        assertFalse("Should not have found the new account [" + partyId + "] in the Leads results", leadIds.contains(partyId));

        //  call the Purchasing search
        PurchasingSearchService purchasingSearch = purchasingSearchParties("searchaccount");

        // verify account does not show up in the List of Suppliers
        List<PartyGroup> suppliers = purchasingSearch.getSuppliers();
        Set<String> supplierIds = Entity.getDistinctFieldValues(String.class, suppliers, PartyGroup.Fields.partyId);
        assertFalse("Should not have found the new account [" + partyId + "] in the Supplier results", supplierIds.contains(partyId));
    }

    /**
     * Test that a contact will show up in the list of search results for contacts, but not for other party types.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testContactSearch() throws Exception {
        //  create a new contact
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("firstName", "Test");
        callCtxt.put("lastName", "searchcontact");
        Map<String, Object> results = runAndAssertServiceSuccess("crmsfa.createContact", callCtxt);
        String partyId = (String) results.get("partyId");

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        //  call the CRM search with the name of the contact
        CrmsfaSearchService crmSearch = crmsfaSearchParties("searchcontact");

        // verify new contact shows up in the list of Contact
        List<Contact> contacts = crmSearch.getContacts();
        Set<String> contactIds = Entity.getDistinctFieldValues(String.class, contacts, Contact.Fields.partyId);
        assertTrue("Did not find the new contact [" + partyId + "]", contactIds.contains(partyId));

        // verify new contact does not shows up in the list of Account or Lead
        List<Account> accounts = crmSearch.getAccounts();
        Set<String> accountIds = Entity.getDistinctFieldValues(String.class, accounts, Account.Fields.partyId);
        List<Lead> leads = crmSearch.getLeads();
        Set<String> leadIds = Entity.getDistinctFieldValues(String.class, leads, Lead.Fields.partyId);
        assertFalse("Should not have found the new contact [" + partyId + "] in the Accounts results", accountIds.contains(partyId));
        assertFalse("Should not have found the new contact [" + partyId + "] in the Leads results", leadIds.contains(partyId));

        //  call the Purchasing search
        PurchasingSearchService purchasingSearch = purchasingSearchParties("searchcontact");

        // verify contact does not show up in the List of Suppliers
        List<PartyGroup> suppliers = purchasingSearch.getSuppliers();
        Set<String> supplierIds = Entity.getDistinctFieldValues(String.class, suppliers, PartyGroup.Fields.partyId);
        assertFalse("Should not have found the new contact [" + partyId + "] in the Supplier results", supplierIds.contains(partyId));
    }

    /**
     * Test that a lead will show up in the list of search results for leads, but not for other party types.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testLeadSearch() throws Exception {
        //  create a new lead
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("firstName", "Test");
        callCtxt.put("lastName", "searchlead");
        callCtxt.put("companyName", "searchleadcompany");
        Map<String, Object> results = runAndAssertServiceSuccess("crmsfa.createLead", callCtxt);
        String partyId = (String) results.get("partyId");

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        //  call the CRM search with the name of the lead
        CrmsfaSearchService crmSearch = crmsfaSearchParties("searchlead");

        // verify new lead shows up in the list of Lead
        List<Lead> leads = crmSearch.getLeads();
        Set<String> leadIds = Entity.getDistinctFieldValues(String.class, leads, Lead.Fields.partyId);
        assertTrue("Did not find the new lead [" + partyId + "]", leadIds.contains(partyId));

        // verify new contact does not shows up in the list of Account or Contact
        List<Account> accounts = crmSearch.getAccounts();
        Set<String> accountIds = Entity.getDistinctFieldValues(String.class, accounts, Account.Fields.partyId);
        List<Contact> contacts = crmSearch.getContacts();
        Set<String> contactIds = Entity.getDistinctFieldValues(String.class, contacts, Contact.Fields.partyId);
        assertFalse("Should not have found the new lead [" + partyId + "] in the Accounts results", accountIds.contains(partyId));
        assertFalse("Should not have found the new lead [" + partyId + "] in the Contact results", contactIds.contains(partyId));

        //  call the Purchasing search
        PurchasingSearchService purchasingSearch = purchasingSearchParties("searchlead");

        // verify account does not show up in the List of Suppliers
        List<PartyGroup> suppliers = purchasingSearch.getSuppliers();
        Set<String> supplierIds = Entity.getDistinctFieldValues(String.class, suppliers, PartyGroup.Fields.partyId);
        assertFalse("Should not have found the new lead [" + partyId + "] in the Supplier results", supplierIds.contains(partyId));
    }

    /**
     * Test that a supplier  will show up in the list of search results for suppliers, but not for other party types.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testSupplierSearch() throws Exception {
        //  create a new supplier
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("groupName", "searchsupplier");
        callCtxt.put("requires1099", "Y");
        Map<String, Object> results = runAndAssertServiceSuccess("purchasing.createSupplier", callCtxt);
        String partyId = (String) results.get("partyId");

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        //  call the CRM search with the name of the supplier
        CrmsfaSearchService crmSearch = crmsfaSearchParties("searchsupplier");

        // verify new supplier does not shows up in the list of Lead Account or Contact
        List<Account> accounts = crmSearch.getAccounts();
        Set<String> accountIds = Entity.getDistinctFieldValues(String.class, accounts, Account.Fields.partyId);
        List<Contact> contacts = crmSearch.getContacts();
        Set<String> contactIds = Entity.getDistinctFieldValues(String.class, contacts, Contact.Fields.partyId);
        List<Lead> leads = crmSearch.getLeads();
        Set<String> leadIds = Entity.getDistinctFieldValues(String.class, leads, Lead.Fields.partyId);
        assertFalse("Should not have found the new supplier [" + partyId + "] in the Account results", accountIds.contains(partyId));
        assertFalse("Should not have found the new supplier [" + partyId + "] in the Contacts results", contactIds.contains(partyId));
        assertFalse("Should not have found the new supplier [" + partyId + "] in the Leads results", leadIds.contains(partyId));

        //  call the Purchasing search
        PurchasingSearchService purchasingSearch = purchasingSearchParties("searchsupplier");

        // verify supplier shows up in the List of Suppliers
        List<PartyGroup> suppliers = purchasingSearch.getSuppliers();
        Set<String> supplierIds = Entity.getDistinctFieldValues(String.class, suppliers, PartyGroup.Fields.partyId);
        assertTrue("Should have found the new supplier [" + partyId + "] in the Supplier results", supplierIds.contains(partyId));
    }

    /**
     * Tests that a sales opportunity can be found by name or description, but once it is canceled, it will no longer be found.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testBasicSalesOpportunitySearch() throws Exception {
        //  create a sales opportunity with a name, description
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("opportunityName", "searchsalesopportunity");
        callCtxt.put("description", "a test search opportunity searchsalesopportunity1 searchsalesopportunity2");
        callCtxt.put("accountPartyId", "DemoCustCompany");
        callCtxt.put("estimatedCloseDate", dateStringToShortLocaleString("10/10/10"));
        Map<String, Object> results = runAndAssertServiceSuccess("crmsfa.createOpportunity", callCtxt);
        String salesOpportunityId = (String) results.get("salesOpportunityId");

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        //  call the CRM search with key words from the name of sales opportunity
        CrmsfaSearchService crmSearch = crmsfaSearchSalesOpportunities("searchsalesopportunity");
        List<SalesOpportunity> salesOpportunities = crmSearch.getSalesOpportunities();
        Set<String> salesOpportunityIds = Entity.getDistinctFieldValues(String.class, salesOpportunities, SalesOpportunity.Fields.salesOpportunityId);
        //  verify the sales opportunity shows up in the list of SalesOpportunity
        assertTrue("Should have found the new sales opportunity [" + salesOpportunityId + "] in the results", salesOpportunityIds.contains(salesOpportunityId));

        //  call the CRM search with key words from the description of sales opportunity
        crmSearch = crmsfaSearchSalesOpportunities("searchsalesopportunity2");
        salesOpportunities = crmSearch.getSalesOpportunities();
        salesOpportunityIds = Entity.getDistinctFieldValues(String.class, salesOpportunities, SalesOpportunity.Fields.salesOpportunityId);

        //  verify the sales opportunity shows up in the list of SalesOpportunity
        assertTrue("Should have found the new sales opportunity [" + salesOpportunityId + "] in the results", salesOpportunityIds.contains(salesOpportunityId));

        //  set the sales opportunity to canceled (lost)
        callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("salesOpportunityId", salesOpportunityId);
        callCtxt.put("opportunityName", "searchsalesopportunity");
        callCtxt.put("description", "a test search opportunity searchsalesopportunity1 searchsalesopportunity2");
        callCtxt.put("changeNote", "Cancel the Sales Opportunity");
        callCtxt.put("opportunityStageId", "SOSTG_LOST");
        callCtxt.put("estimatedCloseDate", dateStringToShortLocaleString("10/10/10"));
        runAndAssertServiceSuccess("crmsfa.updateOpportunity", callCtxt);

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        //  verify that the sales opportunity no longer shows up in the list of sales opportunities when searching by name or description
        crmSearch = crmsfaSearchSalesOpportunities("searchsalesopportunity");
        salesOpportunities = crmSearch.getSalesOpportunities();
        salesOpportunityIds = Entity.getDistinctFieldValues(String.class, salesOpportunities, SalesOpportunity.Fields.salesOpportunityId);
        assertFalse("Should no longer find the Canceled sales opportunity [" + salesOpportunityId + "] in the results", salesOpportunityIds.contains(salesOpportunityId));

        crmSearch = crmsfaSearchSalesOpportunities("searchsalesopportunity2");
        salesOpportunities = crmSearch.getSalesOpportunities();
        salesOpportunityIds = Entity.getDistinctFieldValues(String.class, salesOpportunities, SalesOpportunity.Fields.salesOpportunityId);
        assertFalse("Should no longer find the Canceled sales opportunity [" + salesOpportunityId + "] in the results", salesOpportunityIds.contains(salesOpportunityId));
    }

    /**
     * Tests that sales opportunities search results will rank results based on weights configured in <code>entitysearch.properties</code>.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testRankedSalesOpportunitySearch() throws Exception {
        //  create a sales opportunity #1 with a name, description
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("opportunityName", "testRankedSOSearch");
        callCtxt.put("accountPartyId", "DemoCustCompany");
        callCtxt.put("estimatedCloseDate", dateStringToShortLocaleString("10/10/10"));
        Map<String, Object> results = runAndAssertServiceSuccess("crmsfa.createOpportunity", callCtxt);
        String salesOpportunityId1 = (String) results.get("salesOpportunityId");

        //   create sales opportunity #2  with sales opportunity #1's salesOpportunityId  in its name
        callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", admin);
        callCtxt.put("opportunityName", salesOpportunityId1);
        callCtxt.put("accountPartyId", "DemoCustCompany");
        callCtxt.put("estimatedCloseDate", dateStringToShortLocaleString("10/10/10"));
        results = runAndAssertServiceSuccess("crmsfa.createOpportunity", callCtxt);
        String salesOpportunityId2 = (String) results.get("salesOpportunityId");

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        //  call CRM search with sales opportunity #1's ID as the keyword
        CrmsfaSearchService crmSearch = crmsfaSearchSalesOpportunities(salesOpportunityId1);

        //  verify that List<SalesOpportunity> has sales opportunity #1 first, then sales opportunity #2,  because Id has a higher search weight than name
        boolean found1 = false;
        boolean found2 = false;
        for (SalesOpportunity so : crmSearch.getSalesOpportunities()) {
            if (salesOpportunityId1.equals(so.getSalesOpportunityId())) {
                found1 = true;
                if (found2) {
                    fail("Found sales opportunity 1 after sales opportunity 2");
                }
            }
            if (salesOpportunityId2.equals(so.getSalesOpportunityId())) {
                found2 = true;
                if (!found1) {
                    fail("Found sales opportunity 2 before sales opportunity 1");
                }
            }
        }

        assertTrue("Did not find salesOpportunityId1 [" + salesOpportunityId1 + "]", found1);
        assertTrue("Did not find salesOpportunityId2 [" + salesOpportunityId2 + "]", found2);

    }

    protected CrmsfaSearchService crmsfaSearchParties(String keywords) throws Exception {
        CrmsfaSearchService crmSearch = new CrmsfaSearchService();
        crmSearch.setInfrastructure(new Infrastructure(dispatcher));
        crmSearch.setUser(new User(admin));
        crmSearch.loadDomainsDirectory();
        crmSearch.setKeywords(keywords);
        crmSearch.setSearchAccounts(true);
        crmSearch.setSearchContacts(true);
        crmSearch.setSearchLeads(true);
        crmSearch.search();
        return crmSearch;
    }

    protected CrmsfaSearchService crmsfaSearchSalesOpportunities(String keywords) throws Exception {
        CrmsfaSearchService crmSearch = new CrmsfaSearchService();
        crmSearch.setInfrastructure(new Infrastructure(dispatcher));
        crmSearch.setUser(new User(admin));
        crmSearch.loadDomainsDirectory();
        crmSearch.setKeywords(keywords);
        crmSearch.setSearchSalesOpportunities(true);
        crmSearch.search();
        return crmSearch;
    }

    protected PurchasingSearchService purchasingSearchParties(String keywords) throws Exception {
        PurchasingSearchService purchasingSearch = new PurchasingSearchService();
        purchasingSearch.setInfrastructure(new Infrastructure(dispatcher));
        purchasingSearch.setUser(new User(admin));
        purchasingSearch.loadDomainsDirectory();
        purchasingSearch.setKeywords(keywords);
        purchasingSearch.setSearchSuppliers(true);
        purchasingSearch.search();
        return purchasingSearch;
    }

}
