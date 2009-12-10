/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package com.opensourcestrategies.crmsfa.orders;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javolution.util.FastList;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.common.domain.order.OrderViewForListing;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.search.order.SalesOrderSearchRepositoryInterface;
import org.opentaps.foundation.action.ActionContext;

/**
 * CrmsfaOrderActions - Java Actions for sales orders.
 */
public final class CrmsfaOrderActions {

    private static final String MODULE = CrmsfaOrderActions.class.getName();

    private CrmsfaOrderActions() { }

    /**
     * Action for the lookup sales order screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void findOrders(Map<String, Object> context) throws GeneralException {
        ActionContext ac = new ActionContext(context);
        Locale locale = ac.getLocale();
        TimeZone timeZone = ac.getTimeZone();

        // order by
        String orderParam = ac.getParameter("ordersOrderBy");
        if (UtilValidate.isEmpty(orderParam)) {
            orderParam = "orderDate DESC";
        }
        List<String> orderBy = UtilMisc.toList(orderParam);

        // possible fields we're searching by
        String partyId = ac.getParameter("partyIdSearch");
        // from URL GET request if form field wasn't set
        if (partyId == null) {
            partyId = ac.getParameter("partyId");
        }
        String statusId = ac.getParameter("statusId");
        String correspondingPoId = ac.getParameter("correspondingPoId");
        String orderId = ac.getParameter("orderId");
        String orderName = ac.getParameter("orderName");
        String lotId = ac.getParameter("lotId");
        String serialNumber = ac.getParameter("serialNumber");
        String fromDate = ac.getCompositeParameter("fromDate");
        String thruDate = ac.getCompositeParameter("thruDate");
        String createdBy = ac.getParameter("createdBy");
        String externalId = ac.getParameter("externalId");
        String  productStoreId = ac.getParameter("productStoreId");

        DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(ac);
        SalesOrderSearchRepositoryInterface salesOrderSearchRepository = dd.getOrderDomain().getSalesOrderSearchRepository();
        String organizationPartyId = UtilProperties.getPropertyValue("opentaps", "organizationPartyId");
        String userLoginId = ac.getUser().getOfbizUserLogin().getString("userLoginId");
        // pass locale and timeZone instances for format the date string
        salesOrderSearchRepository.setLocale(locale);
        salesOrderSearchRepository.setTimeZone(timeZone);
        salesOrderSearchRepository.setUserLoginId(userLoginId);
        salesOrderSearchRepository.setOrganizationPartyId(organizationPartyId);
        if (UtilValidate.isNotEmpty(fromDate)) {
            salesOrderSearchRepository.setFromDate(fromDate);
        }
        if (UtilValidate.isNotEmpty(thruDate)) {
            salesOrderSearchRepository.setThruDate(thruDate);
        }
        if (UtilValidate.isNotEmpty(statusId)) {
            salesOrderSearchRepository.setStatusId(statusId);
        }
        if (UtilValidate.isNotEmpty(partyId)) {
            salesOrderSearchRepository.setCustomerPartyId(partyId);
        }
        if (UtilValidate.isNotEmpty(correspondingPoId)) {
            salesOrderSearchRepository.setPurchaseOrderId(correspondingPoId);
        }
        if (UtilValidate.isNotEmpty(orderId)) {
            salesOrderSearchRepository.setOrderId(orderId);
        }
        if (UtilValidate.isNotEmpty(orderName)) {
            salesOrderSearchRepository.setOrderName(orderName);
        }
        if (UtilValidate.isNotEmpty(lotId)) {
            salesOrderSearchRepository.setLotId(lotId);
        }
        if (UtilValidate.isNotEmpty(serialNumber)) {
            salesOrderSearchRepository.setSerialNumber(serialNumber);
        }
        if (UtilValidate.isNotEmpty(createdBy)) {
            salesOrderSearchRepository.setCreatedBy(createdBy);
        }
        if (UtilValidate.isNotEmpty(externalId)) {
            salesOrderSearchRepository.setExteralOrderId(externalId);
        }
        if (UtilValidate.isNotEmpty(productStoreId)) {
            salesOrderSearchRepository.setProductStoreId(productStoreId);
        }
        salesOrderSearchRepository.setOrderBy(orderBy);
        List<OrderViewForListing> orders = salesOrderSearchRepository.findOrders();
        List<Map> orderMaps = FastList.newInstance();
        // return the map collection for the screen render
        for (OrderViewForListing order : orders) {
            orderMaps.add(order.toMap());
        }
        ac.put("ordersListIt", orderMaps);
    }
}
