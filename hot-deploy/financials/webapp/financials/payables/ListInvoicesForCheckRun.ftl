<#--
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if invoices?has_content>

<form method="post" name="processCheckRunForm" action="<@ofbizUrl>processCheckRun</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)">

  <@inputHidden name="paymentMethodId" value=parameters.paymentMethodId! />
  <@inputHidden name="initialCheckNumber" value=parameters.initialCheckNumber! />
  <@inputHidden name="organizationPartyId" value=organizationPartyId! />
  <@inputHidden name="partyIdFrom" value=parameters.partyIdFrom! />
  <@inputHidden name="dueDate" value=parameters.dueDate! />

  <@accountingTagsHidden tags=tagTypes prefix="acctgTagEnumId" />

  <table class="listTable">
    <tr class="listTableHeader">
      <td>${uiLabelMap.AccountingDueDate}</td>
      <td>${uiLabelMap.AccountingInvoiceID}</td>
      <td>${uiLabelMap.FinancialsReferenceNumber}</td>
      <td>${uiLabelMap.AccountingInvoiceDate}</td>
      <td>${uiLabelMap.PartyParty}</td>
      <td align="right">${uiLabelMap.FinancialsAmountOutstanding}</td>
      <td>${uiLabelMap.FinancialsAmountToPay}</td>
      <td><@inputMultiSelectAll form="processCheckRunForm"/></td>
    </tr>

    <#list invoices as invoice>
      <@inputHidden name="partyIdsFrom" value=invoice.partyIdFrom! index=invoice_index />
      <@inputHidden name="invoiceTypeIds" value=invoice.invoiceTypeId! index=invoice_index />
      <@inputHidden name="invoiceIds" value=invoice.invoiceId! index=invoice_index />
      <@inputHidden name="currencyUomIds" value=invoice.currencyUomId! index=invoice_index />

      <tr class="${tableRowClass(invoice_index)}">
        <@displayDateCell date=invoice.dueDate! />
        <@displayLinkCell href="viewInvoice?invoiceId=${invoice.invoiceId}" text=invoice.invoiceId />
        <@displayCell text=invoice.referenceNumber! />
        <@displayDateCell date=invoice.invoiceDate! />
        <@displayLinkCell href="vendorStatement?partyId=${invoice.partyIdFrom}" text=fromPartyNames.get(invoice.invoiceId)! />
        <@displayCurrencyCell amount=invoice.pendingOpenAmount currencyUomId=invoice.currencyUomId! />
        <@inputTextCell name="amounts" size=10 index=invoice_index default=invoice.pendingOpenAmount />
        <@inputMultiCheckCell index=invoice_index />
      </tr>
    </#list>

    <tr>
      <td colspan="6"/>
      <td align="right"><@inputSubmit title=uiLabelMap.FinancialsIssueChecks /></td>
      <td/>
    </tr>

    <@inputHiddenUseRowSubmit />
    <@inputHiddenRowCount list=invoices />
  </table>
</form>

</#if>
