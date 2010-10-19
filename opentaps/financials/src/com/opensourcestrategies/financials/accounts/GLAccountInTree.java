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

import org.apache.commons.lang.StringEscapeUtils;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Represents a GL account in a <code>GLAccountTree</code>.
 */
public class GLAccountInTree extends GLAccount {

    private TreeSet<GLAccountInTree> childAccounts;

    /**
     * Creates a new <code>GLAccountInTree</code> instance.
     *
     * @param delegator a <code>Delegator</code> value
     * @param glAccountId the GL account ID
     * @param balance a <code>BigDecimal</code> value
     * @exception GenericEntityException if an error occurs
     */
    public GLAccountInTree(Delegator delegator, String glAccountId, BigDecimal balance) throws GenericEntityException {
        super(delegator, glAccountId, balance);
        this.childAccounts = new TreeSet<GLAccountInTree>();
    }

    /**
     * Creates a new <code>GLAccountInTree</code> instance.
     *
     * @param delegator a <code>Delegator</code> value
     * @param glAccountId the GL account ID
     * @param balance a <code>BigDecimal</code> value
     * @param childAccounts the list of child GL accounts
     * @exception GenericEntityException if an error occurs
     */
    public GLAccountInTree(Delegator delegator, String glAccountId, BigDecimal balance, List<GLAccountInTree> childAccounts) throws GenericEntityException {
        super(delegator, glAccountId, balance);
        if (childAccounts == null) {
            this.childAccounts = new TreeSet<GLAccountInTree>();
        } else {
            this.childAccounts = new TreeSet<GLAccountInTree>(childAccounts);
        }
    }

