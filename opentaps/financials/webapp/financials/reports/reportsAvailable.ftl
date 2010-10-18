<#--
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
 *  
-->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- TODO: probably some kind of permission checking to see that this userLogin can view such and such reports -->
<div class="tabletext" style="margin-bottom: 30px;">

<table style="width: 100%;">
<tr>
  <td style="vertical-align: top; width: 35%;">

    <p><b>${uiLabelMap.AccountingAccounting}</b>
    <ul class="bulletList">
    <li><a href="<@ofbizUrl>PostedBalances</@ofbizUrl>">${uiLabelMap.FinancialsPostedBalancesByGlAccount}</a></li>
    <li><a href="<@ofbizUrl>TransactionSummary</@ofbizUrl>">${uiLabelMap.FinancialsTransactionSummary}</a></li>
    <li><a href="<@ofbizUrl>AccountActivitiesDetail</@ofbizUrl>">${uiLabelMap.FinancialsAccountActivitiesDetail}</a></li>
    </ul>
    </p>

    <@displayReportGroup group="FIN_FINANCIALS" nameOnly=true >
    <li><a href="<@ofbizUrl>TrialBalance</@ofbizUrl>">${uiLabelMap.AccountingTrialBalance}</a></li>
    <li><a href="<@ofbizUrl>IncomeStatement</@ofbizUrl>">${uiLabelMap.AccountingIncomeStatement}</a></li>
    <li><a href="<@ofbizUrl>ComparativeIncomeStatement</@ofbizUrl>">${uiLabelMap.FinancialsComparativeIncomeStatement}</a></li>
    <li><a href="<@ofbizUrl>BalanceSheet</@ofbizUrl>">${uiLabelMap.AccountingBalanceSheet}</a></li>
    <li><a href="<@ofbizUrl>ComparativeBalance</@ofbizUrl>">${uiLabelMap.FinancialsComparativeBalanceSheet}</a></li>
    <li><a href="<@ofbizUrl>CashFlowStatement</@ofbizUrl>">${uiLabelMap.FinancialsCashFlowStatement}</a></li>
    <li><a href="<@ofbizUrl>ComparativeCashFlowStatement</@ofbizUrl>">${uiLabelMap.FinancialsComparativeCashFlowStatement}</a></li>
    </@displayReportGroup>

  </td>
  <td style="vertical-align: top;">
 
    <p><b>${uiLabelMap.ProductInventory}</b>
    <ul class="bulletList">
    <li><a href="<@ofbizUrl>InventoryValuationReport</@ofbizUrl>">${uiLabelMap.FinancialsInventoryValuationReport}</a></li>
    <li><a href="<@ofbizUrl>InventoryValueDetail</@ofbizUrl>">${uiLabelMap.FinancialsInventoryValueDetail}</a></li>
    <li><a href="<@ofbizUrl>SalesAndInventory</@ofbizUrl>">${uiLabelMap.FinancialsSalesAndInventory}</a></li>
    </ul>
    </p>

    <p><b>${uiLabelMap.FinancialsTax}</b>
    <#assign transformTimestamp = Static["org.opentaps.common.reporting.UtilReports"].getTrasformationTimeByType("SALES_TAX_FACT", delegator)?if_exists />
    <br/><i><#if transformTimestamp?has_content>${uiLabelMap.OpentapsDataAsOf}&nbsp;<@displayDate date=transformTimestamp/><#else>No data</#if></i>
    &nbsp;(<a class="linktext" href="<@ofbizUrl>SalesTaxReportReloadDatamarts</@ofbizUrl>">${uiLabelMap.FinancialsSalesTaxReloadDatamarts}</a>)
    <ul class="bulletList">
    <li><a href="<@ofbizUrl>TaxSummary</@ofbizUrl>">${uiLabelMap.AccountingTaxSummary}</a></li>
    <li><a href="<@ofbizUrl>SalesTaxReportSetup</@ofbizUrl>">${uiLabelMap.FinancialsSalesTaxStatement}</a></li>
    </ul>
    </p>

    <p><b>${uiLabelMap.FinancialsAnalysis}</b>
    <ul class="bulletList">
    <li><a href="<@ofbizUrl>GlActivityAnalysisSetup</@ofbizUrl>">${uiLabelMap.FinancialsGlActivityReport}</a></li>
    <li><a href="<@ofbizUrl>SalesByStoreByDayReportSetup</@ofbizUrl>">${uiLabelMap.FinancialsSalesByStoreByDayReport}</a></li>
    </ul>
    </p>

  </td>
</tr>

<tr>
  <td style="vertical-align: top;">

    <@displayReportGroup group="FIN_BUDGETING" runtimeData="ENCUMB_GL_ENTRY" updater="CreateGlAccountTransEntryFacts" nameOnly=true/>

  </td>
  <td style="vertical-align: top;">

    <@displayReportGroup group="FIN_CONFIGURATION" nameOnly=true />

  </td>  
</tr>

</table>

</div>

