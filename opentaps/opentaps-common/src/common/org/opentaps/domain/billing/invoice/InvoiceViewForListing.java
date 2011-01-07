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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javolution.util.FastMap;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.party.party.PartyHelper;
import org.opentaps.base.entities.StatusItem;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityFieldInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * A POJO view entity used for invoice listing.
 * This does not exists in the Data Model and contains calculated fields,
 *  so it should be built from other real entities.
 */
public class InvoiceViewForListing extends Entity {

    public static enum Fields implements EntityFieldInterface<InvoiceViewForListing> {
        invoiceId("invoiceId"),
        partyIdFrom("partyIdFrom"),
        partyId("partyId"),
        partyNameFrom("partyNameFrom"),
        partyName("partyName"),
        invoiceDate("invoiceDate"),
        dueDate("dueDate"),
        invoiceDateString("invoiceDateString"),
        dueDateString("dueDateString"),
        statusId("statusId"),
        statusDescription("statusDescription"),
        processingStatusId("processingStatusId"),
        processingStatusDescription("processingStatusDescription"),
        currencyUomId("currencyUomId"),
        invoiceTotal("invoiceTotal"),
        openAmount("openAmount"),
        referenceNumber("referenceNumber");
        private final String fieldName;
        private Fields(String name) { fieldName = name; }
        /** {@inheritDoc} */
        public String getName() { return fieldName; }
        /** {@inheritDoc} */
        public String asc() { return fieldName + " ASC"; }
        /** {@inheritDoc} */
        public String desc() { return fieldName + " DESC"; }
    }

    private String invoiceId;
    private String partyId;
    private String partyIdFrom;
    private String partyName;
    private String partyNameFrom;
    private Timestamp invoiceDate;
    private Timestamp dueDate;
    private String invoiceDateString;
    private String dueDateString;
    private String statusId;
    private String statusDescription;
    private String processingStatusId;
    private String processingStatusDescription;
    private String currencyUomId;
    private BigDecimal invoiceTotal;
    private BigDecimal openAmount;
    private String referenceNumber;

    /**
     * Default constructor.
     */
    public InvoiceViewForListing() {
        super();
    }

    /**
     * Creates a new <code>InvoiceViewForListing</code> instance from an <code>EntityInterface</code>.
     * @param clone an <code>EntityInterface</code> value
     * @param delegator a <code>Delegator</code> value
     * @param timeZone a <code>TimeZone</code> value
     * @param locale a <code>Locale</code> value
     * @return a new instance of <code>OrderViewForListing</code> based on the given <code>EntityInterface</code>
     * @throws RepositoryException if an error occurs
     */
    public static InvoiceViewForListing makeInvoiceView(EntityInterface clone, Delegator delegator, TimeZone timeZone, Locale locale) throws RepositoryException {
        InvoiceViewForListing o = new InvoiceViewForListing();
        if (clone.getBaseRepository() != null) {
            o.initRepository(clone.getBaseRepository());
        }
        o.fromMap(clone.toMap());
        o.calculateExtraFields(delegator, timeZone, locale);
        return o;
    }

    /**
     * Creates a List of new <code>OrderViewForListing</code> instance from a List of <code>EntityInterface</code>.
     * @param clones a List of <code>EntityInterface</code> values
     * @param delegator a <code>Delegator</code> value
     * @param timeZone a <code>TimeZone</code> value
     * @param locale a <code>Locale</code> value
     * @return a List of new instances of <code>OrderViewForListing</code> based on the given <code>EntityInterface</code> values
     * @throws RepositoryException if an error occurs
     */
    public static List<InvoiceViewForListing> makeInvoiceView(List<? extends EntityInterface> clones, Delegator delegator, TimeZone timeZone, Locale locale) throws RepositoryException {
        List<InvoiceViewForListing> results = new ArrayList<InvoiceViewForListing>();
        for (EntityInterface clone : clones) {
            results.add(InvoiceViewForListing.makeInvoiceView(clone, delegator, timeZone, locale));
        }
        return results;
    }

