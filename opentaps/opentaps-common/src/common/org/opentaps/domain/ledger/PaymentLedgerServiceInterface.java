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
