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
package org.opentaps.domain.order;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.opentaps.foundation.repository.LookupRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Repository to lookup Purchase Orders.
 */
public interface PurchaseOrderLookupRepositoryInterface extends LookupRepositoryInterface {

    /**
     * Sets the order Id to search for.
     * @param orderId a <code>String</code> value
     */
    public void setOrderId(String orderId);

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
     * Sets the product pattern to search for.
     * @param productPattern a <code>String</code> value
     */
    public void setProductPattern(String productPattern);

    /**
     * Sets the supplier party Id to search for.
     * @param supplierPartyId a <code>String</code> value
     */
    public void setSupplierPartyId(String supplierPartyId);

    /**
     * Sets the from date string to search for.
     * @param fromDate a <code>String</code> value
     */
    public void setFromDate(String fromDate);

    /**
     * Sets the from date string to search for.
     * @param fromDate a <code>Timestamp</code> value
     */
    public void setFromDate(Timestamp fromDate);

    /**
     * Sets the thru date string to search for.
     * @param thruDate a <code>String</code> value
     */
    public void setThruDate(String thruDate);

    /**
     * Sets the thru date string to search for.
     * @param thruDate a <code>Timestamp</code> value
     */
    public void setThruDate(Timestamp thruDate);

    /**
     * Sets the createdBy to search for.
     * @param createdBy a <code>String</code> value
     */
    public void setCreatedBy(String createdBy);

    /**
     * Sets the organization party Id to search for.
     * @param organizationPartyId a <code>String</code> value
     */
    public void setOrganizationPartyId(String organizationPartyId);

    /**
     * Sets the find all desired orders only.
     * @param findDesiredOnly a <code>boolean</code> value
     */
    public void setFindDesiredOnly(boolean findDesiredOnly);

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
