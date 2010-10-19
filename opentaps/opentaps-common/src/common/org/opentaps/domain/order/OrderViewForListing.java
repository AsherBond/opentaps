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
import org.opentaps.common.order.UtilOrder;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityFieldInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * A POJO view entity used for order listing.
 * This does not exists in the Data Model and contains calculated fields,
 *  so it should be built from other real entities.
 */
public class OrderViewForListing extends Entity {

    public static enum Fields implements EntityFieldInterface<OrderViewForListing> {
        orderId("orderId"),
        partyId("partyId"),
        orderName("orderName"),
        orderDate("orderDate"),
        productStoreId("productStoreId"),
        statusId("statusId"),
        statusDescription("statusDescription"),
        correspondingPoId("correspondingPoId"),
        currencyUom("currencyUom"),
        grandTotal("grandTotal"),
        orderNameId("orderNameId"),
        partyName("partyName"),
        shipByDateString("shipByDateString"),
        orderDateString("orderDateString"),
        trackingCodeId("trackingCodeId");
        private final String fieldName;
        private Fields(String name) { fieldName = name; }
        /** {@inheritDoc} */
        public String getName() { return fieldName; }
        /** {@inheritDoc} */
        public String asc() { return fieldName + " ASC"; }
        /** {@inheritDoc} */
        public String desc() { return fieldName + " DESC"; }
    }

    private String orderId;
    private String partyId;
    private String orderName;
    private Timestamp orderDate;
    private String productStoreId;
    private String statusId;
    private String statusDescription;
    private String correspondingPoId;
    private String currencyUom;
    private BigDecimal grandTotal;
    private String orderDateString;
    private String shipByDateString;
    private String partyName;
    private String orderNameId;
    private String trackingCodeId;

    /**
     * Default constructor.
     */
    public OrderViewForListing() {
        super();
    }

