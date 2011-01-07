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

package com.opensourcestrategies.financials.accounts;

import java.math.BigDecimal;

import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.base.util.UtilMisc;

/**
 * Represents a GL account.
 */
public class GLAccount implements Comparable {

    public String glAccountId;
    public BigDecimal balance;
    public String name;
    public boolean isDebitAccount;
    public boolean isCreditAccount;
    public boolean isAssetAccount;
    public boolean isLiabilityAccount;
    public boolean isEquityAccount;
    public boolean isIncomeAccount;
    public boolean isRevenueAccount;
    public boolean isExpenseAccount;

    /**
     * Creates a new <code>GLAccount</code> instance.
     *
     * @param delegator a <code>Delegator</code> value
     * @param glAccountId the GL account ID
     * @param balance a <code>BigDecimal</code> value
     * @exception GenericEntityException if an error occurs
     */
    public GLAccount(Delegator delegator, String glAccountId, BigDecimal balance) throws GenericEntityException {
        this.glAccountId = glAccountId;
        if (balance == null) {
            this.balance = BigDecimal.ZERO;
        } else {
            this.balance = balance.setScale(AccountsHelper.decimals, AccountsHelper.rounding);
        }
        GenericValue gv = delegator.findByPrimaryKey("GlAccount", UtilMisc.toMap("glAccountId", glAccountId));
        this.name = gv.getString("accountName");
        isDebitAccount = UtilAccounting.isDebitAccount(gv);
        isCreditAccount = UtilAccounting.isCreditAccount(gv);
        isAssetAccount = UtilAccounting.isAssetAccount(gv);
        isLiabilityAccount = UtilAccounting.isLiabilityAccount(gv);
        isEquityAccount = UtilAccounting.isEquityAccount(gv);
        isIncomeAccount = UtilAccounting.isIncomeAccount(gv);
        isRevenueAccount = UtilAccounting.isRevenueAccount(gv);
        isExpenseAccount = UtilAccounting.isExpenseAccount(gv);
    }

    /**
     * Compares this GL account to another GL account.
     *
     * @param toCompare an <code>Object</code> value
     * @return an <code>int</code> value
     * @exception ClassCastException if an error occurs
     */
    public int compareTo(Object toCompare) throws ClassCastException {
        GLAccount accToCompare = (GLAccount) toCompare;
        String glAccountIdToCompare = accToCompare.glAccountId;
        int comp = 0;
        try {

            // Try numeric comparison first
            Integer numericId = Integer.valueOf(this.glAccountId);
            Integer numericIdToCompare = Integer.valueOf(glAccountIdToCompare);
            comp = numericId.compareTo(numericIdToCompare);
        } catch (NumberFormatException e) {

            // Fall back to lexicographical comparison
            comp = this.glAccountId.compareTo(glAccountIdToCompare);
        }
        return comp;
    }

}
