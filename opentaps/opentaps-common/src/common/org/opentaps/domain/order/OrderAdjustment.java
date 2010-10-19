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

import org.opentaps.foundation.repository.RepositoryException;

import org.opentaps.base.entities.OrderAdjustmentType;

/**
 * Order Adjustment entity.
 */
public class OrderAdjustment extends org.opentaps.base.entities.OrderAdjustment {

    protected static final BigDecimal PERCENT = new BigDecimal(0.01);

    /**
     * Default constructor.
     */
    public OrderAdjustment() {
        super();
    }

    /**
     * Is this order adjustment related to shipping.
     * @return a <code>Boolean</code> value
     */
    public Boolean isShippingCharge() {
        return getOrderSpecification().isShippingCharge(this);
    }

    /**
     * Is this order adjustment related to sales tax.
     * @return a <code>Boolean</code> value
     */
    public Boolean isSalesTax() {
        return getOrderSpecification().isSalesTax(this);
    }

    /**
     * Is this order adjustment related to something other than tax and shipping.
     * @return a <code>Boolean</code> value
     */
    public Boolean isOther() {
        return !isSalesTax() && !isShippingCharge();
    }

    /**
     * Gets the <code>OrderAdjustmentType</code> for this order item.
     * This is an alias for {@link org.opentaps.base.entities.OrderAdjustment#getOrderAdjustmentType}.
     * @return the <code>OrderAdjustmentType</code>
     * @throws RepositoryException if an error occurs
     */
    public OrderAdjustmentType getType() throws RepositoryException {
        return this.getOrderAdjustmentType();
    }

    /**
     * Calculates this adjustment amount when applied to the given <code>OrderItem</code>.
     * @param item the <code>OrderItem</code> to apply this adjustment to
     * @return the calculated amount
     */
    public BigDecimal calculateAdjustment(OrderItem item) {
        BigDecimal adjustment = BigDecimal.ZERO;
        if (getAmount() != null) {
            adjustment = adjustment.add(calculateAmountAdjustment());
        } else if (getSourcePercentage() != null) {
            adjustment = adjustment.add(calculateSourcePercentageAdjustment(item.getOrderedQuantity().multiply(item.getUnitPrice())));
        }
        return getOrderSpecification().taxCalculationRounding(adjustment);
    }

    /**
     * Calculates this adjustment amount when applied to the given <code>Order</code> total.
     * @param orderTotal the total amount for which to calculate this adjustment
     * @return the calculated amount
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal calculateAdjustment(BigDecimal orderTotal) throws RepositoryException {
        BigDecimal adjustment = BigDecimal.ZERO;
        if (getAmount() != null) {
            adjustment = adjustment.add(calculateAmountAdjustment());
        } else if (getSourcePercentage() != null) {
            adjustment = adjustment.add(calculateSourcePercentageAdjustment(orderTotal));
        }
        return getOrderSpecification().taxCalculationRounding(adjustment);
    }

    /**
     * Calculates this adjustment amount when applied to the given <code>Order</code>.
     * @param order the <code>Order</code> to apply this adjustment to
     * @return the calculated amount
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal calculateAdjustment(Order order) throws RepositoryException {
        return calculateAdjustment(order.getItemsSubTotal());
    }

    /**
     * Gets this adjustment amount scaled and rounded as per the specifications.
     * @return the adjustment amount scaled and rounded
     * @throws RepositoryException if an error occurs
     */
    protected BigDecimal calculateAmountAdjustment() {
        return getOrderSpecification().taxCalculationRounding(getAmount());
    }

    /**
     * Calculates this adjustment as a percentage adjustment for the given amount.
     * @return the resulting amount scaled and rounded
     * @param amount the amount to apply  this adjustment to
     * @throws RepositoryException if an error occurs
     */
    protected BigDecimal calculateSourcePercentageAdjustment(BigDecimal amount) {
        // set scale is called twice here as it was in OrderReadHelper, because db value of 0.825 is pulled as 0.8249999...
        return getOrderSpecification().taxCalculationRounding(getOrderSpecification().taxCalculationRounding(getSourcePercentage()).multiply(amount).multiply(PERCENT));
    }

    /**
     * Get the specification object which contains enumerations and logical checking for Orders.
     * @return the <code>OrderSpecificationInterface</code>
     */
    public OrderSpecificationInterface getOrderSpecification() {
        return getRepository().getOrderSpecification();
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