    /**
     * Creates a new <code>OrderViewForListing</code> instance from an <code>EntityInterface</code>.
     * @param clone an <code>EntityInterface</code> value
     * @param delegator a <code>Delegator</code> value
     * @param timeZone a <code>TimeZone</code> value
     * @param locale a <code>Locale</code> value
     * @return a new instance of <code>OrderViewForListing</code> based on the given <code>EntityInterface</code>
     * @throws RepositoryException if an error occurs
     */
    public static OrderViewForListing makeOrderView(EntityInterface clone, Delegator delegator, TimeZone timeZone, Locale locale) throws RepositoryException {
        OrderViewForListing o = new OrderViewForListing();
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
    public static List<OrderViewForListing> makeOrderView(List<? extends EntityInterface> clones, Delegator delegator, TimeZone timeZone, Locale locale) throws RepositoryException {
        List<OrderViewForListing> results = new ArrayList<OrderViewForListing>();
        for (EntityInterface clone : clones) {
            results.add(OrderViewForListing.makeOrderView(clone, delegator, timeZone, locale));
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
        if (getOrderDate() != null) {
            setOrderDateString(UtilDateTime.timeStampToString(getOrderDate(), UtilDateTime.getDateTimeFormat(locale), timeZone, locale));
        }
        setShipByDateString(UtilOrder.getEarliestShipByDate(delegator, getOrderId(), timeZone, locale));
        if (getPartyId() != null) {
            setPartyName(PartyHelper.getPartyName(delegator, getPartyId(), false));
        }
        setOrderNameId(getOrderId() + (getOrderName() == null ? "" : ": " + getOrderName()));
        if (getStatusId() != null) {
            try {
                GenericValue status = delegator.findByPrimaryKeyCache("StatusItem", UtilMisc.toMap(StatusItem.Fields.statusId.name(), getStatusId()));
                setStatusDescription((String) status.get(StatusItem.Fields.description.name(), locale));
            } catch (GenericEntityException e) {
                throw new RepositoryException(e);
            }

        }
    }

    /**
     * Sets the order date as a localized date string.
     * @param orderDateString the order date as a localized date string
     */
    public void setOrderDateString(String orderDateString) {
        this.orderDateString = orderDateString;
    }

    /**
     * Sets the order ship by date as a localized date string.
     * @param shipByDateString the order ship by date as a localized date string
     */
    public void setShipByDateString(String shipByDateString) {
        this.shipByDateString = shipByDateString;
    }

    /**
     * Sets the formatted customer/supplier name.
     * @param partyName the formatted customer/supplier name
     */
    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    /**
     * Sets the formatted order name and ID.
     * @param orderNameId the formatted order name and ID
     */
    public void setOrderNameId(String orderNameId) {
        this.orderNameId = orderNameId;
    }

    /**
     * Gets the order date as a localized date string.
     * @return the order date as a localized date string
     */
    public String getOrderDateString() {
        return this.orderDateString;
    }

    /**
     * Gets the order ship by date as a localized date string.
     * @return the order ship by date as a localized date string
     */
    public String getShipByDateString() {
        return this.shipByDateString;
    }

    /**
     * Gets the formatted customer/supplier name.
     * @return the formatted customer/supplier name
     */
    public String getPartyName() {
        return this.partyName;
    }

    /**
     * Gets the formatted order name and ID.
     * @return the formatted order name and ID
     */
    public String getOrderNameId() {
        return this.orderNameId;
    }

    // basic accessors
    /**
     * Auto generated value accessor.
     * @return <code>String</code>
     */
    public String getOrderId() {
        return this.orderId;
    }
    /**
     * Auto generated value accessor.
     * @return <code>String</code>
     */
    public String getPartyId() {
        return this.partyId;
    }
    /**
     * Auto generated value accessor.
     * @return <code>String</code>
     */
    public String getOrderName() {
        return this.orderName;
    }
    /**
     * Auto generated value accessor.
     * @return <code>Timestamp</code>
     */
    public Timestamp getOrderDate() {
        return this.orderDate;
    }
    /**
     * Auto generated value accessor.
     * @return <code>String</code>
     */
    public String getProductStoreId() {
        return this.productStoreId;
    }
    /**
     * Auto generated value accessor.
     * @return <code>String</code>
     */
    public String getStatusId() {
        return this.statusId;
    }
    /**
     * Auto generated value accessor.
     * @return <code>String</code>
     */
    public String getStatusDescription() {
        return this.statusDescription;
    }
    /**
     * Auto generated value accessor.
     * @return <code>String</code>
     */
    public String getCorrespondingPoId() {
        return this.correspondingPoId;
    }
    /**
     * Auto generated value accessor.
     * @return <code>String</code>
     */
    public String getCurrencyUom() {
        return this.currencyUom;
    }
    /**
     * Auto generated value accessor.
     * @return <code>BigDecimal</code>
     */
    public BigDecimal getGrandTotal() {
        return this.grandTotal;
    }
    /**
     * Auto generated value accessor.
     * @return <code>String</code>
     */
    public String getTrackingCodeId() {
        return this.trackingCodeId;
    }

    /**
     * Auto generated value accessor.
     * @param v <code>String</code>
     */
    public void setOrderId(String v) {
        this.orderId = v;
    }
    /**
     * Auto generated value accessor.
     * @param v <code>String</code>
     */
    public void setPartyId(String v) {
        this.partyId = v;
    }
    /**
     * Auto generated value accessor.
     * @param v <code>String</code>
     */
    public void setOrderName(String v) {
        this.orderName = v;
    }
    /**
     * Auto generated value accessor.
     * @param v <code>Timestamp</code>
     */
    public void setOrderDate(Timestamp v) {
        this.orderDate = v;
    }
    /**
     * Auto generated value accessor.
     * @param v <code>String</code>
     */
    public void setProductStoreId(String v) {
        this.productStoreId = v;
    }
    /**
     * Auto generated value accessor.
     * @param v <code>String</code>
     */
    public void setStatusId(String v) {
        this.statusId = v;
    }
    /**
     * Auto generated value accessor.
     * @param v <code>String</code>
     */
    public void setStatusDescription(String v) {
        this.statusDescription = v;
    }
    /**
     * Auto generated value accessor.
     * @param v <code>String</code>
     */
    public void setCorrespondingPoId(String v) {
        this.correspondingPoId = v;
    }
    /**
     * Auto generated value accessor.
     * @param v <code>String</code>
     */
    public void setCurrencyUom(String v) {
        this.currencyUom = v;
    }
    /**
     * Auto generated value accessor.
     * @param v <code>BigDecimal</code>
     */
    public void setGrandTotal(BigDecimal v) {
        this.grandTotal = v;
    }
    /**
     * Auto generated value accessor.
     * @param v <code>String</code>
     */
    public void setTrackingCodeId(String v) {
        this.trackingCodeId = v;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> mapValue = new FastMap<String, Object>();
        mapValue.put("orderId", getOrderId());
        mapValue.put("partyId", getPartyId());
        mapValue.put("orderName", getOrderName());
        mapValue.put("orderDate", getOrderDate());
        mapValue.put("productStoreId", getProductStoreId());
        mapValue.put("statusId", getStatusId());
        mapValue.put("statusDescription", getStatusDescription());
        mapValue.put("correspondingPoId", getCorrespondingPoId());
        mapValue.put("currencyUom", getCurrencyUom());
        mapValue.put("grandTotal", getGrandTotal());
        mapValue.put("orderNameId", getOrderNameId());
        mapValue.put("partyName", getPartyName());
        mapValue.put("shipByDateString", getShipByDateString());
        mapValue.put("orderDateString", getOrderDateString());
        mapValue.put("trackingCodeId", getTrackingCodeId());
        return mapValue;
    }

    /** {@inheritDoc} */
    @Override
    public void fromMap(Map<String, Object> mapValue) {
        setOrderId((String) mapValue.get("orderId"));
        setPartyId((String) mapValue.get("partyId"));
        setOrderName((String) mapValue.get("orderName"));
        setOrderDate((Timestamp) mapValue.get("orderDate"));
        setProductStoreId((String) mapValue.get("productStoreId"));
        setStatusId((String) mapValue.get("statusId"));
        setStatusDescription((String) mapValue.get("statusDescription"));
        setCorrespondingPoId((String) mapValue.get("correspondingPoId"));
        setCurrencyUom((String) mapValue.get("currencyUom"));
        setGrandTotal((BigDecimal) mapValue.get("grandTotal"));
        setOrderNameId((String) mapValue.get("orderNameId"));
        setPartyName((String) mapValue.get("partyName"));
        setShipByDateString((String) mapValue.get("shipByDateString"));
        setOrderDateString((String) mapValue.get("orderDateString"));
        setTrackingCodeId((String) mapValue.get("trackingCodeId"));
    }
}
