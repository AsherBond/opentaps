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
package org.opentaps.common.domain.order;


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.constants.OrderTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.InventoryItem;
import org.opentaps.base.entities.OrderHeader;
import org.opentaps.base.entities.OrderItem;
import org.opentaps.base.entities.OrderRole;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.entities.TrackingCodeOrder;
import org.opentaps.common.util.UtilDate;
import org.opentaps.domain.order.OrderViewForListing;
import org.opentaps.domain.order.SalesOrderLookupRepositoryInterface;
import org.opentaps.foundation.entity.hibernate.HibernateUtil;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.CommonLookupRepository;

/**
 * Repository to lookup Sales Orders.
 */
public class SalesOrderLookupRepository extends CommonLookupRepository implements SalesOrderLookupRepositoryInterface {

    @SuppressWarnings("unused")
    private static final String MODULE = SalesOrderLookupRepository.class.getName();

    private String orderId;
    private String statusId;
    private String orderName;
    private String organizationPartyId;
    private String createdBy;
    private String customerPartyId;
    private String externalOrderId;
    private String fromDateStr;
    private Timestamp fromDate;
    private String thruDateStr;
    private Timestamp thruDate;
    private String lotId;
    private String productStoreId;
    private String purchaseOrderId;
    private String serialNumber;
    private String shippingAddress;
    private String shippingCountry;
    private String shippingStateProvince;
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingToName;
    private String shippingAttnName;
    private String userLoginId;
    private String viewPref;
    private String productId;
    private boolean findActiveOnly = false;
    private boolean findDesiredOnly = false;
    private Locale locale;
    private TimeZone timeZone;
    private List<String> orderBy;

    /**
     * Default constructor.
     */
    public SalesOrderLookupRepository() {
        super();
    }

