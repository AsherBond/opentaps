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
package org.opentaps.search.domain;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javolution.util.FastList;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.party.party.PartyHelper;
import org.opentaps.common.order.UtilOrder;
import org.opentaps.common.util.UtilDate;
import org.opentaps.domain.base.entities.OrderHeaderItemAndRolesAndInvCompleted;
import org.opentaps.domain.base.entities.OrderHeaderItemAndRolesAndInvPending;
import org.opentaps.domain.base.entities.StatusItem;
import org.opentaps.domain.search.SalesOrderSearchRepositoryInterface;
import org.opentaps.foundation.entity.util.EntityComparator;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.gwt.common.client.lookup.configuration.OrderLookupConfiguration;

/**
 * {@inheritDoc}.
 */
public class SalesOrderSearchRepository extends Repository implements SalesOrderSearchRepositoryInterface {

    private static final String MODULE = SalesOrderSearchRepository.class.getName();

    private String orderId;
    private String statusId;
    private String orderName;
    private String organizationPartyId;
    private String createdBy;
    private String customerPartyId;
    private String externalOrderId;
    private String fromDate;
    private String lotId;
    private String productStoreId;
    private String purchaseOrderId;
    private String serialNumber;
    private String thruDate;
    private String userLoginId;
    private String viewPref;
    private Locale locale;
    private TimeZone timeZone;
    private List<String> orderBy;

    /**
     * Default constructor.
     */
    public SalesOrderSearchRepository() {
        super();
    }

    /** {@inheritDoc} */
    public List<OrderHeaderItemAndRolesAndInvPending> findOrders() throws RepositoryException {
        List<EntityCondition> searchConditions = FastList.newInstance();

        // convert fromDate into Timestamp and construct EntityExpr for searching
        if (UtilValidate.isNotEmpty(fromDate)) {
            Timestamp fromDateTimestamp = UtilDate.toTimestamp(fromDate, timeZone, locale);
            if (UtilValidate.isNotEmpty(fromDateTimestamp)) {
                searchConditions.add(EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.orderDate.name(), EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp));
            }
        }

