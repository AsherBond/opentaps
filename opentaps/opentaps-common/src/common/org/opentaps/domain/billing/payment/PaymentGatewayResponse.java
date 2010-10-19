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

import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.base.entities.Enumeration;
import org.opentaps.domain.order.OrderRepositoryInterface;

/**
 * Payment Gateway Response entity.
 */
public class PaymentGatewayResponse extends org.opentaps.base.entities.PaymentGatewayResponse {

    private Enumeration transactionCode;

    /**
     * Default constructor.
     */
    public PaymentGatewayResponse() {
        super();
    }

    /**
     * Gets the transaction code <code>Enumeration</code>.
     * @return the transaction code
     * @throws RepositoryException if an error occurs
     */
    public Enumeration getTransactionCode() throws RepositoryException {
        if (transactionCode == null) {
            transactionCode = getRepository().getRelatedTransactionCode(this);
        }
        return transactionCode;
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
