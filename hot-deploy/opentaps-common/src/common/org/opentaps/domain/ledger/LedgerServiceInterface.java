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

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;

/**
 * Interface for ledger services.
 */
public interface LedgerServiceInterface extends ServiceInterface {

    /**
     * Sets the accounting transaction ID, required parameter for {@link #postAcctgTrans}.
     * @param acctgTransId the accounting transaction ID
     */
    public void setAcctgTransId(String acctgTransId);

    /**
     * Service to post a transaction to the ledger.
     * Uses the acctgTransId to find the transaction.
     * @throws ServiceException if an error occurs
     * @see #setAcctgTransId required input <code>acctgTransId</code>
     */
    public void postAcctgTrans() throws ServiceException;

    /**
     * Posts an accounting transaction to the ledger.
     * @param transaction an <code>AccountingTransaction</code> value
     * @throws LedgerException if an error occurs
     * @throws RepositoryException if an error occurs
     */
    public void postToLedger(AccountingTransaction transaction) throws LedgerException, RepositoryException;

    /**
     * Updates balances of account for the transaction, including balances during all applicable accounting time periods.
     * @param account a <code>GeneralLedgerAccount</code> value
     * @param transaction an <code>AccountingTransaction</code> value
     * @param orgRepository an <code>OrganizationRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     * @throws LedgerException if an error occurs
     */
    public void updateBalanceForTransaction(GeneralLedgerAccount account, AccountingTransaction transaction, OrganizationRepositoryInterface orgRepository) throws RepositoryException, LedgerException;

}
