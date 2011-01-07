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

import org.ofbiz.accounting.AccountingException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

import java.math.BigDecimal;
import java.util.*;

/**
 * Represents a tree of GL accounts.
 */
public class GLAccountTree {

    private static final String MODULE = GLAccountTree.class.getName();
    private String organizationPartyId;
    private String currencyUomId;
    private TreeSet<GLAccountInTree> rootAccounts;

    /**
     * Creates a new <code>GLAccountTree</code> instance.
     *
     * @param organizationPartyId the organization party ID for which to build the tree
     * @param currencyUomId the currency used to display amounts in the tree
     * @exception AccountingException if the organizationPartyId is not given
     */
    protected GLAccountTree(String organizationPartyId, String currencyUomId) throws AccountingException {
        if (organizationPartyId == null) {
            throw new AccountingException("organizationPartyId cannot be null");
        }
        this.organizationPartyId = organizationPartyId;
        this.currencyUomId = currencyUomId;
        this.rootAccounts = new TreeSet<GLAccountInTree>();
    }

    /**
     * Creates a new <code>GLAccountTree</code> instance.
     *
     * @param delegator a <code>Delegator</code> value
     * @param organizationPartyId the organization party ID for which to build the tree
     * @param currencyUomId the currency used to display amounts in the tree
     * @param accounts a list of GL accounts to include in the tree
     * @exception GenericEntityException if an error occurs
     * @exception AccountingException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public GLAccountTree(Delegator delegator, String organizationPartyId, String currencyUomId, List<Map<String, Object>> accounts) throws GenericEntityException, AccountingException {
        this(organizationPartyId, currencyUomId);

        // Make an expanded map of accounts which contains all parents, grandparents, etc. so that we can create
        //  the parent-child heirarchy
        Map tempAccounts = new HashMap<String, Map>();
        for (Map account : accounts) {
            tempAccounts.put(account.get("glAccountId"), account);

            // For each parent, grandparent, etc, add to tempAccounts if not present
            List<GenericValue> parents = AccountsHelper.getAccountParents(delegator, (String) account.get("glAccountId"));
            for (GenericValue parent : parents) {
                if (!tempAccounts.containsKey(parent.getString("glAccountId"))) {
                    tempAccounts.put(parent.getString("glAccountId"), parent);
                }
            }
        }

        // Iterate through the expanded accounts, constructing GLAccountInTree objects
        Map<String, GLAccountInTree> tempCrossRef = new HashMap<String, GLAccountInTree>();
        List<Map> allAccountMaps = new ArrayList<Map>(tempAccounts.values());
        for (Map account : allAccountMaps) {
            GLAccountInTree accInTree = new GLAccountInTree(delegator, (String) account.get("glAccountId"), (BigDecimal) account.get("accountSum"));
            tempCrossRef.put((String) account.get("glAccountId"), accInTree);

            // Add any accounts without parents to rootAccounts
            if (account.get("parentGlAccountId") == null) {
                this.rootAccounts.add(accInTree);
            }
        }

        // Iterate through the expanded accounts, retrieving the parent and child from the map of GLAccountInTree
        //  objects, so that we can make the parent-child heirarchy
        for (Map account : allAccountMaps) {
            String glAccountId = (String) account.get("glAccountId");
            String parentGlAccountId = (String) account.get("parentGlAccountId");

            // Don't worry about root accounts
            if (parentGlAccountId == null) {
                continue;
            }

            GLAccountInTree acct = tempCrossRef.get(glAccountId);
            if (acct == null) {
                String errMsg = "Problem constructing GLAccountTree account " + glAccountId + " wasn't found";
                Debug.logError(errMsg, MODULE);
                throw new AccountingException(errMsg);
            }

            GLAccountInTree parentAcct = tempCrossRef.get(parentGlAccountId);
            if (parentAcct == null) {
                String errMsg = "Problem constructing GLAccountTree - parent account " + parentGlAccountId + " of account " + glAccountId + " wasn't found";
                Debug.logError(errMsg, MODULE);
                throw new AccountingException(errMsg);
            }

            parentAcct.addChild(acct);
        }
    }

    /**
     *  Creates a GLAccountTree from a map of (GenericValue) GL account to (Double) balance such as those returned
     *  from the balance sheet, trial balance, and income statement services.
     *  Note that this method couldn't be a constructor because it must turn the Map into a list before calling another constructor.
     * @param delegator a <code>Delegator</code> value
     * @param organizationPartyId the organization party ID for which to build the tree
     * @param currencyUomId the currency used to display amounts in the tree
     * @param accountBalances a <code>Map</code> of GLAccount to the account balance as <code>Double</code>.
     * @return a new <code>GLAccountTree</code> instance
     * @throws GenericEntityException if an error occurs
     * @throws AccountingException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static GLAccountTree getGLAccountTree(Delegator delegator, String organizationPartyId, String currencyUomId, Map<GenericValue, ?> accountBalances) throws GenericEntityException, AccountingException {
        List<Map<String, Object>> accountBalancesList = new ArrayList<Map<String, Object>>();
        if (accountBalances != null) {
            for (GenericValue a : accountBalances.keySet()) {
                Object balObj = accountBalances.get(a);
                BigDecimal bal = BigDecimal.ZERO;
                if (balObj instanceof BigDecimal) {
                    bal = (BigDecimal) balObj;
                } else if (balObj instanceof Double) {
                    bal = BigDecimal.valueOf((Double) balObj).setScale(4, BigDecimal.ROUND_HALF_EVEN);
                }

                Map a2 = new HashMap(a);
                a2.put("accountSum", bal);
                accountBalancesList.add(a2);
            }
        }
        return new GLAccountTree(delegator, organizationPartyId, currencyUomId, accountBalancesList);
    }
    /**
     * Gets the total balance of this tree, which is the sum of the balances of all included GL accounts.
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getTotalBalance() {
        BigDecimal total = BigDecimal.ZERO;
        for (GLAccountInTree acct : rootAccounts) {
            total = total.add(acct.getBalanceOfSelfAndChildren());
        }
        return total.setScale(AccountsHelper.decimals, AccountsHelper.rounding);
    }

    /**
     * Gets the total credit of this tree, which is the sum of the credits of all included GL accounts.
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getTotalCredit() {
        BigDecimal total = BigDecimal.ZERO;
        for (GLAccountInTree acct : rootAccounts) {
            total = total.add(acct.getCreditOfSelfAndChildren());
        }
        return total.setScale(AccountsHelper.decimals, AccountsHelper.rounding);
    }

    /**
     * Gets the total debit of this tree, which is the sum of the debits of all included GL accounts.
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getTotalDebit() {
        BigDecimal total = BigDecimal.ZERO;
        for (GLAccountInTree acct : rootAccounts) {
            total = total.add(acct.getDebitOfSelfAndChildren());
        }
        return total.setScale(AccountsHelper.decimals, AccountsHelper.rounding);
    }

    /**
     * Gets the root GL accounts.
     * @return the root GL accounts
     */
    public TreeSet<GLAccountInTree> getRootAccounts() {
        return this.rootAccounts;
    }

