/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.common.order;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * A simple helper for the storeOrder service.
 */
public class SalesOrderFactory extends OrderFactory {

    private static final String MODULE = SalesOrderFactory.class.getName();

    /**
     * Constructor for a new Sales Order.
     * @param dctx a <code>DispatchContext</code> value
     * @param userLogin the user login <code>GenericValue</code>
     * @param fromParty the vendor party id
     * @param toParty the customer party id
     * @param productStoreId the product store id
     * @throws GenericEntityException if an error occurs
     */
    public SalesOrderFactory(DispatchContext dctx, GenericValue userLogin, String fromParty, String toParty, String productStoreId) throws GenericEntityException {
        super(dctx, userLogin, fromParty, toParty, productStoreId);
        this.orderType = "SALES_ORDER";
    }

    /**
     * Constructor for a new Sales Order.
     * @param delegator a <code>GenericDelegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin the user login <code>GenericValue</code>
     * @param fromParty the vendor party id
     * @param toParty the customer party id
     * @param productStoreId the product store id
     * @throws GenericEntityException if an error occurs
     */
    public SalesOrderFactory(GenericDelegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, String fromParty, String toParty, String productStoreId) throws GenericEntityException {
        super(delegator, dispatcher, userLogin, fromParty, toParty, productStoreId);
        this.orderType = "SALES_ORDER";
    }

    /**
     * Creates the order using the storeOrder service,
     * calculate taxes first if the flag calculateTaxes is set.
     * @return the orderId of the created order
     * @throws GenericServiceException if an error occurs
     */
    public String storeOrder() throws GenericServiceException {
        return storeOrder(null);
    }

    /**
     * Creates the order at given time.
     * @see org.opentaps.common.order.SalesOrderFactory#storeOrder()
     *
     * @param orderDate creation time of new order
     * @return the orderId of created oreder
     * @throws GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public String storeOrder(Timestamp orderDate) throws GenericServiceException {
        if (calculateTaxes) {
            calcTax();
        }

        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", userLogin);
        callCtxt.put("partyId", toParty);
        callCtxt.put("billToCustomerPartyId", toParty);
        callCtxt.put("shipToCustomerPartyId", toParty);
        callCtxt.put("endUserCustomerPartyId", toParty);
        callCtxt.put("placingCustomerPartyId", toParty);
        callCtxt.put("billFromVendorPartyId", fromParty);
        callCtxt.put("shipFromVendorPartyId", fromParty);
        callCtxt.put("orderTypeId", orderType);
        callCtxt.put("orderItems", orderItems);
        callCtxt.put("orderTerms", orderTerms);
        callCtxt.put("orderAdjustments", orderAdjustments);
        callCtxt.put("orderItemShipGroupInfo", orderItemShipGroupInfos);
        callCtxt.put("currencyUom", currencyUomId);
        callCtxt.put("orderPaymentInfo", orderPaymentPreferences);
        callCtxt.put("productStoreId", productStoreId);
        callCtxt.put("orderName", orderName);
        if (orderContactMechs.size() > 0) {
            callCtxt.put("orderContactMechs", orderContactMechs);
        }

        if (orderDate != null) {
            callCtxt.put("orderDate", orderDate);
        }

        Map<String, Object> callResults = dispatcher.runSync("storeOrder", callCtxt);
        if (ServiceUtil.isSuccess(callResults)) {
            orderId = (String) callResults.get("orderId");
            return orderId;
        } else {
            Debug.logError("The service storeOrder did not return success: " + ServiceUtil.getErrorMessage(callResults), MODULE);
            return null;
        }

    }

    /**
     * Approve the order if it has been already created, else also create it first.
     * @throws GenericServiceException if an error occurs
     */
    public void approveOrder() throws GenericServiceException {
        if (orderId == null) {
            storeOrder();
        }
        dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin, "statusId", "ITEM_APPROVED"));
    }


}
