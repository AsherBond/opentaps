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
package org.opentaps.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;
import javolution.util.FastList;
import junit.framework.TestCase;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.entities.InventoryItemTraceDetail;
import org.opentaps.base.entities.Party;
import org.opentaps.base.entities.PartyAcctgPreference;
import org.opentaps.base.entities.PartyGroup;
import org.opentaps.base.entities.PartyRole;
import org.opentaps.base.services.CopyOrganizationLedgerSetupService;
import org.opentaps.common.order.PurchaseOrderFactory;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.common.product.UtilProduct;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.inventory.InventoryDomainInterface;
import org.opentaps.domain.inventory.InventoryItem;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.inventory.InventoryServiceInterface;
import org.opentaps.domain.ledger.LedgerDomainInterface;
import org.opentaps.domain.manufacturing.ManufacturingDomainInterface;
import org.opentaps.domain.manufacturing.ManufacturingRepositoryInterface;
import org.opentaps.domain.manufacturing.bom.BomNodeInterface;
import org.opentaps.domain.manufacturing.bom.BomTreeInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationDomainInterface;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductDomainInterface;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.domain.purchasing.PurchasingDomainInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceWrapper;
import org.opentaps.gwt.common.server.lookup.EntityLookupAndSuggestService;
import org.opentaps.gwt.common.server.lookup.EntityLookupService;

/**
 * Opentaps Test Suite defines common testing patterns and API methods
 * for use by subclasses.  The intent is to help speed up the writing
 * of unit tests, and to keep the tests themselves clean and easy to read.
 */
public class OpentapsTestCase extends TestCase {

    private static final String MODULE = OpentapsTestCase.class.getName();

    public static final long STANDARD_PAUSE_DURATION = 1001; // standard pause
    public static final String DELEGATOR_NAME = "test";

    public static final String organizationPartyId = "Company";
    public static final String facilityId = "WebStoreWarehouse";
    public static final String shipGroupSeqId = "00001";

    private static final EtmMonitor etmMonitor = EtmManager.getEtmMonitor();

    protected Delegator delegator = null;
    protected LocalDispatcher dispatcher = null;
    protected GenericValue admin;
    protected Organization organization;
    protected String defaultOrganizationCostingMethodId = null;

    protected DomainsLoader domainsLoader;
    protected DomainsDirectory domainsDirectory;
    protected OrderDomainInterface orderDomain;
    protected BillingDomainInterface billingDomain;
    protected LedgerDomainInterface ledgerDomain;
    protected InventoryDomainInterface inventoryDomain;
    protected ProductDomainInterface productDomain;
    protected OrganizationDomainInterface organizationDomain;
    protected PurchasingDomainInterface purchasingDomain;

    protected OrganizationRepositoryInterface organizationRepository;

    // TODO: this must go, it conflicts with domain User object - leon 2008-11-14
    protected GenericValue User;

    // Since financial amounts and most business values are rounded to 2 decimal places this way,
    // we set these as the universal rounding settings for all tests.  Create your own rounding settings
    // if you need something different.
    public static final int DECIMALS = 2;
    public static final int ROUNDING = BigDecimal.ROUND_HALF_EVEN;

    /** Configurable final number of decimals in sales tax calculation, see <code>applications/accounting/config/arithmetic.properties</code>, default is <code>2</code>. */
    public static final int SALES_TAX_FINAL_DECIMALS = UtilNumber.getBigDecimalScale("salestax.final.decimals");
    /** Configurable intermediate number of decimals in sales tax calculation, see <code>applications/accounting/config/arithmetic.properties</code>, default is <code>3</code>. */
    public static final int SALES_TAX_CALC_DECIMALS = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    /** Configurable rounding in sales tax calculation, see <code>applications/accounting/config/arithmetic.properties</code>, default is <code>ROUND_HALF_UP</code>. */
    public static final int SALES_TAX_ROUNDING = UtilNumber.getBigDecimalRoundingMode("salestax.rounding");

    @Override
    public void setUp() throws Exception {
        delegator = DelegatorFactory.getDelegator(DELEGATOR_NAME);
        dispatcher = GenericDispatcher.getLocalDispatcher(DELEGATOR_NAME, delegator);
        admin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
        domainsLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        domainsDirectory = domainsLoader.loadDomainsDirectory();
        orderDomain = domainsDirectory.getOrderDomain();
        billingDomain = domainsDirectory.getBillingDomain();
        ledgerDomain = domainsDirectory.getLedgerDomain();
        inventoryDomain = domainsDirectory.getInventoryDomain();
        productDomain = domainsDirectory.getProductDomain();
        organizationDomain = domainsDirectory.getOrganizationDomain();
        purchasingDomain = domainsDirectory.getPurchasingDomain();
        organizationRepository = organizationDomain.getOrganizationRepository();
        organization = organizationRepository.getOrganizationById(organizationPartyId);
        // there might be some special uses where this is not configured or needed
        if ((organization != null) && (organization.getPartyAcctgPreference() != null)) {
            defaultOrganizationCostingMethodId = organization.getPartyAcctgPreference().getCostingMethodId();
        }
    }

    @Override
    public void tearDown() throws Exception {
        // reset some parameters back to default
        setOrganizationCostingMethodId(organizationPartyId, defaultOrganizationCostingMethodId);

        delegator = null;
        dispatcher = null;
    }

    /**
     * Gets the delegator.
     * @return a <code>Delegator</code> value
     */
    public Delegator getDelegator() {
        return delegator;
    }

    /**
     * Gets the dispatcher.
     * @return a <code>LocalDispatcher</code> value
     */
    public LocalDispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * Overload this if you use a different organization for test data.
     * @return the organization partyId to use in the tests, eg: "Company"
     */
    public String getOrganizationPartyId() {
        return OpentapsTestCase.organizationPartyId;
    }

    /**
     * Overload this if you use a different facility for test data.
     * @return the facilityId to use in the tests, eg: "WebStoreWarehouse"
     */
    public String getFacilityId() {
        return OpentapsTestCase.facilityId;
    }

    /*************************************************************************/
    /***                                                                   ***/
    /***                        Pause Function                             ***/
    /***                                                                   ***/
    /*************************************************************************/

    /**
     * Pause the execution for some time to allow timestamps to be distinct on DB that do not have subsecond timestamps (like MySQL).
     * @param reason the message to log
     * @param milliseconds the time to sleep in milliseconds
     */
    public void pause(String reason, long milliseconds) {
        try {
            Debug.logInfo("Waiting " + milliseconds + "ms : " + reason, MODULE);
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            fail("InterruptedException: " + e.toString());
        }
    }

    /**
     * Pause the execution for some time to allow timestamps to be distinct on DB that do not have subsecond timestamps (like MySQL).
     * @param reason the message to log
     */
    public void pause(String reason) {
        pause(reason, STANDARD_PAUSE_DURATION);
    }

    /*************************************************************************/
    /***                                                                   ***/
    /***                        Assert Functions                           ***/
    /***                                                                   ***/
    /*************************************************************************/

    /**
     * Asserts that two Object are not equal. This method is the opposite of assertEquals from JUnit.
     * @param message the assert message
     * @param value actual value
     * @param expected expected value that the actual value should not be equal to
     */
    public void assertNotEquals(String message, Object value, Object expected) {
        if (value.equals(expected)) {
            TestCase.fail(message + " Expected NOT [" + expected + "] but was [" + value + "].");
        }
    }

    /**
     * Asserts that two Numbers are not equal.  This method is the opposite of assertEquals.
     * @param message the assert message
     * @param value the actual value
     * @param expected the expected value that the actual value should not be equal to
     */
    public void assertNotEquals(String message, Number value, Number expected) {
        BigDecimal valueBd = asBigDecimal(value);
        BigDecimal expectedBd = asBigDecimal(expected);
        if (valueBd.compareTo(expectedBd) == 0) {
            TestCase.fail(message + " Expected NOT [" + expectedBd + "] but was [" + valueBd + "].");
        }
    }

    /**
     * Rounds the two BigDecimals according to decimals and rounding, then check if they are not equal.
     * @param message the assert message
     * @param value the actual value
     * @param expected the expected value that the actual value should not be equal to
     * @param decimals passed to <code>setScale</code>
     * @param rounding passed to <code>setScale</code>
     */
    public void assertNotEquals(String message, BigDecimal value, BigDecimal expected, int decimals, RoundingMode rounding) {
        BigDecimal valueRounded = value.setScale(decimals, rounding);
        BigDecimal expectedRounded = expected.setScale(decimals, rounding);
        assertNotEquals(message, valueRounded, expectedRounded);
    }

    /**
     * Asserts that two Numbers are equal.  This method helps test that <code>2.0 == 2.00000</code>.
     * @param message the assert message
     * @param value the actual value
     * @param expected the expected value that the actual value should be equal to
     */
    public void assertEquals(String message, Number value, Number expected) {
        BigDecimal valueBd = asBigDecimal(value);
        BigDecimal expectedBd = asBigDecimal(expected);
        if (valueBd.compareTo(expectedBd) != 0) {
            TestCase.fail(message + " Expected [" + expectedBd + "] but was [" + valueBd + "].");
        }
    }

    /**
     * Asserts two BigDecimal are equal more or less an accepted delta.
     * @param message the assert message
     * @param value the actual value
     * @param expected the expected value that the actual value should be equal to
     * @param delta accepted delta
     */
    public void assertEquals(String message, BigDecimal value, BigDecimal expected, BigDecimal delta) {
        assertEquals(message, value.doubleValue(), expected.doubleValue(), delta.doubleValue());
    }

    /**
     * Round the two BigDecimals according to decimals and rounding, then check if they are equal.
     * @param message the assert message
     * @param value the actual value
     * @param expected the expected value that the actual value should be equal to
     * @param decimals passed to <code>setScale</code>
     * @param rounding passed to <code>setScale</code>
     */
    public void assertEquals(String message, BigDecimal value, BigDecimal expected, int decimals, RoundingMode rounding) {
        BigDecimal valueRounded = value.setScale(decimals, rounding);
        BigDecimal expectedRounded = expected.setScale(decimals, rounding);
        assertEquals(message, valueRounded, expectedRounded);
    }

    /**
     * Asserts that two date times are equal.  This method ignore milliseconds as a workaround for DB that do not support milliseconds.
     * @param message the assert message
     * @param expected the expected <code>Calendar</code> value  that the actual value should be equal to
     * @param given the actual <code>Calendar</code> value
     */
    public void assertDatesEqual(String message, com.ibm.icu.util.Calendar expected, com.ibm.icu.util.Calendar given) {
        expected.set(Calendar.MILLISECOND, 0);
        given.set(Calendar.MILLISECOND, 0);
        if (expected.compareTo(given) != 0) {
            TestCase.fail(message + String.format(" Expected [%1$tc] but was [%2$tc].", expected.getTime(), given.getTime()));
        }
    }

    /**
     * Asserts that two date times are equal.  This method ignore milliseconds as a workaround for DB that do not support milliseconds.
     * @param message the assert message
     * @param expected the expected <code>Calendar</code> value  that the actual value should be equal to
     * @param given the actual <code>Calendar</code> value
     */
    public void assertDatesEqual(String message, Calendar expected, Calendar given) {
        expected.set(Calendar.MILLISECOND, 0);
        given.set(Calendar.MILLISECOND, 0);
        if (expected.compareTo(given) != 0) {
            TestCase.fail(message + String.format(" Expected [%1$tc] but was [%2$tc].", expected.getTime(), given.getTime()));
        }
    }

    /**
     * Asserts that the values in the GenericValue are equal to the given Map.
     * @param message the assert message
     * @param actual a <code>GenericValue</code> value
     * @param expected the <code>Map</code> of expected values
     */
    public void assertEquals(String message, GenericValue actual, Map<String, String> expected) {
        Debug.logInfo("Comparing GenericValue to map :\nactual = " + actual + "\nexpected = " + expected, MODULE);

        for (Object key : expected.keySet()) {
            Object expectedObj = expected.get(key);
            Object actualObj = actual.get(key);
            if (expectedObj == null) {
                assertNull(message + " for key value [" + key + "]", actualObj);
            } else {
                assertEquals(message + " for key value [" + key + "]", expectedObj, actualObj);
            }
        }
    }

    /**
     * Asserts that the values in a Map are equal to the given Map.
     * This uses <code>assertEquals</code> to compare the Map values as the default implementation uses <code>equals</code> which does not always work on <code>BigDecimal</code>.
     * @param message the assert message
     * @param actual a <code>Map</code> of values
     * @param expected the <code>Map</code> of expected values
     * @param ignoreExtraActualValues if set to <code>true</code>, does not fail if some values in the actual Map are not in the expected Map
     */
    public void assertEquals(String message, Map actual, Map expected, boolean ignoreExtraActualValues) {
        Debug.logInfo("Comparing maps :\nactual = " + actual + "\nexpected = " + expected, MODULE);

        for (Object key : expected.keySet()) {
            Object expectedObj = expected.get(key);
            Object actualObj = actual.get(key);
            if (expectedObj == null) {
                assertNull(message + " for key value [" + key + "]", actualObj);
            } else {
                if (actualObj instanceof Map || expectedObj instanceof Map) {
                    assertEquals(message + " for key value [" + key + "]", (Map) expectedObj, (Map) actualObj, ignoreExtraActualValues);
                } else if (actualObj instanceof List || expectedObj instanceof List) {
                    assertEquals(message + " for key value [" + key + "]", (List) expectedObj, (List) actualObj, ignoreExtraActualValues);
                } else if (actualObj instanceof Number || expectedObj instanceof Number) {
                    assertEquals(message + " for key value [" + key + "]", (Number) expectedObj, (Number) actualObj);
                } else {
                    assertEquals(message + " for key value [" + key + "]", expectedObj, actualObj);
                }
            }
        }
        if (!ignoreExtraActualValues) {
            assertTrue("Some keys were found in the actual Map [" + actual.keySet() + "] that were not expected [" + expected.keySet() + "].", expected.keySet().containsAll(actual.keySet()));
        }
    }

