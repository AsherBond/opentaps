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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.GlFiscalTypeConstants;
import org.opentaps.base.entities.AcctgTransEntry;
import org.opentaps.base.entities.CustomTimePeriod;
import org.opentaps.base.entities.GlAccountHistory;
import org.opentaps.base.entities.GlAccountOrganization;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.ledger.AccountingTransaction;
import org.opentaps.domain.ledger.AccountingTransaction.TagBalance;
import org.opentaps.domain.ledger.GeneralLedgerAccount;
import org.opentaps.domain.ledger.LedgerException;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.ledger.LedgerServiceInterface;
import org.opentaps.domain.ledger.LedgerSpecificationInterface;
import org.opentaps.domain.organization.OrganizationDomainInterface;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.entity.util.EntityListIterator;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * POJO Service class for services that interact with the ledger.
 */
public class LedgerService extends DomainService implements LedgerServiceInterface {

    private static final String MODULE = LedgerService.class.getName();

    private String acctgTransId = null;
    private String skipCheckAcctgTags = null;

    /**
     * Default constructor.
     */
    public LedgerService() {
        super();
    }

    /** {@inheritDoc} */
    public void setAcctgTransId(String acctgTransId) {
        this.acctgTransId = acctgTransId;
    }

    /** {@inheritDoc} */
    public void setSkipCheckAcctgTags(String skipCheckAcctgTags) {
        this.skipCheckAcctgTags = skipCheckAcctgTags;
    }

