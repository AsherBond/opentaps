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

package com.opensourcestrategies.financials.financials;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.accounting.AccountingException;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.constants.GlAccountTypeConstants;
import org.opentaps.base.services.FindLastClosedDateService;
import org.opentaps.base.services.GetBalanceSheetForDateService;
import org.opentaps.base.services.GetIncomeStatementByDatesService;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.ledger.GeneralLedgerAccount;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationDomainInterface;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.financials.domain.ledger.LedgerRepository;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * FinancialServices - Services for generating financial reports and statements.
 *
 * @author     <a href="mailto:sichen@opensourcestrategies.com">Si Chen</a>
 * @version    $Rev$
 * @since      2.2
 */
public final class FinancialServices {

    private FinancialServices() { }

    private static String MODULE = FinancialServices.class.getName();

    public static int decimals = UtilNumber.getBigDecimalScale("fin_arithmetic.properties", "financial.statements.decimals");
    public static int rounding = UtilNumber.getBigDecimalRoundingMode("fin_arithmetic.properties", "financial.statements.rounding");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(decimals, rounding);

    /** Default toplevel income statement glAccountTypeIds to group by. */
    public static final List<String> INCOME_STATEMENT_TYPES = Arrays.asList("REVENUE", "COGS", "OPERATING_EXPENSE", "OTHER_EXPENSE", "OTHER_INCOME", "TAX_EXPENSE");
    /** The types that are expenses. */
    public static final List<String> EXPENSES_TYPES = Arrays.asList("COGS", "OPERATING_EXPENSE", "OTHER_EXPENSE", "TAX_EXPENSE");
 

    /** Account classes to search when generating the income statement. */
    public static final List<String> INCOME_STATEMENT_CLASSES = Arrays.asList("REVENUE", "EXPENSE", "INCOME");

    /** A type to group all accounts without glAccountTypeId for income statement. */
    public static final String UNCLASSIFIED_TYPE = "UNCLASSIFIED";

