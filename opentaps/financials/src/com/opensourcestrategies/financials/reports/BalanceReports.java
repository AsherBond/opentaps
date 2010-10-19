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
package com.opensourcestrategies.financials.reports;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.opensourcestrategies.financials.financials.FinancialServices;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.base.entities.EncumbranceDetail;
import org.opentaps.base.entities.Enumeration;
import org.opentaps.base.entities.EnumerationType;
import org.opentaps.domain.ledger.EncumbranceRepositoryInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * BalanceReports - Services for generating balance reports.
 *
 */
public final class BalanceReports {

    private BalanceReports() { }

    private static String MODULE = BalanceReports.class.getName();

    /**
     * Service to prepare the balanceReport data.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> balanceStatementReport(DispatchContext dctx, Map<String, ?> context) {
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        // get parameters
        String organizationPartyId = (String) context.get("organizationPartyId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");
        // the usage that defines what are the tags used in the report
        // this defaults to BALANCE_REPORTS_TAG (see the service definition)
        String accountingTagUsage = (String) context.get("accountingTagUsage");
        // should the budget amounts include income, this defaults to "N"
        boolean includeBudgetIncome = "Y".equals(context.get("includeBudgetIncome"));

        try {
            Infrastructure infrastructure = new Infrastructure(dispatcher);
            User user = new User(userLogin);
            DomainsLoader dl = new DomainsLoader(infrastructure, user);
            DomainsDirectory dd = dl.loadDomainsDirectory();
            OrganizationRepositoryInterface organizationRepository = dd.getOrganizationDomain().getOrganizationRepository();
            EncumbranceRepositoryInterface encumbranceRepository = dd.getLedgerDomain().getEncumbranceRepository();
            Organization organization = organizationRepository.getOrganizationById(organizationPartyId);

            // retrieve the accounting tags for the organization
            Map<Integer, String> accountingTags = organization.getAccountingTagTypes(accountingTagUsage);

            List<Map<String, Object>> reportData = new FastList<Map<String, Object>>();

            // tag filters for the getIncomeStatementByTagByDates service, defaults to all tags
            Map incomeStatementParams = UtilMisc.toMap("organizationPartyId", organizationPartyId, "glFiscalTypeId", "ACTUAL", "userLogin", userLogin);
            UtilAccountingTags.addTagParameters(context, incomeStatementParams);
            // get the statement by these dates
            incomeStatementParams.put("fromDate", fromDate);
            incomeStatementParams.put("thruDate", thruDate);

            // get actual income statement by tag and put it into report data
            Map actualIncomeStatementParams = new FastMap();
            actualIncomeStatementParams.putAll(incomeStatementParams);
            actualIncomeStatementParams.put("glFiscalTypeId", "ACTUAL");
            actualIncomeStatementParams.put("accountingTagUsage", accountingTagUsage);
            Map<String, Object> actualIncomeStatement = dispatcher.runSync("getIncomeStatementByTagByDates", actualIncomeStatementParams);
            addReportLines(reportData, actualIncomeStatement, "ACTUAL", accountingTags, includeBudgetIncome, delegator);

            // get budget income statement by tag and put it into report data
            Map budgetIncomeStatementParams = new FastMap();
            budgetIncomeStatementParams.putAll(incomeStatementParams);
            budgetIncomeStatementParams.put("glFiscalTypeId", "BUDGET");
            budgetIncomeStatementParams.put("accountingTagUsage", accountingTagUsage);
            Map<String, Object> budgetIncomeStatement = dispatcher.runSync("getIncomeStatementByTagByDates", budgetIncomeStatementParams);
            addReportLines(reportData, budgetIncomeStatement, "BUDGET", accountingTags, includeBudgetIncome, delegator);

            // get last encumbrance snapshot prior to thruDate
            List<EncumbranceDetail> encumbranceDetails = encumbranceRepository.getEncumbranceDetails(organizationPartyId, null, thruDate);
            for (EncumbranceDetail encumbranceDetail : encumbranceDetails) {
                Map account = encumbranceDetail.toMap();
                Map<String, Object> reportLine = getReportLine(reportData, account, accountingTags);
                // add it into liens field
                BigDecimal liens = (BigDecimal) reportLine.get("liens");
                if (encumbranceDetail.getOrderId() != null) {
                    BigDecimal unitAmount = encumbranceDetail.getUnitAmount();
                    BigDecimal encumberedQuantity  = encumbranceDetail.getEncumberedQuantity();
                    if (encumberedQuantity == null || unitAmount == null) {
                        continue;
                    }
                    BigDecimal encumberedValue = unitAmount.multiply(encumberedQuantity);
                    Debug.logInfo("Adding encumberedValue = " + encumberedValue + " to line: " + reportLine, MODULE);
                    reportLine.put("liens", liens.add(encumberedValue));
                }
            }

            // calculate balance column
            calculateBalance(reportData);
            // sort the data
            sortReportData(reportData);
            // substitute all tags ID per there code
            describeTagIds(reportData, organizationRepository);
            // all done
            Map results = ServiceUtil.returnSuccess();
            results.put("reportData", reportData);
            // add the tag descriptions so the report can use them for column labels
            List<EnumerationType> enumerationTypes = organizationRepository.findList(EnumerationType.class, EntityCondition.makeCondition(EnumerationType.Fields.enumTypeId.name(), EntityOperator.IN, accountingTags.values()));
            // group by id for easier access
            Map<String, List<EnumerationType>> grouped = Entity.groupByFieldValues(String.class, enumerationTypes, EnumerationType.Fields.enumTypeId);
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                String tagTypeId = accountingTags.get(i);
                if (UtilValidate.isEmpty(tagTypeId)) {
                    continue;
                }
                // use the tag type id as the column label if no description is found
                String description = tagTypeId;
                if (grouped.containsKey(tagTypeId)) {
                    description = grouped.get(tagTypeId).get(0).getDescription();
                }

                results.put("tagTypeDescription" + i, description);
            }
            return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
    }

    /**
     * A balance report generate method.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code>
     */
    @SuppressWarnings("unchecked")
    public static String jasperBalanceReport(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        if (session == null) {
            UtilMessage.addError(request, "OpentapsError_MissingPaginator");
            return "error";
        }
        String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        if (organizationPartyId == null) {
            UtilMessage.addError(request, "OpentapsError_OrganizationNotSet");
            return "error";
        }

        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request.getSession());
        TimeZone timeZone = UtilCommon.getTimeZone(request);