    /**
     * Build the JSON representation of the GLAccountTree which can then be used by the glAccountTree macro.
     * The JSON returned is the list of the JSON representation of all rootAccounts contained in this glAccountTree.
     * @see GLAccountInTree
     * TODO: Move this somewhere else?
     * @return a JSON representation of the tree
     */
    public String toJSONString() {

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Iterator<GLAccountInTree> it = rootAccounts.iterator();
        while (it.hasNext()) {
            GLAccountInTree account = it.next();
            sb.append(account.toJSONString(true));
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }


    /**
     * Gets the organizationPartyId of this tree.
     * @return the organizationPartyId of this tree
     */
    public String getOrganizationPartyId() {
        return organizationPartyId;
    }

    /**
     * Gets the currency UOM used in this tree.
     * @return the currency UOM used in this tree
     */
    public String getCurrencyUomId() {
        return currencyUomId;
    }

    /**
     * Retrieves all root level accounts and their children for an organization.  Note that this will
     * fetch all accounts that are currently configured for the organization.  The currency is stored
     * used for display purposes only and does not affect the lookup.
     *
     * TODO: this mechanism does a bunch of sequential reads on the database
     * @param delegator a <code>Delegator</code> value
     * @param organizationPartyId the organization party ID
     * @param currencyUomId the currency used to display amounts in the tree
     * @return a <code>GLAccountTree</code> value
     * @exception GenericEntityException if an error occurs
     * @exception AccountingException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static GLAccountTree getOrganizationTree(Delegator delegator, String organizationPartyId, String currencyUomId) throws GenericEntityException, AccountingException {
        GLAccountTree tree = new GLAccountTree(organizationPartyId, currencyUomId);

        // start with the root accounts
        List conditions = UtilMisc.toList(
            EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
            EntityCondition.makeCondition("parentGlAccountId", EntityOperator.EQUALS, null),
            EntityUtil.getFilterByDateExpr()
        );
        List<GenericValue> rootAccounts = delegator.findByAnd("GlAccountOrganizationAndClass", conditions, UtilMisc.toList("accountCode"));
        for (GenericValue account : rootAccounts) {
            GLAccountInTree rootNode = new GLAccountInTree(delegator, account.getString("glAccountId"), account.getBigDecimal("postedBalance"));
            tree.rootAccounts.add(rootNode);
            populateChildren(delegator, rootNode, organizationPartyId);
        }
        return tree;
    }

    /**
     * Given a node, fills in the children.  Useful if the children are not already present.  Note that the same
     * tree structure exists across organizations because the model for GL accounts is this way by convention,
     * however a given account might not be configured for an organization.
     * @param delegator a <code>Delegator</code> value
     * @param node a <code>GLAccountInTree</code> value
     * @param organizationPartyId a <code>String</code> value
     * @exception GenericEntityException if an error occurs
     * @exception AccountingException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static void populateChildren(Delegator delegator, GLAccountInTree node, String organizationPartyId) throws GenericEntityException, AccountingException {
        List conditions = UtilMisc.toList(
                EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("parentGlAccountId", EntityOperator.EQUALS, node.glAccountId),
                EntityUtil.getFilterByDateExpr()
            );
        List<GenericValue> children = delegator.findByAnd("GlAccountOrganizationAndClass", conditions);
        for (GenericValue child : children) {
            GLAccountInTree childNode = new GLAccountInTree(delegator, child.getString("glAccountId"), child.getBigDecimal("postedBalance"));
            node.addChild(childNode);
            populateChildren(delegator, childNode, organizationPartyId);
        }
    }

}
