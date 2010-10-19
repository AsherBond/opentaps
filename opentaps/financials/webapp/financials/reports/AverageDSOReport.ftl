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
table.averageDSOReport {
  width: 100%;
  border: 1px solid black;
  border-collapse: collapse;
  font: 10px Verdana, Arial, Helvetica, sans-serif;
}
table.averageDSOReport td {
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

<table class="averageDSOReport">
  <tr>
    <td class="tableheadtext">${uiLabelMap.PartyParty}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsNumberOfInvoices}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsInvoicesTotal}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsDSOAverage}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsDSOWeighted}</td>
  </tr>

<#assign counter = 0>
<#list report as row>

  <#if (counter % 2 == 0)>
    <#assign tdclass = "rowGray"/>
  <#else>
    <#assign tdclass = "rowWhite"/>
  </#if>

  <tr>
    <td class="${tdclass}">
      ${row.partyName} 
      <#if isReceivablesReport>
      (<a href="<@ofbizUrl>/customerStatement?partyId=${row.partyId}</@ofbizUrl>">${row.partyId}</a>)
      <#elseif isPayablesReport>
      (<a href="<@ofbizUrl>/vendorStatement?partyId=${row.partyId}</@ofbizUrl>">${row.partyId}</a>)
      </#if>
    </td>
    <td class="${tdclass}" align="right">${row.numberOfInvoices}</td>
    <td class="${tdclass}" align="right"><@ofbizCurrency amount=row.invoiceSum isoCode=parameters.orgCurrencyUomId/></td>
    <td class="${tdclass}" align="right"><font <#if (row.dsoAvg.doubleValue() > totals.dsoAvg.doubleValue())> color="red"</#if>>${Static["java.lang.Math"].round(row.dsoAvg.doubleValue())}</font></td>
    <td class="${tdclass}" align="right"><font <#if (row.dsoWeighted.doubleValue() > totals.dsoWeighted.doubleValue())> color="red"</#if>>${Static["java.lang.Math"].round(row.dsoWeighted.doubleValue())}</font></td>
  </tr>

  <#assign counter = counter + 1>
</#list>

<#-- Totals row -->
  <tr>
    <td class="tableheadtext">${uiLabelMap.FinancialsTotals}</td>
    <td class="tableheadtext" align="right">${totals.numberOfInvoices}</td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=totals.invoiceSum isoCode=parameters.orgCurrencyUomId/></td>
    <td class="tableheadtext" align="right">${Static["java.lang.Math"].round(totals.dsoAvg.doubleValue())}</td>
    <td class="tableheadtext" align="right">${Static["java.lang.Math"].round(totals.dsoWeighted.doubleValue())}</td>
  </tr>

</table>

</#if>
