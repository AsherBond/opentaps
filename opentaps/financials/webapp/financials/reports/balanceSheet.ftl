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

<#if assetAccountBalancesTree?exists >
  <div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
  <div class="treeViewContainer">
    <div class="treeViewHeader">
      <div class="tableheadtext">${uiLabelMap.AccountingBalanceSheet} for ${parameters.organizationName?if_exists} (${organizationPartyId})</div>
      <div class="tabletext">
        <#if customTimePeriod?has_content>
          ${uiLabelMap.CommonFor} ${customTimePeriod.getRelatedOne("PeriodType").get("description")}
          ${customTimePeriod.periodNum} (${getLocalizedDate(customTimePeriod.fromDate, "DATE_ONLY")} ${uiLabelMap.CommonThru} ${getLocalizedDate(customTimePeriod.thruDate, "DATE_ONLY")})
        </#if>
        <#if asOfDate?has_content>
          ${uiLabelMap.OpentapsAsOfDate} ${getLocalizedDate(asOfDate, "DATE")}
        </#if>
        <#if isClosed?exists>
          <#if isClosed>
            (${uiLabelMap.FinancialsTimePeriodIsClosed})
          <#else>
            (${uiLabelMap.FinancialsTimePeriodIsNotClosed})
          </#if>
        </#if>
      </div>
    </div>

    <div class="tableheadtext" style="margin-top: 20px;">
      <span style="float:left">${uiLabelMap.AccountingAssets}</span>
      <span style="float:right"><@ofbizCurrency amount=assetAccountBalancesTree.getTotalBalance() isoCode=assetAccountBalancesTree.getCurrencyUomId()/></span>
    </div>
    <hr class="sepbar" style="clear:both"/>
    <@glAccountTree glAccountTree=assetAccountBalancesTree treeId="assetAccountBalancesTree"/>

    <div class="tableheadtext" style="margin-top: 20px;">
      <span style="float:left">${uiLabelMap.AccountingLiabilities}</span>
      <span style="float:right"><@ofbizCurrency amount=liabilityAccountBalancesTree.getTotalBalance() isoCode=liabilityAccountBalancesTree.getCurrencyUomId()/></span>
    </div>
    <hr class="sepbar" style="clear:both"/>
    <@glAccountTree glAccountTree=liabilityAccountBalancesTree treeId="liabilityAccountBalancesTree"/>

    <div class="tableheadtext" style="margin-top: 20px;">
      <span style="float:left">${uiLabelMap.AccountingEquities}</span>
      <span style="float:right"><@ofbizCurrency amount=equityAccountBalancesTree.getTotalBalance() isoCode=equityAccountBalancesTree.getCurrencyUomId()/></span>
    </div>
    <hr class="sepbar" style="clear:both"/>
    <@glAccountTree glAccountTree=equityAccountBalancesTree treeId="equityAccountBalancesTree"/>

  </div>

</#if>

