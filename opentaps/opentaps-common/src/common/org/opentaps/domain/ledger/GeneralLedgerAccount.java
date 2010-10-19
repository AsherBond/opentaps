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

import org.ofbiz.base.util.UtilMisc;
import org.opentaps.base.entities.*;
import org.opentaps.domain.ledger.LedgerException;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;

import java.math.BigDecimal;
import java.util.List;

/**
 * General Ledger Account
 */
public class GeneralLedgerAccount extends org.opentaps.base.entities.GlAccount {

    protected Boolean isDebitAccount = null;
    protected List<GlAccountClass> accountClasses = null;

    public GeneralLedgerAccount() {
        super();
    }

    public LedgerRepositoryInterface getRepository() {
        return (LedgerRepositoryInterface) repository;
    }

    /**
     * Returns whether the account class is a member of the DEBIT
     * class hierarchy.  This calculation is done only once and cached for
     * future reference.
     *
     * If the parent class is not a debit or credit account, throws
     * a LedgerException announcing a misconfiguration.
     */
    public boolean isDebitAccount() throws RepositoryException, LedgerException {
        if (isDebitAccount == null) {
            if (accountClasses == null) {
                accountClasses = getRepository().getAccountClassTree(getGlAccountClassId());
            }
            for (GlAccountClass accountClass : accountClasses) {
                if ("DEBIT".equals(accountClass.getGlAccountClassId())) {
                    isDebitAccount = Boolean.TRUE;
                    break;
                }
                if ("CREDIT".equals(accountClass.getGlAccountClassId())) {
                    isDebitAccount = Boolean.FALSE;
                    break;
                }
            }
            if (isDebitAccount == null) {
                throw new LedgerException("FinancialsError_GLAccountClassNotConfigured", UtilMisc.toMap("glAccountClassId", getGlAccountClassId()));
            }
        }
        return isDebitAccount;
    }

    /**
     * This is simply a call to isDebitAccount with the boolean reversed.
     */
    public boolean isCreditAccount() throws RepositoryException, LedgerException {
        return ! isDebitAccount();
    }

    /**
     * Returns the entry amount normalized according to the following rules:
     * Debit entries add to debit accounts and subtract from credit accounts.
     * Credit entries add to credit accounts and subtract from debit accounts.
     */
    public BigDecimal getNormalizedAmount(AcctgTransEntry entry) throws RepositoryException, LedgerException {
        String debitCreditFlag = entry.getDebitCreditFlag();
        if (! ("D".equals(debitCreditFlag) || "C".equals(debitCreditFlag))) {
            throw new LedgerException("FinancialsError_BadDebitCreditFlag", entry.toMap());
        }
        if (isDebitAccount()) {
            if ("D".equals(entry.getDebitCreditFlag())) {
                return entry.getAmount();
            } else {
                return BigDecimal.ZERO.subtract(entry.getAmount());
            }
        } else { // credit account
            if ("C".equals(entry.getDebitCreditFlag())) {
                return entry.getAmount();
            } else {
                return BigDecimal.ZERO.subtract(entry.getAmount());
            }
        }
    }

}
