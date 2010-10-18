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


<#if inventoryValueByProduct?exists>
<div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
<table>
  <tr>
     <td colspan="2" class="tableheadtext" align="center">${uiLabelMap.FinancialsInventoryValueByProduct} for ${parameters.organizationName?if_exists} (${organizationPartyId})</td>
  </tr>
  <tr>
     <td colspan="2" class="tabletext" align="center">
     <#if customTimePeriod?has_content>
     ${uiLabelMap.CommonFor} ${customTimePeriod.getRelatedOne("PeriodType").get("description")}
     ${customTimePeriod.periodNum} (${getLocalizedDate(customTimePeriod.fromDate, "DATE_ONLY")} ${uiLabelMap.CommonThru} ${getLocalizedDate(customTimePeriod.thruDate, "DATE_ONLY")})
     </#if>
     <#if asOfDate?has_content>
     ${uiLabelMap.OpentapsAsOfDate} ${getLocalizedDate(asOfDate, "DATE")}
     </#if>
  </tr>

<tr>
  <td class="tableheadtext" align="left">${uiLabelMap.ProductProductId}</td>
  <td class="tableheadtext" align="right">${uiLabelMap.FinancialsValue}</td>
  <td class="tableheadtext" align="right">${uiLabelMap.FinancialsCumulativeValue}</td>
</tr>
<#assign total = 0/>
<#list inventoryValueKeys as productId>
<#assign total = total + inventoryValueByProduct(productId)/>
<tr>
  <td class="tabletext"><a href="<@ofbizUrl>/AccountActivitiesDetail?productId=${productId}&organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="linktext">${productId}</a></td>
  <td class="tabletext" align="right"><@ofbizCurrency amount=inventoryValueByProduct(productId) isoCode=currencyUomId/></td>
  <td class="tabletext" align="right"><@ofbizCurrency amount=total isoCode=currencyUomId/></td>
</tr>
</#list>

<tr><td colspan="3"><hr/></td></tr>
<tr>
  <td colspan="2" class="tableheadtext" align="right"><@ofbizCurrency amount=total isoCode=currencyUomId/></td>
</tr>
<tr><td colspan="2">&nbsp;</td></tr>
</table>
</#if>
