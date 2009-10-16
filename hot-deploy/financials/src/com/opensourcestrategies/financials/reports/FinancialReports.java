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
import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.opensourcestrategies.financials.financials.FinancialServices;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAlias;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAliasField;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.jndi.DataSourceImpl;
import org.opentaps.common.reporting.etl.UtilEtl;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.foundation.repository.RepositoryException;
import org.pentaho.di.core.exception.KettleException;

/**
 * Events preparing data passed to Jasper reports.
 */
public final class FinancialReports {

    private static final String MODULE = FinancialReports.class.getName();

    private FinancialReports() { }

    /**
     * Prepare query parameters for standard financial report.
     * @param request a <code>HttpServletRequest</code> value
     * @return query parameter map
     * @throws GenericEntityException if error occur
     */
    @SuppressWarnings("unchecked")
    public static Map prepareFinancialReportParameters(HttpServletRequest request) throws GenericEntityException {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilCommon.getTimeZone(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        String glFiscalTypeId = UtilCommon.getParameter(request, "glFiscalTypeId");
        String dateOption = UtilCommon.getParameter(request, "reportDateOption");
        String fromDateText = UtilCommon.getParameter(request, "fromDate");
        String thruDateText = UtilCommon.getParameter(request, "thruDate");
        String reportFormType = UtilCommon.getParameter(request, "reportFormType");
        Timestamp fromDate = null;
        Timestamp thruDate = null;

        Timestamp defaultAsOfDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), timeZone, locale);
        Timestamp asOfDate = null;
        Map<String, Object> ctxt = FastMap.newInstance();
        ctxt.put("userLogin", userLogin);
        ctxt.put("organizationPartyId", organizationPartyId);
        ctxt.put("glFiscalTypeId", glFiscalTypeId);

