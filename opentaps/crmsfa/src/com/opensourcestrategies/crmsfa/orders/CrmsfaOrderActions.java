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
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.order.OrderViewForListing;
import org.opentaps.domain.order.SalesOrderLookupRepositoryInterface;
import org.opentaps.foundation.action.ActionContext;

/**
 * CrmsfaOrderActions - Java Actions for sales orders.
 */
public final class CrmsfaOrderActions {

    @SuppressWarnings("unused")
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
        SalesOrderLookupRepositoryInterface salesOrderLookupRepository = dd.getOrderDomain().getSalesOrderLookupRepository();
        String organizationPartyId = UtilProperties.getPropertyValue("opentaps", "organizationPartyId");
        String userLoginId = ac.getUser().getOfbizUserLogin().getString("userLoginId");
        // pass locale and timeZone instances for format the date string
        salesOrderLookupRepository.setLocale(locale);
        salesOrderLookupRepository.setTimeZone(timeZone);
        salesOrderLookupRepository.setUserLoginId(userLoginId);
        salesOrderLookupRepository.setOrganizationPartyId(organizationPartyId);
        if (UtilValidate.isNotEmpty(fromDate)) {
            salesOrderLookupRepository.setFromDate(fromDate);
        }
        if (UtilValidate.isNotEmpty(thruDate)) {
            salesOrderLookupRepository.setThruDate(thruDate);
        }
        if (UtilValidate.isNotEmpty(statusId)) {
            salesOrderLookupRepository.setStatusId(statusId);
        }
        if (UtilValidate.isNotEmpty(partyId)) {
            salesOrderLookupRepository.setCustomerPartyId(partyId);
        }
        if (UtilValidate.isNotEmpty(correspondingPoId)) {
            salesOrderLookupRepository.setPurchaseOrderId(correspondingPoId);
        }
        if (UtilValidate.isNotEmpty(orderId)) {
            salesOrderLookupRepository.setOrderId(orderId);
        }
        if (UtilValidate.isNotEmpty(orderName)) {
            salesOrderLookupRepository.setOrderName(orderName);
        }
        if (UtilValidate.isNotEmpty(lotId)) {
            salesOrderLookupRepository.setLotId(lotId);
        }
        if (UtilValidate.isNotEmpty(serialNumber)) {
            salesOrderLookupRepository.setSerialNumber(serialNumber);
        }
        if (UtilValidate.isNotEmpty(createdBy)) {
            salesOrderLookupRepository.setCreatedBy(createdBy);
        }
        if (UtilValidate.isNotEmpty(externalId)) {
            salesOrderLookupRepository.setExteralOrderId(externalId);
        }
        if (UtilValidate.isNotEmpty(productStoreId)) {
            salesOrderLookupRepository.setProductStoreId(productStoreId);
        }
        salesOrderLookupRepository.setOrderBy(orderBy);
        salesOrderLookupRepository.enablePagination(false);

        List<OrderViewForListing> orders = salesOrderLookupRepository.findOrders();
        List<Map<String, Object>> orderMaps = FastList.<Map<String, Object>>newInstance();
        // return the map collection for the screen render
        for (OrderViewForListing order : orders) {
            orderMaps.add(order.toMap());
        }

        ac.put("ordersListIt", orderMaps);
    }
}
