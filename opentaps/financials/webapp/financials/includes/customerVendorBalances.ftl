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
<#if isBalanceScreenRendered?exists & isBalanceScreenRendered>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>

<#if reportType == "VENDOR"><#assign action="vendorStatement">
<#elseif reportType == "CUSTOMER"><#assign action="customerStatement">
<#elseif reportType == "COMMISSION"><#assign action="commissionsStatement">
</#if>

<#assign fromDate>${getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].getDayStart(asOfDateTime, -30, timeZone, locale), "DATE")}</#assign>
<#assign thruDate>${getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].getDayStart(asOfDateTime, timeZone, locale), "DATE")}</#assign>

<#-- Outer table is to wrap around the balances to the left and the chart to the right -->
<table border="0" cellpadding="10">
<tr>
<td valign="top">

<#-- Inner tables of the customer/vendor balances -->
<table class="listTable" style="border:0px">
  <tr>
    <td colspan="2" class="tabletext" align="center">
      ${uiLabelMap.CommonFor} ${parameters.organizationName?if_exists} (${organizationPartyId})<br/>
       ${getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp(), "DATE")}
    </td>
  </tr>
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
</table>

<@paginate name="customerVendorBalanceList" list=customerVendorBalanceList action=action fromDate=fromDate thruDate=thruDate>
<#noparse>

<#if parameters.action == "customerStatement">
<form method="post" action="<@ofbizUrl>CustomerStatement.pdf</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="CustomerStatement">
  <@inputHidden name="asOfDate" value=parameters.thruDate />
</#if>

<@navigationHeader/>
<table class="listTable">
  <tr class="listTableHeader">
  	<@headerCell title=uiLabelMap.PartyPartyId orderBy="partyId" />
  	<@headerCell title=uiLabelMap.PartyName orderBy="partyName" />
    <@headerCell title=uiLabelMap.FinancialsReceivablesBalancesReportBalance orderBy="amount" />
    <#if parameters.action == "customerStatement">
    <td><input type="checkbox" name="selectAll" checked value="Y" onclick="javascript:toggleAll(this, 'CustomerStatement');"/></td>
    </#if>
  </tr>

  <#list pageRows as row>
  <#if row.amount != 0.0>
  <tr class="${tableRowClass(row_index)}">
    <@inputHidden name="partyId_o_${row_index}" value=row.partyId />
    <@displayLinkCell href="${parameters.action}?partyId=${row.partyId}" text=row.partyId />
	<@displayCell text=row.partyName />
    <td align="right"><@ofbizCurrency amount=row.amount isoCode=orgCurrencyUomId/></td>
    <#if parameters.action == "customerStatement">
    <td><div class="tabletext"><input type="checkbox" checked name="_rowSubmit_o_${row_index}" value="Y"/> </div></td>
    </#if>
  </tr>
  </#if>
  </#list>
  <#if pageSize != 0 && parameters.action == "customerStatement">
      <tr>
        <td colspan="2" align="right">
        ${uiLabelMap.FinancialsCustomerStatementDateBasis}
        <select name="useAgingDate">
          <option value="true">${uiLabelMap.AccountingDueDate}</option>
          <option value="false">${uiLabelMap.AccountingInvoiceDate}</option>
        </select>
        </td>
        <td colspan="2" align="right">
        <a href="javascript:document.CustomerStatement.submit();" class="subMenuButton">${uiLabelMap.FinancialsPrintStatements}</a>
        </td>
      </tr>
  </#if>
</table>
<#if parameters.action == "customerStatement">
</form>
</#if>
</#noparse>
</@paginate>

<hr />

<table class="listTable" style="border:0px">
  <tr>
    <td class="tableheadtext">&nbsp;</td>
    <td class="tableheadtext" align="right">${uiLabelMap.FinancialsTotalOutstanding}</td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=totalAmount isoCode=orgCurrencyUomId/></td>
  </tr>
</table>

</td>

<#if hasData>
<td rowspan="${numberRows}" valign="top">
  <#-- <#if fileName?has_content><p align="right"><a class="subMenuButton" href="poi/opentaps.xls?&file=${fileName}">Excel</a></p></#if> -->
  <img src="<@ofbizUrl>showChart?chart=${chartImage?if_exists?html}</@ofbizUrl>" style="vertical-align:middle; margin-left:35px"/>
</td>
</#if>

</tr>
</table>

</#if>
