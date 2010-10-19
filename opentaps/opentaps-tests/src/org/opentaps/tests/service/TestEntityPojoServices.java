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
package org.opentaps.tests.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.UserTransaction;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.base.entities.TestEntity;
import org.opentaps.base.entities.TestEntityItem;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;

public class TestEntityPojoServices extends Service {

    private static final String module = TestEntityPojoServices.class.getName();
    //the description which set to TestEntityItem.description
    private String description;

    //number of item that need create
    private Integer numberOfItems;

    //testId list of new TestEntitys
    private List testIds;

    /**
     * Gets the testId created by the service.
     * @return the testId
     */
    public List getTestIds() {
        return testIds;
    }

    /**
     * Sets the required output parameter for service.
     * @param testIds the testId of TestEntity
     */
    public void setTestId(List testIds) {
        this.testIds = testIds;
    }

    /**
     * Gets the description created by the service.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the required input parameter for service.
     * @param description the description to testEntityItems for
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the numberOfItems created by the service.
     * @return the numberOfItems
     */
    public Integer getNumberOfItems() {
        return numberOfItems;
    }

    /**
     * Sets the required input parameter for service.
     * @param numberOfItems the numberOfItems TestEntityItem that need create
     */
    public void setNumberOfItems(Integer numberOfItems) {
        this.numberOfItems = numberOfItems;
    }


    /**
     * Default constructor.
     */
    public TestEntityPojoServices() {
    }

    /**
     * Using hibernate API, create a TestEntity with its description and create numberOfItems
     * TestEntityItem with session.getNextSeqId("TestEntityItemSeqId") as sequence ID.
     * Put the description in itemValue of all the test entity items as well.
     * Next, repeat with the ofbiz delegator
     * @throws ServiceException if an error occurs
     */
    public void createTestEntityAndItems() throws ServiceException {
        testIds = new ArrayList();
        try {
            //get own session to execute hibernate codes
            Session session = getInfrastructure().getSession();
            TestEntity testEntity = new TestEntity();
            //set description to TestStringField
            testEntity.setTestStringField(getDescription());
            //save TestEntity to get primarykey (testId)
            session.save(testEntity);
            //create numberOfItems TestEntityItem
            for (int i = 0; i < getNumberOfItems(); i++) {
                TestEntityItem testEntityItem = new TestEntityItem();
                testEntityItem.setTestEntityId(testEntity.getTestId());
                testEntityItem.setTestEntityItemSeqId(String.valueOf(i));
                testEntityItem.setItemValue(getDescription());
                testEntityItem.setTestEntity(testEntity);
                testEntity.addTestEntityItem(testEntityItem);
            }
            // for cascade store testEntityItems, so persist testEntity again
            session.persist(testEntity);
            session.flush();
            session.close();
            // add testId to return list
            testIds.add(testEntity.getTestId());

            //pause five minutes
            try {
                Thread.sleep(5 * 60 * 1000);
            } catch (InterruptedException e) {
                throw new ServiceException(e);
            }

            //create TestEntity by delegator
            Delegator delegator = getInfrastructure().getDelegator();
            String testId = delegator.getNextSeqId("TestEntity");
            delegator.create("TestEntity", UtilMisc.toMap("testId", testId, "testStringField", getDescription()));
            //create numberOfItems TestEntityItem
            for (int i = 0; i < getNumberOfItems(); i++) {
                delegator.create("TestEntityItem", UtilMisc.toMap("testEntityId", testId
                        , "testEntityItemSeqId", String.valueOf(i), "itemValue", getDescription()));
            }
            // add testId to return list
            testIds.add(testId);
        } catch (InfrastructureException e) {
            throw new ServiceException(e);
        } catch (GenericEntityException e) {
            throw new ServiceException(e);
        }
    }
}