    /**
     * Asserts that the values in a List are equal to the given List.
     * This uses <code>assertEquals</code> to compare the List values as the default implementation uses <code>equals</code> which does not always work on <code>BigDecimal</code>.
     * @param message the assert message
     * @param actual a <code>List</code> of values
     * @param expected the <code>List</code> of expected values
     * @param ignoreExtraActualValues if set to <code>true</code>, does not fail if some values in actual are not in expected
     */
    @SuppressWarnings("unchecked")
    public void assertEquals(String message, List actual, List expected, boolean ignoreExtraActualValues) {
        Debug.logInfo("Comparing lists :\nactual = " + actual + "\nexpected = " + expected, MODULE);

        for (int i = 0; i < expected.size(); i++) {
            Object expectedObj = expected.get(i);
            assertTrue(message + " for index [" + i + "] expected [" + expectedObj + "] but there is no more values", i < actual.size());
            Object actualObj = actual.get(i);
            if (expectedObj == null) {
                assertNull(message + " for index [" + i + "]", actualObj);
            } else {
                if (actualObj instanceof Map || expectedObj instanceof Map) {
                    assertEquals(message + " for index [" + i + "]", (Map) expectedObj, (Map) actualObj, ignoreExtraActualValues);
                } else if (actualObj instanceof List || expectedObj instanceof List) {
                    assertEquals(message + " for index [" + i + "]", (List) expectedObj, (List) actualObj, ignoreExtraActualValues);
                } else if (actualObj instanceof Number || expectedObj instanceof Number) {
                    assertEquals(message + " for index [" + i + "]", (Number) expectedObj, (Number) actualObj);
                } else {
                    assertEquals(message + " for index [" + i + "]", expectedObj, actualObj);
                }
            }
        }

    }

    /**
     * Asserts that the values in a Map are equal to the given Map.
     * This uses <code>assertEquals</code> to compare the Map values as the default implementation uses <code>equals</code> which does not always work on <code>BigDecimal</code>.
     * @param message the assert message
     * @param actual a <code>Map</code> of values
     * @param expected the <code>Map</code> of expected values
     */
    @SuppressWarnings("unchecked")
    public void assertEquals(String message, Map actual, Map expected) {
        assertEquals(message, actual, expected, true);
    }

    /**
     * Asserts that an entity has the expected accounting tags.
     * @param value the entity to test the tags for value
     * @param expected the expected tags
     */
    @SuppressWarnings("unchecked")
    public void assertAccountingTagsEqual(GenericValue value, Map expected) {
        Map<String, String> foundTags = new HashMap<String, String>();
        UtilAccountingTags.putAllAccountingTags(value, foundTags);
        for (String tag : foundTags.keySet()) {
            assertEquals("Entity: " + value + " is not tagged properly.", expected.get(tag), value.getString(tag));
        }
    }

    /**
     * Asserts that an entity has the expected accounting tags.
     * @param value the entity to test the tags for value
     * @param expected the expected tags
     */
    public void assertAccountingTagsEqual(GenericValue value, GenericValue expected) {
        Map<String, String> foundTags = new HashMap<String, String>();
        UtilAccountingTags.putAllAccountingTags(value, foundTags);
        for (String tag : foundTags.keySet()) {
            assertEquals("Entity: " + value + " is not tagged properly.", expected.get(tag), value.getString(tag));
        }
    }

    /**
     * Asserts that an entity has the expected accounting tags.
     * @param value the entity to test the tags for value
     * @param expected the expected tags
     * @exception Exception if an error occurs
     */
    public void assertAccountingTagsEqual(EntityInterface value, GenericValue expected) throws Exception {
        assertAccountingTagsEqual(Repository.genericValueFromEntity(delegator, value), expected);
    }

    /**
     * Asserts that an entity has the expected accounting tags.
     * @param value the entity to test the tags for value
     * @param expected the expected tags
     * @exception Exception if an error occurs
     */
    public void assertAccountingTagsEqual(GenericValue value, EntityInterface expected) throws Exception {
        assertAccountingTagsEqual(value, Repository.genericValueFromEntity(expected));
    }

    /**
     * Asserts that an entity has the expected accounting tags.
     * @param value the entity to test the tags for value
     * @param expected the expected tags
     * @exception Exception if an error occurs
     */
    public void assertAccountingTagsEqual(EntityInterface value, EntityInterface expected) throws Exception {
        assertAccountingTagsEqual(Repository.genericValueFromEntity(delegator, value), Repository.genericValueFromEntity(expected));
    }

    /**
     * Asserts that an entity has the expected accounting tags.
     * @param value the entity to test the tags for value
     * @param expected the expected tags
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void assertAccountingTagsEqual(EntityInterface value, Map expected) throws Exception {
        assertAccountingTagsEqual(Repository.genericValueFromEntity(delegator, value), expected);
    }

    /**
     * Asserts that the given collection is not null, and not empty.
     * @param message the assert message
     * @param list the <code>Collection</code> to test
     */
    @SuppressWarnings("unchecked")
    public void assertNotEmpty(String message, Collection list) {
        assertNotNull(message, list);
        assertTrue(message, list.size() > 0);
    }

    /**
     * Asserts that the given collection is null or empty.
     * @param message the assert message
     * @param list the <code>Collection</code> to test
     */
    @SuppressWarnings("unchecked")
    public void assertEmpty(String message, Collection list) {
        assertTrue(message, list == null || list.isEmpty());
    }

    /**
     * Asserts if a field in the given values are equal.  A pair of fields are equal if both fields
     * are null, or if both fields are not null and are considered equal by the
     * assertEquals() assertion.  The message is automatically generated from the entity
     * names and the field names.
     * @param one first <code>GenericValue</code> value
     * @param two second <code>GenericValue</code> value
     * @param fieldName the field name to test in both entities
     */
    public void assertFieldsEqual(GenericValue one, GenericValue two, String fieldName) {
        Object fieldOne = one.get(fieldName);
        Object fieldTwo = two.get(fieldName);
        String message = one.getEntityName() + " " + makePkString(one) + " has " + fieldName + "[" + fieldOne + "], but " + two.getEntityName() + " " + makePkString(two) + " has a different " + fieldName + " [" + fieldTwo + "]";
        if (fieldOne != null && fieldTwo != null) {
            assertEquals(message, fieldTwo, fieldOne);
        } else {
            assertTrue(message, fieldOne == null && fieldTwo == null);
        }
    }

    /**
     * Runs a service synchronously and asserts that the result is a success.  If it fails or errors out, then the
     * assert message is the error message from the service.  The service is run with default timeout specified in the
     * service definition inside a new transaction.
     * @param <T> the class of <code>ServiceWrapper</code>
     * @param service a <code>ServiceWrapper</code> providing the input and that will contain the output
     */
    public <T extends ServiceWrapper> void runAndAssertServiceSuccess(T service) {
        service.putAllOutput(runAndAssertServiceSuccess(service.name(), service.inputMap()));
    }

    /**
     * Runs a service synchronously and asserts that the result is a success.  If it fails or errors out, then the
     * assert message is the error message from the service.  The service is run with default timeout specified in the
     * service definition inside a new transaction.
     * @param <T> the class of <code>ServiceWrapper</code>
     * @param service a <code>ServiceWrapper</code> providing the input and that will contain the output
     * @param transactionTimeOut specifies transaction timeout.  If 0 or less and requireNewTransaction is true, then service is run with default timeout.  Otherwise it's
     * run with user specified timeout and transaction setting
     * @param requireNewTransaction specifies if new transaction is required
     */
    public <T extends ServiceWrapper> void runAndAssertServiceSuccess(T service, int transactionTimeOut, boolean requireNewTransaction) {
        service.putAllOutput(runAndAssertServiceSuccess(service.name(), service.inputMap(), transactionTimeOut, requireNewTransaction));
    }

    /**
     * Runs a service synchronously and asserts that the result is a success.  If it fails or errors out, then the
     * assert message is the error message from the service.  The service is run with default timeout specified in the
     * service definition inside a new transaction.
     * @param serviceName name of the service to call
     * @param input the service input <code>Map</code>
     * @return the <code>Map</code> returned by the service
     */
    public Map<String, Object> runAndAssertServiceSuccess(String serviceName, Map<String, ?> input) {
        return runAndAssertServiceSuccess(serviceName, input, 0, true);
    }

    /**
     * Runs a service synchronously and asserts that the result is a success.  If it fails or errors out, then the
     * assert message is the error message from the service.  The service is run with default timeout specified in the
     * service definition inside a new transaction.
     * @param serviceName name of the service to call
     * @param input the service input <code>Map</code>
     * @param transactionTimeOut specifies transaction timeout.  If 0 or less and requireNewTransaction is true, then service is run with default timeout.  Otherwise it's
     * run with user specified timeout and transaction setting
     * @param requireNewTransaction specifies if new transaction is required
     * @return the <code>Map</code> returned by the service
     */
    public Map<String, Object> runAndAssertServiceSuccess(String serviceName, Map<String, ?> input, int transactionTimeOut, boolean requireNewTransaction) {
        Debug.logInfo("runAndAssertServiceSuccess: [" + serviceName + "] with input: " + input, MODULE);
        try {
            Map<String, Object> results = new HashMap<String, Object>();
            if ((requireNewTransaction) && (transactionTimeOut <= 0)) {
                // if a new transaction is required and transaction time out is 0 or less, than run it in the default mode of the service engine:
                // inside new transaction and with default services XML timeout
                results = dispatcher.runSync(serviceName, input);
            } else {
                // otherwise, either the user has specified no transaction or a manual timeout, then run using the user's specifications
                results = dispatcher.runSync(serviceName, input, transactionTimeOut, requireNewTransaction);
            }
            boolean success = !ServiceUtil.isError(results) && !ServiceUtil.isFailure(results);
            if (!success) {
                TestCase.fail("Expected service [" + serviceName + "] to return success, but the service returned: " + ServiceUtil.getErrorMessage(results) + "\n\tService input: " + input);
            }
            return results;
        } catch (GenericServiceException e) {
            TestCase.fail("Expected service [" + serviceName + "] to return success, but encountered a service exception.\n\tException message: " + e.getMessage() + "\n\tService input: " + input);
        }
        TestCase.fail("Reached unexpected point.");
        return null;
    }

    /**
     * Runs a service synchronously and asserts that it causes an error and rollback.  If it is not an error,
     * the assert message will be the service message.  Note that service failures are not considered errors because
     * they do not cause rollbacks.
     * @param <T> the class of <code>ServiceWrapper</code>
     * @param service a <code>ServiceWrapper</code> providing the input and that will contain the output
     */
    @SuppressWarnings("unchecked")
    public <T extends ServiceWrapper> void runAndAssertServiceError(T service) {
        Map<String, Object> results = runAndAssertServiceError(service.name(), service.inputMap());
        if (results != null) {
            service.putAllOutput(results);
        }
    }

    /**
     * Runs the service synchronously and asserts that it causes an error and rollback.  If it is not an error,
     * the assert message will be the service message.  Note that service failures are not considered errors because
     * they do not cause rollbacks.
     * @param serviceName name of the service to call
     * @param input the service input <code>Map</code>
     * @return the <code>Map</code> returned by the service
     */
    public Map<String, Object> runAndAssertServiceError(String serviceName, Map<String, ?> input) {
        Debug.logInfo("runAndAssertServiceError: [" + serviceName + "] with input: " + input, MODULE);
        Map<String, Object> results = null;
        try {
            Debug.set(Debug.ERROR, false);
            results = dispatcher.runSync(serviceName, input);
            Debug.set(Debug.ERROR, true);
            if (!ServiceUtil.isError(results)) {
                TestCase.fail("Expected service [" + serviceName + "] to return error, but service returned: " + results + "\n\tService input: " + input);
            }
        } catch (GenericServiceException e) {
            Debug.logInfo("Service " + serviceName + " returned an error, as expected. Results: " + results + "\n\tService input: " + input, MODULE);
        }
        Debug.set(Debug.ERROR, true);
        return results;
    }

    /**
     * Runs the service synchronously and asserts that it causes an error and rollback.  If it is not an error,
     * the assert message will be the service message.  Note that service failures are not considered errors because
     * they do not cause rollbacks.
     * @param <T> the class of <code>ServiceWrapper</code>
     * @param service a <code>ServiceWrapper</code> providing the input and that will contain the output
     */
    public <T extends ServiceWrapper> void runAndAssertServiceFailure(T service) {
        runAndAssertServiceFailure(service.name(), service.inputMap());
    }

    /**
     * Runs the service synchronously and asserts that it causes an error and rollback.  If it is not an error,
     * the assert message will be the service message.  Note that service failures are not considered errors because
     * they do not cause rollbacks.
     * @param serviceName name of the service to call
     * @param input the service input <code>Map</code>
     */
    public void runAndAssertServiceFailure(String serviceName, Map<String, ?> input) {
        Debug.logInfo("runAndAssertServiceError: [" + serviceName + "] with input: " + input, MODULE);
        Map<String, Object> results = null;
        try {
            Debug.set(Debug.ERROR, false);
            results = dispatcher.runSync(serviceName, input);
            Debug.set(Debug.ERROR, true);
            if (!ServiceUtil.isFailure(results)) {
                TestCase.fail("Expected service [" + serviceName + "] to return failure, but service returned: " + results + "\n\tService input: " + input);
            }
        } catch (GenericServiceException e) {
            Debug.logInfo("Service " + serviceName + " returned a failure, as expected. Results: " + results + "\n\tService input: " + input, MODULE);
        }
        Debug.set(Debug.ERROR, true);
    }

    /**
     * Returns a map of invoice item type ID to GL account ID based on the configuration for the organization.
     * @param organizationPartyId the organization to get the map for
     * @param invoiceItemTypeIds a list of invoice item types
     * @return the map of invoice item type ID to GL account ID for the given organization
     * @throws GeneralException if an error occurs
     */
    public Map<String, String> getGetInvoiceItemTypesGlAccounts(String organizationPartyId, List<String> invoiceItemTypeIds) throws GeneralException {
        Map<String, String> invoiceItemTypeGlAccounts = new HashMap<String, String>();

        if (UtilValidate.isNotEmpty(invoiceItemTypeIds)) {
            for (String invoiceItemTypeId : invoiceItemTypeIds) {
                //  first see if there is an invoice item type GL account defined specifically for this organization
                GenericValue invoiceItemTypeGlAccount = delegator.findByPrimaryKeyCache("InvoiceItemTypeGlAccount",
                                                        UtilMisc.toMap("invoiceItemTypeId", invoiceItemTypeId, "organizationPartyId", organizationPartyId));
                if (UtilValidate.isNotEmpty(invoiceItemTypeGlAccount) && UtilValidate.isNotEmpty(invoiceItemTypeGlAccount.getString("glAccountId"))) {
                    invoiceItemTypeGlAccounts.put(invoiceItemTypeId, invoiceItemTypeGlAccount.getString("glAccountId"));
                } else {
                    // look for the default GL account for this invoice item type
                    GenericValue invoiceItemType = delegator.findByPrimaryKeyCache("InvoiceItemType", UtilMisc.toMap("invoiceItemTypeId", invoiceItemTypeId));
                    invoiceItemTypeGlAccounts.put(invoiceItemTypeId, invoiceItemType.getString("defaultGlAccountId"));
                }
            }
        }

        return invoiceItemTypeGlAccounts;
    }

