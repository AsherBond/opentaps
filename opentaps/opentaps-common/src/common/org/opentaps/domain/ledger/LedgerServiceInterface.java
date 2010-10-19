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
     * Sets if the service should skip the checks on the accounting tags, optional parameter for {@link #postAcctgTrans}.
     * @param skipCheckAcctgTags an indicator <code>String</code> value
     */
    public void setSkipCheckAcctgTags(String skipCheckAcctgTags);

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
    
    /**
     * Finds all AcctgTrans where isPosted=Y and sets the postedAmount to the sum of all the debit amounts for the accounting transaction.
     * @throws ServiceException if an error occurs
     * @see #setPaymentId required input <code>paymentId</code>
     */
    public void updatePostedAmountAcctgTrans() throws ServiceException;

}
