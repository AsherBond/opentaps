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
<@import location="component://financials/webapp/financials/includes/commonReportMacros.ftl"/>

<#assign now = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()>
<#assign defaultFromDate = Static["org.ofbiz.base.util.UtilDateTime"].getDayStart(now, timeZone, locale)>
<#assign defaultThruDate = Static["org.ofbiz.base.util.UtilDateTime"].getDayEnd(now, timeZone, locale)>

<@commonReportJs formName="paymentReceiptsDetailForm" />

<form method="POST" name="paymentReceiptsDetailForm" action="">
  <div style="margin-left: 30px; margin-top: 5px;">
    <span class="tableheadtext">${uiLabelMap.CommonFromDate}</span>
    <@inputDateTime name="fromDate" default=fromDate?default(defaultFromDate)/>
  </div>
  <div style="margin-left: 30px; margin-top: 5px;">
    <span class="tableheadtext">${uiLabelMap.CommonThruDate}</span>
    <@inputDateTime name="thruDate" default=thruDate?default(defaultThruDate)/>
  </div>
  <div style="margin-left: 30px; margin-top: 10px;">
    <span class="tableheadtext">${uiLabelMap.FinancialsGlAccount}</span> 
    <select name="glAccountId" class="inputBox" size="1">
      <#list glAccounts as glAccount>
      <#if glAccount.glAccountId == requestParameters.glAccountId?default(defaultGlAccountId)><#assign selected = "selected"><#else><#assign selected = ""></#if>
      <option value="${glAccount.glAccountId}" ${selected}>${glAccount.accountCode}: ${glAccount.accountName} (${glAccount.glAccountId})</option>
      </#list>
    </select>
  </div>
  <div style="margin-left: 30px; margin-top: 10px;">
    <span class="tableheadtext">${uiLabelMap.FinancialsPaymentMethodType}</span> 
    <select name="paymentMethodTypeId" class="inputBox">
      <option value=""></option>
      <#list paymentMethodTypes as paymentMethodType>
          <option value="${paymentMethodType.paymentMethodTypeId}" ${(paymentMethodType.paymentMethodTypeId == requestParameters.paymentMethodTypeId?default(""))?string("selected=\"selected\"","")}>${paymentMethodType.description}</option>
      </#list>
    </select>
  </div>
  <div style="margin-left: 30px; margin-top: 10px;">
    <span class="tableheadtext">${uiLabelMap.FinancialsShowInvoiceLevelDetail}</span> 
    <select name="showInvoiceLevelDetail" class="inputBox">
      <option value="N" ${("N" == requestParameters.showInvoiceLevelDetail?default("N"))?string("selected=\"selected\"","")}>${uiLabelMap.CommonN}</option>
      <option value="Y" ${("Y" == requestParameters.showInvoiceLevelDetail?default("N"))?string("selected=\"selected\"","")}>${uiLabelMap.CommonY}</option>
    </select>
  </div>
  <div style="margin-top: 10px;">
    <@submitReportOptions reportRequest=reportRequest! screenRequest=screenRequest! returnPage=returnPage! returnLabel=returnLabel!/>
  </div>  
</form>

<br/>

<style type="text/css">
table.salesAndInventoryReport {
  width: 100%;
  border: 1px solid black;
  border-collapse: collapse;
  font: 10px Verdana, Arial, Helvetica, sans-serif;
  margin-bottom: 30px;
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

<#assign showInvoiceLevelDetail = ( requestParameters.showInvoiceLevelDetail?default("N") == "Y" )/>

<table class="salesAndInventoryReport">
  <tr>
    <td class="tableheadtext">${uiLabelMap.FinancialsTransactionDate}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsPaymentId}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsCustomer}</td>
    <#if showInvoiceLevelDetail>
        <td class="tableheadtext">${uiLabelMap.FinancialsInvoiceId}</td>
    </#if>
    <td class="tableheadtext">${uiLabelMap.AccountingReferenceNumber}</td>
    <td class="tableheadtext">${uiLabelMap.AccountingAmount}</td>
    <td class="tableheadtext" width="1%">${uiLabelMap.FinancialsCash}</td>
  </tr>

<#assign counter = 0>
<#assign total = 0>
<#assign totalCash = 0>
<#list report as row>

  <#if (counter % 2 == 0)>
    <#assign tdclass = "rowGray"/>
  <#else>
    <#assign tdclass = "rowWhite"/>
  </#if>

  <#assign conversion = 1.0>
  <#if row.currencyUomId?has_content>
    <#assign conversion = Static["com.opensourcestrategies.financials.util.UtilFinancial"].determineUomConversionFactor(delegator, dispatcher, parameters.organizationPartyId, row.currencyUomId, row.transactionDate)>
  </#if>
  <#if showInvoiceLevelDetail>
      <#assign amount = row.amountApplied?default(row.amount)>
  <#else>
      <#assign amount = row.amount>
  </#if>
  <#assign amountConverted = amount * conversion>

  <tr>
    <td class="${tdclass}">${getLocalizedDate(row.transactionDate)}</td>
    <td class="${tdclass}"><a href="<@ofbizUrl>viewPayment?paymentId=${row.paymentId}</@ofbizUrl>">${row.paymentId}</a></td>
    <td class="${tdclass}">${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, row.partyIdFrom, false)} 
      (<a href="<@ofbizUrl>customerStatement?partyId=${row.partyIdFrom}</@ofbizUrl>">${row.partyIdFrom}</a>)</td>
      <#if showInvoiceLevelDetail>
          <td class="${tdclass}">
            <#if row.invoiceId?has_content>
              <a href="<@ofbizUrl>viewInvoice?invoiceId=${row.invoiceId}</@ofbizUrl>">${row.invoiceId}</a>
            </#if>
          </td>
      </#if>
    </td>
    <td class="${tdclass}">${row.paymentRefNum?if_exists}</td>
    <td class="${tdclass}" align="right"><@ofbizCurrency amount=amount isoCode=row.currencyUomId /></td>
    <td class="${tdclass}"><#if row.paymentMethodTypeId?default("none") == "CASH">&#10003;</#if></td>
  </tr>

  <#assign counter = counter + 1>
  <#if row.paymentMethodTypeId?default("none") == "CASH">
    <#assign totalCash = totalCash + amountConverted>
  <#else>
    <#assign total = total + amountConverted>
  </#if>
</#list>

<#-- Totals -->
  <tr>
    <td class="tableheadtext" colspan="${showInvoiceLevelDetail?string("5","4")}" align="right">${uiLabelMap.FinancialsTotalCash}</td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=totalCash isoCode=parameters.orgCurrencyUomId/></td>
    <td></td>
  </tr>
  <tr>
    <td class="tableheadtext" colspan="${showInvoiceLevelDetail?string("5","4")}" align="right">${uiLabelMap.FinancialsTotalNonCash}</td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=total isoCode=parameters.orgCurrencyUomId/></td>
    <td></td>
  </tr>
  <tr>
    <td class="tableheadtext" colspan="${showInvoiceLevelDetail?string("5","4")}" align="right">${uiLabelMap.FinancialsTotalAllReceipts}</td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=(total+totalCash) isoCode=parameters.orgCurrencyUomId/></td>
    <td></td>
  </tr>

</table>

</#if>
