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
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.opentaps.base.entities.ItemIssuance;
import org.opentaps.base.entities.OrderAdjustmentBilling;
import org.opentaps.base.entities.OrderItemAssoc;
import org.opentaps.base.entities.OrderItemBilling;
import org.opentaps.base.entities.OrderItemPriceInfo;
import org.opentaps.base.entities.OrderItemShipGroupAssoc;
import org.opentaps.base.entities.OrderItemType;
import org.opentaps.base.entities.OrderRequirementCommitment;
import org.opentaps.base.entities.OrderStatus;
import org.opentaps.base.entities.StatusItem;
import org.opentaps.domain.product.Product;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Order Item entity.
 * An order item is an item of an <code>Order</code>.
 * @see Order
 */
public class OrderItem extends org.opentaps.base.entities.OrderItem {

    private static final String MODULE = OrderItem.class.getName();

    private Product product;
    private List<OrderStatus> statuses;
    private List<OrderAdjustment> adjustments;
    private List<OrderItemShipGrpInvRes> shipGroupInventoryReservations;
    private List<ReturnItem> returnItems;
    private List<OrderItemAssoc> orderItemAssocsTo;
    private List<OrderItemAssoc> orderItemAssocsFrom;
    private BigDecimal quantity;
    private BigDecimal cancelQuantity;
    private BigDecimal otherAdjustmentsAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal subTotal;
    private BigDecimal orderedQuantity;
    private BigDecimal shippedQuantity;
    private BigDecimal invoicedQuantity;
    private BigDecimal invoicedValue;
    private BigDecimal reservedQuantity;
    private BigDecimal shortfalledQuantity;
    private BigDecimal returnedQuantity;

