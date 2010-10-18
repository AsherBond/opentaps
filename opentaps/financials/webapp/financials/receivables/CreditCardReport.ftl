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

<@commonReportJs formName="creditCardReportForm" />

<form method="POST" name="creditCardReportForm" action="">
  <div style="margin-left: 30px; margin-top: 5px;">
    <span class="tableheadtext">${uiLabelMap.CommonFromDate}</span>
    <@inputDateTime name="fromDate" default=fromDate?default(defaultFromDate)/>
  </div>
  <div style="margin-left: 30px; margin-top: 5px;">
    <span class="tableheadtext">${uiLabelMap.CommonThruDate}</span>
    <@inputDateTime name="thruDate" default=thruDate?default(defaultThruDate)/>
  </div>
  <div style="margin-left: 30px; margin-top: 10px;">
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

<#if sumReport?exists>

<table class="salesAndInventoryReport">
  <tr>
    <td class="tableheadtext">${uiLabelMap.AccountingCardType}</td>
    <td class="tableheadtext">${uiLabelMap.AccountingAmount}</td>
  </tr>

<#assign counter = 0>
<#assign total = 0>
<#list sumReport as row>

  <#if (counter % 2 == 0)>
    <#assign tdclass = "rowGray"/>
  <#else>
    <#assign tdclass = "rowWhite"/>
  </#if>

  <tr>
    <td class="${tdclass}">${row.cardType}</td>
    <td class="${tdclass}" align="right"><@ofbizCurrency amount=row.amount isoCode=parameters.orgCurrencyUomId/></td>
  </tr>

  <#assign counter = counter + 1>
  <#assign total = total + row.amount>
</#list>

<#-- Totals row -->
  <tr>
    <td class="tableheadtext">${uiLabelMap.FinancialsTotals}</td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=total isoCode=parameters.orgCurrencyUomId/></td>
  </tr>
</table>

</#if>

<#if detailReport?exists>

<table class="salesAndInventoryReport">
  <tr>
    <td class="tableheadtext">${uiLabelMap.FinancialsTransactionDate}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsPaymentId}</td>
    <td class="tableheadtext">${uiLabelMap.AccountingCardNumber}</td>
    <td class="tableheadtext">${uiLabelMap.AccountingCardType}</td>
    <td class="tableheadtext">${uiLabelMap.AccountingExpirationDate}</td>
    <td class="tableheadtext">${uiLabelMap.AccountingAmount}</td>
    <td class="tableheadtext">${uiLabelMap.OrderOrderId}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsInvoiceId}</td>
    <td class="tableheadtext">${uiLabelMap.AccountingReferenceNumber}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsGatewayCode}</td>
  </tr>

<#assign counter = 0>
<#list detailReport as row>
  <#-- in case the Payment did not record a CreditCard via paymentMethodId -->
  <#assign creditCard = null>
  <#if row.paymentMethodId?has_content>
     <#assign creditCard = row.getRelatedOne("CreditCard")>
  </#if>

  <#if (counter % 2 == 0)>
    <#assign tdclass = "rowGray"/>
  <#else>
    <#assign tdclass = "rowWhite"/>
  </#if>

  <tr>
    <td class="${tdclass}">${getLocalizedDate(row.transactionDate)}</td>
    <td class="${tdclass}"><a href="<@ofbizUrl>viewPayment?paymentId=${row.paymentId}</@ofbizUrl>">${row.paymentId}</a></td>
    <td class="${tdclass}">${creditCard.cardNumber?default("")}</td>
    <td class="${tdclass}">${creditCard.cardType?default(uiLabelMap.OpentapsUnknown)}</td>
    <td class="${tdclass}">${creditCard.expireDate?if_exists}</td>
    <td class="${tdclass}" align="right">
    <#if row.amountApplied?has_content>
    <@ofbizCurrency amount=row.amountApplied isoCode=row.currencyUomId/></td>
    <#else>
    <@ofbizCurrency amount=row.paymentAmount isoCode=row.currencyUomId/></td>    
    </#if>
    <td class="${tdclass}">
      <#if row.orderId?has_content>
        <a href="/ordermgr/control/orderview?orderId=${row.orderId}${externalKeyParam?if_exists}">${row.orderId}</a>
      </#if>
    </td>
    <td class="${tdclass}">
      <#if row.invoiceId?has_content>
        <a href="<@ofbizUrl>viewInvoice?invoiceId=${row.invoiceId}</@ofbizUrl>">${row.invoiceId}</a>
      </#if>
    </td>
    <td class="${tdclass}">${row.referenceNum?if_exists}</td>
    <td class="${tdclass}">${row.gatewayCode?if_exists}</td>
  </tr>

  <#assign counter = counter + 1>
</#list>

</table>

</#if>
