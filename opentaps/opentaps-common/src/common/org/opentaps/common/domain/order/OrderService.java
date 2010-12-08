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
package org.opentaps.common.domain.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.opentaps.base.constants.ContactMechPurposeTypeConstants;
import org.opentaps.base.constants.OrderAdjustmentTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.OrderAdjustmentBilling;
import org.opentaps.base.entities.OrderItemShipGroupAssoc;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.services.CalcTaxService;
import org.opentaps.base.services.CancelOrderItemNoActionsService;
import org.opentaps.base.services.CreateOrderAdjustmentService;
import org.opentaps.base.services.CreateOrderNoteService;
import org.opentaps.base.services.LoadCartFromOrderService;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.billing.payment.PaymentMethod;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderAdjustment;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderItemShipGroup;
import org.opentaps.domain.order.OrderPaymentPreference;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.order.OrderServiceInterface;
import org.opentaps.domain.order.OrderSpecificationInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceException;

/**
 * Order related services.
 */
public class OrderService extends DomainService implements OrderServiceInterface {

    private static final String MODULE = OrderService.class.getName();

    protected String orderId;
    protected String noteText;
    protected String newOrderContactMechId;
    // parameter passed by the seca when changing a ship group
    protected String contactMechId;

    /** {@inheritDoc} */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public void recreateOrderAdjustments() throws ServiceException {
        try {
            OrderDomainInterface domain = getDomainsDirectory().getOrderDomain();
            OrderRepositoryInterface repository = domain.getOrderRepository();

            // check permission
            if (!hasEntityPermission("ORDERMGR", "_UPDATE")) {
                throw new ServiceException("OpentapsError_PermissionDenied");
            }

            // get the order
            Order order = repository.getOrderById(orderId);

            // check if the order was billed
            boolean orderIsBilled = !order.getOrderItemBillings().isEmpty();

            // cancel all promotion items if the order was not billed only
            // else we do not recreate promotion items, but make a list of promo items id
            // so we can re associate them to the cart items
            Set<String> existingPromoItems = new HashSet<String>();
            for (OrderItem item : order.getPromotionItems()) {
                if (!item.isCancelled()) {
                    if (!orderIsBilled) {
                        CancelOrderItemNoActionsService service = new CancelOrderItemNoActionsService();
                        service.setInOrderId(orderId);
                        service.setInOrderItemSeqId(item.getOrderItemSeqId());
                        runSync(service);
                    } else {
                        Debug.logInfo("Found existingPromoItem: " + item, MODULE);
                        existingPromoItems.add(item.getOrderItemSeqId());
                    }
                }
            }

            // remove all adjustments related to promotions that are not billed yet
            List<OrderAdjustment> promoAdjustments = repository.findList(OrderAdjustment.class, Arrays.asList(
                EntityCondition.makeCondition(OrderAdjustment.Fields.orderId.name(), EntityOperator.EQUALS, orderId),
                EntityCondition.makeCondition(OrderAdjustment.Fields.orderAdjustmentTypeId.name(), EntityOperator.EQUALS, OrderAdjustmentTypeConstants.PROMOTION_ADJUSTMENT),
                EntityCondition.makeCondition(OrderAdjustment.Fields.productPromoId.name(), EntityOperator.NOT_EQUAL, null)));
            List<OrderAdjustment> promoAdjustmentsToRemove = new ArrayList();

            // account the amount removed from each order item and the order
            // promoId => amount
            Map<String, BigDecimal> orderAdjustmentExistingAmounts = new HashMap<String, BigDecimal>();
            Map<String, BigDecimal> orderAdjustmentNewAmounts = new HashMap<String, BigDecimal>();
            // itemId => promoId => amount
            Map<String, Map<String, BigDecimal>> orderItemAdjustmentExistingAmounts = new HashMap<String, Map<String, BigDecimal>>();
            Map<String, Map<String, BigDecimal>> orderItemAdjustmentNewAmounts = new HashMap<String, Map<String, BigDecimal>>();

            for (OrderAdjustment adj : promoAdjustments) {
                // for order adjustments, never remove if the order is billed
                // for item adjustments, only remove if the item and its adjustments are not billed
                if (adj.getOrderItem() != null) {
                    if (adj.getOrderAdjustmentBillings().isEmpty() && adj.getOrderItem().getOrderItemBillings().isEmpty()) {
                        // not billed item, adjustment is safe to remove
                        promoAdjustmentsToRemove.add(adj);
                    } else {
                        // billed item, sum it in orderItemAdjustmentExistingAmounts
                        sumAdjustmentInPerItemPerPromoAmountMap(orderItemAdjustmentExistingAmounts, adj);
                    }
                } else {
                    // order adjustment
                    if (!orderIsBilled) {
                        // order is not billed, safe to remove
                        promoAdjustmentsToRemove.add(adj);
                    } else {
                        // billed order, sum it in orderAdjustmentExistingAmount
                        sumAdjustmentInPerPromoAmountMap(orderAdjustmentExistingAmounts, adj);
                    }
                }
            }
            repository.remove(promoAdjustmentsToRemove);

            // use the loadCartFromOrder service to re calculate the promotions for the order
            LoadCartFromOrderService service = new LoadCartFromOrderService();
            service.setInOrderId(orderId);
            service.setInSkipInventoryChecks(true);
            service.setInSkipProductChecks(true);
            runSync(service);
            ShoppingCart cart = service.getOutShoppingCart();

            // to match only one cart item to one existing promo item
            Set<String> existingPromoItemsPotentials = new HashSet<String>(existingPromoItems);

            // only recreate promotions items if the order was not billed
            Set<String> newPromoItems = new HashSet<String>();
            for (ShoppingCartItem item : (List<ShoppingCartItem>) cart.items()) {
                // skip existing items
                if (UtilValidate.isNotEmpty(item.getOrderItemSeqId())) {
                    continue;
                }
                // if the order is billed we still mark the new items so we can spot their related adjustments
                if (orderIsBilled) {
                    // try to find an equivalent item in the order
                    // because the cart do not load existing promo items
                    String equivalentItemSeqId = null;
                    Debug.logInfo("Looking for equivalentItem in [" + existingPromoItemsPotentials + "] with product [" + item.getProductId() + "], quantity = " + item.getQuantity() + ", unitPrice = " + item.getBasePrice() + ", listPrice = " + item.getListPrice(), MODULE);
                    List<OrderItem> equivalentItems = repository.findList(OrderItem.class, Arrays.asList(
                               EntityCondition.makeCondition(OrderItem.Fields.orderId.name(), EntityOperator.EQUALS, orderId),
                               EntityCondition.makeCondition(OrderItem.Fields.orderItemSeqId.name(), EntityOperator.IN, existingPromoItemsPotentials),
                               EntityCondition.makeCondition(OrderItem.Fields.productId.name(), EntityOperator.EQUALS, item.getProductId()),
                               EntityCondition.makeCondition(OrderItem.Fields.isPromo.name(), EntityOperator.EQUALS, "Y"),
                               EntityCondition.makeCondition(OrderItem.Fields.isModifiedPrice.name(), EntityOperator.EQUALS, "N"),
                               EntityCondition.makeCondition(OrderItem.Fields.quantity.name(), EntityOperator.EQUALS, item.getQuantity()),
                               EntityCondition.makeCondition(OrderItem.Fields.unitPrice.name(), EntityOperator.EQUALS, item.getBasePrice()),
                               EntityCondition.makeCondition(OrderItem.Fields.unitListPrice.name(), EntityOperator.EQUALS, item.getListPrice())
                              ));
                    if (equivalentItems.isEmpty()) {
                        // no equivalent item found, could be a new promotion, not adding
                        Debug.logWarning("Not adding new promo item of " + item.getQuantity() + " x [" + item.getProductId() + "], because the order is already billed and no equivalent item was found in the order.", MODULE);
                        item.setOrderItemSeqId("__BILLED__");
                        continue;
                    } else if (equivalentItems.size() == 1) {
                        // perfect match, setting the corresponding orderItemSeqId to the cart and remove it from the list of candidates
                        equivalentItemSeqId = equivalentItems.get(0).getOrderItemSeqId();
                    } else {
                        // more than one match, considering all matches to be equivalent, pick the first one and remove it from the list of candidates
                        TreeSet<String> potentials = new TreeSet(Entity.getDistinctFieldValues(String.class, equivalentItems, OrderItem.Fields.orderItemSeqId));
                        equivalentItemSeqId = potentials.first();
                    }

                    Debug.logInfo("Adding promo item of " + item.getQuantity() + " x [" + item.getProductId() + "], found equivalent item [" + equivalentItemSeqId + "] in the order.", MODULE);
                    item.setOrderItemSeqId(equivalentItemSeqId);
                    existingPromoItemsPotentials.remove(equivalentItemSeqId);
                    continue;
                }
                // create the new promotion item from the cart
                OrderItem orderItem = new OrderItem();
                orderItem.initRepository(repository);
                orderItem.setOrderId(orderId);
                orderItem.setOrderItemTypeId(item.getItemType());
                orderItem.setSelectedAmount(item.getSelectedAmount());
                orderItem.setUnitPrice(item.getBasePrice());
                orderItem.setUnitListPrice(item.getListPrice());
                orderItem.setItemDescription(item.getName());
                orderItem.setStatusId(item.getStatusId());
                orderItem.setProductId(item.getProductId());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setIsModifiedPrice("N");
                orderItem.setIsPromo("Y");
                if (UtilValidate.isEmpty(orderItem.getStatusId())) {
                    orderItem.setStatusId(StatusItemConstants.OrderItemStatus.ITEM_CREATED);
                }
                orderItem.setNextSubSeqId(OrderItem.Fields.orderItemSeqId.name());
                item.setOrderItemSeqId(orderItem.getOrderItemSeqId());
                newPromoItems.add(orderItem.getOrderItemSeqId());

                // set the OrderItemShipGroupAssoc
                OrderItemShipGroupAssoc assoc = new OrderItemShipGroupAssoc();
                assoc.setOrderId(orderId);
                assoc.setOrderItemSeqId(orderItem.getOrderItemSeqId());
                assoc.setShipGroupSeqId("00001");
                assoc.setQuantity(orderItem.getQuantity());

                // create them
                repository.createOrUpdate(orderItem);
                repository.createOrUpdate(assoc);
            }

            // we can now cancel promotions items that remained in the order but are
            // not billed and have no related adjustments
            // note: if those cannot be canceled, some promotions wont work as expected
            //  because the cart does not load promotion items, for example an order total
            //  discount won't apply on the amount of those items
            if (orderIsBilled) {
                for (OrderItem item : repository.findList(OrderItem.class, Arrays.asList(EntityCondition.makeCondition(OrderItem.Fields.orderId.name(), EntityOperator.EQUALS, orderId), EntityCondition.makeCondition(OrderItem.Fields.orderItemSeqId.name(), EntityOperator.IN, existingPromoItemsPotentials)))) {
                    boolean billedAdjustments = false;
                    for (OrderAdjustment adj : item.getOrderAdjustments()) {
                        if (!adj.getOrderAdjustmentBillings().isEmpty()) {
                            billedAdjustments = true;
                            break;
                        }
                    }

                    // do not cancel if there was any billed adjustments
                    if (billedAdjustments) {
                        continue;
                    }

                    // do not cancel if the item is billed
                    if (item.getOrderItemBillings().isEmpty()) {
                        CancelOrderItemNoActionsService service2 = new CancelOrderItemNoActionsService();
                        service2.setInOrderId(orderId);
                        service2.setInOrderItemSeqId(item.getOrderItemSeqId());
                        runSync(service2);
                    }
                }
            }

            List<GenericValue> newAdjustments = cart.makeAllAdjustments();
            // sum the new total of promotion adjustments to offset changes
            // as a new global adjustment
            // but for all new adjustment linked to the newly created promotion items
            // create individual adjustments instead
            List<OrderAdjustment> toCreate = new ArrayList<OrderAdjustment>();
            for (GenericValue adj : newAdjustments) {
                String productPromoId = adj.getString("productPromoId");
                if (UtilValidate.isEmpty(productPromoId)) {
                    continue;
                }

                if (orderIsBilled && "__BILLED__".equals(adj.getString("orderItemSeqId"))) {
                    // skip this adjustment as it is related to a promotions item that was not added
                    Debug.logWarning("Not adding promo adjustment of " + adj.get("amount") + " for promoId [" + productPromoId + "], because the order is already billed and the related item was not added.", MODULE);
                    continue;
                }

                OrderAdjustment orderAdjustment = Repository.loadFromGeneric(OrderAdjustment.class, adj, repository);
                orderAdjustment.setOrderId(orderId);
                orderAdjustment.setOrderAdjustmentId(orderAdjustment.getNextSeqId());

                if (newPromoItems.contains(adj.getString("orderItemSeqId"))) {
                    // new promotion adjustments for new promo order item
                    toCreate.add(orderAdjustment);
                } else if (UtilValidate.isNotEmpty(adj.getString("orderItemSeqId"))) {
                    // new promotion adjustments for existing order item
                    if (orderIsBilled && orderItemAdjustmentExistingAmounts.containsKey(orderAdjustment.getOrderItemSeqId())) {
                        // if the item was billed, old promo were not removed, sum the new promo amounts in orderItemAdjustmentNewAmounts
                        sumAdjustmentInPerItemPerPromoAmountMap(orderItemAdjustmentNewAmounts, orderAdjustment);
                    } else {
                        // the item was not billed, all existing adjustments were removed, we can safely add the new ones
                        toCreate.add(orderAdjustment);
                    }
                } else {
                    // order level promotion
                    if (orderIsBilled) {
                        // if the order was billed, old promo were not removed, sum the new promo amounts in orderAdjustmentNewAmount
                        sumAdjustmentInPerPromoAmountMap(orderAdjustmentNewAmounts, orderAdjustment);
                    } else {
                        // the order was not billed, all existing adjustments were removed, we can safely add the new ones
                        toCreate.add(orderAdjustment);
                    }
                }
            }

            // create new adjustments / offset existing adjustments
            if (orderIsBilled) {
                // set empty new amounts for existing promotions that are now invalid
                for (String itemId : orderItemAdjustmentExistingAmounts.keySet()) {
                    if (!orderItemAdjustmentNewAmounts.containsKey(itemId)) {
                        orderItemAdjustmentNewAmounts.put(itemId, new HashMap<String, BigDecimal>());
                    }
                }

                // offset item adjustments
                for (String itemId : orderItemAdjustmentNewAmounts.keySet()) {
                    Map<String, BigDecimal> adjustmentsPerPromo = orderItemAdjustmentNewAmounts.get(itemId);
                    if (adjustmentsPerPromo.isEmpty()) {
                        // if the map is empty, there is no new amount instead
                        // we completely offset the existing adjustments for this item
                        adjustmentsPerPromo = orderItemAdjustmentExistingAmounts.get(itemId);
                        for (String promoId : adjustmentsPerPromo.keySet()) {
                            BigDecimal newAmount = adjustmentsPerPromo.get(promoId);
                            if (newAmount == null || newAmount.signum() == 0) {
                                continue;
                            }
                            newAmount = newAmount.negate();

                            OrderAdjustment adj = new OrderAdjustment();
                            adj.initRepository(repository);
                            adj.setOrderAdjustmentId(adj.getNextSeqId());
                            adj.setOrderId(orderId);
                            adj.setOrderItemSeqId(itemId);
                            adj.setProductPromoId(promoId);
                            adj.setOrderAdjustmentTypeId(OrderAdjustmentTypeConstants.PROMOTION_ADJUSTMENT);
                            adj.setAmount(newAmount);
                            adj.setDescription("Existing promotion was = " + newAmount.negate() + ", new promotion amount = " + BigDecimal.ZERO);
                            toCreate.add(adj);
                        }
                    } else {
                        // else there are new adjustments for this item
                        // either add them in full or if there was an existing adjustment
                        // with the same promo, offset it with the difference
                        for (String promoId : adjustmentsPerPromo.keySet()) {
                            OrderAdjustment adj = new OrderAdjustment();
                            adj.initRepository(repository);
                            adj.setOrderAdjustmentId(adj.getNextSeqId());
                            adj.setOrderId(orderId);
                            adj.setOrderItemSeqId(itemId);
                            adj.setProductPromoId(promoId);
                            adj.setOrderAdjustmentTypeId(OrderAdjustmentTypeConstants.PROMOTION_ADJUSTMENT);
                            // check the existing amount
                            Map<String, BigDecimal> existingAdjustments = orderItemAdjustmentExistingAmounts.get(itemId);
                            BigDecimal existingAmount = null;
                            BigDecimal newAmount = adjustmentsPerPromo.get(promoId);
                            if (existingAdjustments != null) {
                                existingAmount = existingAdjustments.get(promoId);
                            }
                            if (existingAmount != null) {
                                adj.setAmount(newAmount.subtract(existingAmount));
                                adj.setDescription("Existing promotion was = " + existingAmount + ", new promotion amount = " + newAmount);
                            } else {
                                adj.setAmount(newAmount);
                            }
                            toCreate.add(adj);
                        }
                    }
                }
                // offset order adjustments
                for (String promoId : orderAdjustmentNewAmounts.keySet()) {
                    BigDecimal newAmount = orderAdjustmentNewAmounts.get(promoId);
                    OrderAdjustment adj = new OrderAdjustment();
                    adj.initRepository(repository);
                    adj.setOrderAdjustmentId(adj.getNextSeqId());
                    adj.setOrderId(orderId);
                    adj.setProductPromoId(promoId);
                    adj.setOrderAdjustmentTypeId(OrderAdjustmentTypeConstants.PROMOTION_ADJUSTMENT);
                    // check the existing amount
                    BigDecimal existingAmount = orderAdjustmentExistingAmounts.get(promoId);
                    if (existingAmount != null) {
                        adj.setAmount(newAmount.subtract(existingAmount));
                        adj.setDescription("Existing promotion was = " + existingAmount + ", new promotion amount = " + newAmount);
                    } else {
                        adj.setAmount(newAmount);
                    }
                    toCreate.add(adj);
                }
            }

            repository.createOrUpdate(toCreate);

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    private void sumAdjustmentInPerItemPerPromoAmountMap(Map<String, Map<String, BigDecimal>> map, OrderAdjustment adj) {
        Map<String, BigDecimal> amounts = map.get(adj.getOrderItemSeqId());
        if (amounts == null) {
            amounts = new HashMap<String, BigDecimal>();
            map.put(adj.getOrderItemSeqId(), amounts);
        }
        sumAdjustmentInPerPromoAmountMap(amounts, adj);
    }

    private void sumAdjustmentInPerPromoAmountMap(Map<String, BigDecimal> map, OrderAdjustment adj) {
        BigDecimal adjAmount = map.get(adj.getProductPromoId());
        if (adjAmount == null) {
            adjAmount = BigDecimal.ZERO;
        }
        adjAmount = adjAmount.add(adj.getAmount());
        map.put(adj.getProductPromoId(), adjAmount);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public void recalcOrderTax() throws ServiceException {
        try {
            OrderDomainInterface domain = getDomainsDirectory().getOrderDomain();
            OrderRepositoryInterface repository = domain.getOrderRepository();
            OrderSpecificationInterface specification = repository.getOrderSpecification();

            // TODO: check that the current user is the placing customer

            // check permission
            if (!hasEntityPermission("ORDERMGR", "_UPDATE")) {
                throw new ServiceException("OpentapsError_PermissionDenied");
            }

            // get the order
            Order order = repository.getOrderById(orderId);

            // don't charge tax on purchase orders
            if (order.isPurchaseOrder()) {
                return;
            }

            // get the current order tax adjustments total
            BigDecimal totalExistingOrderTax = order.getTaxAmount();
            BigDecimal totalNewOrderTax = BigDecimal.ZERO;
            BigDecimal removedTaxAmount = BigDecimal.ZERO;
            BigDecimal createdTaxAmount = BigDecimal.ZERO;

            // may be needed for face-to-face orders
            PostalAddress originAddress = Repository.getFirst(order.getOriginAddresses());

            Boolean keepBilledTaxes = false;
            if (UtilValidate.isNotEmpty(contactMechId)) {
                Debug.logInfo("Found contactMechId [" + contactMechId + "], we are changing a ship group address so keep the already billed taxes", MODULE);
                keepBilledTaxes = true;
            }

            // for each ship groups, as they may have different shipping
            for (OrderItemShipGroup shipGroup : order.getOrderItemShipGroups()) {

                // sum the total amounts of items we are going to tax, we then use this to pro rate the global promo / shipping tax
                // for this ship group
                BigDecimal shipGroupSubTotal = BigDecimal.ZERO;

                // recalculate taxes for the order
                List<OrderItem> validItems = order.getValidItems(shipGroup);
                List<String> itemIds = new ArrayList<String>(validItems.size());
                List<GenericValue> products = new ArrayList<GenericValue>(validItems.size());
                List<BigDecimal> amounts = new ArrayList<BigDecimal>(validItems.size());
                List<BigDecimal> shippingAmounts = new ArrayList<BigDecimal>(validItems.size());
                List<BigDecimal> itemPrices = new ArrayList<BigDecimal>(validItems.size());

                for (int i = 0; i < validItems.size(); i++) {
                    OrderItem item = validItems.get(i);
                    // only calculate the taxes for the non shipped items
                    BigDecimal shippedQty = item.getShippedQuantity(shipGroup);
                    BigDecimal orderedQty = item.getOrderedQuantity(shipGroup);
                    BigDecimal qty = orderedQty;
                    if (keepBilledTaxes) {
                        qty = qty.subtract(shippedQty);
                    }
                    // pro rate the amount and shipping amount to the quantity actually in the ship group
                    // note: the qty cannot be 0 here as getValidItems filter those
                    products.add(i, Repository.genericValueFromEntity(item.getProduct()));
                    BigDecimal pr = qty.divide(item.getOrderedQuantity(), 99, BigDecimal.ROUND_HALF_EVEN);
                    amounts.add(i, item.getSubTotal().multiply(pr));
                    shippingAmounts.add(i, item.getShippingAmount().multiply(pr));
                    itemPrices.add(i, item.getUnitPrice());
                    itemIds.add(i, item.getOrderItemSeqId());
                    Debug.logInfo("Item [" + item.getOrderItemSeqId() + "] calculating tax with : qty = " + qty + " (total ordered = " + item.getOrderedQuantity() + ", in ship group = " + orderedQty + ", of which shipped = " + shippedQty + ", Price = " + item.getUnitPrice() + " ==> amount to tax = " + amounts.get(i), MODULE);
                    // note: use orderedQty instead of qty as we want total value for this ship group in order to compare it to the order subtotal
                    shipGroupSubTotal = shipGroupSubTotal.add(item.getSubTotal().multiply(orderedQty).divide(item.getOrderedQuantity(), 99, BigDecimal.ROUND_HALF_EVEN));
                }

                // determine the shipping address
                PostalAddress shippingAddress = shipGroup.getPostalAddress();
                // face-to-face order; use the facility address
                if (shippingAddress == null) {
                    shippingAddress = originAddress;
                }

                // still no address, then don't calculate tax; it may be an situation where no tax is applicable, or the data is bad and we don't have a way to find an address to check tax for
                if (shippingAddress == null) {
                    continue;
                }

                // call the calcTax service
                CalcTaxService service = new CalcTaxService();
                service.setInProductStoreId(order.getProductStoreId());
                service.setInItemProductList(products);
                service.setInItemAmountList(amounts);
                service.setInItemShippingList(shippingAmounts);
                service.setInItemPriceList(itemPrices);
                service.setInBillToPartyId(order.getBillToPartyId());
                service.setInShippingAddress(Repository.genericValueFromEntity(shippingAddress));
                // for tax authorities that have taxPromotions="Y", sends the sum promotions and this deduct the tax at the global level
                // note: this only includes the order level adjustments
                // note: we should pro rate the shipping and promotion amounts so the taxes are properly distributed among ship groups
                //  and make sure that multi ship group orders do not tax those multiple times
                BigDecimal orderPromotionsAmount = order.getOtherAdjustmentsAmount();
                BigDecimal orderShippingAmount = order.getShippingAmount();
                service.setInOrderPromotionsAmount(orderPromotionsAmount);
                service.setInOrderShippingAmount(orderShippingAmount);
                runSync(service);

                List<GenericValue> orderAdjustments = service.getOutOrderAdjustments();
                List<List<GenericValue>> itemAdjustments = service.getOutItemAdjustments();

                // get the global tax adjustments
                if (orderAdjustments != null) {
                    for (GenericValue adj : orderAdjustments) {
                        if (adj != null && adj.get("amount") != null) {
                            Debug.logInfo("Got new global tax adjustment: amount = " + adj.getBigDecimal("amount") + " for geo " + adj.get("taxAuthGeoId"), MODULE);
                            totalNewOrderTax = specification.taxCalculationRounding(totalNewOrderTax.add(adj.getBigDecimal("amount")));
                        }
                    }
                }

                // round the order global adjustment
                totalNewOrderTax = specification.taxFinalRounding(totalNewOrderTax);

                // recreate item taxes
                if (itemAdjustments != null) {
                    // note: the returned itemAdjustments is a list of list of adjustments, each given item may generate multiple adjustments, typically one per applicable tax authority
                    for (int i = 0; i < itemAdjustments.size(); i++) {
                        List<GenericValue> adjs = itemAdjustments.get(i);
                        if (adjs == null) {
                            continue;
                        }

                        OrderItem item = validItems.get(i);
                        BigDecimal shippedQty = item.getShippedQuantity(shipGroup);
                        BigDecimal orderedQty = item.getOrderedQuantity(shipGroup);
                        BigDecimal itemNewTaxAmount = BigDecimal.ZERO;

                        // get the existing tax adjustments for the same item then remove any non billed adjustment amounts
                        // since we only calculated the taxes for the non shipped items those are the one that will be left to recreate
                        List<OrderAdjustment> existingTaxes = repository.findList(OrderAdjustment.class, repository.map(
                                   OrderAdjustment.Fields.orderId, item.getOrderId(),
                                   OrderAdjustment.Fields.orderItemSeqId, item.getOrderItemSeqId(),
                                   OrderAdjustment.Fields.shipGroupSeqId, shipGroup.getShipGroupSeqId(),
                                   OrderAdjustment.Fields.orderAdjustmentTypeId, OrderAdjustmentTypeConstants.SALES_TAX));
                        BigDecimal existingItemTaxAmount = Entity.sumFieldValues(existingTaxes, OrderAdjustment.Fields.amount);
                        for (OrderAdjustment adj : existingTaxes) {
                            // if this adjustment was not billed, just remove it
                            removedTaxAmount = removedTaxAmount.add(adj.getAmount());
                            List<? extends OrderAdjustmentBilling> billedTaxes = adj.getOrderAdjustmentBillings();
                            if (UtilValidate.isEmpty(billedTaxes)) {
                                Debug.logInfo("Item [" + item.getOrderItemSeqId() + "] removing tax adj = " + adj.getAmount() + " for auth [" + adj.getTaxAuthGeoId() + " " + adj.getTaxAuthPartyId() + "]", MODULE);
                                repository.remove(adj);
                            } else {
                                // else trim the adjustment amount to the billed amount
                                BigDecimal billedTaxAmount = Entity.sumFieldValues(billedTaxes, OrderAdjustmentBilling.Fields.amount);
                                Debug.logInfo("Item [" + item.getOrderItemSeqId() + "] trimming tax adj = " + adj.getAmount() + " to billed amount = " + billedTaxAmount + " for auth [" + adj.getTaxAuthGeoId() + " " + adj.getTaxAuthPartyId() + "]", MODULE);
                                adj.setAmount(billedTaxAmount);
                                // mark this adjustment that it applied to the already shipped quantity so that it does not get pro rated in future Invoices
                                BigDecimal applyToQty = shippedQty;
                                adj.setAppliesToQuantity(applyToQty);
                                // if we do not keep the billed taxes, cancel the existing item tax
                                if (!keepBilledTaxes) {
                                    repository.createOrUpdate(adj);
                                    // mark the offsetting adjustment to apply to the non shipped items so they are properly pro rated in future Invoices
                                    adj.setAppliesToQuantity(orderedQty.subtract(shippedQty));
                                    adj.setOrderAdjustmentId(adj.getNextSeqId());
                                    adj.setAmount(billedTaxAmount.negate());
                                    repository.createOrUpdate(adj);
                                } else {
                                    repository.createOrUpdate(adj);
                                    removedTaxAmount = removedTaxAmount.subtract(billedTaxAmount);
                                    itemNewTaxAmount = itemNewTaxAmount.add(billedTaxAmount);
                                }
                            }
                        }

                        // now create the new taxes adjustments
                        for (GenericValue adj : adjs) {
                            if (adj == null || adj.get("amount") == null) {
                                continue;
                            }
                            Debug.logInfo("Got recalculated tax for order [" + orderId + "] item [" + itemIds.get(i) + "] ship group [" + shipGroup.getShipGroupSeqId() + "], was given product [" + products.get(i) + "], amount [" + amounts.get(i) + "], item price [" + itemPrices.get(i) + "], shipping amount [" + shippingAmounts.get(i) + "]  ===> tax amount = " + adj.getBigDecimal("amount") + " for geo " + adj.get("taxAuthGeoId"), MODULE);
                            OrderAdjustment orderAdjustment = Repository.loadFromGeneric(OrderAdjustment.class, adj, repository);
                            orderAdjustment.setOrderId(orderId);
                            orderAdjustment.setOrderItemSeqId(itemIds.get(i));
                            orderAdjustment.setShipGroupSeqId(shipGroup.getShipGroupSeqId());
                            orderAdjustment.setOrderAdjustmentId(orderAdjustment.getNextSeqId());
                            // mark the offsetting adjustment to apply to the non shipped items so they are properly pro rated in future Invoices
                            orderAdjustment.setAppliesToQuantity(orderedQty.subtract(shippedQty));
                            // add the new tax adjustment
                            Debug.logInfo("Creating new tax adjustment for order [" + orderId + "] item [" + itemIds.get(i) + "] ship group [" + shipGroup.getShipGroupSeqId() + "] and tax auth [" + orderAdjustment.getTaxAuthGeoId() + " " + orderAdjustment.getTaxAuthPartyId() + "], new tax amount = " + orderAdjustment.getAmount(), MODULE);
                            repository.createOrUpdate(orderAdjustment);
                            // account to the new total
                            createdTaxAmount = createdTaxAmount.add(orderAdjustment.getAmount());
                            itemNewTaxAmount = itemNewTaxAmount.add(orderAdjustment.getAmount());
                        }

                        // round the item adjustments
                        // this reflects the rounding that is being done when getting the order taxes
                        // each item tax component is rounded according to taxCalculationRounding, then the tax total by taxFinalRounding
                        Debug.logInfo("Item for order [" + orderId + "] item [" + itemIds.get(i) + "] ship group [" + shipGroup.getShipGroupSeqId() + "], existing tax total was = " + existingItemTaxAmount + ", new tax total = " + itemNewTaxAmount, MODULE);
                        totalNewOrderTax = specification.taxFinalRounding(totalNewOrderTax.add(itemNewTaxAmount));
                        createdTaxAmount = specification.taxFinalRounding(createdTaxAmount);
                        removedTaxAmount = specification.taxFinalRounding(removedTaxAmount);
                    }
                }
            }

            // create the offset for the global order tax adjustments
            // not that those totals are already rounded with taxFinalRounding
            BigDecimal orderTaxDifference = totalNewOrderTax.subtract(createdTaxAmount).subtract(totalExistingOrderTax.subtract(removedTaxAmount));
            Debug.logInfo("totalNewOrderTax = " + totalNewOrderTax + " created = " + createdTaxAmount + ", removed = " + removedTaxAmount + ", totalExistingOrderTax = " + totalExistingOrderTax + " ==> orderTaxDifference = " + orderTaxDifference , MODULE);
            if (orderTaxDifference.signum() != 0) {
                CreateOrderAdjustmentService service = new CreateOrderAdjustmentService();
                service.setInOrderId(orderId);
                service.setInOrderItemSeqId("_NA_");
                service.setInShipGroupSeqId("_NA_");
                // because this tax did not exist when the order was first created
                // if any part of the order was billed already prorating must not be used
                // when billing this adjustment
                service.setInNeverProrate("Y");
                service.setInOrderAdjustmentTypeId(OrderAdjustmentTypeConstants.SALES_TAX);
                service.setInDescription("Sales Tax adjustment due to order change");
                service.setInAmount(orderTaxDifference);
                runSync(service);
            }

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void resetGrandTotal() throws ServiceException {
        try {
            OrderDomainInterface domain = getDomainsDirectory().getOrderDomain();
            OrderRepositoryInterface repository = domain.getOrderRepository();

            // get the order
            Order order = repository.getOrderById(orderId);

            BigDecimal currentTotal = order.getGrandTotal();
            BigDecimal currentSubTotal = order.getRemainingSubTotal();

            // get the new grand total
            BigDecimal updatedTotal = order.getTotal();

            // calculate subTotal as grandTotal - returnsTotal - (tax + shipping of items not returned)
            OrderReadHelper orh = new OrderReadHelper(Repository.genericValueFromEntity(order));
            BigDecimal remainingSubTotal = updatedTotal.subtract(orh.getOrderReturnedTotal()).subtract(orh.getOrderNonReturnedTaxAndShipping());

            Debug.logInfo("resetGrandTotal: order [" + orderId + "] grand total: " + currentTotal + " >> " + updatedTotal + ", remainingSubTotal: " + currentSubTotal + " >> " + remainingSubTotal, MODULE);

            if (currentTotal == null || currentSubTotal == null || updatedTotal.compareTo(currentTotal) != 0 || remainingSubTotal.compareTo(currentSubTotal) != 0) {
                order.setGrandTotal(updatedTotal);
                order.setRemainingSubTotal(remainingSubTotal);
                repository.update(order);
            }


        } catch (GeneralException e) {
            throw new ServiceException("Could not set grandTotal on OrderHeader entity: " + e.toString());
        }
    }

    /** {@inheritDoc} */
    public void addNote(String noteText, boolean isInternal) throws ServiceException {
        try {
            CreateOrderNoteService service = new CreateOrderNoteService();
            service.setInOrderId(orderId);
            service.setInNote(noteText);
            if (isInternal) {
                service.setInInternalNote("Y");
            } else {
                service.setInInternalNote("N");
            }
            runSync(service);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void setContactMechId(String contactMechId) {
        this.contactMechId = contactMechId;
    }

    /** {@inheritDoc} */
    public void setNewOrderContactMechId(String newOrderContactMechId) {
        this.newOrderContactMechId = newOrderContactMechId;
    }

    /** {@inheritDoc} */
    public void updateOrderShippingAddress() throws ServiceException {

        if (UtilValidate.isEmpty(newOrderContactMechId)) {
            return;
        }

        try {

            OrderRepositoryInterface repository = getDomainsDirectory().getOrderDomain().getOrderRepository();
            repository.setInfrastructure(getInfrastructure());
            repository.setUser(getUser());

            repository.updateOrderAddress(orderId, newOrderContactMechId, ContactMechPurposeTypeConstants.SHIPPING_LOCATION);

        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }

    }

    /** {@inheritDoc} */
    public void updateOrderBillingAddress() throws ServiceException {

        if (UtilValidate.isEmpty(newOrderContactMechId)) {
            return;
        }

        try {

            OrderRepositoryInterface repository = getDomainsDirectory().getOrderDomain().getOrderRepository();
            repository.setInfrastructure(getInfrastructure());
            repository.setUser(getUser());

            repository.updateOrderAddress(orderId, newOrderContactMechId, ContactMechPurposeTypeConstants.BILLING_LOCATION);

        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }

    }
 
    /** {@inheritDoc} */
    public void autoSetOrderBillingAddress() throws ServiceException {

        try {

            OrderRepositoryInterface repository = getDomainsDirectory().getOrderDomain().getOrderRepository();

            // get the order
            Order order = repository.getOrderById(orderId);

            // if the order has a billing address we have nothing to do
            if (UtilValidate.isNotEmpty(order.getBillingAddresses())) {
                Debug.logInfo("Order [" + orderId + "] already has a billing address, nothing else to do.", MODULE);
                return;
            }

            PostalAddress defaultBillingAddress = null;

            // check for any payment method set for this order that have a billing address
            for (OrderPaymentPreference pref : order.getOrderPaymentPreferences()) {
                PaymentMethod pm = pref.getPaymentMethod();
                if (pm == null) {
                    continue;
                }
                // not all payment methods have a billing address
                defaultBillingAddress = pm.getPostalAddress();
                // stop at the first found address (usually there would be only one payment method anyway)
                if (defaultBillingAddress != null) {
                    Debug.logInfo("Found billing address [" + defaultBillingAddress.getContactMechId() + "] from payment method [" + pm.getPaymentMethodTypeId() + ":" + pm.getPaymentMethodId() + "] for Order [" + orderId + "].", MODULE);
                    break;
                }
            }

            // else get a default billing address from the customer
            if (defaultBillingAddress == null && order.getPlacingCustomer() != null) {
                defaultBillingAddress = order.getPlacingCustomer().getBillingAddress();
                if (defaultBillingAddress != null) {
                    Debug.logInfo("Found default billing address [" + defaultBillingAddress.getContactMechId() + "] for customer [" + order.getPlacingCustomer().getPartyId() + "] for Order [" + orderId + "].", MODULE);
                }
            }

            // no address found, nothing to do
            if (defaultBillingAddress == null) {
                Debug.logInfo("No billing address found in either the payment methods or the customer for Order [" + orderId + "].", MODULE);
                return;
            }

            // if we found an address set it to the order
            repository.updateOrderAddress(orderId, defaultBillingAddress.getContactMechId(), ContactMechPurposeTypeConstants.BILLING_LOCATION);

        } catch (GeneralException e) {
            throw new ServiceException(e);
        }

    }

}
