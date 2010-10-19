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

package org.opentaps.common.order;

import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

public class PurchaseOrderFactory extends OrderFactory {

    public static String module = PurchaseOrderFactory.class.getName();
    private String toFacilityContactMechId = null;

    // Pseudo product store for purchase orders.
    private static String productStoreId = "PURCHASING";

    /**
     * Constructor for a new Purchase Order
     * @param dctx
     * @param userLogin
     * @param fromParty
     * @param toParty
     * @param toFacilityContactMechId
     * @throws GenericEntityException 
     */
    public PurchaseOrderFactory(DispatchContext dctx, GenericValue userLogin, String fromParty, String toParty, String toFacilityContactMechId) throws GenericEntityException {
        super(dctx, userLogin, fromParty, toParty, productStoreId);
        this.orderType = "PURCHASE_ORDER";
        this.toFacilityContactMechId = toFacilityContactMechId;
    }

    /**
     * Constructor for a new Purchase Order
     * @param delegator
     * @param dispatcher
     * @param userLogin
     * @param fromParty
     * @param toParty
     * @param toFacilityContactMechId
     * @throws GenericEntityException 
     */
    public PurchaseOrderFactory(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, String fromParty, String toParty, String toFacilityContactMechId) throws GenericEntityException {
        super(delegator, dispatcher, userLogin, fromParty, toParty, productStoreId);
        this.orderType = "PURCHASE_ORDER";
        this.toFacilityContactMechId = toFacilityContactMechId;
    }

    /**
     * Creates the order using the storeOrder service, 
     * calculate taxes first if the flag calculateTaxes is set
     * @return the orderId of the created order
     * @throws GenericServiceException
     */
    @SuppressWarnings("unchecked")
    public String storeOrder() throws GenericServiceException {
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
        callCtxt.put("supplierAgentPartyId", fromParty);
        callCtxt.put("orderTypeId", orderType);
        callCtxt.put("orderItems", orderItems);
        callCtxt.put("orderTerms", orderTerms);
        callCtxt.put("orderAdjustments", orderAdjustments);
        callCtxt.put("orderItemShipGroupInfo", orderItemShipGroupInfos);
        callCtxt.put("currencyUom", currencyUomId);
        callCtxt.put("orderPaymentInfo", orderPaymentPreferences);
        callCtxt.put("productStoreId", productStoreId);
        callCtxt.put("orderName", orderName);

        Map<String, Object> callResults = dispatcher.runSync("storeOrder", callCtxt);
        orderId = (String) callResults.get("orderId");
        return orderId;
    }

    /**
     * Approve the order if it has been already created, else also create it first
     * @throws GenericServiceException
     */
    public void approveOrder() throws GenericServiceException {
        if (orderId == null) {
            storeOrder();
        } else {
            dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin, "statusId", "ITEM_APPROVED"));
        }
    }

    /**
     * Add a shipping group to the order, the shipping address is the facility shipping address.
     * @param carrierPartyId
     * @param shipmentMethodTypeId
     * @return the shipping group id which can be used later to add items
     */
    public String addShippingGroup(String carrierPartyId, String shipmentMethodTypeId) {
        String shippingContactMechId = getPartyShippingAddressId(toParty);
        return addShippingGroup(carrierPartyId, shipmentMethodTypeId, toFacilityContactMechId);
    }

}