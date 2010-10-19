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
package org.opentaps.domain.billing.payment;

import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.order.OrderSpecificationInterface;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Credit Card entity.
 */
public class CreditCard extends org.opentaps.base.entities.CreditCard {

    /**
     * Default constructor.
     */
    public CreditCard() {
        super();
    }

    /**
     * Gets a stripped version of this credit card number for safety.
     * This actually only displays the last few numbers.
     * @return the stripped number
     * @exception RepositoryException if an error occurs
     */
    public String getCardNumberStripped() throws RepositoryException {
        String cardNumber = getCardNumber();
        if (cardNumber != null && cardNumber.length() > getOrderSpecification().cardsStrippedNumberLength()) {
            return cardNumber.substring(cardNumber.length() - getOrderSpecification().cardsStrippedNumberLength());
        }
        return "";
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
            repository = DomainsDirectory.getDomainsDirectory(repository).getOrderDomain().getOrderRepository();
            return OrderRepositoryInterface.class.cast(repository);
        }
    }
}
