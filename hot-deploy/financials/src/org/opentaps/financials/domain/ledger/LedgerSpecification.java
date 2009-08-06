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
package org.opentaps.financials.domain.ledger;

import org.opentaps.domain.ledger.LedgerSpecificationInterface;
import org.opentaps.domain.ledger.AccountingTransaction;
import org.opentaps.domain.base.entities.CustomTimePeriod;
import org.opentaps.domain.base.entities.AcctgTransEntry;
import org.opentaps.domain.billing.invoice.Invoice;

/** {@inheritDoc} */
public class LedgerSpecification implements LedgerSpecificationInterface {

    /** {@inheritDoc} */
    public String getDebitFlag() {
        return "D";
    }

    /** {@inheritDoc} */
    public String getCreditFlag() {
        return "C";
    }

    /** {@inheritDoc} */
    public String getAdjustmentTypeIdForWriteOff() {
        return "WRITEOFF";
    }

    /** {@inheritDoc} */
    public String getAdjustmentTypeIdForCashDiscount() {
        return "CASH_DISCOUNT";
    }

    /** {@inheritDoc} */
    public String getAdjustmentTypeIdForEarlyPayDiscount() {
        return "EARLY_PAY_DISCT";
    }

    /** {@inheritDoc} */
    public boolean isPosted(Invoice invoice) {
        return !(invoice.isInProcess() || invoice.isCancelled());
    }

    /** {@inheritDoc} */
    public boolean isPosted(AccountingTransaction transaction) {
        return "Y".equals(transaction.getIsPosted());
    }

    /** {@inheritDoc} */
    public boolean isDebit(AcctgTransEntry entry) {
        return getDebitFlag().equals(entry.getDebitCreditFlag());
    };

    /** {@inheritDoc} */
    public boolean isCredit(AcctgTransEntry entry) {
        return getCreditFlag().equals(entry.getDebitCreditFlag());
    };

    /** {@inheritDoc} */
    public boolean isClosed(CustomTimePeriod timePeriod) {
        return "Y".equals(timePeriod.getIsClosed());
    }

    /** {@inheritDoc} */
    public String getFiscalTypeIdForActual() {
        return "ACTUAL";
    }

    /** {@inheritDoc} */
    public String getStatusNotReconciled() {
        return "AES_NOT_RECONCILED";
    }

    /** {@inheritDoc} */
    public boolean isAutoPostToLedger(AccountingTransaction acctgTrans) {
        return acctgTrans.isAutoPost();
    }
}
