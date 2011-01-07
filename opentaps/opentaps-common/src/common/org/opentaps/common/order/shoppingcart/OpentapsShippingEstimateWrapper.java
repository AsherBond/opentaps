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
/* Copyright (c) Open Source Strategies, Inc. */
/*
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.common.order.shoppingcart;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.shipping.ShippingEvents;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * This class replaces the OFBiz ShippingEstimateWrapper, which is still used by the Order and eCommerce applications and is
 *  still possible to instantiate.
 *
 * @version    $Rev$
 */
public class OpentapsShippingEstimateWrapper implements Serializable {

    private static final String MODULE = OpentapsShippingEstimateWrapper.class.getName();

    private static final int DECIMALS = UtilNumber.getBigDecimalScale("order.decimals");
    private static final int ROUNDING = UtilNumber.getBigDecimalRoundingMode("order.rounding");

    protected String dispatcherName = null;
    protected Map shippingEstimates = null;
    protected List shippingMethods = null;
    protected GenericValue shippingAddress = null;
    protected Map shippableItemFeatures = null;
    protected List shippableItemSizes = null;
    protected List shippableItemInfo = null;
    protected String productStoreId = null;
    protected BigDecimal shippableQuantity = BigDecimal.ZERO;
    protected BigDecimal shippableWeight = BigDecimal.ZERO;
    protected BigDecimal shippableTotal = BigDecimal.ZERO;
    protected Map availableCarrierServices = new HashMap();
    protected ShoppingCart cart = null;
    protected int shipGroupSeqId = 0;

    protected String supplierPartyId = null;
    protected GenericValue shippingOriginAddress = null;

    public static OpentapsShippingEstimateWrapper getWrapper(LocalDispatcher dispatcher, ShoppingCart cart, int shipGroup) {
        return new OpentapsShippingEstimateWrapper(dispatcher, cart, shipGroup);
    }

    public OpentapsShippingEstimateWrapper(LocalDispatcher dispatcher, ShoppingCart cart, int shipGroup) {
        this.cart = cart;
        this.shipGroupSeqId = shipGroup;

        this.dispatcherName = dispatcher.getName();

        this.shippableItemFeatures = cart.getFeatureIdQtyMap(shipGroup);
        this.shippableItemSizes = cart.getShippableSizes(shipGroup);
        this.shippableItemInfo = cart.getShippableItemInfo(shipGroup);
        this.shippableQuantity = cart.getShippableQuantity(shipGroup);
        this.shippableWeight = cart.getShippableWeight(shipGroup);
        this.shippableTotal = cart.getShippableTotal(shipGroup);
        this.shippingAddress = cart.getShippingAddress(shipGroup);
        this.productStoreId = cart.getProductStoreId();
        this.supplierPartyId = cart.getSupplierPartyId(shipGroup);

        // load the drop ship origin address
        if (supplierPartyId != null) {
            try {
                this.shippingOriginAddress = ShippingEvents.getShippingOriginContactMech(cart.getDelegator(), supplierPartyId);
            } catch (GeneralException e) {
                Debug.logError("Cannot obtain origin shipping address for supplier [" + supplierPartyId + "] due to: " + e.getMessage(), MODULE);
            }
        }

        this.loadShippingMethods();
        this.loadEstimates();
        if (this.cart instanceof OpentapsShoppingCart) {
            ((OpentapsShoppingCart) this.cart).setShipEstimateWrapper(shipGroup, this);
        }
    }

    @SuppressWarnings("unchecked")
    protected void loadShippingMethods() {
        try {
            this.shippingMethods = ProductStoreWorker.getAvailableStoreShippingMethods(this.cart.getDelegator(), productStoreId,
                    shippingAddress, shippableItemSizes, shippableItemFeatures, shippableWeight, shippableTotal);

            // Replace ProductStoreShipMethodView with extended ProductStoreShipMethAndCarrier records to make it easier to get the carrierServiceCodes
            if (UtilValidate.isNotEmpty(this.shippingMethods)) {
                List<GenericValue> prodStoreShipMethIds = EntityUtil.getFieldListFromEntityList(this.shippingMethods, "productStoreShipMethId", true);
                this.shippingMethods = this.cart.getDelegator().findByCondition("ProductStoreShipMethAndCarrier", EntityCondition.makeCondition("productStoreShipMethId", EntityOperator.IN, prodStoreShipMethIds), null,  UtilMisc.toList("sequenceNumber"));
            }
        } catch (Throwable t) {
            Debug.logError(t, MODULE);
        }

        // Initialize the map of available services for each carrier. This starts out populated with every service code defined in the CarrierShipmentMethod records,
        //  and will be trimmed by the loadEstimatesUPS() method and its siblings.
        List<String> carrierPartyIds = EntityUtil.getFieldListFromEntityList(this.shippingMethods, "partyId", true);
        if (UtilValidate.isNotEmpty(carrierPartyIds)) {
            for (String carrierPartyId : carrierPartyIds) {
                List<GenericValue> carrierProdStoreShipMethods = EntityUtil.filterByAnd(this.shippingMethods, UtilMisc.toMap("partyId", carrierPartyId));
                carrierProdStoreShipMethods = EntityUtil.filterByCondition(carrierProdStoreShipMethods, EntityCondition.makeCondition("carrierServiceCode", EntityOperator.NOT_EQUAL, null));
                availableCarrierServices.put(carrierPartyId, EntityUtil.getFieldListFromEntityList(carrierProdStoreShipMethods, "carrierServiceCode", true));
            }
        }
    }