    /**
     * Generates an income statement over two CustomTimePeriod entries, returning a Map of GlAccount and amounts and a double.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getIncomeStatementByTimePeriods(DispatchContext dctx, Map context) {
        return reportServiceTimePeriodHelper(dctx, context, "getIncomeStatementByDates");
    }

    /**
     * Generates an income statement over a range of dates, returning a Map of GlAccount and amounts and a netIncome.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getIncomeStatementByDates(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");
        String organizationPartyId = (String) context.get("organizationPartyId");
        String glFiscalTypeId = (String) context.get("glFiscalTypeId");
        // glFiscalTypeId defaults to ACTUAL
        if (UtilValidate.isEmpty(glFiscalTypeId)) {
            glFiscalTypeId = "ACTUAL";
            context.put("glFiscalTypeId", glFiscalTypeId);
        }

        try {
            // get a Map of glAccount -> sums for all income statement accounts for this time period
            Map input = dctx.getModelService("getIncomeStatementAccountSumsByDate").makeValid(context, ModelService.IN_PARAM);
            Map tmpResult = dispatcher.runSync("getIncomeStatementAccountSumsByDate", input);
            if (tmpResult.get("glAccountSums") == null) {
                return ServiceUtil.returnError("Cannot sum up account balances properly for income statement");
            }
            Map<GenericValue, BigDecimal> glAccountSums = (HashMap) tmpResult.get("glAccountSums");

            Map<String, String> glAccountTypeTree = FastMap.newInstance();
            Map<String, List<Map>> glAccountSumsGrouped = FastMap.newInstance();
            Map<String, BigDecimal> glAccountGroupSums = FastMap.newInstance();
            Map<String, BigDecimal> sums = FastMap.newInstance();
            prepareIncomeStatementMaps(glAccountTypeTree, glAccountSumsGrouped, glAccountGroupSums, sums, delegator);

            // sort them into the correct map while also keeping a running total of the aggregations we're interested in (net income, gross profit, etc.)
            for (GenericValue account : glAccountSums.keySet()) {
                calculateIncomeStatementMaps(glAccountTypeTree, glAccountSumsGrouped, glAccountGroupSums, sums, account, glAccountSums.get(account), delegator);
            }

            return makeIncomeStatementResults(glAccountSums, glAccountSumsGrouped, glAccountGroupSums, sums, organizationPartyId, fromDate, thruDate, delegator);

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Calculates net income (of ACTUAL gl fiscal type) since last closed accounting period or, if none exists, since earliest accounting period.
     *  Optionally use periodTypeId to get figure since last closed date of a period type.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getActualNetIncomeSinceLastClosing(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String organizationPartyId = (String) context.get("organizationPartyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String periodTypeId = (String) context.get("periodTypeId");

        Timestamp fromDate = null;

        try {
            // try to get the ending date of the most recent accounting time period which has been closed
            Map tmpResult = dispatcher.runSync("findLastClosedDate", UtilMisc.toMap("organizationPartyId", organizationPartyId,
                    "periodTypeId", periodTypeId, "userLogin", userLogin));
            if ((tmpResult != null) && (tmpResult.get("lastClosedDate") != null)) {
                fromDate = (Timestamp) tmpResult.get("lastClosedDate");
            } else {
                return ServiceUtil.returnError("Cannot get a starting date for net income");
            }

            // add calculated parameters
            Map input = new HashMap(context);
            input.putAll(UtilMisc.toMap("glFiscalTypeId", "ACTUAL", "fromDate", fromDate));
            input = dctx.getModelService("getIncomeStatementByDates").makeValid(input, ModelService.IN_PARAM);
            tmpResult = dispatcher.runSync("getIncomeStatementByDates", input);

            if (!UtilCommon.isSuccess(tmpResult)) {
                return tmpResult;  // probably an error message - pass it back up
            } else if (tmpResult.get("netIncome") == null) {
                return ServiceUtil.returnError("Cannot calculate a net income"); // no error message, no net income either?
            } else {
                // return net income and profit&loss gl account
                Map result = ServiceUtil.returnSuccess();
                result.put("netIncome", tmpResult.get("netIncome"));
                result.put("retainedEarningsGlAccount", tmpResult.get("retainedEarningsGlAccount"));
                result.put("glAccountSumsFlat", tmpResult.get("glAccountSumsFlat"));
                return result;
            }
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Generates balance sheet for a time period and returns separate maps for balances of asset, liability, and equity accounts.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getBalanceSheetForTimePeriod(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String organizationPartyId = (String) context.get("organizationPartyId");
        String customTimePeriodId = (String) context.get("customTimePeriodId");
        String glFiscalTypeId = (String) context.get("glFiscalTypeId");
        // default to generating "ACTUAL" balance sheets
        if (UtilValidate.isEmpty(glFiscalTypeId)) {
            glFiscalTypeId = "ACTUAL";
            context.put("glFiscalTypeId", glFiscalTypeId);
        }

        try {
            // get the current time period and first use it to assume whether this period has been closed or not
            GenericValue currentTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
            boolean isClosed = false;
            boolean accountingTagsUsed = UtilAccountingTags.areTagsSet(context);
            if (currentTimePeriod.getString("isClosed").equals("Y")) {
                isClosed = true;
                if (accountingTagsUsed) {
                    Debug.logWarning("getBalanceSheetForTimePeriod found a closed time period but we have accounting tag, considering the time period as NOT closed", MODULE);
                    isClosed = false;
                }
            }

            if (("ACTUAL".equals(glFiscalTypeId)) && (isClosed)) {
                // if the time period is closed and we're doing ACTUAL balance sheet, then we can use the posted GlAccountHistory
                // first, find all the gl accounts' GlAccountHistory record for this time period
                EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                        EntityCondition.makeCondition("customTimePeriodId", EntityOperator.EQUALS, customTimePeriodId),
                        EntityCondition.makeCondition(EntityOperator.OR,
                                UtilFinancial.getAssetExpr(delegator),
                                UtilFinancial.getLiabilityExpr(delegator),
                                UtilFinancial.getEquityExpr(delegator)));
                List selectedFields = UtilMisc.toList("glAccountId", "glAccountTypeId", "glAccountClassId", "accountName", "postedDebits", "postedCredits");
                selectedFields.add("endingBalance");
                List<GenericValue> accounts = delegator.findByCondition("GlAccountAndHistory", conditions, selectedFields, UtilMisc.toList("glAccountId"));

                // now, create the separate account balance Maps and see if this period has been closed.
                // if the period has been closed, then just get the accounts' balances from the endingBalance
                // otherwise, get it by calculating the net of posted debits and credits
                Map<GenericValue, BigDecimal> assetAccountBalances = new HashMap<GenericValue, BigDecimal>();
                Map<GenericValue, BigDecimal> liabilityAccountBalances = new HashMap<GenericValue, BigDecimal>();
                Map<GenericValue, BigDecimal> equityAccountBalances = new HashMap<GenericValue, BigDecimal>();

                for (Iterator<GenericValue> ai = accounts.iterator(); ai.hasNext();) {
                    GenericValue account = ai.next();
                    BigDecimal balance = BigDecimal.ZERO;
                    if (account.get("endingBalance") != null) {
                        balance = account.getBigDecimal("endingBalance");
                    }
                    // classify and put into the appropriate Map
                    if (UtilAccounting.isAssetAccount(account)) {
                        assetAccountBalances.put(account.getRelatedOne("GlAccount"), balance);
                    } else if (UtilAccounting.isLiabilityAccount(account)) {
                        liabilityAccountBalances.put(account.getRelatedOne("GlAccount"), balance);
                    } else if (UtilAccounting.isEquityAccount(account)) {
                        equityAccountBalances.put(account.getRelatedOne("GlAccount"), balance);
                    }
                }
                Map result = ServiceUtil.returnSuccess();
                result.put("assetAccountBalances", assetAccountBalances);
                result.put("liabilityAccountBalances", liabilityAccountBalances);
                result.put("equityAccountBalances", equityAccountBalances);
                result.put("isClosed", new Boolean(isClosed));
                return result;
            } else {
                Map params = dctx.getModelService("getBalanceSheetForDate").makeValid(context, ModelService.IN_PARAM);
                params.put("asOfDate", UtilDateTime.toTimestamp(currentTimePeriod.getDate("thruDate")));
                return dispatcher.runSync("getBalanceSheetForDate", params);
            }


        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }


    /**
     * Generates the trial balance for a time period and returns separate maps for balances of asset, liability, equity, revenue, expense, income and other accounts.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getTrialBalanceForDate(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String organizationPartyId = (String) context.get("organizationPartyId");
        Timestamp asOfDate = (Timestamp) context.get("asOfDate");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String glFiscalTypeId = (String) context.get("glFiscalTypeId");
        Map tags = UtilAccountingTags.getTagParameters(context);

        // default to generating "ACTUAL" balance sheets
        if (UtilValidate.isEmpty(glFiscalTypeId)) {
            glFiscalTypeId = "ACTUAL";
            context.put("glFiscalTypeId", glFiscalTypeId);
        }
        User user = new User(userLogin);
        Infrastructure infrastructure = new Infrastructure(dispatcher);
        DomainsLoader dl = new DomainsLoader(infrastructure, user);
        try {
            LedgerRepositoryInterface ledgerRepository = dl.loadDomainsDirectory().getLedgerDomain().getLedgerRepository();

            // for totals of balances, debits and credits
            Map<GenericValue, BigDecimal> accountBalances = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> accountDebits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> accountCredits = new HashMap<GenericValue, BigDecimal>();

            // per account type
            Map<GenericValue, BigDecimal> assetAccountBalances = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> liabilityAccountBalances = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> equityAccountBalances = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> revenueAccountBalances = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> expenseAccountBalances = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> incomeAccountBalances = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> otherAccountBalances = new HashMap<GenericValue, BigDecimal>();

            Map<GenericValue, BigDecimal> assetAccountDebits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> liabilityAccountDebits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> equityAccountDebits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> revenueAccountDebits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> expenseAccountDebits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> incomeAccountDebits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> otherAccountDebits = new HashMap<GenericValue, BigDecimal>();

            Map<GenericValue, BigDecimal> assetAccountCredits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> liabilityAccountCredits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> equityAccountCredits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> revenueAccountCredits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> expenseAccountCredits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> incomeAccountCredits = new HashMap<GenericValue, BigDecimal>();
            Map<GenericValue, BigDecimal> otherAccountCredits = new HashMap<GenericValue, BigDecimal>();

            // try to get the ending date of the most recent accounting time period which has been closed
            Timestamp lastClosedDate = UtilDateTime.toTimestamp(1, 1, 1970, 0, 0, 0);   // default if there never has been a period closing
            FindLastClosedDateService findLastClosedDate = new FindLastClosedDateService();
            findLastClosedDate.setInOrganizationPartyId(organizationPartyId);
            // this is in case the current time period is already closed, and you try to run a report at the end of the time period
            // adjust as of date forward 1 second, so if report date is 12/31/XX at 23:59:59.999, but CustomTimePeriod ends at 1/1/XX+1 00:00:00 and the CustomTimePeriod has been
            // closed, it will get the CustomTimePeriod as the last closed period.  Otherwise, it will get the previous time period and add everything again.
            // but don't change the actual as of date because it may cause inconsistencies with other reports 
            // does not appear to be an issue for balance sheet, income statement, because if it is missed you just re-calculate that time period again
            // but this report actually adds both, so if you miss a closed time period, it could cause double-counting
            findLastClosedDate.setInFindDate(new java.sql.Date(UtilDateTime.adjustTimestamp(asOfDate, java.util.Calendar.SECOND, new Integer(1)).getTime()));

            findLastClosedDate.setUser(user);
            findLastClosedDate.runSyncNoNewTransaction(infrastructure);
            
            // these are null unless there has been a closed time period before this
            BigDecimal netIncomeSinceLastClosing = null;
            GeneralLedgerAccount retainedEarningsGlAccount = ledgerRepository.getDefaultLedgerAccount(GlAccountTypeConstants.RETAINED_EARNINGS, organizationPartyId);;
            GeneralLedgerAccount profitLossGlAccount = ledgerRepository.getDefaultLedgerAccount(GlAccountTypeConstants.PROFIT_LOSS_ACCOUNT, organizationPartyId);
            
            // balance sheet account balances as of the previous closed time period, null if there has been no previous closed time periods
            Map balanceSheetAccountBalances = null;  
            // if there has been a previous period closing, then we need both the income statement and balance sheet since the last closing
            if (findLastClosedDate.getOutLastClosedDate() != null) {
                lastClosedDate = findLastClosedDate.getOutLastClosedDate();

                GetIncomeStatementByDatesService getIncomeStatement = new GetIncomeStatementByDatesService();
                getIncomeStatement.setInFromDate(lastClosedDate);
                getIncomeStatement.setInThruDate(asOfDate);
                getIncomeStatement.setInOrganizationPartyId(organizationPartyId);
                getIncomeStatement.setInGlFiscalTypeId(glFiscalTypeId);
                getIncomeStatement.setInTag1((String) tags.get("tag1")); 
                getIncomeStatement.setInTag2((String) tags.get("tag2")); 
                getIncomeStatement.setInTag3((String) tags.get("tag3")); 
                getIncomeStatement.setInTag4((String) tags.get("tag4")); 
                getIncomeStatement.setInTag5((String) tags.get("tag5")); 
                getIncomeStatement.setInTag6((String) tags.get("tag6")); 
                getIncomeStatement.setInTag7((String) tags.get("tag7")); 
                getIncomeStatement.setInTag8((String) tags.get("tag8")); 
                getIncomeStatement.setInTag9((String) tags.get("tag9")); 
                getIncomeStatement.setInTag10((String) tags.get("tag10")); 
                getIncomeStatement.setUser(user);
                getIncomeStatement.runSyncNoNewTransaction(infrastructure);
                
                netIncomeSinceLastClosing = getIncomeStatement.getOutNetIncome();
                
                GetBalanceSheetForDateService getBalanceSheet = new GetBalanceSheetForDateService();
                getBalanceSheet.setInAsOfDate(lastClosedDate);
                getBalanceSheet.setInGlFiscalTypeId(glFiscalTypeId);
                getBalanceSheet.setInOrganizationPartyId(organizationPartyId);
                getBalanceSheet.setInTag1((String) tags.get("tag1")); 
                getBalanceSheet.setInTag2((String) tags.get("tag2")); 
                getBalanceSheet.setInTag3((String) tags.get("tag3")); 
                getBalanceSheet.setInTag4((String) tags.get("tag4")); 
                getBalanceSheet.setInTag5((String) tags.get("tag5")); 
                getBalanceSheet.setInTag6((String) tags.get("tag6")); 
                getBalanceSheet.setInTag7((String) tags.get("tag7")); 
                getBalanceSheet.setInTag8((String) tags.get("tag8")); 
                getBalanceSheet.setInTag9((String) tags.get("tag9")); 
                getBalanceSheet.setInTag10((String) tags.get("tag10")); 
                getBalanceSheet.setUser(user);
                getBalanceSheet.runSyncNoNewTransaction(infrastructure);
                
                balanceSheetAccountBalances = getBalanceSheet.getOutAssetAccountBalances();
                balanceSheetAccountBalances.putAll(getBalanceSheet.getOutLiabilityAccountBalances());
                balanceSheetAccountBalances.putAll(getBalanceSheet.getOutEquityAccountBalances());
            }
            Debug.logInfo("Last closed date is [" + lastClosedDate + "] net income [" + netIncomeSinceLastClosing + "] profit loss account is [" + profitLossGlAccount.getGlAccountId() + "] and retained earnings account [" + retainedEarningsGlAccount.getGlAccountId() + "]", MODULE);
            
            //  a little bit of a hack, using this method with a "null" for GL account class  causes it to return the sum of the transaction entries for all the GL accounts
            accountBalances = getAcctgTransAndEntriesForClassWithDetails(accountBalances, accountDebits, accountCredits, organizationPartyId, lastClosedDate, asOfDate, glFiscalTypeId, null, tags, userLogin, dispatcher);
            
            // if there are balance sheet balances from the last closed time period, then add them to the account balances we just got, thus merging the last closed balance sheet with the trial balance since then
            if (balanceSheetAccountBalances != null) {
                Set<GenericValue> balanceSheetGlAccounts = balanceSheetAccountBalances.keySet();
                for (GenericValue glAccount: balanceSheetGlAccounts) {
                    UtilCommon.addInMapOfBigDecimal(accountBalances, glAccount, (BigDecimal) balanceSheetAccountBalances.get(glAccount));
                }
            }

            //  now we sort them out into separate sections for assets, liabilities, equities, revenue, expense, income
            // this is also where we add the net income since last time period closing to the retained earnings and profit loss accounts
            Set<GenericValue> glAccounts = accountBalances.keySet();
            BigDecimal netBalance = BigDecimal.ZERO;   // net of debit minus credit
            BigDecimal totalDebits = BigDecimal.ZERO;
            BigDecimal totalCredits = BigDecimal.ZERO;
      
            // we need to track if this has been done, and if not do it later manually
            boolean addedNetIncomeSinceLastClosingToRetainedEarnings = false;
            boolean addedNetIncomeSinceLastCLosingToProfitLoss = false;
            
            for (GenericValue glAccount : glAccounts) {
                BigDecimal balance = accountBalances.get(glAccount);              
                String glAccountId = glAccount.getString("glAccountId");
                
                // add net income since last closing to the retained earnings and profit loss accounts
                if (netIncomeSinceLastClosing != null) {
                    if (glAccountId.equals(retainedEarningsGlAccount.getGlAccountId())) {
                        balance = balance.add(netIncomeSinceLastClosing).setScale(decimals, rounding);
                        addedNetIncomeSinceLastClosingToRetainedEarnings = true;
                        Debug.logInfo("Adding retained earnings of [" + netIncomeSinceLastClosing + "] to GL account ["+ glAccountId + "] balance is now [" + balance + "]", MODULE);
                    }
                    if (glAccountId.equals(profitLossGlAccount.getGlAccountId())) {
                        balance = balance.add(netIncomeSinceLastClosing).setScale(decimals, rounding);
                        addedNetIncomeSinceLastCLosingToProfitLoss = true;
                        Debug.logInfo("Adding retained earnings of [" + netIncomeSinceLastClosing + "] to GL account ["+ glAccountId + "] balance is now [" + balance + "]", MODULE);
                    }
                }

                // add to net balance and total debits and total credits
                if (balance != null) {
                    if (UtilAccounting.isDebitAccount(glAccount)) {
                        totalDebits = totalDebits.add(balance);
                        netBalance = netBalance.add(balance);
                    } else {
                        totalCredits = totalCredits.add(balance);
                        netBalance = netBalance.subtract(balance);
                    }
                }


                if (UtilAccounting.isAssetAccount(glAccount)) {
                    assetAccountBalances.put(glAccount, balance);
                    assetAccountDebits.put(glAccount, balance);
                } else if (UtilAccounting.isLiabilityAccount(glAccount)) {
                    liabilityAccountBalances.put(glAccount, balance);
                    liabilityAccountCredits.put(glAccount, balance);
                } else if (UtilAccounting.isEquityAccount(glAccount)) {
                    equityAccountBalances.put(glAccount, balance);
                    equityAccountCredits.put(glAccount, balance);
                } else if (UtilAccounting.isRevenueAccount(glAccount)) {
                    revenueAccountBalances.put(glAccount, balance);
                    revenueAccountCredits.put(glAccount, balance);
                } else if (UtilAccounting.isExpenseAccount(glAccount)) {
                    expenseAccountBalances.put(glAccount, balance);
                    expenseAccountDebits.put(glAccount, balance);
                } else if (UtilAccounting.isIncomeAccount(glAccount)) {
                    incomeAccountBalances.put(glAccount, balance);
                    incomeAccountCredits.put(glAccount, balance);
                } else {
                    Debug.logWarning("Classification of GL account [" + glAccount.getString("glAccountId") + "] is unknown, putting balance [" + balance + "] under debit", MODULE);
                    otherAccountBalances.put(glAccount, balance);
                    otherAccountDebits.put(glAccount, balance);
                }
            }
            
            // now manually put in the retained earnings if that was not done already
            // this assume that since there was no retained earnings from before, it should be zero
            if ((lastClosedDate != null) && !addedNetIncomeSinceLastClosingToRetainedEarnings) { 
                equityAccountBalances.put(LedgerRepository.genericValueFromEntity(retainedEarningsGlAccount), netIncomeSinceLastClosing);
                equityAccountDebits.put(LedgerRepository.genericValueFromEntity(retainedEarningsGlAccount), netIncomeSinceLastClosing);
                equityAccountCredits.put(LedgerRepository.genericValueFromEntity(retainedEarningsGlAccount), netIncomeSinceLastClosing);
                totalCredits = totalCredits.add(netIncomeSinceLastClosing);
                netBalance = netBalance.subtract(netIncomeSinceLastClosing);
                Debug.logInfo("Did not find retained earnings account, so put [" + netIncomeSinceLastClosing + "] for [" + retainedEarningsGlAccount.getGlAccountId() + "]", MODULE);
            }
            // do the same for profit loss.  this should be OK -- an offsetting amount should've been added to retained earnings or put there right above
            if ((lastClosedDate != null) && !addedNetIncomeSinceLastCLosingToProfitLoss) { 
                incomeAccountBalances.put(LedgerRepository.genericValueFromEntity(profitLossGlAccount), netIncomeSinceLastClosing);
                incomeAccountDebits.put(LedgerRepository.genericValueFromEntity(profitLossGlAccount), netIncomeSinceLastClosing);
                incomeAccountCredits.put(LedgerRepository.genericValueFromEntity(profitLossGlAccount), netIncomeSinceLastClosing);
                totalDebits = totalDebits.add(netIncomeSinceLastClosing);
                netBalance = netBalance.add(netIncomeSinceLastClosing);
                Debug.logInfo("Did not find profit loss account, so put [" + netIncomeSinceLastClosing + "] for [" + profitLossGlAccount.getGlAccountId() + "]", MODULE);
            }
            
            // calculate net income from the last closed date
            
            // add it to the retained earnings account and profit and loss account

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("assetAccountBalances", assetAccountBalances);
            results.put("liabilityAccountBalances", liabilityAccountBalances);
            results.put("equityAccountBalances", equityAccountBalances);
            results.put("revenueAccountBalances", revenueAccountBalances);
            results.put("expenseAccountBalances", expenseAccountBalances);
            results.put("incomeAccountBalances", incomeAccountBalances);
            results.put("otherAccountBalances", otherAccountBalances);

            results.put("assetAccountCredits", assetAccountCredits);
            results.put("liabilityAccountCredits", liabilityAccountCredits);
            results.put("equityAccountCredits", equityAccountCredits);
            results.put("revenueAccountCredits", revenueAccountCredits);
            results.put("expenseAccountCredits", expenseAccountCredits);
            results.put("incomeAccountCredits", incomeAccountCredits);
            results.put("otherAccountCredits", otherAccountCredits);

            results.put("assetAccountDebits", assetAccountDebits);
            results.put("liabilityAccountDebits", liabilityAccountDebits);
            results.put("equityAccountDebits", equityAccountDebits);
            results.put("revenueAccountDebits", revenueAccountDebits);
            results.put("expenseAccountDebits", expenseAccountDebits);
            results.put("incomeAccountDebits", incomeAccountDebits);
            results.put("otherAccountDebits", otherAccountDebits);

            results.put("totalBalances", netBalance);
            results.put("totalDebits", totalDebits);
            results.put("totalCredits", totalCredits);

            Debug.logInfo("getTrialBalanceForDate totals => balance = " + netBalance + ", credits = " + totalCredits + ", debits = " + totalDebits + ", calculated balance = " + totalCredits.subtract(totalDebits), MODULE);

            return results;

        } catch (GeneralException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Generates balance sheet as of a particular date, by working forward from the last closed time period,
     *  or, if none, from the beginning of the earliest time period.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getBalanceSheetForDate(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String organizationPartyId = (String) context.get("organizationPartyId");
        Timestamp asOfDate = (Timestamp) context.get("asOfDate");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String glFiscalTypeId = (String) context.get("glFiscalTypeId");
        // default to generating "ACTUAL" balance sheets
        if (UtilValidate.isEmpty(glFiscalTypeId)) {
            glFiscalTypeId = "ACTUAL";
            context.put("glFiscalTypeId", glFiscalTypeId);
        }

        // balances of the asset, liability, and equity GL accounts, initially empty
        Map<GenericValue, BigDecimal> assetAccountBalances = new HashMap<GenericValue, BigDecimal>();
        Map<GenericValue, BigDecimal> liabilityAccountBalances = new HashMap<GenericValue, BigDecimal>();
        Map<GenericValue, BigDecimal> equityAccountBalances = new HashMap<GenericValue, BigDecimal>();
        
        try {

            // figure the date and the last closed time period
            Timestamp lastClosedDate = null;
            GenericValue lastClosedTimePeriod = null;
            Map tmpResult;
            boolean accountingTagsUsed = UtilAccountingTags.areTagsSet(context);
            if (accountingTagsUsed) {
                Debug.logWarning("getBalanceSheetForDate is using accounting tags, not looking for closed time periods", MODULE);
                lastClosedTimePeriod = null;
                List<GenericValue> timePeriods = delegator.findByAnd("CustomTimePeriod", UtilMisc.toMap("organizationPartyId", organizationPartyId), UtilMisc.toList("fromDate ASC"));
                if ((timePeriods != null) && (timePeriods.size() > 0) && ((timePeriods.get(0)).get("fromDate") != null)) {
                    lastClosedDate = UtilDateTime.toTimestamp((timePeriods.get(0)).getDate("fromDate"));
                } else {
                    return ServiceUtil.returnError("Cannot get a starting date for net income");
                }
            } else {
                // find the last closed time period
                tmpResult = dispatcher.runSync("findLastClosedDate", UtilMisc.toMap("organizationPartyId", organizationPartyId, "findDate", new Date(asOfDate.getTime()), "userLogin", userLogin));
                if ((tmpResult == null) || (tmpResult.get("lastClosedDate") == null)) {
                    return ServiceUtil.returnError("Cannot get a closed time period before " + asOfDate);
                } else {
                    lastClosedDate = (Timestamp) tmpResult.get("lastClosedDate");
                }
                if (tmpResult.get("lastClosedTimePeriod") != null) {
                    lastClosedTimePeriod = (GenericValue) tmpResult.get("lastClosedTimePeriod");
                }
            }

            Debug.logVerbose("Last closed time period [" + lastClosedTimePeriod + "] and last closed date [" + lastClosedDate + "]", MODULE);

            // if there was a previously closed time period, then get a balance sheet as of the end of that time period.  This balance sheet is our starting point
            // but this only works for ACTUAL fiscal types.  For Budgets, Forecasts, etc., we should just aggregating transaction entries.
            if ("ACTUAL".equals(glFiscalTypeId) && lastClosedTimePeriod != null) {
                Map input = new HashMap(context);
                input.put("customTimePeriodId", lastClosedTimePeriod.getString("customTimePeriodId"));
                input = dctx.getModelService("getBalanceSheetForTimePeriod").makeValid(input, ModelService.IN_PARAM);
                tmpResult = dispatcher.runSync("getBalanceSheetForTimePeriod", input);
                if (tmpResult != null) {
                    assetAccountBalances = (Map<GenericValue, BigDecimal>) tmpResult.get("assetAccountBalances");
                    liabilityAccountBalances = (Map<GenericValue, BigDecimal>) tmpResult.get("liabilityAccountBalances");
                    equityAccountBalances = (Map<GenericValue, BigDecimal>) tmpResult.get("equityAccountBalances");
                }
            }

            // now add the new asset, liability, and equity transactions
            Map tags = UtilAccountingTags.getTagParameters(context);
            assetAccountBalances = getAcctgTransAndEntriesForClass(assetAccountBalances, organizationPartyId, lastClosedDate, asOfDate, glFiscalTypeId, "ASSET", tags, true, userLogin, dispatcher);
            liabilityAccountBalances = getAcctgTransAndEntriesForClass(liabilityAccountBalances, organizationPartyId, lastClosedDate, asOfDate, glFiscalTypeId, "LIABILITY", tags, true, userLogin, dispatcher);
            equityAccountBalances = getAcctgTransAndEntriesForClass(equityAccountBalances, organizationPartyId, lastClosedDate, asOfDate, glFiscalTypeId, "EQUITY", tags, true, userLogin, dispatcher);

            // calculate a net income since the last closed date and add it to our equity account balances
            Map input = new HashMap(context);
            input.put("fromDate", lastClosedDate);
            input.put("thruDate", asOfDate);
            input = dctx.getModelService("getIncomeStatementByDates").makeValid(input, ModelService.IN_PARAM);
            tmpResult = dispatcher.runSync("getIncomeStatementByDates", input);
            GenericValue retainedEarningsGlAccount = (GenericValue) tmpResult.get("retainedEarningsGlAccount");
            BigDecimal interimNetIncome = (BigDecimal) tmpResult.get("netIncome");

            // if any time periods had been closed, the retained earnings account may have a posted balance but for all accounting tags, which is
            // not appropriate from the accounting tags, so if accountings tags are used, then ignore any existing posted retained earnings balance
            // and just put the interim net income as retained earnings
            if (accountingTagsUsed) {
                equityAccountBalances.put(retainedEarningsGlAccount, interimNetIncome);
            } else {
                UtilCommon.addInMapOfBigDecimal(equityAccountBalances, retainedEarningsGlAccount, interimNetIncome);
            }

            // TODO: This is just copied over from getIncomeStatementByDates for now.  We should implement a good version at some point.
            boolean isClosed = true;
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                    EntityCondition.makeCondition("isClosed", EntityOperator.NOT_EQUAL, "Y"),
                    EntityCondition.makeCondition(EntityOperator.OR,
                            EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, lastClosedDate),
                            EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDate)));
            List timePeriods = delegator.findByCondition("CustomTimePeriod", conditions, UtilMisc.toList("customTimePeriodId"), UtilMisc.toList("customTimePeriodId"));
            if (timePeriods.size() > 0) {
                isClosed = false;
            }

            // all done
            Map result = ServiceUtil.returnSuccess();
            result.put("assetAccountBalances", assetAccountBalances);
            result.put("liabilityAccountBalances", liabilityAccountBalances);
            result.put("equityAccountBalances", equityAccountBalances);
            result.put("isClosed", new Boolean(isClosed));
            result.put("retainedEarningsGlAccount", retainedEarningsGlAccount);
            result.put("interimNetIncomeAmount", interimNetIncome);
            return result;
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

    }

    /**
     * Finds and returns a List of AcctgTransAndEntries based on organizationPartyId, fromDate, thruDate, fiscalTypeId,
     *       subject to the glAccountClassIds in the glAccountClasses List.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getAcctgTransAndEntriesByType(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");
        String organizationPartyId = (String) context.get("organizationPartyId");
        String glFiscalTypeId = (String) context.get("glFiscalTypeId");
        List<String> glAccountClasses = (List) context.get("glAccountClasses"); // search for those in a set of glAccountClassId
        List<String> glAccountTypes = (List) context.get("glAccountTypes"); // search for those in a set of glAccountTypeId
        String productId = (String) context.get("productId");
        String partyId = (String) context.get("partyId");
        // this defaults to FINANCIALS_REPORTS_TAG (see the service definition)
        String accountingTagUsage = (String) context.get("accountingTagUsage");
        List<String> ignoreAcctgTransTypeIds = (List<String>) context.get("ignoreAcctgTransTypeIds");

        if (UtilValidate.isEmpty(glAccountClasses) && UtilValidate.isEmpty(glAccountTypes)) {
            return ServiceUtil.returnError("Please supply either a list of glAccountClassId or glAccountTypeId");
        }

        try {
            // build a condition list of all the GlAccountClasses considered (this entity has parent/child tree structure)
            List<EntityCondition> glAccountClassesConsidered = new ArrayList();
            if (glAccountClasses != null) {
                for (String glAccountClassId : glAccountClasses) {
                    glAccountClassesConsidered.add(UtilFinancial.getGlAccountClassExpr(glAccountClassId, delegator));
                }
            }

            // find all accounting transaction entries for this organizationPartyId and falling into this time period which are
            // of the specified types.  Note we are only getting posted transactions here.  This might change at some point.
            List searchConditions = UtilMisc.toList(
                    EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                    EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"),
                    EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, glFiscalTypeId),
                    EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate),
                    EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
            if (UtilValidate.isNotEmpty(glAccountClassesConsidered)) {
                searchConditions.add(EntityCondition.makeCondition(glAccountClassesConsidered, EntityOperator.OR));
            }
            if (UtilValidate.isNotEmpty(glAccountTypes)) {
                searchConditions.add(EntityCondition.makeCondition("glAccountTypeId", EntityOperator.IN, glAccountTypes));
            }
            if (UtilValidate.isNotEmpty(ignoreAcctgTransTypeIds)) {
                searchConditions.add(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.NOT_IN, ignoreAcctgTransTypeIds));
            }
            if (UtilValidate.isNotEmpty(productId)) {
                searchConditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
            }
            if (UtilValidate.isNotEmpty(partyId)) {
                searchConditions.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
            }
            List<EntityCondition> tagConditions = UtilAccountingTags.buildTagConditions(organizationPartyId, accountingTagUsage, delegator, context);
            if (UtilValidate.isNotEmpty(tagConditions)) {
                searchConditions.addAll(tagConditions);
            }

            EntityCondition conditions = EntityCondition.makeCondition(searchConditions, EntityOperator.AND);

            List fieldsToGet = UtilMisc.toList("acctgTransId", "acctgTransTypeId", "acctgTransEntrySeqId", "glAccountId", "glAccountClassId", "amount");
            fieldsToGet.add("glAccountTypeId");
            fieldsToGet.add("debitCreditFlag");
            fieldsToGet.add("productId");
            fieldsToGet.add("partyId");
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                fieldsToGet.add(UtilAccountingTags.ENTITY_TAG_PREFIX + i);
            }
            List transactionEntries = delegator.findByCondition("AcctgTransAndEntries", conditions,
                    fieldsToGet, // get these fields
                    UtilMisc.toList("acctgTransId", "acctgTransEntrySeqId")); // order by these fields

            Map result = ServiceUtil.returnSuccess();
            result.put("transactionEntries", transactionEntries);
            return result;
        }  catch (GeneralException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Gets a Map of glAccount -> sum of transactions for all income statement accounts (REVENUE, EXPENSE, INCOME) over a period of dates for an organization.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getIncomeStatementAccountSumsByDate(DispatchContext dctx, Map context) {
        return getIncomeStatementAccountSumsCommon(dctx, context);
    }

    /**
     * Takes an initial <code>Map</code> of <code>GlAccount</code>, sums and a <code>List</code> of <code>AcctgTransAndEntries</code> and adds them to the Map,
     *  based on debit/credit flag of transaction transactionEntry and whether the account is a debit or credit account.
     *  Useful for doing income statement and intra-time-period updating of balance sheets, etc.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map addToAccountBalances(DispatchContext dctx, Map context) {
        Map<GenericValue, BigDecimal> glAccountSums = (Map<GenericValue, BigDecimal>) context.get("glAccountSums");
        List<GenericValue> transactionEntries = (List<GenericValue>) context.get("transactionEntries");

        try {
            UtilFinancial.sumBalancesByAccount(glAccountSums, transactionEntries);
            Map result = ServiceUtil.returnSuccess();
            result.put("glAccountSums", glAccountSums);
            return result;
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Takes an <code>initial</code> Map of <code>GlAccount</code>, sums and a <code>List</code> of <code>AcctgTransAndEntries</code> and adds them to the Map,
     *  based on debit/credit flag of transaction transactionEntry and whether the account is a debit or credit account.
     *  Useful for doing income statement and intra-time-period updating of balance sheets, etc.
     * This is like {@link #addToAccountBalances} but returns three <code>Map</code> : <code>glAccountBalancesSums</code>, <code>glAccountDebitsSums</code> and <code>glAccountCreditsSums</code>.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map addToAccountBalancesWithDetails(DispatchContext dctx, Map context) {
        Map<GenericValue, BigDecimal> glAccountBalancesSums = (Map<GenericValue, BigDecimal>) context.get("glAccountBalancesSums");
        Map<GenericValue, BigDecimal> glAccountDebitsSums   = (Map<GenericValue, BigDecimal>) context.get("glAccountDebitsSums");
        Map<GenericValue, BigDecimal> glAccountCreditsSums  = (Map<GenericValue, BigDecimal>) context.get("glAccountCreditsSums");
        List<GenericValue> transactionEntries = (List<GenericValue>) context.get("transactionEntries");

        try {
            UtilFinancial.sumBalancesByAccountWithDetail(glAccountBalancesSums, glAccountDebitsSums, glAccountCreditsSums, transactionEntries);
            Map result = ServiceUtil.returnSuccess();
            result.put("glAccountBalancesSums", glAccountBalancesSums);
            result.put("glAccountDebitsSums", glAccountDebitsSums);
            result.put("glAccountCreditsSums", glAccountCreditsSums);
            return result;
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }


    /**
     * Little method to help out getBalanceSheetForDate.  Gets all AcctgTransAndEntries of a glAccountClassId and add them to the
     * original accountBalances Map, which is returned.
     * @param accountBalances
     * @param organizationPartyId
     * @param fromDate
     * @param thruDate
     * @param glFiscalTypeId
     * @param glAccountClassId
     * @param tags the <code>Map</code> of accounting tags
     * @param userLogin the user login <code>GenericValue</code>
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Map<GenericValue, BigDecimal> getAcctgTransAndEntriesForClass(Map<GenericValue, BigDecimal> accountBalances, String organizationPartyId, Timestamp fromDate, Timestamp thruDate, String glFiscalTypeId, String glAccountClassId, Map tags, GenericValue userLogin, LocalDispatcher dispatcher) {
        return getAcctgTransAndEntriesForClass(accountBalances, organizationPartyId, fromDate, thruDate, glFiscalTypeId, glAccountClassId, tags, false, userLogin, dispatcher);
    }


    /**
     * Little method to help out getBalanceSheetForDate.  Gets all AcctgTransAndEntries of a glAccountClassId and add them to the
     * original accountBalances Map, which is returned.
     * @param accountBalances
     * @param organizationPartyId
     * @param fromDate
     * @param thruDate
     * @param glFiscalTypeId
     * @param glAccountClassId
     * @param tags the <code>Map</code> of accounting tags
     * @param ignoreClosingPeriodTransactions used for getBalanceSheetForDate so as the closing transaction is already accounted for
     * @param userLogin the user login <code>GenericValue</code>
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Map<GenericValue, BigDecimal> getAcctgTransAndEntriesForClass(Map<GenericValue, BigDecimal> accountBalances, String organizationPartyId, Timestamp fromDate, Timestamp thruDate, String glFiscalTypeId, String glAccountClassId, Map tags, boolean ignoreClosingPeriodTransactions, GenericValue userLogin, LocalDispatcher dispatcher) {
        try {
            // first get all the AcctgTransAndEntries of this glAccountClassId
            Map input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "fromDate", fromDate, "thruDate", thruDate, "glFiscalTypeId", glFiscalTypeId, "glAccountClasses", UtilMisc.toList(glAccountClassId), "userLogin", userLogin);
            if (ignoreClosingPeriodTransactions) {
                input.put("ignoreAcctgTransTypeIds", UtilMisc.toList("PERIOD_CLOSING"));
            }

            if (tags != null) {
                input.putAll(tags);
            }
            Map tmpResult = dispatcher.runSync("getAcctgTransAndEntriesByType", input);
            List<GenericValue> transactionEntries = (List<GenericValue>) tmpResult.get("transactionEntries");

            // now add it to accountBalances
            tmpResult = dispatcher.runSync("addToAccountBalances", UtilMisc.toMap("glAccountSums", accountBalances, "transactionEntries", transactionEntries, "userLogin", userLogin));
            accountBalances = (Map<GenericValue, BigDecimal>) tmpResult.get("glAccountSums");
            return accountBalances;
        } catch (GenericServiceException ex) {
            Debug.logError(ex.getMessage(), MODULE);
            return null;
        }
    }

    /**
     * Little method to help out getBalanceSheetForDate.  Gets all AcctgTransAndEntries of a glAccountClassId and add them to the
     * original accountBalances Map, which is returned.
     * @param accountBalances output Map of GLAccount to BigDecimal
     * @param accountDebits output Map of GLAccount to BigDecimal
     * @param accountCredits output Map of GLAccount to BigDecimal
     * @param organizationPartyId
     * @param fromDate
     * @param thruDate
     * @param glFiscalTypeId
     * @param glAccountClassId
     * @param tags the <code>Map</code> of accounting tags
     * @param userLogin the user login <code>GenericValue</code>
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Map<GenericValue, BigDecimal> getAcctgTransAndEntriesForClassWithDetails(Map<GenericValue, BigDecimal> accountBalances, Map<GenericValue, BigDecimal> accountDebits, Map<GenericValue, BigDecimal> accountCredits, String organizationPartyId, Timestamp fromDate, Timestamp thruDate, String glFiscalTypeId, String glAccountClassId, Map tags, GenericValue userLogin, LocalDispatcher dispatcher) {
        try {
            // first get all the AcctgTransAndEntries of this glAccountClassId
            Map input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "fromDate", fromDate, "thruDate", thruDate, "glFiscalTypeId", glFiscalTypeId, "glAccountClasses", UtilMisc.toList(glAccountClassId), "userLogin", userLogin);
            if (tags != null) {
                input.putAll(tags);
            }
            Map tmpResult = dispatcher.runSync("getAcctgTransAndEntriesByType", input);
            List<GenericValue> transactionEntries = (List<GenericValue>) tmpResult.get("transactionEntries");

            // now add it to accountBalances
            tmpResult = dispatcher.runSync("addToAccountBalancesWithDetails", UtilMisc.toMap("glAccountBalancesSums", accountBalances, "glAccountDebitsSums", accountDebits, "glAccountCreditsSums", accountCredits, "transactionEntries", transactionEntries, "userLogin", userLogin));
            accountBalances = (Map<GenericValue, BigDecimal>) tmpResult.get("glAccountBalancesSums");
            accountDebits = (Map<GenericValue, BigDecimal>) tmpResult.get("glAccountDebitsSums");
            accountCredits = (Map<GenericValue, BigDecimal>) tmpResult.get("glAccountCreditsSums");
            return accountBalances;
        } catch (GenericServiceException ex) {
            Debug.logError(ex.getMessage(), MODULE);
            return null;
        }
    }

    /**
     * Generates balance sheet for two dates and determines the balance difference between the two. The balances are in BigDecimal.
     * The output includes the result of getBalanceSheetForDate for the fromDate and thruDate and glFiscalTypeId1 and glFiscalTypeId2.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getComparativeBalanceSheet(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);

        // input parameters
        String organizationPartyId = (String) context.get("organizationPartyId");
        String glFiscalTypeId1 = (String) context.get("glFiscalTypeId1");
        String glFiscalTypeId2 = (String) context.get("glFiscalTypeId2");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            // create the balance sheet for the fromDate
            Map input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "glFiscalTypeId", glFiscalTypeId1, "asOfDate", fromDate, "userLogin", userLogin);
            UtilAccountingTags.addTagParameters(context, input);
            Map fromDateResults = dispatcher.runSync("getBalanceSheetForDate", input);
            if (ServiceUtil.isError(fromDateResults)) {
                return UtilMessage.createAndLogServiceError(fromDateResults, "FinancialsError_CannotCreateComparativeBalanceSheet", locale, MODULE);
            }

            // create the balance sheet for the thruDate
            input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "glFiscalTypeId", glFiscalTypeId2, "asOfDate", thruDate, "userLogin", userLogin);
            UtilAccountingTags.addTagParameters(context, input);
            Map thruDateResults = dispatcher.runSync("getBalanceSheetForDate", input);
            if (ServiceUtil.isError(thruDateResults)) {
                return UtilMessage.createAndLogServiceError(thruDateResults, "FinancialsError_CannotCreateComparativeBalanceSheet", locale, MODULE);
            }

            Map results = ServiceUtil.returnSuccess();

            // include the two balance sheets in the results
            results.put("fromDateAccountBalances", fromDateResults);
            results.put("thruDateAccountBalances", thruDateResults);

            // compute the balance difference for each type of account
            results.put("liabilityAccountBalances",
                    calculateDifferenceBalance((Map) fromDateResults.get("liabilityAccountBalances"), (Map) thruDateResults.get("liabilityAccountBalances")));
            results.put("assetAccountBalances",
                    calculateDifferenceBalance((Map) fromDateResults.get("assetAccountBalances"), (Map) thruDateResults.get("assetAccountBalances")));
            results.put("equityAccountBalances",
                    calculateDifferenceBalance((Map) fromDateResults.get("equityAccountBalances"), (Map) thruDateResults.get("equityAccountBalances")));
            return results;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "FinancialsError_CannotCreateComparativeBalanceSheet", locale, MODULE);
        }
    }

    /**
     * Generates income statement for two sets of dates and glFiscalTypeIds and determines the difference between the two. The balances are in BigDecimal.
     * The output includes the result of getIncomeStatementByDates for the two sets of fromDates and thruDates.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getComparativeIncomeStatement(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);

        // input parameters
        String organizationPartyId = (String) context.get("organizationPartyId");
        String glFiscalTypeId1 = (String) context.get("glFiscalTypeId1");
        String glFiscalTypeId2 = (String) context.get("glFiscalTypeId2");
        Timestamp fromDate1 = (Timestamp) context.get("fromDate1");
        Timestamp thruDate1 = (Timestamp) context.get("thruDate1");
        Timestamp fromDate2 = (Timestamp) context.get("fromDate2");
        Timestamp thruDate2 = (Timestamp) context.get("thruDate2");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // validate the from/thru dates
        if (fromDate1.after(thruDate1) || fromDate2.after(thruDate2)) {
            return UtilMessage.createAndLogServiceError("FinancialsError_CannotCreateComparativeIncomeStatementFromDateAfterThruDate", locale, MODULE);
        }

        try {
            // create the income statement for the fromDate
            Map input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "glFiscalTypeId", glFiscalTypeId1, "fromDate", fromDate1, "thruDate", thruDate1, "userLogin", userLogin);
            UtilAccountingTags.addTagParameters(context, input);
            Map set1Results = dispatcher.runSync("getIncomeStatementByDates", input);
            if (ServiceUtil.isError(set1Results)) {
                return UtilMessage.createAndLogServiceError(set1Results, "FinancialsError_CannotCreateComparativeIncomeStatement", locale, MODULE);
            }

            // create the balance sheet for the thruDate
            input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "glFiscalTypeId", glFiscalTypeId2, "fromDate", fromDate2, "thruDate", thruDate2, "userLogin", userLogin);
            UtilAccountingTags.addTagParameters(context, input);
            Map set2Results = dispatcher.runSync("getIncomeStatementByDates", input);
            if (ServiceUtil.isError(set2Results)) {
                return UtilMessage.createAndLogServiceError(set2Results, "FinancialsError_CannotCreateComparativeIncomeStatement", locale, MODULE);
            }

            Map results = ServiceUtil.returnSuccess();

            // include the two income statements in the results
            results.put("set1IncomeStatement", set1Results);
            results.put("set2IncomeStatement", set2Results);

            // compute the balance difference
            results.put("accountBalances", calculateDifferenceBalance((Map) set1Results.get("glAccountSumsFlat"), (Map) set2Results.get("glAccountSumsFlat")));

            return results;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "FinancialsError_CannotCreateComparativeIncomeStatement", locale, MODULE);
        }
    }

    /**
     * Generates cash flow statement for two sets of dates and glFiscalTypeIds and determines the difference between the two. The balances are in BigDecimal.
     * The output includes the result of getCashFlowStatementForDates for the two sets of fromDates and thruDates.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getComparativeCashFlowStatement(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);

        // input parameters
        String organizationPartyId = (String) context.get("organizationPartyId");
        String glFiscalTypeId1 = (String) context.get("glFiscalTypeId1");
        String glFiscalTypeId2 = (String) context.get("glFiscalTypeId2");
        Timestamp fromDate1 = (Timestamp) context.get("fromDate1");
        Timestamp thruDate1 = (Timestamp) context.get("thruDate1");
        Timestamp fromDate2 = (Timestamp) context.get("fromDate2");
        Timestamp thruDate2 = (Timestamp) context.get("thruDate2");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // validate the from/thru dates
        if (fromDate1.after(thruDate1) || fromDate2.after(thruDate2)) {
            return UtilMessage.createAndLogServiceError("FinancialsError_CannotCreateComparativeCashFlowStatementFromDateAfterThruDate", locale, MODULE);
        }

        try {
            // create the cash flow statement for the fromDate
            Map input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "glFiscalTypeId", glFiscalTypeId1, "fromDate", fromDate1, "thruDate", thruDate1, "userLogin", userLogin);
            UtilAccountingTags.addTagParameters(context, input);
            Map set1Results = dispatcher.runSync("getCashFlowStatementForDates", input);
            if (ServiceUtil.isError(set1Results)) {
                return UtilMessage.createAndLogServiceError(set1Results, "FinancialsError_CannotCreateComparativeCashFlowStatement", locale, MODULE);
            }

            // create the balance sheet for the thruDate
            input = UtilMisc.toMap("organizationPartyId", organizationPartyId, "glFiscalTypeId", glFiscalTypeId2, "fromDate", fromDate2, "thruDate", thruDate2, "userLogin", userLogin);
            UtilAccountingTags.addTagParameters(context, input);
            Map set2Results = dispatcher.runSync("getCashFlowStatementForDates", input);
            if (ServiceUtil.isError(set2Results)) {
                return UtilMessage.createAndLogServiceError(set2Results, "FinancialsError_CannotCreateComparativeCashFlowStatement", locale, MODULE);
            }

            Map results = ServiceUtil.returnSuccess();

            // include the two income statements in the results
            results.put("set1CashFlowStatement", set1Results);
            results.put("set2CashFlowStatement", set2Results);

            // compute the balance difference
            results.put("beginningCashAccountBalances", calculateDifferenceBalance((Map) set1Results.get("beginningCashAccountBalances"), (Map) set2Results.get("beginningCashAccountBalances")));
            results.put("endingCashAccountBalances", calculateDifferenceBalance((Map) set1Results.get("endingCashAccountBalances"), (Map) set2Results.get("endingCashAccountBalances")));
            results.put("operatingCashFlowAccountBalances", calculateDifferenceBalance((Map) set1Results.get("operatingCashFlowAccountBalances"), (Map) set2Results.get("operatingCashFlowAccountBalances")));
            results.put("investingCashFlowAccountBalances", calculateDifferenceBalance((Map) set1Results.get("investingCashFlowAccountBalances"), (Map) set2Results.get("investingCashFlowAccountBalances")));
            results.put("financingCashFlowAccountBalances", calculateDifferenceBalance((Map) set1Results.get("financingCashFlowAccountBalances"), (Map) set2Results.get("financingCashFlowAccountBalances")));

            return results;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "FinancialsError_CannotCreateComparativeCashFlowStatement", locale, MODULE);
        }
    }

    /**
     * Calculates the difference between the two balances for the input maps of {account, fromDateBalance} and {account, thruDateBalance}.
     * @param fromDateMap a <code>Map</code> value
     * @param thruDateMap a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    private static Map<GenericValue, BigDecimal> calculateDifferenceBalance(Map fromDateMap, Map thruDateMap) {
        Map<GenericValue, BigDecimal> resultMap = FastMap.newInstance();

        // first iterate through the thru date accounts
        for (Iterator iter = thruDateMap.keySet().iterator(); iter.hasNext();) {
            GenericValue account = (GenericValue) iter.next();
            BigDecimal thruDateBalance;
            if (thruDateMap.get(account) != null && thruDateMap.get(account) instanceof Double) {
                thruDateBalance = BigDecimal.valueOf((Double) thruDateMap.get(account));
            } else if (thruDateMap.get(account) != null && thruDateMap.get(account) instanceof BigDecimal) {
                thruDateBalance = (BigDecimal) thruDateMap.get(account);
            } else {
                thruDateBalance = BigDecimal.ZERO;
            }
            BigDecimal fromDateBalance;
            if (fromDateMap.get(account) != null && fromDateMap.get(account) instanceof Double) {
                fromDateBalance = BigDecimal.valueOf((Double) fromDateMap.get(account));
            } else if (fromDateMap.get(account) != null && fromDateMap.get(account) instanceof BigDecimal) {
                fromDateBalance = (BigDecimal) fromDateMap.get(account);
            } else {
                fromDateBalance = BigDecimal.ZERO;
            }
            BigDecimal difference = thruDateBalance.subtract(fromDateBalance);
            resultMap.put(account, difference.setScale(decimals, rounding));
        }

        // iterate through the from date accounts that were missed because no thru date account exists
        for (Iterator iter = fromDateMap.keySet().iterator(); iter.hasNext();) {
            GenericValue account = (GenericValue) iter.next();
            if (resultMap.get(account) != null) {
                continue; // already have a balance
            }
            BigDecimal fromDateBalance;
            if (fromDateMap.get(account) != null && fromDateMap.get(account) instanceof Double) {
                fromDateBalance = BigDecimal.valueOf((Double) fromDateMap.get(account));
            } else if (fromDateMap.get(account) != null && fromDateMap.get(account) instanceof BigDecimal) {
                fromDateBalance = (BigDecimal) fromDateMap.get(account);
            } else {
                fromDateBalance = BigDecimal.ZERO;
            }
            if (fromDateBalance == null) {
                fromDateBalance = BigDecimal.ZERO;
            }
            BigDecimal difference = fromDateBalance.negate();
            resultMap.put(account, difference.setScale(decimals, rounding));
        }

        return resultMap;
    }

    /**
     * Helper method to transform input from and thru time periods to fromDate and thruDates. It will then call
     * the given service with the given input map and handle service/entity exceptions.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @param serviceName a <code>String</code> value
     * @param input a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    private static Map reportServiceTimePeriodHelper(DispatchContext dctx, Map context, String serviceName) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String fromTimePeriodId = (String) context.get("fromTimePeriodId");
        String thruTimePeriodId = (String) context.get("thruTimePeriodId");
        String organizationPartyId = (String) context.get("organizationPartyId");

        try {
            // get the right start and ending custom time periods
            GenericValue fromTimePeriod = delegator.findByPrimaryKey("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", fromTimePeriodId));
            GenericValue thruTimePeriod = delegator.findByPrimaryKey("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", thruTimePeriodId));

            // make sure these time periods belong to the organization and the from and thru dates are there
            if (fromTimePeriod == null) {
                return ServiceUtil.returnError("Custom time period " + fromTimePeriodId + " does not exist");
            }
            if (thruTimePeriod == null) {
                return ServiceUtil.returnError("Custom time period " + thruTimePeriodId + " does not exist");
            }
            if (!(fromTimePeriod.getString("organizationPartyId").equals(organizationPartyId))) {
                return ServiceUtil.returnError("Custom time period " + fromTimePeriodId + " does not belong to " + organizationPartyId);
            }
            if (!(thruTimePeriod.getString("organizationPartyId").equals(organizationPartyId))) {
                return ServiceUtil.returnError("Custom time period " + thruTimePeriodId + " does not belong to " + organizationPartyId);
            }
            if (fromTimePeriod.get("fromDate") == null) {
                return ServiceUtil.returnError("Cannot get a starting date from custom time period = " + fromTimePeriodId);
            } else if (thruTimePeriod.get("thruDate") == null) {
                return ServiceUtil.returnError("Cannot get a starting date from custom time period = " + thruTimePeriodId);
            }

            // call the service and pass back the results.
            Map input = new HashMap(context);
            input.put("fromDate", UtilDateTime.toTimestamp(fromTimePeriod.getDate("fromDate")));
            input.put("thruDate", UtilDateTime.toTimestamp(thruTimePeriod.getDate("thruDate")));
            input = dctx.getModelService(serviceName).makeValid(input, ModelService.IN_PARAM);
            Map result = dispatcher.runSync(serviceName, input);
            return result;
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Generates a cash flow statement.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getCashFlowStatementForTimePeriods(DispatchContext dctx, Map context) {
        return reportServiceTimePeriodHelper(dctx, context, "getCashFlowStatementForDates");
    }

    /**
     * Generates a cash flow statement.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getCashFlowStatementForDates(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");
        String organizationPartyId = (String) context.get("organizationPartyId");
        String glFiscalTypeId = (String) context.get("glFiscalTypeId");

        try {
            Map input = dctx.getModelService("getComparativeBalanceSheet").makeValid(context, ModelService.IN_PARAM);
            Map comparativeResults = dispatcher.runSync("getComparativeBalanceSheet", input);
            if (ServiceUtil.isError(comparativeResults)) {
                return ServiceUtil.returnError("Failed to compute cash flow statement. ", null, null, comparativeResults);
            }

            // extract the from date balance sheet and thru date balance sheet for convenience
            Map fromDateAccountBalances = (Map) comparativeResults.get("fromDateAccountBalances");
            Map thruDateAccountBalances = (Map) comparativeResults.get("thruDateAccountBalances");

            // run income statement for same date range
            input = dctx.getModelService("getIncomeStatementByDates").makeValid(context, ModelService.IN_PARAM);
            Map incomeResults = dispatcher.runSync("getIncomeStatementByDates", input);
            if (ServiceUtil.isError(incomeResults)) {
                return ServiceUtil.returnError("Failed to compute cash flow statement. ", null, null, incomeResults);
            }

            // extract the netIncome for convenience
            BigDecimal netIncome = (BigDecimal) incomeResults.get("netIncome");

            // compute the beginning cash amount from accounts with glAccountClassId = CASH_EQUIVALENT and make a map of these accounts for return
            BigDecimal beginningCashAmount = ZERO;
            Map<GenericValue, BigDecimal> beginningCashAccountBalances = FastMap.newInstance();
            Map<GenericValue, BigDecimal> beginningSheet = (Map<GenericValue, BigDecimal>) fromDateAccountBalances.get("assetAccountBalances"); // beginning cash equivalents are in from date balance sheet assets
            for (Iterator<GenericValue> iter = beginningSheet.keySet().iterator(); iter.hasNext();) {
                GenericValue account = iter.next();
                if (!UtilAccounting.isAccountClass(account, "CASH_EQUIVALENT")) {
                    continue; // skip non cash equivalent accounts
                }
                BigDecimal amount = beginningSheet.get(account);
                beginningCashAccountBalances.put(account, amount);
                beginningCashAmount = beginningCashAmount.add(amount).setScale(decimals, rounding);
            }

            // compute the ending cash amount from accounts with glAccountClassId = CASH_EQUIVALENT and make a map of these accounts for return
            BigDecimal endingCashAmount = ZERO;
            Map<GenericValue, BigDecimal> endingCashAccountBalances = FastMap.newInstance();
            Map<GenericValue, BigDecimal> endingSheet = (Map<GenericValue, BigDecimal>) thruDateAccountBalances.get("assetAccountBalances"); // ending cash equivalents are in thru date balance sheet assets
            for (Iterator<GenericValue> iter = endingSheet.keySet().iterator(); iter.hasNext();) {
                GenericValue account = iter.next();
                if (!UtilAccounting.isAccountClass(account, "CASH_EQUIVALENT")) {
                    continue; // use our handy recursive method to test the class
                }
                BigDecimal amount = endingSheet.get(account);
                endingCashAccountBalances.put(account, amount);
                endingCashAmount = endingCashAmount.add(amount).setScale(decimals, rounding);
            }

            // cash flow amounts
            BigDecimal operatingCashFlow = ZERO;
            BigDecimal investingCashFlow = ZERO;
            BigDecimal financingCashFlow = ZERO;

            // cash flow maps of accounts to amounts
            Map operatingCashFlowAccountBalances = FastMap.newInstance();
            Map investingCashFlowAccountBalances = FastMap.newInstance();
            Map financingCashFlowAccountBalances = FastMap.newInstance();

            // add net income to operating cash flow
            operatingCashFlow = netIncome;

            // add non cash expense accounts to the operating cash flow
            Map<GenericValue, BigDecimal> glAccountSums = (Map<GenericValue, BigDecimal>) incomeResults.get("glAccountSumsFlat");
            if ((glAccountSums != null) && (glAccountSums.keySet() != null)) {
                for (Iterator<GenericValue> iter = glAccountSums.keySet().iterator(); iter.hasNext();) {
                    GenericValue account = iter.next();
                    if (UtilAccounting.isAccountClass(account, "NON_CASH_EXPENSE") && !UtilAccounting.isAccountClass(account, "INVENTORY_ADJUST")) {
                        BigDecimal amount = glAccountSums.get(account);
                        // we can just add the amount, because expenses are Debits and all expense accounts are debit accounts
                        // so the amount should already be positive
                        operatingCashFlowAccountBalances.put(account, amount);
                        operatingCashFlow = operatingCashFlow.add(amount).setScale(decimals, rounding);
                    }
                }
            }

            // compute the cash flows from assets
            Map<GenericValue, BigDecimal> statement = (Map<GenericValue, BigDecimal>) comparativeResults.get("assetAccountBalances");
            if ((statement != null) && (statement.keySet() != null)) {
                for (Iterator<GenericValue> iter = statement.keySet().iterator(); iter.hasNext();) {
                    GenericValue account = iter.next();

                    if (UtilAccounting.isAccountClass(account, "CURRENT_ASSET") && !UtilAccounting.isAccountClass(account, "CASH_EQUIVALENT")) {
                        // get current assets that are not cash equivalent accounts and flip the sign of the amounts, then add to operating cash flow
                        BigDecimal amount = statement.get(account);
                        amount = ZERO.subtract(amount).setScale(decimals, rounding); // flip the sign and use this value
                        operatingCashFlowAccountBalances.put(account, amount);
                        operatingCashFlow = operatingCashFlow.add(amount).setScale(decimals, rounding);
                    } else if (UtilAccounting.isAccountClass(account, "LONGTERM_ASSET")
                             && !UtilAccounting.isAccountClass(account, "ACCUM_DEPRECIATION")
                             && !UtilAccounting.isAccountClass(account, "ACCUM_AMORTIZATION")) {
                        // add to investing cash flow any long term assets
                        BigDecimal amount = statement.get(account);
                        investingCashFlowAccountBalances.put(account, amount.negate());
                        investingCashFlow = investingCashFlow.subtract(statement.get(account)).setScale(decimals, rounding);
                    }
                }
            }

            // compute the cash flows from liabilities
            statement = (Map<GenericValue, BigDecimal>) comparativeResults.get("liabilityAccountBalances");
            if ((statement != null) && (statement.keySet() != null)) {
                for (Iterator<GenericValue> iter = statement.keySet().iterator(); iter.hasNext();) {
                    GenericValue account = iter.next();

                    if (UtilAccounting.isAccountClass(account, "CURRENT_LIABILITY")) {
                        // add to operating cash flow any current liabilities
                        operatingCashFlowAccountBalances.put(account, statement.get(account));
                        operatingCashFlow = operatingCashFlow.add(statement.get(account)).setScale(decimals, rounding);
                    } else if (UtilAccounting.isAccountClass(account, "LONGTERM_LIABILITY")) {
                        // add to financing cash flow any long term liabilities
                        financingCashFlowAccountBalances.put(account, statement.get(account));
                        financingCashFlow = financingCashFlow.add(statement.get(account)).setScale(decimals, rounding);
                    }
                }
            }

            // compute the cash flows from equity (sans the retained earnings accounts)
            statement = (Map<GenericValue, BigDecimal>) comparativeResults.get("equityAccountBalances");
            if ((statement != null) && (statement.keySet() != null)) {
                for (Iterator<GenericValue> iter = statement.keySet().iterator(); iter.hasNext();) {
                    GenericValue account = iter.next();

                    // add all equity to financing cash flow
                    if (UtilAccounting.isAccountClass(account, "OWNERS_EQUITY")) {
                        financingCashFlowAccountBalances.put(account, statement.get(account));
                        financingCashFlow = financingCashFlow.add(statement.get(account)).setScale(decimals, rounding);
                    }
                }
            }

            // handle DISTRIBUTION account transactions
            statement = new HashMap<GenericValue, BigDecimal>(); // need to clear it now because this method adds the new DISTRIBUTION accounts and sums to the Map
            statement = getAcctgTransAndEntriesForClass(statement, organizationPartyId, fromDate, thruDate, glFiscalTypeId, "DISTRIBUTION", UtilAccountingTags.getTagParameters(context), userLogin, dispatcher);
            if ((statement != null) && (statement.keySet() != null)) {
                for (Iterator<GenericValue> iter = statement.keySet().iterator(); iter.hasNext();) {
                    GenericValue account = iter.next();
                    if (statement.get(account) != null) {
                        BigDecimal amount = statement.get(account);
                        // here: a DISTRIBUTION should be a Debit transaction on a Credit (Equity) account, so it should already be negative
                        // and can be added directly to the net financing cash flows
                        financingCashFlowAccountBalances.put(account, amount);
                        financingCashFlow = financingCashFlow.add(amount).setScale(decimals, rounding);
                    }
                }
            }

            // complementary validation that the net cash flow is equal for the two methods to compute it
            BigDecimal netCashFlowOne = endingCashAmount.subtract(beginningCashAmount);
            BigDecimal netCashFlowTwo = operatingCashFlow.add(investingCashFlow).add(financingCashFlow);
            if (netCashFlowOne.compareTo(netCashFlowTwo) != 0) {
                Debug.logWarning("Net cash flow computation yielded different values! (ending cash amount - beginning cash amount) = ["
                        + netCashFlowOne.toString() + "]; (operating + investing + financing) = [" + netCashFlowTwo.toString() + "]", MODULE);
            } else {
                Debug.logInfo("Net cash flow computation yielded same values: (ending cash amount - beginning cash amount) = ["
                        + netCashFlowOne.toString() + "]; (operating + investing + financing) = [" + netCashFlowTwo.toString() + "]", MODULE);
            }

            Map results = ServiceUtil.returnSuccess();
            results.put("beginningCashAmount", beginningCashAmount);
            results.put("beginningCashAccountBalances", beginningCashAccountBalances);
            results.put("endingCashAmount", endingCashAmount);
            results.put("endingCashAccountBalances", endingCashAccountBalances);
            results.put("operatingCashFlowAccountBalances", operatingCashFlowAccountBalances);
            results.put("investingCashFlowAccountBalances", investingCashFlowAccountBalances);
            results.put("financingCashFlowAccountBalances", financingCashFlowAccountBalances);
            results.put("operatingCashFlow", operatingCashFlow);
            results.put("investingCashFlow", investingCashFlow);
            results.put("financingCashFlow", financingCashFlow);
            results.put("netIncome", netIncome);
            results.put("netCashFlow", netCashFlowTwo); // return (operating + investing + financing) as the net cash flow
            return results;
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to compute cash flow statement: " + e.getMessage(), MODULE);
            return ServiceUtil.returnError("Failed to compute cash flow statement: " + e.getMessage());
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failed to compute cash flow statement: " + e.getMessage(), MODULE);
            return ServiceUtil.returnError("Failed to compute cash flow statement: " + e.getMessage());
        }
    }

    /**
     * Gets a Map of glAccountId,tag1,tag2,... -> sum of transactions for all income statement accounts (REVENUE, EXPENSE, INCOME)
     * over a period of dates in sepcific tags for an organization.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getIncomeStatementAccountSumsByTagByDate(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String organizationPartyId = (String) context.get("organizationPartyId");
        // this defaults to FINANCIALS_REPORTS_TAG (see the service definition)
        String accountingTagUsage = (String) context.get("accountingTagUsage");
        try {
            DomainsLoader domainsLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domainsDirectory = domainsLoader.loadDomainsDirectory();
            OrganizationDomainInterface organizationDomain = domainsDirectory.getOrganizationDomain();
            OrganizationRepositoryInterface organizationRepository = organizationDomain.getOrganizationRepository();
            Organization organization = organizationRepository.getOrganizationById(organizationPartyId);
            Map accountingTags = organization.getAccountingTagTypes(accountingTagUsage);

            return getIncomeStatementAccountSumsCommon(dctx, context, accountingTags);

        } catch (EntityNotFoundException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (RepositoryException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Takes an initial Map of GlAccount, sums and a List of AcctgTransAndEntries and adds them to the Map,
     *       based on debit/credit flag of transaction transactionEntry and whether the account is a debit or credit account.
     *       Useful for doing income statement and intra-time-period updating of balance sheets, etc.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map addToAccountBalancesByTag(DispatchContext dctx, Map context) {
        Map glAccountSums = (Map) context.get("glAccountSums");
        Map accountingTags = (Map) context.get("accountingTags");
        List transactionEntries = (List) context.get("transactionEntries");

        try {
            UtilFinancial.sumBalancesByTag(glAccountSums, transactionEntries, accountingTags);
            Map result = ServiceUtil.returnSuccess();
            result.put("glAccountSums", glAccountSums);
            return result;
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Generates an income statement over a range of dates, returning a Map of GlAccount and amounts and a netIncome.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getIncomeStatementByTagByDates(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");
        String organizationPartyId = (String) context.get("organizationPartyId");
        String glFiscalTypeId = (String) context.get("glFiscalTypeId");
        // glFiscalTypeId defaults to ACTUAL
        if (UtilValidate.isEmpty(glFiscalTypeId)) {
            glFiscalTypeId = "ACTUAL";
            context.put("glFiscalTypeId", glFiscalTypeId);
        }

        try {
            // get a Map of glAccount -> sums for all income statement accounts for this time period
            Map input = dctx.getModelService("getIncomeStatementAccountSumsByTagByDate").makeValid(context, ModelService.IN_PARAM);
            Map tmpResult = dispatcher.runSync("getIncomeStatementAccountSumsByTagByDate", input);
            if (tmpResult.get("glAccountSums") == null) {
                return ServiceUtil.returnError("Cannot sum up account balances properly for income statement");
            }
            // note the key of this map is in the format: glAccountId,tag1,tag2,...
            Map<String, BigDecimal> glAccountSums = (HashMap<String, BigDecimal>) tmpResult.get("glAccountSums");

            Map<String, String> glAccountTypeTree = FastMap.newInstance();
            Map<String, List<Map>> glAccountSumsGrouped = FastMap.newInstance();
            Map<String, BigDecimal> glAccountGroupSums = FastMap.newInstance();
            Map<String, BigDecimal> sums = FastMap.newInstance();
            prepareIncomeStatementMaps(glAccountTypeTree, glAccountSumsGrouped, glAccountGroupSums, sums, delegator);

            // sort them into the correct map while also keeping a running total of the aggregations we're interested in (net income, gross profit, etc.)
            for (String key : glAccountSums.keySet()) {
                // note the key of this map is in the format: glAccountId,tag1,tag2,...
                String[] keyArray = key.split(",");
                String glAccountId = keyArray[0];

                GenericValue account = delegator.findByPrimaryKeyCache("GlAccount", UtilMisc.toMap("glAccountId", glAccountId));
                Map<String, String> tagMap = FastMap.newInstance();
                // put tags info into map (starting at index 1 since index 0 is the glAccountId)
                for (int i = 1; i < keyArray.length; i++) {
                    tagMap.put(UtilAccountingTags.ENTITY_TAG_PREFIX + i, keyArray[i]);
                }
                calculateIncomeStatementMaps(glAccountTypeTree, glAccountSumsGrouped, glAccountGroupSums, sums, account, glAccountSums.get(key), tagMap, delegator);
            }

            return makeIncomeStatementResults(glAccountSums, glAccountSumsGrouped, glAccountGroupSums, sums, organizationPartyId, fromDate, thruDate, delegator);

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Common method for the <code>getIncomeStatementByTagByDates</code> and <code>getIncomeStatementByDates</code> services.
     * Initializes the Maps used for the calculation.
     * @param glAccountTypeTree an empty <code>Map</code> instance to initialize
     * @param glAccountSumsGrouped an empty <code>Map</code> instance to initialize
     * @param glAccountGroupSums an empty <code>Map</code> instance to initialize
     * @param sums an empty <code>Map</code> instance to initialize
     * @param delegator a <code>Delegator</code> value
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static void prepareIncomeStatementMaps(Map<String, String> glAccountTypeTree, Map<String, List<Map>> glAccountSumsGrouped, Map<String, BigDecimal> glAccountGroupSums, Map<String, BigDecimal> sums, Delegator delegator) throws GenericEntityException {

        // Get a flattened glAccountTypeId -> glAccountTypeId tree where the children of the master INCOME_STATEMENT_TYPEs are related directly.
        // The keys are the children glAccountTypeId and the value is the root glAccountTypeId in INCOME_STATEMENT_TYPE
        for (String glAccountTypeId : INCOME_STATEMENT_TYPES) {
            List<GenericValue> childrenValues = UtilCommon.getEntityChildren(delegator, delegator.findByPrimaryKeyCache("GlAccountType", UtilMisc.toMap("glAccountTypeId", glAccountTypeId)));
            for (GenericValue child : childrenValues) {
                glAccountTypeTree.put(child.getString("glAccountTypeId"), glAccountTypeId);
            }
            // identity element
            glAccountTypeTree.put(glAccountTypeId, glAccountTypeId);
        }

        // Accounts without a type are put in a special unclassified group.  This is the identity element for those accounts.
        glAccountTypeTree.put(UNCLASSIFIED_TYPE, UNCLASSIFIED_TYPE);

        // We will group accounts into a Map with key in INCOME_STATEMENT_TYPES and values lists of accounts that are children of these types.
        // Instead of being a GenericValue, the account is a Map containing an additional field accountSum with the
        // correct sign for display of an income statement.
        // and a map to store the sums for each group

        // initialize the maps first
        for (String glAccountTypeId : INCOME_STATEMENT_TYPES) {
            List<Map> emptyList = FastList.newInstance();
            glAccountSumsGrouped.put(glAccountTypeId, emptyList);
            glAccountGroupSums.put(glAccountTypeId, BigDecimal.ZERO);
        }

        // add an unclassified group for those accounts that don't show up anywhere
        List<Map> emptyList = FastList.newInstance();
        glAccountSumsGrouped.put(UNCLASSIFIED_TYPE, emptyList);
        glAccountGroupSums.put(UNCLASSIFIED_TYPE, BigDecimal.ZERO);

        // initialize the sums Map
        sums.put("grossProfit", BigDecimal.ZERO);     // gross profit     = sum(REVENUE) - sum(COGS)
        sums.put("operatingIncome", BigDecimal.ZERO); // operating income = gross profit - sum(OPERATING_EXPENSE)
        sums.put("pretaxIncome", BigDecimal.ZERO);    // pretax income    = operating income + sum(OTHER_INCOME) - sum(OTHER_EXPENSE)
        sums.put("netIncome", BigDecimal.ZERO);       // net income       = pretax income - sum(TAX_EXPENSE)
    }

    /**
     * Common method for the <code>getIncomeStatementByDates</code> services.
     * Perform the calculation, using the given <code>account</code> and <code>sum</code> which are results from the <code>getIncomeStatementAccountSumsByDate</code> service. They should be returned in the <code>glAccountSums</code> result Map.
     * @param glAccountTypeTree a previously initialized <code>Map</code> instance of {glAccountTypeId: parent glAccountTypeId}
     * @param glAccountSumsGrouped a previously initialized <code>Map</code> instance of {glAccountTypeId: List of GL Account info map}, where the info map contains all the fields from the GL account, plus the total amount for this GL account
     * @param glAccountGroupSums a previously initialized <code>Map</code> instance of {glAccountTypeId: total amount}
     * @param sums a previously initialized <code>Map</code> instance containing grossProfit, operatingIncome, pretaxIncome and netIncome values
     * @param account the <code>GenericValue</code> that is iterated upon
     * @param sum the sum corresponding to the given GL account as given by the <code>getIncomeStatementAccountSumsByDate</code> services
     * @param delegator a <code>Delegator</code> value
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static void calculateIncomeStatementMaps(Map<String, String> glAccountTypeTree, Map<String, List<Map>> glAccountSumsGrouped, Map<String, BigDecimal> glAccountGroupSums, Map<String, BigDecimal> sums, GenericValue account, BigDecimal sum, Delegator delegator) throws GenericEntityException {
        calculateIncomeStatementMaps(glAccountTypeTree, glAccountSumsGrouped, glAccountGroupSums, sums, account, sum, new HashMap<String, String>(), delegator);
    }

    /**
     * Common method for the <code>getIncomeStatementByTagByDates</code> service.
     * Perform the calculation, using the given <code>account</code> and <code>sum</code> which are results from the <code>getIncomeStatementAccountSumsByTagByDate</code> service. They should be returned in the <code>glAccountSums</code> result Map.
     * @param glAccountTypeTree a previously initialized <code>Map</code> instance of {glAccountTypeId: parent glAccountTypeId}
     * @param glAccountSumsGrouped a previously initialized <code>Map</code> instance of {glAccountTypeId: List of GL Account info map}, where the info map contains all the fields from the GL account, plus the total amount for this GL account and the corresponding accounting tags
     * @param glAccountGroupSums a previously initialized <code>Map</code> instance of {glAccountTypeId: total amount}
     * @param sums a previously initialized <code>Map</code> instance containing grossProfit, operatingIncome, pretaxIncome and netIncome values
     * @param account the <code>GenericValue</code> that is iterated upon
     * @param sum the sum corresponding to the given GL account as given by the <code>getIncomeStatementAccountSumsByTagByDate</code> services
     * @param tagMap a <code>Map</code> of tag field to tag value which will be added to the GL account info map in the <code>glAccountGroupSumsGrouped</code> resulting <code>Map</code>
     * @param delegator a <code>Delegator</code> value
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static void calculateIncomeStatementMaps(Map<String, String> glAccountTypeTree, Map<String, List<Map>> glAccountSumsGrouped, Map<String, BigDecimal> glAccountGroupSums, Map<String, BigDecimal> sums, GenericValue account, BigDecimal sum, Map<String, String> tagMap, Delegator delegator) throws GenericEntityException {

        String glAccountTypeId = account.getString("glAccountTypeId");
        if (UtilValidate.isEmpty(glAccountTypeId))  {
            // accounts really *should* have a type TODO maybe put in separate group UNCONFIGURED_TYPE
            Debug.logWarning("Encountered GlAccount [" + account.get("glAccountId") + "] with no glAccountTypeId.", MODULE);
            glAccountTypeId = UNCLASSIFIED_TYPE;
        }

        // first determine what root glAccountType this is (note the setup of glAccountTypeTree above)
        glAccountTypeId = glAccountTypeTree.get(glAccountTypeId);
        if (UtilValidate.isEmpty(glAccountTypeId)) {
            // those that don't have a root in INCOME_STATEMENT_TYPES are considered unclassified
            glAccountTypeId = UNCLASSIFIED_TYPE;
        }

        // get the group of accounts
        List<Map> group = glAccountSumsGrouped.get(glAccountTypeId);
        if (group == null) {
            // if for some reason the glAccountTypeId was misconfigured (the class is correct, but not the type), then put this account in the unclassified group
            Debug.logWarning("Unable to determine what type of account [" + account.get("glAccountId") + "] is for purposes of income statement."
               + "  It needs to be a child type of the following GlAccountTypes: " + INCOME_STATEMENT_TYPES
               + ".  This account will be put in the unclassified group for now.", MODULE);
            glAccountTypeId = UNCLASSIFIED_TYPE;
            group = glAccountSumsGrouped.get(UNCLASSIFIED_TYPE);
        }

        Map accountMap = FastMap.newInstance();
        accountMap.putAll(account.getAllFields());
        accountMap.putAll(tagMap);

        if (UtilAccounting.isRevenueAccount(account) || UtilAccounting.isIncomeAccount(account)) {
            // no sign change
        } else if (UtilAccounting.isExpenseAccount(account)) {
            sum = sum.negate();
        } else {
            // if for some reason the class was misconfigured (which should be impossible due to lookup conditions), then put this account in unclassified group
            Debug.logWarning("Unable to determine if account [" + account.get("glAccountId") + "] is revenue, income or expense. This account will be put in unclassified group.", MODULE);
            glAccountTypeId = UNCLASSIFIED_TYPE;
            group = glAccountSumsGrouped.get(UNCLASSIFIED_TYPE);
        }
        accountMap.put("accountSum", sum);

        // Now aggregate the amounts we're interested in.  We are doing the sum(glAccountTypeId) here.  The arithmetic on the aggregations is done afterwards.
        if ("REVENUE".equals(glAccountTypeId)) {
            sums.put("grossProfit", sums.get("grossProfit").add(sum));
        } else if ("COGS".equals(glAccountTypeId)) {
            // because this is an expense account, positive COGS expenses should already be a negative value, so we can add this
            sums.put("grossProfit", sums.get("grossProfit").add(sum));
        } else if ("OPERATING_EXPENSE".equals(glAccountTypeId)) {
            // note that sign is already negative due to this being an expense, so we add instead of subtract
            sums.put("operatingIncome", sums.get("operatingIncome").add(sum));
        } else if ("OTHER_EXPENSE".equals(glAccountTypeId)) {
            // note that sign is already negative due to this being an expense, so we add instead of subtract
            sums.put("pretaxIncome", sums.get("pretaxIncome").add(sum));
        } else if ("OTHER_INCOME".equals(glAccountTypeId)) {
            sums.put("pretaxIncome", sums.get("pretaxIncome").add(sum));
        } else if ("TAX_EXPENSE".equals(glAccountTypeId)) {
            // note that sign is already negative due to this being an expense, so we add instead of subtract
            sums.put("netIncome", sums.get("netIncome").add(sum));
        } else if (UNCLASSIFIED_TYPE.equals(glAccountTypeId)) {
            sums.put("netIncome", sums.get("netIncome").add(sum));
        }

        // add this account to the group
        group.add(accountMap);

        // keep a running total for the group
        BigDecimal groupSum = glAccountGroupSums.get(glAccountTypeId);
        glAccountGroupSums.put(glAccountTypeId, groupSum.add(sum));
    }

    @SuppressWarnings("unchecked")
    private static Map makeIncomeStatementResults(Map glAccountSums, Map<String, List<Map>> glAccountSumsGrouped, Map<String, BigDecimal> glAccountGroupSums, Map<String, BigDecimal> sums, String organizationPartyId, Timestamp fromDate, Timestamp thruDate, Delegator delegator) throws GenericEntityException {

        // complete the aggregated sums for operating, pretax and net incomes
        sums.put("operatingIncome", sums.get("operatingIncome").add(sums.get("grossProfit")));
        sums.put("pretaxIncome", sums.get("pretaxIncome").add(sums.get("operatingIncome")));
        sums.put("netIncome", sums.get("netIncome").add(sums.get("pretaxIncome")));

        // was this income statement for periods which are closed?  check by seeing if there are any unclosed time periods
        // in between these dates?  If so, then this accounting period has not been closed
        // TODO: this is not very good.  Implement a real service which checks through all interim periods correctly.
        boolean isClosed = true;
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
              EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
              EntityCondition.makeCondition("isClosed", EntityOperator.NOT_EQUAL, "Y"),
              EntityCondition.makeCondition(EntityOperator.OR,
                      EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate),
                      EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate)));
        List timePeriods = delegator.findByCondition("CustomTimePeriod", conditions, UtilMisc.toList("customTimePeriodId"), UtilMisc.toList("customTimePeriodId"));
        if (timePeriods.size() > 0) {
            isClosed = false;
        }

        // now get the profit/loss GlAccount for the organization and return it as well
        String retainedEarningsGlAccountId = null;
        GenericValue retainedEarningsGlAccount = null;

        // get retained earnings account for the organization
        try {
            retainedEarningsGlAccountId = UtilAccounting.getDefaultAccountId("RETAINED_EARNINGS", organizationPartyId, delegator);
        } catch (AccountingException e) {
            return ServiceUtil.returnError("Cannot find a RETAINED_EARNINGS for organization " + organizationPartyId);
        }
        retainedEarningsGlAccount = delegator.findByPrimaryKeyCache("GlAccount", UtilMisc.toMap("glAccountId", retainedEarningsGlAccountId));

        // all done
        Map result = ServiceUtil.returnSuccess();
        result.putAll(sums);
        result.put("glAccountSums", glAccountSumsGrouped);
        result.put("glAccountGroupSums", glAccountGroupSums);
        result.put("glAccountSumsFlat", glAccountSums);
        result.put("isClosed", new Boolean(isClosed));
        result.put("retainedEarningsGlAccount", retainedEarningsGlAccount);
        return result;
    }

    /**
     * Gets a Map of glAccount -> sum of transactions for all income statement accounts (REVENUE, EXPENSE, INCOME) over a period of dates for an organization.
     * Uses the <code>addToAccountBalances</code> service.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    private static Map getIncomeStatementAccountSumsCommon(DispatchContext dctx, Map context) {
        return getIncomeStatementAccountSumsCommon(dctx, context, null);
    }

    /**
     * Gets a Map of (glAccountId,tag1,tag2,...) -> sum of transactions for all income statement accounts (REVENUE, EXPENSE, INCOME) over a period of dates for an organization.
     * Uses the <code>addToAccountBalancesByTag</code> service.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @param accountingTags the <code>Map</code> of accounting tags to consider when calling the <code>addToAccountBalancesByTag</code> service
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    private static Map getIncomeStatementAccountSumsCommon(DispatchContext dctx, Map context, Map accountingTags) {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            // first find all accounting transaction entries for this organizationPartyId and following into this time period which are
            // income statement transactions TODO can be configurable
            List transactionEntries = null;
            Map input = dctx.getModelService("getAcctgTransAndEntriesByType").makeValid(context, ModelService.IN_PARAM);
            input.put("glAccountClasses", INCOME_STATEMENT_CLASSES);
            Map tmpResult = dispatcher.runSync("getAcctgTransAndEntriesByType", input);
            if ((tmpResult != null) && (tmpResult.get("transactionEntries") != null)) {
                transactionEntries = (List) tmpResult.get("transactionEntries");
            }
            // very important - we do not want the PERIOD_CLOSING transactions as this will duplicate the net income
            transactionEntries = EntityUtil.filterOutByCondition(transactionEntries, EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.EQUALS, "PERIOD_CLOSING"));
            // now add them up by account.  since we may have to deal with flipping the signs of transactions, we'd have analysis type of
            // transaction (debit/credit) vs class of account (debit/credit), so it wasn't possible to just use a view-entity to sum it all up
            Map<String, BigDecimal> glAccountSums = new HashMap<String, BigDecimal>();

            input = UtilMisc.toMap("glAccountSums", glAccountSums, "transactionEntries", transactionEntries, "userLogin", userLogin);
            String serviceName = "addToAccountBalances";
            if (accountingTags != null) {
                input.put("accountingTags", accountingTags);
                serviceName = "addToAccountBalancesByTag";
            }

            tmpResult = dispatcher.runSync(serviceName, input);
            if (tmpResult.get("glAccountSums") == null) {
                return ServiceUtil.returnError("Cannot sum up account balances properly for income statement");
            } else {
                Map result = ServiceUtil.returnSuccess();
                result.put("glAccountSums", glAccountSums);
                return result;
            }
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

    }
}