        String includeBudgetIncome = UtilCommon.getParameter(request, "includeBudgetIncome");
        // get the dates and validate (maybe we can simplify this into a UtilCommon.getTimestampParameter() which does all this)
        String fromDateStr = UtilHttp.makeParamValueFromComposite(request, "fromDate", locale);
        String thruDateStr = UtilHttp.makeParamValueFromComposite(request, "thruDate", locale);
        Timestamp fromDate = null;
        Timestamp thruDate = null;
        if (fromDateStr != null) {
            fromDate = UtilDate.toTimestamp(fromDateStr, timeZone, locale);
        }
        if (thruDateStr != null) {
            thruDate = UtilDate.toTimestamp(thruDateStr, timeZone, locale);
        }
        if (fromDateStr != null && fromDate == null) {
            UtilMessage.addFieldError(request, "fromDate", "OpentapsFieldError_BadDateFormat", UtilMisc.toMap("format", UtilDateTime.getDateFormat(locale)));
            return "error";
        }
        if (thruDateStr != null && thruDate == null) {
            UtilMessage.addFieldError(request, "thruDate", "OpentapsFieldError_BadDateFormat", UtilMisc.toMap("format", UtilDateTime.getDateFormat(locale)));
            return "error";
        }
        String currencyUomId = UtilCommon.getParameter(request, "currencyUomId");

