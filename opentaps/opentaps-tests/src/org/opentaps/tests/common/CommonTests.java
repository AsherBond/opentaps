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

package org.opentaps.tests.common;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.product.UtilProduct;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Some common tests.
 */
public class CommonTests extends OpentapsTestCase {

	Infrastructure infrastructure = null;
	
    @Override
    public void setUp() throws Exception {
        super.setUp();
        infrastructure = new Infrastructure(dispatcher);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        infrastructure = null;
    }

    /**
     * Test the UtilCommon isEquivalent method.
     */
    public void testListEquivalenceMethod() {
        assertTrue(UtilCommon.isEquivalent(UtilMisc.toList("A", "B", "C", "D"), UtilMisc.toList("A", "B", "C", "D")));
        assertFalse(UtilCommon.isEquivalent(UtilMisc.toList("A", "B", "C", "D"), UtilMisc.toList("A", "B", "C")));
        assertFalse(UtilCommon.isEquivalent(UtilMisc.toList("A", "B", "C", "D"), UtilMisc.toList(BigDecimal.ONE, new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("4"))));
        assertTrue(UtilCommon.isEquivalent(UtilMisc.toList(BigDecimal.ONE, new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("4")), UtilMisc.toList(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("4"))));

        // the combinations can go on and on but this is enough for me for now
    }

    /**
     * Test UtilCommon getOrgBaseCurrency.
     */
    public void testAcctgPrefBaseCurrency() {
        assertEquals("Base currency for Company should be USD by default", "USD", UtilCommon.getOrgBaseCurrency(organizationPartyId, delegator));
    }

    /**
     * Test UtilCommon getOrgCOGSMethodId.
     */
    public void testAcctgPrefCOGSMethod() {
        assertEquals("COGS Method for Company should be COGS_AVG_COST by default", "COGS_AVG_COST", UtilCommon.getOrgCOGSMethodId(organizationPartyId, delegator));
    }

    /**
     * This method tests that the conservative value for a product is gotten from its standard cost.
     */
    public void testConservativeValueFromStandardCost() {
        assertEquals("Conservative value of MAT_A_COST should be 9.0 USD", new BigDecimal("9.0"), UtilProduct.getConservativeValue("MAT_A_COST", "USD", dispatcher));
    }

    /**
     * This method tests the conservative value for a supplier product.
     */
    public void testConservativeValueFromSupplierProducts() {
        assertEquals("Conservative value of GZ-1000 should be 3.75 USD", new BigDecimal("3.75"), UtilProduct.getConservativeValue("GZ-1000", "USD", dispatcher));
    }

    /**
     * This method tests the conservative value for a supplier product in another currency.
     */
    public void testConservativeValueFromSupplierProductsInEuro() {
        assertEquals("Conservative value of GZ-1000 should be 3.00 EUR", new BigDecimal("3.00"), UtilProduct.getConservativeValue("GZ-1000", "EUR", dispatcher));
    }

    /**
     * This method tests the conservative value for a supplier product for a specific organization.
     */
    public void testConservativeValueFromSupplierProductsByOrgParty() {
        assertEquals("Conservative value of GZ-1000 should be 3.75 USD for Company", new BigDecimal("3.75"), UtilProduct.getConservativeValueForOrg("GZ-1000", "Company", dispatcher));
    }

    /**
     * Test UPCA expansion.
     */
    public void testUPCAExpansion() {
        String upce = "08014146";
        String upca = "080140000016";
        assertEquals("UPC-A expanded from UPC-E " + upce + " should be " + upca, upca, UtilProduct.expandUPCE(upce));
    }

    /**
     * Test UPCE compression.
     */
    public void testUPCECompression() {
        String upce = "08014146";
        String upca = "080140000016";
        assertEquals("UPC-E compressed from UPC-A " + upca + " should be " + upce, upce, UtilProduct.compressUPCA(upca));
    }

    /**
     * Test UPC compression and re expansion.
     */
    public void testUPCCompressExpand() {
        String upce = "08014146";
        assertEquals("UPC-E created by expanding UPC-E " + upce + "to UPC-A and re-compressing to UPC-E should still be " + upce, upce, UtilProduct.compressUPCA(UtilProduct.expandUPCE(upce)));
    }

