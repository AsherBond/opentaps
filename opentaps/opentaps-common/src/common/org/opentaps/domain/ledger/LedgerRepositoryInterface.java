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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.opentaps.base.entities.*;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;
import org.opentaps.foundation.service.ServiceException;

/**
 * Repository for the Ledger domain.
 */
public interface LedgerRepositoryInterface extends RepositoryInterface {

    /**
     * Returns the ledger specifications for literal values and logical tests around ledger.
     * @return a <code>LedgerSpecificationInterface</code> value
     */
    public LedgerSpecificationInterface getSpecification();

    /**
     * Finds a <code>GlAccountOrganization</code> by ID from the database.
     * @param glAccountId the <code>GlAccount</code> ID value
     * @param organizationPartyId the <code>Organization</code> ID value
     * @return a <code>GlAccountOrganization</code> value
     * @throws RepositoryException if an error occurs, or if no <code>GlAccountOrganization</code> is found
     */
    public GlAccountOrganization getOrganizationAccount(String glAccountId, String organizationPartyId) throws RepositoryException;

    /**
     * Finds a <code>GeneralLedgerAccount</code> by ID from the database.
     * @param glAccountId the <code>GlAccount</code> ID value
     * @param organizationPartyId the <code>Organization</code> ID value
     * @return a <code>GeneralLedgerAccount</code> value, or <code>null</code> if no <code>GeneralLedgerAccount</code> is found
     * @throws RepositoryException if an error occurs
     */
    public GeneralLedgerAccount getLedgerAccount(String glAccountId, String organizationPartyId) throws RepositoryException;

