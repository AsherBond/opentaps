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
package org.opentaps.domain.billing.invoice;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.opentaps.foundation.repository.LookupRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Repository to lookup Invoices.
 */
public interface InvoiceLookupRepositoryInterface extends LookupRepositoryInterface {

    /**
     * Sets the invoice type Id to search for.
     * @param invoiceTypeId a <code>String</code> value
     */
    public void setInvoiceTypeId(String invoiceTypeId);

    /**
     * Sets the invoice Id to search for.
     * @param invoiceId a <code>String</code> value
     */
    public void setInvoiceId(String invoiceId);

    /**
     * Sets the status Id to search for.
     * @param statusId a <code>String</code> value
     */
    public void setStatusId(String statusId);

    /**
     * Sets the processing status Id to search for.
     * @param processingStatusId a <code>String</code> value
     */
    public void setProcessingStatusId(String processingStatusId);

    /**
     * Sets the to party Id to search for.
     * @param partyId a <code>String</code> value
     */
    public void setPartyId(String partyId);

    /**
     * Sets the from party Id to search for.
     * @param partyIdFrom a <code>String</code> value
     */
    public void setPartyIdFrom(String partyIdFrom);

    /**
     * Sets the order Id to search for.
     * @param orderId a <code>String</code> value
     */
    public void setOrderId(String orderId);

    /**
     * Sets the invoice message to search for.
     * @param message a <code>String</code> value
     */
    public void setMessage(String message);

    /**
     * Sets the reference number to search for.
     * @param referenceNumber a <code>String</code> value
     */
    public void setReferenceNumber(String referenceNumber);

    /**
     * Sets the invoice item description to search for.
     * @param itemDescription a <code>String</code> value
     */
    public void setItemDescription(String itemDescription);

    /**
     * Sets the from date string to search for, related to the invoice date.
     * @param fromDate a <code>String</code> value
     */
    public void setFromDate(String fromDate);

    /**
     * Sets the from date to search for, related to the invoice date.
     * @param fromDate a <code>Timestamp</code> value
     */
    public void setFromDate(Timestamp fromDate);

    /**
     * Sets the thru date string to search for, related to the invoice date.
     * @param thruDate a <code>String</code> value
     */
    public void setThruDate(String thruDate);

    /**
     * Sets the thru date to search for, related to the invoice date.
     * @param thruDate a <code>Timestamp</code> value
     */
    public void setThruDate(Timestamp thruDate);

    /**
     * Sets the from date string to search for, related to the invoice due date.
     * @param fromDueDate a <code>String</code> value
     */
    public void setFromDueDate(String fromDueDate);

    /**
     * Sets the from date to search for, related to the invoice due date.
     * @param fromDueDate a <code>Timestamp</code> value
     */
    public void setFromDueDate(Timestamp fromDueDate);

    /**
     * Sets the thru date string to search for, related to the invoice due date.
     * @param thruDueDate a <code>String</code> value
     */
    public void setThruDueDate(String thruDueDate);

    /**
     * Sets the thru date to search for, related to the invoice due date.
     * @param thruDueDate a <code>Timestamp</code> value
     */
    public void setThruDueDate(Timestamp thruDueDate);

    /**
     * Sets the from date string to search for, related to the invoice paid date.
     * @param fromPaidDate a <code>String</code> value
     */
    public void setFromPaidDate(String fromPaidDate);

    /**
     * Sets the from date to search for, related to the invoice paid date.
     * @param fromPaidDate a <code>Timestamp</code> value
     */
    public void setFromPaidDate(Timestamp fromPaidDate);

    /**
     * Sets the thru date string to search for, related to the invoice paid date.
     * @param thruPaidDate a <code>String</code> value
     */
    public void setThruPaidDate(String thruPaidDate);

    /**
     * Sets the thru date to search for, related to the invoice paid date.
     * @param thruPaidDate a <code>Timestamp</code> value
     */
    public void setThruPaidDate(Timestamp thruPaidDate);

    /**
     * Sets the from amount to search for, related to the invoice total amount.
     * @param fromAmount a <code>BigDecimal</code> value
     */
    public void setFromAmount(BigDecimal fromAmount);

    /**
     * Sets the thru amount to search for, related to the invoice total amount.
     * @param thruAmount a <code>BigDecimal</code> value
     */
    public void setThruAmount(BigDecimal thruAmount);

    /**
     * Sets the from amount to search for, related to the invoice open amount.
     * @param fromOpenAmount a <code>BigDecimal</code> value
     */
    public void setFromOpenAmount(BigDecimal fromOpenAmount);

    /**
     * Sets the thru amount to search for, related to the invoice open amount.
     * @param thruOpenAmount a <code>BigDecimal</code> value
     */
    public void setThruOpenAmount(BigDecimal thruOpenAmount);

    /**
     * Sets the userLoginId of current login to search for.
     * @param userLoginId a <code>String</code> value
     */
    public void setUserLoginId(String userLoginId);

    /**
     * Sets the organization party Id to search for.
     * @param organizationPartyId a <code>String</code> value
     */
    public void setOrganizationPartyId(String organizationPartyId);

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
     * Finds the list of <code>Invoice</code>.
     * @return list of invoices
     * @throws RepositoryException if an error occurs
     */
    public List<InvoiceViewForListing> findInvoices() throws RepositoryException;
}
