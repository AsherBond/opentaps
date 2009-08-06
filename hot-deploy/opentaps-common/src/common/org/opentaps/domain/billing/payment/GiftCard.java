/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.billing.payment;

import org.opentaps.domain.order.OrderSpecificationInterface;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Gift Card entity.
 */
public class GiftCard extends org.opentaps.domain.base.entities.GiftCard {

    /**
     * Default constructor.
     */
    public GiftCard() {
        super();
    }

    /**
     * Gets a stripped version of this credit card number for safety.
     * This actually only show the last few numbers, the other numbers being starred.
     * @return the stripped number
     * @exception RepositoryException if an error occurs
     */
    public String getCardNumberStripped() throws RepositoryException {
        String cardNumber = getCardNumber();
        if (cardNumber != null && cardNumber.length() > getOrderSpecification().cardsStrippedNumberLength()) {
            StringBuffer result = new StringBuffer();
            for (int i = getOrderSpecification().cardsStrippedNumberLength(); i < cardNumber.length(); i++) {
                result.append("*");
            }

            return result.append(cardNumber.substring(cardNumber.length() - getOrderSpecification().cardsStrippedNumberLength())).toString();
        }
        return cardNumber;
    }

    /**
     * Get the specification object which contains enumerations and logical checking for Orders.
     * @return the <code>OrderSpecificationInterface</code>
     * @exception RepositoryException if an error occurs
     */
    public OrderSpecificationInterface getOrderSpecification() throws RepositoryException {
        return getRepository().getOrderSpecification();
    }

    private OrderRepositoryInterface getRepository() throws RepositoryException {
        try {
            return OrderRepositoryInterface.class.cast(repository);
        } catch (ClassCastException e) {
            repository = repository.getDomainsDirectory().getOrderDomain().getOrderRepository();
            return OrderRepositoryInterface.class.cast(repository);
        }
    }
}