    /**
     * Print the differences between two <code>Map</code> of <code>String</code> => <code>BigDecimal</code> for Debug.
     * @param initialMap the initial <code>Map</code>
     * @param finalMap the final <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static void printMapDifferences(Map initialMap, Map finalMap) {

        Debug.logInfo("---------------------", MODULE);
        Debug.logInfo("printMapDifferences:", MODULE);

        String fs = "%12s %12.3f => %12.3f *** %12.3f";

        Set<Object> keys = new HashSet<Object>();
        keys.addAll(initialMap.keySet());
        keys.addAll(finalMap.keySet());

        for (Iterator<Object> iter = keys.iterator(); iter.hasNext();) {
            Object key = iter.next();

            BigDecimal initialBd = BigDecimal.ZERO;
            if (initialMap.get(key) != null) {
                initialBd = asBigDecimal(initialMap.get(key));
            }
            BigDecimal finalBd = BigDecimal.ZERO;
            if (finalMap.get(key) != null) {
                finalBd = asBigDecimal(finalMap.get(key));
            }
            BigDecimal differenceBd = finalBd.subtract(initialBd);
            Debug.logInfo(String.format(fs, key, initialBd, finalBd, differenceBd), MODULE);

        }
    }


    /**
     * For each key, asserts that the numeric difference is as expected:  finalValue - initialValue = expectedValue.
     * If a value is null, it is assumed to be zero.  The values must be either Numbers or Strings representing Numbers.
     * Pass in an optional message string to help identify the test case failure.
     * @param message the assert message
     * @param initialMap the initial <code>Map</code>
     * @param finalMap the final <code>Map</code>
     * @param expectedMap the <code>Map</code> of expected differences
     */
    @SuppressWarnings("unchecked")
    public static void assertMapDifferenceCorrect(String message, Map initialMap, Map finalMap, Map expectedMap) {
        for (Iterator iter = expectedMap.keySet().iterator(); iter.hasNext();) {
            Object key = iter.next();
            Object expectedValue = expectedMap.get(key);

            BigDecimal expectedBd = asBigDecimal(expectedValue);
            BigDecimal initialBd = BigDecimal.ZERO;
            if (initialMap.get(key) != null) {
                initialBd = asBigDecimal(initialMap.get(key));
            }
            BigDecimal finalBd = BigDecimal.ZERO;
            if (finalMap.get(key) != null) {
                finalBd = asBigDecimal(finalMap.get(key));
            }
            BigDecimal differenceBd = finalBd.subtract(initialBd);

            if (differenceBd.compareTo(expectedBd) != 0) {
                String failMessage = "Unexpected change for [" + key + "] with initial value [" + initialBd + "] and final value [" + finalBd + "]:  Expected difference of [" + expectedBd + "].  Difference is actually [" + differenceBd + "]";
                TestCase.fail((message != null ? message + " " : "") + failMessage);
            }
        }
    }

    /**
     * As above, but without an additional message.
     * @param initialMap the initial <code>Map</code>
     * @param finalMap the final <code>Map</code>
     * @param expectedMap the <code>Map</code> of expected differences
     */
    @SuppressWarnings("unchecked")
    public static void assertMapDifferenceCorrect(Map initialMap, Map finalMap, Map expectedMap) {
        assertMapDifferenceCorrect(null, initialMap, finalMap, expectedMap);
    }

    /**
     * Check that all values of the actual Map agree with the expected Map
     * note it will only check the key values in expectedMap against actualMap
     * use expectedMap of new HashMap() to check that the actualMap should be empty.
     * @param actualMap
     * @param expectedMap
     */
    public static void assertMapCorrect(Map actualMap, Map expectedMap) {
    	for (Iterator iter = expectedMap.keySet().iterator(); iter.hasNext();) {
            Object key = iter.next();
            Object actualValue = actualMap.get(key);
            Object expectedValue = expectedMap.get(key);

            BigDecimal expectedBd = asBigDecimal(expectedValue);
            BigDecimal actualBd = asBigDecimal(actualValue);

            if (actualBd.compareTo(expectedBd) != 0) {
                String failMessage = "Unexpected value of [" + actualBd + "] for [" + key + "]; was expecting [" + expectedBd + "]";
                TestCase.fail(failMessage);
            }
        }
    }

    /**
     * For each key, asserts that the numeric difference is as expected:  Sum(finalValue) - Sum(initialValue) = expectedValue.
     * If a value is null, it is assumed to be zero.  The values must be either Numbers or Strings representing Numbers.
     * Pass in an optional message string to help identify the test case failure.
     * @param message the assert message
     * @param initialList the initial <code>List</code>
     * @param finalList the final <code>List</code>
     * @param expectedBd the expected difference
     */
    @SuppressWarnings("unchecked")
    public static void assertDifferenceCorrect(String message, List initialList, List finalList, BigDecimal expectedBd) {

        BigDecimal initialBd = BigDecimal.ZERO;
        BigDecimal finalBd = BigDecimal.ZERO;

        for (Iterator iter = initialList.iterator(); iter.hasNext();) {
            initialBd.add(asBigDecimal(iter.next()));
        }

        for (Iterator iter = finalList.iterator(); iter.hasNext();) {
            finalBd.add(asBigDecimal(iter.next()));
        }

        BigDecimal differenceBd = finalBd.subtract(initialBd);
        if (differenceBd.compareTo(expectedBd) != 0) {
            String failMessage = "Unexpected change with initial value [" + initialBd + ": " + initialList + "] and final value [" + finalBd + ": " + finalList + "]:  Expected difference of [" + expectedBd + "].  Difference is actually [" + differenceBd + "]";
            TestCase.fail((message != null ? message + " " : "") + failMessage);
        }
    }

    /**
     * As above, but without an additional message.
     * @param initialList the initial <code>List</code>
     * @param finalList the final <code>List</code>
     * @param expectedBd the expected difference
     */
    @SuppressWarnings("unchecked")
    public static void assertDifferenceCorrect(List initialList, List finalList, BigDecimal expectedBd) {
        assertDifferenceCorrect(null, initialList, finalList, expectedBd);
    }


    /*************************************************************************/
    /***                                                                   ***/
    /***                        Helper Functions                           ***/
    /***                                                                   ***/
    /*************************************************************************/