    /**
     * Calculates the extra fields, normal fields should have set first.
     * @param delegator a <code>Delegator</code> value
     * @param timeZone a <code>TimeZone</code> value
     * @param locale a <code>Locale</code> value
     * @exception RepositoryException if an error occurs
     */
    public void calculateExtraFields(Delegator delegator, TimeZone timeZone, Locale locale) throws RepositoryException {
        if (getInvoiceDate() != null) {
            setInvoiceDateString(UtilDateTime.timeStampToString(getInvoiceDate(), UtilDateTime.getDateTimeFormat(locale), timeZone, locale));
        }
        if (getDueDate() != null) {
            setDueDateString(UtilDateTime.timeStampToString(getDueDate(), UtilDateTime.getDateTimeFormat(locale), timeZone, locale));
        }
        if (getPartyId() != null) {
            setPartyName(PartyHelper.getPartyName(delegator, getPartyId(), false));
        }
        if (getPartyIdFrom() != null) {
            setPartyNameFrom(PartyHelper.getPartyName(delegator, getPartyIdFrom(), false));
        }
        if (getStatusId() != null) {
            try {
                GenericValue status = delegator.findByPrimaryKeyCache("StatusItem", UtilMisc.toMap(StatusItem.Fields.statusId.name(), getStatusId()));
                setStatusDescription((String) status.get(StatusItem.Fields.description.name(), locale));
            } catch (GenericEntityException e) {
                throw new RepositoryException(e);
            }
        }
        if (getProcessingStatusId() != null) {
            try {
                GenericValue status = delegator.findByPrimaryKeyCache("StatusItem", UtilMisc.toMap(StatusItem.Fields.statusId.name(), getStatusId()));
                setProcessingStatusDescription((String) status.get(StatusItem.Fields.description.name(), locale));
            } catch (GenericEntityException e) {
                throw new RepositoryException(e);
            }
        }
    }

    /**
     * Gets the invoiceId.
     * @return the value of invoiceId
     */
    public final String getInvoiceId() {
        return this.invoiceId;
    }

    /**
     * Sets the invoiceId.
     * @param invoiceId the invoiceId
     */
    public final void setInvoiceId(final String invoiceId) {
        this.invoiceId = invoiceId;
    }

    /**
     * Gets the value of partyId.
     * @return the value of partyId
     */
    public final String getPartyId() {
        return this.partyId;
    }

    /**
     * Sets the value of partyId.
     * @param partyId Value to assign to this.partyId
     */
    public final void setPartyId(final String partyId) {
        this.partyId = partyId;
    }

    /**
     * Gets the value of partyIdFrom.
     * @return the value of partyIdFrom
     */
    public final String getPartyIdFrom() {
        return this.partyIdFrom;
    }

    /**
     * Sets the value of partyIdFrom.
     * @param partyIdFrom Value to assign to this.partyIdFrom
     */
    public final void setPartyIdFrom(final String partyIdFrom) {
        this.partyIdFrom = partyIdFrom;
    }

    /**
     * Gets the value of partyName.
     * @return the value of partyName
     */
    public final String getPartyName() {
        return this.partyName;
    }

    /**
     * Sets the value of partyName.
     * @param partyName Value to assign to this.partyName
     */
    public final void setPartyName(final String partyName) {
        this.partyName = partyName;
    }

    /**
     * Gets the value of partyNameFrom.
     * @return the value of partyNameFrom
     */
    public final String getPartyNameFrom() {
        return this.partyNameFrom;
    }

    /**
     * Sets the value of partyNameFrom.
     * @param partyNameFrom Value to assign to this.partyNameFrom
     */
    public final void setPartyNameFrom(final String partyNameFrom) {
        this.partyNameFrom = partyNameFrom;
    }

    /**
     * Gets the value of invoiceDate.
     * @return the value of invoiceDate
     */
    public final Timestamp getInvoiceDate() {
        return this.invoiceDate;
    }

