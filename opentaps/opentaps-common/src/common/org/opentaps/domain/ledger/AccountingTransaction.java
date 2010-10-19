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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.base.entities.AcctgTagPostingCheck;
import org.opentaps.base.entities.AcctgTransEntry;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Accounting Transaction Entry.
 */
public class AccountingTransaction extends org.opentaps.base.entities.AcctgTrans {

    private int roundingMode = -1;
    private int decimals = -1;

    private List<AcctgTransEntry> transactionEntries;
    private BigDecimal creditTotal;
    private BigDecimal debitTotal;
    private Map<Integer, Map<String, BigDecimal>> creditTags;
    private Map<Integer, Map<String, BigDecimal>> debitTags;
    private AcctgTagPostingCheck acctgTagPostingCheck;
    private boolean autoPost = true; 

    /**
     * Checks if this transaction is posted to the ledger.
     * @return a <code>boolean</code> value
     */
    public boolean isPosted() {
        return getSpecification().isPosted(this);
    }

    /**
     * Verify if automatic posting flag is set for this transaction.
     * @return
     *   If <code>true</code> post transaction just after creation.
     */
    public boolean isAutoPost() {
        return autoPost;
    }

    /**
     * Sets automatic posting flag.
     * @param autoPost <code>true</code> if transaction should be posted after creation at once
     */
    public void setAutoPost(boolean autoPost) {
        this.autoPost = autoPost;
    }

    /**
     * Gets the rounding mode.
     * TODO: After thinking about this, I think these should be in an interface RoundingInterface and perhaps use a convention such as opentaps.entity.AccountingTransaction.rounding
     * @return an <code>int</code> value
     */
    public int getRoundingMode() {
        if (roundingMode == -1) {
            roundingMode = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
        }
        return roundingMode;
    }

    /**
     * Gets the number of decimals used for rounding.
     * @return an <code>int</code> value
     */
    public int getDecimals() {
        if (decimals == -1) {
            decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
        }
        return decimals;
    }

    /**
     * Get the transaction entries.  Lazy loads the entries so subsequent calls do not hit database again.
     * @return the <code>List</code> of <code>AcctgTransEntry</code> for this transaction.
     * @exception RepositoryException if an error occurs
     */
    public List<AcctgTransEntry> getTransactionEntries() throws RepositoryException {
        if (transactionEntries == null) {
            transactionEntries = getRepository().getTransactionEntries(getAcctgTransId());
        }
        return transactionEntries;
    }

    /**
     * Checks if this transaction can be posted.
     * @return a <code>boolean</code> value
     * @throws LedgerException if an error occurs
     * @throws RepositoryException if an error occurs
     */
    public boolean canPost() throws LedgerException, RepositoryException {
        return canPost(false);
    }

    /**
     * Checks if this transaction can be posted.
     * @param skipCheckAcctgTags flag to ignore the tags check
     * @return a <code>boolean</code> value
     * @throws LedgerException if an error occurs
     * @throws RepositoryException if an error occurs
     */
    public boolean canPost(Boolean skipCheckAcctgTags) throws LedgerException, RepositoryException {
        return (getDebitTotal().compareTo(getCreditTotal()) == 0) && (skipCheckAcctgTags || (accountingTagsBalance() == null));
    }

    private void updateCreditDebitTotals() throws LedgerException, RepositoryException {
        LedgerSpecificationInterface spec = getSpecification();
        creditTotal = BigDecimal.ZERO;
        debitTotal = BigDecimal.ZERO;
        creditTags = new HashMap<Integer, Map<String, BigDecimal>>();
        debitTags = new HashMap<Integer, Map<String, BigDecimal>>();
        // sum up the debit and credit entries, rounding each time we add
        for (AcctgTransEntry entry : getTransactionEntries()) {
            if (spec.isDebit(entry)) {
                debitTotal = debitTotal.add(entry.getAmount()).setScale(getDecimals(), getRoundingMode());
                addAmountToTagMap(debitTags, entry);
            } else if (spec.isCredit(entry)) {
                creditTotal = creditTotal.add(entry.getAmount()).setScale(getDecimals(), getRoundingMode());
                addAmountToTagMap(creditTags, entry);
            } else {
                throw new LedgerException("FinancialsError_BadDebitCreditFlag", entry.toMap());
            }
        }
    }

