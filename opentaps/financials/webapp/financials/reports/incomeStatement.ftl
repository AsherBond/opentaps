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

<#if glAccountSums?has_content && glAccountTrees?exists >
  <div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
  <div class="treeViewContainer">
    <div class="treeViewHeader">
      <div class="tableheadtext">${uiLabelMap.AccountingIncomeStatement} for ${parameters.organizationName?if_exists} (${organizationPartyId})</div>
      <div class="tabletext">${uiLabelMap.CommonFrom} ${getLocalizedDate(fromDate, "DATE")} ${uiLabelMap.CommonThru} ${getLocalizedDate(thruDate, "DATE")}
        <#if isClosed>
          (${uiLabelMap.FinancialsTimePeriodIsClosed})
        <#else>
          (${uiLabelMap.FinancialsTimePeriodIsNotClosed})
        </#if>
      </div>
    </div>
    <#list glAccountTrees.keySet() as glAccountTypeId>
      <#assign glAccountType = delegator.findByPrimaryKeyCache("GlAccountType", Static["org.ofbiz.base.util.UtilMisc"].toMap("glAccountTypeId", glAccountTypeId))?default({})>
      <div style="clear:both"></div>
      <table style="width:100%; margin-top: 15px;">
        <tbody>
          <tr>
            <td class="tableheadtext">${glAccountType.description?default(uiLabelMap.FinancialsUnclassifiedAccounts)}</td>
            <td class="tableheadtext" style="text-align:right;padding:0; margin:0;"><@ofbizCurrency amount=glAccountTrees.get(glAccountTypeId).getTotalBalance() isoCode=glAccountTrees.get(glAccountTypeId).getCurrencyUomId()/></td>
          </tr>
        </tbody>
      </table>
      <hr class="sepbar"/>
      <@glAccountTree glAccountTree=glAccountTrees.get(glAccountTypeId) treeId=glAccountTypeId/>

      <#if glAccountTypeId == "COGS">
        <div style="clear:both"></div>
        <table style="width:100%; margin-top: 15px;">
          <tbody>
            <tr>
              <td class="tableheadtext">${uiLabelMap.FinancialsGrossProfit}</td>
              <td class="tableheadtext" style="text-align:right;padding:0; margin:0;"><@ofbizCurrency amount=grossProfit isoCode=orgCurrencyUomId/></td>
            </tr>
          </tbody>
        </table>
        <hr class="sepbar"/>
      <#elseif glAccountTypeId == "OPERATING_EXPENSE">
        <div style="clear:both"></div>
        <table style="width:100%; margin-top: 15px;">
          <tbody>
            <tr>
              <td class="tableheadtext">${uiLabelMap.FinancialsOperatingIncome}</td>
              <td class="tableheadtext" style="text-align:right;padding:0; margin:0;"><@ofbizCurrency amount=operatingIncome isoCode=orgCurrencyUomId/></td>
            </tr>
          </tbody>
        </table>
        <hr class="sepbar"/>
      <#elseif glAccountTypeId == "OTHER_INCOME">
        <div style="clear:both"></div>
        <table style="width:100%; margin-top: 15px;">
          <tbody>
            <tr>
              <td class="tableheadtext">${uiLabelMap.FinancialsPretaxIncome}</td>
              <td class="tableheadtext" style="text-align:right;padding:0; margin:0;"><@ofbizCurrency amount=pretaxIncome isoCode=orgCurrencyUomId/></td>
            </tr>
          </tbody>
        </table>
        <hr class="sepbar"/>
      </#if>
    </#list>

      <div style="clear:both"></div>
      <table style="width:100%; margin-top: 15px;">
        <tbody>
          <tr>
            <td class="tableheadtext">${uiLabelMap.AccountingNetIncome}</td>
            <td class="tableheadtext" style="text-align:right;padding:0; margin:0;"><@ofbizCurrency amount=netIncome isoCode=orgCurrencyUomId/></td>
          </tr>
        </tbody>
      </table>
      <hr class="sepbar"/>

  </div>
</#if>
