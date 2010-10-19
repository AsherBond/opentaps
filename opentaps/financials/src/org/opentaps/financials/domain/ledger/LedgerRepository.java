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
package org.opentaps.financials.domain.ledger;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.AcctgTagPostingCheck;
import org.opentaps.base.entities.AcctgTransAndEntries;
import org.opentaps.base.entities.AcctgTransEntry;
import org.opentaps.base.entities.GlAccountClass;
import org.opentaps.base.entities.GlAccountHistory;
import org.opentaps.base.entities.GlAccountOrganization;
import org.opentaps.base.entities.GlAccountTypeDefault;
import org.opentaps.base.entities.InvoiceAdjustmentGlAccount;
import org.opentaps.base.entities.InvoiceGlAccountType;
import org.opentaps.base.entities.PartyAcctgPreference;
import org.opentaps.base.entities.ProductGlAccount;
import org.opentaps.domain.ledger.AccountingTransaction;
import org.opentaps.domain.ledger.GeneralLedgerAccount;
import org.opentaps.domain.ledger.LedgerException;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.ledger.LedgerServiceInterface;
import org.opentaps.domain.ledger.LedgerSpecificationInterface;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceException;

/** {@inheritDoc} */
public class LedgerRepository extends Repository implements LedgerRepositoryInterface {

    private static final String MODULE = LedgerRepositoryInterface.class.getName();

    private OrganizationRepositoryInterface organizationRepository;

    /**
     * Default constructor.
     */
    public LedgerRepository() {
        super();
    }

    /**
     * Returns the <code>LedgerSpecification</code> implementation defined in this package.
     * @return an <code>LedgerSpecificationInterface</code> value
     */
    public LedgerSpecificationInterface getSpecification() {
        return new LedgerSpecification();
    }