        try {
            DomainsLoader dl = new DomainsLoader(request);
            DomainsDirectory dd = dl.loadDomainsDirectory();
            OrganizationRepositoryInterface organizationRepository = dd.getOrganizationDomain().getOrganizationRepository();
            Organization organization = organizationRepository.getOrganizationById(organizationPartyId);

            // get the data
            Map<String, Object> ctxt = new FastMap<String, Object>();
            ctxt.put("userLogin", userLogin);
            ctxt.put("organizationPartyId", organizationPartyId);
            ctxt.put("includeBudgetIncome", includeBudgetIncome);
            ctxt.put("fromDate", fromDate);
            ctxt.put("thruDate", thruDate);
            UtilAccountingTags.addTagParameters(request, ctxt);
            Map<String, Object> results = dispatcher.runSync("balanceStatementReport", ctxt);
            if (!UtilCommon.isSuccess(results)) {
                return UtilMessage.createAndLogEventError(request, results, locale, MODULE);
            }

            List<Map<String, Object>> reportData = (List<Map<String, Object>>) results.get("reportData");
            JRMapCollectionDataSource datasource = new JRMapCollectionDataSource(reportData);
            request.setAttribute("jrDataSource", datasource);

            // pass general values to the report
            Map<String, Object> jrParameters = new FastMap<String, Object>();
            jrParameters.put("organizationName", organization.getName());
            jrParameters.put("organizationPartyId", organizationPartyId);

            jrParameters.put("fromDate", fromDate);
            jrParameters.put("thruDate", thruDate);
            jrParameters.put("currencyUomId", currencyUomId);

            // add the column labels
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                String description = (String) results.get("tagTypeDescription" + i);
                if (description == null) {
                    description = "";
                }

                jrParameters.put("tagTypeDescription" + i, description);
            }

