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
<#macro listBalances accounts balances>
    <#list accounts as account>
     <#if account?has_content>
      <tr>
        <td class="tabletext">${account.accountCode?if_exists}: ${account.accountName?if_exists} (<a href="<@ofbizUrl>AccountActivitiesDetail?glAccountId=${account.glAccountId?if_exists}&organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="buttontext">${account.glAccountId?if_exists}</a>)</td>
        <td class="tabletext" align="right"><@ofbizCurrency amount=balances.get(account) isoCode=currencyUomId/></td>
      </tr>
     </#if>
    </#list>
      <tr><td colspan="2"><hr/></td></tr>
</#macro>

<#macro displaySummary summary amount>
      <tr>
        <td class="tableheadtext" align="left">${summary?if_exists}</td>
        <td class="tableheadtext" align="right"><@ofbizCurrency amount=amount isoCode=currencyUomId/></td>
      </tr>
      <tr><td colspan="2">&nbsp;</td></tr>
</#macro>
    
<#if beginningCashAmount?exists>

  <div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
  <div class="treeViewContainer">
    <div class="treeViewHeader">
      <div class="tableheadtext">${uiLabelMap.FinancialsCashFlowStatement} for ${parameters.organizationName?if_exists} (${organizationPartyId})</div>
      <div class="tabletext">
        <#if customTimePeriod?has_content>
          ${uiLabelMap.CommonFrom} ${getLocalizedDate(customTimePeriod.fromDate, "DATE")} ${uiLabelMap.CommonThru} ${getLocalizedDate(customTimePeriod.thruDate, "DATE")}
        </#if>
        ${getLocalizedDate(fromDate, "DATE")} <span class="tableheadtext">Thru</span> ${getLocalizedDate(thruDate, "DATE")}
        <#if isClosed?exists>
          <#if isClosed>
            (${uiLabelMap.FinancialsTimePeriodIsClosed})
            <#else>
              (${uiLabelMap.FinancialsTimePeriodIsNotClosed})
            </#if>
          </#if>
      </div>
    </div>
    <div class="tableheadtext">
      <span style="float:left">${uiLabelMap.FinancialsBeginningCashBalance}</span>
      <span style="float:right"><@ofbizCurrency amount=beginningCashAmount isoCode=orgCurrencyUomId/></span>
    </div>
    <hr class="sepbar" style="clear:both"/>

   <#if glAccountTrees?exists >

     <#assign glAccountTypeId = "operating">
       <div class="tableheadtext" style="margin-top: 20px;">
         <span>${uiLabelMap.FinancialsOperatingCashFlowAccounts}</span>
       </div>
       <div class="tabletext">
         <span style="float:left">${uiLabelMap.AccountingNetIncome}</span>
         <span style="float:right"><@ofbizCurrency amount=netIncome isoCode=currencyUomId/></span>
       </div>
       <hr class="sepbar" style="clear:both"/>
       <@glAccountTree glAccountTree=glAccountTrees.get(glAccountTypeId) treeId=glAccountTypeId/>
       <br/>
       <div class="tableheadtext">
         <span style="float:left">${uiLabelMap.FinancialsTotalOperatingCashFlow}</span>
         <span style="float:right"><@ofbizCurrency amount=operatingCashFlow isoCode=glAccountTrees.get(glAccountTypeId).getCurrencyUomId()/></span>
       </div>
       <hr class="sepbar" style="clear:both"/>

       <#assign glAccountTypeId = "investing">
         <div class="tableheadtext" style="margin-top: 20px;">
           <span>${uiLabelMap.FinancialsInvestingCashFlowAccounts}</span>
         </div>
         <hr class="sepbar" style="clear:both"/>
         <@glAccountTree glAccountTree=glAccountTrees.get(glAccountTypeId) treeId=glAccountTypeId/>
         <br/>
         <div class="tableheadtext">
           <span style="float:left">${uiLabelMap.FinancialsTotalInvestingCashFlow}</span>
           <span style="float:right"><@ofbizCurrency amount=investingCashFlow isoCode=glAccountTrees.get(glAccountTypeId).getCurrencyUomId()/></span>
         </div>
         <hr class="sepbar" style="clear:both"/>

         <#assign glAccountTypeId = "financing">
           <div class="tableheadtext" style="margin-top: 20px;">
             <span>${uiLabelMap.FinancialsFinancingCashFlowAccounts}</span>
           </div>
           <hr class="sepbar" style="clear:both"/>
           <@glAccountTree glAccountTree=glAccountTrees.get(glAccountTypeId) treeId=glAccountTypeId/>
           <br/>
           <div class="tableheadtext">
             <span style="float:left">${uiLabelMap.FinancialsTotalFinancingCashFlow}</span>
             <span style="float:right"><@ofbizCurrency amount=financingCashFlow isoCode=glAccountTrees.get(glAccountTypeId).getCurrencyUomId()/></span>
           </div>
           <hr class="sepbar" style="clear:both"/>

  </#if>
 
    <div class="tableheadtext" style="margin-top: 20px;">
      <span style="float:left">${uiLabelMap.FinancialsTotalNetCashFlow}</span>
      <span style="float:right"><@ofbizCurrency amount=netCashFlow isoCode=orgCurrencyUomId/></span>
    </div>
    <hr class="sepbar" style="clear:both"/>

    <div class="tableheadtext" style="margin-top: 20px;">
      <span style="float:left">${uiLabelMap.FinancialsEndingCashBalance}</span>
      <span style="float:right"><@ofbizCurrency amount=endingCashAmount isoCode=orgCurrencyUomId/></span>
    </div>
    <hr class="sepbar" style="clear:both"/>

  </div>
</#if>