    protected void loadEstimates() {

        this.shippingEstimates = new HashMap();

        // Rate shop for all UPS methods
        loadEstimatesUPS();

        // This is where we get all the other non-rate shop methods, USPS, etc., plus UPS methods without designated serviceName or carrierServiceCodes
        loadOtherEstimates();

        addCodSurchargesToEstimates();
    }

    /*
     * This method will loop through the List of shippingMethods and then fill in a shipping estimate for each one which does not already have a rate estimate.
     * The idea is to run this after any group rate shopping methods, such as the UPS Rate Shop, so that other shipping methods, such as USPS or any other rate-table
     * based method, would get a rate.
     * Note that this method is basically the old OFBiz loadEstimates, except that we check to make sure that a rate estimate is not already available first.
     */
    @SuppressWarnings("unchecked")
    protected void loadOtherEstimates() {
        if (shippingMethods != null) {
            Iterator i = shippingMethods.iterator();
            while (i.hasNext()) {
                GenericValue shipMethod = (GenericValue) i.next();
                String shippingMethodTypeId = shipMethod.getString("shipmentMethodTypeId");
                String carrierRoleTypeId = shipMethod.getString("roleTypeId");
                String carrierPartyId = shipMethod.getString("partyId");

                // Only calculate the shipment estimate if there is not already one
                if (shippingEstimates.containsKey(shipMethod)) {
                    continue;
                }

                // Skip this estimate if the method has a carrierServiceCode that isn't available to the current origin/destination addresses
                String carrierServiceCode = shipMethod.getString("carrierServiceCode");
                if (UtilValidate.isNotEmpty(carrierServiceCode)) {
                    if (!availableCarrierServices.containsKey(carrierPartyId)) {
                        continue;
                    }
                    if (UtilValidate.isEmpty(availableCarrierServices.get(carrierPartyId))) {
                        continue;
                    }
                    if (!((List) availableCarrierServices.get(carrierPartyId)).contains(carrierServiceCode)) {
                        continue;
                    }
                }

                String shippingCmId = shippingAddress != null ? shippingAddress.getString("contactMechId") : null;

                Map estimateMap = ShippingEvents.getShipGroupEstimate(GenericDispatcher.getLocalDispatcher(this.dispatcherName, cart.getDelegator()), cart.getDelegator(), "SALES_ORDER",
                        shippingMethodTypeId, carrierPartyId, carrierRoleTypeId, shippingCmId, productStoreId, supplierPartyId,
                        shippableItemInfo, shippableWeight, shippableQuantity, shippableTotal, cart.getPartyId(), cart.getProductStoreShipMethId());
                shippingEstimates.put(shipMethod, estimateMap.get("shippingTotal"));
            }
        }
    }