            request.setAttribute("jrParameters", jrParameters);

        } catch (GeneralException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return "success";
    }

    /**
     * Adds the value from an incomeStatement results to the report lines.
     * @param reportData the current data in the report
     * @param incomeStatement a <code>Map</code> result of the getIncomeStatementByTagByDates service call
     * @param glFiscalTypeId the fiscal type of the incomeStatement results being added
     * @param accountingTags the <code>Map</code> of accounting tags in the report
     * @param includeBudgetIncome if the Budget amount calculation should also include income
     * @param delegator a <code>Delegator</code> value
     * @throws GenericEntityException if error occur
     */
    @SuppressWarnings("unchecked")
    private static void addReportLines(List<Map<String, Object>> reportData, Map<String, Object> incomeStatement, String glFiscalTypeId, Map<Integer, String> accountingTags, boolean includeBudgetIncome, Delegator delegator) throws GenericEntityException {
        List<String> glAccountIncomeStatementTypes = new ArrayList(FinancialServices.INCOME_STATEMENT_TYPES);
        glAccountIncomeStatementTypes.add(FinancialServices.UNCLASSIFIED_TYPE);
        Map glAccountSums = (Map) incomeStatement.get("glAccountSums");
        // iterate all glAccountTypeId
        for (String glAccountTypeId : glAccountIncomeStatementTypes) {
            // Budget lines only includes the expenses types
            if (!includeBudgetIncome && glFiscalTypeId.equals("BUDGET")) {
                if (!FinancialServices.EXPENSES_TYPES.contains(glAccountTypeId)) {
                    continue;
                }
            }
            List<Map> accountList = (List) glAccountSums.get(glAccountTypeId);
            for (Map account : accountList) {
                // find a report line matching the account tag set, or get a new line
                Map<String, Object> reportLine = getReportLine(reportData, account, accountingTags);
                // add the account sum to the corresponding field, according to the fiscal type and glAccount type
                BigDecimal accountSum = (BigDecimal) account.get("accountSum");
                BigDecimal budget = (BigDecimal) reportLine.get("budget");
                BigDecimal income = (BigDecimal) reportLine.get("income");
                BigDecimal expense = (BigDecimal) reportLine.get("expense");
                Debug.logInfo("Adding incomeStatement data: " + account + " to line: " + reportLine, MODULE);
                if (glFiscalTypeId.equals("BUDGET")) {
                    // if is budget then add accountSum to budget column
                    Debug.logInfo("Adding to BUDGET: " + accountSum, MODULE);
                    reportLine.put("budget", budget.add(accountSum));
                } else {
                    // else add according to the glAccount type
                    String glAccountId = (String) account.get("glAccountId");
                    GenericValue glAccount = delegator.findByPrimaryKeyCache("GlAccount", UtilMisc.toMap("glAccountId", glAccountId));
                    if (UtilAccounting.isRevenueAccount(glAccount) || UtilAccounting.isIncomeAccount(glAccount)) {
                        // if this glAccount is revenue or income then add it into income column
                        Debug.logInfo("Adding to INCOME: " + accountSum, MODULE);
                        reportLine.put("income", income.add(accountSum));
                    } else if (UtilAccounting.isExpenseAccount(glAccount)) {
                        // else then add it into expense column, note that the amount is negative, so we change the sign first
                        Debug.logInfo("Adding to EXPENSE: " + accountSum.negate(), MODULE);
                        reportLine.put("expense", expense.add(accountSum.negate()));
                    }
                }
            }
        }
    }

    /**
     * Gets a report line from report data by glAccountId, if no matching line exists then returns a new line.
     * A report line is a <code>Map</code> containing:
     *  - "budget"  -> value
     *  - "income"  -> value
     *  - "expense" -> value
     *  - "liens"   -> value
     *  - "balance" -> value
     *  - for each "tagName" -> a tag value
     * @param reportData a <code>List<Map<String, Object>></code> value
     * @param accountingTagsValues a <code>Map</code> of accounting tag name to accounting tag value
     * @param accountingTags the <code>Map</code> of accounting tag index to accounting tag type
     * @return the <code>Map<String, Object></code> report line
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getReportLine(List<Map<String, Object>> reportData, Map accountingTagsValues, Map accountingTags) {
        // find report line by tags
        for (Map<String, Object> reportLine : reportData) {
            boolean lineMatch = true;
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                String tag = (String) accountingTags.get(new Integer(i));
                String tagName = UtilAccountingTags.ENTITY_TAG_PREFIX + i;
                if (tag != null && !UtilObject.equalsHelper(reportLine.get(tagName), accountingTagsValues.get(tagName))) {
                    // if any tags not matched, then break
                    Debug.logInfo("getReportLine: mismatch for tagName: " + tagName + ", reportLine value = " + reportLine.get(tagName) + ", account value = " + accountingTagsValues.get(tagName), MODULE);
                    lineMatch = false;
                    break;
                }
            }
            // line did not match, check the next one
            if (!lineMatch) {
                continue;
            }
            // else this line matched
            Debug.logInfo("getReportLine: found matching report line: " + reportLine, MODULE);
            return reportLine;
        }
        // cannot find match line, then create a new map
        Map<String, Object> reportLine = new FastMap<String, Object>();
        reportLine.put("budget", BigDecimal.ZERO);
        reportLine.put("income", BigDecimal.ZERO);
        reportLine.put("expense", BigDecimal.ZERO);
        reportLine.put("liens", BigDecimal.ZERO);
        reportLine.put("balance", BigDecimal.ZERO);
        // put the accounting tags into the new line
        for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
            String tag = (String) accountingTags.get(new Integer(i));
            String tagName = UtilAccountingTags.ENTITY_TAG_PREFIX + i;
            if (tag != null && accountingTagsValues.get(tagName) != null) {
                reportLine.put(tagName, accountingTagsValues.get(tagName));
            }
        }
        reportData.add(reportLine);
        Debug.logInfo("getReportLine: no matching line found, created new report line: " + reportLine, MODULE);
        return reportLine;
    }

    /**
     * Calculates and set each line balance field value, balance = budget + income - expense - liens.
     * @param reportData a <code>List<Map<String, Object>></code> value
     */
    private static void calculateBalance(List<Map<String, Object>> reportData) {
        for (Map<String, Object> reportLine : reportData) {
            // balance = budget + income - expense - liens
            BigDecimal budget = (BigDecimal) reportLine.get("budget");
            BigDecimal income = (BigDecimal) reportLine.get("income");
            BigDecimal expense = (BigDecimal) reportLine.get("expense");
            BigDecimal liens = (BigDecimal) reportLine.get("liens");
            reportLine.put("balance", budget.add(income).subtract(expense).subtract(liens));
        }
    }

    /**
     * Sorts the report data according to the tag values.
     * @param reportData a <code>List<Map<String, Object>></code> value
     */
    private static void sortReportData(List<Map<String, Object>> reportData) {
        Comparator<Map<String, Object>> reportDataComparator = new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                // compare the tags in order
                for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                    String tagName = UtilAccountingTags.ENTITY_TAG_PREFIX + i;
                    String tagA = (String) a.get(tagName);
                    String tagB = (String) b.get(tagName);
                    // consider null tags as empty strings
                    if (tagA == null) {
                        tagA = "";
                    }
                    if (tagB == null) {
                        tagB = "";
                    }

                    int c = tagA.compareTo(tagB);

                    // if a tag value differed, return the compare result
                    if (c != 0) {
                        return c;
                    }
                    // else just test the next tag
                }
                // all the tags where equal, this should not have happened
                Debug.logError("reportDataComparator: all tags are equal between [" + a + "] and [" + b + "] !", MODULE);
                return 0;
            }
        };
        Collections.sort(reportData, reportDataComparator);
    }

    /**
     * Replaces the accounting tag IDs per their corresponding code.
     * @param reportData a <code>List<Map<String, Object>></code> value
     * @param repository an <code>OrganizationRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    private static void describeTagIds(List<Map<String, Object>> reportData, OrganizationRepositoryInterface repository) throws RepositoryException {
        // get the list of tags
        List<EnumerationType> enumerationTypes = repository.findList(EnumerationType.class, repository.map(EnumerationType.Fields.parentTypeId, "ACCOUNTING_TAG"), Arrays.asList(EnumerationType.Fields.enumTypeId.asc()));
        List<Enumeration> enumerations = repository.findList(Enumeration.class, EntityCondition.makeCondition(Enumeration.Fields.enumTypeId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(enumerationTypes, EnumerationType.Fields.enumTypeId)), Arrays.asList(Enumeration.Fields.sequenceId.asc()));
        // group by id for easier access
        Map<String, List<Enumeration>> grouped = Entity.groupByFieldValues(String.class, enumerations, Enumeration.Fields.enumId);
        // find parent enumerations of the primary tag
        List<Enumeration> parents = repository.findList(Enumeration.class, EntityCondition.makeCondition(Enumeration.Fields.enumId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(enumerations, Enumeration.Fields.parentEnumId)));
        // group those parents by id for easier lookup
        Map<String, List<Enumeration>> groupedParents = Entity.groupByFieldValues(String.class, parents, Enumeration.Fields.enumId);

        for (Map<String, Object> data : reportData) {
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                String tagName = UtilAccountingTags.ENTITY_TAG_PREFIX + i;
                String tag = (String) data.get(tagName);
                if (UtilValidate.isEmpty(tag)) {
                    continue;
                }

                if (grouped.containsKey(tag)) {
                    Enumeration enumeration = grouped.get(tag).get(0);
                    String code = enumeration.getEnumCode();
                    // for the primary tag, insert the parent description if it has one
                    if (i == 1) {
                        if (groupedParents.containsKey(enumeration.getParentEnumId())) {
                            String parent = groupedParents.get(enumeration.getParentEnumId()).get(0).getDescription();
                            data.put("parent", parent);
                        }
                    }
                    data.put(tagName, code);
                } else {
                    Debug.logError("Tag ID [" + tag + "] not found in the list of Accounting tags Enumerations", MODULE);
                }
            }
        }
    }
}
