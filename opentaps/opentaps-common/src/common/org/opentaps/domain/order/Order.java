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
package org.opentaps.domain.order;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javolution.util.FastList;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.opentaps.base.entities.ContactMech;
import org.opentaps.base.entities.ContactMechPurposeType;
import org.opentaps.base.entities.Enumeration;
import org.opentaps.base.entities.OrderAdjustmentBilling;
import org.opentaps.base.entities.OrderHeaderNoteView;
import org.opentaps.base.entities.OrderStatus;
import org.opentaps.base.entities.OrderTerm;
import org.opentaps.base.entities.OrderType;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.entities.StatusItem;
import org.opentaps.base.entities.TelecomNumber;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentGatewayResponse;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Order entity and domain.
 */
public class Order extends org.opentaps.base.entities.OrderHeader {

    private static final String MODULE = Order.class.getName();

    private List<ContactMech> contactMechs;
    private List<Invoice> invoices;
    private List<OrderItem> orderItems;
    private List<OrderItem> validOrderItems;
    private List<OrderItemShipGroup> shipGroups;
    private List<OrderAdjustment> adjustments;
    private List<OrderAdjustment> nonItemAdjustments;
    private List<OrderAdjustment> shippingAdjustments;
    private List<OrderAdjustment> nonShippingAdjustments;
    private List<OrderHeaderNoteView> notes;
    private List<OrderPaymentPreference> paymentPreferences;
    private List<OrderPaymentPreference> nonCancelledPaymentPreferences;
    private List<OrderStatus> orderStatuses;
    private List<PostalAddress> billingAddresses;
    private List<PostalAddress> shippingAddresses;
    private List<Payment> payments;
    private List<ReturnItem> returnItems;
    private List<TelecomNumber> phoneNumbers;
    private Map<OrderItem, Map> returnableItemsMap;
    private ProductStore productStore;
    private OrderStatus orderStatus;
    private BigDecimal itemsSubTotal;
    private BigDecimal otherAdjustmentsAmount;
    private BigDecimal globalTaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal grandTotal;
    private BigDecimal openAmount;
    private BigDecimal totalMinusPaymentPrefs;
    private OrderRole affiliateOrderRole;
    private OrderRole distributorOrderRole;
    private OrderRole placingCustomerOrderRole;
    private OrderRole billToCustomerOrderRole;
    private OrderRole billFromVendorOrderRole;
    private OrderRole supplierAgentOrderRole;
    private OrderRole shipToCustomerOrderRole;
    private Party affiliate;
    private Party distributor;
    private Party placingCustomer;
    private Party billToCustomer;
    private Party billFromVendor;
    private Party supplierAgent;
    private Party shipToCustomer;

    /**
     * Default constructor.
     */
    public Order() {
        super();
    }

    /**
     * Get the specification object which contains enumerations and logical checking for Orders.
     * @return the <code>OrderSpecificationInterface</code>
     */
    public OrderSpecificationInterface getOrderSpecification() {
        return getRepository().getOrderSpecification();
    }

    /**
     * Checks if this order is sales order.
     * @return a <code>Boolean</code> value
     */
    public Boolean isSalesOrder() {
        return getOrderSpecification().isSalesOrder(this);
    }

    /**
     * Checks if this order is a purchase order.
     * @return a <code>Boolean</code> value
     */
    public Boolean isPurchaseOrder() {
        return getOrderSpecification().isPurchaseOrder(this);
    }

    /**
     * Checks if this order current status is "approved".
     * @return a <code>Boolean</code> value
     */
    public Boolean isApproved() {
        return getOrderSpecification().isApproved(this);
    }

    /**
     * Checks if this order current status is "completed".
     * @return a <code>Boolean</code> value
     */
    public Boolean isCompleted() {
        return getOrderSpecification().isCompleted(this);
    }

    /**
     * Checks if this order current status is "cancelled".
     * @return a <code>Boolean</code> value
     */
    public Boolean isCancelled() {
        return getOrderSpecification().isCancelled(this);
    }

    /**
     * Checks if this order current status is "created".
     * @return a <code>Boolean</code> value
     */
    public Boolean isCreated() {
        return getOrderSpecification().isCreated(this);
    }

    /**
     * Checks if this order current status is "rejected".
     * @return a <code>Boolean</code> value
     */
    public Boolean isRejected() {
        return getOrderSpecification().isRejected(this);
    }

    /**
     * Checks if this order current status is "sent".
     * @return a <code>Boolean</code> value
     */
    public Boolean isSent() {
        return getOrderSpecification().isSent(this);
    }

    /**
     * Checks if this order current status is "processing".
     * @return a <code>Boolean</code> value
     */
    public Boolean isProcessing() {
        return getOrderSpecification().isProcessing(this);
    }

    /**
     * Checks if this order current status is "on hold".
     * @return a <code>Boolean</code> value
     */
    public Boolean isOnHold() {
        return getOrderSpecification().isOnHold(this);
    }

    /**
     * Checks if this order is "pickable", should appear on a picklist.
     * @return a <code>Boolean</code> value
     */
    public Boolean isPickable() {
        return getOrderSpecification().isPickable(this);
    }

    /**
     * Order is open if it is created, approved or held.
     * @return a <code>true</code> if order is open.
     */
    public Boolean isOpen() {
        return (isCreated() || isApproved() || isOnHold());
    }

    /**
     * Gets this order <code>OrderType</code>.
     * This is an alias for {@link org.opentaps.base.entities.OrderHeader#getOrderType}.
     * @return the <code>OrderType</code>
     * @throws RepositoryException if an error occurs
     */
    public OrderType getType() throws RepositoryException {
        return this.getOrderType();
    }

    /**
     * Gets the return type that applies to this order type.
     * @return the return type that is allowed for this order
     * @throws RepositoryException if an error occurs
     */
    public String getReturnType() throws RepositoryException {
        return getOrderSpecification().getReturnType(this);
    }

