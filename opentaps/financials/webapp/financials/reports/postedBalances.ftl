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

<br />

<#if trialBalancesTree?has_content>
<div class="treeViewContainer">
    <div class="treeViewHeader">
      <div class="tableheadtext">${uiLabelMap.FinancialsPostedBalancesByGlAccount}</div>
      <div class="tabletext">${organizationName?if_exists} (${organizationPartyId})</div>
      <div class="tabletext">${uiLabelMap.CommonFor} ${getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp(), "DATE_TIME")}</div>
    </div>
    <div class="tableheadtext" style="margin-top: 20px;">
        <span style="float:left">&nbsp;</span>
        <span style="float:right"><table style="font-weight: bold;" class="twoColumnAmount"><tr><td class="debitAmount">${uiLabelMap.CommonDebit}</td><td class="creditAmount">${uiLabelMap.CommonCredit}</td></tr></table></span>
    </div>
    <hr class="sepbar" style="clear:both"/>
    <@glAccountTree glAccountTree=trialBalancesTree treeId="trialBalance" className="opentaps.GLAccountTree2Col"/>
    <div class="spacer"></div>
    <div class="tableheadtext">
        <span style="float:left">&nbsp;</span>
        <span style="float:right; padding-top: 10px;"><table class="twoColumnAmount"><tr><td class="debitAmount"><@ofbizCurrency amount=trialBalancesTree.getTotalDebit() isoCode=currencyUomId/></td><td class="creditAmount"><@ofbizCurrency amount=trialBalancesTree.getTotalCredit() isoCode=currencyUomId/></td></tr></table></span>
    </div>
</div>
</#if>
