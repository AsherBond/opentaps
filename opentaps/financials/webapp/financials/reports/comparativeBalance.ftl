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

<#assign currencyUomId = parameters.orgCurrencyUomId>  <!-- for some reason, putting this in context in main-decorator.bsh does not work -->
<#macro listBalances type accounts balances>
  <#assign total = 0.0>
  <#assign fromDateAccounts = fromDateAccountBalances.get(type)/>
  <#assign thruDateAccounts = thruDateAccountBalances.get(type)/>
    <#list accounts as account>
     <#if account?has_content>
      <tr>
        <td class="tabletext">${account.accountCode?if_exists}: ${account.accountName?if_exists} (<a href="<@ofbizUrl>AccountActivitiesDetail?glAccountId=${account.glAccountId?if_exists}&organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="buttontext">${account.glAccountId?if_exists}</a>) </td>
        <td class="tabletext" align="right"><@ofbizCurrency amount=fromDateAccounts.get(account) isoCode=currencyUomId/></td>
        <td class="tabletext" align="right"><@ofbizCurrency amount=thruDateAccounts.get(account) isoCode=currencyUomId/></td>
        <td class="tabletext" align="right"><@ofbizCurrency amount=balances.get(account) isoCode=currencyUomId/></td>
      </tr>
      <#assign total = total + balances.get(account)>
     </#if>
    </#list>
      <tr><td colspan="4"><hr/></td></tr>
      <tr>
        <td colspan="4" class="tableheadtext" align="right"><@ofbizCurrency amount=total isoCode=currencyUomId/></td>
      </tr>
      <tr><td colspan="4">&nbsp;</td></tr>
</#macro>

<#if assetAccounts?has_content || liabilityAccounts?has_content || equityAccounts?has_content>
<div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
<div class="tabletext">
<table>
   <tr>
      <td colspan="4" class="tableheadtext" align="center">${uiLabelMap.FinancialsComparativeBalanceSheet} for ${parameters.organizationName?if_exists} (${organizationPartyId})</td>
   </tr>
   <tr><td colspan="4">&nbsp;</td></tr>
   <tr>
     <td class="tableheadtext" align="left">${uiLabelMap.Account}</td>
     <td class="tableheadtext" align="right" style="white-space:nowrap">
       ${getLocalizedDate(fromDate, "DATE")}<br/>
       (${fromGlFiscalType.description})
     </td>
     <td class="tableheadtext" align="right" style="white-space:nowrap">
       ${getLocalizedDate(thruDate, "DATE")}<br/>
       (${toGlFiscalType.description})
     <td class="tableheadtext" align="right">${uiLabelMap.OpentapsDifference}</td>
   </tr>
   <tr><td class="tableheadtext" align="left">${uiLabelMap.AccountingAssets}</td></tr>
   <@listBalances type="assetAccountBalances" accounts=assetAccounts balances=assetAccountBalances/>
   
   <tr><td class="tableheadtext" align="left">${uiLabelMap.AccountingLiabilities}</td></tr>
   <@listBalances type="liabilityAccountBalances" accounts=liabilityAccounts balances=liabilityAccountBalances/>

   <tr><td class="tableheadtext" align="left">${uiLabelMap.AccountingEquities}</td></tr>
   <@listBalances type="equityAccountBalances" accounts=equityAccounts balances=equityAccountBalances/>
</table>
</#if>