    /** {@inheritDoc} */
    public void postAcctgTrans() throws ServiceException {
        try {
            LedgerRepositoryInterface ledgerRepository = getRepository();
            AccountingTransaction acctgTrans = ledgerRepository.getAccountingTransaction(acctgTransId);
            postToLedger(acctgTrans);
            //store the total debit amount in postedAmount
            acctgTrans.setPostedAmount(acctgTrans.getDebitTotal());
            getRepository().update(acctgTrans);
        } catch (GeneralException e)  {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void postToLedger(AccountingTransaction transaction) throws LedgerException, RepositoryException {
        if (transaction.isPosted()) {
            throw new LedgerException("FinancialsError_CannotPostAlreadyPosted", UtilMisc.toMap("acctgTransId", transaction.getAcctgTransId()));
        }

        // get the organization repository (we'll need for getting time periods in updateBalanceForTransaction())
        OrganizationDomainInterface orgDomain = getDomainsDirectory().getOrganizationDomain();
        OrganizationRepositoryInterface orgRepository = orgDomain.getOrganizationRepository();

        // check if we are scheduled to post
        if (transaction.getScheduledPostingDate() != null) {
            if (UtilDateTime.nowTimestamp().before(transaction.getScheduledPostingDate())) {
                throw new LedgerException("FinancialsError_CannotPostScheduledTransaction", UtilMisc.toMap("acctgTransId", transaction.getAcctgTransId(), "scheduledPostingDate", transaction.getScheduledPostingDate()));
            }
        }

        // verify that the transaction can be posted
        if (!transaction.canPost("Y".equals(skipCheckAcctgTags))) {
            Debug.logError("Cannot post transaction: " + transaction + " with entries " + transaction.getTransactionEntries(), MODULE);
            TagBalance tagNotBalance = transaction.accountingTagsBalance();
            if (tagNotBalance == null) {
                throw new LedgerException("FinancialsError_CannotPostFailedTrialBalance", UtilMisc.toMap("acctgTransId", transaction.getAcctgTransId(), "credit", transaction.getCreditTotal(), "debit", transaction.getDebitTotal()));
            } else {
                throw new LedgerException("FinancialsError_CannotPostFailedTagBalance", UtilMisc.<String, Object>toMap("acctgTransId", transaction.getAcctgTransId(), "credit", tagNotBalance.getCredit(), "debit", tagNotBalance.getDebit(), "tagIndex", tagNotBalance.getIndex(), "tagValue", tagNotBalance.getTagValue()));
            }
        }

        // update each account that the entries are posting to
        Set<String> updatedAccountIds = FastSet.newInstance();
        for (AcctgTransEntry entry : transaction.getTransactionEntries()) {
            if (updatedAccountIds.contains(entry.getGlAccountId())) {
                continue;
            }
            GeneralLedgerAccount account = transaction.getRepository().getLedgerAccount(entry.getGlAccountId(), entry.getOrganizationPartyId());
            updateBalanceForTransaction(account, transaction, orgRepository);
            updatedAccountIds.add(entry.getGlAccountId());
        }

        // mark transaction as posted, this will persist the change
        getRepository().setPosted(transaction);
    }

    /** {@inheritDoc} */
    public void updateBalanceForTransaction(GeneralLedgerAccount account, AccountingTransaction transaction, OrganizationRepositoryInterface orgRepository) throws RepositoryException, LedgerException {

        // only do that for glFiscalTypeId == ACTUAL
        if (!GlFiscalTypeConstants.ACTUAL.equals(transaction.getGlFiscalTypeId())) {
            return;
        }

        // for lazy loading the time periods by organization and the organization accounts
        Map<String, List<CustomTimePeriod>> timePeriodMap = FastMap.newInstance();
        Map<String, GlAccountOrganization> orgAccountMap = FastMap.newInstance();

        // update the balance for each entry and track the changes in the gl account history for the time periods and the relevant organization
        for (AcctgTransEntry entry : transaction.getTransactionEntries()) {
            if (!account.getGlAccountId().equals(entry.getGlAccountId())) {
                continue;
            }

            // get and validate the open time periods (lazy loading)
            List<CustomTimePeriod> openTimePeriods = timePeriodMap.get(entry.getOrganizationPartyId());
            if (openTimePeriods == null) {
                openTimePeriods = orgRepository.getOpenFiscalTimePeriods(entry.getOrganizationPartyId(), transaction.getTransactionDate());
                validateOpenTimePeriods(openTimePeriods, transaction, entry);
                timePeriodMap.put(entry.getOrganizationPartyId(), openTimePeriods);
            }

            // get the organization account, which stores the posted balance
            GlAccountOrganization orgAccount = orgAccountMap.get(entry.getOrganizationPartyId());
            if (orgAccount == null) {
                orgAccount = account.getRepository().getOrganizationAccount(entry.getGlAccountId(), entry.getOrganizationPartyId());
                orgAccountMap.put(entry.getOrganizationPartyId(), orgAccount);
            }

            // calculate the new posted balance
            BigDecimal postingAmount = account.getNormalizedAmount(entry);
            BigDecimal postedBalance = orgAccount.getPostedBalance() == null ? BigDecimal.ZERO : orgAccount.getPostedBalance();
            orgAccount.setPostedBalance(postedBalance.add(postingAmount));
            account.getRepository().update(orgAccount);

            // keep track of how much we posted in each time period history record
            for (CustomTimePeriod period : openTimePeriods) {
                GlAccountHistory history = account.getRepository().getAccountHistory(account.getGlAccountId(), entry.getOrganizationPartyId(), period.getCustomTimePeriodId());
                if (history == null) {
                    history = new GlAccountHistory();
                    history.setGlAccountId(account.getGlAccountId());
                    history.setOrganizationPartyId(entry.getOrganizationPartyId());
                    history.setCustomTimePeriodId(period.getCustomTimePeriodId());
                    history.setPostedDebits(BigDecimal.ZERO);
                    history.setPostedCredits(BigDecimal.ZERO);
                }

                // add to posted debits or credits column
                if (getSpecification().isDebit(entry)) {
                    history.setPostedDebits(history.getPostedDebits().add(entry.getAmount()));
                } else if (getSpecification().isCredit(entry)) {
                    history.setPostedCredits(history.getPostedCredits().add(entry.getAmount()));
                }
                account.getRepository().createOrUpdate(history);
            }
        }
    }

    /** {@inheritDoc} */
    public void updatePostedAmountAcctgTrans() throws ServiceException {
        try {
            LedgerRepositoryInterface ledgerRepository = getRepository();
            EntityCondition condition = EntityCondition.makeCondition(AccountingTransaction.Fields.isPosted.name(), EntityOperator.EQUALS, "Y");
            EntityListIterator<AccountingTransaction> acctgTransIt = ledgerRepository.findIterator(AccountingTransaction.class, condition);
            AccountingTransaction acctgTran = null;
            while ((acctgTran = acctgTransIt.next()) != null) {
                acctgTran.setPostedAmount(acctgTran.getDebitTotal());
                getRepository().update(acctgTran);
            }
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    // verify the open time periods and if something's wrong, throw a ledger exception
    private void validateOpenTimePeriods(List<CustomTimePeriod> periods, AccountingTransaction transaction, AcctgTransEntry entry) throws RepositoryException, LedgerException {

        // if there are no open time periods for the date, then we must not post and warn the user
        if (periods.size() == 0) {
            throw new LedgerException("FinancialsError_NoTimePeriodsToPost", UtilMisc.toMap("transactionDate", transaction.getTransactionDate(), "organizationPartyId", entry.getOrganizationPartyId()));
        }

        // verify that all periods are open, which the repository should guarantee but we check anyway
        for (CustomTimePeriod period : periods) {
            if (getSpecification().isClosed(period)) {
                throw new LedgerException("FinancialsError_TimePeriodClosedForPosting", UtilMisc.toMap("transactionDate", transaction.getTransactionDate(), "organizationPartyId", entry.getOrganizationPartyId(), "customTimePeriodId", period.getCustomTimePeriodId()));
            }
        }
    }

    private LedgerRepositoryInterface getRepository() throws RepositoryException {
        return getDomainsDirectory().getLedgerDomain().getLedgerRepository();
    }

    private LedgerSpecificationInterface getSpecification() throws RepositoryException {
        return getRepository().getSpecification();
    }

}
