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
package org.opentaps.tests.entity;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.base.entities.Enumeration;
import org.opentaps.base.entities.InvoiceItem;
import org.opentaps.base.entities.InvoiceType;
import org.opentaps.base.entities.PaymentAndApplication;
import org.opentaps.base.entities.TestEntity;
import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.util.FoundationUtils;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Entity related tests.
 */
public class EntityTests extends OpentapsTestCase {

    private static final String MODULE = HibernateTests.class.getName();

    private static final String INVOICE_ID = "ENTITY-TEST";
    private static final String INVOICE_TYPE_ID = "SALES_INVOICE";
    private static final String PAYMENT_ID = "ENTITY-TEST";
    private static final String PAYMENT_APPLICATION_ID = "ENTITY-TEST";

    List<GenericValue> enumerations = null;
    GenericValue user = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        enumerations = delegator.findAll("Enumeration");
        user = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
        //
        removeTestData(delegator);
        createTestData(delegator);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        // delegator is reset to null by super.tearDown() so we have to get it again
        removeTestData(DelegatorFactory.getDelegator(OpentapsTestCase.DELEGATOR_NAME));
    }

    private void createTestData(Delegator delegator) throws GenericEntityException {
        delegator.create("Invoice", UtilMisc.toMap("invoiceId", INVOICE_ID, "invoiceTypeId", INVOICE_TYPE_ID, "invoiceDate", UtilDateTime.nowTimestamp()));
        delegator.create("InvoiceItem", UtilMisc.toMap("invoiceId", INVOICE_ID, "invoiceItemSeqId", "00001", "invoiceItemTypeId", "INV_FPROD_ITEM", "quantity", new BigDecimal("5.0"), "amount", new BigDecimal("10.0")));
        delegator.create("Payment", UtilMisc.toMap("paymentId", PAYMENT_ID, "paymentTypeId", "CUSTOMER_PAYMENT", "statusId", "PMNT_CONFIRMED", "amount", new BigDecimal("10.0"), "effectiveDate", UtilDateTime.nowTimestamp()));
        delegator.create("PaymentApplication", UtilMisc.toMap("paymentApplicationId", PAYMENT_APPLICATION_ID, "paymentId", PAYMENT_ID, "invoiceId", INVOICE_ID, "amountApplied", new BigDecimal("10.0")));
    }

    private void removeTestData(Delegator delegator) throws GenericEntityException {
        delegator.removeByCondition("PaymentApplication", EntityCondition.makeCondition("paymentApplicationId", EntityOperator.EQUALS, PAYMENT_APPLICATION_ID));
        delegator.removeByCondition("InvoiceItem", EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, INVOICE_ID));
        delegator.removeByCondition("Invoice", EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, INVOICE_ID));
        delegator.removeByCondition("Payment", EntityCondition.makeCondition("paymentId", EntityOperator.EQUALS, PAYMENT_ID));
    }

    /**
     * Tests converting a <code>GenericValue</code> to a domain base entity using <code>FoundationUtils.loadFromMap</code>.
     * @throws Exception if an error occurs
     */
    public void testEntityLoading() throws Exception {
        for (GenericValue enumeration : enumerations) {
            Enumeration enum1 = new Enumeration();
            enum1.fromMap(enumeration);
            Enumeration enum2 = FoundationUtils.loadFromMap(Enumeration.class, enumeration.getAllFields());
            assertEquals("FoundationUtils did not load Entity class correctly", enum1.toString(), enum2.toString());
        }
    }

    /**
     * Tests loading a domain base entity using <code>Repository.loadFromGeneric</code>.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testRepositoryLoadFromGeneric() throws Exception {
        List enumerationEntities = Repository.loadFromGeneric(Enumeration.class, enumerations);
        for (int i = 0; i < enumerations.size(); i++) {
            Enumeration enum1 = (Enumeration) enumerationEntities.get(i);
            Enumeration enum2 = FoundationUtils.loadFromMap(Enumeration.class, enumerations.get(i).getAllFields());
            assertEquals("Repository did not load Entity class correctly", enum1.toString(), enum2.toString());
        }
    }

    private InvoiceRepositoryInterface getInvoiceRepository() throws Exception {

        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(user));
        BillingDomainInterface billingDomain = dl.loadDomainsDirectory().getBillingDomain();
        return billingDomain.getInvoiceRepository();
    }

    /**
     * This test will validate that the repository will be correctly set in an Entity object and that the fields have correct values,
     * including the Double/BigDecimal conversion.
     * @throws Exception if an error occurs
     */
    public void testInvoiceRepository() throws Exception {
        InvoiceRepositoryInterface repository = getInvoiceRepository();
        Invoice invoice = repository.getInvoiceById(INVOICE_ID);
        GenericValue invoiceValue = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", INVOICE_ID));

        assertEquals("Invoice.invoiceId is correct", invoiceValue.getString("invoiceId"), invoice.getInvoiceId());
        assertEquals("Invoice.invoiceTypeId is correct", invoiceValue.getString("invoiceTypeId"), invoice.getInvoiceTypeId());
        assertEquals("Invoice.invoiceDate is correct", invoiceValue.getTimestamp("invoiceDate"), invoice.getInvoiceDate());

        assertEquals("There is one invoice item for invoice in testInvoiceRepository", BigDecimal.ONE, new BigDecimal(invoice.getInvoiceItems().size()));
        for (InvoiceItem item : invoice.getInvoiceItems()) {
            GenericValue itemValue = delegator.findByPrimaryKey("InvoiceItem", UtilMisc.toMap("invoiceId", item.getInvoiceId(), "invoiceItemSeqId", item.getInvoiceItemSeqId()));
            assertEquals("InvoiceItem.invoiceId is correct", itemValue.getString("invoiceId"), item.getInvoiceId());
            assertEquals("InvoiceItem.invoiceItemSeqId is correct", itemValue.getString("invoiceItemSeqId"), item.getInvoiceItemSeqId());
            assertEquals("InvoiceItem.invoiceItemTypeIdId is correct", itemValue.getString("invoiceItemTypeId"), item.getInvoiceItemTypeId());
            assertEquals("InvoiceItem.amount is correct", itemValue.getBigDecimal("amount"), item.getAmount());
            assertEquals("InvoiceItem.quantity is correct", itemValue.getBigDecimal("quantity"), item.getQuantity());
        }
    }

    /**
     * Tests that the method <code>findOneNotNull</code> from the repository generates the proper exception.
     * @throws Exception if an error occurs
     */
    public void testEntityNotFoundExceptions() throws Exception {
        Repository repository = new Repository(new Infrastructure(dispatcher));
        boolean caught = false;
        try {
            repository.findOneNotNull(Invoice.class, repository.map(Invoice.Fields.invoiceId, "NOTEXISTS"));
        } catch (EntityNotFoundException e) {
            caught = true;
            Debug.logInfo("Caught EntityNotFoundException: " + e.getMessage(), MODULE);
            assertEquals("EntityNotFoundException entity class does not match.", Invoice.class, e.getEntityClass());
            assertEquals("EntityNotFoundException primary key does not match.", repository.map(Invoice.Fields.invoiceId, "NOTEXISTS"), e.getPrimaryKey());
            assertTrue("EntityNotFoundException message does not indicate the entity name.", e.getMessage().contains(Invoice.class.getName()));
            assertTrue("EntityNotFoundException message does not indicate the primary key.", e.getMessage().contains(repository.map(Invoice.Fields.invoiceId, "NOTEXISTS").toString()));
        }
        assertTrue("Should have caught an EntityNotFoundException", caught);
    }


    /**
     * This test is primarily designed to test the mapping of a view-entity, in this case PaymentAndApplication, to its corresponding Java object.
     * @throws Exception if an error occurs
     */
    public void testViewEntityLoading() throws Exception {
        InvoiceRepositoryInterface repository = getInvoiceRepository();
        Invoice invoice = repository.getInvoiceById(INVOICE_ID);
        List<PaymentAndApplication> paymentAndApplications = repository.getPaymentsApplied(invoice, UtilDateTime.nowTimestamp());
        assertEquals("There is one paymentAndApplications for invoice in testInvoiceRepository", BigDecimal.ONE, new BigDecimal(paymentAndApplications.size()));
        for (PaymentAndApplication paymentAndApplication : paymentAndApplications) {
            GenericValue paValue = EntityUtil.getFirst(delegator.findByAnd("PaymentAndApplication", UtilMisc.toMap("paymentApplicationId", paymentAndApplication.getPaymentApplicationId())));
            assertEquals("PaymentAndApplication.paymentTypeId is correct", paValue.getString("paymentTypeId"), paymentAndApplication.getPaymentTypeId());
            assertEquals("PaymentAndApplication.invoiceId is correct", paValue.getString("invoiceId"), paymentAndApplication.getInvoiceId());
            assertEquals("PaymentAndApplication.paymentId is correct", paValue.getString("paymentId"), paymentAndApplication.getPaymentId());
            assertEquals("PaymentAndApplication.amount is correct", paValue.getBigDecimal("amount"), paymentAndApplication.getAmount());
            assertEquals("PaymentAndApplication.amountApplied is correct", paValue.getBigDecimal("amountApplied"), paymentAndApplication.getAmountApplied());
            assertEquals("PaymentAndApplication.effectiveDate is correct", paValue.getTimestamp("effectiveDate"), paymentAndApplication.getEffectiveDate());
        }

    }

    /**
     * Tests <code>Entity.hashCode()</code>.
     * @throws Exception if an error occurs
     */
    public void testEntityHashCode() throws Exception {

        String enumId1 = "INVRO_FIFO_EXP";

        // gets two Enumeration objects of the same enumID, makes sure their hash codes are equal
        Enumeration en1 = Repository.loadFromGeneric(Enumeration.class, delegator.findByPrimaryKey("Enumeration", UtilMisc.toMap("enumId", enumId1)));
        Enumeration en2 = Repository.loadFromGeneric(Enumeration.class, delegator.findByPrimaryKey("Enumeration", UtilMisc.toMap("enumId", enumId1)));
        assertEquals("Two instances of the same Enumeration have different hash code.", en1.hashCode(), en2.hashCode());

        // creates a set and add both Enumeration objects, makes sure the Set has size()=1
        Set<Enumeration> set = new HashSet<Enumeration>();
        set.add(en1);
        set.add(en2);
        assertEquals(1, set.size());

        // modifies one of the objects and verifies that the hash codes are no longer equal
        en2.setDescription("Modified Object");
        assertNotEquals("", en1.hashCode(), en2.hashCode());

        // adds that modified object to the Set and verifies that the size()=2
        set.add(en2);
        assertEquals(2, set.size());

    }

    /**
     * Test <code>getRelatedOne</code> method to retrieve the <code>InvoiceType</code> from the create <code>Invoice</code>.
     * - retrieve the test <code>Invoice</code> domain object
     * - get the <code>InvoiceType</code> using getRelatedOne with the relation name
     * @throws Exception if an error occurs
     */
    public void testGetRelatedOneNamed() throws Exception {
        InvoiceRepositoryInterface repository =  getInvoiceRepository();
        Invoice invoice = repository.getInvoiceById(INVOICE_ID);

        // get the invoice type
        InvoiceType invoiceType = invoice.getRelatedOne(InvoiceType.class, "InvoiceType");
        assertNotNull("Failed getting InvoiceType using the named relation", invoiceType);
        assertEquals("Failed getting InvoiceType using the named relation", invoiceType.getInvoiceTypeId(), INVOICE_TYPE_ID);

        // alternate way to get invoice type
        InvoiceType invoiceType2 = invoice.getInvoiceType();
        assertEquals("getInvoiceType and getRelatedOne(InvoiceType) should return the same result.", invoiceType, invoiceType2);
    }

    /**
     * Test <code>getRelatedOne</code> method to retrieve the <code>InvoiceType</code> from the create <code>Invoice</code>.
     * - retrieve the test <code>Invoice</code> domain object
     * - get the <code>InvoiceType</code> using getRelatedOne with the relation name
     * @throws Exception if an error occurs
     */
    public void testGetRelatedOneUnamed() throws Exception {
        InvoiceRepositoryInterface repository =  getInvoiceRepository();
        Invoice invoice = repository.getInvoiceById(INVOICE_ID);

        // get the invoice type
        InvoiceType invoiceType = invoice.getRelatedOne(InvoiceType.class);
        assertNotNull("Failed getting InvoiceType using the unnamed relation", invoiceType);
        assertEquals("Failed getting InvoiceType using the unnamed relation", invoiceType.getInvoiceTypeId(), INVOICE_TYPE_ID);

        // alternate way to get invoice type
        InvoiceType invoiceType2 = invoice.getInvoiceType();
        assertEquals("getInvoiceType and getRelatedOne(InvoiceType) should return the same result.", invoiceType, invoiceType2);
    }

    /**
     * Test <code>getRelated</code> method to retrieve the <code>InvoiceType</code> from the create <code>Invoice</code>.
     * - retrieve the test <code>Invoice</code> domain object
     * - get the list of <code>InvoiceItem</code> using getRelatedOne with and without the relation name
     * @throws Exception if an error occurs
     */
    public void testGetRelated() throws Exception {
        InvoiceRepositoryInterface repository =  getInvoiceRepository();
        Invoice invoice = repository.getInvoiceById(INVOICE_ID);

        // get the invoice items
        List<InvoiceItem> items = invoice.getRelated(InvoiceItem.class, "InvoiceItem");
        assertNotEmpty("Failed getting the InvoiceItems using the named relation", items);

        InvoiceItem item = Repository.getFirst(items);
        assertNotNull("Failed getting the first InvoiceItem", item);
        assertEquals("Failed getting the first InvoiceItem", item.getInvoiceItemSeqId(), "00001");

        // alternate way to get invoice items
        assertEquals("getInvoiceItems and getRelated(InvoiceItem) should return the same result.", items, invoice.getInvoiceItems());
    }

    /**
     * Tests the repository can decrypt delegator encrypted value.
     * @throws Exception if an error occurs
     */
    public void testRepositoryCanDecryptDelegatorEncryptedValue() throws Exception {
        String beforeEncryptValue = "not encrypt value";
        String testId = delegator.getNextSeqId("TestEntity");
        delegator.create("TestEntity", UtilMisc.toMap("testId", testId, "testStringField", "testRepositoryCanDecryptDelegatorEncryptedValue", "testEncrypt", beforeEncryptValue));

        Repository repository = new Repository(delegator);
        TestEntity testEntity = repository.findOneNotNull(TestEntity.class, repository.map(TestEntity.Fields.testId, testId));
        // verify we can decrypt the encrypted field
        assertEquals("The encrypted field value of TestEntity should have been " + beforeEncryptValue + ".", beforeEncryptValue, testEntity.getTestEncrypt());
    }

    /**
     * Tests the delegator can decrypt repository encrypted value.
     * @throws Exception if an error occurs
     */
    public void testDelegatorCanDecryptRepositoryEncryptedValue() throws Exception {
        String beforeEncryptValue = "not encrypt value";
        Repository repository = new Repository(delegator);
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("testDelegatorCanDecryptHibernateEncryptedValue");
        testEntity.setTestEncrypt(beforeEncryptValue);
        testEntity.setTestId(repository.getNextSeqId(testEntity));
        repository.createOrUpdate(testEntity);

        // verify we can decrypt the encrypted field
        GenericValue testEntityValue = delegator.findByPrimaryKey("TestEntity", UtilMisc.toMap("testId", testEntity.getTestId()));
        assertEquals("The encrypted field value of TestEntity should have been " + beforeEncryptValue + ".", beforeEncryptValue, testEntityValue.getString("testEncrypt"));
    }
}
