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

package org.opentaps.tests.crmsfa.party;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import junit.framework.TestCase;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.PartyGroup;
import org.opentaps.base.entities.PartyRole;
import org.opentaps.base.entities.PartyRolePk;
import org.opentaps.base.entities.Person;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.entities.TelecomNumber;
import org.opentaps.base.services.CrmsfaCreateAccountService;
import org.opentaps.base.services.CrmsfaDeactivateAccountService;
import org.opentaps.base.services.CrmsfaUploadLeadsService;
import org.opentaps.base.services.PurchasingCreateSupplierService;
import org.opentaps.common.domain.party.PartyRepository;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.gwt.common.server.lookup.PartyLookupService;
import org.opentaps.tests.OpentapsTestCase;
import org.opentaps.tests.gwt.TestInputProvider;

/**
 * Party related tests.
 */
public class PartyTests extends OpentapsTestCase {

    private static final String MODULE = PartyTests.class.getName();

    private GenericValue admin;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        admin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
    }

    /**
     * Tests the <code>PartyHelper.getFirstValidCrmsfaPartyRoleTypeId</code> method.
     */
    public void testRoleTypeIds() {
        try {
            assertEquals("First valid role of DemoSalesManager is ACCOUNT_MANAGER", PartyHelper.getFirstValidCrmsfaPartyRoleTypeId("DemoSalesManager", delegator), "ACCOUNT_MANAGER");
            assertEquals("First valid role of DemoAccount1 is ACCOUNT", PartyHelper.getFirstValidCrmsfaPartyRoleTypeId("DemoAccount1", delegator), "ACCOUNT");
            assertEquals("First valid role of DemoAccount1 is CONTACT", PartyHelper.getFirstValidCrmsfaPartyRoleTypeId("DemoContact1", delegator), "CONTACT");
        } catch (GenericEntityException ex) {
            TestCase.fail("CRMSFA roles test failed: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>PartyHelper</code> methods to retrieve contact mechs.
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public void testContactMechMethods() throws GenericEntityException {
        List emailAddresses = UtilMisc.toList("customerservice@mycompany.com");
        // test retrieving primary email addresses
        assertEquals("Primary Email of DemoAccount1 is somebody@demoaccount1.com", org.opentaps.common.party.PartyHelper.getPrimaryEmailForParty("DemoAccount1", delegator), "somebody@demoaccount1.com");
        assertEquals("Primary Email of DemoContact1 is democontact1@demoaccount1.com", org.opentaps.common.party.PartyHelper.getPrimaryEmailForParty("DemoContact1", delegator), "democontact1@demoaccount1.com");

        // test retrieving specific contact mechs
        assertEquals("Received for Owner Email of DemoCSR is customerservice@mycompany.com", org.opentaps.common.party.PartyHelper.getEmailForPartyByPurpose("DemoCSR", "RECEIVE_EMAIL_OWNER", delegator), "customerservice@mycompany.com");

        GenericValue contactMech = (GenericValue) org.opentaps.common.party.PartyHelper.getCurrentContactMechsForParty("DemoCSR", "EMAIL_ADDRESS", "RECEIVE_EMAIL_OWNER", UtilMisc.toList(EntityCondition.makeCondition("infoString", EntityOperator.IN, emailAddresses)), delegator).get(0);
        assertEquals("DemoCSR has a RECEIVE_EMAIL_OWNER email and it is customerservice@mycompany.com", contactMech.getString("infoString"), "customerservice@mycompany.com");

        List<GenericValue> contactMechs = org.opentaps.common.party.PartyHelper.getCurrentContactMechsForParty("DemoCSR2", "EMAIL_ADDRESS", "RECEIVE_EMAIL_OWNER", UtilMisc.toList(EntityCondition.makeCondition("infoString", EntityOperator.IN, emailAddresses)), delegator);
        assertTrue("DemoCSR2 has no RECEIVE_EMAIL_OWNER email", UtilValidate.isEmpty(contactMechs));

    }

    /**
     * test primary contact Id fields
     * 1.  Create a new party
     * 2.  Create a PostalAddress with purpose GENERAL_LOCATION address
     * 3.  Verify that PartySupplementalData.primaryPostalAddress is the new PostalAddress.contactMechId
     * 4.  Call updatePartyPostalAddress to change the address
     * 5.  Verify that the PartySupplementalData.primaryPostalAddress is the new, updated PostalAddress.contactMechId
     * 7.  Add a new address with purpose = SHIPPING_DESTINATION
     * 9.  Verify PartySupplementalData.primaryPostalAddress is still the same as in (5)
     * 10.  Expire the PostalAddress's PartyContactMechPurpose = GENERAL_LOCATION
     * 11.  Verify that PartySupplementalData.primaryPostalAddress is null
     * 12.  Create a new PostalAddress with purpose MAIN_HOME_ADDRESS
     * 13.  Verify that PartySupplementalData.primaryPostalAddress is still null
     * 14.  Set address from (12) as GENERAL_LOCATION address
     * 15.  Verify it is PartySupplementalData.primaryPostalAddress is now the contactMechId from (12)
     * 16.  Expire this address for the party
     * 17.  Verify PartySupplementalData.primaryPostalAddress is now null
     *
     * these steps need to be repeated for the telecom number and email contact mech IDs
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPrimaryContactIDFields() throws GeneralException {
        // 1.  Create a new party
        Map input = UtilMisc.toMap("userLogin", admin);
        input.put("firstName", "testPrimaryContactIDFields");
        input.put("lastName", "testPrimaryContactIDFields");
        Map output = runAndAssertServiceSuccess("crmsfa.createContact", input);
        String partyId = (String) output.get("partyId");

        // 2.  Create a PostalAddress with purpose GENERAL_LOCATION address
        input = UtilMisc.toMap("userLogin", admin);
        input.put("partyId", partyId);
        input.put("contactMechPurposeTypeId", "GENERAL_LOCATION");
        input.put("toName", "generalToName");
        input.put("address1", "generalAddress1");
        input.put("address2", "generalAddress2");
        input.put("city", "generalCity");
        input.put("stateProvinceGeoId", "NY");
        input.put("postalCode", "00000");
        input.put("countryGeoId", "USA");
        output = runAndAssertServiceSuccess("createPartyPostalAddress", input);
        String contactMechId = (String) output.get("contactMechId");

        // 3.  Verify that PartySupplementalData.primaryPostalAddress is the new PostalAddress.contactMechId
        GenericValue partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
        assertNotNull("partySupplementalData should not be null", partySupplementalData);
        assertEquals("partySupplementalData.primaryPostalAddress for customer [" + partyId + "] is not the new GENERAL_LOCATION address", contactMechId, partySupplementalData.getString("primaryPostalAddressId"));

        // 4.  Call updatePartyPostalAddress to change the address
        input = UtilMisc.toMap("userLogin", admin);
        input.put("partyId", partyId);
        input.put("contactMechId", contactMechId);
        input.put("toName", "generalToNameUp");
        input.put("address1", "generalAddress1Up");
        input.put("address2", "generalAddress2Up");
        input.put("city", "generalCityUp");
        input.put("stateProvinceGeoId", "NY");
        input.put("postalCode", "00000");
        input.put("countryGeoId", "USA");
        output = runAndAssertServiceSuccess("updatePartyPostalAddress", input);
        contactMechId = (String) output.get("contactMechId");

        // 5.  Verify that the PartySupplementalData.primaryPostalAddress is the new, updated PostalAddress.contactMechId
        partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
        assertNotNull("partySupplementalData should not be null", partySupplementalData);
        assertEquals("partySupplementalData.primaryPostalAddress for customer [" + partyId + "] is not the updated GENERAL_LOCATION address", contactMechId, partySupplementalData.getString("primaryPostalAddressId"));

        // 7.  Add a new address with purpose = SHIPPING_DESTINATION
        input = UtilMisc.toMap("userLogin", admin);
        input.put("partyId", partyId);
        input.put("contactMechPurposeTypeId", "SHIPPING_LOCATION");
        input.put("toName", "generalToNameShip");
        input.put("address1", "generalAddress1Ship");
        input.put("address2", "generalAddress2Ship");
        input.put("city", "generalCityShip");
        input.put("stateProvinceGeoId", "NY");
        input.put("postalCode", "00000");
        input.put("countryGeoId", "USA");
        output = runAndAssertServiceSuccess("createPartyPostalAddress", input);

        // 9.  Verify PartySupplementalData.primaryPostalAddress is still the same as in (5)
        partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
        assertNotNull("partySupplementalData should not be null", partySupplementalData);
        assertEquals("partySupplementalData.primaryPostalAddress for customer [" + partyId + "] is not the last GENERAL_LOCATION address", contactMechId, partySupplementalData.getString("primaryPostalAddressId"));

        // 10.  Expire the PostalAddress's PartyContactMechPurpose = GENERAL_LOCATION
        input = UtilMisc.toMap("userLogin", admin);
        input.put("partyId", partyId);
        input.put("contactMechId", contactMechId);
        output = runAndAssertServiceSuccess("deletePartyContactMech", input);

        // 11.  Verify that PartySupplementalData.primaryPostalAddress is null
        partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
        assertNotNull("partySupplementalData should not be null", partySupplementalData);
        assertNull("There should be no more partySupplementalData.primaryPostalAddress for customer [" + partyId + "]", partySupplementalData.getString("primaryPostalAddressId"));

        // 12.  Create a new PostalAddress with purpose MAIN_HOME_ADDRESS
        input = UtilMisc.toMap("userLogin", admin);
        input.put("partyId", partyId);
        input.put("contactMechPurposeTypeId", "PRIMARY_LOCATION");
        input.put("toName", "generalToNameMainHome");
        input.put("address1", "generalAddress1MainHome");
        input.put("address2", "generalAddress2MainHome");
        input.put("city", "generalCityMainHome");
        input.put("stateProvinceGeoId", "NY");
        input.put("postalCode", "00000");
        input.put("countryGeoId", "USA");
        output = runAndAssertServiceSuccess("createPartyPostalAddress", input);
        contactMechId = (String) output.get("contactMechId");

        // 13.  Verify that PartySupplementalData.primaryPostalAddress is still null
        partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
        assertNotNull("partySupplementalData should not be null", partySupplementalData);
        assertNull("There should be no more partySupplementalData.primaryPostalAddress for customer [" + partyId + "]", partySupplementalData.getString("primaryPostalAddressId"));

        // 14.  Set address from (12) as GENERAL_LOCATION address
        input = UtilMisc.toMap("userLogin", admin);
        input.put("partyId", partyId);
        input.put("contactMechId", contactMechId);
        input.put("contactMechPurposeTypeId", "GENERAL_LOCATION");
        output = dispatcher.runSync("createPartyContactMechPurpose", input);

        // 15.  Verify it is PartySupplementalData.primaryPostalAddress is now the contactMechId from (12)
        partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
        assertNotNull("partySupplementalData should not be null", partySupplementalData);
        assertEquals("partySupplementalData.primaryPostalAddress for customer [" + partyId + "] is not the last GENERAL_LOCATION address", contactMechId, partySupplementalData.getString("primaryPostalAddressId"));

        // 16.  Expire this address for the party
        input = UtilMisc.toMap("userLogin", admin);
        input.put("partyId", partyId);
        input.put("contactMechId", contactMechId);
        output = runAndAssertServiceSuccess("deletePartyContactMech", input);

        // 17.  Verify PartySupplementalData.primaryPostalAddress is now null
        partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
        assertNotNull("partySupplementalData should not be null", partySupplementalData);
        assertNull("There should be no more partySupplementalData.primaryPostalAddress for customer [" + partyId + "]", partySupplementalData.getString("primaryPostalAddressId"));

    }

    /**
     * Tests getting <code>ContactMech</code> from the <code>Party</code> domain object.
     * @exception Exception if an error occurs
     */
    public void testContactMechPartyDomainMethods() throws Exception {

        PartyRepository repository = new PartyRepository(delegator);
        Party demoAccount1 = repository.getPartyById("DemoAccount1");
        Party demoContact1 = repository.getPartyById("DemoContact1");

        // Verify name
        assertEquals("Name of DemoAccount1 is Demo Sales Account No. 1", demoAccount1.getName(), "Demo Sales Account No. 1");
        assertEquals("Name of DemoContact1 is Demo First Contact", demoContact1.getName(), "Demo First Contact");

        // verify Primary Address
        PostalAddress pa = demoAccount1.getPrimaryAddress();
        assertNotNull("Primary Address Id of DemoAccount1 should not be null", pa);
        assertEquals("Primary Address Id of DemoAccount1 is DemoAddress1", pa.getContactMechId(), "DemoAddress1");

        // verify Primary email
        assertEquals("Primary Email of DemoAccount1 is somebody@demoaccount1.com", demoAccount1.getPrimaryEmail(), "somebody@demoaccount1.com");
        assertEquals("Primary Email of DemoContact1 is democontact1@demoaccount1.com", demoContact1.getPrimaryEmail(), "democontact1@demoaccount1.com");

        // verify Primary phone
        TelecomNumber account1Phone = demoAccount1.getPrimaryPhone();
        TelecomNumber contact1Phone = demoContact1.getPrimaryPhone();
        String accountPhone = account1Phone.getCountryCode() + account1Phone.getAreaCode() + account1Phone.getContactNumber();
        String contactPhone = contact1Phone.getCountryCode() + contact1Phone.getAreaCode() + contact1Phone.getContactNumber();
        assertEquals("Primary phone of DemoAccount1 is 1 (310) 472-1234", accountPhone, "1310472-1234");
        assertEquals("Primary phone of DemoContact1 is 1 (310) 472-1234", contactPhone, "1310472-1234");

    }

    /**
     * Tests Account & Contact domain objects and their relationship.
     * @throws Exception if an error occurs
     */
    public void testAccountContactRelationship() throws Exception {
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepository repo = (PartyRepository) partyDomain.getPartyRepository();

        // Account DemoAccount1 has only contact DemoContact1
        Account acct = repo.getAccountById("DemoAccount1");
        Set<Contact> accountContacts = acct.getContacts();
        assertEquals(1, accountContacts.size());
        for (Contact element : accountContacts) {
            assertEquals("DemoContact1", element.getPartyId());
            break;
        }

        // Contact DemoContact1 is associated with only account DemoAccount1
        Contact contact = repo.getContactById("DemoContact1");
        Set<Account> contactAccounts = contact.getAccounts();
        assertEquals(1, contactAccounts.size());
        for (Account element : contactAccounts) {
            assertEquals("DemoAccount1", element.getPartyId());
            break;
        }

    }

    /**
     * Test Lookup Parties by Primary Phone.
     * @throws Exception if an error occurs
     */
    public void testGetPartyByPhoneNumberForPrimaryPhone() throws Exception {
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepositoryInterface repo = partyDomain.getPartyRepository();
        // "82584405" is primary phone of DemoContact5
        Set<Party> parties = repo.getPartyByPhoneNumber("82584405");
        assertEquals("Correct number of parties found", 1, parties.size());
        for (Party element : parties) {
            assertEquals("DemoContact5", element.getPartyId());
        }

    }

    /**
     * Test Lookup Parties by not existing Phone Number.
     * @throws Exception if an error occurs
     */
    public void testGetPartyByPhoneNumberForNonExistingNumber() throws Exception {
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepositoryInterface repo = partyDomain.getPartyRepository();
        // try to look up a phone number which is not in the system, it will return empty collection
        Set<Party> parties = repo.getPartyByPhoneNumber("44441234");
        assertEquals("Correct number of parties found", 0, parties.size());
    }

    /**
     * Test Lookup Parties by Home Phone.
     * @throws Exception if an error occurs
     */
    public void testGetPartyByPhoneNumberForHomePhone() throws Exception {
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepositoryInterface repo = partyDomain.getPartyRepository();

        // "82584406" is home phone of DemoContact5
        Set<Party>  parties = repo.getPartyByPhoneNumber("82584406");
        assertEquals("Correct number of parties found", 1, parties.size());
        for (Party element : parties) {
            assertEquals("DemoContact5", element.getPartyId());
        }
    }

    /**
     * Test Lookup Parties by Mobile Phone.
     * @throws Exception if an error occurs
     */
    public void testGetPartyByPhoneNumberForMobilePhone() throws Exception {
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepositoryInterface repo = partyDomain.getPartyRepository();

        // "13512340007" is mobile number of DemoContact5
        Set<Party> parties = repo.getPartyByPhoneNumber("13512340007");
        assertEquals("Correct number of parties found", 1, parties.size());
        for (Party element : parties) {
            assertEquals("DemoContact5", element.getPartyId());
        }
    }

    /**
     * Test Lookup Parties by Mobile Phone.
     * @throws Exception if an error occurs
     */
    public void testGetPartyByPhoneNumberForWorkPhone() throws Exception {
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepositoryInterface repo = partyDomain.getPartyRepository();

        // "82584408" is work phone of DemoContact5
        Set<Party> parties = repo.getPartyByPhoneNumber("82584408");
        assertEquals("Correct number of parties found", 1, parties.size());
        for (Party element : parties) {
            assertEquals("DemoContact5", element.getPartyId());
        }
    }

    /**
     * Tests getPartyByPhoneNumber for lookup an expired number.
     * @throws Exception if an error occurs
     */
    public void testGetPartyByPhoneNumberForExpiredPhone() throws Exception {
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepositoryInterface repo = partyDomain.getPartyRepository();
        // "82584409" is the second work phone of DemoContact5 , but it's expired
        Set<Party> parties = repo.getPartyByPhoneNumber("82584409");
        assertEquals("Correct number of parties found", 0, parties.size());
    }


    /**
     * Tests testGetSubAccounts for lookup sub Account.
     * @throws Exception if an error occurs
     */
    public void testGetSubAccounts() throws Exception {
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepositoryInterface repository = partyDomain.getPartyRepository();
        Account demoAccount1 = repository.getAccountById("DemoAccount1");
        assertNotNull("Could not find DemoAccount1", demoAccount1);
        Set<Account> subAccounts = repository.getSubAccounts(demoAccount1);
        assertTrue("[DemoAccount1Sub] is not in the list of DemoAccount1's getSubAccounts", Entity.getDistinctFieldValues(subAccounts, Account.Fields.partyId).contains("DemoAccount1Sub"));
    }

    /**
     * Test the GWT contact lookup.
     * @throws Exception if an error occurs
     */
    public void testGwtContactLookup() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);

        // 1. test that we can find the demo contact DemoCustomer by ID in a case insensitive way
        provider.setParameter(PartyLookupConfiguration.INOUT_PARTY_ID, "democustomer");
        PartyLookupService lookup = new PartyLookupService(provider);
        lookup.findContacts();
        assertTrue("There should be at least one record found (DemoCustomer).", lookup.getResultTotalCount() > 0);

        // test that all found records have their ID starting with "democustomer", and that DemoCustomer is in the list
        boolean found = false;
        for (EntityInterface record : lookup.getResults()) {
            assertTrue("Returned record should have an ID that start with 'democustomer'.", record.getString(PartyLookupConfiguration.INOUT_PARTY_ID).toLowerCase().startsWith("democustomer"));
            if ("DemoCustomer".equals(record.getString(PartyLookupConfiguration.INOUT_PARTY_ID))) {
                if (found) {
                    fail("DemoCustomer was returned twice.");
                }
                found = true;
            }
        }
        assertTrue("DemoCustomer was not found.", found);

        // 2. test search by name
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_FIRST_NAME, "eMo");
        provider.setParameter(PartyLookupConfiguration.INOUT_LAST_NAME, "TACT");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findContacts();

        // test we found the demo data: DemoContact1, DemoContact2, DemoContact5
        assertGwtLookupFound(lookup, Arrays.asList("DemoContact1", "DemoContact2", "DemoContact5"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoCustomer", "DemoPrivilegedCust"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 3. test search by phone
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE, "555");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findContacts();

        // test we found the demo data: dontship2me
        assertGwtLookupFound(lookup, Arrays.asList("dontship2me"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoCustomer", "DemoPrivilegedCust", "DemoContact1"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 4. test search by classification
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.IN_CLASSIFICATION, "PRIVILEGED_CUSTOMERS");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findContacts();

        // test we found the demo data: DemoPrivilegedCust
        assertGwtLookupFound(lookup, Arrays.asList("DemoPrivilegedCust"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoCustomer", "dontship2me", "DemoContact1"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 5. test search by city
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_CITY, "new");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findContacts();

        // test we found the demo data: DemoPrivilegedCust, dontship2me, and not ca1/2 (vallejo), DemoCustomer (no primary city)
        assertGwtLookupFound(lookup, Arrays.asList("DemoPrivilegedCust", "dontship2me"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("ca1", "ca2", "DemoCustomer"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 6. test search by address
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_ADDRESS, "5");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findContacts();

        // test we found the demo data: DemoPrivilegedCust, dontship2me, ca1, ca2,  and not DemoCustomer (no primary address)
        assertGwtLookupFound(lookup, Arrays.asList("DemoPrivilegedCust", "dontship2me", "ca1", "ca2"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoCustomer"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 7. test more specific search, address + state
        provider.setParameter(PartyLookupConfiguration.INOUT_STATE, "CA");
        lookup = new PartyLookupService(provider);
        lookup.findContacts();

        // test we found the demo data: ca1, ca2, not dontship2me DemoPrivilegedCust (state NY)
        assertGwtLookupFound(lookup, Arrays.asList("ca1", "ca2"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("dontship2me", "DemoPrivilegedCust"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 8. test no pager parameter
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_NO_PAGER, "Y");
        lookup = new PartyLookupService(provider);
        lookup.findContacts();

        // should return more than 10 (default page size) results
        assertTrue("There should be at least 11 records returned with no pager.", lookup.getResults().size() > UtilLookup.DEFAULT_LIST_PAGE_SIZE);

        // 9. test smaller page size
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "2");
        lookup = new PartyLookupService(provider);
        lookup.findContacts();

        // should return only 2 (page size) results
        assertTrue("There should be only 2 records returned.", lookup.getResults().size() == 2);
        // but the total number of records should still be more than 10 (there are 11 demo contacts)
        assertTrue("There should be at least 11 records found in total.", lookup.getResultTotalCount() > UtilLookup.DEFAULT_LIST_PAGE_SIZE);

        // 10. test sorting
        provider = new TestInputProvider(admin, dispatcher);
        // note: add a search parameter 'e' to avoid returning all the contacts created during the other tests and which have empty names
        provider.setParameter(PartyLookupConfiguration.INOUT_LAST_NAME, "e");
        provider.setParameter(UtilLookup.PARAM_SORT_FIELD, PartyLookupConfiguration.INOUT_LAST_NAME);
        provider.setParameter(UtilLookup.PARAM_SORT_DIRECTION, "DESC");
        lookup = new PartyLookupService(provider);
        lookup.findContacts();

        // check returned records are sorted correctly
        List<GenericValue> expected = delegator.findByCondition("PartyFromByRelnAndContactInfoAndPartyClassification", EntityCondition.makeCondition(EntityOperator.AND,
                                        EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(PartyLookupConfiguration.INOUT_LAST_NAME), EntityOperator.LIKE, EntityFunction.UPPER("%e%")),
                                        EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "CONTACT")),
                                                                null,
                                                                UtilMisc.toList(PartyLookupConfiguration.INOUT_PARTY_ID, PartyLookupConfiguration.INOUT_LAST_NAME),
                                                                UtilMisc.toList(PartyLookupConfiguration.INOUT_LAST_NAME + " DESC"), Repository.DISTINCT_FIND_OPTIONS);
        expected = expected.subList(0, (UtilLookup.DEFAULT_LIST_PAGE_SIZE > expected.size() ? expected.size() : UtilLookup.DEFAULT_LIST_PAGE_SIZE));
        assertGwtLookupSort(lookup, PartyLookupConfiguration.INOUT_LAST_NAME, expected);
    }

    /**
     * Test the GWT contact auto completer.
     * @throws Exception if an error occurs
     */
    public void testGwtContactSuggest() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);

        // 1. test that no query returns the 10 first contacts (because it is paginated)
        provider.setParameter(UtilLookup.PARAM_SORT_FIELD, PartyLookupConfiguration.INOUT_PARTY_ID);
        PartyLookupService lookup = new PartyLookupService(provider);
        lookup.suggestContacts();
        assertEquals("Should have found (default page size) results.", UtilLookup.DEFAULT_LIST_PAGE_SIZE, lookup.getResults().size());

        // 2. test we can find ca1 by name (Junipero Serra)
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "juniper");
        lookup = new PartyLookupService(provider);
        lookup.suggestContacts();
        assertGwtSuggestFound(lookup, Arrays.asList("ca1"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 3. test we can find ca1 by name (Junipero Serra)
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "serr");
        lookup = new PartyLookupService(provider);
        lookup.suggestContacts();
        assertGwtSuggestFound(lookup, Arrays.asList("ca1"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 4. test we can find ca1 by name (Junipero Serra)
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "junipero ser");
        lookup = new PartyLookupService(provider);
        lookup.suggestContacts();
        assertGwtSuggestFound(lookup, Arrays.asList("ca1"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 5. test we can find by id
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "(ca");
        lookup = new PartyLookupService(provider);
        lookup.suggestContacts();
        assertGwtSuggestFound(lookup, Arrays.asList("ca1", "ca2"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 6. test we can find by name + id
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "junipero serra (ca1)");
        lookup = new PartyLookupService(provider);
        lookup.suggestContacts();
        assertGwtSuggestFound(lookup, Arrays.asList("ca1"), PartyLookupConfiguration.INOUT_PARTY_ID);
    }

    /**
     * Test the GWT account lookup.
     * @throws Exception if an error occurs
     */
    public void testGwtAccountLookup() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);

        // 1. test that we can find the demo account DemoCustCompany by ID in a case insensitive way
        provider.setParameter(PartyLookupConfiguration.INOUT_PARTY_ID, "democustcompany");
        PartyLookupService lookup = new PartyLookupService(provider);
        lookup.findAccounts();
        assertTrue("There should be at least one record found (DemoCustCompany).", lookup.getResultTotalCount() > 0);

        // test that all found records have their ID starting with "democustcompany", and that DemoCustCompany is in the list
        boolean found = false;
        for (EntityInterface record : lookup.getResults()) {
            assertTrue("Returned record should have an ID that start with 'democustcompany'.", record.getString(PartyLookupConfiguration.INOUT_PARTY_ID).toLowerCase().startsWith("democustcompany"));
            if ("DemoCustCompany".equals(record.getString(PartyLookupConfiguration.INOUT_PARTY_ID))) {
                if (found) {
                    fail("DemoCustCompany was returned twice.");
                }
                found = true;
            }
        }
        assertTrue("DemoCustCompany was not found.", found);

        // 2. test search by name
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_GROUP_NAME, "saLes");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findAccounts();

        // test we found the demo data: DemoAccount1, DemoAccount1Sub
        assertGwtLookupFound(lookup, Arrays.asList("DemoAccount1", "DemoAccount1Sub"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("democlass1", "democlass2", "democlass3", "accountlimit100"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 3. test search by phone
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE, "1");
        provider.setParameter(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE, "31");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findAccounts();

        // test we found the demo data: DemoAccount1, and not accountlimit100 (area 555)
        assertGwtLookupFound(lookup, Arrays.asList("DemoAccount1"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("accountlimit100"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 4. test search by classification
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.IN_CLASSIFICATION, "NET_DUE_10TH");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findAccounts();

        // test we found the demo data: democlass1
        assertGwtLookupFound(lookup, Arrays.asList("democlass1"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoAccount1", "democlass2", "democlass3", "accountlimit100"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 5. test search by city
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_CITY, "AngEl");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findAccounts();

        // test we found the demo data: DemoAccount1, democlass1, democlass2, democlass3, and not accountlimit100 (city new york)
        assertGwtLookupFound(lookup, Arrays.asList("DemoAccount1", "democlass1", "democlass2", "democlass3"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("accountlimit100"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 6. test search by postal code
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_POSTAL_CODE, "900");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findAccounts();

        // test we found the demo data: DemoAccount1, democlass1, democlass2, democlass3, demoneton10th
        assertGwtLookupFound(lookup, Arrays.asList("DemoAccount1", "democlass1", "democlass2", "democlass3", "demoneton10th"), PartyLookupConfiguration.INOUT_PARTY_ID);
        // test accountlimit100 is not found, its postal code being 10018
        assertGwtLookupNotFound(lookup, Arrays.asList("accountlimit100"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 7. test more specific search, postal code + address
        provider.setParameter(PartyLookupConfiguration.INOUT_ADDRESS, "blv");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findAccounts();

        // test we found the demo data: DemoAccount1, not democlass1/2/3, accountlimit100 (address in LA avenue)
        assertGwtLookupFound(lookup, Arrays.asList("DemoAccount1"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("democlass1", "democlass2", "democlass3", "accountlimit100"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 8. test no pager parameter
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_NO_PAGER, "Y");
        lookup = new PartyLookupService(provider);
        lookup.findAccounts();

        // should return more than 10 (default page size) results
        assertTrue("There should be at least 11 records returned with no pager.", lookup.getResults().size() > UtilLookup.DEFAULT_LIST_PAGE_SIZE);

        // 9. test smaller page size
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "2");
        lookup = new PartyLookupService(provider);
        lookup.findAccounts();

        // should return only 2 (page size) results
        assertTrue("There should be only 2 records returned.", lookup.getResults().size() == 2);
        // but the total number of records should still be more than 10 (there are 11 demo contacts)
        assertTrue("There should be at least 11 records found in total.", lookup.getResultTotalCount() > UtilLookup.DEFAULT_LIST_PAGE_SIZE);

        // 10. test sorting
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_SORT_FIELD, PartyLookupConfiguration.INOUT_GROUP_NAME);
        provider.setParameter(UtilLookup.PARAM_SORT_DIRECTION, "ASC");
        lookup = new PartyLookupService(provider);
        lookup.findAccounts();

        // check returned records are sorted correctly
        List<GenericValue> expected = delegator.findByCondition("PartyFromByRelnAndContactInfoAndPartyClassification", EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT"), null, UtilMisc.toList(PartyLookupConfiguration.INOUT_PARTY_ID, PartyLookupConfiguration.INOUT_GROUP_NAME), UtilMisc.toList(PartyLookupConfiguration.INOUT_GROUP_NAME + " ASC"), Repository.DISTINCT_FIND_OPTIONS);
        expected = expected.subList(0, (UtilLookup.DEFAULT_LIST_PAGE_SIZE > expected.size() ? expected.size() : UtilLookup.DEFAULT_LIST_PAGE_SIZE));
        assertGwtLookupSort(lookup, PartyLookupConfiguration.INOUT_GROUP_NAME, expected);
    }

    /**
     * Test the GWT account auto completer.
     * @throws Exception if an error occurs
     */
    public void testGwtAccountSuggest() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);

        // 1. test that no query returns the 10 first contacts (because it is paginated)
        provider.setParameter(UtilLookup.PARAM_SORT_FIELD, PartyLookupConfiguration.INOUT_PARTY_ID);
        PartyLookupService lookup = new PartyLookupService(provider);
        lookup.suggestAccounts();
        assertEquals("Should have found (default page size) results.", UtilLookup.DEFAULT_LIST_PAGE_SIZE, lookup.getResults().size());

        // 2. test we can find DemoCustCompany by name (Demo Customer Company)
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "demo");
        lookup = new PartyLookupService(provider);
        lookup.suggestAccounts();
        assertGwtSuggestFound(lookup, Arrays.asList("DemoCustCompany"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 3. test we can find DemoCustCompany by name (Demo Customer Company)
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "demo custo");
        lookup = new PartyLookupService(provider);
        lookup.suggestAccounts();
        assertGwtSuggestFound(lookup, Arrays.asList("DemoCustCompany"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 4. test we can find DemoCustCompany by name (Demo Customer Company)
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "stomer compa");
        lookup = new PartyLookupService(provider);
        lookup.suggestAccounts();
        assertGwtSuggestFound(lookup, Arrays.asList("DemoCustCompany"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 5. test we can find by id
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "(DemoCus");
        lookup = new PartyLookupService(provider);
        lookup.suggestAccounts();
        assertGwtSuggestFound(lookup, Arrays.asList("DemoCustCompany"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 6. test we can find by name + id
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "Demo customer companY (DemoCustCompany)");
        lookup = new PartyLookupService(provider);
        lookup.suggestAccounts();
        assertGwtSuggestFound(lookup, Arrays.asList("DemoCustCompany"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 7. test another find by id that returns more than one
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "democlass");
        lookup = new PartyLookupService(provider);
        lookup.suggestAccounts();
        assertGwtSuggestFound(lookup, Arrays.asList("democlass1", "democlass2", "democlass3"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 8. same but by name
        provider.setParameter(UtilLookup.PARAM_SUGGEST_QUERY, "classifica");
        lookup = new PartyLookupService(provider);
        lookup.suggestAccounts();
        assertGwtSuggestFound(lookup, Arrays.asList("democlass1", "democlass2", "democlass3"), PartyLookupConfiguration.INOUT_PARTY_ID);
    }

    /**
     * Test the GWT partner lookup.
     * @throws Exception if an error occurs
     */
    public void testGwtPartnerLookup() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);
        String testPartner = "demopartner1";
        // 1. test that we can find the testPartner by ID in a case insensitive way
        provider.setParameter(PartyLookupConfiguration.INOUT_PARTY_ID, testPartner.toUpperCase());
        PartyLookupService lookup = new PartyLookupService(provider);
        lookup.findPartners();
        assertTrue("There should be at least one record found (" + testPartner + ").", lookup.getResultTotalCount() > 0);

        // test that all found records have their ID starting with ID of testPartner, and that testPartner is in the list
        boolean found = false;
        for (EntityInterface record : lookup.getResults()) {
            assertTrue("Returned record should have an ID that start with '" + testPartner + "'.", record.getString(PartyLookupConfiguration.INOUT_PARTY_ID).toLowerCase().startsWith(testPartner.toLowerCase()));
            if (testPartner.equals(record.getString(PartyLookupConfiguration.INOUT_PARTY_ID))) {
                if (found) {
                    fail(testPartner + " was returned twice.");
                }
                found = true;
            }
        }
        assertTrue(testPartner + " was not found.", found);

        // 2. test search by name
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_GROUP_NAME, "pArtNer");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findPartners();

        // test we found the demo data: DemoAccount1, DemoAccount1Sub
        assertGwtLookupFound(lookup, Arrays.asList("demopartner1", "demopartner2", "demopartner3"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("participator4"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 3. test search by phone
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE, "1");
        provider.setParameter(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE, "31");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findPartners();

        // test we found the demo data: demopartner2,demopartner3 and not include participator4 (area 207)
        assertGwtLookupFound(lookup, Arrays.asList("demopartner2", "demopartner3"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("demopartner1", "participator4"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 4. test search by classification
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.IN_CLASSIFICATION, "NET_DUE_30DAY");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findPartners();

        // test we found the demo data: demopartner2
        assertGwtLookupFound(lookup, Arrays.asList("demopartner2"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("demopartner1", "demopartner3", "participator4"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 5. test search by city
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_CITY, "Bangor");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findPartners();

        // test we found the demo data: participator4, and not demopartner2, demopartner3 (city new york)
        assertGwtLookupFound(lookup, Arrays.asList("participator4"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("demopartner1", "demopartner2", "demopartner3"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 6. test search by postal code
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_POSTAL_CODE, "10001");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findPartners();

        // test we found the demo data: demopartner2,demopartner3
        assertGwtLookupFound(lookup, Arrays.asList("demopartner2", "demopartner3"), PartyLookupConfiguration.INOUT_PARTY_ID);
        // test participator4 is not found, its postal code being 04401
        assertGwtLookupNotFound(lookup, Arrays.asList("participator4"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 7. test more specific search, postal code + address
        provider.setParameter(PartyLookupConfiguration.INOUT_ADDRESS, "97th");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findPartners();

        // test we found the demo data: demopartner3, not demopartner1/2, participator4
        assertGwtLookupFound(lookup, Arrays.asList("demopartner3"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("demopartner1", "demopartner2", "participator4"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 8. test smaller page size
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "2");
        lookup = new PartyLookupService(provider);
        lookup.findPartners();

        // should return only 2 (page size) results
        assertTrue("There should be only 2 records returned.", lookup.getResults().size() == 2);
        // but the total number of records should still be more than 2 (there are 4 demo partners)
        assertTrue("There should be at least 4 records found in total.", lookup.getResultTotalCount() > lookup.getResults().size());

        // 9. test sorting
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_SORT_FIELD, PartyLookupConfiguration.INOUT_GROUP_NAME);
        provider.setParameter(UtilLookup.PARAM_SORT_DIRECTION, "ASC");
        lookup = new PartyLookupService(provider);
        lookup.findPartners();

        // check returned records are sorted correctly
        List<GenericValue> expected = delegator.findByCondition("PartyFromByRelnAndContactInfoAndPartyClassification", EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "PARTNER"), null, UtilMisc.toList(PartyLookupConfiguration.INOUT_PARTY_ID, PartyLookupConfiguration.INOUT_GROUP_NAME), UtilMisc.toList(PartyLookupConfiguration.INOUT_GROUP_NAME + " ASC"), Repository.DISTINCT_FIND_OPTIONS);
        expected = expected.subList(0, (UtilLookup.DEFAULT_LIST_PAGE_SIZE > expected.size() ? expected.size() : UtilLookup.DEFAULT_LIST_PAGE_SIZE));
        assertGwtLookupSort(lookup, PartyLookupConfiguration.INOUT_GROUP_NAME, expected);
    }

    /**
     * Test the GWT supplier lookup.
     * @throws Exception if an error occurs
     */
    public void testGwtSupplierLookup() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);
        String testSupplier = "DemoSupplier";
        // 1. test that we can find the testSupplier by ID in a case insensitive way
        provider.setParameter(PartyLookupConfiguration.INOUT_PARTY_ID, testSupplier.toLowerCase());
        PartyLookupService lookup = new PartyLookupService(provider);
        lookup.findSuppliers();
        assertTrue("There should be at least one record found (" + testSupplier + ").", lookup.getResultTotalCount() > 0);

        // test that all found records have their ID starting with ID of testSupplier, and that testSupplier is in the list
        boolean found = false;
        for (EntityInterface record : lookup.getResults()) {
            assertTrue("Returned record should have an ID that start with '" + testSupplier + "'.", record.getString(PartyLookupConfiguration.INOUT_PARTY_ID).toLowerCase().startsWith(testSupplier.toLowerCase()));
            if (testSupplier.equals(record.getString(PartyLookupConfiguration.INOUT_PARTY_ID))) {
                if (found) {
                    fail(testSupplier + " was returned twice.");
                }
                found = true;
            }
        }
        assertTrue(testSupplier + " was not found.", found);

        // 2. test search by name
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_GROUP_NAME, "demo");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findSuppliers();

        // test we found the demo data: DemoSupplier
        assertGwtLookupFound(lookup, Arrays.asList("DemoSupplier"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("BigSupplier", "EuroSupplier"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 3. test search by phone
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE, "1");
        provider.setParameter(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE, "555");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findSuppliers();

        // test we found the demo data: BigSupplier and not include DemoSupplier,EuroSupplier
        assertGwtLookupFound(lookup, Arrays.asList("BigSupplier"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoSupplier", "EuroSupplier"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 4. test search by city
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_CITY, "New York");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findSuppliers();

        // test we found the demo data: BigSupplier, and not DemoSupplier, EuroSupplier
        assertGwtLookupFound(lookup, Arrays.asList("BigSupplier"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoSupplier", "EuroSupplier"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 5. test search by address
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_ADDRESS, "2005");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findSuppliers();

        // test we found the demo data: BigSupplier,EuroSupplier not DemoSupplier
        assertGwtLookupFound(lookup, Arrays.asList("BigSupplier", "EuroSupplier"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoSupplier"), PartyLookupConfiguration.INOUT_PARTY_ID);


        // 6. test more specific search, postal code + address
        provider.setParameter(PartyLookupConfiguration.INOUT_POSTAL_CODE, "10000");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findSuppliers();

        // test we found the demo data: BigSupplier
        assertGwtLookupFound(lookup, Arrays.asList("BigSupplier"), PartyLookupConfiguration.INOUT_PARTY_ID);
        // test DemoSupplier,EuroSupplier is not found
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoSupplier", "EuroSupplier"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 7. test smaller page size
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "2");
        lookup = new PartyLookupService(provider);
        lookup.findSuppliers();

        // should return only 2 (page size) results
        assertTrue("There should be only 2 records returned.", lookup.getResults().size() == 2);
        // but the total number of records should still be more than 2 (there are 3 demo supplier)
        Debug.logInfo("lookup.getResultTotalCount() : " + lookup.getResultTotalCount(), MODULE);
        assertTrue("There should be at least 3 records found in total.", lookup.getResultTotalCount() > lookup.getResults().size());

        // 8. test sorting
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_SORT_FIELD, PartyLookupConfiguration.INOUT_GROUP_NAME);
        provider.setParameter(UtilLookup.PARAM_SORT_DIRECTION, "ASC");
        lookup = new PartyLookupService(provider);
        lookup.findSuppliers();

        // check returned records are sorted correctly
        List<GenericValue> expected = delegator.findByCondition("PartyFromByRelnAndContactInfoAndPartyClassification", EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "SUPPLIER"), null, UtilMisc.toList(PartyLookupConfiguration.INOUT_PARTY_ID, PartyLookupConfiguration.INOUT_GROUP_NAME), UtilMisc.toList(PartyLookupConfiguration.INOUT_GROUP_NAME + " ASC"), Repository.DISTINCT_FIND_OPTIONS);
        expected = expected.subList(0, (UtilLookup.DEFAULT_LIST_PAGE_SIZE > expected.size() ? expected.size() : UtilLookup.DEFAULT_LIST_PAGE_SIZE));
        assertGwtLookupSort(lookup, PartyLookupConfiguration.INOUT_GROUP_NAME, expected);

    }

    /**
     * Test the GWT lead lookup.
     * @throws Exception if an error occurs
     */
    public void testGwtLeadLookup() throws Exception {
        InputProviderInterface provider = new TestInputProvider(admin, dispatcher);
        String testLead = "DemoLead1";
        // 1. test that we can find the testLead by ID in a case insensitive way
        provider.setParameter(PartyLookupConfiguration.INOUT_PARTY_ID, testLead.toLowerCase());
        PartyLookupService lookup = new PartyLookupService(provider);
        lookup.findLeads();
        assertTrue("There should be at least one record found (" + testLead + ").", lookup.getResultTotalCount() > 0);

        // test that all found records have their ID starting with ID of testLead, and that testLead is in the list
        boolean found = false;
        for (EntityInterface record : lookup.getResults()) {
            assertTrue("Returned record should have an ID that start with '" + testLead + "'.", record.getString(PartyLookupConfiguration.INOUT_PARTY_ID).toLowerCase().startsWith(testLead.toLowerCase()));
            if (testLead.equals(record.getString(PartyLookupConfiguration.INOUT_PARTY_ID))) {
                if (found) {
                    fail(testLead + " was returned twice.");
                }
                found = true;
            }
        }
        assertTrue(testLead + " was not found.", found);

        // 2. test search by name
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_FIRST_NAME, "eMo");
        provider.setParameter(PartyLookupConfiguration.INOUT_LAST_NAME, "LEad");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findLeads();

        // test we found the demo data: DemoLead1/A/B
        assertGwtLookupFound(lookup, Arrays.asList("DemoLead1", "DemoLeadA", "DemoLeadB"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoLeadC"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 3. test search by phone
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE, "1");
        provider.setParameter(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE, "31");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findLeads();

        // test we found the demo data: DemoLeadA/3 and not include DemoLead1/4
        assertGwtLookupFound(lookup, Arrays.asList("DemoLeadA", "DemoLeadB"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoLead1", "DemoLeadC"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 5. test search by city
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_CITY, "Bangor");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findLeads();

        // test we found the demo data: DemoLeadC, and not DemoLead1/2/3
        assertGwtLookupFound(lookup, Arrays.asList("DemoLead1", "DemoLeadC"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoLeadA", "DemoLeadB"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 6. test search by postal code
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(PartyLookupConfiguration.INOUT_POSTAL_CODE, "10001");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findLeads();

        // test we found the demo data: DemoLeadA,DemoLeadB
        assertGwtLookupFound(lookup, Arrays.asList("DemoLeadA", "DemoLeadB"), PartyLookupConfiguration.INOUT_PARTY_ID);
        // test DemoLeadC is not found, its postal code being 04401
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoLeadC"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 7. test more specific search, postal code + address
        provider.setParameter(PartyLookupConfiguration.INOUT_ADDRESS, "97th");
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "999"); // set high pager limit so other test won't mask the expected results
        lookup = new PartyLookupService(provider);
        lookup.findLeads();

        // test we found the demo data: DemoLeadB, not DemoLead1/A/C
        assertGwtLookupFound(lookup, Arrays.asList("DemoLeadB"), PartyLookupConfiguration.INOUT_PARTY_ID);
        assertGwtLookupNotFound(lookup, Arrays.asList("DemoLead1", "DemoLeadA", "DemoLeadC"), PartyLookupConfiguration.INOUT_PARTY_ID);

        // 8. test smaller page size
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_PAGER_LIMIT, "2");
        lookup = new PartyLookupService(provider);
        lookup.findLeads();

        // should return only 2 (page size) results
        assertTrue("There should be only 2 records returned.", lookup.getResults().size() == 2);
        // but the total number of records should still be more than 2 (there are 4 demo leads)
        assertTrue("There should be at least 4 records found in total.", lookup.getResultTotalCount() > lookup.getResults().size());

        // 9. test sorting
        provider = new TestInputProvider(admin, dispatcher);
        provider.setParameter(UtilLookup.PARAM_SORT_FIELD, PartyLookupConfiguration.INOUT_GROUP_NAME);
        provider.setParameter(UtilLookup.PARAM_SORT_DIRECTION, "ASC");
        lookup = new PartyLookupService(provider);
        lookup.findLeads();

        // check returned records are sorted correctly
        List<GenericValue> expected = delegator.findByCondition("PartyFromByRelnAndContactInfoAndPartyClassification", EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "LEAD"), null, UtilMisc.toList(PartyLookupConfiguration.INOUT_PARTY_ID, PartyLookupConfiguration.INOUT_GROUP_NAME), UtilMisc.toList(PartyLookupConfiguration.INOUT_GROUP_NAME + " ASC"), Repository.DISTINCT_FIND_OPTIONS);
        expected = expected.subList(0, (UtilLookup.DEFAULT_LIST_PAGE_SIZE > expected.size() ? expected.size() : UtilLookup.DEFAULT_LIST_PAGE_SIZE));
        assertGwtLookupSort(lookup, PartyLookupConfiguration.INOUT_GROUP_NAME, expected);

    }

    /**
     * Test create account service with duplicate account name.
     * @throws Exception if an error occurs
     */
    public void testDuplicateAccountsWithName() throws Exception {
        // create a account by service crmsfa.createAccount
        CrmsfaCreateAccountService createAccount = new CrmsfaCreateAccountService();
        createAccount.setInUserLogin(admin);
        createAccount.setInAccountName("Duplicate Account Test");
        runAndAssertServiceSuccess(createAccount);
        String accountId = createAccount.getOutPartyId();
        Debug.logInfo("created an account [" + accountId + "] with account name [Duplicate Account Test]", MODULE);

        createAccount = new CrmsfaCreateAccountService();
        createAccount.setInUserLogin(admin);
        createAccount.setInAccountName("DUPLICATE ACCOUNT TEST");
        runAndAssertServiceError(createAccount);

        Set<PartyGroup> parties = createAccount.getOutDuplicateAccountsWithName();
        assertNotEmpty("Should have found duplicate account [" + accountId + "]", parties);
        Set<String> partyIds = Entity.getDistinctFieldValues(String.class, parties, PartyGroup.Fields.partyId);
        assertTrue("Should have found the account [" + accountId + "] in the duplicate account results", partyIds.contains(accountId));

        createAccount = new CrmsfaCreateAccountService();
        createAccount.setInUserLogin(admin);
        createAccount.setInAccountName("duplicate account test");
        runAndAssertServiceError(createAccount);
        parties = createAccount.getOutDuplicateAccountsWithName();
        assertNotEmpty("Should have found duplicate account [" + accountId + "]", parties);
        partyIds = Entity.getDistinctFieldValues(String.class, parties, PartyGroup.Fields.partyId);
        assertTrue("Should have found the account [" + accountId + "] in the duplicate account results", partyIds.contains(accountId));

        // after passing forceComplete=Y causes the account to be created successfully
        createAccount = new CrmsfaCreateAccountService();
        createAccount.setInUserLogin(admin);
        createAccount.setInAccountName("duplicate account test");
        createAccount.setInForceComplete("Y");
        runAndAssertServiceSuccess(createAccount);
        String duplicateAccountId = createAccount.getOutPartyId();

        // deactive the accounts we created
        CrmsfaDeactivateAccountService deactivateAccount = new CrmsfaDeactivateAccountService();
        deactivateAccount.setInUserLogin(admin);
        deactivateAccount.setInPartyId(accountId);
        runAndAssertServiceSuccess(deactivateAccount);

        deactivateAccount = new CrmsfaDeactivateAccountService();
        deactivateAccount.setInUserLogin(admin);
        deactivateAccount.setInPartyId(duplicateAccountId);
        runAndAssertServiceSuccess(deactivateAccount);

    }

    /**
     * Test create supplier service with duplicate supplier name.
     * @throws Exception if an error occurs
     */
    public void testDuplicateSuppliersWithName() throws Exception {
        // create a account by service purchasing.createSupplier
        PurchasingCreateSupplierService createSupplier = new PurchasingCreateSupplierService();
        createSupplier.setInUserLogin(admin);
        createSupplier.setInGroupName("Duplicate Supplier Test");
        createSupplier.setInRequires1099("Y");
        runAndAssertServiceSuccess(createSupplier);
        String supplierId = createSupplier.getOutPartyId();
        Debug.logInfo("created an supplier [" + supplierId + "] with supplier name [Duplicate Supplier Test]", MODULE);

        createSupplier = new PurchasingCreateSupplierService();
        createSupplier.setInUserLogin(admin);
        createSupplier.setInGroupName("DUPLICATE SUPPLIER TEST");
        createSupplier.setInRequires1099("Y");
        runAndAssertServiceError(createSupplier);

        Set<PartyGroup> parties = createSupplier.getOutDuplicateSuppliersWithName();
        assertNotEmpty("Should have found duplicate supplier [" + supplierId + "]", parties);
        Set<String> partyIds = Entity.getDistinctFieldValues(String.class, parties, PartyGroup.Fields.partyId);
        assertTrue("Should have found the supplier [" + supplierId + "] in the duplicate supplier results", partyIds.contains(supplierId));

        createSupplier = new PurchasingCreateSupplierService();
        createSupplier.setInUserLogin(admin);
        createSupplier.setInGroupName("duplicate supplier test");
        createSupplier.setInRequires1099("Y");
        runAndAssertServiceError(createSupplier);
        parties = createSupplier.getOutDuplicateSuppliersWithName();
        assertNotEmpty("Should have found duplicate supplier [" + supplierId + "]", parties);
        partyIds = Entity.getDistinctFieldValues(String.class, parties, PartyGroup.Fields.partyId);
        assertTrue("Should have found the supplier [" + supplierId + "] in the duplicate supplier results", partyIds.contains(supplierId));

        // after passing forceComplete=Y causes the supplier to be created successfully
        createSupplier = new PurchasingCreateSupplierService();
        createSupplier.setInUserLogin(admin);
        createSupplier.setInGroupName("duplicate supplier test");
        createSupplier.setInForceComplete("Y");
        createSupplier.setInRequires1099("Y");
        runAndAssertServiceSuccess(createSupplier);
        String duplicateSupplierId = createSupplier.getOutPartyId();

        // deactive the suppliers we created
        GenericValue supplierParty = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", supplierId));
        supplierParty.put("statusId", "PARTY_DISABLED");
        supplierParty.store();

        supplierParty = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", duplicateSupplierId));
        supplierParty.put("statusId", "PARTY_DISABLED");
        supplierParty.store();
    }

    /**
     * Test organizationRepository's getOrganizationTemplates returns the new party with role ORGANIZATION_TEMPL.
     * @throws GeneralException if an error occurs
     */
    public void testGetOrganizationTemplates() throws GeneralException {
        // 1. Create PartyGroup
        Session session = domainsLoader.getInfrastructure().getSession();
        org.opentaps.base.entities.Party party = new org.opentaps.base.entities.Party();
        party.setPartyTypeId("PARTY_GROUP");
        session.save(party);
        session.flush();
        PartyGroup partyGroup = new PartyGroup();
        partyGroup.setPartyId(party.getPartyId());
        partyGroup.setGroupName("Test GroupName for testGetOrganizationTemplates");
        session.save(partyGroup);
        // 2. Associate ORGANIZATION_TEMPL role with it
        PartyRole internalOrganizationRole = new PartyRole();
        PartyRolePk pk = new PartyRolePk();
        pk.setPartyId(party.getPartyId());
        pk.setRoleTypeId("ORGANIZATION_TEMPL");
        internalOrganizationRole.setId(pk);
        session.save(internalOrganizationRole);
        session.flush();
        session.close();
        // 3. Verify that it is returned from getOrganizationTemplates
        OrganizationRepositoryInterface orgRepository = organizationDomain.getOrganizationRepository();
        List<PartyGroup> partyGroups = orgRepository.getOrganizationTemplates();
        boolean foundTheParty = false;
        for (PartyGroup group : partyGroups) {
            if (group.getPartyId().equals(party.getPartyId())) {
                foundTheParty = true;
                break;
            }
        }
        assertTrue("We should found new party [" + party.getPartyId() + "] with role ORGANIZATION_TEMPL in the result", foundTheParty);
    }

    /**
     * Test organizationRepository's getOrganizationWithoutLedgerSetup returns the new party with role INTERNAL_ORGANIZATIO.
     * @throws GeneralException if an error occurs
     */
    public void testGetOrganizationWithoutLedgerSetup() throws GeneralException {
        Session session = domainsLoader.getInfrastructure().getSession();
        org.opentaps.base.entities.Party party = new org.opentaps.base.entities.Party();
        party.setPartyTypeId("PARTY_GROUP");
        session.save(party);
        session.flush();
        PartyGroup partyGroup = new PartyGroup();
        partyGroup.setPartyId(party.getPartyId());
        partyGroup.setGroupName("Test GroupName for testGetOrganizationWithoutLedgerSetup");
        session.save(partyGroup);
        PartyRole internalOrganizationRole = new PartyRole();
        PartyRolePk pk = new PartyRolePk();
        pk.setPartyId(party.getPartyId());
        pk.setRoleTypeId("INTERNAL_ORGANIZATIO");
        internalOrganizationRole.setId(pk);
        session.save(internalOrganizationRole);
        session.flush();
        session.close();

        OrganizationRepositoryInterface orgRepository = organizationDomain.getOrganizationRepository();
        List<PartyGroup> partyGroups = orgRepository.getOrganizationWithoutLedgerSetup();
        boolean foundTheParty = false;
        for (PartyGroup group : partyGroups) {
            if (group.getPartyId().equals(party.getPartyId())) {
                foundTheParty = true;
                break;
            }
        }
        assertTrue("We should found new party [" + party.getPartyId() + "] with role INTERNAL_ORGANIZATIO in the result", foundTheParty);
    }

    /**
     * Test the crmsfa.uploadLeads service.
     * @throws Exception if an error occurs
     */
    public void testUploadLeads() throws Exception {
        // upload the test lead_import_example.xls file
        String fileName = "lead_import_example.xls";
        CrmsfaUploadLeadsService upSer = new CrmsfaUploadLeadsService();
        upSer.setInUserLogin(admin);
        upSer.setInUploadedFileFileName(fileName);
        upSer.setInUploadedFileContentType("application/vnd.ms-excel");
        upSer.setInUploadedFile(getByteBufferFromFile("opentaps/crmsfa/data/xls/" + fileName));
        runAndAssertServiceSuccess(upSer);
        List<String> leadIds = upSer.getOutCreatedLeadIds();

        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepository repo = (PartyRepository) partyDomain.getPartyRepository();

        // check that Lana Lee can be found
        List<Person> persons = repo.findList(Person.class, EntityCondition.makeCondition(
                                                                 EntityCondition.makeCondition(Person.Fields.firstName.name(), "Lana"),
                                                                 EntityCondition.makeCondition(Person.Fields.lastName.name(), "Lee"),
                                                                 EntityCondition.makeCondition(Person.Fields.partyId.name(), EntityOperator.IN, leadIds)));
        assertNotEmpty("Should have found the imported lead Lana Lee", persons);

        // check that Leanne Chambers can be found
        persons = repo.findList(Person.class, EntityCondition.makeCondition(
                                                                 EntityCondition.makeCondition(Person.Fields.firstName.name(), "Leanne"),
                                                                 EntityCondition.makeCondition(Person.Fields.lastName.name(), "Chambers"),
                                                                 EntityCondition.makeCondition(Person.Fields.partyId.name(), EntityOperator.IN, leadIds)));
        assertNotEmpty("Should have found the imported lead Leanne Chambers", persons);
    }
    
    /**
     * Test the custom party fields service,
     * opentaps.createPartyAttribute/opentaps.updatePartyAttribute/opentaps.removePartyAttribute.
     * @throws Exception if an error occurs
     */
    public void testCustomFieldsForParty() throws Exception {
        // DemoSalesRep1 creates a custom field (PartyAttribute) for a lead, ie DemoLead1
        GenericValue demoSalesRep1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
        Map input = UtilMisc.toMap("userLogin", demoSalesRep1);
        input.put("partyId", "DemoLead1");
        input.put("attrName", "customField1");
        input.put("attrValue", "initValue1");
        Map output = runAndAssertServiceSuccess("opentaps.createPartyAttribute", input);
        
        // verify:
        // DemoSalesRep2 update the PartyAttribute fails
        GenericValue demoSalesRep2 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
        input = UtilMisc.toMap("userLogin", demoSalesRep2);
        input.put("partyId", "DemoLead1");
        input.put("attrName", "customField1");
        input.put("attrValue", "newValue1");
        output = runAndAssertServiceError("opentaps.updatePartyAttribute", input);
        
        // DemoSalesRep2 delete the PartyAttribute fails
        input = UtilMisc.toMap("userLogin", demoSalesRep2);
        input.put("partyId", "DemoLead1");
        input.put("attrName", "customField1");
        output = runAndAssertServiceError("opentaps.removePartyAttribute", input);
        
        // DemoSalesManager update the PartyAttribute success
        GenericValue demoSalesManager = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        input = UtilMisc.toMap("userLogin", demoSalesManager);
        input.put("partyId", "DemoLead1");
        input.put("attrName", "customField1");
        input.put("attrValue", "newValue1");
        output = runAndAssertServiceSuccess("opentaps.updatePartyAttribute", input);
        
        // DemoSalesManager delete the PartyAttribute success 
        input = UtilMisc.toMap("userLogin", demoSalesManager);
        input.put("partyId", "DemoLead1");
        input.put("attrName", "customField1");
        output = runAndAssertServiceSuccess("opentaps.removePartyAttribute", input);
        
        
    }
}
