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

import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.domain.base.entities.Enumeration;
import org.opentaps.domain.order.OrderRepositoryInterface;

/**
 * Payment Gateway Response entity.
 */
public class PaymentGatewayResponse extends org.opentaps.domain.base.entities.PaymentGatewayResponse {

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