    // TODO: should this get active accounts?  it's used by updateBalances, so consider a separate method to filter by date
    /** {@inheritDoc} */
    public GlAccountOrganization getOrganizationAccount(String glAccountId, String organizationPartyId) throws RepositoryException {
        try {
            // do not cache -- we may have to update these values.
            return findOneNotNull(GlAccountOrganization.class, map(GlAccountOrganization.Fields.glAccountId, glAccountId, GlAccountOrganization.Fields.organizationPartyId, organizationPartyId), "GlAccount [" + glAccountId + "] not found in organization [" + organizationPartyId + "]");
        } catch (GeneralException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public GeneralLedgerAccount getLedgerAccount(String glAccountId, String organizationPartyId) throws RepositoryException {
        return findOne(GeneralLedgerAccount.class, map(GeneralLedgerAccount.Fields.glAccountId, glAccountId));
    }

    /** {@inheritDoc} */
    public GeneralLedgerAccount getDefaultLedgerAccount(String glAccountTypeId, String organizationPartyId) throws RepositoryException, EntityNotFoundException {
        GlAccountTypeDefault glAccountTypeDefault = findOneCache(GlAccountTypeDefault.class, map(GlAccountTypeDefault.Fields.glAccountTypeId, glAccountTypeId, GlAccountTypeDefault.Fields.organizationPartyId, organizationPartyId));
        if (glAccountTypeDefault != null) {
            return getLedgerAccount(glAccountTypeDefault.getGlAccountId(), organizationPartyId);
        }

        // else throw EntityNotFoundException
        throw new EntityNotFoundException(GeneralLedgerAccount.class, "No ProductGLAccount or GlAccountTypeDefault found for glAccountTypeId [" + glAccountTypeId + "] and organizationPartyId [" + organizationPartyId + "]");
    }

    /** {@inheritDoc} */
    public GeneralLedgerAccount getProductLedgerAccount(String productId, String glAccountTypeId, String organizationPartyId) throws RepositoryException, EntityNotFoundException {
        // try the product GL account first
        ProductGlAccount productGlAccount = findOneCache(ProductGlAccount.class, map(ProductGlAccount.Fields.productId, productId, ProductGlAccount.Fields.glAccountTypeId, glAccountTypeId, ProductGlAccount.Fields.organizationPartyId, organizationPartyId));
        if (productGlAccount != null) {
            return getLedgerAccount(productGlAccount.getGlAccountId(), organizationPartyId);
        }

        // else try the default account
        return getDefaultLedgerAccount(glAccountTypeId, organizationPartyId);
    }

    /** {@inheritDoc} */
    public AccountingTransaction getAccountingTransaction(String acctgTransId) throws RepositoryException {
        try {
            return findOneNotNull(AccountingTransaction.class, map(AccountingTransaction.Fields.acctgTransId, acctgTransId), "Accounting Transaction [" + acctgTransId + "] not found.");
        } catch (GeneralException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public AcctgTransEntry getTransactionEntry(String acctgTransId, String acctgTransEntrySeqId) throws RepositoryException {
        try {
            return findOneNotNull(AcctgTransEntry.class, map(AcctgTransEntry.Fields.acctgTransId, acctgTransId, AcctgTransEntry.Fields.acctgTransEntrySeqId, acctgTransEntrySeqId), "Accounting Transaction Entry [" + acctgTransId + "/" + acctgTransEntrySeqId + "] not found.");
        } catch (GeneralException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public List<AcctgTransEntry> getTransactionEntries(String acctgTransId) throws RepositoryException {
        return findList(AcctgTransEntry.class, map(AcctgTransEntry.Fields.acctgTransId, acctgTransId), Arrays.asList("acctgTransEntrySeqId"));
    }

    /** {@inheritDoc} */
    public List<GlAccountClass> getAccountClassTree(String glAccountClassId) throws RepositoryException {
        List<GlAccountClass> classes = FastList.newInstance();
        try {
            GlAccountClass accountClass = findOneNotNull(GlAccountClass.class, map(GlAccountClass.Fields.glAccountClassId, glAccountClassId), "GlAccountClass [" + glAccountClassId + "] not found.");

            // go up the tree of classes
            classes.add(accountClass);
            while (accountClass.getParentClassId() != null && !accountClass.getParentClassId().equals(accountClass.getGlAccountClassId())) {
                accountClass = accountClass.getRelatedOne(GlAccountClass.class, "ParentGlAccountClass");
                classes.add(accountClass);
            }
            return classes;
        } catch (GeneralException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public AccountingTransaction createSimpleTransaction(AccountingTransaction acctgTrans, GeneralLedgerAccount debitAccount, GeneralLedgerAccount creditAccount, String organizationPartyId, BigDecimal amount, String transactionPartyId) throws RepositoryException, ServiceException {

        LedgerSpecificationInterface specification = getSpecification();

        AcctgTransEntry creditEntry = new AcctgTransEntry();
        creditEntry.setGlAccountId(creditAccount.getGlAccountId());
        creditEntry.setOrganizationPartyId(organizationPartyId);
        creditEntry.setDebitCreditFlag(specification.getCreditFlag());
        creditEntry.setAmount(amount);

        AcctgTransEntry debitEntry = new AcctgTransEntry();
        debitEntry.setGlAccountId(debitAccount.getGlAccountId());
        debitEntry.setOrganizationPartyId(organizationPartyId);
        debitEntry.setDebitCreditFlag(specification.getDebitFlag());
        debitEntry.setAmount(amount);

        if (transactionPartyId != null) {
            debitEntry.setPartyId(transactionPartyId);
            creditEntry.setPartyId(transactionPartyId);
        }

        PartyAcctgPreference pref = findOneCache(PartyAcctgPreference.class, map(PartyAcctgPreference.Fields.partyId, organizationPartyId));
        String currencyUomId = pref.getBaseCurrencyUomId();
        if (currencyUomId != null) {
            debitEntry.setCurrencyUomId(currencyUomId);
            creditEntry.setCurrencyUomId(currencyUomId);
        }

        storeAcctgTransAndEntries(acctgTrans, Arrays.asList(creditEntry, debitEntry));
        return acctgTrans;
    }

    /** {@inheritDoc} */
    public InvoiceGlAccountType getInvoiceGlAccountType(String organizationPartyId, String invoiceTypeId) throws RepositoryException, EntityNotFoundException {
       return findOneNotNullCache(InvoiceGlAccountType.class, map(InvoiceGlAccountType.Fields.organizationPartyId, organizationPartyId, InvoiceGlAccountType.Fields.invoiceTypeId, invoiceTypeId));
    }


    /** {@inheritDoc} */
    public GlAccountHistory getAccountHistory(String glAccountId, String organizationPartyId, String customTimePeriodId) throws RepositoryException {
       return findOne(GlAccountHistory.class, map(GlAccountHistory.Fields.glAccountId, glAccountId, GlAccountHistory.Fields.organizationPartyId, organizationPartyId, GlAccountHistory.Fields.customTimePeriodId, customTimePeriodId));
    }

    /** {@inheritDoc} */
    public InvoiceAdjustmentGlAccount getInvoiceAdjustmentGlAccount(String organizationPartyId, String invoiceTypeId, String invoiceAdjustmentTypeId) throws RepositoryException, EntityNotFoundException {
       return findOneNotNullCache(InvoiceAdjustmentGlAccount.class, map(InvoiceAdjustmentGlAccount.Fields.organizationPartyId, organizationPartyId, InvoiceAdjustmentGlAccount.Fields.invoiceTypeId, invoiceTypeId, InvoiceAdjustmentGlAccount.Fields.invoiceAdjustmentTypeId, invoiceAdjustmentTypeId));
    };

    /** {@inheritDoc} */
    public String storeAcctgTransAndEntries(AccountingTransaction acctgTrans, List<AcctgTransEntry> acctgTransEntries) throws RepositoryException, ServiceException {
        // get the seq ID of the AcctgTrans
        if (acctgTrans.getAcctgTransId() == null) {
            acctgTrans.setAcctgTransId(getNextSeqId(acctgTrans));
        }
        // this is the default value
        if (acctgTrans.getGlFiscalTypeId() == null) {
            acctgTrans.setGlFiscalTypeId(getSpecification().getFiscalTypeIdForActual());
        }
        // set the transaction date to NOW if there is none
        if (acctgTrans.getTransactionDate() == null) {
            acctgTrans.setTransactionDate(UtilDateTime.nowTimestamp());
        }
        createOrUpdate(acctgTrans); // persist it

        // counter for the list of transaction entries
        int i = 1;
        for (AcctgTransEntry entry : acctgTransEntries) {
            // this field is always set to _NA_, so do it here
            entry.setAcctgTransEntryTypeId("_NA_");

            // usually acctgTransId should be the same as the main transaction
            if (entry.getAcctgTransId() == null) {
                entry.setAcctgTransId(acctgTrans.getAcctgTransId());
            }

            // auto set the sequence Id
            if (entry.getAcctgTransEntrySeqId() == null) {
                entry.setAcctgTransEntrySeqId(new Integer(i).toString());
            }
            // status should be not reconciled
            entry.setReconcileStatusId(StatusItemConstants.AcctgEnrecStatus.AES_NOT_RECONCILED);
            createOrUpdate(entry);  // persist it
            i++;    // increase counter regardless of it was used, so it's correct relative to real sequence of entries
        }

        // post to ledger if configured to
        if (getSpecification().isAutoPostToLedger(acctgTrans)) {
            LedgerServiceInterface ledgerService = DomainsDirectory.getDomainsDirectory(this).getLedgerDomain().getLedgerService();
            ledgerService.setAcctgTransId(acctgTrans.getAcctgTransId());
            ledgerService.postAcctgTrans();
        }

        return acctgTrans.getAcctgTransId();
    }

    /** {@inheritDoc} */
    public void setPosted(AccountingTransaction transaction) throws RepositoryException, LedgerException {
        if (transaction.isPosted()) {
            throw new LedgerException("Accounting Transaction [" + transaction.getAcctgTransId() + "] has already been posted");
        }

        // if no posted date has been supplied, set it to now
        if (transaction.getPostedDate() == null) {
            transaction.setPostedDate(UtilDateTime.nowTimestamp());
        }
        transaction.setIsPosted("Y");

        // persist it
        update(transaction);
    }

    /** {@inheritDoc} */
    public AcctgTagPostingCheck getAcctgTagPostingCheck(AccountingTransaction transaction) throws RepositoryException {
        // not sure if all that is really necessary, but in case there are more than one organization involved
        Set<String> organizationPartyIds = Entity.getDistinctFieldValues(String.class, transaction.getTransactionEntries(), AcctgTransEntry.Fields.organizationPartyId);
        if (organizationPartyIds.isEmpty()) {
            return null;
        }
        // get the first AcctgTagPostingCheck found for any of the organizations
        for (String organizationPartyId : organizationPartyIds) {
            AcctgTagPostingCheck found = findOne(AcctgTagPostingCheck.class, map(AcctgTagPostingCheck.Fields.organizationPartyId, organizationPartyId));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(AcctgTransEntry entry) throws RepositoryException {
        return getOrganizationRepository().validateTagParameters(entry, entry.getOrganizationPartyId(), UtilAccountingTags.TRANSACTION_ENTRY_TAG);
    }

    /** {@inheritDoc} */
    public List<AccountingTransaction> getPostedTransactions(String organizationPartyId, String glFiscalTypeId, Timestamp fromDate, Timestamp thruDate) throws RepositoryException {
        List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition(AcctgTransAndEntries.Fields.isPosted.getName(), EntityOperator.EQUALS, "Y"),
                EntityCondition.makeCondition(AcctgTransAndEntries.Fields.organizationPartyId.getName(), EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition(AcctgTransAndEntries.Fields.glFiscalTypeId.getName(), EntityOperator.EQUALS, glFiscalTypeId),
                EntityCondition.makeCondition(AcctgTransAndEntries.Fields.transactionDate.getName(), EntityOperator.LESS_THAN_EQUAL_TO, thruDate)
        );

        // add start time if any is specified
        if (fromDate != null) {
            conditions.add(EntityCondition.makeCondition(AcctgTransAndEntries.Fields.transactionDate.getName(), EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
        }

        List<AcctgTransAndEntries> transAndEntries = findList(AcctgTransAndEntries.class, EntityCondition.makeCondition(conditions, EntityOperator.AND), Arrays.asList("acctgTransId"));

        if (UtilValidate.isEmpty(transAndEntries)) {
            return FastList.newInstance();
        }

        // create collection of unique accounting transactions.
        Set<AccountingTransaction> acctgTrans = FastSet.newInstance();
        for (AcctgTransAndEntries transData : transAndEntries) {
            AccountingTransaction transaction = new AccountingTransaction();
            transaction.fromEntity(transData);
            acctgTrans.add(transaction);
        }

        // convert to List and return
        return UtilMisc.toList(acctgTrans);
    }

    /** {@inheritDoc} */
    public List<AcctgTransAndEntries> getPostedTransactionsAndEntries(String organizationPartyId, List<String> glFiscalTypeId, Timestamp fromDate, Timestamp thruDate) throws RepositoryException {
        List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition(AcctgTransAndEntries.Fields.isPosted.getName(), EntityOperator.EQUALS, "Y"),
                EntityCondition.makeCondition(AcctgTransAndEntries.Fields.organizationPartyId.getName(), EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition(AcctgTransAndEntries.Fields.glFiscalTypeId.getName(), EntityOperator.IN, glFiscalTypeId)
        );
        if (fromDate != null) {
            conditions.add(EntityCondition.makeCondition(AcctgTransAndEntries.Fields.transactionDate.getName(), EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
        }
        if (thruDate != null) {
            conditions.add(EntityCondition.makeCondition(AcctgTransAndEntries.Fields.transactionDate.getName(), EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
        }

        // add start time if any is specified
        if (fromDate != null) {
            conditions.add(EntityCondition.makeCondition(AcctgTransAndEntries.Fields.transactionDate.getName(), EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
        }

        List<AcctgTransAndEntries> transAndEntries = findList(AcctgTransAndEntries.class, EntityCondition.makeCondition(conditions, EntityOperator.AND), Arrays.asList("acctgTransId"));

        return (UtilValidate.isEmpty(transAndEntries) ? FastList.<AcctgTransAndEntries>newInstance() : transAndEntries);
    }

    protected OrganizationRepositoryInterface getOrganizationRepository() throws RepositoryException {
        if (organizationRepository == null) {
            organizationRepository = DomainsDirectory.getDomainsDirectory(this).getOrganizationDomain().getOrganizationRepository();
        }
        return organizationRepository;
    }

}