    /**
     * Gets the sales channel for this order.
     * This is an alias for {@link org.opentaps.base.entities.OrderHeader#getSalesChannelEnumeration}.
     * @return the sales channel <code>Enumeration</code>
     * @throws RepositoryException if an error occurs
     */
    public Enumeration getSalesChannel() throws RepositoryException {
        return this.getSalesChannelEnumeration();
    }

    /**
     * Get the Set of product Ids from the order items of this order.
     * @return list of product ids
     * @throws RepositoryException if an error occurs
     */
    public Set<String> getProductIds() throws RepositoryException {
        Set<String> productIds = new HashSet<String>();
        for (OrderItem item : getItems()) {
            productIds.add(item.getProductId());
        }
        return productIds;
    }

    /**
     * Gets the product store for this order.
     * Returns the order domain object instead of the base entity.
     * @return the <code>ProductStore</code> for this order
     * @throws RepositoryException if an error occurs
     */
    @Override
    public ProductStore getProductStore() throws RepositoryException {
        if (this.productStore == null) {
            this.productStore = getRelatedOne(ProductStore.class, "ProductStore");
        }
        return this.productStore;
    }

    /**
     * Gets a specific order item.  If the order items are already loaded, it
     * returns from the list.  Otherwise it fetches the item directly from
     * the persistence layer.
     * @param  orderItemSeqId The line item sequence number of the order item
     * @return OrderItem
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    public OrderItem getOrderItem(String orderItemSeqId) throws RepositoryException, EntityNotFoundException {
        if (orderItems != null) {
            for (OrderItem item : orderItems) {
                if (item.getOrderItemSeqId().equals(orderItemSeqId)) {
                    return item;
                }
            }
            // TODO these exceptions should be constructed on the basis of Entity.class and the pk.
            throw new EntityNotFoundException();
        } else {
            return getRepository().getOrderItem(this, orderItemSeqId);
        }
    }

    /**
     * Gets the <code>OrderItems</code> for this order, ordered by orderItemSeqId.
     * Returns the order domain object instead of the base entity.
     * @return the list of <code>OrderItem</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<OrderItem> getOrderItems() throws RepositoryException {
        if (orderItems == null) {
            orderItems = getRelated(OrderItem.class, "OrderItem", Arrays.asList(OrderItem.Fields.orderItemSeqId.name()));
        }
        return orderItems;
    }

    /**
     * Gets the <code>OrderItems</code> for this order, ordered by orderItemSeqId.
     * This is an alias for {@link #getOrderItems}.
     * @return the list of <code>OrderItem</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItem> getItems() throws RepositoryException {
        return this.getOrderItems();
    }

    /**
     * Gets the <code>OrderItems</code> for this order that are promotions, ordered by orderItemSeqId.
     * @return the list of <code>OrderItem</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItem> getPromotionItems() throws RepositoryException {
        List<OrderItem> promoItems = new ArrayList<OrderItem>();
        for (OrderItem item : getOrderItems()) {
            if (item.isPromo()) {
                promoItems.add(item);
            }
        }
        return promoItems;
    }

    /**
     * Gets the <code>ReturnItems</code> for this order.
     * Returns the order domain object instead of the base entity.
     * @return the list of <code>ReturnItem</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<ReturnItem> getReturnItems() throws RepositoryException {
        if (returnItems == null) {
            returnItems = getRelated(ReturnItem.class, "ReturnItem");
        }
        return returnItems;
    }

    /**
     * Gets the <code>OrderItems</code> for this order that are still valid (not cancelled or rejected).
     * @return list of valid <code>OrderItem</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItem> getValidItems() throws RepositoryException {
        if (validOrderItems == null) {
            validOrderItems = getRepository().getRelatedValidOrderItems(this);
        }
        return validOrderItems;
    }

    /**
     * Gets the <code>OrderItems</code> for this order that are still valid (not cancelled or rejected) and in the given <code>OrderItemShipGroup</code>.
     * @param shipGroup the <code>OrderItemShipGroup</code> to get the items from
     * @return list of valid <code>OrderItem</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItem> getValidItems(OrderItemShipGroup shipGroup) throws RepositoryException {
        return getRepository().getRelatedValidOrderItems(this, shipGroup);
    }

    /**
     * Get the <code>OrderItems</code> for this order that are still valid (not cancelled or rejected)
     *  and for which the ordered quantity is not zero.
     * @return list of valid <code>OrderItem</code> where ordered quantity is not zero.
     * @throws RepositoryException if an error occurs
     * @see #getValidItems
     */
    public List<OrderItem> getValidItemsWithQuantity() throws RepositoryException {
        List<OrderItem> results = new ArrayList<OrderItem>();
        for (OrderItem i : getValidItems()) {
            if (!i.getOrderedQuantity().equals(BigDecimal.ZERO)) {
                results.add(i);
            }
        }
        return results;
    }

