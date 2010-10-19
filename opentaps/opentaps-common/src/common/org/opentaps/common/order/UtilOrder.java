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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.shipment.packing.PackingSession;
import org.opentaps.common.product.UtilProduct;

/**
 * UtilOrder - A place for common crmsfa helper methods.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 12 $
 */
public final class UtilOrder {

    private UtilOrder() { }

    private static final String MODULE = UtilOrder.class.getName();
    private static int decimals = UtilNumber.getBigDecimalScale("order.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(decimals, rounding);

    /**
     * Helper method to obtain the earliest ship by date for an order, so it may be invoked within a form widget or ftl.
     * The result will be the formatted date.
     * @param delegator a <code>Delegator</code> value
     * @param orderId the order to get the earliest ship by date for
     * @param timeZone used for date formatting
     * @param locale used for date formatting
     * @return the earliest ship by date for the given order, formatted in a String
     * @deprecated Use the Order domain class instead
     */
    @Deprecated public static String getEarliestShipByDate(Delegator delegator, String orderId, TimeZone timeZone, Locale locale) {
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        Timestamp date = orh.getEarliestShipByDate();
        if (date == null) {
            return "N/A";
        }
        return UtilDateTime.timeStampToString(date, UtilDateTime.getDateFormat(locale), timeZone, locale);
    }

    /**
     * Helper method to get the price information for a product.
     * Returns a map containing the results of the calculateProductPrice or calculatePurchasePrice service.
     * @param request a <code>HttpServletRequest</code> value
     * @param product the product <code>GenericValue</code> to get price info for
     * @return a <code>Map</code> result of the calculate price service
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map getPriceInfo(HttpServletRequest request, GenericValue product) throws GenericServiceException {
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String webSiteId = CatalogWorker.getWebSiteId(request);
        String catalogId = CatalogWorker.getCurrentCatalogId(request);
        GenericValue productStore = ProductStoreWorker.getProductStore(request);
        String productStoreId = productStore.getString("productStoreId");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
        if (cart == null) {
            Debug.logWarning("Cannot determine price for product [" + product.get("productId") + "] because cart is missing from session.", MODULE);
            return FastMap.newInstance();
        }

        String serviceName = null;
        Map input = UtilMisc.toMap("product", product, "currencyUomId", cart.getCurrency(), "partyId", cart.getPartyId(), "userLogin", userLogin);
        if (cart.isSalesOrder()) {
            input.put("webSiteId", webSiteId);
            input.put("prodCatalogId", catalogId);
            input.put("productStoreId", productStoreId);
            input.put("agreementId", cart.getAgreementId());
            input.put("checkIncludeVat", "Y");
            serviceName = "calculateProductPrice";
        } else {
            serviceName = "calculatePurchasePrice";
        }
        Map results = dispatcher.runSync(serviceName, input);
        if (ServiceUtil.isError(results)) {
            Debug.logError(null, ServiceUtil.getErrorMessage(results), MODULE);
            return FastMap.newInstance();
        }
        return results;
    }

    /**
     * Helper method to get all useful display information for a product.
     * The return map will include the following:
     * <ul>
     * <li>imageUrl - the small image url</li>
     * <li>productName - short product name</li>
     * <li>productDescription - short product description</li>
     * <li>priceInfo - the price information from calculateProductPrice or calculatePurchasePrice</li>
     * <li>
     * </ul>
     * @param request a <code>HttpServletRequest</code> value
     * @param product the product <code>GenericValue</code> to get info for
     * @return a <code>Map</code> value
     * @exception GenericServiceException if an error occurs
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map getProductInfo(HttpServletRequest request, GenericValue product) throws GenericServiceException, GenericEntityException {

        // get all required product data
        ProductContentWrapper content = new ProductContentWrapper(product, request);
        String imageUrl = content.get("SMALL_IMAGE_URL").toString();
        String productName = content.get("PRODUCT_NAME").toString();
        String productDescription = content.get("DESCRIPTION").toString();
        Map priceInfo = getPriceInfo(request, product);

        // and make a new Map out of all the useful fields
        Map productMap = FastMap.newInstance();
        productMap.putAll(product.getAllFields());
        productMap.put("imageUrl", imageUrl);
        productMap.put("productName", productName);
        productMap.put("productDescription", productDescription);
        productMap.put("priceInfo", priceInfo);

        return productMap;
    }

    /**
     * Finds the matching orderItemTypeId based on productTypeId and orderTypeId from the ProductOrderItemType entity.
     * @param productTypeId the product type
     * @param orderTypeId the order type
     * @param delegator a <code>Delegator</code> value
     * @return the orderItemTypeId
     * @exception GenericEntityException if an error occurs
     */
    public static String getOrderItemTypeId(String productTypeId, String orderTypeId, Delegator delegator) throws GenericEntityException {
        String orderItemTypeId = null;
        GenericValue productOrderItemType = delegator.findByPrimaryKeyCache("ProductOrderItemType", UtilMisc.toMap("productTypeId", productTypeId, "orderTypeId", orderTypeId));
        if (UtilValidate.isNotEmpty(productOrderItemType)) {
            orderItemTypeId = productOrderItemType.getString("orderItemTypeId");
        } else {
            orderItemTypeId = "PRODUCT_ORDER_ITEM";
        }

        return orderItemTypeId;
    }


    /**
     * Gets the primary customer PO number for the order, which is the first OrderItem.correspondingPoId encountered.
     * @param order the order <code>GenericValue</code> to get the customer PO number for
     * @return the customer PO number for the given order
     * @exception GenericEntityException if an error occurs
     * @deprecated Use the Order domain class instead
     */
    @Deprecated public static String getCustomerPoNumber(GenericValue order) throws GenericEntityException {
        GenericValue item = EntityUtil.getFirst(order.getRelated("OrderItem", UtilMisc.toList("orderItemSeqId")));
        if (item == null) {
            Debug.logWarning("Could not find PO number of order [" + order.get("orderId") + "]:  No order items found.", MODULE);
            return null;
        }
        return item.getString("correspondingPoId");
    }

    /**
     * As above, except pass in the orderId and delegator.
     * @param delegator a <code>Delegator</code> value
     * @param orderId the order to get the customer PO number for
     * @return the customer PO number for the given order
     * @exception GenericEntityException if an error occurs
     * @deprecated Use the Order domain class instead
     */
    @Deprecated public static String getCustomerPoNumber(Delegator delegator, String orderId) throws GenericEntityException {
        GenericValue order = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        if (order == null) {
            Debug.logWarning("Could not find PO number of order [" + orderId + "]:  Order does not exist.", MODULE);
            return null;
        }
        return getCustomerPoNumber(order);
    }

    /**
     * Gets the number of items already invoiced for a given order item.
     * TODO: This doesn't account for when an invoice gets canceled or written off.
     * @param orderItem the order item <code>GenericValue</code> to get the invoiced quantity for
     * @return the invoiced quantity for the given order item
     * @exception GenericEntityException if an error occurs
     * @deprecated Use the Order domain class instead
     */
    @Deprecated public static double getInvoicedQuantity(GenericValue orderItem) throws GenericEntityException {
        double quantity = 0;
        List<GenericValue> billings = orderItem.getRelatedCache("OrderItemBilling");
        for (Iterator<GenericValue> iter = billings.iterator(); iter.hasNext();) {
            GenericValue billing = iter.next();
            quantity += billing.getDouble("quantity").doubleValue();
        }
        return quantity;
    }

    /**
     * Gets the placing customer for the given order.
     * @param delegator a <code>Delegator</code> value
     * @param orderId the order to get the placing customer for
     * @return the placing customer for the given order, or <code>null</code> if none was found
     * @exception GenericEntityException if an error occurs
     * @deprecated Use the Order domain class instead
     */
    @Deprecated public static String getPlacingCustomerPartyId(Delegator delegator, String orderId) throws GenericEntityException {
        if (UtilValidate.isEmpty(orderId)) {
            return null;
        }
        GenericValue orderRole = EntityUtil.getFirst(delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "PLACING_CUSTOMER")));
        return UtilValidate.isNotEmpty(orderRole) ? orderRole.getString("partyId") : null;
    }

    /**
     * Gets the placing customer party name for the given order.
     * @param delegator a <code>Delegator</code> value
     * @param orderId the order to get the placing customer for
     * @param lastNameFirst if true, puts the last name before the first name
     * @return the placing customer party name for the given order, or <code>null</code> if none was found
     * @exception GenericEntityException if an error occurs
     * @deprecated Use the Order domain class instead
     */
    public static String getPlacingCustomerPartyName(Delegator delegator, String orderId, boolean lastNameFirst) throws GenericEntityException {
        String partyId = getPlacingCustomerPartyId(delegator, orderId);
        String partyName = null;
        if (UtilValidate.isNotEmpty(partyId)) {
            partyName = PartyHelper.getPartyName(delegator, partyId, lastNameFirst);
        }
        return partyName;
    }

    /**
     * Gets the bill to customer for the given order.
     * @param delegator a <code>Delegator</code> value
     * @param orderId the order to get the bill to customer for
     * @return the bill to customer for the given order, or <code>null</code> if none was found
     * @exception GenericEntityException if an error occurs
     * @deprecated Use the Order domain class instead
     */
    @Deprecated public static String getBillToCustomerPartyId(Delegator delegator, String orderId) throws GenericEntityException {
        if (UtilValidate.isEmpty(orderId)) {
            return null;
        }
        GenericValue orderRole = EntityUtil.getFirst(delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "BILL_TO_CUSTOMER")));
        return UtilValidate.isNotEmpty(orderRole) ? orderRole.getString("partyId") : null;
    }

    /**
     * Gets the bill to customer party name for the given order.
     * @param delegator a <code>Delegator</code> value
     * @param orderId the order to get the bill to customer for
     * @param lastNameFirst if true, puts the last name before the first name
     * @return the bill to customer party name for the given order, or <code>null</code> if none was found
     * @exception GenericEntityException if an error occurs
     * @deprecated Use the Order domain class instead
     */
    public static String getBillToCustomerPartyName(Delegator delegator, String orderId, boolean lastNameFirst) throws GenericEntityException {
        String partyId = getBillToCustomerPartyId(delegator, orderId);
        String partyName = null;
        if (UtilValidate.isNotEmpty(partyId)) {
            partyName = PartyHelper.getPartyName(delegator, partyId, lastNameFirst);
        }
        return partyName;
    }

    /**
     * Gets the ship to customer for the given order.
     * @param delegator a <code>Delegator</code> value
     * @param orderId the order to get the ship to customer for
     * @return the ship to customer for the given order, or <code>null</code> if none was found
     * @exception GenericEntityException if an error occurs
     * @deprecated Use the Order domain class instead
     */
    @Deprecated public static String getShipToCustomerPartyId(Delegator delegator, String orderId) throws GenericEntityException {
        if (UtilValidate.isEmpty(orderId)) {
            return null;
        }
        GenericValue orderRole = EntityUtil.getFirst(delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "SHIP_TO_CUSTOMER")));
        return UtilValidate.isNotEmpty(orderRole) ? orderRole.getString("partyId") : null;
    }

    /**
     * Gets the ship to customer party name for the given order.
     * @param delegator a <code>Delegator</code> value
     * @param orderId the order to get the ship to customer for
     * @param lastNameFirst if true, puts the last name before the first name
     * @return the ship to customer party name for the given order, or <code>null</code> if none was found
     * @exception GenericEntityException if an error occurs
     * @deprecated Use the Order domain class instead
     */
    public static String getShipToCustomerPartyName(Delegator delegator, String orderId, boolean lastNameFirst) throws GenericEntityException {
        String partyId = getShipToCustomerPartyId(delegator, orderId);
        String partyName = null;
        if (UtilValidate.isNotEmpty(partyId)) {
            partyName = PartyHelper.getPartyName(delegator, partyId, lastNameFirst);
        }
        return partyName;
    }

    public static String getCurrentCashDrawerId(GenericValue userLogin, String currencyUomId) throws GenericEntityException {
        String cashDrawerId = null;
        EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                                    EntityCondition.makeCondition("closeTimestamp", EntityOperator.EQUALS, null),
                                    EntityCondition.makeCondition("operatorUserLoginId", EntityOperator.EQUALS, userLogin.get("userLoginId")),
                                    EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, currencyUomId));
        List<GenericValue> currentCashDrawers = userLogin.getDelegator().findByCondition("CashDrawer", cond, null, UtilMisc.toList("createdStamp DESC"));
        if (UtilValidate.isNotEmpty(currentCashDrawers)) {
            cashDrawerId = EntityUtil.getFirst(currentCashDrawers).getString("cashDrawerId");
        }
        return cashDrawerId;
    }

    public static BigDecimal calculateCashDrawerBalance(GenericValue cashDrawer) throws GenericEntityException {
        BigDecimal netCash = cashDrawer.getBigDecimal("initialAmount").setScale(decimals, rounding);

        // Only CASH transactions count toward the balance
        List<GenericValue> drawerCashTrans = cashDrawer.getDelegator().findByAnd("CashDrawerTransPaymentAndMType", UtilMisc.toMap("cashDrawerId", cashDrawer.get("cashDrawerId"), "paymentMethodTypeId", "CASH"));
        Iterator<GenericValue> dtit = drawerCashTrans.iterator();
        while (dtit.hasNext()) {
            GenericValue drawerTransaction = dtit.next();
            boolean isDisbursement = UtilAccounting.isDisbursement(drawerTransaction);
            if (isDisbursement) {
                netCash = netCash.subtract(drawerTransaction.getBigDecimal("amount"));
            } else {
                netCash = netCash.add(drawerTransaction.getBigDecimal("amount"));
            }
        }
        return netCash.setScale(decimals, rounding);
    }

    public static BigDecimal getOrderOpenAmount(Delegator delegator, String orderId) throws GenericEntityException {
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        return getOrderOpenAmount(orh);
    }

    public static BigDecimal getOrderOpenAmount(OrderReadHelper orh) throws GenericEntityException {
        BigDecimal total = orh.getOrderGrandTotal();
        BigDecimal openAmount = ZERO;

        Delegator delegator = orh.getOrderHeader().getDelegator();
        EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                                    EntityCondition.makeCondition("orderId", orh.getOrderId()),
                                    EntityCondition.makeCondition("paymentCurrencyUomId", orh.getCurrency()),
                                    EntityCondition.makeCondition("paymentStatusId", EntityOperator.IN, UtilMisc.toList("PMNT_SENT", "PMNT_CONFIRMED", "PMNT_RECEIVED")),
                                    EntityCondition.makeCondition("paymentPaymentTypeId", EntityOperator.IN, UtilMisc.toList("CUSTOMER_PAYMENT", "CUSTOMER_DEPOSIT", "CUSTOMER_REFUND")));
        List<GenericValue> prefsAndPayments = delegator.findByCondition("OrderPaymentPrefAndPayment", cond, null, null);
        Iterator<GenericValue> papit = prefsAndPayments.iterator();
        while (papit.hasNext()) {
            GenericValue prefAndPayment = papit.next();
            BigDecimal amount = prefAndPayment.getBigDecimal("paymentAmount");
            if (UtilValidate.isEmpty(amount)) {
                continue;
            }
            if (UtilAccounting.isDisbursement(prefAndPayment)) {
                openAmount = openAmount.subtract(amount);
            } else {
                openAmount = openAmount.add(amount);
            }
        }
        return total.subtract(openAmount).setScale(decimals, rounding);
    }

    public static List<GenericValue> getShipmentOrderShipGroups(Delegator delegator, String shipmentId) throws GenericEntityException {
        Set<GenericValue> orderShipGroups = new LinkedHashSet<GenericValue>();
        List<GenericValue> shipmentOrderShipGroups = delegator.findByAnd("ShipmentAndOrderItemShipGroup", UtilMisc.toMap("shipmentId", shipmentId));
        for (GenericValue shipmentOrderShipGroup : shipmentOrderShipGroups) {
            GenericValue orderItemShipGroup = shipmentOrderShipGroup.getRelatedOne("OrderItemShipGroup");
            orderShipGroups.add(orderItemShipGroup);
        }
        return UtilMisc.toList(orderShipGroups);
    }

    /**
     * Returns true if the orderItem is not physical.
     * @param orderItem a <code>GenericValue</code>
     * @return is the item physical
     * @throws GenericEntityException if an error occurs
     */
    public static boolean isItemPhysical(GenericValue orderItem) throws GenericEntityException {
        return UtilProduct.isPhysical(orderItem.getRelatedOneCache("Product"));
    }

    /**
     * Given a list of values with productId, filter out those whose products are not shippable.
     * @param list a <code>List</code> of <code>GenericValue</code> having a productId field
     * @return a <code>List</code> of filtered product ids
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> filterNonShippableProducts(List<GenericValue> list) throws GenericEntityException {
        if (list == null || list.size() == 0) {
            return list;
        }
        GenericValue first = list.get(0);
        Delegator delegator = first.getDelegator();
        for (Iterator<GenericValue> iter = list.iterator(); iter.hasNext();) {
            GenericValue value = iter.next();
            GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", value.get("productId")));
            GenericValue productType = product.getRelatedOneCache("ProductType");
            if ("N".equals(productType.get("isPhysical"))) {
                iter.remove();
            }
        }
        return list;
    }

    /**
     * Given a list of values with productId, filter out those that are not reserved in the given facility.
     * @param order the order <code>GenericValue</code> for which to get shippable items
     * @param facilityId the facility ID the reservations should be for
     * @param shipGroupSeqId the ship group of the order to consider
     * @return a <code>List</code> of shippable <code>OrderItemShipGrpInvResAndItem</code>
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> getShippableItems(GenericValue order, String facilityId, String shipGroupSeqId) throws GenericEntityException {
        Delegator delegator = order.getDelegator();
        // get list matching the order, ship group and facilityId
        List<GenericValue> orderItems = delegator.findByAnd("OrderItemShipGrpInvResAndItem", UtilMisc.toMap("orderId", order.get("orderId"), "shipGroupSeqId", shipGroupSeqId, "facilityId", facilityId), UtilMisc.toList("orderItemSeqId"));
        List<GenericValue> result = new ArrayList<GenericValue>();
        // filter non shippable items
        orderItems = filterNonShippableProducts(orderItems);
        String lastItem = null;
        for (GenericValue value : orderItems) {
            if (!value.getString("orderItemSeqId").equals(lastItem)) {
                result.add(delegator.findByPrimaryKey("OrderItemAndShipGroupAssoc", UtilMisc.toMap("orderId", order.get("orderId"), "shipGroupSeqId", shipGroupSeqId, "orderItemSeqId", value.get("orderItemSeqId"))));
                lastItem = value.getString("orderItemSeqId");
            }
        }
        return result;
    }

    /**
     * Obtains the quantity still to pack.
     * @param orderItem the order item <code>GenericValue</code> for which to get the quantity to pack
     * @param shipGroupSeqId the ship group of the order to consider
     * @param facilityId the facility to consider
     * @return the quantity to pack of the item in the given facility
     * @throws GenericEntityException if an error occurs
     */
    public static BigDecimal getQuantityToPack(GenericValue orderItem, String shipGroupSeqId, String facilityId) throws GenericEntityException {
        Delegator delegator = orderItem.getDelegator();
        BigDecimal itemQuantity = BigDecimal.ZERO;
        List<GenericValue> reservations = delegator.findByAnd("OrderItemShipGrpInvResAndItem", UtilMisc.toMap("orderId", orderItem.get("orderId"), "orderItemSeqId", orderItem.get("orderItemSeqId"), "shipGroupSeqId", shipGroupSeqId, "facilityId", facilityId), UtilMisc.toList("orderItemSeqId"));
        for (GenericValue res : reservations) {
            itemQuantity = itemQuantity.add(BigDecimal.valueOf(res.getDouble("quantity")));
        }

        return itemQuantity;
    }

    /**
     * Obtains the quantity still to pack.
     * @param orderItem the order item <code>GenericValue</code> for which to get the quantity to pack
     * @param shipGroupSeqId the ship group of the order to consider
     * @param facilityId the facility to consider
     * @param packingSession the current packing session, to subtract packed quantity
     * @return the quantity to pack of the item in the given facility
     * @throws GenericEntityException if an error occurs
     */
    public static BigDecimal getQuantityToPack(GenericValue orderItem, String shipGroupSeqId, String facilityId, PackingSession packingSession) throws GenericEntityException {
        BigDecimal itemQuantity = getQuantityToPack(orderItem, shipGroupSeqId, facilityId);
        itemQuantity = itemQuantity.subtract(packingSession.getPackedQuantity(orderItem.getString("orderId"), orderItem.getString("orderItemSeqId"), shipGroupSeqId, orderItem.getString("productId")));
        return itemQuantity;
    }

    /**
     * Obtains the quantity shipped of the given item in the given ship group.  As an example, if a new order with one
     * order item has 100 GZ-1000 in ship group 1 and 50 more in ship group 2, then this method will return 100 and 50
     * respectively.  If 25 are packed from ship group 1 and shipped off, then this method will return 75 and 50
     * respectively.
     * @param orderItem the order item <code>GenericValue</code> for which to get the quantity shipped
     * @param shipGroupSeqId the ship group of the order to consider
     * @return the quantity shipped for the given order item and ship group
     * @throws GenericEntityException if an error occurs
     */
    public static BigDecimal getQuantityShippedForItemAndShipGroup(GenericValue orderItem, String shipGroupSeqId) throws GenericEntityException {
        BigDecimal shipped = BigDecimal.ZERO;
        Delegator delegator = orderItem.getDelegator();
        List<GenericValue> issuances = delegator.findByAnd("ItemIssuance", UtilMisc.toMap("orderId", orderItem.get("orderId"), "orderItemSeqId", orderItem.get("orderItemSeqId"), "shipGroupSeqId", shipGroupSeqId));
        for (GenericValue issuance : issuances) {
            shipped = shipped.add(issuance.getBigDecimal("quantity"));
        }
        // this rounding is questionable and should be configured some other way
        return shipped.setScale(OrderReadHelper.scale, OrderReadHelper.rounding);
    }
}