    /**
     * Test if the given fields are equal between two GenericValiues.
     * A field is equal if both fields are null, or if both fields are not null
     * and are considered equals by the equals() function.
     * @param one first <code>GenericValue</code>
     * @param two second <code>GenericValue</code>
     * @param fields list of fields to compare in both entities
     * @return if the two entities have the same value for each of the fields
     */
    public boolean fieldsEqual(GenericValue one, GenericValue two, List<String> fields) {
        for (String field : fields) {
            Object fieldOne = one.get(field);
            Object fieldTwo = two.get(field);

            if (fieldOne != null && fieldTwo != null) {
                if (!fieldOne.equals(fieldTwo)) {
                    return false;
                }
            } else {
                if (!(fieldOne == null && fieldTwo == null)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Transforms a Number or String into a BigDecimal.  If passed a null, returns BigDecimal.ZERO.
     * This function will assert that the input is a Number or String and that the string can
     * be parsed into a BigDecimal.
     * @param obj an <code>Object</code>, which should be either a <code>Number</code> or a <code>String</code>, to convert into a <code>BigDecimal</code>
     * @return a <code>BigDecimal</code> value
     */
    public static BigDecimal asBigDecimal(Object obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        if (!(obj instanceof String) && !(obj instanceof Number)) {
            TestCase.fail("Object [" + obj + "] is not a Number.");
        }
        try {
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            TestCase.fail("Cannot parse [" + obj + "] into a BigDecimal.");
        }
        TestCase.fail("Reached unexpected point.");
        return null;
    }

    /**
     * Generates a string of the pk values for an entity. [WS10010] for Orders,
     * [10010, 2007-01-01 00:00:00.00] for an entity with an ID and a fromDate.
     * @param value a <code>GenericValue</code> value
     * @return the primary key as a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public String makePkString(GenericValue value) {
        StringBuffer buff = new StringBuffer("[");
        ModelEntity model = value.getModelEntity();
        for (Iterator iter = model.getPksIterator(); iter.hasNext();) {
            ModelField field = (ModelField) iter.next();
            buff.append(value.get(field.getName()));
            if (iter.hasNext()) {
                buff.append(", ");
            }
        }
        buff.append("]");
        return buff.toString();
    }

    /**
     * Asserts the given <code>BomTreeInterface</code> matches the expected component quantities.
     *
     * @param tree a <code>BomTreeInterface</code> value
     * @param expectedComponents the <code>Map</code> of expected components Id => quantities
     */
    protected void assertBomTreeCorrect(BomTreeInterface tree, Map<String, BigDecimal> expectedComponents) {
        Map<String, BomNodeInterface> treeQty = new HashMap<String, BomNodeInterface>();
        tree.sumQuantities(treeQty);
        for (String productId : expectedComponents.keySet()) {
            assertNotNull("Missing product [" + productId + "] (qty = " + expectedComponents.get(productId) + " ) node.", treeQty.get(productId));
            assertEquals("Product [" + productId + "] node quantity incorrect.", expectedComponents.get(productId), treeQty.get(productId).getQuantity());
        }
        // also check all the bom components were included in the expectedComponents map
        if (!expectedComponents.keySet().containsAll(treeQty.keySet())) {
            Set<String> extraComponents = treeQty.keySet();
            extraComponents.removeAll(expectedComponents.keySet());
            fail("The BOM Tree includes components that were not expected : " + extraComponents);
        }
    }

    /**
     * Create a Bill of Material (BOM) <code>ProductAssoc</code> where productToId is a component of productId.
     * @param productId the product for which the BOM is defined
     * @param productToId the component product
     * @param sequenceNum the BOM sequence for the component
     * @param quantity the quantity of component in the BOM
     * @param userLogin the user running the services
     */
    protected void createBOMProductAssoc(String productId, String productToId, Long sequenceNum, BigDecimal quantity, GenericValue userLogin) {
        createBOMProductAssoc(productId, productToId, null, sequenceNum, quantity, userLogin);
    }

    /**
     * Create a Bill of Material (BOM) <code>ProductAssoc</code> where productToId is a component of productId.
     * @param productId the product for which the BOM is defined
     * @param productToId the component product
     * @param routingId optional, the routing ID this BOM should be used for, use <code>null</code> for the default BOM
     * @param sequenceNum the BOM sequence for the component
     * @param quantity the quantity of component in the BOM
     * @param userLogin the user running the services
     */
    protected void createBOMProductAssoc(String productId, String productToId, String routingId, Long sequenceNum, BigDecimal quantity, GenericValue userLogin) {
        Map<String, Object> prodAssoc = UtilMisc.<String, Object>toMap("userLogin", userLogin);
        prodAssoc.put("productId", productId);
        prodAssoc.put("productIdTo", productToId);
        prodAssoc.put("productAssocTypeId", "MANUF_COMPONENT");
        prodAssoc.put("sequenceNum", sequenceNum);
        prodAssoc.put("quantity", quantity);
        // to avoid duplicate PK issues, also use a different from date in case a specific routing is given
        // this allow the same component to be given with different quantities on an alternate BOM
        if (routingId != null) {
            prodAssoc.put("specificRoutingWorkEffortId", routingId);
            prodAssoc.put("fromDate", UtilDateTime.toTimestamp("01/01/2001 12:00:00"));
        } else {
            prodAssoc.put("fromDate", UtilDateTime.toTimestamp("01/01/2000 12:00:00"));
        }

        runAndAssertServiceSuccess("createProductAssoc", prodAssoc);

        // VERY IMPORTANT: Otherwise the MRP won't work
        runAndAssertServiceSuccess("initLowLevelCode", UtilMisc.<String, Object>toMap("userLogin", userLogin));
    }

    /**
     * Creates a test Assembly routing work effort with one task for the given product.
     * Assumes the product already has a BOM setup.
     *
     * @param name a <code>String</code> to put in the created work efforts name
     * @param assembleProductId the product assembled by this routing
     * @return the created routing work effort ID
     */
    protected String createTestAssemblingRouting(String name, String assembleProductId) {
        return createTestAssemblingRouting(name, assembleProductId, new BigDecimal("300000"), new BigDecimal("600000"));
    }

    /**
     * Creates a test Assembly routing work effort with one task for the given product.
     * Assumes the product already has a BOM setup.
     *
     * @param name a <code>String</code> to put in the created work efforts name
     * @param assembleProductId the product assembled by this routing
     * @param estimatedMilliSeconds for the task setup
     * @param estimatedSetupMillis for the task setup
     * @return the created routing work effort ID
     */
    protected String createTestAssemblingRouting(String name, String assembleProductId, BigDecimal estimatedMilliSeconds, BigDecimal estimatedSetupMillis) {
        return createTestAssemblingRouting(name, assembleProductId, estimatedMilliSeconds, estimatedSetupMillis, null, null);
    }

    /**
     * Creates a test Assembly routing work effort with one task for the given product.
     * Assumes the product already has a BOM setup.
     *
     * @param name a <code>String</code> to put in the created work efforts name
     * @param assembleProductId the product assembled by this routing
     * @param estimatedMilliSeconds for the task setup
     * @param estimatedSetupMillis for the task setup
     * @param minQuantity min quantity of product
     * @param maxQuantity max quantity of product
     * @return the created routing work effort ID
     */
    protected String createTestAssemblingRouting(String name, String assembleProductId, BigDecimal estimatedMilliSeconds, BigDecimal estimatedSetupMillis, BigDecimal minQuantity, BigDecimal maxQuantity) {
        // create a product routing definition for test purposes
        Map<String, Object> productRoutingContext = UtilMisc.<String, Object>toMap("userLogin", admin);
        productRoutingContext.put("workEffortTypeId", "ROUTING");
        productRoutingContext.put("currentStatusId", "ROU_ACTIVE");

        String workEffortName = "routing for " + name;
        // work effort name is limited to 100 chars
        if (workEffortName.length() > 100) {
            workEffortName = workEffortName.substring(0, 100);
        }
        productRoutingContext.put("workEffortName", workEffortName);
        productRoutingContext.put("quantityToProduce", BigDecimal.ZERO);
        Map<String, Object> createProductRoutingResult = runAndAssertServiceSuccess("createWorkEffort", productRoutingContext);
        final String productRoutingId = (String) createProductRoutingResult.get("workEffortId");

        // create a routing task for the test product routing
        Map<String, Object> routingTaskContext = UtilMisc.<String, Object>toMap("userLogin", admin);
        routingTaskContext.put("workEffortTypeId", "ROU_TASK");
        routingTaskContext.put("workEffortPurposeTypeId", "ROU_ASSEMBLING");
        routingTaskContext.put("currentStatusId", "ROU_ACTIVE");
        workEffortName = "task for " + name;
        // work effort name is limited to 100 chars
        if (workEffortName.length() > 100) {
            workEffortName = workEffortName.substring(0, 100);
        }
        routingTaskContext.put("workEffortName", workEffortName);
        routingTaskContext.put("fixedAssetId", "WORKCENTER_COST");
        routingTaskContext.put("estimatedMilliSeconds", estimatedMilliSeconds);
        routingTaskContext.put("estimatedSetupMillis", estimatedSetupMillis);
        Map<String, Object> createRoutingTaskResult = runAndAssertServiceSuccess("createWorkEffort", routingTaskContext);
        final String routingTaskId = (String) createRoutingTaskResult.get("workEffortId");

        // create a WorkEffortAssoc between the product routing and the routing task for the test product routing
        Map<String, Object> workEffortAssocContext = UtilMisc.<String, Object>toMap("userLogin", admin);
        workEffortAssocContext.put("workEffortIdFrom", productRoutingId);
        workEffortAssocContext.put("workEffortIdTo", routingTaskId);
        workEffortAssocContext.put("workEffortAssocTypeId", "ROUTING_COMPONENT");
        workEffortAssocContext.put("sequenceNum", new Long(10));
        workEffortAssocContext.put("fromDate", UtilDateTime.nowTimestamp());
        runAndAssertServiceSuccess("createWorkEffortAssoc", workEffortAssocContext);

        // create the WorkEffortGoodStandard entity for the product to assemble with [workEffortId : 'ROUTING_COST'; workEffortGoodStdTypeId : 'ROU_PROD_TEMPLATE'; statusId : 'WEGS_CREATED']
        Map<String, Object> workEffortGoodStandardContext =
            UtilMisc.toMap(
                    "userLogin", admin,
                    "productId", assembleProductId,
                    "workEffortId", productRoutingId,
                    "workEffortGoodStdTypeId", "ROU_PROD_TEMPLATE",
                    "statusId", "WEGS_CREATED",
                    "fromDate", UtilDateTime.nowTimestamp()
            );
        if (minQuantity != null) {
            workEffortGoodStandardContext.put("minQuantity", minQuantity);
        }
        if (maxQuantity != null) {
            workEffortGoodStandardContext.put("maxQuantity", maxQuantity);
        }
        runAndAssertServiceSuccess("createWorkEffortGoodStandard", workEffortGoodStandardContext);

        return productRoutingId;
    }

    /**
     * Creates a product with the given name as FINISHED_GOOD
     * non virtual non variant and checks the operation is successful.
     * @param internalName product name, internal name and description
     * @param userLogin user that will call the service
     * @return the created Product GenericValue
     */
    protected GenericValue createTestProduct(String internalName, GenericValue userLogin) {
        return createTestProduct(internalName, "FINISHED_GOOD", new Long(0), userLogin);
    }

    /**
     * Creates a product with the given name and type,
     * non virtual non variant and checks the operation is successful.
     * @param internalName product name, internal name and description
     * @param productTypeId product type
     * @param userLogin user that will call the service
     * @return the created Product GenericValue
     */
    protected GenericValue createTestProduct(String internalName, String productTypeId, GenericValue userLogin) {
        return createTestProduct(internalName, productTypeId, new Long(0), userLogin);
    }

    /**
     * Creates a product with the given name and type,
     * non virtual non variant and checks the operation is successful.
     * @param internalName product name, internal name and description
     * @param productTypeId product type
     * @param billOfMaterialLevel BOM level
     * @param userLogin user that will call the service
     * @return the created Product GenericValue
     */
    protected GenericValue createTestProduct(String internalName, String productTypeId, Long billOfMaterialLevel, GenericValue userLogin) {
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("productTypeId", productTypeId);
        callCtxt.put("internalName", internalName);
        callCtxt.put("productName", internalName);
        callCtxt.put("description", internalName);
        callCtxt.put("isVirtual", "N");
        callCtxt.put("isVariant", "N");
        callCtxt.put("autoCreateKeywords", "N"); // disabling keyword generation on test product can save time running the tests
        callCtxt.put("requirementMethodEnumId", "PRODRQM_NONE");
        callCtxt.put("billOfMaterialLevel", billOfMaterialLevel);
        callCtxt.put("userLogin", userLogin);
        Map<String, Object> productInfo = runAndAssertServiceSuccess("createProduct", callCtxt);
        String productId = (String) productInfo.get("productId");
        assertEquals("Failed to create test product.", true, productId != null);
        try {
            return delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException: " + e.toString());
        }
        return null;
    }

    /**
     * Creates a <code>SupplierProduct</code> record for productId as "Main Supplier" with from date of now.
     * @param productId the product ID
     * @param supplierPartyId the supplier party ID
     * @param lastPrice a <code>BigDecimal</code> value
     * @param currencyUomId a <code>String</code> value
     * @param minimumOrderQuantity a <code>BigDecimal</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected void createMainSupplierForProduct(String productId, String supplierPartyId, BigDecimal lastPrice, String currencyUomId, BigDecimal minimumOrderQuantity, GenericValue userLogin) throws GenericServiceException {
        Map productSupplierContext = UtilMisc.toMap("userLogin", userLogin, "productId", productId, "partyId", supplierPartyId, "availableFromDate", UtilDateTime.nowTimestamp());
        productSupplierContext.put("supplierPrefOrderId", "10_MAIN_SUPPL");
        productSupplierContext.put("lastPrice", lastPrice);
        productSupplierContext.put("currencyUomId", currencyUomId);
        productSupplierContext.put("supplierProductId", productId);
        productSupplierContext.put("minimumOrderQuantity", minimumOrderQuantity);
        runAndAssertServiceSuccess("createSupplierProduct", productSupplierContext);
    }

    /**
     * Assign the given price as DEFAULT_PRICE to the given Product.
     * @param product product
     * @param defaultPrice default price
     * @param userLogin user that will call the service
     */
    protected void assignDefaultPrice(GenericValue product, BigDecimal defaultPrice, GenericValue userLogin) {
        assignDefaultPrice(product, defaultPrice, "_NA_", userLogin);
    }

    /**
     * Assign the given price as DEFAULT_PRICE to the given Product.
     * @param product product
     * @param defaultPrice default price
     * @param productStoreGroupId product store group
     * @param userLogin user that will call the service
     */
    protected void assignDefaultPrice(GenericValue product, BigDecimal defaultPrice, String productStoreGroupId, GenericValue userLogin) {
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", userLogin);
        callCtxt.put("productId", product.getString("productId"));
        callCtxt.put("productPriceTypeId", "DEFAULT_PRICE");
        callCtxt.put("productPricePurposeId", "PURCHASE");
        callCtxt.put("currencyUomId", "USD");
        callCtxt.put("fromDate", UtilDateTime.nowTimestamp());
        callCtxt.put("price", defaultPrice);
        callCtxt.put("productStoreGroupId", productStoreGroupId);
        runAndAssertServiceSuccess("createProductPrice", callCtxt);
    }

    /**
     * Receive the given quantity of the given Product in the test facility.
     * The unit cost is defaulted to <code>0.1</code>.
     * @param product product to receive
     * @param quantity quantity to receive
     * @param inventoryItemTypeId type of inventory to receive
     * @param userLogin user that will call the service
     * @return the result <code>Map</code> from the <code>receiveInventoryProduct</code> service
     */
    protected Map<String, Object> receiveInventoryProduct(GenericValue product, BigDecimal quantity, String inventoryItemTypeId, GenericValue userLogin) {
        return receiveInventoryProduct(product, quantity, inventoryItemTypeId, new BigDecimal("0.1"), userLogin);
    }

    /**
     * Receive the given quantity of the given Product in the test facility.
     * @param product product to receive
     * @param quantity quantity to receive
     * @param inventoryItemTypeId type of inventory to receive
     * @param unitCost unit cost of the received product
     * @param userLogin user that will call the service
     * @return the result <code>Map</code> from the <code>receiveInventoryProduct</code> service
     */
    protected Map<String, Object> receiveInventoryProduct(GenericValue product, BigDecimal quantity, String inventoryItemTypeId, BigDecimal unitCost, GenericValue userLogin) {
        return receiveInventoryProduct(product, quantity, inventoryItemTypeId, unitCost, getFacilityId(), userLogin);
    }

    /**
     * Receive the given quantity of the given Product in the given facility.
     * @param product product to receive
     * @param quantity quantity to receive
     * @param inventoryItemTypeId type of inventory to receive
     * @param unitCost unit cost of the received product
     * @param facilityId facility in which to received the product
     * @param userLogin user that will call the service
     * @return the result <code>Map</code> from the <code>receiveInventoryProduct</code> service
     */
    protected Map<String, Object> receiveInventoryProduct(GenericValue product, BigDecimal quantity, String inventoryItemTypeId, BigDecimal unitCost, String facilityId, GenericValue userLogin) {
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("productId", product.getString("productId"));
        callCtxt.put("facilityId", facilityId);
        callCtxt.put("currencyUomId", "USD");
        callCtxt.put("datetimeReceived", UtilDateTime.nowTimestamp());
        callCtxt.put("quantityRejected", BigDecimal.ZERO);
        callCtxt.put("inventoryItemTypeId", inventoryItemTypeId);
        callCtxt.put("unitCost", unitCost);
        callCtxt.put("quantityAccepted", quantity);
        callCtxt.put("userLogin", userLogin);
        return runAndAssertServiceSuccess("receiveInventoryProduct", callCtxt);
    }

    /**
     * Receive the given quantity of the given Product in the given facility, with the specified accounting tags.
     * @param product product to receive
     * @param quantity quantity to receive
     * @param inventoryItemTypeId type of inventory to receive
     * @param unitCost unit cost of the received product
     * @param facilityId facility in which to received the product
     * @param tags the Map of accounting tags
     * @param userLogin user that will call the service
     * @return the result <code>Map</code> from the <code>receiveInventoryProduct</code> service
     */
    protected Map<String, Object> receiveInventoryProduct(GenericValue product, BigDecimal quantity, String inventoryItemTypeId, BigDecimal unitCost, String facilityId, Map<String, String> tags, GenericValue userLogin) {
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("productId", product.getString("productId"));
        callCtxt.put("facilityId", facilityId);
        callCtxt.put("currencyUomId", "USD");
        callCtxt.put("datetimeReceived", UtilDateTime.nowTimestamp());
        callCtxt.put("quantityRejected", BigDecimal.ZERO);
        callCtxt.put("inventoryItemTypeId", inventoryItemTypeId);
        callCtxt.put("unitCost", unitCost);
        callCtxt.put("quantityAccepted", quantity);
        callCtxt.put("userLogin", userLogin);
        callCtxt.putAll(tags);
        return runAndAssertServiceSuccess("receiveInventoryProduct", callCtxt);
    }

    /**
     * Gets a Product availability using the getProductInventoryAvailable service.
     * @param productId a string that represents the product ID
     * @return the availability Map containing availableToPromiseTotal and quantityOnHandTotal
     */
    protected Map<String, Object> getProductAvailability(final String productId) {
        assertNotNull("User is required and should not be null", User);

        Map<String, Object> retval = new HashMap<String, Object>();

        try {
            InventoryServiceInterface inventoryService = getInventoryService(User);

            // invoke the service
            inventoryService.setProductId(productId);
            inventoryService.setUseCache(false);
            inventoryService.getProductInventoryAvailable();

            // set the result
            retval.put("availableToPromiseTotal", inventoryService.getAvailableToPromiseTotal());
            retval.put("quantityOnHandTotal", inventoryService.getQuantityOnHandTotal());

            Debug.logInfo("getProductInventoryAvailable for product [" + productId + "] : " + retval, MODULE);
            return retval;
        } catch (FoundationException fe) {
            assertTrue("FoundationException:" + fe.toString(), false);
            return null;
        }
    }

    /**
     * Gets the pending requirements for the given product.
     * Used to check the variation after an order.
     * @param product
     * @param productStoreId
     * @return the quantity pending
     */
    protected BigDecimal getPendingRequirements(GenericValue product, String productStoreId) {
        // calculate the sum of pending requirements in the chosen productstore for the given product
        BigDecimal pendingRequirements;
        try {
            pendingRequirements = UtilProduct.countOpenRequirements((String) product.get("productId"), productStoreId, delegator);
        } catch (GenericEntityException e) {
            assertTrue("GenericEntityException:" + e.toString(), false);
            return null;
        }
        Debug.logInfo("Requirements of [" + product.get("productId") + "] : " + pendingRequirements, MODULE);
        return pendingRequirements;
    }

    /**
     * Check that the pending requirements after the order is consistent with the initial requirements, the
     * initial ATP and the quantity ordered.
     * @param product the product to check the requirements for
     * @param orderedQty the ordered quantity of the product, used to calculate the expected requirement quantity
     * @param initialRequirement the initial requirement quantity
     * @param initialAtp the initial ATP
     * @param productStoreId the product store, used to find the related facilities in which the requirements are expected
     */
    protected void checkRequirements(GenericValue product, BigDecimal orderedQty, BigDecimal initialRequirement, BigDecimal initialAtp, String productStoreId) {
        BigDecimal finalRequirement = getPendingRequirements(product, productStoreId);
        String requirementMethodEnumId = (String) product.get("requirementMethodEnumId");
        // product with auto requirement set get a requirement created for each item ordered
        if ("PRODRQM_AUTO".equals(requirementMethodEnumId)) {
            assertEquals("Requirements for product [" + product.get("productId") + "]", orderedQty, (finalRequirement.subtract(initialRequirement)));
        } else if ("PRODRQM_ATP".equals(requirementMethodEnumId)) { // product with ATP requirement method get requirements created according to the rule defined in ProductFacility
            try {
                GenericValue productStore =  delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
                String facilityId = productStore.getString("inventoryFacilityId");
                GenericValue productFacility = delegator.findByPrimaryKey("ProductFacility", UtilMisc.toMap("facilityId", facilityId, "productId", product.get("productId")));
                assertTrue("Product [" + product.get("productId") + "] does not have ProductFacility.minimumStock for facility [" + facilityId + "]", productFacility != null && productFacility.get("minimumStock") != null);
                BigDecimal minimumStock = productFacility.getBigDecimal("minimumStock");
                BigDecimal stock = initialRequirement.add(initialAtp);
                // the requirement created is for the minimum of (quantity ordered | quantity to make ATP go up to minimumStock)
                // so this is not always the quantity to make ATP go up to minimumStock
                BigDecimal shortfall = minimumStock.subtract(stock.subtract(orderedQty));
                BigDecimal required = shortfall.min(orderedQty).max(BigDecimal.ZERO); // do not allow negative requirements which make no sense
                Debug.logInfo("checkRequirements for product [" + product.get("productId") + "], minimumStock = " + minimumStock + ", orderedQty = " + orderedQty + ", stock = " + stock + ", ====> shortfall = " + shortfall + " -------- makes required = " + required, MODULE);
                assertEquals("Requirements for product [" + product.get("productId") + "]", required, (finalRequirement.subtract(initialRequirement)));
            } catch (GenericEntityException e) {
                assertTrue("GenericEntityException:" + e.toString(), false);
            }
        } else if ("PRODRQM_NONE".equals(requirementMethodEnumId)) {
            // currently nothing to do
        } else {
            // else, no rule for this product?
            Debug.logInfo("Product [" + product.get("productId") + "] requirementMethodEnumId : " + requirementMethodEnumId, MODULE);
        }
    }

    /**
     * Creates an order for the given Customer with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order
     * @param customer
     * @param productStoreId
     * @return the SalesOrderFactory object
     */
    protected SalesOrderFactory testCreatesSalesOrder(Map<GenericValue, BigDecimal> order, GenericValue customer, String productStoreId) {
        return testCreatesSalesOrder(order, customer, productStoreId, null);
    }

    /**
     * Creates an order for the given Customer with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order
     * @param customer
     * @param productStoreId
     * @param shipByDate
     * @return the SalesOrderFactory object
     */
    protected SalesOrderFactory testCreatesSalesOrder(Map<GenericValue, BigDecimal> order, GenericValue customer, String productStoreId, Timestamp shipByDate) {
        return testCreatesSalesOrder(order, customer, productStoreId, shipByDate, "EXT_OFFLINE", null, null);
    }

    /**
     * Creates an order for the given Customer with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order
     * @param customer
     * @param productStoreId
     * @param paymentMethodTypeId optional, defaults to EXT_OFFLINE if null
     * @param shippingAddressId the Id of the contactMech that should be used as shipping address, optional
     * @return the SalesOrderFactory object
     */
    protected SalesOrderFactory testCreatesSalesOrder(Map<GenericValue, BigDecimal> order, GenericValue customer, String productStoreId, String paymentMethodTypeId, String shippingAddressId) {
        return testCreatesSalesOrder(order, customer, productStoreId, null, paymentMethodTypeId, null, shippingAddressId);
    }

    /**
     * Creates an order for the given Customer with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order
     * @param customer
     * @param productStoreId
     * @param shipByDate
     * @param paymentMethodTypeId optional, defaults to EXT_OFFLINE if null
     * @param shippingAddressId the Id of the contactMech that should be used as shipping address, optional
     * @return the SalesOrderFactory object
     */
    protected SalesOrderFactory testCreatesSalesOrder(Map<GenericValue, BigDecimal> order, GenericValue customer, String productStoreId, Timestamp shipByDate, String paymentMethodTypeId, String shippingAddressId) {
        return testCreatesSalesOrder(order, customer, productStoreId, shipByDate, paymentMethodTypeId, null, shippingAddressId);
    }

    /**
     * Creates an order for the given Customer partyId with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order a <code>Map</code> or product <code>GenericValue</code> to their quantity to order
     * @param customer the customer
     * @param productStoreId the product store id
     * @param paymentMethodTypeId optional, defaults to EXT_OFFLINE if null
     * @param paymentMethodId optional, defaults to null
     * @param shippingAddressId the Id of the contactMech that should be used as shipping address, optional
     * @return the <code>SalesOrderFactory</code> object
     */
    protected SalesOrderFactory testCreatesSalesOrder(Map<GenericValue, BigDecimal> order, GenericValue customer, String productStoreId, String paymentMethodTypeId, String paymentMethodId, String shippingAddressId) {
        return testCreatesSalesOrder(order, customer.getString("partyId"), productStoreId, null, paymentMethodTypeId, paymentMethodId, shippingAddressId);
    }

    /**
     * Creates an order for the given Customer partyId with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order a <code>Map</code> or product <code>GenericValue</code> to their quantity to order
     * @param customer the customer
     * @param productStoreId the product store id
     * @param shipByDate
     * @param paymentMethodTypeId optional, defaults to EXT_OFFLINE if null
     * @param paymentMethodId optional, defaults to null
     * @param shippingAddressId the Id of the contactMech that should be used as shipping address, optional
     * @return the <code>SalesOrderFactory</code> object
     */
    protected SalesOrderFactory testCreatesSalesOrder(Map<GenericValue, BigDecimal> order, GenericValue customer, String productStoreId, Timestamp shipByDate, String paymentMethodTypeId, String paymentMethodId, String shippingAddressId) {
        return testCreatesSalesOrder(order, customer.getString("partyId"), productStoreId, shipByDate, paymentMethodTypeId, paymentMethodId, shippingAddressId);
    }

    /**
     * Creates an order for the given Customer partyId with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order a <code>Map</code> or product <code>GenericValue</code> to their quantity to order
     * @param customerPartyId the customer party id
     * @param productStoreId the product store id
     * @param shipByDate
     * @param paymentMethodTypeId optional, defaults to EXT_OFFLINE if null
     * @param paymentMethodId optional, defaults to null
     * @param shippingAddressId the Id of the contactMech that should be used as shipping address, optional
     * @return the <code>SalesOrderFactory</code> object
     */
    protected SalesOrderFactory testCreatesSalesOrder(Map<GenericValue, BigDecimal> order, String customerPartyId, String productStoreId, String paymentMethodTypeId, String paymentMethodId, String shippingAddressId) {
        return testCreatesSalesOrder(order, customerPartyId, productStoreId, null, paymentMethodTypeId, paymentMethodId, shippingAddressId);
    }

    /**
     * Creates an order for the given Customer partyId with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order a <code>Map</code> or product <code>GenericValue</code> to their quantity to order
     * @param customerPartyId the customer party id
     * @param productStoreId the product store id
     * @param shipByDate
     * @param paymentMethodTypeId optional, defaults to EXT_OFFLINE if null
     * @param paymentMethodId optional, defaults to null
     * @param shippingAddressId the Id of the contactMech that should be used as shipping address, optional
     * @return the <code>SalesOrderFactory</code> object
     */
    protected SalesOrderFactory testCreatesSalesOrder(Map<GenericValue, BigDecimal> order, String customerPartyId, String productStoreId, Timestamp shipByDate, String paymentMethodTypeId, String paymentMethodId, String shippingAddressId) {
    	return testCreatesSalesOrder(order, customerPartyId, productStoreId, shipByDate, paymentMethodTypeId, paymentMethodId, shippingAddressId, null);
    }

    /**
     * Creates an order for the given Customer partyId with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order a <code>Map</code> or product <code>GenericValue</code> to their quantity to order
     * @param customerPartyId the customer party id
     * @param productStoreId the product store id
     * @param shipByDate
     * @param paymentMethodTypeId optional, defaults to EXT_OFFLINE if null
     * @param paymentMethodId optional, defaults to null
     * @param shippingAddressId the Id of the contactMech that should be used as shipping address, optional
     * @return the <code>SalesOrderFactory</code> object
     */
    protected SalesOrderFactory testCreatesSalesOrder(Map<GenericValue, BigDecimal> order, String customerPartyId, String productStoreId, Timestamp shipByDate, String paymentMethodTypeId, String paymentMethodId, String shippingAddressId, String billingAddressId) {
        // to store ATP, QOH and requirements before and after the order
        Map<GenericValue, BigDecimal> productAtpInitial, productQohInitial, productAtpFinal, productQohFinal, productRequirementInitial;
        productAtpInitial = new HashMap<GenericValue, BigDecimal>();
        productQohInitial = new HashMap<GenericValue, BigDecimal>();
        productAtpFinal = new HashMap<GenericValue, BigDecimal>();
        productQohFinal = new HashMap<GenericValue, BigDecimal>();
        productRequirementInitial = new HashMap<GenericValue, BigDecimal>();
        // store service call results
        Map<String, Object> callResults;

        SalesOrderFactory sof = null;
        EtmPoint point = null;

        point = etmMonitor.createPoint(MODULE + ":SalesOrderFactory");
        try {
            // this throws an exception if it cannot find the ProductStore
            sof = new SalesOrderFactory(delegator, dispatcher, User, getOrganizationPartyId(), customerPartyId, productStoreId);
        } catch (GenericEntityException e) {
            assertTrue("GenericEntityException:" + e.toString(), false);
        } finally {
            point.collect();
        }

        if (shipByDate != null) {
            sof.setShipByDate(shipByDate);
        }

        Debug.logInfo("Currency as defined by the productStore is: " + sof.getCurrencyUomId(), MODULE);

        if (UtilValidate.isEmpty(paymentMethodTypeId)) {
            paymentMethodTypeId = "EXT_OFFLINE";
        }
        point = etmMonitor.createPoint(MODULE + ":SalesOrderFactory.addPaymentMethod");
        sof.addPaymentMethod(paymentMethodTypeId, paymentMethodId);
        point.collect();

        point = etmMonitor.createPoint(MODULE + ":SalesOrderFactory.addShippingGroup");
        if (shippingAddressId == null) {
            sof.addShippingGroup("_NA_", "STANDARD");
        } else {
            sof.addShippingGroup("_NA_", "STANDARD", shippingAddressId);
            sof.addOrderContactMech(shippingAddressId, "SHIPPING_LOCATION");
        }
        if (billingAddressId != null) {
            sof.addOrderContactMech(billingAddressId, "BILLING_LOCATION");
        }
        point.collect();

        for (Iterator<GenericValue> iter = order.keySet().iterator(); iter.hasNext();) {
            GenericValue product = iter.next();
            // get initial products availability
            callResults = getProductAvailability(product.getString("productId"));
            productAtpInitial.put(product, (BigDecimal) callResults.get("availableToPromiseTotal"));
            productQohInitial.put(product, (BigDecimal) callResults.get("quantityOnHandTotal"));
            Debug.logInfo("Initial availability of [" + product.get("productId") + "] : ATP=" + productAtpInitial.get(product) + " QOH=" + productQohInitial.get(product), MODULE);
            // get initial requirements
            productRequirementInitial.put(product, getPendingRequirements(product, productStoreId));
            // add product to the order
            point = etmMonitor.createPoint(MODULE + ":SalesOrderFactory.addShippingGroup");
            try {
                sof.addProduct(product, order.get(product));
            } catch (GenericServiceException e) {
                assertTrue("GenericServiceException:" + e.toString(), false);
            } finally {
                point.collect();
            }
        }

        // create the order, and approve it
        point = etmMonitor.createPoint(MODULE + ":SalesOrderFactory.StoreOrder");
        try {
            String orderId = sof.storeOrder();
            Debug.logInfo("Created order [" + orderId + "]", MODULE);
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            assertNotNull("Could not retrieve order with id [" + orderId + "]", orderHeader);
            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            String billFromPartyId = orderHeader.getString("billFromPartyId");
            assertEquals("Value billFromPartyId of order [" + orderId + "] is wrong.", orh.getBillFromParty().getString("partyId"), billFromPartyId);
            String billToPartyId = orderHeader.getString("billToPartyId");
            assertEquals("Value billToPartyId of order [" + orderId + "] is wrong.", orh.getBillToParty().getString("partyId"), billToPartyId);

            sof.approveOrder();
        } catch (GenericServiceException gse) {
            assertTrue("GenericServiceException:" + gse.toString(), false);
        } catch (GenericEntityException gee) {
            assertTrue("GenericEntityException:" + gee.toString(), false);
        } finally {
            point.collect();
        }

        for (Iterator<GenericValue> iter = order.keySet().iterator(); iter.hasNext();) {
            GenericValue product = iter.next();
            try {
                if (!UtilProduct.isPhysical(product)) {
                    continue;
                }
            } catch (GenericEntityException e) {
                assertTrue("GenericEntityException:" + e.toString(), false);
            }
            // get final products availability
            callResults = getProductAvailability(product.getString("productId"));
            productAtpFinal.put(product, (BigDecimal) callResults.get("availableToPromiseTotal"));
            productQohFinal.put(product, (BigDecimal) callResults.get("quantityOnHandTotal"));
            Debug.logInfo("Final availability of [" + product.get("productId") + "] : ATP=" + productAtpFinal.get(product) + " QOH=" + productQohFinal.get(product), MODULE);
            // check QOH are unchanged
            assertEquals("QOH of " + product.get("productId"), productQohFinal.get(product), productQohInitial.get(product));
            // check ATP have declined by the ordered qty
            BigDecimal expectedAtp = productAtpInitial.get(product).subtract(order.get(product));
            assertEquals("ATP of " + product.get("productId"), productAtpFinal.get(product), expectedAtp);
            // check requirements have been created as needed
            checkRequirements(product, order.get(product), productRequirementInitial.get(product), productAtpInitial.get(product), productStoreId);
        }

        return sof;
    }


    /**
     * Creates an order for the given Supplier with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order
     * @param supplier
     * @param toFacilityContactMechId the Id of the contactMech that should be used as shipping address
     * @return the PurchaseOrderFactory object
     */
    protected PurchaseOrderFactory testCreatesPurchaseOrder(Map<GenericValue, BigDecimal> order, GenericValue supplier, String toFacilityContactMechId) {
        return testCreatesPurchaseOrder(order, supplier, toFacilityContactMechId, "EXT_OFFLINE", null);
    }

    /**
     * Creates an order for the given Supplier with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order
     * @param supplier
     * @param toFacilityContactMechId the Id of the contactMech that should be used as shipping address
     * @param paymentMethodTypeId optional, defaults to EXT_OFFLINE if null
     * @param shippingAddressId the Id of the contactMech that should be used as shipping address, optional
     * @return the PurchaseOrderFactory object
     */
    protected PurchaseOrderFactory testCreatesPurchaseOrder(Map<GenericValue, BigDecimal> order, GenericValue supplier, String toFacilityContactMechId, String paymentMethodTypeId) {
        return testCreatesPurchaseOrder(order, supplier, toFacilityContactMechId, paymentMethodTypeId, null);
    }

    /**
     * Creates an order for the given Supplier with the given products.
     *  Checks that ATP and QOH have declined as needed.
     *  Checks that Requirements have been created as needed.
     * @param order
     * @param supplier
     * @param toFacilityContactMechId the Id of the contactMech that should be used as shipping address
     * @param paymentMethodTypeId optional, defaults to EXT_OFFLINE if null
     * @param paymentMethodId optional, defaults to null
     * @return the PurchaseOrderFactory object
     */
    protected PurchaseOrderFactory testCreatesPurchaseOrder(Map<GenericValue, BigDecimal> order, GenericValue supplier, String toFacilityContactMechId,
            String paymentMethodTypeId, String paymentMethodId) {


        PurchaseOrderFactory pof = null;
        EtmPoint point = null;

        point = etmMonitor.createPoint(MODULE + ":PurchaseOrderFactory");
        try {
            // this throws an exception if it cannot find the ProductStore
            pof = new PurchaseOrderFactory(delegator, dispatcher, User, (String) supplier.get("partyId"), getOrganizationPartyId(), toFacilityContactMechId);
            // unlike the Sales Order, the PO store doesn't have a default currency set, setting manually
            pof.setCurrencyUomId("USD");
        } catch (GenericEntityException e) {
            assertTrue("GenericEntityException:" + e.toString(), false);
        } finally {
            point.collect();
        }

        Debug.logInfo("Currency as defined by the productStore is: " + pof.getCurrencyUomId(), MODULE);

        if (UtilValidate.isEmpty(paymentMethodTypeId)) {
            paymentMethodTypeId = "EXT_OFFLINE";
        }
        point = etmMonitor.createPoint(MODULE + ":PurchaseOrderFactory.addPaymentMethod");
        pof.addPaymentMethod(paymentMethodTypeId, paymentMethodId);
        point.collect();

        point = etmMonitor.createPoint(MODULE + ":PurchaseOrderFactory.addShippingGroup");
        pof.addShippingGroup("UPS", "NEXT_DAY");
        point.collect();

        for (Iterator<GenericValue> iter = order.keySet().iterator(); iter.hasNext();) {
            GenericValue product = iter.next();
            // add product to the order
            point = etmMonitor.createPoint(MODULE + ":PurchaseOrderFactory.addShippingGroup");
            try {
                pof.addProduct(product, order.get(product));
            } catch (GenericServiceException e) {
                assertTrue("GenericServiceException:" + e.toString(), false);
            } finally {
                point.collect();
            }
        }

        // create the order
        point = etmMonitor.createPoint(MODULE + ":PurchaseOrderFactory.StoreOrder");
        try {
            String orderId = pof.storeOrder();
            Debug.logInfo("Created order [" + orderId + "]", MODULE);

            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            String billFromPartyId = orderHeader.getString("billFromPartyId");
            assertEquals("Value billFromPartyId of order [" + orderId + "] is wrong.", orh.getBillFromParty().getString("partyId"), billFromPartyId);
            String billToPartyId = orderHeader.getString("billToPartyId");
            assertEquals("Value billToPartyId of order [" + orderId + "] is wrong.", orh.getBillToParty().getString("partyId"), billToPartyId);

        } catch (GenericServiceException gse) {
            assertTrue("GenericServiceException:" + gse.toString(), false);
        } catch (GenericEntityException gee) {
            assertTrue("GenericEntityException:" + gee.toString(), false);
        } finally {
            point.collect();
        }

        return pof;

    }

    /**
     * Method to simulate the edit/add order item form's call to updateOrderItem service.
     * @param orderId
     * @param orderItemSeqId
     * @param newQuantity
     * @param newUnitPrice set to null if you don't want to change the price
     * @param newDescription
     * orderItemSeqIds should override price.
     * @param userLogin
     * @throws GeneralException
     */
    protected void updateOrderItem(String orderId, String orderItemSeqId, String newQuantity, String newUnitPrice, String newDescription, GenericValue userLogin) throws GeneralException {
        updateOrderItem(orderId, orderItemSeqId, 1, newQuantity, newUnitPrice, newDescription, userLogin);
    }

    /**
     * Method to simulate the edit/add order item form's call to updateOrderItem service.
     * @param orderId
     * @param orderItemSeqId
     * @param groupIdx the index of the shipping group to update, default to 1 (the first group)
     * @param newQuantity
     * @param newUnitPrice set to null if you don't want to change the price
     * @param newDescription
     * orderItemSeqIds should override price.
     * @param userLogin
     * @throws GeneralException
     */
    protected void updateOrderItem(String orderId, String orderItemSeqId, int groupIdx, String newQuantity, String newUnitPrice, String newDescription, GenericValue userLogin) throws GeneralException {
        // these parameters match the web form for update order item
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", userLogin);
        callCtxt.put("orderId", orderId);
        callCtxt.put("itemDescriptionMap", UtilMisc.toMap(orderItemSeqId, newDescription));
        callCtxt.put("itemQtyMap", UtilMisc.toMap(orderItemSeqId + ":" + groupIdx, newQuantity));
        callCtxt.put("itemPriceMap", UtilMisc.toMap(orderItemSeqId, newUnitPrice));
        if (newUnitPrice != null) {
            // this is a Map of orderItemSeqId checkboxes, so "Y" denotes update this price
            callCtxt.put("overridePriceMap", UtilMisc.toMap(orderItemSeqId, "Y"));
        } else {
            // this is a required parameter, so we need to supply an empty Map
            callCtxt.put("overridePriceMap", new HashMap());
        }
        runAndAssertServiceSuccess("updateOrderItems", callCtxt);
    }

    /**
     * Method to simulate the edit/add order item form's call to updateOrderItem service.
     * @param orderId
     * @param orderItemSeqId
     * @param newQuantity
     * @param newUnitPrice set to null if you don't want to change the price
     * @param newDescription
     * orderItemSeqIds should override price.
     * @param userLogin
     * @throws GeneralException is exception occur
     */
    protected void updatePurchaseOrderItem(String orderId, String orderItemSeqId, String newQuantity, String newUnitPrice, String newDescription, GenericValue userLogin) throws GeneralException {
        updatePurchaseOrderItem(orderId, orderItemSeqId, 1, newQuantity, newUnitPrice, newDescription, userLogin);
    }

    /**
     * Method to simulate the edit/add order item form's call to updateOrderItem service.
     * @param orderId
     * @param orderItemSeqId
     * @param groupIdx the index of the shipping group to update, default to 1 (the first group)
     * @param newQuantity
     * @param newUnitPrice set to null if you don't want to change the price
     * @param newDescription
     * orderItemSeqIds should override price.
     * @param userLogin
     * @throws GeneralException is exception occur
     */
    protected void updatePurchaseOrderItem(String orderId, String orderItemSeqId, int groupIdx, String newQuantity, String newUnitPrice, String newDescription, GenericValue userLogin) throws GeneralException {
        // these parameters match the web form for update order item
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", userLogin);
        callCtxt.put("orderId", orderId);
        callCtxt.put("itemDescriptionMap", UtilMisc.toMap(orderItemSeqId, newDescription));
        callCtxt.put("itemQtyMap", UtilMisc.toMap(orderItemSeqId + ":" + groupIdx, newQuantity));
        callCtxt.put("itemPriceMap", UtilMisc.toMap(orderItemSeqId, newUnitPrice));
        if (newUnitPrice != null) {
            // this is a Map of orderItemSeqId checkboxes, so "Y" denotes update this price
            callCtxt.put("overridePriceMap", UtilMisc.toMap(orderItemSeqId, "Y"));
        } else {
            // this is a required parameter, so we need to supply an empty Map
            callCtxt.put("overridePriceMap", new HashMap());
        }
        runAndAssertServiceSuccess("updatePurchaseOrderItems", callCtxt);
    }

    /**
     * Convenience method to cancel an order item.
     * @param orderId
     * @param orderItemSeqId
     * @param shipGroupSeqId
     * @param quantityToCancel
     * @param userLogin
     * @throws GeneralException
     */
    protected void cancelOrderItem(String orderId, String orderItemSeqId, String shipGroupSeqId, BigDecimal quantityToCancel, GenericValue userLogin) throws GeneralException {
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", userLogin);
        callCtxt.put("orderId", orderId);
        callCtxt.put("orderItemSeqId", orderItemSeqId);
        callCtxt.put("shipGroupSeqId", shipGroupSeqId);
        callCtxt.put("cancelQuantity", quantityToCancel);
        runAndAssertServiceSuccess("cancelOrderItem", callCtxt);

    }

    /**
     * Same as below, except receives full quantity of all order items and force complete the items.
     */
    protected Map<String, Object> createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(final GenericValue purchaseOrderHeader, final GenericValue userLogin) throws GeneralException {
        return createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(purchaseOrderHeader, null, true, userLogin);
    }

    /**
     * Creates a input parameter map that will be used to call the
     * warehouse.issueOrderItemToShipmentAndReceiveAgainstPO service.
     * The parameter map created does not have any lot and shipment IDs associated.
     * 1) accepted quantities that are the same as ordered quantities
     * 2) no rejected quantities
     * 3) no lots and unit in lots
     * 4) non serial inventory item type for all the received items
     * 5) no existing shipment
     * 6) "WebStoreWarehouse facility
     * 7) flag to complete the purchase order
     * @param purchaseOrderHeader
     * @param quantitiesToReceive a Map of orderItemSeqId to String of quantity to receive
     * @param forceComplete denotes whether the PO items should be completed, with unreceived quantities cancelled
     * @param userLogin a <code>GenericValue</code> value
     * @return the <code>Map</code> object that contains the result
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(final GenericValue purchaseOrderHeader, Map<String, String> quantitiesToReceive, boolean forceComplete, final GenericValue userLogin) throws GeneralException {
        Map<String, Object> retval = new HashMap<String, Object>();

        OrderReadHelper orh = new OrderReadHelper(purchaseOrderHeader);
        List<GenericValue> orderItems = orh.getOrderItems();

        Map<String, String> orderItemSeqIds = new HashMap<String, String>();
        Map<String, String> productIds = new HashMap<String, String>();
        Map<String, String> quantitiesAccepted = new HashMap<String, String>();
        Map<String, String> quantitiesRejected = new HashMap<String, String>();
        Map<String, String> unitCosts = new HashMap<String, String>();
        Map<String, String> lotIds = new HashMap<String, String>();
        Map<String, String> inventoryItemTypeIds = new HashMap<String, String>();
        Map<String, String> rowSubmit = new HashMap<String, String>();

        ProductRepositoryInterface productRepository = productDomain.getProductRepository();
        OrganizationRepositoryInterface orgRepository = organizationDomain.getOrganizationRepository();
        OrderRepositoryInterface orderRepository = orderDomain.getOrderRepository();

        Order order = orderRepository.getOrderById(orh.getOrderId());
        String orderCurrencyUomId = order.getCurrencyUom();
        Organization org = orgRepository.getOrganizationById(order.getOrganizationParty().getPartyId());
        String orgCurrencyUomId = org.getPartyAcctgPreference().getBaseCurrencyUomId();

        // create parameters
        if (orderItems != null && !orderItems.isEmpty()) {
            int rowNumber = 0;
            for (GenericValue orderItem : orderItems) {
                // if there is a quantitiesToReceive Map, then skip the order items not in this map, so they don't get received
                if ((quantitiesToReceive != null) && (quantitiesToReceive.get(orderItem.getString("orderItemSeqId")) == null)) {
                    Debug.logInfo("Item [" + orderItem.getString("orderItemSeqId") + "] so it will not be in the test receiving", MODULE);
                    continue;
                }

                String strRowNumber = Integer.toString(rowNumber);

                String orderItemSeqId = orderItem.getString("orderItemSeqId");
                orderItemSeqIds.put(strRowNumber, orderItemSeqId);

                String productId = orderItem.getString("productId");
                productIds.put(strRowNumber, productId);

                if (quantitiesToReceive != null) {
                    // use the quantitiesToReceive Map value if there is one
                    quantitiesAccepted.put(strRowNumber, quantitiesToReceive.get(orderItemSeqId));
                } else {
                    // receive full quantity of the order item
                    BigDecimal acceptedQuantity = orderItem.getBigDecimal("quantity");
                    quantitiesAccepted.put(strRowNumber, acceptedQuantity == null ? "0.0" : acceptedQuantity.toString());
                }

                quantitiesRejected.put(strRowNumber, "0.0");
                // set the unit cost from the order item unit price, or if we use standard cost, set it with the item standard cost
                String unitCost = "0.0";
                if (UtilValidate.isNotEmpty(orderItem.get("productId"))) {
                    if (org.usesStandardCosting()) {
                        String costCurrencyUomId = orderCurrencyUomId;
                        Product domainProduct = productRepository.getProductById(orderItem.getString("productId"));
                        if (UtilValidate.isEmpty(costCurrencyUomId)) {
                            costCurrencyUomId = orgCurrencyUomId;
                        }
                        if (UtilValidate.isNotEmpty(costCurrencyUomId)) {
                            unitCost = domainProduct.getStandardCost(costCurrencyUomId).toString();
                        }
                    } else {
                        if (UtilValidate.isNotEmpty(orgCurrencyUomId) && UtilValidate.isNotEmpty(orderCurrencyUomId)) {
                            BigDecimal convertedValue = org.convertUom(orderItem.getBigDecimal("unitPrice"), orderCurrencyUomId);
                            if (convertedValue != null) {
                                unitCost = convertedValue.toString();
                            }
                        }
                    }
                }
                unitCosts.put(strRowNumber, unitCost);

                lotIds.put(strRowNumber, null);

                inventoryItemTypeIds.put(strRowNumber, "NON_SERIAL_INV_ITEM");

                rowSubmit.put(strRowNumber, "Y");

                rowNumber++;
            }
        }

        // put parameters into the return map
        retval.put("orderItemSeqIds", orderItemSeqIds);
        retval.put("productIds", productIds);
        retval.put("quantitiesAccepted", quantitiesAccepted);
        retval.put("quantitiesRejected", quantitiesRejected);
        retval.put("unitCosts", unitCosts);
        retval.put("lotIds", lotIds);
        retval.put("inventoryItemTypeIds", inventoryItemTypeIds);
        retval.put("_rowSubmit", rowSubmit);
        retval.put("shipmentId", null);
        retval.put("purchaseOrderId", orh.getOrderId());
        retval.put("facilityId", facilityId);
        if (forceComplete) {
            retval.put("completePurchaseOrder", "Y");
        } else {
            retval.put("completePurchaseOrder", "N");
        }
        retval.put("ownerPartyId", organizationPartyId);
        retval.put("shipGroupSeqId", shipGroupSeqId);
        retval.put("userLogin", userLogin);

        return retval;
    }

    /**
     * Creates a input parameter map that will be used to call the
     * warehouse.issueOrderItemToShipmentAndReceiveAgainstPO service.
     * The parameter map created does not have any lot and shipment IDs associated.
     * 1) accepted quantities that are the same as ordered quantities
     * 2) no rejected quantities
     * 3) no lots and unit in lots
     * 4) non serial inventory item type for all the received items
     * 5) no existing shipment
     * 6) "WebStoreWarehouse facility
     * @param purchaseOrderId the purchase order id
     * @param orderItems the <code>List</code> of order items <code>GenericValue</code>
     * @param userLogin a <code>GenericValue</code> value
     * @return the <code>Map</code> object that contains the result
     */
    protected Map<String, Object> createTestInputParametersForReceiveInventoryAgainstPurchaseOrderItems(String purchaseOrderId, List<GenericValue> orderItems, final GenericValue userLogin) {
        Map<String, Object> retval = new HashMap<String, Object>();

        Map<String, String> orderItemSeqIds = new HashMap<String, String>();
        Map<String, String> productIds = new HashMap<String, String>();
        Map<String, String> quantitiesAccepted = new HashMap<String, String>();
        Map<String, String> quantitiesRejected = new HashMap<String, String>();
        Map<String, String> unitCosts = new HashMap<String, String>();
        Map<String, String> lotIds = new HashMap<String, String>();
        Map<String, String> inventoryItemTypeIds = new HashMap<String, String>();
        Map<String, String> rowSubmit = new HashMap<String, String>();

        // create parameters
        if (UtilValidate.isNotEmpty(orderItems)) {
            int rowNumber = 0;
            for (GenericValue orderItem : orderItems) {

                String strRowNumber = Integer.toString(rowNumber);

                String orderItemSeqId = orderItem.getString("orderItemSeqId");
                orderItemSeqIds.put(strRowNumber, orderItemSeqId);

                String productId = orderItem.getString("productId");
                productIds.put(strRowNumber, productId);

                BigDecimal acceptedQuantity = orderItem.getBigDecimal("quantity");
                quantitiesAccepted.put(strRowNumber, acceptedQuantity == null ? "0.0" : acceptedQuantity.toString());

                quantitiesRejected.put(strRowNumber, "0.0");
                unitCosts.put(strRowNumber, "0.0");

                lotIds.put(strRowNumber, null);

                inventoryItemTypeIds.put(strRowNumber, "NON_SERIAL_INV_ITEM");

                rowSubmit.put(strRowNumber, "Y");

                rowNumber++;
            }
        }

        // put parameters into the return map
        retval.put("orderItemSeqIds", orderItemSeqIds);
        retval.put("productIds", productIds);
        retval.put("quantitiesAccepted", quantitiesAccepted);
        retval.put("quantitiesRejected", quantitiesRejected);
        retval.put("unitCosts", unitCosts);
        retval.put("lotIds", lotIds);
        retval.put("inventoryItemTypeIds", inventoryItemTypeIds);
        retval.put("_rowSubmit", rowSubmit);
        retval.put("shipmentId", null);
        retval.put("purchaseOrderId", purchaseOrderId);
        retval.put("facilityId", facilityId);
        retval.put("completePurchaseOrder", "N");
        retval.put("ownerPartyId", organizationPartyId);
        retval.put("shipGroupSeqId", shipGroupSeqId);
        retval.put("userLogin", userLogin);

        return retval;
    }

    /**
     * Assert product ATP and QOH are equals to given values.
     * this uses the getProductAvailability method.
     * @param product
     * @param atp
     * @param qoh
     */
    protected void assertProductAvailability(GenericValue product, BigDecimal atp, BigDecimal qoh) {
        Map<String, Object> availability = getProductAvailability(product.getString("productId"));
        assertNotNull("Product [" + product.getString("productId") + "] availability", availability);
        assertEquals("Product [" + product.getString("productId") + "] ATP", (BigDecimal) availability.get("availableToPromiseTotal"), atp);
        assertEquals("Product [" + product.getString("productId") + "] QOH", (BigDecimal) availability.get("quantityOnHandTotal"), qoh);
    }

    /**
     * Assert product ATP equals to given value.
     * this uses the getProductAvailability method.
     * @param product
     * @param atp
     */
    protected void assertProductATP(GenericValue product, BigDecimal atp) {
        Map<String, Object> availability = getProductAvailability(product.getString("productId"));
        assertNotNull("Product [" + product.getString("productId") + "] availability", availability);
        assertEquals("Product [" + product.getString("productId") + "] ATP", (BigDecimal) availability.get("availableToPromiseTotal"), atp);
    }

    /**
     * Assert product QOH equals to given value.
     * this uses the getProductAvailability method.
     * @param product
     * @param qoh
     */
    protected void assertProductQOH(GenericValue product, BigDecimal qoh) {
        Map<String, Object> availability = getProductAvailability(product.getString("productId"));
        assertNotNull("Product [" + product.getString("productId") + "] availability", availability);
        assertEquals("Product [" + product.getString("productId") + "] QOH", (BigDecimal) availability.get("quantityOnHandTotal"), qoh);
    }

    /**
     * Assert inventory item ATP & QOH equals to expected values.
     * Show message that is concatenation of <code>message</code> argument and built-in trailer.
     * ie:
     *  <code>assertInventoryItemQuantities("Be careful!", "XXXX", 0.0, 0.0)</code> may cause message
     *  Be careful!  Inventory item [%1$s] ATP [or QOH] is wrong. Expected 0.0 but was X.X.
     *
     * @param message a <code>String</code> value
     * @param inventoryItemId inventory item to comparison
     * @param expectedATP expected "Available to Promise" value
     * @param expectedQOH expected "Quantity on Hand" value
     * @exception GeneralException if an error occurs
     */
    protected void assertInventoryItemQuantities(String message, String inventoryItemId, Number expectedATP, Number expectedQOH) throws GeneralException {
        InventoryRepositoryInterface repo = inventoryDomain.getInventoryRepository();
        InventoryItem inventoryItem = repo.getInventoryItemById(inventoryItemId);
        BigDecimal invATP = inventoryItem.getAvailableToPromiseTotal();
        if (invATP == null) {
            invATP = BigDecimal.ZERO;
        }
        BigDecimal invQOH = inventoryItem.getQuantityOnHandTotal();
        if (invQOH == null) {
            invQOH = BigDecimal.ZERO;
        }
        assertEquals(String.format("%2$s. Inventory item [%1$s] ATP is wrong. ", inventoryItem.getInventoryItemId(), message), invATP, expectedATP);
        assertEquals(String.format("%2$s. Inventory item [%1$s] QOH is wrong. ", inventoryItem.getInventoryItemId(), message), invQOH, expectedQOH);
    }

    /**
     *  Assert inventory item ATP & QOH equals to expected values.
     *  Show built-in message only.
     *
     * @param inventoryItemId
     * @param expectedATP
     * @param expectedQOH
     * @throws GeneralException
     */
    protected void assertInventoryItemQuantities(String inventoryItemId, Number expectedATP, Number expectedQOH) throws GeneralException {
        assertInventoryItemQuantities("", inventoryItemId, expectedATP, expectedQOH);
    }


    /**
     * Makes a copy from an original party, creating new first and last name in <code>Person</code>.
     * @param templatePartyId the partyId of the <code>Person</code> to use as a template
     * @param newFirstName first name of the new <code>Person</code>
     * @param newLastName last name of the new <code>Person</code>
     * @return the partyId of the new <code>Person</code>
     * @throws GenericEntityException if an error occurs
     */
    public String createPartyFromTemplate(String templatePartyId, String newFirstName, String newLastName) throws GenericEntityException {
        return createPartyFromTemplate(templatePartyId, newFirstName, newLastName, null);
    }

    /**
     * Makes a copy from an original party, creating new company name in <code>PartyGroup</code> and <code>PartySupplementalData</code>.
     * @param templatePartyId the partyId of the <code>PartyGroup</code> to use as a template
     * @param newCompanyName company name of the new <code>PartyGroup</code>
     * @return the partyId of the new <code>PartyGroup</code>
     * @throws GenericEntityException if an error occurs
     */
    public String createPartyFromTemplate(String templatePartyId, String newCompanyName) throws GenericEntityException {
        return createPartyFromTemplate(templatePartyId, null, null, newCompanyName);
    }

    /**
     * Base method for copying a party.  Can change first, last, and company names with this.
     * @param templatePartyId the partyId of the <code>Party</code> to use as a template
     * @param newFirstName first name of the new <code>Person</code>
     * @param newLastName last name of the new <code>Person</code>
     * @param newCompanyName company name of the new <code>PartyGroup</code>
     * @return the partyId of the new <code>Party</code>
     * @throws GenericEntityException if an error occurs
     */
    private String createPartyFromTemplate(String templatePartyId, String newFirstName, String newLastName, String newCompanyName) throws GenericEntityException {
        GenericValue partyTemplate = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", templatePartyId));
        assertNotNull("Failed to find Party template with ID [" + templatePartyId + "]", templatePartyId);

        // make the party
        String partyId = delegator.getNextSeqId("Party");
        GenericValue party = delegator.makeValue("Party", partyTemplate);
        party.put("partyId", partyId);

        List<GenericValue> copies = new FastList<GenericValue>();
        copies.add(party);

        // get all related values and make copies
        List<GenericValue> related = new FastList<GenericValue>();
        GenericValue person = partyTemplate.getRelatedOne("Person");
        GenericValue partyGroup = partyTemplate.getRelatedOne("PartyGroup");
        GenericValue supplementalData = partyTemplate.getRelatedOne("PartySupplementalData");

        if ((newFirstName != null) && (person != null)) {
            person.set("firstName", newFirstName);
        }
        if ((newLastName != null) && (person != null)) {
            person.set("lastName", newLastName);
        }
        if (newCompanyName != null) {
            if (partyGroup != null) {
                partyGroup.set("groupName", newCompanyName);
            }
            if (supplementalData != null) {
                supplementalData.set("companyName", newCompanyName);
            }
        }

        if (person != null) { related.add(person); }
        if (partyGroup != null) { related.add(partyGroup); }
        if (supplementalData != null) { related.add(supplementalData); }
        related.addAll(partyTemplate.getRelated("PartyRole"));
        related.addAll(partyTemplate.getRelated("PartyContactMech"));
        related.addAll(partyTemplate.getRelated("PartyContactMechPurpose"));
        for (GenericValue template : related) {
            GenericValue copy = delegator.makeValue(template.getEntityName(), template);
            copy.put("partyId", partyId);
            copies.add(copy);
        }

        // set the relationships separately due to the differing field names
        List<GenericValue> fromRelationships = partyTemplate.getRelated("FromPartyRelationship");
        for (GenericValue relationship : fromRelationships) {
            GenericValue copy = delegator.makeValue("PartyRelationship", relationship);
            copy.put("partyIdFrom", partyId);
            copies.add(copy);
        }
        List<GenericValue> toRelationships = partyTemplate.getRelated("ToPartyRelationship");
        for (GenericValue relationship : toRelationships) {
            GenericValue copy = delegator.makeValue("PartyRelationship", relationship);
            copy.put("partyIdTo", partyId);
            copies.add(copy);
        }

        // copy any TO agreements if they exist
        List<GenericValue> agreements = partyTemplate.getRelated("ToAgreement");
        for (GenericValue agreementTemplate : agreements) {
            GenericValue agreement = delegator.makeValue("Agreement", agreementTemplate);
            String agreementId = delegator.getNextSeqId("Agreement");
            agreement.put("agreementId", agreementId);
            agreement.put("partyIdTo", partyId);
            copies.add(agreement);

            List<GenericValue> items = agreementTemplate.getRelated("AgreementItem");
            for (GenericValue template : items) {
                GenericValue copy = delegator.makeValue(template.getEntityName(), template);
                copy.put("agreementId", agreementId);
                copies.add(copy);
            }
            List<GenericValue> terms = agreementTemplate.getRelated("AgreementTerm");
            for (GenericValue template : terms) {
                GenericValue copy = delegator.makeValue(template.getEntityName(), template);
                copy.put("agreementId", agreementId);
                copy.put("agreementTermId", delegator.getNextSeqId("AgreementTerm"));
                copies.add(copy);
            }
        }

        // copy the tax exemption
        List<GenericValue> partyTaxAuthInfos = partyTemplate.getRelated("PartyTaxAuthInfo");
        for (GenericValue partyTaxAuthInfoTemplate : partyTaxAuthInfos) {
            GenericValue partyTaxAuthInfo = delegator.makeValue("PartyTaxAuthInfo", partyTaxAuthInfoTemplate);
            partyTaxAuthInfo.put("partyId", partyId);
            copies.add(partyTaxAuthInfo);
        }

        delegator.storeAll(copies);
        return partyId;
    }

    // Domain helpers

    protected OrderRepositoryInterface getOrderRepository(GenericValue user) throws FoundationException {
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(user));
        OrderDomainInterface orderDomain = dl.loadDomainsDirectory().getOrderDomain();
        return orderDomain.getOrderRepository();
    }

    protected ProductRepositoryInterface getProductRepository(GenericValue user) throws FoundationException {
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(user));
        ProductDomainInterface productDomain = dl.loadDomainsDirectory().getProductDomain();
        return productDomain.getProductRepository();
    }

    protected InventoryRepositoryInterface getInventoryRepository(GenericValue user) throws FoundationException {
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(user));
        InventoryDomainInterface inventoryDomain = dl.loadDomainsDirectory().getInventoryDomain();
        return inventoryDomain.getInventoryRepository();
    }

    protected InventoryServiceInterface getInventoryService(GenericValue user) throws FoundationException {
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(user));
        InventoryDomainInterface inventoryDomain = dl.loadDomainsDirectory().getInventoryDomain();
        return inventoryDomain.getInventoryService();
    }