    /** {@inheritDoc} */
    public List<OrderViewForListing> findOrders() throws RepositoryException {

        // convert fromDateStr / thruDateStr into Timestamps if the string versions were given
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = UtilDate.toTimestamp(fromDateStr, timeZone, locale);
        }
        if (UtilValidate.isNotEmpty(thruDateStr)) {
            thruDate = UtilDate.toTimestamp(thruDateStr, timeZone, locale);
        }
        Session session = null;
        try {
            // get a hibernate session
            session = getInfrastructure().getSession();
            Criteria criteria = session.createCriteria(OrderHeader.class);

            // always filter by the current organization
            criteria.add(Restrictions.eq(OrderHeader.Fields.billFromPartyId.name(), organizationPartyId));

            // filters by order type, we only want sales order
            criteria.add(Restrictions.eq(OrderHeader.Fields.orderTypeId.name(), OrderTypeConstants.SALES_ORDER));

            // set the from/thru date filter if they were given
            if (fromDate != null) {
                criteria.add(Restrictions.ge(OrderHeader.Fields.orderDate.name(), fromDate));
            }
            if (thruDate != null) {
                criteria.add(Restrictions.le(OrderHeader.Fields.orderDate.name(), thruDate));
            }

            // filter the role assoc, there is only one customer role per order
            Criteria roleCriteria = criteria.createAlias("orderRoles", "or");
            roleCriteria.add(Restrictions.eq("or.id." + OrderRole.Fields.roleTypeId.name(), RoleTypeConstants.BILL_TO_CUSTOMER));

            // filter orders created by the given user (TODO: what use is viewPref as a string here, should be a boolean flag instead ?)
            if (UtilValidate.isNotEmpty(viewPref)) {
                criteria.add(Restrictions.eq(OrderHeader.Fields.createdBy.name(), userLoginId));
            }

            // filter by order status
            if (findActiveOnly || findDesiredOnly) {
                List<String> statuses = UtilMisc.toList(StatusItemConstants.OrderStatus.ORDER_APPROVED, StatusItemConstants.OrderStatus.ORDER_CREATED, StatusItemConstants.OrderStatus.ORDER_HOLD);
                if (findActiveOnly) {
                    statuses.add(StatusItemConstants.OrderStatus.ORDER_PROCESSING);
                }

                criteria.add(Restrictions.in(OrderHeader.Fields.statusId.name(), statuses));
            }

            // filter by the given orderId string
            if (UtilValidate.isNotEmpty(orderId)) {
                criteria.add(Restrictions.ilike(OrderHeader.Fields.orderId.name(), orderId, MatchMode.START));
            }
            // filter by the given externalOrderId string
            if (UtilValidate.isNotEmpty(externalOrderId)) {
                criteria.add(Restrictions.ilike(OrderHeader.Fields.externalId.name(), externalOrderId, MatchMode.START));
            }

            // filter by exact matching status, if a statusId was given
            if (UtilValidate.isNotEmpty(statusId)) {
                criteria.add(Restrictions.eq(OrderHeader.Fields.statusId.name(), statusId));
            }

            // filter by product store if given
            if (UtilValidate.isNotEmpty(productStoreId)) {
                criteria.add(Restrictions.eq(OrderHeader.Fields.productStoreId.name(), productStoreId));
            }

            // filter by the user who created the order if given
            if (UtilValidate.isNotEmpty(createdBy)) {
                criteria.add(Restrictions.eq(OrderHeader.Fields.createdBy.name(), createdBy));
            }

            // filter by the given orderName string
            if (UtilValidate.isNotEmpty(orderName)) {
                criteria.add(Restrictions.ilike(OrderHeader.Fields.orderName.name(), orderName, MatchMode.START));
            }

            // filter by the given customerPartyId string, from the OrderRole entity
            if (UtilValidate.isNotEmpty(customerPartyId)) {
                roleCriteria.add(Restrictions.ilike("or.id." + OrderRole.Fields.partyId.name(), customerPartyId, MatchMode.START));
            }

            // filter by the given purchaseOrderId string, from the OrderItem entity
            criteria.createAlias("orderItems", "oi");
            if (UtilValidate.isNotEmpty(purchaseOrderId)) {
                criteria.add(Restrictions.ilike("oi." + OrderItem.Fields.correspondingPoId.name(), purchaseOrderId, MatchMode.START));
            }

            // filter by the given productId string, from the OrderItem entity
            if (UtilValidate.isNotEmpty(productId)) {
                criteria.add(Restrictions.ilike("oi." + OrderItem.Fields.productId.name(), productId, MatchMode.START));
            }

            // filter by the given shippingAddress string, from the OrderItemShipGroup entity
            criteria.createAlias("orderItemShipGroups", "oisg");
        	Criteria address = criteria.createCriteria("oisg.postalAddress");
            if (UtilValidate.isNotEmpty(shippingAddress)) {
            	address.add(Restrictions.ilike(PostalAddress.Fields.address1.name(), shippingAddress, MatchMode.ANYWHERE));
            }

            if (UtilValidate.isNotEmpty(shippingCountry)) {
            	address.add(Restrictions.ilike(PostalAddress.Fields.countryGeoId.name(), shippingCountry, MatchMode.EXACT));
            }

            if (UtilValidate.isNotEmpty(shippingStateProvince)) {
            	address.add(Restrictions.ilike(PostalAddress.Fields.stateProvinceGeoId.name(), shippingStateProvince, MatchMode.EXACT));
            }

            if (UtilValidate.isNotEmpty(shippingCity)) {
            	address.add(Restrictions.ilike(PostalAddress.Fields.city.name(), shippingCity, MatchMode.START));
            }

            if (UtilValidate.isNotEmpty(shippingPostalCode)) {
            	address.add(Restrictions.ilike(PostalAddress.Fields.postalCode.name(), shippingPostalCode, MatchMode.START));
            }

            if (UtilValidate.isNotEmpty(shippingToName)) {
            	address.add(Restrictions.ilike(PostalAddress.Fields.toName.name(), shippingToName, MatchMode.START));
            }

            if (UtilValidate.isNotEmpty(shippingAttnName)) {
            	address.add(Restrictions.ilike(PostalAddress.Fields.attnName.name(), shippingAttnName, MatchMode.START));
            }
            // filter by the given lotId and serialNumber, which may come either from
            // OrderItemShipGrpInvRes -> InventoryItem
            // or
            // ItemIssuance -> InventoryItem
            criteria.createCriteria("orderItemShipGrpInvReses", Criteria.LEFT_JOIN).createCriteria("inventoryItem", "rii", Criteria.LEFT_JOIN);
            criteria.createCriteria("itemIssuances", Criteria.LEFT_JOIN).createCriteria("inventoryItem", "iii", Criteria.LEFT_JOIN);
            if (UtilValidate.isNotEmpty(lotId)) {
                criteria.add(Restrictions.or(
                                Restrictions.ilike("rii." + InventoryItem.Fields.lotId.name(), lotId, MatchMode.START),
                                Restrictions.ilike("iii." + InventoryItem.Fields.lotId.name(), lotId, MatchMode.START)));
            }
            if (UtilValidate.isNotEmpty(serialNumber)) {
                criteria.add(Restrictions.or(
                                Restrictions.ilike("rii." + InventoryItem.Fields.serialNumber.name(), serialNumber, MatchMode.START),
                                Restrictions.ilike("iii." + InventoryItem.Fields.serialNumber.name(), serialNumber, MatchMode.START)));
            }

            criteria.createCriteria("trackingCodeOrders", "tco" ,Criteria.LEFT_JOIN);

            // specify the fields to return
            criteria.setProjection(Projections.projectionList()
                                   .add(Projections.distinct(Projections.property(OrderHeader.Fields.orderId.name())))
                                   .add(Projections.property(OrderHeader.Fields.orderName.name()))
                                   .add(Projections.property(OrderHeader.Fields.statusId.name()))
                                   .add(Projections.property(OrderHeader.Fields.grandTotal.name()))
                                   .add(Projections.property(OrderHeader.Fields.orderDate.name()))
                                   .add(Projections.property(OrderHeader.Fields.currencyUom.name()))
                                   .add(Projections.property("or.id." + OrderRole.Fields.partyId.name()))
                                   .add(Projections.property("oi." + OrderItem.Fields.correspondingPoId.name()))
                                   .add(Projections.property("tco." + TrackingCodeOrder.Fields.trackingCodeId.name()))
                                   );
            Debug.logInfo("criteria.toString() : " + criteria.toString(), MODULE);
            // set the order by
            if (orderBy == null) {
                orderBy = Arrays.asList(OrderHeader.Fields.orderDate.desc());
            }
            // some substitution is needed to fit the hibernate field names
            // this also maps the calculated fields and indicates the non sortable fields
            Map<String, String> subs = new HashMap<String, String>();
            subs.put("partyId", "or.id.partyId");
            subs.put("partyName", "or.id.partyId");
            subs.put("orderDateString", "orderDate");
            subs.put("shipByDateString", null);
            subs.put("orderNameId", "orderId");
            subs.put("statusDescription", "statusId");
            subs.put("correspondingPoId", "oi.correspondingPoId");
            subs.put("trackingCodeId", "tco.trackingCodeId");
            HibernateUtil.setCriteriaOrder(criteria, orderBy, subs);

            ScrollableResults results = null;
            List<OrderViewForListing> results2 = new ArrayList<OrderViewForListing>();
            try {
                // fetch the paginated results
                results = criteria.scroll(ScrollMode.SCROLL_INSENSITIVE);
                if (usePagination()) {
                    results.setRowNumber(getPageStart());
                } else {
                    results.first();
                }

                // convert them into OrderViewForListing objects which will also calculate or format some fields for display
                Object[] o = results.get();
                int n = 0; // number of results actually read
                while (o != null) {
                    OrderViewForListing r = new OrderViewForListing();
                    r.initRepository(this);
                    int i = 0;
                    r.setOrderId((String) o[i++]);
                    r.setOrderName((String) o[i++]);
                    r.setStatusId((String) o[i++]);
                    r.setGrandTotal((BigDecimal) o[i++]);
                    r.setOrderDate((Timestamp) o[i++]);
                    r.setCurrencyUom((String) o[i++]);
                    r.setPartyId((String) o[i++]);
                    r.setCorrespondingPoId((String) o[i++]);
                    r.setTrackingCodeId((String) o[i++]);
                    r.calculateExtraFields(getDelegator(), timeZone, locale);
                    results2.add(r);
                    n++;

                    if (!results.next()) {
                        break;
                    }
                    if (usePagination() && n >= getPageSize()) {
                        break;
                    }
                    o = results.get();
                }
                results.last();
                // note: row number starts at 0
                setResultSize(results.getRowNumber() + 1);
            } finally {
            	if (results != null) {
            		results.close();
            	}
            }

            return results2;

        } catch (InfrastructureException e) {
        	Debug.logError(e, MODULE);
            throw new RepositoryException(e);
        }  finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /** {@inheritDoc} */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /** {@inheritDoc} */
    public void setOrderBy(List<String> orderBy) {
        this.orderBy = orderBy;
    }

