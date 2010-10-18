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
      <div class="tableheadtext">${uiLabelMap.AccountingTrialBalance} for ${parameters.organizationName?if_exists} (${organizationPartyId})</div>
      <div class="tabletext">
        <#if customTimePeriod?has_content>
          ${uiLabelMap.CommonFor} ${customTimePeriod.getRelatedOne("PeriodType").get("description")}
          ${customTimePeriod.periodNum} (${getLocalizedDate(customTimePeriod.fromDate, "DATE_ONLY")} ${uiLabelMap.CommonThru} ${getLocalizedDate(customTimePeriod.thruDate, "DATE_ONLY")})
        </#if>
        <#if asOfDate?has_content>
          ${uiLabelMap.OpentapsAsOfDate} ${getLocalizedDate(asOfDate, "DATE")}
        </#if>
      </div>
    </div>

  <#macro displayGlAccountTree accountTree headerLabel="" id="">

    <div class="tableheadtext" style="margin-top: 20px;">
      <span style="float:left">${headerLabel}</span>
      <span style="float:right"><table style="font-weight: bold;" class="twoColumnAmount"><tr><td class="debitAmount">${uiLabelMap.CommonDebit}</td><td class="creditAmount">${uiLabelMap.CommonCredit}</td></tr></table></span>
    </div>
    <hr class="sepbar" style="clear:both"/>

    <@glAccountTree glAccountTree=accountTree treeId=id className="opentaps.GLAccountTree2Col"/>

    <br/>
  </#macro>

    <@displayGlAccountTree accountTree=assetAccountBalancesTree headerLabel="${uiLabelMap.AccountingAssets}" id="assetAccountBalancesTree"/>
    <@displayGlAccountTree accountTree=liabilityAccountBalancesTree headerLabel="${uiLabelMap.AccountingLiabilities}" id="liabilityAccountBalancesTree"/>
    <@displayGlAccountTree accountTree=equityAccountBalancesTree headerLabel="${uiLabelMap.AccountingEquities}" id="equityAccountBalancesTree"/>
    <@displayGlAccountTree accountTree=revenueAccountBalancesTree headerLabel="${uiLabelMap.FinancialsRevenue}" id="revenueAccountBalancesTree"/>
    <@displayGlAccountTree accountTree=expenseAccountBalancesTree headerLabel="${uiLabelMap.FinancialsExpense}" id="expenseAccountBalancesTree"/>
    <@displayGlAccountTree accountTree=incomeAccountBalancesTree headerLabel="${uiLabelMap.FinancialsIncome}" id="incomeAccountBalancesTree"/>
    <@displayGlAccountTree accountTree=otherAccountBalancesTree headerLabel="${uiLabelMap.CommonOther}" id="otherAccountBalancesTree"/>

    <hr class="sepbar" style="clear:both"/>
    <div class="spacer"></div>
    <div class="tableheadtext">
        <span style="float:left">${uiLabelMap.FinancialsTotals}</span>
        <span style="float:right;"><table class="twoColumnAmount"><tr><td class="debitAmount"><@ofbizCurrency amount=totalDebits isoCode=currencyUomId/></td><td class="creditAmount"><@ofbizCurrency amount=totalCredits isoCode=currencyUomId/></td></tr></table></span>
    </div>
    <br/>
    <br/>

  </div>

</#if>

