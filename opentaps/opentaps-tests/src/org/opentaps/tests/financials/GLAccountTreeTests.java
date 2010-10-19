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
package org.opentaps.tests.financials;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;

import com.opensourcestrategies.financials.accounts.GLAccountInTree;
import com.opensourcestrategies.financials.accounts.GLAccountTree;

/**
 * GLAccountTree tests.
 * Those are to test the GLAccountInTree and GLAccountTree classes
 *  that are used for example to generate the Income Statement.
 */
public class GLAccountTreeTests extends FinancialsTestCase {

    /** Facility and inventory owner */
    public final String organizationPartyId = "Company";
    public final String currencyUomId = "USD";

    TimeZone timeZone = TimeZone.getDefault();
    Locale locale = Locale.getDefault();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test the generated JSON.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testGLAccountTree() throws GeneralException {

        // build the tree with test values, normally those values are retreived
        // by the getIncomeStatementByDates service
        // the GLAccountTree is expecting a list of Map containing: (glAccountId, parentGlAccountId, accountSum)
        List<Map<String, Object>> accounts = new ArrayList<Map<String, Object>>();

        // the childrenTotal field here is added only to check balances later, it is not used by GLAccountTree
        accounts.add(UtilMisc.<String, Object>toMap("glAccountId", "100000", "parentGlAccountId", null,     "accountSum", new BigDecimal(10.0), "childrenTotal" , new BigDecimal(280.0)));
        accounts.add(UtilMisc.<String, Object>toMap("glAccountId", "200000", "parentGlAccountId", null,     "accountSum", new BigDecimal(100.0), "childrenTotal", new BigDecimal(0.0)));
        accounts.add(UtilMisc.<String, Object>toMap("glAccountId", "110000", "parentGlAccountId", "100000", "accountSum", new BigDecimal(20.0), "childrenTotal",  new BigDecimal(0.0)));
        accounts.add(UtilMisc.<String, Object>toMap("glAccountId", "120000", "parentGlAccountId", "100000", "accountSum", new BigDecimal(55.0), "childrenTotal",  new BigDecimal(205.0)));
        accounts.add(UtilMisc.<String, Object>toMap("glAccountId", "300000", "parentGlAccountId", null,     "accountSum", new BigDecimal(310.0), "childrenTotal", new BigDecimal(0.0)));
        accounts.add(UtilMisc.<String, Object>toMap("glAccountId", "121000", "parentGlAccountId", "120000", "accountSum", new BigDecimal(205.0), "childrenTotal", new BigDecimal(0.0)));

        // for testing balances later
        Map<String, Map> accountsById = new HashMap();
        for (Map account : accounts) {
            accountsById.put((String)account.get("glAccountId"), account);
        }

        GLAccountTree tree = new GLAccountTree(delegator, organizationPartyId, currencyUomId, accounts);

        // test total balance of the Tree (10 + 100 + 20 + 55 + 310 + 205) = 700
        assertEquals("Total Balance in tree incorrect", tree.getTotalBalance(), new BigDecimal(700.0));

        // test account balances
        TreeSet<GLAccountInTree> accountsInTree = tree.getRootAccounts();
        for (GLAccountInTree ait : accountsInTree) {
            String accountId = ait.glAccountId;
            // test account balance
            assertEquals("Balance in tree incorrect for account " + accountId, ait.balance, (BigDecimal) accountsById.get(accountId).get("accountSum"));
            // test children balance
            assertEquals("Balance in tree incorrect for account " + accountId+" children", ait.getBalanceOfChildren(),  (BigDecimal) accountsById.get(accountId).get("childrenTotal"));
        }

        // test JSON output
        String json = tree.toJSONString();
        String expectedJson = "[{glAccountId:'100000',name:'ASSETS',type:'root',balanceOfSelf:10.00,balanceOfSelfAndChildren:290.00,debitCredit:'DEBIT',children:[{reference:'110000'},{reference:'120000'}]},{glAccountId:'110000',name:'CASH',type:'leaf',balanceOfSelf:20.00,balanceOfSelfAndChildren:20.00,debitCredit:'DEBIT'},{glAccountId:'120000',name:'ACCOUNTS RECEIVABLE',type:'leaf',balanceOfSelf:55.00,balanceOfSelfAndChildren:260.00,debitCredit:'DEBIT',children:[{reference:'121000'}]},{glAccountId:'121000',name:'ACCOUNTS RECEIVABLE - TRADE',type:'leaf',balanceOfSelf:205.00,balanceOfSelfAndChildren:205.00,debitCredit:'DEBIT'},{glAccountId:'200000',name:'LIABILITIES',type:'root',balanceOfSelf:100.00,balanceOfSelfAndChildren:100.00,debitCredit:'CREDIT'},{glAccountId:'300000',name:'OWNERS EQUITY AND NET WORTH',type:'root',balanceOfSelf:310.00,balanceOfSelfAndChildren:310.00,debitCredit:'CREDIT'}]";
        // dont using assertEquals as the output gets truncated, and it reads better like this
        assertTrue("JSON output incorrect, got:\n" + json + "\nexpected:\n" + expectedJson, json.equals(expectedJson));

    }

}