    /**
     * Finds the default <code>GeneralLedgerAccount</code> for the given type and organization.
     *
     * @param glAccountTypeId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @return a <code>GeneralLedgerAccount</code> value
     * @exception RepositoryException if an error occurs
     * @exception EntityNotFoundException if an error occurs
     */
    public GeneralLedgerAccount getDefaultLedgerAccount(String glAccountTypeId, String organizationPartyId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds the GL Account for a product or the default account type based on input. This replaces the simple-method service
     * getProductOrgGlAccount. First it will look in ProductGlAccount using the primary keys productId and
     * productGlAccountTypeId. If none is found, it will look up GlAccountTypeDefault to find the default account for
     * organizationPartyId with type glAccountTypeId.
     *
     * @param productId a <code>String</code> value
     * @param glAccountTypeId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @return a <code>GeneralLedgerAccount</code> value
     * @exception RepositoryException if an error occurs
     * @exception EntityNotFoundException if an error occurs
     */
    public GeneralLedgerAccount getProductLedgerAccount(String productId, String glAccountTypeId, String organizationPartyId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds an <code>AccountingTransaction</code> by ID from the database.
     * @param acctgTransId the accounting transaction ID
     * @return an <code>AccountingTransaction</code> value
     * @throws RepositoryException if an error occurs, or if no <code>AccountingTransaction</code> is found
     */
    public AccountingTransaction getAccountingTransaction(String acctgTransId) throws RepositoryException;

    /**
     * Finds a transaction entry by ID.
     * @param acctgTransId the accounting transaction ID
     * @param acctgTransEntrySeqId the accounting transaction entry ID
     * @return the <code>AcctgTransEntry</code> found
     * @throws RepositoryException if an error occurs or if no <code>AcctgTransEntry</code> is found
     */
    public AcctgTransEntry getTransactionEntry(String acctgTransId, String acctgTransEntrySeqId) throws RepositoryException;

    /**
     * Finds the transaction entries for a transaction ordered by their sequence.
     * @param acctgTransId the accounting transaction ID
     * @return a list of <code>AcctgTransEntry</code>
     * @throws RepositoryException if an error occurs
     */
    public List<AcctgTransEntry> getTransactionEntries(String acctgTransId) throws RepositoryException;

    /**
     * Returns a flat list of account classes that the ledger account is a member of.
     * Normally this is a tree structure, but we flatten it out here.
     * @param glAccountClassId the ID if the starting <code>GlAccountClass</code>
     * @return an <code>AccountingTransaction</code> value
     * @throws RepositoryException if an error occurs, or if the starting <code>GlAccountClass</code> was not found
     */
    public List<GlAccountClass> getAccountClassTree(String glAccountClassId) throws RepositoryException;

    /**
     * Creates a simple transaction consisting of two transaction entries representing a transfer between two accounts.
     * @param acctgTrans an <code>AccountingTransaction</code> value
     * @param debitAccount the <code>GeneralLedgerAccount</code> to debit
     * @param creditAccount the <code>GeneralLedgerAccount</code> to credit
     * @param organizationPartyId the organization owning the transaction accounts
     * @param amount the amount transferred
     * @param transactionPartyId the party ID for the transactions
     * @return the created accounting transaction
     * @exception RepositoryException if an error occurs
     * @see #storeAcctgTransAndEntries
     */
    public AccountingTransaction createSimpleTransaction(AccountingTransaction acctgTrans, GeneralLedgerAccount debitAccount, GeneralLedgerAccount creditAccount, String organizationPartyId, BigDecimal amount, String transactionPartyId) throws RepositoryException, ServiceException;

    /**
     * Finds the <code>GlAccountHistory</code> for the given glAccountId, organizationPartyId and customTimePeriodId if one exists.
     * @param glAccountId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param customTimePeriodId a <code>String</code> value
     * @return a <code>GlAccountHistory</code> value
     * @exception RepositoryException if an error occurs
     */
    public GlAccountHistory getAccountHistory(String glAccountId, String organizationPartyId, String customTimePeriodId) throws RepositoryException;

    /**
     * Finds entity for configuring gl account by invoice type.
     * @param organizationPartyId a <code>String</code> value
     * @param invoiceTypeId a <code>String</code> value
     * @return an <code>InvoiceGlAccountType</code> value
     * @exception RepositoryException if an error occurs
     * @exception EntityNotFoundException if an error occurs
     */
    public InvoiceGlAccountType getInvoiceGlAccountType(String organizationPartyId, String invoiceTypeId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds the <code>InvoiceAdjustmentGlAccount</code> configuration entity.
     * @param organizationPartyId a <code>String</code> value
     * @param invoiceTypeId a <code>String</code> value
     * @param invoiceAdjustmentTypeId a <code>String</code> value
     * @return an <code>InvoiceAdjustmentGlAccount</code> value
     * @exception RepositoryException if an error occurs
     * @exception EntityNotFoundException if an error occurs
     */
    public InvoiceAdjustmentGlAccount getInvoiceAdjustmentGlAccount(String organizationPartyId, String invoiceTypeId, String invoiceAdjustmentTypeId) throws RepositoryException, EntityNotFoundException;

    /**
     * Persists the accounting transaction and its list of related entries.
     * @param acctgTrans an <code>AccountingTransaction</code> value
     * @param acctgTransEntries a list of <code>AcctgTransEntry</code>
     * @return a <code>String</code> value
     * @exception RepositoryException if an error occurs
     */
    public String storeAcctgTransAndEntries(AccountingTransaction acctgTrans, List<AcctgTransEntry> acctgTransEntries) throws RepositoryException, ServiceException;

    /**
     * Sets the transaction as posted and set the posted date to now if it has not already been set.
     * @param transaction an <code>AccountingTransaction</code> value
     * @exception RepositoryException if an error occurs
     * @exception LedgerException if an error occurs
     */
    public void setPosted(AccountingTransaction transaction) throws RepositoryException, LedgerException;

    /**
     * Gets the <code>AcctgTagPostingCheck</code> configuration entity for the given transaction.
     * @param transaction an <code>AccountingTransaction</code> value
     * @return an <code>AcctgTagPostingCheck</code> value
     * @exception RepositoryException if an error occurs
     */
    public AcctgTagPostingCheck getAcctgTagPostingCheck(AccountingTransaction transaction) throws RepositoryException;

    /**
     * Validates the accounting tags for an <code>AcctgTransEntry</code>.
     * @param entry an <code>AcctgTransEntry</code> value
     * @return a list of <code>AccountingTagConfigurationForOrganizationAndUsage</code> that are missing
     * @throws RepositoryException if an exception occurs
     */
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(AcctgTransEntry entry) throws RepositoryException;

    /**
     * Returns list of posted accounting transactions for given fiscal type(s).
     *
     * @param organizationPartyId the company identifier
     * @param glFiscalTypeId list of fiscal type id values
     * @param fromDate start of period or <code>null</code>.
     * @param trhuDate end of period
     * @return
     *   List of accounting transactions
     * @throws RepositoryException
     */
    public List<AccountingTransaction> getPostedTransactions(String organizationPartyId, String glFiscalTypeId, Timestamp fromDate, Timestamp thruDate) throws RepositoryException;

    /**
     * Returns list of posted accounting transactions and entries for given fiscal type(s).
     *
     * @param organizationPartyId the company identifier
     * @param glFiscalTypeId list of fiscal type id values
     * @param fromDate start of period or <code>null</code>.
     * @param trhuDate end of period
     * @return
     *   List of <code>AcctgTransAndEntries</code>
     * @throws RepositoryException
     */
    public List<AcctgTransAndEntries> getPostedTransactionsAndEntries(String organizationPartyId, List<String> glFiscalTypeId, Timestamp fromDate, Timestamp thruDate) throws RepositoryException;

}
