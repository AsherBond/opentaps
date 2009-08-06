<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 *  
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign now = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()>
<#assign defaultFromDate = Static["org.ofbiz.base.util.UtilDateTime"].getDayStart(now, timeZone, locale)>
<#assign defaultThruDate = Static["org.ofbiz.base.util.UtilDateTime"].getDayEnd(now, timeZone, locale)>

<form method="POST" name="paymentReceiptsDetailForm" action="">
  <div style="margin-left: 30px; margin-top: 5px;">
    <span class="tableheadtext">${uiLabelMap.CommonFromDate}</span>
    <@inputDateTime name="fromDate" default=requestParameters.fromDate?default(defaultFromDate)/>
  </div>
  <div style="margin-left: 30px; margin-top: 5px;">
    <span class="tableheadtext">${uiLabelMap.CommonThruDate}</span>
    <@inputDateTime name="thruDate" default=requestParameters.thruDate?default(defaultThruDate)/>
  </div>
  <div style="margin-left: 30px; margin-top: 10px;">
    <span class="tableheadtext">Gl Account</span> 
    <select name="glAccountId" class="inputBox" size="1">
      <#list glAccounts as glAccount>
      <#if glAccount.glAccountId == requestParameters.glAccountId?default(defaultGlAccountId)><#assign selected = "selected"><#else><#assign selected = ""></#if>
      <option value="${glAccount.glAccountId}" ${selected}>${glAccount.accountCode}: ${glAccount.accountName} (${glAccount.glAccountId})</option>
      </#list>
    </select>
  </div>
  <div style="margin-left: 30px; margin-top: 10px;">
    <span class="tableheadtext">${uiLabelMap.AccountingPaymentMethodType}</span> 
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
  <div style="margin-left: 30px; margin-top: 10px;">
    <input type="Submit" class="smallSubmit" name="submitButton" value="${uiLabelMap.CommonRun}"></input>
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
    <td class="tableheadtext">Payment Id</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsCustomer}</td>
    <#if showInvoiceLevelDetail>
        <td class="tableheadtext">Invoice Id</td>
    </#if>
    <td class="tableheadtext">${uiLabelMap.AccountingReferenceNumber}</td>
    <td class="tableheadtext">${uiLabelMap.AccountingAmount}</td>
    <td class="tableheadtext" width="1%">Cash</td>
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
    <td class="tableheadtext" colspan="${showInvoiceLevelDetail?string("5","4")}" align="right">Total Cash</td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=totalCash isoCode=parameters.orgCurrencyUomId/></td>
    <td></td>
  </tr>
  <tr>
    <td class="tableheadtext" colspan="${showInvoiceLevelDetail?string("5","4")}" align="right">Total Non Cash</td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=total isoCode=parameters.orgCurrencyUomId/></td>
    <td></td>
  </tr>
  <tr>
    <td class="tableheadtext" colspan="${showInvoiceLevelDetail?string("5","4")}" align="right">Total All Receipts</td>
    <td class="tableheadtext" align="right"><@ofbizCurrency amount=(total+totalCash) isoCode=parameters.orgCurrencyUomId/></td>
    <td></td>
  </tr>

</table>

</#if>
