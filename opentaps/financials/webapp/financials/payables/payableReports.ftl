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
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="tabletext" style="margin-bottom: 30px;">

<p class="tableheadtext">${uiLabelMap.AccountingReports}
  <ul class="bulletList">
    <li class="tabletext"><a href="<@ofbizUrl>payablesBalancesReport</@ofbizUrl>">${uiLabelMap.FinancialsPayablesBalancesReport}</a></li>
    <li class="tabletext"><a href="<@ofbizUrl>vendorStatement</@ofbizUrl>">${uiLabelMap.FinancialsVendorStatement}</a></li>
    <li class="tabletext"><a href="<@ofbizUrl>payablesAgingReport</@ofbizUrl>">${uiLabelMap.FinancialsPayablesAgingReport}</a></li>
    <li class="tabletext"><a href="<@ofbizUrl>AverageDSOReportPayables</@ofbizUrl>">${uiLabelMap.FinancialsAverageDSOReportPayables}</a></li>
    <li class="tabletext"><a href="<@ofbizUrl>CommissionReport</@ofbizUrl>">${uiLabelMap.FinancialsCommissionReport}</a></li>
    <#assign reportGroupedList = Static["org.opentaps.common.reporting.UtilReports"].getManagedReports(parameters.componentName, "FIN_PAYABLES", delegator, Static["org.ofbiz.base.util.UtilHttp"].getLocale(request))?default([])/>
    <#list reportGroupedList as reportGroup>
      <#list reportGroup.reports as report>
        <li class="tabletext"><a href="<@ofbizUrl>setupReport?reportId=${report.reportId}</@ofbizUrl>">${report.shortName}</a></li>
      </#list>
    </#list>
  </ul>
</p>
</div>