        // determine the period
        if (dateOption == null) {
            // use the default asOfDate and run the report
            ctxt.put("asOfDate", defaultAsOfDate);
        } else if ("byDate".equals(dateOption)) {
            if ("state".equals(reportFormType)) {
                String asOfDateText = UtilCommon.getParameter(request, "asOfDate");
                asOfDate = UtilDateTime.getDayEnd(UtilDate.toTimestamp(asOfDateText, timeZone, locale), timeZone, locale);
                // use current date
                if (asOfDate == null) {
                    asOfDate = defaultAsOfDate;
                }
                ctxt.put("asOfDate", asOfDate);
            } else if ("flow".equals(reportFormType)) {
                fromDateText = UtilCommon.getParameter(request, "fromDate");
                thruDateText = UtilCommon.getParameter(request, "thruDate");
                if (UtilValidate.isNotEmpty(fromDateText)) {
                    fromDate = UtilDateTime.getDayStart(UtilDate.toTimestamp(fromDateText, timeZone, locale), timeZone, locale);
                }
                if (UtilValidate.isNotEmpty(thruDateText)) {
                    thruDate = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText, timeZone, locale), timeZone, locale);
                }
                ctxt.put("fromDate", fromDate);
                ctxt.put("thruDate", thruDate);
            }
        } else if ("byTimePeriod".equals(dateOption)) {
            if ("state".equals(reportFormType) || "flow".equals(reportFormType)) {
                String customTimePeriodId = UtilCommon.getParameter(request, "customTimePeriodId");
                ctxt.put("customTimePeriodId", customTimePeriodId);
                GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
                fromDate = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(((Date) timePeriod.get("fromDate")).getTime()), timeZone, locale);
                thruDate = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) timePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
                ctxt.put("fromDate", fromDate);
                ctxt.put("thruDate", thruDate);
                asOfDate = thruDate;
                ctxt.put("asOfDate", asOfDate);
            }
        }

        return ctxt;
    }

    /**
     * Prepare query parameters for comparative flow report.
     * @param request a <code>HttpServletRequest</code> value
     * @return query parameter map
     * @throws GenericEntityException if error occur
     */
    @SuppressWarnings("unchecked")
    public static Map prepareComparativeFlowReportParameters(HttpServletRequest request) throws GenericEntityException {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
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

        Map<String, Object> ctxt = FastMap.newInstance();
        ctxt.put("userLogin", userLogin);
        ctxt.put("organizationPartyId", organizationPartyId);
        ctxt.put("glFiscalTypeId1", glFiscalTypeId1);
        ctxt.put("glFiscalTypeId2", glFiscalTypeId2);
        // determine the period
        if (dateOption != null) {
            if (dateOption.equals("byDate")) {
                fromDateText1 = UtilCommon.getParameter(request, "fromDate1");
                thruDateText1 = UtilCommon.getParameter(request, "thruDate1");
                fromDateText2 = UtilCommon.getParameter(request, "fromDate2");
                thruDateText2 = UtilCommon.getParameter(request, "thruDate2");
                fromDate1 = UtilDateTime.getDayStart(UtilDate.toTimestamp(fromDateText1, timeZone, locale), timeZone, locale);
                thruDate1 = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText1, timeZone, locale), timeZone, locale);
                fromDate2 = UtilDateTime.getDayStart(UtilDate.toTimestamp(fromDateText2, timeZone, locale), timeZone, locale);
                thruDate2 = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText2, timeZone, locale), timeZone, locale);
                ctxt.put("fromDate1", fromDate1);
                ctxt.put("thruDate1", thruDate1);
                ctxt.put("fromDate2", fromDate2);
                ctxt.put("thruDate2", thruDate2);
            } else if (dateOption.equals("byTimePeriod")) {
                String fromCustomTimePeriodId = UtilCommon.getParameter(request, "fromCustomTimePeriodId");
                String thruCustomTimePeriodId = UtilCommon.getParameter(request, "thruCustomTimePeriodId");
                ctxt.put("fromCustomTimePeriodId", fromCustomTimePeriodId);
                ctxt.put("thruCustomTimePeriodId", thruCustomTimePeriodId);
                GenericValue fromTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", fromCustomTimePeriodId));
                GenericValue thruTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", thruCustomTimePeriodId));
                fromDate1 = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(((Date) fromTimePeriod.get("fromDate")).getTime()), timeZone, locale);
                thruDate1 = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) fromTimePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
                fromDate2 = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(((Date) thruTimePeriod.get("fromDate")).getTime()), timeZone, locale);
                thruDate2 = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) thruTimePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
                ctxt.put("fromDate1", fromDate1);
                ctxt.put("thruDate1", thruDate1);
                ctxt.put("fromDate2", fromDate2);
                ctxt.put("thruDate2", thruDate2);
            }
        }
        return ctxt;
    }

    /**
     * Prepare query parameters for comparative state report.
     * @param request a <code>HttpServletRequest</code> value
     * @return query parameter map
     * @throws GenericEntityException if error occur
     */
    @SuppressWarnings("unchecked")
    public static Map prepareComparativeStateReportParameters(HttpServletRequest request) throws GenericEntityException {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        String fromGlFiscalTypeId = UtilCommon.getParameter(request, "fromGlFiscalTypeId");
        String toGlFiscalTypeId = UtilCommon.getParameter(request, "toGlFiscalTypeId");
       String fromDateText = null;
        String thruDateText = null;
        Timestamp fromDate = null;
        Timestamp thruDate = null;
        String dateOption = UtilCommon.getParameter(request, "reportDateOption");

        Map<String, Object> ctxt = FastMap.newInstance();
        ctxt.put("userLogin", userLogin);
        ctxt.put("organizationPartyId", organizationPartyId);
        ctxt.put("fromGlFiscalTypeId", fromGlFiscalTypeId);
        ctxt.put("toGlFiscalTypeId", toGlFiscalTypeId);

        // determine the period
        if (dateOption != null) {
            // do nothing
            if (dateOption.equals("byDate")) {
                fromDateText = UtilCommon.getParameter(request, "fromDate");
                thruDateText = UtilCommon.getParameter(request, "thruDate");
                fromDate = UtilDateTime.getDayEnd(UtilDate.toTimestamp(fromDateText, timeZone, locale), timeZone, locale);
                thruDate = UtilDateTime.getDayEnd(UtilDate.toTimestamp(thruDateText, timeZone, locale), timeZone, locale);
                ctxt.put("fromDate", fromDate);
                ctxt.put("thruDate", thruDate);
            } else if (dateOption.equals("byTimePeriod")) {
                String fromCustomTimePeriodId = UtilCommon.getParameter(request, "fromCustomTimePeriodId");
                String thruCustomTimePeriodId = UtilCommon.getParameter(request, "thruCustomTimePeriodId");
                ctxt.put("fromCustomTimePeriodId", fromCustomTimePeriodId);
                ctxt.put("thruCustomTimePeriodId", thruCustomTimePeriodId);
                GenericValue fromTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", fromCustomTimePeriodId));
                GenericValue thruTimePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", thruCustomTimePeriodId));
                //  the time periods end at the beginning of the thruDate,  end we want to adjust it so that the report ends at the end of day before the thruDate
                fromDate = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) fromTimePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
                thruDate = UtilDateTime.adjustTimestamp(UtilDateTime.getTimestamp(((Date) thruTimePeriod.get("thruDate")).getTime()), Calendar.MILLISECOND, -1, timeZone, locale);
                ctxt.put("fromDate", fromDate);
                ctxt.put("thruDate", thruDate);
            }
        }
        return ctxt;
    }

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
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);
        Map<String, String> groupMarkers = UtilMisc.<String, String>toMap(
                "REVENUE", uiLabelMap.get("FinancialsGrossProfit"), "COGS", uiLabelMap.get("FinancialsGrossProfit"),
               "OPERATING_EXPENSE", uiLabelMap.get("FinancialsOperatingIncome"),
                "OTHER_EXPENSE", uiLabelMap.get("FinancialsPretaxIncome"), "OTHER_INCOME", uiLabelMap.get("FinancialsPretaxIncome"),
                "TAX_EXPENSE", uiLabelMap.get("AccountingNetIncome")
        );
        String reportType = UtilCommon.getParameter(request, "type");
        try {
            Map ctxt = prepareFinancialReportParameters(request);
            UtilAccountingTags.addTagParameters(request, ctxt);
            Map<String, ?> results = dispatcher.runSync("getIncomeStatementByDates", dispatcher.getDispatchContext().makeValidContext("getIncomeStatementByDates", ModelService.IN_PARAM, ctxt));
            Map<String, Object> glAccountSums = (Map<String, Object>) results.get("glAccountSums");
            BigDecimal grossProfit = (BigDecimal) results.get("grossProfit");
            BigDecimal operatingIncome = (BigDecimal) results.get("operatingIncome");
            BigDecimal pretaxIncome = (BigDecimal) results.get("pretaxIncome");
            BigDecimal netIncome = (BigDecimal) results.get("netIncome");
            Map<String, BigDecimal> glAccountGroupSums = (Map) results.get("glAccountGroupSums");

            List<Map<String, Object>> rows = FastList.newInstance();

            Integer i = 1;
            for (String glAccountTypeId : FinancialServices.INCOME_STATEMENT_TYPES) {
                List<Map<String, Object>> accounts = (List<Map<String, Object>>) glAccountSums.get(glAccountTypeId);

                // find account type
                GenericValue glAccountType = delegator.findByPrimaryKey("GlAccountType", UtilMisc.toMap("glAccountTypeId", glAccountTypeId));

                // create records for report
                if (UtilValidate.isNotEmpty(accounts)) {
                    for (Map<String, Object> account : accounts) {
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
            Collections.sort(rows, new Comparator<Map<String, Object>>() {
                public int compare(Map<String, Object> o1, Map<String, Object> o2) {
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
            jrParameters.put("glFiscalTypeId", ctxt.get("glFiscalTypeId"));
            jrParameters.put("fromDate", ctxt.get("fromDate"));
            jrParameters.put("thruDate", ctxt.get("thruDate"));
            jrParameters.put("organizationPartyId", ctxt.get("organizationPartyId"));
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, (String) ctxt.get("organizationPartyId"), false));
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
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String reportType = UtilCommon.getParameter(request, "type");

        try {

            Map ctxt = prepareComparativeFlowReportParameters(request);
            // validate parameters
            if (ctxt.get("fromDate1") == null || ctxt.get("thruDate1") == null || ctxt.get("fromDate2") == null || ctxt.get("thruDate2") == null) {
                return UtilMessage.createAndLogEventError(request, "FinancialsError_DateRangeMissing", locale, MODULE);
            }

            UtilAccountingTags.addTagParameters(request, ctxt);
            Map<String, Object> results = dispatcher.runSync("getComparativeIncomeStatement", dispatcher.getDispatchContext().makeValidContext("getComparativeIncomeStatement", ModelService.IN_PARAM, ctxt));

            Map<String, Object> set1IncomeStatement = (Map<String, Object>) results.get("set1IncomeStatement");
            Map<String, Object> set2IncomeStatement = (Map<String, Object>) results.get("set2IncomeStatement");
            BigDecimal netIncome1 = (BigDecimal) set1IncomeStatement.get("netIncome");
            BigDecimal netIncome2 = (BigDecimal) set2IncomeStatement.get("netIncome");
            Map<GenericValue, BigDecimal> accountBalances = (Map<GenericValue, BigDecimal>) results.get("accountBalances");
            Map<GenericValue, BigDecimal> set1Accounts = (Map<GenericValue, BigDecimal>) set1IncomeStatement.get("glAccountSumsFlat");
            Map<GenericValue, BigDecimal> set2Accounts = (Map<GenericValue, BigDecimal>) set2IncomeStatement.get("glAccountSumsFlat");
            GenericValue glFiscalType1 = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", ctxt.get("glFiscalTypeId1")));
            GenericValue glFiscalType2 = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", ctxt.get("glFiscalTypeId2")));

            List<Map<String, Object>> rows = FastList.newInstance();

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
            Collections.sort(rows, new Comparator<Map<String, Object>>() {
                public int compare(Map<String, Object> o1, Map<String, Object> o2) {
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
            jrParameters.put("fromDateLeft", ctxt.get("fromDate1"));
            jrParameters.put("thruDateLeft", ctxt.get("thruDate1"));
            jrParameters.put("fromDateRight", ctxt.get("fromDate2"));
            jrParameters.put("thruDateRight", ctxt.get("thruDate2"));
            jrParameters.put("netIncomeLeft", netIncome1);
            jrParameters.put("netIncomeRight", netIncome2);
            jrParameters.put("fiscalTypeLeft", glFiscalType1.get("description", locale));
            jrParameters.put("fiscalTypeRight", glFiscalType2.get("description", locale));
            jrParameters.put("organizationPartyId", ctxt.get("organizationPartyId"));
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, (String) ctxt.get("organizationPartyId"), false));
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
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String reportType = UtilCommon.getParameter(request, "type");

        try {

            // retrieve financial data
            Map ctxt = prepareFinancialReportParameters(request);
            UtilAccountingTags.addTagParameters(request, ctxt);
            Map<String, Object> results = dispatcher.runSync("getTrialBalanceForDate", dispatcher.getDispatchContext().makeValidContext("getTrialBalanceForDate", ModelService.IN_PARAM, ctxt));
            List<Map<String, Object>> rows = FastList.newInstance();

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
            jrParameters.put("asOfDate", ctxt.get("asOfDate"));
            jrParameters.put("glFiscalTypeId", ctxt.get("glFiscalTypeId"));
            jrParameters.put("organizationPartyId", ctxt.get("organizationPartyId"));
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, (String) ctxt.get("organizationPartyId"), false));
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
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);
        String reportType = UtilCommon.getParameter(request, "type");

        try {

            // retrieve financial data
            Map ctxt = prepareFinancialReportParameters(request);
            UtilAccountingTags.addTagParameters(request, ctxt);
            Map<String, Object> results = dispatcher.runSync("getBalanceSheetForDate", dispatcher.getDispatchContext().makeValidContext("getBalanceSheetForDate", ModelService.IN_PARAM, ctxt));

            List<GenericValue> assetAccounts = EntityUtil.orderBy(((Map) results.get("assetAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> assetAccountBalances = (Map) results.get("assetAccountBalances");
            List<GenericValue> liabilityAccounts = EntityUtil.orderBy(((Map) results.get("liabilityAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> liabilityAccountBalances = (Map) results.get("liabilityAccountBalances");
            List<GenericValue> equityAccounts = EntityUtil.orderBy(((Map) results.get("equityAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> equityAccountBalances = (Map) results.get("equityAccountBalances");

            List<Map<String, Object>> rows = FastList.newInstance();

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
            Collections.sort(rows, new Comparator<Map<String, Object>>() {
                public int compare(Map<String, Object> o1, Map<String, Object> o2) {
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
            jrParameters.put("glFiscalTypeId", ctxt.get("glFiscalTypeId"));
            jrParameters.put("asOfDate", ctxt.get("asOfDate"));
            jrParameters.put("organizationPartyId", ctxt.get("organizationPartyId"));
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, (String) ctxt.get("organizationPartyId"), false));
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
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);
        String reportType = UtilCommon.getParameter(request, "type");

        try {
            // retrieve financial data
            Map ctxt = prepareComparativeStateReportParameters(request);
            if (ctxt.get("fromDate") == null || ctxt.get("thruDate") == null) {
                return UtilMessage.createAndLogEventError(request, "FinancialsError_FromOrThruDateMissing", locale, MODULE);
            }

            UtilAccountingTags.addTagParameters(request, ctxt);
            Map<String, Object> results = dispatcher.runSync("getComparativeBalanceSheet", dispatcher.getDispatchContext().makeValidContext("getComparativeBalanceSheet", ModelService.IN_PARAM, ctxt));

            List<GenericValue> assetAccounts = EntityUtil.orderBy(((Map<GenericValue, BigDecimal>) results.get("assetAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> assetAccountBalances = (Map<GenericValue, BigDecimal>) results.get("assetAccountBalances");
            List<GenericValue> liabilityAccounts = EntityUtil.orderBy(((Map<GenericValue, BigDecimal>) results.get("liabilityAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> liabilityAccountBalances = (Map<GenericValue, BigDecimal>) results.get("liabilityAccountBalances");
            List<GenericValue> equityAccounts = EntityUtil.orderBy(((Map<GenericValue, BigDecimal>) results.get("equityAccountBalances")).keySet(), UtilMisc.toList("glAccountId"));
            Map<GenericValue, BigDecimal> equityAccountBalances = (Map<GenericValue, BigDecimal>) results.get("equityAccountBalances");

            Map<String, Object> fromDateAccountBalances = (Map<String, Object>) results.get("fromDateAccountBalances");
            Map<GenericValue, BigDecimal> fromAssetAccountBalances = (Map<GenericValue, BigDecimal>) fromDateAccountBalances.get("assetAccountBalances");
            Map<GenericValue, BigDecimal> fromLiabilityAccountBalances = (Map<GenericValue, BigDecimal>) fromDateAccountBalances.get("liabilityAccountBalances");
            Map<GenericValue, BigDecimal> fromEquityAccountBalances = (Map<GenericValue, BigDecimal>) fromDateAccountBalances.get("equityAccountBalances");

            Map<String, Object> thruDateAccountBalances = (Map<String, Object>) results.get("thruDateAccountBalances");
            Map<GenericValue, BigDecimal> toAssetAccountBalances = (Map<GenericValue, BigDecimal>) thruDateAccountBalances.get("assetAccountBalances");
            Map<GenericValue, BigDecimal> toLiabilityAccountBalances = (Map<GenericValue, BigDecimal>) thruDateAccountBalances.get("liabilityAccountBalances");
            Map<GenericValue, BigDecimal> toEquityAccountBalances = (Map<GenericValue, BigDecimal>) thruDateAccountBalances.get("equityAccountBalances");

            GenericValue fromGlFiscalType = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", ctxt.get("fromGlFiscalTypeId")));
            GenericValue toGlFiscalType = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", ctxt.get("toGlFiscalTypeId")));

            List<Map<String, Object>> rows = FastList.newInstance();

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
            Collections.sort(rows, new Comparator<Map<String, Object>>() {
                public int compare(Map<String, Object> o1, Map<String, Object> o2) {
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
            jrParameters.put("fromDate", ctxt.get("fromDate"));
            jrParameters.put("thruDate", ctxt.get("thruDate"));
            jrParameters.put("fiscalTypeLeft", fromGlFiscalType.get("description", locale));
            jrParameters.put("fiscalTypeRight", toGlFiscalType.get("description", locale));
            jrParameters.put("organizationPartyId", ctxt.get("organizationPartyId"));
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, (String) ctxt.get("organizationPartyId"), false));
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
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String reportType = UtilCommon.getParameter(request, "type");

        try {
            // retrieve financial data
            Map ctxt = prepareFinancialReportParameters(request);
            if (ctxt.get("fromDate") == null || ctxt.get("thruDate") == null) {
                return UtilMessage.createAndLogEventError(request, "FinancialsError_FromOrThruDateMissing", locale, MODULE);
            }

            UtilAccountingTags.addTagParameters(request, ctxt);
            Map<String, Object> results = dispatcher.runSync("getCashFlowStatementForDates", dispatcher.getDispatchContext().makeValidContext("getCashFlowStatementForDates", ModelService.IN_PARAM, ctxt));

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

            List<Map<String, Object>> rows = FastList.newInstance();

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
            jrParameters.put("glFiscalTypeId", ctxt.get("glFiscalTypeId"));
            jrParameters.put("fromDate", ctxt.get("fromDate"));
            jrParameters.put("thruDate", ctxt.get("thruDate"));
            jrParameters.put("organizationPartyId", ctxt.get("organizationPartyId"));
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, (String) ctxt.get("organizationPartyId"), false));
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
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String reportType = UtilCommon.getParameter(request, "type");

        try {

            // retrieve financial data
            Map ctxt = prepareComparativeFlowReportParameters(request);
            if (ctxt.get("fromDate1") == null || ctxt.get("thruDate1") == null || ctxt.get("fromDate2") == null || ctxt.get("thruDate2") == null) {
                return UtilMessage.createAndLogEventError(request, "FinancialsError_DateRangeMissing", locale, MODULE);
            }

            UtilAccountingTags.addTagParameters(request, ctxt);
            Map<String, Object> results = dispatcher.runSync("getComparativeCashFlowStatement", dispatcher.getDispatchContext().makeValidContext("getComparativeCashFlowStatement", ModelService.IN_PARAM, ctxt));

            Map<String, Object> set1CashFlowStatement = (Map<String, Object>) results.get("set1CashFlowStatement");
            Map<String, Object> set2CashFlowStatement = (Map<String, Object>) results.get("set2CashFlowStatement");

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

            List<Map<String, Object>> rows = FastList.newInstance();

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
            Collections.sort(rows, new Comparator<Map<String, Object>>() {
                public int compare(Map<String, Object> o1, Map<String, Object> o2) {
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

            GenericValue glFiscalType1 = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", ctxt.get("glFiscalTypeId1")));
            GenericValue glFiscalType2 = delegator.findByPrimaryKey("GlFiscalType", UtilMisc.toMap("glFiscalTypeId", ctxt.get("glFiscalTypeId2")));

            // prepare report parameters
            Map<String, Object> jrParameters = FastMap.newInstance();
            jrParameters.put("fromDateLeft", ctxt.get("fromDate1"));
            jrParameters.put("thruDateLeft", ctxt.get("thruDate1"));
            jrParameters.put("fromDateRight", ctxt.get("fromDate2"));
            jrParameters.put("thruDateRight", ctxt.get("thruDate2"));
            jrParameters.put("organizationPartyId", ctxt.get("organizationPartyId"));
            jrParameters.put("organizationName", PartyHelper.getPartyName(delegator, (String) ctxt.get("organizationPartyId"), false));
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

    /**
     * Call a service to update GlAccountTransEntryFact entity. This intermediate data are used by budgeting reports.
     *
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code> value
     */
    public static String createGlAccountTransEntryFacts(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        Map<String, Object> context = FastMap.<String, Object>newInstance();
        context.put("organizationPartyId", organizationPartyId);
        context.put("userLogin", userLogin);
        try {
            dispatcher.runSync("financials.collectEncumbranceAndTransEntryFacts", context);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogEventError(request, e, UtilHttp.getLocale(request), MODULE);
        }

        return "success";
    }

    /**
     * Load data to TaxInvoiceItemFact entity.
     * @param dctx a <code>DispatchContext</code> instance
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> loadTaxInvoiceItemFact(DispatchContext dctx, Map<String, Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
        }
        TimeZone timeZone = (TimeZone) context.get("timeZone");
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }

        Set<String> uniqueInvoices = FastSet.<String>newInstance();
        Long sequenceId = 0L;

        try {

            DynamicViewEntity salesInvoiceItems = new DynamicViewEntity();
            salesInvoiceItems.addMemberEntity("I", "Invoice");
            salesInvoiceItems.addMemberEntity("II", "InvoiceItem");
            salesInvoiceItems.addViewLink("I", "II", false, ModelKeyMap.makeKeyMapList("invoiceId"));
            salesInvoiceItems.addAlias("I", "invoiceDate");
            salesInvoiceItems.addAlias("I", "currencyUomId");
            salesInvoiceItems.addAlias("I", "invoiceTypeId");
            salesInvoiceItems.addAlias("I", "statusId");
            salesInvoiceItems.addAlias("I", "partyIdFrom");
            salesInvoiceItems.addAlias("II", "invoiceId");
            salesInvoiceItems.addAlias("II", "invoiceItemSeqId");
            salesInvoiceItems.addAlias("II", "invoiceItemTypeId");
            salesInvoiceItems.addAlias("II", "quantity");
            salesInvoiceItems.addAlias("II", "amount");

            List<String> selectList = UtilMisc.toList("invoiceId", "invoiceItemSeqId", "invoiceDate", "currencyUomId", "quantity", "amount");
            selectList.add("partyIdFrom");

            DateFormat dayOfMonthFmt = new SimpleDateFormat("dd");
            DateFormat monthOfYearFmt = new SimpleDateFormat("MM");
            DateFormat yearNumberFmt = new SimpleDateFormat("yyyy");

            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "SALES_INVOICE"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, Arrays.asList("INVOICE_IN_PROCESS", "INVOICE_CANCELLED", "INVOICE_VOIDED", "INVOICE_WRITEOFF")),
                    EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.NOT_IN, Arrays.asList("ITM_SALES_TAX", "INV_SALES_TAX"))
            );
            EntityListIterator itemsIterator = delegator.findListIteratorByCondition(salesInvoiceItems, conditions, null, selectList, UtilMisc.toList("invoiceId", "invoiceItemSeqId"), null);
            GenericValue invoiceItem = null;
            while ((invoiceItem = itemsIterator.next()) != null) {
                GenericValue taxInvItemFact = delegator.makeValue("TaxInvoiceItemFact");

                // set item ids
                String invoiceId = invoiceItem.getString("invoiceId");
                uniqueInvoices.add(invoiceId);
                String invoiceItemSeqId = invoiceItem.getString("invoiceItemSeqId");
                taxInvItemFact.put("invoiceId", invoiceId);
                taxInvItemFact.put("invoiceItemSeqId", invoiceItemSeqId);

                // set gross amount
                BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
                if (quantity == null) {
                    quantity = BigDecimal.ONE;
                }

                BigDecimal amount = invoiceItem.getBigDecimal("amount");
                if (amount == null) {
                    amount = BigDecimal.ZERO;
                }

                BigDecimal grossAmount = quantity.multiply(amount);
                taxInvItemFact.set("grossAmount", grossAmount != null ? grossAmount.doubleValue() : null);

                // set total promotions amount
                BigDecimal totalPromotions = getTotalPromoAmount(invoiceId, invoiceItemSeqId, delegator);
                taxInvItemFact.set("discounts", totalPromotions != null ? totalPromotions.doubleValue() : null);

                // set total refunds
                BigDecimal totalRefunds = getTotalRefundAmount(invoiceId, invoiceItemSeqId, delegator);
                taxInvItemFact.set("refunds", totalRefunds != null ? totalRefunds.doubleValue() : null);

                // set net amount
                taxInvItemFact.set("netAmount", grossAmount.subtract(totalRefunds).add(totalPromotions).doubleValue());

                // set tax due amount
                List<Map<String, Object>> taxes = getTaxDueAmount(invoiceId, invoiceItemSeqId, delegator);

                // lookup and set date dimension id
                Timestamp invoiceDate = invoiceItem.getTimestamp("invoiceDate");
                String dayOfMonth = dayOfMonthFmt.format(invoiceDate);
                String monthOfYear = monthOfYearFmt.format(invoiceDate);
                String yearNumber = yearNumberFmt.format(invoiceDate);

                EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("dayOfMonth", EntityOperator.EQUALS, dayOfMonth),
                        EntityCondition.makeCondition("monthOfYear", EntityOperator.EQUALS, monthOfYear),
                        EntityCondition.makeCondition("yearNumber", EntityOperator.EQUALS, yearNumber));
                taxInvItemFact.set("dateDimId", UtilEtl.lookupDimension("DateDim", "dateDimId", dateDimConditions, delegator));

                // lookup and set store dimension id
                taxInvItemFact.set("storeDimId", lookupStoreDim(invoiceId, invoiceItemSeqId, delegator));

                // lookup and set currency dimension id
                taxInvItemFact.set("currencyDimId", UtilEtl.lookupDimension("CurrencyDim", "currencyDimId", EntityCondition.makeCondition("uomId", EntityOperator.EQUALS, invoiceItem.getString("currencyUomId")), delegator)
                );

                // lookup and set organization dimension id
                taxInvItemFact.set("organizationDimId", UtilEtl.lookupDimension("OrganizationDim", "organizationDimId", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, invoiceItem.getString("partyIdFrom")), delegator)
                );

                // store collected records
                if (UtilValidate.isNotEmpty(taxes)) {
                    for (Map<String, Object> taxInfo : taxes) {
                        // add a record for each tax authority party & geo
                        EntityCondition taxAuthCondList = EntityCondition.makeCondition(EntityOperator.AND,
                                        EntityCondition.makeCondition("taxAuthPartyId", EntityOperator.EQUALS, taxInfo.get("taxAuthPartyId")),
                                        EntityCondition.makeCondition("taxAuthGeoId", EntityOperator.EQUALS, taxInfo.get("taxAuthGeoId")));
                        taxInvItemFact.set("taxAuthorityDimId", UtilEtl.lookupDimension("TaxAuthorityDim", "taxAuthorityDimId", taxAuthCondList, delegator));
                        BigDecimal taxDue = (BigDecimal) taxInfo.get("taxDue");
                        taxInvItemFact.set("taxDue", taxDue != null ? taxDue.doubleValue() : null);
                        BigDecimal taxable = (BigDecimal) taxInfo.get("taxable");
                        taxInvItemFact.set("taxable", taxable.subtract(totalRefunds).doubleValue());
                        sequenceId++;
                        taxInvItemFact.set("taxInvItemFactId", sequenceId);
                        taxInvItemFact.create();
                    }
                } else {
                    taxInvItemFact.set("taxAuthorityDimId", Long.valueOf(0));
                    taxInvItemFact.set("taxDue", 0.0);
                    taxInvItemFact.set("taxable", 0.0);
                    sequenceId++;
                    taxInvItemFact.set("taxInvItemFactId", sequenceId);
                    taxInvItemFact.create();
                }

            }

            itemsIterator.close();

            DynamicViewEntity dv = new DynamicViewEntity();
            dv.addMemberEntity("I", "Invoice");
            dv.addMemberEntity("II", "InvoiceItem");
            dv.addViewLink("I", "II", false, ModelKeyMap.makeKeyMapList("invoiceId"));
            dv.addAlias("I", "invoiceDate");
            dv.addAlias("I", "currencyUomId");
            dv.addAlias("I", "invoiceId");
            dv.addAlias("I", "partyIdFrom");
            dv.addAlias("II", "invoiceItemSeqId");
            dv.addAlias("II", "invoiceItemTypeId");
            dv.addAlias("II", "parentInvoiceId");
            dv.addAlias("II", "parentInvoiceItemSeqId");
            dv.addAlias("II", "quantity");
            dv.addAlias("II", "amount");
            dv.addAlias("II", "taxAuthPartyId");
            dv.addAlias("II", "taxAuthGeoId");
            ComplexAlias totalAlias = new ComplexAlias("*");
            totalAlias.addComplexAliasMember(new ComplexAliasField("II", "quantity", "1.0", null));
            totalAlias.addComplexAliasMember(new ComplexAliasField("II", "amount", "0.0", null));
            dv.addAlias("II", "totalAmount", null, null, null, null, null, totalAlias);

            selectList = UtilMisc.toList("invoiceId", "invoiceItemSeqId", "totalAmount", "taxAuthPartyId", "taxAuthGeoId", "currencyUomId");
            selectList.add("partyIdFrom");
            selectList.add("invoiceDate");

            // handle tax invoice items w/o parent invoice/item
            EntityCondition invoiceLevelTaxConditions = EntityCondition.makeCondition(EntityOperator.AND,
                            EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.EQUALS, "ITM_SALES_TAX"),
                            EntityCondition.makeCondition("invoiceId", EntityOperator.IN, uniqueInvoices),
                            EntityCondition.makeCondition("parentInvoiceId", EntityOperator.EQUALS, null),
                            EntityCondition.makeCondition("parentInvoiceItemSeqId", EntityOperator.EQUALS, null));

            EntityListIterator iter2 = delegator.findListIteratorByCondition(dv, invoiceLevelTaxConditions, null, selectList, null, null);
            GenericValue tax = null;
            while ((tax = iter2.next()) != null) {
                BigDecimal amount = tax.getBigDecimal("totalAmount");
                if (amount != null) {

                    Timestamp invoiceDate = tax.getTimestamp("invoiceDate");
                    String dayOfMonth = dayOfMonthFmt.format(invoiceDate);
                    String monthOfYear = monthOfYearFmt.format(invoiceDate);
                    String yearNumber = yearNumberFmt.format(invoiceDate);
                    EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
                            EntityCondition.makeCondition("dayOfMonth", EntityOperator.EQUALS, dayOfMonth),
                            EntityCondition.makeCondition("monthOfYear", EntityOperator.EQUALS, monthOfYear),
                            EntityCondition.makeCondition("yearNumber", EntityOperator.EQUALS, yearNumber));

                    GenericValue taxInvItemFact = delegator.makeValue("TaxInvoiceItemFact");
                    taxInvItemFact.set("dateDimId", UtilEtl.lookupDimension("DateDim", "dateDimId", dateDimConditions, delegator));
                    taxInvItemFact.set("storeDimId", lookupStoreDim(tax.getString("invoiceId"), tax.getString("invoiceItemSeqId"), delegator));
                    EntityCondition taxAuthCondList = EntityCondition.makeCondition(EntityOperator.AND,
                            EntityCondition.makeCondition("taxAuthPartyId", EntityOperator.EQUALS, tax.get("taxAuthPartyId")),
                            EntityCondition.makeCondition("taxAuthGeoId", EntityOperator.EQUALS, tax.get("taxAuthGeoId")));
                    taxInvItemFact.set("taxAuthorityDimId", UtilEtl.lookupDimension("TaxAuthorityDim", "taxAuthorityDimId", taxAuthCondList, delegator));
                    taxInvItemFact.set("currencyDimId", UtilEtl.lookupDimension("CurrencyDim", "currencyDimId", EntityCondition.makeCondition("uomId", EntityOperator.EQUALS, tax.getString("currencyUomId")), delegator));
                    taxInvItemFact.set("organizationDimId", UtilEtl.lookupDimension("OrganizationDim", "organizationDimId", EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, tax.getString("partyIdFrom")), delegator));
                    taxInvItemFact.set("invoiceId", tax.getString("invoiceId"));
                    taxInvItemFact.set("invoiceItemSeqId", tax.getString("invoiceItemSeqId"));
                    taxInvItemFact.set("grossAmount", 0.0);
                    taxInvItemFact.set("discounts", 0.0);
                    taxInvItemFact.set("refunds", 0.0);
                    taxInvItemFact.set("netAmount", 0.0);
                    taxInvItemFact.set("taxable", 0.0);
                    BigDecimal totalAmount = tax.getBigDecimal("totalAmount");
                    taxInvItemFact.set("taxDue", totalAmount != null ? totalAmount.doubleValue() : null);
                    sequenceId++;
                    taxInvItemFact.set("taxInvItemFactId", sequenceId);
                    taxInvItemFact.create();
                }
            }
            iter2.close();

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return results;
    }

    /**
     * <p>Collect sales taxes on given sales invoice item.<br>
     * Since a few tax items to different authorities are possible this method returns records grouped
     * by tax authority party and geographical unit.</p>
     *
     * @param invoiceId sales invoice id
     * @param invoiceItemSeqId sales invoice item id
     * @param delegator <code>GenericDelegator</code> instance
     * @return
     * Each record is <code>Map</code> with following members:<br>
     * <b>taxAuthPartyId</b> : tax authority party<br>
     * <b>taxAuthGeoId</b> : tax authority geographical unit<br>
     * <b>taxDue</b> : sales tax amount under the taxing authority<br>
     * <b>taxable</b> : taxable amount of specified invoice item
     * @throws GenericEntityException
     */
    private static List<Map<String, Object>> getTaxDueAmount(String invoiceId, String invoiceItemSeqId, GenericDelegator delegator) throws GenericEntityException {
        List<Map<String, Object>> taxes = FastList.newInstance();

        DynamicViewEntity dv = new DynamicViewEntity();
        ComplexAlias a = new ComplexAlias("*");
        a.addComplexAliasMember(new ComplexAliasField("II", "quantity", "1.0", null));
        a.addComplexAliasMember(new ComplexAliasField("II", "amount", "0.0", null));
        dv.addMemberEntity("II", "InvoiceItem");
        dv.addAlias("II", "taxAuthPartyId", null, null, null, Boolean.TRUE, null);
        dv.addAlias("II", "taxAuthGeoId", null, null, null, Boolean.TRUE, null);
        dv.addAlias("II", "parentInvoiceId");
        dv.addAlias("II", "parentInvoiceItemSeqId");
        dv.addAlias("II", "quantity");
        dv.addAlias("II", "amount");
        dv.addAlias("II", "totalTaxDue", null, null, null, null, "sum", a);
        dv.addAlias("II", "invoiceId", null, null, null, Boolean.TRUE, null);
        dv.addAlias("II", "invoiceItemSeqId", null, null, null, Boolean.TRUE, null);
        dv.addAlias("II", "invoiceItemTypeId");

        EntityCondition conditionList = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.EQUALS, "ITM_SALES_TAX"),
                        EntityCondition.makeCondition("parentInvoiceId", EntityOperator.EQUALS, invoiceId),
                        EntityCondition.makeCondition("parentInvoiceItemSeqId", EntityOperator.EQUALS, invoiceItemSeqId));

        List<String> selectList = UtilMisc.toList("totalTaxDue", "taxAuthPartyId", "taxAuthGeoId");

        EntityListIterator iter = delegator.findListIteratorByCondition(dv, conditionList, null, selectList, null, null);
        List<GenericValue> taxItems = iter.getCompleteList();
        iter.close();
        for (GenericValue taxItem : taxItems) {
            BigDecimal taxDue = taxItem.getBigDecimal("totalTaxDue");
            BigDecimal taxAdjAmount = BigDecimal.ZERO;

            String taxAuthPartyId = taxItem.getString("taxAuthPartyId");
            String taxAuthGeoId = taxItem.getString("taxAuthGeoId");

            // calculate taxable amount for current invoiceId, invoiceItemSeqId and authority data
            BigDecimal taxable = BigDecimal.ZERO;
            GenericValue originalInvoiceItem = delegator.findByPrimaryKey("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId));
            if (originalInvoiceItem != null) {
                BigDecimal taxableQuantity = originalInvoiceItem.getBigDecimal("quantity");
                BigDecimal taxableAmount = originalInvoiceItem.getBigDecimal("amount");
                taxable = (taxableQuantity == null ? BigDecimal.ONE : taxableQuantity).multiply((taxableAmount == null ? BigDecimal.ZERO : taxableAmount));
            }

            // track relation to order and further to return invoice
            List<GenericValue> orderItemBillings = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId));
            for (GenericValue orderItemBilling : orderItemBillings) {
                List<GenericValue> returnItems = delegator.findByAnd("ReturnItem", UtilMisc.toMap("orderId", orderItemBilling.getString("orderId"), "orderItemSeqId", orderItemBilling.getString("orderItemSeqId")));
                for (GenericValue returnItem : returnItems) {
                    List<GenericValue> returnBillings = returnItem.getRelated("ReturnItemBilling");
                    for (GenericValue returnItemBilling : returnBillings) {
                        String ribInvoiceId = returnItemBilling.getString("invoiceId");
                        String ribInvoiceItemSeqId = returnItemBilling.getString("invoiceItemSeqId");

                        DynamicViewEntity rdv = new DynamicViewEntity();
                        rdv.addMemberEntity("RI", "Invoice");
                        rdv.addMemberEntity("RII", "InvoiceItem");
                        rdv.addAlias("RI", "invoiceId");
                        rdv.addAlias("RI", "invoiceTypeId");
                        rdv.addAlias("RII", "parentInvoiceId");
                        rdv.addAlias("RII", "parentInvoiceItemSeqId");
                        rdv.addAlias("RII", "invoiceItemTypeId");
                        rdv.addAlias("RII", "taxAuthPartyId");
                        rdv.addAlias("RII", "taxAuthGeoId");
                        rdv.addAlias("RII", "quantity");
                        rdv.addAlias("RII", "amount");
                        ComplexAlias r = new ComplexAlias("*");
                        r.addComplexAliasMember(new ComplexAliasField("RII", "quantity", "1.0", null));
                        r.addComplexAliasMember(new ComplexAliasField("RII", "amount", "0.0", null));
                        rdv.addAlias("RII", "totalTaxRefundAdj", null, null, null, null, "sum", r);

                        EntityCondition conditionList1 = EntityCondition.makeCondition(EntityOperator.AND,
                                        EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, ribInvoiceId),
                                        EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "CUST_RTN_INVOICE"),
                                        EntityCondition.makeCondition("parentInvoiceId", EntityOperator.EQUALS, ribInvoiceId),
                                        EntityCondition.makeCondition("parentInvoiceItemSeqId", EntityOperator.EQUALS, ribInvoiceItemSeqId),
                                        EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.EQUALS, "CRT_SALES_TAX_ADJ"),
                                        EntityCondition.makeCondition("taxAuthPartyId", EntityOperator.EQUALS, taxAuthPartyId),
                                        EntityCondition.makeCondition("taxAuthGeoId", EntityOperator.EQUALS, taxAuthGeoId));

                        EntityListIterator iter1 = delegator.findListIteratorByCondition(rdv, conditionList1, null, Arrays.asList("totalTaxRefundAdj"), null, null);
                        List<GenericValue> taxAdjs = iter1.getCompleteList();
                        iter1.close();
                        for (GenericValue taxAdj : taxAdjs) {
                            BigDecimal totalTaxRefundAdj = taxAdj.getBigDecimal("totalTaxRefundAdj");
                            if (totalTaxRefundAdj != null) {
                                taxAdjAmount = taxAdjAmount.add(totalTaxRefundAdj);
                            }
                        }
                    }
                }
            }

            taxDue = taxDue.subtract(taxAdjAmount);

            //
            taxes.add(UtilMisc.<String, Object>toMap(
                    "taxDue", taxDue,
                    "taxAuthPartyId", taxAuthPartyId,
                    "taxAuthGeoId", taxAuthGeoId,
                    "taxable", taxable
            ));
        }

        return taxes;
    }

    /**
     * Returns amount that corresponds to sales subject returned by customer.
     *
     * @param invoiceId sales invoice id
     * @param invoiceItemSeqId sales invoice item id
     * @param delegator <code>GenericDelegator</code> instance.
     * @return refund amount for given sales invoice item.
     * @throws GenericEntityException
     */
    private static BigDecimal getTotalRefundAmount(String invoiceId, String invoiceItemSeqId, GenericDelegator delegator) throws GenericEntityException {
        BigDecimal totalRefunds = BigDecimal.ZERO;
        long totalRefundsIiNum = 0;

        // find order item corresponding to given sales invoice item
        List<GenericValue> orderItemBillings = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId));
        if (UtilValidate.isEmpty(orderItemBillings)) {
            // not found
            return BigDecimal.ZERO;
        }

        for (GenericValue orderBillingItem : orderItemBillings) {
            List<GenericValue> returnItems = delegator.findByAnd("ReturnItem", UtilMisc.toMap("orderId", orderBillingItem.getString("orderId"), "orderItemSeqId", orderBillingItem.getString("orderItemSeqId")));
            for (GenericValue returnItem : returnItems) {
                List<GenericValue> returnItemBillings = returnItem.getRelated("ReturnItemBilling");
                for (GenericValue returnItemBilling : returnItemBillings) {
                    GenericValue returnInvoice = returnItemBilling.getRelatedOne("Invoice");
                    if ("CUST_RTN_INVOICE".equals(returnInvoice.getString("invoiceTypeId"))) {
                        GenericValue returnInvoiceItem = returnItemBilling.getRelatedOne("InvoiceItem");
                        if ("CRT_PROD_ITEM".equals(returnInvoiceItem.getString("invoiceItemTypeId"))
                         || "CRT_DPROD_ITEM".equals(returnInvoiceItem.getString("invoiceItemTypeId"))
                         || "CRT_FDPROD_ITEM".equals(returnInvoiceItem.getString("invoiceItemTypeId"))
                         || "CRT_PROD_FEATR_ITEM".equals(returnInvoiceItem.getString("invoiceItemTypeId"))
                         || "CRT_SPROD_ITEM".equals(returnInvoiceItem.getString("invoiceItemTypeId"))
                        ) {
                            BigDecimal quantity = returnInvoiceItem.getBigDecimal("quantity");
                            BigDecimal amount = returnInvoiceItem.getBigDecimal("amount");
                            totalRefunds = totalRefunds.add((quantity == null ? BigDecimal.ONE : quantity).multiply((amount == null ? BigDecimal.ZERO : amount)));
                            totalRefundsIiNum++;
                        }
                    }
                }
            }
        }

        return totalRefunds;
    }

    /**
     * Returns total amount of discounts and promotions on given sales invoice item.
     *
     * @param invoiceId sales invoice id
     * @param invoiceItemSeqId sales invoice item id
     * @param delegator <code>GenericDelegator</code> instance
     * @return sales invoice item adjustments.
     * @throws GenericEntityException
     */
    private static BigDecimal getTotalPromoAmount(String invoiceId, String invoiceItemSeqId, GenericDelegator delegator) throws GenericEntityException {
        BigDecimal totalPromotions = BigDecimal.ZERO;

        EntityCondition promoConditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("parentInvoiceId", EntityOperator.EQUALS, invoiceId),
                EntityCondition.makeCondition("parentInvoiceItemSeqId", EntityOperator.EQUALS, invoiceItemSeqId),
                EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.IN, Arrays.asList("ITM_PROMOTION_ADJ", "ITM_DISCOUNT_ADJ")));

        List<GenericValue> promoItems = delegator.findByCondition("InvoiceItem", promoConditions, UtilMisc.toSet("quantity", "amount"), null);
        if (UtilValidate.isNotEmpty(promoItems)) {
            for (GenericValue promoItem : promoItems) {
                BigDecimal quantity = promoItem.getBigDecimal("quantity");
                BigDecimal amount = promoItem.getBigDecimal("amount");
                totalPromotions = totalPromotions.add((quantity == null ? BigDecimal.ONE : quantity).multiply((amount == null ? BigDecimal.ZERO : amount)));
            }
        }

        return totalPromotions;
    }

    /**
     * Lookup store dimension key.
     *
     * @param invoiceId invoice identifier
     * @param invoiceItemSeqId invoice item sequence number
     * @param delegator <code>GenericDelegator</code> instance
     * @return
     *   Store dimension key.
     * @throws GenericEntityException
     */
    private static Long lookupStoreDim(String invoiceId, String invoiceItemSeqId, GenericDelegator delegator) throws GenericEntityException {
        Long notfound = 0L;
        if (invoiceId == null && invoiceItemSeqId == null) {
            return notfound;
        }

        DynamicViewEntity dv = new DynamicViewEntity();
        dv.addMemberEntity("OIB", "OrderItemBilling");
        dv.addMemberEntity("OH", "OrderHeader");
        dv.addAlias("OIB", "invoiceId");
        dv.addAlias("OIB", "invoiceItemSeqId");
        dv.addAlias("OIB", "orderId");
        dv.addAlias("OH", "orderId");
        dv.addAlias("OH", "productStoreId", null, null, null, null, "max");
        dv.addViewLink("OIB", "OH", false, ModelKeyMap.makeKeyMapList("orderId"));

        EntityCondition conditionList = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, invoiceId),
                EntityCondition.makeCondition("invoiceItemSeqId", EntityOperator.EQUALS, invoiceItemSeqId));

        EntityListIterator iter = delegator.findListIteratorByCondition(dv, conditionList, null, UtilMisc.toList("productStoreId"), null, null);
        GenericValue orderStore = EntityUtil.getFirst(iter.getCompleteList());
        iter.close();
        if (orderStore == null) {
            return notfound;
        }

        String productStoreId = orderStore.getString("productStoreId");
        if (UtilValidate.isEmpty(productStoreId)) {
            return notfound;
        }

        GenericValue storeDim = EntityUtil.getFirst(
                delegator.findByCondition("StoreDim", EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId), Arrays.asList("storeDimId"), null)
        );
        if (storeDim == null) {
            return notfound;
        }

        Long storeDimId = storeDim.getLong("storeDimId");
        return storeDimId == null ? notfound : storeDimId;
    }

    /**
     * Wrapper service that run Kettle sales tax transformations and loadTaxInvoiceItemFact service.
     * @param dctx a <code>DispatchContext</code> instance
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> loadSalesTaxData(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
        }
        TimeZone timeZone = (TimeZone) context.get("timeZone");
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }

        String organizationPartyId = (String) context.get("organizationPartyId");
        if (UtilValidate.isEmpty(organizationPartyId)) {
            organizationPartyId = "ALL";
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        Timestamp startedAt = UtilDateTime.nowTimestamp();

        try {
            long rowCount = delegator.findCountByCondition("DateDim", null, null);
            if (rowCount < 2) {
                UtilEtl.setupDateDimension(delegator, timeZone, locale);
            }

            // clean up datamart data
            Debug.logInfo("Clean up dimension and fact tables", MODULE);
            delegator.removeByCondition("StoreDim", EntityCondition.makeCondition("storeDimId", EntityOperator.NOT_EQUAL, null));
            delegator.removeByCondition("TaxAuthorityDim", EntityCondition.makeCondition("taxAuthorityDimId", EntityOperator.NOT_EQUAL, null));
            delegator.removeByCondition("CurrencyDim", EntityCondition.makeCondition("currencyDimId", EntityOperator.NOT_EQUAL, null));
            delegator.removeByCondition("OrganizationDim", EntityCondition.makeCondition("organizationDimId", EntityOperator.NOT_EQUAL, null));
            delegator.removeByCondition("SalesInvoiceItemFact", EntityCondition.makeCondition("dateDimId", EntityOperator.NOT_EQUAL, null));
            delegator.removeByCondition("TaxInvoiceItemFact", EntityCondition.makeCondition("dateDimId", EntityOperator.NOT_EQUAL, null));

            String dataSourceName = delegator.getGroupHelperName("org.ofbiz");
            DataSourceImpl datasource = new DataSourceImpl(dataSourceName);
            new InitialContext().rebind("java:comp/env/jdbc/default_delegator", (DataSource) datasource);

            // run the ETL transformations to load the datamarts
            UtilEtl.runTrans("component://financials/script/etl/load_product_store_dimension.ktr", null);
            UtilEtl.runTrans("component://financials/script/etl/load_tax_authority_dimension.ktr", null);
            UtilEtl.runTrans("component://financials/script/etl/load_organization_dimension.ktr", null);
            UtilEtl.runTrans("component://financials/script/etl/load_sales_invoice_item_fact.ktr", null);
            UtilEtl.runTrans("component://financials/script/etl/load_invoice_level_promotions.ktr", null);
            dispatcher.runSync("financials.loadTaxInvoiceItemFact", UtilMisc.toMap("userLogin", userLogin, "locale", locale));

            new InitialContext().unbind("java:comp/env/jdbc/default_delegator");

            // keep runtime info
            GenericValue transform = delegator.makeValue("DataWarehouseTransform");
            transform.set("transformId", delegator.getNextSeqId("DataWarehouseTransform"));
            transform.set("organizationPartyId", organizationPartyId);
            transform.set("transformEnumId", "SALES_TAX_FACT");
            transform.set("transformTimestamp", startedAt);
            transform.set("userLoginId", userLogin.get("userLoginId"));
            transform.create();

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (NamingException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (KettleException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return results;
    }
}