    protected InvoiceRepositoryInterface getInvoiceRepository(GenericValue user) throws FoundationException {
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(user));
        BillingDomainInterface billingDomain = dl.loadDomainsDirectory().getBillingDomain();
        return billingDomain.getInvoiceRepository();
    }

    protected ManufacturingRepositoryInterface getManufacturingRepository(GenericValue user) throws FoundationException {
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(user));
        ManufacturingDomainInterface manufacturingDomain = dl.loadDomainsDirectory().getManufacturingDomain();
        return manufacturingDomain.getManufacturingRepository();
    }

    /**
     * Convenience methods to set a new costing method for the given organization. Mostly used to set to STANDARD_COSTING.
     * @param organizationPartyId a <code>String</code> value
     * @param costingMethodId a <code>String</code> value
     * @exception GeneralException if an error occurs
     */
    protected void setOrganizationCostingMethodId(String organizationPartyId, String costingMethodId) throws GeneralException {
        try {
            Organization organization = organizationRepository.getOrganizationById(organizationPartyId);
            PartyAcctgPreference orgAcctgPref = organization.getPartyAcctgPreference();
            // again, the PartyAcctgPreference may not be configured for some special applications
            if (orgAcctgPref != null) {
                orgAcctgPref.setCostingMethodId(costingMethodId);
                organizationRepository.createOrUpdate(orgAcctgPref);
            }
        } catch (EntityNotFoundException ex) {
            throw new GeneralException(ex);
        }
    }

    /**
     * Check inventory trace event for inventoryItemId, toInventoryItemId, usage type and level in one step.
     * @param events list of events where method finds event with specified parameters
     * @param inventoryItemId source inventory item id, may be <code>null</code>
     * @param toInventoryItemId target inventory item id, may be <code>null</code>
     * @param usageTypeId usage type identifier, from InventoryItemUsageType entity
     * @param level event level in tree
     * @return List of events w/o current event, you may pass returned value as <code>events</code> argument in next call.
     */
    protected List<InventoryItemTraceDetail> assertInventoryTraceEvents(List<InventoryItemTraceDetail> events, String inventoryItemId,
            String toInventoryItemId, String usageTypeId, Long level) {
                Iterator<InventoryItemTraceDetail> iterator = events.iterator();
                while (iterator.hasNext()) {
                    InventoryItemTraceDetail current = iterator.next();
                    if (!usageTypeId.equals(current.getInventoryItemUsageTypeId()) || level.compareTo(current.getTraceLevel()) != 0) {
                        continue;
                    }
                    // inventory item ids may be null, compare them separately
                    if ((inventoryItemId == null && current.getInventoryItemId() != null) || (toInventoryItemId == null && current.getToInventoryItemId() != null)) {
                        continue;
                    }
                    if ((inventoryItemId != null && !inventoryItemId.equals(current.getInventoryItemId())) || (toInventoryItemId != null && !toInventoryItemId.equals(current.getToInventoryItemId()))) {
                        continue;
                    }
                    iterator.remove();
                    return events;
                }
                assertEquals("Usage event for " + usageTypeId + " for inventories " + inventoryItemId + "/" + toInventoryItemId + " not found", true, false);
                return FastList.newInstance();
            }

    /**
     * Base method for copying a organization.  Can change company name with this.
     * @param templatePartyId the partyId of the <code>Party</code> to use as a template
     * @param newOrganizationName organization name of the new <code>PartyGroup</code>
     * @return the partyId of the new <code>Party</code>
     * @throws GeneralException if an error occurs
     */
    public String createOrganizationFromTemplate(String templatePartyId, String newOrganizationName) throws GeneralException {
        String newPartyId = createPartyFromTemplate(templatePartyId, newOrganizationName);
        CopyOrganizationLedgerSetupService service = new CopyOrganizationLedgerSetupService();
        service.setUser(new User(admin));
        service.setInOrganizationPartyId(newPartyId);
        service.setInTemplateOrganizationPartyId(templatePartyId);
        service.runSync(new Infrastructure(dispatcher));
        Debug.logInfo("call createOrganizationFromTemplate use template [" + templatePartyId + "], return partyId [" + newPartyId + "]", MODULE);
        return newPartyId;
    }

    // GWT test related methods, for lookup services and autocompleters


    /**
     * Special assert for GWT lookup service.
     * @param lookup the lookup service instance, after its find method has been called
     * @param values a <code>List</code> of values to assert are found
     * @param fieldName the name of the field where to look for the values
     */
    public void assertGwtLookupFound(EntityLookupService lookup, List<String> values, String fieldName) {
        assertTrue("There should be at least " + values.size() + " record found (" + values + ") in total results: " + lookup.getResultTotalCount() + ".", lookup.getResultTotalCount() >= values.size());
        Set<String> found = Entity.getDistinctFieldValues(String.class, lookup.getResults(), fieldName);
        for (String v : values) {
            assertTrue("Results should contain " + v + ".", found.contains(v));
        }
    }

    /**
     * Special assert for GWT lookup service.
     * @param lookup the lookup service instance, after its find method has been called
     * @param values a <code>List</code> of values to assert are not found
     * @param fieldName the name of the field where to look for the values
     */
    public void assertGwtLookupNotFound(EntityLookupService lookup, List<String> values, String fieldName) {
        Set<String> found = Entity.getDistinctFieldValues(String.class, lookup.getResults(), fieldName);
        for (String v : values) {
            assertFalse("Results should not contain " + v + ".", found.contains(v));
        }
    }

    /**
     * Special assert for GWT lookup service.
     * @param lookup the lookup service instance, after its find method has been called
     * @param fieldName the name of the field where to look for the values
     * @param expected the <code>List</code> of <code>GenericValue</code> sorted as the lookup should return it
     */
    public void assertGwtLookupSort(EntityLookupService lookup, String fieldName, List<GenericValue> expected) {
        debugGwtResults(lookup, expected);
        for (int i = 0; i < expected.size(); i++) {
            EntityInterface record = lookup.getResults().get(i);
            GenericValue expectedRecord = expected.get(i);
            assertEquals("Results where not sorted properly.", record.get(fieldName), expectedRecord.get(fieldName));
        }
    }

    /**
     * Special assert for GWT lookup service.
     * @param lookup the lookup service instance, after its find method has been called
     * @param ids a <code>List</code> of ids to assert the lookup found
     * @param idFieldName the name of the id field where to look for the values
     */
    public void assertGwtSuggestFound(EntityLookupAndSuggestService lookup, List<String> ids, String idFieldName) {
        assertTrue("There should be at least " + ids.size() + " record found (" + ids + ") in total results: " + lookup.getResultTotalCount() + ".", lookup.getResultTotalCount() >= ids.size());
        Set<String> found = Entity.getDistinctFieldValues(String.class, lookup.getResults(), idFieldName);
        Debug.logInfo("Found ids =" + found + " ++ " + found.size() + " EXPECTED ids = " + ids + " ++ " + ids.size(), MODULE);
        for (EntityInterface e : lookup.getResults()) {
            Debug.logInfo("Result: " + e, MODULE);
        }

        Debug.logInfo("Results ++ " + lookup.getResults().size(), MODULE);
        assertEquals("Should be no duplicated ids in the result.", found.size(), lookup.getResults().size());
        for (String id : ids) {
            assertTrue("Results should contain " + id + ".", found.contains(id));
        }
    }

    /**
     * Displays the result of a lookup service and the list of expected values side by side for debugging.
     * @param lookup the lookup service instance to test the results for
     * @param expected the list of expected entities as <code>GenericValue</code>
     */
    public void debugGwtResults(EntityLookupService lookup, List<GenericValue> expected) {
        Debug.logInfo("debugGwtResults and expected values ...", MODULE);
        for (int i = 0; i < expected.size(); i++) {
            EntityInterface r = lookup.getResults().get(i);
            GenericValue expectedRecord = expected.get(i);
            Debug.logInfo("** " + r, MODULE);
            Debug.logInfo("## " + expectedRecord, MODULE);
        }
    }

    /**
     * Asserts the party is a Supplier and not a Customer.
     * @param partyGroup a <code>PartyGroup</code> instance
     * @throws RepositoryException if error occur
     */
    public void assertIsSupplierNotCustomer(PartyGroup partyGroup) throws RepositoryException {
        assertIsSupplierNotCustomer(partyGroup.getParty());
    }

    /**
     * Asserts the party is a Supplier and not a Customer.
     * @param party a <code>Party</code> instance
     * @throws RepositoryException if error occur
     */
    public void assertIsSupplierNotCustomer(Party party) throws RepositoryException {
        boolean isSupplier = false;
        boolean isCustomer = false;
        for (PartyRole partyRole : party.getPartyRoles()) {
            if ("SUPPLIER".equals(partyRole.getRoleTypeId())) {
                isSupplier = true;
            }
            if ("ACCOUNT".equals(partyRole.getRoleTypeId())) {
                isCustomer = true;
            }
        }
        assertTrue("The Party [" + party.getPartyId() + "] should be a Supplier.", isSupplier);
        assertFalse("The Party [" + party.getPartyId() + "] shouldn't be a Customer.", isCustomer);
    }

    /**
     * Asserts the party is a Customer and not a Supplier.
     * @param partyGroup a <code>PartyGroup</code> instance
     * @throws RepositoryException if error occur
     */
    public void assertIsCustomerNotSupplier(PartyGroup partyGroup) throws RepositoryException {
        assertIsCustomerNotSupplier(partyGroup.getParty());
    }

    /**
     * Asserts the party is a Customer and not a Supplier.
     * @param party a <code>Party</code> instance
     * @throws RepositoryException if error occur
     */
    public void assertIsCustomerNotSupplier(Party party) throws RepositoryException {
        boolean isSupplier = false;
        boolean isCustomer = false;
        for (PartyRole partyRole : party.getPartyRoles()) {
            if ("SUPPLIER".equals(partyRole.getRoleTypeId())) {
                isSupplier = true;
            }
            if ("ACCOUNT".equals(partyRole.getRoleTypeId())) {
                isCustomer = true;
            }
        }
        assertFalse("The Party [" + party.getPartyId() + "] shouldn't be a Supplier.", isSupplier);
        assertTrue("The Party [" + party.getPartyId() + "] should be a Customer.", isCustomer);
    }

    /**
     * Change dataString to locale short date string.
     * @param dateString a <code>String</code> value
     * @throws ParseException if an error occurs
     * @return a String value
     */
    public static String dateStringToShortLocaleString(String dateString) throws ParseException {
        return dateStringToShortLocaleString(dateString, "yy/M/d");
    }

    /**
     * Change dataString to locale short date string.
     * @param dateString a <code>String</code> value
     * @param formatString a <code>String</code> value
     * @throws ParseException if an error occurs
     * @return a String value
     */
    public static String dateStringToShortLocaleString(String dateString, String formatString) throws ParseException {
        SimpleDateFormat defaultSdf = new SimpleDateFormat(UtilDateTime.getDateTimeFormat(Locale.getDefault()));
        SimpleDateFormat sdf = new SimpleDateFormat(formatString);
        return defaultSdf.format(sdf.parse(dateString));
    }

    /**
     * Change dataString to timestamp.
     * @param dateString a <code>String</code> value
     * @throws ParseException if an error occurs
     * @return a <code>Timestamp</code> value
     */
    public static Timestamp dateStringToTimestamp(String dateString) throws ParseException {
        return dateStringToTimestamp(dateString, "yy/M/d");
    }

    /**
     * Change dataString to timestamp.
     * @param dateString a <code>String</code> value
     * @param formatString a <code>String</code> value
     * @throws ParseException if an error occurs
     * @return a <code>Timestamp</code> value
     */
    public static Timestamp dateStringToTimestamp(String dateString, String formatString) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(formatString);
        return new java.sql.Timestamp(sdf.parse(dateString).getTime());
    }

    /**
     * Method to check that specific BigDecimal number if exist in List.
     *
     * @param numbers a <code>List<BigDecimal></code> instance
     * @param value a <code>BigDecimal</code> instance
     * @return if exist the value in numbers
     */
    public static boolean assertNumberExistsInList(List<BigDecimal> numbers, BigDecimal value) {
        for (BigDecimal number : numbers) {
            if (number.compareTo(value) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to get a ByteBuffer from a file.
     *
     * @param filePath the path to the file
     * @return a <code>ByteBuffer</code> value
     * @throws IOException if an error occurs
     */
    public static ByteBuffer getByteBufferFromFile(String filePath) throws IOException {
        return ByteBuffer.wrap(getBytesFromFile(new File(filePath)));
    }

    /**
     * Helper method to get a ByteBuffer from a file.
     *
     * @param f a <code>File</code> value
     * @return a <code>ByteBuffer</code> value
     * @throws IOException if an error occurs
     */
    public static ByteBuffer getByteBufferFromFile(File f) throws IOException {
        return ByteBuffer.wrap(getBytesFromFile(f));
    }

    /**
     * Helper method to get byte[] from a file.
     *
     * @param f a <code>File</code> value
     * @return a <code>byte[]</code> value
     * @throws IOException if an error occurs
     */
    public static byte[] getBytesFromFile(File f) throws IOException {
        if (f == null) {
            return null;
        }

        FileInputStream stream = new FileInputStream(f);
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] b = new byte[1024];
        int n;
        while ((n = stream.read(b)) != -1) {
            out.write(b, 0, n);
        }
        stream.close();
        out.close();
        return out.toByteArray();
    }
}
