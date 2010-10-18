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
package org.opentaps.purchasing.order;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javolution.util.FastList;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.StatusItem;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.order.OrderViewForListing;
import org.opentaps.domain.order.PurchaseOrderLookupRepositoryInterface;
import org.opentaps.foundation.action.ActionContext;


/**
 * PurchasingOrderActions - Java Actions for purchasing orders.
 */
public final class PurchasingOrderActions {

    @SuppressWarnings("unused")
    private static final String MODULE = PurchasingOrderActions.class.getName();

    private PurchasingOrderActions() { }

    /**
     * Action for the lookup purchasing order screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void findOrders(Map<String, Object> context) throws GeneralException {
        ActionContext ac = new ActionContext(context);
        Locale locale = ac.getLocale();
        TimeZone timeZone = ac.getTimeZone();

        DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(ac);
        PurchaseOrderLookupRepositoryInterface purchaseOrderLookupRepository = dd.getOrderDomain().getPurchaseOrderLookupRepository();
        InventoryRepositoryInterface inventoryRepository = dd.getInventoryDomain().getInventoryRepository();

        // get the list of statuses for the parametrized form ftl
        List<StatusItem> statuses = purchaseOrderLookupRepository.findListCache(StatusItem.class, purchaseOrderLookupRepository.map(StatusItem.Fields.statusTypeId, "ORDER_STATUS"), UtilMisc.toList(StatusItem.Fields.sequenceId.name()));
        List<Map<String, Object>> statusList = new FastList<Map<String, Object>>();
        for (StatusItem s : statuses) {
            Map<String, Object> status = s.toMap();
            status.put("statusDescription", s.get(StatusItem.Fields.description.name(), locale));
            statusList.add(status);
        }
        ac.put("statusItems", statusList);

        // Initial values for the variables passed to the FTL script
        List<Map<String, Object>> resultList = FastList.<Map<String, Object>>newInstance();
        int resultTotalSize = 0;
        String extraParameters = "";

        // populate the organization party which lookup purchase orders requires
        String organizationPartyId = UtilCommon.getOrganizationPartyId(ac.getRequest());
        String facilityId = (String) ac.getRequest().getSession().getAttribute("facilityId");
        if (UtilValidate.isEmpty(organizationPartyId)) {
            organizationPartyId = inventoryRepository.getFacilityById(facilityId).getOwnerPartyId();
        }


        // order by
        String orderParam = ac.getParameter("ordersOrderBy");
        if (UtilValidate.isEmpty(orderParam)) {
            orderParam = "orderDate DESC";
        }
        List<String> orderBy = UtilMisc.toList(orderParam);

        // possible fields we're searching by
        String partyId = ac.getParameter("supplierPartyId");
        // from URL GET request if form field wasn't set
        if (partyId == null) {
            partyId = ac.getParameter("supplierPartyId");
        }
        String statusId = ac.getParameter("statusId");
        String orderId = ac.getParameter("orderId");
        String orderName = ac.getParameter("orderName");
        String performFind = ac.getParameter("performFind");

        // We only perform a lookup if either this is the "Open Orders" form, or if the perform
        // lookup flag is either passed as a parameter or has already been passed in the context
        // (e.g. by setting it up in a screen)
        if ("Y".equals(ac.getString("performFind")) || "Y".equals(ac.getParameter("performFind")) || "true".equals(ac.getString("onlyOpenOrders"))) {
            extraParameters = "&orderId=" + orderId + "&orderName=" + orderName + "&supplierPartyId=" + partyId + "&statusId=" + statusId + "&performFind=" + performFind;
            purchaseOrderLookupRepository.setLocale(locale);
            purchaseOrderLookupRepository.setTimeZone(timeZone);

            purchaseOrderLookupRepository.setOrganizationPartyId(organizationPartyId);
            if (UtilValidate.isNotEmpty(statusId)) {
                purchaseOrderLookupRepository.setStatusId(statusId);
            }

            if (UtilValidate.isNotEmpty(partyId)) {
                purchaseOrderLookupRepository.setSupplierPartyId(partyId);
            }

            if (UtilValidate.isNotEmpty(orderId)) {
                purchaseOrderLookupRepository.setOrderId(orderId);
            }

            if (UtilValidate.isNotEmpty(orderName)) {
                purchaseOrderLookupRepository.setOrderName(orderName);
            }

            if ("true".equals(ac.getString("onlyOpenOrders"))) {
                purchaseOrderLookupRepository.setFindDesiredOnly(true);
            }

            purchaseOrderLookupRepository.setOrderBy(orderBy);
            purchaseOrderLookupRepository.enablePagination(false);
            List<OrderViewForListing> orders = purchaseOrderLookupRepository.findOrders();
            // return the map collection for the screen render
            for (OrderViewForListing order : orders) {
                resultList.add(order.toMap());
            }
            resultTotalSize = resultList.size();
        }

        ac.put("purchaseOrders", resultList);
        ac.put("purchaseOrdersTotalSize", resultTotalSize);
        ac.put("extraParameters", extraParameters);

    }
}
