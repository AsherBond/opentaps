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

import javax.servlet.http.HttpServletRequest;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.OrderHeaderItemAndRolesAndInvPending;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.search.order.SalesOrderSearchRepositoryInterface;
import org.opentaps.foundation.action.ActionContext;

/**
 * CrmsfaOrderActions - Java Actions for sales orders.
 */
public class CrmsfaOrderActions {
    private static final String MODULE = CrmsfaOrderActions.class.getName();

    private CrmsfaOrderActions() {}
    
    /**
     * Action for the lookup sales order screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void findOrders(Map<String, Object> context) throws GeneralException {
        ActionContext ac = new ActionContext(context);
        HttpServletRequest request = ac.getRequest();
        Locale locale = ac.getLocale();

        TimeZone timeZone = UtilCommon.getTimeZone(request);

        // order by
        String orderParam = ac.getParameter("ordersOrderBy");
        if (UtilValidate.isEmpty(orderParam)) orderParam = "orderDate DESC";
        List orderBy = UtilMisc.toList(orderParam);

        // possible fields we're searching by
        String partyId = UtilCommon.getParameter(request, "partyIdSearch");
        // from URL GET request if form field wasn't set
        if (partyId == null) partyId = ac.getParameter("partyId");
        String statusId = UtilCommon.getParameter(request, "statusId");
        String correspondingPoId = UtilCommon.getParameter(request, "correspondingPoId");
        String orderId = UtilCommon.getParameter(request, "orderId");
        String orderName = UtilCommon.getParameter(request, "orderName");
        String lotId = UtilCommon.getParameter(request, "lotId");
        String serialNumber = UtilCommon.getParameter(request, "serialNumber");
        String fromDate = UtilHttp.makeParamValueFromComposite(request, "fromDate", locale);
        String thruDate = UtilHttp.makeParamValueFromComposite(request, "thruDate", locale);
        Debug.logInfo("fromDate : " + fromDate + ", thruDate : " + thruDate, MODULE);
        String createdBy = UtilCommon.getParameter(request, "createdBy");
        String externalId = UtilCommon.getParameter(request, "externalId");
        String  productStoreId = UtilCommon.getParameter(request, "productStoreId");
        
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
        List<OrderHeaderItemAndRolesAndInvPending> orders = salesOrderSearchRepository.findOrders();
        List<Map> orderMaps = FastList.newInstance();
        for (OrderHeaderItemAndRolesAndInvPending order : orders) {
            orderMaps.add(order.toMap());
        }
        context.put("ordersListIt", orderMaps);
    }
}
