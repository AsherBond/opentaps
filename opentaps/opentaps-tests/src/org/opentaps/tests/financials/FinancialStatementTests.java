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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.opensourcestrategies.financials.util.UtilFinancial;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.CustomTimePeriod;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;

/**
 * Verify financial statements are correct based on the data in opentaps/opentaps-tests/data/financials/StatementsTestData.xml
 * and according to the spreadsheet opentaps/opentaps-tests/scripts/spreadsheets/financial statements tests.xls.
 */
public class FinancialStatementTests extends FinancialsTestCase {

    private static final String STATEMENT_TEST_ORG = "STATEMENT-TEST";
    private static final String STATEMENT_DETAILS_TEST_ORG = "STATEMENT-DETAILS";
    private static final String QUARTER1 = "ST2008Q1";
    private static final String QUARTER2 = "ST2008Q2";
    private static final String YEAR0 = "ST2007";
    private static final String YEAR1 = "ST2008";
    private static final String FISCAL_TYPE = "ACTUAL";
    private static final int DECIMALS = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    String trialBalanceOrganizationPartyId = null;     // separate organization for trial balance tests

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // post all test transactions
        List<GenericValue> transactionsToPost = delegator.findByAnd("AcctgTrans", UtilMisc.toList(
                EntityCondition.makeCondition("acctgTransId", EntityOperator.LIKE, "STATEMENT-TEST-%"),
                EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "N")
        ));
        for (GenericValue transaction : transactionsToPost) {
            dispatcher.runSync("postAcctgTrans", UtilMisc.toMap("acctgTransId", transaction.getString("acctgTransId"), "userLogin", demofinadmin));
        }

        // close time periods STD2007 and STD2007Q4 (if not closed already)
        GenericValue timePeriod = delegator.findByPrimaryKey("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", "STD2007"));
        assertNotNull("Time period STD2007 not found.", timePeriod);
        if (!"Y".equals(timePeriod.getString("isClosed"))) {
            runAndAssertServiceSuccess("closeTimePeriod", UtilMisc.toMap("userLogin", demofinadmin, "organizationPartyId", STATEMENT_DETAILS_TEST_ORG, "customTimePeriodId", "STD2007"));
        }
        timePeriod = delegator.findByPrimaryKey("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", "STD2007Q4"));
        if (!"Y".equals(timePeriod.getString("isClosed"))) {
            runAndAssertServiceSuccess("closeTimePeriod", UtilMisc.toMap("userLogin", demofinadmin, "organizationPartyId", STATEMENT_DETAILS_TEST_ORG, "customTimePeriodId", "STD2007Q4"));
        }

        // set up separate trial balance tests' organization once
        if (trialBalanceOrganizationPartyId == null) {
            trialBalanceOrganizationPartyId = setUpOrganizationForTrialBalance(organizationPartyId);
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * To prevent trial balance tests and the other statement tests to "collide" due to the closing of time periods, we create a new
     * organization just for trial balances, based on STATEMENT_TEST_ORG, and close all its time periods first.
     * @param organizationPartyId the organization party id to copy
     * @return the new organization party id
     * @throws Exception if an error occurs
     */
    private String setUpOrganizationForTrialBalance(String organizationPartyId) throws Exception {
        // copy the organization
        String newOrganizationPartyId = this.createOrganizationFromTemplate(STATEMENT_TEST_ORG, "Organization for Trial Balance Tests " + UtilDateTime.nowTimestamp());

        // copy all its acctg trans and entries and post them all
        List<String> acctgTransIds = this.copyAllAcctgTransAndEntries(STATEMENT_TEST_ORG, newOrganizationPartyId);
        for (String acctgTransId : acctgTransIds) {
            runAndAssertServiceSuccess("postAcctgTrans", UtilMisc.toMap("userLogin", demofinadmin, "acctgTransId", acctgTransId));
        }

        return newOrganizationPartyId;
    }

    /*
     * closes all fiscal years of the organization
     */
    private void closeAllFiscalYears(String organizationPartyId) throws Exception {
        // Note this is hardcoded right now.  A better way is to get the fiscal years from STATEMENT_TEST_ORG
        // and then find the same time periods
        closeFiscalYear(organizationPartyId, UtilDateTime.toTimestamp(1, 1, 2008, 0, 0, 0));
        closeFiscalYear(organizationPartyId, UtilDateTime.toTimestamp(1, 1, 2009, 0, 0, 0));
    }
    
    /**
     * close fiscal year ending in the endingDate for the organization
     * @param organizationPartyId
     * @param endingDate
     * @throws Exception
     */
    
    private void closeFiscalYear(String organizationPartyId, Timestamp endingDate) throws Exception {
         // get the fiscal year ends.  
        OrganizationRepositoryInterface orgRepo = organizationDomain.getOrganizationRepository();
        List<CustomTimePeriod> fiscalYearsToClose = orgRepo.getOpenFiscalTimePeriods(organizationPartyId, endingDate);
        
        // close all the fiscal years
        for (CustomTimePeriod fiscalYear : fiscalYearsToClose) {
            if (!"Y".equals(fiscalYear.getIsClosed())) {
                runAndAssertServiceSuccess("closeTimePeriod", UtilMisc.toMap("organizationPartyId", fiscalYear.getOrganizationPartyId(), "customTimePeriodId", fiscalYear.getCustomTimePeriodId(), "userLogin", demofinadmin));
            }
        }
            
    }
    
    /**
     * Verify the trial balance by gl account class is correct for a past date.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testTrialBalanceForAsOfDate() throws Exception {
        // first close all time periods
        closeAllFiscalYears(trialBalanceOrganizationPartyId);
        
        Timestamp asOfDate = UtilDateTime.toTimestamp(7, 1, 2008, 0, 0, 0);
        Map params = UtilMisc.toMap("organizationPartyId", trialBalanceOrganizationPartyId, "asOfDate", asOfDate, "glFiscalTypeId", "ACTUAL", "userLogin", admin);
        Map results = dispatcher.runSync("getTrialBalanceForDate", params);

        Map expectedAssetAccountBalances = UtilMisc.toMap("110000", new BigDecimal("200000.00"), "171000", new BigDecimal("300000.00"), "185000", new BigDecimal("-50000.00"), "191200", new BigDecimal("200000.00"), "191900", new BigDecimal("-40000.00"));
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("assetAccountBalances")), expectedAssetAccountBalances);
        Map expectedLiabilityAccountBalances = UtilMisc.toMap("240000", new BigDecimal("300000.00"));
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("liabilityAccountBalances")), expectedLiabilityAccountBalances);
        Map expectedEquityAccountBalances = UtilMisc.toMap("340000", new BigDecimal("10000.00"), "341000", new BigDecimal("300000.00"), "336000", new BigDecimal("0.0"));
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("equityAccountBalances")), expectedEquityAccountBalances);

        assertEquals("There should be no revenue account balances", new BigDecimal(((Map) results.get("revenueAccountBalances")).keySet().size()), BigDecimal.ZERO);
        assertEquals("There should be no expense account balances", new BigDecimal(((Map) results.get("expenseAccountBalances")).keySet().size()), BigDecimal.ZERO);

        Map expectedIncomeAccountBalances = UtilMisc.toMap("890000", BigDecimal.ZERO);
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("incomeAccountBalances")), expectedIncomeAccountBalances);
        
        assertEquals("There should be no other account balances", new BigDecimal(((Map) results.get("otherAccountBalances")).keySet().size()), BigDecimal.ZERO);
    }

    /**
     * Verify trial balance for a later date which includes all categories of gl accounts: assets, liabilities, equity, revenue, expense, income.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testComplexTrialBalanceForAsOfDate() throws Exception {
        // close FY ending 7/1/08
        closeFiscalYear(trialBalanceOrganizationPartyId, UtilDateTime.toTimestamp(1, 1, 2008, 0, 0, 0));

        // trial balance prior to closing time period should have income, expenses, and the net income offset in profit/loss account
        Timestamp asOfDate = UtilDateTime.toTimestamp(7, 1, 2009, 0, 0, 0);
        Map<String, Object> params = UtilMisc.toMap("organizationPartyId", trialBalanceOrganizationPartyId, "asOfDate", asOfDate, "glFiscalTypeId", "ACTUAL", "userLogin", admin);
        Map<String, Object> results = dispatcher.runSync("getTrialBalanceForDate", params);

        Map<String, BigDecimal> expectedAssetAccountBalances = new HashMap<String, BigDecimal>();
        expectedAssetAccountBalances.put("110000", new BigDecimal("136743.22"));
        expectedAssetAccountBalances.put("120000", new BigDecimal("225100.00"));
        expectedAssetAccountBalances.put("121800", new BigDecimal("500.00"));
        expectedAssetAccountBalances.put("140000", new BigDecimal("165000.00"));
        expectedAssetAccountBalances.put("141000", new BigDecimal("53000.00"));
        expectedAssetAccountBalances.put("171000", new BigDecimal("350000.00"));
        expectedAssetAccountBalances.put("185000", new BigDecimal("-62500.00"));
        expectedAssetAccountBalances.put("191200", new BigDecimal("200000.00"));
        expectedAssetAccountBalances.put("191900", new BigDecimal("-42500.00"));
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("assetAccountBalances")), expectedAssetAccountBalances);

        Map<String, BigDecimal> expectedLiabilityAccountBalances = new HashMap<String, BigDecimal>();
        expectedLiabilityAccountBalances.put("210000", new BigDecimal("248769.00"));
        expectedLiabilityAccountBalances.put("221300", new BigDecimal("1200.00"));
        expectedLiabilityAccountBalances.put("222100", new BigDecimal("12500.00"));
        expectedLiabilityAccountBalances.put("222200", new BigDecimal("2000.00"));
        expectedLiabilityAccountBalances.put("222300", new BigDecimal("1000.00"));
        expectedLiabilityAccountBalances.put("222400", new BigDecimal("4000.00"));
        expectedLiabilityAccountBalances.put("222600", new BigDecimal("5000.00"));
        expectedLiabilityAccountBalances.put("223100", new BigDecimal("2000.00"));
        expectedLiabilityAccountBalances.put("223200", new BigDecimal("1000.00"));
        expectedLiabilityAccountBalances.put("223500", new BigDecimal("5400.00"));
        expectedLiabilityAccountBalances.put("224106", new BigDecimal("1700.00"));
        expectedLiabilityAccountBalances.put("224140", new BigDecimal("2600.00"));
        expectedLiabilityAccountBalances.put("240000", new BigDecimal("330000.00"));
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("liabilityAccountBalances")), expectedLiabilityAccountBalances);

        Map<String, BigDecimal> expectedEquityAccountBalances = UtilMisc.toMap("340000", new BigDecimal("10000.00"), "341000", new BigDecimal("300000.00"), "336000", new BigDecimal("99174.22"), "334000", new BigDecimal("-1000"));
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("equityAccountBalances")), expectedEquityAccountBalances);

        Map<String, BigDecimal> expectedRevenueAccountBalances = new HashMap<String, BigDecimal>();
        expectedRevenueAccountBalances.put("400000", new BigDecimal("374000"));
        expectedRevenueAccountBalances.put("408000", new BigDecimal("42000"));
        
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("revenueAccountBalances")), expectedRevenueAccountBalances);

        Map<String, BigDecimal> expectedExpenseAccountBalances = new HashMap<String, BigDecimal>();
        expectedExpenseAccountBalances.put("500000", new BigDecimal("157000.00"));
        expectedExpenseAccountBalances.put("510000", new BigDecimal("24625.78"));
        expectedExpenseAccountBalances.put("601000", new BigDecimal("46300"));
        expectedExpenseAccountBalances.put("611000", new BigDecimal("24000"));
        expectedExpenseAccountBalances.put("518100", new BigDecimal("-250"));
        expectedExpenseAccountBalances.put("675000", new BigDecimal("12500"));
        expectedExpenseAccountBalances.put("781000", new BigDecimal("8000"));
        expectedExpenseAccountBalances.put("821000", new BigDecimal("10000"));
        expectedExpenseAccountBalances.put("902000", new BigDecimal("3000"));
        expectedExpenseAccountBalances.put("608000", new BigDecimal("1200"));

        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("expenseAccountBalances")), expectedExpenseAccountBalances);

        Map expectedIncomeAccountBalances = UtilMisc.toMap("890000", new BigDecimal("99174.22"));
        // but these are actually credits, so we'll check them to make sure they're on the credit side
        expectedIncomeAccountBalances.put("800000", new BigDecimal("750"));
        expectedIncomeAccountBalances.put("810000", new BigDecimal("500"));
        
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("incomeAccountBalances")), expectedIncomeAccountBalances);
      
        Map<String, BigDecimal> expectedIncomeAccountCredits = new HashMap<String, BigDecimal>();
        expectedIncomeAccountCredits.put("800000", new BigDecimal("750"));
        expectedIncomeAccountCredits.put("810000", new BigDecimal("500"));
        
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("incomeAccountCredits")), expectedIncomeAccountCredits);
      
        
        assertEquals("There should be no other account balances", new BigDecimal(((Map) results.get("otherAccountBalances")).keySet().size()), BigDecimal.ZERO);

        assertEquals("Total debit balance not correct",  (BigDecimal) results.get("totalDebits"), new BigDecimal("1442593.22"));
        assertEquals("Total credit balance not correct", (BigDecimal) results.get("totalCredits"), new BigDecimal("1442593.22"));
        assertEquals("Net balance is not zero", (BigDecimal) results.get("totalBalances"), BigDecimal.ZERO);
        
        // now close FY 2009 and check again as of 7/1/09.  There should be no revenue, expense, income, or other accounts, and the total debit/credit should have changed
        closeFiscalYear(trialBalanceOrganizationPartyId, UtilDateTime.toTimestamp(1, 1, 2009, 0, 0, 0));
       
        results = dispatcher.runSync("getTrialBalanceForDate", params);
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("assetAccountBalances")), expectedAssetAccountBalances);
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("liabilityAccountBalances")), expectedLiabilityAccountBalances);
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("equityAccountBalances")), expectedEquityAccountBalances);

        assertEquals("There should be no other account balances", new BigDecimal(((Map) results.get("revenueAccountBalances")).keySet().size()), BigDecimal.ZERO);
        assertEquals("There should be no other account balances", new BigDecimal(((Map) results.get("expenseAccountBalances")).keySet().size()), BigDecimal.ZERO);
        
        expectedIncomeAccountBalances = new HashMap<String, BigDecimal>();
        expectedIncomeAccountBalances.put("800000", BigDecimal.ZERO);
        
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("incomeAccountBalances")), expectedIncomeAccountBalances);
        assertEquals("There should be no other account balances", new BigDecimal(((Map) results.get("otherAccountBalances")).keySet().size()), BigDecimal.ZERO);

        // also run the trial balance at the end of 6/30/09 to make sure the right time period is found -- same results should apply
        params.put("asOfDate", UtilDateTime.toTimestamp(6, 30, 2009, 23, 59, 59));
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("assetAccountBalances")), expectedAssetAccountBalances);
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("liabilityAccountBalances")), expectedLiabilityAccountBalances);
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("equityAccountBalances")), expectedEquityAccountBalances);
        assertEquals("There should be no other account balances", new BigDecimal(((Map) results.get("revenueAccountBalances")).keySet().size()), BigDecimal.ZERO);
        assertEquals("There should be no other account balances", new BigDecimal(((Map) results.get("expenseAccountBalances")).keySet().size()), BigDecimal.ZERO);
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("incomeAccountBalances")), expectedIncomeAccountBalances);
        assertEquals("There should be no other account balances", new BigDecimal(((Map) results.get("otherAccountBalances")).keySet().size()), BigDecimal.ZERO);

    }

    /**
     * Test FinancialAsserts.getFinancialBalances works correctly with tags.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testFinancialAssertsByTag() throws Exception {
        FinancialAsserts fa = new FinancialAsserts(this, STATEMENT_DETAILS_TEST_ORG, demofinadmin);
        Map financialBalancesByTag = fa.getFinancialBalances(UtilDateTime.nowTimestamp(), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_A"));
        assertEquals("GL account balance for account 110000 TEST_STDTL_TAG1_A is not correct",  (BigDecimal) financialBalancesByTag.get("110000"), new BigDecimal("-25100.00"));
        assertEquals("GL account balance for account 111400 TEST_STDTL_TAG1_A is not correct", (BigDecimal) financialBalancesByTag.get("111400"), new BigDecimal("300000.00"));
        assertEquals("GL account balance for account 680000 TEST_STDTL_TAG1_A is not correct", (BigDecimal) financialBalancesByTag.get("680000"), new BigDecimal("6070.00"));
    }

    /**
     * Test trial balance service with accounting tags.
     * NOTE: This trial balance does not actually balance in net debits and credits because of the way the tags are set up.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testTrialBalanceWithTags() throws Exception {
        Timestamp asOfDate = UtilDateTime.toTimestamp(7, 1, 2009, 0, 0, 0);
        Map params = UtilMisc.toMap("organizationPartyId", "STATEMENT-DETAILS", "asOfDate", asOfDate, "glFiscalTypeId", "ACTUAL", "userLogin", admin);
        params.put("tag1", "TEST_STDTL_TAG1_C");
        params.put("tag2", "TEST_STDTL_TAG2_A");

        Map results = dispatcher.runSync("getTrialBalanceForDate", params);

        // Because assertMapCorrect  will only check the values for the keys in the expected Map,  if we pass an empty map,  it will not check anything.
        //  So, to check that there are no accounts, we need to check the size of the key set
        assertEquals("There should be no asset account balances", new BigDecimal(((Map) results.get("assetAccountBalances")).keySet().size()), BigDecimal.ZERO);
        assertEquals("There should be no liability account balances", new BigDecimal(((Map) results.get("liabilityAccountBalances")).keySet().size()), BigDecimal.ZERO);
        assertEquals("There should be no revenue account balances", new BigDecimal(((Map) results.get("revenueAccountBalances")).keySet().size()), BigDecimal.ZERO);
        assertEquals("There should be no other account balances", new BigDecimal(((Map) results.get("otherAccountBalances")).keySet().size()), BigDecimal.ZERO);
        
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("equityAccountBalances")), UtilMisc.toMap("336000", new BigDecimal("-59490.00")));
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("incomeAccountBalances")), UtilMisc.toMap("890000", new BigDecimal("-59490.00")));
        
        Map<String, BigDecimal> expectedExpenseAccountBalances = UtilMisc.toMap("601000", new BigDecimal("50000.00"), "604100", new BigDecimal("3000.00"), "605100", new BigDecimal("5000.00"), "608000", new BigDecimal("1200.0"), "680000", new BigDecimal("290.00"));
        assertMapCorrect(UtilFinancial.getBalancesByGlAccountId((Map<GenericValue, BigDecimal>) results.get("expenseAccountBalances")), expectedExpenseAccountBalances);

    }
    /**
     * Test financial statements after closing time periods.
     * @exception Exception if an error occurs
     */
    public void testFinancialStatements() throws Exception {
        verifyFinancialStatements("Before closing time period ", STATEMENT_TEST_ORG);

        runAndAssertServiceSuccess("closeAllTimePeriods", UtilMisc.toMap("organizationPartyId", STATEMENT_TEST_ORG, "customTimePeriodId", YEAR0, "userLogin", demofinadmin));
        // financial statements should still be the same
        verifyFinancialStatements("After closing time period " + YEAR0 + " ", STATEMENT_TEST_ORG);

        runAndAssertServiceSuccess("closeAllTimePeriods", UtilMisc.toMap("organizationPartyId", STATEMENT_TEST_ORG, "customTimePeriodId", QUARTER1, "userLogin", demofinadmin));
        verifyFinancialStatements("After closing time period " + QUARTER1 + " ", STATEMENT_TEST_ORG);

        runAndAssertServiceSuccess("closeAllTimePeriods", UtilMisc.toMap("organizationPartyId", STATEMENT_TEST_ORG, "customTimePeriodId", QUARTER2, "userLogin", demofinadmin));
        verifyFinancialStatements("After closing time period " + QUARTER2 + " ", STATEMENT_TEST_ORG);

        runAndAssertServiceSuccess("closeAllTimePeriods", UtilMisc.toMap("organizationPartyId", STATEMENT_TEST_ORG, "customTimePeriodId", YEAR1, "userLogin", demofinadmin));
        verifyFinancialStatements("After closing time period " + YEAR1 + " ", STATEMENT_TEST_ORG);
    }

    /**
     * Test of detailed income statement using the tags.
     * @throws Exception if an error occurs
     */
    public void testDetailedIncomeStatements() throws Exception {
        // Calculate income statement for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_A  during time period STD2008 and verify that net income is -94270
        verifyIncomeStatement("With tag1 = TEST_STDTL_TAG1_A ", STATEMENT_DETAILS_TEST_ORG, "STD2008", null, null, null, new BigDecimal("-94270.00"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_A"));
        // Calculate income statement for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_B, tag2 = TEST_STDTL_TAG2_B  during time period STD2008 and verify that net income is -71530
        verifyIncomeStatement("With tag1 = TEST_STDTL_TAG1_B, tag2 = TEST_STDTL_TAG2_B ", STATEMENT_DETAILS_TEST_ORG, "STD2008", null, null, null, new BigDecimal("-71530.00"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_B", "tag2", "TEST_STDTL_TAG2_B"));
        // Calculate income statement for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_A, tag2 = TEST_STDTL_TAG2_C, tag3 = TEST_STDTL_TAG3_B  during time period STD2008 and verify that net income is -8700
        verifyIncomeStatement("With tag1 = TEST_STDTL_TAG1_A, tag2 = TEST_STDTL_TAG2_C, tag3 = TEST_STDTL_TAG2_B ", STATEMENT_DETAILS_TEST_ORG, "STD2008", null, null, null, new BigDecimal("-8700.00"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_A", "tag2", "TEST_STDTL_TAG2_C", "tag3", "TEST_STDTL_TAG3_B"));
        // Calculate income statement for STATEMENT-DETAILS for all tags set to none is 457000
        verifyIncomeStatement("With all tags set to NONE", STATEMENT_DETAILS_TEST_ORG, "STD2008", null, null, null, new BigDecimal("457000.00"), UtilMisc.toMap("tag1", "NONE", "tag2", "NONE", "tag3", "NONE"));
    }

    /**
     * Test detailed financials balance sheet for each set of tags.
     * @throws Exception if an error occurs
     */
    public void testDetailedBalanceSheets() throws Exception {
        // Verify balance sheet for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_A during time period STD2008:
        //   Assets: 274900  Liabilities: 69170   Equities: 205730
        verifyBalanceSheet("With tag1 = TEST_STDTL_TAG1_A ", STATEMENT_DETAILS_TEST_ORG, "STD2008", new BigDecimal("274900"), new BigDecimal("69170"), new BigDecimal("205730"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_A"));

        // Verify balance sheet for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_B during time period STD2008:
        //   Assets: 174900  Liabilities: 47360   Equities: 127540
        verifyBalanceSheet("With tag1 = TEST_STDTL_TAG1_B ", STATEMENT_DETAILS_TEST_ORG, "STD2008", new BigDecimal("174900"), new BigDecimal("47360"), new BigDecimal("127540"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_B"));

        // Verify balance sheet for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_C during time period STD2008:
        //   Assets: 49800   Liabilities: 82920   Equities: -33120
        verifyBalanceSheet("With tag1 = TEST_STDTL_TAG1_C ", STATEMENT_DETAILS_TEST_ORG, "STD2008", new BigDecimal("49800"), new BigDecimal("82920"), new BigDecimal("-33120"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_C"));

        // verify balance sheet for STATEMENT-DETAILS for all tags set to none during time period STD2008:
        //   Assets: 2250000 Liabilities: 1093000   Equities: 1157000
        verifyBalanceSheet("With all tags set to NONE", STATEMENT_DETAILS_TEST_ORG, "STD2008", new BigDecimal("2250000"), new BigDecimal("1093000"), new BigDecimal("1157000"), UtilMisc.toMap("tag1", "NONE", "tag2", "NONE", "tag3", "NONE"));
    }

    /**
     * Tests cash flow statements using the detailed accounting tags.
     * @throws Exception if an error occurs
     */
    public void testDetailedCashFlowStatements() throws Exception {
        // Verify cashflow statement for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_A during time period STD2007:
        // Beginning = 0 Ending = 300000 Operating = 0 Investing = 0 Financing = 300000
        verifyCashflowStatement("With tag1 = TEST_STDTL_TAG1_A ", STATEMENT_DETAILS_TEST_ORG, "STD2007", BigDecimal.ZERO, new BigDecimal("300000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("300000"), new BigDecimal("300000"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_A"));

        // Verify cashflow statement for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_A during time period STD2008:
        // Beginning = 300000  Ending = 274900 Operating = -25100 Investing = 0 Financing = 0
        verifyCashflowStatement("With tag1 = TEST_STDTL_TAG1_A ", STATEMENT_DETAILS_TEST_ORG, "STD2008", new BigDecimal("300000"), new BigDecimal("274900"), new BigDecimal("-25100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-25100"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_A"));

        // Verify cashflow statement for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_B during time period STD2007:
        // Beginning = 0 Ending = 200000 Operating = 0 Investing = 0 Financing = 200000
        verifyCashflowStatement("With tag1 = TEST_STDTL_TAG1_B ", STATEMENT_DETAILS_TEST_ORG, "STD2007", BigDecimal.ZERO, new BigDecimal("200000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("200000"), new BigDecimal("200000"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_B"));

        // Verify cashflow statement for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_B during time period STD2008:
        // Beginning = 200000  Ending = 174900 Operating = -25100 Investing = 0 Financing = 0
        verifyCashflowStatement("With tag1 = TEST_STDTL_TAG1_B ", STATEMENT_DETAILS_TEST_ORG, "STD2008", new BigDecimal("200000"), new BigDecimal("174900"), new BigDecimal("-25100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-25100"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_B"));

        // Verify cashflow statement for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_C during time period STD2007:
        // Beginning = 0 Ending = 100000 Operating = 0 Investing = 0 Financing = 100000
        verifyCashflowStatement("With tag1 = TEST_STDTL_TAG1_C ", STATEMENT_DETAILS_TEST_ORG, "STD2007", BigDecimal.ZERO, new BigDecimal("100000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100000"), new BigDecimal("100000"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_C"));

        // Verify cashflow statement for STATEMENT-DETAILS and tag1 = TEST_STDTL_TAG1_C during time period STD2008:
        // Beginning = 100000  Ending = 49800 Operating = -50200 Investing = 0 Financing = 0
        verifyCashflowStatement("With tag1 = TEST_STDTL_TAG1_C ", STATEMENT_DETAILS_TEST_ORG, "STD2008", new BigDecimal("100000"), new BigDecimal("49800"), new BigDecimal("-50200"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-50200"), UtilMisc.toMap("tag1", "TEST_STDTL_TAG1_C"));

        // Verify cashflow statement for STATEMENT-DETAILS and tags None during time period STD2008:
        // Beginning = 500000  Ending = 500000 Operating = 0 Investing = 0 Financing = 0
        verifyCashflowStatement("With all tags set to none", STATEMENT_DETAILS_TEST_ORG, "STD2008", new BigDecimal("500000"), new BigDecimal("500000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, UtilMisc.toMap("tag1", "NONE", "tag2", "NONE", "tag3", "NONE"));
    }

    /**
     * This method runs the financial statements over and over again and check them for all the time periods.
     * @param messagePrefix a prefix for the assert messages
     * @param organizationPartyId the organization to test for
     * @throws Exception if an error occurs
     */
    private void verifyFinancialStatements(String messagePrefix, String organizationPartyId) throws Exception {
        assertAllTrialBalancesEqual(organizationPartyId, DECIMALS, ROUNDING);

        // verify income statements
        verifyIncomeStatement(messagePrefix, organizationPartyId, QUARTER1, new BigDecimal("142018.22"), new BigDecimal("37568.22"), new BigDecimal("28818.22"), new BigDecimal("14818.22"));
        verifyIncomeStatement(messagePrefix, organizationPartyId, QUARTER2, new BigDecimal("92356.00"), new BigDecimal("84356.00"), new BigDecimal("84356.00"), new BigDecimal("84356.00"));
        verifyIncomeStatement(messagePrefix, organizationPartyId, YEAR1, new BigDecimal("234374.22"), new BigDecimal("121924.22"), new BigDecimal("113174.22"), new BigDecimal("99174.22"));

        // verify income statement results is the same for YEAR1 using accountingTags ANY/ANY/ANY
        verifyIncomeStatement(messagePrefix, organizationPartyId, YEAR1, new BigDecimal("234374.22"), new BigDecimal("121924.22"), new BigDecimal("113174.22"), new BigDecimal("99174.22"), UtilMisc.toMap("tag1", "ANY", "tag2", "ANY", "tag3", "ANY"));

        // verify balance sheet statements
        verifyBalanceSheet(messagePrefix, organizationPartyId, YEAR0, new BigDecimal("610000"), new BigDecimal("300000"), new BigDecimal("310000"));
        verifyBalanceSheet(messagePrefix, organizationPartyId, QUARTER1, new BigDecimal("867343.22"), new BigDecimal("543525.00"), new BigDecimal("323818.22"));
        verifyBalanceSheet(messagePrefix, organizationPartyId, QUARTER2, new BigDecimal("1025343.22"), new BigDecimal("617169"), new BigDecimal("408174.22"));
        verifyBalanceSheet(messagePrefix, organizationPartyId, YEAR1, new BigDecimal("1025343.22"), new BigDecimal("617169"), new BigDecimal("408174.22"));

        // verify balance sheet results is the same for YEAR1 using accountingTags ANY/ANY/ANY
        verifyBalanceSheet(messagePrefix, organizationPartyId, YEAR1, new BigDecimal("1025343.22"), new BigDecimal("617169"), new BigDecimal("408174.22"), UtilMisc.toMap("tag1", "ANY", "tag2", "ANY", "tag3", "ANY"));
        
        // verify cash flow statements
        verifyCashflowStatement(messagePrefix, organizationPartyId, QUARTER1, new BigDecimal("200000.00"), new BigDecimal("74243.22"), new BigDecimal("-104756.78"), new BigDecimal("-50000.00"), new BigDecimal("29000"), new BigDecimal("-125756.78"));
        verifyCashflowStatement(messagePrefix, organizationPartyId, QUARTER2, new BigDecimal("74243.22"), new BigDecimal("136743.22"), new BigDecimal("62500"), new BigDecimal("0.00"), BigDecimal.ZERO, new BigDecimal("62500"));
        verifyCashflowStatement(messagePrefix, organizationPartyId, YEAR1, new BigDecimal("200000"), new BigDecimal("136743.22"), new BigDecimal("-42256.78"), new BigDecimal("-50000"), new BigDecimal("29000"), new BigDecimal("-63256.78"));

        // verify cashflow statement results is the same for YEAR1 using accountingTags ANY/ANY/ANY
        verifyCashflowStatement(messagePrefix, organizationPartyId, YEAR1, new BigDecimal("200000"), new BigDecimal("136743.22"), new BigDecimal("-42256.78"), new BigDecimal("-50000"), new BigDecimal("29000"), new BigDecimal("-63256.78"), UtilMisc.toMap("tag1", "ANY", "tag2", "ANY", "tag3", "ANY"));
    }

    /**
     * Verify income statement by both date range and time period.
     * @param messagePrefix a prefix for the assert messages
     * @param organizationPartyId the organization to test for
     * @param timePeriodId the time period to get the statement for
     * @param grossProfit the expected gross profit, optional
     * @param operatingIncome the expected operating income, optional
     * @param pretaxIncome the expected pretax income, optional
     * @param netIncome the expected net income, optional
     * @exception Exception if an error occurs
     */
    private void verifyIncomeStatement(String messagePrefix, String organizationPartyId, String timePeriodId, BigDecimal grossProfit, BigDecimal operatingIncome, BigDecimal pretaxIncome, BigDecimal netIncome) throws Exception {
        verifyIncomeStatement(messagePrefix, organizationPartyId, timePeriodId, grossProfit, operatingIncome, pretaxIncome, netIncome, null);
    }

    /**
     * Verify income statement by both date range and time period.
     * @param messagePrefix a prefix for the assert messages
     * @param organizationPartyId the organization to test for
     * @param timePeriodId the time period to get the statement for
     * @param grossProfit the expected gross profit, optional
     * @param operatingIncome the expected operating income, optional
     * @param pretaxIncome the expected pretax income, optional
     * @param netIncome the expected net income, optional
     * @param tags optional accounting tag to use when getting the statement, as a Map of tag1 -> value1, tag2 -> value2, ...
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    private void verifyIncomeStatement(String messagePrefix, String organizationPartyId, String timePeriodId, BigDecimal grossProfit, BigDecimal operatingIncome, BigDecimal pretaxIncome, BigDecimal netIncome, Map tags) throws Exception {
        GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", timePeriodId));

        // test by time period
        Map input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "fromTimePeriodId", timePeriodId, "thruTimePeriodId", timePeriodId, "glFiscalTypeId", FISCAL_TYPE, "userLogin", demofinadmin);
        if (tags != null) {
            input.putAll(tags);
        }

        Map tmpResult = runAndAssertServiceSuccess("getIncomeStatementByTimePeriods", input, -1, false);
        verifyIncomeStatementResults(messagePrefix + " calculated from time period for ", timePeriodId, tmpResult, grossProfit, operatingIncome, pretaxIncome, netIncome);

        // test by date
        input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "glFiscalTypeId", FISCAL_TYPE, "userLogin", demofinadmin);
        if (tags != null) {
            input.putAll(tags);
        }
        input.put("fromDate", UtilDateTime.toTimestamp(timePeriod.getDate("fromDate")));
        input.put("thruDate", UtilDateTime.toTimestamp(timePeriod.getDate("thruDate")));
        tmpResult = runAndAssertServiceSuccess("getIncomeStatementByDates", input, -1, false);
        verifyIncomeStatementResults(messagePrefix + " calculated from dates for ", timePeriodId, tmpResult, grossProfit, operatingIncome, pretaxIncome, netIncome);
    }

    /**
     * Verify cash flow statements by both date range and time period.
     * @param messagePrefix a prefix for the assert messages
     * @param organizationPartyId the organization to test for
     * @param timePeriodId the time period to get the statement for
     * @param beginningCashAmount the expected beginning cash amount
     * @param endingCashAmount the expected ending cash amount
     * @param operatingCashFlow the expected operating cash flow
     * @param investingCashFlow the expected investing cash flow
     * @param financingCashFlow the expected financing cash flow
     * @param netCashFlow the expected net cash flow
     * @exception Exception if an error occurs
     */
    private void verifyCashflowStatement(String messagePrefix, String organizationPartyId, String timePeriodId, BigDecimal beginningCashAmount, BigDecimal endingCashAmount, BigDecimal operatingCashFlow, BigDecimal investingCashFlow, BigDecimal financingCashFlow, BigDecimal netCashFlow) throws Exception {
        verifyCashflowStatement(messagePrefix, organizationPartyId, timePeriodId, beginningCashAmount, endingCashAmount, operatingCashFlow, investingCashFlow, financingCashFlow, netCashFlow, null);
    }

    /**
     * Verify cash flow statements by both date range and time period.
     * @param messagePrefix a prefix for the assert messages
     * @param organizationPartyId the organization to test for
     * @param timePeriodId the time period to get the statement for
     * @param beginningCashAmount the expected beginning cash amount
     * @param endingCashAmount the expected ending cash amount
     * @param operatingCashFlow the expected operating cash flow
     * @param investingCashFlow the expected investing cash flow
     * @param financingCashFlow the expected financing cash flow
     * @param netCashFlow the expected net cash flow
     * @param tags optional accounting tag to use when getting the statement, as a Map of tag1 -> value1, tag2 -> value2, ...
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    private void verifyCashflowStatement(String messagePrefix, String organizationPartyId, String timePeriodId, BigDecimal beginningCashAmount, BigDecimal endingCashAmount, BigDecimal operatingCashFlow, BigDecimal investingCashFlow, BigDecimal financingCashFlow, BigDecimal netCashFlow, Map tags) throws Exception {
        GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", timePeriodId));

        // test by time period
        Map input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "fromTimePeriodId", timePeriodId, "thruTimePeriodId", timePeriodId, "glFiscalTypeId", FISCAL_TYPE, "userLogin", demofinadmin);
        if (tags != null) {
            input.putAll(tags);
        }
        Map tmpResult = runAndAssertServiceSuccess("getCashFlowStatementForTimePeriods", input, -1, false);
        verifyCashflowStatementResults(messagePrefix + " calculated from time period for ", timePeriodId, tmpResult, beginningCashAmount, endingCashAmount, operatingCashFlow, investingCashFlow, financingCashFlow, netCashFlow);

        // test by date
        input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "fromDate", UtilDateTime.toTimestamp(timePeriod.getDate("fromDate")), "thruDate", UtilDateTime.toTimestamp(timePeriod.getDate("thruDate")), "glFiscalTypeId", FISCAL_TYPE, "userLogin", demofinadmin);
        if (tags != null) {
            input.putAll(tags);
        }
        tmpResult = runAndAssertServiceSuccess("getCashFlowStatementForDates", input, -1, false);
        verifyCashflowStatementResults(messagePrefix + " calculated from dates for ", timePeriodId, tmpResult, beginningCashAmount, endingCashAmount, operatingCashFlow, investingCashFlow, financingCashFlow, netCashFlow);
    }

    /**
     * Verify balance sheet by both date range and time period.
     * @param messagePrefix a prefix for the assert messages
     * @param organizationPartyId the organization to test for
     * @param timePeriodId the time period to get the balance sheet for
     * @param assetTotal the expected asset total
     * @param liabilityTotal the expected liability total
     * @param equityTotal the expected equity total
     * @exception Exception if an error occurs
     */
    private void verifyBalanceSheet(String messagePrefix, String organizationPartyId, String timePeriodId, BigDecimal assetTotal, BigDecimal liabilityTotal, BigDecimal equityTotal) throws Exception {
        verifyBalanceSheet(messagePrefix, organizationPartyId, timePeriodId, assetTotal, liabilityTotal, equityTotal, null);
    }

    /**
     * Verify balance sheet by both date range and time period.
     * @param messagePrefix a prefix for the assert messages
     * @param organizationPartyId the organization to test for
     * @param timePeriodId the time period to get the balance sheet for
     * @param assetTotal the expected asset total
     * @param liabilityTotal the expected liability total
     * @param equityTotal the expected equity total
     * @param tags optional accounting tag to use when getting the statement, as a Map of tag1 -> value1, tag2 -> value2, ...
     * @exception Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    private void verifyBalanceSheet(String messagePrefix, String organizationPartyId, String timePeriodId, BigDecimal assetTotal, BigDecimal liabilityTotal, BigDecimal equityTotal, Map tags) throws Exception {
        GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", timePeriodId));

        // test by time period
        Map input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "customTimePeriodId", timePeriodId, "glFiscalTypeId", FISCAL_TYPE, "userLogin", demofinadmin);
        if (tags != null) {
            input.putAll(tags);
        }

        Map tmpResult = runAndAssertServiceSuccess("getBalanceSheetForTimePeriod", input, -1, false);
        verifyBalanceSheetResults(messagePrefix + " calculated from time period for ", timePeriodId, tmpResult, assetTotal, liabilityTotal, equityTotal);

        // test by date
        input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "asOfDate", UtilDateTime.toTimestamp(timePeriod.getDate("thruDate")), "glFiscalTypeId", FISCAL_TYPE, "userLogin", demofinadmin);
        if (tags != null) {
            input.putAll(tags);
        }
        tmpResult = runAndAssertServiceSuccess("getBalanceSheetForDate", input, -1, false);
        verifyBalanceSheetResults(messagePrefix + " calculated from dates for " , timePeriodId, tmpResult, assetTotal, liabilityTotal, equityTotal);

        // test by date, using the day of closing
        input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "asOfDate", UtilDateTime.getTimestamp(timePeriod.getDate("thruDate").getTime() - 1000), "glFiscalTypeId", FISCAL_TYPE, "userLogin", demofinadmin);
        if (tags != null) {
            input.putAll(tags);
        }
        tmpResult = runAndAssertServiceSuccess("getBalanceSheetForDate", input, -1, false);
        verifyBalanceSheetResults(messagePrefix + " calculated from dates for " , timePeriodId, tmpResult, assetTotal, liabilityTotal, equityTotal);

    }

    /**
     * Convenience method for checking the results of income statements in the service map.
     * Used by <code>verifyIncomeStatement</code>.
     * @param messagePrefix a prefix for the assert messages
     * @param timePeriodId the time period to get the statement for
     * @param results the results <code>Map</code> from the service call
     * @param grossProfit the expected gross profit, optional
     * @param operatingIncome the expected operating income, optional
     * @param pretaxIncome the expected pretax income, optional
     * @param netIncome the expected net income, optional
     */
    @SuppressWarnings("unchecked")
    private void verifyIncomeStatementResults(String messagePrefix, String timePeriodId, Map results, BigDecimal grossProfit, BigDecimal operatingIncome, BigDecimal pretaxIncome, BigDecimal netIncome) {
        if (grossProfit != null) {
            verifyMapValue(messagePrefix, timePeriodId, results, "grossProfit", grossProfit);
        }
        if (operatingIncome != null) {
            verifyMapValue(messagePrefix, timePeriodId, results, "operatingIncome", operatingIncome);
        }
        if (pretaxIncome != null) {
            verifyMapValue(messagePrefix, timePeriodId, results, "pretaxIncome", pretaxIncome);
        }
        if (netIncome != null) {
            verifyMapValue(messagePrefix, timePeriodId, results, "netIncome", netIncome);
        }
    }

    /**
     * Convenience method for checking cash flow statement results in service map.
     * Used by <code>verifyCashflowStatement</code>.
     * @param messagePrefix a prefix for the assert messages
     * @param timePeriodId the time period to get the statement for
     * @param results the results <code>Map</code> from the service call
     * @param beginningCashAmount the expected beginning cash amount
     * @param endingCashAmount the expected ending cash amount
     * @param operatingCashFlow the expected operating cash flow
     * @param investingCashFlow the expected investing cash flow
     * @param financingCashFlow the expected financing cash flow
     * @param netCashFlow the expected net cash flow
     */
    @SuppressWarnings("unchecked")
    private void verifyCashflowStatementResults(String messagePrefix, String timePeriodId, Map results, BigDecimal beginningCashAmount, BigDecimal endingCashAmount, BigDecimal operatingCashFlow, BigDecimal investingCashFlow, BigDecimal financingCashFlow, BigDecimal netCashFlow) {
        verifyMapValue(messagePrefix, timePeriodId, results, "beginningCashAmount", beginningCashAmount);
        verifyMapValue(messagePrefix, timePeriodId, results, "endingCashAmount", endingCashAmount);
        verifyMapValue(messagePrefix, timePeriodId, results, "operatingCashFlow", operatingCashFlow);
        verifyMapValue(messagePrefix, timePeriodId, results, "investingCashFlow", investingCashFlow);
        verifyMapValue(messagePrefix, timePeriodId, results, "financingCashFlow", financingCashFlow);
        verifyMapValue(messagePrefix, timePeriodId, results, "netCashFlow", netCashFlow);
   }


    /**
     * Convenience methods to verify cash flow statement results in service map.
     * Used by <code>verifyBalanceSheet</code>.
     * @param messagePrefix a prefix for the assert messages
     * @param timePeriodId the time period to get the balance sheet for
     * @param results the results <code>Map</code> from the service call
     * @param assetTotal the expected asset total
     * @param liabilityTotal the expected liability total
     * @param equityTotal the expected equity total
     */
    @SuppressWarnings("unchecked")
    private void verifyBalanceSheetResults(String messagePrefix, String timePeriodId, Map results,
                                           BigDecimal assetTotal, BigDecimal liabilityTotal, BigDecimal equityTotal) {
        assertEquals(messagePrefix + " for time period [" + timePeriodId + "] : asset total is not correct", sumMapValues((Map) results.get("assetAccountBalances")), assetTotal, DECIMALS, ROUNDING);
        assertEquals(messagePrefix + " for time period [" + timePeriodId + "] : liability total is not correct", sumMapValues((Map) results.get("liabilityAccountBalances")), liabilityTotal, DECIMALS, ROUNDING);
        assertEquals(messagePrefix + " for time period [" + timePeriodId + "] : equity total is not correct", sumMapValues((Map) results.get("equityAccountBalances")), equityTotal, DECIMALS, ROUNDING);

    }

    /**
     * Check the mapKey actually is not null, then check its rounded value versus reference value.
     * @param messagePrefix a prefix for the assert messages
     * @param timePeriodId the time period used
     * @param results the results <code>Map</code> from the service call
     * @param mapKey the map key
     * @param referenceValue the expected value
     */
    @SuppressWarnings("unchecked")
    private void verifyMapValue(String messagePrefix, String timePeriodId, Map results, String mapKey, BigDecimal referenceValue) {
        assertNotNull(messagePrefix + " for time period [" + timePeriodId + "] : " + mapKey + " is null", results.get(mapKey));
        assertEquals(messagePrefix + " for time period [" + timePeriodId + "] : " + mapKey + " is not correct", (BigDecimal) results.get(mapKey), referenceValue, DECIMALS, ROUNDING);

    }

    /**
     * Convenience method to sum up the values of a Map of key -> BigDecimal value.
     * @param results the results <code>Map</code>
     * @return the sum of all the map values
     */
    @SuppressWarnings("unchecked")
    private BigDecimal sumMapValues(Map results) {
        BigDecimal sum = BigDecimal.ZERO;
        Collection values = results.values();
        for (Iterator it = values.iterator(); it.hasNext();) {
            Object o = it.next();
            BigDecimal value = null;
            if (o instanceof BigDecimal) {
                value = (BigDecimal) o;
            } else if (o instanceof Double) {
                value = BigDecimal.valueOf((Double) o);
            }

            sum = sum.add(value);
        }
        return sum;
    }

}
