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

<form method="post" name="processCheckRunForm" action="<@ofbizUrl>processCheckRun</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)">

  <@inputHidden name="paymentMethodId" value=parameters.paymentMethodId! />
  <@inputHidden name="initialCheckNumber" value=parameters.initialCheckNumber! />
  <@inputHidden name="organizationPartyId" value=organizationPartyId! />
  <@inputHidden name="partyIdFrom" value=parameters.partyIdFrom! />
  <@inputHidden name="dueDate" value=parameters.dueDate! />
  <@accountingTagsHidden tags=tagTypes prefix="acctgTagEnumId" />

  <@paginate name="listCheckRunInvoices" list=invoiceListBuilder rememberPage=false organizationPartyId=organizationPartyId tagTypes=tagTypes>
  <#noparse>
  <@navigationHeader/>
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

    <#list pageRows as invoice>
      <@inputHidden name="partyIdsFrom" value=invoice.partyIdFrom! index=invoice_index />
      <@inputHidden name="invoiceTypeIds" value=invoice.invoiceTypeId! index=invoice_index />
      <@inputHidden name="invoiceIds" value=invoice.invoiceId! index=invoice_index />
      <@inputHidden name="currencyUomIds" value=invoice.currencyUomId! index=invoice_index />

      <tr class="${tableRowClass(invoice_index)}">
        <@displayDateCell date=invoice.dueDate! />
        <@displayLinkCell href="viewInvoice?invoiceId=${invoice.invoiceId}" text=invoice.invoiceId />
        <@displayCell text=invoice.referenceNumber! />
        <@displayDateCell date=invoice.invoiceDate! />
        <@displayLinkCell href="vendorStatement?partyId=${invoice.partyIdFrom}" text=invoice.fromPartyName! />
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
    <@inputHiddenRowCount list=pageRows />
  </table>
  </#noparse>
  </@paginate>
</form>
