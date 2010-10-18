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

import org.opentaps.base.services.GetNextOrderIdService;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;

/**
 * A wrapper to getNextOrderId which sleeps to simulate load in order to test concurrenc.
 */
public class TestNextOrderId extends Service {

    private String partyId;
    private String productStoreId;

    private String orderId;

    /**
     * Gets the orderId returned by the "getNextOrderId" service.
     * @return the orderId
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Sets the partyId.
     * @param partyId the partyId
     */
    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    /**
     * Sets the productStoreId.
     * @param productStoreId the productStoreId
     */
    public void setProductStoreId(String productStoreId) {
        this.productStoreId = productStoreId;
    }


    /**
     * Default constructor.
     */
    public TestNextOrderId() {
    }

    /**
     * Calls "getNextOrderId", then sleeps for 10 seconds before returning in order to simulate load.
     * @throws ServiceException if an error occurs
     */
    public void getNextOrderId() throws ServiceException {
        try {
            GetNextOrderIdService service = new GetNextOrderIdService();
            service.setInProductStoreId(productStoreId);
            service.setInPartyId(partyId);
            runSync(service);

            Thread.sleep(10000);

            this.orderId = service.getOutOrderId();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
