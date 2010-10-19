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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import javax.transaction.UserTransaction;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.opentaps.base.entities.DataResource;
import org.opentaps.base.entities.ElectronicText;
import org.opentaps.base.entities.PartyContactInfo;
import org.opentaps.base.entities.SalesOpportunity;
import org.opentaps.base.entities.SalesOpportunityRole;
import org.opentaps.base.entities.TestEntity;
import org.opentaps.base.entities.TestEntityAndItem;
import org.opentaps.base.entities.TestEntityItem;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Test case superclass for all hibernate tests. This defines asserts which are
 * useful for testing crud transactions, etc.
 */
public class HibernateTests extends OpentapsTestCase {

    private static final String MODULE = HibernateTests.class.getName();

    private Session session;
    private Infrastructure infrastructure;
    private String testEntityId1 = "";
    private String testEntityId2 = "";
    //mill-second of a minute
    private long minute = 60 * 1000;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        infrastructure = new Infrastructure(dispatcher);
        removeTestData();
        createTestData();
    }

    @Override
    public void tearDown() throws Exception {
        // Because hibernate session use JDBC connection where from parameter,
        // when we close the session, the connection won't close exactly. so we
        // need close it manually.
        if (session != null && session.isOpen()) {
            Transaction tx = session.getTransaction();
            if (tx != null && tx.isActive()) {
                Debug.logError("Found active transaction, rolling back.", MODULE);
                try {
                    tx.rollback();
                } catch (Exception e) {
                    Debug.logError(e, MODULE);
                }
            }
            session.close();
        }
        super.tearDown();
    }

    private void reOpenSession() throws Exception {
        if (session != null && session.isOpen()) {
            Transaction tx = session.getTransaction();
            if (tx != null && tx.isActive()) {
                Debug.logError("Found active transaction, rolling back.", MODULE);
                try {
                    tx.rollback();
                } catch (Exception e) {
                    Debug.logError(e, MODULE);
                }
            }
            session.close();
        }
        session = infrastructure.getSession();
    }

    /**
     * Test hibernate search.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testHibernateSearch() throws Exception {
        reOpenSession();
        //create search index firstly by run createHibernateSearchIndex
        Map inputParams = UtilMisc.toMap("userLogin", admin);
        runAndAssertServiceSuccess("opentaps.createHibernateSearchIndex", inputParams);
        reOpenSession();
        FullTextSession fullTextSession = Search.getFullTextSession(session.getHibernateSession());
        Transaction tx = fullTextSession.beginTransaction();

        // verify we can search keyword for relative object
        String keyWord = "potentially major";
        // create native Lucene query
        String[] fields = new String[]{"salesOpportunity.description", "id.roleTypeId"};
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, new StandardAnalyzer());
        org.apache.lucene.search.Query subQuery1 = parser.parse(keyWord);
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(subQuery1, BooleanClause.Occur.MUST);
        // wrap by hibernate query
        org.hibernate.search.FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(booleanQuery, SalesOpportunityRole.class);

        fullTextQuery.setProjection(FullTextQuery.THIS, FullTextQuery.SCORE);
        List<Object[]> results = fullTextQuery.list();
        List<SalesOpportunity> salesOpportunities = getSalesOpportunitiesFromSearchResult(results);
        assertEquals("We should found 2 SalesOpportunity by search [" + keyWord + "].", 2, salesOpportunities.size());

        //add filter condition, filter by id.partyId=DemoAccount1
        org.apache.lucene.search.Query subQuery2 = new TermQuery(new Term("id.partyId", "DemoAccount1"));
        booleanQuery.add(subQuery2, BooleanClause.Occur.MUST);
        // search it again
        results = fullTextQuery.list();
        salesOpportunities = getSalesOpportunitiesFromSearchResult(results);
        assertEquals("We should found 1 SalesOpportunity by search [" + keyWord + "] and partyId=DemoAccount1.", 1, salesOpportunities.size());

        tx.commit();
        fullTextSession.close();

    }

    /**
     * get non-repetition SalesOpportunity from search result.
     *
     * @param results a <code>List<Object[]></code> value.
     * @return a <code>List<SalesOpportunity></code> value
     * @throws RepositoryException if an error occurs
     */
    private List<SalesOpportunity> getSalesOpportunitiesFromSearchResult(List<Object[]> results) throws RepositoryException {
        List<SalesOpportunity> salesOpportunities = new ArrayList<SalesOpportunity>();
        for (Object[] result : results) {
            SalesOpportunityRole entity = (SalesOpportunityRole) result[0];
            if (!salesOpportunities.contains(entity.getSalesOpportunity())) {
                salesOpportunities.add(entity.getSalesOpportunity());
            }
        }
        return salesOpportunities;
    }

    /**
     * Tests we can use hibernate create TestEntity in Service with delegator togather.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCreateTestEntityAndItemsInService() throws Exception {
        reOpenSession();
        // Call createTestEntityAndItems to create 2 * TestEntity with 2 * 10 TestEntityItem
        String description = "testCreateTestEntityAndItemsInService";
        Integer numberOfItems = 10;
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", admin);
        input.put("description", description);
        input.put("numberOfItems", numberOfItems);
        Map results = dispatcher.runSync("createTestEntityAndItems", input);
        List testIds = (List) results.get("testIds");
        //verify service return two testId
        assertEquals("We should created two TestEntity.", 2, testIds.size());

        String hql = "from TestEntity eo where eo.testStringField = :param1";
        Query query = session.createQuery(hql);
        query.setString("param1", description);
        List<TestEntity> list = query.list();
        // verify we can find two TestEntity by search description
        Debug.logInfo("found " + list.size() + " TestEntity by testStringField=" + description, MODULE);
        assertEquals("We should found 2 TestEntity by testStringField=" + description + ".", 2, list.size());

        hql = "from TestEntityItem eo where eo.itemValue = :param1";
        query = session.createQuery(hql);
        query.setString("param1", description);
        List<TestEntityItem> listItem = query.list();
        Debug.logInfo("found " + listItem.size() + " TestEntityItem by itemValue=" + description, MODULE);
        // verify we can find 20 TestEntityItem by search description
        assertEquals("We should found 20 TestEntityItem by itemValue=" + description + ".", 20, listItem.size());
    }

    /**
     * Tests hibernate can decrypt delegator encrypted value.
     * @throws Exception if an error occurs
     */
    public void testHibernateCanDecryptDelegatorEncryptedValue() throws Exception {
     // open a new session, if session has opened, then close it first
        reOpenSession();
        String beforeEncryptValue = "not encrypt value";
        String testId = delegator.getNextSeqId("TestEntity");
        delegator.create("TestEntity", UtilMisc.toMap("testId", testId, "testStringField", "testHibernateCanDecryptDelegatorEncryptedValue", "testEncrypt", beforeEncryptValue));

        //load entity by hibernate
        TestEntity testEntity = (TestEntity) session.get(TestEntity.class, testId);
        // verify delegator can decrypt the encrypt field which save by hibernate
        assertEquals("The testEncrypt field value of TestEnity that load by hiberate should equals " + beforeEncryptValue + ".", beforeEncryptValue, testEntity.getTestEncrypt());
    }

    /**
     * Tests delegator can decrypt hibernate encrypted value.
     * @throws Exception if an error occurs
     */
    public void testDelegatorCanDecryptHibernateEncryptedValue() throws Exception {
     // open a new session, if session has opened, then close it first
        reOpenSession();
        String beforeEncryptValue = "not encrypt value";
        UserTransaction tx = session.beginUserTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("testDelegatorCanDecryptHibernateEncryptedValue");
        testEntity.setTestEncrypt(beforeEncryptValue);
        session.save(testEntity);
        session.flush();
        tx.commit();

        //load entity by delegator
        GenericValue testEntityValue = delegator.findByPrimaryKey("TestEntity", UtilMisc.toMap("testId", testEntity.getTestId()));
        // verify delegator can decrypt the encrypt field which save by hibernate
        assertEquals("The testEncrypt field value of TestEnity that load by delegator should equals " + beforeEncryptValue + ".", beforeEncryptValue, testEntityValue.getString("testEncrypt"));
    }

    /**
     * Tests hibernate can automatic encrypt/decrypt field value for encrypt field.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testAutomaticalEncrypted() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        String beforeEncryptValue = "not encrypt value";
        UserTransaction tx = session.beginUserTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("testEntity for testAutomaticalEncrypted");
        testEntity.setTestEncrypt(beforeEncryptValue);
        session.save(testEntity);
        session.flush();
        tx.commit();
        //verify after save, the testEncrypt field value shouldn't equals initial value
        assertNotEquals("After save the testEncrypt field value shouldn't equals " + beforeEncryptValue + ".", beforeEncryptValue, testEntity.getTestEncrypt());
        Debug.logInfo("After save the testEncrypt field is " + testEntity.getTestEncrypt(), MODULE);
        //verify after load, the testEncrypt field value should equals initial value
        testEntity = (TestEntity) session.get(TestEntity.class, testEntity.getTestId());
        assertEquals("After reload the TestEntity, testEncrypt field value should equals " + beforeEncryptValue + ".", beforeEncryptValue, testEntity.getTestEncrypt());
        Debug.logInfo("After load the testEncrypt field is " + testEntity.getTestEncrypt(), MODULE);

        //verify the testEncrypt field value of TestEnity that query by hibernate should equals initial value
        String hql = "from TestEntity eo where eo.testId = :testId";
        Query query = session.createQuery(hql);
        query.setString("testId", testEntity.getTestId());
        List<TestEntity> list = query.list();
        assertEquals("Should found 1 TestEntity by " + hql, 1, list.size());
        testEntity = list.get(0);
        assertEquals("The testEncrypt field value of TestEnity that query by hibernate should equals " + beforeEncryptValue + ".", beforeEncryptValue, testEntity.getTestEncrypt());
        Debug.logInfo("After query the testEncrypt field is " + testEntity.getTestEncrypt(), MODULE);

        //verify we can search testEncrypt field by named parameter
        hql = "from TestEntity eo where eo.testEncrypt = :param1";
        query = session.createQuery(hql);
        query.setParameter("param1", beforeEncryptValue);
        list = query.list();
        assertEquals("Should found 1 TestEntity by " + hql, 1, list.size());
        assertEquals("We should find " + testEntity.getTestId() + " in this list.", testEntity.getTestId(), list.get(0).getTestId());

        //verify we can search testEncrypt field by located parameter
        hql = "from TestEntity eo where eo.testEncrypt like ?";
        query = session.createQuery(hql);
        query.setParameter(0, beforeEncryptValue);
        list = query.list();
        assertEquals("Should found 1 TestEntity by " + hql, 1, list.size());
        assertEquals("We should find " + testEntity.getTestId() + " in this list.", testEntity.getTestId(), list.get(0).getTestId());
    }

    /**
     * Tests the created and updated timestamps are automatically stored when objects are persisted with hibernate.
     * @throws Exception if an error occurs
     */
    public void testAutomaticStoreTimestamps() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        UserTransaction tx = session.beginUserTransaction();
        //create an entity
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("value for testAutomaticalStoreTimestamps");
        session.save(testEntity);
        session.flush();
        tx.commit();
        // wait one minute
        pause("wait a minute", minute);
        //Find the entity, update it, and store again
        testEntity = (TestEntity) session.get(TestEntity.class, testEntity.getTestId());
        tx.begin();
        testEntity.setTestStringField("new value for testAutomaticalStoreTimestamps");
        session.save(testEntity);
        tx.commit();

        Debug.logInfo("createStamp is [" + testEntity.getCreatedStamp() + "], lastUpdatedStamp is [" + testEntity.getLastUpdatedStamp() + "]", MODULE);
        //verify createStamp should not be null
        assertNotNull("CreatedStamp of TestEntity shouldn't be null.", testEntity.getCreatedStamp());
        //verify lastUpdatedStamp should not be null
        assertNotNull("LastUpdatedStamp of TestEntity shouldn't be null.", testEntity.getLastUpdatedStamp());
        //verify lastUpdatedStamp >= createStamp + 1min
        assertTrue("lastUpdatedStamp should >= createStamp + 1min", testEntity.getLastUpdatedStamp().getTime() - testEntity.getCreatedStamp().getTime() >= minute);
    }

    /**
     * Test of the Session.getNextSeqId(seqName) method.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testSessionGetNextSeqId() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // store TestEntityItem.testEntityItemSeqId
        String testEntityItemSeqIds = "";
        // open a transaction
        UserTransaction tx = session.beginUserTransaction();
        Debug.logInfo("tx.getStatus() : " + tx.getStatus(), MODULE);
        // create a TestEntity
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("testGetRelated");
        session.save(testEntity);
        Debug.logInfo("testSessionGetNextSeqId(), testEntity Id is " + testEntity.getTestId(), MODULE);
        // create 10 TestEntityItem values with the same testEntityId and use session.getNextSeqId get testEntityItemSeqId
        for (int i = 0; i < 10; i++) {
            TestEntityItem testEntityItem = new TestEntityItem();
            testEntityItem.setTestEntityId(testEntity.getTestId());
            testEntityItem.setTestEntityItemSeqId(session.getNextSeqId(("TestEntityItemSeqId")));
            session.save(testEntityItem);
            testEntityItemSeqIds = testEntityItemSeqIds.equals("")
                    ? "'" + testEntityItem.getTestEntityItemSeqId() + "'"
                    : testEntityItemSeqIds + ",'" + testEntityItem.getTestEntityItemSeqId() + "'";
        }
        session.flush();
        Debug.logInfo("tx is null : " + (tx == null), MODULE);
        Debug.logInfo("tx.getStatus() : " + tx.getStatus(), MODULE);
        tx.commit();
        // verify that you can get a related TestEntityItem from TestEntity with its testItemSeqId
        String hql = "from TestEntityItem eo where eo.testEntity.testId='" + testEntity.getTestId() + "'" + " and eo.id.testEntityItemSeqId in (" + testEntityItemSeqIds + ")";
        Query query = session.createQuery(hql);
        List<TestEntityItem> list = query.list();
        assertEquals("Should found 10 TestEntityItem with search by " + hql, 10, list.size());
    }


    /**
     * Test Our ID generator and the ofbiz entity engine getNextSeqId work well
     * together.
     *
     * @throws Exception if an error occurs
     */
    public void testIdentifierGeneratorWorkWithGetNextSeqId() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        UserTransaction tx = session.beginUserTransaction();
        TestEntity useIdentifierTestEntity = new TestEntity();
        useIdentifierTestEntity
                .setTestStringField("Use IdentifierGenerator string field");
        session.save(useIdentifierTestEntity);
        // before commit transaction, we create another TestEntity by delegator,
        // and use getNextSeqId to get testId
        String getNextSeqIdTestEntityId = delegator.getNextSeqId("TestEntity");
        GenericValue useGetNextSeqIdTestEntity = delegator.create("TestEntity", UtilMisc.toMap("testId", getNextSeqIdTestEntityId, "testStringField", "Use getNextSeqId string field"));
        Debug.logInfo("useIdentifierTestEntity.testId : " + useIdentifierTestEntity.getTestId(), MODULE);
        Debug.logInfo("useGetNextSeqIdTestEntity.testId : " + useGetNextSeqIdTestEntity.getString("testId"), MODULE);
        assertNotEquals("The ID generator and the ofbiz entity engine getNextSeqId shouldn't generate same Id for entity.", useGetNextSeqIdTestEntity.getString("testId"), useIdentifierTestEntity.getTestId());
        session.flush();
        tx.commit();
    }

    /**
     * Tests inserting a new TestEntity and finding it again by Hibernate.
     * (control by JTA)
     *
     * @throws Exception if an error occurs
     */
    public void testInsertTestEntityWithJTA() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        UserTransaction tx = session.beginUserTransaction();
        TestEntity newTestEntity = new TestEntity();
        newTestEntity.setTestStringField("testInsertTestEntity string field");
        session.save(newTestEntity);
        session.flush();
        tx.commit();

        TestEntity loadEntity = (TestEntity) session.get(TestEntity.class, newTestEntity.getTestId());
        assertNotNull("Cannot find TestEntity with Id " + newTestEntity.getTestId() + " in hibernate", loadEntity);
    }

    /**
     * Tests updating an existing TestEntity created by createTestData() by
     * Hibernate. (control by JTA)
     *
     * @throws Exception if an error occurs
     */
    public void testUpdateTestEntityWithJTA() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // create a TestEntity
        TestEntity testEntity = createAndSaveTestEntity("old value");
        // try to modify this entity
        UserTransaction tx = session.beginUserTransaction();
        TestEntity loadEntity = (TestEntity) session.get(TestEntity.class, testEntity.getTestId());
        assertEquals("Correct value should is old string value", loadEntity.getTestStringField(), "old value");
        loadEntity.setTestStringField("new value");
        session.update(loadEntity);
        session.flush();
        tx.commit();
        loadEntity = (TestEntity) session.get(TestEntity.class, testEntity.getTestId());
        assertEquals("Correct value should is new string value", loadEntity.getTestStringField(), "new value");
    }

    /**
     * Test Eca can work for hibernate persist event.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testEca() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        UserTransaction tx = session.beginUserTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("test string field");
        //on hibernate persist, it should be trigger EcaPersistEventListener.onPersist event
        session.persist(testEntity);
        session.flush();
        tx.commit();

        // verify that 1 TestEntityModifyHistory could be found
        String hql = "from TestEntityModifyHistory eo where eo.testId = '" + testEntity.getTestId() + "'";
        Query query = session.createQuery(hql);
        List<TestEntity> list = query.list();
        assertEquals("Should have found 1 TestEntityModifyHistory values with testId [" + testEntity.getTestId() + "]", 1, list.size());
    }

    /**
     * Test Eca can work for hibernate persist event. (when error occur, it should be rollback)
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testEcaFailed() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        UserTransaction tx = session.beginUserTransaction();
        String testId = null;
        try {
            TestEntity testEntity = new TestEntity();
            // this value will raise an GenericEntityException in ECA of TestEntityServices.raiseGenericEntityException
            testEntity.setTestStringField("GenericEntityException");
            session.persist(testEntity);
            testId = testEntity.getTestId();
            session.flush();
            tx.commit();
        } catch (Exception ex) {
            Debug.logError("Call testEcaFailed() get " + ex.getClass().getCanonicalName() + " : " + ex, MODULE);
            try {
                tx.rollback();
            } catch (HibernateException e) {
                Debug.logError("Couldn't roll back transaction " + e, MODULE);
            } catch (java.lang.IllegalStateException e) {
                Debug.logError("Error state of transcation " + e, MODULE);
            } finally {
                // all hibernate exception is fatal, we need reopen session to
                // continue other work.
                reOpenSession();
            }
        }
        // verify that none TestEntityModifyHistory could be found, because it was dropped by rollback
        Debug.logInfo("check if we have any TestEntityModifyHistory which testId = '" + testId + "'", MODULE);
        String hql = "from TestEntityModifyHistory eo where eo.testId = '" + testId + "'";
        Query query = session.createQuery(hql);
        List<TestEntity> list = query.list();
        assertEquals("Should not have found any TestEntityModifyHistory values with testId [" + testId + "]", 0, list.size());
    }

    /**
     * create and save a TestEntity.
     *
     * @param stringField the stringField value
     * @throws Exception if an error occurs
     * @return TestEntity
     */
    public TestEntity createAndSaveTestEntity(String stringField) throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        UserTransaction tx = session.beginUserTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField(stringField);
        session.save(testEntity);
        session.flush();
        tx.commit();
        return testEntity;
    }

    /**
     * Tests inserting a new TestEntity and finding it again by Hibernate.
     *
     * @throws Exception if an error occurs
     */
    public void testInsertTestEntity() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        Transaction tx = session.beginTransaction();
        TestEntity newTestEntity = new TestEntity();
        newTestEntity.setTestStringField("testInsertTestEntity string field");
        session.save(newTestEntity);
        session.flush();
        tx.commit();

        TestEntity loadEntity = (TestEntity) session.get(TestEntity.class, newTestEntity.getTestId());
        assertNotNull("Cannot find TestEntity with Id " + newTestEntity.getTestId() + " in hibernate", loadEntity);
    }

    /**
     * Tests updating an existing TestEntity created by createTestData() by
     * Hibernate.
     *
     * @throws Exception if an error occurs
     */
    public void testUpdateTestEntity() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        Transaction tx = session.beginTransaction();
        TestEntity loadEntity = (TestEntity) session.get(TestEntity.class, testEntityId1);
        assertNotNull("Cannot find the test entity [" + testEntityId1 + "]", loadEntity);
        assertEquals("Correct value should is old string value", loadEntity.getTestStringField(), "old value");
        loadEntity.setTestStringField("new value");
        session.update(loadEntity);
        session.flush();
        tx.commit();

        loadEntity = (TestEntity) session.get(TestEntity.class, testEntityId1);
        assertNotNull("Cannot find the test entity [" + testEntityId1 + "]", loadEntity);
        assertEquals("Correct value should is new string value", loadEntity.getTestStringField(), "new value");
    }

    /**
     * Tests removing a TestEntity created by createTestData() by Hibernate.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testRemoveTestEntity() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        Transaction tx = session.beginTransaction();
        TestEntity demoTestEntity2 = (TestEntity) session.get(TestEntity.class, testEntityId2);
        assertNotNull("Cannot find the test entity [" + testEntityId2 + "]", demoTestEntity2);
        assertNotNull("TestEntity with Id " + testEntityId2 + " should can retrieve by hibernate", demoTestEntity2);
        session.delete(demoTestEntity2);
        session.flush();
        tx.commit();

        String hql = "from TestEntity eo where eo.testId='" + testEntityId2 + "'";
        Query query = session.createQuery(hql);
        List<TestEntity> list = query.list();
        assertEquals("TestEntity with Id " + testEntityId2 + " should not exist", 0, list.size());
    }

    /**
     * Tests foreign key checking: an insert which violates a foreign key should
     * fail.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testForeignKeyChecking() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // try to create a new TestEntity with enumId="NO_SUCH_VALUE"
        Transaction tx = session.beginTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setEnumId("NO_SUCH_VALUE");
        testEntity.setTestStringField("testForeignKeyChecking");
        try {
            session.save(testEntity);
            session.flush();
            tx.commit();
        } catch (HibernateException ex) {
            Debug.logError("Call testForeignKeyChecking() cause " + ex, MODULE);
            try {
                tx.rollback();
            } catch (HibernateException e) {
                Debug.logError("Couldn't roll back transaction " + e, MODULE);
            } finally {
                // all hibernate exception is fatal, we need reopen session to
                // continue other work.
                reOpenSession();
            }
        }
        // verify that the entity cannot be created
        String hql = "from TestEntity eo where eo.enumId='NO_SUCH_VALUE'";
        Query query = session.createQuery(hql);
        List<TestEntity> list = query.list();
        assertEquals("TestEntity with enumId=\"NO_SUCH_VALUE\" should not exist", 0, list.size());
    }

    /**
     * Tests basic transaction commit.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testBasicTransactionCommit() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // store TestEntity ids
        String ids = "";
        // open a transaction
        Transaction tx = session.beginTransaction();
        // try to create 10 TestEntity values with auto-sequenced keys
        for (int i = 0; i < 10; i++) {
            TestEntity testEntity = new TestEntity();
            testEntity.setTestStringField("string value");
            session.save(testEntity);
            ids = ids.equals("") ? "'" + testEntity.getTestId() + "'" : ids + ",'" + testEntity.getTestId() + "'";
        }
        session.flush();
        // commit the transaction
        tx.commit();

        // verify that all 10 values could be found
        String hql = "from TestEntity eo where eo.testId in (" + ids + ")";
        Query query = session.createQuery(hql);
        List<TestEntity> list = query.list();
        assertEquals("Should have found 10 TestEntity values with query [" + hql + "]", 10, list.size());
    }

    /**
     * Tests basic transaction rollback to make sure values are not created.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testBasicTransactionRollback() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // store TestEntity ids
        String ids = "";
        // open a transaction
        Transaction tx = session.beginTransaction();
        // try to create 10 TestEntity values with auto-sequenced keys
        for (int i = 0; i < 10; i++) {
            TestEntity testEntity = new TestEntity();
            testEntity.setTestStringField("string value");
            session.save(testEntity);
            ids = ids.equals("") ? "'" + testEntity.getTestId() + "'" : ids + ",'" + testEntity.getTestId() + "'";
        }
        // rollback the transaction
        tx.rollback();

        // verify that none of the 10 values could be found
        String hql = "from TestEntity eo where eo.testId in (" + ids + ")";
        Query query = session.createQuery(hql);
        List<TestEntity> list = query.list();
        assertEquals("Shouldn't have found any TestEntity with query [" + hql + "]", 0, list.size());
    }

    /**
     * Tests transaction rollback caused by foreign key violation.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testTransactionForeignKeyRollback() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // store TestEntity ids
        String ids = "";
        // open a transaction
        Transaction tx = session.beginTransaction();
        // try to create 10 TestEntity values with auto-sequenced keys
        boolean shouldFail = false;
        try {
            for (int i = 0; i < 10; i++) {
                TestEntity testEntity = new TestEntity();
                testEntity.setTestStringField("string value");
                session.save(testEntity);
                ids = ids.equals("") ? "'" + testEntity.getTestId() + "'" : ids + ",'" + testEntity.getTestId() + "'";
            }
            // try to create a new TestEntity with enumId="NO_SUCH_VALUE"
            shouldFail = true;
            TestEntity testEntity = new TestEntity();
            testEntity.setEnumId("NO_SUCH_VALUE");
            testEntity.setTestStringField("testTransactionForeignKeyRollback");
            session.save(testEntity);
            ids += ",'" + testEntity.getTestId() + "'";
            session.flush();
            tx.commit();
        } catch (HibernateException ex) {
            // this exception is expected when inserting the "NO_SUCH_VALUE"
            if (!shouldFail) {
                Debug.logError(ex, MODULE);
                fail("Got HibernateException before trying to create the entity that should fail");
            }
            try {
                tx.rollback();
            } catch (HibernateException e) {
                Debug.logError("Couldn't rool back transcation " + e, MODULE);
            } finally {
                // all hibernate exception is fatal, we need reopen session to
                // continue other work.
                reOpenSession();
            }
        }
        // verify that none of the 11 values could be found
        String hql = "from TestEntity eo where eo.testId in (" + ids + ")";
        Debug.logInfo("query of testTransactionForeignKeyRollback : " + hql, MODULE);
        Query query = session.createQuery(hql);
        List<TestEntity> list = query.list();
        assertEquals("Shouldn't found any TestEntity with search by " + hql, 0, list.size());
    }

    /**
     * Tests transaction roll back due to timeout.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testTransactionTimeoutRollback() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // store TestEntity ids
        String ids = "";
        // open a transaction
        Transaction tx = session.getTransaction();
        tx.setTimeout(10);
        tx.begin();
        try {
            // try to create 10 TestEntity values with auto-sequenced keys, but
            // waiting 5 seconds between creating each entity
            for (int i = 0; i < 10; i++) {
                TestEntity testEntity = new TestEntity();
                testEntity.setTestStringField("string value");
                Thread.sleep(5000);
                session.save(testEntity);
                ids = ids.equals("") ? "'" + testEntity.getTestId() + "'" : ids + ",'" + testEntity.getTestId() + "'";
            }
            session.flush();
            // try to close the transaction but this should not work because we
            // should have been rolled back already
            tx.commit();
        } catch (HibernateException ex) {
            Debug.logInfo("A timeout exception is expected here", MODULE);
            Debug.logError(ex, MODULE);
            try {
                tx.rollback();
            } catch (HibernateException e) {
                Debug.logError("Couldn't roll back transaction " + e, MODULE);
            } finally {
             // all hibernate exception is fatal, we need reopen session to
                // continue other work.
                reOpenSession();
            }
        }

        // verify that none of the 10 values could be found
        String hql = "from TestEntity eo where eo.testId in (" + ids + ")";
        Query query = session.createQuery(hql);
        List<TestEntity> list = query.list();
        assertEquals("Shouldn't have found any TestEntity with query [" + hql + "]", 0, list.size());
    }

    /**
     * Tests transaction completes successfully with longer timeout.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testTransactionSetTimeout() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // store TestEntity ids
        String ids = "";
        // open a transaction
        Transaction tx = session.getTransaction();
        // set the transaction timeout to 100 seconds
        tx.setTimeout(100);
        tx.begin();
        try {
            // try to create 10 TestEntity values with auto-sequenced keys, but
            // waiting 5 seconds between creating each entity
            for (int i = 0; i < 10; i++) {
                TestEntity testEntity = new TestEntity();
                testEntity.setTestStringField("string value");
                Thread.sleep(5000);
                session.save(testEntity);
                ids = ids.equals("") ? "'" + testEntity.getTestId() + "'" : ids + ",'" + testEntity.getTestId() + "'";
            }
            session.flush();
            // commit the transaction
            tx.commit();
        } catch (HibernateException ex) {
            Debug.logError(ex, MODULE);
            try {
                tx.rollback();
            } catch (HibernateException e) {
                Debug.logError("Couldn't rool back transcation " + e, MODULE);
            } finally {
             // all hibernate exception is fatal, we need reopen session to
                // continue other work.
                reOpenSession();
            }
        }
        // verify that all 10 values could be found
        String hql = "from TestEntity eo where eo.testId in (" + ids + ")";
        Query query = session.createQuery(hql);
        List<TestEntity> list = query.list();
        assertEquals("Should have found 10 TestEntity with [" + hql + "]", 10, list.size());
    }

    /**
     * Test to verify the ability to go from an entity to related entities and
     * back.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testGetRelated() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // store TestEntityItem.testEntityItemSeqId
        String testEntityItemSeqIds = "";
        // open a transaction
        Transaction tx = session.getTransaction();
        tx.begin();
        // create a TestEntity
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("testGetRelated");
        session.save(testEntity);
        Debug.logInfo("testGetRelated(), testEntity Id :" + testEntity.getTestId(), MODULE);
        // create 10 TestEntityItem values with the same testEntityId but
        // different testItemSeqId
        try {
            for (int i = 0; i < 10; i++) {
                TestEntityItem testEntityItem = new TestEntityItem();
                testEntityItem.setTestEntityId(testEntity.getTestId());
                testEntityItem.setTestEntityItemSeqId("000" + i);
                session.save(testEntityItem);
                testEntityItemSeqIds = testEntityItemSeqIds.equals("")
                        ? "'" + testEntityItem.getTestEntityItemSeqId() + "'"
                        : testEntityItemSeqIds + ",'" + testEntityItem.getTestEntityItemSeqId() + "'";
            }
            session.flush();
            tx.commit();
        } catch (HibernateException ex) {
            Debug.logError(ex, MODULE);
            Debug.logError("create 10 TestEntityItem values with the testEntity " + testEntity.getTestId() + " failed.", MODULE);
            try {
                tx.rollback();
            } catch (HibernateException e) {
                Debug.logError("Call testGetRelated() couldn't roll back transaction " + e, MODULE);
            } finally {
                // all hibernate exception is fatal, we need reopen session to
                // continue other work.
                reOpenSession();
            }
        }
        // verify that you can get all 10 TestEntityItem values from
        // TestEntity.getTestEntityItem()
        session.refresh(testEntity);
        Debug.logInfo("testEntity.getTestEntityItems() is null : " + (testEntity.getTestEntityItems() == null), MODULE);
        assertEquals("Should found 10 TestEntityItem in search by getTestEntityItems()", 10, testEntity.getTestEntityItems().size());
        // verify that from each TestEntityItem, we can retrieve the original
        // TestEntity
        for (TestEntityItem testEntityItem : testEntity.getTestEntityItems()) {
            assertEquals("testEntityItem.getTestEntity() should be " + testEntity.getTestId(), testEntity.getTestId(), testEntityItem.getTestEntity().getTestId());
        }
        // verify that you can get a related TestEntityItem from TestEntity with
        // its testItemSeqId
        String hql = "from TestEntityItem eo where eo.testEntity.testId='" + testEntity.getTestId() + "' and eo.id.testEntityItemSeqId in (" + testEntityItemSeqIds + ")";
        Query query = session.createQuery(hql);
        List<TestEntityItem> list = query.list();
        assertEquals("Should found 10 TestEntityItem with search by " + hql, 10, list.size());
        // verify that if you try to get a related TestEntityItem with the wrong
        // testItemSeqId, it will return not found
        hql = "from TestEntityItem eo where eo.testEntity.testId='" + testEntity.getTestId() + "' and eo.id.testEntityItemSeqId in ('xxx')";
        query = session.createQuery(hql);
        list = query.list();
        assertEquals("Shouldn't have found any TestEntity with [" + hql + "]", 0, list.size());
    }

    /**
     * Test to verify that you can go from an entity to a related entity and
     * modify the value of the related entity successfully.
     *
     * @throws Exception if an error occurs
     */
    public void testGetRelatedAndUpdateValue() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        Transaction tx = session.getTransaction();
        tx.begin();
        // create first TestEntity
        TestEntity firstTestEntity = new TestEntity();
        firstTestEntity.setTestStringField("firstTestEntity");
        session.save(firstTestEntity);
        Debug.logInfo("firstTestEntity Id :" + firstTestEntity.getTestId(), MODULE);
        // create second TestEntity
        TestEntity secondTestEntity = new TestEntity();
        secondTestEntity.setTestStringField("old value");
        session.save(secondTestEntity);
        Debug.logInfo("secondTestEntity Id :" + secondTestEntity.getTestId(), MODULE);
        // create a TestEntityItem for the first TestEntity
        TestEntityItem testEntityItem = new TestEntityItem();
        testEntityItem.setTestEntityId(firstTestEntity.getTestId());
        testEntityItem.setTestEntityItemSeqId("0001");
        session.save(testEntityItem);
        session.flush();
        tx.commit();
        // verify that from the TestEntityItem you can get the first TestEntity
        Debug.logInfo("testEntityItem.getTestEntity() is null : " + (testEntityItem.getTestEntity() == null), MODULE);
        assertEquals("Should get the first TestEntity by relation", firstTestEntity.getTestId(), testEntityItem.getTestEntity().getTestId());

        // update the second TestEntity and store it
        secondTestEntity.setTestStringField("new value");
        tx.begin();
        session.save(secondTestEntity);
        session.flush();
        tx.commit();
        // retrieve the second TestEntity and verify that its value has been
        // updated
        secondTestEntity = (TestEntity) session.get(TestEntity.class, secondTestEntity.getTestId());
        assertEquals("secondTestEntity.getTestStringField() should be new value", "new value", secondTestEntity.getTestStringField());
    }

    /**
     * Test to verify we can retrieve/store blob field in hibernate codes successfully.
     *
     * @throws Exception if an error occurs
     */
    public void testBlobFields() throws Exception {
        reOpenSession();
        UserTransaction tx = session.beginUserTransaction();
        File file1 = new File("opentaps/opentaps-common/webapp/images/opentaps_logo.png");
        File file2  = new File("opentaps/opentaps-common/webapp/images/osslogo_small.jpg");
        byte[] data1 = getBytesFromFile(file1);
        byte[] data2 = getBytesFromFile(file2);
        String fileCRCCode1 = getCRCCode(data1);
        String fileCRCCode2 = getCRCCode(data2);

        // create a TestEntity and set field values
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("testBlobFields-step1");
        testEntity.setTestBlobField(data1);
        session.save(testEntity);
        session.flush();
        tx.commit();
        reOpenSession();

        // reload testEntity from Database
        testEntity = (TestEntity) session.get(TestEntity.class, testEntity.getTestId());
        // verify hibernate can retrieve blob field from entity
        String currentCRCCode = getCRCCode(testEntity.getTestBlobField());
        assertEquals("hibernate should retrieve same blob field value from entity, crc32 code : " + fileCRCCode1, fileCRCCode1, currentCRCCode);

        //update string field
        tx.begin();
        testEntity.setTestStringField("testBlobFields-step2");
        session.save(testEntity);
        session.flush();
        tx.commit();

        reOpenSession();
        // reload testEntity from Database
        testEntity = (TestEntity) session.get(TestEntity.class, testEntity.getTestId());
        // verify hibernate can retrieve blob field from entity
        currentCRCCode = getCRCCode(testEntity.getTestBlobField());
        assertEquals("testBlobField should not change.", fileCRCCode1, currentCRCCode);

        //update blob field
        tx.begin();
        testEntity.setTestStringField("testBlobFields-step3");
        testEntity.setTestBlobField(data2);
        session.save(testEntity);
        session.flush();
        tx.commit();

        reOpenSession();
        // reload testEntity from Database
        testEntity = (TestEntity) session.get(TestEntity.class, testEntity.getTestId());
        // verify hibernate can retrieve blob field from entity
        currentCRCCode = getCRCCode(testEntity.getTestBlobField());
        assertEquals("testBlobField should change to " + fileCRCCode2 + ".", fileCRCCode2, currentCRCCode);

    }


    /**
     * Test to verify that you can save store and retrieve values in all the
     * field types in framework/entity/fieldtype/fieldtype_postgres.xml
     * successfully.
     *
     * @throws Exception if an error occurs
     */
    public void testAllMajorFieldTypes() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        Transaction tx = session.getTransaction();
        tx.begin();
        String testStringField = "test string";

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date timeDate = sdf.parse("2009-3-11 23:45:13");
        Timestamp testDateTimeField = new java.sql.Timestamp(timeDate.getTime());

        Long testNumericField = new Long(123456789);
        BigDecimal testFloatingPointField = new BigDecimal("98765432.12").setScale(DECIMALS, ROUNDING);
        // store a file into the blob
        File file = new File("opentaps/opentaps-common/webapp/images/opentaps_logo.png");
        byte[] data = getBytesFromFile(file);
        String oldCRCCode = getCRCCode(data);

        String testCreditCardNumberField = "4013 8663 6050 0822";
        String testCreditCardDateField = "11/10";
        String testEmailField = "sparksun@opensourcestrategies.com";
        String testUrlField = "http://www.opentaps.org";
        String testTelphoneField = "1 310 4512-4875";

        // create a TestEntity and set field values
        TestEntity testEntity = new TestEntity();
        testEntity.setTestBlobField(data);
        testEntity.setTestCreditCardDateField(testCreditCardDateField);
        testEntity.setTestCreditCardNumberField(testCreditCardNumberField);
        testEntity.setTestDateTimeField(testDateTimeField);
        testEntity.setTestEmailField(testEmailField);
        testEntity.setTestFloatingPointField(testFloatingPointField);
        testEntity.setTestNumericField(testNumericField);
        testEntity.setTestStringField(testStringField);
        testEntity.setTestTelphoneField(testTelphoneField);
        testEntity.setTestUrlField(testUrlField);
        session.save(testEntity);
        session.flush();
        tx.commit();
        reOpenSession();
        // reload testEntity from Database
        testEntity = (TestEntity) session.get(TestEntity.class, testEntity.getTestId());
        // verify hibernate can retrieve blob field from entity
        String newCRCCode = getCRCCode(testEntity.getTestBlobField());
        Debug.logInfo("old crc32 is :" + oldCRCCode + ", new crc 32 is : " + newCRCCode, MODULE);
        assertEquals("hibernate should retrieve same blob field value from entity, crc32 code : " + oldCRCCode, oldCRCCode, newCRCCode);
        // verify hibernate can retrieve credit card date field from entity
        assertEquals("hibernate should retrieve same credit card date field value from entity.", testCreditCardDateField, testEntity.getTestCreditCardDateField());
        // verify hibernate can retrieve credit card number field from entity
        assertEquals("hibernate should retrieve same credit card number value from entity.", testCreditCardNumberField, testEntity.getTestCreditCardNumberField());
        // verify hibernate can retrieve timestamp field from entity
        assertEquals("hibernate should retrieve same timestamp field value from entity.", testDateTimeField, testEntity.getTestDateTimeField());
        // verify hibernate can retrieve email field from entity
        assertEquals("hibernate should retrieve same email field value from entity.", testEmailField, testEntity.getTestEmailField());
        // verify hibernate can retrieve floating point field from entity, round the field value for it might lost accuracy after store in database
        assertEquals("hibernate should retrieve same floating point field value from entity.", testEntity.getTestFloatingPointField().setScale(DECIMALS, ROUNDING), testFloatingPointField);
        // verify hibernate can retrieve numerict field from entity
        assertEquals("hibernate should retrieve same numeric field value from entity.", testNumericField, testEntity.getTestNumericField());
        // verify hibernate can retrieve string field from entity
        assertEquals("hibernate should retrieve same string field value from entity.", testStringField, testEntity.getTestStringField());
        // verify hibernate can retrieve telphone field from entity
        assertEquals("hibernate should retrieve same telphone field value from entity.", testTelphoneField, testEntity.getTestTelphoneField());
        // verify hibernate can retrieve url field from entity
        assertEquals("hibernate should retrieve same url field value from entity.", testUrlField, testEntity.getTestUrlField());
    }

    /**
     * Test to verify that you can cascade persist/delete collection items.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testCascadePersistAndDelete() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        Transaction tx = session.getTransaction();
        tx.begin();
        // create a TestEntity
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("string value");
        session.save(testEntity);
        String testEntityId = testEntity.getTestId();
        // create 10 TestEntityItem values with the same testEntityId but
        // different testItemSeqId
        createTestEntityItems(testEntity, 10);
        // NOTICE: should be persist, not save. persist will cascade down and
        // save all the TestEntityItem
        session.persist(testEntity);
        session.flush();
        tx.commit();

        // reload TestEntity from hibernate
        testEntity = (TestEntity) session.get(TestEntity.class, testEntityId);
        // verify that you can get all 10 TestEntityItem values from
        // TestEntity.getTestEntityItem()
        assertEquals("Should found 10 TestEntityItem values from TestEntity.getTestEntityItem()", 10, testEntity.getTestEntityItems().size());

        tx.begin();
        // Delete the TestEntity which should cause a cascading delete of all
        // its related TestEntityItem
        session.delete(testEntity);
        session.flush();
        tx.commit();
        String hql = "from TestEntityItem eo where eo.id.testEntityId='" + testEntityId + "'";
        Query query = session.createQuery(hql);
        List<TestEntityItem> list = query.list();
        // verify that TestEntity.delete has cascade delete all of testEntityItem
        assertEquals("Shouldn't found any TestEntityItem with search by " + hql, 0, list.size());
    }

    /**
     * Test to verify that you can cascade remove/clear collection item.
     *
     * @throws Exception if an error occurs
     */
    public void testCascadeCollection() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        Transaction tx = session.getTransaction();
        tx.begin();
        // create a TestEntity
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("string value");
        session.save(testEntity);
        String testEntityId = testEntity.getTestId();
        // create 10 TestEntityItem values with the same testEntityId but
        // different testItemSeqId
        createTestEntityItems(testEntity, 10);
        // NOTICE: should be persist, don't use save method, it would cascade
        // save testEntityItems
        session.persist(testEntity);
        session.flush();
        tx.commit();
        // remove one TestEntityItem
        tx.begin();
        TestEntityItem firstTestEntityItem = testEntity.getTestEntityItems().get(0);
        testEntity.removeTestEntityItem(firstTestEntityItem);
        session.persist(testEntity);
        session.flush();
        tx.commit();
        // reload TestEntity from hibernate
        testEntity = (TestEntity) session.get(TestEntity.class, testEntityId);
        // verify that you can get all 9 TestEntityItem values from
        // TestEntity.getTestEntityItem()
        assertEquals("Should found 9 TestEntityItem values from TestEntity.getTestEntityItem()", 9, testEntity.getTestEntityItems().size());

        // clear all TestEntityItem
        tx.begin();
        testEntity.clearTestEntityItem();
        session.persist(testEntity);
        session.flush();
        tx.commit();
        // reload TestEntity from hibernate
        testEntity = (TestEntity) session.get(TestEntity.class, testEntityId);
        // verify that you cannot found any TestEntityItem from
        // TestEntity.getTestEntityItem()
        assertEquals("Shouldn't found any TestEntityItem values from TestEntity.getTestEntityItem()", 0, testEntity.getTestEntityItems().size());
    }

    /**
     * Gets a file crc32 code.
     * @param bytes a <code>byte[]</code> value
     * @throws Exception if an error occurs
     * @return a <code>String</code> CRC32 code
     */
    public static String getCRCCode(byte[] bytes) throws Exception {
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        return Long.toHexString(crc32.getValue());
    }

    /**
     * Create 10 TestEntityItem values for testEntity.
     * @param testEntity a <code>TestEntity</code> value
     * @param count number of TestEntityItem
     */
    public void createTestEntityItems(TestEntity testEntity, int count) {
        for (int i = 0; i < count; i++) {
            TestEntityItem testEntityItem = new TestEntityItem();
            testEntityItem.setTestEntityId(testEntity.getTestId());
            testEntityItem.setTestEntityItemSeqId("000" + i);
            testEntityItem.setItemValue("value " + i);
            testEntityItem.setTestEntity(testEntity);
            testEntity.addTestEntityItem(testEntityItem);
        }
    }

    /**
     * Test to verify that you can query view-entity by HQL.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testViewEntity() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // create a TestEntity
        Transaction tx = session.getTransaction();
        tx.begin();
        // create a TestEntity
        TestEntity testEntity = new TestEntity();
        testEntity.setTestStringField("testViewEntity");
        session.save(testEntity);
        String testEntityId = testEntity.getTestId();
        // create 10 TestEntityItem associated with the TestEntity, each with
        // its own value
        createTestEntityItems(testEntity, 10);
        session.persist(testEntity);
        session.flush();
        tx.commit();
        // get all TestEntityAndItem view entities with the TestEntity's ID
        Query query = session.createQuery("from TestEntityAndItem eo where eo.testId='" + testEntityId + "' order by eo.testEntityItemSeqId");
        List<TestEntityAndItem> list = query.list();
        // Verify that there are 10 TestEntityAndItem
        assertEquals("Should found 10 TestEntityAndItem with search testId = " + testEntityId, 10, list.size());
        // verify that the value of each TestEntityAndItem is the correct one
        for (int i = 0; i < 10; i++) {
            TestEntityAndItem testEntityAndItem = list.get(i);
            assertEquals("the TestEntityAndItem.getValue() should be 'value " + i + "'", "value " + i, testEntityAndItem.getItemValue());
        }
    }

    /**
     * Test to verify that you can query rel-optional="true" view-entity by HQL.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testRelOptionalViewEntity() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        // get all PartyContactInfo view entities size
        Query query = session.createQuery("from PartyContactInfo eo");
        List<PartyContactInfo> list = query.list();
        int allSize = list.size();

        // get all PartyContactInfo view entities size
        query = session.createQuery("from PartyContactInfo eo where eo.partyId is not null");
        list = query.list();
        int hasPartySize = list.size();

        // get partyClassificationGroupId is not null size
        query = session.createQuery("from PartyContactInfo eo where eo.partyClassificationGroupId is not null");
        list = query.list();
        int hasPartyClassificationSize = list.size();

        // Verify that allSize == allHasPartySize
        Debug.logInfo("count of PartyContactInfo is " + allSize + ", count of has Party is " + hasPartySize, MODULE);
        assertTrue("Because PartyClassification is rel-optional=\"false\", so allSize[" + allSize + "] should equals hasPartySize[" + hasPartySize + "]", allSize == hasPartySize);

        // Verify that allSize > hasPartyClassificationSize
        Debug.logInfo("count of PartyContactInfo is " + allSize + ", count of has PartyClassification is " + hasPartyClassificationSize, MODULE);
        assertTrue("Because PartyClassification is rel-optional=\"true\", so allSize[" + allSize + "] should large than hasPartyClassificationSize[" + hasPartyClassificationSize + "]", allSize > hasPartyClassificationSize);
    }

    /**
     * Tests that after hibernate is used to create a value, ofbiz entity engine's find from cache method can retrieve the value.
     * @throws Exception if an error occurs
     */
    public void testHibernateCreateRefreshesOfbizCache() throws Exception {
        reOpenSession();
        String originalDescription = "Original description for TestEntity.testStringField";
        TestEntity originalTestEntity = createAndSaveTestEntity(originalDescription);
        GenericValue originalTestEntityGV = delegator.findByPrimaryKeyCache("TestEntity", UtilMisc.toMap("testId", originalTestEntity.getTestId()));
        assertEquals("Test string field from generic value retrieved after TestEntity is created is not correct", originalTestEntityGV.getString("testStringField"), originalDescription);
    }

    /**
     * Tests that after hibernate updates a value, ofbiz entity engine's find from cache method will retrieve the updated value: ie, it has been
     * updated in the ofbiz entity engine cache.
     * @throws Exception if an error occurs
     */
    public void testHibernateUpdateRefreshesOfbizCache() throws Exception {
        reOpenSession();
        // create the original entity
        String originalDescription = "Original test entity description";
        TestEntity originalTestEntity = createAndSaveTestEntity(originalDescription);
        // this is important: the first load puts it into the ofbiz entity engine cache
        GenericValue originalTestEntityGV = delegator.findByPrimaryKeyCache("TestEntity", UtilMisc.toMap("testId", originalTestEntity.getTestId()));

        // now update the description field
        String newDescription = "New test entity description";
        Transaction tx = session.beginTransaction();
        TestEntity reloadedTestEntity = (TestEntity) session.get(TestEntity.class, originalTestEntity.getTestId());
        reloadedTestEntity.setTestStringField(newDescription);
        session.update(reloadedTestEntity);
        session.flush();
        tx.commit();

        // load it again, this time it should come into the cache
        GenericValue reloadedTestEntityGV = delegator.findByPrimaryKeyCache("TestEntity", UtilMisc.toMap("testId", originalTestEntity.getTestId()));

        assertEquals("Test string field from original and reloaded TestEntity do not equal", reloadedTestEntity.getTestStringField(), originalTestEntity.getTestStringField());
        assertEquals("Test string field from reloaded TestEntity and generic value retrieved after TestEntity is updated do not equal", reloadedTestEntityGV.getString("testStringField"), reloadedTestEntity.getTestStringField());
    }

    /**
     * Tests that after hibernate removes a value, ofbiz entity engine's find from cache method will also no longer have it: ie, it has been
     * removed from the ofbiz entity engine cache.
     * @throws Exception if an error occurs
     */
    public void testHibernateRemoveRefreshesOfbizCache() throws Exception {
        reOpenSession();
        // create the original entity
        String originalDescription = "Original test entity description";
        TestEntity originalTestEntity = createAndSaveTestEntity(originalDescription);
        // this is important: the first load puts it into the ofbiz entity engine cache
        GenericValue originalTestEntityGV = delegator.findByPrimaryKeyCache("TestEntity", UtilMisc.toMap("testId", originalTestEntity.getTestId()));

        // now delete the test entity using hibernate
        Transaction tx = session.beginTransaction();
        session.delete(originalTestEntity);
        session.flush();
        tx.commit();

        // check if the ofbiz entity engine's cache method still has this value around
        GenericValue reloadedTestEntityGV = delegator.findByPrimaryKeyCache("TestEntity", UtilMisc.toMap("testId", originalTestEntity.getTestId()));
        assertNull(reloadedTestEntityGV);

    }

   /**
    * Tests that after ofbiz delegator is used to create a value, hibernate find from cache method can retrieve the value.
    * @throws Exception if an error occurs
    */
   public void testOfbizCreateRefreshesHibernateCache() throws Exception {
       reOpenSession();
       String originalDescription = "Original description for TestEntity.testStringField";
       String testId = delegator.getNextSeqId("TestEntity");
       GenericValue testEntityGV = delegator.create("TestEntity", UtilMisc.toMap("testId", testId, "testStringField", originalDescription));

       TestEntity testEntity = (TestEntity) session.get(TestEntity.class, testId);
       assertEquals("Test string field from generic value retrieved after TestEntity is created is not correct", testEntity.getTestStringField(), testEntityGV.getString("testStringField"));
   }

   /**
    * Tests that after ofbiz updates a value, hibernate load method will retrieve the updated value: ie, it has been
    * updated in the hibernate cache.
    * @throws Exception if an error occurs
    */
   public void testOfbizUpdateRefreshHibernateCache() throws Exception {
       reOpenSession();
       // create the original entity
       String originalDescription = "Original test entity description";
       String testId = delegator.getNextSeqId("TestEntity");
       GenericValue testEntityGV = delegator.create("TestEntity", UtilMisc.toMap("testId", testId, "testStringField", originalDescription));

       // this is important: the first load puts it into the hibernate cache
       TestEntity testEntity = (TestEntity) session.get(TestEntity.class, testId);
       // now update the description field by ofbiz
       String newDescription = "New test entity description";
       testEntityGV.setString("testStringField", newDescription);
       testEntityGV.store();

       // this is important: We need reopen hibernate session for load the change come from ofbiz's engine
       // create new hibernate session is not expensive, if we keep a long session and try to update the 1st level cache, it will be more expensive than re-open it.
       reOpenSession();
       // reload it again by hibernate new session
       TestEntity reloadedTestEntity = (TestEntity) session.get(TestEntity.class, testId);

       // re-load it again by ofbiz entity engine
       GenericValue reloadedTestEntityGV = delegator.findByPrimaryKey("TestEntity", UtilMisc.toMap("testId", testId));
       assertEquals("Test string field from reloaded TestEntity and generic value retrieved after TestEntity is updated do not equal", reloadedTestEntityGV.getString("testStringField"), reloadedTestEntity.getTestStringField());
       assertEquals("Test string field from reloaded TestEntity not equals " + newDescription, newDescription, reloadedTestEntity.getTestStringField());
   }

   /**
    * Tests that after ofbiz delegator removes a value, hibernate retrieve method will also no longer have it: ie, it has been
    * removed from the hibernate 2nd cache.
    * @throws Exception if an error occurs
    */
   public void testOfbizRemoveRefreshesHibernateCache() throws Exception {
       reOpenSession();
       // create the original entity
       String originalDescription = "Original test entity description";
       String testId = delegator.getNextSeqId("TestEntity");
       GenericValue testEntityGV = delegator.create("TestEntity", UtilMisc.toMap("testId", testId, "testStringField", originalDescription));

       // this is important: the first load puts it into the hibernate cache
       TestEntity testEntity = (TestEntity) session.get(TestEntity.class, testId);
       // check we can get the TestEntity by hibernate
       assertNotNull(testEntity);

        // now delete the test entity using delegator
       delegator.removeByPrimaryKey(testEntityGV.getPrimaryKey());

       // check if the hibernate load still has this value around
       // this is important: We need reopen hibernate session for load the change come from ofbiz's engine
       // create new hibernate session is not expensive, if we keep a long session and try to update the 1st level cache, it will be more expensive than re-open it.
       reOpenSession();
       TestEntity reloadedTestEntity = (TestEntity) session.get(TestEntity.class, testId);
       assertNull(reloadedTestEntity);
   }
   
   /**
    * Tests hibernate can save the object with given primary key.
    * @throws Exception if an error occurs
    */
   public void testSaveTheObjectWithGivenId() throws Exception {
    // open a new session, if session has opened, then close it first
       reOpenSession();
       Transaction tx = session.beginTransaction();
       DataResource dataResource = new DataResource();
       dataResource.setDataResourceTypeId("ELECTRONIC_TEXT");
       dataResource.setDataTemplateTypeId("FTL");
       dataResource.setMimeTypeId("text/html");
       String dataResourceId = (String) session.save(dataResource);
       dataResource.setDataResourceId(dataResourceId);
       Debug.logInfo("create DataResource with dataResourceId [" + dataResourceId + "]", MODULE);
       
       ElectronicText electronicText = new ElectronicText();
       electronicText.setDataResourceId(dataResourceId);
       electronicText.setTextData("empty ftl");

       session.save(electronicText);
       session.flush();
       tx.commit();
       assertEquals("ElectronicText primary key dataResourceId not equals initial value after save.", electronicText.getDataResourceId(), dataResourceId);

   }   

    /**
     * Remove all test data from TestEntityItem and TestEntity.
     * @throws Exception if an error occurs
     */
    private void removeTestData() throws Exception {
        // open a new session, if session has opened, then close it first
        reOpenSession();
        UserTransaction tx = session.beginUserTransaction();
        String hql = "from TestEntity";
        Query query = session.createQuery(hql);
        List<TestEntity> list = query.list();
        for (TestEntity testEntity : list) {
            session.delete(testEntity);
        }
        session.flush();
        tx.commit();
        Debug.logInfo("removeTestData: deleted [" + list + "]", MODULE);
    }

    /**
     * create test data for tests which would update these fields.
     *
     * @throws Exception if an error occurs
     */
    private void createTestData() throws Exception {
        Debug.logInfo("createTestData", MODULE);
        // open a new session, if session has opened, then close it first
        reOpenSession();
        UserTransaction tx = session.beginUserTransaction();
        TestEntity newTestEntity1 = new TestEntity();
        newTestEntity1.setTestStringField("old value");
        session.save(newTestEntity1);
        testEntityId1 = newTestEntity1.getTestId();

        TestEntity newTestEntity2 = new TestEntity();
        newTestEntity2.setTestStringField("old value");
        session.save(newTestEntity2);
        testEntityId2 = newTestEntity2.getTestId();
        session.flush();
        tx.commit();
        Debug.logInfo("createTestData: created [" + testEntityId1 + "] and [" + testEntityId2 + "]", MODULE);
    }
    
    /**
     * Tests hibernate session getNextSeqId if sync with ofbiz
     * Gets next id from hibernate session firstly, then get next id from ofbiz
     * @throws Exception if an error occurs
     */    
    public void testHibernateGetNextSeqIdSyncWithOfbiz1() throws Exception {
        reOpenSession();
        // get testId from hibernate session firstly, then get testId from ofbiz
        String nextTestIdFromHibernate = session.getNextSeqId("TestEntity");
        String nextTestIdFromOfbiz = delegator.getNextSeqId("TestEntity");
        assertNotEquals("We should get different ids from hibernate and ofbiz delegator on call getNextSeqId at the same time.", nextTestIdFromHibernate, nextTestIdFromOfbiz);
    }

    /**
     * Tests hibernate session getNextSeqId if sync with ofbiz
     * Gets next id from ofbiz firstly, then get next id from hibernate session
     * @throws Exception if an error occurs
     */    
    public void testHibernateGetNextSeqIdSyncWithOfbiz2() throws Exception {
        reOpenSession();
        // get testId from ofbiz firstly, then get testId from hibernate session
        String nextTestIdFromOfbiz = delegator.getNextSeqId("TestEntity");
        String nextTestIdFromHibernate = session.getNextSeqId("TestEntity");
        assertNotEquals("We should get different ids from hibernate and ofbiz delegator on call getNextSeqId at the same time.", nextTestIdFromHibernate, nextTestIdFromOfbiz);
    }
}