    /**
     * Gets the <code>OrderItems</code> that are not completed.
     * @return list of uncomplete <code>OrderItem</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItem> getUncompleteItems() throws RepositoryException {
        List<OrderItem> results = new ArrayList<OrderItem>();
        for (OrderItem item : getItems()) {
            if (!item.isCompleted()) {
                results.add(item);
            }
        }
        return results;
    }

    /**
     * Gets the non physical <code>OrderItems</code> for this order.
     * @return list of <code>OrderItem</code> that are not physical products.
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItem> getNonPhysicalItems() throws RepositoryException {
        List<OrderItem> nonPhysicalItems = FastList.newInstance();
        for (OrderItem item : getItems()) {
            if (!item.isPhysical()) {
                nonPhysicalItems.add(item);
            }
        }
        return nonPhysicalItems;
    }

    /**
     * Gets the non physical <code>OrderItems</code> for this order that have the given status.
     * @param statusId status for the <code>OrderItems</code> to return
     * @return list of <code>OrderItem</code> that are not physical products and that match the given status.
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItem> getNonPhysicalItemsForStatus(final String statusId) throws RepositoryException {
        List<OrderItem> nonPhysicalItems = FastList.newInstance();
        for (OrderItem item : getItems()) {
            if (statusId.equals(item.getString("statusId")) && !item.isPhysical()) {
                nonPhysicalItems.add(item);
            }
        }
        return nonPhysicalItems;
    }

    /**
     * Gets the payment preferences for this order.
     * Returns the order domain object instead of the base entity.
     * @return list of <code>OrderPaymentPreference</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<OrderPaymentPreference> getOrderPaymentPreferences() throws RepositoryException {
        if (paymentPreferences == null) {
            paymentPreferences = getRelated(OrderPaymentPreference.class, "OrderPaymentPreference");
        }
        return paymentPreferences;
    }

    /**
     * Gets the payment preferences for this order.
     * This is an alias for {@link #getOrderPaymentPreferences}.
     * @return list of <code>OrderPaymentPreference</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderPaymentPreference> getPaymentPreferences() throws RepositoryException {
        return this.getOrderPaymentPreferences();
    }

    /**
     * Gets the payment preferences for this order that are not cancelled.
     * @return list of non cancelled <code>OrderPaymentPreference</code>
     * @throws RepositoryException if an error occurs
     * @see #getOrderPaymentPreferences
     */
    public List<OrderPaymentPreference> getNonCancelledPaymentPreferences() throws RepositoryException {
        if (nonCancelledPaymentPreferences == null) {
            nonCancelledPaymentPreferences = new ArrayList<OrderPaymentPreference>();
            for (OrderPaymentPreference pp : getPaymentPreferences()) {
                if (!pp.isCancelled()) {
                    nonCancelledPaymentPreferences.add(pp);
                }
            }
        }
        return nonCancelledPaymentPreferences;
    }

    /**
     * Gets this order payment preferences that are for a billing account.
     * @return list of <code>OrderPaymentPreference</code>
     * @throws RepositoryException if an error occurs
     * @see #getOrderPaymentPreferences
     */
    public List<OrderPaymentPreference> getBillingAccountPaymentPreferences() throws RepositoryException {
        List<OrderPaymentPreference> results = new ArrayList<OrderPaymentPreference>();
        for (OrderPaymentPreference opp : getPaymentPreferences()) {
            if (opp.isBillingAccountPayment()) {
                results.add(opp);
            }
        }
        return results;
    }

