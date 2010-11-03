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

package org.opentaps.tests.crmsfa.orders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javolution.util.FastMap;
import junit.framework.TestCase;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderAdjustment;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.tests.OpentapsTestCase;
import org.opentaps.warehouse.shipment.ShippingHelper;

/**
 * Order related tests.
 */
public class OrderTestCase extends OpentapsTestCase {

    private static final String MODULE = OrderTestCase.class.getName();

    public static final BigDecimal ZERO_BASE = new BigDecimal("0.000");
    public static final BigDecimal PERCENT_SCALE = new BigDecimal("100.000");

    /** The default shipping address of DemoAccount1. */
    public static final String DEFAULT_SHIPPING_ADDRESS = "DemoAddress1";
    /** The secondary shipping address of DemoAccount1, mostly used for split shipment. */
    public static final String SECONDARY_SHIPPING_ADDRESS = "DemoAddress2";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // remove the promo 9020 which is time based and does not work well with unit tests
        delegator.removeByAnd("ProductStorePromoAppl", UtilMisc.toMap("productStoreId", "9000", "productPromoId", "9020"));
    }

    @Override
    public void tearDown() throws Exception {
        // recreate the promo 9020 which we removed in setUp
        delegator.create("ProductStorePromoAppl", UtilMisc.<String, Object>toMap("productStoreId", "9000", "productPromoId", "9020", "sequenceNum", Long.valueOf(1L), "fromDate", UtilDate.toTimestamp("2001-05-13 12:00:00.0", TimeZone.getDefault(), Locale.getDefault())));
        super.tearDown();
    }

    /**
     * Asserts the given order is Canceled.
     * @param orderId the order id
     */
    public void assertOrderCancelled(String orderId) {
        assertOrderCancelled("", orderId);
    }

    /**
     * Asserts the given order is Canceled.
     * @param message the error message to display on failure
     * @param orderId the order id
     */
    public void assertOrderCancelled(String message, String orderId) {
        assertOrderStatus(message, orderId, "ORDER_CANCELLED", "ITEM_CANCELLED");
    }

    /**
     * Asserts the given order is Completed.
     * @param orderId the order id
     */
    public void assertOrderCompleted(String orderId) {
        assertOrderCompleted("", orderId);
    }

    /**
     * Asserts the given order is Completed.
     * @param message the error message to display on failure
     * @param orderId the order id
     */
    public void assertOrderCompleted(String message, String orderId) {
        assertOrderStatus(message, orderId, "ORDER_COMPLETED", "ITEM_COMPLETED");
    }

    /**
     * Asserts the given order has the given status.
     * @param message the error message to display on failure
     * @param orderId the order id
     * @param statusId the status the order should have, ie: ORDER_CANCELLED
     * @param itemStatusId the equivalent status the order items should have, ie: ITEM_CANCELLED
     */
    public void assertOrderStatus(String message, String orderId, String statusId, String itemStatusId) {
        // checking order header status
        try {
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (!statusId.equals(orderHeader.getString("statusId"))) {
                TestCase.fail(message + " Order [" + orderId + "] status not " + statusId + ", was " + orderHeader.getString("statusId"));
            }
        } catch (GenericEntityException e) {
            TestCase.fail(message + " GenericEntityException:" + e.toString());
        }
        // checking order items status
        try {
            EntityCondition conditionList = EntityCondition.makeCondition(
                Arrays.asList(
                        EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, itemStatusId)
                        ),
                EntityOperator.AND);
            long itemCount = delegator.findCountByCondition("OrderItemShipGrpInvResAndItem", conditionList, null);
            if (itemCount > 0) {
                TestCase.fail(message + " Order [" + orderId + "] has items that are not " + itemStatusId);
            }
        } catch (GenericEntityException e) {
            TestCase.fail(message + " GenericEntityException:" + e.toString());
        }
    }

    /**
     * Asserts the given order item has the given status.
     * @param message the error message to display on failure
     * @param orderId the order id
     * @param orderItemSeqId the order item id
     * @param statusId the status the order items should have, ie: ITEM_CANCELLED
     */
    public void assertOrderItemStatus(String message, String orderId, String orderItemSeqId, String statusId) {
        // checking order item status
        try {
            GenericValue orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
            if (!statusId.equals(orderItem.getString("statusId"))) {
                TestCase.fail(message + " OrderItem [" + orderId + " / " + orderItemSeqId + "] status not " + statusId);
            }
        } catch (GenericEntityException e) {
            TestCase.fail(message + " GenericEntityException:" + e.toString());
        }
    }

    /**
     * Gets the <code>OrderItems</code> entities and <code>OrderItemShipGroupAssoc</code> and check that
     *  all items have a ship group assoc.
     * @param orderId the order id
     */
    public void assertOrderItemsHaveShipGroupAssoc(String orderId) {
        try {
            // order item -> assoc
            List<GenericValue> items = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
            assertNotEmpty("No OrderItem found for OrderItem [" + orderId + "]", items);
            for (GenericValue item : items) {
                List<GenericValue> assocs = item.getRelated("OrderItemShipGroupAssoc");
                assertNotEmpty("No OrderItemShipGroupAssoc found for OrderItem [" + orderId + "/" + item.get("orderItemSeqId") + "]", assocs);
            }
            List<GenericValue> assocs = delegator.findByAnd("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", orderId));
            Debug.logInfo("Found for order [" + orderId + "] " + items.size() + " items and " + assocs.size() + " assocs.", MODULE);
            Debug.logInfo(" items = " + items, MODULE);
            Debug.logInfo(" assocs = " + assocs, MODULE);
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException:" + e.toString());
        }

    }

    /**
     * Asserts the order first item has the given quantities.
     * @param orderId the order id
     * @param quantity the quantity
     * @param cancelQuantity the canceled quantity
     */
    public void assertOrderItemQuantity(String orderId, BigDecimal quantity, BigDecimal cancelQuantity) {
        assertOrderItemQuantity(orderId, "00001", quantity, cancelQuantity);
    }

    /**
     * Asserts the order given item has the given quantities.
     * @param orderId the order id
     * @param orderItemSeqId the order item id
     * @param quantity the quantity
     * @param cancelQuantity the canceled quantity
     */
    public void assertOrderItemQuantity(String orderId, String orderItemSeqId, BigDecimal quantity, BigDecimal cancelQuantity) {
        try {
            GenericValue orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));

            assertNotNull("OrderItem [" + orderId + " : " + orderItemSeqId + "] not found", orderItem);
            assertEquals("OrderItem [" + orderId + " : " + orderItemSeqId + "] quantity should be " + quantity, orderItem.getBigDecimal("quantity"), quantity);
            assertEquals("OrderItem [" + orderId + " : " + orderItemSeqId + "] cancelQuantity should be " + cancelQuantity, orderItem.getBigDecimal("cancelQuantity"), cancelQuantity);

        } catch (GenericEntityException e) {
            fail("Exception while getting the OrderItem");
        }
    }

    /**
     * Asserts it can update the quantities of the given order item in the given ship groups.
     * For the given order item, set the quantity in the ship groups based on the lists <code>shipGroupSeqIds</code> and <code>quantities</code> which must
     *  match in size.
     * @param userLogin a <code>GenericValue</code> value
     * @param orderId the order id
     * @param orderItemSeqId the order item id
     * @param shipGroupSeqIds a <code>List</code> of ship group sequence id
     * @param quantities a <code>List</code> of quantity
     */
    @SuppressWarnings("unchecked")
    protected void assertUpdateOrderItemSuccess(GenericValue userLogin, String orderId, String orderItemSeqId, List<String> shipGroupSeqIds, List<BigDecimal> quantities) {
        Map input = UtilMisc.toMap("userLogin", userLogin);
        input.put("orderId", orderId);

        Map itemQtyMap = new HashMap();
        int idx = 0;
        for (String shipGroupSeqId : shipGroupSeqIds) {
            String key = orderItemSeqId + ":" + shipGroupSeqId;
            itemQtyMap.put(key, quantities.get(idx).toString());
            idx++;
        }
        input.put("itemQtyMap", itemQtyMap);
        input.put("itemPriceMap", new HashMap());
        input.put("overridePriceMap", new HashMap());
        runAndAssertServiceSuccess("opentaps.updateOrderItems", input);
    }

    /**
     * Finds the picklist items and compare to the expected <code>Map</code> of productId : quantity.
     * @param picklistId the picklist to check
     * @param expected a <code>Map</code> of {productId: quantity}
     * @exception GeneralException if an error occurs
     */
    protected void assertPicklistItems(String picklistId, Map<String, BigDecimal> expected) throws GeneralException {
        List<GenericValue> picklistItems = delegator.findByCondition("PicklistItemAndOdrItmShipGrp", EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("pPicklistId", EntityOperator.EQUALS, picklistId)), EntityOperator.AND), null, UtilMisc.toList("piOrderId", "piShipGroupSeqId", "oiProductId"));

        Map<String, BigDecimal> found = new HashMap<String, BigDecimal>();
        for (GenericValue item : picklistItems) {
            BigDecimal qty = found.get(item.getString("oiProductId"));
            if (qty == null) {
                qty = BigDecimal.ZERO;
            }
            qty = qty.add(item.getBigDecimal("piQuantity"));
            found.put(item.getString("oiProductId"), qty);
        }
        assertEquals("Expected map did not match the found picklist items for picklist [" + picklistId + "]", expected, found);
    }

    /**
     * Finds the picklist items and check the corresponding items have been issued.
     * @param picklistId the picklist to check
     * @exception GeneralException if an error occurs
     */
    protected void assertPicklistItemsIssued(String picklistId) throws GeneralException {
        List<GenericValue> picklistBins = delegator.findByAnd("PicklistBin", UtilMisc.toMap("picklistId", picklistId));
        List<String> picklistBinIds = EntityUtil.getFieldListFromEntityList(picklistBins, "picklistBinId", true);
        List<GenericValue> picklistItems = delegator.findByCondition("PicklistItem", EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("picklistBinId", EntityOperator.IN, picklistBinIds)), EntityOperator.AND), null, null);

        for (GenericValue item : picklistItems) {
            BigDecimal qty = item.getBigDecimal("quantity");
            // check the related issuances
            List<GenericValue> issuedItems = item.getRelated("ItemIssuance");
            BigDecimal issuedQty = BigDecimal.ZERO;
            for (GenericValue issue : issuedItems) {
                issuedQty = issuedQty.add(issue.getBigDecimal("quantity"));
            }
            assertEquals("Picklist item [" + item + "] was not fully issued.", qty, issuedQty);
        }

    }

    /**
     * Asserts the ship groups of the order are valid and associated to the default shipping address.
     * @param orderId the order id
     * @see #assertShipGroupValidWithAddress(String, String)
     */
    protected void assertShipGroupValid(String orderId) {
        assertShipGroupValidWithAddress(orderId, shipGroupSeqId, DEFAULT_SHIPPING_ADDRESS);
    }

    /**
     * Asserts the given ship group of the order is valid and associated to the default shipping address.
     * @param orderId the order id
     * @param shipGroupSeqId the ship group id
     * @see #assertShipGroupValidWithAddress(String, String, String)
     */
    protected void assertShipGroupValid(String orderId, String shipGroupSeqId) {
        assertShipGroupValidWithAddress(orderId, shipGroupSeqId, DEFAULT_SHIPPING_ADDRESS);
    }

    /**
     * Asserts the ship groups of the order are valid and associated to the given shipping address.
     * Expect those values:
     * <ul>
     *  <li>shipmentMethodTypeId = STANDARD
     *  <li>maySplit = N
     *  <li>isGift = N
     *  <li>carrierRoleTypeId = CARRIER
     *  <li>contactMechId = the given shipping address
     * </ul>
     * @param orderId the order id
     * @param contactMechId the expected shipping address
     */
    protected void assertShipGroupValidWithAddress(String orderId, String contactMechId) {
        try {
            List<GenericValue> shipGroups = delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isEmpty(shipGroups)) {
                fail("OrderItemShipGroup for order [" + orderId + "] not found");
            }
            for (GenericValue shipGroup : shipGroups) {
                assertShipGroupValid(shipGroup, orderId, shipGroupSeqId, contactMechId);
            }
        } catch (GenericEntityException e) {
            fail("Exception while getting the OrderItemShipGroup [" + orderId + "/" + shipGroupSeqId + "]");
        }
    }

    /**
     * Asserts the given ship group of the order is valid and associated to the given shipping address.
     * Expect those values:
     * <ul>
     *  <li>shipmentMethodTypeId = STANDARD
     *  <li>maySplit = N
     *  <li>isGift = N
     *  <li>carrierRoleTypeId = CARRIER
     *  <li>contactMechId = the given shipping address
     * </ul>
     * @param orderId the order id
     * @param shipGroupSeqId the ship group id
     * @param contactMechId the expected shipping address
     */
    protected void assertShipGroupValidWithAddress(String orderId, String shipGroupSeqId, String contactMechId) {
        try {
            GenericValue shipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));
            assertShipGroupValid(shipGroup, orderId, shipGroupSeqId, contactMechId);
        } catch (GenericEntityException e) {
            fail("Exception while getting the OrderItemShipGroup [" + orderId + "/" + shipGroupSeqId + "]");
        }
    }

    private void assertShipGroupValid(GenericValue shipGroup, String orderId, String shipGroupSeqId, String contactMechId) {
        if (shipGroup == null) {
            fail("OrderItemShipGroup [" + orderId + "/" + shipGroupSeqId + "] not found");
        }
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("shipmentMethodTypeId", "STANDARD");
        expected.put("maySplit", "N");
        expected.put("isGift", "N");
        expected.put("carrierRoleTypeId", "CARRIER");
        expected.put("contactMechId", contactMechId);
        assertEquals("OrderItemShipGroup [" + orderId + "/" + shipGroupSeqId + "] changed", shipGroup, expected);

    }

    /**
     * Asserts the order can be split, with the first item (of the order) being transferred from the first ship group to the new ship group.
     *
     * @param userLogin a <code>GenericValue</code> value
     * @param orderId the order id
     * @param quantity the quantity of the item to transfer
     */
    protected void assertSplitOrderSuccess(GenericValue userLogin, String orderId, String quantity) {
        assertSplitOrderSuccess(userLogin, orderId, quantity, "00001");
    }

    /**
     * Asserts the order can be split, with the first item (of the order) being transferred from the given ship group to the new ship group.
     *
     * @param userLogin a <code>GenericValue</code> value
     * @param orderId the order id
     * @param quantity the quantity of the item to transfer
     * @param shipGroupSeqId the ship group to transfer from
     */
    protected void assertSplitOrderSuccess(GenericValue userLogin, String orderId, String quantity, String shipGroupSeqId) {
        assertSplitOrderSuccess(userLogin, orderId, quantity, shipGroupSeqId, "00001");
    }

    /**
     * Asserts the order can be split, with the given item being transferred from the given ship group to the new ship group.
     *
     * @param userLogin a <code>GenericValue</code> value
     * @param orderId the order id
     * @param quantity the quantity of the item to transfer
     * @param shipGroupSeqId the ship group to transfer from
     * @param orderItemSeqId the order item id to transfer
     */
    protected void assertSplitOrderSuccess(GenericValue userLogin, String orderId, String quantity, String shipGroupSeqId, String orderItemSeqId) {
        assertSplitOrderSuccess(userLogin, orderId, quantity, shipGroupSeqId, orderItemSeqId, DEFAULT_SHIPPING_ADDRESS);
    }

    /**
     * Asserts the order can be split, with the given item being transferred from the given ship group to the new ship group.
     *
     * @param userLogin a <code>GenericValue</code> value
     * @param orderId the order id
     * @param quantity the quantity of the item to transfer
     * @param shipGroupSeqId the ship group to transfer from
     * @param orderItemSeqId the order item id to transfer
     * @param contactMechId shipping address id, DEFAULT_SHIPPING_ADDRESS if null
     */
    protected void assertSplitOrderSuccess(GenericValue userLogin, String orderId, String quantity, String shipGroupSeqId, String orderItemSeqId, String contactMechId) {
        assertSplitOrderSuccess(userLogin, orderId, quantity, shipGroupSeqId, orderItemSeqId, contactMechId, null);
    }

    /**
     * Asserts the order can be split, with the given item being transferred from the given ship group to the new ship group.
     *
     * @param userLogin a <code>GenericValue</code> value
     * @param orderId the order id
     * @param quantity the quantity of the item to transfer
     * @param shipGroupSeqId the ship group to transfer from
     * @param orderItemSeqId the order item id to transfer
     * @param contactMechId shipping address id, DEFAULT_SHIPPING_ADDRESS if null
     * @param shippingMethod shipping method, "STANDARD@_NA_" if null
     */
    @SuppressWarnings("unchecked")
    protected void assertSplitOrderSuccess(GenericValue userLogin, String orderId, String quantity, String shipGroupSeqId, String orderItemSeqId, String contactMechId, String shippingMethod) {
        if (UtilValidate.isEmpty(contactMechId)) {
            contactMechId = DEFAULT_SHIPPING_ADDRESS;
        }

        if (UtilValidate.isEmpty(shippingMethod)) {
            shippingMethod = "STANDARD@_NA_";
        }

        Map input = UtilMisc.toMap("userLogin", userLogin);
        input.put("orderId", orderId);
        input.put("maySplit", "N");
        input.put("isGift", "N");
        input.put("shippingMethod", shippingMethod);
        input.put("contactMechId", contactMechId);
        input.put("_rowSubmit", UtilMisc.toMap("0", "Y"));
        input.put("orderIds", UtilMisc.toMap("0", orderId));
        input.put("qtiesToTransfer", UtilMisc.toMap("0", quantity));
        input.put("shipGroupSeqIds", UtilMisc.toMap("0", shipGroupSeqId));
        input.put("orderItemSeqIds", UtilMisc.toMap("0", orderItemSeqId));
        runAndAssertServiceSuccess("crmsfa.createShipGroup", input);
    }

    /**
     * Asserts the order cannot be split, while trying to transfer the first item (of the order) from the first ship group to the new ship group.
     *
     * @param userLogin a <code>GenericValue</code> value
     * @param orderId the order id
     * @param quantity the quantity of the item to transfer
     */
    protected void assertSplitOrderError(GenericValue userLogin, String orderId, String quantity) {
        assertSplitOrderError(userLogin, orderId, quantity, "00001");
    }

    /**
     * Asserts the order cannot be split, while trying to transfer the first item (of the order) from the given ship group to the new ship group.
     *
     * @param userLogin a <code>GenericValue</code> value
     * @param orderId the order id
     * @param quantity the quantity of the item to transfer
     * @param shipGroupSeqId the ship group to transfer from
     */
    protected void assertSplitOrderError(GenericValue userLogin, String orderId, String quantity, String shipGroupSeqId) {
        assertSplitOrderError(userLogin, orderId, quantity, shipGroupSeqId, "00001");
    }

    /**
     * Asserts the order cannot be split, while trying to transfer the given item from the given ship group to the new ship group.
     *
     * @param userLogin a <code>GenericValue</code> value
     * @param orderId the order id
     * @param quantity the quantity of the item to transfer
     * @param shipGroupSeqId the ship group to transfer from
     * @param orderItemSeqId the order item id to transfer
     */
    @SuppressWarnings("unchecked")
    protected void assertSplitOrderError(GenericValue userLogin, String orderId, String quantity, String shipGroupSeqId, String orderItemSeqId) {
        Map input = UtilMisc.toMap("userLogin", userLogin);
        input.put("orderId", orderId);
        input.put("maySplit", "N");
        input.put("isGift", "N");
        input.put("shippingMethod", "STANDARD@_NA_");
        input.put("contactMechId", DEFAULT_SHIPPING_ADDRESS);
        input.put("_rowSubmit", UtilMisc.toMap("0", "Y"));
        input.put("orderIds", UtilMisc.toMap("0", orderId));
        input.put("qtiesToTransfer", UtilMisc.toMap("0", quantity));
        input.put("shipGroupSeqIds", UtilMisc.toMap("0", shipGroupSeqId));
        input.put("orderItemSeqIds", UtilMisc.toMap("0", orderItemSeqId));
        runAndAssertServiceError("crmsfa.createShipGroup", input);
    }

    /**
     * Finds the <code>OrderItemShipGroupAssoc</code> for the given orderId and orderItemSeqId 00001 (first item)
     * check the given shipGroupSeqIds match.
     * @param orderId the order to check
     * @param shipGroupIds list of ship group ids to check, will check that unlisted ship groups does not exist, must be ordered by ship group seq id
     */
    protected void assertShipGroupAssocs(String orderId, List<String> shipGroupIds) {
        assertShipGroupAssocsQuantities(orderId, shipGroupIds, null, null);
    }

    /**
     * Finds the <code>OrderItemShipGroupAssoc</code> for the given orderId and orderItemSeqId 00001 (first item)
     * check the given shipGroupIds and quantities match, and that the number of ship groups match those list size.
     * @param orderId the order to check
     * @param shipGroupIds list of ship group ids to check, will check that unlisted ship groups does not exist, must be ordered by ship group seq id
     * @param quantities list of quantities to check ordered by ship group id
     */
    protected void assertShipGroupAssocsQuantities(String orderId, List<String> shipGroupIds, List<BigDecimal> quantities) {
        assertShipGroupAssocsQuantities(orderId, shipGroupIds, quantities, null);
    }

    /**
     * Finds the <code>OrderItemShipGroupAssoc</code> for the given orderId and orderItemSeqId 00001 (first item)
     * check the given quantities and cancelQuantities match, and that the number of ship groups match those list size.
     * @param orderId the order to check
     * @param shipGroupIds list of ship group ids to check, will check that unlisted ship groups does not exist, must be ordered by ship group seq id
     * @param quantities list of quantities to check ordered by ship group id
     * @param cancelQuantities list of canceled quantities to check ordered by ship group id
     */
    protected void assertShipGroupAssocsQuantities(String orderId, List<String> shipGroupIds, List<BigDecimal> quantities, List<BigDecimal> cancelQuantities) {
        try {
            List<GenericValue> shipGroups = delegator.findByAnd("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "00001"), UtilMisc.toList("shipGroupSeqId"));

            int numberOfShipGroups = shipGroupIds.size();
            if (numberOfShipGroups > 0) {
                assertNotNull("Order [" + orderId + "] should have " + numberOfShipGroups + " ship groups", shipGroups);
                assertEquals("Order [" + orderId + "] should have " + numberOfShipGroups + " ship groups", numberOfShipGroups, shipGroups.size());
            } else {
                assertNull("Order [" + orderId + "] should not have any ship group", shipGroups);
            }


            int idx = 0;
            for (GenericValue shipGroup : shipGroups) {
                String shipGroupSeqId = shipGroupIds.get(idx);
                assertEquals("Ship group [" + shipGroupSeqId + "] shipGroupSeqId for order [" + orderId + "] is wrong", shipGroup.getString("shipGroupSeqId"), shipGroupIds.get(idx));
                if (quantities != null) {
                    assertEquals("Ship group [" + shipGroupSeqId + "] quantity for order [" + orderId + "] is wrong", shipGroup.getBigDecimal("quantity"), quantities.get(idx));
                }
                if (cancelQuantities != null) {
                    BigDecimal qty = shipGroup.getBigDecimal("cancelQuantity");
                    if (qty == null) {
                        qty = BigDecimal.ZERO;
                    }
                    assertEquals("Ship group [" + shipGroupSeqId + "] canceled quantity for order [" + orderId + "] is wrong", qty, cancelQuantities.get(idx));
                }
                idx++;
            }
        } catch (GenericEntityException e) {
            fail("Exception while getting the OrderItemShipGroupAssoc");
        }
    }

    /**
     * Finds the <code>OrderItemShipGrpInvRes</code> for the given orderId, facilityId and expect to find none.
     * @param orderId the order to check
     * @param facilityId the facility where the reservations should be in
     * @exception GeneralException if an error occurs
     */
    protected void assertNoShipGroupReservations(String orderId, String facilityId) throws GeneralException {
        assertShipGroupReservations(orderId, facilityId, null);
    }

    /**
     * Finds the <code>OrderItemShipGrpInvRes</code> for the given orderId, facilityId and expected <code>Map</code> of reservations.
     * @param orderId the order to check
     * @param facilityId the facility where the reservations should be in
     * @param expected a <code>Map</code> of {shipGroupSeqId: {productId: quantity}}, or <code>null</code> to check that no reservation exists
     * @exception GeneralException if an error occurs
     */
    protected void assertShipGroupReservations(String orderId, String facilityId, Map<String, Map<String, BigDecimal>> expected) throws GeneralException {
        // find the reservations
        List<GenericValue> reservations = delegator.findByAnd("OrderItemShipGrpInvResAndItem", UtilMisc.toMap("orderId", orderId, "facilityId", facilityId));
        // build a Map of {shipGroupSeqId: {productId: quantity}}
        Map<String, Map<String, BigDecimal>> found = new HashMap<String, Map<String, BigDecimal>>();
        for (GenericValue item : reservations) {
            Map<String, BigDecimal> productMap = found.get(item.getString("shipGroupSeqId"));
            if (productMap == null) {
                productMap = new HashMap<String, BigDecimal>();
                found.put(item.getString("shipGroupSeqId"), productMap);
            }

            BigDecimal qty = productMap.get(item.getString("productId"));
            if (qty == null) {
                qty = BigDecimal.ZERO;
            }
            BigDecimal qtyRes = item.getBigDecimal("quantity");
            BigDecimal qtyNaRes = item.getBigDecimal("quantityNotAvailable");
            if (qtyNaRes == null) {
                qtyNaRes = BigDecimal.ZERO;
            }
            if (qtyNaRes.compareTo(qtyRes) > 0) {
                //fail("Found quantityNotAvailable > quantity in reservation: " + item);
            }

            qty = qty.add(qtyRes);
            productMap.put(item.getString("productId"), qty);
        }

        if (expected != null) {
            // check the found quantities match the expected quantities
            assertEquals("Expected map did not match the found OrderItemShipGrpInvResAndItem for order [" + orderId + "] and facility [" + facilityId + "].", expected, found, false);
        } else {
            // if expected is null, check that there was no reservation found
            assertTrue("Expected no reservations for order [" + orderId + "] and facility [" + facilityId + "], but found: " + found, found.keySet().isEmpty());
        }
    }

    /**
     * Finds the <code>OrderItemShipGrpInvRes</code> for the given orderId, facilityId and expected <code>Map</code> of reservations with quantities reserved and quantities not available.
     * @param orderId the order to check
     * @param facilityId the facility where the reservations should be in
     * @param expected a <code>Map</code> of {shipGroupSeqId: {productId: [quantity, quantityNotAvailable]}}, or <code>null</code> to check that no reservation exists
     * @exception GeneralException if an error occurs
     */
    protected void assertShipGroupReservationsAndQuantities(String orderId, String facilityId, Map<String, Map<String, List<BigDecimal>>> expected) throws GeneralException {
        // find the reservations
        List<GenericValue> reservations = delegator.findByAnd("OrderItemShipGrpInvResAndItem", UtilMisc.toMap("orderId", orderId, "facilityId", facilityId));
        // build a Map of {shipGroupSeqId: {productId: quantity}}
        Map<String, Map<String, List<BigDecimal>>> found = new HashMap<String, Map<String, List<BigDecimal>>>();
        for (GenericValue item : reservations) {
            Map<String, List<BigDecimal>> productMap = found.get(item.getString("shipGroupSeqId"));
            if (productMap == null) {
                productMap = new HashMap<String, List<BigDecimal>>();
                found.put(item.getString("shipGroupSeqId"), productMap);
            }

            List<BigDecimal> qties = productMap.get(item.getString("productId"));
            if (qties == null) {
                qties = Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO);
                productMap.put(item.getString("productId"), qties);
            }
            BigDecimal qty = qties.get(0);
            BigDecimal qtyNa = qties.get(1);
            BigDecimal qtyRes = item.getBigDecimal("quantity");
            BigDecimal qtyNaRes = item.getBigDecimal("quantityNotAvailable");
            if (qtyNaRes == null) {
                qtyNaRes = BigDecimal.ZERO;
            }
            if (qtyNaRes.compareTo(qtyRes) > 0) {
                //fail("Found quantityNotAvailable > quantity in reservation: " + item);
            }
            qty = qty.add(qtyRes);
            qtyNa = qtyNa.add(qtyNaRes);
            productMap.put(item.getString("productId"), Arrays.asList(qty, qtyNa));
        }

        if (expected != null) {
            // check the found quantities match the expected quantities
            assertEquals("Expected map did not match the found OrderItemShipGrpInvResAndItem for order [" + orderId + "] and facility [" + facilityId + "].", expected, found, false);
        } else {
            // if expected is null, check that there was no reservation found
            assertTrue("Expected no reservations for order [" + orderId + "] and facility [" + facilityId + "], but found: " + found, found.keySet().isEmpty());
        }
    }

    /**
     * Finds the <code>OrderItemShipGrpInvRes</code> for the given orderId and orderItemSeqId 00001 (first item)
     * check the given shipGroupSeqIds match.
     * @param orderId the order to check
     * @param shipGroupIds list of ship group ids to check, will check that unlisted ship groups does not exist, must be ordered by ship group seq id
     */
    protected void assertShipGroupReservations(String orderId, List<String> shipGroupIds) {
        assertShipGroupReservationsQuantities(orderId, shipGroupIds, null, null);
    }

    /**
     * Finds the <code>OrderItemShipGrpInvRes</code> for the given orderId and orderItemSeqId 00001 (first item)
     * check the given shipGroupIds and quantities match, and that the number of ship groups match those list size.
     * @param orderId the order to check
     * @param shipGroupIds list of ship group ids to check, will check that unlisted ship groups does not exist, must be ordered by ship group seq id
     * @param quantities list of quantities to check ordered by ship group id
     */
    protected void assertShipGroupReservationsQuantities(String orderId, List<String> shipGroupIds, List<BigDecimal> quantities) {
        assertShipGroupReservationsQuantities(orderId, shipGroupIds, quantities, null);
    }

    /**
     * Finds the <code>OrderItemShipGrpInvRes</code> for the given orderId and orderItemSeqId 00001 (first item)
     * check the given quantities and cancelQuantities match, and that the number of ship groups match those list size.
     * @param orderId the order to check
     * @param shipGroupIds list of ship group ids to check, will check that unlisted ship groups does not exist, must be ordered by ship group seq id
     * @param quantities list of quantities to check ordered by ship group id
     * @param quantitiesNotAvailable list of quantities not available to check ordered by ship group id
     */
    protected void assertShipGroupReservationsQuantities(String orderId, List<String> shipGroupIds, List<BigDecimal> quantities, List<BigDecimal> quantitiesNotAvailable) {
        try {
            List<GenericValue> shipGroups = delegator.findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "00001"), UtilMisc.toList("shipGroupSeqId"));

            int numberOfShipGroups = shipGroupIds.size();
            if (numberOfShipGroups > 0) {
                assertNotNull("Order [" + orderId + "] should have " + numberOfShipGroups + " ship groups", shipGroups);
                assertEquals("Order [" + orderId + "] should have " + numberOfShipGroups + " ship groups", numberOfShipGroups, shipGroups.size());
            } else {
                assertNull("Order [" + orderId + "] should not have any ship group", shipGroups);
            }


            int idx = 0;
            for (GenericValue shipGroup : shipGroups) {
                String shipGroupSeqId = shipGroupIds.get(idx);
                assertEquals("Ship group [" + shipGroupSeqId + "] shipGroupSeqId for order [" + orderId + "] is wrong", shipGroup.getString("shipGroupSeqId"), shipGroupIds.get(idx));
                if (quantities != null) {
                    assertEquals("Ship group [" + shipGroupSeqId + "] quantity for order [" + orderId + "] is wrong", shipGroup.getBigDecimal("quantity"), quantities.get(idx));
                }
                if (quantitiesNotAvailable != null) {
                    BigDecimal qty = shipGroup.getBigDecimal("quantityNotAvailable");
                    if (qty == null) {
                        qty = BigDecimal.ZERO;
                    }
                    assertEquals("Ship group [" + shipGroupSeqId + "] quantity not available for order [" + orderId + "] is wrong", qty, quantitiesNotAvailable.get(idx));
                }
                idx++;
            }
        } catch (GenericEntityException e) {
            fail("Exception while getting the OrderItemShipGrpInvRes");
        }
    }

    /**
     * Asserts the given order contains exactly the given <code>Map</code> of product: quantity of non items for the given promo flag value.
     * @param orderId the order to check
     * @param items a <code>Map</code> of productId: quantity
     * @param isPromo the promotion flag, ie: "Y", "N"
     */
    protected void assertOrderItems(String orderId, Map<String, BigDecimal> items, String isPromo) {
        try {
            // get the OrderItems
            List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId, "isPromo", isPromo));
            Map<String, BigDecimal> found = new HashMap<String, BigDecimal>();
            // set found quantities from the expected items to 0
            for (String productId : items.keySet()) {
                found.put(productId, BigDecimal.ZERO);
            }

            for (GenericValue item : orderItems) {
                BigDecimal foundQty = found.get(item.getString("productId"));
                if (foundQty == null) {
                    foundQty = BigDecimal.ZERO;
                }
                BigDecimal qty = item.getBigDecimal("quantity");
                if (qty == null) {
                    qty = BigDecimal.ZERO;
                }
                BigDecimal cancelQuantity = item.getBigDecimal("cancelQuantity");
                if (cancelQuantity == null) {
                    cancelQuantity = BigDecimal.ZERO;
                }

                foundQty = foundQty.add(qty).subtract(cancelQuantity);
                found.put(item.getString("productId"), foundQty);
            }

            Set<String> ids = new HashSet<String>();
            ids.addAll(items.keySet());
            ids.addAll(found.keySet());
            for (String productId : ids) {
                BigDecimal expectedQty = items.get(productId);
                if (expectedQty == null) {
                    expectedQty = BigDecimal.ZERO;
                }

                BigDecimal foundQty = found.get(productId);
                assertEquals("Order Items quantity did not match for order [" + orderId + "] product [" + productId + "]", foundQty, expectedQty);
            }
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException when trying to find order item: " + e.toString());
        }
    }

    /**
     * Asserts the given order contains exactly the given <code>Map</code> of product: quantity of non promotion items for the given item status.
     * @param orderId the order to check
     * @param items a <code>Map</code> of productId: quantity
     */
    protected void assertNormalOrderItems(String orderId, Map<String, BigDecimal> items) {
        assertOrderItems(orderId, items, "N");
    }

    /**
     * Asserts the given order contains exactly the given <code>Map</code> of product: quantity of promotion items for the given item status.
     * @param orderId the order to check
     * @param items a <code>Map</code> of productId: quantity
     */
    protected void assertPromoOrderItems(String orderId, Map<String, BigDecimal> items) {
        assertOrderItems(orderId, items, "Y");
    }

    /**
     * Asserts that one and only one <code>OrderItem</code> matching the given parameters exists.
     * If the product is a virtual product, then make sure that at least one of its variants is in the order, and return
     * the first order item which is a variant.
     * @param orderId the order id
     * @param productId the product id
     * @param quantity the quantity
     * @param isPromo the promotion flag, ie: "Y", "N"
     * @param itemStatus the item status id
     * @return the order item is if found (orderItemSeqId)
     */
    protected String assertOrderItemExists(String orderId, String productId, BigDecimal quantity, String isPromo, String itemStatus) {
        try {

            List<GenericValue> orderItems = null;

            // basic conditions for all queries
            List<EntityCondition> orderItemConditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                                                                                         EntityCondition.makeCondition("quantity", EntityOperator.EQUALS, quantity),
                                                                                         EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, itemStatus),
                                                                                         EntityCondition.makeCondition("isPromo", EntityOperator.EQUALS, isPromo));

            // check if the product is virtual
            GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            assertNotNull("Product [" + productId + "] not found", product);

            if ("Y".equals(product.getString("isVirtual"))) {
                // if so, then look for any order item which is a variant
                List<GenericValue> relatedProducts = product.getRelatedByAndCache("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"));
                if (UtilValidate.isNotEmpty(relatedProducts)) {
                    // note that we can also filter by fromDate/thruDate, but it's not necessary, since even if the product is no longer a variant,
                    // it might have been a variant at order time
                    List<String> relatedProductIds = EntityUtil.getFieldListFromEntityList(relatedProducts, "productIdTo", true);
                    assertNotNull("No variants productIds found for virtual product [" + productId + "]", relatedProductIds);
                    orderItemConditions.add(EntityCondition.makeCondition("productId", EntityOperator.IN, relatedProductIds));
                }
            } else {
                // if not, just look for the product
                orderItemConditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
            }

            Debug.logInfo("About to find order items for condition [" + orderItemConditions + "]", MODULE);
            orderItems = delegator.findByAnd("OrderItem", orderItemConditions);

            // get the OrderItems
            assertEquals("OrderItem not found or more than one found for productId [" + productId + "] in order [" + orderId + "] quantity=" + quantity + " isPromo=" + isPromo + " statusId=" + itemStatus, 1, orderItems.size());
            GenericValue orderItem = orderItems.get(0);
            return orderItem.getString("orderItemSeqId");
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException when trying to find promo item: " + e.toString());
        }
        return null;
    }

    /**
     * Asserts that one and only one corresponding <code>OrderItem</code> exist as Promo with matching parameters,
     * asserts that a <code>OrderItemShipGroupAssoc</code> exist for it and
     * asserts that a <code>OrderAdjustment</code> exist with the proper <code>productPromoId</code>.
     *
     * @param orderId the order id
     * @param productId the product id
     * @param quantity the quantity
     * @param productPromoId the product promo id
     * @param itemStatus the item status id
     */
    protected void assertPromoItemExists(String orderId, String productId, BigDecimal quantity, String productPromoId, String itemStatus) {
        try {
            // get the OrderItem as promo item;
            String orderItemSeqId = assertOrderItemExists(orderId, productId, quantity, "Y", itemStatus);
            // check that the item is associated to a shipping group
            List<GenericValue> orderItemShipGroupAssocs = delegator.findByAnd("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
            assertTrue("Promo OrderItem not associated to any shipping group orderItemSeqId [" + orderItemSeqId + "] in order [" + orderId + "]", orderItemShipGroupAssocs.size() > 0);
            // check that an OrderAdjustment was created for the expected productPromoId
            List<GenericValue> orderAdjustments = delegator.findByAnd("OrderAdjustment", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "orderAdjustmentTypeId", "PROMOTION_ADJUSTMENT", "productPromoId", productPromoId));
            assertTrue("Promo OrderItem not associated to any OrderAdjustment for orderItemSeqId [" + orderItemSeqId + "] and productPromoId [" + productPromoId + "] in order [" + orderId + "]", orderAdjustments.size() > 0);
        } catch (GenericEntityException e) {
            TestCase.fail("GenericEntityException when trying to find promo item: " + e.toString());
        }
    }

    /**
     * This method use to assert the given order is not ready to ship in the given facility.
     * @param order the <code>Order</code> value to check
     * @param facilityId the facility where the order should be ready to ship
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected void assertOrderNotReadyToShip(Order order, String facilityId) throws GeneralException {
        // - shippinghelper.findAllOrdersReadyToShip() => should contain the corresponding OdrItShpGrpHdrInvResAndInvItem entities
        ShippingHelper shippinghelper  = new ShippingHelper(delegator, facilityId);
        List orderIsReadyConditionList = shippinghelper.getOrderIsReadyConditionList();
        // empty list means no order ready to ship, which is good too here
        if (orderIsReadyConditionList.size() == 0) {
            return;
        }
        EntityCondition orderIsReadyConditions = EntityCondition.makeCondition(orderIsReadyConditionList, EntityOperator.OR);
        //  result in any duplicate rows
        List<String> fieldsToSelect = Arrays.asList("orderId", "shipGroupSeqId", "productId", "quantity", "facilityId", "orderDate");
        List<String> orderBy = Arrays.asList("orderId", "shipGroupSeqId", "productId", "quantity", "facilityId", "orderDate");
        List<GenericValue> odrItShpGrpHdrInvResAndInvItems = delegator.findByCondition("OdrItShpGrpHdrInvResAndInvItem", orderIsReadyConditions, null, fieldsToSelect, orderBy, UtilCommon.DISTINCT_READ_OPTIONS);
        // only consider OdrItShpGrpHdrInvResAndInvItem with same order id and facility as the order we are testing for
        odrItShpGrpHdrInvResAndInvItems = EntityUtil.filterByAnd(odrItShpGrpHdrInvResAndInvItems, UtilMisc.toMap("orderId", order.getOrderId(), "facilityId", facilityId));
        assertEmpty("Expected not to find any OdrItShpGrpHdrInvResAndInvItem for order [" + order.getOrderId() + "] and facility [" + facilityId + "], but found : " + odrItShpGrpHdrInvResAndInvItems, odrItShpGrpHdrInvResAndInvItems);
    }

    /**
     * This method use to assert the given order is ready to ship in the given facility.
     * @param order the <code>Order</code> value to check
     * @param facilityId the facility where the order should be ready to ship
     * @exception GeneralException if an error occurs
     */
    protected void assertOrderReadyToShip(Order order, String facilityId) throws GeneralException {
        assertOrderReadyToShip(order, facilityId, null);
    }

    /**
     * This method use to assert the given order is ready to ship in the given facility and the found items match the expected <code>Map</code>.
     * @param order the <code>Order</code> value to check
     * @param facilityId the facility where the order should be ready to ship
     * @param expected a <code>Map</code> of {shipGroupSeqId: {productId: quantity}}, or <code>null</code> if there is no need to check the quantities
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected void assertOrderReadyToShip(Order order, String facilityId, Map<String, Map<String, BigDecimal>> expected) throws GeneralException {
        // - shippinghelper.findAllOrdersReadyToShip() => should contain the corresponding OdrItShpGrpHdrInvResAndInvItem entities
        ShippingHelper shippinghelper  = new ShippingHelper(delegator, facilityId);
        List orderIsReadyConditionList = shippinghelper.getOrderIsReadyConditionList();
        // empty list means no order ready to ship, which is not good here
        if (orderIsReadyConditionList.size() == 0) {
            fail("Did not find any OdrItShpGrpHdrInvResAndInvItem at all, was looking at order [" + order.getOrderId() + "] and facility [" + facilityId + "].");
        }
        EntityCondition orderIsReadyConditions = EntityCondition.makeCondition(orderIsReadyConditionList, EntityOperator.OR);
        //  result in any duplicate rows
        List<String> fieldsToSelect = Arrays.asList("orderId", "shipGroupSeqId", "productId", "quantity", "facilityId", "orderDate");
        List<String> orderBy = Arrays.asList("orderId", "shipGroupSeqId", "productId", "quantity", "facilityId", "orderDate");
        List<GenericValue> odrItShpGrpHdrInvResAndInvItems = delegator.findByCondition("OdrItShpGrpHdrInvResAndInvItem", orderIsReadyConditions, null, fieldsToSelect, orderBy, UtilCommon.DISTINCT_READ_OPTIONS);
        // only consider OdrItShpGrpHdrInvResAndInvItem with same order id and facility as the order we are testing for
        odrItShpGrpHdrInvResAndInvItems = EntityUtil.filterByAnd(odrItShpGrpHdrInvResAndInvItems, UtilMisc.toMap("orderId", order.getOrderId(), "facilityId", facilityId));
        Debug.logInfo("assertOrderReadyToShip: found " + odrItShpGrpHdrInvResAndInvItems, MODULE);
        if (expected != null) {
            // build a Map of {shipGroupSeqId: {productId: quantity}}
            Map<String, Map<String, BigDecimal>> found = new HashMap<String, Map<String, BigDecimal>>();
            for (GenericValue item : odrItShpGrpHdrInvResAndInvItems) {
                Map<String, BigDecimal> productMap = found.get(item.getString("shipGroupSeqId"));
                if (productMap == null) {
                    productMap = new HashMap<String, BigDecimal>();
                    found.put(item.getString("shipGroupSeqId"), productMap);
                }

                BigDecimal qty = productMap.get(item.getString("productId"));
                if (qty == null) {
                    qty = new BigDecimal(0.0);
                }
                qty = qty.add(item.getBigDecimal("quantity"));
                productMap.put(item.getString("productId"), qty);
            }
            // check the found quantities match the expected quantities
            assertEquals("Expected map did not match the found OdrItShpGrpHdrInvResAndInvItem for order [" + order.getOrderId() + "] and facility [" + facilityId + "].", expected, found);
        } else {
            assertNotEmpty("Expected to find OdrItShpGrpHdrInvResAndInvItem for order [" + order.getOrderId() + "] and facility [" + facilityId + "].", odrItShpGrpHdrInvResAndInvItems);
        }
    }

    /**
     * Checks that Sales Taxes have been created for the given order and tax authority.
     * <ul>
     *  <li>check that the correct number of order adjustments is present
     *  <li>check that the total of these order adjustments is correct
     * </ul>
     * @param orderId the order id
     * @param taxAuthPartyId the tax authority
     * @param expectedOrderAdjustmentsNumber the number of order adjustment to expect
     * @param expectedTaxTotal the total of tax to expect
     * @return total amount of sales tax
     */
    protected BigDecimal checkSalesTax(String orderId, String taxAuthPartyId, int expectedOrderAdjustmentsNumber, BigDecimal expectedTaxTotal) {
        return checkSalesTax(orderId, taxAuthPartyId, null, expectedOrderAdjustmentsNumber, expectedTaxTotal);
    }

    /**
     * Checks that Sales Taxes have been created for the given order and tax authority.
     * <ul>
     *  <li>check that the correct number of order adjustments is present
     *  <li>check that the total of these order adjustments is correct
     * </ul>
     * @param orderId the order id
     * @param taxAuthPartyId the tax authority
     * @param orderItemSeqId optional
     * @param expectedOrderAdjustmentsNumber the number of order adjustment to expect
     * @param expectedTaxToal the total of tax to expect
     * @return total amount of sales tax
     */
    protected BigDecimal checkSalesTax(String orderId, String taxAuthPartyId, String orderItemSeqId, int expectedOrderAdjustmentsNumber, BigDecimal expectedTaxToal) {
        BigDecimal taxTotal = ZERO_BASE;
        List<OrderAdjustment> salesTaxes = new ArrayList<OrderAdjustment>();

        // get the order adjustments
        try {
            OrderRepositoryInterface orderRepository = getOrderRepository(User);
            Order order = orderRepository.getOrderById(orderId);

            if (UtilValidate.isNotEmpty(orderItemSeqId)) {
                for (OrderAdjustment oa : order.getOrderAdjustments()) {
                    if (oa.isSalesTax() && taxAuthPartyId.equals(oa.getTaxAuthPartyId()) && orderItemSeqId.equals(oa.getOrderItemSeqId())) {
                        salesTaxes.add(oa);
                    }
                }
            } else {
                for (OrderAdjustment oa : order.getOrderAdjustments()) {
                    if (oa.isSalesTax() && taxAuthPartyId.equals(oa.getTaxAuthPartyId())) {
                        salesTaxes.add(oa);
                    }
                }
            }

            assertEquals("There should be " + expectedOrderAdjustmentsNumber + " Sales Tax adjustement from " + taxAuthPartyId + " for order [" + orderId + "]", expectedOrderAdjustmentsNumber, salesTaxes.size());

            for (OrderAdjustment oa : salesTaxes) {
                taxTotal = taxTotal.add(oa.getAmount());
            }
        } catch (FoundationException e) {
            assertTrue("FoundationException:" + e.toString(), false);
            return null;
        }

        // checking the total tax amount
        assertEquals("Amount of tax", taxTotal, expectedTaxToal);
        return taxTotal;
    }


    /**
     * Convenience method to update autoApproveInvoice of product store.
     * @param productStoreId the product store id
     * @param autoApproveInvoice the new auto approve invoice flag
     * @param delegator a <code>Delegator</code> value
     * @throws GeneralException if an error occurs
     */
    protected void setProductStoreAutoApproveInvoice(String productStoreId, String autoApproveInvoice, Delegator delegator) throws GeneralException {
        GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
        productStore.set("autoApproveInvoice", autoApproveInvoice);
        productStore.store();
    }

    /**
     * Convenience method to update reserveOrderEnumId of product store.
     * @param productStoreId the product store id
     * @param reserveOrderEnumId the new reserveOrderEnumId
     * @param delegator a <code>Delegator</code> value
     * @throws GeneralException if an error occurs
     */
    protected void setProductStoreInventoryReservationEnum(String productStoreId, String reserveOrderEnumId, Delegator delegator) throws GeneralException {
        GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
        productStore.set("reserveOrderEnumId", reserveOrderEnumId);
        productStore.store();
    }

    /**
     * Convenience method to update inventoryReserveOrderEnumId of facility.
     * @param facilityId the facility id
     * @param reserveOrderEnumId the new reserveOrderEnumId
     * @param delegator a <code>Delegator</code> value
     * @throws GeneralException if an error occurs
     */
    protected void setFacilityInventoryReservationEnum(String facilityId, String reserveOrderEnumId, Delegator delegator) throws GeneralException {
        GenericValue facility = delegator.findByPrimaryKey("Facility", UtilMisc.toMap("facilityId", facilityId));
        facility.set("inventoryReserveOrderEnumId", reserveOrderEnumId);
        facility.store();
    }

    /**
     * Perform order item re-reservation.
     *
     * @param orderId the order to re-reserve an item for
     * @param orderItemSeqId the order item ID in the order
     * @param inventoryItemId the inventory item ID to re-reserve from
     * @param shipGroupSeqId the shipGroupSeqId
     * @param facilityId the facility where to re-reserve the order item
     * @param quantity the quantity to re-reserve
     * @exception GeneralException if an error occurs
     */
    protected void reReserveOrderItemInventory(String orderId, String orderItemSeqId, String inventoryItemId, String shipGroupSeqId, String facilityId, BigDecimal quantity) throws GeneralException {
        Map<String, Object> context = FastMap.newInstance();
        context.put("userLogin", delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1")));
        context.put("orderId", orderId);
        context.put("orderItemSeqId", orderItemSeqId);
        context.put("inventoryItemId", inventoryItemId);
        context.put("shipGroupSeqId", shipGroupSeqId);
        context.put("facilityId", facilityId);
        context.put("quantity", quantity);
        runAndAssertServiceSuccess("reReserveProductInventory", context);
    }
}
