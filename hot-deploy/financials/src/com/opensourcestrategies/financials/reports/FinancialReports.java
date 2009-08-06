/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package com.opensourcestrategies.financials.reports;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.opensourcestrategies.financials.financials.FinancialServices;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Events preparing data passed to Jasper reports.
 */
public final class FinancialReports {

    private static final String MODULE = FinancialReports.class.getName();

    private FinancialReports() { }

    /**
     * Prepare data source and parameters for income statement report.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String prepareIncomeStatementReport(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        String reportType = UtilCommon.getParameter(request, "type");
        String glFiscalTypeId = UtilCommon.getParameter(request, "glFiscalTypeId");
        String dateOption = UtilCommon.getParameter(request, "reportDateOption");
        String fromDateText = UtilCommon.getParameter(request, "fromDate");
        String thruDateText = UtilCommon.getParameter(request, "thruDate");
        Timestamp fromDate = null;
        Timestamp thruDate = null;

        try {

            // determine the period
            if (dateOption.equals("byDate")) {
                fromDateText = UtilCommon.getParameter(request, "fromDate");
                thruDateText = UtilCommon.getParameter(request, "thruDate");
                if (UtilValidate.isNotEmpty(fromDateText)) {
                    fromDate = UtilDateTime.getDayStart(UtilDate.toTimestamp(fromDateText, timeZone, locale), timeZone, locale);
                }
                if (UtilValidate.isNotEmpty(thruDateText)) {
                    thruDate = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText, timeZone, locale), timeZone, locale);
                }
            } else if (dateOption.equals("byTimePeriod")) {
                String customTimePeriodId = UtilCommon.getParameter(request, "customTimePeriodId");
                GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
                fromDate = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(((Date) timePeriod.get("fromDate")).getTime()), timeZone, locale);
                thruDate = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) timePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
            }

            Map<String, String> groupMarkers = UtilMisc.toMap(
                    "REVENUE", uiLabelMap.get("FinancialsGrossProfit"), "COGS", uiLabelMap.get("FinancialsGrossProfit"),
                    "OPERATING_EXPENSE", uiLabelMap.get("FinancialsOperatingIncome"),
                    "OTHER_EXPENSE", uiLabelMap.get("FinancialsPretaxIncome"), "OTHER_INCOME", uiLabelMap.get("FinancialsPretaxIncome"),
                    "TAX_EXPENSE", uiLabelMap.get("AccountingNetIncome")
            );

            // validate parameters
            if (fromDate == null || thruDate == null) {
                return UtilMessage.createAndLogEventError(request, "FinancialsError_FromOrThruDateMissing", locale, MODULE);
            }

            // retrieve financial data
            Map<String, Object> ctxt = FastMap.newInstance();
            ctxt.put("userLogin", userLogin);
            ctxt.put("organizationPartyId", organizationPartyId);
            ctxt.put("glFiscalTypeId", glFiscalTypeId);
            ctxt.put("fromDate", fromDate);
            ctxt.put("thruDate", thruDate);
            UtilAccountingTags.addTagParameters(request, ctxt);

            Map<String, ?> results = dispatcher.runSync("getIncomeStatementByDates", ctxt);
            Map<String, ?> glAccountSums = (Map<String, ?>) results.get("glAccountSums");
            BigDecimal grossProfit = (BigDecimal) results.get("grossProfit");
            BigDecimal operatingIncome = (BigDecimal) results.get("operatingIncome");
            BigDecimal pretaxIncome = (BigDecimal) results.get("pretaxIncome");
            BigDecimal netIncome = (BigDecimal) results.get("netIncome");
            Map<String, BigDecimal> glAccountGroupSums = (Map) results.get("glAccountGroupSums");

            List<Map<String, ?>> rows = FastList.newInstance();

            Integer i = 1;
            for (String glAccountTypeId : FinancialServices.INCOME_STATEMENT_TYPES) {
                List<Map<String, ?>> accounts = (List<Map<String, ?>>) glAccountSums.get(glAccountTypeId);

                // find account type
                GenericValue glAccountType = delegator.findByPrimaryKey("GlAccountType", UtilMisc.toMap("glAccountTypeId", glAccountTypeId));

                // create records for report
                if (UtilValidate.isNotEmpty(accounts)) {
                    for (Map<String, ?> account : accounts) {
                        Map<String, Object> row = FastMap.newInstance();
                        row.put("typeSeqNum", i);
                        row.put("accountCode", account.get("accountCode"));
                        row.put("accountName", account.get("accountName"));
                        row.put("glAccountTypeDescription", glAccountType.getString("description"));
                        row.put("glAccountTypeAmount", glAccountGroupSums.get(glAccountTypeId));
                        row.put("accountSum", account.get("accountSum"));
                        row.put("cumulativeIncomeName", groupMarkers.get(glAccountTypeId));
                        if ("REVENUE".equals(glAccountTypeId) || "COGS".equals(glAccountTypeId)) {
                            row.put("cumulativeIncomeAmount", grossProfit);
                        } else if ("OPERATING_EXPENSE".equals(glAccountTypeId)) {
                            row.put("cumulativeIncomeAmount", operatingIncome);
                        } else if ("OTHER_EXPENSE".equals(glAccountTypeId) || "OTHER_INCOME".equals(glAccountTypeId)) {
                            row.put("cumulativeIncomeAmount", pretaxIncome);
                        } else if ("TAX_EXPENSE".equals(glAccountTypeId)) {
                            row.put("cumulativeIncomeAmount", netIncome);
                        } else {
                            row.put("cumulativeIncomeAmount", null);
                        }
                        rows.add(row);
                    }
                } else {
                    // put line w/ empty detail fields to display grouping and totals in any case
                    Map<String, Object> row = FastMap.newInstance();
                    row.put("typeSeqNum", i);
                    row.put("glAccountTypeDescription", glAccountType.getString("description"));
                    row.put("glAccountTypeAmount", glAccountGroupSums.get(glAccountTypeId));
                    row.put("cumulativeIncomeName", groupMarkers.get(glAccountTypeId));
                    if ("REVENUE".equals(glAccountTypeId) || "COGS".equals(glAccountTypeId)) {
                        row.put("cumulativeIncomeAmount", grossProfit);
                    } else if ("OPERATING_EXPENSE".equals(glAccountTypeId)) {
                        row.put("cumulativeIncomeAmount", operatingIncome);
                    } else if ("OTHER_EXPENSE".equals(glAccountTypeId) || "OTHER_INCOME".equals(glAccountTypeId)) {
                        row.put("cumulativeIncomeAmount", pretaxIncome);
                    } else if ("TAX_EXPENSE".equals(glAccountTypeId)) {
                        row.put("cumulativeIncomeAmount", netIncome);
                    } else {
                        row.put("cumulativeIncomeAmount", null);
                    }
                    rows.add(row);
                }
                i++;
            }

            // sort records by account code
            Collections.sort(rows, new Comparator<Map<String, ?>>() {
                public int compare(Map<String, ?> o1, Map<String, ?> o2) {
                    Integer seqNum1 = (Integer) o1.get("typeSeqNum");
                    Integer seqNum2 = (Integer) o2.get("typeSeqNum");
                    int isSeqNumEq = seqNum1.compareTo(seqNum2);
                    if (isSeqNumEq != 0) {
                        return isSeqNumEq;
                    }
                    String accountCode1 = (String) o1.get("accountCode");
                    String accountCode2 = (String) o2.get("accountCode");
                    if (accountCode1 == null && accountCode2 == null) {
                        return 0;
                    }
                    if (accountCode1 == null && accountCode2 != null) {
                        return -1;
                    }
                    if (accountCode1 != null && accountCode2 == null) {
                        return 1;
                    }
                    return accountCode1.compareTo(accountCode2);
                }
            });
            request.setAttribute("jrDataSource", new JRMapCollectionDataSource(rows));

            // prepare report parameters
            Map<String, Object> jrParameters = FastMap.newInstance();
            jrParameters.put("fromDate", fromDate);
            jrParameters.put("thruDate", thruDate);
            jrParameters.put("organizationPartyId", organizationPartyId);
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, organizationPartyId, false));
            jrParameters.put("accountingTags", UtilAccountingTags.formatTagsAsString(request, UtilAccountingTags.FINANCIALS_REPORTS_TAG, delegator));
            request.setAttribute("jrParameters", jrParameters);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return reportType;
    }

    /**
     * Prepare data source and parameters for comparative income statement report.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String prepareComparativeIncomeStatementReport(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        String reportType = UtilCommon.getParameter(request, "type");
        String glFiscalTypeId1 = UtilCommon.getParameter(request, "glFiscalTypeId1");
        String glFiscalTypeId2 = UtilCommon.getParameter(request, "glFiscalTypeId2");
        String fromDateText1 = null;
        String thruDateText1 = null;
        String fromDateText2 = null;
        String thruDateText2 = null;
        Timestamp fromDate1 = null;
        Timestamp thruDate1 = null;
        Timestamp fromDate2 = null;
        Timestamp thruDate2 = null;
        String dateOption = UtilCommon.getParameter(request, "reportDateOption");

        try {

            // determine the period
            if (dateOption.equals("byDate")) {
                fromDateText1 = UtilCommon.getParameter(request, "fromDate1");
                thruDateText1 = UtilCommon.getParameter(request, "thruDate1");
                fromDateText2 = UtilCommon.getParameter(request, "fromDate2");
                thruDateText2 = UtilCommon.getParameter(request, "thruDate2");
                if (UtilValidate.isEmpty(fromDateText1) || UtilValidate.isEmpty(thruDateText1) || UtilValidate.isEmpty(fromDateText2) || UtilValidate.isEmpty(thruDateText2)) {
                    return UtilMessage.createAndLogEventError(request, "FinancialsError_FromOrThruDateMissing", locale, MODULE);
                }
                fromDate1 = UtilDateTime.getDayStart(UtilDate.toTimestamp(fromDateText1, timeZone, locale), timeZone, locale);
                thruDate1 = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText1, timeZone, locale), timeZone, locale);
                fromDate2 = UtilDateTime.getDayStart(UtilDate.toTimestamp(fromDateText2, timeZone, locale), timeZone, locale);
                thruDate2 = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText2, timeZone, locale), timeZone, locale);
            } else if (dateOption.equals("byTimePeriod")) {
                String fromCustomTimePeriodId = UtilCommon.getParameter(request, "fromCustomTimePeriodId");
                String thruCustomTimePeriodId = UtilCommon.getParameter(request, "thruCustomTimePeriodId");
                GenericValue fromTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", fromCustomTimePeriodId));
                GenericValue thruTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", thruCustomTimePeriodId));
                fromDate1 = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(((Date) fromTimePeriod.get("fromDate")).getTime()), timeZone, locale);
                thruDate1 = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) fromTimePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
                fromDate2 = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(((Date) thruTimePeriod.get("fromDate")).getTime()), timeZone, locale);
                thruDate2 = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) thruTimePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
            }

            // validate parameters
            if (fromDate1 == null || thruDate1 == null || fromDate2 == null || thruDate2 == null) {
                return UtilMessage.createAndLogEventError(request, "FinancialsError_DateRangeMissing", locale, MODULE);
            }

            // retrieve financial data
            Map<String, Object> context = FastMap.newInstance();
            context.put("userLogin", userLogin);
            context.put("organizationPartyId", organizationPartyId);
            context.put("glFiscalTypeId1", glFiscalTypeId1);
            context.put("glFiscalTypeId2", glFiscalTypeId2);
            context.put("fromDate1", fromDate1);
            context.put("thruDate1", thruDate1);
            context.put("fromDate2", fromDate2);
            context.put("thruDate2", thruDate2);
            UtilAccountingTags.addTagParameters(request, context);

            Map<String, ?> results = dispatcher.runSync("getComparativeIncomeStatement", context);

            Map<String, ?> set1IncomeStatement = (Map<String, ?>) results.get("set1IncomeStatement");
            Map<String, ?> set2IncomeStatement = (Map<String, ?>) results.get("set2IncomeStatement");
            BigDecimal netIncome1 = (BigDecimal) set1IncomeStatement.get("netIncome");
            BigDecimal netIncome2 = (BigDecimal) set2IncomeStatement.get("netIncome");
            Map<GenericValue, BigDecimal> accountBalances = (Map<GenericValue, BigDecimal>) results.get("accountBalances");
            Map<GenericValue, BigDecimal> set1Accounts = (Map<GenericValue, BigDecimal>) set1IncomeStatement.get("glAccountSumsFlat");
            Map<GenericValue, BigDecimal> set2Accounts = (Map<GenericValue, BigDecimal>) set2IncomeStatement.get("glAccountSumsFlat");
            GenericValue glFiscalType1 = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", glFiscalTypeId1));
            GenericValue glFiscalType2 = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", glFiscalTypeId2));

            List<Map<String, ?>> rows = FastList.newInstance();

            // create records for report
            Set<GenericValue> accounts = accountBalances.keySet();
            for (GenericValue account : accounts) {
                Map<String, Object> row = FastMap.newInstance();
                row.put("accountCode", account.get("accountCode"));
                row.put("accountName", account.get("accountName"));
                row.put("accountSumLeft", set1Accounts.get(account));
                row.put("cumulativeIncomeName", uiLabelMap.get("AccountingNetIncome"));
                row.put("accountSumRight", set2Accounts.get(account));
                row.put("accountSumDiff", accountBalances.get(account));
                rows.add(row);
            }

            // sort records by account code
            Collections.sort(rows, new Comparator<Map<String, ?>>() {
                public int compare(Map<String, ?> o1, Map<String, ?> o2) {
                    String accountCode1 = (String) o1.get("accountCode");
                    String accountCode2 = (String) o2.get("accountCode");
                    if (accountCode1 == null && accountCode2 == null) {
                        return 0;
                    }
                    if (accountCode1 == null && accountCode2 != null) {
                        return -1;
                    }
                    if (accountCode1 != null && accountCode2 == null) {
                        return 1;
                    }
                    return accountCode1.compareTo(accountCode2);
                }
            });
            request.setAttribute("jrDataSource", new JRMapCollectionDataSource(rows));

            // prepare report parameters
            Map<String, Object> jrParameters = FastMap.newInstance();
            jrParameters.put("fromDateLeft", fromDate1);
            jrParameters.put("thruDateLeft", thruDate1);
            jrParameters.put("fromDateRight", fromDate2);
            jrParameters.put("thruDateRight", thruDate2);
            jrParameters.put("netIncomeLeft", netIncome1);
            jrParameters.put("netIncomeRight", netIncome2);
            jrParameters.put("fiscalTypeLeft", glFiscalType1.get("description", locale));
            jrParameters.put("fiscalTypeRight", glFiscalType2.get("description", locale));
            jrParameters.put("organizationPartyId", organizationPartyId);
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, organizationPartyId, false));
            jrParameters.put("accountingTags", UtilAccountingTags.formatTagsAsString(request, UtilAccountingTags.FINANCIALS_REPORTS_TAG, delegator));
            request.setAttribute("jrParameters", jrParameters);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return reportType;
    }

    /**
     * Prepare data source and parameters for trial balance report.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String prepareTrialBalanceReport(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        String reportType = UtilCommon.getParameter(request, "type");
        String glFiscalTypeId = UtilCommon.getParameter(request, "glFiscalTypeId");
        String dateOption = UtilCommon.getParameter(request, "reportDateOption");
        String asOfDateText = UtilCommon.getParameter(request, "asOfDate");

        Timestamp defaultAsOfDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), timeZone, locale);
        Timestamp asOfDate = null;

        try {

            // determine the period
            if (dateOption.equals("byDate")) {
                asOfDate = UtilDateTime.getDayEnd(UtilDate.toTimestamp(asOfDateText, timeZone, locale), timeZone, locale);
            } else if (dateOption.equals("byTimePeriod")) {
                String customTimePeriodId = UtilCommon.getParameter(request, "customTimePeriodId");
                GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
                asOfDate = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) timePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
            }

            // use current date
            if (asOfDate == null) {
                asOfDate = defaultAsOfDate;
            }

            // retrieve financial data
            Map<String, Object> ctxt = FastMap.newInstance();
            ctxt.put("userLogin", userLogin);
            ctxt.put("organizationPartyId", organizationPartyId);
            ctxt.put("glFiscalTypeId", glFiscalTypeId);
            ctxt.put("asOfDate", asOfDate);
            UtilAccountingTags.addTagParameters(request, ctxt);

            Map<String, ?> results = dispatcher.runSync("getTrialBalanceForDate", ctxt);

            List<Map<String, ?>> rows = FastList.newInstance();

            // the account groups that will be included in the report, in the same order
            Map<String, String> accountLabels = new LinkedHashMap<String, String>();
            accountLabels.put("asset", "AccountingAssets");
            accountLabels.put("liability", "AccountingLiabilities");
            accountLabels.put("equity", "AccountingEquities");
            accountLabels.put("revenue", "FinancialsRevenue");
            accountLabels.put("expense", "FinancialsExpense");
            accountLabels.put("income", "FinancialsIncome");
            accountLabels.put("other", "CommonOther");

            for (String pre : accountLabels.keySet()) {
                Object label = uiLabelMap.get(accountLabels.get(pre));
                // for the given group, list the entries ordered by account code
                List<GenericValue> accounts = EntityUtil.orderBy(((Map) results.get(pre + "AccountBalances")).keySet(), UtilMisc.toList("accountCode"));
                if (UtilValidate.isNotEmpty(accounts)) {
                    Map<GenericValue, Double> accountBalances = (Map) results.get(pre + "AccountBalances");
                    Map<GenericValue, Double> accountCredits = (Map) results.get(pre + "AccountCredits");
                    Map<GenericValue, Double> accountDebits = (Map) results.get(pre + "AccountDebits");
                    for (GenericValue account : accounts) {
                        Map<String, Object> row = FastMap.newInstance();
                        row.put("accountCode", account.get("accountCode"));
                        row.put("accountName", account.get("accountName"));
                        row.put("accountBalance", accountBalances.get(account));
                        row.put("accountCredit", accountCredits.get(account));
                        row.put("accountDebit", accountDebits.get(account));
                        row.put("accountType", label);
                        row.put("accountCreditDebitFlag", UtilAccounting.isDebitAccount(account) ? "D" : "C");
                        rows.add(row);
                    }
                } else {
                    // for empty groups
                    rows.add(UtilMisc.toMap("accountType", label));
                }
            }

            request.setAttribute("jrDataSource", new JRMapCollectionDataSource(rows));

            // prepare report parameters
            Map<String, Object> jrParameters = FastMap.newInstance();
            jrParameters.put("asOfDate", asOfDate);
            jrParameters.put("organizationPartyId", organizationPartyId);
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, organizationPartyId, false));
            jrParameters.put("totalDebits", results.get("totalDebits"));
            jrParameters.put("totalCredits", results.get("totalCredits"));
            jrParameters.put("accountingTags", UtilAccountingTags.formatTagsAsString(request, UtilAccountingTags.FINANCIALS_REPORTS_TAG, delegator));
            request.setAttribute("jrParameters", jrParameters);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return reportType;

    }

    /**
     * Prepare data source and parameters for balance sheet report.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String prepareBalanceSheetReport(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        String reportType = UtilCommon.getParameter(request, "type");
        String glFiscalTypeId = UtilCommon.getParameter(request, "glFiscalTypeId");
        String dateOption = UtilCommon.getParameter(request, "reportDateOption");
        String asOfDateText = UtilCommon.getParameter(request, "asOfDate");

        Timestamp defaultAsOfDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), timeZone, locale);
        Timestamp asOfDate = null;

        try {

            // determine the period
            if (dateOption.equals("byDate")) {
                asOfDate = UtilDateTime.getDayEnd(UtilDate.toTimestamp(asOfDateText, timeZone, locale), timeZone, locale);
            } else if (dateOption.equals("byTimePeriod")) {
                String customTimePeriodId = UtilCommon.getParameter(request, "customTimePeriodId");
                GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
                asOfDate = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) timePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
            }

            // use current date
            if (asOfDate == null) {
                asOfDate = defaultAsOfDate;
            }

            // retrieve financial data
            Map<String, Object> ctxt = FastMap.newInstance();
            ctxt.put("userLogin", userLogin);
            ctxt.put("organizationPartyId", organizationPartyId);
            ctxt.put("glFiscalTypeId", glFiscalTypeId);
            ctxt.put("asOfDate", asOfDate);
            UtilAccountingTags.addTagParameters(request, ctxt);

            Map<String, ?> results = dispatcher.runSync("getBalanceSheetForDate", ctxt);
            List<GenericValue> assetAccounts = EntityUtil.orderBy(((Map) results.get("assetAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> assetAccountBalances = (Map) results.get("assetAccountBalances");
            List<GenericValue> liabilityAccounts = EntityUtil.orderBy(((Map) results.get("liabilityAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> liabilityAccountBalances = (Map) results.get("liabilityAccountBalances");
            List<GenericValue> equityAccounts = EntityUtil.orderBy(((Map) results.get("equityAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> equityAccountBalances = (Map) results.get("equityAccountBalances");

            List<Map<String, ?>> rows = FastList.newInstance();

            // create record set for report
            if (UtilValidate.isNotEmpty(assetAccounts)) {
                for (GenericValue account : assetAccounts) {
                    Map<String, Object> row = FastMap.newInstance();
                    row.put("accountCode", account.get("accountCode"));
                    row.put("accountName", account.get("accountName"));
                    row.put("accountBalance", assetAccountBalances.get(account));
                    row.put("accountType", uiLabelMap.get("AccountingAssets"));
                    row.put("accountTypeSeqNum", Integer.valueOf(1));
                    rows.add(row);
                }
            } else {
                rows.add(UtilMisc.toMap("accountType", uiLabelMap.get("AccountingAssets"), "accountTypeSeqNum", Integer.valueOf(1)));
            }

            if (UtilValidate.isNotEmpty(liabilityAccounts)) {
                for (GenericValue account : liabilityAccounts) {
                    Map<String, Object> row = FastMap.newInstance();
                    row.put("accountCode", account.get("accountCode"));
                    row.put("accountName", account.get("accountName"));
                    row.put("accountBalance", liabilityAccountBalances.get(account));
                    row.put("accountType", uiLabelMap.get("AccountingLiabilities"));
                    row.put("accountTypeSeqNum", Integer.valueOf(2));
                    rows.add(row);
                }
            } else {
                rows.add(UtilMisc.toMap("accountType", uiLabelMap.get("AccountingLiabilities"), "accountTypeSeqNum", Integer.valueOf(2)));
            }

            if (UtilValidate.isNotEmpty(equityAccounts)) {
                for (GenericValue account : equityAccounts) {
                    Map<String, Object> row = FastMap.newInstance();
                    row.put("accountCode", account.get("accountCode"));
                    row.put("accountName", account.get("accountName"));
                    row.put("accountBalance", equityAccountBalances.get(account));
                    row.put("accountType", uiLabelMap.get("AccountingEquities"));
                    row.put("accountTypeSeqNum", Integer.valueOf(3));
                    rows.add(row);
                }
            } else {
                rows.add(UtilMisc.toMap("accountType", uiLabelMap.get("AccountingEquities"), "accountTypeSeqNum", Integer.valueOf(3)));
            }


            // sort records by account code
            Collections.sort(rows, new Comparator<Map<String, ?>>() {
                public int compare(Map<String, ?> o1, Map<String, ?> o2) {
                    Integer accountTypeSeqNum1 = (Integer) o1.get("accountTypeSeqNum");
                    Integer accountTypeSeqNum2 = (Integer) o2.get("accountTypeSeqNum");
                    int c = accountTypeSeqNum1.compareTo(accountTypeSeqNum2);
                    if (c == 0) {
                        String accountCode1 = (String) o1.get("accountCode");
                        String accountCode2 = (String) o2.get("accountCode");
                        if (accountCode1 == null && accountCode2 == null) {
                            return 0;
                        }
                        if (accountCode1 == null && accountCode2 != null) {
                            return -1;
                        }
                        if (accountCode1 != null && accountCode2 == null) {
                            return 1;
                        }
                        return accountCode1.compareTo(accountCode2);
                    }
                    return c;
                }
            });
            request.setAttribute("jrDataSource", new JRMapCollectionDataSource(rows));

            // prepare report parameters
            Map<String, Object> jrParameters = FastMap.newInstance();
            jrParameters.put("asOfDate", asOfDate);
            jrParameters.put("organizationPartyId", organizationPartyId);
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, organizationPartyId, false));
            jrParameters.put("accountingTags", UtilAccountingTags.formatTagsAsString(request, UtilAccountingTags.FINANCIALS_REPORTS_TAG, delegator));
            request.setAttribute("jrParameters", jrParameters);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return reportType;
    }

    /**
     * Prepare data source and parameters for comparative balance sheet report.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String prepareComparativeBalanceReport(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        String reportType = UtilCommon.getParameter(request, "type");
        String fromGlFiscalTypeId = UtilCommon.getParameter(request, "fromGlFiscalTypeId");
        String toGlFiscalTypeId = UtilCommon.getParameter(request, "toGlFiscalTypeId");
        String fromDateText = null;
        String thruDateText = null;
        Timestamp fromDate = null;
        Timestamp thruDate = null;
        String dateOption = UtilCommon.getParameter(request, "reportDateOption");

        try {

            // determine the period
            if (dateOption.equals("byDate")) {
                fromDateText = UtilCommon.getParameter(request, "fromDate");
                thruDateText = UtilCommon.getParameter(request, "thruDate");
                fromDate = UtilDateTime.getDayEnd(UtilDate.toTimestamp(fromDateText, timeZone, locale), timeZone, locale);
                thruDate = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText, timeZone, locale), timeZone, locale);
            } else if (dateOption.equals("byTimePeriod")) {
                String fromCustomTimePeriodId = UtilCommon.getParameter(request, "fromCustomTimePeriodId");
                String thruCustomTimePeriodId = UtilCommon.getParameter(request, "thruCustomTimePeriodId");
                GenericValue fromTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", fromCustomTimePeriodId));
                GenericValue thruTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", thruCustomTimePeriodId));
                //  the time periods end at the beginning of the thruDate,  end we want to adjust it so that the report ends at the end of day before the thruDate
                fromDate = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) fromTimePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
                thruDate = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) thruTimePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
            }

            // validate parameters
            if (fromDate == null || thruDate == null) {
                return UtilMessage.createAndLogEventError(request, "FinancialsError_FromOrThruDateMissing", locale, MODULE);
            }

            // retrieve financial data
            Map<String, Object> context = FastMap.newInstance();
            context.put("userLogin", userLogin);
            context.put("organizationPartyId", organizationPartyId);
            context.put("fromGlFiscalTypeId", fromGlFiscalTypeId);
            context.put("toGlFiscalTypeId", toGlFiscalTypeId);
            context.put("fromDate", fromDate);
            context.put("thruDate", thruDate);
            UtilAccountingTags.addTagParameters(request, context);

            Map<String, ?> results = dispatcher.runSync("getComparativeBalanceSheet", context);

            List<GenericValue> assetAccounts = EntityUtil.orderBy(((Map<GenericValue, BigDecimal>) results.get("assetAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> assetAccountBalances = (Map<GenericValue, BigDecimal>) results.get("assetAccountBalances");
            List<GenericValue> liabilityAccounts = EntityUtil.orderBy(((Map<GenericValue, BigDecimal>) results.get("liabilityAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> liabilityAccountBalances = (Map<GenericValue, BigDecimal>) results.get("liabilityAccountBalances");
            List<GenericValue> equityAccounts = EntityUtil.orderBy(((Map<GenericValue, BigDecimal>) results.get("equityAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> equityAccountBalances = (Map<GenericValue, BigDecimal>) results.get("equityAccountBalances");

            Map<String, ?> fromDateAccountBalances = (Map<String, ?>) results.get("fromDateAccountBalances");
            Map<GenericValue, BigDecimal> fromAssetAccountBalances = (Map<GenericValue, BigDecimal>) fromDateAccountBalances.get("assetAccountBalances");
            Map<GenericValue, BigDecimal> fromLiabilityAccountBalances = (Map<GenericValue, BigDecimal>) fromDateAccountBalances.get("liabilityAccountBalances");
            Map<GenericValue, BigDecimal> fromEquityAccountBalances = (Map<GenericValue, BigDecimal>) fromDateAccountBalances.get("equityAccountBalances");

            Map<String, ?> thruDateAccountBalances = (Map<String, ?>) results.get("thruDateAccountBalances");
            Map<GenericValue, BigDecimal> toAssetAccountBalances = (Map<GenericValue, BigDecimal>) thruDateAccountBalances.get("assetAccountBalances");
            Map<GenericValue, BigDecimal> toLiabilityAccountBalances = (Map<GenericValue, BigDecimal>) thruDateAccountBalances.get("liabilityAccountBalances");
            Map<GenericValue, BigDecimal> toEquityAccountBalances = (Map<GenericValue, BigDecimal>) thruDateAccountBalances.get("equityAccountBalances");

            GenericValue fromGlFiscalType = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", fromGlFiscalTypeId));
            GenericValue toGlFiscalType = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", toGlFiscalTypeId));

            List<Map<String, ?>> rows = FastList.newInstance();

            // create record set for report
            if (UtilValidate.isNotEmpty(assetAccounts)) {
                for (GenericValue account : assetAccounts) {
                    Map<String, Object> row = FastMap.newInstance();
                    row.put("accountCode", account.get("accountCode"));
                    row.put("accountName", account.get("accountName"));
                    row.put("accountBalance", assetAccountBalances.get(account));
                    row.put("accountBalanceLeft", fromAssetAccountBalances.get(account));
                    row.put("accountBalanceRight", toAssetAccountBalances.get(account));
                    row.put("accountType", uiLabelMap.get("AccountingAssets"));
                    row.put("accountTypeSeqNum", Integer.valueOf(1));
                    rows.add(row);
                }
            } else {
                rows.add(UtilMisc.toMap("accountType", uiLabelMap.get("AccountingAssets"), "accountTypeSeqNum", Integer.valueOf(1)));
            }

            if (UtilValidate.isNotEmpty(liabilityAccounts)) {
                for (GenericValue account : liabilityAccounts) {
                    Map<String, Object> row = FastMap.newInstance();
                    row.put("accountCode", account.get("accountCode"));
                    row.put("accountName", account.get("accountName"));
                    row.put("accountBalance", liabilityAccountBalances.get(account));
                    row.put("accountBalanceLeft", fromLiabilityAccountBalances.get(account));
                    row.put("accountBalanceRight", toLiabilityAccountBalances.get(account));
                    row.put("accountType", uiLabelMap.get("AccountingLiabilities"));
                    row.put("accountTypeSeqNum", Integer.valueOf(2));
                    rows.add(row);
                }
            } else {
                rows.add(UtilMisc.toMap("accountType", uiLabelMap.get("AccountingLiabilities"), "accountTypeSeqNum", Integer.valueOf(2)));
            }

            if (UtilValidate.isNotEmpty(equityAccounts)) {
                for (GenericValue account : equityAccounts) {
                    Map<String, Object> row = FastMap.newInstance();
                    row.put("accountCode", account.get("accountCode"));
                    row.put("accountName", account.get("accountName"));
                    row.put("accountBalance", equityAccountBalances.get(account));
                    row.put("accountBalanceLeft", fromEquityAccountBalances.get(account));
                    row.put("accountBalanceRight", toEquityAccountBalances.get(account));
                    row.put("accountType", uiLabelMap.get("AccountingEquities"));
                    row.put("accountTypeSeqNum", Integer.valueOf(3));
                    rows.add(row);
                }
            } else {
                rows.add(UtilMisc.toMap("accountType", uiLabelMap.get("AccountingEquities"), "accountTypeSeqNum", Integer.valueOf(3)));
            }

            // sort records by account code
            Collections.sort(rows, new Comparator<Map<String, ?>>() {
                public int compare(Map<String, ?> o1, Map<String, ?> o2) {
                    Integer accountTypeSeqNum1 = (Integer) o1.get("accountTypeSeqNum");
                    Integer accountTypeSeqNum2 = (Integer) o2.get("accountTypeSeqNum");
                    int c = accountTypeSeqNum1.compareTo(accountTypeSeqNum2);
                    if (c == 0) {
                        String accountCode1 = (String) o1.get("accountCode");
                        String accountCode2 = (String) o2.get("accountCode");
                        if (accountCode1 == null && accountCode2 == null) {
                            return 0;
                        }
                        if (accountCode1 == null && accountCode2 != null) {
                            return -1;
                        }
                        if (accountCode1 != null && accountCode2 == null) {
                            return 1;
                        }
                        return accountCode1.compareTo(accountCode2);
                    }
                    return c;
                }
            });
            request.setAttribute("jrDataSource", new JRMapCollectionDataSource(rows));

            // prepare report parameters
            Map<String, Object> jrParameters = FastMap.newInstance();
            jrParameters.put("fromDate", fromDate);
            jrParameters.put("thruDate", thruDate);
            jrParameters.put("fiscalTypeLeft", fromGlFiscalType.get("description", locale));
            jrParameters.put("fiscalTypeRight", toGlFiscalType.get("description", locale));
            jrParameters.put("organizationPartyId", organizationPartyId);
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, organizationPartyId, false));
            jrParameters.put("accountingTags", UtilAccountingTags.formatTagsAsString(request, UtilAccountingTags.FINANCIALS_REPORTS_TAG, delegator));
            request.setAttribute("jrParameters", jrParameters);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return reportType;
    }

    /**
     * Prepare data source and parameters for cash flow report.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String prepareCashFlowStatementReport(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        String reportType = UtilCommon.getParameter(request, "type");
        String glFiscalTypeId = UtilCommon.getParameter(request, "glFiscalTypeId");
        String dateOption = UtilCommon.getParameter(request, "reportDateOption");
        String fromDateText = UtilCommon.getParameter(request, "fromDate");
        String thruDateText = UtilCommon.getParameter(request, "thruDate");
        Timestamp fromDate = null;
        Timestamp thruDate = null;

        try {

            // determine the period
            if (dateOption.equals("byDate")) {
                fromDateText = UtilCommon.getParameter(request, "fromDate");
                thruDateText = UtilCommon.getParameter(request, "thruDate");
                if (UtilValidate.isNotEmpty(fromDateText)) {
                    fromDate = UtilDateTime.getDayStart(UtilDate.toTimestamp(fromDateText, timeZone, locale), timeZone, locale);
                }
                if (UtilValidate.isNotEmpty(thruDateText)) {
                    thruDate = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText, timeZone, locale), timeZone, locale);
                }
            } else if (dateOption.equals("byTimePeriod")) {
                String customTimePeriodId = UtilCommon.getParameter(request, "customTimePeriodId");
                GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
                fromDate = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(((Date) timePeriod.get("fromDate")).getTime()), timeZone, locale);
                thruDate = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) timePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
            }

            // validate parameters
            if (fromDate == null || thruDate == null) {
                return UtilMessage.createAndLogEventError(request, "FinancialsError_FromOrThruDateMissing", locale, MODULE);
            }

            // retrieve financial data
            Map<String, Object> ctxt = FastMap.newInstance();
            ctxt.put("userLogin", userLogin);
            ctxt.put("organizationPartyId", organizationPartyId);
            ctxt.put("glFiscalTypeId", glFiscalTypeId);
            ctxt.put("fromDate", fromDate);
            ctxt.put("thruDate", thruDate);
            UtilAccountingTags.addTagParameters(request, ctxt);

            Map<String, ?> results = dispatcher.runSync("getCashFlowStatementForDates", ctxt);
            BigDecimal beginningCashAmount = (BigDecimal) results.get("beginningCashAmount");
            BigDecimal endingCashAmount = (BigDecimal) results.get("endingCashAmount");
            BigDecimal operatingCashFlow = (BigDecimal) results.get("operatingCashFlow");
            BigDecimal investingCashFlow = (BigDecimal) results.get("investingCashFlow");
            BigDecimal financingCashFlow = (BigDecimal) results.get("financingCashFlow");
            BigDecimal netCashFlow = (BigDecimal) results.get("netCashFlow");
            BigDecimal netIncome = (BigDecimal) results.get("netIncome");
            Map<GenericValue, BigDecimal> operatingCashFlowAccountBalances = (Map<GenericValue, BigDecimal>) results.get("operatingCashFlowAccountBalances");
            Map<GenericValue, BigDecimal> investingCashFlowAccountBalances = (Map<GenericValue, BigDecimal>) results.get("investingCashFlowAccountBalances");
            Map<GenericValue, BigDecimal> financingCashFlowAccountBalances = (Map<GenericValue, BigDecimal>) results.get("financingCashFlowAccountBalances");

            List<GenericValue> operatingAccounts = EntityUtil.orderBy(operatingCashFlowAccountBalances.keySet(), UtilMisc.toList("glAccountId"));
            List<GenericValue> investingAccounts = EntityUtil.orderBy(investingCashFlowAccountBalances.keySet(), UtilMisc.toList("glAccountId"));
            List<GenericValue> financingAccounts = EntityUtil.orderBy(financingCashFlowAccountBalances.keySet(), UtilMisc.toList("glAccountId"));

            List<Map<String, ?>> rows = FastList.newInstance();

            // create record set for report
            for (GenericValue account : operatingAccounts) {
                Map<String, Object> row = FastMap.newInstance();
                row.put("accountCode", account.get("accountCode"));
                row.put("accountName", account.get("accountName"));
                row.put("accountSum", operatingCashFlowAccountBalances.get(account));
                row.put("cashFlowType", uiLabelMap.get("FinancialsOperatingCashFlowAccounts"));
                row.put("cashFlowTypeTotal", uiLabelMap.get("FinancialsTotalOperatingCashFlow"));
                row.put("cashFlowTypeTotalAmount", operatingCashFlow);
                row.put("netCashFlow", uiLabelMap.get("FinancialsTotalNetCashFlow"));
                row.put("endingCashFlow", uiLabelMap.get("FinancialsEndingCashBalance"));
                rows.add(row);
            }

            for (GenericValue account : investingAccounts) {
                Map<String, Object> row = FastMap.newInstance();
                row.put("accountCode", account.get("accountCode"));
                row.put("accountName", account.get("accountName"));
                row.put("accountSum", investingCashFlowAccountBalances.get(account));
                row.put("cashFlowType", uiLabelMap.get("FinancialsInvestingCashFlowAccounts"));
                row.put("cashFlowTypeTotal", uiLabelMap.get("FinancialsTotalInvestingCashFlow"));
                row.put("cashFlowTypeTotalAmount", investingCashFlow);
                row.put("netCashFlow", uiLabelMap.get("FinancialsTotalNetCashFlow"));
                row.put("endingCashFlow", uiLabelMap.get("FinancialsEndingCashBalance"));
                rows.add(row);
            }

            for (GenericValue account : financingAccounts) {
                Map<String, Object> row = FastMap.newInstance();
                row.put("accountCode", account.get("accountCode"));
                row.put("accountName", account.get("accountName"));
                row.put("accountSum", financingCashFlowAccountBalances.get(account));
                row.put("cashFlowType", uiLabelMap.get("FinancialsFinancingCashFlowAccounts"));
                row.put("cashFlowTypeTotal", uiLabelMap.get("FinancialsTotalFinancingCashFlow"));
                row.put("cashFlowTypeTotalAmount", financingCashFlow);
                row.put("netCashFlow", uiLabelMap.get("FinancialsTotalNetCashFlow"));
                row.put("endingCashFlow", uiLabelMap.get("FinancialsEndingCashBalance"));
                rows.add(row);
            }

            request.setAttribute("jrDataSource", new JRMapCollectionDataSource(rows));

            // prepare report parameters
            Map<String, Object> jrParameters = FastMap.newInstance();
            jrParameters.put("fromDate", fromDate);
            jrParameters.put("thruDate", thruDate);
            jrParameters.put("organizationPartyId", organizationPartyId);
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, organizationPartyId, false));
            jrParameters.put("beginningCashAmount", beginningCashAmount);
            jrParameters.put("endingCashAmount", endingCashAmount);
            jrParameters.put("netCashFlow", netCashFlow);
            jrParameters.put("netIncome", netIncome);
            jrParameters.put("accountingTags", UtilAccountingTags.formatTagsAsString(request, UtilAccountingTags.FINANCIALS_REPORTS_TAG, delegator));
            request.setAttribute("jrParameters", jrParameters);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return reportType;
    }

    /**
     * Prepare data source and parameters for comparative cash flow report.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String prepareComparativeCashFlowStatementReport(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        String reportType = UtilCommon.getParameter(request, "type");
        String glFiscalTypeId1 = UtilCommon.getParameter(request, "glFiscalTypeId1");
        String glFiscalTypeId2 = UtilCommon.getParameter(request, "glFiscalTypeId2");
        String fromDateText1 = null;
        String thruDateText1 = null;
        String fromDateText2 = null;
        String thruDateText2 = null;
        Timestamp fromDate1 = null;
        Timestamp thruDate1 = null;
        Timestamp fromDate2 = null;
        Timestamp thruDate2 = null;
        String dateOption = UtilCommon.getParameter(request, "reportDateOption");

        try {

            // determine the period
            if (dateOption.equals("byDate")) {
                fromDateText1 = UtilCommon.getParameter(request, "fromDate1");
                thruDateText1 = UtilCommon.getParameter(request, "thruDate1");
                fromDateText2 = UtilCommon.getParameter(request, "fromDate2");
                thruDateText2 = UtilCommon.getParameter(request, "thruDate2");
                if (UtilValidate.isEmpty(fromDateText1) || UtilValidate.isEmpty(thruDateText1) || UtilValidate.isEmpty(fromDateText2) || UtilValidate.isEmpty(thruDateText2)) {
                    return UtilMessage.createAndLogEventError(request, "FinancialsError_DateRangeMissing", locale, MODULE);
                }
                fromDate1 = UtilDateTime.getDayStart(UtilDate.toTimestamp(fromDateText1, timeZone, locale), timeZone, locale);
                thruDate1 = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText1, timeZone, locale), timeZone, locale);
                fromDate2 = UtilDateTime.getDayStart(UtilDate.toTimestamp(fromDateText2, timeZone, locale), timeZone, locale);
                thruDate2 = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText2, timeZone, locale), timeZone, locale);
            } else if (dateOption.equals("byTimePeriod")) {
                String fromCustomTimePeriodId = UtilCommon.getParameter(request, "fromCustomTimePeriodId");
                String thruCustomTimePeriodId = UtilCommon.getParameter(request, "thruCustomTimePeriodId");
                GenericValue fromTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", fromCustomTimePeriodId));
                GenericValue thruTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", thruCustomTimePeriodId));
                fromDate1 = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(((Date) fromTimePeriod.get("fromDate")).getTime()), timeZone, locale);
                thruDate1 = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) fromTimePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
                fromDate2 = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(((Date) thruTimePeriod.get("fromDate")).getTime()), timeZone, locale);
                thruDate2 = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) thruTimePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
            }

            // validate parameters
            if (fromDate1 == null || thruDate1 == null || fromDate2 == null || thruDate2 == null) {
                return UtilMessage.createAndLogEventError(request, "FinancialsError_DateRangeMissing", locale, MODULE);
            }

            // retrieve financial data
            Map<String, Object> ctxt = FastMap.newInstance();
            ctxt.put("userLogin", userLogin);
            ctxt.put("organizationPartyId", organizationPartyId);
            ctxt.put("glFiscalTypeId1", glFiscalTypeId1);
            ctxt.put("glFiscalTypeId2", glFiscalTypeId2);
            ctxt.put("fromDate1", fromDate1);
            ctxt.put("thruDate1", thruDate1);
            ctxt.put("fromDate2", fromDate2);
            ctxt.put("thruDate2", thruDate2);
            UtilAccountingTags.addTagParameters(request, ctxt);

            Map<String, ?> results = dispatcher.runSync("getComparativeCashFlowStatement", ctxt);

            Map<String, ?> set1CashFlowStatement = (Map<String, ?>) results.get("set1CashFlowStatement");
            Map<String, ?> set2CashFlowStatement = (Map<String, ?>) results.get("set2CashFlowStatement");

            BigDecimal beginningCashAmount1 = (BigDecimal) set1CashFlowStatement.get("beginningCashAmount");
            BigDecimal beginningCashAmount2 = (BigDecimal) set2CashFlowStatement.get("beginningCashAmount");
            BigDecimal endingCashAmount1 = (BigDecimal) set1CashFlowStatement.get("endingCashAmount");
            BigDecimal endingCashAmount2 = (BigDecimal) set2CashFlowStatement.get("endingCashAmount");
            BigDecimal operatingCashFlow1 = (BigDecimal) set1CashFlowStatement.get("operatingCashFlow");
            BigDecimal operatingCashFlow2 = (BigDecimal) set2CashFlowStatement.get("operatingCashFlow");
            BigDecimal investingCashFlow1 = (BigDecimal) set1CashFlowStatement.get("investingCashFlow");
            BigDecimal investingCashFlow2 = (BigDecimal) set2CashFlowStatement.get("investingCashFlow");
            BigDecimal financingCashFlow1 = (BigDecimal) set1CashFlowStatement.get("financingCashFlow");
            BigDecimal financingCashFlow2 = (BigDecimal) set2CashFlowStatement.get("financingCashFlow");
            BigDecimal netCashFlow1 = (BigDecimal) set1CashFlowStatement.get("netCashFlow");
            BigDecimal netCashFlow2 = (BigDecimal) set2CashFlowStatement.get("netCashFlow");
            BigDecimal netIncome1 = (BigDecimal) set1CashFlowStatement.get("netIncome");
            BigDecimal netIncome2 = (BigDecimal) set2CashFlowStatement.get("netIncome");

            Map<GenericValue, BigDecimal> operatingCashFlowAccounts1 = (Map<GenericValue, BigDecimal>) set1CashFlowStatement.get("operatingCashFlowAccountBalances");
            Map<GenericValue, BigDecimal> investingCashFlowAccounts1 = (Map<GenericValue, BigDecimal>) set1CashFlowStatement.get("investingCashFlowAccountBalances");
            Map<GenericValue, BigDecimal> financingCashFlowAccounts1 = (Map<GenericValue, BigDecimal>) set1CashFlowStatement.get("financingCashFlowAccountBalances");
            Map<GenericValue, BigDecimal> operatingCashFlowAccounts2 = (Map<GenericValue, BigDecimal>) set2CashFlowStatement.get("operatingCashFlowAccountBalances");
            Map<GenericValue, BigDecimal> investingCashFlowAccounts2 = (Map<GenericValue, BigDecimal>) set2CashFlowStatement.get("investingCashFlowAccountBalances");
            Map<GenericValue, BigDecimal> financingCashFlowAccounts2 = (Map<GenericValue, BigDecimal>) set2CashFlowStatement.get("financingCashFlowAccountBalances");

            Map<GenericValue, BigDecimal> operatingCashFlowAccountBalances = (Map<GenericValue, BigDecimal>) results.get("operatingCashFlowAccountBalances");
            Map<GenericValue, BigDecimal> investingCashFlowAccountBalances = (Map<GenericValue, BigDecimal>) results.get("investingCashFlowAccountBalances");
            Map<GenericValue, BigDecimal> financingCashFlowAccountBalances = (Map<GenericValue, BigDecimal>) results.get("financingCashFlowAccountBalances");

            List<GenericValue> operatingAccounts = EntityUtil.orderBy(operatingCashFlowAccountBalances.keySet(), UtilMisc.toList("glAccountId"));
            List<GenericValue> investingAccounts = EntityUtil.orderBy(investingCashFlowAccountBalances.keySet(), UtilMisc.toList("glAccountId"));
            List<GenericValue> financingAccounts = EntityUtil.orderBy(financingCashFlowAccountBalances.keySet(), UtilMisc.toList("glAccountId"));

            List<Map<String, ?>> rows = FastList.newInstance();

            // create record set for report
            for (GenericValue account : operatingAccounts) {
                Map<String, Object> row = FastMap.newInstance();
                row.put("accountCode", account.get("accountCode"));
                row.put("accountName", account.get("accountName"));
                row.put("accountSumLeft", operatingCashFlowAccounts1.get(account));
                row.put("accountSumRight", operatingCashFlowAccounts2.get(account));
                row.put("accountSumDiff", operatingCashFlowAccountBalances.get(account));
                row.put("cashFlowType", uiLabelMap.get("FinancialsOperatingCashFlowAccounts"));
                row.put("cashFlowTypeTotal", uiLabelMap.get("FinancialsTotalOperatingCashFlow"));
                row.put("cashFlowTypeTotalAmountLeft", operatingCashFlow1);
                row.put("cashFlowTypeTotalAmountRight", operatingCashFlow2);
                row.put("netCashFlow", uiLabelMap.get("FinancialsTotalNetCashFlow"));
                row.put("endingCashFlow", uiLabelMap.get("FinancialsEndingCashBalance"));
                rows.add(row);
            }

            for (GenericValue account : investingAccounts) {
                Map<String, Object> row = FastMap.newInstance();
                row.put("accountCode", account.get("accountCode"));
                row.put("accountName", account.get("accountName"));
                row.put("accountSumLeft", investingCashFlowAccounts1.get(account));
                row.put("accountSumRight", investingCashFlowAccounts2.get(account));
                row.put("accountSumDiff", investingCashFlowAccountBalances.get(account));
                row.put("cashFlowType", uiLabelMap.get("FinancialsInvestingCashFlowAccounts"));
                row.put("cashFlowTypeTotal", uiLabelMap.get("FinancialsTotalInvestingCashFlow"));
                row.put("cashFlowTypeTotalAmountLeft", investingCashFlow1);
                row.put("cashFlowTypeTotalAmountRight", investingCashFlow2);
                row.put("netCashFlow", uiLabelMap.get("FinancialsTotalNetCashFlow"));
                row.put("endingCashFlow", uiLabelMap.get("FinancialsEndingCashBalance"));
                rows.add(row);
            }

            for (GenericValue account : financingAccounts) {
                Map<String, Object> row = FastMap.newInstance();
                row.put("accountCode", account.get("accountCode"));
                row.put("accountName", account.get("accountName"));
                row.put("accountSumLeft", financingCashFlowAccounts1.get(account));
                row.put("accountSumRight", financingCashFlowAccounts2.get(account));
                row.put("accountSumDiff", financingCashFlowAccountBalances.get(account));
                row.put("cashFlowType", uiLabelMap.get("FinancialsFinancingCashFlowAccounts"));
                row.put("cashFlowTypeTotal", uiLabelMap.get("FinancialsTotalFinancingCashFlow"));
                row.put("cashFlowTypeTotalAmountLeft", financingCashFlow1);
                row.put("cashFlowTypeTotalAmountRight", financingCashFlow2);
                row.put("netCashFlow", uiLabelMap.get("FinancialsTotalNetCashFlow"));
                row.put("endingCashFlow", uiLabelMap.get("FinancialsEndingCashBalance"));
                rows.add(row);
            }

            // sort records by account code
            Collections.sort(rows, new Comparator<Map<String, ?>>() {
                public int compare(Map<String, ?> o1, Map<String, ?> o2) {
                    String accountCode1 = (String) o1.get("accountCode");
                    String accountCode2 = (String) o2.get("accountCode");
                    if (accountCode1 == null && accountCode2 == null) {
                        return 0;
                    }
                    if (accountCode1 == null && accountCode2 != null) {
                        return -1;
                    }
                    if (accountCode1 != null && accountCode2 == null) {
                        return 1;
                    }
                    return accountCode1.compareTo(accountCode2);
                }
            });
            request.setAttribute("jrDataSource", new JRMapCollectionDataSource(rows));

            GenericValue glFiscalType1 = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", glFiscalTypeId1));
            GenericValue glFiscalType2 = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", glFiscalTypeId2));

            // prepare report parameters
            Map<String, Object> jrParameters = FastMap.newInstance();
            jrParameters.put("fromDateLeft", fromDate1);
            jrParameters.put("thruDateLeft", thruDate1);
            jrParameters.put("fromDateRight", fromDate2);
            jrParameters.put("thruDateRight", thruDate2);
            jrParameters.put("organizationPartyId", organizationPartyId);
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, organizationPartyId, false));
            jrParameters.put("fiscalTypeLeft", glFiscalType1.get("description", locale));
            jrParameters.put("fiscalTypeRight", glFiscalType2.get("description", locale));
            jrParameters.put("beginningCashAmountLeft", beginningCashAmount1);
            jrParameters.put("beginningCashAmountRight", beginningCashAmount2);
            jrParameters.put("endingCashAmountLeft", endingCashAmount1);
            jrParameters.put("endingCashAmountRight", endingCashAmount2);
            jrParameters.put("netCashFlowLeft", netCashFlow1);
            jrParameters.put("netCashFlowRight", netCashFlow2);
            jrParameters.put("netIncomeLeft", netIncome1);
            jrParameters.put("netIncomeRight", netIncome2);
            jrParameters.put("accountingTags", UtilAccountingTags.formatTagsAsString(request, UtilAccountingTags.FINANCIALS_REPORTS_TAG, delegator));
            request.setAttribute("jrParameters", jrParameters);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return reportType;
    }
}