        // convert thruDate into Timestamp and construct EntityExpr for searching
        if (UtilValidate.isNotEmpty(thruDate)) {
            Timestamp thruDateTimestamp = UtilDate.toTimestamp(thruDate, timeZone, locale);
            if (UtilValidate.isNotEmpty(thruDateTimestamp)) {
                searchConditions.add(EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.orderDate.name(), EntityOperator.LESS_THAN_EQUAL_TO, thruDateTimestamp));
            }
        }

        // restrict search results to the company set up for CRM
        searchConditions.add(EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.billFromPartyId.name(), EntityOperator.EQUALS, organizationPartyId));

        // other conditions to limit the list
        searchConditions.add(EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.orderTypeId.name(), EntityOperator.EQUALS, "SALES_ORDER"));
        searchConditions.add(EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.roleTypeId.name(), EntityOperator.EQUALS, "BILL_TO_CUSTOMER"));

        // select parties assigned to current user or his team according to view preferences.
        if (UtilValidate.isNotEmpty(viewPref)) {
            EntityCondition additionalConditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.orderTypeId.name(), EntityOperator.EQUALS, "SALES_ORDER"),
                    EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.createdBy.name(), EntityOperator.EQUALS, userLoginId),
                    EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.roleTypeId.name(), EntityOperator.EQUALS, "BILL_TO_CUSTOMER"),
                    EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.statusId.name(), EntityOperator.IN, UtilMisc.toList("ORDER_APPROVED", "ORDER_CREATED", "ORDER_HOLD", "ORDER_PROCESSING")),
                    EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.billFromPartyId.name(), EntityOperator.EQUALS, organizationPartyId));
            searchConditions.add(additionalConditions);
        }
        EntityCondition searchConditionList = EntityCondition.makeCondition(searchConditions, EntityOperator.AND);
        List<EntityCondition> conds = new ArrayList<EntityCondition>();
        conds.add(searchConditionList);
        return findOrderListWithFilters(conds);
    }



    /**
     * Find the orders with conditions and filters.
     * @param conds initial list of conditions
     * @return the list of entities found, or <code>null</code> if an error occurred
     * @throws RepositoryException if error occur
     */
    private List<OrderHeaderItemAndRolesAndInvPending> findOrderListWithFilters(List<EntityCondition> conds) throws RepositoryException {
        if (UtilValidate.isNotEmpty(orderId)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.orderId.name()), EntityOperator.LIKE, EntityFunction.UPPER(orderId + "%")));
        }
        if (UtilValidate.isNotEmpty(statusId)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.statusId.name()), EntityOperator.EQUALS, EntityFunction.UPPER(statusId)));
        }
        if (UtilValidate.isNotEmpty(productStoreId)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.productStoreId.name()), EntityOperator.EQUALS, EntityFunction.UPPER(productStoreId)));
        }
        if (UtilValidate.isNotEmpty(createdBy)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.createdBy.name()), EntityOperator.EQUALS, EntityFunction.UPPER(createdBy)));
        }
        if (UtilValidate.isNotEmpty(orderName)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.orderName.name()), EntityOperator.LIKE, EntityFunction.UPPER(orderName + "%")));
        }
        if (UtilValidate.isNotEmpty(externalOrderId)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.externalId.name()), EntityOperator.LIKE, EntityFunction.UPPER(externalOrderId + "%")));
        }
        if (UtilValidate.isNotEmpty(customerPartyId)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.partyId.name()), EntityOperator.LIKE, EntityFunction.UPPER(customerPartyId + "%")));
        }
        if (UtilValidate.isNotEmpty(purchaseOrderId)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.correspondingPoId.name()), EntityOperator.LIKE, EntityFunction.UPPER(purchaseOrderId + "%")));
        }
        if (UtilValidate.isNotEmpty(lotId)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.lotId.name()), EntityOperator.LIKE, EntityFunction.UPPER(lotId + "%")));
        }
        if (UtilValidate.isNotEmpty(serialNumber)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.serialNumber.name()), EntityOperator.LIKE, EntityFunction.UPPER(serialNumber + "%")));
        }
        if (UtilValidate.isNotEmpty(orderName)) {
            conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderItemAndRolesAndInvPending.Fields.orderName.name()), EntityOperator.LIKE, EntityFunction.UPPER(orderName + "%")));
        }
        // add special condition with complete order
        return findOrdersByCondition(conds);
    }

    /**
     * Find the orders with special filter conditions.
     * @param conds initial list of conditions
     * @return the list of entities found, or <code>null</code> if an error occurred
     * @throws RepositoryException if error occur
     */
    private List<OrderHeaderItemAndRolesAndInvPending> findOrdersByCondition(List<EntityCondition> conds) throws RepositoryException {

        // Note: this code relies on the fact that OrderHeaderItemAndRolesAndInvPending and OrderHeaderItemAndRolesAndInvCompleted entities
        // share the same fields, so we can clone OrderHeaderItemAndRolesAndInvCompleted entity as OrderHeaderItemAndRolesAndInvPending entity
        // later on

        // must specify minimum required fields so that the distinct select works
        List<String> fieldsToSelect = FastList.newInstance();
        fieldsToSelect.add(OrderHeaderItemAndRolesAndInvPending.Fields.orderName.name());
        fieldsToSelect.add(OrderHeaderItemAndRolesAndInvPending.Fields.orderId.name());
        fieldsToSelect.add(OrderHeaderItemAndRolesAndInvPending.Fields.correspondingPoId.name());
        fieldsToSelect.add(OrderHeaderItemAndRolesAndInvPending.Fields.statusId.name());
        fieldsToSelect.add(OrderHeaderItemAndRolesAndInvPending.Fields.grandTotal.name());
        fieldsToSelect.add(OrderHeaderItemAndRolesAndInvPending.Fields.partyId.name());
        fieldsToSelect.add(OrderHeaderItemAndRolesAndInvPending.Fields.orderDate.name());
        fieldsToSelect.add(OrderHeaderItemAndRolesAndInvPending.Fields.currencyUom.name());
        List<String> queryOrderBy = new ArrayList<String>();
        if (orderBy == null) {
            orderBy = Arrays.asList(OrderHeaderItemAndRolesAndInvPending.Fields.orderDate.desc());
        }

        // convert sort condition parameters for findList, for some field is composed fields, and some not in view entity really.
        for (String ord : orderBy) {
            if (OrderHeaderItemAndRolesAndInvPending.Fields.orderNameId.asc().equals(ord)) {
                queryOrderBy.add(OrderHeaderItemAndRolesAndInvPending.Fields.orderName.asc());
                queryOrderBy.add(OrderHeaderItemAndRolesAndInvPending.Fields.orderId.asc());
            } else if (OrderHeaderItemAndRolesAndInvPending.Fields.orderNameId.desc().equals(ord)) {
                queryOrderBy.add(OrderHeaderItemAndRolesAndInvPending.Fields.orderName.desc());
                queryOrderBy.add(OrderHeaderItemAndRolesAndInvPending.Fields.orderId.desc());
            } else if (OrderHeaderItemAndRolesAndInvPending.Fields.orderDateString.asc().equals(ord)) {
                queryOrderBy.add(OrderHeaderItemAndRolesAndInvPending.Fields.orderDate.asc());
            } else if (OrderHeaderItemAndRolesAndInvPending.Fields.orderDateString.desc().equals(ord)) {
                queryOrderBy.add(OrderHeaderItemAndRolesAndInvPending.Fields.orderDate.desc());
            } else if (OrderHeaderItemAndRolesAndInvPending.Fields.statusDescription.asc().equals(ord)) {
                queryOrderBy.add(OrderHeaderItemAndRolesAndInvPending.Fields.statusId.asc());
            } else if (OrderHeaderItemAndRolesAndInvPending.Fields.statusDescription.desc().equals(ord)) {
                queryOrderBy.add(OrderHeaderItemAndRolesAndInvPending.Fields.statusId.desc());
            } else if (OrderHeaderItemAndRolesAndInvPending.Fields.customerName.asc().equals(ord)
                    || OrderHeaderItemAndRolesAndInvPending.Fields.customerName.desc().equals(ord)
                    || OrderHeaderItemAndRolesAndInvPending.Fields.shipByDateString.asc().equals(ord)
                    || OrderHeaderItemAndRolesAndInvPending.Fields.shipByDateString.desc().equals(ord)
                    ) {
                // do nothing for not existing in customerName/shipByDateString fields in view entity really
            } else {
                queryOrderBy.add(ord);
            }
        }
        List<OrderHeaderItemAndRolesAndInvPending> orders = FastList.newInstance();
        List<OrderHeaderItemAndRolesAndInvCompleted> completedOrders = findList(OrderHeaderItemAndRolesAndInvCompleted.class, EntityCondition.makeCondition(conds, EntityOperator.AND), fieldsToSelect, queryOrderBy);
        for (OrderHeaderItemAndRolesAndInvCompleted order : completedOrders) {
            // clone OrderHeaderItemAndRolesAndInvCompleted entity as OrderHeaderItemAndRolesAndInvPending entity and put it to orders list
            OrderHeaderItemAndRolesAndInvPending newOrder = new OrderHeaderItemAndRolesAndInvPending();
            newOrder.setAllFields(order.toMap());
            orders.add(newOrder);
        }

        List<String> orderIds = FastList.newInstance();
        for (OrderHeaderItemAndRolesAndInvCompleted order : completedOrders) {
            orderIds.add(order.getOrderId());
        }
        if (UtilValidate.isNotEmpty(orderIds)) {
            conds.add(EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.orderId.name(), EntityOperator.NOT_IN, orderIds));
        }
        List<OrderHeaderItemAndRolesAndInvPending> pendingOrderList = findList(OrderHeaderItemAndRolesAndInvPending.class, EntityCondition.makeCondition(conds, EntityOperator.AND), fieldsToSelect, queryOrderBy);
        orders.addAll(pendingOrderList);

        // add value for extra fields
        for (OrderHeaderItemAndRolesAndInvPending order : orders) {
            order.setOrderDateString(UtilDateTime.timeStampToString(order.getOrderDate(), UtilDateTime.getDateTimeFormat(locale), timeZone, locale));
            order.setShipByDateString(UtilOrder.getEarliestShipByDate(getDelegator(), order.getOrderId(), timeZone, locale));
            order.setCustomerName(PartyHelper.getPartyName(getDelegator(), order.getPartyId(), false));
            order.setOrderNameId((order.getOrderName() == null ? "" : order.getOrderName()) + "(" + order.getOrderId() + ")");
            // set status description by find statusId in repository
            if (UtilValidate.isNotEmpty(order.getStatusId())) {
                StatusItem statusItem = findOne(StatusItem.class, map(StatusItem.Fields.statusId, order.getStatusId()));
                order.setStatusDescription(statusItem.getDescription());
            }
        }

        // sort the orders collection by order date desc
        Collections.sort(orders, new EntityComparator(orderBy));
        return orders;
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
        this.fromDate = fromDate;
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
    public void setThruDate(String thruDate) {
        this.thruDate = thruDate;
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


}