    private void addAmountToTagMap(Map<Integer, Map<String, BigDecimal>> tagMap, AcctgTransEntry entry) {
        for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
            String tagName = "acctgTagEnumId" + i;
            Map<String, BigDecimal> tagAmountMap = tagMap.get(i);
            if (tagAmountMap == null) {
                tagAmountMap = new HashMap<String, BigDecimal>();
                tagMap.put(i, tagAmountMap);
            }
            if (UtilValidate.isNotEmpty(entry.get(tagName))) {
                String tagValue = entry.getString(tagName);
                BigDecimal tagAmount = tagAmountMap.get(tagValue);
                if (tagAmount == null) {
                    tagAmount = BigDecimal.ZERO;
                }
                tagAmount = tagAmount.add(entry.getAmount()).setScale(getDecimals(), getRoundingMode());
                tagAmountMap.put(tagValue, tagAmount);
            }
        }
    }

    /**
     * Checks that the configured tags balance.
     * @return the <code>TagBalance</code> of the first configured tag that does not balance, or <code>null</code> if they all balance as they should
     * @exception LedgerException if an error occurs
     * @exception RepositoryException if an error occurs
     */
    public TagBalance accountingTagsBalance() throws LedgerException, RepositoryException {
        if (creditTags == null || debitTags == null) {
            updateCreditDebitTotals();
        }
        AcctgTagPostingCheck configuration = getAcctgTagPostingCheck();
        if (configuration == null) {
            return null;
        }

        for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
            if (!"Y".equals(configuration.getString("tagEnum" + i + "MustBalance"))) {
                continue;
            }

            Map<String, BigDecimal> tagCreditAmounts = creditTags.get(i);
            Map<String, BigDecimal> tagDebitAmounts = debitTags.get(i);

            Set<String> tagValues = new HashSet<String>();
            if (tagCreditAmounts != null) {
                tagValues.addAll(tagCreditAmounts.keySet());
            }
            if (tagDebitAmounts != null) {
                tagValues.addAll(tagDebitAmounts.keySet());
            }

            for (String tagValue : tagValues) {
                if (getBalanceForTag(i, tagValue).signum() != 0) {
                    return new TagBalance(i, tagValue, getDebitForTag(i, tagValue), getCreditForTag(i, tagValue));
                }
            }
        }
        return null;
    }

    /**
     * Gets the configuration of tags that must balance.
     * @return an <code>AcctgTagPostingCheck</code> value
     * @exception RepositoryException if an error occurs
     */
    public AcctgTagPostingCheck getAcctgTagPostingCheck() throws RepositoryException {
        if (acctgTagPostingCheck == null) {
            acctgTagPostingCheck = getRepository().getAcctgTagPostingCheck(this);
        }
        return acctgTagPostingCheck;
    }

    /**
     * Gets the sum of all credit entries.
     * @return a <code>BigDecimal</code> value
     * @exception LedgerException if an error occurs
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getCreditTotal() throws LedgerException, RepositoryException {
        if (creditTotal == null) {
            updateCreditDebitTotals();
        }
        return creditTotal;
    }

    /**
     * Gets the sum of all debit entries.
     * @return a <code>BigDecimal</code> value
     * @exception LedgerException if an error occurs
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getDebitTotal() throws LedgerException, RepositoryException {
        if (debitTotal == null) {
            updateCreditDebitTotals();
        }
        return debitTotal;
    }

    /**
     * Gets the sum of all credit entries for the given tag index.
     * @param index the tag position index
     * @param tagValue the value of the tag
     * @return a <code>BigDecimal</code> value
     * @exception LedgerException if an error occurs
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getCreditForTag(Integer index, String tagValue) throws LedgerException, RepositoryException {
        if (creditTags == null) {
            updateCreditDebitTotals();
        }
        Map<String, BigDecimal> tagCredits = creditTags.get(index);
        if (tagCredits == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = tagCredits.get(tagValue);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    /**
     * Gets the sum of all debit entries for the given tag index.
     * @param index the tag position index
     * @param tagValue the value of the tag
     * @return a <code>BigDecimal</code> value
     * @exception LedgerException if an error occurs
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getDebitForTag(Integer index, String tagValue) throws LedgerException, RepositoryException {
        if (debitTags == null) {
            updateCreditDebitTotals();
        }
        Map<String, BigDecimal> tagDebits = debitTags.get(index);
        if (tagDebits == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = tagDebits.get(tagValue);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    /**
     * Gets the balance for the given tag index.
     * @param index the tag position index
     * @param tagValue the value of the tag
     * @return a <code>BigDecimal</code> value
     * @exception LedgerException if an error occurs
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getBalanceForTag(Integer index, String tagValue) throws LedgerException, RepositoryException {
        if (debitTags == null || creditTags == null) {
            updateCreditDebitTotals();
        }
        return getDebitForTag(index, tagValue).subtract(getCreditForTag(index, tagValue)).setScale(getDecimals(), getRoundingMode());
    }

    /**
     * Calculates the trial balance for this transaction which is the sums of all credit entries minus the sum of all debit entries.
     * @return a <code>BigDecimal</code> value
     * @exception LedgerException if an error occurs
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getTrialBalance() throws LedgerException, RepositoryException {
        // return debits minus credits
        return getDebitTotal().subtract(getCreditTotal()).setScale(getDecimals(), getRoundingMode());
    }

    /**
     * Gets the repository.
     * @return a <code>LedgerRepositoryInterface</code> value
     */
    public LedgerRepositoryInterface getRepository() {
        return (LedgerRepositoryInterface) repository;
    }

    /**
     * Gets the ledger specification interface.
     * @return a <code>LedgerSpecificationInterface</code> value
     */
    public LedgerSpecificationInterface getSpecification() {
        return getRepository().getSpecification();
    }

    public static class TagBalance {
        private int index;
        private String tagValue;
        private BigDecimal debit;
        private BigDecimal credit;

        public TagBalance(int index, String tagValue, BigDecimal debit, BigDecimal credit) {
            this.index = index;
            this.tagValue = tagValue;
            this.debit = debit;
            this.credit = credit;
        }

        public int getIndex() {
            return index;
        }

        public String getTagValue() {
            return tagValue;
        }

        public BigDecimal getDebit() {
            return debit;
        }

        public BigDecimal getCredit() {
            return credit;
        }

        public BigDecimal getBalance() {
            return debit.subtract(credit);
        }

    }

}
