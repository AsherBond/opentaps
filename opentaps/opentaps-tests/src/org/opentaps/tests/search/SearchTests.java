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

package org.opentaps.tests.search;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.opentaps.base.constants.SalesOpportunityStageConstants;
import org.opentaps.base.entities.PartyGroup;
import org.opentaps.base.entities.Product;
import org.opentaps.base.entities.SalesOpportunity;
import org.opentaps.base.services.CrmsfaCreateAccountService;
import org.opentaps.base.services.CrmsfaCreateContactService;
import org.opentaps.base.services.CrmsfaCreateLeadService;
import org.opentaps.base.services.CrmsfaCreateOpportunityService;
import org.opentaps.base.services.CrmsfaDeactivateAccountService;
import org.opentaps.base.services.CrmsfaUpdateOpportunityService;
import org.opentaps.base.services.PurchasingCreateSupplierService;
import org.opentaps.common.order.PurchaseOrderFactory;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.crmsfa.search.CrmsfaSearchService;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Lead;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.purchasing.search.PurchasingSearchService;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Test case for the search services.
 */
public class SearchTests extends OpentapsTestCase {

    private static final String MODULE = SearchTests.class.getName();
    private static final long INDEX_PAUSE = 5000;
    private String defaultCrmSearchSecurity = null;
    private Infrastructure infrastructure = null;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        infrastructure = new Infrastructure(dispatcher);
        defaultCrmSearchSecurity = infrastructure.getConfigurationValue(org.opentaps.base.constants.OpentapsConfigurationTypeConstants.CRMSFA_FIND_SEC_FILTER);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown(); 
        infrastructure.setConfigurationValue(org.opentaps.base.constants.OpentapsConfigurationTypeConstants.CRMSFA_FIND_SEC_FILTER, defaultCrmSearchSecurity, "Restored to default by SearchTests");
    }

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
        PurchasingCreateSupplierService createSupplier = new PurchasingCreateSupplierService();
        createSupplier.setInUserLogin(admin);
        createSupplier.setInGroupName(partyGroupName);
        createSupplier.setInRequires1099("Y");
        runAndAssertServiceSuccess(createSupplier);
        String supplierPartyId = createSupplier.getOutPartyId();

        // create a account by service crmsfa.createAccount
        CrmsfaCreateAccountService createAccount = new CrmsfaCreateAccountService();
        createAccount.setInUserLogin(admin);
        createAccount.setInAccountName(partyGroupName);
        createAccount.setInForceComplete("Y");
        runAndAssertServiceSuccess(createAccount);

        // note: the indexing is async, so need to wait a little for the index to be in sync
        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        String keyWord = "Sangfroid";
        CrmsfaSearchService crmSearch = new CrmsfaSearchService();
        crmSearch.setInfrastructure(new Infrastructure(dispatcher));
        crmSearch.setUser(new User(admin));
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
        purchasingSearch.setKeywords(keyWord);
        purchasingSearch.setSearchSuppliers(true);
        purchasingSearch.search();
        List<PartyGroup> partyGroups = purchasingSearch.getSuppliers();
        assertNotEmpty("Should found some Suppliers by keyword " + keyWord, partyGroups);
        for (PartyGroup party : partyGroups) {
            // assert we find the supplier one
            assertIsSupplierNotCustomer(party);
        }

        // deactive the supplier we created in the test
        CrmsfaDeactivateAccountService deactivateAccount = new CrmsfaDeactivateAccountService();
        deactivateAccount.setInUserLogin(admin);
        deactivateAccount.setInPartyId(supplierPartyId);
        runAndAssertServiceSuccess(deactivateAccount);
    }

    /**
     * Test sales order search.
     * - create a Sales Order
     * - check the CrmsfaSearchService can find it
     * - check the PurchasingSearchService cannot find it
     * @throws Exception if an error occurs
     */
    public void testSalesOrderSearch() throws Exception {
        OrderRepositoryInterface repository = orderDomain.getOrderRepository();

        Product product = repository.findOneNotNull(Product.class, repository.map(Product.Fields.productId, "GZ-1005"));
        Party demoAccount1 = repository.findOneNotNull(Party.class, repository.map(Party.Fields.partyId, "DemoAccount1"));

        // create a test order
        Map<GenericValue, BigDecimal> orderProducts = new HashMap<GenericValue, BigDecimal>();
        orderProducts.put(Repository.genericValueFromEntity(product), new BigDecimal("1.0"));
        User = admin;
        SalesOrderFactory salesOrder = testCreatesSalesOrder(orderProducts, Repository.genericValueFromEntity(demoAccount1), "9000", "EXT_OFFLINE", "DemoAddress2");
        Debug.logInfo("testSalesOrderSearch created order [" + salesOrder.getOrderId() + "]", MODULE);
        // set the order name
        Order order = repository.getOrderById(salesOrder.getOrderId());
        assertNotNull("Could not find the created test order [" + salesOrder.getOrderId() + "].", order);
        order.setOrderName("searchsordername");
        repository.update(order);

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        // verify it shows in the search results by ID
        CrmsfaSearchService crmSearch = crmsfaSearchOrders(order.getOrderId());
        List<Order> orders = crmSearch.getSalesOrders();
        Set<String> orderIds = Entity.getDistinctFieldValues(String.class, orders, Order.Fields.orderId);
        assertTrue("Should have found the new order [" + order.getOrderId() + "] in the Sales Orders by ID results", orderIds.contains(order.getOrderId()));

        // verify it shows in the search results by Name
        crmSearch = crmsfaSearchOrders("searchsordername");
        orders = crmSearch.getSalesOrders();
        orderIds = Entity.getDistinctFieldValues(String.class, orders, Order.Fields.orderId);
        assertTrue("Should have found the new order [" + order.getOrderId() + "] in the Sales Orders by Name results", orderIds.contains(order.getOrderId()));

        // verify it does not show in the purchasing search results
        PurchasingSearchService purchasingSearch = purchasingSearchOrders(order.getOrderId());
        orders = purchasingSearch.getPurchaseOrders();
        orderIds = Entity.getDistinctFieldValues(String.class, orders, Order.Fields.orderId);
        assertFalse("Should not have found the new order [" + order.getOrderId() + "] in the Purchase Orders results", orderIds.contains(order.getOrderId()));
    }

    /**
     * Test purchase order search.
     * - create a Purchase Order
     * - check the PurchasingSearchService can find it
     * - check the CrmsfaSearchService cannot find it
     * @throws Exception if an error occurs
     */
    public void testPurchaseOrderSearch() throws Exception {
        OrderRepositoryInterface repository = orderDomain.getOrderRepository();

        Product product = repository.findOneNotNull(Product.class, repository.map(Product.Fields.productId, "GZ-1005"));
        Party supplier = repository.findOneNotNull(Party.class, repository.map(Party.Fields.partyId, "DemoSupplier"));

        // create a test order
        Map<GenericValue, BigDecimal> orderProducts = new HashMap<GenericValue, BigDecimal>();
        orderProducts.put(Repository.genericValueFromEntity(product), new BigDecimal("1.0"));
        User = admin;
        PurchaseOrderFactory pof = testCreatesPurchaseOrder(orderProducts, Repository.genericValueFromEntity(supplier), "9200");
        Debug.logInfo("testPurchaseOrderSearch created order [" + pof.getOrderId() + "]", MODULE);
        // set the order name
        Order order = repository.getOrderById(pof.getOrderId());
        assertNotNull("Could not find the created test order [" + pof.getOrderId() + "].", order);
        order.setOrderName("searchpordername");
        repository.update(order);

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        // verify it shows in the search results by ID
        PurchasingSearchService purchasingSearch = purchasingSearchOrders(order.getOrderId());
        List<Order> orders = purchasingSearch.getPurchaseOrders();
        Set<String> orderIds = Entity.getDistinctFieldValues(String.class, orders, Order.Fields.orderId);
        assertTrue("Should have found the new order [" + order.getOrderId() + "] in the Purchase Orders by ID results", orderIds.contains(order.getOrderId()));

        // verify it shows in the search results by Name
        purchasingSearch = purchasingSearchOrders("searchpordername");
        orders = purchasingSearch.getPurchaseOrders();
        orderIds = Entity.getDistinctFieldValues(String.class, orders, Order.Fields.orderId);
        assertTrue("Should have found the new order [" + order.getOrderId() + "] in the Purchase Orders by Name results", orderIds.contains(order.getOrderId()));

        // verify it does not show in the crmsfa search results
        CrmsfaSearchService crmSearch = crmsfaSearchOrders(order.getOrderId());
        orders = crmSearch.getSalesOrders();
        orderIds = Entity.getDistinctFieldValues(String.class, orders, Order.Fields.orderId);
        assertFalse("Should not have found the new order [" + order.getOrderId() + "] in the Sales Orders results", orderIds.contains(order.getOrderId()));
    }

    /**
     * Test that an account will show up in the list of search results for accounts, but not for other party types.
     * @throws Exception if an error occurs
     */
    public void testAccountSearch() throws Exception {
        //  create a new account
        CrmsfaCreateAccountService createAccount = new CrmsfaCreateAccountService();
        createAccount.setInUserLogin(admin);
        createAccount.setInAccountName("searchaccount");
        runAndAssertServiceSuccess(createAccount);
        String partyId = createAccount.getOutPartyId();

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
     * Test account can be found using full-text index by its name but not
     * by PartySupplementalData.companyName. last one should be used for leads
     * only. 
     */
    public void testAccountSupplementalDataNotSearched() throws Exception {
        //  create account with account name "Aaardvark Zambonis"
        CrmsfaCreateAccountService createAccount = new CrmsfaCreateAccountService();
        createAccount.setInUserLogin(admin);
        createAccount.setInAccountName("Aaardvark Zambonis");
        runAndAssertServiceSuccess(createAccount);
        String partyId = createAccount.getOutPartyId();

        // create a PartySupplementalData record for this account with company name "Pardvaronis"
        GenericValue supplData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
        supplData.set("companyName", "Pardvaronis");
        supplData.store();

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);
        
        // CRM search should return the account for "Aaardvark" and "Zambonis"  but not for "Pardvaronis"
        CrmsfaSearchService crmSearch = crmsfaSearchParties("Aaardvark Zambonis");
        List<Account> accounts = crmSearch.getAccounts();
        assertNotNull("Account isn't found using keywords \"Aaardvark Zambonis\"", accounts);
        assertNotEquals("Account not found using keyword \"Aaardvark Zambonis\"", 0, accounts.size());
        assertTrue("Wrong account is found", "Aaardvark Zambonis".equals(accounts.get(0).getGroupName()));

        crmSearch = crmsfaSearchParties("Pardvaronis");
        accounts = crmSearch.getAccounts();
        assertEquals("Some accounts are found using keyword \"Pardvaronis\" but should not be", 0, accounts.size());
    }

    /**
     * Test that a contact will show up in the list of search results for contacts, but not for other party types.
     * @throws Exception if an error occurs
     */
    public void testContactSearch() throws Exception {
        //  create a new contact
        CrmsfaCreateContactService createContact = new CrmsfaCreateContactService();
        createContact.setInUserLogin(admin);
        createContact.setInFirstName("Test");
        createContact.setInLastName("searchcontact");
        runAndAssertServiceSuccess(createContact);
        String partyId = createContact.getOutPartyId();

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
    public void testLeadSearch() throws Exception {
        //  create a new lead
        CrmsfaCreateLeadService createLead = new CrmsfaCreateLeadService();
        createLead.setInUserLogin(admin);
        createLead.setInFirstName("Test");
        createLead.setInLastName("searchlead");
        createLead.setInCompanyName("searchleadcompany");
        runAndAssertServiceSuccess(createLead);
        String partyId = createLead.getOutPartyId();

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
     * Test that a lead will show up in the list of search results for leads
     * when we search keywords from first/last name and company name that is
     * a field of <code>PartySumplementalData</code>.
     * @throws Exception 
     */
    public void testLeadSupplementalDataSearch() throws Exception {
        // create lead with first name Zaph, last name Bibrocks, company name Hitchhikers Publishing
        CrmsfaCreateLeadService createLead = new CrmsfaCreateLeadService();
        createLead.setInUserLogin(admin);
        createLead.setInFirstName("Zaph");
        createLead.setInLastName("Bibrocks");
        createLead.setInCompanyName("Hitchhikers Publishing");
        runAndAssertServiceSuccess(createLead);

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        // make sure that this lead shows up when searching for 
        // "zaph"
        CrmsfaSearchService crmSearch = crmsfaSearchParties("zaph");
        List<Lead> leads = crmSearch.getLeads();
        assertNotNull("Lead isn't found using keyword \"zaph\"", leads);
        assertNotEquals("Lead not found using keyword \"zaph\"", 0, crmSearch.getLeads().size());
        assertTrue("Wrong lead is found", "Zaph".equals(leads.get(0).getFirstName()));

        // "Bibrocks"
        crmSearch = crmsfaSearchParties("Bibrocks");
        leads = crmSearch.getLeads();
        assertNotNull("Lead isn't found using keyword \"Bibrocks\"", leads);
        assertNotEquals("Lead not found using keyword \"Bibrocks\"", 0, crmSearch.getLeads().size());
        assertTrue("Wrong lead is found", "Bibrocks".equals(leads.get(0).getLastName()));

        // "Hitchhikers" and "Publishing"
        crmSearch = crmsfaSearchParties("Hitchhikers Publishing");
        leads = crmSearch.getLeads();
        assertNotNull("Lead isn't found using keywords \"Hitchhikers Publishing\"", leads);
        assertNotEquals("Lead not found using keywords \"Hitchhikers Publishing\"", 0, crmSearch.getLeads().size());
        assertTrue("Wrong lead is found", "Hitchhikers Publishing".equals(leads.get(0).getCompanyName()));

    }

    /**
     * Test that a supplier  will show up in the list of search results for suppliers, but not for other party types.
     * @throws Exception if an error occurs
     */
    public void testSupplierSearch() throws Exception {
        //  create a new supplier
        PurchasingCreateSupplierService createSupplier = new PurchasingCreateSupplierService();
        createSupplier.setInUserLogin(admin);
        createSupplier.setInGroupName("searchsupplier");
        createSupplier.setInRequires1099("Y");
        runAndAssertServiceSuccess(createSupplier);
        String partyId = createSupplier.getOutPartyId();

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
    public void testBasicSalesOpportunitySearch() throws Exception {
        //  create a sales opportunity with a name, description
        CrmsfaCreateOpportunityService createOpportunity = new CrmsfaCreateOpportunityService();
        createOpportunity.setInUserLogin(admin);
        createOpportunity.setInOpportunityName("searchsalesopportunity");
        createOpportunity.setInDescription("a test search opportunity searchsalesopportunity1 searchsalesopportunity2");
        createOpportunity.setInAccountPartyId("DemoCustCompany");
        createOpportunity.setInOpportunityStageId(SalesOpportunityStageConstants.SOSTG_PROSPECT);
        createOpportunity.setInEstimatedCloseDate(dateStringToShortLocaleString("10/10/10"));
        runAndAssertServiceSuccess(createOpportunity);
        String salesOpportunityId = createOpportunity.getOutSalesOpportunityId();

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        OrderRepositoryInterface repository = orderDomain.getOrderRepository();
        SalesOpportunity so = repository.findOneNotNull(SalesOpportunity.class, repository.map(SalesOpportunity.Fields.salesOpportunityId, salesOpportunityId));
        Debug.logInfo("Create SalesOpportunity: " + so, MODULE);

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

        //  set the sales opportunity to proposal
        CrmsfaUpdateOpportunityService updateOpportunity = new CrmsfaUpdateOpportunityService();
        updateOpportunity.setInUserLogin(admin);
        updateOpportunity.setInSalesOpportunityId(salesOpportunityId);
        updateOpportunity.setInOpportunityName("searchsalesopportunity");
        updateOpportunity.setInDescription("a test search opportunity searchsalesopportunity1 searchsalesopportunity2");
        updateOpportunity.setInChangeNote("Make proposal the Sales Opportunity");
        updateOpportunity.setInOpportunityStageId(SalesOpportunityStageConstants.SOSTG_PROPOSAL);
        updateOpportunity.setInEstimatedCloseDate(dateStringToShortLocaleString("10/10/10"));
        runAndAssertServiceSuccess(updateOpportunity);

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        so = repository.findOneNotNull(SalesOpportunity.class, repository.map(SalesOpportunity.Fields.salesOpportunityId, salesOpportunityId));
        Debug.logInfo("Updated SalesOpportunity to: " + so, MODULE);

        //  call the CRM search with key words from the name of sales opportunity
        crmSearch = crmsfaSearchSalesOpportunities("searchsalesopportunity");
        salesOpportunities = crmSearch.getSalesOpportunities();
        salesOpportunityIds = Entity.getDistinctFieldValues(String.class, salesOpportunities, SalesOpportunity.Fields.salesOpportunityId);
        //  verify the sales opportunity shows up in the list of SalesOpportunity
        assertTrue("Should have found the new sales opportunity [" + salesOpportunityId + "] in the results", salesOpportunityIds.contains(salesOpportunityId));

        //  set the sales opportunity to canceled (lost)
        updateOpportunity = new CrmsfaUpdateOpportunityService();
        updateOpportunity.setInUserLogin(admin);
        updateOpportunity.setInSalesOpportunityId(salesOpportunityId);
        updateOpportunity.setInOpportunityName("searchsalesopportunity");
        updateOpportunity.setInDescription("a test search opportunity searchsalesopportunity1 searchsalesopportunity2");
        updateOpportunity.setInChangeNote("Cancel the Sales Opportunity");
        updateOpportunity.setInOpportunityStageId(SalesOpportunityStageConstants.SOSTG_LOST);
        updateOpportunity.setInEstimatedCloseDate(dateStringToShortLocaleString("10/10/10"));
        runAndAssertServiceSuccess(updateOpportunity);

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
    public void testRankedSalesOpportunitySearch() throws Exception {
        //  create a sales opportunity #1 with a name, description
        CrmsfaCreateOpportunityService createOpportunity = new CrmsfaCreateOpportunityService();
        createOpportunity.setInUserLogin(admin);
        createOpportunity.setInOpportunityName("testRankedSOSearch");
        createOpportunity.setInAccountPartyId("DemoCustCompany");
        createOpportunity.setInEstimatedCloseDate(dateStringToShortLocaleString("10/10/10"));
        createOpportunity.setInOpportunityStageId(SalesOpportunityStageConstants.SOSTG_PROSPECT);
        runAndAssertServiceSuccess(createOpportunity);
        String salesOpportunityId1 = createOpportunity.getOutSalesOpportunityId();

        //  create sales opportunity #2  with sales opportunity #1's salesOpportunityId  in its name
        createOpportunity = new CrmsfaCreateOpportunityService();
        createOpportunity.setInUserLogin(admin);
        createOpportunity.setInOpportunityName(salesOpportunityId1);
        createOpportunity.setInAccountPartyId("DemoCustCompany");
        createOpportunity.setInEstimatedCloseDate(dateStringToShortLocaleString("10/10/10"));
        createOpportunity.setInOpportunityStageId(SalesOpportunityStageConstants.SOSTG_PROSPECT);
        runAndAssertServiceSuccess(createOpportunity);
        String salesOpportunityId2 = createOpportunity.getOutSalesOpportunityId();

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
    
    /**
     * Test that if the find/security filter is on, only the original creator of the lead can find it through search
     * @throws Exception
     */
    public void testLeadSearchSecurity() throws Exception {

        // let's get 2 separate users
        User demoSalesRep1 = new User(delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1")));
        User demoSalesRep2 = new User(delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2")));
        
        // set search filter to secure
        infrastructure.setConfigurationValue(org.opentaps.base.constants.OpentapsConfigurationTypeConstants.CRMSFA_FIND_SEC_FILTER, "Y", "Temporarily set to secured by testLeadSearchSecurity");
        
        //  create a new lead as DemoSalesRep1
        CrmsfaCreateLeadService createLead = new CrmsfaCreateLeadService();
        createLead.setUser(demoSalesRep1);
        createLead.setInFirstName("Samantha");
        createLead.setInLastName("Secura");
        createLead.setInCompanyName("Lead Search Security International");
        runAndAssertServiceSuccess(createLead);
        String partyId = createLead.getOutPartyId();

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        //  call the CRM search with the name of the lead with DemoSalesRep1
        CrmsfaSearchService crmSearch = crmsfaSearchParties("Lead Search Security", demoSalesRep1);

        // verify new lead shows up in the list of Lead
        List<Lead> leads = crmSearch.getLeads();
        Set<String> leadIds = Entity.getDistinctFieldValues(String.class, leads, Lead.Fields.partyId);
        assertTrue("Did not find the new lead [" + partyId + "] when searching with [" + demoSalesRep1.getUserId() + "]", leadIds.contains(partyId));

        // now try with DemoSalesRep2
        crmSearch = crmsfaSearchParties("searchlead", demoSalesRep2);
        leads = crmSearch.getLeads();
        leadIds = Entity.getDistinctFieldValues(String.class, leads, Lead.Fields.partyId);
        assertFalse("Found the new lead [" + partyId + "] when searching with [" + demoSalesRep2.getUserId() + "]", leadIds.contains(partyId));
    }
    

    /**
     * Test that if the find/security filter is on, only the original creator of the contact can find it through search
     * @throws Exception
     */
    public void testContactSearchSecurity() throws Exception {

        // let's get 2 separate users
        User demoSalesRep1 = new User(delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1")));
        User demoSalesRep2 = new User(delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2")));
        
        // set search filter to secure
        infrastructure.setConfigurationValue(org.opentaps.base.constants.OpentapsConfigurationTypeConstants.CRMSFA_FIND_SEC_FILTER, "Y", "Temporarily set to secured by testContactSearchSecurity");
        
        //  create a new contact as DemoSalesRep1
        CrmsfaCreateContactService createContactService = new CrmsfaCreateContactService();
        createContactService.setUser(demoSalesRep1);
        createContactService.setInFirstName("Constantine");
        createContactService.setInLastName("Contactius");
        runAndAssertServiceSuccess(createContactService);
        String partyId = createContactService.getOutPartyId();

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        //  call the CRM search with the name of the contact with DemoSalesRep1
        CrmsfaSearchService crmSearch = crmsfaSearchParties("Contactius", demoSalesRep1);

        // verify new contact shows up in the list of contacts
        List<Contact> contacts = crmSearch.getContacts();
        Set<String> contactIds = Entity.getDistinctFieldValues(String.class, contacts, Contact.Fields.partyId);
        assertTrue("Did not find the new contact [" + partyId + "] when searching with [" + demoSalesRep1.getUserId() + "]", contactIds.contains(partyId));

        // now try with DemoSalesRep2
        crmSearch = crmsfaSearchParties("searchlead", demoSalesRep2);
        contacts = crmSearch.getContacts();
        contactIds = Entity.getDistinctFieldValues(String.class, contacts, Contact.Fields.partyId);
        assertFalse("Found the new contact [" + partyId + "] when searching with [" + demoSalesRep2.getUserId() + "]", contactIds.contains(partyId));
    }

    /**
     * Test that if the find/security filter is on, only the original creator of the account can find it through search
     * @throws Exception
     */
    public void testAccountSearchSecurity() throws Exception {

        // let's get 2 separate users
        User demoSalesRep1 = new User(delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1")));
        User demoSalesRep2 = new User(delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2")));
        
        // set search filter to secure
        infrastructure.setConfigurationValue(org.opentaps.base.constants.OpentapsConfigurationTypeConstants.CRMSFA_FIND_SEC_FILTER, "Y", "Temporarily set to secured by testAccountSearchSecurity");
        
        //  create a new account as DemoSalesRep1
        CrmsfaCreateAccountService createAccountService = new CrmsfaCreateAccountService();
        createAccountService.setUser(demoSalesRep1);
        createAccountService.setInAccountName("Account Search Security LLC");
        runAndAssertServiceSuccess(createAccountService);
        String partyId = createAccountService.getOutPartyId();

        pause("Pausing for the search index to be in sync.", INDEX_PAUSE);

        //  call the CRM search with the name of the account with DemoSalesRep1
        CrmsfaSearchService crmSearch = crmsfaSearchParties("Account Search Security", demoSalesRep1);

        // verify new contact shows up in the list of contacts
        List<Account> accounts = crmSearch.getAccounts();
        Set<String> accountIds = Entity.getDistinctFieldValues(String.class, accounts, Account.Fields.partyId);
        assertTrue("Did not find the new account [" + partyId + "] when searching with [" + demoSalesRep1.getUserId() + "]", accountIds.contains(partyId));

        // now try with DemoSalesRep2
        crmSearch = crmsfaSearchParties("searchlead", demoSalesRep2);
        accounts = crmSearch.getAccounts();
        accountIds = Entity.getDistinctFieldValues(String.class, accounts, Account.Fields.partyId);
        assertFalse("Found the new account [" + partyId + "] when searching with [" + demoSalesRep2.getUserId() + "]", accountIds.contains(partyId));
    }
    
    protected CrmsfaSearchService crmsfaSearchOrders(String keywords) throws Exception {
        CrmsfaSearchService crmSearch = new CrmsfaSearchService();
        crmSearch.setInfrastructure(infrastructure);
        crmSearch.setUser(new User(admin));
        crmSearch.setKeywords(keywords);
        crmSearch.setSearchSalesOrders(true);
        crmSearch.search();
        return crmSearch;
    }

    /**
     * By default search with admin user
     * @param keywords
     * @return
     * @throws Exception
     */
    protected CrmsfaSearchService crmsfaSearchParties(String keywords) throws Exception {
        return crmsfaSearchParties(keywords, new User(admin));
    }

    protected CrmsfaSearchService crmsfaSearchParties(String keywords, User user) throws Exception {
        CrmsfaSearchService crmSearch = new CrmsfaSearchService();
        crmSearch.setInfrastructure(infrastructure);
        crmSearch.setUser(user);
        crmSearch.setKeywords(keywords);
        crmSearch.setSearchAccounts(true);
        crmSearch.setSearchContacts(true);
        crmSearch.setSearchLeads(true);
        crmSearch.search();
        return crmSearch;
    }
    
    protected CrmsfaSearchService crmsfaSearchSalesOpportunities(String keywords) throws Exception {
        CrmsfaSearchService crmSearch = new CrmsfaSearchService();
        crmSearch.setInfrastructure(infrastructure);
        crmSearch.setUser(new User(admin));
        crmSearch.setKeywords(keywords);
        crmSearch.setSearchSalesOpportunities(true);
        crmSearch.search();
        return crmSearch;
    }

    protected PurchasingSearchService purchasingSearchParties(String keywords) throws Exception {
        PurchasingSearchService purchasingSearch = new PurchasingSearchService();
        purchasingSearch.setInfrastructure(infrastructure);
        purchasingSearch.setUser(new User(admin));
        purchasingSearch.setKeywords(keywords);
        purchasingSearch.setSearchSuppliers(true);
        purchasingSearch.search();
        return purchasingSearch;
    }

    protected PurchasingSearchService purchasingSearchOrders(String keywords) throws Exception {
        PurchasingSearchService purchasingSearch = new PurchasingSearchService();
        purchasingSearch.setInfrastructure(infrastructure);
        purchasingSearch.setUser(new User(admin));
        purchasingSearch.setKeywords(keywords);
        purchasingSearch.setSearchPurchaseOrders(true);
        purchasingSearch.search();
        return purchasingSearch;
    }

}
