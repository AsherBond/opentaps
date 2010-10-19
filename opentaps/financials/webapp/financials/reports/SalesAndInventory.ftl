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

<style type="text/css">
table.salesAndInventoryReport {
  width: 100%;
  border: 1px solid black;
  border-collapse: collapse;
  font: 10px Verdana, Arial, Helvetica, sans-serif;
}
table.salesAndInventoryReport td {
  border: 1px solid black;
  padding: 3px;
}
.rowGray {
  background-color: #eee;
}
.rowWhite {
  background-color: white;
}
</style>

<#if report?exists>

<table class="salesAndInventoryReport">
  <tr>
    <td class="tableheadtext">${uiLabelMap.ProductProductId}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsSalesVolume}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsCOGS}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsGrossProfit}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsInventoryStart}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsInventoryEnd}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsAverageInventoryValue}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsInventoryTurnover}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsAnnualizedInventoryTurnover}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsProfitabilityRatio}</td>
  </tr>

<#assign counter = 0>
<#list report as row>

  <#if (counter % 2 == 0)>
    <#assign tdclass = "rowGray"/>
  <#else>
    <#assign tdclass = "rowWhite"/>
  </#if>

  <tr>
    <td class="${tdclass}">${row.productId}</td>
    <td class="${tdclass}" align="right"><@ofbizCurrency amount=row.salesSum isoCode=parameters.orgCurrencyUomId/></td>
    <td class="${tdclass}" align="right"><@ofbizCurrency amount=row.cogsSum isoCode=parameters.orgCurrencyUomId/></td>
    <td class="${tdclass}" align="right"><@ofbizCurrency amount=row.grossProfit isoCode=parameters.orgCurrencyUomId/></td>
    <td class="${tdclass}" align="right"><@ofbizCurrency amount=row.inventorySumStart isoCode=parameters.orgCurrencyUomId/></td>
    <td class="${tdclass}" align="right"><@ofbizCurrency amount=row.inventorySumEnd isoCode=parameters.orgCurrencyUomId/></td>
    <td class="${tdclass}" align="right"><@ofbizCurrency amount=row.inventoryAverage isoCode=parameters.orgCurrencyUomId/></td>
    <td class="${tdclass}" align="right">${row.inventoryTurnover?default("")}</td>
    <td class="${tdclass}" align="right">${row.annualizedInventoryTurnover?default("")}</td>
    <td class="${tdclass}" align="right">${row.profitabilityRatio?default("")}</td>
  </tr>

  <#assign counter = counter + 1>
</#list>

<#-- Totals row -->
  <tr>
    <td class="tableheadtext">${uiLabelMap.FinancialsTotals}</td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=totals.salesSum isoCode=parameters.orgCurrencyUomId/></td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=totals.cogsSum isoCode=parameters.orgCurrencyUomId/></td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=totals.grossProfit isoCode=parameters.orgCurrencyUomId/></td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=totals.inventorySumStart isoCode=parameters.orgCurrencyUomId/></td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=totals.inventorySumEnd isoCode=parameters.orgCurrencyUomId/></td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=totals.inventoryAverage isoCode=parameters.orgCurrencyUomId/></td>
    <td class="tableheadtext" align="right">${totals.inventoryTurnover?default("")}</td>
    <td class="tableheadtext" align="right">${totals.annualizedInventoryTurnover?default("")}</td>
    <td class="tableheadtext" align="right">${totals.profitabilityRatio?default("")}</td>
  </tr>
</table>

</#if>
