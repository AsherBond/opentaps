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

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Test case superclass for all entity engine tests.
 */
public class EntityEngineTests extends OpentapsTestCase {

    private static final String MODULE = EntityEngineTests.class.getName();
    @Override
    public void setUp() throws Exception {
        super.setUp();
        removeTestData(delegator);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        // delegator is reset to null by super.tearDown() so we have to get it again
        removeTestData(DelegatorFactory.getDelegator(OpentapsTestCase.DELEGATOR_NAME));
    }
    /**
     * Tests that after delegator is used to create a value, ofbiz entity engine's find from cache method can retrieve the value
     * @throws Exception
     */
    public void testDelegatorCreateRefreshesOfbizCache() throws Exception {
        String originalDescription = "Original description for TestEntity.testStringField";
        String testId = delegator.getNextSeqId("TestEntity");
        delegator.create("TestEntity", UtilMisc.toMap("testId", testId, "testStringField", originalDescription));
        GenericValue originalTestEntityGV = delegator.findByPrimaryKeyCache("TestEntity", UtilMisc.toMap("testId", testId));
        assertEquals("Test string field from generic value retrieved after generic value create is not correct", originalTestEntityGV.getString("testStringField"), originalDescription);
    }
    
    /**
     * Tests that after delegator updates a value, ofbiz entity engine's find from cache method will retrieve the updated value: ie, it has been
     * updated in the ofbiz entity engine cache
     * @throws Exception
     */
    public void testDelegatorUpdateRefreshesOfbizCache() throws Exception {
        // create the original entity
        String originalDescription = "Original test entity description";
        String testId = delegator.getNextSeqId("TestEntity");
        GenericValue testEntityGV = delegator.create("TestEntity", UtilMisc.toMap("testId", testId, "testStringField", originalDescription));
        // this is important: the first load puts it into the ofbiz entity engine cache
        GenericValue originalTestEntityGV = delegator.findByPrimaryKeyCache("TestEntity", UtilMisc.toMap("testId", testId));
        
        // now update the description field
        String newDescription = "New test entity description";
        testEntityGV.setString("testStringField", newDescription);
        testEntityGV.store();
        
        // load it again, this time it should come into the cache
        GenericValue reloadedTestEntityGV = delegator.findByPrimaryKeyCache("TestEntity", UtilMisc.toMap("testId", testId));

        assertEquals("Test string field from original and reloaded generic value from cache do not equal", reloadedTestEntityGV.getString("testStringField"), testEntityGV.getString("testStringField"));
    }

    
    /**
     * Tests that after delegator removes a value, ofbiz entity engine's find from cache method will also no longer have it: ie, it has been
     * removed from the ofbiz entity engine cache
     * @throws Exception
     */
    public void testDelegatorRemoveRefreshesOfbizCache() throws Exception {
        // create the original entity
        String originalDescription = "Original test entity description";
        String testId = delegator.getNextSeqId("TestEntity");
        GenericValue testEntityGv = delegator.create("TestEntity", UtilMisc.toMap("testId", testId, "testStringField", originalDescription));
        // this is important: the first load puts it into the ofbiz entity engine cache
        GenericValue originalTestEntityGV = delegator.findByPrimaryKeyCache("TestEntity", UtilMisc.toMap("testId", testId));
        
        // now delete the test entity using delegator
        delegator.removeByPrimaryKey(testEntityGv.getPrimaryKey());

        // check if the ofbiz entity engine's cache method still has this value around
        GenericValue reloadedTestEntityGV = delegator.findByPrimaryKeyCache("TestEntity", UtilMisc.toMap("testId", testId));
        assertNull(reloadedTestEntityGV);
    }
    
    /**
     * Remove all test data from TestEntityModifyHistory,TestEntityItem and TestEntity.
     * @throws Exception if an error occurs
     */
    private void removeTestData(Delegator delegator) throws GenericEntityException {
        delegator.removeAll("TestEntityItem");
        delegator.removeAll("TestEntity");
    }

}
