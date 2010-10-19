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

import org.opentaps.base.entities.CustomTimePeriod;
import org.opentaps.base.entities.AcctgTransEntry;
import org.opentaps.domain.billing.invoice.Invoice;

/**
 * Common specifications for the Ledger domain.
 *
 * These specifications contain mapping of conceptual status to their database equivalents, as well as groupings into higher level concepts.
 *
 * This class may be expanded to include other order domain validation code.
 */
public interface LedgerSpecificationInterface {

    /**
     * Gets the debit flag on ledger transactions.
     * @return the <code>String</code> value for the debit flag
     */
    public String getDebitFlag();

    /**
     * Gets the credit flag on ledger transactions.
     * @return the <code>String</code> value for the credit flag
     */
    public String getCreditFlag();

    /**
     * Gets the adjustment type id corresponding to a write-off.
     * @return a <code>String</code> value
     */
    public String getAdjustmentTypeIdForWriteOff();

    /**
     * Gets the adjustment type id corresponding to a cash discount.
     * @return a <code>String</code> value
     */
    public String getAdjustmentTypeIdForCashDiscount();

    /**
     * Gets the adjustment type id corresponding to an early pay discount.
     * @return a <code>String</code> value
     */
    public String getAdjustmentTypeIdForEarlyPayDiscount();

    /**
     * Checks if the invoice is posted.
     * @param invoice an <code>Invoice</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isPosted(Invoice invoice);

    /**
     * Checks if the transaction has been posted to the ledger.
     * @param transaction an <code>AccountingTransaction</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isPosted(AccountingTransaction transaction);

    /**
     * Checks if the transaction entry is a debit.
     * @param entry an <code>AcctgTransEntry</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isDebit(AcctgTransEntry entry);

    /**
     * Checks if the transaction entry is a credit.
     * @param entry an <code>AcctgTransEntry</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isCredit(AcctgTransEntry entry);

    /**
     * Checks if the time period is closed.
     * @param timePeriod an <code>CustomTimePeriod</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isClosed(CustomTimePeriod timePeriod);

    /**
     * Gets the fiscal type id for actual.
     * @return a <code>String</code> value
     */
    public String getFiscalTypeIdForActual();

    /**
     * Gets the status for transactions that are not reconciled.
     * @return a <code>String</code> value
     */
    public String getStatusNotReconciled();

    /**
     * Checks if the transaction should be automatically posted to the ledger.
     * This can be configured on a per transaction basis.
     * @param acctgTrans an <code>AccountingTransaction</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isAutoPostToLedger(AccountingTransaction acctgTrans);
}
