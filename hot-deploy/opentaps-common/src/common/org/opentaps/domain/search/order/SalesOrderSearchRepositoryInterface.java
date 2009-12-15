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
package org.opentaps.domain.search.order;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.opentaps.common.domain.order.OrderViewForListing;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for Orders to handle interaction of Search orders domain with the entity engine (database) and the service engine.
 */
public interface SalesOrderSearchRepositoryInterface extends RepositoryInterface {

    /**
     * Sets the order Id to search for.
     * @param orderId a <code>String</code> value
     */
    public void setOrderId(String orderId);

    /**
     * Sets the external order Id to search for.
     * @param externalOrderId a <code>String</code> value
     */
    public void setExteralOrderId(String externalOrderId);

    /**
     * Sets the status Id to search for.
     * @param statusId a <code>String</code> value
     */
    public void setStatusId(String statusId);

    /**
     * Sets the order name to search for.
     * @param orderName a <code>String</code> value
     */
    public void setOrderName(String orderName);

    /**
     * Sets the customer party Id to search for.
     * @param customerPartyId a <code>String</code> value
     */
    public void setCustomerPartyId(String customerPartyId);

    /**
     * Sets the product store Id to search for.
     * @param productStoreId a <code>String</code> value
     */
    public void setProductStoreId(String productStoreId);

    /**
     * Sets the purchase order Id to search for.
     * @param purchaseOrderId a <code>String</code> value
     */
    public void setPurchaseOrderId(String purchaseOrderId);

    /**
     * Sets the from date string to search for.
     * @param fromDate a <code>String</code> value
     */
    public void setFromDate(String fromDate);

    /**
     * Sets the thru date string to search for.
     * @param thruDate a <code>String</code> value
     */
    public void setThruDate(String thruDate);

    /**
     * Sets the createdBy to search for.
     * @param createdBy a <code>String</code> value
     */
    public void setCreatedBy(String createdBy);

    /**
     * Sets the lot Id to search for.
     * @param lotId a <code>String</code> value
     */
    public void setLotId(String lotId);

    /**
     * Sets the serialNumber to search for.
     * @param serialNumber a <code>String</code> value
     */
    public void setSerialNumber(String serialNumber);

    /**
     * Sets the organization party Id to search for.
     * @param organizationPartyId a <code>String</code> value
     */
    public void setOrganizationPartyId(String organizationPartyId);

    /**
     * Sets the viewPref to search for.
     * @param viewPref a <code>String</code> value
     */
    public void setViewPref(String viewPref);

    /**
     * Sets the find active orders only.
     * @param findAll a <code>String</code> value
     */
    public void setFindActiveOnly(boolean findActiveOnly);
    
    /**
     * Sets the find all desired orders only.
     * @param findAll a <code>String</code> value
     */
    public void setFindDesiredOnly(boolean findDesiredOnly);

    /**
     * Sets the userLoginId of current login to search for.
     * @param userLoginId a <code>String</code> value
     */
    public void setUserLoginId(String userLoginId);

    /**
     * Sets the locale for format date string.
     * @param locale a <code>Locale</code> value
     */
    public void setLocale(Locale locale);

    /**
     * Sets the timeZone for format date string.
     * @param timeZone a <code>TimeZone</code> value
     */
    public void setTimeZone(TimeZone timeZone);

    /**
     * Sets the orderBy list for sorting the result.
     * @param orderBy a <code>List<String></code> value
     */
    public void setOrderBy(List<String> orderBy);

    /**
     * Finds the list of <code>Order</code>.
     * @return list of orders
     * @throws RepositoryException if an error occurs
     */
    public List<OrderViewForListing> findOrders() throws RepositoryException;

}
