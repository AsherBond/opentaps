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
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastList;
import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.AcctgTrans;
import org.opentaps.base.entities.AcctgTransEntry;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Test case superclass for all financials tests.  This defines asserts which are useful for testing
 * transactions, etc.
 */
public class FinancialsTestCase extends OpentapsTestCase {

    private static final String MODULE = FinancialsTestCase.class.getName();

    public static final List<String> transEntryFieldsToCompare = Arrays.asList(new String[] {"partyId", "roleTypeId", "productId", "glAccountId", "organizationPartyId", "currencyUomId", "debitCreditFlag"});
    private static final String TEST_TRANSACTIONS = "TEST_ACCTG_TRANS";
    public String organizationPartyId = "Company";
    public String defaultCostingMethodId;

    protected GenericValue demofinadmin = null;
    protected InvoiceRepositoryInterface invoiceRepository;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        demofinadmin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demofinadmin"));
        invoiceRepository = billingDomain.getInvoiceRepository();

        // save the default organization costing method
        GenericValue organizationAcctgPref = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", organizationPartyId));
        defaultCostingMethodId = organizationAcctgPref.getString("costingMethodId");
    }

    @Override
    public void tearDown() throws Exception {
        // reset the default organization costing method
        setStandardCostingMethod(defaultCostingMethodId);

        super.tearDown();
        demofinadmin = null;
        invoiceRepository = null;
    }

    public void setStandardCostingMethod(String standardCostingMethod) throws GeneralException {
        GenericValue organizationAcctgPref = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", organizationPartyId));
        organizationAcctgPref.set("costingMethodId", standardCostingMethod);
        organizationAcctgPref.store();
    }

    /**
     * Special assertion to check that two sets of transactions are equivalent.  If either set is empty, this fails.
     * The comparison checks that all transaction headers have the same acctgTransTypeId, partyId and roleTypeId.
     * Then it checks that the sum of the entries grouped by the fields defined in transEntryFieldsToCompare
     * are equal for both set.
     *
     * This method accepts either a list of AcctgTrans GenericValues or a list of acctgTransId Strings.
     *
     * @param transactions1 the first <code>Collection</code>
     * @param transactions2 the second <code>Collection</code>
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void assertTransactionEquivalence(Collection transactions1, Collection transactions2) throws GenericEntityException {
        assertNotEmpty("Cannot assert transaction equivalence, first transaction set is empty.", transactions1);
        assertNotEmpty("Cannot assert transaction equivalence, second transaction set is empty.", transactions2);

        Set transIds1 = FastSet.newInstance();
        Set transIds2 = FastSet.newInstance();

        // if we're passed in string Ids, convert them to GenericValues, otherwise build the set of transaction Ids
        if (transactions1.iterator().next() instanceof String) {
            transIds1.addAll(transactions1);
            transactions1 = delegator.findByAnd("AcctgTrans", UtilMisc.toList(EntityCondition.makeCondition("acctgTransId", EntityOperator.IN, transactions1)));
            assertNotEmpty("Cannot assert transaction equivalence, first transaction set is empty.", transactions1);
        } else {
            for (GenericValue trans : (List<GenericValue>) transactions1) {
                transIds1.add(trans.get("acctgTransId"));
            }
        }
        if (transactions2.iterator().next() instanceof String) {
            transIds2.addAll(transactions2);
            transactions2 = delegator.findByAnd("AcctgTrans", UtilMisc.toList(EntityCondition.makeCondition("acctgTransId", EntityOperator.IN, transactions2)));
            assertNotEmpty("Cannot assert transaction equivalence, second transaction set is empty", transactions2);
        } else {
            for (GenericValue trans : (List<GenericValue>) transactions2) {
                transIds2.add(trans.get("acctgTransId"));
            }
        }

        Debug.logInfo("Comparing transaction set1 " + transIds1 + " vs set2 " + transIds2, MODULE);

        // make sure all headers match
        List<GenericValue> allTransactions = FastList.newInstance();
        allTransactions.addAll(transactions1);
        allTransactions.addAll(transactions2);
        GenericValue referenceTrans = allTransactions.remove(0);
        for (GenericValue trans : allTransactions) {
            assertFieldsEqual(referenceTrans, trans, "acctgTransTypeId");
            assertFieldsEqual(referenceTrans, trans, "partyId");
            assertFieldsEqual(referenceTrans, trans, "roleTypeId");
        }

        // compare the *sum* of transaction entries grouped by transEntryFieldsToCompare
        List fieldsToSelect = new ArrayList(transEntryFieldsToCompare);
        fieldsToSelect.add("amount");
        List<GenericValue> sums1 = delegator.findByCondition("AcctgTransEntryEquivalenceSum", EntityCondition.makeCondition("acctgTransId", EntityOperator.IN, transIds1), fieldsToSelect, null);
        List<GenericValue> sums2 = delegator.findByCondition("AcctgTransEntryEquivalenceSum", EntityCondition.makeCondition("acctgTransId", EntityOperator.IN, transIds2), fieldsToSelect, null);
        assertTrue("No transaction entries found for transactions " + transIds1, sums1.size() > 0);
        assertTrue("No transaction entries found for transactions " + transIds2, sums2.size() > 0);

        // also add up the debits and credits separately for each set, so we can make sure they are the same for both sets
        BigDecimal debits1 = new BigDecimal("0.0");
        BigDecimal debits2 = new BigDecimal("0.0");
        BigDecimal credits1 = new BigDecimal("0.0");
        BigDecimal credits2 = new BigDecimal("0.0");
        boolean sum2done = false;

        for (GenericValue sum1 : sums1) {
            BigDecimal amount1 = sum1.getBigDecimal("amount");

            for (GenericValue sum2 : sums2) {
                BigDecimal amount2 = sum2.getBigDecimal("amount");
                if (fieldsEqual(sum1, sum2, transEntryFieldsToCompare)) {
                    Debug.logInfo("Vector sum1 [" + amount1 + "] and sum2 [" + amount2 + "] for group " + sum1.getFields(transEntryFieldsToCompare), MODULE);
                    String message = "Transaction set " + transIds1 + " has sum [" + amount1 + "] but set " + transIds2 + " has sum [" + amount2 + "] for group " + sum1.getFields(transEntryFieldsToCompare) + ".";
                    assertEquals(message, amount1, amount2);
                }
                if (!sum2done && "D".equals(sum2.get("debitCreditFlag"))) {
                    debits2 = debits2.add(amount2);
                }
                if (!sum2done && "C".equals(sum2.get("debitCreditFlag"))) {
                    credits2 = credits2.add(amount2);
                }
            }
            sum2done = true;
            if ("D".equals(sum1.get("debitCreditFlag"))) {
                debits1 = debits1.add(amount1);
            }
            if ("C".equals(sum1.get("debitCreditFlag"))) {
                credits1 = credits1.add(amount1);
            }
        }

        Debug.logInfo("Debit/Credit Totals:  debits1 [" + debits1 + "] vs debits2 [" + debits2 + "] and credits1 [" + credits1 + "] vs credits2 [" + credits2 + "]", MODULE);
        assertEquals("Transactions " + transIds1 + " has total debits [" + debits1 + "], which is different from transactions " + transIds2 + " with total debits [" + debits2 + "].", debits2, debits1);
        assertEquals("Transactions " + transIds1 + " has total credits [" + credits1 + "], which is different from transactions " + transIds2 + " with total credits [" + credits2 + "].", credits2, credits1);
    }

    /**
     * Special assertion to check that two sets of transactions are equivalent.and ignore compare each party id
     *
     * This method accepts either a list of AcctgTrans GenericValues or a list of acctgTransId Strings.
     *
     * @param transactions1 the first <code>Collection</code>
     * @param transactions2 the second <code>Collection</code>
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void assertTransactionEquivalenceIgnoreParty(Collection transactions1, Collection transactions2) throws GenericEntityException {
        assertNotEmpty("Cannot assert transaction equivalence, first transaction set is empty.", transactions1);
        assertNotEmpty("Cannot assert transaction equivalence, second transaction set is empty.", transactions2);

        Set transIds1 = FastSet.newInstance();
        Set transIds2 = FastSet.newInstance();

        // if we're passed in string Ids, convert them to GenericValues, otherwise build the set of transaction Ids
        if (transactions1.iterator().next() instanceof String) {
            transIds1.addAll(transactions1);
            transactions1 = delegator.findByAnd("AcctgTrans", UtilMisc.toList(EntityCondition.makeCondition("acctgTransId", EntityOperator.IN, transactions1)));
            assertNotEmpty("Cannot assert transaction equivalence, first transaction set is empty.", transactions1);
        } else {
            for (GenericValue trans : (List<GenericValue>) transactions1) {
                transIds1.add(trans.get("acctgTransId"));
            }
        }
        if (transactions2.iterator().next() instanceof String) {
            transIds2.addAll(transactions2);
            transactions2 = delegator.findByAnd("AcctgTrans", UtilMisc.toList(EntityCondition.makeCondition("acctgTransId", EntityOperator.IN, transactions2)));
            assertNotEmpty("Cannot assert transaction equivalence, second transaction set is empty", transactions2);
        } else {
            for (GenericValue trans : (List<GenericValue>) transactions2) {
                transIds2.add(trans.get("acctgTransId"));
            }
        }

        Debug.logInfo("Comparing transaction set1 " + transIds1 + " vs set2 " + transIds2, MODULE);

        // make sure all headers match
        List<GenericValue> allTransactions = FastList.newInstance();
        allTransactions.addAll(transactions1);
        allTransactions.addAll(transactions2);
        GenericValue referenceTrans = allTransactions.remove(0);
        for (GenericValue trans : allTransactions) {
            assertFieldsEqual(referenceTrans, trans, "acctgTransTypeId");
            assertFieldsEqual(referenceTrans, trans, "roleTypeId");
        }

        // compare the *sum* of transaction entries grouped by transEntryFieldsToCompare
        List fieldsToSelect = new ArrayList(transEntryFieldsToCompare);
        fieldsToSelect.add("amount");
        List<GenericValue> sums1 = delegator.findByCondition("AcctgTransEntryEquivalenceSum", EntityCondition.makeCondition("acctgTransId", EntityOperator.IN, transIds1), fieldsToSelect, null);
        List<GenericValue> sums2 = delegator.findByCondition("AcctgTransEntryEquivalenceSum", EntityCondition.makeCondition("acctgTransId", EntityOperator.IN, transIds2), fieldsToSelect, null);
        assertTrue("No transaction entries found for transactions " + transIds1, sums1.size() > 0);
        assertTrue("No transaction entries found for transactions " + transIds2, sums2.size() > 0);

        // also add up the debits and credits separately for each set, so we can make sure they are the same for both sets
        BigDecimal debits1 = new BigDecimal("0.0");
        BigDecimal debits2 = new BigDecimal("0.0");
        BigDecimal credits1 = new BigDecimal("0.0");
        BigDecimal credits2 = new BigDecimal("0.0");
        boolean sum2done = false;

        for (GenericValue sum1 : sums1) {
            BigDecimal amount1 = sum1.getBigDecimal("amount");

            for (GenericValue sum2 : sums2) {
                BigDecimal amount2 = sum2.getBigDecimal("amount");
                if (fieldsEqual(sum1, sum2, transEntryFieldsToCompare)) {
                    Debug.logInfo("Vector sum1 [" + amount1 + "] and sum2 [" + amount2 + "] for group " + sum1.getFields(transEntryFieldsToCompare), MODULE);
                    String message = "Transaction set " + transIds1 + " has sum [" + amount1 + "] but set " + transIds2 + " has sum [" + amount2 + "] for group " + sum1.getFields(transEntryFieldsToCompare) + ".";
                    assertEquals(message, amount1, amount2);
                }
                if (!sum2done && "D".equals(sum2.get("debitCreditFlag"))) {
                    debits2 = debits2.add(amount2);
                }
                if (!sum2done && "C".equals(sum2.get("debitCreditFlag"))) {
                    credits2 = credits2.add(amount2);
                }
            }
            sum2done = true;
            if ("D".equals(sum1.get("debitCreditFlag"))) {
                debits1 = debits1.add(amount1);
            }
            if ("C".equals(sum1.get("debitCreditFlag"))) {
                credits1 = credits1.add(amount1);
            }
        }

        Debug.logInfo("Debit/Credit Totals:  debits1 [" + debits1 + "] vs debits2 [" + debits2 + "] and credits1 [" + credits1 + "] vs credits2 [" + credits2 + "]", MODULE);
        assertEquals("Transactions " + transIds1 + " has total debits [" + debits1 + "], which is different from transactions " + transIds2 + " with total debits [" + debits2 + "].", debits2, debits1);
        assertEquals("Transactions " + transIds1 + " has total credits [" + credits1 + "], which is different from transactions " + transIds2 + " with total credits [" + credits2 + "].", credits2, credits1);
    }

    /**
     * Verify that the trial balance from GlAccountOrganization balances on debit and credit sides.
     * @param organizationPartyId the organization to test for
     * @param decimals for rounding
     * @param rounding for rounding
     * @throws Exception if an error occurs
     */
    public void assertPostedBalancesEqual(String organizationPartyId, int decimals, RoundingMode rounding) throws Exception {
        assertTrue("Debit and credit totals on GlAccountOrganization do not equal", UtilFinancial.isGlAccountOrganizationInBalance(organizationPartyId, delegator, decimals, rounding));
    }

    /**
     * Verify that the trial balance stored in GlAccountHistory balances on debit and credit sides for a given time period.
     * @param organizationPartyId the organization to test for
     * @param customTimePeriodId the time period to test the balance for
     * @param decimals for rounding
     * @param rounding for rounding
     * @throws Exception if an error occurs
     */
    public void assertGlAccountHistoryBalancesEqual(String organizationPartyId, String customTimePeriodId, int decimals, RoundingMode rounding) throws Exception {
        assertTrue("Debit and credit totals on GlAccountHistory do not equal for time period ["  + customTimePeriodId + "]", UtilFinancial.isGlAccountHistoryInBalance(organizationPartyId, customTimePeriodId, delegator, decimals, rounding));
    }

    /**
     * Verify that all trial balances for GlAccountOrganization and GlAccountHistory of all time periods equal.
     * @param organizationPartyId the organization to test for
     * @param decimals for rounding
     * @param rounding for rounding
     * @throws Exception if an error occurs
     */
    public void assertAllTrialBalancesEqual(String organizationPartyId, int decimals, RoundingMode rounding) throws Exception {
        // not using the UtilFinancial.areTrialBalancesEqual method here because this will give better junit logging error of which one failed
        assertPostedBalancesEqual(organizationPartyId, decimals, rounding);
        List<GenericValue> timePeriods = delegator.findByAndCache("CustomTimePeriod", UtilMisc.toMap("organizationPartyId", organizationPartyId));
        for (GenericValue timePeriod : timePeriods) {
            assertGlAccountHistoryBalancesEqual(organizationPartyId, timePeriod.getString("customTimePeriodId"), decimals, rounding);

        }
    }

    /**
     * A convenience method to return a set of acctgTransIds since the passed in Timestamp, based on the conditions passed in
     * It actually searches the AcctgTrans entity, so it is more limited than the getAcctgTransSinceDate method (because the created
     *  stamp dooesn't exist in a view entity).
     * It's required to have an acctgTransTypeId condition, otherwise other tests that might use this will collide in
     * another thread.
     * @param conditions initial <code>List</code> of condition
     * @param since only get the transaction with the created stamp since this date
     * @param delegator a <code>Delegator</code> value
     * @return the <code>Set</code> of AcctgTransIds matching the conditions
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAcctgTransCreatedSinceDate(List conditions, Timestamp since, Delegator delegator) throws GenericEntityException {
        conditions.add(EntityCondition.makeCondition("createdStamp", EntityOperator.GREATER_THAN_EQUAL_TO, since));
        conditions.add(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.NOT_EQUAL, TEST_TRANSACTIONS));
        List<GenericValue> matches = delegator.findByAnd("AcctgTrans", conditions);
        Set<String> acctgTransIds = FastSet.newInstance();
        for (GenericValue match : matches) {
            acctgTransIds.add(match.getString("acctgTransId"));
        }
        return acctgTransIds;
    }

    /**
     * As above, but accepts EntityCondition instead of a List of EntityConditions.
     * @param condition initial <code>EntityCondition</code>
     * @param since only get the transaction with the created stamp since this date
     * @param delegator a <code>Delegator</code> value
     * @return the <code>Set</code> of AcctgTransIds matching the conditions
     * @exception GenericEntityException if an error occurs
     */
    public Set<String> getAcctgTransCreatedSinceDate(EntityCondition condition, Timestamp since, Delegator delegator) throws GenericEntityException {
        return getAcctgTransSinceDate(UtilMisc.toList(condition), since, delegator);
    }

    /**
     * A convenience method to return a set of acctgTransIds since the passed in Timestamp, based on the conditions passed in
     * It actually searches the AcctgTransAndEntries view entity, which allows matching entries by productId and so on.
     * It's required to have an acctgTransTypeId condition, otherwise other tests that might use this will collide in
     * another thread.
     * @param conditions initial <code>List</code> of condition
     * @param since only get the transaction with the transaction date since this date
     * @param delegator a <code>Delegator</code> value
     * @return the <code>Set</code> of AcctgTransIds matching the conditions
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAcctgTransSinceDate(List conditions, Timestamp since, Delegator delegator) throws GenericEntityException {
        conditions.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, since));
        conditions.add(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.NOT_EQUAL, TEST_TRANSACTIONS));
        List<GenericValue> matches = delegator.findByAnd("AcctgTransAndEntries", conditions);
        Set<String> acctgTransIds = FastSet.newInstance();
        for (GenericValue match : matches) {
            acctgTransIds.add(match.getString("acctgTransId"));
        }
        return acctgTransIds;
    }

    /**
     * As above, but accepts EntityCondition instead of a List of EntityConditions.
     * @param condition initial <code>EntityCondition</code>
     * @param since only get the transaction with the transaction date since this date
     * @param delegator a <code>Delegator</code> value
     * @return the <code>Set</code> of AcctgTransIds matching the conditions
     * @exception GenericEntityException if an error occurs
     */
    public Set<String> getAcctgTransSinceDate(EntityCondition condition, Timestamp since, Delegator delegator) throws GenericEntityException {
        return getAcctgTransSinceDate(UtilMisc.toList(condition), since, delegator);
    }


    /**
     * Base method for copying all AcctgTrans and AcctgTransEntry to another organization.
     * 1. finds all AcctgTrans where at least one of the AcctgTransEntry.organizationPartyId=fromOrganizationPartyId
     * 2. Creates a duplicate AcctgTrans and AcctgTransEntry
     * 3. set AcctgTransEntry.organizationPartyId to toOrganizationPartyId
     * 4. set AcctgTrans.isPosted=N, AcctgTrans.postedDate=null
     * 5. return a List<String> of the acctgTransId's
     * @param fromOrganizationPartyId the partyId of the <code>Party</code> to use as a template
     * @param toOrganizationPartyId the partyId of the <code>Party</code> to use as a target
     * @return the acctgTransId's of the new <code>List<String></code>
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public List<String> copyAllAcctgTransAndEntries(String fromOrganizationPartyId, String toOrganizationPartyId) throws GeneralException {
        List<String> acctgTransIds = new FastList();
        Infrastructure infrastructure = new Infrastructure(dispatcher);
        Session session = infrastructure.getSession();
        // get AcctgTrans and AcctgTransEntry by hibernate
        String hql = "select distinct eo.acctgTrans from AcctgTransEntry eo where eo.organizationPartyId = :partyId";
        Query query = session.createQuery(hql);
        query.setString("partyId", fromOrganizationPartyId);
        List<AcctgTrans> acctgTranes = query.list();
        // duplicate AcctgTrans and AcctgTransEntry by delegator
        // if we store the AcctgTrans/AcctgTransEntry by hibernate session, the tearDown method will raise a transaction to say no active transaction to commit
        // because organizationAcctgPref is load by delegator on previous transaction and hibernate session commit it before
        // so we using delegator to store these new entities.
        for (AcctgTrans acctgTrans : acctgTranes) {
            if (org.opentaps.base.constants.AcctgTransTypeConstants.PERIOD_CLOSING.equals(acctgTrans.getAcctgTransTypeId())) {
                Debug.logInfo("Not copying acctg trans [" + acctgTrans.getAcctgTransId() + "] from [" + fromOrganizationPartyId + "] to [" + toOrganizationPartyId + "] because it is a period closing transaction", MODULE);
                continue;
            }
            Map<String, Object> acctgTransMap = acctgTrans.toMap();
            acctgTransMap.put("acctgTransId", delegator.getNextSeqId("AcctgTrans"));
            acctgTransMap.put("isPosted", "N");
            acctgTransMap.put("postedDate", null);
            GenericValue toAcctgTrans = delegator.makeValue("AcctgTrans", acctgTransMap);
            toAcctgTrans.create();
            // store the new AcctgTransId into return list
            acctgTransIds.add(toAcctgTrans.getString("acctgTransId"));
            for (AcctgTransEntry acctgTransEntry : acctgTrans.getAcctgTransEntrys()) {
                // Creates a duplicate AcctgTransEntry
                Map<String, Object> acctgTransEntryMap = acctgTransEntry.toMap();
                acctgTransEntryMap.put("acctgTransId", toAcctgTrans.getString("acctgTransId"));
                acctgTransEntryMap.put("organizationPartyId", toOrganizationPartyId);
                GenericValue toAcctgTransEntry = delegator.makeValue("AcctgTransEntry", acctgTransEntryMap);
                toAcctgTransEntry.create();
            }
        }
        return acctgTransIds;
    }
}
