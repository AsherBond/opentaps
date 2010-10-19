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

<@import location="component://financials/webapp/financials/includes/commonReportMacros.ftl"/>

<#assign now = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()>
<#assign defaultFromDate = Static["org.ofbiz.base.util.UtilDateTime"].getDayStart(now, timeZone, locale)>
<#assign defaultThruDate = Static["org.ofbiz.base.util.UtilDateTime"].getDayEnd(now, timeZone, locale)>

<@commonReportJs formName="transactionSummaryForm" />

<form method="POST" name="transactionSummaryForm" action="">
  <@inputHidden name="glFiscalTypeId" value="ACTUAL"/>

  <@dateTimeRangeInputRows defaultFromDate=fromDate?default(defaultFromDate) defaultThruDate=thruDate?default(defaultThruDate) />

  <#if !disableTags?exists && tagTypes?has_content>
    <@accountingTagsInputs tagTypes=tagTypes />
  </#if>

  <@isPostedInputRow default=isPosted! />

  <@submitReportOptions reportRequest="JRTransactionSummary" screenRequest="TransactionSummary" returnPage="reportsMain" returnLabel="${uiLabelMap.FinancialsReturnToReports}"/>
 
</form>

<#if report?exists>
<br/>
<table border="0" cellpadding="0">
  <tr>
     <td colspan="5" class="tableheadtext" align="center">
     ${uiLabelMap.FinancialsTransactionSummary} for ${parameters.organizationName?if_exists} (${organizationPartyId})<br/>
     <#if fromDate?has_content>${uiLabelMap.CommonFrom} ${getLocalizedDate(fromDate)}</#if> <#if thruDate?has_content> ${uiLabelMap.CommonThru} ${getLocalizedDate(thruDate)}</#if></td>
  </tr>
   <tr><td colspan="5">&nbsp;</td></tr>
   
<tr>
 <th class="tableheadtext" width="40%" align="left">${uiLabelMap.account}</th>
 <th class="tableheadtext" width="15%" align="right">${uiLabelMap.FinancialsDebitTotal}</th>
 <th class="tableheadtext" width="15%" align="right">${uiLabelMap.FinancialsCreditTotal}</th>
 <th class="tableheadtext" width="15%" align="right">${uiLabelMap.FinancialsDebitNet}</th>
 <th class="tableheadtext" width="15%" align="right">${uiLabelMap.FinancialsCreditNet}</th>
</tr>

<#assign currencyUomId = parameters.orgCurrencyUomId>  <!-- for some reason, putting this in context in main-decorator.bsh does not work -->
<#assign debitTotal=0/>
<#assign creditTotal=0/>
<#assign netDebitTotal=0/>
<#assign netCreditTotal=0/>

<#list report?if_exists as row>
  <tr>
   <td class="tabletext" nowrap="nowrap">  
   <a href="<@ofbizUrl>AccountActivitiesDetail?glAccountId=${row.glAccountId}&organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="linktext">${row.accountCode?if_exists}</a>: ${row.accountName?if_exists} 
   </td>

   <td class="tabletext" align="right">
     <#if row.get("debitSum")?has_content>
     <@ofbizCurrency amount=row.get("debitSum") isoCode=currencyUomId/>
     <#assign debitTotal = debitTotal + row.get("debitSum")>
     </#if>
   </td>
   <td class="tabletext" align="right">
     <#if row.get("creditSum")?has_content>
     <@ofbizCurrency amount=row.get("creditSum") isoCode=currencyUomId/>
     <#assign creditTotal = creditTotal + row.get("creditSum")>
     </#if>
   </td>
   <#assign netDebit = 0>
   <#assign netCredit = 0>
   <#if (row.get("debitSum")?default(0) gt row.get("creditSum")?default(0))>
     <#assign netDebit = row.get("debitSum")?default(0) - row.get("creditSum")?default(0)>
   <#else>
     <#assign netCredit = row.get("creditSum")?default(0) - row.get("debitSum")?default(0)>
   </#if>
   <td class="tabletext" align="right">
     <#if (netDebit gt 0)>
     <@ofbizCurrency amount=netDebit isoCode=currencyUomId/>
     <#assign netDebitTotal = netDebitTotal + netDebit>
     </#if>
   </td>
   <td class="tabletext" align="right">
     <#if (netCredit gt 0)>
     <@ofbizCurrency amount=netCredit isoCode=currencyUomId/>
     <#assign netCreditTotal = netCreditTotal + netCredit>
     </#if>
   </td>
  </tr>
</#list>

  <tr><td colspan="5"><hr/></td></tr>
  <tr>
     <td>&nbsp;</td>
     <td class="tabletext" align="right"><@ofbizCurrency amount=debitTotal isoCode=currencyUomId/></td>
     <td class="tabletext" align="right"><@ofbizCurrency amount=creditTotal isoCode=currencyUomId/></td>
     <td class="tabletext" align="right"><@ofbizCurrency amount=netDebitTotal isoCode=currencyUomId/></td>
     <td class="tabletext" align="right"><@ofbizCurrency amount=netCreditTotal isoCode=currencyUomId/></td>
  </tr>
</table>
</#if>
