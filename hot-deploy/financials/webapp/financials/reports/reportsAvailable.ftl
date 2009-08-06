<#--
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
 *  
-->
<#-- TODO: probably some kind of permission checking to see that this userLogin can view such and such reports -->
<div class="tabletext" style="margin-bottom: 30px;">
<p><b>Accounting</b>
<ul class="bulletList">
<li><a href="<@ofbizUrl>PostedBalances</@ofbizUrl>">${uiLabelMap.FinancialsPostedBalancesByGlAccount}</a></li>
<li><a href="<@ofbizUrl>TransactionSummary</@ofbizUrl>">${uiLabelMap.FinancialsTransactionSummary}</a></li>
<li><a href="<@ofbizUrl>AccountActivitiesDetail</@ofbizUrl>">${uiLabelMap.FinancialsAccountActivitiesDetail}</a></li>
</ul>
</p>

<p><b>Financials</b>
<ul class="bulletList">
<li><a href="<@ofbizUrl>TrialBalance</@ofbizUrl>">${uiLabelMap.AccountingTrialBalance}</a></li>
<li><a href="<@ofbizUrl>IncomeStatement</@ofbizUrl>">${uiLabelMap.AccountingIncomeStatement}</a></li>
<li><a href="<@ofbizUrl>ComparativeIncomeStatement</@ofbizUrl>">${uiLabelMap.FinancialsComparativeIncomeStatement}</a></li>
<li><a href="<@ofbizUrl>BalanceSheet</@ofbizUrl>">${uiLabelMap.AccountingBalanceSheet}</a></li>
<li><a href="<@ofbizUrl>ComparativeBalance</@ofbizUrl>">${uiLabelMap.FinancialsComparativeBalanceSheet}</a></li>
<li><a href="<@ofbizUrl>CashFlowStatement</@ofbizUrl>">${uiLabelMap.FinancialsCashFlowStatement}</a></li>
<li><a href="<@ofbizUrl>ComparativeCashFlowStatement</@ofbizUrl>">${uiLabelMap.FinancialsComparativeCashFlowStatement}</a></li>
<li><a href="<@ofbizUrl>BalanceReportSetup</@ofbizUrl>">${uiLabelMap.FinancialsBalanceStatement}</a>
    &nbsp;
    (<a class="linktext" href="<@ofbizUrl>createEncumbranceSnapshotAndDetail</@ofbizUrl>">${uiLabelMap.FinancialsEncumbranceRefresh}</a>)
</li>
<#--
<li>${uiLabelMap.FinancialsEquityStatement}</li>
 -->
</ul>
</p>

<p><b>Inventory</b>
<ul class="bulletList">
<li><a href="<@ofbizUrl>InventoryValuationReport</@ofbizUrl>">${uiLabelMap.FinancialsInventoryValuationReport}</a></li>
<li><a href="<@ofbizUrl>InventoryValueDetail</@ofbizUrl>">${uiLabelMap.FinancialsInventoryValueDetail}</a></li>
<li><a href="<@ofbizUrl>SalesAndInventory</@ofbizUrl>">${uiLabelMap.FinancialsSalesAndInventory}</a></li>
</ul>
</p>

<p><b>Tax</b>
<ul class="bulletList">
<li><a href="<@ofbizUrl>TaxSummary</@ofbizUrl>">${uiLabelMap.AccountingTaxSummary}</a></li>
<li>
    <a href="<@ofbizUrl>SalesTaxReportSetup</@ofbizUrl>">${uiLabelMap.FinancialsSalesTaxStatement}</a>
    &nbsp;
    (<a class="linktext" href="<@ofbizUrl>SalesTaxReportReloadDatamarts</@ofbizUrl>">${uiLabelMap.FinancialsSalesTaxReloadDatamarts}</a>)
</li>
</ul>
</p>

<p><b>${uiLabelMap.FinancialsAnalysis}</b>
<ul class="bulletList">
<li><a href="<@ofbizUrl>GlActivityAnalysisSetup</@ofbizUrl>">${uiLabelMap.FinancialsGlActivityReport}</a></li>
<li><a href="<@ofbizUrl>SalesByStoreByDayReportSetup</@ofbizUrl>">${uiLabelMap.FinancialsSalesByStoreByDayReport}</a></li>
</ul>
</p>

</div>