    /**
     * Get a specific order item ship group domain object from this order.
     * @param shipGroupSeqId a <code>String</code> ID of the ship group
     * @return a <code>OrderItemShipGroup</code>
     * @throws RepositoryException if an error occurs
     */
    public OrderItemShipGroup getOrderItemShipGroup(String shipGroupSeqId) throws RepositoryException {
        List<OrderItemShipGroup> groups = getOrderItemShipGroups();
        for (OrderItemShipGroup group : groups) {
            if (group.getShipGroupSeqId().equals(shipGroupSeqId)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Gets the order item ship groups for this order.
     * Returns the order domain object instead of the base entity.
     * @return list of <code>OrderItemShipGroup</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<OrderItemShipGroup> getOrderItemShipGroups() throws RepositoryException {
        if (shipGroups == null) {
            shipGroups = getRelated(OrderItemShipGroup.class, "OrderItemShipGroup");
        }
        return shipGroups;
    }

    /**
     * Gets the order item ship groups for this order.
     * This is an alias for {@link #getOrderItemShipGroups}.
     * @return list of <code>OrderItemShipGroup</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemShipGroup> getShipGroups() throws RepositoryException {
        return this.getOrderItemShipGroups();
    }

    /**
     * Gets the invoices for this order.
     * @return list of <code>Invoice</code>
     * @throws RepositoryException if an error occurs
     */
    public List<Invoice> getInvoices() throws RepositoryException {
        if (invoices == null) {
            invoices = getRepository().getRelatedInvoices(this);
        }
        return invoices;
    }

    /**
     * Gets the payments related to this order.
     * @return list of <code>Payment</code>
     * @throws RepositoryException if an error occurs
     */
    public List<Payment> getPayments() throws RepositoryException {
        if (payments == null) {
            payments = getRepository().getRelatedPayments(this);
        }
        return payments;
    }

    /**
     * Gets the notes for this order ordered from most recent to oldest.
     * @return list of <code>OrderHeaderNoteView</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<OrderHeaderNoteView> getOrderHeaderNoteViews() throws RepositoryException {
        if (notes == null) {
            notes = getRepository().getRelatedOrderNotes(this);
        }
        return notes;
    }

    /**
     * Gets the notes for this order ordered from most recent to oldest.
     * This is an alias for {@link #getOrderHeaderNoteViews}.
     * @return list of <code>OrderHeaderNoteView</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderHeaderNoteView> getNotes() throws RepositoryException {
        return this.getOrderHeaderNoteViews();
    }

    /**
     * Gets the adjustments for this order.
     * Returns the order domain object instead of the base entity.
     * @return list of <code>OrderAdjustment</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<OrderAdjustment> getOrderAdjustments() throws RepositoryException {
        if (adjustments == null) {
            adjustments = getRelated(OrderAdjustment.class, "OrderAdjustment");
        }
        return adjustments;
    }

    /**
     * Gets the adjustments for this order.
     * This is an alias for {@link #getOrderAdjustments}.
     * @return list of <code>OrderAdjustment</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderAdjustment> getAdjustments() throws RepositoryException {
        return this.getOrderAdjustments();
    }

    /**
     * Gets the adjustments for this order that are not related to an order item.
     * This return adjustments that are global to this order such as shipping charge, global promotion, etc ...
     * @return sub list of <code>OrderAdjustment</code> that are not related to an order item
     * @throws RepositoryException if an error occurs
     * @see #getOrderAdjustments
     */
    public List<OrderAdjustment> getNonItemAdjustments() throws RepositoryException {
        if (nonItemAdjustments == null) {
            nonItemAdjustments = getRepository().getRelatedNonItemOrderAdjustments(this);
        }
        return nonItemAdjustments;
    }

    /**
     * Gets the total value of all order adjustments not related to a particular order item.
     * @return the total value of all order adjustments not related to a particular order item
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getNonItemAdjustmentValue() throws RepositoryException {
        BigDecimal result = BigDecimal.ZERO;
        List<OrderAdjustment> nonItemAdjustments = getNonItemAdjustments();

        for (OrderAdjustment adjustment : nonItemAdjustments) {
            result = result.add(adjustment.getAmount());
        }

        return result;
    }

    /**
     * Gets the total value of order adjustments not related to a particular order item which have been invoiced.
     * @return the total value of order adjustments not related to a particular order item which have been invoiced
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getInvoicedNonItemAdjustmentValue() throws RepositoryException {
        BigDecimal result = BigDecimal.ZERO;
        List<OrderAdjustment> nonItemAdjustments = getNonItemAdjustments();
        for (OrderAdjustment adjustment : nonItemAdjustments) {
            List<? extends OrderAdjustmentBilling> adjustmentBillings = adjustment.getOrderAdjustmentBillings();
            for (OrderAdjustmentBilling adjustmentBilling : adjustmentBillings) {
                result = result.add(adjustmentBilling.getAmount());
            }
        }

        return result;
    }

    /**
     * Gets the total value of order adjustments not related to a particular order item which have not been invoiced.
     * @return the total value of order adjustments not related to a particular order item which have not been invoiced
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getUninvoicedNonItemAdjustmentValue() throws RepositoryException {
        return getNonItemAdjustmentValue().subtract(getInvoicedNonItemAdjustmentValue());
    }

    /**
     * Gets the shipping adjustments for this order that are not related to an order item.
     * @return sub list of the order shipping <code>OrderAdjustment</code> that are not related to an order item
     * @throws RepositoryException if an error occurs
     * @see #getOrderAdjustments
     * @see #getNonItemAdjustments
     * @see #getNonShippingAdjustments
     */
    public List<OrderAdjustment> getShippingAdjustments() throws RepositoryException {
        if (shippingAdjustments != null) {
            return shippingAdjustments;
        }

        shippingAdjustments = new ArrayList<OrderAdjustment>();
        for (OrderAdjustment adj : getNonItemAdjustments()) {
            if (adj.isShippingCharge()) {
                shippingAdjustments.add(adj);
            }
        }
        return shippingAdjustments;
    }

    /**
     * Gets the non shipping adjustments for this order that are not related to an order item.
     * @return sub list of the <code>OrderAdjustment</code> other than shipping and that are not related to an order item
     * @throws RepositoryException if an error occurs
     * @see #getOrderAdjustments
     * @see #getNonItemAdjustments
     * @see #getShippingAdjustments
     */
    public List<OrderAdjustment> getNonShippingAdjustments() throws RepositoryException {
        if (nonShippingAdjustments != null) {
            return nonShippingAdjustments;
        }

        nonShippingAdjustments = new ArrayList<OrderAdjustment>();
        for (OrderAdjustment adj : getNonItemAdjustments()) {
            if (!adj.isShippingCharge()) {
                nonShippingAdjustments.add(adj);
            }
        }
        return nonShippingAdjustments;
    }

    /**
     * Gets the order statuses for this order.
     * This list describe the history of status this order went through, with
     *  the current status being the first of the list.
     * This overrides {@link org.opentaps.base.entities.OrderHeader#getOrderStatuses} to specify the ordering.
     * @return list of <code>OrderStatus</code> from current to oldest, that relate this order to a <code>StatusItem</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<OrderStatus> getOrderStatuses() throws RepositoryException {
        if (orderStatuses == null) {
            orderStatuses = getRepository().getRelatedOrderStatuses(this);
        }
        return orderStatuses;
    }

    /**
     * Gets this order current <code>StatusItem</code>.
     * This is an alias for {@link org.opentaps.base.entities.OrderHeader#getStatusItem}.
     * @return the current <code>StatusItem</code>
     * @throws RepositoryException if an error occurs
     * @see #getOrderStatuses
     */
    public StatusItem getStatus() throws RepositoryException {
        return this.getStatusItem();
    }

    /**
     * Gets this order current <code>OrderStatus</code>.
     * To get the current <code>StatusItem</code> use {@link #getStatus} instead
     * @return the current <code>OrderStatus</code>, that relate the order to its current <code>StatusItem</code>
     * @throws RepositoryException if an error occurs
     */
    public OrderStatus getOrderStatus() throws RepositoryException {
        if (orderStatus == null) {
            orderStatus = getRepository().getRelatedOrderStatus(this);
        }
        return orderStatus;
    }

    /**
     * Gets the terms for this order.
     * This is an alias for {@link org.opentaps.base.entities.OrderHeader#getOrderTerms}.
     * @return list of <code>OrderTerm</code>
     * @throws RepositoryException if an error occurs
     */
    public List<? extends OrderTerm> getTerms() throws RepositoryException {
        return this.getOrderTerms();
    }

    /**
     * Gets the primary customer PO number for this order.
     * @return the <code>correspondingPoId</code> of the first order item
     * @throws RepositoryException if an error occurs
     */
    public String getPrimaryPoNumber() throws RepositoryException {
        String correspondingPoId = null;
        for (OrderItem item : getItems()) {
            correspondingPoId = item.getCorrespondingPoId();
            if (correspondingPoId != null) {
                break;
            }
        }
        return correspondingPoId;
    }

    /**
     * Gets the max amount for the billing account related to this the order.
     * @return 0 if no billing account is related, else the maximum of all <code>OrderPaymentPreferences</code> maxAmount for the billing account
     * @throws RepositoryException if an error occurs
     * @see #getBillingAccountPaymentPreferences
     */
    public BigDecimal getBillingAccountMaxAmount() throws RepositoryException {
        if (getBillingAccount() == null) {
            return BigDecimal.ZERO;
        } else {
            BigDecimal result = BigDecimal.ZERO;
            for (OrderPaymentPreference opp : getBillingAccountPaymentPreferences()) {
                if (opp.getMaxAmount() != null) {
                    result = result.max(opp.getMaxAmount());
                }
            }
            return result;
        }
    }

    /**
     * Gets the earliest ship by date for this order.
     * @return the earliest ship by date from all <code>OrderItemShipGroup</code>, or null if no date was found
     * @throws RepositoryException if an error occurs
     */
    public Timestamp getEarliestShipByDate() throws RepositoryException {
        if (getShipGroups().isEmpty()) {
            return null;
        }
        OrderItemShipGroup shipGroup = getShipGroups().get(0);
        return shipGroup.getShipByDate();
    }

    /**
     * Get the earliest ship by date for this order.
     * @param timeZone a <code>TimeZone</code> value
     * @param locale a <code>Locale</code> value
     * @return the formatted date
     * @throws RepositoryException if an error occurs
     */
    public String getEarliestShipByDate(final TimeZone timeZone, final Locale locale) throws RepositoryException {
        Timestamp date = getEarliestShipByDate();
        if (date == null) { return "N/A"; }
        return UtilDateTime.timeStampToString(date, UtilDateTime.getDateFormat(locale), timeZone, locale);
    }


    /**
     * Returns the correct organization party of the order, depending on type of order (purchase order, sales order).
     * @return the customer for a purchase order, the vendor from a sales order
     * @throws RepositoryException if an error occurs
     */
    public Party getOrganizationParty() throws RepositoryException {
        if (isPurchaseOrder()) {
            return getBillToCustomer();
        } else {
            return getBillFromVendor();
        }
    }
    /**
     * Gets the "placing customer" <code>OrderRole</code> for this order.
     * @return the <code>OrderRole</code> for the "placing customer"
     * @throws RepositoryException if an error occurs
     */
    public OrderRole getPlacingCustomerOrderRole() throws RepositoryException {
        if (placingCustomerOrderRole == null) {
            placingCustomerOrderRole = getRepository().getRelatedOrderRoleByTypeId(this, getOrderSpecification().placingCustomerRoleTypeIds());
        }
        return placingCustomerOrderRole;
    }

    /**
     * Gets the "placing customer" party for this order.
     * @return the <code>Party</code> for the "placing customer"
     * @throws RepositoryException if an error occurs
     */
    public Party getPlacingCustomer() throws RepositoryException {
        if (placingCustomer == null) {
            OrderRole role = getPlacingCustomerOrderRole();
            if (role == null || role.getPartyId() == null) {
                return null;
            }
            placingCustomer = role.getParty();
        }
        return placingCustomer;
    }

    /**
     * Gets the "placing customer" party id for this order.
     * @return party id of the "placing customer"
     * @throws RepositoryException if an error occurs
     */
    public String getPlacingCustomerPartyId() throws RepositoryException {
        OrderRole role = getPlacingCustomerOrderRole();
        if (role == null) {
            return null;
        }
        return role.getPartyId();
    }

    /**
     * Gets the "bill to customer" <code>OrderRole</code> for this order.
     * @return the <code>OrderRole</code> of the "bill to customer"
     * @throws RepositoryException if an error occurs
     */
    public OrderRole getBillToCustomerOrderRole() throws RepositoryException {
        if (billToCustomerOrderRole == null) {
            billToCustomerOrderRole = getRepository().getRelatedOrderRoleByTypeId(this, getOrderSpecification().billToCustomerRoleTypeIds());
        }
        return billToCustomerOrderRole;
    }

    /**
     * Gets the "bill to customer" <code>Party</code> for this order.
     * @return the <code>Party</code> of the "bill to customer"
     * @throws RepositoryException if an error occurs
     */
    public Party getBillToCustomer() throws RepositoryException {
        if (billToCustomer == null) {
            OrderRole role = getBillToCustomerOrderRole();
            if (role == null || role.getPartyId() == null) {
                return null;
            }
            billToCustomer = role.getParty();
        }
        return billToCustomer;
    }

    /**
     * Gets the "bill to customer" party id for this order.
     * @return the party id of the "bill to customer"
     * @throws RepositoryException if an error occurs
     */
    public String getBillToCustomerPartyId() throws RepositoryException {
        OrderRole role = getBillToCustomerOrderRole();
        if (role == null) {
            return null;
        }
        return role.getPartyId();
    }

    /**
     * Gets the "bill from vendor" <code>OrderRole</code> for this order.
     * @return the <code>OrderRole</code> of the "bill from vendor"
     * @throws RepositoryException if an error occurs
     */
    public OrderRole getBillFromVendorOrderRole() throws RepositoryException {
        if (billFromVendorOrderRole == null) {
            billFromVendorOrderRole = getRepository().getRelatedOrderRoleByTypeId(this, getOrderSpecification().billFromVendorRoleTypeIds());
        }
        return billFromVendorOrderRole;
    }

    /**
     * Gets the "bill from vendor" <code>Party</code> for this order.
     * @return the <code>Party</code> of the "bill from vendor"
     * @throws RepositoryException if an error occurs
     */
    public Party getBillFromVendor() throws RepositoryException {
        if (billFromVendor == null) {
            OrderRole role = getBillFromVendorOrderRole();
            if (role == null || role.getPartyId() == null) {
                return null;
            }
            billFromVendor = role.getParty();
        }
        return billFromVendor;
    }

    /**
     * Gets the "bill from vendor" party id for this order.
     * @return the party id of the "bill from vendor"
     * @throws RepositoryException if an error occurs
     */
    public String getBillFromVendorPartyId() throws RepositoryException {
        OrderRole role = getBillFromVendorOrderRole();
        if (role == null) {
            return null;
        }
        return role.getPartyId();
    }

    /**
     * Gets the "supplier agent" <code>OrderRole</code> for this order.
     * @return the <code>OrderRole</code> of the "supplier agent"
     * @throws RepositoryException if an error occurs
     */
    public OrderRole getSupplierAgentOrderRole() throws RepositoryException {
        if (supplierAgentOrderRole == null) {
            supplierAgentOrderRole = getRepository().getRelatedOrderRoleByTypeId(this, getOrderSpecification().supplierAgentRoleTypeIds());
        }
        return supplierAgentOrderRole;
    }

    /**
     * Gets the "supplier agent" <code>Party</code> for this order.
     * @return the <code>Party</code> of the "supplier agent"
     * @throws RepositoryException if an error occurs
     */
    public Party getSupplierAgent() throws RepositoryException {
        if (supplierAgent == null) {
            OrderRole role = getSupplierAgentOrderRole();
            if (role == null || role.getPartyId() == null) {
                return null;
            }
            supplierAgent = role.getParty();
        }
        return supplierAgent;
    }

    /**
     * Gets the "supplier agent" party id for this order.
     * @return the party id of the "supplier agent"
     * @throws RepositoryException if an error occurs
     */
    public String getSupplierAgentPartyId() throws RepositoryException {
        OrderRole role = getSupplierAgentOrderRole();
        if (role == null) {
            return null;
        }
        return role.getPartyId();
    }

    /**
     * Gets the "ship to customer" <code>OrderRole</code> for this order.
     * @return the <code>OrderRole</code> of the "ship to customer"
     * @throws RepositoryException if an error occurs
     */
    public OrderRole getShipToCustomerOrderRole() throws RepositoryException {
        if (shipToCustomerOrderRole == null) {
            shipToCustomerOrderRole = getRepository().getRelatedOrderRoleByTypeId(this, getOrderSpecification().shipToCustomerRoleTypeIds());
        }
        return shipToCustomerOrderRole;
    }

    /**
     * Gets the "ship to customer" <code>Party</code> for this order.
     * @return the <code>Party</code> of the "ship to customer"
     * @throws RepositoryException if an error occurs
     */
    public Party getShipToCustomer() throws RepositoryException {
        if (shipToCustomer == null) {
            OrderRole role = getShipToCustomerOrderRole();
            if (role == null || role.getPartyId() == null) {
                return null;
            }
            shipToCustomer = role.getParty();
        }
        return shipToCustomer;
    }

    /**
     * Gets the "ship to customer" party id for this order.
     * @return the party id of the "ship to customer"
     * @throws RepositoryException if an error occurs
     */
    public String getShipToCustomerPartyId() throws RepositoryException {
        OrderRole role = getShipToCustomerOrderRole();
        if (role == null) {
            return null;
        }
        return role.getPartyId();
    }

    /**
     * Gets the "distributor" <code>OrderRole</code> for this order.
     * @return the <code>OrderRole</code> of the "distributor"
     * @throws RepositoryException if an error occurs
     */
    public OrderRole getDistributorOrderRole() throws RepositoryException {
        if (distributorOrderRole == null) {
            distributorOrderRole = getRepository().getRelatedOrderRoleByTypeId(this, getOrderSpecification().distributorRoleTypeIds());
        }
        return distributorOrderRole;
    }

    /**
     * Gets the "distributor" <code>Party</code> for this order.
     * @return the <code>Party</code> of the "distributor"
     * @throws RepositoryException if an error occurs
     */
    public Party getDistributor() throws RepositoryException {
        if (distributor == null) {
            OrderRole role = getDistributorOrderRole();
            if (role == null || role.getPartyId() == null) {
                return null;
            }
            distributor = role.getParty();
        }
        return distributor;
    }

    /**
     * Gets the "distributor" party id for this order.
     * @return the party id of the "distributor"
     * @throws RepositoryException if an error occurs
     */
    public String getDistributorPartyId() throws RepositoryException {
        OrderRole role = getDistributorOrderRole();
        if (role == null) {
            return null;
        }
        return role.getPartyId();
    }

    /**
     * Gets the "affiliate" <code>OrderRole</code> for this order.
     * @return the <code>OrderRole</code> of the "affiliate"
     * @throws RepositoryException if an error occurs
     */
    public OrderRole getAffiliateOrderRole() throws RepositoryException {
        if (affiliateOrderRole == null) {
            affiliateOrderRole = getRepository().getRelatedOrderRoleByTypeId(this, getOrderSpecification().affiliateRoleTypeIds());
        }
        return affiliateOrderRole;
    }

    /**
     * Gets the "affiliate" <code>Party</code> for this order.
     * @return the <code>Party</code> of the "affiliate"
     * @throws RepositoryException if an error occurs
     */
    public Party getAffiliate() throws RepositoryException {
        if (affiliate == null) {
            OrderRole role = getAffiliateOrderRole();
            if (role == null || role.getPartyId() == null) {
                return null;
            }
            affiliate = role.getParty();
        }
        return affiliate;
    }

    /**
     * Gets the "affiliate" party id for this order.
     * @return the party id of the "affiliate"
     * @throws RepositoryException if an error occurs
     */
    public String getAffiliatePartyId() throws RepositoryException {
        OrderRole role = getAffiliateOrderRole();
        if (role == null) {
            return null;
        }
        return role.getPartyId();
    }

    /**
     * Gets the main external <code>Party</code> for this order.
     * @return the <code>Party</code> for the "supplier agent" in case of a Purchase Order, or the "placing customer" for a Sales Order
     * @throws RepositoryException if an error occurs
      */
    public Party getMainExternalParty() throws RepositoryException {
        if (isPurchaseOrder()) {
            return getSupplierAgent();
        } else {
            return getPlacingCustomer();
        }
    }

    /**
     * Gets the "commission agents" parties for this order.
     * @return the list of <code>Party</code> with the "commission agent" role
     * @throws RepositoryException if an error occurs
     */
    public List<Party> getCommissionAgents() throws RepositoryException {
        List<OrderRole> roles = getRepository().getRelatedOrderRolesByTypeId(this, getOrderSpecification().commissionAgentRoleTypeIds());
        List<Party> partys = new ArrayList<Party>();
        for (OrderRole role : roles) {
            partys.add(role.getParty());
        }
        return partys;
    }

    /**
     * Gets the "commission agents" party ids for this order.
     * @return the list of party id for the "commission agents"
     * @throws RepositoryException if an error occurs
     */
    public List<String> getCommissionAgentsPartyIds() throws RepositoryException {
        List<OrderRole> roles = getRepository().getRelatedOrderRolesByTypeId(this, getOrderSpecification().commissionAgentRoleTypeIds());
        List<String> partyIds = new ArrayList<String>();
        for (OrderRole role : roles) {
            partyIds.add(role.getPartyId());
        }
        return partyIds;
    }

    /**
     * Gets the list of <code>ContactMech</code> associated to this order.
     * @return the list of <code>ContactMech</code> associated to the order
     * @throws RepositoryException if an error occurs
     */
    public List<ContactMech> getContactMechs() throws RepositoryException {
        if (contactMechs == null) {
            contactMechs = getRepository().getRelatedContactMechs(this);
        }
        return contactMechs;
    }

    /**
     * Gets the list of <code>TelecomNumber</code> associated to this order and to the main external party.
     * @return a list of <code>TelecomNumber</code>
     * @throws RepositoryException if an error occurs
     * @see #getMainExternalParty
     */
    public List<TelecomNumber> getOrderAndMainExternalPartyPhoneNumbers() throws RepositoryException {
        if (phoneNumbers == null) {
            phoneNumbers = getRepository().getRelatedPhoneNumbers(this, getMainExternalParty());
        }
        return phoneNumbers;
    }

    /**
     * Gets the list of <code>ContactMechPurposeType</code> for the given <code>ContactMech</code> from this order and the main external party.
     * @param contactMech a <code>ContactMech</code>
     * @return a list of <code>ContactMechPurposeType</code>
     * @throws RepositoryException if an error occurs
     * @see #getOrderAndMainExternalPartyPhoneNumbers
     */
    public List<ContactMechPurposeType> getContactMechPurposeTypesForContactMech(ContactMech contactMech) throws RepositoryException {
        return getRepository().getRelatedContactMechPurposeTypes(contactMech, this, getMainExternalParty());
    }

    /**
     * Gets the shipping origin addresses from this order facility.
     * @return the list of <code>PostalAddress</code> associated to this order origin <code>Facility</code> that has a shipping origin purpose
     * @throws RepositoryException if an error occurs
     */
    public List<PostalAddress> getOriginAddresses() throws RepositoryException {
        if (shippingAddresses == null) {
            shippingAddresses = getRepository().getRelatedFacilityOriginAddresses(this);
        }
        return shippingAddresses;
    }

    /**
     * Gets the shipping addresses for this order.
     * @return the list of <code>PostalAddress</code> associated to the order that has a shipping purpose
     * @throws RepositoryException if an error occurs
     */
    public List<PostalAddress> getShippingAddresses() throws RepositoryException {
        if (shippingAddresses == null) {
            shippingAddresses = getRepository().getRelatedShippingAddresses(this);
        }
        return shippingAddresses;
    }

    /**
     * Gets the billing addresses for this order.
     * @return the list of <code>PostalAddress</code> associated to the order that has a billing purpose
     * @throws RepositoryException if an error occurs
     */
    public List<PostalAddress> getBillingAddresses() throws RepositoryException {
        if (billingAddresses == null) {
            billingAddresses = getRepository().getRelatedBillingAddresses(this);
        }
        return billingAddresses;
    }

    /**
     * Sets the given status to the given list of <code>OrderItem</code>.
     * @param items list of <code>OrderItems</code>
     * @param statusId status to apply
     * @throws RepositoryException if an error occurs
     */
    public void setItemsStatus(final List<OrderItem> items, final String statusId) throws RepositoryException {
        for (OrderItem item : items) {
            getRepository().changeOrderItemStatus(item, statusId);
        }
    }

    /**
     * Gets the sum of all <code>OrderItem</code> sub totals for this order.
     * @return sum of items sub total
     * @throws RepositoryException if an error occurs
     * @see org.opentaps.domain.order.OrderItem#getSubTotal
     */
    public BigDecimal getItemsSubTotal() throws RepositoryException {
        if (itemsSubTotal != null) {
            return itemsSubTotal;
        }

        BigDecimal result = BigDecimal.ZERO;

        for (OrderItem item : getValidItems()) {
            result = result.add(item.getSubTotal());
        }

        itemsSubTotal = result;
        return itemsSubTotal;
    }

    /**
     * Gets the total amount of the non item order adjustments that are neither shipping charges nor tax related for this order.
     * For the two other categories of non item adjustments, see {@link #getShippingAmount} and {@link #getTaxAmount} respectively
     * @return the total amount for the order "other" adjustments
     * @exception RepositoryException if an error occurs
     * @see #getNonItemAdjustments
     */
    public BigDecimal getOtherAdjustmentsAmount() throws RepositoryException {
        if (otherAdjustmentsAmount != null) {
            return otherAdjustmentsAmount;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (OrderAdjustment adj : getNonItemAdjustments()) {
            if (adj.isOther()) {
                result = result.add(adj.calculateAdjustment(this));
            }
        }
        otherAdjustmentsAmount = getOrderSpecification().taxFinalRounding(result);
        return otherAdjustmentsAmount;
    }

    /**
     * Gets the total amount of the non item shipping charges for this order.
     * For the two other categories of non item adjustments, see  {@link #getOtherAdjustmentsAmount} and {@link #getTaxAmount} respectively.
     * @return the total amount for the order shipping charges
     * @exception RepositoryException if an error occurs
     * @see #getNonItemAdjustments
     */
    public BigDecimal getShippingAmount() throws RepositoryException {
        if (shippingAmount != null) {
            return shippingAmount;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (OrderAdjustment adj : getNonItemAdjustments()) {
            if (adj.isShippingCharge()) {
                result = result.add(adj.calculateAdjustment(this));
            }
        }
        for (OrderItem item : getValidItems()) {
            result = result.add(item.getShippingAmount());
        }
        shippingAmount = getOrderSpecification().taxFinalRounding(result);
        Debug.logInfo("Order [" + getOrderId() + "] total shipping amount = " + shippingAmount, MODULE);
        return shippingAmount;
    }

    /**
     * Gets the total amount of the non item tax adjustments for this order.
     * @return the total amount for the order sales tax excluding items sales tax
     * @exception RepositoryException if an error occurs
     * @see #getTaxAmount
     */
    public BigDecimal getGlobalTaxAmount() throws RepositoryException {
        if (globalTaxAmount == null) {
            BigDecimal result = BigDecimal.ZERO;
            for (OrderAdjustment adj : getNonItemAdjustments()) {
                if (adj.isSalesTax()) {
                    BigDecimal amount = adj.calculateAdjustment(this);
                    Debug.logInfo("Order [" + getOrderId() + "] global tax amount component = " + amount, MODULE);
                    result = result.add(amount);
                }
            }
            globalTaxAmount = getOrderSpecification().taxFinalRounding(result);
        }
        return globalTaxAmount;
    }

    /**
     * Gets the total amount of tax adjustments for this order.
     * For tax amount not including the items taxes, see  {@link #getGlobalTaxAmount}.
     * @return the total amount for the order sales tax
     * @exception RepositoryException if an error occurs
     * @see #getGlobalTaxAmount
     */
    public BigDecimal getTaxAmount() throws RepositoryException {
        if (taxAmount == null) {
            BigDecimal result = getGlobalTaxAmount();
            Debug.logInfo("Order [" + getOrderId() + "] global tax amount = " + result, MODULE);
            for (OrderItem item : getValidItems()) {
                result = result.add(item.getTaxAmount());
            }
            taxAmount = getOrderSpecification().taxFinalRounding(result);
            Debug.logInfo("Order [" + getOrderId() + "] total tax amount = " + taxAmount, MODULE);
        }
        return taxAmount;
    }

    /**
     * Gets the total amount for this order.
     * This is the sum of:
     * <ul>
     *  <li>The items sub total {@link #getItemsSubTotal}
     *  <li>The shipping charges {@link #getShippingAmount}
     *  <li>The taxes {@link #getTaxAmount}
     *  <li>The other adjustments {@link #getOtherAdjustmentsAmount}
     * </ul>
     * @return the total for this order
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getTotal() throws RepositoryException {
        if (grandTotal != null) {
            return grandTotal;
        }

        BigDecimal result = BigDecimal.ZERO;
        result = result.add(getItemsSubTotal());
        result = result.add(getTaxAmount());
        result = result.add(getShippingAmount());
        result = result.add(getOtherAdjustmentsAmount());
        grandTotal = result;
        return result;
    }

    /**
     * Gets the open amount for this order.
     * @return the order total minus sum of all payments, net of refunds, for this order
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getOpenAmount() throws RepositoryException {
        if (openAmount != null) {
            return openAmount;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (OrderPaymentPreference pref : getPaymentPreferences()) {
            if (pref.isSettled()) {
                List<PaymentGatewayResponse> responses = pref.getCapturedPaymentResponses();
                for (PaymentGatewayResponse response : responses) {
                    BigDecimal amount = response.getAmount();
                    if (amount != null) {
                        result = result.add(amount);
                    }
                }
                responses = pref.getRefundedPaymentResponses();
                for (PaymentGatewayResponse response : responses) {
                    BigDecimal amount = response.getAmount();
                    if (amount != null) {
                        result = result.subtract(amount);
                    }
                }
            } else if (pref.isReceived()) {
                BigDecimal maxAmount = pref.getMaxAmount();
                if (maxAmount != null) {
                    result = result.add(maxAmount);
                }
            }
        }

        openAmount = getTotal().subtract(result);
        return openAmount;
    }

    /**
     * Gets the amount of received and pending payments for this order.
     * This is used to manage the order payment preferences.
     * @return the order total minus sum of all received and pending order payments for this order
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getTotalMinusPaymentPrefs() throws RepositoryException {
        if (totalMinusPaymentPrefs != null) {
            return totalMinusPaymentPrefs;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (OrderPaymentPreference pref : getPaymentPreferences()) {
            if (pref.isCancelled() || pref.isDeclined()) {
                continue;
            } else if (pref.isSettled()) {
                List<PaymentGatewayResponse> responses = pref.getCapturedPaymentResponses();
                for (PaymentGatewayResponse response : responses) {
                    BigDecimal amount = response.getAmount();
                    if (amount != null) {
                        result = result.add(amount);
                    }
                }
                responses = pref.getRefundedPaymentResponses();
                for (PaymentGatewayResponse response : responses) {
                    BigDecimal amount = response.getAmount();
                    if (amount != null) {
                        result = result.subtract(amount);
                    }
                }
            } else {
                // all others are currently "unprocessed" payment preferences
                BigDecimal maxAmount = pref.getMaxAmount();
                if (maxAmount != null) {
                    result = result.add(maxAmount);
                }
            }
        }

        totalMinusPaymentPrefs = getTotal().subtract(result);
        return totalMinusPaymentPrefs;
    }

    /**
     * Finds the returnable <code>OrderItem</code> for to the given <code>Order</code>.
     * The return info map associated to the <code>OrderItem</code> contains:
     * - returnableQuantity
     * - returnablePrice
     * - itemTypeKey (the product type if applicable, else the order item type)
     * @return a <code>Map</code> associating the returnable <code>OrderItem</code> and a return info <code>Map</code>
     * @throws RepositoryException if an error occurs
     * @see org.ofbiz.order.order.OrderReturnServices#getReturnableItems
     */
    public Map<OrderItem, Map> getReturnableItemsMap() throws RepositoryException {
        if (returnableItemsMap == null) {
            returnableItemsMap = getRepository().getReturnableItemsMap(this);
        }
        return returnableItemsMap;
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
