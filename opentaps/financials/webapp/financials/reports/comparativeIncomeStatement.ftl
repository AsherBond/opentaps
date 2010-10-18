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

<#if accounts?has_content>
  <#assign currencyUomId = parameters.orgCurrencyUomId>  <!-- for some reason, putting this in context in main-decorator.bsh does not work -->
  <div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
  <table>
    <tr>
       <td colspan="2" class="tableheadtext" align="right">${uiLabelMap.FinancialsComparativeIncomeStatement} for ${parameters.organizationName?if_exists} (${organizationPartyId})</td>
    </tr>
    <tr><td>&nbsp;</td></tr>
    <tr>
      <td class="tableheadtext" align="left">${uiLabelMap.Account}</td>
      <td class="tableheadtext" align="right" style="white-space:nowrap">
        ${getLocalizedDate(fromDate1, "DATE")} - ${getLocalizedDate(thruDate1, "DATE")}<br/>
        ${isClosed1?string(uiLabelMap.FinancialsTimePeriodIsClosed, uiLabelMap.FinancialsTimePeriodIsNotClosed)} (${glFiscalType1.description})
      </td>
      <td class="tableheadtext" align="right" style="white-space:nowrap">
        ${getLocalizedDate(fromDate2, "DATE")} - ${getLocalizedDate(thruDate2, "DATE")}<br/>
        ${isClosed2?string(uiLabelMap.FinancialsTimePeriodIsClosed, uiLabelMap.FinancialsTimePeriodIsNotClosed)} (${glFiscalType2.description})
      <td class="tableheadtext" align="right">${uiLabelMap.OpentapsDifference}</td>
    </tr>
    <#list accounts as account>
      <#if account?has_content>
        <tr>
          <td class="tabletext">${account.accountCode?if_exists}: ${account.accountName?if_exists} (<a href="<@ofbizUrl>AccountActivitiesDetail?glAccountId=${account.glAccountId?if_exists}&organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="buttontext">${account.glAccountId?if_exists}</a>) </td>
          <td class="tabletext" align="right"><@ofbizCurrency amount=set1Accounts.get(account) isoCode=currencyUomId/></td>
          <td class="tabletext" align="right"><@ofbizCurrency amount=set2Accounts.get(account) isoCode=currencyUomId/></td>
          <td class="tabletext" align="right"><@ofbizCurrency amount=accountBalances.get(account) isoCode=currencyUomId/></td>
      </tr>
      </#if>
    </#list>
    <tr><td colspan="4"><hr/></td></tr>
    <tr>
       <td class="tableheadtext">${uiLabelMap.AccountingNetIncome}</td>
       <td class="tabletext" align="right"><@ofbizCurrency amount=netIncome1 isoCode=currencyUomId/></td>
       <td class="tabletext" align="right"><@ofbizCurrency amount=netIncome2 isoCode=currencyUomId/></td>
       <td class="tabletext" align="right"><@ofbizCurrency amount=(netIncome2 - netIncome1) isoCode=currencyUomId/></td>
    </tr>
  </table>
</#if>