    /**
     * Gets the balance of this GL account children GL accounts.
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getBalanceOfChildren() {
        BigDecimal balanceOfChildren = BigDecimal.ZERO;
        for (GLAccountInTree childAccount : childAccounts) {
            balanceOfChildren = balanceOfChildren.add(getBalanceOfChildrenRec(childAccount));
        }
        return balanceOfChildren.setScale(AccountsHelper.decimals, AccountsHelper.rounding);
    }

    /**
     * Gets the credit of this GL account children GL accounts.
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getCreditOfChildren() {
        BigDecimal creditOfChildren = BigDecimal.ZERO;
        for (GLAccountInTree childAccount : childAccounts) {
            creditOfChildren = creditOfChildren.add(getCreditOfChildrenRec(childAccount));
        }
        return creditOfChildren.setScale(AccountsHelper.decimals, AccountsHelper.rounding);
    }

    /**
     * Gets the debit of this GL account children GL accounts.
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getDebitOfChildren() {
        BigDecimal debitOfChildren = BigDecimal.ZERO;
        for (GLAccountInTree childAccount : childAccounts) {
            debitOfChildren = debitOfChildren.add(getDebitOfChildrenRec(childAccount));
        }
        return debitOfChildren.setScale(AccountsHelper.decimals, AccountsHelper.rounding);
    }

    /**
     * Gets the balance of this GL account given child GL account.
     * @param childAccount a <code>GLAccountInTree</code> value
     * @return a <code>BigDecimal</code> value
     */
    private BigDecimal getBalanceOfChildrenRec(GLAccountInTree childAccount) {
        if (childAccount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal balanceOfChildren = childAccount.balance;
        if ((childAccount.isDebitAccount && this.isCreditAccount)
            || (childAccount.isCreditAccount && this.isDebitAccount)) {
            balanceOfChildren = balanceOfChildren.negate();
        }

        for (GLAccountInTree grandchildAccount : childAccount.childAccounts) {
            balanceOfChildren = balanceOfChildren.add(getBalanceOfChildrenRec(grandchildAccount));
        }
        return balanceOfChildren;
    }

    /**
     * Gets the credit of this GL account given child GL account.
     * @param childAccount a <code>GLAccountInTree</code> value
     * @return a <code>BigDecimal</code> value
     */
    private BigDecimal getCreditOfChildrenRec(GLAccountInTree childAccount) {
        if (childAccount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal creditOfChildren = BigDecimal.ZERO;
        if (childAccount.isCreditAccount) {
            creditOfChildren = childAccount.balance;
        }
        for (GLAccountInTree grandchildAccount : childAccount.childAccounts) {
            creditOfChildren = creditOfChildren.add(getCreditOfChildrenRec(grandchildAccount));
        }
        return creditOfChildren;
    }

    /**
     * Gets the debit of this GL account given child GL account.
     * @param childAccount a <code>GLAccountInTree</code> value
     * @return a <code>BigDecimal</code> value
     */
    private BigDecimal getDebitOfChildrenRec(GLAccountInTree childAccount) {
        if (childAccount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal debitOfChildren = BigDecimal.ZERO;
        if (childAccount.isDebitAccount) {
            debitOfChildren = childAccount.balance;
        }
        for (GLAccountInTree grandchildAccount : childAccount.childAccounts) {
            debitOfChildren = debitOfChildren.add(getDebitOfChildrenRec(grandchildAccount));
        }
        return debitOfChildren;
    }

    /**
     * Gets the total balance of this GL account and his children GL accounts.
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getBalanceOfSelfAndChildren() {
        return this.balance.add(getBalanceOfChildren()).setScale(AccountsHelper.decimals, AccountsHelper.rounding);
    }

    /**
     * Gets the total credit of this GL account and his children GL accounts.
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getCreditOfSelfAndChildren() {
        if (isDebitAccount) {
            return getCreditOfChildren();
        }
        return this.balance.add(getCreditOfChildren()).setScale(AccountsHelper.decimals, AccountsHelper.rounding);
    }

    /**
     * Gets the total debit of this GL account and his children GL accounts.
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getDebitOfSelfAndChildren() {
        if (isCreditAccount) {
            return getDebitOfChildren();
        }
        return this.balance.add(getDebitOfChildren()).setScale(AccountsHelper.decimals, AccountsHelper.rounding);
    }

    /**
     * Adds a child GL account to this GL account.
     * @param child a <code>GLAccountInTree</code> value
     */
    public void addChild(GLAccountInTree child) {
        if (child != null) {
            childAccounts.add(child);
        }
    }

    /**
     * Build the JSON representation of the GLAccountTree which can then be used by the glAccountTree macro.
     * The JSON structure is:
     *  - glAccountId: the account Id
     *  - name: the account name
     *  - type: 'root' (for root accounts only ??)
     *  - balanceOfSelf: balance of the account
     *  - balanceOfSelfAndChildren: total balance of the account and its children
     *  - debitCredit: debit or credit acount, string "DEBIT" or "CREDIT"
     *  - children: list of children accounts that define the tree structure
     * @param isRoot indicate if this GL account is a root in the tree, or a leaf
     * @return the JSON representation of this <code>GLAccountInTree</code>
     * @see GLAccountTree
     * @see GLAccount
     * TODO: Move this somewhere else?
     */
    public String toJSONString(boolean isRoot) {

        StringBuilder sb = new StringBuilder("{");

        // write the JSON for this GLAccount
        sb.append("glAccountId:'").append(glAccountId).append("',");
        sb.append("name:'").append(StringEscapeUtils.escapeJavaScript(name)).append("',");
        sb.append("type:'");
        if (isRoot) {
            sb.append("root");
        } else {
            sb.append("leaf");
        }
        sb.append("',");
        sb.append("balanceOfSelf:").append(balance).append(",");
        sb.append("balanceOfSelfAndChildren:").append(this.getBalanceOfSelfAndChildren()).append(",");
        sb.append("debitCredit:'");
        if (this.isDebitAccount) {
            sb.append("DEBIT'");
        } else if (this.isCreditAccount) {
            sb.append("CREDIT'");
        } else {
            sb.append("");
        }

        // write the children list
        if (childAccounts != null && childAccounts.size() > 0) {
            sb.append(",");
        }
        Iterator<GLAccountInTree> it = childAccounts.iterator();
        if (it.hasNext()) {
            sb.append("children:[");
            while (it.hasNext()) {
                GLAccountInTree account = it.next();
                sb.append("{reference:'").append(account.glAccountId).append("'}");
                if (it.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("]");
        }

        sb.append("}");

        // append the children data
        it = childAccounts.iterator();
        while (it.hasNext()) {
            GLAccountInTree account = it.next();
            sb.append(",").append(account.toJSONString(false));
        }

        return sb.toString();
    }

}