    /*
     * This method will use the UPS Rate Shop API to get estimates for all shipping methods.
     */
    @SuppressWarnings("unchecked")
    protected void loadEstimatesUPS() {

        if (UtilValidate.isEmpty(this.shippingAddress)) {
            Debug.logInfo("Shipping Address in shopping cart is null", MODULE);
            return;
        }

        // Set up input map for upsRateEstimate
        Map input = UtilMisc.toMap("upsRateInquireMode", "Shop"); // triggers the mode of estimate we want
        input.put("shippableQuantity", this.shippableQuantity);
        input.put("shippableWeight", this.shippableWeight);
        input.put("productStoreId", this.productStoreId);
        input.put("carrierRoleTypeId", "CARRIER");
        input.put("carrierPartyId", "UPS");
        input.put("shippingContactMechId", this.shippingAddress.get("contactMechId"));
        if (this.shippingOriginAddress != null) {
            input.put("shippingOriginContactMechId", this.shippingOriginAddress.get("contactMechId"));
        }
        input.put("shippableItemInfo", this.shippableItemInfo);
        input.put("shipmentMethodTypeId", "XXX"); // Dummy value for required field that isn't necessary for rate shop requests
        input.put("shippableTotal", this.shippableTotal);

        try {

            // Results are the estimated amounts keyed to the carrierServiceCode
            Map results = GenericDispatcher.getLocalDispatcher(this.dispatcherName, this.cart.getDelegator()).runSync("upsRateEstimate", input);
            if (ServiceUtil.isError(results) || ServiceUtil.isFailure(results)) {
                Debug.logError(ServiceUtil.getErrorMessage(results), MODULE);
                return;
            }
            Map upsRateCodeMap = (Map) results.get("upsRateCodeMap");

            // Populate the available UPS service codes for the wrapper with the codes returned
            availableCarrierServices.put("UPS", UtilMisc.toList(upsRateCodeMap.keySet()));

            // These are shipping methods which would have used upsRateEstimate, so we can populate them with the results from rate shop.
            // This is in case there are some UPS shipping methods which do not use the upsRateEstimate but use rate table, etc.
            List relevantShippingMethods = EntityUtil.filterByAnd(shippingMethods, UtilMisc.toMap("serviceName", "upsRateEstimate"));

            // Key each ProductStoreShipmentMethAndCarrier to the amount for the corresponding shipment method
            for (Iterator iter = relevantShippingMethods.iterator(); iter.hasNext();) {
                GenericValue m = (GenericValue) iter.next();
                String carrierServiceCode = m.getString("carrierServiceCode");

                // Skip if there's no service code or if the service isn't available
                if (UtilValidate.isEmpty(carrierServiceCode)) {
                    continue;
                }
                if (!upsRateCodeMap.containsKey(carrierServiceCode)) {
                    continue;
                }

                BigDecimal amount = (BigDecimal) upsRateCodeMap.get(carrierServiceCode);
                shippingEstimates.put(m, amount);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
    }

    @SuppressWarnings("unchecked")
    protected void addCodSurchargesToEstimates() {
        if (cart != null && cart instanceof OpentapsShoppingCart && shippingEstimates != null) {
            OpentapsShoppingCart opentapsCart = (OpentapsShoppingCart) cart;
            if (opentapsCart.getCOD(shipGroupSeqId)) {

                Iterator eit = shippingEstimates.keySet().iterator();
                while (eit.hasNext()) {
                    GenericValue carrierShipmentMethod = (GenericValue) eit.next();
                    BigDecimal codSurcharge = carrierShipmentMethod.getBigDecimal("codSurcharge");
                    BigDecimal estimate = (BigDecimal) shippingEstimates.get(carrierShipmentMethod);
                    if (UtilValidate.isNotEmpty(estimate) && UtilValidate.isNotEmpty(codSurcharge)) {
                        shippingEstimates.put(carrierShipmentMethod, estimate.add(codSurcharge));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List getShippingMethods() {
        return shippingMethods;
    }

    @SuppressWarnings("unchecked")
    public Map getAllEstimates() {
        return shippingEstimates;
    }

    public BigDecimal getShippingEstimate(String shipmentMethodTypeId, String carrierPartyId) {
        GenericValue storeCarrierShipMethod = EntityUtil.getFirst(EntityUtil.filterByAnd(shippingMethods, UtilMisc.toMap("shipmentMethodTypeId", shipmentMethodTypeId, "partyId", carrierPartyId)));
        if (UtilValidate.isEmpty(storeCarrierShipMethod)) {
            return null;
        }
        BigDecimal est = (BigDecimal) shippingEstimates.get(storeCarrierShipMethod);
        if (UtilValidate.isEmpty(est)) {
            return null;
        }
        BigDecimal estBd = est.setScale(DECIMALS, ROUNDING);
        return estBd;
    }

    public BigDecimal getShippingEstimate(GenericValue storeCarrierShipMethod) {
        if (UtilValidate.isEmpty(storeCarrierShipMethod)) {
            return null;
        }
        return getShippingEstimate(storeCarrierShipMethod.getString("shipmentMethodTypeId"), storeCarrierShipMethod.getString("partyId"));
    }

    public void setShippingAddress(GenericValue shippingAddress) {
        this.shippingAddress = shippingAddress;
        loadShippingMethods();
        loadEstimates();
    }

    public GenericValue getShippingAddress() {
        return this.shippingAddress;
    }

}