    /**
     * Default constructor.
     */
    public OrderItem() {
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
     * Gets the <code>OrderRequirementCommitments</code> for this order item.
     * This is an alias for {@link org.opentaps.base.entities.OrderItem#getOrderRequirementCommitments}.
     * @return the list of <code>OrderRequirementCommitments</code>
     * @throws RepositoryException if an error occurs
     */
    public List<? extends OrderRequirementCommitment> getRequirementCommitments() throws RepositoryException {
        return this.getOrderRequirementCommitments();
    }

    /**
     * Gets the <code>OrderItemAssocs</code> linked to this order item.
     * @return the list of <code>OrderItemAssoc</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemAssoc> getItemAssocsTo() throws RepositoryException {
        if (orderItemAssocsTo == null) {
            orderItemAssocsTo = getRepository().getRelatedOrderItemAssocsTo(this);
        }
        return orderItemAssocsTo;
    }

    /**
     * Gets the <code>OrderItemAssocs</code> linked from this order item.
     * @return the list of <code>OrderItemAssoc</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemAssoc> getItemAssocsFrom() throws RepositoryException {
        if (orderItemAssocsFrom == null) {
            orderItemAssocsFrom = getRepository().getRelatedOrderItemAssocsFrom(this);
        }
        return orderItemAssocsFrom;
    }

    /**
     * Gets the <code>OrderItemBillings</code> for this order item.
     * This is an alias for {@link org.opentaps.base.entities.OrderItem#getOrderItemBillings}.
     * @return the list of <code>OrderItemBilling</code>
     * @throws RepositoryException if an error occurs
     */
    public List<? extends OrderItemBilling> getBillings() throws RepositoryException {
        return this.getOrderItemBillings();
    }

    /**
     * Gets the adjustments for this order item.
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
     * Gets the adjustments for this order item.
     * This is an alias for {@link #getOrderAdjustments}.
     * @return list of <code>OrderAdjustment</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderAdjustment> getAdjustments() throws RepositoryException {
        return this.getOrderAdjustments();
    }

    /**
     * Gets the list of <code>OrderItemPriceInfo</code> for this order item.
     * This is an alias for {@link org.opentaps.base.entities.OrderItem#getOrderItemPriceInfoes}.
     * @return the list of <code>OrderItemPriceInfo</code>
     * @throws RepositoryException if an error occurs
     */
    public List<? extends OrderItemPriceInfo> getPriceInfos() throws RepositoryException {
        return this.getOrderItemPriceInfoes();
    }

    /**
     * Gets the list of <code>OrderItemShipGrpInvRes</code> for this order item.
     * Returns the order domain object instead of the base entity.
     * @return list of <code>OrderItemShipGrpInvRes</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<OrderItemShipGrpInvRes> getOrderItemShipGrpInvReses() throws RepositoryException {
        if (shipGroupInventoryReservations == null) {
            shipGroupInventoryReservations = getRelated(OrderItemShipGrpInvRes.class, "OrderItemShipGrpInvRes");
        }
        return shipGroupInventoryReservations;
    }

    /**
     * Gets the <code>OrderItemShipGrpInvRes</code> for this order item.
     * Returns the order domain object instead of the base entity.
     * This is an alias for {@link #getOrderItemShipGrpInvReses}.
     * @return list of <code>OrderItemShipGrpInvRes</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemShipGrpInvRes> getShipGroupInventoryReservations() throws RepositoryException {
        return this.getOrderItemShipGrpInvReses();
    }

    /**
     * Gets the <code>Product</code> for this order item.
     * Returns the product domain object instead of the base entity.
     * @return the <code>Product</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public Product getProduct() throws RepositoryException {
        if (product == null) {
            product = getRepository().getRelatedProduct(this);
        }
        return product;
    }

    /**
     * Gets the <code>OrderItemType</code> for this order item.
     * This is an alias for {@link org.opentaps.base.entities.OrderItem#getOrderItemType}.
     * @return the <code>OrderItemType</code>
     * @throws RepositoryException if an error occurs
     */
    public OrderItemType getType() throws RepositoryException {
        return this.getOrderItemType();
    }

    /**
     * Gets this order item current <code>StatusItem</code>.
     * This is an alias for {@link org.opentaps.base.entities.OrderItem#getStatusItem}.
     * @return the current <code>StatusItem</code>
     * @throws RepositoryException if an error occurs
     * @see #getOrderStatuses
     */
    public StatusItem getStatus() throws RepositoryException {
        return this.getStatusItem();
    }

    /**
     * Gets the order statuses for this order item.
     * This list describe the history of status this order item went through, with
     *  the current status being the first of the list.
     * This overrides {@link org.opentaps.base.entities.OrderHeader#getOrderStatuses} to specify the ordering.
     * @return list of <code>OrderStatus</code> from current to oldest, that relate this order item to a <code>StatusItem</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<OrderStatus> getOrderStatuses() throws RepositoryException {
        if (statuses == null) {
            statuses = getRepository().getRelatedOrderStatuses(this);
        }
        return statuses;
    }

    /**
     * Gets the <code>ReturnItems</code> for this order item.
     * Returns the order domain objects instead of the base entity.
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
     * Is this order item product physical.
     * @return is the <code>Product</code> related to this order item physical, if no <code>Product</code> is associated, returns <code>null</code>
     * @throws RepositoryException if an error occurs
     */
    public Boolean isPhysical() throws RepositoryException {
        if (getProduct() == null) {
            return null;
        }
        return getProduct().isPhysical();
    }

    /**
     * Is this order item status "cancelled".
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isCancelled() throws RepositoryException {
        return getOrderSpecification().isCancelled(this);
    }

    /**
     * Is this order item status "rejected".
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isRejected() throws RepositoryException {
        return getOrderSpecification().isRejected(this);
    }

    /**
     * Is this order item status "completed".
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isCompleted() throws RepositoryException {
        return getOrderSpecification().isCompleted(this);
    }

    /**
     * Is this order item a promo item.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isPromo() throws RepositoryException {
        return getOrderSpecification().isPromo(this);
    }

    /**
     * Override the base class accessor and return 0 instead of null.
     * @return the quantity for this order item
     */
    @Override
    public BigDecimal getQuantity() {
        if (quantity == null) {
            quantity = super.getQuantity();
            if (quantity == null) {
                quantity = BigDecimal.ZERO;
            }
        }

        return quantity;
    }

    /**
     * Override the base class accessor and return 0 instead of null.
     * @return the cancelled quantity for this order item
     */
    @Override
    public BigDecimal getCancelQuantity() {
        if (cancelQuantity == null) {
            cancelQuantity = super.getCancelQuantity();
            if (cancelQuantity == null) {
               cancelQuantity  = BigDecimal.ZERO;
            }
        }

        return cancelQuantity;
    }

    /**
     * Gets the actual quantity ordered for this order item.
     * This quantity is (item quantity - cancelled quantity).
     * @return the actual quantity ordered
     */
    public BigDecimal getOrderedQuantity() {
        if (orderedQuantity != null) {
            return orderedQuantity;
        }

        BigDecimal result = this.getQuantity();
        if (this.getCancelQuantity() != null) {
            result = result.subtract(this.getCancelQuantity());
        }
        orderedQuantity = result;
        return result;
    }

    /**
     * Gets the actual quantity ordered for this order item in the given ship group.
     * This quantity is (item quantity - cancelled quantity).
     * @param shipGroup the ship group
     * @return the actual quantity ordered
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getOrderedQuantity(OrderItemShipGroup shipGroup) throws RepositoryException {
        BigDecimal qty = BigDecimal.ZERO;
        if (!isCancelled()) {
            for (OrderItemShipGroupAssoc assoc : shipGroup.getOrderItemShipGroupAssocs()) {
                if (getOrderItemSeqId().equals(assoc.getOrderItemSeqId())) {
                    qty = qty.add(assoc.getQuantity());
                    BigDecimal canceled = assoc.getCancelQuantity();
                    if (canceled != null) {
                        qty = qty.subtract(canceled);
                    }
                }
            }
        }
        return qty;
    }

    /**
     * Gets the number of reserved items.
     * @return the number of reserved items
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getReservedQuantity() throws RepositoryException {
        if (reservedQuantity != null) {
            return reservedQuantity;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (OrderItemShipGrpInvRes res : getShipGroupInventoryReservations()) {
            BigDecimal qty = res.getQuantity();
            if (qty != null) {
                result = result.add(qty);
            }
        }
        reservedQuantity = result;
        return result;
    }

    /**
     * Gets the number of reserved items for which the quantity is not available yet.
     * @return the number of reserved but not available items
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getShortfalledQuantity() throws RepositoryException {
        if (shortfalledQuantity != null) {
            return shortfalledQuantity;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (OrderItemShipGrpInvRes res : getShipGroupInventoryReservations()) {
            BigDecimal qty = res.getQuantityNotAvailable();
            if (qty != null) {
                result = result.add(qty);
            }
        }
        shortfalledQuantity = result;
        return result;
    }

    /**
     * Gets the number of returned items.
     * @return the number of returned items
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getReturnedQuantity() throws RepositoryException {
        if (returnedQuantity != null) {
            return returnedQuantity;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (ReturnItem returnItem : getReturnItems()) {
            if (!"RETURN_CANCELLED".equals(returnItem.getStatusId())) {
                result = result.add(returnItem.getReturnQuantity());
            }
        }
        returnedQuantity = result;
        return result;
    }

    /**
     * Gets the number of shipped items.
     * @return  the number of shipped items
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getShippedQuantity() throws RepositoryException {
        if (shippedQuantity != null) {
            return shippedQuantity;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (ItemIssuance issue : getItemIssuances()) {
            BigDecimal issueQty = issue.getQuantity();
            if (issueQty != null) {
                result = result.add(issueQty);
            }
        }
        shippedQuantity = result;
        return result;
    }

    /**
     * Gets the number of shipped items in the given ship group.
     * @param shipGroup the ship group
     * @return  the number of shipped items
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getShippedQuantity(OrderItemShipGroup shipGroup) throws RepositoryException {
        BigDecimal qty = BigDecimal.ZERO;
        for (ItemIssuance issue : getItemIssuances()) {
            if (shipGroup.getShipGroupSeqId().equals(issue.getShipGroupSeqId())) {
                BigDecimal issueQty = issue.getQuantity();
                if (issueQty != null) {
                    qty = qty.add(issueQty);
                }
            }
        }
        return qty;
    }

    /**
     * Gets the number of items that should still be shipped.
     * This number is (item quantity - cancelled quantity - shipped quantity).
     * This number is always positive, even if the shipped quantity exceed the ordered quantity.
     * If the item is not physical, this value is zero.
     * @return the number of items that should still be shipped
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getRemainingToShipQuantity() throws RepositoryException {
        if (!isPhysical()) {
            return BigDecimal.ZERO;
        } else {
            BigDecimal result = this.getOrderedQuantity().subtract(this.getShippedQuantity());
            return result.max(BigDecimal.ZERO);
        }
    }

    /**
     * Gets the number of items already invoiced for this order item.
     * TODO: This doesn't account for when an invoice gets canceled or written off.
     * @return the number of items already invoiced
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getInvoicedQuantity() throws RepositoryException {
        if (invoicedQuantity != null) {
            return invoicedQuantity;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (OrderItemBilling billing : getBillings()) {
            result = result.add(billing.getQuantity());
        }
        invoicedQuantity = result;
        return result;
    }

    /**
     * Adds up the invoiced value, which is the total of the order item's billings to invoice (OrderItemBilling) and its
     * adjustments' billing to invoice (OrderAdjustmentBiling).
     * @return the invoiced value for this order item
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getInvoicedValue() throws RepositoryException {
        if (invoicedValue != null) {
            return invoicedValue;
        }

        BigDecimal result = BigDecimal.ZERO;
        // first add up all the OrderItemBilling.  It is quantity * amount, and we assume default quantity of 1
        for (OrderItemBilling billing : getBillings()) {
            BigDecimal billingQuantity = billing.getQuantity();
            if (billingQuantity == null) {
                billingQuantity = new BigDecimal("1.0");
            }
            result = result.add(billingQuantity.multiply(billing.getAmount()));
        }

        // now get all the OrderAdjustments, and for each one add its OrderAdjustmentBilling
        for (OrderAdjustment adjustment : getAdjustments()) {
            List<? extends OrderAdjustmentBilling> adjustmentBillings = adjustment.getOrderAdjustmentBillings();
            for (OrderAdjustmentBilling adjustmentBilling : adjustmentBillings) {
                result = result.add(adjustmentBilling.getAmount());
            }
        }
        invoicedValue = result;
        return result;
    }

    /**
     * This is the total value of the order item that has not been invoiced, which is
     * subtotal + tax amount + shipping - invoiced value.
     * @return the total value of the order item that has not been invoiced
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getUninvoicedValue() throws RepositoryException {
        return getSubTotal().add(getTaxAmount()).add(getShippingAmount()).subtract(getInvoicedValue());
    }

    /**
     * Gets the sub total for this order item.
     * The sub total of an item is the unit price by the quantity actually ordered
     * to which is added the items adjustments that are not tax or shipping related.
     * @return the sub total
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getSubTotal() throws RepositoryException {
        if (subTotal == null) {
            if (isCancelled()) {
                subTotal = BigDecimal.ZERO;
            } else {
                BigDecimal result = getUnitPrice().multiply(getOrderedQuantity());
                subTotal = getOrderSpecification().defaultRounding(result.add(getOtherAdjustmentsAmount()));
            }
        }
        Debug.logInfo("Item [" + getOrderItemSeqId() + "] subtotal = " + subTotal + ", adjustments = " + getOtherAdjustmentsAmount(), MODULE);
        return subTotal;
    }

    /**
     * Gets the total of adjustments that are not tax or shipping related for this order item.
     * @return the other adjustments amount
     * @throws RepositoryException if an error occurs
     * @see #getShippingAmount
     * @see #getTaxAmount
     */
    public BigDecimal getOtherAdjustmentsAmount() throws RepositoryException {
        if (otherAdjustmentsAmount != null) {
            return otherAdjustmentsAmount;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (OrderAdjustment adj : getAdjustments()) {
            if (adj.isOther()) {
               result = result.add(adj.calculateAdjustment(this));
            }
        }
        otherAdjustmentsAmount = getOrderSpecification().taxFinalRounding(result);
        return result;
    }

    /**
     * Gets the shipping adjustments total for this order item.
     * @return the shipping amount
     * @throws RepositoryException if an error occurs
     * @see #getOtherAdjustmentsAmount
     * @see #getTaxAmount
     */
    public BigDecimal getShippingAmount() throws RepositoryException {
        if (shippingAmount != null) {
            return shippingAmount;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (OrderAdjustment adj : getAdjustments()) {
            if (adj.isShippingCharge()) {
                result = result.add(adj.calculateAdjustment(this));
            }
        }
        shippingAmount = getOrderSpecification().taxFinalRounding(result);
        return result;
    }

    /**
     * Gets the tax adjustments total for this order item.
     * @return the tax amount
     * @throws RepositoryException if an error occurs
     * @see #getOtherAdjustmentsAmount
     * @see #getShippingAmount
     */
    public BigDecimal getTaxAmount() throws RepositoryException {
        if (taxAmount == null) {
            BigDecimal result = BigDecimal.ZERO;
            for (OrderAdjustment adj : getAdjustments()) {
                if (adj.isSalesTax()) {
                    BigDecimal taxComponent = adj.calculateAdjustment(this);
                    Debug.logInfo("Item [" + getOrderItemSeqId() + "] tax component for [" + adj.getTaxAuthGeoId() + "/" + adj.getTaxAuthPartyId() + "] = " + taxComponent, MODULE);
                    result = result.add(taxComponent);
                }
            }
            Debug.logInfo("Item [" + getOrderItemSeqId() + "] not rounded tax amount = " + result, MODULE);
            taxAmount = getOrderSpecification().taxFinalRounding(result);
        }
        Debug.logInfo("Item [" + getOrderItemSeqId() + "] tax amount = " + taxAmount, MODULE);
        return taxAmount;
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
