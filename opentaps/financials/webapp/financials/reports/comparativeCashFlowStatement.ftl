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

<#macro listBalances type accounts balances>
   <#assign set1AccountList = set1Accounts.get(type)/>
   <#assign set2AccountList = set2Accounts.get(type)/>
   <#list accounts as account>
     <#if account?has_content>
       <tr>
         <td class="tabletext">${account.accountCode?if_exists}: ${account.accountName?if_exists} (<a href="<@ofbizUrl>AccountActivitiesDetail?glAccountId=${account.glAccountId?if_exists}&organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="buttontext">${account.glAccountId?if_exists}</a>)</td>
         <td class="tabletext" align="right"><@ofbizCurrency amount=set1AccountList.get(account) isoCode=currencyUomId/></td>
         <td class="tabletext" align="right"><@ofbizCurrency amount=set2AccountList.get(account) isoCode=currencyUomId/></td>
         <td class="tabletext" align="right"><@ofbizCurrency amount=balances.get(account) isoCode=currencyUomId/></td>
       </tr>
     </#if>
   </#list>
   <tr><td colspan="4"><hr/></td></tr>
</#macro>

<#macro displaySummary summary amount1 amount2>
      <tr>
        <td class="tableheadtext" align="left">${summary?if_exists}</td>
        <td class="tableheadtext" align="right"><@ofbizCurrency amount=amount1 isoCode=currencyUomId/></td>
        <td class="tableheadtext" align="right"><@ofbizCurrency amount=amount2 isoCode=currencyUomId/></td>
        <td class="tableheadtext" align="right"><@ofbizCurrency amount=amount2-amount1 isoCode=currencyUomId/></td>
      </tr>
      <tr><td>&nbsp;</td></tr>
</#macro>
    
<#if beginningCashAmount1?exists>
  <#assign currencyUomId = parameters.orgCurrencyUomId>  <!-- for some reason, putting this in context in main-decorator.bsh does not work -->
  <div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
  <table>
    <tr>
       <td colspan="2" class="tableheadtext" align="right">${uiLabelMap.FinancialsComparativeCashFlowStatement} for ${parameters.organizationName?if_exists} (${organizationPartyId})</td>
    </tr>
    <tr><td>&nbsp;</td></tr>
    <tr>
      <td class="tableheadtext" align="left">${uiLabelMap.Account}</td>
      <td class="tableheadtext" align="right" style="white-space:nowrap">
        ${getLocalizedDate(fromDate1, "DATE")} - ${getLocalizedDate(thruDate1, "DATE")}<br/>
        (${glFiscalType1.description})
      </td>
      <td class="tableheadtext" align="right" style="white-space:nowrap">
        ${getLocalizedDate(fromDate2, "DATE")} - ${getLocalizedDate(thruDate2, "DATE")}<br/>
        (${glFiscalType2.description})
      <td class="tableheadtext" align="right">${uiLabelMap.OpentapsDifference}</td>
    </tr>
    <tr>
      <td><span class="tableheadtext">${uiLabelMap.FinancialsBeginningCashBalance}</span></td>
      <td align="right"><span class="tableheadtext"><@ofbizCurrency amount=beginningCashAmount1 isoCode=currencyUomId/></span></td>
      <td align="right"><span class="tableheadtext"><@ofbizCurrency amount=beginningCashAmount2 isoCode=currencyUomId/></span></td>
      <td align="right"><span class="tableheadtext"><@ofbizCurrency amount=beginningCashAmount2-beginningCashAmount1 isoCode=currencyUomId/></span></td>
    </tr>
    <tr><td>&nbsp;</td></tr>

    <tr><td class="tableheadtext" align="left">${uiLabelMap.FinancialsOperatingCashFlowAccounts}</td></tr>
    <tr>
      <td class="tabletext">${uiLabelMap.AccountingNetIncome}</td>
      <td align="right" class="tabletext"><@ofbizCurrency amount=netIncome1 isoCode=currencyUomId/></td>
      <td align="right" class="tabletext"><@ofbizCurrency amount=netIncome2 isoCode=currencyUomId/></td>
      <td align="right" class="tabletext"><@ofbizCurrency amount=netIncome2-netIncome1 isoCode=currencyUomId/></td>
    </tr>
   <@listBalances type="operatingCashFlowAccounts" accounts=operatingCashFlowAccounts balances=operatingCashFlowAccountBalances/>
   <@displaySummary summary=uiLabelMap.FinancialsTotalOperatingCashFlow amount1=operatingCashFlow1 amount2=operatingCashFlow2/>
 
   <tr><td class="tableheadtext" align="left">${uiLabelMap.FinancialsInvestingCashFlowAccounts}</td></tr>
   <@listBalances type="investingCashFlowAccounts" accounts=investingCashFlowAccounts balances=investingCashFlowAccountBalances/>
   <@displaySummary summary=uiLabelMap.FinancialsTotalInvestingCashFlow amount1=investingCashFlow1 amount2=investingCashFlow2/>

   <tr><td class="tableheadtext" align="left">${uiLabelMap.FinancialsFinancingCashFlowAccounts}</td></tr>
   <@listBalances type="financingCashFlowAccounts" accounts=financingCashFlowAccounts balances=financingCashFlowAccountBalances/>
   <@displaySummary summary=uiLabelMap.FinancialsTotalFinancingCashFlow amount1=financingCashFlow1 amount2=financingCashFlow2/>

   <tr><td >&nbsp;</td></tr>
   <@displaySummary summary=uiLabelMap.FinancialsTotalNetCashFlow amount1=netCashFlow1 amount2=netCashFlow2/>

   <tr><td >&nbsp;</td></tr>
   <tr>
     <td><span class="tableheadtext">${uiLabelMap.FinancialsEndingCashBalance}</span></td>
     <td align="right"><span class="tableheadtext"><@ofbizCurrency amount=endingCashAmount1 isoCode=currencyUomId/></span></td>
     <td align="right"><span class="tableheadtext"><@ofbizCurrency amount=endingCashAmount2 isoCode=currencyUomId/></span></td>
     <td align="right"><span class="tableheadtext"><@ofbizCurrency amount=endingCashAmount2-endingCashAmount1 isoCode=currencyUomId/></span></td>
   </tr>

</table>
</#if>