    /** {@inheritDoc} */
    public void setCustomerPartyId(String customerPartyId) {
        this.customerPartyId = customerPartyId;
    }

    /** {@inheritDoc} */
    public void setExteralOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }

    /** {@inheritDoc} */
    public void setFromDate(String fromDate) {
        this.fromDateStr = fromDate;
    }

    /** {@inheritDoc} */
    public void setFromDate(Timestamp fromDate) {
        this.fromDate = fromDate;
    }

    /** {@inheritDoc} */
    public void setThruDate(String thruDate) {
        this.thruDateStr = thruDate;
    }

    /** {@inheritDoc} */
    public void setThruDate(Timestamp thruDate) {
        this.thruDate = thruDate;
    }

    /** {@inheritDoc} */
    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    /** {@inheritDoc} */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /** {@inheritDoc} */
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /** {@inheritDoc} */
    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    /** {@inheritDoc} */
    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    /** {@inheritDoc} */
    public void setOrganizationPartyId(String organizationPartyId) {
        this.organizationPartyId = organizationPartyId;
    }

    /** {@inheritDoc} */
    public void setProductStoreId(String productStoreId) {
        this.productStoreId = productStoreId;
    }

    /** {@inheritDoc} */
    public void setPurchaseOrderId(String purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    /** {@inheritDoc} */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /** {@inheritDoc} */
    public void setShippingAddress(String address) {
    	this.shippingAddress = address;
    }

    /** {@inheritDoc} */
    public void setShippingCountry(String countryGeoId) {
    	this.shippingCountry = countryGeoId;
    }

    /** {@inheritDoc} */
    public void setShippingStateProvince(String stateProvinceGeoId) {
    	this.shippingStateProvince = stateProvinceGeoId;
    }

    /** {@inheritDoc} */
    public void setShippingCity(String city) {
    	this.shippingCity = city;
    }

    /** {@inheritDoc} */
    public void setShippingPostalCode(String postalCode) {
    	this.shippingPostalCode = postalCode;
    }

    /** {@inheritDoc} */
    public void setShippingToName(String toName) {
    	this.shippingToName = toName;
    }

    /** {@inheritDoc} */
    public void setShippingAttnName(String attnName){
    	this.shippingAttnName = attnName;
    }

    /** {@inheritDoc} */
    public void setUserLoginId(String userLoginId) {
        this.userLoginId = userLoginId;
    }

    /** {@inheritDoc} */
    public void setViewPref(String viewPref) {
        this.viewPref = viewPref;
    }

    /** {@inheritDoc} */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /** {@inheritDoc} */
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /** {@inheritDoc} */
    public void setFindActiveOnly(boolean findActiveOnly) {
        this.findActiveOnly = findActiveOnly;
    }

    /** {@inheritDoc} */
    public void setFindDesiredOnly(boolean findDesiredOnly) {
        this.findDesiredOnly = findDesiredOnly;
    }
}
