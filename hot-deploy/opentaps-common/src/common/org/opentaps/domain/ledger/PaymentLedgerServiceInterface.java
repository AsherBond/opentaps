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
package org.opentaps.domain.ledger;

import org.opentaps.foundation.service.ServiceInterface;
import org.opentaps.foundation.service.ServiceException;

import java.math.BigDecimal;

/**
 * Interface for payment ledger services.
 */
public interface PaymentLedgerServiceInterface extends ServiceInterface {

    /**
     * Sets the payment ID, required parameter for {@link #reconcileParentSubAccountPayment}.
     * @param paymentId the payment ID
     */
    public void setPaymentId(String paymentId);

    /**
     * Creates accounting transactions to offset balances in the sub account with that of the parent account
     * when the payment is a payment from a parent account applied to the invoice of a sub-account
     * @throws ServiceException if an error occurs
     * @see #setPaymentId required input <code>paymentId</code>
     */
    public void reconcileParentSubAccountPayment() throws ServiceException;

}