    /**
     * Test creates UPCA[E] identifiers for product GZ-2002 and tries create the same identifiers for
     * product GZ-5005. Last operation must throw service exception as UPC code should be unique for
     * a product.
     * Then test UPC code validation on a test product.
     * @exception GeneralException if an error occurs
     */
    public void testCreateNonUniqueUpcForProduct() throws GeneralException {

        String testUPCE = "00123457";       // or try 08014146
        String testUPCA = "037631611305";   // this is a real UPC
        String productId = "GZ-2002";
        String refProductId = "GZ-5005";
        String upcValidationProductId = "GZ-1006-1";
        Map<String, Object> input = null;

        // prepare data for testing
        delegator.removeByCondition("GoodIdentification", EntityCondition.makeCondition(EntityOperator.AND,
                                                               EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                                                               EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.IN, UtilMisc.toList("UPCA", "UPCE"))));
        delegator.removeByCondition("GoodIdentification", EntityCondition.makeCondition(EntityOperator.AND,
                                                               EntityCondition.makeCondition("productId", EntityOperator.EQUALS, refProductId),
                                                               EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.IN, UtilMisc.toList("UPCA", "UPCE"))));

        GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
        assertEquals("There is no product " + productId + " that should be used for testing UPC uniqueness.", true, product != null);
        product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", refProductId));
        assertEquals("There is no product " + refProductId + " that should be used for testing UPC uniqueness.", true, product != null);

        input = UtilMisc.toMap("goodIdentificationTypeId", "UPCA", "productId", productId, "idValue", testUPCA, "userLogin", admin);
        runAndAssertServiceSuccess("createGoodIdentification", input);
        input = UtilMisc.toMap("goodIdentificationTypeId", "UPCE", "productId", productId, "idValue", testUPCE, "userLogin", admin);
        runAndAssertServiceSuccess("createGoodIdentification", input);

        // attempts to create the same UPC codes for GZ-5005
        input = UtilMisc.toMap("goodIdentificationTypeId", "UPCA", "productId", refProductId, "idValue", testUPCA, "userLogin", admin);
        runAndAssertServiceError("createGoodIdentification", input);
        input = UtilMisc.toMap("goodIdentificationTypeId", "UPCE", "productId", refProductId, "idValue", testUPCE, "userLogin", admin);
        runAndAssertServiceError("createGoodIdentification", input);

        // validation should cause this to fail: too short
        runAndAssertServiceError("createGoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "UPCA", "productId", upcValidationProductId, "idValue", "123456", "userLogin", admin));

        // validation should cause this to fail: bogus upc
        runAndAssertServiceError("createGoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "UPCA", "productId", upcValidationProductId, "idValue", "999999999999", "userLogin", admin));

        // this is a bad upce - too short
        runAndAssertServiceError("createGoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "UPCE", "productId", upcValidationProductId, "idValue", "0123456", "userLogin", admin));

        // this is a bad upce - bad code
        runAndAssertServiceError("createGoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "UPCE", "productId", upcValidationProductId, "idValue", "00123456", "userLogin", admin));

        // first remove any previous test data
        delegator.removeByCondition("GoodIdentification", EntityCondition.makeCondition(EntityOperator.AND,
                                                              EntityCondition.makeCondition("productId", EntityOperator.EQUALS, upcValidationProductId),
                                                              EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.EQUALS, "UPCA"),
                                                              EntityCondition.makeCondition("idValue", EntityOperator.EQUALS, "012345678998")));

        // this is actually a good UPC Code: I found it by running these tests!
        runAndAssertServiceSuccess("createGoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "UPCA", "productId", upcValidationProductId, "idValue", "012345678998", "userLogin", admin));
    }

    /**
     * Test creates email customerservice@mycompany.com with purpose RECEIVE_EMAIL_OWNER. EECA service
     * opentaps.checkReceiveEmailOwnerUniqueness should be block creation if the address exists with
     * the same purpose.
     * @exception GeneralException if an error occurs
     */
    public void testCreateNonUniqueEmailOwner() throws GeneralException {

        Map<String, Object> createContext = new HashMap<String, Object>();
        createContext.put("userLogin", admin);
        createContext.put("contactMechTypeId", "EMAIL_ADDRESS");
        createContext.put("infoString", "customerservice@mycompany.com");
        createContext.put("partyId", "DemoCSR");
        createContext.put("fromDate", UtilDateTime.nowTimestamp());
        createContext.put("contactMechPurposeTypeId", "RECEIVE_EMAIL_OWNER");
        createContext.put("emailAddress", "customerservice@mycompany.com");

        /*
         * Creates new email address for DemoCSR with purpose RECEIVE_EMAIL_OWNER.
         * Should be fail as DemoCSR is owner of customerservice@mycompany.com already.
         */
//        simple method createPartyEmailAddress creates an email address only if for the same party a similar one does not exist. 
//        It simply returns with an info in the log.
//        runAndAssertServiceError("createPartyEmailAddress", createContext);

        /*
         * Checks is there customerservice@mycompany.com among existent emails. If it is found we
         * expire its purpose.
         */
        List<EntityCondition> conditions = new ArrayList<EntityCondition>();
        conditions.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "RECEIVE_EMAIL_OWNER"));
        conditions.add(EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "EMAIL_ADDRESS"));
        Timestamp now = UtilDateTime.nowTimestamp();
        conditions.add(EntityUtil.getFilterByDateExpr(now, "contactFromDate", "contactThruDate"));
        conditions.add(EntityUtil.getFilterByDateExpr(now, "purposeFromDate", "purposeThruDate"));

        List<GenericValue> emails = delegator.findByCondition("PartyContactWithPurpose", EntityCondition.makeCondition(conditions, EntityOperator.AND), null, null);

        assertEquals("Party have no active emails with purpose RECEIVE_EMAIL_OWNER. It's possible error in demo data.", true, UtilValidate.isNotEmpty(emails));

        for (GenericValue email : emails) {
            if ("customerservice@mycompany.com".equalsIgnoreCase(email.getString("infoString"))) {
            	GenericValue emailPurpose = delegator.findByPrimaryKey("PartyContactMech", UtilMisc.toMap("partyId", email.getString("partyId"), "contactMechId", email.getString("contactMechId"), "fromDate", email.get("purposeFromDate")));
                if (emailPurpose != null) {
                    emailPurpose.set("thruDate", UtilDateTime.nowTimestamp());
                    emailPurpose.store();
                }
            }
        }

        /*
         * Creates new email address for DemoCSR with purpose RECEIVE_EMAIL_OWNER.
         * Should be success as all emails customerservice@mycompany.com were expired in previous step.
         */
        runAndAssertServiceSuccess("createPartyEmailAddress", createContext);

    }

    /**
     * Test the UtilProduct isPhysical.
     * @exception GeneralException if an error occurs
     */
    public void testPhysicalProduct() throws GeneralException {
        assertTrue(UtilProduct.isPhysical(delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "GZ-1000"))));
        assertFalse(UtilProduct.isPhysical(delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "service1"))));
    }

    /**
     * Test the UtilDate date difference calculation.
     * @exception GeneralException if an error occurs
     */
    public void testDateDifference() throws GeneralException {
        // simple test
        assertEquals(10, UtilDate.dateDifference(Timestamp.valueOf("2007-01-01 00:00:00.00"), Timestamp.valueOf("2007-01-11 00:00:00.00")).intValue());
        // as some time to the start and end time, should not change the difference
        assertEquals(10, UtilDate.dateDifference(Timestamp.valueOf("2007-01-01 10:00:00.00"), Timestamp.valueOf("2007-01-11 00:00:00.00")).intValue());
        assertEquals(10, UtilDate.dateDifference(Timestamp.valueOf("2007-01-01 10:00:00.00"), Timestamp.valueOf("2007-01-11 20:00:00.00")).intValue());
        assertEquals(10, UtilDate.dateDifference(Timestamp.valueOf("2007-01-01 00:00:00.00"), Timestamp.valueOf("2007-01-11 20:00:00.00")).intValue());
        // test over more than a year
        assertEquals(375, UtilDate.dateDifference(Timestamp.valueOf("2007-01-01 10:00:00.00"), Timestamp.valueOf("2008-01-11 20:00:00.00")).intValue());
        assertEquals(3663, UtilDate.dateDifference(Timestamp.valueOf("2007-01-01 10:00:00.00"), Timestamp.valueOf("2017-01-11 20:00:00.00")).intValue());
        // leon failed test ?
        assertEquals(20, UtilDate.dateDifference(Timestamp.valueOf("2008-07-31 14:00:00.00"), Timestamp.valueOf("2008-08-20 20:00:00.00")).intValue());

    }
    
    /**
     * Test we can get configuration value
     */
    public void testGetConfiguration() throws GeneralException {
    	// defined in opentaps-tests/data/common/CommonTestData.xml
    	String value = infrastructure.getConfigurationValue("TEST_CONFIG_TYPE");  
    	assertTrue("Test value".equals(value));
    }

    /**
     * Test default value during getConfigurationValue method
     */
    public void testGetConfigurationWithDefault() throws GeneralException {
    	// defined in opentaps-tests/data/common/CommonTestData.xml
    	String DEFAULT_VALUE = "default value 1";
    	String value = infrastructure.getConfigurationValue("TEST_CONFIG_UNCONFIGURED_TYPE", DEFAULT_VALUE);
    	assertTrue(DEFAULT_VALUE.equals(value));
    }

    /**
     * Test default value in OpentapsConfigurationType
     */
    public void testConfigurationTypeDefault() throws GeneralException {
    	// defined in opentaps-tests/data/common/CommonTestData.xml
    	String DEFAULT_VALUE = "Default value 2";
    	String value = infrastructure.getConfigurationValue("TEST_CONFIG_UNCONFIGURED_TYPE");
    	assertTrue(DEFAULT_VALUE.equals(value));
    }

    /**
     * Tests setting a value in OpentapsConfiguration
     * @throws GeneralException
     */
    public void testSetConfiguration() throws GeneralException {
    	// defined in opentaps-tests/data/common/CommonTestData.xml
    	String CONFIG_TYPE_ID = "TEST_CONFIG_SET_CONFIG_TYPE";
    	String DEFAULT_VALUE = "Not set yet";
    	String NEW_VALUE = "Has been set to new value";
    	assertTrue(DEFAULT_VALUE.equals(infrastructure.getConfigurationValue(CONFIG_TYPE_ID)));
    	infrastructure.setConfigurationValue(CONFIG_TYPE_ID, NEW_VALUE, null);
    	assertTrue(NEW_VALUE.equals(infrastructure.getConfigurationValue(CONFIG_TYPE_ID)));
    }
    
}