    /**
     * Sets the value of invoiceDate.
     * @param invoiceDate Value to assign to this.invoiceDate
     */
    public final void setInvoiceDate(final Timestamp invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    /**
     * Gets the value of dueDate.
     * @return the value of dueDate
     */
    public final Timestamp getDueDate() {
        return this.dueDate;
    }

    /**
     * Sets the value of dueDate.
     * @param dueDate Value to assign to this.dueDate
     */
    public final void setDueDate(final Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Gets the value of invoiceDateString.
     * @return the value of invoiceDateString
     */
    public final String getInvoiceDateString() {
        return this.invoiceDateString;
    }

    /**
     * Sets the value of invoiceDateString.
     * @param invoiceDateString Value to assign to this.invoiceDateString
     */
    public final void setInvoiceDateString(final String invoiceDateString) {
        this.invoiceDateString = invoiceDateString;
    }

    /**
     * Gets the value of dueDateString.
     * @return the value of dueDateString
     */
    public final String getDueDateString() {
        return this.dueDateString;
    }

    /**
     * Sets the value of dueDateString.
     * @param dueDateString Value to assign to this.dueDateString
     */
    public final void setDueDateString(final String dueDateString) {
        this.dueDateString = dueDateString;
    }

    /**
     * Gets the value of statusId.
     * @return the value of statusId
     */
    public final String getStatusId() {
        return this.statusId;
    }

    /**
     * Sets the value of statusId.
     * @param statusId Value to assign to this.statusId
     */
    public final void setStatusId(final String statusId) {
        this.statusId = statusId;
    }

    /**
     * Gets the value of statusDescription.
     * @return the value of statusDescription
     */
    public final String getStatusDescription() {
        return this.statusDescription;
    }

    /**
     * Sets the value of statusDescription.
     * @param statusDescription Value to assign to this.statusDescription
     */
    public final void setStatusDescription(final String statusDescription) {
        this.statusDescription = statusDescription;
    }

    /**
     * Gets the value of processingStatusId.
     * @return the value of processingStatusId
     */
    public final String getProcessingStatusId() {
        return this.processingStatusId;
    }

    /**
     * Sets the value of processingStatusId.
     * @param processingStatusId Value to assign to this.processingStatusId
     */
    public final void setProcessingStatusId(final String processingStatusId) {
        this.processingStatusId = processingStatusId;
    }

    /**
     * Gets the value of processingStatusDescription.
     * @return the value of processingStatusDescription
     */
    public final String getProcessingStatusDescription() {
        return this.processingStatusDescription;
    }

    /**
     * Sets the value of processingStatusDescription.
     * @param processingStatusDescription Value to assign to this.processingStatusDescription
     */
    public final void setProcessingStatusDescription(final String processingStatusDescription) {
        this.processingStatusDescription = processingStatusDescription;
    }

    /**
     * Gets the value of currencyUomId.
     * @return the value of currencyUomId
     */
    public final String getCurrencyUomId() {
        return this.currencyUomId;
    }

    /**
     * Sets the value of currencyUomId.
     * @param currencyUom Value to assign to this.currencyUomId
     */
    public final void setCurrencyUomId(final String currencyUomId) {
        this.currencyUomId = currencyUomId;
    }

    /**
     * Gets the value of invoiceTotal.
     * @return the value of invoiceTotal
     */
    public final BigDecimal getInvoiceTotal() {
        return this.invoiceTotal;
    }

    /**
     * Sets the value of invoiceTotal.
     * @param invoiceTotal Value to assign to this.invoiceTotal
     */
    public final void setInvoiceTotal(final BigDecimal invoiceTotal) {
        this.invoiceTotal = invoiceTotal;
    }

    /**
     * Gets the value of openAmount.
     * @return the value of openAmount
     */
    public final BigDecimal getOpenAmount() {
        return this.openAmount;
    }

    /**
     * Sets the value of openAmount.
     * @param openAmount Value to assign to this.openAmount
     */
    public final void setOpenAmount(final BigDecimal openAmount) {
        this.openAmount = openAmount;
    }

    /**
     * Gets the value of referenceNumber.
     * @return the value of referenceNumber
     */
    public final String getReferenceNumber() {
        return this.referenceNumber;
    }

    /**
     * Sets the value of referenceNumber.
     * @param referenceNumber Value to assign to this.referenceNumber
     */
    public final void setReferenceNumber(final String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }


    /** {@inheritDoc} */
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> mapValue = new FastMap<String, Object>();
        mapValue.put("invoiceId", getInvoiceId());
        mapValue.put("partyId", getPartyId());
        mapValue.put("partyIdFrom", getPartyIdFrom());
        mapValue.put("partyName", getPartyName());
        mapValue.put("partyNameFrom", getPartyNameFrom());
        mapValue.put("invoiceDate", getInvoiceDate());
        mapValue.put("invoiceDateString", getInvoiceDateString());
        mapValue.put("dueDate", getDueDate());
        mapValue.put("dueDateString", getDueDateString());
        mapValue.put("statusId", getStatusId());
        mapValue.put("statusDescription", getStatusDescription());
        mapValue.put("processingStatusId", getProcessingStatusId());
        mapValue.put("processingStatusDescription", getProcessingStatusDescription());
        mapValue.put("currencyUomId", getCurrencyUomId());
        mapValue.put("invoiceTotal", getInvoiceTotal());
        mapValue.put("openAmount", getOpenAmount());
        mapValue.put("referenceNumber", getReferenceNumber());
        return mapValue;
    }

    /** {@inheritDoc} */
    @Override
    public void fromMap(Map<String, Object> mapValue) {
        setInvoiceId((String) mapValue.get("invoiceId"));
        setPartyId((String) mapValue.get("partyId"));
        setPartyIdFrom((String) mapValue.get("partyIdFrom"));
        setPartyName((String) mapValue.get("partyName"));
        setPartyNameFrom((String) mapValue.get("partyNameFrom"));
        setInvoiceDateString((String) mapValue.get("invoiceDateString"));
        setDueDateString((String) mapValue.get("dueDateString"));
        setInvoiceDate((Timestamp) mapValue.get("invoiceDate"));
        setDueDate((Timestamp) mapValue.get("dueDate"));
        setStatusId((String) mapValue.get("statusId"));
        setStatusDescription((String) mapValue.get("statusDescription"));
        setProcessingStatusId((String) mapValue.get("processingStatusId"));
        setProcessingStatusDescription((String) mapValue.get("processingStatusDescription"));
        setCurrencyUomId((String) mapValue.get("currencyUomId"));
        setInvoiceTotal((BigDecimal) mapValue.get("invoiceTotal"));
        setOpenAmount((BigDecimal) mapValue.get("openAmount"));
        setReferenceNumber((String) mapValue.get("referenceNumber"));
    }
}
